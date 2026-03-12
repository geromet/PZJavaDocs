/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.MetaEntity;

public class CustomBuckets {
    public static final String MetaEntities = "MetaEntities";
    public static final String NonMetaRenderers = "NonMetaRenderers";
    public static final String NonMetaEntities = "NonMetaEntities";
    public static final String NonMetaCraftLogic = "NonMetaCraftLogic";
    private static final Family nonMetaCraftLogicFamily = Family.all(ComponentType.CraftLogic).get();

    static void initializeCustomBuckets(Engine engine) {
        engine.registerCustomBucket(NonMetaRenderers, new NonMetaRenderersValidator());
        engine.registerCustomBucket(NonMetaEntities, new NonMetaEntitiesValidator());
        engine.registerCustomBucket(NonMetaCraftLogic, new NonMetaCraftLogicValidator());
        engine.registerCustomBucket(MetaEntities, new MetaEntitiesValidator());
    }

    private static class NonMetaRenderersValidator
    implements EntityBucket.EntityValidator {
        private NonMetaRenderersValidator() {
        }

        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return entity.hasRenderers() && !(entity instanceof MetaEntity);
        }
    }

    private static class NonMetaEntitiesValidator
    implements EntityBucket.EntityValidator {
        private NonMetaEntitiesValidator() {
        }

        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return !(entity instanceof MetaEntity);
        }
    }

    private static class NonMetaCraftLogicValidator
    implements EntityBucket.EntityValidator {
        private NonMetaCraftLogicValidator() {
        }

        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return nonMetaCraftLogicFamily.matches(entity) && !(entity instanceof MetaEntity);
        }
    }

    private static class MetaEntitiesValidator
    implements EntityBucket.EntityValidator {
        private MetaEntitiesValidator() {
        }

        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return entity instanceof MetaEntity;
        }
    }
}

