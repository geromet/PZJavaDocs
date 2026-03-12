/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugType;
import zombie.util.PZForEeachElementXmlParseException;
import zombie.util.PZXmlParserException;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class PZXmlUtil {
    private static final ThreadLocal<DocumentBuilder> documentBuilders = ThreadLocal.withInitial(() -> {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            return dbFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex) {
            ExceptionLogger.logException(ex);
            throw new RuntimeException(ex);
        }
    });

    public static Element parseXml(String source2) throws PZXmlParserException {
        Element root;
        String fileName = ZomboidFileSystem.instance.resolveFileOrGUID(source2);
        try {
            root = PZXmlUtil.parseXmlInternal(fileName);
        }
        catch (IOException | SAXException e) {
            throw new PZXmlParserException("Exception thrown while parsing XML file \"" + fileName + "\"", e);
        }
        root = PZXmlUtil.includeAnotherFile(root, fileName);
        String extendsSource = root.getAttribute("x_extends");
        if (extendsSource == null || extendsSource.trim().isEmpty()) {
            return root;
        }
        if (!ZomboidFileSystem.instance.isValidFilePathGuid(extendsSource)) {
            extendsSource = ZomboidFileSystem.instance.resolveRelativePath(fileName, extendsSource);
        }
        Element extendedRoot = PZXmlUtil.parseXml(extendsSource);
        return PZXmlUtil.resolve(root, extendedRoot);
    }

    private static Element includeAnotherFile(Element root, String fileName) throws PZXmlParserException {
        Element includeRoot;
        String includeSource = root.getAttribute("x_include");
        if (includeSource == null || includeSource.trim().isEmpty()) {
            return root;
        }
        if (!ZomboidFileSystem.instance.isValidFilePathGuid(includeSource)) {
            includeSource = ZomboidFileSystem.instance.resolveRelativePath(fileName, includeSource);
        }
        if (!(includeRoot = PZXmlUtil.parseXml(includeSource)).getTagName().equals(root.getTagName())) {
            return root;
        }
        Document doc = PZXmlUtil.createNewDocument();
        Node rootCopy = doc.importNode(root, true);
        Node insertBefore = rootCopy.getFirstChild();
        for (Node node = includeRoot.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element)) continue;
            Element nodeCopy = (Element)doc.importNode(node, true);
            rootCopy.insertBefore(nodeCopy, insertBefore);
        }
        rootCopy.normalize();
        return (Element)rootCopy;
    }

    private static Element resolve(Element root, Element parent) {
        Document doc = PZXmlUtil.createNewDocument();
        Element result = PZXmlUtil.resolve(root, parent, doc);
        doc.appendChild(result);
        if (DebugType.Xml.isEnabled()) {
            DebugType.Xml.debugln("PZXmlUtil.resolve> \r\n<Parent>\r\n" + PZXmlUtil.elementToPrettyStringSafe(parent) + "\r\n</Parent>\r\n<Child>\r\n" + PZXmlUtil.elementToPrettyStringSafe(root) + "\r\n</Child>\r\n<Resolved>\r\n" + PZXmlUtil.elementToPrettyStringSafe(result) + "\r\n</Resolved>");
        }
        return result;
    }

    private static Element resolve(Element root, Element parent, Document resultDoc) {
        if (PZXmlUtil.isTextOnly(root)) {
            return (Element)resultDoc.importNode(root, true);
        }
        Element result = resultDoc.createElement(root.getTagName());
        ArrayList<Attr> resolvedAttributes = new ArrayList<Attr>();
        NamedNodeMap parentAttributes = parent.getAttributes();
        for (int iattr = 0; iattr < parentAttributes.getLength(); ++iattr) {
            Node attrNode = parentAttributes.item(iattr);
            if (!(attrNode instanceof Attr)) {
                DebugType.Xml.trace("PZXmlUtil.resolve> Skipping parent.attrib: %s", attrNode);
                continue;
            }
            Attr attr = (Attr)resultDoc.importNode(attrNode, true);
            resolvedAttributes.add(attr);
        }
        NamedNodeMap childAttributes = root.getAttributes();
        for (int iattr = 0; iattr < childAttributes.getLength(); ++iattr) {
            Node attrNode = childAttributes.item(iattr);
            if (!(attrNode instanceof Attr)) {
                DebugType.Xml.trace("PZXmlUtil.resolve> Skipping attrib: %s", attrNode);
                continue;
            }
            Attr attr = (Attr)resultDoc.importNode(attrNode, true);
            String attrName = attr.getName();
            boolean isNewAttrib = true;
            for (int i = 0; i < resolvedAttributes.size(); ++i) {
                Attr parentAttr = (Attr)resolvedAttributes.get(i);
                String parentName = parentAttr.getName();
                if (!parentName.equals(attrName)) continue;
                resolvedAttributes.set(i, attr);
                isNewAttrib = false;
                break;
            }
            if (!isNewAttrib) continue;
            resolvedAttributes.add(attr);
        }
        for (Attr attr : resolvedAttributes) {
            result.setAttributeNode(attr);
        }
        TagTable parentTagTable = TagTable.createTagTable(parent, resultDoc);
        TagTable childTagTable = TagTable.createTagTable(root, resultDoc);
        parentTagTable.resolveWith(childTagTable, resultDoc);
        for (NamedTagEntry resolvedElement : parentTagTable.resolvedElements) {
            result.appendChild(resolvedElement.element);
        }
        return result;
    }

    private static boolean isTextOnly(Element root) {
        boolean result = false;
        for (Node childNode = root.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            boolean isText = false;
            if (childNode instanceof Text) {
                String textContent = childNode.getTextContent();
                boolean isWhitespace = StringUtils.isNullOrWhitespace(textContent);
                boolean bl = isText = !isWhitespace;
            }
            if (!isText) {
                result = false;
                break;
            }
            result = true;
        }
        return result;
    }

    public static String elementToPrettyStringSafe(Element node) {
        try {
            return PZXmlUtil.elementToPrettyString(node);
        }
        catch (TransformerException e) {
            return null;
        }
    }

    public static String elementToPrettyString(Element node) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("indent", "yes");
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source2 = new DOMSource(node);
        transformer.transform(source2, result);
        return result.getWriter().toString();
    }

    public static Document createNewDocument() {
        DocumentBuilder dBuilder = documentBuilders.get();
        return dBuilder.newDocument();
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Element parseXmlInternal(String fileName) throws SAXException, IOException {
        try (FileInputStream fis = new FileInputStream(fileName);){
            Element element;
            try (BufferedInputStream adrFile = new BufferedInputStream(fis);){
                DocumentBuilder dBuilder = documentBuilders.get();
                Document doc = dBuilder.parse(adrFile);
                adrFile.close();
                Element root = doc.getDocumentElement();
                root.normalize();
                element = root;
            }
            return element;
        }
        catch (SAXException e) {
            System.err.println("Exception parsing filename: " + fileName);
            throw e;
        }
    }

    public static void forEachElement(Element root, Consumer<Element> consumer) {
        for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) continue;
            Element childElement = (Element)child;
            try {
                consumer.accept(childElement);
                continue;
            }
            catch (Exception e) {
                throw new PZForEeachElementXmlParseException("Exception thrown parsing xml.", childElement, e);
            }
        }
    }

    public static <T> T parse(Class<T> type, String source2) throws PZXmlParserException {
        Element root = PZXmlUtil.parseXml(source2);
        return PZXmlUtil.unmarshall(type, root);
    }

    public static <T> T unmarshall(Class<T> type, Element root) throws PZXmlParserException {
        try {
            Unmarshaller unmarshaller = UnmarshallerAllocator.get(type);
            return (T)unmarshaller.unmarshal(root);
        }
        catch (JAXBException e) {
            throw new PZXmlParserException("Exception thrown loading source: \"" + root.getLocalName() + "\". Loading for type \"" + String.valueOf(type) + "\"", e);
        }
    }

    public static <T> void write(T data, File outFile) throws TransformerException, IOException, JAXBException {
        Document doc = PZXmlUtil.createNewDocument();
        Marshaller marshaller = MarshallerAllocator.get(data);
        marshaller.marshal(data, doc);
        PZXmlUtil.write(doc, outFile);
    }

    public static void write(Document doc, File outFile) throws TransformerException, IOException {
        Element rootElement = doc.getDocumentElement();
        String content = PZXmlUtil.elementToPrettyString(rootElement);
        FileOutputStream out = new FileOutputStream(outFile, false);
        PrintWriter writer = new PrintWriter(out);
        writer.write(content);
        writer.flush();
        out.flush();
        out.close();
    }

    public static <T> boolean tryWrite(T data, File outFile) {
        try {
            PZXmlUtil.write(data, outFile);
            return true;
        }
        catch (IOException | JAXBException | TransformerException e) {
            ExceptionLogger.logException(e, "Exception thrown writing data: \"" + String.valueOf(data) + "\". Out file: \"" + String.valueOf(outFile) + "\"");
            return false;
        }
    }

    public static boolean tryWrite(Document doc, File outFile) {
        try {
            PZXmlUtil.write(doc, outFile);
            return true;
        }
        catch (IOException | TransformerException e) {
            ExceptionLogger.logException(e, "Exception thrown writing document: \"" + String.valueOf(doc) + "\". Out file: \"" + String.valueOf(outFile) + "\"");
            return false;
        }
    }

    private static class TagTable {
        public final HashMap<String, NamedTags> tags = new HashMap();
        public final ArrayList<NamedTagEntry> resolvedElements = new ArrayList();

        private TagTable() {
        }

        public static TagTable createTagTable(Element root, Document resultDoc) {
            TagTable tagTable = new TagTable();
            for (Node childNode = root.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (!(childNode instanceof Element)) {
                    DebugType.Xml.trace("PZXmlUtil.resolve> Skipping node: %s", childNode);
                    continue;
                }
                Element nodeElement = (Element)resultDoc.importNode(childNode, true);
                tagTable.addEntry(nodeElement);
            }
            return tagTable;
        }

        public NamedTagEntry getEntry(NamedTagEntry childElement) {
            NamedTags namedTags = this.tags.get(childElement.tag);
            if (namedTags == null) {
                return null;
            }
            if (StringUtils.isNullOrWhitespace(childElement.name)) {
                return PZArrayUtil.find(namedTags.namedTags.values(), value -> value.index == childElement.index);
            }
            return namedTags.namedTags.get(childElement.name);
        }

        public void addEntry(Element nodeElement) {
            String nodeTag = nodeElement.getTagName();
            int tagIndex = this.getTagIndex(nodeTag);
            String nodeName = this.getNodeName(nodeElement);
            NamedTagEntry tagEntry = new NamedTagEntry();
            tagEntry.tag = nodeTag;
            tagEntry.name = nodeName;
            tagEntry.element = nodeElement;
            tagEntry.index = tagIndex;
            this.addEntry(tagEntry);
        }

        public void addEntry(NamedTagEntry entry) {
            this.resolvedElements.add(entry);
            NamedTags tagsEntry = this.getOrCreateTableEntry(entry.tag);
            tagsEntry.namedTags.put(entry.getUniqueKey(), entry);
        }

        private NamedTags getOrCreateTableEntry(String tag) {
            NamedTags tagsEntry = this.tags.get(tag);
            if (tagsEntry == null) {
                tagsEntry = new NamedTags();
                this.tags.put(tag, tagsEntry);
            }
            return tagsEntry;
        }

        private String getNodeName(Element nodeElement) {
            return nodeElement.getAttribute("x_name");
        }

        private String getNodeNameFromTagIdx(String nodeTag) {
            int tagIndex = this.getTagIndex(nodeTag);
            return "nodeTag_" + tagIndex;
        }

        private int getTagIndex(String nodeTag) {
            NamedTags namedTagsEntry = this.tags.get(nodeTag);
            int tagIndex = 0;
            if (namedTagsEntry != null) {
                tagIndex = namedTagsEntry.namedTags.size();
            }
            return tagIndex;
        }

        public void resolveWith(TagTable childTable, Document resultDoc) {
            for (NamedTagEntry childEntry : childTable.resolvedElements) {
                NamedTagEntry parentEntry = this.getEntry(childEntry);
                if (parentEntry == null) {
                    this.addEntry(childEntry);
                    continue;
                }
                parentEntry.element = PZXmlUtil.resolve(childEntry.element, parentEntry.element, resultDoc);
            }
        }
    }

    private static class NamedTagEntry {
        public String tag;
        public String name;
        public Element element;
        public int index;

        private NamedTagEntry() {
        }

        public String getUniqueKey() {
            if (StringUtils.isNullOrWhitespace(this.name)) {
                return "node_" + this.index;
            }
            return this.name;
        }
    }

    private static final class UnmarshallerAllocator {
        private static final ThreadLocal<UnmarshallerAllocator> instance = ThreadLocal.withInitial(UnmarshallerAllocator::new);
        private final Map<Class<?>, Unmarshaller> map = new HashMap();

        private UnmarshallerAllocator() {
        }

        public static <T> Unmarshaller get(Class<T> type) throws JAXBException {
            return instance.get().getOrCreate(type);
        }

        private <T> Unmarshaller getOrCreate(Class<T> type) throws JAXBException {
            Unmarshaller unmarshaller = this.map.get(type);
            if (unmarshaller == null) {
                unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
                this.map.put(type, unmarshaller);
            }
            return unmarshaller;
        }
    }

    private static final class MarshallerAllocator {
        private static final ThreadLocal<MarshallerAllocator> instance = ThreadLocal.withInitial(MarshallerAllocator::new);
        private final Map<Class<?>, Marshaller> map = new HashMap();

        private MarshallerAllocator() {
        }

        public static <T> Marshaller get(T type) throws JAXBException {
            return MarshallerAllocator.get(type.getClass());
        }

        public static <T> Marshaller get(Class<T> type) throws JAXBException {
            return instance.get().getOrCreate(type);
        }

        private <T> Marshaller getOrCreate(Class<T> type) throws JAXBException {
            Marshaller unmarshaller = this.map.get(type);
            if (unmarshaller == null) {
                JAXBContext context = JAXBContext.newInstance(type);
                unmarshaller = context.createMarshaller();
                unmarshaller.setListener(new Marshaller.Listener(this){
                    {
                        Objects.requireNonNull(this$0);
                    }
                });
                this.map.put(type, unmarshaller);
            }
            return unmarshaller;
        }
    }

    private static class NamedTags {
        public final HashMap<String, NamedTagEntry> namedTags = new HashMap();

        private NamedTags() {
        }
    }
}

