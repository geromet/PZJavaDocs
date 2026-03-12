/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.server;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.core.NetTimedAction;
import zombie.debug.DebugLog;

public class AnimEventEmulator {
    private static final AnimEventEmulator instance = new AnimEventEmulator();
    private final ArrayList<AnimEvent> events = new ArrayList();

    public static AnimEventEmulator getInstance() {
        return instance;
    }

    private AnimEventEmulator() {
    }

    public long getDurationMax() {
        return 1800000L;
    }

    public void create(NetTimedAction action, long duration, boolean isOnce, String event, String parameter) {
        DebugLog.Action.debugln("%s %s (%s) %s %d ms", action.type, event, parameter, isOnce ? "after" : "every", duration);
        this.events.add(new AnimEvent(action, duration, isOnce, event, parameter));
    }

    public void remove(NetTimedAction action) {
        this.events.removeIf(event -> event.action == action);
    }

    public void update() {
        long time = GameTime.getServerTimeMills();
        for (AnimEvent event2 : this.events) {
            if (event2.action == null || time < event2.time + event2.duration) continue;
            event2.action.animEvent(event2.event, event2.parameter);
            if (event2.isOnce) {
                event2.time = event2.start + this.getDurationMax();
                continue;
            }
            event2.time = GameTime.getServerTimeMills();
        }
        this.events.removeIf(event -> time >= event.start + this.getDurationMax());
    }

    public static class AnimEvent {
        private final NetTimedAction action;
        private final String event;
        private final String parameter;
        private final long start;
        private final long duration;
        private final boolean isOnce;
        private long time;

        private AnimEvent(NetTimedAction action, long duration, boolean isOnce, String event, String parameter) {
            long time = GameTime.getServerTimeMills();
            this.action = action;
            this.start = time;
            this.time = time;
            this.duration = duration;
            this.isOnce = isOnce;
            this.event = event;
            this.parameter = parameter;
        }
    }
}

