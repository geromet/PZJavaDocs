/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;

public abstract class BaseZombieSoundManager {
    protected final ArrayList<IsoZombie> characters = new ArrayList();
    private final long[] soundTime;
    private final int staleSlotMs;
    private final Comparator<IsoZombie> comp = new Comparator<IsoZombie>(this){
        final /* synthetic */ BaseZombieSoundManager this$0;
        {
            BaseZombieSoundManager baseZombieSoundManager = this$0;
            Objects.requireNonNull(baseZombieSoundManager);
            this.this$0 = baseZombieSoundManager;
        }

        @Override
        public int compare(IsoZombie a, IsoZombie b) {
            float bScore;
            float aScore = this.this$0.getClosestListener(a.getX(), a.getY(), a.getZ());
            if (aScore > (bScore = this.this$0.getClosestListener(b.getX(), b.getY(), b.getZ()))) {
                return 1;
            }
            if (aScore < bScore) {
                return -1;
            }
            return 0;
        }
    };

    public BaseZombieSoundManager(int numSlots, int staleSlotMs) {
        this.soundTime = new long[numSlots];
        this.staleSlotMs = staleSlotMs;
    }

    public void addCharacter(IsoZombie chr) {
        if (!this.characters.contains(chr)) {
            this.characters.add(chr);
        }
    }

    public void update() {
        if (this.characters.isEmpty()) {
            return;
        }
        this.characters.sort(this.comp);
        long ms = System.currentTimeMillis();
        for (int i = 0; i < this.soundTime.length && i < this.characters.size(); ++i) {
            IsoZombie chr = this.characters.get(i);
            if (chr.getCurrentSquare() == null) continue;
            int slot = this.getFreeSoundSlot(ms);
            if (slot == -1) break;
            this.playSound(chr);
            this.soundTime[slot] = ms;
        }
        this.postUpdate();
        this.characters.clear();
    }

    public abstract void playSound(IsoZombie var1);

    public abstract void postUpdate();

    private float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr == null || chr.getCurrentSquare() == null) continue;
            float px = chr.getX();
            float py = chr.getY();
            float pz = chr.getZ();
            float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
            if (!((distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0f)) < minDist)) continue;
            minDist = distSq;
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
        if (ms - oldestTime < (long)this.staleSlotMs) {
            return -1;
        }
        return oldestIndex;
    }
}

