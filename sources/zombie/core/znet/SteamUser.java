/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.core.znet.SteamUtils;

public class SteamUser {
    public static long GetSteamID() {
        if (SteamUtils.isSteamModeEnabled()) {
            return SteamUser.n_GetSteamID();
        }
        return 0L;
    }

    public static String GetSteamIDString() {
        if (SteamUtils.isSteamModeEnabled()) {
            long id = SteamUser.n_GetSteamID();
            return SteamUtils.convertSteamIDToString(id);
        }
        return null;
    }

    private static native long n_GetSteamID();
}

