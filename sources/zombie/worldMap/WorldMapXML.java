/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.util.Lambda;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.SharedStrings;
import zombie.worldMap.WorldMapCell;
import zombie.worldMap.WorldMapData;
import zombie.worldMap.WorldMapFeature;
import zombie.worldMap.WorldMapGeometry;
import zombie.worldMap.WorldMapPoint;
import zombie.worldMap.WorldMapPoints;
import zombie.worldMap.WorldMapProperties;

public final class WorldMapXML {
    private final SharedStrings sharedStrings = new SharedStrings();
    private final WorldMapPoint point = new WorldMapPoint();
    private final WorldMapProperties properties = new WorldMapProperties();
    private final ArrayList<WorldMapProperties> sharedProperties = new ArrayList();

    public boolean read(String filePath, WorldMapData data) throws PZXmlParserException {
        Element root = PZXmlUtil.parseXml(filePath);
        if (root.getNodeName().equals("world")) {
            this.parseWorld(root, data);
            return true;
        }
        return false;
    }

    private void parseWorld(Element node, WorldMapData data) {
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, data, (child, lData) -> {
            if (!child.getNodeName().equals("cell")) {
                DebugLog.General.warn("Warning: Unrecognised element '" + child.getNodeName());
                return;
            }
            WorldMapCell cell = this.parseCell((Element)child);
            lData.cells.add(cell);
        });
    }

    private WorldMapCell parseCell(Element node) {
        WorldMapCell cell = new WorldMapCell();
        cell.x = PZMath.tryParseInt(node.getAttribute("x"), 0);
        cell.y = PZMath.tryParseInt(node.getAttribute("y"), 0);
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, cell, (child, lCell) -> {
            try {
                String s = child.getNodeName();
                if ("feature".equalsIgnoreCase(s)) {
                    WorldMapFeature feature = this.parseFeature(cell, (Element)child);
                    lCell.features.add(feature);
                }
            }
            catch (Exception e) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(e);
            }
        });
        return cell;
    }

    private WorldMapFeature parseFeature(WorldMapCell cell, Element node) {
        WorldMapFeature feature = new WorldMapFeature(cell);
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, feature, (child, lFeature) -> {
            try {
                String s = child.getNodeName();
                if ("geometry".equalsIgnoreCase(s)) {
                    WorldMapGeometry geometry = this.parseGeometry(cell, (Element)child);
                    if (lFeature.geometry != null) {
                        throw new RuntimeException("only one feature geometry is supported");
                    }
                    lFeature.geometry = geometry;
                }
                if ("properties".equalsIgnoreCase(s)) {
                    this.parseFeatureProperties((Element)child, (WorldMapFeature)lFeature);
                }
            }
            catch (Exception e) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(e);
            }
        });
        return feature;
    }

    private void parseFeatureProperties(Element node, WorldMapFeature feature) {
        this.properties.clear();
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, feature, (child, lFeature) -> {
            try {
                String s = child.getNodeName();
                if ("property".equalsIgnoreCase(s)) {
                    String name = this.sharedStrings.get(child.getAttribute("name"));
                    String value = this.sharedStrings.get(child.getAttribute("value"));
                    this.properties.put(name, value);
                }
            }
            catch (Exception e) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(e);
            }
        });
        feature.properties = this.getOrCreateProperties(this.properties);
    }

    private WorldMapProperties getOrCreateProperties(WorldMapProperties properties) {
        for (int i = 0; i < this.sharedProperties.size(); ++i) {
            if (!this.sharedProperties.get(i).equals(properties)) continue;
            return this.sharedProperties.get(i);
        }
        WorldMapProperties result = new WorldMapProperties();
        result.putAll(properties);
        this.sharedProperties.add(result);
        return result;
    }

    private WorldMapGeometry parseGeometry(WorldMapCell cell, Element node) {
        WorldMapGeometry geometry = new WorldMapGeometry(cell);
        geometry.type = WorldMapGeometry.Type.valueOf(node.getAttribute("type"));
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, geometry, (child, lGeometry) -> {
            try {
                String s = child.getNodeName();
                if ("coordinates".equalsIgnoreCase(s)) {
                    WorldMapPoints points = new WorldMapPoints(geometry);
                    this.parseGeometryCoordinates((Element)child, points);
                    lGeometry.points.add(points);
                }
            }
            catch (Exception e) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(e);
            }
        });
        geometry.calculateBounds();
        return geometry;
    }

    private int countPoints(Element node) {
        int pointCount = 0;
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Element childElement;
            if (!(child instanceof Element) || !(childElement = (Element)child).getNodeName().equalsIgnoreCase("point")) continue;
            ++pointCount;
        }
        return pointCount;
    }

    private void parseGeometryCoordinates(Element node, WorldMapPoints points) {
        int numPoints = this.countPoints(node);
        if (numPoints == 0) {
            return;
        }
        WorldMapCell cell = points.owner.cell;
        ShortBuffer pointBuffer = cell.getPointBuffer(numPoints);
        int firstPoint = pointBuffer.position();
        Lambda.forEachFrom(PZXmlUtil::forEachElement, node, points, (child, lPoints) -> {
            try {
                String s = child.getNodeName();
                if ("point".equalsIgnoreCase(s)) {
                    WorldMapPoint point = this.parsePoint((Element)child, this.point);
                    pointBuffer.put((short)point.x);
                    pointBuffer.put((short)point.y);
                }
            }
            catch (Exception e) {
                DebugLog.General.error("Error while parsing xml element: " + child.getNodeName());
                DebugLog.General.error(e);
            }
        });
        int lastPoint = points.owner.cell.pointBuffer.position();
        points.setPoints((short)firstPoint, (short)(lastPoint - firstPoint));
    }

    private WorldMapPoint parsePoint(Element node, WorldMapPoint point) {
        point.x = PZMath.tryParseInt(node.getAttribute("x"), 0);
        point.y = PZMath.tryParseInt(node.getAttribute("y"), 0);
        return point;
    }
}

