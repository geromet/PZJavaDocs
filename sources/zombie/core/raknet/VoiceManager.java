/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import fmod.FMODRecordPosition;
import fmod.FMODSoundData;
import fmod.FMOD_DriverInfo;
import fmod.FMOD_RESULT;
import fmod.javafmod;
import fmod.javafmodJNI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.Platform;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.RakVoice;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManagerData;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Radio;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.FakeClientManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.radio.devices.DeviceData;
import zombie.vehicles.VehiclePart;

public class VoiceManager {
    private static final int FMOD_SOUND_MODE = 1154;
    public static final int modePPT = 1;
    public static final int modeVAD = 2;
    public static final int modeMute = 3;
    public static final int VADModeQuality = 1;
    public static final int VADModeLowBitrate = 2;
    public static final int VADModeAggressive = 3;
    public static final int VADModeVeryAggressive = 4;
    public static final int AGCModeAdaptiveAnalog = 1;
    public static final int AGCModeAdaptiveDigital = 2;
    public static final int AGCModeFixedDigital = 3;
    private static final int bufferSize = 192;
    private static final int complexity = 1;
    private static boolean serverVOIPEnable = true;
    private static int sampleRate = 16000;
    private static int period = 300;
    private static int buffering = 8000;
    private static float minDistance;
    private static float maxDistance;
    private static boolean is3D;
    private boolean isEnable = true;
    private boolean isModeVad;
    private boolean isModePpt;
    private int vadMode = 3;
    private int agcMode = 2;
    private int volumeMic;
    private int volumePlayers;
    public static boolean voipDisabled;
    private boolean isServer;
    private static byte[] fmodReceiveBuffer;
    private final FMODSoundData fmodSoundData = new FMODSoundData();
    private final FMODRecordPosition fmodRecordPosition = new FMODRecordPosition();
    private int fmodSoundDataError;
    private int fmodVoiceRecordDriverId;
    private long fmodChannelGroup;
    private long fmodRecordSound;
    private Semaphore recDevSemaphore;
    private boolean initialiseRecDev;
    private boolean initialisedRecDev;
    private long indicatorIsVoice;
    private Thread thread;
    private boolean quit;
    private long timeLast;
    private final boolean isDebug = false;
    private final boolean isDebugLoopback = false;
    private final boolean isDebugLoopbackLong = false;
    public static VoiceManager instance;
    byte[] buf = new byte[192];
    private final Object notifier = new Object();
    private boolean isClient;
    private boolean testingMicrophone;
    private long testingMicrophoneMs;
    private static long timestamp;

    public static VoiceManager getInstance() {
        return instance;
    }

    public void DeinitRecSound() {
        this.initialisedRecDev = false;
        if (this.fmodRecordSound != 0L) {
            javafmod.FMOD_RecordSound_Release(this.fmodRecordSound);
            this.fmodRecordSound = 0L;
        }
        fmodReceiveBuffer = null;
    }

