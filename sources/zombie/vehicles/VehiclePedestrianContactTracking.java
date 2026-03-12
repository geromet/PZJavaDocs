/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugType;
import zombie.util.Pool;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePedestrianContact;

public class VehiclePedestrianContactTracking {
    private final ArrayList<IsoGameCharacter> hitsThisFrame = new ArrayList();
    private final ArrayList<IsoGameCharacter> hitsCausingDamage = new ArrayList();
    private final ArrayList<VehiclePedestrianContact> persistentContacts = new ArrayList();
    private float timeNow;
    private final float maxTimeOfContact = 3.5f;

    public void postUpdate(BaseVehicle vehicle, Invokers.Params2.ICallback<BaseVehicle, VehiclePedestrianContactTracking> applyDamageCallback) {
        this.updateTimeNow();
        this.populateHitsCausingDamage(vehicle);
        this.moveHitsThisFrameToPersistentContacts();
        this.clearOldContacts();
        this.applyDamage(vehicle, applyDamageCallback);
    }

    private void updateTimeNow() {
        if (this.persistentContacts.isEmpty()) {
            this.timeNow = 0.0f;
            return;
        }
        float timeDelta = GameTime.getInstance().getTimeDelta();
        this.timeNow += timeDelta;
    }

    private void applyDamage(BaseVehicle vehicle, Invokers.Params2.ICallback<BaseVehicle, VehiclePedestrianContactTracking> applyDamageCallback) {
        if (applyDamageCallback != null) {
            applyDamageCallback.accept(vehicle, this);
        }
        this.hitsCausingDamage.clear();
    }

    private void populateHitsCausingDamage(BaseVehicle vehicle) {
        this.hitsCausingDamage.clear();
        for (int i = 0; i < this.hitsThisFrame.size(); ++i) {
            IsoGameCharacter hitChar = this.hitsThisFrame.get(i);
            if (!hitChar.causesDamageToVehicleWhenHit(vehicle) || this.isPersistentContact(hitChar)) continue;
            this.hitsCausingDamage.add(hitChar);
        }
        if (!this.hitsCausingDamage.isEmpty()) {
            DebugType.VehicleHit.debugln("HitsCausingDamage: %d", this.hitsCausingDamage.size());
        }
    }

    public boolean isPersistentContact(IsoGameCharacter hitChar) {
        return PZArrayUtil.contains(this.persistentContacts, hitChar, (chr, contact) -> contact.character == chr);
    }

    private void moveHitsThisFrameToPersistentContacts() {
        for (int i = 0; i < this.hitsThisFrame.size(); ++i) {
            IsoGameCharacter hitChar = this.hitsThisFrame.get(i);
            int indexOf = PZArrayUtil.indexOf(this.persistentContacts, hitChar, (chr, contact) -> contact.character == chr);
            if (indexOf == -1) {
                VehiclePedestrianContact newContact = VehiclePedestrianContact.alloc();
                newContact.character = hitChar;
                newContact.hitTime = this.getTimeNow();
                this.persistentContacts.add(newContact);
                continue;
            }
            VehiclePedestrianContact existingContact = this.persistentContacts.get(i);
            existingContact.hitTime = this.getTimeNow();
        }
        this.hitsThisFrame.clear();
    }

    private float getTimeNow() {
        return this.timeNow;
    }

    private void clearOldContacts() {
        for (int i = 0; i < this.persistentContacts.size(); ++i) {
            VehiclePedestrianContact contact = this.persistentContacts.get(i);
            float timeOfContact = contact.hitTime;
            float timeNow = this.getTimeNow();
            float contactAge = timeNow - timeOfContact;
            if (!(contactAge > 3.5f)) continue;
            this.persistentContacts.remove(i--);
            Pool.tryRelease(contact);
        }
    }

    public void onCharacterHit(IsoGameCharacter hitChar) {
        if (this.hitsThisFrame.contains(hitChar)) {
            return;
        }
        this.hitsThisFrame.add(hitChar);
    }

    public ArrayList<IsoGameCharacter> getHitsCausingDamage() {
        return this.hitsCausingDamage;
    }
}

