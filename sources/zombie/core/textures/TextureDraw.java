/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import gnu.trove.map.hash.TIntObjectHashMap;
import imgui.ImDrawData;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.Display;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.rendering.RenderList;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelInstanceRenderDataList;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.textures.Texture;
import zombie.debug.DebugContext;
import zombie.debug.DebugOptions;
import zombie.iso.IsoWorld;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.ui.UIManager;
import zombie.util.list.PZArrayUtil;
import zombie.viewCone.ViewConeTextureFBO;

public final class TextureDraw {
    public static float nextZ;
    public static float nextChunkDepth;
    public Type type = Type.glDraw;
    public boolean flipped;
    public int a;
    public int b;
    public float f1;
    public float[] vars;
    public int c;
    public int d;
    public int col0;
    public int col1;
    public int col2;
    public int col3;
    public float x0;
    public float x1;
    public float x2;
    public float x3;
    public float y0;
    public float y1;
    public float y2;
    public float y3;
    public float u0;
    public float u1;
    public float u2;
    public float u3;
    public float v0;
    public float v1;
    public float v2;
    public float v3;
    public float z;
    public float chunkDepth;
    public Texture tex;
    public Texture tex1;
    public Texture tex2;
    public byte useAttribArray;
    public float tex1U0;
    public float tex1U1;
    public float tex1U2;
    public float tex1U3;
    public float tex1V0;
    public float tex1V1;
    public float tex1V2;
    public float tex1V3;
    public int tex1Col0;
    public int tex1Col1;
    public int tex1Col2;
    public int tex1Col3;
    public float tex2U0;
    public float tex2U1;
    public float tex2U2;
    public float tex2U3;
    public float tex2V0;
    public float tex2V1;
    public float tex2V2;
    public float tex2V3;
    public boolean singleCol;
    public ImDrawData imDrawData;
    public PerformanceProfileProbe probe;
    public GenericDrawer drawer;
    public Future<?> future;
    private static final ExecutorService slotInitExec;

    public static void glStencilFunc(TextureDraw texd, int a, int b, int c) {
        texd.type = Type.glStencilFunc;
        texd.a = a;
        texd.b = b;
        texd.c = c;
    }

    public static void glBuffer(TextureDraw texd, int a, int b) {
        texd.type = Type.glBuffer;
        texd.a = a;
        texd.b = b;
    }

    public static void glStencilOp(TextureDraw texd, int a, int b, int c) {
        texd.type = Type.glStencilOp;
        texd.a = a;
        texd.b = b;
        texd.c = c;
    }

    public static void glDisable(TextureDraw texd, int a) {
        texd.type = Type.glDisable;
        texd.a = a;
    }

    public static void glClear(TextureDraw texd, int a) {
        texd.type = Type.glClear;
        texd.a = a;
    }

    public static void glBindFramebuffer(TextureDraw texd, int binding, int fbo) {
        texd.type = Type.glBindFramebuffer;
        texd.a = binding;
        texd.b = fbo;
    }

    public static void glClearDepth(TextureDraw texd, float d) {
        texd.type = Type.glClearDepth;
        texd.u0 = d;
    }

    public static void glClearColor(TextureDraw texd, int r, int g, int b, int a) {
        texd.type = Type.glClearColor;
        texd.col0 = r;
        texd.col1 = g;
        texd.col2 = b;
        texd.col3 = a;
    }

    public static void NewFrame(TextureDraw texd) {
        texd.type = Type.NewFrame;
    }

    public static void glDepthFunc(TextureDraw texd, int a) {
        texd.type = Type.glDepthFunc;
        texd.a = a;
    }

    public static void glEnable(TextureDraw texd, int a) {
        texd.type = Type.glEnable;
        texd.a = a;
    }

    public static void glAlphaFunc(TextureDraw texd, int a, float b) {
        texd.type = Type.glAlphaFunc;
        texd.a = a;
        texd.f1 = b;
    }

    public static void glColorMask(TextureDraw texd, int a, int b, int c, int d) {
        texd.type = Type.glColorMask;
        texd.a = a;
        texd.b = b;
        texd.c = c;
        texd.x0 = d;
    }

    public static void glStencilMask(TextureDraw texd, int a) {
        texd.type = Type.glStencilMask;
        texd.a = a;
    }

    public static void glBlendFunc(TextureDraw texd, int a, int b) {
        texd.type = Type.glBlendFunc;
        texd.a = a;
        texd.b = b;
    }

    public static void glBlendFuncSeparate(TextureDraw texd, int a, int b, int c, int d) {
        texd.type = Type.glBlendFuncSeparate;
        texd.a = a;
        texd.b = b;
        texd.c = c;
        texd.d = d;
    }

