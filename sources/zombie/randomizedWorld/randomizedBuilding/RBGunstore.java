/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.WeaponPart;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.scripting.objects.FaceType;
import zombie.scripting.objects.ItemKey;
import zombie.util.StringUtils;
import zombie.util.list.WeightedList;

public final class RBGunstore
extends RandomizedBuildingBase {
    private static final int PROBABILITY_RIFLE_HUNTING = 40;
    private static final int PROBABILITY_RIFLE_M14 = 10;
    private static final int PROBABILITY_RIFLE_VARMINT = 60;
    private static final int PROBABILITY_RIFLE_L94 = 60;
    private static final int PROBABILITY_CARBINE_L92 = 60;
    private static final int PROBABILITY_CARBINE_TRAPPER = 80;
    private static final int PROBABILITY_RIFLE_JS14 = 80;
    private static final int PROBABILITY_RIFLE_MSR7T = 10;
    private static final int PROBABILITY_SHOTGUN = 60;
    private static final int PROBABILITY_SHOTGUN_JS3T = 10;
    private static final int PROBABILITY_PISTOL1 = 80;
    private static final int PROBABILITY_PISTOL2 = 40;
    private static final int PROBABILITY_PISTOL3 = 10;
    private static final int PROBABILITY_REVOLVER = 40;
    private static final int PROBABILITY_REVOLVER_LONG = 10;
    private static final int PROBABILITY_REVOLVER_SHORT = 60;
    private static final int PROBABILITY_AMMO_3030 = 60;
    private static final int PROBABILITY_AMMO_308 = 60;
    private static final int PROBABILITY_AMMO_357 = 20;
    private static final int PROBABILITY_AMMO_38 = 60;
    private static final int PROBABILITY_AMMO_556 = 20;
    private static final int PROBABILITY_AMMO_9mm = 60;
    private static final int PROBABILITY_AMMO_44 = 20;
    private static final int PROBABILITY_AMMO_45 = 20;
    private static final int PROBABILITY_AMMO_SHOTGUN = 80;
    private static final int PROBABILITY_DISPLAY_SHELF_RIFLES = 40;
    private static final int PROBABILITY_DISPLAY_SHELF_PISTOLS = 10;
    private static final int PROBABILITY_DISPLAY_COUNTER_AMMO = 40;
    private static final int PROBABILITY_DISPLAY_COUNTER_GUN = 40;
    private static final int PROBABILITY_DISPLAY_COUNTER_PISTOL = 80;
    private static final int PROBABILITY_SPAWN_RIFLE = 20;
    private static final int PROBABILITY_SPAWN_PISTOL = 20;
    private static final int PROBABILITY_SPAWN_BODYARMOR = 40;
    private static final WeightedList<ItemKey> PISTOLS = new WeightedList();
    private static final WeightedList<ItemKey> RIFLES = new WeightedList();
    private static final WeightedList<ItemKey> AMMO_BOXES = new WeightedList();
    private static final WeightedList<ItemKey> AMMO_CANS = new WeightedList();
    private static final WeightedList<ItemKey> AMMO_CASES = new WeightedList();

    @Override
    public void randomizeBuilding(BuildingDef def) {
        ArrayList<IsoObject> objList = this.getBuildingObjects(def);
        for (IsoObject obj : objList) {
            String type;
            IsoGridSquare sq = obj.getSquare();
            IsoSprite sprite = obj.getSprite();
            if (sprite == null) continue;
            PropertyContainer props = obj.getSprite().getProperties();
            String facing = props.get(IsoPropertyType.FACING);
            boolean facingE = FaceType.E.toString().equals(facing);
            boolean facingW = FaceType.W.toString().equals(facing);
            boolean facingN = FaceType.N.toString().equals(facing);
            if (props.has(IsoPropertyType.CUSTOM_NAME) && props.get(IsoPropertyType.CUSTOM_NAME).contains("Gun")) {
                RBGunstore.doGunShelves(facingE, sq);
                continue;
            }
            ItemContainer container = obj.getContainer();
            if (container == null) continue;
            switch (type = obj.getContainer().getType()) {
                case "counter": {
                    RBGunstore.doCounter(sq, props, facingE, facingW, facingN);
                    break;
                }
                case "locker": {
                    RBGunstore.doAmmoCans(props, facingE, facingW, facingN, sq);
                    break;
                }
                case "displaycase": {
                    RBGunstore.doDisplayCounter(sq, props, facingE, facingW, facingN);
                }
            }
        }
    }

    private static void doCounter(IsoGridSquare sq, PropertyContainer props, boolean facingE, boolean facingW, boolean facingN) {
        boolean result = true;
        for (int i = 0; i < sq.getObjects().size(); ++i) {
            IsoSprite sprite = sq.getObjects().get(i).getSprite();
            PropertyContainer props1 = sprite.getProperties();
            if ((!props1.has(IsoPropertyType.CUSTOM_NAME) || !props1.get(IsoPropertyType.CUSTOM_NAME).contains("Phone") && !props1.get(IsoPropertyType.CUSTOM_NAME).contains("Register") && !props1.get(IsoPropertyType.CUSTOM_NAME).contains("Radio")) && (sprite.getName() == null || !sprite.getName().contains("location_shop_generic_01_12"))) continue;
            result = false;
            break;
        }
        if (result) {
            int roll = Rand.Next(100);
            if (roll < 40) {
                return;
            }
            if (props.has(IsoPropertyType.GROUP_NAME) && props.get(IsoPropertyType.GROUP_NAME).contains("Corner")) {
                RBGunstore.doCornerAmmoCans(facingE, facingW, facingN, sq);
            } else {
                RBGunstore.doCounterAmmoDisplay(facingE, facingW, facingN, sq);
            }
        }
    }

    private static void doDisplayCounter(IsoGridSquare sq, PropertyContainer props, boolean facingE, boolean facingW, boolean facingN) {
        if (!props.has(IsoPropertyType.GROUP_NAME) || !props.get(IsoPropertyType.GROUP_NAME).contains("Corner")) {
            int roll = Rand.Next(100);
            if (roll < 40) {
                return;
            }
            if (roll < 80) {
                RBGunstore.doHandgunCounterDisplay(facingE, facingW, facingN, sq);
            } else {
                RBGunstore.doRifleCounterDisplay(facingE, facingW, facingN, sq);
            }
        }
    }

    private static void doAmmoCans(PropertyContainer props, boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset;
        if (props.has(IsoPropertyType.GROUP_NAME) && !props.get(IsoPropertyType.GROUP_NAME).contains("Green")) {
            return;
        }
        int ammoSlots = Rand.NextInclusive(1, 6);
        float f = facingE ? 0.34f : (xOffset = facingW ? 0.82f : 0.2f);
        float yOffset = facingE ? 0.2f : (facingN ? 0.82f : (facingW ? 0.2f : 0.34f));
        float zOffset = 0.52f;
        float xRotation = 0.0f;
        float yRotation = 0.0f;
        float zRotation = facingE ? 90.0f : (facingN ? 0.0f : (facingW ? 270.0f : 180.0f));
        for (int j = 0; j < ammoSlots; ++j) {
            InventoryContainer ammoCan = (InventoryContainer)InventoryItemFactory.CreateItem(RBGunstore.getAmmoCan());
            sq.AddWorldInventoryItem(ammoCan, xOffset, yOffset, 0.52f, false, true);
            ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
            RBGunstore.setWorldRotation(ammoCan, 0.0f, 0.0f, zRotation);
            if (facingE || facingW) {
                yOffset += 0.14f;
                continue;
            }
            xOffset += 0.14f;
        }
    }

    private static void doBodyArmor(boolean facingE, IsoGridSquare sq) {
        int slotNumber = 4;
        int currentSlot = 1;
        float xOffset = facingE ? 0.12f : 0.28f;
        float yOffset = facingE ? 0.78f : 0.12f;
        float zOffset = 0.5f;
        float xRotation = 270.0f;
        float yRotation = facingE ? 270.0f : 0.0f;
        float zRotation = 0.0f;
        for (int j = 0; j < 4; ++j) {
            Object vest = InventoryItemFactory.CreateItem(ItemKey.Clothing.VEST_BULLET_CIVILIAN);
            if (currentSlot == 1) {
                RBGunstore.spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset += 0.2f;
            } else if (currentSlot == 2) {
                RBGunstore.spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset -= 0.2f;
                if (facingE) {
                    yOffset -= 0.44f;
                } else {
                    xOffset += 0.44f;
                }
            } else if (currentSlot == 3) {
                RBGunstore.spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
                zOffset += 0.2f;
            } else if (currentSlot == 4) {
                RBGunstore.spawnBodyArmor(vest, sq, xOffset, yOffset, zOffset);
            }
            RBGunstore.setWorldRotation(vest, 270.0f, yRotation, 0.0f);
            if (GameServer.server && ((InventoryItem)vest).getWorldItem() != null) {
                ((InventoryItem)vest).getWorldItem().transmitCompleteItemToClients();
            }
            ++currentSlot;
        }
    }

    private static void doCornerAmmoCans(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset2;
        float xOffset;
        int caseSlots = 2;
        float f = facingW ? 0.44f : (xOffset = facingN ? 0.44f : 0.38f);
        float yOffset = facingE ? 0.82f : (facingN ? 0.82f : 0.34f);
        float zOffset = 0.38f;
        float xRotation = 0.0f;
        float yRotation = 0.0f;
        float zRotation = 0.0f;
        for (int j = 0; j < 2; ++j) {
            InventoryContainer ammoCase = (InventoryContainer)InventoryItemFactory.CreateItem(RBGunstore.getAmmoCase());
            sq.AddWorldInventoryItem(ammoCase, xOffset, yOffset, 0.38f, false, true);
            ItemPickerJava.rollContainerItem(ammoCase, null, ItemPickerJava.getItemPickerContainers().get(ammoCase.getType()));
            RBGunstore.setWorldRotation(ammoCase, 0.0f, 0.0f, 0.0f);
            xOffset += 0.38f;
        }
        int ammoSlots = 2;
        float f2 = facingW ? 0.82f : (xOffset2 = facingN ? 0.82f : 0.38f);
        float yOffset2 = facingE ? 0.52f : (facingN ? 0.52f : 0.64f);
        float zOffset2 = 0.38f;
        float xRotation2 = 0.0f;
        float yRotation2 = 0.0f;
        float zRotation2 = facingW ? 270.0f : (facingN ? 270.0f : 90.0f);
        for (int j = 0; j < 2; ++j) {
            InventoryContainer ammoCan = (InventoryContainer)InventoryItemFactory.CreateItem(RBGunstore.getAmmoCan());
            sq.AddWorldInventoryItem(ammoCan, xOffset2, yOffset2, 0.38f, false, true);
            ItemPickerJava.rollContainerItem(ammoCan, null, ItemPickerJava.getItemPickerContainers().get(ammoCan.getType()));
            RBGunstore.setWorldRotation(ammoCan, 0.0f, 0.0f, zRotation2);
            if (facingE || facingN) {
                yOffset2 -= 0.14f;
                continue;
            }
            yOffset2 += 0.14f;
        }
    }

    private static void doCounterAmmoDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset;
        int columns = 3;
        float f = facingE ? 0.2f : (xOffset = facingW ? 0.94f : 0.24f);
        float yOffset = facingE ? 0.84f : (facingW ? 0.84f : (facingN ? 0.94f : 0.24f));
        float zOffset = 0.38f;
        float xRotation = 0.0f;
        float yRotation = 0.0f;
        float zRotation = facingE ? 270.0f : (facingW ? 90.0f : (facingN ? 180.0f : 0.0f));
        for (int j = 0; j < 3; ++j) {
            int typeRoll = Rand.Next(100);
            float xOffset2 = 0.0f;
            float yOffset2 = 0.0f;
            for (int k = 0; k < Rand.NextInclusive(1, 4); ++k) {
                if (xOffset2 == 0.0f || yOffset2 == 0.0f) {
                    xOffset2 = xOffset;
                    yOffset2 = yOffset;
                }
                Object ammoBox = InventoryItemFactory.CreateItem(RBGunstore.getAmmoBox());
                sq.AddWorldInventoryItem((InventoryItem)ammoBox, xOffset2, yOffset2, 0.38f, false, true);
                RBGunstore.setWorldRotation(ammoBox, 0.0f, 0.0f, zRotation);
                xOffset2 += facingE ? 0.12f : 0.0f;
                xOffset2 -= facingW ? 0.12f : 0.0f;
                yOffset2 -= facingN ? 0.12f : 0.0f;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12f : 0.0f;
            }
            if (facingE || facingW) {
                yOffset -= 0.28f;
                continue;
            }
            xOffset += 0.28f;
        }
    }

    private static void doGunShelves(boolean facingE, IsoGridSquare sq) {
        int roll = Rand.Next(100);
        if (roll >= 40) {
            RBGunstore.doGunShelfRifles(facingE, sq);
        } else if (roll >= 10) {
            RBGunstore.doGunShelfHandguns(facingE, sq);
        } else {
            RBGunstore.doBodyArmor(facingE, sq);
        }
    }

    private static void doGunShelfHandguns(boolean facingE, IsoGridSquare sq) {
        int column1Slots = 4;
        float xOffset = facingE ? 0.12f : 0.2f;
        float yOffset = facingE ? 0.92f : 0.12f;
        float zOffset = 0.48f;
        float xRotation = facingE ? 90.0f : 270.0f;
        float yRotation = facingE ? 90.0f : 0.0f;
        float zRotation = facingE ? 180.0f : 0.0f;
        for (int n = 0; n < 4; ++n) {
            if (Rand.Next(100) < 20) continue;
            HandWeapon gun = RBGunstore.spawnPistol(RBGunstore.getPistol());
            sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
            RBGunstore.setWorldRotation(gun, xRotation, yRotation, zRotation);
            zOffset += 0.08f;
        }
        int column2Slots = 4;
        float xOffset2 = facingE ? 0.12f : 0.46f;
        float yOffset2 = facingE ? 0.64f : 0.12f;
        float zOffset2 = 0.48f;
        for (int n = 0; n < 4; ++n) {
            if (Rand.Next(100) < 20) continue;
            HandWeapon gun = RBGunstore.spawnPistol(RBGunstore.getPistol());
            sq.AddWorldInventoryItem(gun, xOffset2, yOffset2, zOffset2, false, true);
            RBGunstore.setWorldRotation(gun, xRotation, yRotation, zRotation);
            zOffset2 += 0.08f;
        }
        int rifleSlots = 2;
        float xOffset3 = facingE ? 0.12f : 0.7f;
        float yOffset3 = facingE ? 0.2f : 0.12f;
        float zOffset3 = 0.6f;
        float xRotation3 = 0.0f;
        float yRotation3 = 90.0f;
        float zRotation3 = facingE ? 180.0f : 270.0f;
        for (int n = 0; n < 2; ++n) {
            if (Rand.Next(100) < 20) continue;
            HandWeapon gun = RBGunstore.spawnRifle(RBGunstore.getRifle());
            sq.AddWorldInventoryItem(gun, xOffset3, yOffset3, 0.6f, false, true);
            RBGunstore.setWorldRotation(gun, 0.0f, 90.0f, zRotation3);
            yOffset3 += facingE ? 0.18f : 0.0f;
            xOffset3 += !facingE ? 0.18f : 0.0f;
        }
    }

    private static void doGunShelfRifles(boolean facingE, IsoGridSquare sq) {
        int slotNumber = 4;
        float xOffset = facingE ? 0.12f : 0.56f;
        float yOffset = facingE ? 0.56f : 0.12f;
        float zOffset = 0.48f;
        float xRotation = facingE ? 90.0f : 270.0f;
        float yRotation = facingE ? 90.0f : 0.0f;
        float zRotation = facingE ? 180.0f : 0.0f;
        for (int n = 0; n < 4; ++n) {
            if (Rand.Next(100) < 20) continue;
            HandWeapon gun = RBGunstore.spawnRifle(RBGunstore.getRifle());
            sq.AddWorldInventoryItem(gun, xOffset, yOffset, zOffset, false, true);
            RBGunstore.setWorldRotation(gun, xRotation, yRotation, zRotation);
            zOffset += 0.08f;
        }
    }

    private static void doHandgunCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        int j;
        float zRotation2;
        float yOffset2;
        float xOffset2;
        float xOffset;
        float f = facingE ? 0.8f : (facingW ? 0.42f : (xOffset = facingN ? 0.82f : 0.38f));
        float yOffset = facingE ? 0.8f : (facingW ? 0.4f : (facingN ? 0.44f : 0.8f));
        float zOffset = 0.38f;
        float xRotation = 0.0f;
        float yRotation = 0.0f;
        float zRotation = facingE ? 270.0f : (facingW ? 90.0f : (facingN ? 180.0f : 0.0f));
        HandWeapon gun = RBGunstore.spawnPistol(RBGunstore.getPistol());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38f, false, true);
        RBGunstore.setWorldRotation(gun, 0.0f, 0.0f, zRotation);
        if (!StringUtils.isNullOrEmpty(gun.getMagazineType())) {
            float f2 = facingE ? 0.88f : (xOffset2 = facingW ? 0.32f : 0.58f);
            float f3 = facingE ? 0.62f : (facingW ? 0.58f : (yOffset2 = facingN ? 0.34f : 0.84f));
            zRotation2 = facingE ? 180.0f : (facingW ? 0.0f : (facingN ? 90.0f : 270.0f));
            int clipSlots = Rand.NextInclusive(1, 2);
            for (j = 0; j < clipSlots; ++j) {
                Object clip = InventoryItemFactory.CreateItem(gun.getMagazineType());
                sq.AddWorldInventoryItem((InventoryItem)clip, xOffset2, yOffset2, 0.38f, false, true);
                RBGunstore.setWorldRotation(clip, 0.0f, 0.0f, zRotation2);
                yOffset2 -= facingE ? 0.06f : 0.0f;
                yOffset2 += facingW ? 0.06f : 0.0f;
                xOffset2 -= facingN ? 0.06f : 0.0f;
                xOffset2 += !facingE && !facingW && !facingN ? 0.06f : 0.0f;
            }
        }
        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float f4 = facingE ? 0.76f : (facingW ? 0.48f : (xOffset2 = facingN ? 0.32f : 0.84f));
            yOffset2 = facingE ? 0.38f : (facingW ? 0.84f : (facingN ? 0.48f : 0.78f));
            zRotation2 = facingE ? 270.0f : 0.0f;
            int boxSlots = Rand.NextInclusive(1, 2);
            for (j = 0; j < boxSlots; ++j) {
                Object ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem((InventoryItem)ammoBox, xOffset2, yOffset2, 0.38f, false, true);
                RBGunstore.setWorldRotation(ammoBox, 0.0f, 0.0f, zRotation2);
                xOffset2 += facingE ? 0.12f : 0.0f;
                xOffset2 -= facingW ? 0.12f : 0.0f;
                yOffset2 -= facingN ? 0.12f : 0.0f;
                yOffset2 += !facingE && !facingW && !facingN ? 0.12f : 0.0f;
            }
        }
    }

    private static void doRifleCounterDisplay(boolean facingE, boolean facingW, boolean facingN, IsoGridSquare sq) {
        float xOffset;
        float f = facingE ? 0.82f : (xOffset = facingW ? 0.42f : 0.6f);
        float yOffset = facingE ? 0.58f : (facingW ? 0.62f : (facingN ? 0.42f : 0.86f));
        float zOffset = 0.38f;
        float xRotation = 0.0f;
        float yRotation = 0.0f;
        float standardZRotation = facingE ? 270.0f : (facingW ? 90.0f : (facingN ? 180.0f : 0.0f));
        HandWeapon gun = RBGunstore.spawnRifle(RBGunstore.getRifle());
        sq.AddWorldInventoryItem(gun, xOffset, yOffset, 0.38f, false, true);
        RBGunstore.setWorldRotation(gun, 0.0f, 0.0f, standardZRotation);
        if (!StringUtils.isNullOrEmpty(gun.getAmmoBox())) {
            float xOffset2;
            float f2 = facingE ? 0.66f : (facingW ? 0.58f : (xOffset2 = facingN ? 0.88f : 0.32f));
            float yOffset2 = facingE ? 0.88f : (facingW ? 0.34f : (facingN ? 0.6f : 0.68f));
            int boxSlots = Rand.NextInclusive(1, 2);
            for (int j = 0; j < boxSlots; ++j) {
                Object ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                sq.AddWorldInventoryItem((InventoryItem)ammoBox, xOffset2, yOffset2, 0.38f, false, true);
                RBGunstore.setWorldRotation(ammoBox, 0.0f, 0.0f, standardZRotation);
                yOffset2 -= facingE ? 0.22f : 0.0f;
                yOffset2 += facingW ? 0.22f : 0.0f;
                xOffset2 -= facingN ? 0.22f : 0.0f;
                xOffset2 += !facingE && !facingW && !facingN ? 0.22f : 0.0f;
            }
        }
    }

    private static void spawnBodyArmor(InventoryItem vest, IsoGridSquare sq, float xOffset, float yOffset, float zOffset) {
        if (Rand.Next(100) >= 40) {
            sq.AddWorldInventoryItem(vest, xOffset, yOffset, zOffset, false, true);
        }
    }

    private static void setWorldRotation(InventoryItem item, float xRotation, float yRotation, float zRotation) {
        item.setWorldXRotation(xRotation);
        item.setWorldYRotation(yRotation);
        item.setWorldZRotation(zRotation);
        if (GameServer.server && item.getWorldItem() != null) {
            item.getWorldItem().transmitCompleteItemToClients();
        }
    }

    private static void addClip(HandWeapon gun) {
        if (gun.usesExternalMagazine() && !gun.isContainsClip()) {
            gun.setContainsClip(true);
        }
    }

    private static HandWeapon spawnPistol(ItemKey gunType) {
        HandWeapon gun = (HandWeapon)InventoryItemFactory.CreateItem(gunType);
        RBGunstore.addClip(gun);
        return gun;
    }

    private static HandWeapon spawnRifle(ItemKey gunType) {
        HandWeapon gun = (HandWeapon)InventoryItemFactory.CreateItem(gunType);
        RBGunstore.addClip(gun);
        if (gun.is(ItemKey.Weapon.VARMINT_RIFLE)) {
            WeaponPart scope = (WeaponPart)InventoryItemFactory.CreateItem(ItemKey.WeaponPart.X2_SCOPE);
            gun.attachWeaponPart(scope);
        }
        if (gun.is(ItemKey.Weapon.HUNTING_RIFLE)) {
            ItemKey scopeType = ItemKey.WeaponPart.X4_SCOPE;
            if (Rand.Next(100) >= 60) {
                scopeType = ItemKey.WeaponPart.X8_SCOPE;
            }
            WeaponPart scope = (WeaponPart)InventoryItemFactory.CreateItem(scopeType);
            gun.attachWeaponPart(scope);
        }
        return gun;
    }

    private static ItemKey getRifle() {
        if (RIFLES.isEmpty()) {
            RIFLES.add(ItemKey.Weapon.ASSAULT_RIFLE_2, 10);
            RIFLES.add(ItemKey.Weapon.HUNTING_RIFLE, 40);
            RIFLES.add(ItemKey.Weapon.SHOTGUN, 60);
            RIFLES.add(ItemKey.Weapon.JS3T_SHOTGUN, 10);
            RIFLES.add(ItemKey.Weapon.VARMINT_RIFLE, 60);
            RIFLES.add(ItemKey.Weapon.L94_RIFLE, 60);
            RIFLES.add(ItemKey.Weapon.L92_CARBINE, 60);
            RIFLES.add(ItemKey.Weapon.TRAPPER_CARBINE, 80);
            RIFLES.add(ItemKey.Weapon.MSR7T_RIFLE, 10);
            RIFLES.add(ItemKey.Weapon.JS14_RIFLE, 80);
        }
        return RIFLES.getRandom();
    }

    private static ItemKey getPistol() {
        if (PISTOLS.isEmpty()) {
            PISTOLS.add(ItemKey.Weapon.PISTOL_3, 10);
            PISTOLS.add(ItemKey.Weapon.REVOLVER_LONG, 10);
            PISTOLS.add(ItemKey.Weapon.REVOLVER, 40);
            PISTOLS.add(ItemKey.Weapon.PISTOL_2, 40);
            PISTOLS.add(ItemKey.Weapon.PISTOL, 80);
            PISTOLS.add(ItemKey.Weapon.REVOLVER_SHORT, 60);
        }
        return PISTOLS.getRandom();
    }

    private static ItemKey getAmmoBox() {
        if (AMMO_BOXES.isEmpty()) {
            AMMO_BOXES.add(ItemKey.Normal.BOX_556, 20);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_45_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_44_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BOX_308, 60);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_9MM_BOX, 60);
            AMMO_BOXES.add(ItemKey.Normal.SHOTGUN_SHELLS_BOX, 80);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_38_BOX, 60);
            AMMO_BOXES.add(ItemKey.Normal.BULLETS_357_BOX, 20);
            AMMO_BOXES.add(ItemKey.Normal.BOX_3030, 60);
        }
        return AMMO_BOXES.getRandom();
    }

    private static ItemKey getAmmoCan() {
        if (AMMO_CANS.isEmpty()) {
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_45, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_44, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_308, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_9MM, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_SHOTGUN_SHELLS, 80);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_38, 60);
        }
        return AMMO_CANS.getRandom();
    }

    private static ItemKey getAmmoCase() {
        if (AMMO_CASES.isEmpty()) {
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_45, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_44, 20);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_308, 60);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_9MM, 60);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_SHOTGUN_SHELLS, 80);
            AMMO_CASES.add(ItemKey.Container.BAG_PROTECTIVE_CASE_BULKY_AMMO_38, 60);
        }
        return AMMO_CASES.getRandom();
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("gunstore") != null;
    }

    public ArrayList<IsoObject> getBuildingObjects(BuildingDef def) {
        ArrayList<IsoObject> objList = new ArrayList<IsoObject>();
        ArrayList<IsoGridSquare> buildingSquares = this.getBuildingSquares(def);
        for (int i = 0; i < buildingSquares.size(); ++i) {
            IsoGridSquare sq = buildingSquares.get(i);
            for (int j = 0; j < sq.getObjects().size(); ++j) {
                if (sq.getObjects().get(j) == null) continue;
                objList.add(sq.getObjects().get(j));
            }
        }
        return objList;
    }

    public ArrayList<IsoGridSquare> getBuildingSquares(BuildingDef def) {
        ArrayList<RoomDef> rooms = def.getRooms();
        ArrayList<IsoGridSquare> buildingSquares = new ArrayList<IsoGridSquare>();
        for (int i = 0; i < rooms.size(); ++i) {
            RoomDef room = rooms.get(i);
            ArrayList<RoomDef.RoomRect> rects = room.getRects();
            for (int j = 0; j < rects.size(); ++j) {
                RoomDef.RoomRect rect = rects.get(j);
                ArrayList<IsoGridSquare> rectSquares = this.getRectSquares(rect, room);
                buildingSquares.addAll(rectSquares);
            }
        }
        return buildingSquares;
    }

    public ArrayList<IsoGridSquare> getRectSquares(RoomDef.RoomRect rect, RoomDef room) {
        ArrayList<IsoGridSquare> squares = new ArrayList<IsoGridSquare>();
        for (int x = rect.getX(); x < rect.getX2(); ++x) {
            for (int y = rect.getY(); y < rect.getY2(); ++y) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, room.getZ());
                if (sq == null || sq.getRoom() == null || !sq.getRoom().getName().contains("gunstore")) continue;
                squares.add(sq);
            }
        }
        return squares;
    }

    public RBGunstore() {
        this.name = "Gunstore";
        this.setAlwaysDo(true);
    }
}

