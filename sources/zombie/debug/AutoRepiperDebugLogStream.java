/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.io.PrintStream;
import java.util.Locale;
import zombie.debug.DebugLogStream;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class AutoRepiperDebugLogStream
extends DebugLogStream {
    private final DebugType defaultDebugType;

    public AutoRepiperDebugLogStream(DebugType defaultOut, DebugType defaultDebugType, LogSeverity logSeverity) {
        super(defaultOut.getLogStream(), null, null, null, logSeverity);
        this.defaultDebugType = defaultDebugType;
    }

    public DebugType getDefaultDebugType() {
        return this.defaultDebugType;
    }

    public RepiperPacket parseRepiper(Object object, LogSeverity defaultLogSeverity) {
        RepiperPacket repiper = RepiperPacket.alloc(object, defaultLogSeverity, this.getDefaultDebugType());
        this.parseRepipeDirection(repiper);
        this.parseRepipedLogSeverity(repiper);
        return repiper;
    }

    protected void parseRepipeDirection(RepiperPacket repiper) {
        Object object = repiper.getParsedObject();
        if (!(object instanceof String)) {
            return;
        }
        String text = (String)object;
        int indexOfColon = text.indexOf(58);
        if (indexOfColon <= 0) {
            return;
        }
        String debugTypeStr = text.substring(0, indexOfColon);
        if (debugTypeStr.indexOf(10) > -1 || debugTypeStr.indexOf(32) > -1 || debugTypeStr.indexOf(9) > -1) {
            return;
        }
        for (DebugType debugType : DebugType.values()) {
            if (!debugType.name().equalsIgnoreCase(debugTypeStr)) continue;
            repiper.repipeDirection = debugType;
            repiper.parsedText = text.substring(indexOfColon + 1);
            break;
        }
    }

    protected void parseRepipedLogSeverity(RepiperPacket repiper) {
        int indexOfColon;
        Object object = repiper.getParsedObject();
        if (!(object instanceof String)) {
            return;
        }
        String text = (String)object;
        int startAt = 0;
        for (int i = 0; i < 2 && (indexOfColon = text.indexOf(58, startAt)) > 0; ++i) {
            String logSeverityStr = text.substring(startAt, indexOfColon);
            LogSeverity parsedLogSeverity = this.parseRepipedLogSeverityExact(logSeverityStr);
            if (parsedLogSeverity != null) {
                repiper.logSeverity = parsedLogSeverity;
                repiper.parsedText = text.substring(indexOfColon + 1);
                return;
            }
            startAt = indexOfColon + 1;
        }
    }

    private LogSeverity parseRepipedLogSeverityExact(String logSeverityStr) {
        return switch (logSeverityStr.toUpperCase(Locale.ROOT)) {
            case "TRACE" -> LogSeverity.Trace;
            case "NOISE" -> LogSeverity.Noise;
            case "DEBUG" -> LogSeverity.Debug;
            case "WARN" -> LogSeverity.Warning;
            case "ERROR" -> LogSeverity.Error;
            default -> null;
        };
    }

    protected PrintStream getRepipedStream(PrintStream stream, DebugType repipedTo) {
        return this.getRepipedStream(stream, repipedTo.getLogStream());
    }

    protected PrintStream getRepipedStream(PrintStream stream, DebugLogStream repipedTo) {
        if (stream == this.getWrappedOutStream()) {
            return repipedTo.getWrappedOutStream();
        }
        if (stream == this.getWrappedWarnStream()) {
            return repipedTo.getWrappedWarnStream();
        }
        if (stream == this.getWrappedErrStream()) {
            return repipedTo.getWrappedErrStream();
        }
        return repipedTo.getWrappedOutStream();
    }

    @Override
    protected void write(PrintStream out, LogSeverity logSeverity, String text) {
        try (RepiperPacket repiperPacket = this.parseRepiper(text, logSeverity);){
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().write(repipedOutStream, repipedLogSeverity, text);
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity logSeverity, String formatNoParams) {
        try (RepiperPacket repiperPacket = this.parseRepiper(formatNoParams, logSeverity);){
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeln(PrintStream out, LogSeverity logSeverity, String format, Object ... params) {
        try (RepiperPacket repiperPacket = this.parseRepiper(format, logSeverity);){
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeln(repipedOutStream, repipedLogSeverity, repiperPacket.getParsedString(), params);
        }
    }

    @Override
    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, Object formatNoParams) {
        try (RepiperPacket repiperPacket = this.parseRepiper(formatNoParams, logSeverity);){
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, backTraceOffset + 1, allowRepeat, repiperPacket.getParsedString());
        }
    }

    @Override
    protected void writeWithCallerPrefixln(PrintStream out, LogSeverity logSeverity, int backTraceOffset, boolean allowRepeat, String format, Object ... params) {
        try (RepiperPacket repiperPacket = this.parseRepiper(format, logSeverity);){
            DebugType repipedDebugType = repiperPacket.repipeDirection;
            PrintStream repipedOutStream = this.getRepipedStream(out, repipedDebugType);
            LogSeverity repipedLogSeverity = repiperPacket.logSeverity;
            repipedDebugType.getLogStream().writeWithCallerPrefixln(repipedOutStream, repipedLogSeverity, backTraceOffset, allowRepeat, repiperPacket.getParsedString(), params);
        }
    }

    public static class RepiperPacket
    extends PooledObject
    implements AutoCloseable {
        private String parsedText;
        private Object inObject;
        private LogSeverity logSeverity;
        private DebugType repipeDirection;
        private static final Pool<RepiperPacket> s_pool = new Pool<RepiperPacket>(RepiperPacket::new);

        private RepiperPacket() {
        }

        @Override
        public void onReleased() {
            this.parsedText = null;
            this.inObject = null;
            this.logSeverity = null;
            this.repipeDirection = null;
        }

        public Object getParsedObject() {
            if (this.parsedText != null) {
                return this.parsedText;
            }
            return this.inObject;
        }

        public String getParsedString() {
            if (this.parsedText != null) {
                return this.parsedText;
            }
            return String.valueOf(this.inObject);
        }

        public static RepiperPacket alloc(Object object, LogSeverity defaultLogSeverity, DebugType defaultDebugType) {
            RepiperPacket newInstance = s_pool.alloc();
            newInstance.parsedText = null;
            newInstance.inObject = object;
            newInstance.logSeverity = defaultLogSeverity;
            newInstance.repipeDirection = defaultDebugType;
            return newInstance;
        }

        @Override
        public void close() {
            Pool.tryRelease(this);
        }
    }
}

