/*
 * Decompiled with CFR 0.152.
 */
package zombie.buildingRooms;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.buildingRooms.BREBuilding;
import zombie.buildingRooms.BRERoom;
import zombie.buildingRooms.BuildingRoomsEditor;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDepthHelper;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;

public final class BuildingRoomsDrawer
extends TextureDraw.GenericDrawer {
    private static final int FLOATS_PER_RECT = 7;
    TFloatArrayList floats = new TFloatArrayList();
    int currentLevel;
    int highlightRectForDeletion = -1;

    void set(ArrayList<BREBuilding> buildings, BRERoom room, int level, int highlightRectForDeletion) {
        this.highlightRectForDeletion = -1;
        this.floats.resetQuick();
        BREBuilding selectedBuilding = room == null ? null : room.building;
        this.currentLevel = level;
        for (BREBuilding building : buildings) {
            for (BRERoom room1 : building.rooms) {
                if (room1.getLevel() != level) continue;
                for (int rectIndex = 0; rectIndex < room1.roomDef.rects.size(); ++rectIndex) {
                    RoomDef.RoomRect rect = room1.getRectangle(rectIndex);
                    if (room1 == room && highlightRectForDeletion == rectIndex) {
                        this.highlightRectForDeletion = this.floats.size() / 7;
                    }
                    this.floats.add(rect.x);
                    this.floats.add(rect.y);
                    this.floats.add(rect.w);
                    this.floats.add(rect.h);
                    this.floats.add(room1.getLevel());
                    this.floats.add(room1 == room ? 1.0f : 0.0f);
                    this.floats.add(building == selectedBuilding ? 1.0f : 0.0f);
                }
            }
        }
    }

    @Override
    public void render() {
        float a;
        float b;
        float g;
        float r;
        boolean bSameBuilding;
        boolean bSelected;
        int level;
        float h;
        float w;
        float y;
        float x;
        int i;
        this.setMatrices();
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        float rcx = camera.rightClickX;
        float rcy = camera.rightClickY;
        float tox = camera.getTOffX();
        float toy = camera.getTOffY();
        float defx = camera.deferedX;
        float defy = camera.deferedY;
        float playerX = Core.getInstance().floatParamMap.get(0).floatValue();
        float playerY = Core.getInstance().floatParamMap.get(1).floatValue();
        float playerZ = Core.getInstance().floatParamMap.get(2).floatValue();
        float cx = playerX - camera.XToIso(-tox - rcx, -toy - rcy, 0.0f);
        float cy = playerY - camera.YToIso(-tox - rcx, -toy - rcy, 0.0f);
        cx += defx;
        cy += defy;
        GL11.glEnable(2929);
        GL11.glDepthFunc(515);
        GL11.glDepthMask(false);
        GL11.glBlendFunc(770, 771);
        VBORenderer vbor = VBORenderer.getInstance();
        float f = 0.05f;
        for (i = 0; i < this.floats.size(); i += 7) {
            x = this.floats.get(i);
            y = this.floats.get(i + 1);
            w = this.floats.get(i + 2);
            h = this.floats.get(i + 3);
            level = Math.round(this.floats.get(i + 4));
            bSelected = Math.round(this.floats.get(i + 5)) != 0;
            bSameBuilding = Math.round(this.floats.get(i + 6)) != 0;
            r = 0.0f;
            g = 1.0f;
            b = 0.0f;
            a = 0.2f;
            if (bSelected) {
                if (i == this.highlightRectForDeletion * 7) {
                    r = 1.0f;
                    g = 0.0f;
                    b = 0.0f;
                }
            } else {
                r = 0.0f;
                g = 0.0f;
                b = 1.0f;
            }
            if (!bSameBuilding) {
                b = 0.75f;
                g = 0.75f;
                r = 0.75f;
            }
            this.renderRect(playerX, playerY, playerZ, cx, cy, x + 0.05f, y + 0.05f, w - 0.1f, h - 0.1f, level, r, g, b, 0.2f);
        }
        for (i = 0; i < this.floats.size(); i += 7) {
            x = this.floats.get(i);
            y = this.floats.get(i + 1);
            w = this.floats.get(i + 2);
            h = this.floats.get(i + 3);
            level = Math.round(this.floats.get(i + 4));
            bSelected = Math.round(this.floats.get(i + 5)) != 0;
            bSameBuilding = Math.round(this.floats.get(i + 6)) != 0;
            r = 0.0f;
            g = 1.0f;
            b = 0.0f;
            a = 1.0f;
            if (bSelected) {
                if (i == this.highlightRectForDeletion * 7) {
                    r = 1.0f;
                    g = 0.0f;
                    b = 0.0f;
                }
            } else {
                r = 0.0f;
                g = 0.0f;
                b = 1.0f;
            }
            if (!bSameBuilding) {
                b = 0.75f;
                g = 0.75f;
                r = 0.75f;
            }
            this.renderRectOutline(playerX, playerY, playerZ, cx, cy, x + 0.05f, y + 0.05f, w - 0.1f, h - 0.1f, level, r, g, b, 1.0f);
        }
        vbor.flush();
        Core.getInstance().projectionMatrixStack.pop();
        GLStateRenderThread.restore();
    }

    private void setMatrices() {
        SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
        PlayerCamera camera = renderState.playerCamera[renderState.playerIndex];
        double screenWidth = (float)camera.offscreenWidth / 1920.0f;
        double screenHeight = (float)camera.offscreenHeight / 1920.0f;
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc();
        projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
        Core.getInstance().projectionMatrixStack.push(projection);
    }

    void renderRect(float playerX, float playerY, float playerZ, float cx, float cy, float x1, float y1, float w1, float h1, int level, float r, float g, float b, float a) {
        VBORenderer vbor = VBORenderer.getInstance();
        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 1.0f;
        float v1 = 0.0f;
        float u2 = 1.0f;
        float v2 = 1.0f;
        float u3 = 0.0f;
        float v3 = 1.0f;
        float x2 = x1 + w1;
        float ox = (x1 + x2) / 2.0f;
        float y2 = y1 + h1;
        float oy = (y1 + y2) / 2.0f;
        float depthAdjust = ox + oy < playerX + playerY ? -1.4E-4f : -1.0E-4f;
        float depth0 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)x1, (float)y1, (float)((float)level)).depthStart + depthAdjust;
        float depth1 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)x2, (float)y1, (float)((float)level)).depthStart + depthAdjust;
        float depth2 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)x2, (float)y2, (float)((float)level)).depthStart + depthAdjust;
        float depth3 = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)playerX), (int)PZMath.fastfloor((float)playerY), (float)x1, (float)y2, (float)((float)level)).depthStart + depthAdjust;
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.scaling(Core.scale);
        modelView.scale((float)Core.tileScale / 2.0f);
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = ((float)level - playerZ) * 2.44949f;
        modelView.translate(-((float)difX), (float)difZ, -((float)difY));
        modelView.scale(-1.0f, 1.0f, -1.0f);
        modelView.translate(0.0f, -0.71999997f, 0.0f);
        vbor.cmdPushAndLoadMatrix(5888, modelView);
        vbor.startRun(vbor.formatPositionColorUvDepth);
        vbor.setMode(7);
        vbor.setDepthTest(false);
        vbor.setTextureID(Texture.getWhite().getTextureId());
        float z = 0.0f;
        vbor.addQuadDepth(x1 - ox, 0.0f, y1 - oy, 0.0f, 0.0f, depth0, x2 - ox, 0.0f, y1 - oy, 1.0f, 0.0f, depth1, x2 - ox, 0.0f, y2 - oy, 1.0f, 1.0f, depth2, x1 - ox, 0.0f, y2 - oy, 0.0f, 1.0f, depth3, r, g, b, a);
        vbor.endRun();
        vbor.cmdPopMatrix(5888);
    }

    void renderRectOutline(float playerX, float playerY, float playerZ, float cx, float cy, float x1, float y1, float w1, float h1, int level, float r, float g, float b, float a) {
        VBORenderer vbor = VBORenderer.getInstance();
        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 1.0f;
        float v1 = 0.0f;
        float u2 = 1.0f;
        float v2 = 1.0f;
        float u3 = 0.0f;
        float v3 = 1.0f;
        float x2 = x1 + w1;
        float y2 = y1 + h1;
        float ox = (x1 + x2) / 2.0f;
        float oy = (y1 + y2) / 2.0f;
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc();
        modelView.scaling(Core.scale);
        modelView.scale((float)Core.tileScale / 2.0f);
        modelView.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        modelView.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        double difX = ox - cx;
        double difY = oy - cy;
        double difZ = ((float)level - playerZ) * 2.44949f;
        modelView.translate(-((float)difX), (float)difZ, -((float)difY));
        modelView.scale(-1.0f, 1.0f, -1.0f);
        modelView.translate(0.0f, -0.71999997f, 0.0f);
        vbor.cmdPushAndLoadMatrix(5888, modelView);
        vbor.startRun(vbor.formatPositionColorUvDepth);
        vbor.setMode(1);
        vbor.setDepthTest(false);
        vbor.setTextureID(Texture.getWhite().getTextureId());
        float z = 0.0f;
        vbor.addQuad(x1 - ox, 0.0f, y1 - oy, 0.0f, 0.0f, x2 - ox, 0.0f, y1 - oy, 1.0f, 0.0f, x2 - ox, 0.0f, y2 - oy, 1.0f, 1.0f, x1 - ox, 0.0f, y2 - oy, 0.0f, 1.0f, r, g, b, a);
        vbor.endRun();
        vbor.cmdPopMatrix(5888);
    }

    @Override
    public void postRender() {
        BuildingRoomsEditor.getInstance().drawerPool.release(this);
    }
}

