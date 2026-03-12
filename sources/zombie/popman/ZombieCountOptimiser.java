/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.network.GameClient;
import zombie.network.statistics.data.GameStatistic;

public class ZombieCountOptimiser {
    private static int zombieCountForDelete;
    public static final int maxZombieCount = 500;
    public static final int minZombieDistance = 20;
    public static final ArrayList<IsoZombie> zombiesForDelete;

    private static boolean isOutside(IsoZombie zombie) {
        return zombie.getCurrentSquare() == null || !zombie.getCurrentSquare().isInARoom() && !zombie.getCurrentSquare().haveRoof;
    }

    public static void startCount() {
        zombieCountForDelete = (int)(1.0f * (float)Math.max(0, GameClient.IDToZombieMap.values().length - SandboxOptions.instance.zombieConfig.zombiesCountBeforeDeletion.getValue()));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void incrementZombie(IsoZombie zombie) {
        if (zombieCountForDelete > 0 && Rand.Next(10) == 0 && zombie.getTarget() == null && ZombieCountOptimiser.isOutside(zombie) && zombie.canBeDeletedUnnoticed(20.0f) && !zombie.isReanimatedPlayer()) {
            ArrayList<IsoZombie> arrayList = zombiesForDelete;
            synchronized (arrayList) {
                zombiesForDelete.add(zombie);
            }
            --zombieCountForDelete;
            GameStatistic.getInstance().zombiesCulled.increase();
        }
    }

    static {
        zombiesForDelete = new ArrayList();
    }
}

