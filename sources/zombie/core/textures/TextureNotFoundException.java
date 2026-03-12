/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

public final class TextureNotFoundException
extends RuntimeException {
    public TextureNotFoundException(String name) {
        super("Image " + name + " not found! ");
    }
}

