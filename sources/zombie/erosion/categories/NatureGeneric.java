/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.properties.IsoPropertyType;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

public final class NatureGeneric
extends ErosionCategory {
    private final ArrayList<ErosionObj> objs = new ArrayList();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall) {
        int objsSize = square.getObjects().size();
        for (int i = objsSize - 1; i >= 1; --i) {
            String cname;
            IsoObject obj = square.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr == null || spr.getName() == null) continue;
            if (spr.getName().startsWith("blends_grassoverlays_01")) {
                if (spr.tileSheetIndex >= 0 && spr.tileSheetIndex <= 21) {
                    spr = IsoSpriteManager.instance.namedMap.getOrDefault("e_newgrass_1_" + (24 + spr.tileSheetIndex), spr);
                } else if (spr.tileSheetIndex >= 22 && spr.tileSheetIndex <= 45) {
                    spr = IsoSpriteManager.instance.namedMap.getOrDefault("e_newgrass_1_" + (48 + spr.tileSheetIndex - 22), spr);
                } else if (spr.tileSheetIndex >= 48 && spr.tileSheetIndex <= 69) {
                    spr = IsoSpriteManager.instance.namedMap.getOrDefault("e_newgrass_1_" + (72 + spr.tileSheetIndex - 48), spr);
                }
            }
            if (!spr.getName().startsWith("e_newgrass_1_") && !spr.getName().startsWith("d_generic_1_")) continue;
            String sprName = spr.getName();
            ErosionObj erosionObj = this.objs.stream().filter(o -> o.getEntry(sprName) != null).findFirst().orElse(null);
            if (erosionObj == null || (cname = spr.getProperties().get(IsoPropertyType.CUSTOM_NAME)) != null && (cname.contains("Twig") || cname.contains("Branch"))) continue;
            CategoryData sqCategoryData = (CategoryData)this.setCatModData(sqErosionData);
            sqCategoryData.gameObj = this.objs.indexOf(erosionObj);
            ErosionObjSprites.Entry entry = erosionObj.getEntry(spr.getName());
            sqCategoryData.stage = entry.stage();
            sqCategoryData.spawnTime = 0;
            sqCategoryData.dispSeason = -1;
            obj.setName(erosionObj.name);
            obj.doNotSync = true;
            sqCategoryData.hasSpawned = true;
            if (spr.getName().startsWith("e_newgrass_1_")) {
                sqCategoryData.maxStage = 2;
            } else {
                sqCategoryData.notGrass = true;
                sqCategoryData.maxStage = 1;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn) {
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
            if (stage > maxStage) {
                stage = maxStage;
            }
            if (stage >= gameObj.stages) {
                stage = gameObj.stages - 1;
            }
            if (sqCategoryData.stage == sqCategoryData.maxStage) {
                stage = sqCategoryData.maxStage;
            }
            int dispSeason = 0;
            if (!sqCategoryData.notGrass) {
                dispSeason = this.currentSeason(sqErosionData.magicNum, gameObj);
                int groundType = this.getGroundGrassType(square);
                if (groundType == 2) {
                    dispSeason = Math.max(dispSeason, 3);
                } else if (groundType == 3) {
                    dispSeason = Math.max(dispSeason, 4);
                }
            }
            boolean bTree = false;
            boolean bloom = false;
            this.updateObj(sqErosionData, data, square, gameObj, false, stage, dispSeason, false);
        } else {
            sqCategoryData.doNothing = true;
        }
    }

    @Override
    public void init() {
        ErosionObj obj;
        int sheetid;
        ErosionObjSprites objSpr;
        for (int i = 0; i < 100; ++i) {
            this.spawnChance[i] = (int)this.clerp((float)(i - 0) / 100.0f, 0.0f, 99.0f);
        }
        this.seasonDisp[5].season1 = 5;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 3;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 5;
        this.seasonDisp[4].split = true;
        int[] snames = new int[]{1, 2, 3, 4, 5};
        int[] stages = new int[]{2, 1, 0};
        for (int grassx = 0; grassx <= 5; ++grassx) {
            objSpr = new ErosionObjSprites(3, "Grass", false, false, false);
            for (int season = 0; season < snames.length; ++season) {
                for (int stage = 0; stage < stages.length; ++stage) {
                    sheetid = season * 24 + stage * 8 + grassx;
                    objSpr.setBase(stages[stage], "e_newgrass_1_" + sheetid, snames[season]);
                }
            }
            obj = new ErosionObj(objSpr, 60, 0.0f, 0.0f, false);
            this.objs.add(obj);
        }
        for (int genericx = 0; genericx <= 15; ++genericx) {
            objSpr = new ErosionObjSprites(2, "Generic", false, false, false);
            for (int stage = 0; stage <= 1; ++stage) {
                int sheetid2 = stage * 16 + genericx;
                objSpr.setBase(stage, "d_generic_1_" + sheetid2, 0);
            }
            obj = new ErosionObj(objSpr, 60, 0.0f, 0.0f, true);
            this.objs.add(obj);
        }
        ErosionIceQueen iceQueen = ErosionIceQueen.instance;
        for (int fernx = 0; fernx <= 7; ++fernx) {
            ErosionObjSprites objSpr2 = new ErosionObjSprites(2, "Fern", true, false, false);
            for (int stage = 0; stage <= 1; ++stage) {
                sheetid = 48 + stage * 32 + fernx;
                objSpr2.setBase(stage, "d_generic_1_" + sheetid, 0);
                iceQueen.addSprite("d_generic_1_" + sheetid, "d_generic_1_" + (sheetid + 16));
            }
            ErosionObj obj2 = new ErosionObj(objSpr2, 60, 0.0f, 0.0f, true);
            this.objs.add(obj2);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new CategoryData();
    }

    private int toInt(char ch) {
        switch (ch) {
            case '0': {
                return 0;
            }
            case '1': {
                return 1;
            }
            case '2': {
                return 2;
            }
            case '3': {
                return 3;
            }
            case '4': {
                return 4;
            }
            case '5': {
                return 5;
            }
            case '6': {
                return 6;
            }
            case '7': {
                return 7;
            }
            case '8': {
                return 8;
            }
            case '9': {
                return 9;
            }
        }
        return 0;
    }

    private int getGroundGrassType(IsoGridSquare square) {
        IsoObject floor = square.getFloor();
        if (floor == null) {
            return 0;
        }
        IsoSprite spr = floor.getSprite();
        if (spr != null && spr.getName() != null && spr.getName().startsWith("blends_natural_01_")) {
            int id = 0;
            for (int x = 18; x < spr.getName().length(); ++x) {
                id += this.toInt(spr.getName().charAt(x));
                if (x >= spr.getName().length() - 1) continue;
                id *= 10;
            }
            int row = id / 8;
            int col = id % 8;
            if (row == 2 && (col == 0 || col >= 5)) {
                return 1;
            }
            if (row == 4 && (col == 0 || col >= 5)) {
                return 2;
            }
            if (row == 6 && (col == 0 || col >= 5)) {
                return 3;
            }
        }
        return 0;
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
        public boolean notGrass;

        private CategoryData() {
        }

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
            output.put(this.notGrass ? (byte)1 : 0);
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
            this.notGrass = input.get() != 0;
        }
    }
}

