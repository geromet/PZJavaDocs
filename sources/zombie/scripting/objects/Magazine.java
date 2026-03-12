/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zombie.core.Core;
import zombie.inventory.InventoryItem;
import zombie.scripting.objects.MagazineSubject;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Magazine {
    private static final Map<MagazineSubject, List<Magazine>> BY_SUBJECTS = new HashMap<MagazineSubject, List<Magazine>>();
    public static final Magazine AIR_AND_SPACE_NEWS = Magazine.registerBase("AirandSpaceNews", 1950, MagazineSubject.MILITARY);
    public static final Magazine ALT_F4 = Magazine.registerBase("AltF4", 1988, MagazineSubject.TECH);
    public static final Magazine AMERICAN_CYCLIST = Magazine.registerBase("AmericanCyclist", 1892, MagazineSubject.SPORTS);
    public static final Magazine AMERICAN_HOBBIES = Magazine.registerBase("AmericanHobbies", 1902, MagazineSubject.POPULAR, MagazineSubject.HOBBY);
    public static final Magazine AMERICAN_POET = Magazine.registerBase("AmericanPoet", 1905, new MagazineSubject[0]);
    public static final Magazine ANDBORG = Magazine.registerBase("Andborg", 1935, MagazineSubject.SCIENCE, MagazineSubject.TECH, MagazineSubject.HOBBY);
    public static final Magazine ART_AMERICA = Magazine.registerBase("ArtAmerica", 1926, MagazineSubject.ART);
    public static final Magazine AU_NATUREL = Magazine.registerBase("AuNaturel", 1904, MagazineSubject.OUTDOORS);
    public static final Magazine AUTOMOBILES_MONTHLY = Magazine.registerBase("AutomobilesMonthly", 1927, MagazineSubject.CARS);
    public static final Magazine BACKROAD = Magazine.registerBase("Backroad", 1922, MagazineSubject.OUTDOORS);
    public static final Magazine BASSLINE = Magazine.registerBase("Bassline", 1954, MagazineSubject.MUSIC);
    public static final Magazine BEAUTY = Magazine.registerBase("Beauty", 1967, MagazineSubject.FASHION);
    public static final Magazine BELIEF = Magazine.registerBase("Belief", 1982, new MagazineSubject[0]);
    public static final Magazine BIG_BICEPS = Magazine.registerBase("BigBiceps", 1984, MagazineSubject.HEALTH);
    public static final Magazine BLOCKO_MAGAZINE = Magazine.registerBase("BlockoMagazine", 1987, MagazineSubject.CHILDS);
    public static final Magazine BLUESN_JAZZ = Magazine.registerBase("BluesnJazz", 1922, MagazineSubject.MUSIC);
    public static final Magazine BOINK = Magazine.registerBase("Boink", 1925, MagazineSubject.HUMOR);
    public static final Magazine BRASH = Magazine.registerBase("Brash", 1978, MagazineSubject.FASHION);
    public static final Magazine BRIEF = Magazine.registerBase("Brief", 1928, new MagazineSubject[0]);
    public static final Magazine CABIN_FEVER = Magazine.registerBase("CabinFever", 1973, MagazineSubject.OUTDOORS);
    public static final Magazine CARD_GAMES = Magazine.registerBase("CardGames", 1908, MagazineSubject.HOBBY);
    public static final Magazine CHARM = Magazine.registerBase("Charm", 1961, MagazineSubject.FASHION);
    public static final Magazine CHECKMATE = Magazine.registerBase("Checkmate", 1966, MagazineSubject.HOBBY);
    public static final Magazine CHRISTIANS_TOGETHER = Magazine.registerBase("ChristiansTogether", 1870, new MagazineSubject[0]);
    public static final Magazine CIGARS_CAVIAR = Magazine.registerBase("CigarsCaviar", 1921, MagazineSubject.RICH);
    public static final Magazine CLASSIC_BATTLES = Magazine.registerBase("ClassicBattles", 1944, MagazineSubject.MILITARY);
    public static final Magazine CODE_WORLD = Magazine.registerBase("CodeWorld", 1979, MagazineSubject.TECH);
    public static final Magazine COLLECTING = Magazine.registerBase("Collecting", 1921, MagazineSubject.HOBBY);
    public static final Magazine COMPRESSED_BOOKS = Magazine.registerBase("CompressedBooks", 1922, MagazineSubject.POPULAR);
    public static final Magazine CONGRESS_WATCHER = Magazine.registerBase("CongressWatcher", 1972, new MagazineSubject[0]);
    public static final Magazine CUSTODIAL_OPERATOR = Magazine.registerBase("CustodialOperator", 1992, MagazineSubject.POLICE);
    public static final Magazine DARK_TALES = Magazine.registerBase("DarkTales", 1923, MagazineSubject.HORROR);
    public static final Magazine DIGITAL_ADVENTURE_POWER = Magazine.registerBase("DigitalAdventurePower", 1983, MagazineSubject.TECH, MagazineSubject.TEENS, MagazineSubject.HOBBY);
    public static final Magazine DRAG_KINGS = Magazine.registerBase("DragKings", 1978, MagazineSubject.CARS);
    public static final Magazine ECONOMY = Magazine.registerBase("Economy", 1971, MagazineSubject.BUSINESS);
    public static final Magazine ELECTRON = Magazine.registerBase("Electron", 1960, MagazineSubject.SCIENCE);
    public static final Magazine ELEGANT = Magazine.registerBase("Elegant", 1954, MagazineSubject.POPULAR, MagazineSubject.FASHION);
    public static final Magazine ELEGANT_GIRL = Magazine.registerBase("ElegantGirl", 1960, MagazineSubject.TEENS);
    public static final Magazine EVERYDAY_MOTORIST = Magazine.registerBase("EverydayMotorist", 1949, MagazineSubject.CARS);
    public static final Magazine EXECUTIVE_LOUNGE = Magazine.registerBase("ExecutiveLounge", 1957, MagazineSubject.RICH);
    public static final Magazine EXTINGUISH = Magazine.registerBase("Extinguish", 1991, new MagazineSubject[0]);
    public static final Magazine FAMILY_CARING = Magazine.registerBase("FamilyCaring", 1990, new MagazineSubject[0]);
    public static final Magazine FIGARY = Magazine.registerBase("Figary", 1990, MagazineSubject.POPULAR, MagazineSubject.HUMOR);
    public static final Magazine FINE = Magazine.registerBase("Fine", 1969, new MagazineSubject[0]);
    public static final Magazine FINE_WINE = Magazine.registerBase("FineWine", 1989, MagazineSubject.RICH);
    public static final Magazine FIRE_SAFETY_NEWS = Magazine.registerBase("FireSafetyNews", 1981, MagazineSubject.POLICE);
    public static final Magazine FLYING_SAUCER_JOURNAL = Magazine.registerBase("FlyingSaucerJournal", 1947, MagazineSubject.HORROR);
    public static final Magazine FOOTBALL_FRENZY = Magazine.registerBase("FootballFrenzy", 1983, MagazineSubject.SPORTS);
    public static final Magazine FORCES = Magazine.registerBase("Forces", 1968, new MagazineSubject[0]);
    public static final Magazine FORE = Magazine.registerBase("Fore", 1982, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine FOREVER_LIVING = Magazine.registerBase("ForeverLiving", 1989, MagazineSubject.HEALTH);
    public static final Magazine FOUL_INTENTIONS = Magazine.registerBase("FowlIntentions", 1958, MagazineSubject.OUTDOORS);
    public static final Magazine FREEWHEELIN = Magazine.registerBase("Freewheelin", 1986, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine FUTURE_PALEONTOLOGY = Magazine.registerBase("FuturePaleontology", 1987, MagazineSubject.CHILDS, MagazineSubject.SCIENCE);
    public static final Magazine GAME_Z = Magazine.registerBase("GameZ", 1989, MagazineSubject.CHILDS, MagazineSubject.TECH, MagazineSubject.TEENS, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine GHOSTLY_HAUNTINGS = Magazine.registerBase("GhostlyHauntings", 1991, MagazineSubject.HORROR);
    public static final Magazine GREENS = Magazine.registerBase("Greens", 1987, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine GRUESOME_CRIME_SCENES = Magazine.registerBase("GruesomeCrimeScenes", 1931, MagazineSubject.HORROR, MagazineSubject.CRIME);
    public static final Magazine HEY_SIR = Magazine.registerBase("HeySir", 1990, new MagazineSubject[0]);
    public static final Magazine HIGH_FLYER = Magazine.registerBase("HighFlyer", 1942, MagazineSubject.RICH);
    public static final Magazine HIKING_FOOTWEAR = Magazine.registerBase("HikingFootwear", 1987, MagazineSubject.OUTDOORS);
    public static final Magazine HOLLYWOOD_NEWS = Magazine.registerBase("HollywoodNews", 1962, MagazineSubject.CINEMA);
    public static final Magazine HOME_RUN = Magazine.registerBase("HomeRun", 1955, MagazineSubject.SPORTS);
    public static final Magazine HOSPITAL_TECHNOLOGY = Magazine.registerBase("HospitalTechnology", 1982, new MagazineSubject[0]);
    public static final Magazine HOUSEHOLD_FINANCES = Magazine.registerBase("HouseholdFinances", 1986, new MagazineSubject[0]);
    public static final Magazine IMPROVE_YOUR_HOUSE = Magazine.registerBase("ImproveYourHouse", 1980, MagazineSubject.POPULAR);
    public static final Magazine IN_FOCUS = Magazine.registerBase("InFocus", 1971, MagazineSubject.ART);
    public static final Magazine INCREDIBLE_HEROES = Magazine.registerBase("IncredibleHeroes", 1911, MagazineSubject.CHILDS, MagazineSubject.MILITARY);
    public static final Magazine INQUIRE = Magazine.registerBase("Inquire", 1933, MagazineSubject.POPULAR);
    public static final Magazine JOKES_JOKES = Magazine.registerBase("JokesJokes", 1967, MagazineSubject.HUMOR);
    public static final Magazine JUMP = Magazine.registerBase("Jump", 1979, MagazineSubject.TEENS);
    public static final Magazine KENTUCKY_DRIVER = Magazine.registerBase("KentuckyDriver", 1933, MagazineSubject.CARS);
    public static final Magazine KENTUCKY_HORSE_RACING = Magazine.registerBase("KentuckyHorseRacing", 1899, MagazineSubject.SPORTS);
    public static final Magazine KENTUCKY_LIBERAL = Magazine.registerBase("KentuckyLiberal", 1940, new MagazineSubject[0]);
    public static final Magazine KENTUCKY_OBSERVER = Magazine.registerBase("KentuckyObserver", 1959, MagazineSubject.POPULAR);
    public static final Magazine KIMS_TANKS_AND_ARMOR_MONTHLY = Magazine.registerBase("KimsTanksandArmorMonthly", 1956, MagazineSubject.MILITARY);
    public static final Magazine KIRRUS_COMPUTING = Magazine.registerBase("KirrusComputing", 1990, MagazineSubject.TECH);
    public static final Magazine KOOL_KIDS = Magazine.registerBase("KoolKids", 1989, MagazineSubject.CHILDS);
    public static final Magazine LABYRINTHS = Magazine.registerBase("Labyrinths", 1991, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine LATEST_FORENSCIS = Magazine.registerBase("LatestForensics", 1992, MagazineSubject.POLICE, MagazineSubject.CRIME);
    public static final Magazine LIFESTYLE = Magazine.registerBase("Lifestyle", 1990, MagazineSubject.POPULAR);
    public static final Magazine LOUISVILLE_BUSINESS_REVIEW = Magazine.registerBase("LouisvilleBusinessReview", 1919, MagazineSubject.BUSINESS);
    public static final Magazine LOUISVILLE_LAUGHTER = Magazine.registerBase("LouisvilleLaughter", 1939, MagazineSubject.HUMOR);
    public static final Magazine MAJESTIC_BIG_GAME = Magazine.registerBase("MajesticBigGame", 1923, MagazineSubject.OUTDOORS);
    public static final Magazine MALPRACTICE_INSURANCE_MONTHLY = Magazine.registerBase("MalpracticeInsuranceMonthly", 1990, MagazineSubject.RICH);
    public static final Magazine MANS_HEALTH = Magazine.registerBase("MansHealth", 1986, MagazineSubject.POPULAR, MagazineSubject.HEALTH);
    public static final Magazine MARKETS_MONTHLY = Magazine.registerBase("MarketsMonthly", 1958, MagazineSubject.BUSINESS);
    public static final Magazine ME = Magazine.registerBase("Me", 1989, new MagazineSubject[0]);
    public static final Magazine MERC = Magazine.registerBase("Merc", 1984, MagazineSubject.MILITARY);
    public static final Magazine MODERN_DANCE = Magazine.registerBase("ModernDance", 1990, MagazineSubject.ART);
    public static final Magazine MODERN_TRAINS = Magazine.registerBase("ModernTrains", 1925, new MagazineSubject[0]);
    public static final Magazine MONSTROUS_TRUCKS = Magazine.registerBase("MonstrousTrucks", 1975, MagazineSubject.CARS, MagazineSubject.TEENS);
    public static final Magazine MOSSY_ROCK = Magazine.registerBase("MossyRock", 1967, MagazineSubject.POPULAR, MagazineSubject.MUSIC);
    public static final Magazine MOTORCYCLE_ENTHUSIAST = Magazine.registerBase("MotorcycleEnthusiast", 1950, MagazineSubject.CARS);
    public static final Magazine MOVIES_WEEKLY = Magazine.registerBase("MoviesWeekly", 1934, MagazineSubject.CINEMA);
    public static final Magazine NATIONAL_CELEBS = Magazine.registerBase("NationalCelebs", 1948, MagazineSubject.POPULAR);
    public static final Magazine NATURE_FACTS = Magazine.registerBase("NatureFacts", 1930, MagazineSubject.CHILDS);
    public static final Magazine NEWSDAY = Magazine.registerBase("Newsday", 1989, MagazineSubject.POPULAR);
    public static final Magazine NEWTON = Magazine.registerBase("Newton", 1956, MagazineSubject.POPULAR, MagazineSubject.SCIENCE);
    public static final Magazine NUMISATICS = Magazine.registerBase("Numisatics", 1967, new MagazineSubject[0]);
    public static final Magazine OLD_WEST_LIFE = Magazine.registerBase("OldWestLife", 1911, new MagazineSubject[0]);
    public static final Magazine OPEN_MIND = Magazine.registerBase("OpenMind", 1953, MagazineSubject.SCIENCE);
    public static final Magazine OSCC_INSIDER = Magazine.registerBase("OSCCInsider", 1951, MagazineSubject.CARS, MagazineSubject.SPORTS);
    public static final Magazine PALE_GNOME = Magazine.registerBase("PaleGnome", 1990, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine PARENTAGE = Magazine.registerBase("Parentage", 1989, new MagazineSubject[0]);
    public static final Magazine PAWS = Magazine.registerBase("Paws", 1988, MagazineSubject.CHILDS);
    public static final Magazine PHILATELTY_LATE = Magazine.registerBase("PhilatelyLately", 1990, new MagazineSubject[0]);
    public static final Magazine PHOTOSPREAD = Magazine.registerBase("Photospread", 1986, MagazineSubject.ART);
    public static final Magazine PILED_DRIVER = Magazine.registerBase("Piledriver", 1989, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine POLICE_FILES = Magazine.registerBase("PoliceFiles", 1940, MagazineSubject.POLICE, MagazineSubject.CRIME);
    public static final Magazine PROLETARIAT = Magazine.registerBase("Proletariat", 1917, new MagazineSubject[0]);
    public static final Magazine READINGS_MONTHLY = Magazine.registerBase("ReadingsMonthly", 1920, MagazineSubject.POPULAR);
    public static final Magazine REAL_ESTATE_INVESTMENT = Magazine.registerBase("RealEstateInvestment", 1982, MagazineSubject.RICH, MagazineSubject.BUSINESS);
    public static final Magazine REELING_IN_AND_GEAR = Magazine.registerBase("ReelinginandGear", 1979, MagazineSubject.OUTDOORS);
    public static final Magazine REVELATIONS = Magazine.registerBase("Revelations", 1970, new MagazineSubject[0]);
    public static final Magazine ROCK_OUT = Magazine.registerBase("RockOut", 1980, MagazineSubject.MUSIC, MagazineSubject.TEENS);
    public static final Magazine RUNNING_LIFE = Magazine.registerBase("RunningLife", 1986, MagazineSubject.HEALTH);
    public static final Magazine RUNNING_N_GUNNING = Magazine.registerBase("RunningnGunning", 1978, MagazineSubject.FIREARM);
    public static final Magazine SCOPED = Magazine.registerBase("Scoped", 1977, MagazineSubject.FIREARM);
    public static final Magazine SCREEN_SHRIEK = Magazine.registerBase("ScreenShriek", 1963, MagazineSubject.HORROR);
    public static final Magazine SECOND_AMENDMENT = Magazine.registerBase("SecondAmendment", 1990, MagazineSubject.FIREARM);
    public static final Magazine SHORTS_ILLUSTRATED = Magazine.registerBase("ShortsIllustrated", 1954, MagazineSubject.FASHION);
    public static final Magazine SILVER_SCREEN = Magazine.registerBase("SilverScreen", 1937, MagazineSubject.CINEMA);
    public static final Magazine SIXTEEN = Magazine.registerBase("Sixteen", 1944, MagazineSubject.TEENS);
    public static final Magazine SKEPTICAL = Magazine.registerBase("Skeptical", 1976, MagazineSubject.SCIENCE);
    public static final Magazine SONG_AND_DANCE = Magazine.registerBase("SongandDance", 1921, MagazineSubject.MUSIC);
    public static final Magazine SORBES = Magazine.registerBase("Sorbes", 1916, MagazineSubject.BUSINESS);
    public static final Magazine SOUTHERN_LITERARY_REVIEW = Magazine.registerBase("SouthernLiteraryReview", 1911, new MagazineSubject[0]);
    public static final Magazine SPORTS_STATS = Magazine.registerBase("SportsStats", 1900, MagazineSubject.SPORTS);
    public static final Magazine SPRINTER = Magazine.registerBase("Sprinter", 1986, MagazineSubject.HEALTH);
    public static final Magazine ST_PATRICKS_JOURNAL = Magazine.registerBase("StPatricksJournal", 1903, new MagazineSubject[0]);
    public static final Magazine STAND_YOUR_GROUND = Magazine.registerBase("StandYourGround", 1988, MagazineSubject.FIREARM);
    public static final Magazine STOCK_TRENDS = Magazine.registerBase("StockTrends", 1983, MagazineSubject.BUSINESS);
    public static final Magazine STYLE = Magazine.registerBase("Style", 1957, MagazineSubject.FASHION);
    public static final Magazine STYLE_LIFE = Magazine.registerBase("StyleLife", 1963, new MagazineSubject[0]);
    public static final Magazine SUSPECTS_AND_WITNESSES = Magazine.registerBase("SuspectsandWitnesses", 1929, MagazineSubject.POLICE);
    public static final Magazine T_POWER = Magazine.registerBase("TPower", 1985, MagazineSubject.GOLF, MagazineSubject.RICH, MagazineSubject.SPORTS);
    public static final Magazine TAKE_A_LIFE = Magazine.registerBase("TakeaLife", 1990, MagazineSubject.CRIME);
    public static final Magazine TELLTALE = Magazine.registerBase("Telltale", 1989, MagazineSubject.POPULAR);
    public static final Magazine THE_BICLOPS = Magazine.registerBase("TheBiclops", 1984, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine THE_BIG_APPLE = Magazine.registerBase("TheBigApple", 1925, new MagazineSubject[0]);
    public static final Magazine THE_FOREIGN_REVIEW = Magazine.registerBase("TheForeignReview", 1914, new MagazineSubject[0]);
    public static final Magazine THE_NATIONALIST = Magazine.registerBase("TheNationalist", 1955, new MagazineSubject[0]);
    public static final Magazine THE_WALL_STREET_POST = Magazine.registerBase("TheWallStreetPost", 1923, MagazineSubject.BUSINESS);
    public static final Magazine THE_WORLD_WARS = Magazine.registerBase("TheWorldWars", 1948, MagazineSubject.MILITARY);
    public static final Magazine THRESHER = Magazine.registerBase("Thresher", 1966, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine TODAY_S_BUILDER = Magazine.registerBase("TodaysBuilder", 1979, new MagazineSubject[0]);
    public static final Magazine TRAILBEATERS = Magazine.registerBase("Trailbeaters", 1972, MagazineSubject.OUTDOORS);
    public static final Magazine TRUCKING_HEAVEN = Magazine.registerBase("TruckingHeaven", 1975, MagazineSubject.CARS);
    public static final Magazine TRUE_CRIME_STORIES = Magazine.registerBase("TrueCrimeStories", 1954, MagazineSubject.CRIME);
    public static final Magazine TRUE_SERIAL_KILLINGS = Magazine.registerBase("TrueSerialKillings", 1970, MagazineSubject.HORROR, MagazineSubject.CRIME);
    public static final Magazine TTRPG = Magazine.registerBase("TTRPG", 1987, MagazineSubject.HOBBY, MagazineSubject.GAMING);
    public static final Magazine TWITCHING_WEEKLY = Magazine.registerBase("TwitchingWeekly", 1944, MagazineSubject.HOBBY);
    public static final Magazine UNPOPULAR_ELECTRONICS = Magazine.registerBase("UnpopularElectronics", 1954, MagazineSubject.TECH, MagazineSubject.HOBBY);
    public static final Magazine UNSOLD_STORIES = Magazine.registerBase("UnsoldStories", 1989, new MagazineSubject[0]);
    public static final Magazine UPPER_CRUST = Magazine.registerBase("UpperCrust", 1987, MagazineSubject.RICH);
    public static final Magazine UPTO_ELEVEN = Magazine.registerBase("UptoEleven", 1988, MagazineSubject.MUSIC);
    public static final Magazine VETERAN = Magazine.registerBase("Veteran", 1970, MagazineSubject.MILITARY);
    public static final Magazine VHS_ENTHUSIAST = Magazine.registerBase("VHSEnthusiast", 1988, MagazineSubject.CINEMA);
    public static final Magazine VITIMANIA_RX = Magazine.registerBase("VitimaniaRx", 1966, MagazineSubject.HEALTH);
    public static final Magazine WATERCRAFT_AND_BIKES = Magazine.registerBase("WatercraftandBikes", 1984, MagazineSubject.CARS, MagazineSubject.OUTDOORS);
    public static final Magazine WORLD_GEOGRAPH = Magazine.registerBase("WorldGeograph", 1888, MagazineSubject.POPULAR);
    public static final Magazine WORLD_MUSIC = Magazine.registerBase("WorldMusic", 1909, MagazineSubject.MUSIC);
    public static final Magazine WORLD_TRADE = Magazine.registerBase("WorldTrade", 1920, MagazineSubject.POPULAR, MagazineSubject.BUSINESS);
    public static final Magazine WWC_INSIDER = Magazine.registerBase("WWCInsider", 1987, MagazineSubject.TEENS, MagazineSubject.SPORTS);
    public static final Magazine Y = Magazine.registerBase("Y", 1990, MagazineSubject.POPULAR);
    public static final Magazine YOUR_INVESTMENT_GUIDE = Magazine.registerBase("YourInvestmentGuide", 1986, MagazineSubject.BUSINESS);
    private final String translationKey;
    private final int firstYear;
    private final Set<MagazineSubject> subjects;

    private Magazine(String translationKey, int firstYear, Set<MagazineSubject> subjects) {
        this.translationKey = "IGUI_MagazineTitle_" + translationKey;
        this.firstYear = firstYear;
        this.subjects = subjects;
    }

    public static Magazine get(ResourceLocation id) {
        return Registries.MAGAZINE.get(id);
    }

    public static List<Magazine> getMagazineBySubject(InventoryItem item) {
        return item.getMagazineSubjects().stream().flatMap(subject -> BY_SUBJECTS.computeIfAbsent((MagazineSubject)subject, subject2 -> Registries.MAGAZINE.values().stream().filter(m -> m.subjects().contains(subject2)).toList()).stream()).collect(Collectors.toList());
    }

    public String translationKey() {
        return this.translationKey;
    }

    public int firstYear() {
        return this.firstYear;
    }

    public Set<MagazineSubject> subjects() {
        return this.subjects;
    }

    public static Magazine register(String id, int firstYear, MagazineSubject ... subjects) {
        return Magazine.register(false, id, new Magazine(id, firstYear, Set.of(subjects)));
    }

    private static Magazine registerBase(String id, int firstYear, MagazineSubject ... subjects) {
        return Magazine.register(true, id, new Magazine(id, firstYear, Set.of(subjects)));
    }

    private static Magazine register(boolean allowDefaultNamespace, String id, Magazine t) {
        return Registries.MAGAZINE.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (Magazine magazine : Registries.MAGAZINE) {
                TranslationKeyValidator.of(magazine.translationKey());
            }
        }
    }
}

