/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import gnu.trove.map.hash.TIntLongHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import zombie.ZomboidFileSystem;
import zombie.core.Core;

public class ChunkChecksum {
    private static final TIntLongHashMap checksumCache = new TIntLongHashMap();
    private static final StringBuilder stringBuilder = new StringBuilder(128);
    private static final CRC32 crc32 = new CRC32();
    private static final byte[] bytes = new byte[1024];

    private static void noise(String s) {
        if (Core.debug) {
            // empty if block
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long getChecksum(int wx, int wy) throws IOException {
        long crc;
        TIntLongHashMap tIntLongHashMap = checksumCache;
        synchronized (tIntLongHashMap) {
            int key = wx + wy * 30 * 1000;
            if (checksumCache.containsKey(key)) {
                ChunkChecksum.noise(wx + "," + wy + " found in cache crc=" + checksumCache.get(key));
                crc = checksumCache.get(key);
            } else {
                stringBuilder.setLength(0);
                stringBuilder.append(ZomboidFileSystem.instance.getGameModeCacheDir());
                stringBuilder.append(File.separator);
                stringBuilder.append(Core.gameSaveWorld);
                stringBuilder.append(File.separator);
                stringBuilder.append("map");
                stringBuilder.append(File.separator);
                stringBuilder.append(wx);
                stringBuilder.append(File.separator);
                stringBuilder.append(wy);
                stringBuilder.append(".bin");
                crc = ChunkChecksum.createChecksum(stringBuilder.toString());
                checksumCache.put(key, crc);
                ChunkChecksum.noise(wx + "," + wy + " read from disk crc=" + crc);
            }
        }
        return crc;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long getChecksumIfExists(int wx, int wy) throws IOException {
        long crc = 0L;
        TIntLongHashMap tIntLongHashMap = checksumCache;
        synchronized (tIntLongHashMap) {
            int key = wx + wy * 30 * 1000;
            if (checksumCache.containsKey(key)) {
                crc = checksumCache.get(key);
            }
        }
        return crc;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void setChecksum(int wx, int wy, long crc) {
        TIntLongHashMap tIntLongHashMap = checksumCache;
        synchronized (tIntLongHashMap) {
            int key = wx + wy * 30 * 1000;
            checksumCache.put(key, crc);
            ChunkChecksum.noise(wx + "," + wy + " set crc=" + crc);
        }
    }

    public static long createChecksum(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            return 0L;
        }
        try (FileInputStream fis = new FileInputStream(filename);){
            int bytesRead;
            crc32.reset();
            while ((bytesRead = ((InputStream)fis).read(bytes)) != -1) {
                crc32.update(bytes, 0, bytesRead);
            }
            long l = crc32.getValue();
            return l;
        }
    }

    public static void Reset() {
        checksumCache.clear();
    }
}

