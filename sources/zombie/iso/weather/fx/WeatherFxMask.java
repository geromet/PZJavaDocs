/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.weather.fx;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import org.joml.Vector2i;
import org.joml.Vector3f;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.iso.DiamondMatrixIterator;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.network.GameServer;
import zombie.worldMap.Rasterize;

public class WeatherFxMask {
    private static final boolean DEBUG_KEYS = false;
    private static TextureFBO fboMask;
    private static TextureFBO fboParticles;
    public static IsoSprite floorSprite;
    public static IsoSprite wallNSprite;
    public static IsoSprite wallWSprite;
    public static IsoSprite wallNWSprite;
    public static IsoSprite wallSESprite;
    private static Texture texWhite;
    private static boolean renderingMask;
    private static int curPlayerIndex;
    public static final int BIT_FLOOR = 0;
    public static final int BIT_WALLN = 1;
    public static final int BIT_WALLW = 2;
    public static final int BIT_IS_CUT = 4;
    public static final int BIT_CHARS = 8;
    public static final int BIT_OBJECTS = 16;
    public static final int BIT_WALL_SE = 32;
    public static final int BIT_DOOR = 64;
    public static float offsetX;
    public static float offsetY;
    public static ColorInfo defColorInfo;
    private static int diamondRows;
    public int x;
    public int y;
    public int z;
    public int flags;
    public IsoGridSquare gs;
    public boolean enabled;
    private static final PlayerFxMask[] playerMasks;
    private static final DiamondMatrixIterator dmiter;
    private static final Vector2i diamondMatrixPos;
    private static final RasterizeBounds tempRasterizeBounds;
    private static final RasterizeBounds[] rasterizeBounds;
    private static final Rasterize rasterize;
    private static IsoChunkMap rasterizeChunkMap;
    private static int rasterizeZ;
    private static final Vector3f tmpVec;
    private static final IsoGameCharacter.TorchInfo tmpTorch;
    private static final ColorInfo tmpColInfo;
    private static final int[] test;
    private static final String[] testNames;
    private static int var1;
    private static int var2;
    private static final float var3 = 1.0f;
    private static int scrMaskAdd;
    private static int dstMaskAdd;
    private static int scrMaskSub;
    private static int dstMaskSub;
    private static int scrParticles;
    private static int dstParticles;
    private static int scrMerge;
    private static int dstMerge;
    private static int scrFinal;
    private static int dstFinal;
    private static int idScrMaskAdd;
    private static int idDstMaskAdd;
    private static int idScrMaskSub;
    private static int idDstMaskSub;
    private static int idScrMerge;
    private static int idDstMerge;
    private static int idScrFinal;
    private static int idDstFinal;
    private static int idScrParticles;
    private static int idDstParticles;
    private static int targetBlend;
    private static boolean debugMask;
    public static boolean maskingEnabled;
    private static boolean debugMaskAndParticles;
    private static final boolean DEBUG_THROTTLE_KEYS = true;
    private static int keypause;

    public static TextureFBO getFboMask() {
        return fboMask;
    }

    public static TextureFBO getFboParticles() {
        return fboParticles;
    }

    public static boolean isRenderingMask() {
        return renderingMask;
    }

    public static void init() throws Exception {
        if (GameServer.server && !GameServer.guiCommandline) {
            return;
        }
        for (int i = 0; i < playerMasks.length; ++i) {
            WeatherFxMask.playerMasks[i] = new PlayerFxMask();
        }
        playerMasks[0].init();
        WeatherFxMask.initGlIds();
        floorSprite = IsoSpriteManager.instance.getSprite("floors_interior_tilesandwood_01_16");
        wallNSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_21");
        wallWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_20");
        wallNWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_22");
        wallSESprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_23");
        texWhite = Texture.getSharedTexture("media/textures/weather/fogwhite.png");
    }

    public static boolean checkFbos() {
        if (GameServer.server) {
            return false;
        }
        TextureFBO fbo = Core.getInstance().getOffscreenBuffer();
        if (Core.getInstance().getOffscreenBuffer() == null) {
            DebugLog.log("fbo=" + (fbo != null));
            return false;
        }
        int width = Core.getInstance().getScreenWidth();
        int height = Core.getInstance().getScreenHeight();
        if (fboMask == null || fboParticles == null || fboMask.getTexture().getWidth() != width || fboMask.getTexture().getHeight() != height) {
            Texture tex;
            if (fboMask != null) {
                fboMask.destroy();
            }
            if (fboParticles != null) {
                fboParticles.destroy();
            }
            fboMask = null;
            fboParticles = null;
            try {
                tex = new Texture(width, height, 16);
                fboMask = new TextureFBO(tex);
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
            try {
                tex = new Texture(width, height, 16);
                fboParticles = new TextureFBO(tex);
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "", LogSeverity.Error);
            }
            return fboMask != null && fboParticles != null;
        }
        return fboMask != null && fboParticles != null;
    }

    public static void destroy() {
        if (fboMask != null) {
            fboMask.destroy();
        }
        fboMask = null;
        if (fboParticles != null) {
            fboParticles.destroy();
        }
        fboParticles = null;
    }

    public static void initMask() {
        if (GameServer.server) {
            return;
        }
        curPlayerIndex = IsoCamera.frameState.playerIndex;
        playerMasks[curPlayerIndex].initMask();
    }

    private static boolean isOnScreen(int x, int y, int z) {
        float sx = (int)IsoUtils.XToScreenInt(x, y, z, 0);
        float sy = (int)IsoUtils.YToScreenInt(x, y, z, 0);
        sx -= (float)((int)IsoCamera.frameState.offX);
        sy -= (float)((int)IsoCamera.frameState.offY);
        if (sx + (float)(32 * Core.tileScale) <= 0.0f) {
            return false;
        }
        if (sy + (float)(32 * Core.tileScale) <= 0.0f) {
            return false;
        }
        if (sx - (float)(32 * Core.tileScale) >= (float)IsoCamera.frameState.offscreenWidth) {
            return false;
        }
        return !(sy - (float)(96 * Core.tileScale) >= (float)IsoCamera.frameState.offscreenHeight);
    }

