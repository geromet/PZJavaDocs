/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.math.interpolators;

import javax.xml.bind.annotation.XmlRootElement;
import zombie.core.math.IInterpolator;

@XmlRootElement
public class LerpZero
extends IInterpolator {
    public static final LerpZero instance = new LerpZero();

    @Override
    public float lerp(float alpha) {
        return 0.0f;
    }
}

