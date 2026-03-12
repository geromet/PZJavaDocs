/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import zombie.gameStates.GameStateMachine;

public class GameState {
    public void enter() {
    }

    public void exit() {
    }

    public void render() {
    }

    public GameState redirectState() {
        return null;
    }

    public GameStateMachine.StateAction update() {
        return GameStateMachine.StateAction.Continue;
    }

    public void yield() {
    }

    public void reenter() {
    }
}

