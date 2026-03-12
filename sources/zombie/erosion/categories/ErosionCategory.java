/*
 * Decompiled with CFR 0.152.
 */
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.ErosionRegions;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;

public abstract class ErosionCategory {
    public int id;
    public ErosionRegions.Region region;
    protected SeasonDisplay[] seasonDisp = new SeasonDisplay[6];

    public ErosionCategory() {
        for (int i = 0; i < 6; ++i) {
            this.seasonDisp[i] = new SeasonDisplay(this);
        }
    }

    protected Data getCatModData(ErosionData.Square sqErosionData) {
        for (int i = 0; i < sqErosionData.regions.size(); ++i) {
            Data data = sqErosionData.regions.get(i);
            if (data.regionId != this.region.id || data.categoryId != this.id) continue;
            return data;
        }
        return null;
    }

    protected Data setCatModData(ErosionData.Square sqErosionData) {
        Data data = this.getCatModData(sqErosionData);
        if (data == null) {
            data = this.allocData();
            data.regionId = this.region.id;
            data.categoryId = this.id;
            sqErosionData.regions.add(data);
            if (sqErosionData.regions.size() > 5) {
                DebugLog.log("> 5 regions on a square");
            }
        }
        return data;
    }

    protected IsoObject validWall(IsoGridSquare square, boolean north, boolean doWindow) {
        if (square == null) {
            return null;
        }
        IsoGridSquare other = north ? square.getTileInDirection(IsoDirections.N) : square.getTileInDirection(IsoDirections.W);
        IsoObject object = null;
        if (square.isWallTo(other)) {
            if (north && square.has(IsoFlagType.cutN) && !square.has(IsoFlagType.canPathN) || !north && square.has(IsoFlagType.cutW) && !square.has(IsoFlagType.canPathW)) {
                object = square.getWall(north);
            }
        } else if (doWindow && (square.isWindowBlockedTo(other) || square.isWindowTo(other)) && (object = square.getWindowTo(other)) == null) {
            object = square.getWall(north);
        }
        if (object != null) {
            if (square.getZ() > 0) {
                String spname = object.getSprite().getName();
                if (spname != null && !spname.contains("roof")) {
                    return object;
                }
                return null;
            }
            return object;
        }
        return null;
    }

    protected float clerp(float t, float a, float b) {
        float t2 = (float)(1.0 - Math.cos((double)t * Math.PI)) / 2.0f;
        return a * (1.0f - t2) + b * t2;
    }

    protected int currentSeason(float magicNum, ErosionObj gameObj) {
        int dispSeason;
        ErosionSeason seasons = ErosionMain.getInstance().getSeasons();
        int season = seasons.getSeason();
        float seasonDay = seasons.getSeasonDay();
        float seasonDays = seasons.getSeasonDays();
        float seasonDaysHalf = seasonDays / 2.0f;
        float halfMagicNum = seasonDaysHalf * magicNum;
        SeasonDisplay curSeason = this.seasonDisp[season];
        if (curSeason.split && seasonDay >= seasonDaysHalf + halfMagicNum) {
            dispSeason = curSeason.season2;
        } else if (curSeason.split && seasonDay >= halfMagicNum || seasonDay >= seasonDays * magicNum) {
            dispSeason = curSeason.season1;
        } else {
            SeasonDisplay prevSeason = season == 5 ? this.seasonDisp[4] : (season == 1 ? this.seasonDisp[5] : (season == 2 ? this.seasonDisp[1] : this.seasonDisp[2]));
            dispSeason = prevSeason.split ? prevSeason.season2 : prevSeason.season1;
        }
        return dispSeason;
    }

    protected boolean currentBloom(float magicNum, ErosionObj gameObj) {
        boolean bloom = false;
        ErosionSeason seasons = ErosionMain.getInstance().getSeasons();
        int season = seasons.getSeason();
        if (gameObj.hasFlower && season == 2) {
            float seasonDay = seasons.getSeasonDay();
            float seasonDays = seasons.getSeasonDays();
            float seasonDaysHalf = seasonDays / 2.0f;
            float halfMagicNum = seasonDaysHalf * magicNum;
            float totalbloomdays = seasonDays - halfMagicNum;
            float curbloomday = seasonDay - halfMagicNum;
            float bdaysMax = totalbloomdays * gameObj.bloomEnd;
            float bdaysMin = totalbloomdays * gameObj.bloomStart;
            float totalsplit = (bdaysMax - bdaysMin) / 2.0f;
            float offset = totalsplit * magicNum;
            bdaysMax = bdaysMin + totalsplit + offset;
            if (curbloomday >= (bdaysMin += offset) && curbloomday <= bdaysMax) {
                bloom = true;
            }
        }
        return bloom;
    }

