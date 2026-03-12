/*
 * Decompiled with CFR 0.152.
 */
package zombie.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.config.ArrayConfigOption;
import zombie.config.ConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;

public final class ConfigFile {
    protected ArrayList<ConfigOption> options;
    protected int version;
    protected String versionString = "Version";
    protected boolean writeTooltips = true;

    private void fileError(String fileName, int lineNumber, String message) {
        DebugLog.log(fileName + ":" + lineNumber + " " + message);
    }

    public boolean read(String fileName) {
        this.options = new ArrayList();
        this.version = 0;
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        }
        DebugLog.DetailedInfo.trace("reading " + fileName);
        try (FileReader fr = new FileReader(file);
             BufferedReader r = new BufferedReader(fr);){
            String line;
            int lineNumber = 0;
            while ((line = r.readLine()) != null) {
                ++lineNumber;
                if ((line = line.trim()).isEmpty() || line.startsWith("#")) continue;
                if (!line.contains("=")) {
                    this.fileError(fileName, lineNumber, line);
                    continue;
                }
                String[] ss = line.split("=");
                if (this.versionString.equals(ss[0])) {
                    try {
                        this.version = Integer.parseInt(ss[1]);
                    }
                    catch (NumberFormatException ex) {
                        this.fileError(fileName, lineNumber, "expected version number, got \"" + ss[1] + "\"");
                    }
                    continue;
                }
                StringConfigOption option = new StringConfigOption(ss[0], ss.length > 1 ? ss[1] : "", -1);
                this.options.add(option);
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    public boolean write(String fileName, int version, ArrayList<? extends ConfigOption> options) {
        File file = new File(fileName);
        DebugLog.DetailedInfo.trace("writing " + fileName);
        try (FileWriter fw = new FileWriter(file, false);){
            if (version != 0) {
                fw.write(this.versionString + "=" + version + System.lineSeparator());
            }
            Object lineSeparator = System.lineSeparator();
            if (this.writeTooltips) {
                lineSeparator = (String)lineSeparator + System.lineSeparator();
            }
            for (int i = 0; i < options.size(); ++i) {
                ArrayConfigOption arrayConfigOption;
                ConfigOption option = options.get(i);
                if (this.writeTooltips) {
                    String tooltip = option.getTooltip();
                    if (tooltip != null) {
                        tooltip = tooltip.replace("\\n", " ").replace("\\\"", "\"");
                        tooltip = tooltip.replaceAll("\n", System.lineSeparator() + "# ");
                        fw.write("# " + tooltip + System.lineSeparator());
                    }
                    if (option instanceof SandboxOptions.EnumSandboxOption) {
                        SandboxOptions.EnumSandboxOption enumOption = (SandboxOptions.EnumSandboxOption)option;
                        for (int j = 1; j <= enumOption.getNumValues(); ++j) {
                            try {
                                String subOptionTranslated = enumOption.getValueTranslationByIndexOrNull(j);
                                if (subOptionTranslated == null) continue;
                                fw.write("    -- " + j + " = " + subOptionTranslated.replace("\\\"", "\"") + System.lineSeparator());
                                continue;
                            }
                            catch (Exception ex) {
                                ExceptionLogger.logException(ex);
                            }
                        }
                    }
                }
                if (option instanceof ArrayConfigOption && (arrayConfigOption = (ArrayConfigOption)option).isMultiLine()) {
                    for (int j = 0; j < arrayConfigOption.size(); ++j) {
                        ConfigOption element = arrayConfigOption.getElement(j);
                        fw.write(option.getName() + "=" + element.getValueAsString());
                        if (j >= arrayConfigOption.size() - 1 && i >= options.size() - 1) continue;
                        fw.write((String)lineSeparator);
                    }
                    continue;
                }
                fw.write(option.getName() + "=" + option.getValueAsString() + (String)(i < options.size() - 1 ? lineSeparator : ""));
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
            return false;
        }
        return true;
    }

    public ArrayList<ConfigOption> getOptions() {
        return this.options;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersionString(String str) {
        this.versionString = str;
    }

    public void setWriteTooltips(boolean bWrite) {
        this.writeTooltips = bWrite;
    }
}

