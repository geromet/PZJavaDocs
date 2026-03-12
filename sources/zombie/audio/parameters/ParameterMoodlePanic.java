/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.scripting.objects.MoodleType;

public final class ParameterMoodlePanic
extends FMODGlobalParameter {
    public ParameterMoodlePanic() {
        super("MoodlePanic");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 0.0f;
        }
        return (float)character.getMoodles().getMoodleLevel(MoodleType.PANIC) / 4.0f;
    }
}

