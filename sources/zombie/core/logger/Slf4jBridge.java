/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.logger;

import java.util.Map;
import java.util.Objects;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class Slf4jBridge
implements SLF4JServiceProvider {
    private static final Map<Level, LogSeverity> LOG_SEVERITY_BY_LEVEL = Map.of(Level.TRACE, LogSeverity.Trace, Level.DEBUG, LogSeverity.Debug, Level.INFO, LogSeverity.General, Level.WARN, LogSeverity.Warning, Level.ERROR, LogSeverity.Error);

    @Override
    public ILoggerFactory getLoggerFactory() {
        return name -> new LegacyAbstractLogger(this){
            final /* synthetic */ Slf4jBridge this$0;
            {
                Slf4jBridge slf4jBridge = this$0;
                Objects.requireNonNull(slf4jBridge);
                this.this$0 = slf4jBridge;
            }

            @Override
            public boolean isTraceEnabled() {
                return DebugLog.General.isEnabled(LogSeverity.Trace);
            }

            @Override
            public boolean isDebugEnabled() {
                return DebugLog.General.isEnabled(LogSeverity.Debug);
            }

            @Override
            public boolean isInfoEnabled() {
                return DebugLog.General.isEnabled(LogSeverity.General);
            }

            @Override
            public boolean isWarnEnabled() {
                return DebugLog.General.isEnabled(LogSeverity.Warning);
            }

            @Override
            public boolean isErrorEnabled() {
                return DebugLog.General.isEnabled(LogSeverity.Error);
            }

            @Override
            protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] args2, Throwable t) {
                msg = msg.replace("%", "%%").replace("{}", "%s");
                if (t == null) {
                    DebugLog.General.routedWrite(3, LOG_SEVERITY_BY_LEVEL.get((Object)level), msg.formatted(args2));
                } else {
                    DebugLog.General.printException(t, LOG_SEVERITY_BY_LEVEL.get((Object)level), msg, args2);
                }
            }

            @Override
            protected String getFullyQualifiedCallerName() {
                return null;
            }
        };
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.0";
    }

    @Override
    public void initialize() {
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return null;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return null;
    }
}

