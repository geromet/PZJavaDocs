/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.random;

import java.util.concurrent.ThreadLocalRandom;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.network.GameServer;

public interface RandInterface {
    public void init();

    public int Next(int var1);

    public long Next(long var1);

    public int Next(int var1, int var2);

    public long Next(long var1, long var3);

    public float Next(float var1, float var2);

    default public boolean NextBool(int invProbability) {
        return this.Next(invProbability) == 0;
    }

    default public int AdjustForFramerate(int chance) {
        chance = GameServer.server ? (int)((float)chance * 0.33333334f) : (int)((float)chance * ((float)PerformanceSettings.getLockFPS() / 30.0f));
        return chance;
    }

    default public boolean NextBool(float chance) {
        float chanceClamped = PZMath.clamp(chance, 0.0f, 1.0f);
        return ThreadLocalRandom.current().nextFloat() < chanceClamped;
    }
}

