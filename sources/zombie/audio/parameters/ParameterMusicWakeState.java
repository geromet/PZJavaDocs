/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicWakeState
extends FMODGlobalParameter {
    private int playerIndex = -1;
    private State state = State.Awake;

    public ParameterMusicWakeState() {
        super("MusicWakeState");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null && this.state == State.Awake && player.isAsleep()) {
            this.state = State.Sleeping;
        }
        return this.state.label;
    }

    public void setState(IsoPlayer player, State state) {
        if (player == this.choosePlayer()) {
            this.state = state;
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
            this.state = player2.isAsleep() ? State.Sleeping : State.Awake;
            return player2;
        }
        return null;
    }

    public static enum State {
        Awake(0),
        Sleeping(1),
        WakeNormal(2),
        WakeNightmare(3),
        WakeZombies(4);

        final int label;

        private State(int label) {
            this.label = label;
        }
    }
}

