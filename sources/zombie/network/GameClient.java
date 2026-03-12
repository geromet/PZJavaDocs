/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.javafmod;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TShortObjectHashMap;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.codec.binary.Base32;
import org.joml.Vector3f;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientStreamManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.SharedDescriptors;
import zombie.SystemDisabler;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Safety;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.chat.ChatManager;
import zombie.commands.serverCommands.ListCommand;
import zombie.commands.serverCommands.LogCommand;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.IsoPropertyType;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
import zombie.core.raknet.VoiceManager;
import zombie.core.raknet.VoiceManagerData;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.erosion.ErosionConfig;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.iso.zones.Zone;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.CoopMaster;
import zombie.network.DBBannedIP;
import zombie.network.DBBannedSteamID;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.Server;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.TableNetworkUtils;
import zombie.network.ZomboidNetData;
import zombie.network.ZomboidNetDataPool;
import zombie.network.packets.AddBrokenGlassPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.KickedPacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.RequestTradingPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.SledgehammerDestroyPacket;
import zombie.network.packets.TradingUIAddItemPacket;
import zombie.network.packets.TradingUIRemoveItemPacket;
import zombie.network.packets.TradingUIUpdateStatePacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.actions.EatFoodPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.actions.SneezeCoughPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.network.packets.connection.LoadPlayerProfilePacket;
import zombie.network.packets.hit.AnimalHitAnimalPacket;
import zombie.network.packets.hit.AnimalHitPlayerPacket;
import zombie.network.packets.hit.AnimalHitThumpablePacket;
import zombie.network.packets.hit.AttackCollisionCheckPacket;
import zombie.network.packets.hit.VehicleHitAnimalPacket;
import zombie.network.packets.hit.VehicleHitPlayerPacket;
import zombie.network.packets.hit.VehicleHitZombiePacket;
import zombie.network.packets.hit.ZombieHitPlayerPacket;
import zombie.network.packets.hit.ZombieHitThumpablePacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.StopSoundPacket;
import zombie.network.statistics.PingManager;
import zombie.network.statistics.StatisticManager;
import zombie.popman.NetworkZombieSimulator;
import zombie.popman.ZombieCountOptimiser;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.savefile.SavefileThumbnail;
import zombie.util.AddCoopPlayer;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;
import zombie.worldMap.network.WorldMapClient;

public class GameClient {
    public static final GameClient instance = new GameClient();
    public static final int DEFAULT_PORT = 16361;
    public static boolean client;
    public static boolean clientSave;
    public static UdpConnection connection;
    public static int count;
    public static String ip;
    public static String serverName;
    public static String localIP;
    public static String password;
    public static String googleKey;
    public static boolean allChatMuted;
    public static String username;
    public static String serverPassword;
    public static boolean useSteamRelay;
    public static int authType;
    public UdpEngine udpEngine;
    public byte id = (byte)-1;
    public float timeSinceKeepAlive;
    UpdateLimit itemSendFrequency = new UpdateLimit(3000L);
    public static int port;
    public boolean playerConnectSent;
    private boolean clientStarted;
    private int resetId;
    private boolean connectionLost;
    public static String checksum;
    public static boolean checksumValid;
    public static List<Long> pingsList;
    public static String gameMap;
    public static boolean fastForward;
    public static final ClientServerMap[] loadedCells;
    public static final int DEBUG_PING = 5;
    public static boolean coopInvite;
    public final ArrayList<IsoPlayer> connectedPlayers = new ArrayList();
    private static boolean isPaused;
    private final ArrayList<IsoPlayer> players = new ArrayList();
    public boolean idMapDirty = true;
    public static final int sendZombieWithoutNeighbor = 4000;
    public static final int sendZombieWithNeighbor = 200;
    public final UpdateLimit sendZombieTimer = new UpdateLimit(4000L);
    public final UpdateLimit sendZombieRequestsTimer = new UpdateLimit(200L);
    private final UpdateLimit updateChannelsRoamingLimit = new UpdateLimit(3010L);
    private long disconnectTime = System.currentTimeMillis();
    private static final long disconnectTimeLimit = 10000L;
    public static long steamID;
    private long clientCycle = System.currentTimeMillis();
    private long clientCycleLast;
    public static final Map<Short, Vector2> positions;
    private int safehouseUpdateTimer;
    private final Vector3f vehicle1PositionVector = new Vector3f();
    private final Vector3f vehicle2PositionVector = new Vector3f();
    private final Vector3f vehicle1VelocityVector = new Vector3f();
    private final Vector3f vehicle2VelocityVector = new Vector3f();
    private boolean delayPacket;
    private final ArrayList<Integer> delayedDisconnect = new ArrayList();
    private static final TShortArrayList tempShortList;
    private volatile RequestState request;
    public KahluaTable serverSpawnRegions;
    private static final ConcurrentLinkedQueue<ZomboidNetData> MainLoopNetDataQ;
    private static final ArrayList<ZomboidNetData> MainLoopNetData;
    private static final ArrayList<ZomboidNetData> LoadingMainLoopNetData;
    private static final ArrayList<ZomboidNetData> DelayedCoopNetData;
    public boolean connected;
    public int timeSinceLastUpdate;
    private final ByteBuffer staticTest = ByteBuffer.allocate(20000);
    private final ByteBufferWriter wr = new ByteBufferWriter(this.staticTest);
    private final long startHeartMilli = 0L;
    private final long endHeartMilli = 0L;
    public int ping;
    public static float serverPredictedAhead;
    public static final HashMap<Short, IsoPlayer> IDToPlayerMap;
    public static final TShortObjectHashMap<IsoZombie> IDToZombieMap;
    public static boolean ingame;
    public static boolean askPing;
    public static boolean askCustomizationData;
    public static boolean sendQR;
    public final ArrayList<String> serverMods = new ArrayList();
    public ErosionConfig erosionConfig;
    public static Calendar startAuth;
    public static String poisonousBerry;
    public static String poisonousMushroom;
    private final HashMap<ItemContainer, ArrayList<InventoryItem>> itemsToSend = new HashMap();
    private final HashMap<ItemContainer, ArrayList<InventoryItem>> itemsToSendRemove = new HashMap();

    public IsoPlayer getPlayerByOnlineID(short id) {
        return IDToPlayerMap.get(id);
    }

    public void init() {
        LoadingMainLoopNetData.clear();
        MainLoopNetDataQ.clear();
        MainLoopNetData.clear();
        DelayedCoopNetData.clear();
        ingame = false;
        IDToPlayerMap.clear();
        IDToZombieMap.clear();
        pingsList.clear();
        this.itemsToSend.clear();
        this.itemsToSendRemove.clear();
        IDToZombieMap.setAutoCompactionFactor(0.0f);
        this.playerConnectSent = false;
        this.connectionLost = false;
        this.delayedDisconnect.clear();
        GameWindow.serverDisconnected = false;
        this.serverSpawnRegions = null;
        this.startClient();
    }

