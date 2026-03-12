/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import org.lwjglx.opengl.OpenGLException;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.ui.UITransition;

public final class ExceptionLogger {
    private static int exceptionCount;
    private static boolean ignore;
    private static final boolean bExceptionPopup = true;
    private static long popupFrameMS;
    private static final UITransition transition;
    private static boolean hide;

    public static synchronized void logException(Throwable ex) {
        ExceptionLogger.logException(ex, null);
    }

    public static synchronized void logException(Throwable ex, String errorMessage) {
        ExceptionLogger.logException(ex, errorMessage, DebugLog.General, LogSeverity.Error);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static synchronized void logException(Throwable ex, String errorMessage, DebugType out, LogSeverity severity) {
        if (ex instanceof OpenGLException) {
            OpenGLException glEx = (OpenGLException)ex;
            RenderThread.logGLException(glEx, false);
        }
        out.getLogStream().printException(ex, errorMessage, DebugLogStream.generateCallerPrefix(), severity);
        try {
            if (ignore) {
                return;
            }
            ignore = true;
            ++exceptionCount;
            if (GameServer.server) {
                return;
            }
            ExceptionLogger.showPopup();
        }
        catch (Throwable ex2) {
            out.printException(ex2, "Exception thrown while trying to logException.", LogSeverity.Error);
        }
        finally {
            ignore = false;
        }
    }

    public static void showPopup() {
        float elapsed = popupFrameMS > 0L ? transition.getElapsed() : 0.0f;
        popupFrameMS = 3000L;
        transition.setIgnoreUpdateTime(true);
        transition.init(500.0f, false);
        transition.setElapsed(elapsed);
        hide = false;
    }

    public static void render() {
        if (UIManager.useUiFbo && !Core.getInstance().uiRenderThisFrame) {
            return;
        }
        boolean force = false;
        if (popupFrameMS <= 0L) {
            return;
        }
        popupFrameMS = (long)((double)popupFrameMS - UIManager.getMillisSinceLastRender());
        transition.update();
        int fontHgt = TextManager.instance.getFontHeight(UIFont.DebugConsole);
        int width = 100;
        int height = fontHgt * 2 + 4;
        int x = Core.getInstance().getScreenWidth() - 100;
        int y = Core.getInstance().getScreenHeight() - (int)((float)height * transition.fraction());
        SpriteRenderer.instance.renderi(null, x, y, 100, height, 0.8f, 0.0f, 0.0f, 1.0f, null);
        SpriteRenderer.instance.renderi(null, x + 1, y + 1, 98, fontHgt - 1, 0.0f, 0.0f, 0.0f, 1.0f, null);
        TextManager.instance.DrawStringCentre(UIFont.DebugConsole, x + 50, y, "ERROR", 1.0, 0.0, 0.0, 1.0);
        TextManager.instance.DrawStringCentre(UIFont.DebugConsole, x + 50, y + fontHgt, Integer.toString(exceptionCount), 0.0, 0.0, 0.0, 1.0);
        if (popupFrameMS <= 0L && !hide) {
            popupFrameMS = 500L;
            transition.init(500.0f, true);
            hide = true;
        }
    }

    static {
        transition = new UITransition();
    }
}

