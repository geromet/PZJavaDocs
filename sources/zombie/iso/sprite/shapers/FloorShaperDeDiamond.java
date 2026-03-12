/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.Color;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.SpritePadding;
import zombie.iso.sprite.shapers.SpritePaddingSettings;

public class FloorShaperDeDiamond
extends FloorShaper {
    public static final FloorShaperDeDiamond instance = new FloorShaperDeDiamond();

    @Override
    public void accept(TextureDraw ddraw) {
        int colTint = this.colTint;
        this.colTint = 0;
        super.accept(ddraw);
        this.applyDeDiamondPadding(ddraw);
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            return;
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            return;
        }
        int col0 = this.col[0];
        int col1 = this.col[1];
        int col2 = this.col[2];
        int col3 = this.col[3];
        int rotatedCol0 = Color.lerpABGR(col0, col3, 0.5f);
        int rotatedCol1 = Color.lerpABGR(col1, col0, 0.5f);
        int rotatedCol2 = Color.lerpABGR(col2, col1, 0.5f);
        int rotatedCol3 = Color.lerpABGR(col3, col2, 0.5f);
        ddraw.col0 = Color.blendBGR(ddraw.col0, rotatedCol0);
        ddraw.col1 = Color.blendBGR(ddraw.col1, rotatedCol1);
        ddraw.col2 = Color.blendBGR(ddraw.col2, rotatedCol2);
        ddraw.col3 = Color.blendBGR(ddraw.col3, rotatedCol3);
        if (colTint != 0) {
            ddraw.col0 = Color.tintABGR(ddraw.col0, colTint);
            ddraw.col1 = Color.tintABGR(ddraw.col1, colTint);
            ddraw.col2 = Color.tintABGR(ddraw.col2, colTint);
            ddraw.col3 = Color.tintABGR(ddraw.col3, colTint);
        }
    }

    private void applyDeDiamondPadding(TextureDraw ddraw) {
        if (PerformanceSettings.fboRenderChunk) {
            return;
        }
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.isoPaddingDeDiamond.getValue()) {
            return;
        }
        Settings settings = this.getSettings();
        Settings.BorderSetting setting = settings.getCurrentZoomSetting();
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
        return SpritePaddingSettings.getSettings().floorDeDiamond;
    }

    @XmlType(name="FloorShaperDeDiamondSettings")
    public static class Settings
    extends SpritePaddingSettings.GenericZoomBasedSettingGroup {
        @XmlElement(name="ZoomedIn")
        public BorderSetting zoomedIn = new BorderSetting(2.0f, 1.0f, 2.0f, 0.01f);
        @XmlElement(name="NotZoomed")
        public BorderSetting notZoomed = new BorderSetting(2.0f, 1.0f, 2.0f, 0.01f);
        @XmlElement(name="ZoomedOut")
        public BorderSetting zoomedOut = new BorderSetting(2.0f, 0.0f, 2.5f, 0.0f);

        public BorderSetting getCurrentZoomSetting() {
            return Settings.getCurrentZoomSetting(this.zoomedIn, this.notZoomed, this.zoomedOut);
        }

        @XmlType(name="BorderSetting")
        public static class BorderSetting {
            @XmlElement(name="borderThicknessUp")
            public float borderThicknessUp = 3.0f;
            @XmlElement(name="borderThicknessDown")
            public float borderThicknessDown = 3.0f;
            @XmlElement(name="borderThicknessLR")
            public float borderThicknessLeftRight;
            @XmlElement(name="uvFraction")
            public float uvFraction = 0.01f;

            public BorderSetting() {
            }

            public BorderSetting(float borderThicknessUp, float borderThicknessDown, float borderThicknessLeftRight, float uvFraction) {
                this.borderThicknessUp = borderThicknessUp;
                this.borderThicknessDown = borderThicknessDown;
                this.borderThicknessLeftRight = borderThicknessLeftRight;
                this.uvFraction = uvFraction;
            }
        }
    }
}

