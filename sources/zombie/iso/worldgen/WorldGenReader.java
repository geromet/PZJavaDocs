/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.iso.worldgen.PrefabStructure;
import zombie.iso.worldgen.StaticModule;
import zombie.iso.worldgen.biomes.Biome;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.maps.BiomeMapEntry;
import zombie.iso.worldgen.roads.RoadConfig;
import zombie.iso.worldgen.utils.probabilities.ProbaDouble;
import zombie.iso.worldgen.utils.probabilities.ProbaString;
import zombie.iso.worldgen.utils.probabilities.Probability;
import zombie.iso.worldgen.veins.OreVeinConfig;
import zombie.iso.worldgen.zones.AnimalsPathConfig;

public class WorldGenReader {
    public Map<String, Biome> loadBiomes(KahluaTable worldgenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldgenTable.rawget(tableKey);
        HashMap<String, Biome> biomes = new HashMap<String, Biome>();
        KahluaTableIterator iterMainTable = mainTable.iterator();
        while (iterMainTable.advance()) {
            Object biomeName = iterMainTable.getKey();
            KahluaTable biomeTable = (KahluaTable)iterMainTable.getValue();
            biomes.put(biomeName.toString(), this.loadBiome(biomeName.toString(), biomeTable));
        }
        biomes.forEach((k, v) -> {
            Biome biome;
            ArrayList<Biome> biomeList = new ArrayList<Biome>(List.of(v));
            while ((biome = (Biome)biomeList.get(0)).parent() != null && !biome.parent().isEmpty()) {
                biomeList.add(0, (Biome)biomes.get(biome.parent()));
            }
            if (biomeList.size() == 1) {
                return;
            }
            HashMap<FeatureType, List<Feature>> features = new HashMap<FeatureType, List<Feature>>();
            HashMap<String, List<Feature>> replacements = new HashMap<String, List<Feature>>();
            EnumSet<BiomeType.Temperature> temperature = ((Biome)biomeList.get(0)).temperature();
            EnumSet<BiomeType.Plant> plant = ((Biome)biomeList.get(0)).plant();
            EnumSet<BiomeType.Bush> bush = ((Biome)biomeList.get(0)).bush();
            EnumSet<BiomeType.Landscape> landscape = ((Biome)biomeList.get(0)).landscape();
            EnumSet<BiomeType.Hygrometry> hygrometry = ((Biome)biomeList.get(0)).hygrometry();
            EnumSet<BiomeType.OreLevel> oreLevel = ((Biome)biomeList.get(0)).oreLevel();
            Map<FeatureType, List<String>> placements = ((Biome)biomeList.get(0)).placements();
            List<String> protectedBiomes = ((Biome)biomeList.get(0)).protectedList();
            float zombies = ((Biome)biomeList.get(0)).zombies();
            Grass grass = ((Biome)biomeList.get(0)).grass();
            for (Biome biome2 : biomeList) {
                Map<String, List<Feature>> biomeReplacements;
                Map<FeatureType, List<Feature>> biomeFeatures = biome2.getFeatures();
                if (biomeFeatures != null) {
                    if (biomeFeatures.get((Object)FeatureType.GROUND) != null) {
                        features.put(FeatureType.GROUND, biomeFeatures.get((Object)FeatureType.GROUND));
                    }
                    if (biomeFeatures.get((Object)FeatureType.TREE) != null) {
                        features.put(FeatureType.TREE, biomeFeatures.get((Object)FeatureType.TREE));
                    }
                    if (biomeFeatures.get((Object)FeatureType.PLANT) != null) {
                        features.put(FeatureType.PLANT, biomeFeatures.get((Object)FeatureType.PLANT));
                    }
                    if (biomeFeatures.get((Object)FeatureType.BUSH) != null) {
                        features.put(FeatureType.BUSH, biomeFeatures.get((Object)FeatureType.BUSH));
                    }
                    if (biomeFeatures.get((Object)FeatureType.ORE) != null) {
                        features.put(FeatureType.ORE, biomeFeatures.get((Object)FeatureType.ORE));
                    }
                }
                if ((biomeReplacements = biome2.getReplacements()) != null) {
                    replacements.putAll(biomeReplacements);
                }
                if (!Objects.equals(biome2.temperature(), EnumSet.allOf(BiomeType.Temperature.class))) {
                    temperature = biome2.temperature();
                }
                if (!Objects.equals(biome2.plant(), EnumSet.allOf(BiomeType.Plant.class))) {
                    plant = biome2.plant();
                }
                if (!Objects.equals(biome2.bush(), EnumSet.allOf(BiomeType.Bush.class))) {
                    bush = biome2.bush();
                }
                if (!Objects.equals(biome2.landscape(), EnumSet.allOf(BiomeType.Landscape.class))) {
                    landscape = biome2.landscape();
                }
                if (!Objects.equals(biome2.hygrometry(), EnumSet.allOf(BiomeType.Hygrometry.class))) {
                    hygrometry = biome2.hygrometry();
                }
                if (!Objects.equals(biome2.oreLevel(), EnumSet.allOf(BiomeType.OreLevel.class))) {
                    oreLevel = biome2.oreLevel();
                }
                if (biome2.placements() != null && !biome2.placements().isEmpty()) {
                    placements = biome2.placements();
                }
                if (biome2.protectedList() != null && !biome2.protectedList().isEmpty()) {
                    protectedBiomes = biome2.protectedList();
                }
                if ((double)biome2.zombies() >= 0.0) {
                    zombies = biome2.zombies();
                }
                grass = biome2.grass();
            }
            biomes.put((String)k, new Biome((String)k, v.parent(), v.generate(), (Map<FeatureType, List<Feature>>)features, (Map<String, List<Feature>>)replacements, landscape, plant, bush, temperature, hygrometry, oreLevel, zombies, placements, protectedBiomes, grass));
        });
        return biomes;
    }

