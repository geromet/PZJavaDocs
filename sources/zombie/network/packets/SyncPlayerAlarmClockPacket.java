/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.IAlarmClock;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class SyncPlayerAlarmClockPacket
implements INetworkPacket {
    protected final PlayerID playerId = new PlayerID();
    int itemId = -1;
    boolean stopRinging = true;
    int alarmHour = -1;
    int alarmMinutes = -1;
    int forceDontRing = -1;
    boolean alarmSet = true;
    IAlarmClock alarmClock;

    @Override
    public void setData(Object ... values2) {
        this.playerId.set((IsoPlayer)values2[0]);
        this.itemId = (Integer)values2[1];
        this.stopRinging = (Boolean)values2[2];
        this.alarmHour = (Integer)values2[3];
        this.alarmMinutes = (Integer)values2[4];
        this.alarmSet = (Boolean)values2[5];
        this.forceDontRing = (Integer)values2[6];
        this.findAlarmClockPlayer((IsoPlayer)values2[0]);
    }

    private <T extends IAlarmClock> void updateAlarmClock(T ac) {
        if (ac == null) {
            DebugLog.Multiplayer.noise("[SyncAlarmClock] skipping update of null AlarmClock");
            return;
        }
        if (this.stopRinging) {
            ac.stopRinging();
        } else {
            ac.setAlarmSet(this.alarmSet);
            ac.setHour(this.alarmHour);
            ac.setMinute(this.alarmMinutes);
            ac.setForceDontRing(this.forceDontRing);
        }
    }

    private void findAlarmClockPlayer(IsoPlayer player) {
        if (player == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] unable to find clock for null player");
            return;
        }
        InventoryItem inventoryItem = player.getInventory().getItemWithID(this.itemId);
        if (inventoryItem != null && inventoryItem instanceof IAlarmClock) {
            IAlarmClock iAlarmClock;
            this.alarmClock = iAlarmClock = (IAlarmClock)((Object)inventoryItem);
        } else {
            DebugLog.Multiplayer.noise("[SyncAlarmClock] unable to find alarmClock for getPlayerNum()=" + player.getPlayerNum() + " getOnlineID()=" + player.getOnlineID() + " itemID:" + this.itemId);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.itemId);
        if (!b.putBoolean(this.stopRinging)) {
            b.putInt(this.alarmHour);
            b.putInt(this.alarmMinutes);
            b.putInt(this.forceDontRing);
            b.putBoolean(this.alarmSet);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.playerId.parse(b, connection);
        this.itemId = b.getInt();
        this.stopRinging = b.getBoolean();
        if (!this.stopRinging) {
            this.alarmHour = b.getInt();
            this.alarmMinutes = b.getInt();
            this.forceDontRing = b.getInt();
            this.alarmSet = b.getBoolean();
        }
        this.findAlarmClockPlayer(this.playerId.getPlayer());
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.playerId.getPlayer() == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] not relevant null isoPlayer");
            return false;
        }
        if (!connection.isRelevantTo(this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y)) {
            DebugLog.Multiplayer.noise("[SyncAlarmClock] not relevant client isoPlayer[x,y]=" + this.playerId.getPlayer().getX() + "," + this.playerId.getPlayer().getY());
            return false;
        }
        return true;
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.playerId != null && this.playerId.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!this.isRelevant(connection)) {
            return;
        }
        this.updateAlarmClock(this.alarmClock);
        if (this.playerId.getPlayer() == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] unable to process on server null isoPlayer");
            return;
        }
        this.sendToRelativeClients(PacketTypes.PacketType.SyncPlayerAlarmClock, null, this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.updateAlarmClock(this.alarmClock);
        if (this.alarmClock != null) {
            this.alarmClock.update();
        }
    }
}

