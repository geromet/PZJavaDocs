/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.characters.action.ActionStateSnapshot;
import zombie.characters.action.ActionTransition;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ActionStateContainer {
    private ActionState currentState;
    private final ArrayList<ActionState> childStates = new ArrayList();
    private ActionTransition transitionUsedForThisState = null;
    private final PerformanceProfileProbe evaluateCurrentStateTransitions = new PerformanceProfileProbe("ActionStateContainer.evaluateCurrentStateTransitions");
    private final PerformanceProfileProbe evaluateSubStateTransitions = new PerformanceProfileProbe("ActionStateContainer.evaluateSubStateTransitions");

    public void evaluateCurrentState(ActionContext actionContext) {
        if (this.currentState == null) {
            return;
        }
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = this.evaluateCurrentStateTransitions.profile();){
            this.evaluateCurrentStateTransitions(actionContext);
        }
        abstractPerformanceProfileProbe = this.evaluateSubStateTransitions.profile();
        try {
            this.evaluateSubStateTransitions(actionContext);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
    }

    public ActionState peekNextState(ActionContext actionContext) {
        ActionState result = null;
        ActionGroup actionGroup = actionContext.getGroup();
        for (int i = 0; i < this.currentState.transitions.size(); ++i) {
            ActionState nextState;
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            String transitionTo = this.getTransitionTo(actionContext, nextTransition);
            if (StringUtils.isNullOrWhitespace(transitionTo) || !nextTransition.passes(actionContext, this.currentState) || (nextState = actionGroup.findState(transitionTo)) == null || this.hasChildState(nextState) || nextTransition.asSubstate && !this.currentStateSupportsChildState(nextState)) continue;
            result = nextState;
            break;
        }
        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); ++subStateIdx) {
            ActionState nextState = null;
            ActionState subState = this.getChildStateAt(subStateIdx);
            for (int transIdx = 0; transIdx < subState.transitions.size(); ++transIdx) {
                ActionState nextSubState;
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (!subTransition.passes(actionContext, subState)) continue;
                if (subTransition.transitionOut) break;
                String transitionTo = this.getTransitionTo(actionContext, subTransition);
                if (StringUtils.isNullOrWhitespace(transitionTo) || (nextSubState = actionGroup.findState(transitionTo)) == null || this.hasChildState(nextSubState)) continue;
                if (this.currentStateSupportsChildState(nextSubState)) break;
                if (!subTransition.forceParent) continue;
                nextState = nextSubState;
                break;
            }
            if (nextState == this.currentState || nextState == null) continue;
            result = nextState;
        }
        return result;
    }

    private String getTransitionTo(ActionContext actionContext, ActionTransition transition) {
        if (transition.transitionOut) {
            return actionContext.peekPreviousStateName();
        }
        return transition.transitionTo;
    }

    private void evaluateCurrentStateTransitions(ActionContext actionContext) {
        String transitionTo;
        ActionTransition nextTransition;
        int i;
        for (i = 0; i < this.currentState.transitions.size(); ++i) {
            nextTransition = this.currentState.transitions.get(i);
            if (nextTransition.asSubstate) continue;
            transitionTo = this.getTransitionTo(actionContext, nextTransition);
            if (StringUtils.isNullOrWhitespace(transitionTo)) {
                DebugType.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                continue;
            }
            if (!nextTransition.passes(actionContext, this.currentState)) continue;
            ActionState nextState = actionContext.getGroup().findState(transitionTo);
            if (nextState == null) {
                DebugType.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                continue;
            }
            this.setCurrentState(nextState, nextTransition);
            break;
        }
        for (i = 0; i < this.currentState.transitions.size(); ++i) {
            nextTransition = this.currentState.transitions.get(i);
            if (!nextTransition.asSubstate) continue;
            transitionTo = this.getTransitionTo(actionContext, nextTransition);
            if (StringUtils.isNullOrWhitespace(transitionTo)) {
                DebugType.ActionSystem.warn("%s> Transition's target state not specified: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                continue;
            }
            if (!nextTransition.passes(actionContext, this.currentState)) continue;
            ActionState nextSubState = actionContext.getGroup().findState(transitionTo);
            if (nextSubState == null) {
                DebugType.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), transitionTo);
                continue;
            }
            if (this.hasChildState(nextSubState)) continue;
            if (!this.currentStateSupportsChildState(nextSubState)) {
                DebugType.ActionSystem.warn("%s> Transition's target state \"%s\" not supported by parent: \"%s\"", actionContext.getOwner().getUID(), transitionTo, this.currentState.getName());
                continue;
            }
            this.tryInsertChildState(nextSubState);
        }
    }

    private void evaluateSubStateTransitions(ActionContext actionContext) {
        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); ++subStateIdx) {
            ActionState nextState = null;
            ActionState subState = this.getChildStateAt(subStateIdx);
            for (int transIdx = 0; transIdx < subState.transitions.size(); ++transIdx) {
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (!subTransition.passes(actionContext, subState)) continue;
                if (subTransition.transitionOut) {
                    this.removeChildStateAt(subStateIdx);
                    --subStateIdx;
                    break;
                }
                if (StringUtils.isNullOrWhitespace(subTransition.transitionTo)) continue;
                ActionState nextSubState = actionContext.getGroup().findState(subTransition.transitionTo);
                if (nextSubState == null) {
                    DebugType.ActionSystem.warn("%s> Transition's target state not found: \"%s\"", actionContext.getOwner().getUID(), subTransition.transitionTo);
                    continue;
                }
                if (this.hasChildState(nextSubState)) continue;
                if (this.currentStateSupportsChildState(nextSubState)) {
                    ActionState previousSubState = this.childStates.set(subStateIdx, nextSubState);
                    DebugType.ActionSystem.trace("%s> Transition passes. SubState \"%s\" replaced with: \"%s\"", actionContext.getOwner().getUID(), previousSubState.getName(), subTransition.transitionTo);
                    break;
                }
                if (!subTransition.forceParent) continue;
                nextState = nextSubState;
                break;
            }
            if (nextState == this.currentState || nextState == null) continue;
            this.setCurrentState(nextState, null);
        }
    }

    public boolean canTransitionToState(ActionGroup actionGroup, String stateName) {
        return this.canTransitionToState(actionGroup, stateName, true);
    }

    public boolean canTransitionToState(ActionGroup actionGroup, String stateName, boolean allowSubState) {
        ActionState nextState = actionGroup.findState(stateName);
        if (nextState == null) {
            return false;
        }
        for (int i = 0; i < this.currentState.transitions.size(); ++i) {
            ActionTransition nextTransition = this.currentState.transitions.get(i);
            if (!StringUtils.equalsIgnoreCase(stateName, nextTransition.transitionTo)) continue;
            return true;
        }
        if (!allowSubState) {
            return false;
        }
        if (!this.currentStateSupportsChildState(nextState)) {
            return false;
        }
        for (int subStateIdx = 0; subStateIdx < this.childStateCount(); ++subStateIdx) {
            ActionState subState = this.getChildStateAt(subStateIdx);
            for (int transIdx = 0; transIdx < subState.transitions.size(); ++transIdx) {
                ActionTransition subTransition = subState.transitions.get(transIdx);
                if (subTransition.transitionOut || !StringUtils.equalsIgnoreCase(stateName, subTransition.transitionTo)) continue;
                return true;
            }
        }
        return false;
    }

    protected boolean currentStateSupportsChildState(ActionState child) {
        return this.currentState != null && this.currentState.canHaveSubState(child);
    }

    public boolean hasChildState(ActionState child) {
        int indexOf = this.indexOfChildState(state -> state == child);
        return indexOf > -1;
    }

    public void setPlaybackStateSnapshot(ActionContext actionContext, ActionStateSnapshot snapshot) {
        String childName;
        int i;
        ActionGroup actionGroup = actionContext.getGroup();
        if (actionGroup == null) {
            return;
        }
        if (snapshot.stateName == null) {
            DebugLog.General.warn("Snapshot not valid. Missing root state name.");
            return;
        }
        ActionState rootState = actionGroup.findState(snapshot.stateName);
        this.setCurrentState(rootState, null);
        if (PZArrayUtil.isNullOrEmpty(snapshot.childStateNames)) {
            while (this.childStateCount() > 0) {
                this.removeChildStateAt(0);
            }
            return;
        }
        for (i = 0; i < this.childStateCount(); ++i) {
            childName = this.getChildStateAt(i).getName();
            boolean childExists = StringUtils.contains(snapshot.childStateNames, childName, StringUtils::equalsIgnoreCase);
            if (childExists) continue;
            this.removeChildStateAt(i);
            --i;
        }
        for (i = 0; i < snapshot.childStateNames.length; ++i) {
            childName = snapshot.childStateNames[i];
            ActionState childState = actionGroup.findState(childName);
            this.tryAddChildState(childState);
        }
    }

    public ActionStateSnapshot getPlaybackStateSnapshot() {
        if (this.currentState == null) {
            return null;
        }
        ActionStateSnapshot snapshot = new ActionStateSnapshot();
        snapshot.stateName = this.currentState.getName();
        snapshot.childStateNames = new String[this.childStates.size()];
        for (int i = 0; i < snapshot.childStateNames.length; ++i) {
            snapshot.childStateNames[i] = this.childStates.get(i).getName();
        }
        return snapshot;
    }

    public boolean setCurrentState(ActionState nextState, ActionTransition transitionUsed) {
        if (nextState == this.currentState) {
            return false;
        }
        this.currentState = nextState;
        this.transitionUsedForThisState = transitionUsed;
        for (int i = 0; i < this.childStates.size(); ++i) {
            ActionState subState = this.childStates.get(i);
            if (this.currentState.canHaveSubState(subState)) continue;
            this.removeChildStateAt(i);
            --i;
        }
        return true;
    }

    private boolean tryAddChildState(ActionState nextState) {
        if (this.hasChildState(nextState)) {
            return false;
        }
        this.childStates.add(nextState);
        return true;
    }

    private boolean tryInsertChildState(ActionState nextState) {
        if (this.hasChildState(nextState)) {
            return false;
        }
        int insertAt = -1;
        ActionState upperState = this.currentState;
        for (int i = 0; i < this.childStates.size(); ++i) {
            ActionState lowerState = this.childStates.get(i);
            if (upperState.canHaveSubState(nextState) && nextState.canHaveSubState(lowerState)) {
                insertAt = i;
                break;
            }
            upperState = lowerState;
        }
        if (insertAt > -1) {
            this.childStates.add(insertAt, nextState);
        } else {
            this.childStates.add(nextState);
        }
        return true;
    }

    public void removeChildStateAt(int subStateIdx) {
        this.childStates.remove(subStateIdx);
    }

    public ActionState getRootState() {
        return this.currentState;
    }

    public boolean hasChildStates() {
        return this.childStateCount() > 0;
    }

    public int childStateCount() {
        return this.childStates.size();
    }

    public void foreachChildState(Consumer<ActionState> consumer) {
        for (int i = 0; i < this.childStateCount(); ++i) {
            ActionState child = this.getChildStateAt(i);
            consumer.accept(child);
        }
    }

    public int indexOfChildState(Predicate<ActionState> predicate) {
        int indexOf = -1;
        for (int i = 0; i < this.childStateCount(); ++i) {
            ActionState child = this.getChildStateAt(i);
            if (!predicate.test(child)) continue;
            indexOf = i;
            break;
        }
        return indexOf;
    }

    public ActionState getChildStateAt(int idx) {
        if (idx < 0 || idx >= this.childStateCount()) {
            throw new IndexOutOfBoundsException(String.format("Index %d out of bounds. childCount: %d", idx, this.childStateCount()));
        }
        return this.childStates.get(idx);
    }

    public List<ActionState> getChildStates() {
        return this.childStates;
    }

    public String getCurrentStateName() {
        return this.currentState == null ? null : this.currentState.getName();
    }

    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        for (int i = this.childStates.size() - 1; i >= 0; --i) {
            ActionState childState = this.childStates.get(i);
            IAnimationVariableSlot childSlot = childState.getVariable(handle);
            if (childSlot == null) continue;
            return childSlot;
        }
        ActionState currentState = this.getRootState();
        if (currentState == null) {
            return null;
        }
        return currentState.getVariable(handle);
    }

    public boolean hasStateVariables() {
        for (int i = this.childStates.size() - 1; i >= 0; --i) {
            ActionState childState = this.childStates.get(i);
            if (!childState.hasStateVariables()) continue;
            return true;
        }
        ActionState currentState = this.getRootState();
        if (currentState == null) {
            return false;
        }
        return currentState.hasStateVariables();
    }

    public void set(ActionStateContainer actionStateContainer) {
        this.currentState = actionStateContainer.currentState;
        this.transitionUsedForThisState = actionStateContainer.transitionUsedForThisState;
        PZArrayUtil.copy(this.childStates, actionStateContainer.childStates);
    }

    public void clear() {
        this.currentState = null;
        this.transitionUsedForThisState = null;
        this.childStates.clear();
    }

    public boolean equalTo(ActionStateContainer rhs) {
        return this.currentState == rhs.currentState && this.subStatesEqual(rhs);
    }

    public boolean subStatesEqual(ActionStateContainer rhs) {
        return PZArrayUtil.sequenceEqual(this.childStates, rhs.childStates, PZArrayUtil.Comparators::referencesEqual);
    }

    public ActionTransition getTransitionUsedForThisState() {
        return this.transitionUsedForThisState;
    }
}

