/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugOptions;
import zombie.iso.IsoGridSquare;
import zombie.iso.SpriteDetails.IsoObjectType;

public final class ParameterOverlapFoliageType
extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterOverlapFoliageType(IsoGameCharacter character) {
        super("OverlapFoliageType");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getFoliageType().ordinal();
    }

    private FoliageType getFoliageType() {
        IsoGridSquare current = this.character.getCurrentSquare();
        if (current == null) {
            return FoliageType.None;
        }
        if (this.character.isInvisible() && !DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            return FoliageType.None;
        }
        if (current.has(IsoObjectType.tree) || current.hasBush()) {
            return FoliageType.Bush;
        }
        return FoliageType.None;
    }

    static enum FoliageType {
        None,
        Bush;

    }
}

