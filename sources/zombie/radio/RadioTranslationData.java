/*
 * Decompiled with CFR 0.152.
 */
package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.Language;
import zombie.core.Languages;
import zombie.core.Translator;
import zombie.util.StringUtils;

public final class RadioTranslationData {
    private final String filePath;
    private String guid;
    private String language;
    private Language languageEnum;
    private int version = -1;
    private final ArrayList<String> translators = new ArrayList();
    private final Map<String, String> translations = new HashMap<String, String>();

    public RadioTranslationData(String file) {
        this.filePath = file;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getGuid() {
        return this.guid;
    }

    public String getLanguage() {
        return this.language;
    }

    public Language getLanguageEnum() {
        return this.languageEnum;
    }

    public int getVersion() {
        return this.version;
    }

    public int getTranslationCount() {
        return this.translations.size();
    }

    public ArrayList<String> getTranslators() {
        return this.translators;
    }

    public boolean validate() {
        return this.guid != null && this.language != null && this.version >= 0;
    }

    public boolean loadTranslations() {
        boolean valid;
        block11: {
            valid = false;
            if (Translator.getLanguage() != this.languageEnum) {
                System.out.println("Radio translations trying to load language that is not the current language...");
                return false;
            }
            try {
                String line;
                File f = new File(this.filePath);
                if (!f.exists() || f.isDirectory()) break block11;
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(this.filePath)));
                boolean open = false;
                ArrayList<String> memberKeys = new ArrayList<String>();
                while ((line = br.readLine()) != null) {
                    if ((line = line.trim()).equals("[Translations]")) {
                        open = true;
                        continue;
                    }
                    if (!open) continue;
                    if (line.equals("[Collection]")) {
                        String text = null;
                        while ((line = br.readLine()) != null && !(line = line.trim()).equals("[/Collection]")) {
                            String[] parts = line.split("=", 2);
                            if (parts.length != 2) continue;
                            String key = parts[0].trim();
                            String val = parts[1].trim();
                            if (key.equals("text")) {
                                text = val;
                                continue;
                            }
                            if (!key.equals("member")) continue;
                            memberKeys.add(val);
                        }
                        if (text != null && !memberKeys.isEmpty()) {
                            for (String guid : memberKeys) {
                                this.translations.put(guid, text);
                            }
                        }
                        memberKeys.clear();
                        continue;
                    }
                    if (line.equals("[/Translations]")) {
                        valid = true;
                        break;
                    }
                    String[] parts = line.split("=", 2);
                    if (parts.length != 2) continue;
                    String key = parts[0].trim();
                    String val = parts[1].trim();
                    this.translations.put(key, val);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                valid = false;
            }
        }
        return valid;
    }

    public String getTranslation(String guid) {
        if (this.translations.containsKey(guid)) {
            return this.translations.get(guid);
        }
        return null;
    }

    public static RadioTranslationData ReadFile(String file) {
        RadioTranslationData transData = new RadioTranslationData(file);
        File f = new File(file);
        if (f.exists() && !f.isDirectory()) {
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis);
                 BufferedReader br = new BufferedReader(isr);){
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        String[] nms;
                        String id = parts[0].trim();
                        Object txt = "";
                        for (int i = 1; i < parts.length; ++i) {
                            txt = (String)txt + parts[i];
                        }
                        txt = ((String)txt).trim();
                        if (id.equals("guid")) {
                            transData.guid = txt;
                        } else if (id.equals("language")) {
                            transData.language = txt;
                        } else if (id.equals("version")) {
                            transData.version = Integer.parseInt((String)txt);
                        } else if (id.equals("translator") && (nms = ((String)txt).split(",")).length > 0) {
                            for (String name : nms) {
                                transData.translators.add(name);
                            }
                        }
                    }
                    if (!(line = line.trim()).equals("[/Info]")) continue;
                    break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        boolean found = false;
        if (transData.language != null) {
            for (Language lang : Translator.getAvailableLanguage()) {
                if (!lang.toString().equals(transData.language)) continue;
                transData.languageEnum = lang;
                found = true;
                break;
            }
        }
        if (!found && transData.language != null) {
            System.out.println("Language " + transData.language + " not found");
            return null;
        }
        if (transData.guid != null && transData.language != null && transData.version >= 0) {
            return transData;
        }
        return null;
    }

    private static Language parseLanguageFromFilename(String fileName) {
        if (!StringUtils.startsWithIgnoreCase(fileName, "RadioData_")) {
            return null;
        }
        String language = fileName.replaceFirst("RadioData_", "").replace(".txt", "");
        return Languages.instance.getByName(language);
    }
}

