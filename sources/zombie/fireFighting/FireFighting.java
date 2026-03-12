/*
 * Decompiled with CFR 0.152.
 */
package zombie.fireFighting;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoFire;

public class FireFighting {
    public static boolean isSquareToExtinguish(IsoGridSquare square) {
        int i;
        if (square == null) {
            return false;
        }
        if (square.has(IsoFlagType.burning)) {
            for (i = 0; i < square.getObjects().size(); ++i) {
                IsoFire isoFire;
                IsoObject object = square.getObjects().get(i);
                if (!(object instanceof IsoFire) || (isoFire = (IsoFire)object).isPermanent()) continue;
                return true;
            }
        }
        for (i = 0; i < square.getMovingObjects().size(); ++i) {
            IsoGameCharacter isoGameCharacter;
            IsoMovingObject movingObj = square.getMovingObjects().get(i);
            if (!(movingObj instanceof IsoGameCharacter) || !(isoGameCharacter = (IsoGameCharacter)movingObj).isOnFire()) continue;
            return true;
        }
        return false;
    }

    public static IsoGridSquare getSquareToExtinguish(IsoGridSquare square) {
        if (FireFighting.isSquareToExtinguish(square)) {
            return square;
        }
        int x = square.getX();
        int y = square.getY();
        int z = square.getZ();
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                if (dx == 0 && dy == 0 || !FireFighting.isSquareToExtinguish(square = IsoWorld.instance.getCell().getGridSquare(x + dx, y + dy, z))) continue;
                return square;
            }
        }
        return null;
    }

    public static InventoryItem getExtinguisher(IsoPlayer playerObj) {
        InventoryItem primary = playerObj.getPrimaryHandItem();
        if (FireFighting.isExtinguisher(primary)) {
            return primary;
        }
        InventoryItem secondary = playerObj.getSecondaryHandItem();
        if (FireFighting.isExtinguisher(secondary)) {
            return secondary;
        }
        InventoryItem extinguisher = null;
        InventoryItem bagOfX = null;
        InventoryItem waterSource = null;
        for (int i = 0; i < playerObj.getInventory().getItems().size(); ++i) {
            InventoryItem item = playerObj.getInventory().getItems().get(i);
            if (!FireFighting.isExtinguisher(item)) continue;
            if (item.isWaterSource() && waterSource == null) {
                waterSource = item;
            }
            if (item.getType().equals("Extinguisher") && extinguisher == null) {
                extinguisher = item;
            }
            if (!item.getType().equals("Sandbag") && !item.getType().equals("Gravelbag") && !item.getType().equals("Dirtbag") || bagOfX != null) continue;
            bagOfX = item;
        }
        if (extinguisher != null) {
            return extinguisher;
        }
        if (bagOfX != null) {
            return bagOfX;
        }
        return waterSource;
    }

    public static boolean isExtinguisher(InventoryItem item) {
        if (item == null) {
            return false;
        }
        if (item.getType().equals("Extinguisher")) {
            return item.getCurrentUses() >= FireFighting.getExtinguisherUses(item);
        }
        if (item.getType().equals("Sandbag") || item.getType().equals("Gravelbag") || item.getType().equals("Dirtbag")) {
            return item.getCurrentUses() >= FireFighting.getExtinguisherUses(item);
        }
        if (item.isWaterSource()) {
            return FireFighting.getWaterUsesInteger(item) >= FireFighting.getExtinguisherUses(item);
        }
        return false;
    }

    public static int getExtinguisherUses(InventoryItem item) {
        if (item == null) {
            return 10000;
        }
        if (item.getType().equals("Extinguisher")) {
            return 1;
        }
        if (item.getType().equals("Sandbag") || item.getType().equals("Gravelbag") || item.getType().equals("Dirtbag")) {
            return 1;
        }
        if (item.isWaterSource()) {
            return 10;
        }
        return 10000;
    }

    public static int getWaterUsesInteger(InventoryItem item) {
        float fluidContainerMillilitresPerUse = 100.0f;
        if (item == null) {
            return 0;
        }
        if (item.hasComponent(ComponentType.FluidContainer)) {
            FluidContainer fluidContainer = item.getFluidContainer();
            if (fluidContainer.getPrimaryFluid() == null) {
                return 0;
            }
            FluidType fluidTypeString = fluidContainer.getPrimaryFluid().getFluidType();
            if (fluidTypeString == FluidType.Water || fluidTypeString == FluidType.TaintedWater) {
                float millilitres = fluidContainer.getAmount() * 1000.0f;
                return (int)Math.floor(millilitres / 100.0f);
            }
        }
        if (item.IsDrainable() && item.isWaterSource()) {
            return item.getCurrentUses();
        }
        return 0;
    }
}

