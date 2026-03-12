/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import zombie.ai.states.StateManager;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.action.ActionContextEvents;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.characters.action.ActionStateContainer;
import zombie.characters.action.ActionStateSnapshot;
import zombie.characters.action.ActionTransition;
import zombie.characters.action.IActionCondition;
import zombie.characters.action.IActionStateChanged;
import zombie.characters.action.conditions.CharacterVariableCondition;
import zombie.characters.action.conditions.EventNotOccurred;
import zombie.characters.action.conditions.EventOccurred;
import zombie.characters.action.conditions.LuaCall;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.DebugType;
import zombie.network.GameClient;
import zombie.util.list.PZArrayUtil;

public final class ActionContext {
    private final IAnimatable owner;
    private ActionGroup actionGroup;
    private final int actionStateHistoryMaxSize = 6;
    private final Stack<ActionState> actionStateHistory = new Stack();
    private final ActionStateContainer previousActionStateContainer = new ActionStateContainer();
    private final ActionStateContainer actionStateContainer = new ActionStateContainer();
    private final ActionStateContainer nextActionStateContainer = new ActionStateContainer();
    private boolean statesChanged;
    public final ArrayList<IActionStateChanged> onStateChanged = new ArrayList();
    private final ActionContextEvents occurredAnimEvents = new ActionContextEvents();
    private final PerformanceProfileProbe updateInternal = new PerformanceProfileProbe("ActionContext.update");
    private final PerformanceProfileProbe postUpdateInternal = new PerformanceProfileProbe("ActionContext.postUpdate");

    public ActionContext(IAnimatable owner) {
        this.owner = owner;
    }

