/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.zip.CRC32;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.GameLoadingState;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatRecipe;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Recipe;

@PacketSetting(ordering=0, priority=0, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=7, anticheats={AntiCheat.Recipe})
public class RecipePacket
implements INetworkPacket,
AntiCheatRecipe.IAntiCheat {
    @JSONField
    private long checksum;
    @JSONField
    private long checksumFromClient;
    @JSONField
    private int salt;
    @JSONField
    private byte flags;

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 3 && values2[0] instanceof Integer && values2[1] instanceof Boolean && values2[2] instanceof Boolean) {
            this.set((Integer)values2[0], (Boolean)values2[1], (Boolean)values2[2]);
        }
    }

    private void set(int salt, boolean queued, boolean done) {
        this.salt = salt;
        this.flags = 0;
        this.flags = (byte)(this.flags | (queued ? (byte)1 : 0));
        this.flags = (byte)(this.flags | (done ? 2 : 0));
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.flags = b.getByte();
        if (GameClient.client) {
            this.salt = b.getInt();
            this.checksum = this.calculateChecksum(this.salt);
        } else if (GameServer.server) {
            this.checksumFromClient = b.getLong();
            this.salt = connection.getValidator().getSalt();
            this.checksum = this.calculateChecksum(this.salt);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.flags);
        if (GameServer.server) {
            b.putInt(this.salt);
        } else if (GameClient.client) {
            b.putLong(this.checksum);
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.sendToServer(PacketTypes.PacketType.Validate);
        if ((this.flags & 2) != 0) {
            GameLoadingState.Done();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if ((this.flags & 1) != 0) {
            connection.getValidator().checksumSend(false, true);
        }
    }

    private long calculateChecksum(int salt) {
        CRC32 crcMaker = new CRC32();
        CRC32 crcReceipt = new CRC32();
        ByteBuffer bb = ByteBuffer.allocate(8);
        crcMaker.update(salt);
        ArrayList<Recipe> recipes = ScriptManager.instance.getAllRecipes();
        for (Recipe recipe : recipes) {
            crcReceipt.reset();
            bb.clear();
            crcReceipt.update(recipe.getOriginalname().getBytes());
            crcReceipt.update((int)recipe.timeToMake);
            if (recipe.skillRequired != null) {
                for (Recipe.RequiredSkill requiredSkill : recipe.skillRequired) {
                    crcReceipt.update(requiredSkill.getPerk().index());
                    crcReceipt.update(requiredSkill.getLevel());
                }
            }
            for (Recipe.Source source2 : recipe.getSource()) {
                for (String item : source2.getItems()) {
                    crcReceipt.update(item.getBytes());
                }
            }
            crcReceipt.update(recipe.getResult().getType().getBytes());
            crcReceipt.update(recipe.getResult().getModule().getBytes());
            crcReceipt.update(recipe.getResult().getCount());
            long value = crcReceipt.getValue();
            bb.putLong(value);
            bb.position(0);
            crcMaker.update(bb);
        }
        return crcMaker.getValue();
    }

    @Override
    public long getClientChecksum() {
        return this.checksum;
    }

    @Override
    public long getServerChecksum() {
        return this.checksumFromClient;
    }
}

