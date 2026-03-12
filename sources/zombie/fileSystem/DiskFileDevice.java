/*
 * Decompiled with CFR 0.152.
 */
package zombie.fileSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import zombie.core.logger.ExceptionLogger;
import zombie.fileSystem.FileOpenMode;
import zombie.fileSystem.FileSeekMode;
import zombie.fileSystem.IFile;
import zombie.fileSystem.IFileDevice;

public final class DiskFileDevice
implements IFileDevice {
    private final String name;

    public DiskFileDevice(String name) {
        this.name = name;
    }

    @Override
    public IFile createFile(IFile child) {
        return new DiskFile(child, this);
    }

    @Override
    public void destroyFile(IFile file) {
    }

    @Override
    public InputStream createStream(String path, InputStream child) throws IOException {
        return new FileInputStream(path);
    }

    @Override
    public void destroyStream(InputStream stream) {
    }

    @Override
    public String name() {
        return this.name;
    }

    private static final class DiskFile
    implements IFile {
        final DiskFileDevice device;
        RandomAccessFile file;
        InputStream inputStream;
        final IFile fallthrough;
        boolean useFallthrough;

        DiskFile(IFile fallthrough, DiskFileDevice device) {
            this.device = device;
            this.fallthrough = fallthrough;
            this.useFallthrough = false;
        }

        @Override
        public boolean open(String path, int mode) {
            boolean wantRead;
            File file = new File(path);
            boolean bl = wantRead = (mode & 1) != 0;
            if (wantRead && !file.exists() && this.fallthrough != null) {
                this.useFallthrough = true;
                return this.fallthrough.open(path, mode);
            }
            try {
                if ((mode & 0x10) == 0) {
                    this.file = new RandomAccessFile(path, FileOpenMode.toStringMode(mode));
                } else {
                    this.inputStream = new FileInputStream(path);
                }
                return true;
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return false;
            }
        }

        @Override
        public void close() {
            if (this.fallthrough != null) {
                this.fallthrough.close();
            }
            if (this.file == null && this.inputStream == null) {
                return;
            }
            try {
                if (this.file != null) {
                    this.file.close();
                }
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
            }
            this.file = null;
            this.inputStream = null;
            this.useFallthrough = false;
        }

        @Override
        public boolean read(byte[] buffer, long size) {
            if (this.useFallthrough) {
                return this.fallthrough.read(buffer, size);
            }
            if (this.file == null) {
                return false;
            }
            try {
                return (long)this.file.read(buffer, 0, (int)size) == size;
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return false;
            }
        }

        @Override
        public boolean write(byte[] buffer, long size) {
            if (this.useFallthrough) {
                return this.fallthrough.write(buffer, size);
            }
            if (this.file == null) {
                return false;
            }
            try {
                this.file.write(buffer, 0, (int)size);
                return true;
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return false;
            }
        }

        @Override
        public byte[] getBuffer() {
            if (this.useFallthrough) {
                return this.fallthrough.getBuffer();
            }
            return null;
        }

        @Override
        public long size() {
            if (this.useFallthrough) {
                return this.fallthrough.size();
            }
            if (this.file == null) {
                return 0L;
            }
            try {
                return this.file.length();
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return 0L;
            }
        }

        @Override
        public boolean seek(FileSeekMode mode, long pos) {
            if (this.useFallthrough) {
                return this.fallthrough.seek(mode, pos);
            }
            if (this.file == null) {
                return false;
            }
            try {
                switch (mode) {
                    case BEGIN: {
                        break;
                    }
                    case CURRENT: {
                        pos += this.file.getFilePointer();
                        break;
                    }
                    case END: {
                        pos = this.file.length() + pos;
                    }
                }
                this.file.seek(pos);
                return true;
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return false;
            }
        }

        @Override
        public long pos() {
            if (this.useFallthrough) {
                return this.fallthrough.pos();
            }
            if (this.file == null) {
                return 0L;
            }
            try {
                return this.file.getFilePointer();
            }
            catch (IOException ex) {
                ExceptionLogger.logException(ex);
                return 0L;
            }
        }

        @Override
        public InputStream getInputStream() {
            return this.inputStream;
        }

        @Override
        public IFileDevice getDevice() {
            return this.device;
        }

        @Override
        public void release() {
            this.getDevice().destroyFile(this);
        }
    }
}

