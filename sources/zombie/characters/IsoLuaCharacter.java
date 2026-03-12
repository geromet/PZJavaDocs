/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.IsoGameCharacter;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;

public final class IsoLuaCharacter
extends IsoGameCharacter {
    public IsoLuaCharacter(float x, float y, float z) {
        super(null, x, y, z);
        this.descriptor = SurvivorFactory.CreateSurvivor();
        this.descriptor.setInstance(this);
        SurvivorDesc desc = this.descriptor;
        this.InitSpriteParts(desc);
    }

    @Override
    public void update() {
    }
}

