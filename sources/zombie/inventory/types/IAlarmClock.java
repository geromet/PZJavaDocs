/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.types;

public interface IAlarmClock {
    public void stopRinging();

    public void setAlarmSet(boolean var1);

    public boolean isAlarmSet();

    public void setHour(int var1);

    public void setMinute(int var1);

    public void setForceDontRing(int var1);

    public int getHour();

    public int getMinute();

    public void update();
}

