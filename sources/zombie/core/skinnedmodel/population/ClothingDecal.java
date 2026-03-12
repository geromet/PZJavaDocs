/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.util.StringUtils;

@XmlRootElement(name="clothingDecal")
public class ClothingDecal {
    @XmlTransient
    public String name;
    @XmlElement(name="texture")
    public String texture;
    @XmlElement(name="x")
    public int x;
    @XmlElement(name="y")
    public int y;
    @XmlElement(name="width")
    public int width;
    @XmlElement(name="height")
    public int height;

    public boolean isValid() {
        return !StringUtils.isNullOrWhitespace(this.texture) && this.x >= 0 && this.y >= 0 && this.width > 0 && this.height > 0;
    }
}

