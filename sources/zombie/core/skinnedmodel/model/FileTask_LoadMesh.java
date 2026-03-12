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
import zombie.core.skinnedmodel.model.FileTask_AbstractLoadModel;
import zombie.core.skinnedmodel.model.MeshAssetManager;
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

public class FileTask_LoadMesh
extends FileTask_AbstractLoadModel {
    ModelMesh mesh;

    public FileTask_LoadMesh(ModelMesh mesh, FileSystem fileSystem, IFileTaskCallback cb) {
        super(fileSystem, cb, "media/models", "media/models_x");
        this.mesh = mesh;
    }

    @Override
    public String getErrorMessage() {
        return this.fileName;
    }

    @Override
    public void done() {
        MeshAssetManager.instance.addWatchedFile(this.fileName);
        this.mesh.fullPath = this.fileName;
        this.fileName = null;
        this.mesh = null;
    }

    @Override
    public String getRawFileName() {
        String pathStr = this.mesh.getPath().getPath();
        int p = pathStr.indexOf(124);
        if (p != -1) {
            return pathStr.substring(0, p);
        }
        return pathStr;
    }

    private String getMeshName() {
        String pathStr = this.mesh.getPath().getPath();
        int p = pathStr.indexOf(124);
        if (p != -1) {
            return pathStr.substring(p + 1);
        }
        return null;
    }

    @Override
    public ProcessedAiScene loadX() throws IOException {
        EnumSet<AiPostProcessSteps[]> postProcessStepSet = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, new AiPostProcessSteps[]{AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES});
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = this.mesh.assetParams.isStatic ? JAssImpImporter.LoadMode.StaticMesh : JAssImpImporter.LoadMode.Normal;
        ModelMesh animationsMesh = this.mesh.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        params.meshName = this.getMeshName();
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadFBX() throws IOException {
        DebugType.Animation.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps[]> postProcessStepSet = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, new AiPostProcessSteps[]{AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES});
        this.handlePostProcessFlags(postProcessStepSet, this.mesh.assetParams.postProcess);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = this.mesh.assetParams.isStatic ? JAssImpImporter.LoadMode.StaticMesh : JAssImpImporter.LoadMode.Normal;
        ModelMesh animationsMesh = this.mesh.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        Quaternion rotateModifier = new Quaternion();
        float angle = 1.5707964f;
        Vector4f axisAngle = new Vector4f(1.0f, 0.0f, 0.0f, -1.5707964f);
        rotateModifier.setFromAxisAngle(axisAngle);
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        params.meshName = this.getMeshName();
        params.animBonesScaleModifier = 0.01f;
        params.animBonesRotateModifier = rotateModifier;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadGLTF() throws IOException {
        DebugType.LoadAnimation.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps[]> postProcessStepSet = EnumSet.of(AiPostProcessSteps.FIND_INSTANCES, new AiPostProcessSteps[]{AiPostProcessSteps.MAKE_LEFT_HANDED, AiPostProcessSteps.LIMIT_BONE_WEIGHTS, AiPostProcessSteps.TRIANGULATE, AiPostProcessSteps.OPTIMIZE_MESHES, AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS, AiPostProcessSteps.JOIN_IDENTICAL_VERTICES});
        this.handlePostProcessFlags(postProcessStepSet, this.mesh.assetParams.postProcess);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = this.mesh.assetParams.isStatic ? JAssImpImporter.LoadMode.StaticMesh : JAssImpImporter.LoadMode.Normal;
        ModelMesh animationsMesh = this.mesh.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = skinningData;
        params.meshName = this.getMeshName();
        params.animBonesScaleModifier = 1.0f;
        params.animBonesRotateModifier = null;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ModelTxt loadTxt() throws IOException {
        boolean bStatic = this.mesh.assetParams.isStatic;
        boolean bReverse = false;
        ModelMesh animationsMesh = this.mesh.assetParams.animationsMesh;
        SkinningData skinningData = animationsMesh == null ? null : animationsMesh.skinningData;
        return ModelLoader.instance.loadTxt(this.fileName, bStatic, false, skinningData);
    }

    private void handlePostProcessFlags(EnumSet<AiPostProcessSteps> postProcessStepSet, String postProcess) {
        String[] ss;
        if (postProcess == null) {
            return;
        }
        for (String flagStr : ss = this.mesh.assetParams.postProcess.split(";")) {
            if (flagStr.startsWith("+")) {
                postProcessStepSet.add(AiPostProcessSteps.valueOf(flagStr.substring(1)));
                continue;
            }
            if (!flagStr.startsWith("-")) continue;
            postProcessStepSet.remove((Object)AiPostProcessSteps.valueOf(flagStr.substring(1)));
        }
    }
}

