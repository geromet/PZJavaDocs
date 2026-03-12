/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.Moodles;

import java.util.HashMap;
import java.util.Map;
import zombie.scripting.objects.MoodleType;

public class MoodleStat {
    private static final Map<MoodleType, MoodleStat> REGISTRY = new HashMap<MoodleType, MoodleStat>();
    public static final MoodleStat ENDURANCE = MoodleStat.register(MoodleType.ENDURANCE, 0.75f, 0.5f, 0.25f, 0.1f, 0.0f);
    public static final MoodleStat ANGRY = MoodleStat.register(MoodleType.ANGRY, 0.0f, 0.1f, 0.25f, 0.5f, 0.75f);
    public static final MoodleStat TIRED = MoodleStat.register(MoodleType.TIRED, 0.0f, 0.6f, 0.7f, 0.8f, 0.9f);
    public static final MoodleStat HUNGRY = MoodleStat.register(MoodleType.HUNGRY, 0.0f, 0.15f, 0.25f, 0.45f, 0.7f);
    public static final MoodleStat PANIC = MoodleStat.register(MoodleType.PANIC, 0.0f, 6.0f, 30.0f, 65.0f, 80.0f);
    public static final MoodleStat SICK = MoodleStat.register(MoodleType.SICK, 0.0f, 0.25f, 0.5f, 0.75f, 0.9f);
    public static final MoodleStat BORED = MoodleStat.register(MoodleType.BORED, 0.0f, 25.0f, 50.0f, 75.0f, 90.0f);
    public static final MoodleStat UNHAPPY = MoodleStat.register(MoodleType.UNHAPPY, 0.0f, 20.0f, 45.0f, 60.0f, 80.0f);
    public static final MoodleStat STRESS = MoodleStat.register(MoodleType.STRESS, 0.0f, 0.25f, 0.5f, 0.75f, 0.9f);
    public static final MoodleStat THIRST = MoodleStat.register(MoodleType.THIRST, 0.0f, 0.12f, 0.25f, 0.7f, 0.84f);
    public static final MoodleStat PAIN = MoodleStat.register(MoodleType.PAIN, 0.0f, 10.0f, 20.0f, 50.0f, 75.0f);
    public static final MoodleStat WET = MoodleStat.register(MoodleType.WET, 0.0f, 15.0f, 40.0f, 70.0f, 90.0f);
    public static final MoodleStat HAS_A_COLD = MoodleStat.register(MoodleType.HAS_A_COLD, 0.0f, 20.0f, 40.0f, 60.0f, 75.0f);
    public static final MoodleStat INJURED = MoodleStat.register(MoodleType.INJURED, 0.0f, 20.0f, 40.0f, 60.0f, 75.0f);
    public static final MoodleStat DRUNK = MoodleStat.register(MoodleType.DRUNK, 0.0f, 10.0f, 30.0f, 50.0f, 70.0f);
    public static final MoodleStat UNCOMFORTABLE = MoodleStat.register(MoodleType.UNCOMFORTABLE, 0.0f, 20.0f, 40.0f, 60.0f, 80.0f);
    public static final MoodleStat NOXIOUS_SMELL = MoodleStat.register(MoodleType.NOXIOUS_SMELL, 0.0f, 0.001f, 0.001f, 0.002f, 0.002f);
    public static final MoodleStat HYPOTHERMIA = MoodleStat.register(MoodleType.HYPOTHERMIA, 0.0f, 30.0f, 70.0f, 9.0f, 100.0f);
    public static final MoodleStat WINDCHILL = MoodleStat.register(MoodleType.WINDCHILL, 0.0f, 5.0f, 10.0f, 15.0f, 20.0f);
    public static final MoodleStat HEAVY_LOAD = MoodleStat.register(MoodleType.HEAVY_LOAD, 0.0f, 1.0f, 1.25f, 1.5f, 1.75f);
    private final MoodleType moodleType;
    private float minimumThreshold;
    private float lowestThreshold;
    private float moderateThreshold;
    private float highestThreshold;
    private float maximumThreshold;

    private MoodleStat(MoodleType moodleType, float minimumThreshold, float lowThreshold, float moderateThreshold, float highestThreshold, float maximumThreshold) {
        this.moodleType = moodleType;
        this.minimumThreshold = minimumThreshold;
        this.lowestThreshold = lowThreshold;
        this.moderateThreshold = moderateThreshold;
        this.highestThreshold = highestThreshold;
        this.maximumThreshold = maximumThreshold;
    }

    public static MoodleStat register(MoodleType moodleType, float minimumThreshold, float lowThreshold, float moderateThreshold, float highestThreshold, float maximumThreshold) {
        return REGISTRY.computeIfAbsent(moodleType, key -> new MoodleStat((MoodleType)key, minimumThreshold, lowThreshold, moderateThreshold, highestThreshold, maximumThreshold));
    }

    public static MoodleStat get(MoodleType moodleType) {
        return REGISTRY.get(moodleType);
    }

    public float getMinimumThreshold() {
        return this.minimumThreshold;
    }

    public void setMinimumThreshold(float minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public float getLowestThreshold() {
        return this.lowestThreshold;
    }

    public void setLowestThreshold(float lowestThreshold) {
        this.lowestThreshold = lowestThreshold;
    }

    public float getModerateThreshold() {
        return this.moderateThreshold;
    }

    public void setModerateThreshold(float moderate) {
        this.moderateThreshold = moderate;
    }

    public float getHighestThreshold() {
        return this.highestThreshold;
    }

    public void setHighestThreshold(float highestThreshold) {
        this.highestThreshold = highestThreshold;
    }

    public float getMaximumThreshold() {
        return this.maximumThreshold;
    }

    public void setMaximumThreshold(float maximumThreshold) {
        this.maximumThreshold = maximumThreshold;
    }

    public MoodleType getMoodleType() {
        return this.moodleType;
    }
}

