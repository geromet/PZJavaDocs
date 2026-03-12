/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zombie.ZomboidFileSystem;
import zombie.core.Language;
import zombie.core.LanguageFile;
import zombie.core.logger.ExceptionLogger;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.Lambda;
import zombie.util.list.PZArrayUtil;

public final class Languages {
    public static final Languages instance = new Languages();
    private final ArrayList<Language> languages = new ArrayList();
    private Language defaultLanguage = new Language("EN", "English", null, false);

    public Languages() {
        this.languages.add(this.defaultLanguage);
    }

    public void init() {
        this.languages.clear();
        this.defaultLanguage = new Language("EN", "English", null, false);
        this.languages.add(this.defaultLanguage);
        this.loadTranslateDirectory(ZomboidFileSystem.instance.getMediaPath("lua/shared/Translate"));
        for (String modId : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modId);
            if (mod == null) continue;
            File file = new File(mod.getCommonDir(), "media/lua/shared/Translate");
            if (file.isDirectory()) {
                this.loadTranslateDirectory(file.getAbsolutePath());
            }
            if (!(file = new File(mod.getVersionDir(), "media/lua/shared/Translate")).isDirectory()) continue;
            this.loadTranslateDirectory(file.getAbsolutePath());
        }
    }

    public Language getDefaultLanguage() {
        return this.defaultLanguage;
    }

    public List<Language> getLanguages() {
        return Collections.unmodifiableList(this.languages);
    }

    public Language getByIndex(int index) {
        if (index >= 0 && index < this.languages.size()) {
            return this.languages.get(index);
        }
        return null;
    }

    public Language getByName(String name) {
        return PZArrayUtil.find(this.languages, Lambda.predicate(name, (language, lName) -> language.name().equalsIgnoreCase((String)lName)));
    }

    public int getIndexByName(String name) {
        return PZArrayUtil.indexOf(this.languages, Lambda.predicate(name, (language, lName) -> language.name().equalsIgnoreCase((String)lName)));
    }

    private void loadTranslateDirectory(String dirName) {
        DirectoryStream.Filter<Path> filter = entry -> Files.isDirectory(entry, new LinkOption[0]) && Files.exists(entry.resolve("language.txt"), new LinkOption[0]);
        Path dir = FileSystems.getDefault().getPath(dirName, new String[0]);
        if (!Files.exists(dir, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter);){
            for (Path path : dstrm) {
                LanguageFile data = this.loadLanguageDirectory(path.toAbsolutePath());
                if (data == null) continue;
                Language lang = new Language(data.name, data.text, data.base, data.azerty);
                int index = this.getIndexByName(data.name);
                if (index == -1) {
                    this.languages.add(lang);
                    continue;
                }
                this.languages.set(index, lang);
                if (!data.name.equals(this.defaultLanguage.name())) continue;
                this.defaultLanguage = lang;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    private LanguageFile loadLanguageDirectory(Path dir) {
        String name = dir.getFileName().toString();
        try {
            return new LanguageFile(name, dir.resolve("language.txt"));
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            return null;
        }
    }
}

