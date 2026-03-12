/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import java.util.Locale;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.util.StringUtils;

public class PopTemplateManager {
    public static final PopTemplateManager instance = new PopTemplateManager();
    public final ArrayList<String> maleSkins = new ArrayList();
    public final ArrayList<String> femaleSkins = new ArrayList();
    public final ArrayList<String> maleSkinsZombie1 = new ArrayList();
    public final ArrayList<String> femaleSkinsZombie1 = new ArrayList();
    public final ArrayList<String> maleSkinsZombie2 = new ArrayList();
    public final ArrayList<String> femaleSkinsZombie2 = new ArrayList();
    public final ArrayList<String> maleSkinsZombie3 = new ArrayList();
    public final ArrayList<String> femaleSkinsZombie3 = new ArrayList();
    public ArrayList<String> cowSkins = new ArrayList();
    public ArrayList<String> ratSkins = new ArrayList();
    public final ArrayList<String> skeletonMaleSkinsZombie = new ArrayList();
    public final ArrayList<String> skeletonFemaleSkinsZombie = new ArrayList();
    public static final int SKELETON_BURNED_SKIN_INDEX = 0;
    public static final int SKELETON_NORMAL_SKIN_INDEX = 1;
    public static final int SKELETON_MUSCLE_SKIN_INDEX = 2;

    public void init() {
        int i;
        for (i = 1; i <= 5; ++i) {
            this.maleSkins.add("MaleBody0" + i);
        }
        for (i = 1; i <= 5; ++i) {
            this.femaleSkins.add("FemaleBody0" + i);
        }
        for (i = 1; i <= 4; ++i) {
            this.maleSkinsZombie1.add("M_ZedBody0" + i + "_level1");
            this.femaleSkinsZombie1.add("F_ZedBody0" + i + "_level1");
            this.maleSkinsZombie2.add("M_ZedBody0" + i + "_level2");
            this.femaleSkinsZombie2.add("F_ZedBody0" + i + "_level2");
            this.maleSkinsZombie3.add("M_ZedBody0" + i + "_level3");
            this.femaleSkinsZombie3.add("F_ZedBody0" + i + "_level3");
        }
        this.skeletonMaleSkinsZombie.add("SkeletonBurned");
        this.skeletonMaleSkinsZombie.add("Skeleton");
        this.skeletonMaleSkinsZombie.add("SkeletonMuscle");
        this.skeletonFemaleSkinsZombie.add("SkeletonBurned");
        this.skeletonFemaleSkinsZombie.add("Skeleton");
        this.skeletonFemaleSkinsZombie.add("SkeletonMuscle");
        this.cowSkins.add("Cow_Black");
        this.cowSkins.add("Cow_Purple");
        this.ratSkins.add("Rat");
    }

