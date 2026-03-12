/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.characters;

public class AttackRateChecker {
    public long timestamp;
    public short count;

    public void set(long timestamp, short count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public void reset() {
        this.set(System.currentTimeMillis(), (short)0);
    }

    public boolean check(int maxProjectiles, int maxTimeMs) {
        this.count = (short)(this.count + 1);
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.timestamp > (long)maxTimeMs) {
            this.set(currentTime, (short)1);
        }
        return this.count > maxProjectiles;
    }
}

