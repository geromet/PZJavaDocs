/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.hash;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class PZHash {
    private static final int FNV1_32_INIT = -2128831035;
    private static final int FNV1_PRIME_32 = 16777619;
    private static final long FNV1_64_INIT = -3750763034362895579L;
    private static final long FNV1_PRIME_64 = 1099511628211L;

    public static long sha256_64(String input) {
        HashCode hashCode = Hashing.sha256().hashString(input, StandardCharsets.UTF_8);
        return hashCode.asLong();
    }

    public static int fnv_32(String text) {
        byte[] data = text.getBytes();
        return PZHash.fnv_32(data, data.length);
    }

    public static int fnv_32(byte[] data) {
        return PZHash.fnv_32(data, data.length);
    }

    public static int fnv_32(byte[] data, int length) {
        int hash = -2128831035;
        for (int i = 0; i < length; ++i) {
            hash ^= data[i] & 0xFF;
            hash *= 16777619;
        }
        return hash;
    }

    public static int fnv_32_init() {
        return -2128831035;
    }

    public static int fnv_32_hash(int hash, int data) {
        hash ^= data;
        return hash *= 16777619;
    }

    public static long fnv_64(String text) {
        byte[] data = text.getBytes();
        return PZHash.fnv_64(data, data.length);
    }

    public static long fnv_64(byte[] data) {
        return PZHash.fnv_64(data, data.length);
    }

    public static long fnv_64(byte[] data, int length) {
        long hash = -3750763034362895579L;
        for (int i = 0; i < length; ++i) {
            hash ^= (long)(data[i] & 0xFF);
            hash *= 1099511628211L;
        }
        return hash;
    }

    public static int murmur_32(String text) {
        byte[] bytes = text.getBytes();
        return PZHash.murmur_32(bytes, bytes.length);
    }

    public static int murmur_32(byte[] data, int length) {
        return PZHash.murmur_32(data, length, 0);
    }

    public static int murmur_32(byte[] data, int length, int seed) {
        int m = 1540483477;
        int r = 24;
        int h = seed ^ length;
        int len4 = length >> 2;
        for (int i = 0; i < len4; ++i) {
            int i4 = i << 2;
            int k = data[i4 + 3];
            k <<= 8;
            k |= data[i4 + 2] & 0xFF;
            k <<= 8;
            k |= data[i4 + 1] & 0xFF;
            k <<= 8;
            k |= data[i4 + 0] & 0xFF;
            k *= 1540483477;
            k ^= k >>> 24;
            h *= 1540483477;
            h ^= (k *= 1540483477);
        }
        int lenM = len4 << 2;
        int left = length - lenM;
        if (left != 0) {
            if (left >= 3) {
                h ^= data[length - 3] << 16;
            }
            if (left >= 2) {
                h ^= data[length - 2] << 8;
            }
            if (left >= 1) {
                h ^= data[length - 1];
            }
            h *= 1540483477;
        }
        h ^= h >>> 13;
        h *= 1540483477;
        h ^= h >>> 15;
        return h;
    }

    public static long murmur_64(String text) {
        byte[] bytes = text.getBytes();
        return PZHash.murmur_64(bytes, bytes.length);
    }

    public static long murmur_64(byte[] data, int length) {
        return PZHash.murmur_64(data, length, -512093083);
    }

    public static long murmur_64(byte[] data, int length, int seed) {
        long m = -4132994306676758123L;
        int r = 47;
        long h = (long)seed & 0xFFFFFFFFL ^ (long)length * -4132994306676758123L;
        int length8 = length / 8;
        for (int i = 0; i < length8; ++i) {
            int i8 = i * 8;
            long k = ((long)data[i8 + 0] & 0xFFL) + (((long)data[i8 + 1] & 0xFFL) << 8) + (((long)data[i8 + 2] & 0xFFL) << 16) + (((long)data[i8 + 3] & 0xFFL) << 24) + (((long)data[i8 + 4] & 0xFFL) << 32) + (((long)data[i8 + 5] & 0xFFL) << 40) + (((long)data[i8 + 6] & 0xFFL) << 48) + (((long)data[i8 + 7] & 0xFFL) << 56);
            k *= -4132994306676758123L;
            k ^= k >>> 47;
            h ^= (k *= -4132994306676758123L);
            h *= -4132994306676758123L;
        }
        switch (length % 8) {
            case 7: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 6] & 0xFF) << 48;
            }
            case 6: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 5] & 0xFF) << 40;
            }
            case 5: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 4] & 0xFF) << 32;
            }
            case 4: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 3] & 0xFF) << 24;
            }
            case 3: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 2] & 0xFF) << 16;
            }
            case 2: {
                h ^= (long)(data[(length & 0xFFFFFFF8) + 1] & 0xFF) << 8;
            }
            case 1: {
                h ^= (long)(data[length & 0xFFFFFFF8] & 0xFF);
                h *= -4132994306676758123L;
            }
        }
        h ^= h >>> 47;
        h *= -4132994306676758123L;
        h ^= h >>> 47;
        return h;
    }
}

