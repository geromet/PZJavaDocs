/*
 * Decompiled with CFR 0.152.
 */
package zombie.world;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.world.DictionaryInfo;
import zombie.world.EntityInfo;
import zombie.world.ItemInfo;
import zombie.world.ScriptsDictionary;
import zombie.world.StringDictionary;
import zombie.world.WorldDictionary;
import zombie.world.WorldDictionaryException;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;

public class DictionaryData {
    protected final Map<Short, ItemInfo> itemIdToInfoMap = new HashMap<Short, ItemInfo>();
    protected final Map<String, ItemInfo> itemTypeToInfoMap = new HashMap<String, ItemInfo>();
    protected final Map<Short, EntityInfo> entityIdToInfoMap = new HashMap<Short, EntityInfo>();
    protected final Map<String, EntityInfo> entityTypeToInfoMap = new HashMap<String, EntityInfo>();
    protected final Map<String, Integer> spriteNameToIdMap = new HashMap<String, Integer>();
    protected final Map<Integer, String> spriteIdToNameMap = new HashMap<Integer, String>();
    protected final Map<String, Byte> objectNameToIdMap = new HashMap<String, Byte>();
    protected final Map<Byte, String> objectIdToNameMap = new HashMap<Byte, String>();
    protected final ArrayList<String> unsetObject = new ArrayList();
    protected final ArrayList<String> unsetSprites = new ArrayList();
    protected short nextInfoId = (short)Short.MIN_VALUE;
    protected int nextSpriteNameId;
    protected byte mextObjectNameId;
    protected byte[] serverDataCache;
    private File dataBackupPath;

    protected DictionaryData() {
    }

    protected boolean isClient() {
        return false;
    }

    protected void reset() {
        this.nextInfoId = 0;
        this.nextSpriteNameId = 0;
        this.mextObjectNameId = 0;
        this.itemIdToInfoMap.clear();
        this.itemTypeToInfoMap.clear();
        this.entityIdToInfoMap.clear();
        this.entityTypeToInfoMap.clear();
        this.objectIdToNameMap.clear();
        this.objectNameToIdMap.clear();
        this.spriteIdToNameMap.clear();
        this.spriteNameToIdMap.clear();
    }

    protected final ItemInfo getItemInfoFromType(String fulltype) {
        return this.itemTypeToInfoMap.get(fulltype);
    }

    protected final ItemInfo getItemInfoFromID(short registeryID) {
        return this.itemIdToInfoMap.get(registeryID);
    }

    protected final short getItemRegistryID(String fullType) {
        ItemInfo info = this.itemTypeToInfoMap.get(fullType);
        if (info != null) {
            return info.registryId;
        }
        if (Core.debug) {
            DebugLog.log("WARNING: Cannot get registry id for item: " + fullType);
        }
        return -1;
    }

    protected final String getItemTypeFromID(short id) {
        ItemInfo info = this.itemIdToInfoMap.get(id);
        if (info != null) {
            return info.fullType;
        }
        return null;
    }

    protected final String getItemTypeDebugString(short id) {
        String type = this.getItemTypeFromID(id);
        if (type == null) {
            type = "Unknown";
        }
        return type;
    }

    protected final EntityInfo getEntityInfoFromType(String fulltype) {
        return this.entityTypeToInfoMap.get(fulltype);
    }

    protected final EntityInfo getEntityInfoFromID(short registeryID) {
        return this.entityIdToInfoMap.get(registeryID);
    }

    protected final short getEntityRegistryID(String fullType) {
        EntityInfo info = this.entityTypeToInfoMap.get(fullType);
        if (info != null) {
            return info.registryId;
        }
        if (Core.debug) {
            DebugLog.log("WARNING: Cannot get registry id for entity: " + fullType);
        }
        return -1;
    }

    protected final String getEntityTypeFromID(short id) {
        EntityInfo info = this.entityIdToInfoMap.get(id);
        if (info != null) {
            return info.fullType;
        }
        return null;
    }

    protected final String getEntityTypeDebugString(short id) {
        String type = this.getEntityTypeFromID(id);
        if (type == null) {
            type = "Unknown";
        }
        return type;
    }

    protected final String getSpriteNameFromID(int id) {
        if (id >= 0) {
            if (this.spriteIdToNameMap.containsKey(id)) {
                return this.spriteIdToNameMap.get(id);
            }
            IsoSprite sprite = IsoSprite.getSprite(IsoSpriteManager.instance, id);
            if (sprite != null && sprite.name != null) {
                return sprite.name;
            }
        }
        DebugLog.log("WorldDictionary, Couldnt find sprite name for ID '" + id + "'.");
        return null;
    }

