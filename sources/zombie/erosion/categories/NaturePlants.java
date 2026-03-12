/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class NaturePlants
extends ErosionCategory {
    private final int[][] soilRef = new int[][]{{17, 17, 17, 17, 17, 17, 17, 17, 17, 1, 2, 8, 8}, {11, 12, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8}, {11, 12, 11, 12, 11, 12, 11, 12, 15, 16, 18, 19}, {22, 22, 22, 22, 22, 22, 22, 22, 22, 3, 4, 14}, {15, 16, 3, 4, 14, 3, 4, 14, 3, 4, 14, 3, 4, 14}, {11, 12, 15, 16, 15, 16, 15, 16, 15, 16, 21}, {13, 13, 13, 13, 13, 13, 13, 13, 13, 5, 6, 24}, {18, 19, 5, 6, 24, 5, 6, 24, 5, 6, 24, 5, 6, 24}, {18, 19, 18, 19, 18, 19, 18, 19, 20, 21}, {7, 7, 7, 7, 7, 7, 7, 7, 7, 9, 10, 23}, {19, 20, 9, 10, 23, 9, 10, 23, 9, 10, 23, 9, 10, 23}, {15, 16, 18, 19, 20, 19, 20, 19, 20}};
    private final int[] spawnChance = new int[100];
    private final ArrayList<ErosionObj> objs = new ArrayList();
    private final PlantInit[] plants = new PlantInit[]{new PlantInit("Butterfly Weed", true, 0.05f, 0.25f), new PlantInit("Butterfly Weed", true, 0.05f, 0.25f), new PlantInit("Swamp Sunflower", true, 0.2f, 0.45f), new PlantInit("Swamp Sunflower", true, 0.2f, 0.45f), new PlantInit("Purple Coneflower", true, 0.1f, 0.35f), new PlantInit("Purple Coneflower", true, 0.1f, 0.35f), new PlantInit("Joe-Pye Weed", true, 0.8f, 1.0f), new PlantInit("Blazing Star", true, 0.25f, 0.65f), new PlantInit("Wild Bergamot", true, 0.45f, 0.6f), new PlantInit("Wild Bergamot", true, 0.45f, 0.6f), new PlantInit("White Beard-tongue", true, 0.2f, 0.65f), new PlantInit("White Beard-tongue", true, 0.2f, 0.65f), new PlantInit("Ironweed", true, 0.75f, 0.85f), new PlantInit("White Baneberry", true, 0.4f, 0.8f), new PlantInit("Wild Columbine", true, 0.85f, 1.0f), new PlantInit("Wild Columbine", true, 0.85f, 1.0f), new PlantInit("Jack-in-the-pulpit", false, 0.0f, 0.0f), new PlantInit("Wild Ginger", true, 0.1f, 0.9f), new PlantInit("Wild Ginger", true, 0.1f, 0.9f), new PlantInit("Wild Geranium", true, 0.65f, 0.9f), new PlantInit("Alumroot", true, 0.35f, 0.75f), new PlantInit("Wild Blue Phlox", true, 0.15f, 0.55f), new PlantInit("Polemonium Reptans", true, 0.4f, 0.6f), new PlantInit("Foamflower", true, 0.45f, 1.0f)};

    @Override
    public boolean replaceExistingObject(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall) {
        int objsSize = square.getObjects().size();
        for (int i = objsSize - 1; i >= 1; --i) {
            IsoObject obj = square.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr == null || spr.getName() == null) continue;
            if (spr.getName().startsWith("d_plants_1_")) {
                int id = Integer.parseInt(spr.getName().replace("d_plants_1_", ""));
                CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = id < 32 ? id % 8 : (id < 48 ? id % 8 + 8 : id % 8 + 16);
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 0;
                square.RemoveTileObjectErosionNoRecalc(obj);
                return true;
            }
            if ("vegetation_groundcover_01_16".equals(spr.getName()) || "vegetation_groundcover_01_17".equals(spr.getName())) {
                CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = 21;
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 0;
                square.RemoveTileObjectErosionNoRecalc(obj);
                while (--i > 0) {
                    obj = square.getObjects().get(i);
                    spr = obj.getSprite();
                    if (spr == null || spr.getName() == null || !spr.getName().startsWith("vegetation_groundcover_01_")) continue;
                    square.RemoveTileObjectErosionNoRecalc(obj);
                }
                return true;
            }
            if (!"vegetation_groundcover_01_18".equals(spr.getName()) && !"vegetation_groundcover_01_19".equals(spr.getName()) && !"vegetation_groundcover_01_20".equals(spr.getName()) && !"vegetation_groundcover_01_21".equals(spr.getName()) && !"vegetation_groundcover_01_22".equals(spr.getName()) && !"vegetation_groundcover_01_23".equals(spr.getName())) continue;
            CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
            sqCategoryData.gameObj = sqErosionData.rand(square.x, square.y, this.plants.length);
            sqCategoryData.stage = 0;
            sqCategoryData.spawnTime = 0;
            square.RemoveTileObjectErosionNoRecalc(obj);
            while (--i > 0) {
                obj = square.getObjects().get(i);
                spr = obj.getSprite();
                if (spr == null || spr.getName() == null || !spr.getName().startsWith("vegetation_groundcover_01_")) continue;
                square.RemoveTileObjectErosionNoRecalc(obj);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn) {
        if (square.getObjects().size() > (hasWall ? 2 : 1)) {
            return false;
        }
        if (sqErosionData.soil < 0 || sqErosionData.soil >= this.soilRef.length) {
            return false;
        }
        int[] soilRef = this.soilRef[sqErosionData.soil];
        int eValue = sqErosionData.noiseMainInt;
        if (sqErosionData.rand(square.x, square.y, 101) < this.spawnChance[eValue]) {
            CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
            sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
            sqCategoryData.stage = 0;
            sqCategoryData.spawnTime = 100 - eValue;
            return true;
        }
        return false;
    }

    @Override
    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionCategory.Data data, ErosionData.Chunk chunkData, int eTick) {
        CategoryData sqCategoryData = (CategoryData)data;
        if (eTick < sqCategoryData.spawnTime || sqCategoryData.doNothing) {
            return;
        }
        if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
            ErosionObj gameObj = this.objs.get(sqCategoryData.gameObj);
            boolean bTree = false;
            boolean stage = false;
            int dispSeason = this.currentSeason(sqErosionData.magicNum, gameObj);
            boolean bloom = this.currentBloom(sqErosionData.magicNum, gameObj);
            this.updateObj(sqErosionData, data, square, gameObj, false, 0, dispSeason, bloom);
        } else {
            this.clearCatModData(sqErosionData);
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; ++i) {
            if (i >= 20 && i < 50) {
                this.spawnChance[i] = (int)this.clerp((float)(i - 20) / 30.0f, 0.0f, 8.0f);
                continue;
            }
            if (i < 50 || i >= 80) continue;
            this.spawnChance[i] = (int)this.clerp((float)(i - 50) / 30.0f, 8.0f, 0.0f);
        }
        this.seasonDisp[5].season1 = 0;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 2;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 0;
        this.seasonDisp[4].split = true;
        String sheet = "d_plants_1_";
        ArrayList<String> springSpr = new ArrayList<String>();
        for (int i = 0; i <= 7; ++i) {
            springSpr.add("d_plants_1_" + i);
        }
        ArrayList<String> autumnSpr = new ArrayList<String>();
        for (int i = 8; i <= 15; ++i) {
            autumnSpr.add("d_plants_1_" + i);
        }
        int offset = 16;
        for (int i = 0; i < this.plants.length; ++i) {
            if (i >= 8) {
                offset = 24;
            }
            if (i >= 16) {
                offset = 32;
            }
            PlantInit plant = this.plants[i];
            ErosionObjSprites objSpr = new ErosionObjSprites(1, plant.name, false, plant.hasFlower, false);
            objSpr.setBase(0, springSpr, 1);
            objSpr.setBase(0, autumnSpr, 4);
            objSpr.setBase(0, "d_plants_1_" + (offset + i), 2);
            objSpr.setFlower(0, "d_plants_1_" + (offset + i + 8));
            float bloomstart = plant.hasFlower ? plant.bloomstart : 0.0f;
            float bloomend = plant.hasFlower ? plant.bloomend : 0.0f;
            ErosionObj obj = new ErosionObj(objSpr, 30, bloomstart, bloomend, false);
            this.objs.add(obj);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new CategoryData();
    }

    @Override
    public void getObjectNames(ArrayList<String> list) {
        for (int i = 0; i < this.objs.size(); ++i) {
            if (this.objs.get((int)i).name == null || list.contains(this.objs.get((int)i).name)) continue;
            list.add(this.objs.get((int)i).name);
        }
    }

    private static final class PlantInit {
        public String name;
        public boolean hasFlower;
        public float bloomstart;
        public float bloomend;

        public PlantInit(String name, boolean hasFlower, float bloomstart, float bloomend) {
            this.name = name;
            this.hasFlower = hasFlower;
            this.bloomstart = bloomstart;
            this.bloomend = bloomend;
        }
    }

    private static final class CategoryData
    extends ErosionCategory.Data {
        public int gameObj;
        public int spawnTime;

        private CategoryData() {
        }

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.putShort((short)this.spawnTime);
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.spawnTime = input.getShort();
        }
    }
}

