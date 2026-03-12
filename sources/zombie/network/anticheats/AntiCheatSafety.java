/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import zombie.characters.Faction;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SafetySystemManager;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.iso.areas.NonPvpZone;
import zombie.network.ServerOptions;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

public class AntiCheatSafety
extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        boolean updateDelay = SafetySystemManager.checkUpdateDelay(field.getWielder(), field.getTarget());
        if (updateDelay) {
            return result;
        }
        return AntiCheatSafety.checkPVP(field.getWielder(), field.getTarget());
    }

    private static String checkPVP(IsoGameCharacter owner, IsoMovingObject obj) {
        IsoPlayer wielder = Type.tryCastTo(owner, IsoPlayer.class);
        IsoPlayer target = Type.tryCastTo(obj, IsoPlayer.class);
        if (wielder == null) {
            return "wielder not found";
        }
        if (target == null) {
            return "target not found";
        }
        if (target.isGodMod()) {
            return "target is in god-mode";
        }
        if (!ServerOptions.instance.pvp.getValue()) {
            return "PVP is disabled";
        }
        if (ServerOptions.instance.safetySystem.getValue() && owner.getSafety().isEnabled() && target.getSafety().isEnabled()) {
            return "safety is enabled";
        }
        if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(wielder.getX()), PZMath.fastfloor(wielder.getY())) != null) {
            long safetyTimestamp = SafetySystemManager.getSafetyTimestamp(wielder.getUsername());
            if (System.currentTimeMillis() - safetyTimestamp < SafetySystemManager.getSafetyDelay()) {
                return "";
            }
            return "wiedler is in non-pvp zone";
        }
        if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(target.getX()), PZMath.fastfloor(target.getY())) != null) {
            return "target is in non-pvp zone";
        }
        if (!wielder.isFactionPvp() && !target.isFactionPvp() && Faction.isInSameFaction(wielder, target)) {
            return "faction pvp is disabled";
        }
        return null;
    }

    public static interface IAntiCheat {
        public IsoGameCharacter getTarget();

        public IsoPlayer getWielder();
    }
}

