/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Job {
    public static final Job ACCOUNTANT = Job.registerBase("Accountant");
    public static final Job ACTOR = Job.registerBase("Actor");
    public static final Job ALARM_INSTALLER = Job.registerBase("AlarmInstaller");
    public static final Job ANIMAL_EXPERT = Job.registerBase("AnimalExpert");
    public static final Job ARCHITECT = Job.registerBase("Architect");
    public static final Job ARTIST = Job.registerBase("Artist");
    public static final Job BABYSITTER = Job.registerBase("Babysitter");
    public static final Job BARBER = Job.registerBase("Barber");
    public static final Job BODYGUARD = Job.registerBase("Bodyguard");
    public static final Job BUILDER = Job.registerBase("Builder");
    public static final Job BUSINESS_CARD_MAKER = Job.registerBase("BusinessCardMaker");
    public static final Job BUSINESS_CONSULTANT = Job.registerBase("BusinessConsultant");
    public static final Job BUSINESS_OWNER = Job.registerBase("BusinessOwner");
    public static final Job BUTCHER = Job.registerBase("Butcher");
    public static final Job CAR_SALESPERSON = Job.registerBase("CarSalesperson");
    public static final Job CARPENTER = Job.registerBase("Carpenter");
    public static final Job CLEANER = Job.registerBase("Cleaner");
    public static final Job CLOTHING_DESIGNER = Job.registerBase("ClothingDesigner");
    public static final Job CLOWN = Job.registerBase("Clown");
    public static final Job CODER = Job.registerBase("Coder");
    public static final Job COOK = Job.registerBase("Cook");
    public static final Job CULT_DEPROGRAMMER = Job.registerBase("CultDeprogrammer");
    public static final Job DANCER = Job.registerBase("Dancer");
    public static final Job DENTIST = Job.registerBase("Dentist");
    public static final Job DERMATOLOGIST = Job.registerBase("Dermatologist");
    public static final Job DIETICIAN = Job.registerBase("Dietician");
    public static final Job DIY = Job.registerBase("DIY");
    public static final Job DOCTOR = Job.registerBase("Doctor");
    public static final Job DRAFTER = Job.registerBase("Drafter");
    public static final Job DRIVER = Job.registerBase("Driver");
    public static final Job DRY_CLEANER = Job.registerBase("DryCleaner");
    public static final Job EFFICIENCY_EXPERT = Job.registerBase("EfficiencyExpert");
    public static final Job ELECTRICIAN = Job.registerBase("Electrician");
    public static final Job ENGINEER = Job.registerBase("Engineer");
    public static final Job ESCORT = Job.registerBase("Escort");
    public static final Job EXORCIST = Job.registerBase("Exorcist");
    public static final Job EXOTIC_DANCER = Job.registerBase("ExoticDancer");
    public static final Job EXTERMINATOR = Job.registerBase("Exterminator");
    public static final Job FACTORY_MANAGER = Job.registerBase("FactoryManager");
    public static final Job FENCER = Job.registerBase("Fencer");
    public static final Job FILM_TV_CREW = Job.registerBase("Film/TVCrew");
    public static final Job FINANCIAL_ADVISOR = Job.registerBase("FinancialAdvisor");
    public static final Job FITNESS_INSTRUCTOR = Job.registerBase("FitnessInstructor");
    public static final Job FLOORER = Job.registerBase("Floorer");
    public static final Job FORTUNE_TELLER = Job.registerBase("FortuneTeller");
    public static final Job FRAMER = Job.registerBase("Framer");
    public static final Job GARDENER = Job.registerBase("Gardener");
    public static final Job GENERAL_MANAGER = Job.registerBase("GeneralManager");
    public static final Job GRAPHIC_DESIGNER = Job.registerBase("GraphicDesigner");
    public static final Job HAIRDRESSER = Job.registerBase("Hairdresser");
    public static final Job HEAD_CHEF = Job.registerBase("HeadChef");
    public static final Job HISTORIAN = Job.registerBase("Historian");
    public static final Job HUMOROUS_FAKE_OCCUPATION_NAME = Job.registerBase("HumorousFakeOccupationName");
    public static final Job HUNTER = Job.registerBase("Hunter");
    public static final Job INSURANCE_AGENT = Job.registerBase("InsuranceAgent");
    public static final Job INTIMATE_DISEASE_SPECIALIST = Job.registerBase("IntimateDiseaseSpecialist");
    public static final Job IT_TECHNICIAN = Job.registerBase("ITTechnician");
    public static final Job JACK_OF_ALL_TRADES = Job.registerBase("JackofallTrades");
    public static final Job JOURNALIST = Job.registerBase("Journalist");
    public static final Job LABORER = Job.registerBase("Laborer");
    public static final Job LAWYER = Job.registerBase("Lawyer");
    public static final Job LECTURER = Job.registerBase("Lecturer");
    public static final Job LOCAL_HISTORY_EXPERT = Job.registerBase("LocalHistoryExpert");
    public static final Job LOCAL_POLITICIAN = Job.registerBase("LocalPolitician");
    public static final Job LOCKSMITH = Job.registerBase("Locksmith");
    public static final Job LOGGER = Job.registerBase("Logger");
    public static final Job LOGISTICS_EXPERT = Job.registerBase("LogisticsExpert");
    public static final Job MACHINE_OPERATOR = Job.registerBase("MachineOperator");
    public static final Job MAKEUP_ARTIST = Job.registerBase("MakeupArtist");
    public static final Job MASSEUSE = Job.registerBase("Masseuse");
    public static final Job MECHANIC = Job.registerBase("Mechanic");
    public static final Job METALWORKER = Job.registerBase("Metalworker");
    public static final Job MIDWIFE = Job.registerBase("Midwife");
    public static final Job NANNY = Job.registerBase("Nanny");
    public static final Job NURSE = Job.registerBase("Nurse");
    public static final Job OPTICIAN = Job.registerBase("Optician");
    public static final Job ORTHODONTIST = Job.registerBase("Orthodontist");
    public static final Job PAINTER = Job.registerBase("Painter");
    public static final Job PEDIATRICIAN = Job.registerBase("Pediatrician");
    public static final Job PERSONAL_TRAINER = Job.registerBase("PersonalTrainer");
    public static final Job PHARMACIST = Job.registerBase("Pharmacist");
    public static final Job PHOTOGRAPHER = Job.registerBase("Photographer");
    public static final Job PHYSIOTHERAPIST = Job.registerBase("Physiotherapist");
    public static final Job PILOT = Job.registerBase("Pilot");
    public static final Job PLUMBER = Job.registerBase("Plumber");
    public static final Job PRIVATE_INVESTIGATOR = Job.registerBase("PrivateInvestigator");
    public static final Job PRODUCER = Job.registerBase("Producer");
    public static final Job PSYCHIATRIST = Job.registerBase("Psychiatrist");
    public static final Job PSYCHIC = Job.registerBase("Psychic");
    public static final Job PUBLISHER = Job.registerBase("Publisher");
    public static final Job REAL_ESTATE_AGENT = Job.registerBase("RealEstateAgent");
    public static final Job REHAB = Job.registerBase("Rehab");
    public static final Job REPAIRMAN = Job.registerBase("Repairman");
    public static final Job SAILOR = Job.registerBase("Sailor");
    public static final Job SALESPERSON = Job.registerBase("Salesperson");
    public static final Job SCIENTIST = Job.registerBase("Scientist");
    public static final Job SCRAPYARD_WORKER = Job.registerBase("ScrapyardWorker");
    public static final Job SECRETARY = Job.registerBase("Secretary");
    public static final Job SECURITY_GUARD = Job.registerBase("SecurityGuard");
    public static final Job SINGER = Job.registerBase("Singer");
    public static final Job STOCK_MARKET_EXPERT = Job.registerBase("StockMarketExpert");
    public static final Job STONEMASON = Job.registerBase("Stonemason");
    public static final Job TAILOR = Job.registerBase("Tailor");
    public static final Job TAX_EXPERT = Job.registerBase("TaxExpert");
    public static final Job TAXI_DRIVER = Job.registerBase("TaxiDriver");
    public static final Job TEACHER = Job.registerBase("Teacher");
    public static final Job TECHNICIAN = Job.registerBase("Technician");
    public static final Job TOUR_GUIDE = Job.registerBase("TourGuide");
    public static final Job TRAVEL_AGENT = Job.registerBase("TravelAgent");
    public static final Job TUTOR = Job.registerBase("Tutor");
    public static final Job UNDERTAKER = Job.registerBase("Undertaker");
    public static final Job VETERINARIAN = Job.registerBase("Veterinarian");
    public static final Job WELDER = Job.registerBase("Welder");
    public static final Job WINDOW_FITTER = Job.registerBase("WindowFitter");
    public static final Job WRITER = Job.registerBase("Writer");
    private final String translationKey;

    private Job(String translationKey) {
        this.translationKey = "IGUI_" + translationKey;
    }

    public static Job get(ResourceLocation id) {
        return Registries.JOB.get(id);
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static Job register(String id) {
        return Job.register(false, id);
    }

    private static Job registerBase(String id) {
        return Job.register(true, id);
    }

    private static Job register(boolean allowDefaultNamespace, String id) {
        return Registries.JOB.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Job(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Job job : Registries.JOB) {
                TranslationKeyValidator.of(job.translationKey());
            }
        }
    }
}

