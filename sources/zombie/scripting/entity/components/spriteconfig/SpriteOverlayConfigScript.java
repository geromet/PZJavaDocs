/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.entity.components.spriteconfig;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.world.ScriptsDictionary;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
public class SpriteOverlayConfigScript
extends ComponentScript {
    private final HashMap<String, OverlayStyle> allStyles = new HashMap();
    private final ArrayList<String> allStyleNames = new ArrayList();
    private boolean isValid;

    public OverlayStyle getStyle(String styleName) {
        return this.allStyles.get(styleName);
    }

    public ArrayList<String> getAllStyleNames() {
        return this.allStyleNames;
    }

    public boolean isValid() {
        return this.isValid;
    }

    private SpriteOverlayConfigScript() {
        super(ComponentType.SpriteOverlayConfig);
    }

    @Override
    public void getVersion(IVersionHash hash) {
        hash.add(this.getName());
        hash.add(String.valueOf(this.isValid()));
        if (this.isValid()) {
            for (OverlayStyle style : this.allStyles.values()) {
                style.getVersion(hash);
            }
        }
    }

    @Override
    public void PreReload() {
        this.allStyles.clear();
        this.allStyleNames.clear();
        this.isValid = false;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (loadMode == ScriptLoadMode.Init) {
            ScriptsDictionary.registerScript(this);
        }
    }

    protected void copyFrom(ComponentScript componentScript) {
        SpriteOverlayConfigScript other = (SpriteOverlayConfigScript)componentScript;
        this.isValid = other.isValid;
        this.allStyles.clear();
        for (String styleName : other.allStyles.keySet()) {
            this.allStyles.put(styleName, OverlayStyle.copy(other.allStyles.get(styleName)));
        }
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);
        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("style")) continue;
            String styleName = child.id;
            this.loadStyle(styleName, child);
        }
        this.checkScripts();
    }

    private void loadStyle(String styleName, ScriptParser.Block block) {
        for (ScriptParser.Block child : block.children) {
            if (!child.type.equals("progress")) continue;
            int progress = Integer.parseInt(child.id);
            String styleMapKey = styleName + "_" + Integer.toString(progress);
            if (this.allStyles.containsKey(styleMapKey)) {
                DebugLog.General.error("Duplicate style name and progress (not allowed): " + styleMapKey);
                continue;
            }
            this.loadFaces(styleMapKey, child);
        }
    }

    private void loadFaces(String styleName, ScriptParser.Block block) {
        OverlayStyle style = new OverlayStyle();
        style.styleName = styleName;
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("face") && !style.isSingleFace) {
                int faceID = SpriteConfigManager.GetFaceIdForString(child.id);
                if (faceID == -1) {
                    DebugLog.General.error("Cannot find face for: " + child.id);
                    continue;
                }
                FaceScript faceScript = new FaceScript();
                faceScript.faceName = child.id;
                faceScript.faceId = faceID;
                this.loadFace(faceScript, child);
                style.faces[faceID] = faceScript;
                if (!child.id.equalsIgnoreCase("single")) continue;
                style.isSingleFace = true;
                continue;
            }
            DebugLog.General.error("Unknown block '" + child.type + "' in entity iso script: " + this.getName());
        }
        this.allStyles.put(styleName, style);
    }

    private void warnOrError(String msg) {
        if (Core.debug) {
            throw new RuntimeException("[" + this.getName() + "] " + msg);
        }
        DebugLog.General.warn("[" + this.getName() + "] " + msg);
    }

    private void checkScripts() {
        for (OverlayStyle style : this.allStyles.values()) {
            if (style.isSingleFace) {
                for (int i = 0; i < 6; ++i) {
                    FaceScript faceScript = style.faces[i];
                    if (faceScript == null || faceScript.faceId == 0) continue;
                    this.warnOrError("SingleFace has other faces defined.");
                    style.faces[i] = null;
                }
            }
            boolean hasFace = false;
            int faceCount = 0;
            for (FaceScript faceScript : style.faces) {
                if (faceScript == null) continue;
                hasFace = true;
                ++faceCount;
                for (ZLayer layer : faceScript.layers) {
                    faceScript.totalHeight = PZMath.max(faceScript.totalHeight, layer.rows.size());
                    for (XRow row : layer.rows) {
                        faceScript.totalWidth = PZMath.max(faceScript.totalWidth, row.tiles.size());
                        for (TileScript tileScript : row.tiles) {
                            if (tileScript.tileName == null || style.allTileNames.contains(tileScript.tileName)) continue;
                            style.allTileNames.add(tileScript.tileName);
                        }
                    }
                }
                if (faceCount <= 1 && faceScript.totalHeight <= 1 && faceScript.totalWidth <= 1) continue;
                style.isMultiTile = true;
            }
            for (FaceScript faceScript : style.faces) {
                if (faceScript == null) continue;
                int complementaryFace = -1;
                switch (faceScript.faceId) {
                    case 0: {
                        complementaryFace = 4;
                        break;
                    }
                    case 4: {
                        complementaryFace = 0;
                        break;
                    }
                    case 1: {
                        complementaryFace = 5;
                        break;
                    }
                    case 5: {
                        complementaryFace = 1;
                    }
                }
                if (complementaryFace == -1 || style.getFace(complementaryFace) == null || faceScript.totalWidth == style.getFace((int)complementaryFace).totalWidth && faceScript.totalHeight == style.getFace((int)complementaryFace).totalHeight) continue;
                DebugLog.General.error("Complementary faces are different sizes. This is not supported. In entity iso script: " + this.getName());
            }
            if (!hasFace) continue;
            style.isValid = true;
        }
        this.isValid = true;
        for (OverlayStyle style : this.allStyles.values()) {
            this.allStyleNames.add(style.styleName);
            if (style.isValid()) continue;
            this.isValid = false;
        }
    }

    private void loadFace(FaceScript faceScript, ScriptParser.Block block) {
        ZLayer layer = new ZLayer();
        this.loadLayer(layer, block);
        if (!layer.rows.isEmpty()) {
            faceScript.layers.add(layer);
            return;
        }
        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("layer")) continue;
            this.loadLayer(layer, child);
            faceScript.layers.add(layer);
            layer = new ZLayer();
        }
    }

    private void loadLayer(ZLayer layer, ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String[] tiles;
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.equalsIgnoreCase("row")) continue;
            XRow row = new XRow();
            for (String tileName : tiles = val.split("\\s+")) {
                TileScript tileScript = new TileScript();
                if (tileName.equalsIgnoreCase("true") || tileName.equalsIgnoreCase("false")) {
                    boolean b = tileName.equalsIgnoreCase("true");
                    tileScript.isEmptySpace = true;
                    tileScript.blocksSquare = b;
                } else {
                    tileScript.tileName = tileName;
                    tileScript.isEmptySpace = false;
                    tileScript.blocksSquare = true;
                }
                row.tiles.add(tileScript);
            }
            layer.rows.add(row);
        }
    }

    @DebugClassFields
    public static class OverlayStyle {
        private String styleName;
        private int progress;
        private final FaceScript[] faces = new FaceScript[6];
        private boolean isSingleFace;
        private boolean isMultiTile;
        private boolean isValid;
        private final ArrayList<String> allTileNames = new ArrayList();

        public String getStyleName() {
            return this.styleName;
        }

        public int getProgress() {
            return this.progress;
        }

        public FaceScript getFace(int id) {
            return this.faces[id];
        }

        public ArrayList<String> getAllTileNames() {
            return this.allTileNames;
        }

        public boolean isSingleFace() {
            return this.isSingleFace;
        }

        public boolean isMultiTile() {
            return this.isMultiTile;
        }

        public boolean isValid() {
            return this.isValid;
        }

        private void getVersion(IVersionHash hash) {
            hash.add(this.styleName);
            hash.add(Integer.toString(this.progress));
            for (FaceScript face : this.faces) {
                if (face == null) continue;
                face.getVersion(hash);
            }
        }

        private static OverlayStyle copy(OverlayStyle other) {
            OverlayStyle style = new OverlayStyle();
            style.styleName = other.styleName;
            style.progress = other.progress;
            style.isSingleFace = other.isSingleFace;
            style.isMultiTile = other.isMultiTile;
            style.isValid = other.isValid;
            style.allTileNames.addAll(other.allTileNames);
            for (int i = 0; i < 6; ++i) {
                style.faces[i] = null;
                if (other.faces[i] == null) continue;
                style.faces[i] = FaceScript.copy(other.faces[i]);
            }
            return style;
        }
    }

    @DebugClassFields
    public static class FaceScript {
        private String faceName;
        private int faceId = -1;
        private int totalWidth;
        private int totalHeight;
        private final ArrayList<ZLayer> layers = new ArrayList();

        public String getFaceName() {
            return this.faceName.toLowerCase();
        }

        public int getTotalWidth() {
            return this.totalWidth;
        }

        public int getTotalHeight() {
            return this.totalHeight;
        }

        public int getZLayers() {
            return this.layers.size();
        }

        public ZLayer getLayer(int z) {
            return this.layers.get(z);
        }

        protected int getFaceID() {
            return this.faceId;
        }

        private void getVersion(IVersionHash hash) {
            hash.add(this.faceName);
            hash.add(String.valueOf(this.layers.size()));
            for (ZLayer layer : this.layers) {
                layer.getVersion(hash);
            }
        }

        private static FaceScript copy(FaceScript other) {
            FaceScript script = new FaceScript();
            script.faceName = other.faceName;
            script.faceId = other.faceId;
            script.totalWidth = other.totalWidth;
            script.totalHeight = other.totalHeight;
            for (ZLayer layer : other.layers) {
                script.layers.add(ZLayer.copy(layer));
            }
            return script;
        }
    }

    @DebugClassFields
    public static class ZLayer {
        private final ArrayList<XRow> rows = new ArrayList();

        public int getHeight() {
            return this.rows.size();
        }

        public XRow getRow(int y) {
            return this.rows.get(y);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getHeight()));
            for (XRow row : this.rows) {
                row.getVersion(hash);
            }
        }

        private static ZLayer copy(ZLayer other) {
            ZLayer layer = new ZLayer();
            for (XRow row : other.rows) {
                layer.rows.add(XRow.copy(row));
            }
            return layer;
        }
    }

    @DebugClassFields
    public static class XRow {
        private final ArrayList<TileScript> tiles = new ArrayList();

        public int getWidth() {
            return this.tiles.size();
        }

        public TileScript getTile(int x) {
            return this.tiles.get(x);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getWidth()));
            for (TileScript tile : this.tiles) {
                tile.getVersion(hash);
            }
        }

        private static XRow copy(XRow other) {
            XRow row = new XRow();
            for (TileScript tile : other.tiles) {
                row.tiles.add(TileScript.copy(tile));
            }
            return row;
        }
    }

    @DebugClassFields
    public static class TileScript {
        private String tileName;
        private boolean isEmptySpace;
        private boolean blocksSquare;

        public String getTileName() {
            return this.tileName;
        }

        public boolean isEmptySpace() {
            return this.isEmptySpace;
        }

        public boolean isBlocksSquare() {
            return this.blocksSquare;
        }

        private void getVersion(IVersionHash hash) {
            if (this.tileName != null) {
                hash.add(this.tileName);
            }
            hash.add(String.valueOf(this.isEmptySpace));
            hash.add(String.valueOf(this.blocksSquare));
        }

        private static TileScript copy(TileScript other) {
            TileScript script = new TileScript();
            script.tileName = other.tileName;
            script.isEmptySpace = other.isEmptySpace;
            script.blocksSquare = other.blocksSquare;
            return script;
        }
    }
}

