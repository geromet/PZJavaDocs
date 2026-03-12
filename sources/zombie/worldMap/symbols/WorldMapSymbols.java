/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.animals.pathfind.Mesh;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.symbols.DoublePoint;
import zombie.worldMap.symbols.DoublePointPool;
import zombie.worldMap.symbols.IWorldMapSymbolListener;
import zombie.worldMap.symbols.MapSymbolDefinitions;
import zombie.worldMap.symbols.SymbolLayout;
import zombie.worldMap.symbols.SymbolSaveData;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapTextSymbol;
import zombie.worldMap.symbols.WorldMapTextureSymbol;

public class WorldMapSymbols {
    public static final int SAVEFILE_VERSION1 = 1;
    public static final int SAVEFILE_VERSION2 = 2;
    public static final int SAVEFILE_VERSION = 2;
    public static final float MIN_VISIBLE_ZOOM = 14.5f;
    public static final float COLLAPSED_RADIUS = 3.0f;
    private final ArrayList<WorldMapBaseSymbol> symbols = new ArrayList();
    private final ArrayList<IWorldMapSymbolListener> listeners = new ArrayList();
    private boolean userEditing;
    private long modificationCount;

    public static String getDefaultTextLayerID() {
        return "text-note";
    }

    public long getModificationCount() {
        return this.modificationCount;
    }

    public WorldMapTextSymbol addTranslatedText(String text, String layerID, float x, float y, float r, float g, float b, float a) {
        return this.addText(text, true, layerID, x, y, 0.0f, 0.0f, 0.666f, r, g, b, a);
    }

    public WorldMapTextSymbol addUntranslatedText(String text, String layerID, float x, float y, float r, float g, float b, float a) {
        return this.addText(text, false, layerID, x, y, 0.0f, 0.0f, 0.666f, r, g, b, a);
    }

    public WorldMapTextSymbol addText(String text, boolean translated, String layerID, float x, float y, float anchorX, float anchorY, float scale, float r, float g, float b, float a) {
        if (StringUtils.isNullOrWhitespace(layerID)) {
            layerID = WorldMapSymbols.getDefaultTextLayerID();
        }
        WorldMapTextSymbol symbol = new WorldMapTextSymbol(this);
        symbol.text = text;
        symbol.translated = translated;
        symbol.layerId = layerID;
        symbol.x = x;
        symbol.y = y;
        symbol.anchorX = PZMath.clamp(anchorX, 0.0f, 1.0f);
        symbol.anchorY = PZMath.clamp(anchorY, 0.0f, 1.0f);
        symbol.scale = scale;
        symbol.r = r;
        symbol.g = g;
        symbol.b = b;
        symbol.a = a;
        this.addSymbol(symbol);
        return symbol;
    }

    public WorldMapTextureSymbol addTexture(String symbolID, float x, float y, float r, float g, float b, float a) {
        return this.addTexture(symbolID, x, y, 0.0f, 0.0f, 0.666f, r, g, b, a);
    }

    public WorldMapTextureSymbol addTexture(String symbolID, float x, float y, float anchorX, float anchorY, float scale, float r, float g, float b, float a) {
        WorldMapTextureSymbol symbol = new WorldMapTextureSymbol(this);
        symbol.setSymbolID(symbolID);
        MapSymbolDefinitions.MapSymbolDefinition symbolDefinition = MapSymbolDefinitions.getInstance().getSymbolById(symbolID);
        if (symbolDefinition == null) {
            symbol.width = 18.0f;
            symbol.height = 18.0f;
        } else {
            symbol.texture = GameServer.server ? null : Texture.getSharedTexture(symbolDefinition.getTexturePath());
            symbol.width = symbolDefinition.getWidth();
            symbol.height = symbolDefinition.getHeight();
        }
        if (symbol.texture == null && !GameServer.server) {
            symbol.texture = Texture.getErrorTexture();
        }
        symbol.x = x;
        symbol.y = y;
        symbol.anchorX = PZMath.clamp(anchorX, 0.0f, 1.0f);
        symbol.anchorY = PZMath.clamp(anchorY, 0.0f, 1.0f);
        symbol.scale = scale;
        symbol.r = r;
        symbol.g = g;
        symbol.b = b;
        symbol.a = a;
        this.addSymbol(symbol);
        return symbol;
    }

    public void addSymbol(WorldMapBaseSymbol symbol) {
        if (this.symbols.contains(symbol)) {
            return;
        }
        ++this.modificationCount;
        this.symbols.add(symbol);
        this.listeners.forEach(listener -> listener.onAdd(symbol));
    }

    public int indexOf(WorldMapBaseSymbol symbol) {
        return this.symbols.indexOf(symbol);
    }

    public void removeSymbol(WorldMapBaseSymbol symbol) {
        int index = this.symbols.indexOf(symbol);
        if (index != -1) {
            this.removeSymbolByIndex(index);
        }
    }

    public void removeSymbolByIndex(int index) {
        ++this.modificationCount;
        WorldMapBaseSymbol symbol = this.symbols.get(index);
        this.listeners.forEach(listener -> listener.onBeforeRemove(symbol));
        this.symbols.remove(index);
        this.listeners.forEach(listener -> listener.onAfterRemove(symbol));
        symbol.release();
    }

    public boolean isUserEditing() {
        return this.userEditing;
    }

    public void setUserEditing(boolean b) {
        this.userEditing = b;
    }

    public void clear() {
        ++this.modificationCount;
        this.listeners.forEach(IWorldMapSymbolListener::onBeforeClear);
        for (int i = 0; i < this.symbols.size(); ++i) {
            this.symbols.get(i).release();
        }
        this.symbols.clear();
        this.listeners.forEach(IWorldMapSymbolListener::onAfterClear);
    }

