/*
 * Decompiled with CFR 0.152.
 */
package zombie.world;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.erosion.ErosionRegions;
import zombie.erosion.categories.ErosionCategory;
import zombie.gameStates.ChooseGameInfo;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiScript;
import zombie.world.DictionaryData;
import zombie.world.DictionaryDataClient;
import zombie.world.EntityInfo;
import zombie.world.ItemInfo;
import zombie.world.ScriptsDictionary;
import zombie.world.StringDictionary;
import zombie.world.WorldDictionaryException;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;

public class WorldDictionary {
    public static final int VERSION = 1;
    public static final String SAVE_FILE_READABLE = "WorldDictionaryReadable.lua";
    public static final String SAVE_FILE_LOG = "WorldDictionaryLog.lua";
    public static final String SAVE_FILE = "WorldDictionary";
    public static final String SAVE_EXT = ".bin";
    public static final boolean logUnset = false;
    public static final boolean logMissingObjectID = false;
    private static final Map<String, ItemInfo> itemLoadList = new HashMap<String, ItemInfo>();
    private static final Map<String, EntityInfo> entityLoadList = new HashMap<String, EntityInfo>();
    private static final List<String> objNameLoadList = new ArrayList<String>();
    private static DictionaryData data;
    private static boolean isNewGame;
    private static boolean allowScriptItemLoading;
    private static final String netValidator = "DICTIONARY_PACKET_END";
    private static byte[] clientRemoteData;

    protected static void log(String line) {
        WorldDictionary.log(line, true);
    }

    protected static void log(String line, boolean debugprint) {
        if (debugprint) {
            DebugLog.log("WorldDictionary: " + line);
        }
    }

    public static void setIsNewGame(boolean isNewGame) {
        WorldDictionary.isNewGame = isNewGame;
    }

    public static boolean isIsNewGame() {
        return isNewGame;
    }

    public static void StartScriptLoading() {
        allowScriptItemLoading = true;
        itemLoadList.clear();
        entityLoadList.clear();
        ScriptsDictionary.StartScriptLoading();
        StringDictionary.StartScriptLoading();
    }

    public static void ScriptsLoaded() {
        allowScriptItemLoading = false;
    }

    public static boolean isAllowScriptItemLoading() {
        return allowScriptItemLoading;
    }

    private static void onLoadItem(Item item) {
        if (GameClient.client) {
            return;
        }
        if (!allowScriptItemLoading) {
            WorldDictionary.log("Warning script item loaded after WorldDictionary is initialised");
            if (Core.debug) {
                throw new RuntimeException("This shouldn't be happening.");
            }
            return;
        }
        ItemInfo info = itemLoadList.get(item.getFullName());
        if (info == null) {
            info = new ItemInfo();
            info.name = item.getName();
            info.moduleName = item.getModuleName();
            info.fullType = item.getFullName();
            itemLoadList.put(item.getFullName(), info);
        }
        if (info.modId != null && !item.getModID().equals(info.modId)) {
            if (info.modOverrides == null) {
                info.modOverrides = new ArrayList();
            }
            if (!info.modOverrides.contains(info.modId)) {
                info.modOverrides.add(info.modId);
            } else {
                WorldDictionary.log("modOverrides for item '" + info.fullType + "' already contains mod id: " + info.modId);
            }
        }
        info.modId = item.getModID();
        if (info.modId.equals("pz-vanilla")) {
            info.existsAsVanilla = true;
        }
        info.isModded = !info.modId.equals("pz-vanilla");
        info.obsolete = item.getObsolete();
        info.scriptItem = item;
        info.entityScript = item;
    }

