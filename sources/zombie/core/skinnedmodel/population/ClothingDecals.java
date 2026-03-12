/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.skinnedmodel.population.ClothingDecal;
import zombie.core.skinnedmodel.population.ClothingDecalGroup;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;

@XmlRootElement(name="clothingDecals")
public class ClothingDecals {
    @XmlElement(name="group")
    public final ArrayList<ClothingDecalGroup> groups = new ArrayList();
    @XmlTransient
    public static ClothingDecals instance;
    private final HashMap<String, CachedDecal> cachedDecals = new HashMap();

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("ClothingDecals Already Initialized.");
        }
        instance = ClothingDecals.Parse(ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath() + File.separator + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar));
        if (instance == null) {
            return;
        }
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            int index;
            ClothingDecalGroup group;
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod == null) continue;
            String modDir = mod.getVersionDir();
            ClothingDecals clothingDecals = ClothingDecals.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar));
            if (clothingDecals != null) {
                for (ClothingDecalGroup groupMod : clothingDecals.groups) {
                    group = instance.FindGroup(groupMod.name);
                    if (group == null) {
                        ClothingDecals.instance.groups.add(groupMod);
                        continue;
                    }
                    DebugType.Clothing.println("mod \"%s\" overrides decal group \"%s\"", modID, groupMod.name);
                    index = ClothingDecals.instance.groups.indexOf(group);
                    ClothingDecals.instance.groups.set(index, groupMod);
                }
                continue;
            }
            modDir = mod.getCommonDir();
            clothingDecals = ClothingDecals.Parse(modDir + File.separator + ZomboidFileSystem.processFilePath("media/clothing/clothingDecals.xml", File.separatorChar));
            if (clothingDecals == null) continue;
            for (ClothingDecalGroup groupMod : clothingDecals.groups) {
                group = instance.FindGroup(groupMod.name);
                if (group == null) {
                    ClothingDecals.instance.groups.add(groupMod);
                    continue;
                }
                DebugType.Clothing.println("mod \"%s\" overrides decal group \"%s\"", modID, groupMod.name);
                index = ClothingDecals.instance.groups.indexOf(group);
                ClothingDecals.instance.groups.set(index, groupMod);
            }
        }
    }

    public static void Reset() {
        if (instance == null) {
            return;
        }
        ClothingDecals.instance.cachedDecals.clear();
        ClothingDecals.instance.groups.clear();
        instance = null;
    }

    public static ClothingDecals Parse(String filename) {
        try {
            return ClothingDecals.parse(filename);
        }
        catch (FileNotFoundException fileNotFoundException) {
        }
        catch (IOException | JAXBException e) {
            ExceptionLogger.logException(e);
        }
        return null;
    }

    public static ClothingDecals parse(String filename) throws JAXBException, IOException {
        try (FileInputStream adrFile = new FileInputStream(filename);){
            JAXBContext ctx = JAXBContext.newInstance(ClothingDecals.class);
            Unmarshaller um = ctx.createUnmarshaller();
            ClothingDecals clothingDecals = (ClothingDecals)um.unmarshal(adrFile);
            return clothingDecals;
        }
    }

    public ClothingDecal getDecal(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        }
        CachedDecal cachedDecal = this.cachedDecals.get(name);
        if (cachedDecal == null) {
            cachedDecal = new CachedDecal();
            this.cachedDecals.put(name, cachedDecal);
        }
        if (cachedDecal.decal != null) {
            return cachedDecal.decal;
        }
        String filePath = ZomboidFileSystem.instance.getString("media/clothing/clothingDecals/" + name + ".xml");
        try {
            cachedDecal.decal = PZXmlUtil.parse(ClothingDecal.class, filePath);
            cachedDecal.decal.name = name;
        }
        catch (PZXmlParserException e) {
            System.err.println("Failed to load ClothingDecal: " + filePath);
            ExceptionLogger.logException(e);
            return null;
        }
        return cachedDecal.decal;
    }

    public ClothingDecalGroup FindGroup(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        }
        for (int i = 0; i < this.groups.size(); ++i) {
            ClothingDecalGroup group = this.groups.get(i);
            if (!group.name.equalsIgnoreCase(name)) continue;
            return group;
        }
        return null;
    }

    public String getRandomDecal(String groupName) {
        ClothingDecalGroup group = this.FindGroup(groupName);
        if (group == null) {
            return null;
        }
        return group.getRandomDecal();
    }

    private static final class CachedDecal {
        ClothingDecal decal;

        private CachedDecal() {
        }
    }
}

