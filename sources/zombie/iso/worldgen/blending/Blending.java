/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.iso.worldgen.blending;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTree;
import zombie.iso.worldgen.WorldGenChunk;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.WorldGenTile;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.blending.BlendDirection;
import zombie.network.GameServer;
import zombie.network.ServerMap;

public class Blending {
    private static final int maxDepth = 4;
    private final WorldGenTile wgTile;
    private final List<String> plantsAdded = Lists.newArrayList("e_newgrass_1_40", "e_newgrass_1_42");

    public Blending() {
        this.wgTile = new WorldGenTile();
    }

    public void applyBlending(IsoChunk chunk) {
        for (BlendDirection dir : BlendDirection.values()) {
            IsoChunk sourceChunk;
            if (chunk.isBlendingDone(dir.index)) continue;
            IsoCell cell = IsoWorld.instance.currentCell;
            IsoChunk isoChunk = sourceChunk = GameServer.server ? ServerMap.instance.getChunk(chunk.wx + dir.x, chunk.wy + dir.y) : IsoWorld.instance.getCell().getChunk(chunk.wx + dir.x, chunk.wy + dir.y);
            if (sourceChunk == null || !sourceChunk.loaded) continue;
            IsoChunk secondaryChunk = cell.getChunk(chunk.wx + dir.opposite().x, chunk.wy + dir.opposite().y);
            if (chunk.isBlendingDonePartial() && (secondaryChunk == null || !secondaryChunk.loaded)) continue;
            if (sourceChunk.isBlendingDoneFull()) {
                DebugType.WorldGen.debugln(String.format("BLENDING: Chunk [%s %s] | %s | Worldgen chunk [%s %s]", new Object[]{chunk.wx, chunk.wy, dir, sourceChunk.wx, sourceChunk.wy}));
                this.removeTrees(cell, chunk, dir);
                this.changeGround(cell, chunk, dir);
            }
            chunk.setBlendingModified(dir.index);
            sourceChunk.setBlendingModified(dir.opposite().index);
        }
    }

    private void removeTrees(IsoCell cell, IsoChunk chunk, BlendDirection dir) {
        Random rnd = WorldGenParams.INSTANCE.getRandom(chunk.wx, chunk.wy);
        for (int x = 0; x < 8; ++x) {
            for (int y = 0; y < 8; ++y) {
                IsoGridSquare square;
                IsoTree tree;
                boolean hasToRemove;
                switch (dir) {
                    default: {
                        throw new MatchException(null, null);
                    }
                    case NORTH: {
                        boolean bl;
                        if (rnd.nextInt(100) >= y * 10) {
                            bl = true;
                            break;
                        }
                        bl = false;
                        break;
                    }
                    case SOUTH: {
                        boolean bl;
                        if (rnd.nextInt(100) >= (!chunk.isBlendingDonePartial() ? (8 - y - 1) * 10 : y * 10)) {
                            bl = true;
                            break;
                        }
                        bl = false;
                        break;
                    }
                    case WEST: {
                        boolean bl;
                        if (rnd.nextInt(100) >= x * 10) {
                            bl = true;
                            break;
                        }
                        bl = false;
                        break;
                    }
                    case EAST: {
                        boolean bl = hasToRemove = rnd.nextInt(100) >= (!chunk.isBlendingDonePartial() ? (8 - x - 1) * 10 : x * 10);
                    }
                }
                if (!hasToRemove || (tree = (square = this.getSquare(cell, chunk, dir, x, y)).getTree()) == null) continue;
                if (GameServer.server) {
                    GameServer.RemoveItemFromMap(tree);
                }
                square.DeleteTileObject(tree);
                if (rnd.nextInt(100) < 25) continue;
                String tile = this.plantsAdded.get(rnd.nextInt(this.plantsAdded.size()));
                square.addTileObject(tile);
                tree.transmitCompleteItemToClients();
            }
        }
    }

    private void changeGround(IsoCell cell, IsoChunk chunk, BlendDirection dir) {
        Random rnd = WorldGenParams.INSTANCE.getRandom(chunk.wx, chunk.wy);
        WorldGenChunk wgChunk = IsoWorld.instance.getWgChunk();
        for (int i = 0; i < 8; ++i) {
            IBiome biome = switch (dir) {
                default -> throw new MatchException(null, null);
                case BlendDirection.NORTH -> wgChunk.getBiome(chunk.wx * 8 + i, chunk.wy * 8 - 1);
                case BlendDirection.SOUTH -> wgChunk.getBiome(chunk.wx * 8 + i, chunk.wy * 8 + 8);
                case BlendDirection.WEST -> wgChunk.getBiome(chunk.wx * 8 - 1, chunk.wy * 8 + i);
                case BlendDirection.EAST -> wgChunk.getBiome(chunk.wx * 8 + 8, chunk.wy * 8 + i);
            };
            TileGroup tileGround = this.wgTile.getGround(biome, rnd);
            if (tileGround == null || tileGround.tiles().isEmpty()) {
                DebugLog.log(String.format("GROUND is empty for biome %s", biome));
                continue;
            }
            int depth = rnd.nextInt(4);
            for (int j = 0; j < depth; ++j) {
                IsoGridSquare square = this.getSquare(cell, chunk, dir, i * dir.planeY + j * dir.planeX, i * dir.planeX + j * dir.planeY);
                if (!square.getFloor().sprite.getName().contains("blends_natural_01")) continue;
                this.wgTile.setGround(this.wgTile.getSprite(tileGround.tiles().get(0)), square);
                this.wgTile.deleteTiles(square, this.plantsAdded);
                TileGroup tilePlant = this.wgTile.getPlant(biome, rnd);
                if (tilePlant == null) continue;
                IsoObject obj = square.addTileObject(tilePlant.tiles().get(0));
                obj.transmitCompleteItemToClients();
            }
        }
    }

    private IsoGridSquare getSquare(IsoCell cell, IsoChunk chunk, BlendDirection dir, int x, int y) {
        IsoGridSquare square;
        IsoGridSquare isoGridSquare;
        int posX = chunk.wx * 8;
        int posY = chunk.wy * 8;
        if (chunk.isBlendingDonePartial()) {
            switch (dir) {
                default: {
                    throw new MatchException(null, null);
                }
                case NORTH: {
                    isoGridSquare = cell.getGridSquare(posX + x, posY + chunk.getModifDepth(BlendDirection.NORTH) + y, 0);
                    break;
                }
                case SOUTH: {
                    isoGridSquare = cell.getGridSquare(posX + x, posY + chunk.getModifDepth(BlendDirection.SOUTH) - y, 0);
                    break;
                }
                case WEST: {
                    isoGridSquare = cell.getGridSquare(posX + chunk.getModifDepth(BlendDirection.WEST) + x, posY + y, 0);
                    break;
                }
                case EAST: {
                    isoGridSquare = cell.getGridSquare(posX + chunk.getModifDepth(BlendDirection.EAST) - x, posY + y, 0);
                    break;
                }
            }
        } else {
            isoGridSquare = cell.getGridSquare(posX + x, posY + y, 0);
        }
        if ((square = isoGridSquare) == null) {
            throw new RuntimeException(String.format("Square is null at [%s, %s]+(%s, %s) | %s", new Object[]{posX, posY, x, y, dir}));
        }
        return square;
    }
}

