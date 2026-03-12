/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.Map;

public class BiomeType {
    public static Map<Class<?>, String> keys = Map.of(Hygrometry.class, "hygrometry", Landscape.class, "landscape", Plant.class, "plant", Bush.class, "bush", Temperature.class, "temperature", OreLevel.class, "ore_level");

    public static enum Hygrometry {
        FLOODING,
        RAIN,
        DRY,
        NONE;

    }

    public static enum Landscape {
        LIGHT_FOREST,
        FOREST,
        PLAIN,
        NONE;

    }

    public static enum Plant {
        FLOWER,
        GRASS,
        NONE;

    }

    public static enum Bush {
        DRY,
        REGULAR,
        FAT,
        NONE;

    }

    public static enum Temperature {
        COLD,
        MEDIUM,
        HOT,
        NONE;

    }

    public static enum OreLevel {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        NONE;

    }
}

