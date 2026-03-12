/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.util.HashMap;
import zombie.util.StringUtils;

public final class ActionContextEvents {
    private Event firstEvent;
    private Event eventPool;

    public synchronized void add(String name, String state) {
        if (this.contains(name, state, false)) {
            return;
        }
        Event event = this.allocEvent();
        event.name = name;
        event.state = state;
        event.next = this.firstEvent;
        this.firstEvent = event;
    }

    public boolean contains(String name, String state) {
        return this.contains(name, state, true);
    }

    public boolean contains(String name, String state, boolean bAgnosticLayer) {
        Event event = this.firstEvent;
        while (event != null) {
            if (event.name.equalsIgnoreCase(name)) {
                if (state == null) {
                    return true;
                }
                if (StringUtils.equalsIgnoreCase(event.state, state)) {
                    return true;
                }
                if (bAgnosticLayer && event.state == null) {
                    return true;
                }
            }
            event = event.next;
        }
        return false;
    }

    public synchronized void clear() {
        if (this.firstEvent == null) {
            return;
        }
        Event last = this.firstEvent;
        while (last.next != null) {
            last = last.next;
        }
        last.next = this.eventPool;
        this.eventPool = this.firstEvent;
        this.firstEvent = null;
    }

    public synchronized void clearEvent(String name) {
        Event prev = null;
        Event event = this.firstEvent;
        while (event != null) {
            Event next = event.next;
            if (event.name.equalsIgnoreCase(name)) {
                this.releaseEvent(event, prev);
            } else {
                prev = event;
            }
            event = next;
        }
    }

    private Event allocEvent() {
        if (this.eventPool == null) {
            return new Event();
        }
        Event event = this.eventPool;
        this.eventPool = event.next;
        return event;
    }

    private void releaseEvent(Event event, Event prev) {
        if (prev == null) {
            assert (event == this.firstEvent);
            this.firstEvent = event.next;
        } else {
            assert (event != this.firstEvent);
            assert (prev.next == event);
            prev.next = event.next;
        }
        event.next = this.eventPool;
        this.eventPool = event;
    }

    public void get(HashMap<String, String> events) {
        Event event = this.firstEvent;
        while (event != null) {
            if (event.state == null) {
                events.put(event.name, event.state);
            }
            event = event.next;
        }
    }

    private static final class Event {
        String state;
        String name;
        Event next;

        private Event() {
        }
    }
}

