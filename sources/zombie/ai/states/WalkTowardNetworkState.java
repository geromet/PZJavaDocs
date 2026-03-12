/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoChunk;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PathFindBehavior2;
import zombie.pathfind.PolygonalMap2;

public class WalkTowardNetworkState
extends State {
    private static final WalkTowardNetworkState INSTANCE = new WalkTowardNetworkState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);

    public static WalkTowardNetworkState instance() {
        return INSTANCE;
    }

    private WalkTowardNetworkState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.set(TICK_COUNT, 0L);
        owner.setVariable("bMoving", true);
        owner.setVariable("bPathfind", false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        long tickCount;
        IsoGameCharacter isoGameCharacter;
        IsoZombie zombie = (IsoZombie)owner;
        PathFindBehavior2 pfb2 = zombie.getPathFindBehavior2();
        zombie.vectorToTarget.x = zombie.networkAi.targetX - zombie.getX();
        zombie.vectorToTarget.y = zombie.networkAi.targetY - zombie.getY();
        pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
        if (zombie.getZ() == (float)zombie.networkAi.targetZ && (zombie.networkAi.predictionType == 3 || zombie.networkAi.predictionType == 4)) {
            if (zombie.networkAi.usePathFind) {
                pfb2.reset();
                zombie.setPath2(null);
                zombie.networkAi.usePathFind = false;
            }
            pfb2.moveToPoint(zombie.networkAi.targetX, zombie.networkAi.targetY, 1.0f);
            zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5f);
        } else if (zombie.getZ() == (float)zombie.networkAi.targetZ && !PolygonalMap2.instance.lineClearCollide(zombie.getX(), zombie.getY(), zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.networkAi.targetZ, null)) {
            if (zombie.networkAi.usePathFind) {
                pfb2.reset();
                zombie.setPath2(null);
                zombie.networkAi.usePathFind = false;
            }
            pfb2.moveToPoint(zombie.networkAi.targetX, zombie.networkAi.targetY, 1.0f);
            zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5f);
        } else if (zombie.getZ() == (float)zombie.networkAi.targetZ && !PolygonalMap2.instance.lineClearCollide(zombie.getX(), zombie.getY(), zombie.realx, zombie.realy, zombie.realz, null)) {
            if (zombie.networkAi.usePathFind) {
                pfb2.reset();
                zombie.setPath2(null);
                zombie.networkAi.usePathFind = false;
            }
            pfb2.moveToPoint(zombie.realx, zombie.realy, 1.0f);
            zombie.setVariable("bMoving", IsoUtils.DistanceManhatten(zombie.networkAi.targetX, zombie.networkAi.targetY, zombie.getNextX(), zombie.getNextY()) > 0.5f);
        } else {
            PathFindBehavior2.BehaviorResult result;
            if (!zombie.networkAi.usePathFind) {
                pfb2.pathToLocationF(zombie.realx, zombie.realy, zombie.realz);
                pfb2.walkingOnTheSpot.reset(zombie.getX(), zombie.getY());
                zombie.networkAi.usePathFind = true;
            }
            if ((result = pfb2.update()) == PathFindBehavior2.BehaviorResult.Failed) {
                zombie.setPathFindIndex(-1);
                return;
            }
            if (result == PathFindBehavior2.BehaviorResult.Succeeded) {
                IsoChunk chunk;
                int tx = PZMath.fastfloor(zombie.getPathFindBehavior2().getTargetX());
                int ty = PZMath.fastfloor(zombie.getPathFindBehavior2().getTargetY());
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(tx / 8, ty / 8) : IsoWorld.instance.currentCell.getChunkForGridSquare(tx, ty, 0);
                if (chunk == null) {
                    zombie.setVariable("bMoving", true);
                    return;
                }
                zombie.setPath2(null);
                zombie.setVariable("bMoving", true);
                return;
            }
        }
        if (!((IsoZombie)owner).crawling) {
            owner.setOnFloor(false);
        }
        boolean bCollidedWithVehicle = owner.isCollidedWithVehicle();
        IsoMovingObject isoMovingObject = zombie.target;
        if (isoMovingObject instanceof IsoGameCharacter && (isoGameCharacter = (IsoGameCharacter)isoMovingObject).getVehicle() != null && isoGameCharacter.getVehicle().isCharacterAdjacentTo(owner)) {
            bCollidedWithVehicle = false;
        }
        if (owner.isCollidedThisFrame() || bCollidedWithVehicle) {
            zombie.allowRepathDelay = 0.0f;
            zombie.pathToLocation(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
            if (!"true".equals(zombie.getVariableString("bPathfind"))) {
                zombie.setVariable("bPathfind", true);
                zombie.setVariable("bMoving", false);
            }
        }
        if ((tickCount = owner.get(TICK_COUNT).longValue()) == 2L) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }
        owner.set(TICK_COUNT, tickCount + 1L);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bMoving", false);
    }
}

