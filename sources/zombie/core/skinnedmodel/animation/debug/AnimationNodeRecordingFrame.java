/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import java.util.List;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.animation.debug.GenericNameWeightRecordingFrame;
import zombie.iso.Vector3;
import zombie.util.list.PZArrayUtil;

public final class AnimationNodeRecordingFrame
extends GenericNameWeightRecordingFrame {
    private String actionGroupName;
    private String actionStateName;
    private final ArrayList<String> actionSubStateNames = new ArrayList();
    private String aiStateName;
    private String animStateName;
    private final ArrayList<String> animSubStateNames = new ArrayList();
    private final ArrayList<String> aiSubStateNames = new ArrayList();
    private final Vector3 characterToPlayerDiff = new Vector3();

    public AnimationNodeRecordingFrame(String fileKey) {
        super(fileKey);
    }

    public void logActionState(ActionGroup group, ActionState state, List<ActionState> childStates) {
        this.actionGroupName = group.getName();
        this.actionStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.actionSubStateNames, childStates, ActionState::getName);
    }

    public void logActionState(String actionGroupName, String actionStateName) {
        this.actionGroupName = actionGroupName;
        this.actionStateName = actionStateName;
        this.actionSubStateNames.clear();
    }

    public void logAIState(State state, List<StateMachine.SubstateSlot> subStates) {
        this.aiStateName = state != null ? state.getName() : null;
        PZArrayUtil.arrayConvert(this.aiSubStateNames, subStates, subState -> !subState.isEmpty() ? subState.getState().getName() : "");
    }

    public void logAIState(String aiStateName) {
        this.aiStateName = aiStateName;
        this.aiSubStateNames.clear();
    }

    public void logAnimState(AnimState state) {
        this.animStateName = state != null ? state.name : null;
    }

    public void logCharacterToPlayerDiff(Vector3 diff) {
        this.characterToPlayerDiff.set(diff);
    }

    @Override
    public void buildHeader(StringBuilder logLine) {
        AnimationNodeRecordingFrame.appendCell(logLine, "toPlayer.x");
        AnimationNodeRecordingFrame.appendCell(logLine, "toPlayer.y");
        AnimationNodeRecordingFrame.appendCell(logLine, "actionGroup");
        AnimationNodeRecordingFrame.appendCell(logLine, "actionState");
        AnimationNodeRecordingFrame.appendCell(logLine, "actionState.sub[0]");
        AnimationNodeRecordingFrame.appendCell(logLine, "actionState.sub[1]");
        AnimationNodeRecordingFrame.appendCell(logLine, "aiState");
        AnimationNodeRecordingFrame.appendCell(logLine, "aiState.sub[0]");
        AnimationNodeRecordingFrame.appendCell(logLine, "aiState.sub[1]");
        AnimationNodeRecordingFrame.appendCell(logLine, "animState");
        AnimationNodeRecordingFrame.appendCell(logLine, "animState.sub[0]");
        AnimationNodeRecordingFrame.appendCell(logLine, "animState.sub[1]");
        AnimationNodeRecordingFrame.appendCell(logLine, "nodeWeights.begin");
        super.buildHeader(logLine);
    }

    @Override
    protected void writeData(StringBuilder logLine) {
        AnimationNodeRecordingFrame.appendCell(logLine, this.characterToPlayerDiff.x);
        AnimationNodeRecordingFrame.appendCell(logLine, this.characterToPlayerDiff.y);
        AnimationNodeRecordingFrame.appendCellQuot(logLine, this.actionGroupName);
        AnimationNodeRecordingFrame.appendCellQuot(logLine, this.actionStateName);
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 0, ""));
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.actionSubStateNames, 1, ""));
        AnimationNodeRecordingFrame.appendCellQuot(logLine, this.aiStateName);
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 0, ""));
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.aiSubStateNames, 1, ""));
        AnimationNodeRecordingFrame.appendCellQuot(logLine, this.animStateName);
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 0, ""));
        AnimationNodeRecordingFrame.appendCellQuot(logLine, PZArrayUtil.getOrDefault(this.animSubStateNames, 1, ""));
        AnimationNodeRecordingFrame.appendCell(logLine);
        super.writeData(logLine);
    }
}

