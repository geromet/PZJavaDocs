/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import zombie.iso.worldgen.PrefabStructure;
import zombie.iso.worldgen.biomes.Biome;

public record StaticModule(Biome biome, PrefabStructure prefab, int xmin, int xmax, int ymin, int ymax) {
}

