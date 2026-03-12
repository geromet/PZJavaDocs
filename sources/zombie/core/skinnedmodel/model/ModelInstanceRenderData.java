/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.ModelInstanceDebugRenderData;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.model.SoftwareModelMeshInstance;
import zombie.core.skinnedmodel.model.VehicleModelInstance;
import zombie.core.skinnedmodel.model.VehicleSubModelInstance;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class ModelInstanceRenderData
extends AnimatedModel.AnimatedModelInstanceRenderData {
    public static boolean invertAttachmentSelfTransform;
    private static final ObjectPool<ModelInstanceRenderData> pool;
    public float depthBias;
    public float hue;
    public float tintR;
    public float tintG;
    public float tintB;
    public int parentBone;
    public SoftwareModelMeshInstance softwareMesh;
    protected ModelInstanceDebugRenderData debugRenderData;

    @Override
    public ModelInstanceRenderData init() {
        super.init();
        assert (this.modelInstance.character == null || this.modelInstance.animPlayer != null);
        if (this.modelInstance.getTextureInitializer() != null) {
            this.modelInstance.getTextureInitializer().renderMain();
        }
        return this;
    }

    @Override
    public void initModel(ModelInstance modelInstance, AnimatedModel.AnimatedModelInstanceRenderData parent) {
        super.initModel(modelInstance, parent);
        this.model = modelInstance.model;
        this.tex = modelInstance.tex;
        this.depthBias = modelInstance.depthBias;
        this.hue = modelInstance.hue;
        this.parentBone = modelInstance.parentBone;
        this.softwareMesh = modelInstance.softwareMesh;
        ++modelInstance.renderRefCount;
        VehicleSubModelInstance vehicleSubModelInstance = Type.tryCastTo(modelInstance, VehicleSubModelInstance.class);
        if (modelInstance instanceof VehicleModelInstance || vehicleSubModelInstance != null) {
            if (modelInstance instanceof VehicleModelInstance) {
                this.xfrm.set(((BaseVehicle)modelInstance.object).renderTransform);
            } else {
                this.xfrm.set(vehicleSubModelInstance.modelInfo.renderTransform);
            }
            ModelInstanceRenderData.postMultiplyMeshTransform(this.xfrm, modelInstance.model.mesh);
        }
    }

    @Override
    public void UpdateCharacter(Shader shader) {
        super.UpdateCharacter(shader);
        if (!PerformanceSettings.fboRenderChunk) {
            this.properties.SetFloat("targetDepth", 0.5f);
        } else if (this.modelInstance.parent != null) {
            this.properties.SetFloat("targetDepth", this.modelInstance.parent.targetDepth);
        }
        this.properties.SetFloat("DepthBias", this.depthBias / 50.0f);
        this.properties.SetFloat("HueShift", this.hue);
        this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
    }

    public void renderDebug() {
        if (this.debugRenderData != null) {
            this.debugRenderData.render();
        }
    }

    public void RenderCharacter(ModelSlotRenderData slotData) {
        this.tintR = this.modelInstance.tintR;
        this.tintG = this.modelInstance.tintG;
        this.tintB = this.modelInstance.tintB;
        this.tex = this.modelInstance.tex;
        if (this.tex == null && this.modelInstance.model.tex == null) {
            return;
        }
        this.properties.SetVector3("TintColour", this.tintR, this.tintG, this.tintB);
        this.model.DrawChar(slotData, this);
    }

    public void RenderVehicle(ModelSlotRenderData slotData) {
        this.tintR = this.modelInstance.tintR;
        this.tintG = this.modelInstance.tintG;
        this.tintB = this.modelInstance.tintB;
        this.tex = this.modelInstance.tex;
        if (this.tex == null && this.modelInstance.model.tex == null) {
            return;
        }
        this.model.DrawVehicle(slotData, this);
    }

    public static org.joml.Matrix4f makeAttachmentTransform(ModelAttachment attachment, org.joml.Matrix4f attachmentXfrm) {
        attachmentXfrm.translation(attachment.getOffset());
        Vector3f rotate = attachment.getRotate();
        attachmentXfrm.rotateXYZ(rotate.x * ((float)Math.PI / 180), rotate.y * ((float)Math.PI / 180), rotate.z * ((float)Math.PI / 180));
        attachmentXfrm.scale(attachment.getScale());
        return attachmentXfrm;
    }

    public static void applyBoneTransform(ModelInstance parentInstance, String boneName, org.joml.Matrix4f transform) {
        if (parentInstance == null || parentInstance.animPlayer == null) {
            return;
        }
        org.joml.Matrix4f boneXfrm2 = (org.joml.Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
        ModelInstanceRenderData.makeBoneTransform(parentInstance.animPlayer, boneName, boneXfrm2);
        boneXfrm2.mul(transform, transform);
        BaseVehicle.TL_matrix4f_pool.get().release(boneXfrm2);
    }

    public static void makeBoneTransform(AnimationPlayer animationPlayer, String boneName, org.joml.Matrix4f transform) {
        transform.identity();
        if (animationPlayer == null) {
            return;
        }
        if (StringUtils.isNullOrWhitespace(boneName)) {
            return;
        }
        int parentBone = animationPlayer.getSkinningBoneIndex(boneName, -1);
        if (parentBone == -1) {
            return;
        }
        Matrix4f boneXfrm = animationPlayer.getModelTransformAt(parentBone);
        PZMath.convertMatrix(boneXfrm, transform);
        transform.transpose();
    }

    public static void makeBoneTransform2(AnimationPlayer animationPlayer, String boneName, org.joml.Matrix4f transform) {
        transform.identity();
        if (animationPlayer == null) {
            return;
        }
        if (StringUtils.isNullOrWhitespace(boneName)) {
            return;
        }
        int parentBone = animationPlayer.getSkinningBoneIndex(boneName, -1);
        if (parentBone == -1) {
            return;
        }
        Matrix4f[] boneXfrms = animationPlayer.getSkinTransforms(animationPlayer.getSkinningData());
        Matrix4f boneXfrm = boneXfrms[parentBone];
        PZMath.convertMatrix(boneXfrm, transform);
        transform.transpose();
    }

    public static org.joml.Matrix4f preMultiplyMeshTransform(org.joml.Matrix4f transform, ModelMesh mesh) {
        if (mesh != null && mesh.isReady() && mesh.transform != null) {
            org.joml.Matrix4f meshTransform = BaseVehicle.allocMatrix4f().set(mesh.transform);
            meshTransform.transpose();
            meshTransform.mul(transform, transform);
            BaseVehicle.releaseMatrix4f(meshTransform);
        }
        return transform;
    }

    public static org.joml.Matrix4f postMultiplyMeshTransform(org.joml.Matrix4f transform, ModelMesh mesh) {
        if (mesh != null && mesh.transform != null) {
            if (mesh.isReady()) {
                org.joml.Matrix4f meshTransform = BaseVehicle.allocMatrix4f().set(mesh.transform);
                meshTransform.transpose();
                transform.mul(meshTransform);
                BaseVehicle.releaseMatrix4f(meshTransform);
            } else {
                transform.scale(0.0f);
            }
        }
        return transform;
    }

    private void testOnBackItem(ModelInstance modelInstance) {
        if (modelInstance.parent == null || modelInstance.parent.modelScript == null) {
            return;
        }
        AnimationPlayer animPlayer = modelInstance.parent.animPlayer;
        ModelAttachment attachment = null;
        for (int i = 0; i < modelInstance.parent.modelScript.getAttachmentCount(); ++i) {
            ModelAttachment attachment2 = modelInstance.parent.getAttachment(i);
            if (attachment2.getBone() == null || this.parentBone != animPlayer.getSkinningBoneIndex(attachment2.getBone(), 0)) continue;
            attachment = attachment2;
            break;
        }
        if (attachment == null) {
            return;
        }
        org.joml.Matrix4f attachmentXfrm = (org.joml.Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
        ModelInstanceRenderData.makeAttachmentTransform(attachment, attachmentXfrm);
        this.xfrm.transpose();
        this.xfrm.mul(attachmentXfrm);
        this.xfrm.transpose();
        ModelAttachment attachment1 = modelInstance.getAttachmentById(attachment.getId());
        if (attachment1 != null) {
            ModelInstanceRenderData.makeAttachmentTransform(attachment1, attachmentXfrm);
            if (invertAttachmentSelfTransform) {
                attachmentXfrm.invert();
            }
            this.xfrm.transpose();
            this.xfrm.mul(attachmentXfrm);
            this.xfrm.transpose();
        }
        BaseVehicle.TL_matrix4f_pool.get().release(attachmentXfrm);
    }

    public static ModelInstanceRenderData alloc() {
        return pool.alloc();
    }

    public static synchronized void release(ArrayList<ModelInstanceRenderData> objs) {
        for (int i = 0; i < objs.size(); ++i) {
            ModelInstanceRenderData data = objs.get(i);
            ModelInstanceRenderData.release(data);
        }
    }

    public static synchronized boolean release(ModelInstanceRenderData data) {
        if (data.modelInstance.getTextureInitializer() != null) {
            data.modelInstance.getTextureInitializer().postRender();
        }
        boolean bInstanceReleased = ModelManager.instance.derefModelInstance(data.modelInstance);
        data.modelInstance = null;
        data.model = null;
        data.tex = null;
        data.softwareMesh = null;
        data.debugRenderData = Pool.tryRelease(data.debugRenderData);
        pool.release(data);
        return bInstanceReleased;
    }

    static {
        pool = new ObjectPool<ModelInstanceRenderData>(ModelInstanceRenderData::new);
    }
}

