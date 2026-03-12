/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDFrame;
import zombie.core.textures.Texture;

public final class AnimatedTexture {
    private final AnimatedTextureID textureId;
    private int frameNumber;
    private final ArrayList<Texture> textures = new ArrayList();
    private long renderTimeMs;
    private long lastRenderTimeMs;

    public AnimatedTexture(AnimatedTextureID textureId) {
        this.textureId = textureId;
    }

    public boolean isReady() {
        return this.textureId.isReady();
    }

    void initTextures() {
        if (!this.textures.isEmpty()) {
            return;
        }
        if (!this.textureId.isReady()) {
            return;
        }
        for (int i = 0; i < this.textureId.frames.size(); ++i) {
            AnimatedTextureIDFrame frame = this.textureId.frames.get(i);
            Texture texture = new Texture(frame.textureId, this.textureId.getPath().getPath() + "#" + (i + 1));
            this.textures.add(texture);
        }
    }

    public int getWidth() {
        if (this.textureId.isReady()) {
            return this.textureId.getWidth();
        }
        return -1;
    }

    public int getHeight() {
        if (this.textureId.isReady()) {
            return this.textureId.getHeight();
        }
        return -1;
    }

    public void renderToWidth(int x, int y, int width, float r, float g, float b, float a) {
        int height = width;
        if (!this.textureId.isReady()) {
            Texture.getErrorTexture().render(x, y, width, height, r, g, b, a, null);
            return;
        }
        this.initTextures();
        AnimatedTextureIDFrame frame = this.textureId.getFrame(this.frameNumber);
        if (frame == null) {
            Texture.getErrorTexture().render(x, y, width, height, r, g, b, a, null);
            return;
        }
        float scale = (float)width / (float)this.textureId.getWidth();
        height = (int)((float)this.textureId.getHeight() * scale);
        this.render(x, y, width, height, r, g, b, a);
    }

    public void render(int x, int y, int width, int height, float r, float g, float b, float a) {
        if (!this.textureId.isReady()) {
            Texture.getErrorTexture().render(x, y, width, height, r, g, b, a, null);
            return;
        }
        this.initTextures();
        AnimatedTextureIDFrame frame = this.textureId.getFrame(this.frameNumber);
        if (frame == null) {
            Texture.getErrorTexture().render(x, y, width, height, r, g, b, a, null);
            return;
        }
        Texture texture = this.textures.get(this.frameNumber);
        boolean bFullCompositedFrame = true;
        texture.render(x, y, width, height, r, g, b, a, null);
        long currentMS = System.currentTimeMillis();
        long dt = this.lastRenderTimeMs == 0L ? 0L : currentMS - this.lastRenderTimeMs;
        this.lastRenderTimeMs = currentMS;
        short numerator = frame.apngFrame.delayNum;
        int denominator = frame.apngFrame.delayDen;
        if (denominator == 0) {
            denominator = 100;
        }
        if (numerator == 0) {
            this.frameNumber = (this.frameNumber + 1) % this.textureId.getFrameCount();
        } else {
            this.renderTimeMs += dt;
            float msPerTick = 1000.0f / (float)denominator;
            long duration = (long)((float)(this.textureId.getFrameCount() * numerator) * msPerTick);
            while (this.renderTimeMs >= duration) {
                this.renderTimeMs -= duration;
            }
            float percent = (float)this.renderTimeMs / (float)duration;
            this.frameNumber = PZMath.clamp((int)(percent * (float)this.textureId.getFrameCount()), 0, this.textureId.getFrameCount() - 1);
        }
    }
}

