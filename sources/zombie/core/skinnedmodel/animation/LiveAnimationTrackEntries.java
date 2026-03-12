/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import java.util.Arrays;
import java.util.List;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.LiveAnimationTrackEntry;
import zombie.debug.DebugLog;
import zombie.util.Pool;

public class LiveAnimationTrackEntries {
    private int totalAnimBlendCount;
    private final int animBlendIndexCacheSize = 32;
    private final LiveAnimationTrackEntry[] liveAnimTrackEntries = new LiveAnimationTrackEntry[32];
    private final int maxLayers = 16;
    private final int[] layerBlendCounts = new int[16];
    private final float[] layerWeightTotals = new float[16];

    public void clear() {
        this.totalAnimBlendCount = 0;
        LiveAnimationTrackEntries.clear(this.liveAnimTrackEntries);
        Arrays.fill(this.layerBlendCounts, 0);
        Arrays.fill(this.layerWeightTotals, 0.0f);
    }

    public void setTracks(List<AnimationTrack> tracks, float minimumValidAnimWeight, boolean normalizeFirstLayerTracks) {
        LiveAnimationTrackEntry trackEntry;
        int tracksCount = tracks.size();
        this.clear();
        for (int trackIdx = 0; trackIdx < tracksCount; ++trackIdx) {
            AnimationTrack track = tracks.get(trackIdx);
            float trackWeight = track.getBlendWeight();
            int trackLayer = track.getLayerIdx();
            int trackPriority = track.getPriority();
            if (trackLayer < 0 || trackLayer >= 16) {
                DebugLog.General.error("Layer index is out of range: %d. Range: 0 - %d", trackLayer, 15);
                continue;
            }
            if (trackWeight < minimumValidAnimWeight || trackLayer > 0 && track.isFinished()) continue;
            int insertAt = -1;
            for (int i = 0; i < this.liveAnimTrackEntries.length; ++i) {
                LiveAnimationTrackEntry trackEntry2 = this.liveAnimTrackEntries[i];
                if (trackEntry2 == null) {
                    insertAt = i;
                    break;
                }
                if (trackLayer > trackEntry2.getLayer()) continue;
                if (trackLayer < trackEntry2.getLayer()) {
                    insertAt = i;
                    break;
                }
                if (trackPriority > trackEntry2.getPriority()) continue;
                if (trackPriority < trackEntry2.getPriority()) {
                    insertAt = i;
                    break;
                }
                if (!(trackWeight < trackEntry2.getBlendWeight())) continue;
                insertAt = i;
                break;
            }
            if (insertAt < 0) {
                DebugLog.General.error("Buffer overflow. Insufficient anim blends in cache. More than %d animations are being blended at once. Will be truncated to %d.", this.liveAnimTrackEntries.length, this.liveAnimTrackEntries.length);
                continue;
            }
            LiveAnimationTrackEntries.insertAt(this.liveAnimTrackEntries, track, insertAt);
        }
        for (int i = 0; i < this.liveAnimTrackEntries.length && (trackEntry = this.liveAnimTrackEntries[i]) != null; ++i) {
            int layerIdx;
            int n = layerIdx = trackEntry.getLayer();
            this.layerWeightTotals[n] = this.layerWeightTotals[n] + trackEntry.getBlendWeight();
            int n2 = layerIdx;
            this.layerBlendCounts[n2] = this.layerBlendCounts[n2] + 1;
            ++this.totalAnimBlendCount;
        }
        if (this.totalAnimBlendCount == 0) {
            return;
        }
        if (normalizeFirstLayerTracks) {
            boolean layerIdx = false;
            int layerNo = this.liveAnimTrackEntries[0].getLayer();
            int layerTrackCount = this.layerBlendCounts[0];
            float layerTotalWeight = this.layerWeightTotals[0];
            if (layerTotalWeight < 1.0f) {
                LiveAnimationTrackEntry trackEntry3;
                int layer;
                for (int i = 0; i < this.totalAnimBlendCount && (layer = (trackEntry3 = this.liveAnimTrackEntries[i]).getLayer()) == layerNo; ++i) {
                    if (layerTotalWeight > 0.0f) {
                        trackEntry3.setBlendWeight(trackEntry3.getBlendWeight() / layerTotalWeight);
                        continue;
                    }
                    trackEntry3.setBlendWeight(1.0f / (float)layerTrackCount);
                }
            }
        }
    }

    private static void insertAt(LiveAnimationTrackEntry[] liveAnimTrackEntries, AnimationTrack track, int insertAt) {
        LiveAnimationTrackEntry newEntry = LiveAnimationTrackEntry.alloc(track);
        if (liveAnimTrackEntries[insertAt] == null) {
            liveAnimTrackEntries[insertAt] = newEntry;
            return;
        }
        LiveAnimationTrackEntry previousEntry = newEntry;
        for (int i = insertAt; i < liveAnimTrackEntries.length; ++i) {
            LiveAnimationTrackEntry currentEntry = liveAnimTrackEntries[i];
            liveAnimTrackEntries[i] = previousEntry;
            previousEntry = currentEntry;
            if (previousEntry == null) break;
        }
        Pool.tryRelease(previousEntry);
    }

    private static void clear(LiveAnimationTrackEntry[] array) {
        for (int i = 0; i < array.length; ++i) {
            Pool.tryRelease(array[i]);
            array[i] = null;
        }
    }

    public int count() {
        return this.totalAnimBlendCount;
    }

    public LiveAnimationTrackEntry get(int trackIdx) {
        return this.liveAnimTrackEntries[trackIdx];
    }
}

