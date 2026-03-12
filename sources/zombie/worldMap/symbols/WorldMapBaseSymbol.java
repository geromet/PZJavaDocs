/*
 * Decompiled with CFR 0.152.
 */
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Faction;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;
import zombie.worldMap.symbols.DoublePoint;
import zombie.worldMap.symbols.DoublePointPool;
import zombie.worldMap.symbols.SymbolLayout;
import zombie.worldMap.symbols.SymbolSaveData;
import zombie.worldMap.symbols.WorldMapSymbolCollisions;
import zombie.worldMap.symbols.WorldMapSymbols;

public abstract class WorldMapBaseSymbol {
    public static final float DEFAULT_SCALE = 0.666f;
    public static final float DEFAULT_MIN_ZOOM = 0.0f;
    public static final float DEFAULT_MAX_ZOOM = 24.0f;
    WorldMapSymbols owner;
    WorldMapSymbolNetworkInfo networkInfo;
    float x;
    float y;
    float width;
    float height;
    float anchorX;
    float anchorY;
    float scale = 0.666f;
    float rotation;
    double cosA = 1.0;
    double sinA;
    boolean matchPerspective;
    boolean applyZoom = true;
    float minZoom = 0.0f;
    float maxZoom = 24.0f;
    float r;
    float g;
    float b;
    float a;
    boolean collide;
    boolean visible = true;
    boolean userDefined = true;
    protected static final ColorInfo s_tempColorInfo = new ColorInfo();

    public WorldMapBaseSymbol() {
    }

    public WorldMapBaseSymbol(WorldMapSymbols owner) {
        this.owner = owner;
    }

    public abstract WorldMapSymbols.WorldMapSymbolType getType();

    public void setNetworkInfo(WorldMapSymbolNetworkInfo info) {
        this.networkInfo = info;
    }

    public WorldMapSymbolNetworkInfo getNetworkInfo() {
        return this.networkInfo;
    }

    public void setPrivate() {
        this.networkInfo = null;
    }

    public boolean isShared() {
        return this.getNetworkInfo() != null;
    }

    public boolean isPrivate() {
        return this.getNetworkInfo() == null;
    }

    public boolean isAuthorLocalPlayer() {
        if (GameServer.server || !GameClient.client) {
            throw new IllegalStateException("not client");
        }
        if (!this.isShared()) {
            return false;
        }
        return StringUtils.equals(GameClient.username, this.getNetworkInfo().getAuthor());
    }

    public boolean isUserDefined() {
        return this.userDefined;
    }

    public void setUserDefined(boolean b) {
        this.userDefined = b;
    }

    public void setAnchor(float x, float y) {
        this.anchorX = PZMath.clamp(x, 0.0f, 1.0f);
        this.anchorY = PZMath.clamp(y, 0.0f, 1.0f);
    }

    public float getAnchorX() {
        return this.anchorX;
    }

    public float getAnchorY() {
        return this.anchorY;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float degrees) {
        this.rotation = PZMath.wrap(degrees, 0.0f, 360.0f);
        if (this.rotation > 0.0f && this.rotation < 360.0f) {
            this.cosA = Math.cos(this.rotation * ((float)Math.PI / 180));
            this.sinA = Math.sin(this.rotation * ((float)Math.PI / 180));
        } else {
            this.cosA = 1.0;
            this.sinA = 0.0;
        }
    }

    public double getCosA() {
        return this.cosA;
    }

    public double getSinA() {
        return this.sinA;
    }

    public boolean isMatchPerspective() {
        return this.matchPerspective;
    }

    public void setMatchPerspective(boolean b) {
        this.matchPerspective = b;
    }

    public boolean isApplyZoom() {
        return this.applyZoom;
    }

    public void setApplyZoom(boolean b) {
        this.applyZoom = b;
    }

    public float getMinZoom() {
        return this.minZoom;
    }

    public void setMinZoom(float zoomF) {
        this.minZoom = zoomF;
    }

    public float getMaxZoom() {
        return this.maxZoom;
    }

    public void setMaxZoom(float zoomF) {
        this.maxZoom = zoomF;
    }

    public void setCollide(boolean collide) {
        this.collide = collide;
    }

    public void setRGBA(float r, float g, float b, float a) {
        this.r = PZMath.clamp_01(r);
        this.g = PZMath.clamp_01(g);
        this.b = PZMath.clamp_01(b);
        this.a = PZMath.clamp_01(a);
    }

