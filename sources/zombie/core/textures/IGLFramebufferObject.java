/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

public interface IGLFramebufferObject {
    public int GL_FRAMEBUFFER();

    public int GL_RENDERBUFFER();

    public int GL_COLOR_ATTACHMENT0();

    public int GL_DEPTH_ATTACHMENT();

    public int GL_STENCIL_ATTACHMENT();

    public int GL_DEPTH_STENCIL();

    public int GL_DEPTH24_STENCIL8();

    public int GL_FRAMEBUFFER_COMPLETE();

    public int GL_FRAMEBUFFER_UNDEFINED();

    public int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT();

    public int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT();

    public int GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS();

    public int GL_FRAMEBUFFER_INCOMPLETE_FORMATS();

    public int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER();

    public int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER();

    public int GL_FRAMEBUFFER_UNSUPPORTED();

    public int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE();

    public int glGenFramebuffers();

    public void glBindFramebuffer(int var1, int var2);

    public void glFramebufferTexture2D(int var1, int var2, int var3, int var4, int var5);

    public int glGenRenderbuffers();

    public void glBindRenderbuffer(int var1, int var2);

    public void glRenderbufferStorage(int var1, int var2, int var3, int var4);

    public void glFramebufferRenderbuffer(int var1, int var2, int var3, int var4);

    public int glCheckFramebufferStatus(int var1);

    public void glDeleteFramebuffers(int var1);

    public void glDeleteRenderbuffers(int var1);
}

