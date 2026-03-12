/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import fmod.javafmod;
import gnu.trove.list.array.TLongArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import zombie.AmbientStreamManager;
import zombie.CombatManager;
import zombie.DebugFileWatcher;
import zombie.FliesSound;
import zombie.GameProfiler;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.LootRespawn;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.MapCollisionData;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.SystemDisabler;
import zombie.VirtualZombieManager;
import zombie.WorldSoundManager;
import zombie.ZombieSpawnRecorder;
import zombie.ZomboidFileSystem;
import zombie.ZomboidGlobals;
import zombie.audio.FMODAmbientWalls;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterRoomTypeEx;
import zombie.characters.AttachedItems.AttachedLocations;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorFactory;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.skills.CustomPerks;
import zombie.characters.skills.PerkFactory;
import zombie.chat.ChatElement;
import zombie.core.ActionManager;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.Languages;
import zombie.core.PZForkJoinPool;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.TransactionManager;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AdvancedAnimator;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.model.ModelOutlines;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.population.BeardStyles;
import zombie.core.skinnedmodel.population.ClothingDecals;
import zombie.core.skinnedmodel.population.HairStyles;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.VoiceStyles;
import zombie.core.stash.StashSystem;
import zombie.core.textures.NinePatchTexture;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamFriends;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.entity.GameEntityManager;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.entity.energy.Energy;
import zombie.erosion.ErosionGlobals;
import zombie.gameStates.AnimationViewerState;
import zombie.gameStates.AttachmentEditorState;
import zombie.gameStates.ChooseGameInfo;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.DebugGlobalObjectState;
import zombie.gameStates.GameLoadingState;
import zombie.gameStates.GameState;
import zombie.gameStates.GameStateMachine;
import zombie.gameStates.MainScreenState;
import zombie.gameStates.SeamEditorState;
import zombie.gameStates.ServerDisconnectState;
import zombie.gameStates.SpriteModelEditorState;
import zombie.gameStates.TileGeometryState;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.SGlobalObjects;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.inventory.ItemSoundManager;
import zombie.iso.BentFences;
import zombie.iso.BrokenFences;
import zombie.iso.BuildingDef;
import zombie.iso.ContainerOverlays;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWorld;
import zombie.iso.LightingThread;
import zombie.iso.RoomDef;
import zombie.iso.SearchMode;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.TileOverlays;
import zombie.iso.WorldMarkers;
import zombie.iso.WorldStreamer;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCorpses;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.RainManager;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.ClimateMoon;
import zombie.iso.weather.Temperature;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.meta.Meta;
import zombie.modding.ActiveMods;
import zombie.network.BodyDamageSync;
import zombie.network.ChunkChecksum;
import zombie.network.ClientServerMap;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PassengerMap;
import zombie.network.ServerGUI;
import zombie.network.ServerOptions;
import zombie.network.WarManager;
import zombie.network.server.AnimEventEmulator;
import zombie.network.statistics.PingManager;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.nativeCode.PathfindNative;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.animal.AnimalController;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.HutchManager;
import zombie.radio.ZomboidRadio;
import zombie.sandbox.CustomSandboxOptions;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.savefile.SavefileThumbnail;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModRegistries;
import zombie.scripting.objects.RegistryReset;
import zombie.seams.SeamManager;
import zombie.seating.SeatingManager;
import zombie.spnetwork.SinglePlayerClient;
import zombie.spnetwork.SinglePlayerServer;
import zombie.spriteModel.SpriteModelManager;
import zombie.text.templating.TemplateText;
import zombie.tileDepth.TileDepthTextureAssignmentManager;
import zombie.tileDepth.TileDepthTextureManager;
import zombie.tileDepth.TileGeometryManager;
import zombie.ui.ActionProgressBar;
import zombie.ui.FPSGraph;
import zombie.ui.ScreenFader;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.TutorialManager;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.vehicles.EditVehicleState;
import zombie.vehicles.VehicleCache;
import zombie.vehicles.VehicleIDMap;
import zombie.vehicles.VehicleType;
import zombie.vehicles.VehiclesDB2;
import zombie.worldMap.WorldMap;
import zombie.worldMap.WorldMapVisited;
import zombie.worldMap.editor.WorldMapEditorState;
import zombie.worldMap.network.WorldMapClient;

