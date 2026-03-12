/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.worldMap.network;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.nio.ByteBuffer;
import java.util.Arrays;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.spnetwork.SinglePlayerServer;
import zombie.util.StringUtils;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;
import zombie.worldMap.symbols.SymbolSaveData;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

public final class WorldMapServer {
    public static final WorldMapServer instance = new WorldMapServer();
    public static final int SAVEFILE_VERSION = 2;
    private static final byte[] FILE_MAGIC = new byte[]{87, 77, 83, 89};
    public static final byte PACKET_AddMarker = 1;
    public static final byte PACKET_RemoveMarker = 2;
    public static final byte PACKET_AddSymbol = 3;
    public static final byte PACKET_RemoveSymbol = 4;
    public static final byte PACKET_ModifySymbol = 5;
    public static final byte PACKET_SetPrivateSymbol = 6;
    public static final byte PACKET_ModifySharing = 7;
    private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(0x100000);
    private static final ByteBufferWriter BYTE_BUFFER_WRITER = new ByteBufferWriter(BYTE_BUFFER);
    private final WorldMapSymbols symbols = new WorldMapSymbols();
    private int nextElementId = 1;
    private final TIntObjectHashMap<WorldMapBaseSymbol> symbolById = new TIntObjectHashMap();

    public void receive(ByteBufferReader bb, UdpConnection connection) throws IOException {
        byte packet = bb.getByte();
        switch (packet) {
            case 1: {
                this.receiveAddMarker(bb, connection);
                break;
            }
            case 2: {
                this.receiveRemoveMarker(bb, connection);
                break;
            }
            case 3: {
                this.receiveAddSymbol(bb, connection);
                break;
            }
            case 4: {
                this.receiveRemoveSymbol(bb, connection);
                break;
            }
            case 5: {
                this.receiveModifySymbol(bb, connection);
                break;
            }
            case 6: {
                this.receiveSetPrivateSymbol(bb, connection);
                break;
            }
            case 7: {
                this.receiveModifySharing(bb, connection);
            }
        }
    }

    private void receiveAddMarker(ByteBufferReader bb, UdpConnection connection) {
    }

    private void receiveRemoveMarker(ByteBufferReader bb, UdpConnection connection) {
    }

    private void receiveAddSymbol(ByteBufferReader bb, UdpConnection connection) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.load(bb.bb);
        WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
        networkInfo.load(bb.bb, 244, 2);
        if (!this.canClientModify(networkInfo, connection)) {
            return;
        }
        WorldMapSymbols.WorldMapSymbolType worldMapSymbolType = bb.getEnum(WorldMapSymbols.WorldMapSymbolType.class);
        int n = 0;
        WorldMapBaseSymbol symbol = switch (SwitchBootstraps.enumSwitch("enumSwitch", new Object[]{"Text", "Texture"}, (WorldMapSymbols.WorldMapSymbolType)worldMapSymbolType, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> new WorldMapTextSymbol(this.symbols);
            case 1 -> new WorldMapTextureSymbol(this.symbols);
            case -1 -> throw new IOException("unknown map symbol type");
        };
        symbol.load(bb.bb, saveData);
        networkInfo.setID(this.nextElementId++);
        symbol.setNetworkInfo(networkInfo);
        this.symbols.addSymbol(symbol);
        this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
        this.addSymbolOnClient(symbol);
    }

