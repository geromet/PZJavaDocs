/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;
import zombie.ZomboidFileSystem;
import zombie.ai.State;
import zombie.ai.StateMachine;
import zombie.characters.IsoPlayer;
import zombie.characters.action.ActionGroup;
import zombie.characters.action.ActionState;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntries;
import zombie.core.skinnedmodel.animation.debug.AnimationEventRecordingFrame;
import zombie.core.skinnedmodel.animation.debug.AnimationNodeRecordingFrame;
import zombie.core.skinnedmodel.animation.debug.AnimationTrackRecordingFrame;
import zombie.core.skinnedmodel.animation.debug.AnimationVariableRecordingFrame;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public final class AnimationPlayerRecorder {
    private static boolean isInitialized;
    private boolean isRecording;
    private boolean isLineActive;
    private final AnimationTrackRecordingFrame animationTrackFrame;
    private final AnimationNodeRecordingFrame animationNodeFrame;
    private final AnimationVariableRecordingFrame animationVariableFrame;
    private final AnimationEventRecordingFrame animationEventFrame;
    private final IsoMovingObject ownerObject;

    public AnimationPlayerRecorder(IsoMovingObject owner) {
        this.ownerObject = owner;
        String characterId = this.ownerObject.getUID();
        String fileKey = characterId + "_AnimRecorder";
        this.animationTrackFrame = new AnimationTrackRecordingFrame(fileKey + "_Track");
        this.animationNodeFrame = new AnimationNodeRecordingFrame(fileKey + "_Node");
        this.animationVariableFrame = new AnimationVariableRecordingFrame(fileKey + "_Vars");
        this.animationEventFrame = new AnimationEventRecordingFrame(fileKey + "_Events");
        AnimationPlayerRecorder.init();
    }

    public static synchronized void init() {
        if (isInitialized) {
            return;
        }
        DebugLog.General.debugln("Initializing...");
        isInitialized = true;
        AnimationPlayerRecorder.backupOldRecordings();
    }

    public static void backupOldRecordings() {
        String recordingDir = AnimationPlayerRecorder.getRecordingDir();
        try {
            File recordingsDir = new File(recordingDir);
            File[] recordingFileList = ZomboidFileSystem.listAllFiles(recordingsDir);
            if (recordingFileList.length == 0) {
                return;
            }
            String backupDirName = "backup_" + ZomboidFileSystem.getStartupTimeStamp();
            File backupDir = new File(recordingDir + File.separator + backupDirName);
            ZomboidFileSystem.ensureFolderExists(backupDir);
            for (int i = 0; i < recordingFileList.length; ++i) {
                File fileToMove = recordingFileList[i];
                if (!fileToMove.isFile()) continue;
                fileToMove.renameTo(new File(backupDir.getAbsolutePath() + File.separator + fileToMove.getName()));
                fileToMove.delete();
            }
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "Exception thrown trying to backup old recordings, Trying to copy old recording files.", LogSeverity.Error);
        }
    }

    public static void discardOldRecordings() {
        String recordingDir = AnimationPlayerRecorder.getRecordingDir();
        try {
            File recordingsDir = new File(recordingDir);
            File[] recordingFileList = ZomboidFileSystem.listAllFiles(recordingsDir);
            if (recordingFileList.length == 0) {
                return;
            }
            for (int i = 0; i < recordingFileList.length; ++i) {
                File fileToMove = recordingFileList[i];
                if (!fileToMove.isFile()) continue;
                fileToMove.delete();
            }
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "Exception thrown trying to discard old recordings, Trying to delete old recording files.", LogSeverity.Error);
        }
    }

    public void newFrame(int frameNo) {
        if (this.isLineActive) {
            this.writeFrame();
        }
        if (!this.isRecording()) {
            this.close();
            return;
        }
        this.isLineActive = true;
        this.animationTrackFrame.reset();
        this.animationTrackFrame.setFrameNumber(frameNo);
        this.animationNodeFrame.reset();
        this.animationNodeFrame.setFrameNumber(frameNo);
        this.animationVariableFrame.reset();
        this.animationVariableFrame.setFrameNumber(frameNo);
        this.animationEventFrame.reset();
        this.animationEventFrame.setFrameNumber(frameNo);
        this.initLogRecording();
    }

    private void initLogRecording() {
        if (DebugLog.getRecordingOut() != null) {
            return;
        }
        PrintStream consoleOut = AnimationPlayerRecorder.openFileStream("console", true, string -> {});
        DebugLog.setRecordingOut(consoleOut);
        DebugLog.setLogSeverity(DebugType.AnimationRecorder, LogSeverity.All);
    }

    private void closeLogRecording() {
        PrintStream consoleout = DebugLog.getRecordingOut();
        if (consoleout == null) {
            return;
        }
        DebugLog.setRecordingOut(null);
        consoleout.flush();
        consoleout.close();
        DebugLog.setLogSeverity(DebugType.AnimationRecorder, LogSeverity.Off);
    }

    public boolean hasActiveLine() {
        return this.isLineActive;
    }

    public void writeFrame() {
        this.animationTrackFrame.writeLine();
        this.animationNodeFrame.writeLine();
        this.animationVariableFrame.writeLine();
        this.animationEventFrame.writeLine();
        this.isLineActive = false;
    }

    public void discardRecording() {
        this.animationTrackFrame.closeAndDiscard();
        this.animationNodeFrame.closeAndDiscard();
        this.animationVariableFrame.closeAndDiscard();
        this.animationEventFrame.closeAndDiscard();
        this.isLineActive = false;
    }

    public void close() {
        this.animationTrackFrame.close();
        this.animationNodeFrame.close();
        this.animationVariableFrame.close();
        this.animationEventFrame.close();
        this.closeLogRecording();
        this.isLineActive = false;
    }

    public static PrintStream openFileStream(String key, boolean append, Consumer<String> fileNameConsumer) {
        String filePath = AnimationPlayerRecorder.getTimeStampedFilePath(key);
        try {
            fileNameConsumer.accept(filePath);
            File file = new File(filePath);
            return new PrintStream(new FileOutputStream(file, append), true);
        }
        catch (FileNotFoundException e) {
            DebugLog.General.error("Exception thrown trying to create animation player recording file.");
            DebugLog.General.error(e);
            e.printStackTrace();
            return null;
        }
    }

    public static String getRecordingDir() {
        String recordingDirPath = ZomboidFileSystem.instance.getCacheDirSub("Recording");
        ZomboidFileSystem.ensureFolderExists(recordingDirPath);
        File recordingDir = new File(recordingDirPath);
        return recordingDir.getAbsolutePath();
    }

    private static String getTimeStampedFilePath(String key) {
        return AnimationPlayerRecorder.getRecordingDir() + File.separator + AnimationPlayerRecorder.getTimeStampedFileName(key) + ".csv";
    }

    private static String getTimeStampedFileName(String name) {
        return ZomboidFileSystem.getStartupTimeStamp() + "_" + name;
    }

    public void logAnimWeights(LiveAnimationTrackEntries trackEntries, Vector2 deferredMovement, Vector3 deferredMovementFromRagdoll) {
        this.animationTrackFrame.logAnimWeights(trackEntries);
        this.animationVariableFrame.logDeferredMovement(deferredMovement, deferredMovementFromRagdoll);
    }

    public void logAnimNode(LiveAnimNode liveNode) {
        if (liveNode.isTransitioningIn()) {
            this.animationNodeFrame.logWeight("transition(" + liveNode.getTransitionFrom() + "->" + liveNode.getName() + ")", liveNode.getTransitionLayerIdx(), liveNode.getTransitionInWeight());
        }
        if (liveNode.runningRagdollTrack != null) {
            this.animationNodeFrame.logWeight(liveNode.getName() + "." + liveNode.runningRagdollTrack.getName(), liveNode.getLayerIdx(), liveNode.runningRagdollTrack.getBlendWeight());
        }
        this.animationNodeFrame.logWeight(liveNode.getName(), liveNode.getLayerIdx(), liveNode.getWeight());
    }

    public void logActionState(ActionGroup actionGroup, ActionState actionState, List<ActionState> subStates) {
        this.animationNodeFrame.logActionState(actionGroup, actionState, subStates);
    }

    public void logActionState(String actionGroupName, String actionStateName) {
        this.animationNodeFrame.logActionState(actionGroupName, actionStateName);
    }

    public void logAIState(State state, List<StateMachine.SubstateSlot> subStates) {
        this.animationNodeFrame.logAIState(state, subStates);
    }

    public void logAIState(String aiStateName) {
        this.animationNodeFrame.logAIState(aiStateName);
    }

    public void logAnimState(AnimState state) {
        this.animationNodeFrame.logAnimState(state);
    }

    public void logVariables(IAnimationVariableSource varSource) {
        this.animationVariableFrame.logVariables(varSource);
    }

    public void logVariable(String variableKey, String variableValue) {
        this.animationVariableFrame.logVariable(variableKey, variableValue);
    }

    public void logVariable(String variableKey, boolean variableValue) {
        this.animationVariableFrame.logVariable(variableKey, variableValue);
    }

    public void logVariable(String variableKey, int variableValue) {
        this.animationVariableFrame.logVariable(variableKey, variableValue);
    }

    public void logVariable(String variableKey, float variableValue) {
        this.animationVariableFrame.logVariable(variableKey, variableValue);
    }

    public void logAnimEvent(AnimationTrack track, AnimEvent evt) {
        this.animationEventFrame.logAnimEvent(track, evt);
    }

    public void logGlobalAnimEvent(AnimEvent evt) {
        this.animationEventFrame.logGlobalAnimEvent(evt);
    }

    public void logCharacterPos() {
        IsoPlayer player = IsoPlayer.getInstance();
        IsoMovingObject chr = this.getOwner();
        Vector3 playerPos = player.getPosition(new Vector3());
        Vector3 charPos = chr.getPosition(new Vector3());
        Vector3 diff = playerPos.sub(charPos, new Vector3());
        this.animationNodeFrame.logCharacterToPlayerDiff(diff);
    }

    public IsoMovingObject getOwner() {
        return this.ownerObject;
    }

    public boolean isRecording() {
        return this.isRecording;
    }

    public void setRecording(boolean value) {
        if (this.isRecording == value) {
            return;
        }
        this.isRecording = value;
        if (!this.isRecording) {
            this.close();
        }
        DebugLog.General.println("AnimationPlayerRecorder %s.", this.isRecording ? "recording" : "stopped");
    }
}

