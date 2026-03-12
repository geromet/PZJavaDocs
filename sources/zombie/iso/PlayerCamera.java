/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameServer;
import zombie.ui.SpeedControls;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;

public final class PlayerCamera {
    public final int playerIndex;
    public float offX;
    public float offY;
    private float tOffX;
    private float tOffY;
    public float lastOffX;
    public float lastOffY;
    public float rightClickTargetX;
    public float rightClickTargetY;
    public float rightClickX;
    public float rightClickY;
    private float rightClickXf;
    private float rightClickYf;
    public float deferedX;
    public float deferedY;
    public float zoom;
    public int offscreenWidth;
    public int offscreenHeight;
    public final Matrix4f projection = new Matrix4f();
    public final Matrix4f modelview = new Matrix4f();
    private static final Vector2 offVec = new Vector2();
    private static float panSpeed = 1.0f;
    private long panTime = -1L;
    private final Vector3f lastVehicleForwardDirection = new Vector3f();
    public int width;
    public int height;
    public float fixJigglyModelsX;
    public float fixJigglyModelsY;
    public float fixJigglyModelsSquareX;
    public float fixJigglyModelsSquareY;
    private static final int[] s_viewport = new int[]{0, 0, 0, 0};
    private static final Vector3f s_tempVector3f_1 = new Vector3f();

    public PlayerCamera(int playerIndex) {
        this.playerIndex = playerIndex;
    }

