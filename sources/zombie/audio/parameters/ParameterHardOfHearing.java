/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.scripting.objects.CharacterTrait;

public final class ParameterHardOfHearing
extends FMODGlobalParameter {
    private int playerIndex = -1;

    public ParameterHardOfHearing() {
        super("HardOfHearing");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null) {
            return player.hasTrait(CharacterTrait.HARD_OF_HEARING) ? 1.0f : 0.0f;
        }
        return 0.0f;
    }

    private IsoPlayer choosePlayer() {
        IsoPlayer player;
        if (this.playerIndex != -1 && (player = IsoPlayer.players[this.playerIndex]) == null) {
            this.playerIndex = -1;
        }
        if (this.playerIndex != -1) {
            return IsoPlayer.players[this.playerIndex];
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer player2 = IsoPlayer.players[i];
            if (player2 == null) continue;
            this.playerIndex = i;
            return player2;
        }
        return null;
    }
}

