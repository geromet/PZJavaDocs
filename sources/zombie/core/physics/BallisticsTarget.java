/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.physics.RagdollController;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimatorsBoneTransform;
import zombie.core.skinnedmodel.model.SkeletonBone;
import zombie.core.skinnedmodel.model.SkinningBone;
import zombie.core.skinnedmodel.model.SkinningBoneHierarchy;
import zombie.debug.DebugOptions;
import zombie.inventory.types.HandWeapon;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class BallisticsTarget
extends PooledObject {
    public static float[] boneTransformData = new float[245];
    private static final Pool<IPooledObject> ballisticsTargetPool = new Pool<IPooledObject>(BallisticsTarget::new);
    private boolean isInitialized;
    private boolean addedToWorld;
    private int numberOfBones = -1;
    private IsoGameCharacter isoGameCharacter;
    private final CombatDamageData combatDamageData = new CombatDamageData();
    private boolean combatDamageDataProcessed = true;
    private int releaseFrame;

    private BallisticsTarget() {
    }

    public static Pool<IPooledObject> getBallisticsTargetPool() {
        return ballisticsTargetPool;
    }

    public static BallisticsTarget alloc(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter == null) {
            return null;
        }
        BallisticsTarget ballisticsTarget = (BallisticsTarget)ballisticsTargetPool.alloc();
        ballisticsTarget.isoGameCharacter = isoGameCharacter;
        return ballisticsTarget;
    }

    public int getID() {
        if (this.isoGameCharacter == null) {
            return -1;
        }
        return this.isoGameCharacter.getID();
    }

    public void setIsoGameCharacter(IsoGameCharacter isoGameCharacter) {
        this.isoGameCharacter = isoGameCharacter;
    }

    public boolean isValidIsoGameCharacter() {
        return this.isoGameCharacter != null;
    }

    private boolean initialize() {
        if (this.isInitialized) {
            return true;
        }
        if (this.isoGameCharacter.getAnimationPlayer() == null || this.isoGameCharacter.getAnimationPlayer().boneTransforms == null || this.isoGameCharacter.getAnimationPlayer().boneTransforms[0] == null) {
            return false;
        }
        this.numberOfBones = this.isoGameCharacter.getAnimationPlayer().boneTransforms.length;
        if (boneTransformData.length < this.numberOfBones * 7) {
            boneTransformData = new float[this.numberOfBones * 7];
        }
        return true;
    }

    public boolean update() {
        if (!this.isInitialized) {
            this.isInitialized = this.initialize();
            return false;
        }
        if (this.combatDamageDataProcessed) {
            if (this.releaseFrame > 0) {
                --this.releaseFrame;
            }
            if (this.releaseFrame == 0) {
                return true;
            }
        }
        if (GameServer.server) {
            return false;
        }
        this.addToWorld();
        float forward = this.isoGameCharacter.getDirectionAngle();
        Bullet.setBallisticsTargetAxis(this.getID(), -1.5707964f, -1.5707964f + this.isoGameCharacter.getAnimationPlayer().getAngle(), (float)Math.PI);
        float x = this.isoGameCharacter.getX();
        float y = this.isoGameCharacter.getY();
        float z = this.isoGameCharacter.getZ();
        Bullet.updateBallisticsTarget(this.isoGameCharacter.getID(), x, z * 2.44949f, y, 0.0f, (float)Math.sin(forward * 0.5f), 0.0f, (float)Math.cos(forward * 0.5f), false);
        this.updateSkeleton();
        return false;
    }

    public void add() {
        this.releaseFrame = 2;
        if (!this.addToWorld()) {
            return;
        }
        this.initialize();
        float forward = this.isoGameCharacter.getDirectionAngle();
        Bullet.setBallisticsTargetAxis(this.getID(), -1.5707964f, -1.5707964f + this.isoGameCharacter.getAnimationPlayer().getAngle(), (float)Math.PI);
        float x = this.isoGameCharacter.getX();
        float y = this.isoGameCharacter.getY();
        float z = this.isoGameCharacter.getZ();
        RagdollController ragdollController = this.isoGameCharacter.getRagdollController();
        if (ragdollController != null) {
            x = ragdollController.getPelvisPositionX();
            y = ragdollController.getPelvisPositionY();
            z = ragdollController.getPelvisPositionZ();
        }
        int id = this.isoGameCharacter.getID();
        Bullet.updateBallisticsTarget(id, x, z * 2.44949f, y, 0.0f, (float)Math.sin(forward * 0.5f), 0.0f, (float)Math.cos(forward * 0.5f), false);
        this.updateSkeleton();
    }

    private void getBoneTransforms() {
        AnimationPlayer animationPlayer = this.isoGameCharacter.getAnimationPlayer();
        SkinningBoneHierarchy skeletonHierarchy = this.getSkeletonBoneHierarchy();
        Vector3f position = HelperFunctions.allocVector3f();
        Quaternion rotation = HelperFunctions.allocQuaternion();
        int floatArrayIndex = 0;
        for (SkeletonBone skeletonBone : SkeletonBone.all()) {
            assert (skeletonHierarchy != null);
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
            BallisticsTarget.boneTransformData[floatArrayIndex++] = -position.x * 1.5f;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = -position.y * 1.5f;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = -position.z * 1.5f;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = rotation.x;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = rotation.y;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = rotation.z;
            BallisticsTarget.boneTransformData[floatArrayIndex++] = rotation.w;
        }
        HelperFunctions.releaseVector3f(position);
        HelperFunctions.releaseQuaternion(rotation);
    }

    private void updateSkeleton() {
        this.getBoneTransforms();
        Bullet.updateBallisticsTargetSkeleton(this.getID(), this.numberOfBones, boneTransformData);
    }

    private void reset() {
        this.isoGameCharacter = null;
        this.addedToWorld = false;
        this.isInitialized = false;
        this.releaseFrame = 0;
        this.combatDamageDataProcessed = true;
    }

    public void releaseTarget() {
        this.removeFromWorld();
        this.reset();
        if (!this.isFree()) {
            this.release();
        }
    }

    public void debugRender() {
        if (Core.debug && DebugOptions.instance.physicsRenderBallisticsTargets.getValue()) {
            PhysicsDebugRenderer.addBallisticsRender(this);
        }
    }

    private boolean addToWorld() {
        int id = this.getID();
        if (!this.addedToWorld) {
            this.debugRender();
            Bullet.addBallisticsTarget(id);
            this.addedToWorld = true;
            return true;
        }
        return false;
    }

    private void removeFromWorld() {
        int id = this.getID();
        if (this.addedToWorld) {
            Bullet.removeBallisticsTarget(id);
            this.addedToWorld = false;
        }
    }

    public void release(int frames) {
        this.releaseFrame = frames;
    }

    private AnimationPlayer getAnimationPlayer() {
        if (this.isoGameCharacter == null) {
            return null;
        }
        return this.isoGameCharacter.getAnimationPlayer();
    }

    private SkinningBoneHierarchy getSkeletonBoneHierarchy() {
        AnimationPlayer animationPlayer = this.getAnimationPlayer();
        if (animationPlayer == null) {
            return null;
        }
        return animationPlayer.getSkeletonBoneHierarchy();
    }

    public void setCombatDamageDataProcessed(boolean processed) {
        this.combatDamageDataProcessed = processed;
    }

    public boolean getCombatDamageDataProcessed() {
        return this.combatDamageDataProcessed;
    }

    public CombatDamageData getCombatDamageData() {
        return this.combatDamageData;
    }

    public static class CombatDamageData {
        public String event;
        public IsoGameCharacter target;
        public IsoGameCharacter attacker;
        public RagdollBodyPart bodyPart;
        public HandWeapon handWeapon;
        public IsoTrap isoTrap;
    }
}

