/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.util.LinkedList;
import zombie.characters.IsoZombie;
import zombie.network.IConnection;

public class NetworkZombieList {
    final LinkedList<NetworkZombie> networkZombies = new LinkedList();
    public Object lock = new Object();

    public NetworkZombie getNetworkZombie(IConnection connection) {
        if (connection == null) {
            return null;
        }
        for (NetworkZombie nz : this.networkZombies) {
            if (nz.connection != connection) continue;
            return nz;
        }
        NetworkZombie nz = new NetworkZombie(connection);
        this.networkZombies.add(nz);
        return nz;
    }

    public static class NetworkZombie {
        public final LinkedList<IsoZombie> zombies = new LinkedList();
        final IConnection connection;

        public NetworkZombie(IConnection connection) {
            this.connection = connection;
        }
    }
}

