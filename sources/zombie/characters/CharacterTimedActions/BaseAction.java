/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.CharacterTimedActions;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameTime;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.StateManager;
import zombie.characters.CharacterActionAnims;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.network.GameClient;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;

public class BaseAction {
    public long soundEffect = -1L;
    public float currentTime = -2.0f;
    public float lastTime = -1.0f;
    public int maxTime = 60;
    public float prevLastTime;
    public boolean useProgressBar = true;
    public boolean forceProgressBar;
    public IsoGameCharacter chr;
    public boolean stopOnWalk = true;
    public boolean stopOnRun = true;
    public boolean stopOnAim;
    public float caloriesModifier = 1.0f;
    public float delta;
    public boolean blockMovementEtc;
    public boolean overrideAnimation;
    public final ArrayList<String> animVariables = new ArrayList();
    public boolean loopAction;
    public boolean started;
    public boolean forceStop;
    public boolean forceComplete;
    public boolean waitForFinished;
    public boolean pathfinding;
    public boolean allowedWhileDraggingCorpses;
    private static final ArrayList<String> specificNetworkAnim = new ArrayList<String>(Arrays.asList("Reload", "Bandage", "Loot", "AttachItem", "Drink", "Eat", "Pour", "Read", "fill_container_tap", "drink_tap", "WearClothing"));
    private InventoryItem primaryHandItem;
    private InventoryItem secondaryHandItem;
    private String primaryHandMdl;
    private String secondaryHandMdl;
    public boolean overrideHandModels;

    public BaseAction(IsoGameCharacter chr) {
        this.chr = chr;
    }

    public void forceStop() {
        this.forceStop = true;
    }

    public void forceComplete() {
        this.forceComplete = true;
    }

    public boolean isForceComplete() {
        return this.forceComplete;
    }

    public void PlayLoopedSoundTillComplete(String name, int radius, float maxGain) {
        this.soundEffect = this.chr.getEmitter().playSound(name);
    }

    public boolean hasStalled() {
        if (!this.started) {
            return false;
        }
        return this.lastTime == this.currentTime && this.lastTime == this.prevLastTime && this.lastTime < 0.0f || this.currentTime < 0.0f;
    }

    public float getJobDelta() {
        return this.delta;
    }

    public void setJobDelta(float delta) {
        this.currentTime = (float)this.maxTime * delta;
        this.delta = delta;
    }

    public void setWaitForFinished(boolean val) {
        this.waitForFinished = val;
    }

    public void resetJobDelta() {
        this.delta = 0.0f;
        this.currentTime = 0.0f;
    }

    public void waitToStart() {
        if (this.chr.shouldWaitToStartTimedAction()) {
            return;
        }
        this.started = true;
        this.start();
    }

    public void update() {
        IsoPlayer isoPlayer;
        IsoGameCharacter isoGameCharacter;
        boolean bUseProgressBar;
        this.prevLastTime = this.lastTime;
        this.lastTime = this.currentTime;
        this.currentTime += GameTime.instance.getMultiplier();
        if (this.currentTime < 0.0f) {
            this.currentTime = 0.0f;
        }
        boolean bl = bUseProgressBar = (Core.getInstance().isOptionProgressBar() || this.forceProgressBar) && this.useProgressBar && (isoGameCharacter = this.chr) instanceof IsoPlayer && (isoPlayer = (IsoPlayer)isoGameCharacter).isLocalPlayer();
        if (this.maxTime == -1) {
            if (bUseProgressBar) {
                UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(Float.POSITIVE_INFINITY);
            }
            return;
        }
        this.delta = this.maxTime == 0 ? 0.0f : Math.min(this.currentTime / (float)this.maxTime, 1.0f);
        if (bUseProgressBar) {
            UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(this.delta);
        }
    }

    public void start() {
        this.forceComplete = false;
        this.forceStop = false;
        if (this.chr.isCurrentState(PlayerActionsState.instance())) {
            InventoryItem primaryItem = this.chr.getPrimaryHandItem();
            InventoryItem secondaryItem = this.chr.getSecondaryHandItem();
            this.chr.setHideWeaponModel(!(primaryItem instanceof HandWeapon) && !(secondaryItem instanceof HandWeapon));
        }
    }

    public void reset() {
        this.currentTime = 0.0f;
        this.forceComplete = false;
        this.forceStop = false;
    }

    public float getCurrentTime() {
        return this.currentTime;
    }

    public void stop() {
        UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(0.0f);
        if (this.soundEffect > -1L) {
            this.chr.getEmitter().stopSound(this.soundEffect);
            this.soundEffect = -1L;
        }
        this.stopTimedActionAnim();
    }

