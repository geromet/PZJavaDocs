/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ZombiesStageDefinitions {
    public static final ZombiesStageDefinitions instance = new ZombiesStageDefinitions();
    public boolean dirty = true;
    public final ArrayList<ZombiesStageDefinition> stageDefinition = new ArrayList();
    public static int daysEarly = 10;
    public static int daysMid = 30;
    public static int daysLate = 90;

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        ArrayList<String> banditStages = new ArrayList<String>();
        banditStages.add("Bandit_Early");
        banditStages.add("Bandit_Mid");
        banditStages.add("Bandit_Late");
        ZombiesStageDefinition def = new ZombiesStageDefinition("Bandit", banditStages, true);
        this.stageDefinition.add(def);
        ArrayList<String> survivalistStages = new ArrayList<String>();
        survivalistStages.add("Survivalist");
        survivalistStages.add("Survivalist_Mid");
        survivalistStages.add("Survivalist_Late");
        def = new ZombiesStageDefinition("Survivalist", survivalistStages, false);
        this.stageDefinition.add(def);
        survivalistStages = new ArrayList();
        survivalistStages.add("Survivalist02");
        survivalistStages.add("Survivalist02_Mid");
        survivalistStages.add("Survivalist02_Late");
        def = new ZombiesStageDefinition("Survivalist02", survivalistStages, false);
        this.stageDefinition.add(def);
        survivalistStages = new ArrayList();
        survivalistStages.add("Survivalist03");
        survivalistStages.add("Survivalist03_Mid");
        survivalistStages.add("Survivalist03_Late");
        def = new ZombiesStageDefinition("Survivalist03", survivalistStages, false);
        this.stageDefinition.add(def);
        survivalistStages = new ArrayList();
        survivalistStages.add("Survivalist04");
        survivalistStages.add("Survivalist04_Mid");
        survivalistStages.add("Survivalist04_Late");
        def = new ZombiesStageDefinition("Survivalist04", survivalistStages, false);
        this.stageDefinition.add(def);
        survivalistStages = new ArrayList();
        survivalistStages.add("Survivalist05");
        survivalistStages.add("Survivalist05_Mid");
        survivalistStages.add("Survivalist05_Late");
        def = new ZombiesStageDefinition("Survivalist05", survivalistStages, false);
        this.stageDefinition.add(def);
    }

    private static ArrayList<String> initStageList(String stages) {
        if (StringUtils.isNullOrWhitespace(stages)) {
            return null;
        }
        String[] split = stages.split(";");
        return new ArrayList<String>(Arrays.asList(split));
    }

    public String getAdvancedOutfitName(String outfitName) {
        if (StringUtils.isNullOrEmpty(outfitName)) {
            return outfitName;
        }
        instance.checkDirty();
        for (int i = 0; i < ZombiesStageDefinitions.instance.stageDefinition.size(); ++i) {
            ZombiesStageDefinition def = ZombiesStageDefinitions.instance.stageDefinition.get(i);
            if (!Objects.equals(def.outfit, outfitName)) continue;
            String newOutfit = this.getAdvancedOutfitName(def);
            if (newOutfit != null) {
                return newOutfit;
            }
            return outfitName;
        }
        return outfitName;
    }

    public String getAdvancedOutfitName(ZombiesStageDefinition def) {
        int maxStages = def.laterOutfits.size();
        if (maxStages < 1) {
            return def.outfit;
        }
        int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        int possibleStages = 0;
        if (days >= daysLate) {
            possibleStages = 3;
        } else if (days >= daysMid) {
            possibleStages = 2;
        } else if (days >= daysEarly) {
            possibleStages = 1;
        }
        possibleStages = Math.min(possibleStages, maxStages);
        if (possibleStages < 1) {
            return null;
        }
        int roll = possibleStages;
        if (def.mixed) {
            roll = Rand.Next(possibleStages) + 1;
        }
        if (roll == 0) {
            return null;
        }
        return def.laterOutfits.get(roll - 1);
    }

    public static final class ZombiesStageDefinition {
        public String outfit;
        public ArrayList<String> laterOutfits;
        public boolean mixed;

        public ZombiesStageDefinition(String outfit, ArrayList<String> laterOutfits, boolean mixed) {
            DebugLog.Zombie.println("Adding Zombies Stage Definition: " + outfit + " - " + String.valueOf(laterOutfits) + " - Mixed : " + mixed);
            this.outfit = outfit;
            this.laterOutfits = laterOutfits;
            this.mixed = mixed;
        }
    }
}

