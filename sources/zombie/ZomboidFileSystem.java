/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import zombie.DebugFileWatcher;
import zombie.FileGuidTable;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.PredicatedFileWatcher;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoWorld;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.modding.ActiveMods;
import zombie.modding.ActiveModsFile;
import zombie.network.CoopMaster;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;

public final class ZomboidFileSystem {
    public static final ZomboidFileSystem instance = new ZomboidFileSystem();
    private final ArrayList<String> loadList = new ArrayList();
    private final Map<String, String> modIdToDir = new HashMap<String, String>();
    private final Map<String, ChooseGameInfo.Mod> modDirToMod = new HashMap<String, ChooseGameInfo.Mod>();
    private ArrayList<String> modFolders;
    private ArrayList<String> modFoldersOrder;
    public final HashMap<String, String> activeFileMap = new HashMap();
    private final HashSet<String> allAbsolutePaths = new HashSet();
    public final PZFolder base = new PZFolder();
    private final PZFolder workdir = new PZFolder();
    private File localWorkdir;
    private final PZFolder anims = new PZFolder();
    private final PZFolder animsX = new PZFolder();
    private final PZFolder animSets = new PZFolder();
    private final PZFolder actiongroups = new PZFolder();
    private File cacheDir;
    private final ConcurrentHashMap<String, String> relativeMap = new ConcurrentHashMap();
    public final ThreadLocal<Boolean> ignoreActiveFileMap = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final ConcurrentHashMap<String, URI> canonicalUriMap = new ConcurrentHashMap();
    private final ArrayList<String> mods = new ArrayList();
    private final HashSet<String> loadedPacks = new HashSet();
    private FileGuidTable fileGuidTable;
    private boolean fileGuidTableWatcherActive;
    private final PredicatedFileWatcher modFileWatcher = new PredicatedFileWatcher(this::isModFile, this::onModFileChanged);
    private final HashSet<String> watchedModFolders = new HashSet();
    private long modsChangedTime;
    private static String startupTimeStamp;
    private static final SimpleDateFormat s_dateTimeSdf;
    private static final SimpleDateFormat s_dateOnlySdf;

    private ZomboidFileSystem() {
    }

    public void init() throws IOException {
        this.base.set(new File("./"));
        this.workdir.set(new File(this.base.canonicalFile, "media"));
        this.localWorkdir = this.base.canonicalFile.toPath().relativize(this.workdir.canonicalFile.toPath()).toFile();
        this.anims.set(new File(this.workdir.canonicalFile, "anims"));
        this.animsX.set(new File(this.workdir.canonicalFile, "anims_X"));
        this.animSets.set(new File(this.workdir.canonicalFile, "AnimSets"));
        this.actiongroups.set(new File(this.workdir.canonicalFile, "actiongroups"));
        this.searchFolders(this.workdir.canonicalFile);
        for (int n = 0; n < this.loadList.size(); ++n) {
            String rel = this.getRelativeFile(this.loadList.get(n));
            File file = new File(this.loadList.get(n)).getAbsoluteFile();
            Object abs = file.getAbsolutePath();
            if (file.isDirectory()) {
                abs = (String)abs + File.separator;
            }
            this.activeFileMap.put(rel.toLowerCase(Locale.ENGLISH), (String)abs);
            this.allAbsolutePaths.add((String)abs);
        }
        this.loadList.clear();
    }

    public File getCanonicalFile(File parent, String child) {
        if (!parent.isDirectory()) {
            return new File(parent, child);
        }
        File[] files = parent.listFiles((dir, name) -> name.equalsIgnoreCase(child));
        if (files == null || files.length == 0) {
            return new File(parent, child);
        }
        return files[0];
    }

    public String getGameModeCacheDir() {
        if (Core.gameMode == null) {
            Core.getInstance().setGameMode("Sandbox");
        }
        String cacheDirRoot = this.getSaveDir();
        return cacheDirRoot + File.separator + Core.gameMode;
    }

    public String getCurrentSaveDir() {
        return this.getGameModeCacheDir() + File.separator + Core.gameSaveWorld;
    }

    public String getFileNameInCurrentSave(String fileName) {
        return this.getCurrentSaveDir() + File.separator + fileName;
    }

    public String getFileNameInCurrentSave(String subDir, String fileName) {
        return this.getFileNameInCurrentSave(subDir + File.separator + fileName);
    }

    public String getFileNameInCurrentSave(String subDir1, String subDir2, String fileName) {
        return this.getFileNameInCurrentSave(subDir1 + File.separator + subDir2 + File.separator + fileName);
    }

    public File getFileInCurrentSave(String fileName) {
        return new File(this.getFileNameInCurrentSave(fileName));
    }

    public File getFileInCurrentSave(String subDir, String fileName) {
        return new File(this.getFileNameInCurrentSave(subDir, fileName));
    }

    public File getFileInCurrentSave(String subDir1, String subDir2, String fileName) {
        return new File(this.getFileNameInCurrentSave(subDir1, subDir2, fileName));
    }

    public String getSaveDir() {
        String savesDir = this.getCacheDirSub("Saves");
        ZomboidFileSystem.ensureFolderExists(savesDir);
        return savesDir;
    }

    public String getSaveDirSub(String subPath) {
        return this.getSaveDir() + File.separator + subPath;
    }

    public String getScreenshotDir() {
        String dir = this.getCacheDirSub("Screenshots");
        ZomboidFileSystem.ensureFolderExists(dir);
        return dir;
    }

    public String getScreenshotDirSub(String subPath) {
        return this.getScreenshotDir() + File.separator + subPath;
    }

    public void setCacheDir(String dir) {
        dir = dir.replace("/", File.separator);
        this.cacheDir = new File(dir).getAbsoluteFile();
        ZomboidFileSystem.ensureFolderExists(this.cacheDir);
    }

