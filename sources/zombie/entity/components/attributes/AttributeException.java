/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.attributes;

public class AttributeException
extends Exception {
    public AttributeException(String errorMessage) {
        super(errorMessage);
    }

    public AttributeException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}

