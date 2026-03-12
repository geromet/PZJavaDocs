/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import zombie.GameWindow;
import zombie.MainThreadQueueException;
import zombie.MainThreadQueueItem;
import zombie.core.ThreadGroups;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.Lambda;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;

public class MainThread {
    public static Thread mainThread;
    private static Runnable mainThreadStart;
    private static Runnable mainThreadLoop;
    private static Runnable mainThreadExit;
    private static Thread.UncaughtExceptionHandler mainThreadExceptionHandler;
    public static final Object m_contextLock;
    private static final ArrayList<MainThreadQueueItem> invokeOnMainQueue;
    private static final ArrayList<MainThreadQueueItem> invokeOnMainQueue_Invoking;
    private static boolean isInitialized;
    private static final Object m_initLock;
    private static volatile boolean isCloseRequested;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Thread init(Runnable mainThreadStart, Runnable mainThreadLoop, Runnable mainThreadExit, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        Object object = m_initLock;
        synchronized (object) {
            if (isInitialized) {
                return mainThread;
            }
            MainThread.mainThreadStart = mainThreadStart;
            MainThread.mainThreadLoop = mainThreadLoop;
            MainThread.mainThreadExit = mainThreadExit;
            mainThreadExceptionHandler = uncaughtExceptionHandler;
            mainThread = new Thread(ThreadGroups.Main, MainThread::mainLoop, "MainThread");
            mainThread.setUncaughtExceptionHandler(MainThread::uncaughtException);
            isInitialized = true;
            mainThread.start();
        }
        return mainThread;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void mainLoop() {
        Object object;
        if (!isInitialized) {
            throw new IllegalStateException("MainThread is not initialized.");
        }
        mainThreadStart.run();
        while (!RenderThread.isCloseRequested() && !GameWindow.closeRequested) {
            object = m_contextLock;
            synchronized (object) {
                MainThread.flushInvokeQueue();
                DebugOptions.testThreadCrash(0);
                mainThreadLoop.run();
            }
            isCloseRequested = RenderThread.isCloseRequested() || GameWindow.closeRequested;
            Thread.yield();
        }
        object = m_initLock;
        synchronized (object) {
            mainThread = null;
            isInitialized = false;
        }
        mainThreadExit.run();
        isCloseRequested = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void uncaughtException(Thread thread2, Throwable e) {
        try {
            mainThreadExceptionHandler.uncaughtException(thread2, e);
        }
        finally {
            DebugLog.General.error("  Notifying InvokeOnMainQueue...");
            ArrayList<MainThreadQueueItem> arrayList = invokeOnMainQueue;
            synchronized (arrayList) {
                invokeOnMainQueue_Invoking.addAll(invokeOnMainQueue);
                invokeOnMainQueue.clear();
            }
            PZArrayUtil.forEach(invokeOnMainQueue_Invoking, MainThreadQueueItem::notifyWaitingListeners);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void flushInvokeQueue() {
        block9: {
            ArrayList<MainThreadQueueItem> arrayList = invokeOnMainQueue;
            synchronized (arrayList) {
                if (!invokeOnMainQueue.isEmpty()) {
                    invokeOnMainQueue_Invoking.addAll(invokeOnMainQueue);
                    invokeOnMainQueue.clear();
                }
            }
            try {
                if (invokeOnMainQueue_Invoking.isEmpty()) break block9;
                long start = System.nanoTime();
                while (!invokeOnMainQueue_Invoking.isEmpty()) {
                    MainThreadQueueItem item = invokeOnMainQueue_Invoking.remove(0);
                    item.invoke();
                    long endJob = System.nanoTime();
                    if (!((double)(endJob - start) > 1.0E7)) continue;
                    break;
                }
                for (int i = invokeOnMainQueue_Invoking.size() - 1; i >= 0; --i) {
                    MainThreadQueueItem item = invokeOnMainQueue_Invoking.get(i);
                    if (!item.isWaiting()) continue;
                    while (i >= 0) {
                        MainThreadQueueItem item1 = invokeOnMainQueue_Invoking.remove(0);
                        item1.invoke();
                        --i;
                    }
                    break;
                }
            }
            catch (Exception ex) {
                DebugLog.General.error("Thrown an " + ex.getClass().getTypeName() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static void invokeOnMainThread(Runnable toInvoke) throws MainThreadQueueException {
        MainThreadQueueItem queueItem = MainThreadQueueItem.alloc(toInvoke);
        queueItem.setWaiting();
        MainThread.queueInvokeOnMainThread(queueItem);
        try {
            queueItem.waitUntilFinished(() -> !isCloseRequested && !GameWindow.gameThreadExited);
        }
        catch (InterruptedException ex) {
            DebugLog.General.error("Thread Interrupted while waiting for queued item to finish:" + String.valueOf(queueItem));
        }
        Throwable t = queueItem.getThrown();
        if (t != null) {
            throw new MainThreadQueueException(t);
        }
    }

    public static boolean invokeQueryOnMainThread(Invokers.Params0.Boolean.ICallback callback) {
        if (mainThread == Thread.currentThread()) {
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
        MainThread.invokeOnMainThread(invoker);
        boolean result = invoker.getAsBoolean();
        invoker.release();
        return result;
    }

    public static <T1> void invokeOnMainThread(T1 val1, Invokers.Params1.ICallback<T1> invoker) {
        Lambda.capture(val1, invoker, (stack, lVal1, lInvoker) -> MainThread.invokeOnMainThread(stack.invoker(lVal1, lInvoker)));
    }

    public static <T1, T2> void invokeOnMainThread(T1 val1, T2 val2, Invokers.Params2.ICallback<T1, T2> invoker) {
        Lambda.capture(val1, val2, invoker, (stack, lVal1, lVal2, lInvoker) -> MainThread.invokeOnMainThread(stack.invoker(lVal1, lVal2, lInvoker)));
    }

    public static <T1, T2, T3> void invokeOnMainThread(T1 val1, T2 val2, T3 val3, Invokers.Params3.ICallback<T1, T2, T3> invoker) {
        Lambda.capture(val1, val2, val3, invoker, (stack, lVal1, lVal2, lVal3, lInvoker) -> MainThread.invokeOnMainThread(stack.invoker(lVal1, lVal2, lVal3, lInvoker)));
    }

    public static <T1, T2, T3, T4> void invokeOnMainThread(T1 val1, T2 val2, T3 val3, T4 val4, Invokers.Params4.ICallback<T1, T2, T3, T4> invoker) {
        Lambda.capture(val1, val2, val3, val4, invoker, (stack, lVal1, lVal2, lVal3, lVal4, lInvoker) -> MainThread.invokeOnMainThread(stack.invoker(lVal1, lVal2, lVal3, lVal4, lInvoker)));
    }

    public static void queueInvokeOnMainThread(Runnable runnable2) {
        MainThread.queueInvokeOnMainThread(MainThreadQueueItem.alloc(runnable2));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void queueInvokeOnMainThread(MainThreadQueueItem queueItem) {
        ArrayList<MainThreadQueueItem> arrayList;
        if (!isInitialized) {
            arrayList = m_initLock;
            synchronized (arrayList) {
                if (!isInitialized) {
                    queueItem.invoke();
                    return;
                }
            }
        }
        if (mainThread == Thread.currentThread()) {
            queueItem.invoke();
            return;
        }
        arrayList = invokeOnMainQueue;
        synchronized (arrayList) {
            invokeOnMainQueue.add(queueItem);
        }
    }

    public static void shutdown() {
    }

    public static boolean isRunning() {
        return isInitialized;
    }

    public static void busyWait() {
        if (Thread.currentThread() != GameWindow.gameThread) {
            return;
        }
        MainThread.flushInvokeQueue();
    }

    static {
        m_contextLock = "MainThread borrowContext Lock";
        invokeOnMainQueue = new ArrayList();
        invokeOnMainQueue_Invoking = new ArrayList();
        m_initLock = "MainThread Initialization Lock";
    }
}