    public Biome loadBiome(String biomeName, KahluaTable biomeTable) {
        Grass grass;
        if (biomeTable == null) {
            return null;
        }
        String parent = (String)biomeTable.rawget("parent");
        KahluaTable featuresTable = (KahluaTable)biomeTable.rawget("features");
        Map<FeatureType, List<Feature>> features = this.loadBiomeFeatures(featuresTable);
        KahluaTable replacementsTable = (KahluaTable)biomeTable.rawget("replacements");
        Map<String, List<Feature>> replacements = this.loadReplacements(replacementsTable);
        KahluaTable paramsTable = (KahluaTable)biomeTable.rawget("params");
        EnumSet<BiomeType.Landscape> landscapes = this.loadBiomeType(BiomeType.Landscape.NONE, paramsTable.rawget("landscape"));
        EnumSet<BiomeType.Plant> plants = this.loadBiomeType(BiomeType.Plant.NONE, paramsTable.rawget("plant"));
        EnumSet<BiomeType.Bush> bushes = this.loadBiomeType(BiomeType.Bush.NONE, paramsTable.rawget("bush"));
        EnumSet<BiomeType.Temperature> temperatures = this.loadBiomeType(BiomeType.Temperature.NONE, paramsTable.rawget("temperature"));
        EnumSet<BiomeType.Hygrometry> hygrometries = this.loadBiomeType(BiomeType.Hygrometry.NONE, paramsTable.rawget("hygrometry"));
        EnumSet<BiomeType.OreLevel> oreLevels = this.loadBiomeType(BiomeType.OreLevel.NONE, paramsTable.rawget("ore_level"));
        Map<FeatureType, List<String>> placements = this.loadPlacements((KahluaTable)paramsTable.rawget("placements"));
        List<String> protectedBiomes = this.loadList((KahluaTable)paramsTable.rawget("protected"));
        KahluaTable grassTable = (KahluaTable)paramsTable.rawget("grass");
        if (grassTable != null) {
            float grassFernChance = this.loadDouble(grassTable.rawget("fernChance"), 0.7).floatValue();
            float grassNoGrassDiv = this.loadDouble(grassTable.rawget("noGrassDiv"), 3.0).floatValue();
            List<Double> noGrassStages = this.loadList((KahluaTable)grassTable.rawget("noGrassStages")).isEmpty() ? List.of(Double.valueOf(0.4)) : this.loadList((KahluaTable)grassTable.rawget("noGrassStages"));
            List<Double> grassStages = this.loadList((KahluaTable)grassTable.rawget("grassStages")).isEmpty() ? List.of(Double.valueOf(0.33), Double.valueOf(0.5)) : this.loadList((KahluaTable)grassTable.rawget("grassStages"));
            grass = new Grass(grassFernChance, grassNoGrassDiv, noGrassStages, grassStages);
        } else {
            grass = new Grass(0.7f, 3.0f, List.of(Double.valueOf(0.4)), List.of(Double.valueOf(0.33), Double.valueOf(0.5)));
        }
        Double zombies = this.loadDouble(paramsTable.rawget("zombies"), -1.0);
        boolean generate = this.loadBoolean(paramsTable.rawget("generate"), true);
        return new Biome(biomeName, parent, generate, features, replacements, landscapes, plants, bushes, temperatures, hygrometries, oreLevels, zombies.floatValue(), placements, protectedBiomes, grass);
    }