    public void ResetRecSound() {
        int result;
        if (this.initialisedRecDev && this.fmodRecordSound != 0L && (result = javafmod.FMOD_System_RecordStop(this.fmodVoiceRecordDriverId)) != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", result);
        }
        this.DeinitRecSound();
        this.fmodRecordSound = javafmod.FMOD_System_CreateRecordSound(this.fmodVoiceRecordDriverId, 1096L, 2L, sampleRate, this.agcMode);
        if (this.fmodRecordSound == 0L) {
            DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", this.fmodRecordSound);
        }
        javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, 11 - this.volumeMic)));
        if (this.initialiseRecDev && (result = javafmod.FMOD_System_RecordStart(this.fmodVoiceRecordDriverId, this.fmodRecordSound, true)) != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", result);
        }
        javafmod.FMOD_System_SetVADMode(this.vadMode - 1);
        fmodReceiveBuffer = new byte[2048];
        this.initialisedRecDev = true;
    }

    public void VoiceRestartClient(boolean isEnable) {
        if (GameClient.connection != null) {
            if (isEnable) {
                this.loadConfig();
                this.VoiceConnectReq(GameClient.connection.getConnectedGUID());
            } else {
                this.threadSafeCode(this::DeinitRecSound);
                this.VoiceConnectClose(GameClient.connection.getConnectedGUID());
                this.loadConfig();
            }
        } else {
            this.loadConfig();
            if (isEnable) {
                this.InitRecDeviceForTest();
            } else {
                this.threadSafeCode(this::DeinitRecSound);
            }
        }
    }

    void VoiceInitClient() {
        this.isServer = false;
        this.recDevSemaphore = new Semaphore(1);
        fmodReceiveBuffer = null;
        RakVoice.RVInit(192);
        RakVoice.SetComplexity(1);
    }

    void VoiceInitServer(boolean enable, int sampleRate, int period, int complexity, int buffering, double minDistance, double maxDistance, boolean is3D) {
        this.isServer = true;
        if (!(period == 2 | period == 5 | period == 10 | period == 20 | period == 40 | period == 60)) {
            DebugLog.Voice.error("Invalid period=%d", period);
            return;
        }
        if (!(sampleRate == 8000 | sampleRate == 16000 | sampleRate == 24000)) {
            DebugLog.Voice.error("Invalid sample rate=%d", sampleRate);
            return;
        }
        if (complexity < 0 | complexity > 10) {
            DebugLog.Voice.error("Invalid quality=%d", complexity);
            return;
        }
        if (buffering < 0 | buffering > 32000) {
            DebugLog.Voice.error("Invalid buffering=%d", buffering);
            return;
        }
        VoiceManager.sampleRate = sampleRate;
        RakVoice.RVInitServer(enable, sampleRate, period, complexity, buffering, (float)minDistance, (float)maxDistance, is3D);
    }

    void VoiceConnectAccept(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
        }
    }

    void InitRecDeviceForTest() {
        this.threadSafeCode(this::ResetRecSound);
    }

    void VoiceOpenChannelReply(long uuid, ByteBufferReader buf) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%d", uuid);
            if (this.isServer) {
                return;
            }
            try {
                if (GameClient.client) {
                    serverVOIPEnable = buf.getInt() != 0;
                    sampleRate = buf.getInt();
                    period = buf.getInt();
                    buf.getInt();
                    buffering = buf.getInt();
                    minDistance = buf.getFloat();
                    maxDistance = buf.getFloat();
                    is3D = buf.getInt() != 0;
                } else {
                    serverVOIPEnable = RakVoice.GetServerVOIPEnable();
                    sampleRate = RakVoice.GetSampleRate();
                    period = RakVoice.GetSendFramePeriod();
                    buffering = RakVoice.GetBuffering();
                    minDistance = RakVoice.GetMinDistance();
                    maxDistance = RakVoice.GetMaxDistance();
                    is3D = RakVoice.GetIs3D();
                }
            }
            catch (Exception e) {
                DebugLog.Voice.printException(e, "RakVoice params set failed", LogSeverity.Error);
                return;
            }
            DebugLog.Voice.debugln("enabled=%b, sample-rate=%d, period=%d, complexity=%d, buffering=%d, is3D=%b", serverVOIPEnable, sampleRate, period, 1, buffering, is3D);
            try {
                this.recDevSemaphore.acquire();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            int mode = is3D ? 1170 : 1154;
            for (VoiceManagerData d : VoiceManagerData.data) {
                if (d.userplaysound == 0L) continue;
                javafmod.FMOD_Sound_SetMode(d.userplaysound, mode);
            }
            long result = javafmod.FMOD_System_SetRawPlayBufferingPeriod(buffering);
            if (result != (long)FMOD_RESULT.FMOD_OK.ordinal()) {
                DebugLog.Voice.warn("FMOD_System_SetRawPlayBufferingPeriod result=%d", result);
            }
            this.ResetRecSound();
            this.recDevSemaphore.release();
        }
    }

    public void VoiceConnectReq(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
            VoiceManagerData.data.clear();
            RakVoice.RequestVoiceChannel(uuid);
        }
    }

    public void VoiceConnectClose(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
            RakVoice.CloseVoiceChannel(uuid);
        }
    }

    public void setMode(int mode) {
        if (mode == 3) {
            this.isModeVad = false;
            this.isModePpt = false;
        } else if (mode == 1) {
            this.isModeVad = false;
            this.isModePpt = true;
        } else if (mode == 2) {
            this.isModeVad = true;
            this.isModePpt = false;
        }
    }

    public void setVADMode(int mode) {
        if (mode < 1 | mode > 4) {
            return;
        }
        this.vadMode = mode;
        if (!this.initialisedRecDev) {
            return;
        }
        this.threadSafeCode(() -> javafmod.FMOD_System_SetVADMode(this.vadMode - 1));
    }

    public void setAGCMode(int mode) {
        if (mode < 1 | mode > 3) {
            return;
        }
        this.agcMode = mode;
        if (!this.initialisedRecDev) {
            return;
        }
        this.threadSafeCode(this::ResetRecSound);
    }

    public void setVolumePlayers(int volume) {
        if (volume < 0 | volume > 11) {
            return;
        }
        this.volumePlayers = volume <= 10 ? volume : 12;
        if (!this.initialisedRecDev) {
            return;
        }
        ArrayList<VoiceManagerData> data = VoiceManagerData.data;
        for (int i = 0; i < data.size(); ++i) {
            VoiceManagerData d = data.get(i);
            if (d == null || d.userplaychannel == 0L) continue;
            javafmod.FMOD_Channel_SetVolume(d.userplaychannel, (float)((double)this.volumePlayers * 0.2));
        }
    }

    public void setVolumeMic(int volume) {
        if (volume < 0 | volume > 11) {
            return;
        }
        this.volumeMic = volume <= 10 ? volume : 12;
        if (!this.initialisedRecDev) {
            return;
        }
        this.threadSafeCode(() -> javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, 11 - this.volumeMic))));
    }

    public static void playerSetMute(String username) {
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
        for (int i1 = 0; i1 < players.size(); ++i1) {
            IsoPlayer player = players.get(i1);
            if (!username.equals(player.username)) continue;
            VoiceManagerData d = VoiceManagerData.get(player.onlineId);
            player.isVoiceMute = d.userplaymute = !d.userplaymute;
            break;
        }
    }

    public static boolean playerGetMute(String username) {
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
        for (int i1 = 0; i1 < players.size(); ++i1) {
            IsoPlayer player = players.get(i1);
            if (!username.equals(player.username)) continue;
            boolean ret = VoiceManagerData.get((short)player.onlineId).userplaymute;
            return ret;
        }
        return true;
    }

    public void LuaRegister(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        table.rawset("playerSetMute", (Object)new JavaFunction(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                VoiceManager.playerSetMute((String)arg1);
                return 1;
            }
        });
        table.rawset("playerGetMute", (Object)new JavaFunction(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                callFrame.push(VoiceManager.playerGetMute((String)arg1));
                return 1;
            }
        });
        table.rawset("RecordDevices", (Object)new JavaFunction(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                if (Core.soundDisabled || voipDisabled) {
                    KahluaTable recordDevices = callFrame.getPlatform().newTable();
                    callFrame.push(recordDevices);
                    return 1;
                }
                int numDevices = javafmod.FMOD_System_GetRecordNumDrivers();
                KahluaTable recordDevices = callFrame.getPlatform().newTable();
                for (int i = 0; i < numDevices; ++i) {
                    FMOD_DriverInfo info = new FMOD_DriverInfo();
                    javafmod.FMOD_System_GetRecordDriverInfo(i, info);
                    recordDevices.rawset(i + 1, (Object)info.name);
                }
                callFrame.push(recordDevices);
                return 1;
            }
        });
        environment.rawset("VoiceManager", (Object)table);
    }

    private void setUserPlaySound(long userPlayChannel, float volume) {
        volume = IsoUtils.clamp(volume * IsoUtils.lerp(this.volumePlayers, 0.0f, 12.0f), 0.0f, 1.0f);
        javafmod.FMOD_Channel_SetVolume(userPlayChannel, volume);
    }

    private long getUserPlaySound(short onlineId) {
        VoiceManagerData d = VoiceManagerData.get(onlineId);
        if (d.userplaychannel == 0L) {
            d.userplaysound = 0L;
            int mode = is3D ? 1170 : 1154;
            d.userplaysound = javafmod.FMOD_System_CreateRAWPlaySound(mode, 2L, sampleRate);
            if (d.userplaysound == 0L) {
                DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", d.userplaysound);
            }
            d.userplaychannel = javafmod.FMOD_System_PlaySound(d.userplaysound, false);
            if (d.userplaychannel == 0L) {
                DebugLog.Voice.warn("FMOD_System_PlaySound result=%d", d.userplaychannel);
            }
            javafmod.FMOD_Channel_SetVolume(d.userplaychannel, (float)((double)this.volumePlayers * 0.2));
            if (is3D) {
                javafmod.FMOD_Channel_Set3DMinMaxDistance(d.userplaychannel, minDistance / 2.0f, maxDistance);
            }
            javafmod.FMOD_Channel_SetChannelGroup(d.userplaychannel, this.fmodChannelGroup);
        }
        return d.userplaysound;
    }

    public void InitVMClient() {
        if (Core.soundDisabled || voipDisabled) {
            this.isEnable = false;
            this.initialiseRecDev = false;
            this.initialisedRecDev = false;
            DebugLog.Voice.debugln("Disabled");
            return;
        }
        int numDevices = javafmod.FMOD_System_GetRecordNumDrivers();
        this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
        if (this.fmodVoiceRecordDriverId < 0 && numDevices > 0) {
            Core.getInstance().setOptionVoiceRecordDevice(1);
            this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
        }
        if (numDevices < 1) {
            DebugLog.Voice.debugln("Microphone not found");
            this.initialiseRecDev = false;
        } else if (this.fmodVoiceRecordDriverId < 0 | this.fmodVoiceRecordDriverId >= numDevices) {
            DebugLog.Voice.warn("Invalid record device");
            this.initialiseRecDev = false;
        } else {
            this.initialiseRecDev = true;
        }
        this.isEnable = Core.getInstance().getOptionVoiceEnable();
        this.setMode(Core.getInstance().getOptionVoiceMode());
        this.vadMode = Core.getInstance().getOptionVoiceVADMode();
        this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
        this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
        this.fmodChannelGroup = javafmod.FMOD_System_CreateChannelGroup("VOIP");
        this.VoiceInitClient();
        this.fmodRecordSound = 0L;
        if (this.isEnable) {
            this.InitRecDeviceForTest();
        }
        this.timeLast = System.currentTimeMillis();
        this.quit = false;
        this.thread = new Thread(this){
            final /* synthetic */ VoiceManager this$0;
            {
                VoiceManager voiceManager = this$0;
                Objects.requireNonNull(voiceManager);
                this.this$0 = voiceManager;
            }

            @Override
            public void run() {
                while (!this.this$0.quit) {
                    try {
                        this.this$0.UpdateVMClient();
                        4.sleep(period / 2);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        this.thread.setName("VoiceManagerClient");
        this.thread.start();
    }

    public void loadConfig() {
        this.isEnable = Core.getInstance().getOptionVoiceEnable();
        this.setMode(Core.getInstance().getOptionVoiceMode());
        this.vadMode = Core.getInstance().getOptionVoiceVADMode();
        this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
        this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
    }

    public void UpdateRecordDevice() {
        if (!this.initialisedRecDev) {
            return;
        }
        this.threadSafeCode(this::UpdateRecordDeviceInternal);
    }

    private void UpdateRecordDeviceInternal() {
        int result = javafmod.FMOD_System_RecordStop(this.fmodVoiceRecordDriverId);
        if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", result);
        }
        this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
        if (this.fmodVoiceRecordDriverId < 0) {
            DebugLog.Voice.error("No record device found");
            return;
        }
        result = javafmod.FMOD_System_RecordStart(this.fmodVoiceRecordDriverId, this.fmodRecordSound, true);
        if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", result);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void DeinitVMClient() {
        if (this.thread != null) {
            this.quit = true;
            Object object = this.notifier;
            synchronized (object) {
                this.notifier.notify();
            }
            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(10L);
                }
                catch (InterruptedException interruptedException) {}
            }
            this.thread = null;
        }
        this.DeinitRecSound();
        ArrayList<VoiceManagerData> data = VoiceManagerData.data;
        for (int i = 0; i < data.size(); ++i) {
            VoiceManagerData d = data.get(i);
            if (d.userplaychannel != 0L) {
                javafmod.FMOD_Channel_Stop(d.userplaychannel);
            }
            if (d.userplaysound == 0L) continue;
            javafmod.FMOD_RAWPlaySound_Release(d.userplaysound);
            d.userplaysound = 0L;
        }
        VoiceManagerData.data.clear();
    }

    public void setTestingMicrophone(boolean testing) {
        if (testing) {
            this.testingMicrophoneMs = System.currentTimeMillis();
        }
        if (testing != this.testingMicrophone) {
            this.testingMicrophone = testing;
            this.notifyThread();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void notifyThread() {
        Object object = this.notifier;
        synchronized (object) {
            this.notifier.notify();
        }
    }

    public void update() {
        long ms;
        if (GameServer.server) {
            return;
        }
        if (this.testingMicrophone && (ms = System.currentTimeMillis()) - this.testingMicrophoneMs > 1000L) {
            this.setTestingMicrophone(false);
        }
        if (GameClient.client && GameClient.connection != null || FakeClientManager.isVOIPEnabled()) {
            if (!this.isClient) {
                this.isClient = true;
                this.notifyThread();
            }
        } else if (this.isClient) {
            this.isClient = false;
            this.notifyThread();
        }
    }

    private float getCanHearAllVolume(float range) {
        return range > minDistance ? IsoUtils.clamp(1.0f - IsoUtils.lerp(range, minDistance, maxDistance), 0.2f, 1.0f) : 1.0f;
    }

    private void threadSafeCode(Runnable runnable2) {
        while (true) {
            try {
                this.recDevSemaphore.acquire();
            }
            catch (InterruptedException interruptedException) {
                continue;
            }
            break;
        }
        try {
            runnable2.run();
        }
        finally {
            this.recDevSemaphore.release();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    synchronized void UpdateVMClient() throws InterruptedException {
        long currentTime;
        while (!(this.quit || this.isClient || this.testingMicrophone)) {
            Object object = this.notifier;
            synchronized (object) {
                try {
                    this.notifier.wait();
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
        }
        if (!serverVOIPEnable) {
            return;
        }
        if (IsoPlayer.getInstance() != null) {
            boolean bl = IsoPlayer.getInstance().isSpeek = System.currentTimeMillis() - this.indicatorIsVoice <= 300L;
        }
        if (this.initialiseRecDev) {
            this.recDevSemaphore.acquire();
            javafmod.FMOD_System_GetRecordPosition(this.fmodVoiceRecordDriverId, this.fmodRecordPosition);
            if (fmodReceiveBuffer != null) {
                while ((this.fmodSoundDataError = javafmod.FMOD_Sound_GetData(this.fmodRecordSound, fmodReceiveBuffer, this.fmodSoundData)) == 0) {
                    if ((IsoPlayer.getInstance() == null || GameClient.connection == null) && !FakeClientManager.isVOIPEnabled() || is3D && IsoPlayer.getInstance().isDead()) continue;
                    if (this.isModePpt) {
                        if (GameKeyboard.isKeyDown("Enable voice transmit")) {
                            RakVoice.SendFrame(GameClient.connection.getConnectedGUID(), IsoPlayer.getInstance().getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size);
                            this.indicatorIsVoice = System.currentTimeMillis();
                        } else if (FakeClientManager.isVOIPEnabled()) {
                            RakVoice.SendFrame(FakeClientManager.getConnectedGUID(), FakeClientManager.getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size);
                            this.indicatorIsVoice = System.currentTimeMillis();
                        }
                    }
                    if (!this.isModeVad || this.fmodSoundData.vad == 0L) continue;
                    RakVoice.SendFrame(GameClient.connection.getConnectedGUID(), IsoPlayer.getInstance().getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size);
                    this.indicatorIsVoice = System.currentTimeMillis();
                }
            }
            this.recDevSemaphore.release();
        }
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
        ArrayList<VoiceManagerData> data = VoiceManagerData.data;
        for (int i = 0; i < data.size(); ++i) {
            VoiceManagerData d = data.get(i);
            boolean online = false;
            for (int pn = 0; pn < players.size(); ++pn) {
                IsoPlayer player = players.get(pn);
                if (player.onlineId != d.index) continue;
                online = true;
                break;
            }
            if (false & d.index == 0) break;
            if (!(d.userplaychannel != 0L & !online)) continue;
            javafmod.FMOD_Channel_Stop(d.userplaychannel);
            d.userplaychannel = 0L;
        }
        if ((currentTime = System.currentTimeMillis() - this.timeLast) >= (long)period) {
            this.timeLast += currentTime;
            if (IsoPlayer.getInstance() == null) {
                return;
            }
            for (IsoPlayer player : players) {
                IsoPlayer me;
                if (player == (me = IsoPlayer.getInstance()) || player.getOnlineID() == -1) continue;
                VoiceManagerData d = VoiceManagerData.get(player.getOnlineID());
                while (RakVoice.ReceiveFrame(player.getOnlineID(), this.buf)) {
                    d.voicetimeout = 10L;
                    if (d.userplaymute) continue;
                    float range = IsoUtils.DistanceTo(me.getX(), me.getY(), player.getX(), player.getY());
                    if (me.canHearAll()) {
                        javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0f);
                        javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0f, 0.0f, 0.0f);
                        this.setUserPlaySound(d.userplaychannel, this.getCanHearAllVolume(range));
                    } else {
                        VoiceManagerData.RadioData rdata = this.checkForNearbyRadios(d);
                        if (rdata != null && rdata.deviceData != null) {
                            javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0f);
                            javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0f, 0.0f, 0.0f);
                            this.setUserPlaySound(d.userplaychannel, rdata.deviceData.getDeviceVolume());
                            rdata.deviceData.doReceiveMPSignal(rdata.lastReceiveDistance);
                        } else {
                            if (rdata == null) {
                                javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0f);
                                javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0f, 0.0f, 0.0f);
                                javafmod.FMOD_Channel_SetVolume(d.userplaychannel, 0.0f);
                            } else {
                                if (is3D) {
                                    javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, IsoUtils.lerp(range, 0.0f, minDistance));
                                    javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, player.getX(), player.getY(), player.getZ(), 0.0f, 0.0f, 0.0f);
                                } else {
                                    javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0f);
                                    javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0f, 0.0f, 0.0f);
                                }
                                this.setUserPlaySound(d.userplaychannel, IsoUtils.smoothstep(maxDistance, minDistance, rdata.lastReceiveDistance));
                            }
                            if (range > maxDistance) {
                                VoiceManager.logFrame(me, player, range);
                            }
                        }
                    }
                    javafmod.FMOD_System_RAWPlayData(this.getUserPlaySound(player.getOnlineID()), this.buf, (long)this.buf.length);
                }
                if (d.voicetimeout == 0L) {
                    player.isSpeek = false;
                    continue;
                }
                --d.voicetimeout;
                player.isSpeek = true;
            }
        }
    }

    private static void logFrame(IsoPlayer me, IsoPlayer player, float distance) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > timestamp) {
            timestamp = currentTime + 5000L;
            DebugLog.Multiplayer.warn(String.format("\"%s\" (%b) received VOIP frame from \"%s\" (%b) at distance=%f", me.getUsername(), me.canHearAll(), player.getUsername(), player.canHearAll(), Float.valueOf(distance)));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private VoiceManagerData.RadioData checkForNearbyRadios(VoiceManagerData radioData) {
        IsoPlayer me = IsoPlayer.getInstance();
        VoiceManagerData myRadioData = VoiceManagerData.get(me.onlineId);
        if (myRadioData.isCanHearAll) {
            myRadioData.radioData.get((int)0).lastReceiveDistance = 0.0f;
            return myRadioData.radioData.get(0);
        }
        ArrayList<VoiceManagerData.RadioData> arrayList = myRadioData.radioData;
        synchronized (arrayList) {
            for (int i = 1; i < myRadioData.radioData.size(); ++i) {
                ArrayList<VoiceManagerData.RadioData> arrayList2 = radioData.radioData;
                synchronized (arrayList2) {
                    for (int j = 1; j < radioData.radioData.size(); ++j) {
                        if (myRadioData.radioData.get((int)i).freq != radioData.radioData.get((int)j).freq) continue;
                        float dx = myRadioData.radioData.get((int)i).x - radioData.radioData.get((int)j).x;
                        float dy = myRadioData.radioData.get((int)i).y - radioData.radioData.get((int)j).y;
                        myRadioData.radioData.get((int)i).lastReceiveDistance = (float)Math.sqrt(dx * dx + dy * dy);
                        if (!(myRadioData.radioData.get((int)i).lastReceiveDistance < radioData.radioData.get((int)j).distance)) continue;
                        return myRadioData.radioData.get(i);
                    }
                    continue;
                }
            }
        }
        arrayList = myRadioData.radioData;
        synchronized (arrayList) {
            ArrayList<VoiceManagerData.RadioData> arrayList3 = radioData.radioData;
            synchronized (arrayList3) {
                if (!radioData.radioData.isEmpty() && !myRadioData.radioData.isEmpty()) {
                    float dx = myRadioData.radioData.get((int)0).x - radioData.radioData.get((int)0).x;
                    float dy = myRadioData.radioData.get((int)0).y - radioData.radioData.get((int)0).y;
                    myRadioData.radioData.get((int)0).lastReceiveDistance = (float)Math.sqrt(dx * dx + dy * dy);
                    if (myRadioData.radioData.get((int)0).lastReceiveDistance < radioData.radioData.get((int)0).distance) {
                        return myRadioData.radioData.get(0);
                    }
                }
            }
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void UpdateChannelsRoaming(UdpConnection connection) {
        IsoPlayer me = IsoPlayer.getInstance();
        if (me.onlineId == -1) {
            return;
        }
        VoiceManagerData myRadioData = VoiceManagerData.get(me.onlineId);
        boolean isCanHearAll = false;
        ArrayList<VoiceManagerData.RadioData> arrayList = myRadioData.radioData;
        synchronized (arrayList) {
            myRadioData.radioData.clear();
            HashSet<Integer> tmpDeviceIDs = new HashSet<Integer>();
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null) continue;
                isCanHearAll |= player.canHearAll();
                myRadioData.radioData.add(new VoiceManagerData.RadioData(RakVoice.GetMaxDistance(), player.getX(), player.getY()));
                for (int j = 0; j < player.getInventory().getItems().size(); ++j) {
                    Radio radio;
                    DeviceData deviceData;
                    InventoryItem item = player.getInventory().getItems().get(j);
                    if (!(item instanceof Radio) || (deviceData = (radio = (Radio)item).getDeviceData()) == null || !deviceData.getIsTurnedOn()) continue;
                    myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, player.getX(), player.getY()));
                }
                int x = (int)player.getX() - 4;
                while ((float)x < player.getX() + 5.0f) {
                    int y = (int)player.getY() - 4;
                    while ((float)y < player.getY() + 5.0f) {
                        for (int z = player.getZi() - 1; z < player.getZi() + 1; ++z) {
                            DeviceData deviceData;
                            VehiclePart part;
                            IsoObject item;
                            int j;
                            IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
                            if (sq == null) continue;
                            if (sq.getObjects() != null) {
                                for (j = 0; j < sq.getObjects().size(); ++j) {
                                    Object id;
                                    IsoRadio isoRadio;
                                    DeviceData deviceData2;
                                    item = sq.getObjects().get(j);
                                    if (!(item instanceof IsoRadio) || (deviceData2 = (isoRadio = (IsoRadio)item).getDeviceData()) == null || !deviceData2.getIsTurnedOn()) continue;
                                    myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData2, (float)sq.x, (float)sq.y));
                                    if (item.getModData().isEmpty() || (id = item.getModData().rawget("RadioItemID")) == null || !(id instanceof Double)) continue;
                                    Double d = (Double)id;
                                    tmpDeviceIDs.add(d.intValue());
                                }
                            }
                            if (sq.getWorldObjects() != null) {
                                for (j = 0; j < sq.getWorldObjects().size(); ++j) {
                                    DeviceData deviceData3;
                                    item = sq.getWorldObjects().get(j);
                                    if (((IsoWorldInventoryObject)item).getItem() == null || !(((IsoWorldInventoryObject)item).getItem() instanceof Radio) || tmpDeviceIDs.contains(((IsoWorldInventoryObject)item).getItem().getID()) || (deviceData3 = ((Radio)((IsoWorldInventoryObject)item).getItem()).getDeviceData()) == null || !deviceData3.getIsTurnedOn()) continue;
                                    myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData3, (float)sq.x, (float)sq.y));
                                }
                            }
                            if (sq.getVehicleContainer() == null || sq != sq.getVehicleContainer().getSquare() || (part = sq.getVehicleContainer().getPartById("Radio")) == null || (deviceData = part.getDeviceData()) == null || !deviceData.getIsTurnedOn()) continue;
                            myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, (float)sq.x, (float)sq.y));
                        }
                        ++y;
                    }
                    ++x;
                }
            }
        }
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.SyncRadioData.doPacket(b);
        b.putBoolean(isCanHearAll);
        b.putInt(myRadioData.radioData.size() * 4);
        for (VoiceManagerData.RadioData data : myRadioData.radioData) {
            b.putInt(data.freq);
            b.putInt((int)data.distance);
            b.putInt(data.x);
            b.putInt(data.y);
        }
        PacketTypes.PacketType.SyncRadioData.send(connection);
    }

    void InitVMServer() {
        this.VoiceInitServer(ServerOptions.instance.voiceEnable.getValue(), 24000, 20, 5, 8000, ServerOptions.instance.voiceMinDistance.getValue(), ServerOptions.instance.voiceMaxDistance.getValue(), ServerOptions.instance.voice3d.getValue());
    }

    public int getMicVolumeIndicator() {
        if (fmodReceiveBuffer == null) {
            return 0;
        }
        return (int)this.fmodSoundData.loudness;
    }

    public boolean getMicVolumeError() {
        if (fmodReceiveBuffer == null) {
            return true;
        }
        return this.fmodSoundDataError == -1;
    }

    public boolean getServerVOIPEnable() {
        return serverVOIPEnable;
    }

    public void VMServerBan(short playerId, boolean isBan) {
        RakVoice.SetVoiceBan(playerId, isBan);
    }

    static {
        instance = new VoiceManager();
    }
}

