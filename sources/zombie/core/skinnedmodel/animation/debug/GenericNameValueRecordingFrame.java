/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.list.PZArrayUtil;

public abstract class GenericNameValueRecordingFrame {
    protected String[] columnNames = new String[0];
    protected final HashMap<String, Integer> nameIndices = new HashMap();
    protected boolean headerDirty;
    protected final String fileKey;
    protected PrintStream outHeader;
    private ByteArrayOutputStream outHeaderByteArrayStream;
    protected PrintStream outHeaderFile;
    protected PrintStream outValues;
    private String headerFilePath;
    private String valuesFilePath;
    protected int firstFrameNumber = -1;
    protected int frameNumber = -1;
    protected static final String delim = ",";
    protected final String valuesFileNameSuffix;
    private String previousLine;
    private int previousFrameNo = -1;
    protected final StringBuilder lineBuffer = new StringBuilder();

    public GenericNameValueRecordingFrame(String fileKey, String valuesFileNameSuffix) {
        this.fileKey = fileKey;
        this.valuesFileNameSuffix = valuesFileNameSuffix;
    }

    protected int addColumnInternal(String name) {
        int animIndex = this.columnNames.length;
        this.columnNames = PZArrayUtil.add(this.columnNames, name);
        this.nameIndices.put(name, animIndex);
        this.headerDirty = true;
        this.onColumnAdded();
        return animIndex;
    }

    public int getOrCreateColumn(String nodeName) {
        if (this.nameIndices.containsKey(nodeName)) {
            return this.nameIndices.get(nodeName);
        }
        return this.addColumnInternal(nodeName);
    }

    public void setFrameNumber(int frameNumber) {
        if (this.frameNumber != frameNumber) {
            this.frameNumber = frameNumber;
            if (this.firstFrameNumber == -1) {
                this.firstFrameNumber = this.frameNumber;
            }
            this.headerDirty = true;
        }
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public String getNameAt(int i) {
        return this.columnNames[i];
    }

    public abstract String getValueAt(int var1);

    protected void openHeader() {
        if (this.outHeader != null) {
            this.outHeader.close();
            this.outHeader = null;
        }
        this.outHeaderByteArrayStream = new ByteArrayOutputStream();
        this.outHeader = new PrintStream((OutputStream)this.outHeaderByteArrayStream, true, StandardCharsets.UTF_8);
    }

    protected void flushHeaderToFile() {
        this.outHeaderFile = AnimationPlayerRecorder.openFileStream(this.fileKey + "_header", false, filePath -> {
            this.headerFilePath = filePath;
        });
        byte[] bytes = this.outHeaderByteArrayStream.toByteArray();
        try {
            this.outHeaderFile.write(bytes);
        }
        catch (IOException ex) {
            DebugType.General.printException(ex, "Exception thrown trying to write recording header file.", LogSeverity.Error);
        }
        this.outHeaderByteArrayStream.reset();
    }

    protected void openValuesFile(boolean append) {
        if (this.outValues != null) {
            this.outValues.close();
            this.outValues = null;
        }
        this.outValues = AnimationPlayerRecorder.openFileStream(this.fileKey + this.valuesFileNameSuffix, append, filePath -> {
            this.valuesFilePath = filePath;
        });
    }

    public void writeLine() {
        if (this.headerDirty || this.outHeader == null) {
            this.headerDirty = false;
            this.writeHeaderToMemory();
            this.flushHeaderToFile();
        }
        this.writeData();
    }

    public void close() {
        if (this.outHeader != null) {
            this.outHeader.close();
            this.outHeader = null;
        }
        if (this.outValues != null) {
            this.outValues.close();
            this.outValues = null;
        }
    }

    public void closeAndDiscard() {
        this.close();
        ZomboidFileSystem.instance.tryDeleteFile(this.headerFilePath);
        this.headerFilePath = null;
        ZomboidFileSystem.instance.tryDeleteFile(this.valuesFilePath);
        this.valuesFilePath = null;
        this.previousLine = null;
        this.previousFrameNo = -1;
    }

    protected abstract void onColumnAdded();

    public abstract void reset();

    protected void writeHeaderToMemory() {
        StringBuilder logLine = new StringBuilder();
        logLine.append("frameNo");
        this.buildHeader(logLine);
        this.openHeader();
        this.outHeader.println(logLine);
        this.outHeader.println(this.firstFrameNumber + delim + this.frameNumber);
    }

    protected void buildHeader(StringBuilder logLine) {
        int columnCount = this.getColumnCount();
        for (int i = 0; i < columnCount; ++i) {
            GenericNameValueRecordingFrame.appendCell(logLine, this.getNameAt(i));
        }
    }

    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }
        StringBuilder logLine = this.lineBuffer;
        logLine.setLength(0);
        this.writeData(logLine);
        if (this.previousLine != null && this.previousLine.contentEquals(logLine)) {
            return;
        }
        this.outValues.print(this.frameNumber);
        this.outValues.println(logLine);
        this.previousLine = logLine.toString();
        this.previousFrameNo = this.frameNumber;
    }

    protected void writeData(StringBuilder logLine) {
        int columnCount = this.getColumnCount();
        for (int i = 0; i < columnCount; ++i) {
            GenericNameValueRecordingFrame.appendCell(logLine, this.getValueAt(i));
        }
    }

    public static StringBuilder appendCell(StringBuilder logLine) {
        return logLine.append(delim);
    }

    public static StringBuilder appendCell(StringBuilder logLine, String cell) {
        return logLine.append(delim).append(cell);
    }

    public static StringBuilder appendCell(StringBuilder logLine, float cell) {
        return logLine.append(delim).append(cell);
    }

    public static StringBuilder appendCell(StringBuilder logLine, int cell) {
        return logLine.append(delim).append(cell);
    }

    public static StringBuilder appendCell(StringBuilder logLine, long cell) {
        return logLine.append(delim).append(cell);
    }

    public static StringBuilder appendCellQuot(StringBuilder logLine, String cell) {
        return logLine.append(delim).append('\"').append(cell).append('\"');
    }
}

