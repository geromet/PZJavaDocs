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
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.TableNetworkUtils;
import zombie.network.packets.ObjectChangePacket;
import zombie.spnetwork.SinglePlayerServer;
import zombie.spnetwork.UdpConnection;
import zombie.spnetwork.UdpEngine;
import zombie.spnetwork.ZomboidNetData;
import zombie.spnetwork.ZomboidNetDataPool;

public final class SinglePlayerClient {
    private static final ArrayList<ZomboidNetData> MainLoopNetData = new ArrayList();
    public static final UdpEngine udpEngine = new UdpEngineClient();
    public static final UdpConnection connection = new UdpConnection(udpEngine);

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addIncoming(short id, ByteBuffer bb) {
        ZomboidNetData d = bb.remaining() > 2048 ? ZomboidNetDataPool.instance.getLong(bb.remaining()) : ZomboidNetDataPool.instance.get();
        d.read(id, bb, connection);
        ArrayList<ZomboidNetData> arrayList = MainLoopNetData;
        synchronized (arrayList) {
            MainLoopNetData.add(d);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void update() throws Exception {
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
                try {
                    SinglePlayerClient.mainLoopDealWithNetData(data);
                    continue;
                }
                finally {
                    MainLoopNetData.remove(n--);
                }
            }
        }
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static void mainLoopDealWithNetData(ZomboidNetData d) throws Exception {
        ByteBufferReader bb = d.bufferReader;
        try {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(d.type);
            switch (packetType) {
                case ClientCommand: {
                    SinglePlayerClient.receiveServerCommand(bb);
                    return;
                }
                case GlobalObjects: {
                    CGlobalObjectNetwork.receive(bb);
                    return;
                }
                case ObjectChange: {
                    SinglePlayerClient.receiveObjectChange(bb);
                    return;
                }
                default: {
                    throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)packetType));
                }
            }
        }
        finally {
            ZomboidNetDataPool.instance.discard(d);
        }
    }

    private static void delayPacket(int x, int y, int z) {
    }

    private static IsoPlayer getPlayerByID(int id) {
        return IsoPlayer.players[id];
    }

    private static void receiveObjectChange(ByteBufferReader bb) {
        try {
            ObjectChangePacket.parseObjectChange(null, bb, connection);
        }
        catch (ObjectChangePacket.IsoObjectChangeTargetNotFoundException otnfe) {
            DebugType.Network.error("SinglePlayerClient failed to parse ObjectChange. %s", otnfe.getMessage());
        }
    }

    public static void sendClientCommand(IsoPlayer player, String module, String command, KahluaTable args2) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putByte(player != null ? player.playerIndex : -1);
        b.putUTF(module);
        b.putUTF(command);
        if (b.putBoolean(args2 != null && !args2.isEmpty())) {
            try {
                KahluaTableIterator it = args2.iterator();
                while (it.advance()) {
                    if (TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
                    DebugLog.log("ERROR: sendClientCommand: can't save key,value=" + String.valueOf(it.getKey()) + "," + String.valueOf(it.getValue()));
                }
                TableNetworkUtils.save(args2, b);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        connection.endPacketImmediate();
    }

    private static void receiveServerCommand(ByteBufferReader bb) {
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
        LuaEventManager.triggerEvent("OnServerCommand", module, command, tbl);
    }

    public static void Reset() {
        for (ZomboidNetData data : MainLoopNetData) {
            ZomboidNetDataPool.instance.discard(data);
        }
        MainLoopNetData.clear();
    }

    private static final class UdpEngineClient
    extends UdpEngine {
        private UdpEngineClient() {
        }

        @Override
        public void Send(ByteBuffer bb) {
            SinglePlayerServer.udpEngine.Receive(bb);
        }

        @Override
        public void Receive(ByteBuffer bb) {
            int id = bb.get() & 0xFF;
            short pkt = bb.getShort();
            SinglePlayerClient.addIncoming(pkt, bb);
        }
    }
}

