/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.random;

import org.uncommons.maths.random.CellularAutomatonRNG;
import org.uncommons.maths.random.SeedException;
import zombie.core.random.PZSeedGenerator;
import zombie.core.random.RandAbstract;

public class RandStandard
extends RandAbstract {
    public static final RandStandard INSTANCE = new RandStandard();

    protected RandStandard() {
    }

    @Override
    public void init() {
        try {
            this.rand = new CellularAutomatonRNG(new PZSeedGenerator());
        }
        catch (SeedException e) {
            e.printStackTrace();
        }
    }
}