    public static void onLoadEntity(GameEntityScript entityScript) {
        if (GameClient.client) {
            return;
        }
        if (!allowScriptItemLoading) {
            WorldDictionary.log("Warning script entityScript loaded after WorldDictionary is initialised");
            if (Core.debug) {
                throw new RuntimeException("This shouldn't be happening.");
            }
            return;
        }
        if (entityScript instanceof Item) {
            Item item = (Item)entityScript;
            WorldDictionary.onLoadItem(item);
            return;
        }
        EntityInfo info = entityLoadList.get(entityScript.getFullName());
        if (info == null) {
            info = new EntityInfo();
            info.name = entityScript.getName();
            info.moduleName = entityScript.getModuleName();
            info.fullType = entityScript.getFullName();
            entityLoadList.put(entityScript.getFullName(), info);
        }
        if (info.modId != null && !entityScript.getModID().equals(info.modId)) {
            if (info.modOverrides == null) {
                info.modOverrides = new ArrayList();
            }
            if (!info.modOverrides.contains(info.modId)) {
                info.modOverrides.add(info.modId);
            } else {
                WorldDictionary.log("modOverrides for entityScript '" + info.fullType + "' already contains mod id: " + info.modId);
            }
        }
        info.modId = entityScript.getModID();
        if (info.modId.equals("pz-vanilla")) {
            info.existsAsVanilla = true;
        }
        info.isModded = !info.modId.equals("pz-vanilla");
        info.obsolete = entityScript.getObsolete();
        info.entityScript = entityScript;
    }

    private static void collectObjectNames() {
        objNameLoadList.clear();
        if (GameClient.client) {
            return;
        }
        ArrayList<String> names = new ArrayList<String>();
        for (int i = 0; i < ErosionRegions.regions.size(); ++i) {
            for (int j = 0; j < ErosionRegions.regions.get((int)i).categories.size(); ++j) {
                ErosionCategory cat = ErosionRegions.regions.get((int)i).categories.get(j);
                names.clear();
                cat.getObjectNames(names);
                for (String objName : names) {
                    if (objNameLoadList.contains(objName)) continue;
                    objNameLoadList.add(objName);
                }
            }
        }
    }

    private static void collectStrings() throws WorldDictionaryException {
        boolean scriptLoading = allowScriptItemLoading;
        allowScriptItemLoading = true;
        ArrayList<XuiScript> layouts = XuiManager.GetAllLayouts();
        for (XuiScript layout : layouts) {
            if (layout.getXuiLayoutName() == null) continue;
            StringDictionary.Generic.register(layout.getXuiLayoutName());
        }
        allowScriptItemLoading = scriptLoading;
    }

    public static void loadDataFromServer(ByteBufferReader bb) throws IOException {
        if (GameClient.client) {
            int dataLen = bb.getInt();
            clientRemoteData = new byte[dataLen];
            bb.get(clientRemoteData, 0, clientRemoteData.length);
        }
    }

    public static void saveDataForClient(ByteBufferWriter bb) throws IOException {
        if (GameServer.server) {
            int position1 = bb.position();
            bb.putInt(0);
            int position2 = bb.position();
            if (WorldDictionary.data.serverDataCache != null) {
                bb.put(WorldDictionary.data.serverDataCache);
            } else {
                if (Core.debug) {
                    throw new RuntimeException("Should be sending data from the serverDataCache here.");
                }
                data.saveToByteBuffer(bb.bb);
            }
            bb.putUTF(netValidator);
            int position3 = bb.position();
            bb.position(position1);
            bb.putInt(position3 - position2);
            bb.position(position3);
        }
    }

