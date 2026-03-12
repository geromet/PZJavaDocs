/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class Letter {
    public static final Letter ACCEPTANCE_LETTER = Letter.registerBase("AcceptanceLetter");
    public static final Letter APPLICATION_LETTER = Letter.registerBase("ApplicationLetter");
    public static final Letter BANK_LETTER = Letter.registerBase("BankLetter");
    public static final Letter BILL = Letter.registerBase("Bill");
    public static final Letter BUSINESS_LETTER = Letter.registerBase("BusinessLetter");
    public static final Letter CHARITY_LETTER = Letter.registerBase("CharityLetter");
    public static final Letter CHILDS_LETTER = Letter.registerBase("ChildsLetter");
    public static final Letter CONDOLENCE_LETTER = Letter.registerBase("CondolenceLetter");
    public static final Letter EMPLOYMENT_LETTER = Letter.registerBase("EmploymentLetter");
    public static final Letter FRIENDLY_LETTER = Letter.registerBase("FriendlyLetter");
    public static final Letter GOVERNMENT_LETTER = Letter.registerBase("GovernmentLetter");
    public static final Letter INVITATION_LETTER = Letter.registerBase("InvitationLetter");
    public static final Letter LEGAL_LETTER = Letter.registerBase("LegalLetter");
    public static final Letter LETTER = Letter.registerBase("Letter");
    public static final Letter OFFICIAL_LETTER = Letter.registerBase("OfficialLetter");
    public static final Letter OVERDUE_BILL = Letter.registerBase("OverdueBill");
    public static final Letter REJECTION_LETTER = Letter.registerBase("RejectionLetter");
    public static final Letter RESIGNATION_LETTER = Letter.registerBase("ResignationLetter");
    public static final Letter ROMANTIC_LETTER = Letter.registerBase("RomanticLetter");
    public static final Letter RUDE_LETTER = Letter.registerBase("RudeLetter");
    public static final Letter SAD_LETTER = Letter.registerBase("SadLetter");
    public static final Letter SCAM_LETTER = Letter.registerBase("ScamLetter");
    public static final Letter THANK_YOU_LETTER = Letter.registerBase("ThankYouLetter");
    public static final Letter THREATENING_LETTER = Letter.registerBase("ThreateningLetter");
    private final String translationKey;

    private Letter(String id) {
        this.translationKey = "IGUI_" + id;
    }

    public static Letter get(ResourceLocation id) {
        return Registries.LETTER.get(id);
    }

    public String toString() {
        return Registries.LETTER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Letter register(String id) {
        return Letter.register(false, id);
    }

    private static Letter registerBase(String id) {
        return Letter.register(true, id);
    }

    private static Letter register(boolean allowDefaultNamespace, String id) {
        return Registries.LETTER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Letter(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Letter letter : Registries.LETTER) {
                TranslationKeyValidator.of(letter.translationKey);
            }
        }
    }
}

