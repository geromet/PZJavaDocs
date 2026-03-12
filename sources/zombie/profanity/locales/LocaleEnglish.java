/*
 * Decompiled with CFR 0.152.
 */
package zombie.profanity.locales;

import java.util.Objects;
import java.util.regex.Matcher;
import zombie.profanity.Phonizer;
import zombie.profanity.locales.Locale;

public class LocaleEnglish
extends Locale {
    public LocaleEnglish(String tag) {
        super(tag);
    }

    @Override
    protected void Init() {
        this.storeVowelsAmount = 3;
        this.addFilterRawWord("ass");
        this.addPhonizer(new Phonizer(this, "strt", "(?<strt>^(?:KN|GN|PN|AE|WR))"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, m.group(this.getName()).substring(1, 2));
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "dropY", "(?<dropY>(?<=M)B$)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "dropB", "(?<dropB>(?<=M)B$)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "z", "(?<z>Z)"){
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
        this.addPhonizer(new Phonizer(this, "ck", "(?<ck>CK)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "q", "(?<q>Q)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "v", "(?<v>V)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "F");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "xS", "(?<xS>^X)"){
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
        this.addPhonizer(new Phonizer(this, "xKS", "(?<xKS>(?<=\\w)X)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "KS");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "ph", "(?<ph>PH)"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "F");
                }
            }
        });
        this.addPhonizer(new Phonizer(this, "c", "(?<c>C(?=[AUOIE]))"){
            {
                Objects.requireNonNull(this$0);
                super(name, regex);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
    }
}

