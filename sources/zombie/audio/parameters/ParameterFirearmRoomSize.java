/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmRoomSize
extends FMODLocalParameter {
    private final IsoPlayer character;

    public ParameterFirearmRoomSize(IsoPlayer character) {
        super("FirearmRoomSize");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        Object roomShooter = this.getRoom(this.character);
        if (roomShooter == null) {
            return 0.0f;
        }
        return this.getRoomSize(roomShooter);
    }

    private Object getRoom(IsoPlayer character) {
        IsoGridSquare square = character.getCurrentSquare();
        if (square == null) {
            return null;
        }
        IsoRoom room = square.getRoom();
        if (room == null) {
            IWorldRegion worldRegion = square.getIsoWorldRegion();
            return worldRegion != null && worldRegion.isPlayerRoom() ? worldRegion : null;
        }
        return room;
    }

    private float getRoomSize(Object room) {
        if (room instanceof IsoRoom) {
            IsoRoom isoRoom = (IsoRoom)room;
            return isoRoom.getRoomDef().getArea();
        }
        return ((IWorldRegion)room).getSquareSize();
    }
}

