/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoObject;

public final class ParameterIsStashTile
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterIsStashTile(IsoGameCharacter character) {
        super("IsStashTile");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.isCharacterOnStashTile() ? 1.0f : 0.0f;
    }

    private boolean isCharacterOnStashTile() {
        if (this.character.getCurrentSquare() == null) {
            return false;
        }
        IsoObject stash = this.character.getCurrentSquare().getHiddenStash();
        return stash != null;
    }
}