    public static void glBlendEquation(TextureDraw texd, int a) {
        texd.type = Type.glBlendEquation;
        texd.a = a;
    }

    public static void pushIsoView(TextureDraw texd, float ox, float oy, float oz, float useangle, boolean vehicle) {
        texd.type = Type.pushIsoView;
        texd.u0 = ox;
        texd.u1 = oy;
        texd.u2 = oz;
        texd.u3 = useangle;
        texd.a = vehicle ? 1 : 0;
    }

    public static void popIsoView(TextureDraw texd) {
        texd.type = Type.popIsoView;
    }

    public static void glDoEndFrame(TextureDraw texd) {
        texd.type = Type.glDoEndFrame;
    }

    public static void glDoEndFrameFx(TextureDraw texd, int player) {
        texd.type = Type.glDoEndFrameFx;
        texd.c = player;
    }

    public static void glIgnoreStyles(TextureDraw texd, boolean b) {
        texd.type = Type.glIgnoreStyles;
        texd.a = b ? 1 : 0;
    }

    public static void glDoStartFrame(TextureDraw texd, int w, int h, float zoom, int player) {
        TextureDraw.glDoStartFrame(texd, w, h, zoom, player, false);
    }

    public static void glDoStartFrameNoZoom(TextureDraw texd, int w, int h, float zoom, int player) {
        texd.type = Type.glDoStartFrameNoZoom;
        texd.a = w;
        texd.b = h;
        texd.f1 = zoom;
        texd.c = player;
    }

    public static void glDoStartFrameFlipY(TextureDraw texd, int x, int y, float zoom, int player) {
        texd.type = Type.glDoStartFrameFlipY;
        texd.a = x;
        texd.b = y;
        texd.f1 = zoom;
        texd.c = player;
    }

    public static void glDoStartFrame(TextureDraw texd, int w, int h, float zoom, int player, boolean isTextFrame) {
        texd.type = isTextFrame ? Type.glDoStartFrameText : Type.glDoStartFrame;
        texd.a = w;
        texd.b = h;
        texd.f1 = zoom;
        texd.c = player;
    }

    public static void glDoStartFrameFx(TextureDraw texd, int w, int h, int player) {
        texd.type = Type.glDoStartFrameFx;
        texd.a = w;
        texd.b = h;
        texd.c = player;
    }

    public static void glTexParameteri(TextureDraw texd, int a, int b, int c) {
        texd.type = Type.glTexParameteri;
        texd.a = a;
        texd.b = b;
        texd.c = c;
    }

    public static void drawModel(TextureDraw texd, ModelManager.ModelSlot modelSlot) {
        texd.type = Type.DrawModel;
        texd.a = modelSlot.id;
        ModelSlotRenderData slotData = ModelSlotRenderData.alloc();
        slotData.initModel(modelSlot);
        texd.drawer = slotData;
        if (DebugOptions.instance.threadModelSlotInit.getValue()) {
            texd.future = slotInitExec.submit(() -> {
                ModelSlotRenderData modelSlotRenderData = slotData;
                synchronized (modelSlotRenderData) {
                    slotData.init(modelSlot);
                }
            });
        } else {
            slotData.init(modelSlot);
        }
    }

    public static void drawSkyBox(TextureDraw texd, Shader shader, int userId, int apiId, int bufferId) {
        texd.type = Type.DrawSkyBox;
        texd.a = shader.getID();
        texd.b = userId;
        texd.c = apiId;
        texd.d = bufferId;
        texd.drawer = null;
    }

    public static void drawWater(TextureDraw texd, Shader shader, int playerIndex, int firstSquare, int numSquares, boolean bShore) {
        texd.type = Type.DrawWater;
        texd.a = shader.getID();
        texd.b = playerIndex;
        texd.c = firstSquare;
        texd.d = numSquares;
        texd.flipped = bShore;
        texd.drawer = null;
    }

    public static void drawPuddles(TextureDraw texd, int playerIndex, int z, int firstSquare, int numSquares) {
        texd.type = Type.DrawPuddles;
        texd.a = playerIndex;
        texd.b = z;
        texd.c = firstSquare;
        texd.d = numSquares;
        texd.drawer = null;
    }

    public static void drawParticles(TextureDraw texd, int userId, int var1, int var2) {
        texd.type = Type.DrawParticles;
        texd.b = userId;
        texd.c = var1;
        texd.d = var2;
        texd.drawer = null;
    }

    public static void StartShader(TextureDraw texd, int iD) {
        texd.type = Type.StartShader;
        texd.a = iD;
    }

    public static void StartShader(TextureDraw texd, int iD, ShaderUniformSetter uniforms) {
        texd.type = Type.StartShader;
        texd.a = iD;
        texd.drawer = uniforms;
    }

