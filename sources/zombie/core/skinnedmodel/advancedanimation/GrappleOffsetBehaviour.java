/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name="GrappleOffsetBehavior")
public enum GrappleOffsetBehaviour {
    NONE,
    GRAPPLED,
    GRAPPLED_TWEEN_OUT_TO_NONE,
    GRAPPLER,
    NONE_TWEEN_IN_GRAPPLER;

}

