/*
 * Decompiled with CFR 0.152.
 */
package zombie.spnetwork;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoObjectChange;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.network.packets.INetworkPacket;
import zombie.spnetwork.SinglePlayerClient;
import zombie.spnetwork.UdpConnection;
import zombie.spnetwork.UdpEngine;
import zombie.spnetwork.ZomboidNetData;
import zombie.spnetwork.ZomboidNetDataPool;

public final class SinglePlayerServer {
    private static final ArrayList<ZomboidNetData> MainLoopNetData = new ArrayList();
    public static final UdpEngineServer udpEngine = new UdpEngineServer();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addIncoming(short id, ByteBuffer bb, UdpConnection connection) {
        ZomboidNetData d = bb.remaining() > 2048 ? ZomboidNetDataPool.instance.getLong(bb.remaining()) : ZomboidNetDataPool.instance.get();
        d.read(id, bb, connection);
        ArrayList<ZomboidNetData> arrayList = MainLoopNetData;
        synchronized (arrayList) {
            MainLoopNetData.add(d);
        }
    }

    private static void sendObjectChange(IsoObject o, IsoObjectChange change, KahluaTable tbl, UdpConnection c) {
        if (o.getSquare() != null) {
            try {
                INetworkPacket packet = c.getPacket(PacketTypes.PacketType.ObjectChange);
                packet.setData(new Object[]{o, change, tbl});
                ByteBufferWriter bbw = c.startPacket();
                PacketTypes.PacketType.ObjectChange.doPacket(bbw);
                packet.write(bbw);
                c.endPacketImmediate();
            }
            catch (Exception e) {
                c.cancelPacket();
                DebugLog.Multiplayer.printException(e, "Packet ObjectChange sp send error", LogSeverity.Error);
            }
        }
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, KahluaTable tbl) {
        if (o == null) {
            return;
        }
        for (int n = 0; n < SinglePlayerServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = SinglePlayerServer.udpEngine.connections.get(n);
            if (!c.ReleventTo(o.getX(), o.getY())) continue;
            SinglePlayerServer.sendObjectChange(o, change, tbl, c);
        }
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, Object ... objects) {
        if (objects.length == 0) {
            SinglePlayerServer.sendObjectChange(o, change, null);
            return;
        }
        if (objects.length % 2 != 0) {
            return;
        }
        KahluaTable t = LuaManager.platform.newTable();
        for (int i = 0; i < objects.length; i += 2) {
            Object v = objects[i + 1];
            if (v instanceof Float) {
                Float f = (Float)v;
                t.rawset(objects[i], (Object)f.doubleValue());
                continue;
            }
            if (v instanceof Integer) {
                Integer integer = (Integer)v;
                t.rawset(objects[i], (Object)integer.doubleValue());
                continue;
            }
            if (v instanceof Short) {
                Short s = (Short)v;
                t.rawset(objects[i], (Object)s.doubleValue());
                continue;
            }
            t.rawset(objects[i], v);
        }
        SinglePlayerServer.sendObjectChange(o, change, t);
    }

    public static void sendServerCommand(String module, String command, KahluaTable args2, UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putUTF(module);
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
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        c.endPacketImmediate();
    }

    public static void sendServerCommand(String module, String command, KahluaTable args2) {
        for (int n = 0; n < SinglePlayerServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = SinglePlayerServer.udpEngine.connections.get(n);
            SinglePlayerServer.sendServerCommand(module, command, args2, c);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void update() {
        if (GameClient.client) {
            return;
        }
        for (short i = 0; i < IsoPlayer.numPlayers; i = (short)(i + 1)) {
            if (IsoPlayer.players[i] == null) continue;
            IsoPlayer.players[i].setOnlineID(i);
        }
        ArrayList<ZomboidNetData> arrayList = MainLoopNetData;
        synchronized (arrayList) {
            for (int n = 0; n < MainLoopNetData.size(); ++n) {
                ZomboidNetData data = MainLoopNetData.get(n);
                SinglePlayerServer.mainLoopDealWithNetData(data);
                MainLoopNetData.remove(n--);
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static void mainLoopDealWithNetData(ZomboidNetData d) {
        ByteBufferReader bb = d.bufferReader;
        try {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(d.type);
            switch (packetType) {
                case ClientCommand: {
                    SinglePlayerServer.receiveClientCommand(bb, d.connection);
                    return;
                }
                case GlobalObjects: {
                    SinglePlayerServer.receiveGlobalObjects(bb, d.connection);
                    return;
                }
            }
            return;
        }
        finally {
            ZomboidNetDataPool.instance.discard(d);
        }
    }

    private static IsoPlayer getAnyPlayerFromConnection(UdpConnection connection) {
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            if (connection.players[playerIndex] == null) continue;
            return connection.players[playerIndex];
        }
        return null;
    }

    private static IsoPlayer getPlayerFromConnection(UdpConnection connection, int playerIndex) {
        if (playerIndex >= 0 && playerIndex < 4) {
            return connection.players[playerIndex];
        }
        return null;
    }

    private static void receiveClientCommand(ByteBufferReader bb, UdpConnection connection) {
        byte playerIndex = bb.getByte();
        String module = bb.getUTF();
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
        IsoPlayer player = SinglePlayerServer.getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = SinglePlayerServer.getAnyPlayerFromConnection(connection);
        }
        if (player == null) {
            DebugLog.log("receiveClientCommand: player is null");
            return;
        }
        LuaEventManager.triggerEvent("OnClientCommand", module, command, player, tbl);
    }

    private static void receiveGlobalObjects(ByteBufferReader bb, UdpConnection connection) {
        byte playerIndex = bb.getByte();
        IsoPlayer player = SinglePlayerServer.getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = SinglePlayerServer.getAnyPlayerFromConnection(connection);
        }
        if (player == null) {
            DebugLog.log("receiveGlobalObjects: player is null");
            return;
        }
        SGlobalObjectNetwork.receive(bb, player);
    }

    public static void Reset() {
        for (ZomboidNetData data : MainLoopNetData) {
            ZomboidNetDataPool.instance.discard(data);
        }
        MainLoopNetData.clear();
    }

    public static final class UdpEngineServer
    extends UdpEngine {
        public final ArrayList<UdpConnection> connections = new ArrayList();

        UdpEngineServer() {
            this.connections.add(new UdpConnection(this));
        }

        @Override
        public void Send(ByteBuffer bb) {
            SinglePlayerClient.udpEngine.Receive(bb);
        }

        @Override
        public void Receive(ByteBuffer bb) {
            int id = bb.get() & 0xFF;
            short pkt = bb.getShort();
            SinglePlayerServer.addIncoming(pkt, bb, SinglePlayerServer.udpEngine.connections.get(0));
        }
    }
}

