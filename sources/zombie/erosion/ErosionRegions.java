/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion;

import java.util.ArrayList;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.categories.Flowerbed;
import zombie.erosion.categories.NatureBush;
import zombie.erosion.categories.NatureGeneric;
import zombie.erosion.categories.NaturePlants;
import zombie.erosion.categories.NatureTrees;
import zombie.erosion.categories.StreetCracks;
import zombie.erosion.categories.WallCracks;
import zombie.erosion.categories.WallVines;

public final class ErosionRegions {
    public static final int REGION_NATURE = 0;
    public static final int CATEGORY_TREES = 0;
    public static final int CATEGORY_BUSH = 1;
    public static final int CATEGORY_PLANTS = 2;
    public static final int CATEGORY_GENERIC = 3;
    public static final int REGION_STREET = 1;
    public static final int CATEGORY_STREET_CRACKS = 0;
    public static final int REGION_WALL = 2;
    public static final int CATEGORY_WALL_VINES = 0;
    public static final int CATEGORY_WALL_CRACKS = 1;
    public static final int REGION_FLOWERBED = 3;
    public static final int CATEGORY_FLOWERBED = 0;
    public static final ArrayList<Region> regions = new ArrayList();

    private static void addRegion(Region region) {
        region.id = regions.size();
        regions.add(region);
    }

    public static ErosionCategory getCategory(int regionID, int categoryID) {
        return ErosionRegions.regions.get((int)regionID).categories.get(categoryID);
    }

    public static void init() {
        regions.clear();
        ErosionRegions.addRegion(new Region(0, "blends_natural_01", true, true, false).addCategory(0, new NatureTrees()).addCategory(1, new NatureBush()).addCategory(2, new NaturePlants()).addCategory(3, new NatureGeneric()));
        ErosionRegions.addRegion(new Region(1, "blends_street", true, true, false).addCategory(0, new StreetCracks()));
        ErosionRegions.addRegion(new Region(2, null, false, false, true).addCategory(0, new WallVines()).addCategory(1, new WallCracks()));
        ErosionRegions.addRegion(new Region(3, null, true, true, false).addCategory(0, new Flowerbed()));
        for (int i = 0; i < regions.size(); ++i) {
            regions.get(i).init();
        }
    }

    public static void Reset() {
        for (int i = 0; i < regions.size(); ++i) {
            regions.get(i).Reset();
        }
        regions.clear();
    }

    public static final class Region {
        public int id;
        public String tileNameMatch;
        public boolean checkExterior;
        public boolean isExterior;
        public boolean hasWall;
        public final ArrayList<ErosionCategory> categories = new ArrayList();

        public Region(int id, String tileMatch, boolean checkExt, boolean isExt, boolean hasWall) {
            this.id = id;
            this.tileNameMatch = tileMatch;
            this.checkExterior = checkExt;
            this.isExterior = isExt;
            this.hasWall = hasWall;
        }

        public Region addCategory(int id, ErosionCategory cat) {
            cat.id = id;
            cat.region = this;
            this.categories.add(cat);
            return this;
        }

        public void init() {
            for (int i = 0; i < this.categories.size(); ++i) {
                this.categories.get(i).init();
            }
        }

        public void Reset() {
            this.categories.clear();
        }
    }
}

