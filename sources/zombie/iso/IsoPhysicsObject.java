/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.network.GameServer;

public class IsoPhysicsObject
extends IsoMovingObject {
    public float velX;
    public float velY;
    public float velZ;
    public float terminalVelocity = -0.05f;
    protected float speedMod = 1.0f;

    public IsoPhysicsObject(IsoCell cell) {
        this.solid = false;
        this.shootable = false;
    }

    public void collideGround() {
    }

    public void collideWall() {
    }

    @Override
    public void update() {
        IsoGridSquare sq = this.getCurrentSquare();
        super.update();
        if (this.isCollidedThisFrame()) {
            if (this.isCollidedN() || this.isCollidedS()) {
                this.velY = -this.velY;
                this.velY *= 0.5f;
                this.collideWall();
            }
            if (this.isCollidedE() || this.isCollidedW()) {
                this.velX = -this.velX;
                this.velX *= 0.5f;
                this.collideWall();
            }
        }
        int fps = GameServer.server ? 10 : PerformanceSettings.getLockFPS();
        float fpsMod = 30.0f / (float)fps;
        this.speedMod = 1.0f;
        float remove = 0.1f * this.speedMod * fpsMod;
        remove = 1.0f - remove;
        this.velX *= remove;
        this.velY *= remove;
        this.velZ -= 0.005f * fpsMod;
        if (this.velZ < this.terminalVelocity) {
            this.velZ = this.terminalVelocity;
        }
        this.setNextX(this.getNextX() + this.velX * this.speedMod * 0.3f * fpsMod);
        this.setNextY(this.getNextY() + this.velY * this.speedMod * 0.3f * fpsMod);
        float lastZ = this.getZ();
        this.setZ(this.getZ() + this.velZ * 0.4f * fpsMod);
        if (this.getZ() < 0.0f) {
            this.setZ(0.0f);
            this.velZ = -this.velZ * 0.5f;
            this.collideGround();
        }
        if (this.getCurrentSquare() != null && PZMath.fastfloor(this.getZ()) < PZMath.fastfloor(lastZ) && (sq != null && sq.TreatAsSolidFloor() || this.getCurrentSquare().TreatAsSolidFloor())) {
            this.setZ(PZMath.fastfloor(lastZ));
            this.velZ = -this.velZ * 0.5f;
            this.collideGround();
        }
        if (Math.abs(this.velX) < 1.0E-4f) {
            this.velX = 0.0f;
        }
        if (Math.abs(this.velY) < 1.0E-4f) {
            this.velY = 0.0f;
        }
        if (this.velX + this.velY == 0.0f) {
            this.sprite.animate = false;
        }
        this.sy = 0.0f;
        this.sx = 0.0f;
    }

    @Override
    public float getGlobalMovementMod(boolean bDoNoises) {
        return 1.0f;
    }
}

