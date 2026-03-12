/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDoor;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;

public class IsoObjectUtils {
    private static final boolean THROW_ERROR = false;
    private static final ThreadLocal<ArrayList<IsoObject>> threadLocalObjects = ThreadLocal.withInitial(ArrayList::new);

    public static boolean isObjectMultiSquare(IsoObject object) {
        if (object == null) {
            return false;
        }
        if (IsoDoor.getDoubleDoorIndex(object) != -1 || IsoDoor.getGarageDoorIndex(object) != -1) {
            return true;
        }
        if (object.getSpriteConfig() != null && object.getSpriteConfig().isValidMultiSquare()) {
            return true;
        }
        return object.getSprite() != null && object.getSprite().getSpriteGrid() != null;
    }

    public static int safelyRemoveTileObjectFromSquare(IsoObject object) {
        if (IsoObjectUtils.isObjectMultiSquare(object)) {
            ArrayList<IsoObject> objects = threadLocalObjects.get();
            objects.clear();
            if (!IsoObjectUtils.getAllMultiTileObjects(object, objects)) {
                if (Core.debug) {
                    DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
                }
                return -1;
            }
            int objectIndex = -1;
            for (IsoObject obj : objects) {
                IsoGridSquare sq = obj.square;
                if (sq != null) {
                    int idx = sq.RemoveTileObject(obj, false);
                    if (object != obj) continue;
                    objectIndex = idx;
                    continue;
                }
                if (!Core.debug) continue;
                DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
            }
            return objectIndex;
        }
        if (object != null && object.square != null) {
            return object.square.RemoveTileObject(object, false);
        }
        return -1;
    }

    public static boolean getAllMultiTileObjects(IsoObject object, ArrayList<IsoObject> outList) {
        int ddIndex = IsoDoor.getDoubleDoorIndex(object);
        if (ddIndex != -1) {
            IsoObject object1 = IsoDoor.getDoubleDoorObject(object, ddIndex);
            IsoObject object2 = IsoDoor.getDoubleDoorObject(object, IsoDoor.getDoubleDoorPartnerIndex(ddIndex));
            if (object1 != null) {
                outList.add(object1);
            }
            if (object2 != null) {
                outList.add(object2);
            }
            return !outList.isEmpty();
        }
        int gdIndex = IsoDoor.getGarageDoorIndex(object);
        if (gdIndex != -1) {
            IsoObject object1 = IsoDoor.getGarageDoorFirst(object);
            while (object1 != null) {
                outList.add(object1);
                object1 = IsoDoor.getGarageDoorNext(object1);
            }
            return !outList.isEmpty();
        }
        if (object.getSpriteConfig() != null && object.getSpriteConfig().isValidMultiSquare()) {
            return object.getSpriteConfig().getAllMultiSquareObjects(outList);
        }
        if (object.getSprite() != null && object.getSprite().getSpriteGrid() != null) {
            return IsoObjectUtils.getSpriteGridMultiTileObjects(object, outList);
        }
        return false;
    }

    private static boolean getSpriteGridMultiTileObjects(IsoObject object, ArrayList<IsoObject> outList) {
        if (object.getSprite() == null || object.getSprite().getSpriteGrid() == null) {
            return false;
        }
        IsoGridSquare origSquare = object.square;
        IsoSpriteGrid spriteGrid = object.getSpriteGrid();
        int ox = spriteGrid.getSpriteGridPosX(object.getSprite());
        int oy = spriteGrid.getSpriteGridPosY(object.getSprite());
        int oz = spriteGrid.getSpriteGridPosZ(object.getSprite());
        for (int z = 0; z < spriteGrid.getLevels(); ++z) {
            for (int x = 0; x < spriteGrid.getWidth(); ++x) {
                for (int y = 0; y < spriteGrid.getHeight(); ++y) {
                    int cx = x - ox;
                    int cy = y - oy;
                    int cz = z - oz;
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(origSquare.x + cx, origSquare.y + cy, origSquare.z + cz);
                    IsoSprite testSprite = spriteGrid.getSprite(x, y, z);
                    boolean multiFound = false;
                    if (sq != null) {
                        for (int i = 0; i < sq.getObjects().size(); ++i) {
                            IsoObject sqObj = sq.getObjects().get(i);
                            if (!IsoObjectUtils.verifyObject(sqObj, testSprite)) continue;
                            outList.add(sqObj);
                            multiFound = true;
                            break;
                        }
                    }
                    if (multiFound) continue;
                    outList.clear();
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean verifyObject(IsoObject object, IsoSprite testSprite) {
        if (object.getSprite() != null) {
            return object.getSprite() == testSprite;
        }
        return false;
    }

    public static void dumpContentsInSquare(IsoObject object) {
        if (object.getSquare() == null) {
            return;
        }
        for (int i = 0; i < object.getContainerCount(); ++i) {
            ItemContainer container = object.getContainerByIndex(i);
            container.dumpContentsInSquare(object.getSquare());
        }
        if (IsoObjectUtils.isObjectMultiSquare(object)) {
            ArrayList<IsoObject> objects = threadLocalObjects.get();
            objects.clear();
            if (!IsoObjectUtils.getAllMultiTileObjects(object, objects)) {
                if (Core.debug) {
                    DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
                }
                return;
            }
            for (IsoObject obj : objects) {
                IsoGridSquare sq = obj.square;
                if (sq == null) continue;
                for (int i = 0; i < obj.getContainerCount(); ++i) {
                    obj.getContainerByIndex(i).dumpContentsInSquare(sq);
                }
            }
        }
        ArrayList<IsoObject> multiSquareObjects = new ArrayList<IsoObject>();
        if (object.hasComponent(ComponentType.SpriteConfig)) {
            ((SpriteConfig)object.getComponent(ComponentType.SpriteConfig)).getAllMultiSquareObjects(multiSquareObjects);
        } else {
            multiSquareObjects.add(object);
        }
        for (IsoObject subObject : multiSquareObjects) {
            for (int i = 0; i < subObject.componentSize(); ++i) {
                Component component = subObject.getComponentForIndex(i);
                if (component == null) continue;
                component.dumpContentsInSquare();
            }
        }
    }
}

