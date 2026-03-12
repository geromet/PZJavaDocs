/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.iso.worldgen.biomes.BiomeAbstract;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.utils.probabilities.ProbaDouble;

public class Biome
extends BiomeAbstract {
    public static final Biome DEFAULT_BIOME = new DefaultBiome();
    private final Map<FeatureType, List<Feature>> features;
    private final Map<String, List<Feature>> replacements;

    public Biome(String name, String parent, boolean generate, Map<FeatureType, List<Feature>> features, Map<String, List<Feature>> replacements, EnumSet<BiomeType.Landscape> landscape, EnumSet<BiomeType.Plant> plant, EnumSet<BiomeType.Bush> bush, EnumSet<BiomeType.Temperature> temperature, EnumSet<BiomeType.Hygrometry> hygrometry, EnumSet<BiomeType.OreLevel> oreLevel, float zombies, Map<FeatureType, List<String>> placements, List<String> protectedList, Grass grass) {
        this.name = name;
        this.parent = parent;
        this.generate = generate;
        this.features = features;
        this.replacements = replacements;
        if (landscape != null) {
            this.landscape = landscape;
        }
        if (plant != null) {
            this.plant = plant;
        }
        if (bush != null) {
            this.bush = bush;
        }
        if (temperature != null) {
            this.temperature = temperature;
        }
        if (hygrometry != null) {
            this.hygrometry = hygrometry;
        }
        if (oreLevel != null) {
            this.oreLevel = oreLevel;
        }
        this.placements = placements;
        this.protectedList = protectedList;
        this.zombies = zombies;
        this.grass = grass;
    }

    public String toString() {
        return String.format("<Biome@%s | %s | %s | %s | %s | %s | %s>", Integer.toHexString(this.hashCode()), this.landscape, this.plant, this.temperature, this.landscape, this.oreLevel, Float.valueOf(this.zombies));
    }

    @Override
    public Map<FeatureType, List<Feature>> getFeatures() {
        return this.features;
    }

    @Override
    public Map<String, List<Feature>> getReplacements() {
        return this.replacements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Biome biome = (Biome)o;
        return this.features.equals(biome.features);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + this.features.hashCode();
        return result;
    }

    private static class DefaultBiome
    extends Biome {
        public DefaultBiome() {
            super("DEFAULT", null, true, DefaultBiome.defaultMap(), null, EnumSet.of(BiomeType.Landscape.NONE), EnumSet.of(BiomeType.Plant.NONE), EnumSet.of(BiomeType.Bush.NONE), EnumSet.of(BiomeType.Temperature.NONE), EnumSet.of(BiomeType.Hygrometry.NONE), EnumSet.of(BiomeType.OreLevel.NONE), 0.0f, Map.of(), List.of(), new Grass(0.0f, 1.0f, List.of(Double.valueOf(0.4)), List.of(Double.valueOf(0.33), Double.valueOf(0.5))));
        }

        private static Map<FeatureType, List<Feature>> defaultMap() {
            HashMap<FeatureType, List<Feature>> map = new HashMap<FeatureType, List<Feature>>();
            List<String> tiles = List.of("carpentry_02_58");
            ArrayList<Feature> features = new ArrayList<Feature>();
            features.add(new Feature(List.of(new TileGroup(1, 1, tiles)), 1, 1, new ProbaDouble(1.0)));
            map.put(FeatureType.GROUND, features);
            return map;
        }
    }
}

