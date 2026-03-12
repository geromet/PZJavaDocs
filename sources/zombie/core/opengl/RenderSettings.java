/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.SandboxOptions;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SearchMode;
import zombie.iso.weather.ClimateColorInfo;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.WorldFlares;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public final class RenderSettings {
    private static RenderSettings instance;
    private static Texture texture;
    private static final float AMBIENT_MIN_SHADER = 0.4f;
    private static final float AMBIENT_MAX_SHADER = 1.0f;
    private static final float AMBIENT_MIN_LEGACY = 0.4f;
    private static final float AMBIENT_MAX_LEGACY = 1.0f;
    private final PlayerRenderSettings[] playerSettings = new PlayerRenderSettings[4];
    private final Color defaultClear = new Color(0, 0, 0, 1);

    public static RenderSettings getInstance() {
        if (instance == null) {
            instance = new RenderSettings();
        }
        return instance;
    }

    public RenderSettings() {
        for (int i = 0; i < this.playerSettings.length; ++i) {
            this.playerSettings[i] = new PlayerRenderSettings();
        }
        texture = Texture.getSharedTexture("media/textures/weather/fogwhite.png");
        if (texture == null) {
            DebugLog.log("Missing texture: media/textures/weather/fogwhite.png");
        }
    }

    public PlayerRenderSettings getPlayerSettings(int playerIndex) {
        return this.playerSettings[playerIndex];
    }

    public void update() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < 4; ++i) {
            if (IsoPlayer.players[i] == null) continue;
            this.playerSettings[i].updateRenderSettings(i, IsoPlayer.players[i]);
        }
    }

    public void applyRenderSettings(int playerIndex) {
        if (GameServer.server) {
            return;
        }
        this.getPlayerSettings(playerIndex).applyRenderSettings(playerIndex);
    }

    public void legacyPostRender(int playerIndex) {
        if (GameServer.server) {
            return;
        }
        if (SceneShaderStore.weatherShader == null || Core.getInstance().getOffscreenBuffer() == null) {
            this.getPlayerSettings(playerIndex).legacyPostRender(playerIndex);
        }
    }

    public float getAmbientForPlayer(int plrIndex) {
        PlayerRenderSettings plrSettings = this.getPlayerSettings(plrIndex);
        if (plrSettings != null) {
            return plrSettings.getAmbient();
        }
        return 0.0f;
    }

    public Color getMaskClearColorForPlayer(int plrIndex) {
        PlayerRenderSettings plrSettings = this.getPlayerSettings(plrIndex);
        if (plrSettings != null) {
            return plrSettings.getMaskClearColor();
        }
        return this.defaultClear;
    }

    public static class PlayerRenderSettings {
        public ClimateColorInfo cmGlobalLight = new ClimateColorInfo();
        public float cmNightStrength;
        public float cmDesaturation;
        public float cmGlobalLightIntensity;
        public float cmAmbient;
        public float cmViewDistance;
        public float cmDayLightStrength;
        public float cmFogIntensity;
        private final Color blendColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
        private final ColorInfo blendInfo = new ColorInfo();
        private float blendIntensity;
        private float desaturation;
        private float darkness;
        private float night;
        private float viewDistance;
        private float ambient;
        private boolean applyNightVisionGoggles;
        private float goggleMod;
        private boolean isExterior;
        private float fogMod = 1.0f;
        private float rmod;
        private float gmod;
        private float bmod;
        private float smRadius;
        private float smAlpha;
        private float drunkFactor;
        private float blurFactor;
        private final Color maskClearColor = new Color(0, 0, 0, 1);

        private void updateRenderSettings(int playerIndex, IsoPlayer player) {
            SearchMode searchMode = SearchMode.getInstance();
            this.smAlpha = 0.0f;
            this.smRadius = 0.0f;
            ClimateManager cm = ClimateManager.getInstance();
            this.cmGlobalLight = cm.getGlobalLight();
            this.cmGlobalLightIntensity = cm.getGlobalLightIntensity();
            this.cmAmbient = cm.getAmbient();
            this.cmDayLightStrength = cm.getDayLightStrength();
            this.cmNightStrength = cm.getNightStrength();
            this.cmDesaturation = cm.getDesaturation();
            this.cmViewDistance = cm.getViewDistance();
            this.cmFogIntensity = cm.getFogIntensity();
            cm.getThunderStorm().applyLightningForPlayer(this, playerIndex, player);
            WorldFlares.applyFlaresForPlayer(this, playerIndex, player);
            this.desaturation = this.cmDesaturation;
            this.viewDistance = this.cmViewDistance;
            this.applyNightVisionGoggles = player != null && player.isWearingNightVisionGoggles();
            this.isExterior = player != null && (player.isDead() || player.getCurrentSquare() != null && !player.getCurrentSquare().isInARoom());
            this.fogMod = 1.0f - this.cmFogIntensity * 0.5f;
            this.night = this.cmNightStrength;
            this.darkness = 1.0f - this.cmDayLightStrength;
            this.isExterior = true;
            if (this.isExterior) {
                this.setBlendColor(this.cmGlobalLight.getExterior());
                this.blendIntensity = this.cmGlobalLight.getExterior().a;
            } else {
                this.setBlendColor(this.cmGlobalLight.getInterior());
                this.blendIntensity = this.cmGlobalLight.getInterior().a;
            }
            this.ambient = this.cmAmbient;
            this.viewDistance = this.cmViewDistance;
            int sv = SandboxOptions.instance.nightDarkness.getValue();
            float ambientMin = switch (sv) {
                case 1 -> 0.0f;
                case 2 -> 0.07f;
                case 3 -> 0.15f;
                case 4 -> 0.25f;
                default -> 0.15f;
            };
            ambientMin += 0.075f * ClimateMoon.getInstance().getMoonFloat() * this.night;
            if (!this.isExterior) {
                ambientMin *= 0.925f - 0.075f * this.darkness;
                this.desaturation *= 0.25f;
            }
            if (this.ambient < 0.2f && player != null && player.hasTrait(CharacterTrait.NIGHT_VISION)) {
                this.ambient = 0.2f;
            }
            this.ambient = ambientMin + (1.0f - ambientMin) * this.ambient;
            if (Core.lastStand) {
                this.ambient = 0.65f;
                this.darkness = 0.25f;
                this.night = 0.25f;
            }
            if (SceneShaderStore.weatherShader != null && Core.getInstance().getOffscreenBuffer() != null) {
                if (this.applyNightVisionGoggles) {
                    this.ambient = 1.0f;
                    this.rmod = GameTime.getInstance().Lerp(1.0f, 0.7f, this.darkness);
                    this.gmod = GameTime.getInstance().Lerp(1.0f, 0.7f, this.darkness);
                    this.bmod = GameTime.getInstance().Lerp(1.0f, 0.7f, this.darkness);
                    this.maskClearColor.r = 0.0f;
                    this.maskClearColor.g = 0.0f;
                    this.maskClearColor.b = 0.0f;
                    this.maskClearColor.a = 0.0f;
                } else {
                    this.desaturation *= 1.0f - this.darkness;
                    this.blendInfo.r = this.blendColor.r;
                    this.blendInfo.g = this.blendColor.g;
                    this.blendInfo.b = this.blendColor.b;
                    this.blendInfo.desaturate(this.desaturation);
                    this.rmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.r, this.blendIntensity);
                    this.gmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.g, this.blendIntensity);
                    this.bmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.b, this.blendIntensity);
                    if (!this.isExterior) {
                        this.maskClearColor.r = 0.0f;
                        this.maskClearColor.g = 0.0f;
                        this.maskClearColor.b = 0.0f;
                        this.maskClearColor.a = 0.0f;
                    } else {
                        this.maskClearColor.r = 0.0f;
                        this.maskClearColor.g = 0.0f;
                        this.maskClearColor.b = 0.0f;
                        this.maskClearColor.a = 0.0f;
                    }
                }
            } else {
                this.desaturation *= 1.0f - this.darkness;
                this.blendInfo.r = this.blendColor.r;
                this.blendInfo.g = this.blendColor.g;
                this.blendInfo.b = this.blendColor.b;
                this.blendInfo.desaturate(this.desaturation);
                this.rmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.r, this.blendIntensity);
                this.gmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.g, this.blendIntensity);
                this.bmod = GameTime.getInstance().Lerp(1.0f, this.blendInfo.b, this.blendIntensity);
                if (this.applyNightVisionGoggles) {
                    this.goggleMod = 1.0f - 0.9f * this.darkness;
                    this.blendIntensity = 0.0f;
                    this.night = 0.0f;
                    this.ambient = 0.8f;
                    this.rmod = 1.0f;
                    this.gmod = 1.0f;
                    this.bmod = 1.0f;
                }
            }
            if (player != null && !player.isDead()) {
                this.drunkFactor = player.getStats().get(CharacterStat.INTOXICATION) / 100.0f;
                this.blurFactor = player.getBlurFactor();
            }
        }

        private void applyRenderSettings(int playerIndex) {
            IsoGridSquare.rmod = this.rmod;
            IsoGridSquare.gmod = this.gmod;
            IsoGridSquare.bmod = this.bmod;
            IsoObject.rmod = this.rmod;
            IsoObject.gmod = this.gmod;
            IsoObject.bmod = this.bmod;
        }

        private void legacyPostRender(int plr) {
            SpriteRenderer.instance.glIgnoreStyles(true);
            if (this.applyNightVisionGoggles) {
                IndieGL.glBlendFunc(770, 768);
                SpriteRenderer.instance.render(texture, 0.0f, 0.0f, Core.getInstance().getOffscreenWidth(plr), Core.getInstance().getOffscreenHeight(plr), 0.05f, 0.95f, 0.05f, this.goggleMod, null);
                IndieGL.glBlendFunc(770, 771);
            } else {
                IndieGL.glBlendFunc(774, 774);
                SpriteRenderer.instance.render(texture, 0.0f, 0.0f, Core.getInstance().getOffscreenWidth(plr), Core.getInstance().getOffscreenHeight(plr), this.blendInfo.r, this.blendInfo.g, this.blendInfo.b, 1.0f, null);
                IndieGL.glBlendFunc(770, 771);
            }
            SpriteRenderer.instance.glIgnoreStyles(false);
        }

        public Color getBlendColor() {
            return this.blendColor;
        }

        public float getBlendIntensity() {
            return this.blendIntensity;
        }

        public float getDesaturation() {
            return this.desaturation;
        }

        public float getDarkness() {
            return this.darkness;
        }

        public float getNight() {
            return this.night;
        }

        public float getViewDistance() {
            return this.viewDistance;
        }

        public float getAmbient() {
            return this.ambient;
        }

        public boolean isApplyNightVisionGoggles() {
            return this.applyNightVisionGoggles;
        }

        public float getRmod() {
            return this.rmod;
        }

        public float getGmod() {
            return this.gmod;
        }

        public float getBmod() {
            return this.bmod;
        }

        public boolean isExterior() {
            return this.isExterior;
        }

        public float getFogMod() {
            return this.fogMod;
        }

        private void setBlendColor(Color c) {
            this.blendColor.a = c.a;
            this.blendColor.r = c.r;
            this.blendColor.g = c.g;
            this.blendColor.b = c.b;
        }

        public Color getMaskClearColor() {
            return this.maskClearColor;
        }

        public float getSM_Radius() {
            return this.smRadius;
        }

        public float getSM_Alpha() {
            return this.smAlpha;
        }

        public float getDrunkFactor() {
            return this.drunkFactor;
        }

        public float getBlurFactor() {
            return this.blurFactor;
        }
    }
}

