/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class DoodleKids {
    public static final DoodleKids A_CARTOON_CHARACTER = DoodleKids.registerBase("aCartoonCharacter");
    public static final DoodleKids A_FAMILY_IN_A_CAR = DoodleKids.registerBase("aFamilyinaCar");
    public static final DoodleKids A_FAMILY_IN_A_GARDEN = DoodleKids.registerBase("aFamilyinaGarden");
    public static final DoodleKids A_FAMILY_ON_THE_BEACH = DoodleKids.registerBase("aFamilyontheBeach");
    public static final DoodleKids A_FOREST = DoodleKids.registerBase("aForest");
    public static final DoodleKids A_FRIENDLY_ALIEN = DoodleKids.registerBase("aFriendlyAlien");
    public static final DoodleKids A_FRIENDLY_CREATURE = DoodleKids.registerBase("aFriendlyCreature");
    public static final DoodleKids A_GARDEN = DoodleKids.registerBase("aGarden");
    public static final DoodleKids A_HOUSE_WITH_A_FAMILY = DoodleKids.registerBase("aHousewithaFamily");
    public static final DoodleKids A_KITTEN = DoodleKids.registerBase("aKitten");
    public static final DoodleKids A_MAP = DoodleKids.registerBase("aMap");
    public static final DoodleKids A_NICE_DAY = DoodleKids.registerBase("aNiceDay");
    public static final DoodleKids A_NICE_TEACHER = DoodleKids.registerBase("aNiceTeacher");
    public static final DoodleKids A_PATTERN = DoodleKids.registerBase("aPattern");
    public static final DoodleKids A_PUPPY = DoodleKids.registerBase("aPuppy");
    public static final DoodleKids A_RAINBOW = DoodleKids.registerBase("aRainbow");
    public static final DoodleKids A_SCARY_ALIEN = DoodleKids.registerBase("aScaryAlien");
    public static final DoodleKids A_SCARY_MONSTER = DoodleKids.registerBase("aScaryMonster");
    public static final DoodleKids A_SMILING_FAMILY = DoodleKids.registerBase("aSmilingFamily");
    public static final DoodleKids A_SMILING_SUN = DoodleKids.registerBase("aSmilingSun");
    public static final DoodleKids A_STICK_FIGURE = DoodleKids.registerBase("aStickFigure");
    public static final DoodleKids A_TREE = DoodleKids.registerBase("aTree");
    public static final DoodleKids AN_ANGRY_TEACHER = DoodleKids.registerBase("anAngryTeacher");
    public static final DoodleKids AN_ODDLY_COLORED_SCENE = DoodleKids.registerBase("anOddlyColoredScene");
    public static final DoodleKids AN_ODDLY_PROPORTIONED_PERSON = DoodleKids.registerBase("anOddlyProportionedPerson");
    public static final DoodleKids BUTTERFLIES = DoodleKids.registerBase("Butterflies");
    public static final DoodleKids CHILDREN_PLAYING = DoodleKids.registerBase("ChildrenPlaying");
    public static final DoodleKids CHRISTMAS = DoodleKids.registerBase("Christmas");
    public static final DoodleKids DOODLE_OF_MAGICAL_WOODLAND = DoodleKids.registerBase("DoodleofMagicalWoodland");
    public static final DoodleKids DOODLE_OF_SPIFFO = DoodleKids.registerBase("DoodleofSpiffo");
    public static final DoodleKids FIREWORKS = DoodleKids.registerBase("Fireworks");
    public static final DoodleKids FLOWERS = DoodleKids.registerBase("Flowers");
    public static final DoodleKids GRANDPARENTS = DoodleKids.registerBase("Grandparents");
    public static final DoodleKids HEARTS = DoodleKids.registerBase("Hearts");
    public static final DoodleKids INSECTS = DoodleKids.registerBase("Insects");
    public static final DoodleKids PARENTS = DoodleKids.registerBase("Parents");
    public static final DoodleKids PEOPLE_CRYING = DoodleKids.registerBase("PeopleCrying");
    public static final DoodleKids PEOPLE_SMILING = DoodleKids.registerBase("PeopleSmiling");
    public static final DoodleKids PEOPLE_WALKING = DoodleKids.registerBase("PeopleWalking");
    public static final DoodleKids RANDOM_COLORS = DoodleKids.registerBase("RandomColors");
    public static final DoodleKids RANDOM_CRAYON_LINES = DoodleKids.registerBase("RandomCrayonLines");
    public static final DoodleKids RANDOM_LINES = DoodleKids.registerBase("RandomLines");
    public static final DoodleKids RANDOM_MARKER_LINES = DoodleKids.registerBase("RandomMarkerLines");
    public static final DoodleKids SOMEONE_CRYING = DoodleKids.registerBase("SomeoneCrying");
    public static final DoodleKids SOMETHING_INDISCERNIBLE = DoodleKids.registerBase("SomethingIndiscernible");
    public static final DoodleKids SQUIGGLY_LINES = DoodleKids.registerBase("SquigglyLines");
    public static final DoodleKids STICK_FIGURES = DoodleKids.registerBase("StickFigures");
    private final String translationKey;

    private DoodleKids(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static DoodleKids get(ResourceLocation id) {
        return Registries.DOODLE_KIDS.get(id);
    }

    public String toString() {
        return Registries.DOODLE_KIDS.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static DoodleKids register(String id) {
        return DoodleKids.register(false, id);
    }

    private static DoodleKids registerBase(String id) {
        return DoodleKids.register(true, id);
    }

    private static DoodleKids register(boolean allowDefaultNamespace, String id) {
        return Registries.DOODLE_KIDS.register(RegistryReset.createLocation(id, allowDefaultNamespace), new DoodleKids(id));
    }

    static {
        if (Core.IS_DEV) {
            for (DoodleKids doodleKids : Registries.DOODLE_KIDS) {
                TranslationKeyValidator.of(doodleKids.translationKey);
            }
        }
    }
}

