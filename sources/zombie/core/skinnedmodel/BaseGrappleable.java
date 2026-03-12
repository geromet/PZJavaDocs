/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel;

import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableCallbackMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;

public class BaseGrappleable
implements IGrappleable {
    private IsoGameCharacter character;
    private IsoDeadBody deadBody;
    private IsoMovingObject isoMovingObject;
    private IGrappleable parentGrappleable;
    private boolean doGrapple;
    private boolean isPickingUpBody;
    private boolean isPuttingDownBody;
    private boolean doContinueGrapple;
    private boolean beingGrappled;
    private IGrappleable grappledBy;
    private boolean isGrappling;
    private IGrappleable grapplingTarget;
    private String sharedGrappleType = "";
    private String sharedGrappleAnimNode = "";
    private float sharedGrappleTime;
    private float sharedGrappleFraction;
    private String grappleResult = "";
    private float grappleOffsetForward;
    private float grappleOffsetYaw;
    private GrappleOffsetBehaviour grappleOffsetBehaviour = GrappleOffsetBehaviour.NONE;
    private boolean isPerformingGrappleGrabAnim;
    private Invokers.Params0.ICallback onGrappleBeginCallback;
    private Invokers.Params0.ICallback onGrappleEndCallback;

    public BaseGrappleable() {
    }

    public BaseGrappleable(IsoGameCharacter character) {
        this.character = character;
        this.isoMovingObject = this.character;
        this.parentGrappleable = this.character;
    }

    public BaseGrappleable(IsoDeadBody deadBody) {
        this.deadBody = deadBody;
        this.isoMovingObject = this.deadBody;
        this.parentGrappleable = this.deadBody;
    }

    @Override
    public IAnimatable getAnimatable() {
        return this.parentGrappleable.getAnimatable();
    }

    @Override
    public void Grappled(IGrappleable grappler, HandWeapon weapon, float grappleEffectiveness, String grappleType) {
        if (grappler == null) {
            DebugLog.Grapple.warn("Grappler is null. Nothing to grapple us.");
            return;
        }
        if (grappleEffectiveness < 0.5f) {
            DebugLog.Grapple.debugln("Effectiveness insufficient. %f. Rejecting grapple.", Float.valueOf(grappleEffectiveness));
            grappler.RejectGrapple(this.getParentGrappleable());
            return;
        }
        if (!this.canBeGrappled()) {
            DebugLog.Grapple.debugln("No transition available to grappled state.");
            grappler.RejectGrapple(this.getParentGrappleable());
            return;
        }
        this.beingGrappled = true;
        this.grappledBy = grappler;
        this.sharedGrappleType = grappleType;
        this.sharedGrappleAnimNode = "";
        this.sharedGrappleTime = 0.0f;
        this.sharedGrappleFraction = 0.0f;
        DebugLog.Grapple.debugln("Accepting grapple by: %s", this.getGrappledByString(), this.getGrappledBy().getClass().getName());
        grappler.AcceptGrapple(this.getParentGrappleable(), grappleType);
        this.invokeOnGrappleBeginEvent();
    }

    @Override
    public void RejectGrapple(IGrappleable grappleRejector) {
        if (this.isGrappling() && !this.isGrapplingTarget(grappleRejector)) {
            DebugLog.Grapple.warn("Target is not being grappled.");
            return;
        }
        DebugLog.Grapple.debugln("Grapple rejected.");
        this.resetGrappleStateToDefault("Rejected");
    }

    @Override
    public void AcceptGrapple(IGrappleable grappleAcceptor, String grappleType) {
        this.setGrapplingTarget(grappleAcceptor, grappleType);
        if (this.character.isLocal()) {
            this.character.setVariable("bearingFromGrappledTarget", this.getBearingFromGrappledTarget());
        }
        DebugLog.Grapple.debugln("Grapple accepted. Grappled target: %s", this.getGrapplingTarget().getClass().getName());
        this.invokeOnGrappleBeginEvent();
    }

    @Override
    public void LetGoOfGrappled(String grappleResult) {
        if (!this.isGrappling()) {
            DebugLog.Grapple.warn("Not currently grappling.");
            return;
        }
        IGrappleable grappledCharacterToLetGo = this.getGrapplingTarget();
        this.resetGrappleStateToDefault(grappleResult);
        if (grappledCharacterToLetGo == null) {
            DebugLog.Grapple.warn("Nothing is being grappled. Nothing to let go of.");
            return;
        }
        DebugLog.Grapple.debugln("Letting go of grappled. Result: %s", grappleResult);
        grappledCharacterToLetGo.GrapplerLetGo(this.getParentGrappleable(), grappleResult);
        this.invokeOnGrappleEndEvent();
    }

    @Override
    public void GrapplerLetGo(IGrappleable grappler, String grappleResult) {
        if (!this.isBeingGrappled()) {
            DebugLog.Grapple.warn("GrapplerLetGo> Not currently being grappled,.");
            return;
        }
        if (!this.isBeingGrappledBy(grappler)) {
            DebugLog.Grapple.warn("GrapplerLetGo> Not being grappled by this character.");
            return;
        }
        DebugLog.Grapple.debugln("Grappler has let us go. Result: %s.", grappleResult);
        this.resetGrappleStateToDefault(grappleResult);
        this.invokeOnGrappleEndEvent();
    }

    private void resetGrappleStateToDefault() {
        this.resetGrappleStateToDefault("");
    }

    @Override
    public void resetGrappleStateToDefault(String grappleResult) {
        this.doGrapple = false;
        this.isPickingUpBody = false;
        this.isPuttingDownBody = false;
        this.doContinueGrapple = false;
        this.isGrappling = false;
        this.beingGrappled = false;
        this.grapplingTarget = null;
        this.grappleResult = grappleResult;
        this.sharedGrappleType = "";
        this.sharedGrappleAnimNode = "";
        this.sharedGrappleTime = 0.0f;
        this.sharedGrappleFraction = 0.0f;
        this.grappleOffsetForward = 0.0f;
        this.grappleOffsetBehaviour = GrappleOffsetBehaviour.NONE;
        this.setGrappleDeferredOffset(0.0f, 0.0f, 0.0f);
    }

    @Override
    public boolean isBeingGrappled() {
        return this.beingGrappled;
    }

    @Override
    public boolean isBeingGrappledBy(IGrappleable grappledBy) {
        return this.isBeingGrappled() && this.getGrappledBy() == grappledBy;
    }

    @Override
    public Vector2 getAnimForwardDirection(Vector2 forwardDirection) {
        forwardDirection.set(1.0f, 0.0f);
        return forwardDirection;
    }

    @Override
    public org.joml.Vector3f getTargetGrapplePos(org.joml.Vector3f result) {
        result.set(0.0f, 0.0f, 0.0f);
        return result;
    }

    @Override
    public Vector3 getTargetGrapplePos(Vector3 result) {
        result.set(0.0f, 0.0f, 0.0f);
        return result;
    }

    @Override
    public void setTargetGrapplePos(org.joml.Vector3f grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(grapplePos);
    }

    @Override
    public void setTargetGrapplePos(Vector3 grapplePos) {
        this.getParentGrappleable().setTargetGrapplePos(grapplePos);
    }

    @Override
    public Vector2 getTargetGrappleRotation(Vector2 result) {
        return this.getParentGrappleable().getTargetGrappleRotation(result);
    }

    @Override
    public void setTargetGrappleRotation(float x, float y) {
        this.getParentGrappleable().setTargetGrappleRotation(x, y);
    }

    @Override
    public void setTargetGrapplePos(float x, float y, float z) {
        this.getParentGrappleable().setTargetGrapplePos(x, y, z);
    }

    @Override
    public void setGrappleDeferredOffset(float x, float y, float z) {
        this.getParentGrappleable().setGrappleDeferredOffset(x, y, z);
    }

    @Override
    public org.joml.Vector3f getGrappleOffset(org.joml.Vector3f result) {
        return this.getParentGrappleable().getGrappleOffset(result);
    }

    @Override
    public Vector3 getGrappleOffset(Vector3 result) {
        return this.getParentGrappleable().getGrappleOffset(result);
    }

    @Override
    public void setForwardDirection(float directionX, float directionY) {
        this.getParentGrappleable().setForwardDirection(directionX, directionY);
    }

    @Override
    public void setTargetAndCurrentDirection(float directionX, float directionY) {
        this.getParentGrappleable().setTargetAndCurrentDirection(directionX, directionY);
    }

    @Override
    public Vector3 getPosition(Vector3 position) {
        return this.getParentGrappleable().getPosition(position);
    }

    @Override
    public Vector3f getPosition(Vector3f position) {
        return this.getParentGrappleable().getPosition(position);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        this.getParentGrappleable().setPosition(x, y, z);
    }

    @Override
    public IGrappleable getGrappledBy() {
        if (this.isBeingGrappled()) {
            return this.grappledBy;
        }
        return null;
    }

    @Override
    public String getGrappledByString() {
        if (this.isBeingGrappled()) {
            return this.grappledBy != null ? this.grappledBy.getClass().getName() + "_" + this.grappledBy.getID() : "null";
        }
        return "";
    }

    @Override
    public String getGrappledByType() {
        if (this.isBeingGrappled()) {
            return this.grappledBy != null ? this.grappledBy.getClass().getName() : "null";
        }
        return "None";
    }

    @Override
    public boolean isGrappling() {
        return this.isGrappling;
    }

    @Override
    public boolean isGrapplingTarget(IGrappleable grapplingTarget) {
        return this.getGrapplingTarget() == grapplingTarget;
    }

    @Override
    public IGrappleable getGrapplingTarget() {
        if (!this.isGrappling()) {
            return null;
        }
        return this.grapplingTarget;
    }

    private void setGrapplingTarget(IGrappleable grapplingTarget, String grappleType) {
        this.resetGrappleStateToDefault();
        this.isGrappling = true;
        this.doContinueGrapple = true;
        this.grapplingTarget = grapplingTarget;
        this.sharedGrappleType = grappleType;
    }

    @Override
    public float getBearingToGrappledTarget() {
        IGrappleable grappledTarget = this.getGrapplingTarget();
        if (grappledTarget == null) {
            return 0.0f;
        }
        return PZMath.calculateBearing(this.getPosition(new Vector3()), this.getAnimForwardDirection(new Vector2()), grappledTarget.getPosition(new Vector3()));
    }

    @Override
    public float getBearingFromGrappledTarget() {
        IGrappleable grappledTarget = this.getGrapplingTarget();
        if (grappledTarget == null) {
            return 0.0f;
        }
        return PZMath.calculateBearing(grappledTarget.getPosition(new Vector3()), grappledTarget.getAnimForwardDirection(new Vector2()), this.getPosition(new Vector3()));
    }

    @Override
    public String getSharedGrappleType() {
        return this.sharedGrappleType;
    }

    @Override
    public void setSharedGrappleType(String sharedGrappleType) {
        IGrappleable grappledBy;
        if (StringUtils.equals(this.sharedGrappleType, sharedGrappleType)) {
            return;
        }
        this.sharedGrappleType = sharedGrappleType;
        IGrappleable grapplingTarget = this.getGrapplingTarget();
        if (grapplingTarget != null) {
            grapplingTarget.setSharedGrappleType(this.sharedGrappleType);
        }
        if ((grappledBy = this.getGrappledBy()) != null) {
            grappledBy.setSharedGrappleType(this.sharedGrappleType);
        }
        this.isPickingUpBody = false;
    }

    @Override
    public String getSharedGrappleAnimNode() {
        return this.sharedGrappleAnimNode;
    }

    @Override
    public void setSharedGrappleAnimNode(String sharedGrappleAnimNode) {
        this.sharedGrappleAnimNode = sharedGrappleAnimNode;
    }

    @Override
    public float getSharedGrappleAnimTime() {
        return this.sharedGrappleTime;
    }

    @Override
    public float getSharedGrappleAnimFraction() {
        return this.sharedGrappleFraction;
    }

    @Override
    public void setSharedGrappleAnimTime(float grappleAnimTime) {
        this.sharedGrappleTime = grappleAnimTime;
    }

    @Override
    public void setSharedGrappleAnimFraction(float grappleAnimFraction) {
        this.sharedGrappleFraction = grappleAnimFraction;
    }

    @Override
    public String getGrappleResult() {
        return this.grappleResult;
    }

    @Override
    public void setGrappleResult(String grappleResult) {
        this.grappleResult = grappleResult;
    }

    public IGrappleable getParentGrappleable() {
        return this.parentGrappleable;
    }

    @Override
    public boolean canBeGrappled() {
        IGrappleable parentGrappleable = this.getParentGrappleable();
        return parentGrappleable != null && parentGrappleable.canBeGrappled();
    }

    @Override
    public void setGrapplePosOffsetForward(float grappleOffsetForward) {
        this.grappleOffsetForward = grappleOffsetForward;
    }

    @Override
    public float getGrapplePosOffsetForward() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrapplePosOffsetForward();
        }
        if (this.isGrappling()) {
            return this.grappleOffsetForward;
        }
        return 0.0f;
    }

    @Override
    public void setGrappleRotOffsetYaw(float grappleOffsetYaw) {
        this.grappleOffsetYaw = grappleOffsetYaw;
    }

    @Override
    public float getGrappleRotOffsetYaw() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrappleRotOffsetYaw();
        }
        if (this.isGrappling()) {
            return this.grappleOffsetYaw;
        }
        return 0.0f;
    }

    @Override
    public GrappleOffsetBehaviour getGrappleOffsetBehaviour() {
        if (this.isBeingGrappled()) {
            return this.getGrappledBy().getGrappleOffsetBehaviour();
        }
        if (this.isGrappling()) {
            return this.grappleOffsetBehaviour;
        }
        return GrappleOffsetBehaviour.NONE;
    }

    @Override
    public void setGrappleoffsetBehaviour(GrappleOffsetBehaviour newBehaviour) {
        this.grappleOffsetBehaviour = newBehaviour;
    }

    @Override
    public boolean isDoGrapple() {
        return this.doGrapple || this.isPerformingGrappleGrabAnimation();
    }

    public boolean isPickingUpBody() {
        return this.isPickingUpBody;
    }

    public boolean isPuttingDownBody() {
        return this.isPuttingDownBody;
    }

    @Override
    public void setDoGrapple(boolean doGrapple) {
        this.doGrapple = doGrapple;
    }

    @Override
    public boolean isDoContinueGrapple() {
        return this.doContinueGrapple;
    }

    @Override
    public void setDoContinueGrapple(boolean doContinueGrapple) {
        this.doContinueGrapple = doContinueGrapple;
        if (!doContinueGrapple) {
            this.isPuttingDownBody = true;
        }
    }

    @Override
    public boolean isPerformingAnyGrappleAnimation() {
        return this.isPerformingGrappleGrabAnimation() || this.isPerformingGrappleAnimation();
    }

    @Override
    public boolean isPerformingGrappleGrabAnimation() {
        return this.isPerformingGrappleGrabAnim;
    }

    @Override
    public void setPerformingGrappleGrabAnimation(boolean grappleGrabAnim) {
        this.isPerformingGrappleGrabAnim = grappleGrabAnim;
    }

    @Override
    public boolean isPerformingGrappleAnimation() {
        return this.getParentGrappleable().isPerformingGrappleAnimation();
    }

    @Override
    public boolean isOnFloor() {
        return this.isoMovingObject != null && this.isoMovingObject.isOnFloor();
    }

    @Override
    public void setOnFloor(boolean onFloor) {
        if (this.isoMovingObject != null) {
            this.isoMovingObject.setOnFloor(onFloor);
        }
    }

    @Override
    public boolean isFallOnFront() {
        return this.character != null && this.character.isFallOnFront() || this.deadBody != null && this.deadBody.isFallOnFront();
    }

    @Override
    public void setFallOnFront(boolean fallOnFront) {
        if (this.character != null) {
            this.character.setFallOnFront(fallOnFront);
        }
        if (this.deadBody != null) {
            this.deadBody.setFallOnFront(fallOnFront);
        }
    }

    @Override
    public boolean isKilledByFall() {
        return this.character != null && this.character.isKilledByFall() || this.deadBody != null && this.deadBody.isKilledByFall();
    }

    @Override
    public void setKilledByFall(boolean killedByFall) {
        if (this.character != null) {
            this.character.setKilledByFall(killedByFall);
        }
        if (this.deadBody != null) {
            this.deadBody.setKilledByFall(killedByFall);
        }
    }

    public void setOnGrappledBeginCallback(Invokers.Params0.ICallback onGrappleBegin) {
        this.onGrappleBeginCallback = onGrappleBegin;
    }

    private void invokeOnGrappleBeginEvent() {
        this.isPickingUpBody = true;
        if (this.onGrappleBeginCallback != null) {
            this.onGrappleBeginCallback.accept();
        }
    }

    public void setOnGrappledEndCallback(Invokers.Params0.ICallback onGrappleBegin) {
        this.onGrappleEndCallback = onGrappleBegin;
    }

    private void invokeOnGrappleEndEvent() {
        this.isPuttingDownBody = false;
        if (this.onGrappleEndCallback != null) {
            this.onGrappleEndCallback.accept();
        }
    }

    public static void RegisterGrappleVariables(IAnimationVariableCallbackMap variableMap, IGrappleable grappleable) {
        variableMap.setVariable("bDoGrapple", grappleable::isDoGrapple, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bDoContinueGrapple", grappleable::isDoContinueGrapple, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bIsGrappling", grappleable::isGrappling, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappleResult", grappleable::getGrappleResult, grappleable::setGrappleResult, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleType", grappleable::getSharedGrappleType, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleAnimNode", grappleable::getSharedGrappleAnimNode, grappleable::setSharedGrappleAnimNode, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleTime", grappleable::getSharedGrappleAnimTime, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("sharedGrappleFraction", grappleable::getSharedGrappleAnimFraction, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappleOffsetForward", grappleable::getGrapplePosOffsetForward, grappleable::setGrapplePosOffsetForward, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappleOffsetBehaviour", GrappleOffsetBehaviour.class, grappleable::getGrappleOffsetBehaviour, grappleable::setGrappleoffsetBehaviour, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bearingToGrappledTarget", grappleable::getBearingToGrappledTarget, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("bBeingGrappled", grappleable::isBeingGrappled, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappledBy", grappleable::getGrappledByString, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("grappledByType", grappleable::getGrappledByType, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("GrappleGrabAnim", grappleable::isPerformingGrappleGrabAnimation, grappleable::setPerformingGrappleGrabAnimation, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("GrappleAnim", grappleable::isPerformingGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
        variableMap.setVariable("AnyGrappleAnim", grappleable::isPerformingAnyGrappleAnimation, IAnimationVariableSlotDescriptor.Null);
    }
}

