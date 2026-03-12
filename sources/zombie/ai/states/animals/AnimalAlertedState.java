/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states.animals;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;

public final class AnimalAlertedState
extends State {
    private static final AnimalAlertedState INSTANCE = new AnimalAlertedState();
    private float alertedFor;
    private float spottedDist;

    public static AnimalAlertedState instance() {
        return INSTANCE;
    }

    private AnimalAlertedState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        this.alertedFor = 0.0f;
        if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null) {
            this.spottedDist = animal.alertedChr.getCurrentSquare().DistToProper((int)animal.getX(), (int)animal.getY());
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.alertedChr == null || animal.alertedChr.getCurrentSquare() == null) {
            animal.setDefaultState();
            return;
        }
        this.setTurnAlertedValues(animal, animal.alertedChr);
        if (this.alertedFor > 1000.0f) {
            animal.setIsAlerted(false);
            animal.alertedChr = null;
            animal.setDefaultState();
        }
        this.alertedFor += GameTime.getInstance().getMultiplier();
        if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null) {
            float dist = animal.alertedChr.getCurrentSquare().DistToProper((int)animal.getX(), (int)animal.getY());
            if (animal.alertedChr != null && animal.alertedChr.getCurrentSquare() != null && (dist < this.spottedDist - 2.0f || dist <= 4.0f)) {
                animal.spottedChr = animal.alertedChr;
                animal.getBehavior().lastAlerted = 10000.0f;
                animal.setIsAlerted(false);
                animal.alertedChr = null;
            }
        }
    }

    public void setTurnAlertedValues(IsoAnimal animal, IsoMovingObject chr) {
        float degX;
        IsoAnimal.tempVector2.x = animal.getX() - chr.getX();
        IsoAnimal.tempVector2.y = animal.getY() - chr.getY();
        float radDirOfSound = IsoAnimal.tempVector2.getDirectionNeg();
        radDirOfSound = radDirOfSound < 0.0f ? Math.abs(radDirOfSound) : (float)(Math.PI * 2 - (double)radDirOfSound);
        double degreeOfSound = Math.toDegrees(radDirOfSound);
        IsoAnimal.tempVector2.x = animal.getDir().Rot180().ToVector().x;
        IsoAnimal.tempVector2.y = animal.getDir().Rot180().ToVector().y;
        IsoAnimal.tempVector2.normalize();
        float radDirOfZombie = IsoAnimal.tempVector2.getDirectionNeg();
        radDirOfZombie = radDirOfZombie < 0.0f ? Math.abs(radDirOfZombie) : (float)Math.PI * 2 - radDirOfZombie;
        double degreeOfZombie = Math.toDegrees(radDirOfZombie);
        if ((int)degreeOfZombie == 360) {
            degreeOfZombie = 0.0;
        }
        if ((int)degreeOfSound == 360) {
            degreeOfSound = 0.0;
        }
        if (degreeOfSound > degreeOfZombie) {
            int sumUpDegree = (int)(degreeOfSound - degreeOfZombie);
            if (sumUpDegree > 180) {
                sumUpDegree = 180 - (sumUpDegree - 180);
                degX = (float)sumUpDegree / 180.0f - (float)sumUpDegree / 180.0f * 2.0f;
            } else {
                degX = (float)sumUpDegree / 180.0f;
            }
        } else {
            int sumUpDegree = (int)(degreeOfZombie - degreeOfSound);
            degX = (float)sumUpDegree / 180.0f - (float)sumUpDegree / 180.0f * 2.0f;
        }
        if (GameClient.client) {
            animal.setVariable("AlertX", IsoAnimal.tempVector2.set(chr.getX() - animal.getX(), chr.getY() - animal.getY()).getDirection());
            animal.setVariable("AlertY", 0.0f);
        } else {
            animal.setVariable("AlertX", degX);
            animal.setVariable("AlertY", 0.0f);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }
}

