/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.ZomboidFileSystem;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferReader;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionConfig;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.globalObjects.CGlobalObjects;
import zombie.iso.IsoChunkMap;
import zombie.network.ConnectionManager;
import zombie.network.CoopMaster;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.world.WorldDictionary;

public final class ConnectToServerState
extends GameState {
    public static ConnectToServerState instance;
    private final ByteBufferReader connectionDetails;
    private State state;
    private final ArrayList<WorkshopItem> workshopItems = new ArrayList();
    private final ArrayList<WorkshopItem> confirmItems = new ArrayList();
    private ItemQuery query;

    private static void noise(String s) {
        DebugLog.log("ConnectToServerState: " + s);
    }

    public ConnectToServerState(ByteBufferReader bb) {
        this.connectionDetails = new ByteBufferReader(ByteBuffer.allocate(bb.capacity()));
        this.connectionDetails.put(bb.bb);
        this.connectionDetails.rewind();
    }

    @Override
    public void enter() {
        instance = this;
        ConnectionManager.log("connect-state", State.Start.name().toLowerCase(), GameClient.connection);
        this.state = State.Start;
    }

    @Override
    public GameStateMachine.StateAction update() {
        try {
            switch (this.state.ordinal()) {
                case 0: {
                    this.Start();
                    break;
                }
                case 1: {
                    this.TestTCP();
                    break;
                }
                case 2: {
                    this.WorkshopInit();
                    break;
                }
                case 3: {
                    this.WorkshopQuery();
                    break;
                }
                case 4: {
                    this.WorkshopConfirm();
                    break;
                }
                case 5: {
                    this.ServerWorkshopItemScreen();
                    break;
                }
                case 6: {
                    this.WorkshopUpdate();
                    break;
                }
                case 7: {
                    this.CheckMods();
                    break;
                }
                case 8: {
                    this.Finish();
                    break;
                }
                case 9: {
                    return GameStateMachine.StateAction.Continue;
                }
            }
            return GameStateMachine.StateAction.Remain;
        }
        catch (Exception e) {
            DebugLog.General.printStackTrace();
            return GameStateMachine.StateAction.Continue;
        }
    }

    private void receiveStartLocation(ByteBufferReader bb) {
        LuaEventManager.triggerEvent("OnConnectionStateChanged", "Connected");
        IsoChunkMap.mpWorldXa = bb.getInt();
        IsoChunkMap.mpWorldYa = bb.getInt();
        IsoChunkMap.mpWorldZa = bb.getInt();
        GameClient.username = GameClient.username.trim();
        Core.getInstance().setGameMode("Multiplayer");
        LuaManager.GlobalObject.createWorld(Core.gameSaveWorld);
        GameClient.instance.connected = true;
    }

    private void receiveServerOptions(ByteBufferReader bb) throws IOException {
        int optionsSize = bb.getInt();
        for (int i = 0; i < optionsSize; ++i) {
            String option = bb.getUTF();
            String value = bb.getUTF();
            ServerOptions.instance.putOption(option, value);
        }
        Core.getInstance().ResetLua("client", "ConnectedToServer");
        Core.getInstance().setGameMode("Multiplayer");
        GameClient.connection.setIP(GameClient.ip);
    }

    private void receiveSandboxOptions(ByteBufferReader bb) throws IOException {
        SandboxOptions.instance.load(bb.bb);
        SandboxOptions.instance.applySettings();
        SandboxOptions.instance.toLua();
    }

    private void receiveGameTime(ByteBufferReader bb) throws IOException {
        GameTime.getInstance().load(bb);
        GameTime.getInstance().save();
    }

    private void receiveErosionMain(ByteBufferReader bb) {
        GameClient.instance.erosionConfig = new ErosionConfig();
        GameClient.instance.erosionConfig.load(bb);
    }

    private void receiveGlobalObjects(ByteBufferReader bb) throws IOException {
        CGlobalObjects.loadInitialState(bb);
    }

    private void receiveResetID(ByteBufferReader bb) {
        int resetId = bb.getInt();
        GameClient.instance.setResetID(resetId);
    }

    private void receiveBerries(ByteBufferReader bb) {
        Core.getInstance().setPoisonousBerry(bb.getUTF());
        GameClient.poisonousBerry = Core.getInstance().getPoisonousBerry();
        Core.getInstance().setPoisonousMushroom(bb.getUTF());
        GameClient.poisonousMushroom = Core.getInstance().getPoisonousMushroom();
    }

    private void receiveWorldDictionary(ByteBufferReader bb) throws IOException {
        WorldDictionary.loadDataFromServer(bb);
        ClientPlayerDB.setAllow(true);
        LuaEventManager.triggerEvent("OnConnected");
    }

    private void Start() {
        ConnectToServerState.noise("Start");
        ByteBufferReader bb = this.connectionDetails;
        GameClient.connection.isCoopHost = bb.getBoolean();
        GameClient.connection.setMaxPlayers(bb.getInt());
        if (bb.getBoolean()) {
            long hostSteamID = bb.getLong();
            String serverName = bb.getUTF();
            Core.gameSaveWorld = hostSteamID + "_" + serverName + "_player";
        }
        GameClient.instance.id = bb.getByte();
        ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.TestTCP.name().toLowerCase(), GameClient.connection);
        this.state = State.TestTCP;
    }

    private void TestTCP() {
        ConnectToServerState.noise("TestTCP");
        ByteBufferReader bb = this.connectionDetails;
        GameClient.connection.setRole(new Role(""));
        GameClient.connection.getRole().parse(bb);
        if (Core.debug && !GameClient.connection.getRole().hasCapability(Capability.ConnectWithDebug) && !CoopMaster.instance.isRunning()) {
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_DebugNotAllowed"));
            GameClient.connection.forceDisconnect("connect-debug-used");
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.Exit.name().toLowerCase(), GameClient.connection);
            this.state = State.Exit;
            return;
        }
        GameClient.gameMap = bb.getUTF();
        if (GameClient.gameMap.contains(";")) {
            String[] ss = GameClient.gameMap.split(";");
            Core.gameMap = ss[0].trim();
        } else {
            Core.gameMap = GameClient.gameMap.trim();
        }
        if (SteamUtils.isSteamModeEnabled()) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopInit.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopInit;
        } else {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.CheckMods.name().toLowerCase(), GameClient.connection);
            this.state = State.CheckMods;
        }
    }

    private void WorkshopInit() {
        ByteBufferReader bb = this.connectionDetails;
        int count = bb.getShort();
        for (int i = 0; i < count; ++i) {
            long itemID = bb.getLong();
            long timeStamp = bb.getLong();
            WorkshopItem item = new WorkshopItem(itemID, timeStamp);
            this.workshopItems.add(item);
        }
        if (this.workshopItems.isEmpty()) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopUpdate.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopUpdate;
            return;
        }
        long[] itemIDs = new long[this.workshopItems.size()];
        for (int i = 0; i < this.workshopItems.size(); ++i) {
            WorkshopItem item = this.workshopItems.get(i);
            itemIDs[i] = item.id;
        }
        this.query = new ItemQuery(this);
        this.query.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(itemIDs, this.query);
        if (this.query.handle != 0L) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopQuery.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopQuery;
        } else {
            this.query = null;
            LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_CreateQueryUGCDetailsRequest"));
            GameClient.connection.forceDisconnect("connect-workshop-query");
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.Exit.name().toLowerCase(), GameClient.connection);
            this.state = State.Exit;
        }
    }

    private void WorkshopConfirm() {
        this.confirmItems.clear();
        for (int i = 0; i < this.workshopItems.size(); ++i) {
            WorkshopItem item = this.workshopItems.get(i);
            long itemState = SteamWorkshop.instance.GetItemState(item.id);
            ConnectToServerState.noise("WorkshopConfirm GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + item.id);
            if (SteamWorkshopItem.ItemState.Installed.and(itemState) && SteamWorkshopItem.ItemState.NeedsUpdate.not(itemState) && item.details != null && item.details.getTimeCreated() != 0L && item.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(item.id)) {
                ConnectToServerState.noise("Installed status but timeUpdated doesn't match!!!");
                itemState |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }
            if (itemState == (long)(SteamWorkshopItem.ItemState.Subscribed.getValue() | SteamWorkshopItem.ItemState.Installed.getValue())) continue;
            this.confirmItems.add(item);
        }
        if (this.confirmItems.isEmpty()) {
            this.query = null;
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopUpdate.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopUpdate;
        } else if (this.query == null) {
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopUpdate.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopUpdate;
        } else {
            assert (this.query.isCompleted());
            ArrayList<String> itemIDstr = new ArrayList<String>();
            for (int i = 0; i < this.workshopItems.size(); ++i) {
                WorkshopItem item = this.workshopItems.get(i);
                itemIDstr.add(SteamUtils.convertSteamIDToString(item.id));
            }
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Required", itemIDstr);
            ArrayList<SteamUGCDetails> details = this.query.details;
            this.query = null;
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.ServerWorkshopItemScreen.name().toLowerCase(), GameClient.connection);
            this.state = State.ServerWorkshopItemScreen;
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Details", details);
        }
    }

    private void WorkshopQuery() {
        if (this.query.isCompleted()) {
            block0: for (SteamUGCDetails details : this.query.details) {
                for (WorkshopItem workshopItem : this.workshopItems) {
                    if (workshopItem.id != details.getID()) continue;
                    workshopItem.details = details;
                    continue block0;
                }
            }
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.WorkshopConfirm.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopConfirm;
            return;
        }
        if (this.query.isNotCompleted()) {
            this.query = null;
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.ServerWorkshopItemScreen.name().toLowerCase(), GameClient.connection);
            this.state = State.ServerWorkshopItemScreen;
            LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", "ItemQueryNotCompleted");
            return;
        }
    }

    private void ServerWorkshopItemScreen() {
    }

    private void WorkshopUpdate() {
        for (int i = 0; i < this.workshopItems.size(); ++i) {
            WorkshopItem item = this.workshopItems.get(i);
            item.update();
            if (item.state == WorkshopItemState.Fail) {
                ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.ServerWorkshopItemScreen.name().toLowerCase(), GameClient.connection);
                this.state = State.ServerWorkshopItemScreen;
                LuaEventManager.triggerEvent("OnServerWorkshopItems", "Error", item.id, item.error);
                return;
            }
            if (item.state == WorkshopItemState.Ready) continue;
            return;
        }
        ZomboidFileSystem.instance.resetModFolders();
        LuaEventManager.triggerEvent("OnServerWorkshopItems", "Success");
        ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.CheckMods.name().toLowerCase(), GameClient.connection);
        this.state = State.CheckMods;
    }

    private void CheckMods() {
        ByteBufferReader bb = this.connectionDetails;
        ArrayList<String> mods = new ArrayList<String>();
        HashMap<String, String> modIdToWorkshopId = new HashMap<String, String>();
        HashMap<String, String> modIdToModName = new HashMap<String, String>();
        int modCount = bb.getInt();
        for (int i = 0; i < modCount; ++i) {
            String id = bb.getUTF();
            String workshopId = bb.getUTF();
            String name = bb.getUTF();
            mods.add(id);
            modIdToWorkshopId.put(id, workshopId);
            modIdToModName.put(id, name);
        }
        GameClient.instance.serverMods.clear();
        GameClient.instance.serverMods.addAll(mods);
        mods.clear();
        String missingMod = ZomboidFileSystem.instance.loadModsAux(GameClient.instance.serverMods, mods);
        if (missingMod != null) {
            String error = Translator.getText("UI_OnConnectFailed_ModRequired", modIdToModName.get(missingMod));
            String errorMessage = "%s [ModID: %s, WorkshopID: %s]".formatted(error, missingMod, modIdToWorkshopId.get(missingMod));
            LuaEventManager.triggerEvent("OnConnectFailed", errorMessage);
            GameClient.connection.forceDisconnect("connect-mod-required");
            ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.Exit.name().toLowerCase(), GameClient.connection);
            this.state = State.Exit;
            return;
        }
        ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.Finish.name().toLowerCase(), GameClient.connection);
        this.state = State.Finish;
    }

    private void Finish() {
        ByteBufferReader bb = this.connectionDetails;
        try {
            try {
                this.receiveStartLocation(bb);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "receiveStartLocation error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveServerOptions(bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "receiveServerOptions error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveSandboxOptions(bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "receiveSandboxOptions error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveGameTime(bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "receiveGameTime error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveErosionMain(bb);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "receiveErosionMain error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveGlobalObjects(bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "receiveGlobalObjects error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveResetID(bb);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "receiveResetID error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveBerries(bb);
            }
            catch (Exception e) {
                DebugLog.Multiplayer.printException(e, "receiveBerries error", LogSeverity.Error);
                throw e;
            }
            try {
                this.receiveWorldDictionary(bb);
            }
            catch (IOException e) {
                DebugLog.Multiplayer.printException(e, "receiveWorldDictionary error", LogSeverity.Error);
                throw e;
            }
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
            LuaEventManager.triggerEvent("OnConnectFailed", "WorldDictionary error");
            GameClient.connection.forceDisconnect("connection-details-error");
        }
        ConnectionManager.log("connect-state-" + this.state.name().toLowerCase(), State.Exit.name().toLowerCase(), GameClient.connection);
        this.state = State.Exit;
    }

    public void FromLua(String button) {
        if (this.state != State.ServerWorkshopItemScreen) {
            throw new IllegalStateException("state != ServerWorkshopItemScreen");
        }
        if ("install".equals(button)) {
            ConnectionManager.log("connect-state-lua-" + this.state.name().toLowerCase(), State.WorkshopUpdate.name().toLowerCase(), GameClient.connection);
            this.state = State.WorkshopUpdate;
            return;
        }
        if ("disconnect".equals(button)) {
            LuaEventManager.triggerEvent("OnConnectFailed", "ServerWorkshopItemsCancelled");
            if (GameClient.connection != null) {
                GameClient.connection.forceDisconnect("connect-workshop-canceled");
            }
            ConnectionManager.log("connect-state-lua-" + this.state.name().toLowerCase(), State.Exit.name().toLowerCase(), GameClient.connection);
            this.state = State.Exit;
            return;
        }
    }

    @Override
    public void exit() {
        instance = null;
    }

    private static enum State {
        Start,
        TestTCP,
        WorkshopInit,
        WorkshopQuery,
        WorkshopConfirm,
        ServerWorkshopItemScreen,
        WorkshopUpdate,
        CheckMods,
        Finish,
        Exit;

    }

    private static final class WorkshopItem
    implements ISteamWorkshopCallback {
        long id;
        long serverTimeStamp;
        WorkshopItemState state = WorkshopItemState.CheckItemState;
        boolean subscribed;
        long downloadStartTime;
        long downloadQueryTime;
        String error;
        SteamUGCDetails details;

        WorkshopItem(long id, long serverTimeStamp) {
            this.id = id;
            this.serverTimeStamp = serverTimeStamp;
        }

        void update() {
            switch (this.state.ordinal()) {
                case 0: {
                    this.CheckItemState();
                    break;
                }
                case 1: {
                    this.SubscribePending();
                    break;
                }
                case 2: {
                    this.DownloadPending();
                    break;
                }
            }
        }

        void setState(WorkshopItemState newState) {
            ConnectToServerState.noise("item state " + String.valueOf((Object)this.state) + " -> " + String.valueOf((Object)newState) + " ID=" + this.id);
            this.state = newState;
        }

        void CheckItemState() {
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            ConnectToServerState.noise("GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            if (!SteamWorkshopItem.ItemState.Subscribed.and(itemState)) {
                if (SteamWorkshop.instance.SubscribeItem(this.id, this)) {
                    this.setState(WorkshopItemState.SubscribePending);
                    return;
                }
                this.error = "SubscribeItemFalse";
                this.setState(WorkshopItemState.Fail);
                return;
            }
            if (SteamWorkshopItem.ItemState.Installed.and(itemState) && SteamWorkshopItem.ItemState.NeedsUpdate.not(itemState) && this.details != null && this.details.getTimeCreated() != 0L && this.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(this.id)) {
                ConnectToServerState.noise("Installed status but timeUpdated doesn't match!!!");
                itemState |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }
            if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                if (SteamWorkshop.instance.DownloadItem(this.id, true, this)) {
                    this.setState(WorkshopItemState.DownloadPending);
                    this.downloadStartTime = System.currentTimeMillis();
                    return;
                }
                this.error = "DownloadItemFalse";
                this.setState(WorkshopItemState.Fail);
                return;
            }
            if (SteamWorkshopItem.ItemState.Installed.and(itemState)) {
                long timeStamp = SteamWorkshop.instance.GetItemInstallTimeStamp(this.id);
                if (timeStamp == 0L) {
                    this.error = "GetItemInstallTimeStamp";
                    this.setState(WorkshopItemState.Fail);
                    return;
                }
                if (timeStamp != this.serverTimeStamp) {
                    this.error = "VersionMismatch";
                    this.setState(WorkshopItemState.Fail);
                    return;
                }
                this.setState(WorkshopItemState.Ready);
                return;
            }
            this.error = "UnknownItemState";
            this.setState(WorkshopItemState.Fail);
        }

        void SubscribePending() {
            long itemState;
            if (this.subscribed && SteamWorkshopItem.ItemState.Subscribed.and(itemState = SteamWorkshop.instance.GetItemState(this.id))) {
                this.setState(WorkshopItemState.CheckItemState);
            }
        }

        void DownloadPending() {
            long time = System.currentTimeMillis();
            if (this.downloadQueryTime + 100L > time) {
                return;
            }
            this.downloadQueryTime = time;
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                long[] progress = new long[2];
                if (SteamWorkshop.instance.GetItemDownloadInfo(this.id, progress)) {
                    ConnectToServerState.noise("download " + progress[0] + "/" + progress[1] + " ID=" + this.id);
                    LuaEventManager.triggerEvent("OnServerWorkshopItems", "Progress", SteamUtils.convertSteamIDToString(this.id), progress[0], Math.max(progress[1], 1L));
                }
                return;
            }
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
            ConnectToServerState.noise("onItemSubscribed itemID=" + itemID);
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.subscribed = true;
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
            ConnectToServerState.noise("onItemNotSubscribed itemID=" + itemID + " result=" + result);
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.error = "ItemNotSubscribed";
            this.setState(WorkshopItemState.Fail);
        }

        @Override
        public void onItemDownloaded(long itemID) {
            ConnectToServerState.noise("onItemDownloaded itemID=" + itemID + " time=" + (System.currentTimeMillis() - this.downloadStartTime) + " ms");
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.setState(WorkshopItemState.CheckItemState);
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
            ConnectToServerState.noise("onItemNotDownloaded itemID=" + itemID + " result=" + result);
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.error = "ItemNotDownloaded";
            this.setState(WorkshopItemState.Fail);
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
        }
    }

    private class ItemQuery
    implements ISteamWorkshopCallback {
        long handle;
        ArrayList<SteamUGCDetails> details;
        boolean completed;
        boolean notCompleted;

        private ItemQuery(ConnectToServerState connectToServerState) {
            Objects.requireNonNull(connectToServerState);
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public boolean isNotCompleted() {
            return this.notCompleted;
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
        }

        @Override
        public void onItemDownloaded(long itemID) {
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
            ConnectToServerState.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
            if (handle != this.handle) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            ArrayList<SteamUGCDetails> detailsList = new ArrayList<SteamUGCDetails>();
            for (int i = 0; i < numResults; ++i) {
                SteamUGCDetails details = SteamWorkshop.instance.GetQueryUGCResult(handle, i);
                if (details == null) continue;
                detailsList.add(details);
            }
            this.details = detailsList;
            SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
            this.completed = true;
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
            ConnectToServerState.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
            if (handle != this.handle) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
            this.notCompleted = true;
        }
    }

    private static enum WorkshopItemState {
        CheckItemState,
        SubscribePending,
        DownloadPending,
        Ready,
        Fail;

    }
}

