/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.List;

public record Grass(float fernChance, float noGrassDiv, List<Double> noGrassStages, List<Double> grassStages) {
    public static final Grass DEFAULT = new Grass(0.3f, 12.0f, List.of(Double.valueOf(0.4)), List.of(Double.valueOf(0.33), Double.valueOf(0.5)));
}

