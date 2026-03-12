/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Postcard {
    public static final Postcard ALASKA = Postcard.registerBase("Alaska");
    public static final Postcard ALCATRAZ = Postcard.registerBase("Alcatraz");
    public static final Postcard AMSTERDAM = Postcard.registerBase("Amsterdam");
    public static final Postcard AN_AFRICAN_SAFARI = Postcard.registerBase("anAfricanSafari");
    public static final Postcard ANTARCTICA = Postcard.registerBase("Antarctica");
    public static final Postcard ARGENTINA = Postcard.registerBase("Argentina");
    public static final Postcard ASIA = Postcard.registerBase("Asia");
    public static final Postcard ASPEN = Postcard.registerBase("Aspen");
    public static final Postcard ATHENS = Postcard.registerBase("Athens");
    public static final Postcard ATLANTA = Postcard.registerBase("Atlanta");
    public static final Postcard AUSTRALIA = Postcard.registerBase("Australia");
    public static final Postcard AUSTRIA = Postcard.registerBase("Austria");
    public static final Postcard AYERS_ROCK = Postcard.registerBase("AyersRock");
    public static final Postcard BALI = Postcard.registerBase("Bali");
    public static final Postcard BARCELONA = Postcard.registerBase("Barcelona");
    public static final Postcard BATON_ROUGE = Postcard.registerBase("BatonRouge");
    public static final Postcard BEACON_HILL = Postcard.registerBase("BeaconHill");
    public static final Postcard BEIJING = Postcard.registerBase("Beijing");
    public static final Postcard BERLIN = Postcard.registerBase("Berlin");
    public static final Postcard BERMUDA = Postcard.registerBase("Bermuda");
    public static final Postcard BIG_SUR = Postcard.registerBase("BigSur");
    public static final Postcard BOSTON = Postcard.registerBase("Boston");
    public static final Postcard BRAZIL = Postcard.registerBase("Brazil");
    public static final Postcard BROOKLYN = Postcard.registerBase("Brooklyn");
    public static final Postcard BUCKINGHAM_PALACE = Postcard.registerBase("BuckinghamPalace");
    public static final Postcard BUENOS_AIRES = Postcard.registerBase("BuenosAires");
    public static final Postcard CAIRO = Postcard.registerBase("Cairo");
    public static final Postcard CALCUTTA = Postcard.registerBase("Calcutta");
    public static final Postcard CALIFORNIA = Postcard.registerBase("California");
    public static final Postcard CAMBODIA = Postcard.registerBase("Cambodia");
    public static final Postcard CANADA = Postcard.registerBase("Canada");
    public static final Postcard CAPE_CANAVERAL = Postcard.registerBase("CapeCanaveral");
    public static final Postcard CAPE_COD = Postcard.registerBase("CapeCod");
    public static final Postcard CARNEGIE_HALL = Postcard.registerBase("CarnegieHall");
    public static final Postcard CHICAGO = Postcard.registerBase("Chicago");
    public static final Postcard CHINA = Postcard.registerBase("China");
    public static final Postcard CLEVELAND = Postcard.registerBase("Cleveland");
    public static final Postcard COLONIAL_WILLIAMSBURG = Postcard.registerBase("ColonialWilliamsburg");
    public static final Postcard COLORADO = Postcard.registerBase("Colorado");
    public static final Postcard CONEY_ISLAND = Postcard.registerBase("ConeyIsland");
    public static final Postcard COPENHAGEN = Postcard.registerBase("Copenhagen");
    public static final Postcard COSTA_RICA = Postcard.registerBase("CostaRica");
    public static final Postcard CUMBERLAND_FALLS = Postcard.registerBase("CumberlandFalls");
    public static final Postcard CYPRUS = Postcard.registerBase("Cyprus");
    public static final Postcard CZECH_REPUBLIC = Postcard.registerBase("CzechRepublic");
    public static final Postcard CZECHOSLOVAKIA = Postcard.registerBase("Czechoslovakia");
    public static final Postcard DALLAS = Postcard.registerBase("Dallas");
    public static final Postcard DEATH_VALLEY = Postcard.registerBase("DeathValley");
    public static final Postcard DUBLIN = Postcard.registerBase("Dublin");
    public static final Postcard EASTER_ISLAND = Postcard.registerBase("EasterIsland");
    public static final Postcard EDINBURGH = Postcard.registerBase("Edinburgh");
    public static final Postcard EGYPT = Postcard.registerBase("Egypt");
    public static final Postcard EL_CAPITAN = Postcard.registerBase("ElCapitan");
    public static final Postcard ELLIS_ISLAND = Postcard.registerBase("EllisIsland");
    public static final Postcard ENGLAND = Postcard.registerBase("England");
    public static final Postcard EUROPE = Postcard.registerBase("Europe");
    public static final Postcard FANEUIL_HALL = Postcard.registerBase("FaneuilHall");
    public static final Postcard FINLAND = Postcard.registerBase("Finland");
    public static final Postcard FLORENCE = Postcard.registerBase("Florence");
    public static final Postcard FLORIDA = Postcard.registerBase("Florida");
    public static final Postcard FORT_INDEPENDENCE = Postcard.registerBase("FortIndependence");
    public static final Postcard FORT_SUMTER = Postcard.registerBase("FortSumter");
    public static final Postcard FRANCE = Postcard.registerBase("France");
    public static final Postcard GALLERIA_DELL_ACCADEMIA = Postcard.registerBase("GalleriadellAccademia");
    public static final Postcard GERMANY = Postcard.registerBase("Germany");
    public static final Postcard GETTYSBURG = Postcard.registerBase("Gettysburg");
    public static final Postcard GIBRALTAR = Postcard.registerBase("Gibraltar");
    public static final Postcard GLEN_CANYON = Postcard.registerBase("GlenCanyon");
    public static final Postcard GOLDEN_GATE_PARK = Postcard.registerBase("GoldenGatePark");
    public static final Postcard GREAT_BARRIER_REEF = Postcard.registerBase("GreatBarrierReef");
    public static final Postcard GREECE = Postcard.registerBase("Greece");
    public static final Postcard GRIFFITH_OBSERVATORY = Postcard.registerBase("GriffithObservatory");
    public static final Postcard HA_LONG_BAY = Postcard.registerBase("HaLongBay");
    public static final Postcard HAITI = Postcard.registerBase("Haiti");
    public static final Postcard HANOI = Postcard.registerBase("Hanoi");
    public static final Postcard HAWAII = Postcard.registerBase("Hawaii");
    public static final Postcard HO_CHI_MINH_CITY = Postcard.registerBase("HoChiMinhCity");
    public static final Postcard HOLLYWOOD = Postcard.registerBase("Hollywood");
    public static final Postcard HONG_KONG = Postcard.registerBase("HongKong");
    public static final Postcard HONOLULU = Postcard.registerBase("Honolulu");
    public static final Postcard ICELAND = Postcard.registerBase("Iceland");
    public static final Postcard ILLINOIS = Postcard.registerBase("Illinois");
    public static final Postcard INDIA = Postcard.registerBase("India");
    public static final Postcard INDIANA = Postcard.registerBase("Indiana");
    public static final Postcard INDONESIA = Postcard.registerBase("Indonesia");
    public static final Postcard IRELAND = Postcard.registerBase("Ireland");
    public static final Postcard ISTANBUL = Postcard.registerBase("Istanbul");
    public static final Postcard ITALY = Postcard.registerBase("Italy");
    public static final Postcard JAMAICA = Postcard.registerBase("Jamaica");
    public static final Postcard JAMESTOWN = Postcard.registerBase("Jamestown");
    public static final Postcard JAPAN = Postcard.registerBase("Japan");
    public static final Postcard JERUSALEM = Postcard.registerBase("Jerusalem");
    public static final Postcard JOSHUA_TREE_NATIONAL_PARK = Postcard.registerBase("JoshuaTreeNationalPark");
    public static final Postcard KENYA = Postcard.registerBase("Kenya");
    public static final Postcard KEY_WEST = Postcard.registerBase("KeyWest");
    public static final Postcard KILLARNEY = Postcard.registerBase("Killarney");
    public static final Postcard KINGSMOUTH_ISLAND = Postcard.registerBase("KingsmouthIsland");
    public static final Postcard LAKE_BAIKAL = Postcard.registerBase("LakeBaikal");
    public static final Postcard LAKE_COMO = Postcard.registerBase("LakeComo");
    public static final Postcard LAKE_GARDA = Postcard.registerBase("LakeGarda");
    public static final Postcard LAKE_LUCERNE = Postcard.registerBase("LakeLucerne");
    public static final Postcard LAKE_MEAD = Postcard.registerBase("LakeMead");
    public static final Postcard LAKE_PLACID = Postcard.registerBase("LakePlacid");
    public static final Postcard LAKE_SUPERIOR = Postcard.registerBase("LakeSuperior");
    public static final Postcard LAKE_TAHOE = Postcard.registerBase("LakeTahoe");
    public static final Postcard LAKE_TITICACA = Postcard.registerBase("LakeTiticaca");
    public static final Postcard LAND_BETWEEN_THE_LAKES = Postcard.registerBase("LandBetweentheLakes");
    public static final Postcard LAPLAND = Postcard.registerBase("Lapland");
    public static final Postcard LAS_VEGAS = Postcard.registerBase("LasVegas");
    public static final Postcard LISBON = Postcard.registerBase("Lisbon");
    public static final Postcard LOCH_NESS = Postcard.registerBase("LochNess");
    public static final Postcard LONDON = Postcard.registerBase("London");
    public static final Postcard LOS_ANGELES = Postcard.registerBase("LosAngeles");
    public static final Postcard LOUISIANA = Postcard.registerBase("Louisiana");
    public static final Postcard MACHU_PICCHU = Postcard.registerBase("MachuPicchu");
    public static final Postcard MADRID = Postcard.registerBase("Madrid");
    public static final Postcard MALAYSIA = Postcard.registerBase("Malaysia");
    public static final Postcard MALDIVES = Postcard.registerBase("Maldives");
    public static final Postcard MALIBU = Postcard.registerBase("Malibu");
    public static final Postcard MALTA = Postcard.registerBase("Malta");
    public static final Postcard MANHATTAN = Postcard.registerBase("Manhattan");
    public static final Postcard MARENGO_CAVE = Postcard.registerBase("MarengoCave");
    public static final Postcard MARTHAS_VINEYARD = Postcard.registerBase("MarthasVineyard");
    public static final Postcard MASSACHUSETTS = Postcard.registerBase("Massachusetts");
    public static final Postcard MAUI = Postcard.registerBase("Maui");
    public static final Postcard MEMPHIS = Postcard.registerBase("Memphis");
    public static final Postcard MESA_VERDE = Postcard.registerBase("MesaVerde");
    public static final Postcard MEXICO = Postcard.registerBase("Mexico");
    public static final Postcard MIAMI = Postcard.registerBase("Miami");
    public static final Postcard MIAMI_BEACH = Postcard.registerBase("MiamiBeach");
    public static final Postcard MILWAUKEE = Postcard.registerBase("Milwaukee");
    public static final Postcard MINNESOTA = Postcard.registerBase("Minnesota");
    public static final Postcard MISSOURI = Postcard.registerBase("Missouri");
    public static final Postcard MONACO = Postcard.registerBase("Monaco");
    public static final Postcard MONGOLIA = Postcard.registerBase("Mongolia");
    public static final Postcard MONROE_LAKE = Postcard.registerBase("MonroeLake");
    public static final Postcard MONROEVILLE = Postcard.registerBase("Monroeville");
    public static final Postcard MONT_BLANC = Postcard.registerBase("MontBlanc");
    public static final Postcard MONT_SANT_MICHEL = Postcard.registerBase("MontSaintMichel");
    public static final Postcard MONTANA = Postcard.registerBase("Montana");
    public static final Postcard MONTICELLO = Postcard.registerBase("Monticello");
    public static final Postcard MONTREAL = Postcard.registerBase("Montreal");
    public static final Postcard MONTSERRAT = Postcard.registerBase("Montserrat");
    public static final Postcard MONUMENT_VALLEY = Postcard.registerBase("MonumentValley");
    public static final Postcard MOROCCO = Postcard.registerBase("Morocco");
    public static final Postcard MOSCOW = Postcard.registerBase("Moscow");
    public static final Postcard MOUNT_EVERST = Postcard.registerBase("MountEverest");
    public static final Postcard MOUNT_FUJI = Postcard.registerBase("MountFuji");
    public static final Postcard MOUNT_KILIMANJARO = Postcard.registerBase("MountKilimanjaro");
    public static final Postcard MOUNT_MCKINLEY = Postcard.registerBase("MountMcKinley");
    public static final Postcard MOUNT_OLYMPUS = Postcard.registerBase("MountOlympus");
    public static final Postcard MOUNT_RAINIER = Postcard.registerBase("MountRainier");
    public static final Postcard MOUNT_RUSHMORE = Postcard.registerBase("MountRushmore");
    public static final Postcard MOUNT_VERNON = Postcard.registerBase("MountVernon");
    public static final Postcard MOUNT_WILLIAMSON = Postcard.registerBase("MountWilliamson");
    public static final Postcard MOUNTAIN_LION_PICTURES_STUDIO = Postcard.registerBase("MountainLionPicturesStudio");
    public static final Postcard MY_OLD_KENTUCKY_HOME_PARK = Postcard.registerBase("MyOldKentuckyHomePark");
    public static final Postcard NASHVILLE = Postcard.registerBase("Nashville");
    public static final Postcard NEUSCHWANSTEIN_CASTLE = Postcard.registerBase("NeuschwansteinCastle");
    public static final Postcard NEW_ORLEANS = Postcard.registerBase("NewOrleans");
    public static final Postcard NEW_YORK = Postcard.registerBase("NewYork");
    public static final Postcard NEW_ZEALAND = Postcard.registerBase("NewZealand");
    public static final Postcard NEWCASTLE = Postcard.registerBase("Newcastle");
    public static final Postcard NEWGRANGE = Postcard.registerBase("Newgrange");
    public static final Postcard NIAGARA_FALLS = Postcard.registerBase("NiagaraFalls");
    public static final Postcard NORWAY = Postcard.registerBase("Norway");
    public static final Postcard OHIO = Postcard.registerBase("Ohio");
    public static final Postcard OKLAHOMA = Postcard.registerBase("Oklahoma");
    public static final Postcard PARIS = Postcard.registerBase("Paris");
    public static final Postcard PEARL_HARBOR = Postcard.registerBase("PearlHarbor");
    public static final Postcard PETRA = Postcard.registerBase("Petra");
    public static final Postcard PHILADELPHIA = Postcard.registerBase("Philadelphia");
    public static final Postcard PISA = Postcard.registerBase("Pisa");
    public static final Postcard POMPEII = Postcard.registerBase("Pompeii");
    public static final Postcard PORTUGAL = Postcard.registerBase("Portugal");
    public static final Postcard PRAGUE = Postcard.registerBase("Prague");
    public static final Postcard PUERTO_RICO = Postcard.registerBase("PuertoRico");
    public static final Postcard QUEBEC = Postcard.registerBase("Quebec");
    public static final Postcard RALEIGH = Postcard.registerBase("Raleigh");
    public static final Postcard RED_ROCK_CANYON = Postcard.registerBase("RedRockCanyon");
    public static final Postcard RICHMOND = Postcard.registerBase("Richmond");
    public static final Postcard RIO_DE_JANEIRO = Postcard.registerBase("RiodeJaneiro");
    public static final Postcard ROME = Postcard.registerBase("Rome");
    public static final Postcard ROSWELL = Postcard.registerBase("Roswell");
    public static final Postcard ROUTE_66 = Postcard.registerBase("Route66");
    public static final Postcard RUSSIA = Postcard.registerBase("Russia");
    public static final Postcard SAINT_PETERSBURG = Postcard.registerBase("SaintPetersburg");
    public static final Postcard SALEM = Postcard.registerBase("Salem");
    public static final Postcard SAN_DIEGO = Postcard.registerBase("SanDiego");
    public static final Postcard SAN_FRANCISCO = Postcard.registerBase("SanFrancisco");
    public static final Postcard SANTA_CATALINA_ISLAND = Postcard.registerBase("SantaCatalinaIsland");
    public static final Postcard SAUDI_ARABIA = Postcard.registerBase("SaudiArabia");
    public static final Postcard SCOTLAND = Postcard.registerBase("Scotland");
    public static final Postcard SEATTLE = Postcard.registerBase("Seattle");
    public static final Postcard SEDONA = Postcard.registerBase("Sedona");
    public static final Postcard SEOUL = Postcard.registerBase("Seoul");
    public static final Postcard SHANGHAI = Postcard.registerBase("Shanghai");
    public static final Postcard SIERRA_NEVADA = Postcard.registerBase("SierraNevada");
    public static final Postcard SINGAPORE = Postcard.registerBase("Singapore");
    public static final Postcard SOUTH_AFRICA = Postcard.registerBase("SouthAfrica");
    public static final Postcard SOUTH_AMERICA = Postcard.registerBase("SouthAmerica");
    public static final Postcard SOUTH_KOREA = Postcard.registerBase("SouthKorea");
    public static final Postcard SPAIN = Postcard.registerBase("Spain");
    public static final Postcard SPIFFO_WORLD = Postcard.registerBase("SpiffoWorld");
    public static final Postcard STONEHENGE = Postcard.registerBase("Stonehenge");
    public static final Postcard SWEDEN = Postcard.registerBase("Sweden");
    public static final Postcard SWITZERLAND = Postcard.registerBase("Switzerland");
    public static final Postcard SYDNEY = Postcard.registerBase("Sydney");
    public static final Postcard TAIWAN = Postcard.registerBase("Taiwan");
    public static final Postcard TANZANIA = Postcard.registerBase("Tanzania");
    public static final Postcard TENNESSEE = Postcard.registerBase("Tennessee");
    public static final Postcard TEXAS = Postcard.registerBase("Texas");
    public static final Postcard THAILAND = Postcard.registerBase("Thailand");
    public static final Postcard THE_ALAMO = Postcard.registerBase("theAlamo");
    public static final Postcard THE_ALPS = Postcard.registerBase("theAlps");
    public static final Postcard THE_AMAZON = Postcard.registerBase("theAmazon");
    public static final Postcard THE_AMERICAN_GUN_MUSEUM = Postcard.registerBase("theAmericanGunMuseum");
    public static final Postcard THE_AMERICAN_HISTORY_MUSEUM = Postcard.registerBase("theAmericanHistoryMuseum");
    public static final Postcard THE_AMERICAN_MEDIA_MUSEUM = Postcard.registerBase("theAmericanMediaMuseum");
    public static final Postcard THE_AMERICAN_MUSIC_HALL_OF_FAME = Postcard.registerBase("theAmericanMusicHallofFame");
    public static final Postcard THE_AMERICAN_WWII_MUSEUM = Postcard.registerBase("theAmericanWWIIMuseum");
    public static final Postcard THE_ANDES = Postcard.registerBase("theAndes");
    public static final Postcard THE_APPALACHIAN_TRAIL = Postcard.registerBase("theAppalachianTrail");
    public static final Postcard THE_APPALACHIANS = Postcard.registerBase("theAppalachians");
    public static final Postcard THE_AUSTIN_PARKER_MANSION = Postcard.registerBase("theAustinParkerMansion");
    public static final Postcard THE_AUSTRALIAN_OUTBACK = Postcard.registerBase("theAustralianOutback");
    public static final Postcard THE_BAHAMAS = Postcard.registerBase("theBahamas");
    public static final Postcard THE_BRITISH_MUSEUM = Postcard.registerBase("theBritishMuseum");
    public static final Postcard THE_BRONX = Postcard.registerBase("theBronx");
    public static final Postcard THE_CALIFORNIA_SCIENCE_MUSEUM = Postcard.registerBase("theCaliforniaScienceMuseum");
    public static final Postcard THE_CARIBBEAN = Postcard.registerBase("theCaribbean");
    public static final Postcard THE_CHEVALIER_MUSEUM = Postcard.registerBase("theChevalierMuseum");
    public static final Postcard THE_CINQUE_TERRE = Postcard.registerBase("theCinqueTerre");
    public static final Postcard THE_COLOSSEUM = Postcard.registerBase("theColosseum");
    public static final Postcard THE_DEAD_SEA = Postcard.registerBase("theDeadSea");
    public static final Postcard THE_EMPIRE_STATE_BUILDING = Postcard.registerBase("theEmpireStateBuilding");
    public static final Postcard THE_EVERGLADES = Postcard.registerBase("theEverglades");
    public static final Postcard THE_FORBIDDEN_CITY = Postcard.registerBase("theForbiddenCity");
    public static final Postcard THE_FRANKIE_MONRO_MUSEUM = Postcard.registerBase("theFrankieMonroMuseum");
    public static final Postcard THE_GALAPAGOS_ISLANDS = Postcard.registerBase("theGalapagosIslands");
    public static final Postcard THE_GRAND_CANYON = Postcard.registerBase("theGrandCanyon");
    public static final Postcard THE_GREAT_SMOKY_MOUNTAINS = Postcard.registerBase("theGreatSmokyMountains");
    public static final Postcard THE_GREAT_WALL_OF_CHINA = Postcard.registerBase("theGreatWallofChina");
    public static final Postcard THE_HAGIA_SOPHIA = Postcard.registerBase("theHagiaSophia");
    public static final Postcard THE_HANK_GILMAN_MUSEUM = Postcard.registerBase("theHankGilmanMuseum");
    public static final Postcard THE_HIMALAYAS = Postcard.registerBase("theHimalayas");
    public static final Postcard THE_HOOVER_DAM = Postcard.registerBase("theHooverDam");
    public static final Postcard THE_KLAMATH_MOUNTAINS = Postcard.registerBase("theKlamathMountains");
    public static final Postcard THE_LINCOLN_MEMORIAL = Postcard.registerBase("theLincolnMemorial");
    public static final Postcard THE_LOUVRE = Postcard.registerBase("theLouvre");
    public static final Postcard THE_MATTERHORN = Postcard.registerBase("theMatterhorn");
    public static final Postcard THE_MODERN_ART_MUSEUM = Postcard.registerBase("theModernArtMuseum");
    public static final Postcard THE_MOJAVE = Postcard.registerBase("theMojave");
    public static final Postcard THE_NATIONAL_AIR_AND_SPACE_MUSEUM = Postcard.registerBase("theNationalAirandSpaceMuseum");
    public static final Postcard THE_NATIONAL_ART_GALLERY = Postcard.registerBase("theNationalArtGallery");
    public static final Postcard THE_NATIONAL_BASEBALL_MUSEUM = Postcard.registerBase("theNationalBaseballMuseum");
    public static final Postcard THE_NATIONAL_BASKETBALL_MUSEUM = Postcard.registerBase("theNationalBasketballMuseum");
    public static final Postcard THE_NATIONAL_CAR_MUSEUM = Postcard.registerBase("theNationalCarMuseum");
    public static final Postcard THE_NATIONAL_FOOTBALL_MUSEUM = Postcard.registerBase("theNationalFootballMuseum");
    public static final Postcard THE_NATIONAL_NATURAL_HISTORY_MUSEUM = Postcard.registerBase("theNationalNaturalHistoryMuseum");
    public static final Postcard THE_NORTH_POLE = Postcard.registerBase("theNorthPole");
    public static final Postcard THE_OSCC_HALL_OF_FAME = Postcard.registerBase("theOSCCHallofFame");
    public static final Postcard THE_PACIFIC_COAST_HIGHWAY = Postcard.registerBase("thePacificCoastHighway");
    public static final Postcard THE_PALACE_OF_VERSAILLES = Postcard.registerBase("thePalaceofVersailles");
    public static final Postcard THE_PYRAMIDS_AT_GIZA = Postcard.registerBase("thePyramidsatGiza");
    public static final Postcard THE_RED_SEA = Postcard.registerBase("theRedSea");
    public static final Postcard THE_RMS_QUEEN_MARY = Postcard.registerBase("theRMSQueenMary");
    public static final Postcard THE_ROCKIES = Postcard.registerBase("theRockies");
    public static final Postcard THE_SOVIET_UNION = Postcard.registerBase("theSovietUnion");
    public static final Postcard THE_STATUE_OF_LIBERTY = Postcard.registerBase("theStatueofLiberty");
    public static final Postcard THE_TAJ_MAHAL = Postcard.registerBase("theTajMahal");
    public static final Postcard THE_UFFIZI = Postcard.registerBase("theUffizi");
    public static final Postcard THE_US_CAPITOL = Postcard.registerBase("theUSCapitol");
    public static final Postcard THE_VIRGINIA_PATTERSON_MUSEUM = Postcard.registerBase("theVirginiaPattersonMuseum");
    public static final Postcard THE_WASHINGTON_MONUMENT = Postcard.registerBase("theWashingtonMonument");
    public static final Postcard THE_WHITE_HOUSE = Postcard.registerBase("theWhiteHouse");
    public static final Postcard THE_ZOCALO = Postcard.registerBase("theZocalo");
    public static final Postcard TIBET = Postcard.registerBase("Tibet");
    public static final Postcard TIJUANA = Postcard.registerBase("Tijuana");
    public static final Postcard TIMES_SQUARE = Postcard.registerBase("TimesSquare");
    public static final Postcard TOKYO = Postcard.registerBase("Tokyo");
    public static final Postcard TORONTO = Postcard.registerBase("Toronto");
    public static final Postcard TURKEY = Postcard.registerBase("Turkey");
    public static final Postcard UKRAINE = Postcard.registerBase("Ukraine");
    public static final Postcard URUGUAY = Postcard.registerBase("Uruguay");
    public static final Postcard VALLEY_FORGE = Postcard.registerBase("ValleyForge");
    public static final Postcard VANCOUVER = Postcard.registerBase("Vancouver");
    public static final Postcard VATICAN_CITY = Postcard.registerBase("VaticanCity");
    public static final Postcard VENICE = Postcard.registerBase("Venice");
    public static final Postcard VENICE_BEACH = Postcard.registerBase("VeniceBeach");
    public static final Postcard VICTORIA_FALLS = Postcard.registerBase("VictoriaFalls");
    public static final Postcard VIETNAM = Postcard.registerBase("Vietnam");
    public static final Postcard VIRGINIA = Postcard.registerBase("Virginia");
    public static final Postcard WASHINGTON_DC = Postcard.registerBase("WashingtonDC");
    public static final Postcard WEST_VIRGINIA = Postcard.registerBase("WestVirginia");
    public static final Postcard WISCONSIN = Postcard.registerBase("Wisconsin");
    public static final Postcard YELLOWSTONE = Postcard.registerBase("Yellowstone");
    public static final Postcard YOSEMITE = Postcard.registerBase("Yosemite");
    public static final Postcard YUGOSLAVIA = Postcard.registerBase("Yugoslavia");
    public static final Postcard ZION_NATIONAL_PARK = Postcard.registerBase("ZionNationalPark");
    private final String translationKey;

    private Postcard(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Postcard get(ResourceLocation id) {
        return Registries.POSTCARD.get(id);
    }

    public String toString() {
        return Registries.POSTCARD.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Postcard register(String id) {
        return Postcard.register(false, id);
    }

    private static Postcard registerBase(String id) {
        return Postcard.register(true, id);
    }

    private static Postcard register(boolean allowDefaultNamespace, String id) {
        return Registries.POSTCARD.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Postcard(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Postcard postcard : Registries.POSTCARD) {
                TranslationKeyValidator.of(postcard.translationKey);
            }
        }
    }
}

