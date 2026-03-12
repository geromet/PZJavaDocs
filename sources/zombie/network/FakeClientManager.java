/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import com.google.common.collect.Sets;
import fmod.fmod.FMODManager;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.zip.CRC32;
import org.json.JSONArray;
import org.json.JSONObject;
import zombie.characters.NetworkCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.RakNetPeerInterface;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.core.random.RandLua;
import zombie.core.random.RandStandard;
import zombie.core.secure.PZcrypt;
import zombie.core.utils.UpdateLimit;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoDirections;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.NetworkVariables;
import zombie.network.PacketTypes;
import zombie.network.packets.character.PlayerInjuriesPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.PathFindBehavior2;

public class FakeClientManager {
    private static final int SERVER_PORT = 16261;
    private static final int CLIENT_PORT = 17500;
    private static final String CLIENT_ADDRESS = "0.0.0.0";
    private static final String versionNumber = Core.getInstance().getVersionNumber();
    private static final DateFormat logDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private static int logLevel;
    private static final long startTime;
    private static final HashSet<Player> players;

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static HashMap<Integer, Movement> load(String filename) {
        HashMap<Integer, Movement> movements = new HashMap<Integer, Movement>();
        try {
            Path path = Paths.get(filename, new String[0]);
            String jsonString = new String(Files.readAllBytes(path));
            JSONObject jsonObject = new JSONObject(jsonString);
            Movement.version = jsonObject.getString("version");
            JSONObject jsonConfig = jsonObject.getJSONObject("config");
            JSONObject jsonClient = jsonConfig.getJSONObject("client");
            JSONObject jsonConnection = jsonClient.getJSONObject("connection");
            if (jsonConnection.has("serverHost")) {
                Client.connectionServerHost = jsonConnection.getString("serverHost");
            }
            Client.connectionInterval = jsonConnection.getLong("interval");
            Client.connectionTimeout = jsonConnection.getLong("timeout");
            Client.connectionDelay = jsonConnection.getLong("delay");
            JSONObject jsonStatistics = jsonClient.getJSONObject("statistics");
            Client.statisticsPeriod = jsonStatistics.getInt("period");
            Client.statisticsClientID = Math.max(jsonStatistics.getInt("id"), -1);
            if (jsonClient.has("checksum")) {
                JSONObject jsonChecksum = jsonClient.getJSONObject("checksum");
                Client.luaChecksum = jsonChecksum.getString("lua");
                Client.scriptChecksum = jsonChecksum.getString("script");
            }
            if (jsonConfig.has("zombies")) {
                JSONObject jsonZombies = jsonConfig.getJSONObject("zombies");
                ZombieSimulator.Behaviour behaviour = ZombieSimulator.Behaviour.Normal;
                if (jsonZombies.has("behaviour")) {
                    behaviour = ZombieSimulator.Behaviour.valueOf(jsonZombies.getString("behaviour"));
                }
                ZombieSimulator.behaviour = behaviour;
                if (jsonZombies.has("maxZombiesPerUpdate")) {
                    ZombieSimulator.maxZombiesPerUpdate = jsonZombies.getInt("maxZombiesPerUpdate");
                }
                if (jsonZombies.has("deleteZombieDistance")) {
                    int val = jsonZombies.getInt("deleteZombieDistance");
                    ZombieSimulator.deleteZombieDistanceSquared = val * val;
                }
                if (jsonZombies.has("forgotZombieDistance")) {
                    int val = jsonZombies.getInt("forgotZombieDistance");
                    ZombieSimulator.forgotZombieDistanceSquared = val * val;
                }
                if (jsonZombies.has("canSeeZombieDistance")) {
                    int val = jsonZombies.getInt("canSeeZombieDistance");
                    ZombieSimulator.canSeeZombieDistanceSquared = val * val;
                }
                if (jsonZombies.has("seeZombieDistance")) {
                    int val = jsonZombies.getInt("seeZombieDistance");
                    ZombieSimulator.seeZombieDistanceSquared = val * val;
                }
                if (jsonZombies.has("canChangeTarget")) {
                    ZombieSimulator.canChangeTarget = jsonZombies.getBoolean("canChangeTarget");
                }
            }
            JSONObject jsonPlayer = jsonConfig.getJSONObject("player");
            Player.fps = jsonPlayer.getInt("fps");
            Player.predictInterval = jsonPlayer.getInt("predict");
            if (jsonPlayer.has("damage")) {
                Player.damage = (float)jsonPlayer.getDouble("damage");
            }
            if (jsonPlayer.has("voip")) {
                Player.isVOIPEnabled = jsonPlayer.getBoolean("voip");
            }
            JSONObject jsonMovement = jsonConfig.getJSONObject("movement");
            Movement.defaultRadius = jsonMovement.getInt("radius");
            JSONObject jsonMotion = jsonMovement.getJSONObject("motion");
            Movement.aimSpeed = jsonMotion.getInt("aim");
            Movement.sneakSpeed = jsonMotion.getInt("sneak");
            Movement.sneakRunSpeed = jsonMotion.getInt("sneakrun");
            Movement.walkSpeed = jsonMotion.getInt("walk");
            Movement.runSpeed = jsonMotion.getInt("run");
            Movement.sprintSpeed = jsonMotion.getInt("sprint");
            JSONObject jsonPedestrianSpeed = jsonMotion.getJSONObject("pedestrian");
            Movement.pedestrianSpeedMin = jsonPedestrianSpeed.getInt("min");
            Movement.pedestrianSpeedMax = jsonPedestrianSpeed.getInt("max");
            JSONObject jsonVehicleSpeed = jsonMotion.getJSONObject("vehicle");
            Movement.vehicleSpeedMin = jsonVehicleSpeed.getInt("min");
            Movement.vehicleSpeedMax = jsonVehicleSpeed.getInt("max");
            JSONArray jsonMovements = jsonObject.getJSONArray("movements");
            for (int i = 0; i < jsonMovements.length(); ++i) {
                Movement.Motion motion;
                jsonMovement = jsonMovements.getJSONObject(i);
                int id = jsonMovement.getInt("id");
                String description = null;
                if (jsonMovement.has("description")) {
                    description = jsonMovement.getString("description");
                }
                int spawnX = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                int spawnY = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                if (jsonMovement.has("spawn")) {
                    JSONObject jsonSpawn = jsonMovement.getJSONObject("spawn");
                    spawnX = jsonSpawn.getInt("x");
                    spawnY = jsonSpawn.getInt("y");
                }
                Movement.Motion motion2 = motion = Math.random() > (double)0.8f ? Movement.Motion.Vehicle : Movement.Motion.Pedestrian;
                if (jsonMovement.has("motion")) {
                    motion = Movement.Motion.valueOf(jsonMovement.getString("motion"));
                }
                int speed = 0;
                if (jsonMovement.has("speed")) {
                    speed = jsonMovement.getInt("speed");
                } else {
                    switch (motion.ordinal()) {
                        case 0: {
                            speed = Movement.aimSpeed;
                            break;
                        }
                        case 1: {
                            speed = Movement.sneakSpeed;
                            break;
                        }
                        case 3: {
                            speed = Movement.sneakRunSpeed;
                            break;
                        }
                        case 2: {
                            speed = Movement.walkSpeed;
                            break;
                        }
                        case 4: {
                            speed = Movement.runSpeed;
                            break;
                        }
                        case 5: {
                            speed = Movement.sprintSpeed;
                            break;
                        }
                        case 6: {
                            speed = (int)Math.round(Math.random() * (double)(Movement.pedestrianSpeedMax - Movement.pedestrianSpeedMin) + (double)Movement.pedestrianSpeedMin);
                            break;
                        }
                        case 7: {
                            speed = (int)Math.round(Math.random() * (double)(Movement.vehicleSpeedMax - Movement.vehicleSpeedMin) + (double)Movement.vehicleSpeedMin);
                        }
                    }
                }
                Movement.Type type = Movement.Type.Line;
                if (jsonMovement.has("type")) {
                    type = Movement.Type.valueOf(jsonMovement.getString("type"));
                }
                int radius = Movement.defaultRadius;
                if (jsonMovement.has("radius")) {
                    radius = jsonMovement.getInt("radius");
                }
                IsoDirections direction = IsoDirections.getRandom();
                if (jsonMovement.has("direction")) {
                    direction = IsoDirections.valueOf(jsonMovement.getString("direction"));
                }
                boolean ghost = false;
                if (jsonMovement.has("ghost")) {
                    ghost = jsonMovement.getBoolean("ghost");
                }
                long connectDelay = (long)id * Client.connectionInterval;
                if (jsonMovement.has("connect")) {
                    connectDelay = jsonMovement.getLong("connect");
                }
                long disconnectDelay = 0L;
                if (jsonMovement.has("disconnect")) {
                    disconnectDelay = jsonMovement.getLong("disconnect");
                }
                long reconnectDelay = 0L;
                if (jsonMovement.has("reconnect")) {
                    reconnectDelay = jsonMovement.getLong("reconnect");
                }
                long teleportDelay = 0L;
                if (jsonMovement.has("teleport")) {
                    teleportDelay = jsonMovement.getLong("teleport");
                }
                int destinationX = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                int destinationY = (int)Math.round(Math.random() * 6000.0 + 6000.0);
                if (jsonMovement.has("destination")) {
                    JSONObject jsonSpawn = jsonMovement.getJSONObject("destination");
                    destinationX = jsonSpawn.getInt("x");
                    destinationY = jsonSpawn.getInt("y");
                }
                HordeCreator hordeCreator = null;
                if (jsonMovement.has("createHorde")) {
                    JSONObject jsonCreateHorde = jsonMovement.getJSONObject("createHorde");
                    int hordeCount = jsonCreateHorde.getInt("count");
                    int hordeRadius = jsonCreateHorde.getInt("radius");
                    long hordeInterval = jsonCreateHorde.getLong("interval");
                    if (hordeInterval != 0L) {
                        hordeCreator = new HordeCreator(hordeRadius, hordeCount, hordeInterval);
                    }
                }
                SoundMaker soundMaker = null;
                if (jsonMovement.has("makeSound")) {
                    JSONObject jsonMakeSound = jsonMovement.getJSONObject("makeSound");
                    int makeSoundInterval = jsonMakeSound.getInt("interval");
                    int makeSoundRadius = jsonMakeSound.getInt("radius");
                    String makeSoundMessage = jsonMakeSound.getString("message");
                    if (makeSoundInterval != 0) {
                        soundMaker = new SoundMaker(makeSoundInterval, makeSoundRadius, makeSoundMessage);
                    }
                }
                Movement movement = new Movement(id, description, spawnX, spawnY, motion, speed, type, radius, destinationX, destinationY, direction, ghost, connectDelay, disconnectDelay, reconnectDelay, teleportDelay, hordeCreator, soundMaker);
                if (movements.containsKey(id)) {
                    FakeClientManager.error(id, String.format("Client %d already exists", movement.id));
                    continue;
                }
                movements.put(id, movement);
            }
        }
        catch (Exception e) {
            FakeClientManager.error(-1, "Scenarios file load failed");
            e.printStackTrace();
        }
        finally {
            return movements;
        }
    }

