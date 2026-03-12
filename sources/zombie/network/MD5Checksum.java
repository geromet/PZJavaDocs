/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.CRC32;

public class MD5Checksum {
    public static long createChecksum(String filename) throws Exception {
        int bytesRead;
        File ddd = new File(filename);
        if (!ddd.exists()) {
            return 0L;
        }
        FileInputStream fis = new FileInputStream(filename);
        CRC32 crcMaker = new CRC32();
        byte[] buffer = new byte[1024];
        while ((bytesRead = ((InputStream)fis).read(buffer)) != -1) {
            crcMaker.update(buffer, 0, bytesRead);
        }
        long crc = crcMaker.getValue();
        ((InputStream)fis).close();
        return crc;
    }

    static void main(String[] args2) {
    }
}

