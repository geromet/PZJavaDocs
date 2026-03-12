/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientSoundManager;
import zombie.AmbientStreamManager;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.MapCollisionData;
import zombie.PersistentOutfits;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.asset.AssetManagers;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Role;
import zombie.characters.Roles;
import zombie.characters.Safety;
import zombie.characters.SafetySystemManager;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.commands.CommandBase;
import zombie.core.ActionManager;
import zombie.core.Core;
import zombie.core.ImportantAreaManager;
import zombie.core.Languages;
import zombie.core.PerformanceSettings;
import zombie.core.ProxyPrintStream;
import zombie.core.ThreadGroups;
import zombie.core.TradingManager;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.WordsFilter;
import zombie.core.backup.ZipBackup;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LimitSizeFileOutputStream;
import zombie.core.logger.LoggerManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.PhysicsShape;
import zombie.core.physics.PhysicsShapeAssetManager;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileFrameProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.IsoObjectChange;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.UdpEngine;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.model.AiSceneAsset;
import zombie.core.skinnedmodel.model.AiSceneAssetManager;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.AnimationAssetManager;
import zombie.core.skinnedmodel.model.MeshAssetManager;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelAssetManager;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.ClothingItemAssetManager;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.AnimatedTextureID;
import zombie.core.textures.AnimatedTextureIDAssetManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.NinePatchTextureAssetManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureAssetManager;
import zombie.core.textures.TextureID;
import zombie.core.textures.TextureIDAssetManager;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.GameServerDetails;
import zombie.core.znet.PortMapper;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.IngameState;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Radio;
import zombie.iso.BuildingDef;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.iso.worldgen.WorldGenUtils;
import zombie.iso.zones.Zone;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.CoopSlave;
import zombie.network.CustomizationManager;
import zombie.network.DBBannedIP;
import zombie.network.DBBannedSteamID;
import zombie.network.DBTicket;
import zombie.network.DiscordBot;
import zombie.network.GameServerWorkshopItems;
import zombie.network.IConnection;
import zombie.network.IZomboidPacket;
import zombie.network.LoginQueue;
import zombie.network.NetworkAIParams;
import zombie.network.NetworkPlayerManager;
import zombie.network.PacketTypes;
import zombie.network.PlayerDownloadServer;
import zombie.network.RCONServer;
import zombie.network.RequestDataManager;
import zombie.network.Server;
import zombie.network.ServerGUI;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.ServerPlayersVehicles;
import zombie.network.ServerWorldDatabase;
import zombie.network.StackBot;
import zombie.network.TableNetworkUtils;
import zombie.network.WarManager;
import zombie.network.ZomboidNetData;
import zombie.network.ZomboidNetDataPool;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.chat.ChatServer;
import zombie.network.id.ObjectIDManager;
import zombie.network.packets.AddBrokenGlassPacket;
import zombie.network.packets.AddXpPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.MessageForAdminPacket;
import zombie.network.packets.MetaGridPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.RequestDataPacket;
import zombie.network.packets.SafetyPacket;
import zombie.network.packets.SyncVisualsPacket;
import zombie.network.packets.VariableSyncPacket;
import zombie.network.packets.WaveSignalPacket;
import zombie.network.packets.WeatherPacket;
import zombie.network.packets.ZombieHelmetFallingPacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.actions.HelicopterPacket;
import zombie.network.packets.actions.SmashWindowPacket;
import zombie.network.packets.hit.HitCharacter;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.network.packets.sound.PlayWorldSoundPacket;
import zombie.network.packets.sound.WorldSoundPacket;
import zombie.network.server.EventManager;
import zombie.network.statistics.StatisticManager;
import zombie.network.statistics.data.NetworkStatistic;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.NetworkZombieManager;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ServerPlayerDB;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModRegistries;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileSeamManager;
import zombie.util.PZSQLUtils;
import zombie.util.PublicServerUtil;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMapRemotePlayer;
import zombie.worldMap.WorldMapRemotePlayers;
import zombie.worldMap.network.WorldMapServer;

public class GameServer {
    public static final int MAX_PLAYERS = 512;
    public static final int TimeLimitForProcessPackets = 70;
    public static final int PacketsUpdateRate = 200;
    public static final int FPS = 10;
    private static final HashMap<String, CCFilter> ccFilters = new HashMap();
    public static int test = 432432;
    public static int defaultPort = 16261;
    public static int udpPort = 16262;
    public static String ipCommandline;
    public static int portCommandline;
    public static int udpPortCommandline;
    public static Boolean steamVacCommandline;
    public static boolean guiCommandline;
    public static boolean server;
    public static boolean coop;
    public static boolean debug;
    public static boolean closed;
    public static boolean softReset;
    public static String seed;
    public static UdpEngine udpEngine;
    public static final HashMap<Short, Long> IDToAddressMap;
    public static final HashMap<Short, IsoPlayer> IDToPlayerMap;
    public static final ArrayList<IsoPlayer> Players;
    public static float timeSinceKeepAlive;
    public static final HashSet<UdpConnection> DebugPlayer;
    public static int resetId;
    public static final ArrayList<String> ServerMods;
    public static final ArrayList<Long> WorkshopItems;
    public static String[] workshopInstallFolders;
    public static long[] workshopTimeStamps;
    public static String serverName;
    public static final DiscordBot discordBot;
    public static String checksum;
    public static String gameMap;
    public static boolean fastForward;
    public static String ip;
    public static final UdpConnection[] SlotToConnection;
    public static final HashMap<IsoPlayer, Long> PlayerToAddressMap;
    private static boolean done;
    private static boolean launched;
    private static final ArrayList<String> consoleCommands;
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopPlayerUpdateQ;
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataHighPriorityQ;
    private static final ConcurrentLinkedQueue<IZomboidPacket> MainLoopNetDataQ;
    private static final ArrayList<IZomboidPacket> MainLoopNetData2;
    public static final HashMap<Short, Vector2> playerToCoordsMap;
    private String poisonousBerry;
    private String poisonousMushroom;
    private String difficulty = "Hardcore";
    private static int droppedPackets;
    private static int countOfDroppedPackets;
    public static int countOfDroppedConnections;
    public static UdpConnection removeZombiesConnection;
    public static UdpConnection removeAnimalsConnection;
    public static UdpConnection removeCorpsesConnection;
    public static UdpConnection removeVehiclesConnection;
    private static final UpdateLimit calcCountPlayersInRelevantPositionLimiter;
    private static final UpdateLimit sendWorldMapPlayerPositionLimiter;
    private static int mainCycleExceptionLogCount;
    public static Thread mainThread;
    public static final ArrayList<IsoPlayer> tempPlayers;
    private static final ConcurrentHashMap<String, DelayedConnection> MainLoopDelayedDisconnectQ;
    private static final Thread shutdownHook;

    private static String parseIPFromCommandline(String[] args2, int n, String option) {
        if (n == args2.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args2[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"\" + option + \"\"");
            System.exit(0);
        } else {
            String[] ss = args2[n + 1].trim().split("\\.");
            if (ss.length == 4) {
                for (int i = 0; i < 4; ++i) {
                    try {
                        int octet = Integer.parseInt(ss[i]);
                        if (octet >= 0 && octet <= 255) continue;
                        DebugLog.log("expected IP address after \"" + option + "\", got \"" + args2[n + 1] + "\"");
                        System.exit(0);
                        continue;
                    }
                    catch (NumberFormatException e) {
                        DebugLog.log("expected IP address after \"" + option + "\", got \"" + args2[n + 1] + "\"");
                        System.exit(0);
                    }
                }
            } else {
                DebugLog.log("expected IP address after \"" + option + "\", got \"" + args2[n + 1] + "\"");
                System.exit(0);
            }
        }
        return args2[n + 1];
    }

    private static int parsePortFromCommandline(String[] args2, int n, String option) {
        if (n == args2.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args2[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"" + option + "\"");
            System.exit(0);
        } else {
            try {
                return Integer.parseInt(args2[n + 1].trim());
            }
            catch (NumberFormatException e) {
                DebugLog.log("expected an integer after \"" + option + "\"");
                System.exit(0);
            }
        }
        return -1;
    }

    private static boolean parseBooleanFromCommandline(String[] args2, int n, String option) {
        if (n == args2.length - 1) {
            DebugLog.log("expected argument after \"" + option + "\"");
            System.exit(0);
        } else if (args2[n + 1].trim().isEmpty()) {
            DebugLog.log("empty argument given to \"" + option + "\"");
            System.exit(0);
        } else {
            String arg = args2[n + 1].trim();
            if ("true".equalsIgnoreCase(arg)) {
                return true;
            }
            if ("false".equalsIgnoreCase(arg)) {
                return false;
            }
            DebugLog.log("expected true or false after \"" + option + "\"");
            System.exit(0);
        }
        return false;
    }

