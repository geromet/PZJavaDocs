/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Catalogue {
    public static final Catalogue AA_RON_HUNTING_SUPPLIES = Catalogue.registerBase("AARonHuntingSupplies");
    public static final Catalogue AMERICAN_TIRE = Catalogue.registerBase("AmericanTire");
    public static final Catalogue AMERICAN_VACATIONS = Catalogue.registerBase("AmericanVacations");
    public static final Catalogue ART_GALLERY_OF_LOUISVILLE_ARTWORKS = Catalogue.registerBase("ArtGalleryofLouisvilleArtworks");
    public static final Catalogue BARG_N_CLOTHES = Catalogue.registerBase("BargNClothes");
    public static final Catalogue BETTER_FURNISHINGS = Catalogue.registerBase("BetterFurnishings");
    public static final Catalogue BICYCLES_93 = Catalogue.registerBase("Bicycles93");
    public static final Catalogue BLOCKO = Catalogue.registerBase("Blocko");
    public static final Catalogue BRAC_BRICK_FACTORY = Catalogue.registerBase("BracBrickFactory");
    public static final Catalogue BUTTERFLY_MACHINERY = Catalogue.registerBase("ButterflyMachinery");
    public static final Catalogue CALLYS_GIFTS = Catalogue.registerBase("CallysGifts");
    public static final Catalogue CROSSROADS_MALL = Catalogue.registerBase("CrossroadsMall");
    public static final Catalogue CRYSTALS_CHARMS = Catalogue.registerBase("CrystalsCharms");
    public static final Catalogue DASH_MOTORS = Catalogue.registerBase("DashMotors");
    public static final Catalogue DOLLIE_1993 = Catalogue.registerBase("Dollie1993");
    public static final Catalogue FAMOUS_ARTWORKS = Catalogue.registerBase("FamousArtworks");
    public static final Catalogue FASHIONA_BELLE = Catalogue.registerBase("FashionaBelle");
    public static final Catalogue FRANKLIN_MOTORS = Catalogue.registerBase("FranklinMotors");
    public static final Catalogue GUNS_AMMO = Catalogue.registerBase("GunsAmmo");
    public static final Catalogue HIT_VIDS = Catalogue.registerBase("HitVids");
    public static final Catalogue HOMEWARD_REAL_ESTATE = Catalogue.registerBase("HomewardRealEstate");
    public static final Catalogue HUGO_PLUSH = Catalogue.registerBase("HugoPlush");
    public static final Catalogue INKR_ALLEY = Catalogue.registerBase("InkrAlley");
    public static final Catalogue KENTUCKY_REAL_ESTATE_GUIDE = Catalogue.registerBase("KentuckyRealEstateGuide");
    public static final Catalogue KIRRUS_COMPUTERS = Catalogue.registerBase("KirrusComputers");
    public static final Catalogue KIRRUS_SOFTWARE = Catalogue.registerBase("KirrusSoftware");
    public static final Catalogue KNOX_KITCHENS = Catalogue.registerBase("KnoxKitchens");
    public static final Catalogue LOUISVILLE_BOAT_CLUB = Catalogue.registerBase("LouisvilleBoatClub");
    public static final Catalogue LOUISVILLE_BRUISER = Catalogue.registerBase("LouisvilleBruiser");
    public static final Catalogue LOUISVILLE_STATE_UNIVERSITY = Catalogue.registerBase("LouisvilleStateUniversity");
    public static final Catalogue LUTHEX_WATCHES = Catalogue.registerBase("LuthexWatches");
    public static final Catalogue MOTORBIKES_AND_MORE = Catalogue.registerBase("MotorbikesandMore");
    public static final Catalogue NOLANS_USED_CARS = Catalogue.registerBase("NolansUsedCars");
    public static final Catalogue OLYMPIA_DEPARTMENT_STORE = Catalogue.registerBase("OlympiaDepartmentStore");
    public static final Catalogue PALM_TRAVEL = Catalogue.registerBase("PalmTravel");
    public static final Catalogue PANTHER_MOTORS = Catalogue.registerBase("PantherMotors");
    public static final Catalogue PAWS = Catalogue.registerBase("Paws");
    public static final Catalogue PET_SHELTER = Catalogue.registerBase("PetShelter");
    public static final Catalogue PLANE_SAILING = Catalogue.registerBase("PlaneSailing");
    public static final Catalogue SCENIC_MILE_CAR_DEALERSHIP = Catalogue.registerBase("ScenicMileCarDealership");
    public static final Catalogue SEAT_YOURSELF_FURNITURE = Catalogue.registerBase("SeatYourselfFurniture");
    public static final Catalogue SHEBA_JEWELLERS = Catalogue.registerBase("ShebaJewellers");
    public static final Catalogue SHOED_FOR_THE_STARS = Catalogue.registerBase("ShoedFortheStars");
    public static final Catalogue SIGHTS_OF_KENTUCKY = Catalogue.registerBase("SightsofKentucky");
    public static final Catalogue TEEN_IDLEZ = Catalogue.registerBase("TeenIdlez");
    public static final Catalogue THE_GRAND_OHIO_MALL = Catalogue.registerBase("TheGrandOhioMall");
    public static final Catalogue THE_MODERATORS = Catalogue.registerBase("TheModerators");
    public static final Catalogue THE_TOP_HANGER = Catalogue.registerBase("TheTopHanger");
    public static final Catalogue TIGHT_END_SWIMWEAR = Catalogue.registerBase("TightEndSwimwear");
    public static final Catalogue TIME_4_SPORT = Catalogue.registerBase("Time4Sport");
    public static final Catalogue TOYZ = Catalogue.registerBase("Toyz");
    public static final Catalogue TTRPG = Catalogue.registerBase("TTRPG");
    public static final Catalogue WLW_MOTORS = Catalogue.registerBase("WLWMotors");
    public static final Catalogue WOOL_WEAR = Catalogue.registerBase("WoolWear");
    public static final Catalogue WORLDWIDE_VACATIONS = Catalogue.registerBase("WorldwideVacations");
    public static final Catalogue ZACS_HARDWARE = Catalogue.registerBase("ZacsHardware");
    private final String translationKey;

    private Catalogue(String id) {
        this.translationKey = "IGUI_" + id;
    }

    public static Catalogue get(ResourceLocation id) {
        return Registries.CATALOGUE.get(id);
    }

    public String toString() {
        return Registries.CATALOGUE.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Catalogue register(String id) {
        return Catalogue.register(false, id);
    }

    private static Catalogue registerBase(String id) {
        return Catalogue.register(true, id);
    }

    private static Catalogue register(boolean allowDefaultNamespace, String id) {
        return Registries.CATALOGUE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Catalogue(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Catalogue catalogue : Registries.CATALOGUE) {
                TranslationKeyValidator.of(catalogue.translationKey);
            }
        }
    }
}

