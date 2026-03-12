/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import zombie.asset.Asset;
import zombie.asset.AssetManagers;
import zombie.asset.AssetPath;
import zombie.asset.AssetStateObserver;
import zombie.asset.AssetTask;
import zombie.asset.AssetTask_LoadFromFileAsync;
import zombie.asset.AssetType;
import zombie.debug.DebugType;
import zombie.fileSystem.IFile;

public abstract class AssetManager
implements AssetStateObserver {
    private final AssetTable assets = new AssetTable();
    private AssetManagers owner;
    private boolean isUnloadEnabled;

    public void create(AssetType type, AssetManagers owner) {
        owner.add(type, this);
        this.owner = owner;
    }

    public void destroy() {
        this.assets.forEachValue(asset -> {
            if (!asset.isEmpty()) {
                DebugType.Asset.println("Leaking asset %s", asset.getPath());
            }
            this.destroyAsset((Asset)asset);
            return true;
        });
    }

    public void removeUnreferenced() {
        if (!this.isUnloadEnabled) {
            return;
        }
        ArrayList toRemove = new ArrayList();
        this.assets.forEachValue(asset -> {
            if (asset.getRefCount() == 0) {
                toRemove.add(asset);
            }
            return true;
        });
        for (Asset asset2 : toRemove) {
            this.assets.remove(asset2.getPath());
            this.destroyAsset(asset2);
        }
    }

    public Asset load(AssetPath path) {
        return this.load(path, null);
    }

    public Asset load(AssetPath path, AssetParams params) {
        if (!path.isValid()) {
            return null;
        }
        Asset asset = this.get(path);
        if (asset == null) {
            asset = this.createAsset(path, params);
            this.assets.put(path.getPath(), asset);
        }
        if (asset.isEmpty() && asset.priv.desiredState == Asset.State.EMPTY) {
            this.doLoad(asset, params);
        }
        asset.addRef();
        return asset;
    }

    public void load(Asset asset) {
        if (asset.isEmpty() && asset.priv.desiredState == Asset.State.EMPTY) {
            this.doLoad(asset, null);
        }
        asset.addRef();
    }

    public void unload(AssetPath path) {
        Asset asset = this.get(path);
        if (asset != null) {
            this.unload(asset);
        }
    }

    public void unload(Asset asset) {
        int newRefCount = asset.rmRef();
        assert (newRefCount >= 0);
        if (newRefCount == 0 && this.isUnloadEnabled) {
            this.doUnload(asset);
        }
    }

    public void unloadWithoutDeref(Asset asset) {
        this.doUnload(asset);
    }

    public void onDataReloaded(Asset asset) {
        asset.priv.desiredState = Asset.State.READY;
        this.onLoadingSucceeded(asset);
    }

    public void reload(AssetPath path) {
        Asset asset = this.get(path);
        if (asset != null) {
            this.reload(asset);
        }
    }

    public void reload(Asset asset) {
        this.reload(asset, null);
    }

    public void reload(Asset asset, AssetParams params) {
        this.doUnload(asset);
        this.doLoad(asset, params);
    }

    public void enableUnload(boolean enable) {
        this.isUnloadEnabled = enable;
        if (!enable) {
            return;
        }
        this.assets.forEachValue(asset -> {
            if (asset.getRefCount() == 0) {
                this.doUnload((Asset)asset);
            }
            return true;
        });
    }

    private void doLoad(Asset asset, AssetParams params) {
        if (asset.priv.desiredState == Asset.State.READY) {
            return;
        }
        asset.priv.desiredState = Asset.State.READY;
        asset.setAssetParams(params);
        this.startLoading(asset);
    }

    private void doUnload(Asset asset) {
        if (asset.priv.task != null) {
            asset.priv.task.cancel();
            asset.priv.task = null;
        }
        asset.priv.desiredState = Asset.State.EMPTY;
        this.unloadData(asset);
        assert (asset.priv.emptyDepCount <= 1);
        asset.priv.emptyDepCount = 1;
        asset.priv.failedDepCount = 0;
        asset.priv.checkState();
    }

    @Override
    public void onStateChanged(Asset.State oldState, Asset.State newState, Asset asset) {
    }

    protected void startLoading(Asset asset) {
        if (asset.priv.task != null) {
            return;
        }
        asset.priv.task = new AssetTask_LoadFromFileAsync(asset, false);
        asset.priv.task.execute();
    }

    protected final void onLoadingSucceeded(Asset asset) {
        asset.priv.onLoadingSucceeded();
    }

    protected final void onLoadingFailed(Asset asset) {
        asset.priv.onLoadingFailed();
    }

    protected final void setTask(Asset asset, AssetTask task) {
        if (asset.priv.task != null) {
            if (task == null) {
                asset.priv.task = null;
            }
            return;
        }
        asset.priv.task = task;
    }

    protected boolean loadDataFromFile(Asset asset, IFile file) {
        throw new RuntimeException("not implemented");
    }

    protected void unloadData(Asset asset) {
    }

    public AssetTable getAssetTable() {
        return this.assets;
    }

    public AssetManagers getOwner() {
        return this.owner;
    }

    protected abstract Asset createAsset(AssetPath var1, AssetParams var2);

    protected abstract void destroyAsset(Asset var1);

    protected Asset get(AssetPath path) {
        return (Asset)this.assets.get(path.getPath());
    }

    public static final class AssetTable
    extends THashMap<String, Asset> {
    }

    public static class AssetParams {
    }
}

