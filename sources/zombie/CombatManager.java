/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.util.vector.Matrix4f;
import zombie.AttackType;
import zombie.AttackTypeModifier;
import zombie.EffectsManager;
import zombie.Lua.LuaEventManager;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.ai.states.AttackState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.FakeDeadZombieState;
import zombie.ai.states.FishingState;
import zombie.ai.states.PlayerHitReactionPVPState;
import zombie.ai.states.PlayerHitReactionState;
import zombie.ai.states.SwipeStatePlayer;
import zombie.ai.states.ZombieGetUpState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.audio.parameters.ParameterMeleeHitSurface;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.CharacterStat;
import zombie.characters.Faction;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.combat.CombatConfig;
import zombie.combat.CombatConfigKey;
import zombie.combat.HitReaction;
import zombie.combat.MeleeTargetComparator;
import zombie.combat.RangeTargetComparator;
import zombie.combat.Rect3D;
import zombie.combat.ShotDirection;
import zombie.combat.TargetComparator;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.BallisticsTarget;
import zombie.core.physics.RagdollBodyPart;
import zombie.core.physics.RagdollSettingsManager;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.textures.ColorInfo;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.debugWindows.TargetHitInfoPanel;
import zombie.entity.components.combat.Durability;
import zombie.entity.util.TimSort;
import zombie.input.AimingMode;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridSquareCollisionData;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.iso.Vector3;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.enums.MaterialType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.IsoZombieGiblets;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoReticle;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.fields.hit.AttackVars;
import zombie.network.fields.hit.HitInfo;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.VehicleScript;
import zombie.scripting.objects.WeaponCategory;
import zombie.statistics.StatisticCategory;
import zombie.statistics.StatisticType;
import zombie.statistics.StatisticsManager;
import zombie.ui.MoodlesUI;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public final class CombatManager {
    private final CombatConfig combatConfig = new CombatConfig();
    private static final ArrayList<HitInfo> HitList2 = new ArrayList();
    public final ObjectPool<HitInfo> hitInfoPool = new ObjectPool<HitInfo>(HitInfo::new);
    private static final int MINIMUM_WEAPON_LEVEL = 0;
    private static final int MAXIMUM_WEAPON_LEVEL = 10;
    private static final float BallisticsTargetsHighlightAlpha = 0.65f;
    private static final float MeleeTargetsHighlightAlpha = 1.0f;
    private static final float VehicleDamageScaleFactor = 50.0f;
    public static final int StrengthLevelOffset = 15;
    public static final float StrengthLevelMuscleStrainModifier = 10.0f;
    public static final float TwoHandedWeaponMuscleStrainModifier = 0.5f;
    private static final float PainThreshold = 10.0f;
    private static final float MinPainFactor = 1.0f;
    private static final float MaxPainFactor = 30.0f;
    private static final int StressLevelDamageReductionThreshold = 1;
    private static final int PanicLevelDamageReductionThreshold = 1;
    private static final float PanicLevelDamageSplitModifier = 0.1f;
    private static final float StressLevelDamageSplitModifier = 0.1f;
    private static final float MinBaseDamageSplitModifier = 0.1f;
    private static final float MinDamageSplit = 0.7f;
    private static final float MaxDamageSplit = 1.0f;
    private static final float StrengthPerkStompModifier = 0.2f;
    private static final float NoShoesDamageSplitModifier = 0.5f;
    private static final float EnduranceLevel1DamageSplitModifier = 0.5f;
    private static final float EnduranceLevel2DamageSplitModifier = 0.2f;
    private static final float EnduranceLevel3DamageSplitModifier = 0.1f;
    private static final float EnduranceLevel4DamageSplitModifier = 0.05f;
    private static final float TiredLevel1DamageSplitModifier = 0.5f;
    private static final float TiredLevel2DamageSplitModifier = 0.2f;
    private static final float TiredLevel3DamageSplitModifier = 0.1f;
    private static final float TiredLevel4DamageSplitModifier = 0.05f;
    private static final float BaseBodyPartClothingDefenseModifier = 0.5f;
    private static final int ZombieMaxDefense = 100;
    private static final int AxeVsTreeBonusModifier = 2;
    private static final float UseChargeDelta = 3.0f;
    private static final float CriticalHitSpeedMultiplier = 1.1f;
    private static final float BreakMultiplierBase = 1.0f;
    private static final float BreakMultiplierChargeModifier = 1.5f;
    private static final float MinAngleFloorModifier = 1.5f;
    private static final Integer PARAM_LOWER_CONDITION = 0;
    private static final Integer PARAM_ATTACKED = 1;
    public static final int ISOCURSOR = 0;
    public static final int ISORETICLE = 1;
    public static int targetReticleMode = 0;
    private boolean hitOnlyTree;
    private IsoTree treeHit;
    private IsoObject objHit;
    private final ArrayList<Float> dotList = new ArrayList();
    private static final Vector3 tempVector3_1 = new Vector3();
    private static final Vector3 tempVector3_2 = new Vector3();
    private static final Vector2 tempVector2_1 = new Vector2();
    private static final Vector2 tempVector2_2 = new Vector2();
    private final Vector4f tempVector4f = new Vector4f();
    private static final Vector3 tempVectorBonePos = new Vector3();
    private static final float DefaultMaintenanceXP = 1.0f;
    private static final int ConditionLowerChance = 10;
    private static final Vector3 ballisticsDirectionVector = new Vector3();
    private static final Vector3 ballisticsStartPosition = new Vector3();
    private static final Vector3 ballisticsEndPosition = new Vector3();
    private static final String BreakLightBulbSound = "SmashWindow";
    private static final Color OccludedTargetDebugColor = Color.white;
    private static final Color TargetableDebugColor = Color.green;
    private static final float TargetDebugAlpha = 1.0f;
    private static final float VehicleTargetDebugRadius = 1.5f;
    private static final float CharacterTargetDebugRadius = 0.1f;
    private final Rect3D rect0 = new Rect3D();
    private final Rect3D rect1 = new Rect3D();
    private final MeleeTargetComparator meleeTargetComparator = new MeleeTargetComparator();
    private final RangeTargetComparator rangeTargetComparator = new RangeTargetComparator();
    private final WindowVisitor windowVisitor = new WindowVisitor();
    private final TimSort timSort = new TimSort();

    public CombatConfig getCombatConfig() {
        return this.combatConfig;
    }

    public static CombatManager getInstance() {
        return Holder.instance;
    }

    private CombatManager() {
    }

    public float calculateDamageToVehicle(IsoGameCharacter isoGameCharacter, float vehicleDurability, float damage, int doorDamage) {
        if (vehicleDurability == 0.0f) {
            return doorDamage;
        }
        if (damage <= 0.0f || doorDamage == 0) {
            return 0.0f;
        }
        return damage * 50.0f * ((float)doorDamage / 10.0f) / vehicleDurability;
    }

    private void setParameterCharacterHitResult(IsoGameCharacter owner, IsoZombie zombie, long zombieHitSound) {
        if (zombieHitSound == 0L) {
            return;
        }
        int hitResult = 0;
        if (zombie != null) {
            if (zombie.isDead()) {
                hitResult = 2;
            } else if (zombie.isKnockedDown()) {
                hitResult = 1;
            }
        }
        owner.getEmitter().setParameterValue(zombieHitSound, FMODManager.instance.getParameterDescription("CharacterHitResult"), hitResult);
    }

    public HandWeapon getWeapon(IsoGameCharacter owner) {
        return owner.getAttackingWeapon();
    }

    private LosUtil.TestResults getResultLOS(IsoGridSquare square, IsoGameCharacter chr) {
        return LosUtil.lineClear(chr.getCell(), PZMath.fastfloor(square.getX()), PZMath.fastfloor(square.getY()), PZMath.fastfloor(square.getZ()), PZMath.fastfloor(chr.getX()), PZMath.fastfloor(chr.getY()), PZMath.fastfloor(chr.getZ()), false);
    }

    private boolean checkObjectHit(IsoGameCharacter owner, HandWeapon weapon, IsoGridSquare square, boolean north, boolean west) {
        if (square == null) {
            return false;
        }
        LosUtil.TestResults testResultsLOS = this.getResultLOS(square, owner);
        boolean canHitObject = testResultsLOS == LosUtil.TestResults.Clear;
        boolean canHitDoor = canHitObject || testResultsLOS == LosUtil.TestResults.ClearThroughClosedDoor;
        boolean canHitWindow = canHitObject || testResultsLOS == LosUtil.TestResults.ClearThroughWindow;
        boolean canHitBacksideOfWall = testResultsLOS == LosUtil.TestResults.Blocked && square.isAdjacentTo(owner.getSquare());
        for (int n = square.getSpecialObjects().size() - 1; n >= 0; --n) {
            boolean canHitOtherSide;
            Thumpable thumpable1;
            IsoObject special = square.getSpecialObjects().get(n);
            IsoDoor door = Type.tryCastTo(special, IsoDoor.class);
            IsoThumpable thumpable = Type.tryCastTo(special, IsoThumpable.class);
            IsoWindow window = Type.tryCastTo(special, IsoWindow.class);
            IsoCompost compost = Type.tryCastTo(special, IsoCompost.class);
            if (door != null && (north && door.north || west && !door.north)) {
                thumpable1 = door.getThumpableFor(owner);
                boolean bl = canHitOtherSide = owner.getCurrentSquare() == door.getOppositeSquare();
                if (thumpable1 != null && (canHitDoor || canHitOtherSide)) {
                    thumpable1.WeaponHit(owner, weapon);
                    this.objHit = door;
                    return true;
                }
            }
            if (thumpable != null) {
                if (thumpable.isDoor() || thumpable.isWindow() || thumpable.isWall() || !thumpable.isBlockAllTheSquare()) {
                    if (north && thumpable.north || west && !thumpable.north) {
                        thumpable1 = thumpable.getThumpableFor(owner);
                        boolean bl = canHitOtherSide = owner.getCurrentSquare() == thumpable.getOppositeSquare();
                        if (thumpable1 != null && (canHitDoor || canHitOtherSide || canHitWindow || thumpable.isWall() && canHitBacksideOfWall)) {
                            thumpable1.WeaponHit(owner, weapon);
                            this.objHit = thumpable;
                            return true;
                        }
                    }
                } else {
                    thumpable1 = thumpable.getThumpableFor(owner);
                    if (thumpable1 != null && canHitObject) {
                        thumpable1.WeaponHit(owner, weapon);
                        this.objHit = thumpable;
                        return true;
                    }
                }
            }
            if (window != null && (north && window.isNorth() || west && !window.isNorth()) && (thumpable1 = window.getThumpableFor(owner)) != null && canHitWindow) {
                thumpable1.WeaponHit(owner, weapon);
                this.objHit = window;
                return true;
            }
            if (compost == null || (thumpable1 = compost.getThumpableFor(owner)) == null || !canHitObject) continue;
            thumpable1.WeaponHit(owner, weapon);
            this.objHit = compost;
            return true;
        }
        return false;
    }

    private boolean CheckObjectHit(IsoGameCharacter owner, HandWeapon weapon) {
        IsoGridSquare sq;
        IsoGridSquare sq2;
        IsoGridSquare playerSq;
        IsoCell cell;
        IsoGridSquare next;
        this.objHit = null;
        this.treeHit = null;
        this.hitOnlyTree = false;
        if (owner.isAimAtFloor()) {
            return false;
        }
        boolean hit = false;
        int hitCount = 0;
        int hitTreeCount = 0;
        IsoDirections dir = IsoDirections.fromAngle(owner.getForwardDirection());
        int x = 0;
        int y = 0;
        if (dir == IsoDirections.NE || dir == IsoDirections.N || dir == IsoDirections.NW) {
            --y;
        }
        if (dir == IsoDirections.SE || dir == IsoDirections.S || dir == IsoDirections.SW) {
            ++y;
        }
        if (dir == IsoDirections.NW || dir == IsoDirections.W || dir == IsoDirections.SW) {
            --x;
        }
        if (dir == IsoDirections.NE || dir == IsoDirections.E || dir == IsoDirections.SE) {
            ++x;
        }
        if ((next = (cell = IsoWorld.instance.currentCell).getGridSquare((playerSq = owner.getCurrentSquare()).getX() + x, playerSq.getY() + y, playerSq.getZ())) != null) {
            if (this.checkObjectHit(owner, weapon, next, false, false)) {
                hit = true;
                ++hitCount;
            }
            if (!next.isBlockedTo(playerSq)) {
                for (int n = 0; n < next.getObjects().size(); ++n) {
                    IsoTree isoTree;
                    IsoObject object = next.getObjects().get(n);
                    if (!(object instanceof IsoTree)) continue;
                    this.treeHit = isoTree = (IsoTree)object;
                    hit = true;
                    ++hitCount;
                    ++hitTreeCount;
                    if (object.getObjectIndex() != -1) continue;
                    --n;
                }
            }
        }
        if ((dir == IsoDirections.NE || dir == IsoDirections.N || dir == IsoDirections.NW) && this.checkObjectHit(owner, weapon, playerSq, true, false)) {
            hit = true;
            ++hitCount;
        }
        if ((dir == IsoDirections.SE || dir == IsoDirections.S || dir == IsoDirections.SW) && this.checkObjectHit(owner, weapon, sq2 = cell.getGridSquare(playerSq.getX(), playerSq.getY() + 1, playerSq.getZ()), true, false)) {
            hit = true;
            ++hitCount;
        }
        if ((dir == IsoDirections.SE || dir == IsoDirections.E || dir == IsoDirections.NE) && this.checkObjectHit(owner, weapon, sq = cell.getGridSquare(playerSq.getX() + 1, playerSq.getY(), playerSq.getZ()), false, true)) {
            hit = true;
            ++hitCount;
        }
        if ((dir == IsoDirections.NW || dir == IsoDirections.W || dir == IsoDirections.SW) && this.checkObjectHit(owner, weapon, playerSq, false, true)) {
            hit = true;
            ++hitCount;
        }
        this.hitOnlyTree = hit && hitCount == hitTreeCount;
        return hit;
    }

    public void splash(IsoMovingObject obj, HandWeapon weapon, IsoGameCharacter owner) {
        IsoPlayer isoPlayer;
        IsoGameCharacter isoGameCharacter = (IsoGameCharacter)obj;
        if (weapon != null && SandboxOptions.instance.bloodLevel.getValue() > 1) {
            int spn = weapon.getSplatNumber();
            if (spn < 1) {
                spn = 1;
            }
            if (Core.lastStand) {
                spn *= 3;
            }
            switch (SandboxOptions.instance.bloodLevel.getValue()) {
                case 2: {
                    spn /= 2;
                    break;
                }
                case 4: {
                    spn *= 2;
                    break;
                }
                case 5: {
                    spn *= 5;
                }
            }
            for (int n = 0; n < spn; ++n) {
                isoGameCharacter.splatBlood(3, 0.3f);
            }
        }
        int rand = 3;
        int nbRepeat = 7;
        switch (SandboxOptions.instance.bloodLevel.getValue()) {
            case 1: {
                nbRepeat = 0;
                break;
            }
            case 2: {
                nbRepeat = 4;
                rand = 5;
                break;
            }
            case 4: {
                nbRepeat = 10;
                rand = 2;
                break;
            }
            case 5: {
                nbRepeat = 15;
                rand = 0;
            }
        }
        if (SandboxOptions.instance.bloodLevel.getValue() > 1) {
            isoGameCharacter.splatBloodFloorBig();
        }
        float dz = 0.5f;
        if (isoGameCharacter instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)isoGameCharacter;
            if (isoZombie.crawling || isoGameCharacter.getCurrentState() == ZombieOnGroundState.instance()) {
                dz = 0.2f;
            }
        }
        float addedDistX = Rand.Next(1.5f, 5.0f);
        float addedDistY = Rand.Next(1.5f, 5.0f);
        if (owner instanceof IsoPlayer && (isoPlayer = (IsoPlayer)owner).isDoShove()) {
            addedDistX = Rand.Next(0.0f, 0.5f);
            addedDistY = Rand.Next(0.0f, 0.5f);
        }
        if (nbRepeat > 0) {
            isoGameCharacter.playBloodSplatterSound();
        }
        for (int i = 0; i < nbRepeat; ++i) {
            if (Rand.Next(rand) != 0) continue;
            new IsoZombieGiblets(IsoZombieGiblets.GibletType.A, isoGameCharacter.getCell(), isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ() + dz, isoGameCharacter.getHitDir().x * addedDistX, isoGameCharacter.getHitDir().y * addedDistY);
        }
    }

    private int DoSwingCollisionBoneCheck(IsoGameCharacter owner, HandWeapon weapon, IsoGameCharacter mover, int bone, float tempoLengthTest) {
        float weaponLength = weapon.weaponLength;
        weaponLength += 0.5f;
        if (owner.isAimAtFloor() && ((IsoLivingCharacter)owner).isDoShove()) {
            weaponLength = 0.3f;
        }
        Model.boneToWorldCoords(mover, bone, tempVectorBonePos);
        for (int x = 1; x <= 10; ++x) {
            boolean hit;
            float delta = (float)x / 10.0f;
            CombatManager.tempVector3_1.x = owner.getX();
            CombatManager.tempVector3_1.y = owner.getY();
            CombatManager.tempVector3_1.z = owner.getZ();
            CombatManager.tempVector3_1.x += owner.getForwardDirectionX() * weaponLength * delta;
            CombatManager.tempVector3_1.y += owner.getForwardDirectionY() * weaponLength * delta;
            CombatManager.tempVector3_1.x = CombatManager.tempVectorBonePos.x - CombatManager.tempVector3_1.x;
            CombatManager.tempVector3_1.y = CombatManager.tempVectorBonePos.y - CombatManager.tempVector3_1.y;
            CombatManager.tempVector3_1.z = 0.0f;
            boolean bl = hit = tempVector3_1.getLength() < tempoLengthTest;
            if (!hit) continue;
            return bone;
        }
        return -1;
    }

    public void processMaintenanceCheck(IsoGameCharacter owner, HandWeapon weapon, IsoObject isoObject) {
        if (!owner.isActuallyAttackingWithMeleeWeapon() || GameClient.client || weapon == null) {
            return;
        }
        float breakMultiplier = 1.0f;
        if (((IsoPlayer)owner).isAttackType(AttackType.CHARGE)) {
            breakMultiplier /= 1.5f;
        }
        if (isoObject instanceof IsoTree) {
            int axeBonusConditionModifier;
            boolean axeBonus = weapon.getScriptItem().containsWeaponCategory(WeaponCategory.AXE);
            int n = axeBonusConditionModifier = axeBonus ? 2 : 1;
            if (Rand.Next(weapon.getConditionLowerChance() * axeBonusConditionModifier + owner.getMaintenanceMod()) == 0) {
                weapon.checkSyncItemFields(weapon.damageCheck(0, breakMultiplier));
            }
            LuaEventManager.triggerEvent("OnWeaponHitTree", owner, weapon);
        } else {
            weapon.checkSyncItemFields(weapon.damageCheck(0, breakMultiplier));
        }
        if (!(!Rand.NextBool(2) || owner.isAimAtFloor() && ((IsoLivingCharacter)owner).isDoShove())) {
            if (weapon.isTwoHandWeapon() && (owner.getPrimaryHandItem() != weapon || owner.getSecondaryHandItem() != weapon) && Rand.NextBool(3)) {
                return;
            }
            if (!(weapon.hasTag(ItemTag.NO_MAINTENANCE_XP) || owner.isShoving() || owner.isDoStomp())) {
                float amount = 2.0f;
                if (weapon.getConditionLowerChance() > 10) {
                    amount = amount * 10.0f / (float)weapon.getConditionLowerChance();
                }
                if (GameServer.server) {
                    GameServer.addXp((IsoPlayer)owner, PerkFactory.Perks.Maintenance, amount);
                } else {
                    owner.getXp().AddXP(PerkFactory.Perks.Maintenance, amount);
                }
            }
        }
    }

    private boolean processIsoBarricade(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon, HitInfo hitInfo) {
        IsoWindow isoWindow = null;
        if (hitInfo.getObject() == null && hitInfo.window.getObject() != null) {
            isoWindow = (IsoWindow)hitInfo.window.getObject();
        }
        if (isoWindow == null) {
            return true;
        }
        IsoBarricade isoBarricade = isoWindow.getBarricadeForCharacter(isoGameCharacter);
        if (isoBarricade != null) {
            if (!isoBarricade.canAttackBypassIsoBarricade(isoGameCharacter, handWeapon)) {
                isoBarricade.WeaponHit(isoGameCharacter, handWeapon);
                return false;
            }
            return true;
        }
        return true;
    }

    private void processIsoWindow(IsoGameCharacter owner, HitInfo hitInfo) {
        IsoWindow isoWindow = null;
        if (hitInfo.getObject() == null && hitInfo.window.getObject() != null) {
            isoWindow = (IsoWindow)hitInfo.window.getObject();
        }
        if (isoWindow == null) {
            return;
        }
        if (!isoWindow.isDestroyed()) {
            isoWindow.addBrokenGlass(owner);
        }
        isoWindow.smashWindow();
    }

    public void attackCollisionCheck(IsoGameCharacter owner, HandWeapon weapon, SwipeStatePlayer swipeStatePlayer, AttackType attackTypeModifier) {
        IsoLivingCharacter ownerLiving = (IsoLivingCharacter)owner;
        if (owner.isPerformingShoveAnimation()) {
            ownerLiving.setDoShove(true);
        }
        if (owner.isPerformingGrappleGrabAnimation()) {
            ownerLiving.setDoGrapple(true);
        }
        IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
        if (GameServer.server) {
            DebugType.Combat.debugln("Player swing connects.");
        }
        if (GameClient.client && ownerPlayer != null && weapon.isAimedFirearm()) {
            ownerPlayer.networkAi.onShot();
        }
        LuaEventManager.triggerEvent("OnWeaponSwingHitPoint", owner, weapon);
        if (weapon.getPhysicsObject() != null) {
            owner.Throw(weapon);
        }
        if (weapon.isUseSelf()) {
            weapon.Use();
        }
        if (weapon.isOtherHandUse() && owner.getSecondaryHandItem() != null) {
            owner.getSecondaryHandItem().Use();
        }
        boolean bIgnoreDamage = false;
        if (ownerLiving.isDoShove() && !owner.isShoveStompAnim()) {
            bIgnoreDamage = true;
        }
        boolean processConditionLoss = false;
        boolean helmetFall = false;
        IsoMovingObject networkHitIsoObject = null;
        owner.getAttackVars().setWeapon(weapon);
        owner.getAttackVars().targetOnGround.set(ownerLiving.targetOnGround);
        owner.getAttackVars().aimAtFloor = owner.isAimAtFloor();
        owner.getAttackVars().doShove = ownerLiving.isDoShove() || owner.isPerformingShoveAnimation();
        owner.getAttackVars().doGrapple = ownerLiving.isDoGrapple() || ownerLiving.isPerformingGrappleGrabAnimation();
        this.calculateHitInfoList(owner);
        int hitCount = owner.getHitInfoList().size();
        if (GameClient.client) {
            if (ownerPlayer != null) {
                GameClient.sendAttackCollisionCheck(ownerPlayer, weapon, hitCount);
            }
        } else {
            this.processWeaponEndurance(owner, weapon);
            owner.addCombatMuscleStrain(weapon, hitCount);
        }
        owner.setLastHitCount(hitCount);
        int split = 1;
        this.dotList.clear();
        if (hitCount == 0 && owner.getClickSound() != null && !ownerLiving.isDoShove()) {
            if (ownerPlayer == null || ownerPlayer.isLocalPlayer()) {
                owner.getEmitter().playSound(owner.getClickSound());
            }
            owner.setRecoilDelay(this.combatConfig.get(CombatConfigKey.RECOIL_DELAY));
        }
        boolean shotgunXPAwarded = false;
        for (int i = 0; i < hitCount; ++i) {
            boolean hit;
            boolean ignoreHitCountDamage = false;
            int hitHead = 0;
            boolean hitLegs = false;
            boolean removeKnife = false;
            HitInfo hitInfo = owner.getHitInfoList().get(i);
            IsoMovingObject hitObject = hitInfo.getObject();
            BaseVehicle hitVehicle = Type.tryCastTo(hitObject, BaseVehicle.class);
            IsoGameCharacter hitCharacter = Type.tryCastTo(hitObject, IsoGameCharacter.class);
            IsoZombie hitZombie = Type.tryCastTo(hitObject, IsoZombie.class);
            if (networkHitIsoObject == null && hitObject != null) {
                networkHitIsoObject = hitObject;
            }
            if (hitCharacter != null) {
                boolean isCharacterHit;
                boolean isCharacterStanding = hitCharacter.isStanding();
                boolean isCharacterProne = hitCharacter.isProne();
                boolean bl = isCharacterHit = attackTypeModifier == AttackType.NONE || isCharacterProne && attackTypeModifier.hasModifier(AttackTypeModifier.Prone) || isCharacterStanding && attackTypeModifier.hasModifier(AttackTypeModifier.Standing);
                if (!isCharacterHit) continue;
            }
            if (!this.processIsoBarricade(owner, weapon, hitInfo)) continue;
            this.processIsoWindow(owner, hitInfo);
            if (hitObject == null) continue;
            if (this.isWindowBetween(owner, hitObject)) {
                this.smashWindowBetween(owner, hitObject, weapon);
            }
            boolean bl = hit = Rand.Next(100) <= hitInfo.chance;
            if (!hit) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Bullets Chance Missed", 1.0f);
                if (!SandboxOptions.instance.firearmUseDamageChance.getValue()) continue;
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Bullets Damage Ignored", 1.0f);
                ignoreHitCountDamage = true;
            }
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Bullets Chance Hit", 1.0f);
            Vector2 oPos2 = tempVector2_1.set(owner.getX(), owner.getY());
            Vector2 tPos2 = tempVector2_2.set(hitObject.getX(), hitObject.getY());
            tPos2.x -= oPos2.x;
            tPos2.y -= oPos2.y;
            Vector2 angle = owner.getLookVector(tempVector2_1);
            angle.tangent();
            tPos2.normalize();
            float minimumWeaponDamage = weapon.getMinDamage();
            float maximumWeaponDamage = weapon.getMaxDamage();
            long zombieHitSound = 0L;
            if (!weapon.isRangeFalloff()) {
                boolean piercedTargetDamageReduction = false;
                float dot = angle.dot(tPos2);
                for (int d = 0; d < this.dotList.size(); ++d) {
                    float dots = this.dotList.get(d).floatValue();
                    if (!(Math.abs(dot - dots) < 1.0E-4f)) continue;
                    piercedTargetDamageReduction = true;
                    break;
                }
                if (this.dotList.isEmpty()) {
                    this.dotList.add(Float.valueOf(dot));
                }
                if (piercedTargetDamageReduction) {
                    minimumWeaponDamage /= this.combatConfig.get(CombatConfigKey.PIERCING_BULLET_DAMAGE_REDUCTION);
                    maximumWeaponDamage /= this.combatConfig.get(CombatConfigKey.PIERCING_BULLET_DAMAGE_REDUCTION);
                }
            }
            if (owner.isAimAtFloor() && !weapon.isRanged() && owner.isNPC()) {
                this.splash(hitObject, weapon, owner);
                hitHead = Rand.Next(2);
            } else if (owner.isAimAtFloor() && !weapon.isRanged()) {
                int bone;
                if (ownerPlayer == null || ownerPlayer.isLocalPlayer()) {
                    if (!StringUtils.isNullOrEmpty(weapon.getHitFloorSound())) {
                        owner.getEmitter().stopSoundByName(weapon.getSwingSound());
                        if (ownerPlayer != null) {
                            ownerPlayer.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                        }
                        zombieHitSound = owner.playSound(weapon.getHitFloorSound());
                    } else {
                        owner.getEmitter().stopSoundByName(weapon.getSwingSound());
                        if (ownerPlayer != null) {
                            ownerPlayer.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                        }
                        zombieHitSound = owner.playSound(weapon.getZombieHitSound());
                    }
                }
                if ((bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_Head", -1), 0.28f)) == -1) {
                    bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_Spine", -1), 0.28f);
                    if (bone == -1) {
                        bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_L_Calf", -1), 0.13f);
                        if (bone == -1) {
                            bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Calf", -1), 0.13f);
                        }
                        if (bone == -1) {
                            bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_L_Foot", -1), 0.23f);
                        }
                        if (bone == -1) {
                            bone = this.DoSwingCollisionBoneCheck(owner, this.getWeapon(owner), (IsoGameCharacter)hitObject, ((IsoGameCharacter)hitObject).getAnimationPlayer().getSkinningBoneIndex("Bip01_R_Foot", -1), 0.23f);
                        }
                        if (bone == -1) continue;
                        hitLegs = true;
                    }
                } else {
                    this.splash(hitObject, weapon, owner);
                    this.splash(hitObject, weapon, owner);
                    hitHead = Rand.Next(0, 3) + 1;
                }
            }
            if (owner.getVariableBoolean("PistolWhipAnim")) {
                zombieHitSound = owner.playSound(weapon.getZombieHitSound());
            }
            if (!(owner.getAttackVars().aimAtFloor || owner.getAttackVars().closeKill && owner.isCriticalHit() || ownerLiving.isDoShove() || !(hitObject instanceof IsoGameCharacter))) {
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)hitObject;
                if (ownerPlayer == null || ownerPlayer.isLocalPlayer()) {
                    if (ownerPlayer != null) {
                        ownerPlayer.setMeleeHitSurface(ParameterMeleeHitSurface.Material.Body);
                    }
                    if (weapon.isRanged()) {
                        zombieHitSound = isoGameCharacter.playSound(weapon.getZombieHitSound());
                        FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription = FMODManager.instance.getParameterDescription("BulletHitSurface");
                        isoGameCharacter.getEmitter().setParameterValue(zombieHitSound, parameterDescription, hitHead > 0 ? (float)MaterialType.Flesh_Hollow.ordinal() : (float)MaterialType.Flesh.ordinal());
                    } else {
                        owner.getEmitter().stopSoundByName(weapon.getSwingSound());
                        zombieHitSound = owner.playSound(weapon.getZombieHitSound());
                    }
                }
            }
            if (weapon.isRanged() && hitZombie != null) {
                Vector2 oPos = tempVector2_1.set(owner.getX(), owner.getY());
                Vector2 tPos = tempVector2_2.set(hitObject.getX(), hitObject.getY());
                tPos.x -= oPos.x;
                tPos.y -= oPos.y;
                Vector2 dir = hitZombie.getForwardDirection();
                tPos.normalize();
                dir.normalize();
                float dot2 = tPos.dot(dir);
                hitZombie.setHitFromBehind(dot2 > 0.5f);
            }
            if (hitZombie != null && hitZombie.isCurrentState(ZombieOnGroundState.instance())) {
                hitZombie.setReanimateTimer(hitZombie.getReanimateTimer() + (float)Rand.Next(10));
            }
            if (hitZombie != null && hitZombie.isCurrentState(ZombieGetUpState.instance())) {
                hitZombie.setReanimateTimer(Rand.Next(60) + 30);
            }
            boolean isTwoHanded = !weapon.isTwoHandWeapon() || owner.isItemInBothHands(weapon);
            float damage = Rand.Next(minimumWeaponDamage, maximumWeaponDamage);
            if (!weapon.isRanged()) {
                damage *= weapon.getDamageMod(owner) * owner.getHittingMod();
            }
            if (!isTwoHanded && !weapon.isRanged() && maximumWeaponDamage > minimumWeaponDamage) {
                damage -= minimumWeaponDamage;
            }
            if (!weapon.isRanged()) {
                if (!owner.isAimAtFloor() || !ownerLiving.isDoShove()) {
                    float armsPain = 0.0f;
                    for (int x = BodyPartType.ToIndex(BodyPartType.Hand_L); x <= BodyPartType.ToIndex(BodyPartType.UpperArm_R); ++x) {
                        armsPain += owner.getBodyDamage().getBodyParts().get(x).getPain();
                    }
                    if (armsPain > 10.0f) {
                        damage /= PZMath.clamp(armsPain / 10.0f, 1.0f, 30.0f);
                        MoodlesUI.getInstance().wiggle(MoodleType.PAIN);
                        MoodlesUI.getInstance().wiggle(MoodleType.INJURED);
                    }
                } else {
                    float legsPain = 0.0f;
                    for (int x = BodyPartType.ToIndex(BodyPartType.UpperLeg_L); x <= BodyPartType.ToIndex(BodyPartType.Foot_R); ++x) {
                        legsPain += owner.getBodyDamage().getBodyParts().get(x).getPain();
                    }
                    if (legsPain > 10.0f) {
                        damage /= PZMath.clamp(legsPain / 10.0f, 1.0f, 30.0f);
                        MoodlesUI.getInstance().wiggle(MoodleType.PAIN);
                        MoodlesUI.getInstance().wiggle(MoodleType.INJURED);
                    }
                }
            }
            if (!weapon.isRanged()) {
                damage *= owner.getCharacterTraits().getTraitDamageDealtReductionModifier();
            }
            float damageSplit = damage;
            if (!weapon.isRanged()) {
                damageSplit = damage / ((float)split++ * 0.5f);
            }
            Vector2 oPos = tempVector2_1.set(owner.getX(), owner.getY());
            Vector2 tPos = tempVector2_2.set(hitObject.getX(), hitObject.getY());
            tPos.x -= oPos.x;
            tPos.y -= oPos.y;
            float dist2 = tPos.getLength();
            float rangeDel = weapon.isRangeFalloff() ? 1.0f : (weapon.isRanged() ? 0.5f : dist2 / weapon.getMaxRange(owner));
            if ((rangeDel *= 2.0f) < 0.3f) {
                rangeDel = 1.0f;
            }
            if (!weapon.isRanged() && owner.getMoodles().getMoodleLevel(MoodleType.PANIC) > 1) {
                damageSplit -= (float)owner.getMoodles().getMoodleLevel(MoodleType.PANIC) * 0.1f;
                MoodlesUI.getInstance().wiggle(MoodleType.PANIC);
            }
            if (!weapon.isRanged() && owner.getMoodles().getMoodleLevel(MoodleType.STRESS) > 1) {
                damageSplit -= (float)owner.getMoodles().getMoodleLevel(MoodleType.STRESS) * 0.1f;
                MoodlesUI.getInstance().wiggle(MoodleType.STRESS);
            }
            if (damageSplit < 0.0f) {
                damageSplit = 0.1f;
            }
            if (owner.isAimAtFloor() && ownerLiving.isDoShove()) {
                damageSplit = Rand.Next(0.7f, 1.0f) + (float)owner.getPerkLevel(PerkFactory.Perks.Strength) * 0.2f;
                Clothing shoes = (Clothing)owner.getWornItem(ItemBodyLocation.SHOES);
                damageSplit = shoes == null ? (damageSplit *= 0.5f) : (damageSplit *= shoes.getStompPower());
            }
            if (!weapon.isRanged()) {
                switch (owner.getMoodles().getMoodleLevel(MoodleType.ENDURANCE)) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        damageSplit *= 0.5f;
                        MoodlesUI.getInstance().wiggle(MoodleType.ENDURANCE);
                        break;
                    }
                    case 2: {
                        damageSplit *= 0.2f;
                        MoodlesUI.getInstance().wiggle(MoodleType.ENDURANCE);
                        break;
                    }
                    case 3: {
                        damageSplit *= 0.1f;
                        MoodlesUI.getInstance().wiggle(MoodleType.ENDURANCE);
                        break;
                    }
                    case 4: {
                        damageSplit *= 0.05f;
                        MoodlesUI.getInstance().wiggle(MoodleType.ENDURANCE);
                    }
                }
                switch (owner.getMoodles().getMoodleLevel(MoodleType.TIRED)) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        damageSplit *= 0.5f;
                        MoodlesUI.getInstance().wiggle(MoodleType.TIRED);
                        break;
                    }
                    case 2: {
                        damageSplit *= 0.2f;
                        MoodlesUI.getInstance().wiggle(MoodleType.TIRED);
                        break;
                    }
                    case 3: {
                        damageSplit *= 0.1f;
                        MoodlesUI.getInstance().wiggle(MoodleType.TIRED);
                        break;
                    }
                    case 4: {
                        damageSplit *= 0.05f;
                        MoodlesUI.getInstance().wiggle(MoodleType.TIRED);
                    }
                }
            }
            owner.knockbackAttackMod = 1.0f;
            if ("KnifeDeath".equals(owner.getVariableString("ZombieHitReaction"))) {
                rangeDel *= 1000.0f;
                owner.knockbackAttackMod = 0.0f;
                owner.addWorldSoundUnlessInvisible(4, 4, false);
                owner.getAttackVars().closeKill = true;
                hitObject.setCloseKilled(true);
            } else {
                owner.getAttackVars().closeKill = false;
                hitObject.setCloseKilled(false);
                owner.addWorldSoundUnlessInvisible(8, 8, false);
                if (Rand.Next(3) == 0 || owner.isAimAtFloor() && ownerLiving.isDoShove()) {
                    owner.addWorldSoundUnlessInvisible(10, 10, false);
                } else if (Rand.Next(7) == 0) {
                    owner.addWorldSoundUnlessInvisible(16, 16, false);
                }
            }
            hitObject.setHitFromAngle(hitInfo.dot);
            if (hitZombie != null) {
                hitZombie.setHitFromBehind(owner.isBehind(hitZombie));
                hitZombie.setPlayerAttackPosition(hitZombie.testDotSide(owner));
                hitZombie.setHitHeadWhileOnFloor(hitHead);
                hitZombie.setHitLegsWhileOnFloor(hitLegs);
                processConditionLoss = true;
            }
            if (hitCharacter != null) {
                if (weapon.isMelee()) {
                    int partHit = hitHead > 0 ? Rand.Next(BodyPartType.ToIndex(BodyPartType.Head), BodyPartType.ToIndex(BodyPartType.Neck) + 1) : (hitLegs ? Rand.Next(BodyPartType.ToIndex(BodyPartType.Groin), BodyPartType.ToIndex(BodyPartType.Foot_R) + 1) : Rand.Next(BodyPartType.ToIndex(BodyPartType.Hand_L), BodyPartType.ToIndex(BodyPartType.Neck) + 1));
                    damageSplit = this.applyMeleeHitLocationDamage(hitCharacter, weapon, partHit, hitHead, hitLegs, damageSplit);
                    this.resolveSpikedArmorDamage(owner, weapon, hitCharacter, partHit);
                    if (hitZombie != null) {
                        removeKnife = this.applyKnifeDeathEffect(owner, hitZombie);
                    }
                } else if (weapon.isRanged()) {
                    int bodyPart = this.processHit(weapon, owner, hitCharacter);
                    damageSplit = this.applyRangeHitLocationDamage(hitCharacter, weapon, bodyPart, damageSplit);
                }
                if (!GameClient.client && !GameServer.server || GameClient.client && IsoPlayer.isLocalPlayer(owner)) {
                    helmetFall = hitCharacter.helmetFall(hitHead > 0);
                }
            }
            float hitDamage = 0.0f;
            boolean isCriticalHit = owner.isCriticalHit();
            if (hitVehicle == null && hitObject.getSquare() != null && owner.getSquare() != null) {
                hitObject.setCloseKilled(owner.getAttackVars().closeKill);
                if (ownerPlayer.isLocalPlayer() || owner.isNPC()) {
                    hitDamage = hitObject.Hit(weapon, owner, damageSplit, bIgnoreDamage, rangeDel);
                    if (hitObject instanceof IsoGameCharacter) {
                        IsoGameCharacter isoGameCharacter = (IsoGameCharacter)hitObject;
                        this.applyMeleeEnduranceLoss(owner, isoGameCharacter, weapon, hitDamage);
                    }
                    this.setParameterCharacterHitResult(owner, hitZombie, zombieHitSound);
                }
                if (!(ignoreHitCountDamage || GameClient.client || GameServer.server || weapon.isRangeFalloff() && shotgunXPAwarded)) {
                    LuaEventManager.triggerEvent("OnWeaponHitXp", owner, weapon, hitObject, Float.valueOf(damageSplit), 1);
                    if (weapon.isRangeFalloff()) {
                        shotgunXPAwarded = true;
                    }
                }
                if ((!ownerLiving.isDoShove() || owner.isAimAtFloor()) && owner.DistToSquared(hitObject) < 2.0f && Math.abs(owner.getZ() - hitObject.getZ()) < 0.5f) {
                    owner.addBlood(null, false, false, false);
                }
                if (hitObject instanceof IsoGameCharacter) {
                    IsoGameCharacter character = (IsoGameCharacter)hitObject;
                    processConditionLoss = true;
                    this.processMaintenanceCheck(owner, weapon, hitObject);
                    if (character.isDead()) {
                        owner.getStats().remove(CharacterStat.STRESS, 0.02f);
                    } else if (!(hitObject instanceof IsoPlayer || ownerLiving.isDoShove() && !owner.isAimAtFloor())) {
                        this.splash(hitObject, weapon, owner);
                    }
                    HitReactionNetworkAI.CalcHitReactionWeapon(owner, character, weapon);
                    if (character instanceof IsoPlayer && character.isCurrentGameClientState(ClimbThroughWindowState.instance())) {
                        character.getNetworkCharacterAI().resetState();
                    }
                    GameClient.sendPlayerHit(owner, hitObject, weapon, hitDamage, ignoreHitCountDamage, rangeDel, isCriticalHit, helmetFall, hitHead > 0, removeKnife);
                }
            } else if (hitVehicle != null) {
                if (!hitVehicle.processHit(owner, weapon, damageSplit)) break;
                processConditionLoss = true;
                this.processMaintenanceCheck(owner, weapon, hitVehicle);
                hitDamage = damageSplit;
                GameClient.sendPlayerHit(owner, hitVehicle, weapon, hitDamage, ignoreHitCountDamage, rangeDel, isCriticalHit, helmetFall, hitHead > 0, removeKnife);
                break;
            }
            bIgnoreDamage |= ignoreHitCountDamage;
        }
        if (this.processTreeHit(owner, weapon)) {
            processConditionLoss = true;
        }
        if ((processConditionLoss = this.checkForConditionLoss(owner, weapon, processConditionLoss)) && !weapon.isMelee()) {
            weapon.checkSyncItemFields(weapon.damageCheck(0, 1.0f, false, true, owner));
        }
        if (this.objHit != null) {
            this.processMaintenanceCheck(owner, weapon, this.objHit);
            GameClient.sendPlayerHit(owner, this.objHit, weapon, 0.0f, bIgnoreDamage, 1.0f, owner.isCriticalHit(), false, false, false);
        }
        if (!processConditionLoss) {
            GameClient.sendPlayerHit(owner, null, weapon, 0.0f, bIgnoreDamage, 1.0f, owner.isCriticalHit(), false, false, false);
        }
        owner.set(SwipeStatePlayer.LOWER_CONDITION, processConditionLoss);
        owner.set(SwipeStatePlayer.ATTACKED, true);
        if (weapon.isAimedFirearm()) {
            EffectsManager.getInstance().startMuzzleFlash(owner, 1);
            BallisticsController ballisticsController = owner.getBallisticsController();
            ballisticsController.update();
            this.fireWeapon(weapon, owner);
            StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Shots Fired", 1.0f);
        }
    }

    private boolean checkForConditionLoss(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon, boolean processConditionLoss) {
        boolean hasCondition;
        if (handWeapon.isBareHands()) {
            return false;
        }
        boolean bl = hasCondition = handWeapon.getCondition() > 0;
        if (hasCondition && handWeapon.isAimedFirearm()) {
            return processConditionLoss || isoGameCharacter.isRangedWeaponEmpty();
        }
        if (!processConditionLoss) {
            return false;
        }
        return hasCondition;
    }

    public void processWeaponEndurance(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        if (!handWeapon.isUseEndurance()) {
            return;
        }
        float weight = handWeapon.getEffectiveWeight();
        float enduranceTwoHandsWeaponModifier = 0.0f;
        if (handWeapon.isTwoHandWeapon() && (isoGameCharacter.getPrimaryHandItem() != handWeapon || isoGameCharacter.getSecondaryHandItem() != handWeapon)) {
            enduranceTwoHandsWeaponModifier = weight / 1.5f / 10.0f;
        }
        float val = (weight * 0.18f * handWeapon.getFatigueMod(isoGameCharacter) * isoGameCharacter.getFatigueMod() * handWeapon.getEnduranceMod() * 0.3f + enduranceTwoHandsWeaponModifier) * 0.04f;
        float mod = isoGameCharacter.getCharacterTraits().getTraitEnduranceLossModifier();
        isoGameCharacter.getStats().remove(CharacterStat.ENDURANCE, val * mod);
    }

    private boolean processTreeHit(IsoGameCharacter isoGameCharacter, HandWeapon handWeapon) {
        this.treeHit = null;
        boolean objectHit = this.CheckObjectHit(isoGameCharacter, handWeapon);
        if (!objectHit) {
            return false;
        }
        if (this.hitOnlyTree) {
            this.processMaintenanceCheck(isoGameCharacter, handWeapon, this.treeHit);
            GameClient.sendPlayerHit(isoGameCharacter, this.treeHit, handWeapon, 0.0f, false, 1.0f, false, false, false, false);
        }
        if (this.treeHit != null) {
            this.treeHit.WeaponHit(isoGameCharacter, handWeapon);
        }
        return true;
    }

    public void releaseBallisticsTargets(IsoGameCharacter isoGameCharacter) {
        PZArrayList<HitInfo> hitInfoList = isoGameCharacter.getHitInfoList();
        for (int i = 0; i < hitInfoList.size(); ++i) {
            IsoZombie zombie;
            BallisticsTarget ballisticsTarget;
            HitInfo hitInfo = hitInfoList.get(i);
            IsoMovingObject isoMovingObject = hitInfo.getObject();
            if (!(isoMovingObject instanceof IsoZombie) || (ballisticsTarget = (zombie = (IsoZombie)isoMovingObject).getBallisticsTarget()) == null) continue;
            zombie.releaseBallisticsTarget();
        }
    }

    public void applyDamage(IsoGameCharacter isoGameCharacter, float damageAmount) {
        if (isoGameCharacter.isInvulnerable()) {
            return;
        }
        isoGameCharacter.applyDamage(damageAmount);
    }

    public void applyDamage(BodyPart bodyPart, float damageAmount) {
        IsoGameCharacter isoGameCharacter = bodyPart.getParentChar();
        if (isoGameCharacter == null || isoGameCharacter.isInvulnerable()) {
            return;
        }
        bodyPart.ReduceHealth(damageAmount);
    }

    public void calculateAttackVars(IsoLivingCharacter isoLivingCharacter) {
        this.calculateAttackVars(isoLivingCharacter, isoLivingCharacter.getAttackVars());
    }

    private void calculateAttackVars(IsoLivingCharacter owner, AttackVars vars) {
        HitInfo bestProne;
        if (vars.isProcessed) {
            return;
        }
        HandWeapon weapon = Type.tryCastTo(owner.getPrimaryHandItem(), HandWeapon.class);
        if (weapon != null && weapon.getOtherHandRequire() != null) {
            InventoryItem secondary = owner.getSecondaryHandItem();
            if (secondary != null) {
                if (!secondary.hasTag(weapon.getOtherHandRequire()) || secondary.getCurrentUses() == 0) {
                    weapon = null;
                }
            } else {
                weapon = null;
            }
        }
        if (GameClient.client && !owner.isLocal()) {
            return;
        }
        boolean bAttacking = owner.isPerformingHostileAnimation();
        vars.setWeapon(weapon == null ? owner.bareHands : weapon);
        vars.targetOnGround.set(null);
        vars.aimAtFloor = false;
        vars.closeKill = false;
        vars.doShove = owner.isDoShove();
        vars.doGrapple = owner.isDoGrapple();
        vars.useChargeDelta = owner.useChargeDelta;
        vars.recoilDelay = 0;
        if (vars.doGrapple) {
            vars.aimAtFloor = false;
            vars.setWeapon(owner.bareHands);
        } else if ((vars.getWeapon(owner) == owner.bareHands || vars.doShove) && !((IsoPlayer)owner).isAttackType(AttackType.CHARGE)) {
            vars.doShove = true;
            vars.aimAtFloor = false;
            vars.setWeapon(owner.bareHands);
        }
        this.calcValidTargets(owner, vars.getWeapon(owner), vars.targetsProne, vars.targetsStanding);
        HitInfo bestStanding = vars.targetsStanding.isEmpty() ? null : vars.targetsStanding.get(0);
        HitInfo hitInfo = bestProne = vars.targetsProne.isEmpty() ? null : vars.targetsProne.get(0);
        if (this.isProneTargetBetter(owner, bestStanding, bestProne)) {
            bestStanding = null;
        }
        if (!bAttacking) {
            owner.setAimAtFloor(false);
        }
        float lowestDistSq = Float.MAX_VALUE;
        if (bestStanding != null) {
            if (!bAttacking) {
                owner.setAimAtFloor(false);
            }
            vars.aimAtFloor = false;
            vars.targetOnGround.set(null);
            vars.targetStanding.set(bestStanding.getObject());
            vars.targetDistance = PZMath.sqrt(bestStanding.distSq);
            lowestDistSq = bestStanding.distSq;
        } else if (bestProne != null && (Core.getInstance().isOptionAutoProneAtk() || owner.isDoShove())) {
            float targetDistance = PZMath.sqrt(bestProne.distSq);
            if (!bAttacking) {
                owner.setAimAtFloor(true);
            }
            vars.aimAtFloor = !owner.isDoShove() || targetDistance < 0.6f;
            vars.targetOnGround.set(bestProne.getObject());
            vars.targetStanding.set(null);
            vars.targetDistance = targetDistance;
        }
        if (!(lowestDistSq >= vars.getWeapon(owner).getMinRange() * vars.getWeapon(owner).getMinRange() || bestStanding != null && this.isWindowBetween(owner, bestStanding.getObject()))) {
            if (owner.getStats().numChasingZombies <= 1 && WeaponType.getWeaponType(owner) == WeaponType.KNIFE) {
                vars.closeKill = true;
                return;
            }
            vars.doShove = true;
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (player != null && (!player.isAuthorizedHandToHand() || player.isAttackType(AttackType.CHARGE))) {
                vars.doShove = false;
            }
            vars.aimAtFloor = false;
            if (owner.bareHands.getSwingAnim() != null) {
                vars.useChargeDelta = 3.0f;
            }
        }
        int keyFloorAtk = GameKeyboard.whichKeyDown("ManualFloorAtk");
        int keySprint = GameKeyboard.whichKeyDown("Sprint");
        boolean bStartedAttackWhileSprinting = owner.getVariableBoolean("StartedAttackWhileSprinting");
        if (GameKeyboard.isKeyDown("ManualFloorAtk") && (keyFloorAtk != keySprint || !bStartedAttackWhileSprinting)) {
            vars.aimAtFloor = true;
            vars.doShove = GameKeyboard.isKeyDown("Melee") || vars.getWeapon(owner) == owner.bareHands;
            owner.setDoShove(vars.doShove);
        }
        if (vars.getWeapon(owner).isRanged() && !"Auto".equalsIgnoreCase(owner.getFireMode())) {
            vars.recoilDelay = vars.getWeapon(owner).getRecoilDelay(owner);
        }
    }

    public void calcValidTargets(IsoLivingCharacter owner, HandWeapon weapon, PZArrayList<HitInfo> targetsProne, PZArrayList<HitInfo> targetsStanding) {
        boolean useBallistics = false;
        BallisticsController ballisticsController = owner.getBallisticsController();
        if (ballisticsController != null) {
            useBallistics = weapon.isAimedFirearm();
        }
        TargetComparator targetComparator = this.meleeTargetComparator;
        this.hitInfoPool.release((List<HitInfo>)targetsProne);
        this.hitInfoPool.release((List<HitInfo>)targetsStanding);
        targetsProne.clear();
        targetsStanding.clear();
        float ignoreProneRange = Core.getInstance().getIgnoreProneZombieRange();
        float weaponRange = weapon.getMaxRange() * weapon.getRangeMod(owner);
        boolean applyLungeStateExtraRange = !useBallistics;
        float range = Math.max(ignoreProneRange, weaponRange + (applyLungeStateExtraRange ? 1.0f : 0.0f));
        float minAngle = weapon.getMinAngle();
        float maxAngle = weapon.getMaxAngle();
        HitInfo meleeTargetTooCloseTooShoot = null;
        if (useBallistics) {
            this.calculateBallistics(owner, range);
        }
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();
        for (int i = 0; i < objects.size(); ++i) {
            IsoMovingObject mov = objects.get(i);
            HitInfo hitInfo = this.calcValidTarget(owner, weapon, mov, range);
            if (hitInfo == null) continue;
            if (mov.isStanding()) {
                targetsStanding.add(hitInfo);
                if (!useBallistics || !this.isMeleeTargetTooCloseToShoot(owner, weapon, minAngle, maxAngle, hitInfo)) continue;
                if (meleeTargetTooCloseTooShoot == null) {
                    meleeTargetTooCloseTooShoot = this.hitInfoPool.alloc().init(hitInfo);
                    continue;
                }
                if (this.meleeTargetComparator.compare(hitInfo, meleeTargetTooCloseTooShoot) >= 0) continue;
                meleeTargetTooCloseTooShoot.init(hitInfo);
                continue;
            }
            targetsProne.add(hitInfo);
        }
        if (!useBallistics && !targetsProne.isEmpty() && this.shouldIgnoreProneZombies(owner, targetsStanding, ignoreProneRange)) {
            this.hitInfoPool.release((List<HitInfo>)targetsProne);
            targetsProne.clear();
        }
        if (weapon.isRanged()) {
            targetComparator = this.rangeTargetComparator;
        }
        if (useBallistics) {
            this.removeUnhittableBallisticsTargets(owner, weapon, range, this.combatConfig.get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD), targetsStanding);
        } else {
            this.removeUnhittableMeleeTargets(owner, weapon, minAngle, maxAngle, targetsStanding);
        }
        if (weapon.isRanged() && ballisticsController != null) {
            this.removeUnhittableBallisticsTargets(owner, weapon, range, this.combatConfig.get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD), targetsProne);
        } else {
            this.removeUnhittableMeleeTargets(owner, weapon, minAngle /= 1.5f, maxAngle, targetsProne);
        }
        if (meleeTargetTooCloseTooShoot != null) {
            this.hitInfoPool.releaseAll((List<HitInfo>)targetsStanding);
            targetsStanding.clear();
            targetsStanding.add(meleeTargetTooCloseTooShoot);
        }
        targetComparator.setBallisticsController(ballisticsController);
        this.timSort.doSort(targetsStanding.getElements(), targetComparator, 0, targetsStanding.size());
        this.timSort.doSort(targetsProne.getElements(), targetComparator, 0, targetsProne.size());
    }

    private boolean shouldIgnoreProneZombies(IsoGameCharacter owner, PZArrayList<HitInfo> targetsStanding, float range) {
        IsoPlayer isoPlayer;
        if (range <= 0.0f) {
            return false;
        }
        boolean isInvisible = owner.isInvisible() || owner instanceof IsoPlayer && (isoPlayer = (IsoPlayer)owner).isGhostMode();
        for (int i = 0; i < targetsStanding.size(); ++i) {
            boolean collideStand;
            HitInfo hitInfo = targetsStanding.get(i);
            IsoZombie zombie = Type.tryCastTo(hitInfo.getObject(), IsoZombie.class);
            if (zombie != null && zombie.target == null && !isInvisible || hitInfo.distSq > range * range || (collideStand = PolygonalMap2.instance.lineClearCollide(owner.getX(), owner.getY(), hitInfo.getObject().getX(), hitInfo.getObject().getY(), PZMath.fastfloor(owner.getZ()), owner, false, true))) continue;
            return true;
        }
        return false;
    }

    private boolean isPointWithinDistance(Vector3 start, Vector3 end, Vector3 target, float distanceThreshold) {
        float abX = end.x - start.x;
        float abY = end.y - start.y;
        float abZ = end.z - start.z;
        float apX = target.x - start.x;
        float apY = target.y - start.y;
        float apZ = target.z - start.z;
        float crossX = apY * abZ - apZ * abY;
        float crossY = apZ * abX - apX * abZ;
        float crossZ = apX * abY - apY * abX;
        float crossSquared = crossX * crossX + crossY * crossY + crossZ * crossZ;
        float abSquared = abX * abX + abY * abY + abZ * abZ;
        float dotProduct = abX * apX + abY * apY + abZ * apZ;
        return crossSquared <= distanceThreshold * distanceThreshold * abSquared && dotProduct > 0.0f;
    }

    private void removeUnhittableBallisticsTargets(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float range, float distanceThreshold, PZArrayList<HitInfo> targets) {
        for (int i = targets.size() - 1; i >= 0; --i) {
            HitInfo hitInfo = targets.get(i);
            tempVector3_1.set(hitInfo.x, hitInfo.y, hitInfo.z);
            if (!this.isPointWithinDistance(ballisticsStartPosition, ballisticsEndPosition, tempVector3_1, distanceThreshold)) {
                this.hitInfoPool.release(hitInfo);
                targets.remove(i);
                continue;
            }
            if (!Core.debug || !DebugOptions.instance.character.debug.render.aimCone.getValue()) continue;
            LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1f, CombatManager.TargetableDebugColor.r, CombatManager.TargetableDebugColor.g, CombatManager.TargetableDebugColor.b, 1.0f);
        }
    }

    private boolean isUnhittableMeleeTarget(IsoGameCharacter owner, HandWeapon weapon, float minAngle, float maxAngle, HitInfo hitInfo) {
        if (hitInfo.dot < minAngle || hitInfo.dot > maxAngle) {
            return true;
        }
        Vector3 targetPos = tempVectorBonePos.set(hitInfo.x, hitInfo.y, hitInfo.z);
        return !owner.isMeleeAttackRange(weapon, hitInfo.getObject(), targetPos);
    }

    private boolean isMeleeTargetTooCloseToShoot(IsoGameCharacter owner, HandWeapon weapon, float minAngle, float maxAngle, HitInfo hitInfo) {
        if (this.isUnhittableMeleeTarget(owner, ((IsoLivingCharacter)owner).bareHands, minAngle, maxAngle, hitInfo)) {
            return false;
        }
        tempVector3_1.set(hitInfo.x, hitInfo.y, hitInfo.z);
        return !this.isPointWithinDistance(ballisticsStartPosition, ballisticsEndPosition, tempVector3_1, this.combatConfig.get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD));
    }

    private void calculateBallistics(IsoGameCharacter isoGameCharacter, float range) {
        BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
        if (ballisticsController == null) {
            return;
        }
        ballisticsController.calculateMuzzlePosition(ballisticsStartPosition, ballisticsDirectionVector);
        ballisticsDirectionVector.normalize();
        ballisticsEndPosition.set(CombatManager.ballisticsStartPosition.x + CombatManager.ballisticsDirectionVector.x * range, CombatManager.ballisticsStartPosition.y + CombatManager.ballisticsDirectionVector.y * range, CombatManager.ballisticsStartPosition.z + CombatManager.ballisticsDirectionVector.z * range);
    }

    private boolean isHittableBallisticsTarget(BallisticsController ballisticsController, float distanceThreshold, Vector3 targetPos) {
        Vector3 isoAimingPosition = ballisticsController.getIsoAimingPosition();
        return isoAimingPosition.distanceTo(targetPos) < distanceThreshold;
    }

    private boolean isHittableBallisticsTarget(IsoGameCharacter isoGameCharacter, float range, float distanceThreshold, Vector3 targetPos) {
        BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
        boolean isReticleTarget = this.isHittableBallisticsTarget(ballisticsController, distanceThreshold, targetPos);
        if (Core.debug && DebugOptions.instance.character.debug.render.aimCone.getValue() && isReticleTarget) {
            LineDrawer.DrawIsoCircle(targetPos.x, targetPos.y, targetPos.z, 0.1f, CombatManager.TargetableDebugColor.r, CombatManager.TargetableDebugColor.g, CombatManager.TargetableDebugColor.b, 1.0f);
        }
        return isReticleTarget || this.isPointWithinDistance(ballisticsStartPosition, ballisticsEndPosition, targetPos, distanceThreshold);
    }

    private void removeUnhittableMeleeTargets(IsoGameCharacter owner, HandWeapon weapon, float minAngle, float maxAngle, PZArrayList<HitInfo> targets) {
        for (int i = targets.size() - 1; i >= 0; --i) {
            HitInfo hitInfo = targets.get(i);
            if (!this.isUnhittableMeleeTarget(owner, weapon, minAngle, maxAngle, hitInfo)) continue;
            this.hitInfoPool.release(hitInfo);
            targets.remove(i);
        }
    }

    private boolean getNearestMeleeTargetPosAndDot(IsoGameCharacter owner, HandWeapon weapon, IsoMovingObject target, Vector4f out) {
        this.getNearestTargetPosAndDot(owner, target, out);
        float dot = out.w;
        float minAngle = weapon.getMinAngle();
        float maxAngle = weapon.getMaxAngle();
        if (target instanceof IsoGameCharacter) {
            IsoGameCharacter targetChr = (IsoGameCharacter)target;
            if (target.isProne()) {
                minAngle /= 1.5f;
            }
        }
        if (dot < minAngle || dot > maxAngle) {
            return false;
        }
        Vector3 targetPos = tempVectorBonePos.set(out.x, out.y, out.z);
        return owner.isMeleeAttackRange(weapon, target, targetPos);
    }

    private void getNearestTargetPosAndDot(IsoGameCharacter owner, IsoMovingObject target, Vector3 bonePos, Vector2 minDistSq, Vector4f out) {
        float dot = owner.getDotWithForwardDirection(bonePos);
        dot = PZMath.clamp(dot, -1.0f, 1.0f);
        out.w = Math.max(dot, out.w);
        float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), (float)PZMath.fastfloor(owner.getZ()) * 3.0f, bonePos.x, bonePos.y, (float)PZMath.fastfloor(target.getZ()) * 3.0f);
        if (distSq < minDistSq.x) {
            minDistSq.x = distSq;
            out.set(bonePos.x, bonePos.y, bonePos.z, out.w);
        }
    }

    private void getNearestTargetPosAndDot(IsoGameCharacter owner, IsoMovingObject target, String boneName, Vector2 minDistSq, Vector4f out) {
        Vector3 bonePos = CombatManager.getBoneWorldPos(target, boneName, tempVectorBonePos);
        this.getNearestTargetPosAndDot(owner, target, bonePos, minDistSq, out);
    }

    private void getNearestTargetPosAndDot(IsoGameCharacter owner, IsoMovingObject target, Vector4f out) {
        Vector2 minDistSq = tempVector2_1.set(Float.MAX_VALUE, Float.NaN);
        out.w = Float.NEGATIVE_INFINITY;
        if (!(target instanceof IsoGameCharacter)) {
            this.getNearestTargetPosAndDot(owner, target, (String)null, minDistSq, out);
            return;
        }
        IsoGameCharacter targetChr = (IsoGameCharacter)target;
        Vector3 headPos = tempVector3_1;
        int boneIndex = CombatManager.getBoneIndex(target, "Bip01_Head");
        if (boneIndex != -1 && CombatManager.getBoneIndex(target, "Bip01_HeadNub") != -1) {
            CombatManager.getBoneWorldPos(target, "Bip01_Head", tempVector3_1);
            CombatManager.getBoneWorldPos(target, "Bip01_HeadNub", tempVector3_2);
            tempVector3_1.addToThis(tempVector3_2);
            tempVector3_1.div(2.0f);
        } else if (boneIndex != -1) {
            CombatManager.getPointAlongBoneXAxis(target, "Bip01_Head", 0.075f, headPos);
            Model.vectorToWorldCoords(targetChr, headPos);
        }
        if (target.isStanding()) {
            this.getNearestTargetPosAndDot(owner, target, headPos, minDistSq, out);
            this.getNearestTargetPosAndDot(owner, target, "Bip01_Pelvis", minDistSq, out);
            Vector3 targetPos = tempVectorBonePos.set(target.getX(), target.getY(), target.getZ());
            this.getNearestTargetPosAndDot(owner, target, targetPos, minDistSq, out);
        } else {
            this.getNearestTargetPosAndDot(owner, target, headPos, minDistSq, out);
            this.getNearestTargetPosAndDot(owner, target, "Bip01_Pelvis", minDistSq, out);
            boneIndex = CombatManager.getBoneIndex(target, "Bip01_DressFrontNub");
            if (boneIndex == -1) {
                boneIndex = CombatManager.getBoneIndex(target, "Bip01_DressFront02");
                if (boneIndex != -1) {
                    Vector3 bonePos = tempVector3_2;
                    CombatManager.getPointAlongBoneXAxis(target, "Bip01_DressFront02", 0.2f, bonePos);
                    Model.vectorToWorldCoords(targetChr, bonePos);
                    this.getNearestTargetPosAndDot(owner, target, bonePos, minDistSq, out);
                }
            } else {
                this.getNearestTargetPosAndDot(owner, target, "Bip01_DressFrontNub", minDistSq, out);
            }
        }
    }

    private HitInfo calcValidTarget(IsoLivingCharacter owner, HandWeapon weapon, IsoMovingObject mov, float range) {
        if (mov == owner) {
            return null;
        }
        if (!(mov instanceof IsoGameCharacter)) {
            return null;
        }
        IsoGameCharacter isoGameCharacter = (IsoGameCharacter)mov;
        if (mov.getCurrentSquare() == null) {
            return null;
        }
        if (isoGameCharacter.isGodMod() && !isoGameCharacter.isZombie()) {
            return null;
        }
        if (!isoGameCharacter.isAnimal() && !CombatManager.checkPVP(owner, mov)) {
            return null;
        }
        if (weapon != null && !weapon.isRanged() && owner.DistToSquared(isoGameCharacter) > 9.0f) {
            return null;
        }
        float deltaZ = Math.abs(isoGameCharacter.getZ() - owner.getZ());
        if (!weapon.isRanged() && deltaZ >= 0.5f) {
            return null;
        }
        if (deltaZ > 3.3f) {
            return null;
        }
        if (!isoGameCharacter.isShootable()) {
            return null;
        }
        if (isoGameCharacter.isCurrentState(FakeDeadZombieState.instance())) {
            return null;
        }
        if (isoGameCharacter.isDead()) {
            return null;
        }
        if (isoGameCharacter instanceof IsoZombie && Type.tryCastTo(isoGameCharacter, IsoZombie.class).isReanimatedForGrappleOnly()) {
            return null;
        }
        if (isoGameCharacter.getHitReaction() != null && isoGameCharacter.getHitReaction().contains("Death")) {
            return null;
        }
        Vector4f posAndDot = this.tempVector4f;
        this.getNearestTargetPosAndDot(owner, isoGameCharacter, posAndDot);
        float dot = posAndDot.w;
        float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), PZMath.fastfloor(owner.getZ()) * 3, posAndDot.x, posAndDot.y, PZMath.fastfloor(isoGameCharacter.getZ()) * 3);
        if (dot < 0.0f) {
            return null;
        }
        if (distSq > range * range) {
            return null;
        }
        LosUtil.TestResults testResults = LosUtil.lineClear(owner.getCell(), PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY()), PZMath.fastfloor(owner.getZ()), PZMath.fastfloor(isoGameCharacter.getX()), PZMath.fastfloor(isoGameCharacter.getY()), PZMath.fastfloor(isoGameCharacter.getZ()), false);
        if (testResults == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor) {
            return null;
        }
        return this.hitInfoPool.alloc().init(isoGameCharacter, dot, distSq, posAndDot.x, posAndDot.y, posAndDot.z);
    }

    public boolean isProneTargetBetter(IsoGameCharacter owner, HitInfo bestStanding, HitInfo bestProne) {
        if (bestStanding == null || bestStanding.getObject() == null) {
            return false;
        }
        if (bestProne == null || bestProne.getObject() == null) {
            return false;
        }
        if (bestStanding.distSq <= bestProne.distSq) {
            return false;
        }
        boolean collideStand = PolygonalMap2.instance.lineClearCollide(owner.getX(), owner.getY(), bestStanding.getObject().getX(), bestStanding.getObject().getY(), PZMath.fastfloor(owner.getZ()), null, false, true);
        if (!collideStand) {
            return false;
        }
        boolean collideProne = PolygonalMap2.instance.lineClearCollide(owner.getX(), owner.getY(), bestProne.getObject().getX(), bestProne.getObject().getY(), PZMath.fastfloor(owner.getZ()), null, false, true);
        return !collideProne;
    }

    public static boolean checkPVP(IsoGameCharacter owner, IsoMovingObject obj) {
        if (obj instanceof IsoAnimal) {
            return true;
        }
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoPlayer objPlayer = Type.tryCastTo(obj, IsoPlayer.class);
        if (GameClient.client && objPlayer != null && owner != null) {
            if (objPlayer.isGodMod() || !ServerOptions.instance.pvp.getValue() || ServerOptions.instance.safetySystem.getValue() && owner.getSafety().isEnabled() && ((IsoGameCharacter)obj).getSafety().isEnabled()) {
                return false;
            }
            if (NonPvpZone.getNonPvpZone(PZMath.fastfloor(obj.getX()), PZMath.fastfloor(obj.getY())) != null) {
                return false;
            }
            if (player != null && NonPvpZone.getNonPvpZone(PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY())) != null) {
                return false;
            }
            if (player != null && !player.factionPvp && !objPlayer.factionPvp) {
                Faction fact = Faction.getPlayerFaction(player);
                Faction factOther = Faction.getPlayerFaction(objPlayer);
                if (factOther != null && fact == factOther) {
                    return false;
                }
            }
        }
        return GameClient.client || objPlayer == null || IsoPlayer.getCoopPVP();
    }

    private void calcHitListShove(IsoGameCharacter owner, AttackVars attackVars, PZArrayList<HitInfo> hitList) {
        boolean attackingGround = attackVars.aimAtFloor;
        HandWeapon weapon = attackVars.getWeapon((IsoLivingCharacter)owner);
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();
        for (int n = 0; n < objects.size(); ++n) {
            Vector4f posAndDot;
            boolean bHittable;
            IsoZombie zombie;
            IsoGameCharacter isoGameCharacter;
            IsoMovingObject obj = objects.get(n);
            if (obj == owner || obj instanceof BaseVehicle || obj.getCurrentSquare() == null || !(obj instanceof IsoGameCharacter) || (isoGameCharacter = (IsoGameCharacter)obj).isGodMod() && !isoGameCharacter.isZombie() || isoGameCharacter.isDead() || isoGameCharacter.isProne() && !attackingGround || owner.DistToSquared(obj) > 9.0f || (zombie = Type.tryCastTo(obj, IsoZombie.class)) != null && zombie.isCurrentState(FakeDeadZombieState.instance()) || zombie != null && zombie.isReanimatedForGrappleOnly() || !CombatManager.checkPVP(owner, obj)) continue;
            boolean bl = bHittable = obj == attackVars.targetOnGround.getObject() || obj.isShootable() && obj.isStanding() && !attackVars.aimAtFloor || obj.isShootable() && obj.isProne() && attackVars.aimAtFloor;
            if (!bHittable || !this.getNearestMeleeTargetPosAndDot(owner, weapon, obj, posAndDot = this.tempVector4f)) continue;
            float dot = posAndDot.w;
            float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), (float)PZMath.fastfloor(owner.getZ()) * 3.0f, posAndDot.x, posAndDot.y, PZMath.fastfloor(posAndDot.z) * 3);
            LosUtil.TestResults testResults = LosUtil.lineClear(owner.getCell(), PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY()), PZMath.fastfloor(owner.getZ()), PZMath.fastfloor(obj.getX()), PZMath.fastfloor(obj.getY()), PZMath.fastfloor(obj.getZ()), false);
            if (testResults == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor || obj.getCurrentSquare() != null && owner.getCurrentSquare() != null && obj.getCurrentSquare() != owner.getCurrentSquare() && obj.getCurrentSquare().isWindowBlockedTo(owner.getCurrentSquare()) || obj.getSquare() != null && owner.getSquare() != null && obj.getSquare().getTransparentWallTo(owner.getSquare()) != null) continue;
            HitInfo hitInfo = this.hitInfoPool.alloc().init(obj, dot, distSq, posAndDot.x, posAndDot.y, posAndDot.z);
            if (attackVars.targetOnGround.getObject() == obj) {
                hitList.clear();
                hitList.add(hitInfo);
                break;
            }
            hitList.add(hitInfo);
        }
    }

    private void getNearbyGrappleTargets(IsoMovingObject owner, Predicate<IsoMovingObject> predicate, Collection<IsoMovingObject> foundTargets) {
        foundTargets.clear();
        int playerX = PZMath.fastfloor(owner.getX());
        int playerY = PZMath.fastfloor(owner.getY());
        int playerZ = PZMath.fastfloor(owner.getZ());
        for (int dy = -2; dy <= 2; ++dy) {
            for (int dx = -2; dx <= 2; ++dx) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(playerX + dx, playerY + dy, playerZ);
                if (square == null) continue;
                ArrayList<IsoMovingObject> movingObjects = square.getMovingObjects();
                for (int i = 0; i < movingObjects.size(); ++i) {
                    IsoMovingObject object = movingObjects.get(i);
                    if (!predicate.test(object)) continue;
                    foundTargets.add(object);
                }
                ArrayList<IsoMovingObject> staticMovingObjects = square.getStaticMovingObjects();
                for (int i = 0; i < staticMovingObjects.size(); ++i) {
                    IsoMovingObject object = staticMovingObjects.get(i);
                    if (!predicate.test(object)) continue;
                    foundTargets.add(object);
                }
            }
        }
    }

    private void calcHitListGrapple(IsoGameCharacter owner, AttackVars attackVars, PZArrayList<HitInfo> hitList) {
        ArrayList<IsoMovingObject> foundObjects = CalcHitListGrappleReusables.foundObjects;
        foundObjects.clear();
        this.getNearbyGrappleTargets(owner, foundObject -> {
            if (foundObject instanceof BaseVehicle) {
                return false;
            }
            if (foundObject instanceof IsoGameCharacter) {
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)foundObject;
                return !isoGameCharacter.isGodMod() || isoGameCharacter.isZombie();
            }
            return true;
        }, foundObjects);
        HandWeapon weapon = attackVars.getWeapon((IsoLivingCharacter)owner);
        for (IsoMovingObject foundObject2 : foundObjects) {
            LosUtil.TestResults testResults;
            Vector4f posAndDot;
            if (foundObject2 == owner || !this.getNearestMeleeTargetPosAndDot(owner, weapon, foundObject2, posAndDot = CalcHitListGrappleReusables.posAndDot) || (testResults = LosUtil.lineClear(owner.getCell(), PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY()), PZMath.fastfloor(owner.getZ()), PZMath.fastfloor(foundObject2.getX()), PZMath.fastfloor(foundObject2.getY()), PZMath.fastfloor(foundObject2.getZ()), false)) == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor || foundObject2.getCurrentSquare() != null && owner.getCurrentSquare() != null && foundObject2.getCurrentSquare() != owner.getCurrentSquare() && foundObject2.getCurrentSquare().isWindowBlockedTo(owner.getCurrentSquare()) || foundObject2.getSquare().getTransparentWallTo(owner.getSquare()) != null) continue;
            float dot = posAndDot.w;
            float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), (float)PZMath.fastfloor(owner.getZ()) * 3.0f, posAndDot.x, posAndDot.y, (float)PZMath.fastfloor(posAndDot.z) * 3.0f);
            HitInfo hitInfo = this.hitInfoPool.alloc().init(foundObject2, dot, distSq, posAndDot.x, posAndDot.y, posAndDot.z);
            if (attackVars.targetOnGround.getObject() == foundObject2) {
                hitList.clear();
                hitList.add(hitInfo);
                break;
            }
            hitList.add(hitInfo);
        }
        foundObjects.clear();
    }

    private boolean removeTargetObjects(IsoMovingObject obj, IsoGameCharacter owner) {
        IsoZombie zombie;
        IsoZombie isoZombie;
        IsoGameCharacter gameCharacter;
        IsoGameCharacter isoGameCharacter;
        if (obj instanceof IsoGameCharacter && ((isoGameCharacter = (gameCharacter = (IsoGameCharacter)obj)).isDead() || isoGameCharacter.isGodMod() && !isoGameCharacter.isZombie())) {
            return true;
        }
        if (obj instanceof IsoZombie && ((isoZombie = (zombie = (IsoZombie)obj)).isCurrentState(FakeDeadZombieState.instance()) || isoZombie.isReanimatedForGrappleOnly())) {
            return true;
        }
        if (obj == owner) {
            return true;
        }
        if (obj.getCurrentSquare() == null) {
            return true;
        }
        return !CombatManager.checkPVP(owner, obj);
    }

    private void calculateHitListWeapon(IsoGameCharacter owner, AttackVars attackVars, PZArrayList<HitInfo> hitList) {
        boolean attackingGround;
        HandWeapon weapon = attackVars.getWeapon((IsoLivingCharacter)owner);
        float weaponRange = weapon.getMaxRange(owner) * weapon.getRangeMod(owner);
        ArrayList<IsoMovingObject> objects = new ArrayList<IsoMovingObject>(IsoWorld.instance.currentCell.getObjectList());
        if (weapon.isAimedFirearm()) {
            objects.removeIf(obj -> ballisticsStartPosition.distanceTo(obj.getX(), obj.getY(), obj.getZ()) > weaponRange);
        }
        if (!weapon.isAimedFirearm() && !(attackingGround = attackVars.aimAtFloor)) {
            objects.removeIf(IsoMovingObject::isProne);
        }
        objects.removeIf(obj -> this.removeTargetObjects((IsoMovingObject)obj, owner));
        for (int n = 0; n < objects.size(); ++n) {
            IsoWindow window;
            LosUtil.TestResults testResults;
            IsoMovingObject obj2 = objects.get(n);
            IsoGameCharacter isoGameCharacter = Type.tryCastTo(obj2, IsoGameCharacter.class);
            if (!weapon.isAimedFirearm()) {
                boolean bHittable;
                boolean bl = bHittable = obj2 == attackVars.targetOnGround.getObject() || obj2.isShootable() && obj2.isStanding() && !attackVars.aimAtFloor || obj2.isShootable() && obj2.isProne() && attackVars.aimAtFloor;
                if (!bHittable) continue;
            }
            Vector4f posAndDot = this.tempVector4f;
            if (obj2 instanceof BaseVehicle) {
                if (weapon.isRanged()) {
                    tempVector3_1.set(obj2.getX(), obj2.getY(), obj2.getZ());
                    if (!this.isHittableBallisticsTarget(owner, weapon.getMaxRange(), this.combatConfig.get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD), tempVector3_1)) {
                        if (!Core.debug || !DebugOptions.instance.character.debug.render.aimCone.getValue()) continue;
                        LineDrawer.DrawIsoCircle(CombatManager.tempVector3_1.x, CombatManager.tempVector3_1.y, CombatManager.tempVector3_1.z, 1.5f, CombatManager.OccludedTargetDebugColor.r, CombatManager.OccludedTargetDebugColor.g, CombatManager.OccludedTargetDebugColor.b, 1.0f);
                        continue;
                    }
                    posAndDot.set(obj2.getX(), obj2.getY(), obj2.getZ());
                    if (Core.debug && DebugOptions.instance.character.debug.render.aimCone.getValue()) {
                        LineDrawer.DrawIsoCircle(CombatManager.tempVector3_1.x, CombatManager.tempVector3_1.y, CombatManager.tempVector3_1.z, 1.5f, CombatManager.TargetableDebugColor.r, CombatManager.TargetableDebugColor.g, CombatManager.TargetableDebugColor.b, 1.0f);
                    }
                } else {
                    float dot;
                    if (owner.DistToSquared(obj2) > 9.0f || (dot = owner.getDotWithForwardDirection(obj2.getX(), obj2.getY())) < 0.8f) continue;
                    posAndDot.set(obj2.getX(), obj2.getY(), obj2.getZ(), dot);
                }
            } else {
                if (isoGameCharacter == null || !weapon.isRanged() && owner.DistToSquared(obj2) > 9.0f) continue;
                if (weapon.isRanged()) {
                    tempVector3_1.set(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
                    posAndDot.set(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
                    if (!this.isHittableBallisticsTarget(owner, weapon.getMaxRange(), this.combatConfig.get(CombatConfigKey.BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD), tempVector3_1)) {
                        continue;
                    }
                } else if (!this.getNearestMeleeTargetPosAndDot(owner, weapon, obj2, posAndDot)) continue;
            }
            if ((testResults = LosUtil.lineClear(owner.getCell(), PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY()), PZMath.fastfloor(owner.getZ()), PZMath.fastfloor(obj2.getX()), PZMath.fastfloor(obj2.getY()), PZMath.fastfloor(obj2.getZ()), false)) == LosUtil.TestResults.Blocked || testResults == LosUtil.TestResults.ClearThroughClosedDoor || obj2.getSquare() != null && owner.getSquare() != null && obj2.getSquare().getTransparentWallTo(owner.getSquare()) != null && !weapon.canAttackPierceTransparentWall(owner, weapon) || (window = this.getWindowBetween(owner, obj2)) != null && window.isBarricaded() && !window.canAttackBypassIsoBarricade(owner, weapon)) continue;
            float dot = posAndDot.w;
            float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), (float)PZMath.fastfloor(owner.getZ()) * 3.0f, posAndDot.x, posAndDot.y, (float)PZMath.fastfloor(obj2.getZ()) * 3.0f);
            HitInfo hitInfo = this.hitInfoPool.alloc().init(obj2, dot, distSq, posAndDot.x, posAndDot.y, posAndDot.z);
            hitInfo.window.setObject(window);
            hitList.add(hitInfo);
        }
        if (!hitList.isEmpty() && weapon.isRanged()) {
            this.processBallisticsTargets(owner, weapon, hitList);
            return;
        }
        this.CalcHitListWindow(owner, weapon, hitList);
    }

    private boolean isOccluded(Vector3f origin, Vector3f direction, Vector3 position, float width, float height, ArrayList<HitInfo> hitList) {
        this.rect0.setMin(position.x - width, position.y - width, position.z);
        this.rect0.setMax(position.x + width, position.y + width, position.z + height);
        for (int i = 0; i < hitList.size(); ++i) {
            HitInfo hitInfo = hitList.get(i);
            this.rect1.setMin(hitInfo.x - width, hitInfo.y - width, hitInfo.z);
            this.rect1.setMax(hitInfo.x + width, hitInfo.y + width, hitInfo.z + height);
            if (!this.isOccluded(origin, direction, this.rect0, this.rect1)) continue;
            return true;
        }
        return false;
    }

    private boolean isOccluded(Vector3f origin, Vector3f direction, Rect3D rect0, Rect3D rect1) {
        float t1;
        float t0 = rect0.rayIntersection(origin, direction);
        return t0 > (t1 = rect1.rayIntersection(origin, direction));
    }

    private void processBallisticsTargets(IsoGameCharacter owner, HandWeapon weapon, PZArrayList<HitInfo> hitList) {
        BallisticsController ballisticsController;
        if (!hitList.isEmpty()) {
            for (int i = 0; i < hitList.size(); ++i) {
                IsoGameCharacter character;
                BallisticsTarget ballisticsTarget;
                IsoMovingObject isoMovingObject;
                HitInfo hitInfo = hitList.get(i);
                if (hitInfo.getObject() instanceof IsoAnimal || !((isoMovingObject = hitInfo.getObject()) instanceof IsoGameCharacter) || (ballisticsTarget = (character = (IsoGameCharacter)isoMovingObject).ensureExistsBallisticsTarget(character)) == null) continue;
                this.highlightTarget(character, Color.yellow, 0.65f);
                ballisticsTarget.add();
            }
        }
        if ((ballisticsController = owner.getBallisticsController()) == null) {
            return;
        }
        if (weapon.isRanged()) {
            targetReticleMode = 1;
            float range = weapon.getMaxRange(owner);
            ballisticsController.setRange(range);
            if (weapon.isRangeFalloff()) {
                int projectileCount = weapon.getProjectileCount();
                float projectileSpread = weapon.getProjectileSpread();
                float projectileWeightCenter = weapon.getProjectileWeightCenter();
                owner.getBallisticsController().getCameraTargets(range + 0.5f, true);
                owner.getBallisticsController().getSpreadData(range, projectileSpread, projectileWeightCenter, projectileCount);
            } else {
                owner.getBallisticsController().getCameraTargets(range + 0.5f, true);
                owner.getBallisticsController().getTargets(range);
            }
        }
        if (!hitList.isEmpty()) {
            for (int i = 0; i < hitList.size(); ++i) {
                HitInfo hitInfo = hitList.get(i);
                IsoMovingObject projectileWeightCenter = hitInfo.getObject();
                if (projectileWeightCenter instanceof BaseVehicle) {
                    BaseVehicle baseVehicle = (BaseVehicle)projectileWeightCenter;
                    boolean wasVehicleHit = this.checkHitVehicle(owner, baseVehicle);
                    if (!wasVehicleHit) {
                        hitList.remove(i);
                        --i;
                        continue;
                    }
                    owner.getBallisticsController().setBallisticsTargetHitLocation(baseVehicle.getId(), hitInfo);
                    if (!DebugOptions.instance.physicsRenderBallisticsTargets.getValue()) continue;
                    LineDrawer.DrawIsoCircle(hitInfo.x, hitInfo.y, hitInfo.z, 0.1f, CombatManager.TargetableDebugColor.r, CombatManager.TargetableDebugColor.g, CombatManager.TargetableDebugColor.b, 1.0f);
                    continue;
                }
                IsoMovingObject wasVehicleHit = hitInfo.getObject();
                if (!(wasVehicleHit instanceof IsoGameCharacter)) continue;
                IsoGameCharacter isoGameCharacter = (IsoGameCharacter)wasVehicleHit;
                owner.getBallisticsController().setBallisticsTargetHitLocation(isoGameCharacter.getID(), hitInfo);
                owner.getBallisticsController().setBallisticsCameraTargetHitLocation(isoGameCharacter.getID(), hitInfo);
                BallisticsTarget ballisticsTarget = isoGameCharacter.getBallisticsTarget();
                if (ballisticsTarget == null) continue;
                int id = isoGameCharacter.getID();
                if (ballisticsController.isValidTarget(id) || ballisticsController.isValidCachedTarget(id)) {
                    if (ballisticsController.isCameraTarget(id) || ballisticsController.isCachedCameraTarget(id)) {
                        this.highlightTarget(isoGameCharacter, Color.red, 0.65f);
                    }
                    if (!ballisticsController.isSpreadTarget(id) && !ballisticsController.isCachedSpreadTarget(id)) continue;
                    this.highlightTarget(isoGameCharacter, Color.magenta, 0.65f);
                    continue;
                }
                this.highlightTarget(isoGameCharacter, Color.white, 0.65f);
                hitList.remove(i);
                --i;
            }
        }
        if (!weapon.isRangeFalloff()) {
            return;
        }
        if (!hitList.isEmpty()) {
            int hitListSize = hitList.size();
            for (int i = 0; i < hitListSize; ++i) {
                int count;
                IsoGameCharacter isoGameCharacter;
                BallisticsTarget ballisticsTarget;
                HitInfo hitInfo = hitList.get(i);
                IsoMovingObject id = hitInfo.getObject();
                if (!(id instanceof IsoGameCharacter) || (ballisticsTarget = (isoGameCharacter = (IsoGameCharacter)id).getBallisticsTarget()) == null || (count = ballisticsController.spreadCount(isoGameCharacter.getID())) <= 1) continue;
                for (int j = 0; j < count - 1; ++j) {
                    HitInfo copy = this.hitInfoPool.alloc().init(hitInfo);
                    hitList.add(copy);
                }
            }
        }
    }

    private void CalcHitListWindow(IsoGameCharacter owner, HandWeapon weapon, PZArrayList<HitInfo> hitList) {
        Vector2 lookVector = owner.getLookVector(tempVector2_1);
        lookVector.setLength(weapon.getMaxRange(owner) * weapon.getRangeMod(owner));
        HitInfo hitInfo = null;
        ArrayList<IsoWindow> windows = IsoWorld.instance.currentCell.getWindowList();
        for (int i = 0; i < windows.size(); ++i) {
            IsoGridSquare square;
            IsoWindow window = windows.get(i);
            if (PZMath.fastfloor(window.getZ()) != PZMath.fastfloor(owner.getZ()) || !this.windowVisitor.isHittable(window)) continue;
            float windowX1 = window.getX();
            float windowY1 = window.getY();
            float windowX2 = windowX1 + (window.getNorth() ? 1.0f : 0.0f);
            float windowY2 = windowY1 + (window.getNorth() ? 0.0f : 1.0f);
            if (!Line2D.linesIntersect(owner.getX(), owner.getY(), owner.getX() + lookVector.x, owner.getY() + lookVector.y, windowX1, windowY1, windowX2, windowY2) || (square = window.getAddSheetSquare(owner)) == null || LosUtil.lineClearCollide(PZMath.fastfloor(owner.getX()), PZMath.fastfloor(owner.getY()), PZMath.fastfloor(owner.getZ()), square.x, square.y, square.z, false)) continue;
            float distSq = IsoUtils.DistanceToSquared(owner.getX(), owner.getY(), windowX1 + (windowX2 - windowX1) / 2.0f, windowY1 + (windowY2 - windowY1) / 2.0f);
            if (hitInfo != null && hitInfo.distSq < distSq) continue;
            float dot = 1.0f;
            if (hitInfo == null) {
                hitInfo = this.hitInfoPool.alloc();
            }
            hitInfo.init(window, 1.0f, distSq);
        }
        if (hitInfo != null) {
            hitList.add(hitInfo);
        }
    }

    public void calculateHitInfoList(IsoGameCharacter owner) {
        if (GameClient.client && !owner.isLocal()) {
            owner.updateAimingMode();
            return;
        }
        PZArrayList<HitInfo> hitInfoList = owner.getHitInfoList();
        if (!hitInfoList.isEmpty()) {
            owner.updateAimingMode();
            return;
        }
        AttackVars attackVars = owner.getAttackVars();
        HandWeapon weapon = attackVars.getWeapon((IsoLivingCharacter)owner);
        int maxHit = weapon.getMaxHitCount();
        if (attackVars.doShove) {
            int n = maxHit = WeaponType.getWeaponType(owner) != WeaponType.UNARMED ? 3 : 1;
        }
        if (!weapon.isRanged() && !SandboxOptions.instance.multiHitZombies.getValue()) {
            maxHit = 1;
        }
        if (weapon == ((IsoPlayer)owner).bareHands && !(owner.getPrimaryHandItem() instanceof HandWeapon)) {
            maxHit = 1;
        }
        if (weapon == ((IsoPlayer)owner).bareHands && attackVars.targetOnGround.getObject() != null) {
            maxHit = 1;
        }
        if (maxHit <= 0) {
            owner.updateAimingMode();
            return;
        }
        if (attackVars.doShove) {
            this.calcHitListShove(owner, attackVars, hitInfoList);
        } else if (attackVars.doGrapple) {
            this.calcHitListGrapple(owner, attackVars, hitInfoList);
        } else {
            this.calculateHitListWeapon(owner, attackVars, hitInfoList);
        }
        if (hitInfoList.size() == 1 && hitInfoList.get(0).getObject() == null) {
            owner.updateAimingMode();
            return;
        }
        float ownerZ = owner.getZ();
        if (weapon.isRanged() && !weapon.isRangeFalloff()) {
            this.filterTargetsByZ(ownerZ, hitInfoList);
        }
        TargetComparator targetComparator = this.meleeTargetComparator;
        if (weapon.isRanged()) {
            targetComparator = this.rangeTargetComparator;
        }
        BallisticsController ballisticsController = owner.getBallisticsController();
        targetComparator.setBallisticsController(ballisticsController);
        this.timSort.doSort(hitInfoList.getElements(), targetComparator, 0, hitInfoList.size());
        if (!weapon.isRanged()) {
            while (hitInfoList.size() > maxHit) {
                this.hitInfoPool.release((HitInfo)hitInfoList.removeLast());
            }
        }
        if (weapon.isRanged()) {
            HitList2.clear();
            int hitCount = 0;
            double referenceAngle = -1.0;
            for (int i = 0; i < hitInfoList.size(); ++i) {
                HitInfo hitInfo = hitInfoList.get(i);
                IsoMovingObject obj = hitInfo.getObject();
                int id = obj.getID();
                if (obj instanceof BaseVehicle) {
                    BaseVehicle baseVehicle = (BaseVehicle)obj;
                    if (!ballisticsController.isValidTarget(baseVehicle.vehicleId)) continue;
                }
                if (obj instanceof IsoGameCharacter && !(obj instanceof IsoAnimal) && !ballisticsController.isValidTarget(id)) continue;
                if (weapon.isPiercingBullets()) {
                    double angleDeg = Math.toDegrees(Math.atan2(owner.getY() - obj.getY(), obj.getX() - owner.getX()));
                    if (referenceAngle < 0.0) {
                        referenceAngle = angleDeg;
                    } else if (Math.abs(referenceAngle - angleDeg) >= 1.0) continue;
                }
                HitList2.add(hitInfo);
                hitInfoList.remove(i--);
                if (++hitCount >= maxHit) break;
            }
            this.hitInfoPool.release((List<HitInfo>)hitInfoList);
            hitInfoList.clear();
            hitInfoList.addAll(HitList2);
            HitList2.clear();
        }
        for (int i = 0; i < hitInfoList.size(); ++i) {
            HitInfo hitInfo = hitInfoList.get(i);
            HitChanceData hitChanceData = this.calculateHitChanceData(owner, weapon, hitInfo);
            hitInfo.chance = (int)hitChanceData.hitChance;
            if (!DebugOptions.instance.character.debug.alwaysHitTarget.getValue()) continue;
            hitInfo.chance = (int)this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE);
        }
        owner.updateAimingMode();
    }

    private void filterTargetsByZ(float ownerZ, PZArrayList<HitInfo> hitList) {
        float deltaZ;
        float targetZ;
        IsoMovingObject object;
        HitInfo hitInfo;
        int i;
        float minDeltaZ = Float.MAX_VALUE;
        HitInfo hitInfoMinDeltaZ = null;
        for (i = 0; i < hitList.size(); ++i) {
            hitInfo = hitList.get(i);
            object = hitInfo.getObject();
            targetZ = object == null ? hitInfo.z : object.getZ();
            deltaZ = Math.abs(targetZ - ownerZ);
            if (!(deltaZ < minDeltaZ)) continue;
            minDeltaZ = deltaZ;
            hitInfoMinDeltaZ = hitInfo;
        }
        if (hitInfoMinDeltaZ == null) {
            return;
        }
        for (i = hitList.size() - 1; i >= 0; --i) {
            hitInfo = hitList.get(i);
            if (hitInfo == hitInfoMinDeltaZ || !((deltaZ = Math.abs((targetZ = (object = hitInfo.getObject()) == null ? hitInfo.z : object.getZ()) - hitInfoMinDeltaZ.z)) > 0.5f)) continue;
            this.hitInfoPool.release(hitInfo);
            hitList.remove(i);
        }
    }

    private static int getBoneIndex(IsoMovingObject target, String boneName) {
        IsoGameCharacter isoGameCharacter = Type.tryCastTo(target, IsoGameCharacter.class);
        if (isoGameCharacter == null || boneName == null) {
            return -1;
        }
        AnimationPlayer animPlayer = isoGameCharacter.getAnimationPlayer();
        if (animPlayer == null || !animPlayer.isReady()) {
            return -1;
        }
        return animPlayer.getSkinningBoneIndex(boneName, -1);
    }

    public static Vector3 getBoneWorldPos(IsoMovingObject target, String boneName, Vector3 bonePos) {
        int boneIndex = CombatManager.getBoneIndex(target, boneName);
        if (boneIndex == -1) {
            return bonePos.set(target.getX(), target.getY(), target.getZ());
        }
        Model.boneToWorldCoords((IsoGameCharacter)target, boneIndex, bonePos);
        return bonePos;
    }

    private static Vector3 getPointAlongBoneXAxis(IsoMovingObject target, String boneName, float distance, Vector3 bonePos) {
        int boneIndex = CombatManager.getBoneIndex(target, boneName);
        if (boneIndex == -1) {
            return bonePos.set(target.getX(), target.getY(), target.getZ());
        }
        AnimationPlayer animPlayer = ((IsoGameCharacter)target).getAnimationPlayer();
        Matrix4f boneMtx = animPlayer.getModelTransformAt(boneIndex);
        float posx = boneMtx.m03;
        float posy = boneMtx.m13;
        float posz = boneMtx.m23;
        float xAxisx = boneMtx.m00;
        float xAxisy = boneMtx.m10;
        float xAxisz = boneMtx.m20;
        return bonePos.set(posx + xAxisx * distance, posy + xAxisy * distance, posz + xAxisz * distance);
    }

    public float getDistanceModifierSightless(float dist, boolean prone) {
        float pointBlankDistance = this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE);
        if (dist > pointBlankDistance) {
            return (pointBlankDistance - dist) * (this.combatConfig.get(CombatConfigKey.SIGHTLESS_TO_HIT_BASE_DISTANCE) + (pointBlankDistance - dist) * -this.combatConfig.get(CombatConfigKey.POINT_BLANK_DROP_OFF_TO_HIT_PENALTY));
        }
        if (dist < pointBlankDistance) {
            return (pointBlankDistance - dist) / pointBlankDistance * this.combatConfig.get(CombatConfigKey.POINT_BLANK_TO_HIT_MAXIMUM_BONUS) * (prone ? this.combatConfig.get(CombatConfigKey.SIGHTLESS_TO_HIT_PRONE_MODIFIER) : 1.0f);
        }
        return 0.0f;
    }

    public float getAimDelayPenaltySightless(float aimDelay, float dist) {
        float pointBlankDistance = this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE);
        if (dist < pointBlankDistance) {
            aimDelay *= dist / pointBlankDistance;
        } else if (dist > pointBlankDistance) {
            aimDelay *= 1.0f + (dist - pointBlankDistance * this.combatConfig.get(CombatConfigKey.SIGHTLESS_AIM_DELAY_TO_HIT_DISTANCE_MODIFIER));
        }
        return aimDelay;
    }

    public float getDistanceModifier(float dist, float min, float max, boolean prone) {
        if (dist < min) {
            if (dist > this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
                return (dist - min) * (this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY) + (dist - min) * -this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY_INCREMENT));
            }
            return 0.0f;
        }
        if (dist > max) {
            return -((dist - max) * (this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY) + (dist - max) * this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY_INCREMENT)));
        }
        float scale = (max - min) * 0.5f;
        return (float)((double)this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS) * Math.exp(-PZMath.pow(dist - (min + scale), 2.0f) / PZMath.pow(2.0f * ((max - min) / 7.0f), 2.0f)));
    }

    public static float getMovePenalty(IsoGameCharacter character, float dist) {
        float penalty = character.getBeenMovingFor() * (1.0f - (float)(character.getPerkLevel(PerkFactory.Perks.Aiming) + character.getPerkLevel(PerkFactory.Perks.Nimble)) / 40.0f);
        penalty = dist < 10.0f ? (penalty *= dist / 10.0f) : (penalty *= 1.0f + (dist - 10.0f) * 0.07f);
        return penalty;
    }

    public float getAimDelayPenalty(float delay, float dist, float min, float max) {
        if (min > -1.0f && dist >= min && dist <= max) {
            float scale = (max - min) * 0.5f;
            delay *= 1.0f - (1.0f - Math.abs(dist - (min + scale)) / scale) * 0.25f;
        } else if (dist > max) {
            delay *= 1.0f + (dist - max) * 0.1f;
        }
        return delay *= dist < this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE) ? dist / this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE) : 1.0f;
    }

    public float getMoodlesPenalty(IsoGameCharacter character, float distance) {
        return ((float)character.getMoodles().getMoodleLevel(MoodleType.PANIC) * (this.combatConfig.get(CombatConfigKey.PANIC_TO_HIT_BASE_PENALTY) + distance * this.combatConfig.get(CombatConfigKey.PANIC_TO_HIT_DISTANCE_MODIFIER)) + (float)character.getMoodles().getMoodleLevel(MoodleType.STRESS) * (this.combatConfig.get(CombatConfigKey.STRESS_TO_HIT_BASE_PENALTY) + distance * this.combatConfig.get(CombatConfigKey.STRESS_TO_HIT_DISTANCE_MODIFIER)) + (float)character.getMoodles().getMoodleLevel(MoodleType.TIRED) * this.combatConfig.get(CombatConfigKey.TIRED_TO_HIT_BASE_PENALTY) + (float)character.getMoodles().getMoodleLevel(MoodleType.ENDURANCE) * this.combatConfig.get(CombatConfigKey.ENDURANCE_TO_HIT_BASE_PENALTY) + (float)character.getMoodles().getMoodleLevel(MoodleType.DRUNK) * (this.combatConfig.get(CombatConfigKey.DRUNK_TO_HIT_BASE_PENALTY) + distance * this.combatConfig.get(CombatConfigKey.DRUNK_TO_HIT_DISTANCE_MODIFIER))) * (float)SandboxOptions.instance.firearmMoodleMultiplier.getValue();
    }

    public float getWeatherPenalty(IsoGameCharacter character, HandWeapon weapon, IsoGridSquare square, float distance) {
        int n;
        boolean isThermal;
        float weatherPenalty = 0.0f;
        if (square == null) {
            return weatherPenalty;
        }
        boolean bl = isThermal = weapon.getActiveSight() != null && weapon.getActiveSight().hasTag(ItemTag.THERMAL);
        if (square.isOutside()) {
            weatherPenalty += ClimateManager.getInstance().getWindIntensity() * (this.combatConfig.get(CombatConfigKey.WIND_INTENSITY_TO_HIT_PENALTY) - (float)character.getPerkLevel(PerkFactory.Perks.Aiming) * this.combatConfig.get(CombatConfigKey.WIND_INTENSITY_TO_HIT_AIMING_MODIFIER)) * distance * (character.hasTrait(CharacterTrait.MARKSMAN) ? this.combatConfig.get(CombatConfigKey.WIND_INTENSITY_TO_HIT_MINIMUM_MARKSMAN_MODIFIER) : this.combatConfig.get(CombatConfigKey.WIND_INTENSITY_TO_HIT_MAXIMUM_MARKSMAN_MODIFIER));
            weatherPenalty += ClimateManager.getInstance().getRainIntensity() * distance * this.combatConfig.get(CombatConfigKey.RAIN_INTENSITY_TO_HIT_DISTANCE_MODIFIER);
            weatherPenalty *= character.getCharacterTraits().getTraitWeatherPenaltyModifier();
            if (!isThermal) {
                weatherPenalty += ClimateManager.getInstance().getFogIntensity() * this.combatConfig.get(CombatConfigKey.FOG_INTENSITY_DISTANCE_MODIFIER) * distance;
            }
            weatherPenalty *= PZMath.min(distance / this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE), this.combatConfig.get(CombatConfigKey.POINT_BLANK_MAXIMUM_DISTANCE_MODIFIER));
            weatherPenalty *= (float)SandboxOptions.instance.firearmWeatherMultiplier.getValue();
        }
        if (isThermal) {
            return weatherPenalty;
        }
        if (character instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)character;
            n = isoPlayer.getPlayerNum();
        } else {
            n = -1;
        }
        float light = square.getLightLevel(n);
        if (light < this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD)) {
            float lightPenalty = PZMath.max(0.0f, this.combatConfig.get(CombatConfigKey.LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY) * (1.0f - light / this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD)));
            weatherPenalty += lightPenalty - weapon.getLowLightBonus();
        }
        return weatherPenalty;
    }

    public float getPainPenalty(IsoGameCharacter character) {
        float armsPain = 0.0f;
        for (int x = BodyPartType.ToIndex(BodyPartType.Hand_L); x <= BodyPartType.ToIndex(BodyPartType.UpperArm_R); ++x) {
            armsPain += character.getBodyDamage().getBodyParts().get(x).getPain();
        }
        return armsPain * this.combatConfig.get(CombatConfigKey.ARM_PAIN_TO_HIT_MODIFIER);
    }

    private HitChanceData calculateHitChanceData(IsoGameCharacter owner, HandWeapon weapon, HitInfo hitInfo) {
        IsoMovingObject isoMovingObject;
        HitChanceData hitChanceData = new HitChanceData();
        IsoMovingObject target = null;
        if (hitInfo != null) {
            target = hitInfo.getObject();
        }
        float hitChance = Math.min((float)weapon.getHitChance(), this.combatConfig.get(CombatConfigKey.MAXIMUM_START_TO_HIT_CHANCE));
        hitChance += weapon.getAimingPerkHitChanceModifier() * (float)owner.getPerkLevel(PerkFactory.Perks.Aiming);
        if (owner.getVehicle() != null && target != null) {
            BaseVehicle v = owner.getVehicle();
            Vector3f fwd = v.getForwardVector((Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc());
            Vector2 outwindowdirection = (Vector2)Vector2ObjectPool.get().alloc();
            outwindowdirection.x = fwd.x;
            outwindowdirection.y = fwd.z;
            outwindowdirection.normalize();
            Vector2 zombieDirection = (Vector2)Vector2ObjectPool.get().alloc();
            zombieDirection.x = target.getX();
            zombieDirection.y = target.getY();
            zombieDirection.x -= owner.getX();
            zombieDirection.y -= owner.getY();
            zombieDirection.normalize();
            boolean backwards = zombieDirection.dot(outwindowdirection) < 0.0f;
            int seat = v.getSeat(owner);
            VehicleScript.Area area = v.getScript().getAreaById(v.getPassengerArea(seat));
            int rotation = area.x > 0.0f ? 90 : -90;
            outwindowdirection.rotate((float)Math.toRadians(rotation));
            outwindowdirection.normalize();
            float facing = zombieDirection.dot(outwindowdirection);
            Vector2ObjectPool.get().release(outwindowdirection);
            Vector2ObjectPool.get().release(zombieDirection);
            BaseVehicle.TL_vector3f_pool.get().release(fwd);
            if (facing > -0.6f && !weapon.isRanged()) {
                return hitChanceData;
            }
            if (facing > this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_MAXIMUM_ANGLE)) {
                return hitChanceData;
            }
            if (!weapon.isRanged()) {
                hitChanceData.hitChance = this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE);
                return hitChanceData;
            }
            if (owner.isDriving() && weapon.isTwoHandWeapon()) {
                return hitChanceData;
            }
            if (facing > this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_OPTIMAL_ANGLE)) {
                VehiclePart part;
                float maxFacing = this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_MAXIMUM_ANGLE);
                if (v.isDriver(owner)) {
                    maxFacing -= 0.15f;
                }
                if ((part = v.getPartForSeatContainer(seat)) != null) {
                    maxFacing -= part.getItemContainer().getCapacityWeight() * (weapon.isTwoHandWeapon() ? 0.05f : 0.025f);
                }
                maxFacing -= owner.getInventory().getCapacityWeight() * 0.01f;
                if (backwards) {
                    maxFacing -= weapon.isTwoHandWeapon() ? 0.15f : 0.1f;
                }
                float penalty = PZMath.clamp(this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_TO_HIT_MAXIMUM_PENALTY) * (1.0f - (facing - 0.1f) / (this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_OPTIMAL_ANGLE) - 0.1f)), 0.0f, this.combatConfig.get(CombatConfigKey.DRIVEBY_DOT_TO_HIT_MAXIMUM_PENALTY)) * (weapon.isTwoHandWeapon() ? 1.5f : 1.0f) * (backwards ? 1.5f : 1.0f) * (facing <= maxFacing ? 1.0f : 3.0f);
                hitChance -= penalty;
                hitChanceData.aimPenalty += penalty;
            }
            float vehicleSpeedPenalty = Math.abs(v.getCurrentSpeedKmHour()) * (v.isDriver(owner) ? 3.0f : 2.0f);
            hitChance -= vehicleSpeedPenalty;
            hitChanceData.aimPenalty += vehicleSpeedPenalty;
        }
        if (!weapon.isRanged()) {
            hitChanceData.hitChance = this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE);
            return hitChanceData;
        }
        float max = weapon.getMaxSightRange(owner);
        float min = weapon.getMinSightRange(owner);
        float dist = hitInfo != null ? PZMath.sqrt(hitInfo.distSq) : max;
        boolean isProne = false;
        if (target != null) {
            isProne = target.isProne();
        }
        float delayPenaltySightless = this.getAimDelayPenaltySightless(PZMath.max(0.0f, owner.getAimingDelay()), dist);
        float delayPenaltySights = this.getAimDelayPenalty(PZMath.max(0.0f, owner.getAimingDelay()), dist, min, max);
        float distanceModifierSightless = this.getDistanceModifierSightless(dist, isProne);
        float distanceModifierSights = this.getDistanceModifier(dist, min, max, isProne);
        float combinedPenalty = 0.0f;
        float sightsPenalty = PZMath.max(distanceModifierSightless - delayPenaltySightless, distanceModifierSights - delayPenaltySights);
        hitChance += sightsPenalty;
        hitChanceData.aimPenalty -= sightsPenalty;
        float modifier = CombatManager.getMovePenalty(owner, dist);
        combinedPenalty += modifier;
        if (hitInfo != null && (isoMovingObject = hitInfo.getObject()) instanceof IsoPlayer) {
            IsoPlayer plyr = (IsoPlayer)isoMovingObject;
            if (plyr.getVehicle() != null) {
                float vehicleSpeedPenalty = Math.abs(plyr.getVehicle().getCurrentSpeedKmHour()) * 2.0f;
                hitChance -= vehicleSpeedPenalty;
                hitChanceData.aimPenalty += vehicleSpeedPenalty;
            } else if (plyr.isSprinting()) {
                hitChance -= this.combatConfig.get(CombatConfigKey.SPRINTING_TO_HIT_PENALTY);
                hitChanceData.aimPenalty += this.combatConfig.get(CombatConfigKey.SPRINTING_TO_HIT_PENALTY);
            } else if (plyr.isRunning()) {
                hitChance -= this.combatConfig.get(CombatConfigKey.RUNNING_TO_HIT_PENALTY);
                hitChanceData.aimPenalty += this.combatConfig.get(CombatConfigKey.RUNNING_TO_HIT_PENALTY);
            } else if (plyr.isPlayerMoving()) {
                hitChance -= this.combatConfig.get(CombatConfigKey.MOVING_TO_HIT_PENALTY);
                hitChanceData.aimPenalty += this.combatConfig.get(CombatConfigKey.MOVING_TO_HIT_PENALTY);
            }
        }
        if (owner.hasTrait(CharacterTrait.MARKSMAN)) {
            hitChance += this.combatConfig.get(CombatConfigKey.MARKSMAN_TRAIT_TO_HIT_BONUS);
        }
        modifier = this.getPainPenalty(owner);
        combinedPenalty += modifier;
        IsoGridSquare isoGridSquare = owner.getSquare();
        if (target != null) {
            isoGridSquare = target.getSquare();
        }
        modifier = this.getWeatherPenalty(owner, weapon, isoGridSquare, dist);
        combinedPenalty += modifier;
        modifier = this.getMoodlesPenalty(owner, dist);
        combinedPenalty += modifier;
        if (SandboxOptions.instance.firearmHeadGearEffect.getValue()) {
            modifier = this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE) - this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE) / owner.getWornItemsVisionModifier();
            combinedPenalty += modifier;
        }
        if (dist < this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
            combinedPenalty *= dist / this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE);
        }
        hitChanceData.aimPenalty += combinedPenalty;
        hitChanceData.hitChance = PZMath.clamp((float)((int)(hitChance -= combinedPenalty)), this.combatConfig.get(CombatConfigKey.MINIMUM_TO_HIT_CHANCE), this.combatConfig.get(CombatConfigKey.MAXIMUM_TO_HIT_CHANCE));
        return hitChanceData;
    }

    private LosUtil.TestResults los(int x0, int y0, int x1, int y1, int z, LOSVisitor visitor) {
        IsoCell cell = IsoWorld.instance.currentCell;
        int z0 = z;
        int z1 = z;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5f;
        float t2 = 0.5f;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
        if (Math.abs(dx) > Math.abs(dy)) {
            float m = (float)dy / (float)dx;
            float m2 = (float)dz / (float)dx;
            t += (float)y0;
            t2 += (float)z0;
            dx = dx < 0 ? -1 : 1;
            m *= (float)dx;
            m2 *= (float)dx;
            while (x0 != x1) {
                IsoGridSquare a = cell.getGridSquare(x0 += dx, PZMath.fastfloor(t += m), PZMath.fastfloor(t2 += m2));
                if (visitor.visit(a, b)) {
                    return visitor.getResult();
                }
                b = a;
            }
        } else {
            float m = (float)dx / (float)dy;
            float m2 = (float)dz / (float)dy;
            t += (float)x0;
            t2 += (float)z0;
            dy = dy < 0 ? -1 : 1;
            m *= (float)dy;
            m2 *= (float)dy;
            while (y0 != y1) {
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t += m), y0 += dy, PZMath.fastfloor(t2 += m2));
                if (visitor.visit(a, b)) {
                    return visitor.getResult();
                }
                b = a;
            }
        }
        return LosUtil.TestResults.Clear;
    }

    private IsoWindow getWindowBetween(int x0, int y0, int x1, int y1, int z) {
        this.windowVisitor.init();
        this.los(x0, y0, x1, y1, z, this.windowVisitor);
        return this.windowVisitor.window;
    }

    private IsoWindow getWindowBetween(IsoMovingObject a, IsoMovingObject b) {
        return this.getWindowBetween(PZMath.fastfloor(a.getX()), PZMath.fastfloor(a.getY()), PZMath.fastfloor(b.getX()), PZMath.fastfloor(b.getY()), PZMath.fastfloor(a.getZ()));
    }

    private boolean isWindowBetween(IsoMovingObject a, IsoMovingObject b) {
        return this.getWindowBetween(a, b) != null;
    }

    private void smashWindowBetween(IsoGameCharacter owner, IsoMovingObject target, HandWeapon weapon) {
        if (target == null) {
            return;
        }
        IsoWindow window = this.getWindowBetween(owner, target);
        if (window == null) {
            return;
        }
        window.smashWindow();
    }

    public void Reset() {
        this.hitInfoPool.forEach(hitInfo -> {
            hitInfo.object = null;
        });
    }

    private HitReaction resolveHitReaction(IsoGameCharacter wielder, IsoGameCharacter target, ShotDirection shotDirection) {
        boolean criticalHit = wielder.isCriticalHit();
        switch (shotDirection) {
            case NORTH: {
                if (criticalHit) {
                    return HitReaction.SHOT_HEAD_BWD;
                }
                return target.isHitFromBehind() ? HitReaction.SHOT_BELLY_STEP_BEHIND : (Rand.Next(2) == 0 ? HitReaction.SHOT_BELLY : HitReaction.SHOT_BELLY_STEP);
            }
            case SOUTH: {
                if (criticalHit) {
                    return Rand.Next(2) == 0 ? HitReaction.SHOT_HEAD_FWD : HitReaction.SHOT_HEAD_FWD02;
                }
                return target.isHitFromBehind() ? HitReaction.SHOT_BELLY_STEP_BEHIND : (Rand.Next(2) == 0 ? HitReaction.SHOT_BELLY : HitReaction.SHOT_BELLY_STEP);
            }
            case LEFT: 
            case RIGHT: {
                if (criticalHit && Rand.Next(4) == 0) {
                    return HitReaction.SHOT_HEAD_BWD;
                }
                if (target.isHitFromBehind()) {
                    return switch (Rand.Next(3)) {
                        case 0 -> {
                            if (shotDirection == ShotDirection.LEFT) {
                                yield HitReaction.SHOT_CHEST_L;
                            }
                            yield HitReaction.SHOT_CHEST_R;
                        }
                        case 1 -> {
                            if (shotDirection == ShotDirection.LEFT) {
                                yield HitReaction.SHOT_LEG_L;
                            }
                            yield HitReaction.SHOT_LEG_R;
                        }
                        default -> shotDirection == ShotDirection.LEFT ? HitReaction.SHOT_SHOULDER_STEP_L : HitReaction.SHOT_SHOULDER_STEP_R;
                    };
                }
                return switch (Rand.Next(5)) {
                    case 0 -> {
                        if (shotDirection == ShotDirection.LEFT) {
                            yield HitReaction.SHOT_CHEST_L;
                        }
                        yield HitReaction.SHOT_CHEST_R;
                    }
                    case 1 -> {
                        if (shotDirection == ShotDirection.LEFT) {
                            yield HitReaction.SHOT_CHEST_STEP_L;
                        }
                        yield HitReaction.SHOT_CHEST_STEP_R;
                    }
                    case 2 -> {
                        if (shotDirection == ShotDirection.LEFT) {
                            yield HitReaction.SHOT_LEG_L;
                        }
                        yield HitReaction.SHOT_LEG_R;
                    }
                    case 3 -> {
                        if (shotDirection == ShotDirection.LEFT) {
                            yield HitReaction.SHOT_SHOULDER_L;
                        }
                        yield HitReaction.SHOT_SHOULDER_R;
                    }
                    default -> shotDirection == ShotDirection.LEFT ? HitReaction.SHOT_SHOULDER_STEP_L : HitReaction.SHOT_SHOULDER_STEP_R;
                };
            }
        }
        return HitReaction.NONE;
    }

    public int processHit(HandWeapon weapon, IsoGameCharacter wielder, IsoGameCharacter target) {
        IsoZombie isoZombie;
        HitReaction hitReaction = HitReaction.fromString(wielder.getVariableString("ZombieHitReaction"));
        this.setUsePhysicHitReaction(weapon, target);
        ShotDirection shotDirection = this.calculateShotDirection(wielder, target);
        if (hitReaction == HitReaction.SHOT) {
            int targetedBodyPart;
            wielder.setCriticalHit(Rand.Next(100) < ((IsoPlayer)wielder).calculateCritChance(target));
            BallisticsController ballisticsController = wielder.getBallisticsController();
            boolean isCameraTarget = ballisticsController.isCameraTarget(target.getID());
            int n = targetedBodyPart = isCameraTarget ? ballisticsController.getCachedTargetedBodyPart(target.getID()) : RagdollBodyPart.BODYPART_COUNT.ordinal();
            if (isCameraTarget) {
                String bodyPartName = RagdollBodyPart.values()[targetedBodyPart].name();
                DebugType.Combat.debugln("CombatManager::ProcessHit %d isCameraTarget and hit BodyPart %d - %s", target.getID(), targetedBodyPart, bodyPartName);
            }
            if (targetedBodyPart != RagdollBodyPart.BODYPART_COUNT.ordinal()) {
                this.processTargetedHit(weapon, wielder, target, RagdollBodyPart.values()[targetedBodyPart], shotDirection);
                return targetedBodyPart;
            }
            hitReaction = this.resolveHitReaction(wielder, target, shotDirection);
        }
        this.applyBlood(weapon, target, hitReaction, shotDirection);
        if (target instanceof IsoZombie && (isoZombie = (IsoZombie)target).getEatBodyTarget() != null) {
            HitReaction hitReaction2 = hitReaction = target.getVariableBoolean("onknees") ? HitReaction.ON_KNEES : HitReaction.EATING;
        }
        if (hitReaction == HitReaction.FLOOR && target.isCurrentState(ZombieGetUpState.instance()) && target.isFallOnFront()) {
            hitReaction = HitReaction.GETTING_UP_FRONT;
        }
        if (hitReaction != HitReaction.NONE && !target.isAnimalRunningToDeathPosition()) {
            target.setHitReaction(hitReaction.getValue());
        } else {
            if (target instanceof IsoZombie) {
                isoZombie = (IsoZombie)target;
                isoZombie.setStaggerBack(true);
            }
            target.setHitReaction("");
        }
        RagdollBodyPart bodyPart = this.getBodyPart(hitReaction, shotDirection);
        this.createCombatData(weapon, wielder, target, bodyPart);
        return bodyPart.ordinal();
    }

    private RagdollBodyPart getBodyPart(HitReaction hitReaction, ShotDirection shotDirection) {
        switch (hitReaction) {
            case SHOT_HEAD_FWD: 
            case SHOT_HEAD_FWD02: 
            case SHOT_HEAD_BWD: {
                return RagdollBodyPart.BODYPART_HEAD;
            }
            case SHOT_CHEST: 
            case SHOT_CHEST_L: 
            case SHOT_CHEST_R: 
            case SHOT_CHEST_STEP_L: 
            case SHOT_CHEST_STEP_R: {
                return RagdollBodyPart.BODYPART_SPINE;
            }
            case SHOT_BELLY: 
            case SHOT_BELLY_STEP: 
            case SHOT_BELLY_STEP_BEHIND: {
                return RagdollBodyPart.BODYPART_PELVIS;
            }
            case SHOT_LEG_L: 
            case SHOT_LEG_R: {
                boolean lower;
                boolean bl = lower = Rand.Next(2) == 0;
                if (shotDirection == ShotDirection.LEFT) {
                    return lower ? RagdollBodyPart.BODYPART_LEFT_LOWER_LEG : RagdollBodyPart.BODYPART_LEFT_UPPER_LEG;
                }
                return lower ? RagdollBodyPart.BODYPART_RIGHT_LOWER_LEG : RagdollBodyPart.BODYPART_RIGHT_UPPER_LEG;
            }
            case SHOT_SHOULDER_L: 
            case SHOT_SHOULDER_STEP_L: 
            case SHOT_SHOULDER_R: 
            case SHOT_SHOULDER_STEP_R: {
                boolean lower;
                boolean bl = lower = Rand.Next(2) == 0;
                if (shotDirection == ShotDirection.LEFT) {
                    return lower ? RagdollBodyPart.BODYPART_LEFT_LOWER_ARM : RagdollBodyPart.BODYPART_LEFT_UPPER_ARM;
                }
                return lower ? RagdollBodyPart.BODYPART_RIGHT_LOWER_ARM : RagdollBodyPart.BODYPART_RIGHT_UPPER_ARM;
            }
        }
        return RagdollBodyPart.BODYPART_SPINE;
    }

    private ShotDirection calculateShotDirection(IsoGameCharacter wielder, IsoGameCharacter target) {
        double deg;
        Vector2 playerFwd = wielder.getForwardDirection();
        Vector2 zombieFwd = target.getForwardDirection();
        double crossProduct = playerFwd.x * zombieFwd.y - playerFwd.y * zombieFwd.x;
        double crossProductSign = crossProduct >= 0.0 ? 1.0 : -1.0;
        double dotProduct = playerFwd.x * zombieFwd.x + playerFwd.y * zombieFwd.y;
        double angleBetween = Math.acos(dotProduct) * crossProductSign;
        if (angleBetween < 0.0) {
            angleBetween += Math.PI * 2;
        }
        if ((deg = Math.toDegrees(angleBetween)) < 45.0) {
            int rng = Rand.Next(9);
            if (rng > 6) {
                return ShotDirection.LEFT;
            }
            if (rng > 4) {
                return ShotDirection.RIGHT;
            }
            return ShotDirection.SOUTH;
        }
        if (deg < 90.0) {
            return Rand.Next(4) == 0 ? ShotDirection.SOUTH : ShotDirection.RIGHT;
        }
        if (deg < 135.0) {
            return ShotDirection.RIGHT;
        }
        if (deg < 180.0) {
            return Rand.Next(4) == 0 ? ShotDirection.NORTH : ShotDirection.RIGHT;
        }
        if (deg < 225.0) {
            int rng = Rand.Next(9);
            if (rng > 6) {
                return ShotDirection.LEFT;
            }
            if (rng > 4) {
                return ShotDirection.RIGHT;
            }
            return ShotDirection.NORTH;
        }
        if (deg < 270.0) {
            return Rand.Next(4) == 0 ? ShotDirection.NORTH : ShotDirection.LEFT;
        }
        if (deg < 315.0) {
            return ShotDirection.LEFT;
        }
        return Rand.Next(4) == 0 ? ShotDirection.SOUTH : ShotDirection.LEFT;
    }

    private void applyBlood(HandWeapon weapon, IsoGameCharacter target, HitReaction hitReaction, ShotDirection shotDirection) {
        boolean criticalHit = target.isCriticalHit();
        switch (hitReaction) {
            case SHOT_HEAD_FWD: 
            case SHOT_HEAD_FWD02: 
            case SHOT_HEAD_BWD: {
                target.addBlood(BloodBodyPartType.Head, false, true, true);
                break;
            }
            case SHOT_CHEST: 
            case SHOT_CHEST_L: 
            case SHOT_CHEST_R: 
            case SHOT_CHEST_STEP_L: 
            case SHOT_CHEST_STEP_R: {
                target.addBlood(BloodBodyPartType.Torso_Upper, !criticalHit, criticalHit, true);
                break;
            }
            case SHOT_BELLY: 
            case SHOT_BELLY_STEP: {
                target.addBlood(BloodBodyPartType.Torso_Lower, !criticalHit, criticalHit, true);
                break;
            }
            case SHOT_LEG_L: 
            case SHOT_LEG_R: {
                boolean lower;
                boolean bl = lower = Rand.Next(2) == 0;
                if (shotDirection == ShotDirection.LEFT) {
                    target.addBlood(lower ? BloodBodyPartType.LowerLeg_L : BloodBodyPartType.UpperLeg_L, !criticalHit, criticalHit, true);
                    break;
                }
                target.addBlood(lower ? BloodBodyPartType.LowerLeg_R : BloodBodyPartType.UpperLeg_R, !criticalHit, criticalHit, true);
                break;
            }
            case SHOT_SHOULDER_L: 
            case SHOT_SHOULDER_STEP_L: 
            case SHOT_SHOULDER_R: 
            case SHOT_SHOULDER_STEP_R: {
                boolean lower;
                boolean bl = lower = Rand.Next(2) == 0;
                if (shotDirection == ShotDirection.LEFT) {
                    target.addBlood(lower ? BloodBodyPartType.ForeArm_L : BloodBodyPartType.UpperArm_L, !criticalHit, criticalHit, true);
                    break;
                }
                target.addBlood(lower ? BloodBodyPartType.ForeArm_R : BloodBodyPartType.UpperArm_R, !criticalHit, criticalHit, true);
                break;
            }
            default: {
                if (weapon.isOfWeaponCategory(WeaponCategory.BLUNT)) {
                    target.addBlood(BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperArm_L.index(), BloodBodyPartType.Groin.index())), false, false, true);
                    break;
                }
                if (weapon.isOfWeaponCategory(WeaponCategory.UNARMED)) break;
                target.addBlood(BloodBodyPartType.FromIndex(Rand.Next(BloodBodyPartType.UpperArm_L.index(), BloodBodyPartType.Groin.index())), false, true, true);
            }
        }
    }

    public void highlightTarget(IsoGameCharacter isoGameCharacter, Color color, float alpha) {
        if (!DebugOptions.instance.physicsRenderHighlightBallisticsTargets.getValue()) {
            return;
        }
        isoGameCharacter.setOutlineHighlight(true);
        isoGameCharacter.setOutlineHighlightCol(color.r, color.g, color.b, alpha);
    }

    private void highlightTargets(IsoPlayer isoPlayer) {
        boolean isRanged = false;
        InventoryItem inventoryItem = isoPlayer.getPrimaryHandItem();
        if (inventoryItem instanceof HandWeapon) {
            HandWeapon handWeapon = (HandWeapon)inventoryItem;
            isRanged = handWeapon.isRanged();
        }
        if (DebugOptions.instance.character.debug.render.meleeOutline.getValue() && !isRanged) {
            this.highlightMeleeTargets(isoPlayer, Color.cyan.r, Color.cyan.g, Color.cyan.b, 0.65f);
        }
        if (isRanged) {
            return;
        }
        if (!Core.getInstance().getOptionMeleeOutline()) {
            return;
        }
        if (!isoPlayer.isLocalPlayer() || isoPlayer.isNPC() || !isoPlayer.isAiming()) {
            return;
        }
        ColorInfo badColor = Core.getInstance().getBadHighlitedColor();
        this.highlightMeleeTargets(isoPlayer, badColor.r, badColor.g, badColor.b, 1.0f);
    }

    private void highlightMeleeTargets(IsoPlayer isoPlayer, float r, float g, float b, float a) {
        this.calculateAttackVars(isoPlayer);
        this.calculateHitInfoList(isoPlayer);
        PZArrayList<HitInfo> hitInfoList = isoPlayer.getHitInfoList();
        for (int i = 0; i < hitInfoList.size(); ++i) {
            HitInfo hitInfo = hitInfoList.get(i);
            IsoMovingObject isoMovingObject = hitInfo.getObject();
            if (!(isoMovingObject instanceof IsoGameCharacter)) continue;
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            isoGameCharacter.setOutlineHighlight(isoPlayer.getPlayerNum(), true);
            isoGameCharacter.setOutlineHighlightCol(r, g, b, a);
        }
    }

    /*
     * Unable to fully structure code
     */
    public void pressedAttack(IsoPlayer isoPlayer) {
        block39: {
            isRemotePlayer = GameClient.client != false && isoPlayer.isLocalPlayer() == false;
            bWasSprinting = isoPlayer.isSprinting();
            isoPlayer.setSprinting(false);
            isoPlayer.setForceSprint(false);
            if (isoPlayer.isAttackStarted() || isoPlayer.isCurrentState(PlayerHitReactionState.instance())) {
                return;
            }
            if (isoPlayer.isCurrentState(FishingState.instance())) {
                return;
            }
            if (GameClient.client && isoPlayer.isCurrentState(PlayerHitReactionPVPState.instance()) && !ServerOptions.instance.pvpMeleeWhileHitReaction.getValue()) {
                return;
            }
            if (!isoPlayer.canPerformHandToHandCombat()) {
                isoPlayer.clearHandToHandAttack();
                return;
            }
            if (!isoPlayer.isDoShove() && !isoPlayer.isWeaponReady()) {
                return;
            }
            if (!isoPlayer.isAttackStarted()) {
                isoPlayer.setVariable("StartedAttackWhileSprinting", bWasSprinting);
            }
            isoPlayer.setInitiateAttack(true);
            isoPlayer.setAttackStarted(true);
            if (!isRemotePlayer) {
                isoPlayer.setCriticalHit(false);
            }
            isoPlayer.setAttackFromBehind(false);
            v0 = weaponType = isoPlayer.isDoShove() != false ? WeaponType.UNARMED : WeaponType.getWeaponType(isoPlayer);
            if (!GameClient.client || isoPlayer.isLocalPlayer()) {
                isoPlayer.setAttackType(weaponType.getPossibleAttack().getRandom());
            }
            if (!GameClient.client || isoPlayer.isLocalPlayer()) {
                isoPlayer.setCombatSpeed(isoPlayer.calculateCombatSpeed());
            }
            this.calculateAttackVars(isoPlayer);
            animVariableWeapon = isoPlayer.getVariableString("Weapon");
            if (animVariableWeapon != null && animVariableWeapon.equals("throwing") && !isoPlayer.getAttackVars().doShove) {
                isoPlayer.setAttackAnimThrowTimer(2000L);
                isoPlayer.setIsAiming(true);
            }
            if (isRemotePlayer) {
                isoPlayer.getAttackVars().doShove = isoPlayer.isDoShove();
                isoPlayer.getAttackVars().aimAtFloor = isoPlayer.isAimAtFloor();
                isoPlayer.getAttackVars().doGrapple = isoPlayer.isDoGrapple();
            }
            if (isoPlayer.getAttackVars().doShove && !isoPlayer.isAuthorizedHandToHand()) {
                isoPlayer.setDoShove(false);
                isoPlayer.setInitiateAttack(false);
                isoPlayer.setAttackStarted(false);
                isoPlayer.setAttackType(AttackType.NONE);
                return;
            }
            if (isoPlayer.getAttackVars().doGrapple && !isoPlayer.isAuthorizedHandToHand()) {
                isoPlayer.setDoGrapple(false);
                isoPlayer.setInitiateAttack(false);
                isoPlayer.setAttackStarted(false);
                isoPlayer.setAttackType(AttackType.NONE);
                return;
            }
            handWeapon = isoPlayer.getAttackVars().getWeapon(isoPlayer);
            isoPlayer.setUseHandWeapon(handWeapon);
            isoPlayer.setAimAtFloor(isoPlayer.getAttackVars().aimAtFloor);
            isoPlayer.setDoShove(isoPlayer.getAttackVars().doShove);
            isoPlayer.setDoGrapple(isoPlayer.getAttackVars().doGrapple);
            isoPlayer.targetOnGround = (IsoGameCharacter)isoPlayer.getAttackVars().targetOnGround.getObject();
            if (handWeapon != null && weaponType.isRanged()) {
                isoPlayer.setRecoilDelay(handWeapon.getRecoilDelay(isoPlayer));
            }
            if ((variation = Rand.Next(0, 3)) == 0) {
                isoPlayer.setAttackVariationX(Rand.Next(-1.0f, -0.5f));
            }
            if (variation == 1) {
                isoPlayer.setAttackVariationX(0.0f);
            }
            if (variation == 2) {
                isoPlayer.setAttackVariationX(Rand.Next(0.5f, 1.0f));
            }
            isoPlayer.setAttackVariationY(0.0f);
            this.calculateHitInfoList(isoPlayer);
            closestHitTarget = null;
            if (!isoPlayer.getHitInfoList().isEmpty()) {
                closestHitTarget = Type.tryCastTo(isoPlayer.getHitInfoList().get(0).getObject(), IsoGameCharacter.class);
            }
            if (closestHitTarget == null) {
                if (isoPlayer.isAiming() && !isoPlayer.isMeleePressed() && handWeapon != isoPlayer.bareHands) {
                    isoPlayer.setDoShove(false);
                }
                if (isoPlayer.isAiming() && !isoPlayer.isGrapplePressed() && handWeapon != isoPlayer.bareHands) {
                    isoPlayer.setDoGrapple(false);
                }
                isoPlayer.setLastAttackWasHandToHand(isoPlayer.isDoHandToHandAttack());
                if (weaponType.isCanMiss() && !isoPlayer.isAimAtFloor() && (!GameClient.client || isoPlayer.isLocalPlayer())) {
                    isoPlayer.setAttackType(AttackType.MISS);
                }
                return;
            }
            if (!GameClient.client || isoPlayer.isLocalPlayer()) {
                isoPlayer.setAttackFromBehind(isoPlayer.isBehind((IsoGameCharacter)closestHitTarget));
            }
            closestDist = IsoUtils.DistanceTo(closestHitTarget.getX(), closestHitTarget.getY(), isoPlayer.getX(), isoPlayer.getY());
            isoPlayer.setVariable("TargetDist", closestDist);
            criticalHitChance = isoPlayer.calculateCritChance((IsoGameCharacter)closestHitTarget);
            closestToTarget = isoPlayer.getClosestTo((IsoGameCharacter)closestHitTarget);
            if (!isoPlayer.getAttackVars().aimAtFloor && closestDist > 1.25f && weaponType == WeaponType.SPEAR && (closestToTarget == null || IsoUtils.DistanceTo(closestHitTarget.getX(), closestHitTarget.getY(), closestToTarget.getX(), closestToTarget.getY()) > 1.7f)) {
                if (!GameClient.client || isoPlayer.isLocalPlayer()) {
                    isoPlayer.setAttackType(AttackType.OVERHEAD);
                }
                if (isoPlayer.getPrimaryHandItem() == null || isoPlayer.getPrimaryHandItem().hasTag(ItemTag.FAKE_SPEAR)) {
                    criticalHitChance += 30.0f;
                }
            }
            if (isoPlayer.isLocalPlayer() && !closestHitTarget.isOnFloor()) {
                closestHitTarget.setHitFromBehind(isoPlayer.isAttackFromBehind());
            }
            if (!isoPlayer.isAttackFromBehind()) break block39;
            if (!(closestHitTarget instanceof IsoZombie)) ** GOTO lbl-1000
            isoZombie = (IsoZombie)closestHitTarget;
            if (isoZombie.target == null && (isoPlayer.getPrimaryHandItem() == null || isoPlayer.getPrimaryHandItem().hasTag(ItemTag.FAKE_SPEAR))) {
                criticalHitChance += this.combatConfig.get(CombatConfigKey.ADDITIONAL_CRITICAL_HIT_CHANCE_FROM_BEHIND);
            } else lbl-1000:
            // 2 sources

            {
                criticalHitChance += this.combatConfig.get(CombatConfigKey.ADDITIONAL_CRITICAL_HIT_CHANCE_DEFAULT);
            }
        }
        if (closestHitTarget instanceof IsoPlayer && weaponType.isRanged() && !isoPlayer.isDoHandToHandAttack()) {
            criticalHitChance = isoPlayer.getAttackVars().getWeapon(isoPlayer).getStopPower() * (1.0f + (float)isoPlayer.getPerkLevel(PerkFactory.Perks.Aiming) / 15.0f);
        }
        if (isoPlayer.getPrimaryHandItem() != null && isoPlayer.getPrimaryHandItem().hasTag(ItemTag.NO_CRITICALS)) {
            criticalHitChance = 0.0f;
        }
        if (!GameClient.client || isoPlayer.isLocalPlayer()) {
            isoPlayer.setCriticalHit((float)Rand.Next(100) < criticalHitChance);
            if (isoPlayer.isAttackFromBehind() && isoPlayer.getAttackVars().closeKill && closestHitTarget instanceof IsoZombie) {
                isoZombie = (IsoZombie)closestHitTarget;
                if (isoZombie.target == null && isoPlayer.getPrimaryHandItem() != null && !isoPlayer.getPrimaryHandItem().hasTag(ItemTag.FAKE_SPEAR)) {
                    isoPlayer.setCriticalHit(true);
                }
            }
            if (isoPlayer.isCriticalHit() && !isoPlayer.getAttackVars().closeKill && !isoPlayer.isDoShove() && weaponType == WeaponType.KNIFE) {
                isoPlayer.setCriticalHit(false);
            }
            if (isoPlayer.getStats().numChasingZombies > 1 && isoPlayer.getAttackVars().closeKill && !isoPlayer.isDoShove() && weaponType == WeaponType.KNIFE) {
                isoPlayer.setCriticalHit(false);
            }
        }
        if (isoPlayer.getPrimaryHandItem() != null && isoPlayer.getPrimaryHandItem().hasTag(ItemTag.NO_CRITICALS)) {
            isoPlayer.setCriticalHit(false);
        }
        if (isoPlayer.isCriticalHit()) {
            isoPlayer.setCombatSpeed(isoPlayer.getCombatSpeed() * 1.1f);
        }
        if (Core.debug) {
            DebugType.Combat.debugln("Attacked zombie: dist: " + closestDist + ", chance: (" + isoPlayer.getHitInfoList().get((int)0).chance + "), crit: " + isoPlayer.isCriticalHit() + " (" + criticalHitChance + "%) from behind: " + isoPlayer.isAttackFromBehind());
        }
        isoPlayer.setLastAttackWasHandToHand(isoPlayer.isDoHandToHandAttack());
    }

    private void processTargetedHit(HandWeapon handWeapon, IsoGameCharacter wielder, IsoGameCharacter target, RagdollBodyPart targetedBodyPart, ShotDirection shotDirection) {
        IsoZombie isoZombie;
        HitReaction hitReaction;
        int randomHitReaction = Rand.Next(2);
        switch (targetedBodyPart) {
            case BODYPART_PELVIS: {
                hitReaction = HitReaction.SHOT_BELLY_STEP;
                if (shotDirection != ShotDirection.NORTH) break;
                if (target.isHitFromBehind()) {
                    hitReaction = HitReaction.SHOT_BELLY_STEP_BEHIND;
                    break;
                }
                if (randomHitReaction != 0) break;
                hitReaction = HitReaction.SHOT_BELLY;
                break;
            }
            case BODYPART_SPINE: {
                hitReaction = HitReaction.SHOT_CHEST;
                if (shotDirection == ShotDirection.LEFT) {
                    hitReaction = randomHitReaction == 0 ? HitReaction.SHOT_CHEST_L : HitReaction.SHOT_CHEST_STEP_L;
                    break;
                }
                if (shotDirection != ShotDirection.RIGHT) break;
                hitReaction = randomHitReaction == 0 ? HitReaction.SHOT_CHEST_R : HitReaction.SHOT_CHEST_STEP_R;
                break;
            }
            case BODYPART_HEAD: {
                HitReaction hitReaction2 = hitReaction = randomHitReaction == 0 ? HitReaction.SHOT_HEAD_FWD : HitReaction.SHOT_HEAD_FWD02;
                if (shotDirection != ShotDirection.LEFT && shotDirection != ShotDirection.RIGHT || Rand.Next(4) != 0) break;
                hitReaction = HitReaction.SHOT_HEAD_BWD;
                break;
            }
            case BODYPART_LEFT_UPPER_LEG: 
            case BODYPART_LEFT_LOWER_LEG: {
                hitReaction = HitReaction.SHOT_LEG_L;
                break;
            }
            case BODYPART_RIGHT_UPPER_LEG: 
            case BODYPART_RIGHT_LOWER_LEG: {
                hitReaction = HitReaction.SHOT_LEG_R;
                break;
            }
            case BODYPART_LEFT_UPPER_ARM: 
            case BODYPART_LEFT_LOWER_ARM: {
                hitReaction = target.isHitFromBehind() ? HitReaction.SHOT_SHOULDER_STEP_L : HitReaction.SHOT_SHOULDER_L;
                break;
            }
            case BODYPART_RIGHT_UPPER_ARM: 
            case BODYPART_RIGHT_LOWER_ARM: {
                hitReaction = target.isHitFromBehind() ? HitReaction.SHOT_SHOULDER_STEP_R : HitReaction.SHOT_SHOULDER_R;
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + String.valueOf((Object)targetedBodyPart));
            }
        }
        this.applyBlood(handWeapon, target, hitReaction, shotDirection);
        if (target instanceof IsoZombie && (isoZombie = (IsoZombie)target).getEatBodyTarget() != null) {
            hitReaction = target.getVariableBoolean("onknees") ? HitReaction.ON_KNEES : HitReaction.EATING;
        }
        target.setHitReaction(hitReaction.getValue());
        this.createCombatData(handWeapon, wielder, target, targetedBodyPart);
        DebugType.Combat.debugln("hitReaction = %s", new Object[]{hitReaction});
    }

    private void createCombatData(HandWeapon weapon, IsoGameCharacter attacker, IsoGameCharacter target, RagdollBodyPart targetedBodyPart) {
        if (!target.usePhysicHitReaction() || !target.canRagdoll()) {
            return;
        }
        BallisticsTarget ballisticsTarget = target.getBallisticsTarget();
        if (ballisticsTarget != null) {
            BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
            combatDamageData.event = target.getHitReaction();
            combatDamageData.target = target;
            combatDamageData.attacker = attacker;
            combatDamageData.handWeapon = weapon;
            combatDamageData.isoTrap = null;
            combatDamageData.bodyPart = targetedBodyPart;
            ballisticsTarget.setCombatDamageDataProcessed(false);
        }
    }

    private void createCombatData(IsoTrap isoTrap, IsoGameCharacter target, RagdollBodyPart targetedBodyPart) {
        if (!target.usePhysicHitReaction() || !target.canRagdoll()) {
            return;
        }
        BallisticsTarget ballisticsTarget = target.getBallisticsTarget();
        if (ballisticsTarget != null) {
            BallisticsTarget.CombatDamageData combatDamageData = ballisticsTarget.getCombatDamageData();
            combatDamageData.event = target.getHitReaction();
            combatDamageData.target = target;
            combatDamageData.attacker = isoTrap.getAttacker();
            combatDamageData.handWeapon = isoTrap.getHandWeapon();
            combatDamageData.isoTrap = isoTrap;
            combatDamageData.bodyPart = targetedBodyPart;
            ballisticsTarget.setCombatDamageDataProcessed(false);
        }
    }

    public void update(boolean doUpdate) {
        if (IsoPlayer.players[0] == null) {
            return;
        }
        if (!IsoPlayer.players[0].isWeaponReady()) {
            return;
        }
        this.updateReticle(IsoPlayer.players[0]);
        this.highlightTargets(IsoPlayer.players[0]);
        this.debugUpdate();
    }

    private void debugUpdate() {
        if (DebugOptions.instance.thumpableResetCurrentCellWindows.getValue()) {
            IsoWindow.resetCurrentCellWindows();
            DebugOptions.instance.thumpableResetCurrentCellWindows.setValue(false);
        } else if (DebugOptions.instance.thumpableBarricadeCurrentCellWindowsFullPlanks.getValue()) {
            IsoBarricade.barricadeCurrentCellWithPlanks(4);
            DebugOptions.instance.thumpableBarricadeCurrentCellWindowsFullPlanks.setValue(false);
        } else if (DebugOptions.instance.thumpableBarricadeCurrentCellWindowsHalfPlanks.getValue()) {
            IsoBarricade.barricadeCurrentCellWithPlanks(2);
            DebugOptions.instance.thumpableBarricadeCurrentCellWindowsHalfPlanks.setValue(false);
        } else if (DebugOptions.instance.thumpableRemoveBarricadeCurrentCellWindows.getValue()) {
            IsoBarricade.barricadeCurrentCellWithPlanks(0);
            DebugOptions.instance.thumpableRemoveBarricadeCurrentCellWindows.setValue(false);
        } else if (DebugOptions.instance.thumpableBarricadeCurrentCellWindowsFullMetalBars.getValue()) {
            IsoBarricade.barricadeCurrentCellWithMetalBars();
            DebugOptions.instance.thumpableBarricadeCurrentCellWindowsFullMetalBars.setValue(false);
        } else if (DebugOptions.instance.thumpableBarricadeCurrentCellWindowsMetalPlate.getValue()) {
            IsoBarricade.barricadeCurrentCellWithMetalPlate();
            DebugOptions.instance.thumpableBarricadeCurrentCellWindowsMetalPlate.setValue(false);
        }
    }

    public void postUpdate(boolean doUpdate) {
    }

    public void updateReticle(IsoPlayer isoPlayer) {
        if (targetReticleMode == 0) {
            return;
        }
        if (!isoPlayer.isLocalPlayer() || isoPlayer.isNPC()) {
            return;
        }
        if (!isoPlayer.isAiming()) {
            return;
        }
        HandWeapon weapon = Type.tryCastTo(isoPlayer.getPrimaryHandItem(), HandWeapon.class);
        if (weapon == null || weapon.getSwingAnim() == null || weapon.getCondition() <= 0) {
            weapon = isoPlayer.bareHands;
        }
        if (!weapon.isRanged()) {
            return;
        }
        boolean bDoShove1 = isoPlayer.isDoShove();
        boolean bDoGrapple1 = isoPlayer.isDoGrapple();
        HandWeapon weapon1 = isoPlayer.getUseHandWeapon();
        isoPlayer.setDoShove(false);
        isoPlayer.setDoGrapple(false);
        isoPlayer.setUseHandWeapon(weapon);
        this.calculateAttackVars(isoPlayer);
        this.calculateHitInfoList(isoPlayer);
        if (Core.debug) {
            this.updateTargetHitInfoPanel(isoPlayer);
        }
        ColorInfo tempColorInfo = IsoGameCharacter.getInf();
        HitChanceData hitChanceData = this.calculateHitChanceData(isoPlayer, weapon, null);
        IsoReticle.getInstance().setChance((int)hitChanceData.hitChance);
        IsoReticle.getInstance().setAimPenalty((int)hitChanceData.aimPenalty);
        IsoReticle.getInstance().hasTarget(isoPlayer.getAimingMode() == AimingMode.TARGET_FOUND);
        IsoReticle.getInstance().setReticleColor(Core.getInstance().getNoTargetColor());
        float closest = Float.MAX_VALUE;
        for (int i = 0; i < isoPlayer.getHitInfoList().size(); ++i) {
            HitInfo hitInfo = isoPlayer.getHitInfoList().get(i);
            IsoMovingObject object = hitInfo.getObject();
            if (!(hitInfo.distSq < closest)) continue;
            if (object instanceof IsoZombie || object instanceof IsoPlayer) {
                float delta = (float)hitInfo.chance < 70.0f ? (float)hitInfo.chance / 140.0f : ((float)hitInfo.chance - 70.0f) / 30.0f * 0.5f + 0.5f;
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), delta, tempColorInfo);
                closest = hitInfo.distSq;
                IsoReticle.getInstance().setChance(hitInfo.chance);
                IsoReticle.getInstance().setReticleColor(Core.getInstance().getTargetColor());
            }
            if (hitInfo.window.getObject() == null) continue;
            hitInfo.window.getObject().setHighlightColor(0.8f, 0.1f, 0.1f, 0.5f);
            hitInfo.window.getObject().setHighlighted(true);
        }
        IsoReticle.getInstance().setAimColor(tempColorInfo);
        isoPlayer.setDoShove(bDoShove1);
        isoPlayer.setDoGrapple(bDoGrapple1);
        isoPlayer.setUseHandWeapon(weapon1);
    }

    private void updateTargetHitInfoPanel(IsoGameCharacter isoGameCharacter) {
        for (BaseDebugWindow baseDebugWindow : DebugContext.instance.getWindows()) {
            if (!(baseDebugWindow instanceof TargetHitInfoPanel)) continue;
            TargetHitInfoPanel targetHitInfoPanel = (TargetHitInfoPanel)baseDebugWindow;
            targetHitInfoPanel.setIsoGameCharacter(isoGameCharacter);
            targetHitInfoPanel.hitInfoList.clear();
            PZArrayUtil.addAll(isoGameCharacter.getHitInfoList(), targetHitInfoPanel.hitInfoList);
            break;
        }
    }

    private void fireWeapon(HandWeapon handWeapon, IsoGameCharacter isoGameCharacter) {
        if (handWeapon == null) {
            return;
        }
        float range = handWeapon.getMaxRange(isoGameCharacter);
        BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
        ballisticsController.calculateMuzzlePosition(ballisticsStartPosition, ballisticsDirectionVector);
        ballisticsDirectionVector.normalize();
        float firingVectorX = CombatManager.ballisticsStartPosition.x + CombatManager.ballisticsDirectionVector.x * range;
        float firingVectorY = CombatManager.ballisticsStartPosition.y + CombatManager.ballisticsDirectionVector.y * range;
        float firingVectorZ = CombatManager.ballisticsStartPosition.z + CombatManager.ballisticsDirectionVector.z * range;
        boolean checkHitVehicle = this.isVehicleHit(isoGameCharacter.getHitInfoList());
        if (handWeapon.isRangeFalloff()) {
            if (!ballisticsController.hasSpreadData()) {
                ballisticsController.setRange(range);
                ballisticsController.update();
                int projectileCount = handWeapon.getProjectileCount();
                float projectileSpread = handWeapon.getProjectileSpread();
                float projectileWeightCenter = handWeapon.getProjectileWeightCenter();
                ballisticsController.getSpreadData(range, projectileSpread, projectileWeightCenter, projectileCount);
            }
            int numberOfSpreadData = ballisticsController.getNumberOfSpreadData();
            float[] ballisticsSpreadData = ballisticsController.getBallisticsSpreadData();
            int arrayIndex = 0;
            Vector3f start = BaseVehicle.allocVector3f();
            Vector3f end = BaseVehicle.allocVector3f();
            Vector3f intersect = BaseVehicle.allocVector3f();
            for (int i = 0; i < numberOfSpreadData; ++i) {
                float id = ballisticsSpreadData[arrayIndex++];
                float x = ballisticsSpreadData[arrayIndex++];
                float z = ballisticsSpreadData[arrayIndex++] / 2.44949f;
                float y = ballisticsSpreadData[arrayIndex++];
                start.set(CombatManager.ballisticsStartPosition.x, CombatManager.ballisticsStartPosition.y, CombatManager.ballisticsStartPosition.z);
                end.set(x, y, z);
                if (id == 0.0f) {
                    Vector3 closest;
                    Vector3f intersectPoint;
                    if (checkHitVehicle && (intersectPoint = this.checkHitVehicle(isoGameCharacter.getHitInfoList(), start, end, intersect)) != null) {
                        closest = PZMath.closestVector3(start.x(), start.y(), start.z(), end.x(), end.y(), end.z(), intersectPoint.x(), intersectPoint.y(), end.z());
                        IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, closest.x, closest.y, closest.z);
                        continue;
                    }
                    IsoGridSquareCollisionData isoGridSquareCollisionData = LosUtil.getFirstBlockingIsoGridSquare(isoGameCharacter.getCell(), PZMath.fastfloor(CombatManager.ballisticsStartPosition.x), PZMath.fastfloor(CombatManager.ballisticsStartPosition.y), PZMath.fastfloor(CombatManager.ballisticsStartPosition.z), PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z), false);
                    if (isoGridSquareCollisionData.testResults == LosUtil.TestResults.Clear || isoGridSquareCollisionData.testResults == LosUtil.TestResults.ClearThroughClosedDoor) {
                        IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, x, y, z);
                        continue;
                    }
                    closest = PZMath.closestVector3(CombatManager.ballisticsStartPosition.x, CombatManager.ballisticsStartPosition.y, CombatManager.ballisticsStartPosition.z, x, y, z, isoGridSquareCollisionData.hitPosition.x, isoGridSquareCollisionData.hitPosition.y, z);
                    IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, closest.x, closest.y, closest.z, isoGridSquareCollisionData.isoGridSquare);
                    continue;
                }
                IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, x, y, z);
            }
            BaseVehicle.releaseVector3f(start);
            BaseVehicle.releaseVector3f(end);
            BaseVehicle.releaseVector3f(intersect);
        } else {
            PZArrayList<HitInfo> hitInfoList = isoGameCharacter.getHitInfoList();
            if (hitInfoList.isEmpty()) {
                this.missedShot(isoGameCharacter, range, firingVectorX, firingVectorY, firingVectorZ);
            } else {
                assert (hitInfoList.size() == 1);
                HitInfo hitInfo = hitInfoList.get(0);
                if (checkHitVehicle) {
                    Vector3f start = BaseVehicle.allocVector3f();
                    Vector3f end = BaseVehicle.allocVector3f();
                    start.set(CombatManager.ballisticsStartPosition.x, CombatManager.ballisticsStartPosition.y, CombatManager.ballisticsStartPosition.z);
                    end.set(hitInfo.x, hitInfo.y, hitInfo.z);
                    Vector3f intersect = BaseVehicle.allocVector3f();
                    boolean bIntersect = this.checkHitVehicle(isoGameCharacter.getHitInfoList(), start, end, intersect) != null;
                    BaseVehicle.releaseVector3f(start);
                    BaseVehicle.releaseVector3f(end);
                    BaseVehicle.releaseVector3f(intersect);
                    if (!bIntersect) {
                        this.missedShot(isoGameCharacter, range, firingVectorX, firingVectorY, firingVectorZ);
                        return;
                    }
                }
                IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, hitInfo.x, hitInfo.y, hitInfo.z);
            }
        }
    }

    private void missedShot(IsoGameCharacter isoGameCharacter, float range, float firingVectorX, float firingVectorY, float firingVectorZ) {
        IsoGridSquareCollisionData isoGridSquareCollisionData = LosUtil.getFirstBlockingIsoGridSquare(isoGameCharacter.getCell(), PZMath.fastfloor(CombatManager.ballisticsStartPosition.x), PZMath.fastfloor(CombatManager.ballisticsStartPosition.y), PZMath.fastfloor(CombatManager.ballisticsStartPosition.z), PZMath.fastfloor(firingVectorX), PZMath.fastfloor(firingVectorY), PZMath.fastfloor(firingVectorZ), false);
        if (isoGridSquareCollisionData.testResults == LosUtil.TestResults.Clear || isoGridSquareCollisionData.testResults == LosUtil.TestResults.ClearThroughClosedDoor) {
            IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range);
        } else {
            Vector3 closest = PZMath.closestVector3(CombatManager.ballisticsStartPosition.x, CombatManager.ballisticsStartPosition.y, CombatManager.ballisticsStartPosition.z, firingVectorX, firingVectorY, firingVectorZ, isoGridSquareCollisionData.hitPosition.x, isoGridSquareCollisionData.hitPosition.y, firingVectorZ);
            IsoBulletTracerEffects.getInstance().addEffect(isoGameCharacter, range, closest.x, closest.y, closest.z, isoGridSquareCollisionData.isoGridSquare);
        }
    }

    private boolean isVehicleHit(PZArrayList<HitInfo> hitInfoArrayList) {
        for (int i = 0; i < hitInfoArrayList.size(); ++i) {
            IsoMovingObject hitObject = hitInfoArrayList.get(i).getObject();
            if (hitObject == null || !(hitObject instanceof BaseVehicle)) continue;
            BaseVehicle hitVehicle = (BaseVehicle)hitObject;
            return true;
        }
        return false;
    }

    private boolean checkHitVehicle(IsoGameCharacter isoGameCharacter, BaseVehicle baseVehicle) {
        short id = baseVehicle.getId();
        BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
        return ballisticsController.isTarget(id) || ballisticsController.isSpreadTarget(id);
    }

    private Vector3f checkHitVehicle(PZArrayList<HitInfo> hitInfoArrayList, Vector3f start, Vector3f end, Vector3f result) {
        Vector3f intersectPoint = null;
        for (int i = 0; i < hitInfoArrayList.size(); ++i) {
            BaseVehicle hitVehicle;
            IsoMovingObject hitObject = hitInfoArrayList.get(i).getObject();
            if (!(hitObject instanceof BaseVehicle) || (intersectPoint = (hitVehicle = (BaseVehicle)hitObject).getIntersectPoint(start, end, result)) == null) continue;
            return intersectPoint;
        }
        return intersectPoint;
    }

    public boolean hitIsoGridSquare(IsoGridSquare isoGridSquare, Vector3f hitLocation) {
        return this.shootPlacedItems(isoGridSquare, new Vector3(hitLocation.x(), hitLocation.y(), hitLocation.z()));
    }

    private boolean shootPlacedItems(IsoGridSquare isoGridSquare, Vector3 hitPosition) {
        float nearestDistance = Float.MAX_VALUE;
        IsoWorldInventoryObject nearestIsoWorldInventoryObject = null;
        IsoObject[] objects = isoGridSquare.getObjects().getElements();
        int nObjects = isoGridSquare.getObjects().size();
        for (int i = 0; i < nObjects; ++i) {
            Vector3 objectPosition;
            float distance;
            Durability durability;
            IsoObject obj = objects[i];
            IsoWorldInventoryObject worldObj = Type.tryCastTo(obj, IsoWorldInventoryObject.class);
            IsoLightSwitch isoLightSwitch = Type.tryCastTo(obj, IsoLightSwitch.class);
            if (worldObj == null && isoLightSwitch == null) continue;
            if (isoLightSwitch != null && !isoLightSwitch.lightRoom && isoLightSwitch.hasLightBulb()) {
                StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Lights Shot", 1.0f);
                SoundManager.instance.playImpactSound(isoGridSquare, null, MaterialType.Glass_Solid);
                SoundManager.instance.PlayWorldSound(BreakLightBulbSound, isoGridSquare, 0.2f, 20.0f, 1.0f, true);
                isoLightSwitch.setBulbItemRaw(null);
            }
            if (worldObj == null || worldObj.item == null || (durability = worldObj.item.getDurabilityComponent()) == null || !((distance = hitPosition.distanceTo(objectPosition = new Vector3(worldObj.getWorldPosX(), worldObj.getWorldPosY(), worldObj.getWorldPosZ()))) < nearestDistance)) continue;
            nearestDistance = distance;
            nearestIsoWorldInventoryObject = worldObj;
        }
        if (nearestIsoWorldInventoryObject != null) {
            Durability durability = nearestIsoWorldInventoryObject.item.getDurabilityComponent();
            SoundManager.instance.playImpactSound(isoGridSquare, null, durability.getMaterial());
            isoGridSquare.transmitRemoveItemFromSquare(nearestIsoWorldInventoryObject);
            return true;
        }
        return false;
    }

    private void setUsePhysicHitReaction(HandWeapon handWeapon, IsoGameCharacter isoGameCharacter) {
        if (!isoGameCharacter.canRagdoll()) {
            isoGameCharacter.setUsePhysicHitReaction(false);
            return;
        }
        RagdollSettingsManager ragdollSettingsManager = RagdollSettingsManager.getInstance();
        isoGameCharacter.setUsePhysicHitReaction(ragdollSettingsManager.usePhysicHitReaction(isoGameCharacter) && handWeapon.isRanged());
    }

    public void processInstantExplosion(IsoGameCharacter target, IsoTrap isoTrap) {
        target.setUsePhysicHitReaction(!target.canRagdoll());
        RagdollSettingsManager ragdollSettingsManager = RagdollSettingsManager.getInstance();
        target.setUsePhysicHitReaction(ragdollSettingsManager.usePhysicHitReaction(target));
        target.setHitReaction(HitReaction.SHOT_CHEST.getValue());
        BallisticsTarget ballisticsTarget = target.ensureExistsBallisticsTarget(target);
        ballisticsTarget.add();
        this.createCombatData(isoTrap, target, RagdollBodyPart.getRandomPart());
        if (DebugOptions.instance.character.debug.render.explosionHitDirection.getValue()) {
            HandWeapon handWeapon = isoTrap.getHandWeapon();
            IsoGridSquare isoGridSquare = handWeapon.getAttackTargetSquare(null);
            LineDrawer.addAlphaDecayingLine(isoGridSquare.getX(), isoGridSquare.getY(), isoGridSquare.getZ(), target.getX(), target.getY(), target.getZ(), 0.0f, 1.0f, 0.5f, 1.0f);
            LineDrawer.addAlphaDecayingIsoCircle(isoGridSquare.getX(), isoGridSquare.getY(), isoGridSquare.getZ(), 1.0f, 16, 0.0f, 1.0f, 0.5f, 1.0f);
        }
    }

    public float applyGlobalDamageReductionMultipliers(HandWeapon handWeapon, float damage) {
        if (handWeapon.isMelee()) {
            damage *= this.combatConfig.get(CombatConfigKey.GLOBAL_MELEE_DAMAGE_REDUCTION_MULTIPLIER);
        }
        return damage;
    }

    public float applyWeaponLevelDamageModifier(IsoGameCharacter isoGameCharacter, float damage) {
        int weaponLevel = Math.max(isoGameCharacter.getWeaponLevel(), 0);
        weaponLevel = Math.min(weaponLevel, 10);
        return damage *= this.combatConfig.get(CombatConfigKey.BASE_WEAPON_DAMAGE_MULTIPLIER) + (float)weaponLevel * this.combatConfig.get(CombatConfigKey.WEAPON_LEVEL_DAMAGE_MULTIPLIER_INCREMENT);
    }

    public float applyPlayerReceivedDamageModifier(IsoGameCharacter isoGameCharacter, float damage) {
        return damage * (isoGameCharacter instanceof IsoPlayer ? this.combatConfig.get(CombatConfigKey.PLAYER_RECEIVED_DAMAGE_MULTIPLIER) : this.combatConfig.get(CombatConfigKey.NON_PLAYER_RECEIVED_DAMAGE_MULTIPLIER));
    }

    public float applyOneHandedDamagePenalty(IsoGameCharacter isoGameCharacter, HandWeapon weapon, float damage) {
        boolean usingTwoHandedWeaponIncorrectly = weapon.isTwoHandWeapon() && !isoGameCharacter.isItemInBothHands(weapon);
        return damage * (usingTwoHandedWeaponIncorrectly ? this.combatConfig.get(CombatConfigKey.DAMAGE_PENALTY_ONE_HANDED_TWO_HANDED_WEAPON_MULTIPLIER) : 1.0f);
    }

    public void applyMeleeEnduranceLoss(IsoGameCharacter attacker, IsoGameCharacter target, HandWeapon handWeapon, float damage) {
        float dmgEnduranceModifier;
        if (!handWeapon.isMelee()) {
            return;
        }
        if (!handWeapon.isUseEndurance()) {
            return;
        }
        boolean usingTwoHandedIncorrectly = handWeapon.isTwoHandWeapon() && (attacker.getPrimaryHandItem() != handWeapon || attacker.getSecondaryHandItem() != handWeapon);
        float twoHandedPenalty = usingTwoHandedIncorrectly ? handWeapon.getWeight() / this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_TWO_HANDED_PENALTY_DIVISOR) / this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_TWO_HANDED_PENALTY_SCALE) : 0.0f;
        float enduranceLoss = (handWeapon.getWeight() * this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_BASE_SCALE) * handWeapon.getFatigueMod(attacker) * attacker.getFatigueMod() * handWeapon.getEnduranceMod() * this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_WEIGHT_MODIFIER) + twoHandedPenalty) * this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_FINAL_MULTIPLIER);
        if (attacker instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)attacker;
            if (attacker.isAimAtFloor() && isoPlayer.isDoShove()) {
                enduranceLoss *= this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_FLOOR_SHOVE_MULTIPLIER);
            }
        }
        if (damage <= 0.0f) {
            dmgEnduranceModifier = 1.0f;
        } else if (target.isCloseKilled()) {
            dmgEnduranceModifier = this.combatConfig.get(CombatConfigKey.ENDURANCE_LOSS_CLOSE_KILL_MODIFIER);
        } else {
            float realDmgLeft = Math.min(damage, target.getHealth());
            dmgEnduranceModifier = Math.min(realDmgLeft / handWeapon.getMaxDamage(), 1.0f);
        }
        attacker.getStats().remove(CharacterStat.ENDURANCE, enduranceLoss * dmgEnduranceModifier);
    }

    private float calculateTotalDefense(int partHit, IsoGameCharacter isoGameCharacter, HandWeapon weapon) {
        float totalDefense = isoGameCharacter.getBodyPartClothingDefense(partHit, false, weapon.isRanged()) * 0.5f;
        totalDefense += isoGameCharacter.getBodyPartClothingDefense(partHit, true, weapon.isRanged());
        totalDefense *= (float)SandboxOptions.instance.lore.zombiesArmorFactor.getValue();
        int maxDefense = SandboxOptions.instance.lore.zombiesMaxDefense.getValue();
        if (maxDefense > 100) {
            maxDefense = 100;
        }
        if (totalDefense > (float)maxDefense) {
            totalDefense = maxDefense;
        }
        return totalDefense;
    }

    private float applyMeleeHitLocationDamage(IsoGameCharacter isoGameCharacter, HandWeapon weapon, int partHit, int hitHead, boolean hitLegs, float damageSplit) {
        if (hitHead > 0) {
            isoGameCharacter.addBlood(BloodBodyPartType.Head, true, true, true);
            isoGameCharacter.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
            isoGameCharacter.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
            isoGameCharacter.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
            damageSplit *= this.combatConfig.get(CombatConfigKey.HEAD_HIT_DAMAGE_SPLIT_MODIFIER);
        }
        if (hitLegs) {
            damageSplit *= this.combatConfig.get(CombatConfigKey.LEG_HIT_DAMAGE_SPLIT_MODIFIER);
        }
        float calculatedDamage = this.calculateHitLocationDamage(isoGameCharacter, weapon, partHit, damageSplit);
        StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Melee Damage", calculatedDamage);
        return calculatedDamage;
    }

    private float applyRangeHitLocationDamage(IsoGameCharacter isoGameCharacter, HandWeapon weapon, int bodyPart, float damageSplit) {
        if (RagdollBodyPart.isHead(bodyPart)) {
            isoGameCharacter.addBlood(BloodBodyPartType.Head, true, true, true);
            isoGameCharacter.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
            isoGameCharacter.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
            isoGameCharacter.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
            damageSplit *= this.combatConfig.get(CombatConfigKey.HEAD_HIT_DAMAGE_SPLIT_MODIFIER);
        }
        if (RagdollBodyPart.isLeg(bodyPart) || RagdollBodyPart.isArm(bodyPart)) {
            damageSplit *= this.combatConfig.get(CombatConfigKey.LEG_HIT_DAMAGE_SPLIT_MODIFIER);
        }
        int partHit = RagdollBodyPart.getBodyPartType(bodyPart);
        float calculatedDamage = this.calculateHitLocationDamage(isoGameCharacter, weapon, partHit, damageSplit);
        StatisticsManager.getInstance().incrementStatistic(StatisticType.Player, StatisticCategory.Combat, "Bullet Damage", calculatedDamage);
        return calculatedDamage;
    }

    private float applyTotalDefense(float damageSplit, float totalDefense) {
        return damageSplit * Math.abs(1.0f - totalDefense / 100.0f);
    }

    private float calculateHitLocationDamage(IsoGameCharacter isoGameCharacter, HandWeapon weapon, int partHit, float damageSplit) {
        float totalDefense = this.calculateTotalDefense(partHit, isoGameCharacter, weapon);
        float calculatedDamage = this.applyTotalDefense(damageSplit, totalDefense);
        if (Core.debug) {
            BodyPartType partType = BodyPartType.FromIndex(partHit);
            DebugType.Combat.debugln("Zombie got hit in " + BodyPartType.getDisplayName(partType) + " with a " + weapon.getFullType() + " for " + calculatedDamage + " out of " + damageSplit + " after totalDef of " + totalDefense + "% was applied");
        }
        return calculatedDamage;
    }

    private void resolveSpikedArmorDamage(IsoGameCharacter owner, HandWeapon weapon, IsoGameCharacter hitZombie, int partHit) {
        boolean shove = ((IsoLivingCharacter)owner).isDoShove();
        if (shove || WeaponType.getWeaponType(weapon) == WeaponType.KNIFE) {
            boolean spikedSecondary;
            boolean behind = !owner.isAimAtFloor() ? owner.isBehind(hitZombie) : hitZombie.isFallOnFront();
            boolean spikedPart = behind ? hitZombie.bodyPartIsSpikedBehind(partHit) : hitZombie.bodyPartIsSpiked(partHit);
            boolean spikedFoot = spikedPart && owner.isAimAtFloor() && shove;
            boolean spikedPrimary = spikedPart && !spikedFoot && (owner.getPrimaryHandItem() == null || owner.getPrimaryHandItem() instanceof HandWeapon);
            boolean bl = spikedSecondary = spikedPart && !spikedFoot && (owner.getSecondaryHandItem() == null || owner.getSecondaryHandItem() instanceof HandWeapon) && shove;
            if (spikedFoot) {
                hitZombie.addBlood(BloodBodyPartType.FromIndex(partHit), true, false, false);
                owner.spikePart(BodyPartType.Foot_R);
            }
            if (spikedPrimary) {
                hitZombie.addBlood(BloodBodyPartType.FromIndex(partHit), true, false, false);
                owner.spikePart(BodyPartType.Hand_R);
            }
            if (spikedSecondary) {
                hitZombie.addBlood(BloodBodyPartType.FromIndex(partHit), true, false, false);
                owner.spikePart(BodyPartType.Hand_L);
            }
        }
    }

    private boolean applyKnifeDeathEffect(IsoGameCharacter owner, IsoZombie hitZombie) {
        boolean removeKnife = false;
        if ("KnifeDeath".equals(owner.getVariableString("ZombieHitReaction")) && !"Tutorial".equals(Core.gameMode)) {
            int knifeLvl;
            int rand = 8;
            if (hitZombie.isCurrentState(AttackState.instance())) {
                rand = 3;
            }
            if (Rand.NextBool(rand + (knifeLvl = owner.getPerkLevel(PerkFactory.Perks.SmallBlade) + 1) * 2)) {
                InventoryItem item = owner.getPrimaryHandItem();
                owner.getInventory().Remove(item);
                owner.removeFromHands(item);
                hitZombie.setAttachedItem("JawStab", item);
                hitZombie.setJawStabAttach(true);
                removeKnife = true;
            }
            hitZombie.setKnifeDeath(true);
        }
        return removeKnife;
    }

    public void setAimingDelay(IsoPlayer isoPlayer, HandWeapon handWeapon) {
        float aimingDelay = isoPlayer.getAimingDelay() + ((float)handWeapon.getRecoilDelay(isoPlayer) * this.combatConfig.get(CombatConfigKey.POST_SHOT_AIMING_DELAY_RECOIL_MODIFIER) + (float)handWeapon.getAimingTime() * this.combatConfig.get(CombatConfigKey.POST_SHOT_AIMING_DELAY_AIMING_MODIFIER));
        float maximumAimingDelay = isoPlayer.getPrimaryHandItem() instanceof HandWeapon ? (float)((HandWeapon)isoPlayer.getPrimaryHandItem()).getAimingTime() : 0.0f;
        isoPlayer.setAimingDelay(PZMath.clamp(aimingDelay, 0.0f, maximumAimingDelay));
    }

    private static class Holder {
        private static final CombatManager instance = new CombatManager();

        private Holder() {
        }
    }

    private static final class WindowVisitor
    implements LOSVisitor {
        private LosUtil.TestResults test;
        private IsoWindow window;

        private WindowVisitor() {
        }

        private void init() {
            this.test = LosUtil.TestResults.Clear;
            this.window = null;
        }

        @Override
        public boolean visit(IsoGridSquare a, IsoGridSquare b) {
            IsoWindow window;
            if (a == null || b == null) {
                return false;
            }
            boolean bSpecialDiag = true;
            boolean bIgnoreDoors = false;
            LosUtil.TestResults newTest = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, false);
            if (newTest == LosUtil.TestResults.ClearThroughWindow && this.isHittable(window = a.getWindowTo(b)) && window.TestVision(a, b) == IsoObject.VisionResult.Unblocked) {
                this.window = window;
                return true;
            }
            if (newTest == LosUtil.TestResults.Blocked || this.test == LosUtil.TestResults.Clear || newTest == LosUtil.TestResults.ClearThroughWindow && this.test == LosUtil.TestResults.ClearThroughOpenDoor) {
                this.test = newTest;
            } else if (newTest == LosUtil.TestResults.ClearThroughClosedDoor && this.test == LosUtil.TestResults.ClearThroughOpenDoor) {
                this.test = newTest;
            }
            return this.test == LosUtil.TestResults.Blocked;
        }

        @Override
        public LosUtil.TestResults getResult() {
            return this.test;
        }

        boolean isHittable(IsoWindow window) {
            if (window == null) {
                return false;
            }
            if (window.isBarricaded()) {
                return true;
            }
            return !window.isDestroyed() && !window.IsOpen();
        }
    }

    private static final class CalcHitListGrappleReusables {
        static final ArrayList<IsoMovingObject> foundObjects = new ArrayList();
        static final Vector4f posAndDot = new Vector4f();

        private CalcHitListGrappleReusables() {
        }
    }

    private static class HitChanceData {
        public float hitChance = 0.0f;
        public float aimPenalty = 0.0f;
    }

    private static interface LOSVisitor {
        public boolean visit(IsoGridSquare var1, IsoGridSquare var2);

        public LosUtil.TestResults getResult();
    }
}

