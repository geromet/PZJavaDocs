/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;

public final class ParameterRoomSize
extends FMODGlobalParameter {
    public ParameterRoomSize() {
        super("RoomSize");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 0.0f;
        }
        RoomDef roomDef = character.getCurrentRoomDef();
        if (roomDef != null) {
            return roomDef.getArea();
        }
        IsoGridSquare gs = character.getCurrentSquare();
        if (gs != null && gs.isInARoom()) {
            return gs.getRoomSize();
        }
        return 0.0f;
    }
}

