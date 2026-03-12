/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.codec.binary.Hex;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.packets.service.ChecksumPacket;

public final class NetChecksum {
    public static final Checksummer checksummer = new Checksummer();
    public static final Comparer comparer = new Comparer();

    public static final class Checksummer {
        private MessageDigest md;
        private final byte[] fileBytes = new byte[1024];
        private final byte[] convertBytes = new byte[1024];
        private boolean convertLineEndings;

        public void reset(boolean convertLineEndings) throws NoSuchAlgorithmException {
            if (this.md == null) {
                this.md = MessageDigest.getInstance("MD5");
            }
            this.convertLineEndings = convertLineEndings;
            this.md.reset();
        }

        public void addFile(String relPath, String absPath) throws NoSuchAlgorithmException {
            if (this.md == null) {
                this.md = MessageDigest.getInstance("MD5");
            }
            try (FileInputStream fis = new FileInputStream(absPath);){
                int count;
                GroupOfFiles.addFile(relPath, absPath);
                while ((count = fis.read(this.fileBytes)) != -1) {
                    if (this.convertLineEndings) {
                        boolean trailingCRLF = false;
                        int convertCnt = 0;
                        for (int p = 0; p < count - 1; ++p) {
                            if (this.fileBytes[p] == 13 && this.fileBytes[p + 1] == 10) {
                                this.convertBytes[convertCnt++] = 10;
                                trailingCRLF = true;
                                continue;
                            }
                            trailingCRLF = false;
                            this.convertBytes[convertCnt++] = this.fileBytes[p];
                        }
                        if (!trailingCRLF) {
                            this.convertBytes[convertCnt++] = this.fileBytes[count - 1];
                        }
                        this.md.update(this.convertBytes, 0, convertCnt);
                        GroupOfFiles.updateFile(this.convertBytes, convertCnt);
                        continue;
                    }
                    this.md.update(this.fileBytes, 0, count);
                    GroupOfFiles.updateFile(this.fileBytes, count);
                }
                GroupOfFiles.endFile();
            }
            catch (Exception ex) {
                DebugLog.General.printException(ex, "absPath:" + absPath, LogSeverity.Error);
            }
        }

        public String checksumToString() {
            byte[] mdbytes = this.md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; ++i) {
                sb.append(Integer.toString((mdbytes[i] & 0xFF) + 256, 16).substring(1));
            }
            return sb.toString();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            for (GroupOfFiles group : GroupOfFiles.groups) {
                String message = group.toString();
                stringBuilder.append("\n").append(message);
                if (!GameClient.client) continue;
                ChecksumPacket.sendError(GameClient.connection, message);
            }
            return stringBuilder.toString();
        }
    }

    public static final class Comparer {
        public static final short NUM_GROUPS_TO_SEND = 10;
        public State state = State.Init;
        public short currentIndex;
        public String error;

        public void beginCompare() {
            this.error = null;
            ChecksumPacket.sendTotalChecksum();
        }

        private void gc() {
            GroupOfFiles.gc();
        }

        public void update() {
            switch (this.state.ordinal()) {
                case 0: 
                case 1: 
                case 2: 
                case 3: {
                    break;
                }
                case 4: {
                    this.gc();
                    GameClient.checksumValid = true;
                    break;
                }
                case 5: {
                    this.gc();
                    GameClient.connection.forceDisconnect("checksum-" + this.error);
                    GameWindow.serverDisconnected = true;
                    GameWindow.kickReason = this.error;
                }
            }
        }

        public static enum State {
            Init,
            SentTotalChecksum,
            SentGroupChecksum,
            SentFileChecksums,
            Success,
            Failed;

        }
    }

    public static final class GroupOfFiles {
        public static final int MAX_FILES = 20;
        static MessageDigest mdTotal;
        static MessageDigest mdCurrentFile;
        public static final ArrayList<GroupOfFiles> groups;
        static GroupOfFiles currentGroup;
        public byte[] totalChecksum;
        public short fileCount;
        public final String[] relPaths = new String[20];
        final String[] absPaths = new String[20];
        public final byte[][] checksums = new byte[20][];

        private GroupOfFiles() throws NoSuchAlgorithmException {
            if (mdTotal == null) {
                mdTotal = MessageDigest.getInstance("MD5");
                mdCurrentFile = MessageDigest.getInstance("MD5");
            }
            mdTotal.reset();
            groups.add(this);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder().append(this.fileCount).append(" files, ").append(this.absPaths.length).append("/").append(this.relPaths.length).append("/").append(this.checksums.length).append(" \"").append(Hex.encodeHexString(this.totalChecksum)).append("\"");
            for (int i = 0; i < 20; ++i) {
                stringBuilder.append("\n");
                if (i < this.relPaths.length) {
                    stringBuilder.append(" \"").append(this.relPaths[i]).append("\"");
                }
                if (i < this.checksums.length) {
                    if (this.checksums[i] == null) {
                        stringBuilder.append(" \"\"");
                    } else {
                        stringBuilder.append(" \"").append(Hex.encodeHexString(this.checksums[i])).append("\"");
                    }
                }
                if (i >= this.absPaths.length) continue;
                stringBuilder.append(" \"").append(this.absPaths[i]).append("\"");
            }
            return stringBuilder.toString();
        }

        private void gc_() {
            Arrays.fill(this.relPaths, null);
            Arrays.fill(this.absPaths, null);
            Arrays.fill((Object[])this.checksums, null);
        }

        public static void initChecksum() {
            groups.clear();
            currentGroup = null;
        }

        public static void finishChecksum() {
            if (currentGroup != null) {
                GroupOfFiles.currentGroup.totalChecksum = mdTotal.digest();
                currentGroup = null;
            }
        }

        private static void addFile(String relPath, String absPath) throws NoSuchAlgorithmException {
            if (currentGroup == null) {
                currentGroup = new GroupOfFiles();
            }
            GroupOfFiles.currentGroup.relPaths[GroupOfFiles.currentGroup.fileCount] = relPath;
            GroupOfFiles.currentGroup.absPaths[GroupOfFiles.currentGroup.fileCount] = absPath;
            mdCurrentFile.reset();
        }

        private static void updateFile(byte[] data, int count) {
            mdCurrentFile.update(data, 0, count);
            mdTotal.update(data, 0, count);
        }

        private static void endFile() {
            GroupOfFiles.currentGroup.checksums[GroupOfFiles.currentGroup.fileCount] = mdCurrentFile.digest();
            GroupOfFiles.currentGroup.fileCount = (short)(GroupOfFiles.currentGroup.fileCount + 1);
            if (GroupOfFiles.currentGroup.fileCount >= 20) {
                GroupOfFiles.currentGroup.totalChecksum = mdTotal.digest();
                currentGroup = null;
            }
        }

        public static void gc() {
            for (GroupOfFiles group : groups) {
                group.gc_();
            }
            groups.clear();
        }

        static {
            groups = new ArrayList();
        }
    }
}

