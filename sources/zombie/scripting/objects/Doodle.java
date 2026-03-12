/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Doodle {
    public static final Doodle A_BATTLE = Doodle.registerBase("aBattle");
    public static final Doodle A_CARTOON = Doodle.registerBase("aCartoon");
    public static final Doodle A_CARTOON_CHARACTER = Doodle.registerBase("aCartoonCharacter");
    public static final Doodle A_COUPLE = Doodle.registerBase("aCouple");
    public static final Doodle A_CUTE_ANIMAL = Doodle.registerBase("aCuteAnimal");
    public static final Doodle A_DEAD_STICK_FIGURE = Doodle.registerBase("aDeadStickFigure");
    public static final Doodle A_FACE = Doodle.registerBase("aFace");
    public static final Doodle A_FAMOUS_PERSON = Doodle.registerBase("aFamousPerson");
    public static final Doodle A_FUNNY_SCENE = Doodle.registerBase("aFunnyScene");
    public static final Doodle A_GARDEN = Doodle.registerBase("aGarden");
    public static final Doodle A_HANDSOME_MAN = Doodle.registerBase("aHandsomeMan");
    public static final Doodle A_HOUSE = Doodle.registerBase("aHouse");
    public static final Doodle A_LANDMARK = Doodle.registerBase("aLandmark");
    public static final Doodle A_LOGO = Doodle.registerBase("aLogo");
    public static final Doodle A_MAN = Doodle.registerBase("aMan");
    public static final Doodle A_MANS_FACE = Doodle.registerBase("aMan'sFace");
    public static final Doodle A_MONSTER = Doodle.registerBase("aMonster");
    public static final Doodle A_MOVIE_CHARACTER = Doodle.registerBase("aMovieCharacter");
    public static final Doodle A_NATURE_SCENE = Doodle.registerBase("aNatureScene");
    public static final Doodle A_PATTERN = Doodle.registerBase("aPattern");
    public static final Doodle A_PERSON = Doodle.registerBase("aPerson");
    public static final Doodle A_PET = Doodle.registerBase("aPet");
    public static final Doodle A_RELIGIOUS_SCENE = Doodle.registerBase("aReligiousScene");
    public static final Doodle A_SPACE_SCENE = Doodle.registerBase("aSpaceScene");
    public static final Doodle A_STICK_FIGURE = Doodle.registerBase("aStickFigure");
    public static final Doodle A_SURREAL_SCENE = Doodle.registerBase("aSurrealScene");
    public static final Doodle A_VIOLENT_SCENE = Doodle.registerBase("aViolentScene");
    public static final Doodle A_WEIRD_FACE = Doodle.registerBase("aWeirdFace");
    public static final Doodle A_WILD_ANIMAL = Doodle.registerBase("aWildAnimal");
    public static final Doodle A_WOMAN = Doodle.registerBase("aWoman");
    public static final Doodle A_WOMANS_FACE = Doodle.registerBase("aWoman'sFace");
    public static final Doodle AN_ALIEN = Doodle.registerBase("anAlien");
    public static final Doodle AN_ANGRY_MAN = Doodle.registerBase("anAngryMan");
    public static final Doodle AN_ANGRY_PERSON = Doodle.registerBase("anAngryPerson");
    public static final Doodle AN_ANGRY_WOMAN = Doodle.registerBase("anAngryWoman");
    public static final Doodle AN_ATTRACTIVE_LADY = Doodle.registerBase("anAttractiveLady");
    public static final Doodle AN_EXOTIC_ANIMAL = Doodle.registerBase("anExoticAnimal");
    public static final Doodle BUILDINGS = Doodle.registerBase("Buildings");
    public static final Doodle CLOUDS = Doodle.registerBase("Clouds");
    public static final Doodle DEAD_STICK_FIGURES = Doodle.registerBase("DeadStickFigures");
    public static final Doodle FLOWERS = Doodle.registerBase("Flowers");
    public static final Doodle FOOD = Doodle.registerBase("Food");
    public static final Doodle FUNNY_CHARACTERS = Doodle.registerBase("FunnyCharacters");
    public static final Doodle HEARTS = Doodle.registerBase("Hearts");
    public static final Doodle NOTHING_MUCH = Doodle.registerBase("NothingMuch");
    public static final Doodle NUMBERS = Doodle.registerBase("Numbers");
    public static final Doodle RANDOM_LINES = Doodle.registerBase("RandomLines");
    public static final Doodle RANDOM_SHAPES = Doodle.registerBase("RandomShapes");
    public static final Doodle SHAPES = Doodle.registerBase("Shapes");
    public static final Doodle SOMETHING_CRUDE = Doodle.registerBase("SomethingCrude");
    public static final Doodle SQUIGGLY_LINES = Doodle.registerBase("SquigglyLines");
    public static final Doodle STICK_FIGURES = Doodle.registerBase("StickFigures");
    public static final Doodle STICK_FIGURES_FIGHTING = Doodle.registerBase("StickFiguresFighting");
    public static final Doodle SYMBOLS = Doodle.registerBase("Symbols");
    public static final Doodle TEXT = Doodle.registerBase("Text");
    public static final Doodle WEIRD_FACES = Doodle.registerBase("WeirdFaces");
    private final String translationKey;

    private Doodle(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Doodle get(ResourceLocation id) {
        return Registries.DOODLE.get(id);
    }

    public String toString() {
        return Registries.DOODLE.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Doodle register(String id) {
        return Doodle.register(false, id);
    }

    private static Doodle registerBase(String id) {
        return Doodle.register(true, id);
    }

    private static Doodle register(boolean allowDefaultNamespace, String id) {
        return Registries.DOODLE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Doodle(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Doodle doodle : Registries.DOODLE) {
                TranslationKeyValidator.of(doodle.translationKey);
            }
        }
    }
}

