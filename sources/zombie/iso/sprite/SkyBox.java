/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import zombie.GameTime;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugOptions;
import zombie.interfaces.ITexture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.SkyBoxShader;
import zombie.iso.weather.ClimateManager;

public class SkyBox
extends IsoObject {
    private static SkyBox instance;
    public IsoSpriteInstance def;
    private TextureFBO textureFboA;
    private TextureFBO textureFboB;
    private boolean isCurrentA;
    public Shader effect;
    private final UpdateLimit renderLimit = new UpdateLimit(1000L);
    private boolean isUpdated;
    private int skyBoxTime;
    private float skyBoxParamCloudCount;
    private float skyBoxParamCloudSize;
    private final Vector3f skyBoxParamSunLight = new Vector3f();
    private final Color skyBoxParamSunColor = new Color(1.0f, 1.0f, 1.0f);
    private final Color skyBoxParamSkyHColour = new Color(1.0f, 1.0f, 1.0f);
    private final Color skyBoxParamSkyLColour = new Color(1.0f, 1.0f, 1.0f);
    private float skyBoxParamCloudLight;
    private float skyBoxParamStars;
    private float skyBoxParamFog;
    private final Vector3f skyBoxParamWind;
    private boolean isSetAvg;
    private float skyBoxParamCloudCountAvg;
    private float skyBoxParamCloudSizeAvg;
    private final Vector3f skyBoxParamSunLightAvg = new Vector3f();
    private final Color skyBoxParamSunColorAvg = new Color(1.0f, 1.0f, 1.0f);
    private final Color skyBoxParamSkyHColourAvg = new Color(1.0f, 1.0f, 1.0f);
    private final Color skyBoxParamSkyLColourAvg = new Color(1.0f, 1.0f, 1.0f);
    private float skyBoxParamCloudLightAverage;
    private float skyBoxParamStarsAvg;
    private float skyBoxParamFogAvg;
    private final Vector3f skyBoxParamWindInt;
    private final Texture texAm;
    private final Texture texPm;
    private final Color skyHColourDay = new Color(0.1f, 0.1f, 0.4f);
    private final Color skyHColourDawn = new Color(0.2f, 0.2f, 0.3f);
    private final Color skyHColourDusk = new Color(0.2f, 0.2f, 0.3f);
    private final Color skyHColourNight = new Color(0.01f, 0.01f, 0.04f);
    private final Color skyLColourDay = new Color(0.1f, 0.45f, 0.7f);
    private final Color skyLColourDawn = new Color(0.1f, 0.4f, 0.6f);
    private final Color skyLColourDusk = new Color(0.1f, 0.4f, 0.6f);
    private final Color skyLColourNight = new Color(0.01f, 0.045f, 0.07f);
    private int apiId;

    public static synchronized SkyBox getInstance() {
        if (instance == null) {
            instance = new SkyBox();
        }
        return instance;
    }

    public void update(ClimateManager cm) {
        if (this.isUpdated) {
            return;
        }
        this.isUpdated = true;
        GameTime gt = GameTime.getInstance();
        ClimateManager.DayInfo currentDay = cm.getCurrentDay();
        float dawn = currentDay.season.getDawn();
        float dusk = currentDay.season.getDusk();
        float noon = currentDay.season.getDayHighNoon();
        float time = gt.getTimeOfDay();
        if (time < dawn || time > dusk) {
            float total = 24.0f - dusk + dawn;
            if (time > dusk) {
                float t = (time - dusk) / total;
                this.skyHColourDusk.interp(this.skyHColourDawn, t, this.skyBoxParamSkyHColour);
                this.skyLColourDusk.interp(this.skyLColourDawn, t, this.skyBoxParamSkyLColour);
                this.skyBoxParamSunLight.set(0.35f, 0.22f, 0.3f);
                this.skyBoxParamSunLight.normalize();
                this.skyBoxParamSunLight.mul(Math.min(1.0f, t * 5.0f));
            } else {
                float t = (24.0f - dusk + time) / total;
                this.skyHColourDusk.interp(this.skyHColourDawn, t, this.skyBoxParamSkyHColour);
                this.skyLColourDusk.interp(this.skyLColourDawn, t, this.skyBoxParamSkyLColour);
                this.skyBoxParamSunLight.set(0.35f, 0.22f, 0.3f);
                this.skyBoxParamSunLight.normalize();
                this.skyBoxParamSunLight.mul(Math.min(1.0f, (1.0f - t) * 5.0f));
            }
            this.skyBoxParamSunColor.set(cm.getGlobalLight().getExterior());
            this.skyBoxParamSunColor.scale(cm.getNightStrength());
        } else if (time < noon) {
            t = (time - dawn) / (noon - dawn);
            this.skyHColourDawn.interp(this.skyHColourDay, t, this.skyBoxParamSkyHColour);
            this.skyLColourDawn.interp(this.skyLColourDay, t, this.skyBoxParamSkyLColour);
            this.skyBoxParamSunLight.set(4.0f * t - 4.0f, 0.22f, 0.3f);
            this.skyBoxParamSunLight.normalize();
            this.skyBoxParamSunLight.mul(Math.min(1.0f, t * 10.0f));
            this.skyBoxParamSunColor.set(cm.getGlobalLight().getExterior());
        } else {
            t = (time - noon) / (dusk - noon);
            this.skyHColourDay.interp(this.skyHColourDusk, t, this.skyBoxParamSkyHColour);
            this.skyLColourDay.interp(this.skyLColourDusk, t, this.skyBoxParamSkyLColour);
            this.skyBoxParamSunLight.set(4.0f * t, 0.22f, 0.3f);
            this.skyBoxParamSunLight.normalize();
            this.skyBoxParamSunLight.mul(Math.min(1.0f, (1.0f - t) * 10.0f));
            this.skyBoxParamSunColor.set(cm.getGlobalLight().getExterior());
        }
        this.skyBoxParamSkyHColour.interp(this.skyHColourNight, cm.getNightStrength(), this.skyBoxParamSkyHColour);
        this.skyBoxParamSkyLColour.interp(this.skyLColourNight, cm.getNightStrength(), this.skyBoxParamSkyLColour);
        this.skyBoxParamCloudCount = Math.min(Math.max(cm.getCloudIntensity(), cm.getPrecipitationIntensity() * 2.0f), 0.999f);
        this.skyBoxParamCloudSize = 0.02f + cm.getTemperature() / 70.0f;
        this.skyBoxParamFog = cm.getFogIntensity();
        this.skyBoxParamStars = cm.getNightStrength();
        this.skyBoxParamCloudLight = (float)(1.0 - (1.0 - 1.0 * Math.pow(1000.0, -cm.getPrecipitationIntensity() - cm.getNightStrength())));
        float windAngleClouds = (1.0f - (cm.getWindAngleIntensity() + 1.0f) * 0.5f + 0.25f) % 1.0f;
        this.skyBoxParamWind.set((float)Math.cos(Math.toRadians(windAngleClouds *= 360.0f)), 0.0f, (float)Math.sin(Math.toRadians(windAngleClouds)));
        this.skyBoxParamWind.mul(cm.getWindIntensity());
        if (!this.isSetAvg) {
            this.isSetAvg = true;
            this.skyBoxParamCloudCountAvg = this.skyBoxParamCloudCount;
            this.skyBoxParamCloudSizeAvg = this.skyBoxParamCloudSize;
            this.skyBoxParamSunLightAvg.set(this.skyBoxParamSunLight);
            this.skyBoxParamSunColorAvg.set(this.skyBoxParamSunColor);
            this.skyBoxParamSkyHColourAvg.set(this.skyBoxParamSkyHColour);
            this.skyBoxParamSkyLColourAvg.set(this.skyBoxParamSkyLColour);
            this.skyBoxParamCloudLightAverage = this.skyBoxParamCloudLight;
            this.skyBoxParamStarsAvg = this.skyBoxParamStars;
            this.skyBoxParamFogAvg = this.skyBoxParamFog;
            this.skyBoxParamWindInt.set(this.skyBoxParamWind);
        } else {
            this.skyBoxParamCloudCountAvg += (this.skyBoxParamCloudCount - this.skyBoxParamCloudCountAvg) * 0.1f;
            this.skyBoxParamCloudSizeAvg += (this.skyBoxParamCloudSizeAvg + this.skyBoxParamCloudSize) * 0.1f;
            this.skyBoxParamSunLightAvg.lerp(this.skyBoxParamSunLight, 0.1f);
            this.skyBoxParamSunColorAvg.interp(this.skyBoxParamSunColor, 0.1f, this.skyBoxParamSunColorAvg);
            this.skyBoxParamSkyHColourAvg.interp(this.skyBoxParamSkyHColour, 0.1f, this.skyBoxParamSkyHColourAvg);
            this.skyBoxParamSkyLColourAvg.interp(this.skyBoxParamSkyLColour, 0.1f, this.skyBoxParamSkyLColourAvg);
            this.skyBoxParamCloudLightAverage += (this.skyBoxParamCloudLight - this.skyBoxParamCloudLightAverage) * 0.1f;
            this.skyBoxParamStarsAvg += (this.skyBoxParamStars - this.skyBoxParamStarsAvg) * 0.1f;
            this.skyBoxParamFogAvg += (this.skyBoxParamFog - this.skyBoxParamFogAvg) * 0.1f;
            this.skyBoxParamWindInt.add(this.skyBoxParamWind);
        }
    }

    public int getShaderTime() {
        return this.skyBoxTime;
    }

    public float getShaderCloudCount() {
        return this.skyBoxParamCloudCount;
    }

    public float getShaderCloudSize() {
        return this.skyBoxParamCloudSize;
    }

    public Vector3f getShaderSunLight() {
        return this.skyBoxParamSunLight;
    }

    public Color getShaderSunColor() {
        return this.skyBoxParamSunColor;
    }

    public Color getShaderSkyHColour() {
        return this.skyBoxParamSkyHColour;
    }

    public Color getShaderSkyLColour() {
        return this.skyBoxParamSkyLColour;
    }

    public float getShaderCloudLight() {
        return this.skyBoxParamCloudLight;
    }

    public float getShaderStars() {
        return this.skyBoxParamStars;
    }

    public float getShaderFog() {
        return this.skyBoxParamFog;
    }

    public Vector3f getShaderWind() {
        return this.skyBoxParamWindInt;
    }

    public SkyBox() {
        this.texAm = Texture.getSharedTexture("media/textures/CMVehicleReflection/ref_am.png");
        this.texPm = Texture.getSharedTexture("media/textures/CMVehicleReflection/ref_am.png");
        try {
            Texture texA = new Texture(512, 512, 16);
            Texture texB = new Texture(512, 512, 16);
            this.textureFboA = new TextureFBO(texA);
            this.textureFboB = new TextureFBO(texB);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.def = IsoSpriteInstance.get(this.sprite);
        this.skyBoxTime = 0;
        this.skyBoxParamSunLight.set(0.35f, 0.22f, 0.3f);
        this.skyBoxParamSunColor.set(1.0f, 0.86f, 0.7f, 1.0f);
        this.skyBoxParamSkyHColour.set(0.1f, 0.1f, 0.4f, 1.0f);
        this.skyBoxParamSkyLColour.set(0.1f, 0.45f, 0.7f, 1.0f);
        this.skyBoxParamCloudLight = 0.99f;
        this.skyBoxParamCloudCount = 0.3f;
        this.skyBoxParamCloudSize = 0.2f;
        this.skyBoxParamFog = 0.0f;
        this.skyBoxParamStars = 0.0f;
        this.skyBoxParamWind = new Vector3f(0.0f);
        this.skyBoxParamWindInt = new Vector3f(0.0f);
        RenderThread.invokeOnRenderContext(() -> {
            this.effect = Core.getInstance().getPerfSkybox() == 0 ? new SkyBoxShader("skybox_hires") : new SkyBoxShader("skybox");
            if (GL.getCapabilities().OpenGL30) {
                this.apiId = 1;
            }
            if (GL.getCapabilities().GL_ARB_framebuffer_object) {
                this.apiId = 2;
            }
            if (GL.getCapabilities().GL_EXT_framebuffer_object) {
                this.apiId = 3;
            }
        });
    }

    public ITexture getTextureCurrent() {
        if (!Core.getInstance().getUseShaders() || Core.getInstance().getPerfSkybox() == 2) {
            return this.texAm;
        }
        if (this.isCurrentA) {
            return this.textureFboA.getTexture();
        }
        return this.textureFboB.getTexture();
    }

    public ITexture getTexturePrev() {
        if (!Core.getInstance().getUseShaders() || Core.getInstance().getPerfSkybox() == 2) {
            return this.texPm;
        }
        if (this.isCurrentA) {
            return this.textureFboB.getTexture();
        }
        return this.textureFboA.getTexture();
    }

    public TextureFBO getTextureFBOPrev() {
        if (!Core.getInstance().getUseShaders() || Core.getInstance().getPerfSkybox() == 2) {
            return null;
        }
        if (this.isCurrentA) {
            return this.textureFboB;
        }
        return this.textureFboA;
    }

    public float getTextureShift() {
        if (!Core.getInstance().getUseShaders() || Core.getInstance().getPerfSkybox() == 2) {
            return 1.0f - GameTime.getInstance().getNight();
        }
        return (float)this.renderLimit.getTimePeriod();
    }

    public void swapTextureFBO() {
        this.renderLimit.updateTimePeriod();
        this.isCurrentA = !this.isCurrentA;
    }

    public void render() {
        if (!Core.getInstance().getUseShaders() || Core.getInstance().getPerfSkybox() == 2) {
            return;
        }
        if (!this.renderLimit.Check()) {
            if (GameTime.getInstance().getMultiplier() >= 20.0f) {
                ++this.skyBoxTime;
            }
            return;
        }
        ++this.skyBoxTime;
        int nPlayer = IsoCamera.frameState.playerIndex;
        int ox = IsoCamera.getOffscreenLeft(nPlayer);
        int oy = IsoCamera.getOffscreenTop(nPlayer);
        int ow = IsoCamera.getOffscreenWidth(nPlayer);
        int oh = IsoCamera.getOffscreenHeight(nPlayer);
        SpriteRenderer.instance.drawSkyBox(this.effect, nPlayer, this.apiId, this.getTextureFBOPrev().getBufferId());
        this.isUpdated = false;
    }

    public void draw() {
        if (Core.debug && DebugOptions.instance.skyboxShow.getValue()) {
            ((Texture)this.getTextureCurrent()).render(0.0f, 0.0f, 512.0f, 512.0f, 1.0f, 1.0f, 1.0f, 1.0f, null);
        }
    }
}

