/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

public final class WallVines
extends ErosionCategory {
    private final ArrayList<ErosionObjOverlay> objs = new ArrayList();
    private static final int DIRNW = 0;
    private static final int DIRN = 1;
    private static final int DIRW = 2;
    private final int[][] objsRef = new int[3][2];
    private final HashMap<String, Integer> spriteToObj = new HashMap();
    private final HashMap<String, Integer> spriteToStage = new HashMap();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall) {
        int objsSize = square.getObjects().size();
        for (int i = objsSize - 1; i >= 1; --i) {
            IsoObject obj = square.getObjects().get(i);
            if (obj.attachedAnimSprite == null) continue;
            for (int j = 0; j < obj.attachedAnimSprite.size(); ++j) {
                IsoSprite spr = obj.attachedAnimSprite.get((int)j).parentSprite;
                if (spr == null || spr.getName() == null || !spr.getName().startsWith("f_wallvines_1_") || !this.spriteToObj.containsKey(spr.getName())) continue;
                CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = this.spriteToObj.get(spr.getName());
                sqCategoryData.stage = this.spriteToStage.get(spr.getName());
                sqCategoryData.maxStage = 2;
                sqCategoryData.spawnTime = 0;
                obj.attachedAnimSprite.remove(j);
                if (obj.attachedAnimSprite != null && j < obj.attachedAnimSprite.size()) {
                    obj.attachedAnimSprite.remove(j);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn) {
        int dir;
        if (!isExterior) {
            return false;
        }
        int eValue = sqErosionData.noiseMainInt;
        int spawnChance = this.spawnChance[eValue];
        if (spawnChance == 0) {
            return false;
        }
        if (sqErosionData.rand(square.x, square.y, 101) >= spawnChance) {
            return false;
        }
        IsoObject north = this.validWall(square, true, true);
        IsoObject west = this.validWall(square, false, true);
        if (north != null && west != null) {
            dir = 0;
        } else if (north != null) {
            dir = 1;
        } else if (west != null) {
            dir = 2;
        } else {
            return false;
        }
        CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
        sqCategoryData.gameObj = this.objsRef[dir][sqErosionData.rand(square.x, square.y, this.objsRef[dir].length)];
        sqCategoryData.maxStage = eValue > 65 ? 3 : (eValue > 60 ? 2 : (eValue > 55 ? 1 : 0));
        sqCategoryData.stage = 0;
        sqCategoryData.spawnTime = 100 - eValue;
        if (sqCategoryData.maxStage == 3) {
            IsoGridSquare topsq = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
            if (topsq != null) {
                IsoObject obj = this.validWall(topsq, dir == 1, true);
                ErosionObjOverlay gameObj = this.objs.get(sqCategoryData.gameObj);
                if (obj != null && gameObj != null) {
                    CategoryData topsqCategoryData = new CategoryData();
                    topsqCategoryData.gameObj = this.objsRef[dir][sqErosionData.rand(square.x, square.y, this.objsRef[dir].length)];
                    topsqCategoryData.maxStage = eValue > 75 ? 2 : (eValue > 70 ? 1 : 0);
                    topsqCategoryData.stage = 0;
                    topsqCategoryData.spawnTime = sqCategoryData.spawnTime + (int)((float)gameObj.cycleTime / ((float)sqCategoryData.maxStage + 1.0f) * 4.0f);
                    sqCategoryData.hasTop = topsqCategoryData;
                } else {
                    sqCategoryData.maxStage = 2;
                }
            } else {
                sqCategoryData.maxStage = 2;
            }
        }
        return true;
    }

    @Override
    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionCategory.Data data, ErosionData.Chunk chunkData, int eTick) {
        CategoryData sqCategoryData = (CategoryData)data;
        if (eTick < sqCategoryData.spawnTime || sqCategoryData.doNothing) {
            return;
        }
        if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
            ErosionObjOverlay gameObj = this.objs.get(sqCategoryData.gameObj);
            int maxStage = sqCategoryData.maxStage;
            int stage = (int)Math.floor((float)(eTick - sqCategoryData.spawnTime) / ((float)gameObj.cycleTime / ((float)maxStage + 1.0f)));
            if (stage < sqCategoryData.stage) {
                stage = sqCategoryData.stage;
            }
            if (stage > maxStage) {
                stage = maxStage;
            }
            if (stage > gameObj.stages) {
                stage = gameObj.stages;
            }
            if (stage == 3 && sqCategoryData.hasTop != null && sqCategoryData.hasTop.spawnTime > eTick) {
                stage = 2;
            }
            int dispSeason = ErosionMain.getInstance().getSeasons().getSeason();
            if (stage != sqCategoryData.stage || sqCategoryData.dispSeason != dispSeason) {
                IsoGridSquare topsq;
                IsoObject object = null;
                IsoObject north = this.validWall(square, true, true);
                IsoObject west = this.validWall(square, false, true);
                if (north != null && west != null) {
                    object = north;
                } else if (north != null) {
                    object = north;
                } else if (west != null) {
                    object = west;
                }
                sqCategoryData.dispSeason = dispSeason;
                if (object != null) {
                    int curId = sqCategoryData.curId;
                    int id = gameObj.setOverlay(object, curId, stage, dispSeason, 0.0f);
                    if (id >= 0) {
                        sqCategoryData.curId = id;
                    }
                } else {
                    sqCategoryData.doNothing = true;
                }
                if (stage == 3 && sqCategoryData.hasTop != null && (topsq = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1)) != null) {
                    this.update(topsq, sqErosionData, sqCategoryData.hasTop, chunkData, eTick);
                }
            }
        } else {
            sqCategoryData.doNothing = true;
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; ++i) {
            this.spawnChance[i] = i >= 50 ? 100 : 0;
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
        String sheet = "f_wallvines_1_";
        int[] seasons = new int[]{5, 2, 4, 1};
        int[] dir = new int[]{2, 2, 1, 1, 0, 0};
        int[] count = new int[3];
        for (int i = 0; i < dir.length; ++i) {
            ErosionObjOverlaySprites vinespr = new ErosionObjOverlaySprites(4, "WallVines");
            for (int stage = 0; stage <= 3; ++stage) {
                for (int season = 0; season <= 2; ++season) {
                    int id = season * 24 + stage * 6 + i;
                    vinespr.setSprite(stage, "f_wallvines_1_" + id, seasons[season]);
                    if (season == 2) {
                        vinespr.setSprite(stage, "f_wallvines_1_" + id, seasons[season + 1]);
                    }
                    this.spriteToObj.put("f_wallvines_1_" + id, this.objs.size());
                    this.spriteToStage.put("f_wallvines_1_" + id, stage);
                }
            }
            this.objs.add(new ErosionObjOverlay(vinespr, 60, false));
            int n = dir[i];
            int n2 = count[n];
            count[n] = n2 + 1;
            this.objsRef[dir[i]][n2] = this.objs.size() - 1;
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
        public CategoryData hasTop;

        private CategoryData() {
        }

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
            output.putInt(this.curId);
            if (this.hasTop != null) {
                output.put((byte)1);
                output.put((byte)this.hasTop.gameObj);
                output.putShort((short)this.hasTop.spawnTime);
                output.putInt(this.hasTop.curId);
            } else {
                output.put((byte)0);
            }
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            boolean hasTop;
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
            this.curId = input.getInt();
            boolean bl = hasTop = input.get() != 0;
            if (hasTop) {
                this.hasTop = new CategoryData();
                this.hasTop.gameObj = input.get();
                this.hasTop.spawnTime = input.getShort();
                this.hasTop.curId = input.getInt();
            }
        }
    }
}

