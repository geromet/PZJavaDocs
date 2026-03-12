/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.CharacterSmartTexture;
import zombie.characterTextures.ItemSmartTexture;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.population.ClothingDecal;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugType;
import zombie.popman.ObjectPool;
import zombie.util.Lambda;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ModelInstanceTextureCreator
extends TextureDraw.GenericDrawer {
    private boolean zombie;
    public int renderRefCount;
    private final CharacterMask mask = new CharacterMask();
    private final boolean[] holeMask = new boolean[BloodBodyPartType.MAX.index()];
    private final ItemVisuals itemVisuals = new ItemVisuals();
    private final CharacterData chrData = new CharacterData();
    private final ArrayList<ItemData> itemData = new ArrayList();
    private final CharacterSmartTexture characterSmartTexture = new CharacterSmartTexture();
    private final ItemSmartTexture itemSmartTexture = new ItemSmartTexture(null);
    private final ArrayList<Texture> tempTextures = new ArrayList();
    private boolean rendered;
    private final ArrayList<Texture> texturesNotReady = new ArrayList();
    public int testNotReady = -1;
    private final ArrayList<ItemData> localItemData = new ArrayList();
    private static final ObjectPool<ModelInstanceTextureCreator> pool = new ObjectPool<ModelInstanceTextureCreator>(ModelInstanceTextureCreator::new);

    public void init(IsoGameCharacter chr) {
        ModelManager.ModelSlot modelSlot = chr.legsSprite.modelSlot;
        if (chr instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)chr;
            this.init(isoAnimal.getAnimalVisual(), modelSlot.model);
            return;
        }
        HumanVisual humanVisual = ((IHumanVisual)((Object)chr)).getHumanVisual();
        chr.getItemVisuals(this.itemVisuals);
        this.init(humanVisual, this.itemVisuals, modelSlot.model);
        this.itemVisuals.clear();
    }

    public void init(BaseVisual baseVisual, ItemVisuals itemVisuals, ModelInstance chrModelInstance) {
        if (baseVisual instanceof AnimalVisual) {
            AnimalVisual animalVisual = (AnimalVisual)baseVisual;
            this.init(animalVisual, chrModelInstance);
            return;
        }
        if (baseVisual instanceof HumanVisual) {
            HumanVisual humanVisual = (HumanVisual)baseVisual;
            this.init(humanVisual, itemVisuals, chrModelInstance);
            return;
        }
        throw new IllegalArgumentException("unhandled BaseVisual " + String.valueOf(baseVisual));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void init(AnimalVisual animalVisual, ModelInstance chrModelInstance) {
        this.chrData.modelInstance = chrModelInstance;
        this.rendered = false;
        this.zombie = false;
        Arrays.fill(this.holeMask, false);
        ArrayList<ItemData> arrayList = this.itemData;
        synchronized (arrayList) {
            ItemData.pool.release((List<ItemData>)this.itemData);
            this.itemData.clear();
        }
        this.texturesNotReady.clear();
        this.chrData.mask.setAllVisible(true);
        this.chrData.maskFolder = "media/textures/Body/Masks";
        this.chrData.baseTexture = "media/textures/Body/" + animalVisual.getSkinTexture() + ".png";
        Arrays.fill(this.chrData.blood, 0.0f);
        Arrays.fill(this.chrData.dirt, 0.0f);
        Texture tex = Texture.getSharedTexture(this.chrData.baseTexture);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
            String maskFolder = this.chrData.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, lMaskFolder, lTexturesNotReady) -> {
                Texture tex1 = Texture.getSharedTexture(lMaskFolder + "/" + String.valueOf(part) + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    lTexturesNotReady.add(tex1);
                }
            });
            this.chrData.mask.forEachVisible(consumer);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void init(HumanVisual humanVisual, ItemVisuals itemVisuals, ModelInstance chrModelInstance) {
        int i;
        int i2;
        this.chrData.modelInstance = chrModelInstance;
        this.rendered = false;
        this.zombie = humanVisual.isZombie();
        CharacterMask overlayerMask = this.mask;
        overlayerMask.setAllVisible(true);
        String overlayMasksFolder = "media/textures/Body/Masks";
        Arrays.fill(this.holeMask, false);
        ArrayList<ItemData> arrayList = this.itemData;
        synchronized (arrayList) {
            ItemData.pool.release((List<ItemData>)this.itemData);
            this.itemData.clear();
        }
        this.texturesNotReady.clear();
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        for (i2 = itemVisuals.size() - 1; i2 >= 0; --i2) {
            int j;
            String modelName;
            ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i2);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                DebugType.Clothing.warn("ClothingItem not found for ItemVisual:%s", itemVisual);
                continue;
            }
            if (!clothingItem.isReady()) {
                DebugType.Clothing.warn("ClothingItem not ready for ItemVisual:%s", itemVisual);
                continue;
            }
            if (PopTemplateManager.instance.isItemModelHidden(bodyLocationGroup, itemVisuals, itemVisual)) continue;
            ModelInstance modelInstance = this.findModelInstance(chrModelInstance.sub, itemVisual);
            if (modelInstance == null && !StringUtils.isNullOrWhitespace(modelName = clothingItem.getModel(humanVisual.isFemale()))) {
                DebugType.Clothing.warn("ModelInstance not found for ItemVisual:%s", itemVisual);
                continue;
            }
            this.addClothingItem(modelInstance, itemVisual, clothingItem, overlayerMask, overlayMasksFolder);
            for (j = 0; j < BloodBodyPartType.MAX.index(); ++j) {
                BloodBodyPartType bpt = BloodBodyPartType.FromIndex(j);
                if (!(itemVisual.getHole(bpt) > 0.0f) || !overlayerMask.isBloodBodyPartVisible(bpt)) continue;
                this.holeMask[j] = true;
            }
            for (j = 0; j < clothingItem.masks.size(); ++j) {
                CharacterMask.Part cmp = CharacterMask.Part.fromInt(clothingItem.masks.get(j));
                for (BloodBodyPartType bpt : cmp.getBloodBodyPartTypes()) {
                    if (!(itemVisual.getHole(bpt) <= 0.0f)) continue;
                    this.holeMask[bpt.index()] = false;
                }
            }
            itemVisual.getClothingItemCombinedMask(overlayerMask);
            if (StringUtils.equalsIgnoreCase(clothingItem.underlayMasksFolder, "media/textures/Body/Masks")) continue;
            overlayMasksFolder = clothingItem.underlayMasksFolder;
        }
        this.chrData.mask.copyFrom(overlayerMask);
        this.chrData.maskFolder = overlayMasksFolder;
        this.chrData.baseTexture = "media/textures/Body/" + humanVisual.getSkinTexture() + ".png";
        Arrays.fill(this.chrData.blood, 0.0f);
        for (i2 = 0; i2 < BloodBodyPartType.MAX.index(); ++i2) {
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i2);
            this.chrData.blood[i2] = humanVisual.getBlood(bpt);
            this.chrData.dirt[i2] = humanVisual.getDirt(bpt);
        }
        Texture tex = ModelInstanceTextureCreator.getTextureWithFlags(this.chrData.baseTexture);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        if (!this.chrData.mask.isAllVisible() && !this.chrData.mask.isNothingVisible()) {
            String maskFolder = this.chrData.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, lMaskFolder, lTexturesNotReady) -> {
                Texture tex1 = ModelInstanceTextureCreator.getTextureWithFlags(lMaskFolder + "/" + String.valueOf(part) + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    lTexturesNotReady.add(tex1);
                }
            });
            this.chrData.mask.forEachVisible(consumer);
        }
        if ((tex = ModelInstanceTextureCreator.getTextureWithFlags("media/textures/BloodTextures/BloodOverlay.png")) != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        if ((tex = ModelInstanceTextureCreator.getTextureWithFlags("media/textures/BloodTextures/GrimeOverlay.png")) != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        if ((tex = ModelInstanceTextureCreator.getTextureWithFlags("media/textures/patches/patchesmask.png")) != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        for (i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            String leatherPatchMask;
            String denimPatchMask;
            String basicPatchMask;
            String hole;
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            String mask = "media/textures/BloodTextures/" + CharacterSmartTexture.MaskFiles[bpt.index()] + ".png";
            tex = ModelInstanceTextureCreator.getTextureWithFlags(mask);
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
            if ((tex = ModelInstanceTextureCreator.getTextureWithFlags(hole = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[bpt.index()] + ".png")) != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
            if ((tex = ModelInstanceTextureCreator.getTextureWithFlags(basicPatchMask = "media/textures/patches/" + CharacterSmartTexture.BasicPatchesMaskFiles[bpt.index()] + ".png")) != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
            if ((tex = ModelInstanceTextureCreator.getTextureWithFlags(denimPatchMask = "media/textures/patches/" + CharacterSmartTexture.DenimPatchesMaskFiles[bpt.index()] + ".png")) != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
            if ((tex = ModelInstanceTextureCreator.getTextureWithFlags(leatherPatchMask = "media/textures/patches/" + CharacterSmartTexture.LeatherPatchesMaskFiles[bpt.index()] + ".png")) == null || tex.isReady()) continue;
            this.texturesNotReady.add(tex);
        }
        overlayerMask.setAllVisible(true);
        overlayMasksFolder = "media/textures/Body/Masks";
        for (i = humanVisual.getBodyVisuals().size() - 1; i >= 0; --i) {
            String modelName;
            ItemVisual itemVisual = (ItemVisual)humanVisual.getBodyVisuals().get(i);
            ClothingItem clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                DebugType.Clothing.warn("ClothingItem not found for ItemVisual:%s", itemVisual);
                continue;
            }
            if (!clothingItem.isReady()) {
                DebugType.Clothing.warn("ClothingItem not ready for ItemVisual:%s", itemVisual);
                continue;
            }
            ModelInstance modelInstance = this.findModelInstance(chrModelInstance.sub, itemVisual);
            if (modelInstance == null && !StringUtils.isNullOrWhitespace(modelName = clothingItem.getModel(humanVisual.isFemale()))) {
                DebugType.Clothing.warn("ModelInstance not found for ItemVisual:%s", itemVisual);
                continue;
            }
            this.addClothingItem(modelInstance, itemVisual, clothingItem, overlayerMask, overlayMasksFolder);
        }
    }

    private ModelInstance findModelInstance(ArrayList<ModelInstance> sub, ItemVisual itemVisual) {
        for (int i = 0; i < sub.size(); ++i) {
            ModelInstance modelInstance = sub.get(i);
            ItemVisual itemVisual1 = modelInstance.getItemVisual();
            if (itemVisual1 == null || itemVisual1.getClothingItem() != itemVisual.getClothingItem()) continue;
            return modelInstance;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void addClothingItem(ModelInstance modelInstance, ItemVisual itemVisual, ClothingItem clothingItem, CharacterMask overlayerMask, String overlayMasksFolder) {
        ClothingDecal decal;
        Object maskFolder;
        String textureName = modelInstance == null ? itemVisual.getBaseTexture(clothingItem) : itemVisual.getTextureChoice(clothingItem);
        ImmutableColor tintColor = itemVisual.getTint(clothingItem);
        float hue = itemVisual.getHue(clothingItem);
        ItemData data = ItemData.pool.alloc();
        data.modelInstance = modelInstance;
        data.category = 3;
        data.mask.copyFrom(overlayerMask);
        data.maskFolder = clothingItem.masksFolder;
        if (StringUtils.equalsIgnoreCase(data.maskFolder, "media/textures/Body/Masks")) {
            data.maskFolder = overlayMasksFolder;
        }
        if (StringUtils.equalsIgnoreCase(data.maskFolder, "none")) {
            data.mask.setAllVisible(true);
        }
        if (data.maskFolder.contains("Clothes/Hat/Masks")) {
            data.mask.setAllVisible(true);
        }
        data.baseTexture = "media/textures/" + textureName + ".png";
        data.tint = tintColor;
        data.hue = hue;
        data.decalTexture = null;
        Arrays.fill(data.basicPatches, 0.0f);
        Arrays.fill(data.denimPatches, 0.0f);
        Arrays.fill(data.leatherPatches, 0.0f);
        Arrays.fill(data.blood, 0.0f);
        Arrays.fill(data.dirt, 0.0f);
        Arrays.fill(data.hole, 0.0f);
        int flags = ModelManager.instance.getTextureFlags();
        Texture tex = Texture.getSharedTexture(data.baseTexture, flags);
        if (tex != null && !tex.isReady()) {
            this.texturesNotReady.add(tex);
        }
        if (!data.mask.isAllVisible() && !data.mask.isNothingVisible()) {
            maskFolder = data.maskFolder;
            Consumer<CharacterMask.Part> consumer = Lambda.consumer(maskFolder, this.texturesNotReady, (part, lMaskFolder, lTexturesNotReady) -> {
                Texture tex1 = ModelInstanceTextureCreator.getTextureWithFlags(lMaskFolder + "/" + String.valueOf(part) + ".png");
                if (tex1 != null && !tex1.isReady()) {
                    lTexturesNotReady.add(tex1);
                }
            });
            data.mask.forEachVisible(consumer);
        }
        if (Core.getInstance().isOptionSimpleClothingTextures(this.zombie)) {
            maskFolder = this.itemData;
            synchronized (maskFolder) {
                this.itemData.add(data);
            }
            return;
        }
        String decalName = itemVisual.getDecal(clothingItem);
        if (!StringUtils.isNullOrWhitespace(decalName) && (decal = ClothingDecals.instance.getDecal(decalName)) != null && decal.isValid()) {
            data.decalTexture = decal.texture;
            data.decalX = decal.x;
            data.decalY = decal.y;
            data.decalWidth = decal.width;
            data.decalHeight = decal.height;
            tex = ModelInstanceTextureCreator.getTextureWithFlags("media/textures/" + data.decalTexture + ".png");
            if (tex != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
        }
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            String mask;
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            data.blood[i] = itemVisual.getBlood(bpt);
            data.dirt[i] = itemVisual.getDirt(bpt);
            data.basicPatches[i] = itemVisual.getBasicPatch(bpt);
            data.denimPatches[i] = itemVisual.getDenimPatch(bpt);
            data.leatherPatches[i] = itemVisual.getLeatherPatch(bpt);
            data.hole[i] = itemVisual.getHole(bpt);
            if (data.hole[i] > 0.0f && (tex = ModelInstanceTextureCreator.getTextureWithFlags(mask = "media/textures/HoleTextures/" + CharacterSmartTexture.MaskFiles[bpt.index()] + ".png")) != null && !tex.isReady()) {
                this.texturesNotReady.add(tex);
            }
            if (data.hole[i] != 0.0f || !this.holeMask[i]) continue;
            data.hole[i] = -1.0f;
            if (!data.mask.isBloodBodyPartVisible(bpt)) continue;
        }
        ArrayList<ItemData> arrayList = this.itemData;
        synchronized (arrayList) {
            this.itemData.add(data);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void render() {
        Serializable texture;
        if (this.rendered) {
            return;
        }
        if (this.chrData.modelInstance == null) {
            return;
        }
        for (int i = 0; i < this.texturesNotReady.size(); ++i) {
            texture = this.texturesNotReady.get(i);
            if (texture.isReady()) continue;
            return;
        }
        GL11.glPushAttrib(2048);
        try {
            int i;
            this.tempTextures.clear();
            CharacterSmartTexture characterTexture = this.createFullCharacterTexture();
            assert (characterTexture == this.characterSmartTexture);
            if (!(this.chrData.modelInstance.tex instanceof CharacterSmartTexture)) {
                this.chrData.modelInstance.tex = new CharacterSmartTexture();
            }
            ((CharacterSmartTexture)this.chrData.modelInstance.tex).clear();
            this.applyCharacterTexture(characterTexture.result, (CharacterSmartTexture)this.chrData.modelInstance.tex);
            characterTexture.clear();
            this.tempTextures.add(characterTexture.result);
            characterTexture.result = null;
            characterTexture = (CharacterSmartTexture)this.chrData.modelInstance.tex;
            this.localItemData.clear();
            texture = this.itemData;
            synchronized (texture) {
                PZArrayUtil.addAll(this.localItemData, this.itemData);
            }
            for (i = this.localItemData.size() - 1; i >= 0; --i) {
                Texture itemTexture;
                ItemData data = this.localItemData.get(i);
                if (this.isSimpleTexture(data)) {
                    int flags = ModelManager.instance.getTextureFlags();
                    itemTexture = Texture.getSharedTexture(data.baseTexture, flags);
                    if (!this.isItemSmartTextureRequired(data)) {
                        data.modelInstance.tex = itemTexture;
                        continue;
                    }
                } else {
                    ItemSmartTexture fullTexture = this.createFullItemTexture(data);
                    assert (fullTexture == this.itemSmartTexture);
                    itemTexture = fullTexture.result;
                    this.tempTextures.add(fullTexture.result);
                    fullTexture.result = null;
                }
                if (data.modelInstance == null) {
                    this.applyItemTexture(data, itemTexture, characterTexture);
                    continue;
                }
                if (!(data.modelInstance.tex instanceof ItemSmartTexture)) {
                    data.modelInstance.tex = new ItemSmartTexture(null);
                }
                ((ItemSmartTexture)data.modelInstance.tex).clear();
                this.applyItemTexture(data, itemTexture, (ItemSmartTexture)data.modelInstance.tex);
                ((ItemSmartTexture)data.modelInstance.tex).calculate();
                ((ItemSmartTexture)data.modelInstance.tex).clear();
            }
            characterTexture.calculate();
            characterTexture.clear();
            this.itemSmartTexture.clear();
            for (i = 0; i < this.tempTextures.size(); ++i) {
                for (int j = 0; j < this.localItemData.size(); ++j) {
                    ModelInstance modelInstance = this.localItemData.get((int)j).modelInstance;
                    if (modelInstance != null && this.tempTextures.get(i) == modelInstance.tex) assert (false);
                }
                TextureCombiner.instance.releaseTexture(this.tempTextures.get(i));
            }
            this.tempTextures.clear();
        }
        finally {
            GL11.glPopAttrib();
        }
        this.rendered = true;
    }

    private CharacterSmartTexture createFullCharacterTexture() {
        CharacterSmartTexture smartTex = this.characterSmartTexture;
        smartTex.clear();
        smartTex.addTexture(this.chrData.baseTexture, 0, ImmutableColor.white, 0.0f);
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            if (this.chrData.dirt[i] > 0.0f) {
                smartTex.addDirt(bpt, this.chrData.dirt[i], null);
            }
            if (!(this.chrData.blood[i] > 0.0f)) continue;
            smartTex.addBlood(bpt, this.chrData.blood[i], null);
        }
        smartTex.calculate();
        return smartTex;
    }

    private void applyCharacterTexture(Texture fullTex, CharacterSmartTexture destTex) {
        destTex.addMaskedTexture(this.chrData.mask, this.chrData.maskFolder, fullTex, 0, ImmutableColor.white, 0.0f);
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            if (!this.holeMask[i]) continue;
            destTex.removeHole(fullTex, bpt);
        }
    }

    private boolean isSimpleTexture(ItemData data) {
        if (data.hue != 0.0f) {
            return false;
        }
        ImmutableColor tint = data.tint;
        if (data.modelInstance != null) {
            tint = ImmutableColor.white;
        }
        if (!tint.equals(ImmutableColor.white)) {
            return false;
        }
        if (data.decalTexture != null) {
            return false;
        }
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (data.blood[i] > 0.0f) {
                return false;
            }
            if (data.dirt[i] > 0.0f) {
                return false;
            }
            if (data.hole[i] > 0.0f) {
                return false;
            }
            if (data.basicPatches[i] > 0.0f) {
                return false;
            }
            if (data.denimPatches[i] > 0.0f) {
                return false;
            }
            if (!(data.leatherPatches[i] > 0.0f)) continue;
            return false;
        }
        return true;
    }

    private ItemSmartTexture createFullItemTexture(ItemData data) {
        BloodBodyPartType bpt;
        int i;
        ItemSmartTexture smartTex = this.itemSmartTexture;
        smartTex.clear();
        ImmutableColor tint = data.tint;
        if (data.modelInstance != null) {
            data.modelInstance.tintB = 1.0f;
            data.modelInstance.tintG = 1.0f;
            data.modelInstance.tintR = 1.0f;
        }
        smartTex.addTexture(data.baseTexture, data.category, tint, data.hue);
        if (data.decalTexture != null) {
            smartTex.addRect("media/textures/" + data.decalTexture + ".png", data.decalX, data.decalY, data.decalWidth, data.decalHeight);
        }
        for (i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (data.blood[i] > 0.0f) {
                bpt = BloodBodyPartType.FromIndex(i);
                smartTex.addBlood("media/textures/BloodTextures/BloodOverlay.png", bpt, data.blood[i]);
            }
            if (data.dirt[i] > 0.0f) {
                bpt = BloodBodyPartType.FromIndex(i);
                smartTex.addDirt("media/textures/BloodTextures/GrimeOverlay.png", bpt, data.dirt[i]);
            }
            if (data.basicPatches[i] > 0.0f) {
                bpt = BloodBodyPartType.FromIndex(i);
                smartTex.setBasicPatches(bpt);
            }
            if (data.denimPatches[i] > 0.0f) {
                bpt = BloodBodyPartType.FromIndex(i);
                smartTex.setDenimPatches(bpt);
            }
            if (!(data.leatherPatches[i] > 0.0f)) continue;
            bpt = BloodBodyPartType.FromIndex(i);
            smartTex.setLeatherPatches(bpt);
        }
        for (i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (!(data.hole[i] > 0.0f)) continue;
            bpt = BloodBodyPartType.FromIndex(i);
            Texture result = smartTex.addHole(bpt);
            assert (result != smartTex.result);
            this.tempTextures.add(result);
        }
        smartTex.calculate();
        return smartTex;
    }

    private boolean isItemSmartTextureRequired(ItemData data) {
        if (data.modelInstance == null) {
            return true;
        }
        if (data.modelInstance.tex instanceof ItemSmartTexture) {
            return true;
        }
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (!(data.hole[i] < 0.0f)) continue;
            return true;
        }
        return !data.mask.isAllVisible();
    }

    private void applyItemTexture(ItemData data, Texture fullTex, SmartTexture destTex) {
        destTex.addMaskedTexture(data.mask, data.maskFolder, fullTex, data.category, ImmutableColor.white, 0.0f);
        for (int i = 0; i < BloodBodyPartType.MAX.index(); ++i) {
            if (!(data.hole[i] < 0.0f)) continue;
            BloodBodyPartType bpt = BloodBodyPartType.FromIndex(i);
            destTex.removeHole(fullTex, bpt);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void postRender() {
        if (!this.rendered) {
            boolean bl = this.chrData.modelInstance.character == null ? true : true;
        }
        ArrayList<ItemData> arrayList = this.itemData;
        synchronized (arrayList) {
            for (int i = 0; i < this.itemData.size(); ++i) {
                this.itemData.get((int)i).modelInstance = null;
            }
            this.chrData.modelInstance = null;
            this.texturesNotReady.clear();
            ItemData.pool.release((List<ItemData>)this.itemData);
            this.itemData.clear();
        }
        pool.release(this);
    }

    public boolean isRendered() {
        if (this.testNotReady > 0) {
            return false;
        }
        return this.rendered;
    }

    private static Texture getTextureWithFlags(String fileName) {
        return Texture.getSharedTexture(fileName, ModelManager.instance.getTextureFlags());
    }

    public static ModelInstanceTextureCreator alloc() {
        return pool.alloc();
    }

    private static final class CharacterData {
        ModelInstance modelInstance;
        final CharacterMask mask = new CharacterMask();
        String maskFolder;
        String baseTexture;
        final float[] blood = new float[BloodBodyPartType.MAX.index()];
        final float[] dirt = new float[BloodBodyPartType.MAX.index()];

        private CharacterData() {
        }
    }

    private static final class ItemData {
        ModelInstance modelInstance;
        final CharacterMask mask = new CharacterMask();
        String maskFolder;
        String baseTexture;
        int category;
        ImmutableColor tint;
        float hue;
        String decalTexture;
        int decalX;
        int decalY;
        int decalWidth;
        int decalHeight;
        final float[] blood = new float[BloodBodyPartType.MAX.index()];
        final float[] dirt = new float[BloodBodyPartType.MAX.index()];
        final float[] basicPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] denimPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] leatherPatches = new float[BloodBodyPartType.MAX.index()];
        final float[] hole = new float[BloodBodyPartType.MAX.index()];
        static final ObjectPool<ItemData> pool = new ObjectPool<ItemData>(ItemData::new);

        private ItemData() {
        }
    }
}