    public static void init() throws WorldDictionaryException {
        boolean proceedLoadingWorld;
        block13: {
            proceedLoadingWorld = true;
            WorldDictionary.collectObjectNames();
            WorldDictionaryLogger.startLogging();
            WorldDictionaryLogger.log("-------------------------------------------------------", false);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            WorldDictionaryLogger.log("Time: " + dateFormat.format(new Date()), false);
            WorldDictionary.log("Checking dictionary...");
            Log.Info logInfo = null;
            try {
                if (!GameClient.client) {
                    if (data == null || data.isClient()) {
                        data = new DictionaryData();
                    }
                } else if (data == null || !data.isClient()) {
                    data = new DictionaryDataClient();
                }
                data.reset();
                ScriptsDictionary.reset();
                StringDictionary.reset();
                WorldDictionary.collectStrings();
                if (GameClient.client) {
                    if (clientRemoteData == null) {
                        throw new WorldDictionaryException("WorldDictionary data not received from server.");
                    }
                    ByteBuffer bb = ByteBuffer.wrap(clientRemoteData);
                    data.loadFromByteBuffer(bb);
                    String end = GameWindow.ReadString(bb);
                    if (!end.equals(netValidator)) {
                        throw new WorldDictionaryException("WorldDictionary data received from server is corrupt.");
                    }
                    clientRemoteData = null;
                }
                data.backupCurrentDataSet();
                data.load();
                ArrayList<String> modIDs = new ArrayList<String>();
                logInfo = new Log.Info(dateFormat.format(new Date()), Core.gameSaveWorld, 244, modIDs);
                WorldDictionaryLogger.log(logInfo);
                data.parseInfoLoadList(itemLoadList);
                data.parseInfoLoadList(entityLoadList);
                data.parseCurrentInfoSet();
                itemLoadList.clear();
                entityLoadList.clear();
                data.parseObjectNameLoadList(objNameLoadList);
                objNameLoadList.clear();
                StringDictionary.parseRegisters();
                ScriptsDictionary.parseRegisters();
                data.getDictionaryMods(modIDs);
                data.saveAsText(SAVE_FILE_READABLE);
                data.save();
                data.deleteBackupCurrentDataSet();
            }
            catch (Exception e) {
                proceedLoadingWorld = false;
                e.printStackTrace();
                WorldDictionary.log("Warning: error occurred loading dictionary!");
                if (logInfo != null) {
                    logInfo.hasErrored = true;
                }
                if (data == null) break block13;
                data.createErrorBackups();
            }
        }
        try {
            WorldDictionaryLogger.saveLog(SAVE_FILE_LOG);
            WorldDictionaryLogger.reset();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (!proceedLoadingWorld) {
            throw new WorldDictionaryException("WorldDictionary: Cannot load world due to WorldDictionary error.");
        }
    }

    public static void onWorldLoaded() {
    }

    public static ItemInfo getItemInfoFromType(String fulltype) {
        return data.getItemInfoFromType(fulltype);
    }

    public static ItemInfo getItemInfoFromID(short registeryID) {
        return data.getItemInfoFromID(registeryID);
    }

    public static short getItemRegistryID(String fullType) {
        return data.getItemRegistryID(fullType);
    }

    public static String getItemTypeFromID(short id) {
        return data.getItemTypeFromID(id);
    }

    public static String getItemTypeDebugString(short id) {
        return data.getItemTypeDebugString(id);
    }

    public static EntityInfo getEntityInfoFromType(String fulltype) {
        return data.getEntityInfoFromType(fulltype);
    }

    public static EntityInfo getEntityInfoFromID(short registeryID) {
        return data.getEntityInfoFromID(registeryID);
    }

    public static short getEntityRegistryID(String fullType) {
        return data.getEntityRegistryID(fullType);
    }

    public static String getEntityTypeFromID(short id) {
        return data.getEntityTypeFromID(id);
    }

    public static String getEntityTypeDebugString(short id) {
        return data.getEntityTypeDebugString(id);
    }

    public static String getSpriteNameFromID(int id) {
        return data.getSpriteNameFromID(id);
    }

    public static int getIdForSpriteName(String name) {
        return data.getIdForSpriteName(name);
    }

    public static String getObjectNameFromID(byte id) {
        return data.getObjectNameFromID(id);
    }

    public static byte getIdForObjectName(String name) {
        return data.getIdForObjectName(name);
    }

    public static String getItemModID(short registeryID) {
        ItemInfo info = WorldDictionary.getItemInfoFromID(registeryID);
        if (info != null) {
            return info.modId;
        }
        return null;
    }

    public static String getItemModID(String fulltype) {
        ItemInfo info = WorldDictionary.getItemInfoFromType(fulltype);
        if (info != null) {
            return info.modId;
        }
        return null;
    }

    public static String getModNameFromID(String modid) {
        if (modid != null) {
            if (modid.equals("pz-vanilla")) {
                return "Project Zomboid";
            }
            ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modid);
            if (mod != null && mod.getName() != null) {
                return mod.getName();
            }
        }
        return "Unknown mod";
    }

