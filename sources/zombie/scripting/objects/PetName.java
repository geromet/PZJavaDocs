/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class PetName {
    public static final PetName ACE = PetName.registerBase("Ace");
    public static final PetName ACORN = PetName.registerBase("Acorn");
    public static final PetName AMERICA = PetName.registerBase("America");
    public static final PetName ARCHIE = PetName.registerBase("Archie");
    public static final PetName AVOCADO = PetName.registerBase("Avocado");
    public static final PetName BABY = PetName.registerBase("Baby");
    public static final PetName BACON = PetName.registerBase("Bacon");
    public static final PetName BADGER = PetName.registerBase("Badger");
    public static final PetName BAGEL = PetName.registerBase("Bagel");
    public static final PetName BAILEY = PetName.registerBase("Bailey");
    public static final PetName BANDIT = PetName.registerBase("Bandit");
    public static final PetName BEANIE = PetName.registerBase("Beanie");
    public static final PetName BEANS = PetName.registerBase("Beans");
    public static final PetName BELLA = PetName.registerBase("Bella");
    public static final PetName BELLE = PetName.registerBase("Belle");
    public static final PetName BEN = PetName.registerBase("Ben");
    public static final PetName BERSERKER = PetName.registerBase("Berserker");
    public static final PetName BERT = PetName.registerBase("Bert");
    public static final PetName BESS = PetName.registerBase("Bess");
    public static final PetName BISCUIT = PetName.registerBase("Biscuit");
    public static final PetName BLONDIE = PetName.registerBase("Blondie");
    public static final PetName BLOSSOM = PetName.registerBase("Blossom");
    public static final PetName BORIS = PetName.registerBase("Boris");
    public static final PetName BOXER = PetName.registerBase("Boxer");
    public static final PetName BRANDY = PetName.registerBase("Brandy");
    public static final PetName BRUCE = PetName.registerBase("Bruce");
    public static final PetName BRUNO = PetName.registerBase("Bruno");
    public static final PetName BUBBLE = PetName.registerBase("Bubble");
    public static final PetName BUBBLES = PetName.registerBase("Bubbles");
    public static final PetName BUCK = PetName.registerBase("Buck");
    public static final PetName BUCKSHOT = PetName.registerBase("Buckshot");
    public static final PetName BUD = PetName.registerBase("Bud");
    public static final PetName BUDDY = PetName.registerBase("Buddy");
    public static final PetName BULLET = PetName.registerBase("Bullet");
    public static final PetName BUTTERCUP = PetName.registerBase("Buttercup");
    public static final PetName CALLY = PetName.registerBase("Cally");
    public static final PetName CHAPLIN = PetName.registerBase("Chaplin");
    public static final PetName CHARLIE = PetName.registerBase("Charlie");
    public static final PetName CHIEF = PetName.registerBase("Chief");
    public static final PetName CHOCOLATE = PetName.registerBase("Chocolate");
    public static final PetName CHOPPER = PetName.registerBase("Chopper");
    public static final PetName CHRONOS = PetName.registerBase("Chronos");
    public static final PetName CLAUDE = PetName.registerBase("Claude");
    public static final PetName CLEAVER = PetName.registerBase("Cleaver");
    public static final PetName CLOUD = PetName.registerBase("Cloud");
    public static final PetName CLOVER = PetName.registerBase("Clover");
    public static final PetName COCO = PetName.registerBase("Coco");
    public static final PetName COFFEE = PetName.registerBase("Coffee");
    public static final PetName COOKIE = PetName.registerBase("Cookie");
    public static final PetName COOPER = PetName.registerBase("Cooper");
    public static final PetName COPPER = PetName.registerBase("Copper");
    public static final PetName CROCKETT = PetName.registerBase("Crockett");
    public static final PetName CUPCAKE = PetName.registerBase("Cupcake");
    public static final PetName DAISY = PetName.registerBase("Daisy");
    public static final PetName DAKOTA = PetName.registerBase("Dakota");
    public static final PetName DOCTOR = PetName.registerBase("Doctor");
    public static final PetName DOT = PetName.registerBase("Dot");
    public static final PetName DUDE = PetName.registerBase("Dude");
    public static final PetName DUKE = PetName.registerBase("Duke");
    public static final PetName DYLAN = PetName.registerBase("Dylan");
    public static final PetName ED = PetName.registerBase("Ed");
    public static final PetName ELLE = PetName.registerBase("Elle");
    public static final PetName FIFI = PetName.registerBase("Fifi");
    public static final PetName FLORA = PetName.registerBase("Flora");
    public static final PetName FLUFFY = PetName.registerBase("Fluffy");
    public static final PetName FLUFFYFOOT = PetName.registerBase("Fluffyfoot");
    public static final PetName FREDDY = PetName.registerBase("Freddy");
    public static final PetName FREEDOM = PetName.registerBase("Freedom");
    public static final PetName FROSTY = PetName.registerBase("Frosty");
    public static final PetName FUDGE = PetName.registerBase("Fudge");
    public static final PetName FURBERT = PetName.registerBase("Furbert");
    public static final PetName GINGER = PetName.registerBase("Ginger");
    public static final PetName GOBLIN = PetName.registerBase("Goblin");
    public static final PetName GOLDIE = PetName.registerBase("Goldie");
    public static final PetName GRAVY = PetName.registerBase("Gravy");
    public static final PetName GRIFFIN = PetName.registerBase("Griffin");
    public static final PetName GUNNER = PetName.registerBase("Gunner");
    public static final PetName HARGRAVE = PetName.registerBase("Hargrave");
    public static final PetName HARRY = PetName.registerBase("Harry");
    public static final PetName HAZEL = PetName.registerBase("Hazel");
    public static final PetName HERB = PetName.registerBase("Herb");
    public static final PetName HOLLY = PetName.registerBase("Holly");
    public static final PetName HONEY = PetName.registerBase("Honey");
    public static final PetName JACK = PetName.registerBase("Jack");
    public static final PetName JACQUES = PetName.registerBase("Jacques");
    public static final PetName JAY = PetName.registerBase("Jay");
    public static final PetName JENNY = PetName.registerBase("Jenny");
    public static final PetName JILL = PetName.registerBase("Jill");
    public static final PetName JILLY = PetName.registerBase("Jilly");
    public static final PetName JOKER = PetName.registerBase("Joker");
    public static final PetName JOSH = PetName.registerBase("Josh");
    public static final PetName JOSHIE = PetName.registerBase("Joshie");
    public static final PetName JOSS = PetName.registerBase("Joss");
    public static final PetName JULIET = PetName.registerBase("Juliet");
    public static final PetName KAI = PetName.registerBase("Kai");
    public static final PetName KATANA = PetName.registerBase("Katana");
    public static final PetName KATJA = PetName.registerBase("Katja");
    public static final PetName KENTUCKY = PetName.registerBase("Kentucky");
    public static final PetName LADDIE = PetName.registerBase("Laddie");
    public static final PetName LADY = PetName.registerBase("Lady");
    public static final PetName LARRY = PetName.registerBase("Larry");
    public static final PetName LASER = PetName.registerBase("Laser");
    public static final PetName LAVENDER = PetName.registerBase("Lavender");
    public static final PetName LAZ = PetName.registerBase("Laz");
    public static final PetName LESTER = PetName.registerBase("Lester");
    public static final PetName LIBERTY = PetName.registerBase("Liberty");
    public static final PetName LILY = PetName.registerBase("Lily");
    public static final PetName LINCOLN = PetName.registerBase("Lincoln");
    public static final PetName LORD_PUDDINGTON = PetName.registerBase("Lord Puddington");
    public static final PetName LOUIS = PetName.registerBase("Louis");
    public static final PetName LOUISE = PetName.registerBase("Louise");
    public static final PetName LOVELY = PetName.registerBase("Lovely");
    public static final PetName LUCY = PetName.registerBase("Lucy");
    public static final PetName LULU = PetName.registerBase("Lulu");
    public static final PetName LUNA = PetName.registerBase("Luna");
    public static final PetName MACHETE = PetName.registerBase("Machete");
    public static final PetName MADAME = PetName.registerBase("Madame");
    public static final PetName MAGNUM = PetName.registerBase("Magnum");
    public static final PetName MANGO = PetName.registerBase("Mango");
    public static final PetName MANTELL = PetName.registerBase("Mantell");
    public static final PetName MARGE = PetName.registerBase("Marge");
    public static final PetName MARIA = PetName.registerBase("Maria");
    public static final PetName MAULER = PetName.registerBase("Mauler");
    public static final PetName MAX = PetName.registerBase("Max");
    public static final PetName MAYO = PetName.registerBase("Mayo");
    public static final PetName MILKSHAKE = PetName.registerBase("Milkshake");
    public static final PetName MISTER = PetName.registerBase("Mister");
    public static final PetName MISTY = PetName.registerBase("Misty");
    public static final PetName MOLLY = PetName.registerBase("Molly");
    public static final PetName MOON = PetName.registerBase("Moon");
    public static final PetName MOSS = PetName.registerBase("Moss");
    public static final PetName MR_WAFFLES = PetName.registerBase("Mr Waffles");
    public static final PetName MUFFIN = PetName.registerBase("Muffin");
    public static final PetName NICKI = PetName.registerBase("Nicki");
    public static final PetName NIKO = PetName.registerBase("Niko");
    public static final PetName NUGGET = PetName.registerBase("Nugget");
    public static final PetName ODIN = PetName.registerBase("Odin");
    public static final PetName ORCA = PetName.registerBase("Orca");
    public static final PetName ORCHID = PetName.registerBase("Orchid");
    public static final PetName OSCAR = PetName.registerBase("Oscar");
    public static final PetName PANCAKE = PetName.registerBase("Pancake");
    public static final PetName PANCHO = PetName.registerBase("Pancho");
    public static final PetName PATRIOT = PetName.registerBase("Patriot");
    public static final PetName PENNY = PetName.registerBase("Penny");
    public static final PetName PEPPER = PetName.registerBase("Pepper");
    public static final PetName PIKE = PetName.registerBase("Pike");
    public static final PetName PIPSQUEAK = PetName.registerBase("Pipsqueak");
    public static final PetName PISTOL = PetName.registerBase("Pistol");
    public static final PetName PLONKIE = PetName.registerBase("Plonkie");
    public static final PetName POINTER = PetName.registerBase("Pointer");
    public static final PetName POLLY = PetName.registerBase("Polly");
    public static final PetName POPPY = PetName.registerBase("Poppy");
    public static final PetName PRIMROSE = PetName.registerBase("Primrose");
    public static final PetName PRINCE = PetName.registerBase("Prince");
    public static final PetName PRINCESS = PetName.registerBase("Princess");
    public static final PetName PUDDING = PetName.registerBase("Pudding");
    public static final PetName PUMPKIN = PetName.registerBase("Pumpkin");
    public static final PetName PUPPERS = PetName.registerBase("Puppers");
    public static final PetName RADA = PetName.registerBase("Rada");
    public static final PetName RAINBOW = PetName.registerBase("Rainbow");
    public static final PetName RANGER = PetName.registerBase("Ranger");
    public static final PetName RASPBERRY = PetName.registerBase("Raspberry");
    public static final PetName REVOLVER = PetName.registerBase("Revolver");
    public static final PetName REX = PetName.registerBase("Rex");
    public static final PetName RIVER = PetName.registerBase("River");
    public static final PetName ROCKY = PetName.registerBase("Rocky");
    public static final PetName RODNEY = PetName.registerBase("Rodney");
    public static final PetName ROMAN = PetName.registerBase("Roman");
    public static final PetName ROMEO = PetName.registerBase("Romeo");
    public static final PetName ROOSEVELT = PetName.registerBase("Roosevelt");
    public static final PetName ROSEMARY = PetName.registerBase("Rosemary");
    public static final PetName ROSIE = PetName.registerBase("Rosie");
    public static final PetName ROVER = PetName.registerBase("Rover");
    public static final PetName ROY = PetName.registerBase("Roy");
    public static final PetName RUA = PetName.registerBase("Rua");
    public static final PetName RUBY = PetName.registerBase("Ruby");
    public static final PetName RUCA = PetName.registerBase("Ruca");
    public static final PetName SAGE = PetName.registerBase("Sage");
    public static final PetName SALLY = PetName.registerBase("Sally");
    public static final PetName SAM = PetName.registerBase("Sam");
    public static final PetName SAMMY = PetName.registerBase("Sammy");
    public static final PetName SANDY = PetName.registerBase("Sandy");
    public static final PetName SANTA = PetName.registerBase("Santa");
    public static final PetName SCOTT = PetName.registerBase("Scott");
    public static final PetName SCOUT = PetName.registerBase("Scout");
    public static final PetName SHADOW = PetName.registerBase("Shadow");
    public static final PetName SHEP = PetName.registerBase("Shep");
    public static final PetName SID = PetName.registerBase("Sid");
    public static final PetName SILVER = PetName.registerBase("Silver");
    public static final PetName SIREN = PetName.registerBase("Siren");
    public static final PetName SNIPER = PetName.registerBase("Sniper");
    public static final PetName SNOWIE = PetName.registerBase("Snowie");
    public static final PetName SOLDIER = PetName.registerBase("Soldier");
    public static final PetName SPARKY = PetName.registerBase("Sparky");
    public static final PetName SPIFFO = PetName.registerBase("Spiffo");
    public static final PetName SPIRAL = PetName.registerBase("Spiral");
    public static final PetName SPOT = PetName.registerBase("Spot");
    public static final PetName SQUEAK = PetName.registerBase("Squeak");
    public static final PetName STRAWBERRY = PetName.registerBase("Strawberry");
    public static final PetName SUGAR = PetName.registerBase("Sugar");
    public static final PetName SWEETIE = PetName.registerBase("Sweetie");
    public static final PetName TAMMY = PetName.registerBase("Tammy");
    public static final PetName TED = PetName.registerBase("Ted");
    public static final PetName TEDDY = PetName.registerBase("Teddy");
    public static final PetName TERRY = PetName.registerBase("Terry");
    public static final PetName THOR = PetName.registerBase("Thor");
    public static final PetName TIA = PetName.registerBase("Tia");
    public static final PetName TIGER = PetName.registerBase("Tiger");
    public static final PetName TINKLER = PetName.registerBase("Tinkler");
    public static final PetName TOBY = PetName.registerBase("Toby");
    public static final PetName TOFFEE = PetName.registerBase("Toffee");
    public static final PetName TOMMY = PetName.registerBase("Tommy");
    public static final PetName TRIXIE = PetName.registerBase("Trixie");
    public static final PetName TWINKLE = PetName.registerBase("Twinkle");
    public static final PetName VIC = PetName.registerBase("Vic");
    public static final PetName VIOLET = PetName.registerBase("Violet");
    public static final PetName WALLY = PetName.registerBase("Wally");
    public static final PetName WASHINGTON = PetName.registerBase("Washington");
    public static final PetName WATERFALL = PetName.registerBase("Waterfall");
    public static final PetName WHIMBLY = PetName.registerBase("Whimbly");
    public static final PetName WHISKEY = PetName.registerBase("Whiskey");
    public static final PetName WILLOW = PetName.registerBase("Willow");
    public static final PetName WOODY = PetName.registerBase("Woody");
    public static final PetName YORKIE = PetName.registerBase("Yorkie");
    public static final PetName ZUKO = PetName.registerBase("Zuko");
    private final String translationKey;

    private PetName(String id) {
        this.translationKey = "IGUI_PetName_" + id;
    }

    public static PetName get(ResourceLocation id) {
        return Registries.PET_NAME.get(id);
    }

    public String toString() {
        return Registries.PET_NAME.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static PetName register(String id) {
        return PetName.register(false, id);
    }

    private static PetName registerBase(String id) {
        return PetName.register(true, id);
    }

    private static PetName register(boolean allowDefaultNamespace, String id) {
        return Registries.PET_NAME.register(RegistryReset.createLocation(id, allowDefaultNamespace), new PetName(id));
    }

    static {
        if (Core.IS_DEV) {
            for (PetName petName : Registries.PET_NAME) {
                TranslationKeyValidator.of(petName.getTranslationKey());
            }
        }
    }
}