    public boolean isLoc(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public static boolean playerHasMaskToDraw(int plrIndex) {
        if (plrIndex < playerMasks.length) {
            return WeatherFxMask.playerMasks[plrIndex].hasMaskToDraw;
        }
        return false;
    }

    public static void setDiamondIterDone(int plrIndex) {
        if (plrIndex < playerMasks.length) {
            WeatherFxMask.playerMasks[plrIndex].diamondIterDone = true;
        }
    }

    public static void forceMaskUpdate(int plrIndex) {
        if (plrIndex < playerMasks.length) {
            WeatherFxMask.playerMasks[plrIndex].plrSquare = null;
        }
    }

    public static void forceMaskUpdateAll() {
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < playerMasks.length; ++i) {
            WeatherFxMask.playerMasks[i].plrSquare = null;
        }
    }

    private static boolean getIsStairs(IsoGridSquare gs) {
        return gs != null && (gs.has(IsoObjectType.stairsBN) || gs.has(IsoObjectType.stairsBW) || gs.has(IsoObjectType.stairsMN) || gs.has(IsoObjectType.stairsMW) || gs.has(IsoObjectType.stairsTN) || gs.has(IsoObjectType.stairsTW));
    }

    private static boolean getHasDoor(IsoGridSquare gs) {
        if (gs != null && (gs.has(IsoFlagType.cutN) || gs.has(IsoFlagType.cutW)) && (gs.has(IsoFlagType.DoorWallN) || gs.has(IsoFlagType.DoorWallW)) && !gs.has(IsoFlagType.doorN) && !gs.has(IsoFlagType.doorW)) {
            return gs.getCanSee(curPlayerIndex);
        }
        return false;
    }

