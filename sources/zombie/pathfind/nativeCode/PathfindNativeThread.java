/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.nativeCode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.ai.astar.Mover;
import zombie.core.logger.ExceptionLogger;
import zombie.pathfind.Path;
import zombie.pathfind.PathNode;
import zombie.pathfind.nativeCode.ChunkUpdateTask;
import zombie.pathfind.nativeCode.IPathfindTask;
import zombie.pathfind.nativeCode.PathFindRequest;
import zombie.pathfind.nativeCode.PathRequestTask;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.pathfind.nativeCode.RequestQueue;
import zombie.pathfind.nativeCode.SquareUpdateTask;

public class PathfindNativeThread
extends Thread {
    public static PathfindNativeThread instance;
    public boolean stop;
    public final Object notifier = new Object();
    public final Object renderLock = new Object();
    protected final ConcurrentLinkedQueue<IPathfindTask> chunkTaskQueue = new ConcurrentLinkedQueue();
    protected final ConcurrentLinkedQueue<SquareUpdateTask> squareTaskQueue = new ConcurrentLinkedQueue();
    protected final ConcurrentLinkedQueue<IPathfindTask> vehicleTaskQueue = new ConcurrentLinkedQueue();
    protected final ConcurrentLinkedQueue<PathRequestTask> requestTaskQueue = new ConcurrentLinkedQueue();
    protected final ConcurrentLinkedQueue<IPathfindTask> taskReturnQueue = new ConcurrentLinkedQueue();
    private final RequestQueue requests = new RequestQueue();
    protected final HashMap<Mover, PathFindRequest> requestMap = new HashMap();
    protected final ConcurrentLinkedQueue<PathFindRequest> requestToMain = new ConcurrentLinkedQueue();
    protected final Path shortestPath = new Path();
    protected final ByteBuffer pathBb = ByteBuffer.allocateDirect(3072);
    private final Sync sync = new Sync();

    PathfindNativeThread() {
        this.pathBb.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void run() {
        while (!this.stop) {
            try {
                this.runInner();
            }
            catch (Throwable t) {
                ExceptionLogger.logException(t);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runInner() {
        this.sync.startFrame();
        Object object = this.renderLock;
        synchronized (object) {
            this.updateThread();
        }
        this.sync.endFrame();
        while (this.shouldWait()) {
            object = this.notifier;
            synchronized (object) {
                try {
                    this.notifier.wait();
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
        }
    }

    private void updateThread() {
        int update = 10;
        IPathfindTask task = this.chunkTaskQueue.poll();
        while (task != null) {
            task.execute();
            this.taskReturnQueue.add(task);
            if (task instanceof ChunkUpdateTask && --update <= 0) break;
            task = this.chunkTaskQueue.poll();
        }
        task = this.squareTaskQueue.poll();
        while (task != null) {
            ((SquareUpdateTask)task).execute();
            this.taskReturnQueue.add(task);
            task = this.squareTaskQueue.poll();
        }
        task = this.vehicleTaskQueue.poll();
        while (task != null) {
            task.execute();
            this.taskReturnQueue.add(task);
            task = this.vehicleTaskQueue.poll();
        }
        task = this.requestTaskQueue.poll();
        while (task != null) {
            ((PathRequestTask)task).execute();
            this.taskReturnQueue.add(task);
            task = this.requestTaskQueue.poll();
        }
        PathfindNative.update();
        int requestsPerUpdate = 2;
        while (!this.requests.isEmpty()) {
            PathFindRequest request = this.requests.removeFirst();
            if (request.cancel) {
                this.requestToMain.add(request);
                continue;
            }
            try {
                this.findPath(request);
                if (!request.targetXyz.isEmpty()) {
                    this.findShortestPathOfMultiple(request);
                }
            }
            catch (Throwable t) {
                ExceptionLogger.logException(t);
                request.path.clear();
            }
            this.requestToMain.add(request);
            if (--requestsPerUpdate != 0) continue;
            break;
        }
    }

    private boolean shouldWait() {
        if (this.stop) {
            return false;
        }
        if (!this.chunkTaskQueue.isEmpty()) {
            return false;
        }
        if (!this.squareTaskQueue.isEmpty()) {
            return false;
        }
        if (!this.vehicleTaskQueue.isEmpty()) {
            return false;
        }
        if (!this.requestTaskQueue.isEmpty()) {
            return false;
        }
        return this.requests.isEmpty();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void wake() {
        Object object = this.notifier;
        synchronized (object) {
            this.notifier.notify();
        }
    }

    public void addRequest(PathFindRequest request, int queueNumber) {
        if (queueNumber == 1) {
            this.requests.playerQ.add(request);
        } else if (queueNumber == 2) {
            this.requests.aggroZombieQ.add(request);
        } else {
            this.requests.otherQ.add(request);
        }
    }

    private void findPath(PathFindRequest request) {
        this.pathBb.clear();
        int result = PathfindNative.instance.findPath(request, this.pathBb, request.doNotRelease);
        int count = 0;
        if (result == 1) {
            count = this.pathBb.getShort();
            this.pathBb.limit(2 + count * 3 * 4);
        }
        request.path.clear();
        for (int i = 0; i < count; ++i) {
            float x = this.pathBb.getFloat();
            float y = this.pathBb.getFloat();
            float z = this.pathBb.getFloat() - 32.0f;
            request.path.addNode(x, y, z);
        }
        if (count == 1) {
            PathNode node = request.path.getNode(0);
            request.path.addNode(node.x, node.y, node.z);
        }
        if (request.path.size() < 2) {
            request.path.clear();
            return;
        }
    }

    private void findShortestPathOfMultiple(PathFindRequest request) {
        this.shortestPath.copyFrom(request.path);
        float targetX = request.targetX;
        float targetY = request.targetY;
        float targetZ = request.targetZ;
        float minLength = this.shortestPath.isEmpty() ? Float.MAX_VALUE : this.shortestPath.length();
        for (int i = 0; i < request.targetXyz.size(); i += 3) {
            float length;
            request.targetX = request.targetXyz.get(i);
            request.targetY = request.targetXyz.get(i + 1);
            request.targetZ = request.targetXyz.get(i + 2);
            request.path.clear();
            this.findPath(request);
            if (request.path.isEmpty() || !((length = request.path.length()) < minLength)) continue;
            minLength = length;
            this.shortestPath.copyFrom(request.path);
            targetX = request.targetX;
            targetY = request.targetY;
            targetZ = request.targetZ;
        }
        request.path.copyFrom(this.shortestPath);
        request.targetX = targetX;
        request.targetY = targetY;
        request.targetZ = targetZ;
    }

    public void stopThread() {
        this.stop = true;
        this.wake();
        while (this.isAlive()) {
            try {
                Thread.sleep(5L);
            }
            catch (InterruptedException interruptedException) {}
        }
    }

    public void cleanup() {
        PathFindRequest request;
        IPathfindTask task = this.chunkTaskQueue.poll();
        while (task != null) {
            task.release();
            task = this.chunkTaskQueue.poll();
        }
        task = this.squareTaskQueue.poll();
        while (task != null) {
            ((SquareUpdateTask)task).release();
            task = this.squareTaskQueue.poll();
        }
        task = this.vehicleTaskQueue.poll();
        while (task != null) {
            task.release();
            task = this.vehicleTaskQueue.poll();
        }
        task = this.requestTaskQueue.poll();
        while (task != null) {
            ((PathRequestTask)task).release();
            task = this.requestTaskQueue.poll();
        }
        task = this.taskReturnQueue.poll();
        while (task != null) {
            task.release();
            task = this.taskReturnQueue.poll();
        }
        while (!this.requests.isEmpty()) {
            request = this.requests.removeLast();
            if (request.doNotRelease) continue;
            request.release();
        }
        while (!this.requestToMain.isEmpty()) {
            request = (PathFindRequest)this.requestToMain.remove();
            if (request.doNotRelease) continue;
            request.release();
        }
        this.requestMap.clear();
    }

    private static class Sync {
        private final int fps = 20;
        private final long period = 50000000L;
        private long excess;
        private long beforeTime = System.nanoTime();
        private long overSleepTime;

        private Sync() {
        }

        void begin() {
            this.beforeTime = System.nanoTime();
            this.overSleepTime = 0L;
        }

        void startFrame() {
            this.excess = 0L;
        }

        void endFrame() {
            long afterTime = System.nanoTime();
            long timeDiff = afterTime - this.beforeTime;
            long sleepTime = 50000000L - timeDiff - this.overSleepTime;
            if (sleepTime > 0L) {
                try {
                    Thread.sleep(sleepTime / 1000000L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                this.overSleepTime = System.nanoTime() - afterTime - sleepTime;
            } else {
                this.excess -= sleepTime;
                this.overSleepTime = 0L;
            }
            this.beforeTime = System.nanoTime();
        }
    }
}

