/*
 * Decompiled with CFR 0.152.
 */
package zombie.profanity.locales;

import java.util.Objects;
import java.util.regex.Matcher;
import zombie.profanity.Phonizer;
import zombie.profanity.locales.LocaleEnglish;

public class LocaleGerman
extends LocaleEnglish {
    public LocaleGerman(String tag) {
        super(tag);
    }

    @Override
    protected void Init() {
        this.storeVowelsAmount = 3;
        super.Init();
        this.addPhonizer(new Phonizer(this, "ringelS", "(?<ringelS>\u00df)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "S");
                }
            }
        });
    }
}