    public String getCacheDir() {
        if (this.cacheDir == null) {
            String cacheDirRoot = System.getProperty("deployment.user.cachedir");
            if (cacheDirRoot == null || System.getProperty("os.name").startsWith("Win")) {
                cacheDirRoot = System.getProperty("user.home");
            }
            String cacheDirPath = cacheDirRoot + File.separator + "Zomboid";
            this.setCacheDir(cacheDirPath);
        }
        return this.cacheDir.getPath();
    }

    public String getCacheDirSub(String subPath) {
        return this.getCacheDir() + File.separator + subPath;
    }

    public String getMessagingDir() {
        String messagingDir = this.getCacheDirSub("messaging");
        ZomboidFileSystem.ensureFolderExists(messagingDir);
        return messagingDir;
    }

    public String getMessagingDirSub(String subPath) {
        return this.getMessagingDir() + File.separator + subPath;
    }

    public URI getMediaLowercaseURI() {
        return this.workdir.lowercaseUri;
    }

    public File getMediaRootFile() {
        assert (this.workdir.canonicalFile != null);
        return this.workdir.canonicalFile;
    }

    public String getMediaRootPath() {
        return this.workdir.canonicalFile.getPath();
    }

    public File getMediaFile(String subPath) {
        assert (this.workdir.canonicalFile != null);
        return new File(this.workdir.canonicalFile, subPath);
    }

    public String getMediaPath(String subPath) {
        return this.getMediaFile(subPath).getPath();
    }

    public String getAbsoluteWorkDir() {
        return this.workdir.canonicalFile.getPath();
    }

    public String getLocalWorkDir() {
        return this.localWorkdir.getPath();
    }

    public String getLocalWorkDirSub(String subPath) {
        return this.getLocalWorkDir() + File.separator + subPath;
    }

    public File getAnimsXFile() {
        return this.animsX.canonicalFile;
    }

    public String getAnimSetsPath() {
        return this.animSets.canonicalFile.getPath();
    }

    public String getActionGroupsPath() {
        return this.actiongroups.canonicalFile.getPath();
    }

    public static boolean ensureFolderExists(String path) {
        return ZomboidFileSystem.ensureFolderExists(new File(path).getAbsoluteFile());
    }

    public static boolean ensureFolderExists(File directory) {
        return directory.exists() || directory.mkdirs();
    }

    public void searchFolders(File fo) {
        if (!GameServer.server) {
            Thread.yield();
            Core.getInstance().DoFrameReady();
        }
        if (fo.isDirectory()) {
            String path = fo.getAbsolutePath().replace("\\", "/").replace("./", "");
            if (path.contains("media/maps/")) {
                this.loadList.add(path);
            }
            String[] internalNames = fo.list();
            for (int i = 0; i < internalNames.length; ++i) {
                this.searchFolders(new File(fo.getAbsolutePath() + File.separator + internalNames[i]));
            }
        } else {
            this.loadList.add(fo.getAbsolutePath().replace("\\", "/").replace("./", ""));
        }
    }

