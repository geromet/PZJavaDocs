/*
 * Decompiled with CFR 0.152.
 */
package zombie.globalObjects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.SGlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.globalObjects.SGlobalObjects;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.spnetwork.SinglePlayerServer;

public final class SGlobalObjectNetwork {
    public static final byte PACKET_ServerCommand = 1;
    public static final byte PACKET_ClientCommand = 2;
    public static final byte PACKET_NewLuaObjectAt = 3;
    public static final byte PACKET_RemoveLuaObjectAt = 4;
    public static final byte PACKET_UpdateLuaObjectAt = 5;
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(0x100000);
    private static final ByteBufferWriter BYTE_BUFFER_WRITER = new ByteBufferWriter(BYTE_BUFFER);

    public static void receive(ByteBufferReader bb, IsoPlayer player) {
        byte packet = bb.getByte();
        switch (packet) {
            case 2: {
                SGlobalObjectNetwork.receiveClientCommand(bb, player);
            }
        }
    }

    private static void sendPacket(ByteBuffer bb) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.put(bb);
                c.endPacketImmediate();
            }
        } else {
            if (GameClient.client) {
                throw new IllegalStateException("can't call this method on the client");
            }
            for (int n = 0; n < SinglePlayerServer.udpEngine.connections.size(); ++n) {
                zombie.spnetwork.UdpConnection c = SinglePlayerServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                bb.flip();
                b.put(bb);
                c.endPacketImmediate();
            }
        }
    }

    private static void writeServerCommand(String systemName, String command, KahluaTable args2, ByteBufferWriter b) {
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)1);
        b.putUTF(systemName);
        b.putUTF(command);
        if (b.putBoolean(args2 != null && !args2.isEmpty())) {
            try {
                KahluaTableIterator it = args2.iterator();
                while (it.advance()) {
                    if (TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
                    DebugLog.log("ERROR: sendServerCommand: can't save key,value=" + String.valueOf(it.getKey()) + "," + String.valueOf(it.getValue()));
                }
                TableNetworkUtils.save(args2, b);
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
            }
        }
    }

    public static void sendServerCommand(String systemName, String command, KahluaTable args2) {
        BYTE_BUFFER.clear();
        SGlobalObjectNetwork.writeServerCommand(systemName, command, args2, BYTE_BUFFER_WRITER);
        SGlobalObjectNetwork.sendPacket(BYTE_BUFFER);
    }

    public static void addGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)3);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte(globalObject.getZ());
        SGlobalObjectSystem system = (SGlobalObjectSystem)globalObject.system;
        TableNetworkUtils.saveSome(globalObject.getModData(), b, system.objectSyncKeys);
        SGlobalObjectNetwork.sendPacket(BYTE_BUFFER);
    }

    public static void removeGlobalObjectOnClient(GlobalObject globalObject) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)4);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte(globalObject.getZ());
        SGlobalObjectNetwork.sendPacket(BYTE_BUFFER);
    }

    public static void updateGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.GlobalObjects.doPacket(b);
        b.putByte((byte)5);
        b.putUTF(globalObject.system.name);
        b.putInt(globalObject.getX());
        b.putInt(globalObject.getY());
        b.putByte(globalObject.getZ());
        SGlobalObjectSystem system = (SGlobalObjectSystem)globalObject.system;
        TableNetworkUtils.saveSome(globalObject.getModData(), b, system.objectSyncKeys);
        SGlobalObjectNetwork.sendPacket(BYTE_BUFFER);
    }

    private static void receiveClientCommand(ByteBufferReader bb, IsoPlayer player) {
        String systemName = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();
            try {
                TableNetworkUtils.load(tbl, bb);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        SGlobalObjects.receiveClientCommand(systemName, command, player, tbl);
    }
}

