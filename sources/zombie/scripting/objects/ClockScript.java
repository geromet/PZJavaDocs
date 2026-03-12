/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;

public final class ClockScript
extends BaseScriptObject {
    public String replacementSprite;
    public float handX;
    public float handY;
    public float handZ;
    public boolean north;
    public final HandScript hourHand = new HandScript();
    public final HandScript minuteHand = new HandScript();
    public boolean replacementOnly;

    public ClockScript() {
        super(ScriptType.Clock);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.replacementSprite = null;
        this.handZ = 0.0f;
        this.handY = 0.0f;
        this.handX = 0.0f;
        this.north = false;
        this.replacementOnly = false;
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        boolean bReplacementOnly1 = block.children.isEmpty();
        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("handOffset".equalsIgnoreCase(k)) {
                String[] ss = v.split("\\s+");
                if (ss.length == 3) {
                    this.handX = PZMath.tryParseFloat(ss[0], 0.0f);
                    this.handY = PZMath.tryParseFloat(ss[1], 0.0f);
                    this.handZ = PZMath.tryParseFloat(ss[2], 0.0f);
                }
                bReplacementOnly1 = false;
                continue;
            }
            if ("north".equalsIgnoreCase(k)) {
                this.north = StringUtils.tryParseBoolean(v);
                bReplacementOnly1 = false;
                continue;
            }
            if (!"replacementSprite".equalsIgnoreCase(k)) continue;
            this.replacementSprite = StringUtils.discardNullOrWhitespace(v);
        }
        if (bReplacementOnly1) {
            this.replacementOnly = true;
            return;
        }
        for (ScriptParser.Block child : block.children) {
            if (!"hand".equalsIgnoreCase(child.type)) continue;
            if ("hour".equalsIgnoreCase(child.id)) {
                this.LoadHand(child, this.hourHand);
            }
            if (!"minute".equalsIgnoreCase(child.id)) continue;
            this.LoadHand(child, this.minuteHand);
        }
    }

    private void LoadHand(ScriptParser.Block block, HandScript handScript) {
        handScript.length = 1.0f;
        handScript.thickness = 0.1f;
        handScript.texture = "white.png";
        handScript.textureAxisY = Float.NaN;
        handScript.textureAxisX = Float.NaN;
        handScript.a = 1.0f;
        handScript.b = 1.0f;
        handScript.g = 1.0f;
        handScript.r = 1.0f;
        for (ScriptParser.Value value : block.values) {
            String[] ss;
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("length".equalsIgnoreCase(k)) {
                handScript.length = PZMath.tryParseFloat(v, 1.0f);
                continue;
            }
            if ("texture".equalsIgnoreCase(k)) {
                String texture = StringUtils.discardNullOrWhitespace(v);
                if (texture == null) continue;
                handScript.texture = "media/textures/" + texture + ".png";
                continue;
            }
            if ("textureInfo".equalsIgnoreCase(k)) {
                ss = v.split("\\s+");
                if (ss.length != 4) continue;
                float texW = PZMath.tryParseFloat(ss[0], 128.0f);
                float texH = PZMath.tryParseFloat(ss[1], 128.0f);
                float axisX = PZMath.tryParseFloat(ss[2], 0.5f);
                float axisY = PZMath.tryParseFloat(ss[3], 0.5f);
                handScript.textureAxisX = axisX / texW;
                handScript.textureAxisY = axisY / texH;
                continue;
            }
            if ("thickness".equalsIgnoreCase(k)) {
                handScript.thickness = PZMath.tryParseFloat(v, 0.1f);
                continue;
            }
            if (!"rgba".equalsIgnoreCase(k) || (ss = v.split("\\s+")).length != 4) continue;
            handScript.r = PZMath.tryParseFloat(ss[0], 1.0f);
            handScript.g = PZMath.tryParseFloat(ss[1], 1.0f);
            handScript.b = PZMath.tryParseFloat(ss[2], 1.0f);
            handScript.a = PZMath.tryParseFloat(ss[3], 1.0f);
        }
    }

    public static final class HandScript {
        public float length = 1.0f;
        public float thickness = 0.1f;
        public String texture;
        public float textureAxisX = Float.NaN;
        public float textureAxisY = Float.NaN;
        public float r;
        public float g;
        public float b;
        public float a;
    }
}

