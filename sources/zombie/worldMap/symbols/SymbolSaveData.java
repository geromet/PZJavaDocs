/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.worldMap.symbols.WorldMapBaseSymbol;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapTextSymbol;

public final class SymbolSaveData {
    public int worldVersion;
    public int symbolsVersion;
    public final HashMap<String, Integer> fontNameToIndex = new HashMap();
    public final HashMap<Integer, String> indexToFontName = new HashMap();

    public SymbolSaveData(int worldVersion, int symbolsVersion) {
        this.worldVersion = worldVersion;
        this.symbolsVersion = symbolsVersion;
    }

    public void load(ByteBuffer input) throws IOException {
        int numFonts = input.get() & 0xFF;
        for (int i = 0; i < numFonts; ++i) {
            String fontID = GameWindow.ReadString(input);
            this.indexToFontName.put(i, fontID);
            this.fontNameToIndex.put(fontID, i);
        }
    }

    public void save(ByteBuffer output, WorldMapSymbols symbols) throws IOException {
        String[] fonts = this.initFontLookup(symbols);
        output.put((byte)fonts.length);
        for (int i = 0; i < fonts.length; ++i) {
            String fontID = fonts[i];
            GameWindow.WriteString(output, fontID);
        }
    }

    String[] initFontLookup(WorldMapSymbols symbols) {
        HashSet<String> fontSet = new HashSet<String>();
        for (int i = 0; i < symbols.getSymbolCount(); ++i) {
            WorldMapBaseSymbol symbol = symbols.getSymbolByIndex(i);
            if (!(symbol instanceof WorldMapTextSymbol)) continue;
            WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)symbol;
            fontSet.add(textSymbol.getLayerID());
        }
        String[] fonts = fontSet.toArray(new String[0]);
        for (int i = 0; i < fonts.length; ++i) {
            String fontID = fonts[i];
            this.indexToFontName.put(i, fontID);
            this.fontNameToIndex.put(fontID, i);
        }
        return fonts;
    }

    public void save(ByteBuffer output, WorldMapBaseSymbol symbol) throws IOException {
        this.worldVersion = 244;
        this.symbolsVersion = 2;
        String[] fonts = new String[]{};
        if (symbol instanceof WorldMapTextSymbol) {
            WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)symbol;
            String fontID = textSymbol.getLayerID();
            this.indexToFontName.put(0, fontID);
            this.fontNameToIndex.put(fontID, 0);
            fonts = new String[]{textSymbol.getLayerID()};
        }
        output.put((byte)fonts.length);
        for (int i = 0; i < fonts.length; ++i) {
            String fontName = fonts[i];
            GameWindow.WriteString(output, fontName);
        }
    }
}

