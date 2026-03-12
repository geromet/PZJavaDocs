/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states.animals;

import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.list.PZArrayUtil;

public final class AnimalZoneState
extends State {
    private static final AnimalZoneState INSTANCE = new AnimalZoneState();
    public static final State.Param<String> ACTION = State.Param.ofString("action", null);
    public static final State.Param<ZoneState> STATE = State.Param.of("state", ZoneState.class);

    public static AnimalZoneState instance() {
        return INSTANCE;
    }

    private AnimalZoneState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.set(ACTION, ((IsoAnimal)owner).getAnimalZone().getAction());
        owner.set(STATE, new StateFollow((IsoAnimal)owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        ZoneState zoneState = owner.get(STATE);
        zoneState.update();
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return owner.isMoving();
    }

    public static class StateFollow
    extends ZoneState {
        double nextRestTime = -1.0;

        public StateFollow(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
            if ("Eat".equals(this.getCurrentZoneAction())) {
                this.setState(new StateMoveToEat(this.animal));
                return;
            }
            if (this.nextRestTime < 0.0) {
                this.nextRestTime = GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(2, 5) / 60.0;
            }
            if (GameTime.getInstance().getWorldAgeHours() > this.nextRestTime) {
                this.setState(new StateSleep(this.animal));
            }
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();
            for (int i = 0; i < junctions.size(); ++i) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Eat".equals(junction.zoneOther.getAction())) {
                    return junction;
                }
                if (!"Follow".equals(junction.zoneOther.getAction())) continue;
                possibleJunctions.add(junction);
            }
            if ("Follow".equals(zone.getAction()) && !bEndPoint && Rand.NextBool(possibleJunctions.size() + 1)) {
                return null;
            }
            return (AnimalZoneJunction)PZArrayUtil.pickRandom(possibleJunctions);
        }
    }

    private static abstract class ZoneState {
        protected static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList();
        protected static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList();
        IsoAnimal animal;

        ZoneState(IsoAnimal animal) {
            this.animal = animal;
        }

        String getCurrentZoneAction() {
            return this.animal.get(ACTION);
        }

        void setCurrentZoneAction(String action) {
            this.animal.set(ACTION, action);
        }

        void setState(ZoneState state) {
            this.animal.set(STATE, state);
        }

        public abstract void update();

        protected AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            return null;
        }

        protected void reachedEnd() {
        }

        void moveAlongPath(String action, float distance) {
            AnimalZone closestZone = this.animal.getAnimalZone();
            if (closestZone == null) {
                this.animal.setAnimalZone(null);
                return;
            }
            this.moveAlongPath(distance, closestZone);
        }

        void moveAlongPath(float distance, AnimalZone zone) {
            AnimalZoneJunction junction;
            Vector2f pos = new Vector2f();
            float t = zone.getClosestPointOnPolyline(this.animal.getX(), this.animal.getY(), pos);
            float zoneLength = zone.getPolylineLength();
            boolean moveForwardOnZone = this.animal.isMoveForwardOnZone();
            float t2 = moveForwardOnZone ? t + distance / zoneLength : t - distance / zoneLength;
            zone.getJunctionsBetween(t, t2, tempJunctions);
            if (!tempJunctions.isEmpty() && (junction = this.visitJunction(zone, tempJunctions.get(0).isFirstPointOnZone1() || tempJunctions.get(0).isLastPointOnZone1(), tempJunctions)) != null) {
                zone = junction.zoneOther;
                t2 = zone.getDistanceOfPointFromStart(junction.pointIndexOther) / zone.getPolylineLength();
                this.animal.setMoveForwardOnZone(junction.isFirstPointOnZone2() || !junction.isLastPointOnZone2() && Rand.NextBool(2));
                t2 = this.animal.isMoveForwardOnZone() ? t2 + 1.0f / zone.getPolylineLength() : t2 - 1.0f / zone.getPolylineLength();
                this.setCurrentZoneAction(zone.getAction());
            }
            if (t2 >= 1.0f && moveForwardOnZone) {
                t2 = 1.0f;
                this.animal.setMoveForwardOnZone(false);
                this.reachedEnd();
            }
            if (t2 <= 0.0f && moveForwardOnZone) {
                t2 = 0.0f;
                this.animal.setMoveForwardOnZone(true);
                this.reachedEnd();
            }
            zone.getPointOnPolyline(t2, pos);
            this.animal.setNextX(pos.x);
            this.animal.setNextY(pos.y);
            zone.getDirectionOnPolyline(t2, pos);
            if (!this.animal.isMoveForwardOnZone()) {
                pos.mul(-1.0f);
            }
            this.animal.setForwardDirection(pos.x, pos.y);
            this.animal.setAnimalZone(zone);
        }
    }

    public static class StateSleep
    extends ZoneState {
        double wakeTime = GameTime.getInstance().getWorldAgeHours() + (double)Rand.Next(1, 3) / 60.0;

        public StateSleep(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours > this.wakeTime) {
                this.setCurrentZoneAction("Follow");
                this.setState(new StateFollow(this.animal));
            }
        }
    }

    public static class StateMoveFromEat
    extends ZoneState {
        public StateMoveFromEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();
            for (int i = 0; i < junctions.size(); ++i) {
                AnimalZoneJunction junction = junctions.get(i);
                if (!"Follow".equals(junction.zoneOther.getAction())) continue;
                possibleJunctions.add(junction);
            }
            AnimalZoneJunction junction = (AnimalZoneJunction)PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            }
            this.setState(new StateFollow(this.animal));
            return junction;
        }
    }

    public static class StateMoveToEat
    extends ZoneState {
        public StateMoveToEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public void reachedEnd() {
            this.setState(new StateEat(this.animal));
        }
    }

    public static class StateEat
    extends ZoneState {
        double startTime = GameTime.getInstance().getWorldAgeHours();

        public StateEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours - this.startTime > 0.03333333333333333) {
                this.setState(new StateMoveFromEat(this.animal));
            }
        }
    }
}

