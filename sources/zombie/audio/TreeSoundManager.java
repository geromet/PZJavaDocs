/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;

public class TreeSoundManager {
    private final ArrayList<IsoGridSquare> squares = new ArrayList();
    private final Slot[] slots = new Slot[10];
    private final Comparator<IsoGridSquare> comp = (a, b) -> {
        float bScore;
        float aScore = this.getClosestListener((float)a.x + 0.5f, (float)a.y + 0.5f, a.z);
        if (aScore > (bScore = this.getClosestListener((float)b.x + 0.5f, (float)b.y + 0.5f, b.z))) {
            return 1;
        }
        if (aScore < bScore) {
            return -1;
        }
        return 0;
    };

    public TreeSoundManager() {
        for (int i = 0; i < this.slots.length; ++i) {
            this.slots[i] = new Slot();
        }
    }

    public void addSquare(IsoGridSquare square) {
        if (!this.squares.contains(square)) {
            this.squares.add(square);
        }
    }

    public void update() {
        int j;
        IsoGridSquare square;
        int i;
        for (int i2 = 0; i2 < this.slots.length; ++i2) {
            this.slots[i2].playing = false;
        }
        long ms = System.currentTimeMillis();
        if (this.squares.isEmpty()) {
            this.stopNotPlaying(ms);
            return;
        }
        Collections.sort(this.squares, this.comp);
        int count = Math.min(this.squares.size(), this.slots.length);
        for (i = 0; i < count; ++i) {
            square = this.squares.get(i);
            if (!this.shouldPlay(square) || (j = this.getExistingSlot(square)) == -1) continue;
            this.slots[j].playSound(square);
            this.slots[j].soundTime = ms;
        }
        for (i = 0; i < count; ++i) {
            square = this.squares.get(i);
            if (!this.shouldPlay(square) || (j = this.getExistingSlot(square)) != -1) continue;
            j = this.getFreeSlot();
            this.slots[j].playSound(square);
            this.slots[j].soundTime = ms;
        }
        this.stopNotPlaying(ms);
        this.squares.clear();
    }

    private float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr == null || chr.getCurrentSquare() == null) continue;
            float px = chr.getX();
            float py = chr.getY();
            float pz = chr.getZ();
            float dist = IsoUtils.DistanceTo(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
            if (!((dist *= chr.getHearDistanceModifier()) < minDist)) continue;
            minDist = dist;
        }
        return minDist;
    }

    boolean shouldPlay(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        return !(this.getClosestListener((float)square.x + 0.5f, (float)square.y + 0.5f, square.z) > 20.0f);
    }

    int getExistingSlot(IsoGridSquare square) {
        for (int i = 0; i < this.slots.length; ++i) {
            if (this.slots[i].square != square) continue;
            return i;
        }
        return -1;
    }

    private int getFreeSlot() {
        for (int i = 0; i < this.slots.length; ++i) {
            if (this.slots[i].playing) continue;
            return i;
        }
        return -1;
    }

    private int getFreeSlot(long ms) {
        long oldestTime = Long.MAX_VALUE;
        int oldestIndex = -1;
        for (int i = 0; i < this.slots.length; ++i) {
            if (this.slots[i].soundTime >= oldestTime) continue;
            oldestTime = this.slots[i].soundTime;
            oldestIndex = i;
        }
        if (ms - oldestTime < 1000L) {
            return -1;
        }
        return oldestIndex;
    }

    void stopNotPlaying(long ms) {
        for (int i = 0; i < this.slots.length; ++i) {
            Slot slot = this.slots[i];
            if (slot.playing || slot.soundTime > ms - 1000L) continue;
            slot.stopPlaying();
            slot.square = null;
        }
    }

    private static final class Slot {
        long soundTime;
        IsoGridSquare square;
        boolean playing;
        BaseSoundEmitter emitter;
        long instance;

        private Slot() {
        }

        void playSound(IsoGridSquare square) {
            if (this.emitter == null) {
                this.emitter = Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter();
            }
            this.emitter.setPos((float)square.x + 0.5f, (float)square.y + 0.5f, square.z);
            if (!this.emitter.isPlaying("Bushes")) {
                this.instance = this.emitter.playSoundImpl("Bushes", (IsoObject)null);
                this.emitter.setParameterValueByName(this.instance, "Occlusion", 0.0f);
            }
            this.square = square;
            this.playing = true;
            this.emitter.tick();
        }

        void stopPlaying() {
            if (this.emitter == null || this.instance == 0L) {
                if (this.emitter != null && !this.emitter.isEmpty()) {
                    this.emitter.tick();
                }
                return;
            }
            if (this.emitter.hasSustainPoints(this.instance)) {
                this.emitter.triggerCue(this.instance);
                this.instance = 0L;
                return;
            }
            this.emitter.stopAll();
            this.instance = 0L;
        }
    }
}

