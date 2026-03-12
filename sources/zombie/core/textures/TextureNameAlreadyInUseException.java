/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

public final class TextureNameAlreadyInUseException
extends RuntimeException {
    public TextureNameAlreadyInUseException(String name) {
        super("Texture Name " + name + " is already in use");
    }
}

