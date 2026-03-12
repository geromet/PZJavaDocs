/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import zombie.debug.LogSeverity;

public interface IDebugLogFormatter {
    public String format(LogSeverity var1, String var2, boolean var3, String var4);

    public String format(LogSeverity var1, String var2, boolean var3, String var4, Object ... var5);
}

