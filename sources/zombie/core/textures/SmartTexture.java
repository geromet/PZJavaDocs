/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.CharacterSmartTexture;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.SmartShader;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureCombinerCommand;
import zombie.core.textures.TextureCombinerShaderParam;
import zombie.core.utils.WrappedBuffer;
import zombie.debug.DebugLog;
import zombie.util.Lambda;
import zombie.util.list.PZArrayUtil;

public class SmartTexture
extends Texture {
    public final ArrayList<TextureCombinerCommand> commands = new ArrayList();
    public Texture result;
    private boolean dirty = true;
    private static SmartShader hue;
    private static SmartShader tint;
    private static SmartShader masked;
    private static SmartShader dirtMask;
    private final HashMap<Integer, ArrayList<Integer>> categoryMap = new HashMap();
    private static SmartShader bodyMask;
    private static SmartShader bodyMaskTint;
    private static SmartShader bodyMaskHue;
    private static final ArrayList<TextureCombinerShaderParam> bodyMaskParams;
    private static SmartShader addHole;
    private static final ArrayList<TextureCombinerShaderParam> addHoleParams;
    private static SmartShader removeHole;
    private static final ArrayList<TextureCombinerShaderParam> removeHoleParams;
    private static SmartShader blit;

    public SmartTexture() {
        this.name = "SmartTexture";
    }

    void addToCat(int cat) {
        ArrayList<Object> catArr;
        if (!this.categoryMap.containsKey(cat)) {
            catArr = new ArrayList();
            this.categoryMap.put(cat, catArr);
        } else {
            catArr = this.categoryMap.get(cat);
        }
        catArr.add(this.commands.size());
    }

    public TextureCombinerCommand getFirstFromCategory(int cat) {
        if (!this.categoryMap.containsKey(cat)) {
            return null;
        }
        return this.commands.get(this.categoryMap.get(cat).get(0));
    }

    public void addOverlayPatches(String tex, String mask, int category) {
        if (blit == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> params = new ArrayList<TextureCombinerShaderParam>();
        this.add(tex, blit, mask, params, 770, 771);
    }

    public void addOverlay(String tex, String mask, float intensity, int category) {
        if (masked == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> params = new ArrayList<TextureCombinerShaderParam>();
        params.add(new TextureCombinerShaderParam("intensity", intensity));
        params.add(new TextureCombinerShaderParam("bloodDark", 0.5f, 0.5f));
        this.addSeparate(tex, masked, mask, params, 774, 771, 772, 771);
    }

    public void addDirtOverlay(String tex, String mask, float intensity, int category) {
        if (dirtMask == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> params = new ArrayList<TextureCombinerShaderParam>();
        params.add(new TextureCombinerShaderParam("intensity", intensity));
        this.addSeparate(tex, dirtMask, mask, params, 774, 771, 772, 771);
    }

    public void addOverlay(String tex, SmartShader shader) {
        if (tint == null) {
            this.create();
        }
        this.addSeparate(tex, shader, 774, 771, 772, 771);
    }

    public void addTintedOverlay(String tex, String mask, float intensity, int category, float r, float g, float b) {
        if (masked == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> params = new ArrayList<TextureCombinerShaderParam>();
        params.add(new TextureCombinerShaderParam("intensity", intensity));
        params.add(new TextureCombinerShaderParam("bloodDark", 0.5f, 0.5f));
        this.addSeparate(tex, masked, mask, params, 774, 771, 772, 771);
        this.addTint(SmartTexture.getTextureWithFlags(tex), category, r, g, b);
    }

    public void addRect(String tex, int x, int y, int w, int h) {
        if (blit == null) {
            this.create();
        }
        this.commands.add(TextureCombinerCommand.get().init(SmartTexture.getTextureWithFlags(tex), blit, x, y, w, h));
        this.dirty = true;
    }

    @Override
    public void destroy() {
        if (this.result != null) {
            TextureCombiner.instance.releaseTexture(this.result);
        }
        this.clear();
        this.dirty = false;
    }

    public void addTint(String tex, int category, float r, float g, float b) {
        this.addTint(SmartTexture.getTextureWithFlags(tex), category, r, g, b);
    }

    public void addTint(Texture tex, int category, float r, float g, float b) {
        if (tint == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> l = new ArrayList<TextureCombinerShaderParam>();
        l.add(new TextureCombinerShaderParam("R", r));
        l.add(new TextureCombinerShaderParam("G", g));
        l.add(new TextureCombinerShaderParam("B", b));
        this.add(tex, tint, l);
    }

    public void addHue(String tex, int category, float h) {
        this.addHue(SmartTexture.getTextureWithFlags(tex), category, h);
    }

    public void addHue(Texture tex, int category, float h) {
        if (hue == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> l = new ArrayList<TextureCombinerShaderParam>();
        l.add(new TextureCombinerShaderParam("HueChange", h));
        this.add(tex, hue, l);
    }

    public Texture addHole(BloodBodyPartType part) {
        String mask = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[part.index()] + ".png";
        if (addHole == null) {
            this.create();
        }
        this.addToCat(3);
        this.calculate();
        Texture resolvedTex = this.result;
        this.clear();
        this.result = null;
        this.commands.add(TextureCombinerCommand.get().initSeparate(resolvedTex, addHole, addHoleParams, SmartTexture.getTextureWithFlags(mask), 770, 0, 1, 771));
        this.dirty = true;
        return resolvedTex;
    }

    public void removeHole(String bodyTex, BloodBodyPartType part) {
        String mask = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[part.index()] + ".png";
        this.removeHole(SmartTexture.getTextureWithFlags(bodyTex), SmartTexture.getTextureWithFlags(mask), part);
    }

    public void removeHole(Texture bodyTex, BloodBodyPartType part) {
        String mask = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[part.index()] + ".png";
        this.removeHole(bodyTex, SmartTexture.getTextureWithFlags(mask), part);
    }

    public void removeHole(Texture bodyTex, Texture maskTex, BloodBodyPartType part) {
        if (removeHole == null) {
            this.create();
        }
        this.addToCat(3);
        this.commands.add(TextureCombinerCommand.get().init(bodyTex, removeHole, removeHoleParams, maskTex, 770, 771));
        this.dirty = true;
    }

    public void mask(String tex, String maskTex, int category) {
        this.mask(SmartTexture.getTextureWithFlags(tex), SmartTexture.getTextureWithFlags(maskTex), category);
    }

    public void mask(Texture tex, Texture maskTex, int category) {
        if (bodyMask == null) {
            this.create();
        }
        this.addToCat(category);
        this.commands.add(TextureCombinerCommand.get().init(tex, bodyMask, bodyMaskParams, maskTex, 770, 771));
        this.dirty = true;
    }

    public void maskHue(String tex, String maskTex, int category, float h) {
        this.maskHue(SmartTexture.getTextureWithFlags(tex), SmartTexture.getTextureWithFlags(maskTex), category, h);
    }

    public void maskHue(Texture tex, Texture maskTex, int category, float h) {
        if (bodyMask == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> bodyMaskParams = new ArrayList<TextureCombinerShaderParam>();
        bodyMaskParams.add(new TextureCombinerShaderParam("HueChange", h));
        this.commands.add(TextureCombinerCommand.get().init(tex, bodyMaskHue, bodyMaskParams, maskTex, 770, 771));
        this.dirty = true;
    }

    public void maskTint(String tex, String maskTex, int category, float r, float g, float b) {
        this.maskTint(SmartTexture.getTextureWithFlags(tex), SmartTexture.getTextureWithFlags(maskTex), category, r, g, b);
    }

    public void maskTint(Texture tex, Texture maskTex, int category, float r, float g, float b) {
        if (bodyMask == null) {
            this.create();
        }
        this.addToCat(category);
        ArrayList<TextureCombinerShaderParam> bodyMaskParams = new ArrayList<TextureCombinerShaderParam>();
        bodyMaskParams.add(new TextureCombinerShaderParam("R", r));
        bodyMaskParams.add(new TextureCombinerShaderParam("G", g));
        bodyMaskParams.add(new TextureCombinerShaderParam("B", b));
        this.commands.add(TextureCombinerCommand.get().init(tex, bodyMaskTint, bodyMaskParams, maskTex, 770, 771));
        this.dirty = true;
    }

    public void addMaskedTexture(CharacterMask mask, String masksFolder, String base, int category, ImmutableColor tint, float hue) {
        SmartTexture.addMaskedTexture(this, mask, masksFolder, SmartTexture.getTextureWithFlags(base), category, tint, hue);
    }

    public void addMaskedTexture(CharacterMask mask, String masksFolder, Texture base, int category, ImmutableColor tint, float hue) {
        SmartTexture.addMaskedTexture(this, mask, masksFolder, base, category, tint, hue);
    }

    private static void addMaskFlags(SmartTexture tex, CharacterMask mask, String masksFolder, Texture base, int category) {
        Consumer<CharacterMask.Part> consumer = Lambda.consumer(tex, masksFolder, base, category, (part, lTex, lMasksFolder, lBase, lCategory) -> lTex.mask((Texture)lBase, SmartTexture.getTextureWithFlags(lMasksFolder + "/" + String.valueOf(part) + ".png"), (int)lCategory));
        mask.forEachVisible(consumer);
    }

    private static void addMaskFlagsHue(SmartTexture tex, CharacterMask mask, String masksFolder, Texture base, int category, float hue) {
        Consumer<CharacterMask.Part> consumer = Lambda.consumer(tex, masksFolder, base, category, Float.valueOf(hue), (part, lTex, lMasksFolder, lBase, lCategory, lHue) -> lTex.maskHue((Texture)lBase, SmartTexture.getTextureWithFlags(lMasksFolder + "/" + String.valueOf(part) + ".png"), (int)lCategory, lHue.floatValue()));
        mask.forEachVisible(consumer);
    }

    private static void addMaskFlagsTint(SmartTexture tex, CharacterMask mask, String masksFolder, Texture base, int category, ImmutableColor tint) {
        Consumer<CharacterMask.Part> consumer = Lambda.consumer(tex, masksFolder, base, category, tint, (part, lTex, lMasksfolder, lBase, lCategory, lTint) -> lTex.maskTint((Texture)lBase, SmartTexture.getTextureWithFlags(lMasksfolder + "/" + String.valueOf(part) + ".png"), (int)lCategory, lTint.r, lTint.g, lTint.b));
        mask.forEachVisible(consumer);
    }

    private static void addMaskedTexture(SmartTexture tex, CharacterMask mask, String masksFolder, Texture base, int category, ImmutableColor tint, float hue) {
        if (mask.isNothingVisible()) {
            return;
        }
        if (mask.isAllVisible()) {
            if (!ImmutableColor.white.equals(tint)) {
                tex.addTint(base, category, tint.r, tint.g, tint.b);
            } else if (hue < -1.0E-4f || hue > 1.0E-4f) {
                tex.addHue(base, category, hue);
            } else {
                tex.add(base);
            }
            return;
        }
        if (!ImmutableColor.white.equals(tint)) {
            SmartTexture.addMaskFlagsTint(tex, mask, masksFolder, base, category, tint);
        } else if (hue < -1.0E-4f || hue > 1.0E-4f) {
            SmartTexture.addMaskFlagsHue(tex, mask, masksFolder, base, category, hue);
        } else {
            SmartTexture.addMaskFlags(tex, mask, masksFolder, base, category);
        }
    }

    public void addTexture(String base, int category, ImmutableColor tint, float hue) {
        SmartTexture.addTexture(this, base, category, tint, hue);
    }

    private static void addTexture(SmartTexture tex, String base, int category, ImmutableColor tint, float hue) {
        if (!ImmutableColor.white.equals(tint)) {
            tex.addTint(base, category, tint.r, tint.g, tint.b);
        } else if (hue < -1.0E-4f || hue > 1.0E-4f) {
            tex.addHue(base, category, hue);
        } else {
            tex.add(base);
        }
    }

    private void create() {
        tint = new SmartShader("hueChange");
        hue = new SmartShader("hueChange");
        masked = new SmartShader("overlayMask");
        dirtMask = new SmartShader("dirtMask");
        bodyMask = new SmartShader("bodyMask");
        bodyMaskHue = new SmartShader("bodyMaskHue");
        bodyMaskTint = new SmartShader("bodyMaskTint");
        addHole = new SmartShader("addHole");
        removeHole = new SmartShader("removeHole");
        blit = new SmartShader("blit");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public WrappedBuffer getData() {
        SmartTexture smartTexture = this;
        synchronized (smartTexture) {
            if (this.dirty) {
                this.calculate();
            }
            return this.result.dataid.getData();
        }
    }

    @Override
    public synchronized void bind() {
        if (this.dirty) {
            this.calculate();
        }
        this.result.bind(3553);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int getID() {
        SmartTexture smartTexture = this;
        synchronized (smartTexture) {
            if (this.dirty) {
                this.calculate();
            }
        }
        return this.result.dataid.id;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void calculate() {
        SmartTexture smartTexture = this;
        synchronized (smartTexture) {
            if (Core.debug) {
                GL11.glGetError();
            }
            try {
                this.result = TextureCombiner.instance.combine(this.commands);
            }
            catch (Exception e) {
                DebugLog.General.error(e.getClass().getSimpleName() + " encountered while combining texture.");
                DebugLog.General.error("Intended width : " + TextureCombiner.getResultingWidth(this.commands));
                DebugLog.General.error("Intended height: " + TextureCombiner.getResultingHeight(this.commands));
                DebugLog.General.error("");
                DebugLog.General.error("Commands list: " + PZArrayUtil.arrayToString(this.commands));
                DebugLog.General.error("");
                DebugLog.General.error("Stack trace: ");
                ExceptionLogger.logException(e);
                DebugLog.General.error("This SmartTexture will no longer be valid.");
                this.width = -1;
                this.height = -1;
                this.dirty = false;
                return;
            }
            this.width = this.result.width;
            this.height = this.result.height;
            this.dirty = false;
        }
    }

    public void clear() {
        TextureCombinerCommand.pool.release((List<TextureCombinerCommand>)this.commands);
        this.commands.clear();
        this.categoryMap.clear();
        this.dirty = false;
    }

    public void add(String tex) {
        this.add(SmartTexture.getTextureWithFlags(tex));
    }

    public void add(Texture tex) {
        if (blit == null) {
            this.create();
        }
        this.commands.add(TextureCombinerCommand.get().init(tex, blit));
        this.dirty = true;
    }

    public void add(String tex, SmartShader shader, ArrayList<TextureCombinerShaderParam> params) {
        this.add(SmartTexture.getTextureWithFlags(tex), shader, params);
    }

    public void add(Texture tex, SmartShader shader, ArrayList<TextureCombinerShaderParam> params) {
        this.commands.add(TextureCombinerCommand.get().init(tex, shader, params));
        this.dirty = true;
    }

    public void add(String tex, SmartShader shader, String maskTex, int srcBlend, int destBlend) {
        this.add(SmartTexture.getTextureWithFlags(tex), shader, SmartTexture.getTextureWithFlags(maskTex), srcBlend, destBlend);
    }

    public void add(Texture tex, SmartShader shader, Texture maskTex, int srcBlend, int destBlend) {
        this.commands.add(TextureCombinerCommand.get().init(tex, shader, maskTex, srcBlend, destBlend));
        this.dirty = true;
    }

    public void add(String tex, SmartShader shader, int srcBlend, int destBlend) {
        this.add(SmartTexture.getTextureWithFlags(tex), shader, srcBlend, destBlend);
    }

    public void add(Texture tex, SmartShader shader, int srcBlend, int destBlend) {
        this.addSeparate(tex, shader, srcBlend, destBlend, 1, 771);
    }

    public void addSeparate(String tex, SmartShader shader, int srcBlend, int destBlend, int srcBlendA, int destBlendA) {
        this.addSeparate(SmartTexture.getTextureWithFlags(tex), shader, srcBlend, destBlend, srcBlendA, destBlendA);
    }

    public void addSeparate(Texture tex, SmartShader shader, int srcBlend, int destBlend, int srcBlendA, int destBlendA) {
        this.commands.add(TextureCombinerCommand.get().initSeparate(tex, shader, srcBlend, destBlend, srcBlendA, destBlendA));
        this.dirty = true;
    }

    public void add(String tex, SmartShader shader, String maskTex, ArrayList<TextureCombinerShaderParam> params, int srcBlend, int destBlend) {
        this.add(SmartTexture.getTextureWithFlags(tex), shader, SmartTexture.getTextureWithFlags(maskTex), params, srcBlend, destBlend);
    }

    public void add(Texture tex, SmartShader shader, Texture maskTex, ArrayList<TextureCombinerShaderParam> params, int srcBlend, int destBlend) {
        this.addSeparate(tex, shader, maskTex, params, srcBlend, destBlend, 1, 771);
    }

    public void addSeparate(String tex, SmartShader shader, String maskTex, ArrayList<TextureCombinerShaderParam> params, int srcBlend, int destBlend, int srcBlendA, int destBlendA) {
        this.addSeparate(SmartTexture.getTextureWithFlags(tex), shader, SmartTexture.getTextureWithFlags(maskTex), params, srcBlend, destBlend, srcBlendA, destBlendA);
    }

    public void addSeparate(Texture tex, SmartShader shader, Texture maskTex, ArrayList<TextureCombinerShaderParam> params, int srcBlend, int destBlend, int srcBlendA, int destBlendA) {
        this.commands.add(TextureCombinerCommand.get().initSeparate(tex, shader, params, maskTex, srcBlend, destBlend, srcBlendA, destBlendA));
        this.dirty = true;
    }

    private static Texture getTextureWithFlags(String fileName) {
        return Texture.getSharedTexture(fileName, ModelManager.instance.getTextureFlags());
    }

    @Override
    public void saveOnRenderThread(String filename) {
        if (this.dirty) {
            this.calculate();
        }
        this.result.saveOnRenderThread(filename);
    }

    protected void setDirty() {
        this.dirty = true;
    }

    @Override
    public boolean isEmpty() {
        return this.result == null ? true : this.result.isEmpty();
    }

    @Override
    public boolean isFailure() {
        return this.result == null ? false : this.result.isFailure();
    }

    @Override
    public boolean isReady() {
        return this.result == null ? false : this.result.isReady();
    }

    static {
        bodyMaskParams = new ArrayList();
        addHoleParams = new ArrayList();
        removeHoleParams = new ArrayList();
    }
}

