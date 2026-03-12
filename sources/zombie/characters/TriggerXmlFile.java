/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="triggerXmlFile")
public final class TriggerXmlFile {
    @XmlElement(name="outfitName")
    public String outfitName;
    @XmlElement(name="clothingItemGUID")
    public String clothingItemGuid;
    @XmlElement(name="isMale")
    public boolean isMale;
}

