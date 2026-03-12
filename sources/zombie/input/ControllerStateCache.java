/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import zombie.input.ControllerState;

public class ControllerStateCache {
    private final Object lock = "ControllerStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final ControllerState[] states = new ControllerState[]{new ControllerState(), new ControllerState()};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void poll() {
        Object object = this.lock;
        synchronized (object) {
            ControllerState statePolling = this.getStatePolling();
            if (statePolling.wasPolled()) {
                return;
            }
            statePolling.poll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void swap() {
        Object object = this.lock;
        synchronized (object) {
            if (!this.getStatePolling().wasPolled()) {
                return;
            }
            this.stateIndexUsing = this.stateIndexPolling;
            this.stateIndexPolling = this.stateIndexPolling == 1 ? 0 : 1;
            this.getStatePolling().set(this.getState());
            this.getStatePolling().reset();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public ControllerState getState() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexUsing];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private ControllerState getStatePolling() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexPolling];
        }
    }

    public void quit() {
        this.states[0].quit();
        this.states[1].quit();
    }
}

