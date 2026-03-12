/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.VirtualAnimal;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;

public class MigrationGroupDefinitions {
    public String type;
    public String male;
    public String female;
    public String baby;
    public int minAnimal;
    public int maxAnimal;
    public int maxMale;
    public int babyChance;
    public int minTimeBeforeEat = 10;
    public int maxTimeBeforeEat = 60;
    public int timeToEat = 30;
    public int minTimeBeforeSleep = 180;
    public int maxTimeBeforeSleep = 600;
    public ArrayList<String> sleepPeriodStart;
    public ArrayList<String> sleepPeriodEnd;
    public ArrayList<String> eatPeriodStart;
    public ArrayList<String> eatPeriodEnd;
    public int timeToSleep = 30;
    public float speed = 1.0f;
    public int trackChance = 200;
    public int poopChance = 200;
    public int brokenTwigsChance = 200;
    public int herbGrazeChance = 200;
    public int furChance = 200;
    public int flatHerbChance = 200;
    ArrayList<String> possibleBreed;
    ArrayList<MigrationGroupDefinitionsGrouped> group;
    public static HashMap<String, MigrationGroupDefinitions> migrationDef;

    public static HashMap<String, MigrationGroupDefinitions> getMigrationDefs() {
        if (migrationDef == null) {
            MigrationGroupDefinitions.loadMigrationsDefinitions();
        }
        return migrationDef;
    }

