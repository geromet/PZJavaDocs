/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.core.physics.RagdollBodyPart;
import zombie.scripting.objects.AmmoType;

public class PhysicsHitReaction {
    public boolean useImpulseOverride;
    public AmmoType ammoType;
    public String physicsObject;
    public float overrideForwardImpulse = 80.0f;
    public float overrideUpwardImpulse = 40.0f;
    public float[] impulse = new float[RagdollBodyPart.BODYPART_COUNT.ordinal()];
    public float[] upwardImpulse = new float[RagdollBodyPart.BODYPART_COUNT.ordinal()];
}

