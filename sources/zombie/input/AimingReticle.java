/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.math.interpolators.LerpType;
import zombie.input.AimingMode;
import zombie.input.JoypadManager;
import zombie.input.Mouse;
import zombie.iso.Vector2;
import zombie.util.list.PZArrayUtil;

public class AimingReticle {
    private static final Vector2[] aimingPositions = PZArrayUtil.newInstance(Vector2.class, 4, Vector2::new);

    public static int getX(int playerIndex) {
        return (int)((float)AimingReticle.getXA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getY(int playerIndex) {
        return (int)((float)AimingReticle.getYA(playerIndex) * Core.getInstance().getZoom(playerIndex));
    }

    public static int getXA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        if (player == null || player.joypadBind < 0) {
            return Mouse.getXA();
        }
        return (int)AimingReticle.aimingPositions[playerIndex].x;
    }

    public static int getYA(int playerIndex) {
        IsoPlayer player = IsoPlayer.getPlayer(playerIndex);
        if (player == null || player.joypadBind < 0) {
            return Mouse.getYA();
        }
        return (int)AimingReticle.aimingPositions[playerIndex].y;
    }

    private static float axisToScreenX(float aimingX) {
        float screenHalfWidth = AimingReticle.getScreenHalfWidth();
        return screenHalfWidth * (1.0f + aimingX);
    }

    private static float axisToScreenY(float aimingY) {
        float screenHalfHeight = AimingReticle.getScreenHalfHeight();
        return screenHalfHeight * (1.0f + aimingY);
    }

    private static float renormalizeAimingAxisValue(float aimingAxisVal) {
        float aimingAxisMin = 0.3f;
        float aimingAxisMax = 0.9f;
        float aimingAxisSign = PZMath.sign(aimingAxisVal);
        float renormalizedAxis = PZMath.clamp((aimingAxisVal * aimingAxisSign - 0.3f) / 0.59999996f, 0.0f, 1.0f) * aimingAxisSign;
        float aimingMax = 0.8f;
        float aimingMin = 0.02f;
        return PZMath.lerp(0.02f, 0.8f, renormalizedAxis * aimingAxisSign, LerpType.EaseOutQuad) * aimingAxisSign;
    }

    private static float getAimingScreenX(IsoPlayer player) {
        float aimingAxisRaw = JoypadManager.instance.getAimingAxisX(player.joypadBind);
        float aimingAxis = AimingReticle.renormalizeAimingAxisValue(aimingAxisRaw);
        return AimingReticle.axisToScreenX(aimingAxis);
    }

    private static float getAimingScreenY(IsoPlayer player) {
        float aimingAxisRaw = JoypadManager.instance.getAimingAxisY(player.joypadBind);
        float aimingAxis = AimingReticle.renormalizeAimingAxisValue(aimingAxisRaw);
        return AimingReticle.axisToScreenY(aimingAxis);
    }

    private static float getScreenHalfWidth() {
        float screenWidth = Core.getInstance().getScreenWidth();
        return screenWidth / 2.0f;
    }

    private static float getScreenHalfHeight() {
        float screenHeight = Core.getInstance().getScreenHeight();
        return screenHeight / 2.0f;
    }

    public static void update() {
        float screenHalfWidth = AimingReticle.getScreenHalfWidth();
        float screenHalfHeight = AimingReticle.getScreenHalfHeight();
        for (int i = 0; i < aimingPositions.length; ++i) {
            IsoPlayer player = IsoPlayer.getPlayer(i);
            if (player == null || player.joypadBind <= -1) continue;
            AimingMode aimingMode = player.getAimingMode();
            AimingReticle.aimingPositions[i].x = aimingMode.lerpAxis(AimingReticle.aimingPositions[i].x, AimingReticle.getAimingScreenX(player), screenHalfWidth);
            AimingReticle.aimingPositions[i].y = aimingMode.lerpAxis(AimingReticle.aimingPositions[i].y, AimingReticle.getAimingScreenY(player), screenHalfHeight);
        }
    }
}

