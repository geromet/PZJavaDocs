/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.sprite.IsoSprite;

public final class IsoRoofFixer {
    private static final boolean PER_ROOM_MODE = true;
    private static final int MAX_Z = 8;
    private static final int SCAN_RANGE = 3;
    private static final boolean ALWAYS_INVIS_FLOORS = false;
    private static boolean roofTileGlassCacheDirty = true;
    private static boolean roofTileIsGlass;
    private static IsoSprite roofTileCache;
    private static int roofTilePlaceFloorIndexCache;
    private static final String invisFloor = "invisible_01_0";
    private static final Map<Integer, String> roofGroups;
    private static PlaceFloorInfo[] placeFloorInfos;
    private static int floorInfoIndex;
    private static final IsoGridSquare[] sqCache;
    private static IsoRoom workingRoom;
    private static final int[] interiorAirSpaces;
    private static final int I_UNCHECKED = 0;
    private static final int I_TRUE = 1;
    private static final int I_FALSE = 2;

    private static void ensureCapacityFloorInfos() {
        if (floorInfoIndex == placeFloorInfos.length) {
            PlaceFloorInfo[] old = placeFloorInfos;
            placeFloorInfos = new PlaceFloorInfo[placeFloorInfos.length + 400];
            System.arraycopy(old, 0, placeFloorInfos, 0, old.length);
        }
    }

