/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmInside
extends FMODLocalParameter {
    private static final float VALUE_DIFFERENT_BUILDING = -1.0f;
    private static final float VALUE_OUTSIDE = 0.0f;
    private static final float VALUE_SAME_BUILDING = 1.0f;
    private final IsoPlayer character;

    public ParameterFirearmInside(IsoPlayer character) {
        super("FirearmInside");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        Object roomShooter = this.getRoom(this.character);
        if (roomShooter == null) {
            return 0.0f;
        }
        IsoPlayer listener = this.getClosestListener(this.character.getX(), this.character.getY(), this.character.getZ());
        if (listener == null) {
            return -1.0f;
        }
        if (listener == this.character) {
            return 1.0f;
        }
        Object roomListener = this.getRoom(listener);
        if (roomShooter == roomListener) {
            return 1.0f;
        }
        return -1.0f;
    }

    private Object getRoom(IsoPlayer character) {
        IsoGridSquare square = character.getCurrentSquare();
        if (square == null) {
            return null;
        }
        IsoRoom room = square.getRoom();
        if (room == null) {
            IWorldRegion worldRegion = square.getIsoWorldRegion();
            return worldRegion != null && worldRegion.isPlayerRoom() ? worldRegion : null;
        }
        return room;
    }

    private IsoPlayer getClosestListener(float soundX, float soundY, float soundZ) {
        if (IsoPlayer.numPlayers == 1) {
            return IsoPlayer.players[0];
        }
        float minDist = Float.MAX_VALUE;
        IsoPlayer closest = null;
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr == null) continue;
            if (chr == this.character) {
                return this.character;
            }
            if (chr.getCurrentSquare() == null) continue;
            float px = chr.getX();
            float py = chr.getY();
            float pz = chr.getZ();
            float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0f, soundX, soundY, soundZ * 3.0f);
            if (!((distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0f)) < minDist)) continue;
            minDist = distSq;
            closest = chr;
        }
        return closest;
    }
}