    private static void error(int id, String message) {
        System.out.print(String.format("%5s : %s , [%2d] > %s\n", "ERROR", logDateFormat.format(Calendar.getInstance().getTime()), id, message));
    }

    private static void info(int id, String message) {
        if (logLevel >= 0) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "INFO", logDateFormat.format(Calendar.getInstance().getTime()), id, message));
        }
    }

    private static void log(int id, String message) {
        if (logLevel >= 1) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "LOG", logDateFormat.format(Calendar.getInstance().getTime()), id, message));
        }
    }

    private static void trace(int id, String message) {
        if (logLevel >= 2) {
            System.out.print(String.format("%5s : %s , [%2d] > %s\n", "TRACE", logDateFormat.format(Calendar.getInstance().getTime()), id, message));
        }
    }

    public static boolean isVOIPEnabled() {
        return Player.isVOIPEnabled && FakeClientManager.getOnlineID() != -1L && FakeClientManager.getConnectedGUID() != -1L;
    }

    public static long getConnectedGUID() {
        if (players.isEmpty()) {
            return -1L;
        }
        return FakeClientManager.players.iterator().next().client.connectionGuid;
    }

    public static long getOnlineID() {
        if (players.isEmpty()) {
            return -1L;
        }
        return FakeClientManager.players.iterator().next().onlineId;
    }

    static void main(String[] args2) {
        Network network;
        int port;
        String filename = null;
        int clientID = -1;
        for (int n = 0; n < args2.length; ++n) {
            if (args2[n].startsWith("-scenarios=")) {
                filename = args2[n].replace("-scenarios=", "").trim();
                continue;
            }
            if (!args2[n].startsWith("-id=")) continue;
            clientID = Integer.parseInt(args2[n].replace("-id=", "").trim());
        }
        if (filename == null || filename.isBlank()) {
            FakeClientManager.error(-1, "Invalid scenarios file name");
            System.exit(0);
        }
        RandStandard.INSTANCE.init();
        RandLua.INSTANCE.init();
        System.loadLibrary("RakNet64");
        System.loadLibrary("ZNetNoSteam64");
        try {
            String networkLogLevel = System.getProperty("zomboid.znetlog");
            if (networkLogLevel != null) {
                logLevel = Integer.parseInt(networkLogLevel);
                ZNet.init();
                ZNet.SetLogLevel(logLevel);
            }
        }
        catch (NumberFormatException ex) {
            FakeClientManager.error(-1, "Invalid log arguments");
        }
        DebugLog.setLogEnabled(DebugType.General, false);
        HashMap<Integer, Movement> movements = FakeClientManager.load(filename);
        if (Player.isVOIPEnabled) {
            FMODManager.instance.init();
            VoiceManager.instance.InitVMClient();
            VoiceManager.instance.setMode(1);
        }
        if (clientID != -1) {
            port = 17500 + clientID;
            network = new Network(movements.size(), port);
        } else {
            port = 17500;
            network = new Network(movements.size(), port);
        }
        if (network.isStarted()) {
            int connectionIndex = 0;
            if (clientID != -1) {
                Movement movement = movements.get(clientID);
                if (movement != null) {
                    players.add(new Player(movement, network, connectionIndex, port));
                } else {
                    FakeClientManager.error(clientID, "Client movement not found");
                }
            } else {
                for (Movement movement : movements.values()) {
                    players.add(new Player(movement, network, connectionIndex++, port));
                }
            }
            while (!players.isEmpty()) {
                FakeClientManager.sleep(1000L);
            }
        }
    }

    static {
        startTime = System.currentTimeMillis();
        players = new HashSet();
    }

    private static class Movement {
        static String version;
        static int defaultRadius;
        static int aimSpeed;
        static int sneakSpeed;
        static int walkSpeed;
        static int sneakRunSpeed;
        static int runSpeed;
        static int sprintSpeed;
        static int pedestrianSpeedMin;
        static int pedestrianSpeedMax;
        static int vehicleSpeedMin;
        static int vehicleSpeedMax;
        static final float zombieLungeDistanceSquared = 100.0f;
        static final float zombieWalkSpeed = 3.0f;
        static final float zombieLungeSpeed = 6.0f;
        final int id;
        final String description;
        final Vector2 spawn;
        Motion motion;
        float speed;
        final Type type;
        final int radius;
        final IsoDirections direction;
        final Vector2 destination;
        final boolean ghost;
        final long connectDelay;
        final long disconnectDelay;
        final long reconnectDelay;
        final long teleportDelay;
        final HordeCreator hordeCreator;
        SoundMaker soundMaker;
        long timestamp;

        public Movement(int id, String description, int spawnX, int spawnY, Motion motion, int speed, Type type, int radius, int destinationX, int destinationY, IsoDirections direction, boolean ghost, long connectDelay, long disconnectDelay, long reconnectDelay, long teleportDelay, HordeCreator hordeCreator, SoundMaker soundMaker) {
            this.id = id;
            this.description = description;
            this.spawn = new Vector2(spawnX, spawnY);
            this.motion = motion;
            this.speed = speed;
            this.type = type;
            this.radius = radius;
            this.direction = direction;
            this.destination = new Vector2(destinationX, destinationY);
            this.ghost = ghost;
            this.connectDelay = connectDelay;
            this.disconnectDelay = disconnectDelay;
            this.reconnectDelay = reconnectDelay;
            this.teleportDelay = teleportDelay;
            this.hordeCreator = hordeCreator;
            this.soundMaker = soundMaker;
        }

        public void connect(int playerId) {
            long currentTime = System.currentTimeMillis();
            if (this.disconnectDelay != 0L) {
                FakeClientManager.info(this.id, String.format("Player %3d connect in %.3fs, disconnect in %.3fs", playerId, Float.valueOf((float)(currentTime - this.timestamp) / 1000.0f), Float.valueOf((float)this.disconnectDelay / 1000.0f)));
            } else {
                FakeClientManager.info(this.id, String.format("Player %3d connect in %.3fs", playerId, Float.valueOf((float)(currentTime - this.timestamp) / 1000.0f)));
            }
            this.timestamp = currentTime;
        }

        public void disconnect(int playerId) {
            long currentTime = System.currentTimeMillis();
            if (this.reconnectDelay != 0L) {
                FakeClientManager.info(this.id, String.format("Player %3d disconnect in %.3fs, reconnect in %.3fs", playerId, Float.valueOf((float)(currentTime - this.timestamp) / 1000.0f), Float.valueOf((float)this.reconnectDelay / 1000.0f)));
            } else {
                FakeClientManager.info(this.id, String.format("Player %3d disconnect in %.3fs", playerId, Float.valueOf((float)(currentTime - this.timestamp) / 1000.0f)));
            }
            this.timestamp = currentTime;
        }

        public boolean doTeleport() {
            return this.teleportDelay != 0L;
        }

        public boolean doDisconnect() {
            return this.disconnectDelay != 0L;
        }

        public boolean checkDisconnect() {
            return System.currentTimeMillis() - this.timestamp > this.disconnectDelay;
        }

        public boolean doReconnect() {
            return this.reconnectDelay != 0L;
        }

        public boolean checkReconnect() {
            return System.currentTimeMillis() - this.timestamp > this.reconnectDelay;
        }

        static {
            defaultRadius = 150;
            aimSpeed = 4;
            sneakSpeed = 6;
            walkSpeed = 7;
            sneakRunSpeed = 10;
            runSpeed = 13;
            sprintSpeed = 19;
            pedestrianSpeedMin = 5;
            pedestrianSpeedMax = 20;
            vehicleSpeedMin = 40;
            vehicleSpeedMax = 80;
        }

        private static enum Motion {
            Aim,
            Sneak,
            Walk,
            SneakRun,
            Run,
            Sprint,
            Pedestrian,
            Vehicle;

        }

        private static enum Type {
            Stay,
            Line,
            Circle,
            AIAttackZombies,
            AIRunAwayFromZombies,
            AIRunToAnotherPlayers,
            AINormal;

        }
    }

    private static class Client {
        private static String connectionServerHost = "127.0.0.1";
        private static long connectionInterval = 1500L;
        private static long connectionTimeout = 10000L;
        private static long connectionDelay = 15000L;
        private static int statisticsClientID = -1;
        private static int statisticsPeriod = 1;
        private static long serverTimeShift;
        private static boolean serverTimeShiftIsSet;
        private final HashMap<Integer, Request> requests = new HashMap();
        private final Player player;
        private final Network network;
        private final int connectionIndex;
        private final int port;
        private long connectionGuid = -1L;
        private int requestId;
        private long stateTime;
        private State state;
        private String host;
        public static String luaChecksum;
        public static String scriptChecksum;

        private Client(Player player, Network network, int connectionIndex, int port) {
            this.connectionIndex = connectionIndex;
            this.network = network;
            this.player = player;
            this.port = port;
            try {
                this.host = InetAddress.getByName(connectionServerHost).getHostAddress();
                this.state = State.CONNECT;
                Thread thread2 = new Thread(ThreadGroups.Workers, this::updateThread, this.player.username);
                thread2.setDaemon(true);
                thread2.start();
            }
            catch (UnknownHostException e) {
                this.state = State.QUIT;
                e.printStackTrace();
            }
        }

        private void updateThread() {
            FakeClientManager.info(this.player.movement.id, String.format("Start client (%d) %s:%d => %s:%d / \"%s\"", this.connectionIndex, FakeClientManager.CLIENT_ADDRESS, this.port, this.host, 16261, this.player.movement.description));
            FakeClientManager.sleep(this.player.movement.connectDelay);
            switch (this.player.movement.type.ordinal()) {
                case 2: {
                    this.player.circleMovement();
                    break;
                }
                case 1: {
                    this.player.lineMovement();
                    break;
                }
                case 3: {
                    this.player.aiAttackZombiesMovement();
                    break;
                }
                case 4: {
                    this.player.aiRunAwayFromZombiesMovement();
                    break;
                }
                case 5: {
                    this.player.aiRunToAnotherPlayersMovement();
                    break;
                }
                case 6: {
                    this.player.aiNormalMovement();
                }
            }
            while (this.state != State.QUIT) {
                this.update();
                FakeClientManager.sleep(1L);
            }
            FakeClientManager.info(this.player.movement.id, String.format("Stop client (%d) %s:%d => %s:%d / \"%s\"", this.connectionIndex, FakeClientManager.CLIENT_ADDRESS, this.port, this.host, 16261, this.player.movement.description));
        }

        private void updateTime() {
            this.stateTime = System.currentTimeMillis();
        }

        private long getServerTime() {
            return serverTimeShiftIsSet ? System.nanoTime() + serverTimeShift : 0L;
        }

        private boolean checkConnectionTimeout() {
            return System.currentTimeMillis() - this.stateTime > connectionTimeout;
        }

        private boolean checkConnectionDelay() {
            return System.currentTimeMillis() - this.stateTime > connectionDelay;
        }

        private void changeState(State newState) {
            this.updateTime();
            FakeClientManager.log(this.player.movement.id, String.format("%s >> %s", new Object[]{this.state, newState}));
            if (State.RUN == newState) {
                this.player.movement.connect(this.player.onlineId);
                if (this.player.teleportLimiter == null) {
                    this.player.teleportLimiter = new UpdateLimit(this.player.movement.teleportDelay);
                }
                if (this.player.movement.id == statisticsClientID) {
                    this.sendTimeSync();
                    this.sendInjuries();
                }
            } else if (State.DISCONNECT == newState && State.DISCONNECT != this.state) {
                this.player.movement.disconnect(this.player.onlineId);
            }
            this.state = newState;
        }

        private void update() {
            switch (this.state.ordinal()) {
                case 0: {
                    this.player.movement.timestamp = System.currentTimeMillis();
                    this.network.connect(this.player.movement.id, this.host);
                    this.changeState(State.WAIT);
                    break;
                }
                case 1: {
                    this.sendPlayerLogin();
                    this.sendPlayerLoginRequest();
                    this.changeState(State.WAIT);
                    break;
                }
                case 3: {
                    this.sendPlayerConnect();
                    this.changeState(State.WAIT);
                    break;
                }
                case 2: {
                    this.sendChecksum();
                    this.changeState(State.WAIT);
                    break;
                }
                case 4: {
                    this.sendPlayerExtraInfo(this.player.movement.ghost, this.player.movement.hordeCreator != null || Player.isVOIPEnabled);
                    this.sendEquip();
                    this.changeState(State.WAIT);
                    break;
                }
                case 5: {
                    this.requestId = 0;
                    this.requests.clear();
                    this.requestFullUpdate();
                    this.requestLargeAreaZip();
                    this.changeState(State.WAIT);
                    break;
                }
                case 6: {
                    if (this.player.movement.doDisconnect() && this.player.movement.checkDisconnect()) {
                        this.changeState(State.DISCONNECT);
                        break;
                    }
                    this.player.run();
                    break;
                }
                case 7: {
                    if (!this.checkConnectionTimeout()) break;
                    this.changeState(State.DISCONNECT);
                    break;
                }
                case 8: {
                    if (this.network.isConnected()) {
                        this.player.movement.timestamp = System.currentTimeMillis();
                        this.network.disconnect(this.connectionGuid, this.player.movement.id, this.host);
                    }
                    if ((!this.player.movement.doReconnect() || !this.player.movement.checkReconnect()) && (this.player.movement.doReconnect() || !this.checkConnectionDelay())) break;
                    this.changeState(State.CONNECT);
                    break;
                }
            }
        }

        private void receive(short type, ByteBufferReader bb) {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(type);
            Network.logUserPacket(this.player.movement.id, type);
            switch (packetType) {
                case PlayerConnect: {
                    if (!this.receivePlayerConnect(bb)) break;
                    if (luaChecksum.isEmpty()) {
                        this.changeState(State.PLAYER_EXTRA_INFO);
                        break;
                    }
                    this.changeState(State.CHECKSUM);
                    break;
                }
                case LoginQueueRequest: {
                    if (!this.receiveLoginQueueRequest(bb)) break;
                    this.sendLoginQueueDone();
                    this.changeState(State.PLAYER_CONNECT);
                    break;
                }
                case ExtraInfo: {
                    if (!this.receivePlayerExtraInfo(bb)) break;
                    this.changeState(State.RUN);
                    break;
                }
                case SentChunk: {
                    if (this.state != State.WAIT || !this.receiveChunkPart(bb)) break;
                    this.updateTime();
                    if (!this.allChunkPartsReceived()) break;
                    this.changeState(State.PLAYER_CONNECT);
                    break;
                }
                case NotRequiredInZip: {
                    if (this.state != State.WAIT || !this.receiveNotRequired(bb)) break;
                    this.updateTime();
                    if (!this.allChunkPartsReceived()) break;
                    this.changeState(State.PLAYER_CONNECT);
                    break;
                }
                case PlayerHitSquare: 
                case PlayerHitVehicle: 
                case PlayerHitZombie: 
                case PlayerHitPlayer: 
                case PlayerHitAnimal: 
                case ZombieHitPlayer: 
                case AnimalHitPlayer: 
                case AnimalHitAnimal: 
                case AnimalHitThumpable: 
                case VehicleHitZombie: 
                case VehicleHitPlayer: {
                    break;
                }
                case TimeSync: {
                    this.receiveTimeSync(bb);
                    break;
                }
                case SyncClock: {
                    this.receiveSyncClock(bb);
                    break;
                }
                case ZombieSynchronizationUnreliable: 
                case ZombieSynchronizationReliable: {
                    this.receiveZombieSimulation(bb);
                    break;
                }
                case PlayerUpdateUnreliable: 
                case PlayerUpdateReliable: {
                    this.player.playerManager.parsePlayer(bb);
                    break;
                }
                case PlayerTimeout: {
                    this.player.playerManager.parsePlayerTimeout(bb);
                    break;
                }
                case Kicked: {
                    this.receiveKicked(bb);
                    break;
                }
                case Checksum: {
                    this.receiveChecksum(bb);
                    break;
                }
                case Teleport: {
                    this.receiveTeleport(bb);
                    break;
                }
            }
            bb.clear();
        }

        private void doPacket(short type, ByteBufferWriter bb) {
            bb.putByte(134);
            bb.putShort(type);
        }

        private void sendPlayerLogin() {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.Login.getId(), bb);
            bb.putUTF(this.player.username);
            bb.putUTF(this.player.username);
            bb.putUTF(versionNumber);
            bb.putInt(1);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendPlayerLoginRequest() {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.LoginQueueRequest.getId(), bb);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendLoginQueueDone() {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.LoginQueueDone.getId(), bb);
            bb.putLong(100L);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendPlayerConnect() {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerConnect.getId(), bb);
            this.writePlayerConnectData(bb);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void writePlayerConnectData(ByteBufferWriter b) {
            b.putByte(0);
            b.putByte(13);
            b.putFloat(this.player.x);
            b.putFloat(this.player.y);
            b.putFloat(this.player.z);
            b.putInt(0);
            b.putUTF(this.player.username);
            b.putUTF(this.player.username);
            b.putUTF(this.player.isFemale == 0 ? "Kate" : "Male");
            b.putInt(this.player.isFemale);
            b.putUTF("fireofficer");
            b.putInt(0);
            b.putInt(4);
            b.putUTF("Sprinting");
            b.putInt(1);
            b.putUTF("Fitness");
            b.putInt(6);
            b.putUTF("Strength");
            b.putInt(6);
            b.putUTF("Axe");
            b.putInt(1);
            b.putByte(0);
            b.putByte(0);
            b.putByte(Math.round(Math.random() * 5.0));
            b.putByte(0);
            b.putByte(0);
            b.putByte(0);
            b.putByte(0);
            int items = this.player.clothes.size();
            b.putByte(items);
            for (Player.Clothes clothes : this.player.clothes) {
                b.putByte(clothes.flags);
                b.putUTF("Base." + clothes.name);
                b.putUTF(null);
                b.putUTF(clothes.name);
                b.putByte(-1);
                b.putByte(-1);
                b.putByte(-1);
                b.putByte(clothes.text);
                b.putFloat(0.0f);
                b.putByte(0);
                b.putByte(0);
                b.putByte(0);
                b.putByte(0);
                b.putByte(0);
                b.putByte(0);
            }
            b.putUTF("fake_str");
            b.putShort(0);
            b.putInt(2);
            b.putUTF("Fit");
            b.putUTF("Stout");
            b.putFloat(0.0f);
            b.putInt(0);
            b.putInt(0);
            b.putInt(4);
            b.putUTF("Sprinting");
            b.putFloat(75.0f);
            b.putUTF("Fitness");
            b.putFloat(67500.0f);
            b.putUTF("Strength");
            b.putFloat(67500.0f);
            b.putUTF("Axe");
            b.putFloat(75.0f);
            b.putInt(4);
            b.putUTF("Sprinting");
            b.putInt(1);
            b.putUTF("Fitness");
            b.putInt(6);
            b.putUTF("Strength");
            b.putInt(6);
            b.putUTF("Axe");
            b.putInt(1);
            b.putInt(0);
            b.putBoolean(true);
            b.putUTF("fake");
            b.putFloat(this.player.tagColor.r);
            b.putFloat(this.player.tagColor.g);
            b.putFloat(this.player.tagColor.b);
            b.putInt(0);
            b.putDouble(0.0);
            b.putInt(0);
            b.putUTF(this.player.username);
            b.putFloat(this.player.speakColor.r);
            b.putFloat(this.player.speakColor.g);
            b.putFloat(this.player.speakColor.b);
            b.putBoolean(true);
            b.putBoolean(false);
            b.putByte(0);
            b.putByte(0);
            b.putInt(0);
            b.putInt(0);
        }

        @Deprecated
        private void sendPlayerExtraInfo(boolean ghost, boolean admin) {
        }

        private void sendSyncRadioData() {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.SyncRadioData.getId(), b);
            b.putBoolean(Player.isVOIPEnabled);
            b.putInt(4);
            b.putInt(0);
            b.putInt((int)RakVoice.GetMaxDistance());
            b.putInt((int)this.player.x);
            b.putInt((int)this.player.y);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendEquip() {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.Equip.getId(), bb);
            bb.putByte(0);
            bb.putByte(0);
            bb.putByte(1);
            bb.putInt(16);
            Objects.requireNonNull(this.player);
            bb.putShort((short)1202);
            bb.putByte(1);
            Objects.requireNonNull(this.player);
            bb.putInt(837602032);
            bb.putByte(0);
            bb.putInt(0);
            bb.putInt(0);
            bb.putByte(0);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendChatMessage(String message) {
            ByteBufferWriter bb = this.network.startPacket();
            bb.putShort(this.player.onlineId);
            bb.putInt(2);
            bb.putUTF(this.player.username);
            bb.putUTF(message);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private short getBooleanVariables() {
            short booleanVariables = 0;
            if (this.player.movement.speed > 0.0f) {
                switch (this.player.movement.motion.ordinal()) {
                    case 0: {
                        booleanVariables = (short)(booleanVariables | 0x40);
                        break;
                    }
                    case 1: {
                        booleanVariables = (short)(booleanVariables | 1);
                        break;
                    }
                    case 3: {
                        booleanVariables = (short)(booleanVariables | 0x11);
                        break;
                    }
                    case 4: {
                        booleanVariables = (short)(booleanVariables | 0x10);
                        break;
                    }
                    case 5: {
                        booleanVariables = (short)(booleanVariables | 0x20);
                    }
                }
                booleanVariables = (short)(booleanVariables | 0x4400);
            }
            return booleanVariables;
        }

        private void sendPlayer(NetworkCharacter.Transform transform, int t, Vector2 direction) {
            PlayerPacket packet = new PlayerPacket();
            packet.prediction.x = transform.position.x;
            packet.prediction.y = transform.position.y;
            packet.prediction.z = (byte)this.player.z;
            packet.prediction.direction = direction.getDirection();
            packet.prediction.type = 0;
            packet.booleanVariables = this.getBooleanVariables();
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerUpdateReliable.getId(), b);
            ByteBufferWriter bw = new ByteBufferWriter(b.bb);
            packet.write(bw);
            this.network.endPacket(this.connectionGuid);
        }

        private boolean receivePlayerConnect(ByteBufferReader bb) {
            short id = bb.getShort();
            if (id == -1) {
                byte playerIndex = bb.getByte();
                this.player.onlineId = id = bb.getShort();
                return true;
            }
            return false;
        }

        private boolean receiveLoginQueueRequest(ByteBufferReader bb) {
            return bb.getBoolean();
        }

        private boolean receivePlayerExtraInfo(ByteBufferReader bb) {
            short id = bb.getShort();
            return id == this.player.onlineId;
        }

        private boolean receiveChunkPart(ByteBufferReader bb) {
            boolean result = false;
            int requestNumber = bb.getInt();
            int numChunks = bb.getInt();
            int chunkIndex = bb.getInt();
            int fileSize = bb.getInt();
            int offset = bb.getInt();
            int count = bb.getInt();
            if (this.requests.remove(requestNumber) != null) {
                result = true;
            }
            return result;
        }

        private boolean receiveNotRequired(ByteBufferReader bb) {
            boolean result = false;
            int count = bb.getInt();
            for (int n = 0; n < count; ++n) {
                int requestNumber = bb.getInt();
                boolean sameOnServer = bb.getBoolean();
                if (this.requests.remove(requestNumber) == null) continue;
                result = true;
            }
            return result;
        }

        private boolean allChunkPartsReceived() {
            return this.requests.isEmpty();
        }

        private void addChunkRequest(int wx, int wy, int x, int y) {
            Request request = new Request(wx, wy, this.requestId);
            ++this.requestId;
            this.requests.put(request.id, request);
        }

        private void requestZipList() {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.RequestZipList.getId(), b);
            b.putInt(this.requests.size());
            for (Request request : this.requests.values()) {
                b.putInt(request.id);
                b.putInt(request.wx);
                b.putInt(request.wy);
                b.putLong(request.crc);
            }
            this.network.endPacket(this.connectionGuid);
        }

        private void requestLargeAreaZip() {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.RequestLargeAreaZip.getId(), b);
            b.putInt(this.player.worldX);
            b.putInt(this.player.worldY);
            b.putInt(13);
            this.network.endPacketImmediate(this.connectionGuid);
            int minX = this.player.worldX - 6 + 2;
            int minY = this.player.worldY - 6 + 2;
            int maxX = this.player.worldX + 6 + 2;
            int maxY = this.player.worldY + 6 + 2;
            for (int y = minY; y <= maxY; ++y) {
                for (int x = minX; x <= maxX; ++x) {
                    Request request = new Request(x, y, this.requestId);
                    ++this.requestId;
                    this.requests.put(request.id, request);
                }
            }
            this.requestZipList();
        }

        private void requestFullUpdate() {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.IsoRegionClientRequestFullUpdate.getId(), b);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void requestChunkObjectState() {
            for (Request request : this.requests.values()) {
                ByteBufferWriter b = this.network.startPacket();
                this.doPacket(PacketTypes.PacketType.ChunkObjectState.getId(), b);
                b.putShort(request.wx);
                b.putShort(request.wy);
                this.network.endPacket(this.connectionGuid);
            }
        }

        private void requestChunks() {
            if (!this.requests.isEmpty()) {
                this.requestZipList();
                this.requestChunkObjectState();
                this.requests.clear();
            }
        }

        private void receiveStatistics(ByteBufferReader bb) {
            long min = bb.getLong();
            long max = bb.getLong();
            long avg = bb.getLong();
            long fps = bb.getLong();
            long tps = bb.getLong();
            long con = bb.getLong();
            long c1 = bb.getLong();
            long c2 = bb.getLong();
            long c3 = bb.getLong();
            FakeClientManager.info(this.player.movement.id, String.format("ServerStats: con=[%2d] fps=[%2d] tps=[%2d] upt=[%4d-%4d/%4d], c1=[%d] c2=[%d] c3=[%d]", con, fps, tps, min, max, avg, c1, c2, c3));
        }

        private void sendTimeSync() {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.TimeSync.getId(), b);
            long timeNsClient = System.nanoTime();
            b.putLong(timeNsClient);
            b.putLong(0L);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void receiveTimeSync(ByteBufferReader bb) {
            long timeClientSend = bb.getLong();
            long timeServer = bb.getLong();
            long timeClientReceive = System.nanoTime();
            long localPing = timeClientReceive - timeClientSend;
            long localServerTimeShift = timeServer - timeClientReceive + localPing / 2L;
            long serverTimeShiftLast = serverTimeShift;
            serverTimeShift = !serverTimeShiftIsSet ? localServerTimeShift : (long)((float)serverTimeShift + (float)(localServerTimeShift - serverTimeShift) * 0.05f);
            long serverTimeuQality = 10000000L;
            if (Math.abs(serverTimeShift - serverTimeShiftLast) > 10000000L) {
                this.sendTimeSync();
            } else {
                serverTimeShiftIsSet = true;
            }
        }

        private void receiveSyncClock(ByteBufferReader bb) {
            FakeClientManager.trace(this.player.movement.id, String.format("Player %3d sync clock", this.player.onlineId));
        }

        private void receiveKicked(ByteBufferReader bb) {
            String chat = bb.getUTF();
            FakeClientManager.info(this.player.movement.id, String.format("Client kicked. Reason: %s", chat));
        }

        private void receiveChecksum(ByteBufferReader bb) {
            FakeClientManager.trace(this.player.movement.id, String.format("Player %3d receive Checksum", this.player.onlineId));
            short checksum = bb.getShort();
            boolean isLuaOk = bb.getBoolean();
            boolean isScriptOk = bb.getBoolean();
            if (checksum != 1 || !isLuaOk || !isScriptOk) {
                FakeClientManager.info(this.player.movement.id, String.format("checksum lua: %b, script: %b", isLuaOk, isScriptOk));
            }
            this.changeState(State.PLAYER_EXTRA_INFO);
        }

        private void receiveTeleport(ByteBufferReader bb) {
            byte playerIndex = bb.getByte();
            float x = bb.getFloat();
            float y = bb.getFloat();
            float z = bb.getFloat();
            FakeClientManager.info(this.player.movement.id, String.format("Player %3d teleport to (%d, %d)", this.player.onlineId, (int)x, (int)y));
            this.player.x = x;
            this.player.y = y;
        }

        private void receiveZombieSimulation(ByteBufferReader bb) {
            this.player.simulator.clear();
            boolean isNeighborPlayer = bb.getBoolean();
            short numDeletingZombies = bb.getShort();
            for (short n = 0; n < numDeletingZombies; n = (short)(n + 1)) {
                short zombieId = bb.getShort();
                Zombie zombie = this.player.simulator.zombies.get(zombieId);
                this.player.simulator.zombies4Delete.add(zombie);
            }
            short numAuths = bb.getShort();
            for (short n = 0; n < numAuths; n = (short)(n + 1)) {
                short zombieId = bb.getShort();
                this.player.simulator.add(zombieId);
            }
            this.player.simulator.receivePacket(bb);
            this.player.simulator.process();
        }

        private void sendInjuries() {
            PlayerInjuriesPacket packet = new PlayerInjuriesPacket();
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.PlayerInjuries.getId(), b);
            ByteBufferWriter bw = new ByteBufferWriter(b.bb);
            packet.write(bw);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendChecksum() {
            if (luaChecksum.isEmpty()) {
                return;
            }
            FakeClientManager.trace(this.player.movement.id, String.format("Player %3d sendChecksum", this.player.onlineId));
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.Checksum.getId(), b);
            b.putShort(1);
            b.putUTF(luaChecksum);
            b.putUTF(scriptChecksum);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        public void sendCommand(String command) {
            ByteBufferWriter b = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.ReceiveCommand.getId(), b);
            b.putUTF(command);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        private void sendEventPacket(short id, int x, int y, int z, byte eventID, String type1) {
        }

        private void sendWorldSound4Player(int x, int y, int z, int radius, int volume) {
            ByteBufferWriter bb = this.network.startPacket();
            this.doPacket(PacketTypes.PacketType.WorldSoundPacket.getId(), bb);
            bb.putInt(x);
            bb.putInt(y);
            bb.putInt(z);
            bb.putInt(radius);
            bb.putInt(volume);
            bb.putByte(0);
            bb.putFloat(0.0f);
            bb.putFloat(1.0f);
            bb.putByte(0);
            bb.putByte(0);
            this.network.endPacketImmediate(this.connectionGuid);
        }

        static {
            luaChecksum = "";
            scriptChecksum = "";
        }

        private static enum State {
            CONNECT,
            LOGIN,
            CHECKSUM,
            PLAYER_CONNECT,
            PLAYER_EXTRA_INFO,
            LOAD,
            RUN,
            WAIT,
            DISCONNECT,
            QUIT;

        }

        private static final class Request {
            private final int id;
            private final int wx;
            private final int wy;
            private final long crc;

            private Request(int wx, int wy, int requestId) {
                this.id = requestId;
                this.wx = wx;
                this.wy = wy;
                CRC32 crc32 = new CRC32();
                crc32.reset();
                crc32.update(String.format("map_%d_%d.bin", wx, wy).getBytes());
                this.crc = crc32.getValue();
            }
        }
    }

    private static class ZombieSimulator {
        public static Behaviour behaviour = Behaviour.Stay;
        public static int deleteZombieDistanceSquared = 10000;
        public static int forgotZombieDistanceSquared = 225;
        public static int canSeeZombieDistanceSquared = 100;
        public static int seeZombieDistanceSquared = 25;
        private static boolean canChangeTarget = true;
        private static final int updatePeriod = 100;
        private static final int attackPeriod = 1000;
        public static int maxZombiesPerUpdate = 300;
        private final ByteBuffer bb = ByteBuffer.allocate(1000000);
        private final UpdateLimit updateLimiter = new UpdateLimit(100L);
        private final UpdateLimit attackLimiter = new UpdateLimit(1000L);
        private final Player player;
        private final ZombiePacket zombiePacket = new ZombiePacket();
        private HashSet<Short> authoriseZombiesCurrent = new HashSet();
        private HashSet<Short> authoriseZombiesLast = new HashSet();
        private final ArrayList<Short> unknownZombies = new ArrayList();
        private final HashMap<Integer, Zombie> zombies = new HashMap();
        private final ArrayDeque<Zombie> zombies4Add = new ArrayDeque();
        private final ArrayDeque<Zombie> zombies4Delete = new ArrayDeque();
        private final HashSet<Short> authoriseZombies = new HashSet();
        private final ArrayDeque<Zombie> sendQueue = new ArrayDeque();
        private static final Vector2 tmpDir = new Vector2();

        public ZombieSimulator(Player player) {
            this.player = player;
        }

        public void becomeLocal(Zombie z) {
            z.localOwnership = true;
        }

        public void becomeRemote(Zombie z) {
            z.localOwnership = false;
        }

        public void clear() {
            HashSet<Short> temp = this.authoriseZombiesCurrent;
            this.authoriseZombiesCurrent = this.authoriseZombiesLast;
            this.authoriseZombiesLast = temp;
            this.authoriseZombiesLast.removeIf(zombieId -> this.zombies.get(zombieId) == null);
            this.authoriseZombiesCurrent.clear();
        }

        public void add(short onlineId) {
            this.authoriseZombiesCurrent.add(onlineId);
        }

        public void receivePacket(ByteBufferReader b) {
            short num = b.getShort();
            for (short n = 0; n < num; n = (short)(n + 1)) {
                this.parseZombie(b);
            }
        }

        private void parseZombie(ByteBufferReader b) {
            ZombiePacket packet = this.zombiePacket;
            packet.parse(b, null);
            Zombie z = this.zombies.get(packet.id);
            if (this.authoriseZombies.contains(packet.id) && z != null) {
                return;
            }
            if (z == null) {
                z = new Zombie(packet.id);
                this.zombies4Add.add(z);
                FakeClientManager.trace(this.player.movement.id, String.format("New zombie %s", z.onlineId));
            }
            z.lastUpdate = System.currentTimeMillis();
            z.zombiePacket.copy(packet);
            z.x = packet.realX;
            z.y = packet.realY;
            z.z = packet.realZ;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void process() {
            Sets.SetView<Short> newZombies = Sets.difference(this.authoriseZombiesCurrent, this.authoriseZombiesLast);
            for (Short zombieId : newZombies) {
                Zombie z = this.zombies.get(zombieId);
                if (z != null) {
                    this.becomeLocal(z);
                    continue;
                }
                if (this.unknownZombies.contains(zombieId)) continue;
                this.unknownZombies.add(zombieId);
            }
            Sets.SetView<Short> removeZombies = Sets.difference(this.authoriseZombiesLast, this.authoriseZombiesCurrent);
            for (Short zombieId : removeZombies) {
                Zombie z = this.zombies.get(zombieId);
                if (z == null) continue;
                this.becomeRemote(z);
            }
            HashSet<Short> hashSet = this.authoriseZombies;
            synchronized (hashSet) {
                this.authoriseZombies.clear();
                this.authoriseZombies.addAll(this.authoriseZombiesCurrent);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public void send() {
            Zombie z;
            if (this.authoriseZombies.isEmpty() && this.unknownZombies.isEmpty()) {
                return;
            }
            if (this.sendQueue.isEmpty()) {
                HashSet<Short> hashSet = this.authoriseZombies;
                synchronized (hashSet) {
                    for (Short zombieId : this.authoriseZombies) {
                        z = this.zombies.get(zombieId);
                        if (z == null || z.onlineId == -1) continue;
                        this.sendQueue.add(z);
                    }
                }
            }
            this.bb.clear();
            this.bb.putShort((short)0);
            int unknownZombiesCount = this.unknownZombies.size();
            this.bb.putShort((short)unknownZombiesCount);
            for (int k = 0; k < this.unknownZombies.size(); ++k) {
                if (this.unknownZombies.get(k) == null) {
                    return;
                }
                this.bb.putShort(this.unknownZombies.get(k));
            }
            this.unknownZombies.clear();
            int position = this.bb.position();
            this.bb.putShort((short)maxZombiesPerUpdate);
            int realCount = 0;
            while (!this.sendQueue.isEmpty()) {
                z = this.sendQueue.poll();
                if (z.onlineId == -1) continue;
                z.zombiePacket.write(this.bb);
                if (++realCount < maxZombiesPerUpdate) continue;
                break;
            }
            if (realCount < maxZombiesPerUpdate) {
                int endposition = this.bb.position();
                this.bb.position(position);
                this.bb.putShort((short)realCount);
                this.bb.position(endposition);
            }
            if (realCount > 0 || unknownZombiesCount > 0) {
                ByteBufferWriter bb2 = this.player.client.network.startPacket();
                this.player.client.doPacket(PacketTypes.PacketType.ZombieSimulationReliable.getId(), bb2);
                bb2.put(this.bb.array(), 0, this.bb.position());
                this.player.client.network.endPacketSuperHighUnreliable(this.player.client.connectionGuid);
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void simulate(Integer id, Zombie zombie) {
            float distanceSquared = IsoUtils.DistanceToSquared(this.player.x, this.player.y, zombie.x, zombie.y);
            if (distanceSquared > (float)deleteZombieDistanceSquared || !zombie.localOwnership && zombie.lastUpdate + 5000L < System.currentTimeMillis()) {
                this.zombies4Delete.add(zombie);
                return;
            }
            tmpDir.set(-zombie.x + this.player.x, -zombie.y + this.player.y);
            if (zombie.isMoving) {
                float a = 0.2f;
                zombie.x = PZMath.lerp(zombie.x, zombie.zombiePacket.x, 0.2f);
                zombie.y = PZMath.lerp(zombie.y, zombie.zombiePacket.y, 0.2f);
                zombie.z = 0.0f;
                zombie.dir = IsoDirections.fromAngle(tmpDir);
            }
            if (canChangeTarget) {
                HashMap<Integer, PlayerManager.RemotePlayer> a = this.player.playerManager.players;
                synchronized (a) {
                    for (PlayerManager.RemotePlayer remotePlayer : this.player.playerManager.players.values()) {
                        float remotePlayerDistanceSquared = IsoUtils.DistanceToSquared(remotePlayer.x, remotePlayer.y, zombie.x, zombie.y);
                        if (!(remotePlayerDistanceSquared < (float)seeZombieDistanceSquared)) continue;
                        zombie.zombiePacket.target = remotePlayer.onlineId;
                        break;
                    }
                }
            } else {
                zombie.zombiePacket.target = this.player.onlineId;
            }
            if (behaviour == Behaviour.Stay) {
                zombie.isMoving = false;
            } else if (behaviour == Behaviour.Normal) {
                if (distanceSquared > (float)forgotZombieDistanceSquared) {
                    zombie.isMoving = false;
                }
                if (distanceSquared < (float)canSeeZombieDistanceSquared && (Rand.Next(100) < 1 || zombie.dir == IsoDirections.fromAngle(tmpDir))) {
                    zombie.isMoving = true;
                }
                if (distanceSquared < (float)seeZombieDistanceSquared) {
                    zombie.isMoving = true;
                }
            } else {
                zombie.isMoving = true;
            }
            if (zombie.isMoving) {
                Vector2 chrDir = zombie.dir.ToVector();
                float speed = distanceSquared < 100.0f ? 6.0f : 3.0f;
                long dt = System.currentTimeMillis() - zombie.lastUpdate;
                zombie.zombiePacket.x = zombie.x + chrDir.x * (float)dt * 0.001f * speed;
                zombie.zombiePacket.y = zombie.y + chrDir.y * (float)dt * 0.001f * speed;
                zombie.zombiePacket.z = (byte)zombie.z;
                zombie.zombiePacket.predictionType = 1;
            } else {
                zombie.zombiePacket.x = zombie.x;
                zombie.zombiePacket.y = zombie.y;
                zombie.zombiePacket.z = (byte)zombie.z;
                zombie.zombiePacket.predictionType = 0;
            }
            zombie.zombiePacket.booleanVariables = 0;
            if (distanceSquared < 100.0f) {
                zombie.zombiePacket.booleanVariables = (short)(zombie.zombiePacket.booleanVariables | 2);
            }
            zombie.zombiePacket.timeSinceSeenFlesh = (short)(zombie.isMoving ? 0 : Short.MAX_VALUE);
            zombie.zombiePacket.smParamTargetAngle = 0;
            zombie.zombiePacket.speedMod = (short)1000;
            zombie.zombiePacket.walkType = NetworkVariables.WalkType.values()[zombie.walkType];
            zombie.zombiePacket.realX = zombie.x;
            zombie.zombiePacket.realY = zombie.y;
            zombie.zombiePacket.realZ = (byte)zombie.z;
            zombie.zombiePacket.health = (byte)(zombie.health * 100.0f);
            zombie.zombiePacket.realState = NetworkVariables.ZombieState.fromString("fakezombie-" + behaviour.toString().toLowerCase());
            if (zombie.isMoving) {
                zombie.zombiePacket.pfb.goal = PathFindBehavior2.Goal.Character;
                zombie.zombiePacket.pfb.target.setID(this.player.onlineId);
            } else {
                zombie.zombiePacket.pfb.goal = PathFindBehavior2.Goal.None;
            }
            if (distanceSquared < 2.0f && this.attackLimiter.Check()) {
                zombie.health -= Player.damage;
                this.sendHitCharacter(zombie, Player.damage);
                if (zombie.health <= 0.0f) {
                    this.player.client.sendChatMessage("DIE!!");
                    this.zombies4Delete.add(zombie);
                }
            }
            zombie.lastUpdate = System.currentTimeMillis();
        }

        private void writeHitInfoToZombie(ByteBufferWriter bb, short zombieId, float x, float y, float damage) {
            bb.putByte(2);
            bb.putShort(zombieId);
            bb.putByte(0);
            bb.putFloat(x);
            bb.putFloat(y);
            bb.putFloat(0.0f);
            bb.putFloat(damage);
            bb.putFloat(1.0f);
            bb.putInt(100);
        }

        private void sendHitCharacter(Zombie zombie, float damage) {
            int i;
            boolean isCritical = false;
            ByteBufferWriter bb = this.player.client.network.startPacket();
            this.player.client.doPacket(PacketTypes.PacketType.PlayerHitZombie.getId(), bb);
            bb.putByte(3);
            bb.putShort(this.player.onlineId);
            bb.putShort(0);
            bb.putFloat(this.player.x);
            bb.putFloat(this.player.y);
            bb.putFloat(this.player.z);
            bb.putFloat(this.player.direction.x);
            bb.putFloat(this.player.direction.y);
            bb.putUTF("");
            bb.putUTF("");
            bb.putUTF("");
            Objects.requireNonNull(this.player);
            bb.putShort(0 + 0);
            bb.putFloat(1.0f);
            bb.putFloat(1.0f);
            bb.putFloat(1.0f);
            bb.putUTF("default");
            byte flags = 0;
            Objects.requireNonNull(this.player);
            flags = (byte)(flags | (byte)0);
            bb.putByte(flags);
            bb.putByte(0);
            bb.putShort(0);
            bb.putFloat(1.0f);
            bb.putInt(0);
            int count = 1;
            bb.putByte(count);
            for (i = 0; i < count; ++i) {
                this.writeHitInfoToZombie(bb, zombie.onlineId, zombie.x, zombie.y, damage);
            }
            count = 0;
            bb.putByte(count);
            count = 1;
            bb.putByte(count);
            for (i = 0; i < count; ++i) {
                this.writeHitInfoToZombie(bb, zombie.onlineId, zombie.x, zombie.y, damage);
            }
            Objects.requireNonNull(this.player);
            bb.putByte(0);
            bb.putShort(zombie.onlineId);
            bb.putShort(damage >= zombie.health ? 3 : 0);
            bb.putFloat(zombie.x);
            bb.putFloat(zombie.y);
            bb.putFloat(zombie.z);
            bb.putFloat(zombie.dir.ToVector().x);
            bb.putFloat(zombie.dir.ToVector().y);
            bb.putUTF("");
            bb.putUTF("");
            bb.putUTF("");
            bb.putShort(0);
            bb.putUTF("");
            bb.putUTF("FRONT");
            bb.putByte(0);
            bb.putFloat(damage);
            bb.putFloat(1.0f);
            bb.putFloat(this.player.direction.x);
            bb.putFloat(this.player.direction.y);
            bb.putFloat(1.0f);
            bb.putByte(0);
            if (tmpDir.getLength() > 0.0f) {
                zombie.dropPositionX = zombie.x + ZombieSimulator.tmpDir.x / tmpDir.getLength();
                zombie.dropPositionY = zombie.y + ZombieSimulator.tmpDir.y / tmpDir.getLength();
            } else {
                zombie.dropPositionX = zombie.x;
                zombie.dropPositionY = zombie.y;
            }
            bb.putFloat(zombie.dropPositionX);
            bb.putFloat(zombie.dropPositionY);
            bb.putByte((byte)zombie.z);
            bb.putFloat(zombie.dir.toAngle());
            this.player.client.network.endPacketImmediate(this.player.client.connectionGuid);
        }

        private void sendSendDeadZombie(Zombie zombie) {
            ByteBufferWriter bb = this.player.client.network.startPacket();
            this.player.client.doPacket(PacketTypes.PacketType.ZombieDeath.getId(), bb);
            bb.putShort(zombie.onlineId);
            bb.putFloat(zombie.x);
            bb.putFloat(zombie.y);
            bb.putFloat(zombie.z);
            bb.putFloat(zombie.dir.toAngle());
            bb.putByte(zombie.dir.ordinal());
            bb.putByte(0);
            bb.putByte(0);
            bb.putByte(0);
            this.player.client.network.endPacketImmediate(this.player.client.connectionGuid);
        }

        public void simulateAll() {
            Zombie z;
            while (!this.zombies4Add.isEmpty()) {
                z = this.zombies4Add.poll();
                this.zombies.put(Integer.valueOf(z.onlineId), z);
            }
            this.zombies.forEach(this::simulate);
            while (!this.zombies4Delete.isEmpty()) {
                z = this.zombies4Delete.poll();
                this.zombies.remove(z.onlineId);
            }
        }

        public void update() {
            if (this.updateLimiter.Check()) {
                this.simulateAll();
                this.send();
            }
        }

        private static enum Behaviour {
            Stay,
            Normal,
            Attack;

        }
    }

    private static class Player {
        private static final int cellSize = 50;
        private static final int spawnMinX = 3550;
        private static final int spawnMaxX = 14450;
        private static final int spawnMinY = 5050;
        private static final int spawnMaxY = 12950;
        private static final int ChunkGridWidth = 13;
        private static final int ChunksPerWidth = 10;
        private static int fps = 60;
        private static int predictInterval = 1000;
        private static float damage = 1.0f;
        private static boolean isVOIPEnabled;
        private final NetworkCharacter networkCharacter;
        private final UpdateLimit updateLimiter;
        private final UpdateLimit predictLimiter;
        private final UpdateLimit timeSyncLimiter;
        private final Client client;
        private final Movement movement;
        private final ArrayList<Clothes> clothes;
        private final String username;
        private final int isFemale;
        private final Color tagColor;
        private final Color speakColor;
        private UpdateLimit teleportLimiter;
        private short onlineId;
        private float x;
        private float y;
        private final float z;
        private final Vector2 direction;
        private int worldX;
        private int worldY;
        private float angle;
        private final ZombieSimulator simulator;
        private final PlayerManager playerManager;
        private final boolean weaponIsBareHeads = false;
        private final int weaponId = 837602032;
        private final short registryId = (short)1202;
        static float distance;
        private int lastPlayerForHello = -1;

        private Player(Movement movement, Network network, int connectionIndex, int port) {
            this.username = String.format("Client%d", movement.id);
            this.tagColor = Colors.SkyBlue;
            this.speakColor = Colors.GetRandomColor();
            this.isFemale = (int)Math.round(Math.random());
            this.onlineId = (short)-1;
            this.clothes = new ArrayList();
            this.clothes.add(new Clothes(11, 0, "Shirt_FormalWhite"));
            this.clothes.add(new Clothes(13, 3, "Tie_Full"));
            this.clothes.add(new Clothes(11, 0, "Socks_Ankle"));
            this.clothes.add(new Clothes(13, 0, "Trousers_Suit"));
            this.clothes.add(new Clothes(13, 0, "Suit_Jacket"));
            this.clothes.add(new Clothes(11, 0, "Shoes_Black"));
            this.clothes.add(new Clothes(11, 0, "Glasses_Sun"));
            this.worldX = (int)this.x / 10;
            this.worldY = (int)this.y / 10;
            this.movement = movement;
            this.z = 0.0f;
            this.angle = 0.0f;
            this.x = movement.spawn.x;
            this.y = movement.spawn.y;
            this.direction = movement.direction.ToVector();
            this.networkCharacter = new NetworkCharacter();
            this.simulator = new ZombieSimulator(this);
            this.playerManager = new PlayerManager(this);
            this.client = new Client(this, network, connectionIndex, port);
            network.createdClients.put(connectionIndex, this.client);
            this.updateLimiter = new UpdateLimit(1000 / fps);
            this.predictLimiter = new UpdateLimit((long)((float)predictInterval * 0.6f));
            this.timeSyncLimiter = new UpdateLimit(10000L);
        }

        private float getDistance(float speed) {
            return speed / 3.6f / (float)fps;
        }

        private void teleportMovement() {
            float nx = this.movement.destination.x;
            float ny = this.movement.destination.y;
            FakeClientManager.info(this.movement.id, String.format("Player %3d teleport (%9.3f,%9.3f) => (%9.3f,%9.3f) / %9.3f, next in %.3fs", this.onlineId, Float.valueOf(this.x), Float.valueOf(this.y), Float.valueOf(nx), Float.valueOf(ny), Math.sqrt(Math.pow(nx - this.x, 2.0) + Math.pow(ny - this.y, 2.0)), Float.valueOf((float)this.movement.teleportDelay / 1000.0f)));
            this.x = nx;
            this.y = ny;
            this.angle = 0.0f;
            this.teleportLimiter.Reset(this.movement.teleportDelay);
        }

        private void lineMovement() {
            distance = this.getDistance(this.movement.speed);
            this.direction.set(this.movement.destination.x - this.x, this.movement.destination.y - this.y);
            this.direction.normalize();
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            if (this.x < this.movement.destination.x && nx > this.movement.destination.x || this.x > this.movement.destination.x && nx < this.movement.destination.x || this.y < this.movement.destination.y && ny > this.movement.destination.y || this.y > this.movement.destination.y && ny < this.movement.destination.y) {
                nx = this.movement.destination.x;
                ny = this.movement.destination.y;
            }
            this.x = nx;
            this.y = ny;
        }

        private void circleMovement() {
            this.angle = (this.angle + (float)(2.0 * Math.asin(this.getDistance(this.movement.speed) / 2.0f / (float)this.movement.radius))) % 360.0f;
            float nx = this.movement.spawn.x + (float)((double)this.movement.radius * Math.sin(this.angle));
            float ny = this.movement.spawn.y + (float)((double)this.movement.radius * Math.cos(this.angle));
            this.x = nx;
            this.y = ny;
        }

        private Zombie getNearestZombie() {
            Zombie targetZombie = null;
            float distanceSquared = Float.POSITIVE_INFINITY;
            for (Zombie z : this.simulator.zombies.values()) {
                float ds = IsoUtils.DistanceToSquared(this.x, this.y, z.x, z.y);
                if (!(ds < distanceSquared)) continue;
                targetZombie = z;
                distanceSquared = ds;
            }
            return targetZombie;
        }

        private Zombie getNearestZombie(PlayerManager.RemotePlayer targetPlayer) {
            Zombie targetZombie = null;
            float distanceSquared = Float.POSITIVE_INFINITY;
            for (Zombie z : this.simulator.zombies.values()) {
                float ds = IsoUtils.DistanceToSquared(targetPlayer.x, targetPlayer.y, z.x, z.y);
                if (!(ds < distanceSquared)) continue;
                targetZombie = z;
                distanceSquared = ds;
            }
            return targetZombie;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private PlayerManager.RemotePlayer getNearestPlayer() {
            PlayerManager.RemotePlayer targetPlayer = null;
            float distanceSquared = Float.POSITIVE_INFINITY;
            HashMap<Integer, PlayerManager.RemotePlayer> hashMap = this.playerManager.players;
            synchronized (hashMap) {
                for (PlayerManager.RemotePlayer p : this.playerManager.players.values()) {
                    float ds = IsoUtils.DistanceToSquared(this.x, this.y, p.x, p.y);
                    if (!(ds < distanceSquared)) continue;
                    targetPlayer = p;
                    distanceSquared = ds;
                }
            }
            return targetPlayer;
        }

        private void aiAttackZombiesMovement() {
            Zombie targetZombie = this.getNearestZombie();
            float distance = this.getDistance(this.movement.speed);
            if (targetZombie != null) {
                this.direction.set(targetZombie.x - this.x, targetZombie.y - this.y);
                this.direction.normalize();
            }
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            this.x = nx;
            this.y = ny;
        }

        private void aiRunAwayFromZombiesMovement() {
            Zombie targetZombie = this.getNearestZombie();
            float distance = this.getDistance(this.movement.speed);
            if (targetZombie != null) {
                this.direction.set(this.x - targetZombie.x, this.y - targetZombie.y);
                this.direction.normalize();
            }
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            this.x = nx;
            this.y = ny;
        }

        private void aiRunToAnotherPlayersMovement() {
            PlayerManager.RemotePlayer targetPlayer = this.getNearestPlayer();
            float distance = this.getDistance(this.movement.speed);
            float nx = this.x + distance * this.direction.x;
            float ny = this.y + distance * this.direction.y;
            if (targetPlayer != null) {
                this.direction.set(targetPlayer.x - this.x, targetPlayer.y - this.y);
                float length = this.direction.normalize();
                if (length > 2.0f) {
                    this.x = nx;
                    this.y = ny;
                } else if (this.lastPlayerForHello != targetPlayer.onlineId) {
                    this.lastPlayerForHello = targetPlayer.onlineId;
                }
            }
        }

        private void aiNormalMovement() {
            float distance = this.getDistance(this.movement.speed);
            PlayerManager.RemotePlayer targetPlayer = this.getNearestPlayer();
            if (targetPlayer == null) {
                this.aiRunAwayFromZombiesMovement();
                return;
            }
            float dsp = IsoUtils.DistanceToSquared(this.x, this.y, targetPlayer.x, targetPlayer.y);
            if (dsp > 36.0f) {
                this.movement.speed = 13.0f;
                this.movement.motion = Movement.Motion.Run;
            } else {
                this.movement.speed = 4.0f;
                this.movement.motion = Movement.Motion.Walk;
            }
            Zombie targetZombie = this.getNearestZombie();
            float dsz = Float.POSITIVE_INFINITY;
            if (targetZombie != null) {
                dsz = IsoUtils.DistanceToSquared(this.x, this.y, targetZombie.x, targetZombie.y);
            }
            Zombie targetZombieForPlayer = this.getNearestZombie(targetPlayer);
            float dsz4player = Float.POSITIVE_INFINITY;
            if (targetZombieForPlayer != null) {
                dsz4player = IsoUtils.DistanceToSquared(targetPlayer.x, targetPlayer.y, targetZombieForPlayer.x, targetZombieForPlayer.y);
            }
            if (dsz4player < 25.0f) {
                targetZombie = targetZombieForPlayer;
                dsz = dsz4player;
            }
            if (dsp > 25.0f || targetZombie == null) {
                this.direction.set(targetPlayer.x - this.x, targetPlayer.y - this.y);
                float length = this.direction.normalize();
                if (length > 4.0f) {
                    float nx = this.x + distance * this.direction.x;
                    float ny = this.y + distance * this.direction.y;
                    this.x = nx;
                    this.y = ny;
                } else if (this.lastPlayerForHello != targetPlayer.onlineId) {
                    this.lastPlayerForHello = targetPlayer.onlineId;
                }
            } else if (dsz < 25.0f) {
                this.direction.set(targetZombie.x - this.x, targetZombie.y - this.y);
                this.direction.normalize();
                this.x += distance * this.direction.x;
                this.y += distance * this.direction.y;
            }
        }

        private void checkRequestChunks() {
            int currentWorldX = (int)this.x / 10;
            int currentWorldY = (int)this.y / 10;
            if (Math.abs(currentWorldX - this.worldX) >= 13 || Math.abs(currentWorldY - this.worldY) >= 13) {
                int minwx = this.worldX - 6;
                int minwy = this.worldY - 6;
                int maxwx = this.worldX + 6;
                int maxwy = this.worldY + 6;
                for (int xx = minwx; xx <= maxwx; ++xx) {
                    for (int yy = minwy; yy <= maxwy; ++yy) {
                        this.client.addChunkRequest(xx, yy, xx - minwx, yy - minwy);
                    }
                }
            } else if (currentWorldX != this.worldX) {
                if (currentWorldX < this.worldX) {
                    for (int i = -6; i <= 6; ++i) {
                        this.client.addChunkRequest(this.worldX - 6, this.worldY + i, 0, i + 6);
                    }
                } else {
                    for (int i = -6; i <= 6; ++i) {
                        this.client.addChunkRequest(this.worldX + 6, this.worldY + i, 12, i + 6);
                    }
                }
            } else if (currentWorldY != this.worldY) {
                if (currentWorldY < this.worldY) {
                    for (int i = -6; i <= 6; ++i) {
                        this.client.addChunkRequest(this.worldX + i, this.worldY - 6, i + 6, 0);
                    }
                } else {
                    for (int i = -6; i <= 6; ++i) {
                        this.client.addChunkRequest(this.worldX + i, this.worldY + 6, i + 6, 12);
                    }
                }
            }
            this.client.requestChunks();
            this.worldX = currentWorldX;
            this.worldY = currentWorldY;
        }

        private void hit() {
            FakeClientManager.info(this.movement.id, String.format("Player %3d hit", this.onlineId));
        }

        private void run() {
            this.simulator.update();
            if (this.updateLimiter.Check()) {
                if (isVOIPEnabled) {
                    FMODManager.instance.tick();
                    VoiceManager.instance.update();
                }
                if (this.movement.doTeleport() && this.teleportLimiter.Check()) {
                    this.teleportMovement();
                }
                switch (this.movement.type.ordinal()) {
                    case 2: {
                        this.circleMovement();
                        break;
                    }
                    case 1: {
                        this.lineMovement();
                        break;
                    }
                    case 3: {
                        this.aiAttackZombiesMovement();
                        break;
                    }
                    case 4: {
                        this.aiRunAwayFromZombiesMovement();
                        break;
                    }
                    case 5: {
                        this.aiRunToAnotherPlayersMovement();
                        break;
                    }
                    case 6: {
                        this.aiNormalMovement();
                    }
                }
                this.checkRequestChunks();
                if (this.predictLimiter.Check()) {
                    int currentTime = (int)(this.client.getServerTime() / 1000000L);
                    this.networkCharacter.checkResetPlayer(currentTime);
                    NetworkCharacter.Transform transform = this.networkCharacter.predict(predictInterval, currentTime, this.x, this.y, this.direction.x, this.direction.y);
                    this.client.sendPlayer(transform, currentTime, this.direction);
                }
                if (this.timeSyncLimiter.Check()) {
                    this.client.sendTimeSync();
                    this.client.sendSyncRadioData();
                }
                if (this.movement.hordeCreator != null && this.movement.hordeCreator.hordeCreatorLimiter.Check()) {
                    this.client.sendCommand(this.movement.hordeCreator.getCommand(PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), PZMath.fastfloor(this.z)));
                }
                if (this.movement.soundMaker != null && this.movement.soundMaker.soundMakerLimiter.Check()) {
                    this.client.sendWorldSound4Player(PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), PZMath.fastfloor(this.z), this.movement.soundMaker.radius, this.movement.soundMaker.radius);
                    this.client.sendChatMessage(this.movement.soundMaker.message);
                    this.client.sendEventPacket(this.onlineId, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), PZMath.fastfloor(this.z), (byte)4, "shout");
                }
            }
        }

        private static class Clothes {
            private final byte flags;
            private final byte text;
            private final String name;

            Clothes(byte flags, byte text, String name) {
                this.flags = flags;
                this.text = text;
                this.name = name;
            }
        }
    }

    private static class HordeCreator {
        private final int radius;
        private final int count;
        private final long interval;
        private final UpdateLimit hordeCreatorLimiter;

        public HordeCreator(int radius, int count, long interval) {
            this.radius = radius;
            this.count = count;
            this.interval = interval;
            this.hordeCreatorLimiter = new UpdateLimit(interval);
        }

        public String getCommand(int x, int y, int z) {
            return String.format("/createhorde2 -x %d -y %d -z %d -count %d -radius %d -crawler false -isFallOnFront false -isFakeDead false -knockedDown false -health 1 -outfit", x, y, z, this.count, this.radius);
        }
    }

    private static class SoundMaker {
        private final int radius;
        private final int interval;
        private final String message;
        private final UpdateLimit soundMakerLimiter;

        public SoundMaker(int interval, int radius, String message) {
            this.radius = radius;
            this.message = message;
            this.interval = interval;
            this.soundMakerLimiter = new UpdateLimit(interval);
        }
    }

    private static class Network {
        private final HashMap<Integer, Client> createdClients = new HashMap();
        private final HashMap<Long, Client> connectedClients = new HashMap();
        private final ByteBufferReader rb = new ByteBufferReader(ByteBuffer.allocate(1000000));
        private final ByteBufferWriter wb = new ByteBufferWriter(ByteBuffer.allocate(1000000));
        private final RakNetPeerInterface peer = new RakNetPeerInterface();
        private final int started;
        private int connected = -1;
        private static final HashMap<Integer, String> systemPacketTypeNames = new HashMap();

        boolean isConnected() {
            return this.connected == 0;
        }

        boolean isStarted() {
            return this.started == 0;
        }

        private Network(int maxConnections, int port) {
            this.peer.Init(false);
            this.peer.SetMaximumIncomingConnections(0);
            this.peer.SetClientPort(port);
            this.peer.SetOccasionalPing(true);
            this.started = this.peer.Startup(maxConnections);
            if (this.started == 0) {
                Thread thread2 = new Thread(ThreadGroups.Network, this::receiveThread, "PeerInterfaceReceive");
                thread2.setDaemon(true);
                thread2.start();
                FakeClientManager.log(-1, "Network start ok");
            } else {
                FakeClientManager.error(-1, String.format("Network start failed: %d", this.started));
            }
        }

        private void connect(int clientID, String address) {
            this.connected = this.peer.Connect(address, 16261, PZcrypt.hash("", true), false);
            if (this.connected == 0) {
                FakeClientManager.log(clientID, String.format("Client connected to %s:%d", address, 16261));
            } else {
                FakeClientManager.error(clientID, String.format("Client connection to %s:%d failed: %d", address, 16261, this.connected));
            }
        }

        private void disconnect(long connectedGUID, int clientID, String address) {
            if (connectedGUID != 0L) {
                this.peer.disconnect(connectedGUID, "");
                this.connected = -1;
            }
            if (this.connected == -1) {
                FakeClientManager.log(clientID, String.format("Client disconnected from %s:%d", address, 16261));
            } else {
                FakeClientManager.log(clientID, String.format("Client disconnection from %s:%d failed: %d", address, 16261, connectedGUID));
            }
        }

        private ByteBufferWriter startPacket() {
            this.wb.clear();
            return this.wb;
        }

        private void cancelPacket() {
            this.wb.clear();
        }

        private void sendPacket(int priority, int reliability, long connectionGUID) {
            this.startPacket();
            this.peer.Send(this.wb.bb, priority, reliability, (byte)0, connectionGUID, false);
        }

        private void endPacket(long connectionGUID) {
            this.sendPacket(1, 3, connectionGUID);
        }

        private void endPacketImmediate(long connectionGUID) {
            this.sendPacket(0, 3, connectionGUID);
        }

        private void endPacketSuperHighUnreliable(long connectionGUID) {
            this.sendPacket(0, 1, connectionGUID);
        }

        private void receiveThread() {
            while (true) {
                if (this.peer.Receive(this.rb.bb)) {
                    this.decode(this.rb);
                    continue;
                }
                FakeClientManager.sleep(1L);
            }
        }

        private static void logUserPacket(int id, short type) {
            PacketTypes.PacketType packetType = PacketTypes.packetTypes.get(type);
            String packet = packetType == null ? "unknown user packet" : packetType.name();
            FakeClientManager.trace(id, String.format("## %s (%d)", packet, type));
        }

        private static void logSystemPacket(int id, int type) {
            String packet = systemPacketTypeNames.getOrDefault(type, "unknown system packet");
            FakeClientManager.trace(id, String.format("## %s (%d)", packet, type));
        }

        private void decode(ByteBufferReader buf) {
            int type = buf.getByte() & 0xFF;
            int connectionIndex = -1;
            Network.logSystemPacket(connectionIndex, type);
            switch (type) {
                case 17: 
                case 18: 
                case 23: 
                case 24: 
                case 32: {
                    FakeClientManager.error(-1, "Connection failed: " + type);
                    break;
                }
                case 22: {
                    connectionIndex = buf.getByte() & 0xFF;
                    Client client = this.createdClients.get(connectionIndex);
                    if (client == null) break;
                    client.changeState(Client.State.DISCONNECT);
                    break;
                }
                case 21: {
                    connectionIndex = buf.getByte() & 0xFF;
                    long connectionGUID = this.peer.getGuidOfPacket();
                    Client client = this.connectedClients.get(connectionGUID);
                    if (client != null) {
                        this.connectedClients.remove(connectionGUID);
                        client.changeState(Client.State.DISCONNECT);
                    }
                    FakeClientManager.log(-1, String.format("Connected clients: %d (connection index %d)", this.connectedClients.size(), connectionIndex));
                    break;
                }
                case 19: {
                    connectionIndex = buf.getByte() & 0xFF;
                }
                case 44: 
                case 45: {
                    long connectionGUID = this.peer.getGuidOfPacket();
                    break;
                }
                case 16: {
                    connectionIndex = buf.getByte() & 0xFF;
                    long connectionGUID = this.peer.getGuidOfPacket();
                    Client client = this.createdClients.get(connectionIndex);
                    if (client != null) {
                        client.connectionGuid = connectionGUID;
                        this.connectedClients.put(connectionGUID, client);
                        VoiceManager.instance.VoiceConnectReq(connectionGUID);
                        client.changeState(Client.State.LOGIN);
                    }
                    FakeClientManager.log(-1, String.format("Connected clients: %d (connection index %d)", this.connectedClients.size(), connectionIndex));
                    break;
                }
                case 0: 
                case 1: 
                case 20: 
                case 25: 
                case 31: 
                case 33: {
                    break;
                }
                case 134: {
                    short userPacketId = buf.getShort();
                    long connectionGUID = this.peer.getGuidOfPacket();
                    Client client = this.connectedClients.get(connectionGUID);
                    if (client == null) break;
                    client.receive(userPacketId, buf);
                    break;
                }
            }
        }

        static {
            systemPacketTypeNames.put(22, "connection lost");
            systemPacketTypeNames.put(21, "disconnected");
            systemPacketTypeNames.put(23, "connection banned");
            systemPacketTypeNames.put(17, "connection failed");
            systemPacketTypeNames.put(20, "no free connections");
            systemPacketTypeNames.put(16, "connection accepted");
            systemPacketTypeNames.put(18, "already connected");
            systemPacketTypeNames.put(44, "voice request");
            systemPacketTypeNames.put(45, "voice reply");
            systemPacketTypeNames.put(25, "wrong protocol version");
            systemPacketTypeNames.put(0, "connected ping");
            systemPacketTypeNames.put(1, "unconnected ping");
            systemPacketTypeNames.put(33, "new remote connection");
            systemPacketTypeNames.put(31, "remote disconnection");
            systemPacketTypeNames.put(32, "remote connection lost");
            systemPacketTypeNames.put(24, "invalid password");
            systemPacketTypeNames.put(19, "new connection");
            systemPacketTypeNames.put(134, "user packet");
        }
    }

    private static class PlayerManager {
        private final Player player;
        private final PlayerPacket playerPacket = new PlayerPacket();
        public final HashMap<Integer, RemotePlayer> players = new HashMap();

        public PlayerManager(Player player) {
            this.player = player;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void parsePlayer(ByteBufferReader b) {
            PlayerPacket packet = this.playerPacket;
            packet.parse(b, null);
            HashMap<Integer, RemotePlayer> hashMap = this.players;
            synchronized (hashMap) {
                RemotePlayer p = this.players.get(packet.id);
                if (p == null) {
                    FakeClientManager.trace(this.player.movement.id, String.format("New player %s", p.onlineId));
                }
                p.x = packet.prediction.position.x;
                p.y = packet.prediction.position.y;
                p.z = packet.prediction.position.z;
            }
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        private void parsePlayerTimeout(ByteBufferReader b) {
            short onlineId = b.getShort();
            HashMap<Integer, RemotePlayer> hashMap = this.players;
            synchronized (hashMap) {
                this.players.remove(onlineId);
            }
            FakeClientManager.trace(this.player.movement.id, String.format("Remove player %s", onlineId));
        }

        private class RemotePlayer {
            public float x;
            public float y;
            public float z;
            public short onlineId;
            public PlayerPacket playerPacket;

            public RemotePlayer(PlayerManager playerManager, short onlineId) {
                Objects.requireNonNull(playerManager);
                this.playerPacket = new PlayerPacket();
                this.onlineId = onlineId;
            }
        }
    }

    private static class Zombie {
        public long lastUpdate;
        public float x;
        public float y;
        public float z;
        public short onlineId;
        public boolean localOwnership;
        public ZombiePacket zombiePacket;
        public IsoDirections dir = IsoDirections.N;
        public float health = 1.0f;
        public byte walkType = (byte)Rand.Next(NetworkVariables.WalkType.values().length);
        public float dropPositionX;
        public float dropPositionY;
        public boolean isMoving;

        public Zombie(short onlineId) {
            this.zombiePacket = new ZombiePacket();
            this.zombiePacket.id = onlineId;
            this.onlineId = onlineId;
            this.localOwnership = false;
        }
    }
}

