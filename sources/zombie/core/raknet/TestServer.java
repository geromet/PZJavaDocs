/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import java.nio.ByteBuffer;
import zombie.core.raknet.RakNetPeerInterface;

public class TestServer {
    static RakNetPeerInterface server;
    static ByteBuffer buf;

    static void main(String[] args2) {
        server = new RakNetPeerInterface();
        server.SetServerPort(12203, 12204);
        server.Init(false);
        int result = server.Startup(32);
        System.out.println("Result: " + result);
        server.SetMaximumIncomingConnections(32);
        server.SetOccasionalPing(true);
        server.SetIncomingPassword("spiffo");
        boolean bDone = false;
        while (true) {
            String test = "This is a test message";
            ByteBuffer buf = TestServer.Receive();
            TestServer.decode(buf);
        }
    }

    private static void decode(ByteBuffer buf) {
        int packetIdentifier = buf.get() & 0xFF;
        switch (packetIdentifier) {
            case 21: {
                System.out.println("ID_DISCONNECTION_NOTIFICATION");
                break;
            }
            case 19: {
                int id = buf.get() & 0xFF;
                long guid = server.getGuidFromIndex(id);
                break;
            }
            case 25: {
                System.out.println("ID_INCOMPATIBLE_PROTOCOL_VERSION");
                break;
            }
            case 0: 
            case 1: {
                System.out.println("PING");
                break;
            }
            case 22: {
                System.out.println("ID_CONNECTION_LOST");
                break;
            }
            default: {
                System.out.println("Other: " + packetIdentifier);
            }
        }
    }

    public static ByteBuffer Receive() {
        boolean bRead;
        do {
            try {
                Thread.sleep(1L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (!(bRead = server.Receive(buf)));
        return buf;
    }

    static {
        buf = ByteBuffer.allocate(2048);
    }
}

