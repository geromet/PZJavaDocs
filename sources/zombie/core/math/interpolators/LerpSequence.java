/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.math.interpolators;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;

@XmlRootElement
public class LerpSequence
extends IInterpolator {
    @XmlElement(name="point")
    public List<LerpSequenceEntry> sequenceRaw = new ArrayList<LerpSequenceEntry>();
    private int numEntries = -1;
    private LerpSequenceEntry[] sequenceEntries;
    private float minX;
    private float maxX;

    @Override
    public float lerp(float alpha) {
        int numEntries = this.numEntries;
        if (numEntries <= 0) {
            return 0.0f;
        }
        if (alpha <= this.minX) {
            return this.sequenceEntries[0].y;
        }
        if (alpha >= this.maxX) {
            return this.sequenceEntries[numEntries - 1].y;
        }
        for (int i = 1; i < numEntries; ++i) {
            LerpSequenceEntry entry = this.sequenceEntries[i];
            float pointX = entry.x;
            if (pointX < alpha) continue;
            LerpSequenceEntry prevEntry = this.sequenceEntries[i - 1];
            float prevPointX = prevEntry.x;
            if (prevPointX == pointX) continue;
            float lerpAlpha = (alpha - prevPointX) / (pointX - prevPointX);
            return PZMath.lerp(prevEntry.y, entry.y, lerpAlpha, entry.lerpType);
        }
        return this.sequenceEntries[numEntries - 1].y;
    }

    private void parse() {
        this.numEntries = this.sequenceRaw.size();
        this.sequenceEntries = new LerpSequenceEntry[this.numEntries];
        this.minX = Float.MAX_VALUE;
        this.maxX = Float.MIN_VALUE;
        for (int x = 0; x < this.numEntries; ++x) {
            LerpSequenceEntry rawEntry;
            this.sequenceEntries[x] = rawEntry = this.sequenceRaw.get(x);
        }
        for (int entryIdx = 0; entryIdx < this.numEntries; ++entryIdx) {
            int smallestIdx = entryIdx;
            for (int otherIdx = entryIdx + 1; otherIdx < this.numEntries; ++otherIdx) {
                if (!(this.sequenceEntries[otherIdx].x < this.sequenceEntries[entryIdx].x)) continue;
                smallestIdx = otherIdx;
            }
            if (smallestIdx != entryIdx) {
                LerpSequenceEntry tempEntry = this.sequenceEntries[entryIdx];
                this.sequenceEntries[entryIdx] = this.sequenceEntries[smallestIdx];
                this.sequenceEntries[smallestIdx] = tempEntry;
            }
            this.minX = PZMath.min(this.sequenceEntries[entryIdx].x, this.minX);
            this.maxX = PZMath.max(this.sequenceEntries[entryIdx].x, this.maxX);
        }
    }

    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.parse();
    }

    @XmlType(name="LerpSequenceEntry")
    public static final class LerpSequenceEntry {
        @XmlElement(name="out")
        public LerpType lerpType = LerpType.Linear;
        @XmlAttribute(name="x")
        public float x;
        @XmlAttribute(name="y")
        public float y;
    }
}

