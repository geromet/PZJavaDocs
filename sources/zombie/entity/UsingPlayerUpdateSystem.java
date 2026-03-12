/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.characters.IsoPlayer;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class UsingPlayerUpdateSystem
extends EngineSystem {
    EntityBucket isoEntities;

    public UsingPlayerUpdateSystem(int updatePriority) {
        super(true, false, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.isoEntities = engine.getIsoObjectBucket();
    }

    @Override
    public void update() {
        if (GameClient.client) {
            return;
        }
        ImmutableArray<GameEntity> entities = this.isoEntities.getEntities();
        for (int i = 0; i < entities.size(); ++i) {
            GameEntity entity = entities.get(i);
            if (!entity.isValidEngineEntity()) continue;
            IsoPlayer usingPlayer = entity.getUsingPlayer();
            if (entity.getUsingPlayer() == null) continue;
            int distance = 10;
            if (!(usingPlayer.getX() < entity.getX() - 10.0f || usingPlayer.getX() > entity.getX() + 10.0f || usingPlayer.getY() < entity.getY() - 10.0f || usingPlayer.getY() > entity.getY() + 10.0f || usingPlayer.getZ() != entity.getZ()) && !usingPlayer.isDead()) continue;
            entity.setUsingPlayer(null);
        }
    }
}

