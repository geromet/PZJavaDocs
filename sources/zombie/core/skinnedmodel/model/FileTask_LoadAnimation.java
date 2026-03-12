/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;
import java.io.IOException;
import java.util.EnumSet;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.FileTask_AbstractLoadModel;
import zombie.core.skinnedmodel.model.ModelLoader;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelTxt;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiSceneParams;
import zombie.debug.DebugType;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.IFileTaskCallback;

public class FileTask_LoadAnimation
extends FileTask_AbstractLoadModel {
    private final AnimationAsset anim;

    public FileTask_LoadAnimation(AnimationAsset anim, FileSystem fileSystem, IFileTaskCallback cb) {
        super(fileSystem, cb, "media/anims", "media/anims_x");
        this.anim = anim;
    }

    @Override
    public String getRawFileName() {
        return this.anim.getPath().getPath();
    }

    @Override
    public String getErrorMessage() {
        return this.fileName;
    }

    @Override
    public void done() {
    }

    @Override
    public ProcessedAiScene loadX() throws IOException {
        DebugType.LoadAnimation.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.AnimationOnly;
        ModelMesh animationsMesh = this.anim.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadFBX() throws IOException {
        DebugType.LoadAnimation.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.AnimationOnly;
        ModelMesh animationsMesh = this.anim.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        Quaternion rotateModifier = new Quaternion();
        float angle = 1.5707964f;
        Vector4f axisAngle = new Vector4f(1.0f, 0.0f, 0.0f, -1.5707964f);
        rotateModifier.setFromAxisAngle(axisAngle);
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        params.animBonesScaleModifier = 0.01f;
        params.animBonesRotateModifier = rotateModifier;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadGLTF() throws IOException {
        DebugType.LoadAnimation.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.AnimationOnly;
        ModelMesh animationsMesh = this.anim.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        Quaternion rotateModifier = new Quaternion();
        float angle = 1.5707964f;
        Vector4f axisAngle = new Vector4f(1.0f, 0.0f, 0.0f, -1.5707964f);
        rotateModifier.setFromAxisAngle(axisAngle);
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        params.animBonesScaleModifier = 0.01f;
        params.animBonesRotateModifier = rotateModifier;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ModelTxt loadTxt() throws IOException {
        boolean bStatic = false;
        boolean bReverse = false;
        ModelMesh animationsMesh = this.anim.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        return ModelLoader.instance.loadTxt(this.fileName, false, false, skinningData);
    }
}

