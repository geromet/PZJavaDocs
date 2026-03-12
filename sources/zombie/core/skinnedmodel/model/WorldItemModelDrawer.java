/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.properties.IsoPropertyType;
import zombie.core.skinnedmodel.model.ExtendedPlacementDrawer;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;

public final class WorldItemModelDrawer
extends TextureDraw.GenericDrawer {
    private static final ObjectPool<WorldItemModelDrawer> s_modelDrawerPool = new ObjectPool<WorldItemModelDrawer>(WorldItemModelDrawer::new);
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final Matrix4f s_attachmentXfrm = new Matrix4f();
    public static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5f, 0.5f, 0.5f);
    public static final ImmutableColor HIGHLIGHT_COLOR = new ImmutableColor(0.5f, 1.0f, 1.0f);
    public static final boolean NEW_WAY = true;
    public static final float CARPET_DELTA_Z = 0.003f;
    private static final float CLIPPING_Z_OFFSET = 0.00175f;
    private final ItemModelRenderer renderer = new ItemModelRenderer();

    public static ItemModelRenderer.RenderStatus renderMain(InventoryItem item, IsoGridSquare square, IsoGridSquare renderSquare, float x, float y, float z, float flipAngle) {
        return WorldItemModelDrawer.renderMain(item, square, renderSquare, x, y, z, flipAngle, -1.0f, false);
    }

    public static ItemModelRenderer.RenderStatus renderMain(InventoryItem item, IsoGridSquare square, IsoGridSquare renderSquare, float x, float y, float z, float flipAngle, float forcedRotation, boolean bIgnoreItemsInChunkTexture) {
        ItemModelRenderer.RenderStatus status;
        boolean bRenderToChunkTexture;
        if (item == null || square == null) {
            return ItemModelRenderer.RenderStatus.Failed;
        }
        if (!Core.getInstance().isOption3DGroundItem()) {
            return ItemModelRenderer.RenderStatus.NoModel;
        }
        if (WorldItemModelDrawer.renderAtlasTexture(item, square, x, y, z, flipAngle, forcedRotation, bIgnoreItemsInChunkTexture)) {
            return ItemModelRenderer.RenderStatus.Ready;
        }
        if (!ItemModelRenderer.itemHasModel(item)) {
            return ItemModelRenderer.RenderStatus.NoModel;
        }
        WorldItemModelDrawer modelDrawer = s_modelDrawerPool.alloc();
        boolean bl = bRenderToChunkTexture = PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
        if (!bIgnoreItemsInChunkTexture && !DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
            bRenderToChunkTexture = false;
        }
        float adjustedZ = 0.00175f;
        if (square.getProperties().propertyEquals(IsoPropertyType.FOOTSTEP_MATERIAL, "Carpet")) {
            adjustedZ += 0.003f;
        }
        if ((status = modelDrawer.renderer.renderMain(item, square, renderSquare, x, y, z + adjustedZ, flipAngle, forcedRotation, bRenderToChunkTexture)) == ItemModelRenderer.RenderStatus.Ready) {
            if (item.isDoingExtendedPlacement()) {
                ExtendedPlacementDrawer epd = ExtendedPlacementDrawer.s_pool.alloc();
                epd.init(item.getWorldItem(), modelDrawer.renderer.calculateMinModelZ());
                SpriteRenderer.instance.drawGeneric(epd);
            }
            SpriteRenderer.instance.drawGeneric(modelDrawer);
            return status;
        }
        modelDrawer.renderer.reset();
        s_modelDrawerPool.release(modelDrawer);
        return status;
    }

    private static boolean renderAtlasTexture(InventoryItem item, IsoGridSquare square, float x, float y, float z, float flipAngle, float forcedRotation, boolean bIgnoreItemsInChunkTexture) {
        boolean bMaxZoomIsOne;
        boolean bRenderToChunkTexture;
        if (flipAngle > 0.0f) {
            return false;
        }
        if (forcedRotation >= 0.0f) {
            return false;
        }
        if (item.isDoingExtendedPlacement()) {
            return false;
        }
        boolean bUseAtlas = DebugOptions.instance.worldItemAtlas.enable.getValue();
        boolean bl = bRenderToChunkTexture = PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
        if (!bIgnoreItemsInChunkTexture && !DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
            bRenderToChunkTexture = false;
        }
        if (bRenderToChunkTexture) {
            bUseAtlas = false;
        }
        if (!bUseAtlas) {
            return false;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        float zoom = Core.getInstance().getZoom(playerIndex);
        boolean bl2 = bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75f;
        if (item.atlasTexture != null && !item.atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
            item.atlasTexture = null;
        }
        if (item.atlasTexture == null) {
            item.atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
        }
        if (item.atlasTexture == null) {
            return false;
        }
        if (item.atlasTexture.isTooBig()) {
            return false;
        }
        float alpha = IsoWorldInventoryObject.getSurfaceAlpha(square, z - (float)((int)z));
        if (alpha <= 0.0f) {
            return true;
        }
        if (IsoSprite.globalOffsetX == -1.0f) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }
        float adjustedZ = z;
        if (square.getProperties().propertyEquals(IsoPropertyType.FOOTSTEP_MATERIAL, "Carpet")) {
            adjustedZ += 0.003f;
        }
        float sx = IsoUtils.XToScreen(x, y, adjustedZ, 0);
        float sy = IsoUtils.YToScreen(x, y, adjustedZ, 0);
        if (FBORenderChunkManager.instance.isCaching()) {
            sx = IsoUtils.XToScreen(PZMath.coordmodulof(x, 8), PZMath.coordmodulof(y, 8), adjustedZ, 0);
            sy = IsoUtils.YToScreen(PZMath.coordmodulof(x, 8), PZMath.coordmodulof(y, 8), adjustedZ, 0);
            sx += FBORenderChunkManager.instance.getXOffset();
            sy += FBORenderChunkManager.instance.getYOffset();
            TextureDraw.nextZ = IsoDepthHelper.calculateDepth(x + 0.25f, y + 0.25f, adjustedZ) * 2.0f - 1.0f;
        } else {
            sx += IsoSprite.globalOffsetX;
            sy += IsoSprite.globalOffsetY;
        }
        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
            sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * zoom;
            sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * zoom;
        }
        square.interpolateLight(tempColorInfo, x % 1.0f, y % 1.0f);
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(square)) {
            float fade = 1.0f - FBORenderCell.instance.getBlackedOutRoomFadeRatio(square);
            tempColorInfo.set(WorldItemModelDrawer.tempColorInfo.r * fade, WorldItemModelDrawer.tempColorInfo.g * fade, WorldItemModelDrawer.tempColorInfo.b * fade, WorldItemModelDrawer.tempColorInfo.a);
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            tempColorInfo.set(1.0f, 1.0f, 1.0f, WorldItemModelDrawer.tempColorInfo.a);
        }
        if (item.getWorldItem() != null && item.getWorldItem().isHighlighted()) {
            ColorInfo highlightColor = item.getWorldItem().getHighlightColor();
            tempColorInfo.set(highlightColor.r, highlightColor.g, highlightColor.b, 1.0f);
        }
        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)(x + 0.25f), (float)(y + 0.25f), (float)adjustedZ).depthStart * 2.0f - 1.0f;
        }
        item.atlasTexture.render(x, y, adjustedZ, sx, sy, WorldItemModelDrawer.tempColorInfo.r, WorldItemModelDrawer.tempColorInfo.g, WorldItemModelDrawer.tempColorInfo.b, alpha);
        WorldItemAtlas.instance.render();
        return item.atlasTexture.isRenderMainOK();
    }

    @Override
    public void render() {
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderThreadCurrent;
        if (PerformanceSettings.fboRenderChunk && renderChunk != null) {
            FBORenderItems.getInstance().setCamera(renderChunk, this.renderer.x, this.renderer.y, this.renderer.z, this.renderer.angle);
            this.renderer.DoRender(FBORenderItems.getInstance().getCamera(), true, renderChunk.highRes);
            return;
        }
        this.renderer.DoRenderToWorld(this.renderer.x, this.renderer.y, this.renderer.z, this.renderer.angle);
    }

    @Override
    public void postRender() {
        this.renderer.reset();
        s_modelDrawerPool.release(this);
    }
}

