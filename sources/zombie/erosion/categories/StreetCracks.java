/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public final class StreetCracks
extends ErosionCategory {
    private final ArrayList<ErosionObj> objs = new ArrayList();
    private final ArrayList<ErosionObjOverlay> crackObjs = new ArrayList();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall) {
        return false;
    }

    @Override
    public boolean validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn) {
        int eValue = sqErosionData.noiseMainInt;
        int spawnChance = this.spawnChance[eValue];
        if (spawnChance == 0) {
            return false;
        }
        if (sqErosionData.rand(square.x, square.y, 101) >= spawnChance) {
            return false;
        }
        CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
        sqCategoryData.gameObj = sqErosionData.rand(square.x, square.y, this.crackObjs.size());
        sqCategoryData.maxStage = eValue > 65 ? 2 : (eValue > 55 ? 1 : 0);
        sqCategoryData.stage = 0;
        sqCategoryData.spawnTime = 150 - eValue;
        if (sqErosionData.magicNum > 0.5f) {
            sqCategoryData.hasGrass = true;
        }
        return true;
    }

    @Override
    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionCategory.Data data, ErosionData.Chunk chunkData, int eTick) {
        CategoryData sqCategoryData = (CategoryData)data;
        if (eTick < sqCategoryData.spawnTime || sqCategoryData.doNothing) {
            return;
        }
        IsoObject floor = square.getFloor();
        if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.crackObjs.size() && floor != null) {
            ErosionObj grassObj;
            ErosionObjOverlay gameObj = this.crackObjs.get(sqCategoryData.gameObj);
            int maxStage = sqCategoryData.maxStage;
            int stage = (int)Math.floor((float)(eTick - sqCategoryData.spawnTime) / ((float)gameObj.cycleTime / ((float)maxStage + 1.0f)));
            if (stage < sqCategoryData.stage) {
                stage = sqCategoryData.stage;
            }
            if (stage >= gameObj.stages) {
                stage = gameObj.stages - 1;
            }
            if (stage != sqCategoryData.stage) {
                int curId = sqCategoryData.curId;
                int id = gameObj.setOverlay(floor, curId, stage, 0, 0.0f);
                if (id >= 0) {
                    sqCategoryData.curId = id;
                }
                sqCategoryData.stage = stage;
            } else if (!sqCategoryData.hasGrass && stage == gameObj.stages - 1) {
                sqCategoryData.doNothing = true;
            }
            if (sqCategoryData.hasGrass && (grassObj = this.objs.get(sqCategoryData.gameObj)) != null) {
                int dispSeason = this.currentSeason(sqErosionData.magicNum, grassObj);
                boolean bTree = false;
                boolean bloom = false;
                this.updateObj(sqErosionData, data, square, grassObj, false, stage, dispSeason, false);
            }
        } else {
            sqCategoryData.doNothing = true;
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; ++i) {
            this.spawnChance[i] = i >= 40 ? (int)this.clerp((float)(i - 40) / 60.0f, 0.0f, 60.0f) : 0;
        }
        this.seasonDisp[5].season1 = 5;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 4;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 5;
        this.seasonDisp[4].split = true;
        String sheet = "d_streetcracks_1_";
        int[] seasons = new int[]{5, 1, 2, 4};
        for (int i = 0; i <= 7; ++i) {
            ErosionObjOverlaySprites crackspr = new ErosionObjOverlaySprites(3, "StreeCracks");
            ErosionObjSprites grassspr = new ErosionObjSprites(3, "CrackGrass", false, false, false);
            for (int stage = 0; stage <= 2; ++stage) {
                for (int season = 0; season <= seasons.length; ++season) {
                    int id = season * 24 + stage * 8 + i;
                    if (season == 0) {
                        crackspr.setSprite(stage, "d_streetcracks_1_" + id, 0);
                        continue;
                    }
                    grassspr.setBase(stage, "d_streetcracks_1_" + id, seasons[season - 1]);
                }
            }
            this.crackObjs.add(new ErosionObjOverlay(crackspr, 60, false));
            this.objs.add(new ErosionObj(grassspr, 60, 0.0f, 0.0f, false));
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

    private static final class CategoryData
    extends ErosionCategory.Data {
        public int gameObj;
        public int maxStage;
        public int spawnTime;
        public int curId = -999999;
        public boolean hasGrass;

        private CategoryData() {
        }

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
            output.putInt(this.curId);
            output.put(this.hasGrass ? (byte)1 : 0);
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
            this.curId = input.getInt();
            this.hasGrass = input.get() != 0;
        }
    }
}

