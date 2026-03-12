/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.iso.CellLoader;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WorldGenUtils;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.roads.Road;
import zombie.iso.worldgen.veins.OreVein;

public class WorldGenTile {
    public static final String NO_TREE = "NO_TREE";
    public static final String NO_BUSH = "NO_BUSH";
    public static final String NO_GRASS = "NO_GRASS";

    public void setTiles(IBiome biome, IsoGridSquare square, IsoChunk ch, IsoCell cell, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, boolean isMap, Random rnd) {
        ArrayList<String> tiles = new ArrayList<String>();
        for (FeatureType type : FeatureType.values()) {
            String tile = this.getBiomeTile(biome, type, ch, tileX, tileY, tileZ, toBeDone, rnd, 16);
            if (tile == null) continue;
            if (isMap) {
                IsoObject bush;
                IsoTree tree = square.getTree();
                if (tree != null) {
                    square.DeleteTileObject(tree);
                }
                if ((bush = square.getBush()) != null) {
                    square.DeleteTileObject(bush);
                }
                if (square.getObjects().size() - square.getGrassLike().size() != 1) continue;
                tiles.add(tile);
                continue;
            }
            if (tiles.size() > 1) {
                tiles.remove(1);
            }
            tiles.add(tile);
        }
        for (String tile : tiles) {
            this.applyTile(tile, square, cell, x, y, z, rnd);
        }
    }

    public boolean setTiles(IBiome biome, FeatureType type, IsoGridSquare square, IsoChunk ch, IsoCell cell, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, Random rnd) {
        for (int i = 0; i < 16; ++i) {
            int targetSize = 8 >> i;
            if (targetSize == 0) {
                return false;
            }
            String tile = this.getBiomeTile(biome, type, ch, tileX, tileY, tileZ, toBeDone, rnd, targetSize);
            if (tile == null) continue;
            this.applyTile(tile, square, cell, x, y, z, rnd);
            return true;
        }
        return false;
    }

    private String getBiomeTile(IBiome biome, FeatureType type, IsoChunk ch, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, Random rnd, int targetSize) {
        String tile = toBeDone.get((Object)type)[tileX + tileY * 8];
        if (tile == null || tile.isEmpty()) {
            Map<FeatureType, List<Feature>> features = biome.getFeatures();
            if (features == null) {
                return null;
            }
            List<Feature> featuresList = features.get((Object)type);
            if (featuresList == null || featuresList.isEmpty()) {
                return null;
            }
            ArrayList<Feature> featuresFiltered = new ArrayList<Feature>();
            for (Feature feature : featuresList) {
                if (feature.minSize() > targetSize) continue;
                featuresFiltered.add(feature);
            }
            float prefilterProba = 0.0f;
            float postfilterProba = 0.0f;
            for (Feature feature : featuresList) {
                prefilterProba += feature.probability().getValue();
            }
            for (Feature feature : featuresFiltered) {
                postfilterProba += feature.probability().getValue();
            }
            Feature feature = this.findFeature(featuresFiltered, prefilterProba, postfilterProba, rnd);
            if (feature == null) {
                return null;
            }
            ArrayList<TileGroup> tileGroups = new ArrayList<TileGroup>();
            for (TileGroup tileGroup : feature.tileGroups()) {
                if (tileGroup.sx() > targetSize || tileGroup.sy() > targetSize) continue;
                tileGroups.add(tileGroup);
            }
            if (tileGroups.isEmpty()) {
                return null;
            }
            TileGroup tileGroup = (TileGroup)tileGroups.get(rnd.nextInt(tileGroups.size()));
            if (tileX + tileGroup.sx() - 1 >= 8 || tileY + tileGroup.sy() - 1 >= 8) {
                return null;
            }
            if (biome.placements() != null && !this.checkFutureSquares(tileGroup, ch, tileX, tileY, tileZ, biome.placements().get((Object)type))) {
                return null;
            }
            for (int ix = 0; ix < tileGroup.sx(); ++ix) {
                for (int iy = 0; iy < tileGroup.sy(); ++iy) {
                    toBeDone.get((Object)((Object)type))[ix + tileX + (iy + tileY) * 8] = tileGroup.tiles().get(ix + iy * tileGroup.sx());
                }
            }
            tile = tileGroup.tiles().get(0);
        }
        return tile;
    }

    public Feature findFeature(List<Feature> features, float prefilterProba, float postfilterProba, Random rnd) {
        if (features == null || features.isEmpty()) {
            return null;
        }
        float rndValue = rnd.nextFloat();
        float probability = 0.0f;
        Feature feature = null;
        for (Feature tmpFeature : features) {
            if (rndValue >= (probability += tmpFeature.probability().getValue() / postfilterProba * prefilterProba)) continue;
            feature = tmpFeature;
            break;
        }
        return feature;
    }

