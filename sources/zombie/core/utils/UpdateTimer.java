/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.utils;

public class UpdateTimer {
    private long time;

    public UpdateTimer() {
        this.time = System.currentTimeMillis() + 3800L;
    }

    public UpdateTimer(long resetTime) {
        this.time = System.currentTimeMillis() + resetTime;
    }

    public void reset(long time) {
        this.time = System.currentTimeMillis() + time;
    }

    public boolean check() {
        return this.time != 0L && System.currentTimeMillis() + 200L >= this.time;
    }

    public long getTime() {
        return this.time;
    }
}

