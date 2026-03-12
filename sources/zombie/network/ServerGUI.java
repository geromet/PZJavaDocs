/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import org.lwjglx.opengl.PixelFormat;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.WorldSoundManager;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterInside;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TexturePackPage;
import zombie.debug.LineDrawer;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.WorldMarkers;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderAreaHighlights;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;

public class ServerGUI {
    private static boolean created;
    private static int minX;
    private static int minY;
    private static int maxX;
    private static int maxY;
    private static int maxZ;
    private static final ArrayList<IsoGridSquare> GridStack;
    private static final ArrayList<IsoGridSquare> MinusFloorCharacters;
    private static final ArrayList<IsoGridSquare> SolidFloor;
    private static final ArrayList<IsoGridSquare> VegetationCorpses;
    private static final ColorInfo defColorInfo;

    public static boolean isCreated() {
        return created;
    }

    public static void init() {
        created = true;
        try {
            Display.setFullscreen(false);
            Display.setResizable(false);
            Display.setVSyncEnabled(false);
            Display.setTitle("Project Zomboid Server");
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
            Core.width = 1366;
            Core.height = 768;
            Display.setDisplayMode(new DisplayMode(Core.width, Core.height));
            Display.create(new PixelFormat(32, 0, 24, 8, 0));
            Display.setIcon(MainScreenState.loadIcons());
            GLVertexBufferObject.init();
            Display.makeCurrent();
            SpriteRenderer.instance.create();
            TextManager.instance.Init();
            while (TextManager.instance.font.isEmpty()) {
                GameWindow.fileSystem.updateAsyncTransactions();
                try {
                    Thread.sleep(10L);
                }
                catch (InterruptedException interruptedException) {}
            }
            Core.getInstance().initGlobalShader();
            TexturePackPage.ignoreWorldItemTextures = true;
            int flags = 2;
            GameWindow.LoadTexturePack("UI", flags);
            GameWindow.LoadTexturePack("UI2", flags);
            GameWindow.LoadTexturePack("IconsMoveables", flags);
            GameWindow.LoadTexturePack("RadioIcons", flags);
            GameWindow.LoadTexturePack("ApComUI", flags);
            GameWindow.LoadTexturePack("WeatherFx", flags);
            TexturePackPage.ignoreWorldItemTextures = false;
            flags = 0;
            GameWindow.LoadTexturePack("Tiles2x", flags);
            GameWindow.LoadTexturePack("JumboTrees2x", flags);
            GameWindow.LoadTexturePack("Overlays2x", flags);
            GameWindow.LoadTexturePack("Tiles2x.floor", 0);
            GameWindow.DoLoadingText("");
            GameWindow.setTexturePackLookup();
            IsoObjectPicker.Instance.Init();
            Display.makeCurrent();
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            Display.releaseContext();
            RenderThread.initServerGUI();
            RenderThread.startRendering();
            Core.getInstance().initFBOs();
            Core.getInstance().initShaders();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            created = false;
            return;
        }
    }

    public static void init2() {
        if (!created) {
            return;
        }
        BaseVehicle.LoadAllVehicleTextures();
    }

    public static void shutdown() {
        if (!created) {
            return;
        }
        RenderThread.shutdown();
    }

