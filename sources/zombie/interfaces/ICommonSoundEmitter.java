/*
 * Decompiled with CFR 0.152.
 */
package zombie.interfaces;

public interface ICommonSoundEmitter {
    public void setPos(float var1, float var2, float var3);

    public long playSound(String var1);

    @Deprecated
    public long playSound(String var1, boolean var2);

    public void tick();

    public boolean isEmpty();

    public void setPitch(long var1, float var3);

    public void setVolume(long var1, float var3);

    public boolean hasSustainPoints(long var1);

    public void triggerCue(long var1);

    public int stopSound(long var1);

    public void stopOrTriggerSound(long var1);

    public void stopOrTriggerSoundLocal(long var1);

    public void stopOrTriggerSoundByName(String var1);

    public boolean isPlaying(long var1);

    public boolean isPlaying(String var1);
}

