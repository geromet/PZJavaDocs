/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

public class NetworkVariables {

    public static enum ZombieState {
        Idle("idle"),
        WalkToward("walktoward"),
        TurnAlerted("turnalerted"),
        PathFind("pathfind"),
        Sitting("sitting"),
        HitReaction("hitreaction"),
        HitReactionHit("hitreaction-hit"),
        Thump("thump"),
        ClimbFence("climbfence"),
        Lunge("lunge"),
        Attack("attack"),
        AttackNetwork("attack-network"),
        AttackVehicle("attackvehicle"),
        AttackVehicleNetwork("attackvehicle-network"),
        Bumped("bumped"),
        ClimbWindow("climbwindow"),
        EatBody("eatbody"),
        FaceTarget("face-target"),
        FakeDead("fakedead"),
        FakeDeadAttack("fakedead-attack"),
        FakeDeadAttackNetwork("fakedead-attack-network"),
        FallDown("falldown"),
        Falling("falling"),
        GetDown("getdown"),
        Getup("getup"),
        Grappled("grappled"),
        HitWhileStaggered("hitwhilestaggered"),
        LungeNetwork("lunge-network"),
        OnGround("onground"),
        StaggerBack("staggerback"),
        WalkTowardNetwork("walktoward-network"),
        AnimalAlerted("alerted"),
        AnimalDeath("death"),
        AnimalEating("eating"),
        AnimalHutch("hutch"),
        AnimalTrailer("trailer"),
        AnimalWalk("walk"),
        AnimalZone("zone"),
        FakeZombieStay("fakezombie-stay"),
        FakeZombieNormal("fakezombie-normal"),
        FakeZombieAttack("fakezombie-attack");

        private final String zombieState;

        private ZombieState(String zombieState) {
            this.zombieState = zombieState.toLowerCase();
        }

        public String toString() {
            return this.zombieState;
        }

        public static ZombieState fromString(String zombieState) {
            if (zombieState == null) {
                return Idle;
            }
            String zombieStateLC = zombieState.toLowerCase();
            for (ZombieState type : ZombieState.values()) {
                if (!type.zombieState.equals(zombieStateLC)) continue;
                return type;
            }
            return Idle;
        }

        public byte toByte() {
            return (byte)this.ordinal();
        }
    }

    public static enum ThumpType {
        TTNone(""),
        TTDoor("Door"),
        TTClaw("DoorClaw"),
        TTBang("DoorBang");

        private final String thumpType;

        private ThumpType(String thumpType) {
            this.thumpType = thumpType;
        }

        public String toString() {
            return this.thumpType;
        }

        public static ThumpType fromString(String thumpType) {
            for (ThumpType type : ThumpType.values()) {
                if (!type.thumpType.equalsIgnoreCase(thumpType)) continue;
                return type;
            }
            return TTNone;
        }
    }

    public static enum WalkType {
        WT1("1"),
        WT2("2"),
        WT3("3"),
        WT4("4"),
        WT5("5"),
        WTSprint1("sprint1"),
        WTSprint2("sprint2"),
        WTSprint3("sprint3"),
        WTSprint4("sprint4"),
        WTSprint5("sprint5"),
        WTSlow1("slow1"),
        WTSlow2("slow2"),
        WTSlow3("slow3");

        private final String walkType;

        private WalkType(String walkType) {
            this.walkType = walkType;
        }

        public String toString() {
            return this.walkType;
        }

        public static WalkType fromString(String walkType) {
            for (WalkType type : WalkType.values()) {
                if (!type.walkType.equalsIgnoreCase(walkType)) continue;
                return type;
            }
            return WT1;
        }
    }
}

