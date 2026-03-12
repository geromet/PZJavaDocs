/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import org.lwjgl.opengl.GL20;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderProgram;
import zombie.core.textures.TextureDraw;

public final class ShaderUniformSetter
extends TextureDraw.GenericDrawer {
    Type type;
    int location;
    float f1;
    float f2;
    float f3;
    float f4;
    int i1;
    int i2;
    int i3;
    int i4;
    ShaderUniformSetter next;
    static ShaderUniformSetter pool;

    private ShaderUniformSetter set(Type type, int location) {
        this.type = type;
        this.location = location;
        this.next = null;
        return this;
    }

    public ShaderUniformSetter setNext(ShaderUniformSetter next) {
        this.next = next;
        return next;
    }

    @Override
    public void render() {
    }

    @Override
    public void postRender() {
        ShaderUniformSetter e = this;
        while (e != null) {
            ShaderUniformSetter next1 = e.next;
            ShaderUniformSetter.release(e);
            e = next1;
        }
    }

    public void invokeAll() {
        ShaderUniformSetter e = this;
        while (e != null) {
            e.invoke();
            e = e.next;
        }
    }

    private void invoke() {
        switch (this.type.ordinal()) {
            case 1: {
                GL20.glUniform1f(this.location, this.f1);
                break;
            }
            case 2: {
                GL20.glUniform2f(this.location, this.f1, this.f2);
                break;
            }
            case 3: {
                GL20.glUniform3f(this.location, this.f1, this.f2, this.f3);
                break;
            }
            case 4: {
                GL20.glUniform4f(this.location, this.f1, this.f2, this.f3, this.f4);
                break;
            }
            case 5: {
                GL20.glUniform1i(this.location, this.i1);
                break;
            }
            case 6: {
                GL20.glUniform2i(this.location, this.i1, this.i2);
                break;
            }
            case 7: {
                GL20.glUniform3i(this.location, this.i1, this.i2, this.i3);
                break;
            }
            case 8: {
                GL20.glUniform4i(this.location, this.i1, this.i2, this.i3, this.i4);
            }
        }
    }

    public static ShaderUniformSetter uniform1f(int location, float f1) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform1f, location);
        e.f1 = f1;
        return e;
    }

    public static ShaderUniformSetter uniform2f(int location, float f1, float f2) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform2f, location);
        e.f1 = f1;
        e.f2 = f2;
        return e;
    }

    public static ShaderUniformSetter uniform3f(int location, float f1, float f2, float f3) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform3f, location);
        e.f1 = f1;
        e.f2 = f2;
        e.f3 = f3;
        return e;
    }

    public static ShaderUniformSetter uniform4f(int location, float f1, float f2, float f3, float f4) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform4f, location);
        e.f1 = f1;
        e.f2 = f2;
        e.f3 = f3;
        e.f4 = f4;
        return e;
    }

    private static ShaderProgram.Uniform getShaderUniform(Shader shader, String uniformName, int uniformType) {
        if (shader == null) {
            return null;
        }
        ShaderProgram program = shader.getProgram();
        if (program == null) {
            return null;
        }
        return program.getUniform(uniformName, uniformType, false);
    }

    public static ShaderUniformSetter uniform1f(Shader shader, String location, float f1) {
        ShaderProgram.Uniform u = ShaderUniformSetter.getShaderUniform(shader, location, 5126);
        if (u == null) {
            return ShaderUniformSetter.alloc().set(Type.NIL, -1);
        }
        return ShaderUniformSetter.uniform1f(u.loc, f1);
    }

    public static ShaderUniformSetter uniform1i(int location, int i1) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform1i, location);
        e.i1 = i1;
        return e;
    }

    public static ShaderUniformSetter uniform2i(int location, int i1, int i2) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform2i, location);
        e.i1 = i1;
        e.i2 = i2;
        return e;
    }

    public static ShaderUniformSetter uniform3i(int location, int i1, int i2, int i3) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform3i, location);
        e.i1 = i1;
        e.i2 = i2;
        e.i3 = i3;
        return e;
    }

    public static ShaderUniformSetter uniform4i(int location, int i1, int i2, int i3, int i4) {
        ShaderUniformSetter e = ShaderUniformSetter.alloc().set(Type.Uniform4i, location);
        e.i1 = i1;
        e.i2 = i2;
        e.i3 = i3;
        e.i4 = i4;
        return e;
    }

    public static ShaderUniformSetter uniform1i(Shader shader, String location, int i1) {
        ShaderProgram.Uniform u = ShaderUniformSetter.getShaderUniform(shader, location, 5124);
        if (u == null) {
            return ShaderUniformSetter.alloc().set(Type.NIL, -1);
        }
        return ShaderUniformSetter.uniform1i(u.loc, i1);
    }

    public static ShaderUniformSetter alloc() {
        if (pool == null) {
            return new ShaderUniformSetter();
        }
        ShaderUniformSetter e = pool;
        pool = e.next;
        return e;
    }

    public static void release(ShaderUniformSetter e) {
        e.next = pool;
        pool = e;
    }

    public static enum Type {
        NIL,
        Uniform1f,
        Uniform2f,
        Uniform3f,
        Uniform4f,
        Uniform1i,
        Uniform2i,
        Uniform3i,
        Uniform4i;

    }
}

