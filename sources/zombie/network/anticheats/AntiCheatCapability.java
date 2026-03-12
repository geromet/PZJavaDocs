/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.network.anticheats.AbstractAntiCheat;

public class AntiCheatCapability
extends AbstractAntiCheat {
    public static boolean validate(UdpConnection connection, Capability capability) {
        return connection.getRole().hasCapability(capability);
    }
}

