/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterEquippedBaggageContainer
extends FMODLocalParameter {
    private final IsoGameCharacter character;
    private ContainerType containerType = ContainerType.None;

    public ParameterEquippedBaggageContainer(IsoGameCharacter character) {
        super("EquippedBaggageContainer");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.containerType.label;
    }

    public void setContainerType(ContainerType containerType) {
        this.containerType = containerType;
    }

    public void setContainerType(String containerType) {
        if (containerType == null) {
            return;
        }
        try {
            this.containerType = ContainerType.valueOf(containerType);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            // empty catch block
        }
    }

    public static enum ContainerType {
        None(0),
        HikingBag(1),
        DuffleBag(2),
        PlasticBag(3),
        SchoolBag(4),
        ToteBag(5),
        GarbageBag(6);

        public final int label;

        private ContainerType(int label) {
            this.label = label;
        }
    }
}

