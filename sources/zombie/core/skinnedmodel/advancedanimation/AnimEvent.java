/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;

@XmlType(name="AnimEvent")
public class AnimEvent {
    @XmlElement(name="m_EventName")
    public String eventName;
    @XmlElement(name="m_Time")
    public AnimEventTime time = AnimEventTime.PERCENTAGE;
    @XmlElement(name="m_TimePc")
    public float timePc;
    @XmlElement(name="m_ParameterValue")
    public String parameterValue;
    @XmlTransient
    public AnimNode parentAnimNode;

    public AnimEvent() {
    }

    public AnimEvent(AnimEvent src) {
        this.eventName = src.eventName;
        this.time = src.time;
        this.timePc = src.timePc;
        this.parameterValue = src.parameterValue;
    }

    public String toString() {
        return String.format("%s { %s }", this.getClass().getName(), this.toDetailsString());
    }

    public String toDetailsString() {
        return String.format("Details: %s %s, time: %s", this.eventName, this.parameterValue, this.time == AnimEventTime.PERCENTAGE ? Float.toString(this.timePc) : this.time.name());
    }

    @XmlEnum
    @XmlType(name="AnimEventTime")
    public static enum AnimEventTime {
        PERCENTAGE,
        START,
        END;

    }
}

