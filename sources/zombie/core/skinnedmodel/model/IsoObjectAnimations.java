/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoObject;
import zombie.iso.SpriteModel;
import zombie.iso.objects.IsoDoor;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class IsoObjectAnimations {
    private static IsoObjectAnimations instance;
    final ObjectPool<AnimatedObject> animatedObjectPool = new ObjectPool<AnimatedObject>(AnimatedObject::new);
    final ArrayList<AnimatedObject> animatedObjects = new ArrayList();
    final ArrayList<MatrixPaletteForFrame> matrixPalettes = new ArrayList();
    final ArrayList<IsoObject> dancingDoors = new ArrayList();
    float dancingDoorsTimer;
    boolean dancingDoorsOpen;

    public static IsoObjectAnimations getInstance() {
        if (instance == null) {
            instance = new IsoObjectAnimations();
        }
        return instance;
    }

    public void addObject(IsoObject object, SpriteModel spriteModel, String animationName) {
        if (GameServer.server) {
            return;
        }
        AnimatedObject animatedObject = this.getAnimatedObject(object);
        if (animatedObject == null) {
            animatedObject = this.animatedObjectPool.alloc();
            this.animatedObjects.add(animatedObject);
        }
        animatedObject.object = object;
        animatedObject.spriteModel = spriteModel;
        animatedObject.animationName = animationName;
        animatedObject.modelScript = ScriptManager.instance.getModelScript(spriteModel.getModelScriptName());
        animatedObject.initModel();
        animatedObject.initAnimationPlayer();
    }

    public void update() {
        if (GameServer.server) {
            return;
        }
        this.updateDancingDoors();
        for (int i = 0; i < this.animatedObjects.size(); ++i) {
            AnimatedObject animatedObject = this.animatedObjects.get(i);
            if (animatedObject.object.getObjectIndex() == -1) {
                this.animatedObjects.remove(i--);
                this.animatedObjectPool.release(animatedObject);
                continue;
            }
            boolean bRemove = animatedObject.update();
            if (!bRemove) continue;
            animatedObject.object.onAnimationFinished();
            animatedObject.object.invalidateRenderChunkLevel(256L);
            this.animatedObjects.remove(i--);
            this.animatedObjectPool.release(animatedObject);
        }
    }

    private AnimatedObject getAnimatedObject(IsoObject object) {
        for (int i = 0; i < this.animatedObjects.size(); ++i) {
            AnimatedObject animatedObject = this.animatedObjects.get(i);
            if (animatedObject.object != object) continue;
            return animatedObject;
        }
        return null;
    }

    public AnimationPlayer getAnimationPlayer(IsoObject object) {
        AnimatedObject animatedObject = this.getAnimatedObject(object);
        if (animatedObject == null) {
            return null;
        }
        return animatedObject.animationPlayer;
    }

    public org.joml.Matrix4f getAttachmentTransform(IsoObject object, String attachmentName, org.joml.Matrix4f xfrm) {
        xfrm.identity();
        AnimatedObject animatedObject = this.getAnimatedObject(object);
        if (animatedObject == null) {
            SpriteModel spriteModel = object.getSpriteModel();
            ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.getModelScriptName());
            if (modelScript == null) {
                return xfrm;
            }
            ModelAttachment attachment = modelScript.getAttachmentById(attachmentName);
            if (attachment == null) {
                return xfrm;
            }
            ModelInstanceRenderData.makeAttachmentTransform(attachment, xfrm);
            Model model = ModelManager.instance.getLoadedModel(modelScript.getMeshName());
            if (model == null || !model.isReady()) {
                return xfrm;
            }
            SkinningData skinningData = (SkinningData)model.tag;
            int parentBone = skinningData.boneIndices.get(attachment.getBone());
            if (parentBone == -1) {
                return xfrm;
            }
            MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(model, spriteModel.animationName, spriteModel.animationTime);
            int position = mpff.matrixPalette.position();
            mpff.matrixPalette.position(parentBone * 4 * 4);
            org.joml.Matrix4f boneXfrm = BaseVehicle.allocMatrix4f().set(mpff.matrixPalette);
            mpff.matrixPalette.position(position);
            boneXfrm.transpose();
            boneXfrm.mul(xfrm, xfrm);
            BaseVehicle.releaseMatrix4f(boneXfrm);
            return xfrm;
        }
        ModelAttachment attachment = animatedObject.modelScript.getAttachmentById(attachmentName);
        if (attachment == null) {
            return xfrm;
        }
        ModelInstanceRenderData.makeAttachmentTransform(attachment, xfrm);
        org.joml.Matrix4f boneXfrm = BaseVehicle.allocMatrix4f();
        ModelInstanceRenderData.makeBoneTransform2(animatedObject.animationPlayer, attachment.getBone(), boneXfrm);
        boneXfrm.mul(xfrm, xfrm);
        BaseVehicle.releaseMatrix4f(boneXfrm);
        return xfrm;
    }

    private MatrixPaletteForFrame getOrCreateMatrixPaletteForFrame(Model model, String animation, float time) {
        MatrixPaletteForFrame mpff = null;
        for (int i = 0; i < this.matrixPalettes.size(); ++i) {
            MatrixPaletteForFrame mpff2 = this.matrixPalettes.get(i);
            if (mpff2.model != model || !mpff2.animation.equalsIgnoreCase(animation) || mpff2.time != time) continue;
            mpff = mpff2;
            break;
        }
        if (mpff == null) {
            mpff = new MatrixPaletteForFrame();
            mpff.model = model;
            mpff.animation = animation;
            mpff.time = time;
            mpff.init();
            this.matrixPalettes.add(mpff);
        }
        if (mpff.matrixPalette == null || mpff.modificationCount != model.mesh.modificationCount) {
            mpff.init();
        }
        return mpff;
    }

    public FloatBuffer getMatrixPaletteForFrame(Model model, String animation, float time) {
        MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(model, animation, time);
        return mpff.matrixPalette;
    }

    public TFloatArrayList getBonesForFrame(Model model, String animation, float time) {
        MatrixPaletteForFrame mpff = this.getOrCreateMatrixPaletteForFrame(model, animation, time);
        return mpff.boneCoords;
    }

    public void addDancingDoor(IsoObject object) {
        if (GameServer.server) {
            return;
        }
        if (!DebugOptions.instance.animation.dancingDoors.getValue()) {
            return;
        }
        this.dancingDoors.add(object);
    }

    public void removeDancingDoor(IsoObject object) {
        if (GameServer.server) {
            return;
        }
        if (!DebugOptions.instance.animation.dancingDoors.getValue()) {
            return;
        }
        this.dancingDoors.remove(object);
    }

    private void updateDancingDoors() {
        if (GameServer.server) {
            return;
        }
        if (!DebugOptions.instance.animation.dancingDoors.getValue()) {
            return;
        }
        this.dancingDoorsTimer += GameTime.getInstance().getRealworldSecondsSinceLastUpdate();
        if (this.dancingDoorsTimer < 2.0f) {
            return;
        }
        while (this.dancingDoorsTimer >= 2.0f) {
            this.dancingDoorsTimer -= 2.0f;
        }
        this.dancingDoorsOpen = !this.dancingDoorsOpen;
        for (int i = 0; i < this.dancingDoors.size(); ++i) {
            IsoObject object = this.dancingDoors.get(i);
            if (object.getObjectIndex() == -1) {
                this.dancingDoors.remove(i--);
                continue;
            }
            IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
            if (door == null || door.IsOpen() == this.dancingDoorsOpen) continue;
            door.ToggleDoorActual(IsoPlayer.getInstance());
        }
    }

    static final class AnimatedObject {
        IsoObject object;
        SpriteModel spriteModel;
        String animationName;
        ModelScript modelScript;
        Model model;
        AnimationPlayer animationPlayer;
        AnimationTrack track;

        AnimatedObject() {
        }

        void initModel() {
            AnimationsMesh animationsMesh;
            String meshName = this.modelScript.getMeshName();
            String texName = this.modelScript.getTextureName();
            String shaderName = this.modelScript.getShaderName();
            boolean bStatic = this.modelScript.isStatic;
            this.model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
            if (this.model == null && !bStatic && this.modelScript.animationsMesh != null && (animationsMesh = ScriptManager.instance.getAnimationsMesh(this.modelScript.animationsMesh)) != null && animationsMesh.modelMesh != null) {
                this.model = ModelManager.instance.loadModel(meshName, texName, animationsMesh.modelMesh, shaderName);
            }
            if (this.model == null) {
                ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                this.model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
            }
        }

        void initAnimationPlayer() {
            if (this.track != null) {
                this.animationPlayer.getMultiTrack().removeTrack(this.track);
                this.track = null;
            }
            if (this.animationPlayer != null && this.animationPlayer.getModel() != this.model) {
                this.animationPlayer.release();
                this.animationPlayer = null;
            }
            if (this.animationPlayer == null) {
                this.animationPlayer = AnimationPlayer.alloc(this.model);
            }
            this.track = this.animationPlayer.play(this.animationName, false);
            if (this.track != null) {
                this.track.setBlendWeight(1.0f);
                this.track.setSpeedDelta(1.5f);
                this.track.isPlaying = true;
                this.track.reverse = false;
            }
        }

        boolean update() {
            if (this.animationPlayer == null) {
                return true;
            }
            this.animationPlayer.Update(GameTime.isGamePaused() || !GameWindow.isIngameState() ? 0.0f : GameTime.instance.getTimeDelta());
            if (this.track == null) {
                return true;
            }
            return this.track.isFinished();
        }
    }

    static final class MatrixPaletteForFrame {
        static AnimationPlayer animationPlayer;
        Model model;
        int modificationCount;
        String animation;
        float time;
        FloatBuffer matrixPalette;
        final TFloatArrayList boneCoords = new TFloatArrayList();

        MatrixPaletteForFrame() {
        }

        void init() {
            if (animationPlayer != null) {
                while (animationPlayer.getMultiTrack().getTrackCount() > 0) {
                    animationPlayer.getMultiTrack().removeTrackAt(0);
                }
            }
            if (animationPlayer != null && animationPlayer.getModel() != this.model) {
                animationPlayer.release();
                animationPlayer = null;
            }
            if (animationPlayer == null) {
                animationPlayer = AnimationPlayer.alloc(this.model);
            }
            if (!animationPlayer.isReady()) {
                return;
            }
            this.modificationCount = this.model.mesh.modificationCount;
            AnimationTrack track = animationPlayer.play(this.animation, false);
            if (track == null) {
                return;
            }
            float duration = track.getDuration();
            track.setCurrentTimeValue(this.time * duration);
            track.setBlendWeight(1.0f);
            track.setSpeedDelta(1.0f);
            track.isPlaying = false;
            track.reverse = false;
            animationPlayer.Update(100.0f);
            this.initMatrixPalette();
            this.initSkeleton(animationPlayer);
        }

        private void initMatrixPalette() {
            SkinningData skinningData = (SkinningData)this.model.tag;
            if (skinningData == null) {
                DebugLog.General.warn("skinningData is null, matrixPalette may be invalid");
                return;
            }
            Matrix4f[] skinTransforms = animationPlayer.getSkinTransforms(skinningData);
            int matrixFloats = 16;
            if (this.matrixPalette == null || this.matrixPalette.capacity() < skinTransforms.length * 16) {
                this.matrixPalette = BufferUtils.createFloatBuffer(skinTransforms.length * 16);
            }
            this.matrixPalette.clear();
            for (int i = 0; i < skinTransforms.length; ++i) {
                skinTransforms[i].store(this.matrixPalette);
            }
            this.matrixPalette.flip();
        }

        void initSkeleton(AnimationPlayer animPlayer) {
            this.boneCoords.resetQuick();
            if (animPlayer == null || !animPlayer.hasSkinningData() || animPlayer.isBoneTransformsNeedFirstFrame()) {
                return;
            }
            Integer translationBoneIndex = animPlayer.getSkinningData().boneIndices.get("Translation_Data");
            for (int i = 0; i < animPlayer.getModelTransformsCount(); ++i) {
                int parentIdx;
                if (translationBoneIndex != null && i == translationBoneIndex || (parentIdx = animPlayer.getSkinningData().skeletonHierarchy.get(i).intValue()) < 0) continue;
                this.initSkeleton(animPlayer, i);
                this.initSkeleton(animPlayer, parentIdx);
            }
        }

        void initSkeleton(AnimationPlayer animPlayer, int boneIndex) {
            Matrix4f boneTransform = animPlayer.getModelTransformAt(boneIndex);
            float x = boneTransform.m03;
            float y = boneTransform.m13;
            float z = boneTransform.m23;
            this.boneCoords.add(x);
            this.boneCoords.add(y);
            this.boneCoords.add(z);
        }
    }
}

