/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.random;

import java.util.Random;
import zombie.core.random.RandAbstract;
import zombie.iso.worldgen.WorldGenParams;

public class RandSeeded
extends RandAbstract {
    public RandSeeded(long seed) {
        this.rand = new Random(seed);
    }

    @Override
    public void init() {
        this.rand = new Random(WorldGenParams.INSTANCE.getSeed());
    }
}

