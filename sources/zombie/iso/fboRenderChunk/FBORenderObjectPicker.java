/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.entity.util.TimSort;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.ObjectRenderInfo;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayList;

public final class FBORenderObjectPicker {
    private static FBORenderObjectPicker instance;
    private final ObjectPool<IsoObjectPicker.ClickObject> clickObjectPool = new ObjectPool<IsoObjectPicker.ClickObject>(IsoObjectPicker.ClickObject::new);
    private final ArrayList<IsoObject> objects = new ArrayList();
    private final PZArrayList<IsoObjectPicker.ClickObject> clickObjects = new PZArrayList<IsoObjectPicker.ClickObject>(IsoObjectPicker.ClickObject.class, 128);
    private final PZArrayList<IsoObjectPicker.ClickObject> choices = new PZArrayList<IsoObjectPicker.ClickObject>(IsoObjectPicker.ClickObject.class, 32);
    private final TimSort timSort = new TimSort();
    private final int[] leftSideXy = new int[]{0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3};
    private final int[] rightSideXy = new int[]{0, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3};

    public static FBORenderObjectPicker getInstance() {
        if (instance == null) {
            instance = new FBORenderObjectPicker();
        }
        return instance;
    }

    public IsoObjectPicker.ClickObject ContextPick(int screenX, int screenY) {
        boolean playerIndex = false;
        float zoom = Core.getInstance().getZoom(0);
        float x = (float)screenX * zoom;
        float y = (float)screenY * zoom;
        this.clickObjectPool.releaseAll((List<IsoObjectPicker.ClickObject>)this.clickObjects);
        this.clickObjects.clear();
        this.getClickObjects(screenX, screenY, this.clickObjects);
        this.choices.clear();
        for (int n = this.clickObjects.size() - 1; n >= 0; --n) {
            float targetAlpha;
            IsoSprite sprite;
            IsoObjectPicker.ClickObject clickObject = this.clickObjects.get(n);
            IsoObject object = clickObject.tile;
            if (object instanceof IsoPlayer && object == IsoPlayer.players[0] || (sprite = object.sprite) != null && ((targetAlpha = object.getRenderInfo((int)0).targetAlpha) == 0.0f || this.shouldIgnoreWallLikeObject(object, sprite, targetAlpha)) || !(x > (float)clickObject.x) || !(y > (float)clickObject.y) || !(x <= (float)(clickObject.x + clickObject.width)) || !(y <= (float)(clickObject.y + clickObject.height)) || object instanceof IsoPlayer) continue;
            if (clickObject.scaleX != 1.0f || clickObject.scaleY != 1.0f) {
                float x1 = (float)clickObject.x + (x - (float)clickObject.x) / clickObject.scaleX;
                float y1 = (float)clickObject.y + (y - (float)clickObject.y) / clickObject.scaleY;
                if (!object.isMaskClicked((int)(x1 - (float)clickObject.x), (int)(y1 - (float)clickObject.y), clickObject.flip)) continue;
                if (object.rerouteMask != null) {
                    clickObject.tile = object.rerouteMask;
                }
                clickObject.lx = PZMath.fastfloor(x) - clickObject.x;
                clickObject.ly = PZMath.fastfloor(y) - clickObject.y;
                this.choices.add(clickObject);
                continue;
            }
            if (!object.isMaskClicked((int)(x - (float)clickObject.x), (int)(y - (float)clickObject.y), clickObject.flip)) continue;
            if (object.rerouteMask != null) {
                clickObject.tile = object.rerouteMask;
            }
            clickObject.lx = PZMath.fastfloor(x) - clickObject.x;
            clickObject.ly = PZMath.fastfloor(y) - clickObject.y;
            this.choices.add(clickObject);
        }
        if (this.choices.isEmpty()) {
            return null;
        }
        for (int i = 0; i < this.choices.size(); ++i) {
            IsoObjectPicker.ClickObject choice = this.choices.get(i);
            choice.score = choice.calculateScore();
        }
        try {
            this.timSort.doSort(this.choices.getElements(), IsoObjectPicker.comp, 0, this.choices.size());
        }
        catch (IllegalArgumentException ex) {
            if (Core.debug) {
                ExceptionLogger.logException(ex);
            }
            return null;
        }
        return this.choices.get(this.choices.size() - 1);
    }

