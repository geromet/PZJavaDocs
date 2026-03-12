/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Business {
    public static final Business MC_COY_LOGGING = Business.registerBase("McCoyLogging");
    public static final Business VALU_TECH = Business.registerBase("ValuTech");
    public static final Business EGENEREX = Business.registerBase("Egenerex");
    public static final Business UNITED_SHIPPING_LOGISTICS = Business.registerBase("UnitedShippingLogistics");
    public static final Business PERFICK_POTATO_CO = Business.registerBase("PerfickPotatoCo");
    public static final Business HERR_FLICK_KNIVES = Business.registerBase("HerrFlickKnives");
    public static final Business COBBER_METALS = Business.registerBase("CobberMetals");
    public static final Business BANSHEE_HOLLOWAY = Business.registerBase("BansheeHolloway");
    public static final Business BERING_COMPANY = Business.registerBase("BeringCompany");
    public static final Business YURI_DESIGN = Business.registerBase("YuriDesign");
    public static final Business NEWCASTLE_PAPERAND_INK = Business.registerBase("NewcastlePaperandInk");
    public static final Business BUSAN_TELECOMMUNICATIONS = Business.registerBase("BusanTelecommunications");
    public static final Business KITTEN_KNIVES = Business.registerBase("KittenKnives");
    public static final Business BUTTERFLY_MACHINERY = Business.registerBase("ButterflyMachinery");
    public static final Business WIRKLICHLANGESWORT_AG = Business.registerBase("WirklichlangeswortAG");
    public static final Business SANCHEZ_GOLDBERG = Business.registerBase("SanchezGoldberg");
    public static final Business BEANZ = Business.registerBase("Beanz");
    public static final Business BRUCEY_SOUPS = Business.registerBase("BruceySoups");
    public static final Business FELLOWS_INC = Business.registerBase("FellowsInc");
    public static final Business INVISIBLE_SLEDGEHAMMER_CORP = Business.registerBase("InvisibleSledgehammerCorp");
    public static final Business PANTHER_MOTORS = Business.registerBase("PantherMotors");
    public static final Business KILLIAN_FOODSTUFFS = Business.registerBase("KillianFoodstuffs");
    public static final Business GRENNADE_CHEMICALS = Business.registerBase("GrennadeChemicals");
    public static final Business REALLY_HARD_STEEL = Business.registerBase("ReallyHardSteel");
    public static final Business CHINESE_PETROLEUM = Business.registerBase("ChinesePetroleum");
    public static final Business BANKOF_KENTUCKY = Business.registerBase("BankofKentucky");
    public static final Business LOVEHEART_SHIPBUILDING = Business.registerBase("LoveheartShipbuilding");
    public static final Business DOUBLE_ENTRY_ACCOUNTING = Business.registerBase("DoubleEntryAccounting");
    public static final Business SWIFT_THOMPSON_AEROSPACE = Business.registerBase("SwiftThompsonAerospace");
    public static final Business FUN_XTREME_INC = Business.registerBase("FunXtremeInc");
    public static final Business IMEKAGI = Business.registerBase("Imekagi");
    public static final Business WOLFRAM_WAFFEN = Business.registerBase("WolframWaffen");
    public static final Business FOSSOIL = Business.registerBase("Fossoil");
    public static final Business SPIFFO_CORP = Business.registerBase("SpiffoCorp");
    public static final Business GIGA_MART = Business.registerBase("GigaMart");
    public static final Business KIRRUS_INC = Business.registerBase("KirrusInc");
    public static final Business FRANKLIN_MOTORS = Business.registerBase("FranklinMotors");
    public static final Business GLOBAL_COMPUTER_SOLUTIONS = Business.registerBase("GlobalComputerSolutions");
    public static final Business PARASOL_INC = Business.registerBase("ParasolInc");
    public static final Business TISCONSTRUCTION = Business.registerBase("TISConstruction");
    public static final Business PREMIUM_TECHNOLOGIES = Business.registerBase("PremiumTechnologies");
    public static final Business MMM_INC = Business.registerBase("MmmInc");
    public static final Business ALGOL_ELECTRONICS = Business.registerBase("AlgolElectronics");
    public static final Business FIBROIL = Business.registerBase("Fibroil");
    public static final Business SEAHORSE_COFFEE_CORP = Business.registerBase("SeahorseCoffeeCorp");
    public static final Business HAWTHORN_OIL = Business.registerBase("HawthornOil");
    public static final Business POP_CO = Business.registerBase("PopCo");
    public static final Business CHRYSALIS = Business.registerBase("Chrysalis");
    public static final Business NIKODA = Business.registerBase("Nikoda");
    public static final Business VALU_INSURANCE = Business.registerBase("ValuInsurance");
    public static final Business ZIPPEE = Business.registerBase("Zippee");
    public static final Business PHARMAHUG = Business.registerBase("Pharmahug");
    public static final Business SPECIFIC_ELECTRIC = Business.registerBase("SpecificElectric");
    public static final Business HALLOWAY_FRAMER = Business.registerBase("HallowayFramer");
    public static final Business REDMOND_REDMOND = Business.registerBase("RedmondRedmond");
    public static final Business HAVISHAM_HOTELS = Business.registerBase("HavishamHotels");
    public static final Business AMERICAN_TIRE = Business.registerBase("AmericanTire");
    public static final Business AMERI_GLOBE_INC = Business.registerBase("AmeriGlobeInc");
    public static final Business MASS_GENFAC_CO = Business.registerBase("MassGenfacCo");
    public static final Business FINNEGAN_GROUP = Business.registerBase("FinneganGroup");
    public static final Business PALM_TRAVEL = Business.registerBase("PalmTravel");
    public static final Business GENERAL_BROADCAST_CORPORATION = Business.registerBase("GeneralBroadcastCorporation");
    public static final Business SCITT_WILKER_FIREARMS = Business.registerBase("ScittWilkerFirearms");
    private final String translation;

    private Business(String id) {
        this.translation = "IGUI_" + id;
    }

    public static Business get(ResourceLocation id) {
        return Registries.BUSINESS.get(id);
    }

    public String toString() {
        return Registries.BUSINESS.getLocation(this).getPath();
    }

    public String getTranslation() {
        return this.translation;
    }

    public static Business register(String id) {
        return Business.register(false, id);
    }

    private static Business registerBase(String id) {
        return Business.register(true, id);
    }

    private static Business register(boolean allowDefaultNamespace, String id) {
        return Registries.BUSINESS.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Business(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Business business : Registries.BUSINESS) {
                TranslationKeyValidator.of(business.getTranslation());
            }
        }
    }
}