    public static void ShaderUpdate1i(TextureDraw texd, int shaderID, int uniform, int uniformValue) {
        texd.type = Type.ShaderUpdate;
        texd.a = shaderID;
        texd.b = uniform;
        texd.c = -1;
        texd.d = uniformValue;
    }

    public static void ShaderUpdate1f(TextureDraw texd, int shaderID, int uniform, float uniformValue) {
        texd.type = Type.ShaderUpdate;
        texd.a = shaderID;
        texd.b = uniform;
        texd.c = 1;
        texd.u0 = uniformValue;
    }

    public static void ShaderUpdate2f(TextureDraw texd, int shaderID, int uniform, float value1, float value2) {
        texd.type = Type.ShaderUpdate;
        texd.a = shaderID;
        texd.b = uniform;
        texd.c = 2;
        texd.u0 = value1;
        texd.u1 = value2;
    }

    public static void ShaderUpdate3f(TextureDraw texd, int shaderID, int uniform, float value1, float value2, float value3) {
        texd.type = Type.ShaderUpdate;
        texd.a = shaderID;
        texd.b = uniform;
        texd.c = 3;
        texd.u0 = value1;
        texd.u1 = value2;
        texd.u2 = value3;
    }

    public static void ShaderUpdate4f(TextureDraw texd, int shaderID, int uniform, float value1, float value2, float value3, float value4) {
        texd.type = Type.ShaderUpdate;
        texd.a = shaderID;
        texd.b = uniform;
        texd.c = 4;
        texd.u0 = value1;
        texd.u1 = value2;
        texd.u2 = value3;
        texd.u3 = value4;
    }

    public static void FBORenderChunkStart(TextureDraw textureDraw, int index, boolean bClear) {
        textureDraw.type = Type.FBORenderChunkStart;
        textureDraw.a = index;
        textureDraw.b = bClear ? 1 : 0;
    }

    public static void FBORenderChunkEnd(TextureDraw textureDraw) {
        textureDraw.type = Type.FBORenderChunkEnd;
    }

