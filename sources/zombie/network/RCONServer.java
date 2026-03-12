/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.debug.DebugLog;
import zombie.network.GameServer;

public class RCONServer {
    public static final int SERVERDATA_RESPONSE_VALUE = 0;
    public static final int SERVERDATA_AUTH_RESPONSE = 2;
    public static final int SERVERDATA_EXECCOMMAND = 2;
    public static final int SERVERDATA_AUTH = 3;
    private static RCONServer instance;
    private ServerSocket welcomeSocket;
    private ServerThread thread;
    private final String password;
    private final ConcurrentLinkedQueue<ExecCommand> toMain = new ConcurrentLinkedQueue();

    private RCONServer(int port, String password, boolean isLocal) {
        this.password = password;
        try {
            this.welcomeSocket = new ServerSocket();
            if (isLocal) {
                this.welcomeSocket.bind(new InetSocketAddress("127.0.0.1", port));
            } else if (GameServer.ipCommandline != null) {
                this.welcomeSocket.bind(new InetSocketAddress(GameServer.ipCommandline, port));
            } else {
                this.welcomeSocket.bind(new InetSocketAddress(port));
            }
            DebugLog.log("RCON: listening on port " + port);
        }
        catch (IOException ex) {
            DebugLog.log("RCON: error creating socket on port " + port);
            ex.printStackTrace();
            try {
                this.welcomeSocket.close();
                this.welcomeSocket = null;
            }
            catch (IOException ex2) {
                ex2.printStackTrace();
            }
            return;
        }
        this.thread = new ServerThread(this);
        this.thread.start();
    }

    private void updateMain() {
        ExecCommand command = this.toMain.poll();
        while (command != null) {
            command.update();
            command = this.toMain.poll();
        }
    }