    /*
     * Enabled aggressive block sorting
     */
    public void updateObj(ErosionData.Square sqErosionData, Data sqCategoryData, IsoGridSquare square, ErosionObj gameObj, boolean tree, int stage, int dispSeason, boolean bloom) {
        if (!sqCategoryData.hasSpawned) {
            if (!gameObj.placeObject(square, stage, tree, dispSeason, bloom)) {
                this.clearCatModData(sqErosionData);
                return;
            }
            sqCategoryData.hasSpawned = true;
        } else if (sqCategoryData.stage != stage || sqCategoryData.dispSeason != dispSeason || sqCategoryData.dispBloom != bloom) {
            IsoObject obj = gameObj.getObject(square, false);
            if (obj == null) {
                this.clearCatModData(sqErosionData);
                return;
            }
            gameObj.setStageObject(stage, obj, dispSeason, bloom);
        }
        sqCategoryData.stage = stage;
        sqCategoryData.dispSeason = dispSeason;
        sqCategoryData.dispBloom = bloom;
    }

    protected void clearCatModData(ErosionData.Square sqErosionData) {
        for (int i = 0; i < sqErosionData.regions.size(); ++i) {
            Data data = sqErosionData.regions.get(i);
            if (data.regionId != this.region.id || data.categoryId != this.id) continue;
            sqErosionData.regions.remove(i);
            return;
        }
    }

    public abstract void init();

    public abstract boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, boolean var4, boolean var5);

    public abstract boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, boolean var4, boolean var5, boolean var6);

    public abstract void update(IsoGridSquare var1, ErosionData.Square var2, Data var3, ErosionData.Chunk var4, int var5);

    protected abstract Data allocData();

    public static Data loadCategoryData(ByteBuffer input, int worldVersion) {
        byte regionID = input.get();
        byte categoryID = input.get();
        ErosionCategory category = ErosionRegions.getCategory(regionID, categoryID);
        Data data = category.allocData();
        data.regionId = regionID;
        data.categoryId = categoryID;
        data.load(input, worldVersion);
        return data;
    }

    public abstract void getObjectNames(ArrayList<String> var1);

    protected class SeasonDisplay {
        int season1;
        int season2;
        boolean split;

        protected SeasonDisplay(ErosionCategory this$0) {
            Objects.requireNonNull(this$0);
        }
    }

    public static class Data {
        public int regionId;
        public int categoryId;
        public boolean doNothing;
        public boolean hasSpawned;
        public int stage;
        public int dispSeason;
        public boolean dispBloom;

        public void save(ByteBuffer output) {
            byte flags = 0;
            if (this.doNothing) {
                flags = (byte)(flags | 1);
            }
            if (this.hasSpawned) {
                flags = (byte)(flags | 2);
            }
            if (this.dispBloom) {
                flags = (byte)(flags | 4);
            }
            if (this.stage == 1) {
                flags = (byte)(flags | 8);
            } else if (this.stage == 2) {
                flags = (byte)(flags | 0x10);
            } else if (this.stage == 3) {
                flags = (byte)(flags | 0x20);
            } else if (this.stage == 4) {
                flags = (byte)(flags | 0x40);
            } else if (this.stage > 4) {
                flags = (byte)(flags | 0x80);
            }
            output.put((byte)this.regionId);
            output.put((byte)this.categoryId);
            output.put((byte)this.dispSeason);
            output.put(flags);
            if (this.stage > 4) {
                output.put((byte)this.stage);
            }
        }

        public void load(ByteBuffer input, int worldVersion) {
            this.stage = 0;
            this.dispSeason = input.get();
            byte flags = input.get();
            this.doNothing = (flags & 1) != 0;
            this.hasSpawned = (flags & 2) != 0;
            boolean bl = this.dispBloom = (flags & 4) != 0;
            if ((flags & 8) != 0) {
                this.stage = 1;
            } else if ((flags & 0x10) != 0) {
                this.stage = 2;
            } else if ((flags & 0x20) != 0) {
                this.stage = 3;
            } else if ((flags & 0x40) != 0) {
                this.stage = 4;
            } else if ((flags & 0x80) != 0) {
                this.stage = input.get();
            }
        }
    }
}

