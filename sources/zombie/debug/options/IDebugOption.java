/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.options.IDebugOptionGroup;

public interface IDebugOption {
    public String getName();

    public IDebugOptionGroup getParent();

    public void setParent(IDebugOptionGroup var1);

    public void onFullPathChanged();
}

