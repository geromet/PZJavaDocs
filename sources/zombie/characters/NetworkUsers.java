/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.Collection;
import zombie.characters.NetworkUser;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;

public class NetworkUsers {
    public static NetworkUsers instance = new NetworkUsers();
    public ArrayList<NetworkUser> users = new ArrayList();

    public ArrayList<NetworkUser> getUsers() {
        return this.users;
    }

    public NetworkUser getUser(String username) {
        for (NetworkUser user : this.users) {
            if (!username.equals(user.username)) continue;
            return user;
        }
        return null;
    }

    public static void send(ByteBufferWriter output, Collection<NetworkUser> usersInt) {
        output.putInt(usersInt.size());
        for (NetworkUser user : usersInt) {
            user.send(output);
        }
    }

    public void parse(ByteBufferReader input) {
        this.users.clear();
        int count = input.getInt();
        for (int i = 0; i < count; ++i) {
            NetworkUser user = new NetworkUser();
            user.parse(input);
            this.users.add(user);
        }
    }
}