    public <T extends Enum<T>> Map<T, List<Double>> loadSelection(T biomeType, KahluaTable worldgenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldgenTable.rawget(tableKey);
        KahluaTableIterator iterTable = ((KahluaTable)mainTable.rawget(BiomeType.keys.get(biomeType.getDeclaringClass()))).iterator();
        HashMap<T, List<T>> selections = new HashMap<T, List<T>>();
        while (iterTable.advance()) {
            T name = Enum.valueOf(biomeType.getDeclaringClass(), (String)iterTable.getKey());
            List<T> values2 = this.loadList((KahluaTable)iterTable.getValue());
            selections.put(name, values2);
        }
        return selections;
    }

    private Map<FeatureType, List<Feature>> loadBiomeFeatures(KahluaTable mainTable) {
        if (mainTable == null) {
            return null;
        }
        HashMap<FeatureType, List<Feature>> types = new HashMap<FeatureType, List<Feature>>();
        KahluaTableIterator iterTypesTable = mainTable.iterator();
        while (iterTypesTable.advance()) {
            String typeName = (String)iterTypesTable.getKey();
            KahluaTable typeTable = (KahluaTable)iterTypesTable.getValue();
            ArrayList<Feature> features = new ArrayList<Feature>();
            KahluaTableIterator iterTypeTable = typeTable.iterator();
            while (iterTypeTable.advance()) {
                Probability probability;
                Object featuresObj = ((KahluaTable)iterTypeTable.getValue()).rawget("f");
                Object probaObj = ((KahluaTable)iterTypeTable.getValue()).rawget("p");
                if (featuresObj == null || probaObj == null) {
                    throw new RuntimeException(String.format("Features not found or probability absents | %s | %s", featuresObj, probaObj));
                }
                List<TileGroup> tileGroups = this.loadFeatures((KahluaTable)((KahluaTable)featuresObj).rawget("main"));
                if (probaObj instanceof Double) {
                    Double d = (Double)probaObj;
                    probability = new ProbaDouble(d);
                } else if (probaObj instanceof String) {
                    String s = (String)probaObj;
                    probability = new ProbaString(s);
                } else {
                    throw new RuntimeException("Unsupported probability type");
                }
                int minSize = Integer.MAX_VALUE;
                int maxSize = 0;
                for (TileGroup tileGroup : tileGroups) {
                    minSize = Math.min(Math.min(minSize, tileGroup.sx()), tileGroup.sy());
                    maxSize = Math.max(Math.max(maxSize, tileGroup.sx()), tileGroup.sy());
                }
                features.add(new Feature(tileGroups, minSize, maxSize, probability));
            }
            types.put(FeatureType.valueOf(typeName), features);
        }
        return types;
    }

