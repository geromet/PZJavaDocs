/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.utils.probabilities.Probability;

public record Feature(List<TileGroup> tileGroups, int minSize, int maxSize, Probability probability) {
}

