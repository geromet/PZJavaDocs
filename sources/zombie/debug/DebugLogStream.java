/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.PrintStream;
import java.util.HashSet;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.IDebugLogFormatter;
import zombie.debug.LogSeverity;
import zombie.debug.StackTraceContainer;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class DebugLogStream
extends PrintStream {
    private LogSeverity logSeverity;
    private final PrintStream wrappedStream;
    private final PrintStream wrappedWarnStream;
    private final PrintStream wrappedErrStream;
    private final IDebugLogFormatter formatter;
    private static final int LEFT_JUSTIFY = 36;
    private final HashSet<String> debugOnceHashSet = new HashSet();

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter) {
        this(out, warn, err, formatter, LogSeverity.Off);
    }

    public DebugLogStream(PrintStream out, PrintStream warn, PrintStream err, IDebugLogFormatter formatter, LogSeverity logSeverity) {
        super(out);
        this.wrappedStream = out;
        this.wrappedWarnStream = warn;
        this.wrappedErrStream = err;
        this.formatter = formatter;
        this.logSeverity = logSeverity;
    }

    public void setLogSeverity(LogSeverity newSeverity) {
        this.logSeverity = newSeverity;
    }

    public LogSeverity getLogSeverity() {
        return this.logSeverity;
    }

    public PrintStream getWrappedOutStream() {
        return this.wrappedStream;
    }

    public PrintStream getWrappedWarnStream() {
        return this.wrappedWarnStream;
    }

    public PrintStream getWrappedErrStream() {
        return this.wrappedErrStream;
    }

    public IDebugLogFormatter getFormatter() {
        return this.formatter;
    }

    protected void write(PrintStream out, LogSeverity logSeverity, String text) {
        String formattedString;
        if (this.isLogEnabled(logSeverity) && (formattedString = this.formatter.format(logSeverity, "", true, text)) != null) {
            out.print(formattedString);
            DebugLog.echoToLogFiles(logSeverity, formattedString);
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String formatNoParams) {
        String formattedString;
        if (this.isLogEnabled(logSeverity) && (formattedString = this.formatter.format(logSeverity, "", true, formatNoParams)) != null) {
            out.println(formattedString);
            DebugLog.echoToLogFiles(logSeverity, formattedString);
        }
    }

    protected void writeln(PrintStream out, LogSeverity logSeverity, String format, Object ... params) {
        String formattedString;
        if (this.isLogEnabled(logSeverity) && (formattedString = this.formatter.format(logSeverity, "", true, format, params)) != null) {
            out.println(formattedString);
            DebugLog.echoToLogFiles(logSeverity, formattedString);
        }
    }

    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, Object formatNoParams) {
        if (this.isLogEnabled(logSeverity)) {
            String callerAffix = DebugLogStream.generateCallerPrefix_Internal(backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> ");
            String formattedString = this.formatter.format(logSeverity, callerAffix, allowRepeat, "%s", formatNoParams);
            if (!allowRepeat) {
                if (this.debugOnceHashSet.contains(callerAffix)) {
                    return;
                }
                this.debugOnceHashSet.add(callerAffix);
            }
            if (formattedString != null) {
                out.println(formattedString);
                DebugLog.echoToLogFiles(logSeverity, formattedString);
            }
        }
    }

    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, String format, Object ... params) {
        String formattedOutputStr;
        String callerAffix;
        String formattedString;
        if (this.isLogEnabled(logSeverity) && (formattedString = this.formatter.format(logSeverity, callerAffix = DebugLogStream.generateCallerPrefix_Internal(backTraceOffset, 36, DebugLog.isLogTraceFileLocationEnabled(), "> "), allowRepeat, formattedOutputStr = String.format(format, params))) != null) {
            out.println(formattedString);
            DebugLog.echoToLogFiles(logSeverity, formattedString);
        }
    }

    private void writeln(PrintStream out, String formatNoParams) {
        this.writeln(out, LogSeverity.General, formatNoParams);
    }

    private void writeln(PrintStream out, String format, Object ... params) {
        this.writeln(out, LogSeverity.General, format, params);
    }

    public static String generateCallerPrefix() {
        return DebugLogStream.generateCallerPrefix_Internal(1, 0, DebugLog.isLogTraceFileLocationEnabled(), "");
    }

    private static String generateCallerPrefix_Internal(int backTraceOffset, int leftJustify, boolean includeLogTraceFileLocation, String suffix) {
        StackTraceElement stackTraceElement = DebugLogStream.tryGetCallerTraceElement(4 + backTraceOffset);
        if (stackTraceElement == null) {
            return StringUtils.leftJustify("(UnknownStack)", leftJustify) + suffix;
        }
        String stackTraceElementString = DebugLogStream.getStackTraceElementString(stackTraceElement, includeLogTraceFileLocation);
        if (leftJustify <= 0) {
            return stackTraceElementString + suffix;
        }
        return StringUtils.leftJustify(stackTraceElementString, leftJustify) + suffix;
    }

    public static StackTraceElement tryGetCallerTraceElement(int depthIdx) {
        try {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            if (stackTraceElements.length <= depthIdx) {
                return null;
            }
            return stackTraceElements[depthIdx];
        }
        catch (SecurityException secEx) {
            return null;
        }
    }

    public static String getStackTraceElementString(StackTraceElement stackTraceElement, boolean includeLogTraceFileLocation) {
        String comment;
        if (stackTraceElement == null) {
            return "(UnknownStack)";
        }
        String classNameOnly = DebugLogStream.getUnqualifiedClassName(stackTraceElement.getClassName());
        String methodName = stackTraceElement.getMethodName();
        if (stackTraceElement.isNativeMethod()) {
            comment = " (Native Method)";
        } else if (includeLogTraceFileLocation) {
            int lineNo = stackTraceElement.getLineNumber();
            String fileName = stackTraceElement.getFileName();
            comment = String.format("(%s:%d)", fileName, lineNo);
        } else {
            comment = "";
        }
        return classNameOnly + "." + methodName + comment;
    }

    public static String getTopStackTraceString(Throwable ex) {
        if (ex == null) {
            return "Null Exception";
        }
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "No Stack Trace Available";
        }
        StackTraceElement topElement = stackTrace[0];
        return DebugLogStream.getStackTraceElementString(topElement, true);
    }

    public void printStackTrace(LogSeverity severity, int depthStart, int depthCount, String messageFormat, Object ... params) {
        if (!this.isLogEnabled(severity)) {
            return;
        }
        PrintStream outStream = this.getPrintStream(severity);
        if (messageFormat != null) {
            String message = !PZArrayUtil.isNullOrEmpty(params) ? String.format(messageFormat, params) : messageFormat;
            outStream.println(message);
            DebugLog.echoExceptionLineToLogFiles(LogSeverity.Error, "StackTraceMessage", message);
        }
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String stackTraceString = StackTraceContainer.getStackTraceString(stackTraceElements, "\t", depthStart + 2, depthCount);
        outStream.println(stackTraceString);
        DebugLog.echoExceptionLineToLogFiles(LogSeverity.Error, "StackTrace", stackTraceString);
    }

    private PrintStream getPrintStream(LogSeverity severity) {
        return switch (severity) {
            case LogSeverity.Trace, LogSeverity.Noise, LogSeverity.Debug, LogSeverity.General -> this.wrappedStream;
            case LogSeverity.Warning -> this.wrappedWarnStream;
            case LogSeverity.Error -> this.wrappedErrStream;
            default -> {
                this.error("Unhandled LogSeverity: %s. Defaulted to Error.", String.valueOf((Object)severity));
                yield this.wrappedErrStream;
            }
        };
    }

    private static String getUnqualifiedClassName(String className) {
        String classNameOnly = className;
        int lastIndexOf = className.lastIndexOf(46);
        if (lastIndexOf > -1 && lastIndexOf < className.length() - 1) {
            classNameOnly = className.substring(lastIndexOf + 1);
        }
        return classNameOnly;
    }

    public boolean isEnabled() {
        return this.getLogSeverity() != LogSeverity.Off;
    }

    public boolean isLogEnabled(LogSeverity logSeverity) {
        return this.isEnabled() && logSeverity.ordinal() >= this.getLogSeverity().ordinal();
    }

    public void trace(Object formatNoParams) {
        this.trace(1, formatNoParams);
    }

    public void trace(String format, Object ... params) {
        this.trace(1, format, params);
    }

    public void debugln(Object formatNoParams) {
        this.debugln(1, formatNoParams);
    }

    public void debugln(String format, Object ... params) {
        this.debugln(1, format, params);
    }

    public void debugOnceln(Object formatNoParams) {
        this.debugOnceln(1, formatNoParams);
    }

    public void debugOnceln(String format, Object ... params) {
        this.debugOnceln(1, format, params);
    }

    public void noise(Object formatNoParams) {
        this.noise(1, formatNoParams);
    }

    public void noise(String format, Object ... params) {
        this.noise(1, format, params);
    }

    public void warn(Object formatNoParams) {
        this.warn(1, formatNoParams);
    }

    public void warn(String format, Object ... params) {
        this.warn(1, format, params);
    }

    public void warnOnce(Object formatNoParams) {
        this.warnOnce(1, formatNoParams);
    }

    public void warnOnce(String format, Object ... params) {
        this.warnOnce(1, format, params);
    }

    public void error(Object formatNoParams) {
        this.error(1, formatNoParams);
    }

    public void error(String format, Object ... params) {
        this.error(1, format, params);
    }

    public void debugln(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, true, formatNoParams);
        }
    }

    public void debugln(int backTraceOffset, String format, Object ... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, true, format, params);
        }
    }

    public void debugOnceln(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, false, formatNoParams);
        }
    }

    public void debugOnceln(int backTraceOffset, String format, Object ... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Debug, backTraceOffset + 1, false, format, params);
        }
    }

    public void noise(int backTraceOffset, Object formatNoParams) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, backTraceOffset + 1, true, formatNoParams);
        }
    }

    public void noise(int backTraceOffset, String format, Object ... params) {
        if (Core.debug) {
            this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Noise, backTraceOffset + 1, true, format, params);
        }
    }

    public void warn(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, true, formatNoParams);
    }

    public void warn(int backTraceOffset, String format, Object ... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, true, format, params);
    }

    public void error(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, backTraceOffset + 1, true, formatNoParams);
    }

    public void error(int backTraceOffset, String format, Object ... params) {
        this.writeWithCallerPrefixln(this.wrappedErrStream, LogSeverity.Error, backTraceOffset + 1, true, format, params);
    }

    public void trace(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, backTraceOffset + 1, true, formatNoParams);
    }

    public void trace(int backTraceOffset, String format, Object ... params) {
        this.writeWithCallerPrefixln(this.wrappedStream, LogSeverity.Trace, backTraceOffset + 1, true, format, params);
    }

    public void warnOnce(int backTraceOffset, Object formatNoParams) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, false, formatNoParams);
    }

    public void warnOnce(int backTraceOffset, String format, Object ... params) {
        this.writeWithCallerPrefixln(this.wrappedWarnStream, LogSeverity.Warning, backTraceOffset + 1, false, format, params);
    }

    @Override
    public void print(boolean b) {
        this.write(this.wrappedStream, LogSeverity.General, b ? "true" : "false");
    }

    @Override
    public void print(char c) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(c));
    }

    @Override
    public void print(int i) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(i));
    }

    @Override
    public void print(long l) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(l));
    }

    @Override
    public void print(float f) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(f));
    }

    @Override
    public void print(double d) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(d));
    }

    @Override
    public void print(String s) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(s));
    }

    @Override
    public void print(Object obj) {
        this.write(this.wrappedStream, LogSeverity.General, String.valueOf(obj));
    }

    @Override
    public PrintStream printf(String format, Object ... args2) {
        this.write(this.wrappedStream, LogSeverity.General, String.format(format, args2));
        return this;
    }

    @Override
    public void println() {
        this.writeln(this.wrappedStream, "");
    }

    @Override
    public void println(boolean x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(char x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(int x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(long x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(float x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(double x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        this.writeln(this.wrappedStream, "%s", String.valueOf(x));
    }

    @Override
    public void println(String x) {
        this.writeln(this.wrappedStream, x);
    }

    @Override
    public void println(Object x) {
        this.writeln(this.wrappedStream, "%s", x);
    }

    public void println(String format, Object ... params) {
        this.writeln(this.wrappedStream, LogSeverity.General, format, params);
    }

    public void printException(Throwable ex, String errorMessage, LogSeverity severity) {
        this.printException(ex, errorMessage, DebugLogStream.generateCallerPrefix(), severity);
    }

    public void printException(Throwable ex, String errorMessage, String callerPrefix, LogSeverity severity) {
        if (ex == null) {
            this.warn("Null exception passed.");
            return;
        }
        if (!this.isLogEnabled(severity)) {
            return;
        }
        PrintStream outStream = this.getPrintStream(severity);
        boolean includeStack = this.shouldIncludeStackTrace(severity);
        if (includeStack) {
            StringBuilder sb = new StringBuilder();
            if (errorMessage != null) {
                sb.append(String.format("%s> Exception thrown%s\t%s at %s. Message: %s", callerPrefix, System.lineSeparator(), ex, DebugLogStream.getTopStackTraceString(ex), errorMessage));
            } else {
                sb.append(String.format("%s> Exception thrown%s\t%s at %s.", callerPrefix, System.lineSeparator(), ex, DebugLogStream.getTopStackTraceString(ex)));
            }
            sb.append(System.lineSeparator());
            StackTraceContainer.getStackTraceString(sb, ex, "Stack trace:", "\t", 0, -1);
            this.write(outStream, severity, sb.toString());
        } else if (errorMessage != null) {
            String message = String.format("%s> Exception thrown %s at %s. Message: %s", callerPrefix, ex, DebugLogStream.getTopStackTraceString(ex), errorMessage);
            this.writeln(outStream, severity, message);
        } else {
            String message = String.format("%s> Exception thrown %s at %s.", callerPrefix, ex, DebugLogStream.getTopStackTraceString(ex));
            this.writeln(outStream, severity, message);
        }
    }

    private boolean shouldIncludeStackTrace(LogSeverity severity) {
        return switch (severity) {
            case LogSeverity.Trace, LogSeverity.Noise, LogSeverity.General, LogSeverity.Warning -> false;
            default -> true;
        };
    }

    public void printException(Throwable ex, LogSeverity severity, String callerPrefix, String errorMessageFormat, Object ... params) {
        if (!this.isLogEnabled(severity)) {
            return;
        }
        String errorMessage = String.format(errorMessageFormat, params);
        this.printException(ex, errorMessage, callerPrefix, severity);
    }
}

