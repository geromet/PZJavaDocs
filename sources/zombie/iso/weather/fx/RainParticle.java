/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import java.util.Objects;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherParticle;

public class RainParticle
extends WeatherParticle {
    private double angleRadians;
    private float lastAngle = -1.0f;
    private float lastIntensity = -1.0f;
    protected float angleOffset;
    private float alphaMod;
    private float incarnateAlpha = 1.0f;
    private float life;
    private final RenderPoints rp;
    private boolean angleUpdate;
    private float tmpAngle;

    public RainParticle(Texture texture, int height) {
        super(texture);
        if (height > 6) {
            this.bounds.setSize(Rand.Next(1, 2), height);
        } else {
            this.bounds.setSize(1, height);
        }
        this.oWidth = this.bounds.getWidth();
        this.oHeight = this.bounds.getHeight();
        this.recalcSizeOnZoom = true;
        this.zoomMultiW = 0.0f;
        this.zoomMultiH = 2.0f;
        this.setLife();
        this.rp = new RenderPoints(this);
        this.rp.setDimensions(this.oWidth, this.oHeight);
    }

    protected void setLife() {
        this.life = Rand.Next(20, 60);
    }

    @Override
    public void update(float delta) {
        this.angleUpdate = false;
        if (this.updateZoomSize()) {
            this.rp.setDimensions(this.oWidth, this.oHeight);
            this.angleUpdate = true;
        }
        if (this.angleUpdate || this.lastAngle != IsoWeatherFX.instance.windAngle || this.lastIntensity != IsoWeatherFX.instance.windPrecipIntensity.value()) {
            this.tmpAngle = IsoWeatherFX.instance.windAngle + (this.angleOffset - this.angleOffset * 0.5f * IsoWeatherFX.instance.windPrecipIntensity.value());
            if (this.tmpAngle > 360.0f) {
                this.tmpAngle -= 360.0f;
            }
            if (this.tmpAngle < 0.0f) {
                this.tmpAngle += 360.0f;
            }
            this.angleRadians = Math.toRadians(this.tmpAngle);
            this.velocity.set((float)Math.cos(this.angleRadians) * this.speed, (float)Math.sin(this.angleRadians) * this.speed);
            this.lastAngle = IsoWeatherFX.instance.windAngle;
            this.lastIntensity = IsoWeatherFX.instance.windPrecipIntensity.value();
            this.angleUpdate = true;
        }
        this.position.x += this.velocity.x * (1.0f + IsoWeatherFX.instance.windSpeed * 0.1f) * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        this.position.y += this.velocity.y * (1.0f + IsoWeatherFX.instance.windSpeed * 0.1f) * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        this.life -= 1.0f;
        if (this.life < 0.0f) {
            this.setLife();
            this.incarnateAlpha = 0.0f;
            this.position.set(Rand.Next(0, this.parent.getWidth()), Rand.Next(0, this.parent.getHeight()));
        }
        if (this.incarnateAlpha < 1.0f) {
            this.incarnateAlpha += 0.035f;
            if (this.incarnateAlpha > 1.0f) {
                this.incarnateAlpha = 1.0f;
            }
        }
        this.update(delta, false);
        this.bounds.setLocation((int)this.position.x, (int)this.position.y);
        if (this.angleUpdate) {
            this.tmpAngle += 90.0f;
            if (this.tmpAngle > 360.0f) {
                this.tmpAngle -= 360.0f;
            }
            if (this.tmpAngle < 0.0f) {
                this.tmpAngle += 360.0f;
            }
            this.angleRadians = Math.toRadians(this.tmpAngle);
            this.rp.rotate(this.angleRadians);
        }
        this.alphaMod = 1.0f - 0.2f * IsoWeatherFX.instance.windIntensity.value();
        this.renderAlpha = this.alpha * this.alphaMod * this.alphaFadeMod.value() * IsoWeatherFX.instance.indoorsAlphaMod.value() * this.incarnateAlpha;
        this.renderAlpha *= 0.55f;
        if (IsoWeatherFX.instance.playerIndoors) {
            this.renderAlpha *= 0.5f;
        }
    }

    @Override
    public void render(float offsetx, float offsety) {
        double x = offsetx + (float)this.bounds.getX();
        double y = offsety + (float)this.bounds.getY();
        if (PerformanceSettings.fboRenderChunk) {
            IsoWeatherFX.instance.getDrawer(this.parent.id).addParticle(this.texture, (float)(x + this.rp.getX(0)), (float)(y + this.rp.getY(0)), (float)(x + this.rp.getX(1)), (float)(y + this.rp.getY(1)), (float)(x + this.rp.getX(2)), (float)(y + this.rp.getY(2)), (float)(x + this.rp.getX(3)), (float)(y + this.rp.getY(3)), this.color.r, this.color.g, this.color.b, this.renderAlpha);
            return;
        }
        SpriteRenderer.instance.render(this.texture, x + this.rp.getX(0), y + this.rp.getY(0), x + this.rp.getX(1), y + this.rp.getY(1), x + this.rp.getX(2), y + this.rp.getY(2), x + this.rp.getX(3), y + this.rp.getY(3), this.color.r, this.color.g, this.color.b, this.renderAlpha, null);
    }

    private class RenderPoints {
        Point[] points;
        Point center;
        Point dim;
        final /* synthetic */ RainParticle this$0;

        public RenderPoints(RainParticle rainParticle) {
            RainParticle rainParticle2 = rainParticle;
            Objects.requireNonNull(rainParticle2);
            this.this$0 = rainParticle2;
            this.points = new Point[4];
            this.center = new Point(this.this$0);
            this.dim = new Point(this.this$0);
            for (int i = 0; i < this.points.length; ++i) {
                this.points[i] = new Point(rainParticle);
            }
        }

        public double getX(int i) {
            return this.points[i].x;
        }

        public double getY(int i) {
            return this.points[i].y;
        }

        public void setCenter(float x, float y) {
            this.center.set(x, y);
        }

        public void setDimensions(float w, float h) {
            this.dim.set(w, h);
            this.points[0].setOrig(-w / 2.0f, -h / 2.0f);
            this.points[1].setOrig(w / 2.0f, -h / 2.0f);
            this.points[2].setOrig(w / 2.0f, h / 2.0f);
            this.points[3].setOrig(-w / 2.0f, h / 2.0f);
        }

        public void rotate(double angle) {
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            for (int i = 0; i < this.points.length; ++i) {
                this.points[i].x = this.points[i].origx * cos - this.points[i].origy * sin;
                this.points[i].y = this.points[i].origx * sin + this.points[i].origy * cos;
            }
        }
    }

    private class Point {
        private double origx;
        private double origy;
        private double x;
        private double y;

        private Point(RainParticle rainParticle) {
            Objects.requireNonNull(rainParticle);
        }

        public void setOrig(double x, double y) {
            this.origx = x;
            this.origy = y;
            this.x = x;
            this.y = y;
        }

        public void set(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}

