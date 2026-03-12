/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import org.joml.Matrix4f;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.opengl.PZGLUtil;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.debug.DebugOptions;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.vehicles.BaseVehicle;

public final class ModelInstanceDebugRenderData
extends PooledObject {
    private static final Pool<ModelInstanceDebugRenderData> s_pool = new Pool<ModelInstanceDebugRenderData>(ModelInstanceDebugRenderData::new);
    private final ArrayList<Matrix4f> attachmentMatrices = new ArrayList();

    public static ModelInstanceDebugRenderData alloc() {
        return s_pool.alloc();
    }

    public ModelInstanceDebugRenderData init(ModelSlotRenderData slotData, ModelInstanceRenderData instData) {
        this.initAttachments(slotData, instData);
        return this;
    }

    public void render() {
        this.renderAttachments();
        if (DebugOptions.instance.model.render.axis.getValue()) {
            Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
        }
    }

    private void initAttachments(ModelSlotRenderData slotData, ModelInstanceRenderData instData) {
        BaseVehicle.Matrix4fObjectPool pool = BaseVehicle.TL_matrix4f_pool.get();
        pool.release(this.attachmentMatrices);
        this.attachmentMatrices.clear();
        if (!DebugOptions.instance.model.render.attachments.getValue()) {
            return;
        }
        ModelScript modelScript = instData.modelInstance.modelScript;
        if (modelScript == null) {
            return;
        }
        Matrix4f modelMatrix = ((Matrix4f)pool.alloc()).set(instData.xfrm);
        IsoGameCharacter isoGameCharacter = slotData.character;
        if (isoGameCharacter instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)isoGameCharacter;
            modelMatrix.scale(isoAnimal.getAnimalSize());
        }
        Matrix4f boneMatrix = (Matrix4f)pool.alloc();
        modelMatrix.transpose();
        for (int i = 0; i < modelScript.getAttachmentCount(); ++i) {
            ModelAttachment attachment = modelScript.getAttachment(i);
            Matrix4f attachmentMatrix = (Matrix4f)pool.alloc();
            instData.modelInstance.getAttachmentMatrix(attachment, attachmentMatrix);
            if (instData.model.isStatic || attachment.getBone() == null) {
                modelMatrix.mul(attachmentMatrix, attachmentMatrix);
            } else if (slotData.animPlayer != null && slotData.animPlayer.hasSkinningData()) {
                int boneIndex = slotData.animPlayer.getSkinningBoneIndex(attachment.getBone(), 0);
                org.lwjgl.util.vector.Matrix4f boneMatrix1 = slotData.animPlayer.getModelTransformAt(boneIndex);
                PZMath.convertMatrix(boneMatrix1, boneMatrix);
                boneMatrix.transpose();
                boneMatrix.mul(attachmentMatrix, attachmentMatrix);
                modelMatrix.mul(attachmentMatrix, attachmentMatrix);
            }
            this.attachmentMatrices.add(attachmentMatrix);
        }
        pool.release(boneMatrix);
        pool.release(modelMatrix);
    }

    private void renderAttachments() {
        for (int i = 0; i < this.attachmentMatrices.size(); ++i) {
            Matrix4f attachmentMatrix = this.attachmentMatrices.get(i);
            PZGLUtil.pushAndMultMatrix(5888, attachmentMatrix);
            Model.debugDrawAxis(0.0f, 0.0f, 0.0f, 0.05f, 1.0f);
            PZGLUtil.popMatrix(5888);
        }
    }
}