    public static void loadMigrationsDefinitions() {
        migrationDef = new HashMap();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("MigrationGroupDefinitions");
        if (definitions == null) {
            return;
        }
        KahluaTableIterator iterator2 = definitions.iterator();
        while (iterator2.advance()) {
            MigrationGroupDefinitions def = new MigrationGroupDefinitions();
            def.type = iterator2.getKey().toString();
            KahluaTableImpl value2 = (KahluaTableImpl)iterator2.getValue();
            if (value2.rawget("groups") != null) {
                def.group = MigrationGroupDefinitions.loadGroup((KahluaTableImpl)value2.rawget("groups"));
                migrationDef.put(def.type, def);
                continue;
            }
            KahluaTableIterator it2 = value2.iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value = it2.getValue();
                String valueStr = value.toString().trim();
                if ("male".equalsIgnoreCase(key)) {
                    def.male = valueStr;
                }
                if ("female".equalsIgnoreCase(key)) {
                    def.female = valueStr;
                }
                if ("baby".equalsIgnoreCase(key)) {
                    def.baby = valueStr;
                }
                if ("maxAnimal".equalsIgnoreCase(key)) {
                    def.maxAnimal = Float.valueOf(valueStr).intValue();
                }
                if ("minAnimal".equalsIgnoreCase(key)) {
                    def.minAnimal = Float.valueOf(valueStr).intValue();
                }
                if ("maxMale".equalsIgnoreCase(key)) {
                    def.maxMale = Float.valueOf(valueStr).intValue();
                }
                if ("babyChance".equalsIgnoreCase(key)) {
                    def.babyChance = Float.valueOf(valueStr).intValue();
                }
                if ("minTimeBeforeEat".equalsIgnoreCase(key)) {
                    def.minTimeBeforeEat = Float.valueOf(valueStr).intValue();
                }
                if ("maxTimeBeforeEat".equalsIgnoreCase(key)) {
                    def.maxTimeBeforeEat = Float.valueOf(valueStr).intValue();
                }
                if ("timeToEat".equalsIgnoreCase(key)) {
                    def.timeToEat = Float.valueOf(valueStr).intValue();
                }
                if ("trackChance".equalsIgnoreCase(key)) {
                    def.trackChance = Float.valueOf(valueStr).intValue();
                }
                if ("poopChance".equalsIgnoreCase(key)) {
                    def.poopChance = Float.valueOf(valueStr).intValue();
                }
                if ("brokenTwigsChance".equalsIgnoreCase(key)) {
                    def.brokenTwigsChance = Float.valueOf(valueStr).intValue();
                }
                if ("herbGrazeChance".equalsIgnoreCase(key)) {
                    def.herbGrazeChance = Float.valueOf(valueStr).intValue();
                }
                if ("flatHerbChance".equalsIgnoreCase(key)) {
                    def.flatHerbChance = Float.valueOf(valueStr).intValue();
                }
                if ("furChance".equalsIgnoreCase(key)) {
                    def.furChance = Float.valueOf(valueStr).intValue();
                }
                if ("minTimeBeforeSleep".equalsIgnoreCase(key)) {
                    def.minTimeBeforeSleep = Float.valueOf(valueStr).intValue();
                }
                if ("maxTimeBeforeSleep".equalsIgnoreCase(key)) {
                    def.maxTimeBeforeSleep = Float.valueOf(valueStr).intValue();
                }
                if ("timeToSleep".equalsIgnoreCase(key)) {
                    def.timeToSleep = Float.valueOf(valueStr).intValue();
                }
                if ("sleepPeriodStart".equalsIgnoreCase(key)) {
                    def.sleepPeriodStart = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("sleepPeriodEnd".equalsIgnoreCase(key)) {
                    def.sleepPeriodEnd = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("eatPeriodEnd".equalsIgnoreCase(key)) {
                    def.eatPeriodEnd = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("eatPeriodStart".equalsIgnoreCase(key)) {
                    def.eatPeriodStart = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
                }
                if ("speed".equalsIgnoreCase(key)) {
                    def.speed = Float.parseFloat(valueStr);
                }
                if (!"possibleBreed".equalsIgnoreCase(key)) continue;
                def.possibleBreed = new ArrayList<String>(Arrays.asList(valueStr.split(",")));
            }
            migrationDef.put(def.type, def);
        }
    }

    private static ArrayList<MigrationGroupDefinitionsGrouped> loadGroup(KahluaTableImpl def) {
        KahluaTableIterator it = def.iterator();
        ArrayList<MigrationGroupDefinitionsGrouped> result = new ArrayList<MigrationGroupDefinitionsGrouped>();
        while (it.advance()) {
            String key = it.getKey().toString();
            KahluaTableImpl value = (KahluaTableImpl)it.getValue();
            MigrationGroupDefinitionsGrouped newGroup = new MigrationGroupDefinitionsGrouped();
            newGroup.animal = value.rawgetStr("animal");
            newGroup.chance = value.rawgetInt("chance");
            result.add(newGroup);
        }
        return result;
    }

    public static ArrayList<IsoAnimal> generatePossibleAnimals(VirtualAnimal vAnimal, String type) {
        MigrationGroupDefinitions def = MigrationGroupDefinitions.getMigrationDefs().get(type);
        if (def == null) {
            DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + type + " check MigrationGroupDefinitions.lua");
            return null;
        }
        if (def.group != null && !def.group.isEmpty()) {
            return MigrationGroupDefinitions.generatePossibleAnimalsFromGroup(vAnimal, def);
        }
        int virtualID = Rand.Next(1000000) + 1000000;
        vAnimal.migrationGroup = type;
        ArrayList<IsoAnimal> result = new ArrayList<IsoAnimal>();
        for (int i = 0; i < def.maxMale; ++i) {
            IsoAnimal male = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, def.male, def.getRandBreed());
            male.virtualId = virtualID;
            male.migrationGroup = type;
            result.add(male);
        }
        int animals = Rand.Next(def.minAnimal, def.maxAnimal + 1);
        for (int i = 0; i < animals; ++i) {
            IsoAnimal female = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, def.female, def.getRandBreed());
            female.randomizeAge();
            female.virtualId = virtualID;
            female.migrationGroup = type;
            result.add(female);
            if (!female.getData().canHaveBaby() || Rand.Next(100) >= def.babyChance) continue;
            result.add(female.addBaby());
        }
        vAnimal.animals.addAll(result);
        return result;
    }

    private static ArrayList<IsoAnimal> generatePossibleAnimalsFromGroup(VirtualAnimal vAnimal, MigrationGroupDefinitions def) {
        int totalChance = 0;
        int chanceIndex = 0;
        MigrationGroupDefinitions choosenDef = null;
        for (int i = 0; i < def.group.size(); ++i) {
            totalChance += def.group.get((int)i).chance;
        }
        int rand = Rand.Next(totalChance);
        for (int i = 0; i < def.group.size(); ++i) {
            int chance = def.group.get((int)i).chance;
            choosenDef = MigrationGroupDefinitions.getMigrationDefs().get(def.group.get((int)i).animal);
            if (chance + chanceIndex >= rand) break;
            chanceIndex += chance;
            choosenDef = null;
        }
        if (choosenDef == null) {
            DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + def.type + " check MigrationGroupDefinitions.lua");
            return null;
        }
        return MigrationGroupDefinitions.generatePossibleAnimals(vAnimal, choosenDef.type);
    }

    public static double getNextEatTime(String animalType) {
        MigrationGroupDefinitions def = MigrationGroupDefinitions.getMigrationDefs().get(animalType);
        if (def == null) {
            DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + animalType + " check MigrationGroupDefinitions.lua");
            return 0.0;
        }
        return GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(def.minTimeBeforeEat, def.maxTimeBeforeEat + 1) / 60.0;
    }

    public static double getNextSleepTime(String animalType) {
        MigrationGroupDefinitions def = MigrationGroupDefinitions.getMigrationDefs().get(animalType);
        if (def == null) {
            DebugLog.Animal.debugln("Couldn't find a migration group definition for type: " + animalType + " check MigrationGroupDefinitions.lua");
            return 0.0;
        }
        return GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(def.minTimeBeforeSleep, def.maxTimeBeforeSleep + 1) / 60.0;
    }

    public String getRandBreed() {
        return this.possibleBreed.get(Rand.Next(0, this.possibleBreed.size()));
    }

    public static void initValueFromDef(VirtualAnimal animal) {
        int i;
        MigrationGroupDefinitions def = MigrationGroupDefinitions.getMigrationDefs().get(animal.migrationGroup);
        if (def == null) {
            return;
        }
        animal.speed = def.speed;
        animal.timeToEat = def.timeToEat;
        animal.timeToSleep = def.timeToSleep;
        animal.trackChance = def.trackChance;
        animal.poopChance = def.poopChance;
        animal.brokenTwigsChance = def.brokenTwigsChance;
        animal.herbGrazeChance = def.herbGrazeChance;
        animal.furChance = def.furChance;
        animal.flatHerbChance = def.flatHerbChance;
        if (def.sleepPeriodStart != null) {
            for (i = 0; i < def.sleepPeriodStart.size(); ++i) {
                animal.sleepPeriodStart.add(Integer.parseInt(def.sleepPeriodStart.get(i)));
            }
        }
        if (def.sleepPeriodEnd != null) {
            for (i = 0; i < def.sleepPeriodEnd.size(); ++i) {
                animal.sleepPeriodEnd.add(Integer.parseInt(def.sleepPeriodEnd.get(i)));
            }
        }
        if (def.eatPeriodStart != null) {
            for (i = 0; i < def.eatPeriodStart.size(); ++i) {
                animal.eatPeriodStart.add(Integer.parseInt(def.eatPeriodStart.get(i)));
            }
        }
        if (def.eatPeriodEnd != null) {
            for (i = 0; i < def.eatPeriodEnd.size(); ++i) {
                animal.eatPeriodEnd.add(Integer.parseInt(def.eatPeriodEnd.get(i)));
            }
        }
    }

    public static void Reset() {
        migrationDef = null;
    }

    public static class MigrationGroupDefinitionsGrouped {
        public String animal;
        public int chance;
    }
}

