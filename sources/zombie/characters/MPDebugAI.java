/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.ai.states.PathFindState;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugOptions;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.pathfind.PathFindBehavior2;

public class MPDebugAI {
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();

    public static IsoPlayer getNearestPlayer(IsoPlayer target) {
        IsoPlayer player = null;
        for (IsoPlayer p : GameClient.IDToPlayerMap.values()) {
            if (p == target || player != null && !(player.getDistanceSq(target) > p.getDistanceSq(target))) continue;
            player = p;
        }
        return player;
    }

    public static boolean updateMovementFromInput(IsoPlayer player, IsoPlayer.MoveVars moveVars) {
        if (GameClient.client && player.isLocalPlayer() && (DebugOptions.instance.multiplayer.debug.attackPlayer.getValue() || DebugOptions.instance.multiplayer.debug.followPlayer.getValue())) {
            IsoPlayer p = MPDebugAI.getNearestPlayer(player);
            if (p != null) {
                Vector2 move = new Vector2(p.getX() - player.getX(), player.getY() - p.getY());
                move.rotate(-0.7853982f);
                move.normalize();
                moveVars.moveX = move.x;
                moveVars.moveY = move.y;
                moveVars.newFacing = IsoDirections.fromAngle(move);
                if (p.getDistanceSq(player) > 100.0f) {
                    player.removeFromSquare();
                    player.setX(p.realx);
                    player.setY(p.realy);
                    player.setZ(p.realz);
                    player.setLastX(p.realx);
                    player.setLastY(p.realy);
                    player.setLastZ(p.realz);
                    player.ensureOnTile();
                } else if (p.getDistanceSq(player) > 50.0f) {
                    player.setRunning(true);
                    player.setSprinting(true);
                } else if (p.getDistanceSq(player) > 5.0f) {
                    player.setRunning(true);
                } else if (p.getDistanceSq(player) < 1.25f) {
                    moveVars.moveX = 0.0f;
                    moveVars.moveY = 0.0f;
                }
            }
            PathFindBehavior2 pfb2 = player.getPathFindBehavior2();
            if (moveVars.moveX == 0.0f && moveVars.moveY == 0.0f && player.getPath2() != null && pfb2.isStrafing() && !pfb2.stopping) {
                Vector2 v = tempo.set(pfb2.getTargetX() - player.getX(), pfb2.getTargetY() - player.getY());
                Vector2 u = tempo2.set(-1.0f, 0.0f);
                float uLen = 1.0f;
                float vDotU = v.dot(u);
                float forwardComponent = vDotU / 1.0f;
                u = tempo2.set(0.0f, -1.0f);
                vDotU = v.dot(u);
                float rightComponent = vDotU / 1.0f;
                tempo.set(rightComponent, forwardComponent);
                tempo.normalize();
                tempo.rotate(0.7853982f);
                moveVars.moveX = MPDebugAI.tempo.x;
                moveVars.moveY = MPDebugAI.tempo.y;
            }
            if (moveVars.moveX != 0.0f || moveVars.moveY != 0.0f) {
                if (player.stateMachine.getCurrent() == PathFindState.instance()) {
                    player.setDefaultState();
                }
                player.setJustMoved(true);
                player.setMoveDelta(1.0f);
                if (player.isStrafing()) {
                    tempo.set(moveVars.moveX, moveVars.moveY);
                    tempo.normalize();
                    float angle = player.legsSprite.modelSlot.model.animPlayer.getRenderedAngle();
                    angle = (float)((double)angle + 0.7853981633974483);
                    if ((double)angle > Math.PI * 2) {
                        angle = (float)((double)angle - Math.PI * 2);
                    }
                    if (angle < 0.0f) {
                        angle = (float)((double)angle + Math.PI * 2);
                    }
                    tempo.rotate(angle);
                    moveVars.strafeX = MPDebugAI.tempo.x;
                    moveVars.strafeY = MPDebugAI.tempo.y;
                } else {
                    tempo.set(moveVars.moveX, -moveVars.moveY);
                    tempo.normalize();
                    tempo.rotate(-0.7853982f);
                    player.setForwardDirection(tempo);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean updateInputState(IsoPlayer player, IsoPlayer.InputState state) {
        if (GameClient.client && player.isLocalPlayer()) {
            if (DebugOptions.instance.multiplayer.debug.attackPlayer.getValue()) {
                state.reset();
                IsoPlayer p = MPDebugAI.getNearestPlayer(player);
                if (p != null) {
                    state.isCharging = true;
                    if (p.getDistanceSq(player) < 0.5f) {
                        state.melee = true;
                        state.isAttacking = true;
                    }
                }
                return true;
            }
            if (DebugOptions.instance.multiplayer.debug.followPlayer.getValue()) {
                state.reset();
                return true;
            }
        }
        return false;
    }
}