    public ModelInstance addClothingItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, ItemVisual itemVisual, ClothingItem clothingItem) {
        return this.addClothingItem(chr, modelSlot, itemVisual, clothingItem, false);
    }

    public ModelInstance addClothingItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, ItemVisual itemVisual, ClothingItem clothingItem, boolean alt) {
        String modelFileName = clothingItem.getModel(chr.isFemale());
        if (alt && clothingItem.getAltModel(chr.isFemale()) != null) {
            modelFileName = clothingItem.getAltModel(chr.isFemale());
        }
        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            DebugType.Clothing.debugln("No model specified by item: %s", clothingItem.mame);
            return null;
        }
        modelFileName = this.processModelFileName(modelFileName);
        String textureName = itemVisual.getTextureChoice(clothingItem);
        ImmutableColor tintColor = itemVisual.getTint(clothingItem);
        String attachBone = clothingItem.attachBone;
        String shaderName = clothingItem.shader;
        ModelInstance inst = attachBone != null && !attachBone.isEmpty() ? ModelManager.instance.newStaticInstance(modelSlot, modelFileName, textureName, attachBone, shaderName) : ModelManager.instance.newAdditionalModelInstance(modelFileName, textureName, chr, modelSlot.model.animPlayer, shaderName);
        if (inst == null) {
            return null;
        }
        this.postProcessNewItemInstance(inst, modelSlot, tintColor);
        inst.setItemVisual(itemVisual);
        return inst;
    }

    private void addHeadHairItem(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, String modelFileName, String textureName, ImmutableColor tintColor) {
        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            DebugType.Clothing.warn("No model specified.");
            return;
        }
        ModelInstance inst = ModelManager.instance.newAdditionalModelInstance(modelFileName = this.processModelFileName(modelFileName), textureName, chr, modelSlot.model.animPlayer, null);
        if (inst == null) {
            return;
        }
        this.postProcessNewItemInstance(inst, modelSlot, tintColor);
    }

    private void addHeadHair(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot, HumanVisual humanVisual, ItemVisual hatVisual, boolean beardOnly) {
        ImmutableColor col = humanVisual.getHairColor();
        if (beardOnly) {
            col = humanVisual.getBeardColor();
        }
        if (chr.isFemale()) {
            if (!beardOnly) {
                HairStyle hairStyle = HairStyles.instance.FindFemaleStyle(humanVisual.getHairModel());
                if (hairStyle != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                    hairStyle = HairStyles.instance.getAlternateForHat(hairStyle, hatVisual.getClothingItem().hatCategory);
                }
                if (hairStyle != null && hairStyle.isValid()) {
                    DebugType.Clothing.debugln("  Adding female hair: %s", hairStyle.name);
                    this.addHeadHairItem(chr, modelSlot, hairStyle.model, hairStyle.texture, col);
                }
            }
        } else if (!beardOnly) {
            HairStyle hairStyle = HairStyles.instance.FindMaleStyle(humanVisual.getHairModel());
            if (hairStyle != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                hairStyle = HairStyles.instance.getAlternateForHat(hairStyle, hatVisual.getClothingItem().hatCategory);
            }
            if (hairStyle != null && hairStyle.isValid()) {
                DebugType.Clothing.debugln("  Adding male hair: %s", hairStyle.name);
                this.addHeadHairItem(chr, modelSlot, hairStyle.model, hairStyle.texture, col);
            }
        } else {
            BeardStyle beardStyle = BeardStyles.instance.FindStyle(humanVisual.getBeardModel());
            if (beardStyle != null && beardStyle.isValid()) {
                if (hatVisual != null && hatVisual.getClothingItem() != null && !StringUtils.isNullOrEmpty(hatVisual.getClothingItem().hatCategory) && hatVisual.getClothingItem().hatCategory.contains("nobeard")) {
                    return;
                }
                DebugType.Clothing.debugln("  Adding beard: %s", beardStyle.name);
                this.addHeadHairItem(chr, modelSlot, beardStyle.model, beardStyle.texture, col);
            }
        }
    }

    public void populateCharacterModelSlot(IsoGameCharacter chr, ModelManager.ModelSlot modelSlot) {
        ClothingItem clothingItem;
        ItemVisual itemVisual;
        int i;
        if (!(chr instanceof IHumanVisual)) {
            DebugType.Clothing.warn("Supplied character is not an IHumanVisual. Ignored. %s", chr);
            return;
        }
        IHumanVisual iHumanVisual = (IHumanVisual)((Object)chr);
        HumanVisual humanVisual = iHumanVisual.getHumanVisual();
        ItemVisuals itemVisuals = new ItemVisuals();
        chr.getItemVisuals(itemVisuals);
        CharacterMask mask = HumanVisual.GetMask(itemVisuals);
        DebugType.Clothing.debugln("characterType:%s, name:%s", chr.getClass().getName(), chr.getName());
        if (mask.isPartVisible(CharacterMask.Part.Head)) {
            this.addHeadHair(chr, modelSlot, humanVisual, itemVisuals.findHat(), false);
            this.addHeadHair(chr, modelSlot, humanVisual, itemVisuals.findMask(), true);
        }
        for (i = itemVisuals.size() - 1; i >= 0; --i) {
            itemVisual = (ItemVisual)itemVisuals.get(i);
            clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                DebugType.Clothing.warn("ClothingItem not found for ItemVisual:%s", itemVisual);
                continue;
            }
            if (this.isItemModelHidden(chr.getBodyLocationGroup(), itemVisuals, itemVisual)) continue;
            if (this.isItemModelAlt(chr.getBodyLocationGroup(), itemVisuals, itemVisual)) {
                this.addClothingItem(chr, modelSlot, itemVisual, clothingItem, true);
                continue;
            }
            this.addClothingItem(chr, modelSlot, itemVisual, clothingItem);
        }
        for (i = humanVisual.getBodyVisuals().size() - 1; i >= 0; --i) {
            itemVisual = (ItemVisual)humanVisual.getBodyVisuals().get(i);
            clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null) {
                DebugType.Clothing.warn("ClothingItem not found for ItemVisual:%s", itemVisual);
                continue;
            }
            this.addClothingItem(chr, modelSlot, itemVisual, clothingItem);
        }
        chr.postUpdateModelTextures();
    }

    public boolean isItemModelHidden(BodyLocationGroup bodyLocationGroup, ItemVisuals visuals, ItemVisual visual) {
        Item item1 = visual.getScriptItem();
        if (item1 == null || bodyLocationGroup.getLocation(item1.getBodyLocation()) == null) {
            return false;
        }
        for (int i = 0; i < visuals.size(); ++i) {
            Item item2;
            if (visuals.get(i) == visual || (item2 = ((ItemVisual)visuals.get(i)).getScriptItem()) == null || bodyLocationGroup.getLocation(item2.getBodyLocation()) == null || !bodyLocationGroup.isHideModel(item2.getBodyLocation(), item1.getBodyLocation())) continue;
            return true;
        }
        return false;
    }

    public boolean isItemModelHidden(ItemVisuals visuals, ItemBodyLocation bodyLocation) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        for (int i = 0; i < visuals.size(); ++i) {
            Item item2 = ((ItemVisual)visuals.get(i)).getScriptItem();
            if (item2 == null || bodyLocationGroup.getLocation(item2.getBodyLocation()) == null || !bodyLocationGroup.isHideModel(item2.getBodyLocation(), bodyLocation)) continue;
            return true;
        }
        return false;
    }

    public boolean isItemModelAlt(BodyLocationGroup bodyLocationGroup, ItemVisuals visuals, ItemVisual visual) {
        Item item1 = visual.getScriptItem();
        if (item1 == null || bodyLocationGroup.getLocation(item1.getBodyLocation()) == null) {
            return false;
        }
        for (int i = 0; i < visuals.size(); ++i) {
            Item item2;
            if (visuals.get(i) == visual || (item2 = ((ItemVisual)visuals.get(i)).getScriptItem()) == null || bodyLocationGroup.getLocation(item2.getBodyLocation()) == null || !bodyLocationGroup.isAltModel(item2.getBodyLocation(), item1.getBodyLocation())) continue;
            return true;
        }
        return false;
    }

    public boolean isItemModelAlt(ItemVisuals visuals, ItemBodyLocation bodyLocation) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        for (int i = 0; i < visuals.size(); ++i) {
            Item item2 = ((ItemVisual)visuals.get(i)).getScriptItem();
            if (item2 == null || bodyLocationGroup.getLocation(item2.getBodyLocation()) == null || !bodyLocationGroup.isAltModel(item2.getBodyLocation(), bodyLocation)) continue;
            return true;
        }
        return false;
    }

    private String processModelFileName(String modelFileName) {
        modelFileName = modelFileName.replaceAll("\\\\", "/");
        modelFileName = modelFileName.toLowerCase(Locale.ENGLISH);
        return modelFileName;
    }

    private void postProcessNewItemInstance(ModelInstance modelInstance, ModelManager.ModelSlot parentSlot, ImmutableColor tintColor) {
        modelInstance.depthBias = 0.0f;
        modelInstance.matrixModel = parentSlot.model;
        modelInstance.tintR = tintColor.r;
        modelInstance.tintG = tintColor.g;
        modelInstance.tintB = tintColor.b;
        modelInstance.parent = parentSlot.model;
        modelInstance.animPlayer = parentSlot.model.animPlayer;
        if (parentSlot.model == modelInstance) {
            DebugLog.General.printStackTrace("ERROR: parentSlot.model and modelInstance are equal");
            DebugLog.General.warn("Model=" + modelInstance.model.name);
        }
        parentSlot.model.sub.add(0, modelInstance);
        parentSlot.sub.add(0, modelInstance);
        modelInstance.setOwner(parentSlot);
    }
}

