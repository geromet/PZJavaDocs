/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import java.util.function.BooleanSupplier;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;

public final class RenderContextQueueItem {
    private Runnable runnable;
    private boolean isFinished;
    private boolean isWaiting;
    private Throwable runnableThrown;
    private final Object waitLock = "RenderContextQueueItem Wait Lock";

    private RenderContextQueueItem() {
    }

    public static RenderContextQueueItem alloc(Runnable runnable2) {
        RenderContextQueueItem newItem = new RenderContextQueueItem();
        newItem.resetInternal();
        newItem.runnable = runnable2;
        return newItem;
    }

    private void resetInternal() {
        this.runnable = null;
        this.isFinished = false;
        this.runnableThrown = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void waitUntilFinished(BooleanSupplier waitCallback) throws InterruptedException {
        while (!this.isFinished()) {
            if (!waitCallback.getAsBoolean()) {
                return;
            }
            Object object = this.waitLock;
            synchronized (object) {
                if (!this.isFinished()) {
                    this.waitLock.wait();
                }
            }
        }
    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public void setWaiting() {
        this.isWaiting = true;
    }

    public boolean isWaiting() {
        return this.isWaiting;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void invoke() {
        try {
            this.runnableThrown = null;
            this.runnable.run();
        }
        catch (Throwable t) {
            this.runnableThrown = t;
            DebugLog.General.error("%s thrown during invoke().", t.toString());
            ExceptionLogger.logException(t);
        }
        finally {
            Object object = this.waitLock;
            synchronized (object) {
                this.isFinished = true;
                this.waitLock.notifyAll();
            }
        }
    }

    public Throwable getThrown() {
        return this.runnableThrown;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void notifyWaitingListeners() {
        Object object = this.waitLock;
        synchronized (object) {
            this.waitLock.notifyAll();
        }
    }
}

