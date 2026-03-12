/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.math.interpolators;

import javax.xml.bind.annotation.XmlRootElement;
import zombie.core.math.IInterpolator;

@XmlRootElement
public class LerpOne
extends IInterpolator {
    public static final LerpOne instance = new LerpOne();

    @Override
    public float lerp(float alpha) {
        return 1.0f;
    }
}

