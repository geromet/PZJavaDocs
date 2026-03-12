/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;

public final class LineDrawer {
    private static final ArrayList<DrawableLine> lines = new ArrayList();
    private static final ArrayList<DrawableLine> alphaDecayingLines = new ArrayList();
    private static final ArrayDeque<DrawableLine> pool = new ArrayDeque();
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();

    private static void DrawTexturedRect(Texture tex, float x, float y, float width, float height, int z, float r, float g, float bl) {
        x = (int)x;
        y = (int)y;
        Vector2 a = new Vector2(x, y);
        Vector2 b = new Vector2(x + width, y);
        Vector2 c = new Vector2(x + width, y + height);
        Vector2 d = new Vector2(x, y + height);
        Vector2 at = new Vector2(IsoUtils.XToScreen(a.x, a.y, z, 0), IsoUtils.YToScreen(a.x, a.y, z, 0));
        Vector2 bt = new Vector2(IsoUtils.XToScreen(b.x, b.y, z, 0), IsoUtils.YToScreen(b.x, b.y, z, 0));
        Vector2 ct = new Vector2(IsoUtils.XToScreen(c.x, c.y, z, 0), IsoUtils.YToScreen(c.x, c.y, z, 0));
        Vector2 dt = new Vector2(IsoUtils.XToScreen(d.x, d.y, z, 0), IsoUtils.YToScreen(d.x, d.y, z, 0));
        PlayerCamera camera = IsoCamera.cameras[IsoPlayer.getPlayerIndex()];
        at.x -= camera.offX;
        bt.x -= camera.offX;
        ct.x -= camera.offX;
        dt.x -= camera.offX;
        at.y -= camera.offY;
        bt.y -= camera.offY;
        ct.y -= camera.offY;
        dt.y -= camera.offY;
        float offsetY = -240.0f;
        float offsetX = -32.0f;
        at.y -= (offsetY -= 128.0f);
        bt.y -= offsetY;
        ct.y -= offsetY;
        dt.y -= offsetY;
        at.x -= -32.0f;
        bt.x -= -32.0f;
        ct.x -= -32.0f;
        dt.x -= -32.0f;
        SpriteRenderer.instance.renderdebug(tex, at.x, at.y, bt.x, bt.y, ct.x, ct.y, dt.x, dt.y, r, g, bl, 1.0f, r, g, bl, 1.0f, r, g, bl, 1.0f, r, g, bl, 1.0f, null);
    }

