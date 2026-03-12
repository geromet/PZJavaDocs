/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import org.lwjgl.util.Rectangle;
import zombie.core.Color;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.ParticleRectangle;
import zombie.iso.weather.fx.SteppedUpdateFloat;

public abstract class WeatherParticle {
    protected ParticleRectangle parent;
    protected Rectangle bounds;
    protected Texture texture;
    protected Color color = Color.white;
    protected Vector2 position = new Vector2(0.0f, 0.0f);
    protected Vector2 velocity = new Vector2(0.0f, 0.0f);
    protected float alpha = 1.0f;
    protected float speed;
    protected SteppedUpdateFloat alphaFadeMod = new SteppedUpdateFloat(0.0f, 0.1f, 0.0f, 1.0f);
    protected float renderAlpha;
    protected float oWidth;
    protected float oHeight;
    protected float zoomMultiW;
    protected float zoomMultiH;
    protected boolean recalcSizeOnZoom;
    protected float lastZoomMod = -1.0f;

    public WeatherParticle(Texture texture) {
        this.texture = texture;
        this.bounds = new Rectangle(0, 0, texture.getWidth(), texture.getHeight());
        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
    }

    public WeatherParticle(Texture texture, int w, int h) {
        this.texture = texture;
        this.bounds = new Rectangle(0, 0, w, h);
        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
    }

    protected void setParent(ParticleRectangle parent) {
        this.parent = parent;
    }

    public void update(float delta) {
        this.update(delta, true);
    }

    public void update(float delta, boolean doBounds) {
        this.alphaFadeMod.update(delta);
        if (this.position.x > (float)this.parent.getWidth()) {
            this.position.x -= (float)((int)(this.position.x / (float)this.parent.getWidth()) * this.parent.getWidth());
        } else if (this.position.x < 0.0f) {
            this.position.x -= (float)((int)((this.position.x - (float)this.parent.getWidth()) / (float)this.parent.getWidth()) * this.parent.getWidth());
        }
        if (this.position.y > (float)this.parent.getHeight()) {
            this.position.y -= (float)((int)(this.position.y / (float)this.parent.getHeight()) * this.parent.getHeight());
        } else if (this.position.y < 0.0f) {
            this.position.y -= (float)((int)((this.position.y - (float)this.parent.getHeight()) / (float)this.parent.getHeight()) * this.parent.getHeight());
        }
        if (doBounds) {
            this.bounds.setLocation((int)this.position.x - this.bounds.getWidth() / 2, (int)this.position.y - this.bounds.getHeight() / 2);
        }
    }

    protected boolean updateZoomSize() {
        if (this.recalcSizeOnZoom && this.lastZoomMod != IsoWeatherFX.zoomMod) {
            this.lastZoomMod = IsoWeatherFX.zoomMod;
            this.oWidth = this.bounds.getWidth();
            this.oHeight = this.bounds.getHeight();
            if (this.lastZoomMod > 0.0f) {
                this.oWidth *= 1.0f + IsoWeatherFX.zoomMod * this.zoomMultiW;
                this.oHeight *= 1.0f + IsoWeatherFX.zoomMod * this.zoomMultiH;
            }
            return true;
        }
        return false;
    }

    public boolean isOnScreen(float offsetx, float offsety) {
        int screenW = IsoCamera.frameState.offscreenWidth;
        int screenH = IsoCamera.frameState.offscreenHeight;
        float x1 = offsetx + (float)this.bounds.getX();
        float y1 = offsety + (float)this.bounds.getY();
        float x2 = x1 + this.oWidth;
        float y2 = y1 + this.oHeight;
        if (x1 >= (float)screenW || x2 <= 0.0f) {
            return false;
        }
        return !(y1 >= (float)screenH) && !(y2 <= 0.0f);
    }

    public void render(float offsetx, float offsety) {
        if (PerformanceSettings.fboRenderChunk) {
            IsoWeatherFX.instance.getDrawer(this.parent.id).addParticle(this.texture, offsetx + (float)this.bounds.getX(), offsety + (float)this.bounds.getY(), this.oWidth, this.oHeight, this.color.r, this.color.g, this.color.b, this.renderAlpha);
            return;
        }
        SpriteRenderer.instance.render(this.texture, offsetx + (float)this.bounds.getX(), offsety + (float)this.bounds.getY(), this.oWidth, this.oHeight, this.color.r, this.color.g, this.color.b, this.renderAlpha, null);
    }
}

