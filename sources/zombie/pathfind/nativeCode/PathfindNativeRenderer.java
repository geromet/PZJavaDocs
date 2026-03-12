/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind.nativeCode;

import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.VBORenderer;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;
import zombie.pathfind.nativeCode.PathfindNativeThread;

public final class PathfindNativeRenderer {
    public static final PathfindNativeRenderer instance = new PathfindNativeRenderer();
    final Drawer[][] drawers = new Drawer[4][3];
    PlayerCamera camera;

    public void render() {
        if (!Core.debug) {
            return;
        }
        if (!DebugOptions.instance.pathfindPathToMouseEnable.getValue()) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        int stateIndex = SpriteRenderer.instance.getMainStateIndex();
        Drawer drawer = this.drawers[playerIndex][stateIndex];
        if (drawer == null) {
            Drawer drawer2 = new Drawer(playerIndex);
            this.drawers[playerIndex][stateIndex] = drawer2;
            drawer = drawer2;
        }
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void drawLine(float fromX, float fromY, float fromZ, float toX, float toY, float toZ, float thickness, float r, float g, float b, float a) {
        VBORenderer vbor = VBORenderer.getInstance();
        float x1 = this.camera.XToScreenExact(fromX, fromY, fromZ, 0);
        float y1 = this.camera.YToScreenExact(fromX, fromY, fromZ, 0);
        float x2 = this.camera.XToScreenExact(toX, toY, toZ, 0);
        float y2 = this.camera.YToScreenExact(toX, toY, toZ, 0);
        if (PerformanceSettings.fboRenderChunk) {
            x1 += this.camera.fixJigglyModelsX * this.camera.zoom;
            y1 += this.camera.fixJigglyModelsY * this.camera.zoom;
            x2 += this.camera.fixJigglyModelsX * this.camera.zoom;
            y2 += this.camera.fixJigglyModelsY * this.camera.zoom;
        }
        if (thickness == 1.0f) {
            vbor.addLine(x1, y1, 0.0f, x2, y2, 0.0f, r, g, b, a);
        } else {
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(7);
            vbor.addLineWithThickness(x1, y1, 0.0f, x2, y2, 0.0f, thickness, r, g, b, a);
            vbor.endRun();
            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);
        }
    }

    public void drawRect(float x, float y, float z, float w, float h, float r, float g, float b, float a) {
        float thickness = 1.0f;
        this.drawLine(x, y, z, x + w, y, z, 1.0f, r, g, b, a);
        this.drawLine(x + w, y, z, x + w, y + h, z, 1.0f, r, g, b, a);
        this.drawLine(x + w, y + h, z, x, y + h, z, 1.0f, r, g, b, a);
        this.drawLine(x, y + h, z, x, y, z, 1.0f, r, g, b, a);
    }

    native void renderNative();

    native void setDebugOption(String var1, String var2);

    static final class Drawer
    extends TextureDraw.GenericDrawer {
        int playerIndex;

        Drawer(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void render() {
            if (PathfindNativeThread.instance == null) {
                return;
            }
            Object object = PathfindNativeThread.instance.renderLock;
            synchronized (object) {
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.startRun(vbor.formatPositionColor);
                vbor.setMode(1);
                int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
                PathfindNativeRenderer.instance.camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
                instance.setDebugOption(DebugOptions.instance.pathfindPathToMouseRenderSuccessors.getName(), DebugOptions.instance.pathfindPathToMouseRenderSuccessors.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.pathfindSmoothPlayerPath.getName(), DebugOptions.instance.pathfindSmoothPlayerPath.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.pathfindRenderChunkRegions.getName(), DebugOptions.instance.pathfindRenderChunkRegions.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.pathfindRenderPath.getName(), DebugOptions.instance.pathfindRenderPath.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.polymapRenderClusters.getName(), DebugOptions.instance.polymapRenderClusters.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.polymapRenderConnections.getName(), DebugOptions.instance.polymapRenderConnections.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.polymapRenderCrawling.getName(), DebugOptions.instance.polymapRenderCrawling.getValueAsString());
                instance.setDebugOption(DebugOptions.instance.polymapRenderNodes.getName(), DebugOptions.instance.polymapRenderNodes.getValueAsString());
                instance.renderNative();
                vbor.endRun();
                vbor.flush();
            }
        }
    }
}