    public static void addMaskLocation(IsoGridSquare gs, int x, int y, int z) {
        if (GameServer.server) {
            return;
        }
        PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (!playerFxMask.requiresUpdate) {
            return;
        }
        if (!playerFxMask.hasMaskToDraw || playerFxMask.playerZ != z) {
            return;
        }
        IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex);
        if (WeatherFxMask.isInPlayerBuilding(gs, x, y, z)) {
            IsoGridSquare square = chunkMap.getGridSquare(x, y - 1, z);
            boolean connectN = !WeatherFxMask.isInPlayerBuilding(square, x, y - 1, z);
            square = chunkMap.getGridSquare(x - 1, y, z);
            boolean connectW = !WeatherFxMask.isInPlayerBuilding(square, x - 1, y, z);
            square = chunkMap.getGridSquare(x - 1, y - 1, z);
            boolean connectNW = !WeatherFxMask.isInPlayerBuilding(square, x - 1, y - 1, z);
            int walls = 0;
            if (connectN) {
                walls |= 1;
            }
            if (connectW) {
                walls |= 2;
            }
            if (connectNW) {
                walls |= 0x20;
            }
            boolean added = false;
            boolean isStairs = WeatherFxMask.getIsStairs(gs);
            if (gs != null && (connectN || connectW || connectNW)) {
                int charsAndObjects = 24;
                if (connectN && !gs.getProperties().has(IsoFlagType.WallN) && !gs.has(IsoFlagType.WallNW)) {
                    playerFxMask.addMask(x - 1, y, z, null, 8, false);
                    playerFxMask.addMask(x, y, z, gs, 24);
                    playerFxMask.addMask(x + 1, y, z, null, 24, false);
                    playerFxMask.addMask(x + 2, y, z, null, 8, false);
                    playerFxMask.addMask(x, y + 1, z, null, 8, false);
                    playerFxMask.addMask(x + 1, y + 1, z, null, 24, false);
                    playerFxMask.addMask(x + 2, y + 1, z, null, 24, false);
                    playerFxMask.addMask(x + 2, y + 2, z, null, 16, false);
                    playerFxMask.addMask(x + 3, y + 2, z, null, 16, false);
                    added = true;
                }
                if (connectW && !gs.getProperties().has(IsoFlagType.WallW) && !gs.getProperties().has(IsoFlagType.WallNW)) {
                    playerFxMask.addMask(x, y - 1, z, null, 8, false);
                    playerFxMask.addMask(x, y, z, gs, 24);
                    playerFxMask.addMask(x, y + 1, z, null, 24, false);
                    playerFxMask.addMask(x, y + 2, z, null, 8, false);
                    playerFxMask.addMask(x + 1, y, z, null, 8, false);
                    playerFxMask.addMask(x + 1, y + 1, z, null, 24, false);
                    playerFxMask.addMask(x + 1, y + 2, z, null, 24, false);
                    playerFxMask.addMask(x + 2, y + 2, z, null, 16, false);
                    playerFxMask.addMask(x + 2, y + 3, z, null, 16, false);
                    added = true;
                }
                if (connectNW) {
                    int flags = isStairs ? 24 : walls;
                    playerFxMask.addMask(x, y, z, gs, flags);
                    added = true;
                }
            }
            if (!added) {
                int flags = isStairs ? 24 : walls;
                playerFxMask.addMask(x, y, z, gs, flags);
            }
        } else {
            IsoGridSquare square = chunkMap.getGridSquare(x, y - 1, z);
            boolean connectN = WeatherFxMask.isInPlayerBuilding(square, x, y - 1, z);
            square = chunkMap.getGridSquare(x - 1, y, z);
            boolean connectW = WeatherFxMask.isInPlayerBuilding(square, x - 1, y, z);
            if (connectN || connectW) {
                int flags = 4;
                if (connectN) {
                    flags |= 1;
                }
                if (connectW) {
                    flags |= 2;
                }
                if (WeatherFxMask.getHasDoor(gs)) {
                    flags |= 0x40;
                }
                playerFxMask.addMask(x, y, z, gs, flags);
            } else {
                square = chunkMap.getGridSquare(x - 1, y - 1, z);
                if (WeatherFxMask.isInPlayerBuilding(square, x - 1, y - 1, z)) {
                    playerFxMask.addMask(x, y, z, gs, 4);
                }
            }
        }
    }

    private static boolean isInPlayerBuilding(IsoGridSquare gs, int x, int y, int z) {
        PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (gs != null && gs.has(IsoFlagType.solidfloor)) {
            if (gs.getBuilding() != null && gs.getBuilding() == playerFxMask.player.getBuilding()) {
                return true;
            }
            if (gs.getBuilding() == null) {
                return playerFxMask.curIsoWorldRegion != null && gs.getIsoWorldRegion() != null && gs.getIsoWorldRegion().isFogMask() && (gs.getIsoWorldRegion() == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(gs.getIsoWorldRegion()));
            }
        } else {
            if (WeatherFxMask.isInteriorLocation(x, y, z)) {
                return true;
            }
            if (gs != null && gs.getBuilding() == null) {
                return playerFxMask.curIsoWorldRegion != null && gs.getIsoWorldRegion() != null && gs.getIsoWorldRegion().isFogMask() && (gs.getIsoWorldRegion() == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(gs.getIsoWorldRegion()));
            }
            if (gs == null && playerFxMask.curIsoWorldRegion != null) {
                IWorldRegion mr = IsoRegions.getIsoWorldRegion(x, y, z);
                return mr != null && mr.isFogMask() && (mr == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(mr));
            }
        }
        return false;
    }

    private static boolean isInteriorLocation(int x, int y, int maxZ) {
        PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        for (int z = maxZ; z >= 0; --z) {
            IsoGridSquare square = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex).getGridSquare(x, y, z);
            if (square == null) continue;
            if (square.getBuilding() != null && square.getBuilding() == playerFxMask.player.getBuilding()) {
                return true;
            }
            if (!square.has(IsoFlagType.exterior)) continue;
            return false;
        }
        return false;
    }

    private static void scanForTilesOld(int nPlayer) {
        PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (playerFxMask.diamondIterDone) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[nPlayer];
        int maxZ = PZMath.fastfloor(player.getZ());
        boolean x1 = false;
        boolean y1 = false;
        int x2 = 0 + IsoCamera.getOffscreenWidth(nPlayer);
        int y2 = 0 + IsoCamera.getOffscreenHeight(nPlayer);
        float topLeftX = IsoUtils.XToIso(0.0f, 0.0f, 0.0f);
        float topRightY = IsoUtils.YToIso(x2, 0.0f, 0.0f);
        float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0f);
        float bottomLeftY = IsoUtils.YToIso(0.0f, y2, 6.0f);
        float topRightX = IsoUtils.XToIso(x2, 0.0f, 0.0f);
        int minY = (int)topRightY;
        int maxY = (int)bottomLeftY;
        int minX = (int)topLeftX;
        int maxX = (int)bottomRightX;
        diamondRows = (int)topRightX * 4;
        minY -= 2;
        dmiter.reset(maxX - (minX -= 2));
        Vector2i v = diamondMatrixPos;
        IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(nPlayer);
        while (dmiter.next(v)) {
            if (v == null) continue;
            IsoGridSquare square = chunkMap.getGridSquare(v.x + minX, v.y + minY, maxZ);
            if (square == null) {
                WeatherFxMask.addMaskLocation(null, v.x + minX, v.y + minY, maxZ);
                continue;
            }
            IsoChunk c = square.getChunk();
            if (c == null || !square.IsOnScreen()) continue;
            WeatherFxMask.addMaskLocation(square, v.x + minX, v.y + minY, maxZ);
        }
    }

    public static boolean checkVisibleSquares(int playerIndex, int z) {
        if (!WeatherFxMask.playerMasks[playerIndex].hasMaskToDraw) {
            return false;
        }
        if (rasterizeBounds[playerIndex] == null) {
            return true;
        }
        tempRasterizeBounds.calculate(playerIndex, z);
        return !tempRasterizeBounds.equals(rasterizeBounds[playerIndex]);
    }

    private static void scanForTiles(int nPlayer) {
        PlayerFxMask playerFxMask = playerMasks[nPlayer];
        if (playerFxMask.diamondIterDone) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[nPlayer];
        int playerZ = PZMath.fastfloor(player.getZ());
        if (rasterizeBounds[nPlayer] == null) {
            WeatherFxMask.rasterizeBounds[nPlayer] = new RasterizeBounds();
        }
        RasterizeBounds rb = rasterizeBounds[nPlayer];
        GameProfiler profiler = GameProfiler.getInstance();
        try (GameProfiler.ProfileArea profileArea = profiler.profile("Calc Bounds");){
            rb.calculate(nPlayer, playerZ);
        }
        if (Core.debug) {
            // empty if block
        }
        boolean bRender = false;
        try (GameProfiler.ProfileArea profileArea = profiler.profile("scanTriangle");){
            rasterizeChunkMap = IsoWorld.instance.getCell().getChunkMap(nPlayer);
            rasterizeZ = playerZ;
            rasterize.scanTriangle(rb.x1, rb.y1, rb.x2, rb.y2, rb.x4, rb.y4, 0, 100000, (vx, vy) -> {
                IsoGridSquare square = rasterizeChunkMap.getGridSquare(vx, vy, rasterizeZ);
                WeatherFxMask.addMaskLocation(square, vx, vy, rasterizeZ);
                if (bRender) {
                    LineDrawer.addRect((float)vx + 0.05f, (float)vy + 0.05f, rasterizeZ, 0.9f, 0.9f, 1.0f, 0.0f, 0.0f);
                }
            });
            rasterize.scanTriangle(rb.x2, rb.y2, rb.x3, rb.y3, rb.x4, rb.y4, 0, 100000, (vx, vy) -> {
                IsoGridSquare square = rasterizeChunkMap.getGridSquare(vx, vy, rasterizeZ);
                WeatherFxMask.addMaskLocation(square, vx, vy, rasterizeZ);
                if (bRender) {
                    LineDrawer.addRect(vx, vy, rasterizeZ, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f);
                }
            });
        }
        if (bRender) {
            LineDrawer.addLine(rb.x1, rb.y1, (float)rasterizeZ, rb.x2, rb.y2, (float)rasterizeZ, 1.0f, 1.0f, 1.0f, 0.5f);
            LineDrawer.addLine(rb.x2, rb.y2, (float)rasterizeZ, rb.x3, rb.y3, (float)rasterizeZ, 1.0f, 1.0f, 1.0f, 0.5f);
            LineDrawer.addLine(rb.x3, rb.y3, (float)rasterizeZ, rb.x4, rb.y4, (float)rasterizeZ, 1.0f, 1.0f, 1.0f, 0.5f);
            LineDrawer.addLine(rb.x1, rb.y1, (float)rasterizeZ, rb.x4, rb.y4, (float)rasterizeZ, 1.0f, 1.0f, 1.0f, 0.5f);
            float ox = IsoCamera.getOffX();
            float oy = IsoCamera.getOffY();
            LineDrawer.drawLine((float)rb.cx1 - ox, (float)rb.cy1 - oy, (float)rb.cx2 - ox, (float)rb.cy2 - oy, 1.0f, 1.0f, 1.0f, 0.5f, 2);
            LineDrawer.drawLine((float)rb.cx2 - ox, (float)rb.cy2 - oy, (float)rb.cx3 - ox, (float)rb.cy3 - oy, 1.0f, 1.0f, 1.0f, 0.5f, 2);
            LineDrawer.drawLine((float)rb.cx3 - ox, (float)rb.cy3 - oy, (float)rb.cx4 - ox, (float)rb.cy4 - oy, 1.0f, 1.0f, 1.0f, 0.5f, 2);
            LineDrawer.drawLine((float)rb.cx4 - ox, (float)rb.cy4 - oy, (float)rb.cx1 - ox, (float)rb.cy1 - oy, 1.0f, 1.0f, 1.0f, 0.5f, 2);
        }
    }

    private static void renderMaskFloor(int x, int y, int z) {
        floorSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
    }

    private static void renderMaskWall(IsoGridSquare square, int x, int y, int z, boolean n, boolean w, int playerIndex) {
        IsoDirections dir;
        IsoSprite sprite;
        int cutawayE;
        if (square == null) {
            return;
        }
        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        long currentTimeMillis = System.currentTimeMillis();
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
        int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
        int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
        int n2 = cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
        if (n && w) {
            sprite = wallNWSprite;
            dir = IsoDirections.NW;
        } else if (n) {
            sprite = wallNSprite;
            dir = IsoDirections.N;
        } else if (w) {
            sprite = wallWSprite;
            dir = IsoDirections.W;
        } else {
            sprite = wallSESprite;
            dir = IsoDirections.SE;
        }
        square.DoCutawayShaderSprite(sprite, dir, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE);
    }

    private static void renderMaskWallNoCuts(int x, int y, int z, boolean n, boolean w) {
        if (n && w) {
            wallNWSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else if (n) {
            wallNSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else if (w) {
            wallWSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else {
            wallSESprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        }
    }

    public static void renderFxMask(int nPlayer) {
        if (IsoCamera.frameState.camCharacterZ < 0.0f) {
            return;
        }
        if (!DebugOptions.instance.weather.fx.getValue()) {
            return;
        }
        if (GameServer.server) {
            return;
        }
        if (IsoWeatherFX.instance == null) {
            return;
        }
        if (LuaManager.thread != null && LuaManager.thread.step) {
            return;
        }
        if (!WeatherFxMask.playerMasks[nPlayer].maskEnabled) {
            return;
        }
        PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (!playerFxMask.maskEnabled) {
            return;
        }
        if (maskingEnabled && !WeatherFxMask.checkFbos()) {
            maskingEnabled = false;
        }
        if (!maskingEnabled || !playerFxMask.hasMaskToDraw) {
            if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
                SpriteRenderer.instance.glIgnoreStyles(true);
                IndieGL.glBlendFunc(770, 771);
                IsoWorld.instance.getCell().getWeatherFX().render();
                SpriteRenderer.instance.glIgnoreStyles(false);
            }
            return;
        }
        GameProfiler profiler = GameProfiler.getInstance();
        try (GameProfiler.ProfileArea profileArea = profiler.profile("scanForTiles");){
            WeatherFxMask.scanForTiles(nPlayer);
        }
        SpriteRenderer.instance.glIgnoreStyles(true);
        if (maskingEnabled) {
            profileArea = profiler.profile("drawFxMask");
            try {
                WeatherFxMask.drawFxMask(nPlayer);
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        if (debugMaskAndParticles) {
            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
            SpriteRenderer.instance.glClear(16640);
            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
        } else if (debugMask) {
            SpriteRenderer.instance.glClearColor(0, 255, 0, 255);
            SpriteRenderer.instance.glClear(16640);
            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
        }
        profileArea = profiler.profile("drawFxLayered");
        try {
            if (!RenderSettings.getInstance().getPlayerSettings(nPlayer).isExterior()) {
                WeatherFxMask.drawFxLayered(nPlayer, false, false, false);
            }
            if (IsoWeatherFX.instance.hasCloudsToRender()) {
                WeatherFxMask.drawFxLayered(nPlayer, true, false, false);
            }
            if (IsoWeatherFX.instance.hasFogToRender() && PerformanceSettings.fogQuality == 2) {
                WeatherFxMask.drawFxLayered(nPlayer, false, true, false);
            }
            if (Core.getInstance().getOptionRenderPrecipitation() == 1 && IsoWeatherFX.instance.hasPrecipitationToRender()) {
                WeatherFxMask.drawFxLayered(nPlayer, false, false, true);
            }
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        SpriteRenderer.glBlendfuncEnabled = true;
        SpriteRenderer.instance.glIgnoreStyles(false);
    }

    private static void drawFxMask(int nPlayer) {
        int ow = IsoCamera.getOffscreenWidth(nPlayer);
        int oh = IsoCamera.getOffscreenHeight(nPlayer);
        renderingMask = true;
        SpriteRenderer.instance.glBuffer(4, nPlayer);
        SpriteRenderer.instance.glDoStartFrameFx(ow, oh, nPlayer);
        IsoWorld.instance.getCell().DrawStencilMask();
        IndieGL.glDepthMask(true);
        IndieGL.enableDepthTest();
        SpriteRenderer.instance.glClearColor(0, 0, 0, 0);
        SpriteRenderer.instance.glClear(16640);
        SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
        IndieGL.glDepthMask(false);
        IndieGL.disableDepthTest();
        SpriteRenderer.instance.StartShader(0, nPlayer);
        WeatherFxMask[] masks = WeatherFxMask.playerMasks[nPlayer].masks;
        int maskPointer = WeatherFxMask.playerMasks[nPlayer].maskPointer;
        for (int i = 0; i < maskPointer; ++i) {
            boolean doChars;
            boolean wW;
            boolean wN;
            WeatherFxMask mask = masks[i];
            if (!mask.enabled) continue;
            if ((mask.flags & 4) == 4) {
                boolean door;
                SpriteRenderer.glBlendfuncEnabled = true;
                IndieGL.glBlendFunc(scrMaskSub, dstMaskSub);
                SpriteRenderer.instance.glBlendEquation(32779);
                IndieGL.enableAlphaTest();
                IndieGL.glAlphaFunc(516, 0.02f);
                SpriteRenderer.glBlendfuncEnabled = false;
                wN = (mask.flags & 1) == 1;
                wW = (mask.flags & 2) == 2;
                WeatherFxMask.renderMaskWall(mask.gs, mask.x, mask.y, mask.z, wN, wW, nPlayer);
                SpriteRenderer.glBlendfuncEnabled = true;
                SpriteRenderer.instance.glBlendEquation(32774);
                SpriteRenderer.glBlendfuncEnabled = false;
                boolean bl = door = (mask.flags & 0x40) == 64;
                if (!door || mask.gs == null) continue;
                SpriteRenderer.glBlendfuncEnabled = true;
                IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
                SpriteRenderer.glBlendfuncEnabled = false;
                mask.gs.RenderOpenDoorOnly();
                continue;
            }
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
            SpriteRenderer.glBlendfuncEnabled = false;
            WeatherFxMask.renderMaskFloor(mask.x, mask.y, mask.z);
            boolean doObjects = (mask.flags & 0x10) == 16;
            boolean bl = doChars = (mask.flags & 8) == 8;
            if (!doObjects) {
                wN = (mask.flags & 1) == 1;
                boolean bl2 = wW = (mask.flags & 2) == 2;
                if (wN || wW) {
                    WeatherFxMask.renderMaskWall(mask.gs, mask.x, mask.y, mask.z, wN, wW, nPlayer);
                } else if ((mask.flags & 0x20) == 32) {
                    WeatherFxMask.renderMaskWall(mask.gs, mask.x, mask.y, mask.z, false, false, nPlayer);
                }
            }
            if (doObjects && mask.gs != null) {
                mask.gs.RenderMinusFloorFxMask(mask.z + 1, false, false);
            }
            if (!doChars || mask.gs == null) continue;
            mask.gs.renderCharacters(mask.z + 1, false, false);
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
            SpriteRenderer.glBlendfuncEnabled = false;
        }
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.glBuffer(5, nPlayer);
        SpriteRenderer.instance.glDoEndFrameFx(nPlayer);
        renderingMask = false;
    }

    private static void drawFxLayered(int nPlayer, boolean doClouds, boolean doFog, boolean doPrecip) {
        Texture tex;
        int ox = IsoCamera.getOffscreenLeft(nPlayer);
        int oy = IsoCamera.getOffscreenTop(nPlayer);
        int ow = IsoCamera.getOffscreenWidth(nPlayer);
        int oh = IsoCamera.getOffscreenHeight(nPlayer);
        int sx = IsoCamera.getScreenLeft(nPlayer);
        int sy = IsoCamera.getScreenTop(nPlayer);
        int sw = IsoCamera.getScreenWidth(nPlayer);
        int sh = IsoCamera.getScreenHeight(nPlayer);
        IndieGL.glDepthMask(false);
        IndieGL.disableDepthTest();
        SpriteRenderer.instance.glBuffer(6, nPlayer);
        SpriteRenderer.instance.glDoStartFrameFx(ow, oh, nPlayer);
        if (!(doClouds || doFog || doPrecip)) {
            Color c = RenderSettings.getInstance().getMaskClearColorForPlayer(nPlayer);
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFuncSeparate(scrParticles, dstParticles, 1, 771);
            SpriteRenderer.glBlendfuncEnabled = false;
            SpriteRenderer.instance.renderi(texWhite, 0, 0, ow, oh, c.r, c.g, c.b, c.a, null);
            SpriteRenderer.glBlendfuncEnabled = true;
        } else if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFuncSeparate(scrParticles, dstParticles, 1, 771);
            SpriteRenderer.glBlendfuncEnabled = false;
            IsoWorld.instance.getCell().getWeatherFX().renderLayered(doClouds, doFog, doPrecip);
            SpriteRenderer.glBlendfuncEnabled = true;
        }
        if (maskingEnabled) {
            IndieGL.glBlendFunc(scrMerge, dstMerge);
            SpriteRenderer.instance.glBlendEquation(32779);
            ((Texture)fboMask.getTexture()).rendershader2(0.0f, 0.0f, ow, oh, sx, sy, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
            SpriteRenderer.instance.glBlendEquation(32774);
        }
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.glBuffer(7, nPlayer);
        SpriteRenderer.instance.glDoEndFrameFx(nPlayer);
        if (!debugMask && !debugMaskAndParticles || debugMaskAndParticles) {
            tex = (Texture)fboParticles.getTexture();
            IndieGL.glBlendFunc(scrFinal, dstFinal);
        } else {
            tex = (Texture)fboMask.getTexture();
            IndieGL.glBlendFunc(770, 771);
        }
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        float sx1 = (float)sx / (float)tex.getWidthHW();
        float ey1 = (float)sy / (float)tex.getHeightHW();
        float ex1 = (float)(sx + sw) / (float)tex.getWidthHW();
        float sy1 = (float)(sy + sh) / (float)tex.getHeightHW();
        SpriteRenderer.instance.render(tex, 0.0f, 0.0f, ow, oh, 1.0f, 1.0f, 1.0f, 1.0f, sx1, sy1, ex1, sy1, ex1, ey1, sx1, ey1);
        IndieGL.glDefaultBlendFunc();
    }

    private static void initGlIds() {
        for (int i = 0; i < test.length; ++i) {
            if (test[i] == scrMaskAdd) {
                idScrMaskAdd = i;
                continue;
            }
            if (test[i] == dstMaskAdd) {
                idDstMaskAdd = i;
                continue;
            }
            if (test[i] == scrMaskSub) {
                idScrMaskSub = i;
                continue;
            }
            if (test[i] == dstMaskSub) {
                idDstMaskSub = i;
                continue;
            }
            if (test[i] == scrParticles) {
                idScrParticles = i;
                continue;
            }
            if (test[i] == dstParticles) {
                idDstParticles = i;
                continue;
            }
            if (test[i] == scrMerge) {
                idScrMerge = i;
                continue;
            }
            if (test[i] == dstMerge) {
                idDstMerge = i;
                continue;
            }
            if (test[i] == scrFinal) {
                idScrFinal = i;
                continue;
            }
            if (test[i] != dstFinal) continue;
            idDstFinal = i;
        }
    }

    private static void updateDebugKeys() {
        if (keypause > 0) {
            --keypause;
        }
        if (keypause == 0) {
            boolean modechanged = false;
            boolean targetchanged = false;
            boolean debugchanged = false;
            boolean finalchanged = false;
            boolean domaskingchanged = false;
            if (targetBlend == 0) {
                var1 = idScrMaskAdd;
                var2 = idDstMaskAdd;
            } else if (targetBlend == 1) {
                var1 = idScrMaskSub;
                var2 = idDstMaskSub;
            } else if (targetBlend == 2) {
                var1 = idScrMerge;
                var2 = idDstMerge;
            } else if (targetBlend == 3) {
                var1 = idScrFinal;
                var2 = idDstFinal;
            } else if (targetBlend == 4) {
                var1 = idScrParticles;
                var2 = idDstParticles;
            }
            if (GameKeyboard.isKeyDown(79)) {
                if (--var1 < 0) {
                    var1 = test.length - 1;
                }
                modechanged = true;
            } else if (GameKeyboard.isKeyDown(81)) {
                if (++var1 >= test.length) {
                    var1 = 0;
                }
                modechanged = true;
            } else if (GameKeyboard.isKeyDown(75)) {
                if (--var2 < 0) {
                    var2 = test.length - 1;
                }
                modechanged = true;
            } else if (GameKeyboard.isKeyDown(77)) {
                if (++var2 >= test.length) {
                    var2 = 0;
                }
                modechanged = true;
            } else if (GameKeyboard.isKeyDown(71)) {
                if (--targetBlend < 0) {
                    targetBlend = 4;
                }
                modechanged = true;
                targetchanged = true;
            } else if (GameKeyboard.isKeyDown(73)) {
                if (++targetBlend >= 5) {
                    targetBlend = 0;
                }
                modechanged = true;
                targetchanged = true;
            } else if (maskingEnabled && GameKeyboard.isKeyDown(82)) {
                debugMask = !debugMask;
                modechanged = true;
                debugchanged = true;
            } else if (maskingEnabled && GameKeyboard.isKeyDown(80)) {
                debugMaskAndParticles = !debugMaskAndParticles;
                modechanged = true;
                finalchanged = true;
            } else if (!GameKeyboard.isKeyDown(72) && GameKeyboard.isKeyDown(76)) {
                maskingEnabled = !maskingEnabled;
                modechanged = true;
                domaskingchanged = true;
            }
            if (modechanged) {
                if (targetchanged) {
                    if (targetBlend == 0) {
                        DebugLog.log("TargetBlend = MASK_ADD");
                    } else if (targetBlend == 1) {
                        DebugLog.log("TargetBlend = MASK_SUB");
                    } else if (targetBlend == 2) {
                        DebugLog.log("TargetBlend = MERGE");
                    } else if (targetBlend == 3) {
                        DebugLog.log("TargetBlend = FINAL");
                    } else if (targetBlend == 4) {
                        DebugLog.log("TargetBlend = PARTICLES");
                    }
                } else if (debugchanged) {
                    DebugLog.log("DEBUG_MASK = " + debugMask);
                } else if (finalchanged) {
                    DebugLog.log("DEBUG_MASK_AND_PARTICLES = " + debugMaskAndParticles);
                } else if (domaskingchanged) {
                    DebugLog.log("MASKING_ENABLED = " + maskingEnabled);
                } else {
                    if (targetBlend == 0) {
                        idScrMaskAdd = var1;
                        idDstMaskAdd = var2;
                        scrMaskAdd = test[idScrMaskAdd];
                        dstMaskAdd = test[idDstMaskAdd];
                    } else if (targetBlend == 1) {
                        idScrMaskSub = var1;
                        idDstMaskSub = var2;
                        scrMaskSub = test[idScrMaskSub];
                        dstMaskSub = test[idDstMaskSub];
                    } else if (targetBlend == 2) {
                        idScrMerge = var1;
                        idDstMerge = var2;
                        scrMerge = test[idScrMerge];
                        dstMerge = test[idDstMerge];
                    } else if (targetBlend == 3) {
                        idScrFinal = var1;
                        idDstFinal = var2;
                        scrFinal = test[idScrFinal];
                        dstFinal = test[idDstFinal];
                    } else if (targetBlend == 4) {
                        idScrParticles = var1;
                        idDstParticles = var2;
                        scrParticles = test[idScrParticles];
                        dstParticles = test[idDstParticles];
                    }
                    DebugLog.log("Blendmode = " + testNames[var1] + " -> " + testNames[var2]);
                }
                keypause = 30;
            }
        }
    }

    static {
        offsetX = 32 * Core.tileScale;
        offsetY = 96 * Core.tileScale;
        defColorInfo = new ColorInfo();
        diamondRows = 1000;
        playerMasks = new PlayerFxMask[4];
        dmiter = new DiamondMatrixIterator(0);
        diamondMatrixPos = new Vector2i();
        tempRasterizeBounds = new RasterizeBounds();
        rasterizeBounds = new RasterizeBounds[4];
        rasterize = new Rasterize();
        tmpVec = new Vector3f();
        tmpTorch = new IsoGameCharacter.TorchInfo();
        tmpColInfo = new ColorInfo();
        test = new int[]{0, 1, 768, 769, 774, 775, 770, 771, 772, 773, 32769, 32770, 32771, 32772, 776, 35065, 35066, 34185, 35067};
        testNames = new String[]{"GL_ZERO", "GL_ONE", "GL_SRC_COLOR", "GL_ONE_MINUS_SRC_COLOR", "GL_DST_COLOR", "GL_ONE_MINUS_DST_COLOR", "GL_SRC_ALPHA", "GL_ONE_MINUS_SRC_ALPHA", "GL_DST_ALPHA", "GL_ONE_MINUS_DST_ALPHA", "GL_CONSTANT_COLOR", "GL_ONE_MINUS_CONSTANT_COLOR", "GL_CONSTANT_ALPHA", "GL_ONE_MINUS_CONSTANT_ALPHA", "GL_SRC_ALPHA_SATURATE", "GL_SRC1_COLOR (33)", "GL_ONE_MINUS_SRC1_COLOR (33)", "GL_SRC1_ALPHA (15)", "GL_ONE_MINUS_SRC1_ALPHA (33)"};
        var1 = 1;
        var2 = 1;
        scrMaskAdd = 770;
        dstMaskAdd = 771;
        scrMaskSub = 0;
        dstMaskSub = 0;
        scrParticles = 1;
        dstParticles = 771;
        scrMerge = 770;
        dstMerge = 771;
        scrFinal = 770;
        dstFinal = 771;
        maskingEnabled = true;
    }

    public static class PlayerFxMask {
        private WeatherFxMask[] masks;
        private int maskPointer;
        private boolean maskEnabled;
        private IsoGridSquare plrSquare;
        private int disabledMasks;
        private boolean requiresUpdate;
        private boolean hasMaskToDraw = true;
        private int playerIndex;
        private IsoPlayer player;
        private int playerZ;
        private IWorldRegion curIsoWorldRegion;
        private final ArrayList<IWorldRegion> curConnectedRegions = new ArrayList();
        private final ArrayList<IWorldRegion> isoWorldRegionTemp = new ArrayList();
        private final TLongObjectHashMap<WeatherFxMask> maskHashMap = new TLongObjectHashMap();
        private boolean diamondIterDone;
        private boolean isFirstSquare = true;
        private IsoGridSquare firstSquare;

        private void init() {
            this.masks = new WeatherFxMask[30000];
            for (int i = 0; i < this.masks.length; ++i) {
                if (this.masks[i] != null) continue;
                this.masks[i] = new WeatherFxMask();
            }
            this.maskEnabled = true;
        }

        private void initMask() {
            if (GameServer.server) {
                return;
            }
            if (!this.maskEnabled) {
                this.init();
            }
            this.playerIndex = IsoCamera.frameState.playerIndex;
            this.player = IsoPlayer.players[this.playerIndex];
            this.playerZ = PZMath.fastfloor(this.player.getZ());
            this.diamondIterDone = false;
            this.requiresUpdate = false;
            if (this.player != null) {
                if (this.isFirstSquare || this.plrSquare == null || this.plrSquare != this.player.getSquare()) {
                    this.plrSquare = this.player.getSquare();
                    this.maskPointer = 0;
                    this.maskHashMap.clear();
                    this.disabledMasks = 0;
                    this.requiresUpdate = true;
                    if (this.firstSquare == null) {
                        this.firstSquare = this.plrSquare;
                    }
                    if (this.firstSquare != null && this.firstSquare != this.plrSquare) {
                        this.isFirstSquare = false;
                    }
                }
                this.curIsoWorldRegion = this.player.getMasterRegion();
                this.curConnectedRegions.clear();
                if (this.curIsoWorldRegion != null && this.player.getMasterRegion().isFogMask()) {
                    this.isoWorldRegionTemp.clear();
                    this.isoWorldRegionTemp.add(this.curIsoWorldRegion);
                    while (!this.isoWorldRegionTemp.isEmpty()) {
                        IWorldRegion current = this.isoWorldRegionTemp.remove(0);
                        this.curConnectedRegions.add(current);
                        if (current.getNeighbors().isEmpty()) continue;
                        for (IsoWorldRegion neighbor : current.getNeighbors()) {
                            if (this.isoWorldRegionTemp.contains(neighbor) || this.curConnectedRegions.contains(neighbor) || !neighbor.isFogMask()) continue;
                            this.isoWorldRegionTemp.add(neighbor);
                        }
                    }
                } else {
                    this.curIsoWorldRegion = null;
                }
            }
            if (IsoWeatherFX.instance == null) {
                this.hasMaskToDraw = false;
                return;
            }
            this.hasMaskToDraw = true;
            if (this.hasMaskToDraw) {
                this.hasMaskToDraw = this.player.getSquare() != null && (this.player.getSquare().getBuilding() != null || !this.player.getSquare().has(IsoFlagType.exterior)) || this.curIsoWorldRegion != null && this.curIsoWorldRegion.isFogMask();
            }
        }

        private void addMask(int x, int y, int z, IsoGridSquare gs, int flags) {
            this.addMask(x, y, z, gs, flags, true);
        }

        private void addMask(int x, int y, int z, IsoGridSquare gs, int flags, boolean enabled) {
            WeatherFxMask locA;
            if (!this.hasMaskToDraw || !this.requiresUpdate) {
                return;
            }
            if (!this.maskEnabled) {
                this.init();
            }
            if ((locA = this.getMask(x, y, z)) == null) {
                WeatherFxMask mask = this.getFreeMask();
                mask.x = x;
                mask.y = y;
                mask.z = z;
                mask.flags = flags;
                mask.gs = gs;
                mask.enabled = enabled;
                if (!enabled && this.disabledMasks < diamondRows) {
                    ++this.disabledMasks;
                }
                this.maskHashMap.put((long)y << 32 | (long)x, mask);
            } else {
                if (locA.flags != flags) {
                    locA.flags |= flags;
                }
                if (!locA.enabled && enabled) {
                    WeatherFxMask mask = this.getFreeMask();
                    mask.x = x;
                    mask.y = y;
                    mask.z = z;
                    mask.flags = locA.flags;
                    mask.gs = gs;
                    mask.enabled = enabled;
                    this.maskHashMap.put((long)y << 32 | (long)x, mask);
                } else {
                    boolean bl = locA.enabled = locA.enabled ? locA.enabled : enabled;
                    if (enabled && gs != null && locA.gs == null) {
                        locA.gs = gs;
                    }
                }
            }
        }

        private WeatherFxMask getFreeMask() {
            if (this.maskPointer >= this.masks.length) {
                DebugLog.log("Weather Mask buffer out of bounds. Increasing cache.");
                WeatherFxMask[] old = this.masks;
                this.masks = new WeatherFxMask[this.masks.length + 10000];
                for (int i = 0; i < this.masks.length; ++i) {
                    this.masks[i] = i < old.length && old[i] != null ? old[i] : new WeatherFxMask();
                }
            }
            return this.masks[this.maskPointer++];
        }

        private boolean masksContains(int x, int y, int z) {
            return this.getMask(x, y, z) != null;
        }

        private WeatherFxMask getMask(int x, int y, int z) {
            return this.maskHashMap.get((long)y << 32 | (long)x);
        }
    }

    public static final class RasterizeBounds {
        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;
        int cx1;
        int cy1;
        int cx2;
        int cy2;
        int cx3;
        int cy3;
        int cx4;
        int cy4;

        public void calculate(int playerIndex, int z) {
            boolean x1 = false;
            boolean y1 = false;
            int x2 = 0 + IsoCamera.getOffscreenWidth(playerIndex);
            int y2 = 0 + IsoCamera.getOffscreenHeight(playerIndex);
            this.x1 = IsoUtils.XToIso(0.0f, 0.0f, z);
            this.y1 = IsoUtils.YToIso(0.0f, 0.0f, z);
            this.x2 = IsoUtils.XToIso(x2, 0.0f, z);
            this.y2 = IsoUtils.YToIso(x2, 0.0f, z);
            this.x3 = IsoUtils.XToIso(x2, y2, z);
            this.y3 = IsoUtils.YToIso(x2, y2, z);
            this.x4 = IsoUtils.XToIso(0.0f, y2, z);
            this.y4 = IsoUtils.YToIso(0.0f, y2, z);
            this.cx1 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x1) - 0.5f, (float)PZMath.fastfloor(this.y1) + 0.5f, -666.0f, -666);
            this.cy1 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x1) - 0.5f, (float)PZMath.fastfloor(this.y1) + 0.5f, -666.0f, -666);
            this.cx2 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x2) + 0.5f, (float)PZMath.fastfloor(this.y2) - 0.5f, -666.0f, -666);
            this.cy2 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x2) + 0.5f, (float)PZMath.fastfloor(this.y2) - 0.5f, -666.0f, -666);
            this.cx3 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x3) + 1.5f, (float)PZMath.fastfloor(this.y3) + 0.5f, -666.0f, -666);
            this.cy3 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x3) + 1.5f, (float)PZMath.fastfloor(this.y3) + 0.5f, -666.0f, -666);
            this.cx4 = (int)IsoUtils.XToScreen((float)PZMath.fastfloor(this.x4) + 0.5f, (float)PZMath.fastfloor(this.y4) + 1.5f, -666.0f, -666);
            this.cy4 = (int)IsoUtils.YToScreen((float)PZMath.fastfloor(this.x4) + 0.5f, (float)PZMath.fastfloor(this.y4) + 1.5f, -666.0f, -666);
            this.x3 += 3.0f;
            this.y3 += 3.0f;
            this.x4 += 3.0f;
            this.y4 += 3.0f;
        }

        public boolean equals(Object rhs) {
            if (!(rhs instanceof RasterizeBounds)) {
                return false;
            }
            RasterizeBounds rhs1 = (RasterizeBounds)rhs;
            return this.cx1 == rhs1.cx1 && this.cy1 == rhs1.cy1 && this.cx2 == rhs1.cx2 && this.cy2 == rhs1.cy2 && this.cx3 == rhs1.cx3 && this.cy3 == rhs1.cy3 && this.cx4 == rhs1.cx4 && this.cy4 == rhs1.cy4;
        }
    }
}

