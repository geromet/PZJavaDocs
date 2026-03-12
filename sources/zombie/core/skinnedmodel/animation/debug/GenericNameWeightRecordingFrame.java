/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation.debug;

import zombie.core.skinnedmodel.animation.debug.GenericNameValueRecordingFrame;
import zombie.util.list.PZArrayUtil;

public class GenericNameWeightRecordingFrame
extends GenericNameValueRecordingFrame {
    private float[] weights = new float[0];

    public GenericNameWeightRecordingFrame(String fileKey) {
        super(fileKey, "_weights");
    }

    @Override
    protected void onColumnAdded() {
        this.weights = PZArrayUtil.add(this.weights, 0.0f);
    }

    public void logWeight(String name, int layer, float weight) {
        int columnIndex;
        int n = columnIndex = this.getOrCreateColumn(name, layer);
        this.weights[n] = this.weights[n] + weight;
    }

    public int getOrCreateColumn(String name, int layer) {
        String duplicateNameKey;
        Object layerKey = layer != 0 ? layer + ":" : "";
        String rawNameKey = String.format("%s%s", layerKey, name);
        int columnIndex = this.getOrCreateColumn(rawNameKey);
        if (this.weights[columnIndex] == 0.0f) {
            return columnIndex;
        }
        int d = 1;
        while (this.weights[columnIndex = this.getOrCreateColumn(duplicateNameKey = String.format("%s%s-%d", layerKey, name, d))] != 0.0f) {
            ++d;
        }
        return columnIndex;
    }

    public float getWeightAt(int i) {
        return this.weights[i];
    }

    @Override
    public String getValueAt(int i) {
        return String.valueOf(this.getWeightAt(i));
    }

    @Override
    public void reset() {
        int weightCount = this.weights.length;
        for (int i = 0; i < weightCount; ++i) {
            this.weights[i] = 0.0f;
        }
    }
}

