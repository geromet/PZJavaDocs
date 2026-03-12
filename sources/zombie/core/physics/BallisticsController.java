/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import org.joml.Quaternionf;
import zombie.CombatManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.combat.CombatConfigKey;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.physics.PhysicsDebugRenderer;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.input.AimingReticle;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.network.GameServer;
import zombie.network.fields.hit.HitInfo;
import zombie.scripting.objects.ModelAttachment;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class BallisticsController
extends PooledObject {
    private static final float angleX = 0.7853982f;
    private static final float angleY = -0.5235988f;
    private static final float angleZ = 0.0f;
    private static final int maxBallisticsTargets = 20;
    private static final int maxBallisticsTargetsArraySize = 80;
    private static final int maxBallisticsCameraTargets = 10;
    public static final int maxBallisticsCameraTargetsArraySize = 50;
    private static final int maxBallisticsSpreadLocations = 9;
    public static final int maxBallisticsSpreadLocationsArraySize = 36;
    private final Vector3 muzzlePosition = new Vector3();
    private final Vector3 muzzleDirection = new Vector3();
    private final Vector3 convertedMuzzleDirection = new Vector3();
    private final float[] ballisticsTargets = new float[80];
    private final float[] ballisticsCameraTargets = new float[50];
    private final float[] ballisticsSpreadData = new float[36];
    private final float[] cachedBallisticsTargets = new float[80];
    private final float[] cachedBallisticsCameraTargets = new float[50];
    private final float[] cachedBallisticsSpreadData = new float[36];
    private int numberOfTargets;
    private int numberOfSpreadData;
    private int numberOfCameraTargets;
    private int cachedNumberOfTargets;
    private int cachedNumberOfSpreadData;
    private int cachedNumberOfCameraTargets;
    private boolean isInitialized;
    private IsoGameCharacter isoGameCharacter;
    private static final Quaternionf pzCameraRotation = new Quaternionf();
    private final Vector3 isoAimingPosition = new Vector3();
    private static final Pool<BallisticsController> controllerPool = new Pool<BallisticsController>(BallisticsController::new);
    private final Vector3 targetPosition = new Vector3();

    public static BallisticsController alloc() {
        return controllerPool.alloc();
    }

    private BallisticsController() {
    }

    public int getID() {
        return this.isoGameCharacter.getID();
    }

    public void setIsoGameCharacter(IsoGameCharacter isoGameCharacter) {
        this.isoGameCharacter = isoGameCharacter;
    }

    public void update() {
        if (!this.isInitialized) {
            this.initialize();
        }
        if (this.isoGameCharacter == null) {
            DebugType.Ballistics.debugln("isoGameCharacter == null");
            return;
        }
        if (this.isoGameCharacter.primaryHandModel == null) {
            DebugType.Ballistics.debugln("isoGameCharacter.primaryHandModel == null");
            return;
        }
        if (this.isoGameCharacter.primaryHandModel.modelScript == null) {
            DebugType.Ballistics.debugln("isoGameCharacter.primaryHandModel.m_modelScript == null");
            return;
        }
        ModelAttachment attachment = this.isoGameCharacter.primaryHandModel.modelScript.getAttachmentById("muzzle");
        if (attachment == null) {
            DebugType.Ballistics.debugln("muzzle attachment == null");
            return;
        }
        this.calculateMuzzlePosition(this.muzzlePosition, this.muzzleDirection);
        this.convertedMuzzleDirection.set(this.muzzleDirection.x, this.muzzleDirection.z * 2.44949f, this.muzzleDirection.y);
        this.convertedMuzzleDirection.normalize();
        this.muzzleDirection.normalize();
        float x = AimingReticle.getX(IsoPlayer.getPlayerIndex(this.isoGameCharacter));
        float y = AimingReticle.getY(IsoPlayer.getPlayerIndex(this.isoGameCharacter));
        float z = 0.0f;
        this.isoAimingPosition.set(IsoUtils.XToIso(x, y, 0.0f), IsoUtils.YToIso(x, y, 0.0f), 0.0f);
        if (!GameServer.server) {
            Bullet.updateBallistics(this.isoGameCharacter.getID(), this.muzzlePosition.x, this.muzzlePosition.z * 2.44949f, this.muzzlePosition.y);
            Bullet.updateBallisticsAimReticlePosition(this.isoGameCharacter.getID(), IsoUtils.XToIso(x, y, 0.0f), 0.0f, IsoUtils.YToIso(x, y, 0.0f));
            Bullet.updateBallisticsMuzzleAimDirection(this.isoGameCharacter.getID(), this.convertedMuzzleDirection.x, this.convertedMuzzleDirection.y, this.convertedMuzzleDirection.z);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
        }
        HandWeapon weapon = this.isoGameCharacter.getAttackingWeapon();
        float weaponRange = weapon.getMaxRange() * weapon.getRangeMod(this.isoGameCharacter);
        if (this.hasBallisticsTarget()) {
            this.getCameraTarget(0, this.targetPosition);
        } else {
            this.targetPosition.set(this.muzzlePosition.x + this.muzzleDirection.x * weaponRange, this.muzzlePosition.y + this.muzzleDirection.y * weaponRange, this.muzzlePosition.z + this.muzzleDirection.z * weaponRange);
        }
        if (!this.isInitialized) {
            if (Core.debug) {
                if (!GameServer.server) {
                    Bullet.setBallisticsSize(this.isoGameCharacter.getID(), 0.025f);
                    Bullet.setBallisticsColor(this.isoGameCharacter.getID(), 0.0f, 0.0f, 1.0f);
                } else {
                    DebugLog.General.printStackTrace("Bullet is disabled on server");
                }
            }
            pzCameraRotation.rotationYXZ(0.7853982f, -0.5235988f, 0.0f);
            if (!GameServer.server) {
                Bullet.updateBallisticsAimReticleQuaternion(this.isoGameCharacter.getID(), BallisticsController.pzCameraRotation.x, BallisticsController.pzCameraRotation.y, BallisticsController.pzCameraRotation.z, BallisticsController.pzCameraRotation.w);
            } else {
                DebugLog.General.printStackTrace("Bullet is disabled on server");
            }
            this.isInitialized = true;
        }
    }

    public boolean updateAimingVector(IsoGameCharacter isoGameCharacter, AimingVectorParameters parameters) {
        if (!this.isInitialized) {
            parameters.resetOutputs();
            return false;
        }
        if (!this.hasBallisticsTarget()) {
            parameters.resetOutputs();
            return false;
        }
        parameters.targetPosition.set(this.targetPosition);
        if (PZMath.equal(parameters.targetPosition.x, 0.0f, 0.001f) && PZMath.equal(parameters.targetPosition.y, 0.0f, 0.001f) && PZMath.equal(parameters.targetPosition.z, 0.0f, 0.001f)) {
            parameters.resetOutputs();
            return false;
        }
        parameters.muzzlePosition.set(this.muzzlePosition);
        parameters.muzzleDirection.set(this.muzzleDirection);
        float charPosX = isoGameCharacter.getX();
        float charPosY = isoGameCharacter.getY();
        float charPosZ = isoGameCharacter.getZ();
        float dx = parameters.targetPosition.x - charPosX;
        float dy = parameters.targetPosition.y - charPosY;
        float dz = parameters.targetPosition.z - (charPosZ + 0.47f);
        parameters.desiredForward.set(dx, dy, dz);
        if (parameters.desiredForward.getLengthSq() <= 0.0f) {
            parameters.resetOutputs();
            return false;
        }
        parameters.desiredForward.normalize();
        parameters.desiredForward2f.set(parameters.desiredForward.x, parameters.desiredForward.y);
        float horizontalDistanceSq = parameters.desiredForward2f.getLengthSquared();
        if (horizontalDistanceSq <= 0.0f) {
            parameters.desiredForwardPitchRads = parameters.desiredForward.z > 0.0f ? 1.5707964f : -1.5707964f;
            return false;
        }
        float horizontalDistance = parameters.desiredForward2f.normalize();
        parameters.desiredForwardPitchRads = (float)Math.atan2(parameters.desiredForward.z, horizontalDistance);
        parameters.desiredForwardPitchRads *= 2.44949f;
        return true;
    }

    public void debugRender() {
        if (Core.debug && DebugOptions.instance.physicsRenderBallisticsControllers.getValue() && this.isoGameCharacter.isAiming()) {
            PhysicsDebugRenderer.addBallisticsRender(this);
        }
    }

    public void calculateMuzzlePosition(Vector3 muzzlePosition, Vector3 muzzleDirectionUnnormalized) {
        if (this.isoGameCharacter.primaryHandModel != null && this.isoGameCharacter.primaryHandModel.modelScript != null) {
            ModelInstance ballisticsFiringModelInstance = this.isoGameCharacter.primaryHandModel;
            ModelAttachment attachment = this.isoGameCharacter.primaryHandModel.modelScript.getAttachmentById("muzzle");
            if (attachment != null && ballisticsFiringModelInstance.animPlayer != null) {
                ballisticsFiringModelInstance.getAttachmentWorldPosition(attachment, muzzlePosition, muzzleDirectionUnnormalized);
            }
        }
    }

    private boolean initialize() {
        if (this.isInitialized) {
            return true;
        }
        pzCameraRotation.rotationYXZ(0.7853982f, -0.5235988f, 0.0f);
        if (!GameServer.server) {
            Bullet.updateBallisticsAimReticleQuaternion(this.isoGameCharacter.getID(), BallisticsController.pzCameraRotation.x, BallisticsController.pzCameraRotation.y, BallisticsController.pzCameraRotation.z, BallisticsController.pzCameraRotation.w);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
        }
        return true;
    }

    public void setRange(float range) {
        if (!GameServer.server) {
            Bullet.setBallisticsRange(this.isoGameCharacter.getID(), range);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
        }
    }

    public void getTargets(float range) {
        if (!GameServer.server) {
            this.numberOfTargets = Bullet.getBallisticsTargets(this.isoGameCharacter.getID(), range, 20, this.ballisticsTargets);
            System.arraycopy(this.ballisticsTargets, 0, this.cachedBallisticsTargets, 0, 80);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
            this.numberOfTargets = 0;
        }
        this.cachedNumberOfTargets = this.numberOfTargets;
    }

    public float[] getBallisticsSpreadData() {
        return this.ballisticsSpreadData;
    }

    public float[] getBallisticsTargets() {
        return this.ballisticsTargets;
    }

    public float[] getCachedBallisticsTargets() {
        return this.cachedBallisticsTargets;
    }

    public float[] getCachedBallisticsTargetSpreadData() {
        return this.cachedBallisticsSpreadData;
    }

    public void getSpreadData(float range, float spread, float weightCenter, int numberOfBullets) {
        if (!GameServer.server) {
            this.numberOfSpreadData = Bullet.getBallisticsTargetsSpreadData(this.isoGameCharacter.getID(), range, spread, weightCenter, numberOfBullets, 9, this.ballisticsSpreadData);
            System.arraycopy(this.ballisticsSpreadData, 0, this.cachedBallisticsSpreadData, 0, 36);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
            this.numberOfSpreadData = 0;
        }
        this.cachedNumberOfSpreadData = this.numberOfSpreadData;
    }

    public void getCameraTargets(float range, boolean parts) {
        if (!GameServer.server) {
            this.numberOfCameraTargets = Bullet.getBallisticsCameraTargets(this.isoGameCharacter.getID(), range, 10, parts, this.ballisticsCameraTargets);
            System.arraycopy(this.ballisticsCameraTargets, 0, this.cachedBallisticsCameraTargets, 0, 50);
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
            this.numberOfCameraTargets = 0;
        }
        this.cachedNumberOfCameraTargets = this.numberOfCameraTargets;
    }

    public float[] getCameraTargets() {
        return this.ballisticsCameraTargets;
    }

    public boolean isValidTarget(int id) {
        return this.isTarget(id) || this.isCameraTarget(id) || this.isSpreadTarget(id);
    }

    public boolean isValidCachedTarget(int id) {
        return this.isCachedTarget(id) || this.isCachedCameraTarget(id) || this.isCachedSpreadTarget(id);
    }

    public boolean isTarget(int id) {
        for (int i = 0; i < this.numberOfTargets; ++i) {
            int arrayIndex = i * 4;
            if (this.ballisticsTargets[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public boolean isCachedTarget(int id) {
        for (int i = 0; i < this.cachedNumberOfTargets; ++i) {
            int arrayIndex = i * 4;
            if (this.cachedBallisticsTargets[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public boolean isCameraTarget(int id) {
        for (int i = 0; i < this.numberOfCameraTargets; ++i) {
            int arrayIndex = i * 5;
            if (this.ballisticsCameraTargets[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public boolean isCachedCameraTarget(int id) {
        for (int i = 0; i < this.cachedNumberOfCameraTargets; ++i) {
            int arrayIndex = i * 5;
            if (this.cachedBallisticsCameraTargets[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public int getTargetedBodyPart(int id) {
        for (int i = 0; i < this.numberOfCameraTargets; ++i) {
            int arrayIndex = i * 5;
            if (this.ballisticsCameraTargets[arrayIndex] != (float)id) continue;
            return (int)this.ballisticsCameraTargets[arrayIndex + 4];
        }
        return RagdollBodyPart.BODYPART_COUNT.ordinal();
    }

    public int getCachedTargetedBodyPart(int id) {
        for (int i = 0; i < this.cachedNumberOfCameraTargets; ++i) {
            int arrayIndex = i * 5;
            if (this.cachedBallisticsCameraTargets[arrayIndex] != (float)id) continue;
            return (int)this.cachedBallisticsCameraTargets[arrayIndex + 4];
        }
        return RagdollBodyPart.BODYPART_COUNT.ordinal();
    }

    public boolean isSpreadTarget(int id) {
        for (int i = 0; i < this.numberOfSpreadData; ++i) {
            int arrayIndex = i * 4;
            if (this.ballisticsSpreadData[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public boolean isCachedSpreadTarget(int id) {
        for (int i = 0; i < this.cachedNumberOfSpreadData; ++i) {
            int arrayIndex = i * 4;
            if (this.cachedBallisticsSpreadData[arrayIndex] != (float)id) continue;
            return true;
        }
        return false;
    }

    public boolean hasSpreadData() {
        return this.numberOfSpreadData != 0;
    }

    public int getNumberOfSpreadData() {
        return this.numberOfSpreadData;
    }

    public int getNumberOfCachedSpreadData() {
        return this.cachedNumberOfSpreadData;
    }

    private void removeFromWorld() {
        if (!GameServer.server) {
            Bullet.removeBallistics(this.getID());
        } else {
            DebugLog.General.printStackTrace("Bullet is disabled on server");
        }
    }

    public void releaseController() {
        this.removeFromWorld();
        this.reset();
        this.release();
    }

    public void postUpdate() {
        this.numberOfTargets = 0;
        this.numberOfSpreadData = 0;
        this.numberOfCameraTargets = 0;
    }

    private void reset() {
        this.numberOfTargets = 0;
        this.numberOfSpreadData = 0;
        this.numberOfCameraTargets = 0;
        this.isInitialized = false;
    }

    public int getNumberOfCameraTargets() {
        return this.numberOfCameraTargets;
    }

    public int spreadCount(int id) {
        int count = 0;
        for (int i = 0; i < this.numberOfSpreadData; ++i) {
            int arrayIndex = i * 4;
            if (this.ballisticsSpreadData[arrayIndex] != (float)id) continue;
            ++count;
        }
        return count;
    }

    public int cachedSpreadCount(int id) {
        int count = 0;
        for (int i = 0; i < this.cachedNumberOfSpreadData; ++i) {
            int arrayIndex = i * 4;
            if (this.cachedBallisticsSpreadData[arrayIndex] != (float)id) continue;
            ++count;
        }
        return count;
    }

    public void clearCacheTargets() {
        this.cachedNumberOfTargets = 0;
        this.cachedNumberOfCameraTargets = 0;
        this.cachedNumberOfSpreadData = 0;
    }

    public int getNumberOfTargets() {
        return this.numberOfTargets;
    }

    public int getCachedNumberOfTargets() {
        return this.cachedNumberOfTargets;
    }

    public boolean hasBallisticsTarget() {
        return this.numberOfCameraTargets > 0;
    }

    public void renderlast() {
        if (this.isoGameCharacter.primaryHandModel == null) {
            return;
        }
        boolean thickness = true;
        float alpha = 1.0f;
        Vector3 firingStart = new Vector3();
        Vector3 firingDirection = new Vector3();
        Vector3 firingEnd = new Vector3();
        Color isoAimPositionColor = Color.magenta;
        Color muzzleVectorColor = Color.red;
        Color muzzlePositionColor = Color.orange;
        Color targetPositionColor = Color.cyan;
        HandWeapon weapon = this.isoGameCharacter.getAttackingWeapon();
        float weaponRange = weapon.getMaxRange(this.isoGameCharacter) * weapon.getRangeMod(this.isoGameCharacter);
        this.calculateMuzzlePosition(firingStart, firingDirection);
        firingDirection.normalize();
        firingEnd.set(firingStart.x + firingDirection.x * weaponRange, firingStart.y + firingDirection.y * weaponRange, firingStart.z + firingDirection.z * weaponRange);
        LineDrawer.DrawIsoLine(firingStart.x, firingStart.y, firingStart.z, firingEnd.x, firingEnd.y, firingEnd.z, muzzleVectorColor.r, muzzleVectorColor.g, muzzleVectorColor.b, 1.0f, 1);
        LineDrawer.DrawIsoCircle(firingStart.x, firingStart.y, firingStart.z, 0.1f, 8, muzzlePositionColor.r, muzzlePositionColor.g, muzzlePositionColor.b, 1.0f);
        LineDrawer.DrawIsoCircle(firingEnd.x, firingEnd.y, firingEnd.z, 0.1f, 8, muzzlePositionColor.r, muzzlePositionColor.g, muzzlePositionColor.b, 1.0f);
        LineDrawer.DrawIsoCircle(this.isoAimingPosition.x, this.isoAimingPosition.y, 0.0f, CombatManager.getInstance().getCombatConfig().get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD), 32, isoAimPositionColor.r, isoAimPositionColor.g, isoAimPositionColor.b, 1.0f);
        LineDrawer.DrawIsoLine(firingStart.x, firingStart.y, firingStart.z, this.targetPosition.x, this.targetPosition.y, this.targetPosition.z, targetPositionColor.r, targetPositionColor.g, targetPositionColor.b, 1.0f, 1);
        LineDrawer.DrawIsoCircle(this.targetPosition.x, this.targetPosition.y, this.targetPosition.z, 0.1f, 8, targetPositionColor.r, targetPositionColor.g, targetPositionColor.b, 1.0f);
    }

    public Vector3 getMuzzlePosition() {
        return this.muzzlePosition;
    }

    private void getCameraTarget(int index, Vector3 targetPosition) {
        int id = (int)this.ballisticsCameraTargets[index * 5];
        float x = this.ballisticsCameraTargets[index * 5 + 1];
        float y = this.ballisticsCameraTargets[index * 5 + 2] / 2.44949f;
        float z = this.ballisticsCameraTargets[index * 5 + 3];
        float ragdollBodyPart = (int)this.ballisticsCameraTargets[index * 5 + 4];
        targetPosition.set(x, z, y);
    }

    public Vector3 getMuzzleDirection() {
        return this.muzzleDirection;
    }

    public Vector3 getIsoAimingPosition() {
        return this.isoAimingPosition;
    }

    public void setBallisticsTargetHitLocation(int id, HitInfo hitInfo) {
        for (int i = 0; i < this.numberOfTargets; ++i) {
            int arrayIndex = i * 4;
            if ((int)this.ballisticsTargets[arrayIndex] != id) continue;
            hitInfo.x = this.ballisticsTargets[arrayIndex + 1];
            hitInfo.z = this.ballisticsTargets[arrayIndex + 2] / 2.44949f;
            hitInfo.y = this.ballisticsTargets[arrayIndex + 3];
            return;
        }
    }

    public void setBallisticsCameraTargetHitLocation(int id, HitInfo hitInfo) {
        for (int i = 0; i < this.numberOfCameraTargets; ++i) {
            int arrayIndex = i * 5;
            if ((int)this.ballisticsCameraTargets[arrayIndex] != id) continue;
            hitInfo.x = this.ballisticsCameraTargets[arrayIndex + 1];
            hitInfo.z = this.ballisticsCameraTargets[arrayIndex + 2] / 2.44949f;
            hitInfo.y = this.ballisticsCameraTargets[arrayIndex + 3];
            return;
        }
    }

    public static class AimingVectorParameters {
        public final Vector3 muzzlePosition = new Vector3();
        public final Vector3 muzzleDirection = new Vector3();
        public final Vector3 targetPosition = new Vector3();
        public final Vector3 desiredForward = new Vector3();
        public final Vector2 desiredForward2f = new Vector2();
        public float desiredForwardPitchRads;

        public void resetOutputs() {
            this.muzzlePosition.set(0.0f, 0.0f, 0.0f);
            this.muzzleDirection.set(0.0f, 0.0f, 0.0f);
            this.targetPosition.set(0.0f, 0.0f, 0.0f);
            this.desiredForward.set(this.desiredForward2f.x, this.desiredForward2f.y, 0.0f);
            this.desiredForwardPitchRads = 0.0f;
        }
    }
}

