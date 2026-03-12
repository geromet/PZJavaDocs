/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.Vector2;

public class NetworkPlayerVariables {
    private static final Vector2 deferredMovement = new Vector2();

    public static short getBooleanVariables(IsoPlayer isoPlayer) {
        short val = 0;
        val = (short)(val | (isoPlayer.isSneaking() ? 1 : 0));
        val = (short)(val | (isoPlayer.isOnFire() ? 2 : 0));
        val = (short)(val | (isoPlayer.isAsleep() ? 4 : 0));
        val = (short)(val | (isoPlayer.isRunning() ? 8 : 0));
        val = (short)(val | (isoPlayer.isSprinting() ? 16 : 0));
        val = (short)(val | (isoPlayer.isCharging ? 32 : 0));
        val = (short)(val | (isoPlayer.isChargingLt ? 64 : 0));
        val = (short)(val | (isoPlayer.isDoShove() ? 128 : 0));
        val = (short)(val | (isoPlayer.isDoGrapple() ? 4096 : 0));
        val = (short)(val | (isoPlayer.isDoContinueGrapple() ? 8192 : 0));
        isoPlayer.getDeferredMovement(deferredMovement);
        val = (short)(val | (deferredMovement.getLength() > 0.0f ? 256 : 0));
        val = (short)(val | (isoPlayer.isOnFloor() ? 512 : 0));
        val = (short)(val | (isoPlayer.isGodMod() ? 1024 : 0));
        val = (short)(val | (Core.debug ? 2048 : 0));
        val = (short)(val | (isoPlayer.getAutoDrink() ? 16384 : 0));
        return val;
    }

    public static void setBooleanVariables(IsoPlayer isoPlayer, short val) {
        isoPlayer.setSneaking((val & 1) != 0);
        if ((val & 2) != 0) {
            isoPlayer.SetOnFire();
        } else {
            isoPlayer.StopBurning();
        }
        isoPlayer.setAsleep((val & 4) != 0);
        isoPlayer.setRunning((val & 8) != 0);
        isoPlayer.setSprinting((val & 0x10) != 0);
        isoPlayer.isCharging = (val & 0x20) != 0;
        boolean bl = isoPlayer.isChargingLt = (val & 0x40) != 0;
        if (!isoPlayer.isDoShove() && (val & 0x80) != 0) {
            isoPlayer.setDoShove((val & 0x80) != 0);
        }
        isoPlayer.setDoGrapple((val & 0x1000) != 0);
        isoPlayer.setDoContinueGrapple((val & 0x2000) != 0);
        isoPlayer.networkAi.moving = (val & 0x100) != 0;
        isoPlayer.setOnFloor((val & 0x200) != 0);
        isoPlayer.setAutoDrink((val & 0x4000) != 0);
    }

    public static class Flags {
        public static final short isSneaking = 1;
        public static final short isOnFire = 2;
        public static final short isAsleep = 4;
        public static final short isRunning = 8;
        public static final short isSprinting = 16;
        public static final short isCharging = 32;
        public static final short isChargingLT = 64;
        public static final short isDoShove = 128;
        public static final short hasDeferredMovement = 256;
        public static final short isOnFloor = 512;
        public static final short isCheatMode = 1024;
        public static final short isDebugMode = 2048;
        public static final short isDoGrapple = 4096;
        public static final short isDoContinueGrapple = 8192;
        public static final short isAutoDrinkEnabled = 16384;
    }
}