    private boolean checkFutureSquares(TileGroup tg, IsoChunk ch, int tileX, int tileY, int tileZ, List<String> placement) {
        if (tg.sx() == 1 && tg.sy() == 1) {
            return true;
        }
        for (int ix = 0; ix < tg.sx(); ++ix) {
            for (int iy = 0; iy < tg.sy(); ++iy) {
                if (ix == 0 && iy == 0) continue;
                IsoGridSquare square = ch.getGridSquare(tileX + ix, tileY + iy, tileZ);
                if (square == null) {
                    return false;
                }
                IsoObject floor = square.getFloor();
                if (floor == null) {
                    return false;
                }
                if (square.getObjects().size() - square.getGrassLike().size() - square.getBushes().size() > 1) {
                    return false;
                }
                if (WorldGenUtils.INSTANCE.canPlace(placement, floor.getSprite().getName())) continue;
                return false;
            }
        }
        return true;
    }

    public void setTile(OreVein vein, IsoGridSquare square, IsoCell cell, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, Random rnd) {
        List<TileGroup> tileGroups = vein.getSingleFeatures();
        TileGroup tileGroup = tileGroups.get(rnd.nextInt(tileGroups.size()));
        this.applyTile(tileGroup.tiles().get(0), square, cell, x, y, z, rnd);
    }

    public void setTile(Road road, IsoGridSquare square, IsoCell cell, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, Random rnd) {
        List<TileGroup> tileGroups = road.getSingleFeatures();
        TileGroup tileGroup = tileGroups.get(rnd.nextInt(tileGroups.size()));
        this.applyTile(tileGroup.tiles().get(0), square, cell, x, y, z, rnd);
    }

    public void applyTile(String tile, IsoGridSquare square, IsoCell cell, int x, int y, int z, Random rnd) {
        if (tile.equals(NO_TREE) || tile.equals(NO_BUSH) || tile.equals(NO_GRASS)) {
            return;
        }
        IsoSprite spr = this.getSprite(IsoChunk.Fix2x(tile));
        CellLoader.DoTileObjectCreation(spr, spr.getTileType(), square, cell, x, y, z, tile);
    }

    public IsoSprite getSprite(String tile) {
        IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
        if (spr == null) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
            spr = IsoSprite.getSprite(IsoSpriteManager.instance, "carpentry_02_58", 0);
        }
        return spr;
    }

    public TileGroup getGround(IBiome biome, Random rnd) {
        List<Feature> features = biome.getFeatures().get((Object)FeatureType.GROUND);
        Feature feature = features.get(rnd.nextInt(features.size()));
        List<TileGroup> tileGroups = feature.tileGroups();
        return tileGroups.get(rnd.nextInt(features.size()));
    }

    public TileGroup getPlant(IBiome biome, Random rnd) {
        List<Feature> features = biome.getFeatures().get((Object)FeatureType.PLANT);
        if (features == null || features.isEmpty()) {
            return null;
        }
        Feature feature = features.get(rnd.nextInt(features.size()));
        if (rnd.nextFloat() > feature.probability().getValue()) {
            return null;
        }
        List<TileGroup> tileGroups = feature.tileGroups();
        return tileGroups.get(rnd.nextInt(tileGroups.size()));
    }

    public void setGround(IsoSprite spr, IsoGridSquare sq) {
        spr.solidfloor = true;
        IsoObject floor = sq.getFloor();
        if (floor != null) {
            floor.clearAttachedAnimSprite();
            floor.setSprite(spr);
        }
    }

    public void deleteTiles(IsoGridSquare sq) {
        ArrayList<IsoObject> toDelete = new ArrayList<IsoObject>();
        for (IsoObject element : sq.getObjects().getElements()) {
            if (element == null || element.isFloor()) continue;
            toDelete.add(element);
        }
        for (IsoObject element : toDelete) {
            sq.DeleteTileObject(element);
        }
    }

    public void deleteTiles(IsoGridSquare sq, List<String> toRemove) {
        ArrayList<IsoObject> toDelete = new ArrayList<IsoObject>();
        for (IsoObject element : sq.getObjects().getElements()) {
            String tmpTile;
            if (element == null || !toRemove.contains(tmpTile = element.getSprite().name)) continue;
            toDelete.add(element);
        }
        for (IsoObject element : toDelete) {
            sq.DeleteTileObject(element);
        }
    }
}

