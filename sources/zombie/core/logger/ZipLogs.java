/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.network.MD5Checksum;

public final class ZipLogs {
    static ArrayList<String> filePaths = new ArrayList();

    public static void addZipFile(boolean saveTheSaveData) {
        FileSystem zipfs = null;
        try {
            String zipname = ZomboidFileSystem.instance.getCacheDir() + File.separator + "logs.zip";
            String zipnamePath = new File(zipname).toURI().toString();
            URI zipDisk = URI.create("jar:" + zipnamePath);
            Path zipLocation = FileSystems.getDefault().getPath(zipname, new String[0]).toAbsolutePath();
            HashMap<String, String> env = new HashMap<String, String>();
            env.put("create", String.valueOf(Files.notExists(zipLocation, new LinkOption[0])));
            try {
                zipfs = FileSystems.newFileSystem(zipDisk, env);
            }
            catch (ZipException ze) {
                ze.printStackTrace();
                DebugLog.log("Deleting possibly-corrupt " + zipname);
                try {
                    Files.deleteIfExists(zipLocation);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            catch (IOException e) {
                e.printStackTrace();
                return;
            }
            long consoleHash = ZipLogs.getMD5FromZip(zipfs, "/meta/console.txt.md5");
            long consoleHashCoop = ZipLogs.getMD5FromZip(zipfs, "/meta/coop-console.txt.md5");
            long consoleHashServer = ZipLogs.getMD5FromZip(zipfs, "/meta/server-console.txt.md5");
            long consoleHashDebugLog = ZipLogs.getMD5FromZip(zipfs, "/meta/DebugLog.txt.md5");
            ZipLogs.addLogToZip(zipfs, "console", "console.txt", consoleHash);
            ZipLogs.addLogToZip(zipfs, "coop-console", "coop-console.txt", consoleHashCoop);
            ZipLogs.addLogToZip(zipfs, "server-console", "server-console.txt", consoleHashServer);
            ZipLogs.addDebugLogToZip(zipfs, "debug-log", "DebugLog.txt", consoleHashDebugLog);
            ZipLogs.addToZip(zipfs, "/configs/options.ini", "options.ini");
            ZipLogs.addToZip(zipfs, "/configs/popman-options.ini", "popman-options.ini");
            ZipLogs.addToZip(zipfs, "/configs/latestSave.ini", "latestSave.ini");
            ZipLogs.addToZip(zipfs, "/configs/debug-options.ini", "debug-options.ini");
            ZipLogs.addToZip(zipfs, "/configs/sounds.ini", "sounds.ini");
            ZipLogs.addToZip(zipfs, "/addition/translationProblems.txt", "translationProblems.txt");
            ZipLogs.addToZip(zipfs, "/addition/gamepadBinding.config", "gamepadBinding.config");
            ZipLogs.addFilelistToZip(zipfs, "/addition/mods.txt", "mods");
            ZipLogs.addDirToZip(zipfs, "/statistic", "Statistic");
            if (!saveTheSaveData) {
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_t.bin", "map_t.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_ver.bin", "map_ver.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map.bin", "map.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_sand.bin", "map_sand.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/reanimated.bin", "reanimated.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/zombies.ini", "zombies.ini");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/z_outfits.bin", "z_outfits.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_p.bin", "map_p.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_meta.bin", "map_meta.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/map_zone.bin", "map_zone.bin");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/serverid.dat", "serverid.dat");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/thumb.png", "thumb.png");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/players.db", "players.db");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/players.db-journal", "players.db-journal");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/vehicles.db", "vehicles.db");
                ZipLogs.addSaveOldToZip(zipfs, "/save_old/vehicles.db-journal", "vehicles.db-journal");
                ZipLogs.putTextFile(zipfs, "/save_old/description.txt", ZipLogs.getLastSaveDescription());
            } else {
                ZipLogs.addSaveToZip(zipfs, "/save/map_t.bin", "map_t.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map_ver.bin", "map_ver.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map.bin", "map.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map_sand.bin", "map_sand.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/reanimated.bin", "reanimated.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/zombies.ini", "zombies.ini");
                ZipLogs.addSaveToZip(zipfs, "/save/z_outfits.bin", "z_outfits.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map_p.bin", "map_p.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map_meta.bin", "map_meta.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/map_zone.bin", "map_zone.bin");
                ZipLogs.addSaveToZip(zipfs, "/save/serverid.dat", "serverid.dat");
                ZipLogs.addSaveToZip(zipfs, "/save/thumb.png", "thumb.png");
                ZipLogs.addSaveToZip(zipfs, "/save/players.db", "players.db");
                ZipLogs.addSaveToZip(zipfs, "/save/players.db-journal", "players.db-journal");
                ZipLogs.addSaveToZip(zipfs, "/save/vehicles.db", "vehicles.db");
                ZipLogs.addSaveToZip(zipfs, "/save/vehicles.db-journal", "vehicles.db-journal");
                ZipLogs.putTextFile(zipfs, "/save/description.txt", ZipLogs.getCurrentSaveDescription());
            }
            try {
                zipfs.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            if (zipfs != null) {
                try {
                    zipfs.close();
                }
                catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            e.printStackTrace();
        }
    }

    private static void copyToZip(Path root, Path contents, Path file) throws IOException {
        Path to = root.resolve(contents.relativize(file).toString());
        if (Files.isDirectory(file, new LinkOption[0])) {
            Files.createDirectories(to, new FileAttribute[0]);
        } else {
            Files.copy(file, to, new CopyOption[0]);
        }
    }

    public static void addToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            Files.createDirectories(internalTargetPath.getParent(), new FileAttribute[0]);
            Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename, new String[0]).toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath, new LinkOption[0])) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addSaveToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            Files.createDirectories(internalTargetPath.getParent(), new FileAttribute[0]);
            Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getFileNameInCurrentSave(filename), new String[0]).toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath, new LinkOption[0])) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addSaveOldToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini")));
            }
            catch (FileNotFoundException e) {
                return;
            }
            String world = br.readLine();
            String gameMode = br.readLine();
            br.close();
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            Files.createDirectories(internalTargetPath.getParent(), new FileAttribute[0]);
            Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getSaveDir() + File.separator + gameMode + File.separator + world + File.separator + filename, new String[0]).toAbsolutePath();
            Files.deleteIfExists(internalTargetPath);
            if (Files.exists(filePath, new LinkOption[0])) {
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getLastSaveDescription() {
        try {
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "latestSave.ini")));
            }
            catch (FileNotFoundException e) {
                return "-";
            }
            String world = br.readLine();
            String gameMode = br.readLine();
            br.close();
            return "World: " + world + "\n\rGameMode:" + gameMode;
        }
        catch (IOException e) {
            e.printStackTrace();
            return "-";
        }
    }

    private static String getCurrentSaveDescription() {
        String gameMode = "Sandbox";
        if (Core.gameMode != null) {
            gameMode = Core.gameMode;
        }
        String world = "-";
        if (Core.gameSaveWorld != null) {
            world = Core.gameSaveWorld;
        }
        return "World: " + world + "\n\rGameMode:" + gameMode;
    }

    public static void addDirToZip(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            ZipLogs.deleteDirectory(zipfs, internalTargetPath);
            Files.createDirectories(internalTargetPath, new FileAttribute[0]);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename, new String[0]).toAbsolutePath();
            Stream<Path> files = Files.walk(dirPath, new FileVisitOption[0]);
            files.forEach(file -> {
                try {
                    ZipLogs.copyToZip(internalTargetPath, dirPath, file);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void addDirToZipLua(FileSystem zipfs, String filenameInternal, String filename) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            ZipLogs.deleteDirectory(zipfs, internalTargetPath);
            Files.createDirectories(internalTargetPath, new FileAttribute[0]);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename, new String[0]).toAbsolutePath();
            Stream<Path> files = Files.walk(dirPath, new FileVisitOption[0]);
            files.forEach(file -> {
                try {
                    if (!file.endsWith("ServerList.txt") && !file.endsWith("ServerListSteam.txt")) {
                        ZipLogs.copyToZip(internalTargetPath, dirPath, file);
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void addFilelistToZip(FileSystem zipfs, String filenameInternal, String dirname) {
        try {
            Path internalTargetPath = zipfs.getPath(filenameInternal, new String[0]);
            Path dirPath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + dirname, new String[0]).toAbsolutePath();
            Stream<Path> files = Files.list(dirPath);
            String filelist = files.map(Path::getFileName).map(Path::toString).collect(Collectors.joining("; "));
            Files.deleteIfExists(internalTargetPath);
            Files.write(internalTargetPath, filelist.getBytes(), new OpenOption[0]);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    static void deleteDirectory(FileSystem zipfs, Path dir) {
        filePaths.clear();
        ZipLogs.getDirectoryFiles(dir);
        for (String s : filePaths) {
            try {
                Files.delete(zipfs.getPath(s, new String[0]));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void getDirectoryFiles(Path dir) {
        try {
            Stream<Path> files = Files.walk(dir, new FileVisitOption[0]);
            files.forEach(file -> {
                if (!file.toString().equals(dir.toString())) {
                    if (Files.isDirectory(file, new LinkOption[0])) {
                        ZipLogs.getDirectoryFiles(file);
                    } else if (!filePaths.contains(file.toString())) {
                        filePaths.add(file.toString());
                    }
                }
            });
            filePaths.add(dir.toString());
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static void addLogToZip(FileSystem zipfs, String dirnameInternal, String filename, long hashFromZip) {
        long md5;
        try {
            md5 = MD5Checksum.createChecksum(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename);
        }
        catch (Exception e) {
            md5 = 0L;
        }
        File consoleFileCoop = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename);
        if (consoleFileCoop.exists() && !consoleFileCoop.isDirectory() && md5 != hashFromZip) {
            try {
                Path pathInZipfile = zipfs.getPath("/" + dirnameInternal + "/log_5.txt", new String[0]);
                Files.delete(pathInZipfile);
            }
            catch (Exception pathInZipfile) {
                // empty catch block
            }
            for (int i = 5; i > 0; --i) {
                Path sourceURI = zipfs.getPath("/" + dirnameInternal + "/log_" + i + ".txt", new String[0]);
                Path destinationURI = zipfs.getPath("/" + dirnameInternal + "/log_" + (i + 1) + ".txt", new String[0]);
                try {
                    Files.move(sourceURI, destinationURI, new CopyOption[0]);
                    continue;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            try {
                Path internalTargetPath = zipfs.getPath("/" + dirnameInternal + "/log_1.txt", new String[0]);
                Files.createDirectories(internalTargetPath.getParent(), new FileAttribute[0]);
                Path filePath = FileSystems.getDefault().getPath(ZomboidFileSystem.instance.getCacheDir() + File.separator + filename, new String[0]).toAbsolutePath();
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
                Path pathInZipfile = zipfs.getPath("/meta/" + filename + ".md5", new String[0]);
                Files.createDirectories(pathInZipfile.getParent(), new FileAttribute[0]);
                try {
                    Files.delete(pathInZipfile);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                Files.write(pathInZipfile, String.valueOf(md5).getBytes(), new OpenOption[0]);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void addDebugLogToZip(FileSystem zipfs, String dirnameInternal, String filename, long hashFromZip) {
        long md5;
        String filepath = null;
        File logsDir = new File(LoggerManager.getLogsDir());
        String[] logFileNameList = logsDir.list();
        for (int i = 0; i < logFileNameList.length; ++i) {
            String logFileName = logFileNameList[i];
            if (!logFileName.contains("DebugLog.txt")) continue;
            filepath = LoggerManager.getLogsDir() + File.separator + logFileName;
            break;
        }
        if (filepath == null) {
            return;
        }
        try {
            md5 = MD5Checksum.createChecksum(filepath);
        }
        catch (Exception e) {
            md5 = 0L;
        }
        File consoleFileCoop = new File(filepath);
        if (consoleFileCoop.exists() && !consoleFileCoop.isDirectory() && md5 != hashFromZip) {
            try {
                Path pathInZipfile = zipfs.getPath("/" + dirnameInternal + "/log_5.txt", new String[0]);
                Files.delete(pathInZipfile);
            }
            catch (Exception pathInZipfile) {
                // empty catch block
            }
            for (int i = 5; i > 0; --i) {
                Path sourceURI = zipfs.getPath("/" + dirnameInternal + "/log_" + i + ".txt", new String[0]);
                Path destinationURI = zipfs.getPath("/" + dirnameInternal + "/log_" + (i + 1) + ".txt", new String[0]);
                try {
                    Files.move(sourceURI, destinationURI, new CopyOption[0]);
                    continue;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            try {
                Path internalTargetPath = zipfs.getPath("/" + dirnameInternal + "/log_1.txt", new String[0]);
                Files.createDirectories(internalTargetPath.getParent(), new FileAttribute[0]);
                Path filePath = FileSystems.getDefault().getPath(filepath, new String[0]).toAbsolutePath();
                Files.copy(filePath, internalTargetPath, StandardCopyOption.REPLACE_EXISTING);
                Path pathInZipfile = zipfs.getPath("/meta/" + filename + ".md5", new String[0]);
                Files.createDirectories(pathInZipfile.getParent(), new FileAttribute[0]);
                try {
                    Files.delete(pathInZipfile);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                Files.write(pathInZipfile, String.valueOf(md5).getBytes(), new OpenOption[0]);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static long getMD5FromZip(FileSystem zipfs, String filenameInternal) {
        long md5 = 0L;
        try {
            Path pathInZipfile = zipfs.getPath(filenameInternal, new String[0]);
            if (Files.exists(pathInZipfile, new LinkOption[0])) {
                List<String> lines = Files.readAllLines(pathInZipfile);
                md5 = Long.parseLong(lines.get(0));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return md5;
    }

    public static void putTextFile(FileSystem zipfs, String filename, String text) {
        try {
            Path pathInZipfile = zipfs.getPath(filename, new String[0]);
            Files.createDirectories(pathInZipfile.getParent(), new FileAttribute[0]);
            try {
                Files.delete(pathInZipfile);
            }
            catch (Exception exception) {
                // empty catch block
            }
            Files.write(pathInZipfile, text.getBytes(), new OpenOption[0]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

