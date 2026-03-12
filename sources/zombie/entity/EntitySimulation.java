/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.GameTime;

public class EntitySimulation {
    private static final long MILLIS_PER_TICK = 100L;
    private static final double SECONDS_PER_TICK = 0.1;
    private static long currentTimeMillis;
    private static int simulationTicksThisFrame;
    private static long lastTimeStamp;

    public static long getMillisPerTick() {
        return 100L;
    }

    public static double secondsPerTick() {
        return 0.1;
    }

    public static long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    public static int getSimulationTicksThisFrame() {
        return simulationTicksThisFrame;
    }

    public static double getGameSecondsPerTick() {
        return 2.4000000000000004;
    }

    protected static void update() {
        long millisPassed = (long)(GameTime.instance.getTimeDelta() * 1000.0f);
        long elapsed = (currentTimeMillis += millisPassed) - lastTimeStamp;
        if (elapsed >= 100L) {
            simulationTicksThisFrame = (int)(elapsed / 100L);
            lastTimeStamp = currentTimeMillis - (elapsed - (long)simulationTicksThisFrame * 100L);
        } else {
            simulationTicksThisFrame = 0;
        }
    }

    protected static void reset() {
        currentTimeMillis = 0L;
        simulationTicksThisFrame = 0;
        lastTimeStamp = 0L;
    }
}

