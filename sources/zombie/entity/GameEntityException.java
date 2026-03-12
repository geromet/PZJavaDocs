/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity;

import zombie.entity.GameEntity;

public class GameEntityException
extends Exception {
    public GameEntityException(String errorMessage) {
        super(errorMessage);
    }

    public GameEntityException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public GameEntityException(String errorMessage, GameEntity entity) {
        super((entity == null ? "[null]" : entity.getExceptionCompatibleString()) + " " + errorMessage);
    }

    public GameEntityException(String errorMessage, Throwable err, GameEntity entity) {
        super((entity == null ? "[null]" : entity.getExceptionCompatibleString()) + " " + errorMessage, err);
    }
}

