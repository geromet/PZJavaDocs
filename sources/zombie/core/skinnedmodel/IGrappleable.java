/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public interface IGrappleable {
    public void Grappled(IGrappleable var1, HandWeapon var2, float var3, String var4);

    public void AcceptGrapple(IGrappleable var1, String var2);

    public void RejectGrapple(IGrappleable var1);

    public void LetGoOfGrappled(String var1);

    public void GrapplerLetGo(IGrappleable var1, String var2);

    public GrappleOffsetBehaviour getGrappleOffsetBehaviour();

    public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour var1);

    public boolean isDoGrapple();

    public void setDoGrapple(boolean var1);

    default public void setDoGrappleLetGo() {
        this.setDoContinueGrapple(false);
    }

    public IAnimatable getAnimatable();

    public static IAnimatable getAnimatable(IGrappleable grappleable) {
        return grappleable != null ? grappleable.getAnimatable() : null;
    }

    public boolean isDoContinueGrapple();

    public void setDoContinueGrapple(boolean var1);

    public IGrappleable getGrappledBy();

    public String getGrappledByString();

    public String getGrappledByType();

    public boolean isGrappling();

    public boolean isBeingGrappled();

    public boolean isBeingGrappledBy(IGrappleable var1);

    public Vector2 getAnimForwardDirection(Vector2 var1);

    public org.joml.Vector3f getTargetGrapplePos(org.joml.Vector3f var1);

    public Vector3 getTargetGrapplePos(Vector3 var1);

    default public void setTargetGrapplePos(org.joml.Vector3f grapplePos) {
        this.setTargetGrapplePos(grapplePos.x, grapplePos.y, grapplePos.z);
    }

    default public void setTargetGrapplePos(Vector3 grapplePos) {
        this.setTargetGrapplePos(grapplePos.x, grapplePos.y, grapplePos.z);
    }

    public void setTargetGrapplePos(float var1, float var2, float var3);

    public Vector2 getTargetGrappleRotation(Vector2 var1);

    default public void setTargetGrappleRotation(Vector2 forward) {
        this.setTargetGrappleRotation(forward.x, forward.y);
    }

    public void setTargetGrappleRotation(float var1, float var2);

    default public void setGrappleDeferredOffset(org.joml.Vector3f grappleOffset) {
        this.setGrappleDeferredOffset(grappleOffset.x, grappleOffset.y, grappleOffset.z);
    }

    default public void setGrappleDeferredOffset(Vector3 grappleOffset) {
        this.setGrappleDeferredOffset(grappleOffset.x, grappleOffset.y, grappleOffset.z);
    }

    public void setGrappleDeferredOffset(float var1, float var2, float var3);

    public org.joml.Vector3f getGrappleOffset(org.joml.Vector3f var1);

    public Vector3 getGrappleOffset(Vector3 var1);

    public void setForwardDirection(float var1, float var2);

    public void setTargetAndCurrentDirection(float var1, float var2);

    public Vector3 getPosition(Vector3 var1);

    public Vector3f getPosition(Vector3f var1);

    default public void setPosition(Vector3 position) {
        this.setPosition(position.x, position.y, position.z);
    }

    public void setPosition(float var1, float var2, float var3);

    public float getGrapplePosOffsetForward();

    public void setGrapplePosOffsetForward(float var1);

    public float getGrappleRotOffsetYaw();

    public void setGrappleRotOffsetYaw(float var1);

    public boolean isGrapplingTarget(IGrappleable var1);

    public IGrappleable getGrapplingTarget();

    public float getBearingToGrappledTarget();

    public float getBearingFromGrappledTarget();

    public String getSharedGrappleType();

    public void setSharedGrappleType(String var1);

    public String getSharedGrappleAnimNode();

    public void setSharedGrappleAnimNode(String var1);

    public float getSharedGrappleAnimTime();

    public float getSharedGrappleAnimFraction();

    public void setSharedGrappleAnimTime(float var1);

    public void setSharedGrappleAnimFraction(float var1);

    public String getGrappleResult();

    public void setGrappleResult(String var1);

    default public int getID() {
        return -1;
    }

    public boolean canBeGrappled();

    public boolean isPerformingAnyGrappleAnimation();

    public boolean isPerformingGrappleGrabAnimation();

    public void setPerformingGrappleGrabAnimation(boolean var1);

    public boolean isPerformingGrappleAnimation();

    public boolean isOnFloor();

    public void setOnFloor(boolean var1);

    public boolean isFallOnFront();

    public void setFallOnFront(boolean var1);

    public boolean isKilledByFall();

    public void setKilledByFall(boolean var1);

    default public boolean isMoving() {
        return false;
    }

    public void resetGrappleStateToDefault(String var1);
}

