/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Locket {
    public static final Locket A_BABY = Locket.registerBase("aBaby");
    public static final Locket A_BIRTHDAY_PARTY = Locket.registerBase("aBirthdayParty");
    public static final Locket A_BOY = Locket.registerBase("aBoy");
    public static final Locket A_CHILD = Locket.registerBase("aChild");
    public static final Locket A_COUPLE = Locket.registerBase("aCouple");
    public static final Locket A_DECEASED_LOVED_ONE = Locket.registerBase("aDeceasedLovedOne");
    public static final Locket A_FAMILY = Locket.registerBase("aFamily");
    public static final Locket A_FAMILY_WITH_A_BABY = Locket.registerBase("aFamilywithaBaby");
    public static final Locket A_FAMILY_WITH_A_PET = Locket.registerBase("aFamilywithaPet");
    public static final Locket A_FAMILY_WITH_CHILDREN = Locket.registerBase("aFamilywithChildren");
    public static final Locket A_FAMOUS_PERSON = Locket.registerBase("aFamousPerson");
    public static final Locket A_FATHER_AND_CHILDREN = Locket.registerBase("aFatherandChildren");
    public static final Locket A_FATHER_AND_DAUGHTER = Locket.registerBase("aFatherandDaughter");
    public static final Locket A_FATHER_AND_SON = Locket.registerBase("aFatherandSon");
    public static final Locket A_GIRL = Locket.registerBase("aGirl");
    public static final Locket A_HAPPY_FAMILY = Locket.registerBase("aHappyFamily");
    public static final Locket A_LARGE_FAMILY = Locket.registerBase("aLargeFamily");
    public static final Locket A_LOVED_ONE = Locket.registerBase("aLovedOne");
    public static final Locket A_MAN_WITH_A_BABY = Locket.registerBase("aManwithaBaby");
    public static final Locket A_MARRIED_COUPLE = Locket.registerBase("aMarriedCouple");
    public static final Locket A_MOTHER_AND_CHILDREN = Locket.registerBase("aMotherandChildren");
    public static final Locket A_MOTHER_AND_SON = Locket.registerBase("aMotherandSon");
    public static final Locket A_PET = Locket.registerBase("aPet");
    public static final Locket A_RELIGIOUS_FIGURE = Locket.registerBase("aReligiousFigure");
    public static final Locket A_SMILING_COUPLE = Locket.registerBase("aSmilingCouple");
    public static final Locket A_SMILING_MAN = Locket.registerBase("aSmilingMan");
    public static final Locket A_SMILING_WOMAN = Locket.registerBase("aSmilingWoman");
    public static final Locket A_SOLDIER = Locket.registerBase("aSoldier");
    public static final Locket A_WEDDING = Locket.registerBase("aWedding");
    public static final Locket A_WOMAN_WITH_A_BABY = Locket.registerBase("aWomanwithaBaby");
    public static final Locket A_YOUNG_COUPLE = Locket.registerBase("aYoungCouple");
    public static final Locket A_YOUNG_MAN = Locket.registerBase("aYoungMan");
    public static final Locket A_YOUNG_WOMAN = Locket.registerBase("aYoungWoman");
    public static final Locket AN_OLD_COUPLE = Locket.registerBase("anOldCouple");
    public static final Locket AN_OLD_MAN = Locket.registerBase("anOldMan");
    public static final Locket AN_OLD_WOMAN = Locket.registerBase("anOldWoman");
    public static final Locket CHILDREN = Locket.registerBase("Children");
    public static final Locket CHILDREN_AND_A_BABY = Locket.registerBase("ChildrenandaBaby");
    public static final Locket CHILDREN_AND_BABIES = Locket.registerBase("ChildrenandBabies");
    public static final Locket GRANDPARENTS_AND_GRANDCHILDREN = Locket.registerBase("GrandparentsandGrandchildren");
    public static final Locket PARENTS_WITH_A_DAUGHTER = Locket.registerBase("ParentswithaDaughter");
    public static final Locket PARENTS_WITH_A_SON = Locket.registerBase("ParentswithaSon");
    public static final Locket PARENTS_WITH_TEENAGERS = Locket.registerBase("ParentswithTeenagers");
    public static final Locket PARENTS_WITH_YOUNG_CHILDREN = Locket.registerBase("ParentswithYoungChildren");
    public static final Locket SOMETHING_TOO_FADED_TO_MAKE_OUT = Locket.registerBase("SomethingTooFadedtoMakeOut");
    private final String translationKey;

    private Locket(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Locket get(ResourceLocation id) {
        return Registries.LOCKET.get(id);
    }

    public String toString() {
        return Registries.LOCKET.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Locket register(String id) {
        return Locket.register(false, id);
    }

    private static Locket registerBase(String id) {
        return Locket.register(true, id);
    }

    private static Locket register(boolean allowDefaultNamespace, String id) {
        return Registries.LOCKET.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Locket(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Locket locket : Registries.LOCKET) {
                TranslationKeyValidator.of(locket.translationKey);
            }
        }
    }
}

