/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.ChunkMapFilenames;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.GameEntityFactory;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoLot;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.MapFiles;
import zombie.iso.ObjectCache;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.WorldStreamer;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class CellLoader {
    public static final ObjectCache<IsoObject> isoObjectCache = new ObjectCache();
    public static final ObjectCache<IsoTree> isoTreeCache = new ObjectCache();
    private static final HashSet<String> missingTiles = new HashSet();
    public static final HashMap<IsoSprite, IsoSprite> glassRemovedWindowSpriteMap = new HashMap();
    public static final HashMap<IsoSprite, IsoSprite> smashedWindowSpriteMap = new HashMap();

    public static void DoTileObjectCreation(IsoSprite spr, IsoObjectType type, IsoGridSquare sq, IsoCell cell, int x, int y, int height, String name) throws NumberFormatException {
        IsoObject obj;
        block44: {
            IsoObject obj2;
            IsoObject floor;
            block61: {
                block63: {
                    block65: {
                        block64: {
                            block62: {
                                block60: {
                                    block59: {
                                        block58: {
                                            block57: {
                                                boolean isStove;
                                                block56: {
                                                    block55: {
                                                        block54: {
                                                            block53: {
                                                                block52: {
                                                                    block51: {
                                                                        block50: {
                                                                            block49: {
                                                                                block48: {
                                                                                    block47: {
                                                                                        boolean bSmashedWindow;
                                                                                        boolean bGlassRemovedWindow;
                                                                                        block46: {
                                                                                            block45: {
                                                                                                block43: {
                                                                                                    obj = null;
                                                                                                    if (sq == null) {
                                                                                                        return;
                                                                                                    }
                                                                                                    bGlassRemovedWindow = false;
                                                                                                    bSmashedWindow = false;
                                                                                                    if (glassRemovedWindowSpriteMap.containsKey(spr)) {
                                                                                                        spr = glassRemovedWindowSpriteMap.get(spr);
                                                                                                        type = spr.getTileType();
                                                                                                        bGlassRemovedWindow = true;
                                                                                                    } else if (smashedWindowSpriteMap.containsKey(spr)) {
                                                                                                        spr = smashedWindowSpriteMap.get(spr);
                                                                                                        type = spr.getTileType();
                                                                                                        bSmashedWindow = true;
                                                                                                    }
                                                                                                    PropertyContainer props = spr.getProperties();
                                                                                                    boolean bl = isStove = spr.getProperties().propertyEquals(IsoPropertyType.CONTAINER, "stove") || spr.getProperties().propertyEquals(IsoPropertyType.CONTAINER, "toaster") || spr.getProperties().propertyEquals(IsoPropertyType.CONTAINER, "coffeemaker");
                                                                                                    if (spr.solidfloor && props.has(IsoFlagType.diamondFloor) && !props.has(IsoFlagType.transparentFloor) && (floor = sq.getFloor()) != null && floor.getProperties().has(IsoFlagType.diamondFloor)) {
                                                                                                        if (floor.hasComponents()) {
                                                                                                            if (floor.isAddedToEngine()) {
                                                                                                                throw new IllegalStateException("entity was added to engine");
                                                                                                            }
                                                                                                            for (int i = floor.componentSize() - 1; i >= 0; --i) {
                                                                                                                Component component = floor.getComponentForIndex(i);
                                                                                                                GameEntityFactory.RemoveComponent(floor, component);
                                                                                                            }
                                                                                                            floor.removeFromSquare();
                                                                                                        } else {
                                                                                                            floor.clearAttachedAnimSprite();
                                                                                                            floor.setSprite(spr);
                                                                                                            return;
                                                                                                        }
                                                                                                    }
                                                                                                    if (type != IsoObjectType.doorW && type != IsoObjectType.doorN) break block43;
                                                                                                    IsoDoor door = new IsoDoor(cell, sq, spr, type == IsoObjectType.doorN);
                                                                                                    obj = door;
                                                                                                    CellLoader.AddSpecialObject(sq, obj);
                                                                                                    if (spr.getProperties().has(IsoPropertyType.DOUBLE_DOOR)) {
                                                                                                        door.locked = false;
                                                                                                        door.lockedByKey = false;
                                                                                                    }
                                                                                                    if (spr.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
                                                                                                        door.locked = !door.IsOpen();
                                                                                                        door.lockedByKey = false;
                                                                                                    }
                                                                                                    break block44;
                                                                                                }
                                                                                                if (type != IsoObjectType.lightswitch) break block45;
                                                                                                obj = new IsoLightSwitch(cell, sq, spr, sq.getRoomID());
                                                                                                CellLoader.AddObject(sq, obj);
                                                                                                if (obj.sprite.getProperties().has(IsoPropertyType.RED_LIGHT)) {
                                                                                                    float r = Float.parseFloat(obj.sprite.getProperties().get(IsoPropertyType.RED_LIGHT)) / 255.0f;
                                                                                                    float g = Float.parseFloat(obj.sprite.getProperties().get(IsoPropertyType.GREEN_LIGHT)) / 255.0f;
                                                                                                    float b = Float.parseFloat(obj.sprite.getProperties().get(IsoPropertyType.BLUE_LIGHT)) / 255.0f;
                                                                                                    int radius = 10;
                                                                                                    if (obj.sprite.getProperties().has(IsoPropertyType.LIGHT_RADIUS) && Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS)) > 0) {
                                                                                                        radius = Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.LIGHT_RADIUS));
                                                                                                    }
                                                                                                    IsoLightSource l = new IsoLightSource(obj.square.getX(), obj.square.getY(), obj.square.getZ(), r, g, b, radius);
                                                                                                    l.active = true;
                                                                                                    l.hydroPowered = true;
                                                                                                    l.switches.add((IsoLightSwitch)obj);
                                                                                                    ((IsoLightSwitch)obj).lights.add(l);
                                                                                                } else {
                                                                                                    ((IsoLightSwitch)obj).lightRoom = true;
                                                                                                }
                                                                                                break block44;
                                                                                            }
                                                                                            if (type != IsoObjectType.curtainN && type != IsoObjectType.curtainS && type != IsoObjectType.curtainE && type != IsoObjectType.curtainW) break block46;
                                                                                            boolean closed = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1)) % 8 <= 3;
                                                                                            obj = new IsoCurtain(cell, sq, spr, type == IsoObjectType.curtainN || type == IsoObjectType.curtainS, closed);
                                                                                            CellLoader.AddSpecialObject(sq, obj);
                                                                                            break block44;
                                                                                        }
                                                                                        if (!spr.getProperties().has(IsoFlagType.windowW) && !spr.getProperties().has(IsoFlagType.windowN)) break block47;
                                                                                        obj = new IsoWindow(cell, sq, spr, spr.getProperties().has(IsoFlagType.windowN));
                                                                                        if (bGlassRemovedWindow) {
                                                                                            ((IsoWindow)obj).setSmashed(true);
                                                                                            ((IsoWindow)obj).setGlassRemoved(true);
                                                                                        } else if (bSmashedWindow) {
                                                                                            ((IsoWindow)obj).setSmashed(true);
                                                                                        }
                                                                                        CellLoader.AddSpecialObject(sq, obj);
                                                                                        break block44;
                                                                                    }
                                                                                    if (!spr.getProperties().has(IsoFlagType.WindowW) && !spr.getProperties().has(IsoFlagType.WindowN)) break block48;
                                                                                    obj = new IsoWindowFrame(cell, sq, spr, spr.getProperties().has(IsoFlagType.WindowN));
                                                                                    CellLoader.AddObject(sq, obj);
                                                                                    break block44;
                                                                                }
                                                                                if (!spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("barbecue") && !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("barbecuepropane")) break block49;
                                                                                obj = new IsoBarbecue(cell, sq, spr);
                                                                                CellLoader.AddObject(sq, obj);
                                                                                break block44;
                                                                            }
                                                                            if (!spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("fireplace")) break block50;
                                                                            obj = new IsoFireplace(cell, sq, spr);
                                                                            CellLoader.AddObject(sq, obj);
                                                                            break block44;
                                                                        }
                                                                        if (spr.getName().equals("camping_01_04") || spr.getName().equals("camping_01_05") || spr.getName().equals("camping_01_06") || !spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("campfire")) break block51;
                                                                        obj = new IsoFireplace(cell, sq, spr);
                                                                        CellLoader.AddObject(sq, obj);
                                                                        break block44;
                                                                    }
                                                                    if (!"brazier".equals(spr.getProperties().get(IsoPropertyType.CONTAINER))) break block52;
                                                                    obj = new IsoFireplace(cell, sq, spr);
                                                                    CellLoader.AddObject(sq, obj);
                                                                    break block44;
                                                                }
                                                                if (!"IsoCombinationWasherDryer".equals(spr.getProperties().get(IsoPropertyType.ISO_TYPE))) break block53;
                                                                obj = new IsoCombinationWasherDryer(cell, sq, spr);
                                                                CellLoader.AddObject(sq, obj);
                                                                break block44;
                                                            }
                                                            if (!spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("clothingdryer")) break block54;
                                                            obj = new IsoClothingDryer(cell, sq, spr);
                                                            CellLoader.AddObject(sq, obj);
                                                            break block44;
                                                        }
                                                        if (!spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("clothingwasher")) break block55;
                                                        obj = new IsoClothingWasher(cell, sq, spr);
                                                        CellLoader.AddObject(sq, obj);
                                                        break block44;
                                                    }
                                                    if (!spr.getProperties().has(IsoFlagType.container) || !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("woodstove")) break block56;
                                                    obj = new IsoFireplace(cell, sq, spr);
                                                    CellLoader.AddObject(sq, obj);
                                                    break block44;
                                                }
                                                if (!spr.getProperties().has(IsoFlagType.container) || !isStove && !spr.getProperties().get(IsoPropertyType.CONTAINER).equals("microwave")) break block57;
                                                obj = new IsoStove(cell, sq, spr);
                                                CellLoader.AddObject(sq, obj);
                                                break block44;
                                            }
                                            if (type != IsoObjectType.jukebox) break block58;
                                            obj = new IsoJukebox(cell, sq, spr);
                                            obj.outlineOnMouseover = true;
                                            CellLoader.AddObject(sq, obj);
                                            break block44;
                                        }
                                        if (type != IsoObjectType.radio) break block59;
                                        obj = new IsoRadio(cell, sq, spr);
                                        CellLoader.AddObject(sq, obj);
                                        break block44;
                                    }
                                    if (!spr.getProperties().has("signal")) break block60;
                                    String signaltype = spr.getProperties().get("signal");
                                    if ("radio".equals(signaltype)) {
                                        obj = new IsoRadio(cell, sq, spr);
                                    } else if ("tv".equals(signaltype)) {
                                        obj = new IsoTelevision(cell, sq, spr);
                                    }
                                    CellLoader.AddObject(sq, obj);
                                    break block44;
                                }
                                if (!spr.getProperties().has(IsoFlagType.WallOverlay)) break block61;
                                obj2 = null;
                                if (!spr.getProperties().has(IsoFlagType.attachedSE)) break block62;
                                obj2 = sq.getWallSE();
                                break block63;
                            }
                            if (!spr.getProperties().has(IsoFlagType.attachedW)) break block64;
                            obj2 = sq.getWall(false);
                            if (!(obj2 != null || (obj2 = sq.getWindow(false)) == null || obj2.getProperties() != null && obj2.getProperties().has(IsoFlagType.WindowW))) {
                                obj2 = null;
                            }
                            if (obj2 != null) break block63;
                            obj2 = sq.getGarageDoor(false);
                            break block63;
                        }
                        if (!spr.getProperties().has(IsoFlagType.attachedN)) break block65;
                        obj2 = sq.getWall(true);
                        if (!(obj2 != null || (obj2 = sq.getWindow(true)) == null || obj2.getProperties() != null && obj2.getProperties().has(IsoFlagType.WindowN))) {
                            obj2 = null;
                        }
                        if (obj2 != null) break block63;
                        obj2 = sq.getGarageDoor(true);
                        break block63;
                    }
                    for (int n = sq.getObjects().size() - 1; n >= 0; --n) {
                        IsoObject obj3 = sq.getObjects().get(n);
                        if (!obj3.sprite.getProperties().has(IsoFlagType.cutW) && !obj3.sprite.getProperties().has(IsoFlagType.cutN)) continue;
                        obj2 = obj3;
                        break;
                    }
                }
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList(4);
                    }
                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(spr));
                } else {
                    obj = IsoObject.getNew();
                    obj.sx = 0.0f;
                    obj.sprite = spr;
                    obj.square = sq;
                    CellLoader.AddObject(sq, obj);
                }
                return;
            }
            if (spr.getProperties().has(IsoFlagType.FloorOverlay)) {
                obj2 = sq.getFloor();
                if (obj2 != null) {
                    if (obj2.attachedAnimSprite == null) {
                        obj2.attachedAnimSprite = new ArrayList(4);
                    }
                    obj2.attachedAnimSprite.add(IsoSpriteInstance.get(spr));
                }
            } else if (IsoMannequin.isMannequinSprite(spr)) {
                obj = new IsoMannequin(cell, sq, spr);
                CellLoader.AddObject(sq, obj);
            } else if (type == IsoObjectType.tree) {
                if (spr.getName() != null && spr.getName().startsWith("vegetation_trees") && ((floor = sq.getFloor()) == null || floor.getSprite() == null || floor.getSprite().getName() == null || !floor.getSprite().getName().startsWith("blends_natural"))) {
                    DebugLog.General.error("removed tree at " + sq.x + "," + sq.y + "," + sq.z + " because floor is not blends_natural");
                    return;
                }
                obj = IsoTree.getNew();
                obj.sprite = spr;
                obj.square = sq;
                obj.sx = 0.0f;
                ((IsoTree)obj).initTree();
                for (int i = 0; i < sq.getObjects().size(); ++i) {
                    IsoObject obj22 = sq.getObjects().get(i);
                    if (!(obj22 instanceof IsoTree)) continue;
                    IsoTree isoTree = (IsoTree)obj22;
                    sq.getObjects().remove(i);
                    obj22.reset();
                    isoTreeCache.push(isoTree);
                    break;
                }
                CellLoader.AddObject(sq, obj);
            } else {
                if (spr.hasNoTextures() && !spr.invisible && !GameServer.server) {
                    if (!missingTiles.contains(name)) {
                        if (Core.debug) {
                            DebugLog.General.error("CellLoader> missing tile " + name);
                        }
                        missingTiles.add(name);
                    }
                    spr.LoadSingleTexture(Core.debug ? "media/ui/missing-tile-debug.png" : "media/ui/missing-tile.png");
                    if (spr.hasNoTextures()) {
                        return;
                    }
                }
                obj = IsoObject.getNew();
                obj.sx = 0.0f;
                obj.sprite = spr;
                obj.square = sq;
                CellLoader.AddObject(sq, obj);
            }
        }
        if (obj != null) {
            IsoWaveSignal isoWaveSignal;
            obj.setTile(name);
            obj.createContainersFromSpriteProperties();
            obj.createFluidContainersFromSpriteProperties();
            if (obj.sprite.getProperties().has(IsoFlagType.vegitation)) {
                obj.tintr = 0.7f + (float)Rand.Next(30) / 100.0f;
                obj.tintg = 0.7f + (float)Rand.Next(30) / 100.0f;
                obj.tintb = 0.7f + (float)Rand.Next(30) / 100.0f;
            }
            if (sq.shouldNotSpawnActivatedRadiosOrTvs() && obj instanceof IsoWaveSignal && (isoWaveSignal = (IsoWaveSignal)obj).getDeviceData().getIsTurnedOn()) {
                isoWaveSignal.getDeviceData().setTurnedOnRaw(false);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean LoadCellBinaryChunk(IsoCell cell, int wx, int wy, IsoChunk chunk) {
        int nonEmptySquareCount;
        boolean[] bDoneSquares;
        IsoLot lot;
        String filenamepack;
        int cellY;
        int cellX;
        block11: {
            LotHeader lotHeader;
            block10: {
                cellX = PZMath.fastfloor((float)wx / 32.0f);
                filenamepack = "world_" + cellX + "_" + (cellY = PZMath.fastfloor((float)wy / 32.0f)) + ".lotpack";
                if (!IsoLot.InfoFileNames.containsKey(filenamepack)) {
                    return false;
                }
                File fo = new File(IsoLot.InfoFileNames.get(filenamepack));
                if (!fo.exists()) return false;
                lot = null;
                lotHeader = IsoLot.InfoHeaders.get(ChunkMapFilenames.instance.getHeader(cellX, cellY));
                lot = IsoLot.get(lotHeader.mapFiles, cellX, cellY, wx, wy, chunk);
                if (lot.info != null) break block10;
                boolean bl = true;
                if (lot == null) return bl;
                IsoLot.put(lot);
                return bl;
            }
            bDoneSquares = new boolean[64];
            nonEmptySquareCount = cell.PlaceLot(lot, 0, 0, lot.minLevel, chunk, wx, wy, bDoneSquares);
            if (nonEmptySquareCount > 0) {
                chunk.addModded(lotHeader.mapFiles.infoFileModded.get(filenamepack));
            }
            if (nonEmptySquareCount != 64) break block11;
            boolean bl = true;
            if (lot == null) return bl;
            IsoLot.put(lot);
            return bl;
        }
        try {
            for (int i = lot.info.mapFiles.priority + 1; i < IsoLot.MapFiles.size(); ++i) {
                IsoLot lot2;
                block12: {
                    MapFiles mapFiles = IsoLot.MapFiles.get(i);
                    if (!mapFiles.hasCell(cellX, cellY)) continue;
                    lot2 = null;
                    try {
                        lot2 = IsoLot.get(mapFiles, cellX, cellY, wx, wy, chunk);
                        nonEmptySquareCount = cell.PlaceLot(lot2, 0, 0, lot2.minLevel, chunk, wx, wy, bDoneSquares);
                        if (nonEmptySquareCount > 0) {
                            chunk.addModded(mapFiles.infoFileModded.get(filenamepack));
                        }
                        if (nonEmptySquareCount != 64) break block12;
                        if (lot2 == null) break;
                    }
                    catch (Throwable throwable) {
                        if (lot2 == null) throw throwable;
                        IsoLot.put(lot2);
                        throw throwable;
                    }
                    IsoLot.put(lot2);
                    break;
                }
                if (lot2 == null) continue;
                IsoLot.put(lot2);
            }
            if (lot == null) return true;
        }
        catch (Throwable throwable) {
            if (lot == null) throw throwable;
            IsoLot.put(lot);
            throw throwable;
        }
        IsoLot.put(lot);
        return true;
    }

    public static IsoCell LoadCellBinaryChunk(IsoSpriteManager spr, int wx, int wy) throws IOException {
        IsoCell cell = new IsoCell(256, 256);
        if (!GameServer.server) {
            if (GameClient.client) {
                WorldStreamer.instance.requestLargeAreaZip(wx, wy, IsoChunkMap.chunkGridWidth / 2 + 2);
                IsoChunk.doServerRequests = false;
            }
            for (int n = 0; n < 1; ++n) {
                cell.chunkMap[n].setInitialPos(wx, wy);
                IsoPlayer.assumedPlayer = n;
                int minwx = wx - IsoChunkMap.chunkGridWidth / 2;
                int minwy = wy - IsoChunkMap.chunkGridWidth / 2;
                int maxwx = wx + IsoChunkMap.chunkGridWidth / 2 + 1;
                int maxwy = wy + IsoChunkMap.chunkGridWidth / 2 + 1;
                for (int x = minwx; x < maxwx; ++x) {
                    for (int y = minwy; y < maxwy; ++y) {
                        if (!IsoWorld.instance.getMetaGrid().isValidChunk(x, y)) continue;
                        cell.chunkMap[n].LoadChunk(x, y, x - minwx, y - minwy);
                    }
                }
            }
        }
        IsoPlayer.assumedPlayer = 0;
        LuaEventManager.triggerEvent("OnPostMapLoad", cell, wx, wy);
        CellLoader.ConnectMultitileObjects(cell);
        return cell;
    }

    private static void RecurseMultitileObjects(IsoCell cell, IsoGridSquare from, IsoGridSquare sq, ArrayList<IsoPushableObject> list) {
        IsoMovingObject foundpush = null;
        boolean bFoundOnX = false;
        for (IsoMovingObject obj : sq.getMovingObjects()) {
            int offX;
            int offY;
            if (!(obj instanceof IsoPushableObject)) continue;
            IsoPushableObject push = (IsoPushableObject)obj;
            int difX = from.getX() - sq.getX();
            int difY = from.getY() - sq.getY();
            if (difY != 0 && obj.sprite.getProperties().has(IsoPropertyType.CONNECT_Y) && (offY = Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.CONNECT_Y))) == difY) {
                push.connectList = list;
                list.add(push);
                foundpush = push;
                break;
            }
            if (difX == 0 || !obj.sprite.getProperties().has(IsoPropertyType.CONNECT_X) || (offX = Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.CONNECT_X))) != difX) continue;
            push.connectList = list;
            list.add(push);
            foundpush = push;
            bFoundOnX = true;
            break;
        }
        if (foundpush != null) {
            IsoGridSquare other;
            if (((IsoPushableObject)foundpush).sprite.getProperties().has(IsoPropertyType.CONNECT_Y) && bFoundOnX) {
                int offY = Integer.parseInt(((IsoPushableObject)foundpush).sprite.getProperties().get(IsoPropertyType.CONNECT_Y));
                other = cell.getGridSquare(foundpush.getCurrentSquare().getX(), foundpush.getCurrentSquare().getY() + offY, foundpush.getCurrentSquare().getZ());
                CellLoader.RecurseMultitileObjects(cell, foundpush.getCurrentSquare(), other, ((IsoPushableObject)foundpush).connectList);
            }
            if (((IsoPushableObject)foundpush).sprite.getProperties().has(IsoPropertyType.CONNECT_X) && !bFoundOnX) {
                int offX = Integer.parseInt(((IsoPushableObject)foundpush).sprite.getProperties().get(IsoPropertyType.CONNECT_X));
                other = cell.getGridSquare(foundpush.getCurrentSquare().getX() + offX, foundpush.getCurrentSquare().getY(), foundpush.getCurrentSquare().getZ());
                CellLoader.RecurseMultitileObjects(cell, foundpush.getCurrentSquare(), other, ((IsoPushableObject)foundpush).connectList);
            }
        }
    }

    private static void ConnectMultitileObjects(IsoCell cell) {
        for (IsoMovingObject obj : cell.getObjectList()) {
            IsoGridSquare other;
            if (!(obj instanceof IsoPushableObject)) continue;
            IsoPushableObject push = (IsoPushableObject)obj;
            if (!obj.sprite.getProperties().has(IsoPropertyType.CONNECT_Y) && !obj.sprite.getProperties().has(IsoPropertyType.CONNECT_X) || push.connectList != null) continue;
            push.connectList = new ArrayList();
            push.connectList.add(push);
            if (obj.sprite.getProperties().has(IsoPropertyType.CONNECT_Y)) {
                int offY = Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.CONNECT_Y));
                other = cell.getGridSquare(obj.getCurrentSquare().getX(), obj.getCurrentSquare().getY() + offY, obj.getCurrentSquare().getZ());
                CellLoader.RecurseMultitileObjects(cell, push.getCurrentSquare(), other, push.connectList);
            }
            if (!obj.sprite.getProperties().has(IsoPropertyType.CONNECT_X)) continue;
            int offX = Integer.parseInt(obj.sprite.getProperties().get(IsoPropertyType.CONNECT_X));
            other = cell.getGridSquare(obj.getCurrentSquare().getX() + offX, obj.getCurrentSquare().getY(), obj.getCurrentSquare().getZ());
            CellLoader.RecurseMultitileObjects(cell, push.getCurrentSquare(), other, push.connectList);
        }
    }

    private static void AddObject(IsoGridSquare square, IsoObject obj) {
        GameEntityFactory.CreateIsoEntityFromCellLoading(obj);
        int index = square.placeWallAndDoorCheck(obj, square.getObjects().size());
        if (index != square.getObjects().size() && index >= 0 && index <= square.getObjects().size()) {
            square.getObjects().add(index, obj);
        } else {
            square.getObjects().add(obj);
        }
    }

    private static void AddSpecialObject(IsoGridSquare square, IsoObject obj) {
        GameEntityFactory.CreateIsoEntityFromCellLoading(obj);
        int index = square.placeWallAndDoorCheck(obj, square.getObjects().size());
        if (index != square.getObjects().size() && index >= 0 && index <= square.getObjects().size()) {
            square.getObjects().add(index, obj);
        } else {
            square.getObjects().add(obj);
            square.getSpecialObjects().add(obj);
        }
    }
}

