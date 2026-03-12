/*
 * Decompiled with CFR 0.152.
 */
package zombie.world.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.world.logger.Log;

public class WorldDictionaryLogger {
    private static final ArrayList<Log.BaseLog> _logItems = new ArrayList();

    public static void reset() {
        _logItems.clear();
    }

    public static void startLogging() {
        WorldDictionaryLogger.reset();
    }

    public static void log(Log.BaseLog log) {
        if (GameClient.client) {
            return;
        }
        _logItems.add(log);
    }

    public static void log(String msg) {
        WorldDictionaryLogger.log(msg, true);
    }

    public static void log(String msg, boolean debugPrint) {
        if (GameClient.client) {
            return;
        }
        if (debugPrint) {
            DebugLog.log("WorldDictionary: " + msg);
        }
        _logItems.add(new Log.Comment(msg));
    }

    public static void saveLog(String saveFile) throws IOException {
        Log.BaseLog log;
        if (GameClient.client) {
            return;
        }
        boolean changesToSave = false;
        for (int i = 0; i < _logItems.size(); ++i) {
            log = _logItems.get(i);
            if (log.isIgnoreSaveCheck()) continue;
            changesToSave = true;
            break;
        }
        if (!changesToSave) {
            return;
        }
        File path = new File(ZomboidFileSystem.instance.getCurrentSaveDir() + File.separator);
        if (path.exists() && path.isDirectory()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave(saveFile);
            File f = new File(fileName);
            try (FileWriter w = new FileWriter(f, true);){
                w.write("log = log or {};" + System.lineSeparator());
                w.write("table.insert(log, {" + System.lineSeparator());
                for (int i = 0; i < _logItems.size(); ++i) {
                    log = _logItems.get(i);
                    log.saveAsText(w, "\t");
                }
                w.write("};" + System.lineSeparator());
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw new IOException("Error saving WorldDictionary log.");
            }
        }
    }
}

