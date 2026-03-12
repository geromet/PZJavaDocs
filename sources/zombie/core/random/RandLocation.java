/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.random;

import zombie.core.random.RandAbstract;
import zombie.iso.worldgen.WorldGenParams;

public class RandLocation
extends RandAbstract {
    public RandLocation(int x, int y) {
        this.rand = WorldGenParams.INSTANCE.getRandom(x, y);
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException();
    }
}

