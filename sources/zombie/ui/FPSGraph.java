/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.utils.BoundedQueue;
import zombie.input.Mouse;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;

public final class FPSGraph
extends UIElement {
    public static FPSGraph instance;
    private static final int NUM_BARS = 30;
    private static final int BAR_WID = 8;
    private final Graph fpsGraph = new Graph(this);
    private final Graph upsGraph = new Graph(this);
    private final Graph lpsGraph = new Graph(this);
    private final Graph uiGraph = new Graph(this);

    public FPSGraph() {
        this.setVisible(false);
        this.setWidth(232.0);
    }

    public void addRender(long time) {
        while (this.fpsGraph.queue.size() >= 64) {
            this.fpsGraph.queue.poll();
        }
        this.fpsGraph.queue.add(time);
    }

    public void addUpdate(long time) {
        this.upsGraph.add(time);
    }

    public void addLighting(long time) {
        while (this.lpsGraph.queue.size() >= 64) {
            this.lpsGraph.queue.poll();
        }
        this.lpsGraph.queue.add(time);
    }

    public void addUI(long time) {
        this.uiGraph.add(time);
    }

    @Override
    public void update() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        this.setHeight(PZMath.max(108, TextManager.instance.getFontHeight(UIFont.Small) * 4));
        this.setWidth(232.0);
        this.setX(20.0);
        this.setY((double)(Core.getInstance().getScreenHeight() - 20) - this.getHeight());
        super.update();
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        if (!UIManager.visibleAllUi) {
            return;
        }
        int height = this.getHeight().intValue() - 4;
        int bar = -1;
        if (this.isMouseOver().booleanValue()) {
            this.DrawTextureScaledCol(UIElement.white, 0.0, 0.0, this.getWidth(), this.getHeight(), 0.0, 0.2f, 0.0, 0.5);
            int lx = Mouse.getXA() - this.getAbsoluteX().intValue();
            bar = lx / 8;
        }
        int fontHgt = TextManager.instance.getFontHeight(UIFont.Small);
        int textY = (int)(this.getHeight() - (double)(fontHgt * 4)) / 2;
        this.fpsGraph.flushQueue();
        this.fpsGraph.render(0.0f, 1.0f, 0.0f);
        if (bar >= 0 && bar < this.fpsGraph.bars.size()) {
            this.DrawText("FPS: " + this.fpsGraph.bars.get(bar), 20.0, textY + fontHgt, 0.0, 1.0, 0.0, 1.0);
        }
        this.lpsGraph.flushQueue();
        this.lpsGraph.render(1.0f, 1.0f, 0.0f);
        if (bar >= 0 && bar < this.lpsGraph.bars.size()) {
            this.DrawText("LPS: " + this.lpsGraph.bars.get(bar), 20.0, textY + fontHgt * 2, 1.0, 1.0, 0.0, 1.0);
        }
        this.upsGraph.render(0.0f, 1.0f, 1.0f);
        if (bar >= 0 && bar < this.upsGraph.bars.size()) {
            this.DrawText("UPS: " + this.upsGraph.bars.get(bar), 20.0, textY + fontHgt * 3, 0.0, 1.0, 1.0, 1.0);
            this.DrawTextureScaledCol(UIElement.white, bar * 8 + 4, 0.0, 1.0, this.getHeight(), 1.0, 1.0, 1.0, 0.5);
        }
        this.uiGraph.render(1.0f, 0.0f, 1.0f);
        if (bar >= 0 && bar < this.uiGraph.bars.size()) {
            this.DrawText("UI: " + this.uiGraph.bars.get(bar), 20.0, textY, 1.0, 0.0, 1.0, 1.0);
        }
        long free = Runtime.getRuntime().freeMemory() / 0x100000L;
        long total = Runtime.getRuntime().totalMemory() / 0x100000L;
        this.DrawText("Memory (MB): total %d, used %d, free %d, max %d".formatted(total, total - free, free, Runtime.getRuntime().maxMemory() / 0x100000L), 0.0, 0 - TextManager.instance.getFontHeight(UIFont.Small), 1.0, 1.0, 1.0, 1.0);
    }

    private final class Graph {
        private final TLongArrayList times;
        private final BoundedQueue<Long> times2;
        private final TIntArrayList bars;
        private final ConcurrentLinkedQueue<Long> queue;
        final /* synthetic */ FPSGraph this$0;

        private Graph(FPSGraph fPSGraph) {
            FPSGraph fPSGraph2 = fPSGraph;
            Objects.requireNonNull(fPSGraph2);
            this.this$0 = fPSGraph2;
            this.times = new TLongArrayList();
            this.times2 = new BoundedQueue(300);
            this.bars = new TIntArrayList();
            this.queue = new ConcurrentLinkedQueue();
        }

        void flushQueue() {
            Long ms = this.queue.poll();
            while (ms != null) {
                this.add(ms);
                ms = this.queue.poll();
            }
        }

        public void add(long time) {
            this.times.add(time);
            this.bars.resetQuick();
            long start = this.times.get(0);
            int count = 1;
            for (int i = 1; i < this.times.size(); ++i) {
                if (i == this.times.size() - 1 || this.times.get(i) - start > 1000L) {
                    long noUpdates = (this.times.get(i) - start) / 1000L - 1L;
                    int j = 0;
                    while ((long)j < noUpdates) {
                        this.bars.add(0);
                        ++j;
                    }
                    this.bars.add(count);
                    count = 1;
                    start = this.times.get(i);
                    continue;
                }
                ++count;
            }
            while (this.bars.size() > 30) {
                int numTimes = this.bars.get(0);
                for (int i = 0; i < numTimes; ++i) {
                    this.times.removeAt(0);
                }
                this.bars.removeAt(0);
            }
            this.times2.add(time);
        }

        public void render(float r, float g, float b) {
            if (this.bars.isEmpty()) {
                return;
            }
            float height = this.this$0.getHeight().intValue() - 4;
            float bottom = this.this$0.getHeight().intValue() - 2;
            int fps = Math.max(PerformanceSettings.getLockFPS(), PerformanceSettings.lightingFps);
            int barX = 8;
            float lastBarHeight = height * ((float)Math.min(fps, this.bars.get(0)) / (float)fps);
            for (int i = 1; i < this.bars.size() - 1; ++i) {
                float barHeight = height * ((float)Math.min(fps, this.bars.get(i)) / (float)fps);
                SpriteRenderer.instance.renderline(null, this.this$0.getAbsoluteX().intValue() + barX - 8 + 4, this.this$0.getAbsoluteY().intValue() + (int)(bottom - lastBarHeight), this.this$0.getAbsoluteX().intValue() + barX + 4, this.this$0.getAbsoluteY().intValue() + (int)(bottom - barHeight), r, g, b, 0.35f, 1.0f);
                barX += 8;
                lastBarHeight = barHeight;
            }
        }

        public void renderFrameTimes(float r, float g, float b) {
            if (this.times2.isEmpty()) {
                return;
            }
            int barX = 0;
            int numBars = (int)((double)Core.getInstance().getScreenWidth() - this.this$0.getAbsoluteX() * 2.0) / 8;
            numBars = PZMath.min(numBars, this.times2.size());
            for (int i = 0; i < numBars - 1; ++i) {
                long elapsed = this.times2.get(this.times2.size() - numBars + i + 1) - this.times2.get(this.times2.size() - numBars + i);
                float barHeight = elapsed * 10L;
                SpriteRenderer.instance.renderi(null, this.this$0.getAbsoluteX().intValue() + barX, this.this$0.getAbsoluteY().intValue() + this.this$0.getHeight().intValue() - (int)barHeight, 8, (int)barHeight, r, g, b, 0.35f, null);
                barX += 8;
            }
            float dy = 1000.0f / (float)PerformanceSettings.getLockFPS() * 10.0f;
            SpriteRenderer.instance.render(null, this.this$0.getAbsoluteX().intValue(), (int)(this.this$0.getAbsoluteY() + this.this$0.getHeight() - (double)dy), Core.getInstance().getScreenWidth(), 2.0f, 1.0f, 1.0f, 1.0f, 1.0f, null);
        }
    }
}

