/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class BookSubject {
    public static final BookSubject ADVENTURE_NON_FICTION = BookSubject.registerBase("adventure_non_fiction");
    public static final BookSubject ART = BookSubject.registerBase("art");
    public static final BookSubject BASEBALL = BookSubject.registerBase("baseball");
    public static final BookSubject BIBLE = BookSubject.registerBase("bible");
    public static final BookSubject BIOGRAPHY = BookSubject.registerBase("biography");
    public static final BookSubject BUSINESS = BookSubject.registerBase("business");
    public static final BookSubject CHILDS = BookSubject.registerBase("childs");
    public static final BookSubject CHILDS_PICTURE_SPECIAL = BookSubject.registerBase("childs_picture_special");
    public static final BookSubject CINEMA = BookSubject.registerBase("cinema");
    public static final BookSubject CLASSIC = BookSubject.registerBase("classic");
    public static final BookSubject CLASSIC_FICTION = BookSubject.registerBase("classic_fiction");
    public static final BookSubject CLASSIC_NONFICTION = BookSubject.registerBase("classic_nonfiction");
    public static final BookSubject COMPUTER = BookSubject.registerBase("computer");
    public static final BookSubject CONSPIRACY = BookSubject.registerBase("conspiracy");
    public static final BookSubject CRIME_FICTION = BookSubject.registerBase("crime_fiction");
    public static final BookSubject DIET = BookSubject.registerBase("diet");
    public static final BookSubject FANTASY = BookSubject.registerBase("fantasy");
    public static final BookSubject FARMING = BookSubject.registerBase("farming");
    public static final BookSubject FASHION = BookSubject.registerBase("fashion");
    public static final BookSubject GENERAL_FICTION = BookSubject.registerBase("general_fiction");
    public static final BookSubject GENERAL_REFERENCE = BookSubject.registerBase("general_reference");
    public static final BookSubject GOLF = BookSubject.registerBase("golf");
    public static final BookSubject HASS = BookSubject.registerBase("hass");
    public static final BookSubject HISTORY = BookSubject.registerBase("history");
    public static final BookSubject HORROR = BookSubject.registerBase("horror");
    public static final BookSubject LEGAL = BookSubject.registerBase("legal");
    public static final BookSubject MEDICAL = BookSubject.registerBase("medical");
    public static final BookSubject MILITARY = BookSubject.registerBase("military");
    public static final BookSubject MILITARY_HISTORY = BookSubject.registerBase("military_history");
    public static final BookSubject MUSIC = BookSubject.registerBase("music");
    public static final BookSubject NATURE = BookSubject.registerBase("nature");
    public static final BookSubject NEW_AGE = BookSubject.registerBase("new_age");
    public static final BookSubject OCCULT = BookSubject.registerBase("occult");
    public static final BookSubject PHILOSOPHY = BookSubject.registerBase("philosophy");
    public static final BookSubject PHOTO_SPECIAL = BookSubject.registerBase("photo_special");
    public static final BookSubject PLAY = BookSubject.registerBase("play");
    public static final BookSubject POLICING = BookSubject.registerBase("policing");
    public static final BookSubject POLITICS = BookSubject.registerBase("politics");
    public static final BookSubject QUACKERY = BookSubject.registerBase("quackery");
    public static final BookSubject QUIGLEY = BookSubject.registerBase("quigley");
    public static final BookSubject RELATIONSHIP = BookSubject.registerBase("relationship");
    public static final BookSubject RELIGION = BookSubject.registerBase("religion");
    public static final BookSubject ROMANCE = BookSubject.registerBase("romance");
    public static final BookSubject SAD_NON_FICTION = BookSubject.registerBase("sad_non_fiction");
    public static final BookSubject SCHOOL_TEXTBOOK = BookSubject.registerBase("school_textbook");
    public static final BookSubject SCIENCE = BookSubject.registerBase("science");
    public static final BookSubject SCIFI = BookSubject.registerBase("scifi");
    public static final BookSubject SELF_HELP = BookSubject.registerBase("self_help");
    public static final BookSubject SEXY = BookSubject.registerBase("sexy");
    public static final BookSubject SPORTS = BookSubject.registerBase("sports");
    public static final BookSubject TEENS = BookSubject.registerBase("teens");
    public static final BookSubject THRILLER = BookSubject.registerBase("thriller");
    public static final BookSubject TRAVEL = BookSubject.registerBase("travel");
    public static final BookSubject TRUE_CRIME = BookSubject.registerBase("true_crime");
    public static final BookSubject WESTERN = BookSubject.registerBase("western");

    private BookSubject() {
    }

    public static BookSubject get(ResourceLocation id) {
        return Registries.BOOK_SUBJECT.get(id);
    }

    public String toString() {
        return Registries.BOOK_SUBJECT.getLocation(this).toString();
    }

    public static BookSubject register(String id) {
        return BookSubject.register(false, id);
    }

    private static BookSubject registerBase(String id) {
        return BookSubject.register(true, id);
    }

    private static BookSubject register(boolean allowDefaultNamespace, String id) {
        return Registries.BOOK_SUBJECT.register(RegistryReset.createLocation(id, allowDefaultNamespace), new BookSubject());
    }
}

