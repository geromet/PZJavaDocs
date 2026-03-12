/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.characters.Stats;
import zombie.core.math.PZMath;

public final class ParameterMusicZombiesTargeting
extends FMODGlobalParameter {
    private int playerIndex = -1;

    public ParameterMusicZombiesTargeting() {
        super("MusicZombiesTargeting");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null) {
            Stats stats = player.getStats();
            return PZMath.clamp(stats.musicZombiesTargetingDistantNotMoving + stats.musicZombiesTargetingNearbyNotMoving + stats.musicZombiesTargetingDistantMoving + stats.musicZombiesTargetingNearbyMoving, 0, 50);
        }
        return 0.0f;
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
            return player2;
        }
        return null;
    }
}

