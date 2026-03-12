/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import zombie.input.KeyboardState;

public final class KeyboardStateCache {
    private final Object lock = "KeyboardStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final KeyboardState[] states = new KeyboardState[]{new KeyboardState(), new KeyboardState()};

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void poll() {
        Object object = this.lock;
        synchronized (object) {
            KeyboardState statePolling = this.getStatePolling();
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
    public KeyboardState getState() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexUsing];
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public KeyboardState getStatePolling() {
        Object object = this.lock;
        synchronized (object) {
            return this.states[this.stateIndexPolling];
        }
    }
}