    protected final int getIdForSpriteName(String name) {
        if (name != null) {
            if (this.spriteNameToIdMap.containsKey(name)) {
                return this.spriteNameToIdMap.get(name);
            }
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(name);
            if (sprite != null && sprite.id >= 0 && sprite.id != 20000000 && sprite.name.equals(name)) {
                return sprite.id;
            }
        }
        return -1;
    }

    protected final String getObjectNameFromID(byte id) {
        if (id >= 0) {
            if (this.objectIdToNameMap.containsKey(id)) {
                return this.objectIdToNameMap.get(id);
            }
            if (Core.debug) {
                DebugLog.log("WorldDictionary, Couldnt find object name for ID '" + id + "'.");
            }
        }
        return null;
    }

    protected final byte getIdForObjectName(String name) {
        if (name != null) {
            if (this.objectNameToIdMap.containsKey(name)) {
                return this.objectNameToIdMap.get(name);
            }
            if (Core.debug) {
                // empty if block
            }
        }
        return -1;
    }

    protected final void getDictionaryMods(List<String> list) {
        int i;
        List s;
        list.clear();
        for (Map.Entry<Short, ItemInfo> entry : this.itemIdToInfoMap.entrySet()) {
            if (!list.contains(entry.getValue().modId)) {
                list.add(entry.getValue().modId);
            }
            if (entry.getValue().modOverrides == null) continue;
            s = entry.getValue().modOverrides;
            for (i = 0; i < s.size(); ++i) {
                if (list.contains(s.get(i))) continue;
                list.add((String)s.get(i));
            }
        }
        for (Map.Entry<Short, DictionaryInfo> entry : this.entityIdToInfoMap.entrySet()) {
            if (!list.contains(((EntityInfo)entry.getValue()).modId)) {
                list.add(((EntityInfo)entry.getValue()).modId);
            }
            if (((EntityInfo)entry.getValue()).modOverrides == null) continue;
            s = ((EntityInfo)entry.getValue()).modOverrides;
            for (i = 0; i < s.size(); ++i) {
                if (list.contains(s.get(i))) continue;
                list.add((String)s.get(i));
            }
        }
    }

    protected final void getModuleList(List<String> list) {
        this.getModuleList(list, this.itemIdToInfoMap);
        this.getModuleList(list, this.entityIdToInfoMap);
    }

    private final <T extends DictionaryInfo<?>> void getModuleList(List<String> list, Map<Short, T> map) {
        for (Map.Entry<Short, T> entry : map.entrySet()) {
            if (list.contains(((DictionaryInfo)entry.getValue()).moduleName)) continue;
            list.add(((DictionaryInfo)entry.getValue()).moduleName);
        }
    }

    protected <T extends DictionaryInfo<?>> void parseInfoLoadList(Map<String, T> infoLoadList) throws WorldDictionaryException {
        for (Map.Entry<String, T> entry : infoLoadList.entrySet()) {
            DictionaryInfo info = (DictionaryInfo)entry.getValue();
            DictionaryInfo stored = info instanceof ItemInfo ? (DictionaryInfo)this.itemTypeToInfoMap.get(info.fullType) : (DictionaryInfo)this.entityTypeToInfoMap.get(info.fullType);
            if (stored == null) {
                if (info.obsolete) continue;
                if (this.nextInfoId >= Short.MAX_VALUE) {
                    throw new WorldDictionaryException("Max item ID value reached for WorldDictionary!");
                }
                this.nextInfoId = (short)(this.nextInfoId + 1);
                info.registryId = info.registryId;
                info.isLoaded = true;
                if (info instanceof ItemInfo) {
                    ItemInfo itemInfo = (ItemInfo)info;
                    this.itemTypeToInfoMap.put(info.fullType, itemInfo);
                    this.itemIdToInfoMap.put(info.registryId, itemInfo);
                } else {
                    this.entityTypeToInfoMap.put(info.fullType, (EntityInfo)info);
                    this.entityIdToInfoMap.put(info.registryId, (EntityInfo)info);
                }
                WorldDictionaryLogger.log(new Log.RegisterItem((DictionaryInfo<?>)info.copy()));
                continue;
            }
            if (stored.removed && !info.obsolete) {
                stored.removed = false;
                WorldDictionaryLogger.log(new Log.ReinstateItem((DictionaryInfo<?>)stored.copy()));
            }
            if (!stored.modId.equals(info.modId)) {
                String oldID = stored.modId;
                stored.modId = info.modId;
                stored.isModded = !info.modId.equals("pz-vanilla");
                WorldDictionaryLogger.log(new Log.ModIDChangedItem((DictionaryInfo<?>)stored.copy(), oldID, stored.modId));
            }
            if (!(!info.obsolete || stored.obsolete && stored.removed)) {
                stored.obsolete = true;
                stored.removed = true;
                WorldDictionaryLogger.log(new Log.ObsoleteItem((DictionaryInfo<?>)stored.copy()));
            }
            stored.isLoaded = true;
        }
    }

