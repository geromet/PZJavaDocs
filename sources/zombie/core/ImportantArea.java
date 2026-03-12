/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ImportantArea {
    public int sx;
    public int sy;
    public long lastUpdate = System.currentTimeMillis();

    public ImportantArea(int sx, int sy) {
        this.sx = sx;
        this.sy = sy;
    }

    public final void load(ByteBuffer input, int worldVersion) throws IOException {
        this.sx = input.getInt();
        this.sy = input.getInt();
        this.lastUpdate = System.currentTimeMillis();
    }

    public final void save(ByteBuffer output) throws IOException {
        output.putInt(this.sx);
        output.putInt(this.sy);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ImportantArea that = (ImportantArea)o;
        return this.sx == that.sx && this.sy == that.sy;
    }

    public int hashCode() {
        return Objects.hash(this.sx, this.sy);
    }
}

