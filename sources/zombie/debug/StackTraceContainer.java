/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class StackTraceContainer {
    private final int depthStart;
    private final int depthCount;
    private final StackTraceElement[] stackTraceElements;
    private final String indent;

    public StackTraceContainer(StackTraceElement[] stackTraceElements, String indent, int depthStart, int depthCount) {
        this.depthStart = Math.max(depthStart, 0);
        this.depthCount = depthCount;
        this.stackTraceElements = stackTraceElements;
        this.indent = indent;
    }

    public String toString() {
        return StackTraceContainer.getStackTraceString(this.stackTraceElements, this.indent, this.depthStart, this.depthCount);
    }

    public static String getStackTraceString(StackTraceElement[] stackTraceElements, String indent, int depthStart, int depthCount) {
        StringBuilder result = new StringBuilder();
        depthCount = depthCount <= 0 ? stackTraceElements.length : depthCount;
        int numElements = 0;
        for (int d = depthStart; numElements < depthCount && d < stackTraceElements.length; ++d) {
            StackTraceElement stackTraceElement = stackTraceElements[d];
            String stackTraceElementString = stackTraceElement.toString();
            if (numElements > 0 && stackTraceElementString.startsWith("zombie.core.profiling.PerformanceProbes$")) continue;
            if (numElements > 0) {
                result.append(System.lineSeparator());
            }
            result.append(indent).append(stackTraceElementString);
            ++numElements;
        }
        return result.toString();
    }

    public static StringBuilder getStackTraceString(StringBuilder result, Throwable throwable, String indent, int depthStart, int depthCount) {
        StackTraceElement[] trace = throwable.getStackTrace();
        depthCount = depthCount <= 0 ? trace.length : depthCount;
        int numElements = 0;
        for (int d = depthStart; numElements < depthCount && d < trace.length; ++d) {
            StackTraceElement stackTraceElement = trace[d];
            String stackTraceElementString = stackTraceElement.toString();
            if (numElements > 0 && stackTraceElementString.startsWith("zombie.core.profiling.PerformanceProbes$")) continue;
            result.append(indent).append(stackTraceElementString).append(System.lineSeparator());
            ++numElements;
        }
        return result;
    }

    public static StringBuilder getStackTraceString(StringBuilder result, Throwable throwable, String caption, String prefix, int depthStart, int depthCount) {
        Set<Throwable> done = Collections.newSetFromMap(new IdentityHashMap());
        done.add(throwable);
        if (caption != null) {
            result.append(prefix).append(caption).append(System.lineSeparator());
        }
        StackTraceContainer.getStackTraceString(result, throwable, prefix + "\t", depthStart, depthCount);
        StackTraceElement[] trace = throwable.getStackTrace();
        for (Throwable se : throwable.getSuppressed()) {
            StackTraceContainer.getEnclosedStackTraceString(result, trace, "Suppressed: ", prefix, se, 0, -1, done);
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            StackTraceContainer.getEnclosedStackTraceString(result, trace, "Caused by: ", prefix, cause, 0, -1, done);
        }
        return result;
    }

    public static StringBuilder getEnclosedStackTraceString(StringBuilder result, StackTraceElement[] enclosingTrace, String caption, String prefix, Throwable throwable, int depthStart, int depthCount, Set<Throwable> done) {
        if (done.contains(throwable)) {
            result.append(prefix).append(caption).append("[CIRCULAR REFERENCE: ").append(throwable).append("]");
            return result;
        }
        done.add(throwable);
        StackTraceElement[] trace = throwable.getStackTrace();
        int m = trace.length - 1;
        for (int n = enclosingTrace.length - 1; m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n]); --m, --n) {
        }
        int framesInCommon = trace.length - 1 - m;
        result.append(prefix).append(caption).append(throwable).append(System.lineSeparator());
        StackTraceContainer.getStackTraceString(result, throwable, prefix + "\t", 0, m + 1);
        if (framesInCommon != 0) {
            result.append(prefix).append("\t").append("... ").append(framesInCommon).append(" more").append(System.lineSeparator());
        }
        for (Throwable se : throwable.getSuppressed()) {
            StackTraceContainer.getEnclosedStackTraceString(result, trace, "Suppressed: ", prefix, se, 0, -1, done);
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            StackTraceContainer.getEnclosedStackTraceString(result, trace, "Caused by: ", prefix, cause, 0, -1, done);
        }
        return result;
    }
}