    private static void setRoofTileCache(IsoObject object) {
        IsoSprite sprite;
        IsoSprite isoSprite = sprite = object != null ? object.sprite : null;
        if (roofTileCache != sprite) {
            roofTileCache = sprite;
            roofTilePlaceFloorIndexCache = 0;
            if (sprite != null && sprite.getProperties() != null && sprite.getProperties().get("RoofGroup") != null) {
                try {
                    int group = Integer.parseInt(sprite.getProperties().get("RoofGroup"));
                    if (roofGroups.containsKey(group)) {
                        roofTilePlaceFloorIndexCache = group;
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            roofTileGlassCacheDirty = true;
        }
    }

    private static boolean isRoofTileCacheGlass() {
        if (roofTileGlassCacheDirty) {
            PropertyContainer props;
            roofTileIsGlass = false;
            if (roofTileCache != null && (props = roofTileCache.getProperties()) != null) {
                String mat = props.get("Material");
                roofTileIsGlass = mat != null && mat.equalsIgnoreCase("glass");
            }
            roofTileGlassCacheDirty = false;
        }
        return roofTileIsGlass;
    }

    public static void FixRoofsAt(IsoGridSquare current) {
        try {
            IsoRoofFixer.FixRoofsPerRoomAt(current);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void FixRoofsPerRoomAt(IsoGridSquare current) {
        IsoRoom roomBelow;
        floorInfoIndex = 0;
        if (current.getZ() > 0 && !current.TreatAsSolidFloor() && current.getRoom() == null && (roomBelow = IsoRoofFixer.getRoomBelow(current)) != null && !roomBelow.def.isRoofFixed()) {
            IsoRoofFixer.resetInteriorSpaceCache();
            workingRoom = roomBelow;
            ArrayList<IsoGridSquare> squares = roomBelow.getSquares();
            for (int i = 0; i < squares.size(); ++i) {
                IsoGridSquare square = squares.get(i);
                IsoGridSquare test = IsoRoofFixer.getRoofFloorForColumn(square);
                if (test == null) continue;
                IsoRoofFixer.ensureCapacityFloorInfos();
                placeFloorInfos[floorInfoIndex++].set(test, roofTilePlaceFloorIndexCache);
            }
            roomBelow.def.setRoofFixed(true);
        }
        for (int i = 0; i < floorInfoIndex; ++i) {
            IsoRoofFixer.placeFloorInfos[i].square.addFloor(roofGroups.get(IsoRoofFixer.placeFloorInfos[i].floorType));
        }
    }

    private static void clearSqCache() {
        for (int i = 0; i < sqCache.length; ++i) {
            IsoRoofFixer.sqCache[i] = null;
        }
    }

    private static IsoGridSquare getRoofFloorForColumn(IsoGridSquare square) {
        IsoGridSquare test;
        if (square == null) {
            return null;
        }
        IsoCell cell = IsoCell.getInstance();
        int count = 0;
        boolean lastWasNull = false;
        for (int z = 7; z >= square.getZ() + 1; --z) {
            test = cell.getGridSquare(square.x, square.y, z);
            if (test == null) {
                if (z == square.getZ() + 1 && z > 0 && !IsoRoofFixer.isStairsBelow(square.x, square.y, z)) {
                    test = IsoGridSquare.getNew(cell, null, square.x, square.y, z);
                    cell.ConnectNewSquare(test, false);
                    test.EnsureSurroundNotNull();
                    test.RecalcAllWithNeighbours(true);
                    IsoRoofFixer.sqCache[count++] = test;
                }
                lastWasNull = true;
                continue;
            }
            if (test.TreatAsSolidFloor()) {
                IsoGridSquare below;
                PropertyContainer props;
                if (test.getRoom() != null) {
                    if (!lastWasNull) break;
                    test = IsoGridSquare.getNew(cell, null, square.x, square.y, z + 1);
                    cell.ConnectNewSquare(test, false);
                    test.EnsureSurroundNotNull();
                    test.RecalcAllWithNeighbours(true);
                    IsoRoofFixer.sqCache[count++] = test;
                    break;
                }
                IsoObject floor = test.getFloor();
                if (floor != null && IsoRoofFixer.isObjectRoof(floor) && floor.getProperties() != null && !(props = floor.getProperties()).has(IsoFlagType.FloorHeightOneThird) && !props.has(IsoFlagType.FloorHeightTwoThirds) && (below = cell.getGridSquare(square.x, square.y, z - 1)) != null && below.getRoom() == null) {
                    lastWasNull = false;
                    continue;
                }
                return null;
            }
            if (test.HasStairsBelow()) break;
            lastWasNull = false;
            IsoRoofFixer.sqCache[count++] = test;
        }
        if (count == 0) {
            return null;
        }
        boolean checkAbove = true;
        for (int index = 0; index < count; ++index) {
            test = sqCache[index];
            if (test.getRoom() == null && IsoRoofFixer.isInteriorAirSpace(test.getX(), test.getY(), test.getZ())) {
                return null;
            }
            if (IsoRoofFixer.isRoofAt(test, true)) {
                return test;
            }
            for (int x = test.x - 3; x <= test.x + 3; ++x) {
                for (int y = test.y - 3; y <= test.y + 3; ++y) {
                    IsoObject obj;
                    IsoGridSquare gs;
                    if (x == test.x && y == test.y || (gs = cell.getGridSquare(x, y, test.z)) == null) continue;
                    for (int i = 0; i < gs.getObjects().size(); ++i) {
                        obj = gs.getObjects().get(i);
                        if (!IsoRoofFixer.isObjectRoofNonFlat(obj)) continue;
                        IsoRoofFixer.setRoofTileCache(obj);
                        return test;
                    }
                    IsoGridSquare above = cell.getGridSquare(gs.x, gs.y, gs.z + 1);
                    if (above == null || above.getObjects().isEmpty()) continue;
                    for (int i = 0; i < above.getObjects().size(); ++i) {
                        obj = above.getObjects().get(i);
                        if (!IsoRoofFixer.isObjectRoofFlatFloor(obj)) continue;
                        IsoRoofFixer.setRoofTileCache(obj);
                        return test;
                    }
                }
            }
        }
        return null;
    }

    private static void FixRoofsPerTileAt(IsoGridSquare current) {
        if (current.getZ() > 0 && !current.TreatAsSolidFloor() && current.getRoom() == null && IsoRoofFixer.hasRoomBelow(current) && (IsoRoofFixer.isRoofAt(current, true) || IsoRoofFixer.scanIsRoofAt(current, true))) {
            if (IsoRoofFixer.isRoofTileCacheGlass()) {
                current.addFloor(invisFloor);
            } else {
                current.addFloor("carpentry_02_58");
            }
        }
    }

    private static boolean scanIsRoofAt(IsoGridSquare square, boolean checkAbove) {
        if (square == null) {
            return false;
        }
        for (int x = square.x - 3; x <= square.x + 3; ++x) {
            for (int y = square.y - 3; y <= square.y + 3; ++y) {
                IsoGridSquare gs;
                if (x == square.x && y == square.y || (gs = square.getCell().getGridSquare(x, y, square.z)) == null || !IsoRoofFixer.isRoofAt(gs, checkAbove)) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean isRoofAt(IsoGridSquare square, boolean checkAbove) {
        IsoGridSquare above;
        IsoObject obj;
        if (square == null) {
            return false;
        }
        for (int i = 0; i < square.getObjects().size(); ++i) {
            obj = square.getObjects().get(i);
            if (!IsoRoofFixer.isObjectRoofNonFlat(obj)) continue;
            IsoRoofFixer.setRoofTileCache(obj);
            return true;
        }
        if (checkAbove && (above = square.getCell().getGridSquare(square.x, square.y, square.z + 1)) != null && !above.getObjects().isEmpty()) {
            for (int i = 0; i < above.getObjects().size(); ++i) {
                obj = above.getObjects().get(i);
                if (!IsoRoofFixer.isObjectRoofFlatFloor(obj)) continue;
                IsoRoofFixer.setRoofTileCache(obj);
                return true;
            }
        }
        return false;
    }

    private static boolean isObjectRoof(IsoObject object) {
        return object != null && (object.getType() == IsoObjectType.WestRoofT || object.getType() == IsoObjectType.WestRoofB || object.getType() == IsoObjectType.WestRoofM);
    }

    private static boolean isObjectRoofNonFlat(IsoObject object) {
        PropertyContainer props;
        if (IsoRoofFixer.isObjectRoof(object) && (props = object.getProperties()) != null) {
            return !props.has(IsoFlagType.solidfloor) || props.has(IsoFlagType.FloorHeightOneThird) || props.has(IsoFlagType.FloorHeightTwoThirds);
        }
        return false;
    }

    private static boolean isObjectRoofFlatFloor(IsoObject object) {
        PropertyContainer props;
        if (IsoRoofFixer.isObjectRoof(object) && (props = object.getProperties()) != null && props.has(IsoFlagType.solidfloor)) {
            return !props.has(IsoFlagType.FloorHeightOneThird) && !props.has(IsoFlagType.FloorHeightTwoThirds);
        }
        return false;
    }

    private static boolean hasRoomBelow(IsoGridSquare current) {
        return IsoRoofFixer.getRoomBelow(current) != null;
    }

    private static IsoRoom getRoomBelow(IsoGridSquare current) {
        if (current == null) {
            return null;
        }
        for (int z = current.z - 1; z >= 0; --z) {
            IsoGridSquare testSq = current.getCell().getGridSquare(current.x, current.y, z);
            if (testSq == null) continue;
            if (testSq.TreatAsSolidFloor() && testSq.getRoom() == null) {
                return null;
            }
            if (testSq.getRoom() == null) continue;
            return testSq.getRoom();
        }
        return null;
    }

    private static boolean isStairsBelow(int x, int y, int z) {
        if (z == 0) {
            return false;
        }
        IsoCell cell = IsoCell.getInstance();
        IsoGridSquare square = cell.getGridSquare(x, y, z - 1);
        return square != null && square.HasStairs();
    }

    private static void resetInteriorSpaceCache() {
        for (int i = 0; i < interiorAirSpaces.length; ++i) {
            IsoRoofFixer.interiorAirSpaces[i] = 0;
        }
    }

    private static boolean isInteriorAirSpace(int sx, int sy, int sz) {
        if (interiorAirSpaces[sz] != 0) {
            return interiorAirSpaces[sz] == 1;
        }
        ArrayList<IsoGridSquare> squares = workingRoom.getSquares();
        boolean hasRailing = false;
        if (!squares.isEmpty() && sz > squares.get(0).getZ()) {
            block0: for (int i = 0; i < IsoRoofFixer.workingRoom.rects.size(); ++i) {
                RoomDef.RoomRect rect = IsoRoofFixer.workingRoom.rects.get(i);
                for (int x = rect.getX(); x < rect.getX2(); ++x) {
                    if (!IsoRoofFixer.hasRailing(x, rect.getY(), sz, IsoDirections.N) && !IsoRoofFixer.hasRailing(x, rect.getY2() - 1, sz, IsoDirections.S)) continue;
                    hasRailing = true;
                    break;
                }
                if (hasRailing) break;
                for (int y = rect.getY(); y < rect.getY2(); ++y) {
                    if (!IsoRoofFixer.hasRailing(rect.getX(), y, sz, IsoDirections.W) && !IsoRoofFixer.hasRailing(rect.getX2() - 1, y, sz, IsoDirections.E)) continue;
                    hasRailing = true;
                    continue block0;
                }
            }
        }
        IsoRoofFixer.interiorAirSpaces[sz] = hasRailing ? 1 : 2;
        return hasRailing;
    }

    private static boolean hasRailing(int x, int y, int z, IsoDirections dir) {
        IsoCell cell = IsoCell.getInstance();
        IsoGridSquare curr = cell.getGridSquare(x, y, z);
        if (curr == null) {
            return false;
        }
        switch (dir) {
            case N: {
                return curr.isHoppableTo(cell.getGridSquare(x, y - 1, z));
            }
            case E: {
                return curr.isHoppableTo(cell.getGridSquare(x + 1, y, z));
            }
            case S: {
                return curr.isHoppableTo(cell.getGridSquare(x, y + 1, z));
            }
            case W: {
                return curr.isHoppableTo(cell.getGridSquare(x - 1, y, z));
            }
        }
        return false;
    }

    static {
        roofGroups = new HashMap<Integer, String>();
        placeFloorInfos = new PlaceFloorInfo[10000];
        roofGroups.put(0, "carpentry_02_57");
        roofGroups.put(1, "roofs_01_22");
        roofGroups.put(2, "roofs_01_54");
        roofGroups.put(3, "roofs_02_22");
        roofGroups.put(4, invisFloor);
        roofGroups.put(5, "roofs_03_22");
        roofGroups.put(6, "roofs_03_54");
        roofGroups.put(7, "roofs_04_22");
        roofGroups.put(8, "roofs_04_54");
        roofGroups.put(9, "roofs_05_22");
        roofGroups.put(10, "roofs_05_54");
        for (int i = 0; i < placeFloorInfos.length; ++i) {
            IsoRoofFixer.placeFloorInfos[i] = new PlaceFloorInfo();
        }
        sqCache = new IsoGridSquare[8];
        interiorAirSpaces = new int[8];
    }

    private static final class PlaceFloorInfo {
        private IsoGridSquare square;
        private int floorType;

        private PlaceFloorInfo() {
        }

        private void set(IsoGridSquare s, int t) {
            this.square = s;
            this.floorType = t;
        }
    }
}

