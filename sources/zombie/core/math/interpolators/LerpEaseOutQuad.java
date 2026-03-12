/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.math.interpolators;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;

@XmlRootElement
public class LerpEaseOutQuad
extends IInterpolator {
    public static final LerpEaseOutQuad instance = new LerpEaseOutQuad();
    @XmlAttribute(name="y0")
    public float startValue;
    @XmlAttribute(name="y1")
    public float endValue = 1.0f;

    @Override
    public float lerp(float alpha) {
        return PZMath.lerp(this.startValue, this.endValue, PZMath.lerpFunc_EaseOutQuad(alpha));
    }
}

