/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PARAMETER_ID;

public abstract class FMODParameter {
    private final String name;
    private final FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription;
    private float currentValue = Float.NaN;

    public FMODParameter(String name) {
        this.name = name;
        this.parameterDescription = FMODManager.instance.getParameterDescription(name);
    }

    public String getName() {
        return this.name;
    }

    public FMOD_STUDIO_PARAMETER_DESCRIPTION getParameterDescription() {
        return this.parameterDescription;
    }

    public FMOD_STUDIO_PARAMETER_ID getParameterID() {
        return this.parameterDescription == null ? null : this.parameterDescription.id;
    }

    public float getCurrentValue() {
        return this.currentValue;
    }

    public void update() {
        float value = this.calculateCurrentValue();
        if (value == this.currentValue) {
            return;
        }
        this.currentValue = value;
        this.setCurrentValue(this.currentValue);
    }

    public void resetToDefault() {
    }

    public abstract float calculateCurrentValue();

    public abstract void setCurrentValue(float var1);

    public abstract void startEventInstance(long var1);

    public abstract void stopEventInstance(long var1);
}

