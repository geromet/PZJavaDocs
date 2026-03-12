/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.streets;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector2f;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.math.PZMath;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperOffset;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.streets.CharLayout;
import zombie.worldMap.streets.ClosestPoint;
import zombie.worldMap.streets.Intersection;
import zombie.worldMap.streets.PointOn;
import zombie.worldMap.streets.StreetPoints;
import zombie.worldMap.streets.StreetRenderData;
import zombie.worldMap.streets.WorldMapStreets;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

public final class WorldMapStreet {
    static final ObjectPool<WorldMapStreet> s_pool = new ObjectPool<WorldMapStreet>(WorldMapStreet::new);
    static final ObjectPool<StreetPoints> s_pointsPool = new ObjectPool<StreetPoints>(StreetPoints::new);
    private static final ClosestPoint s_closestPoint = new ClosestPoint();
    private static final PointOn s_pointOn = new PointOn();
    private static final StreetPoints s_clipped = new StreetPoints();
    private static final StreetPoints s_reverse = new StreetPoints();
    static final ObjectPool<CharLayout> s_charLayoutPool = new ObjectPool<CharLayout>(CharLayout::new);
    private static final ArrayList<CharLayout> s_layout = new ArrayList();
    private static final TFloatArrayList triangles2 = new TFloatArrayList();
    private static double cosA;
    private static double sinA;
    private static double x;
    private static double y;
    private static double xAlongLine;
    private static final int GAP = 200;
    private static double fontScale;
    private static float sdfThreshold;
    private static float shadow;
    private static float outlineThickness;
    private static float outlineR;
    private static float outlineG;
    private static float outlineB;
    private static float outlineA;
    private WorldMapStreets owner;
    private String translatedText;
    private StreetPoints points;
    private int width = 5;
    private final ArrayList<Intersection> intersections = new ArrayList();
    WorldMapStreet connectedToStart;
    WorldMapStreet connectedToEnd;
    final ArrayList<WorldMapStreet> splitStreets = new ArrayList();
    long changeCount;
    static final LayoutCounts countsForward;
    static final LayoutCounts countsBackward;
    static Clipper clipper;

    private WorldMapStreet() {
    }

    public WorldMapStreet(WorldMapStreets owner, String translatedText, StreetPoints points) {
        this.owner = owner;
        this.translatedText = translatedText;
        this.points = points;
        this.points.calculateBounds();
    }

    WorldMapStreet init(WorldMapStreets owner, String translatedText, StreetPoints points) {
        this.owner = owner;
        this.translatedText = translatedText;
        if (this.points != null) {
            s_pointsPool.release(this.points);
        }
        this.points = points;
        this.points.calculateBounds();
        this.intersections.clear();
        this.splitStreets.clear();
        this.changeCount = 0L;
        return this;
    }

    public WorldMapStreets getOwner() {
        return this.owner;
    }

    public float getMinX() {
        return this.points.getMinX();
    }

    public float getMinY() {
        return this.points.getMinY();
    }

    public float getMaxX() {
        return this.points.getMaxX();
    }

    public float getMaxY() {
        return this.points.getMaxY();
    }

    public int getNumPoints() {
        return this.points.numPoints();
    }

    public float getPointX(int index) {
        return this.points.getX(index);
    }

    public float getPointY(int index) {
        return this.points.getY(index);
    }

    public float getLength(UIWorldMap ui) {
        return this.points.calculateLength(ui);
    }

    public float getLengthSquared(UIWorldMap ui) {
        float length = this.getLength(ui);
        return length * length;
    }

    public void addPoint(float x, float y) {
        this.points.add(x);
        this.points.add(y);
        this.points.invalidateBounds();
    }

    public void insertPoint(int index, float x, float y) {
        this.points.insert(index * 2, x);
        this.points.insert(index * 2 + 1, y);
        this.points.invalidateBounds();
    }

    public void removePoint(int index) {
        this.points.removeAt(index * 2);
        this.points.removeAt(index * 2);
        this.points.invalidateBounds();
    }

    public void setPoint(int index, float x, float y) {
        this.points.set(index * 2, x);
        this.points.set(index * 2 + 1, y);
        this.points.invalidateBounds();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return this.width;
    }

    public void reverseDirection() {
        this.points.reverse();
    }

    public String getTranslatedText() {
        return this.translatedText;
    }

    public void setTranslatedText(String text) {
        this.translatedText = StringUtils.isNullOrWhitespace(text) ? "" : text;
    }

    public UIFont getFont(UIWorldMap ui) {
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        return textLayer.getFont();
    }

    public double getFontScale(UIWorldMap ui) {
        float worldScale = ui.getAPI().getWorldScale();
        worldScale = PZMath.clamp(worldScale, 1.9805f, 3.66f);
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        return (double)(worldScale *= textLayer.calculateScale(ui)) * 0.2;
    }

    public ArrayList<Intersection> getIntersections() {
        return this.intersections;
    }

    public void resetIntersectionRenderFlag() {
        for (int i = 0; i < this.getIntersections().size(); ++i) {
            Intersection intersection = this.getIntersections().get(i);
            intersection.renderedLabelOver = false;
        }
    }

