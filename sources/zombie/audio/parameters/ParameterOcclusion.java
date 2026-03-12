/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import fmod.fmod.FMODSoundEmitter;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

public final class ParameterOcclusion
extends FMODLocalParameter {
    private final FMODSoundEmitter emitter;
    private float currentValue = Float.NaN;

    public ParameterOcclusion(FMODSoundEmitter emitter) {
        super("Occlusion");
        this.emitter = emitter;
    }

    @Override
    public float calculateCurrentValue() {
        float occlusion = 1.0f;
        for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
            float value = this.calculateValueForPlayer(playerIndex);
            occlusion = PZMath.min(occlusion, value);
        }
        this.currentValue = occlusion;
        return (float)((int)(this.currentValue * 1000.0f)) / 1000.0f;
    }

    @Override
    public void resetToDefault() {
        this.currentValue = Float.NaN;
    }

    private float calculateValueForPlayer(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null) {
            return 1.0f;
        }
        IsoGridSquare sqPlayer = player.getCurrentSquare();
        IsoGridSquare sqSound = IsoWorld.instance.getCell().getGridSquare(this.emitter.x, this.emitter.y, this.emitter.z);
        if (sqSound == null) {
            boolean bl = true;
        }
        float occlusion = 0.0f;
        if (sqPlayer != null && sqSound != null && !sqSound.isCouldSee(playerIndex)) {
            occlusion = 1.0f;
        }
        return occlusion;
    }
}

