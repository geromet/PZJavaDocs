/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import java.util.ArrayList;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.iso.IsoDirections;

public final class CharacterTextures {
    final ArrayList<CTAnimSet> animSets = new ArrayList();

    CTAnimSet getAnimSet(String animSet) {
        for (int i = 0; i < this.animSets.size(); ++i) {
            CTAnimSet ctAnimSet = this.animSets.get(i);
            if (!ctAnimSet.name.equals(animSet)) continue;
            return ctAnimSet;
        }
        return null;
    }

    DeadBodyAtlas.BodyTexture getTexture(String animSet, String state, IsoDirections dir, int frame) {
        CTAnimSet ctAnimSet = this.getAnimSet(animSet);
        if (ctAnimSet == null) {
            return null;
        }
        CTState ctState = ctAnimSet.getState(state);
        if (ctState == null) {
            return null;
        }
        CTEntry ctEntry = ctState.getEntry(dir, frame);
        if (ctEntry == null) {
            return null;
        }
        return ctEntry.texture;
    }

    void addTexture(String animSet, String state, IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
        CTAnimSet ctAnimSet = this.getAnimSet(animSet);
        if (ctAnimSet == null) {
            ctAnimSet = new CTAnimSet();
            ctAnimSet.name = animSet;
            this.animSets.add(ctAnimSet);
        }
        ctAnimSet.addEntry(state, dir, frame, texture);
    }

    void clear() {
        this.animSets.clear();
    }

    private static final class CTAnimSet {
        String name;
        final ArrayList<CTState> states = new ArrayList();

        private CTAnimSet() {
        }

        CTState getState(String state) {
            for (int i = 0; i < this.states.size(); ++i) {
                CTState ctState = this.states.get(i);
                if (!ctState.name.equals(state)) continue;
                return ctState;
            }
            return null;
        }

        void addEntry(String state, IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
            CTState ctState = this.getState(state);
            if (ctState == null) {
                ctState = new CTState();
                ctState.name = state;
                this.states.add(ctState);
            }
            ctState.addEntry(dir, frame, texture);
        }
    }

    private static final class CTState {
        String name;
        final CTEntryList[] entries = new CTEntryList[IsoDirections.values().length];

        CTState() {
            for (int i = 0; i < this.entries.length; ++i) {
                this.entries[i] = new CTEntryList();
            }
        }

        CTEntry getEntry(IsoDirections dir, int frame) {
            CTEntryList entries = this.entries[dir.ordinal()];
            for (int i = 0; i < entries.size(); ++i) {
                CTEntry entry = (CTEntry)entries.get(i);
                if (entry.frame != frame) continue;
                return entry;
            }
            return null;
        }

        void addEntry(IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
            CTEntryList entries = this.entries[dir.ordinal()];
            CTEntry entry = new CTEntry();
            entry.frame = frame;
            entry.texture = texture;
            entries.add(entry);
        }
    }

    private static final class CTEntry {
        int frame;
        DeadBodyAtlas.BodyTexture texture;

        private CTEntry() {
        }
    }

    private static final class CTEntryList
    extends ArrayList<CTEntry> {
        private CTEntryList() {
        }
    }
}

