/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.iso.IsoMovingObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ZombieEatBodyState
extends State {
    private static final ZombieEatBodyState INSTANCE = new ZombieEatBodyState();
    public static final State.Param<IsoDeadBody> BODY = State.Param.of("body", IsoDeadBody.class);

    public static ZombieEatBodyState instance() {
        return INSTANCE;
    }

    private ZombieEatBodyState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.setStateEventDelayTimer(Rand.Next(1800.0f, 3600.0f));
        zombie.setVariable("onknees", Rand.Next(3) != 0);
        if (zombie.getEatBodyTarget() instanceof IsoDeadBody) {
            IsoDeadBody bodyToEat = (IsoDeadBody)zombie.eatBodyTarget;
            if (!zombie.isEatingOther(bodyToEat)) {
                owner.set(BODY, bodyToEat);
                bodyToEat.getEatingZombies().add(zombie);
            }
            if (GameClient.client && zombie.isLocal()) {
                GameClient.sendEatBody(zombie, zombie.getEatBodyTarget());
            }
        } else if (zombie.getEatBodyTarget() instanceof IsoPlayer && GameClient.client && zombie.isLocal()) {
            GameClient.sendEatBody(zombie, zombie.getEatBodyTarget());
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoMovingObject targetChar = zombie.getEatBodyTarget();
        if (zombie.getStateEventDelayTimer() <= 0.0f) {
            zombie.setEatBodyTarget(null, false);
        } else if (!GameServer.server && !Core.soundDisabled && Rand.Next(Rand.AdjustForFramerate(15)) == 0) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Eating);
        }
        zombie.timeSinceSeenFlesh = 0.0f;
        if (targetChar != null) {
            zombie.faceThisObject(targetChar);
        }
        if (Rand.Next(Rand.AdjustForFramerate(450)) == 0) {
            zombie.getCurrentSquare().getChunk().addBloodSplat(zombie.getX() + Rand.Next(-0.5f, 0.5f), zombie.getY() + Rand.Next(-0.5f, 0.5f), zombie.getZ(), Rand.Next(8));
            if (Rand.Next(6) == 0) {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.B, zombie.getCell(), zombie.getX(), zombie.getY(), zombie.getZ() + 0.3f, Rand.Next(-0.2f, 0.2f) * 1.5f, Rand.Next(-0.2f, 0.2f) * 1.5f);
            } else {
                new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, zombie.getCell(), zombie.getX(), zombie.getY(), zombie.getZ() + 0.3f, Rand.Next(-0.2f, 0.2f) * 1.5f, Rand.Next(-0.2f, 0.2f) * 1.5f);
            }
            if (Rand.Next(4) == 0) {
                zombie.addBlood(null, true, false, false);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoDeadBody isoDeadBody = owner.get(BODY);
        if (isoDeadBody instanceof IsoDeadBody) {
            IsoDeadBody deadBody = isoDeadBody;
            deadBody.getEatingZombies().remove(zombie);
        }
        if (zombie.parameterZombieState.isState(ParameterZombieState.State.Eating)) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
        }
        if (GameClient.client && zombie.isLocal()) {
            GameClient.sendEatBody(zombie, null);
        }
    }
}

