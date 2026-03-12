/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.util.list.PZArrayUtil;

public final class DebugFileWatcher {
    private boolean isInitialized;
    private final HashMap<Path, String> watchedFiles = new HashMap();
    private final HashMap<WatchKey, Path> watchkeyMapping = new HashMap();
    private final ArrayList<PredicatedFileWatcher> predicateWatchers = new ArrayList();
    private final ArrayList<PredicatedFileWatcher> predicateWatchersInvoking = new ArrayList();
    private final FileSystem fs = FileSystems.getDefault();
    private WatchService watcher;
    private boolean predicateWatchersInvokingDirty = true;
    private long modificationTime = -1L;
    private final ArrayList<String> modifiedFiles = new ArrayList();
    private final ArrayList<IOnInitListener> onInitListeners = new ArrayList();
    public static final DebugFileWatcher instance = new DebugFileWatcher();

    private DebugFileWatcher() {
    }

    public void init() {
        try {
            this.watcher = this.fs.newWatchService();
            this.registerDirRecursive(this.fs.getPath(ZomboidFileSystem.instance.getMediaRootPath(), new String[0]));
            this.registerDirRecursive(this.fs.getPath(ZomboidFileSystem.instance.getMessagingDir(), new String[0]));
            this.isInitialized = true;
            this.invokeOnInitListeners();
        }
        catch (IOException except) {
            this.watcher = null;
        }
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    private void invokeOnInitListeners() {
        ArrayList<IOnInitListener> listeners = new ArrayList<IOnInitListener>(this.onInitListeners);
        this.onInitListeners.clear();
        for (IOnInitListener listener : listeners) {
            listener.onInit(this);
        }
    }

    private void addOnInitListener(IOnInitListener listener) {
        if (!this.onInitListeners.contains(listener)) {
            this.onInitListeners.add(listener);
        }
    }

    private void registerDirRecursive(Path start) {
        try {
            Files.walkFileTree(start, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
                final /* synthetic */ DebugFileWatcher this$0;
                {
                    DebugFileWatcher debugFileWatcher = this$0;
                    Objects.requireNonNull(debugFileWatcher);
                    this.this$0 = debugFileWatcher;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    this.this$0.registerDir(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException except) {
            ExceptionLogger.logException(except);
            this.watcher = null;
        }
    }

    private void registerDir(Path dir) {
        if (!this.isInitialized()) {
            this.addOnInitListener(watcher -> this.registerDir(dir));
            return;
        }
        try {
            WatchKey key = dir.register(this.watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            this.watchkeyMapping.put(key, dir);
        }
        catch (IOException except) {
            ExceptionLogger.logException(except);
            this.watcher = null;
        }
    }

    private void addWatchedFile(String path) {
        if (path != null) {
            this.watchedFiles.put(this.fs.getPath(path, new String[0]), path);
        }
    }

    public void add(PredicatedFileWatcher watcher) {
        if (!this.predicateWatchers.contains(watcher)) {
            this.addWatchedFile(watcher.getPath());
            this.predicateWatchers.add(watcher);
            this.predicateWatchersInvokingDirty = true;
        }
    }

    public void addDirectory(String path) {
        if (path != null) {
            this.registerDir(this.fs.getPath(path, new String[0]));
        }
    }

    public void addDirectoryRecurse(String path) {
        if (path != null) {
            this.registerDirRecursive(this.fs.getPath(path, new String[0]));
        }
    }

    public void remove(PredicatedFileWatcher watcher) {
        this.predicateWatchers.remove(watcher);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        if (this.watcher == null) {
            return;
        }
        WatchKey key = this.watcher.poll();
        while (key != null) {
            try {
                Path dir = this.watchkeyMapping.getOrDefault(key, null);
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    String registeredPath;
                    Path fullPath;
                    Path filename;
                    WatchEvent<?> watchEventPath;
                    if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        watchEventPath = watchEvent;
                        filename = (Path)watchEventPath.context();
                        fullPath = dir.resolve(filename);
                        registeredPath = this.watchedFiles.getOrDefault(fullPath, fullPath.toString());
                        this.modificationTime = System.currentTimeMillis();
                        if (this.modifiedFiles.contains(registeredPath)) continue;
                        this.modifiedFiles.add(registeredPath);
                        continue;
                    }
                    if (watchEvent.kind() != StandardWatchEventKinds.ENTRY_CREATE) continue;
                    watchEventPath = watchEvent;
                    filename = (Path)watchEventPath.context();
                    fullPath = dir.resolve(filename);
                    if (Files.isDirectory(fullPath, new LinkOption[0])) {
                        this.registerDirRecursive(fullPath);
                        continue;
                    }
                    registeredPath = this.watchedFiles.getOrDefault(fullPath, fullPath.toString());
                    this.modificationTime = System.currentTimeMillis();
                    if (this.modifiedFiles.contains(registeredPath)) continue;
                    this.modifiedFiles.add(registeredPath);
                }
            }
            finally {
                if (!key.reset()) {
                    this.watchkeyMapping.remove(key);
                }
            }
            key = this.watcher.poll();
        }
        if (this.modifiedFiles.isEmpty()) {
            return;
        }
        if (this.modificationTime + 2000L > System.currentTimeMillis()) {
            return;
        }
        for (int i = this.modifiedFiles.size() - 1; i >= 0; --i) {
            String registeredPath = this.modifiedFiles.remove(i);
            this.swapWatcherArrays();
            for (PredicatedFileWatcher watcher : this.predicateWatchersInvoking) {
                watcher.onModified(registeredPath);
            }
        }
    }

    private void swapWatcherArrays() {
        if (this.predicateWatchersInvokingDirty) {
            this.predicateWatchersInvoking.clear();
            PZArrayUtil.addAll(this.predicateWatchersInvoking, this.predicateWatchers);
            this.predicateWatchersInvokingDirty = false;
        }
    }

    public static interface IOnInitListener {
        public void onInit(DebugFileWatcher var1);
    }
}