    public void center() {
        float offX = this.offX;
        float offY = this.offY;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter != null) {
            float chrZ = IsoCamera.frameState.calculateCameraZ(isoGameCharacter);
            offX = IsoUtils.XToScreen(isoGameCharacter.getX() + this.deferedX, isoGameCharacter.getY() + this.deferedY, chrZ, 0);
            offY = IsoUtils.YToScreen(isoGameCharacter.getX() + this.deferedX, isoGameCharacter.getY() + this.deferedY, chrZ, 0);
            offX -= (float)IsoCamera.getOffscreenWidth(this.playerIndex) / 2.0f;
            offY -= (float)IsoCamera.getOffscreenHeight(this.playerIndex) / 2.0f;
            offY -= isoGameCharacter.getOffsetY() * 1.5f;
            offX += (float)IsoCamera.playerOffsetX;
            offY += (float)IsoCamera.playerOffsetY;
        }
        this.offX = this.tOffX = offX;
        this.offY = this.tOffY = offY;
    }

    public void update() {
        BaseVehicle vehicle;
        this.center();
        float ddx = (this.tOffX - this.offX) / 15.0f;
        float ddy = (this.tOffY - this.offY) / 15.0f;
        this.offX += ddx;
        this.offY += ddy;
        if (this.lastOffX == 0.0f && this.lastOffY == 0.0f) {
            this.lastOffX = this.offX;
            this.lastOffY = this.offY;
        }
        long now = System.currentTimeMillis();
        panSpeed = 110.0f;
        float mult = (float)this.panTime < 0.0f ? 1.0f : (float)(now - this.panTime) / 1000.0f * panSpeed;
        mult = 1.0f / mult;
        this.panTime = now;
        IsoPlayer player = IsoPlayer.players[this.playerIndex];
        boolean isJoypad = GameWindow.activatedJoyPad != null && player != null && player.joypadBind != -1;
        BaseVehicle baseVehicle = vehicle = player == null ? null : player.getVehicle();
        if (vehicle != null && vehicle.getCurrentSpeedKmHour() <= 1.0f) {
            vehicle.getForwardVector(this.lastVehicleForwardDirection);
        }
        if (Core.getInstance().getOptionPanCameraWhileDriving() && vehicle != null && vehicle.getCurrentSpeedKmHour() > 1.0f) {
            float zoom = Core.getInstance().getZoom(this.playerIndex);
            float s = vehicle.getCurrentSpeedKmHour() * BaseVehicle.getFakeSpeedModifier() / 10.0f;
            s *= zoom;
            Vector3f forward = vehicle.getForwardVector((Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc());
            float angleDiff = this.lastVehicleForwardDirection.angle(forward) * 57.295776f;
            if (angleDiff > 1.0f) {
                float f = angleDiff / 180.0f / (float)PerformanceSettings.getLockFPS();
                f = PZMath.max(f, 0.1f);
                this.lastVehicleForwardDirection.lerp(forward, f, forward);
                this.lastVehicleForwardDirection.set(forward);
            }
            this.rightClickTargetX = (int)IsoUtils.XToScreen(forward.x * s, forward.z * s, player.getZ(), 0);
            this.rightClickTargetY = (int)IsoUtils.YToScreen(forward.x * s, forward.z * s, player.getZ(), 0);
            BaseVehicle.TL_vector3f_pool.get().release(forward);
            boolean screenX = false;
            boolean screenY = false;
            int screenW = IsoCamera.getOffscreenWidth(this.playerIndex);
            int screenH = IsoCamera.getOffscreenHeight(this.playerIndex);
            float centerX = 0.0f + (float)screenW / 2.0f;
            float centerY = 0.0f + (float)screenH / 2.0f;
            float border = 150.0f * zoom;
            this.rightClickTargetX = (float)((int)PZMath.clamp(centerX + this.rightClickTargetX, border, (float)screenW - border)) - centerX;
            this.rightClickTargetY = (float)((int)PZMath.clamp(centerY + this.rightClickTargetY, border, (float)screenH - border)) - centerY;
            if (Math.abs(s) < 5.0f) {
                float f = 1.0f - Math.abs(s) / 5.0f;
                this.returnToCenter(1.0f / (16.0f * mult / f));
            } else {
                mult /= 0.5f * zoom;
                float playerX = IsoUtils.XToScreenExact(player.getX(), player.getY(), player.getZ(), 0);
                float playerY = IsoUtils.YToScreenExact(player.getX(), player.getY(), player.getZ(), 0);
                if (playerX < border / 2.0f || playerX > (float)screenW - border / 2.0f || playerY < border / 2.0f || playerY > (float)screenH - border / 2.0f) {
                    mult /= 4.0f;
                }
                this.rightClickXf = PZMath.step(this.rightClickXf, this.rightClickTargetX, 1.875f * (float)PZMath.sign(this.rightClickTargetX - this.rightClickXf) / mult);
                this.rightClickYf = PZMath.step(this.rightClickYf, this.rightClickTargetY, 1.875f * (float)PZMath.sign(this.rightClickTargetY - this.rightClickYf) / mult);
                this.rightClickX = (int)this.rightClickXf;
                this.rightClickY = (int)this.rightClickYf;
            }
        } else if (isJoypad && player != null) {
            if ((player.isAiming() || player.isLookingWhileInVehicle()) && JoypadManager.instance.isRBPressed(player.joypadBind) && !player.joypadIgnoreAimUntilCentered) {
                this.rightClickTargetX = JoypadManager.instance.getAimingAxisX(player.joypadBind) * 1500.0f;
                this.rightClickTargetY = JoypadManager.instance.getAimingAxisY(player.joypadBind) * 1500.0f;
                this.rightClickXf = PZMath.step(this.rightClickXf, this.rightClickTargetX, (this.rightClickTargetX - this.rightClickXf) / (80.0f * (mult /= 0.5f * Core.getInstance().getZoom(this.playerIndex))));
                this.rightClickYf = PZMath.step(this.rightClickYf, this.rightClickTargetY, (this.rightClickTargetY - this.rightClickYf) / (80.0f * mult));
                this.rightClickX = (int)this.rightClickXf;
                this.rightClickY = (int)this.rightClickYf;
                player.dirtyRecalcGridStackTime = 2.0f;
            } else {
                this.returnToCenter(1.0f / (16.0f * mult));
            }
        } else if (this.playerIndex == 0 && player != null && !player.isBlockMovement() && GameKeyboard.isKeyDown("PanCamera")) {
            float screenRatio;
            int screenWidth = IsoCamera.getScreenWidth(this.playerIndex);
            int screenHeight = IsoCamera.getScreenHeight(this.playerIndex);
            int x1 = IsoCamera.getScreenLeft(this.playerIndex);
            int y1 = IsoCamera.getScreenTop(this.playerIndex);
            float difX = (float)Mouse.getXA() - ((float)x1 + (float)screenWidth / 2.0f);
            float difY = (float)Mouse.getYA() - ((float)y1 + (float)screenHeight / 2.0f);
            if (screenWidth > screenHeight) {
                screenRatio = (float)screenHeight / (float)screenWidth;
                difX *= screenRatio;
            } else {
                screenRatio = (float)screenWidth / (float)screenHeight;
                difY *= screenRatio;
            }
            offVec.set(difX, difY);
            offVec.setLength(Math.min(offVec.getLength(), (float)Math.min(screenWidth, screenHeight) / 2.0f));
            difX = PlayerCamera.offVec.x / (screenRatio *= (float)screenWidth / 1366.0f);
            difY = PlayerCamera.offVec.y / screenRatio;
            this.rightClickTargetX = difX * 2.0f;
            this.rightClickTargetY = difY * 2.0f;
            this.rightClickXf = PZMath.step(this.rightClickXf, this.rightClickTargetX, (this.rightClickTargetX - this.rightClickXf) / (80.0f * (mult /= 0.5f * Core.getInstance().getZoom(this.playerIndex))));
            this.rightClickYf = PZMath.step(this.rightClickYf, this.rightClickTargetY, (this.rightClickTargetY - this.rightClickYf) / (80.0f * mult));
            this.rightClickX = (int)this.rightClickXf;
            this.rightClickY = (int)this.rightClickYf;
            player.dirtyRecalcGridStackTime = 2.0f;
            IsoSprite.globalOffsetX = -1.0f;
        } else if (this.playerIndex == 0 && Core.getInstance().getOptionPanCameraWhileAiming() && SpeedControls.instance.getCurrentGameSpeed() > 0) {
            boolean alive;
            boolean isclient = !GameServer.server;
            boolean isaiming = !UIManager.isMouseOverInventory() && player != null && player.isAiming();
            boolean bl = alive = !isJoypad && player != null && !player.isDead();
            if (isclient && isaiming && alive) {
                float screenRatio;
                int screenWidth = IsoCamera.getScreenWidth(this.playerIndex);
                int screenHeight = IsoCamera.getScreenHeight(this.playerIndex);
                int x1 = IsoCamera.getScreenLeft(this.playerIndex);
                int y1 = IsoCamera.getScreenTop(this.playerIndex);
                float difX = (float)Mouse.getXA() - ((float)x1 + (float)screenWidth / 2.0f);
                float difY = (float)Mouse.getYA() - ((float)y1 + (float)screenHeight / 2.0f);
                if (screenWidth > screenHeight) {
                    screenRatio = (float)screenHeight / (float)screenWidth;
                    difX *= screenRatio;
                } else {
                    screenRatio = (float)screenWidth / (float)screenHeight;
                    difY *= screenRatio;
                }
                screenRatio *= (float)screenWidth / 1366.0f;
                float screenDim = (float)Math.min(screenWidth, screenHeight) / 6.0f;
                float minPan = (float)Math.min(screenWidth, screenHeight) / 2.0f - screenDim;
                offVec.set(difX, difY);
                if (offVec.getLength() < minPan) {
                    difY = 0.0f;
                    difX = 0.0f;
                } else {
                    offVec.setLength(Math.min(offVec.getLength(), (float)Math.min(screenWidth, screenHeight) / 2.0f) - minPan);
                    difX = PlayerCamera.offVec.x / screenRatio;
                    difY = PlayerCamera.offVec.y / screenRatio;
                }
                this.rightClickTargetX = difX * 7.0f;
                this.rightClickTargetY = difY * 7.0f;
                this.rightClickXf = PZMath.step(this.rightClickXf, this.rightClickTargetX, (this.rightClickTargetX - this.rightClickXf) / (80.0f * (mult /= 0.5f * Core.getInstance().getZoom(this.playerIndex))));
                this.rightClickYf = PZMath.step(this.rightClickYf, this.rightClickTargetY, (this.rightClickTargetY - this.rightClickYf) / (80.0f * mult));
                this.rightClickX = (int)this.rightClickXf;
                this.rightClickY = (int)this.rightClickYf;
                player.dirtyRecalcGridStackTime = 2.0f;
            } else {
                this.returnToCenter(1.0f / (16.0f * mult));
            }
            IsoSprite.globalOffsetX = -1.0f;
        } else {
            this.returnToCenter(1.0f / (16.0f * mult));
        }
        this.zoom = Core.getInstance().getZoom(this.playerIndex);
    }

    private void returnToCenter(float mult) {
        this.rightClickTargetX = 0.0f;
        this.rightClickTargetY = 0.0f;
        if (this.rightClickTargetX != this.rightClickX || this.rightClickTargetY != this.rightClickY) {
            this.rightClickXf = PZMath.step(this.rightClickXf, this.rightClickTargetX, (this.rightClickTargetX - this.rightClickXf) * mult);
            this.rightClickYf = PZMath.step(this.rightClickYf, this.rightClickTargetY, (this.rightClickTargetY - this.rightClickYf) * mult);
            this.rightClickX = (int)this.rightClickXf;
            this.rightClickY = (int)this.rightClickYf;
            if (Math.abs(this.rightClickTargetX - this.rightClickXf) < 0.001f) {
                this.rightClickXf = this.rightClickX = (float)((int)this.rightClickTargetX);
            }
            if (Math.abs(this.rightClickTargetY - this.rightClickYf) < 0.001f) {
                this.rightClickYf = this.rightClickY = (float)((int)this.rightClickTargetY);
            }
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            player.dirtyRecalcGridStackTime = 2.0f;
        }
    }

    public float getOffX() {
        return (int)(this.offX + this.rightClickX);
    }

    public float getOffY() {
        return (int)(this.offY + this.rightClickY);
    }

    public float getTOffX() {
        float x = this.tOffX - this.offX;
        return (int)(this.offX + this.rightClickX - x);
    }

    public float getTOffY() {
        float y = this.tOffY - this.offY;
        return (int)(this.offY + this.rightClickY - y);
    }

    public float getLastOffX() {
        return (int)(this.lastOffX + this.rightClickX);
    }

    public float getLastOffY() {
        return (int)(this.lastOffY + this.rightClickY);
    }

    public float XToIso(float screenX, float screenY, float floor) {
        screenX = (int)screenX;
        screenY = (int)screenY;
        float px = screenX + this.getOffX();
        float py = screenY + this.getOffY();
        float tx = (px + 2.0f * py) / (64.0f * (float)Core.tileScale);
        return tx += 3.0f * floor;
    }

    public float YToIso(float screenX, float screenY, float floor) {
        screenX = (int)screenX;
        screenY = (int)screenY;
        float px = screenX + this.getOffX();
        float py = screenY + this.getOffY();
        float ty = (px - 2.0f * py) / (-64.0f * (float)Core.tileScale);
        return ty += 3.0f * floor;
    }

    public float YToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        float wy = IsoUtils.YToScreen(objectX, objectY, objectZ, screenZ);
        return wy -= this.getOffY();
    }

    public float XToScreenExact(float objectX, float objectY, float objectZ, int screenZ) {
        float wx = IsoUtils.XToScreen(objectX, objectY, objectZ, screenZ);
        return wx -= this.getOffX();
    }

    public void copyFrom(PlayerCamera other) {
        this.offX = other.offX;
        this.offY = other.offY;
        this.tOffX = other.tOffX;
        this.tOffY = other.tOffY;
        this.lastOffX = other.lastOffX;
        this.lastOffY = other.lastOffY;
        this.rightClickTargetX = other.rightClickTargetX;
        this.rightClickTargetY = other.rightClickTargetY;
        this.rightClickX = other.rightClickX;
        this.rightClickY = other.rightClickY;
        this.deferedX = other.deferedX;
        this.deferedY = other.deferedY;
        this.zoom = other.zoom;
        this.offscreenWidth = other.offscreenWidth;
        this.offscreenHeight = other.offscreenHeight;
        this.width = other.width;
        this.height = other.height;
        this.fixJigglyModelsX = other.fixJigglyModelsX;
        this.fixJigglyModelsY = other.fixJigglyModelsY;
        this.fixJigglyModelsSquareX = other.fixJigglyModelsSquareX;
        this.fixJigglyModelsSquareY = other.fixJigglyModelsSquareY;
        this.projection.set(other.projection);
        this.modelview.set(other.modelview);
    }

    public void initFromIsoCamera(int playerIndex) {
        this.copyFrom(IsoCamera.cameras[playerIndex]);
        this.zoom = Core.getInstance().getZoom(playerIndex);
        this.offscreenWidth = IsoCamera.getOffscreenWidth(playerIndex);
        this.offscreenHeight = IsoCamera.getOffscreenHeight(playerIndex);
        this.width = IsoCamera.getScreenWidth(playerIndex);
        this.height = IsoCamera.getScreenHeight(playerIndex);
    }

    public void calculateModelViewProjection(float ox, float oy, float oz) {
        PlayerCamera cam = this;
        this.offscreenWidth = IsoCamera.getOffscreenWidth(this.playerIndex);
        this.offscreenHeight = IsoCamera.getOffscreenHeight(this.playerIndex);
        this.zoom = Core.getInstance().getZoom(this.playerIndex);
        float rcx = cam.rightClickX;
        float rcy = cam.rightClickY;
        float tox = cam.getTOffX();
        float toy = cam.getTOffY();
        float defx = cam.deferedX;
        float defy = cam.deferedY;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        float x = isoGameCharacter.getX();
        float y = isoGameCharacter.getY();
        float z = IsoCamera.frameState.calculateCameraZ(isoGameCharacter);
        x -= cam.XToIso(-tox - rcx, -toy - rcy, 0.0f);
        y -= cam.YToIso(-tox - rcx, -toy - rcy, 0.0f);
        x += defx;
        y += defy;
        double screenWidth = (float)cam.offscreenWidth / 1920.0f;
        double screenHeight = (float)cam.offscreenHeight / 1920.0f;
        this.projection.setOrtho(-((float)screenWidth) / 2.0f, (float)screenWidth / 2.0f, -((float)screenHeight) / 2.0f, (float)screenHeight / 2.0f, -10.0f, 10.0f);
        this.modelview.scaling(Core.scale);
        this.modelview.scale((float)Core.tileScale / 2.0f);
        this.modelview.rotate(0.5235988f, 1.0f, 0.0f, 0.0f);
        this.modelview.rotate(2.3561945f, 0.0f, 1.0f, 0.0f);
        double difX = ox - x;
        double difY = oy - y;
        this.modelview.translate(-((float)difX), (oz - z) * 2.44949f, -((float)difY));
        this.modelview.rotate((float)Math.PI, 0.0f, 1.0f, 0.0f);
        this.modelview.translate(0.0f, -0.71999997f, 0.0f);
    }

    public void calculateFixForJigglyModels(float ox, float oy, float oz) {
        if (!DebugOptions.instance.fboRenderChunk.fixJigglyModels.getValue()) {
            this.fixJigglyModelsX = 0.0f;
            this.fixJigglyModelsY = 0.0f;
            this.fixJigglyModelsSquareX = 0.0f;
            this.fixJigglyModelsSquareY = 0.0f;
            return;
        }
        int wx = PZMath.fastfloor(ox / 8.0f);
        int wy = PZMath.fastfloor(oy / 8.0f);
        float chrZ = IsoCamera.frameState.calculateCameraZ(IsoCamera.getCameraCharacter());
        float x1 = IsoUtils.XToScreen(wx * 8, wy * 8, chrZ, 0);
        float y1 = IsoUtils.YToScreen(wx * 8, wy * 8, chrZ, 0);
        x1 -= this.getOffX();
        y1 -= this.getOffY();
        Vector3f v1 = this.worldToUI(wx * 8, wy * 8, chrZ, s_tempVector3f_1);
        float x2 = v1.x;
        float y2 = (float)IsoCamera.getScreenHeight(this.playerIndex) - v1.y;
        this.fixJigglyModelsX = x2 - (x1 /= this.zoom);
        this.fixJigglyModelsY = y2 - (y1 /= this.zoom);
        float jx = this.fixJigglyModelsX * this.zoom;
        float jy = this.fixJigglyModelsY * this.zoom;
        this.fixJigglyModelsSquareX = (jx + 2.0f * jy) / (64.0f * (float)Core.tileScale);
        this.fixJigglyModelsSquareY = (jx - 2.0f * jy) / (-64.0f * (float)Core.tileScale);
    }

    private Vector3f worldToUI(float worldX, float worldY, float worldZ, Vector3f result) {
        PlayerCamera camera = this;
        Matrix4f matrix4f = BaseVehicle.allocMatrix4f();
        matrix4f.set(camera.projection);
        matrix4f.mul(camera.modelview);
        PlayerCamera.s_viewport[2] = IsoCamera.getScreenWidth(this.playerIndex);
        PlayerCamera.s_viewport[3] = IsoCamera.getScreenHeight(this.playerIndex);
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        matrix4f.project(worldX - isoGameCharacter.getX(), (worldZ - isoGameCharacter.getZ()) * 3.0f * 0.8164967f, worldY - isoGameCharacter.getY(), s_viewport, result);
        BaseVehicle.releaseMatrix4f(matrix4f);
        return result;
    }
}

