/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.function.Function;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.rendering.InstancedBuffer;
import zombie.core.rendering.ShaderBuffer;
import zombie.debug.DebugLog;

public class ShaderParameter {
    private static final FloatBuffer VectorBuffer = MemoryUtil.memAllocFloat(4);
    private static final FloatBuffer MatrixBuffer = MemoryUtil.memAllocFloat(16);
    public final String name;
    private Object value;
    private Object defaultValue;
    private ParameterTypes type;
    public int offset;
    public int length;

    public ShaderParameter(ShaderParameter other) {
        this.name = other.name;
        this.defaultValue = other.defaultValue;
        this.type = other.type;
        this.offset = other.offset;
        this.length = other.length;
        switch (other.type.ordinal()) {
            case 3: {
                this.value = new Vector2f(other.GetVector2());
                break;
            }
            case 4: {
                this.value = new Vector3f(other.GetVector3());
                break;
            }
            case 5: {
                this.value = new Vector4f(other.GetVector4());
                break;
            }
            case 7: {
                this.value = new Matrix4f(other.GetMatrix4());
                break;
            }
            case 11: {
                this.value = this.CopyArray(Vector2f.class, other.GetVector2Array(), Vector2f::new);
                break;
            }
            case 12: {
                this.value = this.CopyArray(Vector3f.class, other.GetVector3Array(), Vector3f::new);
                break;
            }
            case 13: {
                this.value = this.CopyArray(Vector4f.class, other.GetVector4Array(), Vector4f::new);
                break;
            }
            case 15: {
                this.value = this.CopyArray(Matrix4f.class, other.GetMatrix4Array(), Matrix4f::new);
                break;
            }
            default: {
                this.value = other.value;
            }
        }
    }

    private ShaderParameter(String name, Object value, ParameterTypes type) {
        if (name.startsWith("instancedStructs[0].")) {
            name = name.substring("instancedStructs[0].".length());
        }
        if (name.endsWith("[0]")) {
            name = name.substring(0, name.length() - "[0]".length());
        }
        this.name = name;
        this.value = value;
        this.defaultValue = value;
        this.type = type;
        this.offset = -1;
        this.length = 1;
    }

    public ShaderParameter(String name, boolean value) {
        this(name, value, ParameterTypes.Bool);
    }

    public ShaderParameter(String name, int value) {
        this(name, (Object)value, ParameterTypes.Int);
    }

    public ShaderParameter(String name, int value, boolean isTexture) {
        this(name, (Object)value, isTexture ? ParameterTypes.Texture : ParameterTypes.Int);
    }

    public ShaderParameter(String name, float value) {
        this(name, Float.valueOf(value), ParameterTypes.Float);
    }

    public ShaderParameter(String name, Vector2f value) {
        this(name, value, ParameterTypes.Vector2);
    }

    public ShaderParameter(String name, Vector3f value) {
        this(name, value, ParameterTypes.Vector3);
    }

    public ShaderParameter(String name, Vector4f value) {
        this(name, value, ParameterTypes.Vector4);
    }

    public ShaderParameter(String name, Matrix3f value) {
        this(name, value, ParameterTypes.Matrix3);
    }

    public ShaderParameter(String name, Matrix4f value) {
        this(name, value, ParameterTypes.Matrix4);
    }

    public ShaderParameter(String name, int[] value, boolean isTexture) {
        this(name, (Object)value, isTexture ? ParameterTypes.TextureArray : ParameterTypes.IntArray);
    }

    public ShaderParameter(String name, float[] value) {
        this(name, value, ParameterTypes.FloatArray);
    }

