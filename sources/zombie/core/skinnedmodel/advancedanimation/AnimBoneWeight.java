/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="AnimBoneWeight")
public final class AnimBoneWeight {
    @XmlElement(name="boneName")
    public String boneName;
    @XmlElement(name="weight")
    public float weight = 1.0f;
    @XmlElement(name="includeDescendants")
    public boolean includeDescendants = true;

    public AnimBoneWeight() {
    }

    public AnimBoneWeight(String boneName, float weight) {
        this.boneName = boneName;
        this.weight = weight;
        this.includeDescendants = true;
    }
}

