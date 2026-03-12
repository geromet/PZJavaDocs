/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import java.nio.FloatBuffer;
import org.lwjgl.util.ReadableColor;

public enum AlphaOp {
    PREMULTIPLY{

        @Override
        protected int calc(ReadableColor c, int alpha) {
            float alpha00 = (float)(c.getAlpha() * alpha) * 0.003921569f;
            float preMultAlpha00 = alpha00 * 0.003921569f;
            return (int)((float)c.getRed() * preMultAlpha00) << 0 | (int)((float)c.getGreen() * preMultAlpha00) << 8 | (int)((float)c.getBlue() * preMultAlpha00) << 16 | (int)alpha00 << 24;
        }
    }
    ,
    KEEP{

        @Override
        protected int calc(ReadableColor c, int alpha) {
            return c.getRed() << 0 | c.getGreen() << 8 | c.getBlue() << 16 | c.getAlpha() << 24;
        }
    }
    ,
    ZERO{

        @Override
        protected int calc(ReadableColor c, int alpha) {
            float alpha00 = (float)(c.getAlpha() * alpha) * 0.003921569f;
            float preMultAlpha00 = alpha00 * 0.003921569f;
            return (int)((float)c.getRed() * preMultAlpha00) << 0 | (int)((float)c.getGreen() * preMultAlpha00) << 8 | (int)((float)c.getBlue() * preMultAlpha00) << 16;
        }
    };

    private static final float PREMULT_ALPHA = 0.003921569f;

    public final void op(ReadableColor c, int alpha, FloatBuffer dest) {
        dest.put(Float.intBitsToFloat(this.calc(c, alpha)));
    }

    public final void op(int c, int alpha, FloatBuffer dest) {
        dest.put(Float.intBitsToFloat(c));
    }

    protected abstract int calc(ReadableColor var1, int var2);
}

