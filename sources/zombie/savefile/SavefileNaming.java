/*
 * Decompiled with CFR 0.152.
 */
package zombie.savefile;

import java.io.File;
import zombie.ZomboidFileSystem;

public final class SavefileNaming {
    public static final String SUBDIR_APOP = "apop";
    public static final String SUBDIR_CHUNKDATA = "chunkdata";
    public static final String SUBDIR_MAP = "map";
    public static final String SUBDIR_ZPOP = "zpop";
    public static final String SUBDIR_METAGRID = "metagrid";

    public static void ensureSubdirectoriesExist(String savefileDir) {
        File file = new File(savefileDir);
        ZomboidFileSystem.ensureFolderExists(new File(file, SUBDIR_APOP));
        ZomboidFileSystem.ensureFolderExists(new File(file, SUBDIR_CHUNKDATA));
        ZomboidFileSystem.ensureFolderExists(new File(file, SUBDIR_MAP));
        ZomboidFileSystem.ensureFolderExists(new File(file, SUBDIR_ZPOP));
        ZomboidFileSystem.ensureFolderExists(new File(file, SUBDIR_METAGRID));
    }
}

