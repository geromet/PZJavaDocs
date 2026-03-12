/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.anticheats;

import java.util.HashMap;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.anticheats.AbstractAntiCheat;
import zombie.network.packets.INetworkPacket;

public class AntiCheatNoClip
extends AbstractAntiCheat {
    private static final Vector3 sourcePoint = new Vector3();
    private static final Vector3 targetPoint = new Vector3();
    private static final Vector2 direction2 = new Vector2();
    private static final Vector3 direction3 = new Vector3();
    private static final HashMap<Short, Long> teleports = new HashMap();
    private static IsoGridSquare sourceSquare;
    private static IsoGridSquare targetSquare;

    public static void teleport(IsoPlayer player) {
        teleports.put(player.getOnlineID(), System.currentTimeMillis());
    }

    @Override
    public void react(UdpConnection connection, INetworkPacket packet) {
        super.react(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        IsoPlayer player = connection.players[field.getPlayerIndex()];
        if (connection.getRole().hasCapability(Capability.ToggleNoclipHimself) && player.isNoClip() || connection.getRole().hasCapability(Capability.UseFastMoveCheat) && player.isFastMoveCheat()) {
            return;
        }
        sourcePoint.set(connection.releventPos[field.getPlayerIndex()]);
        GameServer.sendTeleport(player, AntiCheatNoClip.sourcePoint.x, AntiCheatNoClip.sourcePoint.y, AntiCheatNoClip.sourcePoint.z);
        teleports.remove(player.getOnlineID());
    }

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        IAntiCheat field = (IAntiCheat)((Object)packet);
        IsoPlayer player = connection.players[field.getPlayerIndex()];
        if (player.isDead()) {
            return result;
        }
        if (player.isKnockedDown() || player.getCurrentState() != null && ("PlayerGetUpState".equals(player.getCurrentState().getName()) || "PlayerHitReactionState".equals(player.getCurrentState().getName()))) {
            return result;
        }
        if ("ClimbThroughWindowState".equals(player.getNetworkCharacterAI().getState().getEnterStateName())) {
            return result;
        }
        long timestamp = teleports.getOrDefault(player.getOnlineID(), 0L);
        if (connection.getRole().hasCapability(Capability.ToggleNoclipHimself) && player.isNoClip() || connection.getRole().hasCapability(Capability.UseFastMoveCheat) && player.isFastMoveCheat()) {
            return result;
        }
        if (player.getVehicle() != null) {
            return result;
        }
        if (System.currentTimeMillis() - timestamp <= 500L) {
            return result;
        }
        teleports.remove(player.getOnlineID());
        sourcePoint.set(connection.releventPos[field.getPlayerIndex()]);
        sourceSquare = ServerMap.getGridSquare(sourcePoint);
        field.getPosition(targetPoint);
        targetSquare = ServerMap.getGridSquare(targetPoint);
        if (sourceSquare != null && targetSquare != null && sourceSquare != targetSquare) {
            direction2.set(AntiCheatNoClip.targetSquare.x - AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.targetSquare.y - AntiCheatNoClip.sourceSquare.y);
            IsoDirections dir = IsoDirections.fromAngle(direction2);
            direction3.set(AntiCheatNoClip.targetSquare.x - AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.targetSquare.y - AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.targetSquare.z - AntiCheatNoClip.sourceSquare.z);
            IsoGridSquare outsideBasement = ServerMap.instance.getGridSquare(AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.sourceSquare.z);
            if (direction2.getLength() > 2.5f && AntiCheatNoClip.sourceSquare.x > 100 && AntiCheatNoClip.sourceSquare.y > 100) {
                result = String.format("Long blocked (%d,%d,%d) => (%d,%d,%d) len=%f", AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.sourceSquare.z, AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z, Float.valueOf(direction3.getLength()));
            } else if (1.0f < direction2.getLength() && direction2.getLength() < 2.0f) {
                boolean isReachable;
                IsoGridSquare int1 = ServerMap.instance.getGridSquare(AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z);
                IsoGridSquare int2 = ServerMap.instance.getGridSquare(AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.targetSquare.z);
                boolean bl = isReachable = AntiCheatNoClip.checkPathClamp(sourceSquare, int1) && AntiCheatNoClip.checkPathClamp(int1, targetSquare) || AntiCheatNoClip.checkPathClamp(sourceSquare, int2) && AntiCheatNoClip.checkPathClamp(int2, targetSquare);
                if (!isReachable) {
                    result = String.format("Diagonal blocked (%d,%d,%d) => (%d,%d,%d) len=%f", AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.sourceSquare.z, AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z, Float.valueOf(direction3.getLength()));
                }
            } else if (direction3.getLength() > 1.0f && AntiCheatNoClip.sourceSquare.z < 0 && AntiCheatNoClip.targetSquare.z == 0 && !outsideBasement.TreatAsSolidFloor() && !sourceSquare.HasStairs()) {
                boolean isReachable = AntiCheatNoClip.checkPathClamp(sourceSquare, outsideBasement);
                if (!isReachable) {
                    result = String.format("Basement blocked (%d,%d,%d) => (%d,%d,%d) len=%f", AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.sourceSquare.z, AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z, Float.valueOf(direction3.getLength()));
                }
            } else {
                boolean isReachable = AntiCheatNoClip.checkPathClamp(sourceSquare, targetSquare);
                if (isReachable) {
                    boolean bl = isReachable = AntiCheatNoClip.sourceSquare.z > AntiCheatNoClip.targetSquare.z || AntiCheatNoClip.checkReachablePath(dir, player);
                    if (!isReachable) {
                        result = String.format("Reachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f", AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.sourceSquare.z, AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z, Float.valueOf(direction3.getLength()));
                    }
                } else {
                    isReachable = AntiCheatNoClip.checkUnreachablePath(dir, player);
                    if (!isReachable) {
                        result = String.format("Unreachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f", AntiCheatNoClip.sourceSquare.x, AntiCheatNoClip.sourceSquare.y, AntiCheatNoClip.sourceSquare.z, AntiCheatNoClip.targetSquare.x, AntiCheatNoClip.targetSquare.y, AntiCheatNoClip.targetSquare.z, Float.valueOf(direction3.getLength()));
                    }
                }
            }
        }
        return result;
    }

    private static boolean checkPathClamp(IsoGridSquare source2, IsoGridSquare target) {
        boolean isReachableTarget = !source2.getPathMatrix(PZMath.clamp(target.x - source2.x, -1, 1), PZMath.clamp(target.y - source2.y, -1, 1), PZMath.clamp(target.z - source2.z, -1, 1));
        boolean isReachableSource = !target.getPathMatrix(PZMath.clamp(source2.x - target.x, -1, 1), PZMath.clamp(source2.y - target.y, -1, 1), PZMath.clamp(source2.z - target.z, -1, 1));
        return isReachableTarget || isReachableSource && target.z == source2.z;
    }

    private static boolean isClose(IsoGridSquare source2, IsoGridSquare target) {
        return !(-1 > target.x - source2.x && target.x - source2.x > 1 || -1 > target.y - source2.y && target.y - source2.y > 1);
    }

    private static boolean checkUnreachablePath(IsoDirections direction, IsoPlayer player) {
        IsoObject isoObject = null;
        boolean isHopable = false;
        boolean hasObstacle = false;
        if (IsoDirections.N == direction) {
            isoObject = sourceSquare.getWall(true);
            isHopable = sourceSquare.has(IsoFlagType.HoppableN);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(sourceSquare);
        } else if (IsoDirections.W == direction) {
            isoObject = sourceSquare.getWall(false);
            isHopable = sourceSquare.has(IsoFlagType.HoppableW);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(sourceSquare);
        } else if (IsoDirections.S == direction) {
            isoObject = targetSquare.getWall(true);
            isHopable = targetSquare.has(IsoFlagType.HoppableN);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(targetSquare);
        } else if (IsoDirections.E == direction) {
            isoObject = targetSquare.getWall(false);
            isHopable = targetSquare.has(IsoFlagType.HoppableW);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(targetSquare);
        }
        boolean isClose = AntiCheatNoClip.isClose(sourceSquare, targetSquare);
        if (isClose) {
            if (AntiCheatNoClip.targetSquare.z < AntiCheatNoClip.sourceSquare.z) {
                return true;
            }
            if (AntiCheatNoClip.sourceSquare.haveSheetRope || AntiCheatNoClip.targetSquare.haveSheetRope) {
                return true;
            }
            if (sourceSquare.HasStairs() || targetSquare.HasStairs()) {
                return true;
            }
            if (sourceSquare.getProperties().has(IsoFlagType.burntOut) || targetSquare.getProperties().has(IsoFlagType.burntOut)) {
                return true;
            }
        }
        if (isoObject != null) {
            if (isoObject.isHoppable() || player.canClimbOverWall(direction) || isHopable) {
                return true;
            }
            if (isoObject instanceof IsoThumpable) {
                IsoThumpable thumpable = (IsoThumpable)isoObject;
                return thumpable.canClimbThrough(player) || thumpable.canClimbOver(player) || thumpable.IsOpen() || thumpable.couldBeOpen(player);
            }
            if (isoObject instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)isoObject;
                return door.IsOpen() || door.canClimbOver(player) || door.couldBeOpen(player);
            }
            if (isoObject instanceof IsoWindow) {
                IsoWindow window = (IsoWindow)isoObject;
                return window.canClimbThrough(player);
            }
        }
        return hasObstacle;
    }

    private static boolean checkReachablePath(IsoDirections direction, IsoPlayer player) {
        IsoObject isoObject = null;
        if (IsoDirections.N == direction) {
            isoObject = sourceSquare.getDoorOrWindow(true);
        } else if (IsoDirections.W == direction) {
            isoObject = sourceSquare.getDoorOrWindow(false);
        } else if (IsoDirections.S == direction) {
            isoObject = targetSquare.getDoorOrWindow(true);
        } else if (IsoDirections.E == direction) {
            isoObject = targetSquare.getDoorOrWindow(false);
        }
        if (isoObject instanceof IsoThumpable) {
            IsoThumpable thumpable = (IsoThumpable)isoObject;
            return thumpable.canClimbThrough(player) || thumpable.canClimbOver(player) || thumpable.IsOpen() || thumpable.couldBeOpen(player);
        }
        if (isoObject instanceof IsoDoor) {
            IsoDoor door = (IsoDoor)isoObject;
            return door.IsOpen() || door.canClimbOver(player) || door.couldBeOpen(player);
        }
        if (isoObject instanceof IsoWindow) {
            IsoWindow window = (IsoWindow)isoObject;
            return window.canClimbThrough(player);
        }
        return true;
    }

    public static interface IAntiCheat {
        public byte getPlayerIndex();

        public Vector3 getPosition(Vector3 var1);
    }
}

