/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.ai.ZombieGroupManager;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;

public final class ZombieGroup {
    private final ArrayList<IsoZombie> members = new ArrayList();
    public float lastSpreadOutTime;
    public float idealSizeFactor = 1.0f;
    int randomizer;
    boolean negativeValue;

    public ZombieGroup() {
        this.randomizer = Rand.Next(SandboxOptions.instance.zombieConfig.rallyGroupSizeVariance.getValue());
        this.negativeValue = Rand.NextBool(2);
    }

    public ZombieGroup reset() {
        this.members.clear();
        this.lastSpreadOutTime = -1.0f;
        this.idealSizeFactor = 1.0f;
        int random = Rand.Next(this.randomizer);
        if (Rand.NextBool(2)) {
            random *= -1;
        }
        this.idealSizeFactor += (float)random / 100.0f;
        if (this.idealSizeFactor < 0.01f) {
            this.idealSizeFactor = 0.01f;
        }
        return this;
    }

    public void add(IsoZombie zombie) {
        if (this.members.contains(zombie)) {
            return;
        }
        if (zombie.group != null) {
            zombie.group.remove(zombie);
        }
        this.members.add(zombie);
        zombie.group = this;
    }

    public void remove(IsoZombie zombie) {
        this.members.remove(zombie);
        zombie.group = null;
    }

    public IsoZombie getLeader() {
        return this.members.isEmpty() ? null : this.members.get(0);
    }

    public boolean isEmpty() {
        return this.members.isEmpty();
    }

    public int size() {
        return this.members.size();
    }

    public void update() {
        int rallyDist = SandboxOptions.instance.zombieConfig.rallyTravelDistance.getValue();
        for (int i = 0; i < this.members.size(); ++i) {
            IsoZombie zombie = this.members.get(i);
            float distSq = 0.0f;
            if (i > 0) {
                distSq = IsoUtils.DistanceToSquared(this.members.get(0).getX(), this.members.get(0).getY(), zombie.getX(), zombie.getY());
            }
            if (zombie.group == this && !(distSq > (float)(rallyDist * rallyDist)) && ZombieGroupManager.instance.shouldBeInGroup(zombie)) continue;
            if (zombie.group == this) {
                zombie.group = null;
            }
            this.members.remove(i--);
        }
    }
}

