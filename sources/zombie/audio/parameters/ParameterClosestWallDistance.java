/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.iso.NearestWalls;

public final class ParameterClosestWallDistance
extends FMODGlobalParameter {
    public ParameterClosestWallDistance() {
        super("ClosestWallDistance");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 127.0f;
        }
        return NearestWalls.ClosestWallDistance(character.getCurrentSquare(), false);
    }
}

