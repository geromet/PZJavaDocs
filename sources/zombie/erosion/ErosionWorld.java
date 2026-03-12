/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion;

import zombie.erosion.ErosionData;
import zombie.erosion.ErosionRegions;
import zombie.erosion.categories.ErosionCategory;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;

public final class ErosionWorld {
    public boolean init() {
        ErosionRegions.init();
        return true;
    }

    public void validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData) {
        String sqTexName;
        boolean isExterior = square.has(IsoFlagType.exterior);
        boolean hasWall = square.has(IsoObjectType.wall);
        IsoObject floor = square.getFloor();
        String string = sqTexName = floor != null && floor.getSprite() != null ? floor.getSprite().getName() : null;
        if (sqTexName == null) {
            sqErosionData.doNothing = true;
            return;
        }
        boolean hasSpawned = false;
        block0: for (int i = 0; i < ErosionRegions.regions.size(); ++i) {
            ErosionRegions.Region region = ErosionRegions.regions.get(i);
            String m = region.tileNameMatch;
            if (m != null && !sqTexName.startsWith(m) || region.checkExterior && region.isExterior != isExterior || region.hasWall && region.hasWall != hasWall) continue;
            for (int j = 0; j < region.categories.size(); ++j) {
                ErosionCategory category = region.categories.get(j);
                boolean spawned = category.replaceExistingObject(square, sqErosionData, chunkData, isExterior, hasWall);
                if (!spawned) {
                    spawned = category.validateSpawn(square, sqErosionData, chunkData, isExterior, hasWall, false);
                }
                if (!spawned) continue;
                hasSpawned = true;
                continue block0;
            }
        }
        if (!hasSpawned) {
            sqErosionData.doNothing = true;
        }
    }

    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, int eTick) {
        if (sqErosionData.regions == null) {
            return;
        }
        for (int i = 0; i < sqErosionData.regions.size(); ++i) {
            ErosionCategory.Data sqCategoryData = sqErosionData.regions.get(i);
            ErosionCategory category = ErosionRegions.getCategory(sqCategoryData.regionId, sqCategoryData.categoryId);
            int size = sqErosionData.regions.size();
            category.update(square, sqErosionData, sqCategoryData, chunkData, eTick);
            if (size <= sqErosionData.regions.size()) continue;
            --i;
        }
    }
}

