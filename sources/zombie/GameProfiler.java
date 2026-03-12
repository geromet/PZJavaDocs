/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import zombie.DebugFileWatcher;
import zombie.GameProfileRecording;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.core.profiling.TriggerGameProfilerFile;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.ui.TextManager;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class GameProfiler {
    private static final String s_currentSessionUUID = UUID.randomUUID().toString();
    private static final ThreadLocal<GameProfiler> s_instance = ThreadLocal.withInitial(GameProfiler::new);
    private final Stack<ProfileArea> stack = new Stack();
    private final RecordingFrame currentFrame = new RecordingFrame();
    private final RecordingFrame previousFrame = new RecordingFrame();
    private boolean isInFrame;
    private boolean isRunning;
    private final GameProfileRecording recorder;
    private static final Object m_gameProfilerRecordingTriggerLock = "Game Profiler Recording Watcher, synchronization lock";
    private static PredicatedFileWatcher gameProfilerRecordingTriggerWatcher;
    private static final ArrayList<String> m_validThreadNames;
    private static final int MAX_DEPTH = 20;

    private GameProfiler() {
        String currentThreadName = Thread.currentThread().getName();
        String recordingClassName = currentThreadName.replace("-", "").replace(" ", "");
        String recordingUUID = String.format("%s_GameProfiler_%s", this.getCurrentSessionUUID(), recordingClassName);
        this.recorder = new GameProfileRecording(recordingUUID);
    }

    public static boolean isValidThread() {
        return m_validThreadNames.contains(Thread.currentThread().getName());
    }

    private static void onTrigger_setAnimationRecorderTriggerFile(TriggerGameProfilerFile triggerXml) {
        DebugOptions.instance.gameProfilerEnabled.setValue(triggerXml.isRecording);
    }

    private String getCurrentSessionUUID() {
        return s_currentSessionUUID;
    }

    public static GameProfiler getInstance() {
        return s_instance.get();
    }

    public void startFrame(String frameInvokerKey) {
        if (this.isInFrame) {
            throw new RuntimeException("Already inside a frame.");
        }
        this.isInFrame = true;
        this.isRunning = DebugOptions.instance.gameProfilerEnabled.getValue();
        if (!this.stack.empty()) {
            throw new RuntimeException("Recording stack should be empty at the start of a frame.");
        }
        if (!this.isRunning) {
            return;
        }
        int frameCount = IsoCamera.frameState.frameCount;
        if (this.currentFrame.frameNo != frameCount) {
            this.previousFrame.transferFrom(this.currentFrame);
            if (this.previousFrame.frameNo != -1) {
                this.recorder.writeLine();
            }
            long timeNs = GameProfiler.getTimeNs();
            this.currentFrame.frameNo = frameCount;
            this.currentFrame.frameInvokerKey = frameInvokerKey;
            this.currentFrame.startTime = timeNs;
            this.recorder.reset();
            this.recorder.setFrameNumber(this.currentFrame.frameNo);
            this.recorder.setStartTime(this.currentFrame.startTime);
        }
    }

    public void endFrame() {
        try {
            if (!this.isInFrame) {
                throw new RuntimeException("Not inside a frame.");
            }
            if (!this.isRunning) {
                return;
            }
            this.currentFrame.endTime = GameProfiler.getTimeNs();
            this.currentFrame.totalTime = this.currentFrame.endTime - this.currentFrame.startTime;
            if (!this.stack.empty()) {
                throw new RuntimeException("Recording stack should be empty at the end of a frame.");
            }
        }
        finally {
            this.isInFrame = false;
            this.isRunning = DebugOptions.instance.gameProfilerEnabled.getValue();
        }
    }

    private boolean checkShouldMeasure() {
        if (!GameProfiler.isRunning()) {
            return false;
        }
        if (!this.isInFrame) {
            DebugLog.General.warn("Not inside in a frame. Find the root caller function for this thread, and add call to invokeAndMeasureFrame.");
            return false;
        }
        return true;
    }

    public static boolean isRunning() {
        return GameProfiler.getInstance().isRunning;
    }

    public @Nullable ProfileArea profile(String key) {
        return this.checkShouldMeasure() ? this.start(key) : null;
    }

    @Deprecated
    public ProfileArea start(String areaKey) {
        if (this.stack.size() >= 20) {
            return null;
        }
        long timeNs = GameProfiler.getTimeNs();
        ProfileArea area = ProfileArea.alloc();
        area.key = areaKey;
        return this.start(area, timeNs);
    }

    private synchronized ProfileArea start(ProfileArea area, long timeNs) {
        area.startTime = timeNs;
        area.depth = this.stack.size();
        if (!this.stack.isEmpty()) {
            ProfileArea parentArea = this.stack.peek();
            parentArea.children.add(area);
        }
        this.stack.push(area);
        return area;
    }

    @Deprecated
    public synchronized void end(ProfileArea area) {
        if (area == null) {
            return;
        }
        area.endTime = GameProfiler.getTimeNs();
        area.total = area.endTime - area.startTime;
        if (this.stack.peek() != area) {
            throw new RuntimeException("Incorrect exit. ProfileArea " + String.valueOf(area) + " is not at the top of the stack: " + String.valueOf(this.stack.peek()));
        }
        this.stack.pop();
        if (this.stack.isEmpty()) {
            this.recorder.logTimeSpan(area);
            area.release();
        }
    }

    private void renderPercent(String label, long time, int x, int y, float r, float g, float b) {
        float tFloat = (float)time / (float)this.previousFrame.totalTime;
        tFloat *= 100.0f;
        tFloat = (float)((int)(tFloat * 10.0f)) / 10.0f;
        TextManager.instance.DrawString(x, y, label, r, g, b, 1.0);
        TextManager.instance.DrawString(x + 300, y, tFloat + "%", r, g, b, 1.0);
    }

    public void render(int x, int y) {
        this.renderPercent(this.previousFrame.frameInvokerKey, this.previousFrame.totalTime, x, y, 1.0f, 1.0f, 1.0f);
    }

    public static long getTimeNs() {
        return System.nanoTime();
    }

    public static void init() {
        GameProfiler.initTriggerWatcher();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void initTriggerWatcher() {
        if (gameProfilerRecordingTriggerWatcher == null) {
            Object object = m_gameProfilerRecordingTriggerLock;
            synchronized (object) {
                if (gameProfilerRecordingTriggerWatcher == null) {
                    gameProfilerRecordingTriggerWatcher = new PredicatedFileWatcher(ZomboidFileSystem.instance.getMessagingDirSub("Trigger_PerformanceProfiler.xml"), TriggerGameProfilerFile.class, GameProfiler::onTrigger_setAnimationRecorderTriggerFile);
                    DebugFileWatcher.instance.add(gameProfilerRecordingTriggerWatcher);
                }
            }
        }
    }

    static {
        m_validThreadNames = new ArrayList();
        m_validThreadNames.add("main");
        m_validThreadNames.add("MainThread");
    }

    public static class RecordingFrame {
        private String frameInvokerKey = "";
        private int frameNo = -1;
        private long startTime;
        private long endTime;
        private long totalTime;

        public void transferFrom(RecordingFrame srcFrame) {
            this.clear();
            this.frameNo = srcFrame.frameNo;
            this.frameInvokerKey = srcFrame.frameInvokerKey;
            this.startTime = srcFrame.startTime;
            this.endTime = srcFrame.endTime;
            this.totalTime = srcFrame.totalTime;
            srcFrame.clear();
        }

        public void clear() {
            this.frameNo = -1;
            this.frameInvokerKey = "";
            this.startTime = 0L;
            this.endTime = 0L;
            this.totalTime = 0L;
        }
    }

    public static class ProfileArea
    extends PooledObject
    implements AutoCloseable {
        public String key;
        public long startTime;
        public long endTime;
        public long total;
        public int depth;
        public float r = 1.0f;
        public float g = 1.0f;
        public float b = 1.0f;
        public final List<ProfileArea> children = new ArrayList<ProfileArea>();
        private static final Pool<ProfileArea> s_pool = new Pool<ProfileArea>(ProfileArea::new);

        @Override
        public void onReleased() {
            super.onReleased();
            this.clear();
        }

        public void clear() {
            this.startTime = 0L;
            this.endTime = 0L;
            this.total = 0L;
            this.depth = 0;
            IPooledObject.release(this.children);
        }

        public static ProfileArea alloc() {
            return s_pool.alloc();
        }

        @Override
        public void close() {
            GameProfiler.getInstance().end(this);
        }
    }
}

