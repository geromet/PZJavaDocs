/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

public class BoxedStaticValues {
    static Double[] doubles = new Double[10000];
    static Double[] negdoubles = new Double[10000];
    static Double[] doublesh = new Double[10000];
    static Double[] negdoublesh = new Double[10000];

    public static Double toDouble(double d) {
        if (d > -10000.0 && d < 10000.0) {
            double abs = Math.abs(d);
            if ((double)((int)abs) == abs) {
                if (d < 0.0) {
                    return negdoubles[(int)(-d)];
                }
                return doubles[(int)d];
            }
            if ((double)((int)abs) == abs - 0.5) {
                if (d < 0.0) {
                    return negdoublesh[(int)(-d)];
                }
                return doublesh[(int)d];
            }
        }
        return d;
    }

    static {
        for (int x = 0; x < 10000; ++x) {
            BoxedStaticValues.doubles[x] = x;
            BoxedStaticValues.negdoubles[x] = -doubles[x].doubleValue();
            BoxedStaticValues.doublesh[x] = (double)x + 0.5;
            BoxedStaticValues.negdoublesh[x] = -(doubles[x] + 0.5);
        }
    }
}

