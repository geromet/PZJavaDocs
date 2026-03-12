/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="animStateTriggerXmlFile")
public final class AnimStateTriggerXmlFile {
    @XmlElement(name="forceAnim")
    public boolean forceAnim;
    @XmlElement(name="animSet")
    public String animSet;
    @XmlElement(name="stateName")
    public String stateName;
    @XmlElement(name="nodeName")
    public String nodeName;
    @XmlElement(name="setScalarValues")
    public boolean setScalarValues;
    @XmlElement(name="scalarValue")
    public String scalarValue;
    @XmlElement(name="scalarValue2")
    public String scalarValue2;
}

