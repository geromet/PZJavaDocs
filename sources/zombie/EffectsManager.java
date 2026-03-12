/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelInstanceRenderDataList;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCamera;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelKey;

public final class EffectsManager {
    public static final int MuzzleFlashInverseProbability = 1;
    private static final int NumberOfMuzzleFlashTextures = 6;
    private final ArrayList<ModelInstanceEffect> modelInstanceEffects = new ArrayList();
    private final ArrayList<Texture> muzzleFlashTextures = new ArrayList();

    public static EffectsManager getInstance() {
        return Holder.instance;
    }

    private EffectsManager() {
        for (int i = 0; i < 6; ++i) {
            Texture texture = Texture.getSharedTexture("media/textures/weapons/firearm/fx/muzzle_flash_0" + i + ".png");
            if (texture == null) continue;
            this.muzzleFlashTextures.add(texture);
        }
    }

    public void startMuzzleFlash(IsoGameCharacter isoGameCharacter, int muzzleFlashInverseProbability) {
        if (!Rand.NextBool(muzzleFlashInverseProbability)) {
            return;
        }
        float invAmb = GameTime.getInstance().getNight() * 0.8f;
        if (isoGameCharacter.isInARoom()) {
            invAmb = PZMath.max(invAmb, 1.0f - isoGameCharacter.getCurrentSquare().getLightLevel(IsoCamera.frameState.playerIndex));
        }
        invAmb = Math.max(invAmb, 0.2f);
        IsoLightSource lightSource = new IsoLightSource(PZMath.fastfloor(isoGameCharacter.getX()), PZMath.fastfloor(isoGameCharacter.getY()), PZMath.fastfloor(isoGameCharacter.getZ()), 0.8f * invAmb, 0.8f * invAmb, 0.6f * invAmb, 18, 6);
        IsoWorld.instance.currentCell.getLamppostPositions().add(lightSource);
        ModelInstanceEffect modelInstanceEffect = this.getModelInstanceEffect(isoGameCharacter);
        if (modelInstanceEffect != null) {
            modelInstanceEffect.currentTime = 0.0f;
        } else {
            modelInstanceEffect = new ModelInstanceEffect(isoGameCharacter);
            this.modelInstanceEffects.add(modelInstanceEffect);
        }
        modelInstanceEffect.init(isoGameCharacter);
    }

    public void initMuzzleFlashModel(ModelInstanceRenderData instData, IsoGameCharacter isoGameCharacter, ModelInstanceRenderDataList modelData, ModelManager.ModelSlot modelSlot) {
        if (DebugOptions.instance.model.render.muzzleFlash.getValue() && !this.activeMuzzleFlash(isoGameCharacter)) {
            EffectsManager.getInstance().startMuzzleFlash(isoGameCharacter, 1);
        }
        if (!this.activeMuzzleFlash(isoGameCharacter)) {
            return;
        }
        ModelAttachment modelAttachment = instData.modelInstance.getAttachmentById("muzzle");
        if (modelAttachment == null) {
            return;
        }
        ModelInstanceEffect modelInstanceEffect = this.getModelInstanceEffect(isoGameCharacter);
        if (modelInstanceEffect == null) {
            return;
        }
        modelInstanceEffect.currentTime += GameTime.getInstance().getTimeDelta();
        if (modelInstanceEffect.currentTime > modelInstanceEffect.effectTime) {
            modelInstanceEffect.currentTime = modelInstanceEffect.effectTime;
        }
        modelInstanceEffect.textureId = Rand.Next(6);
        String muzzleFlashModelKey = modelInstanceEffect.muzzleFlashModelKey;
        if (muzzleFlashModelKey == null) {
            this.releaseModelInstanceEffects();
            return;
        }
        Model muzzleFlashModel = ModelManager.instance.getLoadedModel(muzzleFlashModelKey);
        if (muzzleFlashModel != null && muzzleFlashModel.isReady()) {
            ModelInstanceRenderData muzzleFlashData = ModelInstanceRenderData.alloc();
            ModelInstance muzzleFlashModelInstance = ModelManager.instance.newInstance(muzzleFlashModel, isoGameCharacter, isoGameCharacter.getAnimationPlayer());
            if (muzzleFlashModelInstance == null) {
                return;
            }
            muzzleFlashModelInstance.parent = instData.modelInstance;
            muzzleFlashModelInstance.attachmentNameSelf = null;
            muzzleFlashModelInstance.attachmentNameParent = "muzzle";
            muzzleFlashModelInstance.tex = this.muzzleFlashTextures.get(modelInstanceEffect.textureId);
            muzzleFlashData.initModel(muzzleFlashModelInstance, instData);
            muzzleFlashModelInstance.applyModelScriptScale(muzzleFlashModelKey);
            muzzleFlashData.ignoreLighting = true;
            modelData.add(muzzleFlashData);
            muzzleFlashModelInstance.setOwner(modelSlot);
            modelSlot.muzzleFlashModels.add(muzzleFlashModelInstance);
        }
        this.releaseModelInstanceEffects();
    }

    public boolean postRender(IsoGameCharacter isoGameCharacter, ModelInstance modelInstance, ModelInstanceRenderData instData, ModelManager.ModelSlot modelSlot) {
        if (modelSlot.muzzleFlashModels.remove(modelInstance)) {
            boolean bInstanceReleased = ModelInstanceRenderData.release(instData);
            if (!bInstanceReleased && !modelInstance.resetAfterRender) {
                ModelManager.instance.resetModelInstance(modelInstance, modelSlot);
            }
            return true;
        }
        return false;
    }

    private boolean activeMuzzleFlash(IsoGameCharacter isoGameCharacter) {
        return this.getModelInstanceEffect(isoGameCharacter) != null;
    }

    private ModelInstanceEffect getModelInstanceEffect(IsoGameCharacter isoGameCharacter) {
        for (ModelInstanceEffect modelInstanceEffect : this.modelInstanceEffects) {
            if (modelInstanceEffect.isoGameCharacter != isoGameCharacter) continue;
            return modelInstanceEffect;
        }
        return null;
    }

    private void releaseModelInstanceEffects() {
        this.modelInstanceEffects.removeIf(modelInstanceEffect -> modelInstanceEffect.currentTime >= modelInstanceEffect.effectTime);
    }

    private static class Holder {
        private static final EffectsManager instance = new EffectsManager();

        private Holder() {
        }
    }

    private static final class ModelInstanceEffect {
        private final float effectTime = 0.04f;
        private float currentTime;
        private final IsoGameCharacter isoGameCharacter;
        private int textureId;
        private String muzzleFlashModelKey;

        private ModelInstanceEffect(IsoGameCharacter isoGameCharacter) {
            this.isoGameCharacter = isoGameCharacter;
        }

        private ModelInstanceEffect init(IsoGameCharacter isoGameCharacter) {
            HandWeapon handWeapon;
            ModelKey modelKey;
            this.muzzleFlashModelKey = null;
            InventoryItem inventoryItem = isoGameCharacter.getPrimaryHandItem();
            if (inventoryItem instanceof HandWeapon && (modelKey = (handWeapon = (HandWeapon)inventoryItem).getMuzzleFlashModelKey()) != null) {
                this.muzzleFlashModelKey = modelKey.toString();
            }
            return this;
        }
    }
}