    private List<TileGroup> loadFeatures(KahluaTable mainTable) {
        ArrayList<TileGroup> features = new ArrayList<TileGroup>();
        KahluaTableIterator iterMainTable = mainTable.iterator();
        while (iterMainTable.advance()) {
            Object value = iterMainTable.getValue();
            if (value instanceof String) {
                String s = (String)value;
                features.add(new TileGroup(1, 1, List.of(s)));
                continue;
            }
            if (value instanceof KahluaTable) {
                KahluaTable kahluaTable = (KahluaTable)value;
                int sx = 0;
                int sy = 0;
                ArrayList<String> tiles = new ArrayList<String>();
                KahluaTableIterator iterOuter = kahluaTable.iterator();
                while (iterOuter.advance()) {
                    ++sy;
                    List t = this.loadList((KahluaTable)iterOuter.getValue());
                    sx = t.size();
                    tiles.addAll(t);
                }
                features.add(new TileGroup(sx, sy, tiles));
                continue;
            }
            throw new RuntimeException("Only strings and tables in there!");
        }
        return features;
    }

    public Map<String, List<Feature>> loadReplacements(KahluaTable replacementsTable) {
        HashMap<String, List<Feature>> replacements = new HashMap<String, List<Feature>>();
        if (replacementsTable != null) {
            KahluaTableIterator iterReplacementsTable = replacementsTable.iterator();
            while (iterReplacementsTable.advance()) {
                String key = (String)iterReplacementsTable.getKey();
                KahluaTable value = (KahluaTable)iterReplacementsTable.getValue();
                ArrayList<Feature> features = new ArrayList<Feature>();
                KahluaTableIterator iterFeaturesTable = value.iterator();
                while (iterFeaturesTable.advance()) {
                    Probability probability;
                    Object featuresObj = ((KahluaTable)iterFeaturesTable.getValue()).rawget("f");
                    Object probaObj = ((KahluaTable)iterFeaturesTable.getValue()).rawget("p");
                    if (featuresObj == null || probaObj == null) {
                        throw new RuntimeException(String.format("Features not found or probability absents | %s | %s", featuresObj, probaObj));
                    }
                    List<TileGroup> tileGroups = this.loadFeatures((KahluaTable)((KahluaTable)featuresObj).rawget("main"));
                    if (probaObj instanceof Double) {
                        Double d = (Double)probaObj;
                        probability = new ProbaDouble(d);
                    } else if (probaObj instanceof String) {
                        String s = (String)probaObj;
                        probability = new ProbaString(s);
                    } else {
                        throw new RuntimeException("Unsupported probability type");
                    }
                    int minSize = Integer.MAX_VALUE;
                    int maxSize = 0;
                    for (TileGroup tileGroup : tileGroups) {
                        minSize = Math.min(Math.min(minSize, tileGroup.sx()), tileGroup.sy());
                        maxSize = Math.max(Math.max(maxSize, tileGroup.sx()), tileGroup.sy());
                    }
                    features.add(new Feature(tileGroups, minSize, maxSize, probability));
                }
                replacements.put(key, features);
            }
        }
        return replacements;
    }

    private Map<FeatureType, List<String>> loadPlacements(KahluaTable placementsTable) {
        if (placementsTable == null) {
            return null;
        }
        HashMap<FeatureType, List<String>> placements = new HashMap<FeatureType, List<String>>();
        KahluaTableIterator iter = placementsTable.iterator();
        HashMap map = new HashMap();
        while (iter.advance()) {
            String typeName = (String)iter.getKey();
            List placement = this.loadList((KahluaTable)iter.getValue());
            map.put(typeName, placement);
        }
        for (FeatureType type : FeatureType.values()) {
            ArrayList p = new ArrayList();
            if (map.containsKey("GENERIC")) {
                p.addAll((Collection)map.get("GENERIC"));
            }
            if (map.containsKey(type.toString())) {
                p.addAll((Collection)map.get(type.toString()));
            }
            placements.put(type, p);
        }
        return placements;
    }

