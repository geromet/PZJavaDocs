/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;

public interface IBiome {
    public String name();

    public Map<FeatureType, List<Feature>> getFeatures();

    public Map<String, List<Feature>> getReplacements();

    public EnumSet<BiomeType.Landscape> landscape();

    public EnumSet<BiomeType.Plant> plant();

    public EnumSet<BiomeType.Bush> bush();

    public EnumSet<BiomeType.Temperature> temperature();

    public EnumSet<BiomeType.Hygrometry> hygrometry();

    public EnumSet<BiomeType.OreLevel> oreLevel();

    public Map<FeatureType, List<String>> placements();

    public List<String> protectedList();

    public String parent();

    public boolean generate();

    public float zombies();

    public Grass grass();

    public IBiome landscape(BiomeType.Landscape var1);

    public IBiome plant(BiomeType.Plant var1);

    public IBiome bush(BiomeType.Bush var1);

    public IBiome temperature(BiomeType.Temperature var1);

    public IBiome hygrometry(BiomeType.Hygrometry var1);

    public IBiome oreLevel(BiomeType.OreLevel var1);

    public IBiome landscape(EnumSet<BiomeType.Landscape> var1);

    public IBiome plant(EnumSet<BiomeType.Plant> var1);

    public IBiome bush(EnumSet<BiomeType.Bush> var1);

    public IBiome temperature(EnumSet<BiomeType.Temperature> var1);

    public IBiome hygrometry(EnumSet<BiomeType.Hygrometry> var1);

    public IBiome oreLevel(EnumSet<BiomeType.OreLevel> var1);

    public IBiome placements(Map<FeatureType, List<String>> var1);

    public IBiome protectedList(List<String> var1);

    public IBiome zombies(float var1);

    public IBiome grass(Grass var1);
}

