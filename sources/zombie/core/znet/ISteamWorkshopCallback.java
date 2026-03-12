/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.znet;

public interface ISteamWorkshopCallback {
    public void onItemCreated(long var1, boolean var3);

    public void onItemNotCreated(int var1);

    public void onItemUpdated(boolean var1);

    public void onItemNotUpdated(int var1);

    public void onItemSubscribed(long var1);

    public void onItemNotSubscribed(long var1, int var3);

    public void onItemDownloaded(long var1);

    public void onItemNotDownloaded(long var1, int var3);

    public void onItemQueryCompleted(long var1, int var3);

    public void onItemQueryNotCompleted(long var1, int var3);
}

