/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.Arrays;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.SurvivorDesc;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.population.BeardStyle;
import zombie.core.skinnedmodel.population.HairStyle;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

public final class HairOutfitDefinitions {
    public static final HairOutfitDefinitions instance = new HairOutfitDefinitions();
    public boolean dirty = true;
    public String hairStyle;
    public int minWorldAge;
    public final ArrayList<HaircutDefinition> haircutDefinition = new ArrayList();
    public final ArrayList<HaircutOutfitDefinition> outfitDefinition = new ArrayList();
    private final ThreadLocal<ArrayList<HairStyle>> tempHairStyles = ThreadLocal.withInitial(ArrayList::new);

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        Object def;
        this.haircutDefinition.clear();
        this.outfitDefinition.clear();
        KahluaTableImpl definition = (KahluaTableImpl)LuaManager.env.rawget("HairOutfitDefinitions");
        if (definition == null) {
            return;
        }
        Object object = definition.rawget("haircutDefinition");
        if (!(object instanceof KahluaTableImpl)) {
            return;
        }
        KahluaTableImpl hairForWorldAgeDefinition = (KahluaTableImpl)object;
        KahluaTableIterator iterator2 = hairForWorldAgeDefinition.iterator();
        while (iterator2.advance()) {
            Object object2 = iterator2.getValue();
            if (!(object2 instanceof KahluaTableImpl)) continue;
            KahluaTableImpl hairForWorldAge = (KahluaTableImpl)object2;
            def = new HaircutDefinition(hairForWorldAge.rawgetStr("name"), hairForWorldAge.rawgetInt("minWorldAge"), new ArrayList<String>(Arrays.asList(hairForWorldAge.rawgetStr("onlyFor").split(","))));
            this.haircutDefinition.add((HaircutDefinition)def);
        }
        def = definition.rawget("haircutOutfitDefinition");
        if (!(def instanceof KahluaTableImpl)) {
            return;
        }
        KahluaTableImpl hairForOutfitDefinition = (KahluaTableImpl)def;
        iterator2 = hairForOutfitDefinition.iterator();
        while (iterator2.advance()) {
            Object object3 = iterator2.getValue();
            if (!(object3 instanceof KahluaTableImpl)) continue;
            KahluaTableImpl hairForOutfit = (KahluaTableImpl)object3;
            HaircutOutfitDefinition def2 = new HaircutOutfitDefinition(hairForOutfit.rawgetStr("outfit"), HairOutfitDefinitions.initStringChance(hairForOutfit.rawgetStr("haircut")), HairOutfitDefinitions.initStringChance(hairForOutfit.rawgetStr("femaleHaircut")), HairOutfitDefinitions.initStringChance(hairForOutfit.rawgetStr("maleHaircut")), HairOutfitDefinitions.initStringChance(hairForOutfit.rawgetStr("beard")), HairOutfitDefinitions.initStringChance(hairForOutfit.rawgetStr("haircutColor")));
            this.outfitDefinition.add(def2);
        }
    }

    public boolean isHaircutValid(String outfit, String haircut) {
        instance.checkDirty();
        if (StringUtils.isNullOrEmpty(outfit)) {
            return true;
        }
        for (int i = 0; i < HairOutfitDefinitions.instance.haircutDefinition.size(); ++i) {
            HaircutDefinition def = HairOutfitDefinitions.instance.haircutDefinition.get(i);
            if (!def.hairStyle.equals(haircut)) continue;
            if (!def.onlyFor.contains(outfit)) {
                return false;
            }
            if (!(IsoWorld.instance.getWorldAgeDays() < (float)def.minWorldAge)) continue;
            return false;
        }
        return true;
    }

    public void getValidHairStylesForOutfit(String outfit, ArrayList<HairStyle> hairStyles, ArrayList<HairStyle> result) {
        result.clear();
        for (int i = 0; i < hairStyles.size(); ++i) {
            HairStyle hairStyle = hairStyles.get(i);
            if (hairStyle.isNoChoose() || !this.isHaircutValid(outfit, hairStyle.name)) continue;
            result.add(hairStyle);
        }
    }

    public String getRandomHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        }
        String haircut = OutfitRNG.pickRandom(validStyles).name;
        boolean done = false;
        for (int i = 0; i < HairOutfitDefinitions.instance.outfitDefinition.size(); ++i) {
            HaircutOutfitDefinition outfitDef = HairOutfitDefinitions.instance.outfitDefinition.get(i);
            if (!outfitDef.outfit.equals(outfit) || outfitDef.haircutChance == null) continue;
            return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
        }
        return haircut;
    }

    public String getRandomHaircutFromOutfitDef(HaircutOutfitDefinition outfitDef, String haircut, ArrayList<HairStyle> validStyles) {
        float choice = OutfitRNG.Next(0.0f, 100.0f);
        float subtotal = 0.0f;
        for (int j = 0; j < outfitDef.haircutChance.size(); ++j) {
            StringChance stringChance = outfitDef.haircutChance.get(j);
            if (!(choice < (subtotal += stringChance.chance))) continue;
            haircut = stringChance.str;
            if ("null".equalsIgnoreCase(stringChance.str)) {
                haircut = "";
            }
            if ("random".equalsIgnoreCase(stringChance.str)) {
                haircut = OutfitRNG.pickRandom(validStyles).name;
            }
            return haircut;
        }
        return haircut;
    }

    public String getRandomFemaleHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        }
        String haircut = OutfitRNG.pickRandom(validStyles).name;
        boolean done = false;
        block0: for (int i = 0; i < HairOutfitDefinitions.instance.outfitDefinition.size() && !done; ++i) {
            HaircutOutfitDefinition outfitDef = HairOutfitDefinitions.instance.outfitDefinition.get(i);
            if (outfitDef.outfit.equals(outfit) && outfitDef.femaleHaircutChance != null) {
                float choice = OutfitRNG.Next(0.0f, 100.0f);
                float subtotal = 0.0f;
                for (int j = 0; j < outfitDef.femaleHaircutChance.size(); ++j) {
                    StringChance stringChance = outfitDef.femaleHaircutChance.get(j);
                    if (!(choice < (subtotal += stringChance.chance))) continue;
                    haircut = stringChance.str;
                    if ("null".equalsIgnoreCase(stringChance.str)) {
                        haircut = "";
                    }
                    if ("random".equalsIgnoreCase(stringChance.str)) {
                        haircut = OutfitRNG.pickRandom(validStyles).name;
                    }
                    done = true;
                    continue block0;
                }
                continue;
            }
            if (!outfitDef.outfit.equals(outfit) || outfitDef.femaleHaircutChance != null || outfitDef.haircutChance == null) continue;
            return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
        }
        return haircut;
    }

    public String getRandomMaleHaircut(String outfit, ArrayList<HairStyle> hairList) {
        ArrayList<HairStyle> validStyles = this.tempHairStyles.get();
        this.getValidHairStylesForOutfit(outfit, hairList, validStyles);
        if (validStyles.isEmpty()) {
            return "";
        }
        String haircut = OutfitRNG.pickRandom(validStyles).name;
        boolean done = false;
        block0: for (int i = 0; i < HairOutfitDefinitions.instance.outfitDefinition.size() && !done; ++i) {
            HaircutOutfitDefinition outfitDef = HairOutfitDefinitions.instance.outfitDefinition.get(i);
            if (outfitDef.outfit.equals(outfit) && outfitDef.maleHaircutChance != null) {
                float choice = OutfitRNG.Next(0.0f, 100.0f);
                float subtotal = 0.0f;
                for (int j = 0; j < outfitDef.maleHaircutChance.size(); ++j) {
                    StringChance stringChance = outfitDef.maleHaircutChance.get(j);
                    if (!(choice < (subtotal += stringChance.chance))) continue;
                    haircut = stringChance.str;
                    if ("null".equalsIgnoreCase(stringChance.str)) {
                        haircut = "";
                    }
                    if ("random".equalsIgnoreCase(stringChance.str)) {
                        haircut = OutfitRNG.pickRandom(validStyles).name;
                    }
                    done = true;
                    continue block0;
                }
                continue;
            }
            if (!outfitDef.outfit.equals(outfit) || outfitDef.maleHaircutChance != null || outfitDef.haircutChance == null) continue;
            return this.getRandomHaircutFromOutfitDef(outfitDef, haircut, validStyles);
        }
        return haircut;
    }

    public ImmutableColor getRandomHaircutColor(String outfit) {
        ImmutableColor result = SurvivorDesc.HairCommonColors.get(OutfitRNG.Next(SurvivorDesc.HairCommonColors.size()));
        String strColor = null;
        boolean done = false;
        block0: for (int i = 0; i < HairOutfitDefinitions.instance.outfitDefinition.size() && !done; ++i) {
            HaircutOutfitDefinition outfitDef = HairOutfitDefinitions.instance.outfitDefinition.get(i);
            if (!outfitDef.outfit.equals(outfit) || outfitDef.haircutColor == null) continue;
            float choice = OutfitRNG.Next(0.0f, 100.0f);
            float subtotal = 0.0f;
            for (int j = 0; j < outfitDef.haircutColor.size(); ++j) {
                StringChance stringChance = outfitDef.haircutColor.get(j);
                if (!(choice < (subtotal += stringChance.chance))) continue;
                strColor = stringChance.str;
                if ("random".equalsIgnoreCase(stringChance.str)) {
                    result = SurvivorDesc.HairCommonColors.get(OutfitRNG.Next(SurvivorDesc.HairCommonColors.size()));
                    strColor = null;
                }
                done = true;
                continue block0;
            }
        }
        if (!StringUtils.isNullOrEmpty(strColor)) {
            String[] colorTable = strColor.split(",");
            result = new ImmutableColor(Float.parseFloat(colorTable[0]), Float.parseFloat(colorTable[1]), Float.parseFloat(colorTable[2]));
        }
        return result;
    }

    public String getRandomBeard(String outfit, ArrayList<BeardStyle> beardList) {
        String beard = OutfitRNG.pickRandom(beardList).name;
        boolean done = false;
        block0: for (int i = 0; i < HairOutfitDefinitions.instance.outfitDefinition.size() && !done; ++i) {
            HaircutOutfitDefinition outfitDef = HairOutfitDefinitions.instance.outfitDefinition.get(i);
            if (!outfitDef.outfit.equals(outfit) || outfitDef.beardChance == null) continue;
            float choice = OutfitRNG.Next(0.0f, 100.0f);
            float subtotal = 0.0f;
            for (int j = 0; j < outfitDef.beardChance.size(); ++j) {
                StringChance stringChance = outfitDef.beardChance.get(j);
                if (!(choice < (subtotal += stringChance.chance))) continue;
                beard = stringChance.str;
                if ("null".equalsIgnoreCase(stringChance.str)) {
                    beard = "";
                }
                if ("random".equalsIgnoreCase(stringChance.str)) {
                    beard = OutfitRNG.pickRandom(beardList).name;
                }
                done = true;
                continue block0;
            }
        }
        return beard;
    }

    private static ArrayList<StringChance> initStringChance(String styles) {
        if (StringUtils.isNullOrWhitespace(styles)) {
            return null;
        }
        ArrayList<StringChance> result = new ArrayList<StringChance>();
        String[] split = styles.split(";");
        int totalChance = 0;
        for (String style : split) {
            String[] splitStyle = style.split(":");
            StringChance stringChance = new StringChance();
            stringChance.str = splitStyle[0];
            stringChance.chance = Float.parseFloat(splitStyle[1]);
            totalChance = (int)((float)totalChance + stringChance.chance);
            result.add(stringChance);
        }
        if (totalChance < 100) {
            StringChance stringChance = new StringChance();
            stringChance.str = "random";
            stringChance.chance = 100 - totalChance;
            result.add(stringChance);
        }
        return result;
    }

    public static final class HaircutDefinition {
        public String hairStyle;
        public int minWorldAge;
        public ArrayList<String> onlyFor;

        public HaircutDefinition(String hairStyle, int minWorldAge, ArrayList<String> onlyFor) {
            this.hairStyle = hairStyle;
            this.minWorldAge = minWorldAge;
            this.onlyFor = onlyFor;
        }
    }

    public static final class HaircutOutfitDefinition {
        public String outfit;
        public ArrayList<StringChance> haircutChance;
        public ArrayList<StringChance> femaleHaircutChance;
        public ArrayList<StringChance> maleHaircutChance;
        public ArrayList<StringChance> beardChance;
        public ArrayList<StringChance> haircutColor;

        public HaircutOutfitDefinition(String outfit, ArrayList<StringChance> haircutChance, ArrayList<StringChance> femaleHaircutChance, ArrayList<StringChance> maleHaircutChance, ArrayList<StringChance> beardChance, ArrayList<StringChance> haircutColor) {
            this.outfit = outfit;
            this.haircutChance = haircutChance;
            this.femaleHaircutChance = femaleHaircutChance;
            this.maleHaircutChance = maleHaircutChance;
            this.beardChance = beardChance;
            this.haircutColor = haircutColor;
        }
    }

    private static final class StringChance {
        String str;
        float chance;

        private StringChance() {
        }
    }
}

