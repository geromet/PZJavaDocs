/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen.biomes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;
import zombie.iso.worldgen.biomes.IBiome;

public abstract class BiomeAbstract
implements IBiome {
    protected EnumSet<BiomeType.Landscape> landscape = EnumSet.allOf(BiomeType.Landscape.class);
    protected EnumSet<BiomeType.Plant> plant = EnumSet.allOf(BiomeType.Plant.class);
    protected EnumSet<BiomeType.Bush> bush = EnumSet.allOf(BiomeType.Bush.class);
    protected EnumSet<BiomeType.Temperature> temperature = EnumSet.allOf(BiomeType.Temperature.class);
    protected EnumSet<BiomeType.Hygrometry> hygrometry = EnumSet.allOf(BiomeType.Hygrometry.class);
    protected EnumSet<BiomeType.OreLevel> oreLevel = EnumSet.allOf(BiomeType.OreLevel.class);
    protected Map<FeatureType, List<String>> placements = new HashMap<FeatureType, List<String>>();
    protected List<String> protectedList = new ArrayList<String>();
    protected String name;
    protected String parent;
    protected boolean generate;
    protected Grass grass;
    protected float zombies;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public abstract Map<FeatureType, List<Feature>> getFeatures();

    @Override
    public abstract Map<String, List<Feature>> getReplacements();

    @Override
    public EnumSet<BiomeType.Landscape> landscape() {
        return this.landscape;
    }

    @Override
    public EnumSet<BiomeType.Plant> plant() {
        return this.plant;
    }

    @Override
    public EnumSet<BiomeType.Bush> bush() {
        return this.bush;
    }

    @Override
    public EnumSet<BiomeType.Temperature> temperature() {
        return this.temperature;
    }

    @Override
    public EnumSet<BiomeType.Hygrometry> hygrometry() {
        return this.hygrometry;
    }

    @Override
    public EnumSet<BiomeType.OreLevel> oreLevel() {
        return this.oreLevel;
    }

    @Override
    public Map<FeatureType, List<String>> placements() {
        return this.placements;
    }

    @Override
    public List<String> protectedList() {
        return this.protectedList;
    }

    @Override
    public String parent() {
        return this.parent;
    }

    @Override
    public boolean generate() {
        return this.generate;
    }

    @Override
    public float zombies() {
        return this.zombies;
    }

    @Override
    public Grass grass() {
        return this.grass;
    }

    @Override
    public IBiome landscape(BiomeType.Landscape landscape) {
        this.landscape = EnumSet.of(landscape);
        return this;
    }

    @Override
    public IBiome plant(BiomeType.Plant plant) {
        this.plant = EnumSet.of(plant);
        return this;
    }

    @Override
    public IBiome bush(BiomeType.Bush bush) {
        this.bush = EnumSet.of(bush);
        return this;
    }

    @Override
    public IBiome temperature(BiomeType.Temperature temperature) {
        this.temperature = EnumSet.of(temperature);
        return this;
    }

    @Override
    public IBiome hygrometry(BiomeType.Hygrometry hygrometry) {
        this.hygrometry = EnumSet.of(hygrometry);
        return this;
    }

    @Override
    public IBiome oreLevel(BiomeType.OreLevel oreLevel) {
        this.oreLevel = EnumSet.of(oreLevel);
        return this;
    }

    @Override
    public IBiome landscape(EnumSet<BiomeType.Landscape> landscape) {
        this.landscape = landscape;
        return this;
    }

    @Override
    public IBiome plant(EnumSet<BiomeType.Plant> plant) {
        this.plant = plant;
        return this;
    }

    @Override
    public IBiome bush(EnumSet<BiomeType.Bush> bush) {
        this.bush = bush;
        return this;
    }

    @Override
    public IBiome temperature(EnumSet<BiomeType.Temperature> temperature) {
        this.temperature = temperature;
        return this;
    }

    @Override
    public IBiome hygrometry(EnumSet<BiomeType.Hygrometry> hygrometry) {
        this.hygrometry = hygrometry;
        return this;
    }

    @Override
    public IBiome oreLevel(EnumSet<BiomeType.OreLevel> oreLevel) {
        this.oreLevel = oreLevel;
        return this;
    }

    @Override
    public IBiome placements(Map<FeatureType, List<String>> placement) {
        this.placements = placement;
        return this;
    }

    @Override
    public IBiome protectedList(List<String> protectedList) {
        this.protectedList = protectedList;
        return this;
    }

    @Override
    public IBiome zombies(float chance) {
        this.zombies = chance;
        return this;
    }

    @Override
    public IBiome grass(Grass grass) {
        this.grass = grass;
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        BiomeAbstract that = (BiomeAbstract)o;
        return this.generate == that.generate && Float.compare(this.zombies, that.zombies) == 0 && Objects.equals(this.landscape, that.landscape) && Objects.equals(this.plant, that.plant) && Objects.equals(this.bush, that.bush) && Objects.equals(this.temperature, that.temperature) && Objects.equals(this.hygrometry, that.hygrometry) && Objects.equals(this.oreLevel, that.oreLevel) && Objects.equals(this.placements, that.placements) && Objects.equals(this.name, that.name) && Objects.equals(this.parent, that.parent) && Objects.equals(this.grass, that.grass);
    }

    public int hashCode() {
        int result = Objects.hashCode(this.landscape);
        result = 31 * result + Objects.hashCode(this.plant);
        result = 31 * result + Objects.hashCode(this.bush);
        result = 31 * result + Objects.hashCode(this.temperature);
        result = 31 * result + Objects.hashCode(this.hygrometry);
        result = 31 * result + Objects.hashCode(this.oreLevel);
        result = 31 * result + Objects.hashCode(this.placements);
        result = 31 * result + Objects.hashCode(this.name);
        result = 31 * result + Objects.hashCode(this.parent);
        result = 31 * result + Boolean.hashCode(this.generate);
        result = 31 * result + Objects.hashCode(this.grass);
        result = 31 * result + Float.hashCode(this.zombies);
        return result;
    }
}

