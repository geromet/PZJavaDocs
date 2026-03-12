/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import org.joml.Vector3f;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.inventory.types.HandWeapon;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public interface IGrappleableWrapper
extends IGrappleable {
    public IGrappleable getWrappedGrappleable();

    @Override
    default public boolean isDoGrapple() {
        return this.getWrappedGrappleable().isDoGrapple();
    }

    @Override
    default public void setDoGrapple(boolean doGrapple) {
        this.getWrappedGrappleable().setDoGrapple(doGrapple);
    }

    @Override
    default public boolean isDoContinueGrapple() {
        return this.getWrappedGrappleable().isDoContinueGrapple();
    }

    @Override
    default public void setDoContinueGrapple(boolean doContinueGrapple) {
        this.getWrappedGrappleable().setDoContinueGrapple(doContinueGrapple);
    }

    @Override
    default public void Grappled(IGrappleable grappler, HandWeapon weapon, float grappleEffectiveness, String grappleType) {
        this.getWrappedGrappleable().Grappled(grappler, weapon, grappleEffectiveness, grappleType);
    }

    @Override
    default public void RejectGrapple(IGrappleable grappleRejector) {
        this.getWrappedGrappleable().RejectGrapple(grappleRejector);
    }

    @Override
    default public void AcceptGrapple(IGrappleable grappleAcceptor, String grappleType) {
        this.getWrappedGrappleable().AcceptGrapple(grappleAcceptor, grappleType);
    }

    @Override
    default public void LetGoOfGrappled(String grappleResult) {
        this.getWrappedGrappleable().LetGoOfGrappled(grappleResult);
    }

    @Override
    default public void GrapplerLetGo(IGrappleable grappler, String grappleResult) {
        this.getWrappedGrappleable().GrapplerLetGo(grappler, grappleResult);
    }

    @Override
    default public GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
        return this.getWrappedGrappleable().getGrappleOffsetBehaviour();
    }

    @Override
    default public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour newBehaviour) {
        this.getWrappedGrappleable().setGrappleoffsetBehaviour(newBehaviour);
    }

    @Override
    default public boolean isBeingGrappled() {
        return this.getWrappedGrappleable().isBeingGrappled();
    }

    @Override
    default public boolean isBeingGrappledBy(IGrappleable grappledBy) {
        return this.getWrappedGrappleable().isBeingGrappledBy(grappledBy);
    }

    @Override
    default public IGrappleable getGrappledBy() {
        return this.getWrappedGrappleable().getGrappledBy();
    }

    @Override
    default public String getGrappledByString() {
        return this.getWrappedGrappleable().getGrappledByString();
    }

    @Override
    default public String getGrappledByType() {
        return this.getWrappedGrappleable().getGrappledByType();
    }

    @Override
    default public boolean isGrappling() {
        return this.getWrappedGrappleable().isGrappling();
    }

    @Override
    default public boolean isGrapplingTarget(IGrappleable grapplingTarget) {
        return this.getWrappedGrappleable().isGrapplingTarget(grapplingTarget);
    }

    @Override
    default public IGrappleable getGrapplingTarget() {
        return this.getWrappedGrappleable().getGrapplingTarget();
    }

    @Override
    default public float getBearingToGrappledTarget() {
        return this.getWrappedGrappleable().getBearingToGrappledTarget();
    }

    @Override
    default public float getBearingFromGrappledTarget() {
        return this.getWrappedGrappleable().getBearingFromGrappledTarget();
    }

    @Override
    default public String getSharedGrappleType() {
        return this.getWrappedGrappleable().getSharedGrappleType();
    }

    @Override
    default public void setSharedGrappleType(String sharedGrappleType) {
        this.getWrappedGrappleable().setSharedGrappleType(sharedGrappleType);
    }

    @Override
    default public String getSharedGrappleAnimNode() {
        return this.getWrappedGrappleable().getSharedGrappleAnimNode();
    }

    @Override
    default public void setSharedGrappleAnimNode(String sharedGrappleAnimNode) {
        this.getWrappedGrappleable().setSharedGrappleAnimNode(sharedGrappleAnimNode);
    }

    @Override
    default public float getSharedGrappleAnimTime() {
        return this.getWrappedGrappleable().getSharedGrappleAnimTime();
    }

    @Override
    default public float getSharedGrappleAnimFraction() {
        return this.getWrappedGrappleable().getSharedGrappleAnimFraction();
    }

    @Override
    default public void setSharedGrappleAnimTime(float grappleAnimTime) {
        this.getWrappedGrappleable().setSharedGrappleAnimTime(grappleAnimTime);
    }

    @Override
    default public void setSharedGrappleAnimFraction(float grappleAnimFraction) {
        this.getWrappedGrappleable().setSharedGrappleAnimFraction(grappleAnimFraction);
    }

    @Override
    default public String getGrappleResult() {
        return this.getWrappedGrappleable().getGrappleResult();
    }

    @Override
    default public void setGrappleResult(String grappleResult) {
        this.getWrappedGrappleable().setGrappleResult(grappleResult);
    }

    @Override
    default public void setGrapplePosOffsetForward(float grappleOffsetForward) {
        this.getWrappedGrappleable().setGrapplePosOffsetForward(grappleOffsetForward);
    }

    @Override
    default public float getGrappleRotOffsetYaw() {
        return this.getWrappedGrappleable().getGrappleRotOffsetYaw();
    }

    @Override
    default public void setGrappleRotOffsetYaw(float grappleOffsetYaw) {
        this.getWrappedGrappleable().setGrappleRotOffsetYaw(grappleOffsetYaw);
    }

    @Override
    default public float getGrapplePosOffsetForward() {
        return this.getWrappedGrappleable().getGrapplePosOffsetForward();
    }

    @Override
    default public void setTargetAndCurrentDirection(float directionX, float directionY) {
        this.setForwardDirection(directionX, directionY);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetAndCurrentDirection(directionX, directionY);
        }
    }

    @Override
    default public Vector3f getTargetGrapplePos(Vector3f result) {
        result.set(0.0f, 0.0f, 0.0f);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(result);
        }
        return result;
    }

    @Override
    default public Vector3 getTargetGrapplePos(Vector3 result) {
        result.set(0.0f, 0.0f, 0.0f);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrapplePos(result);
        }
        return result;
    }

    @Override
    default public void setTargetGrapplePos(float x, float y, float z) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetGrapplePos(x, y, z);
        }
    }

    @Override
    default public void setTargetGrappleRotation(float x, float y) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setTargetGrappleRotation(x, y);
        }
    }

    @Override
    default public Vector2 getTargetGrappleRotation(Vector2 result) {
        result.set(1.0f, 0.0f);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getTargetGrappleRotation(result);
        }
        return result;
    }

    @Override
    default public Vector3f getGrappleOffset(Vector3f result) {
        result.set(0.0f, 0.0f, 0.0f);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(result);
        }
        return result;
    }

    @Override
    default public Vector3 getGrappleOffset(Vector3 result) {
        result.set(0.0f, 0.0f, 0.0f);
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().getGrappleOffset(result);
        }
        return result;
    }

    @Override
    default public void setGrappleDeferredOffset(float x, float y, float z) {
        IAnimatable thisAnimatable = this.getAnimatable();
        if (thisAnimatable != null && thisAnimatable.hasAnimationPlayer()) {
            thisAnimatable.getAnimationPlayer().setGrappleOffset(x, y, z);
        }
    }

    @Override
    default public boolean canBeGrappled() {
        return !this.isBeingGrappled();
    }

    @Override
    default public boolean isPerformingAnyGrappleAnimation() {
        return this.getWrappedGrappleable().isPerformingAnyGrappleAnimation();
    }

    @Override
    default public boolean isPerformingGrappleGrabAnimation() {
        return this.getWrappedGrappleable().isPerformingGrappleGrabAnimation();
    }

    @Override
    default public void setPerformingGrappleGrabAnimation(boolean grappleGrabAnim) {
        this.getWrappedGrappleable().setPerformingGrappleGrabAnimation(grappleGrabAnim);
    }

    @Override
    default public boolean isOnFloor() {
        return this.getWrappedGrappleable().isOnFloor();
    }

    @Override
    default public void setOnFloor(boolean onFloor) {
        this.getWrappedGrappleable().setOnFloor(onFloor);
    }

    @Override
    default public void resetGrappleStateToDefault(String grappleResult) {
        this.getWrappedGrappleable().resetGrappleStateToDefault(grappleResult);
    }
}