    private boolean shouldIgnoreWallLikeObject(IsoObject object, IsoSprite sprite, float targetAlpha) {
        IsoThumpable isoThumpable;
        if (!sprite.hasProperty(IsoFlagType.cutW) && !sprite.hasProperty(IsoFlagType.cutN)) {
            return false;
        }
        if (object instanceof IsoWindow) {
            return false;
        }
        if (object instanceof IsoThumpable && (isoThumpable = (IsoThumpable)object).isDoor()) {
            return false;
        }
        if (object instanceof IsoDoor) {
            return false;
        }
        return targetAlpha < 1.0f;
    }

    private boolean shouldIgnoreCutawayWall(IsoObject object) {
        IsoThumpable thumpable;
        IsoWindowFrame windowFrame;
        if (object instanceof IsoWindowFrame && !(windowFrame = (IsoWindowFrame)object).hasWindow()) {
            return false;
        }
        if (object instanceof IsoThumpable && (thumpable = (IsoThumpable)object).isWindow() && object.getSquare().getWindow(thumpable.getNorth()) == null) {
            return false;
        }
        return !(object instanceof IsoDoor);
    }

    public void getClickObjects(int screenX, int screenY, PZArrayList<IsoObjectPicker.ClickObject> clickObjects) {
        boolean playerIndex = false;
        float zoom = Core.getInstance().getZoom(0);
        float x = (float)screenX * zoom;
        float y = (float)screenY * zoom;
        this.getObjectsAt((int)x, (int)y, this.objects);
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoObject object = this.objects.get(i);
            FBORenderLevels renderLevels = object.getSquare().getChunk().getRenderLevels(0);
            FBORenderChunk renderChunk = renderLevels.getFBOForLevel(object.getSquare().z, zoom);
            if (renderChunk == null || this.handleWaterShader(object, clickObjects)) continue;
            ObjectRenderInfo renderInfo = object.getRenderInfo(0);
            if (renderInfo.cutaway && this.shouldIgnoreCutawayWall(object)) continue;
            IsoObjectPicker.ClickObject clickObject = this.clickObjectPool.alloc();
            clickObject.tile = object;
            clickObject.square = object.getSquare();
            if (renderInfo.layer == ObjectRenderLayer.Translucent || renderInfo.layer == ObjectRenderLayer.TranslucentFloor) {
                clickObject.x = (int)renderInfo.renderX;
                clickObject.y = (int)renderInfo.renderY;
            } else if (DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
                clickObject.x = (int)(renderChunk.renderX + renderInfo.renderX);
                clickObject.y = (int)(renderChunk.renderY + renderInfo.renderY);
            } else {
                clickObject.x = (int)(renderChunk.renderX * zoom + renderInfo.renderX);
                clickObject.y = (int)(renderChunk.renderY * zoom + renderInfo.renderY);
            }
            clickObject.width = (int)renderInfo.renderWidth;
            clickObject.height = (int)renderInfo.renderHeight;
            clickObject.scaleX = (int)renderInfo.renderScaleX;
            clickObject.scaleY = (int)renderInfo.renderScaleY;
            clickObject.flip = false;
            clickObject.score = 0;
            clickObjects.add(clickObject);
        }
        this.timSort.doSort(clickObjects.getElements(), (o1, o2) -> {
            int c = o1.square.z - o2.square.z;
            if (c != 0) {
                return c;
            }
            c = FBORenderObjectPicker.compareRenderLayer(o1, o2);
            if (c != 0) {
                return c;
            }
            return FBORenderObjectPicker.compareSquare(o1, o2);
        }, 0, clickObjects.size());
    }

    boolean handleWaterShader(IsoObject object, PZArrayList<IsoObjectPicker.ClickObject> clickObjects) {
        boolean playerIndex = false;
        if (object.getRenderInfo((int)0).layer != ObjectRenderLayer.None) {
            return false;
        }
        if (object.sprite == null || !object.sprite.getProperties().has(IsoFlagType.water)) {
            return false;
        }
        IsoGridSquare square = object.square;
        IsoObjectPicker.ClickObject clickObject = this.clickObjectPool.alloc();
        clickObject.tile = object;
        clickObject.square = square;
        clickObject.x = (int)(IsoUtils.XToScreen(square.x, square.y, square.z, 0) - IsoCamera.frameState.offX - object.offsetX);
        clickObject.y = (int)(IsoUtils.YToScreen(square.x, square.y, square.z, 0) - IsoCamera.frameState.offY - object.offsetY);
        clickObject.width = 64 * Core.tileScale;
        clickObject.height = 128 * Core.tileScale;
        clickObject.scaleX = 1.0f;
        clickObject.scaleY = 1.0f;
        clickObject.flip = false;
        clickObject.score = 0;
        clickObjects.add(clickObject);
        return true;
    }

    static int compareRenderLayer(IsoObjectPicker.ClickObject o1, IsoObjectPicker.ClickObject o2) {
        return FBORenderObjectPicker.renderLayerIndex(o1) - FBORenderObjectPicker.renderLayerIndex(o2);
    }

    static int renderLayerIndex(IsoObjectPicker.ClickObject o) {
        return switch (o.tile.getRenderInfo((int)0).layer) {
            default -> throw new MatchException(null, null);
            case ObjectRenderLayer.None -> 1000;
            case ObjectRenderLayer.Floor, ObjectRenderLayer.TranslucentFloor -> 0;
            case ObjectRenderLayer.Vegetation -> 1;
            case ObjectRenderLayer.Corpse -> 2;
            case ObjectRenderLayer.MinusFloor, ObjectRenderLayer.Translucent -> 3;
            case ObjectRenderLayer.WorldInventoryObject -> 4;
            case ObjectRenderLayer.MinusFloorSE -> 5;
        };
    }

    static int compareSquare(IsoObjectPicker.ClickObject o1, IsoObjectPicker.ClickObject o2) {
        int index1 = o1.square.x + o1.square.y * 100000;
        int index2 = o2.square.x + o2.square.y * 100000;
        return index1 - index2;
    }

    void getObjectsAt(int screenX, int screenY, ArrayList<IsoObject> objects) {
        objects.clear();
        boolean playerIndex = false;
        IsoPlayer player = IsoPlayer.players[0];
        if (player.getZ() < 0.0f) {
            for (int z = -32; z < 0; ++z) {
                float worldY;
                float worldX = IsoUtils.XToIso(screenX, screenY, z);
                boolean bRightOfSquare = worldX % 1.0f > (worldY = IsoUtils.YToIso(screenX, screenY, z)) % 1.0f;
                int[] dxy = bRightOfSquare ? this.rightSideXy : this.leftSideXy;
                for (int i = 0; i < dxy.length; i += 2) {
                    this.getObjectsOnSquare((int)worldX + dxy[i], (int)worldY + dxy[i + 1], z, objects);
                }
                this.getCorpsesNear(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), z, objects, bRightOfSquare, screenX, screenY);
            }
            return;
        }
        for (int z = 0; z <= 31; ++z) {
            float worldX = IsoUtils.XToIso(screenX, screenY, z);
            float worldY = IsoUtils.YToIso(screenX, screenY, z);
            boolean bRightOfSquare = PZMath.coordmodulof(worldX, 1) > PZMath.coordmodulof(worldY, 1);
            int[] dxy = bRightOfSquare ? this.rightSideXy : this.leftSideXy;
            for (int i = 0; i < dxy.length; i += 2) {
                this.getObjectsOnSquare(PZMath.fastfloor(worldX) + dxy[i], PZMath.fastfloor(worldY) + dxy[i + 1], z, objects);
            }
            this.getCorpsesNear(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), z, objects, bRightOfSquare, screenX, screenY);
        }
    }

    void getObjectsOnSquare(int worldX, int worldY, int z, ArrayList<IsoObject> objects) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(worldX, worldY, z);
        if (square == null) {
            return;
        }
        boolean playerIndex = false;
        IsoObject[] objects1 = square.getObjects().getElements();
        int numObjects = square.getObjects().size();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject object = objects1[i];
            ObjectRenderInfo renderInfo = object.getRenderInfo(0);
            if (renderInfo.layer == ObjectRenderLayer.None || renderInfo.targetAlpha == 0.0f) {
                if (object.sprite == null || !object.sprite.getProperties().has(IsoFlagType.water)) continue;
                objects.add(object);
                continue;
            }
            if (renderInfo.renderWidth <= 0.0f || renderInfo.renderHeight <= 0.0f) continue;
            objects.add(object);
        }
    }

    void getCorpsesNear(int worldX, int worldY, int z, ArrayList<IsoObject> objects, boolean bRightOfSquare, int screenX, int screenY) {
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                this.getCorpsesNear(worldX + dx, worldY + dy, z, objects, screenX, screenY);
            }
        }
    }

    void getCorpsesNear(int worldX, int worldY, int z, ArrayList<IsoObject> objects, int screenX, int screenY) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(worldX, worldY, z);
        if (square == null) {
            return;
        }
        ArrayList<IsoMovingObject> corpses = square.getStaticMovingObjects();
        for (int i = 0; i < corpses.size(); ++i) {
            IsoDeadBody deadBody;
            IsoMovingObject isoMovingObject = corpses.get(i);
            if (!(isoMovingObject instanceof IsoDeadBody) || !(deadBody = (IsoDeadBody)isoMovingObject).isMouseOver(screenX, screenY)) continue;
            objects.add(deadBody);
        }
    }

    IsoObject getFirst(int screenX, int screenY, IObjectPickerPredicate predicate) {
        boolean playerIndex = false;
        float zoom = Core.getInstance().getZoom(0);
        float x = (float)screenX * zoom;
        float y = (float)screenY * zoom;
        this.clickObjectPool.releaseAll((List<IsoObjectPicker.ClickObject>)this.clickObjects);
        this.clickObjects.clear();
        this.getClickObjects(screenX, screenY, this.clickObjects);
        for (int i = this.clickObjects.size() - 1; i >= 0; --i) {
            IsoObjectPicker.ClickObject clickObject = this.clickObjects.get(i);
            int c = predicate.test(clickObject, x, y);
            if (c == -1) {
                return null;
            }
            if (c != 1) continue;
            return clickObject.tile;
        }
        return null;
    }

    public IsoObject PickDoor(int screenX, int screenY, boolean bTransparent) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int ly;
            int lx;
            boolean playerIndex = false;
            if (!(obj.tile instanceof IsoDoor)) {
                return 0;
            }
            if (obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (bTransparent != obj.tile.getRenderInfo((int)0).targetAlpha < 1.0f) {
                return 0;
            }
            if (obj.contains(x, y) && obj.tile.isMaskClicked(lx = PZMath.fastfloor(x - (float)obj.x), ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) {
                return 1;
            }
            return 0;
        });
    }

    public IsoObject PickWindow(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            boolean playerIndex = false;
            if (!(obj.tile instanceof IsoWindow) && !(obj.tile instanceof IsoCurtain)) {
                return 0;
            }
            if (obj.tile.sprite != null && obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (obj.contains(x, y)) {
                int ly;
                int lx = PZMath.fastfloor(x - (float)obj.x);
                if (obj.tile.isMaskClicked(lx, ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) {
                    return 1;
                }
                if (obj.tile instanceof IsoWindow) {
                    int ty;
                    boolean above = false;
                    boolean below = false;
                    for (ty = ly; ty >= 0; --ty) {
                        if (!obj.tile.isMaskClicked(lx, ty)) continue;
                        above = true;
                        break;
                    }
                    for (ty = ly; ty < obj.height; ++ty) {
                        if (!obj.tile.isMaskClicked(lx, ty)) continue;
                        below = true;
                        break;
                    }
                    if (above && below) {
                        return 1;
                    }
                }
            }
            return 0;
        });
    }

    public IsoObject PickWindowFrame(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            boolean playerIndex = false;
            if (!(obj.tile instanceof IsoWindowFrame)) {
                return 0;
            }
            if (obj.tile.sprite != null && obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (obj.contains(x, y)) {
                int ty;
                int ly;
                int lx = PZMath.fastfloor(x - (float)obj.x);
                if (obj.tile.isMaskClicked(lx, ly = PZMath.fastfloor(y - (float)obj.y), obj.flip)) {
                    return 1;
                }
                boolean above = false;
                boolean below = false;
                for (ty = ly; ty >= 0; --ty) {
                    if (!obj.tile.isMaskClicked(lx, ty)) continue;
                    above = true;
                    break;
                }
                for (ty = ly; ty < obj.height; ++ty) {
                    if (!obj.tile.isMaskClicked(lx, ty)) continue;
                    below = true;
                    break;
                }
                if (above && below) {
                    return 1;
                }
            }
            return 0;
        });
    }

    public IsoObject PickThumpable(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            boolean playerIndex = false;
            IsoObject patt0$temp = obj.tile;
            if (!(patt0$temp instanceof IsoThumpable)) {
                return 0;
            }
            IsoThumpable thump = (IsoThumpable)patt0$temp;
            if (obj.tile.sprite != null && obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (obj.contains(x, y)) {
                int lx = (int)(x - (float)obj.x);
                int ly = (int)(y - (float)obj.y);
                if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                    return 1;
                }
                if (thump.isWindow()) {
                    int ty;
                    boolean above = false;
                    boolean below = false;
                    for (ty = ly; ty >= 0; --ty) {
                        if (!obj.tile.isMaskClicked(lx, ty)) continue;
                        above = true;
                        break;
                    }
                    for (ty = ly; ty < obj.height; ++ty) {
                        if (!obj.tile.isMaskClicked(lx, ty)) continue;
                        below = true;
                        break;
                    }
                    if (above && below) {
                        return 1;
                    }
                }
            }
            return 0;
        });
    }

    public IsoObject PickHoppable(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int ly;
            int lx;
            boolean playerIndex = false;
            if (!obj.tile.isHoppable()) {
                return 0;
            }
            if (obj.tile.sprite != null && obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (obj.contains(x, y) && obj.tile.isMaskClicked(lx = (int)(x - (float)obj.x), ly = (int)(y - (float)obj.y), obj.flip)) {
                return 1;
            }
            return 0;
        });
    }

    public IsoObject PickCorpse(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            boolean playerIndex = false;
            IsoObject patt0$temp = obj.tile;
            if (patt0$temp instanceof IsoDeadBody) {
                IsoDeadBody isoDeadBody = (IsoDeadBody)patt0$temp;
                return isoDeadBody.isMouseOver(x, y) ? 1 : 0;
            }
            if (obj.contains(x, y)) {
                if (obj.tile.getRenderInfo((int)0).targetAlpha < 1.0f) {
                    return 0;
                }
                if (obj.tile.isMaskClicked((int)(x - (float)obj.x), (int)(y - (float)obj.y), obj.flip) && !(obj.tile instanceof IsoWindow)) {
                    return -1;
                }
            }
            return 0;
        });
    }

    public IsoObject PickTree(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int ly;
            int lx;
            boolean playerIndex = false;
            if (!(obj.tile instanceof IsoTree)) {
                return 0;
            }
            if (obj.tile.sprite != null && obj.tile.getRenderInfo((int)0).targetAlpha == 0.0f) {
                return 0;
            }
            if (obj.contains(x, y) && obj.tile.isMaskClicked(lx = (int)(x - (float)obj.x), ly = (int)(y - (float)obj.y), obj.flip)) {
                return 1;
            }
            return 0;
        });
    }

    public Vector2f getPointRelativeToTopLeftOfTexture(IsoObject object, int screenX, int screenY, Vector2f out) {
        int clickObjectY;
        int clickObjectX;
        if (object == null) {
            return null;
        }
        if (object.getSprite() == null) {
            return null;
        }
        Texture texture = object.getSprite().getTextureForCurrentFrame(object.getDir(), object);
        if (texture == null) {
            return null;
        }
        if (object.getSquare() == null) {
            return null;
        }
        boolean playerIndex = false;
        float zoom = Core.getInstance().getZoom(0);
        FBORenderLevels renderLevels = object.getSquare().getChunk().getRenderLevels(0);
        FBORenderChunk renderChunk = renderLevels.getFBOForLevel(object.getSquare().z, zoom);
        if (renderChunk == null) {
            return null;
        }
        ObjectRenderInfo renderInfo = object.getRenderInfo(0);
        if (renderInfo.layer == ObjectRenderLayer.Translucent || renderInfo.layer == ObjectRenderLayer.TranslucentFloor) {
            clickObjectX = (int)renderInfo.renderX;
            clickObjectY = (int)renderInfo.renderY;
        } else if (DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
            clickObjectX = (int)(renderChunk.renderX + renderInfo.renderX);
            clickObjectY = (int)(renderChunk.renderY + renderInfo.renderY);
        } else {
            clickObjectX = (int)(renderChunk.renderX * zoom + renderInfo.renderX);
            clickObjectY = (int)(renderChunk.renderY * zoom + renderInfo.renderY);
        }
        out.x = (float)screenX * zoom - (float)clickObjectX - texture.getOffsetX();
        out.y = (float)screenY * zoom - (float)clickObjectY - texture.getOffsetY();
        return out;
    }

    public static interface IObjectPickerPredicate {
        public int test(IsoObjectPicker.ClickObject var1, float var2, float var3);
    }
}

