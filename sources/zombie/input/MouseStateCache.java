/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import zombie.input.MouseState;

public final class MouseStateCache {
    private final Object lock = "MouseStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final MouseState[] states = new MouseState[]{new MouseState(), new MouseState()};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void poll() {
        Object object = this.lock;
        synchronized (object) {
            MouseState statePolling = this.getStatePolling();
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
    public MouseState getState() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexUsing];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private MouseState getStatePolling() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexPolling];
        }
    }
}

