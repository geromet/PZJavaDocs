/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.joml.Vector2f;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.audio.FMODAmbientWalls;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.properties.IsoPropertyType;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.entity.util.TimSort;
import zombie.gameStates.DebugChunkState;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkLevel;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPuddlesGeometry;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.IsoWaterGeometry;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.WorldMarkers;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCorpses;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.fboRenderChunk.FBORenderSnow;
import zombie.iso.fboRenderChunk.FBORenderTrees;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaper;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.popman.ObjectPool;
import zombie.tileDepth.TileSeamManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vispoly.VisibilityPolygon2;

public final class FBORenderCell {
    public static final FBORenderCell instance = new FBORenderCell();
    private static final float BLACK_OUT_DIST = 10.0f;
    public static final boolean OUTLINE_DOUBLEDOOR_FRAMES = true;
    public static IsoObject lowestCutawayObjectW;
    public static IsoObject lowestCutawayObjectN;
    public IsoCell cell;
    public final ArrayList<IsoGridSquare> waterSquares = new ArrayList();
    public final ArrayList<IsoGridSquare> waterAttachSquares = new ArrayList();
    public final ArrayList<IsoGridSquare> fishSplashSquares = new ArrayList();
    public final ArrayList<IsoMannequin> mannequinList = new ArrayList();
    public boolean renderAnimatedAttachments;
    public boolean renderTranslucentOnly;
    public boolean renderDebugChunkState;
    private final PerPlayerData[] perPlayerData = new PerPlayerData[4];
    private long currentTimeMillis;
    private boolean windEffects;
    private boolean waterShader;
    private int puddlesQuality = -1;
    private float puddlesValue;
    private float wetGroundValue;
    private long puddlesRedrawTimeMs;
    private float snowFracTarget;
    private final TimSort timSort = new TimSort();
    private final int maxChunksPerFrame = 5;
    private final ColorInfo defColorInfo = new ColorInfo(1.0f, 1.0f, 1.0f, 1.0f);
    private final ArrayList<ArrayList<IsoFloorBloodSplat>> splatByType = new ArrayList();
    private final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects = new PZArrayList<IsoWorldInventoryObject>(IsoWorldInventoryObject.class, 16);
    private final PZArrayList<IsoGridSquare> tempSquares = new PZArrayList<IsoGridSquare>(IsoGridSquare.class, 64);
    private final ArrayList<IsoGameCharacter.Location> tempLocations = new ArrayList();
    private final ObjectPool<IsoGameCharacter.Location> locationPool = new ObjectPool<IsoGameCharacter.Location>(IsoGameCharacter.Location::new);
    private long delayedLoadingTimerMs;
    private boolean invalidateDelayedLoadingLevels;
    public static final PerformanceProfileProbe calculateRenderInfo;
    public static final PerformanceProfileProbe cutaways;
    public static final PerformanceProfileProbe fog;
    public static final PerformanceProfileProbe puddles;
    public static final PerformanceProfileProbe renderOneChunk;
    public static final PerformanceProfileProbe renderOneChunkLevel;
    public static final PerformanceProfileProbe renderOneChunkLevel2;
    public static final PerformanceProfileProbe translucentFloor;
    public static final PerformanceProfileProbe translucentNonFloor;
    public static final PerformanceProfileProbe updateLighting;
    public static final PerformanceProfileProbe water;
    public static final PerformanceProfileProbe tilesProbe;
    public static final PerformanceProfileProbe itemsProbe;
    public static final PerformanceProfileProbe movingObjectsProbe;
    public static final PerformanceProfileProbe shadowsProbe;
    public static final PerformanceProfileProbe visibilityProbe;
    public static final PerformanceProfileProbe translucentFloorObjectsProbe;
    public static final PerformanceProfileProbe translucentObjectsProbe;
    public static final boolean FIX_CORPSE_CLIPPING = true;
    public static final boolean FIX_ITEM_CLIPPING = true;
    public static final boolean FIX_JUMBO_CLIPPING = true;
    private final PZArrayList<IsoChunk> sortedChunks = new PZArrayList<IsoChunk>(IsoChunk.class, 121);
    public static float blackedOutRoomFadeBlackness;
    public static long blackedOutRoomFadeDurationMs;

