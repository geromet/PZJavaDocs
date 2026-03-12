/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.anticheats.AbstractAntiCheat;

public class AntiCheatChecksumUpdate
extends AbstractAntiCheat {
    @Override
    public boolean preUpdate(UdpConnection connection) {
        if (this.antiCheat.isEnabled() && connection.checksumState == UdpConnection.ChecksumState.Different && connection.checksumTime + 8000L < System.currentTimeMillis()) {
            DebugLog.log("timed out connection because checksum was different");
            connection.checksumState = UdpConnection.ChecksumState.Init;
            return false;
        }
        return true;
    }
}

