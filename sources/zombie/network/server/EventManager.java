/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.server;

import java.util.ArrayList;
import zombie.network.server.IEventController;

public class EventManager {
    private static final EventManager instance = new EventManager();
    private static final ArrayList<IEventController> callbacks = new ArrayList();

    private EventManager() {
    }

    public static EventManager instance() {
        return instance;
    }

    public void registerCallback(IEventController controller) {
        if (controller != null) {
            callbacks.add(controller);
        }
    }

    public void report(String event) {
        for (IEventController controller : callbacks) {
            controller.process(event);
        }
    }
}

