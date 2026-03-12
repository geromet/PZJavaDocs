/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

import zombie.core.Core;
import zombie.core.math.PZMath;

public class Bits {
    public static final boolean ENABLED = true;
    public static final int BIT_0 = 0;
    public static final int BIT_1 = 1;
    public static final int BIT_2 = 2;
    public static final int BIT_3 = 4;
    public static final int BIT_4 = 8;
    public static final int BIT_5 = 16;
    public static final int BIT_6 = 32;
    public static final int BIT_7 = 64;
    public static final int BIT_BYTE_MAX = 64;
    public static final int BIT_8 = 128;
    public static final int BIT_9 = 256;
    public static final int BIT_10 = 512;
    public static final int BIT_11 = 1024;
    public static final int BIT_12 = 2048;
    public static final int BIT_13 = 4096;
    public static final int BIT_14 = 8192;
    public static final int BIT_15 = 16384;
    public static final int BIT_SHORT_MAX = 16384;
    public static final int BIT_16 = 32768;
    public static final int BIT_17 = 65536;
    public static final int BIT_18 = 131072;
    public static final int BIT_19 = 262144;
    public static final int BIT_20 = 524288;
    public static final int BIT_21 = 0x100000;
    public static final int BIT_22 = 0x200000;
    public static final int BIT_23 = 0x400000;
    public static final int BIT_24 = 0x800000;
    public static final int BIT_25 = 0x1000000;
    public static final int BIT_26 = 0x2000000;
    public static final int BIT_27 = 0x4000000;
    public static final int BIT_28 = 0x8000000;
    public static final int BIT_29 = 0x10000000;
    public static final int BIT_30 = 0x20000000;
    public static final int BIT_31 = 0x40000000;
    public static final int BIT_INT_MAX = 0x40000000;
    public static final long BIT_32 = 0x80000000L;
    public static final long BIT_33 = 0x100000000L;
    public static final long BIT_34 = 0x200000000L;
    public static final long BIT_35 = 0x400000000L;
    public static final long BIT_36 = 0x800000000L;
    public static final long BIT_37 = 0x1000000000L;
    public static final long BIT_38 = 0x2000000000L;
    public static final long BIT_39 = 0x4000000000L;
    public static final long BIT_40 = 0x8000000000L;
    public static final long BIT_41 = 0x10000000000L;
    public static final long BIT_42 = 0x20000000000L;
    public static final long BIT_43 = 0x40000000000L;
    public static final long BIT_44 = 0x80000000000L;
    public static final long BIT_45 = 0x100000000000L;
    public static final long BIT_46 = 0x200000000000L;
    public static final long BIT_47 = 0x400000000000L;
    public static final long BIT_48 = 0x800000000000L;
    public static final long BIT_49 = 0x1000000000000L;
    public static final long BIT_50 = 0x2000000000000L;
    public static final long BIT_51 = 0x4000000000000L;
    public static final long BIT_52 = 0x8000000000000L;
    public static final long BIT_53 = 0x10000000000000L;
    public static final long BIT_54 = 0x20000000000000L;
    public static final long BIT_55 = 0x40000000000000L;
    public static final long BIT_56 = 0x80000000000000L;
    public static final long BIT_57 = 0x100000000000000L;
    public static final long BIT_58 = 0x200000000000000L;
    public static final long BIT_59 = 0x400000000000000L;
    public static final long BIT_60 = 0x800000000000000L;
    public static final long BIT_61 = 0x1000000000000000L;
    public static final long BIT_62 = 0x2000000000000000L;
    public static final long BIT_63 = 0x4000000000000000L;
    public static final long BIT_LONG_MAX = 0x4000000000000000L;
    private static final StringBuilder sb = new StringBuilder();

    public static byte packFloatUnitToByte(float f) {
        if (f < 0.0f || f > 1.0f) {
            if (Core.debug) {
                throw new RuntimeException("UtilsIO Cannot pack float units out of the range 0.0 to 1.0");
            }
            f = PZMath.clamp(f, 0.0f, 1.0f);
        }
        return (byte)(f * 255.0f + -128.0f);
    }

    public static float unpackByteToFloatUnit(byte b) {
        return (float)(b - -128) / 255.0f;
    }

