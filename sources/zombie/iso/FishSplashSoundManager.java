/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.Comparator;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;

public class FishSplashSoundManager {
    public static final FishSplashSoundManager instance = new FishSplashSoundManager();
    private final ArrayList<IsoGridSquare> squares = new ArrayList();
    private final long[] soundTime = new long[6];
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

    public void addSquare(IsoGridSquare square) {
        if (!this.squares.contains(square)) {
            this.squares.add(square);
        }
    }

    public void update() {
        if (this.squares.isEmpty()) {
            return;
        }
        this.squares.sort(this.comp);
        long ms = System.currentTimeMillis();
        for (int i = 0; i < this.soundTime.length && i < this.squares.size(); ++i) {
            IsoGridSquare square = this.squares.get(i);
            if (this.getClosestListener((float)square.x + 0.5f, (float)square.y + 0.5f, square.z) > 20.0f) continue;
            int slot = this.getFreeSoundSlot(ms);
            if (slot == -1) break;
            square.playSoundLocal("FishBreath");
            this.soundTime[slot] = ms;
        }
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

    private int getFreeSoundSlot(long ms) {
        long oldestTime = Long.MAX_VALUE;
        int oldestIndex = -1;
        for (int i = 0; i < this.soundTime.length; ++i) {
            if (this.soundTime[i] >= oldestTime) continue;
            oldestTime = this.soundTime[i];
            oldestIndex = i;
        }
        if (ms - oldestTime < 3000L) {
            return -1;
        }
        return oldestIndex;
    }
}

