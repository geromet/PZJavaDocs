/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import zombie.core.opengl.GLState;

public class GLStateRenderThread {
    private static final GLState.C4BooleansValue temp4BooleansValue = new GLState.C4BooleansValue();
    private static final GLState.C3IntsValue temp3IntsValue = new GLState.C3IntsValue();
    private static final GLState.C4IntsValue temp4IntsValue = new GLState.C4IntsValue();
    private static final GLState.CIntFloatValue tempIntFloatValue = new GLState.CIntFloatValue();
    private static final GLState.CIntValue tempIntValue = new GLState.CIntValue();
    public static final CAlphaFunc AlphaFunc = new CAlphaFunc();
    public static final CAlphaTest AlphaTest = new CAlphaTest();
    public static final CBlend Blend = new CBlend();
    public static final CBlendFuncSeparate BlendFuncSeparate = new CBlendFuncSeparate();
    public static final CColorMask ColorMask = new CColorMask();
    public static final CDepthFunc DepthFunc = new CDepthFunc();
    public static final CDepthMask DepthMask = new CDepthMask();
    public static final CDepthTest DepthTest = new CDepthTest();
    public static final CScissorTest ScissorTest = new CScissorTest();
    public static final CStencilFunc StencilFunc = new CStencilFunc();
    public static final CStencilMask StencilMask = new CStencilMask();
    public static final CStencilOp StencilOp = new CStencilOp();
    public static final CStencilTest StencilTest = new CStencilTest();

    public static void startFrame() {
        AlphaFunc.setDirty();
        AlphaTest.setDirty();
        Blend.setDirty();
        BlendFuncSeparate.setDirty();
        ColorMask.setDirty();
        DepthFunc.setDirty();
        DepthMask.setDirty();
        DepthTest.setDirty();
        ScissorTest.setDirty();
        StencilFunc.setDirty();
        StencilMask.setDirty();
        StencilOp.setDirty();
        StencilTest.setDirty();
    }

    public static void restore() {
        AlphaFunc.restore();
        AlphaTest.restore();
        Blend.restore();
        BlendFuncSeparate.restore();
        ColorMask.restore();
        DepthFunc.restore();
        DepthMask.restore();
        DepthTest.restore();
        ScissorTest.restore();
        StencilFunc.restore();
        StencilMask.restore();
        StencilOp.restore();
        StencilTest.restore();
    }

    public static final class CAlphaFunc
    extends GLState.BaseIntFloat {
        public void set(int a, float b) {
            this.set(tempIntFloatValue.set(a, b));
        }

        @Override
        void Set(GLState.CIntFloatValue value) {
            GL11.glAlphaFunc(value.a, value.b);
        }
    }

    public static final class CAlphaTest
    extends GLState.BaseBoolean {
        @Override
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3008);
            } else {
                GL11.glDisable(3008);
            }
        }
    }

    public static final class CBlend
    extends GLState.BaseBoolean {
        @Override
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3042);
            } else {
                GL11.glDisable(3042);
            }
        }
    }

    public static final class CBlendFuncSeparate
    extends GLState.Base4Ints {
        public void set(int a, int b, int c, int d) {
            this.set(temp4IntsValue.set(a, b, c, d));
        }

        @Override
        public void restore() {
            this.Set((GLState.C4IntsValue)this.getCurrentValue());
        }

        @Override
        void Set(GLState.C4IntsValue value) {
            GL14.glBlendFuncSeparate(value.a, value.b, value.c, value.d);
        }
    }

    public static final class CColorMask
    extends GLState.Base4Booleans {
        public void set(boolean a, boolean b, boolean c, boolean d) {
            this.set(temp4BooleansValue.set(a, b, c, d));
        }

        @Override
        void Set(GLState.C4BooleansValue value) {
            GL11.glColorMask(value.a, value.b, value.c, value.d);
        }
    }

    public static final class CDepthFunc
    extends GLState.BaseInt {
        public CDepthFunc() {
            ((GLState.CIntValue)this.currentValue).value = 513;
        }

        @Override
        public void set(int a) {
            this.set(tempIntValue.set(a));
        }

        @Override
        void Set(GLState.CIntValue value) {
            GL11.glDepthFunc(value.value);
        }
    }

    public static final class CDepthMask
    extends GLState.BaseBoolean {
        @Override
        public void set(boolean b) {
            this.set(b ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            GL11.glDepthMask(value.value);
        }
    }

    public static final class CDepthTest
    extends GLState.BaseBoolean {
        public CDepthTest() {
            ((GLState.CBooleanValue)this.currentValue).value = false;
        }

        @Override
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(2929);
            } else {
                GL11.glDisable(2929);
            }
        }
    }

    public static final class CScissorTest
    extends GLState.BaseBoolean {
        public CScissorTest() {
            ((GLState.CBooleanValue)this.currentValue).value = false;
        }

        @Override
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(3089);
            } else {
                GL11.glDisable(3089);
            }
        }
    }

    public static final class CStencilFunc
    extends GLState.Base3Ints {
        public CStencilFunc() {
            ((GLState.C3IntsValue)this.currentValue).a = 519;
            ((GLState.C3IntsValue)this.currentValue).b = 0;
            ((GLState.C3IntsValue)this.currentValue).c = 0;
        }

        public void set(int a, int b, int c) {
            this.set(temp3IntsValue.set(a, b, c));
        }

        @Override
        void Set(GLState.C3IntsValue value) {
            GL11.glStencilFunc(value.a, value.b, value.c);
        }
    }

    public static final class CStencilMask
    extends GLState.BaseInt {
        public CStencilMask() {
            ((GLState.CIntValue)this.currentValue).value = -1;
        }

        @Override
        public void set(int a) {
            this.set(tempIntValue.set(a));
        }

        @Override
        void Set(GLState.CIntValue value) {
            GL11.glStencilMask(value.value);
        }
    }

    public static final class CStencilOp
    extends GLState.Base3Ints {
        public void set(int a, int b, int c) {
            this.set(temp3IntsValue.set(a, b, c));
        }

        @Override
        void Set(GLState.C3IntsValue value) {
            GL11.glStencilOp(value.a, value.b, value.c);
        }
    }

    public static final class CStencilTest
    extends GLState.BaseBoolean {
        @Override
        public void set(boolean a) {
            this.set(a ? GLState.CBooleanValue.TRUE : GLState.CBooleanValue.FALSE);
        }

        @Override
        void Set(GLState.CBooleanValue value) {
            if (value.value) {
                GL11.glEnable(2960);
            } else {
                GL11.glDisable(2960);
            }
        }
    }
}

