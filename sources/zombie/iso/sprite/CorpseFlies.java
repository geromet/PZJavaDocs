/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import zombie.GameTime;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoUtils;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameServer;

public final class CorpseFlies {
    private static Texture texture;
    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 128;
    private static final int COLUMNS = 8;
    private static final int ROWS = 7;
    private static final int NUM_FRAMES = 56;
    private static float counter;
    private static int frame;

    public static void render(int squareX, int squareY, int squareZ) {
        int width;
        if (texture == null) {
            texture = Texture.getSharedTexture("media/textures/CorpseFlies.png");
        }
        if (texture == null || !texture.isReady()) {
            return;
        }
        int frame = (CorpseFlies.frame + squareX + squareY) % 56;
        int column = frame % 8;
        int row = frame / 8;
        float u1 = (float)(column * 128) / (float)texture.getWidth();
        float v1 = (float)(row * 128) / (float)texture.getHeight();
        float u2 = (float)((column + 1) * 128) / (float)texture.getWidth();
        float v2 = (float)((row + 1) * 128) / (float)texture.getHeight();
        if (IsoSprite.globalOffsetX == -1.0f) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }
        float screenX = IsoUtils.XToScreen((float)squareX + 0.5f, (float)squareY + 0.5f, squareZ, 0) + IsoSprite.globalOffsetX;
        float screenY = IsoUtils.YToScreen((float)squareX + 0.5f, (float)squareY + 0.5f, squareZ, 0) + IsoSprite.globalOffsetY;
        int squareWidth = 64;
        int height = width = 64 * Core.tileScale;
        screenX -= (float)(width / 2);
        screenY -= (float)(height + 16 * Core.tileScale);
        if (Core.debug) {
            // empty if block
        }
        SpriteRenderer.instance.StartShader(0, IsoCamera.frameState.playerIndex);
        TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)squareX + 0.75f), (float)((float)squareY + 0.75f), (float)((float)squareZ + 0.25f)).depthStart * 2.0f - 1.0f;
        SpriteRenderer.instance.render(texture, screenX, screenY, width, height, 1.0f, 1.0f, 1.0f, 1.0f, u1, v1, u2, v1, u2, v2, u1, v2);
    }

    public static void update() {
        if (GameServer.server) {
            return;
        }
        counter += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * 1000.0f;
        float fps = 20.0f;
        if (counter > 50.0f) {
            counter %= 50.0f;
            ++frame;
            frame %= 56;
        }
    }

    public static void Reset() {
        texture = null;
    }
}

