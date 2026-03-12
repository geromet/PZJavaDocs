/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Photo {
    public static final Photo A_BABY = Photo.registerBase("aBaby");
    public static final Photo A_BATTLEFIELD_MEDIC = Photo.registerBase("aBattlefieldMedic");
    public static final Photo A_BATTLEFIELD_NURSE = Photo.registerBase("aBattlefieldNurse");
    public static final Photo A_BEAUTIFUL_YOUNG_WOMAN = Photo.registerBase("aBeautifulYoungWoman");
    public static final Photo A_BIG_HOUSE = Photo.registerBase("aBigHouse");
    public static final Photo A_BIRTH_CERTIFICATE = Photo.registerBase("aBirthCertificate");
    public static final Photo A_BRIDE_GETTING_READY_FOR_HER_WEDDING = Photo.registerBase("aBrideGettingReadyForHerWedding");
    public static final Photo A_BUILDING_BEING_BUILT = Photo.registerBase("aBuildingBeingBuilt");
    public static final Photo A_BUILDING_ON_FIRE = Photo.registerBase("aBuildingonFire");
    public static final Photo A_BUSINESS_MARKET = Photo.registerBase("aBusyMarket");
    public static final Photo A_BUSY_STREET = Photo.registerBase("aBusyStreet");
    public static final Photo A_CABIN = Photo.registerBase("aCabin");
    public static final Photo A_CAMP = Photo.registerBase("aCamp");
    public static final Photo A_CAR_CRASH = Photo.registerBase("aCarCrash");
    public static final Photo A_CARNIVAL = Photo.registerBase("aCarnival");
    public static final Photo A_CATTLE_DRIVE = Photo.registerBase("aCattleDrive");
    public static final Photo A_CHILD = Photo.registerBase("aChild");
    public static final Photo A_CIRCUS = Photo.registerBase("aCircus");
    public static final Photo A_CITY = Photo.registerBase("aCity");
    public static final Photo A_CIVIL_WAR_BATTLEFIELD = Photo.registerBase("aCivilWarBattlefield");
    public static final Photo A_CIVIL_WAR_SOLDIER = Photo.registerBase("aCivilWarSoldier");
    public static final Photo A_COUPLE_HOLDING_HANDS = Photo.registerBase("aCoupleHoldingHands");
    public static final Photo A_COUPLE_KISSING = Photo.registerBase("aCoupleKissing");
    public static final Photo A_COUPLE_WITH_A_BABY = Photo.registerBase("aCoupleWithaBaby");
    public static final Photo A_COWBOY = Photo.registerBase("aCowboy");
    public static final Photo A_DEAD_BODY = Photo.registerBase("aDeadBody");
    public static final Photo A_DEATH_CERTIFICATE = Photo.registerBase("aDeathCertificate");
    public static final Photo A_FAMILY = Photo.registerBase("aFamily");
    public static final Photo A_FAMILY_CELEBRATING_THANKSGIVING = Photo.registerBase("aFamilyCelebratingThanksgiving");
    public static final Photo A_FAMILY_HAVING_CHRISTMAS_DINNER = Photo.registerBase("aFamilyHavingChristmasDinner");
    public static final Photo A_FAMOUS_OUTLAW = Photo.registerBase("aFamousOutlaw");
    public static final Photo A_FAMOUS_PERSON_FROM_A_LONG_TIME_AGO = Photo.registerBase("aFamousPersonFromaLongTimeAgo");
    public static final Photo A_FIRST_WORLD_WAR_SOLDIER = Photo.registerBase("aFirstWorldWarSoldier");
    public static final Photo A_FLOOD = Photo.registerBase("aFlood");
    public static final Photo A_FORT = Photo.registerBase("aFort");
    public static final Photo A_FRONTIER_FAMILY = Photo.registerBase("aFrontierFamily");
    public static final Photo A_FRONTIERSMAN = Photo.registerBase("aFrontiersman");
    public static final Photo A_GENTLEMAN = Photo.registerBase("aGentleman");
    public static final Photo A_GHOST = Photo.registerBase("aGhost");
    public static final Photo A_GROOM_GETTING_READY_FOR_HIS_WEDDING = Photo.registerBase("aGroomGettingReadyForHisWedding");
    public static final Photo A_GROUP_OF_ABOLITIONISTS = Photo.registerBase("aGroupofAbolitionists");
    public static final Photo A_GROUP_OF_CHILDREN = Photo.registerBase("aGroupofChildren");
    public static final Photo A_GROUP_OF_CIVIL_WAR_SOLDIERS = Photo.registerBase("aGroupofCivilWarSoldiers");
    public static final Photo A_GROUP_OF_COWBOYS = Photo.registerBase("aGroupofCowboys");
    public static final Photo A_GROUP_OF_FIRST_WORLD_WAR_SOLDIERS = Photo.registerBase("aGroupofFirstWorldWarSoldiers");
    public static final Photo A_GROUP_OF_KOREAN_WAR_SOLDIERS = Photo.registerBase("aGroupofKoreanWarSoldiers");
    public static final Photo A_GROUP_OF_MEN = Photo.registerBase("aGroupofMen");
    public static final Photo A_GROUP_OF_NATIVE_AMERICANS = Photo.registerBase("aGroupofNativeAmericans");
    public static final Photo A_GROUP_OF_PACIFISTS = Photo.registerBase("aGroupofPacifists");
    public static final Photo A_GROUP_OF_PEOPLE = Photo.registerBase("aGroupofPeople");
    public static final Photo A_GROUP_OF_PEOPLE_IN_BED = Photo.registerBase("aGroupofPeopleinBed");
    public static final Photo A_GROUP_OF_PROHIBITIONISTS = Photo.registerBase("aGroupofProhibitionists");
    public static final Photo A_GROUP_OF_PROTESTORS = Photo.registerBase("aGroupofProtestors");
    public static final Photo A_GROUP_OF_SCHOOLBOYS = Photo.registerBase("aGroupofSchoolboys");
    public static final Photo A_GROUP_OF_SCHOOLCHILDREN = Photo.registerBase("aGroupofSchoolchildren");
    public static final Photo A_GROUP_OF_SCHOOLGIRLS = Photo.registerBase("aGroupofSchoolgirls");
    public static final Photo A_GROUP_OF_SECOND_WORLD_WAR_SOLDIERS = Photo.registerBase("aGroupofSecondWorldWarSoldiers");
    public static final Photo A_GROUP_OF_SPIRITUALISTS = Photo.registerBase("aGroupofSpiritualists");
    public static final Photo A_GROUP_OF_SUFFRAGETTES = Photo.registerBase("aGroupofSuffragettes");
    public static final Photo A_GROUP_OF_UNCLOTHED_PEOPLE = Photo.registerBase("aGroupofUnclothedPeople");
    public static final Photo A_GROUP_OF_UNUSUAL_PLANTS = Photo.registerBase("aGroupofUnusualPlants");
    public static final Photo A_GROUP_OF_WOMEN = Photo.registerBase("aGroupofWomen");
    public static final Photo A_GROUP_OF_YOUNG_PEOPLE = Photo.registerBase("aGroupofYoungPeople");
    public static final Photo A_GRUESOME_SCENE = Photo.registerBase("aGruesomeScene");
    public static final Photo A_GUN = Photo.registerBase("aGun");
    public static final Photo A_HANDSOME_YOUNG_MAN = Photo.registerBase("aHandsomeYoungMan");
    public static final Photo A_HOMESTEADER_FAMILY = Photo.registerBase("aHomesteaderFamily");
    public static final Photo A_HORSE_DRAWING_A_PLOW = Photo.registerBase("aHorseDrawingaPlow");
    public static final Photo A_HORSE_RACE = Photo.registerBase("aHorseRace");
    public static final Photo A_HORSEDRAWN_CARRIAGE_ARRIVING_AT_A_CHURCH = Photo.registerBase("aHorsedrawnCarriageArrivingataChurch");
    public static final Photo A_HOUSE = Photo.registerBase("aHouse");
    public static final Photo A_HOUSE_BEING_BUILT = Photo.registerBase("aHouseBeingBuilt");
    public static final Photo A_HUNTER = Photo.registerBase("aHunter");
    public static final Photo A_KOREAN_WAR_SOLDIER = Photo.registerBase("aKoreanWarSoldier");
    public static final Photo A_LADY = Photo.registerBase("aLady");
    public static final Photo A_LANDMARK_BEING_BUILT = Photo.registerBase("aLandmarkBeingBuilt");
    public static final Photo A_LANDSCAPE = Photo.registerBase("aLandscape");
    public static final Photo A_LARGE_PUBLIC_EVENT = Photo.registerBase("aLargePublicEvent");
    public static final Photo A_LEADER = Photo.registerBase("aLeader");
    public static final Photo A_LICENSE_PLATE = Photo.registerBase("aLicensePlate");
    public static final Photo A_MAN = Photo.registerBase("aMan");
    public static final Photo A_MAN_ON_A_BICYCLE = Photo.registerBase("aManonaBicycle");
    public static final Photo A_MAN_WITH_A_LARGE_MUSTACHE = Photo.registerBase("aManwithaLargeMustache");
    public static final Photo A_MAN_WITH_A_LONG_BEARD = Photo.registerBase("aManwithaLongBeard");
    public static final Photo A_MARRIAGE_CERTIFICATE = Photo.registerBase("aMarriageCertificate");
    public static final Photo A_MILITARY_CAMP = Photo.registerBase("aMilitaryCamp");
    public static final Photo A_MILITARY_OFFICER = Photo.registerBase("aMilitaryOfficer");
    public static final Photo A_MISSING_PERSON = Photo.registerBase("aMissingPerson");
    public static final Photo A_MUGSHOT = Photo.registerBase("aMugshot");
    public static final Photo A_NATIVE_AMERICAN = Photo.registerBase("aNativeAmerican");
    public static final Photo A_NERVOUS_PERSON = Photo.registerBase("aNervousPerson");
    public static final Photo A_NINETEENTH_CENTURY_FAMILY = Photo.registerBase("aNineteenthCenturyFamily");
    public static final Photo A_OUTLAW = Photo.registerBase("anOutlaw");
    public static final Photo A_PADDLE_STEAMER_ON_THE_OHIO = Photo.registerBase("aPaddleSteamerontheOhio");
    public static final Photo A_PARADE = Photo.registerBase("aParade");
    public static final Photo A_PATERNITY_TEST = Photo.registerBase("aPaternityTest");
    public static final Photo A_PERSON_IN_A_COMPROMISING_POSITION = Photo.registerBase("aPersoninaCompromisingPosition");
    public static final Photo A_PERSON_WHO_IS_TIED_UP = Photo.registerBase("aPersonWho'sTiedUp");
    public static final Photo A_PERSON_WITH_CROSSHAIRS_ON_THEIR_FACE = Photo.registerBase("aPersonwithCrosshairsonTheirFace");
    public static final Photo A_PERSON_WITH_THEIR_FACE_CROSSED_OUT = Photo.registerBase("aPersonWithTheirFaceCrossedOut");
    public static final Photo A_PET = Photo.registerBase("aPet");
    public static final Photo A_PILE_OF_CASH = Photo.registerBase("aPileofCash");
    public static final Photo A_POLICE_OFFICER = Photo.registerBase("aPoliceOfficer");
    public static final Photo A_POLITICAL_MEETING = Photo.registerBase("aPoliticalMeeting");
    public static final Photo A_POLITICIAN = Photo.registerBase("aPolitician");
    public static final Photo A_PRESIDENT = Photo.registerBase("aPresident");
    public static final Photo A_RELIGIOUS_LEADER = Photo.registerBase("aReligiousLeader");
    public static final Photo A_RELIGIOUS_SERVICE = Photo.registerBase("aReligiousService");
    public static final Photo A_ROMANTIC_NATURE = Photo.registerBase("aRomanticNature");
    public static final Photo A_RUGGED_CABIN = Photo.registerBase("aRuggedCabin");
    public static final Photo A_SAILING_SHIP = Photo.registerBase("aSailingShip");
    public static final Photo A_SALOON = Photo.registerBase("aSaloon");
    public static final Photo A_SEANCE = Photo.registerBase("aSeance");
    public static final Photo A_SECOND_WORLD_WAR_SOLDIER = Photo.registerBase("aSecondWorldWarSoldier");
    public static final Photo A_SHERIFF = Photo.registerBase("aSheriff");
    public static final Photo A_SMALL_HOUSE = Photo.registerBase("aSmallHouse");
    public static final Photo A_SPORTS_GAME = Photo.registerBase("aSportsGame");
    public static final Photo A_STEAM_TRAIN = Photo.registerBase("aSteamTrain");
    public static final Photo A_STEAMSHIP = Photo.registerBase("aSteamship");
    public static final Photo A_STREET_OF_WOODEN_BUILDINGS = Photo.registerBase("aStreetofWoodenBuildings");
    public static final Photo A_SUSPICIOUS_GROUP_OF_PEOPLE = Photo.registerBase("aSuspiciousGroupofPeople");
    public static final Photo A_SUSPICIOUS_MEETING = Photo.registerBase("aSuspiciousMeeting");
    public static final Photo A_SUSPICIOUS_OBJECT = Photo.registerBase("aSuspiciousObject");
    public static final Photo A_SUSPICIOUS_PERSON = Photo.registerBase("aSuspiciousPerson");
    public static final Photo A_TEENAGER = Photo.registerBase("aTeenager");
    public static final Photo A_TOWN = Photo.registerBase("aTown");
    public static final Photo A_TRAIN_STATION_IN_THE_OLD_DAYS = Photo.registerBase("aTrainStationintheOldDays");
    public static final Photo A_TYPICAL_WESTERN_SCENE = Photo.registerBase("aTypicalWesternScene");
    public static final Photo A_VACATION = Photo.registerBase("aVacation");
    public static final Photo A_WAGON_TRAIN = Photo.registerBase("aWagonTrain");
    public static final Photo A_WANTED_FUGITIVE = Photo.registerBase("aWantedFugitive");
    public static final Photo A_WEDDING = Photo.registerBase("aWedding");
    public static final Photo A_WELL_BUILT_CABIN = Photo.registerBase("aWellBuiltCabin");
    public static final Photo A_WOMAN = Photo.registerBase("aWoman");
    public static final Photo A_WOMAN_ON_A_BICYCLE = Photo.registerBase("aWomanonaBicycle");
    public static final Photo A_WOMAN_WITH_A_HUGE_HAT = Photo.registerBase("aWomanwithaHugeHat");
    public static final Photo A_YOUNG_COUPLE = Photo.registerBase("aYoungCouple");
    public static final Photo A_YOUNG_MAN = Photo.registerBase("aYoungMan");
    public static final Photo A_YOUNG_WOMAN = Photo.registerBase("aYoungWoman");
    public static final Photo AN_ARTICLE_ABOUT_A_CRIME = Photo.registerBase("anArticleAboutaCrime");
    public static final Photo AN_ILLICIT_NATURE = Photo.registerBase("anIllicitNature");
    public static final Photo AN_OUTDATED_PIECE_OF_TECHNOLOGY = Photo.registerBase("anOutdatedPieceofTechnology");
    public static final Photo AN_UNCLOTHED_COUPLE = Photo.registerBase("anUnclothedCouple");
    public static final Photo CASH = Photo.registerBase("Cash");
    public static final Photo CHILDREN_PLAYING = Photo.registerBase("ChildrenPlaying");
    public static final Photo IMMIGRANTS = Photo.registerBase("Immigrants");
    public static final Photo IMMIGRANTS_IN_THEIR_NATIVE_DRESS = Photo.registerBase("ImmigrantsintheirNativeDress");
    public static final Photo LOUISVILLE = Photo.registerBase("Louisville");
    public static final Photo MINERS = Photo.registerBase("Miners");
    public static final Photo PEOPLE_DANCING_AT_A_WEDDING = Photo.registerBase("PeopleDancingataWedding");
    public static final Photo PEOPLE_DRESSED_UP = Photo.registerBase("PeopleDressedUp");
    public static final Photo PEOPLE_FARMING = Photo.registerBase("PeopleFarming");
    public static final Photo PEOPLE_ON_A_HORSE_DRAWN_BUGGY = Photo.registerBase("PeopleonaHorseDrawnBuggy");
    public static final Photo PEOPLE_PLAYING_BASEBALL = Photo.registerBase("PeoplePlayingBaseball");
    public static final Photo PEOPLE_PLAYING_FOOTBALL = Photo.registerBase("PeoplePlayingFootball");
    public static final Photo PEOPLE_SITTING_TOGETHER = Photo.registerBase("PeopleSittingTogether");
    public static final Photo PEOPLE_STANDING_TOGETHER = Photo.registerBase("PeopleStandingTogether");
    public static final Photo PEOPLE_WITH_AN_EARLY_MOTORCAR = Photo.registerBase("PeopleWithanEarlyMotorcar");
    public static final Photo PEOPLE_WORKING = Photo.registerBase("PeopleWorking");
    public static final Photo PEOPLE_WORKING_IN_A_FACTORY = Photo.registerBase("PeopleWorkinginaFactory");
    public static final Photo PRISONERS = Photo.registerBase("Prisoners");
    public static final Photo SECURITY_FOOTAGE = Photo.registerBase("SecurityFootage");
    public static final Photo SOME_DUBIOUS_DOCUMENTS = Photo.registerBase("SomeDubiousDocuments");
    public static final Photo SOMEONE_BEING_ARRESTED = Photo.registerBase("SomeoneBeingArrested");
    public static final Photo SOMEONE_COMMITTING_ILL_DEEDS = Photo.registerBase("SomeoneCommittingIllDeeds");
    public static final Photo SOMEONE_CONSUMING_A_SUSPICIOUS_SUBSTANCE = Photo.registerBase("SomeoneConsumingaSuspiciousSubstance");
    public static final Photo SOMEONE_FIRING_A_GUN = Photo.registerBase("SomeoneFiringaGun");
    public static final Photo SOMEONE_FORGOTTEN = Photo.registerBase("SomeoneForgotten");
    public static final Photo SOMEONE_HANDING_OVER_AN_ENVELOPE = Photo.registerBase("SomeoneHandingOveranEnvelope");
    public static final Photo SOMEONE_HOLDING_A_VERY_LARGE_VEGETABLE = Photo.registerBase("SomeoneHoldingaVeryLargeVegetable");
    public static final Photo SOMEONE_RECEIVING_A_BRIEFCASE = Photo.registerBase("SomeoneReceivingaBriefcase");
    public static final Photo SOMEONE_RECEIVING_A_PACKAGE = Photo.registerBase("SomeoneReceivingaPackage");
    public static final Photo SOMEONE_TRYING_TO_HIDE = Photo.registerBase("SomeoneTryingtoHide");
    public static final Photo SOMEONE_UNCLOTHED = Photo.registerBase("SomeoneUnclothed");
    public static final Photo SOMEONES_ANCESTORS = Photo.registerBase("SomeonesAncestors");
    public static final Photo SOMETHING_SAUCY = Photo.registerBase("SomethingSaucy");
    public static final Photo SOMETHING_TOO_FADED_TO_MAKE_OUT = Photo.registerBase("SomethingTooFadedtoMakeOut");
    public static final Photo SOMETHING_TOO_STAINED_TO_MAKE_OUT = Photo.registerBase("SomethingTooStainedtoMakeOut");
    public static final Photo THREE_PEOPLE_IN_BED = Photo.registerBase("ThreePeopleinBed");
    public static final Photo TWO_MEN_KISSING = Photo.registerBase("TwoMenKissing");
    public static final Photo TWO_PEOPLE_IN_BED = Photo.registerBase("TwoPeopleinBed");
    public static final Photo TWO_PEOPLE_KISSING = Photo.registerBase("TwoPeopleKissing");
    public static final Photo TWO_PEOPLE_SHAKING_HANDS = Photo.registerBase("TwoPeopleShakingHands");
    public static final Photo TWO_WOMEN_KISSING = Photo.registerBase("TwoWomenKissing");
    private final String translationKey;

    private Photo(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Photo get(ResourceLocation id) {
        return Registries.PHOTO.get(id);
    }

    public String toString() {
        return Registries.PHOTO.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Photo register(String id) {
        return Photo.register(false, id);
    }

    private static Photo registerBase(String id) {
        return Photo.register(true, id);
    }

    private static Photo register(boolean allowDefaultNamespace, String id) {
        return Registries.PHOTO.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Photo(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Photo photo : Registries.PHOTO) {
                TranslationKeyValidator.of(photo.getTranslationKey());
            }
        }
    }
}