    protected void parseCurrentInfoSet() throws WorldDictionaryException {
        this.parseCurrentInfoSet(this.itemTypeToInfoMap);
        this.parseCurrentInfoSet(this.entityTypeToInfoMap);
    }

    protected <T extends DictionaryInfo<?>> void parseCurrentInfoSet(Map<String, T> infoLoadList) throws WorldDictionaryException {
        for (Map.Entry<String, T> entry : infoLoadList.entrySet()) {
            DictionaryInfo info = (DictionaryInfo)entry.getValue();
            boolean loggedRemoved = false;
            if (!info.isLoaded) {
                info.removed = true;
                WorldDictionaryLogger.log(new Log.RemovedItem((DictionaryInfo<?>)info.copy(), false));
                loggedRemoved = true;
            }
            boolean registered = false;
            if (info instanceof ItemInfo) {
                ItemInfo itemInfo = (ItemInfo)info;
                if (itemInfo.scriptItem == null) {
                    itemInfo.scriptItem = ScriptManager.instance.getSpecificItem(info.fullType);
                    itemInfo.entityScript = itemInfo.scriptItem;
                }
                if (itemInfo.scriptItem != null) {
                    itemInfo.scriptItem.setRegistry_id(info.registryId);
                    registered = true;
                }
            } else {
                EntityInfo entityInfo = (EntityInfo)info;
                if (entityInfo.entityScript == null) {
                    entityInfo.entityScript = ScriptManager.instance.getSpecificEntity(info.fullType);
                }
                if (entityInfo.entityScript != null) {
                    entityInfo.entityScript.setRegistry_id(info.registryId);
                    registered = true;
                }
            }
            if (registered) continue;
            info.removed = true;
            if (loggedRemoved) continue;
            WorldDictionaryLogger.log(new Log.RemovedItem((DictionaryInfo<?>)info.copy(), true));
        }
    }

    protected void parseObjectNameLoadList(List<String> objNameLoadList) throws WorldDictionaryException {
        for (int i = 0; i < objNameLoadList.size(); ++i) {
            byte id;
            String name = objNameLoadList.get(i);
            if (this.objectNameToIdMap.containsKey(name)) continue;
            if (this.mextObjectNameId >= 127) {
                WorldDictionaryLogger.log("Max value for object names reached.");
                if (!Core.debug) continue;
                throw new WorldDictionaryException("Max value for object names reached.");
            }
            this.mextObjectNameId = (byte)(this.mextObjectNameId + 1);
            this.objectIdToNameMap.put(id, name);
            this.objectNameToIdMap.put(name, id);
            WorldDictionaryLogger.log(new Log.RegisterObject(name, id));
        }
    }

    protected void backupCurrentDataSet() throws IOException {
        this.dataBackupPath = null;
        if (Core.getInstance().isNoSave()) {
            return;
        }
        File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary.bin"));
        if (path.exists()) {
            long ut = Instant.now().getEpochSecond();
            this.dataBackupPath = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary_" + ut + ".bak"));
            Files.copy(path, this.dataBackupPath);
        }
    }

    protected void deleteBackupCurrentDataSet() throws IOException {
        if (Core.getInstance().isNoSave()) {
            this.dataBackupPath = null;
            return;
        }
        if (this.dataBackupPath != null) {
            this.dataBackupPath.delete();
        }
        this.dataBackupPath = null;
    }

