/*
 * Decompiled with CFR 0.152.
 */
package zombie.popman;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="zombieTriggerXmlFile")
public final class ZombieTriggerXmlFile {
    @XmlElement(name="spawnHorde")
    public int spawnHorde;
    @XmlElement(name="setDebugLoggingEnabled")
    public boolean setDebugLoggingEnabled;
    @XmlElement(name="bDebugLoggingEnabled")
    public boolean debugLoggingEnabled;
}

