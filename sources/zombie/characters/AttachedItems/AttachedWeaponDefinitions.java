/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.AttachedItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.AttachedItems.AttachedWeaponCustomOutfit;
import zombie.characters.AttachedItems.AttachedWeaponDefinition;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoWorld;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;

public final class AttachedWeaponDefinitions {
    public static final AttachedWeaponDefinitions instance = new AttachedWeaponDefinitions();
    public boolean dirty = true;
    public int chanceOfAttachedWeapon;
    public final ArrayList<AttachedWeaponDefinition> definitions = new ArrayList();
    public final ArrayList<AttachedWeaponCustomOutfit> outfitDefinitions = new ArrayList();

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    public void addRandomAttachedWeapon(IsoZombie zed) {
        if ("Tutorial".equals(Core.getInstance().getGameMode())) {
            return;
        }
        this.checkDirty();
        if (this.definitions.isEmpty()) {
            return;
        }
        ArrayList<AttachedWeaponDefinition> definitions = L_addRandomAttachedWeapon.definitions;
        definitions.clear();
        int repeatNbr = 1;
        AttachedWeaponCustomOutfit customOutfit = null;
        Outfit zedOutfit = zed.getHumanVisual().getOutfit();
        if (zedOutfit != null) {
            for (int i = 0; i < this.outfitDefinitions.size(); ++i) {
                customOutfit = this.outfitDefinitions.get(i);
                if (customOutfit.outfit.equals(zedOutfit.name) && OutfitRNG.Next(100) < customOutfit.chance) {
                    definitions.addAll(customOutfit.weapons);
                    repeatNbr = customOutfit.maxitem > -1 ? customOutfit.maxitem : 1;
                    break;
                }
                customOutfit = null;
            }
        }
        if (definitions.isEmpty()) {
            if (OutfitRNG.Next(100) + 1 > this.chanceOfAttachedWeapon) {
                return;
            }
            definitions.addAll(this.definitions);
        }
        while (repeatNbr > 0) {
            AttachedWeaponDefinition toDo = this.pickRandomInList(definitions, zed);
            if (toDo == null) {
                return;
            }
            definitions.remove(toDo);
            --repeatNbr;
            this.addAttachedWeapon(toDo, zed);
            if (customOutfit == null || OutfitRNG.Next(100) < customOutfit.chance) continue;
            return;
        }
    }

    public void alwaysAddARandomAttachedWeapon(IsoZombie zed) {
        if ("Tutorial".equals(Core.getInstance().getGameMode())) {
            return;
        }
        this.checkDirty();
        if (this.definitions.isEmpty()) {
            return;
        }
        ArrayList<AttachedWeaponDefinition> definitions = L_addRandomAttachedWeapon.definitions;
        definitions.clear();
        definitions.addAll(this.definitions);
        AttachedWeaponDefinition toDo = this.pickRandomInList(definitions, zed);
        if (toDo == null) {
            return;
        }
        definitions.remove(toDo);
        this.addAttachedWeapon(toDo, zed);
    }