    public void startClient() {
        if (this.clientStarted) {
            this.udpEngine.Connect(ip, port, serverPassword, useSteamRelay);
            return;
        }
        try {
            this.udpEngine = new UdpEngine(Rand.Next(10000) + 12345, 0, 1, null, false);
            if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
                this.udpEngine.Connect("127.0.0.1", CoopMaster.instance.getServerPort(), serverPassword, false);
            } else {
                this.udpEngine.Connect(ip, port, serverPassword, useSteamRelay);
            }
            this.clientStarted = true;
        }
        catch (Exception e) {
            DebugLog.Network.printException(e, "Exception thrown during GameClient.startClient.", LogSeverity.Error);
        }
    }

    public String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    public String getGoogleAuthenticatorBarCode(String secretKey, String account, String issuer) {
        try {
            return "otpauth://totp/" + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20") + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20") + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getQR(String name, String key) {
        String proj = "Zomboid";
        String barCodeUrl = this.getGoogleAuthenticatorBarCode(key, name, "Zomboid");
        DebugLog.General.println(barCodeUrl);
        return barCodeUrl;
    }

    public void Shutdown() {
        if (this.clientStarted) {
            this.udpEngine.Shutdown();
            this.clientStarted = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        ZombieCountOptimiser.startCount();
        if (this.safehouseUpdateTimer == 0 && ServerOptions.instance.disableSafehouseWhenPlayerConnected.getValue()) {
            this.safehouseUpdateTimer = 3000;
            SafeHouse.updateSafehousePlayersConnected();
        }
        if (this.safehouseUpdateTimer > 0) {
            --this.safehouseUpdateTimer;
        }
        Object data = MainLoopNetDataQ.poll();
        while (data != null) {
            MainLoopNetData.add((ZomboidNetData)data);
            data = MainLoopNetDataQ.poll();
        }
        data = this.delayedDisconnect;
        synchronized (data) {
            while (!this.delayedDisconnect.isEmpty()) {
                int packet = this.delayedDisconnect.remove(0);
                switch (packet) {
                    case 21: {
                        LuaEventManager.triggerEvent("OnDisconnect");
                        break;
                    }
                    case 18: {
                        LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_AlreadyConnected"));
                        break;
                    }
                    case 32: {
                        LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_ConnectionLost"));
                        break;
                    }
                    case 23: {
                        LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_Banned"));
                        break;
                    }
                    case 17: {
                        if (SteamUtils.isSteamModeEnabled()) break;
                        LuaEventManager.triggerEvent("OnConnectFailed", null);
                        break;
                    }
                    case 24: {
                        LuaEventManager.triggerEvent("OnConnectFailed", Translator.getText("UI_OnConnectFailed_InvalidServerPassword"));
                    }
                }
            }
        }
        if (this.connectionLost) {
            if (!this.playerConnectSent) {
                for (int n = 0; n < MainLoopNetData.size(); ++n) {
                    ZomboidNetData data2 = MainLoopNetData.get(n);
                    this.gameLoadingDealWithNetData(data2);
                }
                MainLoopNetData.clear();
            } else {
                for (int n = 0; n < MainLoopNetData.size(); ++n) {
                    ZomboidNetData data3 = MainLoopNetData.get(n);
                    if (data3.type != PacketTypes.PacketType.Kicked) continue;
                    KickedPacket packet = new KickedPacket();
                    packet.parse(data3.buffer, null);
                    GameWindow.kickReason = packet.getMessage();
                    DebugLog.Multiplayer.warn("ReceiveKickedDisconnect: " + packet.reason);
                }
                MainLoopNetData.clear();
            }
            GameWindow.serverDisconnected = true;
            connection = null;
            ConnectionManager.getInstance().process();
            return;
        }
        if (!this.playerConnectSent) {
            for (int n = 0; n < MainLoopNetData.size(); ++n) {
                ZomboidNetData data4 = MainLoopNetData.get(n);
                if (this.gameLoadingDealWithNetData(data4)) continue;
                LoadingMainLoopNetData.add(data4);
            }
            MainLoopNetData.clear();
            WorldStreamer.instance.updateMain();
            return;
        }
        if (!LoadingMainLoopNetData.isEmpty()) {
            DebugLog.log(DebugType.Network, "Processing delayed packets...");
            MainLoopNetData.addAll(0, LoadingMainLoopNetData);
            LoadingMainLoopNetData.clear();
        }
        if (!DelayedCoopNetData.isEmpty() && IsoWorld.instance.addCoopPlayers.isEmpty()) {
            DebugLog.log(DebugType.Network, "Processing delayed coop packets...");
            MainLoopNetData.addAll(0, DelayedCoopNetData);
            DelayedCoopNetData.clear();
        }
        long time = System.currentTimeMillis();
        for (int n = 0; n < MainLoopNetData.size(); ++n) {
            ZomboidNetData data5 = MainLoopNetData.get(n);
            if (data5.time + 5L > time) continue;
            this.mainLoopDealWithNetData(data5);
            MainLoopNetData.remove(n--);
        }
        for (IsoPlayer player : this.getPlayers()) {
            if (player == null || player.isLocalPlayer() || System.currentTimeMillis() - player.getLastRemoteUpdate() <= 5000L) continue;
            GameClient.receivePlayerTimeout(player.getOnlineID());
        }
        try {
            this.sendAddedRemovedItems(false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            ExceptionLogger.logException(ex);
        }
        if (this.updateChannelsRoamingLimit.Check()) {
            VoiceManager.getInstance().UpdateChannelsRoaming(connection);
        }
        this.updateVehiclesAnticlipping();
        WorldStreamer.instance.updateMain();
        this.timeSinceKeepAlive += GameTime.getInstance().getMultiplier();
        connection.checkReady();
        ChatManager.UpdateClient();
        this.clientCycleLast = this.clientCycle;
        this.clientCycle = System.currentTimeMillis();
        long dif = this.clientCycle - this.clientCycleLast;
        StatisticManager.getInstance().update(dif);
    }

    private void updateVehiclesAnticlipping() {
        if (!DebugOptions.instance.multiplayer.debug.anticlippingAlgorithm.getValue()) {
            return;
        }
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        for (int i = 0; i < vehicles.size(); ++i) {
            VehicleInterpolationData data;
            BaseVehicle vehicle = vehicles.get(i);
            if (!(vehicle.getCurrentSpeedKmHour() > 0.8f) || vehicle.netPlayerAuthorization != BaseVehicle.Authorization.Remote && vehicle.netPlayerAuthorization != BaseVehicle.Authorization.RemoteCollide || (data = vehicle.interpolation.getLastAddedInterpolationPoint()) == null) continue;
            data.getPosition(this.vehicle1PositionVector);
            data.getVelocity(this.vehicle1VelocityVector);
            for (int j = 0; j < vehicles.size(); ++j) {
                VehicleInterpolationData data2;
                BaseVehicle vehicle2 = vehicles.get(j);
                if (vehicle2 == vehicle || !(vehicle2.getCurrentSpeedKmHour() > 0.8f) || vehicle2.interpolation.isDelayLengthIncreased() || (data2 = vehicle2.interpolation.getLastAddedInterpolationPoint()) == null) continue;
                data2.getPosition(this.vehicle2PositionVector);
                data2.getVelocity(this.vehicle2VelocityVector);
                float distance = this.vehicle1PositionVector.distance(this.vehicle2PositionVector);
                float velocity = this.vehicle1VelocityVector.distance(this.vehicle2VelocityVector);
                if (!(distance < velocity * 3.5f)) continue;
                int ping = PingManager.getPing();
                float delayMultiplexer = 2.0f;
                if (ping > 290) {
                    delayMultiplexer = 3.0f;
                }
                vehicle.interpolation.setDelayLength(delayMultiplexer);
                if (vehicle.getVehicleTowing() != null) {
                    vehicle.getVehicleTowing().interpolation.setDelayLength(delayMultiplexer);
                }
                if (vehicle.getVehicleTowedBy() == null) continue;
                vehicle.getVehicleTowedBy().interpolation.setDelayLength(delayMultiplexer);
            }
        }
    }

    public void smashWindow(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setSmashWindow(isoWindow);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.SmashWindow.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.SmashWindow.send(connection);
    }

    public void removeBrokenGlass(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setRemoveBrokenGlass(isoWindow);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.SmashWindow.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.SmashWindow.send(connection);
    }

    public void delayPacket(int x, int y, int z) {
        if (IsoWorld.instance == null) {
            return;
        }
        for (int i = 0; i < IsoWorld.instance.addCoopPlayers.size(); ++i) {
            AddCoopPlayer acp = IsoWorld.instance.addCoopPlayers.get(i);
            if (!acp.isLoadingThisSquare(x, y)) continue;
            this.delayPacket = true;
            return;
        }
    }

    private void mainLoopDealWithNetData(ZomboidNetData d) {
        int position = d.buffer.position();
        this.delayPacket = false;
        if (d.type == null) {
            ZomboidNetDataPool.instance.discard(d);
            return;
        }
        try {
            this.mainLoopHandlePacketInternal(d);
            if (this.delayPacket) {
                d.buffer.position(position);
                DelayedCoopNetData.add(d);
                return;
            }
        }
        catch (Exception ex) {
            DebugLog.Network.printException(ex, "Error with packet of type: " + String.valueOf((Object)d.type), LogSeverity.Error);
        }
        ZomboidNetDataPool.instance.discard(d);
    }

    private void mainLoopHandlePacketInternal(ZomboidNetData d) throws Exception {
        if (!DebugOptions.instance.network.client.mainLoop.getValue()) {
            return;
        }
        d.type.onClientPacket(d.buffer);
    }

    public static void receiveAddBrokenGlass(ByteBufferReader bb, short packetType) {
        int z;
        int y;
        int x = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y = bb.getInt(), z = bb.getInt());
        if (sq != null) {
            sq.addBrokenGlass();
        }
    }

    public static void sendBrokenGlass(IsoGridSquare sq) {
        AddBrokenGlassPacket packet = new AddBrokenGlassPacket();
        packet.set(sq);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.AddBrokenGlass.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.AddBrokenGlass.send(connection);
    }

    public static void sendPlayerDamage(IsoPlayer player) {
        INetworkPacket.send(PacketTypes.PacketType.PlayerDamage, player);
    }

    public static void sendBigWaterSplash(int x, int y, float dx, float dy) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.StartFishSplash.doPacket(b);
        b.putInt(x);
        b.putInt(y);
        b.putFloat(dx);
        b.putFloat(dy);
        PacketTypes.PacketType.StartFishSplash.send(connection);
    }

    public static void receiveBigWaterSplash(ByteBufferReader bb, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        float dx = bb.getFloat();
        float dy = bb.getFloat();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
        if (sq != null) {
            sq.startWaterSplash(true, dx, dy);
        }
    }

    public static void sendFishingDataRequest() {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.FishingData.doPacket(b);
        PacketTypes.PacketType.FishingData.send(connection);
    }

    public static void receiveFishingData(ByteBufferReader bb, short packetType) {
        FishSchoolManager.getInstance().receiveFishingData(bb);
    }

    public static boolean IsClientPaused() {
        return isPaused;
    }

    public static void setIsClientPaused(boolean val) {
        isPaused = val;
    }

    public static void receiveChatMessageToPlayer(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processChatMessagePacket(bb);
    }

    public static void receivePlayerConnectedToChat(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().setFullyConnected();
    }

    public static void receivePlayerJoinChat(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processJoinChatPacket(bb);
    }

    public static void receiveInvMngRemoveItem(ByteBufferReader bb, short packetType) {
        int itemId = bb.getInt();
        InventoryItem item = IsoPlayer.getInstance().getInventory().getItemWithIDRecursiv(itemId);
        if (item == null) {
            DebugLog.log("ERROR: invMngRemoveItem can not find " + itemId + " item.");
            return;
        }
        IsoPlayer.getInstance().removeWornItem(item);
        if (item.getCategory().equals("Clothing")) {
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
        }
        if (item == IsoPlayer.getInstance().getPrimaryHandItem()) {
            IsoPlayer.getInstance().setPrimaryHandItem(null);
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
        } else if (item == IsoPlayer.getInstance().getSecondaryHandItem()) {
            IsoPlayer.getInstance().setSecondaryHandItem(null);
            LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
        }
        boolean result = IsoPlayer.getInstance().getInventory().removeItemWithIDRecurse(itemId);
        if (!result) {
            DebugLog.log("ERROR: GameClient.invMngRemoveItem can not remove item " + itemId);
        }
    }

    public static void receiveInvMngGetItem(ByteBufferReader bb, short packetType) throws IOException {
        short caller = bb.getShort();
        InventoryItem item = null;
        try {
            item = InventoryItem.loadItem(bb.bb, 244);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (item != null) {
            IsoPlayer.getInstance().getInventory().addItem(item);
        }
    }

    public static void receiveInvMngReqItem(ByteBufferReader bb, short packetType) throws IOException {
        Object item;
        int itemId = 0;
        String type = null;
        if (bb.getBoolean()) {
            type = bb.getUTF();
        } else {
            itemId = bb.getInt();
        }
        short askingPlayerId = bb.getShort();
        if (type == null) {
            item = IsoPlayer.getInstance().getInventory().getItemWithIDRecursiv(itemId);
            if (item == null) {
                DebugLog.log("ERROR: invMngReqItem can not find " + itemId + " item.");
                return;
            }
        } else {
            item = InventoryItemFactory.CreateItem(type);
        }
        if (item != null) {
            if (type == null) {
                IsoPlayer.getInstance().removeWornItem((InventoryItem)item);
                if (((InventoryItem)item).getCategory().equals("Clothing")) {
                    LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
                }
                if (item == IsoPlayer.getInstance().getPrimaryHandItem()) {
                    IsoPlayer.getInstance().setPrimaryHandItem(null);
                    LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
                } else if (item == IsoPlayer.getInstance().getSecondaryHandItem()) {
                    IsoPlayer.getInstance().setSecondaryHandItem(null);
                    LuaEventManager.triggerEvent("OnClothingUpdated", IsoPlayer.getInstance());
                }
                IsoPlayer.getInstance().getInventory().removeItemWithIDRecurse(((InventoryItem)item).getID());
            } else {
                IsoPlayer.getInstance().getInventory().RemoveOneOf(type.split("\\.")[1]);
            }
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.InvMngGetItem.doPacket(b);
            b.putShort(askingPlayerId);
            ((InventoryItem)item).saveWithSize(b.bb, false);
            PacketTypes.PacketType.InvMngGetItem.send(connection);
        }
    }

    public static void invMngRequestItem(int itemId, String itemType, short playerID, String username) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.InvMngReqItem.doPacket(b);
        if (b.putBoolean(itemType != null)) {
            b.putUTF(itemType);
        } else {
            b.putInt(itemId);
        }
        b.putShort(IsoPlayer.getInstance().getOnlineID());
        b.putShort(playerID);
        PacketTypes.PacketType.InvMngReqItem.send(connection);
    }

    public static void invMngRequestRemoveItem(int itemId, short playerID, String username) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.InvMngRemoveItem.doPacket(b);
        b.putInt(itemId);
        b.putShort(playerID);
        PacketTypes.PacketType.InvMngRemoveItem.send(connection);
    }

    public static void invMngRequestUpdateItem(InventoryItem item, short playerID) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.InvMngUpdateItem.doPacket(b);
        b.putShort(playerID);
        try {
            item.saveWithSize(b.bb, true);
            PacketTypes.PacketType.InvMngUpdateItem.send(connection);
        }
        catch (Exception e) {
            connection.cancelPacket();
        }
    }

    public static void receiveSyncFaction(ByteBufferReader bb, short packetType) {
        String name = bb.getUTF();
        String owner = bb.getUTF();
        int playersSize = bb.getInt();
        Faction faction = Faction.getFaction(name);
        if (faction == null) {
            faction = new Faction(name, owner);
            Faction.getFactions().add(faction);
        }
        faction.getPlayers().clear();
        if (bb.getBoolean()) {
            faction.setTag(bb.getUTF());
            faction.setTagColor(new ColorInfo(bb.getFloat(), bb.getFloat(), bb.getFloat(), 1.0f));
        }
        for (int i = 0; i < playersSize; ++i) {
            faction.getPlayers().add(bb.getUTF());
        }
        faction.setOwner(owner);
        boolean remove = bb.getBoolean();
        if (remove) {
            Faction.getFactions().remove(faction);
            if (GameServer.server || LuaManager.GlobalObject.isAdmin()) {
                DebugLog.log("faction: removed " + name + " owner=" + faction.getOwner());
            }
        }
        LuaEventManager.triggerEvent("SyncFaction", name);
    }

    public static void receiveChangeTextColor(ByteBufferReader bb, short packetType) {
        short id = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(id);
        if (player == null) {
            return;
        }
        float r = bb.getFloat();
        float g = bb.getFloat();
        float b = bb.getFloat();
        player.setSpeakColourInfo(new ColorInfo(r, g, b, 1.0f));
    }

    public static void receivePlaySoundEveryPlayer(ByteBufferReader bb, short packetType) {
        String name = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        DebugLog.log(DebugType.Sound, "sound: received " + name + " at " + x + "," + y + "," + z);
        if (!Core.soundDisabled) {
            FMOD_STUDIO_EVENT_DESCRIPTION eventDescription = FMODManager.instance.getEventDescription(name);
            if (eventDescription == null) {
                return;
            }
            long inst = javafmod.FMOD_Studio_System_CreateEventInstance(eventDescription.address);
            if (inst <= 0L) {
                return;
            }
            javafmod.FMOD_Studio_EventInstance_SetVolume(inst, (float)Core.getInstance().getOptionAmbientVolume() / 20.0f);
            javafmod.FMOD_Studio_EventInstance3D(inst, x, y, z);
            javafmod.FMOD_Studio_StartEvent(inst);
            javafmod.FMOD_Studio_ReleaseEventInstance(inst);
        }
    }

    public static void receiveAddAlarm(ByteBufferReader bb, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        DebugLog.log(DebugType.Multiplayer, "ReceiveAlarm at [ " + x + " , " + y + " ]");
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
        if (sq == null || sq.getBuilding() == null || sq.getBuilding().getDef() == null) {
            return;
        }
        sq.getBuilding().getDef().alarmed = true;
        AmbientStreamManager.instance.doAlarm(sq.room.def);
    }

    public static void receiveToxicBuilding(ByteBufferReader bb, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        boolean toxic = bb.getBoolean();
        DebugLog.log(DebugType.Multiplayer, "Receive Toxic Building at [ " + x + " , " + y + " Toxic: " + toxic + " ]");
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, 0);
        if (sq == null || sq.getBuilding() == null || sq.getBuilding().getDef() == null) {
            return;
        }
        sq.getBuilding().setToxic(toxic);
    }

    public static void receiveSyncDoorKey(ByteBufferReader bb, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        byte index = bb.getByte();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            instance.delayPacket(x, y, z);
            return;
        }
        if (index >= 0 && index < sq.getObjects().size()) {
            IsoObject obj = sq.getObjects().get(index);
            if (obj instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)obj;
                door.keyId = bb.getInt();
            } else {
                DebugLog.log("SyncDoorKey: expected IsoDoor index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
            }
        } else {
            DebugLog.log("SyncDoorKey: index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
        }
    }

    static void receiveConstructedZone(ByteBufferReader bb, short packetType) {
        int z;
        int y;
        int x = bb.getInt();
        Zone zone = IsoWorld.instance.metaGrid.getZoneAt(x, y = bb.getInt(), z = bb.getInt());
        if (zone != null) {
            zone.setHaveConstruction(true);
        }
    }

    static void receiveZombieDescriptors(ByteBufferReader bb, short packetType) {
        try {
            SharedDescriptors.Descriptor sharedDesc = new SharedDescriptors.Descriptor();
            sharedDesc.load(bb, 244);
            SharedDescriptors.registerPlayerZombieDescriptor(sharedDesc);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendGetAnimalTracks(IsoGameCharacter character) {
        INetworkPacket.send(PacketTypes.PacketType.AnimalTracks, character);
    }

    static void receivePing(ByteBufferReader bb, short packetType) {
        String ip = bb.getUTF();
        String users = bb.getInt() - 1 + "/" + bb.getInt();
        LuaEventManager.triggerEvent("ServerPinged", ip, users);
        connection.forceDisconnect("receive-ping");
        askPing = false;
    }

    public static void sendChangeSafety(Safety safety) {
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.ChangeSafety.doPacket(bbw);
        try {
            SafetyPacket packet = new SafetyPacket(safety);
            packet.write(bbw);
            PacketTypes.PacketType.ChangeSafety.send(connection);
        }
        catch (Exception e) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(e, "SendChangeSafety: failed", LogSeverity.Error);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addDisconnectPacket(int packet) {
        ArrayList<Integer> arrayList = this.delayedDisconnect;
        synchronized (arrayList) {
            this.delayedDisconnect.add(packet);
        }
        ConnectionManager.log("disconnect", String.valueOf(packet), connection);
    }

    public void connectionLost() {
        this.connectionLost = true;
        positions.clear();
        WorldMapRemotePlayers.instance.Reset();
    }

    public static void SendCommandToServer(String command) {
        String[] args2;
        if (ServerOptions.clientOptionsList == null) {
            ServerOptions.initClientCommandsHelp();
        }
        if (command.startsWith("/roll")) {
            try {
                int roll = Integer.parseInt(command.split(" ")[1]);
                if (roll > 100) {
                    ChatManager.getInstance().showServerChatMessage(ServerOptions.clientOptionsList.get("roll"));
                    return;
                }
            }
            catch (Exception e) {
                ChatManager.getInstance().showServerChatMessage(ServerOptions.clientOptionsList.get("roll"));
                return;
            }
            if (!IsoPlayer.getInstance().getInventory().contains("Dice") && !connection.getRole().hasCapability(Capability.GeneralCheats)) {
                ChatManager.getInstance().showServerChatMessage(ServerOptions.clientOptionsList.get("roll"));
                return;
            }
        }
        if (command.startsWith("/card") && !IsoPlayer.getInstance().getInventory().contains("CardDeck") && !connection.getRole().hasCapability(Capability.GeneralCheats)) {
            ChatManager.getInstance().showServerChatMessage(ServerOptions.clientOptionsList.get("card"));
            return;
        }
        if (command.startsWith("/list") && (args2 = command.split(" ")).length == 2) {
            ChatManager.getInstance().showServerChatMessage(ListCommand.List(args2[1]));
        }
        if (command.startsWith("/log ")) {
            String[] args3;
            String tabTitleID = ChatManager.getInstance().getFocusTab().getTitleID();
            if ("UI_chat_admin_tab_title_id".equals(tabTitleID)) {
                ByteBufferWriter b = connection.startPacket();
                PacketTypes.PacketType.ReceiveCommand.doPacket(b);
                b.putUTF(command);
                PacketTypes.PacketType.ReceiveCommand.send(connection);
            } else if ("UI_chat_main_tab_title_id".equals(tabTitleID) && (args3 = command.split(" ")).length == 3) {
                ChatManager.getInstance().showServerChatMessage(LogCommand.process(args3[1], args3[2]));
            }
            return;
        }
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ReceiveCommand.doPacket(b);
        b.putUTF(command);
        PacketTypes.PacketType.ReceiveCommand.send(connection);
    }

    private boolean gameLoadingDealWithNetData(ZomboidNetData d) {
        try {
            return d.type.onClientLoadingPacket(d.buffer);
        }
        catch (Exception ex) {
            DebugLog.log(DebugType.Network, "Error with packet of type: " + String.valueOf((Object)d.type));
            ex.printStackTrace();
            ZomboidNetDataPool.instance.discard(d);
            return true;
        }
    }

    static void receiveStartRain(ByteBufferReader bb, short packetType) {
        RainManager.setRandRainMin(bb.getInt());
        RainManager.setRandRainMax(bb.getInt());
        RainManager.startRaining();
        RainManager.rainDesiredIntensity = bb.getFloat();
    }

    public static void receiveStopRain(ByteBufferReader bb, short packetType) {
        RainManager.stopRaining();
    }

    public static void receiveWeather(ByteBufferReader bb, short packetType) {
        WeatherPacket packet = new WeatherPacket();
        packet.parse(bb, connection);
    }

    public static void receiveWorldMapPlayerPosition(ByteBufferReader bb, short packetType) {
        tempShortList.clear();
        boolean isFullUpdate = bb.getBoolean();
        int count = bb.getShort();
        for (int i = 0; i < count; ++i) {
            short changeCount;
            short playerID = bb.getShort();
            WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getOrCreatePlayerByID(playerID);
            if (isFullUpdate) {
                IsoPlayer player;
                changeCount = bb.getShort();
                String username = bb.getUTF();
                String forename = bb.getUTF();
                String surname = bb.getUTF();
                String accessLevel = bb.getUTF();
                int rolePower = bb.getInt();
                float x = bb.getFloat();
                float y = bb.getFloat();
                boolean invisible = bb.getBoolean();
                boolean disguised = bb.getBoolean();
                remotePlayer.setFullData(changeCount, username, forename, surname, accessLevel, rolePower, x, y, invisible, disguised);
                if (positions.containsKey(playerID)) {
                    positions.get(playerID).set(x, y);
                } else {
                    positions.put(playerID, new Vector2(x, y));
                }
                if (!ServerOptions.getInstance().hideDisguisedUserName.getValue() && !ServerOptions.getInstance().usernameDisguises.getValue() || (player = IDToPlayerMap.get(playerID)) == null) continue;
                player.updateDisguisedState();
                remotePlayer.setPlayer(player);
                continue;
            }
            changeCount = bb.getShort();
            float x = bb.getFloat();
            float y = bb.getFloat();
            if (remotePlayer.getChangeCount() != changeCount) {
                tempShortList.add(playerID);
                continue;
            }
            remotePlayer.setPosition(x, y);
            if (positions.containsKey(playerID)) {
                positions.get(playerID).set(x, y);
                continue;
            }
            positions.put(playerID, new Vector2(x, y));
        }
        if (tempShortList.isEmpty()) {
            return;
        }
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(b);
        b.putShort(tempShortList.size());
        for (int i = 0; i < tempShortList.size(); ++i) {
            b.putShort(tempShortList.get(i));
        }
        PacketTypes.PacketType.WorldMapPlayerPosition.send(connection);
    }

    static void receiveClientCommand(ByteBufferReader bb, short packetType) {
        String module = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();
            try {
                TableNetworkUtils.load(tbl, bb);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        LuaEventManager.triggerEvent("OnServerCommand", module, command, tbl);
    }

    public static void receiveWorldMap(ByteBufferReader bb, short packetType) throws IOException {
        WorldMapClient.instance.receive(bb);
    }

    public void setRequest(RequestState request) {
        this.request = request;
    }

    public void GameLoadingRequestData() {
        RequestDataPacket packet = new RequestDataPacket();
        this.request = RequestState.Start;
        while (this.request != RequestState.Complete) {
            if (this.request == RequestState.Start) {
                packet.setRequest();
                ByteBufferWriter bb = connection.startPacket();
                PacketTypes.PacketType.RequestData.doPacket(bb);
                packet.write(bb);
                PacketTypes.PacketType.RequestData.send(connection);
                this.request = RequestState.Loading;
            }
            try {
                Thread.sleep(30L);
            }
            catch (InterruptedException ex) {
                DebugLog.Multiplayer.printException(ex, "GameLoadingRequestData sleep error", LogSeverity.Error);
            }
        }
    }

    public static void receiveSendCustomColor(ByteBufferReader bb, short packetType) {
        IsoObject o;
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        int index = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            instance.delayPacket(x, y, z);
            return;
        }
        if (sq != null && index < sq.getObjects().size() && (o = sq.getObjects().get(index)) != null) {
            o.setCustomColor(new ColorInfo(bb.getFloat(), bb.getFloat(), bb.getFloat(), bb.getFloat()));
        }
    }

    public static void receiveUpdateItemSprite(ByteBufferReader bb, short packetType) {
        int bbbb = bb.getInt();
        String spriteName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        int index = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            instance.delayPacket(x, y, z);
            return;
        }
        if (sq != null && index < sq.getObjects().size()) {
            try {
                IsoObject o = sq.getObjects().get(index);
                if (o != null) {
                    boolean hitByCar = o.sprite != null && o.sprite.getProperties().has("HitByCar") && o.sprite.getProperties().get(IsoPropertyType.DAMAGED_SPRITE) != null && !o.sprite.getProperties().get(IsoPropertyType.DAMAGED_SPRITE).isEmpty();
                    o.sprite = IsoSpriteManager.instance.getSprite(bbbb);
                    if (o.sprite == null && !spriteName.isEmpty()) {
                        o.setSprite(spriteName);
                    }
                    o.RemoveAttachedAnims();
                    int count = bb.getByte() & 0xFF;
                    for (int i = 0; i < count; ++i) {
                        int id = bb.getInt();
                        IsoSprite spr = IsoSpriteManager.instance.getSprite(id);
                        if (spr == null) continue;
                        o.AttachExistingAnim(spr, 0, 0, false, 0, false, 0.0f);
                    }
                    if (o instanceof IsoThumpable) {
                        IsoThumpable isoThumpable = (IsoThumpable)o;
                        if (hitByCar && (o.sprite == null || !o.sprite.getProperties().has("HitByCar"))) {
                            isoThumpable.setBlockAllTheSquare(false);
                        }
                    }
                    sq.RecalcAllWithNeighbours(true);
                    sq.invalidateRenderChunkLevel(256L);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private KahluaTable copyTable(KahluaTable orig) {
        KahluaTable copy = LuaManager.platform.newTable();
        KahluaTableIterator it = orig.iterator();
        while (it.advance()) {
            Object key = it.getKey();
            Object value = it.getValue();
            if (value instanceof KahluaTable) {
                KahluaTable kahluaTable = (KahluaTable)value;
                copy.rawset(key, (Object)this.copyTable(kahluaTable));
                continue;
            }
            copy.rawset(key, value);
        }
        return copy;
    }

    public KahluaTable getServerSpawnRegions() {
        return this.copyTable(this.serverSpawnRegions);
    }

    public static void sendZombieHit(IsoZombie wielder, IsoPlayer target, boolean didDamage, String hitReaction) {
        boolean wielderIsLocal;
        boolean targetIsLocal = target.isLocalPlayer();
        boolean bl = wielderIsLocal = !wielder.isRemoteZombie();
        if (wielderIsLocal && targetIsLocal) {
            ByteBufferWriter bbw = connection.startPacket();
            try {
                PacketTypes.PacketType.ZombieHitPlayer.doPacket(bbw);
                ZombieHitPlayerPacket packet = new ZombieHitPlayerPacket();
                packet.set(wielder, target, didDamage, hitReaction);
                packet.write(bbw);
                PacketTypes.PacketType.ZombieHitPlayer.send(connection);
            }
            catch (Exception e) {
                connection.cancelPacket();
                DebugLog.Multiplayer.printException(e, "SendHitCharacter: failed", LogSeverity.Error);
            }
        }
    }

    public static void sendAnimalHitPlayer(IsoGameCharacter wielder, IsoMovingObject target, float damage, boolean ignoreDamage) {
        AnimalHitPlayerPacket packet = new AnimalHitPlayerPacket();
        packet.set((IsoAnimal)wielder, (IsoPlayer)target, ignoreDamage, damage);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.AnimalHitPlayer.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.AnimalHitPlayer.send(connection);
    }

    public static void sendAnimalHitAnimal(IsoGameCharacter wielder, IsoMovingObject target, float damage, boolean ignoreDamage) {
        AnimalHitAnimalPacket packet = new AnimalHitAnimalPacket();
        packet.set((IsoAnimal)wielder, (IsoAnimal)target, ignoreDamage, damage);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.AnimalHitAnimal.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.AnimalHitAnimal.send(connection);
    }

    public static void sendZombieHitThumpable(IsoGameCharacter wielder, IsoObject thumpable) {
        ZombieHitThumpablePacket packet = new ZombieHitThumpablePacket();
        packet.set((IsoZombie)wielder, thumpable);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.ZombieHitThumpable.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.ZombieHitThumpable.send(connection);
    }

    public static void sendAnimalHitThumpable(IsoGameCharacter wielder) {
        AnimalHitThumpablePacket packet = new AnimalHitThumpablePacket();
        packet.set((IsoAnimal)wielder, ((IsoAnimal)wielder).thumpTarget);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.AnimalHitThumpable.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.AnimalHitThumpable.send(connection);
    }

    public static void sendAttackCollisionCheck(IsoPlayer wielder, HandWeapon weapon, int hitCount) {
        AttackCollisionCheckPacket packet = new AttackCollisionCheckPacket();
        packet.set(wielder, weapon, hitCount);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.AttackCollisionCheckPacket.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.AttackCollisionCheckPacket.send(connection);
    }

    public static void sendForageItemFound(IsoPlayer player, String type, float amount) {
        INetworkPacket.send(PacketTypes.PacketType.ForageItemFound, player, type, Float.valueOf(amount));
    }

    public static void sendPlayerHit(IsoGameCharacter wielder, IsoObject target, HandWeapon weapon, float damage, boolean ignoreDamage, float range, boolean isCriticalHit, boolean helmetFall, boolean hitHead, boolean removeKnife) {
        if (!client) {
            return;
        }
        if (!wielder.isLocal()) {
            return;
        }
        if (target == null) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitSquare, wielder, weapon, ignoreDamage, isCriticalHit);
        } else if (target instanceof IsoAnimal) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitAnimal, wielder, weapon, ignoreDamage, isCriticalHit, target, Float.valueOf(damage), Float.valueOf(range), hitHead);
        } else if (target instanceof IsoPlayer) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitPlayer, wielder, weapon, ignoreDamage, isCriticalHit, target, Float.valueOf(damage), Float.valueOf(range), hitHead);
        } else if (target instanceof IsoZombie) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitZombie, wielder, weapon, ignoreDamage, isCriticalHit, target, Float.valueOf(damage), Float.valueOf(range), helmetFall, hitHead, removeKnife);
        } else if (target instanceof BaseVehicle) {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitVehicle, wielder, weapon, ignoreDamage, isCriticalHit, target, Float.valueOf(damage));
        } else {
            INetworkPacket.send(PacketTypes.PacketType.PlayerHitObject, wielder, weapon, ignoreDamage, isCriticalHit, target);
        }
    }

    public static void sendVehicleHit(IsoPlayer wielder, IsoGameCharacter target, BaseVehicle vehicle, float damage, boolean isTargetHitFromBehind, int vehicleDamage, float vehicleSpeed, boolean isVehicleHitFromBehind) {
        ByteBufferWriter bbw = connection.startPacket();
        try {
            if (target instanceof IsoAnimal) {
                IsoAnimal isoAnimal = (IsoAnimal)target;
                PacketTypes.PacketType.VehicleHitAnimal.doPacket(bbw);
                VehicleHitAnimalPacket packet = new VehicleHitAnimalPacket();
                packet.set(wielder, isoAnimal, vehicle, damage, isTargetHitFromBehind, vehicleDamage, vehicleSpeed, isVehicleHitFromBehind);
                packet.write(bbw);
                PacketTypes.PacketType.VehicleHitAnimal.send(connection);
            } else if (target instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)target;
                PacketTypes.PacketType.VehicleHitPlayer.doPacket(bbw);
                VehicleHitPlayerPacket packet = new VehicleHitPlayerPacket();
                packet.set(wielder, isoPlayer, vehicle, damage, isTargetHitFromBehind, vehicleDamage, vehicleSpeed, isVehicleHitFromBehind);
                packet.write(bbw);
                PacketTypes.PacketType.VehicleHitPlayer.send(connection);
            } else if (target instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)target;
                PacketTypes.PacketType.VehicleHitZombie.doPacket(bbw);
                VehicleHitZombiePacket packet = new VehicleHitZombiePacket();
                packet.set(wielder, isoZombie, vehicle, damage, isTargetHitFromBehind, vehicleDamage, vehicleSpeed, isVehicleHitFromBehind);
                packet.write(bbw);
                PacketTypes.PacketType.VehicleHitZombie.send(connection);
            } else {
                DebugLog.Multiplayer.warn(String.format("SendHitVehicle: unknown target type (wielder=%s, target=%s)", wielder.getClass().getName(), target.getClass().getName()));
            }
        }
        catch (Exception e) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(e, "SendHitVehicle: failed", LogSeverity.Error);
        }
    }

    public static void sendEatBody(IsoZombie zombie, IsoMovingObject target) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.EatBody.doPacket(b);
        try {
            b.putShort(zombie.getOnlineID());
            if (target instanceof IsoDeadBody) {
                IsoDeadBody bodyToEat = (IsoDeadBody)target;
                b.putByte(1);
                b.putBoolean(zombie.getVariableBoolean("onknees"));
                b.putFloat(zombie.getEatSpeed());
                b.putFloat(zombie.getStateEventDelayTimer());
                b.putInt(bodyToEat.getStaticMovingObjectIndex());
                b.putFloat(bodyToEat.getSquare().getX());
                b.putFloat(bodyToEat.getSquare().getY());
                b.putFloat(bodyToEat.getSquare().getZ());
            } else if (target instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)target;
                b.putByte(2);
                b.putBoolean(zombie.getVariableBoolean("onknees"));
                b.putFloat(zombie.getEatSpeed());
                b.putFloat(zombie.getStateEventDelayTimer());
                b.putShort(isoPlayer.getOnlineID());
            } else {
                b.putByte(0);
            }
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, "SendEatBody");
            }
            PacketTypes.PacketType.EatBody.send(connection);
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "SendEatBody: failed", LogSeverity.Error);
            connection.cancelPacket();
        }
    }

    public static void receiveEatBody(ByteBufferReader bb, short packetType) {
        try {
            IsoZombie zombie;
            short zombieID = bb.getShort();
            byte type = bb.getByte();
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, String.format("ReceiveEatBody: zombie=%d type=%d", zombieID, type));
            }
            if ((zombie = IDToZombieMap.get(zombieID)) == null) {
                DebugLog.Multiplayer.error("ReceiveEatBody: zombie " + zombieID + " not found");
                return;
            }
            if (type == 1) {
                float z;
                float y;
                boolean isOnKnees = bb.getBoolean();
                float eatSpeed = bb.getFloat();
                float stateEventDelayTimer = bb.getFloat();
                int index = bb.getInt();
                float x = bb.getFloat();
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y = bb.getFloat(), z = bb.getFloat());
                if (sq == null) {
                    DebugLog.Multiplayer.error("ReceiveEatBody: incorrect square");
                    return;
                }
                if (index >= 0 && index < sq.getStaticMovingObjects().size()) {
                    IsoDeadBody isoDeadBody = (IsoDeadBody)sq.getStaticMovingObjects().get(index);
                    if (isoDeadBody != null) {
                        zombie.setTarget(null);
                        zombie.setEatBodyTarget(isoDeadBody, true, eatSpeed);
                        zombie.setVariable("onknees", isOnKnees);
                        zombie.setStateEventDelayTimer(stateEventDelayTimer);
                    } else {
                        DebugLog.Multiplayer.error("ReceiveEatBody: no corpse with index " + index + " on square");
                    }
                } else {
                    DebugLog.Multiplayer.error("ReceiveEatBody: no corpse on square");
                }
            } else if (type == 2) {
                boolean isOnKnees = bb.getBoolean();
                float eatSpeed = bb.getFloat();
                float stateEventDelayTimer = bb.getFloat();
                short playerID = bb.getShort();
                IsoPlayer player = IDToPlayerMap.get(playerID);
                if (player == null) {
                    DebugLog.Multiplayer.error("ReceiveEatBody: player " + playerID + " not found");
                    return;
                }
                zombie.setTarget(null);
                zombie.setEatBodyTarget(player, true, eatSpeed);
                zombie.setVariable("onknees", isOnKnees);
                zombie.setStateEventDelayTimer(stateEventDelayTimer);
            } else {
                zombie.setEatBodyTarget(null, false);
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "ReceiveEatBody: failed", LogSeverity.Error);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void receiveSyncRadioData(ByteBufferReader bb, short packetType) {
        short onlineId = bb.getShort();
        VoiceManagerData d = VoiceManagerData.get(onlineId);
        ArrayList<VoiceManagerData.RadioData> arrayList = d.radioData;
        synchronized (arrayList) {
            d.isCanHearAll = bb.getBoolean();
            short radioDataSize = (short)bb.getInt();
            d.radioData.clear();
            for (int i = 0; i < radioDataSize / 4; ++i) {
                int freq = bb.getInt();
                int distance = bb.getInt();
                int x = bb.getInt();
                int y = bb.getInt();
                d.radioData.add(new VoiceManagerData.RadioData(freq, distance, x, y));
            }
        }
    }

    public void sendWorldSound(WorldSoundManager.WorldSound sound) {
        INetworkPacket.send(PacketTypes.PacketType.WorldSoundPacket, sound);
    }

    public void sendLoginQueueRequest() {
        ByteBufferWriter bb2 = connection.startPacket();
        PacketTypes.PacketType.LoginQueueRequest.doPacket(bb2);
        PacketTypes.PacketType.LoginQueueRequest.send(connection);
        ConnectionManager.log("send-packet", "login-queue-request", connection);
    }

    public void sendLoginQueueDone(long dt) {
        INetworkPacket.send(PacketTypes.PacketType.LoginQueueDone, dt);
        ConnectionManager.log("send-packet", "login-queue-done", connection);
    }

    public static boolean canSeePlayerStats() {
        return connection.getRole().hasCapability(Capability.CanSeePlayersStats);
    }

    public void sendPersonalColor(IsoPlayer player) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ChangeTextColor.doPacket(b);
        b.putShort(player.getPlayerNum());
        b.putFloat(Core.getInstance().getMpTextColor().r);
        b.putFloat(Core.getInstance().getMpTextColor().g);
        b.putFloat(Core.getInstance().getMpTextColor().b);
        PacketTypes.PacketType.ChangeTextColor.send(connection);
    }

    public void sendChangedPlayerStats(IsoPlayer otherPlayer) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ChangePlayerStats.doPacket(b);
        otherPlayer.createPlayerStats(b, username);
        PacketTypes.PacketType.ChangePlayerStats.send(connection);
    }

    public static void receiveChangePlayerStats(ByteBufferReader bb, short packetType) {
        short id = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(id);
        if (player == null) {
            return;
        }
        String adminUserName = bb.getUTF();
        player.setPlayerStats(bb, adminUserName);
        allChatMuted = player.isAllChatMuted();
    }

    public void sendPlayerConnect(IsoPlayer player) {
        player.setOnlineID((short)-1);
        connection.setUserName(player.username);
        INetworkPacket.send(PacketTypes.PacketType.PlayerConnect, player);
        allChatMuted = player.isAllChatMuted();
        GameClient.sendPerks(player);
        player.updateEquippedRadioFreq();
        this.playerConnectSent = true;
        INetworkPacket.send(PacketTypes.PacketType.TimeSync, new Object[0]);
        ConnectionManager.log("send-packet", "player-connect", connection);
    }

    public static void sendCreatePlayer(byte playerIndex) {
        INetworkPacket.send(PacketTypes.PacketType.CreatePlayer, playerIndex);
    }

    public void sendPlayer2(IsoPlayer isoPlayer) {
        PlayerPacket packet;
        PacketTypes.PacketType packetType;
        if (!client || !isoPlayer.isLocalPlayer()) {
            return;
        }
        if (isoPlayer.networkAi.isReliable()) {
            this.sendPlayer(isoPlayer);
        }
        if ((packetType = (packet = isoPlayer.getNetworkCharacterAI().getPlayerPacket()).set(isoPlayer)) != null) {
            packet.sendToServer(packetType);
        }
    }

    public void sendPlayer(IsoPlayer isoPlayer) {
        isoPlayer.networkAi.needToUpdate();
    }

    public void heartBeat() {
        ++count;
    }

    public static IsoZombie getZombie(short id) {
        return IDToZombieMap.get(id);
    }

    public static void sendPlayerExtraInfo(IsoPlayer p) {
        INetworkPacket.send(PacketTypes.PacketType.ExtraInfo, p);
    }

    public void setResetID(int resetId) {
        this.resetId = 0;
        this.loadResetID();
        if (this.resetId != resetId) {
            File dstFile;
            File srcFile;
            int i;
            ArrayList<String> preserveFiles = new ArrayList<String>();
            preserveFiles.add("map_symbols.bin");
            preserveFiles.add("map_visited.bin");
            preserveFiles.add("recorded_media.bin");
            for (i = 0; i < preserveFiles.size(); ++i) {
                try {
                    srcFile = ZomboidFileSystem.instance.getFileInCurrentSave((String)preserveFiles.get(i));
                    if (!srcFile.exists()) continue;
                    dstFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + (String)preserveFiles.get(i));
                    if (dstFile.exists()) {
                        dstFile.delete();
                    }
                    srcFile.renameTo(dstFile);
                    continue;
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
            }
            DebugLog.log("server was reset, deleting " + Core.gameMode + File.separator + Core.gameSaveWorld);
            LuaManager.GlobalObject.deleteSave(Core.gameMode + File.separator + Core.gameSaveWorld);
            LuaManager.GlobalObject.createWorld(Core.gameSaveWorld);
            for (i = 0; i < preserveFiles.size(); ++i) {
                try {
                    srcFile = ZomboidFileSystem.instance.getFileInCurrentSave((String)preserveFiles.get(i));
                    dstFile = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + (String)preserveFiles.get(i));
                    if (!dstFile.exists()) continue;
                    dstFile.renameTo(srcFile);
                    continue;
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
            }
        }
        this.resetId = resetId;
        this.saveResetID();
    }

    public void loadResetID() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("serverid.dat");
        if (inFile.exists()) {
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(inFile);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            DataInputStream input = new DataInputStream(inStream);
            try {
                this.resetId = input.readInt();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inStream.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveResetID() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("serverid.dat");
        FileOutputStream inStream = null;
        try {
            inStream = new FileOutputStream(inFile);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        DataOutputStream input = new DataOutputStream(inStream);
        try {
            input.writeInt(this.resetId);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            inStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<IsoPlayer> getPlayers() {
        if (!this.idMapDirty) {
            return this.players;
        }
        this.players.clear();
        this.players.addAll(IDToPlayerMap.values());
        this.idMapDirty = false;
        return this.players;
    }

    public static void receiveSyncIsoObject(ByteBufferReader bb, short packetType) {
        if (!DebugOptions.instance.network.client.syncIsoObject.getValue()) {
            return;
        }
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        byte index = bb.getByte();
        byte exist = bb.getByte();
        byte state = bb.getByte();
        if (exist != 2) {
            // empty if block
        }
        if (exist == 1) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (sq == null) {
                return;
            }
            if (index >= 0 && index < sq.getObjects().size()) {
                sq.getObjects().get(index).syncIsoObject(true, state, null, bb);
            } else {
                DebugLog.Network.warn("SyncIsoObject: index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
            }
        }
    }

    private static void skipPacket(ByteBuffer bb, short packetType) {
    }

    public static void receivePlayerTimeout(short playerID) {
        WorldMapRemotePlayers.instance.removePlayerByID(playerID);
        positions.remove(playerID);
        IsoPlayer p = IDToPlayerMap.get(playerID);
        if (p == null) {
            return;
        }
        DebugLog.DetailedInfo.trace("Received timeout for player " + p.username + " id " + p.onlineId);
        NetworkZombieSimulator.getInstance().clearTargetAuth(p);
        if (p.getVehicle() != null) {
            int seat = p.getVehicle().getSeat(p);
            if (seat != -1) {
                p.getVehicle().clearPassenger(seat);
            }
            VehicleManager.instance.sendVehicleRequest(p.getVehicle().vehicleId, (short)2);
        }
        p.removeFromWorld();
        p.removeFromSquare();
        IDToPlayerMap.remove(p.onlineId);
        GameClient.instance.idMapDirty = true;
        LuaEventManager.triggerEvent("OnMiniScoreboardUpdate");
    }

    public void disconnect(boolean doResetDisconnectTimer) {
        if (doResetDisconnectTimer) {
            this.resetDisconnectTimer();
        }
        this.connected = false;
        if (IsoPlayer.getInstance() != null) {
            IsoPlayer.getInstance().setOnlineID((short)-1);
        }
    }

    public void resetDisconnectTimer() {
        this.disconnectTime = System.currentTimeMillis();
    }

    public String getReconnectCountdownTimer() {
        return String.valueOf((int)Math.ceil((10000L - System.currentTimeMillis() + this.disconnectTime) / 1000L));
    }

    public boolean canConnect() {
        return System.currentTimeMillis() - this.disconnectTime > 10000L;
    }

    public void addIncoming(short id, ByteBufferReader bb) {
        if (connection == null) {
            return;
        }
        if (id == PacketTypes.PacketType.SentChunk.getId()) {
            WorldStreamer.instance.receiveChunkPart(bb);
            return;
        }
        if (id == PacketTypes.PacketType.NotRequiredInZip.getId()) {
            WorldStreamer.instance.receiveNotRequired(bb);
            return;
        }
        if (id == PacketTypes.PacketType.LoadPlayerProfile.getId()) {
            LoadPlayerProfilePacket packet = new LoadPlayerProfilePacket();
            packet.parse(bb, connection);
            packet.processClient(connection);
            return;
        }
        ZomboidNetData d = bb.remaining() > 2048 ? ZomboidNetDataPool.instance.getLong(bb.remaining()) : ZomboidNetDataPool.instance.get();
        d.read(id, bb, connection);
        d.time = System.currentTimeMillis();
        MainLoopNetDataQ.add(d);
    }

    public void doDisconnect(String string) {
        if (connection != null) {
            try {
                if (Thread.currentThread() == GameWindow.gameThread) {
                    SavefileThumbnail.createForMP(connection.getIP(), port, username);
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
            connection.forceDisconnect(string);
            this.connected = false;
            connection = null;
            client = false;
        } else {
            instance.Shutdown();
        }
    }

    public void removeZombieFromCache(IsoZombie z) {
        if (IDToZombieMap.containsKey(z.onlineId)) {
            IDToZombieMap.remove(z.onlineId);
        }
    }

    public void sendWorldMessage(String line) {
        ChatManager.getInstance().showInfoMessage(line);
    }

    private void convertGameSaveWorldDirectory(String oldDir, String newDir) {
        File dir = new File(oldDir);
        if (!dir.isDirectory()) {
            return;
        }
        File newDirFile = new File(newDir);
        boolean res = dir.renameTo(newDirFile);
        if (res) {
            DebugLog.log("CONVERT: The GameSaveWorld directory was renamed from " + oldDir + " to " + newDir);
        } else {
            DebugLog.log("ERROR: The GameSaveWorld directory cannot rename from " + oldDir + " to " + newDir);
        }
    }

    public void doConnect(String user, String pass, String serverIP, String localIP, String port, String serverPassword, String serverName, boolean useSteamRelay, int authType) {
        this.doConnect(user, pass, serverIP, localIP, port, serverPassword, serverName, useSteamRelay, authType, "");
    }

    public void doConnect(String user, String pass, String serverIP, String localIP, String port, String serverPassword, String serverName, boolean useSteamRelay, int authType, String googleKey) {
        username = user.trim();
        password = pass.trim();
        ip = serverIP.trim();
        GameClient.localIP = localIP.trim();
        GameClient.port = Integer.parseInt(port.trim());
        GameClient.serverPassword = serverPassword.trim();
        GameClient.serverName = serverName.trim();
        GameClient.useSteamRelay = useSteamRelay;
        GameClient.authType = authType;
        GameClient.googleKey = googleKey;
        instance.init();
        Core.gameSaveWorld = ip + "_" + GameClient.port + "_" + ServerWorldDatabase.encrypt(user);
        this.convertGameSaveWorldDirectory(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + ip + "_" + GameClient.port + "_" + user, ZomboidFileSystem.instance.getCurrentSaveDir());
        if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
            Core.gameSaveWorld = CoopMaster.instance.getPlayerSaveFolder(CoopMaster.instance.getServerName());
        }
    }

    public void doConnectCoop(String serverSteamID) {
        username = SteamFriends.GetPersonaName();
        password = "";
        ip = serverSteamID;
        localIP = "";
        port = 0;
        serverPassword = "";
        this.init();
        if (CoopMaster.instance != null && CoopMaster.instance.isRunning()) {
            Core.gameSaveWorld = CoopMaster.instance.getPlayerSaveFolder(CoopMaster.instance.getServerName());
        }
    }

    public static void receiveAddAmbient(ByteBufferReader bb, short packetType) {
        String name = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int radius = bb.getInt();
        float volume = bb.getFloat();
        DebugLog.log(DebugType.Sound, "ambient: received " + name + " at " + x + "," + y + " radius=" + radius);
        AmbientStreamManager.instance.addAmbient(name, x, y, radius, volume);
    }

    public void sendClientCommand(IsoPlayer player, String module, String command, KahluaTable args2) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putByte(player != null ? player.playerIndex : -1);
        b.putUTF(module);
        b.putUTF(command);
        if (b.putBoolean(args2 != null && !args2.isEmpty())) {
            try {
                KahluaTableIterator it = args2.iterator();
                while (it.advance()) {
                    if (TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
                    DebugLog.log("ERROR: sendClientCommand: can't save key,value=" + String.valueOf(it.getKey()) + "," + String.valueOf(it.getValue()));
                }
                TableNetworkUtils.save(args2, b);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        PacketTypes.PacketType.ClientCommand.send(connection);
    }

    public void sendClientCommandV(IsoPlayer player, String module, String command, Object ... objects) {
        if (objects.length == 0) {
            this.sendClientCommand(player, module, command, null);
            return;
        }
        if (objects.length % 2 != 0) {
            DebugLog.log("ERROR: sendClientCommand called with wrong number of arguments (" + module + " " + command + ")");
            return;
        }
        KahluaTable t = LuaManager.platform.newTable();
        for (int i = 0; i < objects.length; i += 2) {
            Object v = objects[i + 1];
            if (v instanceof Float) {
                Float f = (Float)v;
                t.rawset(objects[i], (Object)f.doubleValue());
                continue;
            }
            if (v instanceof Integer) {
                Integer integer = (Integer)v;
                t.rawset(objects[i], (Object)integer.doubleValue());
                continue;
            }
            if (v instanceof Short) {
                Short s = (Short)v;
                t.rawset(objects[i], (Object)s.doubleValue());
                continue;
            }
            t.rawset(objects[i], v);
        }
        this.sendClientCommand(player, module, command, t);
    }

    public void sendAttachedItem(IsoGameCharacter character, String location, InventoryItem item) {
        INetworkPacket.send(PacketTypes.PacketType.GameCharacterAttachedItem, character, location, item);
    }

    public void sendVisual(IsoPlayer player) {
        if (player == null || player.onlineId == -1) {
            return;
        }
        INetworkPacket.send(PacketTypes.PacketType.HumanVisual, player);
    }

    public static void receiveBloodSplatter(ByteBufferReader bb, short packetType) {
        String weaponType = bb.getUTF();
        float x = bb.getFloat();
        float y = bb.getFloat();
        float z = bb.getFloat();
        float hitDirX = bb.getFloat();
        float hitDirY = bb.getFloat();
        boolean isCloseKilled = bb.getBoolean();
        boolean isRadial = bb.getBoolean();
        int splatNumber = bb.getByte();
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoGridSquare sq = cell.getGridSquare(x, y, z);
        if (sq == null) {
            instance.delayPacket(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
            return;
        }
        if (isRadial && SandboxOptions.instance.bloodLevel.getValue() > 1) {
            for (int sx = -1; sx <= 1; ++sx) {
                for (int sy = -1; sy <= 1; ++sy) {
                    if (sx == 0 && sy == 0) continue;
                    new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, (float)sx * Rand.Next(0.25f, 0.5f), (float)sy * Rand.Next(0.25f, 0.5f));
                }
            }
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, cell, x, y, z, hitDirX * 0.8f, hitDirY * 0.8f);
            return;
        }
        if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
            for (int n = 0; n < splatNumber; ++n) {
                sq.splatBlood(3, 0.3f);
            }
            sq.getChunk().addBloodSplat(x, y, PZMath.fastfloor(z), Rand.Next(20));
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.5f, hitDirY * 1.5f);
        }
        int rand = 3;
        int rand2 = 0;
        int nbRepeat = 1;
        switch (SandboxOptions.instance.bloodLevel.getValue()) {
            case 1: {
                nbRepeat = 0;
                break;
            }
            case 2: {
                nbRepeat = 1;
                rand = 5;
                rand2 = 2;
                break;
            }
            case 4: {
                nbRepeat = 3;
                rand = 2;
                break;
            }
            case 5: {
                nbRepeat = 10;
                rand = 0;
            }
        }
        for (int i = 0; i < nbRepeat; ++i) {
            if (Rand.Next(isCloseKilled ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.5f, hitDirY * 1.5f);
            }
            if (Rand.Next(isCloseKilled ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.5f, hitDirY * 1.5f);
            }
            if (Rand.Next(isCloseKilled ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.8f, hitDirY * 1.8f);
            }
            if (Rand.Next(isCloseKilled ? 8 : rand) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.9f, hitDirY * 1.9f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 3.5f, hitDirY * 3.5f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 3.8f, hitDirY * 3.8f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 3.9f, hitDirY * 3.9f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 1.5f, hitDirY * 1.5f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 3.8f, hitDirY * 3.8f);
            }
            if (Rand.Next(isCloseKilled ? 4 : rand2) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, cell, x, y, z, hitDirX * 3.9f, hitDirY * 3.9f);
            }
            if (Rand.Next(isCloseKilled ? 9 : 6) != 0) continue;
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.Eye, cell, x, y, z, hitDirX * 0.8f, hitDirY * 0.8f);
        }
    }

    public static void receiveZombieSound(ByteBufferReader bb, short packetType) {
        short id = bb.getShort();
        byte sid = bb.getByte();
        IsoZombie.ZombieSound sound = bb.getEnum(IsoZombie.ZombieSound.class);
        DebugLog.log(DebugType.Sound, "sound: received " + sid + " for zombie " + id);
        IsoZombie zom = IDToZombieMap.get(id);
        if (zom != null && zom.getCurrentSquare() != null) {
            float radius = sound.radius();
            switch (sound) {
                case Burned: {
                    String t = zom.getDescriptor().getVoicePrefix() + "Death";
                    zom.getEmitter().playVocals(t);
                    break;
                }
                case DeadCloseKilled: {
                    zom.getEmitter().playSoundImpl("HeadStab", null);
                    String t = zom.getDescriptor().getVoicePrefix() + "Death";
                    zom.getEmitter().playVocals(t);
                    zom.getEmitter().tick();
                    break;
                }
                case DeadNotCloseKilled: {
                    if (zom.isKilledBySlicingWeapon()) {
                        zom.getEmitter().playSoundImpl("HeadSlice", null);
                    } else {
                        zom.getEmitter().playSoundImpl("HeadSmash", null);
                    }
                    String t = zom.getDescriptor().getVoicePrefix() + "Death";
                    zom.getEmitter().playVocals(t);
                    zom.getEmitter().tick();
                    break;
                }
                case Hurt: {
                    zom.playHurtSound();
                    break;
                }
                case Idle: {
                    String t = zom.getDescriptor().getVoicePrefix() + "Idle";
                    zom.getEmitter().playVocals(t);
                    break;
                }
                case Lunge: {
                    String t = zom.getDescriptor().getVoicePrefix() + "Attack";
                    zom.getEmitter().playVocals(t);
                    break;
                }
                default: {
                    DebugLog.log("unhandled zombie sound " + String.valueOf((Object)sound));
                }
            }
        }
    }

    public void eatFood(IsoPlayer player, Food food, float percentage) {
        EatFoodPacket packet = new EatFoodPacket();
        packet.set(player, food, percentage);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.EatFood.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.EatFood.send(connection);
    }

    public void drink(IsoPlayer player, float drink) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.Drink.doPacket(b);
        b.putByte(player.playerIndex);
        b.putFloat(drink);
        PacketTypes.PacketType.Drink.send(connection);
    }

    public void addToItemSendBuffer(IsoObject parent, ItemContainer container, InventoryItem item) {
        ArrayList<InventoryItem> items;
        if (parent instanceof IsoWorldInventoryObject) {
            InventoryContainer inventoryContainer;
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)parent;
            InventoryItem containerItem = isoWorldInventoryObject.getItem();
            if (item == null || containerItem == null || !(containerItem instanceof InventoryContainer) || container != (inventoryContainer = (InventoryContainer)containerItem).getInventory()) {
                DebugLog.log("ERROR: addToItemSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent instanceof BaseVehicle) {
            if (container.vehiclePart == null || container.vehiclePart.getItemContainer() != container || container.vehiclePart.getVehicle() != parent) {
                DebugLog.log("ERROR: addToItemSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent == null || item == null || parent.getContainerIndex(container) == -1) {
            DebugLog.log("ERROR: addToItemSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
            if (Core.debug) {
                throw new IllegalStateException();
            }
            return;
        }
        if (this.itemsToSendRemove.containsKey(container) && (items = this.itemsToSendRemove.get(container)).remove(item)) {
            if (items.isEmpty()) {
                this.itemsToSendRemove.remove(container);
            }
            return;
        }
        if (this.itemsToSend.containsKey(container)) {
            this.itemsToSend.get(container).add(item);
        } else {
            ArrayList<InventoryItem> a = new ArrayList<InventoryItem>();
            this.itemsToSend.put(container, a);
            a.add(item);
        }
    }

    public void addToItemRemoveSendBuffer(IsoObject parent, ItemContainer container, InventoryItem item) {
        if (parent instanceof IsoWorldInventoryObject) {
            InventoryContainer inventoryContainer;
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)parent;
            InventoryItem containerItem = isoWorldInventoryObject.getItem();
            if (item == null || containerItem == null || !(containerItem instanceof InventoryContainer) || container != (inventoryContainer = (InventoryContainer)containerItem).getInventory()) {
                DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent instanceof BaseVehicle) {
            if (container.vehiclePart == null || container.vehiclePart.getItemContainer() != container || container.vehiclePart.getVehicle() != parent) {
                DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent instanceof IsoDeadBody) {
            if (item == null || container != parent.getContainer()) {
                DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent == null || item == null || parent.getContainerIndex(container) == -1) {
            DebugLog.log("ERROR: addToItemRemoveSendBuffer parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
            if (Core.debug) {
                throw new IllegalStateException();
            }
            return;
        }
        if (!SystemDisabler.doWorldSyncEnable) {
            ArrayList<InventoryItem> items;
            if (this.itemsToSend.containsKey(container) && (items = this.itemsToSend.get(container)).remove(item)) {
                if (items.isEmpty()) {
                    this.itemsToSend.remove(container);
                }
                return;
            }
            if (this.itemsToSendRemove.containsKey(container)) {
                this.itemsToSendRemove.get(container).add(item);
            } else {
                items = new ArrayList();
                items.add(item);
                this.itemsToSendRemove.put(container, items);
            }
            return;
        }
        INetworkPacket.send(PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, item);
    }

    public void sendAddedRemovedItems(boolean force) {
        int n;
        IsoObject o;
        ArrayList<InventoryItem> items;
        ItemContainer container;
        boolean sendFrequencyCheck;
        boolean bl = sendFrequencyCheck = force || this.itemSendFrequency.Check();
        if (!SystemDisabler.doWorldSyncEnable && !this.itemsToSendRemove.isEmpty() && sendFrequencyCheck) {
            for (Map.Entry<ItemContainer, ArrayList<InventoryItem>> pair : this.itemsToSendRemove.entrySet()) {
                container = pair.getKey();
                items = pair.getValue();
                o = container.getParent();
                if (container.getContainingItem() != null && container.getContainingItem().getWorldItem() != null) {
                    o = container.getContainingItem().getWorldItem();
                }
                if (o == null || o.square == null) continue;
                try {
                    INetworkPacket.send(PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, items);
                }
                catch (Exception ex) {
                    DebugLog.log("sendAddedRemovedItems: itemsToSendRemove container:" + String.valueOf(container) + "." + String.valueOf(o) + " items:" + String.valueOf(items));
                    if (items != null) {
                        for (n = 0; n < items.size(); ++n) {
                            if (items.get(n) == null) {
                                DebugLog.log("item:null");
                                continue;
                            }
                            DebugLog.log("item:" + items.get(n).getName());
                        }
                        DebugLog.log("itemSize:" + items.size());
                    }
                    ex.printStackTrace();
                    connection.cancelPacket();
                }
            }
            this.itemsToSendRemove.clear();
        }
        if (!this.itemsToSend.isEmpty() && sendFrequencyCheck) {
            for (Map.Entry<ItemContainer, ArrayList<InventoryItem>> pair : this.itemsToSend.entrySet()) {
                container = pair.getKey();
                items = pair.getValue();
                o = container.getParent();
                if (container.getContainingItem() != null && container.getContainingItem().getWorldItem() != null) {
                    o = container.getContainingItem().getWorldItem();
                }
                if (o == null || o.square == null) continue;
                try {
                    INetworkPacket.send(PacketTypes.PacketType.AddInventoryItemToContainer, container, items);
                }
                catch (Exception ex) {
                    DebugLog.log("sendAddedRemovedItems: itemsToSend container:" + String.valueOf(container) + "." + String.valueOf(o) + " items:" + String.valueOf(items));
                    if (items != null) {
                        for (n = 0; n < items.size(); ++n) {
                            if (items.get(n) == null) {
                                DebugLog.log("item:null");
                                continue;
                            }
                            DebugLog.log("item:" + items.get(n).getName());
                        }
                        DebugLog.log("itemSize:" + items.size());
                    }
                    ex.printStackTrace();
                    connection.cancelPacket();
                }
            }
            this.itemsToSend.clear();
        }
    }

    public void checkAddedRemovedItems(IsoObject aboutToRemove) {
        if (aboutToRemove == null) {
            return;
        }
        if (this.itemsToSend.isEmpty() && this.itemsToSendRemove.isEmpty()) {
            return;
        }
        if (aboutToRemove instanceof IsoDeadBody) {
            if (this.itemsToSend.containsKey(aboutToRemove.getContainer()) || this.itemsToSendRemove.containsKey(aboutToRemove.getContainer())) {
                this.sendAddedRemovedItems(true);
            }
            return;
        }
        if (aboutToRemove instanceof IsoWorldInventoryObject) {
            InventoryContainer inventoryContainer;
            ItemContainer container;
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)aboutToRemove;
            InventoryItem containerItem = isoWorldInventoryObject.getItem();
            if (containerItem instanceof InventoryContainer && (this.itemsToSend.containsKey(container = (inventoryContainer = (InventoryContainer)containerItem).getInventory()) || this.itemsToSendRemove.containsKey(container))) {
                this.sendAddedRemovedItems(true);
            }
            return;
        }
        if (aboutToRemove instanceof BaseVehicle) {
            return;
        }
        for (int i = 0; i < aboutToRemove.getContainerCount(); ++i) {
            ItemContainer container = aboutToRemove.getContainerByIndex(i);
            if (!this.itemsToSend.containsKey(container) && !this.itemsToSendRemove.containsKey(container)) continue;
            this.sendAddedRemovedItems(true);
            return;
        }
    }

    public static void sendRemoveItemFromContainer(ItemContainer container, InventoryItem item) {
        INetworkPacket.send(PacketTypes.PacketType.SyncItemDelete, container, item);
    }

    public void sendItemStats(InventoryItem item) {
        IsoObject parent;
        if (item == null) {
            return;
        }
        if (item.getWorldItem() != null && item.getWorldItem().getWorldObjectIndex() != -1) {
            IsoWorldInventoryObject o = item.getWorldItem();
            INetworkPacket.send(PacketTypes.PacketType.ItemStats, item.getContainer(), item);
            return;
        }
        if (item.getContainer() == null) {
            DebugLog.log("ERROR: sendItemStats(): item is neither in a container nor on the ground");
            if (Core.debug) {
                throw new IllegalStateException();
            }
            return;
        }
        ItemContainer container = item.getContainer();
        IsoObject o = container.getParent();
        if (container.getContainingItem() != null && container.getContainingItem().getWorldItem() != null) {
            o = container.getContainingItem().getWorldItem();
        }
        if ((parent = o) instanceof IsoWorldInventoryObject) {
            InventoryContainer inventoryContainer;
            IsoWorldInventoryObject isoWorldInventoryObject = (IsoWorldInventoryObject)parent;
            InventoryItem containerItem = isoWorldInventoryObject.getItem();
            if (!(containerItem instanceof InventoryContainer) || container != (inventoryContainer = (InventoryContainer)containerItem).getInventory()) {
                DebugLog.log("ERROR: sendItemStats() parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent instanceof BaseVehicle) {
            if (container.vehiclePart == null || container.vehiclePart.getItemContainer() != container || container.vehiclePart.getVehicle() != parent) {
                DebugLog.log("ERROR: sendItemStats() parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent instanceof IsoDeadBody) {
            if (container != parent.getContainer()) {
                DebugLog.log("ERROR: sendItemStats() parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
                if (Core.debug) {
                    throw new IllegalStateException();
                }
                return;
            }
        } else if (parent == null || parent.getContainerIndex(container) == -1) {
            DebugLog.log("ERROR: sendItemStats() parent=" + String.valueOf(parent) + " item=" + String.valueOf(item));
            if (Core.debug) {
                throw new IllegalStateException();
            }
            return;
        }
        INetworkPacket.send(PacketTypes.PacketType.ItemStats, container, item);
    }

    public void PlayWorldSound(String name, int x, int y, byte z) {
        PlayWorldSoundPacket packet = new PlayWorldSoundPacket();
        packet.set(name, x, y, z, -1);
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.PlayWorldSound.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.PlayWorldSound.send(connection);
    }

    public void StopSound(IsoMovingObject object, String soundName, boolean trigger) {
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.StopSound.doPacket(bbw);
        StopSoundPacket packet = new StopSoundPacket();
        packet.set(object, soundName, trigger);
        packet.write(bbw);
        PacketTypes.PacketType.StopSound.send(connection);
    }

    public void startLocalServer() throws Exception {
        client = true;
        clientSave = true;
        ip = "127.0.0.1";
        Thread serverThread = new Thread(ThreadGroups.Workers, () -> {
            String separator = System.getProperty("file.separator");
            String classpath = System.getProperty("java.class.path");
            String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
            ProcessBuilder processBuilder = new ProcessBuilder(path, "-Xms2048m", "-Xmx2048m", "-Djava.library.path=../natives/", "-cp", "lwjgl.jar;lwjgl_util.jar;sqlitejdbc-v056.jar;../bin/", "zombie.network.GameServer");
            processBuilder.redirectErrorStream(true);
            Process process = null;
            try {
                process = processBuilder.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            InputStreamReader reader = new InputStreamReader(process.getInputStream());
            boolean bDone = false;
            try {
                while (!((Reader)reader).ready()) {
                    try {
                        int ch;
                        while ((ch = ((Reader)reader).read()) != -1) {
                            System.out.print((char)ch);
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        ((Reader)reader).close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        serverThread.start();
    }

    public static void sendPing() {
        if (client) {
            ByteBufferWriter bb = connection.startPingPacket();
            PacketTypes.doPingPacket(bb);
            bb.putLong(System.currentTimeMillis());
            bb.putLong(0L);
            connection.endPingPacket();
        }
    }

    public IsoPlayer getPlayerFromUsername(String username) {
        if (username != null) {
            ArrayList<IsoPlayer> players = this.getPlayers();
            for (int i = 0; i < players.size(); ++i) {
                IsoPlayer player = players.get(i);
                if (!player.getUsername().equals(username)) continue;
                return player;
            }
        }
        return null;
    }

    public static void destroy(IsoObject obj) {
        if (ServerOptions.instance.allowDestructionBySledgehammer.getValue()) {
            SledgehammerDestroyPacket packet = new SledgehammerDestroyPacket();
            packet.set(obj);
            ByteBufferWriter bb = connection.startPacket();
            PacketTypes.PacketType.SledgehammerDestroy.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.SledgehammerDestroy.send(connection);
            obj.getSquare().RemoveTileObject(obj);
        }
    }

    public static void sendStopFire(IsoGridSquare sq) {
        INetworkPacket.send(PacketTypes.PacketType.StopFire, sq);
    }

    public static void receiveRadioDeviceDataState(ByteBufferReader bb, short packetType) {
        byte deviceType = bb.getByte();
        if (deviceType == 1) {
            IsoWaveSignal isoWaveSignal;
            DeviceData deviceData;
            IsoObject obj;
            int x = bb.getInt();
            int y = bb.getInt();
            int z = bb.getInt();
            int index = bb.getInt();
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (sq != null && index >= 0 && index < sq.getObjects().size() && (obj = sq.getObjects().get(index)) instanceof IsoWaveSignal && (deviceData = (isoWaveSignal = (IsoWaveSignal)obj).getDeviceData()) != null) {
                try {
                    deviceData.receiveDeviceDataStatePacket(bb, null);
                }
                catch (Exception e) {
                    System.out.print(e.getMessage());
                }
            }
        } else if (deviceType == 0) {
            short onlineIndex = bb.getShort();
            IsoPlayer player = IDToPlayerMap.get(onlineIndex);
            byte hand = bb.getByte();
            if (player != null) {
                Radio radio = null;
                if (hand == 1 && player.getPrimaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getPrimaryHandItem();
                } else if (hand == 2 && player.getSecondaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getSecondaryHandItem();
                } else if (hand == 0) {
                    int id = bb.getInt();
                    InventoryItem item = player.getInventory().getItemWithID(id);
                    if (item instanceof Radio) {
                        radio = (Radio)item;
                    }
                }
                if (radio != null && radio.getDeviceData() != null) {
                    try {
                        radio.getDeviceData().receiveDeviceDataStatePacket(bb, connection);
                    }
                    catch (Exception e) {
                        System.out.print(e.getMessage());
                    }
                }
            }
        } else if (deviceType == 2) {
            DeviceData deviceData;
            VehiclePart part;
            short vehicleID = bb.getShort();
            short partIndex = bb.getShort();
            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(vehicleID);
            if (vehicle != null && (part = vehicle.getPartByIndex(partIndex)) != null && (deviceData = part.getDeviceData()) != null) {
                try {
                    deviceData.receiveDeviceDataStatePacket(bb, null);
                }
                catch (Exception e) {
                    System.out.print(e.getMessage());
                }
            }
        }
    }

    public static void sendRadioServerDataRequest() {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.RadioServerData.doPacket(bb);
        PacketTypes.PacketType.RadioServerData.send(connection);
    }

    public static void receiveRadioServerData(ByteBufferReader bb, short packetType) {
        ZomboidRadio radio = ZomboidRadio.getInstance();
        int size = bb.getInt();
        for (int i = 0; i < size; ++i) {
            String cat = bb.getUTF();
            int catsize = bb.getInt();
            for (int j = 0; j < catsize; ++j) {
                int freq = bb.getInt();
                String name = bb.getUTF();
                radio.addChannelName(name, freq, cat);
            }
        }
        radio.setHasRecievedServerData(true);
        ZomboidRadio.postRadioSilence = bb.getBoolean();
    }

    public static void receiveRadioPostSilence(ByteBufferReader bb, short packetType) {
        ZomboidRadio.postRadioSilence = bb.getBoolean();
    }

    public static void sendIsoWaveSignal(int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.WaveSignal.doPacket(bb);
        try {
            WaveSignalPacket packet = new WaveSignalPacket();
            packet.set(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
            packet.write(bb);
            PacketTypes.PacketType.WaveSignal.send(connection);
        }
        catch (Exception e) {
            connection.cancelPacket();
            DebugLog.Multiplayer.printException(e, "SendIsoWaveSignal: failed", LogSeverity.Error);
        }
    }

    public static void sendPlayerListensChannel(int channel, boolean listenmode, boolean isTV) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.PlayerListensChannel.doPacket(bb);
        bb.putInt(channel);
        bb.putBoolean(listenmode);
        bb.putBoolean(isTV);
        PacketTypes.PacketType.PlayerListensChannel.send(connection);
    }

    public static void sendCompost(IsoCompost isoCompost) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SyncCompost.doPacket(bb);
        bb.putInt(isoCompost.getSquare().getX());
        bb.putInt(isoCompost.getSquare().getY());
        bb.putInt(isoCompost.getSquare().getZ());
        bb.putUTF(isoCompost.getSpriteName());
        bb.putFloat(isoCompost.getCompost());
        PacketTypes.PacketType.SyncCompost.send(connection);
    }

    static void receiveSyncCompost(ByteBufferReader bb, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        String spriteName = bb.getUTF();
        if (sq != null) {
            IsoCompost compost = sq.getCompost();
            if (compost == null) {
                compost = new IsoCompost(sq.getCell(), sq, spriteName);
                sq.AddSpecialObject(compost);
            }
            compost.setCompost(bb.getFloat());
            compost.updateSprite();
        }
    }

    public void requestUserlog(String username) {
        if (!connection.getRole().hasCapability(Capability.ReadUserLog)) {
            return;
        }
        INetworkPacket.send(PacketTypes.PacketType.RequestUserLog, username);
    }

    public void addUserlog(String user, String type, String text) {
        if (connection.getRole().hasCapability(Capability.AddUserlog)) {
            INetworkPacket.send(PacketTypes.PacketType.AddUserlog, user, type, text);
        }
    }

    public void removeUserlog(String user, String type, String text) {
        if (connection.getRole().hasCapability(Capability.WorkWithUserlog)) {
            INetworkPacket.send(PacketTypes.PacketType.RemoveUserlog, user, type, text);
        }
    }

    public void addWarningPoint(String user, String reason, int amount) {
        if (connection.getRole().hasCapability(Capability.AddUserlog)) {
            INetworkPacket.send(PacketTypes.PacketType.AddWarningPoint, user, reason, amount);
        }
    }

    public ArrayList<IsoPlayer> getConnectedPlayers() {
        return this.connectedPlayers;
    }

    public static void sendNonPvpZone(NonPvpZone nonPvpZone, boolean remove) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SyncNonPvpZone.doPacket(bb);
        nonPvpZone.save(bb.bb);
        bb.putBoolean(remove);
        PacketTypes.PacketType.SyncNonPvpZone.send(connection);
    }

    public static void sendFaction(Faction faction, boolean remove) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SyncFaction.doPacket(bb);
        faction.writeToBuffer(bb, remove);
        PacketTypes.PacketType.SyncFaction.send(connection);
    }

    public static void sendFactionInvite(Faction faction, IsoPlayer host, String invited) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SendFactionInvite.doPacket(bb);
        bb.putUTF(faction.getName());
        bb.putUTF(host.getUsername());
        bb.putUTF(invited);
        PacketTypes.PacketType.SendFactionInvite.send(connection);
    }

    static void receiveSendFactionInvite(ByteBufferReader bb, short packetType) {
        String factionName = bb.getUTF();
        String host = bb.getUTF();
        LuaEventManager.triggerEvent("ReceiveFactionInvite", factionName, host);
    }

    public static void acceptFactionInvite(Faction faction, String host) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.AcceptedFactionInvite.doPacket(bb);
        bb.putUTF(faction.getName());
        bb.putUTF(host);
        PacketTypes.PacketType.AcceptedFactionInvite.send(connection);
    }

    static void receiveAcceptedFactionInvite(ByteBufferReader bb, short packetType) {
        String factionName = bb.getUTF();
        String host = bb.getUTF();
        Faction faction = Faction.getFaction(factionName);
        if (faction != null) {
            faction.addPlayer(host);
        }
        LuaEventManager.triggerEvent("AcceptedFactionInvite", factionName, host);
    }

    public static void getBannedIPs() {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.ViewBannedIPs.doPacket(bb);
        PacketTypes.PacketType.ViewBannedIPs.send(connection);
    }

    static void receiveViewBannedIPs(ByteBufferReader bb, short packetType) {
        ArrayList<DBBannedIP> result = new ArrayList<DBBannedIP>();
        int size = bb.getInt();
        for (int i = 0; i < size; ++i) {
            DBBannedIP newBannedIP = new DBBannedIP(bb.getUTF(), bb.getUTF(), bb.getUTF());
            result.add(newBannedIP);
        }
        LuaEventManager.triggerEvent("ViewBannedIPs", result);
    }

    public static void getBannedSteamIDs() {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.ViewBannedSteamIDs.doPacket(bb);
        PacketTypes.PacketType.ViewBannedSteamIDs.send(connection);
    }

    static void receiveViewBannedSteamIDs(ByteBufferReader bb, short packetType) {
        ArrayList<DBBannedSteamID> result = new ArrayList<DBBannedSteamID>();
        int size = bb.getInt();
        for (int i = 0; i < size; ++i) {
            DBBannedSteamID newBannedSteamID = new DBBannedSteamID(bb.getUTF(), bb.getUTF());
            result.add(newBannedSteamID);
        }
        LuaEventManager.triggerEvent("ViewBannedSteamIDs", result);
    }

    public static boolean sendItemListNet(IsoPlayer sender, ArrayList<InventoryItem> items, IsoPlayer receiver, String sessionID, String custom) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SendItemListNet.doPacket(bb);
        if (bb.putBoolean(receiver != null)) {
            bb.putShort(receiver.getOnlineID());
        }
        if (bb.putBoolean(sender != null)) {
            bb.putShort(sender.getOnlineID());
        }
        bb.putUTF(sessionID);
        if (bb.putBoolean(custom != null)) {
            bb.putUTF(custom);
        }
        try {
            CompressIdenticalItems.save(bb.bb, items, null);
        }
        catch (Exception e) {
            e.printStackTrace();
            connection.cancelPacket();
            return false;
        }
        PacketTypes.PacketType.SendItemListNet.send(connection);
        return true;
    }

    public static void receiveSendItemListNet(ByteBufferReader bb, short packetType) {
        IsoPlayer receiver = null;
        if (bb.getBoolean()) {
            receiver = IDToPlayerMap.get(bb.getShort());
        }
        IsoPlayer sender = null;
        if (bb.getBoolean()) {
            sender = IDToPlayerMap.get(bb.getShort());
        }
        String sessionID = bb.getUTF();
        String custom = null;
        if (bb.getBoolean()) {
            custom = bb.getUTF();
        }
        int itemCount = bb.getShort();
        ArrayList<InventoryItem> items = new ArrayList<InventoryItem>(itemCount);
        try {
            for (int i = 0; i < itemCount; ++i) {
                InventoryItem item = InventoryItem.loadItem(bb.bb, 244);
                if (item == null) continue;
                items.add(item);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        LuaEventManager.triggerEvent("OnReceiveItemListNet", sender, items, receiver, sessionID, custom);
    }

    public void requestTrading(IsoPlayer you, IsoPlayer other) {
        RequestTradingPacket packet = new RequestTradingPacket();
        packet.ask(you, other);
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.RequestTrading.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.RequestTrading.send(connection);
    }

    public void acceptTrading(IsoPlayer you, IsoPlayer other, boolean accept) {
        RequestTradingPacket packet = new RequestTradingPacket();
        if (accept) {
            packet.accept(you, other);
        } else {
            packet.reject(you, other);
        }
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.RequestTrading.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.RequestTrading.send(connection);
    }

    public void tradingUISendAddItem(IsoPlayer you, IsoPlayer other, InventoryItem item) {
        TradingUIAddItemPacket packet = new TradingUIAddItemPacket();
        packet.set(you, other, item);
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.TradingUIAddItem.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.TradingUIAddItem.send(connection);
    }

    public void tradingUISendRemoveItem(IsoPlayer you, IsoPlayer other, InventoryItem item) {
        TradingUIRemoveItemPacket packet = new TradingUIRemoveItemPacket();
        packet.set(you, other, item);
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.TradingUIRemoveItem.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.TradingUIRemoveItem.send(connection);
    }

    public void tradingUISendUpdateState(IsoPlayer you, IsoPlayer other, int state) {
        TradingUIUpdateStatePacket packet = new TradingUIUpdateStatePacket();
        packet.set(you, other, (byte)state);
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.TradingUIUpdateState.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.TradingUIUpdateState.send(connection);
    }

    public static void receiveSpawnRegion(ByteBufferReader bb, short packetType) {
        if (GameClient.instance.serverSpawnRegions == null) {
            GameClient.instance.serverSpawnRegions = LuaManager.platform.newTable();
        }
        int index = bb.getInt();
        KahluaTable newReg = LuaManager.platform.newTable();
        try {
            newReg.load(bb.bb, 244);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        GameClient.instance.serverSpawnRegions.rawset(index, (Object)newReg);
    }

    public static void receiveClimateManagerPacket(ByteBufferReader bb, short packetType) {
        ClimateManager cm = ClimateManager.getInstance();
        if (cm != null) {
            try {
                cm.receiveClimatePacket(bb, null);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void receiveIsoRegionServerPacket(ByteBufferReader bb, short packetType) {
        IsoRegions.receiveServerUpdatePacket(bb);
    }

    public static void sendIsoRegionDataRequest() {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.doPacket(bb);
        PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.send(connection);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendSandboxOptionsToServer(SandboxOptions options) {
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.SandboxOptions.doPacket(bbw);
        try {
            options.save(bbw.bb);
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
        }
        finally {
            PacketTypes.PacketType.SandboxOptions.send(connection);
        }
    }

    public static void receiveSandboxOptions(ByteBufferReader bb, short packetType) {
        try {
            SandboxOptions.instance.load(bb.bb);
            SandboxOptions.instance.applySettings();
            SandboxOptions.instance.toLua();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public static void receiveChunkObjectState(ByteBufferReader bb, short packetType) {
        short wy;
        short wx = bb.getShort();
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(wx, wy = bb.getShort());
        if (chunk == null) {
            return;
        }
        try {
            chunk.loadObjectState(bb.bb);
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
    }

    public static void receivePlayerLeaveChat(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processLeaveChatPacket(bb);
    }

    public static void receiveInitPlayerChat(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processInitPlayerChatPacket(bb);
    }

    public static void receiveAddChatTab(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processAddTabPacket(bb);
    }

    public static void receiveRemoveChatTab(ByteBufferReader bb, short packetType) {
        ChatManager.getInstance().processRemoveTabPacket(bb);
    }

    public static void receivePlayerNotFound(ByteBufferReader bb, short packetType) {
        String destPlayerName = bb.getUTF();
        ChatManager.getInstance().processPlayerNotFound(destPlayerName);
    }

    public static void sendPerks(IsoPlayer player) {
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.SyncPerks.doPacket(b);
        b.putByte(player.playerIndex);
        b.putInt(player.getPerkLevel(PerkFactory.Perks.Sneak));
        b.putInt(player.getPerkLevel(PerkFactory.Perks.Strength));
        b.putInt(player.getPerkLevel(PerkFactory.Perks.Fitness));
        PacketTypes.PacketType.SyncPerks.send(connection);
    }

    public static void receiveSyncPerks(ByteBufferReader bb, short packetType) {
        short id = bb.getShort();
        int sneakLvl = bb.getInt();
        int strLvl = bb.getInt();
        int fitLvl = bb.getInt();
        IsoPlayer p = IDToPlayerMap.get(id);
        if (p == null || p.isLocalPlayer()) {
            return;
        }
        p.remoteSneakLvl = sneakLvl;
        p.remoteStrLvl = strLvl;
        p.remoteFitLvl = fitLvl;
    }

    public static void sendEquippedRadioFreq(IsoPlayer plyr) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SyncEquippedRadioFreq.doPacket(bb);
        bb.putByte(plyr.playerIndex);
        bb.putInt(plyr.invRadioFreq.size());
        for (int i = 0; i < plyr.invRadioFreq.size(); ++i) {
            bb.putInt((Integer)plyr.invRadioFreq.get(i));
        }
        PacketTypes.PacketType.SyncEquippedRadioFreq.send(connection);
    }

    public static void receiveSyncEquippedRadioFreq(ByteBufferReader bb, short packetType) {
        short id = bb.getShort();
        int size = bb.getInt();
        IsoPlayer p = IDToPlayerMap.get(id);
        if (p != null) {
            int i;
            p.invRadioFreq.clear();
            for (i = 0; i < size; ++i) {
                p.invRadioFreq.add(bb.getInt());
            }
            for (i = 0; i < p.invRadioFreq.size(); ++i) {
                System.out.println(p.invRadioFreq.get(i));
            }
        }
    }

    public static void sendSneezingCoughing(IsoPlayer player, int sneezingCoughing, byte sneezeVar) {
        SneezeCoughPacket packet = new SneezeCoughPacket();
        packet.set(player, sneezingCoughing, sneezeVar);
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.SneezeCough.doPacket(bb);
        packet.write(bb);
        PacketTypes.PacketType.SneezeCough.send(connection);
    }

    public static void rememberPlayerPosition(IsoPlayer player, float x, float y) {
        if (player == null || player.isLocalPlayer()) {
            return;
        }
        if (positions.containsKey(player.getOnlineID())) {
            positions.get(player.getOnlineID()).set(x, y);
        } else {
            positions.put(player.getOnlineID(), new Vector2(x, y));
        }
        WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getPlayerByID(player.getOnlineID());
        if (remotePlayer != null) {
            remotePlayer.setPosition(x, y);
        }
        player.setLastRemoteUpdate(System.currentTimeMillis());
    }

    public static KahluaTable sortBrowserList(KahluaTableImpl table, String sortType, boolean sortDown, KahluaTableImpl filterTable) {
        KahluaTable newTable = LuaManager.platform.newTable();
        ArrayList<Server> sortedList = new ArrayList<Server>();
        KahluaTableIterator it = table.iterator();
        boolean filterVersion = filterTable.rawgetBool("version");
        boolean filterEmpty = filterTable.rawgetBool("empty");
        boolean filterWhitelist = filterTable.rawgetBool("whitelist");
        boolean filterPassword = filterTable.rawgetBool("password");
        boolean filterFull = filterTable.rawgetBool("full");
        boolean filterModded = filterTable.rawgetBool("modded");
        String filterName = filterTable.rawgetStr("name").toLowerCase();
        while (it.advance()) {
            Server server = (Server)it.getValue();
            if (!StringUtils.isNullOrEmpty(filterName) && !server.getName().toLowerCase().contains(filterName) || !filterVersion && !server.getVersion().equals(Core.getInstance().getVersionNumber()) || !filterEmpty && Integer.parseInt(server.getPlayers()) <= 0 || !filterWhitelist && !server.isOpen() || !filterPassword && server.isPasswordProtected() || !filterFull && Integer.parseInt(server.getPlayers()) >= Integer.parseInt(server.getMaxPlayers()) || !filterModded && !StringUtils.isNullOrEmpty(server.getMods())) continue;
            sortedList.add(server);
        }
        Comparator<Server> comparator = switch (sortType) {
            case "player" -> Comparator.comparingInt(a -> Integer.parseInt(a.getPlayers()));
            case "ping" -> Comparator.comparingInt(a -> Integer.parseInt(a.getPing()));
            default -> Comparator.comparing(Server::getName);
        };
        sortedList.sort(sortDown ? comparator.reversed() : comparator);
        int size = sortedList.size();
        for (int i = 0; i < size; ++i) {
            Server server = (Server)sortedList.get(i);
            newTable.rawset(server.getIp() + server.getName() + Rand.Next(0, 10000), (Object)server);
        }
        return newTable;
    }

    static {
        ip = "localhost";
        serverName = "";
        localIP = "";
        password = "testpass";
        googleKey = "";
        username = "lemmy101";
        serverPassword = "";
        authType = 1;
        port = GameServer.defaultPort;
        checksum = "";
        pingsList = new ArrayList<Long>();
        loadedCells = new ClientServerMap[4];
        positions = new HashMap<Short, Vector2>(ServerOptions.getInstance().getMaxPlayers());
        tempShortList = new TShortArrayList();
        MainLoopNetDataQ = new ConcurrentLinkedQueue();
        MainLoopNetData = new ArrayList();
        LoadingMainLoopNetData = new ArrayList();
        DelayedCoopNetData = new ArrayList();
        IDToPlayerMap = new HashMap();
        IDToZombieMap = new TShortObjectHashMap();
    }

    public static enum RequestState {
        Start,
        Loading,
        Complete;

    }
}