    public static void update() {
        int wheel;
        if (!created) {
            return;
        }
        Mouse.update();
        GameKeyboard.update();
        Display.processMessages();
        if (RenderThread.isCloseRequested()) {
            // empty if block
        }
        if ((wheel = Mouse.getWheelState()) != 0) {
            int del = wheel - 0 < 0 ? 1 : -1;
            Core.getInstance().doZoomScroll(0, del);
        }
        boolean playerIndex = false;
        IsoPlayer player = ServerGUI.getPlayerToFollow();
        if (player == null) {
            Core.getInstance().StartFrame();
            Core.getInstance().EndFrame();
            Core.getInstance().StartFrameUI();
            SpriteRenderer.instance.renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0f, 0.0f, 0.0f, 1.0f, null);
            Core.getInstance().EndFrameUI();
            return;
        }
        DebugChunkState.checkInstance();
        IsoPlayer.setInstance(player);
        IsoPlayer.players[0] = player;
        IsoCamera.setCameraCharacter(player);
        IsoWorld.instance.currentCell.chunkMap[0].calculateZExtentsForChunkMap();
        Core.getInstance().StartFrame(0, true);
        ServerGUI.renderWorld();
        Core.getInstance().EndFrame(0);
        Core.getInstance().RenderOffScreenBuffer();
        Core.getInstance().StartFrameUI();
        ServerGUI.renderUI();
        Core.getInstance().EndFrameUI();
    }

    private static IsoPlayer getPlayerToFollow() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); ++n) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (!c.isFullyConnected()) continue;
            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = c.players[playerIndex];
                if (player == null || player.onlineId == -1) continue;
                return player;
            }
        }
        return null;
    }

    private static void updateCamera(IsoPlayer player) {
        boolean playerIndex = false;
        PlayerCamera camera = IsoCamera.cameras[0];
        float offX = IsoUtils.XToScreen(player.getX() + camera.deferedX, player.getY() + camera.deferedY, player.getZ(), 0);
        float offY = IsoUtils.YToScreen(player.getX() + camera.deferedX, player.getY() + camera.deferedY, player.getZ(), 0);
        offX -= (float)(IsoCamera.getOffscreenWidth(0) / 2);
        offY -= (float)(IsoCamera.getOffscreenHeight(0) / 2);
        offY -= player.getOffsetY() * 1.5f;
        camera.offX = offX += (float)IsoCamera.playerOffsetX;
        camera.offY = offY += (float)IsoCamera.playerOffsetY;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        IsoCamera.FrameState frameState = IsoCamera.frameState;
        frameState.paused = false;
        frameState.playerIndex = 0;
        frameState.camCharacter = player;
        frameState.camCharacterX = isoGameCharacter.getX();
        frameState.camCharacterY = isoGameCharacter.getY();
        frameState.camCharacterZ = isoGameCharacter.getZ();
        frameState.camCharacterSquare = isoGameCharacter.getCurrentSquare();
        frameState.camCharacterRoom = frameState.camCharacterSquare == null ? null : frameState.camCharacterSquare.getRoom();
        frameState.offX = IsoCamera.getOffX();
        frameState.offY = IsoCamera.getOffY();
        frameState.offscreenWidth = IsoCamera.getOffscreenWidth(0);
        frameState.offscreenHeight = IsoCamera.getOffscreenHeight(0);
    }

    private static void renderWorld() {
        IsoObject obj;
        IsoPlayer player = ServerGUI.getPlayerToFollow();
        if (player == null) {
            return;
        }
        boolean playerIndex = false;
        IsoPlayer.setInstance(player);
        IsoPlayer.players[0] = player;
        IsoCamera.setCameraCharacter(player);
        ServerGUI.updateCamera(player);
        SpriteRenderer.instance.doCoreIntParam(0, player.getX());
        SpriteRenderer.instance.doCoreIntParam(1, player.getY());
        SpriteRenderer.instance.doCoreIntParam(2, player.getZ());
        IsoWorld.instance.sceneCullZombies();
        IsoWorld.instance.sceneCullAnimals();
        IsoSprite.globalOffsetX = -1.0f;
        IsoCell cell = IsoWorld.instance.currentCell;
        try {
            WeatherFxMask.initMask();
            DeadBodyAtlas.instance.render();
            WorldItemAtlas.instance.render();
            cell.render();
            DeadBodyAtlas.instance.renderDebug();
            WorldSoundManager.instance.render();
            WorldFlares.debugRender();
            WorldMarkers.instance.debugRender();
            ObjectAmbientEmitters.getInstance().render();
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderAreaHighlights.getInstance().render();
            }
            ParameterInside.renderDebug();
            LineDrawer.render();
            SkyBox.getInstance().render();
        }
        catch (Throwable t) {
            ExceptionLogger.logException(t);
        }
        boolean x1 = false;
        boolean y1 = false;
        int x2 = 0 + IsoCamera.getOffscreenWidth(0);
        int y2 = 0 + IsoCamera.getOffscreenHeight(0);
        float topLeftX = IsoUtils.XToIso(0.0f, 0.0f, 0.0f);
        float topRightY = IsoUtils.YToIso(x2, 0.0f, 0.0f);
        float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0f);
        float bottomLeftY = IsoUtils.YToIso(0.0f, y2, 6.0f);
        minY = (int)topRightY;
        maxY = (int)bottomLeftY;
        minX = (int)topLeftX;
        maxX = (int)bottomRightX;
        minX -= 2;
        minY -= 2;
        maxZ = (int)player.getZ();
        IsoCell cell2 = IsoWorld.instance.currentCell;
        cell2.DrawStencilMask();
        IsoObjectPicker.Instance.StartRender();
        ServerGUI.RenderTiles();
        for (int n = 0; n < cell2.getObjectList().size(); ++n) {
            obj = cell2.getObjectList().get(n);
            ((IsoMovingObject)obj).renderlast();
        }
        for (int i = 0; i < cell2.getStaticUpdaterObjectList().size(); ++i) {
            obj = cell2.getStaticUpdaterObjectList().get(i);
            obj.renderlast();
        }
        if (WorldSimulation.instance.created) {
            TextureDraw.GenericDrawer drawer = WorldSimulation.getDrawer(0);
            SpriteRenderer.instance.drawGeneric(drawer);
        }
        WorldSoundManager.instance.render();
        LineDrawer.clear();
    }

    private static void RenderTiles() {
        IsoCell.PerPlayerRender perPlayerRender;
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean playerIndex = false;
        if (IsoCell.perPlayerRender[0] == null) {
            IsoCell.perPlayerRender[0] = new IsoCell.PerPlayerRender();
        }
        if ((perPlayerRender = IsoCell.perPlayerRender[0]) == null) {
            IsoCell.perPlayerRender[0] = new IsoCell.PerPlayerRender();
        }
        perPlayerRender.setSize(maxX - minX + 1, maxY - minY + 1);
        for (int zza = 0; zza <= maxZ; ++zza) {
            int i;
            GridStack.clear();
            for (int n = minY; n < maxY; ++n) {
                int x = minX;
                IsoGridSquare square = ServerMap.instance.getGridSquare(x, n, zza);
                IsoDirections navStepDirIdx = IsoDirections.E;
                while (x < maxX) {
                    if (square != null && square.getY() != n) {
                        square = null;
                    }
                    if (square == null && (square = ServerMap.instance.getGridSquare(x, n, zza)) == null) {
                        ++x;
                        continue;
                    }
                    IsoChunk c = square.getChunk();
                    if (c != null && square.IsOnScreen()) {
                        GridStack.add(square);
                    }
                    square = square.getAdjacentSquare(navStepDirIdx);
                    ++x;
                }
            }
            SolidFloor.clear();
            VegetationCorpses.clear();
            MinusFloorCharacters.clear();
            for (i = 0; i < GridStack.size(); ++i) {
                IsoGridSquare square = GridStack.get(i);
                square.setLightInfoServerGUIOnly(defColorInfo);
                int flags = ServerGUI.renderFloor(square);
                if (!square.getStaticMovingObjects().isEmpty()) {
                    flags |= 2;
                }
                for (int m = 0; m < square.getMovingObjects().size(); ++m) {
                    IsoMovingObject mov = square.getMovingObjects().get(m);
                    boolean bOnFloor = mov.isOnFloor();
                    if (bOnFloor && mov instanceof IsoZombie) {
                        IsoZombie zombie = (IsoZombie)mov;
                        boolean bl = bOnFloor = zombie.crawling || zombie.legsSprite.currentAnim != null && zombie.legsSprite.currentAnim.name.equals("ZombieDeath") && zombie.def.isFinished();
                    }
                    if (bOnFloor) {
                        flags |= 2;
                        continue;
                    }
                    flags |= 4;
                }
                if ((flags & 1) != 0) {
                    SolidFloor.add(square);
                }
                if ((flags & 2) != 0) {
                    VegetationCorpses.add(square);
                }
                if ((flags & 4) == 0) continue;
                MinusFloorCharacters.add(square);
            }
            LuaEventManager.triggerEvent("OnPostFloorLayerDraw", zza);
            for (i = 0; i < VegetationCorpses.size(); ++i) {
                IsoGridSquare square = VegetationCorpses.get(i);
                ServerGUI.renderMinusFloor(square, false, true);
                ServerGUI.renderCharacters(square, true);
            }
            for (i = 0; i < MinusFloorCharacters.size(); ++i) {
                IsoGridSquare square = MinusFloorCharacters.get(i);
                boolean hasSE = ServerGUI.renderMinusFloor(square, false, false);
                ServerGUI.renderCharacters(square, false);
                if (!hasSE) continue;
                ServerGUI.renderMinusFloor(square, true, false);
            }
        }
        MinusFloorCharacters.clear();
        SolidFloor.clear();
        VegetationCorpses.clear();
    }

    private static int renderFloor(IsoGridSquare square) {
        int flags = 0;
        boolean playerIndex = false;
        for (int i = 0; i < square.getObjects().size(); ++i) {
            IsoObject obj = square.getObjects().get(i);
            boolean bDoIt = true;
            if (obj.sprite != null && !obj.sprite.properties.has(IsoFlagType.solidfloor)) {
                bDoIt = false;
                flags |= 4;
            }
            if (bDoIt) {
                IndieGL.glAlphaFunc(516, 0.0f);
                obj.setAlphaAndTarget(0, 1.0f);
                obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
                obj.renderObjectPicker(square.x, square.y, square.z, defColorInfo);
                if (obj.isHighlightRenderOnce()) {
                    obj.setHighlighted(false, false);
                }
                flags |= 1;
            }
            if (bDoIt || obj.sprite == null || !obj.sprite.properties.has(IsoFlagType.canBeRemoved) && !obj.sprite.properties.has(IsoFlagType.attachedFloor)) continue;
            flags |= 2;
        }
        return flags;
    }

    private static boolean isSpriteOnSouthOrEastWall(IsoObject obj) {
        if (obj instanceof IsoBarricade) {
            return obj.getDir() == IsoDirections.S || obj.getDir() == IsoDirections.E;
        }
        if (obj instanceof IsoCurtain) {
            IsoCurtain curtain = (IsoCurtain)obj;
            return curtain.getType() == IsoObjectType.curtainS || curtain.getType() == IsoObjectType.curtainE;
        }
        PropertyContainer properties = obj.getProperties();
        return properties != null && (properties.has(IsoFlagType.attachedE) || properties.has(IsoFlagType.attachedS));
    }

    private static int DoWallLightingN(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static int DoWallLightingW(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static int DoWallLightingNW(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static boolean renderMinusFloor(IsoGridSquare square, boolean doSE, boolean vegitationRender) {
        int start = doSE ? square.getObjects().size() - 1 : 0;
        int end = doSE ? 0 : square.getObjects().size() - 1;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        IsoRoom camCharacterRoom = IsoCamera.frameState.camCharacterRoom;
        boolean bCouldSee = true;
        float darkMulti = 1.0f;
        int sx = (int)(IsoUtils.XToScreenInt(square.x, square.y, square.z, 0) - IsoCamera.frameState.offX);
        int sy = (int)(IsoUtils.YToScreenInt(square.x, square.y, square.z, 0) - IsoCamera.frameState.offY);
        boolean bInStencilRect = true;
        IsoCell cell = square.getCell();
        if (sx + 32 * Core.tileScale <= cell.stencilX1 || sx - 32 * Core.tileScale >= cell.stencilX2 || sy + 32 * Core.tileScale <= cell.stencilY1 || sy - 96 * Core.tileScale >= cell.stencilY2) {
            bInStencilRect = false;
        }
        int stenciled = 0;
        boolean hasSE = false;
        int n = start;
        while (doSE ? n >= end : n <= end) {
            IsoObject obj = square.getObjects().get(n);
            boolean bDoIt = true;
            IsoGridSquare.circleStencil = false;
            if (obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                bDoIt = false;
            }
            if ((!vegitationRender || obj.sprite == null || obj.sprite.properties.has(IsoFlagType.canBeRemoved) || obj.sprite.properties.has(IsoFlagType.attachedFloor)) && (vegitationRender || obj.sprite == null || !obj.sprite.properties.has(IsoFlagType.canBeRemoved) && !obj.sprite.properties.has(IsoFlagType.attachedFloor))) {
                IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
                if (obj.sprite != null && (obj.sprite.getTileType() == IsoObjectType.WestRoofB || obj.sprite.getTileType() == IsoObjectType.WestRoofM || obj.sprite.getTileType() == IsoObjectType.WestRoofT) && square.z == maxZ && square.z == isoGameCharacter.getZi()) {
                    bDoIt = false;
                }
                if (isoGameCharacter.isClimbing() && obj.sprite != null && !obj.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                    bDoIt = true;
                }
                if (ServerGUI.isSpriteOnSouthOrEastWall(obj)) {
                    if (!doSE) {
                        bDoIt = false;
                    }
                    hasSE = true;
                } else if (doSE) {
                    bDoIt = false;
                }
                if (bDoIt) {
                    IndieGL.glAlphaFunc(516, 0.0f);
                    if (obj.sprite != null && !square.getProperties().has(IsoFlagType.blueprint) && (obj.sprite.getTileType() == IsoObjectType.doorFrW || obj.sprite.getTileType() == IsoObjectType.doorFrN || obj.sprite.getTileType() == IsoObjectType.doorW || obj.sprite.getTileType() == IsoObjectType.doorN || obj.sprite.getProperties().has(IsoFlagType.cutW) || obj.sprite.getProperties().has(IsoFlagType.cutN))) {
                        if (obj.getTargetAlpha(playerIndex) < 1.0f) {
                            boolean bForceNoStencil = false;
                            if (bForceNoStencil) {
                                IsoGridSquare toNorth;
                                if (obj.sprite.getProperties().has(IsoFlagType.cutW) && square.getProperties().has(IsoFlagType.WallSE)) {
                                    IsoGridSquare toNorthWest = square.getAdjacentSquare(IsoDirections.NW);
                                    if (toNorthWest == null || toNorthWest.getRoom() == null) {
                                        bForceNoStencil = false;
                                    }
                                } else if (obj.sprite.getTileType() == IsoObjectType.doorFrW || obj.sprite.getTileType() == IsoObjectType.doorW || obj.sprite.getProperties().has(IsoFlagType.cutW)) {
                                    IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                                    if (toWest == null || toWest.getRoom() == null) {
                                        bForceNoStencil = false;
                                    }
                                } else if (!(obj.sprite.getTileType() != IsoObjectType.doorFrN && obj.sprite.getTileType() != IsoObjectType.doorN && !obj.sprite.getProperties().has(IsoFlagType.cutN) || (toNorth = square.getAdjacentSquare(IsoDirections.N)) != null && toNorth.getRoom() != null)) {
                                    bForceNoStencil = false;
                                }
                            }
                            if (!bForceNoStencil) {
                                IsoGridSquare.circleStencil = bInStencilRect;
                            }
                            obj.setAlphaAndTarget(playerIndex, 1.0f);
                        }
                        if (obj.sprite.getProperties().has(IsoFlagType.cutW) && obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                            stenciled = ServerGUI.DoWallLightingNW(square, obj, stenciled);
                        } else if (obj.sprite.getTileType() == IsoObjectType.doorFrW || obj.sprite.getTileType() == IsoObjectType.doorW || obj.sprite.getProperties().has(IsoFlagType.cutW)) {
                            stenciled = ServerGUI.DoWallLightingW(square, obj, stenciled);
                        } else if (obj.sprite.getTileType() == IsoObjectType.doorFrN || obj.sprite.getTileType() == IsoObjectType.doorN || obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                            stenciled = ServerGUI.DoWallLightingN(square, obj, stenciled);
                        }
                    } else {
                        if (camCharacterSquare != null) {
                            // empty if block
                        }
                        obj.setTargetAlpha(playerIndex, 1.0f);
                        if (isoGameCharacter != null && obj.getProperties() != null && (obj.getProperties().has(IsoFlagType.solid) || obj.getProperties().has(IsoFlagType.solidtrans))) {
                            int dx = square.getX() - (int)isoGameCharacter.getX();
                            int dy = square.getY() - (int)isoGameCharacter.getY();
                            if (dx > 0 && dx < 3 && dy >= 0 && dy < 3 || dy > 0 && dy < 3 && dx >= 0 && dx < 3) {
                                obj.setTargetAlpha(playerIndex, 0.99f);
                            }
                        }
                        if (obj instanceof IsoWindow) {
                            IsoGridSquare oppositeSq;
                            IsoWindow w = (IsoWindow)obj;
                            if (obj.getTargetAlpha(playerIndex) < 1.0E-4f && (oppositeSq = w.getOppositeSquare()) != null && oppositeSq != square && oppositeSq.lighting[playerIndex].bSeen()) {
                                obj.setTargetAlpha(playerIndex, oppositeSq.lighting[playerIndex].darkMulti() * 2.0f);
                            }
                        }
                        if (obj instanceof IsoTree) {
                            IsoTree isoTree = (IsoTree)obj;
                            isoTree.renderFlag = bInStencilRect && square.x >= (int)IsoCamera.frameState.camCharacterX && square.y >= (int)IsoCamera.frameState.camCharacterY && camCharacterSquare != null && camCharacterSquare.has(IsoFlagType.exterior);
                        }
                        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
                    }
                    if (obj.sprite != null) {
                        obj.renderObjectPicker(square.x, square.y, square.z, defColorInfo);
                    }
                    if (obj.isHighlightRenderOnce()) {
                        obj.setHighlighted(false, false);
                    }
                }
            }
            n += doSE ? -1 : 1;
        }
        return hasSE;
    }

    private static void renderCharacters(IsoGridSquare square, boolean deadRender) {
        IsoMovingObject mov;
        int n;
        int size = square.getStaticMovingObjects().size();
        for (n = 0; n < size; ++n) {
            mov = square.getStaticMovingObjects().get(n);
            if (mov.sprite == null || deadRender && !(mov instanceof IsoDeadBody) || !deadRender && mov instanceof IsoDeadBody) continue;
            mov.render(mov.getX(), mov.getY(), mov.getZ(), defColorInfo, true, false, null);
            mov.renderObjectPicker(mov.getX(), mov.getY(), mov.getZ(), defColorInfo);
        }
        size = square.getMovingObjects().size();
        for (n = 0; n < size; ++n) {
            mov = square.getMovingObjects().get(n);
            if (mov == null || mov.sprite == null) continue;
            boolean bOnFloor = mov.isOnFloor();
            if (bOnFloor && mov instanceof IsoZombie) {
                IsoZombie zombie = (IsoZombie)mov;
                boolean bl = bOnFloor = zombie.crawling || zombie.legsSprite.currentAnim != null && zombie.legsSprite.currentAnim.name.equals("ZombieDeath") && zombie.def.isFinished();
            }
            if (deadRender && !bOnFloor || !deadRender && bOnFloor) continue;
            mov.setAlphaAndTarget(0, 1.0f);
            if (mov instanceof IsoGameCharacter) {
                IsoGameCharacter chr = (IsoGameCharacter)mov;
                chr.renderServerGUI();
            } else {
                mov.render(mov.getX(), mov.getY(), mov.getZ(), defColorInfo, true, false, null);
            }
            mov.renderObjectPicker(mov.getX(), mov.getY(), mov.getZ(), defColorInfo);
        }
    }

    private static void renderUI() {
    }

    static {
        GridStack = new ArrayList();
        MinusFloorCharacters = new ArrayList(1000);
        SolidFloor = new ArrayList(5000);
        VegetationCorpses = new ArrayList(5000);
        defColorInfo = new ColorInfo();
    }
}

