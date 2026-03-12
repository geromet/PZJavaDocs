/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import java.util.Arrays;
import zombie.core.SpriteRenderer;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherParticle;

public class ParticleRectangle {
    protected static final boolean DEBUG_BOUNDS = false;
    protected int id;
    private final int width;
    private final int height;
    private WeatherParticle[] particles;
    private int particlesToRender;
    private int particlesReqUpdCnt;

    public ParticleRectangle(int id, int w, int h) {
        this.id = id;
        this.width = w;
        this.height = h;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void SetParticles(WeatherParticle[] parts) {
        for (int i = 0; i < parts.length; ++i) {
            parts[i].setParent(this);
        }
        this.particles = parts;
        this.particlesToRender = parts.length;
    }

    public void SetParticlesStrength(float str) {
        this.particlesToRender = (int)((float)this.particles.length * str);
    }

    public boolean requiresUpdate() {
        return this.particlesToRender > 0 || this.particlesReqUpdCnt > 0;
    }

    public void update(float delta) {
        this.particlesReqUpdCnt = 0;
        for (int i = 0; i < this.particles.length; ++i) {
            WeatherParticle p = this.particles[i];
            if (i < this.particlesToRender) {
                p.alphaFadeMod.setTarget(1.0f);
            } else if (i >= this.particlesToRender) {
                p.alphaFadeMod.setTarget(0.0f);
            }
            p.update(delta);
            if (!(p.renderAlpha > 0.0f)) continue;
            ++this.particlesReqUpdCnt;
        }
    }

    public void render() {
        int player = IsoCamera.frameState.playerIndex;
        int screenW = IsoCamera.frameState.offscreenWidth;
        int screenH = IsoCamera.frameState.offscreenHeight;
        int cellsW = (int)Math.ceil(screenW / this.width) + 2;
        int cellsH = (int)Math.ceil(screenH / this.height) + 2;
        int gridOffsetX = IsoCamera.frameState.offX >= 0.0f ? (int)IsoCamera.frameState.offX % this.width : this.width - (int)Math.abs(IsoCamera.frameState.offX) % this.width;
        int gridOffsetY = IsoCamera.frameState.offY >= 0.0f ? (int)IsoCamera.frameState.offY % this.height : this.height - (int)Math.abs(IsoCamera.frameState.offY) % this.height;
        int baseX = -gridOffsetX;
        int baseY = -gridOffsetY;
        SpriteRenderer.instance.StartShader(0, player);
        for (int cy = -1; cy < cellsH; ++cy) {
            for (int cx = -1; cx < cellsW; ++cx) {
                int x = baseX + cx * this.width;
                int y = baseY + cy * this.height;
                if (IsoWeatherFX.debugBounds) {
                    LineDrawer.drawRect(x, y, this.width, this.height, 0.0f, 1.0f, 0.0f, 1.0f, 1);
                }
                for (int i = 0; i < this.particles.length; ++i) {
                    WeatherParticle p = this.particles[i];
                    if (p.renderAlpha <= 0.0f || !p.isOnScreen(x, y)) continue;
                    p.render(x, y);
                    if (!IsoWeatherFX.debugBounds) continue;
                    LineDrawer.drawRect(x + p.bounds.getX(), y + p.bounds.getY(), p.bounds.getWidth(), p.bounds.getHeight(), 0.0f, 0.0f, 1.0f, 0.5f, 1);
                }
            }
        }
    }

    public void Reset() {
        Arrays.fill(this.particles, null);
        this.particles = null;
    }
}

