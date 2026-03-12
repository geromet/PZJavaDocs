/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

public interface IServerBrowserCallback {
    public void OnServerResponded(int var1);

    public void OnServerFailedToRespond(int var1);

    public void OnRefreshComplete();

    public void OnServerResponded(String var1, int var2);

    public void OnServerFailedToRespond(String var1, int var2);

    public void OnSteamRulesRefreshComplete(String var1, int var2);
}

