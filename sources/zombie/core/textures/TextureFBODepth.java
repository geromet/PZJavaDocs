/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import gnu.trove.stack.array.TIntArrayStack;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.GLFramebufferObject30;
import zombie.core.textures.GLFramebufferObjectARB;
import zombie.core.textures.GLFramebufferObjectEXT;
import zombie.core.textures.IGLFramebufferObject;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.interfaces.ITexture;

public class TextureFBODepth {
    private static IGLFramebufferObject funcs;
    public static int lastID;
    private static final TIntArrayStack stack;
    private int id;
    ITexture texture;
    private int depth;
    private int width;
    private int height;
    private static Boolean checked;

    public void swapTexture(ITexture newTex) {
        assert (lastID == this.id);
        if (newTex == null || newTex == this.texture) {
            return;
        }
        if (newTex.getWidth() != this.width || newTex.getHeight() != this.height) {
            return;
        }
        if (newTex.getID() == -1) {
            newTex.bind();
        }
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        funcs.glFramebufferTexture2D(funcs.GL_FRAMEBUFFER(), funcs.GL_DEPTH_ATTACHMENT(), 3553, newTex.getID(), 0);
        this.texture = newTex;
    }

    public TextureFBODepth(ITexture destination) {
        RenderThread.invokeOnRenderContext(destination, this::init);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void init(ITexture destination) {
        int curID = lastID;
        try {
            this.initInternal(destination);
        }
        finally {
            IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
            lastID = curID;
            funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), lastID);
        }
    }

    public static IGLFramebufferObject getFuncs() {
        if (funcs == null) {
            TextureFBODepth.checkFBOSupport();
        }
        return funcs;
    }

    public void blitDepth(float x, float y, float w, float h) {
        GL30C.glBindFramebuffer(36008, this.id);
        GL30C.glBindFramebuffer(36009, lastID);
        GL30C.glBlitFramebuffer(0, 0, this.width, this.height, (int)x, (int)y, (int)w, (int)h, 256, 9729);
        GL30C.glBindFramebuffer(36160, lastID);
    }

    private void initInternal(ITexture destination) {
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        try {
            PZGLUtil.checkGLErrorThrow("Enter.", new Object[0]);
            this.texture = destination;
            this.width = this.texture.getWidth();
            this.height = this.texture.getHeight();
            if (!TextureFBODepth.checkFBOSupport()) {
                throw new RuntimeException("Could not create FBO. FBO's not supported.");
            }
            if (this.texture == null) {
                throw new NullPointerException("Could not create FBO. Texture is null.");
            }
            this.texture.bind();
            PZGLUtil.checkGLErrorThrow("Binding texture. %s", this.texture);
            GL11.glTexImage2D(3553, 0, 6402, this.texture.getWidthHW(), this.texture.getHeightHW(), 0, 6402, 5126, (IntBuffer)null);
            PZGLUtil.checkGLErrorThrow("glTexImage2D(width: %d, height: %d)", this.texture.getWidthHW(), this.texture.getHeightHW());
            GL11.glTexParameteri(3553, 10242, 33071);
            GL11.glTexParameteri(3553, 10243, 33071);
            Texture.lastTextureID = 0;
            GL11.glBindTexture(3553, 0);
            this.id = funcs.glGenFramebuffers();
            PZGLUtil.checkGLErrorThrow("glGenFrameBuffers", new Object[0]);
            funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), this.id);
            PZGLUtil.checkGLErrorThrow("glBindFramebuffer(%d)", this.id);
            funcs.glFramebufferTexture2D(funcs.GL_FRAMEBUFFER(), funcs.GL_DEPTH_ATTACHMENT(), 3553, this.texture.getID(), 0);
            PZGLUtil.checkGLErrorThrow("glFramebufferTexture2D texture: %s", this.texture);
            int status = funcs.glCheckFramebufferStatus(funcs.GL_FRAMEBUFFER());
            if (status != funcs.GL_FRAMEBUFFER_COMPLETE()) {
                if (status == funcs.GL_FRAMEBUFFER_UNDEFINED()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_UNDEFINED");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_FORMATS()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_FORMATS");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
                }
                if (status == funcs.GL_FRAMEBUFFER_UNSUPPORTED()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_UNSUPPORTED");
                }
                if (status == funcs.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE()) {
                    DebugLog.General.error("glCheckFramebufferStatus = GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE");
                }
                throw new RuntimeException("Could not create FBO!");
            }
        }
        catch (Exception ex) {
            funcs.glDeleteFramebuffers(this.id);
            funcs.glDeleteRenderbuffers(this.depth);
            this.id = 0;
            this.depth = 0;
            this.texture = null;
            throw ex;
        }
    }

    public static boolean checkFBOSupport() {
        if (checked != null) {
            return checked;
        }
        if (GL.getCapabilities().OpenGL30) {
            DebugLog.General.debugln("OpenGL 3.0 framebuffer objects supported");
            funcs = new GLFramebufferObject30();
            checked = Boolean.TRUE;
            return checked;
        }
        if (GL.getCapabilities().GL_ARB_framebuffer_object) {
            DebugLog.General.debugln("GL_ARB_framebuffer_object supported");
            funcs = new GLFramebufferObjectARB();
            checked = Boolean.TRUE;
            return checked;
        }
        if (GL.getCapabilities().GL_EXT_framebuffer_object) {
            DebugLog.General.debugln("GL_EXT_framebuffer_object supported");
            if (!GL.getCapabilities().GL_EXT_packed_depth_stencil) {
                DebugLog.General.debugln("GL_EXT_packed_depth_stencil not supported");
            }
            funcs = new GLFramebufferObjectEXT();
            checked = Boolean.TRUE;
            return checked;
        }
        DebugLog.General.debugln("None of OpenGL 3.0, GL_ARB_framebuffer_object or GL_EXT_framebuffer_object are supported, zoom disabled");
        checked = Boolean.TRUE;
        return checked;
    }

    public void destroy() {
        if (this.id == 0 || this.depth == 0) {
            return;
        }
        if (lastID == this.id) {
            lastID = 0;
        }
        RenderThread.invokeOnRenderContext(() -> {
            if (this.texture != null) {
                this.texture.destroy();
                this.texture = null;
            }
            IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
            funcs.glDeleteFramebuffers(this.id);
            funcs.glDeleteRenderbuffers(this.depth);
            this.id = 0;
            this.depth = 0;
        });
    }

    public void destroyLeaveTexture() {
        if (this.id == 0 || this.depth == 0) {
            return;
        }
        RenderThread.invokeOnRenderContext(() -> {
            this.texture = null;
            IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
            funcs.glDeleteFramebuffers(this.id);
            funcs.glDeleteRenderbuffers(this.depth);
            this.id = 0;
            this.depth = 0;
        });
    }

    public void releaseTexture() {
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        funcs.glFramebufferTexture2D(funcs.GL_FRAMEBUFFER(), funcs.GL_DEPTH_ATTACHMENT(), 3553, 0, 0);
        this.texture = null;
    }

    public void endDrawing() {
        lastID = stack.size() != 0 ? stack.pop() : 0;
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), lastID);
    }

    public ITexture getTexture() {
        return this.texture;
    }

    public int getBufferId() {
        return this.id;
    }

    public boolean isDestroyed() {
        return this.texture == null || this.id == 0 || this.depth == 0;
    }

    public void startDrawing() {
        this.startDrawing(false);
    }

    public void startDrawing(boolean clear) {
        stack.push(lastID);
        lastID = this.id;
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), this.id);
        if (this.texture == null) {
            return;
        }
        if (clear) {
            GL11.glClearDepth(1.0);
            GL11.glClear(256);
        }
    }

    public void setTexture(Texture tex3) {
        int curID = lastID;
        IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
        lastID = this.id;
        funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), lastID);
        this.swapTexture(tex3);
        lastID = curID;
        funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), lastID);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public static int getCurrentID() {
        return lastID;
    }

    public static void reset() {
        stack.clear();
        if (lastID != 0) {
            IGLFramebufferObject funcs = TextureFBODepth.getFuncs();
            lastID = 0;
            funcs.glBindFramebuffer(funcs.GL_FRAMEBUFFER(), 0);
        }
    }

    static {
        stack = new TIntArrayStack();
    }
}

