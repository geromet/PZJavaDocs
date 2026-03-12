/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFileTaskCallback;
import zombie.worldMap.WorldMapBinary;
import zombie.worldMap.WorldMapData;

public final class FileTask_LoadWorldMapBinary
extends FileTask {
    WorldMapData worldMapData;
    String filename;

    public FileTask_LoadWorldMapBinary(WorldMapData worldMapData, String filename, FileSystem fileSystem, IFileTaskCallback cb) {
        super(fileSystem, cb);
        this.worldMapData = worldMapData;
        this.filename = filename;
    }

    @Override
    public String getErrorMessage() {
        return this.filename;
    }

    @Override
    public void done() {
        this.worldMapData = null;
        this.filename = null;
    }

    @Override
    public Object call() throws Exception {
        WorldMapBinary reader = new WorldMapBinary();
        return reader.read(this.filename, this.worldMapData) ? Boolean.TRUE : Boolean.FALSE;
    }
}