    boolean setIntersectionRenderFlag(UIWorldMap ui, float start, float end) {
        boolean set = false;
        for (int i = 0; i < this.getIntersections().size(); ++i) {
            Intersection intersection = this.getIntersections().get(i);
            if (!(intersection.getDistanceFromStart(ui, this) >= start - 10.0f) || !(intersection.getDistanceFromStart(ui, this) <= end + 10.0f)) continue;
            intersection.renderedLabelOver = true;
            set = true;
        }
        return set;
    }

    Intersection getMatchingIntersection(WorldMapStreet street, float worldX, float worldY) {
        for (int i = 0; i < this.getIntersections().size(); ++i) {
            Intersection intersection = this.getIntersections().get(i);
            if (street != null && intersection.street != street || !PZMath.equal(intersection.worldX, worldX, 0.01f) || !PZMath.equal(intersection.worldY, worldY)) continue;
            return intersection;
        }
        return null;
    }

    boolean overlapsAnotherLabel(UIWorldMap ui, float start, float end) {
        for (int i = 0; i < this.getIntersections().size(); ++i) {
            Intersection intersectionOther;
            Intersection intersection = this.getIntersections().get(i);
            if (!(intersection.getDistanceFromStart(ui, this) >= start) || !(intersection.getDistanceFromStart(ui, this) <= end) || (intersectionOther = intersection.street.getMatchingIntersection(null, intersection.worldX, intersection.worldY)) == null || !intersectionOther.renderedLabelOver) continue;
            return true;
        }
        return false;
    }

    public boolean getPointOn(UIWorldMap ui, float t, PointOn pointOn) {
        t = PZMath.clamp_01(t);
        pointOn.y = 0.0f;
        pointOn.x = 0.0f;
        pointOn.worldY = 0.0f;
        pointOn.worldX = 0.0f;
        pointOn.segment = -1;
        float length = this.getLength(ui);
        if (length <= 0.0f) {
            return false;
        }
        float distanceFromStart = length * t;
        float segmentStart = 0.0f;
        for (int i = 0; i < this.getNumPoints() - 1; ++i) {
            float y2;
            float worldX1 = this.points.getX(i);
            float worldY1 = this.points.getY(i);
            float worldX2 = this.points.getX(i + 1);
            float worldY2 = this.points.getY(i + 1);
            float x1 = ui.getAPI().worldToUIX(worldX1, worldY1);
            float y1 = ui.getAPI().worldToUIY(worldX1, worldY1);
            float x2 = ui.getAPI().worldToUIX(worldX2, worldY2);
            float segmentLength = Vector2f.length(x2 - x1, (y2 = ui.getAPI().worldToUIY(worldX2, worldY2)) - y1);
            if (segmentStart + segmentLength >= distanceFromStart) {
                float f = (distanceFromStart - segmentStart) / segmentLength;
                pointOn.x = x1 + (x2 - x1) * f;
                pointOn.y = y1 + (y2 - y1) * f;
                pointOn.worldX = worldX1 + (worldX2 - worldX1) * f;
                pointOn.worldY = worldY1 + (worldY2 - worldY1) * f;
                pointOn.segment = i;
                pointOn.distFromSegmentStart = distanceFromStart - segmentStart;
                pointOn.distanceFromStart = distanceFromStart;
                return true;
            }
            segmentStart += segmentLength;
        }
        return false;
    }

    @Deprecated
    void splitAtOccludedIntersections(ArrayList<WorldMapStreet> split) {
        StreetPoints points1 = s_reverse;
        points1.clear();
        points1.addAll(this.points);
        for (int i = this.getIntersections().size() - 1; i >= 0; --i) {
            Intersection intersection = this.getIntersections().get(i);
            Intersection intersectionOther = intersection.street.getMatchingIntersection(intersection.street, intersection.worldX, intersection.worldY);
            if (intersectionOther == null || !intersectionOther.renderedLabelOver) continue;
            WorldMapStreet street2 = s_pool.alloc();
            StreetPoints points2 = street2.points == null ? new StreetPoints() : street2.points;
            points2.clear();
            points2.add(intersection.worldX);
            points2.add(intersection.worldY);
            for (int j = (intersection.segment + 1) * 2; j < points1.size(); ++j) {
                points2.add(points1.get(j));
            }
            street2.init(this.owner, this.getTranslatedText(), points2);
            split.add(0, street2);
            points1.remove((intersection.segment + 1) * 2, points1.size() - (intersection.segment + 1) * 2);
            points1.add(intersection.worldX);
            points1.add(intersection.worldY);
        }
        if (!points1.isEmpty() && !split.isEmpty()) {
            WorldMapStreet street2 = s_pool.alloc();
            StreetPoints points2 = street2.points == null ? new StreetPoints() : street2.points;
            points2.clear();
            points2.addAll(points1);
            street2.init(this.owner, this.getTranslatedText(), points2);
            split.add(0, street2);
        }
    }

