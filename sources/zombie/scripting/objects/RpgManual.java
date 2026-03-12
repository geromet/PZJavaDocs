/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class RpgManual {
    public static final RpgManual ADVENTURE_MANAGER_IMAGINATION_RULEBOOK = RpgManual.registerBase("AdventureManagerImaginationRulebook");
    public static final RpgManual ADVERSARY_COMPENDIUM = RpgManual.registerBase("AdversaryCompendium");
    public static final RpgManual CRATER_WORLD = RpgManual.registerBase("CraterWorld");
    public static final RpgManual CREATURE_DIRECTORY = RpgManual.registerBase("CreatureDirectory");
    public static final RpgManual DAMSELS_DANGERS = RpgManual.registerBase("DamselsDangers");
    public static final RpgManual FANTASIES_OF_POWER = RpgManual.registerBase("FantasiesofPower");
    public static final RpgManual JOHN_SPIRAL = RpgManual.registerBase("JohnSpiral");
    public static final RpgManual LEGENDS_OF_IRONSAND_BOG = RpgManual.registerBase("LegendsofIronsandBog");
    public static final RpgManual LOOT_LORDS = RpgManual.registerBase("LootLords");
    public static final RpgManual METALSPEAR_600_K = RpgManual.registerBase("Metalspear600K");
    public static final RpgManual PLANET_MASTERS = RpgManual.registerBase("PlanetMasters");
    public static final RpgManual PLANET_MASTERS_ALIENS_REFERENCE = RpgManual.registerBase("PlanetMastersAliensReference");
    public static final RpgManual RECORD_SHEETS_FOR_ENCHANTMENTS_AND_BATTLE = RpgManual.registerBase("RecordSheetsforEnchantmentsAndBattle");
    public static final RpgManual SWORDS_FIREBALLS = RpgManual.registerBase("SwordsFireballs");
    public static final RpgManual TRIALS_OF_TANGLEWOOD = RpgManual.registerBase("TrialsofTanglewood");
    public static final RpgManual VAMPIRE_HACKERS = RpgManual.registerBase("VampireHackers");
    private final String translationKey;

    private RpgManual(String id) {
        this.translationKey = "IGUI_RPG_" + id;
    }

    public static RpgManual get(ResourceLocation id) {
        return Registries.RPG_MANUAL.get(id);
    }

    public String toString() {
        return Registries.RPG_MANUAL.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static RpgManual register(String id) {
        return RpgManual.register(false, id);
    }

    private static RpgManual registerBase(String id) {
        return RpgManual.register(true, id);
    }

    private static RpgManual register(boolean allowDefaultNamespace, String id) {
        return Registries.RPG_MANUAL.register(RegistryReset.createLocation(id, allowDefaultNamespace), new RpgManual(id));
    }

    static {
        if (Core.IS_DEV) {
            for (RpgManual rpgMagazine : Registries.RPG_MANUAL) {
                TranslationKeyValidator.of(rpgMagazine.translationKey);
            }
        }
    }
}