public final class IngameState
extends GameState {
    public static int waitMul = 20;
    public static IngameState instance;
    public static float draww;
    public static float drawh;
    private static float xPos;
    private static float yPos;
    private static float offx;
    private static float offy;
    private static float zoom;
    public long numberTicks;
    public boolean paused;
    public float saveDelay;
    private GameState redirectState;
    private boolean didServerDisconnectState;
    private boolean fpsKeyDown;
    private final TLongArrayList debugTimes = new TLongArrayList();
    private int tickCount;
    public boolean showAnimationViewer;
    public boolean showAttachmentEditor;
    public boolean showChunkDebugger;
    public boolean showSpriteModelEditor;
    public boolean showTileGeometryEditor;
    public boolean showGlobalObjectDebugger;
    public String showVehicleEditor;
    public String showWorldMapEditor;
    public boolean showSeamEditor;
    public static boolean loading;

    public IngameState() {
        instance = this;
    }

    public static void renderDebugOverhead(IsoCell cell, int drawFloor, int tilew, int offx, int offy) {
        Mouse.update();
        int mx = Mouse.getX();
        int my = Mouse.getY();
        mx -= offx;
        my -= offy;
        SpriteRenderer.instance.renderi(null, offx, offy, tilew * cell.getWidthInTiles(), tilew * cell.getHeightInTiles(), 0.7f, 0.7f, 0.7f, 1.0f, null);
        IsoGridSquare msq = cell.getGridSquare((mx /= tilew) + cell.chunkMap[0].getWorldXMinTiles(), (my /= tilew) + cell.chunkMap[0].getWorldYMinTiles(), 0);
        if (msq != null) {
            int n;
            int y = 48;
            int x = 48;
            TextManager.instance.DrawString(x, y, "SQUARE FLAGS", 1.0, 1.0, 1.0, 1.0);
            y += 20;
            x += 8;
            for (n = 0; n < IsoFlagType.MAX.index(); ++n) {
                if (!msq.has(IsoFlagType.fromIndex(n))) continue;
                TextManager.instance.DrawString(x, y, IsoFlagType.fromIndex(n).toString(), 0.6, 0.6, 0.8, 1.0);
                y += 18;
            }
            x = 48;
            TextManager.instance.DrawString(x, y += 16, "SQUARE OBJECT TYPES", 1.0, 1.0, 1.0, 1.0);
            y += 20;
            x += 8;
            for (n = 0; n < 64; ++n) {
                if (!msq.has(n)) continue;
                TextManager.instance.DrawString(x, y, IsoObjectType.fromIndex(n).toString(), 0.6, 0.6, 0.8, 1.0);
                y += 18;
            }
        }
        for (int x = 0; x < cell.getWidthInTiles(); ++x) {
            for (int y = 0; y < cell.getHeightInTiles(); ++y) {
                IsoGridSquare sq = cell.getGridSquare(x + cell.chunkMap[0].getWorldXMinTiles(), y + cell.chunkMap[0].getWorldYMinTiles(), drawFloor);
                if (sq == null) continue;
                if (sq.getProperties().has(IsoFlagType.solid) || sq.getProperties().has(IsoFlagType.solidtrans)) {
                    SpriteRenderer.instance.renderi(null, offx + x * tilew, offy + y * tilew, tilew, tilew, 0.5f, 0.5f, 0.5f, 255.0f, null);
                } else if (!sq.getProperties().has(IsoFlagType.exterior)) {
                    SpriteRenderer.instance.renderi(null, offx + x * tilew, offy + y * tilew, tilew, tilew, 0.8f, 0.8f, 0.8f, 1.0f, null);
                }
                if (sq.has(IsoObjectType.tree)) {
                    SpriteRenderer.instance.renderi(null, offx + x * tilew, offy + y * tilew, tilew, tilew, 0.4f, 0.8f, 0.4f, 1.0f, null);
                }
                if (sq.getProperties().has(IsoFlagType.collideN)) {
                    SpriteRenderer.instance.renderi(null, offx + x * tilew, offy + y * tilew, tilew, 1, 0.2f, 0.2f, 0.2f, 1.0f, null);
                }
                if (!sq.getProperties().has(IsoFlagType.collideW)) continue;
                SpriteRenderer.instance.renderi(null, offx + x * tilew, offy + y * tilew, 1, tilew, 0.2f, 0.2f, 0.2f, 1.0f, null);
            }
        }
    }

    public static float translatePointX(float x, float camX, float zoom, float offx) {
        x -= camX;
        x *= zoom;
        x += offx;
        return x += draww / 2.0f;
    }

    public static float invTranslatePointX(float x, float camX, float zoom, float offx) {
        x -= draww / 2.0f;
        x -= offx;
        x /= zoom;
        return x += camX;
    }

    public static float invTranslatePointY(float y, float camY, float zoom, float offy) {
        y -= drawh / 2.0f;
        y -= offy;
        y /= zoom;
        return y += camY;
    }

    public static float translatePointY(float y, float camY, float zoom, float offy) {
        y -= camY;
        y *= zoom;
        y += offy;
        return y += drawh / 2.0f;
    }

    public static void renderRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        float x1 = IngameState.translatePointX(x, xPos, zoom, offx);
        float y1 = IngameState.translatePointY(y, yPos, zoom, offy);
        float x2 = IngameState.translatePointX(x + w, xPos, zoom, offx);
        float y2 = IngameState.translatePointY(y + h, yPos, zoom, offy);
        w = x2 - x1;
        h = y2 - y1;
        if (x1 >= (float)Core.getInstance().getScreenWidth() || x2 < 0.0f || y1 >= (float)Core.getInstance().getScreenHeight() || y2 < 0.0f) {
            return;
        }
        SpriteRenderer.instance.render(null, x1, y1, w, h, r, g, b, a, null);
    }

    public static void renderLine(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        float sx1 = IngameState.translatePointX(x1, xPos, zoom, offx);
        float sy1 = IngameState.translatePointY(y1, yPos, zoom, offy);
        float sx2 = IngameState.translatePointX(x2, xPos, zoom, offx);
        float sy2 = IngameState.translatePointY(y2, yPos, zoom, offy);
        if (sx1 >= (float)Core.getInstance().getScreenWidth() && sx2 >= (float)Core.getInstance().getScreenWidth() || sy1 >= (float)Core.getInstance().getScreenHeight() && sy2 >= (float)Core.getInstance().getScreenHeight() || sx1 < 0.0f && sx2 < 0.0f || sy1 < 0.0f && sy2 < 0.0f) {
            return;
        }
        SpriteRenderer.instance.renderline(null, (int)sx1, (int)sy1, (int)sx2, (int)sy2, r, g, b, a);
    }

    public static void renderDebugOverhead2(IsoCell cell, int drawFloor, float zoom, int offx, int offy, float xPos, float yPos, int draww, int drawh) {
        IngameState.draww = draww;
        IngameState.drawh = drawh;
        IngameState.xPos = xPos;
        IngameState.yPos = yPos;
        IngameState.offx = offx;
        IngameState.offy = offy;
        IngameState.zoom = zoom;
        IsoChunkMap chunkMap = cell.getChunkMap(0);
        float strx = chunkMap.getWorldXMinTiles();
        float stry = chunkMap.getWorldYMinTiles();
        IngameState.renderRect(strx, stry, cell.getWidthInTiles(), cell.getWidthInTiles(), 0.7f, 0.7f, 0.7f, 1.0f);
        for (int lx = 0; lx < cell.getWidthInTiles(); ++lx) {
            for (int ly = 0; ly < cell.getHeightInTiles(); ++ly) {
                IsoGridSquare sq = cell.getGridSquare(lx + chunkMap.getWorldXMinTiles(), ly + chunkMap.getWorldYMinTiles(), drawFloor);
                float x = (float)lx + strx;
                float y = (float)ly + stry;
                if (sq == null) continue;
                if (sq.getProperties().has(IsoFlagType.solid) || sq.getProperties().has(IsoFlagType.solidtrans)) {
                    IngameState.renderRect(x, y, 1.0f, 1.0f, 0.5f, 0.5f, 0.5f, 1.0f);
                } else if (!sq.getProperties().has(IsoFlagType.exterior)) {
                    IngameState.renderRect(x, y, 1.0f, 1.0f, 0.8f, 0.8f, 0.8f, 1.0f);
                }
                if (sq.has(IsoObjectType.tree)) {
                    IngameState.renderRect(x, y, 1.0f, 1.0f, 0.4f, 0.8f, 0.4f, 1.0f);
                }
                if (sq.getProperties().has(IsoFlagType.collideN)) {
                    IngameState.renderRect(x, y, 1.0f, 0.2f, 0.2f, 0.2f, 0.2f, 1.0f);
                }
                if (!sq.getProperties().has(IsoFlagType.collideW)) continue;
                IngameState.renderRect(x, y, 0.2f, 1.0f, 0.2f, 0.2f, 0.2f, 1.0f);
            }
        }
        int csis = 256;
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        IngameState.renderRect(metaGrid.minX * 256, metaGrid.minY * 256, metaGrid.getWidth() * 256, metaGrid.getHeight() * 256, 1.0f, 1.0f, 1.0f, 0.05f);
        if (zoom > 0.1f) {
            for (int y = metaGrid.minY; y <= metaGrid.maxY; ++y) {
                IngameState.renderLine(metaGrid.minX * 256, y * 256, (metaGrid.maxX + 1) * 256, y * 256, 1.0f, 1.0f, 1.0f, 0.15f);
            }
            for (int x = metaGrid.minX; x <= metaGrid.maxX; ++x) {
                IngameState.renderLine(x * 256, metaGrid.minY * 256, x * 256, (metaGrid.maxY + 1) * 256, 1.0f, 1.0f, 1.0f, 0.15f);
            }
        }
        for (int xx = 0; xx < metaGrid.getWidth(); ++xx) {
            for (int y = 0; y < metaGrid.getHeight(); ++y) {
                int n;
                if (!metaGrid.hasCell(xx, y)) continue;
                IsoMetaCell metaCell = metaGrid.getCell(xx, y);
                if (metaCell == null) {
                    IngameState.renderRect((metaGrid.minX + xx) * 256 + 1, (metaGrid.minY + y) * 256 + 1, 254.0f, 254.0f, 0.2f, 0.0f, 0.0f, 0.3f);
                    continue;
                }
                if ((double)zoom > 0.8) {
                    for (n = 0; n < metaCell.buildings.size(); ++n) {
                        BuildingDef buildingDef = metaCell.buildings.get(n);
                        for (int ri = 0; ri < buildingDef.rooms.size(); ++ri) {
                            RoomDef roomDef = buildingDef.rooms.get(ri);
                            if (roomDef.level != drawFloor) continue;
                            float r = 0.5f;
                            float g = 0.5f;
                            float b = 0.8f;
                            float a = 0.3f;
                            if (buildingDef.alarmed) {
                                r = 0.8f;
                                g = 0.8f;
                                b = 0.5f;
                            }
                            for (int rri = 0; rri < roomDef.rects.size(); ++rri) {
                                RoomDef.RoomRect roomRect = roomDef.rects.get(rri);
                                IngameState.renderRect(roomRect.getX(), roomRect.getY(), roomRect.getW(), roomRect.getH(), r, g, b, 0.3f);
                            }
                        }
                    }
                    continue;
                }
                for (n = 0; n < metaCell.buildings.size(); ++n) {
                    BuildingDef def = metaCell.buildings.get(n);
                    if (def.alarmed) {
                        IngameState.renderRect(def.getX(), def.getY(), def.getW(), def.getH(), 0.8f, 0.8f, 0.5f, 0.3f);
                        continue;
                    }
                    IngameState.renderRect(def.getX(), def.getY(), def.getW(), def.getH(), 0.5f, 0.5f, 0.8f, 0.3f);
                }
            }
        }
    }

    public static void copyWorld(String src, String dest) {
        Object str = ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + src + File.separator;
        str = ((String)str).replace("/", File.separator);
        str = ((String)str).replace("\\", File.separator);
        String dir = ((String)str).substring(0, ((String)str).lastIndexOf(File.separator));
        dir = dir.replace("\\", "/");
        File f = new File(dir);
        str = ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + dest + File.separator;
        str = ((String)str).replace("/", File.separator);
        str = ((String)str).replace("\\", File.separator);
        String dir2 = ((String)str).substring(0, ((String)str).lastIndexOf(File.separator));
        dir2 = dir2.replace("\\", "/");
        File f2 = new File(dir2);
        if (GameLoadingState.convertingWorld) {
            GameLoadingState.convertingFileMax = IngameState.countAllFiles(f);
        }
        try {
            IngameState.copyDirectory(f, f2);
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
        }
    }

    private static int countAllFiles(File dir) {
        try {
            Path path = dir.toPath();
            CountFileVisitor visitor = new CountFileVisitor();
            Files.walkFileTree(path, visitor);
            return visitor.count;
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
            return -1;
        }
    }

    public static void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; ++i) {
                IngameState.copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
            }
        } else {
            try (FileInputStream in = new FileInputStream(sourceLocation);
                 FileOutputStream out = new FileOutputStream(targetLocation);){
                out.getChannel().transferFrom(in.getChannel(), 0L, sourceLocation.length());
            }
            if (GameLoadingState.convertingWorld) {
                ++GameLoadingState.convertingFileCount;
            }
        }
    }

    public static void createWorld(String worldName) {
        worldName = worldName.replace(" ", "_").trim();
        Object str = ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + worldName + File.separator;
        str = ((String)str).replace("/", File.separator);
        str = ((String)str).replace("\\", File.separator);
        String dir = ((String)str).substring(0, ((String)str).lastIndexOf(File.separator));
        File f = new File(dir = dir.replace("\\", "/"));
        if (!f.exists()) {
            f.mkdirs();
        }
        Core.gameSaveWorld = worldName;
    }

    public void debugFullyStreamedIn(int squareX, int squareY) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(squareX, squareY, 0);
        if (square == null) {
            return;
        }
        if (square.getBuilding() == null) {
            return;
        }
        BuildingDef def = square.getBuilding().getDef();
        if (def == null) {
            return;
        }
        boolean fullyStreamedIn = def.isFullyStreamedIn();
        int chunksPerWidth = 8;
        for (int i = 0; i < def.overlappedChunks.size(); i += 2) {
            short wx = def.overlappedChunks.get(i);
            short wy = def.overlappedChunks.get(i + 1);
            if (fullyStreamedIn) {
                IngameState.renderRect(wx * 8, wy * 8, 8.0f, 8.0f, 0.0f, 1.0f, 0.0f, 0.5f);
                continue;
            }
            IngameState.renderRect(wx * 8, wy * 8, 8.0f, 8.0f, 1.0f, 0.0f, 0.0f, 0.5f);
        }
    }

    public void UpdateStuff() {
        GameProfiler.ProfileArea profileArea;
        GameClient.ingame = true;
        this.saveDelay += GameTime.instance.getMultiplier();
        if (this.saveDelay / 60.0f > 30.0f) {
            this.saveDelay = 0.0f;
        }
        GameTime.instance.lastLastTimeOfDay = GameTime.instance.getLastTimeOfDay();
        GameTime.instance.setLastTimeOfDay(GameTime.getInstance().getTimeOfDay());
        boolean asleep = false;
        if (!GameServer.server && IsoPlayer.getInstance() != null) {
            asleep = IsoPlayer.allPlayersAsleep();
        }
        GameTime.getInstance().update(asleep && UIManager.getFadeAlpha() == 1.0);
        GameProfiler profiler = GameProfiler.getInstance();
        if (!this.paused) {
            profileArea = profiler.profile("ScriptManager.update");
            try {
                ScriptManager.instance.update();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        if (!this.paused) {
            GameProfiler.ProfileArea ex2;
            block142: {
                try {
                    profileArea = profiler.profile("WorldSoundManager.update");
                    try {
                        WorldSoundManager.instance.update();
                    }
                    finally {
                        if (profileArea != null) {
                            profileArea.close();
                        }
                    }
                }
                catch (Exception ex2) {
                    ExceptionLogger.logException(ex2);
                }
                try {
                    ex2 = profiler.profile("IsoFireManager.Update");
                    try {
                        IsoFireManager.Update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                }
                catch (Exception ex3) {
                    ExceptionLogger.logException(ex3);
                }
                try {
                    ex2 = profiler.profile("RainManager.Update");
                    try {
                        RainManager.Update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                }
                catch (Exception ex4) {
                    ExceptionLogger.logException(ex4);
                }
                ex2 = profiler.profile("Meta.update");
                try {
                    Meta.instance.update();
                }
                finally {
                    if (ex2 != null) {
                        ex2.close();
                    }
                }
                try {
                    ex2 = profiler.profile("VirtualZombieManager.update");
                    try {
                        VirtualZombieManager.instance.update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("MapCollisionData.updateMain");
                    try {
                        MapCollisionData.instance.updateMain();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("ZombiePopulationManager.updateMain");
                    try {
                        ZombiePopulationManager.instance.updateMain();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("PathfindNative.checkUseNativeCode");
                    try {
                        PathfindNative.instance.checkUseNativeCode();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    if (PathfindNative.useNativeCode) {
                        ex2 = profiler.profile("PathfindNative.updateMain");
                        try {
                            PathfindNative.instance.updateMain();
                            break block142;
                        }
                        finally {
                            if (ex2 != null) {
                                ex2.close();
                            }
                        }
                    }
                    ex2 = profiler.profile("PolygonalMap2.updateMain");
                    try {
                        PolygonalMap2.instance.updateMain();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                }
                catch (Throwable t) {
                    ExceptionLogger.logException(t);
                }
            }
            try (GameProfiler.ProfileArea t = profiler.profile("LootRespawn.update");){
                LootRespawn.update();
            }
            catch (Exception ex5) {
                ExceptionLogger.logException(ex5);
            }
            if (GameServer.server) {
                try {
                    AmbientStreamManager.instance.update();
                }
                catch (Exception ex6) {
                    ExceptionLogger.logException(ex6);
                }
                try {
                    AnimEventEmulator.getInstance().update();
                }
                catch (Exception ex7) {
                    ExceptionLogger.logException(ex7);
                }
                try {
                    BodyDamageSync.instance.update();
                }
                catch (Exception ex8) {
                    ExceptionLogger.logException(ex8);
                }
            }
            if (!GameServer.server) {
                try {
                    ex2 = profiler.profile("ItemSoundManager.update");
                    try {
                        ItemSoundManager.update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("FliesSound.update");
                    try {
                        FliesSound.instance.update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("CorpseFlies.update");
                    try {
                        CorpseFlies.update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                    ex2 = profiler.profile("WorldMapVisited.update");
                    try {
                        WorldMapVisited.update();
                    }
                    finally {
                        if (ex2 != null) {
                            ex2.close();
                        }
                    }
                }
                catch (Exception ex9) {
                    ExceptionLogger.logException(ex9);
                }
            }
            profileArea = profiler.profile("SearchMode.update");
            try {
                SearchMode.getInstance().update();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
            profileArea = profiler.profile("RenderSettings.update");
            try {
                RenderSettings.getInstance().update();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
    }

    private void TickMusicDirector() {
        if (!this.paused && !GameServer.server) {
            LuaManager.call("SadisticMusicDirectorTick", null);
        }
    }

    @Override
    public void enter() {
        loading = true;
        boolean bl = UIManager.useUiFbo = Core.getInstance().supportsFBO() && Core.getInstance().getOptionUIFBO();
        if (!Core.getInstance().getUseShaders()) {
            SceneShaderStore.weatherShader = null;
        }
        GameSounds.fix3DListenerPosition(false);
        IsoPlayer.getInstance().updateUsername();
        IsoPlayer.getInstance().setSceneCulled(false);
        IsoPlayer.getInstance().getInventory().addItemsToProcessItems();
        ZombieSpawnRecorder.instance.init();
        if (!GameServer.server) {
            IsoWorld.instance.currentCell.chunkMap[0].processAllLoadGridSquare();
            IsoWorld.instance.currentCell.chunkMap[0].update();
            if (!GameClient.client) {
                LightingThread.instance.GameLoadingUpdate();
            }
        }
        loading = false;
        try {
            MapCollisionData.instance.startGame();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        IsoWorld.instance.currentCell.putInVehicle(IsoPlayer.getInstance());
        SoundManager.instance.setMusicState("Tutorial".equals(Core.getInstance().getGameMode()) ? "Tutorial" : "InGame");
        ClimateManager.getInstance().update();
        AmbientStreamManager.instance.checkHaveElectricity();
        LuaEventManager.triggerEvent("OnGameStart");
        LuaEventManager.triggerEvent("OnLoad");
        if (GameClient.client) {
            GameClient.instance.sendPlayerConnect(IsoPlayer.getInstance());
            DebugLog.log("Waiting for player-connect response from server");
            while (IsoPlayer.getInstance().onlineId == -1 && GameClient.connection != null) {
                try {
                    Thread.sleep(10L);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GameClient.instance.update();
            }
            ClimateManager.getInstance().update();
            LightingThread.instance.GameLoadingUpdate();
        }
        if (GameClient.client && SteamUtils.isSteamModeEnabled()) {
            SteamFriends.UpdateRichPresenceConnectionInfo("In game", "+connect " + GameClient.ip + ":" + GameClient.port);
        }
        FBORenderOcclusion.getInstance().init();
        UIManager.setbFadeBeforeUI(false);
        UIManager.FadeIn(0.35f);
    }

    @Override
    public void exit() {
        DebugType.ExitDebug.debugln("IngameState.exit 1");
        if (SteamUtils.isSteamModeEnabled()) {
            SteamFriends.UpdateRichPresenceConnectionInfo("", "");
        }
        UIManager.useUiFbo = false;
        if (FPSGraph.instance != null) {
            FPSGraph.instance.setVisible(false);
        }
        UIManager.updateBeforeFadeOut();
        SoundManager.instance.setMusicState("MainMenu");
        ScreenFader screenFader = new ScreenFader();
        screenFader.startFadeToBlack();
        boolean useUIFBO = UIManager.useUiFbo;
        UIManager.useUiFbo = false;
        DebugType.ExitDebug.debugln("IngameState.exit 2");
        while (screenFader.isFading()) {
            boolean clear = true;
            for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                if (IsoPlayer.players[n] == null) continue;
                IsoPlayer.setInstance(IsoPlayer.players[n]);
                IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
                IsoSprite.globalOffsetX = -1.0f;
                Core.getInstance().StartFrame(n, clear);
                IsoCamera.frameState.set(n);
                IsoWorld.instance.render();
                Core.getInstance().EndFrame(n);
                clear = false;
            }
            Core.getInstance().RenderOffScreenBuffer();
            Core.getInstance().StartFrameUI();
            UIManager.render();
            screenFader.update();
            screenFader.render();
            Core.getInstance().EndFrameUI();
            DebugType.ExitDebug.debugln("IngameState.exit 3 (alpha=" + screenFader.getAlpha() + ")");
            if (!screenFader.isFading()) continue;
            try {
                Thread.sleep(33L);
            }
            catch (Exception exception) {}
        }
        UIManager.useUiFbo = useUIFBO;
        DebugType.ExitDebug.debugln("IngameState.exit 4");
        RenderThread.setWaitForRenderState(false);
        SpriteRenderer.instance.notifyRenderStateQueue();
        long time = System.currentTimeMillis();
        while (WorldStreamer.instance.isBusy()) {
            try {
                Thread.sleep(1L);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - time <= 6000L) continue;
            DebugType.ExitDebug.debugln("IngameState.exit 4A TIMEOUT");
            break;
        }
        DebugType.ExitDebug.debugln("IngameState.exit 5");
        WorldStreamer.instance.stop();
        LightingThread.instance.stop();
        MapCollisionData.instance.stop();
        AnimalPopulationManager.getInstance().stop();
        ZombiePopulationManager.instance.stop();
        IsoPuddles.getInstance().freeHMTextureBuffer();
        if (PathfindNative.useNativeCode) {
            PathfindNative.instance.stop();
        } else {
            PolygonalMap2.instance.stop();
        }
        AnimalInstanceManager.getInstance().stop();
        DebugType.ExitDebug.debugln("IngameState.exit 6");
        for (int i = 0; i < IsoWorld.instance.currentCell.chunkMap.length; ++i) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[i];
            for (int n = 0; n < IsoChunkMap.chunkGridWidth * IsoChunkMap.chunkGridWidth; ++n) {
                IsoChunk chunk = chunkMap.getChunk(n % IsoChunkMap.chunkGridWidth, n / IsoChunkMap.chunkGridWidth);
                if (chunk == null || !chunk.refs.contains(chunkMap)) continue;
                chunk.refs.remove(chunkMap);
                if (!chunk.refs.isEmpty()) continue;
                chunk.removeFromWorld();
                chunk.doReuseGridsquares();
            }
        }
        ModelManager.instance.Reset();
        IsoPlayer.Reset();
        ZombieSpawnRecorder.instance.quit();
        DebugType.ExitDebug.debugln("IngameState.exit 7");
        IsoPlayer.numPlayers = 1;
        Core.getInstance().offscreenBuffer.destroy();
        WeatherFxMask.destroy();
        IsoRegions.reset();
        Temperature.reset();
        WorldMarkers.instance.reset();
        IsoMarkers.instance.reset();
        SearchMode.reset();
        ZomboidRadio.getInstance().Reset();
        IsoWaveSignal.Reset();
        GameEntityManager.Reset();
        SpriteConfigManager.Reset();
        Fluid.Reset();
        Energy.Reset();
        CraftRecipeManager.Reset();
        ErosionGlobals.Reset();
        IsoGenerator.Reset();
        StashSystem.Reset();
        LootRespawn.Reset();
        VehicleCache.Reset();
        VehicleIDMap.instance.Reset();
        IsoWorld.instance.KillCell();
        ItemSoundManager.Reset();
        IsoChunk.Reset();
        ChunkChecksum.Reset();
        ClientServerMap.Reset();
        SinglePlayerClient.Reset();
        SinglePlayerServer.Reset();
        PassengerMap.Reset();
        DeadBodyAtlas.instance.Reset();
        WorldItemAtlas.instance.Reset();
        FBORenderCorpses.getInstance().Reset();
        FBORenderItems.getInstance().Reset();
        FBORenderChunkManager.instance.Reset();
        FBORenderCell.instance.Reset();
        CorpseFlies.Reset();
        if (PlayerDB.isAvailable()) {
            PlayerDB.getInstance().close();
        }
        VehiclesDB2.instance.Reset();
        WorldMap.Reset();
        AnimalDefinitions.Reset();
        AnimalZones.Reset();
        MigrationGroupDefinitions.Reset();
        DesignationZone.Reset();
        DesignationZoneAnimal.Reset();
        HutchManager.getInstance().clear();
        if (GameWindow.loadedAsClient) {
            WorldMapClient.instance.Reset();
        }
        WorldStreamer.instance = new WorldStreamer();
        WorldSimulation.instance.destroy();
        WorldSimulation.instance = new WorldSimulation();
        DebugType.ExitDebug.debugln("IngameState.exit 8");
        VirtualZombieManager.instance.Reset();
        VirtualZombieManager.instance = new VirtualZombieManager();
        ReanimatedPlayers.instance = new ReanimatedPlayers();
        GameSounds.Reset();
        VehicleType.Reset();
        TemplateText.Reset();
        CombatManager.getInstance().Reset();
        ClimateMoon.getInstance().Reset();
        ClimateManager.getInstance().Reset();
        LuaEventManager.Reset();
        MapObjects.Reset();
        CGlobalObjects.Reset();
        SGlobalObjects.Reset();
        AmbientStreamManager.instance.stop();
        SoundManager.instance.stop();
        IsoPlayer.setInstance(null);
        IsoCamera.clearCameraCharacter();
        TutorialManager.instance.stealControl = false;
        UIManager.init();
        RegistryReset.resetAll();
        ScriptManager.instance.Reset();
        ClothingDecals.Reset();
        BeardStyles.Reset();
        HairStyles.Reset();
        OutfitManager.Reset();
        AnimationSet.Reset();
        GameSounds.Reset();
        SurvivorFactory.Reset();
        ChooseGameInfo.Reset();
        AttachedLocations.Reset();
        BodyLocations.reset();
        ContainerOverlays.instance.Reset();
        BentFences.getInstance().Reset();
        BrokenFences.getInstance().Reset();
        TileOverlays.instance.Reset();
        LuaHookManager.Reset();
        CustomPerks.Reset();
        PerkFactory.Reset();
        CustomSandboxOptions.Reset();
        SandboxOptions.Reset();
        LuaManager.init();
        JoypadManager.instance.Reset();
        GameKeyboard.doLuaKeyPressed = true;
        GameWindow.activatedJoyPad = null;
        GameWindow.okToSaveOnExit = false;
        GameWindow.loadedAsClient = false;
        Core.lastStand = false;
        Core.challengeId = null;
        Core.tutorial = false;
        Core.getInstance().setChallenge(false);
        Core.getInstance().setForceSnow(false);
        Core.getInstance().setZombieGroupSound(true);
        Core.getInstance().setFlashIsoCursor(false);
        SystemDisabler.Reset();
        Texture.nullTextures.clear();
        NinePatchTexture.Reset();
        SpriteModelManager.getInstance().Reset();
        TileGeometryManager.getInstance().Reset();
        TileDepthTextureManager.getInstance().Reset();
        SeamManager.getInstance().Reset();
        SeatingManager.getInstance().Reset();
        DebugType.ExitDebug.debugln("IngameState.exit 9");
        ZomboidFileSystem.instance.Reset();
        WarManager.clear();
        if (!Core.soundDisabled && !GameServer.server) {
            javafmod.FMOD_System_Update();
        }
        try {
            ZomboidFileSystem.instance.init();
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
        }
        Core.optionModsEnabled = true;
        DebugType.ExitDebug.debugln("IngameState.exit 10");
        ZomboidFileSystem.instance.loadMods("default");
        ZomboidFileSystem.instance.loadModPackFiles();
        Languages.instance.init();
        Translator.loadFiles();
        DebugType.ExitDebug.debugln("IngameState.exit 11");
        CustomPerks.instance.init();
        CustomPerks.instance.initLua();
        CustomSandboxOptions.instance.init();
        CustomSandboxOptions.instance.initInstance(SandboxOptions.instance);
        ModRegistries.init();
        try {
            ScriptManager.instance.Load();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        SpriteModelManager.getInstance().init();
        ModelManager.instance.initAnimationMeshes(true);
        ModelManager.instance.loadModAnimations();
        ClothingDecals.init();
        BeardStyles.init();
        HairStyles.init();
        OutfitManager.init();
        VoiceStyles.init();
        TileGeometryManager.getInstance().init();
        TileDepthTextureAssignmentManager.getInstance().init();
        TileDepthTextureManager.getInstance().init();
        SeamManager.getInstance().init();
        SeatingManager.getInstance().init();
        DebugType.ExitDebug.debugln("IngameState.exit 12");
        try {
            TextManager.instance.Init();
            LuaManager.LoadDirBase();
        }
        catch (Exception e) {
            ExceptionLogger.logException(e);
        }
        ZomboidGlobals.Load();
        DebugType.ExitDebug.debugln("IngameState.exit 13");
        LuaEventManager.triggerEvent("OnGameBoot");
        SoundManager.instance.resumeSoundAndMusic();
        for (IsoPlayer player : IsoPlayer.players) {
            if (player == null) continue;
            player.dirtyRecalcGridStack = true;
        }
        RenderThread.setWaitForRenderState(true);
        DebugType.ExitDebug.debugln("IngameState.exit 14");
    }

    @Override
    public void yield() {
        SoundManager.instance.setMusicState("PauseMenu");
    }

    @Override
    public GameState redirectState() {
        if (this.redirectState != null) {
            GameState state = this.redirectState;
            this.redirectState = null;
            return state;
        }
        return new MainScreenState();
    }

    @Override
    public void reenter() {
        SoundManager.instance.setMusicState("InGame");
    }

    public void renderframetext(int nPlayer) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderFrameText.profile();){
            this.renderFrameTextInternal(nPlayer);
        }
    }

    private void renderFrameTextInternal(int nPlayer) {
        IndieGL.disableAlphaTest();
        IndieGL.disableDepthTest();
        ArrayList<UIElementInterface> uis = UIManager.getUI();
        for (int i = 0; i < uis.size(); ++i) {
            UIElementInterface ui = uis.get(i);
            if (ui instanceof ActionProgressBar || !ui.isVisible().booleanValue() || !ui.isFollowGameWorld().booleanValue() || ui.getRenderThisPlayerOnly() != -1 && ui.getRenderThisPlayerOnly() != nPlayer) continue;
            ui.render();
        }
        ActionProgressBar progressBar = UIManager.getProgressBar(nPlayer);
        if (progressBar != null && progressBar.isVisible().booleanValue()) {
            progressBar.render();
        }
        WorldMarkers.instance.render();
        IsoMarkers.instance.render();
        TextDrawObject.RenderBatch(nPlayer);
        ChatElement.RenderBatch(nPlayer);
        if (DebugOptions.instance.character.debug.render.fmodRoomType.getValue()) {
            ParameterRoomTypeEx.renderRoomTones();
        }
        try {
            Core.getInstance().EndFrameText(nPlayer);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void renderframe(int nPlayer) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderFrame.profile();){
            this.renderFrameInternal(nPlayer);
        }
    }

    private void renderFrameInternal(int nPlayer) {
        if (IsoPlayer.getInstance() == null) {
            IsoPlayer.setInstance(IsoPlayer.players[0]);
            IsoCamera.setCameraCharacter(IsoPlayer.getInstance());
        }
        RenderSettings.getInstance().applyRenderSettings(nPlayer);
        ActionProgressBar progressBar = UIManager.getProgressBar(nPlayer);
        if (progressBar != null) {
            progressBar.update(nPlayer);
        }
        IndieGL.disableAlphaTest();
        IndieGL.disableDepthTest();
        if (IsoPlayer.getInstance() != null && !IsoPlayer.getInstance().isAsleep() || UIManager.getFadeAlpha(nPlayer) < 1.0f) {
            ModelOutlines.instance.startFrameMain(nPlayer);
            IsoWorld.instance.render();
            ModelOutlines.instance.endFrameMain(nPlayer);
            RenderSettings.getInstance().legacyPostRender(nPlayer);
            LuaEventManager.triggerEvent("OnPostRender");
        }
        LineDrawer.clear();
        if (Core.debug && GameKeyboard.isKeyPressed("ToggleAnimationText")) {
            DebugOptions.instance.animation.debug.setValue(!DebugOptions.instance.animation.debug.getValue());
        }
        try {
            Core.getInstance().EndFrame(nPlayer);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void renderframeui() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderFrameUI.profile();){
            this.renderFrameUI();
        }
    }

    private void renderFrameUI() {
        if (Core.getInstance().StartFrameUI()) {
            TextManager.instance.DrawTextFromGameWorld();
            SkyBox.getInstance().draw();
            try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("UIManager.render");){
                UIManager.render();
            }
            ZomboidRadio.getInstance().render();
            if (Core.debug && IsoPlayer.getInstance() != null && IsoPlayer.getInstance().isGhostMode()) {
                IsoWorld.instance.currentCell.chunkMap[0].drawDebugChunkMap();
            }
            DeadBodyAtlas.instance.renderUI();
            WorldItemAtlas.instance.renderUI();
            if (Core.debug) {
                if (GameKeyboard.isKeyDown("Display FPS")) {
                    if (!this.fpsKeyDown) {
                        this.fpsKeyDown = true;
                        if (FPSGraph.instance == null) {
                            FPSGraph.instance = new FPSGraph();
                        }
                        FPSGraph.instance.setVisible(FPSGraph.instance.isVisible() == false);
                    }
                } else {
                    this.fpsKeyDown = false;
                }
                if (FPSGraph.instance != null) {
                    FPSGraph.instance.render();
                }
            }
            if (!GameServer.server) {
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null || player.isDead() || !player.isAsleep()) continue;
                    float hours = GameClient.fastForward ? GameTime.getInstance().serverTimeOfDay : GameTime.getInstance().getTimeOfDay();
                    LuaEventManager.triggerEvent("OnSleepingTick", BoxedStaticValues.toDouble(i), BoxedStaticValues.toDouble(hours));
                }
            }
            ActiveMods.renderUI();
            JoypadManager.instance.renderUI();
        }
        if (Core.debug && DebugOptions.instance.animation.animRenderPicker.getValue() && IsoPlayer.players[0] != null) {
            IsoPlayer.players[0].advancedAnimator.render();
        }
        if (Core.debug) {
            ModelOutlines.instance.renderDebug();
        }
        Core.getInstance().EndFrameUI();
    }

    @Override
    public void render() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.render.profile();){
            this.renderInternal();
        }
    }

    private void renderInternal() {
        int n;
        boolean clear = true;
        for (n = 0; n < IsoPlayer.numPlayers; ++n) {
            if (IsoPlayer.players[n] == null) {
                if (n != 0) continue;
                SpriteRenderer.instance.prePopulating();
                continue;
            }
            IsoPlayer.setInstance(IsoPlayer.players[n]);
            IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
            Core.getInstance().StartFrame(n, clear);
            IsoCamera.frameState.set(n);
            clear = false;
            IsoSprite.globalOffsetX = -1.0f;
            this.renderframe(n);
        }
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderObjectHighlight.getInstance().clearHighlightOnceFlag();
        }
        if (DebugOptions.instance.offscreenBuffer.render.getValue()) {
            Core.getInstance().RenderOffScreenBuffer();
        }
        for (n = 0; n < IsoPlayer.numPlayers; ++n) {
            if (IsoPlayer.players[n] == null) continue;
            IsoPlayer.setInstance(IsoPlayer.players[n]);
            IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
            IsoCamera.frameState.set(n);
            Core.getInstance().StartFrameText(n);
            this.renderframetext(n);
        }
        UIManager.resize();
        this.renderframeui();
    }

    @Override
    public GameStateMachine.StateAction update() {
        try (AbstractPerformanceProfileProbe ignored = s_performance.update.profile();){
            GameStateMachine.StateAction stateAction = this.updateInternal();
            return stateAction;
        }
    }

    private GameStateMachine.StateAction updateInternal() {
        GameProfiler.ProfileArea profileArea;
        GameProfiler profiler;
        block111: {
            ++this.tickCount;
            if (this.tickCount < 60) {
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    if (IsoPlayer.players[i] == null) continue;
                    IsoPlayer.players[i].dirtyRecalcGridStackTime = 20.0f;
                }
            }
            LuaEventManager.triggerEvent("OnTickEvenPaused", BoxedStaticValues.toDouble(this.numberTicks));
            DebugFileWatcher.instance.update();
            AdvancedAnimator.checkModifiedFiles();
            if (Core.debug) {
                this.debugTimes.resetQuick();
                this.debugTimes.add(System.nanoTime());
            }
            if (Core.exiting) {
                DebugType.ExitDebug.debugln("IngameState Exiting...");
                DebugType.ExitDebug.debugln("IngameState.updateInternal 1");
                Core.exiting = false;
                if (GameClient.client) {
                    WorldStreamer.instance.stop();
                    GameClient.instance.doDisconnect("exiting");
                }
                DebugType.ExitDebug.debugln("IngameState.updateInternal 2");
                if (PlayerDB.isAllow()) {
                    PlayerDB.getInstance().saveLocalPlayersForce();
                    PlayerDB.getInstance().canSavePlayers = false;
                }
                try {
                    GameWindow.save(true);
                }
                catch (Throwable t) {
                    ExceptionLogger.logException(t);
                }
                DebugType.ExitDebug.debugln("IngameState.updateInternal 3");
                try {
                    LuaEventManager.triggerEvent("OnPostSave");
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
                if (ClientPlayerDB.isAllow()) {
                    ClientPlayerDB.getInstance().close();
                }
                DebugType.ExitDebug.debugln("IngameState Exiting done.");
                return GameStateMachine.StateAction.Continue;
            }
            if (GameWindow.serverDisconnected) {
                try {
                    if (Thread.currentThread() == GameWindow.gameThread && GameClient.connection != null) {
                        SavefileThumbnail.createForMP(GameClient.connection.getIP(), GameClient.port, GameClient.username);
                    }
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                }
                TutorialManager.instance.stealControl = true;
                if (!this.didServerDisconnectState) {
                    this.didServerDisconnectState = true;
                    this.redirectState = new ServerDisconnectState();
                    return GameStateMachine.StateAction.Yield;
                }
                GameClient.connection = null;
                GameClient.instance.connected = false;
                GameClient.client = false;
                GameWindow.serverDisconnected = false;
                ConnectionManager.getInstance().process();
                return GameStateMachine.StateAction.Continue;
            }
            if (Core.debug) {
                if (this.showGlobalObjectDebugger || GameKeyboard.isKeyPressed(60) && GameKeyboard.isKeyDown(29)) {
                    this.showGlobalObjectDebugger = false;
                    DebugLog.General.debugln("Activating DebugGlobalObjectState.");
                    this.redirectState = new DebugGlobalObjectState();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showChunkDebugger || GameKeyboard.isKeyPressed(60)) {
                    this.showChunkDebugger = false;
                    DebugLog.General.debugln("Activating DebugChunkState.");
                    this.redirectState = DebugChunkState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showAnimationViewer || GameKeyboard.isKeyPressed(65) && GameKeyboard.isKeyDown(29)) {
                    this.showAnimationViewer = false;
                    DebugLog.General.debugln("Activating AnimationViewerState.");
                    this.redirectState = AnimationViewerState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showAttachmentEditor || GameKeyboard.isKeyPressed(65) && GameKeyboard.isKeyDown(42)) {
                    this.showAttachmentEditor = false;
                    DebugLog.General.debugln("Activating AttachmentEditorState.");
                    this.redirectState = AttachmentEditorState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showVehicleEditor != null || GameKeyboard.isKeyPressed(65)) {
                    DebugLog.General.debugln("Activating EditVehicleState.");
                    EditVehicleState state = EditVehicleState.checkInstance();
                    if (!StringUtils.isNullOrWhitespace(this.showVehicleEditor)) {
                        state.setScript(this.showVehicleEditor);
                    }
                    this.showVehicleEditor = null;
                    this.redirectState = state;
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showSpriteModelEditor || GameKeyboard.isKeyPressed(66) && GameKeyboard.isKeyDown(29)) {
                    this.showSpriteModelEditor = false;
                    DebugLog.General.debugln("Activating SpriteModelEditorState.");
                    this.redirectState = SpriteModelEditorState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showTileGeometryEditor || GameKeyboard.isKeyPressed(66) && GameKeyboard.isKeyDown(42)) {
                    this.showTileGeometryEditor = false;
                    DebugLog.General.debugln("Activating TileGeometryState.");
                    this.redirectState = TileGeometryState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showWorldMapEditor != null || GameKeyboard.isKeyPressed(66)) {
                    WorldMapEditorState state = WorldMapEditorState.checkInstance();
                    this.showWorldMapEditor = null;
                    this.redirectState = state;
                    return GameStateMachine.StateAction.Yield;
                }
                if (this.showSeamEditor || GameKeyboard.isKeyPressed(67)) {
                    this.showSeamEditor = false;
                    DebugLog.General.debugln("Activating SeamEditorState.");
                    this.redirectState = SeamEditorState.checkInstance();
                    return GameStateMachine.StateAction.Yield;
                }
            }
            if (Core.debug) {
                this.debugTimes.add(System.nanoTime());
            }
            profiler = GameProfiler.getInstance();
            try {
                if (!GameServer.server) {
                    if (IsoPlayer.getInstance() != null && IsoPlayer.allPlayersDead()) {
                        if (IsoPlayer.getInstance() != null) {
                            UIManager.getSpeedControls().SetCurrentGameSpeed(1);
                        }
                        IsoCamera.update();
                    }
                    waitMul = 1;
                    if (UIManager.getSpeedControls() != null) {
                        if (UIManager.getSpeedControls().getCurrentGameSpeed() == 2) {
                            waitMul = 15;
                        }
                        if (UIManager.getSpeedControls().getCurrentGameSpeed() == 3) {
                            waitMul = 30;
                        }
                    }
                }
                if (Core.debug) {
                    this.debugTimes.add(System.nanoTime());
                }
                if (GameServer.server) {
                    boolean bl = this.paused = GameServer.Players.isEmpty() && ServerOptions.instance.pauseEmpty.getValue() && ZombiePopulationManager.instance.readyToPause();
                }
                if (this.paused && !GameClient.client) break block111;
                try {
                    if (IsoCamera.getCameraCharacter() != null && IsoWorld.instance.doChunkMapUpdate) {
                        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                            if (IsoPlayer.players[n] == null || IsoWorld.instance.currentCell.chunkMap[n].ignore || GameServer.server) continue;
                            IsoCamera.setCameraCharacter(IsoPlayer.players[n]);
                            IsoPlayer.setInstance(IsoPlayer.players[n]);
                            IsoWorld.instance.currentCell.chunkMap[n].ProcessChunkPos(IsoCamera.getCameraCharacter());
                        }
                    }
                    if (Core.debug) {
                        this.debugTimes.add(System.nanoTime());
                    }
                    IsoWorld.instance.update();
                    CompletableFuture<Void> objAmbEmit = null;
                    if (DebugOptions.instance.threadAmbient.getValue() && !GameServer.server) {
                        objAmbEmit = CompletableFuture.runAsync(ObjectAmbientEmitters.getInstance()::update, PZForkJoinPool.commonPool());
                    }
                    try (GameProfiler.ProfileArea profileArea2 = profiler.profile("GEM Update");){
                        GameEntityManager.Update();
                    }
                    profileArea2 = profiler.profile("Animal");
                    try {
                        AnimalController.getInstance().update();
                    }
                    finally {
                        if (profileArea2 != null) {
                            profileArea2.close();
                        }
                    }
                    if (Core.debug) {
                        this.debugTimes.add(System.nanoTime());
                    }
                    profileArea2 = profiler.profile("Radio");
                    try {
                        ZomboidRadio.getInstance().update();
                    }
                    finally {
                        if (profileArea2 != null) {
                            profileArea2.close();
                        }
                    }
                    profileArea2 = profiler.profile("Stuff");
                    try {
                        this.UpdateStuff();
                    }
                    finally {
                        if (profileArea2 != null) {
                            profileArea2.close();
                        }
                    }
                    profileArea2 = profiler.profile("On Tick");
                    try {
                        this.onTick();
                    }
                    finally {
                        if (profileArea2 != null) {
                            profileArea2.close();
                        }
                    }
                    try {
                        FMODAmbientWalls.getInstance().update();
                    }
                    catch (Throwable t) {
                        ExceptionLogger.logException(t);
                    }
                    this.TickMusicDirector();
                    if (objAmbEmit != null) {
                        try (GameProfiler.ProfileArea t = profiler.profile("ObjectAmbientEmitters.update");){
                            objAmbEmit.join();
                        }
                    } else {
                        ObjectAmbientEmitters.getInstance().update();
                    }
                    this.numberTicks = Math.max(this.numberTicks + 1L, 0L);
                }
                catch (Exception ex) {
                    ExceptionLogger.logException(ex);
                    if (!GameServer.server) {
                        if (GameClient.client) {
                            WorldStreamer.instance.stop();
                        }
                        String old = Core.gameSaveWorld;
                        IngameState.createWorld(Core.gameSaveWorld + "_crash");
                        IngameState.copyWorld(old, Core.gameSaveWorld);
                        if (GameClient.client && PlayerDB.isAllow()) {
                            PlayerDB.getInstance().saveLocalPlayersForce();
                            PlayerDB.getInstance().canSavePlayers = false;
                        }
                        try {
                            GameWindow.save(true);
                        }
                        catch (Throwable t) {
                            ExceptionLogger.logException(t);
                        }
                        if (GameClient.client) {
                            try {
                                LuaEventManager.triggerEvent("OnPostSave");
                            }
                            catch (Exception e) {
                                ExceptionLogger.logException(e);
                            }
                            if (ClientPlayerDB.isAllow()) {
                                ClientPlayerDB.getInstance().close();
                            }
                        }
                    }
                    if (GameClient.client) {
                        GameClient.instance.doDisconnect("crash");
                    }
                    return GameStateMachine.StateAction.Continue;
                }
            }
            catch (Exception ex) {
                System.err.println("IngameState.update caught an exception.");
                ExceptionLogger.logException(ex);
            }
        }
        if (Core.debug) {
            this.debugTimes.add(System.nanoTime());
        }
        if (!GameServer.server || ServerGUI.isCreated()) {
            profileArea = profiler.profile("Update Model");
            try {
                ModelManager.instance.update();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        if (Core.debug && FPSGraph.instance != null) {
            FPSGraph.instance.addUpdate(System.currentTimeMillis());
            FPSGraph.instance.update();
        }
        if (GameClient.client || GameServer.server) {
            profileArea = profiler.profile("Update Managers");
            try {
                IngameState.updateManagers();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        return GameStateMachine.StateAction.Remain;
    }

    private void onTick() {
        LuaEventManager.triggerEvent("OnTick", BoxedStaticValues.toDouble(this.numberTicks));
    }

    private static void updateManagers() {
        TransactionManager.update();
        ActionManager.update();
        PingManager.update();
    }

    private static final class CountFileVisitor
    implements FileVisitor<Path> {
        private int count;

        private CountFileVisitor() {
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            ++this.count;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            ExceptionLogger.logException(exc);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }

    private static class s_performance {
        private static final PerformanceProfileProbe render = new PerformanceProfileProbe("IngameState.render");
        private static final PerformanceProfileProbe renderFrame = new PerformanceProfileProbe("IngameState.renderFrame");
        private static final PerformanceProfileProbe renderFrameText = new PerformanceProfileProbe("IngameState.renderFrameText");
        private static final PerformanceProfileProbe renderFrameUI = new PerformanceProfileProbe("IngameState.renderFrameUI");
        private static final PerformanceProfileProbe update = new PerformanceProfileProbe("IngameState.update");

        private s_performance() {
        }
    }
}