    public void render(UIWorldMap ui, StreetRenderData renderData) {
        int i;
        if (this.splitStreets.isEmpty()) {
            this.render2(ui, null, renderData);
        } else {
            for (i = 0; i < this.splitStreets.size(); ++i) {
                WorldMapStreet street = this.splitStreets.get(i);
                street.render2(ui, this, renderData);
            }
        }
        if (ui.getAPI().getBoolean("OutlineStreets")) {
            this.createPolygon(renderData.polygon);
            for (i = 0; i < renderData.polygon.size() / 2; ++i) {
                float x1 = renderData.polygon.get(i * 2);
                float y1 = renderData.polygon.get(i * 2 + 1);
                int j = (i + 1) % (renderData.polygon.size() / 2);
                float x2 = renderData.polygon.get(j * 2);
                float y2 = renderData.polygon.get(j * 2 + 1);
                this.renderLine(ui, x1, y1, x2, y2, 0.0f, 0.0f, 1.0f, 1.0f, 1, renderData);
            }
        }
    }

    float getDistanceAlongOriginalStreet(UIWorldMap ui, float t, WorldMapStreet originalStreet) {
        this.getPointOn(ui, t, s_pointOn);
        originalStreet.getClosestPointOn(ui, WorldMapStreet.s_pointOn.x, WorldMapStreet.s_pointOn.y, s_closestPoint);
        return WorldMapStreet.s_closestPoint.distanceFromStart;
    }

    void render2(UIWorldMap ui, WorldMapStreet originalStreet, StreetRenderData renderData) {
        UIFont uiFont = this.getFont(ui);
        AngelCodeFont font = TextManager.instance.getFontFromEnum(uiFont);
        float streetLength = this.getLength(ui);
        fontScale = this.getFontScale(ui);
        double textWidth = (double)font.getWidth(this.getTranslatedText()) * fontScale;
        if (textWidth > (double)streetLength) {
            return;
        }
        float lx1 = 0.0f;
        float ly1 = 0.0f;
        float lx2 = 10.0f;
        float ly2 = 0.0f;
        double radian = (float)Math.atan2(-0.0, 10.0);
        cosA = Math.cos(radian);
        sinA = Math.sin(radian);
        sdfThreshold = this.getSdfThreshold(ui);
        shadow = 0.0f;
        outlineThickness = 0.25f;
        outlineR = 0.0f;
        outlineG = 0.0f;
        outlineB = 0.0f;
        outlineA = 1.0f;
        if (!TextManager.instance.isSdf(uiFont)) {
            outlineThickness = 0.0f;
            outlineA = 0.0f;
        }
        this.render2b(ui, originalStreet, renderData, (float)textWidth, streetLength, 0.0f, 1.0f);
    }

