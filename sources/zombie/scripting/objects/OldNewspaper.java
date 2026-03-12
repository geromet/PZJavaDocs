/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class OldNewspaper {
    public static final OldNewspaper BOWLING_GREEN_POST = OldNewspaper.registerBase("BowlingGreenPost");
    public static final OldNewspaper BRANDENBURG_BUGLE = OldNewspaper.registerBase("BrandenburgBugle");
    public static final OldNewspaper CHRISTIAN_BULLETIN = OldNewspaper.registerBase("ChristianBulletin");
    public static final OldNewspaper EVANSVILLE_POST = OldNewspaper.registerBase("EvansvillePost");
    public static final OldNewspaper KENTUCKY_HERALD = OldNewspaper.registerBase("KentuckyHerald");
    public static final OldNewspaper KENTUCKY_OBSERVER = OldNewspaper.registerBase("KentuckyObserver");
    public static final OldNewspaper KNOX_FRONTLINE = OldNewspaper.registerBase("KnoxFrontline");
    public static final OldNewspaper KNOX_KNEWS = OldNewspaper.registerBase("KnoxKnews");
    public static final OldNewspaper LOUISVILLE_STUDENT = OldNewspaper.registerBase("LouisvilleStudent");
    public static final OldNewspaper LOUISVILLE_SUN = OldNewspaper.registerBase("LouisvilleSun");
    public static final OldNewspaper LOUISVILLE_SUN_TIMES = OldNewspaper.registerBase("LouisvilleSunTimes");
    public static final OldNewspaper MULDRAUGH_MESSENGER = OldNewspaper.registerBase("MuldraughMessenger");
    public static final OldNewspaper NATIONAL_DISPATCH = OldNewspaper.registerBase("NationalDispatch");
    public static final OldNewspaper NATIONAL_FINANCE = OldNewspaper.registerBase("NationalFinance");
    public static final OldNewspaper OWENSBORO_OUTSIDER = OldNewspaper.registerBase("OwensboroOutsider");
    public static final OldNewspaper PADUCAH_POST = OldNewspaper.registerBase("PaducahPost");
    public static final OldNewspaper THE_CINCINNATI_TIMES = OldNewspaper.registerBase("TheCincinnatiTimes");
    public static final OldNewspaper THE_KENTUCKY_DEFENDER = OldNewspaper.registerBase("TheKentuckyDefender");
    public static final OldNewspaper THE_LEXINGTON_VOICE = OldNewspaper.registerBase("TheLexingtonVoice");
    public static final OldNewspaper THE_LONDON_POST = OldNewspaper.registerBase("TheLondonPost");
    public static final OldNewspaper WALL_STREET_INSIDER = OldNewspaper.registerBase("WallStreetInsider");
    public static final OldNewspaper WASHINGTON_HERALD = OldNewspaper.registerBase("WashingtonHerald");
    private final String translationKey;

    private OldNewspaper(String id) {
        this.translationKey = "IGUI_NewspaperTitle_" + id;
    }

    public static OldNewspaper get(ResourceLocation id) {
        return Registries.OLD_NEWSPAPER.get(id);
    }

    public String toString() {
        return Registries.OLD_NEWSPAPER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static OldNewspaper register(String id) {
        return OldNewspaper.register(false, id);
    }

    private static OldNewspaper registerBase(String id) {
        return OldNewspaper.register(true, id);
    }

    private static OldNewspaper register(boolean allowDefaultNamespace, String id) {
        return Registries.OLD_NEWSPAPER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new OldNewspaper(id));
    }

    static {
        if (Core.IS_DEV) {
            for (OldNewspaper newspaper : Registries.OLD_NEWSPAPER) {
                TranslationKeyValidator.of(newspaper.getTranslationKey());
            }
        }
    }
}