    public Map<String, Double> loadPriorities(KahluaTable worldgenTable, String tableKey) {
        HashMap<String, Double> priorities = new HashMap<String, Double>();
        KahluaTable priorityTable = (KahluaTable)worldgenTable.rawget(tableKey);
        KahluaTableIterator iterPriorityTable = priorityTable.iterator();
        while (iterPriorityTable.advance()) {
            priorities.put((String)iterPriorityTable.getValue(), (Double)iterPriorityTable.getKey());
        }
        return priorities;
    }

    private <T extends Enum<T>> EnumSet<T> loadBiomeType(T biomeType, Object object) {
        if (object != null) {
            KahluaTable landscapeTable = (KahluaTable)object;
            KahluaTableIterator iterLandscapeTable = landscapeTable.iterator();
            ArrayList<T> tmpLandscapes = new ArrayList<T>();
            while (iterLandscapeTable.advance()) {
                tmpLandscapes.add(Enum.valueOf(biomeType.getDeclaringClass(), (String)iterLandscapeTable.getValue()));
            }
            if (tmpLandscapes.isEmpty()) {
                return EnumSet.of(biomeType);
            }
            return EnumSet.copyOf(tmpLandscapes);
        }
        return null;
    }

    public List<StaticModule> loadStaticModules(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new ArrayList<StaticModule>();
        }
        ArrayList<StaticModule> staticModules = new ArrayList<StaticModule>();
        KahluaTableIterator mainTableIter = mainTable.iterator();
        while (mainTableIter.advance()) {
            KahluaTable staticModuleTable = (KahluaTable)mainTableIter.getValue();
            KahluaTable positionTable = (KahluaTable)staticModuleTable.rawget("position");
            Double xmin = this.loadDouble(positionTable.rawget("xmin"), -1.7976931348623157E308);
            Double xmax = this.loadDouble(positionTable.rawget("xmax"), (Double)Double.MAX_VALUE);
            Double ymin = this.loadDouble(positionTable.rawget("ymin"), -1.7976931348623157E308);
            Double ymax = this.loadDouble(positionTable.rawget("ymax"), (Double)Double.MAX_VALUE);
            Biome biome = this.loadBiome("", (KahluaTable)staticModuleTable.rawget("biome"));
            PrefabStructure structure = this.loadPrefab((KahluaTable)staticModuleTable.rawget("prefab"));
            if (biome == null && structure == null) {
                throw new RuntimeException("Need at least one of 'biome' or 'prefab' in WorldGenOverride.lua/worlgen.static_modules");
            }
            staticModules.add(new StaticModule(biome, structure, xmin.intValue(), xmax.intValue(), ymin.intValue(), ymax.intValue()));
        }
        return staticModules;
    }

    private PrefabStructure loadPrefab(KahluaTable prefabTable) {
        if (prefabTable == null) {
            return null;
        }
        KahluaTable dimensionTable = (KahluaTable)prefabTable.rawget("dimensions");
        KahluaTableIterator dimensionTableIter = dimensionTable.iterator();
        int[] dimensions = new int[2];
        while (dimensionTableIter.advance()) {
            int i = ((Double)dimensionTableIter.getKey()).intValue() - 1;
            dimensions[i] = ((Double)dimensionTableIter.getValue()).intValue();
        }
        List<String> tiles = this.loadList((KahluaTable)prefabTable.rawget("tiles"));
        KahluaTable schematicTable = (KahluaTable)prefabTable.rawget("schematic");
        KahluaTableIterator schematicTableIter = schematicTable.iterator();
        HashMap<String, int[][]> schematic = new HashMap<String, int[][]>();
        while (schematicTableIter.advance()) {
            String key = (String)schematicTableIter.getKey();
            KahluaTable valueTable = (KahluaTable)schematicTableIter.getValue();
            KahluaTableIterator valueTableIter = valueTable.iterator();
            int[][] subSchem = new int[dimensions[1]][dimensions[0]];
            while (valueTableIter.advance()) {
                int i = ((Double)valueTableIter.getKey()).intValue() - 1;
                subSchem[i] = Stream.of(((String)valueTableIter.getValue()).split(",")).mapToInt(Integer::parseInt).toArray();
            }
            schematic.put(key, subSchem);
        }
        Double zombies = this.loadDouble(prefabTable.rawget("zombies"), 0.0);
        return new PrefabStructure(dimensions, tiles, schematic, zombies.floatValue());
    }

    public Map<String, OreVeinConfig> loadVeinsConfig(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new HashMap<String, OreVeinConfig>();
        }
        HashMap<String, OreVeinConfig> configs = new HashMap<String, OreVeinConfig>();
        KahluaTableIterator iterMainTable = mainTable.iterator();
        while (iterMainTable.advance()) {
            String key = (String)iterMainTable.getKey();
            KahluaTable subTable = (KahluaTable)iterMainTable.getValue();
            KahluaTable featureTable = (KahluaTable)subTable.rawget("feature");
            List<TileGroup> tiles = this.loadFeatures((KahluaTable)((KahluaTable)featureTable.rawget("f")).rawget("main"));
            KahluaTable armsTable = (KahluaTable)subTable.rawget("arms");
            int armsAmountMin = this.loadInteger(armsTable.rawget("amount_min"), 3);
            int armsAmountMax = this.loadInteger(armsTable.rawget("amount_max"), 6);
            int armsDistMin = this.loadInteger(armsTable.rawget("distance_min"), 100);
            int armsDistMax = this.loadInteger(armsTable.rawget("distance_max"), 400);
            int armsDeltaAngle = this.loadInteger(armsTable.rawget("delta_angle"), 5);
            float armsProb = this.loadDouble(armsTable.rawget("p"), 0.25).floatValue();
            KahluaTable centerTable = (KahluaTable)subTable.rawget("center");
            int centerRadius = this.loadInteger(centerTable.rawget("radius"), 5);
            float centerProb = this.loadDouble(centerTable.rawget("p"), 0.5).floatValue();
            float probability = this.loadDouble(subTable.rawget("p"), 0.01).floatValue();
            configs.put(key, new OreVeinConfig(tiles, centerRadius, centerProb, armsAmountMin, armsAmountMax, armsDistMin, armsDistMax, armsDeltaAngle, armsProb, probability));
        }
        return configs;
    }

    public Map<String, RoadConfig> loadRoadConfig(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new HashMap<String, RoadConfig>();
        }
        HashMap<String, RoadConfig> configs = new HashMap<String, RoadConfig>();
        KahluaTableIterator iterMainTable = mainTable.iterator();
        while (iterMainTable.advance()) {
            String key = (String)iterMainTable.getKey();
            KahluaTable subTable = (KahluaTable)iterMainTable.getValue();
            KahluaTable featureTable = (KahluaTable)subTable.rawget("feature");
            List<TileGroup> tiles = this.loadFeatures((KahluaTable)((KahluaTable)featureTable.rawget("f")).rawget("main"));
            double probaRoads = this.loadDouble(featureTable.rawget("p"), 1.0);
            double filter = this.loadDouble(subTable.rawget("filter_edge"), 5.0E8);
            double probability = this.loadDouble(subTable.rawget("p"), 5.0E-4);
            configs.put(key, new RoadConfig(tiles, probaRoads, probability, filter));
        }
        return configs;
    }

    public List<AnimalsPathConfig> loadAnimalsPath(KahluaTable animalsPathTable) {
        ArrayList<AnimalsPathConfig> animalsPathConfig = new ArrayList<AnimalsPathConfig>();
        KahluaTableIterator iterAnimalsPath = animalsPathTable.iterator();
        while (iterAnimalsPath.advance()) {
            List<Object> list;
            List<Object> radiusDouble;
            List<Object> pointsDouble;
            Object animalName = iterAnimalsPath.getKey();
            KahluaTable subTable = (KahluaTable)iterAnimalsPath.getValue();
            Object animalObj = subTable.rawget("animal");
            Object countObj = subTable.rawget("count");
            Object chanceObj = subTable.rawget("chance");
            Object pointsObj = subTable.rawget("points");
            Object radiusObj = subTable.rawget("radius");
            Object extensionObj = subTable.rawget("extension");
            Object extensionChanceObj = subTable.rawget("extension_chance");
            String animal = this.loadString(animalObj, null);
            int count = this.loadInteger(countObj, 1);
            Double chance = this.loadDouble(chanceObj, 0.0);
            if (pointsObj instanceof KahluaTable) {
                KahluaTable table = (KahluaTable)pointsObj;
                v0 = this.loadList(table);
            } else {
                v0 = pointsDouble = List.of(this.loadDouble(pointsObj, -1.0));
            }
            if (radiusObj instanceof KahluaTable) {
                KahluaTable table = (KahluaTable)radiusObj;
                v1 = this.loadList(table);
            } else {
                v1 = radiusDouble = List.of(this.loadDouble(radiusObj, -1.0));
            }
            if (extensionObj instanceof KahluaTable) {
                KahluaTable table = (KahluaTable)extensionObj;
                list = this.loadList(table);
            } else {
                list = List.of(this.loadDouble(extensionObj, -1.0));
            }
            List<Object> extensionDouble = list;
            Double extensionChance = this.loadDouble(extensionChanceObj, 1.0);
            int[] points = pointsDouble.stream().mapToInt(Double::intValue).toArray();
            int[] radius = radiusDouble.stream().mapToInt(Double::intValue).toArray();
            int[] extension = extensionDouble.stream().mapToInt(Double::intValue).toArray();
            if (animal == null) continue;
            animalsPathConfig.add(new AnimalsPathConfig(animal, count, chance.floatValue(), points, radius, extension, extensionChance.floatValue()));
        }
        return animalsPathConfig;
    }

    public Map<Integer, BiomeMapEntry> loadBiomeMapConfig(KahluaTable biomeMapTable) {
        HashMap<Integer, BiomeMapEntry> biomeMap = new HashMap<Integer, BiomeMapEntry>();
        KahluaTableIterator iterBiomeMapConfig = biomeMapTable.iterator();
        while (iterBiomeMapConfig.advance()) {
            KahluaTable subTable = (KahluaTable)iterBiomeMapConfig.getValue();
            int pixel = this.loadInteger(subTable.rawget("pixel"), 0);
            String biome = this.loadString(subTable.rawget("biome"), null);
            String ore = this.loadString(subTable.rawget("ore"), null);
            String zone = this.loadString(subTable.rawget("zone"), null);
            biomeMap.put(pixel, new BiomeMapEntry(pixel, biome, ore, zone));
        }
        return biomeMap;
    }

    private <T> T[] loadArray(KahluaTable mainTable) {
        return this.loadList(mainTable).toArray();
    }

    private <T> List<T> loadList(KahluaTable mainTable) {
        ArrayList<Object> tiles = new ArrayList<Object>();
        if (mainTable == null) {
            return tiles;
        }
        KahluaTableIterator iterFeaturesTable = mainTable.iterator();
        while (iterFeaturesTable.advance()) {
            tiles.add(iterFeaturesTable.getValue());
        }
        return tiles;
    }

    private int loadInteger(Object object, int defaultValue) {
        return object == null ? defaultValue : ((Double)object).intValue();
    }

    private Double loadDouble(Object object, Double defaultValue) {
        return object == null ? defaultValue : (Double)object;
    }

    private String loadString(Object object, String defaultValue) {
        return object == null ? defaultValue : (String)object;
    }

    private boolean loadBoolean(Object object, boolean defaultValue) {
        return object == null ? defaultValue : (Boolean)object;
    }
}

