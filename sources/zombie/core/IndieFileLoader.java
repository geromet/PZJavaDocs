/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import zombie.ZomboidFileSystem;
import zombie.core.Core;

public class IndieFileLoader {
    public static InputStreamReader getStreamReader(String path) throws FileNotFoundException {
        return IndieFileLoader.getStreamReader(path, false);
    }

    public static InputStreamReader getStreamReader(String path, boolean bIgnoreJar) throws FileNotFoundException {
        InputStreamReader isr;
        InputStream is = null;
        if (is != null && !bIgnoreJar) {
            isr = new InputStreamReader(is);
        } else {
            try {
                FileInputStream fis = new FileInputStream(ZomboidFileSystem.instance.getString(path));
                isr = new InputStreamReader((InputStream)fis, "UTF-8");
            }
            catch (Exception ex) {
                FileInputStream fis = new FileInputStream(Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + path);
                isr = new InputStreamReader(fis);
            }
        }
        return isr;
    }
}

