/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.joml.Math;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.AttachedItems.AttachedModelName;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.characters.WornItems.BodyLocationGroup;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.IModelCamera;
import zombie.core.opengl.VBORenderer;
import zombie.core.rendering.RenderList;
import zombie.core.rendering.ShaderParameter;
import zombie.core.rendering.ShaderPropertyBlock;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.model.CharacterMask;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelInstanceTextureCreator;
import zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.PopTemplateManager;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.visual.AnimalVisual;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IAnimalVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.objects.ShadowParams;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ResourceLocation;
import zombie.ui.UIManager;
import zombie.util.Lambda;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

public final class AnimatedModel
extends AnimationVariableSource
implements IAnimatable,
IAnimEventCallback,
IActionStateChanged,
IAnimalVisual,
IHumanVisual {
    private String animSetName = "player-avatar";
    private String outfitName;
    private IsoGameCharacter character;
    private IGrappleable grappleable;
    private BaseVisual baseVisual;
    private final HumanVisual humanVisual = new HumanVisual(this);
    private final ItemVisuals itemVisuals = new ItemVisuals();
    private String primaryHandModelName;
    private String secondaryHandModelName;
    private final AttachedModelNames attachedModelNames = new AttachedModelNames();
    private ModelInstance modelInstance;
    private boolean female;
    private boolean zombie;
    private boolean skeleton;
    private String animalType;
    private float finalScale = 1.0f;
    private String state;
    private final Vector2 angle = new Vector2();
    private final Vector3f offset = new Vector3f(0.0f, -0.45f, 0.0f);
    private boolean isometric = true;
    private boolean flipY;
    private float alpha = 1.0f;
    private AnimationPlayer animPlayer;
    private final ActionContext actionContext = new ActionContext(this);
    private final AdvancedAnimator advancedAnimator = new AdvancedAnimator();
    private float trackTime;
    private final String uid;
    private float lightsOriginX;
    private float lightsOriginY;
    private float lightsOriginZ;
    private final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
    private final ColorInfo ambient = new ColorInfo();
    private float highResDepthMultiplier;
    private boolean outside = true;
    private boolean room;
    private boolean updateTextures;
    private boolean clothingChanged;
    private boolean animate = true;
    private boolean showBip01;
    private ModelInstanceTextureCreator textureCreator;
    private int cullFace = 1028;
    private final StateInfo[] stateInfos = new StateInfo[3];
    private boolean ready;
    private static final ObjectPool<AnimatedModelInstanceRenderData> instDataPool = new ObjectPool<AnimatedModelInstanceRenderData>(AnimatedModelInstanceRenderData::new);
    private final UIModelCamera uiModelCamera = new UIModelCamera(this);
    private static final WorldModelCamera worldModelCamera = new WorldModelCamera();

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public AnimatedModel() {
        int i;
        this.uid = String.format("%s-%s", this.getClass().getSimpleName(), UUID.randomUUID());
        this.advancedAnimator.init(this);
        this.advancedAnimator.animCallbackHandlers.add(this);
        this.actionContext.onStateChanged.add(this);
        for (i = 0; i < this.lights.length; ++i) {
            this.lights[i] = new IsoGridSquare.ResultLight();
        }
        for (i = 0; i < this.stateInfos.length; ++i) {
            this.stateInfos[i] = new StateInfo();
        }
    }

    public void setVisual(BaseVisual baseVisual) {
        this.baseVisual = baseVisual;
    }

    public BaseVisual getVisual() {
        return this.baseVisual;
    }

    @Override
    public AnimalVisual getAnimalVisual() {
        return Type.tryCastTo(this.baseVisual, AnimalVisual.class);
    }

    @Override
    public String getAnimalType() {
        return this.animalType;
    }

    @Override
    public float getAnimalSize() {
        return this.finalScale;
    }

    @Override
    public HumanVisual getHumanVisual() {
        return Type.tryCastTo(this.baseVisual, HumanVisual.class);
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        itemVisuals.clear();
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public boolean isZombie() {
        return this.zombie;
    }

    @Override
    public boolean isSkeleton() {
        return this.skeleton;
    }

    public void setAnimSetName(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            throw new IllegalArgumentException("invalid AnimSet \"" + name + "\"");
        }
        this.animSetName = name;
    }

    public void setOutfitName(String name, boolean female, boolean zombie) {
        this.outfitName = name;
        this.female = female;
        this.zombie = zombie;
    }

    public void setCharacter(IsoGameCharacter character) {
        this.outfitName = null;
        if (this.baseVisual != null) {
            this.baseVisual.clear();
        }
        this.itemVisuals.clear();
        if (!(character instanceof IHumanVisual)) {
            return;
        }
        IHumanVisual iHumanVisual = (IHumanVisual)((Object)character);
        character.getItemVisuals(this.itemVisuals);
        this.character = character;
        this.setGrappleable(character);
        if (character.getAttachedItems() != null) {
            this.attachedModelNames.initFrom(character.getAttachedItems());
        }
        if (character instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)character;
            this.setModelData(isoAnimal.getAnimalVisual(), this.itemVisuals, isoAnimal);
        } else {
            this.setModelData(iHumanVisual.getHumanVisual(), this.itemVisuals);
        }
    }

    public void setGrappleable(IGrappleable grappleable) {
        this.grappleable = grappleable;
    }

    public void setSurvivorDesc(SurvivorDesc survivorDesc) {
        this.outfitName = null;
        if (this.baseVisual != null) {
            this.baseVisual.clear();
        }
        this.itemVisuals.clear();
        survivorDesc.getWornItems().getItemVisuals(this.itemVisuals);
        this.attachedModelNames.clear();
        this.setModelData(survivorDesc.getHumanVisual(), this.itemVisuals);
    }

    public void setPrimaryHandModelName(String name) {
        this.primaryHandModelName = name;
    }

    public void setSecondaryHandModelName(String name) {
        this.secondaryHandModelName = name;
    }

    public void setAttachedModelNames(AttachedModelNames attachedModelNames) {
        this.attachedModelNames.copyFrom(attachedModelNames);
    }

    public void setModelData(BaseVisual baseVisual, ItemVisuals itemVisuals) {
        this.setModelData(baseVisual, itemVisuals, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setModelData(BaseVisual baseVisual, ItemVisuals itemVisuals, IsoAnimal animal) {
        Model oldModel;
        AnimationPlayer oldAnimPlayer = this.animPlayer;
        Model model = oldModel = this.animPlayer == null ? null : oldAnimPlayer.getModel();
        if (this.baseVisual != baseVisual) {
            if (this.baseVisual == null || this.baseVisual.getClass() != baseVisual.getClass()) {
                if (baseVisual instanceof AnimalVisual) {
                    this.baseVisual = new AnimalVisual(this);
                }
                if (baseVisual instanceof HumanVisual) {
                    this.baseVisual = new HumanVisual(this);
                }
            }
            this.baseVisual.copyFrom(baseVisual);
        }
        if (this.itemVisuals != itemVisuals) {
            this.itemVisuals.clear();
            PZArrayUtil.addAll(this.itemVisuals, itemVisuals);
        }
        if (this.baseVisual != baseVisual) {
            this.female = false;
            this.zombie = false;
            this.skeleton = false;
            this.animalType = null;
            this.finalScale = 1.0f;
            if (baseVisual instanceof AnimalVisual) {
                AnimalVisual animalVisual = (AnimalVisual)baseVisual;
                this.animalType = animalVisual.getAnimalType();
                this.finalScale = animalVisual.getAnimalSize();
                this.skeleton = animalVisual.isSkeleton();
            }
            if (baseVisual instanceof HumanVisual) {
                HumanVisual humanVisual = (HumanVisual)baseVisual;
                this.female = humanVisual.isFemale();
                this.zombie = humanVisual.isZombie();
                this.skeleton = humanVisual.isSkeleton();
            }
        }
        if (this.modelInstance != null) {
            ModelManager.instance.resetModelInstanceRecurse(this.modelInstance, this);
        }
        Model model2 = baseVisual.getModel();
        if (baseVisual instanceof AnimalVisual) {
            this.character = animal;
        }
        this.getAnimationPlayer().setModel(model2);
        this.modelInstance = ModelManager.instance.newInstance(model2, null, this.getAnimationPlayer());
        this.modelInstance.modelScript = baseVisual.getModelScript();
        this.modelInstance.setOwner(this);
        this.populateCharacterModelSlot();
        this.DoCharacterModelEquipped();
        boolean reset = false;
        if (this.animate) {
            AnimationSet animSet = AnimationSet.GetAnimationSet(this.GetAnimSetName(), false);
            if (animSet != this.advancedAnimator.animSet || oldAnimPlayer != this.getAnimationPlayer() || oldModel != model2) {
                reset = true;
            }
        } else {
            reset = true;
        }
        if (reset) {
            this.advancedAnimator.OnAnimDataChanged(false);
        }
        if (this.animate) {
            ActionGroup actionGroup = ActionGroup.getActionGroup(this.GetAnimSetName());
            if (actionGroup != this.actionContext.getGroup()) {
                this.actionContext.setGroup(actionGroup);
            }
            String stateName = StringUtils.isNullOrWhitespace(this.state) ? this.actionContext.getCurrentStateName() : this.state;
            this.advancedAnimator.setState(stateName, PZArrayUtil.listConvert(this.actionContext.getChildStates(), state -> state.getName()));
        } else if (!StringUtils.isNullOrWhitespace(this.state)) {
            this.advancedAnimator.setState(this.state);
        }
        if (reset) {
            float fpsMultiplier = GameTime.getInstance().fpsMultiplier;
            GameTime.getInstance().fpsMultiplier = 100.0f;
            try {
                this.advancedAnimator.update(this.getAnimationTimeDelta());
            }
            finally {
                GameTime.getInstance().fpsMultiplier = fpsMultiplier;
            }
        }
        if (Core.debug && !this.animate && this.stateInfoMain().readyData.isEmpty()) {
            this.getAnimationPlayer().resetBoneModelTransforms();
        }
        this.trackTime = 0.0f;
        this.stateInfoMain().modelsReady = this.isReadyToRender();
    }

    private float getAnimationTimeDelta() {
        if (this.character != null) {
            return this.character.getAnimationTimeDelta();
        }
        return GameTime.instance.getTimeDelta();
    }

    public void setAmbient(ColorInfo ambient, boolean outside, boolean room) {
        this.ambient.set(ambient.r, ambient.g, ambient.b, 1.0f);
        this.outside = outside;
        this.room = room;
    }

    public void setLights(IsoGridSquare.ResultLight[] lights, float x, float y, float z) {
        this.lightsOriginX = x;
        this.lightsOriginY = y;
        this.lightsOriginZ = z;
        for (int i = 0; i < lights.length; ++i) {
            this.lights[i].copyFrom(lights[i]);
        }
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }

    public void setAngle(Vector2 angle) {
        this.angle.set(angle);
    }

    public void setOffset(float x, float y, float z) {
        this.offset.set(x, y, z);
    }

    public void setOffsetWhileRendering(float x, float y, float z) {
        this.stateInfoRender().offset.set(x, y, z);
    }

    public void setIsometric(boolean iso) {
        this.isometric = iso;
    }

    public boolean isIsometric() {
        return this.isometric;
    }

    public void setFlipY(boolean flip) {
        this.flipY = flip;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setTint(float r, float g, float b) {
        StateInfo stateInfo = this.stateInfoMain();
        stateInfo.tintR = r;
        stateInfo.tintG = g;
        stateInfo.tintB = b;
    }

    public void setTint(ColorInfo tint) {
        this.setTint(tint.r, tint.g, tint.b);
    }

    public void setTrackTime(float trackTime) {
        this.trackTime = trackTime;
    }

    public void setScale(float scale) {
        this.finalScale = PZMath.max(0.0f, scale);
    }

    public float getScale() {
        return this.finalScale;
    }

    public void setCullFace(int culLFace) {
        this.cullFace = culLFace;
    }

    public void setHighResDepthMultiplier(float m) {
        this.highResDepthMultiplier = m;
    }

    public void clothingItemChanged(String itemGuid) {
        this.clothingChanged = true;
    }

    public boolean isAnimate() {
        return this.animate;
    }

    public void setAnimate(boolean animate) {
        this.animate = animate;
    }

    public void setShowBip01(boolean show) {
        this.showBip01 = show;
    }

    private void initOutfit() {
        String outfitName = this.outfitName;
        this.outfitName = null;
        if (StringUtils.isNullOrWhitespace(outfitName)) {
            return;
        }
        ModelManager.instance.create();
        this.baseVisual.dressInNamedOutfit(outfitName, this.itemVisuals);
        this.setModelData(this.baseVisual, this.itemVisuals);
    }

    private void populateCharacterModelSlot() {
        ClothingItem clothingItem;
        ItemVisual itemVisual;
        int i;
        HumanVisual humanVisual = this.getHumanVisual();
        if (humanVisual == null) {
            this.updateTextures = true;
            return;
        }
        CharacterMask mask = HumanVisual.GetMask(this.itemVisuals);
        if (mask.isPartVisible(CharacterMask.Part.Head)) {
            this.addHeadHair(this.itemVisuals.findHat());
        }
        for (i = this.itemVisuals.size() - 1; i >= 0; --i) {
            itemVisual = (ItemVisual)this.itemVisuals.get(i);
            clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null || !clothingItem.isReady() || this.isItemModelHidden(this.itemVisuals, itemVisual)) continue;
            this.addClothingItem(itemVisual, clothingItem);
        }
        for (i = humanVisual.getBodyVisuals().size() - 1; i >= 0; --i) {
            itemVisual = (ItemVisual)humanVisual.getBodyVisuals().get(i);
            clothingItem = itemVisual.getClothingItem();
            if (clothingItem == null || !clothingItem.isReady()) continue;
            this.addClothingItem(itemVisual, clothingItem);
        }
        this.updateTextures = true;
        Lambda.forEachFrom(PZArrayUtil::forEach, this.modelInstance.sub, this.modelInstance, (m, modelInstance) -> {
            m.animPlayer = modelInstance.animPlayer;
        });
    }

    private void addHeadHair(ItemVisual hatVisual) {
        HumanVisual humanVisual = this.getHumanVisual();
        ImmutableColor col = humanVisual.getHairColor();
        ImmutableColor beardcol = humanVisual.getBeardColor();
        if (this.isFemale()) {
            HairStyle hairStyle = HairStyles.instance.FindFemaleStyle(humanVisual.getHairModel());
            if (hairStyle != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                hairStyle = HairStyles.instance.getAlternateForHat(hairStyle, hatVisual.getClothingItem().hatCategory);
            }
            if (hairStyle != null && hairStyle.isValid()) {
                DebugType.Clothing.debugln("  Adding female hair: %s", hairStyle.name);
                this.addHeadHairItem(hairStyle.model, hairStyle.texture, col);
            }
        } else {
            BeardStyle beardStyle;
            HairStyle hairStyle = HairStyles.instance.FindMaleStyle(humanVisual.getHairModel());
            if (hairStyle != null && hatVisual != null && hatVisual.getClothingItem() != null) {
                hairStyle = HairStyles.instance.getAlternateForHat(hairStyle, hatVisual.getClothingItem().hatCategory);
            }
            if (hairStyle != null && hairStyle.isValid()) {
                DebugType.Clothing.debugln("  Adding male hair: %s", hairStyle.name);
                this.addHeadHairItem(hairStyle.model, hairStyle.texture, col);
            }
            if ((beardStyle = BeardStyles.instance.FindStyle(humanVisual.getBeardModel())) != null && beardStyle.isValid()) {
                if (hatVisual != null && hatVisual.getClothingItem() != null && !StringUtils.isNullOrEmpty(hatVisual.getClothingItem().hatCategory) && hatVisual.getClothingItem().hatCategory.contains("nobeard")) {
                    return;
                }
                DebugType.Clothing.debugln("  Adding beard: %s", beardStyle.name);
                this.addHeadHairItem(beardStyle.model, beardStyle.texture, beardcol);
            }
        }
    }

    private void addHeadHairItem(String modelFileName, String textureName, ImmutableColor tintColor) {
        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            DebugType.Clothing.warn("No model specified.");
            return;
        }
        ModelInstance inst = ModelManager.instance.newAdditionalModelInstance(modelFileName = this.processModelFileName(modelFileName), textureName, null, this.modelInstance.animPlayer, null);
        if (inst == null) {
            return;
        }
        this.postProcessNewItemInstance(this.modelInstance, inst, tintColor);
    }

    private void addClothingItem(ItemVisual itemVisual, ClothingItem clothingItem) {
        String modelFileName = clothingItem.getModel(this.female);
        if (StringUtils.isNullOrWhitespace(modelFileName)) {
            DebugType.Clothing.debugln("No model specified by item: %s", clothingItem.mame);
            return;
        }
        modelFileName = this.processModelFileName(modelFileName);
        String textureName = itemVisual.getTextureChoice(clothingItem);
        ImmutableColor tintColor = itemVisual.getTint(clothingItem);
        String attachBone = clothingItem.attachBone;
        String shaderName = clothingItem.shader;
        ModelInstance inst = attachBone != null && !attachBone.isEmpty() ? this.addStatic(modelFileName, textureName, attachBone, shaderName) : ModelManager.instance.newAdditionalModelInstance(modelFileName, textureName, null, this.modelInstance.animPlayer, shaderName);
        if (inst == null) {
            return;
        }
        this.postProcessNewItemInstance(this.modelInstance, inst, tintColor);
        inst.setItemVisual(itemVisual);
    }

    private boolean isItemModelHidden(ItemVisuals visuals, ItemVisual visual) {
        BodyLocationGroup bodyLocationGroup = BodyLocations.getGroup("Human");
        return PopTemplateManager.instance.isItemModelHidden(bodyLocationGroup, visuals, visual);
    }

    private String processModelFileName(String modelFileName) {
        modelFileName = modelFileName.replaceAll("\\\\", "/");
        modelFileName = modelFileName.toLowerCase(Locale.ENGLISH);
        return modelFileName;
    }

    private void postProcessNewItemInstance(ModelInstance parentInstance, ModelInstance modelInstance, ImmutableColor tintColor) {
        modelInstance.depthBias = 0.0f;
        modelInstance.matrixModel = this.modelInstance;
        modelInstance.tintR = tintColor.r;
        modelInstance.tintG = tintColor.g;
        modelInstance.tintB = tintColor.b;
        modelInstance.animPlayer = this.modelInstance.animPlayer;
        parentInstance.sub.add(modelInstance);
        modelInstance.setOwner(this);
    }

    private void DoCharacterModelEquipped() {
        ModelInstance modelInstance2;
        if (!StringUtils.isNullOrWhitespace(this.primaryHandModelName)) {
            modelInstance2 = this.addStatic(this.primaryHandModelName, "Bip01_Prop1");
            this.postProcessNewItemInstance(this.modelInstance, modelInstance2, ImmutableColor.white);
        }
        if (!StringUtils.isNullOrWhitespace(this.secondaryHandModelName)) {
            modelInstance2 = this.addStatic(this.secondaryHandModelName, "Bip01_Prop2");
            this.postProcessNewItemInstance(this.modelInstance, modelInstance2, ImmutableColor.white);
        }
        for (int i = 0; i < this.attachedModelNames.size(); ++i) {
            AttachedModelName amn = this.attachedModelNames.get(i);
            if (ModelManager.instance.shouldHideModel(this.itemVisuals, ItemBodyLocation.get(ResourceLocation.of(amn.attachmentNameSelf))) || ModelManager.instance.shouldHideModel(this.itemVisuals, ItemBodyLocation.get(ResourceLocation.of(amn.attachmentNameParent)))) continue;
            ModelInstance modelInstance1 = ModelManager.instance.addStatic(null, amn.modelName, amn.attachmentNameSelf, amn.attachmentNameParent);
            this.postProcessNewItemInstance(this.modelInstance, modelInstance1, ImmutableColor.white);
            if (amn.bloodLevel > 0.0f && !Core.getInstance().getOptionSimpleWeaponTextures()) {
                ModelInstanceTextureInitializer miti = ModelInstanceTextureInitializer.alloc();
                miti.init(modelInstance1, amn.bloodLevel);
                modelInstance1.setTextureInitializer(miti);
            }
            for (int j = 0; j < amn.getChildCount(); ++j) {
                AttachedModelName amn2 = amn.getChildByIndex(j);
                ModelInstance subInstance = ModelManager.instance.addStatic(modelInstance1, amn2.modelName, amn2.attachmentNameSelf, amn2.attachmentNameParent);
                modelInstance1.sub.remove(subInstance);
                this.postProcessNewItemInstance(modelInstance1, subInstance, ImmutableColor.white);
            }
        }
    }

    private ModelInstance addStatic(String staticModel, String boneName) {
        String meshName = staticModel;
        String texName = staticModel;
        String shaderName = null;
        ModelScript modelScript = ScriptManager.instance.getModelScript(staticModel);
        if (modelScript != null) {
            meshName = modelScript.getMeshName();
            texName = modelScript.getTextureName();
            shaderName = modelScript.getShaderName();
        }
        return this.addStatic(meshName, texName, boneName, shaderName);
    }

    private ModelInstance addStatic(String meshName, String texName, String boneName, String shaderName) {
        DebugType.ModelManager.debugln("Adding static Model: %s", meshName);
        Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, true, shaderName, false);
        if (model == null) {
            ModelManager.instance.loadStaticModel(meshName.toLowerCase(), texName, shaderName);
            model = ModelManager.instance.getLoadedModel(meshName, texName, true, shaderName);
            if (model == null) {
                DebugType.ModelManager.error("ModelManager.addStatic> Model not found. model:" + meshName + " tex:" + texName);
                return null;
            }
        }
        ModelInstance inst = ModelManager.instance.newInstance(model, null, this.modelInstance.animPlayer);
        inst.parent = this.modelInstance;
        if (this.modelInstance.animPlayer != null) {
            inst.parentBone = this.modelInstance.animPlayer.getSkinningBoneIndex(boneName, inst.parentBone);
            inst.parentBoneName = boneName;
        }
        return inst;
    }

    private StateInfo stateInfoMain() {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        return this.stateInfos[stateIndex];
    }

    private StateInfo stateInfoRender() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        return this.stateInfos[stateIndex];
    }

    public void update() {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("AnimatedModel.Update");){
            this.updateInternal();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void updateInternal() {
        this.initOutfit();
        if (this.clothingChanged) {
            this.clothingChanged = false;
            this.setModelData(this.baseVisual, this.itemVisuals);
        }
        this.modelInstance.SetForceDir(this.angle);
        GameTime gameTime = GameTime.getInstance();
        float fpsMultiplier = gameTime.fpsMultiplier;
        float multiplier = gameTime.getTrueMultiplier();
        if (this.animate) {
            ModelInstance modelInstance1;
            gameTime.setMultiplier(1.0f);
            if (UIManager.useUiFbo) {
                gameTime.fpsMultiplier *= GameWindow.averageFPS / (float)Core.getInstance().getOptionUIRenderFPS();
            }
            this.actionContext.update();
            this.advancedAnimator.update(this.getAnimationTimeDelta());
            this.animPlayer.Update();
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            StateInfo stateInfo = this.stateInfos[stateIndex];
            if (!stateInfo.readyData.isEmpty() && (modelInstance1 = stateInfo.readyData.get((int)0).modelInstance) != this.modelInstance && modelInstance1.animPlayer != this.modelInstance.animPlayer) {
                modelInstance1.Update(this.getAnimationTimeDelta());
            }
            gameTime.fpsMultiplier = fpsMultiplier;
            gameTime.setMultiplier(multiplier);
        } else {
            gameTime.fpsMultiplier = 100.0f;
            try {
                this.advancedAnimator.update(this.getAnimationTimeDelta());
            }
            finally {
                gameTime.fpsMultiplier = fpsMultiplier;
            }
            if (this.trackTime > 0.0f && this.animPlayer.getMultiTrack().getTrackCount() > 0) {
                this.animPlayer.getMultiTrack().getTracks().get(0).setCurrentTimeValue(this.trackTime);
            }
            this.animPlayer.Update(0.0f);
        }
    }

    private boolean isModelInstanceReadyNoChildren(ModelInstance modelInstance) {
        if (modelInstance.model == null || !modelInstance.model.isReady()) {
            return false;
        }
        return modelInstance.model.mesh.isReady() && modelInstance.model.mesh.vb != null;
    }

    private boolean isModelInstanceReady(ModelInstance modelInstance) {
        if (!this.isModelInstanceReadyNoChildren(modelInstance)) {
            return false;
        }
        for (int i = 0; i < modelInstance.sub.size(); ++i) {
            ModelInstance subInstance = modelInstance.sub.get(i);
            if (this.isModelInstanceReady(subInstance)) continue;
            return false;
        }
        return true;
    }

    private void incrementRefCount(ModelInstance modelInstance) {
        ++modelInstance.renderRefCount;
        for (int i = 0; i < modelInstance.sub.size(); ++i) {
            ModelInstance subInstance = modelInstance.sub.get(i);
            this.incrementRefCount(subInstance);
        }
    }

    private void initRenderData(StateInfo stateInfo, AnimatedModelInstanceRenderData parentData, ModelInstance modelInstance) {
        AnimatedModelInstanceRenderData renderData = instDataPool.alloc();
        renderData.initModel(modelInstance, parentData);
        renderData.init();
        renderData.modelInstance.targetDepth = 0.5f;
        stateInfo.instData.add(renderData);
        renderData.transformToParent(parentData);
        for (int i = 0; i < modelInstance.sub.size(); ++i) {
            ModelInstance subInstance = modelInstance.sub.get(i);
            this.initRenderData(stateInfo, renderData, subInstance);
        }
    }

    public boolean isReadyToRender() {
        if (!this.animPlayer.isReady()) {
            return false;
        }
        return this.isModelInstanceReady(this.modelInstance);
    }

    public int renderMain() {
        StateInfo stateInfo = this.stateInfoMain();
        if (this.modelInstance != null) {
            if (this.updateTextures) {
                this.updateTextures = false;
                this.textureCreator = ModelInstanceTextureCreator.alloc();
                this.textureCreator.init(this.getVisual(), this.itemVisuals, this.modelInstance);
            }
            this.incrementRefCount(this.modelInstance);
            instDataPool.release((List<AnimatedModelInstanceRenderData>)stateInfo.instData);
            stateInfo.instData.clear();
            if (!stateInfo.modelsReady && this.isReadyToRender()) {
                float fpsMultiplier = GameTime.getInstance().fpsMultiplier;
                GameTime.getInstance().fpsMultiplier = 100.0f;
                try {
                    this.advancedAnimator.update(this.getAnimationTimeDelta());
                }
                finally {
                    GameTime.getInstance().fpsMultiplier = fpsMultiplier;
                }
                this.animPlayer.Update(0.0f);
                stateInfo.modelsReady = true;
            }
            this.initRenderData(stateInfo, null, this.modelInstance);
        }
        stateInfo.modelInstance = this.modelInstance;
        stateInfo.textureCreator = this.textureCreator != null && !this.textureCreator.isRendered() ? this.textureCreator : null;
        for (int i = 0; i < stateInfo.readyData.size(); ++i) {
            AnimatedModelInstanceRenderData data = stateInfo.readyData.get(i);
            if (data.modelInstance.animPlayer != null && data.modelInstance.animPlayer.getModel() != data.modelInstance.model) continue;
            data.init();
            data.transformToParent(stateInfo.getParentData(data.modelInstance));
        }
        stateInfo.offset.set(this.offset);
        stateInfo.rendered = false;
        return SpriteRenderer.instance.getMainStateIndex();
    }

    public boolean isRendered() {
        return this.stateInfoRender().rendered;
    }

    private void doneWithTextureCreator(ModelInstanceTextureCreator tc) {
        if (tc == null) {
            return;
        }
        for (int i = 0; i < this.stateInfos.length; ++i) {
            if (this.stateInfos[i].textureCreator != tc) continue;
            return;
        }
        if (tc.isRendered()) {
            tc.postRender();
            if (tc == this.textureCreator) {
                this.textureCreator = null;
            }
        } else if (tc != this.textureCreator) {
            tc.postRender();
        }
    }

    private void release(ArrayList<AnimatedModelInstanceRenderData> instData) {
        for (int i = 0; i < instData.size(); ++i) {
            AnimatedModelInstanceRenderData data = instData.get(i);
            if (data.modelInstance.getTextureInitializer() != null) {
                data.modelInstance.getTextureInitializer().postRender();
            }
            ModelManager.instance.derefModelInstance(data.modelInstance);
        }
        instDataPool.release((List<AnimatedModelInstanceRenderData>)instData);
    }

    public void postRender(boolean bRendered) {
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        StateInfo stateInfo = this.stateInfos[stateIndex];
        ModelInstanceTextureCreator tc = stateInfo.textureCreator;
        stateInfo.textureCreator = null;
        this.doneWithTextureCreator(tc);
        stateInfo.modelInstance = null;
        if (this.animate && stateInfo.rendered) {
            this.release(stateInfo.readyData);
            stateInfo.readyData.clear();
            PZArrayUtil.addAll(stateInfo.readyData, stateInfo.instData);
            stateInfo.instData.clear();
        } else if (!this.animate) {
            // empty if block
        }
        this.release(stateInfo.instData);
        stateInfo.instData.clear();
    }

    public void setTargetDepth(float targetDepth) {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        StateInfo stateInfo = this.stateInfos[stateIndex];
        for (int i = 0; i < stateInfo.instData.size(); ++i) {
            AnimatedModelInstanceRenderData mird = stateInfo.instData.get(i);
            mird.modelInstance.targetDepth = targetDepth;
        }
    }

    public void DoRender(IModelCamera camera) {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        StateInfo stateInfo = this.stateInfos[stateIndex];
        this.ready = true;
        ModelInstanceTextureCreator textureCreator = stateInfo.textureCreator;
        if (textureCreator != null && !textureCreator.isRendered()) {
            textureCreator.render();
            if (!textureCreator.isRendered()) {
                this.ready = false;
            }
        }
        for (int i = 0; i < stateInfo.instData.size(); ++i) {
            ModelInstanceTextureInitializer miti;
            AnimatedModelInstanceRenderData mird = stateInfo.instData.get(i);
            if (!this.isModelInstanceReadyNoChildren(mird.modelInstance)) {
                this.ready = false;
            }
            if ((miti = mird.modelInstance.getTextureInitializer()) == null || miti.isRendered()) continue;
            miti.render();
            if (miti.isRendered()) continue;
            this.ready = false;
        }
        if (this.ready && !stateInfo.modelsReady) {
            this.ready = false;
        }
        if (!this.ready && stateInfo.readyData.isEmpty()) {
            return;
        }
        GL11.glPushClientAttrib(-1);
        GL11.glPushAttrib(1048575);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.0f);
        camera.Begin();
        this.StartCharacter();
        this.Render();
        boolean bDepthDebugBox = false;
        this.EndCharacter();
        camera.End();
        GL11.glDepthFunc(519);
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
        Texture.lastTextureID = -1;
        GLStateRenderThread.restore();
        SpriteRenderer.ringBuffer.restoreVbos = true;
        stateInfo.rendered = this.ready;
    }

    public void DoRender(int x, int y, int w, int h, float sizeV, float animPlayerAngle) {
        GL11.glClear(256);
        this.uiModelCamera.viewportX = x;
        this.uiModelCamera.viewportY = y;
        this.uiModelCamera.viewportW = w;
        this.uiModelCamera.viewportH = h;
        this.uiModelCamera.sizeV = sizeV;
        this.uiModelCamera.animPlayerAngle = animPlayerAngle;
        this.DoRender(this.uiModelCamera);
    }

    public void DoRenderToWorld(float x, float y, float z, float animPlayerAngle) {
        AnimatedModel.worldModelCamera.posX = x;
        AnimatedModel.worldModelCamera.posY = y;
        AnimatedModel.worldModelCamera.posZ = z;
        AnimatedModel.worldModelCamera.angle = animPlayerAngle;
        AnimatedModel.worldModelCamera.animatedModel = this;
        this.DoRender(worldModelCamera);
    }

    private void debugDrawAxes() {
        if (Core.debug && DebugOptions.instance.model.render.axis.getValue()) {
            Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 1.0f, 4.0f);
        }
    }

    private void StartCharacter() {
        GL11.glEnable(2929);
        GL11.glEnable(3042);
        if (UIManager.useUiFbo) {
            GL14.glBlendFuncSeparate(770, 771, 1, 771);
        } else {
            GL11.glBlendFunc(770, 771);
        }
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.0f);
        GL11.glDisable(3089);
        GL11.glDepthMask(true);
    }

    private void EndCharacter() {
        GL11.glDepthMask(false);
        GL11.glViewport(0, 0, Core.width, Core.height);
    }

    private void Render() {
        int stateIndex = SpriteRenderer.instance.getRenderStateIndex();
        StateInfo stateInfo = this.stateInfos[stateIndex];
        ModelInstance modelInstance = stateInfo.modelInstance;
        if (modelInstance == null) {
            boolean bl = true;
        } else {
            ArrayList<AnimatedModelInstanceRenderData> instData = this.ready ? stateInfo.instData : stateInfo.readyData;
            for (int i = 0; i < instData.size(); ++i) {
                AnimatedModelInstanceRenderData instData1 = instData.get(i);
                this.DrawChar(instData1);
            }
        }
        this.debugDrawAxes();
    }

    private void DrawChar(AnimatedModelInstanceRenderData instData) {
        StateInfo stateInfo = this.stateInfoRender();
        ModelInstance inst = instData.modelInstance;
        FloatBuffer matrixPalette = instData.matrixPalette;
        if (inst == null) {
            return;
        }
        if (inst.animPlayer == null) {
            return;
        }
        if (!inst.animPlayer.hasSkinningData()) {
            return;
        }
        if (inst.model == null) {
            return;
        }
        if (!inst.model.isReady()) {
            return;
        }
        if (inst.tex == null && inst.model.tex == null) {
            return;
        }
        if (inst.model.effect == null) {
            inst.model.CreateShader("basicEffect");
        }
        GL11.glEnable(2884);
        GL11.glCullFace(this.cullFace);
        GL11.glEnable(2929);
        GL11.glEnable(3008);
        GL11.glDepthFunc(513);
        GL11.glDepthRange(0.0, 1.0);
        GL11.glAlphaFunc(516, 0.01f);
        if (!inst.model.effect.isInstanced()) {
            this.DrawCharSingular(instData);
            return;
        }
        instData.properties.SetFloat("Alpha", this.alpha);
        instData.properties.SetVector3("AmbientColour", this.ambient.r * 0.45f, this.ambient.g * 0.45f, this.ambient.b * 0.45f);
        instData.properties.SetVector3("TintColour", this.modelInstance.tintR * stateInfo.tintR, this.modelInstance.tintG * stateInfo.tintG, this.modelInstance.tintB * stateInfo.tintB);
        if (this.highResDepthMultiplier != 0.0f) {
            instData.properties.SetFloat("HighResDepthMultiplier", this.highResDepthMultiplier);
        }
        org.joml.Matrix4f mvp = Core.getInstance().modelViewMatrixStack.alloc();
        VertexBufferObject.getModelViewProjection(mvp);
        instData.properties.SetMatrix4("mvp", mvp).transpose();
        Core.getInstance().modelViewMatrixStack.release(mvp);
        RenderList.DrawImmediate(null, instData);
        ShaderHelper.forgetCurrentlyBound();
        ShaderHelper.glUseProgramObjectARB(0);
    }

    private void DrawCharSingular(AnimatedModelInstanceRenderData instData) {
        int boneIndex;
        StateInfo stateInfo = this.stateInfoRender();
        ModelInstance inst = instData.modelInstance;
        FloatBuffer matrixPalette = instData.matrixPalette;
        Shader effect = inst.model.effect;
        if (effect != null) {
            effect.Start();
            if (inst.model.isStatic) {
                effect.setTransformMatrix(instData.xfrm, true);
            } else {
                effect.setMatrixPalette(matrixPalette, true);
            }
            effect.setLight(0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
            effect.setLight(1, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
            effect.setLight(2, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
            effect.setLight(3, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
            effect.setLight(4, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
            float lightMul = 0.7f;
            for (int i = 0; i < this.lights.length; ++i) {
                IsoGridSquare.ResultLight light = this.lights[i];
                if (light.radius <= 0) continue;
                effect.setLight(i, (float)light.x + 0.5f, (float)light.y + 0.5f, (float)light.z + 0.5f, light.r * 0.7f, light.g * 0.7f, light.b * 0.7f, light.radius, instData.animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, null);
            }
            if (inst.tex != null) {
                effect.setTexture(inst.tex, "Texture", 0);
            } else if (inst.model.tex != null) {
                effect.setTexture(inst.model.tex, "Texture", 0);
            }
            if (this.outside) {
                float mul = 1.7f;
                effect.setLight(3, this.lightsOriginX - 2.0f, this.lightsOriginY - 2.0f, this.lightsOriginZ + 1.0f, this.ambient.r * 1.7f / 4.0f, this.ambient.g * 1.7f / 4.0f, this.ambient.b * 1.7f / 4.0f, 5000.0f, instData.animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, null);
                effect.setLight(4, this.lightsOriginX + 2.0f, this.lightsOriginY + 2.0f, this.lightsOriginZ + 1.0f, this.ambient.r * 1.7f / 4.0f, this.ambient.g * 1.7f / 4.0f, this.ambient.b * 1.7f / 4.0f, 5000.0f, instData.animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, null);
            } else if (this.room) {
                float mul = 1.7f;
                effect.setLight(4, this.lightsOriginX + 2.0f, this.lightsOriginY + 2.0f, this.lightsOriginZ + 1.0f, this.ambient.r * 1.7f / 4.0f, this.ambient.g * 1.7f / 4.0f, this.ambient.b * 1.7f / 4.0f, 5000.0f, instData.animPlayerAngle, this.lightsOriginX, this.lightsOriginY, this.lightsOriginZ, null);
            }
            float targetDepth = inst.targetDepth;
            effect.setTargetDepth(targetDepth);
            effect.setDepthBias(inst.depthBias / 50.0f);
            effect.setAmbient(this.ambient.r * 0.45f, this.ambient.g * 0.45f, this.ambient.b * 0.45f);
            effect.setLightingAmount(1.0f);
            effect.setHueShift(inst.hue);
            effect.setTint(inst.tintR * stateInfo.tintR, inst.tintG * stateInfo.tintG, inst.tintB * stateInfo.tintB);
            effect.setAlpha(this.alpha);
            effect.setScale(this.finalScale);
            if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                for (int i = 0; i < 5; ++i) {
                    effect.setLight(i, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, Float.NaN, inst);
                }
                effect.setAmbient(1.0f, 1.0f, 1.0f);
            }
            if (this.highResDepthMultiplier != 0.0f) {
                effect.setHighResDepthMultiplier(this.highResDepthMultiplier);
            }
        }
        inst.model.mesh.Draw(effect);
        if (effect != null) {
            if (this.highResDepthMultiplier != 0.0f) {
                effect.setHighResDepthMultiplier(0.0f);
            }
            effect.setScale(1.0f);
            effect.End();
        }
        DefaultShader.isActive = false;
        ShaderHelper.forgetCurrentlyBound();
        GL20.glUseProgram(0);
        if (Core.debug && DebugOptions.instance.model.render.lights.getValue() && inst.parent == null) {
            if (this.lights[0].radius > 0) {
                Model.debugDrawLightSource(this.lights[0].x, this.lights[0].y, this.lights[0].z, 0.0f, 0.0f, 0.0f, -instData.animPlayerAngle);
            }
            if (this.lights[1].radius > 0) {
                Model.debugDrawLightSource(this.lights[1].x, this.lights[1].y, this.lights[1].z, 0.0f, 0.0f, 0.0f, -instData.animPlayerAngle);
            }
            if (this.lights[2].radius > 0) {
                Model.debugDrawLightSource(this.lights[2].x, this.lights[2].y, this.lights[2].z, 0.0f, 0.0f, 0.0f, -instData.animPlayerAngle);
            }
        }
        if (Core.debug && DebugOptions.instance.model.render.bones.getValue()) {
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);
            vbor.setLineWidth(1.0f);
            vbor.setDepthTest(false);
            for (int i = 0; i < inst.animPlayer.getModelTransformsCount(); ++i) {
                int parentIdx = inst.animPlayer.getSkinningData().skeletonHierarchy.get(i);
                if (parentIdx < 0) continue;
                Matrix4f mtx1 = inst.animPlayer.getModelTransformAt(i);
                Matrix4f mtx2 = inst.animPlayer.getModelTransformAt(parentIdx);
                Color c = Model.debugDrawColours[i % Model.debugDrawColours.length];
                vbor.addLine(mtx1.m03, mtx1.m13, mtx1.m23, mtx2.m03, mtx2.m13, mtx2.m23, c.r, c.g, c.b, 1.0f);
            }
            vbor.endRun();
            vbor.flush();
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            GL11.glEnable(2929);
        }
        if (this.showBip01 && inst.animPlayer.getModelTransformsCount() > 0 && (boneIndex = inst.animPlayer.getSkinningBoneIndex("Bip01", -1)) != -1) {
            Matrix4f mtx = inst.animPlayer.getModelTransformAt(boneIndex);
            Model.debugDrawAxis(mtx.m03, 0.0f * mtx.m13, mtx.m23, 0.1f, 4.0f);
        }
        ShaderHelper.glUseProgramObjectARB(0);
    }

    public ShadowParams calculateShadowParams(ShadowParams sp, boolean bRagdoll) {
        return IsoGameCharacter.calculateShadowParams(this.getAnimationPlayer(), this.getAnimalSize(), bRagdoll, sp);
    }

    public void releaseAnimationPlayer() {
        if (this.animPlayer != null) {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        if (StringUtils.isNullOrWhitespace(event.eventName)) {
            return;
        }
        String stateName = AnimLayer.getCurrentStateName(sender);
        this.actionContext.reportEvent(stateName, event.eventName);
    }

    @Override
    public boolean hasAnimationPlayer() {
        return true;
    }

    @Override
    public IGrappleable getGrappleable() {
        return this.grappleable;
    }

    @Override
    public AnimationPlayer getAnimationPlayer() {
        Model model = this.getVisual().getModel();
        if (this.animPlayer != null && this.animPlayer.getModel() != model) {
            this.animPlayer = Pool.tryRelease(this.animPlayer);
        }
        if (this.animPlayer == null) {
            this.animPlayer = AnimationPlayer.alloc(model);
        }
        return this.animPlayer;
    }

    @Override
    public void actionStateChanged(ActionContext sender) {
        this.advancedAnimator.setState(sender.getCurrentStateName(), PZArrayUtil.listConvert(sender.getChildStates(), state -> state.getName()));
    }

    @Override
    public AnimationPlayerRecorder getAnimationRecorder() {
        return null;
    }

    @Override
    public boolean isAnimationRecorderActive() {
        return false;
    }

    @Override
    public ActionContext getActionContext() {
        return this.actionContext;
    }

    @Override
    public AdvancedAnimator getAdvancedAnimator() {
        return this.advancedAnimator;
    }

    @Override
    public ModelInstance getModelInstance() {
        return this.modelInstance;
    }

    @Override
    public String GetAnimSetName() {
        return this.animSetName;
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    public static final class StateInfo {
        ModelInstance modelInstance;
        ModelInstanceTextureCreator textureCreator;
        final ArrayList<AnimatedModelInstanceRenderData> instData = new ArrayList();
        final ArrayList<AnimatedModelInstanceRenderData> readyData = new ArrayList();
        boolean modelsReady;
        boolean rendered;
        float tintR = 1.0f;
        float tintG = 1.0f;
        float tintB = 1.0f;
        final Vector3f offset = new Vector3f();

        AnimatedModelInstanceRenderData getParentData(ModelInstance inst) {
            for (int i = 0; i < this.readyData.size(); ++i) {
                AnimatedModelInstanceRenderData data = this.readyData.get(i);
                if (data.modelInstance != inst.parent) continue;
                return data;
            }
            return null;
        }
    }

    private final class UIModelCamera
    extends ModelCamera {
        int viewportX;
        int viewportY;
        int viewportW;
        int viewportH;
        float sizeV;
        float animPlayerAngle;
        final /* synthetic */ AnimatedModel this$0;

        private UIModelCamera(AnimatedModel animatedModel) {
            AnimatedModel animatedModel2 = animatedModel;
            Objects.requireNonNull(animatedModel2);
            this.this$0 = animatedModel2;
        }

        @Override
        public void Begin() {
            GL11.glViewport(this.viewportX, this.viewportY, this.viewportW, this.viewportH);
            org.joml.Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
            float xScale = (float)this.viewportW / (float)this.viewportH;
            if (this.this$0.flipY) {
                projection.setOrtho(-this.sizeV * xScale, this.sizeV * xScale, this.sizeV, -this.sizeV, -100.0f, 100.0f);
            } else {
                projection.setOrtho(-this.sizeV * xScale, this.sizeV * xScale, -this.sizeV, this.sizeV, -100.0f, 100.0f);
            }
            float f = Math.sqrt(2048.0f);
            projection.scale(-f, f, f);
            Core.getInstance().projectionMatrixStack.push(projection);
            org.joml.Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
            modelView.identity();
            if (this.this$0.isometric) {
                modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
                modelView.rotate(this.animPlayerAngle + 0.7853982f, 0.0f, 1.0f, 0.0f);
            } else {
                modelView.rotate(this.animPlayerAngle, 0.0f, 1.0f, 0.0f);
            }
            Vector3f offset = this.this$0.stateInfoRender().offset;
            modelView.translate(offset.x(), offset.y(), offset.z());
            Core.getInstance().modelViewMatrixStack.push(modelView);
        }

        @Override
        public void End() {
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
        }
    }

    public static class AnimatedModelInstanceRenderData {
        public Model model;
        public Texture tex;
        public ModelInstance modelInstance;
        public FloatBuffer matrixPalette;
        private boolean matrixPaletteValid;
        public final org.joml.Matrix4f xfrm = new org.joml.Matrix4f();
        float animPlayerAngle;
        public boolean ignoreLighting;
        public final ShaderPropertyBlock properties = new ShaderPropertyBlock();
        public AnimatedModelInstanceRenderData parent;

        public void initMatrixPalette() {
            this.animPlayerAngle = Float.NaN;
            this.matrixPaletteValid = false;
            if (this.modelInstance.animPlayer != null && this.modelInstance.animPlayer.isReady()) {
                this.animPlayerAngle = this.modelInstance.animPlayer.getRenderedAngle();
                if (!this.modelInstance.model.isStatic) {
                    SkinningData skinningData = (SkinningData)this.modelInstance.model.tag;
                    if (Core.debug && skinningData == null) {
                        DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
                    }
                    Matrix4f[] skinTransforms = this.modelInstance.animPlayer.getSkinTransforms(skinningData);
                    int matrixFloats = 16;
                    if (this.matrixPalette == null || this.matrixPalette.capacity() < skinTransforms.length * 16) {
                        this.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
                    }
                    this.matrixPalette.clear();
                    for (int i = 0; i < skinTransforms.length; ++i) {
                        skinTransforms[i].store(this.matrixPalette);
                    }
                    this.matrixPalette.flip();
                    this.matrixPaletteValid = true;
                }
            }
        }

        public AnimatedModelInstanceRenderData init() {
            if (this.matrixPaletteValid) {
                Matrix4f[] bones;
                ShaderParameter palette = this.properties.GetParameter("MatrixPalette");
                if (palette == null) {
                    bones = new Matrix4f[60];
                    this.properties.SetMatrix4Array("MatrixPalette", bones);
                } else {
                    bones = palette.GetMatrix4Array();
                }
                int numBones = this.matrixPalette.limit() / 64;
                for (int i = 0; i < numBones; ++i) {
                    bones[i].load(this.matrixPalette);
                }
                this.matrixPalette.position(0);
            }
            if (this.modelInstance.getTextureInitializer() != null) {
                this.modelInstance.getTextureInitializer().renderMain();
            }
            this.UpdateCharacter(this.modelInstance.model.effect);
            return this;
        }

        public void initModel(ModelInstance modelInstance, AnimatedModelInstanceRenderData parentData) {
            this.xfrm.identity();
            this.modelInstance = modelInstance;
            this.parent = parentData;
            this.ignoreLighting = false;
            if (!modelInstance.model.isStatic && this.matrixPalette == null) {
                this.matrixPalette = BufferUtils.createFloatBuffer(960);
            }
            this.initMatrixPalette();
        }

        public void UpdateCharacter(Shader shader) {
            this.properties.SetFloat("Alpha", 1.0f);
            this.properties.SetVector2("UVScale", 1.0f, 1.0f);
            this.properties.SetFloat("targetDepth", this.modelInstance.targetDepth);
            this.properties.SetFloat("DepthBias", this.modelInstance.depthBias / 50.0f);
            this.properties.SetFloat("LightingAmount", 1.0f);
            this.properties.SetFloat("HueChange", this.modelInstance.hue);
            this.properties.SetVector3("TintColour", 1.0f, 1.0f, 1.0f);
            if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                for (int i = 0; i < 4; ++i) {
                    this.properties.SetVector3ArrayElement("LightDirection", i, 0.0f, 1.0f, 0.0f);
                }
                this.properties.SetVector3("AmbientColour", 1.0f, 1.0f, 1.0f);
            }
            this.properties.SetMatrix4("transform", this.xfrm);
        }

        public AnimatedModelInstanceRenderData transformToParent(AnimatedModelInstanceRenderData parentData) {
            ModelAttachment selfAttachment;
            if (this.modelInstance instanceof VehicleModelInstance || this.modelInstance instanceof VehicleSubModelInstance) {
                return this;
            }
            if (parentData == null) {
                return this;
            }
            this.xfrm.set(parentData.xfrm);
            this.xfrm.transpose();
            org.joml.Matrix4f attachmentXfrm = (org.joml.Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
            ModelAttachment parentAttachment = parentData.modelInstance.getAttachmentById(this.modelInstance.attachmentNameParent);
            if (parentAttachment == null && this.modelInstance.parentBoneName != null) {
                parentAttachment = parentData.modelInstance.getAttachmentById(this.modelInstance.parentBoneName);
            }
            if (parentAttachment == null) {
                if (this.modelInstance.parentBoneName != null && parentData.modelInstance.animPlayer != null) {
                    ModelInstanceRenderData.applyBoneTransform(parentData.modelInstance, this.modelInstance.parentBoneName, this.xfrm);
                }
            } else {
                ModelInstanceRenderData.applyBoneTransform(parentData.modelInstance, parentAttachment.getBone(), this.xfrm);
                ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
                this.xfrm.mul(attachmentXfrm);
            }
            if ((selfAttachment = this.modelInstance.getAttachmentById(this.modelInstance.attachmentNameSelf)) == null && this.modelInstance.parentBoneName != null) {
                selfAttachment = this.modelInstance.getAttachmentById(this.modelInstance.parentBoneName);
            }
            if (selfAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }
                this.xfrm.mul(attachmentXfrm);
            }
            ModelInstanceRenderData.postMultiplyMeshTransform(this.xfrm, this.modelInstance.model.mesh);
            if (this.modelInstance.scale != 1.0f) {
                this.xfrm.scale(this.modelInstance.scale);
            }
            this.xfrm.transpose();
            BaseVehicle.TL_matrix4f_pool.get().release(attachmentXfrm);
            return this;
        }
    }

    private static final class WorldModelCamera
    extends ModelCamera {
        float posX;
        float posY;
        float posZ;
        float angle;
        AnimatedModel animatedModel;

        private WorldModelCamera() {
        }

        @Override
        public void Begin() {
            Core.getInstance().DoPushIsoStuff(this.posX, this.posY, this.posZ, this.angle, false);
            GL11.glDepthMask(true);
            if (PerformanceSettings.fboRenderChunk) {
                float targetDepth = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)this.posX, (float)this.posY, (float)this.posZ).depthStart;
                float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
                targetDepth = targetDepth - (depthBufferValue + 1.0f) / 2.0f + 0.5f;
                this.animatedModel.setTargetDepth(targetDepth);
            } else {
                this.animatedModel.setTargetDepth(0.5f);
            }
        }

        @Override
        public void End() {
            Core.getInstance().DoPopIsoStuff();
        }
    }
}

