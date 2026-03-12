/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.characters.IsoGameCharacter;
import zombie.iso.ICurtain;

public interface ILockableDoor {
    public boolean isLockedByKey();

    public boolean IsOpen();

    public int getKeyId();

    public void setKeyId(int var1);

    public void setLockedByKey(boolean var1);

    public ICurtain HasCurtains();

    public boolean canAddCurtain();

    public boolean canClimbOver(IsoGameCharacter var1);

    public boolean couldBeOpen(IsoGameCharacter var1);
}

