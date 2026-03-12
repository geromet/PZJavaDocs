/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.types.HandWeapon;
import zombie.vehicles.BaseVehicle;

public interface ILuaGameCharacterDamage {
    public BodyDamage getBodyDamage();

    public BodyDamage getBodyDamageRemote();

    public float getHealth();

    public void setHealth(float var1);

    public float Hit(BaseVehicle var1, float var2, boolean var3, float var4, float var5, boolean var6, float var7, float var8);

    public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5);

    public float Hit(HandWeapon var1, IsoGameCharacter var2, float var3, boolean var4, float var5, boolean var6);

    public boolean isOnFire();

    public void StopBurning();

    public int getLastHitCount();

    public void setLastHitCount(int var1);

    public boolean addHole(BloodBodyPartType var1);

    public void addBlood(BloodBodyPartType var1, boolean var2, boolean var3, boolean var4);

    public boolean isBumped();

    public String getBumpType();

    public boolean isOnDeathDone();

    public void setOnDeathDone(boolean var1);

    public boolean isOnKillDone();

    public void setOnKillDone(boolean var1);

    public boolean isDeathDragDown();

    public void setDeathDragDown(boolean var1);

    public boolean isPlayingDeathSound();

    public void setPlayingDeathSound(boolean var1);
}

