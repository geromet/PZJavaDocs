/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.ArrayList;
import java.util.List;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.util.list.PZArrayUtil;

public final class AnimationMultiTrack {
    private final ArrayList<AnimationTrack> tracks = new ArrayList();
    private static final ArrayList<AnimationTrack> tempTracks = new ArrayList();

    public void addTrack(AnimationTrack track) {
        this.tracks.add(track);
    }

    public void removeTrack(AnimationTrack track) {
        int indexOf = this.getIndexOfTrack(track);
        if (indexOf > -1) {
            this.removeTrackAt(indexOf);
        }
    }

    public void removeTracks(List<AnimationTrack> tracks) {
        tempTracks.clear();
        PZArrayUtil.addAll(tempTracks, tracks);
        for (int i = 0; i < tempTracks.size(); ++i) {
            this.removeTrack(tempTracks.get(i));
        }
    }

    public void removeTrackAt(int indexOf) {
        this.tracks.remove(indexOf).release();
    }

    public int getIndexOfTrack(AnimationTrack track) {
        if (track == null) {
            return -1;
        }
        int indexOf = -1;
        for (int i = 0; i < this.tracks.size(); ++i) {
            AnimationTrack animationTrack = this.tracks.get(i);
            if (animationTrack != track) continue;
            indexOf = i;
            break;
        }
        return indexOf;
    }

    public void Update(float time) {
        for (int n = 0; n < this.tracks.size(); ++n) {
            AnimationTrack track = this.tracks.get(n);
            track.Update(time);
            if (track.currentClip != null) continue;
            this.removeTrackAt(n);
            --n;
        }
    }

    public float getDuration() {
        float longestDuration = 0.0f;
        for (int i = 0; i < this.tracks.size(); ++i) {
            AnimationTrack track = this.tracks.get(i);
            float trackDuration = track.getDuration();
            if (track.currentClip == null || !(trackDuration > longestDuration)) continue;
            longestDuration = trackDuration;
        }
        return longestDuration;
    }

    public void reset() {
        int trackCount = this.tracks.size();
        for (int i = 0; i < trackCount; ++i) {
            AnimationTrack track = this.tracks.get(i);
            track.reset();
        }
        AnimationPlayer.releaseTracks(this.tracks);
        this.tracks.clear();
    }

    public List<AnimationTrack> getTracks() {
        return this.tracks;
    }

    public int getTrackCount() {
        return this.tracks.size();
    }

    public AnimationTrack getTrackAt(int i) {
        return this.tracks.get(i);
    }

    public boolean containsAnyRagdollTracks() {
        List<AnimationTrack> tracks = this.getTracks();
        for (int trackIdx = 0; trackIdx < tracks.size(); ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            if (!track.isRagdoll()) continue;
            return true;
        }
        return false;
    }

    public boolean anyRagdollFirstFrame() {
        List<AnimationTrack> tracks = this.getTracks();
        for (int trackIdx = 0; trackIdx < tracks.size(); ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            if (!track.isRagdollFirstFrame()) continue;
            return true;
        }
        return false;
    }

    public void initRagdollTransforms(TwistableBoneTransform[] boneTransforms, boolean bForce) {
        List<AnimationTrack> tracks = this.getTracks();
        for (int trackIdx = 0; trackIdx < tracks.size(); ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            if (bForce || !track.isRagdollFirstFrame()) continue;
            track.initRagdollTransforms(boneTransforms);
        }
    }

    public AnimationTrack getActiveRagdollTrack() {
        AnimationTrack foundTrack = null;
        List<AnimationTrack> tracks = this.getTracks();
        for (int trackIdx = 0; trackIdx < tracks.size(); ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            if (!track.isRagdoll() || !track.isPlaying || track.getBlendWeight() <= 0.0f || foundTrack != null && !(foundTrack.getBlendWeight() > track.getBlendWeight())) continue;
            foundTrack = track;
        }
        return foundTrack;
    }
}

