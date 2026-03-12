/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.util.ArrayList;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.core.utils.ImageUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoCursor;
import zombie.iso.sprite.IsoReticle;
import zombie.network.GameServer;
import zombie.network.ServerGUI;

public final class MultiTextureFBO2 {
    private final float[] zoomLevelsDefault = new float[]{2.5f, 2.25f, 2.0f, 1.75f, 1.5f, 1.25f, 1.0f, 0.75f, 0.5f, 0.25f};
    private float[] zoomLevels;
    public TextureFBO current;
    public volatile TextureFBO fboRendered;
    private final float[] zoom = new float[4];
    private final float[] targetZoom = new float[4];
    private final float[] startZoom = new float[4];
    private float zoomedInLevel;
    private float zoomedOutLevel;
    public final boolean[] autoZoom = new boolean[4];
    public boolean zoomEnabled = true;

    public MultiTextureFBO2() {
        for (int n = 0; n < 4; ++n) {
            this.startZoom[n] = 1.0f;
            this.targetZoom[n] = 1.0f;
            this.zoom[n] = 1.0f;
        }
    }

    public int getWidth(int playerIndex) {
        return (int)((float)IsoCamera.getScreenWidth(playerIndex) * this.getDisplayZoom(playerIndex) * ((float)Core.tileScale / 2.0f));
    }

    public int getHeight(int playerIndex) {
        return (int)((float)IsoCamera.getScreenHeight(playerIndex) * this.getDisplayZoom(playerIndex) * ((float)Core.tileScale / 2.0f));
    }

    public void setZoom(int playerIndex, float value) {
        this.zoom[playerIndex] = value;
    }

    public void setZoomAndTargetZoom(int playerIndex, float value) {
        this.zoom[playerIndex] = value;
        this.targetZoom[playerIndex] = value;
    }

    public float getZoom(int playerIndex) {
        return this.zoom[playerIndex];
    }

    public float getTargetZoom(int playerIndex) {
        return this.targetZoom[playerIndex];
    }

    public float getDisplayZoom(int playerIndex) {
        if ((float)Core.width > Core.initialWidth) {
            return this.zoom[playerIndex] * (Core.initialWidth / (float)Core.width);
        }
        return this.zoom[playerIndex];
    }

    public void setTargetZoom(int playerIndex, float target) {
        if (this.targetZoom[playerIndex] != target) {
            this.targetZoom[playerIndex] = target;
            this.startZoom[playerIndex] = this.zoom[playerIndex];
        }
    }

    public ArrayList<Integer> getDefaultZoomLevels() {
        ArrayList<Integer> percents = new ArrayList<Integer>();
        float[] levels = this.zoomLevelsDefault;
        for (int i = 0; i < levels.length; ++i) {
            percents.add(Math.round(levels[i] * 100.0f));
        }
        return percents;
    }

    public void setZoomLevels(Double ... zooms) {
        this.zoomLevels = new float[zooms.length];
        for (int i = 0; i < zooms.length; ++i) {
            this.zoomLevels[i] = zooms[i].floatValue();
        }
    }

