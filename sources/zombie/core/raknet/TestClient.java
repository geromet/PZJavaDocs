/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import java.nio.ByteBuffer;
import zombie.core.raknet.RakNetPeerInterface;

public class TestClient {
    static RakNetPeerInterface client;
    private static boolean connected;

    static void main(String[] args2) {
        client = new RakNetPeerInterface();
        client.Init(false);
        int result = client.Startup(1);
        System.out.println("Result: " + result);
        client.SetOccasionalPing(true);
        System.out.println("Client connecting: " + client.Connect("127.0.0.1", 12203, "spiffo", false));
        boolean bDone = false;
        ByteBuffer buf = ByteBuffer.allocate(500000);
        int n = 0;
        while (true) {
            ++n;
            while (client.Receive(buf)) {
                TestClient.decode(buf);
            }
            try {
                Thread.sleep(33L);
                continue;
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            break;
        }
    }

    private static void decode(ByteBuffer buf) {
        byte packetIdentifier = buf.get();
        switch (packetIdentifier) {
            case 21: {
                System.out.println("ID_DISCONNECTION_NOTIFICATION");
                break;
            }
            case 18: {
                System.out.println("ID_ALREADY_CONNECTED");
                break;
            }
            case 25: {
                System.out.println("ID_INCOMPATIBLE_PROTOCOL_VERSION");
                break;
            }
            case 31: {
                System.out.println("ID_REMOTE_DISCONNECTION_NOTIFICATION");
                break;
            }
            case 32: {
                System.out.println("ID_REMOTE_CONNECTION_LOST");
                break;
            }
            case 33: {
                System.out.println("ID_REMOTE_NEW_INCOMING_CONNECTION");
                break;
            }
            case 23: {
                System.out.println("ID_CONNECTION_BANNED");
                break;
            }
            case 17: {
                System.out.println("ID_CONNECTION_ATTEMPT_FAILED");
                break;
            }
            case 20: {
                System.out.println("ID_NO_FREE_INCOMING_CONNECTIONS");
                break;
            }
            case 24: {
                System.out.println("ID_INVALID_PASSWORD");
                break;
            }
            case 22: {
                System.out.println("ID_CONNECTION_LOST");
                break;
            }
            case 16: {
                System.out.println("ID_CONNECTION_REQUEST_ACCEPTED");
                connected = true;
                buf.clear();
                buf.put((byte)-122);
                for (int n = 0; n < 1000; ++n) {
                    buf.put((byte)-1);
                }
                System.out.println("Sending: " + client.Send(buf, 1, 3, (byte)0, 0L, false));
                break;
            }
            case 0: 
            case 1: {
                System.out.println("PING");
                break;
            }
        }
    }
}

