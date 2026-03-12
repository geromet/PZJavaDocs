/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.VBO;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface IGLBufferObject {
    public int GL_ARRAY_BUFFER();

    public int GL_ELEMENT_ARRAY_BUFFER();

    public int GL_STATIC_DRAW();

    public int GL_STREAM_DRAW();

    public int GL_BUFFER_SIZE();

    public int GL_WRITE_ONLY();

    public int glGenBuffers();

    public void glBindBuffer(int var1, int var2);

    public void glDeleteBuffers(int var1);

    public void glBufferData(int var1, ByteBuffer var2, int var3);

    public void glBufferData(int var1, long var2, int var4);

    public ByteBuffer glMapBuffer(int var1, int var2, long var3, ByteBuffer var5);

    public boolean glUnmapBuffer(int var1);

    public void glGetBufferParameter(int var1, int var2, IntBuffer var3);
}

