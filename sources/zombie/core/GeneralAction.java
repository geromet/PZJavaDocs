/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import zombie.core.Action;

public class GeneralAction
extends Action {
    @Override
    float getDuration() {
        return 0.0f;
    }

    @Override
    void start() {
    }

    @Override
    void stop() {
    }

    @Override
    boolean isValid() {
        return false;
    }

    @Override
    boolean isUsingTimeout() {
        return true;
    }

    @Override
    void update() {
    }

    @Override
    boolean perform() {
        return false;
    }
}

