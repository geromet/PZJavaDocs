/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public final class ChunkMapFilenames {
    public static ChunkMapFilenames instance = new ChunkMapFilenames();
    public final ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap();
    public final ConcurrentHashMap<Long, Object> headerMap = new ConcurrentHashMap();
    private File dirFile;
    private String cacheDir;
    private final HashSet<Integer> wxFolders = new HashSet();

    public ChunkMapFilenames() {
        File[] directories;
        this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        for (File dir : directories = ZomboidFileSystem.listAllDirectories(this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map", file -> true, false)) {
            try {
                this.wxFolders.add(Integer.valueOf(dir.getName()));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void clear() {
        this.dirFile = null;
        this.cacheDir = null;
        this.map.clear();
        this.headerMap.clear();
        this.wxFolders.clear();
    }

    public File getFilename(int wx, int wy) {
        long key = (long)wx << 32 | (long)wy & 0xFFFFFFFFL;
        if (this.map.containsKey(key)) {
            return (File)this.map.get(key);
        }
        if (this.cacheDir == null) {
            this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        }
        if (this.wxFolders.add(wx)) {
            try {
                Files.createDirectories(Path.of(this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map" + File.separator + wx, new String[0]), new FileAttribute[0]);
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
        String filename = this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map" + File.separator + wx + File.separator + wy + ".bin";
        File f = new File(filename);
        this.map.put(key, f);
        return f;
    }

    public File getDir(String gameSaveWorld) {
        if (this.cacheDir == null) {
            this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        }
        if (this.dirFile == null) {
            this.dirFile = new File(this.cacheDir, "map" + File.separator + gameSaveWorld);
        }
        return this.dirFile;
    }

    public String getHeader(int wX, int wY) {
        long key = (long)wX << 32 | (long)wY & 0xFFFFFFFFL;
        if (this.headerMap.containsKey(key)) {
            return this.headerMap.get(key).toString();
        }
        String filename = wX + "_" + wY + ".lotheader";
        this.headerMap.put(key, filename);
        return filename;
    }
}

