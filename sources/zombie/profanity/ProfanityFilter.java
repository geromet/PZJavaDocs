/*
 * Decompiled with CFR 0.152.
 */
package zombie.profanity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zombie.profanity.locales.Locale;
import zombie.profanity.locales.LocaleChinese;
import zombie.profanity.locales.LocaleEnglish;
import zombie.profanity.locales.LocaleGerman;

public class ProfanityFilter {
    public static final boolean DEBUG = false;
    private final Map<String, Locale> locales = new HashMap<String, Locale>();
    private Locale locale;
    private Locale localeDefault;
    private final Pattern prePattern;
    private boolean enabled = true;
    public static final String LOCALES_DIR = "media" + File.separator + "profanity" + File.separator + "locales" + File.separator;
    private static ProfanityFilter instance;

    public static ProfanityFilter getInstance() {
        if (instance == null) {
            instance = new ProfanityFilter();
        }
        return instance;
    }

    private ProfanityFilter() {
        this.addLocale(new LocaleEnglish("EN"), true);
        this.addLocale(new LocaleGerman("GER"));
        this.addLocale(new LocaleChinese("CHIN"));
        this.prePattern = Pattern.compile("(?<spaced>(?:(?:\\s|\\W)[\\w\\$@](?=\\s|\\W)){2,20})|(?<word>[\\w'\\$@_-]+)");
    }

    public static void printDebug(String str) {
    }

    public void enable(boolean b) {
        this.enabled = b;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public int getFilterWordsCount() {
        if (this.locale != null) {
            return this.locale.getFilterWordsCount();
        }
        return 0;
    }

    public void addLocale(Locale l) {
        this.addLocale(l, false);
    }

    public void addLocale(Locale l, boolean setDefault) {
        this.locales.put(l.getID(), l);
        if (setDefault) {
            this.locale = l;
            this.localeDefault = l;
        }
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void addWhiteListWord(String word) {
        if (this.locale != null) {
            this.locale.addWhiteListWord(word);
        }
    }

    public void removeWhiteListWord(String word) {
        if (this.locale != null) {
            this.locale.removeWhiteListWord(word);
        }
    }

    public void addFilterWord(String word) {
        if (this.locale != null) {
            this.locale.addFilterWord(word);
        }
    }

    public void removeFilterWord(String word) {
        if (this.locale != null) {
            this.locale.removeFilterWord(word);
        }
    }

    public void setLocale(String locale) {
        this.locale = this.locales.containsKey(locale) ? this.locales.get(locale) : this.localeDefault;
    }

    public String filterString(String str) {
        if (this.enabled && this.locale != null && str != null && this.locale.getFilterWordsCount() > 0) {
            try {
                StringBuffer s = new StringBuffer();
                Matcher m = this.prePattern.matcher(str);
                while (m.find()) {
                    if (m.group("word") != null) {
                        m.appendReplacement(s, Matcher.quoteReplacement(this.locale.filterWord(m.group("word"), true)));
                        continue;
                    }
                    if (m.group("spaced") == null) continue;
                    m.appendReplacement(s, Matcher.quoteReplacement(" " + this.locale.filterWord(m.group("spaced").replaceAll("\\s+", ""))));
                }
                m.appendTail(s);
                return s.toString();
            }
            catch (Exception e) {
                System.out.println("Profanity failed for: " + str);
            }
        }
        return str;
    }

    public String validateString(String str) {
        return this.validateString(str, true, true, true);
    }

    public String validateString(String str, boolean includePhonetics, boolean includeContains, boolean includeSpacedWords) {
        if (this.enabled && this.locale != null && str != null && this.locale.getFilterWordsCount() > 0) {
            try {
                boolean foundErrors = false;
                StringBuilder s = new StringBuilder();
                Matcher m = this.prePattern.matcher(str);
                while (m.find()) {
                    String error;
                    if (includePhonetics && m.group("word") != null) {
                        error = this.locale.validateWord(m.group("word"), includeContains);
                        if (error == null) continue;
                        if (foundErrors) {
                            s.append(", ");
                        }
                        s.append(error);
                        foundErrors = true;
                        continue;
                    }
                    if (!includeSpacedWords || m.group("spaced") == null || (error = this.locale.validateWord(m.group("spaced").replaceAll("\\s+", ""), false)) == null) continue;
                    if (foundErrors) {
                        s.append(", ");
                    }
                    s.append(error);
                    foundErrors = true;
                }
                return foundErrors ? s.toString() : null;
            }
            catch (Exception e) {
                System.out.println("Profanity validate string failed for: " + str);
                e.printStackTrace();
            }
        }
        return "Failed to parse string :(.";
    }
}