    protected void createErrorBackups() {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        try {
            WorldDictionary.log("Attempting to copy WorldDictionary backups...");
            long ut = Instant.now().getEpochSecond();
            String errorPathStr = ZomboidFileSystem.instance.getFileNameInCurrentSave("WD_ERROR_" + ut) + File.separator;
            WorldDictionary.log("path = " + errorPathStr);
            File errorPath = new File(errorPathStr);
            boolean dirCreated = true;
            if (!errorPath.exists()) {
                dirCreated = errorPath.mkdir();
            }
            if (!dirCreated) {
                WorldDictionary.log("Could not create backup folder folder.");
                return;
            }
            if (this.dataBackupPath != null) {
                File backupTarget = new File(errorPathStr + "WorldDictionary_backup.bin");
                if (this.dataBackupPath.exists()) {
                    Files.copy(this.dataBackupPath, backupTarget);
                }
            }
            File logFilePath = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionaryLog.lua"));
            File logFileTarget = new File(errorPathStr + "WorldDictionaryLog.lua");
            if (logFilePath.exists()) {
                Files.copy(logFilePath, logFileTarget);
            }
            File readablePath = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionaryReadable.lua"));
            File readableTarget = new File(errorPathStr + "WorldDictionaryReadable.lua");
            if (readablePath.exists()) {
                Files.copy(readablePath, readableTarget);
            }
            File dataPath = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary.bin"));
            File dataTarget = new File(errorPathStr + "WorldDictionary.bin");
            if (dataPath.exists()) {
                Files.copy(dataPath, dataTarget);
            }
        }
        catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    protected void load() throws IOException, WorldDictionaryException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary.bin");
        File path = new File(fileName);
        if (!path.exists()) {
            if (!WorldDictionary.isIsNewGame()) {
                throw new WorldDictionaryException("WorldDictionary data file is missing from world folder.");
            }
            return;
        }
        try (FileInputStream inStream = new FileInputStream(path);){
            DebugLog.log("Loading WorldDictionary:" + fileName);
            ByteBuffer bb = ByteBuffer.allocate((int)path.length());
            bb.clear();
            int len = inStream.read(bb.array());
            bb.limit(len);
            this.loadFromByteBuffer(bb);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WorldDictionaryException("Error loading WorldDictionary.", e);
        }
    }

    protected void loadFromByteBuffer(ByteBuffer bb) throws IOException {
        int version = bb.getInt();
        this.nextInfoId = bb.getShort();
        this.mextObjectNameId = bb.get();
        this.nextSpriteNameId = bb.getInt();
        ArrayList<String> modIDs = new ArrayList<String>();
        int modIDsLen = bb.getInt();
        for (int i = 0; i < modIDsLen; ++i) {
            modIDs.add(GameWindow.ReadString(bb));
        }
        ArrayList<String> modules = new ArrayList<String>();
        int modulesLen = bb.getInt();
        for (int i = 0; i < modulesLen; ++i) {
            modules.add(GameWindow.ReadString(bb));
        }
        int itemLen = bb.getInt();
        for (int i = 0; i < itemLen; ++i) {
            ItemInfo info = new ItemInfo();
            info.load(bb, version, modIDs, modules);
            this.itemIdToInfoMap.put(info.registryId, info);
            this.itemTypeToInfoMap.put(info.fullType, info);
        }
        int entityLen = bb.getInt();
        for (int i = 0; i < entityLen; ++i) {
            EntityInfo info = new EntityInfo();
            info.load(bb, version, modIDs, modules);
            this.entityIdToInfoMap.put(info.registryId, info);
            this.entityTypeToInfoMap.put(info.fullType, info);
        }
        int objLen = bb.getInt();
        for (int i = 0; i < objLen; ++i) {
            byte id = bb.get();
            String name = GameWindow.ReadString(bb);
            this.objectIdToNameMap.put(id, name);
            this.objectNameToIdMap.put(name, id);
        }
        int sprLen = bb.getInt();
        for (int i = 0; i < sprLen; ++i) {
            int id = bb.getInt();
            String name = GameWindow.ReadString(bb);
            this.spriteIdToNameMap.put(id, name);
            this.spriteNameToIdMap.put(name, id);
        }
        StringDictionary.loadFromByteBuffer(bb, version);
        ScriptsDictionary.loadFromByteBuffer(bb, version);
    }