    public static void DebugPrintItem(InventoryItem item) {
        Item script = item.getScriptItem();
        if (script != null) {
            WorldDictionary.DebugPrintItem(script);
        } else {
            String type = item.getFullType();
            ItemInfo info = null;
            if (type != null) {
                info = WorldDictionary.getItemInfoFromType(type);
            }
            if (info == null && item.getRegistry_id() >= 0) {
                info = WorldDictionary.getItemInfoFromID(item.getRegistry_id());
            }
            if (info != null) {
                info.DebugPrint();
            } else {
                DebugLog.log("WorldDictionary: Cannot debug print item: " + (type != null ? type : "unknown"));
            }
        }
    }

    public static void DebugPrintItem(Item item) {
        String type = item.getFullName();
        ItemInfo info = null;
        if (type != null) {
            info = WorldDictionary.getItemInfoFromType(type);
        }
        if (info == null && item.getRegistry_id() >= 0) {
            info = WorldDictionary.getItemInfoFromID(item.getRegistry_id());
        }
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print item: " + (type != null ? type : "unknown"));
        }
    }

    public static void DebugPrintItem(String fullitem) {
        ItemInfo info = WorldDictionary.getItemInfoFromType(fullitem);
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print item: " + fullitem);
        }
    }

    public static void DebugPrintItem(short id) {
        ItemInfo info = WorldDictionary.getItemInfoFromID(id);
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print item id: " + id);
        }
    }

    public static String getEntityModID(short registeryID) {
        EntityInfo info = WorldDictionary.getEntityInfoFromID(registeryID);
        if (info != null) {
            return info.modId;
        }
        return null;
    }

    public static String getEntityModID(String fulltype) {
        EntityInfo info = WorldDictionary.getEntityInfoFromType(fulltype);
        if (info != null) {
            return info.modId;
        }
        return null;
    }

    public static void DebugPrintEntity(GameEntity entity) {
        if (entity instanceof InventoryItem) {
            InventoryItem inventoryItem = (InventoryItem)entity;
            WorldDictionary.DebugPrintItem(inventoryItem);
            return;
        }
        if (Core.debug) {
            throw new RuntimeException("Not implemented yet.");
        }
    }

    public static void DebugPrintEntity(GameEntityScript entityScript) {
        String type = entityScript.getFullName();
        EntityInfo info = null;
        if (type != null) {
            info = WorldDictionary.getEntityInfoFromType(type);
        }
        if (info == null && entityScript.getRegistry_id() >= 0) {
            info = WorldDictionary.getEntityInfoFromID(entityScript.getRegistry_id());
        }
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print entity: " + (type != null ? type : "unknown"));
        }
    }

    public static void DebugPrintEntity(String fullitem) {
        EntityInfo info = WorldDictionary.getEntityInfoFromType(fullitem);
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print entity: " + fullitem);
        }
    }

    public static void DebugPrintEntity(short id) {
        EntityInfo info = WorldDictionary.getEntityInfoFromID(id);
        if (info != null) {
            info.DebugPrint();
        } else {
            DebugLog.log("WorldDictionary: Cannot debug print entity id: " + id);
        }
    }

    static {
        isNewGame = true;
    }
}

