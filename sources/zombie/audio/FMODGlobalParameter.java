/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.javafmod;
import zombie.audio.FMODParameter;

public abstract class FMODGlobalParameter
extends FMODParameter {
    public FMODGlobalParameter(String name) {
        super(name);
        if (this.getParameterDescription() != null) {
            if (!this.getParameterDescription().isGlobal()) {
                boolean bl = true;
            } else {
                FMODManager.instance.addGlobalParameter(this);
            }
        }
    }

    @Override
    public void setCurrentValue(float value) {
        javafmod.FMOD_Studio_System_SetParameterByID(this.getParameterID(), value, false);
    }

    @Override
    public void startEventInstance(long eventInstance) {
    }

    @Override
    public void stopEventInstance(long eventInstance) {
    }
}

