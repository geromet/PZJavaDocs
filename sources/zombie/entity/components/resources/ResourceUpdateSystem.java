/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.resources;

import java.util.List;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class ResourceUpdateSystem
extends EngineSystem {
    private EntityBucket resourcesEntities;

    public ResourceUpdateSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.resourcesEntities = engine.getBucket(Family.all(ComponentType.Resources).get());
    }

    private boolean isValidEntity(GameEntity entity) {
        return entity.isEntityValid() && entity.isValidEngineEntity();
    }

    @Override
    public void updateSimulation() {
        if (GameClient.client) {
            return;
        }
        ImmutableArray<GameEntity> entities = this.resourcesEntities.getEntities();
        if (entities.size() == 0) {
            return;
        }
        for (int i = 0; i < entities.size(); ++i) {
            Resources resources;
            GameEntity entity = entities.get(i);
            if (!this.isValidEntity(entity) || !(resources = (Resources)entity.getComponent(ComponentType.Resources)).isValid()) continue;
            List<Resource> resourcesArray = resources.getResources();
            for (int j = 0; j < resourcesArray.size(); ++j) {
                Resource resource = resourcesArray.get(j);
                if (resource.getType() != ResourceType.Energy || resource.isEmpty()) continue;
                ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
                if (!resource.isAutoDecay() || resource.isDirty()) continue;
                float amount = resourceEnergy.getEnergyCapacity() * 0.05f;
                resourceEnergy.setEnergyAmount(resourceEnergy.getEnergyAmount() - amount);
            }
            if (!resources.isDirty()) continue;
            resources.resetDirty();
        }
    }
}

