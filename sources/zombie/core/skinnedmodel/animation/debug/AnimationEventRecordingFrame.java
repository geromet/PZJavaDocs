/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.GenericNameValueRecordingFrame;
import zombie.debug.DebugType;

public class AnimationEventRecordingFrame
extends GenericNameValueRecordingFrame {
    private final ArrayList<AnimEvent> events = new ArrayList();
    private final ArrayList<String> tracks = new ArrayList();

    public AnimationEventRecordingFrame(String fileKey) {
        super(fileKey, "_events");
        this.addColumnInternal("animEvent.name");
        this.addColumnInternal("animEvent.parameter");
        this.addColumnInternal("animNode");
        this.addColumnInternal("track");
        this.addColumnInternal("animEvent.time");
    }

    public void logAnimEvent(AnimationTrack track, AnimEvent evt) {
        this.tracks.add(track != null ? track.getName() : "");
        this.events.add(evt);
    }

    public void logGlobalAnimEvent(AnimEvent evt) {
        this.tracks.add("__GLOBAL__");
        this.events.add(evt);
    }

    @Override
    public void reset() {
        this.tracks.clear();
        this.events.clear();
    }

    @Override
    public String getValueAt(int i) {
        return "";
    }

    @Override
    protected void onColumnAdded() {
    }

    protected void writeData(String track, AnimEvent event, StringBuilder logLine) {
        AnimationEventRecordingFrame.appendCell(logLine, event.eventName);
        AnimationEventRecordingFrame.appendCell(logLine, event.parameterValue != null ? event.parameterValue : "");
        AnimationEventRecordingFrame.appendCell(logLine, event.parentAnimNode != null ? event.parentAnimNode.name : "");
        AnimationEventRecordingFrame.appendCell(logLine, track);
        if (event.time == AnimEvent.AnimEventTime.PERCENTAGE) {
            AnimationEventRecordingFrame.appendCell(logLine, event.timePc);
        } else {
            AnimationEventRecordingFrame.appendCell(logLine, event.time.toString());
        }
    }

    @Override
    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }
        StringBuilder logLine = this.lineBuffer;
        for (int i = 0; i < this.events.size(); ++i) {
            logLine.setLength(0);
            this.buildHeader(logLine);
            String headerStr = logLine.toString();
            logLine.setLength(0);
            AnimEvent event = this.events.get(i);
            String track = this.tracks.get(i);
            this.writeData(track, event, logLine);
            this.outValues.print(this.frameNumber);
            this.outValues.println(logLine);
            DebugType.AnimationRecorder.println("AnimEvent triggered at f:%d: Details: \r\n%s\r\n%s", this.frameNumber, headerStr, logLine);
        }
    }
}

