/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

import zombie.Lua.LuaEventManager;
import zombie.core.znet.IJoinRequestCallback;
import zombie.core.znet.SteamUtils;

public class CallbackManager
implements IJoinRequestCallback {
    public CallbackManager() {
        SteamUtils.addJoinRequestCallback(this);
    }

    @Override
    public void onJoinRequest(long friendSteamID, String connectionString) {
        LuaEventManager.triggerEvent("OnAcceptInvite", connectionString);
    }
}

