/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.SpritePadding;
import zombie.iso.sprite.shapers.SpritePaddingSettings;

public class FloorShaperAttachedSprites
extends FloorShaper {
    public static final FloorShaperAttachedSprites instance = new FloorShaperAttachedSprites();

    @Override
    public void accept(TextureDraw ddraw) {
        super.accept(ddraw);
        if (PerformanceSettings.fboRenderChunk) {
            return;
        }
        this.applyAttachedSpritesPadding(ddraw);
    }

    private void applyAttachedSpritesPadding(TextureDraw ddraw) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.isoPaddingAttached.getValue()) {
            return;
        }
        Settings settings = this.getSettings();
        Settings.ASBorderSetting setting = settings.getCurrentZoomSetting();
        float borderThicknessUp = setting.borderThicknessUp;
        float borderThicknessDown = setting.borderThicknessDown;
        float borderThicknessLR = setting.borderThicknessLeftRight;
        float uvFraction = setting.uvFraction;
        float width = ddraw.x1 - ddraw.x0;
        float height = ddraw.y2 - ddraw.y1;
        float uWidth = ddraw.u1 - ddraw.u0;
        float vHeight = ddraw.v2 - ddraw.v1;
        float xb = borderThicknessLR;
        float yUp = borderThicknessUp;
        float yDown = borderThicknessDown;
        float uBorder = uWidth * xb / width;
        float vUp = vHeight * yUp / height;
        float vDown = vHeight * yDown / height;
        float ub = uvFraction * uBorder;
        float vbUp = uvFraction * vUp;
        float vbDown = uvFraction * vDown;
        SpritePadding.applyPadding(ddraw, xb, yUp, xb, yDown, ub, vbUp, ub, vbDown);
    }

    private Settings getSettings() {
        return SpritePaddingSettings.getSettings().attachedSprites;
    }

    @XmlType(name="FloorShaperAttachedSpritesSettings")
    public static class Settings
    extends SpritePaddingSettings.GenericZoomBasedSettingGroup {
        @XmlElement(name="ZoomedIn")
        public ASBorderSetting zoomedIn = new ASBorderSetting(2.0f, 1.0f, 3.0f, 0.01f);
        @XmlElement(name="NotZoomed")
        public ASBorderSetting notZoomed = new ASBorderSetting(2.0f, 1.0f, 3.0f, 0.01f);
        @XmlElement(name="ZoomedOut")
        public ASBorderSetting zoomedOut = new ASBorderSetting(2.0f, 0.0f, 2.5f, 0.0f);

        public ASBorderSetting getCurrentZoomSetting() {
            return Settings.getCurrentZoomSetting(this.zoomedIn, this.notZoomed, this.zoomedOut);
        }

        @XmlType(name="ASBorderSetting")
        public static class ASBorderSetting {
            @XmlElement(name="borderThicknessUp")
            public float borderThicknessUp;
            @XmlElement(name="borderThicknessDown")
            public float borderThicknessDown;
            @XmlElement(name="borderThicknessLR")
            public float borderThicknessLeftRight;
            @XmlElement(name="uvFraction")
            public float uvFraction;

            public ASBorderSetting() {
            }

            public ASBorderSetting(float borderThicknessUp, float borderThicknessDown, float borderThicknessLeftRight, float uvFraction) {
                this.borderThicknessUp = borderThicknessUp;
                this.borderThicknessDown = borderThicknessDown;
                this.borderThicknessLeftRight = borderThicknessLeftRight;
                this.uvFraction = uvFraction;
            }
        }
    }
}

