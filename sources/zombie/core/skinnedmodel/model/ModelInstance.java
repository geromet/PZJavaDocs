/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import org.joml.AxisAngle4f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceTextureInitializer;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.SoftwareModelMeshInstance;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.IsoRoom;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.ReferencedObject;
import zombie.util.StringUtils;

public class ModelInstance
extends ReferencedObject {
    public static final float MODEL_LIGHT_MULT_OUTSIDE = 1.7f;
    public static final float MODEL_LIGHT_MULT_ROOM = 1.7f;
    public Model model;
    public AnimationPlayer animPlayer;
    public SkinningData data;
    public Texture tex;
    public ModelInstanceTextureInitializer textureInitializer;
    public IsoGameCharacter character;
    public IsoMovingObject object;
    public float tintR = 1.0f;
    public float tintG = 1.0f;
    public float tintB = 1.0f;
    public ModelInstance parent;
    public int parentBone;
    public String parentBoneName;
    public float hue;
    public float depthBias;
    public ModelInstance matrixModel;
    public SoftwareModelMeshInstance softwareMesh;
    public final ArrayList<ModelInstance> sub = new ArrayList();
    public float targetDepth;
    private int instanceSkip;
    private ItemVisual itemVisual;
    public boolean resetAfterRender;
    private Object owner;
    public int renderRefCount;
    private static final int INITIAL_SKIP_VALUE = Integer.MAX_VALUE;
    private int skipped = Integer.MAX_VALUE;
    public final Object lock = "ModelInstance Thread Lock";
    public ModelScript modelScript;
    public String attachmentNameSelf;
    public String attachmentNameParent;
    public float scale = 1.0f;
    public String maskVariableValue;
    private static final org.joml.Matrix4f gcAttachmentMatrix = new org.joml.Matrix4f();
    private static final org.joml.Matrix4f gcTransposedAttachmentMatrix = new org.joml.Matrix4f();
    private static final Matrix4f gcBoneModelTransform = new Matrix4f();
    private static final org.joml.Vector3f gcVector3f = new org.joml.Vector3f();
    private static final AxisAngle4f gcAxisAngle4f = new AxisAngle4f();
    private static final Vector3 modelSpaceForward = new Vector3(0.0f, 0.0f, 1.0f);
    private static final Vector3 gcRotatedForward = new Vector3();
    public PlayerData[] playerData;
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final ColorInfo tempColorInfo2 = new ColorInfo();

    public ModelInstance init(Model model, IsoGameCharacter character, AnimationPlayer player) {
        this.data = (SkinningData)model.tag;
        this.model = model;
        this.tex = model.tex;
        if (!model.isStatic && player == null) {
            player = AnimationPlayer.alloc(model);
        }
        this.animPlayer = player;
        this.character = character;
        this.object = character;
        return this;
    }

    public boolean isRendering() {
        return this.renderRefCount > 0;
    }

    public void reset() {
        if (this.tex instanceof SmartTexture) {
            Texture tex = this.tex;
            RenderThread.queueInvokeOnRenderContext(tex::destroy);
        }
        this.animPlayer = null;
        this.character = null;
        this.data = null;
        this.hue = 0.0f;
        this.itemVisual = null;
        this.matrixModel = null;
        this.model = null;
        this.object = null;
        this.parent = null;
        this.parentBone = 0;
        this.parentBoneName = null;
        this.skipped = Integer.MAX_VALUE;
        this.sub.clear();
        this.softwareMesh = null;
        this.tex = null;
        if (this.textureInitializer != null) {
            this.textureInitializer.release();
            this.textureInitializer = null;
        }
        this.tintR = 1.0f;
        this.tintG = 1.0f;
        this.tintB = 1.0f;
        this.resetAfterRender = false;
        this.renderRefCount = 0;
        this.scale = 1.0f;
        this.owner = null;
        this.modelScript = null;
        this.attachmentNameSelf = null;
        this.attachmentNameParent = null;
        this.maskVariableValue = null;
        if (this.playerData != null) {
            PlayerData.pool.release((PlayerData[])this.playerData);
            Arrays.fill(this.playerData, null);
        }
    }

    public void LoadTexture(String name) {
        if (name == null || name.isEmpty()) {
            this.tex = null;
            return;
        }
        this.tex = Texture.getSharedTexture("media/textures/" + name + ".png");
        if (this.tex == null) {
            if (name.equals("Vest_White")) {
                this.tex = Texture.getSharedTexture("media/textures/Shirt_White.png");
            } else if (name.contains("Hair")) {
                this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
            } else if (name.contains("Beard")) {
                this.tex = Texture.getSharedTexture("media/textures/F_Hair_White.png");
            } else {
                DebugLog.log("ERROR: model texture \"" + name + "\" wasn't found");
            }
        }
    }

    public void dismember(int bone) {
        this.animPlayer.dismember(bone);
    }

    public void UpdateDir() {
        if (this.animPlayer == null) {
            return;
        }
        this.animPlayer.updateForwardDirection(this.character);
    }

    public void Update(float deltaT) {
        if (this.character != null) {
            float distToPlayer = this.character.DistTo(IsoPlayer.getInstance());
            if (!this.character.amputations.isEmpty() && distToPlayer > 0.0f && this.animPlayer != null) {
                this.animPlayer.dismembered.clear();
                ArrayList<String> amputations = this.character.amputations;
                for (int i = 0; i < amputations.size(); ++i) {
                    String amputation = amputations.get(i);
                    this.animPlayer.dismember(this.animPlayer.getSkinningData().boneIndices.get(amputation));
                }
            }
        }
        this.instanceSkip = 0;
        if (this.animPlayer != null) {
            if (this.matrixModel == null) {
                if (this.skipped >= this.instanceSkip) {
                    if (this.skipped == Integer.MAX_VALUE) {
                        this.skipped = 1;
                    }
                    float time = deltaT * (float)this.skipped;
                    this.animPlayer.Update(time);
                } else {
                    this.animPlayer.DoAngles(deltaT);
                }
                this.animPlayer.parentPlayer = null;
            } else {
                this.animPlayer.parentPlayer = this.matrixModel.animPlayer;
            }
        }
        if (this.skipped >= this.instanceSkip) {
            this.skipped = 0;
        }
        ++this.skipped;
    }

    public void SetForceDir(Vector2 dir) {
        if (this.animPlayer != null) {
            this.animPlayer.setTargetAndCurrentDirection(dir);
        }
    }

    public void setInstanceSkip(int c) {
        this.instanceSkip = c;
        for (int i = 0; i < this.sub.size(); ++i) {
            ModelInstance modelInstance = this.sub.get(i);
            modelInstance.instanceSkip = c;
        }
    }

    public void destroySmartTextures() {
        if (this.tex instanceof SmartTexture) {
            this.tex.destroy();
            this.tex = null;
        }
        for (int i = 0; i < this.sub.size(); ++i) {
            ModelInstance inst = this.sub.get(i);
            inst.destroySmartTextures();
        }
    }

    public void updateLights() {
        boolean bNeverDone;
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.playerData == null) {
            this.playerData = new PlayerData[4];
        }
        boolean bl = bNeverDone = this.playerData[playerIndex] == null;
        if (this.playerData[playerIndex] == null) {
            this.playerData[playerIndex] = PlayerData.pool.alloc();
        }
        this.playerData[playerIndex].updateLights(this.character, bNeverDone);
    }

    public ItemVisual getItemVisual() {
        return this.itemVisual;
    }

    public void setItemVisual(ItemVisual itemVisual) {
        this.itemVisual = itemVisual;
    }

    public void applyModelScriptScale(String modelName) {
        this.modelScript = ScriptManager.instance.getModelScript(modelName);
        if (this.modelScript != null) {
            this.scale = this.modelScript.scale;
        }
    }

    public ModelAttachment getAttachment(int index) {
        return this.modelScript == null ? null : this.modelScript.getAttachment(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            return null;
        }
        return this.modelScript == null ? null : this.modelScript.getAttachmentById(id);
    }

    public org.joml.Matrix4f getAttachmentMatrix(ModelAttachment attachment, org.joml.Matrix4f out) {
        out.translation(attachment.getOffset());
        org.joml.Vector3f rot = attachment.getRotate();
        out.rotateXYZ(rot.x * ((float)Math.PI / 180), rot.y * ((float)Math.PI / 180), rot.z * ((float)Math.PI / 180));
        return out;
    }

    public org.joml.Matrix4f getAttachmentMatrix(int index, org.joml.Matrix4f out) {
        ModelAttachment attachment = this.getAttachment(index);
        if (attachment == null) {
            return out.identity();
        }
        return this.getAttachmentMatrix(attachment, out);
    }

    public org.joml.Matrix4f getAttachmentMatrixById(String id, org.joml.Matrix4f out) {
        ModelAttachment attachment = this.getAttachmentById(id);
        if (attachment == null) {
            return out.identity();
        }
        return this.getAttachmentMatrix(attachment, out);
    }

    public void setOwner(Object owner) {
        Objects.requireNonNull(owner);
        assert (this.owner == null);
        this.owner = owner;
    }

    public void clearOwner(Object expectedOwner) {
        Objects.requireNonNull(expectedOwner);
        assert (this.owner == expectedOwner);
        this.owner = null;
    }

    public Object getOwner() {
        return this.owner;
    }

    public void setTextureInitializer(ModelInstanceTextureInitializer textureInitializer) {
        this.textureInitializer = textureInitializer;
    }

    public ModelInstanceTextureInitializer getTextureInitializer() {
        return this.textureInitializer;
    }

    public boolean hasTextureCreator() {
        return this.textureInitializer != null && this.textureInitializer.isDirty();
    }

    public void getAttachmentWorldPosition(ModelAttachment attachment, Vector3 worldPosition, Vector3 worldDirectionUnnormalized) {
        float angle = this.animPlayer.getRenderedAngle();
        this.getAttachmentWorldPosition(attachment, angle, worldPosition, worldDirectionUnnormalized);
    }

    public void getAttachmentWorldPosition(ModelAttachment attachment, float yawAngle, Vector3 worldPosition, Vector3 worldDirectionUnnormalized) {
        this.animPlayer.getBoneModelTransform(this.parentBone, gcBoneModelTransform);
        this.getAttachmentMatrix(attachment, gcAttachmentMatrix);
        PZMath.convertMatrix(gcBoneModelTransform, gcTransposedAttachmentMatrix);
        gcTransposedAttachmentMatrix.transpose();
        gcTransposedAttachmentMatrix.mul(gcAttachmentMatrix, gcAttachmentMatrix);
        gcAttachmentMatrix.getTranslation(gcVector3f);
        worldPosition.x = ModelInstance.gcVector3f.x;
        worldPosition.y = ModelInstance.gcVector3f.y;
        worldPosition.z = ModelInstance.gcVector3f.z;
        Model.vectorToWorldCoords(this.character.getX(), this.character.getY(), this.character.getZ(), yawAngle, worldPosition);
        gcAttachmentMatrix.getRotation(gcAxisAngle4f);
        this.rotateVectorByAxisAngle();
        worldDirectionUnnormalized.x = ModelInstance.gcRotatedForward.x;
        worldDirectionUnnormalized.y = ModelInstance.gcRotatedForward.y;
        worldDirectionUnnormalized.z = ModelInstance.gcRotatedForward.z;
        Model.vectorToWorldCoords(0.0f, 0.0f, 0.0f, yawAngle, worldDirectionUnnormalized);
    }

    private void rotateVectorByAxisAngle() {
        float ux = ModelInstance.gcAxisAngle4f.x;
        float uy = ModelInstance.gcAxisAngle4f.y;
        float uz = ModelInstance.gcAxisAngle4f.z;
        float angle = ModelInstance.gcAxisAngle4f.angle;
        float cosTheta = org.joml.Math.cos(angle);
        float sinTheta = org.joml.Math.sin(angle);
        float x = ModelInstance.modelSpaceForward.x * (cosTheta + ux * ux * (1.0f - cosTheta)) + ModelInstance.modelSpaceForward.y * (ux * uy * (1.0f - cosTheta) - uz * sinTheta) + ModelInstance.modelSpaceForward.z * (ux * uz * (1.0f - cosTheta) + uy * sinTheta);
        float y = ModelInstance.modelSpaceForward.x * (uy * ux * (1.0f - cosTheta) + uz * sinTheta) + ModelInstance.modelSpaceForward.y * (cosTheta + uy * uy * (1.0f - cosTheta)) + ModelInstance.modelSpaceForward.z * (uy * uz * (1.0f - cosTheta) - ux * sinTheta);
        float z = ModelInstance.modelSpaceForward.x * (uz * ux * (1.0f - cosTheta) - uy * sinTheta) + ModelInstance.modelSpaceForward.y * (uz * uy * (1.0f - cosTheta) + ux * sinTheta) + ModelInstance.modelSpaceForward.z * (cosTheta + uz * uz * (1.0f - cosTheta));
        gcRotatedForward.set(x, y, z);
    }

    public static final class PlayerData {
        private FrameLightInfo[] frameLights;
        private ArrayList<IsoGridSquare.ResultLight> chosenLights;
        private org.joml.Vector3f targetAmbient;
        public org.joml.Vector3f currentAmbient;
        public EffectLight[] effectLightsMain;
        private static final ObjectPool<PlayerData> pool = new ObjectPool<PlayerData>(PlayerData::new);

        private void registerFrameLight(IsoGridSquare.ResultLight light) {
            this.chosenLights.add(light);
        }

        private void initFrameLightsForFrame() {
            if (this.frameLights == null) {
                this.effectLightsMain = new EffectLight[5];
                for (int i = 0; i < 5; ++i) {
                    this.effectLightsMain[i] = new EffectLight();
                }
                this.frameLights = new FrameLightInfo[5];
                this.chosenLights = new ArrayList();
                this.targetAmbient = new org.joml.Vector3f();
                this.currentAmbient = new org.joml.Vector3f();
            }
            for (EffectLight effectLight : this.effectLightsMain) {
                effectLight.radius = -1;
            }
            this.chosenLights.clear();
        }

        private void completeFrameLightsForFrame() {
            for (int x = 0; x < 5; ++x) {
                if (this.frameLights[x] == null) continue;
                this.frameLights[x].foundThisFrame = false;
            }
            block1: for (int i = 0; i < this.chosenLights.size(); ++i) {
                int x;
                IsoGridSquare.ResultLight chosenLight = this.chosenLights.get(i);
                boolean found = false;
                int foundIndex = 0;
                for (x = 0; x < 5; ++x) {
                    if (this.frameLights[x] == null || !this.frameLights[x].active || (chosenLight.id != -1 ? chosenLight.id != this.frameLights[x].id : this.frameLights[x].x != chosenLight.x || this.frameLights[x].y != chosenLight.y || this.frameLights[x].z != chosenLight.z)) continue;
                    found = true;
                    foundIndex = x;
                    break;
                }
                if (found) {
                    this.frameLights[foundIndex].foundThisFrame = true;
                    this.frameLights[foundIndex].x = chosenLight.x;
                    this.frameLights[foundIndex].y = chosenLight.y;
                    this.frameLights[foundIndex].z = chosenLight.z;
                    this.frameLights[foundIndex].flags = chosenLight.flags;
                    this.frameLights[foundIndex].radius = chosenLight.radius;
                    this.frameLights[foundIndex].targetColor.x = chosenLight.r;
                    this.frameLights[foundIndex].targetColor.y = chosenLight.g;
                    this.frameLights[foundIndex].targetColor.z = chosenLight.b;
                    this.frameLights[foundIndex].stage = FrameLightBlendStatus.In;
                    continue;
                }
                for (x = 0; x < 5; ++x) {
                    if (this.frameLights[x] != null && this.frameLights[x].active) continue;
                    if (this.frameLights[x] == null) {
                        this.frameLights[x] = new FrameLightInfo();
                    }
                    this.frameLights[x].x = chosenLight.x;
                    this.frameLights[x].y = chosenLight.y;
                    this.frameLights[x].z = chosenLight.z;
                    this.frameLights[x].r = chosenLight.r;
                    this.frameLights[x].g = chosenLight.g;
                    this.frameLights[x].b = chosenLight.b;
                    this.frameLights[x].flags = chosenLight.flags;
                    this.frameLights[x].radius = chosenLight.radius;
                    this.frameLights[x].id = chosenLight.id;
                    this.frameLights[x].currentColor.x = 0.0f;
                    this.frameLights[x].currentColor.y = 0.0f;
                    this.frameLights[x].currentColor.z = 0.0f;
                    this.frameLights[x].targetColor.x = chosenLight.r;
                    this.frameLights[x].targetColor.y = chosenLight.g;
                    this.frameLights[x].targetColor.z = chosenLight.b;
                    this.frameLights[x].stage = FrameLightBlendStatus.In;
                    this.frameLights[x].active = true;
                    this.frameLights[x].foundThisFrame = true;
                    continue block1;
                }
            }
            float multiplier = GameTime.getInstance().getMultiplier();
            for (int x = 0; x < 5; ++x) {
                FrameLightInfo frameLight = this.frameLights[x];
                if (frameLight == null || !frameLight.active) continue;
                if (!frameLight.foundThisFrame) {
                    frameLight.targetColor.x = 0.0f;
                    frameLight.targetColor.y = 0.0f;
                    frameLight.targetColor.z = 0.0f;
                    frameLight.stage = FrameLightBlendStatus.Out;
                }
                frameLight.currentColor.x = this.step(frameLight.currentColor.x, frameLight.targetColor.x, Math.signum(frameLight.targetColor.x - frameLight.currentColor.x) / (60.0f * multiplier));
                frameLight.currentColor.y = this.step(frameLight.currentColor.y, frameLight.targetColor.y, Math.signum(frameLight.targetColor.y - frameLight.currentColor.y) / (60.0f * multiplier));
                frameLight.currentColor.z = this.step(frameLight.currentColor.z, frameLight.targetColor.z, Math.signum(frameLight.targetColor.z - frameLight.currentColor.z) / (60.0f * multiplier));
                if (frameLight.stage != FrameLightBlendStatus.Out || !(frameLight.currentColor.x < 0.01f) || !(frameLight.currentColor.y < 0.01f) || !(frameLight.currentColor.z < 0.01f)) continue;
                frameLight.active = false;
            }
        }

        private void sortLights(IsoGameCharacter character) {
            for (int i = 0; i < this.frameLights.length; ++i) {
                FrameLightInfo fl = this.frameLights[i];
                if (fl == null) continue;
                fl.distSq = !fl.active ? Float.MAX_VALUE : IsoUtils.DistanceToSquared(character.getX(), character.getY(), character.getZ(), (float)fl.x + 0.5f, (float)fl.y + 0.5f, fl.z);
            }
            Arrays.sort(this.frameLights, (o1, o2) -> {
                boolean null2;
                boolean null1 = o1 == null || o1.radius == -1 || !o1.active;
                boolean bl = null2 = o2 == null || o2.radius == -1 || !o2.active;
                if (null1 && null2) {
                    return 0;
                }
                if (null1) {
                    return 1;
                }
                if (null2) {
                    return -1;
                }
                if (o1.stage.ordinal() < o2.stage.ordinal()) {
                    return -1;
                }
                if (o1.stage.ordinal() > o2.stage.ordinal()) {
                    return 1;
                }
                return (int)Math.signum(o1.distSq - o2.distSq);
            });
        }

        private void updateLights(IsoGameCharacter character, boolean bNeverDone) {
            IsoGridSquare above;
            int i;
            this.initFrameLightsForFrame();
            if (character == null) {
                return;
            }
            if (character.getCurrentSquare() == null) {
                return;
            }
            IsoGridSquare.ILighting lighting = character.getCurrentSquare().lighting[IsoCamera.frameState.playerIndex];
            int lightCount = org.joml.Math.min(lighting.resultLightCount(), 4);
            for (i = 0; i < lightCount; ++i) {
                IsoGridSquare.ResultLight light = lighting.getResultLight(i);
                this.registerFrameLight(light);
            }
            if (bNeverDone) {
                for (i = 0; i < this.frameLights.length; ++i) {
                    if (this.frameLights[i] == null) continue;
                    this.frameLights[i].active = false;
                }
            }
            this.completeFrameLightsForFrame();
            character.getCurrentSquare().interpolateLight(tempColorInfo, character.getX() % 1.0f, character.getY() % 1.0f);
            this.targetAmbient.x = ModelInstance.tempColorInfo.r;
            this.targetAmbient.y = ModelInstance.tempColorInfo.g;
            this.targetAmbient.z = ModelInstance.tempColorInfo.b;
            if (character.getZ() - (float)PZMath.fastfloor(character.getZ()) > 0.2f && (above = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(character.getX()), PZMath.fastfloor(character.getY()), PZMath.fastfloor(character.getZ()) + 1)) != null) {
                ColorInfo colorAbove = tempColorInfo2;
                above.lighting[IsoCamera.frameState.playerIndex].lightInfo();
                above.interpolateLight(colorAbove, character.getX() % 1.0f, character.getY() % 1.0f);
                tempColorInfo.interp(colorAbove, (character.getZ() - ((float)PZMath.fastfloor(character.getZ()) + 0.2f)) / 0.8f, tempColorInfo);
                this.targetAmbient.set(ModelInstance.tempColorInfo.r, ModelInstance.tempColorInfo.g, ModelInstance.tempColorInfo.b);
            }
            float multiplier = GameTime.getInstance().getMultiplier();
            this.currentAmbient.x = this.step(this.currentAmbient.x, this.targetAmbient.x, (this.targetAmbient.x - this.currentAmbient.x) / (10.0f * multiplier));
            this.currentAmbient.y = this.step(this.currentAmbient.y, this.targetAmbient.y, (this.targetAmbient.y - this.currentAmbient.y) / (10.0f * multiplier));
            this.currentAmbient.z = this.step(this.currentAmbient.z, this.targetAmbient.z, (this.targetAmbient.z - this.currentAmbient.z) / (10.0f * multiplier));
            if (bNeverDone) {
                this.setCurrentToTarget();
            }
            this.sortLights(character);
            float lightMul = 0.7f;
            for (int i2 = 0; i2 < 5; ++i2) {
                IsoRoom room;
                FrameLightInfo light = this.frameLights[i2];
                if (light == null || !light.active) continue;
                EffectLight effectLight = this.effectLightsMain[i2];
                int lightFlags = light.flags;
                if ((lightFlags & 1) != 0 && ((room = character.getCurrentSquare().getRoom()) == null || room.findRoomLightByID(light.id) == null)) {
                    lightFlags &= 0xFFFFFFFE;
                }
                if ((lightFlags & 1) != 0) {
                    effectLight.set(character.getX(), character.getY(), PZMath.fastfloor(character.getZ()) + 1, light.currentColor.x * 0.7f, light.currentColor.y * 0.7f, light.currentColor.z * 0.7f, light.radius);
                    continue;
                }
                if ((light.flags & 2) != 0) {
                    if (character instanceof IsoPlayer) {
                        IsoPlayer isoPlayer = (IsoPlayer)character;
                        int maxLightsPerPlayer = 4;
                        int id = GameClient.client ? isoPlayer.onlineId + 1 : isoPlayer.playerIndex + 1;
                        int playerIndex = isoPlayer.playerIndex;
                        int minID = playerIndex * 4 + 1;
                        int maxID = playerIndex * 4 + 4 - 1 + 1;
                        if (light.id >= minID && light.id <= maxID) continue;
                        effectLight.set(light.x, light.y, light.z, light.currentColor.x, light.currentColor.y, light.currentColor.z, light.radius);
                        continue;
                    }
                    effectLight.set(light.x, light.y, light.z, light.currentColor.x * 2.0f, light.currentColor.y, light.currentColor.z, light.radius);
                    continue;
                }
                effectLight.set((float)light.x + 0.5f, (float)light.y + 0.5f, (float)light.z + 0.5f, light.currentColor.x * 0.7f, light.currentColor.y * 0.7f, light.currentColor.z * 0.7f, light.radius);
            }
            if (lightCount <= 3 && character instanceof IsoPlayer && character.getTorchStrength() > 0.0f) {
                this.effectLightsMain[2].set(character.getX() + character.getForwardDirectionX() * 0.5f, character.getY() + character.getForwardDirectionY() * 0.5f, character.getZ() + 0.25f, 1.0f, 1.0f, 1.0f, 2);
            }
            float modelLightFrom = 0.0f;
            float modelLightTo = 1.0f;
            float ambr = this.lerp(0.0f, 1.0f, this.currentAmbient.x);
            float ambg = this.lerp(0.0f, 1.0f, this.currentAmbient.y);
            float ambb = this.lerp(0.0f, 1.0f, this.currentAmbient.z);
            if (character.getCurrentSquare().isOutside()) {
                this.effectLightsMain[3].set(character.getX() - 2.0f, character.getY() - 2.0f, character.getZ() + 1.0f, (ambr *= 1.7f) / 4.0f, (ambg *= 1.7f) / 4.0f, (ambb *= 1.7f) / 4.0f, 5000);
                this.effectLightsMain[4].set(character.getX() + 2.0f, character.getY() + 2.0f, character.getZ() + 1.0f, ambr / 4.0f, ambg / 4.0f, ambb / 4.0f, 5000);
            } else if (character.getCurrentSquare().getRoom() != null) {
                this.effectLightsMain[3].set(character.getX() - 2.0f, character.getY() - 2.0f, character.getZ() + 1.0f, (ambr *= 1.7f) / 4.0f, (ambg *= 1.7f) / 4.0f, (ambb *= 1.7f) / 4.0f, 5000);
                this.effectLightsMain[4].set(character.getX() + 2.0f, character.getY() + 2.0f, character.getZ() + 1.0f, ambr / 4.0f, ambg / 4.0f, ambb / 4.0f, 5000);
            }
        }

        private float lerp(float f, float t, float d) {
            return f + (t - f) * d;
        }

        private void setCurrentToTarget() {
            for (int i = 0; i < this.frameLights.length; ++i) {
                FrameLightInfo frameLight = this.frameLights[i];
                if (frameLight == null) continue;
                frameLight.currentColor.set(frameLight.targetColor);
            }
            this.currentAmbient.set(this.targetAmbient);
        }

        private float step(float current, float target, float add) {
            if (current < target) {
                return ClimateManager.clamp(0.0f, target, current + add);
            }
            if (current > target) {
                return ClimateManager.clamp(target, 1.0f, current + add);
            }
            return current;
        }
    }

    public static final class FrameLightInfo {
        public FrameLightBlendStatus stage;
        public int id;
        public int x;
        public int y;
        public int z;
        public float distSq;
        public int radius;
        public float r;
        public float g;
        public float b;
        public int flags;
        public final Vector3f currentColor = new Vector3f();
        public final Vector3f targetColor = new Vector3f();
        public boolean active;
        public boolean foundThisFrame;
    }

    public static enum FrameLightBlendStatus {
        In,
        During,
        Out;

    }

    public static final class EffectLight {
        public float x;
        public float y;
        public float z;
        public float r;
        public float g;
        public float b;
        public int radius;

        public void set(float x, float y, float z, float r, float g, float b, int radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.r = r;
            this.g = g;
            this.b = b;
            this.radius = radius;
        }

        public void clear() {
            this.set(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0);
        }

        public void set(IsoLightSource ls) {
            this.x = (float)ls.x + 0.5f;
            this.y = (float)ls.y + 0.5f;
            this.z = ls.z;
            this.r = ls.r;
            this.g = ls.g;
            this.b = ls.b;
            this.radius = ls.radius;
        }
    }
}

