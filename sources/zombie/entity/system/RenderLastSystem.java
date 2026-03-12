/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.system;

import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

public class RenderLastSystem
extends EngineSystem {
    EntityBucket nonMetaRenderers;

    public RenderLastSystem(int renderPriority) {
        super(false, false, Integer.MAX_VALUE, true, renderPriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.nonMetaRenderers = engine.getCustomBucket("NonMetaRenderers");
    }

    @Override
    public void renderLast() {
        ImmutableArray<GameEntity> entities = this.nonMetaRenderers.getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity entity = entities.get(i);
            if (!entity.isValidEngineEntity() || !(entity instanceof IsoObject) && !(entity instanceof VehiclePart)) continue;
            entity.renderlastComponents();
        }
    }
}

