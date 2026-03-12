/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import zombie.core.SpriteRenderer;
import zombie.core.opengl.IOpenGLState;
import zombie.util.Type;

public final class GLState {
    public static final CAlphaFunc AlphaFunc = new CAlphaFunc();
    public static final CAlphaTest AlphaTest = new CAlphaTest();
    public static final CBlend Blend = new CBlend();
    public static final CBlendFunc BlendFunc = new CBlendFunc();
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
        BlendFunc.setDirty();
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

    public static final class CAlphaFunc
    extends BaseIntFloat {
        @Override
        void Set(CIntFloatValue value) {
            SpriteRenderer.instance.glAlphaFunc(value.a, value.b);
        }
    }

    public static final class CAlphaTest
    extends BaseBoolean {
        @Override
        void Set(CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3008);
            } else {
                SpriteRenderer.instance.glDisable(3008);
            }
        }
    }

    public static final class CBlend
    extends BaseBoolean {
        CBlend() {
            ((CBooleanValue)this.currentValue).value = true;
        }

        @Override
        void Set(CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3042);
            } else {
                SpriteRenderer.instance.glDisable(3042);
            }
        }
    }

    public static final class CBlendFunc
    extends Base2Ints {
        @Override
        void Set(C2IntsValue value) {
            SpriteRenderer.instance.glBlendFunc(value.a, value.b);
        }
    }

    public static final class CBlendFuncSeparate
    extends Base4Ints {
        @Override
        void Set(C4IntsValue value) {
            SpriteRenderer.instance.glBlendFuncSeparate(value.a, value.b, value.c, value.d);
        }
    }

    public static final class CColorMask
    extends Base4Booleans {
        @Override
        void Set(C4BooleansValue value) {
            SpriteRenderer.instance.glColorMask(value.a ? 1 : 0, value.b ? 1 : 0, value.c ? 1 : 0, value.d ? 1 : 0);
        }
    }

    public static final class CDepthFunc
    extends BaseInt {
        CDepthFunc() {
            ((CIntValue)this.currentValue).value = 513;
        }

        @Override
        void Set(CIntValue value) {
            SpriteRenderer.instance.glDepthFunc(value.value);
        }
    }

    public static final class CDepthMask
    extends BaseBoolean {
        @Override
        void Set(CBooleanValue value) {
            SpriteRenderer.instance.glDepthMask(value.value);
        }
    }

    public static final class CDepthTest
    extends BaseBoolean {
        CDepthTest() {
            ((CBooleanValue)this.currentValue).value = false;
        }

        @Override
        void Set(CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(2929);
            } else {
                SpriteRenderer.instance.glDisable(2929);
            }
        }
    }

    public static final class CScissorTest
    extends BaseBoolean {
        CScissorTest() {
            ((CBooleanValue)this.currentValue).value = false;
        }

        @Override
        void Set(CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(3089);
            } else {
                SpriteRenderer.instance.glDisable(3089);
            }
        }
    }

    public static final class CStencilFunc
    extends Base3Ints {
        @Override
        void Set(C3IntsValue value) {
            SpriteRenderer.instance.glStencilFunc(value.a, value.b, value.c);
        }
    }

    public static final class CStencilMask
    extends BaseInt {
        CStencilMask() {
            ((CIntValue)this.currentValue).value = 255;
        }

        @Override
        void Set(CIntValue value) {
            SpriteRenderer.instance.glStencilMask(value.value);
        }
    }

    public static final class CStencilOp
    extends Base3Ints {
        @Override
        void Set(C3IntsValue value) {
            SpriteRenderer.instance.glStencilOp(value.a, value.b, value.c);
        }
    }

    public static final class CStencilTest
    extends BaseBoolean {
        @Override
        void Set(CBooleanValue value) {
            if (value.value) {
                SpriteRenderer.instance.glEnable(2960);
            } else {
                SpriteRenderer.instance.glDisable(2960);
            }
        }
    }

    public static abstract class Base4Ints
    extends IOpenGLState<C4IntsValue> {
        @Override
        C4IntsValue defaultValue() {
            return new C4IntsValue();
        }
    }

    public static abstract class Base3Ints
    extends IOpenGLState<C3IntsValue> {
        @Override
        C3IntsValue defaultValue() {
            return new C3IntsValue();
        }
    }

    public static abstract class Base2Ints
    extends IOpenGLState<C2IntsValue> {
        @Override
        C2IntsValue defaultValue() {
            return new C2IntsValue();
        }
    }

    public static abstract class BaseInt
    extends IOpenGLState<CIntValue> {
        @Override
        CIntValue defaultValue() {
            return new CIntValue();
        }
    }

    public static abstract class BaseIntFloat
    extends IOpenGLState<CIntFloatValue> {
        @Override
        CIntFloatValue defaultValue() {
            return new CIntFloatValue();
        }
    }

    public static abstract class Base4Booleans
    extends IOpenGLState<C4BooleansValue> {
        @Override
        C4BooleansValue defaultValue() {
            return new C4BooleansValue();
        }
    }

    public static abstract class BaseBoolean
    extends IOpenGLState<CBooleanValue> {
        @Override
        CBooleanValue defaultValue() {
            return new CBooleanValue(true);
        }
    }

    public static final class CIntFloatValue
    implements IOpenGLState.Value {
        int a;
        float b;

        public CIntFloatValue set(int a, float b) {
            this.a = a;
            this.b = b;
            return this;
        }

        public boolean equals(Object other) {
            CIntFloatValue rhs = Type.tryCastTo(other, CIntFloatValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            CIntFloatValue rhs = (CIntFloatValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            return this;
        }
    }

    public static final class C4IntsValue
    implements IOpenGLState.Value {
        int a;
        int b;
        int c;
        int d;

        public C4IntsValue set(int a, int b, int c, int d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            return this;
        }

        public boolean equals(Object other) {
            C4IntsValue rhs = Type.tryCastTo(other, C4IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c && rhs.d == this.d;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            C4IntsValue rhs = (C4IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            this.d = rhs.d;
            return this;
        }
    }

    public static final class C3IntsValue
    implements IOpenGLState.Value {
        int a;
        int b;
        int c;

        public C3IntsValue set(int a, int b, int c) {
            this.a = a;
            this.b = b;
            this.c = c;
            return this;
        }

        public boolean equals(Object other) {
            C3IntsValue rhs = Type.tryCastTo(other, C3IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            C3IntsValue rhs = (C3IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            return this;
        }
    }

    public static final class C2IntsValue
    implements IOpenGLState.Value {
        int a;
        int b;

        public C2IntsValue set(int a, int b) {
            this.a = a;
            this.b = b;
            return this;
        }

        public boolean equals(Object other) {
            C2IntsValue rhs = Type.tryCastTo(other, C2IntsValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            C2IntsValue rhs = (C2IntsValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            return this;
        }
    }

    public static class CIntValue
    implements IOpenGLState.Value {
        int value;

        public CIntValue set(int a) {
            this.value = a;
            return this;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object other) {
            if (!(other instanceof CIntValue)) return false;
            CIntValue cIntValue = (CIntValue)other;
            if (cIntValue.value != this.value) return false;
            return true;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            this.value = ((CIntValue)other).value;
            return this;
        }
    }

    public static final class C4BooleansValue
    implements IOpenGLState.Value {
        boolean a;
        boolean b;
        boolean c;
        boolean d;

        public C4BooleansValue set(boolean a, boolean b, boolean c, boolean d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            return this;
        }

        public boolean equals(Object other) {
            C4BooleansValue rhs = Type.tryCastTo(other, C4BooleansValue.class);
            return rhs != null && rhs.a == this.a && rhs.b == this.b && rhs.c == this.c && rhs.d == this.d;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            C4BooleansValue rhs = (C4BooleansValue)other;
            this.a = rhs.a;
            this.b = rhs.b;
            this.c = rhs.c;
            this.d = rhs.d;
            return this;
        }
    }

    public static class CBooleanValue
    implements IOpenGLState.Value {
        public static final CBooleanValue TRUE = new CBooleanValue(true);
        public static final CBooleanValue FALSE = new CBooleanValue(false);
        boolean value;

        CBooleanValue(boolean value) {
            this.value = value;
        }

        /*
         * Enabled force condition propagation
         * Lifted jumps to return sites
         */
        public boolean equals(Object other) {
            if (!(other instanceof CBooleanValue)) return false;
            CBooleanValue cBooleanValue = (CBooleanValue)other;
            if (cBooleanValue.value != this.value) return false;
            return true;
        }

        @Override
        public IOpenGLState.Value set(IOpenGLState.Value other) {
            this.value = ((CBooleanValue)other).value;
            return this;
        }
    }
}

