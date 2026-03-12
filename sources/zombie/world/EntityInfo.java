/*
 * Decompiled with CFR 0.152.
 */
package zombie.world;

import zombie.world.DictionaryInfo;

public class EntityInfo
extends DictionaryInfo<EntityInfo> {
    @Override
    public String getInfoType() {
        return "entity";
    }

    @Override
    public EntityInfo copy() {
        EntityInfo c = new EntityInfo();
        c.copyFrom(this);
        return c;
    }
}

