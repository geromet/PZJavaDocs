/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.core.znet.SteamUtils;

public class SteamRemotePlay {
    private static native int n_GetSessionCount();

    public static int GetSessionCount() {
        if (SteamUtils.isSteamModeEnabled()) {
            return SteamRemotePlay.n_GetSessionCount();
        }
        return 0;
    }
}

