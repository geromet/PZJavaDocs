/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.Styles;

import zombie.core.Styles.FloatList;
import zombie.core.Styles.ShortList;

public class GeometryData {
    private final FloatList vertexData;
    private final ShortList indexData;

    public GeometryData(FloatList vertexData, ShortList indexData) {
        this.vertexData = vertexData;
        this.indexData = indexData;
    }

    public void clear() {
        this.vertexData.clear();
        this.indexData.clear();
    }

    public FloatList getVertexData() {
        return this.vertexData;
    }

    public ShortList getIndexData() {
        return this.indexData;
    }
}