    static void DrawIsoLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        tempo.set(x, y);
        tempo2.set(x2, y2);
        Vector2 at = new Vector2(IsoUtils.XToScreen(LineDrawer.tempo.x, LineDrawer.tempo.y, 0.0f, 0), IsoUtils.YToScreen(LineDrawer.tempo.x, LineDrawer.tempo.y, 0.0f, 0));
        Vector2 bt = new Vector2(IsoUtils.XToScreen(LineDrawer.tempo2.x, LineDrawer.tempo2.y, 0.0f, 0), IsoUtils.YToScreen(LineDrawer.tempo2.x, LineDrawer.tempo2.y, 0.0f, 0));
        at.x -= IsoCamera.getOffX();
        bt.x -= IsoCamera.getOffX();
        at.y -= IsoCamera.getOffY();
        bt.y -= IsoCamera.getOffY();
        LineDrawer.drawLine(at.x, at.y, bt.x, bt.y, r, g, b, a, thickness);
    }

    public static void DrawRect(float x, float y, float z, float width, float height, float r, float g, float b, float a, int thickness) {
        LineDrawer.DrawIsoLine(x - width, y - width, z, x + width, y - width, z, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y + width, z, x + width, y + width, z, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x + width, y - width, z, x + width, y + width, z, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y - width, z, x - width, y + width, z, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y - width, z + height, x + width, y - width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y + width, z + height, x + width, y + width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x + width, y - width, z + height, x + width, y + width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y - width, z + height, x - width, y + width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y - width, z, x - width, y - width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x + width, y - width, z, x + width, y - width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x - width, y + width, z, x - width, y + width, z + height, r, g, b, a, thickness);
        LineDrawer.DrawIsoLine(x + width, y + width, z, x + width, y + width, z + height, r, g, b, a, thickness);
    }

    public static void DrawIsoRect(float x, float y, float width, float height, int z, float r, float g, float bl) {
        LineDrawer.DrawIsoRect(x, y, width, height, z, 0, r, g, bl);
    }

    public static void DrawIsoRect(float x, float y, float width, float height, int z, int yPixelOffset, float r, float g, float bl) {
        if (width < 0.0f) {
            width = -width;
            x -= width;
        }
        if (height < 0.0f) {
            height = -height;
            y -= height;
        }
        float ax = IsoUtils.XToScreenExact(x, y, z, 0);
        float ay = IsoUtils.YToScreenExact(x, y, z, 0);
        float bx = IsoUtils.XToScreenExact(x + width, y, z, 0);
        float by = IsoUtils.YToScreenExact(x + width, y, z, 0);
        float cx = IsoUtils.XToScreenExact(x + width, y + height, z, 0);
        float cy = IsoUtils.YToScreenExact(x + width, y + height, z, 0);
        float dx = IsoUtils.XToScreenExact(x, y + height, z, 0);
        float dy = IsoUtils.YToScreenExact(x, y + height, z, 0);
        float f = -yPixelOffset * Core.tileScale;
        ay += f;
        by += f;
        cy += f;
        dy += f;
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        LineDrawer.drawLine(ax += camera.fixJigglyModelsX * zoom, ay += camera.fixJigglyModelsY * zoom, bx += camera.fixJigglyModelsX * zoom, by += camera.fixJigglyModelsY * zoom, r, g, bl);
        LineDrawer.drawLine(bx, by, cx += camera.fixJigglyModelsX * zoom, cy += camera.fixJigglyModelsY * zoom, r, g, bl);
        LineDrawer.drawLine(cx, cy, dx += camera.fixJigglyModelsX * zoom, dy += camera.fixJigglyModelsY * zoom, r, g, bl);
        LineDrawer.drawLine(dx, dy, ax, ay, r, g, bl);
    }

    public static void DrawIsoRectRotated(float x, float y, float z, float w, float h, float angleRadians, float r, float g, float b, float a) {
        Vector2 vec = tempo.setLengthAndDirection(angleRadians, 1.0f);
        Vector2 vec2 = tempo2.set(vec);
        vec2.tangent();
        vec.x *= h / 2.0f;
        vec.y *= h / 2.0f;
        vec2.x *= w / 2.0f;
        vec2.y *= w / 2.0f;
        float fx = x + vec.x;
        float fy = y + vec.y;
        float bx = x - vec.x;
        float by = y - vec.y;
        float fx1 = fx - vec2.x;
        float fy1 = fy - vec2.y;
        float fx2 = fx + vec2.x;
        float fy2 = fy + vec2.y;
        float bx1 = bx - vec2.x;
        float by1 = by - vec2.y;
        float bx2 = bx + vec2.x;
        float by2 = by + vec2.y;
        boolean thickness = true;
        LineDrawer.DrawIsoLine(fx1, fy1, z, fx2, fy2, z, r, g, b, a, 1);
        LineDrawer.DrawIsoLine(fx1, fy1, z, bx1, by1, z, r, g, b, a, 1);
        LineDrawer.DrawIsoLine(fx2, fy2, z, bx2, by2, z, r, g, b, a, 1);
        LineDrawer.DrawIsoLine(bx1, by1, z, bx2, by2, z, r, g, b, a, 1);
    }

    public static void DrawIsoLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        LineDrawer.drawLine(sx += camera.fixJigglyModelsX * zoom, sy += camera.fixJigglyModelsY * zoom, sx2 += camera.fixJigglyModelsX * zoom, sy2 += camera.fixJigglyModelsY * zoom, r, g, b, a, thickness);
    }

    public static void DrawIsoLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a, float baseThickness, float topThickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        LineDrawer.drawLine(sx += camera.fixJigglyModelsX * zoom, sy += camera.fixJigglyModelsY * zoom, sx2 += camera.fixJigglyModelsX * zoom, sy2 += camera.fixJigglyModelsY * zoom, r, g, b, a, baseThickness, topThickness);
    }

    public static void DrawIsoTransform(float px, float py, float z, float rx, float ry, float radius, int segments, float r, float g, float b, float a, int t) {
        LineDrawer.DrawIsoCircle(px, py, z, radius, segments, r, g, b, a);
        LineDrawer.DrawIsoLine(px, py, z, px + rx + radius / 2.0f, py + ry + radius / 2.0f, z, r, g, b, a, t);
    }

    public static void DrawIsoCircle(float x, float y, float z, float radius, float r, float g, float b, float a) {
        int segments = 16;
        LineDrawer.DrawIsoCircle(x, y, z, radius, 16, r, g, b, a);
    }

    public static void DrawIsoCircle(float x, float y, float z, float radius, int segments, float r, float g, float b, float a) {
        double lx = (double)x + (double)radius * Math.cos(Math.toRadians(0.0 / (double)segments));
        double ly = (double)y + (double)radius * Math.sin(Math.toRadians(0.0 / (double)segments));
        for (int i = 1; i <= segments; ++i) {
            double cx = (double)x + (double)radius * Math.cos(Math.toRadians((double)i * 360.0 / (double)segments));
            double cy = (double)y + (double)radius * Math.sin(Math.toRadians((double)i * 360.0 / (double)segments));
            LineDrawer.addLine((float)lx, (float)ly, z, (float)cx, (float)cy, z, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    static void drawLine(float x, float y, float x2, float y2, float r, float g, float b) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, 1.0f, 1);
    }

    public static void drawLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, a, thickness);
    }

    public static void drawLine(float x, float y, float x2, float y2, float r, float g, float b, float a, float baseThickness, float topThickness) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, a, baseThickness, topThickness);
    }

    public static void drawRect(float x, float y, float width, float height, float r, float g, float b, float a, int thickness) {
        SpriteRenderer.instance.render(null, x, y + (float)thickness, thickness, height - (float)(thickness * 2), r, g, b, a, null);
        SpriteRenderer.instance.render(null, x, y, width, thickness, r, g, b, a, null);
        SpriteRenderer.instance.render(null, x + width - (float)thickness, y + (float)thickness, thickness, height - (float)(thickness * 2), r, g, b, a, null);
        SpriteRenderer.instance.render(null, x, y + height - (float)thickness, width, thickness, r, g, b, a, null);
    }

    public static void drawArc(float cx, float cy, float cz, float radius, float direction, float angle, int segments, float r, float g, float b, float a) {
        float startAngleRadians = direction + (float)Math.acos(angle);
        float endAngleRadians = direction - (float)Math.acos(angle);
        float x1 = cx + (float)Math.cos(startAngleRadians) * radius;
        float y1 = cy + (float)Math.sin(startAngleRadians) * radius;
        for (int i = 1; i <= segments; ++i) {
            float angleRadians = startAngleRadians + (endAngleRadians - startAngleRadians) * (float)i / (float)segments;
            float x2 = cx + (float)Math.cos(angleRadians) * radius;
            float y2 = cy + (float)Math.sin(angleRadians) * radius;
            LineDrawer.DrawIsoLine(x1, y1, cz, x2, y2, cz, r, g, b, a, 1);
            x1 = x2;
            y1 = y2;
        }
    }

    public static void drawCircle(float x, float y, float radius, int segments, float r, float g, float b) {
        double lx = (double)x + (double)radius * Math.cos(Math.toRadians(0.0 / (double)segments));
        double ly = (double)y + (double)radius * Math.sin(Math.toRadians(0.0 / (double)segments));
        for (int i = 1; i <= segments; ++i) {
            double cx = (double)x + (double)radius * Math.cos(Math.toRadians((double)i * 360.0 / (double)segments));
            double cy = (double)y + (double)radius * Math.sin(Math.toRadians((double)i * 360.0 / (double)segments));
            LineDrawer.drawLine((float)lx, (float)ly, (float)cx, (float)cy, r, g, b, 1.0f, 1);
            lx = cx;
            ly = cy;
        }
    }

    public static void drawDirectionLine(float cx, float cy, float cz, float radius, float radians, float r, float g, float b, float a, int thickness) {
        float x2 = cx + (float)Math.cos(radians) * radius;
        float y2 = cy + (float)Math.sin(radians) * radius;
        LineDrawer.DrawIsoLine(cx, cy, cz, x2, y2, cz, r, g, b, a, thickness);
    }

    public static void drawDirectionLine(float cx, float cy, float cz, float radius, float radians, float r, float g, float b, float a, float baseThickness, float topThickness) {
        float x2 = cx + (float)Math.cos(radians) * radius;
        float y2 = cy + (float)Math.sin(radians) * radius;
        LineDrawer.DrawIsoLine(cx, cy, cz, x2, y2, cz, r, g, b, a, baseThickness, topThickness);
    }

    public static void drawDotLines(float cx, float cy, float cz, float radius, float direction, float dot, float r, float g, float b, float a, int thickness) {
        LineDrawer.drawDirectionLine(cx, cy, cz, radius, direction + (float)Math.acos(dot), r, g, b, a, thickness);
        LineDrawer.drawDirectionLine(cx, cy, cz, radius, direction - (float)Math.acos(dot), r, g, b, a, thickness);
    }

    public static void addAlphaDecayingIsoCircle(float x, float y, float z, float radius, int segments, float r, float g, float b, float a) {
        double lx = (double)x + (double)radius * Math.cos(Math.toRadians(0.0 / (double)segments));
        double ly = (double)y + (double)radius * Math.sin(Math.toRadians(0.0 / (double)segments));
        for (int i = 1; i <= segments; ++i) {
            double cx = (double)x + (double)radius * Math.cos(Math.toRadians((double)i * 360.0 / (double)segments));
            double cy = (double)y + (double)radius * Math.sin(Math.toRadians((double)i * 360.0 / (double)segments));
            LineDrawer.addAlphaDecayingLine((float)lx, (float)ly, z, (float)cx, (float)cy, z, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    public static void addAlphaDecayingLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        alphaDecayingLines.add(line.init(x, y, z, x2, y2, z2, r, g, b, a));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, boolean bLine) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        alphaDecayingLines.add(line.init(x, y, z, x2, y2, z2, r, g, b, "", bLine));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x2, y2, z2, r, g, b, a));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, int r, int g, int b, String name) {
        LineDrawer.addLine(x, y, z, x2, y2, z2, r, g, b, name, true);
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name, boolean bLine) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x2, y2, z2, r, g, b, name, bLine));
    }

    public static void addRect(float x, float y, float z, float width, float height, float r, float g, float b) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x + width, y + height, z, r, g, b, null, false));
    }

    public static void addRectYOffset(float x, float y, float z, float width, float height, int yOffset, float r, float g, float b) {
        DrawableLine line = pool.isEmpty() ? new DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x + width, y + height, z, r, g, b, null, false));
        line.yPixelOffset = yOffset;
    }

    public static void clear() {
        if (lines.isEmpty()) {
            return;
        }
        for (int i = 0; i < lines.size(); ++i) {
            pool.push(lines.get(i));
        }
        lines.clear();
    }

    public void removeLine(String name) {
        for (int i = 0; i < lines.size(); ++i) {
            if (!LineDrawer.lines.get((int)i).name.equals(name)) continue;
            lines.remove(lines.get(i));
            --i;
        }
    }

    public static void render() {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.StartShader(0);
            IndieGL.disableDepthTest();
            IndieGL.glBlendFunc(770, 771);
        }
        for (int n = 0; n < lines.size(); ++n) {
            DrawableLine line2 = lines.get(n);
            if (!line2.line) {
                LineDrawer.DrawIsoRect(line2.xstart, line2.ystart, line2.xend - line2.xstart, line2.yend - line2.ystart, (int)line2.zstart, line2.yPixelOffset, line2.red, line2.green, line2.blue);
                continue;
            }
            LineDrawer.DrawIsoLine(line2.xstart, line2.ystart, line2.zstart, line2.xend, line2.yend, line2.zend, line2.red, line2.green, line2.blue, line2.alpha, 1);
        }
        for (DrawableLine line2 : alphaDecayingLines) {
            if (!line2.line) {
                LineDrawer.DrawIsoRect(line2.xstart, line2.ystart, line2.xend - line2.xstart, line2.yend - line2.ystart, (int)line2.zstart, line2.yPixelOffset, line2.red, line2.green, line2.blue);
            } else {
                LineDrawer.DrawIsoLine(line2.xstart, line2.ystart, line2.zstart, line2.xend, line2.yend, line2.zend, line2.red, line2.green, line2.blue, line2.alpha, 1);
            }
            line2.alpha -= 0.01f;
            if (!(line2.alpha < 0.0f)) continue;
            pool.push(line2);
        }
        alphaDecayingLines.removeIf(line -> line.alpha < 0.0f);
    }

    public static void drawLines() {
        LineDrawer.clear();
    }

    public static class DrawableLine {
        public boolean line;
        private String name;
        private float red;
        private float green;
        private float blue;
        private float alpha;
        private float xstart;
        private float ystart;
        private float zstart;
        private float xend;
        private float yend;
        private float zend;
        private int yPixelOffset;

        public DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = 1.0f;
            this.name = name;
            this.yPixelOffset = 0;
            return this;
        }

        public DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name, boolean bLine) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = 1.0f;
            this.name = name;
            this.line = bLine;
            this.yPixelOffset = 0;
            return this;
        }

        public DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = a;
            this.name = null;
            this.line = true;
            this.yPixelOffset = 0;
            return this;
        }

        public boolean equals(Object o) {
            if (o instanceof DrawableLine) {
                DrawableLine drawableLine = (DrawableLine)o;
                return drawableLine.name.equals(this.name);
            }
            return o.equals(this);
        }
    }
}

