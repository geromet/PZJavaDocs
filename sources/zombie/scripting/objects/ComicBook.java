/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class ComicBook {
    public static final ComicBook HUNDRED_BILLION_BC = ComicBook.registerBase("100BillionBC", 201, true);
    public static final ComicBook ABSOLUTE_HOOEY = ComicBook.registerBase("AbsoluteHooey", 237, false);
    public static final ComicBook ATOM_LIZARD_AWAKES = ComicBook.registerBase("AtomLizardAwakes", 3, false);
    public static final ComicBook ATOM_LIZARD_AWAKES_AGAIN = ComicBook.registerBase("AtomLizardAwakesAgain", 6, false);
    public static final ComicBook ATOM_LIZARD_DESTROYS_CITIES = ComicBook.registerBase("AtomLizardDestroysCities", 9, false);
    public static final ComicBook ATOM_LIZARD_FAR_FUTURE_STORIES = ComicBook.registerBase("AtomLizardFarFutureStories", 12, true);
    public static final ComicBook ATOM_LIZARD_THE_DRAGON_KING_MEDIEVAL_MAYHEM = ComicBook.registerBase("AtomLizardTheDragonKingMedievalMayhem", 12, true);
    public static final ComicBook ATOMMAN_MINIATURE_MAYHEM = ComicBook.registerBase("AtommanMiniatureMayhem", 269, true);
    public static final ComicBook BLASTFORCE = ComicBook.registerBase("BlastForce", 169, false);
    public static final ComicBook BLASTFORCE_UNLEASHED = ComicBook.registerBase("BlastForceUnleashed", 59, true);
    public static final ComicBook BLINDMANS_BLUFF = ComicBook.registerBase("BlindmansBluff", 53, false);
    public static final ComicBook BLOODY_AXEOF_KARNTHE_SLAYER = ComicBook.registerBase("BloodyAxeofKarntheSlayer", 132, true);
    public static final ComicBook BRAT_PENS_HOUSEOF_IDEAS = ComicBook.registerBase("BratPensHouseofIdeas", 513, false);
    public static final ComicBook BRAVELY_INTO_PERIL = ComicBook.registerBase("BravelyIntoPeril", 174, false);
    public static final ComicBook BRICKLAYER = ComicBook.registerBase("Bricklayer", 187, false);
    public static final ComicBook BRIDEOF_THE_CACTUS = ComicBook.registerBase("BrideOfTheCactus", 13, false);
    public static final ComicBook BRUISER = ComicBook.registerBase("Bruiser", 216, false);
    public static final ComicBook BRUISERS = ComicBook.registerBase("Bruisers", 32, true);
    public static final ComicBook CACTUS_ETERNAL = ComicBook.registerBase("CactusETERNAL", 0, false);
    public static final ComicBook CAPTAIN_WOOF_CANINE_PILOT = ComicBook.registerBase("CaptainWoofCaninePilot", 362, true);
    public static final ComicBook CHILDDETECTIVE_TALES = ComicBook.registerBase("ChildDetectiveTales", 317, false);
    public static final ComicBook CHUCKLE = ComicBook.registerBase("Chuckle", 534, true);
    public static final ComicBook COMMANDO_RAID = ComicBook.registerBase("CommandoRaid", 212, false);
    public static final ComicBook CORPSEBOUND = ComicBook.registerBase("Corpsebound", 57, true);
    public static final ComicBook CRIMESOFTHE_CENTURY = ComicBook.registerBase("CrimesoftheCentury", 100, false);
    public static final ComicBook CRYPTOF_INSANITY = ComicBook.registerBase("CryptofInsanity", 135, false);
    public static final ComicBook CURSE_RYDER = ComicBook.registerBase("CurseRyder", 116, true);
    public static final ComicBook DAMSELS_DANGERS = ComicBook.registerBase("DamselsDangers", 72, true);
    public static final ComicBook DANGER_HORSE = ComicBook.registerBase("DangerHorse", 11, true);
    public static final ComicBook DARKDRIVETHE_SORCERER_CAR = ComicBook.registerBase("DarkdrivetheSorcererCar", 120, false);
    public static final ComicBook DEATHOFTHE_NIGHTPORTER = ComicBook.registerBase("DeathoftheNightporter", 0, false);
    public static final ComicBook DENSE_ALLOY = ComicBook.registerBase("DenseAlloy", 257, true);
    public static final ComicBook DINO_TRAPPER = ComicBook.registerBase("DinoTrapper", 193, false);
    public static final ComicBook DINO_TRAPPER_VSATOM_LIZARD = ComicBook.registerBase("DinoTrapperVSAtomLizard", 3, true);
    public static final ComicBook DR_APE_JR_GANG_EXPLORES_SPACE = ComicBook.registerBase("DrApeJrGangExploresSpace", 35, false);
    public static final ComicBook DR_APE_SCIENCE_VIGILANTE = ComicBook.registerBase("DrApeScienceVigilante", 358, false);
    public static final ComicBook DR_APE_BATTLES_ATOM_LIZARD = ComicBook.registerBase("DrApeBattlesAtomLizard", 2, false);
    public static final ComicBook DR_WEREWOLF = ComicBook.registerBase("DrWerewolf", 146, false);
    public static final ComicBook DR_WEREWOLF_DR_APE_TECHNOLOGY_HOSPITAL_SHOWDOWN = ComicBook.registerBase("DrWerewolfDrApeTechnologyHospitalShowdown", 0, true);
    public static final ComicBook DR_WEREWOLF_FULL_MOON = ComicBook.registerBase("DrWerewolfFullMoon", 0, true);
    public static final ComicBook DR_WEREWOLF_RETURNS = ComicBook.registerBase("DrWerewolfReturns", 26, true);
    public static final ComicBook DRACULA_HUNTERS = ComicBook.registerBase("DraculaHunters", 42, true);
    public static final ComicBook DRACULA_HUNTERS_AGAINST_ATOM_LIZARD = ComicBook.registerBase("DraculaHuntersAgainstAtomLizard", 0, true);
    public static final ComicBook DRACULA_HUNTERSVS_QUEEN_VAMPIRE = ComicBook.registerBase("DraculaHuntersvsQueenVampire", 2, false);
    public static final ComicBook DRAKE_STEELE_ESCAPE_FROM_DRACULA = ComicBook.registerBase("DrakeSteeleEscapeFromDracula", 3, false);
    public static final ComicBook DRAKE_STEELE_ESCAPE_FROM_THE_DRACULA_HUNTERS = ComicBook.registerBase("DrakeSteeleEscapeFromTheDraculaHunters", 2, false);
    public static final ComicBook DRAKE_STEELE_VAMPIRE_DETECTIVE = ComicBook.registerBase("DrakeSteeleVampireDetective", 168, true);
    public static final ComicBook ENCOUNTER_CRITICAL = ComicBook.registerBase("EncounterCritical", 17, false);
    public static final ComicBook ESCAPE_FROM_PERIL = ComicBook.registerBase("EscapeFromPeril", 5, false);
    public static final ComicBook FALLOFTHE_STEELMAN = ComicBook.registerBase("FalloftheSteelman", 0, false);
    public static final ComicBook FANTASIES_OF_POWER = ComicBook.registerBase("FantasiesOfPower", 5, false);
    public static final ComicBook FARRAGO = ComicBook.registerBase("Farrago", 127, true);
    public static final ComicBook FASTER_THAN_LIGHTSPEED = ComicBook.registerBase("FasterThanLightspeed", 37, true);
    public static final ComicBook FORBIDDEN_EXPERIMENTS = ComicBook.registerBase("ForbiddenExperiments", 157, false);
    public static final ComicBook FRANKEN_LAD = ComicBook.registerBase("FrankenLad", 53, false);
    public static final ComicBook FRANKEN_LAD_FRANKEN_MUTT = ComicBook.registerBase("FrankenLadFrankenMutt", 11, true);
    public static final ComicBook FREEDOM_ENFORCERS = ComicBook.registerBase("FreedomEnforcers", 53, true);
    public static final ComicBook FROM_OUTOFTHE_SHRIEKING_MORTUARY = ComicBook.registerBase("FromOutoftheShriekingMortuary", 382, false);
    public static final ComicBook FURIOUS_FIVEAND_SCORPMAN = ComicBook.registerBase("FuriousFiveandScorpman", 50, true);
    public static final ComicBook FURIOUS_FIVE_MELTDOWN = ComicBook.registerBase("FuriousFiveMeltdown", 6, false);
    public static final ComicBook FUTURELORDS = ComicBook.registerBase("Futurelords", 23, true);
    public static final ComicBook GAMMA_RADIATION_CHILD = ComicBook.registerBase("GammaRadiationChild", 173, false);
    public static final ComicBook GHOST_KIDS = ComicBook.registerBase("GhostKids", 428, false);
    public static final ComicBook GLADIATOR_ADVENTURES = ComicBook.registerBase("GladiatorAdventures", 195, false);
    public static final ComicBook GORE_RIDE = ComicBook.registerBase("GoreRide", 32, true);
    public static final ComicBook GORILLA_TRUCKER = ComicBook.registerBase("GorillaTrucker", 201, true);
    public static final ComicBook GORILLA_TRUCKERS = ComicBook.registerBase("GorillaTruckers", 84, true);
    public static final ComicBook GREASY_BIKER_STORIES = ComicBook.registerBase("GreasyBikerStories", 315, true);
    public static final ComicBook GREEN_HAWK = ComicBook.registerBase("GreenHawk", 294, true);
    public static final ComicBook GROSS_OUT = ComicBook.registerBase("GrossOut", 86, true);
    public static final ComicBook HE_FRANKENSTEIN = ComicBook.registerBase("HeFrankenstein", 26, false);
    public static final ComicBook HEAVY_WATER = ComicBook.registerBase("HeavyWater", 278, true);
    public static final ComicBook HISAYOSAN_DEATHIS_LIFE = ComicBook.registerBase("HisayosanDeathisLife", 58, false);
    public static final ComicBook HISTORY_UNBELIEVED = ComicBook.registerBase("HistoryUnbelieved", 284, true);
    public static final ComicBook HOLY_WITCH = ComicBook.registerBase("HolyWitch", 135, true);
    public static final ComicBook HORSESHOECRAB = ComicBook.registerBase("Horseshoecrab", 17, true);
    public static final ComicBook HUMANMAN = ComicBook.registerBase("Humanman", 78, false);
    public static final ComicBook HUMANMAN_MEETS_HUMANWOMAN = ComicBook.registerBase("HumanmanMeetsHumanwoman", 5, true);
    public static final ComicBook HUMANWOMAN = ComicBook.registerBase("Humanwoman", 315, false);
    public static final ComicBook ICEBLAZE = ComicBook.registerBase("Iceblaze", 65, true);
    public static final ComicBook ICKY_TALES = ComicBook.registerBase("IckyTales", 216, false);
    public static final ComicBook IMBROGLIO = ComicBook.registerBase("Imbroglio", 61, true);
    public static final ComicBook JACKIE_JAYE_TRUTHSEEKER = ComicBook.registerBase("JackieJayeTruthseeker", 3, false);
    public static final ComicBook JOHN_SPIRAL_NUCLEAR_KERFUFFLE = ComicBook.registerBase("JohnSpiralNuclearKerfuffle", 3, false);
    public static final ComicBook JOHN_SPIRAL_SPYAT_LARGE = ComicBook.registerBase("JohnSpiralSpyatLarge", 158, true);
    public static final ComicBook JOHN_SPIRAL_TORPEDO_TUBE_TALES = ComicBook.registerBase("JohnSpiralTorpedoTubeTales", 3, false);
    public static final ComicBook JUNGLE_ADVENTURE = ComicBook.registerBase("JungleAdventure", 236, false);
    public static final ComicBook KAZUSA = ComicBook.registerBase("Kazusa", 32, false);
    public static final ComicBook KENTUCKY_MUTANTS = ComicBook.registerBase("KentuckyMutants", 56, true);
    public static final ComicBook KING_REDWOOD = ComicBook.registerBase("KingRedwood", 144, false);
    public static final ComicBook KING_REDWOODVS_ATOM_LIZARD = ComicBook.registerBase("KingRedwoodvsAtomLizard", 0, false);
    public static final ComicBook KIT_SEQUOIAS_TWO_FISTS = ComicBook.registerBase("KitSequoiasTwoFists", 184, false);
    public static final ComicBook KIT_SEQUOIAS_MEETS_KING_REDWOOD = ComicBook.registerBase("KitSequoiasMeetsKingRedwood", 4, true);
    public static final ComicBook KARNTHE_DEFEATER = ComicBook.registerBase("KarntheDefeater", 267, true);
    public static final ComicBook KARN_RELENTLESS = ComicBook.registerBase("KarnRelentless", 53, false);
    public static final ComicBook KARN_THE_SAVAGE_SORCERER_SLAYING_SPECIAL = ComicBook.registerBase("KarnTheSavageSorcererSlayingSpecial", 6, false);
    public static final ComicBook KARNVS_MERLIN = ComicBook.registerBase("KarnvsMerlin", 0, false);
    public static final ComicBook LASSO_LADY = ComicBook.registerBase("LassoLady", 350, true);
    public static final ComicBook LASSO_LADYAND_NIGHTPORTER = ComicBook.registerBase("LassoLadyandNightporter", 121, true);
    public static final ComicBook LASSO_LADYAND_STEELMAN = ComicBook.registerBase("LassoLadyandSteelman", 133, true);
    public static final ComicBook LASSO_LADYVS_CTHULHU = ComicBook.registerBase("LassoLadyvsCthulhu", 0, false);
    public static final ComicBook LASSO_LADY_FIGHTTOTHE_DEATH = ComicBook.registerBase("LassoLadyFighttotheDeath", 12, false);
    public static final ComicBook MAIDEN_CANADA = ComicBook.registerBase("MaidenCanada", 235, true);
    public static final ComicBook MAYOR_ACADEMY = ComicBook.registerBase("MayorAcademy", 92, true);
    public static final ComicBook METAL_SPEAR_QUEST = ComicBook.registerBase("MetalSpearQuest", 108, true);
    public static final ComicBook MONSTER_PRESIDENTS = ComicBook.registerBase("MonsterPresidents", 7, true);
    public static final ComicBook MILLIPEDE_KID = ComicBook.registerBase("MillipedeKid", 345, false);
    public static final ComicBook NEEDLEFACE = ComicBook.registerBase("Needleface", 41, true);
    public static final ComicBook NIGHTPORTER_2020 = ComicBook.registerBase("Nightporter2020", 6, false);
    public static final ComicBook NIGHTPORTER_JR = ComicBook.registerBase("NightporterJr", 16, true);
    public static final ComicBook NIGHTPORTERVS_STEELMAN = ComicBook.registerBase("NightportervsSteelman", 3, false);
    public static final ComicBook NIGHTPORTER_YEAR_ZERO = ComicBook.registerBase("NightporterYearZero", 6, false);
    public static final ComicBook NINJA_ATTACK = ComicBook.registerBase("NinjaAttack", 87, true);
    public static final ComicBook OILY_REVENGER = ComicBook.registerBase("OilyRevenger", 131, true);
    public static final ComicBook OMEGA_DEPARTMENT_HAUNTED_HARGRAVE = ComicBook.registerBase("OmegaDepartmentHauntedHargrave", 3, false);
    public static final ComicBook OMEGA_DEPARTMENT_MYSTERIOUS_MANTELL = ComicBook.registerBase("OmegaDepartmentMysteriousMantell", 3, false);
    public static final ComicBook PEACEDRIVERS = ComicBook.registerBase("Peacedrivers", 25, true);
    public static final ComicBook THE_PINCUSHION = ComicBook.registerBase("ThePincushion", 213, false);
    public static final ComicBook PTIME_HUNTER = ComicBook.registerBase("PTimeHunter", 33, true);
    public static final ComicBook PLASMA_TEAM = ComicBook.registerBase("PlasmaTeam", 16, false);
    public static final ComicBook PLASMA_WOMAN = ComicBook.registerBase("PlasmaWoman", 139, true);
    public static final ComicBook PLUTO_MAN = ComicBook.registerBase("PlutoMan", 86, false);
    public static final ComicBook PROFESSOR_IDIOT = ComicBook.registerBase("ProfessorIdiot", 384, false);
    public static final ComicBook PROFESSOR_IDIOT_BUNGLES_AGAIN = ComicBook.registerBase("ProfessorIdiotBunglesAgain", 8, true);
    public static final ComicBook PUTRID = ComicBook.registerBase("PUTRID", 65, true);
    public static final ComicBook QUEEN_THUNORAOF_LEMURIA = ComicBook.registerBase("QueenThunoraofLemuria", 99, true);
    public static final ComicBook QUEEN_VAMPIRE = ComicBook.registerBase("QueenVampire", 167, false);
    public static final ComicBook REANIMATED_GREEN_BERETS = ComicBook.registerBase("ReanimatedGreenBerets", 18, true);
    public static final ComicBook RETURNOFTHE_NIGHTPORTER = ComicBook.registerBase("ReturnoftheNightporter", 3, false);
    public static final ComicBook REVENGEOFTHE_STOCKBROKER = ComicBook.registerBase("RevengeoftheStockbroker", 0, false);
    public static final ComicBook RIKOTO = ComicBook.registerBase("Rikoto", 0, false);
    public static final ComicBook RIPSCORPMAN = ComicBook.registerBase("RIPScorpman", 6, false);
    public static final ComicBook ROBOBATTLES = ComicBook.registerBase("Robobattles", 38, true);
    public static final ComicBook ROBOT_UNIT = ComicBook.registerBase("RobotUnit", 217, true);
    public static final ComicBook SANTAS_SECRET_ADVENTURES = ComicBook.registerBase("SantasSecretAdventures", 75, false);
    public static final ComicBook SAUCER_SORCERY = ComicBook.registerBase("SaucerSorcery", 0, false);
    public static final ComicBook SCORPMANAND_BLINDMAN = ComicBook.registerBase("ScorpmanandBlindman", 8, false);
    public static final ComicBook SCORPMANANDTHE_ZPEOPLE = ComicBook.registerBase("ScorpmanandtheZPeople", 23, true);
    public static final ComicBook SCORPMAN_DANGERVILLE = ComicBook.registerBase("ScorpmanDangerville", 6, false);
    public static final ComicBook SCORPMAN_THE_DEATHOF_DEE_DERRY = ComicBook.registerBase("ScorpmanTheDeathofDeeDerry", 3, false);
    public static final ComicBook SHE_FRANKENSTEIN = ComicBook.registerBase("SheFrankenstein", 301, true);
    public static final ComicBook SHE_FRANKENSTEIN_SHE_NEANDERTHAL_TEAM_UP = ComicBook.registerBase("SheFrankensteinSheNeanderthalTeamUp", 0, false);
    public static final ComicBook SHIKOKU_MONOGATARI = ComicBook.registerBase("ShikokuMonogatari", 11, false);
    public static final ComicBook SHOGGOTHS_ATTACK = ComicBook.registerBase("ShoggothsAttack", 0, false);
    public static final ComicBook SHOGGOTHS_ATTACK_THE_FINAL_WAR = ComicBook.registerBase("ShoggothsAttackTheFinalWar", 6, true);
    public static final ComicBook SHOGGOTHS_ATTACK_THE_SHOGGOTH_WAR = ComicBook.registerBase("ShoggothsAttackTheShoggothWar", 3, false);
    public static final ComicBook SHONEN = ComicBook.registerBase("Shonen", 153, false);
    public static final ComicBook SHOOTING_IRONS = ComicBook.registerBase("ShootingIrons", 138, false);
    public static final ComicBook SIDEKICK_ADVENTURES = ComicBook.registerBase("SidekickAdventures", 85, false);
    public static final ComicBook SIDEKICK_AVENGERS = ComicBook.registerBase("SidekickAvengers", 12, false);
    public static final ComicBook SIDEKICK_CEMETERY = ComicBook.registerBase("SidekickCemetery", 0, false);
    public static final ComicBook SOLAR_CALCULATOR = ComicBook.registerBase("SolarCalculator", 46, true);
    public static final ComicBook SOLDIERMAN = ComicBook.registerBase("Soldierman", 350, true);
    public static final ComicBook SPACE_WARLOCK = ComicBook.registerBase("SpaceWarlock", 139, true);
    public static final ComicBook STAR_AVENGERS = ComicBook.registerBase("StarAvengers", 0, false);
    public static final ComicBook STEELGRANNY_STORIES = ComicBook.registerBase("SteelgrannyStories", 50, false);
    public static final ComicBook STEELLADY = ComicBook.registerBase("Steellady", 200, true);
    public static final ComicBook STEELLADY_GOESTO_MARS = ComicBook.registerBase("SteelladyGoestoMars", 20, false);
    public static final ComicBook STEELMAN_RETURNS = ComicBook.registerBase("SteelmanReturns", 12, false);
    public static final ComicBook STEELMANVS_SCORPMANVS_THE_NIGHTPORTER = ComicBook.registerBase("SteelmanvsScorpmanvsTheNightporter", 3, false);
    public static final ComicBook STEELMANVS_THE_NIGHTPORTER = ComicBook.registerBase("SteelmanvsTheNightporter", 3, false);
    public static final ComicBook STEELMAN_FORGEDIN_FIRE = ComicBook.registerBase("SteelmanForgedinFire", 0, false);
    public static final ComicBook STEELMAN_THE_IMPOSSIBLE_SUN = ComicBook.registerBase("SteelmanTheImpossibleSun", 12, false);
    public static final ComicBook STEELWEDDING = ComicBook.registerBase("Steelwedding", 3, false);
    public static final ComicBook STIPULATORVS_TERMINATRIX = ComicBook.registerBase("StipulatorvsTerminatrix", 3, false);
    public static final ComicBook SWORDS_FIREBALLS = ComicBook.registerBase("SwordsFireballs", 83, true);
    public static final ComicBook THE_SWORDFIGHTING_NURSE = ComicBook.registerBase("TheSwordfightingNurse", 54, false);
    public static final ComicBook TALESOF_MISFORTUNE = ComicBook.registerBase("TalesofMisfortune", 184, false);
    public static final ComicBook TALESOF_THE_JEWEL_THRONE = ComicBook.registerBase("TalesofTheJewelThrone", 4, false);
    public static final ComicBook TANKTAUR = ComicBook.registerBase("Tanktaur", 25, true);
    public static final ComicBook TECHNOLOGY_HOSPITAL = ComicBook.registerBase("TechnologyHospital", 109, false);
    public static final ComicBook TEEN_STEELMAN = ComicBook.registerBase("TeenSteelman", 82, true);
    public static final ComicBook TEEN_SURGEONS = ComicBook.registerBase("TeenSurgeons", 158, false);
    public static final ComicBook TERMINATRIX_VSATOM_LIZARD = ComicBook.registerBase("TerminatrixVSAtomLizard", 0, false);
    public static final ComicBook THE_BEE_PEOPLE = ComicBook.registerBase("TheBeePeople", 0, false);
    public static final ComicBook THE_CACTUS = ComicBook.registerBase("TheCactus", 174, false);
    public static final ComicBook THE_CERES_INVASION = ComicBook.registerBase("TheCeresInvasion", 8, false);
    public static final ComicBook THE_CRYPTOF_PAIN = ComicBook.registerBase("TheCryptofPain", 294, false);
    public static final ComicBook THE_CURTAIN = ComicBook.registerBase("TheCurtain", 32, true);
    public static final ComicBook THE_CRIMEFIGHTING_DOGS = ComicBook.registerBase("TheCrimefightingDogs", 175, false);
    public static final ComicBook THE_DARKEST_NIGHTPORTER = ComicBook.registerBase("TheDarkestNightporter", 0, false);
    public static final ComicBook THE_FLUMMOXER = ComicBook.registerBase("TheFlummoxer", 53, true);
    public static final ComicBook THE_FOREMAN = ComicBook.registerBase("TheForeman", 165, false);
    public static final ComicBook THE_FURIOUS_FIVE = ComicBook.registerBase("TheFuriousFive", 184, true);
    public static final ComicBook THE_GHOST_BRAWLERS = ComicBook.registerBase("TheGhostBrawlers", 71, true);
    public static final ComicBook THE_HYENA = ComicBook.registerBase("TheHyena", 46, true);
    public static final ComicBook THE_IMPOSSIBLE_STEELMAN = ComicBook.registerBase("TheImpossibleSteelman", 398, true);
    public static final ComicBook THE_INCREDIBLE_SCORPMAN = ComicBook.registerBase("TheIncredibleScorpman", 206, true);
    public static final ComicBook THE_LEOPARD_TOOTHY_TALES = ComicBook.registerBase("TheLeopardToothyTales", 165, true);
    public static final ComicBook THE_MAGIC_HOODIE = ComicBook.registerBase("TheMagicHoodie", 0, false);
    public static final ComicBook THE_MODERATORS = ComicBook.registerBase("TheModerators", 165, true);
    public static final ComicBook THE_MODERATORS_BEYONDTHE_ICE_PALACE = ComicBook.registerBase("TheModeratorsBeyondtheIcePalace", 3, false);
    public static final ComicBook THE_MODERATORS_OLDWORLDS_NEW_WORLD = ComicBook.registerBase("TheModeratorsOldworldsNewWorld", 12, false);
    public static final ComicBook THE_NEW_RISQUE_ADVENTURESOF_MERLIN = ComicBook.registerBase("TheNewRisqueAdventuresofMerlin", 21, false);
    public static final ComicBook THE_NEW_SCORPMAN = ComicBook.registerBase("TheNewScorpman", 15, false);
    public static final ComicBook THE_OMEGA_DEPARTMENT_ARCHIVE = ComicBook.registerBase("TheOmegaDepartmentArchive", 78, true);
    public static final ComicBook THE_SCAVENGER = ComicBook.registerBase("TheScavenger", 150, true);
    public static final ComicBook THE_SEVEN_SUNS = ComicBook.registerBase("TheSevenSuns", 77, false);
    public static final ComicBook THE_SHORTCHANGER = ComicBook.registerBase("TheShortchanger", 37, true);
    public static final ComicBook THE_SPITFIGHTIN_DAMES = ComicBook.registerBase("TheSpitfightinDames", 184, true);
    public static final ComicBook THE_STIPULATOR = ComicBook.registerBase("TheStipulator", 130, true);
    public static final ComicBook THE_STOCKBROKER_DEAD_CAT_BOUNCE = ComicBook.registerBase("TheStockbrokerDeadCatBounce", 3, false);
    public static final ComicBook THE_STOCKBROKER_NEW_INVESTMENTS = ComicBook.registerBase("TheStockbrokerNewInvestments", 12, false);
    public static final ComicBook THE_TERMINATRIX = ComicBook.registerBase("TheTerminatrix", 83, true);
    public static final ComicBook THE_CHURL = ComicBook.registerBase("TheChurl", 30, false);
    public static final ComicBook THE_THOMPSONS_HERBS_NEW_CAR = ComicBook.registerBase("TheThompsonsHerbsNewCar", 0, false);
    public static final ComicBook THE_THOMPSONS_LESTER_GETS_IN_TROUBLE = ComicBook.registerBase("TheThompsonsLesterGetsInTrouble", 0, false);
    public static final ComicBook THE_THOMPSONS_MARIE_THE_TRAIN_DRIVER = ComicBook.registerBase("TheThompsonsMarieTheTrainDriver", 0, false);
    public static final ComicBook THE_UNCOUTH_SQUAD = ComicBook.registerBase("TheUncouthSquad", 40, true);
    public static final ComicBook THE_WARLOCKHAMMER_OF_TANGLEWOOD = ComicBook.registerBase("TheWarlockhammerOfTanglewood", 33, false);
    public static final ComicBook THRILLING_DINOSAUR_STORIES = ComicBook.registerBase("ThrillingDinosaurStories", 204, false);
    public static final ComicBook THUNORATHE_SHE_NEANDERTHAL = ComicBook.registerBase("ThunoratheSheNeanderthal", 267, false);
    public static final ComicBook TODDLER_POLICE = ComicBook.registerBase("ToddlerPolice", 38, true);
    public static final ComicBook TOILET_HUMOR = ComicBook.registerBase("ToiletHumor", 214, true);
    public static final ComicBook TRUE_HAUNTINGS = ComicBook.registerBase("TrueHauntings", 157, true);
    public static final ComicBook TRUE_MERCENARY = ComicBook.registerBase("TrueMercenary", 193, true);
    public static final ComicBook TRUE_NINJA_HISTORY = ComicBook.registerBase("TrueNinjaHistory", 45, false);
    public static final ComicBook ZPEOPLE = ComicBook.registerBase("Zpeople", 216, true);
    public static final ComicBook ZPEOPLE_ATTHE_NORTH_POLE = ComicBook.registerBase("ZpeopleAttheNorthPole", 6, false);
    public static final ComicBook ZPEOPLE_OUTOF_TIME = ComicBook.registerBase("ZpeopleOutofTime", 6, false);
    public static final ComicBook ZPEOPLE_THE_LEOPARD = ComicBook.registerBase("ZpeopleTheLeopard", 3, false);
    public static final ComicBook ZOINKS = ComicBook.registerBase("Zoinks", 315, true);
    public static final ComicBook ZOINKS_AFTER_HOURS_UNABASHED = ComicBook.registerBase("ZoinksAfterHoursUnabashed", 0, false);
    public static final ComicBook UMLAUT = ComicBook.registerBase("UMLAUT", 138, true);
    private final String translationKey;
    private final boolean inPrint;
    private final int issues;

    private ComicBook(String id, int issues, boolean inPrint) {
        this.translationKey = "IGUI_ComicTitle_" + id;
        this.inPrint = inPrint;
        this.issues = issues;
    }

    public static ComicBook get(ResourceLocation id) {
        return Registries.COMIC_BOOK.get(id);
    }

    public String toString() {
        return Registries.COMIC_BOOK.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public boolean isInPrint() {
        return this.inPrint;
    }

    public int getIssues() {
        return this.issues;
    }

    public static ComicBook register(String id, int issues, boolean inPrint) {
        return ComicBook.register(false, id, new ComicBook(id, issues, inPrint));
    }

    private static ComicBook registerBase(String id, int issues, boolean inPrint) {
        return ComicBook.register(true, id, new ComicBook(id, issues, inPrint));
    }

    private static ComicBook register(boolean allowDefaultNamespace, String id, ComicBook t) {
        return Registries.COMIC_BOOK.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (ComicBook comicBook : Registries.COMIC_BOOK) {
                TranslationKeyValidator.of(comicBook.getTranslationKey());
            }
        }
    }
}