    private void receiveRemoveSymbol(ByteBufferReader bb, UdpConnection connection) {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol == null) {
            return;
        }
        if (!this.canClientModify(symbol.getNetworkInfo(), connection)) {
            return;
        }
        this.symbolById.remove(id);
        this.symbols.removeSymbol(symbol);
        this.removeSymbolOnClient(id);
        symbol.release();
    }

    private void receiveModifySymbol(ByteBufferReader bb, UdpConnection connection) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.load(bb.bb);
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol == null) {
            return;
        }
        if (!this.canClientModify(symbol.getNetworkInfo(), connection)) {
            return;
        }
        symbol.load(bb.bb, saveData);
        this.modifySymbolOnClient(symbol);
    }

    private void receiveSetPrivateSymbol(ByteBufferReader bb, UdpConnection connection) throws IOException {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol == null) {
            return;
        }
        if (!this.canClientModify(symbol.getNetworkInfo(), connection)) {
            return;
        }
        this.setPrivateSymbolOnClient(symbol);
        this.symbolById.remove(id);
        this.symbols.removeSymbol(symbol);
        symbol.release();
    }

    private void receiveModifySharing(ByteBufferReader bb, UdpConnection connection) throws IOException {
        int id = bb.getInt();
        WorldMapBaseSymbol symbol = this.symbolById.get(id);
        if (symbol == null) {
            return;
        }
        if (!this.canClientModify(symbol.getNetworkInfo(), connection)) {
            return;
        }
        symbol.getNetworkInfo().load(bb.bb, 244, 2);
        this.modifySymbolOnClient(symbol);
    }

    private boolean canClientModify(WorldMapSymbolNetworkInfo networkInfo, UdpConnection connection) {
        return StringUtils.equals(networkInfo.getAuthor(), connection.getUserName());
    }

    private void sendPacket(ByteBuffer bb) {
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

    public void addMarkerOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)1);
        this.sendPacket(BYTE_BUFFER);
    }

    public void removeMarkerOnClient(int id) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)2);
        this.sendPacket(BYTE_BUFFER);
    }

    public void addSymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)3);
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.save(b.bb, symbol);
        symbol.getNetworkInfo().save(b.bb);
        b.putEnum(symbol.getType());
        symbol.save(b.bb, saveData);
        this.sendPacket(BYTE_BUFFER);
    }

    public void removeSymbolOnClient(int id) {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)4);
        b.putInt(id);
        this.sendPacket(BYTE_BUFFER);
    }

    public void modifySymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)5);
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.save(b.bb, symbol);
        b.putInt(symbol.getNetworkInfo().getID());
        symbol.getNetworkInfo().save(b.bb);
        symbol.save(b.bb, saveData);
        this.sendPacket(BYTE_BUFFER);
    }

    public void setPrivateSymbolOnClient(WorldMapBaseSymbol symbol) throws IOException {
        BYTE_BUFFER.clear();
        ByteBufferWriter b = BYTE_BUFFER_WRITER;
        PacketTypes.PacketType.WorldMap.doPacket(b);
        b.putByte((byte)6);
        b.putInt(symbol.getNetworkInfo().getID());
        this.sendPacket(BYTE_BUFFER);
    }

    public void sendRequestData(ByteBuffer bb) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.save(bb, this.symbols);
        bb.putInt(this.symbols.getSymbolCount());
        for (int i = 0; i < this.symbols.getSymbolCount(); ++i) {
            WorldMapBaseSymbol symbol = this.symbols.getSymbolByIndex(i);
            symbol.getNetworkInfo().save(bb);
            bb.put((byte)symbol.getType().ordinal());
            symbol.save(bb, saveData);
        }
    }

    public void writeSavefile() {
        try {
            ByteBuffer out = SliceY.SliceBuffer;
            out.clear();
            out.put(FILE_MAGIC);
            out.putInt(244);
            out.putInt(2);
            this.writeSavefile(out);
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));
            try (FileOutputStream fos = new FileOutputStream(file);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);){
                bos.write(out.array(), 0, out.position());
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    private void writeSavefile(ByteBuffer bb) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.save(bb, this.symbols);
        bb.putInt(this.symbols.getSymbolCount());
        for (int i = 0; i < this.symbols.getSymbolCount(); ++i) {
            WorldMapBaseSymbol symbol = this.symbols.getSymbolByIndex(i);
            symbol.getNetworkInfo().save(bb);
            bb.put((byte)symbol.getType().ordinal());
            symbol.save(bb, saveData);
        }
    }

    public void readSavefile() {
        File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("servermap_symbols.bin"));
        try (FileInputStream fis2 = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis2);){
            ByteBuffer in = SliceY.SliceBuffer;
            in.clear();
            int numBytes = bis.read(in.array());
            in.limit(numBytes);
            byte[] magic = new byte[4];
            in.get(magic);
            if (!Arrays.equals(magic, FILE_MAGIC)) {
                throw new IOException(file.getAbsolutePath() + " does not appear to be servermap_symbols.bin");
            }
            int worldVersion = in.getInt();
            int symbolsVersion = in.getInt();
            this.readSavefile(in, worldVersion, symbolsVersion);
        }
        catch (FileNotFoundException fis2) {
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    private void readSavefile(ByteBuffer bb, int worldVersion, int symbolsVersion) throws IOException {
        SymbolSaveData saveData = new SymbolSaveData(worldVersion, symbolsVersion);
        saveData.load(bb);
        int count = bb.getInt();
        for (int i = 0; i < count; ++i) {
            WorldMapBaseSymbol symbol;
            WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
            networkInfo.load(bb, worldVersion, symbolsVersion);
            byte symbolType = bb.get();
            if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.ordinal()) {
                symbol = new WorldMapTextSymbol(this.symbols);
            } else if (symbolType == WorldMapSymbols.WorldMapSymbolType.Texture.ordinal()) {
                symbol = new WorldMapTextureSymbol(this.symbols);
            } else {
                throw new IOException("unknown map symbol type " + symbolType);
            }
            symbol.load(bb, saveData);
            networkInfo.setID(this.nextElementId++);
            symbol.setNetworkInfo(networkInfo);
            this.symbols.addSymbol(symbol);
            this.symbolById.put(symbol.getNetworkInfo().getID(), symbol);
        }
    }
}

