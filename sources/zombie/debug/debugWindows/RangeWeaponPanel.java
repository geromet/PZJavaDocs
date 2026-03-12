/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.debug.debugWindows.PZDebugWindow;
import zombie.debug.debugWindows.Wrappers;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;

public class RangeWeaponPanel
extends PZDebugWindow {
    private final RangeWeaponsConfigOptions rangeWeaponsConfigOptions = new RangeWeaponsConfigOptions(this);
    private final ArrayList<Item> allRangeWeaponScriptItems = new ArrayList();
    private boolean allowSavingDefaults;

    @Override
    public String getTitle() {
        return "Range Weapon Editor";
    }

    @Override
    protected void doWindowContents() {
        this.cacheAllRangeWeaponScriptItems();
        boolean showWeaponTab = IsoPlayer.players[0] != null && !this.allRangeWeaponScriptItems.isEmpty();
        ImGui.beginChild("Begin");
        this.allowSavingDefaults = Wrappers.checkbox("Allow Saving Defaults", this.allowSavingDefaults);
        if (this.allowSavingDefaults) {
            ImGui.sameLine();
            if (ImGui.button("Save All Defaults")) {
                for (Item item : this.allRangeWeaponScriptItems) {
                    if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON)) continue;
                    this.rangeWeaponsConfigOptions.saveDefaults(item, item.getName());
                }
                this.allowSavingDefaults = false;
            }
        }
        if (ImGui.button("Load All Defaults")) {
            for (Item item : this.allRangeWeaponScriptItems) {
                if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON)) continue;
                this.rangeWeaponsConfigOptions.loadDefaults(item, item.getName());
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Save All Custom Data")) {
            for (Item item : this.allRangeWeaponScriptItems) {
                if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON)) continue;
                this.rangeWeaponsConfigOptions.save(item, item.getName());
            }
        }
        ImGui.sameLine();
        if (ImGui.button("Load All Custom Data")) {
            for (Item item : this.allRangeWeaponScriptItems) {
                if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON)) continue;
                this.rangeWeaponsConfigOptions.load(item, item.getName());
            }
        }
        if (showWeaponTab) {
            if (ImGui.beginTabBar("tabSelector")) {
                this.weaponTab();
            }
            ImGui.endTabBar();
        }
        ImGui.endChild();
    }

    private void cacheAllRangeWeaponScriptItems() {
        if (!this.allRangeWeaponScriptItems.isEmpty()) {
            return;
        }
        ArrayList<Item> allScriptItems = ScriptManager.instance.getAllItems();
        if (allScriptItems != null) {
            for (Item item : allScriptItems) {
                if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON)) continue;
                this.allRangeWeaponScriptItems.add(item);
            }
        }
    }

    private void weaponTab() {
        for (Item item : this.allRangeWeaponScriptItems) {
            String itemName;
            boolean tabSelected = false;
            if (!item.isRanged() || item.hasTag(ItemTag.FAKE_WEAPON) || !ImGui.beginTabItem(itemName = item.getName(), 0)) continue;
            if (this.editItem(item)) {
                this.updateInventoryItems(item);
            }
            if (ImGui.button("Load " + itemName + " Defaults") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                this.rangeWeaponsConfigOptions.loadDefaults(item, itemName);
            }
            ImGui.sameLine();
            if (ImGui.button("Save " + itemName + " Custom Data") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                this.rangeWeaponsConfigOptions.save(item, itemName);
            }
            ImGui.sameLine();
            if (ImGui.button("Load " + itemName + " Custom Data") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                this.rangeWeaponsConfigOptions.load(item, itemName);
            }
            ImGui.endTabItem();
        }
    }

    private void updateInventoryItems(Item item) {
        if (IsoPlayer.players[0] == null) {
            return;
        }
        ItemContainer itemContainer = IsoPlayer.players[0].getInventory();
        for (InventoryItem inventoryItem : itemContainer.items) {
            Item scriptItem = inventoryItem.getScriptItem();
            if (item != scriptItem) continue;
            inventoryItem.setName("ScriptItem");
            if (!(inventoryItem instanceof HandWeapon)) continue;
            HandWeapon handWeapon = (HandWeapon)inventoryItem;
            handWeapon.setConditionLowerChance(scriptItem.conditionLowerChance);
            handWeapon.setMinDamage(scriptItem.minDamage);
            handWeapon.setMaxDamage(scriptItem.maxDamage);
            handWeapon.setMinRange(scriptItem.minRange);
            handWeapon.setMinSightRange(scriptItem.minSightRange);
            handWeapon.setMaxSightRange(scriptItem.maxSightRange);
            handWeapon.setMaxAmmo(scriptItem.maxAmmo);
            handWeapon.setDoorDamage(scriptItem.doorDamage);
            handWeapon.setSoundRadius(scriptItem.soundRadius);
            handWeapon.setToHitModifier(scriptItem.toHitModifier);
            handWeapon.setCriticalChance(scriptItem.criticalChance);
            handWeapon.setCriticalDamageMultiplier(scriptItem.critDmgMultiplier);
            handWeapon.setAimingPerkCritModifier(scriptItem.aimingPerkCritModifier);
            handWeapon.setAimingPerkRangeModifier(scriptItem.aimingPerkRangeModifier);
            handWeapon.setAimingPerkHitChanceModifier(scriptItem.aimingPerkHitChanceModifier);
            handWeapon.setHitChance(scriptItem.hitChance);
            handWeapon.setRecoilDelay(scriptItem.recoilDelay);
            handWeapon.setReloadTime(scriptItem.reloadTime);
            handWeapon.setAimingTime(scriptItem.aimingTime);
            handWeapon.setJamGunChance(scriptItem.jamGunChance);
            handWeapon.setCyclicRateMultiplier(scriptItem.cyclicRateMultiplier);
            handWeapon.setProjectileCount(scriptItem.projectileCount);
            handWeapon.setProjectileSpread(scriptItem.projectileSpread);
            handWeapon.setProjectileWeightCenter(scriptItem.projectileWeightCenter);
        }
    }

    private boolean editItem(Item item) {
        Wrappers.clearValueChanged();
        item.conditionLowerChance = Wrappers.sliderIntShowRange(RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getId(), item.conditionLowerChance, (int)RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMin(), (int)RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMax());
        item.minDamage = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MIN_DAMAGE.getId(), item.minDamage, (float)RangeWeaponAttribute.MIN_DAMAGE.getMin(), item.maxDamage);
        item.maxDamage = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MAX_DAMAGE.getId(), item.maxDamage, item.minDamage, (float)RangeWeaponAttribute.MAX_DAMAGE.getMax());
        item.minRange = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MIN_RANGE.getId(), item.minRange, (float)RangeWeaponAttribute.MIN_RANGE.getMin(), item.maxRange);
        item.maxRange = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MAX_RANGE.getId(), item.maxRange, item.minRange, (float)RangeWeaponAttribute.MAX_RANGE.getMax());
        item.minSightRange = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MIN_SIGHT_RANGE.getId(), item.minSightRange, (float)RangeWeaponAttribute.MIN_SIGHT_RANGE.getMin(), item.maxSightRange);
        item.maxSightRange = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.MAX_SIGHT_RANGE.getId(), item.maxSightRange, item.minSightRange, (float)RangeWeaponAttribute.MAX_SIGHT_RANGE.getMax());
        item.maxHitCount = Wrappers.sliderIntShowRange(RangeWeaponAttribute.MAX_HIT_COUNT.getId(), item.maxHitCount, (int)RangeWeaponAttribute.MAX_HIT_COUNT.getMin(), (int)RangeWeaponAttribute.MAX_HIT_COUNT.getMax());
        item.maxAmmo = Wrappers.sliderIntShowRange(RangeWeaponAttribute.MAX_AMMO.getId(), item.maxAmmo, (int)RangeWeaponAttribute.MAX_AMMO.getMin(), (int)RangeWeaponAttribute.MAX_AMMO.getMax());
        item.doorDamage = Wrappers.sliderIntShowRange(RangeWeaponAttribute.DOOR_DAMAGE.getId(), item.doorDamage, (int)RangeWeaponAttribute.DOOR_DAMAGE.getMin(), (int)RangeWeaponAttribute.DOOR_DAMAGE.getMax());
        item.soundRadius = Wrappers.sliderIntShowRange(RangeWeaponAttribute.SOUND_RADIUS.getId(), item.soundRadius, (int)RangeWeaponAttribute.SOUND_RADIUS.getMin(), (int)RangeWeaponAttribute.SOUND_RADIUS.getMax());
        item.toHitModifier = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.TO_HIT_MODIFIER.getId(), item.toHitModifier, (float)RangeWeaponAttribute.TO_HIT_MODIFIER.getMin(), (float)RangeWeaponAttribute.TO_HIT_MODIFIER.getMax());
        item.criticalChance = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.CRITICAL_CHANCE.getId(), item.criticalChance, (float)RangeWeaponAttribute.CRITICAL_CHANCE.getMin(), (float)RangeWeaponAttribute.CRITICAL_CHANCE.getMax());
        item.critDmgMultiplier = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getId(), item.critDmgMultiplier, (float)RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMin(), (float)RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMax());
        item.aimingPerkCritModifier = Wrappers.sliderIntShowRange(RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getId(), item.aimingPerkCritModifier, (int)RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMin(), (int)RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMax());
        item.aimingPerkRangeModifier = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getId(), item.aimingPerkRangeModifier, (float)RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMin(), (float)RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMax());
        item.aimingPerkHitChanceModifier = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getId(), item.aimingPerkHitChanceModifier, (float)RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMin(), (float)RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMax());
        item.hitChance = Wrappers.sliderIntShowRange(RangeWeaponAttribute.HIT_CHANCE.getId(), item.hitChance, (int)RangeWeaponAttribute.HIT_CHANCE.getMin(), (int)RangeWeaponAttribute.HIT_CHANCE.getMax());
        item.recoilDelay = Wrappers.sliderIntShowRange(RangeWeaponAttribute.RECOIL_DELAY.getId(), item.recoilDelay, (int)RangeWeaponAttribute.RECOIL_DELAY.getMin(), (int)RangeWeaponAttribute.RECOIL_DELAY.getMax());
        item.reloadTime = Wrappers.sliderIntShowRange(RangeWeaponAttribute.RELOAD_TIME.getId(), item.reloadTime, (int)RangeWeaponAttribute.RELOAD_TIME.getMin(), (int)RangeWeaponAttribute.RELOAD_TIME.getMax());
        item.aimingTime = Wrappers.sliderIntShowRange(RangeWeaponAttribute.AIMING_TIME.getId(), item.aimingTime, (int)RangeWeaponAttribute.AIMING_TIME.getMin(), (int)RangeWeaponAttribute.AIMING_TIME.getMax());
        item.jamGunChance = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.JAM_GUN_CHANCE.getId(), item.jamGunChance, (float)RangeWeaponAttribute.JAM_GUN_CHANCE.getMin(), (float)RangeWeaponAttribute.JAM_GUN_CHANCE.getMax());
        item.cyclicRateMultiplier = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getId(), item.cyclicRateMultiplier, (float)RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMin(), (float)RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMax());
        item.projectileCount = Wrappers.sliderIntShowRange(RangeWeaponAttribute.PROJECTILE_COUNT.getId(), item.projectileCount, (int)RangeWeaponAttribute.PROJECTILE_COUNT.getMin(), (int)RangeWeaponAttribute.PROJECTILE_COUNT.getMax());
        item.projectileSpread = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.PROJECTILE_SPREAD.getId(), item.projectileSpread, (float)RangeWeaponAttribute.PROJECTILE_SPREAD.getMin(), (float)RangeWeaponAttribute.PROJECTILE_SPREAD.getMax());
        item.projectileWeightCenter = Wrappers.sliderFloatShowRange(RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getId(), item.projectileWeightCenter, (float)RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMin(), (float)RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMax());
        return Wrappers.didValuesChange();
    }

    public class RangeWeaponsConfigOptions {
        private static final int VERSION = 1;
        private final ArrayList<RangeWeaponsConfigOption> options;
        final /* synthetic */ RangeWeaponPanel this$0;

        RangeWeaponsConfigOptions(RangeWeaponPanel this$0) {
            RangeWeaponPanel rangeWeaponPanel = this$0;
            Objects.requireNonNull(rangeWeaponPanel);
            this.this$0 = rangeWeaponPanel;
            this.options = new ArrayList();
            new RangeWeaponsConfigOption(RangeWeaponAttribute.CONDITION_LOWER_CHANCE, RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMin(), RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMax(), RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MIN_DAMAGE, RangeWeaponAttribute.MIN_DAMAGE.getMin(), RangeWeaponAttribute.MIN_DAMAGE.getMax(), RangeWeaponAttribute.MIN_DAMAGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MAX_DAMAGE, RangeWeaponAttribute.MAX_DAMAGE.getMin(), RangeWeaponAttribute.MAX_DAMAGE.getMax(), RangeWeaponAttribute.MAX_DAMAGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MIN_RANGE, RangeWeaponAttribute.MIN_RANGE.getMin(), RangeWeaponAttribute.MIN_RANGE.getMax(), RangeWeaponAttribute.MIN_RANGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MAX_RANGE, RangeWeaponAttribute.MAX_RANGE.getMin(), RangeWeaponAttribute.MAX_RANGE.getMax(), RangeWeaponAttribute.MAX_RANGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MIN_SIGHT_RANGE, RangeWeaponAttribute.MIN_SIGHT_RANGE.getMin(), RangeWeaponAttribute.MIN_SIGHT_RANGE.getMax(), RangeWeaponAttribute.MIN_SIGHT_RANGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MAX_SIGHT_RANGE, RangeWeaponAttribute.MAX_SIGHT_RANGE.getMin(), RangeWeaponAttribute.MAX_SIGHT_RANGE.getMax(), RangeWeaponAttribute.MAX_SIGHT_RANGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MAX_HIT_COUNT, RangeWeaponAttribute.MAX_HIT_COUNT.getMin(), RangeWeaponAttribute.MAX_HIT_COUNT.getMax(), RangeWeaponAttribute.MAX_HIT_COUNT.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.MAX_AMMO, RangeWeaponAttribute.MAX_AMMO.getMin(), RangeWeaponAttribute.MAX_AMMO.getMax(), RangeWeaponAttribute.MAX_AMMO.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.DOOR_DAMAGE, RangeWeaponAttribute.DOOR_DAMAGE.getMin(), RangeWeaponAttribute.DOOR_DAMAGE.getMax(), RangeWeaponAttribute.DOOR_DAMAGE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.SOUND_RADIUS, RangeWeaponAttribute.SOUND_RADIUS.getMin(), RangeWeaponAttribute.SOUND_RADIUS.getMax(), RangeWeaponAttribute.SOUND_RADIUS.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.TO_HIT_MODIFIER, RangeWeaponAttribute.TO_HIT_MODIFIER.getMin(), RangeWeaponAttribute.TO_HIT_MODIFIER.getMax(), RangeWeaponAttribute.TO_HIT_MODIFIER.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.CRITICAL_CHANCE, RangeWeaponAttribute.CRITICAL_CHANCE.getMin(), RangeWeaponAttribute.CRITICAL_CHANCE.getMax(), RangeWeaponAttribute.CRITICAL_CHANCE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.CRIT_DMG_MULTIPLIER, RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMin(), RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMax(), RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.AIMING_PERK_CRIT_MOD, RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMin(), RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMax(), RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.AIMING_PERK_RANGE_MOD, RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMin(), RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMax(), RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD, RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMin(), RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMax(), RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.HIT_CHANCE, RangeWeaponAttribute.HIT_CHANCE.getMin(), RangeWeaponAttribute.HIT_CHANCE.getMax(), RangeWeaponAttribute.HIT_CHANCE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.RECOIL_DELAY, RangeWeaponAttribute.RECOIL_DELAY.getMin(), RangeWeaponAttribute.RECOIL_DELAY.getMax(), RangeWeaponAttribute.RECOIL_DELAY.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.RELOAD_TIME, RangeWeaponAttribute.RELOAD_TIME.getMin(), RangeWeaponAttribute.RELOAD_TIME.getMax(), RangeWeaponAttribute.RELOAD_TIME.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.AIMING_TIME, RangeWeaponAttribute.AIMING_TIME.getMin(), RangeWeaponAttribute.AIMING_TIME.getMax(), RangeWeaponAttribute.AIMING_TIME.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.JAM_GUN_CHANCE, RangeWeaponAttribute.JAM_GUN_CHANCE.getMin(), RangeWeaponAttribute.JAM_GUN_CHANCE.getMax(), RangeWeaponAttribute.JAM_GUN_CHANCE.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER, RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMin(), RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMax(), RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.PROJECTILE_COUNT, RangeWeaponAttribute.PROJECTILE_COUNT.getMin(), RangeWeaponAttribute.PROJECTILE_COUNT.getMax(), RangeWeaponAttribute.PROJECTILE_COUNT.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.PROJECTILE_SPREAD, RangeWeaponAttribute.PROJECTILE_SPREAD.getMin(), RangeWeaponAttribute.PROJECTILE_SPREAD.getMax(), RangeWeaponAttribute.PROJECTILE_SPREAD.getDefaultValue(), this.options);
            new RangeWeaponsConfigOption(RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER, RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMin(), RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMax(), RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getDefaultValue(), this.options);
        }

        private int getOptionCount() {
            return this.options.size();
        }

        private ConfigOption getOptionByIndex(int index) {
            return this.options.get(index);
        }

        private ConfigOption getOptionByName(String name) {
            for (int i = 0; i < this.options.size(); ++i) {
                ConfigOption setting = this.options.get(i);
                if (!setting.getName().equals(name)) continue;
                return setting;
            }
            return null;
        }

        private void setOptionValue(RangeWeaponAttribute rangeWeaponAttribute, double value) {
            RangeWeaponsConfigOption rangeWeaponsConfigOption = (RangeWeaponsConfigOption)this.this$0.rangeWeaponsConfigOptions.getOptionByName(rangeWeaponAttribute.getId());
            if (rangeWeaponsConfigOption != null) {
                rangeWeaponsConfigOption.setValue(value);
            }
        }

        public void save(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath() + File.separator + "editor" + File.separator + "custom_range_weapon_data_" + weaponKey.toLowerCase() + ".txt";
            this.save(fileName, item);
        }

        public void saveDefaults(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath() + File.separator + "editor" + File.separator + "default_range_weapon_data_" + weaponKey.toLowerCase() + ".txt";
            this.save(fileName, item);
        }

        private void save(String fileName, Item item) {
            this.setOptionValue(RangeWeaponAttribute.CONDITION_LOWER_CHANCE, item.conditionLowerChance);
            this.setOptionValue(RangeWeaponAttribute.MIN_DAMAGE, item.minDamage);
            this.setOptionValue(RangeWeaponAttribute.MAX_DAMAGE, item.maxDamage);
            this.setOptionValue(RangeWeaponAttribute.MIN_RANGE, item.minRange);
            this.setOptionValue(RangeWeaponAttribute.MAX_RANGE, item.maxRange);
            this.setOptionValue(RangeWeaponAttribute.MIN_SIGHT_RANGE, item.minSightRange);
            this.setOptionValue(RangeWeaponAttribute.MAX_SIGHT_RANGE, item.maxSightRange);
            this.setOptionValue(RangeWeaponAttribute.MAX_HIT_COUNT, item.maxHitCount);
            this.setOptionValue(RangeWeaponAttribute.MAX_AMMO, item.maxAmmo);
            this.setOptionValue(RangeWeaponAttribute.DOOR_DAMAGE, item.doorDamage);
            this.setOptionValue(RangeWeaponAttribute.SOUND_RADIUS, item.soundRadius);
            this.setOptionValue(RangeWeaponAttribute.TO_HIT_MODIFIER, item.toHitModifier);
            this.setOptionValue(RangeWeaponAttribute.CRITICAL_CHANCE, item.criticalChance);
            this.setOptionValue(RangeWeaponAttribute.CRIT_DMG_MULTIPLIER, item.critDmgMultiplier);
            this.setOptionValue(RangeWeaponAttribute.AIMING_PERK_CRIT_MOD, item.aimingPerkCritModifier);
            this.setOptionValue(RangeWeaponAttribute.AIMING_PERK_RANGE_MOD, item.aimingPerkRangeModifier);
            this.setOptionValue(RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD, item.aimingPerkHitChanceModifier);
            this.setOptionValue(RangeWeaponAttribute.HIT_CHANCE, item.hitChance);
            this.setOptionValue(RangeWeaponAttribute.RECOIL_DELAY, item.recoilDelay);
            this.setOptionValue(RangeWeaponAttribute.RELOAD_TIME, item.reloadTime);
            this.setOptionValue(RangeWeaponAttribute.AIMING_TIME, item.aimingTime);
            this.setOptionValue(RangeWeaponAttribute.JAM_GUN_CHANCE, item.jamGunChance);
            this.setOptionValue(RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER, item.cyclicRateMultiplier);
            this.setOptionValue(RangeWeaponAttribute.PROJECTILE_COUNT, item.projectileCount);
            this.setOptionValue(RangeWeaponAttribute.PROJECTILE_SPREAD, item.projectileSpread);
            this.setOptionValue(RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER, item.projectileWeightCenter);
            ConfigFile configFile = new ConfigFile();
            configFile.write(fileName, 1, this.options);
        }

        public void load(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath() + File.separator + "editor" + File.separator + "custom_range_weapon_data_" + weaponKey.toLowerCase() + ".txt";
            this.load(fileName, item);
        }

        public void loadDefaults(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath() + File.separator + "editor" + File.separator + "default_range_weapon_data_" + weaponKey.toLowerCase() + ".txt";
            this.load(fileName, item);
        }

        private void load(String fileName, Item item) {
            ConfigFile configFile = new ConfigFile();
            if (configFile.read(fileName)) {
                for (int i = 0; i < configFile.getOptions().size(); ++i) {
                    ConfigOption configOption = configFile.getOptions().get(i);
                    RangeWeaponsConfigOption rangeWeaponsConfigOption = (RangeWeaponsConfigOption)this.this$0.rangeWeaponsConfigOptions.getOptionByName(configOption.getName());
                    if (rangeWeaponsConfigOption == null) continue;
                    rangeWeaponsConfigOption.parse(configOption.getValueAsString());
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getId())) {
                        item.conditionLowerChance = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MIN_DAMAGE.getId())) {
                        item.minDamage = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MAX_DAMAGE.getId())) {
                        item.maxDamage = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MIN_RANGE.getId())) {
                        item.minRange = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MAX_RANGE.getId())) {
                        item.maxRange = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MIN_SIGHT_RANGE.getId())) {
                        item.minSightRange = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MAX_SIGHT_RANGE.getId())) {
                        item.maxSightRange = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MAX_HIT_COUNT.getId())) {
                        item.maxHitCount = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.MAX_AMMO.getId())) {
                        item.maxAmmo = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.DOOR_DAMAGE.getId())) {
                        item.doorDamage = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.SOUND_RADIUS.getId())) {
                        item.soundRadius = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.TO_HIT_MODIFIER.getId())) {
                        item.toHitModifier = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.CRITICAL_CHANCE.getId())) {
                        item.criticalChance = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getId())) {
                        item.critDmgMultiplier = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getId())) {
                        item.aimingPerkCritModifier = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getId())) {
                        item.aimingPerkRangeModifier = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getId())) {
                        item.aimingPerkHitChanceModifier = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.HIT_CHANCE.getId())) {
                        item.hitChance = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.RECOIL_DELAY.getId())) {
                        item.recoilDelay = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.RELOAD_TIME.getId())) {
                        item.reloadTime = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.AIMING_TIME.getId())) {
                        item.aimingTime = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.JAM_GUN_CHANCE.getId())) {
                        item.jamGunChance = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getId())) {
                        item.cyclicRateMultiplier = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.PROJECTILE_COUNT.getId())) {
                        item.projectileCount = (int)rangeWeaponsConfigOption.getValue();
                    }
                    if (rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.PROJECTILE_SPREAD.getId())) {
                        item.projectileSpread = (float)rangeWeaponsConfigOption.getValue();
                    }
                    if (!rangeWeaponsConfigOption.getName().equals(RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getId())) continue;
                    item.projectileWeightCenter = (float)rangeWeaponsConfigOption.getValue();
                }
            }
        }
    }

    private static enum RangeWeaponAttribute {
        CONDITION_LOWER_CHANCE("ConditionLowerChance", 1.0, 300.0, 1.0),
        MIN_DAMAGE("MinDamage", 0.0, 10.0, 1.0),
        MAX_DAMAGE("MaxDamage", 0.0, 10.0, 1.0),
        MIN_RANGE("MinRange", 0.0, 50.0, 1.0),
        MAX_RANGE("MaxRange", 0.0, 50.0, 1.0),
        MIN_SIGHT_RANGE("minSightRange", 0.0, 20.0, 1.0),
        MAX_SIGHT_RANGE("maxSightRange", 0.0, 20.0, 1.0),
        MAX_HIT_COUNT("MaxHitCount", 1.0, 10.0, 1.0),
        MAX_AMMO("maxAmmo", 1.0, 100.0, 1.0),
        DOOR_DAMAGE("DoorDamage", 1.0, 20.0, 1.0),
        SOUND_RADIUS("SoundRadius", 1.0, 200.0, 1.0),
        TO_HIT_MODIFIER("ToHitModifier", 0.0, 100.0, 0.0),
        CRITICAL_CHANCE("CriticalChance", 0.0, 100.0, 0.0),
        CRIT_DMG_MULTIPLIER("critDmgMultiplier", 0.0, 20.0, 1.0),
        AIMING_PERK_CRIT_MOD("AimingPerkCritModifier", 0.0, 20.0, 0.0),
        AIMING_PERK_RANGE_MOD("AimingPerkRangeModifier", 0.0, 20.0, 0.0),
        AIMING_PERK_HIT_CHANCE_MOD("AimingPerkHitChanceModifier", 0.0, 100.0, 0.0),
        HIT_CHANCE("HitChance", 1.0, 100.0, 1.0),
        RECOIL_DELAY("RecoilDelay", 1.0, 100.0, 1.0),
        RELOAD_TIME("reloadTime", 1.0, 100.0, 1.0),
        AIMING_TIME("aimingTime", 1.0, 100.0, 1.0),
        JAM_GUN_CHANCE("jamGunChance", 0.0, 20.0, 0.0),
        CYCLIC_RATE_MULTIPLIER("cyclicRateMultiplier", 0.0, 100.0, 1.0),
        PROJECTILE_COUNT("ProjectileCount", 1.0, 9.0, 1.0),
        PROJECTILE_SPREAD("projectileSpread", 0.0, 10.0, 0.0),
        PROJECTILE_WEIGHT_CENTER("projectileWeightCenter", 0.0, 10.0, 0.0);

        private final String id;
        private final double min;
        private final double max;
        private final double defaultValue;

        private RangeWeaponAttribute(String id, double min, double max, double defaultValue) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        public String getId() {
            return this.id;
        }

        public double getMin() {
            return this.min;
        }

        public double getMax() {
            return this.max;
        }

        public double getDefaultValue() {
            return this.defaultValue;
        }
    }

    public static class RangeWeaponsConfigOption
    extends DoubleConfigOption {
        public RangeWeaponsConfigOption(RangeWeaponAttribute rangeWeaponAttribute, double min, double max, double defaultValue, ArrayList<RangeWeaponsConfigOption> options) {
            super(rangeWeaponAttribute.getId(), min, max, defaultValue);
            options.add(this);
        }
    }
}

