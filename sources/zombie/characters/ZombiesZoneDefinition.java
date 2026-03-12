/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.PersistentOutfits;
import zombie.characters.AttachedItems.AttachedWeaponDefinitions;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.UnderwearDefinition;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class ZombiesZoneDefinition {
    private static final ArrayList<ZZDZone> s_zoneList = new ArrayList();
    private static final HashMap<String, ZZDZone> s_zoneMap = new HashMap();
    public static boolean dirty = true;
    private static final PickDefinition pickDef = new PickDefinition();
    private static final HashMap<String, ZZDOutfit> s_customOutfitMap = new HashMap();

    private static void checkDirty() {
        if (dirty) {
            dirty = false;
            ZombiesZoneDefinition.init();
        }
    }

    private static void init() {
        s_zoneList.clear();
        s_zoneMap.clear();
        Object object = LuaManager.env.rawget("ZombiesZoneDefinition");
        if (!(object instanceof KahluaTableImpl)) {
            return;
        }
        KahluaTableImpl zombiesZoneDefinition = (KahluaTableImpl)object;
        KahluaTableIterator iterator2 = zombiesZoneDefinition.iterator();
        while (iterator2.advance()) {
            Object object2 = iterator2.getValue();
            if (!(object2 instanceof KahluaTableImpl)) continue;
            KahluaTableImpl zoneTable = (KahluaTableImpl)object2;
            ZZDZone zone = ZombiesZoneDefinition.initZone(iterator2.getKey().toString(), zoneTable);
            if (zone == null) continue;
            s_zoneList.add(zone);
            s_zoneMap.put(zone.name, zone);
        }
    }

    private static ZZDZone initZone(String name, KahluaTableImpl zoneTable) {
        ZZDZone zone = new ZZDZone();
        zone.name = name;
        zone.femaleChance = zoneTable.rawgetInt("femaleChance");
        zone.maleChance = zoneTable.rawgetInt("maleChance");
        zone.chanceToSpawn = zoneTable.rawgetInt("chanceToSpawn");
        zone.toSpawn = zoneTable.rawgetInt("toSpawn");
        KahluaTableIterator iterator2 = zoneTable.iterator();
        while (iterator2.advance()) {
            KahluaTableImpl outfitTable;
            ZZDOutfit outfit;
            Object object = iterator2.getValue();
            if (!(object instanceof KahluaTableImpl) || (outfit = ZombiesZoneDefinition.initOutfit(outfitTable = (KahluaTableImpl)object)) == null) continue;
            outfit.customName = "ZZD." + zone.name + "." + outfit.name;
            zone.outfits.add(outfit);
        }
        return zone;
    }

    private static ZZDOutfit initOutfit(KahluaTableImpl outfitTable) {
        ZZDOutfit outfit = new ZZDOutfit();
        outfit.name = outfitTable.rawgetStr("name");
        outfit.chance = outfitTable.rawgetFloat("chance");
        outfit.gender = outfitTable.rawgetStr("gender");
        outfit.toSpawn = outfitTable.rawgetInt("toSpawn");
        outfit.mandatory = outfitTable.rawgetStr("mandatory");
        outfit.room = outfitTable.rawgetStr("room");
        outfit.femaleHairStyles = ZombiesZoneDefinition.initStringChance(outfitTable.rawgetStr("femaleHairStyles"));
        outfit.maleHairStyles = ZombiesZoneDefinition.initStringChance(outfitTable.rawgetStr("maleHairStyles"));
        outfit.beardStyles = ZombiesZoneDefinition.initStringChance(outfitTable.rawgetStr("beardStyles"));
        return outfit;
    }

    private static ArrayList<StringChance> initStringChance(String styles) {
        String[] split;
        if (StringUtils.isNullOrWhitespace(styles)) {
            return null;
        }
        ArrayList<StringChance> result = new ArrayList<StringChance>();
        for (String style : split = styles.split(";")) {
            String[] splitStyle = style.split(":");
            StringChance stringChance = new StringChance();
            stringChance.str = splitStyle[0];
            stringChance.chance = Float.parseFloat(splitStyle[1]);
            result.add(stringChance);
        }
        return result;
    }

    public static void dressInRandomOutfit(IsoZombie chr) {
        if (chr.isSkeleton()) {
            return;
        }
        IsoGridSquare square = chr.getCurrentSquare();
        if (square == null) {
            return;
        }
        PickDefinition pickDef = ZombiesZoneDefinition.pickDefinition(square.x, square.y, square.z, chr.isFemale(), false);
        if (pickDef == null) {
            String roomName = square.getRoom() == null ? null : square.getRoom().getName();
            Outfit outfit = ZombiesZoneDefinition.getRandomDefaultOutfit(chr.isFemale(), roomName);
            UnderwearDefinition.addRandomUnderwear(chr);
            chr.dressInPersistentOutfit(outfit.name);
            return;
        }
        UnderwearDefinition.addRandomUnderwear(chr);
        ZombiesZoneDefinition.applyDefinition(chr, pickDef.zone, pickDef.table, pickDef.female);
    }

    public static Zone getDefinitionZoneAt(int x, int y, int z) {
        Zone zone;
        int i;
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesAt(x, y, z);
        ArrayList<Zone> zones2 = new ArrayList<Zone>();
        for (i = zones.size() - 1; i >= 0; --i) {
            zone = zones.get(i);
            if ("ZombiesType".equalsIgnoreCase(zone.type) && zone.name != null && s_zoneMap.get(zone.name) != null) {
                return zone;
            }
            if (!s_zoneMap.containsKey(zone.type)) continue;
            zones2.add(zone);
        }
        for (i = zones2.size() - 1; i >= 0; --i) {
            zone = (Zone)zones2.get(i);
            if (!s_zoneMap.containsKey(zone.type)) continue;
            return zone;
        }
        return null;
    }

    public static PickDefinition pickDefinition(int x, int y, int z, boolean bFemale, boolean isOnSpawn) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (square == null) {
            return null;
        }
        String roomName = square.getRoom() == null ? null : square.getRoom().getName();
        ZombiesZoneDefinition.checkDirty();
        Zone zombieZone = ZombiesZoneDefinition.getDefinitionZoneAt(x, y, z);
        if (zombieZone == null) {
            return null;
        }
        if (zombieZone.spawnSpecialZombies == Boolean.FALSE) {
            return null;
        }
        String name = StringUtils.isNullOrEmpty(zombieZone.name) ? zombieZone.type : zombieZone.name;
        ZZDZone zedSpawnTable = s_zoneMap.get(name);
        if (zedSpawnTable == null) {
            return null;
        }
        if (zedSpawnTable.chanceToSpawn != -1) {
            int chance = zedSpawnTable.chanceToSpawn;
            int maxToSpawn = zedSpawnTable.toSpawn;
            ArrayList<UUID> alreadySpawnedZone = IsoWorld.instance.getSpawnedZombieZone().get(zombieZone.getName());
            if (alreadySpawnedZone == null) {
                alreadySpawnedZone = new ArrayList();
                IsoWorld.instance.getSpawnedZombieZone().put(zombieZone.getName(), alreadySpawnedZone);
            }
            if (alreadySpawnedZone.contains(zombieZone.id)) {
                zombieZone.spawnSpecialZombies = true;
            }
            if (maxToSpawn == -1 || zombieZone.spawnSpecialZombies == null && alreadySpawnedZone.size() < maxToSpawn) {
                if (Rand.Next(100) < chance) {
                    zombieZone.spawnSpecialZombies = true;
                    alreadySpawnedZone.add(zombieZone.id);
                } else {
                    zombieZone.spawnSpecialZombies = false;
                    zombieZone = null;
                }
            }
        }
        if (zombieZone == null) {
            return null;
        }
        ArrayList<ZZDOutfit> mandatory = new ArrayList<ZZDOutfit>();
        ArrayList<ZZDOutfit> normal = new ArrayList<ZZDOutfit>();
        int maleChance = zedSpawnTable.maleChance;
        int femaleChance = zedSpawnTable.femaleChance;
        if (maleChance > 0 && Rand.Next(100) < maleChance) {
            bFemale = false;
        }
        if (femaleChance > 0 && Rand.Next(100) < femaleChance) {
            bFemale = true;
        }
        for (int i = 0; i < zedSpawnTable.outfits.size(); ++i) {
            ZZDOutfit zedType = zedSpawnTable.outfits.get(i);
            String gender = zedType.gender;
            String room = zedType.room;
            if (room != null && (roomName == null || !room.contains(roomName)) || "male".equalsIgnoreCase(gender) && bFemale || "female".equalsIgnoreCase(gender) && !bFemale) continue;
            String outfitName = zedType.name;
            boolean isMandatory = Boolean.parseBoolean(zedType.mandatory);
            if (isMandatory) {
                int alreadySpawnNumber;
                int n = alreadySpawnNumber = zombieZone.spawnedZombies == null ? 0 : zombieZone.spawnedZombies.getOrDefault(outfitName, 0);
                if (alreadySpawnNumber >= zedType.toSpawn) continue;
                mandatory.add(zedType);
                if (!isOnSpawn) continue;
                if (zombieZone.spawnedZombies == null) {
                    zombieZone.spawnedZombies = new HashMap();
                    zombieZone.spawnedZombies.put(outfitName, 1);
                    continue;
                }
                zombieZone.spawnedZombies.put(outfitName, alreadySpawnNumber + 1);
                continue;
            }
            normal.add(zedType);
        }
        ZZDOutfit zombieToSpawn = !mandatory.isEmpty() ? (ZZDOutfit)PZArrayUtil.pickRandom(mandatory) : ZombiesZoneDefinition.getRandomOutfitInSetList(normal, true);
        if (zombieToSpawn == null) {
            return null;
        }
        ZombiesZoneDefinition.pickDef.table = zombieToSpawn;
        ZombiesZoneDefinition.pickDef.female = bFemale;
        ZombiesZoneDefinition.pickDef.zone = zombieZone;
        return pickDef;
    }

    public static void applyDefinition(IsoZombie chr, Zone zombieZone, ZZDOutfit zombieToSpawn, boolean bFemale) {
        chr.setFemaleEtc(bFemale);
        Outfit outfitSource = !bFemale ? OutfitManager.instance.FindMaleOutfit(zombieToSpawn.name) : OutfitManager.instance.FindFemaleOutfit(zombieToSpawn.name);
        if (outfitSource == null) {
            outfitSource = OutfitManager.instance.GetRandomOutfit(bFemale);
        } else if (zombieZone != null) {
            if (zombieZone.spawnedZombies == null) {
                zombieZone.spawnedZombies = new HashMap();
                zombieZone.spawnedZombies.put(outfitSource.name, 1);
            } else {
                int alreadySpawnNumber = zombieZone.spawnedZombies.getOrDefault(outfitSource.name, 0);
                zombieZone.spawnedZombies.put(outfitSource.name, alreadySpawnNumber + 1);
            }
        }
        if (outfitSource != null) {
            chr.dressInPersistentOutfit(outfitSource.name);
        }
        ModelManager.instance.ResetNextFrame(chr);
        chr.advancedAnimator.OnAnimDataChanged(false);
    }

    public static Outfit getRandomDefaultOutfit(boolean bFemale, String roomName) {
        ZZDOutfit zombieToSpawn;
        ArrayList<ZZDOutfit> list = new ArrayList<ZZDOutfit>();
        KahluaTable zombiesZoneDefinition = (KahluaTable)LuaManager.env.rawget("ZombiesZoneDefinition");
        ZZDZone zedSpawnTable = s_zoneMap.get("Default");
        for (int i = 0; i < zedSpawnTable.outfits.size(); ++i) {
            zombieToSpawn = zedSpawnTable.outfits.get(i);
            String gender = zombieToSpawn.gender;
            String room = zombieToSpawn.room;
            if (room != null && (roomName == null || !room.contains(roomName)) || gender != null && (!"male".equalsIgnoreCase(gender) || bFemale) && (!"female".equalsIgnoreCase(gender) || !bFemale)) continue;
            list.add(zombieToSpawn);
        }
        zombieToSpawn = ZombiesZoneDefinition.getRandomOutfitInSetList(list, false);
        Outfit result = null;
        if (zombieToSpawn != null) {
            result = bFemale ? OutfitManager.instance.FindFemaleOutfit(zombieToSpawn.name) : OutfitManager.instance.FindMaleOutfit(zombieToSpawn.name);
        }
        if (result == null) {
            result = OutfitManager.instance.GetRandomOutfit(bFemale);
        }
        return result;
    }

    public static ZZDOutfit getRandomOutfitInSetList(ArrayList<ZZDOutfit> list, boolean doTotalChance100) {
        float totalChance = 0.0f;
        for (int i = 0; i < list.size(); ++i) {
            ZZDOutfit outfitTable = list.get(i);
            totalChance += outfitTable.chance;
        }
        float choice = Rand.Next(0.0f, 100.0f);
        if (!doTotalChance100 || totalChance > 100.0f) {
            choice = Rand.Next(0.0f, totalChance);
        }
        float subtotal = 0.0f;
        for (int i = 0; i < list.size(); ++i) {
            ZZDOutfit outfitTable = list.get(i);
            if (!(choice < (subtotal += outfitTable.chance))) continue;
            return outfitTable;
        }
        return null;
    }

    private static String getRandomHairOrBeard(ArrayList<StringChance> styles) {
        float choice = OutfitRNG.Next(0.0f, 100.0f);
        float subtotal = 0.0f;
        for (int i = 0; i < styles.size(); ++i) {
            StringChance stringChance = styles.get(i);
            if (!(choice < (subtotal += stringChance.chance))) continue;
            if ("null".equalsIgnoreCase(stringChance.str)) {
                return "";
            }
            return stringChance.str;
        }
        return null;
    }

    public static void registerCustomOutfits() {
        ZombiesZoneDefinition.checkDirty();
        s_customOutfitMap.clear();
        for (ZZDZone zone : s_zoneList) {
            for (ZZDOutfit outfit : zone.outfits) {
                PersistentOutfits.instance.registerOutfitter(outfit.customName, true, ZombiesZoneDefinition::ApplyCustomOutfit);
                s_customOutfitMap.put(outfit.customName, outfit);
            }
        }
    }

    private static void ApplyCustomOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        ZZDOutfit zombieToSpawn = s_customOutfitMap.get(outfitName);
        boolean female = (outfitID & Integer.MIN_VALUE) != 0;
        IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
        if (zombie != null) {
            zombie.setFemaleEtc(female);
        }
        chr.dressInNamedOutfit(zombieToSpawn.name);
        if (zombie == null) {
            PersistentOutfits.instance.removeFallenHat(outfitID, chr);
            return;
        }
        AttachedWeaponDefinitions.instance.addRandomAttachedWeapon(zombie);
        zombie.addRandomBloodDirtHolesEtc();
        boolean bFemale = chr.isFemale();
        if (bFemale && zombieToSpawn.femaleHairStyles != null) {
            zombie.getHumanVisual().setHairModel(ZombiesZoneDefinition.getRandomHairOrBeard(zombieToSpawn.femaleHairStyles));
        }
        if (!bFemale && zombieToSpawn.maleHairStyles != null) {
            zombie.getHumanVisual().setHairModel(ZombiesZoneDefinition.getRandomHairOrBeard(zombieToSpawn.maleHairStyles));
        }
        if (!bFemale && zombieToSpawn.beardStyles != null) {
            zombie.getHumanVisual().setBeardModel(ZombiesZoneDefinition.getRandomHairOrBeard(zombieToSpawn.beardStyles));
        }
        PersistentOutfits.instance.removeFallenHat(outfitID, chr);
    }

    public static int pickPersistentOutfit(IsoGridSquare square) {
        Outfit outfit;
        if (!GameServer.server) {
            return 0;
        }
        boolean bFemale = Rand.Next(2) == 0;
        PickDefinition pickDef = ZombiesZoneDefinition.pickDefinition(square.x, square.y, square.z, bFemale, true);
        if (pickDef == null) {
            String roomName = square.getRoom() == null ? null : square.getRoom().getName();
            outfit = ZombiesZoneDefinition.getRandomDefaultOutfit(bFemale, roomName);
        } else {
            bFemale = pickDef.female;
            String outfitName = pickDef.table.name;
            outfit = bFemale ? OutfitManager.instance.FindFemaleOutfit(outfitName) : OutfitManager.instance.FindMaleOutfit(outfitName);
        }
        if (outfit == null) {
            boolean outfitName = true;
        } else {
            int outfitID = PersistentOutfits.instance.pickOutfit(outfit.name, bFemale);
            if (outfitID == 0) {
                boolean bl = true;
            } else {
                return outfitID;
            }
        }
        return 0;
    }

    private static final class ZZDZone {
        String name;
        int femaleChance;
        int maleChance;
        int chanceToSpawn;
        int toSpawn;
        final ArrayList<ZZDOutfit> outfits = new ArrayList();

        private ZZDZone() {
        }
    }

    private static final class ZZDOutfit {
        String name;
        String customName;
        float chance;
        int toSpawn;
        String gender;
        String mandatory;
        String room;
        ArrayList<StringChance> femaleHairStyles;
        ArrayList<StringChance> maleHairStyles;
        ArrayList<StringChance> beardStyles;

        private ZZDOutfit() {
        }
    }

    private static final class StringChance {
        String str;
        float chance;

        private StringChance() {
        }
    }

    public static final class PickDefinition {
        Zone zone;
        ZZDOutfit table;
        boolean female;
    }
}

