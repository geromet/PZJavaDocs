/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public class AttackNetworkState
extends State {
    private static final AttackNetworkState INSTANCE = new AttackNetworkState();
    public static final State.Param<Boolean> SKIP_TEST_DEFENCE = State.Param.ofBool("skip_test_defence", false);
    private String attackOutcome;

    public static AttackNetworkState instance() {
        return INSTANCE;
    }

    private AttackNetworkState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        owner.clear(this);
        owner.set(SKIP_TEST_DEFENCE, false);
        this.attackOutcome = zombie.getAttackOutcome();
        zombie.setAttackOutcome("start");
        owner.clearVariable("AttackDidDamage");
        owner.clearVariable("ZombieBiteDone");
        zombie.setTargetSeenTime(1.0f);
        if (!zombie.crawling) {
            zombie.setVariable("AttackType", "bite");
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
        if (targetChar != null && "Chainsaw".equals(targetChar.getVariableString("ZombieHitReaction"))) {
            return;
        }
        String outcome = zombie.getAttackOutcome();
        if (!(!"success".equals(outcome) || owner.getVariableBoolean("bAttack") || targetChar != null && targetChar.isGodMod() || owner.getVariableBoolean("AttackDidDamage") || owner.getVariableString("ZombieBiteDone") == "true")) {
            zombie.setAttackOutcome("interrupted");
        }
        if (targetChar == null || targetChar.isDead()) {
            zombie.setTargetSeenTime(10.0f);
        }
        if (!(targetChar == null || owner.get(SKIP_TEST_DEFENCE).booleanValue() || "started".equals(outcome) || StringUtils.isNullOrEmpty(owner.getVariableString("PlayerHitReaction")))) {
            owner.set(SKIP_TEST_DEFENCE, true);
        }
        zombie.setShootable(true);
        if (zombie.target != null && !zombie.crawling) {
            if (!"fail".equals(outcome) && !"interrupted".equals(outcome)) {
                zombie.faceThisObject(zombie.target);
            }
            zombie.setOnFloor(false);
        }
        if (zombie.target != null) {
            zombie.target.setTimeSinceZombieAttack(0);
            zombie.target.setLastTargettedBy(zombie);
        }
        if (!zombie.crawling) {
            zombie.setVariable("AttackType", "bite");
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        owner.clearVariable("AttackOutcome");
        owner.clearVariable("AttackType");
        owner.clearVariable("PlayerHitReaction");
        owner.setStateMachineLocked(false);
        if (zombie.target != null && zombie.target.isOnFloor()) {
            zombie.setEatBodyTarget(zombie.target, true);
            zombie.setTarget(null);
        }
        zombie.allowRepathDelay = 0.0f;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (GameClient.client && zombie.isRemoteZombie()) {
            IsoMovingObject isoMovingObject;
            if (event.eventName.equalsIgnoreCase("SetAttackOutcome")) {
                zombie.setAttackOutcome("fail".equals(this.attackOutcome) ? "fail" : "success");
            }
            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck") && (isoMovingObject = zombie.target) instanceof IsoPlayer) {
                IsoPlayer player = (IsoPlayer)isoMovingObject;
                if (zombie.scratch) {
                    zombie.getEmitter().playSoundImpl("ZombieScratch", zombie);
                } else if (zombie.laceration) {
                    zombie.getEmitter().playSoundImpl("ZombieScratch", zombie);
                } else {
                    zombie.getEmitter().playSoundImpl(zombie.getBiteSoundName(), zombie);
                    player.splatBloodFloorBig();
                    player.splatBloodFloorBig();
                    player.splatBloodFloorBig();
                }
            }
            if (event.eventName.equalsIgnoreCase("EatBody")) {
                owner.setVariable("EatingStarted", true);
                ((IsoZombie)owner).setEatBodyTarget(((IsoZombie)owner).target, true);
                ((IsoZombie)owner).setTarget(null);
            }
        }
        if (event.eventName.equalsIgnoreCase("SetState")) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Attack);
        }
    }

    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
    }
}

