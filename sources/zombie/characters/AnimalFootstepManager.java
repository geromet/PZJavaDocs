/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.BaseAnimalSoundManager;
import zombie.characters.animals.IsoAnimal;

public final class AnimalFootstepManager
extends BaseAnimalSoundManager {
    public static final AnimalFootstepManager instance = new AnimalFootstepManager();

    public AnimalFootstepManager() {
        super(20, 500);
    }

    @Override
    public void playSound(IsoAnimal chr) {
        chr.playNextFootstepSound();
    }

    @Override
    public void postUpdate() {
    }
}

