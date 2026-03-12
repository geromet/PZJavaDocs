/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.roads;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;

public record RoadConfig(List<TileGroup> tiles, double probaRoads, double probability, double filter) {
}