    private void addAttachedWeapon(AttachedWeaponDefinition toDo, IsoZombie zed) {
        String weaponType = OutfitRNG.pickRandom(toDo.weapons);
        if (SandboxOptions.instance.removeZombieLoot.getValue() && ItemPickerJava.getLootModifier(weaponType) == 0.0f) {
            return;
        }
        Object weapon = InventoryItemFactory.CreateItem(weaponType);
        if (weapon == null) {
            return;
        }
        if (weapon instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)weapon;
            handWeapon.randomizeBullets();
        }
        ((InventoryItem)weapon).setConditionNoSound(OutfitRNG.Next(Math.max(2, ((InventoryItem)weapon).getConditionMax() - 5), ((InventoryItem)weapon).getConditionMax()));
        zed.setAttachedItem(OutfitRNG.pickRandom(toDo.weaponLocation), (InventoryItem)weapon);
        if (toDo.ensureItem != null && !this.outfitHasItem(zed, toDo.ensureItem)) {
            Item scriptItem = ScriptManager.instance.FindItem(toDo.ensureItem);
            if (scriptItem != null && scriptItem.getClothingItemAsset() != null) {
                zed.getHumanVisual().addClothingItem(zed.getItemVisuals(), scriptItem);
            } else {
                zed.addItemToSpawnAtDeath((InventoryItem)InventoryItemFactory.CreateItem(toDo.ensureItem));
            }
        }
        if (!toDo.bloodLocations.isEmpty()) {
            float bloodLevel = Math.max((float)Rand.Next(100) / 100.0f, (float)Rand.Next(100) / 100.0f);
            bloodLevel = Math.max(bloodLevel, (float)Rand.Next(100) / 100.0f);
            ((InventoryItem)weapon).setBloodLevel(bloodLevel);
            for (int i = 0; i < toDo.bloodLocations.size(); ++i) {
                BloodBodyPartType part = toDo.bloodLocations.get(i);
                zed.addBlood(part, true, true, true);
                zed.addBlood(part, true, true, true);
                zed.addBlood(part, true, true, true);
                if (!toDo.addHoles) continue;
                zed.addHole(part, true);
            }
        }
    }

    private AttachedWeaponDefinition pickRandomInList(ArrayList<AttachedWeaponDefinition> definitions, IsoZombie zed) {
        AttachedWeaponDefinition toDo = null;
        int totalChance = 0;
        ArrayList<AttachedWeaponDefinition> possibilities = L_addRandomAttachedWeapon.possibilities;
        possibilities.clear();
        for (int i = 0; i < definitions.size(); ++i) {
            AttachedWeaponDefinition value = definitions.get(i);
            if (value.daySurvived > 0) {
                if (!(IsoWorld.instance.getWorldAgeDays() > (float)value.daySurvived)) continue;
                totalChance += value.chance;
                possibilities.add(value);
                continue;
            }
            if (!value.outfit.isEmpty()) {
                if (zed.getHumanVisual().getOutfit() == null || !value.outfit.contains(zed.getHumanVisual().getOutfit().name)) continue;
                totalChance += value.chance;
                possibilities.add(value);
                continue;
            }
            totalChance += value.chance;
            possibilities.add(value);
        }
        int choice = OutfitRNG.Next(totalChance);
        int subtotal = 0;
        for (int i = 0; i < possibilities.size(); ++i) {
            AttachedWeaponDefinition testTable = possibilities.get(i);
            if (choice >= (subtotal += testTable.chance)) continue;
            toDo = testTable;
            break;
        }
        return toDo;
    }

    public boolean outfitHasItem(IsoZombie zombie, String ensureItem) {
        assert (ensureItem.contains("."));
        ItemVisuals itemVisuals = zombie.getItemVisuals();
        for (int i = 0; i < itemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
            if (StringUtils.equals(itemVisual.getItemType(), ensureItem)) {
                return true;
            }
            if ("Base.HolsterSimple".equals(ensureItem) && StringUtils.equals(itemVisual.getItemType(), "Base.HolsterDouble")) {
                return true;
            }
            if (!"Base.HolsterDouble".equals(ensureItem) || !StringUtils.equals(itemVisual.getItemType(), "Base.HolsterSimple")) continue;
            return true;
        }
        return false;
    }

    private void init() {
        this.definitions.clear();
        this.outfitDefinitions.clear();
        KahluaTableImpl definition = (KahluaTableImpl)LuaManager.env.rawget("AttachedWeaponDefinitions");
        if (definition == null) {
            return;
        }
        this.chanceOfAttachedWeapon = SandboxOptions.instance.lore.chanceOfAttachedWeapon.getValue();
        for (Map.Entry<Object, Object> objectObjectEntry : definition.delegate.entrySet()) {
            Iterator<Map.Entry<Object, Object>> iterator2 = objectObjectEntry.getValue();
            if (!(iterator2 instanceof KahluaTableImpl)) continue;
            KahluaTableImpl outfitdefinition = (KahluaTableImpl)((Object)iterator2);
            if ("attachedWeaponCustomOutfit".equals(objectObjectEntry.getKey())) {
                for (Map.Entry<Object, Object> customOutfit : outfitdefinition.delegate.entrySet()) {
                    AttachedWeaponCustomOutfit def = this.initOutfit((String)customOutfit.getKey(), (KahluaTableImpl)customOutfit.getValue());
                    if (def == null) continue;
                    this.outfitDefinitions.add(def);
                }
                continue;
            }
            AttachedWeaponDefinition def = this.init((String)objectObjectEntry.getKey(), outfitdefinition);
            if (def == null) continue;
            this.definitions.add(def);
        }
        Collections.sort(this.definitions, Comparator.comparing(o -> o.id));
    }

    private AttachedWeaponCustomOutfit initOutfit(String key, KahluaTableImpl value) {
        AttachedWeaponCustomOutfit def = new AttachedWeaponCustomOutfit();
        def.outfit = key;
        def.chance = value.rawgetInt("chance");
        def.maxitem = value.rawgetInt("maxitem");
        KahluaTableImpl weaponsDef = (KahluaTableImpl)value.rawget("weapons");
        for (Map.Entry<Object, Object> weapon : weaponsDef.delegate.entrySet()) {
            KahluaTableImpl wpnValue = (KahluaTableImpl)weapon.getValue();
            AttachedWeaponDefinition weaponDef = this.init(wpnValue.rawgetStr("id"), wpnValue);
            if (weaponDef == null) continue;
            def.weapons.add(weaponDef);
        }
        return def;
    }

    private AttachedWeaponDefinition init(String key, KahluaTableImpl value) {
        AttachedWeaponDefinition def = new AttachedWeaponDefinition();
        def.id = key;
        def.chance = value.rawgetInt("chance");
        this.tableToArrayList(value, "outfit", def.outfit);
        this.tableToArrayList(value, "weaponLocation", def.weaponLocation);
        KahluaTableImpl bloodPossibilitiesTable = (KahluaTableImpl)value.rawget("bloodLocations");
        if (bloodPossibilitiesTable != null) {
            KahluaTableIterator iterator2 = bloodPossibilitiesTable.iterator();
            while (iterator2.advance()) {
                BloodBodyPartType part = BloodBodyPartType.FromString(iterator2.getValue().toString());
                if (part == BloodBodyPartType.MAX) continue;
                def.bloodLocations.add(part);
            }
        }
        def.addHoles = value.rawgetBool("addHoles");
        def.daySurvived = value.rawgetInt("daySurvived");
        def.ensureItem = value.rawgetStr("ensureItem");
        this.tableToArrayList(value, "weapons", def.weapons);
        Collections.sort(def.weaponLocation);
        Collections.sort(def.bloodLocations);
        Collections.sort(def.weapons);
        return def;
    }

    private void tableToArrayList(KahluaTable table, String key, ArrayList<String> list) {
        KahluaTableImpl table2 = (KahluaTableImpl)table.rawget(key);
        if (table2 == null) {
            return;
        }
        int len = table2.len();
        for (int i = 1; i <= len; ++i) {
            Object o = table2.rawget(i);
            if (o == null) continue;
            list.add(o.toString());
        }
    }

    private static final class L_addRandomAttachedWeapon {
        static final ArrayList<AttachedWeaponDefinition> possibilities = new ArrayList();
        static final ArrayList<AttachedWeaponDefinition> definitions = new ArrayList();

        private L_addRandomAttachedWeapon() {
        }
    }
}

