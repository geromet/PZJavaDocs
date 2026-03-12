/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.util.ArrayList;
import zombie.iso.IsoGridSquare;

public class IsoGridStack {
    public ArrayList<ArrayList<IsoGridSquare>> squares;

    public IsoGridStack(int count) {
        this.squares = new ArrayList(count);
        for (int i = 0; i < count; ++i) {
            this.squares.add(new ArrayList(5000));
        }
    }
}

