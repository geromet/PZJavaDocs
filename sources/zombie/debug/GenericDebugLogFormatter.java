/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.IDebugLogFormatter;
import zombie.debug.LogSeverity;

class GenericDebugLogFormatter
implements IDebugLogFormatter {
    private final DebugType debugType;

    public GenericDebugLogFormatter(DebugType debugType) {
        this.debugType = debugType;
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean allowRepeat, String formatNoParams) {
        return DebugLog.formatString(this.debugType, logSeverity, affix, allowRepeat, formatNoParams);
    }

    @Override
    public String format(LogSeverity logSeverity, String affix, boolean allowRepeat, String format, Object ... params) {
        return DebugLog.formatString(this.debugType, logSeverity, affix, allowRepeat, format, params);
    }
}

