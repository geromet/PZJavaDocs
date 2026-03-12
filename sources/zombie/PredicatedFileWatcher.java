/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.io.File;
import java.util.function.Predicate;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

public final class PredicatedFileWatcher {
    private final String path;
    private final Predicate<String> predicate;
    private final IPredicatedFileWatcherCallback callback;

    public PredicatedFileWatcher(Predicate<String> predicate, IPredicatedFileWatcherCallback callback) {
        this(null, predicate, callback);
    }

    public PredicatedFileWatcher(String path, IPredicatedFileWatcherCallback callback) {
        this(path, null, callback);
    }

    public <T> PredicatedFileWatcher(String path, Class<T> clazz, IPredicatedDataPacketFileWatcherCallback<T> callback) {
        this(path, null, new GenericPredicatedFileWatcherCallback<T>(clazz, callback));
    }

    public PredicatedFileWatcher(String path, Predicate<String> predicate, IPredicatedFileWatcherCallback callback) {
        this.path = this.processPath(path);
        this.predicate = predicate != null ? predicate : this::pathsEqual;
        this.callback = callback;
    }

    public String getPath() {
        return this.path;
    }

    private String processPath(String path) {
        if (path != null) {
            return ZomboidFileSystem.processFilePath(path, File.separatorChar);
        }
        return null;
    }

    private boolean pathsEqual(String entryKey) {
        return entryKey.equals(this.path);
    }

    public void onModified(String entryKey) {
        if (this.predicate.test(entryKey)) {
            this.callback.call(entryKey);
        }
    }

    public static interface IPredicatedFileWatcherCallback {
        public void call(String var1);
    }

    public static class GenericPredicatedFileWatcherCallback<T>
    implements IPredicatedFileWatcherCallback {
        private final Class<T> clazz;
        private final IPredicatedDataPacketFileWatcherCallback<T> callback;

        public GenericPredicatedFileWatcherCallback(Class<T> clazz, IPredicatedDataPacketFileWatcherCallback<T> callback) {
            this.clazz = clazz;
            this.callback = callback;
        }

        @Override
        public void call(String xmlFile) {
            T data;
            try {
                data = PZXmlUtil.parse(this.clazz, xmlFile);
            }
            catch (PZXmlParserException e) {
                DebugLog.General.error("Exception thrown. " + String.valueOf(e));
                return;
            }
            this.callback.call(data);
        }
    }

    public static interface IPredicatedDataPacketFileWatcherCallback<T> {
        public void call(T var1);
    }
}

