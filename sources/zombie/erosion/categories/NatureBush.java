/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class NatureBush
extends ErosionCategory {
    private final int[][] soilRef = new int[][]{{11, 11, 12, 13}, {5, 5, 7, 8, 11, 11, 12, 13, 11, 11, 12, 13}, {5, 5, 7, 8, 5, 5, 7, 8, 11, 11, 12, 13}, {1, 1, 4, 5}, {5, 5, 7, 8, 1, 1, 4, 5, 1, 1, 4, 5}, {5, 5, 7, 8, 5, 5, 7, 8, 1, 1, 4, 5}, {9, 10, 14, 15}, {5, 5, 7, 8, 9, 10, 14, 15, 9, 10, 14, 15}, {5, 5, 7, 8, 5, 5, 7, 8, 9, 10, 14, 15}, {2, 3, 16, 16}, {5, 5, 7, 8, 2, 3, 16, 16, 2, 3, 16, 16}, {5, 5, 7, 8, 5, 5, 7, 8, 2, 3, 16, 16}};
    private final ArrayList<ErosionObj> objs = new ArrayList();
    private final int[] spawnChance = new int[100];
    private final BushInit[] bush = new BushInit[]{new BushInit(this, "Spicebush", 0.05f, 0.35f, false), new BushInit(this, "Ninebark", 0.65f, 0.75f, true), new BushInit(this, "Ninebark", 0.65f, 0.75f, true), new BushInit(this, "Blueberry", 0.4f, 0.5f, true), new BushInit(this, "Blackberry", 0.4f, 0.5f, true), new BushInit(this, "Piedmont azalea", 0.0f, 0.15f, true), new BushInit(this, "Piedmont azalea", 0.0f, 0.15f, true), new BushInit(this, "Arrowwood viburnum", 0.3f, 0.8f, true), new BushInit(this, "Red chokeberry", 0.9f, 1.0f, true), new BushInit(this, "Red chokeberry", 0.9f, 1.0f, true), new BushInit(this, "Beautyberry", 0.7f, 0.85f, true), new BushInit(this, "New jersey tea", 0.4f, 0.8f, true), new BushInit(this, "New jersey tea", 0.4f, 0.8f, true), new BushInit(this, "Wild hydrangea", 0.2f, 0.35f, true), new BushInit(this, "Wild hydrangea", 0.2f, 0.35f, true), new BushInit(this, "Shrubby St. John's wort", 0.35f, 0.75f, true)};

    @Override
    public boolean replaceExistingObject(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall) {
        int objsSize = square.getObjects().size();
        boolean replaced = false;
        for (int i = objsSize - 1; i >= 1; --i) {
            IsoObject obj = square.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr == null || spr.getName() == null) continue;
            if (spr.getName().startsWith("vegetation_foliage")) {
                int soil = sqErosionData.soil;
                if (soil < 0 || soil >= this.soilRef.length) {
                    soil = sqErosionData.rand(square.x, square.y, this.soilRef.length);
                }
                int[] soilRef = this.soilRef[soil];
                int eValue = sqErosionData.noiseMainInt;
                CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
                sqCategoryData.stage = sqCategoryData.maxStage = (int)Math.floor((float)eValue / 60.0f);
                sqCategoryData.spawnTime = 0;
                square.RemoveTileObject(obj, false);
                replaced = true;
            }
            if (!spr.getName().startsWith("f_bushes_1_")) continue;
            int id = Integer.parseInt(spr.getName().replace("f_bushes_1_", ""));
            CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
            sqCategoryData.gameObj = id % 16;
            sqCategoryData.stage = sqCategoryData.maxStage = 1;
            sqCategoryData.spawnTime = 0;
            square.RemoveTileObject(obj, false);
            replaced = true;
        }
        return replaced;
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
        int randValue = sqErosionData.rand(square.x, square.y, 101);
        if (randValue < this.spawnChance[eValue]) {
            CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
            sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
            sqCategoryData.maxStage = (int)Math.floor((float)eValue / 60.0f);
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
            int maxStage = sqCategoryData.maxStage;
            int stage = (int)Math.floor((float)(eTick - sqCategoryData.spawnTime) / ((float)gameObj.cycleTime / ((float)maxStage + 1.0f)));
            if (stage < sqCategoryData.stage) {
                stage = sqCategoryData.stage;
            }
            if (stage > maxStage) {
                stage = maxStage;
            }
            int dispSeason = this.currentSeason(sqErosionData.magicNum, gameObj);
            boolean bloom = this.currentBloom(sqErosionData.magicNum, gameObj);
            boolean bTree = false;
            this.updateObj(sqErosionData, data, square, gameObj, false, stage, dispSeason, bloom);
        } else {
            sqCategoryData.doNothing = true;
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; ++i) {
            if (i >= 45 && i < 60) {
                this.spawnChance[i] = (int)this.clerp((float)(i - 45) / 15.0f, 0.0f, 20.0f);
            }
            if (i < 60 || i >= 90) continue;
            this.spawnChance[i] = (int)this.clerp((float)(i - 60) / 30.0f, 20.0f, 0.0f);
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
        ErosionIceQueen iceQueen = ErosionIceQueen.instance;
        String sheet = "f_bushes_1_";
        for (int id = 1; id <= this.bush.length; ++id) {
            int i = id - 1;
            int trunk = i - (int)Math.floor((float)i / 8.0f) * 8;
            BushInit b = this.bush[i];
            ErosionObjSprites objSpr = new ErosionObjSprites(2, b.name, true, b.hasFlower, true);
            int baseId = 0 + trunk;
            int snowId = baseId + 16;
            int springId = snowId + 16;
            int autumnId = springId + 16;
            int summerId = 64 + i;
            int bloomId = summerId + 16;
            objSpr.setBase(0, "f_bushes_1_" + baseId, 0);
            objSpr.setBase(1, "f_bushes_1_" + (baseId + 8), 0);
            iceQueen.addSprite("f_bushes_1_" + baseId, "f_bushes_1_" + snowId);
            iceQueen.addSprite("f_bushes_1_" + (baseId + 8), "f_bushes_1_" + (snowId + 8));
            objSpr.setChildSprite(0, "f_bushes_1_" + springId, 1);
            objSpr.setChildSprite(1, "f_bushes_1_" + (springId + 8), 1);
            objSpr.setChildSprite(0, "f_bushes_1_" + autumnId, 4);
            objSpr.setChildSprite(1, "f_bushes_1_" + (autumnId + 8), 4);
            objSpr.setChildSprite(0, "f_bushes_1_" + summerId, 2);
            objSpr.setChildSprite(1, "f_bushes_1_" + (summerId + 32), 2);
            if (b.hasFlower) {
                objSpr.setFlower(0, "f_bushes_1_" + bloomId);
                objSpr.setFlower(1, "f_bushes_1_" + (bloomId + 32));
            }
            float bloomstart = b.hasFlower ? b.bloomstart : 0.0f;
            float bloomend = b.hasFlower ? b.bloomend : 0.0f;
            ErosionObj obj = new ErosionObj(objSpr, 60, bloomstart, bloomend, true);
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

    private class BushInit {
        public String name;
        public float bloomstart;
        public float bloomend;
        public boolean hasFlower;

        public BushInit(NatureBush natureBush, String name, float bloomstart, float bloomend, boolean hasFlower) {
            Objects.requireNonNull(natureBush);
            this.name = name;
            this.bloomstart = bloomstart;
            this.bloomend = bloomend;
            this.hasFlower = hasFlower;
        }
    }

    private static final class CategoryData
    extends ErosionCategory.Data {
        public int gameObj;
        public int maxStage;
        public int spawnTime;

        private CategoryData() {
        }

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
        }
    }
}

