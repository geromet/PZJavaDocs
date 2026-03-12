/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.zones.RoomTone;

public final class ParameterRoomType
extends FMODGlobalParameter {
    static ParameterRoomType instance;
    static RoomType roomType;

    public ParameterRoomType() {
        super("RoomType");
        instance = this;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getRoomType().label;
    }

    private RoomType getRoomType() {
        if (roomType != null) {
            return roomType;
        }
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return RoomType.Generic;
        }
        BuildingDef buildingDef = character.getCurrentBuildingDef();
        if (buildingDef == null) {
            return RoomType.Generic;
        }
        if (character.getCurrentSquare().getZ() < 0) {
            if (character.getCurrentBuildingDef().getW() > 48 && character.getCurrentBuildingDef().getH() > 48) {
                return RoomType.Bunker;
            }
            return RoomType.Basement;
        }
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(PZMath.fastfloor(character.getX() / 256.0f), PZMath.fastfloor(character.getY() / 256.0f));
        if (metaCell == null || metaCell.roomTones.isEmpty()) {
            return RoomType.Generic;
        }
        RoomDef currentRoomDef = character.getCurrentRoomDef();
        RoomTone roomToneForBuilding = null;
        for (int i = 0; i < metaCell.roomTones.size(); ++i) {
            RoomTone roomTone = metaCell.roomTones.get(i);
            RoomDef roomDef = metaGrid.getRoomAt(roomTone.x, roomTone.y, roomTone.z);
            if (roomDef == null) continue;
            if (roomDef == currentRoomDef) {
                return RoomType.valueOf(roomTone.enumValue);
            }
            if (!roomTone.entireBuilding || roomDef.building != buildingDef) continue;
            roomToneForBuilding = roomTone;
        }
        if (roomToneForBuilding != null) {
            return RoomType.valueOf(roomToneForBuilding.enumValue);
        }
        return RoomType.Generic;
    }

    public static void setRoomType(int roomType) {
        try {
            ParameterRoomType.roomType = RoomType.values()[roomType];
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            ParameterRoomType.roomType = null;
        }
    }

    public static void render(IsoPlayer player) {
        if (instance == null) {
            return;
        }
        if (player != FMODParameterUtils.getFirstListener()) {
            return;
        }
        if (player != IsoCamera.frameState.camCharacter) {
            return;
        }
        player.drawDebugTextBelow("RoomType : " + instance.getRoomType().name());
    }

    private static enum RoomType {
        Generic(0),
        Barn(1),
        Mall(2),
        Warehouse(3),
        Prison(4),
        Church(5),
        Office(6),
        Factory(7),
        MovieTheater(8),
        Basement(9),
        Bunker(10),
        House(11),
        Commercial(12);

        final int label;

        private RoomType(int label) {
            this.label = label;
        }
    }
}