    public static void setupCoop() throws FileNotFoundException {
        CoopSlave.init();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void main(String[] args2) {
        String mods;
        String map;
        String s;
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        mainThread = Thread.currentThread();
        server = true;
        softReset = System.getProperty("softreset") != null;
        Core.getInstance().setConsoleDotTxtSizeKB(20480);
        String consoleDotTxtSizeString = System.getProperty("zomboid.ConsoleDotTxtSizeKB");
        Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);
        for (int n = 0; n < args2.length; ++n) {
            if (args2[n] == null) continue;
            if (args2[n].startsWith("-cachedir=")) {
                ZomboidFileSystem.instance.setCacheDir(args2[n].replace("-cachedir=", "").trim());
                continue;
            }
            if (args2[n].startsWith("-console_dot_txt_size_kb=")) {
                consoleDotTxtSizeString = args2[n].replace("-console_dot_txt_size_kb=", "").trim();
                Core.getInstance().setConsoleDotTxtSizeKB(consoleDotTxtSizeString);
                continue;
            }
            if (!args2[n].equals("-coop")) continue;
            coop = true;
        }
        if (coop) {
            try {
                CoopSlave.initStreams();
            }
            catch (FileNotFoundException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        } else {
            try {
                String logFileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "server-console.txt";
                int consoleDotTxtSizeKB = Core.getInstance().getConsoleDotTxtSizeKB();
                LimitSizeFileOutputStream fout = new LimitSizeFileOutputStream(new File(logFileName), consoleDotTxtSizeKB);
                PrintStream fileStream = new PrintStream(fout, true);
                System.setOut(new ProxyPrintStream(System.out, fileStream));
                System.setErr(new ProxyPrintStream(System.err, fileStream));
            }
            catch (FileNotFoundException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
        DebugLog.init();
        LoggerManager.init();
        DebugLog.DetailedInfo.trace("cachedir set to \"" + ZomboidFileSystem.instance.getCacheDir() + "\"");
        if (coop) {
            try {
                GameServer.setupCoop();
                CoopSlave.status("UI_ServerStatus_Initialising");
            }
            catch (FileNotFoundException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                SteamUtils.shutdown();
                System.exit(37);
                return;
            }
        }
        PZSQLUtils.init();
        Clipper.init();
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        DebugLog.General.println("version=%s demo=%s", Core.getInstance().getVersion(), false);
        if (!"62e0c8afb10b8dd38c0cbb2f95d49897058ed073".isEmpty()) {
            DebugLog.General.println("revision=%s date=%s time=%s (%s)", "62e0c8afb10b8dd38c0cbb2f95d49897058ed073", "2026-03-10", "12:36:37", "ZB");
        }
        if (System.getProperty("debug") != null) {
            debug = true;
            Core.debug = true;
        }
        DebugLog.setDefaultLogSeverity();
        DebugLog.printLogLevels();
        seed = WorldGenUtils.INSTANCE.generateSeed();
        for (int n = 0; n < args2.length; ++n) {
            if (args2[n] == null) continue;
            if (args2[n].startsWith("-disablelog=")) {
                for (String string : args2[n].replace("-disablelog=", "").split(",")) {
                    if ("All".equals(string)) {
                        DebugType[] debugTypeArray = DebugType.values();
                        int n2 = debugTypeArray.length;
                        for (int i = 0; i < n2; ++i) {
                            DebugType dt = debugTypeArray[i];
                            DebugLog.setLogEnabled(dt, false);
                        }
                        continue;
                    }
                    try {
                        DebugLog.setLogEnabled(DebugType.valueOf(string), false);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        // empty catch block
                    }
                }
                continue;
            }
            if (args2[n].startsWith("-debuglog=")) {
                for (String string : args2[n].replace("-debuglog=", "").split(",")) {
                    try {
                        DebugLog.setLogEnabled(DebugType.valueOf(string), true);
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        // empty catch block
                    }
                }
                continue;
            }
            if (args2[n].equals("-adminusername")) {
                if (n == args2.length - 1) {
                    DebugLog.log("expected argument after \"-adminusername\"");
                    System.exit(0);
                    continue;
                }
                if (!ServerWorldDatabase.isValidUserName(args2[n + 1].trim())) {
                    DebugLog.log("invalid username given to \"-adminusername\"");
                    System.exit(0);
                    continue;
                }
                ServerWorldDatabase.instance.commandLineAdminUsername = args2[n + 1].trim();
                ++n;
                continue;
            }
            if (args2[n].equals("-adminpassword")) {
                if (n == args2.length - 1) {
                    DebugLog.log("expected argument after \"-adminpassword\"");
                    System.exit(0);
                    continue;
                }
                if (args2[n + 1].trim().isEmpty()) {
                    DebugLog.log("empty argument given to \"-adminpassword\"");
                    System.exit(0);
                    continue;
                }
                ServerWorldDatabase.instance.commandLineAdminPassword = args2[n + 1].trim();
                ++n;
                continue;
            }
            if (args2[n].startsWith("-cachedir=")) continue;
            if (args2[n].equals("-ip")) {
                ipCommandline = GameServer.parseIPFromCommandline(args2, n, "-ip");
                ++n;
                continue;
            }
            if (args2[n].equals("-gui")) {
                guiCommandline = true;
                continue;
            }
            if (args2[n].equals("-nosteam")) {
                System.setProperty("zomboid.steam", "0");
                continue;
            }
            if (args2[n].equals("-port")) {
                portCommandline = GameServer.parsePortFromCommandline(args2, n, "-port");
                ++n;
                continue;
            }
            if (args2[n].equals("-udpport")) {
                udpPortCommandline = GameServer.parsePortFromCommandline(args2, n, "-udpport");
                ++n;
                continue;
            }
            if (args2[n].equals("-steamvac")) {
                steamVacCommandline = GameServer.parseBooleanFromCommandline(args2, n, "-steamvac");
                ++n;
                continue;
            }
            if (args2[n].equals("-servername")) {
                if (n == args2.length - 1) {
                    DebugLog.log("expected argument after \"-servername\"");
                    System.exit(0);
                    continue;
                }
                if (args2[n + 1].trim().isEmpty()) {
                    DebugLog.log("empty argument given to \"-servername\"");
                    System.exit(0);
                    continue;
                }
                serverName = args2[n + 1].trim();
                ++n;
                continue;
            }
            if (args2[n].equals("-coop")) {
                ServerWorldDatabase.instance.doAdmin = false;
                continue;
            }
            if (args2[n].startsWith("-seed=")) {
                try {
                    seed = args2[n].replace("-seed=", "");
                    if (seed.isEmpty()) continue;
                    ServerOptions.instance.seed.setValue(seed);
                }
                catch (IllegalArgumentException consoleDotTxtSizeKB) {}
                continue;
            }
            if (args2[n].equals("-no-worldgen")) {
                IsoChunk.doWorldgen = false;
                continue;
            }
            if (args2[n].equals("-no-foraging")) {
                IsoChunk.doForaging = false;
                continue;
            }
            if (args2[n].equals("-no-attachment")) {
                IsoChunk.doAttachments = false;
                continue;
            }
            DebugLog.log("unknown option \"" + args2[n] + "\"");
        }
        DebugLog.DetailedInfo.trace("server name is \"" + serverName + "\"");
        String versionUnsupportedString = GameServer.isWorldVersionUnsupported();
        if (versionUnsupportedString != null) {
            DebugLog.log(versionUnsupportedString);
            CoopSlave.status(versionUnsupportedString);
            return;
        }
        SteamUtils.init();
        RakNetPeerInterface.init();
        ZombiePopulationManager.init();
        PathfindNative.init();
        try {
            ZomboidFileSystem.instance.init();
            Languages.instance.init();
            Translator.loadFiles();
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
        }
        ServerOptions.instance.init();
        GameServer.initClientCommandFilter();
        if (portCommandline != -1) {
            ServerOptions.instance.defaultPort.setValue(portCommandline);
        }
        if (udpPortCommandline != -1) {
            ServerOptions.instance.udpPort.setValue(udpPortCommandline);
        }
        if (steamVacCommandline != null) {
            ServerOptions.instance.steamVac.setValue(steamVacCommandline);
        }
        defaultPort = ServerOptions.instance.defaultPort.getValue();
        udpPort = ServerOptions.instance.udpPort.getValue();
        if (CoopSlave.instance != null) {
            ServerOptions.instance.serverPlayerId.setValue("");
        }
        if (SteamUtils.isSteamModeEnabled() && ((s = ServerOptions.instance.publicName.getValue()) == null || s.isEmpty())) {
            ServerOptions.instance.publicName.setValue("My PZ Server");
        }
        if ((map = ServerOptions.instance.map.getValue()) != null && !map.trim().isEmpty()) {
            gameMap = map.trim();
            if (gameMap.contains(";")) {
                String[] ss = gameMap.split(";");
                map = ss[0];
            }
            Core.gameMap = map.trim();
        }
        if ((mods = ServerOptions.instance.mods.getValue()) != null) {
            String[] ss;
            for (String modId : ss = mods.replace("\\", "").split(";")) {
                if (modId.trim().isEmpty()) continue;
                ServerMods.add(modId.trim());
            }
        }
        if (SteamUtils.isSteamModeEnabled()) {
            int serverMode;
            int n = serverMode = ServerOptions.instance.steamVac.getValue() ? 3 : 2;
            if (!SteamGameServer.Init(ipCommandline, defaultPort, udpPort, serverMode, Core.getInstance().getSteamServerVersion())) {
                SteamUtils.shutdown();
                return;
            }
            SteamGameServer.SetProduct("zomboid");
            SteamGameServer.SetGameDescription("Project Zomboid");
            SteamGameServer.SetModDir("zomboid");
            SteamGameServer.SetDedicatedServer(true);
            SteamGameServer.SetMaxPlayerCount(ServerOptions.getInstance().getMaxPlayers());
            SteamGameServer.SetServerName(ServerOptions.instance.publicName.getValue());
            SteamGameServer.SetMapName(ServerOptions.instance.map.getValue());
            GameServer.setupSteamGameServer();
            String string = ServerOptions.instance.workshopItems.getValue();
            if (string != null) {
                String[] ss;
                for (String itemID : ss = string.split(";")) {
                    if ((itemID = itemID.trim()).isEmpty() || !SteamUtils.isValidSteamID(itemID)) continue;
                    WorkshopItems.add(SteamUtils.convertStringToSteamID(itemID));
                }
            }
            if (coop) {
                CoopSlave.instance.sendMessage("status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Count", WorkshopItems.size()));
            }
            SteamWorkshop.init();
            SteamGameServer.LogOnAnonymous();
            SteamGameServer.EnableHeartBeats(true);
            DebugLog.log("Waiting for response from Steam servers");
            while (true) {
                SteamUtils.runLoop();
                int state = SteamGameServer.GetSteamServersConnectState();
                if (state == 1) break;
                if (state == 2) {
                    DebugLog.log("Failed to connect to Steam servers");
                    SteamUtils.shutdown();
                    return;
                }
                try {
                    Thread.sleep(100L);
                }
                catch (InterruptedException interruptedException) {}
            }
            if (coop) {
                CoopSlave.status("UI_ServerStatus_Downloading_Workshop_Items");
            }
            if (!GameServerWorkshopItems.Install(WorkshopItems)) {
                return;
            }
        }
        ZipBackup.onStartup();
        ZipBackup.onVersion();
        int updateDBCount = 0;
        try {
            ServerWorldDatabase.instance.create();
        }
        catch (ClassNotFoundException | SQLException exception) {
            DebugLog.General.printException(exception, "", LogSeverity.Error);
        }
        Roles.init();
        if (ServerOptions.instance.uPnp.getValue()) {
            DebugLog.log("Router detection/configuration starting.");
            DebugLog.log("If the server hangs here, set UPnP=false.");
            PortMapper.startup();
            if (PortMapper.discover()) {
                DebugLog.DetailedInfo.trace("UPnP-enabled internet gateway found: " + PortMapper.getGatewayInfo());
                String string = PortMapper.getExternalAddress();
                DebugLog.DetailedInfo.trace("External IP address: " + string);
                DebugLog.log("trying to setup port forwarding rules...");
                int leaseTime = 86400;
                boolean force = true;
                if (PortMapper.addMapping(defaultPort, defaultPort, "PZ Server default port", "UDP", 86400, true)) {
                    DebugLog.log(DebugType.Network, "Default port has been mapped successfully");
                } else {
                    DebugLog.log(DebugType.Network, "Failed to map default port");
                }
                if (SteamUtils.isSteamModeEnabled()) {
                    int udpPort = ServerOptions.instance.udpPort.getValue();
                    if (PortMapper.addMapping(udpPort, udpPort, "PZ Server UDPPort", "UDP", 86400, true)) {
                        DebugLog.log(DebugType.Network, "AdditionUDPPort has been mapped successfully");
                    } else {
                        DebugLog.log(DebugType.Network, "Failed to map AdditionUDPPort");
                    }
                }
            } else {
                DebugLog.log(DebugType.Network, "No UPnP-enabled Internet gateway found, you must configure port forwarding on your gateway manually in order to make your server accessible from the Internet.");
            }
        }
        Core.getInstance().setGameMode("Multiplayer");
        done = false;
        DebugLog.log(DebugType.Network, "Initialising Server Systems...");
        CoopSlave.status("UI_ServerStatus_Initialising");
        try {
            GameServer.doMinimumInit();
        }
        catch (Exception exception) {
            DebugLog.General.printException(exception, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
        }
        LosUtil.init(100, 100);
        ChatServer.getInstance().init();
        DebugLog.log(DebugType.Network, "Loading world...");
        CoopSlave.status("UI_ServerStatus_LoadingWorld");
        try {
            ClimateManager.setInstance(new ClimateManager());
            RagdollSettingsManager.setInstance(new RagdollSettingsManager());
            IsoWorld.instance.init();
        }
        catch (Exception exception) {
            DebugLog.General.printException(exception, "Exception Thrown", LogSeverity.Error);
            DebugLog.General.println("Server Terminated.");
            CoopSlave.status("UI_ServerStatus_Terminated");
            return;
        }
        File file = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");
        if (!file.exists()) {
            ServerOptions.instance.changeOption("ResetID", Integer.toString(Rand.Next(100000000)));
        }
        try {
            SpawnPoints.instance.initServer2(IsoWorld.instance.metaGrid);
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
        LuaEventManager.triggerEvent("OnGameTimeLoaded");
        SGlobalObjects.initSystems();
        SoundManager.instance = new SoundManager();
        AmbientStreamManager.instance = new AmbientSoundManager();
        AmbientStreamManager.instance.init();
        ServerMap.instance.lastSaved = System.currentTimeMillis();
        VehicleManager.instance = new VehicleManager();
        ServerPlayersVehicles.instance.init();
        DebugOptions.instance.init();
        GameProfiler.init();
        WorldMapServer.instance.readSavefile();
        try {
            GameServer.startServer();
        }
        catch (ConnectException e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
            SteamUtils.shutdown();
            return;
        }
        if (SteamUtils.isSteamModeEnabled()) {
            DebugLog.DetailedInfo.trace("##########\nServer Steam ID " + SteamGameServer.GetSteamID() + "\n##########");
        }
        UpdateLimit serverUpdateLimiter = new UpdateLimit(100L);
        PerformanceSettings.setLockFPS(10);
        IngameState state = new IngameState();
        float averageFPS = PerformanceSettings.getLockFPS();
        long serverCycle = System.currentTimeMillis();
        if (!SteamUtils.isSteamModeEnabled()) {
            PublicServerUtil.init();
            PublicServerUtil.insertOrUpdate();
        }
        ServerLOS.init();
        NetworkAIParams.Init();
        int rconPort = ServerOptions.instance.rconPort.getValue();
        String rconPwd = ServerOptions.instance.rconPassword.getValue();
        if (rconPort != 0 && rconPwd != null && !rconPwd.isEmpty()) {
            String isLocal = System.getProperty("rconlo");
            RCONServer.init(rconPort, rconPwd, isLocal != null);
        }
        LuaManager.GlobalObject.refreshAnimSets(true);
        while (!done) {
            try {
                AbstractPerformanceProfileProbe abstractPerformanceProfileProbe;
                Object data;
                long startServerCycle = System.nanoTime();
                MainLoopNetData2.clear();
                IZomboidPacket data2 = MainLoopNetDataHighPriorityQ.poll();
                while (data2 != null) {
                    MainLoopNetData2.add(data2);
                    data2 = MainLoopNetDataHighPriorityQ.poll();
                }
                Iterator<Map.Entry<String, DelayedConnection>> iterator2 = MainLoopDelayedDisconnectQ.entrySet().iterator();
                while (iterator2.hasNext()) {
                    DelayedConnection packet = iterator2.next().getValue();
                    if (!packet.isCooldown()) continue;
                    packet.disconnect();
                    iterator2.remove();
                }
                NetworkStatistic.getInstance().packets.increase(MainLoopNetData2.size());
                for (int n = 0; n < MainLoopNetData2.size(); ++n) {
                    data = MainLoopNetData2.get(n);
                    if (data.isConnect()) {
                        if (!closed) {
                            ((DelayedConnection)data).connect();
                            continue;
                        }
                        ((DelayedConnection)data).connection.forceDisconnect("server-closed");
                        continue;
                    }
                    if (data.isDisconnect()) {
                        ((DelayedConnection)data).disconnect();
                        continue;
                    }
                    GameServer.mainLoopDealWithNetData((ZomboidNetData)data);
                }
                MainLoopNetData2.clear();
                IZomboidPacket data3 = MainLoopPlayerUpdateQ.poll();
                while (data3 != null) {
                    MainLoopNetData2.add(data3);
                    data3 = MainLoopPlayerUpdateQ.poll();
                }
                NetworkStatistic.getInstance().packets.increase(MainLoopNetData2.size());
                for (int n = 0; n < MainLoopNetData2.size(); ++n) {
                    data = MainLoopNetData2.get(n);
                    abstractPerformanceProfileProbe = s_performance.mainLoopDealWithNetData.profile();
                    try {
                        GameServer.mainLoopDealWithNetData((ZomboidNetData)data);
                        continue;
                    }
                    finally {
                        if (abstractPerformanceProfileProbe != null) {
                            abstractPerformanceProfileProbe.close();
                        }
                    }
                }
                MainLoopNetData2.clear();
                data = MainLoopNetDataQ.poll();
                while (data != null) {
                    MainLoopNetData2.add(data);
                    data = MainLoopNetDataQ.poll();
                }
                for (int n = 0; n < MainLoopNetData2.size(); ++n) {
                    if (n % 10 == 0 && (System.nanoTime() - startServerCycle) / 1000000L > 70L) {
                        if (droppedPackets == 0) {
                            String message = "Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.";
                            DebugLog.log("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                            ChatServer.getInstance().sendMessageToAdminChat("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                            EventManager.instance().report("Server is too busy. Server will drop updates of vehicle's physics. Server is closed for new connections.");
                        }
                        droppedPackets += 2;
                        countOfDroppedPackets += MainLoopNetData2.size() - n;
                        break;
                    }
                    data = MainLoopNetData2.get(n);
                    abstractPerformanceProfileProbe = s_performance.mainLoopDealWithNetData.profile();
                    try {
                        GameServer.mainLoopDealWithNetData((ZomboidNetData)data);
                        continue;
                    }
                    finally {
                        if (abstractPerformanceProfileProbe != null) {
                            abstractPerformanceProfileProbe.close();
                        }
                    }
                }
                MainLoopNetData2.clear();
                if (droppedPackets == 1) {
                    DebugLog.log("Server is working normal. Server will not drop updates of vehicle's physics. The server is open for new connections. Server dropped " + countOfDroppedPackets + " packets and " + countOfDroppedConnections + " connections.");
                    countOfDroppedPackets = 0;
                    countOfDroppedConnections = 0;
                }
                droppedPackets = Math.max(0, Math.min(1000, droppedPackets - 1));
                if (!serverUpdateLimiter.Check()) {
                    long delay = PZMath.clamp((5000000L - System.nanoTime() + startServerCycle) / 1000000L, 0L, 100L);
                    if (delay <= 0L) continue;
                    try {
                        Thread.sleep(delay);
                    }
                    catch (InterruptedException e) {
                        DebugLog.General.printException(e, "", LogSeverity.Error);
                    }
                    continue;
                }
                ++IsoCamera.frameState.frameCount;
                IsoCamera.frameState.updateUnPausedAccumulator();
                try {
                    AbstractPerformanceProfileProbe delay = s_performance.frameStep.profile();
                    try {
                        int n;
                        timeSinceKeepAlive += GameTime.getInstance().getMultiplier();
                        ServerMap.instance.preupdate();
                        data = consoleCommands;
                        synchronized (data) {
                            for (int i = 0; i < consoleCommands.size(); ++i) {
                                String command = consoleCommands.get(i);
                                try {
                                    if (CoopSlave.instance != null && CoopSlave.instance.handleCommand(command)) continue;
                                    System.out.println(GameServer.handleServerCommand(command, null));
                                    continue;
                                }
                                catch (Exception e) {
                                    DebugLog.General.printException(e, "", LogSeverity.Error);
                                }
                            }
                            consoleCommands.clear();
                        }
                        if (removeZombiesConnection != null) {
                            NetworkZombieManager.removeZombies(removeZombiesConnection);
                            removeZombiesConnection = null;
                        }
                        if (removeAnimalsConnection != null) {
                            AnimalInstanceManager.removeAnimals(removeAnimalsConnection);
                            removeAnimalsConnection = null;
                        }
                        if (removeCorpsesConnection != null) {
                            IsoDeadBody.removeDeadBodies(removeCorpsesConnection);
                            removeCorpsesConnection = null;
                        }
                        if (removeVehiclesConnection != null) {
                            for (IsoPlayer player : GameServer.removeVehiclesConnection.players) {
                                if (player == null) continue;
                                VehicleManager.instance.removeVehicles(player);
                            }
                            removeVehiclesConnection = null;
                        }
                        data = s_performance.RCONServerUpdate.profile();
                        try {
                            RCONServer.update();
                        }
                        finally {
                            if (data != null) {
                                ((AbstractPerformanceProfileProbe)data).close();
                            }
                        }
                        try {
                            MapCollisionData.instance.updateGameState();
                            state.update();
                            VehicleManager.instance.serverUpdate();
                            ObjectIDManager.getInstance().checkForSaveDataFile(false);
                        }
                        catch (Exception e) {
                            DebugLog.General.printException(e, "", LogSeverity.Error);
                        }
                        int asleepCount = 0;
                        int playerCount = 0;
                        for (int n3 = 0; n3 < Players.size(); ++n3) {
                            IsoPlayer p = Players.get(n3);
                            if (p.isAlive()) {
                                if (!IsoWorld.instance.currentCell.getObjectList().contains(p)) {
                                    IsoWorld.instance.currentCell.getObjectList().add(p);
                                }
                                ++playerCount;
                                if (p.isAsleep()) {
                                    ++asleepCount;
                                }
                            }
                            ServerMap.instance.characterIn(p);
                        }
                        ImportantAreaManager.getInstance().process();
                        GameServer.setFastForward(ServerOptions.instance.sleepAllowed.getValue() && playerCount > 0 && asleepCount == playerCount);
                        boolean needCalcCountPlayersInRelevantPosition = calcCountPlayersInRelevantPositionLimiter.Check();
                        for (n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                            UdpConnection c = GameServer.udpEngine.connections.get(n);
                            if (needCalcCountPlayersInRelevantPosition) {
                                c.calcCountPlayersInRelevantPosition();
                            }
                            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                                Vector3 area = c.connectArea[playerIndex];
                                if (area != null) {
                                    ServerMap.instance.characterIn(PZMath.fastfloor(area.x), PZMath.fastfloor(area.y), PZMath.fastfloor(area.z));
                                }
                                ClientServerMap.characterIn(c, playerIndex);
                            }
                            if (c.getPlayerDownloadServer() == null) continue;
                            c.getPlayerDownloadServer().update();
                        }
                        for (n = 0; n < IsoWorld.instance.currentCell.getObjectList().size(); ++n) {
                            IsoMovingObject o = IsoWorld.instance.currentCell.getObjectList().get(n);
                            if (o instanceof IsoAnimal || !(o instanceof IsoPlayer) || Players.contains(o)) continue;
                            DebugLog.log("Disconnected player in CurrentCell.getObjectList() removed");
                            IsoWorld.instance.currentCell.getObjectList().remove(n--);
                        }
                        if (++updateDBCount > 150) {
                            for (n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                                UdpConnection connection = GameServer.udpEngine.connections.get(n);
                                try {
                                    if (connection.getUserName() != null || connection.awaitingCoopApprove || LoginQueue.isInTheQueue(connection) || !connection.isConnectionAttemptTimeout() || connection.googleAuth && !connection.isGoogleAuthTimeout()) continue;
                                    GameServer.disconnect(connection, "connection-attempt-timeout");
                                    udpEngine.forceDisconnect(connection.getConnectedGUID(), "connection-attempt-timeout");
                                    continue;
                                }
                                catch (Exception e) {
                                    DebugLog.General.printException(e, "", LogSeverity.Error);
                                }
                            }
                            updateDBCount = 0;
                        }
                        ServerMap.instance.postupdate();
                        try {
                            ServerGUI.update();
                        }
                        catch (Exception e) {
                            DebugLog.General.printException(e, "", LogSeverity.Error);
                        }
                        long serverCycleLast = serverCycle;
                        serverCycle = System.currentTimeMillis();
                        long dif = serverCycle - serverCycleLast;
                        float frames = 1000.0f / (float)dif;
                        if (!Float.isNaN(frames)) {
                            averageFPS = (float)((double)averageFPS + Math.min((double)(frames - averageFPS) * 0.05, 1.0));
                        }
                        GameTime.instance.fpsMultiplier = 60.0f / averageFPS;
                        GameServer.launchCommandHandler();
                        StatisticManager.getInstance().update(dif);
                        if (!SteamUtils.isSteamModeEnabled()) {
                            PublicServerUtil.update();
                            PublicServerUtil.updatePlayerCountIfChanged();
                        }
                        for (int i = 0; i < GameServer.udpEngine.connections.size(); ++i) {
                            UdpConnection connection = GameServer.udpEngine.connections.get(i);
                            connection.getValidator().update();
                            if (connection.chunkObjectState.isEmpty()) continue;
                            int chunksPerWidth = 8;
                            for (int j = 0; j < connection.chunkObjectState.size(); j += 2) {
                                short wy;
                                short wx = connection.chunkObjectState.get(j);
                                if (connection.RelevantTo(wx * 8 + 4, (wy = connection.chunkObjectState.get(j + 1)) * 8 + 4, connection.getChunkGridWidth() * 4 * 8)) continue;
                                connection.chunkObjectState.remove(j, 2);
                                j -= 2;
                            }
                        }
                        if (sendWorldMapPlayerPositionLimiter.Check()) {
                            try {
                                GameServer.sendWorldMapPlayerPosition();
                            }
                            catch (Exception ex) {
                                boolean bl = true;
                            }
                        }
                        if (CoopSlave.instance != null) {
                            CoopSlave.instance.update();
                            if (CoopSlave.instance.masterLost()) {
                                DebugLog.log("Coop master is not responding, terminating");
                                ServerMap.instance.QueueQuit();
                            }
                        }
                        LoginQueue.update();
                        ZipBackup.onPeriod();
                        SteamUtils.runLoop();
                        TradingManager.getInstance().update();
                        WarManager.update();
                        NetworkPlayerManager.getInstance().update();
                        GameWindow.fileSystem.updateAsyncTransactions();
                    }
                    finally {
                        if (delay == null) continue;
                        delay.close();
                    }
                }
                catch (Exception ex) {
                    if (mainCycleExceptionLogCount-- <= 0) continue;
                    DebugLog.Multiplayer.printException(ex, "Server processing error", LogSeverity.Error);
                }
            }
            catch (Exception ex) {
                if (mainCycleExceptionLogCount-- <= 0) continue;
                DebugLog.Multiplayer.printException(ex, "Server error", LogSeverity.Error);
            }
        }
        System.exit(0);
    }

    public static void setupSteamGameServer() {
        String[] modIDs;
        SteamGameServer.SetServerName(ServerOptions.instance.publicName.getValue());
        SteamGameServer.SetKeyValue("description", ServerOptions.instance.publicDescription.getValue());
        SteamGameServer.SetKeyValue("version", Core.getInstance().getVersionNumber());
        SteamGameServer.SetKeyValue("open", ServerOptions.instance.open.getValue() ? "1" : "0");
        SteamGameServer.SetKeyValue("public", ServerOptions.instance.isPublic.getValue() ? "1" : "0");
        SteamGameServer.SetKeyValue("pvp", ServerOptions.instance.pvp.getValue() ? "1" : "0");
        Object tags = ServerOptions.instance.isPublic.getValue() ? "" : "hidden";
        tags = (String)tags + (CoopSlave.instance != null ? ";hosted" : "");
        tags = (String)tags + (ServerOptions.instance.mods.getValue().isEmpty() ? ";vanilla" : ";modded");
        tags = (String)tags + (!ServerOptions.instance.open.getValue() ? ";closed" : "");
        tags = (String)tags + (ServerOptions.instance.pvp.getValue() ? ";pvp" : "");
        tags = (String)tags + ";VERSION:" + Core.getInstance().getVersionNumber();
        SteamGameServer.SetGameTags((String)tags);
        String modsString = ServerOptions.instance.mods.getValue();
        int totalMods = 0;
        for (String modID : modIDs = modsString.split(";")) {
            if (StringUtils.isNullOrWhitespace(modID)) continue;
            ++totalMods;
        }
        if (modsString.length() > 128) {
            String[] ss;
            StringBuilder sb = new StringBuilder();
            for (String modID : ss = modsString.split(";")) {
                if (sb.length() + 1 + modID.length() > 128) break;
                if (!sb.isEmpty()) {
                    sb.append(';');
                }
                sb.append(modID);
            }
            modsString = sb.toString();
        }
        SteamGameServer.SetKeyValue("mods", modsString);
        SteamGameServer.SetKeyValue("modCount", String.valueOf(totalMods));
    }

    public static Server steamGetInternetServerDetails(GameServerDetails steamServer) {
        if (steamServer == null) {
            return null;
        }
        Server newServer = new Server();
        newServer.setName(steamServer.name);
        newServer.setDescription("");
        newServer.setSteamId(Long.toString(steamServer.steamId));
        newServer.setPing(Integer.toString(steamServer.ping));
        newServer.setPlayers(Integer.toString(steamServer.numPlayers));
        newServer.setMaxPlayers(Integer.toString(steamServer.maxPlayers));
        newServer.setOpen(!steamServer.tags.contains("closed"));
        newServer.setPublic(!steamServer.tags.contains("hidden"));
        newServer.setIp(steamServer.address);
        newServer.setPort(steamServer.port);
        newServer.setMods(steamServer.tags.contains("modded") ? "+" : "");
        if (!steamServer.tags.contains("VERSION:") && !steamServer.tags.replace("hidden", "").replace("hosted", "").replace(";", "").isEmpty()) {
            newServer.setMods(steamServer.tags.replace(";hosted", "").replace("hidden", ""));
        }
        newServer.setHosted(steamServer.tags.contains("hosted"));
        newServer.setVersion("");
        int versionIndex = steamServer.tags.indexOf("VERSION:");
        if (versionIndex != -1) {
            newServer.setVersion(steamServer.tags.substring(versionIndex + "VERSION:".length()));
        }
        newServer.setLastUpdate(1);
        newServer.setPasswordProtected(steamServer.passwordProtected);
        newServer.setMapName(steamServer.map);
        return newServer;
    }

    private static void launchCommandHandler() {
        if (launched) {
            return;
        }
        launched = true;
        new Thread(ThreadGroups.Workers, () -> {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    if (input.ready()) {
                        String command = input.readLine();
                        if (command == null) break;
                        if (command.isEmpty()) continue;
                        System.out.println("command entered via server console (System.in): \"" + command + "\"");
                        ArrayList<String> arrayList = consoleCommands;
                        synchronized (arrayList) {
                            consoleCommands.add(command);
                        }
                    }
                    Thread.sleep(100L);
                }
                consoleCommands.add("process-status@eof");
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }, "command handler").start();
    }

    public static String rcon(String command) {
        try {
            return GameServer.handleServerCommand(command, null);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static String handleServerCommand(String input, UdpConnection connection) {
        Class<?> cls;
        if (input == null) {
            return null;
        }
        String adminUsername = "admin";
        Role accessLevel = Roles.getDefaultForAdmin();
        if (connection != null) {
            adminUsername = connection.getUserName();
            if (!connection.isCoopHost) {
                accessLevel = connection.getRole();
            }
        }
        if ((cls = CommandBase.findCommandCls(input)) != null) {
            Constructor<?> constructor = cls.getConstructors()[0];
            try {
                CommandBase com = (CommandBase)constructor.newInstance(adminUsername, accessLevel, input, connection);
                return com.Execute();
            }
            catch (InvocationTargetException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                return "A InvocationTargetException error occured";
            }
            catch (IllegalAccessException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                return "A IllegalAccessException error occured";
            }
            catch (InstantiationException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                return "A InstantiationException error occured";
            }
            catch (SQLException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                return "A SQL error occured";
            }
        }
        return "Unknown command " + input;
    }

    public static void sendTeleport(IsoPlayer player, float x, float y, float z) {
        if (player != null) {
            UdpConnection playerConnection = GameServer.getConnectionFromPlayer(player);
            INetworkPacket.send(playerConnection, PacketTypes.PacketType.Teleport, player, Float.valueOf(x), Float.valueOf(y), Float.valueOf(z));
            if (player.getNetworkCharacterAI() != null) {
                player.getNetworkCharacterAI().resetSpeedLimiter();
            }
            AntiCheatNoClip.teleport(player);
            player.getNetworkCharacterAI().resetState();
            for (IsoPlayer other : IDToPlayerMap.values()) {
                UdpConnection otherConnection;
                if (other.getOnlineID() == player.getOnlineID() || !other.isAlive() || (otherConnection = GameServer.getConnectionFromPlayer(other)) == null || playerConnection == null || !otherConnection.isRelevantTo(x, y) || otherConnection.isRelevantTo(player.getX(), player.getY())) continue;
                other.getNetworkCharacterAI().getState().sync(playerConnection);
                INetworkPacket.send(otherConnection, PacketTypes.PacketType.PlayerInjuries, player);
            }
            INetworkPacket.send(playerConnection, PacketTypes.PacketType.PlayerInjuries, player);
        }
    }

    public static void sendPlayerExtraInfo(IsoPlayer p, UdpConnection connection) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.ExtraInfo, p, false);
    }

    public static void sendPlayerExtraInfo(IsoPlayer p, UdpConnection connection, boolean isForced) {
        INetworkPacket.sendToAll(PacketTypes.PacketType.ExtraInfo, p, isForced);
    }

    public static boolean canModifyPlayerStats(UdpConnection c, IsoPlayer player) {
        return c.getRole().hasCapability(Capability.CanModifyPlayerStatsInThePlayerStatsUI) || c.havePlayer(player);
    }

    static void receiveChangePlayerStats(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short id = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(id);
        if (player == null) {
            return;
        }
        String adminUserName = bb.getUTF();
        player.setPlayerStats(bb, adminUserName);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) continue;
            if (c.getConnectedGUID() == PlayerToAddressMap.get(player).longValue()) {
                c.setAllChatMuted(player.isAllChatMuted());
                c.setRole(player.role);
            }
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.ChangePlayerStats.doPacket(b);
            player.createPlayerStats(b, adminUserName);
            PacketTypes.PacketType.ChangePlayerStats.send(c);
        }
    }

    public static void doMinimumInit() throws IOException {
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        DebugFileWatcher.instance.init();
        ArrayList<String> mods = new ArrayList<String>(ServerMods);
        ZomboidFileSystem.instance.loadMods(mods);
        LuaManager.init();
        PerkFactory.init();
        CustomPerks.instance.init();
        CustomPerks.instance.initLua();
        if (guiCommandline && !softReset) {
            ServerGUI.init();
        }
        AssetManagers assetManagers = GameWindow.assetManagers;
        AiSceneAssetManager.instance.create(AiSceneAsset.ASSET_TYPE, assetManagers);
        AnimatedTextureIDAssetManager.instance.create(AnimatedTextureID.ASSET_TYPE, assetManagers);
        AnimationAssetManager.instance.create(AnimationAsset.ASSET_TYPE, assetManagers);
        AnimNodeAssetManager.instance.create(AnimationAsset.ASSET_TYPE, assetManagers);
        ClothingItemAssetManager.instance.create(ClothingItem.ASSET_TYPE, assetManagers);
        MeshAssetManager.instance.create(ModelMesh.ASSET_TYPE, assetManagers);
        ModelAssetManager.instance.create(Model.ASSET_TYPE, assetManagers);
        NinePatchTextureAssetManager.instance.create(NinePatchTexture.ASSET_TYPE, assetManagers);
        PhysicsShapeAssetManager.instance.create(PhysicsShape.ASSET_TYPE, assetManagers);
        TextureIDAssetManager.instance.create(TextureID.ASSET_TYPE, assetManagers);
        TextureAssetManager.instance.create(Texture.ASSET_TYPE, assetManagers);
        if (guiCommandline && !softReset) {
            TileDepthTextureManager.getInstance().init();
            TileDepthMapManager.instance.init();
            TileSeamManager.instance.init();
            GameWindow.initFonts();
        }
        CustomSandboxOptions.instance.init();
        CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
        ModRegistries.init();
        ScriptManager.instance.Load();
        CustomizationManager.getInstance().load();
        ClothingDecals.init();
        BeardStyles.init();
        HairStyles.init();
        OutfitManager.init();
        VoiceStyles.init();
        JAssImpImporter.Init();
        ModelManager.noOpenGL = !ServerGUI.isCreated();
        ModelManager.instance.create();
        System.out.println("LOADING ASSETS: START");
        CoopSlave.status("UI_ServerStatus_Loading_Assets");
        while (GameWindow.fileSystem.hasWork()) {
            GameWindow.fileSystem.updateAsyncTransactions();
        }
        System.out.println("LOADING ASSETS: FINISH");
        CoopSlave.status("UI_ServerStatus_Initing_Checksum");
        try {
            LuaManager.initChecksum();
            LuaManager.LoadDirBase("shared");
            LuaManager.LoadDirBase("client", true);
            LuaManager.LoadDirBase("server");
            LuaManager.finishChecksum();
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
        ScriptManager.instance.LoadedAfterLua();
        CoopSlave.status("UI_ServerStatus_Loading_Sandbox_Vars");
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "Server" + File.separator + serverName + "_SandboxVars.lua");
        if (file.exists()) {
            if (!SandboxOptions.instance.loadServerLuaFile(serverName)) {
                System.out.println("Exiting due to errors loading " + file.getCanonicalPath());
                System.exit(1);
            }
            SandboxOptions.instance.handleOldServerZombiesFile();
            SandboxOptions.instance.saveServerLuaFile(serverName);
            SandboxOptions.instance.toLua();
        } else {
            SandboxOptions.instance.handleOldServerZombiesFile();
            SandboxOptions.instance.saveServerLuaFile(serverName);
            SandboxOptions.instance.toLua();
        }
        WordsFilter.getInstance().loadWords(ServerOptions.instance.badWordListFile.getValue(), ServerOptions.instance.goodWordListFile.getValue());
        LuaEventManager.triggerEvent("OnGameBoot");
        ZomboidGlobals.Load();
        SpawnPoints.instance.initServer1();
        ServerGUI.init2();
        StatisticManager.getInstance().init();
    }

    public static void startServer() throws ConnectException {
        String serverPassword = ServerOptions.instance.password.getValue();
        if (CoopSlave.instance != null && SteamUtils.isSteamModeEnabled()) {
            serverPassword = "";
        }
        udpEngine = new UdpEngine(defaultPort, udpPort, 101, serverPassword, true);
        DebugLog.log(DebugType.Network, "*** SERVER STARTED ****");
        DebugLog.log(DebugType.Network, "*** Steam is " + (SteamUtils.isSteamModeEnabled() ? "enabled" : "not enabled"));
        if (SteamUtils.isSteamModeEnabled()) {
            DebugLog.DetailedInfo.trace("Server is listening on port " + defaultPort + " (for Steam connection) and port " + udpPort + " (for UDPRakNet connection)");
            DebugLog.DetailedInfo.trace("Clients should use " + defaultPort + " port for connections");
        } else {
            DebugLog.DetailedInfo.trace("server is listening on port " + defaultPort);
        }
        resetId = ServerOptions.instance.resetId.getValue();
        if (CoopSlave.instance != null) {
            if (SteamUtils.isSteamModeEnabled()) {
                RakNetPeerInterface peer = udpEngine.getPeer();
                CoopSlave.instance.sendMessage("server-address", null, peer.GetServerIP() + ":" + defaultPort);
                long serverSteamID = SteamGameServer.GetSteamID();
                CoopSlave.instance.sendMessage("steam-id", null, SteamUtils.convertSteamIDToString(serverSteamID));
            } else {
                String serverAddress = "127.0.0.1";
                CoopSlave.instance.sendMessage("server-address", null, "127.0.0.1:" + defaultPort);
            }
        }
        LuaEventManager.triggerEvent("OnServerStarted");
        if (SteamUtils.isSteamModeEnabled()) {
            CoopSlave.status("UI_ServerStatus_Started");
        } else {
            CoopSlave.status("UI_ServerStatus_Started");
        }
        boolean discordEnable = ServerOptions.instance.discordEnable.getValue();
        String discordToken = ServerOptions.instance.discordToken.getValue();
        String discordChatChannel = ServerOptions.instance.discordChatChannel.getValue();
        String discordLogChannel = ServerOptions.instance.discordLogChannel.getValue();
        String discordCommandChannel = ServerOptions.instance.discordCommandChannel.getValue();
        discordBot.connect(discordEnable, discordToken, discordChatChannel, discordLogChannel, discordCommandChannel);
        EventManager.instance().registerCallback(discordBot);
        EventManager.instance().report("Server connected");
        String webhookAddress = ServerOptions.instance.webhookAddress.getValue();
        if (!webhookAddress.isEmpty()) {
            StackBot stackBot = new StackBot(webhookAddress);
            EventManager.instance().registerCallback(stackBot);
        }
    }

    private static void mainLoopDealWithNetData(ZomboidNetData d) {
        if (!SystemDisabler.getDoMainLoopDealWithNetData()) {
            return;
        }
        UdpConnection connection = udpEngine.getActiveConnection(d.connection);
        if (d.type == null) {
            ZomboidNetDataPool.instance.discard(d);
            return;
        }
        try {
            if (connection == null) {
                DebugLog.log(DebugType.Network, "Received packet type=" + d.type.name() + " connection is null.");
                return;
            }
            if (connection.getUserName() == null) {
                switch (d.type) {
                    case Login: 
                    case Ping: 
                    case ScoreboardUpdate: 
                    case GoogleAuth: 
                    case GoogleAuthKey: 
                    case ServerCustomization: {
                        break;
                    }
                    default: {
                        DebugLog.log("Received packet type=" + d.type.name() + " before Login, disconnecting " + connection.getInetSocketAddress().getHostString());
                        connection.forceDisconnect("unacceptable-packet");
                        ZomboidNetDataPool.instance.discard(d);
                        return;
                    }
                }
            }
            d.type.onServerPacket(d.buffer, connection);
        }
        catch (Exception e) {
            if (connection == null) {
                DebugLog.log(DebugType.Network, "Error with packet of type: " + String.valueOf((Object)d.type) + " connection is null.");
            } else {
                DebugLog.General.error("Error with packet of type: " + String.valueOf((Object)d.type) + " for " + connection.getConnectedGUID());
                AntiCheat.PacketException.act(connection, d.type.name());
            }
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
        ZomboidNetDataPool.instance.discard(d);
    }

    static void receiveInvMngRemoveItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        InventoryItem item;
        int itemId = bb.getInt();
        short requested = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(requested);
        if (player != null && (item = player.getInventory().getItemWithID(itemId)) != null) {
            player.getInventory().Remove(item);
            GameServer.sendRemoveItemFromContainer(player.getInventory(), item);
        }
    }

    static void receiveInvMngGetItem(ByteBufferReader bb, UdpConnection connection, short packetType) throws IOException {
        short caller = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(caller);
        if (player == null) {
            return;
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || c.getConnectedGUID() != PlayerToAddressMap.get(player).longValue()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.InvMngGetItem.doPacket(b);
            bb.rewind();
            b.put(bb);
            PacketTypes.PacketType.InvMngGetItem.send(c);
            break;
        }
    }

    static void receiveInvMngReqItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        Object item;
        int itemId = 0;
        String type = null;
        if (bb.getBoolean()) {
            type = bb.getUTF();
        } else {
            itemId = bb.getInt();
        }
        short caller = bb.getShort();
        short requested = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(requested);
        if (player == null) {
            return;
        }
        IsoPlayer callerPlayer = IDToPlayerMap.get(caller);
        if (callerPlayer == null) {
            return;
        }
        if (type == null) {
            item = player.getInventory().getItemWithIDRecursiv(itemId);
            if (item == null) {
                return;
            }
        } else {
            item = InventoryItemFactory.CreateItem(type);
        }
        if (item != null) {
            callerPlayer.getInventory().addItem((InventoryItem)item);
            INetworkPacket.send(callerPlayer, PacketTypes.PacketType.AddInventoryItemToContainer, callerPlayer.getInventory(), item);
            if (((InventoryItem)item).getCategory().equals("Clothing")) {
                player.removeWornItem((InventoryItem)item);
            }
            if (item == player.getPrimaryHandItem()) {
                player.setPrimaryHandItem(null);
            } else if (item == player.getSecondaryHandItem()) {
                player.setSecondaryHandItem(null);
            }
            if (type == null) {
                player.getInventory().removeItemWithID(itemId);
                INetworkPacket.send(player, PacketTypes.PacketType.RemoveInventoryItemFromContainer, player.getInventory(), item);
            } else {
                item = player.getInventory().getItemFromType(type.split("\\.")[1]);
                player.getInventory().Remove((InventoryItem)item);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SyncItemDelete, player.getInventory(), item);
            }
        }
    }

    static void receiveInvMngUpdateItem(ByteBufferReader bb, UdpConnection connection, short packetType) {
        InventoryItem itemNew;
        short playerID = bb.getShort();
        IsoPlayer player = IDToPlayerMap.get(playerID);
        if (player == null) {
            return;
        }
        try {
            itemNew = InventoryItem.loadItem(bb.bb, 244);
        }
        catch (IOException e) {
            return;
        }
        InventoryItem itemOld = player.getInventory().getItemWithIDRecursiv(itemNew.getID());
        if (itemOld == null) {
            return;
        }
        ItemContainer container = itemOld.getContainer();
        container.Remove(itemOld);
        container.AddItem(itemNew);
        INetworkPacket.sendToAll(PacketTypes.PacketType.ReplaceInventoryItemInContainer, container, itemOld, itemNew);
    }

    static void receivePlayerStartPMChat(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ChatServer.getInstance().processPlayerStartWhisperChatPacket(bb);
    }

    public static void updateZombieControl(IsoZombie zombie, short value) {
        if (zombie.getOwner() != null) {
            INetworkPacket.send(zombie.getOwner(), PacketTypes.PacketType.ZombieControl, zombie, value);
        }
    }

    static void receiveSandboxOptions(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            SandboxOptions.instance.load(bb.bb);
            SandboxOptions.instance.applySettings();
            SandboxOptions.instance.toLua();
            SandboxOptions.instance.saveServerLuaFile(serverName);
            for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.SandboxOptions.doPacket(b);
                bb.rewind();
                b.put(bb);
                PacketTypes.PacketType.SandboxOptions.send(c);
            }
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
    }

    static void receiveChunkObjectState(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short wy;
        short wx = bb.getShort();
        IsoChunk chunk = ServerMap.instance.getChunk(wx, wy = bb.getShort());
        if (chunk == null) {
            connection.chunkObjectState.add(wx);
            connection.chunkObjectState.add(wy);
        } else {
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.ChunkObjectState.doPacket(b);
            b.putShort(wx);
            b.putShort(wy);
            try {
                if (chunk.saveObjectState(b.bb)) {
                    PacketTypes.PacketType.ChunkObjectState.send(connection);
                } else {
                    connection.cancelPacket();
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
                connection.cancelPacket();
                return;
            }
        }
    }

    static void receiveSyncFaction(ByteBufferReader bb, UdpConnection connection, short packetType) {
        String name = bb.getUTF();
        String owner = bb.getUTF();
        int playersSize = bb.getInt();
        Faction faction = Faction.getFaction(name);
        boolean shouldCreateChat = false;
        if (faction == null) {
            faction = new Faction(name, owner);
            shouldCreateChat = true;
            Faction.getFactions().add(faction);
        }
        faction.getPlayers().clear();
        if (bb.getBoolean()) {
            faction.setTag(bb.getUTF());
            faction.setTagColor(new ColorInfo(bb.getFloat(), bb.getFloat(), bb.getFloat(), 1.0f));
        }
        for (int i = 0; i < playersSize; ++i) {
            String playerName = bb.getUTF();
            faction.getPlayers().add(playerName);
        }
        if (!faction.getOwner().equals(owner)) {
            faction.setOwner(owner);
        }
        boolean remove = bb.getBoolean();
        if (ChatServer.isInited()) {
            if (shouldCreateChat) {
                ChatServer.getInstance().createFactionChat(name);
            }
            if (remove) {
                ChatServer.getInstance().removeFactionChat(name);
            } else {
                ChatServer.getInstance().syncFactionChatMembers(name, owner, faction.getPlayers());
            }
        }
        if (remove) {
            Faction.getFactions().remove(faction);
            if (server || LuaManager.GlobalObject.isAdmin()) {
                DebugLog.log("faction: removed " + name + " owner=" + faction.getOwner());
            }
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (connection != null && c.getConnectedGUID() == connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SyncFaction.doPacket(b);
            faction.writeToBuffer(b, remove);
            PacketTypes.PacketType.SyncFaction.send(c);
        }
    }

    public static void sendNonPvpZone(NonPvpZone zone, boolean remove, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (connection != null && c.getConnectedGUID() == connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SyncNonPvpZone.doPacket(b);
            zone.save(b.bb);
            b.putBoolean(remove);
            PacketTypes.PacketType.SyncNonPvpZone.send(c);
        }
    }

    static void receiveChangeTextColor(ByteBufferReader bb, UdpConnection connection, short packetType) {
        short playerIndex = bb.getShort();
        IsoPlayer player = GameServer.getPlayerFromConnection(connection, playerIndex);
        if (player == null) {
            return;
        }
        float r = bb.getFloat();
        float g = bb.getFloat();
        float b = bb.getFloat();
        player.setSpeakColourInfo(new ColorInfo(r, g, b, 1.0f));
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.ChangeTextColor.doPacket(b2);
            b2.putShort(player.getOnlineID());
            b2.putFloat(r);
            b2.putFloat(g);
            b2.putFloat(b);
            PacketTypes.PacketType.ChangeTextColor.send(c);
        }
    }

    static void receiveSyncCompost(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        String spriteName = bb.getUTF();
        if (sq != null) {
            IsoCompost compost = sq.getCompost();
            if (compost == null) {
                assert (compost != null);
                compost = new IsoCompost(sq.getCell(), sq, spriteName);
                sq.AddSpecialObject(compost);
            }
            float compostValue = bb.getFloat();
            compost.setCompost(compostValue);
            GameServer.sendCompost(compost, connection);
        }
    }

    public static void sendCompost(IsoCompost compost, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isRelevantTo(compost.square.x, compost.square.y) || (connection == null || c.getConnectedGUID() == connection.getConnectedGUID()) && connection != null) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SyncCompost.doPacket(b);
            b.putInt(compost.square.x);
            b.putInt(compost.square.y);
            b.putInt(compost.square.z);
            b.putUTF(compost.getSpriteName());
            b.putFloat(compost.getCompost());
            PacketTypes.PacketType.SyncCompost.send(c);
        }
    }

    public static void sendHelicopter(float x, float y, boolean active) {
        HelicopterPacket packet = new HelicopterPacket();
        packet.set(x, y, active);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            ByteBufferWriter bb = c.startPacket();
            PacketTypes.PacketType.Helicopter.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.Helicopter.send(c);
        }
    }

    public static void open() {
        closed = false;
        String message = "Server was opened for all";
        DebugLog.General.println("Server was opened for all");
        ChatServer.getInstance().sendMessageToAdminChat("Server was opened for all");
        EventManager.instance().report("[SERVER] Server was opened for all");
    }

    public static void close() {
        closed = true;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            c.forceDisconnect("server-closed");
        }
        String message = "Server was closed for all";
        DebugLog.General.println("Server was closed for all");
        ChatServer.getInstance().sendMessageToAdminChat("Server was closed for all");
        EventManager.instance().report("[SERVER] Server was closed for all");
    }

    public static void sendZone(Zone zone) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.RegisterZone.doPacket(b);
            b.putUTF(zone.name);
            b.putUTF(zone.type);
            b.putInt(zone.x);
            b.putInt(zone.y);
            b.putInt(zone.z);
            b.putInt(zone.w);
            b.putInt(zone.h);
            b.putInt(zone.lastActionTimestamp);
            PacketTypes.PacketType.RegisterZone.send(c);
        }
    }

    static void receiveConstructedZone(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int z;
        int y;
        int x = bb.getInt();
        Zone zone = IsoWorld.instance.metaGrid.getZoneAt(x, y = bb.getInt(), z = bb.getInt());
        if (zone != null) {
            zone.setHaveConstruction(true);
        }
    }

    public static void addXp(IsoPlayer p, PerkFactory.Perk perk, float xp) {
        GameServer.addXp(p, perk, xp, false, false);
    }

    public static void addXp(IsoPlayer p, PerkFactory.Perk perk, float xp, boolean noMultiplier) {
        GameServer.addXp(p, perk, xp, noMultiplier, false);
    }

    public static void addXp(IsoPlayer p, PerkFactory.Perk perk, float xp, boolean noMultiplier, boolean showXP) {
        UdpConnection c = GameServer.getConnectionFromPlayer(p);
        if (c != null) {
            AddXpPacket.addXp(c, p, perk, xp, noMultiplier, showXP);
        }
    }

    public static void addXpMultiplier(IsoPlayer p, PerkFactory.Perk perk, float multiplier, int minLevel, int maxLevel) {
        p.getXp().addXpMultiplier(perk, multiplier, minLevel, maxLevel);
        INetworkPacket.send(p, PacketTypes.PacketType.AddXPMultiplier, p, perk, Float.valueOf(multiplier), minLevel, maxLevel);
    }

    private static void answerPing(ByteBufferReader bb, UdpConnection connection) {
        String ip = bb.getUTF();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.Ping.doPacket(b);
            b.putUTF(ip);
            b.putInt(GameServer.udpEngine.connections.size());
            b.putInt(512);
            PacketTypes.PacketType.Ping.send(c);
        }
    }

    static void receiveUpdateItemSprite(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int bbbb = bb.getInt();
        String spriteName = bb.getUTF();
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        int index = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq != null && index < sq.getObjects().size()) {
            try {
                IsoObject o = sq.getObjects().get(index);
                if (o != null) {
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
                    o.transmitUpdatedSpriteToClients(connection);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static void sendOptionsToClients() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            c.getValidator().resetCounters();
            c.getValidator().resetTimers();
            INetworkPacket.send(c, PacketTypes.PacketType.ReloadOptions, new Object[0]);
        }
    }

    public static void sendCorpse(IsoDeadBody body) {
        IsoGridSquare sq = body.getSquare();
        if (sq != null) {
            AddCorpseToMapPacket packet = new AddCorpseToMapPacket();
            packet.set(sq, body);
            for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (!c.isRelevantTo(sq.x, sq.y)) continue;
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.AddCorpseToMap.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.AddCorpseToMap.send(c);
            }
        }
    }

    static void receiveChatMessageFromPlayer(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ChatServer.getInstance().processMessageFromPlayerPacket(bb, connection);
    }

    public static void loadModData(IsoGridSquare sq) {
        if (sq.getModData().rawget("id") != null && sq.getModData().rawget("id") != null && (sq.getModData().rawget("remove") == null || sq.getModData().rawget("remove").equals("false"))) {
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":x", (Object)sq.getX());
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":y", (Object)sq.getY());
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":z", (Object)sq.getZ());
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":typeOfSeed", sq.getModData().rawget("typeOfSeed"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":nbOfGrow", sq.getModData().rawget("nbOfGrow"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":id", sq.getModData().rawget("id"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterLvl", sq.getModData().rawget("waterLvl"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":lastWaterHour", sq.getModData().rawget("lastWaterHour"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterNeeded", sq.getModData().rawget("waterNeeded"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":waterNeededMax", sq.getModData().rawget("waterNeededMax"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":mildewLvl", sq.getModData().rawget("mildewLvl"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":aphidLvl", sq.getModData().rawget("aphidLvl"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":fliesLvl", sq.getModData().rawget("fliesLvl"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":fertilizer", sq.getModData().rawget("fertilizer"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":nextGrowing", sq.getModData().rawget("nextGrowing"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":hasVegetable", sq.getModData().rawget("hasVegetable"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":hasSeed", sq.getModData().rawget("hasSeed"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":health", sq.getModData().rawget("health"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":badCare", sq.getModData().rawget("badCare"));
            GameTime.getInstance().getModData().rawset("planting:" + ((Double)sq.getModData().rawget("id")).intValue() + ":state", sq.getModData().rawget("state"));
            if (sq.getModData().rawget("hoursElapsed") != null) {
                GameTime.getInstance().getModData().rawset("hoursElapsed", sq.getModData().rawget("hoursElapsed"));
            }
        }
        ReceiveModDataPacket packet = new ReceiveModDataPacket();
        packet.set(sq);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isRelevantTo(sq.getX(), sq.getY())) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.ReceiveModData.doPacket(b2);
            packet.write(b2);
            PacketTypes.PacketType.ReceiveModData.send(c);
        }
    }

    static void receiveDrink(ByteBufferReader bb, UdpConnection connection, short packetType) {
        byte playerIndex = bb.getByte();
        float am = bb.getFloat();
        IsoPlayer pl = GameServer.getPlayerFromConnection(connection, playerIndex);
        if (pl != null) {
            pl.getStats().remove(CharacterStat.THIRST, am);
        }
    }

    static void receiveReceiveCommand(ByteBufferReader bb, UdpConnection connection, short packetType) {
        String chat = bb.getUTF();
        Object message = GameServer.handleClientCommand(chat.substring(1), connection);
        if (message == null) {
            message = GameServer.handleServerCommand(chat.substring(1), connection);
        }
        if (message == null) {
            message = "Unknown command " + chat;
        }
        if (chat.substring(1).startsWith("roll") || chat.substring(1).startsWith("card")) {
            ChatServer.getInstance().sendMessageToServerChat(connection, (String)message);
        } else {
            ChatServer.getInstance().sendMessageToServerChat(connection, (String)message);
        }
    }

    private static String handleClientCommand(String input, UdpConnection connection) {
        String command;
        if (input == null) {
            return null;
        }
        ArrayList<String> args1 = new ArrayList<String>();
        Matcher m1 = Pattern.compile("([^\"]\\S*|\".*?\")\\s*").matcher(input);
        while (m1.find()) {
            args1.add(m1.group(1).replace("\"", ""));
        }
        int argc = args1.size();
        String[] argv = args1.toArray(new String[argc]);
        String string = command = argc > 0 ? argv[0].toLowerCase() : "";
        if (command.equals("card")) {
            GameServer.PlayWorldSoundServer("ChatDrawCard", false, GameServer.getAnyPlayerFromConnection(connection).getCurrentSquare(), 0.0f, 3.0f, 1.0f, false);
            return connection.getUserName() + " drew " + ServerOptions.getRandomCard();
        }
        if (command.equals("roll")) {
            if (argc != 2) {
                return ServerOptions.clientOptionsList.get("roll");
            }
            try {
                int number = Integer.parseInt(argv[1]);
                GameServer.PlayWorldSoundServer("ChatRollDice", false, GameServer.getAnyPlayerFromConnection(connection).getCurrentSquare(), 0.0f, 3.0f, 1.0f, false);
                return connection.getUserName() + " rolls a " + number + "-sided dice and obtains " + Rand.Next(number);
            }
            catch (Exception e) {
                return ServerOptions.clientOptionsList.get("roll");
            }
        }
        if (command.equals("changepwd")) {
            if (argc == 3) {
                String previousPass = argv[1];
                String newPass = argv[2];
                try {
                    return ServerWorldDatabase.instance.changePwd(connection.getUserName(), previousPass.trim(), newPass.trim());
                }
                catch (SQLException e) {
                    DebugLog.General.printException(e, "A SQL error occured", LogSeverity.Error);
                    return "A SQL error occured";
                }
            }
            return ServerOptions.clientOptionsList.get("changepwd");
        }
        if (command.equals("dragons")) {
            return "Sorry, you don't have the required materials.";
        }
        if (command.equals("dance")) {
            return "Stop kidding me...";
        }
        if (command.equals("safehouse")) {
            if (argc != 2 || connection == null) {
                return ServerOptions.clientOptionsList.get("safehouse");
            }
            if (!ServerOptions.instance.playerSafehouse.getValue() && !ServerOptions.instance.adminSafehouse.getValue()) {
                return "Safehouses are disabled on this server.";
            }
            if ("release".equals(argv[1])) {
                SafeHouse safeHouse = SafeHouse.hasSafehouse(connection.getUserName());
                if (safeHouse == null) {
                    return "You don't have a safehouse.";
                }
                if (!safeHouse.isOwner(connection.getUserName())) {
                    return "Only owner can release safehouse";
                }
                if (!ServerOptions.instance.playerSafehouse.getValue() && !connection.getRole().hasCapability(Capability.CanSetupSafehouses)) {
                    return "Only admin or moderator may release safehouses";
                }
                SafeHouse.removeSafeHouse(safeHouse);
                return "Safehouse released";
            }
            return ServerOptions.clientOptionsList.get("safehouse");
        }
        return null;
    }

    private static void PlayWorldSound(String name, IsoGridSquare source2, float radius, int index) {
        if (!server || source2 == null) {
            return;
        }
        int x = source2.getX();
        int y = source2.getY();
        int z = source2.getZ();
        PlayWorldSoundPacket packet = new PlayWorldSoundPacket();
        packet.set(name, x, y, (byte)z, index);
        DebugLog.log(DebugType.Sound, "sending " + packet.getDescription() + " radius=" + radius);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
            if (p == null || !c.RelevantTo(x, y, radius * 2.0f)) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.PlayWorldSound.doPacket(b2);
            packet.write(b2);
            PacketTypes.PacketType.PlayWorldSound.send(c);
        }
    }

    public static void PlayWorldSoundServer(String name, IsoGridSquare source2, float radius, int index) {
        GameServer.PlayWorldSound(name, source2, radius, index);
    }

    public static void PlayWorldSoundServer(String name, boolean loop, IsoGridSquare source2, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        GameServer.PlayWorldSound(name, source2, radius, -1);
    }

    public static void PlayWorldSoundServer(IsoGameCharacter character, String name, boolean loop, IsoGridSquare source2, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        if (character != null && character.isInvisible() && !DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            return;
        }
        GameServer.PlayWorldSound(name, source2, radius, -1);
    }

    public static void PlayWorldSoundWavServer(String name, boolean loop, IsoGridSquare source2, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        GameServer.PlayWorldSound(name, source2, radius, -1);
    }

    public static void PlaySoundAtEveryPlayer(String name, int x, int y, int z) {
        GameServer.PlaySoundAtEveryPlayer(name, x, y, z, false);
    }

    public static void PlaySoundAtEveryPlayer(String name) {
        GameServer.PlaySoundAtEveryPlayer(name, -1, -1, -1, true);
    }

    public static void PlaySoundAtEveryPlayer(String name, int x, int y, int z, boolean usePlrCoords) {
        if (!server) {
            return;
        }
        if (usePlrCoords) {
            DebugLog.log(DebugType.Sound, "sound: sending " + name + " at every player (using player location)");
        } else {
            DebugLog.log(DebugType.Sound, "sound: sending " + name + " at every player location x=" + x + " y=" + y);
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
            if (p == null || p.hasTrait(CharacterTrait.DEAF)) continue;
            if (usePlrCoords) {
                x = PZMath.fastfloor(p.getX());
                y = PZMath.fastfloor(p.getY());
                z = PZMath.fastfloor(p.getZ());
            }
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.PlaySoundEveryPlayer.doPacket(b2);
            b2.putUTF(name);
            b2.putInt(x);
            b2.putInt(y);
            b2.putInt(z);
            PacketTypes.PacketType.PlaySoundEveryPlayer.send(c);
        }
    }

    public static void sendZombieSound(IsoZombie.ZombieSound sound, IsoZombie zombie) {
        float radius = sound.radius();
        DebugLog.log(DebugType.Sound, "sound: sending zombie sound " + String.valueOf((Object)sound));
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isFullyConnected() || !c.RelevantTo(zombie.getX(), zombie.getY(), radius)) continue;
            ByteBufferWriter bb = c.startPacket();
            PacketTypes.PacketType.ZombieSound.doPacket(bb);
            bb.putShort(zombie.onlineId);
            bb.putEnum(sound);
            PacketTypes.PacketType.ZombieSound.send(c);
        }
    }

    public static boolean helmetFall(IsoGameCharacter character, boolean hitHead) {
        InventoryItem item;
        ItemVisuals tempItemVisuals = new ItemVisuals();
        character.getItemVisuals(tempItemVisuals);
        if (!character.isUsingWornItems() && tempItemVisuals.isEmpty()) {
            character.dressInPersistentOutfitID(character.getPersistentOutfitID());
            character.getItemVisuals(tempItemVisuals);
        }
        if ((item = PersistentOutfits.instance.processFallingHat(character, hitHead)) == null) {
            return false;
        }
        float x = character.getX() + 0.6f;
        float y = character.getY() + 0.6f;
        if (PolygonalMap2.instance.lineClearCollide(character.getX(), character.getY(), x, y, character.getZi(), null, false, true)) {
            x = character.getX();
            y = character.getY();
        }
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, character.getZ());
        sq.AddWorldInventoryItem(item, x % 1.0f, y % 1.0f, character.getZ(), false);
        ZombieHelmetFallingPacket packet = new ZombieHelmetFallingPacket();
        packet.set(character, item, x, y, character.getZ());
        for (int i = 0; i < tempItemVisuals.size(); ++i) {
            ItemVisual itemVisual = (ItemVisual)tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (!scriptItem.name.equals(item.getType())) continue;
            tempItemVisuals.remove(i);
            break;
        }
        character.getItemVisuals().clear();
        character.getItemVisuals().addAll(tempItemVisuals);
        ModelManager.instance.ResetNextFrame(character);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isFullyConnected() || !c.isRelevantTo(x, y)) continue;
            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.ZombieHelmetFalling.doPacket(b2);
                packet.write(b2);
                PacketTypes.PacketType.ZombieHelmetFalling.send(c);
                continue;
            }
            catch (Throwable t) {
                c.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
        return true;
    }

    public static void initClientCommandFilter() {
        String[] ss;
        String filter = ServerOptions.getInstance().clientCommandFilter.getValue();
        ccFilters.clear();
        for (String s : ss = filter.split(";")) {
            String[] ss1;
            if (s.isEmpty() || !s.contains(".") || !s.startsWith("+") && !s.startsWith("-") || (ss1 = s.split("\\.")).length != 2) continue;
            String module = ss1[0].substring(1);
            String command = ss1[1];
            CCFilter ccf = new CCFilter();
            ccf.command = command;
            ccf.allow = ss1[0].startsWith("+");
            ccf.next = ccFilters.get(module);
            ccFilters.put(module, ccf);
        }
    }

    static void receiveClientCommand(ByteBufferReader bb, UdpConnection connection, short packetType) {
        byte playerIndex = bb.getByte();
        String module = bb.getUTF();
        String command = bb.getUTF();
        boolean hasArgs = bb.getBoolean();
        KahluaTable tbl = null;
        if (hasArgs) {
            tbl = LuaManager.platform.newTable();
            try {
                TableNetworkUtils.load(tbl, bb);
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                return;
            }
        }
        IsoPlayer player = GameServer.getPlayerFromConnection(connection, playerIndex);
        if (playerIndex == -1) {
            player = GameServer.getAnyPlayerFromConnection(connection);
        }
        if (player == null) {
            DebugLog.log("receiveClientCommand: player is null");
            return;
        }
        CCFilter ccf = ccFilters.get(module);
        if (ccf == null || ccf.passes(command)) {
            LoggerManager.getLogger("cmd").write(connection.getIDStr() + " \"" + player.username + "\" " + module + "." + command + " @ " + player.getXi() + "," + player.getYi() + "," + player.getZi());
        }
        if ("vehicle".equals(module) && "remove".equals(command) && !Core.debug && !connection.getRole().hasCapability(Capability.GeneralCheats) && !player.networkAi.isDismantleAllowed()) {
            return;
        }
        LuaEventManager.triggerEvent("OnClientCommand", module, command, player, tbl);
    }

    static void receiveWorldMap(ByteBufferReader bb, UdpConnection connection, short packetType) throws IOException {
        WorldMapServer.instance.receive(bb, connection);
    }

    public static IsoPlayer getAnyPlayerFromConnection(IConnection connection) {
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            if (connection.getPlayerAt(playerIndex) == null) continue;
            return connection.getPlayerAt(playerIndex);
        }
        return null;
    }

    public static IsoPlayer getPlayerFromConnection(IConnection connection, int playerIndex) {
        if (playerIndex >= 0 && playerIndex < 4) {
            return connection.getPlayerAt(playerIndex);
        }
        return null;
    }

    public static IsoPlayer getPlayerByRealUserName(String username) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = c.players[playerIndex];
                if (player == null || !player.username.equals(username)) continue;
                return player;
            }
        }
        return null;
    }

    public static IsoPlayer getPlayerByUserName(String username) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = c.players[playerIndex];
                if (player == null || !player.getDisplayName().equals(username) && !player.getUsername().equals(username)) continue;
                return player;
            }
        }
        return null;
    }

    public static IsoPlayer getPlayerByUserNameForCommand(String username) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = c.players[playerIndex];
                if (player == null || !player.getUsername().equalsIgnoreCase(username)) continue;
                return player;
            }
        }
        return null;
    }

    public static UdpConnection getConnectionByPlayerOnlineID(short onlineID) {
        return udpEngine.getActiveConnection(IDToAddressMap.get(onlineID));
    }

    public static UdpConnection getConnectionFromPlayer(IsoPlayer player) {
        Long guid = PlayerToAddressMap.get(player);
        if (guid == null) {
            return null;
        }
        return udpEngine.getActiveConnection(guid);
    }

    public static void sendAddItemToContainer(ItemContainer container, InventoryItem item) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, container, item);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.getParent().getX(), (float)((int)container.getParent().getY()), container, item);
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.inventoryContainer.getWorldItem().getX(), (float)((int)container.inventoryContainer.getWorldItem().getY()), container, item);
        }
    }

    public static void sendAddItemsToContainer(ItemContainer container, ArrayList<InventoryItem> items) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.AddInventoryItemToContainer, container, items);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.getParent().getX(), (float)((int)container.getParent().getY()), container, items);
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, (int)container.inventoryContainer.getWorldItem().getX(), (float)((int)container.inventoryContainer.getWorldItem().getY()), container, items);
        }
    }

    public static void sendReplaceItemInContainer(ItemContainer container, InventoryItem oldItem, InventoryItem newItem) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.ReplaceInventoryItemInContainer, container, oldItem, newItem);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ReplaceInventoryItemInContainer, (int)container.getParent().getX(), (float)((int)container.getParent().getY()), container, oldItem, newItem);
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ReplaceInventoryItemInContainer, (int)container.inventoryContainer.getWorldItem().getX(), (float)((int)container.inventoryContainer.getWorldItem().getY()), container, oldItem, newItem);
        }
    }

    public static void sendRemoveItemFromContainer(ItemContainer container, InventoryItem item) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, item);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.getParent().getX(), (float)((int)container.getParent().getY()), container, item);
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.inventoryContainer.getWorldItem().getX(), (float)((int)container.inventoryContainer.getWorldItem().getY()), container, item);
        }
    }

    public static void sendRemoveItemsFromContainer(ItemContainer container, ArrayList<InventoryItem> items) {
        if (container.getCharacter() instanceof IsoPlayer) {
            INetworkPacket.send((IsoPlayer)container.getCharacter(), PacketTypes.PacketType.RemoveInventoryItemFromContainer, container, items);
        } else if (container.getParent() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.getParent().getX(), (float)((int)container.getParent().getY()), container, items);
        } else if (container.inventoryContainer != null && container.inventoryContainer.getWorldItem() != null) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveInventoryItemFromContainer, (int)container.inventoryContainer.getWorldItem().getX(), (float)((int)container.inventoryContainer.getWorldItem().getY()), container, items);
        }
    }

    public static void sendSyncPlayerFields(IsoPlayer player, byte syncParams) {
        if (player == null || player.onlineId == -1) {
            return;
        }
        INetworkPacket.send(player, PacketTypes.PacketType.SyncPlayerFields, player, syncParams);
    }

    public static void sendSyncClothing(IsoPlayer player, ItemBodyLocation location, InventoryItem item) {
        if (player != null && player.onlineId != -1) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.SyncClothing, player.getX(), player.getY(), player);
        }
    }

    public static void syncVisuals(IsoPlayer player) {
        if (player == null || player.onlineId == -1) {
            return;
        }
        SyncVisualsPacket packet = new SyncVisualsPacket();
        packet.set(player);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isRelevantTo(player.getX(), player.getY())) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SyncVisuals.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.SyncVisuals.send(c);
        }
    }

    public static void sendItemsInContainer(IsoObject o, ItemContainer container) {
        if (udpEngine == null) {
            return;
        }
        if (container == null) {
            DebugLog.log("sendItemsInContainer: container is null");
            return;
        }
        if (o instanceof IsoWorldInventoryObject) {
            IsoWorldInventoryObject worldInvObj = (IsoWorldInventoryObject)o;
            InventoryItem inventoryItem = worldInvObj.getItem();
            if (!(inventoryItem instanceof InventoryContainer)) {
                DebugLog.log("sendItemsInContainer: IsoWorldInventoryObject item isn't a container");
                return;
            }
            InventoryContainer invContainer = (InventoryContainer)inventoryItem;
            if (invContainer.getInventory() != container) {
                DebugLog.log("sendItemsInContainer: wrong container for IsoWorldInventoryObject");
                return;
            }
        } else if (o instanceof BaseVehicle) {
            if (container.vehiclePart == null || container.vehiclePart.getItemContainer() != container || container.vehiclePart.getVehicle() != o) {
                DebugLog.log("sendItemsInContainer: wrong container for BaseVehicle");
                return;
            }
        } else if (o instanceof IsoDeadBody) {
            if (container != o.getContainer()) {
                DebugLog.log("sendItemsInContainer: wrong container for IsoDeadBody");
                return;
            }
        } else if (o.getContainerIndex(container) == -1) {
            DebugLog.log("sendItemsInContainer: wrong container for IsoObject");
            return;
        }
        if (o == null || container.getItems().isEmpty()) {
            return;
        }
        INetworkPacket.sendToRelative(PacketTypes.PacketType.AddInventoryItemToContainer, o.square.x, (float)o.square.y, container, container.getItems());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addConnection(UdpConnection con) {
        ConcurrentLinkedQueue<IZomboidPacket> concurrentLinkedQueue = MainLoopNetDataHighPriorityQ;
        synchronized (concurrentLinkedQueue) {
            MainLoopNetDataHighPriorityQ.add(new DelayedConnection(con, true));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addDisconnect(UdpConnection con) {
        ConcurrentLinkedQueue<IZomboidPacket> concurrentLinkedQueue = MainLoopNetDataHighPriorityQ;
        synchronized (concurrentLinkedQueue) {
            MainLoopNetDataHighPriorityQ.add(new DelayedConnection(con, false));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addDelayedDisconnect(UdpConnection con) {
        ConcurrentHashMap<String, DelayedConnection> concurrentHashMap = MainLoopDelayedDisconnectQ;
        synchronized (concurrentHashMap) {
            MainLoopDelayedDisconnectQ.put(con.getUserName(), new DelayedConnection(con, false));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void doDelayedDisconnect(IsoPlayer player) {
        ConcurrentHashMap<String, DelayedConnection> concurrentHashMap = MainLoopDelayedDisconnectQ;
        synchronized (concurrentHashMap) {
            DelayedConnection data = MainLoopDelayedDisconnectQ.remove(player.username);
            if (data != null) {
                data.disconnect();
            }
        }
    }

    public static boolean isDelayedDisconnect(UdpConnection con) {
        if (con != null && con.getUserName() != null) {
            return MainLoopDelayedDisconnectQ.containsKey(con.getUserName());
        }
        return false;
    }

    public static boolean isDelayedDisconnect(IsoPlayer player) {
        if (player != null && player.username != null) {
            return MainLoopDelayedDisconnectQ.containsKey(player.username);
        }
        return false;
    }

    public static void disconnectPlayer(IsoPlayer player, IConnection connection) {
        if (player == null) {
            return;
        }
        SafetySystemManager.storeSafety(player);
        ChatServer.getInstance().disconnectPlayer(player.getOnlineID());
        if (player.getVehicle() != null) {
            int seat;
            VehiclesDB2.instance.updateVehicleAndTrailer(player.getVehicle());
            if (player.getVehicle().isDriver(player) && player.getVehicle().isNetPlayerId(player.getOnlineID())) {
                player.getVehicle().setNetPlayerAuthorization(BaseVehicle.Authorization.Server, -1);
                if (player.getVehicle().getController() != null) {
                    player.getVehicle().getController().clientForce = 0.0f;
                }
                player.getVehicle().jniLinearVelocity.set(0.0f, 0.0f, 0.0f);
            }
            if ((seat = player.getVehicle().getSeat(player)) != -1) {
                player.getVehicle().clearPassenger(seat);
            }
        }
        NetworkZombieManager.getInstance().clearTargetAuth(connection, player);
        player.removeFromWorld();
        player.removeFromSquare();
        PlayerToAddressMap.remove(player);
        IDToAddressMap.remove(player.onlineId);
        IDToPlayerMap.remove(player.onlineId);
        Players.remove(player);
        SafeHouse.updateSafehousePlayersConnected();
        SafeHouse safeHouse = SafeHouse.hasSafehouse(player);
        if (safeHouse != null && safeHouse.isOwner(player)) {
            for (IsoPlayer member : IDToPlayerMap.values()) {
                safeHouse.checkTrespass(member);
            }
        }
        connection.setUserName(player.playerIndex, null);
        connection.setPlayerAt(player.playerIndex, null);
        connection.setPlayerId(player.playerIndex, (short)-1);
        connection.setRelevantPos(player.playerIndex, null);
        connection.setConnectArea(player.playerIndex, null);
        INetworkPacket.sendToAll(PacketTypes.PacketType.PlayerTimeout, player);
        ServerLOS.instance.removePlayer(player);
        ZombiePopulationManager.instance.updateLoadedAreas();
        DebugLog.DetailedInfo.trace("Disconnected player \"" + player.getDisplayName() + "\" " + connection.getConnectedGUID());
        LoggerManager.getLogger("user").write(connection.getIDStr() + " \"" + player.getUsername() + "\" disconnected player " + LoggerManager.getPlayerCoords(player));
        SteamGameServer.RemovePlayer(player);
    }

    public static short getFreeSlot() {
        for (short n = 0; n < udpEngine.getMaxConnections(); n = (short)(n + 1)) {
            if (SlotToConnection[n] != null) continue;
            return n;
        }
        return -1;
    }

    public static void receiveClientConnect(UdpConnection connection, ServerWorldDatabase.LogonResult r) {
        ConnectionManager.log("receive-packet", "client-connect", connection);
        short slot = GameServer.getFreeSlot();
        short playerID = (short)(slot * 4);
        if (connection.getPlayerDownloadServer() != null) {
            try {
                IDToAddressMap.put(playerID, connection.getConnectedGUID());
                connection.getPlayerDownloadServer().destroy();
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
        playerToCoordsMap.put(playerID, new Vector2());
        GameServer.SlotToConnection[slot] = connection;
        connection.playerIds[0] = playerID;
        IDToAddressMap.put(playerID, connection.getConnectedGUID());
        connection.setPlayerDownloadServer(new PlayerDownloadServer(connection));
        DebugLog.log(DebugType.Network, "Connected new client " + connection.getConnectedGUID() + " ID # " + playerID);
        KahluaTable spawnRegions = SpawnPoints.instance.getSpawnRegions();
        for (int i = 1; i < spawnRegions.size() + 1; ++i) {
            ByteBufferWriter b2 = connection.startPacket();
            PacketTypes.PacketType.SpawnRegion.doPacket(b2);
            b2.putInt(i);
            try {
                ((KahluaTable)spawnRegions.rawget(i)).save(b2.bb);
                PacketTypes.PacketType.SpawnRegion.send(connection);
                continue;
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
        RequestDataPacket packet = new RequestDataPacket();
        packet.sendConnectingDetails(connection, r);
    }

    public static void sendMetaGrid(int cellX, int cellY, int roomID, UdpConnection connection) {
        MetaGridPacket packet = new MetaGridPacket();
        if (packet.set(cellX, cellY, roomID)) {
            ByteBufferWriter bb = connection.startPacket();
            PacketTypes.PacketType.MetaGrid.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.MetaGrid.send(connection);
        }
    }

    public static void sendMetaGrid(int cellX, int cellY, int roomID) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendMetaGrid(cellX, cellY, roomID, c);
        }
    }

    private static void preventIndoorZombies(int x, int y, int z) {
        RoomDef room = IsoWorld.instance.metaGrid.getRoomAt(x, y, z);
        if (room == null) {
            return;
        }
        boolean killThemAll = GameServer.isSpawnBuilding(room.getBuilding());
        room.getBuilding().setAllExplored(true);
        room.getBuilding().setAlarmed(false);
        ArrayList<IsoZombie> zombies = IsoWorld.instance.currentCell.getZombieList();
        for (int i = 0; i < zombies.size(); ++i) {
            IsoZombie zombie = zombies.get(i);
            if (!killThemAll && !zombie.indoorZombie || zombie.getSquare() == null || zombie.getSquare().getRoom() == null || zombie.getSquare().getRoom().def.building != room.getBuilding()) continue;
            VirtualZombieManager.instance.removeZombieFromWorld(zombie);
            if (i < zombies.size() && zombies.get(i) == zombie) continue;
            --i;
        }
    }

    public static void setCustomVariables(IsoPlayer p, IConnection c) {
        for (String key : VariableSyncPacket.syncedVariables) {
            if (p.getVariable(key) == null) continue;
            INetworkPacket.send(c, PacketTypes.PacketType.VariableSync, p, key, p.getVariableString(key));
        }
    }

    public static void sendPlayerConnected(IsoPlayer p, IConnection c) {
        if (p == null) {
            return;
        }
        boolean reply = PlayerToAddressMap.get(p) != null && c.getConnectedGUID() == PlayerToAddressMap.get(p).longValue() && !GameServer.isDelayedDisconnect(p);
        INetworkPacket.send(c, PacketTypes.PacketType.ConnectedPlayer, p, reply);
        GameServer.setCustomVariables(p, c);
        if (!reply) {
            INetworkPacket.send(c, PacketTypes.PacketType.Equip, p);
            GameServer.syncActivatedItems(p, c);
        }
    }

    private static void syncActivatedItems(IsoPlayer p, IConnection c) {
        GameServer.syncActivatedItem(p, p.getPrimaryHandItem(), c);
        GameServer.syncActivatedItem(p, p.getSecondaryHandItem(), c);
        p.getAttachedItems().forEach(item -> GameServer.syncActivatedItem(p, item.getItem(), c));
    }

    private static void syncActivatedItem(IsoPlayer p, InventoryItem item, IConnection c) {
        if (item != null && item.isActivated()) {
            INetworkPacket.send(c, PacketTypes.PacketType.SyncItemActivated, p, item.getID(), item.isActivated());
        }
    }

    public static void receivePlayerConnect(ByteBufferReader bb, IConnection connection, String username) {
        ConnectionManager.log("receive-packet", "player-connect", connection);
        byte playerIndex = bb.getByte();
        DebugLog.DetailedInfo.trace("User: \"%s\" index=%d ip=%s is trying to connect", username, playerIndex, connection.getIP());
        if (playerIndex < 0 || playerIndex >= 4 || connection.getPlayerAt(playerIndex) != null) {
            return;
        }
        byte range = (byte)Math.min(20, bb.getByte());
        connection.setRelevantRange((byte)(range / 2 + 2));
        IsoPlayer player = coop && SteamUtils.isSteamModeEnabled() ? ServerPlayerDB.getInstance().serverLoadNetworkCharacter(playerIndex, connection.getIDStr()) : ServerPlayerDB.getInstance().serverLoadNetworkCharacter(playerIndex, connection.getUserName());
        if (player == null) {
            GameServer.kick(connection, "UI_LoadPlayerProfileError", null);
            connection.forceDisconnect("UI_LoadPlayerProfileError");
            return;
        }
        connection.getRelevantPos((int)playerIndex).x = player.getX();
        connection.getRelevantPos((int)playerIndex).y = player.getY();
        connection.getRelevantPos((int)playerIndex).z = player.getZ();
        connection.setConnectArea(playerIndex, null);
        connection.setChunkGridWidth(range);
        connection.setLoadedCells(playerIndex, new ClientServerMap(playerIndex, PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), range));
        player.realx = player.getX();
        player.realy = player.getY();
        player.realz = (byte)player.getZi();
        player.playerIndex = playerIndex;
        player.onlineChunkGridWidth = range;
        Players.add(player);
        player.remote = true;
        connection.setPlayerAt(playerIndex, player);
        short o = connection.getPlayerId(playerIndex);
        IDToPlayerMap.put(o, player);
        PlayerToAddressMap.put(player, connection.getConnectedGUID());
        player.setOnlineID(o);
        byte extraInfoFlags = bb.getByte();
        player.setRole(connection.getRole());
        player.setExtraInfoFlags(extraInfoFlags, true);
        if (SteamUtils.isSteamModeEnabled()) {
            player.setSteamID(connection.getSteamId());
            SteamGameServer.BUpdateUserData(connection.getSteamId(), connection.getUserName(), 0);
        }
        player.username = username;
        ChatServer.getInstance().initPlayer(player.onlineId);
        connection.setFullyConnected();
        GameServer.sendWeather(connection);
        SafetySystemManager.restoreSafety(player);
        if (!connection.getRole().hasCapability(Capability.HideFromSteamUserList)) {
            SteamGameServer.AddPlayer(player);
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendPlayerConnected(player, c);
            GameServer.sendPlayerExtraInfo(player, c, true);
        }
        for (IsoPlayer isoPlayer : IDToPlayerMap.values()) {
            if (isoPlayer.getOnlineID() == player.getOnlineID() || !isoPlayer.isAlive()) continue;
            GameServer.sendPlayerConnected(isoPlayer, connection);
            GameServer.setCustomVariables(isoPlayer, connection);
            isoPlayer.getNetworkCharacterAI().getState().sync(connection);
            INetworkPacket.send(connection, PacketTypes.PacketType.PlayerInjuries, isoPlayer);
        }
        connection.getLoadedCell(playerIndex).setLoaded();
        connection.getLoadedCell(playerIndex).sendPacket(connection);
        GameServer.preventIndoorZombies(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY()), PZMath.fastfloor(player.getZ()));
        ServerLOS.instance.addPlayer(player);
        WarManager.sendWarToPlayer(player);
        LoggerManager.getLogger("user").write(connection.getIDStr() + " \"" + player.username + "\" fully connected " + LoggerManager.getPlayerCoords(player));
    }

    public static void sendInitialWorldState(IConnection c) {
        if (RainManager.isRaining().booleanValue()) {
            GameServer.sendStartRain(c);
        }
        INetworkPacket.send(c, PacketTypes.PacketType.VehicleTowingState, VehicleManager.instance.towedVehicleMap);
        try {
            if (!ClimateManager.getInstance().isUpdated()) {
                ClimateManager.getInstance().update();
            }
            ClimateManager.getInstance().sendInitialState(c);
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
    }

    public static void sendObjectModData(IsoObject o) {
        if (softReset || fastForward) {
            return;
        }
        INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectModData, o.getX(), o.getY(), o);
    }

    public static void sendSlowFactor(IsoGameCharacter chr) {
        if (!(chr instanceof IsoPlayer)) {
            return;
        }
        IsoPlayer isoPlayer = (IsoPlayer)chr;
        INetworkPacket.send(isoPlayer, PacketTypes.PacketType.SlowFactor, chr);
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, KahluaTable tbl) {
        if (softReset) {
            return;
        }
        if (o == null || o.getSquare() == null) {
            return;
        }
        INetworkPacket.sendToRelative(PacketTypes.PacketType.ObjectChange, (int)o.getX(), (float)((int)o.getY()), new Object[]{o, change, tbl});
    }

    public static void sendObjectChange(IsoObject o, IsoObjectChange change, Object ... objects) {
        if (softReset) {
            return;
        }
        if (objects.length == 0) {
            GameServer.sendObjectChange(o, change, null);
            return;
        }
        if (objects.length % 2 != 0) {
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
        GameServer.sendObjectChange(o, change, t);
    }

    static void receiveSyncIsoObject(ByteBufferReader bb, UdpConnection connection, short packetType) {
        if (!DebugOptions.instance.network.server.syncIsoObject.getValue()) {
            return;
        }
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        byte index = bb.getByte();
        byte exist = bb.getByte();
        byte state = bb.getByte();
        if (exist != 1) {
            return;
        }
        IsoGridSquare sq = ServerMap.instance.getGridSquare(x, y, z);
        if (sq != null && index >= 0 && index < sq.getObjects().size()) {
            sq.getObjects().get(index).syncIsoObject(true, state, connection, bb);
        } else if (sq != null) {
            DebugLog.log("SyncIsoObject: index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
        } else {
            DebugLog.log("SyncIsoObject: sq is null x,y,z=" + x + "," + y + "," + z);
        }
    }

    static void receiveSyncDoorKey(ByteBufferReader bb, UdpConnection connection, short packetType) {
        IsoObject obj;
        int x = bb.getInt();
        int y = bb.getInt();
        int z = bb.getInt();
        byte index = bb.getByte();
        int keyId = bb.getInt();
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq != null && index >= 0 && index < sq.getObjects().size()) {
            obj = sq.getObjects().get(index);
            if (!(obj instanceof IsoDoor)) {
                DebugLog.log("SyncDoorKey: expected IsoDoor index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
                return;
            }
        } else {
            if (sq != null) {
                DebugLog.log("SyncDoorKey: index=" + index + " is invalid x,y,z=" + x + "," + y + "," + z);
                return;
            }
            DebugLog.log("SyncDoorKey: sq is null x,y,z=" + x + "," + y + "," + z);
            return;
        }
        IsoDoor door = (IsoDoor)obj;
        door.keyId = keyId;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SyncDoorKey.doPacket(b);
            b.putInt(x);
            b.putInt(y);
            b.putInt(z);
            b.putByte(index);
            b.putInt(keyId);
            PacketTypes.PacketType.SyncDoorKey.send(c);
        }
    }

    public static int RemoveItemFromMap(IsoObject obj) {
        int x = obj.getSquare().getX();
        int y = obj.getSquare().getY();
        int z = obj.getSquare().getZ();
        int index = obj.getObjectIndex();
        INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveItemFromSquare, null, (float)x, y, obj);
        RemoveItemFromSquarePacket.removeItemFromMap(null, x, y, z, index);
        return index;
    }

    public static void sendBloodSplatter(HandWeapon weapon, float x, float y, float z, Vector2 hitDir, boolean closeKilled, boolean radial) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.BloodSplatter.doPacket(b);
            b.putUTF(weapon != null ? weapon.getType() : "");
            b.putFloat(x);
            b.putFloat(y);
            b.putFloat(z);
            b.putFloat(hitDir.getX());
            b.putFloat(hitDir.getY());
            b.putBoolean(closeKilled);
            b.putBoolean(radial);
            b.putByte(weapon == null ? 0 : Math.max(weapon.getSplatNumber(), 1));
            PacketTypes.PacketType.BloodSplatter.send(c);
        }
    }

    public static void disconnect(UdpConnection connection, String description) {
        if (connection.getPlayerDownloadServer() != null) {
            try {
                connection.getPlayerDownloadServer().destroy();
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
            connection.setPlayerDownloadServer(null);
        }
        RequestDataManager.getInstance().disconnect(connection);
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            IsoPlayer player = connection.players[playerIndex];
            if (player != null) {
                TransactionManager.cancelAllRelevantToUser(player);
                ServerPlayerDB.getInstance().serverUpdateNetworkCharacter(player, playerIndex, connection);
                ChatServer.getInstance().disconnectPlayer(connection.playerIds[playerIndex]);
                GameServer.disconnectPlayer(player, connection);
            }
            connection.usernames[playerIndex] = null;
            connection.players[playerIndex] = null;
            connection.playerIds[playerIndex] = -1;
            connection.releventPos[playerIndex] = null;
            connection.connectArea[playerIndex] = null;
        }
        for (int i = 0; i < udpEngine.getMaxConnections(); ++i) {
            if (SlotToConnection[i] != connection) continue;
            GameServer.SlotToConnection[i] = null;
        }
        Iterator<Map.Entry<Short, Long>> iter = IDToAddressMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Short, Long> entry = iter.next();
            if (entry.getValue().longValue() != connection.getConnectedGUID()) continue;
            iter.remove();
        }
        if (!SteamUtils.isSteamModeEnabled()) {
            PublicServerUtil.updatePlayers();
        }
        if (CoopSlave.instance != null && connection.isCoopHost) {
            DebugLog.log("Host user disconnected, stopping the server");
            ServerMap.instance.QueueQuit();
        }
        if (server) {
            ConnectionManager.log("disconnect", description, connection);
            EventManager.instance().report("[" + connection.getUserName() + "] disconnected from server");
        }
    }

    public static void addIncoming(short id, ByteBufferReader bb, UdpConnection connection) {
        ZomboidNetData d = bb.limit() > 2048 ? ZomboidNetDataPool.instance.getLong(bb.limit()) : ZomboidNetDataPool.instance.get();
        d.read(id, bb, connection);
        if (d.type == null) {
            try {
                AntiCheat.PacketType.act(connection, String.valueOf(id));
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
            return;
        }
        d.time = System.currentTimeMillis();
        if (d.type == PacketTypes.PacketType.PlayerUpdateUnreliable || d.type == PacketTypes.PacketType.PlayerUpdateReliable) {
            MainLoopPlayerUpdateQ.add(d);
        } else if (d.type == PacketTypes.PacketType.VehiclePhysicsReliable || d.type == PacketTypes.PacketType.VehiclePhysicsUnreliable) {
            MainLoopNetDataQ.add(d);
        } else {
            MainLoopNetDataHighPriorityQ.add(d);
        }
    }

    public static void smashWindow(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setSmashWindow(isoWindow);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isRelevantTo(isoWindow.getX(), isoWindow.getY())) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SmashWindow.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.SmashWindow.send(c);
        }
    }

    public static void removeBrokenGlass(IsoWindow isoWindow) {
        SmashWindowPacket packet = new SmashWindowPacket();
        packet.setRemoveBrokenGlass(isoWindow);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isRelevantTo(isoWindow.getX(), isoWindow.getY())) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SmashWindow.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.SmashWindow.send(c);
        }
    }

    public static void sendHitCharacter(HitCharacter packet, PacketTypes.PacketType packetType, UdpConnection connection) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !packet.isRelevant(c)) continue;
            ByteBufferWriter bbw = c.startPacket();
            packetType.doPacket(bbw);
            packet.write(bbw);
            packetType.send(c);
        }
    }

    public static void sendCharacterDeath(IsoDeadBody body) {
        if (body.isZombie()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ZombieDeath, body.getX(), body.getY(), body);
        } else if (body.isAnimal()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalDeath, body.getX(), body.getY(), body);
        } else if (body.isPlayer()) {
            INetworkPacket.sendToRelative(PacketTypes.PacketType.PlayerDeath, body.getX(), body.getY(), body);
        }
    }

    public static void sendItemStats(InventoryItem item) {
        if (item.getContainer() != null && item.getContainer().getParent() instanceof IsoPlayer) {
            INetworkPacket.send(GameServer.getConnectionFromPlayer((IsoPlayer)item.getContainer().getParent()), PacketTypes.PacketType.ItemStats, item.getContainer(), item);
            return;
        }
        if (item.getWorldItem() != null) {
            ItemContainer container = new ItemContainer("floor", item.getWorldItem().square, null);
            INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, item.getWorldItem().getSquare().x, (float)item.getWorldItem().getSquare().y, container, item);
            return;
        }
        if (item.getOutermostContainer() != null) {
            if (item.getOutermostContainer().getSourceGrid() != null) {
                INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, item.getOutermostContainer().getSourceGrid().x, (float)item.getOutermostContainer().getSourceGrid().y, item.getContainer(), item);
            } else if (item.getOutermostContainer().getParent() != null) {
                INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, item.getOutermostContainer().getParent().getX(), item.getOutermostContainer().getParent().getY(), item.getContainer(), item);
            } else {
                INetworkPacket.sendToAll(PacketTypes.PacketType.ItemStats, item.getOutermostContainer(), item);
            }
        }
    }

    public static void receiveEatBody(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            short zombieID;
            IsoZombie zombie;
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, "ReceiveEatBody");
            }
            if ((zombie = ServerMap.instance.zombieMap.get(zombieID = bb.getShort())) == null) {
                DebugLog.Multiplayer.error("ReceiveEatBody: zombie " + zombieID + " not found");
                return;
            }
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (!c.isRelevantTo(zombie.getX(), zombie.getY())) continue;
                if (Core.debug) {
                    DebugLog.log(DebugType.Multiplayer, "SendEatBody");
                }
                ByteBufferWriter bbw = c.startPacket();
                PacketTypes.PacketType.EatBody.doPacket(bbw);
                bb.position(0);
                bbw.put(bb);
                PacketTypes.PacketType.EatBody.send(c);
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "ReceiveEatBody: failed", LogSeverity.Error);
        }
    }

    public static void receiveSyncRadioData(ByteBufferReader bb, UdpConnection connection, short packetType) {
        try {
            boolean isCanHearAll = bb.getBoolean();
            int radioDataSize = bb.getInt();
            int[] radioData = new int[radioDataSize];
            for (int i = 0; i < radioDataSize; ++i) {
                radioData[i] = bb.getInt();
            }
            RakVoice.SetChannelsRouting(connection.getConnectedGUID(), isCanHearAll, radioData, (short)radioDataSize);
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (c == connection || connection.players[0] == null) continue;
                ByteBufferWriter bbw = c.startPacket();
                PacketTypes.PacketType.SyncRadioData.doPacket(bbw);
                bbw.putShort(connection.players[0].onlineId);
                bb.position(0);
                bbw.put(bb);
                PacketTypes.PacketType.SyncRadioData.send(c);
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "SyncRadioData: failed", LogSeverity.Error);
        }
    }

    public static void sendWorldSound(WorldSoundManager.WorldSound sound, UdpConnection connection) {
        WorldSoundPacket packet = new WorldSoundPacket();
        packet.setData(sound);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isFullyConnected() || !c.RelevantTo(sound.x, sound.y, sound.radius)) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.WorldSoundPacket.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.WorldSoundPacket.send(c);
        }
    }

    public static void kick(IConnection connection, String description, String reason) {
        ConnectionManager.log("kick", reason, connection);
        INetworkPacket.send(connection, PacketTypes.PacketType.Kicked, description, reason);
    }

    private static void sendStartRain(IConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.StartRain.doPacket(b);
        b.putInt(RainManager.randRainMin);
        b.putInt(RainManager.randRainMax);
        b.putFloat(RainManager.rainDesiredIntensity);
        PacketTypes.PacketType.StartRain.send(c);
    }

    public static void startRain() {
        if (udpEngine == null) {
            return;
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendStartRain(c);
        }
    }

    private static void sendStopRain(UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.StopRain.doPacket(b);
        PacketTypes.PacketType.StopRain.send(c);
    }

    public static void stopRain() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendStopRain(c);
        }
    }

    private static void sendWeather(IConnection c) {
        WeatherPacket packet = new WeatherPacket();
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.Weather.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.Weather.send(c);
    }

    public static void sendWeather() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendWeather(c);
        }
    }

    private static boolean isInSameFaction(IsoPlayer player1, IsoPlayer player2) {
        Faction factionLocal = Faction.getPlayerFaction(player1);
        Faction factionRemote = Faction.getPlayerFaction(player2);
        return factionLocal != null && factionLocal == factionRemote;
    }

    private static boolean isAnyPlayerInSameFaction(UdpConnection c1, IsoPlayer player2) {
        for (int i = 0; i < 4; ++i) {
            IsoPlayer player1 = c1.players[i];
            if (player1 == null || !GameServer.isInSameFaction(player1, player2)) continue;
            return true;
        }
        return false;
    }

    private static boolean isAnyPlayerInSameSafehouse(UdpConnection c1, IsoPlayer player2) {
        for (int i = 0; i < 4; ++i) {
            IsoPlayer player1 = c1.players[i];
            if (player1 == null || !SafeHouse.isInSameSafehouse(player1.getUsername(), player2.getUsername())) continue;
            return true;
        }
        return false;
    }

    private static boolean shouldSendWorldMapPlayerPosition(UdpConnection c, IsoPlayer player) {
        if (player == null || player.isDead()) {
            return false;
        }
        UdpConnection c2 = GameServer.getConnectionFromPlayer(player);
        if (c2 == null || c2 == c || !c2.isFullyConnected()) {
            return false;
        }
        if (c.getRole().hasCapability(Capability.SeeWorldMap)) {
            return true;
        }
        for (IsoPlayer connectedPlayer : c.players) {
            if (!connectedPlayer.checkCanSeeClient(player)) continue;
            return true;
        }
        int mapRemotePlayerVisibility = ServerOptions.getInstance().mapRemotePlayerVisibility.getValue();
        if (mapRemotePlayerVisibility == 3) {
            return true;
        }
        if (mapRemotePlayerVisibility == 2) {
            return GameServer.isAnyPlayerInSameFaction(c, player) || GameServer.isAnyPlayerInSameSafehouse(c, player);
        }
        return false;
    }

    private static void sendWorldMapPlayerPosition(UdpConnection c) {
        tempPlayers.clear();
        for (int i = 0; i < Players.size(); ++i) {
            IsoPlayer player = Players.get(i);
            if (!GameServer.shouldSendWorldMapPlayerPosition(c, player)) continue;
            tempPlayers.add(player);
        }
        if (tempPlayers.isEmpty()) {
            return;
        }
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(b);
        b.putBoolean(false);
        b.putShort(tempPlayers.size());
        for (int i = 0; i < tempPlayers.size(); ++i) {
            IsoPlayer player = tempPlayers.get(i);
            WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getOrCreatePlayer(player);
            remotePlayer.setPlayer(player);
            b.putShort(remotePlayer.getOnlineID());
            b.putShort(remotePlayer.getChangeCount());
            b.putFloat(remotePlayer.getX());
            b.putFloat(remotePlayer.getY());
        }
        PacketTypes.PacketType.WorldMapPlayerPosition.send(c);
    }

    public static void sendWorldMapPlayerPosition() {
        int mapRemotePlayerVisibility = ServerOptions.getInstance().mapRemotePlayerVisibility.getValue();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendWorldMapPlayerPosition(c);
        }
    }

    public static void receiveWorldMapPlayerPosition(ByteBufferReader bb, UdpConnection connection, short packetType) {
        IsoPlayer player;
        int count = bb.getShort();
        tempPlayers.clear();
        for (int i = 0; i < count; ++i) {
            short playerID = bb.getShort();
            player = IDToPlayerMap.get(playerID);
            if (player == null || !GameServer.shouldSendWorldMapPlayerPosition(connection, player)) continue;
            tempPlayers.add(player);
        }
        if (tempPlayers.isEmpty()) {
            return;
        }
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.WorldMapPlayerPosition.doPacket(b);
        b.putBoolean(true);
        b.putShort(tempPlayers.size());
        for (int i = 0; i < tempPlayers.size(); ++i) {
            player = tempPlayers.get(i);
            WorldMapRemotePlayer remotePlayer = WorldMapRemotePlayers.instance.getOrCreatePlayer(player);
            remotePlayer.setPlayer(player);
            b.putShort(remotePlayer.getOnlineID());
            b.putShort(remotePlayer.getChangeCount());
            b.putUTF(remotePlayer.getUsername());
            b.putUTF(remotePlayer.getForename());
            b.putUTF(remotePlayer.getSurname());
            b.putUTF(remotePlayer.getAccessLevel());
            b.putInt(remotePlayer.getRolePower());
            b.putFloat(remotePlayer.getX());
            b.putFloat(remotePlayer.getY());
            b.putBoolean(remotePlayer.isInvisible());
            b.putBoolean(remotePlayer.isDisguised());
        }
        PacketTypes.PacketType.WorldMapPlayerPosition.send(connection);
    }

    private static void syncClock(UdpConnection c) {
        INetworkPacket.send(c, PacketTypes.PacketType.SyncClock, new Object[0]);
    }

    public static void syncClock() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.syncClock(c);
        }
    }

    public static void sendServerCommand(String module, String command, KahluaTable args2, UdpConnection c) {
        ByteBufferWriter b = c.startPacket();
        PacketTypes.PacketType.ClientCommand.doPacket(b);
        b.putUTF(module);
        b.putUTF(command);
        if (b.putBoolean(args2 != null && !args2.isEmpty())) {
            try {
                KahluaTableIterator it = args2.iterator();
                while (it.advance()) {
                    if (TableNetworkUtils.canSave(it.getKey(), it.getValue())) continue;
                    DebugLog.log("ERROR: sendServerCommand: can't save key,value=" + String.valueOf(it.getKey()) + "," + String.valueOf(it.getValue()));
                }
                TableNetworkUtils.save(args2, b);
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
        PacketTypes.PacketType.ClientCommand.send(c);
    }

    public static void sendServerCommand(String module, String command, KahluaTable args2) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendServerCommand(module, command, args2, c);
        }
    }

    public static void sendServerCommandV(String module, String command, Object ... objects) {
        if (objects.length == 0) {
            GameServer.sendServerCommand(module, command, null);
            return;
        }
        if (objects.length % 2 != 0) {
            DebugLog.log("ERROR: sendServerCommand called with invalid number of arguments (" + module + " " + command + ")");
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
        GameServer.sendServerCommand(module, command, t);
    }

    public static void sendServerCommand(IsoPlayer player, String module, String command, KahluaTable args2) {
        if (!PlayerToAddressMap.containsKey(player)) {
            return;
        }
        long id = PlayerToAddressMap.get(player);
        UdpConnection con = udpEngine.getActiveConnection(id);
        if (con == null) {
            return;
        }
        GameServer.sendServerCommand(module, command, args2, con);
    }

    public static ArrayList<IsoPlayer> getPlayers(ArrayList<IsoPlayer> players) {
        players.clear();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = c.players[playerIndex];
                if (player == null || player.onlineId == -1) continue;
                players.add(player);
            }
        }
        return players;
    }

    public static ArrayList<IsoPlayer> getPlayers() {
        ArrayList<IsoPlayer> players = new ArrayList<IsoPlayer>();
        return GameServer.getPlayers(players);
    }

    public static int getPlayerCount() {
        int count = 0;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getRole() == null || c.getRole().hasCapability(Capability.HideFromSteamUserList)) continue;
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                if (c.playerIds[playerIndex] == -1) continue;
                ++count;
            }
        }
        return count;
    }

    public static String addUser(String newUsername, String newUserPassword) {
        if (!ServerWorldDatabase.isValidUserName(newUsername)) {
            return "Invalid username \"" + newUsername + "\"";
        }
        try {
            return ServerWorldDatabase.instance.addUser(newUsername.trim(), newUserPassword.trim());
        }
        catch (SQLException e) {
            e.printStackTrace();
            return "exception occurs";
        }
    }

    public static String changeRole(String adminName, UdpConnection adminConnection, String user, String newAccessLevelName) throws SQLException {
        UdpConnection c;
        IsoPlayer pl = GameServer.getPlayerByUserName(user);
        if (!ServerWorldDatabase.instance.containsUser(user) && pl != null && (c = GameServer.getConnectionFromPlayer(pl)) != null) {
            String addUserResult = GameServer.addUser(user, c.password);
            if (!ServerWorldDatabase.instance.containsUser(user)) {
                return addUserResult;
            }
        }
        if (adminConnection != null && adminConnection.isCoopHost || ServerWorldDatabase.instance.containsUser(user) || adminConnection == null) {
            Role newRole = Roles.getRole(newAccessLevelName.trim());
            if (adminConnection != null && adminConnection.getRole().hasCapability(Capability.ChangeAccessLevel) && adminConnection.getRole().getPosition() < newRole.getPosition()) {
                return "You do not have sufficient rights to set this access level.";
            }
            if (newRole == null) {
                Object accessLevels = "";
                for (Role r : Roles.getRoles()) {
                    if (!((String)accessLevels).isEmpty()) {
                        accessLevels = (String)accessLevels + ", ";
                    }
                    accessLevels = (String)accessLevels + r.getName();
                }
                return "Access Level '" + newAccessLevelName.trim() + "' unknown, list of access level: " + (String)accessLevels;
            }
            if (pl != null) {
                if (pl.networkAi != null) {
                    pl.networkAi.setCheckAccessLevelDelay(2000L);
                }
                UdpConnection connection1 = GameServer.getConnectionFromPlayer(pl);
                Role oldRole = null;
                if (connection1 != null) {
                    oldRole = connection1.getRole();
                }
                if (oldRole != newRole) {
                    if (newRole.hasCapability(Capability.AdminChat) && !oldRole.hasCapability(Capability.AdminChat)) {
                        ChatServer.getInstance().joinAdminChat(pl.onlineId);
                    } else if (!newRole.hasCapability(Capability.AdminChat) && oldRole.hasCapability(Capability.AdminChat)) {
                        ChatServer.getInstance().leaveAdminChat(pl.onlineId);
                    }
                }
                if (!newRole.hasCapability(Capability.ToggleInvisibleHimself) && oldRole.hasCapability(Capability.ToggleInvisibleHimself)) {
                    pl.setGhostMode(false);
                }
                if (!newRole.hasCapability(Capability.ToggleNoclipHimself) && oldRole.hasCapability(Capability.ToggleNoclipHimself)) {
                    pl.setNoClip(false);
                }
                if (!newRole.hasCapability(Capability.ToggleGodModHimself) && oldRole.hasCapability(Capability.ToggleGodModHimself)) {
                    pl.setGodMod(false);
                }
                pl.setRole(newRole);
                if (connection1 != null) {
                    connection1.setRole(newRole);
                }
                if (!newRole.hasCapability(Capability.HideFromSteamUserList) && oldRole.hasCapability(Capability.HideFromSteamUserList)) {
                    SteamGameServer.AddPlayer(pl);
                }
                if (newRole.hasCapability(Capability.HideFromSteamUserList) && !oldRole.hasCapability(Capability.HideFromSteamUserList)) {
                    SteamGameServer.RemovePlayer(pl);
                }
                if (newRole.hasCapability(Capability.ToggleInvisibleHimself) && !oldRole.hasCapability(Capability.ToggleInvisibleHimself)) {
                    pl.setGhostMode(true);
                }
                if (newRole.hasCapability(Capability.ToggleNoclipHimself) && !oldRole.hasCapability(Capability.ToggleNoclipHimself)) {
                    pl.setNoClip(true);
                }
                if (newRole.hasCapability(Capability.ToggleGodModHimself) && !oldRole.hasCapability(Capability.ToggleGodModHimself)) {
                    pl.setGodMod(true);
                }
                GameServer.sendPlayerExtraInfo(pl, null);
            }
            LoggerManager.getLogger("admin").write(adminName + " granted " + newRole.getName() + " access level on " + user);
            return ServerWorldDatabase.instance.setRole(user, newRole);
        }
        return "User \"" + user + "\" is not in the whitelist nor the server, use /adduser first";
    }

    public static void sendAmbient(String name, int x, int y, int radius, float volume) {
        DebugLog.log(DebugType.Sound, "ambient: sending " + name + " at " + x + "," + y + " radius=" + radius);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
            if (p == null) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.AddAmbient.doPacket(b2);
            b2.putUTF(name);
            b2.putInt(x);
            b2.putInt(y);
            b2.putInt(radius);
            b2.putFloat(volume);
            PacketTypes.PacketType.AddAmbient.send(c);
        }
    }

    public static void sendChangeSafety(Safety safety) {
        try {
            SafetyPacket packet = new SafetyPacket(safety);
            for (UdpConnection c : GameServer.udpEngine.connections) {
                ByteBufferWriter b = c.startPacket();
                PacketTypes.PacketType.ChangeSafety.doPacket(b);
                packet.write(b);
                PacketTypes.PacketType.ChangeSafety.send(c);
            }
        }
        catch (Exception e) {
            DebugLog.Multiplayer.printException(e, "SendChangeSafety: failed", LogSeverity.Error);
        }
    }

    static void receivePing(ByteBufferReader bb, UdpConnection connection, short packetType) {
        connection.setPinged(true);
        GameServer.answerPing(bb, connection);
    }

    public static void updateOverlayForClients(IsoObject object, String spriteName, float r, float g, float b, float a, UdpConnection playerConnection) {
        if (udpEngine == null) {
            return;
        }
        INetworkPacket.sendToRelative(PacketTypes.PacketType.UpdateOverlaySprite, playerConnection, (float)object.square.x, object.square.y, object, spriteName, Float.valueOf(r), Float.valueOf(g), Float.valueOf(b), Float.valueOf(a));
    }

    public static void sendReanimatedZombieID(IsoPlayer player, IsoZombie zombie) {
        if (PlayerToAddressMap.containsKey(player)) {
            GameServer.sendObjectChange((IsoObject)player, IsoObjectChange.REANIMATED_ID, "ID", zombie.onlineId);
        }
    }

    public static void receiveRadioServerData(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ByteBufferWriter bb2 = connection.startPacket();
        PacketTypes.PacketType.RadioServerData.doPacket(bb2);
        ZomboidRadio.getInstance().WriteRadioServerDataPacket(bb2);
        PacketTypes.PacketType.RadioServerData.send(connection);
    }

    public static void receiveRadioDeviceDataState(ByteBufferReader bb, UdpConnection connection, short packetType) {
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
            byte playerIndex = bb.getByte();
            IsoPlayer player = GameServer.getPlayerFromConnection(connection, playerIndex);
            byte hand = bb.getByte();
            if (player != null) {
                Radio radio = null;
                if (hand == 1 && player.getPrimaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getPrimaryHandItem();
                }
                if (hand == 2 && player.getSecondaryHandItem() instanceof Radio) {
                    radio = (Radio)player.getSecondaryHandItem();
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

    public static void sendIsoWaveSignal(long source2, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV) {
        WaveSignalPacket packet = new WaveSignalPacket();
        packet.set(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (source2 == c.getConnectedGUID()) continue;
            ByteBufferWriter bb = c.startPacket();
            PacketTypes.PacketType.WaveSignal.doPacket(bb);
            packet.write(bb);
            PacketTypes.PacketType.WaveSignal.send(c);
        }
    }

    public static void receivePlayerListensChannel(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int channel = bb.getInt();
        boolean listenmode = bb.getBoolean();
        boolean isTV = bb.getBoolean();
        ZomboidRadio.getInstance().PlayerListensChannel(channel, listenmode, isTV);
    }

    public static void sendAlarm(int x, int y) {
        DebugLog.log(DebugType.Multiplayer, "SendAlarm at [ " + x + " , " + y + " ]");
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
            if (p == null) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.AddAlarm.doPacket(b2);
            b2.putInt(x);
            b2.putInt(y);
            PacketTypes.PacketType.AddAlarm.send(c);
        }
    }

    public static void sendToxicBuilding(int x, int y, boolean toxic) {
        DebugLog.log(DebugType.Multiplayer, "Send Toxic Building at [ " + x + " , " + y + " Toxic: " + toxic + " ]");
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
            if (p == null) continue;
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.ToxicBuilding.doPacket(b2);
            b2.putInt(x);
            b2.putInt(y);
            b2.putBoolean(toxic);
            PacketTypes.PacketType.ToxicBuilding.send(c);
        }
    }

    public static boolean isSpawnBuilding(BuildingDef def) {
        return SpawnPoints.instance.isSpawnBuilding(def);
    }

    private static void setFastForward(boolean fastForward) {
        if (fastForward == GameServer.fastForward) {
            return;
        }
        GameServer.fastForward = fastForward;
        GameServer.syncClock();
    }

    public static void sendAdminMessage(String message, int x, int y, int z) {
        MessageForAdminPacket packet = new MessageForAdminPacket();
        packet.setData(message, x, y, z);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.getRole().hasCapability(Capability.CanSeeMessageForAdmin)) continue;
            packet.sendToClient(PacketTypes.PacketType.MessageForAdmin, c);
        }
    }

    static void receiveSendFactionInvite(ByteBufferReader bb, UdpConnection connection, short packetType) {
        String factionName = bb.getUTF();
        String host = bb.getUTF();
        String invitedName = bb.getUTF();
        IsoPlayer invitedP = GameServer.getPlayerByUserName(invitedName);
        if (invitedP == null) {
            return;
        }
        Long invited = IDToAddressMap.get(invitedP.getOnlineID());
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != invited.longValue()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SendFactionInvite.doPacket(b);
            b.putUTF(factionName);
            b.putUTF(host);
            PacketTypes.PacketType.SendFactionInvite.send(c);
            break;
        }
    }

    static void receiveAcceptedFactionInvite(ByteBufferReader bb, UdpConnection connection, short packetType) {
        String factionName = bb.getUTF();
        String host = bb.getUTF();
        IsoPlayer invitedP = GameServer.getPlayerByUserName(host);
        Long invited = IDToAddressMap.get(invitedP.getOnlineID());
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            Faction faction;
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != invited.longValue() || (faction = Faction.getPlayerFaction(c.getUserName())) == null || !faction.getName().equals(factionName)) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.AcceptedFactionInvite.doPacket(b);
            b.putUTF(factionName);
            b.putUTF(host);
            PacketTypes.PacketType.AcceptedFactionInvite.send(c);
        }
    }

    static void receiveViewBannedIPs(ByteBufferReader bb, UdpConnection connection, short packetType) throws SQLException {
        GameServer.sendBannedIPs(connection);
    }

    private static void sendBannedIPs(UdpConnection connection) throws SQLException {
        ArrayList<DBBannedIP> result = ServerWorldDatabase.instance.getBannedIPs();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.ViewBannedIPs.doPacket(b);
            b.putInt(result.size());
            for (int i = 0; i < result.size(); ++i) {
                DBBannedIP bannedIP = result.get(i);
                b.putUTF(bannedIP.getUsername());
                b.putUTF(bannedIP.getIp());
                b.putUTF(bannedIP.getReason());
            }
            PacketTypes.PacketType.ViewBannedIPs.send(c);
            break;
        }
    }

    static void receiveViewBannedSteamIDs(ByteBufferReader bb, UdpConnection connection, short packetType) throws SQLException {
        GameServer.sendBannedSteamIDs(connection);
    }

    private static void sendBannedSteamIDs(UdpConnection connection) throws SQLException {
        ArrayList<DBBannedSteamID> result = ServerWorldDatabase.instance.getBannedSteamIDs();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.ViewBannedSteamIDs.doPacket(b);
            b.putInt(result.size());
            for (int i = 0; i < result.size(); ++i) {
                DBBannedSteamID bannedSteamID = result.get(i);
                b.putUTF(bannedSteamID.getSteamID());
                b.putUTF(bannedSteamID.getReason());
            }
            PacketTypes.PacketType.ViewBannedSteamIDs.send(c);
            break;
        }
    }

    public static void sendTickets(String author, UdpConnection connection) throws SQLException {
        ArrayList<DBTicket> result = ServerWorldDatabase.instance.getTickets(author);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() != connection.getConnectedGUID()) continue;
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.ViewTickets.doPacket(b);
            b.putInt(result.size());
            for (int i = 0; i < result.size(); ++i) {
                DBTicket ticket = result.get(i);
                b.putUTF(ticket.getAuthor());
                b.putUTF(ticket.getMessage());
                b.putInt(ticket.getTicketID());
                b.putBoolean(ticket.isViewed());
                if (!b.putBoolean(ticket.getAnswer() != null)) continue;
                b.putUTF(ticket.getAnswer().getAuthor());
                b.putUTF(ticket.getAnswer().getMessage());
                b.putInt(ticket.getAnswer().getTicketID());
                b.putBoolean(ticket.getAnswer().isViewed());
            }
            PacketTypes.PacketType.ViewTickets.send(c);
            break;
        }
    }

    public static boolean sendItemListNet(UdpConnection ignore, IsoPlayer sender, ArrayList<InventoryItem> items, IsoPlayer receiver, String sessionID, String custom) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (ignore != null && c == ignore) continue;
            if (receiver != null) {
                boolean hasReceiver = false;
                for (int i = 0; i < c.players.length; ++i) {
                    IsoPlayer p = c.players[i];
                    if (p == null || p != receiver) continue;
                    hasReceiver = true;
                    break;
                }
                if (!hasReceiver) continue;
            }
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.SendItemListNet.doPacket(b);
            if (b.putBoolean(receiver != null)) {
                b.putShort(receiver.getOnlineID());
            }
            if (b.putBoolean(sender != null)) {
                b.putShort(sender.getOnlineID());
            }
            b.putUTF(sessionID);
            if (b.putBoolean(custom != null)) {
                b.putUTF(custom);
            }
            try {
                CompressIdenticalItems.save(b.bb, items, null);
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
                c.cancelPacket();
                return false;
            }
            PacketTypes.PacketType.SendItemListNet.send(c);
        }
        return true;
    }

    static void receiveSendItemListNet(ByteBufferReader bb, UdpConnection connection, short packetType) {
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
        ArrayList<InventoryItem> items = new ArrayList<InventoryItem>();
        try {
            CompressIdenticalItems.load(bb.bb, 244, items, null);
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
        }
        if (receiver == null) {
            LuaEventManager.triggerEvent("OnReceiveItemListNet", sender, items, receiver, sessionID, custom);
        } else {
            GameServer.sendItemListNet(connection, sender, items, receiver, sessionID, custom);
        }
    }

    static void receiveClimateManagerPacket(ByteBufferReader bb, UdpConnection connection, short packetType) {
        ClimateManager cm = ClimateManager.getInstance();
        if (cm != null) {
            try {
                cm.receiveClimatePacket(bb, connection);
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
        }
    }

    static void receiveIsoRegionClientRequestFullUpdate(ByteBufferReader bb, UdpConnection connection, short packetType) {
        IsoRegions.receiveClientRequestFullDataChunks(bb, connection);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String isWorldVersionUnsupported() {
        File inFile = new File(ZomboidFileSystem.instance.getSaveDir() + File.separator + "Multiplayer" + File.separator + serverName + File.separator + "map_t.bin");
        if (!inFile.exists()) {
            DebugLog.log("map_t.bin does not exist, cannot determine the server's WorldVersion.  This is ok the first time a server is started.");
            return null;
        }
        DebugLog.log("checking server WorldVersion in map_t.bin");
        try (FileInputStream inStream = new FileInputStream(inFile);
             DataInputStream input = new DataInputStream(inStream);){
            byte b1 = input.readByte();
            byte b2 = input.readByte();
            byte b3 = input.readByte();
            byte b4 = input.readByte();
            if (b1 == 71 && b2 == 77 && b3 == 84 && b4 == 77) {
                int savedWorldVersion = input.readInt();
                if (savedWorldVersion > 244) {
                    String string = "The server savefile appears to be from a newer version of the game and cannot be loaded.";
                    return string;
                }
                if (savedWorldVersion > 143) return null;
                String string = "The server savefile appears to be from a pre-animations version of the game and cannot be loaded.\nDue to the extent of changes required to implement animations, saves from earlier versions are not compatible.";
                return string;
            }
            String string = "The server savefile appears to be from an old version of the game and cannot be loaded.";
            return string;
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
            return null;
        }
    }

    public String getPoisonousBerry() {
        return this.poisonousBerry;
    }

    public void setPoisonousBerry(String poisonousBerry) {
        this.poisonousBerry = poisonousBerry;
    }

    public String getPoisonousMushroom() {
        return this.poisonousMushroom;
    }

    public void setPoisonousMushroom(String poisonousMushroom) {
        this.poisonousMushroom = poisonousMushroom;
    }

    public String getDifficulty() {
        return this.difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public static void transmitBrokenGlass(IsoGridSquare sq) {
        AddBrokenGlassPacket packet = new AddBrokenGlassPacket();
        packet.set(sq);
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            try {
                if (!c.isRelevantTo(sq.getX(), sq.getY())) continue;
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.AddBrokenGlass.doPacket(b2);
                packet.write(b2);
                PacketTypes.PacketType.AddBrokenGlass.send(c);
                continue;
            }
            catch (Throwable t) {
                c.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    public static void transmitBigWaterSplash(int x, int y, float dx, float dy) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            try {
                if (!c.isRelevantTo(x, y)) continue;
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.StartFishSplash.doPacket(b2);
                b2.putInt(x);
                b2.putInt(y);
                b2.putFloat(dx);
                b2.putFloat(dy);
                PacketTypes.PacketType.StartFishSplash.send(c);
                continue;
            }
            catch (Throwable t) {
                c.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    public static void receiveBigWaterSplash(ByteBufferReader bb, UdpConnection connection, short packetType) {
        int x = bb.getInt();
        int y = bb.getInt();
        float dx = bb.getFloat();
        float dy = bb.getFloat();
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || !c.isRelevantTo(x, y)) continue;
            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.StartFishSplash.doPacket(b2);
                b2.putInt(x);
                b2.putInt(y);
                b2.putFloat(dx);
                b2.putFloat(dy);
                PacketTypes.PacketType.StartFishSplash.send(c);
                continue;
            }
            catch (Throwable t) {
                c.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    public static void transmitFishingData(int seed, int trashSeed, TLongIntHashMap noiseFishPointDisabler, TLongObjectHashMap<FishSchoolManager.ChumData> chumPoints) {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.FishingData.doPacket(b2);
                b2.putInt(seed);
                b2.putInt(trashSeed);
                b2.putInt(noiseFishPointDisabler.size());
                noiseFishPointDisabler.forEachKey(l -> {
                    b2.putLong(l);
                    return true;
                });
                b2.putInt(chumPoints.size());
                chumPoints.forEachEntry((key, chumData) -> {
                    b2.putLong(key);
                    b2.putInt(chumData.maxForceTime);
                    return true;
                });
                PacketTypes.PacketType.FishingData.send(c);
                continue;
            }
            catch (Throwable t) {
                c.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    static void receiveFishingDataRequest(ByteBufferReader bb, UdpConnection c, short packetType) {
        try {
            ByteBufferWriter b2 = c.startPacket();
            PacketTypes.PacketType.FishingData.doPacket(b2);
            FishSchoolManager.getInstance().setFishingData(b2);
            PacketTypes.PacketType.FishingData.send(c);
        }
        catch (Throwable t) {
            c.cancelPacket();
            ExceptionLogger.logException(t);
        }
    }

    public static boolean isServerDropPackets() {
        return droppedPackets > 0;
    }

    static void receiveSyncPerks(ByteBufferReader bb, UdpConnection connection, short packetType) {
        byte playerIndex = bb.getByte();
        int sneakLvl = bb.getInt();
        int strLvl = bb.getInt();
        int fitLvl = bb.getInt();
        IsoPlayer p = GameServer.getPlayerFromConnection(connection, playerIndex);
        if (p == null) {
            return;
        }
        p.remoteSneakLvl = sneakLvl;
        p.remoteStrLvl = strLvl;
        p.remoteFitLvl = fitLvl;
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            IsoPlayer p2;
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || (p2 = GameServer.getAnyPlayerFromConnection(connection)) == null) continue;
            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SyncPerks.doPacket(b2);
                b2.putShort(p.onlineId);
                b2.putInt(sneakLvl);
                b2.putInt(strLvl);
                b2.putInt(fitLvl);
                PacketTypes.PacketType.SyncPerks.send(c);
                continue;
            }
            catch (Throwable t) {
                connection.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    static void receiveSyncEquippedRadioFreq(ByteBufferReader bb, UdpConnection connection, short packetType) {
        byte playerIndex = bb.getByte();
        int size = bb.getInt();
        ArrayList<Integer> invRadioFreq = new ArrayList<Integer>();
        for (int i = 0; i < size; ++i) {
            invRadioFreq.add(bb.getInt());
        }
        IsoPlayer p = GameServer.getPlayerFromConnection(connection, playerIndex);
        if (p == null) {
            return;
        }
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            IsoPlayer p2;
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.getConnectedGUID() == connection.getConnectedGUID() || (p2 = GameServer.getAnyPlayerFromConnection(connection)) == null) continue;
            try {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.SyncEquippedRadioFreq.doPacket(b2);
                b2.putShort(p.onlineId);
                b2.putInt(size);
                for (int i = 0; i < invRadioFreq.size(); ++i) {
                    b2.putInt((Integer)invRadioFreq.get(i));
                }
                PacketTypes.PacketType.SyncEquippedRadioFreq.send(c);
                continue;
            }
            catch (Throwable t) {
                connection.cancelPacket();
                ExceptionLogger.logException(t);
            }
        }
    }

    public static void sendRadioPostSilence() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            GameServer.sendRadioPostSilence(c);
        }
    }

    public static void sendRadioPostSilence(UdpConnection c) {
        try {
            ByteBufferWriter b = c.startPacket();
            PacketTypes.PacketType.RadioPostSilenceEvent.doPacket(b);
            b.putBoolean(ZomboidRadio.postRadioSilence);
            PacketTypes.PacketType.RadioPostSilenceEvent.send(c);
        }
        catch (Exception e) {
            DebugLog.General.printException(e, "", LogSeverity.Error);
            c.cancelPacket();
        }
    }

    static {
        portCommandline = -1;
        udpPortCommandline = -1;
        seed = "";
        IDToAddressMap = new HashMap();
        IDToPlayerMap = new HashMap();
        Players = new ArrayList();
        DebugPlayer = new HashSet();
        ServerMods = new ArrayList();
        WorkshopItems = new ArrayList();
        serverName = "servertest";
        discordBot = new DiscordBot(serverName, (user, msg) -> ChatServer.getInstance().sendMessageFromDiscordToGeneralChat(user, msg));
        checksum = "";
        gameMap = "Muldraugh, KY";
        ip = "127.0.0.1";
        SlotToConnection = new UdpConnection[512];
        PlayerToAddressMap = new HashMap();
        consoleCommands = new ArrayList();
        MainLoopPlayerUpdateQ = new ConcurrentLinkedQueue();
        MainLoopNetDataHighPriorityQ = new ConcurrentLinkedQueue();
        MainLoopNetDataQ = new ConcurrentLinkedQueue();
        MainLoopNetData2 = new ArrayList();
        playerToCoordsMap = new HashMap();
        calcCountPlayersInRelevantPositionLimiter = new UpdateLimit(2000L);
        sendWorldMapPlayerPositionLimiter = new UpdateLimit(1000L);
        mainCycleExceptionLogCount = 25;
        tempPlayers = new ArrayList();
        MainLoopDelayedDisconnectQ = new ConcurrentHashMap();
        shutdownHook = new Thread(){

            @Override
            public void run() {
                try {
                    System.out.println("Shutdown handling started");
                    CoopSlave.status("UI_ServerStatus_Terminated");
                    DebugLog.log(DebugType.Network, "Server exited");
                    if (softReset) {
                        return;
                    }
                    done = true;
                    ServerMap.instance.QueuedQuit();
                    Set<Thread> runningThreads = Thread.getAllStackTraces().keySet();
                    for (Thread th : runningThreads) {
                        if (th == Thread.currentThread() || th.isDaemon() || !th.getClass().getName().startsWith("zombie")) continue;
                        System.out.println("Interrupting '" + String.valueOf(th.getClass()) + "' termination");
                        th.interrupt();
                    }
                    for (Thread th : runningThreads) {
                        if (th == Thread.currentThread() || th.isDaemon() || !th.isInterrupted()) continue;
                        System.out.println("Waiting '" + th.getName() + "' termination");
                        th.join();
                    }
                }
                catch (InterruptedException ex) {
                    System.out.println("Shutdown handling interrupted");
                }
                System.out.println("Shutdown handling finished");
            }
        };
    }

    private static class DelayedConnection
    implements IZomboidPacket {
        public UdpConnection connection;
        public boolean connect;
        public String hostString;
        public long timestamp;

        public DelayedConnection(UdpConnection connection, boolean connect2) {
            this.connection = connection;
            this.connect = connect2;
            if (connect2) {
                try {
                    this.hostString = connection.getInetSocketAddress().getHostString();
                }
                catch (Exception e) {
                    DebugLog.General.printException(e, "", LogSeverity.Error);
                }
            }
            this.timestamp = System.currentTimeMillis() + (long)ServerOptions.getInstance().safetyDisconnectDelay.getValue() * 2000L;
        }

        @Override
        public boolean isConnect() {
            return this.connect;
        }

        @Override
        public boolean isDisconnect() {
            return !this.connect;
        }

        public boolean isCooldown() {
            return System.currentTimeMillis() > this.timestamp;
        }

        public void connect() {
            LoggerManager.getLogger("user").write(String.format("Connection add index=%d guid=%d id=%s", this.connection.getIndex(), this.connection.getConnectedGUID(), this.connection.getIDStr()));
            GameServer.udpEngine.connections.add(this.connection);
        }

        public void disconnect() {
            LoginQueue.disconnect(this.connection);
            ActionManager.getInstance().disconnectPlayer(this.connection);
            LoggerManager.getLogger("user").write(String.format("Connection remove index=%d guid=%d id=%s", this.connection.getIndex(), this.connection.getConnectedGUID(), this.connection.getIDStr()));
            GameServer.udpEngine.connections.remove(this.connection);
            GameServer.disconnect(this.connection, "receive-disconnect");
        }
    }

    private static class s_performance {
        static final PerformanceProfileFrameProbe frameStep = new PerformanceProfileFrameProbe("GameServer.frameStep");
        static final PerformanceProfileProbe mainLoopDealWithNetData = new PerformanceProfileProbe("GameServer.mainLoopDealWithNetData");
        static final PerformanceProfileProbe RCONServerUpdate = new PerformanceProfileProbe("RCONServer.update");

        private s_performance() {
        }
    }

    private static final class CCFilter {
        String command;
        boolean allow;
        CCFilter next;

        private CCFilter() {
        }

        boolean matches(String command) {
            return this.command.equals(command) || "*".equals(this.command);
        }

        boolean passes(String command) {
            if (this.matches(command)) {
                return this.allow;
            }
            if (this.next == null) {
                return true;
            }
            return this.next.passes(command);
        }
    }
}