    public IAnimatable getOwner() {
        return this.owner;
    }

    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = this.updateInternal.profile();){
            this.updateInternal();
        }
        abstractPerformanceProfileProbe = this.postUpdateInternal.profile();
        try {
            this.postUpdateInternal();
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
    }

    private void updateInternal() {
        this.nextActionStateContainer.set(this.actionStateContainer);
        this.nextActionStateContainer.evaluateCurrentState(this);
        this.transferActionState(this.nextActionStateContainer);
    }

    private void transferActionState(ActionStateContainer nextActionStateContainer) {
        boolean subStatesChanged;
        if (this.actionStateContainer.equalTo(nextActionStateContainer)) {
            return;
        }
        this.previousActionStateContainer.set(this.actionStateContainer);
        this.actionStateContainer.set(nextActionStateContainer);
        this.updateHistory(this.previousActionStateContainer.getRootState(), this.actionStateContainer.getRootState(), this.actionStateContainer.getTransitionUsedForThisState());
        boolean rootStateChanged = this.previousActionStateContainer.getRootState() != nextActionStateContainer.getRootState();
        boolean bl = subStatesChanged = !this.previousActionStateContainer.subStatesEqual(nextActionStateContainer);
        if (rootStateChanged) {
            DebugType.ActionSystem.trace("%s>  State changed from \"%s\" to \"%s\",", this.getOwner().getUID(), this.peekPreviousStateName(), this.getCurrentStateName());
            if (GameClient.client) {
                StateManager.exitState(this.owner, this.previousActionStateContainer.getRootState());
                StateManager.enterState(this.owner, this.actionStateContainer.getRootState());
            }
        }
        if (subStatesChanged) {
            int subStatei;
            for (subStatei = 0; subStatei < this.previousActionStateContainer.childStateCount(); ++subStatei) {
                ActionState oldSubState = this.previousActionStateContainer.getChildStateAt(subStatei);
                if (nextActionStateContainer.hasChildState(oldSubState)) continue;
                DebugType.ActionSystem.trace("%s> SubState exited. \"%s\"", this.getOwner().getUID(), oldSubState.getName());
                if (!GameClient.client) continue;
                StateManager.exitSubState(this.owner, oldSubState);
            }
            for (subStatei = 0; subStatei < nextActionStateContainer.childStateCount(); ++subStatei) {
                ActionState nextSubState = nextActionStateContainer.getChildStateAt(subStatei);
                if (this.previousActionStateContainer.hasChildState(nextSubState)) continue;
                ActionState upperState = subStatei > 0 ? nextActionStateContainer.getChildStateAt(subStatei - 1) : nextActionStateContainer.getRootState();
                DebugType.ActionSystem.trace("%s> Transition passes. SubState \"%s\" added to parent state: \"%s\"", this.getOwner().getUID(), nextSubState.getName(), upperState.getName());
                if (!GameClient.client) continue;
                StateManager.enterSubState(this.owner, nextSubState);
            }
        }
        this.onStatesChanged();
    }

    private void updateHistory(ActionState previousState, ActionState currentState, ActionTransition transitionUsed) {
        if (currentState == null) {
            DebugType.ActionSystem.error("Current state is null.");
            return;
        }
        if (previousState == currentState) {
            return;
        }
        if (previousState == null) {
            DebugType.ActionSystem.debugln("Previous state null. Resetting history. Entering state: %s", currentState.getName());
            this.actionStateHistory.clear();
            return;
        }
        if (!this.actionStateHistory.isEmpty() && transitionUsed != null && transitionUsed.transitionOut) {
            ActionState previousStateInHistory = this.peekPreviousState();
            if (previousStateInHistory == currentState) {
                this.popPreviousState();
                DebugType.ActionSystem.debugln("TransitionOut success. Returning from: %s to: %s", previousState.getName(), currentState.getName());
                return;
            }
            DebugType.ActionSystem.error("TransitionOut mismatch. Previous state \"%s\" != inHistory \"%s\"", currentState.getName(), previousStateInHistory.getName());
            this.actionStateHistory.clear();
        }
        this.pushPreviousState(previousState);
    }

    private void postUpdateInternal() {
        this.clearActionContextEvents();
        this.invokeAnyStateChangedEvents();
        this.logCurrentState();
    }

    public ActionState peekNextState() {
        return this.actionStateContainer.peekNextState(this);
    }

    public boolean canTransitionToState(String stateName) {
        return this.canTransitionToState(stateName, true);
    }

    public boolean canTransitionToState(String stateName, boolean allowSubState) {
        return this.actionStateContainer.canTransitionToState(this.getGroup(), stateName, allowSubState);
    }

    public void setPlaybackStateSnapshot(ActionStateSnapshot snapshot) {
        this.nextActionStateContainer.clear();
        this.nextActionStateContainer.setPlaybackStateSnapshot(this, snapshot);
        this.transferActionState(this.nextActionStateContainer);
    }

    public ActionStateSnapshot getPlaybackStateSnapshot() {
        return this.actionStateContainer.getPlaybackStateSnapshot();
    }

    public void setCurrentState(ActionState nextState) {
        this.nextActionStateContainer.clear();
        this.nextActionStateContainer.setCurrentState(nextState, null);
        this.transferActionState(this.nextActionStateContainer);
    }

    private void onStatesChanged() {
        this.statesChanged = true;
    }

    public void logCurrentState() {
        if (this.owner.isAnimationRecorderActive()) {
            this.owner.getAnimationRecorder().logActionState(this.actionGroup, this.actionStateContainer.getRootState(), this.actionStateContainer.getChildStates());
            this.owner.getAnimationRecorder().logVariable("actionStateHistory", PZArrayUtil.arrayToString(this.actionStateHistory, ActionState::getName, "", "", ";"));
        }
    }

    private void invokeAnyStateChangedEvents() {
        if (!this.statesChanged) {
            return;
        }
        this.statesChanged = false;
        for (int i = 0; i < this.onStateChanged.size(); ++i) {
            IActionStateChanged callback = this.onStateChanged.get(i);
            callback.actionStateChanged(this);
        }
        IAnimatable iAnimatable = this.owner;
        if (iAnimatable instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)iAnimatable;
            isoZombie.networkAi.extraUpdate();
        }
    }

    public void clearActionContextEvents() {
        this.occurredAnimEvents.clear();
    }

    public ActionState getCurrentState() {
        return this.actionStateContainer.getRootState();
    }

    public void setGroup(ActionGroup group) {
        this.actionGroup = group;
        this.setCurrentState(group.getInitialState());
    }

    public ActionGroup getGroup() {
        return this.actionGroup;
    }

    public void reportEvent(String event) {
        this.reportEvent(null, event);
    }

    public void reportEvent(String state, String event) {
        IsoPlayer player;
        IAnimatable iAnimatable;
        this.occurredAnimEvents.add(event, state);
        if (state == null && GameClient.client && (iAnimatable = this.owner) instanceof IsoPlayer && (player = (IsoPlayer)iAnimatable).isLocalPlayer()) {
            player.getNetworkCharacterAI().getState().reportEvent(state, event);
        }
    }

    public ActionState getChildStateAt(int idx) {
        return this.actionStateContainer.getChildStateAt(idx);
    }

    public List<ActionState> getChildStates() {
        return this.actionStateContainer.getChildStates();
    }

    public String getCurrentStateName() {
        return this.actionStateContainer.getRootState() != null ? this.actionStateContainer.getCurrentStateName() : this.actionGroup.getDefaultState().getName();
    }

    public String peekPreviousStateName() {
        return this.getStateNameOrDefault(this.peekPreviousState());
    }

    public ActionState popPreviousState() {
        return !this.actionStateHistory.isEmpty() ? this.actionStateHistory.pop() : null;
    }

    public ActionState peekPreviousState() {
        return !this.actionStateHistory.isEmpty() ? this.actionStateHistory.peek() : null;
    }

    private void pushPreviousState(ActionState currentState) {
        if (currentState != null && this.peekPreviousState() != currentState) {
            this.actionStateHistory.push(currentState);
        }
        while (this.actionStateHistory.size() >= 6) {
            this.actionStateHistory.removeFirst();
        }
    }

    private String getStateNameOrDefault(ActionState previousState) {
        return previousState != null ? previousState.getName() : this.actionGroup.getDefaultState().getName();
    }

    public boolean hasEventOccurred(String eventName) {
        return this.hasEventOccurred(eventName, null);
    }

    public boolean hasEventOccurred(String eventName, String stateName) {
        return this.occurredAnimEvents.contains(eventName, stateName);
    }

    public void clearEvent(String eventName) {
        this.occurredAnimEvents.clearEvent(eventName);
    }

    public void getEvents(HashMap<String, String> events) {
        this.occurredAnimEvents.get(events);
    }

    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        return this.actionStateContainer.getVariable(handle);
    }

    public boolean hasStateVariables() {
        return this.actionStateContainer.hasStateVariables();
    }

    static {
        CharacterVariableCondition.Factory factory2 = new CharacterVariableCondition.Factory();
        IActionCondition.registerFactory("isTrue", factory2);
        IActionCondition.registerFactory("isFalse", factory2);
        IActionCondition.registerFactory("compare", factory2);
        IActionCondition.registerFactory("gtr", factory2);
        IActionCondition.registerFactory("less", factory2);
        IActionCondition.registerFactory("equals", factory2);
        IActionCondition.registerFactory("lessEqual", factory2);
        IActionCondition.registerFactory("gtrEqual", factory2);
        IActionCondition.registerFactory("notEquals", factory2);
        IActionCondition.registerFactory("eventOccurred", new EventOccurred.Factory());
        IActionCondition.registerFactory("eventNotOccurred", new EventNotOccurred.Factory());
        IActionCondition.registerFactory("lua", new LuaCall.Factory());
    }
}

