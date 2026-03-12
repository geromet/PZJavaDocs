/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import zombie.worldMap.symbols.TextLayout;

public final class SymbolLayout {
    float x;
    float y;
    boolean collided;
    final TextLayout textLayout = new TextLayout();

    SymbolLayout set(SymbolLayout other) {
        this.collided = other.collided;
        this.x = other.x;
        this.y = other.y;
        this.textLayout.set(other.textLayout);
        return this;
    }
}