    public boolean valid() {
        return true;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean finished() {
        return !this.waitForFinished && this.currentTime >= (float)this.maxTime && this.maxTime != -1;
    }

    public void perform() {
        UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(1.0f);
        if (!this.loopAction) {
            this.stopTimedActionAnim();
        }
    }

    public void complete() {
    }

    public void setUseProgressBar(boolean use) {
        this.useProgressBar = use;
    }

    public void setBlockMovementEtc(boolean block) {
        this.blockMovementEtc = block;
    }

    public void setPathfinding(boolean b) {
        this.pathfinding = b;
    }

    public boolean isPathfinding() {
        return this.pathfinding;
    }

    public void setAllowedWhileDraggingCorpses(boolean val) {
        this.allowedWhileDraggingCorpses = val;
    }

    public boolean isAllowedWhileDraggingCorpses() {
        return this.allowedWhileDraggingCorpses;
    }

    public void setOverrideAnimation(boolean override) {
        this.overrideAnimation = override;
    }

    public void stopTimedActionAnim() {
        IsoPlayer player;
        IsoGameCharacter isoGameCharacter;
        for (int i = 0; i < this.animVariables.size(); ++i) {
            String key = this.animVariables.get(i);
            this.chr.clearVariable(key);
        }
        this.chr.setVariable("IsPerformingAnAction", false);
        if (this.overrideHandModels) {
            this.overrideHandModels = false;
            this.chr.resetEquippedHandsModels();
        }
        if (GameClient.client && (isoGameCharacter = this.chr) instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
            StateManager.exitSubState(this.chr, PlayerActionsState.instance());
        }
    }

    public void setAnimVariable(String key, String val) {
        if (!this.animVariables.contains(key)) {
            this.animVariables.add(key);
        }
        this.chr.setVariable(key, val);
    }

    public void setAnimVariable(String key, boolean val) {
        if (!this.animVariables.contains(key)) {
            this.animVariables.add(key);
        }
        this.chr.setVariable(key, String.valueOf(val));
    }

    public String getPrimaryHandMdl() {
        return this.primaryHandMdl;
    }

    public String getSecondaryHandMdl() {
        return this.secondaryHandMdl;
    }

    public InventoryItem getPrimaryHandItem() {
        return this.primaryHandItem;
    }

    public InventoryItem getSecondaryHandItem() {
        return this.secondaryHandItem;
    }

    public void setActionAnim(CharacterActionAnims act) {
        this.setActionAnim(act.toString());
    }

    public void setActionAnim(String animNode) {
        IsoPlayer player;
        IsoGameCharacter isoGameCharacter;
        this.setAnimVariable("PerformingAction", animNode);
        this.chr.setVariable("IsPerformingAnAction", true);
        if (Core.debug) {
            this.chr.advancedAnimator.printDebugCharacterActions(animNode);
        }
        if (GameClient.client && (isoGameCharacter = this.chr) instanceof IsoPlayer && (player = (IsoPlayer)isoGameCharacter).isLocalPlayer()) {
            StateManager.enterSubState(this.chr, PlayerActionsState.instance());
        }
    }

    public void setOverrideHandModels(InventoryItem primaryHand, InventoryItem secondaryHand) {
        this.setOverrideHandModels(primaryHand, secondaryHand, true);
    }

    public void setOverrideHandModels(InventoryItem primaryHand, InventoryItem secondaryHand, boolean resetModel) {
        this.setOverrideHandModelsObject(primaryHand, secondaryHand, resetModel);
    }

    public void setOverrideHandModelsString(String primaryHand, String secondaryHand) {
        this.setOverrideHandModelsString(primaryHand, secondaryHand, true);
    }

    public void setOverrideHandModelsString(String primaryHand, String secondaryHand, boolean resetModel) {
        this.setOverrideHandModelsObject(primaryHand, secondaryHand, resetModel);
    }

    public void setOverrideHandModelsObject(Object primaryHand, Object secondaryHand, boolean resetModel) {
        this.overrideHandModels = true;
        this.primaryHandItem = Type.tryCastTo(primaryHand, InventoryItem.class);
        this.secondaryHandItem = Type.tryCastTo(secondaryHand, InventoryItem.class);
        this.primaryHandMdl = StringUtils.discardNullOrWhitespace(Type.tryCastTo(primaryHand, String.class));
        this.secondaryHandMdl = StringUtils.discardNullOrWhitespace(Type.tryCastTo(secondaryHand, String.class));
        if (resetModel) {
            this.chr.resetEquippedHandsModels();
        }
    }

    public void overrideWeaponType() {
        WeaponType weaponType = WeaponType.getWeaponType(this.chr, this.primaryHandItem, this.secondaryHandItem);
        this.chr.setVariable("Weapon", weaponType.getType());
    }

    public void restoreWeaponType() {
        WeaponType weaponType = WeaponType.getWeaponType(this.chr);
        this.chr.setVariable("Weapon", weaponType.getType());
    }

    public void OnAnimEvent(AnimEvent event) {
    }

    public void setLoopedAction(boolean looped) {
        this.loopAction = looped;
    }

    public void getDeltaModifiers(MoveDeltaModifiers modifiers) {
    }
}