    public ShaderParameter(String name, Vector2f[] value) {
        this(name, value, ParameterTypes.Vector2Array);
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != null) continue;
            value[i] = new Vector2f();
        }
    }

    public ShaderParameter(String name, Vector3f[] value) {
        this(name, value, ParameterTypes.Vector3Array);
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != null) continue;
            value[i] = new Vector3f();
        }
    }

    public ShaderParameter(String name, Vector4f[] value) {
        this(name, value, ParameterTypes.Vector4Array);
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != null) continue;
            value[i] = new Vector4f();
        }
    }

    public ShaderParameter(String name, Matrix3f[] value) {
        this(name, value, ParameterTypes.Matrix3Array);
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != null) continue;
            value[i] = new Matrix3f();
        }
    }

    public ShaderParameter(String name, Matrix4f[] value) {
        this(name, value, ParameterTypes.Matrix4Array);
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != null) continue;
            value[i] = new Matrix4f();
        }
    }

    public String toString() {
        return String.format("%s { %s: %3s }", this.getClass().getSimpleName(), this.name, this.value);
    }

    public ParameterTypes GetType() {
        return this.type;
    }

    private <T> T[] CopyArray(Class<T> token, T[] from, Function<T, T> create) {
        T[] to = Arrays.copyOf(from, from.length);
        for (int i = 0; i < to.length; ++i) {
            to[i] = create.apply(from[i]);
        }
        return to;
    }

    public void Copy(ShaderParameter param, boolean copyDefault, boolean matchType) {
        if (this.type == param.type || !matchType) {
            if (copyDefault || this.type != param.type) {
                this.defaultValue = param.defaultValue;
            }
            this.type = param.type;
            this.value = param.value;
        }
    }

    public void ResetValue() {
        this.value = this.defaultValue;
    }

    public int GetSize() {
        return switch (this.type.ordinal()) {
            case 0, 1, 2, 8 -> 4;
            case 3 -> 8;
            case 4 -> 12;
            case 5 -> 16;
            case 6 -> 36;
            case 7 -> 64;
            case 9, 10, 16 -> this.length * 4;
            case 11 -> this.length * 8;
            case 12, 13 -> this.length * 16;
            case 14 -> this.length * 36;
            case 15 -> this.length * 64;
            default -> 0;
        };
    }

    public Object GetValue() {
        return this.value;
    }

    public Boolean GetBool() {
        return (boolean)((Boolean)this.value);
    }

    public int GetInt() {
        return (Integer)this.value;
    }

    public float GetFloat() {
        return ((Float)this.value).floatValue();
    }

    public Vector2f GetVector2() {
        return (Vector2f)this.value;
    }

    public Vector3f GetVector3() {
        return (Vector3f)this.value;
    }

    public Vector4f GetVector4() {
        return (Vector4f)this.value;
    }

    public Matrix3f GetMatrix3() {
        return (Matrix3f)this.value;
    }

    public Matrix4f GetMatrix4() {
        return (Matrix4f)this.value;
    }

    public int GetTexture() {
        return (Integer)this.value;
    }

    public int[] GetIntArray() {
        return (int[])this.value;
    }

    public float[] GetFloatArray() {
        return (float[])this.value;
    }

    public Vector2f[] GetVector2Array() {
        return (Vector2f[])this.value;
    }

    public Vector3f[] GetVector3Array() {
        return (Vector3f[])this.value;
    }

    public Vector4f[] GetVector4Array() {
        return (Vector4f[])this.value;
    }

    public Matrix3f[] GetMatrix3Array() {
        return (Matrix3f[])this.value;
    }

    public Matrix4f[] GetMatrix4Array() {
        return (Matrix4f[])this.value;
    }

    public int[] GetTextureArray() {
        return (int[])this.value;
    }

    public FloatBuffer GetBuffer() {
        return (FloatBuffer)this.value;
    }

    private void SetValue(Object value, ParameterTypes type) {
        if (type != this.type) {
            String str = String.format("Changing parameter %s from %s to %s", new Object[]{this.name, this.type, type});
            DebugLog.Shader.warn(str);
            this.type = type;
            this.defaultValue = value;
        }
        this.value = value;
    }

    public void SetBool(boolean value) {
        this.SetValue(value, ParameterTypes.Bool);
    }

    public void SetInt(int value) {
        if (this.type == ParameterTypes.Int && this.value != null && (Integer)this.value == value) {
            return;
        }
        this.SetValue(value, ParameterTypes.Int);
    }

    public void SetFloat(float value) {
        if (this.type == ParameterTypes.Float && this.value != null && ((Float)this.value).floatValue() == value) {
            return;
        }
        this.SetValue(Float.valueOf(value), ParameterTypes.Float);
    }

    public void SetVector2(Vector2f vec) {
        this.SetVector2(vec.x, vec.y);
    }

    public void SetVector2(float x, float y) {
        if (this.value == null || this.type != ParameterTypes.Vector2) {
            this.SetValue(new Vector2f(x, y), ParameterTypes.Vector2);
        } else {
            Vector2f vec = (Vector2f)this.value;
            vec.set(x, y);
        }
    }

    public void SetVector3(Vector3f vec) {
        this.SetVector3(vec.x, vec.y, vec.z);
    }

    public void SetVector3(float x, float y, float z) {
        if (this.value == null || this.type != ParameterTypes.Vector3) {
            this.SetValue(new Vector3f(x, y, z), ParameterTypes.Vector3);
        } else {
            Vector3f vec = (Vector3f)this.value;
            vec.set(x, y, z);
        }
    }

    public void SetVector4(Vector4f vec) {
        this.SetVector4(vec.x, vec.y, vec.z, vec.w);
    }

    public void SetVector4(float x, float y, float z, float w) {
        if (this.value == null || this.type != ParameterTypes.Vector4) {
            this.SetValue(new Vector4f(x, y, z, w), ParameterTypes.Vector4);
        } else {
            Vector4f vec = (Vector4f)this.value;
            vec.set(x, y, z, w);
        }
    }

    public void SetMatrix3(Matrix3f mat) {
        Matrix3f matrix;
        if (this.value == null || this.type != ParameterTypes.Matrix3) {
            matrix = new Matrix3f();
            this.SetValue(matrix, ParameterTypes.Matrix3);
        } else {
            matrix = (Matrix3f)this.value;
        }
        matrix.load(mat);
    }

    public void SetMatrix4(Matrix4f mat) {
        Matrix4f matrix;
        if (this.value == null || this.type != ParameterTypes.Matrix4) {
            matrix = new Matrix4f();
            this.SetValue(matrix, ParameterTypes.Matrix4);
        } else {
            matrix = (Matrix4f)this.value;
        }
        matrix.load(mat);
    }

    public void SetTexture(int value) {
        this.SetValue(value, ParameterTypes.Texture);
    }

    public void SetIntArray(int[] value) {
        this.SetValue(value, ParameterTypes.IntArray);
    }

    public void SetFloatArray(float[] value) {
        this.SetValue(value, ParameterTypes.FloatArray);
    }

    public void SetVector2Array(Vector2f[] value) {
        this.SetValue(value, ParameterTypes.Vector2Array);
    }

    public void SetVector3Array(Vector3f[] value) {
        this.SetValue(value, ParameterTypes.Vector3Array);
    }

    public void SetVector4Array(Vector4f[] value) {
        this.SetValue(value, ParameterTypes.Vector4Array);
    }

    public void SetMatrix3Array(Matrix3f[] value) {
        this.SetValue(value, ParameterTypes.Matrix3Array);
    }

    public void SetMatrix4Array(Matrix4f[] value) {
        this.SetValue(value, ParameterTypes.Matrix4Array);
    }

    public void SetTextureArray(int[] value) {
        this.SetValue(value, ParameterTypes.TextureArray);
    }

    private static FloatBuffer StoreVectors(Vector[] vs, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * vs.length);
        for (int i = 0; i < vs.length; ++i) {
            vs[i].store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    private void LoadVectors(int program, Vector[] vs, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * vs.length);
        GL43.glGetUniformfv(program, this.offset, buffer);
        buffer.rewind();
        for (int i = 0; i < vs.length; ++i) {
            vs[i].load(buffer);
        }
        MemoryUtil.memFree(buffer);
    }

    private static FloatBuffer StoreMatrices(Matrix[] ms, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * ms.length);
        for (int i = 0; i < ms.length; ++i) {
            ms[i].store(buffer);
        }
        buffer.flip();
        return buffer;
    }

    private void LoadMatrices(int program, Matrix[] ms, int floatsPerElement) {
        FloatBuffer buffer = MemoryUtil.memAllocFloat(floatsPerElement * ms.length);
        GL43.glGetUniformfv(program, this.offset, buffer);
        buffer.rewind();
        for (int i = 0; i < ms.length; ++i) {
            ms[i].load(buffer);
        }
        MemoryUtil.memFree(buffer);
    }

    public void UpdateDefault() {
        this.defaultValue = this.value;
    }

    public void PushUniform() {
        switch (this.type.ordinal()) {
            case 0: {
                GL43.glUniform1i(this.offset, (Boolean)this.value != false ? 1 : 0);
                break;
            }
            case 1: 
            case 8: {
                GL43.glUniform1i(this.offset, (Integer)this.value);
                break;
            }
            case 2: {
                GL43.glUniform1f(this.offset, ((Float)this.value).floatValue());
                break;
            }
            case 3: {
                Vector2f v = (Vector2f)this.value;
                GL43.glUniform2f(this.offset, v.x, v.y);
                break;
            }
            case 4: {
                Vector3f v = (Vector3f)this.value;
                GL43.glUniform3f(this.offset, v.x, v.y, v.z);
                break;
            }
            case 5: {
                Vector4f v = (Vector4f)this.value;
                GL43.glUniform4f(this.offset, v.x, v.y, v.z, v.w);
                break;
            }
            case 6: {
                ((Matrix3f)this.value).store(MatrixBuffer);
                GL43.glUniformMatrix3fv(this.offset, true, MatrixBuffer.flip());
                MatrixBuffer.limit(MatrixBuffer.capacity());
                break;
            }
            case 7: {
                ((Matrix4f)this.value).store(MatrixBuffer);
                GL43.glUniformMatrix4fv(this.offset, true, MatrixBuffer.flip());
                break;
            }
            case 9: 
            case 16: {
                GL43.glUniform1iv(this.offset, (int[])this.value);
                break;
            }
            case 10: {
                GL43.glUniform1fv(this.offset, (float[])this.value);
                break;
            }
            case 11: {
                FloatBuffer buffer = ShaderParameter.StoreVectors((Vector2f[])this.value, 2);
                GL43.glUniform2fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case 12: {
                FloatBuffer buffer = ShaderParameter.StoreVectors((Vector3f[])this.value, 3);
                GL43.glUniform3fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case 13: {
                FloatBuffer buffer = ShaderParameter.StoreVectors((Vector3f[])this.value, 4);
                GL43.glUniform4fv(this.offset, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case 14: {
                FloatBuffer buffer = ShaderParameter.StoreMatrices((Matrix3f[])this.value, 9);
                GL43.glUniformMatrix3fv(this.offset, true, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
            case 15: {
                FloatBuffer buffer = ShaderParameter.StoreMatrices((Matrix4f[])this.value, 16);
                GL43.glUniformMatrix4fv(this.offset, true, buffer);
                MemoryUtil.memFree(buffer);
                break;
            }
        }
    }

    public void PullUniform(int program) {
        switch (this.type.ordinal()) {
            case 0: {
                this.value = GL43.glGetUniformi(program, this.offset) != 0;
                break;
            }
            case 1: 
            case 8: {
                this.value = GL43.glGetUniformi(program, this.offset);
                break;
            }
            case 2: {
                this.value = Float.valueOf(GL43.glGetUniformf(program, this.offset));
                break;
            }
            case 3: {
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector2f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            }
            case 4: {
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector3f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            }
            case 5: {
                GL43.glGetUniformfv(program, this.offset, VectorBuffer);
                VectorBuffer.rewind();
                ((Vector4f)this.value).load(VectorBuffer);
                VectorBuffer.rewind();
                break;
            }
            case 6: {
                GL43.glGetUniformfv(program, this.offset, MatrixBuffer);
                MatrixBuffer.rewind();
                ((Matrix3f)this.value).load(MatrixBuffer);
                MatrixBuffer.rewind();
                break;
            }
            case 7: {
                GL43.glGetUniformfv(program, this.offset, MatrixBuffer);
                MatrixBuffer.rewind();
                ((Matrix4f)this.value).load(MatrixBuffer);
                MatrixBuffer.rewind();
                break;
            }
            case 9: 
            case 16: {
                GL43.glGetUniformiv(program, this.offset, (int[])this.value);
                break;
            }
            case 10: {
                GL43.glGetUniformfv(program, this.offset, (float[])this.value);
                break;
            }
            case 11: {
                this.LoadVectors(program, (Vector2f[])this.value, 2);
                break;
            }
            case 12: {
                this.LoadVectors(program, (Vector3f[])this.value, 3);
                break;
            }
            case 13: {
                this.LoadVectors(program, (Vector4f[])this.value, 4);
                break;
            }
            case 14: {
                this.LoadMatrices(program, (Matrix3f[])this.value, 9);
                break;
            }
            case 15: {
                this.LoadMatrices(program, (Matrix4f[])this.value, 16);
            }
        }
    }

    public void PushInstanced(InstancedBuffer buffer, int baseOffset) {
        this.WriteToBuffer(buffer.data, baseOffset);
    }

    public void WriteToBuffer(ByteBuffer buffer, int baseOffset) {
        if (this.offset < 0 || this.type == ParameterTypes.Texture || this.type == ParameterTypes.TextureArray) {
            return;
        }
        buffer.position(baseOffset + this.offset);
        switch (this.type.ordinal()) {
            case 0: {
                ShaderBuffer.PushBool(buffer, (Boolean)this.value);
                break;
            }
            case 1: 
            case 8: {
                ShaderBuffer.PushInt(buffer, (Integer)this.value);
                break;
            }
            case 2: {
                ShaderBuffer.PushFloat(buffer, ((Float)this.value).floatValue());
                break;
            }
            case 3: {
                ShaderBuffer.PushVector2(buffer, (Vector2f)this.value);
                break;
            }
            case 4: {
                ShaderBuffer.PushVector3(buffer, (Vector3f)this.value);
                break;
            }
            case 5: {
                ShaderBuffer.PushVector4(buffer, (Vector4f)this.value);
                break;
            }
            case 6: {
                ShaderBuffer.PushMatrix3(buffer, (Matrix3f)this.value);
                break;
            }
            case 7: {
                ShaderBuffer.PushMatrix4(buffer, (Matrix4f)this.value);
                break;
            }
            case 9: 
            case 16: {
                ShaderBuffer.PushIntArray(buffer, (int[])this.value);
                break;
            }
            case 10: {
                ShaderBuffer.PushFloatArray(buffer, (float[])this.value);
                break;
            }
            case 11: {
                ShaderBuffer.PushVector2Array(buffer, (Vector2f[])this.value);
                break;
            }
            case 12: {
                ShaderBuffer.PushVector3Array(buffer, (Vector3f[])this.value);
                break;
            }
            case 13: {
                ShaderBuffer.PushVector4Array(buffer, (Vector4f[])this.value);
                break;
            }
            case 15: {
                ShaderBuffer.PushMatrix4Array(buffer, (Matrix4f[])this.value);
            }
        }
    }

    public static enum ParameterTypes {
        Bool,
        Int,
        Float,
        Vector2,
        Vector3,
        Vector4,
        Matrix3,
        Matrix4,
        Texture,
        IntArray,
        FloatArray,
        Vector2Array,
        Vector3Array,
        Vector4Array,
        Matrix3Array,
        Matrix4Array,
        TextureArray;

    }
}

