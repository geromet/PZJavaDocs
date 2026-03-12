/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="triggerSetAnimationRecorderFile")
public final class TriggerSetAnimationRecorderFile {
    @XmlElement(name="isRecording")
    public boolean isRecording;
    @XmlElement(name="discard")
    public boolean discard;
}

