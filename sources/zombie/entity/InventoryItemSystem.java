/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.util.ImmutableArray;
import zombie.inventory.InventoryItem;

public class InventoryItemSystem
extends EngineSystem {
    EntityBucket itemEntities;

    public InventoryItemSystem(int updatePriority) {
        super(true, false, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.itemEntities = engine.getInventoryItemBucket();
    }

    @Override
    public void update() {
        ImmutableArray<GameEntity> entities = this.itemEntities.getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity entity = entities.get(i);
            InventoryItem item = (InventoryItem)entity;
            if (item.getEquipParent() != null && !item.getEquipParent().isDead()) continue;
            GameEntityManager.UnregisterEntity(entity);
        }
    }
}

