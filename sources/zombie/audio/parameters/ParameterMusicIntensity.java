/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicIntensity
extends FMODGlobalParameter {
    private int playerIndex = -1;
    private Intensity intensity = Intensity.Low;

    public ParameterMusicIntensity() {
        super("MusicIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        float intensity;
        IsoPlayer player = this.choosePlayer();
        this.intensity = player == null ? Intensity.Low : ((intensity = player.getMusicIntensityEvents().getIntensity()) < 34.0f ? Intensity.Low : (intensity < 67.0f ? Intensity.Medium : Intensity.High));
        return this.intensity.label;
    }

    public void setState(IsoPlayer player, Intensity state) {
        if (player == this.choosePlayer()) {
            this.intensity = state;
        }
    }

    private IsoPlayer choosePlayer() {
        IsoPlayer player;
        if (this.playerIndex != -1 && ((player = IsoPlayer.players[this.playerIndex]) == null || player.isDead())) {
            this.playerIndex = -1;
        }
        if (this.playerIndex != -1) {
            return IsoPlayer.players[this.playerIndex];
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player2 = IsoPlayer.players[i];
            if (player2 == null || player2.isDead()) continue;
            this.playerIndex = i;
            this.intensity = Intensity.Low;
            return player2;
        }
        return null;
    }

    public static enum Intensity {
        Low(0),
        Medium(1),
        High(2);

        final int label;

        private Intensity(int label) {
            this.label = label;
        }
    }
}

