/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import org.joml.Vector3f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.iso.Vector2;
import zombie.util.Type;

public class LungeNetworkState
extends State {
    private static final LungeNetworkState INSTANCE = new LungeNetworkState();
    public static final State.Param<Long> TICK_COUNT = State.Param.ofLong("tick_count", 0L);
    private final Vector2 temp = new Vector2();
    private final Vector3f worldPos = new Vector3f();

    public static LungeNetworkState instance() {
        return INSTANCE;
    }

    private LungeNetworkState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        WalkTowardNetworkState.instance().enter(owner);
        IsoZombie zombie = (IsoZombie)owner;
        zombie.lungeTimer = 180.0f;
        owner.set(TICK_COUNT, 0L);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        long tickCount;
        WalkTowardNetworkState.instance().execute(owner);
        IsoZombie zombie = (IsoZombie)owner;
        owner.setOnFloor(false);
        owner.setShootable(true);
        if (zombie.lunger) {
            zombie.walkVariantUse = "ZombieWalk3";
        }
        zombie.lungeTimer -= GameTime.getInstance().getThirtyFPSMultiplier();
        IsoPlayer player = Type.tryCastTo(zombie.getTarget(), IsoPlayer.class);
        if (player != null && player.isGhostMode()) {
            zombie.lungeTimer = 0.0f;
        }
        if (zombie.lungeTimer < 0.0f) {
            zombie.lungeTimer = 0.0f;
        }
        if (zombie.lungeTimer <= 0.0f) {
            zombie.allowRepathDelay = 0.0f;
        }
        if ((tickCount = owner.get(TICK_COUNT).longValue()) == 2L) {
            ((IsoZombie)owner).parameterZombieState.setState(ParameterZombieState.State.LockTarget);
        }
        owner.set(TICK_COUNT, tickCount + 1L);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        WalkTowardNetworkState.instance().exit(owner);
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return true;
    }
}

