/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import gnu.trove.set.hash.THashSet;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.RagdollBuilder;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.Bullet;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.physics.RagdollControllerDebugRenderer;
import zombie.core.physics.RagdollJoint;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.physics.RagdollStateData;
import zombie.core.physics.WorldSimulation;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.animation.BoneTransform;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.scripting.objects.PhysicsHitReactionScript;
import zombie.scripting.objects.RagdollBodyDynamics;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

public final class RagdollController
extends PooledObject {
    private static final float ActiveRagdollDistance = 1.0f;
    public static final float MovementThreshold = 0.01f;
    public static final float MovementThresholdTime = 1.5f;
    private static final float simulationTimeoutDecayFactor = 10.0f;
    public static float vehicleCollisionFriction = 0.4f;
    private static final float[] skeletonBuffer = new float[245];
    private static final float[] rigidBodyBuffer = new float[77];
    private static final float[] impulseBuffer = new float[6];
    private static final RagdollBodyDynamics vehicleRagdollBodyDynamics = new RagdollBodyDynamics();
    private static final float[] vehicleRagdollBodyDynamicsParams = new float[8];
    private boolean isInitialized;
    private int addedToWorldFrameNo = -1;
    private boolean isUpright = true;
    private boolean isOnBack;
    private final Vector3 headPosition = new Vector3();
    private final Vector3 pelvisPosition = new Vector3();
    private final Vector3 leftShoulderPosition = new Vector3();
    private final Vector3 rightShoulderPosition = new Vector3();
    private final Vector3 desiredCharacterPosition = new Vector3();
    private final Vector3 previousHeadPosition = new Vector3();
    private final Vector3 previousPelvisPosition = new Vector3();
    private boolean addedToWorld;
    private final RagdollControllerDebugRenderer.DebugDrawSettings debugDrawSettings = new RagdollControllerDebugRenderer.DebugDrawSettings();
    private int simulationState = -1;
    private IsoGameCharacter gameCharacterObject;
    private boolean wasContactingVehicle;
    private final RagdollStateData ragdollStateData = new RagdollStateData();
    private Keyframe[] keyframesForBone;
    private final Vector3f ragdollWorldPosition = new Vector3f();
    private final Vector3f ragdollWorldPositionPzBullet = new Vector3f();
    private final Quaternion ragdollWorldRotationPzBullet = new Quaternion();
    private final Quaternion ragdollLocalRotation = new Quaternion();
    private final Reusables_Vectors vectors = new Reusables_Vectors();
    private final Reusables_Quaternions quaternions = new Reusables_Quaternions();
    private static final Reusables_Quaternions squaternions = new Reusables_Quaternions();
    private static int numberOfActiveSimulations;
    private static final Pool<RagdollController> ragdollControllerPool;

    private RagdollController() {
    }

    public static RagdollController alloc() {
        return ragdollControllerPool.alloc();
    }

    public RagdollStateData getRagdollStateData() {
        return this.ragdollStateData;
    }

    public boolean isIsoPlayer() {
        return Type.tryCastTo(this.getGameCharacterObject(), IsoPlayer.class) != null;
    }

    public boolean isSimulationSleeping() {
        return this.simulationState == SimulationState.ISLAND_SLEEPING.ordinal();
    }

    public boolean isSimulationActive() {
        return this.ragdollStateData.isSimulating;
    }

    public IsoGameCharacter getGameCharacterObject() {
        return this.gameCharacterObject;
    }

    public void setGameCharacterObject(IsoGameCharacter gameCharacterObject) {
        this.gameCharacterObject = gameCharacterObject;
    }

    public int getID() {
        return this.getGameCharacterObject().getID();
    }

    public RagdollControllerDebugRenderer.DebugDrawSettings getDebugDrawSettings() {
        return this.debugDrawSettings;
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public boolean isFirstFrame() {
        return this.addedToWorldFrameNo == -1 || this.addedToWorldFrameNo >= WorldSimulation.instance.getBulletFrameNo() - 1;
    }

    public boolean isUpright() {
        return this.isUpright;
    }

    public boolean isOnBack() {
        return this.isOnBack;
    }

    public Vector3 getHeadPosition(Vector3 headPosition) {
        return headPosition.set(this.headPosition);
    }

    public Vector3 getPelvisPosition(Vector3 pelvisPosition) {
        return pelvisPosition.set(this.pelvisPosition);
    }

    public float getPelvisPositionX() {
        return this.pelvisPosition.x;
    }

    public float getPelvisPositionY() {
        return this.pelvisPosition.y;
    }

    public float getPelvisPositionZ() {
        return this.pelvisPosition.z;
    }

    public Vector3 getDesiredCharacterPosition(Vector3 desiredCharacterPosition) {
        return desiredCharacterPosition.set(this.desiredCharacterPosition);
    }

    public float getDesiredCharacterPositionX() {
        return this.desiredCharacterPosition.x;
    }

    public float getDesiredCharacterPositionY() {
        return this.desiredCharacterPosition.y;
    }

    public float getDesiredCharacterPositionZ() {
        return this.desiredCharacterPosition.z;
    }

    private boolean initialize() {
        if (this.isInitialized()) {
            return true;
        }
        DebugType.Ragdoll.debugln("Initializing...");
        if (this.getAnimationPlayer() == null || !this.getAnimationPlayer().isReady()) {
            DebugType.Ragdoll.warn("AnimationPlayer is not ready. %s", this.getGameCharacterObject());
            return false;
        }
        this.ragdollStateData.reset();
        this.ragdollStateData.isSimulating = true;
        this.addToWorld();
        this.updateRagdollSkeleton();
        this.setActive(true);
        return true;
    }

    private void reset() {
        this.removeFromWorld();
        this.ragdollStateData.reset();
        this.isUpright = true;
        this.isOnBack = false;
        this.simulationState = -1;
        this.gameCharacterObject.setRagdollFall(false);
        this.isInitialized = false;
        this.addedToWorldFrameNo = -1;
        this.gameCharacterObject.setUsePhysicHitReaction(false);
    }

    public void reinitialize() {
        this.reset();
        this.addedToWorldFrameNo = -1;
        this.isInitialized = this.initialize();
    }

    public static Vector3f pzSpaceToBulletSpace(Vector3f result) {
        float x = result.x;
        float y = result.y;
        float z = result.z;
        result.x = x;
        result.y = z * 2.44949f;
        result.z = y;
        return result;
    }

    public void setActive(boolean active) {
        this.updateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        Bullet.setRagdollActive(this.getID(), active);
    }

    public void addToWorld() {
        if (this.addedToWorld) {
            return;
        }
        ++numberOfActiveSimulations;
        DebugType.Ragdoll.debugln("Adding to world: Character:%s ID: %d", this.getGameCharacterObject(), this.getID());
        this.calculateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        Bullet.addRagdoll(this.getID(), this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        this.addedToWorld = true;
        this.addedToWorldFrameNo = WorldSimulation.instance.getBulletFrameNo();
    }

    private void removeFromWorld() {
        if (!this.addedToWorld) {
            return;
        }
        --numberOfActiveSimulations;
        Bullet.removeRagdoll(this.getID());
        this.addedToWorld = false;
    }

    public void updateRagdollSkeleton() {
        int id = this.getID();
        this.setRagdollLocalRotation();
        this.updateRagdollWorldTransform(this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet);
        this.uploadAnimationBoneTransformsToRagdoll(id);
        this.uploadAnimationBonePreviousTransformsToRagdoll(id);
    }

    private void uploadAnimationBoneTransformsToRagdoll(int id) {
        this.getBoneTransformsFromAnimation(skeletonBuffer);
        Bullet.updateRagdollSkeletonTransforms(id, this.getNumberOfBones(), skeletonBuffer);
    }

    private void uploadAnimationBonePreviousTransformsToRagdoll(int id) {
        if (!DebugOptions.instance.character.debug.ragdoll.enableInitialVelocities.getValue()) {
            return;
        }
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return;
        }
        float deltaT = PZMath.max(animationPlayer.getBoneTransformsTimeDelta(), 0.0f);
        this.getBoneTransformVelocitiesFromAnimation(skeletonBuffer, deltaT);
        Bullet.updateRagdollSkeletonPreviousTransforms(id, this.getNumberOfBones(), deltaT, skeletonBuffer);
    }

    public void update(float deltaT, Vector3f ragdollWorldPosition, Quaternion ragdollWorldRotation) {
        if (!this.isInitialized()) {
            this.addedToWorldFrameNo = -1;
            this.isInitialized = this.initialize();
            return;
        }
        DebugType.Ragdoll.trace("Simulating Ragdoll for Character:%s ID: %d", this.getGameCharacterObject(), this.getID());
        this.simulateRagdoll(this.getID(), this.ragdollWorldPosition, this.ragdollWorldPositionPzBullet, this.ragdollWorldRotationPzBullet, skeletonBuffer, rigidBodyBuffer);
        this.updateSimulationStateID();
        this.simulateHitReaction();
        if (Core.debug) {
            RagdollControllerDebugRenderer.updateDebug(this);
        }
        ragdollWorldPosition.set(this.ragdollWorldPosition);
        ragdollWorldRotation.set(this.ragdollWorldRotationPzBullet);
    }

    public void postUpdate(float deltaT) {
        RagdollStateData ragdollStateData = this.getRagdollStateData();
        this.calculateSimulationData(ragdollStateData, deltaT);
        this.updateSimulationTimeout(ragdollStateData);
    }

    private void updateSimulationTimeout(RagdollStateData ragdollStateData) {
        boolean endSimulation = false;
        if (ragdollStateData.simulationTimeout > 0.0f) {
            ragdollStateData.simulationTimeout -= GameTime.getInstance().getTimeDelta();
        }
        if (!ragdollStateData.isSimulationMovement && ragdollStateData.simulationTimeout <= 0.0f) {
            endSimulation = true;
        }
        if (this.isSimulationSleeping() && endSimulation) {
            ragdollStateData.isSimulating = false;
            this.setActive(false);
        }
    }

    private void simulateHitReaction() {
        float upwardImpulse;
        float impulse;
        BallisticsTarget ballisticsTarget = this.gameCharacterObject.getBallisticsTarget();
        if (ballisticsTarget == null || ballisticsTarget.getCombatDamageDataProcessed()) {
            return;
        }
        BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
        if (combatDamageData == null || combatDamageData.bodyPart == RagdollBodyPart.BODYPART_COUNT) {
            return;
        }
        ballisticsTarget.setCombatDamageDataProcessed(true);
        boolean dismember = false;
        RagdollJoint dismemberJoint = RagdollJoint.JOINT_COUNT;
        DebugType.Ragdoll.debugln("RagdollState: HitReaction %s", this.gameCharacterObject.getHitReaction());
        RagdollBodyPart bodyPart = combatDamageData.bodyPart;
        this.gameCharacterObject.setHitReaction("");
        if (!DebugOptions.instance.character.debug.ragdoll.physics.physicsHitReaction.getValue()) {
            return;
        }
        Vector3 direction = new Vector3();
        Vector3 targetPosition = new Vector3();
        Vector3 attackerPosition = new Vector3();
        combatDamageData.target.getPosition(targetPosition);
        combatDamageData.attacker.getPosition(attackerPosition);
        ballisticsTarget.setCombatDamageDataProcessed(true);
        if (combatDamageData.handWeapon.getAmmoType() != null) {
            impulse = PhysicsHitReactionScript.getImpulse(bodyPart, combatDamageData.handWeapon.getAmmoType());
            upwardImpulse = PhysicsHitReactionScript.getUpwardImpulse(bodyPart, combatDamageData.handWeapon.getAmmoType());
        } else if (combatDamageData.handWeapon.isExplosive()) {
            impulse = PhysicsHitReactionScript.getImpulse(bodyPart, combatDamageData.handWeapon.getPhysicsObject());
            upwardImpulse = PhysicsHitReactionScript.getUpwardImpulse(bodyPart, combatDamageData.handWeapon.getPhysicsObject());
            combatDamageData.handWeapon.getAttackTargetSquare(attackerPosition);
        } else {
            return;
        }
        targetPosition.sub(attackerPosition, direction);
        direction.normalize();
        RagdollController.impulseBuffer[0] = direction.x * impulse;
        RagdollController.impulseBuffer[1] = upwardImpulse;
        RagdollController.impulseBuffer[2] = direction.y * impulse;
        RagdollController.impulseBuffer[3] = 0.0f;
        RagdollController.impulseBuffer[4] = 0.0f;
        RagdollController.impulseBuffer[5] = 0.0f;
        Bullet.applyImpulse(this.gameCharacterObject.getID(), bodyPart.ordinal(), impulseBuffer);
        if (DebugOptions.instance.character.debug.ragdoll.physics.allowJointConstraintDetach.getValue() && false & dismemberJoint != RagdollJoint.JOINT_COUNT) {
            Bullet.detachConstraint(this.gameCharacterObject.getID(), dismemberJoint.ordinal());
            this.gameCharacterObject.getAnimationPlayer().dismember(this.getJointAssociatedBone(dismemberJoint).ordinal());
        }
    }

    private void simulateHitReaction0() {
        float upImpulse;
        float impulse;
        RagdollBodyPart bodyPart;
        if (!this.gameCharacterObject.hasHitReaction()) {
            return;
        }
        boolean dismember = false;
        RagdollJoint dismemberJoint = RagdollJoint.JOINT_COUNT;
        DebugType.Ragdoll.debugln("RagdollState: HitReaction %s", this.gameCharacterObject.getHitReaction());
        String hitReaction = this.gameCharacterObject.getHitReaction();
        RagdollSettingsManager ragdollSettingsManager = RagdollSettingsManager.getInstance();
        if (ragdollSettingsManager.isForcedHitReaction()) {
            hitReaction = ragdollSettingsManager.getForcedHitReactionLocationAsShotLocation();
        }
        switch (hitReaction) {
            case "ShotBelly": 
            case "ShotBellyStep": {
                bodyPart = RagdollBodyPart.BODYPART_PELVIS;
                break;
            }
            case "ShotChest": 
            case "ShotChestR": 
            case "ShotChestL": {
                bodyPart = RagdollBodyPart.BODYPART_SPINE;
                break;
            }
            case "ShotLegR": {
                bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
                break;
            }
            case "ShotLegL": {
                bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
                break;
            }
            case "ShotShoulderStepR": {
                bodyPart = RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
                break;
            }
            case "ShotShoulderStepL": {
                bodyPart = RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
                break;
            }
            case "ShotHeadFwd": 
            case "ShotHeadBwd": 
            case "ShotHeadFwd02": {
                bodyPart = RagdollBodyPart.BODYPART_HEAD;
                break;
            }
            default: {
                DebugType.Ragdoll.debugln("RagdollState: HitReaction %s CASE NOT DEFINED", this.gameCharacterObject.getHitReaction());
                return;
            }
        }
        this.gameCharacterObject.setHitReaction("");
        if (ragdollSettingsManager.isForcedHitReaction()) {
            boolean enabled;
            RagdollSettingsManager.HitReactionSetting hitReactionSetting = ragdollSettingsManager.getHitReactionSetting(0);
            impulse = ragdollSettingsManager.getGlobalImpulseSetting();
            upImpulse = ragdollSettingsManager.getGlobalUpImpulseSetting();
            if (!hitReactionSetting.isEnableAdmin() && (enabled = ragdollSettingsManager.getEnabledSetting(bodyPart))) {
                impulse = ragdollSettingsManager.getImpulseSetting(bodyPart);
                upImpulse = ragdollSettingsManager.getUpImpulseSetting(bodyPart);
            }
        } else {
            impulse = ragdollSettingsManager.getSandboxHitReactionImpulseStrength();
            upImpulse = ragdollSettingsManager.getSandboxHitReactionUpImpulseStrength();
        }
        Vector3 direction = new Vector3();
        BallisticsTarget ballisticsTarget = this.gameCharacterObject.getBallisticsTarget();
        if (ballisticsTarget != null) {
            Vector3 targetPosition = new Vector3();
            Vector3 attackerPosition = new Vector3();
            BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
            combatDamageData.target.getPosition(targetPosition);
            combatDamageData.attacker.getPosition(attackerPosition);
            targetPosition.sub(attackerPosition, direction);
            direction.normalize();
        }
        RagdollController.impulseBuffer[0] = direction.x * impulse;
        RagdollController.impulseBuffer[1] = direction.z * upImpulse;
        RagdollController.impulseBuffer[2] = direction.y * impulse;
        RagdollController.impulseBuffer[3] = 0.0f;
        RagdollController.impulseBuffer[4] = 0.0f;
        RagdollController.impulseBuffer[5] = 0.0f;
        Bullet.applyImpulse(this.gameCharacterObject.getID(), bodyPart.ordinal(), impulseBuffer);
        if (false & dismemberJoint != RagdollJoint.JOINT_COUNT) {
            Bullet.detachConstraint(this.gameCharacterObject.getID(), dismemberJoint.ordinal());
            this.gameCharacterObject.getAnimationPlayer().dismember(this.getJointAssociatedBone(dismemberJoint).ordinal());
        }
    }

    private SkeletonBone getJointAssociatedBone(RagdollJoint joint) {
        SkeletonBone bone = SkeletonBone.Dummy01;
        switch (joint) {
            case JOINT_PELVIS_SPINE: {
                bone = SkeletonBone.Bip01_Pelvis;
                break;
            }
            case JOINT_SPINE_HEAD: {
                bone = SkeletonBone.Bip01_Neck;
                break;
            }
            case JOINT_LEFT_HIP: {
                bone = SkeletonBone.Bip01_L_Thigh;
                break;
            }
            case JOINT_LEFT_KNEE: {
                bone = SkeletonBone.Bip01_L_Calf;
                break;
            }
            case JOINT_RIGHT_HIP: {
                bone = SkeletonBone.Bip01_R_Thigh;
                break;
            }
            case JOINT_RIGHT_KNEE: {
                bone = SkeletonBone.Bip01_R_Calf;
                break;
            }
            case JOINT_LEFT_SHOULDER: {
                bone = SkeletonBone.Bip01_L_UpperArm;
                break;
            }
            case JOINT_LEFT_ELBOW: {
                bone = SkeletonBone.Bip01_L_Forearm;
                break;
            }
            case JOINT_RIGHT_SHOULDER: {
                bone = SkeletonBone.Bip01_R_UpperArm;
                break;
            }
            case JOINT_RIGHT_ELBOW: {
                bone = SkeletonBone.Bip01_R_Forearm;
                break;
            }
        }
        return bone;
    }

    public void debugRender() {
        if (DebugOptions.instance.character.debug.ragdoll.render.pelvisLocation.getValue()) {
            RagdollControllerDebugRenderer.drawIsoDebug(this.getGameCharacterObject(), this.isOnBack, this.isUpright, this.pelvisPosition, this.desiredCharacterPosition, this.ragdollStateData);
        }
    }

    public void simulateRagdoll(int id, Vector3f ragdollWorldPosition, Vector3f ragdollWorldPositionPZBullet, Quaternion ragdollWorldRotationPZBullet, float[] skeletonBuffer, float[] rigidBodyBuffer) {
        this.updateRagdollWorldTransform(ragdollWorldPosition, ragdollWorldPositionPZBullet, ragdollWorldRotationPZBullet);
        this.setRagdollLocalRotation();
        int numberOfBones = Bullet.simulateRagdollWithRigidBodyOutput(id, skeletonBuffer, rigidBodyBuffer);
        this.setBoneTransformsToAnimation(skeletonBuffer, numberOfBones);
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        AnimationPlayer animPlayer = gameCharacterObject.getAnimationPlayer();
        this.getRagdollStateData().simulationRenderedAngle = animPlayer.getRenderedAngle();
        this.getRagdollStateData().simulationCharacterForwardAngle = gameCharacterObject.getAnimAngleRadians();
    }

    private void setRagdollLocalRotation() {
        Quaternion ragdollLocalRotation = RagdollController.getRagdollLocalRotation(this.ragdollLocalRotation);
        Bullet.setRagdollLocalTransformRotation(this.getID(), ragdollLocalRotation.x, ragdollLocalRotation.y, ragdollLocalRotation.z, ragdollLocalRotation.w);
    }

    private void updateRagdollWorldTransform(Vector3f ragdollWorldPosition, Vector3f ragdollWorldPositionPZBullet, Quaternion ragdollWorldRotationPZBullet) {
        int id = this.getID();
        this.calculateRagdollWorldTransform(ragdollWorldPosition, ragdollWorldPositionPZBullet, ragdollWorldRotationPZBullet);
        Bullet.updateRagdoll(id, ragdollWorldPositionPZBullet, ragdollWorldRotationPZBullet);
    }

    private void calculateRagdollWorldTransform(Vector3f position, Vector3f positionPZBullet, Quaternion ragdollWorldRotationPZBullet) {
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        gameCharacterObject.getPosition(position);
        positionPZBullet.set(position);
        RagdollController.pzSpaceToBulletSpace(positionPZBullet);
        float forward = gameCharacterObject.getAnimAngleRadians();
        this.calculateRagdollWorldRotation(forward, ragdollWorldRotationPZBullet);
    }

    private Quaternion calculateRagdollWorldRotation(float characterForwardAngle, Quaternion result) {
        Quaternion quatYaw = PZMath.setFromAxisAngle(0.0f, 1.0f, 0.0f, -characterForwardAngle, this.quaternions.yAxis);
        result.set(quatYaw);
        return result;
    }

    public static Quaternion getRagdollLocalRotation(Quaternion result) {
        result.setIdentity();
        PZMath.setFromAxisAngle(0.0f, 1.0f, 0.0f, (float)(-Math.PI), result);
        Quaternion quatYaw = PZMath.setFromAxisAngle(0.0f, 0.0f, 1.0f, -1.5707964f, RagdollController.squaternions.yawAdjust);
        Quaternion.mul(result, quatYaw, result);
        return result;
    }

    public void updateSimulationStateID() {
        this.simulationState = Bullet.getRagdollSimulationState(this.getID());
    }

    private void setBoneTransformsToAnimation(float[] floats, int numberOfBones) {
        AnimationClip ragdollSimulationAnimationClip = this.getRagdollSimulationAnimationClip();
        if (ragdollSimulationAnimationClip == null) {
            DebugType.Ragdoll.warn("No Ragdoll Simulation AnimationClip found,");
            return;
        }
        if (this.isFirstFrame()) {
            return;
        }
        ragdollSimulationAnimationClip.setRagdollSimulationActive(true);
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        if (skeletonHierarchy == null) {
            DebugType.Ragdoll.warn("No Skeleton found,");
            return;
        }
        Vector3f pos = HelperFunctions.allocVector3f();
        Quaternion rot = HelperFunctions.allocQuaternion();
        Vector3f scale = HelperFunctions.allocVector3f(1.0f, 1.0f, 1.0f);
        SkeletonBone[] skeletonBones = SkeletonBone.all();
        int floatArrayIndex = 0;
        for (int i = 0; i < numberOfBones; ++i) {
            SkeletonBone skeletonBone = skeletonBones[i];
            SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
            pos.x = floats[floatArrayIndex++] / 1.5f;
            pos.y = floats[floatArrayIndex++] / 1.5f;
            pos.z = floats[floatArrayIndex++] / 1.5f;
            float rx = floats[floatArrayIndex++];
            float ry = floats[floatArrayIndex++];
            float rz = floats[floatArrayIndex++];
            float rw = floats[floatArrayIndex++];
            rot.set(rx, ry, rz, rw);
            if (bone == null) continue;
            pos.x *= -1.0f;
            pos.y *= -1.0f;
            pos.z *= -1.0f;
            int boneIndex = bone.index;
            this.setBoneKeyframePRS(ragdollSimulationAnimationClip, boneIndex, pos, rot, scale);
        }
        HelperFunctions.releaseVector3f(pos);
        HelperFunctions.releaseQuaternion(rot);
        HelperFunctions.releaseVector3f(scale);
    }

    private void getBoneTransformsFromAnimation(float[] floats) {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        Vector3f position = HelperFunctions.allocVector3f();
        Quaternion rotation = HelperFunctions.allocQuaternion();
        int floatArrayIndex = 0;
        for (SkeletonBone skeletonBone : SkeletonBone.all()) {
            SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
            if (bone != null) {
                int boneIndex = bone.index;
                AnimatorsBoneTransform boneTransform = animationPlayer.getBoneTransformAt(boneIndex);
                boneTransform.getPosition(position);
                boneTransform.getRotation(rotation);
            } else {
                position.set(0.0f, 0.0f, 0.0f);
                rotation.setIdentity();
            }
            floats[floatArrayIndex++] = -position.x * 1.5f;
            floats[floatArrayIndex++] = -position.y * 1.5f;
            floats[floatArrayIndex++] = -position.z * 1.5f;
            floats[floatArrayIndex++] = rotation.x;
            floats[floatArrayIndex++] = rotation.y;
            floats[floatArrayIndex++] = rotation.z;
            floats[floatArrayIndex++] = rotation.w;
        }
        HelperFunctions.releaseVector3f(position);
        HelperFunctions.releaseQuaternion(rotation);
    }

    private void getBoneTransformVelocitiesFromAnimation(float[] floats, float deltaT) {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return;
        }
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        if (skeletonHierarchy == null) {
            return;
        }
        Vector3f previousPos = HelperFunctions.allocVector3f();
        Quaternion previousRot = HelperFunctions.allocQuaternion();
        Vector3f currentPos = HelperFunctions.allocVector3f();
        Quaternion currentRot = HelperFunctions.allocQuaternion();
        Vector3f velocityPos = HelperFunctions.allocVector3f();
        Quaternion velocityRot = HelperFunctions.allocQuaternion();
        BoneTransform previousTransform = BoneTransform.alloc();
        int floatArrayIndex = 0;
        for (SkeletonBone skeletonBone : SkeletonBone.all()) {
            SkinningBone bone = skeletonHierarchy.getBone(skeletonBone);
            if (deltaT > 0.0f && bone != null) {
                int boneIndex = bone.index;
                AnimatorsBoneTransform boneTransform = animationPlayer.getBoneTransformAt(boneIndex);
                boneTransform.getPosition(currentPos);
                boneTransform.getRotation(currentRot);
                boneTransform.getPreviousTransform(previousTransform);
                previousTransform.getPosition(previousPos);
                previousTransform.getRotation(previousRot);
                float deltaTMultiplier = 1.0f / deltaT;
                velocityPos.x = (currentPos.x - previousPos.x) * deltaTMultiplier;
                velocityPos.y = (currentPos.y - previousPos.y) * deltaTMultiplier;
                velocityPos.z = (currentPos.z - previousPos.z) * deltaTMultiplier;
                velocityRot.x = (currentRot.x - previousRot.x) * deltaTMultiplier;
                velocityRot.y = (currentRot.y - previousRot.y) * deltaTMultiplier;
                velocityRot.z = (currentRot.z - previousRot.z) * deltaTMultiplier;
                velocityRot.w = (currentRot.w - previousRot.w) * deltaTMultiplier;
            } else {
                velocityPos.set(0.0f, 0.0f, 0.0f);
                velocityRot.setIdentity();
            }
            floats[floatArrayIndex++] = -velocityPos.x * 1.5f;
            floats[floatArrayIndex++] = -velocityPos.y * 1.5f;
            floats[floatArrayIndex++] = -velocityPos.z * 1.5f;
            floats[floatArrayIndex++] = velocityRot.x;
            floats[floatArrayIndex++] = velocityRot.y;
            floats[floatArrayIndex++] = velocityRot.z;
            floats[floatArrayIndex++] = velocityRot.w;
        }
        HelperFunctions.releaseVector3f(previousPos);
        HelperFunctions.releaseQuaternion(previousRot);
        HelperFunctions.releaseVector3f(currentPos);
        HelperFunctions.releaseQuaternion(currentRot);
        HelperFunctions.releaseVector3f(velocityPos);
        HelperFunctions.releaseQuaternion(velocityRot);
        previousTransform.release();
    }

    private Keyframe[] getKeyframesForBone(int boneIndex) {
        AnimationClip ragdollSimulationAnimationClip = this.getRagdollSimulationAnimationClip();
        if (ragdollSimulationAnimationClip == null) {
            DebugType.Ragdoll.warn("No Ragdoll Simulation AnimationClip found,");
            return null;
        }
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        if (skeletonHierarchy == null) {
            DebugType.Ragdoll.warn("No Skeleton found,");
            return null;
        }
        return this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
    }

    private Keyframe[] getKeyframesForBone(AnimationClip ragdollSimulationAnimationClip, int boneIndex) {
        this.keyframesForBone = ragdollSimulationAnimationClip.getKeyframesForBone(boneIndex, this.keyframesForBone);
        assert (this.keyframesForBone.length == 2);
        assert (this.keyframesForBone[1].none == boneIndex);
        return this.keyframesForBone;
    }

    private Keyframe getKeyframeForBone(AnimationClip ragdollSimulationAnimationClip, int boneIndex) {
        Keyframe[] keyframesForBone = this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
        return keyframesForBone[1];
    }

    private void setBoneKeyframePRS(AnimationClip ragdollSimulationAnimationClip, int boneIndex, Vector3f pos, Quaternion rot, Vector3f scale) {
        Keyframe[] keyframesForBone = this.getKeyframesForBone(ragdollSimulationAnimationClip, boneIndex);
        Keyframe prevKeyframe = keyframesForBone[0];
        Keyframe keyframe = keyframesForBone[1];
        assert (prevKeyframe.none == boneIndex && keyframe.none == boneIndex);
        prevKeyframe.set(keyframe.position, keyframe.rotation, keyframe.scale);
        keyframe.set(pos, rot, scale);
    }

    private AnimationClip getRagdollSimulationAnimationClip() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return null;
        }
        return animationPlayer.getRagdollSimulationAnimationClip();
    }

    private SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return null;
        }
        return animationPlayer.getSkeletonBoneHierarchy();
    }

    @Override
    public void onReleased() {
        this.reset();
    }

    public int getNumberOfBones() {
        return SkeletonBone.count();
    }

    public AnimationPlayer getAnimationPlayer() {
        IsoGameCharacter character = this.getGameCharacterObject();
        if (character == null) {
            return null;
        }
        return character.getAnimationPlayer();
    }

    public boolean isSimulationDirectionCalculated() {
        return this.ragdollStateData.isCalculated;
    }

    public Vector2 getCalculatedSimulationDirection(Vector2 result) {
        result.set(this.ragdollStateData.simulationDirection);
        return result;
    }

    public float getCalculatedSimulationDirectionAngle() {
        return this.ragdollStateData.simulationDirection.getDirection();
    }

    public float getSimulationRenderedAngle() {
        return this.ragdollStateData.simulationRenderedAngle;
    }

    public float getSimulationCharacterForwardAngle() {
        return this.ragdollStateData.simulationCharacterForwardAngle;
    }

    private void calculateSimulationData(RagdollStateData ragdollStateData, float deltaT) {
        if (this.isFirstFrame()) {
            return;
        }
        this.previousHeadPosition.set(this.headPosition);
        this.previousPelvisPosition.set(this.pelvisPosition);
        IsoGameCharacter gameCharacterObject = this.getGameCharacterObject();
        Model.boneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.headBone, this.headPosition);
        Model.boneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.pelvisBone, this.pelvisPosition);
        if (!ragdollStateData.isCalculated) {
            this.previousHeadPosition.set(this.headPosition);
            this.previousPelvisPosition.set(this.pelvisPosition);
            gameCharacterObject.getPosition(this.desiredCharacterPosition);
        }
        this.desiredCharacterPosition.set(this.desiredCharacterPosition.x + (this.pelvisPosition.x - this.previousPelvisPosition.x), this.desiredCharacterPosition.y + (this.pelvisPosition.y - this.previousPelvisPosition.y), PZMath.roundFloat(this.desiredCharacterPosition.z + (this.pelvisPosition.z - this.previousPelvisPosition.z), 1));
        ragdollStateData.isSimulationMovement = false;
        float headDistance = this.previousHeadPosition.distanceTo(this.headPosition);
        float pelvisDistance = this.previousPelvisPosition.distanceTo(this.pelvisPosition);
        if (ragdollStateData.isContactingVehicle) {
            ragdollStateData.simulationTimeout = 1.5f;
            ragdollStateData.isContactingVehicle = false;
        } else if (headDistance > 0.01f || pelvisDistance > 0.01f) {
            if (ragdollStateData.simulationTimeout < 1.5f) {
                ragdollStateData.simulationTimeout = 1.5f;
            }
            ragdollStateData.isSimulationMovement = true;
        } else if (this.isSimulationSleeping()) {
            ragdollStateData.simulationTimeout -= deltaT * 10.0f;
        }
        Vector3 groundPosition = new Vector3();
        Model.boneToWorldCoords(gameCharacterObject, 0, groundPosition);
        this.isUpright = this.headPosition.z > groundPosition.z + 0.6f - 0.2f;
        gameCharacterObject.setOnFloor(!this.isUpright);
        Model.boneZDirectionToWorldCoords(gameCharacterObject, RagdollBuilder.instance.pelvisBone, ragdollStateData.pelvisDirection, 0.5f);
        Model.boneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.leftShoulder, this.leftShoulderPosition);
        Model.boneToWorldCoords(gameCharacterObject, RagdollBuilder.instance.rightShoulder, this.rightShoulderPosition);
        Vector3 shoulderCenter = PZMath.lerp(this.vectors.center, this.leftShoulderPosition, this.rightShoulderPosition, 0.5f);
        boolean bl = this.isOnBack = !gameCharacterObject.isFallOnFront();
        if (!this.isUpright && gameCharacterObject.isFullyRagdolling()) {
            Vector3 shoulderRight = Vector3.sub(this.rightShoulderPosition, shoulderCenter, this.vectors.right);
            shoulderRight.z = 0.0f;
            Vector3 pelvisToShoulders = Vector3.sub(shoulderCenter, this.pelvisPosition, this.vectors.up);
            pelvisToShoulders.z = 0.0f;
            Vector3 shoulderForward = PZMath.cross(shoulderRight, pelvisToShoulders, this.vectors.forward);
            shoulderForward.normalize();
            if (PZMath.abs(shoulderForward.z) > 0.2f) {
                boolean bl2 = this.isOnBack = shoulderForward.z > 0.0f;
            }
        }
        if (DebugOptions.instance.character.debug.ragdoll.showIsOnBackOutlines.getValue()) {
            if (this.isOnBack) {
                gameCharacterObject.setOutlineHighlightCol(0.0f, 1.0f, 0.0f, 1.0f);
                gameCharacterObject.setOutlineHighlight(true);
            } else {
                gameCharacterObject.setOutlineHighlightCol(0.0f, 0.0f, 1.0f, 1.0f);
                gameCharacterObject.setOutlineHighlight(true);
            }
        }
        if (this.isOnBack) {
            ragdollStateData.simulationDirection.x = this.pelvisPosition.x - shoulderCenter.x;
            ragdollStateData.simulationDirection.y = this.pelvisPosition.y - shoulderCenter.y;
        } else {
            ragdollStateData.simulationDirection.x = shoulderCenter.x - this.pelvisPosition.x;
            ragdollStateData.simulationDirection.y = shoulderCenter.y - this.pelvisPosition.y;
        }
        ragdollStateData.isCalculated = true;
        ragdollStateData.simulationDirection.normalize();
        gameCharacterObject.setFallOnFront(!this.isOnBack);
        gameCharacterObject.setRagdollFall(true);
    }

    public static int getNumberOfActiveSimulations() {
        return numberOfActiveSimulations;
    }

    public static boolean checkForActiveRagdoll(IsoGridSquare isoGridSquare) {
        THashSet<IPooledObject> activeRagdollControllers = ragdollControllerPool.getPoolStacks().get().getInUse();
        for (IPooledObject iPooledObject : activeRagdollControllers) {
            RagdollController ragdollController;
            float distance;
            if (!(iPooledObject instanceof RagdollController) || !((distance = isoGridSquare.DistToProper((ragdollController = (RagdollController)iPooledObject).getGameCharacterObject())) < 1.0f)) continue;
            return true;
        }
        return false;
    }

    public void vehicleCollision(IsoZombie isoZombie, BaseVehicle collidedVehicle) {
        boolean isCurrentlyContacting = false;
        if (collidedVehicle != null) {
            this.ragdollStateData.lastCollidedVehicle = collidedVehicle;
        }
        if (this.ragdollStateData.lastCollidedVehicle != null) {
            this.ragdollStateData.isContactingVehicle = this.ragdollStateData.lastCollidedVehicle.isCollided(this.gameCharacterObject);
            isCurrentlyContacting = this.ragdollStateData.lastCollidedVehicle.testTouchingVehicle(isoZombie, this);
        }
        if (this.wasContactingVehicle && !isCurrentlyContacting) {
            DebugType.Physics.println("ResetVehicleBodyDynamics");
            this.resetVehicleRagdollBodyDynamics();
        } else if (!this.wasContactingVehicle && isCurrentlyContacting) {
            Bullet.setRagdollBodyDynamics(this.getID(), vehicleRagdollBodyDynamicsParams);
        }
        this.wasContactingVehicle = isCurrentlyContacting;
    }

    public static void setVehicleRagdollBodyDynamics(RagdollBodyDynamics ragdollBodyDynamics) {
        RagdollController.vehicleRagdollBodyDynamics.linearDamping = ragdollBodyDynamics.defaultLinearDamping;
        RagdollController.vehicleRagdollBodyDynamics.angularDamping = ragdollBodyDynamics.defaultAngularDamping;
        RagdollController.vehicleRagdollBodyDynamics.deactivationTime = ragdollBodyDynamics.defaultDeactivationTime;
        RagdollController.vehicleRagdollBodyDynamics.linearSleepingThreshold = ragdollBodyDynamics.defaultLinearSleepingThreshold;
        RagdollController.vehicleRagdollBodyDynamics.angularSleepingThreshold = ragdollBodyDynamics.defaultAngularSleepingThreshold;
        RagdollController.vehicleRagdollBodyDynamics.friction = vehicleCollisionFriction;
        RagdollController.vehicleRagdollBodyDynamics.rollingFriction = ragdollBodyDynamics.defaultRollingFriction;
        int arrayIndex = 0;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollBodyPart.BODYPART_COUNT.ordinal();
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.linearDamping;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.angularDamping;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.deactivationTime;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.linearSleepingThreshold;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.angularSleepingThreshold;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.friction;
        RagdollController.vehicleRagdollBodyDynamicsParams[arrayIndex++] = RagdollController.vehicleRagdollBodyDynamics.rollingFriction;
    }

    private void resetVehicleRagdollBodyDynamics() {
        Bullet.resetRagdollBodyDynamics(this.getID());
    }

    static {
        ragdollControllerPool = new Pool<RagdollController>(RagdollController::new);
    }

    private static class Reusables_Vectors {
        private final Vector3 center = new Vector3();
        private final Vector3 forward = new Vector3();
        private final Vector3 left = new Vector3();
        private final Vector3 right = new Vector3();
        private final Vector3 up = new Vector3();

        private Reusables_Vectors() {
        }
    }

    private static class Reusables_Quaternions {
        private final Quaternion xAxis = new Quaternion();
        private final Quaternion yAxis = new Quaternion();
        private final Quaternion zAxis = new Quaternion();
        private final Quaternion yawAdjust = new Quaternion();

        private Reusables_Quaternions() {
        }
    }

    public static enum SimulationState {
        UNKNOWN,
        ACTIVE_TAG,
        ISLAND_SLEEPING,
        WANTS_DEACTIVATION,
        DISABLE_DEACTIVATION,
        DISABLE_SIMULATION;

    }
}

