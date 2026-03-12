/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.debug.DebugLog;
import zombie.network.packets.actions.StatePacket;

public class StateManager {
    private static IsoGameCharacter getCharacter(IAnimatable owner) {
        IsoPlayer player;
        IsoPlayer character = null;
        if (owner instanceof IsoPlayer && (player = (IsoPlayer)owner).isLocalPlayer() && !player.isAnimal()) {
            character = player;
        }
        return character;
    }

    public static void enterState(IAnimatable owner, ActionState actionState) {
        State state;
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && (state = character.tryGetAIState(actionState.getName())) != null && state.isSyncOnEnter()) {
            DebugLog.Multiplayer.trace("action state enter %s", actionState.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Enter});
            character.getNetworkCharacterAI().getState().updateEnterState(packet);
        }
    }

    public static void enterSubState(IAnimatable owner, ActionState actionState) {
        State state;
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && (state = character.tryGetAIState(actionState.getName())) != null && state.isSyncOnEnter()) {
            DebugLog.Multiplayer.trace("action state enter %s", actionState.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Enter});
            character.getNetworkCharacterAI().getState().updateEnterSubState(packet);
        }
    }

    public static void exitState(IAnimatable owner, ActionState actionState) {
        State state;
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && (state = character.tryGetAIState(actionState.getName())) != null && state.isSyncOnExit()) {
            DebugLog.Multiplayer.trace("action state exit %s", actionState.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Exit});
            character.getNetworkCharacterAI().getState().updateExitState(packet);
        }
    }

    public static void exitSubState(IAnimatable owner, ActionState actionState) {
        State state;
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && (state = character.tryGetAIState(actionState.getName())) != null && state.isSyncOnExit()) {
            DebugLog.Multiplayer.trace("action state exit %s", actionState.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Exit});
            character.getNetworkCharacterAI().getState().updateExitSubState(packet);
        }
    }

    public static void enterState(IsoGameCharacter owner, State state) {
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && state != null && state.isSyncOnEnter()) {
            DebugLog.Multiplayer.trace("state enter %s", state.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Enter});
            character.getNetworkCharacterAI().getState().updateEnterState(packet);
        }
    }

    public static void enterSubState(IsoGameCharacter owner, State state) {
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && state != null && state.isSyncOnEnter()) {
            DebugLog.Multiplayer.trace("state enter %s", state.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Enter});
            character.getNetworkCharacterAI().getState().updateEnterSubState(packet);
        }
    }

    public static void exitState(IsoGameCharacter owner, State state) {
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && state != null && state.isSyncOnExit()) {
            DebugLog.Multiplayer.trace("state exit %s", state.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Exit});
            character.getNetworkCharacterAI().getState().updateExitState(packet);
        }
    }

    public static void exitSubState(IsoGameCharacter owner, State state) {
        IsoGameCharacter character = StateManager.getCharacter(owner);
        if (character != null && state != null && state.isSyncOnExit()) {
            DebugLog.Multiplayer.trace("state exit %s", state.getName());
            StatePacket packet = new StatePacket();
            packet.setData(new Object[]{character, state, State.Stage.Exit});
            character.getNetworkCharacterAI().getState().updateExitSubState(packet);
        }
    }
}