    public void quit() {
        if (this.welcomeSocket != null) {
            try {
                this.welcomeSocket.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
            this.welcomeSocket = null;
            this.thread.quit();
            this.thread = null;
        }
    }

    public static void init(int port, String password, boolean isLocal) {
        instance = new RCONServer(port, password, isLocal);
    }

    public static void update() {
        if (instance != null) {
            instance.updateMain();
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.quit();
        }
    }

    private class ServerThread
    extends Thread {
        private final ArrayList<ClientThread> connections;
        public boolean quit;
        final /* synthetic */ RCONServer this$0;

        public ServerThread(RCONServer rCONServer) {
            RCONServer rCONServer2 = rCONServer;
            Objects.requireNonNull(rCONServer2);
            this.this$0 = rCONServer2;
            this.connections = new ArrayList();
            this.setName("RCONServer");
        }

        @Override
        public void run() {
            while (!this.quit) {
                this.runInner();
            }
        }

        private void runInner() {
            block4: {
                try {
                    Socket socket = this.this$0.welcomeSocket.accept();
                    for (int i = 0; i < this.connections.size(); ++i) {
                        ClientThread connection = this.connections.get(i);
                        if (connection.isAlive()) continue;
                        this.connections.remove(i--);
                    }
                    if (this.connections.size() >= 5) {
                        socket.close();
                        return;
                    }
                    DebugLog.DetailedInfo.trace("RCON: new connection " + socket.toString());
                    ClientThread connection = new ClientThread(socket, this.this$0.password);
                    this.connections.add(connection);
                    connection.start();
                }
                catch (IOException e) {
                    if (this.quit) break block4;
                    e.printStackTrace();
                }
            }
        }

        public void quit() {
            this.quit = true;
            while (this.isAlive()) {
                try {
                    Thread.sleep(50L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < this.connections.size(); ++i) {
                ClientThread connection = this.connections.get(i);
                connection.quit();
            }
        }
    }

    private static class ExecCommand {
        public int id;
        public String command;
        public String response;
        public ClientThread thread;

        public ExecCommand(int id, String command, ClientThread thread2) {
            this.id = id;
            this.command = command;
            this.thread = thread2;
        }

        public void update() {
            this.response = GameServer.rcon(this.command);
            if (this.thread.isAlive()) {
                this.thread.toThread.add(this);
            }
        }
    }

    private static class ClientThread
    extends Thread {
        public Socket socket;
        public boolean auth;
        public boolean quit;
        private final String password;
        private InputStream in;
        private OutputStream out;
        private final ConcurrentLinkedQueue<ExecCommand> toThread = new ConcurrentLinkedQueue();
        private int pendingCommands;

        public ClientThread(Socket socket, String password) {
            this.socket = socket;
            this.password = password;
            try {
                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            this.setName("RCONClient" + socket.getLocalPort());
        }

        @Override
        public void run() {
            if (this.in == null) {
                return;
            }
            if (this.out == null) {
                return;
            }
            while (!this.quit) {
                try {
                    this.runInner();
                }
                catch (SocketException ex) {
                    this.quit = true;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            try {
                this.socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            DebugLog.DetailedInfo.trace("RCON: connection closed " + this.socket.toString());
        }

        private void runInner() throws IOException {
            int packetSize;
            byte[] bytes = new byte[4];
            int receivedBytes = this.in.read(bytes, 0, 4);
            if (receivedBytes < 0) {
                this.quit = true;
                return;
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int remainingBytes = packetSize = bb.getInt();
            byte[] packetData = new byte[packetSize];
            do {
                if ((receivedBytes = this.in.read(packetData, packetSize - remainingBytes, remainingBytes)) >= 0) continue;
                this.quit = true;
                return;
            } while ((remainingBytes -= receivedBytes) > 0);
            bb = ByteBuffer.wrap(packetData);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int id = bb.getInt();
            int type = bb.getInt();
            String body = new String(bb.array(), bb.position(), bb.limit() - bb.position() - 2);
            this.handlePacket(id, type, body);
        }

        private void handlePacket(int id, int type, String body) throws IOException {
            if (!"players".equals(body)) {
                DebugLog.DetailedInfo.trace("RCON: ID=" + id + " Type=" + type + " Body='" + body + "' " + this.socket.toString());
            }
            switch (type) {
                case 3: {
                    this.auth = body.equals(this.password);
                    if (!this.auth) {
                        DebugLog.log("RCON: password doesn't match");
                        this.quit = true;
                    }
                    ByteBuffer bb = ByteBuffer.allocate(14);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(id);
                    bb.putInt(0);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
                    bb.clear();
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(this.auth ? id : -1);
                    bb.putInt(2);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
                    break;
                }
                case 2: {
                    if (!this.checkAuth()) break;
                    ExecCommand command = new ExecCommand(id, body, this);
                    ++this.pendingCommands;
                    RCONServer.instance.toMain.add(command);
                    while (this.pendingCommands > 0) {
                        command = this.toThread.poll();
                        if (command != null) {
                            --this.pendingCommands;
                            this.handleResponse(command);
                            continue;
                        }
                        try {
                            Thread.sleep(50L);
                        }
                        catch (InterruptedException ex) {
                            if (!this.quit) continue;
                            return;
                        }
                    }
                    break;
                }
                case 0: {
                    if (!this.checkAuth()) break;
                    ByteBuffer bb = ByteBuffer.allocate(14);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(id);
                    bb.putInt(0);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
                    this.out.write(bb.array());
                    break;
                }
                default: {
                    DebugLog.log("RCON: unknown packet Type=" + type);
                }
            }
        }

        public void handleResponse(ExecCommand command) {
            String s = command.response;
            if (s == null) {
                s = "";
            }
            ByteBuffer bb = ByteBuffer.allocate(12 + s.length() + 2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(bb.capacity() - 4);
            bb.putInt(command.id);
            bb.putInt(0);
            bb.put(s.getBytes());
            bb.putShort((short)0);
            try {
                this.out.write(bb.array());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean checkAuth() throws IOException {
            if (this.auth) {
                return true;
            }
            this.quit = true;
            ByteBuffer bb = ByteBuffer.allocate(14);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(bb.capacity() - 4);
            bb.putInt(-1);
            bb.putInt(2);
            bb.putShort((short)0);
            this.out.write(bb.array());
            return false;
        }

        public void quit() {
            if (this.socket != null) {
                try {
                    this.socket.close();
                }
                catch (IOException iOException) {
                    // empty catch block
                }
            }
            this.quit = true;
            this.interrupt();
            while (this.isAlive()) {
                try {
                    Thread.sleep(50L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