    protected void save() throws IOException, WorldDictionaryException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        try {
            byte[] bytes = new byte[0x500000];
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            this.saveToByteBuffer(bb);
            bb.flip();
            if (GameServer.server) {
                bytes = new byte[bb.limit()];
                bb.get(bytes, 0, bytes.length);
                this.serverDataCache = bytes;
            }
            File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary.tmp"));
            FileOutputStream output = new FileOutputStream(path);
            output.getChannel().truncate(0L);
            output.write(bb.array(), 0, bb.limit());
            output.flush();
            output.close();
            File pathFinal = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("WorldDictionary.bin"));
            Files.copy(path, pathFinal);
            path.delete();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new WorldDictionaryException("Error saving WorldDictionary.", e);
        }
    }

    protected void saveToByteBuffer(ByteBuffer bb) throws IOException {
        DictionaryInfo info;
        bb.putInt(1);
        bb.putShort(this.nextInfoId);
        bb.put(this.mextObjectNameId);
        bb.putInt(this.nextSpriteNameId);
        ArrayList<String> modIDs = new ArrayList<String>();
        this.getDictionaryMods(modIDs);
        bb.putInt(modIDs.size());
        for (String modid : modIDs) {
            GameWindow.WriteString(bb, modid);
        }
        ArrayList<String> modules = new ArrayList<String>();
        this.getModuleList(modules);
        bb.putInt(modules.size());
        for (String string : modules) {
            GameWindow.WriteString(bb, string);
        }
        bb.putInt(this.itemIdToInfoMap.size());
        for (Map.Entry entry : this.itemIdToInfoMap.entrySet()) {
            info = (ItemInfo)entry.getValue();
            info.save(bb, modIDs, modules);
        }
        bb.putInt(this.entityIdToInfoMap.size());
        for (Map.Entry entry : this.entityIdToInfoMap.entrySet()) {
            info = (EntityInfo)entry.getValue();
            info.save(bb, modIDs, modules);
        }
        bb.putInt(this.objectIdToNameMap.size());
        for (Map.Entry entry : this.objectIdToNameMap.entrySet()) {
            bb.put((Byte)entry.getKey());
            GameWindow.WriteString(bb, (String)entry.getValue());
        }
        bb.putInt(this.spriteIdToNameMap.size());
        for (Map.Entry entry : this.spriteIdToNameMap.entrySet()) {
            bb.putInt((Integer)entry.getKey());
            GameWindow.WriteString(bb, (String)entry.getValue());
        }
        StringDictionary.saveToByteBuffer(bb);
        ScriptsDictionary.saveToByteBuffer(bb);
    }

    protected void saveAsText(String saveFile) throws IOException, WorldDictionaryException {
        if (Core.getInstance().isNoSave()) {
            return;
        }
        File path = new File(ZomboidFileSystem.instance.getCurrentSaveDir() + File.separator);
        if (path.exists() && path.isDirectory()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave(saveFile);
            File f = new File(fileName);
            try (FileWriter w = new FileWriter(f, false);){
                w.write("--[[ ---- ITEMS ---- --]]" + System.lineSeparator());
                w.write("items = {" + System.lineSeparator());
                for (Map.Entry<Short, ItemInfo> entry : this.itemIdToInfoMap.entrySet()) {
                    w.write("\t{" + System.lineSeparator());
                    entry.getValue().saveAsText(w, "\t\t");
                    w.write("\t}," + System.lineSeparator());
                }
                w.write("}" + System.lineSeparator());
                w.write(System.lineSeparator());
                w.write("--[[ ---- ENTITIES ---- --]]" + System.lineSeparator());
                w.write("entities = {" + System.lineSeparator());
                for (Map.Entry<Short, DictionaryInfo> entry : this.entityIdToInfoMap.entrySet()) {
                    w.write("\t{" + System.lineSeparator());
                    ((EntityInfo)entry.getValue()).saveAsText(w, "\t\t");
                    w.write("\t}," + System.lineSeparator());
                }
                w.write("}" + System.lineSeparator());
                w.write(System.lineSeparator());
                w.write("--[[ ---- OBJECTS ---- --]]" + System.lineSeparator());
                w.write("objects = {" + System.lineSeparator());
                for (Map.Entry<Number, Object> entry : this.objectIdToNameMap.entrySet()) {
                    w.write("\t" + String.valueOf(entry.getKey()) + " = \"" + (String)entry.getValue() + "\"," + System.lineSeparator());
                }
                w.write("}" + System.lineSeparator());
                w.write(System.lineSeparator());
                w.write("--[[ ---- SPRITES ---- --]]" + System.lineSeparator());
                w.write("sprites = {" + System.lineSeparator());
                for (Map.Entry<Number, Object> entry : this.spriteIdToNameMap.entrySet()) {
                    w.write("\t" + String.valueOf(entry.getKey()) + " = \"" + (String)entry.getValue() + "\"," + System.lineSeparator());
                }
                w.write("}" + System.lineSeparator());
                w.write(System.lineSeparator());
                w.write("--[[ ---- STRINGS ---- --]]" + System.lineSeparator());
                w.write("strings = {" + System.lineSeparator());
                StringDictionary.saveAsText(w, "\t");
                w.write("}" + System.lineSeparator());
                w.write(System.lineSeparator());
                w.write("}" + System.lineSeparator());
                w.write("--[[ ---- SCRIPTS ---- --]]" + System.lineSeparator());
                w.write("scripts = {" + System.lineSeparator());
                ScriptsDictionary.saveAsText(w, "\t");
                w.write("}" + System.lineSeparator());
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw new WorldDictionaryException("Error saving WorldDictionary as text.", ex);
            }
        }
    }
}

