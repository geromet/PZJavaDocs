/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicThreat
extends FMODGlobalParameter {
    private int playerIndex = -1;
    private ThreatLevel threatLevel = ThreatLevel.Low;

    public ParameterMusicThreat() {
        super("MusicThreat");
    }

    @Override
    public float calculateCurrentValue() {
        float intensity;
        IsoPlayer player = this.choosePlayer();
        this.threatLevel = player == null ? ThreatLevel.Low : ((intensity = player.getMusicThreatStatuses().getIntensity()) < 34.0f ? ThreatLevel.Low : (intensity < 67.0f ? ThreatLevel.Medium : ThreatLevel.High));
        return this.threatLevel.label;
    }

    public void setState(IsoPlayer player, ThreatLevel state) {
        if (player == this.choosePlayer()) {
            this.threatLevel = state;
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
            this.threatLevel = ThreatLevel.Low;
            return player2;
        }
        return null;
    }

    public static enum ThreatLevel {
        Low(0),
        Medium(1),
        High(2);

        final int label;

        private ThreatLevel(int label) {
            this.label = label;
        }
    }
}

