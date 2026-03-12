/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import java.io.IOException;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.LWJGLException;
import org.lwjglx.input.Controllers;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.OpenGLException;
import org.lwjglx.opengl.Util;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.core.Clipboard;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderContextQueueException;
import zombie.core.opengl.RenderContextQueueItem;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.TextureID;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoPuddles;
import zombie.network.GameServer;
import zombie.ui.FPSGraph;
import zombie.util.Lambda;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public class RenderThread {
    public static Thread renderThread;
    private static Thread contextThread;
    private static boolean isDisplayCreated;
    private static int contextLockReentrantDepth;
    public static final Object m_contextLock;
    private static final ArrayList<RenderContextQueueItem> invokeOnRenderQueue;
    private static final ArrayList<RenderContextQueueItem> invokeOnRenderQueue_Invoking;
    private static boolean isInitialized;
    private static final Object m_initLock;
    private static volatile boolean isCloseRequested;
    private static volatile int displayWidth;
    private static volatile int displayHeight;
    private static volatile boolean renderingEnabled;
    private static volatile boolean waitForRenderState;
    private static volatile boolean hasContext;
    private static boolean cursorVisible;
    private static long renderTime;
    private static long startWaitTime;
    private static long waitTime;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void init() throws IOException, LWJGLException {
        Object object = m_initLock;
        synchronized (object) {
            if (isInitialized) {
                return;
            }
            renderThread = Thread.currentThread();
            displayWidth = Display.getWidth();
            displayHeight = Display.getHeight();
            isInitialized = true;
            if (!GameServer.server) {
                GameWindow.InitDisplay();
                Controllers.create();
                Clipboard.initMainThread();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void initServerGUI() {
        Object object = m_initLock;
        synchronized (object) {
            if (isInitialized) {
                return;
            }
            renderThread = new Thread(ThreadGroups.Main, RenderThread::renderLoop, "RenderThread Main Loop");
            renderThread.setName("Render Thread");
            renderThread.setUncaughtExceptionHandler(RenderThread::uncaughtException);
            displayWidth = Display.getWidth();
            displayHeight = Display.getHeight();
            isInitialized = true;
        }
        renderThread.start();
    }

    public static long getRenderTime() {
        return renderTime;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void renderLoop() {
        if (!isInitialized) {
            throw new IllegalStateException("RenderThread is not initialized.");
        }
        RenderThread.acquireContextReentrant();
        boolean isAlive = true;
        while (isAlive) {
            long startTime = System.nanoTime();
            if (startWaitTime == 0L) {
                startWaitTime = startTime;
            }
            Object object = m_contextLock;
            synchronized (object) {
                if (!hasContext) {
                    RenderThread.acquireContextReentrant();
                }
                displayWidth = Display.getWidth();
                displayHeight = Display.getHeight();
                if (renderingEnabled) {
                    try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderStep.profile();){
                        RenderThread.renderStep();
                    }
                } else if (isDisplayCreated && hasContext) {
                    Display.processMessages();
                }
                RenderThread.flushInvokeQueue();
                if (renderingEnabled) {
                    GameWindow.GameInput.poll();
                    Mouse.poll();
                    GameKeyboard.poll();
                    isCloseRequested = isCloseRequested || Display.isCloseRequested();
                } else {
                    isCloseRequested = false;
                }
                if (!GameServer.server) {
                    Clipboard.updateMainThread();
                }
                DebugOptions.testThreadCrash(0);
                isAlive = !GameWindow.gameThreadExited;
            }
            renderTime = System.nanoTime() - startTime;
            Thread.yield();
        }
        RenderThread.releaseContextReentrant();
        Object object = m_initLock;
        synchronized (object) {
            renderThread = null;
            isInitialized = false;
        }
        RenderThread.shutdown();
        System.exit(0);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void uncaughtException(Thread thread2, Throwable e) {
        try {
            GameWindow.uncaughtException(thread2, e);
        }
        finally {
            Runnable forceClose = () -> {
                long timeNow;
                long maxTimeMs = 120000L;
                long timeMs = 0L;
                long timePrev = timeNow = System.currentTimeMillis();
                if (!GameWindow.gameThreadExited) {
                    try {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    DebugLog.General.error("  Waiting for GameThread to exit...");
                    try {
                        Thread.sleep(2000L);
                    }
                    catch (InterruptedException interruptedException) {
                        // empty catch block
                    }
                    while (!GameWindow.gameThreadExited) {
                        Thread.yield();
                        timeNow = System.currentTimeMillis();
                        long timeDiff = timeNow - timePrev;
                        if ((timeMs += timeDiff) >= 120000L) {
                            DebugLog.General.error("  GameThread failed to exit within time limit.");
                            break;
                        }
                        timePrev = timeNow;
                    }
                }
                DebugLog.General.error("  Shutting down...");
                System.exit(1);
            };
            Thread forceCloseThread = new Thread(forceClose, "ForceCloseThread");
            forceCloseThread.start();
            DebugLog.General.error("Shutting down sequence starts.");
            isCloseRequested = true;
            DebugLog.General.error("  Notifying render state queue...");
            RenderThread.notifyRenderStateQueue();
            DebugLog.General.error("  Notifying InvokeOnRenderQueue...");
            ArrayList<RenderContextQueueItem> arrayList = invokeOnRenderQueue;
            synchronized (arrayList) {
                invokeOnRenderQueue_Invoking.addAll(invokeOnRenderQueue);
                invokeOnRenderQueue.clear();
            }
            PZArrayUtil.forEach(invokeOnRenderQueue_Invoking, RenderContextQueueItem::notifyWaitingListeners);
        }
    }

    private static boolean renderStep() {
        boolean result = false;
        try {
            result = RenderThread.lockStepRenderStep();
        }
        catch (OpenGLException glEx) {
            RenderThread.logGLException(glEx);
        }
        catch (Exception ex) {
            DebugLog.General.error("Thrown an " + ex.getClass().getTypeName() + ": " + ex.getMessage());
            ExceptionLogger.logException(ex);
        }
        return result;
    }

    public static long getWaitTime() {
        return waitTime;
    }

    private static boolean lockStepRenderStep() {
        SpriteRenderState renderState = SpriteRenderer.instance.acquireStateForRendering(RenderThread::waitForRenderStateCallback);
        if (renderState == null) {
            RenderThread.notifyRenderStateQueue();
            if (!waitForRenderState || LuaManager.thread != null && LuaManager.thread.step) {
                try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.displayUpdate.profile();){
                    Display.processMessages();
                }
            }
            return true;
        }
        waitTime = System.nanoTime() - startWaitTime;
        startWaitTime = 0L;
        cursorVisible = renderState.cursorVisible;
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.spriteRendererPostRender.profile();){
            SpriteRenderer.instance.postRender();
        }
        abstractPerformanceProfileProbe = s_performance.displayUpdate.profile();
        try {
            Display.update(true);
            RenderThread.checkControllers();
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        if (Core.debug && FPSGraph.instance != null) {
            FPSGraph.instance.addRender(System.currentTimeMillis());
        }
        return true;
    }

    private static void checkControllers() {
    }

    private static boolean waitForRenderStateCallback() {
        RenderThread.flushInvokeQueue();
        return RenderThread.shouldContinueWaiting();
    }

    private static boolean shouldContinueWaiting() {
        return !isCloseRequested && !GameWindow.gameThreadExited && (waitForRenderState || SpriteRenderer.instance.isWaitingForRenderState());
    }

    public static boolean isWaitForRenderState() {
        return waitForRenderState;
    }

    public static void setWaitForRenderState(boolean wait) {
        waitForRenderState = wait;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void flushInvokeQueue() {
        ArrayList<RenderContextQueueItem> arrayList = invokeOnRenderQueue;
        synchronized (arrayList) {
            if (!invokeOnRenderQueue.isEmpty()) {
                PZArrayUtil.addAll(invokeOnRenderQueue_Invoking, invokeOnRenderQueue);
                invokeOnRenderQueue.clear();
            }
        }
        try {
            if (!invokeOnRenderQueue_Invoking.isEmpty()) {
                long start = System.nanoTime();
                while (!invokeOnRenderQueue_Invoking.isEmpty()) {
                    RenderContextQueueItem item = invokeOnRenderQueue_Invoking.remove(0);
                    long startJob = System.nanoTime();
                    item.invoke();
                    long endJob = System.nanoTime();
                    if ((double)(endJob - startJob) > 1.0E7) {
                        boolean bl = true;
                    }
                    if (!((double)(endJob - start) > 1.0E7)) continue;
                    break;
                }
                for (int i = invokeOnRenderQueue_Invoking.size() - 1; i >= 0; --i) {
                    RenderContextQueueItem item = invokeOnRenderQueue_Invoking.get(i);
                    if (!item.isWaiting()) continue;
                    while (i >= 0) {
                        RenderContextQueueItem item1 = invokeOnRenderQueue_Invoking.remove(0);
                        item1.invoke();
                        --i;
                    }
                    break;
                }
            }
            if (TextureID.deleteTextureIDS.position() > 0) {
                TextureID.deleteTextureIDS.flip();
                GL11.glDeleteTextures(TextureID.deleteTextureIDS);
                TextureID.deleteTextureIDS.clear();
            }
        }
        catch (OpenGLException glEx) {
            RenderThread.logGLException(glEx);
        }
        catch (Exception ex) {
            DebugLog.General.error("Thrown an " + ex.getClass().getTypeName() + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void logGLException(OpenGLException glEx) {
        RenderThread.logGLException(glEx, true);
    }

    public static void logGLException(OpenGLException glEx, boolean stackTrace) {
        DebugLog.General.error("OpenGLException thrown: " + glEx.getMessage());
        int extraErrorCode = GL11.glGetError();
        while (extraErrorCode != 0) {
            String errorString = Util.translateGLErrorString(extraErrorCode);
            DebugLog.General.error("  Also detected error: " + errorString + " ( code:" + extraErrorCode + ")");
            extraErrorCode = GL11.glGetError();
        }
        if (stackTrace) {
            DebugLog.General.error("Stack trace:");
            glEx.printStackTrace();
        }
    }

    public static void Ready() {
        SpriteRenderer.instance.pushFrameDown();
        if (!isInitialized) {
            RenderThread.invokeOnRenderContext(RenderThread::renderStep);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void acquireContextReentrant() {
        Object object = m_contextLock;
        synchronized (object) {
            RenderThread.acquireContextReentrantInternal();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void releaseContextReentrant() {
        Object object = m_contextLock;
        synchronized (object) {
            RenderThread.releaseContextReentrantInternal();
        }
    }

    private static void acquireContextReentrantInternal() {
        Thread currentThread = Thread.currentThread();
        if (contextThread != null && contextThread != currentThread) {
            throw new RuntimeException("Context thread mismatch: " + String.valueOf(contextThread) + ", " + String.valueOf(currentThread));
        }
        if (++contextLockReentrantDepth > 1) {
            return;
        }
        contextThread = currentThread;
        isDisplayCreated = Display.isCreated();
        if (isDisplayCreated) {
            try {
                hasContext = true;
                Display.makeCurrent();
                Display.setVSyncEnabled(Core.getInstance().getOptionVSync());
            }
            catch (LWJGLException e) {
                DebugLog.General.error("Exception thrown trying to gain GL context.");
                e.printStackTrace();
            }
        }
    }

    private static void releaseContextReentrantInternal() {
        Thread currentThread = Thread.currentThread();
        if (contextThread != currentThread) {
            throw new RuntimeException("Context thread mismatch: " + String.valueOf(contextThread) + ", " + String.valueOf(currentThread));
        }
        if (contextLockReentrantDepth == 0) {
            throw new RuntimeException("Context thread release overflow: 0: " + String.valueOf(contextThread) + ", " + String.valueOf(currentThread));
        }
        if (--contextLockReentrantDepth > 0) {
            return;
        }
        if (isDisplayCreated && hasContext) {
            try {
                hasContext = false;
                Display.releaseContext();
            }
            catch (LWJGLException e) {
                DebugLog.General.error("Exception thrown trying to release GL context.");
                e.printStackTrace();
            }
        }
        contextThread = null;
    }

    public static void invokeOnRenderContext(Runnable toInvoke) throws RenderContextQueueException {
        RenderContextQueueItem queueItem = RenderContextQueueItem.alloc(toInvoke);
        queueItem.setWaiting();
        RenderThread.queueInvokeOnRenderContext(queueItem);
        try {
            queueItem.waitUntilFinished(() -> {
                RenderThread.notifyRenderStateQueue();
                return !isCloseRequested && !GameWindow.gameThreadExited;
            });
        }
        catch (InterruptedException ex) {
            DebugLog.General.error("Thread Interrupted while waiting for queued item to finish:" + String.valueOf(queueItem));
            RenderThread.notifyRenderStateQueue();
        }
        Throwable t = queueItem.getThrown();
        if (t != null) {
            throw new RenderContextQueueException(t);
        }
    }

    public static boolean invokeQueryOnRenderContext(Invokers.Params0.Boolean.ICallback callback) {
        if (contextThread == Thread.currentThread()) {
            return callback.accept();
        }
        if (!isInitialized) {
            for (int i = 0; i < 0x100000 && !isInitialized && !Thread.interrupted(); ++i) {
                Thread.yield();
            }
            if (!isInitialized) {
                return false;
            }
        }
        Invokers.Params0.Boolean.CallbackStackItem invoker = Lambda.invokerBoolean(callback);
        RenderThread.invokeOnRenderContext(invoker);
        boolean result = invoker.getAsBoolean();
        invoker.release();
        return result;
    }

    public static <T1> void invokeOnRenderContext(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        Lambda.capture(val1, invoker, (stack, lVal1, lInvoker) -> RenderThread.invokeOnRenderContext(stack.invoker(lVal1, lInvoker)));
    }

    public static <T1, T2> void invokeOnRenderContext(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        Lambda.capture(val1, val2, invoker, (stack, lVal1, lVal2, lInvoker) -> RenderThread.invokeOnRenderContext(stack.invoker(lVal1, lVal2, lInvoker)));
    }

    public static <T1, T2, T3> void invokeOnRenderContext(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        Lambda.capture(val1, val2, val3, invoker, (stack, lVal1, lVal2, lVal3, lInvoker) -> RenderThread.invokeOnRenderContext(stack.invoker(lVal1, lVal2, lVal3, lInvoker)));
    }

    public static <T1, T2, T3, T4> void invokeOnRenderContext(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        Lambda.capture(val1, val2, val3, val4, invoker, (stack, lVal1, lVal2, lVal3, lVal4, lInvoker) -> RenderThread.invokeOnRenderContext(stack.invoker(lVal1, lVal2, lVal3, lVal4, lInvoker)));
    }

    protected static void notifyRenderStateQueue() {
        if (SpriteRenderer.instance != null) {
            SpriteRenderer.instance.notifyRenderStateQueue();
        }
    }

    public static void queueInvokeOnRenderContext(Runnable runnable2) {
        RenderThread.queueInvokeOnRenderContext(RenderContextQueueItem.alloc(runnable2));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void queueInvokeOnRenderContext(RenderContextQueueItem queueItem) {
        ArrayList<RenderContextQueueItem> arrayList;
        if (!isInitialized) {
            arrayList = m_initLock;
            synchronized (arrayList) {
                if (!isInitialized) {
                    try {
                        RenderThread.acquireContextReentrant();
                        queueItem.invoke();
                    }
                    finally {
                        RenderThread.releaseContextReentrant();
                    }
                    return;
                }
            }
        }
        if (contextThread == Thread.currentThread()) {
            queueItem.invoke();
            return;
        }
        arrayList = invokeOnRenderQueue;
        synchronized (arrayList) {
            invokeOnRenderQueue.add(queueItem);
        }
    }

    public static void shutdown() {
        GameWindow.GameInput.quit();
        IsoPuddles.getInstance().freeHMTextureBuffer();
        if (isInitialized) {
            RenderThread.queueInvokeOnRenderContext(Display::destroy);
        } else {
            Display.destroy();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isCloseRequested() {
        if (isCloseRequested) {
            DebugType.ExitDebug.debugln("RenderThread.isCloseRequested 1");
            return isCloseRequested;
        }
        if (!isInitialized) {
            Object object = m_initLock;
            synchronized (object) {
                if (!isInitialized && (isCloseRequested = Display.isCloseRequested())) {
                    DebugType.ExitDebug.debugln("RenderThread.isCloseRequested 2");
                }
            }
        }
        return isCloseRequested;
    }

    public static int getDisplayWidth() {
        if (!isInitialized) {
            return Display.getWidth();
        }
        return displayWidth;
    }

    public static int getDisplayHeight() {
        if (!isInitialized) {
            return Display.getHeight();
        }
        return displayHeight;
    }

    public static boolean isRunning() {
        return isInitialized;
    }

    public static void startRendering() {
        renderingEnabled = true;
    }

    public static void onGameThreadExited() {
        DebugLog.General.println("GameThread exited.");
        if (renderThread != null) {
            renderThread.interrupt();
        }
    }

    public static boolean isCursorVisible() {
        return cursorVisible;
    }

    static {
        m_contextLock = "RenderThread borrowContext Lock";
        invokeOnRenderQueue = new ArrayList();
        invokeOnRenderQueue_Invoking = new ArrayList();
        m_initLock = "RenderThread Initialization Lock";
        renderingEnabled = true;
        cursorVisible = true;
    }

    private static class s_performance {
        static final PerformanceProfileFrameProbe renderStep = new PerformanceProfileFrameProbe("RenderThread.renderStep");
        static final PerformanceProfileProbe displayUpdate = new PerformanceProfileProbe("Display.update(true)");
        static final PerformanceProfileProbe spriteRendererPostRender = new PerformanceProfileProbe("SpriteRenderer.postRender");

        private s_performance() {
        }
    }
}

