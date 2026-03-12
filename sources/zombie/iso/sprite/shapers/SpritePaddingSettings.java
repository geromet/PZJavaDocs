/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite.shapers;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.TransformerException;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.SpritePadding;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;

public class SpritePaddingSettings {
    private static Settings settings;
    private static String settingsFilePath;
    private static PredicatedFileWatcher fileWatcher;

    public static void settingsFileChanged(Settings settings) {
        DebugLog.General.println("Settings file changed.");
        SpritePaddingSettings.settings = settings;
    }

    private static void loadSettings() {
        String settingsFilePath = SpritePaddingSettings.getSettingsFilePath();
        File settingsFile = new File(settingsFilePath).getAbsoluteFile();
        if (settingsFile.isFile()) {
            try {
                settings = PZXmlUtil.parse(Settings.class, settingsFile.getPath());
            }
            catch (PZXmlParserException e) {
                DebugLog.General.printException(e, "Error parsing file: " + settingsFilePath, LogSeverity.Warning);
                settings = new Settings();
            }
        } else {
            settings = new Settings();
            SpritePaddingSettings.saveSettings();
        }
        if (fileWatcher == null) {
            fileWatcher = new PredicatedFileWatcher(settingsFilePath, Settings.class, SpritePaddingSettings::settingsFileChanged);
            DebugFileWatcher.instance.add(fileWatcher);
        }
    }

    private static String getSettingsFilePath() {
        if (settingsFilePath == null) {
            settingsFilePath = ZomboidFileSystem.instance.getLocalWorkDirSub("SpritePaddingSettings.xml");
        }
        return settingsFilePath;
    }

    private static void saveSettings() {
        try {
            PZXmlUtil.write(settings, new File(SpritePaddingSettings.getSettingsFilePath()).getAbsoluteFile());
        }
        catch (IOException | JAXBException | TransformerException e) {
            e.printStackTrace();
        }
    }

    public static Settings getSettings() {
        if (settings == null) {
            SpritePaddingSettings.loadSettings();
        }
        return settings;
    }

    @XmlRootElement(name="FloorShaperDeDiamondSettings")
    public static class Settings {
        @XmlElement(name="IsoPadding")
        public SpritePadding.IsoPaddingSettings isoPadding = new SpritePadding.IsoPaddingSettings();
        @XmlElement(name="FloorDeDiamond")
        public FloorShaperDeDiamond.Settings floorDeDiamond = new FloorShaperDeDiamond.Settings();
        @XmlElement(name="AttachedSprites")
        public FloorShaperAttachedSprites.Settings attachedSprites = new FloorShaperAttachedSprites.Settings();
    }

    public static abstract class GenericZoomBasedSettingGroup {
        public abstract <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting();

        public static <ZoomBasedSetting> ZoomBasedSetting getCurrentZoomSetting(ZoomBasedSetting zoomedIn, ZoomBasedSetting notZoomed, ZoomBasedSetting zoomedOut) {
            float currentZoom = Core.getInstance().getCurrentPlayerZoom();
            if (currentZoom < 1.0f) {
                return zoomedIn;
            }
            if (currentZoom == 1.0f) {
                return notZoomed;
            }
            return zoomedOut;
        }
    }
}

