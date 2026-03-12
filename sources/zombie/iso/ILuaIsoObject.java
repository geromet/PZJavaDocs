/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.iso.IsoDirections;

public interface ILuaIsoObject {
    default public void setDir(IsoDirections directions) {
        this.setForwardIsoDirection(directions);
    }

    public void setForwardIsoDirection(IsoDirections var1);
}

