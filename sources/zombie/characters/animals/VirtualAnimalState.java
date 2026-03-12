/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.characters.animals.AnimalChunk;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalTracksDefinitions;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.animals.VirtualAnimal;
import zombie.core.random.Rand;
import zombie.util.list.PZArrayUtil;

public abstract class VirtualAnimalState {
    static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList();
    static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList();
    static float distanceBoost = 1.0f;
    protected VirtualAnimal animal;

    public void addAnimalTracks(AnimalTracksDefinitions.AnimalTracksType trackType) {
        if (trackType == null) {
            return;
        }
        AnimalChunk chunk = AnimalManagerWorker.getInstance().getAnimalChunk(this.animal.x, this.animal.y);
        if (chunk == null) {
            return;
        }
        for (int i = 0; i < chunk.animalTracks.size(); ++i) {
            if (!chunk.animalTracks.get((int)i).animalType.equalsIgnoreCase(this.animal.migrationGroup) || !chunk.animalTracks.get((int)i).trackType.equalsIgnoreCase(trackType.type)) continue;
            return;
        }
        chunk.addTracks(this.animal, trackType);
    }

    public VirtualAnimalState(VirtualAnimal animal) {
        this.animal = animal;
    }

    public abstract void update();

    protected AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
        return null;
    }

    protected void reachedEnd() {
    }

    void moveAlongPath(String action, float distance) {
        AnimalZone closestZone = AnimalZones.getInstance().getClosestZone(this.animal.x, this.animal.y, action);
        if (closestZone == null) {
            this.animal.animals.forEach(isoAnimal -> isoAnimal.setAnimalZone(null));
            return;
        }
        this.moveAlongPath(distance * distanceBoost * this.animal.speed, closestZone);
    }

    void moveAlongPath(float distance, AnimalZone zone) {
        AnimalZoneJunction junction;
        Vector2f pos = new Vector2f();
        float t = zone.getClosestPointOnPolyline(this.animal.x, this.animal.y, pos);
        float zoneLength = zone.getPolylineLength();
        boolean moveForwardOnZone = this.animal.moveForwardOnZone;
        float t2 = moveForwardOnZone ? t + distance / zoneLength : t - distance / zoneLength;
        zone.getJunctionsBetween(t, t2, tempJunctions);
        if (!tempJunctions.isEmpty() && (junction = this.visitJunction(zone, tempJunctions.get(0).isFirstPointOnZone1() || tempJunctions.get(0).isLastPointOnZone1(), tempJunctions)) != null) {
            zone = junction.zoneOther;
            t2 = zone.getDistanceOfPointFromStart(junction.pointIndexOther) / zone.getPolylineLength();
            this.animal.moveForwardOnZone = junction.isFirstPointOnZone2() || !junction.isLastPointOnZone2() && Rand.NextBool(2);
            t2 = this.animal.moveForwardOnZone ? t2 + 1.0f / zone.getPolylineLength() : t2 - 1.0f / zone.getPolylineLength();
            this.animal.currentZoneAction = zone.action;
        }
        if (t2 >= 1.0f && moveForwardOnZone) {
            t2 = 1.0f;
            this.animal.moveForwardOnZone = false;
            this.reachedEnd();
        }
        if (t2 <= 0.0f && !moveForwardOnZone) {
            t2 = 0.0f;
            this.animal.moveForwardOnZone = true;
            this.reachedEnd();
        }
        zone.getPointOnPolyline(t2, pos);
        zone.getDirectionOnPolyline(t2, this.animal.forwardDirection);
        if (!this.animal.moveForwardOnZone) {
            this.animal.forwardDirection.mul(-1.0f);
        }
        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        AnimalZone zone2 = zone;
        this.animal.animals.forEach(isoAnimal -> {
            isoAnimal.setAnimalZone(zone2);
            isoAnimal.setMoveForwardOnZone(this.animal.moveForwardOnZone);
        });
        mgrWorker.moveAnimal(this.animal, pos.x, pos.y);
    }

    public static class StateSleep
    extends VirtualAnimalState {
        public StateSleep(VirtualAnimal animal) {
            super(animal);
            animal.wakeTime = GameTime.getInstance().getWorldAgeHours() + (double)((float)this.animal.timeToSleep / 60.0f);
        }

        @Override
        public void update() {
            this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "sleep"));
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours > this.animal.wakeTime) {
                this.animal.debugForceSleep = false;
                this.animal.state = new StateMoveFromSleep(this.animal);
                this.animal.nextRestTime = -1.0;
            }
        }
    }

    public static class StateMoveFromEat
    extends VirtualAnimalState {
        public StateMoveFromEat(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();
            for (int i = 0; i < junctions.size(); ++i) {
                AnimalZoneJunction junction = junctions.get(i);
                if (!"Follow".equals(junction.zoneOther.action)) continue;
                possibleJunctions.add(junction);
            }
            AnimalZoneJunction junction = (AnimalZoneJunction)PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            }
            this.animal.state = new StateFollow(this.animal);
            return junction;
        }
    }

    public static class StateMoveToEat
    extends VirtualAnimalState {
        public StateMoveToEat(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public void reachedEnd() {
            this.animal.state = new StateEat(this.animal);
        }
    }

    public static class StateMoveFromSleep
    extends VirtualAnimalState {
        public StateMoveFromSleep(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();
            for (int i = 0; i < junctions.size(); ++i) {
                AnimalZoneJunction junction = junctions.get(i);
                if (!"Follow".equals(junction.zoneOther.action)) continue;
                possibleJunctions.add(junction);
            }
            AnimalZoneJunction junction = (AnimalZoneJunction)PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            }
            this.animal.state = new StateFollow(this.animal);
            return junction;
        }
    }

    public static class StateMoveToSleep
    extends VirtualAnimalState {
        public StateMoveToSleep(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2f);
        }

        @Override
        public void reachedEnd() {
            this.animal.state = new StateSleep(this.animal);
        }
    }

    public static class StateFollow
    extends VirtualAnimalState {
        public StateFollow(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getMultiplier() / 2.0f * 0.2f);
            if (this.animal.isRemoved()) {
                return;
            }
            this.checkNextEatTime();
            if ("Eat".equals(this.animal.currentZoneAction) && this.animal.isTimeToEat()) {
                this.animal.state = new StateMoveToEat(this.animal);
                return;
            }
            this.checkNextRestTime();
            if ("Sleep".equals(this.animal.currentZoneAction) && this.animal.isTimeToSleep()) {
                this.animal.state = new StateMoveToSleep(this.animal);
                return;
            }
            this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "walk"));
        }

        private void checkNextEatTime() {
            if (this.animal.nextEatTime < 0.0) {
                this.animal.nextEatTime = MigrationGroupDefinitions.getNextEatTime(this.animal.migrationGroup);
            }
        }

        private void checkNextRestTime() {
            if (this.animal.nextRestTime < 0.0) {
                this.animal.nextRestTime = MigrationGroupDefinitions.getNextSleepTime(this.animal.migrationGroup);
            }
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();
            for (int i = 0; i < junctions.size(); ++i) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Eat".equals(junction.zoneOther.action) && this.animal.isTimeToEat()) {
                    return junction;
                }
                if ("Sleep".equals(junction.zoneOther.action) && this.animal.isTimeToSleep()) {
                    return junction;
                }
                if (!"Follow".equals(junction.zoneOther.action)) continue;
                possibleJunctions.add(junction);
            }
            if ("Follow".equals(zone.action) && !bEndPoint && Rand.NextBool(possibleJunctions.size() + 1)) {
                return null;
            }
            return (AnimalZoneJunction)PZArrayUtil.pickRandom(possibleJunctions);
        }
    }

    public static class StateEat
    extends VirtualAnimalState {
        public StateEat(VirtualAnimal animal) {
            super(animal);
            animal.eatStartTime = GameTime.getInstance().getWorldAgeHours();
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "eat"));
            if (worldAgeHours - this.animal.eatStartTime > (double)((float)this.animal.timeToEat / 60.0f)) {
                this.animal.state = new StateMoveFromEat(this.animal);
                this.animal.nextEatTime = -1.0;
                this.animal.debugForceEat = false;
            }
        }
    }
}

