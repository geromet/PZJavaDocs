/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.io.Serializable;

class AlphaColorIndex
implements Serializable {
    byte alpha;
    byte blue;
    byte green;
    byte red;

    AlphaColorIndex(int red, int green, int blue, int alpha) {
        this.red = (byte)red;
        this.green = (byte)green;
        this.blue = (byte)blue;
        this.alpha = (byte)alpha;
    }
}

