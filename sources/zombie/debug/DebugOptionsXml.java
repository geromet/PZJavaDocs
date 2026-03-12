/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name="debugOptionsXml")
final class DebugOptionsXml {
    @XmlElement(name="setDebugMode")
    public boolean setDebugMode;
    @XmlElement(name="debugMode")
    public boolean debugMode = true;
    @XmlElement(name="options")
    public final ArrayList<OptionNode> options = new ArrayList();

    DebugOptionsXml() {
    }

    @XmlType(name="OptionNode")
    public static final class OptionNode {
        @XmlElement(name="name")
        public String name;
        @XmlElement(name="value")
        public boolean value;

        public OptionNode() {
        }

        public OptionNode(String name, boolean value) {
            this.name = name;
            this.value = value;
        }
    }
}