    public static byte addFlags(byte value, int flags) {
        if (flags < 0 || flags > 64) {
            throw new RuntimeException("Cannot add flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        return (byte)(value | (byte)flags);
    }

    public static byte addFlags(byte value, long flags) {
        if (flags < 0L || flags > 64L) {
            throw new RuntimeException("Cannot add flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        return (byte)(value | (byte)flags);
    }

    public static short addFlags(short value, int flags) {
        if (flags < 0 || flags > 16384) {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return (short)(value | (short)flags);
    }

    public static short addFlags(short value, long flags) {
        if (flags < 0L || flags > 16384L) {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return (short)(value | (short)flags);
    }

    public static int addFlags(int value, int flags) {
        if (flags < 0 || flags > 0x40000000) {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return value | flags;
    }

    public static int addFlags(int value, long flags) {
        if (flags < 0L || flags > 0x40000000L) {
            throw new RuntimeException("Cannot add flags, exceeding integer bounds or negative number flags. (" + flags + ")");
        }
        return value | (int)flags;
    }

    public static long addFlags(long value, int flags) {
        if (flags < 0 || (long)flags > 0x4000000000000000L) {
            throw new RuntimeException("Cannot add flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
        return value | (long)flags;
    }

    public static long addFlags(long value, long flags) {
        if (flags < 0L || flags > 0x4000000000000000L) {
            throw new RuntimeException("Cannot add flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
        return value | flags;
    }

    public static byte removeFlags(byte value, int flags) {
        if (flags < 0 || flags > 64) {
            throw new RuntimeException("Cannot remove flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        return (byte)(value & (byte)(~flags));
    }

    public static byte removeFlags(byte value, long flags) {
        if (flags < 0L || flags > 64L) {
            throw new RuntimeException("Cannot remove flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        return (byte)(value & (byte)(flags ^ 0xFFFFFFFFFFFFFFFFL));
    }

    public static short removeFlags(short value, int flags) {
        if (flags < 0 || flags > 16384) {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return (short)(value & (short)(~flags));
    }

    public static short removeFlags(short value, long flags) {
        if (flags < 0L || flags > 16384L) {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return (short)(value & (short)(flags ^ 0xFFFFFFFFFFFFFFFFL));
    }

    public static int removeFlags(int value, int flags) {
        if (flags < 0 || flags > 0x40000000) {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
        return value & ~flags;
    }

    public static int removeFlags(int value, long flags) {
        if (flags < 0L || flags > 0x40000000L) {
            throw new RuntimeException("Cannot remove flags, exceeding integer bounds or negative number flags. (" + flags + ")");
        }
        return value & (int)(flags ^ 0xFFFFFFFFFFFFFFFFL);
    }

    public static long removeFlags(long value, int flags) {
        if (flags < 0 || (long)flags > 0x4000000000000000L) {
            throw new RuntimeException("Cannot remove flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
        return value & (long)(~flags);
    }

    public static long removeFlags(long value, long flags) {
        if (flags < 0L || flags > 0x4000000000000000L) {
            throw new RuntimeException("Cannot remove flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
        return value & (flags ^ 0xFFFFFFFFFFFFFFFFL);
    }

    public static boolean hasFlags(byte value, int flags) {
        return Bits.checkFlags(value, flags, 64, CompareOption.ContainsAll);
    }

    public static boolean hasFlags(byte value, long flags) {
        return Bits.checkFlags((long)value, flags, 64L, CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(byte value, int flags) {
        return Bits.checkFlags(value, flags, 64, CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(byte value, long flags) {
        return Bits.checkFlags((long)value, flags, 64L, CompareOption.HasEither);
    }

    public static boolean notHasFlags(byte value, int flags) {
        return Bits.checkFlags(value, flags, 64, CompareOption.NotHas);
    }

    public static boolean notHasFlags(byte value, long flags) {
        return Bits.checkFlags((long)value, flags, 64L, CompareOption.NotHas);
    }

    public static boolean hasFlags(short value, int flags) {
        return Bits.checkFlags(value, flags, 16384, CompareOption.ContainsAll);
    }

    public static boolean hasFlags(short value, long flags) {
        return Bits.checkFlags((long)value, flags, 16384L, CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(short value, int flags) {
        return Bits.checkFlags(value, flags, 16384, CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(short value, long flags) {
        return Bits.checkFlags((long)value, flags, 16384L, CompareOption.HasEither);
    }

    public static boolean notHasFlags(short value, int flags) {
        return Bits.checkFlags(value, flags, 16384, CompareOption.NotHas);
    }

    public static boolean notHasFlags(short value, long flags) {
        return Bits.checkFlags((long)value, flags, 16384L, CompareOption.NotHas);
    }

    public static boolean hasFlags(int value, int flags) {
        return Bits.checkFlags(value, flags, 0x40000000, CompareOption.ContainsAll);
    }

    public static boolean hasFlags(int value, long flags) {
        return Bits.checkFlags((long)value, flags, 0x40000000L, CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(int value, int flags) {
        return Bits.checkFlags(value, flags, 0x40000000, CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(int value, long flags) {
        return Bits.checkFlags((long)value, flags, 0x40000000L, CompareOption.HasEither);
    }

    public static boolean notHasFlags(int value, int flags) {
        return Bits.checkFlags(value, flags, 0x40000000, CompareOption.NotHas);
    }

    public static boolean notHasFlags(int value, long flags) {
        return Bits.checkFlags((long)value, flags, 0x40000000L, CompareOption.NotHas);
    }

    public static boolean hasFlags(long value, int flags) {
        return Bits.checkFlags(value, (long)flags, 0x4000000000000000L, CompareOption.ContainsAll);
    }

    public static boolean hasFlags(long value, long flags) {
        return Bits.checkFlags(value, flags, 0x4000000000000000L, CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(long value, int flags) {
        return Bits.checkFlags(value, (long)flags, 0x4000000000000000L, CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(long value, long flags) {
        return Bits.checkFlags(value, flags, 0x4000000000000000L, CompareOption.HasEither);
    }

    public static boolean notHasFlags(long value, int flags) {
        return Bits.checkFlags(value, (long)flags, 0x4000000000000000L, CompareOption.NotHas);
    }

    public static boolean notHasFlags(long value, long flags) {
        return Bits.checkFlags(value, flags, 0x4000000000000000L, CompareOption.NotHas);
    }

    public static boolean checkFlags(int value, int flags, int limit, CompareOption option) {
        if (flags < 0 || flags > limit) {
            throw new RuntimeException("Cannot check for flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        if (option == CompareOption.ContainsAll) {
            return (value & flags) == flags;
        }
        if (option == CompareOption.HasEither) {
            return (value & flags) != 0;
        }
        if (option == CompareOption.NotHas) {
            return (value & flags) == 0;
        }
        throw new RuntimeException("No valid compare option.");
    }

    public static boolean checkFlags(long value, long flags, long limit, CompareOption option) {
        if (flags < 0L || flags > limit) {
            throw new RuntimeException("Cannot check for flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
        if (option == CompareOption.ContainsAll) {
            return (value & flags) == flags;
        }
        if (option == CompareOption.HasEither) {
            return (value & flags) != 0L;
        }
        if (option == CompareOption.NotHas) {
            return (value & flags) == 0L;
        }
        throw new RuntimeException("No valid compare option.");
    }

    public static int getLen(byte b) {
        return 1;
    }

    public static int getLen(short s) {
        return 2;
    }

    public static int getLen(int i) {
        return 4;
    }

    public static int getLen(long l) {
        return 8;
    }

    private static void clearStringBuilder() {
        if (!sb.isEmpty()) {
            sb.delete(0, sb.length());
        }
    }

    public static String getBitsString(byte bits) {
        return Bits.getBitsString(bits, 8);
    }

    public static String getBitsString(short bits) {
        return Bits.getBitsString(bits, 16);
    }

    public static String getBitsString(int bits) {
        return Bits.getBitsString(bits, 32);
    }

    public static String getBitsString(long bits) {
        return Bits.getBitsString(bits, 64);
    }

    private static String getBitsString(long bits, int len) {
        Bits.clearStringBuilder();
        if (bits != 0L) {
            sb.append("Bits(" + (len - 1) + "): ");
            long bitval = 1L;
            for (int i = 1; i < len; ++i) {
                sb.append("[" + i + "]");
                if ((bits & bitval) == bitval) {
                    sb.append("1");
                } else {
                    sb.append("0");
                }
                if (i < len - 1) {
                    sb.append(" ");
                }
                bitval *= 2L;
            }
        } else {
            sb.append("No bits saved, 0x0.");
        }
        return sb.toString();
    }

    public static enum CompareOption {
        ContainsAll,
        HasEither,
        NotHas;

    }
}

