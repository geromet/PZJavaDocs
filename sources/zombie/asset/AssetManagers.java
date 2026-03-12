/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import gnu.trove.map.hash.TLongObjectHashMap;
import zombie.asset.AssetManager;
import zombie.asset.AssetType;
import zombie.fileSystem.FileSystem;

public final class AssetManagers {
    private final AssetManagerTable managers = new AssetManagerTable();
    private final FileSystem fileSystem;

    public AssetManagers(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public AssetManager get(AssetType type) {
        return (AssetManager)this.managers.get(type.type);
    }

    public void add(AssetType type, AssetManager rm) {
        this.managers.put(type.type, rm);
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public static final class AssetManagerTable
    extends TLongObjectHashMap<AssetManager> {
    }
}