    public void setZoomLevelsFromOption(String levels) {
        this.zoomLevels = this.zoomLevelsDefault;
        if (levels == null || levels.isEmpty()) {
            return;
        }
        String[] ss = levels.split(";");
        if (ss.length == 0) {
            return;
        }
        ArrayList<Integer> percents = new ArrayList<Integer>();
        block2: for (String s : ss) {
            if (s.isEmpty()) continue;
            try {
                int percent = Integer.parseInt(s);
                for (float knownLevel : this.zoomLevels) {
                    if (Math.round(knownLevel * 100.0f) != percent) continue;
                    if (percents.contains(percent)) continue block2;
                    percents.add(percent);
                    continue block2;
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        if (!percents.contains(100)) {
            percents.add(100);
        }
        percents.sort((o1, o2) -> o2 - o1);
        this.zoomLevels = new float[percents.size()];
        for (int i = 0; i < percents.size(); ++i) {
            int playerIndex = IsoPlayer.getPlayerIndex();
            this.zoomLevels[i] = (float)((Integer)percents.get(i)).intValue() / 100.0f;
        }
    }

    public void destroy() {
        if (this.current == null) {
            return;
        }
        this.current.destroy();
        this.current = null;
        this.fboRendered = null;
        for (int n = 0; n < 4; ++n) {
            this.targetZoom[n] = 1.0f;
            this.zoom[n] = 1.0f;
        }
    }

    public void create(int xres, int yres) throws Exception {
        if (!this.zoomEnabled) {
            return;
        }
        if (this.zoomLevels == null) {
            this.zoomLevels = this.zoomLevelsDefault;
        }
        this.zoomedInLevel = this.zoomLevels[this.zoomLevels.length - 1];
        this.zoomedOutLevel = this.zoomLevels[0];
        int x = ImageUtils.getNextPowerOfTwoHW(xres);
        int y = ImageUtils.getNextPowerOfTwoHW(yres);
        this.current = this.createTexture(x, y, false);
    }

    public void update() {
        int playerIndex = IsoPlayer.getPlayerIndex();
        if (!this.zoomEnabled) {
            this.targetZoom[playerIndex] = 1.0f;
            this.zoom[playerIndex] = 1.0f;
        }
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (this.autoZoom[playerIndex] && isoGameCharacter != null && this.zoomEnabled) {
            float dist = IsoUtils.DistanceTo(IsoCamera.getRightClickOffX(), IsoCamera.getRightClickOffY(), 0.0f, 0.0f);
            float delta = dist / 300.0f;
            if (delta > 1.0f) {
                delta = 1.0f;
            }
            float zoom = this.shouldAutoZoomIn() ? this.zoomedInLevel : this.zoomedOutLevel;
            if ((zoom += delta) > this.zoomLevels[0]) {
                zoom = this.zoomLevels[0];
            }
            if (isoGameCharacter.getVehicle() != null) {
                zoom = this.getMaxZoom();
            }
            this.setTargetZoom(playerIndex, zoom);
        }
        float step = 0.004f * GameTime.instance.getMultiplier() / GameTime.instance.getTrueMultiplier() * (Core.tileScale == 2 ? 1.5f : 1.5f);
        if (!this.autoZoom[playerIndex]) {
            step *= 5.0f;
        } else if (this.targetZoom[playerIndex] > this.zoom[playerIndex]) {
            step *= 1.0f;
        }
        if (this.targetZoom[playerIndex] > this.zoom[playerIndex]) {
            int n = playerIndex;
            this.zoom[n] = this.zoom[n] + step;
            IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 2.0f;
            if (this.zoom[playerIndex] > this.targetZoom[playerIndex] || Math.abs(this.zoom[playerIndex] - this.targetZoom[playerIndex]) < 0.001f) {
                this.zoom[playerIndex] = this.targetZoom[playerIndex];
            }
        }
        if (this.targetZoom[playerIndex] < this.zoom[playerIndex]) {
            int n = playerIndex;
            this.zoom[n] = this.zoom[n] - step;
            IsoPlayer.players[playerIndex].dirtyRecalcGridStackTime = 2.0f;
            if (this.zoom[playerIndex] < this.targetZoom[playerIndex] || Math.abs(this.zoom[playerIndex] - this.targetZoom[playerIndex]) < 0.001f) {
                this.zoom[playerIndex] = this.targetZoom[playerIndex];
            }
        }
        this.setCameraToCentre();
    }

    private boolean shouldAutoZoomIn() {
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            return false;
        }
        IsoGridSquare square = isoGameCharacter.getCurrentSquare();
        if (square != null && !square.isOutside()) {
            return true;
        }
        if (!(isoGameCharacter instanceof IsoPlayer)) {
            return false;
        }
        IsoPlayer player = (IsoPlayer)isoGameCharacter;
        if (player.isRunning() || player.isSprinting()) {
            return false;
        }
        if (player.closestZombie < 6.0f && player.isTargetedByZombie()) {
            return true;
        }
        return player.lastTargeted < (float)(PerformanceSettings.getLockFPS() * 4);
    }

    private void setCameraToCentre() {
        PlayerCamera camera = IsoCamera.cameras[IsoPlayer.getPlayerIndex()];
        camera.center();
    }

    private TextureFBO createTexture(int x, int y, boolean test) {
        if (test) {
            Texture tex = new Texture(x, y, 16);
            TextureFBO newOne = new TextureFBO(tex);
            newOne.destroy();
            return null;
        }
        Texture tex = new Texture(x, y, 19);
        return new TextureFBO(tex);
    }

    public void render() {
        int playerIndex;
        if (this.current == null) {
            return;
        }
        int max = 0;
        for (playerIndex = 3; playerIndex >= 0; --playerIndex) {
            if (IsoPlayer.players[playerIndex] == null) continue;
            max = playerIndex > 1 ? 3 : playerIndex;
            break;
        }
        max = Math.max(max, IsoPlayer.numPlayers - 1);
        for (playerIndex = 0; playerIndex <= max; ++playerIndex) {
            if (SceneShaderStore.weatherShader != null && DebugOptions.instance.fboRenderChunk.useWeatherShader.getValue()) {
                IndieGL.StartShader(SceneShaderStore.weatherShader, playerIndex);
            }
            int sx = IsoCamera.getScreenLeft(playerIndex);
            int sy = IsoCamera.getScreenTop(playerIndex);
            int sw = IsoCamera.getScreenWidth(playerIndex);
            int sh = IsoCamera.getScreenHeight(playerIndex);
            if (!(IsoPlayer.players[playerIndex] != null || GameServer.server && ServerGUI.isCreated())) {
                SpriteRenderer.instance.renderi(null, sx, sy, sw, sh, 0.0f, 0.0f, 0.0f, 1.0f, null);
                continue;
            }
            ((Texture)this.current.getTexture()).rendershader2(sx, sy, sw, sh, sx, sy, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (SceneShaderStore.weatherShader != null) {
            IndieGL.EndShader();
        }
        switch (CombatManager.targetReticleMode) {
            case 1: {
                IsoReticle.getInstance().render(0);
                break;
            }
            default: {
                IsoCursor.getInstance().render(0);
            }
        }
    }

    public TextureFBO getCurrent(int nPlayer) {
        return this.current;
    }

    public Texture getTexture(int nPlayer) {
        return (Texture)this.current.getTexture();
    }

    public void doZoomScroll(int playerIndex, int del) {
        this.targetZoom[playerIndex] = this.getNextZoom(playerIndex, del);
    }

    public float getNextZoom(int playerIndex, int del) {
        block4: {
            block3: {
                if (!this.zoomEnabled || this.zoomLevels == null) {
                    return 1.0f;
                }
                if (del <= 0) break block3;
                for (int i = this.zoomLevels.length - 1; i > 0; --i) {
                    if (this.targetZoom[playerIndex] != this.zoomLevels[i]) continue;
                    return this.zoomLevels[i - 1];
                }
                break block4;
            }
            if (del >= 0) break block4;
            for (int i = 0; i < this.zoomLevels.length - 1; ++i) {
                if (this.targetZoom[playerIndex] != this.zoomLevels[i]) continue;
                return this.zoomLevels[i + 1];
            }
        }
        return this.targetZoom[playerIndex];
    }

    public float getMinZoom() {
        if (!this.zoomEnabled || this.zoomLevels == null || this.zoomLevels.length == 0) {
            return 1.0f;
        }
        return this.zoomLevels[this.zoomLevels.length - 1];
    }

    public float getMaxZoom() {
        if (!this.zoomEnabled || this.zoomLevels == null || this.zoomLevels.length == 0) {
            return 1.0f;
        }
        return this.zoomLevels[0];
    }

    public boolean test() {
        try {
            this.createTexture(16, 16, true);
        }
        catch (Exception ex) {
            DebugLog.General.error("Failed to create Test FBO");
            ex.printStackTrace();
            Core.safeMode = true;
            return false;
        }
        return true;
    }
}

