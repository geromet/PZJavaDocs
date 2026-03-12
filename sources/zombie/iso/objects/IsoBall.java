/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.objects;

import zombie.WorldSoundManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.network.GameClient;

public class IsoBall
extends IsoPhysicsObject {
    private HandWeapon weapon;
    private IsoGameCharacter character;
    private final int lastCheckX = 0;
    private final int lastCheckY = 0;

    @Override
    public String getObjectName() {
        return "MolotovCocktail";
    }

    public IsoBall(IsoCell cell) {
        super(cell);
    }

    public IsoBall(IsoCell cell, float x, float y, float z, float xvel, float yvel, HandWeapon weapon, IsoGameCharacter character) {
        super(cell);
        this.weapon = weapon;
        this.character = character;
        this.velX = xvel;
        this.velY = yvel;
        float randX = (float)Rand.Next(4000) / 10000.0f;
        float randY = (float)Rand.Next(4000) / 10000.0f;
        this.velX += (randX -= 0.2f);
        this.velY += (randY -= 0.2f);
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.offsetX = 0.0f;
        this.offsetY = 0.0f;
        this.terminalVelocity = -0.02f;
        Texture tex = this.sprite.LoadFrameExplicit(weapon.getTex().getName());
        if (tex != null) {
            this.sprite.animate = false;
            int scale = Core.tileScale;
            this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * scale, 16 * scale);
        }
        this.speedMod = 0.6f;
    }

    @Override
    public void collideGround() {
        this.Fall();
    }

    @Override
    public void collideWall() {
        this.Fall();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        if (Core.debug) {
            // empty if block
        }
    }

    void Fall() {
        IsoGameCharacter isoGameCharacter;
        this.getCurrentSquare().getMovingObjects().remove(this);
        this.getCell().Remove(this);
        if (!GameClient.client) {
            WorldSoundManager.instance.addSound(this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), 0, 600, 600);
        }
        if ((isoGameCharacter = this.character) instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)isoGameCharacter;
            if (isoPlayer.isLocalPlayer()) {
                this.square.AddWorldInventoryItem(this.weapon, Rand.Next(0.2f, 0.8f), Rand.Next(0.2f, 0.8f), 0.0f, true);
            }
        } else {
            DebugLog.General.error("IsoBall: character isn't instance of IsoPlayer");
        }
    }
}

