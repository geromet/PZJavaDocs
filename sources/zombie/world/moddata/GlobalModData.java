/*
 * Decompiled with CFR 0.152.
 */
package zombie.world.moddata;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.service.GlobalModDataPacket;
import zombie.network.packets.service.GlobalModDataRequestPacket;
import zombie.world.WorldDictionary;

public final class GlobalModData {
    public static final String SAVE_EXT = ".bin";
    public static final String SAVE_FILE = "global_mod_data";
    public static GlobalModData instance = new GlobalModData();
    private final Map<String, KahluaTable> modData = new HashMap<String, KahluaTable>();
    private static final int BLOCK_SIZE = 524288;
    private static int lastBlockSize = -1;

    private KahluaTable createModDataTable() {
        return LuaManager.platform.newTable();
    }

    public GlobalModData() {
        this.reset();
    }

    public void init() throws IOException {
        this.reset();
        this.load();
        LuaEventManager.triggerEvent("OnInitGlobalModData", WorldDictionary.isIsNewGame());
    }

    public void reset() {
        lastBlockSize = -1;
        this.modData.clear();
    }

    public void collectTableNames(List<String> list) {
        list.clear();
        for (Map.Entry<String, KahluaTable> entry : this.modData.entrySet()) {
            list.add(entry.getKey());
        }
    }

    public boolean exists(String tag) {
        return this.modData.containsKey(tag);
    }

    public KahluaTable getOrCreate(String tag) {
        KahluaTable table = this.get(tag);
        if (table == null) {
            table = this.create(tag);
        }
        return table;
    }

    public KahluaTable get(String tag) {
        return this.modData.get(tag);
    }

    public String create() {
        String uuid = UUID.randomUUID().toString();
        this.create(uuid);
        return uuid;
    }

    public KahluaTable create(String tag) {
        if (this.exists(tag)) {
            DebugLog.log("GlobalModData -> Cannot create table '" + tag + "', already exists. Returning null.");
            return null;
        }
        KahluaTable table = this.createModDataTable();
        this.modData.put(tag, table);
        return table;
    }

    public KahluaTable remove(String tag) {
        return this.modData.remove(tag);
    }

    public void add(String tag, KahluaTable table) {
        this.modData.put(tag, table);
    }

