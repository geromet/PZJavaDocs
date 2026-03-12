/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.BaseZombieSoundManager;
import zombie.characters.IsoZombie;

public final class ZombieFootstepManager
extends BaseZombieSoundManager {
    public static final ZombieFootstepManager instance = new ZombieFootstepManager();

    public ZombieFootstepManager() {
        super(40, 500);
    }

    @Override
    public void playSound(IsoZombie chr) {
        chr.getEmitter().playFootsteps("ZombieFootstepsCombined", chr.getFootstepVolume());
    }

    @Override
    public void postUpdate() {
    }
}

