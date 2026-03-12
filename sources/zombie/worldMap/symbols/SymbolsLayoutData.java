/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.joml.Quaternionf;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.symbols.SymbolLayout;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbolCollisions;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;

public final class SymbolsLayoutData {
    static final ObjectPool<SymbolLayout> s_layoutPool = new ObjectPool<SymbolLayout>(SymbolLayout::new);
    public final UIWorldMap ui;
    private long modificationCount = -1L;
    public final WorldMapSymbolCollisions collision = new WorldMapSymbolCollisions();
    public float worldScale;
    public final Quaternionf layoutRotation = new Quaternionf();
    public boolean isometric = true;
    public boolean miniMapSymbols;
    final HashMap<WorldMapBaseSymbol, SymbolLayout> symbolToLayout = new HashMap();

    public SymbolsLayoutData() {
        this.ui = null;
    }

    public SymbolsLayoutData(UIWorldMap ui) {
        this.ui = ui;
    }

    public boolean getMiniMapSymbols() {
        return this.miniMapSymbols;
    }

    public float getWorldScale() {
        return this.worldScale;
    }

    public void checkLayout() {
        SymbolLayout layout;
        WorldMapBaseSymbol symbol;
        int i;
        UIWorldMap ui = this.ui;
        WorldMapSymbols symbols = ui.getSymbolsDirect();
        if (symbols == null) {
            return;
        }
        Quaternionf q = ((Quaternionf)BaseVehicle.TL_quaternionf_pool.get().alloc()).setFromUnnormalized(ui.getAPI().getRenderer().getModelViewMatrix());
        if (this.modificationCount == symbols.getModificationCount() && this.worldScale == ui.getAPI().getWorldScale() && this.isometric == ui.getAPI().getBoolean("Isometric") && this.miniMapSymbols == ui.getAPI().getBoolean("MiniMapSymbols") && this.layoutRotation.equals(q)) {
            BaseVehicle.TL_quaternionf_pool.get().release(q);
            return;
        }
        this.modificationCount = symbols.getModificationCount();
        this.worldScale = ui.getAPI().getWorldScale();
        this.isometric = ui.getAPI().getBoolean("Isometric");
        this.miniMapSymbols = ui.getAPI().getBoolean("MiniMapSymbols");
        this.layoutRotation.set(q);
        BaseVehicle.TL_quaternionf_pool.get().release(q);
        float rox = ui.getAPI().worldOriginX();
        float roy = ui.getAPI().worldOriginY();
        s_layoutPool.releaseAll((List<SymbolLayout>)new ArrayList<SymbolLayout>(this.symbolToLayout.values()));
        this.symbolToLayout.clear();
        this.collision.boxes.clear();
        boolean collided = false;
        for (i = 0; i < symbols.getSymbolCount(); ++i) {
            symbol = symbols.getSymbolByIndex(i);
            layout = this.getLayout(symbol);
            symbol.layout(ui, this.collision, rox, roy, layout);
            collided |= layout.collided;
        }
        if (collided) {
            for (i = 0; i < symbols.getSymbolCount(); ++i) {
                symbol = symbols.getSymbolByIndex(i);
                layout = this.getLayout(symbol);
                if (layout.collided || !this.collision.isCollision(i)) continue;
                layout.collided = true;
            }
        }
    }

    SymbolLayout getLayout(WorldMapBaseSymbol symbol) {
        SymbolLayout layout = this.symbolToLayout.get(symbol);
        if (layout == null) {
            layout = s_layoutPool.alloc();
            this.symbolToLayout.put(symbol, layout);
        }
        return layout;
    }

    void initMainThread() {
        s_layoutPool.releaseAll((List<SymbolLayout>)new ArrayList<SymbolLayout>(this.symbolToLayout.values()));
        this.symbolToLayout.clear();
    }

    void initMainThread(WorldMapBaseSymbol originalSymbol, WorldMapBaseSymbol symbolCopy, SymbolsLayoutData originalLayoutData, WorldMapStyle styleCopy) {
        SymbolLayout layout = originalLayoutData.getLayout(originalSymbol);
        SymbolLayout layoutCopy = s_layoutPool.alloc().set(layout);
        if (symbolCopy instanceof WorldMapTextSymbol) {
            WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)symbolCopy;
            layoutCopy.textLayout.textLayer = styleCopy.getTextStyleLayerOrDefault(textSymbol.getLayerID());
        }
        this.symbolToLayout.put(symbolCopy, layoutCopy);
    }
}