    void render2b(UIWorldMap ui, WorldMapStreet originalStreet, StreetRenderData renderData, float textWidth, float streetLength, float startFractionWS, float endFractionWS) {
        boolean bOnScreen;
        float middleFraction = startFractionWS + (endFractionWS - startFractionWS) / 2.0f;
        this.getPointOn(ui, middleFraction, s_pointOn);
        double xAlongStreet = WorldMapStreet.s_pointOn.distanceFromStart - textWidth / 2.0f;
        float minX = WorldMapStreet.s_pointOn.x - textWidth / 2.0f;
        float minY = WorldMapStreet.s_pointOn.y - textWidth / 2.0f;
        float maxX = WorldMapStreet.s_pointOn.x + textWidth / 2.0f;
        float maxY = WorldMapStreet.s_pointOn.y + textWidth / 2.0f;
        boolean bRendered = false;
        boolean bl = bOnScreen = (double)minX < ui.getWidth() && maxX > 0.0f && (double)minY < ui.getHeight() && maxY > 0.0f;
        if (!bOnScreen) {
            boolean bl2 = true;
        } else if (originalStreet != null) {
            float end;
            float start = this.getDistanceAlongOriginalStreet(ui, (float)xAlongStreet / streetLength, originalStreet);
            if (originalStreet.overlapsAnotherLabel(ui, start, end = this.getDistanceAlongOriginalStreet(ui, (float)(xAlongStreet + (double)textWidth) / streetLength, originalStreet))) {
                this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 1.0f, 0.0f, 0.0f, 0.5f, 3, renderData);
            } else if (this.shouldRenderForward(ui, textWidth, (float)xAlongStreet, streetLength)) {
                if (originalStreet.setIntersectionRenderFlag(ui, start, end)) {
                    this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 0.0f, 1.0f, 0.0f, 0.75f, 3, renderData);
                }
                this.renderForward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
                bRendered = true;
            } else {
                if (originalStreet.setIntersectionRenderFlag(ui, start, end)) {
                    this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 0.0f, 1.0f, 0.0f, 0.75f, 3, renderData);
                }
                this.renderBackward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
                bRendered = true;
            }
        } else if (this.overlapsAnotherLabel(ui, (float)xAlongStreet, (float)(xAlongStreet + (double)textWidth))) {
            this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 1.0f, 0.0f, 0.0f, 0.5f, 3, renderData);
        } else if (this.shouldRenderForward(ui, textWidth, (float)xAlongStreet, streetLength)) {
            if (this.setIntersectionRenderFlag(ui, (float)xAlongStreet, (float)(xAlongStreet + (double)textWidth))) {
                this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 0.0f, 1.0f, 0.0f, 0.75f, 3, renderData);
            }
            this.renderForward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
            bRendered = true;
        } else {
            if (this.setIntersectionRenderFlag(ui, (float)xAlongStreet, (float)(xAlongStreet + (double)textWidth))) {
                this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + (double)textWidth) / streetLength, 0.0f, 1.0f, 0.0f, 0.75f, 3, renderData);
            }
            this.renderBackward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
            bRendered = true;
        }
        float middleLeft = startFractionWS + (middleFraction - startFractionWS) / 2.0f;
        if (bRendered || !bOnScreen) {
            if (middleFraction * streetLength - textWidth / 2.0f >= middleLeft * streetLength + textWidth / 2.0f + 200.0f) {
                this.render2b(ui, originalStreet, renderData, textWidth, streetLength, startFractionWS, middleFraction);
                this.render2b(ui, originalStreet, renderData, textWidth, streetLength, middleFraction, endFractionWS);
            }
        } else if (middleFraction * streetLength >= middleLeft * streetLength + textWidth / 2.0f + 200.0f) {
            this.render2b(ui, originalStreet, renderData, textWidth, streetLength, startFractionWS, middleFraction);
            this.render2b(ui, originalStreet, renderData, textWidth, streetLength, middleFraction, endFractionWS);
        }
    }

    void renderSection(UIWorldMap ui, float start, float end, float r, float g, float b, float a, int thickness, StreetRenderData renderData) {
    }

    public void renderLines(UIWorldMap ui, float r, float g, float b, float a, int thickness, StreetRenderData renderData) {
        for (int i = 0; i < this.getNumPoints() - 1; ++i) {
            float x1 = this.points.getX(i);
            float y1 = this.points.getY(i);
            float x2 = this.points.getX(i + 1);
            float y2 = this.points.getY(i + 1);
            this.renderLine(ui, x1, y1, x2, y2, r, g, b, a, thickness, renderData);
        }
    }

    void renderLine(UIWorldMap ui, float worldX1, float worldY1, float worldX2, float worldY2, float r, float g, float b, float a, int thickness, StreetRenderData renderData) {
        float uiX1 = ui.getAPI().worldToUIX(worldX1, worldY1);
        float uiY1 = ui.getAPI().worldToUIY(worldX1, worldY1);
        float uiX2 = ui.getAPI().worldToUIX(worldX2, worldY2);
        float uiY2 = ui.getAPI().worldToUIY(worldX2, worldY2);
        if (renderData == null) {
            ui.DrawLine(null, uiX1, uiY1, uiX2, uiY2, thickness, r, g, b, a);
            return;
        }
        renderData.lines.add(uiX1);
        renderData.lines.add(uiY1);
        renderData.lines.add(uiX2);
        renderData.lines.add(uiY2);
        renderData.lines.add(r);
        renderData.lines.add(g);
        renderData.lines.add(b);
        renderData.lines.add(a);
        renderData.lines.add(thickness);
    }

    public void renderIntersections(UIWorldMap ui, float r, float g, float b, float a) {
        for (Intersection ixn : this.getIntersections()) {
            float x1 = ixn.worldX - 1.0f;
            float y1 = ixn.worldY - 1.0f;
            float x2 = ixn.worldX + 1.0f;
            float y2 = ixn.worldY + 1.0f;
            int thickness = ixn.renderedLabelOver ? 3 : 1;
            this.renderLine(ui, x1, y1, x2, y1, r, g, b, a, thickness, null);
            this.renderLine(ui, x2, y1, x2, y2, r, g, b, a, thickness, null);
            this.renderLine(ui, x2, y2, x1, y2, r, g, b, a, thickness, null);
            this.renderLine(ui, x1, y2, x1, y1, r, g, b, a, thickness, null);
        }
    }

    private boolean shouldRenderForward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength) {
        this.layoutForward(ui, xAlongStreet, streetLength);
        this.countUpsideDownCharacters(countsForward);
        boolean bForwardOK = false;
        if (WorldMapStreet.countsForward.upsideDown == 0) {
            return true;
        }
        if (WorldMapStreet.countsForward.total == WorldMapStreet.countsForward.up) {
            bForwardOK = true;
        }
        if (WorldMapStreet.countsForward.upsideDown == WorldMapStreet.countsForward.up) {
            bForwardOK = true;
        }
        this.layoutBackward(ui, textWidth, xAlongStreet, streetLength);
        this.countUpsideDownCharacters(countsBackward);
        if (WorldMapStreet.countsBackward.upsideDown == 0) {
            return false;
        }
        if (bForwardOK) {
            return true;
        }
        if (WorldMapStreet.countsBackward.upsideDown == WorldMapStreet.countsBackward.up) {
            return false;
        }
        return WorldMapStreet.countsForward.upsideDown < WorldMapStreet.countsBackward.upsideDown;
    }

    private void countUpsideDownCharacters(LayoutCounts counts) {
        counts.down = 0;
        counts.up = 0;
        counts.upsideDown = 0;
        counts.total = 0;
        for (int i = 0; i < s_layout.size(); ++i) {
            boolean bUpright;
            CharLayout charLayout = s_layout.get(i);
            double dx = charLayout.rightTop[0] - charLayout.leftTop[0];
            double dy = charLayout.rightTop[1] - charLayout.leftTop[1];
            if (dx == 0.0 && dy == 0.0) continue;
            ++counts.total;
            double radians = (float)Math.atan2(-dy, dx);
            long degrees = Math.round(radians * 57.2957763671875);
            if (degrees < 0L) {
                degrees += 360L;
            }
            if (degrees == 90L) {
                ++counts.up;
            }
            if (degrees == 270L) {
                ++counts.down;
            }
            boolean bl = bUpright = degrees >= 0L && degrees < 90L || degrees >= 270L;
            if (bUpright) continue;
            ++counts.upsideDown;
        }
    }

    public void clipToObscuredCells() {
        int i;
        s_pool.releaseAll((List<WorldMapStreet>)this.splitStreets);
        this.splitStreets.clear();
        s_clipped.clear();
        if (clipper == null) {
            clipper = new Clipper();
        }
        clipper.clear();
        if (IsoMetaGrid.clipperOffset == null) {
            IsoMetaGrid.clipperOffset = new ClipperOffset();
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
        }
        ByteBuffer bb = IsoMetaGrid.clipperBuffer;
        bb.clear();
        if (this.points.isClockwise()) {
            for (i = this.getNumPoints() - 1; i >= 0; --i) {
                bb.putFloat(this.getPointX(i));
                bb.putFloat(this.getPointY(i));
            }
        } else {
            for (i = 0; i < this.getNumPoints(); ++i) {
                bb.putFloat(this.getPointX(i));
                bb.putFloat(this.getPointY(i));
            }
        }
        clipper.addPath(this.getNumPoints(), bb, false, false);
        for (i = 0; i < this.getOwner().obscuredCells.size() / 2; ++i) {
            int cellX = this.getOwner().obscuredCells.get(i * 2);
            int cellY = this.getOwner().obscuredCells.get(i * 2 + 1);
            clipper.clipAABB(cellX * 300, cellY * 300, (cellX + 1) * 300, (cellY + 1) * 300);
        }
        int numPolygons = clipper.generatePolygons();
        for (int i2 = 0; i2 < numPolygons; ++i2) {
            bb.clear();
            clipper.getPolygon(i2, bb);
            int numPoints = bb.getShort();
            if (numPoints < 2) continue;
            StreetPoints points = s_pointsPool.alloc();
            points.clear();
            for (int j = 0; j < numPoints; ++j) {
                float x = bb.getFloat();
                float y = bb.getFloat();
                points.add(x, y);
            }
            WorldMapStreet street = s_pool.alloc().init(this.getOwner(), this.getTranslatedText(), points);
            street.setWidth(this.getWidth());
            this.splitStreets.add(street);
        }
    }

    private double layoutForward(UIWorldMap ui, float xAlongStreet, float streetLength) {
        s_charLayoutPool.releaseAll((List<CharLayout>)s_layout);
        s_layout.clear();
        float t = xAlongStreet / streetLength;
        this.getPointOn(ui, t, s_pointOn);
        if (WorldMapStreet.s_pointOn.segment == -1) {
            return xAlongStreet;
        }
        int firstChar = 0;
        xAlongLine = WorldMapStreet.s_pointOn.distFromSegmentStart;
        for (int i = WorldMapStreet.s_pointOn.segment; i < this.points.numPoints() - 1; ++i) {
            float x1 = this.points.getX(i);
            float y1 = this.points.getY(i);
            float x2 = this.points.getX(i + 1);
            float y2 = this.points.getY(i + 1);
            float uiX1 = ui.getAPI().worldToUIX(x1, y1);
            float uiY1 = ui.getAPI().worldToUIY(x1, y1);
            float uiX2 = ui.getAPI().worldToUIX(x2, y2);
            float uiY2 = ui.getAPI().worldToUIY(x2, y2);
            float lineLength = IsoUtils.DistanceTo(uiX1, uiY1, uiX2, uiY2);
            while (xAlongLine < (double)lineLength) {
                double xAlongLine1 = xAlongLine;
                int numCharsRendered = this.layoutTextAlongLine(ui, firstChar, uiX1, uiY1, uiX2, uiY2);
                if (numCharsRendered < 1) break;
                xAlongStreet += (float)(xAlongLine - xAlongLine1);
                if ((firstChar += numCharsRendered) < this.translatedText.length()) continue;
                return xAlongStreet;
            }
            xAlongLine = Math.max(0.0, xAlongLine - (double)lineLength);
        }
        return xAlongStreet;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private double layoutBackward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength) {
        StreetPoints oldPoints = this.points;
        this.points.setReverse(s_reverse);
        this.points = s_reverse;
        try {
            double xAlongStreet1 = this.layoutForward(ui, (float)((double)streetLength - ((double)xAlongStreet + textWidth)), streetLength);
            double d = (double)xAlongStreet + textWidth;
            return d;
        }
        finally {
            this.points = oldPoints;
        }
    }

    private double renderForward(UIWorldMap ui, float textWidth, float xAlongStreet, float streetLength, StreetRenderData renderData) {
        xAlongStreet = (float)this.layoutForward(ui, xAlongStreet, streetLength);
        PZArrayUtil.addAll(renderData.characters, s_layout);
        s_layout.clear();
        return xAlongStreet;
    }

    private double renderBackward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength, StreetRenderData renderData) {
        xAlongStreet = (float)this.layoutBackward(ui, textWidth, xAlongStreet, streetLength);
        PZArrayUtil.addAll(renderData.characters, s_layout);
        s_layout.clear();
        return xAlongStreet;
    }

    private int layoutTextAlongLine(UIWorldMap ui, int firstChar, float lx1, float ly1, float lx2, float ly2) {
        int numCharsRendered = 0;
        x = lx1;
        y = ly1;
        float lineLength = IsoUtils.DistanceTo(lx1, ly1, lx2, ly2);
        double radian = (float)Math.atan2(-(ly2 - ly1), lx2 - lx1);
        cosA = Math.cos(radian);
        sinA = Math.sin(radian);
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        WorldMapStyleLayer.RGBAf rgbaf = textLayer.evalColor(ui.getAPI().getZoomF(), textLayer.fill);
        AngelCodeFont font = TextManager.instance.getFontFromEnum(this.getFont(ui));
        AngelCodeFont.CharDef lastCharDef = null;
        double scale = fontScale;
        double x = xAlongLine / scale;
        double y = (double)(-font.getHeight("Marvin Place", false, false)) / 2.0;
        double textWidth = 0.0;
        double dx = 0.0;
        double dy = 0.0;
        for (int i = firstChar; i < this.translatedText.length(); ++i) {
            AngelCodeFont.CharDef charDef;
            int ch = this.translatedText.charAt(i);
            if (ch >= font.chars.length) {
                ch = 63;
            }
            if ((charDef = font.chars[ch]) == null) continue;
            if (lastCharDef != null) {
                x += (double)lastCharDef.getKerning(ch);
            }
            if (i > firstChar && (x + (double)charDef.xadvance) * scale >= (double)lineLength) break;
            if (charDef.width > 0 && charDef.height > 0) {
                double x0 = x + (double)charDef.xoffset - 0.0;
                double y0 = y + (double)charDef.yoffset - 0.0;
                double x1 = x0 + (double)charDef.width;
                double y1 = y0 + (double)charDef.height;
                CharLayout charLayout = s_charLayoutPool.alloc();
                charLayout.charDef = charDef;
                this.getAbsolutePosition(ui, x0, y0, charLayout.leftTop);
                this.getAbsolutePosition(ui, x1, y0, charLayout.rightTop);
                this.getAbsolutePosition(ui, x1, y1, charLayout.rightBottom);
                this.getAbsolutePosition(ui, x0, y1, charLayout.leftBottom);
                charLayout.r = rgbaf.r;
                charLayout.g = rgbaf.g;
                charLayout.b = rgbaf.b;
                charLayout.a = rgbaf.a;
                charLayout.sdfThreshold = sdfThreshold;
                charLayout.sdfShadow = shadow;
                charLayout.outlineThickness = outlineThickness;
                charLayout.outlineR = outlineR;
                charLayout.outlineG = outlineG;
                charLayout.outlineB = outlineB;
                charLayout.outlineA = outlineA * charLayout.a;
                s_layout.add(charLayout);
            }
            lastCharDef = charDef;
            x += (double)charDef.xadvance;
            textWidth += (double)charDef.xadvance;
            ++numCharsRendered;
        }
        xAlongLine += textWidth * scale;
        WorldMapStyleLayer.RGBAf.s_pool.release(rgbaf);
        return numCharsRendered;
    }

    private float getSdfThreshold(UIWorldMap ui) {
        double[] p0 = this.getAbsolutePosition(ui, -5.0, 0.0, L_getSdfThreshold.p0);
        double[] p1 = this.getAbsolutePosition(ui, 5.0, 0.0, L_getSdfThreshold.p1);
        double distance = Math.hypot(p0[0] - p1[0], p0[1] - p1[1]);
        return (float)(0.125 / (distance / 10.0));
    }

    private double[] getAbsolutePosition(UIWorldMap ui, double localX, double localY, double[] xy) {
        double scale = fontScale;
        xy[0] = (localX *= scale) * cosA + (localY *= scale) * sinA + x;
        xy[1] = -localX * sinA + localY * cosA + y;
        return xy;
    }

    public float getClosestPointOn(UIWorldMap ui, float uiX, float uiY, ClosestPoint closestPoint) {
        double closestDistSq = Double.MAX_VALUE;
        Vector2f closest = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0f;
        for (int i = 0; i < this.getNumPoints() - 1; ++i) {
            float uiY2;
            float uiX2;
            float uiY1;
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float x2 = this.getPointX(i + 1);
            float y2 = this.getPointY(i + 1);
            float uiX1 = ui.getAPI().worldToUIX(x1, y1);
            double distSq = PZMath.closestPointOnLineSegment(uiX1, uiY1 = ui.getAPI().worldToUIY(x1, y1), uiX2 = ui.getAPI().worldToUIX(x2, y2), uiY2 = ui.getAPI().worldToUIY(x2, y2), uiX, uiY, 0.0, closest);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPoint.x = ui.getAPI().uiToWorldX(closest.x, closest.y);
                closestPoint.y = ui.getAPI().uiToWorldY(closest.x, closest.y);
                closestPoint.distSq = (float)distSq;
                closestPoint.distanceFromStart = distanceFromStart + Vector2f.length(closest.x - uiX1, closest.y - uiY1);
                closestPoint.index = i;
            }
            distanceFromStart += Vector2f.length(uiX2 - uiX1, uiY2 - uiY1);
        }
        BaseVehicle.releaseVector2f(closest);
        return closestPoint.distSq;
    }

    public float getClosestPointOn(float worldX, float worldY, ClosestPoint closestPoint) {
        double closestDistSq = Double.MAX_VALUE;
        Vector2f closest = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0f;
        for (int i = 0; i < this.getNumPoints() - 1; ++i) {
            float y2;
            float x2;
            float y1;
            float x1 = this.getPointX(i);
            double distSq = PZMath.closestPointOnLineSegment(x1, y1 = this.getPointY(i), x2 = this.getPointX(i + 1), y2 = this.getPointY(i + 1), worldX, worldY, 0.0, closest);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPoint.x = closest.x;
                closestPoint.y = closest.y;
                closestPoint.distSq = (float)distSq;
                closestPoint.distanceFromStart = distanceFromStart + Vector2f.length(closest.x - x1, closest.y - y1);
                closestPoint.index = i;
            }
            distanceFromStart += Vector2f.length(x2 - x1, y2 - y1);
        }
        BaseVehicle.releaseVector2f(closest);
        return closestPoint.distSq;
    }

    public int pickPoint(UIWorldMap ui, float uiX, float uiY) {
        float closestDistSq = Float.MAX_VALUE;
        int closest = -1;
        for (int i = 0; i < this.getNumPoints(); ++i) {
            float uiY1;
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float uiX1 = ui.getAPI().worldToUIX(x1, y1);
            float distSq = IsoUtils.DistanceToSquared(uiX, uiY, uiX1, uiY1 = ui.getAPI().worldToUIY(x1, y1));
            if (!(distSq < closestDistSq) || !(distSq < 100.0f)) continue;
            closestDistSq = distSq;
            closest = i;
        }
        return closest;
    }

    public ClosestPoint getAddPointLocation(UIWorldMap ui, float uiX, float uiY, ClosestPoint closestPoint) {
        float distSq = this.getClosestPointOn(ui, uiX, uiY, closestPoint);
        if (distSq >= 100.0f) {
            return null;
        }
        closestPoint.x = (float)Math.round(closestPoint.x * 2.0f) / 2.0f;
        closestPoint.y = (float)Math.round(closestPoint.y * 2.0f) / 2.0f;
        return closestPoint;
    }

    void calculateIntersections(WorldMapStreet other) {
        Vector2f v = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0f;
        for (int i = 0; i < this.getNumPoints() - 1; ++i) {
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float x2 = this.getPointX(i + 1);
            float y2 = this.getPointY(i + 1);
            for (int j = 0; j < other.getNumPoints() - 1; ++j) {
                float y2o;
                float x2o;
                float y1o;
                float x1o = other.getPointX(j);
                if (!PZMath.intersectLineSegments(x1, y1, x2, y2, x1o, y1o = other.getPointY(j), x2o = other.getPointX(j + 1), y2o = other.getPointY(j + 1), v)) continue;
                Intersection intersection = new Intersection();
                intersection.street = other;
                intersection.worldX = v.x;
                intersection.worldY = v.y;
                intersection.segment = i;
                intersection.distanceFromStart = distanceFromStart + Vector2f.length(v.x - x1, v.y - y1);
                if (this.getMatchingIntersection(other, v.x, v.y) != null) continue;
                this.getIntersections().add(intersection);
            }
            distanceFromStart += Vector2f.length(x2 - x1, y2 - y1);
        }
        BaseVehicle.releaseVector2f(v);
    }

    public void createHighlightPolygons(TFloatArrayList polygon, TFloatArrayList triangles) {
        for (int i = 0; i < this.splitStreets.size(); ++i) {
            WorldMapStreet street = this.splitStreets.get(i);
            street.createPolygon(polygon);
            street.triangulate(polygon, triangles2);
            triangles.addAll(triangles2);
        }
    }

    public void createPolygon(TFloatArrayList points) {
        points.clear();
        if (IsoMetaGrid.clipperOffset == null) {
            IsoMetaGrid.clipperOffset = new ClipperOffset();
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
        }
        ClipperOffset clipperOffset = IsoMetaGrid.clipperOffset;
        ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
        clipperOffset.clear();
        clipperBuffer.clear();
        float dxy = 0.0f;
        for (int j = 0; j < this.getNumPoints(); ++j) {
            float x1 = this.getPointX(j);
            float y1 = this.getPointY(j);
            clipperBuffer.putFloat(x1 + 0.0f);
            clipperBuffer.putFloat(y1 + 0.0f);
        }
        clipperBuffer.flip();
        clipperOffset.addPath(this.getNumPoints(), clipperBuffer, ClipperOffset.JoinType.Miter.ordinal(), ClipperOffset.EndType.Butt.ordinal());
        float thickness = this.width;
        clipperOffset.execute(thickness / 2.0f);
        int numPolys = clipperOffset.getPolygonCount();
        if (numPolys < 1) {
            return;
        }
        clipperBuffer.clear();
        clipperOffset.getPolygon(0, clipperBuffer);
        int pointCount = clipperBuffer.getShort();
        points.ensureCapacity(pointCount * 2);
        for (int k = 0; k < pointCount; ++k) {
            points.add(clipperBuffer.getFloat());
            points.add(clipperBuffer.getFloat());
        }
    }

    boolean isClockwise(TFloatArrayList polygon) {
        float sum = 0.0f;
        int numPoints = polygon.size() / 2;
        for (int i = 0; i < numPoints; ++i) {
            float p1x = polygon.get(i * 2);
            float p1y = polygon.get(i * 2 + 1);
            int j = i % numPoints;
            float p2x = polygon.get(j * 2);
            float p2y = polygon.get(j * 2 + 1);
            sum += (p2x - p1x) * (p2y + p1y);
        }
        return (double)sum > 0.0;
    }

    public void triangulate(TFloatArrayList polygon, TFloatArrayList triangles) {
        triangles.clear();
        if (polygon.size() < 6) {
            return;
        }
        if (clipper == null) {
            clipper = new Clipper();
        }
        clipper.clear();
        if (IsoMetaGrid.clipperOffset == null) {
            IsoMetaGrid.clipperOffset = new ClipperOffset();
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(4096);
        }
        ByteBuffer bb = IsoMetaGrid.clipperBuffer;
        bb.clear();
        if (this.isClockwise(polygon)) {
            for (i = polygon.size() / 2 - 1; i >= 0; --i) {
                bb.putFloat(polygon.get(i * 2));
                bb.putFloat(polygon.get(i * 2 + 1));
            }
        } else {
            for (i = 0; i < polygon.size() / 2; ++i) {
                bb.putFloat(polygon.get(i * 2));
                bb.putFloat(polygon.get(i * 2 + 1));
            }
        }
        clipper.addPath(polygon.size() / 2, bb, false);
        int numPolygons = clipper.generatePolygons();
        if (numPolygons < 1) {
            return;
        }
        bb.clear();
        try {
            int numPoints = clipper.triangulate(0, bb);
            triangles.clear();
            for (int i = 0; i < numPoints; ++i) {
                triangles.add(bb.getFloat());
                triangles.add(bb.getFloat());
            }
        }
        catch (BufferOverflowException ex) {
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(bb.capacity() + 1024);
        }
    }

    public boolean isOnScreen(UIWorldMap ui) {
        float x1 = ui.getAPI().worldToUIX(this.getMinX(), this.getMinY());
        float y1 = ui.getAPI().worldToUIY(this.getMinX(), this.getMinY());
        float x2 = ui.getAPI().worldToUIX(this.getMaxX(), this.getMinY());
        float y2 = ui.getAPI().worldToUIY(this.getMaxX(), this.getMinY());
        float x3 = ui.getAPI().worldToUIX(this.getMaxX(), this.getMaxY());
        float y3 = ui.getAPI().worldToUIY(this.getMaxX(), this.getMaxY());
        float x4 = ui.getAPI().worldToUIX(this.getMinX(), this.getMaxY());
        float y4 = ui.getAPI().worldToUIY(this.getMinX(), this.getMaxY());
        float minX = PZMath.min(x1, x2, x3, x4);
        float minY = PZMath.min(y1, y2, y3, y4);
        float maxX = PZMath.max(x1, x2, x3, x4);
        float maxY = PZMath.max(y1, y2, y3, y4);
        return (double)minX < ui.getWidth() && maxX > 0.0f && (double)minY < ui.getHeight() && maxY > 0.0f;
    }

    public WorldMapStreet createCopy(WorldMapStreets owner) {
        StreetPoints points = s_pointsPool.alloc();
        points.clear();
        points.addAll(this.points);
        WorldMapStreet street = s_pool.alloc().init(owner, this.getTranslatedText(), points);
        street.setWidth(this.getWidth());
        return street;
    }

    static {
        countsForward = new LayoutCounts();
        countsBackward = new LayoutCounts();
    }

    static final class LayoutCounts {
        int total;
        int upsideDown;
        int up;
        int down;

        LayoutCounts() {
        }
    }

    static final class L_getSdfThreshold {
        static final double[] p0 = new double[2];
        static final double[] p1 = new double[2];

        L_getSdfThreshold() {
        }
    }
}

