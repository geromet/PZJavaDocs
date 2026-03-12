/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.CombatManager;
import zombie.Lua.LuaHookManager;
import zombie.WorldSoundManager;
import zombie.ai.states.SwipeStatePlayer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.scripting.objects.MoodleType;
import zombie.ui.UIManager;

public class IsoLivingCharacter
extends IsoGameCharacter {
    public float useChargeDelta;
    public final HandWeapon bareHands = (HandWeapon)InventoryItemFactory.CreateItem("Base.BareHands");
    private boolean doShove;
    public boolean collidedWithPushable;
    public IsoGameCharacter targetOnGround;

    public IsoLivingCharacter(IsoCell cell, float x, float y, float z) {
        super(cell, x, y, z);
        this.registerVariableCallbacks();
    }

    private void registerVariableCallbacks() {
        this.setVariable("bDoShove", this::isDoShove, (IAnimationVariableSource owner) -> "This character wants to perform a Shove attack.");
        this.setVariable("bDoStomp", this::isDoStomp, (IAnimationVariableSource owner) -> "This character wants to perform a Stomp attack.");
    }

    public boolean isCollidedWithPushableThisFrame() {
        return this.collidedWithPushable;
    }

    public boolean AttemptAttack(float chargeDelta) {
        HandWeapon handWeapon;
        InventoryItem inventoryItem = this.leftHandItem;
        HandWeapon leftHandItem = inventoryItem instanceof HandWeapon ? (handWeapon = (HandWeapon)inventoryItem) : this.bareHands;
        if (leftHandItem != this.bareHands && this instanceof IsoPlayer) {
            CombatManager.getInstance().calculateAttackVars(this);
            this.setDoShove(this.attackVars.doShove);
            this.setDoGrapple(this.attackVars.doGrapple);
            if (LuaHookManager.TriggerHook("Attack", this, Float.valueOf(chargeDelta), leftHandItem)) {
                return false;
            }
        }
        return this.DoAttack(chargeDelta);
    }

    public boolean DoAttack(float chargeDelta) {
        InventoryItem attackItem;
        if (this.isDead()) {
            return false;
        }
        if (this.leftHandItem != null && (attackItem = this.leftHandItem) instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)attackItem;
            this.setUseHandWeapon(handWeapon);
            if (this.useHandWeapon.getCondition() <= 0) {
                return false;
            }
            int moodleLevel = this.moodles.getMoodleLevel(MoodleType.ENDURANCE);
            if (this.useHandWeapon.isCantAttackWithLowestEndurance() && moodleLevel == 4) {
                return false;
            }
            int hitCount = 0;
            if (this.useHandWeapon.isRanged()) {
                this.setRecoilDelay(this.useHandWeapon.getRecoilDelay(this));
            }
            if (this instanceof IsoSurvivor && this.useHandWeapon.isRanged() && hitCount < this.useHandWeapon.getMaxHitCount()) {
                for (int n = 0; n < this.getCell().getObjectList().size(); ++n) {
                    IsoMovingObject obj = this.getCell().getObjectList().get(n);
                    if (obj == this || !obj.isShootable() || !this.IsAttackRange(obj.getX(), obj.getY(), obj.getZ())) continue;
                    float delta = 1.0f;
                    Vector2 oPos = new Vector2(this.getX(), this.getY());
                    Vector2 tPos = new Vector2(obj.getX(), obj.getY());
                    tPos.x -= oPos.x;
                    tPos.y -= oPos.y;
                    boolean bZero = false;
                    if (tPos.x == 0.0f && tPos.y == 0.0f) {
                        bZero = true;
                    }
                    Vector2 dir = this.getForwardDirection();
                    this.DirectionFromVector(dir);
                    tPos.normalize();
                    float dot = tPos.dot(dir);
                    if (bZero) {
                        dot = 1.0f;
                    }
                    if (dot > 1.0f) {
                        dot = 1.0f;
                    }
                    if (dot < -1.0f) {
                        dot = -1.0f;
                    }
                    if (dot >= this.useHandWeapon.getMinAngle() && dot <= this.useHandWeapon.getMaxAngle()) {
                        ++hitCount;
                    }
                    if (hitCount >= this.useHandWeapon.getMaxHitCount()) break;
                }
            }
            if (UIManager.getPicked() != null) {
                this.attackTargetSquare = UIManager.getPicked().square;
                IsoObject isoObject = UIManager.getPicked().tile;
                if (isoObject instanceof IsoMovingObject) {
                    IsoMovingObject isoMovingObject = (IsoMovingObject)isoObject;
                    this.attackTargetSquare = isoMovingObject.getCurrentSquare();
                }
            }
            if (this.useHandWeapon.getAmmoType() != null && !this.inventory.contains(this.useHandWeapon.getAmmoType().getItemKey())) {
                return false;
            }
            if (this.useHandWeapon.getOtherHandRequire() != null && (this.rightHandItem == null || !this.rightHandItem.getType().equals(this.useHandWeapon.getOtherHandRequire()) && !this.rightHandItem.hasTag(this.useHandWeapon.getOtherHandRequire()) || this.rightHandItem.getCurrentUses() == 0)) {
                return false;
            }
            if (!this.useHandWeapon.isRanged()) {
                this.getEmitter().playSound(this.useHandWeapon.getSwingSound(), this);
                WorldSoundManager.instance.addSound(this, this.getXi(), this.getYi(), this.getZi(), this.useHandWeapon.getSoundRadius(), this.useHandWeapon.getSoundVolume());
            }
            this.changeState(SwipeStatePlayer.instance());
            if (this.useHandWeapon.getAmmoType() != null) {
                if (this instanceof IsoPlayer) {
                    IsoPlayer.getInstance().inventory.RemoveOneOf(this.useHandWeapon.getAmmoType().getItemKey());
                } else {
                    this.inventory.RemoveOneOf(this.useHandWeapon.getAmmoType().getItemKey());
                }
            }
            if (this.useHandWeapon.isUseSelf() && this.leftHandItem != null) {
                this.leftHandItem.Use();
            }
            if (this.useHandWeapon.isOtherHandUse() && this.rightHandItem != null) {
                this.rightHandItem.Use();
            }
            return true;
        }
        return false;
    }

    public boolean isDoShove() {
        return this.doShove;
    }

    public void setDoShove(boolean bDoShove) {
        this.doShove = bDoShove;
    }

    @Override
    public boolean isShoving() {
        return this.isDoShove() && !this.isDoStomp();
    }

    @Override
    public boolean isDoStomp() {
        return this.isDoShove() && this.isAimAtFloor();
    }

    @Override
    public HandWeapon getAttackingWeapon() {
        if (this.isDoHandToHandAttack()) {
            return this.bareHands;
        }
        HandWeapon weapon = this.getUseHandWeapon();
        if (weapon != null) {
            return weapon;
        }
        InventoryItem attackItem = this.getPrimaryHandItem();
        if (!(attackItem instanceof HandWeapon)) {
            return this.bareHands;
        }
        HandWeapon attackWeapon = (HandWeapon)attackItem;
        return attackWeapon;
    }

    public void clearHandToHandAttack() {
        this.setDoShove(false);
        this.setDoGrapple(false);
    }

    public boolean isDoHandToHandAttack() {
        return this.isDoShove() || this.isDoGrapple();
    }

    public boolean isShovingWhileAiming() {
        return this.isAiming() && this.isShoving();
    }

    public boolean isGrapplingWhileAiming() {
        return this.isAiming() && this.isDoGrapple();
    }

    @Override
    protected boolean isPrimaryHandModelReady() {
        if (this.useHandWeapon == null) {
            this.useHandWeapon = this.bareHands;
        }
        if (this.useHandWeapon.isBareHands()) {
            return true;
        }
        return this.primaryHandModel != null && this.primaryHandModel.model != null && this.primaryHandModel.model.isReady();
    }
}

