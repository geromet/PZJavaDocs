/*
 * Decompiled with CFR 0.152.
 */
package zombie.fileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import zombie.GameWindow;
import zombie.core.logger.ExceptionLogger;
import zombie.fileSystem.DeviceList;
import zombie.fileSystem.DiskFileDevice;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;
import zombie.fileSystem.IFile;
import zombie.fileSystem.IFileDevice;
import zombie.fileSystem.IFileTask2Callback;
import zombie.fileSystem.MemoryFileDevice;
import zombie.fileSystem.TexturePackDevice;
import zombie.gameStates.GameLoadingState;

public final class FileSystemImpl
extends FileSystem {
    private final ArrayList<DeviceList> devices = new ArrayList();
    private final ArrayList<AsyncItem> inProgress = new ArrayList();
    private final ArrayList<AsyncItem> pending = new ArrayList();
    private int lastId;
    private final DiskFileDevice diskDevice;
    private final MemoryFileDevice memoryFileDevice;
    private final HashMap<String, TexturePackDevice> texturepackDevices = new HashMap();
    private final HashMap<String, DeviceList> texturepackDevicelists = new HashMap();
    private final DeviceList defaultDevice;
    private final ExecutorService executor;
    private final AtomicBoolean lock = new AtomicBoolean(false);
    private final ArrayList<AsyncItem> added = new ArrayList();
    public static final HashMap<String, Boolean> TexturePackCompression = new HashMap();

    public FileSystemImpl() {
        this.diskDevice = new DiskFileDevice("disk");
        this.memoryFileDevice = new MemoryFileDevice();
        this.defaultDevice = new DeviceList();
        this.defaultDevice.add(this.diskDevice);
        this.defaultDevice.add(this.memoryFileDevice);
        int numThreads = Runtime.getRuntime().availableProcessors() <= 4 ? 2 : 4;
        this.executor = Executors.newFixedThreadPool(numThreads);
    }

    @Override
    public boolean mount(IFileDevice device) {
        return true;
    }

    @Override
    public boolean unMount(IFileDevice device) {
        return this.devices.remove(device);
    }

    @Override
    public IFile open(DeviceList deviceList, String path, int mode) {
        IFile file = deviceList.createFile();
        if (file != null) {
            if (file.open(path, mode)) {
                return file;
            }
            file.release();
            return null;
        }
        return null;
    }

    @Override
    public void close(IFile file) {
        file.close();
        file.release();
    }

    @Override
    public int openAsync(DeviceList deviceList, String path, int mode, IFileTask2Callback cb) {
        IFile file = deviceList.createFile();
        if (file != null) {
            OpenTask item = new OpenTask(this);
            item.file = file;
            item.path = path;
            item.mode = mode;
            item.cb = cb;
            return this.runAsync(item);
        }
        return -1;
    }

    @Override
    public void closeAsync(IFile file, IFileTask2Callback cb) {
        CloseTask item = new CloseTask(this);
        item.file = file;
        item.cb = cb;
        this.runAsync(item);
    }

    @Override
    public void cancelAsync(int id) {
        AsyncItem item;
        int i;
        if (id == -1) {
            return;
        }
        for (i = 0; i < this.pending.size(); ++i) {
            item = this.pending.get(i);
            if (item.id != id) continue;
            item.future.cancel(false);
            return;
        }
        for (i = 0; i < this.inProgress.size(); ++i) {
            item = this.inProgress.get(i);
            if (item.id != id) continue;
            item.future.cancel(false);
            return;
        }
        while (true) {
            if (this.lock.compareAndSet(false, true)) {
                for (i = 0; i < this.added.size(); ++i) {
                    item = this.added.get(i);
                    if (item.id != id) continue;
                    item.future.cancel(false);
                    break;
                }
                break;
            }
            Thread.onSpinWait();
        }
        this.lock.set(false);
    }

    @Override
    public InputStream openStream(DeviceList deviceList, String path) throws IOException {
        return deviceList.createStream(path);
    }

    @Override
    public void closeStream(InputStream stream) {
    }

    private int runAsync(AsyncItem item) {
        Thread thread2 = Thread.currentThread();
        if (thread2 != GameWindow.gameThread && thread2 != GameLoadingState.loader) {
            boolean bl = true;
        }
        while (true) {
            if (this.lock.compareAndSet(false, true)) {
                item.id = this.lastId++;
                if (this.lastId < 0) {
                    this.lastId = 0;
                }
                break;
            }
            Thread.onSpinWait();
        }
        this.added.add(item);
        this.lock.set(false);
        return item.id;
    }

    @Override
    public int runAsync(FileTask fileTask) {
        AsyncItem item = new AsyncItem();
        item.task = fileTask;
        item.future = new FutureTask<Object>(fileTask);
        return this.runAsync(item);
    }

    @Override
    public void updateAsyncTransactions() {
        int n = Math.min(this.inProgress.size(), 16);
        for (int i = 0; i < n; ++i) {
            AsyncItem item = this.inProgress.get(i);
            if (!item.future.isDone()) continue;
            this.inProgress.remove(i--);
            --n;
            if (item.future.isCancelled()) {
                boolean bl = true;
            } else {
                Object result = null;
                try {
                    result = item.future.get();
                }
                catch (Throwable t) {
                    ExceptionLogger.logException(t, item.task.getErrorMessage());
                }
                item.task.handleResult(result);
            }
            item.task.done();
            item.task = null;
            item.future = null;
        }
        while (true) {
            if (this.lock.compareAndSet(false, true)) {
                boolean priority = true;
                for (int i = 0; i < this.added.size(); ++i) {
                    AsyncItem item = this.added.get(i);
                    int insertAt = this.pending.size();
                    for (int j = 0; j < this.pending.size(); ++j) {
                        AsyncItem item1 = this.pending.get(j);
                        if (item.task.priority <= item1.task.priority) continue;
                        insertAt = j;
                        break;
                    }
                    this.pending.add(insertAt, item);
                }
                break;
            }
            Thread.onSpinWait();
        }
        this.added.clear();
        this.lock.set(false);
        int canAdd = 16 - this.inProgress.size();
        while (canAdd > 0 && !this.pending.isEmpty()) {
            AsyncItem item = this.pending.remove(0);
            if (item.future.isCancelled()) {
                item.task.done();
                continue;
            }
            this.inProgress.add(item);
            this.executor.submit(item.future);
            --canAdd;
        }
    }

    @Override
    public boolean hasWork() {
        if (!this.pending.isEmpty() || !this.inProgress.isEmpty()) {
            return true;
        }
        while (true) {
            if (this.lock.compareAndSet(false, true)) {
                boolean hasWork = !this.added.isEmpty();
                this.lock.set(false);
                return hasWork;
            }
            Thread.onSpinWait();
        }
    }

    @Override
    public DeviceList getDefaultDevice() {
        return this.defaultDevice;
    }

    @Override
    public void mountTexturePack(String name, FileSystem.TexturePackTextures subTextures, int flags) {
        TexturePackDevice texturePackDevice = new TexturePackDevice(name, flags);
        if (subTextures != null) {
            try {
                texturePackDevice.getSubTextureInfo(subTextures);
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
            }
        }
        this.texturepackDevices.put(name, texturePackDevice);
        DeviceList deviceList = new DeviceList();
        deviceList.add(texturePackDevice);
        this.texturepackDevicelists.put(texturePackDevice.name(), deviceList);
    }

    @Override
    public DeviceList getTexturePackDevice(String name) {
        return this.texturepackDevicelists.get(name);
    }

    @Override
    public int getTexturePackFlags(String name) {
        return this.texturepackDevices.get(name).getTextureFlags();
    }

    @Override
    public boolean getTexturePackAlpha(String name, String page) {
        return this.texturepackDevices.get(name).isAlpha(page);
    }

    private static final class OpenTask
    extends FileTask {
        IFile file;
        String path;
        int mode;
        IFileTask2Callback cb;

        OpenTask(FileSystem fileSystem) {
            super(fileSystem);
        }

        @Override
        public Object call() throws Exception {
            return this.file.open(this.path, this.mode);
        }

        @Override
        public void handleResult(Object result) {
            if (this.cb != null) {
                this.cb.onFileTaskFinished(this.file, result);
            }
        }

        @Override
        public void done() {
            if ((this.mode & 5) == 5) {
                this.fileSystem.closeAsync(this.file, null);
            }
            this.file = null;
            this.path = null;
            this.cb = null;
        }
    }

    private static final class CloseTask
    extends FileTask {
        IFile file;
        IFileTask2Callback cb;

        CloseTask(FileSystem fileSystem) {
            super(fileSystem);
        }

        @Override
        public Object call() throws Exception {
            this.file.close();
            this.file.release();
            return null;
        }

        @Override
        public void handleResult(Object result) {
            if (this.cb != null) {
                this.cb.onFileTaskFinished(this.file, result);
            }
        }

        @Override
        public void done() {
            this.file = null;
            this.cb = null;
        }
    }

    private static final class AsyncItem {
        int id;
        FileTask task;
        FutureTask<Object> future;

        private AsyncItem() {
        }
    }
}

