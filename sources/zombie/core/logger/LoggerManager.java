/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.logger.ZLogger;
import zombie.debug.DebugLog;

public final class LoggerManager {
    private static boolean isInitialized;
    private static final HashMap<String, ZLogger> s_loggers;

    public static synchronized ZLogger getLogger(String loggerName) {
        if (!s_loggers.containsKey(loggerName)) {
            LoggerManager.createLogger(loggerName, false);
        }
        return s_loggers.get(loggerName);
    }

    public static synchronized void init() {
        if (isInitialized) {
            return;
        }
        DebugLog.General.debugln("Initializing...");
        isInitialized = true;
        LoggerManager.backupOldLogFiles();
    }

    private static void backupOldLogFiles() {
        try {
            File logsDir = new File(LoggerManager.getLogsDir());
            File[] logFileList = ZomboidFileSystem.listAllFiles(logsDir);
            if (logFileList.length == 0) {
                return;
            }
            Date lastModified = LoggerManager.getLogFileLastModifiedTime(logFileList[0]);
            String backupDirName = "logs_" + ZomboidFileSystem.getDateStampString(lastModified);
            File backupDir = new File(LoggerManager.getLogsDir() + File.separator + backupDirName);
            ZomboidFileSystem.ensureFolderExists(backupDir);
            for (int i = 0; i < logFileList.length; ++i) {
                File fileToMove = logFileList[i];
                if (!fileToMove.isFile()) continue;
                fileToMove.renameTo(new File(backupDir.getAbsolutePath() + File.separator + fileToMove.getName()));
                fileToMove.delete();
            }
        }
        catch (Exception e) {
            DebugLog.General.error("Exception thrown trying to initialize LoggerManager, trying to copy old log files.");
            DebugLog.General.error("Exception: ");
            DebugLog.General.error(e);
            e.printStackTrace();
        }
    }

    private static Date getLogFileLastModifiedTime(File logFile) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(logFile.lastModified());
        return calendar.getTime();
    }

    public static synchronized void createLogger(String loggerName, boolean useConsole) {
        LoggerManager.init();
        s_loggers.put(loggerName, new ZLogger(loggerName, useConsole));
    }

    public static String getLogsDir() {
        String logsDirPath = ZomboidFileSystem.instance.getCacheDirSub("Logs");
        ZomboidFileSystem.ensureFolderExists(logsDirPath);
        File logsDir = new File(logsDirPath);
        return logsDir.getAbsolutePath();
    }

    public static String getPlayerCoords(IsoGameCharacter player) {
        return "(" + player.getXi() + "," + player.getYi() + "," + player.getZi() + ")";
    }

    static {
        s_loggers = new HashMap();
    }
}

