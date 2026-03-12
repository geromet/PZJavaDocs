/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.styles;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.ui.UIFont;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.styles.IWorldMapStyleListener;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

public final class WorldMapStyle {
    private final ArrayList<WorldMapStyleLayer> layers = new ArrayList();
    private final HashMap<String, WorldMapStyleLayer> layerByName = new HashMap();
    private final ArrayList<IWorldMapStyleListener> listeners = new ArrayList();
    private final WorldMapTextStyleLayer defaultTextLayer = new WorldMapTextStyleLayer("text-note");

    public WorldMapStyle() {
        this.defaultTextLayer.font = UIFont.Handwritten;
        this.defaultTextLayer.lineHeight = 40;
        this.defaultTextLayer.fill.add(new WorldMapStyleLayer.ColorStop(0.0f, 0, 0, 0, 255));
    }

    public int getLayerCount() {
        return this.layers.size();
    }

    public WorldMapStyleLayer getLayerByIndex(int index) {
        return this.layers.get(index);
    }

    public WorldMapStyleLayer getLayerByID(String id) {
        return this.layerByName.getOrDefault(id, null);
    }

    public WorldMapTextStyleLayer getDefaultTextLayer() {
        return this.defaultTextLayer;
    }

    public WorldMapTextStyleLayer getTextStyleLayerOrDefault(String layerID) {
        WorldMapStyleLayer layer = this.getLayerByID(layerID);
        if (layer instanceof WorldMapTextStyleLayer) {
            WorldMapTextStyleLayer textLayer = (WorldMapTextStyleLayer)layer;
            return textLayer;
        }
        return this.getDefaultTextLayer();
    }

    public void addLayer(WorldMapStyleLayer layer) {
        this.layers.add(layer);
        this.layerByName.put(layer.id, layer);
        this.listeners.forEach(listener -> listener.onAdd(layer));
    }

    public void insertLayer(int index, WorldMapStyleLayer layer) {
        this.layers.add(index, layer);
        this.layerByName.put(layer.id, layer);
        this.listeners.forEach(listener -> listener.onAdd(layer));
    }

    public void removeLayer(WorldMapStyleLayer layer) {
        this.listeners.forEach(listener -> listener.onBeforeRemove(layer));
        this.layers.remove(layer);
        this.layerByName.remove(layer.id);
        this.listeners.forEach(listener -> listener.onAfterRemove(layer));
    }

    public WorldMapStyleLayer removeAt(int index) {
        WorldMapStyleLayer layer = this.getLayerByIndex(index);
        this.listeners.forEach(listener -> listener.onBeforeRemove(layer));
        this.layers.remove(layer);
        this.layerByName.remove(layer.id);
        this.listeners.forEach(listener -> listener.onAfterRemove(layer));
        return layer;
    }

    public void moveLayer(int indexFrom, int indexTo) {
        WorldMapStyleLayer layer = this.layers.remove(indexFrom);
        this.layers.add(indexTo, layer);
        this.listeners.forEach(listener -> listener.onMoveLayer(indexFrom, indexTo));
    }

    public void setLayerID(WorldMapStyleLayer layer, String id) {
        this.layerByName.remove(layer.id);
        layer.id = id;
        this.layerByName.put(layer.id, layer);
    }

    public int indexOf(WorldMapStyleLayer layer) {
        return this.layers.indexOf(layer);
    }

    public void copyFrom(WorldMapStyle other) {
        this.layers.clear();
        this.layerByName.clear();
        PZArrayUtil.addAll(this.layers, other.layers);
        this.layerByName.putAll(other.layerByName);
    }

    public void addListener(IWorldMapStyleListener listener) {
        if (listener == null) {
            return;
        }
        if (this.listeners.contains(listener)) {
            return;
        }
        this.listeners.add(listener);
    }

    public void clear() {
        this.listeners.forEach(IWorldMapStyleListener::onBeforeClear);
        this.layers.clear();
        this.layerByName.clear();
        this.listeners.forEach(IWorldMapStyleListener::onAfterClear);
    }
}

