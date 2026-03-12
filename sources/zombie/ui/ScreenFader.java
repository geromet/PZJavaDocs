/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;

public final class ScreenFader {
    private Stage stage = Stage.StartFadeToBlack;
    private float alpha;
    private float targetAlpha = 1.0f;
    private long fadeStartMs;
    private final long fadeDurationMs = 350L;

    public void startFadeToBlack() {
        this.alpha = 0.0f;
        this.stage = Stage.StartFadeToBlack;
    }

    public void startFadeFromBlack() {
        this.alpha = 1.0f;
        this.stage = Stage.StartFadeFromBlack;
    }

    public void update() {
        switch (this.stage.ordinal()) {
            case 0: {
                this.targetAlpha = 1.0f;
                this.stage = Stage.UpdateFadeToBlack;
                this.fadeStartMs = System.currentTimeMillis();
                break;
            }
            case 1: {
                this.alpha = PZMath.clamp((float)(System.currentTimeMillis() - this.fadeStartMs) / 350.0f, 0.0f, 1.0f);
                if (!(this.alpha >= this.targetAlpha)) break;
                this.stage = Stage.Hold;
                break;
            }
            case 2: {
                break;
            }
            case 3: {
                this.targetAlpha = 0.0f;
                this.stage = Stage.UpdateFadeFromBlack;
                this.fadeStartMs = System.currentTimeMillis();
                break;
            }
            case 4: {
                this.alpha = 1.0f - PZMath.clamp((float)(System.currentTimeMillis() - this.fadeStartMs) / 350.0f, 0.0f, 1.0f);
                if (!(this.alpha <= this.targetAlpha)) break;
                this.stage = Stage.Hold;
            }
        }
    }

    public void preRender() {
        Core.getInstance().StartFrame();
        Core.getInstance().EndFrame();
        Core.getInstance().StartFrameUI();
        this.update();
    }

    public void postRender() {
        this.render();
        Core.getInstance().EndFrameUI();
    }

    public void render() {
        int screenW = Core.getInstance().getScreenWidth();
        int screenH = Core.getInstance().getScreenHeight();
        SpriteRenderer.instance.renderi(null, 0, 0, screenW, screenH, 0.0f, 0.0f, 0.0f, this.alpha, null);
    }

    public boolean isFading() {
        return this.stage != Stage.Hold;
    }

    public float getAlpha() {
        return this.alpha;
    }

    private static enum Stage {
        StartFadeToBlack,
        UpdateFadeToBlack,
        Hold,
        StartFadeFromBlack,
        UpdateFadeFromBlack;

    }
}

