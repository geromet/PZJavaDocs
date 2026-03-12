/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.streets;

import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerException;
import org.joml.Vector2f;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import zombie.core.math.PZMath;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.worldMap.streets.StreetPoints;
import zombie.worldMap.streets.WorldMapStreet;
import zombie.worldMap.streets.WorldMapStreets;

public final class WorldMapStreetsXML {
    private final Vector2f point = new Vector2f();
    private WorldMapStreets streets;

    public boolean read(String filePath, WorldMapStreets streets) throws PZXmlParserException {
        this.streets = streets;
        Element root = PZXmlUtil.parseXml(filePath);
        if ("streets".equals(root.getNodeName())) {
            this.parseWorld(root);
            return true;
        }
        return false;
    }

    public void write(String filePath, WorldMapStreets streets) throws TransformerException, IOException {
        Document doc = PZXmlUtil.createNewDocument();
        Element rootElement = doc.createElement("streets");
        rootElement.setAttribute("version", "1");
        doc.appendChild(rootElement);
        for (int i = 0; i < streets.getStreetCount(); ++i) {
            WorldMapStreet street = streets.getStreetByIndex(i);
            Element streetElement = doc.createElement("street");
            rootElement.appendChild(streetElement);
            streetElement.setAttribute("name", street.getTranslatedText());
            streetElement.setAttribute("width", String.valueOf(street.getWidth()));
            Element pointsElement = doc.createElement("points");
            streetElement.appendChild(pointsElement);
            for (int j = 0; j < street.getNumPoints(); ++j) {
                float x = street.getPointX(j);
                float y = street.getPointY(j);
                Element pointElement = doc.createElement("point");
                pointsElement.appendChild(pointElement);
                pointElement.setAttribute("x", String.format("%.1f", Float.valueOf(x)));
                pointElement.setAttribute("y", String.format("%.1f", Float.valueOf(y)));
            }
        }
        PZXmlUtil.write(doc, new File(filePath));
    }

    private void parseWorld(Element node) throws PZXmlParserException {
        int version = PZMath.tryParseInt(node.getAttribute("version"), 0);
        if (version != 1) {
            throw new PZXmlParserException("missing or invalid version");
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) continue;
            Element element = (Element)child;
            String nodeName = element.getNodeName();
            if ("street".equals(nodeName)) {
                WorldMapStreet street = this.parseStreet(element);
                this.streets.addStreet(street);
                continue;
            }
            throw new PZXmlParserException(String.format("unrecognised element \"%s\"", nodeName));
        }
    }

    private WorldMapStreet parseStreet(Element node) throws PZXmlParserException {
        String translatedText = node.getAttribute("name");
        int width = PZMath.tryParseInt(node.getAttribute("width"), 5);
        StreetPoints points = new StreetPoints();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Element element;
            String nodeName;
            if (!(child instanceof Element) || !"points".equals(nodeName = (element = (Element)child).getNodeName())) continue;
            this.parsePoints(element, points);
        }
        if (StringUtils.isNullOrWhitespace(translatedText) || points.isEmpty()) {
            throw new PZXmlParserException("invalid street name or points list");
        }
        WorldMapStreet street = new WorldMapStreet(this.streets, translatedText, points);
        street.setWidth(width);
        return street;
    }

    private void parsePoints(Element node, StreetPoints points) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) continue;
            Element element = (Element)child;
            String nodeName = child.getNodeName();
            if (!"point".equalsIgnoreCase(nodeName)) continue;
            Vector2f point = this.parsePoint(element, this.point);
            points.add(point.x);
            points.add(point.y);
        }
    }

    private Vector2f parsePoint(Element node, Vector2f point) {
        point.x = PZMath.tryParseFloat(node.getAttribute("x"), 0.0f);
        point.y = PZMath.tryParseFloat(node.getAttribute("y"), 0.0f);
        return point;
    }
}