    public static void releaseFBORenderChunkLock(TextureDraw textureDraw) {
        textureDraw.type = Type.releaseFBORenderChunkLock;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void run() {
        switch (this.type.ordinal()) {
            case 43: {
                TIntObjectHashMap indexToChunk = SpriteRenderer.instance.getRenderingState().cachedRenderChunkIndexMap;
                indexToChunk.forEachValue(fboRenderChunk -> {
                    fboRenderChunk.submitted = false;
                    return true;
                });
                break;
            }
            case 40: {
                FBORenderChunkManager.instance.renderThreadChunkStart(this.a, this.b == 1);
                break;
            }
            case 39: {
                FBORenderChunkManager.instance.renderThreadChunkEnd();
                break;
            }
            case 16: {
                GenericDrawer genericDrawer;
                ShaderHelper.glUseProgramObjectARB(this.a);
                if (Shader.ShaderMap.containsKey(this.a)) {
                    Shader.ShaderMap.get(this.a).startRenderThread(this);
                }
                if (this.a == 0) {
                    SpriteRenderer.ringBuffer.checkShaderChangedTexture1();
                }
                if (!((genericDrawer = this.drawer) instanceof ShaderUniformSetter)) break;
                ShaderUniformSetter uniforms = (ShaderUniformSetter)genericDrawer;
                uniforms.invokeAll();
                break;
            }
            case 26: {
                GL20.glUseProgram(this.a);
                if (this.c == 1) {
                    GL20.glUniform1f(this.b, this.u0);
                }
                if (this.c == 2) {
                    GL20.glUniform2f(this.b, this.u0, this.u1);
                }
                if (this.c == 3) {
                    GL20.glUniform3f(this.b, this.u0, this.u1, this.u2);
                }
                if (this.c == 4) {
                    GL20.glUniform4f(this.b, this.u0, this.u1, this.u2, this.u3);
                }
                if (this.c != -1) break;
                GL20.glUniform1i(this.b, this.d);
                break;
            }
            case 27: {
                GL13.glActiveTexture(this.a);
                if (this.b != -1) {
                    GL11.glBindTexture(3553, this.b);
                }
                GL13.glActiveTexture(33984);
                break;
            }
            case 49: {
                Display.drawImGuiDrawData(this.imDrawData);
                break;
            }
            case 21: {
                Object uniforms;
                if (this.drawer == null) break;
                if (this.future != null) {
                    uniforms = GameProfiler.getInstance().profile("Wait");
                    try {
                        this.GetFutureResultThrow();
                    }
                    finally {
                        if (uniforms != null) {
                            ((GameProfiler.ProfileArea)uniforms).close();
                        }
                    }
                }
                uniforms = this.drawer;
                synchronized (uniforms) {
                    this.drawer.render(this);
                    break;
                }
            }
            case 22: {
                try {
                    ModelManager.instance.RenderSkyBox(this, this.a, this.b, this.c, this.d);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case 23: {
                try {
                    ModelManager.instance.RenderWater(this, this.a, this.b, this.c, this.d, this.flipped);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case 24: {
                try {
                    ModelManager.instance.RenderPuddles(this.a, this.b, this.c, this.d);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case 25: {
                try {
                    ModelManager.instance.RenderParticles(this, this.b, this.c);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            }
            case 9: {
                IndieGL.glClearA(this.a);
                break;
            }
            case 10: {
                GL30.glBindFramebuffer(this.a, this.b);
                break;
            }
            case 45: {
                GL11.glClearDepth(this.u0);
                break;
            }
            case 32: {
                GL11.glClearColor((float)this.col0 / 255.0f, (float)this.col1 / 255.0f, (float)this.col2 / 255.0f, (float)this.col3 / 255.0f);
                break;
            }
            case 11: {
                GLStateRenderThread.BlendFuncSeparate.set(this.a, this.b, this.a, this.b);
                break;
            }
            case 33: {
                GLStateRenderThread.BlendFuncSeparate.set(this.a, this.b, this.c, this.d);
                break;
            }
            case 7: {
                GLStateRenderThread.ColorMask.set(this.a == 1, this.b == 1, this.c == 1, this.x0 == 1.0f);
                break;
            }
            case 15: {
                IndieGL.glTexParameteriActual(this.a, this.b, this.c);
                break;
            }
            case 8: {
                GLStateRenderThread.StencilMask.set(this.a);
                break;
            }
            case 14: {
                Core.getInstance().DoEndFrameStuff(this.a, this.b);
                break;
            }
            case 30: {
                Core.getInstance().DoEndFrameStuffFx(this.a, this.b, this.c);
                break;
            }
            case 12: {
                Core.getInstance().DoStartFrameStuff(this.a, this.b, this.f1, this.c);
                break;
            }
            case 41: {
                Core.getInstance().DoStartFrameNoZoom(this.a, this.b, this.f1, this.c, false, false, false);
                break;
            }
            case 42: {
                Core.getInstance().StartFrameFlipY(this.a, this.b, this.f1, this.c);
                break;
            }
            case 13: {
                Core.getInstance().DoStartFrameStuff(this.a, this.b, this.f1, this.c, true);
                break;
            }
            case 29: {
                Core.getInstance().DoStartFrameStuffSmartTextureFx(this.a, this.b, this.c);
                break;
            }
            case 2: {
                GLStateRenderThread.StencilFunc.set(this.a, this.b, this.c);
                break;
            }
            case 1: {
                if (!Core.getInstance().supportsFBO()) break;
                if (this.a == 1) {
                    SpriteRenderer.instance.getRenderingState().fbo.startDrawing(false, false);
                    break;
                }
                if (this.a == 2) {
                    UIManager.uiFbo.startDrawing(true, true);
                    break;
                }
                if (this.a == 3) {
                    UIManager.uiFbo.endDrawing();
                    break;
                }
                if (this.a == 4) {
                    WeatherFxMask.getFboMask().startDrawing(true, true);
                    break;
                }
                if (this.a == 5) {
                    WeatherFxMask.getFboMask().endDrawing();
                    break;
                }
                if (this.a == 6) {
                    WeatherFxMask.getFboParticles().startDrawing(true, true);
                    break;
                }
                if (this.a == 7) {
                    WeatherFxMask.getFboParticles().endDrawing();
                    break;
                }
                if (this.a == 8) {
                    ViewConeTextureFBO.instance.startDrawing();
                    break;
                }
                if (this.a == 9) {
                    ViewConeTextureFBO.instance.stopDrawing();
                    break;
                }
                if (this.a == 10) {
                    FBORenderChunkManager.instance.startDrawingCombined();
                    break;
                }
                if (this.a == 11) {
                    FBORenderChunkManager.instance.endDrawingCombined();
                    break;
                }
                if (this.a == 12) {
                    if (!Core.isUseGameViewport()) break;
                    DebugContext.instance.endDrawing();
                    break;
                }
                SpriteRenderer.instance.getRenderingState().fbo.endDrawing();
                if (!Core.isUseGameViewport()) break;
                DebugContext.instance.startDrawing();
                break;
            }
            case 4: {
                GLStateRenderThread.StencilOp.set(this.a, this.b, this.c);
                break;
            }
            case 17: {
                GL11.glLoadIdentity();
                break;
            }
            case 19: {
                GL11.glBindTexture(3553, this.a);
                Texture.lastlastTextureID = Texture.lastTextureID;
                Texture.lastTextureID = this.a;
                break;
            }
            case 20: {
                GL11.glViewport(this.a, this.b, this.c, this.d);
                break;
            }
            case 36: {
                IsoWorld.instance.renderTerrain();
                break;
            }
            case 35: {
                Core.getInstance().floatParamMap.put(this.a, Float.valueOf(this.f1));
                break;
            }
            case 34: {
                GLStateRenderThread.DepthMask.set(this.a == 1);
                break;
            }
            case 18: {
                break;
            }
            case 3: {
                GLStateRenderThread.AlphaFunc.set(this.a, this.f1);
                break;
            }
            case 5: {
                if (this.a == 3008) {
                    GLStateRenderThread.AlphaTest.set(true);
                    break;
                }
                if (this.a == 3042) {
                    GLStateRenderThread.Blend.set(true);
                    break;
                }
                if (this.a == 2929) {
                    GLStateRenderThread.DepthTest.set(true);
                    break;
                }
                if (this.a == 3089) {
                    GLStateRenderThread.ScissorTest.set(true);
                    break;
                }
                if (this.a == 2960) {
                    GLStateRenderThread.StencilTest.set(true);
                    break;
                }
                IndieGL.glEnableA(this.a);
                break;
            }
            case 46: {
                Core.getInstance().projectionMatrixStack.clear();
                Core.getInstance().modelViewMatrixStack.clear();
                break;
            }
            case 44: {
                GLStateRenderThread.DepthFunc.set(this.a);
                break;
            }
            case 6: {
                if (this.a == 3008) {
                    GLStateRenderThread.AlphaTest.set(false);
                    break;
                }
                if (this.a == 3042) {
                    GLStateRenderThread.Blend.set(false);
                    break;
                }
                if (this.a == 2929) {
                    GLStateRenderThread.DepthTest.set(false);
                    break;
                }
                if (this.a == 3089) {
                    GLStateRenderThread.ScissorTest.set(false);
                    break;
                }
                if (this.a == 2960) {
                    GLStateRenderThread.StencilTest.set(false);
                    break;
                }
                IndieGL.glDisableA(this.a);
                break;
            }
            case 28: {
                GL14.glBlendEquation(this.a);
                break;
            }
            case 37: {
                Core.getInstance().DoPushIsoStuff2D(this.u0, this.u1, this.u2, this.u3, this.a == 1);
                break;
            }
            case 38: {
                Core.getInstance().DoPopIsoStuff();
                break;
            }
            case 31: {
                SpriteRenderer.RingBuffer.ignoreStyles = this.a == 1;
                break;
            }
            case 47: {
                GenericDrawer genericDrawer;
                if (this.drawer == null || !((genericDrawer = this.drawer) instanceof ModelSlotRenderData)) break;
                ModelSlotRenderData modelSlotRenderData = (ModelSlotRenderData)genericDrawer;
                TextureDraw.DrawQueued(modelSlotRenderData);
                break;
            }
            case 48: {
                RenderList.RenderOpaque();
                RenderList.Reset();
                break;
            }
            case 50: {
                this.probe.start();
                break;
            }
            case 51: {
                this.probe.end();
            }
        }
    }

    private static void DrawQueued(ModelSlotRenderData slot) {
        slot.checkReady();
        if (!slot.canRender()) {
            return;
        }
        ModelInstanceRenderDataList modelData = slot.getModelData();
        for (int i = 0; i < modelData.size(); ++i) {
            ModelInstanceRenderData inst = (ModelInstanceRenderData)modelData.get(i);
            RenderList.DrawQueued(slot, inst);
        }
    }

    public static void glDepthMask(TextureDraw textureDraw, boolean b) {
        textureDraw.type = Type.glDepthMask;
        textureDraw.a = b ? 1 : 0;
    }

    public static void doCoreIntParam(TextureDraw textureDraw, int id, float val) {
        textureDraw.type = Type.doCoreIntParam;
        textureDraw.a = id;
        textureDraw.f1 = val;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "{ " + String.valueOf((Object)this.type) + ", a:" + this.a + ", b:" + this.b + ", f1:" + this.f1 + ", vars:" + (this.vars != null ? PZArrayUtil.arrayToString(this.vars, "{", "}", ", ") : "null") + ", c:" + this.c + ", d:" + this.d + ", col0:" + this.col0 + ", col1:" + this.col1 + ", col2:" + this.col2 + ", col3:" + this.col3 + ", x0:" + this.x0 + ", x1:" + this.x1 + ", x2:" + this.x2 + ", x3:" + this.x3 + ", x0:" + this.x0 + ", x1:" + this.x1 + ", x2:" + this.x2 + ", x3:" + this.x3 + ", y0:" + this.y0 + ", y1:" + this.y1 + ", y2:" + this.y2 + ", y3:" + this.y3 + ", u0:" + this.u0 + ", u1:" + this.u1 + ", u2:" + this.u2 + ", u3:" + this.u3 + ", v0:" + this.v0 + ", v1:" + this.v1 + ", v2:" + this.v2 + ", v3:" + this.v3 + ", tex:" + String.valueOf(this.tex) + ", tex1:" + String.valueOf(this.tex1) + ", useAttribArray:" + this.useAttribArray + ", tex1_u0:" + this.tex1U0 + ", tex1_u1:" + this.tex1U1 + ", tex1_u2:" + this.tex1U2 + ", tex1_u3:" + this.tex1U3 + ", tex1_v0:" + this.tex1V0 + ", tex1_v1:" + this.tex1V1 + ", tex1_v2:" + this.tex1V2 + ", tex1_v3:" + this.tex1V3 + ", tex1_col0:" + this.tex1Col0 + ", tex1_col1:" + this.tex1Col1 + ", tex1_col2:" + this.tex1Col2 + ", tex1_col3:" + this.tex1Col3 + ", tex2_u0:" + this.tex2U0 + ", tex2_u1:" + this.tex2U1 + ", tex2_u2:" + this.tex2U2 + ", tex2_u3:" + this.tex2U3 + ", tex2_v0:" + this.tex2V0 + ", tex2_v1:" + this.tex2V1 + ", tex2_v2:" + this.tex2V2 + ", tex2_v3:" + this.tex2V3 + ", bSingleCol:" + this.singleCol + " }";
    }

    public static TextureDraw Create(TextureDraw texd, Texture tex, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        int col0 = Color.colorToABGR(r, g, b, a);
        TextureDraw.Create(texd, tex, x, y, x + width, y, x + width, y + height, x, y + height, col0, col0, col0, col0, texdModifier);
        return texd;
    }

    public static TextureDraw Create(TextureDraw texd, Texture tex, SpriteRenderer.WallShaderTexRender wallSection, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        float y2;
        float y3;
        float x1;
        float x2;
        float y0;
        float y1;
        float x0;
        float x3;
        int col0 = Color.colorToABGR(r, g, b, a);
        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 1.0f;
        float v1 = 0.0f;
        float u2 = 1.0f;
        float v2 = 1.0f;
        float u3 = 0.0f;
        float v3 = 1.0f;
        switch (wallSection) {
            case LeftOnly: {
                x0 = x3 = x;
                y0 = y1 = y;
                x1 = x2 = x + width / 2.0f;
                y2 = y3 = y + height;
                if (tex == null) break;
                float xend = tex.getXEnd();
                float xstart = tex.getXStart();
                float yend = tex.getYEnd();
                float ystart = tex.getYStart();
                float uspan = 0.5f * (xend - xstart);
                u0 = xstart;
                u1 = xstart + uspan;
                u2 = xstart + uspan;
                u3 = xstart;
                v0 = ystart;
                v1 = ystart;
                v2 = yend;
                v3 = yend;
                break;
            }
            case RightOnly: {
                x0 = x3 = x + width / 2.0f;
                y0 = y1 = y;
                x1 = x2 = x + width;
                y2 = y3 = y + height;
                if (tex == null) break;
                float xend = tex.getXEnd();
                float xstart = tex.getXStart();
                float yend = tex.getYEnd();
                float ystart = tex.getYStart();
                float uspan = 0.5f * (xend - xstart);
                u0 = xstart + uspan;
                u1 = xend;
                u2 = xend;
                u3 = xstart + uspan;
                v0 = ystart;
                v1 = ystart;
                v2 = yend;
                v3 = yend;
                break;
            }
            default: {
                x0 = x3 = x;
                y0 = y1 = y;
                x1 = x2 = x + width;
                y2 = y3 = y + height;
                if (tex == null) break;
                float xend = tex.getXEnd();
                float xstart = tex.getXStart();
                float yend = tex.getYEnd();
                float ystart = tex.getYStart();
                u0 = xstart;
                u1 = xend;
                u2 = xend;
                u3 = xstart;
                v0 = ystart;
                v1 = ystart;
                v2 = yend;
                v3 = yend;
            }
        }
        TextureDraw.Create(texd, tex, x0, y0, x1, y1, x2, y2, x3, y3, col0, col0, col0, col0, u0, v0, u1, v1, u2, v2, u3, v3, texdModifier);
        texd.z = nextZ;
        texd.chunkDepth = nextChunkDepth;
        nextZ = 0.0f;
        nextChunkDepth = 0.0f;
        return texd;
    }

    public static TextureDraw Create(TextureDraw texd, Texture tex, float x, float y, float width, float height, float r, float g, float b, float a, float u1, float v1, float u2, float v2, float u3, float v3, float u4, float v4, Consumer<TextureDraw> texdModifier) {
        int col = Color.colorToABGR(r, g, b, a);
        TextureDraw.Create(texd, tex, x, y, x + width, y, x + width, y + height, x, y + height, col, col, col, col, u1, v1, u2, v2, u3, v3, u4, v4, texdModifier);
        return texd;
    }

    public static void Create(TextureDraw texd, Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, float r3, float g3, float b3, float a3, float r4, float g4, float b4, float a4, Consumer<TextureDraw> texdModifier) {
        int col0 = Color.colorToABGR(r1, g1, b1, a1);
        int col1 = Color.colorToABGR(r2, g2, b2, a2);
        int col2 = Color.colorToABGR(r3, g3, b3, a3);
        int col3 = Color.colorToABGR(r4, g4, b4, a4);
        TextureDraw.Create(texd, tex, x1, y1, x2, y2, x3, y3, x4, y4, col0, col1, col2, col3, texdModifier);
    }

    public static void Create(TextureDraw texd, Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r1, float g1, float b1, float a1) {
        int col0 = Color.colorToABGR(r1, g1, b1, a1);
        TextureDraw.Create(texd, tex, x1, y1, x2, y2, x3, y3, x4, y4, col0, col0, col0, col0, null);
    }

    public static void Create(TextureDraw texd, Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int c1, int c2, int c3, int c4) {
        TextureDraw.Create(texd, tex, x1, y1, x2, y2, x3, y3, x4, y4, c1, c2, c3, c4, null);
    }

    public static TextureDraw Create(TextureDraw texd, Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int c1, int c2, int c3, int c4, Consumer<TextureDraw> texdModifier) {
        float u1 = 0.0f;
        float v1 = 0.0f;
        float u2 = 1.0f;
        float v2 = 0.0f;
        float u3 = 1.0f;
        float v3 = 1.0f;
        float u4 = 0.0f;
        float v4 = 1.0f;
        if (tex != null) {
            float xend = tex.getXEnd();
            float xstart = tex.getXStart();
            float yend = tex.getYEnd();
            float ystart = tex.getYStart();
            u1 = xstart;
            v1 = ystart;
            u2 = xend;
            v2 = ystart;
            u3 = xend;
            v3 = yend;
            u4 = xstart;
            v4 = yend;
        }
        return TextureDraw.Create(texd, tex, x1, y1, x2, y2, x3, y3, x4, y4, c1, c2, c3, c4, u1, v1, u2, v2, u3, v3, u4, v4, texdModifier);
    }

    public static TextureDraw Create(TextureDraw texd, Texture tex, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int c0, int c1, int c2, int c3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3, Consumer<TextureDraw> texdModifier) {
        texd.singleCol = c0 == c1 && c0 == c2 && c0 == c3;
        texd.tex = tex;
        texd.x0 = x0;
        texd.y0 = y0;
        texd.x1 = x1;
        texd.y1 = y1;
        texd.x2 = x2;
        texd.y2 = y2;
        texd.x3 = x3;
        texd.y3 = y3;
        texd.col0 = c0;
        texd.col1 = c1;
        texd.col2 = c2;
        texd.col3 = c3;
        texd.u0 = u0;
        texd.u1 = u1;
        texd.u2 = u2;
        texd.u3 = u3;
        texd.v0 = v0;
        texd.v1 = v1;
        texd.v2 = v2;
        texd.v3 = v3;
        if (tex != null) {
            texd.flipped = tex.flip;
        }
        if (texdModifier != null) {
            texdModifier.accept(texd);
            texd.singleCol = texd.col0 == texd.col1 && texd.col0 == texd.col2 && texd.col0 == texd.col3;
        }
        texd.z = nextZ;
        texd.chunkDepth = nextChunkDepth;
        nextZ = 0.0f;
        nextChunkDepth = 0.0f;
        return texd;
    }

    public int getColor(int i) {
        if (this.singleCol) {
            return this.col0;
        }
        if (i == 0) {
            return this.col0;
        }
        if (i == 1) {
            return this.col1;
        }
        if (i == 2) {
            return this.col2;
        }
        if (i == 3) {
            return this.col3;
        }
        return this.col0;
    }

    public void reset() {
        this.type = Type.glDraw;
        this.flipped = false;
        this.tex = null;
        this.tex1 = null;
        this.tex2 = null;
        this.useAttribArray = (byte)-1;
        this.col0 = -1;
        this.col1 = -1;
        this.col2 = -1;
        this.col3 = -1;
        this.singleCol = true;
        this.y3 = -1.0f;
        this.y2 = -1.0f;
        this.y1 = -1.0f;
        this.y0 = -1.0f;
        this.x3 = -1.0f;
        this.x2 = -1.0f;
        this.x1 = -1.0f;
        this.x0 = -1.0f;
        this.z = 0.0f;
        this.chunkDepth = 0.0f;
        this.drawer = null;
        this.future = null;
        this.probe = null;
    }

    public static void glLoadIdentity(TextureDraw textureDraw) {
        textureDraw.type = Type.glLoadIdentity;
    }

    public static void glGenerateMipMaps(TextureDraw textureDraw, int a) {
        textureDraw.type = Type.glGenerateMipMaps;
        textureDraw.a = a;
    }

    public static void glBind(TextureDraw textureDraw, int a) {
        textureDraw.type = Type.glBind;
        textureDraw.a = a;
    }

    public static void glViewport(TextureDraw textureDraw, int x, int y, int width, int height) {
        textureDraw.type = Type.glViewport;
        textureDraw.a = x;
        textureDraw.b = y;
        textureDraw.c = width;
        textureDraw.d = height;
    }

    public static void DrawQueued(TextureDraw texd, ModelManager.ModelSlot model) {
        texd.type = Type.DrawQueued;
        texd.a = model.id;
        ModelSlotRenderData slotData = ModelSlotRenderData.alloc();
        slotData.initModel(model);
        texd.drawer = slotData;
        if (DebugOptions.instance.threadModelSlotInit.getValue()) {
            texd.future = slotInitExec.submit(() -> {
                ModelSlotRenderData modelSlotRenderData = slotData;
                synchronized (modelSlotRenderData) {
                    slotData.init(model);
                }
            });
        } else {
            slotData.init(model);
        }
    }

    public static void RenderQueued(TextureDraw texd) {
        texd.type = Type.RenderQueued;
    }

    public static void BeginProfile(TextureDraw texd, PerformanceProfileProbe probe) {
        texd.type = Type.BeginProfile;
        texd.probe = probe;
    }

    public static void EndProfile(TextureDraw texd, PerformanceProfileProbe probe) {
        texd.type = Type.EndProfile;
        texd.probe = probe;
    }

    public void postRender() {
        Shader shader;
        if (this.type == Type.StartShader && (shader = Shader.ShaderMap.get(this.a)) != null) {
            shader.postRender(this);
        }
        if (this.drawer != null) {
            if (this.future != null) {
                try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("Wait");){
                    this.GetFutureResultNoThrow();
                }
            }
            this.drawer.postRender();
            this.drawer = null;
        }
        this.tex = null;
        this.tex1 = null;
    }

    private void GetFutureResultThrow() {
        try {
            this.future.get();
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
        finally {
            this.future = null;
        }
    }

    private void GetFutureResultNoThrow() {
        try {
            this.future.get();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        finally {
            this.future = null;
        }
    }

    static {
        slotInitExec = Executors.newFixedThreadPool(8);
    }

    public static enum Type {
        glDraw,
        glBuffer,
        glStencilFunc,
        glAlphaFunc,
        glStencilOp,
        glEnable,
        glDisable,
        glColorMask,
        glStencilMask,
        glClear,
        glBindFramebuffer,
        glBlendFunc,
        glDoStartFrame,
        glDoStartFrameText,
        glDoEndFrame,
        glTexParameteri,
        StartShader,
        glLoadIdentity,
        glGenerateMipMaps,
        glBind,
        glViewport,
        DrawModel,
        DrawSkyBox,
        DrawWater,
        DrawPuddles,
        DrawParticles,
        ShaderUpdate,
        BindActiveTexture,
        glBlendEquation,
        glDoStartFrameFx,
        glDoEndFrameFx,
        glIgnoreStyles,
        glClearColor,
        glBlendFuncSeparate,
        glDepthMask,
        doCoreIntParam,
        drawTerrain,
        pushIsoView,
        popIsoView,
        FBORenderChunkEnd,
        FBORenderChunkStart,
        glDoStartFrameNoZoom,
        glDoStartFrameFlipY,
        releaseFBORenderChunkLock,
        glDepthFunc,
        glClearDepth,
        NewFrame,
        DrawQueued,
        RenderQueued,
        DrawImGui,
        BeginProfile,
        EndProfile;

    }

    public static abstract class GenericDrawer {
        public abstract void render();

        public void render(TextureDraw texd) {
            this.render();
        }

        public void postRender() {
        }
    }
}

