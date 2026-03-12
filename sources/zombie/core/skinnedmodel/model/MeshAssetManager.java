/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.util.HashSet;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.core.skinnedmodel.model.FileTask_LoadMesh;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelTxt;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.fileSystem.FileSystem;
import zombie.util.StringUtils;

public final class MeshAssetManager
extends AssetManager {
    public static final MeshAssetManager instance = new MeshAssetManager();
    private final HashSet<String> watchedFiles = new HashSet();
    private final PredicatedFileWatcher watcher = new PredicatedFileWatcher(MeshAssetManager::isWatched, MeshAssetManager::watchedFileChanged);

    private MeshAssetManager() {
        DebugFileWatcher.instance.add(this.watcher);
    }

    @Override
    protected void startLoading(Asset asset) {
        ModelMesh mesh = (ModelMesh)asset;
        FileSystem fileSystem = this.getOwner().getFileSystem();
        FileTask_LoadMesh fileTask = new FileTask_LoadMesh(mesh, fileSystem, result -> this.loadCallback(mesh, result));
        fileTask.setPriority(6);
        AssetTask_RunFileTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        ((AssetTask)assetTask).execute();
    }

    private void loadCallback(ModelMesh mesh, Object result) {
        if (result instanceof ProcessedAiScene) {
            ProcessedAiScene processedAiScene = (ProcessedAiScene)result;
            mesh.onLoadedX(processedAiScene);
            this.onLoadingSucceeded(mesh);
        } else if (result instanceof ModelTxt) {
            ModelTxt modelTxt = (ModelTxt)result;
            mesh.onLoadedTxt(modelTxt);
            this.onLoadingSucceeded(mesh);
        } else {
            DebugLog.General.warn("Failed to load asset: " + String.valueOf(mesh.getPath()));
            this.onLoadingFailed(mesh);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new ModelMesh(path, this, (ModelMesh.MeshAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private static boolean isWatched(String entryKey) {
        if (!(StringUtils.endsWithIgnoreCase(entryKey, ".fbx") || StringUtils.endsWithIgnoreCase(entryKey, ".glb") || StringUtils.endsWithIgnoreCase(entryKey, ".x"))) {
            return false;
        }
        String fullPath = ZomboidFileSystem.instance.getString(entryKey);
        return MeshAssetManager.instance.watchedFiles.contains(fullPath);
    }

    private static void watchedFileChanged(String entryKey) {
        DebugType.Asset.println("%s changed\n", entryKey);
        String fullPath = ZomboidFileSystem.instance.getString(entryKey);
        instance.getAssetTable().forEachValue(asset -> {
            ModelMesh meshAsset = (ModelMesh)asset;
            if (!meshAsset.isEmpty() && fullPath.equalsIgnoreCase(meshAsset.fullPath)) {
                ModelMesh.MeshAssetParams assetParams = new ModelMesh.MeshAssetParams();
                assetParams.animationsMesh = meshAsset.animationsMesh;
                assetParams.isStatic = meshAsset.isStatic;
                assetParams.postProcess = meshAsset.postProcess;
                instance.reload((Asset)asset, assetParams);
            }
            return true;
        });
    }

    public void addWatchedFile(String fileName) {
        this.watchedFiles.add(fileName);
    }
}

