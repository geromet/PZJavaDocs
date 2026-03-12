/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiMesh;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiSceneParams;

public class ImportedSkeletonParams
extends ProcessedAiSceneParams {
    AiMesh mesh;

    ImportedSkeletonParams() {
    }

    public static ImportedSkeletonParams create(ProcessedAiSceneParams aiSceneParams, AiMesh mesh) {
        ImportedSkeletonParams params = new ImportedSkeletonParams();
        params.set(aiSceneParams);
        params.mesh = mesh;
        return params;
    }
}

