/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.sprite;

import zombie.core.sprite.GenericSpriteRenderState;

public final class SpriteRenderStateUI
extends GenericSpriteRenderState {
    public boolean active;

    public SpriteRenderStateUI(int index) {
        super(index);
    }

    @Override
    public void clear() {
        try {
            this.active = true;
            super.clear();
        }
        finally {
            this.active = false;
        }
    }
}