    public Object[] getAllPathsContaining(String str) {
        ArrayList<String> loadList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.activeFileMap.entrySet()) {
            if (!entry.getKey().contains(str)) continue;
            loadList.add(entry.getValue());
        }
        return loadList.toArray();
    }

    public Object[] getAllPathsContaining(String str, String str2) {
        ArrayList<String> loadList = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.activeFileMap.entrySet()) {
            if (!entry.getKey().contains(str) || !entry.getKey().contains(str2)) continue;
            loadList.add(entry.getValue());
        }
        return loadList.toArray();
    }

    public synchronized String getString(String str) {
        if (this.ignoreActiveFileMap.get().booleanValue()) {
            return str;
        }
        String lower = str.toLowerCase(Locale.ENGLISH);
        String relative = this.relativeMap.get(lower);
        if (relative != null) {
            lower = relative;
        } else {
            String ostr = lower;
            lower = this.getRelativeFile(str);
            lower = lower.toLowerCase(Locale.ENGLISH);
            this.relativeMap.put(ostr, lower);
        }
        String absolute = this.activeFileMap.get(lower);
        if (absolute != null) {
            return absolute;
        }
        return str;
    }

    public synchronized String getDirectoryString(String str) {
        String absPath = this.getString(str);
        if (absPath != str) {
            return absPath;
        }
        if (str.endsWith("/")) {
            return this.getString(str.substring(0, str.length() - 1));
        }
        return this.getString(str + "/");
    }

    public synchronized boolean isKnownFile(String str) {
        if (this.allAbsolutePaths.contains(str)) {
            return true;
        }
        String lower = str.toLowerCase(Locale.ENGLISH);
        String relative = this.relativeMap.get(lower);
        if (relative != null) {
            lower = relative;
        } else {
            String ostr = lower;
            lower = this.getRelativeFile(str);
            lower = lower.toLowerCase(Locale.ENGLISH);
            this.relativeMap.put(ostr, lower);
        }
        String absolute = this.activeFileMap.get(lower);
        return absolute != null;
    }

    public String getAbsolutePath(String rel) {
        String lower = rel.toLowerCase(Locale.ENGLISH);
        return this.activeFileMap.get(lower);
    }

    public void Reset() {
        this.loadList.clear();
        this.activeFileMap.clear();
        this.allAbsolutePaths.clear();
        this.canonicalUriMap.clear();
        this.modIdToDir.clear();
        this.modDirToMod.clear();
        this.mods.clear();
        this.modFolders = null;
        ActiveMods.Reset();
        if (this.fileGuidTable != null) {
            this.fileGuidTable.clear();
            this.fileGuidTable = null;
        }
    }

    public File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        }
        catch (Exception e) {
            return file.getAbsoluteFile();
        }
    }

    public File getCanonicalFile(String path) {
        return this.getCanonicalFile(new File(path));
    }

    public String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        }
        catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    public String getCanonicalPath(String path) {
        return this.getCanonicalPath(new File(path));
    }

    public URI getCanonicalURI(String path) {
        URI uri = this.canonicalUriMap.get(path);
        if (uri == null) {
            uri = this.getCanonicalFile(path).toURI();
            this.canonicalUriMap.put(path, uri);
        }
        return uri;
    }

    public void resetModFolders() {
        this.modFolders = null;
    }

    public void getInstalledItemModsFolders(ArrayList<String> out) {
        String[] folders;
        if (SteamUtils.isSteamModeEnabled() && (folders = SteamWorkshop.instance.GetInstalledItemFolders()) != null) {
            for (String folder : folders) {
                File file = new File(folder + File.separator + "mods");
                if (!file.exists()) continue;
                out.add(file.getAbsolutePath());
            }
        }
    }

    public void getStagedItemModsFolders(ArrayList<String> out) {
        if (SteamUtils.isSteamModeEnabled()) {
            ArrayList<String> folders = SteamWorkshop.instance.getStageFolders();
            for (int i = 0; i < folders.size(); ++i) {
                File file = new File(folders.get(i) + File.separator + "Contents" + File.separator + "mods");
                if (!file.exists()) continue;
                out.add(file.getAbsolutePath());
            }
        }
    }

    public String getModVersionDirName(Path modDir) {
        int gameVersion = Core.getInstance().getGameVersion().getInt();
        String versionDirName = ChooseGameInfo.getMinRequiredVersion().toString();
        int bestVersion = ChooseGameInfo.getMinRequiredVersion().getInt();
        String[] dirFileNames = modDir.toFile().list();
        if (dirFileNames != null) {
            for (String name : dirFileNames) {
                int version = this.getGameVersionIntFromName(name);
                if (version < bestVersion || version > gameVersion) continue;
                versionDirName = name;
                bestVersion = version;
            }
        }
        return versionDirName;
    }

    private void getAllModFoldersAux(String folder, List<String> out) {
        Path dir = FileSystems.getDefault().getPath(folder, new String[0]);
        if (!Files.exists(dir, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, x$0 -> Files.isDirectory(x$0, new LinkOption[0]));){
            for (Path path : dstrm) {
                if (path.getFileName().toString().equalsIgnoreCase("examplemod")) {
                    DebugLog.Mod.println("refusing to list " + String.valueOf(path.getFileName()));
                    continue;
                }
                String commonDirName = "common";
                String versionDirName = this.getModVersionDirName(path);
                if (!Files.exists(path.resolve("common", new String[]{"mod.info"}), new LinkOption[0]) && !Files.exists(path.resolve(versionDirName, new String[]{"mod.info"}), new LinkOption[0])) continue;
                String absolutePath = path.toAbsolutePath().toString();
                Path commonDirMedia = path.resolve("common", new String[]{"media"});
                Path versionDirMedia = path.resolve(versionDirName, new String[]{"media"});
                if (!this.watchedModFolders.contains(absolutePath)) {
                    this.watchedModFolders.add(absolutePath);
                    DebugFileWatcher.instance.addDirectory(absolutePath);
                    if (Files.exists(commonDirMedia, new LinkOption[0])) {
                        DebugFileWatcher.instance.addDirectoryRecurse(commonDirMedia.toAbsolutePath().toString());
                    }
                    if (Files.exists(versionDirMedia, new LinkOption[0])) {
                        DebugFileWatcher.instance.addDirectoryRecurse(versionDirMedia.toAbsolutePath().toString());
                    }
                }
                out.add(absolutePath);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setModFoldersOrder(String s) {
        this.modFoldersOrder = new ArrayList<String>(Arrays.asList(s.split(",")));
    }

    public void getAllModFolders(List<String> out) {
        if (this.modFolders == null) {
            this.modFolders = new ArrayList();
            if (this.modFoldersOrder == null) {
                this.setModFoldersOrder("workshop,steam,mods");
            }
            ArrayList<String> modsFolders = new ArrayList<String>();
            for (int i = 0; i < this.modFoldersOrder.size(); ++i) {
                String s = this.modFoldersOrder.get(i);
                if ("workshop".equals(s)) {
                    this.getStagedItemModsFolders(modsFolders);
                }
                if ("steam".equals(s)) {
                    this.getInstalledItemModsFolders(modsFolders);
                }
                if (!"mods".equals(s)) continue;
                modsFolders.add(Core.getMyDocumentFolder() + File.separator + "mods");
            }
            for (int j = 0; j < modsFolders.size(); ++j) {
                String folder = (String)modsFolders.get(j);
                if (!this.watchedModFolders.contains(folder)) {
                    this.watchedModFolders.add(folder);
                    DebugFileWatcher.instance.addDirectory(folder);
                }
                this.getAllModFoldersAux(folder, this.modFolders);
            }
            DebugFileWatcher.instance.add(this.modFileWatcher);
        }
        out.clear();
        out.addAll(this.modFolders);
    }

    public int getGameVersionIntFromName(String name) {
        if (name == null) {
            return 0;
        }
        String[] parts = name.split("\\.");
        if (parts.length == 1) {
            return PZMath.tryParseInt(parts[0], 0) * 1000;
        }
        if (parts.length >= 2) {
            return PZMath.tryParseInt(parts[0], 0) * 1000 + Math.min(PZMath.tryParseInt(parts[1], 0), 999);
        }
        return 0;
    }

    public ArrayList<ChooseGameInfo.Mod> getWorkshopItemMods(long itemID) {
        File[] files;
        ArrayList<ChooseGameInfo.Mod> mods = new ArrayList<ChooseGameInfo.Mod>();
        if (!SteamUtils.isSteamModeEnabled()) {
            return mods;
        }
        String folder = SteamWorkshop.instance.GetItemInstallFolder(itemID);
        if (folder == null) {
            return mods;
        }
        File file = new File(folder + File.separator + "mods");
        if (!file.exists() || !file.isDirectory()) {
            return mods;
        }
        for (File file2 : files = file.listFiles()) {
            ChooseGameInfo.Mod modInfo;
            if (!file2.isDirectory() || (modInfo = ChooseGameInfo.readModInfo(file2.getAbsolutePath())) == null) continue;
            mods.add(modInfo);
        }
        return mods;
    }

    public void setModIdToDir(String id, String dir) {
        this.modIdToDir.putIfAbsent(id, dir);
    }

    public ChooseGameInfo.Mod searchForModInfo(File path, String modSearched, ArrayList<ChooseGameInfo.Mod> mods) {
        if (path.isDirectory()) {
            String[] internalNames = path.list();
            if (internalNames == null) {
                return null;
            }
            for (int i = 0; i < internalNames.length; ++i) {
                File file = new File(path.getAbsolutePath() + File.separator + internalNames[i]);
                ChooseGameInfo.Mod result = this.searchForModInfo(file, modSearched, mods);
                if (result == null) continue;
                return result;
            }
        } else if (path.getAbsolutePath().endsWith("mod.info")) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.readModInfo(path.getAbsoluteFile().getParent());
            if (mod == null) {
                return null;
            }
            if (!mod.getId().equals("\\")) {
                this.modIdToDir.put(mod.getId(), mod.getDir());
                mods.add(mod);
            }
            if (mod.getId().equals(modSearched)) {
                return mod;
            }
        }
        return null;
    }

    public void loadMod(String modId) {
        if (this.getModDir(modId) != null) {
            String absPath;
            String rel;
            int n;
            if (CoopMaster.instance != null) {
                CoopMaster.instance.update();
            }
            DebugLog.Mod.println("loading " + modId);
            ChooseGameInfo.Mod mod = this.getModInfoForDir(this.getModDir(modId));
            this.loadList.clear();
            File modPathbase = new File(mod.getCommonDir().toLowerCase(Locale.ENGLISH));
            URI modPathbaseURI = modPathbase.toURI();
            this.searchFolders(new File(mod.getCommonDir()));
            for (n = 0; n < this.loadList.size(); ++n) {
                rel = this.getRelativeFile(modPathbaseURI, this.loadList.get(n));
                if (this.activeFileMap.containsKey(rel = rel.toLowerCase(Locale.ENGLISH)) && !rel.endsWith("mod.info") && !rel.endsWith("poster.png")) {
                    DebugLog.Mod.println("mod \"" + modId + "\" overrides " + rel);
                }
                absPath = new File(this.loadList.get(n)).getAbsolutePath();
                this.activeFileMap.put(rel, absPath);
                this.allAbsolutePaths.add(absPath);
            }
            this.loadList.clear();
            modPathbase = new File(mod.getVersionDir().toLowerCase(Locale.ENGLISH));
            modPathbaseURI = modPathbase.toURI();
            this.searchFolders(new File(mod.getVersionDir()));
            for (n = 0; n < this.loadList.size(); ++n) {
                rel = this.getRelativeFile(modPathbaseURI, this.loadList.get(n));
                if (this.activeFileMap.containsKey(rel = rel.toLowerCase(Locale.ENGLISH)) && !rel.endsWith("mod.info") && !rel.endsWith("poster.png")) {
                    DebugLog.Mod.println("mod \"" + modId + "\" overrides " + rel);
                }
                absPath = new File(this.loadList.get(n)).getAbsolutePath();
                this.activeFileMap.put(rel, absPath);
                this.allAbsolutePaths.add(absPath);
            }
            this.loadList.clear();
        }
    }

    private ArrayList<String> readLoadedDotTxt() {
        String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "loaded.txt";
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        ArrayList<String> modIDs = new ArrayList<String>();
        try (FileReader fr = new FileReader(path);
             BufferedReader br = new BufferedReader(fr);){
            String str = br.readLine();
            while (str != null) {
                if (!(str = str.trim()).isEmpty()) {
                    modIDs.add(str);
                }
                str = br.readLine();
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            modIDs = null;
        }
        try {
            file.delete();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        return modIDs;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private ActiveMods readDefaultModsTxt() {
        ActiveMods activeMods = ActiveMods.getById("default");
        ArrayList<String> modIDs = this.readLoadedDotTxt();
        if (modIDs != null) {
            activeMods.getMods().addAll(modIDs);
            this.saveModsFile();
        }
        activeMods.clear();
        String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "default.txt";
        try {
            ActiveModsFile activeModsFile = new ActiveModsFile();
            if (!activeModsFile.read(path, activeMods)) return activeMods;
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        return activeMods;
    }

    public void loadMods(String activeMods) {
        if (!Core.optionModsEnabled) {
            return;
        }
        if (GameClient.client) {
            ArrayList<String> toLoad = new ArrayList<String>();
            this.loadTranslationMods(toLoad);
            toLoad.addAll(GameClient.instance.serverMods);
            this.loadMods(toLoad);
            return;
        }
        ActiveMods activeMods1 = ActiveMods.getById(activeMods);
        if (!"default".equalsIgnoreCase(activeMods)) {
            ActiveMods.setLoadedMods(activeMods1);
            this.loadMods(activeMods1.getMods());
            return;
        }
        try {
            activeMods1 = this.readDefaultModsTxt();
            activeMods1.checkMissingMods();
            activeMods1.checkMissingMaps();
            ActiveMods.setLoadedMods(activeMods1);
            this.loadMods(activeMods1.getMods());
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    private boolean isValidTranslationModSubDir(PZFolder pzFolder) {
        this.loadList.clear();
        this.searchFolders(pzFolder.canonicalFile);
        for (String path : this.loadList) {
            String rel = this.getRelativeFile(pzFolder.lowercaseUri, path);
            if (!rel.startsWith("media/")) continue;
            if (!rel.startsWith("media/lua/shared/translate/")) {
                return false;
            }
            if (rel.endsWith(".txt") || rel.endsWith(".json")) continue;
            return false;
        }
        return true;
    }

    private boolean isTranslationMod(String modId) {
        ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
        if (info == null) {
            return false;
        }
        return this.isValidTranslationModSubDir(info.baseFile.common) && this.isValidTranslationModSubDir(info.baseFile.version);
    }

    private void loadTranslationMods(ArrayList<String> toLoad) {
        if (!GameClient.client) {
            return;
        }
        ActiveMods activeMods = this.readDefaultModsTxt();
        ArrayList<String> mods = new ArrayList<String>();
        if (this.loadModsAux(activeMods.getMods(), mods) == null) {
            for (String modId : mods) {
                if (!this.isTranslationMod(modId)) continue;
                DebugLog.Mod.println("loading translation mod \"" + modId + "\"");
                if (toLoad.contains(modId)) continue;
                toLoad.add(modId);
            }
        }
    }

    private String loadModAndRequired(String modId, ArrayList<String> ordered) {
        String reqId;
        if (modId.isEmpty()) {
            return null;
        }
        if (modId.equalsIgnoreCase("examplemod")) {
            DebugLog.Mod.warn("refusing to load " + modId);
            return null;
        }
        if (ordered.contains(modId)) {
            return null;
        }
        ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
        if (info == null) {
            if (GameServer.server) {
                GameServer.ServerMods.remove(modId);
            }
            DebugLog.Mod.warn("required mod \"" + modId + "\" not found");
            return modId;
        }
        if (info.getRequire() != null && (reqId = this.loadModsAux(info.getRequire(), ordered)) != null) {
            return reqId;
        }
        ordered.add(modId);
        return null;
    }

    public String loadModsAux(ArrayList<String> toLoad, ArrayList<String> ordered) {
        for (String modId : toLoad) {
            String failId = this.loadModAndRequired(modId, ordered);
            if (failId == null) continue;
            return failId;
        }
        return null;
    }

    public void loadMods(ArrayList<String> toLoad) {
        this.mods.clear();
        for (String modId : toLoad) {
            this.loadModAndRequired(modId, this.mods);
        }
        for (String modId : this.mods) {
            this.loadMod(modId);
        }
    }

    public ArrayList<String> getModIDs() {
        return this.mods;
    }

    public String getModDir(String modId) {
        return this.modIdToDir.get(modId);
    }

    public ChooseGameInfo.Mod getModInfoForDir(String modDir) {
        ChooseGameInfo.Mod modInfo = this.modDirToMod.get(modDir);
        if (modInfo == null) {
            modInfo = new ChooseGameInfo.Mod(modDir);
            this.modDirToMod.put(modDir, modInfo);
        }
        return modInfo;
    }

    public ChunkGenerationStatus isModded(String path) {
        for (String lookup : this.modDirToMod.keySet()) {
            if (!path.startsWith(lookup)) continue;
            return ChunkGenerationStatus.MODDED;
        }
        return ChunkGenerationStatus.CORE;
    }

    public String getRelativeFile(File file) {
        return this.getRelativeFile(this.base.lowercaseUri, file.getAbsolutePath());
    }

    public String getRelativeFile(String string) {
        return this.getRelativeFile(this.base.lowercaseUri, string);
    }

    public String getRelativeFile(URI root, File file) {
        return this.getRelativeFile(root, file.getAbsolutePath());
    }

    public String getRelativeFile(URI root, String string) {
        String absolutePath = new File(string).getAbsolutePath();
        URI uri = new File(absolutePath.toLowerCase(Locale.ENGLISH)).toURI();
        URI rel = root.relativize(uri);
        if (rel.equals(uri)) {
            return string;
        }
        Object relPath = rel.getPath();
        if (string.endsWith("/") && !((String)relPath).endsWith("/")) {
            relPath = (String)relPath + "/";
        }
        return relPath;
    }

    public String getAnimName(URI mediaURI, File file) {
        String relativePath = this.getRelativeFile(mediaURI, file);
        String animName = relativePath.toLowerCase(Locale.ENGLISH);
        int dotIndex = animName.lastIndexOf(46);
        if (dotIndex > -1) {
            animName = animName.substring(0, dotIndex);
        }
        if (animName.startsWith("anims/")) {
            animName = animName.substring("anims/".length());
        } else if (animName.startsWith("anims_x/")) {
            animName = animName.substring("anims_x/".length());
        }
        return animName;
    }

    public String resolveRelativePath(String srcFilePath, String relativePath) {
        Path srcPath = Paths.get(srcFilePath, new String[0]);
        Path srcFolder = srcPath.getParent();
        Path destPath = srcFolder.resolve(relativePath);
        String result = destPath.toString();
        result = this.getRelativeFile(result);
        return result;
    }

    public void saveModsFile() {
        try {
            ZomboidFileSystem.ensureFolderExists(Core.getMyDocumentFolder() + File.separator + "mods");
            String path = Core.getMyDocumentFolder() + File.separator + "mods" + File.separator + "default.txt";
            ActiveModsFile activeModsFile = new ActiveModsFile();
            activeModsFile.write(path, ActiveMods.getById("default"));
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
    }

    public void loadModPackFiles() {
        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info == null) continue;
                for (ChooseGameInfo.PackFile pack : info.getPacks()) {
                    String rel = this.getRelativeFile("media/texturepacks/" + pack.name + ".pack");
                    if (!this.activeFileMap.containsKey(rel = rel.toLowerCase(Locale.ENGLISH))) {
                        DebugLog.Mod.warn("pack file \"" + pack.name + "\" needed by " + modId + " not found");
                        continue;
                    }
                    String fileName = instance.getString("media/texturepacks/" + pack.name + ".pack");
                    if (this.loadedPacks.contains(fileName)) continue;
                    GameWindow.LoadTexturePack(pack.name, pack.flags, modId);
                    this.loadedPacks.add(fileName);
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
        GameWindow.setTexturePackLookup();
    }

    public void loadModTileDefs() {
        HashSet<Integer> usedFileNumbers = new HashSet<Integer>();
        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info == null) continue;
                for (ChooseGameInfo.TileDef tileDef : info.getTileDefs()) {
                    if (usedFileNumbers.contains(tileDef.fileNumber)) {
                        DebugLog.Mod.error("tiledef fileNumber " + tileDef.fileNumber + " used by more than one mod");
                        continue;
                    }
                    String name = tileDef.name;
                    String rel = this.getRelativeFile("media/" + name + ".tiles");
                    if (!this.activeFileMap.containsKey(rel = rel.toLowerCase(Locale.ENGLISH))) {
                        DebugLog.Mod.error("tiledef file \"" + tileDef.name + "\" needed by " + modId + " not found");
                        continue;
                    }
                    name = this.activeFileMap.get(rel);
                    IsoWorld.instance.LoadTileDefinitions(IsoSpriteManager.instance, name, tileDef.fileNumber);
                    usedFileNumbers.add(tileDef.fileNumber);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void loadModTileDefPropertyStrings() {
        HashSet<Integer> usedFileNumbers = new HashSet<Integer>();
        for (String modId : this.mods) {
            try {
                ChooseGameInfo.Mod info = ChooseGameInfo.getAvailableModDetails(modId);
                if (info == null) continue;
                for (ChooseGameInfo.TileDef tileDef : info.getTileDefs()) {
                    if (usedFileNumbers.contains(tileDef.fileNumber)) {
                        DebugLog.Mod.error("tiledef fileNumber " + tileDef.fileNumber + " used by more than one mod");
                        continue;
                    }
                    String name = tileDef.name;
                    String rel = this.getRelativeFile("media/" + name + ".tiles");
                    if (!this.activeFileMap.containsKey(rel = rel.toLowerCase(Locale.ENGLISH))) {
                        DebugLog.Mod.error("tiledef file \"" + tileDef.name + "\" needed by " + modId + " not found");
                        continue;
                    }
                    name = this.activeFileMap.get(rel);
                    IsoWorld.instance.LoadTileDefinitionsPropertyStrings(IsoSpriteManager.instance, name, tileDef.fileNumber);
                    usedFileNumbers.add(tileDef.fileNumber);
                }
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void loadFileGuidTable() {
        File file = instance.getMediaFile("fileGuidTable.xml");
        try (FileInputStream adrFile = new FileInputStream(file);){
            JAXBContext ctx = JAXBContext.newInstance(FileGuidTable.class);
            Unmarshaller um = ctx.createUnmarshaller();
            this.fileGuidTable = (FileGuidTable)um.unmarshal(adrFile);
            this.fileGuidTable.setModID("game");
        }
        catch (IOException | JAXBException e) {
            System.err.println("Failed to load file Guid table.");
            ExceptionLogger.logException(e);
            return;
        }
        try {
            JAXBContext ctx = JAXBContext.newInstance(FileGuidTable.class);
            Unmarshaller um = ctx.createUnmarshaller();
            for (String modID : this.getModIDs()) {
                FileGuidTable table;
                FileInputStream fis2;
                ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
                if (mod == null) continue;
                try {
                    fis2 = new FileInputStream(mod.getCommonDir() + "/media/fileGuidTable.xml");
                    try {
                        table = (FileGuidTable)um.unmarshal(fis2);
                        table.setModID(modID);
                        this.fileGuidTable.mergeFrom(table);
                    }
                    finally {
                        fis2.close();
                    }
                }
                catch (FileNotFoundException fis2) {
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
                try {
                    fis2 = new FileInputStream(mod.getVersionDir() + "/media/fileGuidTable.xml");
                    try {
                        table = (FileGuidTable)um.unmarshal(fis2);
                        table.setModID(modID);
                        this.fileGuidTable.mergeFrom(table);
                    }
                    finally {
                        fis2.close();
                    }
                }
                catch (FileNotFoundException fis3) {
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        this.fileGuidTable.loaded();
        if (!this.fileGuidTableWatcherActive) {
            DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/fileGuidTable.xml", entryKey -> this.loadFileGuidTable()));
            this.fileGuidTableWatcherActive = true;
        }
    }

    public FileGuidTable getFileGuidTable() {
        if (this.fileGuidTable == null) {
            this.loadFileGuidTable();
        }
        return this.fileGuidTable;
    }

    public String getFilePathFromGuid(String guid) {
        FileGuidTable table = this.getFileGuidTable();
        if (table != null) {
            return table.getFilePathFromGuid(guid);
        }
        return null;
    }

    public String getGuidFromFilePath(String path) {
        FileGuidTable table = this.getFileGuidTable();
        if (table != null) {
            return table.getGuidFromFilePath(path);
        }
        return null;
    }

    public String resolveFileOrGUID(String source2) {
        String lower;
        String fileName = source2;
        String guidFileName = this.getFilePathFromGuid(source2);
        if (guidFileName != null) {
            fileName = guidFileName;
        }
        if (this.activeFileMap.containsKey(lower = fileName.toLowerCase(Locale.ENGLISH))) {
            return this.activeFileMap.get(lower);
        }
        return fileName;
    }

    public boolean isValidFilePathGuid(String source2) {
        return this.getFilePathFromGuid(source2) != null;
    }

    public static File[] listAllDirectories(String rootPath, FileFilter filter, boolean recursive) {
        File root = new File(rootPath).getAbsoluteFile();
        return ZomboidFileSystem.listAllDirectories(root, filter, recursive);
    }

    public static File[] listAllDirectories(File root, FileFilter filter, boolean recursive) {
        if (!root.isDirectory()) {
            return new File[0];
        }
        ArrayList<File> result = new ArrayList<File>();
        ZomboidFileSystem.listAllDirectoriesInternal(root, filter, recursive, result);
        return result.toArray(new File[0]);
    }

    private static void listAllDirectoriesInternal(File folder, FileFilter filter, boolean recursive, ArrayList<File> result) {
        File[] list = folder.listFiles();
        if (list == null) {
            return;
        }
        for (File listedFile : list) {
            if (listedFile.isFile() || !listedFile.isDirectory()) continue;
            if (filter.accept(listedFile)) {
                result.add(listedFile);
            }
            if (!recursive) continue;
            ZomboidFileSystem.listAllFilesInternal(listedFile, filter, true, result);
        }
    }

    public static File[] listAllFiles(String folderPath, FileFilter filter, boolean recursive) {
        File folder = new File(folderPath).getAbsoluteFile();
        return ZomboidFileSystem.listAllFiles(folder, filter, recursive);
    }

    public static File[] listAllFiles(File folder, FileFilter filter, boolean recursive) {
        if (folder == null || !folder.isDirectory()) {
            return new File[0];
        }
        ArrayList<File> result = new ArrayList<File>();
        ZomboidFileSystem.listAllFilesInternal(folder, filter, recursive, result);
        return result.toArray(new File[0]);
    }

    public static File[] listAllFiles(File folder) {
        return ZomboidFileSystem.listAllFiles(folder, (File file) -> true, false);
    }

    private static void listAllFilesInternal(File folder, FileFilter filter, boolean recursive, ArrayList<File> result) {
        File[] list = folder.listFiles();
        if (list == null) {
            return;
        }
        for (File listedFile : list) {
            if (listedFile.isFile()) {
                if (!filter.accept(listedFile)) continue;
                result.add(listedFile);
                continue;
            }
            if (!listedFile.isDirectory() || !recursive) continue;
            ZomboidFileSystem.listAllFilesInternal(listedFile, filter, true, result);
        }
    }

    public void walkGameAndModFiles(String relPath, boolean recursive, IWalkFilesVisitor consumer) {
        this.walkGameAndModFilesInternal(this.base.canonicalFile, relPath, recursive, consumer);
        ArrayList<String> modIDs = this.getModIDs();
        for (int n = 0; n < modIDs.size(); ++n) {
            String modID = modIDs.get(n);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            this.walkGameAndModFilesInternal(new File(mod.getCommonDir()), relPath, recursive, consumer);
            this.walkGameAndModFilesInternal(new File(mod.getVersionDir()), relPath, recursive, consumer);
        }
    }

    private void walkGameAndModFilesInternal(File rootFile, String relPath, boolean recursive, IWalkFilesVisitor consumer) {
        File fo = new File(rootFile, relPath);
        if (!fo.isDirectory()) {
            return;
        }
        File[] list = fo.listFiles();
        if (list == null) {
            return;
        }
        for (File listedFile : list) {
            consumer.visit(listedFile, relPath);
            if (!recursive || !listedFile.isDirectory()) continue;
            this.walkGameAndModFilesInternal(rootFile, relPath + "/" + listedFile.getName(), true, consumer);
        }
    }

    public String[] resolveAllDirectories(String relPath, FileFilter filter, boolean recursive) {
        ArrayList result = new ArrayList();
        this.walkGameAndModFiles(relPath, recursive, (file, relPath2) -> {
            String relPath3;
            if (file.isDirectory() && filter.accept(file) && !result.contains(relPath3 = relPath2 + "/" + file.getName())) {
                result.add(relPath3);
            }
        });
        return result.toArray(new String[0]);
    }

    public String[] resolveAllFiles(String relPath, FileFilter filter, boolean recursive) {
        ArrayList result = new ArrayList();
        this.walkGameAndModFiles(relPath, recursive, (file, relPath2) -> {
            String relPath3;
            if (file.isFile() && filter.accept(file) && !result.contains(relPath3 = relPath2 + "/" + file.getName())) {
                result.add(relPath3);
            }
        });
        return result.toArray(new String[0]);
    }

    public String normalizeFolderPath(String path) {
        path = ((String)path).toLowerCase(Locale.ENGLISH).replace('\\', '/');
        path = (String)path + "/";
        path = ((String)path).replace("///", "/").replace("//", "/");
        return path;
    }

    public static String processFilePath(String filePath, char separatorChar) {
        if (separatorChar != '\\') {
            filePath = filePath.replace('\\', separatorChar);
        }
        if (separatorChar != '/') {
            filePath = filePath.replace('/', separatorChar);
        }
        return filePath;
    }

    public boolean tryDeleteFile(String filePath) {
        if (StringUtils.isNullOrWhitespace(filePath)) {
            return false;
        }
        try {
            return this.deleteFile(filePath);
        }
        catch (IOException e) {
            DebugType.FileIO.printException(e, String.format("Failed to delete file: \"%s\"", filePath), LogSeverity.General);
            return false;
        }
    }

    public static boolean deleteDirectory(String dirPath) {
        File directoryToBeDeleted = new File(dirPath);
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                ZomboidFileSystem.deleteDirectory(file.getAbsolutePath());
            }
        }
        return directoryToBeDeleted.delete();
    }

    public boolean deleteFile(String filePath) throws IOException {
        File file = new File(filePath).getAbsoluteFile();
        if (!file.isFile()) {
            throw new FileNotFoundException(String.format("File path not found: \"%s\"", filePath));
        }
        if (file.delete()) {
            DebugType.FileIO.debugln("File deleted successfully: \"%s\"", filePath);
            return true;
        }
        DebugType.FileIO.debugln("Failed to delete file: \"%s\"", filePath);
        return false;
    }

    public void update() {
        if (this.modsChangedTime == 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.modsChangedTime > now) {
            return;
        }
        this.modsChangedTime = 0L;
        this.modFolders = null;
        this.modIdToDir.clear();
        this.modDirToMod.clear();
        ChooseGameInfo.Reset();
        for (String modID : this.getModIDs()) {
            ChooseGameInfo.getModDetails(modID);
        }
        LuaEventManager.triggerEvent("OnModsModified");
    }

    private boolean isModFile(String path) {
        if (this.modsChangedTime > 0L) {
            return false;
        }
        if (this.modFolders == null) {
            return false;
        }
        if ((path = path.toLowerCase().replace('\\', '/')).endsWith("/mods/default.txt")) {
            return false;
        }
        for (int i = 0; i < this.modFolders.size(); ++i) {
            String path1 = this.modFolders.get(i).toLowerCase().replace('\\', '/');
            if (!path.startsWith(path1)) continue;
            return true;
        }
        return false;
    }

    private void onModFileChanged(String path) {
        this.modsChangedTime = System.currentTimeMillis() + 2000L;
    }

    public void cleanMultiplayerSaves() {
        DebugType.FileIO.println("Start cleaning save fs");
        String cacheDirRoot = this.getSaveDir();
        String saveDirRoot = cacheDirRoot + File.separator + "Multiplayer" + File.separator;
        File mpDir = new File(saveDirRoot);
        if (!mpDir.exists()) {
            mpDir.mkdir();
        }
        try {
            File[] mpDirFiles;
            for (File saveDir : mpDirFiles = mpDir.listFiles()) {
                File mapBin;
                DebugType.FileIO.println("Checking " + String.valueOf(saveDir.getAbsoluteFile()) + " dir");
                if (!saveDir.isDirectory() || !(mapBin = new File(String.valueOf(saveDir) + File.separator + "map.bin")).exists()) continue;
                DebugType.FileIO.println("Processing dir: %s", saveDir.getAbsoluteFile());
                try {
                    Stream<Path> files = Files.walk(saveDir.toPath(), new FileVisitOption[0]);
                    files.forEach(file -> {
                        if (file.getFileName().toString().matches("map_\\d+_\\d+.bin")) {
                            DebugType.FileIO.println("Delete %s", file.getFileName().toString());
                            file.toFile().delete();
                        }
                    });
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        catch (RuntimeException e) {
            DebugType.FileIO.printException(e, e.toString(), LogSeverity.Warning);
        }
    }

    public void resetDefaultModsForNewRelease(String versionStr) {
        ZomboidFileSystem.ensureFolderExists(this.getCacheDirSub("mods"));
        String path = this.getCacheDirSub("mods") + File.separator + "reset-mods-" + versionStr + ".txt";
        File file = new File(path);
        if (file.exists()) {
            return;
        }
        try (FileWriter fr = new FileWriter(file);
             BufferedWriter br = new BufferedWriter(fr);){
            String contents = "If this file does not exist, default.txt will be reset to empty (no mods active).";
            br.write("If this file does not exist, default.txt will be reset to empty (no mods active).");
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return;
        }
        ActiveMods activeMods = ActiveMods.getById("default");
        activeMods.clear();
        this.saveModsFile();
    }

    public static synchronized String getStartupTimeStamp() {
        if (startupTimeStamp == null) {
            startupTimeStamp = ZomboidFileSystem.getDateTimeStampStringNow();
        }
        return startupTimeStamp;
    }

    public static String getDateTimeStampStringNow() {
        return ZomboidFileSystem.getTimeStampStringNow(s_dateTimeSdf);
    }

    public static String getDateTimeStampString(Date time) {
        return ZomboidFileSystem.getTimeStampString(time, s_dateTimeSdf);
    }

    public static String getDateStampString(Date time) {
        return ZomboidFileSystem.getTimeStampString(time, s_dateOnlySdf);
    }

    public static String getTimeStampString(Date time, SimpleDateFormat format) {
        return format.format(time);
    }

    public static String getTimeStampStringNow(SimpleDateFormat format) {
        Date time = Calendar.getInstance().getTime();
        return ZomboidFileSystem.getTimeStampString(time, format);
    }

    static {
        s_dateTimeSdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        s_dateOnlySdf = new SimpleDateFormat("yyyy-MM-dd");
    }

    public static final class PZFolder {
        public File absoluteFile;
        public File canonicalFile;
        public URI canonicalUri;
        public URI lowercaseUri;

        public void set(File file) throws IOException {
            this.absoluteFile = file.getAbsoluteFile();
            this.canonicalFile = this.absoluteFile.getCanonicalFile();
            this.canonicalUri = this.canonicalFile.toURI();
            this.lowercaseUri = new File(this.canonicalFile.getPath().toLowerCase(Locale.ENGLISH)).toURI();
        }

        public void setWithCatch(File file) {
            try {
                this.set(file);
            }
            catch (IOException ex) {
                this.canonicalFile = null;
                this.canonicalUri = null;
                this.lowercaseUri = null;
                ExceptionLogger.logException(ex);
            }
        }
    }

    public static final class PZModFolder {
        public PZFolder common = new PZFolder();
        public PZFolder version = new PZFolder();

        public void setWithCatch(File commonFile, File versionFile) {
            this.common.setWithCatch(commonFile);
            this.version.setWithCatch(versionFile);
        }
    }

    public static interface IWalkFilesVisitor {
        public void visit(File var1, String var2);
    }
}

