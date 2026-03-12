/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.util.ArrayList;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.vehicles.BaseVehicle;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.symbols.SymbolsLayoutData;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;

public final class SymbolsRenderData {
    final ArrayList<WorldMapBaseSymbol> symbols = new ArrayList();
    boolean miniMapSymbols;
    boolean mapEditor;
    boolean userEditing;

    public void renderMain(UIWorldMap ui, WorldMapSymbols symbols, SymbolsLayoutData layoutData, WorldMapStyle styleCopy) {
        this.symbols.clear();
        this.miniMapSymbols = ui.getSymbolsLayoutData().getMiniMapSymbols();
        this.mapEditor = ui.isMapEditor();
        boolean bl = this.userEditing = ui.getSymbolsDirect() != null && ui.getSymbolsDirect().isUserEditing();
        if (symbols == null) {
            return;
        }
        ui.checkSymbolsLayout();
        layoutData.initMainThread();
        for (int i = 0; i < symbols.getSymbolCount(); ++i) {
            WorldMapBaseSymbol symbol = symbols.getSymbolByIndex(i);
            if (!symbols.isSymbolVisible(ui, symbol) || !symbol.isOnScreen(ui)) continue;
            if (Core.debug) {
                // empty if block
            }
            WorldMapBaseSymbol symbolCopy = symbol.createCopy();
            symbolCopy.setNetworkInfo(symbol.getNetworkInfo());
            this.symbols.add(symbolCopy);
            layoutData.initMainThread(symbol, symbolCopy, ui.getSymbolsLayoutData(), styleCopy);
        }
    }

    public void render(WorldMapRenderer.Drawer drawer, boolean bUserDefined) {
        VBORenderer.getInstance().flush();
        Matrix4f proj = BaseVehicle.allocMatrix4f();
        proj.setOrtho2D(0.0f, drawer.width, drawer.height, 0.0f);
        Matrix4f view = BaseVehicle.allocMatrix4f();
        view.identity();
        PZGLUtil.pushAndLoadMatrix(5889, proj);
        PZGLUtil.pushAndLoadMatrix(5888, view);
        BaseVehicle.releaseMatrix4f(proj);
        BaseVehicle.releaseMatrix4f(view);
        for (int i = 0; i < this.symbols.size(); ++i) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (symbol.isUserDefined() != bUserDefined) continue;
            symbol.render(drawer);
        }
        VBORenderer.getInstance().flush();
        PZGLUtil.popMatrix(5889);
        PZGLUtil.popMatrix(5888);
    }

    public void postRender() {
        for (int i = 0; i < this.symbols.size(); ++i) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            symbol.release();
        }
        this.symbols.clear();
    }
}

