/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.Platform;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ZipLogs;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.ICoopServerMessageListener;

public class CoopMaster {
    private Process serverProcess;
    private Thread serverThread;
    private PrintStream serverCommandStream;
    private final List<String> incomingMessages = new LinkedList<String>();
    private final Pattern serverMessageParser;
    private TerminationReason serverTerminationReason;
    private Thread timeoutWatchThread;
    private boolean serverResponded;
    public static final CoopMaster instance = new CoopMaster();
    private String adminUsername;
    private final String adminPassword;
    private String serverName;
    private Long serverSteamId;
    private String serverIp;
    private Integer serverPort;
    private int autoCookie;
    private static final int autoCookieOffset = 1000000;
    private static final int maxAutoCookie = 1000000;
    private final List<Pair<ICoopServerMessageListener, ListenerOptions>> listeners = new LinkedList<Pair<ICoopServerMessageListener, ListenerOptions>>();

    private CoopMaster() {
        this.serverMessageParser = Pattern.compile("^([\\-\\w]+)(\\[(\\d+)\\])?@(.*)$");
        this.adminPassword = UUID.randomUUID().toString();
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void launchServer(String serverName, String username, int memory) throws IOException {
        this.launchServer(serverName, username, memory, false);
    }

    public void softreset(String serverName, String username, int memory) throws IOException {
        this.launchServer(serverName, username, memory, true);
    }

    private void launchServer(String serverName, String username, int memory, boolean softreset) throws IOException {
        String javaPath = Paths.get(System.getProperty("java.home"), "bin", "java").toAbsolutePath().toString();
        if (SteamUtils.isSteamModeEnabled()) {
            username = "admin";
        }
        ArrayList<String> command = new ArrayList<String>();
        command.add(javaPath);
        command.add("-Xms" + memory + "m");
        command.add("-Xmx" + memory + "m");
        command.add("-Djava.library.path=" + System.getProperty("java.library.path"));
        command.add("-Djava.class.path=" + System.getProperty("java.class.path"));
        command.add("-Duser.dir=" + System.getProperty("user.dir"));
        command.add("-Duser.home=" + System.getProperty("user.home"));
        command.add("-Dzomboid.znetlog=2");
        command.add("-Dzomboid.steam=" + (SteamUtils.isSteamModeEnabled() ? "1" : "0"));
        command.add("-Djava.awt.headless=true");
        command.add("-XX:-OmitStackTraceInFastThrow");
        String gc = this.getGarbageCollector();
        if (gc != null) {
            command.add(gc);
        }
        if (softreset) {
            command.add("-Dsoftreset");
        }
        if (Core.debug) {
            command.add("-Ddebug");
        }
        command.add("zombie.network.GameServer");
        command.add("-coop");
        command.add("-servername");
        this.serverName = serverName;
        command.add(this.serverName);
        command.add("-adminusername");
        this.adminUsername = username;
        command.add(this.adminUsername);
        command.add("-adminpassword");
        command.add(this.adminPassword);
        command.add("-cachedir=" + ZomboidFileSystem.instance.getCacheDir());
        ProcessBuilder pb = new ProcessBuilder(command);
        ZipLogs.addZipFile(false);
        this.serverTerminationReason = TerminationReason.NormalTermination;
        this.serverResponded = false;
        this.serverProcess = pb.start();
        this.serverCommandStream = new PrintStream(this.serverProcess.getOutputStream());
        this.serverThread = new Thread(ThreadGroups.Workers, this::readServer);
        this.serverThread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        this.serverThread.start();
        this.timeoutWatchThread = new Thread(ThreadGroups.Workers, this::watchServer);
        this.timeoutWatchThread.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        this.timeoutWatchThread.start();
    }

    private String getGarbageCollector() {
        try {
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            boolean useZGC = false;
            boolean useG1GC = false;
            for (String argument : arguments) {
                if ("-XX:+UseZGC".equals(argument)) {
                    useZGC = true;
                }
                if ("-XX:-UseZGC".equals(argument)) {
                    useZGC = false;
                }
                if ("-XX:+UseG1GC".equals(argument)) {
                    useG1GC = true;
                }
                if (!"-XX:-UseG1GC".equals(argument)) continue;
                useG1GC = false;
            }
            if (useZGC) {
                return "-XX:+UseZGC";
            }
            if (useG1GC) {
                return "-XX:+UseG1GC";
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return null;
    }

    private void readServer() {
        BufferedReader serverOutput = new BufferedReader(new InputStreamReader(this.serverProcess.getInputStream()));
        while (true) {
            try {
                int e = this.serverProcess.exitValue();
            }
            catch (IllegalThreadStateException e) {
                String line = null;
                try {
                    line = serverOutput.readLine();
                }
                catch (IOException e2) {
                    e2.printStackTrace();
                }
                if (line == null) continue;
                this.storeMessage(line);
                this.serverResponded = true;
                continue;
            }
            break;
        }
        this.storeMessage("process-status@terminated");
        ZipLogs.addZipFile(true);
    }

    public void abortServer() {
        this.serverProcess.destroy();
    }

    private void watchServer() {
        int timeout2 = 20;
        try {
            Thread.sleep(20000L);
            if (!this.serverResponded) {
                this.serverTerminationReason = TerminationReason.Timeout;
                this.abortServer();
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return this.serverThread != null && this.serverThread.isAlive();
    }

    public TerminationReason terminationReason() {
        return this.serverTerminationReason;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void storeMessage(String message) {
        List<String> list = this.incomingMessages;
        synchronized (list) {
            this.incomingMessages.add(message);
        }
    }

    public synchronized void sendMessage(String tag, String cookie, String payload) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        if (cookie == null) {
            stringBuilder.append("@");
        } else {
            stringBuilder.append("[");
            stringBuilder.append(cookie);
            stringBuilder.append("]@");
        }
        stringBuilder.append(payload);
        String message = stringBuilder.toString();
        if (this.serverCommandStream != null) {
            this.serverCommandStream.println(message);
            this.serverCommandStream.flush();
        }
    }

    public void sendMessage(String tag, String payload) {
        this.sendMessage(tag, null, payload);
    }

    public synchronized void invokeServer(String tag, String payload, ICoopServerMessageListener responseHandler) {
        this.autoCookie = (this.autoCookie + 1) % 1000000;
        String cookie = Integer.toString(1000000 + this.autoCookie);
        this.addListener(responseHandler, new ListenerOptions(this, tag, cookie, true));
        this.sendMessage(tag, cookie, payload);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getMessage() {
        String result = null;
        List<String> list = this.incomingMessages;
        synchronized (list) {
            if (!this.incomingMessages.isEmpty()) {
                result = this.incomingMessages.get(0);
                this.incomingMessages.remove(0);
                if (!"ping@ping".equals(result)) {
                    System.out.println("SERVER: " + result);
                }
            }
        }
        return result;
    }

    public void update() {
        String message;
        while ((message = this.getMessage()) != null) {
            Matcher matcher = this.serverMessageParser.matcher(message);
            if (matcher.find()) {
                String tag = matcher.group(1);
                String cookie = matcher.group(3);
                String payload = matcher.group(4);
                if (!tag.equals("ping")) {
                    LuaEventManager.triggerEvent("OnCoopServerMessage", tag, cookie, payload);
                }
                this.handleMessage(tag, cookie, payload);
                continue;
            }
            DebugLog.log(DebugType.Network, "[CoopMaster] Unknown message incoming from the slave server: " + message);
        }
    }

    private void handleMessage(String tag, String cookie, String payload) {
        if (Objects.equals(tag, "ping")) {
            this.sendMessage("ping", cookie, "pong");
        } else if (Objects.equals(tag, "steam-id")) {
            this.serverSteamId = Objects.equals(payload, "null") ? null : Long.valueOf(SteamUtils.convertStringToSteamID(payload));
        } else if (Objects.equals(tag, "server-address")) {
            DebugLog.log("Got server-address: " + payload);
            String pattern = "^(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)$";
            Pattern compiledPattern = Pattern.compile("^(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)$");
            Matcher matcher = compiledPattern.matcher(payload);
            if (matcher.find()) {
                String ipString = matcher.group(1);
                String portString = matcher.group(2);
                this.serverIp = ipString;
                this.serverPort = Integer.valueOf(portString);
                DebugLog.log("Successfully parsed: address = " + this.serverIp + ", port = " + this.serverPort);
            } else {
                DebugLog.log("Failed to parse server address");
            }
        }
        this.invokeListeners(tag, cookie, payload);
    }

    public void register(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        table.rawset("launch", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                boolean result;
                block7: {
                    block4: {
                        String arg2;
                        String arg1;
                        Object object;
                        block6: {
                            block5: {
                                result = false;
                                if (nArguments != 4) break block4;
                                object = callFrame.get(1);
                                if (!(object instanceof String)) break block5;
                                arg1 = (String)object;
                                object = callFrame.get(2);
                                if (!(object instanceof String)) break block5;
                                arg2 = (String)object;
                                object = callFrame.get(3);
                                if (object instanceof Double) break block6;
                            }
                            return 0;
                        }
                        Double arg3 = (Double)object;
                        try {
                            this.this$0.launchServer(arg1, arg2, arg3.intValue());
                            result = true;
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        break block7;
                    }
                    DebugLog.log(DebugType.Network, "[CoopMaster] wrong number of arguments: " + nArguments);
                }
                callFrame.push(result);
                return 1;
            }
        });
        table.rawset("softreset", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                boolean result;
                block7: {
                    block4: {
                        String arg2;
                        String arg1;
                        Object object;
                        block6: {
                            block5: {
                                result = false;
                                if (nArguments != 4) break block4;
                                object = callFrame.get(1);
                                if (!(object instanceof String)) break block5;
                                arg1 = (String)object;
                                object = callFrame.get(2);
                                if (!(object instanceof String)) break block5;
                                arg2 = (String)object;
                                object = callFrame.get(3);
                                if (object instanceof Double) break block6;
                            }
                            return 0;
                        }
                        Double arg3 = (Double)object;
                        try {
                            this.this$0.softreset(arg1, arg2, arg3.intValue());
                            result = true;
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                        break block7;
                    }
                    DebugLog.log(DebugType.Network, "[CoopMaster] wrong number of arguments: " + nArguments);
                }
                callFrame.push(result);
                return 1;
            }
        });
        table.rawset("isRunning", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                callFrame.push(this.this$0.isRunning());
                return 1;
            }
        });
        table.rawset("sendMessage", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object object;
                if (nArguments == 4) {
                    Object object2 = callFrame.get(1);
                    if (object2 instanceof String) {
                        String arg1 = (String)object2;
                        object2 = callFrame.get(2);
                        if (object2 instanceof String) {
                            String arg2 = (String)object2;
                            object2 = callFrame.get(3);
                            if (object2 instanceof String) {
                                String arg3 = (String)object2;
                                this.this$0.sendMessage(arg1, arg2, arg3);
                            }
                        }
                    }
                } else if (nArguments == 3 && (object = callFrame.get(1)) instanceof String) {
                    String arg1 = (String)object;
                    object = callFrame.get(2);
                    if (object instanceof String) {
                        String arg2 = (String)object;
                        this.this$0.sendMessage(arg1, arg2);
                    }
                }
                return 0;
            }
        });
        table.rawset("getAdminPassword", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                callFrame.push(this.this$0.adminPassword);
                return 1;
            }
        });
        table.rawset("getTerminationReason", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                callFrame.push(this.this$0.serverTerminationReason.toString());
                return 1;
            }
        });
        table.rawset("getSteamID", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                if (this.this$0.serverSteamId != null) {
                    callFrame.push(SteamUtils.convertSteamIDToString(this.this$0.serverSteamId));
                    return 1;
                }
                return 0;
            }
        });
        table.rawset("getAddress", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                callFrame.push(this.this$0.serverIp);
                return 1;
            }
        });
        table.rawset("getPort", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                callFrame.push(this.this$0.serverPort);
                return 1;
            }
        });
        table.rawset("abort", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                this.this$0.abortServer();
                return 0;
            }
        });
        table.rawset("getServerSaveFolder", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                callFrame.push(this.this$0.getServerSaveFolder((String)arg1));
                return 1;
            }
        });
        table.rawset("getPlayerSaveFolder", (Object)new JavaFunction(this){
            final /* synthetic */ CoopMaster this$0;
            {
                CoopMaster coopMaster = this$0;
                Objects.requireNonNull(coopMaster);
                this.this$0 = coopMaster;
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                callFrame.push(this.this$0.getPlayerSaveFolder((String)arg1));
                return 1;
            }
        });
        environment.rawset("CoopServer", (Object)table);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addListener(ICoopServerMessageListener listener, ListenerOptions options) {
        List<Pair<ICoopServerMessageListener, ListenerOptions>> list = this.listeners;
        synchronized (list) {
            this.listeners.add(new Pair<ICoopServerMessageListener, ListenerOptions>(this, listener, options));
        }
    }

    public void addListener(ICoopServerMessageListener listener) {
        this.addListener(listener, null);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeListener(ICoopServerMessageListener listener) {
        List<Pair<ICoopServerMessageListener, ListenerOptions>> list = this.listeners;
        synchronized (list) {
            int index;
            for (index = 0; index < this.listeners.size() && this.listeners.get((int)index).first != listener; ++index) {
            }
            if (index < this.listeners.size()) {
                this.listeners.remove(index);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void invokeListeners(String tag, String cookie, String payload) {
        List<Pair<ICoopServerMessageListener, ListenerOptions>> list = this.listeners;
        synchronized (list) {
            Iterator<Pair<ICoopServerMessageListener, ListenerOptions>> iterator2 = this.listeners.iterator();
            while (iterator2.hasNext()) {
                Pair<ICoopServerMessageListener, ListenerOptions> item = iterator2.next();
                ICoopServerMessageListener listener = (ICoopServerMessageListener)item.first;
                ListenerOptions options = (ListenerOptions)item.second;
                if (listener == null) continue;
                if (options == null) {
                    listener.OnCoopServerMessage(tag, cookie, payload);
                    continue;
                }
                if (options.tag != null && !options.tag.equals(tag) || options.cookie != null && !options.cookie.equals(cookie)) continue;
                if (options.autoRemove) {
                    iterator2.remove();
                }
                listener.OnCoopServerMessage(tag, cookie, payload);
            }
        }
    }

    public String getServerName() {
        return this.serverName;
    }

    public String getServerSaveFolder(String serverName) {
        return LuaManager.GlobalObject.sanitizeWorldName(serverName);
    }

    public String getPlayerSaveFolder(String serverName) {
        return LuaManager.GlobalObject.sanitizeWorldName(serverName + "_player");
    }

    public static enum TerminationReason {
        NormalTermination,
        Timeout;

    }

    public class ListenerOptions {
        public String tag;
        public String cookie;
        public boolean autoRemove;

        public ListenerOptions(CoopMaster this$0, String tag, String cookie, boolean autoRemove) {
            Objects.requireNonNull(this$0);
            this.tag = tag;
            this.cookie = cookie;
            this.autoRemove = autoRemove;
        }

        public ListenerOptions(CoopMaster this$0, String tag, String cookie) {
            this(this$0, tag, cookie, false);
        }

        public ListenerOptions(CoopMaster this$0, String tag) {
            this(this$0, tag, null, false);
        }
    }

    private class Pair<K, V> {
        private final K first;
        private final V second;

        public Pair(CoopMaster coopMaster, K first, V second) {
            Objects.requireNonNull(coopMaster);
            this.first = first;
            this.second = second;
        }

        public K getFirst() {
            return this.first;
        }

        public V getSecond() {
            return this.second;
        }
    }
}

