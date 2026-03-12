/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import zombie.asset.Asset;

public interface AssetStateObserver {
    public void onStateChanged(Asset.State var1, Asset.State var2, Asset var3);
}