    public void transmit(String tag) {
        KahluaTable table = this.get(tag);
        if (table != null) {
            if (GameClient.client) {
                GlobalModDataPacket packet = new GlobalModDataPacket();
                packet.set(tag, table);
                ByteBufferWriter bbw = GameClient.connection.startPacket();
                PacketTypes.PacketType.GlobalModData.doPacket(bbw);
                try {
                    packet.write(bbw);
                    PacketTypes.PacketType.GlobalModData.send(GameClient.connection);
                }
                catch (RuntimeException e) {
                    GameClient.connection.cancelPacket();
                }
            } else if (GameServer.server) {
                try {
                    GlobalModDataPacket packet = new GlobalModDataPacket();
                    packet.set(tag, table);
                    for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                        UdpConnection c = GameServer.udpEngine.connections.get(n);
                        ByteBufferWriter bbw = c.startPacket();
                        PacketTypes.PacketType.GlobalModData.doPacket(bbw);
                        try {
                            packet.write(bbw);
                            PacketTypes.PacketType.GlobalModData.send(c);
                            continue;
                        }
                        catch (RuntimeException e) {
                            c.cancelPacket();
                        }
                    }
                }
                catch (Exception e) {
                    DebugLog.log(e.getMessage());
                }
            }
        } else {
            DebugLog.log("GlobalModData -> cannot transmit moddata not found: " + tag);
        }
    }

    public void request(String tag) {
        if (GameClient.client) {
            GlobalModDataRequestPacket packet = new GlobalModDataRequestPacket();
            packet.set(tag);
            ByteBufferWriter bbw = GameClient.connection.startPacket();
            PacketTypes.PacketType.GlobalModDataRequest.doPacket(bbw);
            try {
                packet.write(bbw);
                PacketTypes.PacketType.GlobalModDataRequest.send(GameClient.connection);
            }
            catch (RuntimeException e) {
                GameClient.connection.cancelPacket();
            }
        } else {
            DebugLog.log("GlobalModData -> can only request from Client.");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void receiveRequest(ByteBufferReader bb, IConnection requesterConnection) {
        String tag = bb.getUTF();
        KahluaTable table = this.get(tag);
        if (table == null) {
            DebugLog.log("GlobalModData -> received request for non-existing table, table: " + tag);
        }
        if (GameServer.server) {
            try {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    if (c != requesterConnection) continue;
                    ByteBufferWriter bbw = c.startPacket();
                    PacketTypes.PacketType.GlobalModData.doPacket(bbw);
                    ByteBuffer output = bbw.bb;
                    try {
                        GameWindow.WriteString(output, tag);
                        output.put(table != null ? (byte)1 : 0);
                        if (table == null) continue;
                        table.save(output);
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        c.cancelPacket();
                        continue;
                    }
                    finally {
                        PacketTypes.PacketType.GlobalModData.send(c);
                    }
                }
            }
            catch (Exception e) {
                DebugLog.log(e.getMessage());
            }
        }
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb) {
        if (bb == null) {
            lastBlockSize = 0x100000;
            return ByteBuffer.allocate(lastBlockSize);
        }
        lastBlockSize = bb.capacity() + 524288;
        ByteBuffer newBB = ByteBuffer.allocate(lastBlockSize);
        return newBB.put(bb.array(), 0, bb.position());
    }

    public void save() throws IOException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        try {
            DebugLog.log("Saving GlobalModData");
            ByteBuffer bb = ByteBuffer.allocate(lastBlockSize == -1 ? 0x100000 : lastBlockSize);
            bb.putInt(244);
            bb.putInt(this.modData.size());
            int elementPosition = 0;
            for (Map.Entry<String, KahluaTable> entry : this.modData.entrySet()) {
                if (bb.capacity() - bb.position() < 4) {
                    elementPosition = bb.position();
                    GlobalModData.ensureCapacity(bb);
                    bb.position(elementPosition);
                }
                int position1 = bb.position();
                bb.putInt(0);
                int position2 = bb.position();
                while (true) {
                    try {
                        elementPosition = bb.position();
                        GameWindow.WriteString(bb, entry.getKey());
                        entry.getValue().save(bb);
                    }
                    catch (BufferOverflowException e) {
                        bb = GlobalModData.ensureCapacity(bb);
                        bb.position(elementPosition);
                        continue;
                    }
                    break;
                }
                int position3 = bb.position();
                bb.position(position1);
                bb.putInt(position3 - position2);
                bb.position(position3);
            }
            bb.flip();
            File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.tmp"));
            FileOutputStream output = new FileOutputStream(path);
            output.getChannel().truncate(0L);
            output.write(bb.array(), 0, bb.limit());
            output.flush();
            output.close();
            File pathFinal = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.bin"));
            Files.copy(path, pathFinal);
            path.delete();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error saving GlobalModData.", e);
        }
    }

    public void load() throws IOException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("global_mod_data.bin");
        File path = new File(fileName);
        if (!path.exists()) {
            if (!WorldDictionary.isIsNewGame()) {
                // empty if block
            }
            return;
        }
        try (FileInputStream inStream = new FileInputStream(path);){
            DebugLog.DetailedInfo.trace("Loading GlobalModData:" + fileName);
            this.modData.clear();
            ByteBuffer bb = ByteBuffer.allocate((int)path.length());
            bb.clear();
            int len = inStream.read(bb.array());
            bb.limit(len);
            int worldVersion = bb.getInt();
            int size = bb.getInt();
            for (int i = 0; i < size; ++i) {
                int dataBlockSize = bb.getInt();
                String tag = GameWindow.ReadString(bb);
                KahluaTable table = this.createModDataTable();
                table.load(bb, worldVersion);
                this.modData.put(tag, table);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error loading GlobalModData.", e);
        }
    }
}

