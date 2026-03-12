/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;

public final class SoundInstanceLimiter {
    private static final ObjectPool<SoundInstance> pool = new ObjectPool<SoundInstance>(SoundInstance::new);
    private static final DistanceComparator distanceComparator = new DistanceComparator();
    private final Map<String, SoundInstanceGroup> groups = new HashMap<String, SoundInstanceGroup>();

    public void startFrame() {
        for (Map.Entry<String, SoundInstanceGroup> it : this.groups.entrySet()) {
            it.getValue().startFrame();
        }
    }

    public void getActiveGroups(List<String> activeGroups) {
        activeGroups.clear();
        for (Map.Entry<String, SoundInstanceGroup> it : this.groups.entrySet()) {
            if (it.getValue().instances.isEmpty()) continue;
            it.getValue().sortByDistance(distanceComparator);
            activeGroups.add(it.getKey());
        }
    }

    public void register(Object object, String soundName, float x, float y, float z) {
        SoundInstanceGroup group = this.groups.get(soundName);
        if (group == null) {
            group = new SoundInstanceGroup(soundName);
            this.groups.put(soundName, group);
        }
        group.register(object, x, y, z);
    }

    public boolean isClosest(Object object, String soundName) {
        SoundInstanceGroup group = this.groups.get(soundName);
        if (group != null) {
            return group.isClosest(object);
        }
        return false;
    }

    private static final class SoundInstanceGroup {
        String soundName;
        final List<SoundInstance> instances = new ArrayList<SoundInstance>();

        SoundInstanceGroup(String soundName) {
            this.soundName = soundName;
        }

        void startFrame() {
            pool.releaseAll(this.instances);
            this.instances.clear();
        }

        SoundInstance findObject(Object object) {
            for (SoundInstance instance : this.instances) {
                if (instance.object != object) continue;
                return instance;
            }
            return null;
        }

        void register(Object object, float x, float y, float z) {
            SoundInstance instance = this.findObject(object);
            if (instance == null) {
                this.instances.add(pool.alloc().set(object, x, y, z));
                return;
            }
            instance.x = x;
            instance.y = y;
            instance.z = z;
        }

        void sortByDistance(DistanceComparator distanceComparator) {
            this.instances.sort(distanceComparator);
        }

        boolean isClosest(Object object) {
            return !this.instances.isEmpty() && object == ((SoundInstance)this.instances.getFirst()).object;
        }
    }

    private static final class DistanceComparator
    implements Comparator<SoundInstance> {
        private DistanceComparator() {
        }

        @Override
        public int compare(SoundInstance o1, SoundInstance o2) {
            float aScore = this.getClosestListener(o1.x, o1.y, o1.z);
            float bScore = this.getClosestListener(o2.x, o2.y, o2.z);
            return Float.compare(aScore, bScore);
        }

        float getClosestListener(float soundX, float soundY, float soundZ) {
            IsoGameCharacter listener = null;
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer chr = IsoPlayer.players[i];
                if (chr == null || chr.hasTrait(CharacterTrait.DEAF) && listener != null && !listener.hasTrait(CharacterTrait.DEAF)) continue;
                float px = chr.getX();
                float py = chr.getY();
                float pz = chr.getZ();
                float dist = IsoUtils.DistanceTo(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
                if (!((dist *= chr.getHearDistanceModifier()) < minDist)) continue;
                minDist = dist;
                listener = chr;
            }
            return minDist;
        }
    }

    private static final class SoundInstance {
        Object object;
        float x;
        float y;
        float z;

        private SoundInstance() {
        }

        SoundInstance set(Object object, float x, float y, float z) {
            this.object = object;
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
    }
}

