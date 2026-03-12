/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.IOException;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Action;
import zombie.core.ActionManager;
import zombie.core.Transaction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;

public class FishingAction
extends Action {
    public static byte flagStartFishing = 1;
    public static byte flagStopFishing = (byte)2;
    public static byte flagUpdateFish = (byte)4;
    public static byte flagUpdateBobberParameters = (byte)8;
    public static byte flagCreateBobber = (byte)16;
    public static byte flagDestroyBobber = (byte)32;
    private static final HashMap<IsoPlayer, InventoryItem> fishForPickUp = new HashMap();
    @JSONField
    public byte contentFlag;
    @JSONField
    private int fishingRodId;
    @JSONField
    private final Square position = new Square();
    private final KahluaTable tbl = LuaManager.platform.newTable();
    private KahluaTable fishingRod;
    private KahluaTable bobber;
    private KahluaTable currentFish;
    private InventoryItem currentFishItem;
    private InventoryItem lastSentFishItem;
    private boolean catchFishStarted;

    public void setStartFishing(IsoPlayer player, InventoryItem item, IsoGridSquare sq, KahluaTable bobber) {
        this.set(player);
        this.contentFlag = (byte)(this.contentFlag | flagStartFishing);
        this.fishingRodId = item == null ? 0 : item.getID();
        this.position.set(sq);
        this.bobber = bobber;
    }

    @Override
    public float getDuration() {
        return 100000.0f;
    }

    @Override
    public void start() {
        fishForPickUp.put(this.playerId.getPlayer(), null);
        this.setTimeData();
        KahluaTable fishingClass = (KahluaTable)LuaManager.env.rawget("Fishing");
        KahluaTable fishingRodClass = (KahluaTable)fishingClass.rawget("FishingRod");
        this.fishingRod = (KahluaTable)LuaManager.caller.pcall(LuaManager.thread, fishingRodClass.rawget("new"), fishingRodClass, this.playerId.getPlayer())[1];
        this.fishingRod.rawset("mpAimX", (Object)this.position.getX());
        this.fishingRod.rawset("mpAimY", (Object)this.position.getY());
        LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("cast"), (Object)this.fishingRod);
        this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
        DebugLog.Action.trace("FishingAction.start %s", this.getDescription());
    }

    @Override
    public void stop() {
        if (this.bobber != null) {
            LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("destroy"), (Object)this.bobber);
        }
        DebugLog.Action.trace("FishingAction.stop %s", this.getDescription());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isUsingTimeout() {
        return true;
    }

    @Override
    public void update() {
        this.setTimeData();
        boolean shouldSend = false;
        if (GameClient.client) {
            if (this.state != Transaction.TransactionState.Accept) {
                return;
            }
            boolean catchFishStarted = (Boolean)this.bobber.rawget("catchFishStarted");
            if (catchFishStarted != this.catchFishStarted) {
                this.catchFishStarted = catchFishStarted;
                this.contentFlag = (byte)(this.contentFlag | flagUpdateBobberParameters);
                shouldSend = true;
            }
            if (shouldSend) {
                ByteBufferWriter bb = GameClient.connection.startPacket();
                PacketTypes.PacketType.FishingAction.doPacket(bb);
                this.write(bb);
                PacketTypes.PacketType.FishingAction.send(GameClient.connection);
                this.contentFlag = 0;
            }
            return;
        }
        LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("update"), (Object)this.fishingRod);
        if (this.bobber == null) {
            this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
        } else {
            this.currentFish = (KahluaTable)this.bobber.rawget("fish");
            this.currentFishItem = this.currentFish != null ? (InventoryItem)this.currentFish.rawget("fishItem") : null;
            if (this.currentFishItem != this.lastSentFishItem) {
                this.contentFlag = (byte)(this.contentFlag | flagUpdateFish);
                shouldSend = true;
                fishForPickUp.put(this.playerId.getPlayer(), this.currentFishItem);
            }
            if (shouldSend) {
                UdpConnection connection = GameServer.getConnectionFromPlayer(this.playerId.getPlayer());
                ByteBufferWriter bb = connection.startPacket();
                PacketTypes.PacketType.FishingAction.doPacket(bb);
                this.write(bb);
                PacketTypes.PacketType.FishingAction.send(connection);
                this.contentFlag = 0;
            }
        }
    }

    @Override
    public boolean perform() {
        return false;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.contentFlag = b.getByte();
        if ((this.contentFlag & flagStartFishing) != 0) {
            this.fishingRodId = b.getInt();
            this.position.parse(b, connection);
        }
        if ((this.contentFlag & flagUpdateFish) != 0) {
            boolean hasFish = b.getBoolean();
            if (hasFish) {
                try {
                    this.currentFish = LuaManager.platform.newTable();
                    this.currentFish.load(b.bb, 244);
                    this.currentFishItem = InventoryItem.loadItem(b.bb, 244);
                }
                catch (Exception ex) {
                    DebugLog.Objects.printException(ex, this.getDescription(), LogSeverity.Error);
                }
            } else {
                this.currentFish = null;
                this.currentFishItem = null;
            }
        }
        if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            this.catchFishStarted = b.getInt() == 1;
        }
        DebugLog.Action.trace("FishingAction.parse: %s", this.getDescription());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putByte(this.contentFlag);
        if ((this.contentFlag & flagStartFishing) != 0) {
            b.putInt(this.fishingRodId);
            this.position.write(b);
        }
        if ((this.contentFlag & flagUpdateFish) != 0) {
            if (b.putBoolean(this.currentFishItem != null)) {
                try {
                    this.currentFish.save(b.bb);
                    this.currentFishItem.saveWithSize(b.bb, false);
                }
                catch (IOException e) {
                    DebugLog.Objects.printException(e, this.getDescription(), LogSeverity.Error);
                }
            }
            this.lastSentFishItem = this.currentFishItem;
        }
        if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            b.putInt(this.catchFishStarted ? 1 : 0);
        }
        DebugLog.Action.trace("FishingAction.write: %s", this.getDescription());
    }

    public KahluaTable getLuaTable() {
        this.tbl.wipe();
        if (ActionManager.getPlayer(this.id) == null) {
            return null;
        }
        this.tbl.rawset("player", (Object)ActionManager.getPlayer(this.id));
        this.tbl.rawset("Reject", (Object)(this.state == Transaction.TransactionState.Reject ? 1 : 0));
        this.tbl.rawset("UpdateFish", (Object)((this.contentFlag & flagUpdateFish) != 0 ? 1 : 0));
        this.tbl.rawset("UpdateBobberParameters", (Object)((this.contentFlag & flagUpdateBobberParameters) != 0 ? 1 : 0));
        this.tbl.rawset("CreateBobber", (Object)((this.contentFlag & flagCreateBobber) != 0 ? 1 : 0));
        this.tbl.rawset("DestroyBobber", (Object)((this.contentFlag & flagDestroyBobber) != 0 ? 1 : 0));
        this.tbl.rawset("CatchFishStarted", (Object)this.catchFishStarted);
        if ((this.contentFlag & flagUpdateFish) != 0) {
            this.tbl.rawset("fish", (Object)this.currentFish);
            this.tbl.rawset("fishItem", (Object)this.currentFishItem);
        }
        return this.tbl;
    }

    public static InventoryItem getPickedUpFish(IsoPlayer player) {
        return fishForPickUp.get(player);
    }
}

