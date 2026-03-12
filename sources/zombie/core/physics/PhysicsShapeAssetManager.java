/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import java.util.HashSet;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_RunFileTask;
import zombie.core.physics.FileTask_LoadPhysicsShape;
import zombie.core.physics.PhysicsShape;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.fileSystem.FileSystem;
import zombie.util.StringUtils;

public final class PhysicsShapeAssetManager
extends AssetManager {
    public static final PhysicsShapeAssetManager instance = new PhysicsShapeAssetManager();
    private final HashSet<String> watchedFiles = new HashSet();
    private final PredicatedFileWatcher watcher = new PredicatedFileWatcher(PhysicsShapeAssetManager::isWatched, PhysicsShapeAssetManager::watchedFileChanged);

    private PhysicsShapeAssetManager() {
        DebugFileWatcher.instance.add(this.watcher);
    }

    @Override
    protected void startLoading(Asset asset) {
        PhysicsShape physicsShape = (PhysicsShape)asset;
        FileSystem fileSystem = this.getOwner().getFileSystem();
        FileTask_LoadPhysicsShape fileTask = new FileTask_LoadPhysicsShape(physicsShape, fileSystem, result -> this.loadCallback(physicsShape, result));
        fileTask.setPriority(6);
        AssetTask_RunFileTask assetTask = new AssetTask_RunFileTask(fileTask, asset);
        this.setTask(asset, assetTask);
        ((AssetTask)assetTask).execute();
    }

    private void loadCallback(PhysicsShape physicsShape, Object result) {
        if (result instanceof ProcessedAiScene) {
            ProcessedAiScene processedAiScene = (ProcessedAiScene)result;
            physicsShape.onLoadedX(processedAiScene);
            this.onLoadingSucceeded(physicsShape);
        } else {
            DebugLog.General.warn("Failed to load asset: " + String.valueOf(physicsShape.getPath()));
            this.onLoadingFailed(physicsShape);
        }
    }

    @Override
    protected Asset createAsset(AssetPath path, AssetManager.AssetParams params) {
        return new PhysicsShape(path, this, (PhysicsShape.PhysicsShapeAssetParams)params);
    }

    @Override
    protected void destroyAsset(Asset asset) {
    }

    private static boolean isWatched(String entryKey) {
        if (!(StringUtils.endsWithIgnoreCase(entryKey, ".fbx") || StringUtils.endsWithIgnoreCase(entryKey, ".glb") || StringUtils.endsWithIgnoreCase(entryKey, ".x"))) {
            return false;
        }
        String fullPath = ZomboidFileSystem.instance.getString(entryKey);
        return PhysicsShapeAssetManager.instance.watchedFiles.contains(fullPath);
    }

    private static void watchedFileChanged(String entryKey) {
        DebugType.Asset.println("%s changed\n", entryKey);
        String fullPath = ZomboidFileSystem.instance.getString(entryKey);
        instance.getAssetTable().forEachValue(asset -> {
            PhysicsShape physicsShape = (PhysicsShape)asset;
            if (!physicsShape.isEmpty() && fullPath.equalsIgnoreCase(physicsShape.fullPath)) {
                PhysicsShape.PhysicsShapeAssetParams assetParams = new PhysicsShape.PhysicsShapeAssetParams();
                assetParams.postProcess = physicsShape.postProcess;
                assetParams.allMeshes = physicsShape.allMeshes;
                instance.reload((Asset)asset, assetParams);
            }
            return true;
        });
    }

    public void addWatchedFile(String fileName) {
        this.watchedFiles.add(fileName);
    }
}