    public void clearDefaultAnnotations() {
        for (int i = this.symbols.size() - 1; i >= 0; --i) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (symbol.isUserDefined()) continue;
            this.removeSymbolByIndex(i);
        }
    }

    public void clearUserAnnotations() {
        for (int i = this.symbols.size() - 1; i >= 0; --i) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (!symbol.isUserDefined()) continue;
            this.removeSymbolByIndex(i);
        }
    }

    public void invalidateLayout() {
        ++this.modificationCount;
    }

    public int getSymbolCount() {
        return this.symbols.size();
    }

    public WorldMapBaseSymbol getSymbolByIndex(int index) {
        return this.symbols.get(index);
    }

    boolean isSymbolVisible(UIWorldMap ui, WorldMapBaseSymbol symbol) {
        if (symbol.widthScaled(ui) < 10.0f || symbol.heightScaled(ui) < 10.0f) {
            return false;
        }
        return symbol.isVisible(ui);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    int hitTest(UIWorldMap ui, float uiX, float uiY) {
        ui.checkSymbolsLayout();
        float closestDist = Float.MAX_VALUE;
        int closestIndex = -1;
        DoublePoint leftTop = DoublePointPool.alloc();
        DoublePoint rightTop = DoublePointPool.alloc();
        DoublePoint rightBottom = DoublePointPool.alloc();
        DoublePoint leftBottom = DoublePointPool.alloc();
        try {
            for (int i = 0; i < this.symbols.size(); ++i) {
                float y2;
                float y1;
                float x2;
                float x1;
                float dist;
                WorldMapBaseSymbol symbol = this.symbols.get(i);
                if (!this.isSymbolVisible(ui, symbol) || !symbol.isUserDefined() && !ui.isMapEditor()) continue;
                SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(symbol);
                if (layout.collided && (dist = IsoUtils.DistanceToSquared(((x1 = layout.x + (symbol.widthScaled(ui) / 2.0f - 1.5f)) + (x2 = x1 + 6.0f)) / 2.0f, ((y1 = layout.y + (symbol.heightScaled(ui) / 2.0f - 1.5f)) + (y2 = y1 + 6.0f)) / 2.0f, uiX, uiY)) < closestDist) {
                    closestDist = dist;
                    closestIndex = i;
                }
                symbol.getOutlinePoints(ui, leftTop, rightTop, rightBottom, leftBottom);
                float z = 0.0f;
                if (Mesh.testPointInTriangle(uiX, uiY, 0.0f, (float)leftTop.x, (float)leftTop.y, 0.0f, (float)rightTop.x, (float)rightTop.y, 0.0f, (float)rightBottom.x, (float)rightBottom.y, 0.0f)) {
                    int n = i;
                    return n;
                }
                if (!Mesh.testPointInTriangle(uiX, uiY, 0.0f, (float)leftTop.x, (float)leftTop.y, 0.0f, (float)rightBottom.x, (float)rightBottom.y, 0.0f, (float)leftBottom.x, (float)leftBottom.y, 0.0f)) continue;
                int n = i;
                return n;
            }
        }
        finally {
            DoublePointPool.release(leftTop);
            DoublePointPool.release(rightTop);
            DoublePointPool.release(rightBottom);
            DoublePointPool.release(leftBottom);
        }
        if (closestIndex != -1 && closestDist < 100.0f) {
            return closestIndex;
        }
        return -1;
    }

    public void save(ByteBuffer output) throws IOException {
        WorldMapBaseSymbol symbol;
        int i;
        output.putShort((short)2);
        SymbolSaveData saveData = new SymbolSaveData(244, 2);
        saveData.save(output, this);
        int count = 0;
        for (i = 0; i < this.symbols.size(); ++i) {
            symbol = this.symbols.get(i);
            if (!symbol.isUserDefined() || symbol.getNetworkInfo() != null) continue;
            ++count;
        }
        output.putInt(count);
        for (i = 0; i < this.symbols.size(); ++i) {
            symbol = this.symbols.get(i);
            if (!symbol.isUserDefined() || symbol.getNetworkInfo() != null) continue;
            output.put((byte)symbol.getType().ordinal());
            symbol.save(output, saveData);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        short symbolsVersion = input.getShort();
        if (symbolsVersion < 1 || symbolsVersion > 2) {
            throw new IOException("unknown map symbols version " + symbolsVersion);
        }
        SymbolSaveData saveData = new SymbolSaveData(worldVersion, symbolsVersion);
        if (symbolsVersion >= 2) {
            saveData.load(input);
        }
        int symbolCount = input.getInt();
        for (int i = 0; i < symbolCount; ++i) {
            byte symbolType = input.get();
            if (symbolType == WorldMapSymbolType.Text.ordinal()) {
                WorldMapTextSymbol textSymbol = new WorldMapTextSymbol(this);
                textSymbol.load(input, saveData);
                this.symbols.add(textSymbol);
                continue;
            }
            if (symbolType == WorldMapSymbolType.Texture.ordinal()) {
                WorldMapTextureSymbol textureSymbol = new WorldMapTextureSymbol(this);
                textureSymbol.load(input, saveData);
                this.symbols.add(textureSymbol);
                continue;
            }
            throw new IOException("unknown map symbol type " + symbolType);
        }
    }

    public void addListener(IWorldMapSymbolListener listener) {
        this.listeners.add(listener);
    }

    public static enum WorldMapSymbolType {
        Text,
        Texture;

    }
}

