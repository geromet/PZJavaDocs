/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import zombie.core.textures.Texture;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherParticle;

public class CloudParticle
extends WeatherParticle {
    private double angleRadians;
    private float lastAngle = -1.0f;
    private final float lastIntensity = -1.0f;
    protected float angleOffset;
    private float alphaMod;
    private float tmpAngle;

    public CloudParticle(Texture texture) {
        super(texture);
    }

    public CloudParticle(Texture texture, int w, int h) {
        super(texture, w, h);
    }

    @Override
    public void update(float delta) {
        if (this.lastAngle != IsoWeatherFX.instance.windAngleClouds || -1.0f != IsoWeatherFX.instance.windIntensity.value()) {
            this.tmpAngle = IsoWeatherFX.instance.windAngleClouds;
            if (this.tmpAngle > 360.0f) {
                this.tmpAngle -= 360.0f;
            }
            if (this.tmpAngle < 0.0f) {
                this.tmpAngle += 360.0f;
            }
            this.angleRadians = Math.toRadians(this.tmpAngle);
            this.velocity.set((float)Math.cos(this.angleRadians) * this.speed, (float)Math.sin(this.angleRadians) * this.speed);
            this.lastAngle = IsoWeatherFX.instance.windAngleClouds;
        }
        this.position.x += this.velocity.x * IsoWeatherFX.instance.windSpeedFog * delta;
        this.position.y += this.velocity.y * IsoWeatherFX.instance.windSpeedFog * delta;
        super.update(delta);
        this.alphaMod = IsoWeatherFX.instance.cloudIntensity.value() * 0.3f;
        this.renderAlpha = this.alpha * this.alphaMod * this.alphaFadeMod.value() * IsoWeatherFX.instance.indoorsAlphaMod.value();
    }
}

