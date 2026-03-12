/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedRanch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedRanch.RanchZoneDefinitions;
import zombie.util.StringUtils;

public class RandomizedRanchBase
extends RandomizedWorldBase {
    public boolean alwaysDo;
    public static final int baseChance = 15;
    public static int totalChance;
    public static final String ranchStory = "Ranch";
    public int chance;

    public static boolean checkRanchStory(Zone zone, boolean force) {
        if (ranchStory.equals(zone.type) && zone.isFullyStreamed() && zone.hourLastSeen == 0) {
            DesignationZoneAnimal newZone = new DesignationZoneAnimal(ranchStory, zone.x, zone.y, zone.z, zone.x + zone.getWidth(), zone.y + zone.getHeight(), true);
            ArrayList<DesignationZoneAnimal> connectedZone = DesignationZoneAnimal.getAllDZones(null, newZone, null);
            for (int i = 0; i < connectedZone.size(); ++i) {
                DesignationZoneAnimal checkZone = connectedZone.get(i);
                if (checkZone.hourLastSeen <= 0) continue;
                newZone.setName(checkZone.getName());
                newZone.hourLastSeen = checkZone.hourLastSeen;
            }
            if (newZone.hourLastSeen == 0) {
                RandomizedRanchBase.doRandomRanch(zone, force, newZone);
            }
            ++newZone.hourLastSeen;
            ++zone.hourLastSeen;
        } else {
            return false;
        }
        return true;
    }

    private static boolean doRandomRanch(Zone zone, boolean force, DesignationZoneAnimal newDZone) {
        ++zone.hourLastSeen;
        int chance = 6;
        switch (SandboxOptions.instance.animalRanchChance.getValue()) {
            case 1: {
                return false;
            }
            case 2: {
                chance = 7;
                break;
            }
            case 4: {
                chance = 20;
                break;
            }
            case 5: {
                chance = 55;
                break;
            }
            case 6: {
                chance = 85;
                break;
            }
            case 7: {
                chance = 120;
            }
        }
        if (force) {
            chance = 100;
        }
        if (Rand.Next(100) < chance) {
            RandomizedRanchBase.randomizeRanch(zone, newDZone);
        }
        return false;
    }

    public static RanchZoneDefinitions getRandomDef(String animalType) {
        RanchZoneDefinitions choosenDef = null;
        HashMap<String, ArrayList<RanchZoneDefinitions>> defs = RanchZoneDefinitions.getDefs();
        int rand = Rand.Next(RanchZoneDefinitions.totalChance);
        int chanceIndex = 0;
        if (!StringUtils.isNullOrEmpty(animalType)) {
            int j;
            int totalChance = 0;
            ArrayList<RanchZoneDefinitions> possibleDefs = defs.get(animalType);
            if (possibleDefs == null) {
                DebugLog.Animal.debugln(animalType + " wasn't found in the RanchZoneDefinitions");
                return null;
            }
            for (j = 0; j < possibleDefs.size(); ++j) {
                totalChance += possibleDefs.get((int)j).chance;
            }
            rand = Rand.Next(totalChance);
            for (j = 0; j < possibleDefs.size(); ++j) {
                choosenDef = possibleDefs.get(j);
                if (choosenDef.chance + chanceIndex >= rand) break;
                chanceIndex += choosenDef.chance;
                choosenDef = null;
            }
            return choosenDef;
        }
        Iterator<String> it = defs.keySet().iterator();
        while (it.hasNext() && choosenDef == null) {
            String defType = it.next();
            ArrayList<RanchZoneDefinitions> possibleDefs = defs.get(defType);
            for (int j = 0; j < possibleDefs.size(); ++j) {
                choosenDef = possibleDefs.get(j);
                if (choosenDef.chance + chanceIndex >= rand) {
                    return choosenDef;
                }
                chanceIndex += choosenDef.chance;
                choosenDef = null;
            }
        }
        return null;
    }

    public static void randomizeRanch(Zone zone, DesignationZoneAnimal newDZone) {
        int i;
        String animalType = zone.name;
        RanchZoneDefinitions choosenDef = RandomizedRanchBase.getRandomDef(animalType);
        if (choosenDef == null) {
            DebugLog.Animal.debugln("No def was found for this ranch " + animalType + " was found in the RanchZoneDefinitions");
            return;
        }
        if (!choosenDef.possibleDef.isEmpty()) {
            choosenDef = RandomizedRanchBase.getDefInPossibleDefList(choosenDef.possibleDef);
        }
        AnimalDefinitions femaleDef = AnimalDefinitions.getDef(choosenDef.femaleType);
        AnimalDefinitions maleDef = AnimalDefinitions.getDef(choosenDef.maleType);
        if (femaleDef == null) {
            DebugLog.Animal.debugln("No female def was found for " + choosenDef.femaleType);
            return;
        }
        if (maleDef == null) {
            DebugLog.Animal.debugln("No male def was found for " + choosenDef.maleType);
            return;
        }
        AnimalBreed breed = null;
        boolean randomBreed = true;
        if (!StringUtils.isNullOrEmpty(choosenDef.forcedBreed)) {
            breed = femaleDef.getBreedByName(choosenDef.forcedBreed);
            randomBreed = false;
            if (breed == null) {
                DebugLog.Animal.debugln("No breed def was found for " + choosenDef.forcedBreed + " taking random one");
                randomBreed = true;
            }
        }
        int femaleNb = Rand.Next(choosenDef.minFemaleNb, choosenDef.maxFemaleNb + 1);
        int maleNb = Rand.Next(choosenDef.minMaleNb, choosenDef.maxMaleNb + 1);
        if (Rand.Next(100) > choosenDef.maleChance) {
            maleNb = 0;
        }
        for (i = 0; i < femaleNb; ++i) {
            int randValue;
            IsoGridSquare sq;
            if (randomBreed) {
                breed = RandomizedRanchBase.getRandomBreed(femaleDef);
            }
            if ((sq = zone.getRandomFreeSquareInZone()) == null) {
                DebugLog.Animal.debugln("No free square was found in the zone.");
                return;
            }
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), sq.x, sq.y, zone.z, femaleDef.animalTypeStr, breed);
            animal.setWild(false);
            animal.addToWorld();
            animal.randomizeAge();
            if (Core.getInstance().animalCheat) {
                animal.setCustomName(Translator.getText("IGUI_AnimalType_" + animal.getAnimalType()) + " " + animal.getAnimalID());
            }
            IsoAnimal baby = null;
            if (animal.getData().canHaveBaby() && Rand.Next(100) <= choosenDef.chanceForBaby) {
                baby = animal.addBaby();
                baby.setWild(false);
                if (Core.getInstance().animalCheat) {
                    baby.setCustomName(Translator.getText("IGUI_AnimalType_Baby", baby.mother.getFullName()));
                }
                if (animal.canBeMilked()) {
                    animal.getData().setMilkQuantity(Rand.Next(5.0f, animal.getData().getMaxMilk()));
                }
                baby.randomizeAge();
            }
            if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0 && Rand.NextBool(randValue = Math.max(0, 190 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin()))) {
                animal.setHealth(0.0f);
                if (baby != null) {
                    baby.setHealth(0.0f);
                }
            }
            if (!GameServer.server) continue;
            AnimalInstanceManager.getInstance().add(animal, animal.getOnlineID());
            if (baby == null) continue;
            AnimalInstanceManager.getInstance().add(baby, baby.getOnlineID());
        }
        for (i = 0; i < maleNb; ++i) {
            int randValue;
            if (randomBreed) {
                breed = RandomizedRanchBase.getRandomBreed(maleDef);
            }
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), Rand.Next(zone.x, zone.x + zone.getWidth()), Rand.Next(zone.y, zone.y + zone.getHeight()), zone.z, maleDef.animalTypeStr, breed);
            if (Core.getInstance().animalCheat) {
                animal.setCustomName(Translator.getText("IGUI_AnimalType_" + animal.getAnimalType()) + " " + animal.getAnimalID());
            }
            animal.addToWorld();
            animal.randomizeAge();
            if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0 && Rand.NextBool(randValue = Math.max(0, 250 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin()))) {
                animal.setHealth(0.0f);
            }
            if (!GameServer.server) continue;
            AnimalInstanceManager.getInstance().add(animal, animal.getOnlineID());
        }
        newDZone.setName(Translator.getText("UI_Ranch", Translator.getText("IGUI_AnimalType_Global_" + choosenDef.globalName), Rand.Next(10000)));
    }

    private static RanchZoneDefinitions getDefInPossibleDefList(ArrayList<RanchZoneDefinitions> possibleDefs) {
        int totalChance = 0;
        for (int j = 0; j < possibleDefs.size(); ++j) {
            totalChance += possibleDefs.get((int)j).chance;
        }
        int rand = Rand.Next(totalChance);
        int chanceIndex = 0;
        for (int j = 0; j < possibleDefs.size(); ++j) {
            RanchZoneDefinitions choosenDef = possibleDefs.get(j);
            if (choosenDef.chance + chanceIndex >= rand) {
                return choosenDef;
            }
            chanceIndex += choosenDef.chance;
        }
        return null;
    }

    private static AnimalBreed getRandomBreed(AnimalDefinitions def) {
        return def.getBreeds().get(Rand.Next(0, def.getBreeds().size()));
    }

    public boolean isValid() {
        return true;
    }
}

