/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.sprite;

import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.input.AimingReticle;
import zombie.iso.IsoCamera;

public final class IsoCursor {
    private static IsoCursor instance;
    IsoCursorShader shader;

    public static IsoCursor getInstance() {
        if (instance == null) {
            instance = new IsoCursor();
        }
        return instance;
    }

    private IsoCursor() {
        RenderThread.invokeOnRenderContext(this::createShader);
        if (this.shader != null) {
            this.shader.textureCursor = Texture.getSharedTexture("media/ui/isocursor.png");
        }
    }

    private void createShader() {
        this.shader = new IsoCursorShader();
    }

    public void render(int playerIndex) {
        if (!Core.getInstance().displayCursor) {
            return;
        }
        if (Core.getInstance().getOffscreenBuffer() == null) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null || player.isDead() || !player.isAiming()) {
            return;
        }
        if (GameTime.isGamePaused()) {
            return;
        }
        if (this.shader == null || !this.shader.isCompiled()) {
            return;
        }
        float zoom = 1.0f / Core.getInstance().getZoom(playerIndex);
        int width = (int)((float)(this.shader.textureCursor.getWidth() * Core.tileScale) / 2.0f * zoom);
        int height = (int)((float)(this.shader.textureCursor.getHeight() * Core.tileScale) / 2.0f * zoom);
        this.shader.screenX = AimingReticle.getXA(playerIndex) - width / 2;
        this.shader.screenY = AimingReticle.getYA(playerIndex) - height / 2;
        this.shader.setWidth(width);
        this.shader.setHeight(height);
        int screenX = IsoCamera.getScreenLeft(playerIndex);
        int screenY = IsoCamera.getScreenTop(playerIndex);
        int screenW = IsoCamera.getScreenWidth(playerIndex);
        int screenH = IsoCamera.getScreenHeight(playerIndex);
        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.StartShader(this.shader.getID(), playerIndex);
        SpriteRenderer.instance.renderClamped(this.shader.textureCursor, this.shader.screenX, this.shader.screenY, width, height, screenX, screenY, screenW, screenH, 1.0f, 1.0f, 1.0f, 1.0f, this.shader);
        SpriteRenderer.instance.EndShader();
    }

    private static class IsoCursorShader
    extends Shader
    implements Consumer<TextureDraw> {
        private float alpha = 1.0f;
        private Texture textureCursor;
        private Texture textureWorld;
        private int screenX;
        private int screenY;

        IsoCursorShader() {
            super("isocursor");
        }

        @Override
        public void startMainThread(TextureDraw texd, int playerIndex) {
            this.alpha = Core.getInstance().getIsoCursorAlpha();
            this.textureWorld = Core.getInstance().offscreenBuffer.getTexture(playerIndex);
        }

        @Override
        public void startRenderThread(TextureDraw texd) {
            this.getProgram().setValue("u_alpha", this.alpha);
            this.getProgram().setValue("TextureCursor", this.textureCursor, 0);
            this.getProgram().setValue("TextureBackground", this.textureWorld, 1);
            SpriteRenderer.ringBuffer.shaderChangedTexture1();
            GL11.glEnable(3042);
        }

        @Override
        public void accept(TextureDraw textureDraw) {
            boolean playerIndex = false;
            int dx1 = (int)textureDraw.x0 - this.screenX;
            int dy1 = (int)textureDraw.y0 - this.screenY;
            int dx2 = this.screenX + this.getWidth() - (int)textureDraw.x2;
            int dy2 = this.screenY + this.getHeight() - (int)textureDraw.y2;
            this.screenX += dx1;
            this.screenY += dy1;
            this.setWidth(this.getWidth() - (dx1 + dx2));
            this.setHeight(this.getHeight() - (dy1 + dy2));
            float worldWidth = this.textureWorld.getWidthHW();
            float worldHeight = this.textureWorld.getHeightHW();
            float screenY = IsoCamera.getScreenTop(0) + IsoCamera.getScreenHeight(0) - (this.screenY + this.getHeight());
            textureDraw.tex1 = this.textureWorld;
            textureDraw.tex1U0 = (float)this.screenX / worldWidth;
            textureDraw.tex1V3 = screenY / worldHeight;
            textureDraw.tex1U1 = (float)(this.screenX + this.getWidth()) / worldWidth;
            textureDraw.tex1V2 = screenY / worldHeight;
            textureDraw.tex1U2 = (float)(this.screenX + this.getWidth()) / worldWidth;
            textureDraw.tex1V1 = (screenY + (float)this.getHeight()) / worldHeight;
            textureDraw.tex1U3 = (float)this.screenX / worldWidth;
            textureDraw.tex1V0 = (screenY + (float)this.getHeight()) / worldHeight;
        }
    }
}