    private FBORenderCell() {
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            this.perPlayerData[playerIndex] = new PerPlayerData(playerIndex);
        }
    }

    public void renderInternal() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        int playerZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        if (!PerformanceSettings.newRoofHiding) {
            if (this.cell.hideFloors[playerIndex] && this.cell.unhideFloorsCounter[playerIndex] > 0) {
                int n = playerIndex;
                this.cell.unhideFloorsCounter[n] = this.cell.unhideFloorsCounter[n] - 1;
            }
            if (this.cell.unhideFloorsCounter[playerIndex] <= 0) {
                this.cell.hideFloors[playerIndex] = false;
                this.cell.unhideFloorsCounter[playerIndex] = 60;
            }
        }
        boolean x1 = false;
        boolean y1 = false;
        int x2 = 0 + IsoCamera.getOffscreenWidth(playerIndex);
        int y2 = 0 + IsoCamera.getOffscreenHeight(playerIndex);
        float topLeftX = IsoUtils.XToIso(0.0f, 0.0f, 0.0f);
        float topRightY = IsoUtils.YToIso(x2, 0.0f, 0.0f);
        float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0f);
        float bottomLeftY = IsoUtils.YToIso(0.0f, y2, 6.0f);
        this.cell.minY = (int)topRightY;
        this.cell.maxY = (int)bottomLeftY;
        this.cell.minX = (int)topLeftX;
        this.cell.maxX = (int)bottomRightX;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.occludedGridX1 = this.cell.minX;
        perPlayerData1.occludedGridY1 = this.cell.minY;
        perPlayerData1.occludedGridX2 = this.cell.maxX;
        perPlayerData1.occludedGridY2 = this.cell.maxY;
        this.cell.minX -= 2;
        this.cell.minY -= 2;
        this.cell.minX -= this.cell.minX % 8;
        this.cell.minY -= this.cell.minY % 8;
        this.cell.maxX += 8 - this.cell.maxX % 8;
        this.cell.maxY += 8 - this.cell.maxY % 8;
        this.cell.maxZ = IsoCell.maxHeight;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            this.cell.maxZ = 1;
        }
        if (IsoPlayer.getInstance().getZ() < 0.0f) {
            this.cell.maxZ = (int)Math.ceil(IsoPlayer.getInstance().getZ()) + 1;
        }
        if (this.cell.minX != this.cell.lastMinX || this.cell.minY != this.cell.lastMinY) {
            this.cell.lightUpdateCount = 10;
        }
        if (!PerformanceSettings.newRoofHiding) {
            IsoGridSquare currentSq;
            IsoGridSquare isoGridSquare = currentSq = isoGameCharacter == null ? null : isoGameCharacter.getCurrentSquare();
            if (currentSq != null) {
                IsoGridSquare sq = this.cell.getGridSquare(Math.round(isoGameCharacter.getX()), Math.round(isoGameCharacter.getY()), playerZ);
                if (sq != null && this.cell.IsBehindStuff(sq)) {
                    this.cell.hideFloors[playerIndex] = true;
                }
                if (!this.cell.hideFloors[playerIndex] && currentSq.getProperties().has(IsoFlagType.hidewalls) || !currentSq.getProperties().has(IsoFlagType.exterior)) {
                    this.cell.hideFloors[playerIndex] = true;
                }
            }
            if (this.cell.hideFloors[playerIndex]) {
                this.cell.maxZ = playerZ + 1;
            }
        }
        this.cell.DrawStencilMask();
        long lastPlayerWindowPeekingRoomId = this.cell.playerWindowPeekingRoomId[playerIndex];
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoBuilding currentBuilding;
            this.cell.playerWindowPeekingRoomId[i] = -1L;
            IsoPlayer player2 = IsoPlayer.players[i];
            if (player2 == null || (currentBuilding = player2.getCurrentBuilding()) != null) continue;
            IsoDirections playerDir = IsoDirections.fromAngle(player2.getForwardDirection());
            currentBuilding = this.cell.GetPeekedInBuilding(player2.getCurrentSquare(), playerDir);
            if (currentBuilding == null) continue;
            this.cell.playerWindowPeekingRoomId[i] = this.cell.playerPeekedRoomId;
        }
        if (lastPlayerWindowPeekingRoomId != this.cell.playerWindowPeekingRoomId[playerIndex]) {
            IsoPlayer.players[playerIndex].dirtyRecalcGridStack = true;
        }
        if (isoGameCharacter != null && isoGameCharacter.getCurrentSquare() != null && isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.hidewalls)) {
            this.cell.maxZ = playerZ + 1;
        }
        this.cell.rendering = true;
        try {
            int maxHeight = playerZ < 0 ? playerZ : IsoCell.getInstance().chunkMap[playerIndex].maxHeight;
            int min = this.cell.chunkMap[playerIndex].minHeight;
            min = Math.max(min, playerZ);
            this.RenderTiles(min, maxHeight);
        }
        catch (Exception ex) {
            this.cell.rendering = false;
            ExceptionLogger.logException(ex);
        }
        this.cell.rendering = false;
        if (IsoGridSquare.getRecalcLightTime() < 0.0f) {
            IsoGridSquare.setRecalcLightTime(60.0f);
        }
        if (IsoGridSquare.getLightcache() <= 0) {
            IsoGridSquare.setLightcache(90);
        }
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("renderLast");){
            IsoObject obj;
            for (int n = 0; n < this.cell.getObjectList().size(); ++n) {
                obj = this.cell.getObjectList().get(n);
                ((IsoMovingObject)obj).renderlast();
            }
            for (int i = 0; i < this.cell.getStaticUpdaterObjectList().size(); ++i) {
                obj = this.cell.getStaticUpdaterObjectList().get(i);
                obj.renderlast();
            }
        }
        IsoTree.checkChopTreeIndicators(playerIndex);
        IsoTree.renderChopTreeIndicators();
        this.cell.lastMinX = this.cell.minX;
        this.cell.lastMinY = this.cell.minY;
        this.cell.DoBuilding(playerIndex, true);
    }

    public void RenderTiles(int minHeight, int maxHeight) {
        this.cell.minHeight = minHeight;
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = IsoCell.s_performance.isoCellRenderTiles.profile();){
            this.renderTilesInternal(maxHeight);
        }
    }

    private void renderTilesInternal(int maxHeight) {
        FBORenderChunkManager.instance.recycle();
        if (!DebugOptions.instance.terrain.renderTiles.enable.getValue()) {
            return;
        }
        if (IsoCell.floorRenderShader == null) {
            RenderThread.invokeOnRenderContext(this.cell::initTileShaders);
        }
        FBORenderLevels.clearCachedSquares = true;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        player.dirtyRecalcGridStackTime -= GameTime.getInstance().getMultiplier() / 4.0f;
        IsoCell.PerPlayerRender perPlayerRender = this.cell.getPerPlayerRenderAt(playerIndex);
        perPlayerRender.setSize(this.cell.maxX - this.cell.minX + 1, this.cell.maxY - this.cell.minY + 1);
        this.currentTimeMillis = System.currentTimeMillis();
        if (this.cell.minX != perPlayerRender.minX || this.cell.minY != perPlayerRender.minY || this.cell.maxX != perPlayerRender.maxX || this.cell.maxY != perPlayerRender.maxY) {
            perPlayerRender.minX = this.cell.minX;
            perPlayerRender.minY = this.cell.minY;
            perPlayerRender.maxX = this.cell.maxX;
            perPlayerRender.maxY = this.cell.maxY;
        }
        int currentZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("updateWeatherMask");){
            this.updateWeatherMask(playerIndex, currentZ);
        }
        boolean bForceCutawayUpdate = false;
        if (perPlayerData1.lastZ != currentZ) {
            if (currentZ < 0 != perPlayerData1.lastZ < 0) {
                player.dirtyRecalcGridStack = true;
                this.invalidateAll(playerIndex);
            } else if (player.getBuilding() != null) {
                player.getBuilding().getDef().invalidateOverlappedChunkLevelsAbove(playerIndex, PZMath.min(currentZ, perPlayerData1.lastZ), 2048L);
            } else if (player.isClimbing()) {
                bForceCutawayUpdate = true;
            }
            perPlayerData1.lastZ = currentZ;
            this.checkSeenRooms(player, currentZ);
        }
        int puddlesValue1 = (int)Math.ceil(IsoPuddles.getInstance().getPuddlesSizeFinalValue() * 500.0f);
        int wetGround1 = (int)Math.ceil(IsoPuddles.getInstance().getWetGroundFinalValue() * 500.0f);
        if (PerformanceSettings.puddlesQuality == 2 && (this.puddlesValue != (float)puddlesValue1 || this.wetGroundValue != (float)wetGround1) && this.puddlesRedrawTimeMs + 1000L < this.currentTimeMillis) {
            this.puddlesValue = puddlesValue1;
            this.wetGroundValue = wetGround1;
            this.puddlesRedrawTimeMs = this.currentTimeMillis;
            this.invalidateAll(playerIndex);
        }
        if (SandboxOptions.instance.enableSnowOnGround.getValue() && this.snowFracTarget != (float)this.cell.getSnowTarget()) {
            this.snowFracTarget = this.cell.getSnowTarget();
            this.invalidateAll(playerIndex);
        }
        CompletableFuture<Boolean> checkFuture = null;
        if (DebugOptions.instance.threadGridStacks.getValue()) {
            checkFuture = CompletableFuture.supplyAsync(() -> this.recalculateGridStacks(player, playerIndex), PZForkJoinPool.commonPool());
        }
        try (AutoCloseable autoCloseable = GameProfiler.getInstance().profile("runChecks");){
            bForceCutawayUpdate |= this.runChecks(playerIndex);
        }
        autoCloseable = IsoCell.s_performance.renderTiles.recalculateAnyGridStacks.profile();
        try {
            bForceCutawayUpdate = checkFuture != null ? (bForceCutawayUpdate |= checkFuture.join().booleanValue()) : (bForceCutawayUpdate |= this.recalculateGridStacks(player, playerIndex));
        }
        finally {
            if (autoCloseable != null) {
                ((AbstractPerformanceProfileProbe)autoCloseable).close();
            }
        }
        for (int z = 0; z < 8; ++z) {
            bForceCutawayUpdate |= this.checkDebugKeys(playerIndex, z);
        }
        if (bForceCutawayUpdate |= this.checkDebugKeys(playerIndex, currentZ)) {
            FBORenderCutaways.getInstance().squareChanged(null);
        }
        try (AbstractPerformanceProfileProbe z = cutaways.profile();){
            bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkPlayerRoom(playerIndex);
            bForceCutawayUpdate |= this.cell.SetCutawayRoomsForPlayer();
            bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkExteriorWalls(perPlayerData1.onScreenChunks);
            if (bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkSlopedSurfaces(perPlayerData1.onScreenChunks)) {
                FBORenderCutaways.getInstance().squareChanged(null);
            }
            this.prepareChunksForUpdating(playerIndex);
            if (bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkOccludedRooms(playerIndex, perPlayerData1.onScreenChunks)) {
                FBORenderCutaways.getInstance().doCutawayVisitSquares(playerIndex, perPlayerData1.onScreenChunks);
            }
        }
        perPlayerData1.occlusionChanged = false;
        if (FBORenderOcclusion.getInstance().enabled && this.hasAnyDirtyChunkTextures(playerIndex)) {
            perPlayerData1.occlusionChanged = true;
            int size = (perPlayerData1.occludedGridX2 - perPlayerData1.occludedGridX1 + 1) * (perPlayerData1.occludedGridY2 - perPlayerData1.occludedGridY1 + 1);
            if (perPlayerData1.occludedGrid == null || perPlayerData1.occludedGrid.length < size) {
                perPlayerData1.occludedGrid = new int[size];
            }
            Arrays.fill(perPlayerData1.occludedGrid, -32);
            this.calculateOccludingSquares(playerIndex);
            FBORenderOcclusion.getInstance().occludedGrid = perPlayerData1.occludedGrid;
            FBORenderOcclusion.getInstance().occludedGridX1 = perPlayerData1.occludedGridX1;
            FBORenderOcclusion.getInstance().occludedGridY1 = perPlayerData1.occludedGridY1;
            FBORenderOcclusion.getInstance().occludedGridX2 = perPlayerData1.occludedGridX2;
            FBORenderOcclusion.getInstance().occludedGridY2 = perPlayerData1.occludedGridY2;
        }
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = updateLighting.profile();){
            this.updateChunkLighting(playerIndex);
        }
        this.checkBlackedOutBuildings(playerIndex);
        this.checkBlackedOutRooms(playerIndex);
        FBORenderLevels.clearCachedSquares = false;
        abstractPerformanceProfileProbe = IsoCell.s_performance.renderTiles.performRenderTiles.profile();
        try {
            this.performRenderTiles(perPlayerRender, playerIndex, this.currentTimeMillis);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        FBORenderLevels.clearCachedSquares = true;
        this.cell.playerCutawaysDirty[playerIndex] = false;
        IsoCell.ShadowSquares.clear();
        IsoCell.MinusFloorCharacters.clear();
        IsoCell.ShadedFloor.clear();
        IsoCell.SolidFloor.clear();
        IsoCell.VegetationCorpses.clear();
        abstractPerformanceProfileProbe = IsoCell.s_performance.renderTiles.renderDebugPhysics.profile();
        try {
            this.cell.renderDebugPhysics(playerIndex);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        abstractPerformanceProfileProbe = IsoCell.s_performance.renderTiles.renderDebugLighting.profile();
        try {
            this.cell.renderDebugLighting(perPlayerRender, maxHeight);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        FMODAmbientWalls.getInstance().render();
    }

    private boolean recalculateGridStacks(IsoPlayer player, int playerIndex) {
        boolean bForceCutawayUpdate = false;
        FBORenderCutaways.getInstance().CalculatePointsOfInterest();
        bForceCutawayUpdate |= FBORenderCutaways.getInstance().CalculateBuildingsToCollapse();
        bForceCutawayUpdate |= FBORenderCutaways.getInstance().checkHiddenBuildingLevels();
        this.recalculateAnyGridStacks(playerIndex);
        return bForceCutawayUpdate |= player.dirtyRecalcGridStack;
    }

    private void updateWeatherMask(int playerIndex, int currentZ) {
        if (WeatherFxMask.checkVisibleSquares(playerIndex, currentZ)) {
            WeatherFxMask.forceMaskUpdate(playerIndex);
            WeatherFxMask.initMask();
        }
    }

    private boolean runChecks(int playerIndex) {
        boolean result;
        GameProfiler profiler = GameProfiler.getInstance();
        this.checkWaterQualityOption(playerIndex);
        this.checkWindEffectsOption(playerIndex);
        try (GameProfiler.ProfileArea profileArea = profiler.profile("Newly");){
            result = this.checkNewlyOnScreenChunks(playerIndex);
        }
        profileArea = profiler.profile("Obscuring");
        try {
            this.checkObjectsObscuringPlayer(playerIndex);
            this.checkFadingInObjectsObscuringPlayer(playerIndex);
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        profileArea = profiler.profile("Chunks");
        try {
            this.checkChunksWithTrees(playerIndex);
            this.checkSeamChunks(playerIndex);
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        this.checkMannequinRenderDirection(playerIndex);
        this.checkPuddlesQualityOption(playerIndex);
        return result;
    }

    private void invalidateAll(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        for (int xx = 0; xx < IsoChunkMap.chunkGridWidth; ++xx) {
            for (int yy = 0; yy < IsoChunkMap.chunkGridWidth; ++yy) {
                IsoChunk c = chunkMap.getChunk(xx, yy);
                if (c == null || c.lightingNeverDone[playerIndex]) continue;
                FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                renderLevels.invalidateAll(2048L);
            }
        }
    }

    private void checkObjectsObscuringPlayer(int playerIndex) {
        IsoGridSquare square;
        IsoGameCharacter.Location location;
        int i;
        this.calculatePlayerRenderBounds(playerIndex);
        this.calculateObjectsObscuringPlayer(playerIndex, this.tempLocations);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (this.tempLocations.equals(perPlayerData1.squaresObscuringPlayer)) {
            this.locationPool.releaseAll((List<IsoGameCharacter.Location>)this.tempLocations);
            this.tempLocations.clear();
            return;
        }
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        for (i = 0; i < perPlayerData1.squaresObscuringPlayer.size(); ++i) {
            location = perPlayerData1.squaresObscuringPlayer.get(i);
            if (this.listContainsLocation(this.tempLocations, location) || (square = chunkMap.getGridSquare(location.x, location.y, location.z)) == null) continue;
            square.invalidateRenderChunkLevel(8192L);
            this.invalidateChunkLevelForRenderSquare(square);
        }
        this.locationPool.releaseAll((List<IsoGameCharacter.Location>)perPlayerData1.squaresObscuringPlayer);
        perPlayerData1.squaresObscuringPlayer.clear();
        PZArrayUtil.addAll(perPlayerData1.squaresObscuringPlayer, this.tempLocations);
        for (i = 0; i < perPlayerData1.squaresObscuringPlayer.size(); ++i) {
            location = perPlayerData1.squaresObscuringPlayer.get(i);
            square = chunkMap.getGridSquare(location.x, location.y, location.z);
            if (square == null) continue;
            square.invalidateRenderChunkLevel(8192L);
            this.invalidateChunkLevelForRenderSquare(square);
        }
        this.tempLocations.clear();
    }

    private void calculatePlayerRenderBounds(int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        float playerZ = IsoCamera.frameState.camCharacterZ;
        perPlayerData1.playerBoundsX = IsoUtils.XToScreen(playerX, playerY, playerZ, 0);
        perPlayerData1.playerBoundsY = IsoUtils.YToScreen(playerX, playerY, playerZ, 0);
        perPlayerData1.playerBoundsX -= (float)(32 * Core.tileScale);
        perPlayerData1.playerBoundsY -= (float)(112 * Core.tileScale);
        perPlayerData1.playerBoundsW = 64 * Core.tileScale;
        perPlayerData1.playerBoundsH = 128 * Core.tileScale;
    }

    private boolean isPotentiallyObscuringObject(IsoObject object) {
        if (object == null) {
            return false;
        }
        IsoSprite sprite = object.getSprite();
        if (sprite == null) {
            return false;
        }
        IsoGameCharacter chr = IsoCamera.frameState.camCharacter;
        if (chr != null && chr.isSittingOnFurniture() && chr.isSitOnFurnitureObject(object)) {
            return false;
        }
        if (chr != null && chr.isOnBed() && object == chr.getBed()) {
            return false;
        }
        if (sprite.getProperties().has(IsoFlagType.water)) {
            return false;
        }
        if (sprite.getProperties().has(IsoFlagType.attachedSurface) && (object.square.has(IsoFlagType.solid) || object.square.has(IsoFlagType.solidtrans))) {
            return true;
        }
        if (sprite.getProperties().has(IsoFlagType.attachedE) || sprite.getProperties().has(IsoFlagType.attachedS) || sprite.getProperties().has(IsoFlagType.attachedCeiling)) {
            return true;
        }
        if (object.isStairsNorth()) {
            if (IsoCamera.frameState.camCharacterSquare != null && IsoCamera.frameState.camCharacterSquare.HasStairs()) {
                return false;
            }
            return object.getX() > (float)PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
        }
        if (object.isStairsWest()) {
            if (IsoCamera.frameState.camCharacterSquare != null && IsoCamera.frameState.camCharacterSquare.HasStairs()) {
                return false;
            }
            return object.getY() > (float)PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
        }
        return sprite.solid || sprite.solidTrans;
    }

    private void calculateObjectsObscuringPlayer(int playerIndex, ArrayList<IsoGameCharacter.Location> locations) {
        int i;
        this.locationPool.releaseAll((List<IsoGameCharacter.Location>)locations);
        locations.clear();
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null || player.getCurrentSquare() == null) {
            return;
        }
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        int sqx = player.getCurrentSquare().getX();
        int sqy = player.getCurrentSquare().getY();
        int sqz = player.getCurrentSquare().getZ();
        int sqLeftX = sqx - 1;
        int sqLeftY = sqy;
        int sqRightX = sqx;
        int sqRightY = sqy - 1;
        this.testSquareObscuringPlayer(playerIndex, sqx, sqy, sqz, locations);
        for (i = 1; i <= 3; ++i) {
            this.testSquareObscuringPlayer(playerIndex, sqx + i, sqy + i, sqz, locations);
            this.testSquareObscuringPlayer(playerIndex, sqLeftX + i, sqLeftY + i, sqz, locations);
            this.testSquareObscuringPlayer(playerIndex, sqRightX + i, sqRightY + i, sqz, locations);
            this.testSquareObscuringPlayer(playerIndex, sqx - 1 + i, sqy + 1 + i, sqz, locations);
            this.testSquareObscuringPlayer(playerIndex, sqx + 1 + i, sqy - 1 + i, sqz, locations);
        }
        for (i = 0; i < locations.size(); ++i) {
            IsoGameCharacter.Location location = locations.get(i);
            IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
            if (square == null) continue;
            for (int j = 0; j < square.getObjects().size(); ++j) {
                IsoObject object = square.getObjects().get(j);
                if (!this.isPotentiallyObscuringObject(object)) continue;
                this.addObscuringStairObjects(locations, square, object);
                IsoSprite sprite = object.getSprite();
                if (sprite.getSpriteGrid() == null) continue;
                IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
                int spriteGridPosX = spriteGrid.getSpriteGridPosX(sprite);
                int spriteGridPosY = spriteGrid.getSpriteGridPosY(sprite);
                int spriteGridPosZ = spriteGrid.getSpriteGridPosZ(sprite);
                for (int spriteGridZ = 0; spriteGridZ < spriteGrid.getLevels(); ++spriteGridZ) {
                    for (int spriteGridY = 0; spriteGridY < spriteGrid.getHeight(); ++spriteGridY) {
                        for (int spriteGridX = 0; spriteGridX < spriteGrid.getWidth(); ++spriteGridX) {
                            int squareZ;
                            int squareY;
                            int squareX;
                            if (spriteGrid.getSprite(spriteGridX, spriteGridY) == null || chunkMap.getGridSquare(squareX = square.x - spriteGridPosX + spriteGridX, squareY = square.y - spriteGridPosY + spriteGridY, squareZ = square.z - spriteGridPosZ + spriteGridZ) == null || this.listContainsLocation(locations, squareX, squareY, squareZ)) continue;
                            IsoGameCharacter.Location location1 = this.locationPool.alloc();
                            location1.set(squareX, squareY, squareZ);
                            locations.add(location1);
                        }
                    }
                }
            }
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i2 = 0; i2 < perPlayerData1.squaresObscuringPlayer.size(); ++i2) {
            IsoGridSquare square;
            IsoGameCharacter.Location location = perPlayerData1.squaresObscuringPlayer.get(i2);
            if (this.listContainsLocation(locations, location) || this.listContainsLocation(perPlayerData1.fadingInSquares, location) || !this.squareHasFadingInObjects(playerIndex, square = chunkMap.getGridSquare(location.x, location.y, location.z))) continue;
            IsoGameCharacter.Location location1 = this.locationPool.alloc();
            location1.set(square.x, square.y, square.z);
            perPlayerData1.fadingInSquares.add(location1);
        }
    }

    private void addObscuringStairObjects(ArrayList<IsoGameCharacter.Location> locations, IsoGridSquare square, IsoObject object) {
        IsoGameCharacter.Location location1;
        IsoGridSquare square1;
        if (object.isStairsNorth()) {
            int dy1 = 0;
            int dy2 = 0;
            if (object.getType() == IsoObjectType.stairsTN) {
                dy2 = 2;
            }
            if (object.getType() == IsoObjectType.stairsMN) {
                dy1 = -1;
                dy2 = 1;
            }
            if (object.getType() == IsoObjectType.stairsBN) {
                dy1 = -2;
                dy2 = 0;
            }
            if (dy1 < dy2) {
                for (int dy = dy1; dy <= dy2; ++dy) {
                    square1 = IsoWorld.instance.currentCell.getGridSquare(square.x, square.y + dy, square.z);
                    if (square1 == null || this.listContainsLocation(locations, square.x, square.y + dy, square.z)) continue;
                    location1 = this.locationPool.alloc();
                    location1.set(square.x, square.y + dy, square.z);
                    locations.add(location1);
                }
            }
        }
        if (object.isStairsWest()) {
            int dx1 = 0;
            int dx2 = 0;
            if (object.getType() == IsoObjectType.stairsTW) {
                dx2 = 2;
            }
            if (object.getType() == IsoObjectType.stairsMW) {
                dx1 = -1;
                dx2 = 1;
            }
            if (object.getType() == IsoObjectType.stairsBW) {
                dx1 = -2;
                dx2 = 0;
            }
            if (dx1 < dx2) {
                for (int dx = dx1; dx <= dx2; ++dx) {
                    square1 = IsoWorld.instance.currentCell.getGridSquare(square.x + dx, square.y, square.z);
                    if (square1 == null || this.listContainsLocation(locations, square.x + dx, square.y, square.z)) continue;
                    location1 = this.locationPool.alloc();
                    location1.set(square.x + dx, square.y, square.z);
                    locations.add(location1);
                }
            }
        }
    }

    private void checkFadingInObjectsObscuringPlayer(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.fadingInSquares.size(); ++i) {
            IsoGameCharacter.Location location = perPlayerData1.fadingInSquares.get(i);
            IsoGridSquare square = chunkMap.getGridSquare(location.x, location.y, location.z);
            if (square == null || this.squareHasFadingInObjects(playerIndex, square)) continue;
            square.invalidateRenderChunkLevel(8192L);
            this.invalidateChunkLevelForRenderSquare(square);
            perPlayerData1.fadingInSquares.remove(i--);
            this.locationPool.release(location);
        }
    }

    private boolean squareHasFadingInObjects(int playerIndex, IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        for (int i = 0; i < square.getObjects().size(); ++i) {
            IsoObject object = square.getObjects().get(i);
            if (!this.isPotentiallyObscuringObject(object) || !(object.getAlpha(playerIndex) < 1.0f)) continue;
            return true;
        }
        return false;
    }

    private void invalidateChunkLevelForRenderSquare(IsoGridSquare square) {
        IsoGridSquare renderSquare;
        if (square.getWorldObjects().isEmpty()) {
            return;
        }
        int chunksPerWidth = 8;
        if (PZMath.coordmodulo(square.x, 8) == 0 && PZMath.coordmodulo(square.y, 8) == 7 && (renderSquare = square.getAdjacentSquare(IsoDirections.S)) != null) {
            renderSquare.invalidateRenderChunkLevel(8192L);
        }
        if (PZMath.coordmodulo(square.x, 8) == 7 && PZMath.coordmodulo(square.y, 8) == 0 && (renderSquare = square.getAdjacentSquare(IsoDirections.E)) != null) {
            renderSquare.invalidateRenderChunkLevel(8192L);
        }
    }

    private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> locations, IsoGameCharacter.Location location) {
        return this.listContainsLocation(locations, location.x, location.y, location.z);
    }

    private boolean listContainsLocation(ArrayList<IsoGameCharacter.Location> locations, int x, int y, int z) {
        for (int i = 0; i < locations.size(); ++i) {
            if (!locations.get(i).equals(x, y, z)) continue;
            return true;
        }
        return false;
    }

    private void testSquareObscuringPlayer(int playerIndex, int x, int y, int z, ArrayList<IsoGameCharacter.Location> locations) {
        IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
        IsoGridSquare square = chunkMap.getGridSquare(x, y, z);
        if (this.isSquareObscuringPlayer(playerIndex, square)) {
            if (this.listContainsLocation(locations, square.x, square.y, square.z)) {
                return;
            }
            IsoGameCharacter.Location location = this.locationPool.alloc();
            location.set(square.x, square.y, square.z);
            locations.add(location);
        }
    }

    private boolean isSquareObscuringPlayer(int playerIndex, IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (!(square.has(IsoFlagType.attachedE) || square.has(IsoFlagType.attachedS) || square.has(IsoFlagType.attachedCeiling) || square.HasStairs() || square.has(IsoFlagType.solid) || square.has(IsoFlagType.solidtrans))) {
            return false;
        }
        for (int i = 0; i < square.getObjects().size(); ++i) {
            Texture texture;
            IsoObject object = square.getObjects().get(i);
            if (!this.isPotentiallyObscuringObject(object) || (texture = object.sprite.getTextureForCurrentFrame(object.dir, object)) == null || !perPlayerData1.isObjectObscuringPlayer(square, texture, object.offsetX, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale)) continue;
            return true;
        }
        return false;
    }

    private void checkChunksWithTrees(int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            FBORenderLevels renderLevels;
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            if (0 < c.minLevel || 0 > c.maxLevel || !(renderLevels = c.getRenderLevels(playerIndex)).isOnScreen(0)) continue;
            boolean bInStencilRect = renderLevels.calculateInStencilRect(0);
            if (!bInStencilRect && renderLevels.inStencilRect) {
                renderLevels.inStencilRect = false;
                renderLevels.invalidateLevel(0, 4096L);
                continue;
            }
            renderLevels.inStencilRect = bInStencilRect;
            if (!this.checkTreeTranslucency(playerIndex, renderLevels)) continue;
            renderLevels.invalidateLevel(0, 4096L);
        }
    }

    private void checkSeamChunks(int playerIndex) {
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
            if (renderLevels.adjacentChunkLoadedCounter == c.adjacentChunkLoadedCounter) continue;
            renderLevels.adjacentChunkLoadedCounter = c.adjacentChunkLoadedCounter;
            renderLevels.invalidateAll(1024L);
        }
    }

    private boolean checkTreeTranslucency(int playerIndex, FBORenderLevels renderLevels) {
        if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            return false;
        }
        float zoom = Core.getInstance().getZoom(playerIndex);
        if (renderLevels.isDirty(0, zoom)) {
            return false;
        }
        ArrayList<IsoGridSquare> squares = renderLevels.treeSquares;
        boolean bChanged = false;
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare renderSquare;
            IsoTree tree;
            IsoGridSquare square = squares.get(i);
            if (square.chunk == null || (tree = square.getTree()) == null) continue;
            boolean bChanged2 = false;
            if (tree.fadeAlpha < 1.0f != tree.wasFaded) {
                tree.wasFaded = tree.fadeAlpha < 1.0f;
                bChanged = true;
                bChanged2 = true;
            }
            if (this.isTranslucentTree(tree) != tree.renderFlag) {
                tree.renderFlag = !tree.renderFlag;
                bChanged = true;
                bChanged2 = true;
            }
            if (!bChanged2 || (renderSquare = tree.getRenderSquare()) == null || tree.getSquare() == renderSquare) continue;
            renderSquare.invalidateRenderChunkLevel(4096L);
        }
        return bChanged;
    }

    private void checkWaterQualityOption(int playerIndex) {
        if (this.waterShader == IsoWater.getInstance().getShaderEnable()) {
            return;
        }
        this.waterShader = IsoWater.getInstance().getShaderEnable();
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            FBORenderLevels renderLevels;
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            if (0 < c.minLevel || 0 > c.maxLevel || !(renderLevels = c.getRenderLevels(playerIndex)).calculateOnScreen(0)) continue;
            renderLevels.invalidateLevel(0, 1024L);
        }
    }

    private void checkWindEffectsOption(int playerIndex) {
        if (this.windEffects == Core.getInstance().getOptionDoWindSpriteEffects()) {
            return;
        }
        this.windEffects = Core.getInstance().getOptionDoWindSpriteEffects();
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            FBORenderLevels renderLevels;
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            if (0 < c.minLevel || 0 > c.maxLevel || !(renderLevels = c.getRenderLevels(playerIndex)).calculateOnScreen(0)) continue;
            renderLevels.invalidateLevel(0, 4096L);
        }
    }

    private void checkPuddlesQualityOption(int playerIndex) {
        if (this.puddlesQuality == 2 == (PerformanceSettings.puddlesQuality == 2)) {
            return;
        }
        this.puddlesQuality = PerformanceSettings.puddlesQuality;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[playerIndex];
        for (int cy = 0; cy < IsoChunkMap.chunkGridWidth; ++cy) {
            for (int cx = 0; cx < IsoChunkMap.chunkGridWidth; ++cx) {
                IsoChunk chunk = chunkMap.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                    IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];
                    for (int i = 0; i < squares.length; ++i) {
                        IsoPuddlesGeometry pg;
                        IsoGridSquare square = squares[i];
                        if (square == null || (pg = square.getPuddles()) == null) continue;
                        pg.init(square);
                    }
                }
            }
        }
        this.invalidateAll(playerIndex);
    }

    private boolean checkNewlyOnScreenChunks(int playerIndex) {
        boolean bForceCutawaysUpdate = false;
        float cameraZoom = Core.getInstance().getZoom(playerIndex);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.onScreenChunks.clear();
        perPlayerData1.chunksWithAnimatedAttachments.clear();
        perPlayerData1.chunksWithFlies.clear();
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        for (int xx = 0; xx < IsoChunkMap.chunkGridWidth; ++xx) {
            for (int yy = 0; yy < IsoChunkMap.chunkGridWidth; ++yy) {
                int z;
                IsoChunk c = chunkMap.getChunk(xx, yy);
                if (c == null || c.lightingNeverDone[playerIndex]) continue;
                FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
                if (!c.IsOnScreen(true)) {
                    for (z = c.minLevel; z <= c.maxLevel; ++z) {
                        renderLevels.setOnScreen(z, false);
                        renderLevels.freeFBOsForLevel(z);
                    }
                    continue;
                }
                perPlayerData1.onScreenChunks.add(c);
                if (renderLevels.prevMinZ != c.minLevel || renderLevels.prevMaxZ != c.maxLevel) {
                    for (z = c.minLevel; z <= c.maxLevel; ++z) {
                        renderLevels.invalidateLevel(z, 64L);
                    }
                }
                for (z = c.minLevel; z <= c.maxLevel; ++z) {
                    if (z != renderLevels.getMinLevel(z)) continue;
                    boolean bWasOnScreen = renderLevels.isOnScreen(z);
                    boolean bOnScreen = renderLevels.calculateOnScreen(z);
                    if (bOnScreen && !renderLevels.getCachedSquares_Flies(z).isEmpty()) {
                        perPlayerData1.addChunkWith_Flies(c);
                    }
                    if (bWasOnScreen == bOnScreen) continue;
                    if (bOnScreen) {
                        renderLevels.setOnScreen(z, true);
                        renderLevels.invalidateLevel(z, 1024L);
                        if (!renderLevels.isDirty(z, 16384L, cameraZoom)) continue;
                        bForceCutawaysUpdate = true;
                        continue;
                    }
                    renderLevels.setOnScreen(z, false);
                    renderLevels.freeFBOsForLevel(z);
                }
            }
        }
        FBORenderChunkManager.instance.recycle();
        return bForceCutawaysUpdate;
    }

    private void performRenderTiles(IsoCell.PerPlayerRender perPlayerRender, int playerIndex, long currentTimeMillis) {
        AutoCloseable autoCloseable;
        Shader floorRenderShader = null;
        Shader wallRenderShader = null;
        this.renderAnimatedAttachments = false;
        this.renderTranslucentOnly = false;
        FBORenderChunkManager.instance.startFrame();
        IsoPuddles.getInstance().clearThreadData();
        IsoWater.getInstance().clearThreadData();
        this.invalidateDelayedLoadingLevels = false;
        if (this.delayedLoadingTimerMs != 0L && this.delayedLoadingTimerMs <= currentTimeMillis) {
            this.delayedLoadingTimerMs = 0L;
            this.invalidateDelayedLoadingLevels = true;
        }
        SpriteRenderer.instance.beginProfile(tilesProbe);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        perPlayerData1.chunksWithTranslucentFloor.clear();
        perPlayerData1.chunksWithTranslucentNonFloor.clear();
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            autoCloseable = renderOneChunk.profile();
            try {
                this.renderOneChunk(c, perPlayerRender, playerIndex, currentTimeMillis, floorRenderShader, wallRenderShader);
                continue;
            }
            finally {
                if (autoCloseable != null) {
                    ((AbstractPerformanceProfileProbe)autoCloseable).close();
                }
            }
        }
        SpriteRenderer.instance.endProfile(tilesProbe);
        FBORenderCorpses.getInstance().update();
        FBORenderItems.getInstance().update();
        FBORenderChunkManager.instance.endFrame();
        FBORenderShadows.getInstance().clear();
        this.renderPlayers(playerIndex);
        this.renderCorpseShadows(playerIndex);
        this.renderMannequinShadows(playerIndex);
        if (!DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
            this.renderCorpsesInWorld(playerIndex);
        }
        if (!DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
            SpriteRenderer.instance.beginProfile(itemsProbe);
            this.renderItemsInWorld(playerIndex);
            SpriteRenderer.instance.endProfile(itemsProbe);
        }
        if (PerformanceSettings.puddlesQuality < 2) {
            try (AbstractPerformanceProfileProbe i = puddles.profile();){
                this.renderPuddles(playerIndex);
            }
        }
        this.renderOpaqueObjectsEvent(playerIndex);
        SpriteRenderer.instance.beginProfile(movingObjectsProbe);
        this.renderMovingObjects();
        SpriteRenderer.instance.endProfile(movingObjectsProbe);
        try (AbstractPerformanceProfileProbe i = water.profile();){
            this.renderWater(playerIndex);
        }
        this.renderAnimatedAttachments(playerIndex);
        this.renderFlies(playerIndex);
        FBORenderObjectHighlight.getInstance().render(playerIndex);
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.getChunkMap(playerIndex);
        for (int z = chunkMap.minHeight; z <= chunkMap.maxHeight; ++z) {
            SpriteRenderer.instance.beginProfile(translucentFloorObjectsProbe);
            autoCloseable = translucentFloor.profile();
            try {
                this.renderTranslucentFloorObjects(playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }
            finally {
                if (autoCloseable != null) {
                    ((AbstractPerformanceProfileProbe)autoCloseable).close();
                }
            }
            SpriteRenderer.instance.endProfile(translucentFloorObjectsProbe);
            autoCloseable = puddles.profile();
            try {
                this.renderPuddlesTranslucentFloorsOnly(playerIndex, z);
            }
            finally {
                if (autoCloseable != null) {
                    ((AbstractPerformanceProfileProbe)autoCloseable).close();
                }
            }
            if (z == 0) {
                this.renderWaterShore(playerIndex);
            }
            this.renderRainSplashes(playerIndex, z);
            SpriteRenderer.instance.beginProfile(shadowsProbe);
            FBORenderShadows.getInstance().renderMain(z);
            SpriteRenderer.instance.endProfile(shadowsProbe);
            if (z == PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                SpriteRenderer.instance.beginProfile(visibilityProbe);
                autoCloseable = GameProfiler.getInstance().profile("Visibility");
                try {
                    VisibilityPolygon2.getInstance().renderMain(playerIndex);
                }
                finally {
                    if (autoCloseable != null) {
                        ((GameProfiler.ProfileArea)autoCloseable).close();
                    }
                }
                SpriteRenderer.instance.endProfile(visibilityProbe);
            }
            WorldMarkers.instance.renderGridSquareMarkers(z);
            this.renderTranslucentOnly = true;
            IsoMarkers.instance.renderIsoMarkers(perPlayerRender, z, playerIndex);
            this.renderTranslucentOnly = false;
            SpriteRenderer.instance.beginProfile(translucentObjectsProbe);
            autoCloseable = translucentNonFloor.profile();
            try {
                this.renderTranslucentObjects(playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
            }
            finally {
                if (autoCloseable != null) {
                    ((AbstractPerformanceProfileProbe)autoCloseable).close();
                }
            }
            SpriteRenderer.instance.endProfile(translucentObjectsProbe);
        }
        FBORenderShadows.getInstance().endRender();
        if (DebugOptions.instance.weather.showUsablePuddles.getValue()) {
            this.renderPuddleDebug(playerIndex);
        }
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = fog.profile();){
            this.renderFog(playerIndex);
        }
        FBORenderObjectOutline.getInstance().render(playerIndex);
    }

    private void renderOneChunk(IsoChunk c, IsoCell.PerPlayerRender perPlayerRender, int playerIndex, long currentTimeMillis, Shader floorRenderShader, Shader wallRenderShader) {
        if (c == null || !c.IsOnScreen(true)) {
            return;
        }
        if (c.lightingNeverDone[playerIndex]) {
            return;
        }
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        renderLevels.prevMinZ = c.minLevel;
        renderLevels.prevMaxZ = c.maxLevel;
        for (int zza = c.minLevel; zza <= c.maxLevel; ++zza) {
            try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = renderOneChunkLevel.profile();){
                this.renderOneLevel(c, zza, perPlayerRender, playerIndex, currentTimeMillis, floorRenderShader, wallRenderShader);
            }
            if (!DebugOptions.instance.fboRenderChunk.renderWallLines.getValue() || zza != PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) continue;
            c.getCutawayData().debugRender(zza);
        }
        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel(IsoChunk c, int level, IsoCell.PerPlayerRender perPlayerRender, int playerIndex, long currentTimeMillis, Shader floorRenderShader, Shader wallRenderShader) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            renderLevels.freeFBOsForLevel(level);
            return;
        }
        float zoom = Core.getInstance().getZoom(playerIndex);
        if (this.invalidateDelayedLoadingLevels && renderLevels.isDelayedLoading(level) && level == renderLevels.getMinLevel(level)) {
            renderLevels.invalidateLevel(level, 1024L);
        }
        if (FBORenderOcclusion.getInstance().enabled) {
            if (level == renderLevels.getMinLevel(level) && perPlayerData1.occlusionChanged) {
                renderLevels.setRenderedSquaresCount(level, this.calculateRenderedSquaresCount(playerIndex, c, level));
            }
            if (renderLevels.getRenderedSquaresCount(level) == 0) {
                if (level == renderLevels.getMaxLevel(level)) {
                    renderLevels.clearDirty(level, zoom);
                    if (renderLevels.getFBOForLevel(level, zoom) != null) {
                        renderLevels.freeFBOsForLevel(level);
                        FBORenderChunkManager.instance.recycle();
                    }
                }
                return;
            }
        }
        int frameNo = IsoWorld.instance.getFrameNo();
        boolean canRender = true;
        boolean isDirty = FBORenderChunkManager.instance.beginRenderChunkLevel(c, level, zoom, canRender, true);
        if (DebugOptions.instance.delayObjectRender.getValue()) {
            boolean bl = canRender = (long)frameNo == c.loadedFrame || (long)frameNo >= c.renderFrame;
        }
        if (!isDirty || !canRender) {
            FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, false);
            if (!renderLevels.getCachedSquares_AnimatedAttachments(level).isEmpty()) {
                perPlayerData1.addChunkWith_AnimatedAttachments(c);
            }
            if (!renderLevels.getCachedSquares_TranslucentFloor(level).isEmpty()) {
                perPlayerData1.addChunkWith_TranslucentFloor(c);
            }
            if (renderLevels.getCachedSquares_Items(level).size() + renderLevels.getCachedSquares_TranslucentNonFloor(level).size() > 0) {
                perPlayerData1.addChunkWith_TranslucentNonFloor(c);
            }
            return;
        }
        boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;
        IsoCell.ShadowSquares.clear();
        IsoCell.SolidFloor.clear();
        IsoCell.ShadedFloor.clear();
        IsoCell.VegetationCorpses.clear();
        IsoCell.MinusFloorCharacters.clear();
        GameProfiler profiler = GameProfiler.getInstance();
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = calculateRenderInfo.profile();){
            if (level == renderLevels.getMinLevel(level)) {
                renderLevels.clearCachedSquares(level);
            }
            if (level == 0) {
                renderLevels.treeSquares.clear();
            }
            FBORenderCutaways.ChunkLevelData levelData = c.getCutawayDataForLevel(level);
            IsoGridSquare[] squares = c.squares[c.squaresIndexOfLevel(level)];
            for (int i = 0; i < squares.length; ++i) {
                IsoGridSquare square = squares[i];
                if (!levelData.shouldRenderSquare(playerIndex, square) || FBORenderOcclusion.getInstance().isOccluded(square.x, square.y, square.z)) {
                    this.setNotRendered(square);
                    continue;
                }
                if (square.getObjects().isEmpty()) continue;
                square.flattenGrassEtc = false;
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Calculate");){
                    this.calculateObjectRenderInfo(square);
                }
                int flags = 0;
                boolean bHasTranslucentFloor = false;
                boolean bHasTranslucentNonFloor = false;
                boolean bHasAttachedSpritesOnWater = false;
                boolean bHasAnimatedAttachments = false;
                boolean bHasItems = false;
                IsoObject[] objects = square.getObjects().getElements();
                int numObjects = square.getObjects().size();
                for (int j = 0; j < numObjects; ++j) {
                    IsoObject object = objects[j];
                    ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                    switch (renderInfo.layer) {
                        case Floor: {
                            flags |= 1;
                            break;
                        }
                        case Vegetation: {
                            flags |= 2;
                            break;
                        }
                        case MinusFloor: {
                            flags |= 4;
                            break;
                        }
                        case MinusFloorSE: {
                            flags |= 4;
                            break;
                        }
                        case WorldInventoryObject: {
                            flags |= 4;
                            break;
                        }
                        case Translucent: {
                            bHasTranslucentNonFloor = true;
                            break;
                        }
                        case TranslucentFloor: {
                            bHasTranslucentFloor = true;
                        }
                    }
                    if (renderInfo.layer == ObjectRenderLayer.None && object.sprite != null && object.sprite.getProperties().has(IsoFlagType.water) && object.getAttachedAnimSprite() != null && !object.getAttachedAnimSprite().isEmpty()) {
                        bHasAttachedSpritesOnWater = true;
                    }
                    bHasAnimatedAttachments |= object.hasAnimatedAttachments();
                    if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) continue;
                    bHasItems |= object instanceof IsoWorldInventoryObject;
                }
                if (bHasAnimatedAttachments) {
                    renderLevels.getCachedSquares_AnimatedAttachments(level).add(square);
                }
                if (!square.getStaticMovingObjects().isEmpty()) {
                    renderLevels.getCachedSquares_Corpses(level).add(square);
                }
                if (square.hasFlies()) {
                    renderLevels.getCachedSquares_Flies(level).add(square);
                }
                if (bHasItems) {
                    renderLevels.getCachedSquares_Items(level).add(square);
                }
                try (GameProfiler.ProfileArea j = profiler.profile("Puddles");){
                    if (square.getPuddles() != null && square.getPuddles().shouldRender()) {
                        renderLevels.getCachedSquares_Puddles(level).add(square);
                    }
                }
                if (bHasTranslucentFloor) {
                    renderLevels.getCachedSquares_TranslucentFloor(level).add(square);
                }
                if (bHasTranslucentNonFloor) {
                    renderLevels.getCachedSquares_TranslucentNonFloor(level).add(square);
                }
                if (!square.getStaticMovingObjects().isEmpty()) {
                    flags |= 2;
                    flags |= 0x10;
                    if (square.HasStairs()) {
                        flags |= 4;
                    }
                }
                if (!square.getWorldObjects().isEmpty()) {
                    flags |= 2;
                }
                for (int m = 0; m < square.getMovingObjects().size(); ++m) {
                    IsoMovingObject mov = square.getMovingObjects().get(m);
                    boolean bOnFloor = mov.isOnFloor();
                    if (bOnFloor && mov instanceof IsoZombie) {
                        IsoZombie zombie = (IsoZombie)mov;
                        bOnFloor = zombie.isProne();
                        if (!BaseVehicle.renderToTexture) {
                            bOnFloor = false;
                        }
                    }
                    flags = bOnFloor ? (flags |= 2) : (flags |= 4);
                    flags |= 0x10;
                }
                if (square.hasFlies()) {
                    flags |= 4;
                }
                if ((flags & 1) != 0) {
                    IsoCell.SolidFloor.add(square);
                }
                if ((flags & 8) != 0) {
                    IsoCell.ShadedFloor.add(square);
                }
                if ((flags & 2) != 0) {
                    IsoCell.VegetationCorpses.add(square);
                }
                if ((flags & 4) != 0) {
                    IsoCell.MinusFloorCharacters.add(square);
                }
                if ((flags & 0x10) != 0) {
                    IsoCell.ShadowSquares.add(square);
                }
                if (level == 0 && square.has(IsoObjectType.tree)) {
                    renderLevels.treeSquares.add(square);
                }
                if (square.getWater() != null && square.getWater().hasWater()) {
                    renderLevels.getCachedSquares_Water(level).add(square);
                }
                if (square.getWater() != null && square.getWater().isbShore() && IsoWater.getInstance().getShaderEnable()) {
                    renderLevels.getCachedSquares_WaterShore(level).add(square);
                }
                if (!bHasAttachedSpritesOnWater) continue;
                renderLevels.getCachedSquares_WaterAttach(level).add(square);
            }
        }
        abstractPerformanceProfileProbe = renderOneChunkLevel2.profile();
        try {
            if (level == renderLevels.getMinLevel(level)) {
                renderLevels.clearDelayedLoading(level);
            }
            boolean renderFloor = true;
            boolean renderObjects = true;
            if (DebugOptions.instance.delayObjectRender.getValue()) {
                renderFloor = (long)frameNo == c.loadedFrame || (long)frameNo > c.renderFrame;
                boolean bl = renderObjects = (long)frameNo >= c.renderFrame;
            }
            if (renderFloor) {
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Floor");){
                    for (int i = 0; i < IsoCell.SolidFloor.size(); ++i) {
                        IsoGridSquare square = IsoCell.SolidFloor.get(i);
                        this.renderFloor(square);
                    }
                }
                profileArea = profiler.profile("Snow");
                try {
                    IndieGL.disableDepthTest();
                    FBORenderSnow.getInstance().RenderSnow(c, level);
                    IndieGL.enableDepthTest();
                }
                finally {
                    if (profileArea != null) {
                        profileArea.close();
                    }
                }
                profileArea = profiler.profile("Blood");
                try {
                    if (IsoCamera.frameState.camCharacterZ >= 0.0f || level <= PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                        int chunksPerWidth = 8;
                        this.renderOneLevel_Blood(c, level, c.wx * 8, c.wy * 8, (c.wx + 1) * 8, (c.wy + 1) * 8);
                        this.renderOneLevel_Blood(c, -1, -1, level);
                        this.renderOneLevel_Blood(c, 0, -1, level);
                        this.renderOneLevel_Blood(c, 1, -1, level);
                        this.renderOneLevel_Blood(c, -1, 0, level);
                        this.renderOneLevel_Blood(c, 1, 0, level);
                        this.renderOneLevel_Blood(c, -1, 1, level);
                        this.renderOneLevel_Blood(c, 0, 1, level);
                        this.renderOneLevel_Blood(c, 1, 1, level);
                    }
                }
                finally {
                    if (profileArea != null) {
                        profileArea.close();
                    }
                }
            }
            if (!renderObjects) {
                FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, false);
                return;
            }
            if (DebugOptions.instance.terrain.renderTiles.vegetationCorpses.getValue()) {
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Vegetation Corpses");){
                    if (DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) {
                        IsoGridSquare squareW;
                        IsoGridSquare squareN;
                        IsoGridSquare squareNW = c.getGridSquare(0, 0, level);
                        IsoGridSquare isoGridSquare = squareN = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.N);
                        if (squareNW != null && squareN != null) {
                            squareN.cacheLightInfo();
                            this.renderCorpses(squareN, squareNW, true);
                        }
                        IsoGridSquare isoGridSquare2 = squareW = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.W);
                        if (squareNW != null && squareW != null) {
                            squareW.cacheLightInfo();
                            this.renderCorpses(squareW, squareNW, true);
                        }
                    }
                    for (int i = 0; i < IsoCell.VegetationCorpses.size(); ++i) {
                        IsoGridSquare square = IsoCell.VegetationCorpses.get(i);
                        this.renderVegetation(square);
                        if (!DebugOptions.instance.fboRenderChunk.corpsesInChunkTexture.getValue()) continue;
                        this.renderCorpses(square, square, true);
                    }
                }
            }
            if (DebugOptions.instance.terrain.renderTiles.minusFloorCharacters.getValue()) {
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Minus Floor Chars");){
                    IsoGridSquare squareW;
                    IsoGridSquare squareN;
                    if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                        IsoGridSquare squareW2;
                        IsoGridSquare squareN2;
                        IsoGridSquare squareNW = c.getGridSquare(0, 0, level);
                        IsoGridSquare isoGridSquare = squareN2 = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.N);
                        if (squareNW != null && squareN2 != null) {
                            squareN2.cacheLightInfo();
                            this.renderWorldInventoryObjects(squareN2, squareNW, true);
                        }
                        IsoGridSquare isoGridSquare3 = squareW2 = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.W);
                        if (squareNW != null && squareW2 != null) {
                            squareW2.cacheLightInfo();
                            this.renderWorldInventoryObjects(squareW2, squareNW, true);
                        }
                    }
                    FBORenderTrees.current = FBORenderTrees.alloc();
                    FBORenderTrees.current.init();
                    IsoGridSquare squareNW = c.getGridSquare(0, 0, level);
                    IsoGridSquare isoGridSquare = squareN = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.N);
                    if (squareNW != null && squareN != null) {
                        squareN.cacheLightInfo();
                        this.renderMinusFloor(c, squareN);
                    }
                    IsoGridSquare isoGridSquare4 = squareW = squareNW == null ? null : squareNW.getAdjacentSquare(IsoDirections.W);
                    if (squareNW != null && squareW != null) {
                        squareW.cacheLightInfo();
                        this.renderMinusFloor(c, squareW);
                    }
                    for (int i = 0; i < IsoCell.MinusFloorCharacters.size(); ++i) {
                        IsoGridSquare square = IsoCell.MinusFloorCharacters.get(i);
                        if (square.getLightInfo(playerIndex) == null) continue;
                        this.renderMinusFloor(c, square);
                        if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                            this.renderWorldInventoryObjects(square, square, true);
                        }
                        this.renderMinusFloorSE(square);
                    }
                    if (FBORenderTrees.current.trees.isEmpty()) {
                        FBORenderTrees.s_pool.release(FBORenderTrees.current);
                    } else {
                        SpriteRenderer.instance.drawGeneric(FBORenderTrees.current);
                    }
                }
            }
            if (PerformanceSettings.puddlesQuality == 2) {
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Low Puddles");){
                    this.renderPuddlesToChunkTexture(playerIndex, level, c);
                }
            }
            try (GameProfiler.ProfileArea profileArea = profiler.profile("Add Chunk");){
                if (!renderLevels.getCachedSquares_AnimatedAttachments(level).isEmpty()) {
                    perPlayerData1.addChunkWith_AnimatedAttachments(c);
                }
                if (!renderLevels.getCachedSquares_TranslucentFloor(level).isEmpty()) {
                    perPlayerData1.addChunkWith_TranslucentFloor(c);
                }
                if (renderLevels.getCachedSquares_Items(level).size() + renderLevels.getCachedSquares_TranslucentNonFloor(level).size() > 0) {
                    perPlayerData1.addChunkWith_TranslucentNonFloor(c);
                }
            }
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        FBORenderChunkManager.instance.endRenderChunkLevel(c, level, zoom, true);
    }

    private void calculateObjectRenderInfo(IsoGridSquare square) {
        IsoObject object;
        int i;
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.calculateObjectRenderInfo(playerIndex, square, square.getObjects());
        for (i = 0; i < square.getStaticMovingObjects().size(); ++i) {
            object = square.getStaticMovingObjects().get(i);
            if (!(object instanceof IsoDeadBody)) continue;
            IsoDeadBody body = (IsoDeadBody)object;
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            renderInfo.layer = ObjectRenderLayer.Corpse;
            renderInfo.renderAlpha = 1.0f;
            renderInfo.cutaway = false;
        }
        for (i = 0; i < square.getWorldObjects().size(); ++i) {
            object = square.getWorldObjects().get(i);
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            renderInfo.layer = ObjectRenderLayer.WorldInventoryObject;
            renderInfo.renderAlpha = 1.0f;
            renderInfo.cutaway = false;
        }
    }

    private void calculateObjectRenderInfo(int playerIndex, IsoGridSquare square, PZArrayList<IsoObject> objectList) {
        IsoObject[] objects = objectList.getElements();
        int numObjects = objectList.size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            this.calculateObjectRenderInfo(playerIndex, square, object);
        }
    }

    private void calculateObjectRenderInfo(int playerIndex, IsoGridSquare square, IsoObject object) {
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        renderInfo.layer = this.calculateObjectRenderLayer(object);
        renderInfo.targetAlpha = this.calculateObjectTargetAlpha(object);
        renderInfo.renderAlpha = 0.0f;
        renderInfo.cutaway = false;
        if (renderInfo.targetAlpha < 1.0f) {
            if (object instanceof IsoMannequin) {
                boolean bl = true;
            } else if (renderInfo.layer == ObjectRenderLayer.MinusFloor || renderInfo.layer == ObjectRenderLayer.MinusFloorSE) {
                renderInfo.layer = ObjectRenderLayer.Translucent;
            }
        }
        if (renderInfo.layer == ObjectRenderLayer.Translucent && square.getLightLevel(playerIndex) == 0.0f) {
            object.setAlpha(playerIndex, renderInfo.targetAlpha);
        }
        renderInfo.renderHeight = 0.0f;
        renderInfo.renderWidth = 0.0f;
    }

    private void setNotRendered(IsoGridSquare square) {
        if (square == null) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            renderInfo.layer = ObjectRenderLayer.None;
            renderInfo.targetAlpha = 0.0f;
        }
    }

    private ObjectRenderLayer calculateObjectRenderLayer(IsoObject object) {
        if (object instanceof IsoWorldInventoryObject) {
            return ObjectRenderLayer.WorldInventoryObject;
        }
        if (this.isObjectRenderLayer_TranslucentFloor(object)) {
            return ObjectRenderLayer.TranslucentFloor;
        }
        if (this.isObjectRenderLayer_Floor(object)) {
            return ObjectRenderLayer.Floor;
        }
        if (this.isObjectRenderLayer_Vegetation(object)) {
            return ObjectRenderLayer.Vegetation;
        }
        if (this.isObjectRenderLayer_MinusFloor(object)) {
            return ObjectRenderLayer.MinusFloor;
        }
        if (this.isObjectRenderLayer_MinusFloorSE(object)) {
            return ObjectRenderLayer.MinusFloorSE;
        }
        if (this.isObjectRenderLayer_Translucent(object)) {
            return ObjectRenderLayer.Translucent;
        }
        return ObjectRenderLayer.None;
    }

    private boolean isObjectRenderLayer_Floor(IsoObject object) {
        IsoWaterGeometry water;
        IsoGridSquare square = object.square;
        if (square == null) {
            return false;
        }
        boolean bDoIt = true;
        if (object.sprite != null && !object.sprite.solidfloor && object.sprite.renderLayer != 1) {
            bDoIt = false;
        }
        if (object instanceof IsoFire || object instanceof IsoCarBatteryCharger) {
            bDoIt = false;
        }
        IsoWaterGeometry isoWaterGeometry = water = square.z == 0 ? square.getWater() : null;
        if (IsoWater.getInstance().getShaderEnable() && water != null && water.isValid() && object.sprite != null && object.sprite.properties.has(IsoFlagType.water)) {
            bDoIt = water.isbShore();
        }
        if (bDoIt && IsoWater.getInstance().getShaderEnable() && water != null && water.isValid() && !water.isbShore()) {
            IsoObject waterObj = square.getWaterObject();
            boolean bl = bDoIt = waterObj != null && waterObj.getObjectIndex() < object.getObjectIndex();
        }
        if (bDoIt && object.sprite != null && object.sprite.getProperties().getSlopedSurfaceDirection() != null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (FBORenderCutaways.getInstance().shouldHideElevatedFloor(playerIndex, object)) {
            return false;
        }
        return bDoIt;
    }

    private boolean isObjectRenderLayer_Vegetation(IsoObject object) {
        IsoGridSquare square = object.square;
        boolean bGrassEtc = object.sprite != null && (object.sprite.isBush || object.sprite.canBeRemoved || object.sprite.attachedFloor);
        return bGrassEtc && square.flattenGrassEtc;
    }

    private boolean isObjectRenderLayer_MinusFloor(IsoObject object) {
        boolean bGrassEtc;
        IsoMannequin mannequin;
        IsoSprite sprite = object.getSprite();
        if (sprite != null && (sprite.depthFlags & 2) != 0) {
            return false;
        }
        if (object.isAnimating()) {
            return false;
        }
        if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            if (object instanceof IsoTree) {
                return false;
            }
            if (object.getWindRenderEffects() != null) {
                return false;
            }
        } else {
            if (this.isTranslucentTree(object)) {
                return false;
            }
            if (object instanceof IsoTree && object.getObjectRenderEffects() != null) {
                return false;
            }
        }
        if (object.getObjectRenderEffectsToApply() != null) {
            return false;
        }
        if (object instanceof IsoTree) {
            IsoTree isoTree = (IsoTree)object;
            if (isoTree.fadeAlpha < 1.0f) {
                return false;
            }
        }
        if ((mannequin = Type.tryCastTo(object, IsoMannequin.class)) != null && mannequin.shouldRenderEachFrame()) {
            return false;
        }
        IsoGridSquare square = object.square;
        boolean bDoIt = true;
        IsoObjectType t = IsoObjectType.MAX;
        if (sprite != null) {
            t = sprite.getTileType();
        }
        if (sprite != null && (sprite.solidfloor || sprite.renderLayer == 1) && sprite.getProperties().getSlopedSurfaceDirection() == null) {
            bDoIt = false;
        }
        if (object instanceof IsoFire) {
            bDoIt = false;
        }
        int maxZ = 1000;
        if (!(square.z < 1000 || sprite != null && sprite.alwaysDraw)) {
            bDoIt = false;
        }
        boolean bl = bGrassEtc = sprite != null && (sprite.isBush || sprite.canBeRemoved || sprite.attachedFloor);
        if (bGrassEtc && square.flattenGrassEtc) {
            return false;
        }
        if (sprite != null && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && square.z == 999 && square.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
            bDoIt = false;
        }
        if (sprite != null && !sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
            bDoIt = true;
        }
        if (square.isSpriteOnSouthOrEastWall(object)) {
            bDoIt = false;
        }
        boolean bTranslucent = object instanceof IsoWindow;
        IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
        bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans");
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (bTranslucent |= sprite != null && (sprite.solid || sprite.solidTrans) && (object.getAlpha(playerIndex) < 1.0f && perPlayerData1.isFadingInSquare(square) || perPlayerData1.isSquareObscuringPlayer(square))) {
            bDoIt = false;
        }
        return bDoIt;
    }

    private boolean isObjectRenderLayer_MinusFloorSE(IsoObject object) {
        boolean bGrassEtc;
        IsoGridSquare square = object.square;
        boolean bDoIt = true;
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getTileType();
        }
        if (object.sprite != null && (object.sprite.solidfloor || object.sprite.renderLayer == 1)) {
            bDoIt = false;
        }
        if (object instanceof IsoFire) {
            bDoIt = false;
        }
        int maxZ = 1000;
        if (!(square.z < 1000 || object.sprite != null && object.sprite.alwaysDraw)) {
            bDoIt = false;
        }
        boolean bl = bGrassEtc = object.sprite != null && (object.sprite.isBush || object.sprite.canBeRemoved || object.sprite.attachedFloor);
        if (bGrassEtc) {
            return false;
        }
        if (object.sprite != null && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && square.z == 999 && square.z == PZMath.fastfloor(IsoCamera.getCameraCharacterZ())) {
            bDoIt = false;
        }
        if (object.sprite != null && !object.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
            bDoIt = true;
        }
        if (!square.isSpriteOnSouthOrEastWall(object)) {
            bDoIt = false;
        }
        boolean bTranslucent = object instanceof IsoWindow;
        IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
        if (bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans")) {
            bDoIt = false;
        }
        return bDoIt;
    }

    private boolean isObjectRenderLayer_TranslucentFloor(IsoObject object) {
        boolean bTranslucent;
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (FBORenderCutaways.getInstance().shouldHideElevatedFloor(playerIndex, object)) {
            return false;
        }
        boolean bl = bTranslucent = object.getSprite() != null && object.getSprite().getProperties().has(IsoFlagType.transparentFloor);
        if (object.getSprite() != null && object.getSprite().solidfloor && IsoWater.getInstance().getShaderEnable() && DebugOptions.instance.terrain.renderTiles.isoGridSquare.shoreFade.getValue()) {
            boolean isShore;
            IsoWaterGeometry water = object.square.z == 0 ? object.square.getWater() : null;
            boolean bl2 = isShore = water != null && water.isbShore();
            if (isShore) {
                return true;
            }
        }
        return bTranslucent;
    }

    private boolean isObjectRenderLayer_Translucent(IsoObject object) {
        IsoSprite sprite = object.getSprite();
        if (sprite != null && (sprite.depthFlags & 2) != 0) {
            return true;
        }
        if (object instanceof IsoFire) {
            return true;
        }
        if (object.isAnimating()) {
            return true;
        }
        if (Core.getInstance().getOptionDoWindSpriteEffects()) {
            if (object instanceof IsoTree) {
                return true;
            }
            if (object.getWindRenderEffects() != null) {
                return true;
            }
        } else {
            if (this.isTranslucentTree(object)) {
                return true;
            }
            if (object instanceof IsoTree && object.getObjectRenderEffects() != null) {
                return true;
            }
        }
        if (object.getObjectRenderEffectsToApply() != null) {
            return true;
        }
        if (object instanceof IsoTree) {
            IsoTree isoTree = (IsoTree)object;
            if (isoTree.fadeAlpha < 1.0f) {
                return true;
            }
        }
        boolean bTranslucent = object instanceof IsoWindow;
        IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
        bTranslucent |= door != null && door.getProperties() != null && door.getProperties().has("doorTrans");
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        return bTranslucent |= sprite != null && (sprite.solid || sprite.solidTrans) && (object.getAlpha(playerIndex) < 1.0f && perPlayerData1.isFadingInSquare(object.square) || perPlayerData1.isSquareObscuringPlayer(object.square));
    }

    public boolean isTranslucentTree(IsoObject object) {
        if (!(object instanceof IsoTree)) {
            return false;
        }
        IsoTree tree = (IsoTree)object;
        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderLevels renderLevels = object.square.chunk.getRenderLevels(playerIndex);
        if (!renderLevels.inStencilRect) {
            return false;
        }
        IsoGridSquare square = object.square;
        square.IsOnScreen();
        float sx = square.cachedScreenX - IsoCamera.frameState.offX;
        float sy = square.cachedScreenY - IsoCamera.frameState.offY;
        IsoCell cell = IsoWorld.instance.currentCell;
        if (sx + (float)(32 * Core.tileScale) <= (float)cell.stencilX1 || sx - (float)(32 * Core.tileScale) >= (float)cell.stencilX2 || sy + (float)(32 * Core.tileScale) <= (float)cell.stencilY1 || sy - (float)(96 * Core.tileScale) >= (float)cell.stencilY2) {
            return false;
        }
        return square.x >= PZMath.fastfloor(IsoCamera.frameState.camCharacterX) && square.y >= PZMath.fastfloor(IsoCamera.frameState.camCharacterY) && IsoCamera.frameState.camCharacterSquare != null;
    }

    /*
     * Unable to fully structure code
     */
    private float calculateObjectTargetAlpha(IsoObject object) {
        block6: {
            block7: {
                playerIndex = IsoCamera.frameState.playerIndex;
                renderLayer = object.getRenderInfo((int)playerIndex).layer;
                t = IsoObjectType.MAX;
                if (object.sprite != null) {
                    t = object.sprite.getTileType();
                }
                if (renderLayer != ObjectRenderLayer.MinusFloor && renderLayer != ObjectRenderLayer.MinusFloorSE && renderLayer != ObjectRenderLayer.Translucent) break block6;
                if (!(object instanceof IsoDoor)) break block7;
                door = (IsoDoor)object;
                if (door.open) ** GOTO lbl-1000
            }
            if (object instanceof IsoThumpable) {
                isoThumpable = (IsoThumpable)object;
                ** if (!isoThumpable.open) goto lbl-1000
            }
            ** GOTO lbl-1000
lbl-1000:
            // 2 sources

            {
                v0 = true;
                ** GOTO lbl18
            }
lbl-1000:
            // 2 sources

            {
                v0 = isOpenDoor = false;
            }
lbl18:
            // 2 sources

            if (isOpenDoor && object.getProperties() != null && !object.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
                return 0.6f;
            }
            isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW != false;
            v1 = isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN != false;
            if (isWestDoorOrWall || isNorthDoorOrWall) {
                return this.calculateObjectTargetAlpha_DoorOrWall(object);
            }
            return this.calculateObjectTargetAlpha_NotDoorOrWall(object);
        }
        return 1.0f;
    }

    private float calculateObjectTargetAlpha_DoorOrWall(IsoObject object) {
        int cutawayE;
        if (object.sprite == null) {
            return 1.0f;
        }
        if (object.sprite.cutW && object.sprite.cutN) {
            return 1.0f;
        }
        IsoObjectType t = object.sprite.getTileType();
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGridSquare square = object.getSquare();
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        if (object.isFascia() && this.shouldHideFascia(playerIndex, object)) {
            object.setAlphaAndTarget(playerIndex, 0.0f);
            return 0.0f;
        }
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int n = cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        if (t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite.cutW) {
            boolean isCutaway;
            IsoObjectType doorFrType = IsoObjectType.doorFrW;
            IsoObjectType doorType = IsoObjectType.doorW;
            boolean isDoor = t == doorFrType || t == doorType;
            boolean isWindow = object instanceof IsoWindow;
            boolean bl = isCutaway = (cutawaySelf & 2) != 0;
            if ((isDoor || isWindow) && isCutaway) {
                if (isDoor && !this.hasSeenDoorW(playerIndex, square)) {
                    return 0.0f;
                }
                if (isWindow && !this.hasSeenWindowW(playerIndex, square)) {
                    return 0.0f;
                }
                return 0.4f;
            }
        } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite.cutN) {
            boolean isCutaway;
            IsoObjectType doorFrType = IsoObjectType.doorFrN;
            IsoObjectType doorType = IsoObjectType.doorN;
            boolean isDoor = t == doorFrType || t == doorType;
            boolean isWindow = object instanceof IsoWindow;
            boolean bl = isCutaway = (cutawaySelf & 1) != 0;
            if ((isDoor || isWindow) && isCutaway) {
                if (isDoor && !this.hasSeenDoorN(playerIndex, square)) {
                    return 0.0f;
                }
                if (isWindow && !this.hasSeenWindowN(playerIndex, square)) {
                    return 0.0f;
                }
                return 0.4f;
            }
        }
        return 1.0f;
    }

    private boolean hasSeenDoorW(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenDoorW = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObjectType type;
            IsoObject obj = objectArray[i];
            IsoSprite sprite = obj.sprite;
            IsoObjectType isoObjectType = type = sprite == null ? IsoObjectType.MAX : sprite.getTileType();
            if (type != IsoObjectType.doorFrW && type != IsoObjectType.doorW) continue;
            IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
            bHasSeenDoorW |= true;
        }
        return bHasSeenDoorW;
    }

    private boolean hasSeenDoorN(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenDoorN = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObjectType type;
            IsoObject obj = objectArray[i];
            IsoSprite sprite = obj.sprite;
            IsoObjectType isoObjectType = type = sprite == null ? IsoObjectType.MAX : sprite.getTileType();
            if (type != IsoObjectType.doorFrN && type != IsoObjectType.doorN) continue;
            IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
            bHasSeenDoorN |= true;
        }
        return bHasSeenDoorN;
    }

    private boolean hasSeenWindowW(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenWindowW = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject obj = objectArray[i];
            if (!square.isWindowOrWindowFrame(obj, false)) continue;
            IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
            bHasSeenWindowW |= true;
        }
        return bHasSeenWindowW;
    }

    private boolean hasSeenWindowN(int playerIndex, IsoGridSquare square) {
        boolean bCouldSee = true;
        boolean bHasSeenWindowN = false;
        IsoObject[] objectArray = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject obj = objectArray[i];
            if (!square.isWindowOrWindowFrame(obj, true)) continue;
            IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
            bHasSeenWindowN |= true;
        }
        return bHasSeenWindowN;
    }

    private float calculateObjectTargetAlpha_NotDoorOrWall(IsoObject object) {
        boolean bIsValidOverhang;
        boolean bCutawayWest;
        int cutawaySelf;
        IsoBarricade barricade;
        IsoCurtain curtain;
        Object attachedTo;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGridSquare square = object.getSquare();
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getTileType();
        }
        if (object instanceof IsoCurtain && (attachedTo = (curtain = (IsoCurtain)object).getObjectAttachedTo()) != null && square.getTargetDarkMulti(playerIndex) <= ((IsoObject)attachedTo).getSquare().getTargetDarkMulti(playerIndex)) {
            return this.calculateObjectTargetAlpha_NotDoorOrWall((IsoObject)attachedTo);
        }
        if (object instanceof IsoBarricade && (attachedTo = (barricade = (IsoBarricade)object).getBarricadedObject()) instanceof IsoObject) {
            IsoObject isoObject = (IsoObject)attachedTo;
            if (square.getTargetDarkMulti(playerIndex) <= attachedTo.getSquare().getTargetDarkMulti(playerIndex)) {
                return this.calculateObjectTargetAlpha_NotDoorOrWall(isoObject);
            }
        }
        boolean bCutawayNorth = ((cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis)) & 1) != 0;
        boolean bl = bCutawayWest = (cutawaySelf & 2) != 0;
        if (object instanceof IsoWindowFrame) {
            IsoWindowFrame windowFrame = (IsoWindowFrame)object;
            return this.calculateWindowTargetAlpha(playerIndex, object, windowFrame.getOppositeSquare(), windowFrame.getNorth());
        }
        if (object instanceof IsoWindow) {
            IsoWindow window = (IsoWindow)object;
            return this.calculateWindowTargetAlpha(playerIndex, object, window.getOppositeSquare(), window.getNorth());
        }
        boolean bIsRoof = t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT;
        boolean bl2 = bIsValidOverhang = bIsRoof && PZMath.fastfloor(IsoCamera.frameState.camCharacterZ) == square.getZ() && square.getBuilding() == null;
        if (bIsValidOverhang && FBORenderCutaways.getInstance().CanBuildingSquareOccludePlayer(square, playerIndex)) {
            return 0.05f;
        }
        if (object.isFascia() && this.shouldHideFascia(playerIndex, object)) {
            object.setAlphaAndTarget(playerIndex, 0.0f);
            return 0.0f;
        }
        if (IsoCamera.frameState.camCharacterSquare == null || IsoCamera.frameState.camCharacterSquare.getRoom() != square.getRoom()) {
            boolean bCutaway = false;
            if (square.has(IsoFlagType.cutN) && square.has(IsoFlagType.cutW)) {
                bCutaway = bCutawayNorth || bCutawayWest;
            } else if (square.has(IsoFlagType.cutW)) {
                bCutaway = bCutawayWest;
            } else if (square.has(IsoFlagType.cutN)) {
                bCutaway = bCutawayNorth;
            }
            if (bCutaway) {
                return square.isCanSee(playerIndex) ? 0.25f : 0.0f;
            }
        }
        if (this.isPotentiallyObscuringObject(object) && this.perPlayerData[playerIndex].isSquareObscuringPlayer(square)) {
            if (object.sprite != null && object.sprite.getProperties().has(IsoFlagType.attachedCeiling)) {
                return 0.25f;
            }
            if (object.isStairsObject()) {
                return 0.5f;
            }
            return 0.66f;
        }
        return 1.0f;
    }

    public float calculateWindowTargetAlpha(int playerIndex, IsoObject object, IsoGridSquare oppositeSq, boolean bNorth) {
        IsoGridSquare square = object.getSquare();
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        boolean bCutawayNorth = (cutawaySelf & 1) != 0;
        boolean bCutawayWest = (cutawaySelf & 2) != 0;
        float targetAlpha = 1.0f;
        if (object.getTargetAlpha(playerIndex) < 1.0E-4f && oppositeSq != null && oppositeSq != square && oppositeSq.lighting[playerIndex].bSeen()) {
            targetAlpha = oppositeSq.lighting[playerIndex].darkMulti() * 2.0f;
        }
        if (targetAlpha > 0.75f && (bCutawayNorth && bNorth || bCutawayWest && !bNorth)) {
            float maxOpacity = 0.75f;
            float minOpacity = 0.1f;
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null) {
                float maxFadeDistanceSquared = 25.0f;
                float distanceSquared = PZMath.min(IsoUtils.DistanceToSquared(player.getX(), player.getY(), (float)square.x + 0.5f, (float)square.y + 0.5f), 25.0f);
                float fadeAmount = PZMath.lerp(0.1f, 0.75f, 1.0f - distanceSquared / 25.0f);
                targetAlpha = Math.max(fadeAmount, 0.1f);
            } else {
                targetAlpha = 0.1f;
            }
        }
        return targetAlpha;
    }

    public void renderFloor(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.Floor) continue;
            this.renderFloor(object);
        }
    }

    public void renderFloor(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IndieGL.glAlphaFunc(516, 0.0f);
        object.setTargetAlpha(playerIndex, renderInfo.targetAlpha);
        object.setAlpha(playerIndex, renderInfo.targetAlpha);
        if (!DebugOptions.instance.terrain.renderTiles.renderGridSquares.getValue()) {
            return;
        }
        if (object.sprite == null) {
            return;
        }
        IndieGL.glDepthMask(true);
        if (object.sprite.getProperties().getSlopedSurfaceDirection() != null && square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0) {
            IsoSprite sprite = IsoSpriteManager.instance.getSprite("ramps_01_23");
            this.defColorInfo.set(square.getLightInfo(playerIndex));
            if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                this.defColorInfo.set(1.0f, 1.0f, 1.0f, this.defColorInfo.a);
            }
            sprite.render(object, square.x, square.y, square.z, object.getDir(), object.offsetX, object.offsetY, this.defColorInfo, true);
            return;
        }
        FloorShaperAttachedSprites attachedFloorShaper = FloorShaperAttachedSprites.instance;
        FloorShaper floorShaper = object.getProperties().has(IsoFlagType.diamondFloor) || object.getProperties().has(IsoFlagType.water) ? FloorShaperDiamond.instance : FloorShaperDeDiamond.instance;
        IsoWaterGeometry water = square.z == 0 ? square.getWater() : null;
        boolean isShore = water != null && water.isbShore() && IsoWater.getInstance().getShaderEnable();
        float depth0 = water == null ? 0.0f : water.depth[0];
        float depth1 = water == null ? 0.0f : water.depth[3];
        float depth2 = water == null ? 0.0f : water.depth[2];
        float depth3 = water == null ? 0.0f : water.depth[1];
        int col0 = square.getVertLight(0, playerIndex);
        int col1 = square.getVertLight(1, playerIndex);
        int col2 = square.getVertLight(2, playerIndex);
        int col3 = square.getVertLight(3, playerIndex);
        if (this.isBlackedOutBuildingSquare(square)) {
            float fade = instance.getBlackedOutRoomFadeRatio(square);
            col0 = Color.lerpABGR(col0, -16777216, fade);
            col1 = Color.lerpABGR(col1, -16777216, fade);
            col2 = Color.lerpABGR(col2, -16777216, fade);
            col3 = Color.lerpABGR(col3, -16777216, fade);
        }
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
            col0 = -65536;
            col1 = -65536;
            col2 = -16776961;
            col3 = -16776961;
        }
        attachedFloorShaper.setShore(isShore);
        attachedFloorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
        attachedFloorShaper.setVertColors(col0, col1, col2, col3);
        floorShaper.setShore(isShore);
        floorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
        floorShaper.setVertColors(col0, col1, col2, col3);
        TileSeamModifier.instance.setShore(isShore);
        TileSeamModifier.instance.setWaterDepth(depth0, depth1, depth2, depth3);
        TileSeamModifier.instance.setVertColors(col0, col1, col2, col3);
        IsoGridSquare.setBlendFunc();
        Shader floorShader = null;
        IndieGL.StartShader(floorShader, playerIndex);
        this.defColorInfo.set(1.0f, 1.0f, 1.0f, 1.0f);
        object.renderFloorTile(square.x, square.y, square.z, this.defColorInfo, true, false, floorShader, floorShaper, attachedFloorShaper);
        IndieGL.EndShader();
    }

    private void renderFishSplashes(int playerIndex, ArrayList<IsoGridSquare> squares) {
        IndieGL.glBlendFunc(770, 771);
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare square = squares.get(i);
            ColorInfo lightInfo = square.getLightInfo(playerIndex);
            square.renderFishSplash(playerIndex, lightInfo);
        }
    }

    private void renderVegetation(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.Vegetation) continue;
            this.renderVegetation(object);
        }
    }

    private void renderVegetation(IsoObject object) {
        this.renderMinusFloor(object);
    }

    private void renderCorpsesInWorld(int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                if (!renderLevels.isOnScreen(z) || z != renderLevels.getMinLevel(z)) continue;
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Corpses(z);
                for (int j = 0; j < squares.size(); ++j) {
                    IsoGridSquare square = squares.get(j);
                    this.renderCorpses(square, square, false);
                }
            }
        }
    }

    private void renderCorpses(IsoGridSquare square, IsoGridSquare renderSquare, boolean bInChunkTexture) {
        if (!this.shouldRenderSquare(renderSquare)) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = square.getLightInfo(playerIndex);
        FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
        for (int i = 0; i < square.getStaticMovingObjects().size(); ++i) {
            IsoMovingObject mov = square.getStaticMovingObjects().get(i);
            if (mov.sprite == null && !(mov instanceof IsoDeadBody) || !(mov instanceof IsoDeadBody)) continue;
            IsoDeadBody isoDeadBody = (IsoDeadBody)mov;
            if (bInChunkTexture) {
                if (renderSquare != mov.getRenderSquare()) continue;
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(square.z, Core.getInstance().getZoom(playerIndex));
                FBORenderCorpses.getInstance().render(renderChunk.index, isoDeadBody);
                continue;
            }
            mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
        }
        int size = square.getMovingObjects().size();
        for (int i = 0; i < size; ++i) {
            IsoMovingObject mov = square.getMovingObjects().get(i);
            if (mov == null || mov.sprite == null) continue;
            boolean bOnFloor = mov.isOnFloor();
            if (bOnFloor && mov instanceof IsoZombie) {
                IsoZombie zombie = (IsoZombie)mov;
                bOnFloor = zombie.isProne();
                if (!BaseVehicle.renderToTexture) {
                    bOnFloor = false;
                }
            }
            if (!bOnFloor) continue;
            mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
        }
    }

    private void renderItemsInWorld(int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                if (!renderLevels.isOnScreen(z) || z != renderLevels.getMinLevel(z)) continue;
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Items(z);
                for (int j = 0; j < squares.size(); ++j) {
                    IsoGridSquare square = squares.get(j);
                    if (square.chunk == chunk) {
                        this.renderWorldInventoryObjects(square, square, false);
                        continue;
                    }
                    IsoGridSquare renderSquare = chunk.getGridSquare(0, 0, square.z);
                    this.renderWorldInventoryObjects(square, renderSquare, false);
                }
            }
        }
    }

    private void renderMinusFloor(IsoChunk chunk, IsoGridSquare square) {
        this.renderMinusFloor(chunk, square, square.getObjects());
    }

    private void renderMinusFloor(IsoChunk chunk, IsoGridSquare square, PZArrayList<IsoObject> objectList) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        boolean bForceRender = FBORenderCutaways.getInstance().isForceRenderSquare(playerIndex, square);
        IsoObject[] objects = objectList.getElements();
        int numObjects = objectList.size();
        for (int i = 0; i < numObjects; ++i) {
            IsoSpriteGrid spriteGrid;
            IsoGridSquare renderSquare;
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.MinusFloor || (renderSquare = object.getRenderSquare()) == null || chunk != renderSquare.chunk || bForceRender && ((spriteGrid = object.getSpriteGrid()) == null || spriteGrid.getLevels() == 1)) continue;
            this.renderMinusFloor(object);
        }
    }

    private void renderMinusFloor(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getTileType();
        }
        boolean isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW;
        boolean isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN;
        IndieGL.glAlphaFunc(516, 0.0f);
        object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        IsoGridSquare.setBlendFunc();
        if (object.sprite != null && (isWestDoorOrWall || isNorthDoorOrWall)) {
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                this.renderMinusFloor_DoorOrWall(object);
            }
        } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
            this.renderMinusFloor_NotDoorOrWall(object);
        }
    }

    private void renderMinusFloor_DoorOrWall(IsoObject object) {
        boolean bNeverCutaway;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getTileType();
        }
        IndieGL.glAlphaFunc(516, 0.0f);
        object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        this.defColorInfo.set(1.0f, 1.0f, 1.0f, 1.0f);
        boolean stenciled = false;
        Shader wallRenderShader = null;
        boolean bHasSeenDoorN = false;
        boolean bHasSeenDoorW = false;
        boolean bHasSeenWindowN = false;
        boolean bHasSeenWindowW = false;
        boolean bCouldSee = square.lighting[playerIndex].bCouldSee();
        lowestCutawayObjectN = null;
        lowestCutawayObjectW = null;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoGridSquare toWest;
            IsoGridSquare toNorth;
            IsoObjectType t2;
            IsoObject obj = objects[i];
            IsoObjectType isoObjectType = t2 = obj.sprite == null ? IsoObjectType.MAX : obj.sprite.getTileType();
            if (lowestCutawayObjectN == null && square.isWindowOrWindowFrame(obj, true) && (cutawaySelf & 1) != 0) {
                toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenWindowN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                lowestCutawayObjectN = obj;
            }
            if (lowestCutawayObjectW == null && square.isWindowOrWindowFrame(obj, false) && (cutawaySelf & 2) != 0) {
                toWest = square.getAdjacentSquare(IsoDirections.W);
                bHasSeenWindowW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
                lowestCutawayObjectW = obj;
            }
            if (lowestCutawayObjectN == null && obj.sprite != null && (t2 == IsoObjectType.doorFrN || t2 == IsoObjectType.doorN || obj.sprite.getProperties().has(IsoFlagType.DoorWallN)) && (cutawaySelf & 1) != 0) {
                toNorth = square.getAdjacentSquare(IsoDirections.N);
                bHasSeenDoorN = bCouldSee || toNorth != null && toNorth.isCouldSee(playerIndex);
                lowestCutawayObjectN = obj;
            }
            if (lowestCutawayObjectW != null || obj.sprite == null || t2 != IsoObjectType.doorFrW && t2 != IsoObjectType.doorW && !obj.sprite.getProperties().has(IsoFlagType.DoorWallW) || (cutawaySelf & 2) == 0) continue;
            toWest = square.getAdjacentSquare(IsoDirections.W);
            bHasSeenDoorW = bCouldSee || toWest != null && toWest.isCouldSee(playerIndex);
            lowestCutawayObjectW = obj;
        }
        IsoGridSquare.circleStencil = true;
        boolean bl = bNeverCutaway = object.getProperties() != null && object.getProperties().has(IsoFlagType.NeverCutaway);
        if (bNeverCutaway) {
            IsoGridSquare.circleStencil = false;
        }
        IndieGL.glDepthMask(true);
        if (object.isWallSE()) {
            square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader);
        } else if (object.sprite.cutW && object.sprite.cutN) {
            square.DoWallLightingNW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenDoorW, bHasSeenWindowN, bHasSeenWindowW, wallRenderShader);
        } else if (t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite.cutW) {
            square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader);
        } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite.cutN) {
            square.DoWallLightingN(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenWindowN, wallRenderShader);
        }
    }

    void renderMinusFloor_NotDoorOrWall(IsoObject object) {
        IsoGridSquare square2;
        IsoGridSquare below;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
        IsoGridSquare square = object.square;
        IndieGL.glAlphaFunc(516, 0.0f);
        if (this.renderTranslucentOnly) {
            object.setTargetAlpha(playerIndex, renderInfo.targetAlpha);
            if (object.getType() == IsoObjectType.WestRoofT) {
                object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
            }
        } else {
            object.setAlphaAndTarget(playerIndex, renderInfo.targetAlpha);
        }
        ColorInfo lightInfo = this.sanitizeLightInfo(playerIndex, square);
        boolean bForceRender = FBORenderCutaways.getInstance().isForceRenderSquare(playerIndex, square);
        if (bForceRender && (below = this.cell.getGridSquare(square.x, square.y, square.z - 1)) != null) {
            lightInfo = this.sanitizeLightInfo(playerIndex, below);
        }
        if (object instanceof IsoTree) {
            IsoTree isoTree = (IsoTree)object;
            isoTree.renderFlag = this.isTranslucentTree(object);
        }
        IndieGL.glDepthMask(true);
        if (this.isRoofTileWithPossibleSeamSameLevel(square, object.sprite, IsoDirections.E)) {
            square2 = square.getAdjacentSquare(IsoDirections.E);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.E);
        }
        if (this.isRoofTileWithPossibleSeamSameLevel(square, object.sprite, IsoDirections.S)) {
            square2 = square.getAdjacentSquare(IsoDirections.S);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.S);
        }
        if (this.isRoofTileWithPossibleSeamBelow(square, object.sprite, IsoDirections.E)) {
            square2 = this.cell.getGridSquare(square.x + 1, square.y, square.z - 1);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.E);
        }
        if (this.isRoofTileWithPossibleSeamBelow(square, object.sprite, IsoDirections.S)) {
            square2 = this.cell.getGridSquare(square.x, square.y + 1, square.z - 1);
            this.renderJoinedRoofTile(playerIndex, object, square2, IsoDirections.S);
        }
        if (object instanceof IsoWindow) {
            IsoWindow window = (IsoWindow)object;
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis);
            boolean stenciled = false;
            Shader wallRenderShader = null;
            boolean bHasSeenDoorN = false;
            boolean bHasSeenDoorW = false;
            boolean bHasSeenWindowN = false;
            boolean bHasSeenWindowW = false;
            if (window.getNorth() && object == square.getWall(true)) {
                IsoGridSquare.circleStencil = false;
                square.DoWallLightingN(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, false, false, wallRenderShader);
                return;
            }
            if (!window.getNorth() && object == square.getWall(false)) {
                IsoGridSquare.circleStencil = false;
                square.DoWallLightingW(object, 0, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, false, false, wallRenderShader);
                return;
            }
        }
        object.render(square.x, square.y, square.z, lightInfo, true, false, null);
    }

    private boolean isRoofTileset(IsoSprite sprite) {
        if (sprite == null) {
            return false;
        }
        return sprite.getRoofProperties() != null;
    }

    private boolean isRoofTileWithPossibleSeamSameLevel(IsoGridSquare square, IsoSprite sprite, IsoDirections dir) {
        if (sprite == null) {
            return false;
        }
        if (dir == IsoDirections.E && PZMath.coordmodulo(square.x, 8) != 7) {
            return false;
        }
        if (dir == IsoDirections.S && PZMath.coordmodulo(square.y, 8) != 7) {
            return false;
        }
        if (!this.isRoofTileset(sprite)) {
            return false;
        }
        return sprite.getRoofProperties().hasPossibleSeamSameLevel(dir);
    }

    private boolean isRoofTileWithPossibleSeamBelow(IsoGridSquare square, IsoSprite sprite, IsoDirections dir) {
        if (sprite == null) {
            return false;
        }
        if (!this.isRoofTileset(sprite)) {
            return false;
        }
        return sprite.getRoofProperties().hasPossibleSeamLevelBelow(dir);
    }

    private boolean areRoofTilesJoinedSameLevel(IsoSprite sprite1, IsoSprite sprite2, IsoDirections dir) {
        if (!this.isRoofTileset(sprite1)) {
            return false;
        }
        if (!this.isRoofTileset(sprite2)) {
            return false;
        }
        if (dir == IsoDirections.E) {
            return sprite1.getRoofProperties().isJoinedSameLevelEast(sprite2.getRoofProperties());
        }
        if (dir == IsoDirections.S) {
            return sprite1.getRoofProperties().isJoinedSameLevelSouth(sprite2.getRoofProperties());
        }
        return false;
    }

    private boolean areRoofTilesJoinedLevelBelow(IsoSprite sprite1, IsoSprite sprite2, IsoDirections dir) {
        if (!this.isRoofTileset(sprite1)) {
            return false;
        }
        if (!this.isRoofTileset(sprite2)) {
            return false;
        }
        if (dir == IsoDirections.E) {
            return sprite1.getRoofProperties().isJoinedLevelBelowEast(sprite2.getRoofProperties());
        }
        if (dir == IsoDirections.S) {
            return sprite1.getRoofProperties().isJoinedLevelBelowSouth(sprite2.getRoofProperties());
        }
        return false;
    }

    private void renderJoinedRoofTile(int playerIndex, IsoObject object, IsoGridSquare square2, IsoDirections dir) {
        if (square2 == null) {
            return;
        }
        IsoGridSquare square = object.getSquare();
        for (int i = 0; i < square2.getObjects().size(); ++i) {
            IsoObject object2 = square2.getObjects().get(i);
            if (square.z == square2.z ? !this.areRoofTilesJoinedSameLevel(object.sprite, object2.sprite, dir) : !this.areRoofTilesJoinedLevelBelow(object.sprite, object2.sprite, dir)) continue;
            ObjectRenderInfo renderInfo2 = object2.getRenderInfo(playerIndex);
            if (renderInfo2.targetAlpha == 0.0f || square.chunk != square2.chunk) {
                float renderWidth = renderInfo2.renderWidth;
                float renderHeight = renderInfo2.renderHeight;
                this.calculateObjectRenderInfo(playerIndex, object2.square, object2);
                renderInfo2.renderWidth = renderWidth;
                renderInfo2.renderHeight = renderHeight;
                if (object2.getRenderInfo((int)playerIndex).targetAlpha == 0.0f) continue;
            }
            if (object2.getRenderInfo((int)playerIndex).targetAlpha < 1.0f) continue;
            object2.renderSquareOverride = square;
            object2.renderDepthAdjust = -1.0E-5f;
            object2.sx = 0.0f;
            if (this.renderTranslucentOnly) {
                object2.setTargetAlpha(playerIndex, object2.getRenderInfo((int)playerIndex).targetAlpha);
            } else {
                object2.setAlphaAndTarget(playerIndex, object2.getRenderInfo((int)playerIndex).targetAlpha);
            }
            object2.render(square2.x, square2.y, square2.z, this.sanitizeLightInfo(playerIndex, square2), true, false, null);
            object2.sx = 0.0f;
            object2.renderSquareOverride = null;
            object2.renderDepthAdjust = 0.0f;
            break;
        }
    }

    private void renderWorldInventoryObjects(IsoGridSquare square, IsoGridSquare renderSquare, boolean bChunkTexture) {
        if (!this.shouldRenderSquare(renderSquare)) {
            return;
        }
        this.tempWorldInventoryObjects.clear();
        PZArrayUtil.addAll(this.tempWorldInventoryObjects, square.getWorldObjects());
        this.timSort.doSort(this.tempWorldInventoryObjects.getElements(), (o1, o2) -> {
            float d1 = o1.xoff * o1.xoff + o1.yoff * o1.yoff;
            float d2 = o2.xoff * o2.xoff + o2.yoff * o2.yoff;
            if (d1 == d2) {
                return 0;
            }
            return d1 > d2 ? 1 : -1;
        }, 0, this.tempWorldInventoryObjects.size());
        int playerIndex = IsoCamera.frameState.playerIndex;
        for (int i = 0; i < this.tempWorldInventoryObjects.size(); ++i) {
            IsoWorldInventoryObject object = this.tempWorldInventoryObjects.get(i);
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.WorldInventoryObject || bChunkTexture && renderSquare != object.getRenderSquare()) continue;
            this.renderWorldInventoryObject(object, bChunkTexture);
        }
    }

    private void renderWorldInventoryObject(IsoWorldInventoryObject worldObj, boolean bChunkTexture) {
        IsoGridSquare square = worldObj.getSquare();
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (bChunkTexture) {
            IsoGridSquare renderSquare = worldObj.getRenderSquare();
            if (!(worldObj.zoff < 0.01f) && (this.isTableTopObjectFadedOut(playerIndex, square) || this.isTableTopObjectSquareCutaway(playerIndex, square, worldObj.zoff))) {
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Items(renderSquare.z);
                if (!squares.contains(square)) {
                    squares.add(square);
                }
                return;
            }
            if (!worldObj.getItem().getScriptItem().isWorldRender().booleanValue()) {
                return;
            }
            if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(worldObj.getItem())) {
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(renderSquare.z, Core.getInstance().getZoom(playerIndex));
                FBORenderItems.getInstance().render(renderChunk.index, worldObj);
                return;
            }
        }
        if (this.renderTranslucentOnly) {
            if (worldObj.zoff < 0.01f) {
                return;
            }
            if (!this.isTableTopObjectFadedOut(playerIndex, square) && !this.isTableTopObjectSquareCutaway(playerIndex, square, worldObj.zoff)) {
                return;
            }
        }
        ColorInfo lightInfo = square.getLightInfo(playerIndex);
        worldObj.render(square.x, square.y, square.z, lightInfo, true, false, null);
    }

    private boolean isTableTopObjectSquareCutaway(int playerIndex, IsoGridSquare square, float zoff) {
        boolean cutawayE;
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
        boolean cutawaySelf = square.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        boolean cutawayS = squareS != null && squareS.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        boolean bl = cutawayE = squareE != null && squareE.getPlayerCutawayFlag(playerIndex, this.currentTimeMillis) != 0;
        if (IsoCamera.frameState.camCharacterSquare == null || IsoCamera.frameState.camCharacterSquare.getRoom() != square.getRoom()) {
            boolean bCutaway = cutawaySelf;
            if (square.has(IsoFlagType.cutN) && square.has(IsoFlagType.cutW)) {
                bCutaway |= cutawayS | cutawayE;
            } else if (square.has(IsoFlagType.cutW)) {
                bCutaway |= cutawayS;
            } else if (square.has(IsoFlagType.cutN)) {
                bCutaway |= cutawayE;
            }
            if (bCutaway) {
                return true;
            }
        }
        return false;
    }

    private boolean isTableTopObjectFadedOut(int playerIndex, IsoGridSquare square) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        return this.listContainsLocation(perPlayerData1.squaresObscuringPlayer, square.x, square.y, square.z) || this.listContainsLocation(perPlayerData1.fadingInSquares, square.x, square.y, square.z);
    }

    private void renderMinusFloorSE(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = numObjects - 1; i >= 0; --i) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.MinusFloorSE) continue;
            this.renderMinusFloorSE(object);
        }
    }

    private void renderMinusFloorSE(IsoObject object) {
        this.renderMinusFloor(object);
    }

    private void renderTranslucentFloor(IsoGridSquare square) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.TranslucentFloor) continue;
            this.renderTranslucent(object);
        }
    }

    private void renderTranslucent(IsoGridSquare square, boolean bAttachedSE) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[bAttachedSE ? numObjects - 1 - i : i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
            if (renderInfo.layer != ObjectRenderLayer.Translucent || bAttachedSE != square.isSpriteOnSouthOrEastWall(object)) continue;
            this.renderTranslucent(object);
        }
    }

    public void renderTranslucent(IsoObject object) {
        boolean isNorthDoorOrWall;
        IsoThumpable isoThumpable;
        IndieGL.glDefaultBlendFunc();
        IsoSprite sprite = object.getSprite();
        if (sprite != null && sprite.getProperties().has(IsoFlagType.transparentFloor)) {
            this.renderFloor(object);
            return;
        }
        if (object instanceof IsoDoor || object instanceof IsoThumpable && (isoThumpable = (IsoThumpable)object).isDoor()) {
            object.sx = 0.0f;
            this.renderMinusFloor_DoorOrWall(object);
            return;
        }
        if (object.getType() == IsoObjectType.doorFrW || object.getType() == IsoObjectType.doorFrN) {
            this.renderMinusFloor_DoorOrWall(object);
            return;
        }
        if (sprite != null && sprite.solidfloor && object.square.getWater() != null && object.square.getWater().isbShore()) {
            this.renderFloor(object);
            return;
        }
        object.getRenderInfo((int)IsoCamera.frameState.playerIndex).targetAlpha = sprite != null && (sprite.cutN || sprite.cutW) ? this.calculateObjectTargetAlpha_DoorOrWall(object) : this.calculateObjectTargetAlpha_NotDoorOrWall(object);
        IsoObjectType t = IsoObjectType.MAX;
        if (object.sprite != null) {
            t = object.sprite.getTileType();
        }
        boolean isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || object.sprite != null && object.sprite.cutW;
        boolean bl = isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || object.sprite != null && object.sprite.cutN;
        if (object.sprite != null && (isWestDoorOrWall || isNorthDoorOrWall)) {
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                this.renderMinusFloor_DoorOrWall(object);
            }
        } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
            this.renderMinusFloor_NotDoorOrWall(object);
        }
        if (object instanceof IsoBarbecue && FBORenderObjectHighlight.getInstance().isRendering()) {
            return;
        }
        if (object.hasAnimatedAttachments()) {
            this.renderAnimatedAttachments(object);
        }
    }

    private void renderAnimatedAttachments(int playerIndex) {
        this.renderTranslucentOnly = true;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.chunksWithAnimatedAttachments.size(); ++i) {
            IsoChunk chunk = perPlayerData1.chunksWithAnimatedAttachments.get(i);
            this.renderOneChunk_AnimatedAttachments(playerIndex, chunk);
        }
    }

    private void renderOneChunk_AnimatedAttachments(int playerIndex, IsoChunk chunk) {
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);
        for (int zza = chunk.minLevel; zza <= chunk.maxLevel; ++zza) {
            this.renderOneLevel_AnimatedAttachments(playerIndex, chunk, zza);
        }
        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_AnimatedAttachments(int playerIndex, IsoChunk chunk, int level) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            return;
        }
        FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
        ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_AnimatedAttachments(level);
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare square = squares.get(i);
            if (square.z != level || !levelData.shouldRenderSquare(playerIndex, square) || !square.IsOnScreen()) continue;
            this.renderAnimatedAttachments(square);
        }
    }

    private void renderAnimatedAttachments(IsoGridSquare square) {
        this.renderAnimatedAttachments = true;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoObject[] objects = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects[i];
            if (object.getRenderInfo((int)playerIndex).layer == ObjectRenderLayer.Translucent || !object.hasAnimatedAttachments()) continue;
            this.renderAnimatedAttachments(object);
        }
        this.renderAnimatedAttachments = false;
    }

    public void renderAnimatedAttachments(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = object.square.getLightInfo(playerIndex);
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            this.defColorInfo.set(1.0f, 1.0f, 1.0f, lightInfo.a);
            lightInfo = this.defColorInfo;
        }
        IndieGL.glDefaultBlendFunc();
        object.renderAnimatedAttachments(object.getX(), object.getY(), object.getZ(), lightInfo);
    }

    private void renderFlies(int playerIndex) {
        this.renderTranslucentOnly = true;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.chunksWithFlies.size(); ++i) {
            IsoChunk chunk = perPlayerData1.chunksWithFlies.get(i);
            this.renderOneChunk_Flies(playerIndex, chunk);
        }
    }

    private void renderOneChunk_Flies(int playerIndex, IsoChunk chunk) {
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);
        IndieGL.enableBlend();
        IndieGL.glBlendFunc(770, 771);
        for (int zza = chunk.minLevel; zza <= chunk.maxLevel; ++zza) {
            this.renderOneLevel_Flies(playerIndex, chunk, zza);
        }
        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_Flies(int playerIndex, IsoChunk chunk, int level) {
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            return;
        }
        FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
        ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Flies(level);
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare square = squares.get(i);
            if (square.z != level || !levelData.shouldRenderSquare(playerIndex, square) || !square.IsOnScreen() || !square.hasFlies()) continue;
            CorpseFlies.render(square.x, square.y, square.z);
        }
    }

    private void updateChunkLighting(int playerIndex) {
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            return;
        }
        if (!DebugOptions.instance.fboRenderChunk.updateSquareLightInfo.getValue()) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        if (perPlayerData1.lightingUpdateCounter == LightingJNI.getUpdateCounter(playerIndex)) {
            return;
        }
        perPlayerData1.lightingUpdateCounter = LightingJNI.getUpdateCounter(playerIndex);
        int chunkLevelCount = 0;
        this.sortedChunks.clear();
        PZArrayUtil.addAll(this.sortedChunks, perPlayerData1.onScreenChunks);
        this.timSort.doSort(this.sortedChunks.getElements(), Comparator.comparingInt(a -> a.lightingUpdateCounter), 0, this.sortedChunks.size());
        for (int i = 0; i < this.sortedChunks.size(); ++i) {
            IsoChunk chunk = this.sortedChunks.get(i);
            boolean updated = false;
            for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                if (!this.updateChunkLevelLighting(playerIndex, chunk, z)) continue;
                updated = true;
                chunk.lightingUpdateCounter = perPlayerData1.lightingUpdateCounter;
            }
            if (!DebugOptions.instance.lightingSplitUpdate.getValue() || !updated || ++chunkLevelCount < 5) continue;
            return;
        }
    }

    private boolean updateChunkLevelLighting(int playerIndex, IsoChunk chunk, int level) {
        if (level < chunk.minLevel || level > chunk.maxLevel) {
            return false;
        }
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            return false;
        }
        if (!LightingJNI.getChunkDirty(playerIndex, chunk.wx, chunk.wy, level + 32)) {
            return false;
        }
        FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(level);
        IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(level)];
        for (int i = 0; i < squares.length; ++i) {
            IsoGridSquare square = squares[i];
            if (square == null || !levelData.shouldRenderSquare(playerIndex, square)) continue;
            square.cacheLightInfo();
        }
        return true;
    }

    private void renderOneLevel_Blood(IsoChunk chunk, int zza, int minX, int minY, int maxX, int maxY) {
        IsoFloorBloodSplat b;
        int n;
        if (!DebugOptions.instance.terrain.renderTiles.bloodDecals.getValue()) {
            return;
        }
        int optionBloodDecals = Core.getInstance().getOptionBloodDecals();
        if (optionBloodDecals == 0) {
            return;
        }
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderCutaways.ChunkLevelData cutawayLevel = chunk.getCutawayDataForLevel(zza);
        if (this.splatByType.isEmpty()) {
            for (int i = 0; i < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; ++i) {
                this.splatByType.add(new ArrayList());
            }
        }
        for (int n2 = 0; n2 < IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length; ++n2) {
            this.splatByType.get(n2).clear();
        }
        int cx = chunk.wx * 8;
        int cy = chunk.wy * 8;
        for (n = 0; n < chunk.floorBloodSplatsFade.size(); ++n) {
            b = chunk.floorBloodSplatsFade.get(n);
            if (b.index >= 1 && b.index <= 10 && IsoChunk.renderByIndex[optionBloodDecals - 1][b.index - 1] == 0 || (float)cx + b.x < (float)minX || (float)cx + b.x > (float)maxX || (float)cy + b.y < (float)minY || (float)cy + b.y > (float)maxY || PZMath.fastfloor(b.z) != zza || b.type < 0 || b.type >= IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) continue;
            b.chunk = chunk;
            this.splatByType.get(b.type).add(b);
        }
        for (int i = 0; i < chunk.floorBloodSplats.size(); ++i) {
            b = chunk.floorBloodSplats.get(i);
            if (b.index >= 1 && b.index <= 10 && IsoChunk.renderByIndex[optionBloodDecals - 1][b.index - 1] == 0 || (float)cx + b.x < (float)minX || (float)cx + b.x > (float)maxX || (float)cy + b.y < (float)minY || (float)cy + b.y > (float)maxY || PZMath.fastfloor(b.z) != zza || b.type < 0 || b.type >= IsoFloorBloodSplat.FLOOR_BLOOD_TYPES.length) continue;
            b.chunk = chunk;
            this.splatByType.get(b.type).add(b);
        }
        for (n = 0; n < this.splatByType.size(); ++n) {
            IsoSprite use;
            ArrayList<IsoFloorBloodSplat> splats = this.splatByType.get(n);
            if (splats.isEmpty()) continue;
            String type = IsoFloorBloodSplat.FLOOR_BLOOD_TYPES[n];
            if (!IsoFloorBloodSplat.spriteMap.containsKey(type)) {
                IsoSprite sp = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                sp.LoadSingleTexture(type);
                IsoFloorBloodSplat.spriteMap.put(type, sp);
                use = sp;
            } else {
                use = IsoFloorBloodSplat.spriteMap.get(type);
            }
            for (int i = 0; i < splats.size(); ++i) {
                IsoGridSquare square;
                IsoFloorBloodSplat b2 = splats.get(i);
                ColorInfo inf = this.defColorInfo;
                inf.r = 1.0f;
                inf.g = 1.0f;
                inf.b = 1.0f;
                inf.a = 0.27f;
                float aa = (b2.x + b2.y / b2.x) * (float)(b2.type + 1);
                float bb = aa * b2.x / b2.y * (float)(b2.type + 1) / (aa + b2.y);
                float cc = bb * aa * bb * b2.x / (b2.y + 2.0f);
                aa *= 42367.543f;
                bb *= 6367.123f;
                cc *= 23367.133f;
                aa %= 1000.0f;
                bb %= 1000.0f;
                cc %= 1000.0f;
                aa /= 1000.0f;
                bb /= 1000.0f;
                cc /= 1000.0f;
                if (aa > 0.25f) {
                    aa = 0.25f;
                }
                inf.r -= aa * 2.0f;
                inf.g -= aa * 2.0f;
                inf.b -= aa * 2.0f;
                inf.r += bb / 3.0f;
                inf.g -= cc / 3.0f;
                inf.b -= cc / 3.0f;
                float deltaAge = worldAge - b2.worldAge;
                if (deltaAge >= 0.0f && deltaAge < 72.0f) {
                    float f = 1.0f - deltaAge / 72.0f;
                    inf.r *= 0.2f + f * 0.8f;
                    inf.g *= 0.2f + f * 0.8f;
                    inf.b *= 0.2f + f * 0.8f;
                    inf.a *= 0.25f + f * 0.75f;
                } else {
                    inf.r *= 0.2f;
                    inf.g *= 0.2f;
                    inf.b *= 0.2f;
                    inf.a *= 0.25f;
                }
                if (b2.fade > 0) {
                    inf.a *= (float)b2.fade / ((float)PerformanceSettings.getLockFPS() * 5.0f);
                    if (--b2.fade == 0) {
                        b2.chunk.floorBloodSplatsFade.remove(b2);
                    }
                }
                if (!cutawayLevel.shouldRenderSquare(playerIndex, square = b2.chunk.getGridSquare(PZMath.fastfloor(b2.x), PZMath.fastfloor(b2.y), PZMath.fastfloor(b2.z)))) continue;
                if (this.isBlackedOutBuildingSquare(square)) {
                    inf.set(0.0f, 0.0f, 0.0f, inf.a);
                }
                if (square != null) {
                    int l0 = square.getVertLight(0, playerIndex);
                    int l1 = square.getVertLight(1, playerIndex);
                    int l2 = square.getVertLight(2, playerIndex);
                    int l3 = square.getVertLight(3, playerIndex);
                    float r0 = Color.getRedChannelFromABGR(l0);
                    float g0 = Color.getGreenChannelFromABGR(l0);
                    float b0 = Color.getBlueChannelFromABGR(l0);
                    float r1 = Color.getRedChannelFromABGR(l1);
                    float g1 = Color.getGreenChannelFromABGR(l1);
                    float b1 = Color.getBlueChannelFromABGR(l1);
                    float r2 = Color.getRedChannelFromABGR(l2);
                    float g2 = Color.getGreenChannelFromABGR(l2);
                    float b22 = Color.getBlueChannelFromABGR(l2);
                    float r3 = Color.getRedChannelFromABGR(l3);
                    float g3 = Color.getGreenChannelFromABGR(l3);
                    float b3 = Color.getBlueChannelFromABGR(l3);
                    inf.r *= (r0 + r1 + r2 + r3) / 4.0f;
                    inf.g *= (g0 + g1 + g2 + g3) / 4.0f;
                    inf.b *= (b0 + b1 + b22 + b3) / 4.0f;
                }
                use.renderBloodSplat((float)(b2.chunk.wx * 8) + b2.x, (float)(b2.chunk.wy * 8) + b2.y, b2.z, inf);
            }
        }
    }

    private void renderOneLevel_Blood(IsoChunk chunk, int dwx, int dwy, int zza) {
        IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + dwx, chunk.wy + dwy);
        if (chunk2 == null) {
            return;
        }
        int chunksPerWidth = 8;
        int minX = chunk.wx * 8 - 1;
        int minY = chunk.wy * 8 - 1;
        int maxX = (chunk.wx + 1) * 8 + 1;
        int maxY = (chunk.wy + 1) * 8 + 1;
        this.renderOneLevel_Blood(chunk2, zza, minX, minY, maxX, maxY);
    }

    private void recalculateAnyGridStacks(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (!player.dirtyRecalcGridStack) {
            return;
        }
        player.dirtyRecalcGridStack = false;
        WeatherFxMask.setDiamondIterDone(playerIndex);
    }

    private boolean hasAnyDirtyChunkTextures(int playerIndex) {
        float zoom = Core.getInstance().getZoom(playerIndex);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
            for (int z = c.minLevel; z <= c.maxLevel; ++z) {
                if (!renderLevels.isOnScreen(z) || !renderLevels.isDirty(z, zoom)) continue;
                return true;
            }
        }
        return false;
    }

    private void calculateOccludingSquares(int playerIndex) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
            for (int z = c.minLevel; z <= c.maxLevel; ++z) {
                FBORenderCutaways.ChunkLevelData levelData;
                boolean bChanged;
                if (!renderLevels.isOnScreen(z) || !(bChanged = (levelData = c.getCutawayData().getDataForLevel(z)).calculateOccludingSquares(playerIndex, perPlayerData1.occludedGridX1, perPlayerData1.occludedGridY1, perPlayerData1.occludedGridX2, perPlayerData1.occludedGridY2, perPlayerData1.occludedGrid))) continue;
                FBORenderOcclusion.getInstance().invalidateOverlappedChunkLevels(playerIndex, c, z);
            }
        }
    }

    private int calculateRenderedSquaresCount(int playerIndex, IsoChunk chunk, int level) {
        int minLevel = chunk.getRenderLevels(playerIndex).getMinLevel(level);
        int maxLevel = chunk.getRenderLevels(playerIndex).getMaxLevel(level);
        int renderedSquaresCount = 0;
        for (int z = minLevel; z <= maxLevel; ++z) {
            FBORenderCutaways.ChunkLevelData chunkLevelData = chunk.getCutawayDataForLevel(z);
            IsoGridSquare[] squares = chunk.getSquaresForLevel(z);
            for (int i = 0; i < squares.length; ++i) {
                IsoGridSquare square = squares[i];
                if (!chunkLevelData.shouldRenderSquare(playerIndex, square) || FBORenderOcclusion.getInstance().isOccluded(square.x, square.y, z)) continue;
                if (DebugOptions.instance.cheapOcclusionCount.getValue()) {
                    return 1;
                }
                ++renderedSquaresCount;
            }
        }
        return renderedSquaresCount;
    }

    private void prepareChunksForUpdating(int playerIndex) {
        float zoom = Core.getInstance().getZoom(playerIndex);
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk c = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
            for (int z = c.minLevel; z <= c.maxLevel; ++z) {
                if (z != renderLevels.getMinLevel(z) || !renderLevels.isOnScreen(z) || !renderLevels.isDirty(z, zoom)) continue;
                this.prepareChunkForUpdating(playerIndex, c, z);
            }
        }
    }

    private void prepareChunkForUpdating(int playerIndex, IsoChunk chunk, int z) {
        if (chunk == null) {
            return;
        }
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(z)) {
            return;
        }
        int minLevel = renderLevels.getMinLevel(z);
        int maxLevel = renderLevels.getMaxLevel(z);
        for (int z2 = minLevel; z2 <= maxLevel; ++z2) {
            FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z2);
            for (int y = 0; y < 8; ++y) {
                for (int x = 0; x < 8; ++x) {
                    IsoGridSquare sq = chunk.getGridSquare(x, y, z2);
                    levelData.squareFlags[playerIndex][x + y * 8] = 0;
                    if (sq == null) continue;
                    sq.cacheLightInfo();
                    if (sq.getLightInfo(playerIndex) == null || !this.shouldRenderSquare(sq)) continue;
                    byte[] byArray = levelData.squareFlags[playerIndex];
                    int n = x + y * 8;
                    byArray[n] = (byte)(byArray[n] | 1);
                }
            }
        }
    }

    private boolean shouldRenderSquare(IsoGridSquare square) {
        if (square == null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (square.getLightInfo(playerIndex) == null || square.lighting[playerIndex] == null) {
            return false;
        }
        return FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, square);
    }

    private void renderTranslucentFloorObjects(int playerIndex, int z, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (!DebugOptions.instance.fboRenderChunk.renderTranslucentFloor.getValue()) {
            return;
        }
        this.renderTranslucentOnly = true;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.chunksWithTranslucentFloor.size(); ++i) {
            IsoChunk chunk = perPlayerData1.chunksWithTranslucentFloor.get(i);
            this.renderOneChunk_TranslucentFloor(chunk, playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
        }
    }

    private void renderOneChunk_TranslucentFloor(IsoChunk c, int playerIndex, int zza, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (c == null) {
            return;
        }
        if (c.lightingNeverDone[playerIndex]) {
            return;
        }
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);
        this.renderOneLevel_TranslucentFloor(playerIndex, c, zza);
        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_TranslucentFloor(int playerIndex, IsoChunk c, int level) {
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            return;
        }
        ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_TranslucentFloor(level);
        for (int i = 0; i < squares.size(); ++i) {
            IsoGridSquare square = squares.get(i);
            if (square.z != level || !square.IsOnScreen()) continue;
            this.renderTranslucentFloor(square);
        }
    }

    private void renderTranslucentObjects(int playerIndex, int z, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (!DebugOptions.instance.fboRenderChunk.renderTranslucentNonFloor.getValue()) {
            return;
        }
        this.renderTranslucentOnly = true;
        FBORenderTrees.current = FBORenderTrees.alloc();
        FBORenderTrees.current.init();
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.chunksWithTranslucentNonFloor.size(); ++i) {
            IsoChunk chunk = perPlayerData1.chunksWithTranslucentNonFloor.get(i);
            this.renderOneChunk_Translucent(chunk, playerIndex, z, floorRenderShader, wallRenderShader, currentTimeMillis);
        }
        SpriteRenderer.instance.drawGeneric(FBORenderTrees.current);
    }

    private void renderOneChunk_Translucent(IsoChunk c, int playerIndex, int zza, Shader floorRenderShader, Shader wallRenderShader, long currentTimeMillis) {
        if (c == null || !c.IsOnScreen(true)) {
            return;
        }
        if (c.lightingNeverDone[playerIndex]) {
            return;
        }
        IndieGL.enableDepthTest();
        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(false);
        this.renderOneLevel_Translucent(playerIndex, c, zza);
        IndieGL.glDepthMask(false);
        IndieGL.glDepthFunc(519);
    }

    private void renderOneLevel_Translucent(int playerIndex, IsoChunk c, int level) {
        FBORenderLevels renderLevels = c.getRenderLevels(playerIndex);
        if (!renderLevels.isOnScreen(level)) {
            return;
        }
        FBORenderCutaways.ChunkLevelData levelData = c.getCutawayDataForLevel(level);
        ArrayList<IsoGridSquare> squaresObjects = renderLevels.getCachedSquares_TranslucentNonFloor(level);
        ArrayList<IsoGridSquare> squaresItems = renderLevels.getCachedSquares_Items(level);
        if (squaresObjects.size() + squaresItems.size() == 0) {
            return;
        }
        PZArrayList<IsoGridSquare> sorted2 = this.tempSquares;
        sorted2.clear();
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("Sort");){
            PZArrayUtil.addAll(sorted2, squaresItems);
            for (int i = 0; i < squaresObjects.size(); ++i) {
                IsoGridSquare square = squaresObjects.get(i);
                if (sorted2.contains(square)) continue;
                sorted2.add(square);
            }
        }
        this.timSort.doSort(sorted2.getElements(), (o1, o2) -> {
            int worldRight = IsoWorld.instance.getMetaGrid().getMaxX() * 256;
            int i1 = o1.x + o1.y * worldRight;
            int i2 = o2.x + o2.y * worldRight;
            return i1 - i2;
        }, 0, sorted2.size());
        for (int i = 0; i < sorted2.size(); ++i) {
            IsoGridSquare square = sorted2.get(i);
            if (square.z != level || !levelData.shouldRenderSquare(playerIndex, square) || !square.IsOnScreen()) continue;
            this.renderTranslucent(square, false);
            if (DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue() && !square.getWorldObjects().isEmpty()) {
                if (square.chunk == c) {
                    this.renderWorldInventoryObjects(square, square, false);
                } else {
                    IsoGridSquare renderSquare = c.getGridSquare(0, 0, square.z);
                    this.renderWorldInventoryObjects(square, renderSquare, false);
                }
            }
            this.renderTranslucent(square, true);
        }
    }

    private void renderCorpseShadows(int playerIndex) {
        if (!DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
            return;
        }
        if (!Core.getInstance().getOptionCorpseShadows()) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
            for (int z = chunk.minLevel; z <= chunk.maxLevel; ++z) {
                if (!renderLevels.isOnScreen(z) || z != renderLevels.getMinLevel(z)) continue;
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Corpses(z);
                for (int j = 0; j < squares.size(); ++j) {
                    IsoGridSquare square = squares.get(j);
                    for (int k = 0; k < square.getStaticMovingObjects().size(); ++k) {
                        IsoMovingObject isoMovingObject = square.getStaticMovingObjects().get(k);
                        if (!(isoMovingObject instanceof IsoDeadBody)) continue;
                        IsoDeadBody deadBody = (IsoDeadBody)isoMovingObject;
                        if (square.HasStairs()) continue;
                        deadBody.renderShadow();
                    }
                }
            }
        }
    }

    private void checkMannequinRenderDirection(int playerIndex) {
        for (int i = 0; i < this.mannequinList.size(); ++i) {
            IsoMannequin mannequin = this.mannequinList.get(i);
            if (mannequin.getObjectIndex() == -1) {
                this.mannequinList.remove(i--);
                continue;
            }
            mannequin.checkRenderDirection(playerIndex);
        }
    }

    private void renderMannequinShadows(int playerIndex) {
        for (int i = 0; i < this.mannequinList.size(); ++i) {
            IsoMannequin mannequin = this.mannequinList.get(i);
            if (mannequin.getObjectIndex() == -1) {
                this.mannequinList.remove(i--);
                continue;
            }
            if (!this.shouldRenderSquare(mannequin.getSquare())) continue;
            mannequin.renderShadow(mannequin.getX() + 0.5f, mannequin.getY() + 0.5f, mannequin.getZ());
            if (!mannequin.shouldRenderEachFrame()) continue;
            ColorInfo lightInfo = mannequin.getSquare().getLightInfo(playerIndex);
            mannequin.render(mannequin.getX(), mannequin.getY(), mannequin.getZ(), lightInfo, true, false, null);
        }
    }

    private void renderOpaqueObjectsEvent(int playerIndex) {
        int buildZ;
        int buildY;
        int buildX;
        if (JoypadManager.instance.getFromPlayer(playerIndex) == null) {
            if (UIManager.getPickedTile() == null) {
                return;
            }
            buildX = PZMath.fastfloor(UIManager.getPickedTile().x);
            buildY = PZMath.fastfloor(UIManager.getPickedTile().y);
            buildZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        } else {
            buildX = PZMath.fastfloor(IsoCamera.frameState.camCharacterX);
            buildY = PZMath.fastfloor(IsoCamera.frameState.camCharacterY);
            buildZ = PZMath.fastfloor(IsoCamera.frameState.camCharacterZ);
        }
        if (IsoWorld.instance.isValidSquare(buildX, buildY, buildZ)) {
            IsoGridSquare square = this.cell.getGridSquare(buildX, buildY, buildZ);
            LuaEventManager.triggerEvent("RenderOpaqueObjectsInWorld", playerIndex, buildX, buildY, buildZ, square);
        }
    }

    private void renderPlayers(int playerIndex) {
        if (GameClient.client) {
            for (IsoPlayer player : GameClient.IDToPlayerMap.values()) {
                this.renderPlayer(playerIndex, player);
            }
            return;
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null) continue;
            this.renderPlayer(playerIndex, player);
        }
    }

    private void renderPlayer(int playerIndex, IsoPlayer player) {
        if (!this.cell.getObjectList().contains(player)) {
            return;
        }
        if (player.getCurrentSquare() == null) {
            return;
        }
        if (!player.isOnScreen()) {
            return;
        }
        if (player.getCurrentSquare().getLightInfo(playerIndex) == null) {
            return;
        }
        if (!FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, player.getCurrentSquare())) {
            return;
        }
        if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
            player.renderShadow(player.getX(), player.getY(), player.getZ());
        }
        player.render(player.getX(), player.getY(), player.getZ(), player.getCurrentSquare().getLightInfo(IsoPlayer.getPlayerIndex()), true, false, null);
        this.debugChunkStateRenderPlayer(player);
    }

    private void renderMovingObjects() {
        this.renderTranslucentOnly = true;
        ArrayList<IsoMovingObject> objList = IsoWorld.instance.getCell().getObjectList();
        for (int i = 0; i < objList.size(); ++i) {
            IsoMovingObject isoMovingObject = objList.get(i);
            this.renderMovingObject(isoMovingObject);
        }
        this.renderTranslucentOnly = false;
        SpriteRenderer.instance.renderQueued();
    }

    private void renderMovingObject(IsoMovingObject isoMovingObject) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (isoMovingObject.getClass() == IsoPlayer.class) {
            return;
        }
        if (isoMovingObject.getCurrentSquare() == null) {
            return;
        }
        if (!isoMovingObject.isOnScreen()) {
            return;
        }
        if (isoMovingObject.getCurrentSquare().getLightInfo(playerIndex) == null) {
            return;
        }
        if (!this.shouldRenderSquare(isoMovingObject.getCurrentSquare())) {
            return;
        }
        if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
            IsoGameCharacter chr = Type.tryCastTo(isoMovingObject, IsoGameCharacter.class);
            if (chr != null && chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs() && chr.isRagdoll()) {
                boolean bl = true;
            } else if (chr != null) {
                chr.renderShadow(isoMovingObject.getX(), isoMovingObject.getY(), isoMovingObject.getZ());
            }
            if (isoMovingObject instanceof BaseVehicle) {
                BaseVehicle vehicle = (BaseVehicle)isoMovingObject;
                vehicle.renderShadow();
            }
        }
        isoMovingObject.render(isoMovingObject.getX(), isoMovingObject.getY(), isoMovingObject.getZ(), isoMovingObject.getCurrentSquare().getLightInfo(playerIndex), true, false, null);
    }

    private void renderWater(int playerIndex) {
        if (!(DebugOptions.instance.weather.waterPuddles.getValue() && DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterBody.getValue())) {
            return;
        }
        if (IsoCamera.frameState.camCharacterZ < 0.0f) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        int maxZ = chunkMap.maxHeight;
        for (int z = 0; z <= maxZ; ++z) {
            int j;
            int i;
            this.waterSquares.clear();
            this.waterAttachSquares.clear();
            this.fishSplashSquares.clear();
            for (i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
                IsoGridSquare square;
                FBORenderLevels renderLevels;
                IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                if (z < chunk.minLevel || z > chunk.maxLevel || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(z)) continue;
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_Water(z);
                int n = squares.size();
                for (j = 0; j < n; ++j) {
                    IsoObject floor;
                    square = squares.get(j);
                    if (!square.IsOnScreen() || (floor = square.getFloor()) != null && floor.getRenderInfo((int)playerIndex).layer == ObjectRenderLayer.TranslucentFloor) continue;
                    if (IsoWater.getInstance().getShaderEnable() && square.getWater() != null && square.getWater().isValid()) {
                        this.waterSquares.add(square);
                    }
                    if (!square.shouldRenderFishSplash(playerIndex)) continue;
                    this.fishSplashSquares.add(square);
                }
                squares = renderLevels.getCachedSquares_WaterAttach(z);
                n = squares.size();
                for (j = 0; j < n; ++j) {
                    square = squares.get(j);
                    if (!square.IsOnScreen() || !IsoWater.getInstance().getShaderEnable() || square.getWater() == null || !square.getWater().isValid()) continue;
                    this.waterAttachSquares.add(square);
                }
            }
            if (!this.waterSquares.isEmpty()) {
                IsoWater.getInstance().render(this.waterSquares, z);
            }
            for (i = 0; i < this.waterAttachSquares.size(); ++i) {
                IsoGridSquare square = this.waterAttachSquares.get(i);
                IsoObject[] objects = square.getObjects().getElements();
                int numObjects = square.getObjects().size();
                for (j = 0; j < numObjects; ++j) {
                    IsoObject object = objects[j];
                    if (object == null || object.getRenderInfo((int)playerIndex).layer != ObjectRenderLayer.None || object.getAttachedAnimSprite() == null || object.getAttachedAnimSprite().isEmpty()) continue;
                    this.renderTranslucentOnly = true;
                    object.renderAttachedAndOverlaySprites(object.dir, square.x, square.y, square.z, square.getLightInfo(playerIndex), true, false, null, null);
                    this.renderTranslucentOnly = false;
                }
            }
            if (this.fishSplashSquares.isEmpty()) continue;
            this.renderFishSplashes(playerIndex, this.fishSplashSquares);
        }
    }

    private void renderWaterShore(int playerIndex) {
        if (!(DebugOptions.instance.weather.waterPuddles.getValue() && DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterShore.getValue())) {
            return;
        }
        if (!IsoWater.getInstance().getShaderEnable()) {
            return;
        }
        if (IsoCamera.frameState.camCharacterZ < 0.0f) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        int maxZ = chunkMap.maxHeight;
        for (int z = 0; z <= maxZ; ++z) {
            int j;
            int i;
            this.waterSquares.clear();
            this.waterAttachSquares.clear();
            for (i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
                FBORenderLevels renderLevels;
                IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                if (z < chunk.minLevel || z > chunk.maxLevel || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(z)) continue;
                ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_WaterShore(z);
                int n = squares.size();
                for (j = 0; j < n; ++j) {
                    IsoGridSquare square = squares.get(j);
                    if (!square.IsOnScreen() || square.getWater() == null || !square.getWater().isbShore()) continue;
                    this.waterSquares.add(square);
                    this.waterAttachSquares.add(square);
                }
            }
            if (!this.waterSquares.isEmpty()) {
                IsoWater.getInstance().renderShore(this.waterSquares, z);
            }
            for (i = 0; i < this.waterAttachSquares.size(); ++i) {
                IsoGridSquare square = this.waterAttachSquares.get(i);
                IsoObject[] objects = square.getObjects().getElements();
                int numObjects = square.getObjects().size();
                for (j = 0; j < numObjects; ++j) {
                    IsoObject object = objects[j];
                    if (object == null || object.getRenderInfo((int)playerIndex).layer != ObjectRenderLayer.None || object.getAttachedAnimSprite() == null || object.getAttachedAnimSprite().isEmpty()) continue;
                    this.renderTranslucentOnly = true;
                    object.renderAttachedAndOverlaySprites(object.dir, square.x, square.y, square.z, square.getLightInfo(playerIndex), true, false, null, null);
                    this.renderTranslucentOnly = false;
                }
            }
        }
    }

    private void renderPuddles(int playerIndex) {
        if (!IsoPuddles.getInstance().shouldRenderPuddles()) {
            return;
        }
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        int maxZ = chunkMap.maxHeight;
        if (Core.getInstance().getPerfPuddles() > 0) {
            maxZ = 0;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int z = 0; z <= maxZ; ++z) {
            this.waterSquares.clear();
            for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
                ArrayList<IsoGridSquare> squares;
                FBORenderLevels renderLevels;
                IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                if (z < chunk.minLevel || z > chunk.maxLevel || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(z) || (squares = renderLevels.getCachedSquares_Puddles(z)).isEmpty()) continue;
                FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
                for (int j = 0; j < squares.size(); ++j) {
                    IsoPuddlesGeometry puddlesGeometry;
                    IsoObject floor;
                    IsoGridSquare square = squares.get(j);
                    if (square.getZ() != z || !levelData.shouldRenderSquare(playerIndex, square) || !square.IsOnScreen() || (floor = square.getFloor()) == null || PerformanceSettings.puddlesQuality < 2 && floor.getRenderInfo((int)playerIndex).layer == ObjectRenderLayer.TranslucentFloor || (puddlesGeometry = square.getPuddles()) == null || !puddlesGeometry.shouldRender()) continue;
                    this.waterSquares.add(square);
                }
            }
            IsoPuddles.getInstance().render(this.waterSquares, z);
        }
    }

    private void renderPuddleDebug(int playerIndex) {
        if (!IsoPuddles.getInstance().shouldRenderPuddles()) {
            return;
        }
        Texture tex = Texture.getSharedTexture("media/textures/Item_Waterdrop_Grey.png");
        if (tex == null) {
            return;
        }
        IndieGL.disableDepthTest();
        IndieGL.StartShader(0);
        boolean maxZ = false;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int z = 0; z <= 0; ++z) {
            for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
                IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                for (int x = 0; x < 8; ++x) {
                    for (int y = 0; y < 8; ++y) {
                        float level;
                        IsoGridSquare square = chunk.getGridSquare(x, y, z);
                        if (square == null || (level = square.getPuddlesInGround()) <= 0.09f) continue;
                        int sqx = square.getX();
                        int sqy = square.getY();
                        float sx = IsoUtils.XToScreen(sqx, sqy, z, 0) - IsoCamera.frameState.offX;
                        float sy = IsoUtils.YToScreen(sqx, sqy, z, 0) - IsoCamera.frameState.offY;
                        float opacity = PZMath.clamp(0.1f + level, 0.2f, 1.0f);
                        SpriteRenderer.instance.render(tex, sx -= (float)tex.getWidth() / 2.0f * (float)Core.tileScale, sy -= (float)tex.getHeight() / 2.0f * (float)Core.tileScale, tex.getWidth() * Core.tileScale, tex.getHeight() * Core.tileScale, opacity, opacity, opacity, opacity, null);
                    }
                }
            }
        }
    }

    private void renderPuddlesTranslucentFloorsOnly(int playerIndex, int z) {
        if (!IsoPuddles.getInstance().shouldRenderPuddles()) {
            return;
        }
        if (Core.getInstance().getPerfPuddles() > 0 && z != 0) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        this.waterSquares.clear();
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            FBORenderLevels renderLevels;
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            if (z < chunk.minLevel || z > chunk.maxLevel || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(z) || renderLevels.getCachedSquares_Puddles(z).isEmpty()) continue;
            FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
            ArrayList<IsoGridSquare> squares = renderLevels.getCachedSquares_TranslucentFloor(z);
            for (int j = 0; j < squares.size(); ++j) {
                IsoPuddlesGeometry puddlesGeometry;
                IsoObject floor;
                IsoGridSquare square = squares.get(j);
                if (!levelData.shouldRenderSquare(playerIndex, square) || !square.IsOnScreen() || (floor = square.getFloor()) == null || floor.getRenderInfo((int)playerIndex).layer != ObjectRenderLayer.TranslucentFloor || (puddlesGeometry = square.getPuddles()) == null || !puddlesGeometry.shouldRender()) continue;
                this.waterSquares.add(square);
            }
        }
        IsoPuddles.getInstance().render(this.waterSquares, z);
    }

    private void renderPuddlesToChunkTexture(int playerIndex, int z, IsoChunk chunk) {
        if (!IsoPuddles.getInstance().shouldRenderPuddles()) {
            return;
        }
        if (z < chunk.minLevel || z > chunk.maxLevel) {
            return;
        }
        if (!chunk.getRenderLevels(playerIndex).isOnScreen(z)) {
            return;
        }
        this.waterSquares.clear();
        FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
        IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];
        for (int j = 0; j < squares.length; ++j) {
            IsoPuddlesGeometry puddlesGeometry;
            IsoObject floor;
            IsoGridSquare square = squares[j];
            if (!levelData.shouldRenderSquare(playerIndex, square) || (floor = square.getFloor()) == null || floor.getRenderInfo((int)playerIndex).layer == ObjectRenderLayer.TranslucentFloor || (puddlesGeometry = square.getPuddles()) == null || !puddlesGeometry.shouldRender()) continue;
            this.waterSquares.add(square);
        }
        IsoPuddles.getInstance().renderToChunkTexture(this.waterSquares, z);
    }

    private void renderRainSplashes(int playerIndex, int z) {
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        IsoChunkMap chunkMap = this.cell.chunkMap[playerIndex];
        this.waterSquares.clear();
        for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
            IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
            if (z < chunk.minLevel || z > chunk.maxLevel || !chunk.getRenderLevels(playerIndex).isOnScreen(z)) continue;
            IsoChunkLevel levelData = chunk.getLevelData(z);
            levelData.updateRainSplashes();
            levelData.renderRainSplashes(playerIndex);
        }
    }

    private void renderFog(int playerIndex) {
        if (IsoCamera.frameState.camCharacterZ < 0.0f) {
            return;
        }
        if (PerformanceSettings.fogQuality == 2) {
            return;
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        ImprovedFog.getDrawer().startFrame();
        boolean bFirst = true;
        for (int z = 0; z <= 1; ++z) {
            if (!ImprovedFog.startRender(playerIndex, z)) continue;
            if (bFirst) {
                bFirst = false;
                ImprovedFog.startFrame(ImprovedFog.getDrawer());
            }
            for (int i = 0; i < perPlayerData1.onScreenChunks.size(); ++i) {
                FBORenderLevels renderLevels;
                IsoChunk chunk = perPlayerData1.onScreenChunks.get(i);
                if (z < chunk.minLevel || z > chunk.maxLevel || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(z)) continue;
                FBORenderCutaways.ChunkLevelData levelData = chunk.getCutawayDataForLevel(z);
                IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(z)];
                block7: for (int j = 0; j < squares.length; ++j) {
                    IsoGridSquare square = squares[j];
                    if (!levelData.shouldRenderSquare(playerIndex, square)) continue;
                    IsoObject[] objects = square.getObjects().getElements();
                    int numObjects = square.getObjects().size();
                    for (int k = 0; k < numObjects; ++k) {
                        IsoObject object = objects[k];
                        ObjectRenderInfo renderInfo = object.getRenderInfo(playerIndex);
                        if (renderInfo.layer != ObjectRenderLayer.MinusFloor) continue;
                        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("ImprovedFog");){
                            ImprovedFog.renderRowsBehind(square);
                            continue block7;
                        }
                    }
                }
            }
            ImprovedFog.endRender();
        }
        ImprovedFog.getDrawer().endFrame();
    }

    public void handleDelayedLoading(IsoObject object) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        object.getChunk().getRenderLevels(playerIndex).handleDelayedLoading(object);
        if (this.delayedLoadingTimerMs == 0L) {
            this.delayedLoadingTimerMs = System.currentTimeMillis() + 250L;
        }
    }

    private ColorInfo sanitizeLightInfo(int playerIndex, IsoGridSquare square) {
        ColorInfo lightInfo = square.getLightInfo(playerIndex);
        if (lightInfo == null) {
            lightInfo = this.defColorInfo;
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            this.defColorInfo.set(1.0f, 1.0f, 1.0f, lightInfo.a);
            lightInfo = this.defColorInfo;
        }
        return lightInfo;
    }

    private void debugChunkStateRenderPlayer(IsoPlayer player) {
        if (GameWindow.states.current != DebugChunkState.instance) {
            return;
        }
        DebugChunkState.instance.drawObjectAtCursor();
        if (!DebugChunkState.instance.getBoolean("ObjectAtCursor")) {
            return;
        }
        if (!"player".equals(DebugChunkState.instance.fromLua1("getObjectAtCursor", "id"))) {
            return;
        }
        float gridXf = DebugChunkState.instance.gridXf;
        float gridYf = DebugChunkState.instance.gridYf;
        int mZ = DebugChunkState.instance.z;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(gridXf, gridYf, (double)mZ);
        if (square == null) {
            return;
        }
        float x = player.getX();
        float y = player.getY();
        float z = player.getZ();
        IsoGridSquare psquare = player.getCurrentSquare();
        float apparentZ = square.getApparentZ(gridXf % 1.0f, gridYf % 1.0f);
        player.setX(gridXf);
        player.setY(gridYf);
        player.setZ(apparentZ);
        player.setCurrent(square);
        this.renderDebugChunkState = true;
        player.render(gridXf, gridYf, apparentZ, new ColorInfo(), true, false, null);
        this.renderDebugChunkState = false;
        player.setX(x);
        player.setY(y);
        player.setZ(z);
        player.setCurrent(psquare);
    }

    private boolean checkDebugKeys(int playerIndex, int currentZ) {
        boolean bForceCutawayUpdate = false;
        if (Core.debug && GameKeyboard.isKeyPressed(28)) {
            IsoChunkMap chunkMap = this.cell.getChunkMap(playerIndex);
            for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
                for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                    FBORenderLevels renderLevels;
                    IsoChunk chunk = chunkMap.getChunk(x, y);
                    if (chunk == null || currentZ < chunk.minLevel || currentZ > chunk.maxLevel || !chunk.IsOnScreen(true) || !(renderLevels = chunk.getRenderLevels(playerIndex)).isOnScreen(currentZ)) continue;
                    renderLevels.invalidateLevel(currentZ, 64L);
                    this.prepareChunkForUpdating(playerIndex, chunk, currentZ);
                    IsoGridSquare[] squares = chunk.squares[chunk.squaresIndexOfLevel(currentZ)];
                    for (int i = 0; i < squares.length; ++i) {
                        if (squares[i] == null) continue;
                        squares[i].setPlayerCutawayFlag(playerIndex, 0, 0L);
                    }
                    chunk.getCutawayData().invalidateOccludedSquaresMaskForSeenRooms(playerIndex, currentZ);
                }
            }
            bForceCutawayUpdate = true;
        }
        return bForceCutawayUpdate;
    }

    public void renderSeamFix1_Floor(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix1.getValue()) {
            IsoGridSquare e;
            IsoGridSquare s;
            IsoGridSquare square = object.getSquare();
            IsoSprite sprite = object.getSprite();
            if (PZMath.coordmodulo(square.y, 8) == 7 && (s = square.getAdjacentSquare(IsoDirections.S)) != null && s.getFloor() != null) {
                sprite.render(object, x, y, z, object.dir, object.offsetX + 5.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 5.0f, stCol, !object.isBlink(), texdModifier);
            }
            if (PZMath.coordmodulo(square.x, 8) == 7 && (e = square.getAdjacentSquare(IsoDirections.E)) != null && e.getFloor() != null) {
                sprite.render(object, x, y, z, object.dir, object.offsetX - 5.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 5.0f, stCol, !object.isBlink(), texdModifier);
            }
        }
    }

    public void renderSeamFix2_Floor(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        IsoGridSquare squareE;
        boolean bShoreE;
        IsoGridSquare squareW;
        boolean bShoreW;
        IsoGridSquare squareN;
        boolean bShoreN;
        IsoGridSquare e;
        IsoGridSquare s;
        boolean bShoreS;
        if (this.renderTranslucentOnly) {
            return;
        }
        if (!PerformanceSettings.fboRenderChunk || !DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) {
            return;
        }
        IsoGridSquare square = object.getSquare();
        IsoSprite sprite = object.getSprite();
        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
        boolean bl = bShoreS = squareS != null && squareS.getWater() != null && squareS.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
        if (!(PZMath.coordmodulo(square.y, 8) != 7 && !bShoreS || (s = square.getAdjacentSquare(IsoDirections.S)) == null || s.getFloor() == null || !bShoreS && s.has(IsoFlagType.water) && PerformanceSettings.waterQuality != 2)) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouth;
            if (sprite.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouthOneThird;
            }
            if (sprite.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouthTwoThirds;
            }
            object.sx = 0.0f;
            if (bShoreS) {
                object.renderDepthAdjust = -0.001f;
            }
            sprite.render(object, x, y, z, object.dir, object.offsetX + 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            object.renderDepthAdjust = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        if (!(PZMath.coordmodulo(square.x, 8) != 7 || (e = square.getAdjacentSquare(IsoDirections.E)) == null || e.getFloor() == null || e.has(IsoFlagType.water) && PerformanceSettings.waterQuality != 2)) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
            if (sprite.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEastOneThird;
            }
            if (sprite.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEastTwoThirds;
            }
            object.sx = 0.0f;
            sprite.render(object, x, y, z, object.dir, object.offsetX - 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        boolean bl2 = bShoreN = (squareN = square.getAdjacentSquare(IsoDirections.N)) != null && squareN.getWater() != null && squareN.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
        if (bShoreN) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorSouth;
            object.sx = 0.0f;
            object.renderSquareOverride2 = squareN;
            object.renderDepthAdjust = -0.001f;
            sprite.render(object, x, y - 1.0f, z, object.dir, object.offsetX, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            object.renderSquareOverride2 = null;
            object.renderDepthAdjust = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        boolean bl3 = bShoreW = (squareW = square.getAdjacentSquare(IsoDirections.W)) != null && squareW.getWater() != null && squareW.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
        if (bShoreW) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
            object.sx = 0.0f;
            object.renderSquareOverride2 = squareW;
            object.renderDepthAdjust = -0.001f;
            sprite.render(object, x - 1.0f, y, z, object.dir, object.offsetX - 2.0f, object.offsetY - 1.0f + object.getRenderYOffset() * (float)Core.tileScale, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            object.renderSquareOverride2 = null;
            object.renderDepthAdjust = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        boolean bl4 = bShoreE = (squareE = square.getAdjacentSquare(IsoDirections.E)) != null && squareE.getWater() != null && squareE.getWater().isbShore() && IsoWater.getInstance().getShaderEnable();
        if (bShoreE) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.FloorEast;
            object.sx = 0.0f;
            sprite.render(object, x, y, z, object.dir, object.offsetX - 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
    }

    public void renderSeamFix1_Wall(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        if (PerformanceSettings.fboRenderChunk && DebugOptions.instance.fboRenderChunk.seamFix1.getValue()) {
            IsoGridSquare e;
            IsoGridSquare s;
            IsoGridSquare square = object.getSquare();
            IsoSprite sprite = object.getSprite();
            if (sprite.getProperties().has(IsoFlagType.WallW) && PZMath.coordmodulo(square.y, 8) == 7 && (s = square.getAdjacentSquare(IsoDirections.S)) != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
                sprite.renderWallSliceW(object, x, y, z, object.dir, object.offsetX, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale, stCol, !object.isBlink(), texdModifier);
            }
            if (sprite.getProperties().has(IsoFlagType.WallN) && PZMath.coordmodulo(square.x, 8) == 7 && (e = square.getAdjacentSquare(IsoDirections.E)) != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
                sprite.renderWallSliceN(object, x, y, z, object.dir, object.offsetX, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale, stCol, !object.isBlink(), texdModifier);
            }
        }
    }

    public void renderSeamFix2_Wall(IsoObject object, float x, float y, float z, ColorInfo stCol, Consumer<TextureDraw> texdModifier) {
        IsoGridSquare e;
        IsoGridSquare s;
        if (!PerformanceSettings.fboRenderChunk || !DebugOptions.instance.fboRenderChunk.seamFix2.getValue()) {
            return;
        }
        IsoGridSquare square = object.getSquare();
        IsoSprite sprite = object.getSprite();
        if (sprite.getProperties().has(IsoFlagType.HoppableN) || sprite.getProperties().has(IsoFlagType.HoppableW)) {
            return;
        }
        if (sprite.tileSheetIndex >= 80 && sprite.tileSheetIndex <= 82 && sprite.tilesetName != null && sprite.tilesetName.equals("carpentry_02")) {
            return;
        }
        if (sprite.tileSheetIndex >= 48 && sprite.tileSheetIndex <= 55 && sprite.tilesetName != null && sprite.tilesetName.equals("walls_logs")) {
            return;
        }
        if (sprite.tilesetName != null && sprite.tilesetName.equals("walls_logs")) {
            return;
        }
        if (sprite.getProperties().has(IsoFlagType.WallNW) && texdModifier == WallShaperW.instance && PZMath.coordmodulo(square.y, 8) == 7 && (s = square.getAdjacentSquare(IsoDirections.S)) != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.WallSouth;
            object.sx = 0.0f;
            sprite.render(object, x, y, z, IsoDirections.NW, object.offsetX + 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        if (sprite.getProperties().has(IsoFlagType.WallNW) && texdModifier == WallShaperN.instance && PZMath.coordmodulo(square.x, 8) == 7 && (e = square.getAdjacentSquare(IsoDirections.E)) != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.WallEast;
            object.sx = 0.0f;
            sprite.render(object, x, y, z, IsoDirections.NW, object.offsetX - 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        if ((sprite.getProperties().has(IsoFlagType.WallW) || sprite.getProperties().has(IsoFlagType.WindowW)) && PZMath.coordmodulo(square.y, 8) == 7 && (s = square.getAdjacentSquare(IsoDirections.S)) != null && ((s.getWallType() & 4) != 0 || s.getWindowFrame(false) != null || s.has(IsoFlagType.DoorWallW))) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.WallSouth;
            object.sx = 0.0f;
            sprite.render(object, x, y, z, IsoDirections.W, object.offsetX + 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
        if ((sprite.getProperties().has(IsoFlagType.WallN) || sprite.getProperties().has(IsoFlagType.WindowN)) && PZMath.coordmodulo(square.x, 8) == 7 && (e = square.getAdjacentSquare(IsoDirections.E)) != null && ((e.getWallType() & 1) != 0 || e.getWindowFrame(true) != null || e.has(IsoFlagType.DoorWallN))) {
            IsoSprite.seamFix2 = TileSeamManager.Tiles.WallEast;
            object.sx = 0.0f;
            sprite.render(object, x, y, z, IsoDirections.N, object.offsetX - 6.0f, object.offsetY + object.getRenderYOffset() * (float)Core.tileScale - 3.0f, stCol, !object.isBlink(), texdModifier);
            object.sx = 0.0f;
            IsoSprite.seamFix2 = null;
        }
    }

    private void checkSeenRooms(IsoPlayer player, int level) {
        if (GameClient.client) {
            return;
        }
        IsoBuilding building = player.getBuilding();
        if (building == null) {
            return;
        }
        for (IsoRoom room : building.rooms) {
            if (room.def.explored || PZMath.abs(room.def.level - level) > 1) continue;
            room.def.explored = true;
            IsoWorld.instance.getCell().roomSpotted(room);
        }
    }

    private boolean shouldHideFascia(int playerIndex, IsoObject object) {
        IsoGridSquare square = object.getFasciaAttachedSquare();
        if (square == null) {
            return false;
        }
        return !FBORenderCutaways.getInstance().shouldRenderBuildingSquare(playerIndex, square);
    }

    private boolean checkBlackedOutBuildings(int playerIndex) {
        BuildingDef buildingDef;
        int i;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        float playerX = IsoCamera.frameState.camCharacterX;
        float playerY = IsoCamera.frameState.camCharacterY;
        Vector2f closestPoint = BaseVehicle.allocVector2f();
        ArrayList<BuildingDef> collapsedBuildings = FBORenderCutaways.getInstance().getCollapsedBuildings();
        boolean bChanged = false;
        for (i = 0; i < collapsedBuildings.size(); ++i) {
            buildingDef = collapsedBuildings.get(i);
            float distSq = buildingDef.getClosestPoint(playerX, playerY, closestPoint);
            int index = perPlayerData1.blackedOutBuildings.indexOf(buildingDef);
            if (index == -1) {
                if (!(distSq > 100.0f)) continue;
                perPlayerData1.blackedOutBuildings.add(buildingDef);
                buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
                bChanged = true;
                continue;
            }
            if (!(distSq <= 100.0f)) continue;
            perPlayerData1.blackedOutBuildings.remove(index);
            buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
            bChanged = true;
        }
        BaseVehicle.releaseVector2f(closestPoint);
        for (i = 0; i < perPlayerData1.blackedOutBuildings.size(); ++i) {
            buildingDef = perPlayerData1.blackedOutBuildings.get(i);
            int index = collapsedBuildings.indexOf(buildingDef);
            if (index != -1) continue;
            perPlayerData1.blackedOutBuildings.remove(i--);
            buildingDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
            bChanged = true;
        }
        return bChanged;
    }

    private void checkBlackedOutRooms(int playerIndex) {
        int i;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        ArrayList<LightingJNI.VisibleRoom> visibleRooms = LightingJNI.getVisibleRooms(playerIndex);
        if (visibleRooms == null) {
            return;
        }
        visibleRooms.forEach(visibleRoom -> {
            RoomDef roomDef;
            if (perPlayerData1.visibleRooms.contains(visibleRoom)) {
                return;
            }
            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(visibleRoom.cellX, visibleRoom.cellY);
            RoomDef roomDef2 = roomDef = metaCell == null ? null : metaCell.roomByMetaId.get(visibleRoom.metaId);
            if (roomDef != null) {
                roomDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
            }
            if (this.shouldDarkenIndividualRooms()) {
                for (int i = perPlayerData1.fadingRooms.size() - 1; i >= 0; --i) {
                    FadingRoom fadingRoom = perPlayerData1.fadingRooms.get(i);
                    if (!fadingRoom.equals(visibleRoom.cellX, visibleRoom.cellY, visibleRoom.metaId)) continue;
                    fadingRoom.release();
                    perPlayerData1.fadingRooms.remove(i);
                }
            }
        });
        perPlayerData1.visibleRooms.forEach(visibleRoom -> {
            IsoMetaCell metaCell;
            RoomDef roomDef;
            if (visibleRooms.contains(visibleRoom)) {
                return;
            }
            if (this.shouldDarkenIndividualRooms()) {
                FadingRoom fadingRoom = FadingRoom.alloc().set(visibleRoom.cellX, visibleRoom.cellY, visibleRoom.metaId);
                fadingRoom.startTimeMs = System.currentTimeMillis();
                fadingRoom.blackness = 0.0f;
                perPlayerData1.fadingRooms.add(fadingRoom);
            }
            RoomDef roomDef2 = roomDef = (metaCell = IsoWorld.instance.getMetaGrid().getCellData(visibleRoom.cellX, visibleRoom.cellY)) == null ? null : metaCell.roomByMetaId.get(visibleRoom.metaId);
            if (roomDef != null) {
                roomDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
            }
        });
        LightingJNI.VisibleRoom.releaseAll(perPlayerData1.visibleRooms);
        perPlayerData1.visibleRooms.clear();
        for (i = 0; i < visibleRooms.size(); ++i) {
            LightingJNI.VisibleRoom visibleRoom1 = visibleRooms.get(i);
            LightingJNI.VisibleRoom visibleRoom2 = LightingJNI.VisibleRoom.alloc().set(visibleRoom1);
            perPlayerData1.visibleRooms.add(visibleRoom2);
        }
        if (!this.shouldDarkenIndividualRooms()) {
            return;
        }
        for (i = perPlayerData1.fadingRooms.size() - 1; i >= 0; --i) {
            RoomDef roomDef;
            FadingRoom fadingRoom = perPlayerData1.fadingRooms.get(i);
            if (fadingRoom.startTimeMs + blackedOutRoomFadeDurationMs <= System.currentTimeMillis()) {
                fadingRoom.release();
                perPlayerData1.fadingRooms.remove(i);
                continue;
            }
            float ratio = (float)(System.currentTimeMillis() - fadingRoom.startTimeMs) / (float)blackedOutRoomFadeDurationMs;
            float fade = (float)((int)PZMath.ceil(ratio * 100.0f) / 10) * 0.1f;
            if ((fade *= blackedOutRoomFadeBlackness) == fadingRoom.blackness) continue;
            fadingRoom.blackness = fade;
            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(fadingRoom.cellX, fadingRoom.cellY);
            RoomDef roomDef2 = roomDef = metaCell == null ? null : metaCell.roomByMetaId.get(fadingRoom.metaId);
            if (roomDef == null) continue;
            roomDef.setInvalidateCacheForAllChunks(playerIndex, 32L);
        }
    }

    public boolean shouldDarkenIndividualRooms() {
        return blackedOutRoomFadeBlackness > 0.0f;
    }

    public boolean isBlackedOutBuildingSquare(IsoGridSquare square) {
        BuildingDef buildingDef;
        if (!PerformanceSettings.fboRenderChunk) {
            return false;
        }
        if (!FBORenderCutaways.getInstance().isAnyBuildingCollapsed()) {
            return false;
        }
        if (square == null) {
            return false;
        }
        BuildingDef buildingDef2 = buildingDef = square.getBuilding() == null ? null : square.getBuilding().getDef();
        if (buildingDef == null) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.shouldDarkenIndividualRooms()) {
            long metaID;
            int cellY;
            IsoRoom room = square.getRoom();
            if (room == null) {
                return false;
            }
            int cellX = buildingDef.getCellX();
            if (!LightingJNI.isRoomVisible(playerIndex, cellX, cellY = buildingDef.getCellY(), metaID = room.getRoomDef().metaId)) {
                return true;
            }
        }
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        return perPlayerData1.blackedOutBuildings.contains(buildingDef);
    }

    public float getBlackedOutRoomFadeRatio(IsoGridSquare square) {
        BuildingDef buildingDef;
        if (!this.shouldDarkenIndividualRooms()) {
            return 1.0f;
        }
        if (square == null) {
            return blackedOutRoomFadeBlackness;
        }
        BuildingDef buildingDef2 = buildingDef = square.getBuilding() == null ? null : square.getBuilding().getDef();
        if (buildingDef == null) {
            return blackedOutRoomFadeBlackness;
        }
        IsoRoom room = square.getRoom();
        if (room == null) {
            return blackedOutRoomFadeBlackness;
        }
        int cellX = buildingDef.getCellX();
        int cellY = buildingDef.getCellY();
        long metaID = room.getRoomDef().metaId;
        int playerIndex = IsoCamera.frameState.playerIndex;
        PerPlayerData perPlayerData1 = this.perPlayerData[playerIndex];
        for (int i = perPlayerData1.fadingRooms.size() - 1; i >= 0; --i) {
            FadingRoom fadingRoom = perPlayerData1.fadingRooms.get(i);
            if (!fadingRoom.equals(cellX, cellY, metaID)) continue;
            return fadingRoom.blackness;
        }
        return blackedOutRoomFadeBlackness;
    }

    public void Reset() {
        for (int i = 0; i < 4; ++i) {
            this.perPlayerData[i].reset();
        }
    }

    static {
        calculateRenderInfo = new PerformanceProfileProbe("FBORenderCell.calculateRenderInfo");
        cutaways = new PerformanceProfileProbe("FBORenderCell.cutaways");
        fog = new PerformanceProfileProbe("FBORenderCell.fog");
        puddles = new PerformanceProfileProbe("FBORenderCell.puddles");
        renderOneChunk = new PerformanceProfileProbe("FBORenderCell.renderOneChunk");
        renderOneChunkLevel = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel");
        renderOneChunkLevel2 = new PerformanceProfileProbe("FBORenderCell.renderOneChunkLevel2");
        translucentFloor = new PerformanceProfileProbe("FBORenderCell.translucentFloor");
        translucentNonFloor = new PerformanceProfileProbe("FBORenderCell.translucentNonFloor");
        updateLighting = new PerformanceProfileProbe("FBORenderCell.updateLighting");
        water = new PerformanceProfileProbe("FBORenderCell.water");
        tilesProbe = new PerformanceProfileProbe("renderTiles");
        itemsProbe = new PerformanceProfileProbe("renderItemsInWorld");
        movingObjectsProbe = new PerformanceProfileProbe("renderMovingObjects");
        shadowsProbe = new PerformanceProfileProbe("renderShadows");
        visibilityProbe = new PerformanceProfileProbe("VisibilityPolygon2");
        translucentFloorObjectsProbe = new PerformanceProfileProbe("renderTranslucentFloorObjects");
        translucentObjectsProbe = new PerformanceProfileProbe("renderTranslucentObjects");
        blackedOutRoomFadeDurationMs = 800L;
    }

    private static final class PerPlayerData {
        private final int playerIndex;
        private int lastZ = Integer.MAX_VALUE;
        private final ArrayList<IsoChunk> onScreenChunks = new ArrayList();
        private final ArrayList<IsoChunk> chunksWithAnimatedAttachments = new ArrayList();
        private final ArrayList<IsoChunk> chunksWithFlies = new ArrayList();
        private final ArrayList<IsoChunk> chunksWithTranslucentFloor = new ArrayList();
        private final ArrayList<IsoChunk> chunksWithTranslucentNonFloor = new ArrayList();
        private float playerBoundsX;
        private float playerBoundsY;
        private float playerBoundsW;
        private float playerBoundsH;
        private final ArrayList<IsoGameCharacter.Location> squaresObscuringPlayer = new ArrayList();
        private final ArrayList<IsoGameCharacter.Location> fadingInSquares = new ArrayList();
        private int lightingUpdateCounter;
        private int occludedGridX1;
        private int occludedGridY1;
        private int occludedGridX2;
        private int occludedGridY2;
        private int[] occludedGrid;
        private boolean occlusionChanged;
        private final ArrayList<BuildingDef> blackedOutBuildings = new ArrayList();
        final ArrayList<LightingJNI.VisibleRoom> visibleRooms = new ArrayList();
        final ArrayList<FadingRoom> fadingRooms = new ArrayList();

        private PerPlayerData(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        private void addChunkWith_AnimatedAttachments(IsoChunk chunk) {
            if (this.chunksWithAnimatedAttachments.contains(chunk)) {
                return;
            }
            this.chunksWithAnimatedAttachments.add(chunk);
        }

        private void addChunkWith_Flies(IsoChunk chunk) {
            if (this.chunksWithFlies.contains(chunk)) {
                return;
            }
            this.chunksWithFlies.add(chunk);
        }

        private void addChunkWith_TranslucentFloor(IsoChunk chunk) {
            if (this.chunksWithTranslucentFloor.contains(chunk)) {
                return;
            }
            this.chunksWithTranslucentFloor.add(chunk);
        }

        private void addChunkWith_TranslucentNonFloor(IsoChunk chunk) {
            if (this.chunksWithTranslucentNonFloor.contains(chunk)) {
                return;
            }
            this.chunksWithTranslucentNonFloor.add(chunk);
        }

        private boolean isSquareObscuringPlayer(IsoGridSquare square) {
            for (int i = 0; i < this.squaresObscuringPlayer.size(); ++i) {
                IsoGameCharacter.Location location = this.squaresObscuringPlayer.get(i);
                if (!location.equals(square.x, square.y, square.z)) continue;
                return true;
            }
            return false;
        }

        private boolean isFadingInSquare(IsoGridSquare square) {
            for (int i = 0; i < this.fadingInSquares.size(); ++i) {
                IsoGameCharacter.Location location = this.fadingInSquares.get(i);
                if (!location.equals(square.x, square.y, square.z)) continue;
                return true;
            }
            return false;
        }

        private boolean isObjectObscuringPlayer(IsoGridSquare square, Texture texture, float offsetX, float offsetY) {
            square.cachedScreenX = IsoUtils.XToScreen(square.x, square.y, square.z, 0);
            square.cachedScreenY = IsoUtils.YToScreen(square.x, square.y, square.z, 0);
            float textureX = square.cachedScreenX - offsetX + texture.getOffsetX();
            float textureY = square.cachedScreenY - offsetY + texture.getOffsetY();
            return textureX < this.playerBoundsX + this.playerBoundsW && textureX + (float)texture.getWidth() > this.playerBoundsX && textureY < this.playerBoundsY + this.playerBoundsH && textureY + (float)texture.getHeight() > this.playerBoundsY;
        }

        private void reset() {
            this.blackedOutBuildings.clear();
            LightingJNI.VisibleRoom.releaseAll(this.visibleRooms);
            this.visibleRooms.clear();
            this.chunksWithAnimatedAttachments.clear();
            this.chunksWithFlies.clear();
            this.chunksWithTranslucentFloor.clear();
            this.chunksWithTranslucentNonFloor.clear();
            this.fadingInSquares.clear();
            this.lastZ = Integer.MAX_VALUE;
            this.lightingUpdateCounter = 0;
            this.onScreenChunks.clear();
        }
    }

    static final class FadingRoom {
        public int cellX;
        public int cellY;
        public long metaId;
        public long startTimeMs;
        public float blackness;
        private static final ObjectPool<FadingRoom> pool = new ObjectPool<FadingRoom>(FadingRoom::new);

        FadingRoom() {
        }

        FadingRoom set(int cellX, int cellY, long metaID) {
            this.cellX = cellX;
            this.cellY = cellY;
            this.metaId = metaID;
            return this;
        }

        public FadingRoom set(LightingJNI.VisibleRoom other) {
            return this.set(other.cellX, other.cellY, other.metaId);
        }

        public boolean equals(Object rhs) {
            if (rhs instanceof LightingJNI.VisibleRoom) {
                LightingJNI.VisibleRoom other = (LightingJNI.VisibleRoom)rhs;
                return this.equals(other.cellX, other.cellY, other.metaId);
            }
            return false;
        }

        boolean equals(int cellX, int cellY, long metaID) {
            return this.cellX == cellX && this.cellY == cellY && this.metaId == metaID;
        }

        public static FadingRoom alloc() {
            return pool.alloc();
        }

        public void release() {
            pool.release(this);
        }

        public static void releaseAll(List<FadingRoom> objs) {
            pool.releaseAll(objs);
        }
    }
}

