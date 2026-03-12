/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public class ParameterBroadcastVoiceType
extends FMODLocalParameter {
    private BroadcastVoiceType voiceType = BroadcastVoiceType.Male;

    public ParameterBroadcastVoiceType() {
        super("BroadcastVoiceType");
    }

    @Override
    public float calculateCurrentValue() {
        return this.voiceType.label;
    }

    public void setValue(BroadcastVoiceType voiceType) {
        this.voiceType = voiceType;
    }

    public static enum BroadcastVoiceType {
        Male(0),
        Female(1);

        final int label;

        private BroadcastVoiceType(int label) {
            this.label = label;
        }

        public int getValue() {
            return this.label;
        }
    }
}