    boolean hasCustomColor() {
        return true;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    protected float getLayoutWorldScale(WorldMapRenderer.Drawer drawer) {
        return drawer.worldScale;
    }

    public float getDisplayScale(UIWorldMap ui) {
        float scale;
        float f = scale = this.scale > 0.0f ? this.scale : 1.0f;
        if (this.isApplyZoom()) {
            if (ui.getSymbolsLayoutData().getMiniMapSymbols()) {
                return PZMath.min(ui.getSymbolsLayoutData().getWorldScale(), 1.0f);
            }
            return ui.getSymbolsLayoutData().getWorldScale() * scale;
        }
        return scale;
    }

    public float getDisplayScale(WorldMapRenderer.Drawer drawer) {
        float scale;
        float f = scale = this.scale > 0.0f ? this.scale : 1.0f;
        if (this.isApplyZoom()) {
            if (drawer.symbolsRenderData.miniMapSymbols) {
                return PZMath.min(this.getLayoutWorldScale(drawer), 1.0f);
            }
            return this.getLayoutWorldScale(drawer) * scale;
        }
        return scale;
    }

    public void layout(UIWorldMap ui, WorldMapSymbolCollisions collisions, float rox, float roy, SymbolLayout layout) {
        float x = ui.getAPI().worldToUIX(this.x, this.y) - rox;
        float y = ui.getAPI().worldToUIY(this.x, this.y) - roy;
        layout.x = x - this.widthScaled(ui) * this.anchorX;
        layout.y = y - this.heightScaled(ui) * this.anchorY;
        layout.collided = collisions.addBox(layout.x, layout.y, this.widthScaled(ui), this.heightScaled(ui), this.collide);
        if (layout.collided) {
            // empty if block
        }
    }

    public float widthScaled(UIWorldMap ui) {
        return this.width * this.getDisplayScale(ui);
    }

    public float heightScaled(UIWorldMap ui) {
        return this.height * this.getDisplayScale(ui);
    }

    public float widthScaled(WorldMapRenderer.Drawer drawer) {
        return this.width * this.getDisplayScale(drawer);
    }

    public float heightScaled(WorldMapRenderer.Drawer drawer) {
        return this.height * this.getDisplayScale(drawer);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible(UIWorldMap ui) {
        if (!this.isUserDefined() && !ui.getAPI().getRenderer().getBoolean("PlaceNames")) {
            return false;
        }
        if (this.isShared() && !this.getNetworkInfo().isVisibleToEveryone() && !this.isAuthorLocalPlayer()) {
            boolean visible = false;
            if (this.getNetworkInfo().isVisibleToFaction() && Faction.getPlayerFaction(GameClient.username) != null && Faction.getPlayerFaction(GameClient.username) == Faction.getPlayerFaction(this.getNetworkInfo().getAuthor())) {
                visible = true;
            }
            if (this.getNetworkInfo().isVisibleToSafehouse() && SafeHouse.hasSafehouse(GameClient.username) != null && SafeHouse.hasSafehouse(GameClient.username) == SafeHouse.hasSafehouse(this.getNetworkInfo().getAuthor())) {
                visible = true;
            }
            if (this.getNetworkInfo().hasPlayer(GameClient.username)) {
                visible = true;
            }
            if (!visible) {
                return false;
            }
        }
        return this.visible;
    }

    public boolean isOnScreen(UIWorldMap ui) {
        SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(this);
        float uiX = ui.getAPI().worldOriginX() + layout.x;
        float uiY = ui.getAPI().worldOriginY() + layout.y;
        return !(uiX + this.widthScaled(ui) <= 0.0f || (double)uiX >= ui.getWidth() || uiY + this.heightScaled(ui) <= 0.0f) && !((double)uiY >= ui.getHeight());
    }

    public void save(ByteBuffer output, SymbolSaveData saveData) throws IOException {
        output.putFloat(this.x);
        output.putFloat(this.y);
        output.putFloat(this.anchorX);
        output.putFloat(this.anchorY);
        output.putFloat(this.scale);
        output.putFloat(this.rotation);
        output.put((byte)(this.r * 255.0f));
        output.put((byte)(this.g * 255.0f));
        output.put((byte)(this.b * 255.0f));
        output.put((byte)(this.a * 255.0f));
        output.put(this.collide ? (byte)1 : 0);
        byte flags = 0;
        if (this.matchPerspective) {
            flags = (byte)(flags | 1);
        }
        if (this.applyZoom) {
            flags = (byte)(flags | 2);
        }
        if (this.minZoom != 0.0f) {
            flags = (byte)(flags | 4);
        }
        if (this.maxZoom != 24.0f) {
            flags = (byte)(flags | 8);
        }
        output.put(flags);
        if (this.minZoom != 0.0f) {
            output.putFloat(this.minZoom);
        }
        if (this.maxZoom != 24.0f) {
            output.putFloat(this.maxZoom);
        }
    }

    public void load(ByteBuffer input, SymbolSaveData saveData) throws IOException {
        this.x = input.getFloat();
        this.y = input.getFloat();
        this.anchorX = input.getFloat();
        this.anchorY = input.getFloat();
        this.scale = input.getFloat();
        if (saveData.symbolsVersion >= 2) {
            this.setRotation(input.getFloat());
        }
        this.r = (float)(input.get() & 0xFF) / 255.0f;
        this.g = (float)(input.get() & 0xFF) / 255.0f;
        this.b = (float)(input.get() & 0xFF) / 255.0f;
        this.a = (float)(input.get() & 0xFF) / 255.0f;
        boolean bl = this.collide = input.get() != 0;
        if (saveData.symbolsVersion >= 2) {
            int flags = input.get() & 0xFF;
            this.matchPerspective = (flags & 1) != 0;
            boolean bl2 = this.applyZoom = (flags & 2) != 0;
            if ((flags & 4) != 0) {
                this.minZoom = PZMath.clamp(input.getFloat(), 0.0f, 24.0f);
            }
            if ((flags & 8) != 0) {
                this.maxZoom = PZMath.clamp(input.getFloat(), 0.0f, 24.0f);
            }
        }
    }

    public abstract void render(WorldMapRenderer.Drawer var1);

    void renderCollided(WorldMapRenderer.Drawer drawer) {
        SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
        float x = drawer.worldOriginUiX + layout.x + this.widthScaled(drawer) / 2.0f;
        float y = drawer.worldOriginUiY + layout.y + this.heightScaled(drawer) / 2.0f;
    }

    void renderOutline(UIWorldMap ui, float r, float g, float b, float a, float thickness) {
        DoublePoint leftTop = DoublePointPool.alloc();
        DoublePoint rightTop = DoublePointPool.alloc();
        DoublePoint rightBottom = DoublePointPool.alloc();
        DoublePoint leftBottom = DoublePointPool.alloc();
        this.getOutlinePoints(ui, leftTop, rightTop, rightBottom, leftBottom);
        ui.DrawLine(null, leftTop.x, leftTop.y, rightTop.x, rightTop.y, thickness /= 2.0f, r, g, b, a);
        ui.DrawLine(null, rightTop.x, rightTop.y, rightBottom.x, rightBottom.y, thickness, r, g, b, a);
        ui.DrawLine(null, leftBottom.x, leftBottom.y, rightBottom.x, rightBottom.y, thickness, r, g, b, a);
        ui.DrawLine(null, leftTop.x, leftTop.y, leftBottom.x, leftBottom.y, thickness, r, g, b, a);
        DoublePointPool.release(leftTop);
        DoublePointPool.release(rightTop);
        DoublePointPool.release(rightBottom);
        DoublePointPool.release(leftBottom);
    }

    void getOutlinePoints(UIWorldMap ui, DoublePoint leftTop, DoublePoint rightTop, DoublePoint rightBottom, DoublePoint leftBottom) {
        float rox = ui.getAPI().worldOriginX();
        float roy = ui.getAPI().worldOriginY();
        float scale = this.getDisplayScale(ui);
        SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(this);
        double pointOfRotationX = rox + layout.x + this.width * this.anchorX * scale;
        double pointOfRotationY = roy + layout.y + this.height * this.anchorY * scale;
        if (this.isMatchPerspective()) {
            scale /= ui.getSymbolsLayoutData().getWorldScale();
            pointOfRotationX = this.x;
            pointOfRotationY = this.y;
        }
        double cosA = this.getCosA();
        double sinA = this.getSinA();
        WorldMapBaseSymbol.getAbsolutePosition(0.0f - this.width * this.anchorX, 0.0f - this.height * this.anchorY, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftTop);
        WorldMapBaseSymbol.getAbsolutePosition(this.width * (1.0f - this.anchorX), 0.0f - this.height * this.anchorY, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightTop);
        WorldMapBaseSymbol.getAbsolutePosition(this.width * (1.0f - this.anchorX), this.height * (1.0f - this.anchorY), pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightBottom);
        WorldMapBaseSymbol.getAbsolutePosition(0.0f - this.width * this.anchorX, this.height * (1.0f - this.anchorY), pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftBottom);
        if (this.isMatchPerspective()) {
            this.worldToUI(ui, leftTop);
            this.worldToUI(ui, rightTop);
            this.worldToUI(ui, rightBottom);
            this.worldToUI(ui, leftBottom);
        }
    }

    void getOutlinePoints(WorldMapRenderer.Drawer drawer, DoublePoint leftTop, DoublePoint rightTop, DoublePoint rightBottom, DoublePoint leftBottom) {
        float rox = drawer.worldOriginUiX;
        float roy = drawer.worldOriginUiY;
        float scale = this.getDisplayScale(drawer);
        SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
        double pointOfRotationX = rox + layout.x + this.width * this.anchorX * scale;
        double pointOfRotationY = roy + layout.y + this.height * this.anchorY * scale;
        if (this.isMatchPerspective()) {
            scale /= this.getLayoutWorldScale(drawer);
            pointOfRotationX = this.x;
            pointOfRotationY = this.y;
        }
        double cosA = this.getCosA();
        double sinA = this.getSinA();
        WorldMapBaseSymbol.getAbsolutePosition(0.0f - this.width * this.anchorX, 0.0f - this.height * this.anchorY, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftTop);
        WorldMapBaseSymbol.getAbsolutePosition(this.width * (1.0f - this.anchorX), 0.0f - this.height * this.anchorY, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightTop);
        WorldMapBaseSymbol.getAbsolutePosition(this.width * (1.0f - this.anchorX), this.height * (1.0f - this.anchorY), pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightBottom);
        WorldMapBaseSymbol.getAbsolutePosition(0.0f - this.width * this.anchorX, this.height * (1.0f - this.anchorY), pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftBottom);
        if (this.isMatchPerspective()) {
            this.worldToUI(drawer, leftTop);
            this.worldToUI(drawer, rightTop);
            this.worldToUI(drawer, rightBottom);
            this.worldToUI(drawer, leftBottom);
        }
    }

    ColorInfo getColor(WorldMapRenderer.Drawer drawer, ColorInfo colorInfo) {
        if (this.isUserDefined() && this.isPrivate() && drawer.renderer.isDimUnsharedSymbols()) {
            return colorInfo.set(0.25f, 0.25f, 0.25f, 0.5f);
        }
        if (this.getMinZoom() > drawer.zoomF || this.getMaxZoom() < drawer.zoomF) {
            float alpha;
            if (drawer.symbolsRenderData.mapEditor) {
                alpha = 0.25f;
            } else if (drawer.symbolsRenderData.userEditing && this.isUserDefined()) {
                alpha = 0.25f;
            } else {
                float frac = 0.0f;
                if (this.getMinZoom() > drawer.zoomF) {
                    frac = PZMath.min(this.getMinZoom() - drawer.zoomF, 0.5f) / 0.5f;
                }
                if (this.getMaxZoom() < drawer.zoomF) {
                    frac = PZMath.min(drawer.zoomF - this.getMaxZoom(), 0.5f) / 0.5f;
                }
                alpha = PZMath.lerp(1.0f, 0.0f, frac);
            }
            return colorInfo.set(this.r, this.g, this.b, alpha);
        }
        return colorInfo.set(this.r, this.g, this.b, this.a);
    }

    static DoublePoint getAbsolutePosition(double localX, double localY, double pointOfRotationX, double pointOfRotationY, double cosA, double sinA, double scaleX, double scaleY, DoublePoint out) {
        out.x = pointOfRotationX + (localX *= scaleX) * cosA + (localY *= scaleY) * sinA;
        out.y = pointOfRotationY - localX * sinA + localY * cosA;
        return out;
    }

    DoublePoint toLocalCoordinates(double relativeToPointOfRotationX, double relativeToPointOfRotationY, double cosA, double sinA, double scaleX, double scaleY, DoublePoint out) {
        double lx = relativeToPointOfRotationX * cosA - relativeToPointOfRotationY * sinA;
        double ly = relativeToPointOfRotationX * sinA + relativeToPointOfRotationY * cosA;
        out.x = (double)(this.width * this.anchorX) + lx / scaleX;
        out.y = (double)(this.height * this.anchorY) + ly / scaleY;
        return out;
    }

    void worldToUI(UIWorldMap ui, DoublePoint pos) {
        double x = ui.getAPI().worldToUIX((float)pos.x, (float)pos.y);
        double y = ui.getAPI().worldToUIY((float)pos.x, (float)pos.y);
        pos.set(x, y);
    }

    void worldToUI(WorldMapRenderer.Drawer drawer, DoublePoint pos) {
        double x = drawer.worldToUIX((float)pos.x, (float)pos.y);
        double y = drawer.worldToUIY((float)pos.x, (float)pos.y);
        pos.set(x, y);
    }

    abstract WorldMapBaseSymbol createCopy();

    protected WorldMapBaseSymbol initCopy(WorldMapBaseSymbol original) {
        this.owner = original.owner;
        this.x = original.x;
        this.y = original.y;
        this.width = original.width;
        this.height = original.height;
        this.anchorX = original.anchorX;
        this.anchorY = original.anchorY;
        this.scale = original.scale;
        this.rotation = original.rotation;
        this.cosA = original.cosA;
        this.sinA = original.sinA;
        this.matchPerspective = original.matchPerspective;
        this.applyZoom = original.applyZoom;
        this.minZoom = original.minZoom;
        this.maxZoom = original.maxZoom;
        this.r = original.r;
        this.g = original.g;
        this.b = original.b;
        this.a = original.a;
        this.collide = original.collide;
        this.visible = original.visible;
        this.userDefined = original.userDefined;
        return this;
    }

    public abstract void release();
}

