/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherParticle;

public class SnowParticle
extends WeatherParticle {
    private double angleRadians;
    private float lastAngle = -1.0f;
    private final float lastIntensity = -1.0f;
    protected float angleOffset;
    private float alphaMod;
    private float incarnateAlpha = 1.0f;
    private float life;
    private final float fadeTime = 80.0f;
    private float tmpAngle;

    public SnowParticle(Texture texture) {
        super(texture);
        this.recalcSizeOnZoom = true;
        this.zoomMultiW = 1.0f;
        this.zoomMultiH = 1.0f;
    }

    protected void setLife() {
        this.life = 80.0f + (float)Rand.Next(60, 500);
    }

    @Override
    public void update(float delta) {
        if (this.lastAngle != IsoWeatherFX.instance.windAngle || -1.0f != IsoWeatherFX.instance.windPrecipIntensity.value()) {
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
        }
        if (this.life >= 80.0f) {
            this.position.x += this.velocity.x * IsoWeatherFX.instance.windSpeed * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
            this.position.y += this.velocity.y * IsoWeatherFX.instance.windSpeed * delta * Core.getInstance().getOptionPrecipitationSpeedMultiplier();
        } else {
            this.incarnateAlpha = this.life / 80.0f;
        }
        this.life -= 1.0f;
        if (this.life < 0.0f) {
            this.setLife();
            this.incarnateAlpha = 0.0f;
            this.position.set(Rand.Next(0, this.parent.getWidth()), Rand.Next(0, this.parent.getHeight()));
        }
        if (this.incarnateAlpha < 1.0f) {
            this.incarnateAlpha += 0.05f;
            if (this.incarnateAlpha > 1.0f) {
                this.incarnateAlpha = 1.0f;
            }
        }
        super.update(delta);
        this.updateZoomSize();
        this.alphaMod = 1.0f - 0.2f * IsoWeatherFX.instance.windIntensity.value();
        this.renderAlpha = this.alpha * this.alphaMod * this.alphaFadeMod.value() * IsoWeatherFX.instance.indoorsAlphaMod.value() * this.incarnateAlpha;
        this.renderAlpha *= 0.7f;
    }
}

