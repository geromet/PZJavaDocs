/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiScene;
import org.lwjgl.util.vector.Quaternion;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;

public class ProcessedAiSceneParams {
    public AiScene scene;
    public JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.Normal;
    public SkinningData skinnedTo;
    public String meshName;
    public float animBonesScaleModifier = 1.0f;
    public Quaternion animBonesRotateModifier;
    public boolean allMeshes;

    ProcessedAiSceneParams() {
    }

    public static ProcessedAiSceneParams create() {
        return new ProcessedAiSceneParams();
    }

    protected void set(ProcessedAiSceneParams src) {
        this.scene = src.scene;
        this.mode = src.mode;
        this.skinnedTo = src.skinnedTo;
        this.meshName = src.meshName;
        this.animBonesScaleModifier = src.animBonesScaleModifier;
        this.animBonesRotateModifier = src.animBonesRotateModifier;
        this.allMeshes = src.allMeshes;
    }
}

