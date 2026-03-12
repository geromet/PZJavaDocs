/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import zombie.ai.IStateCharacter;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.ILuaGameCharacterAttachedItems;
import zombie.characters.ILuaGameCharacterClothing;
import zombie.characters.ILuaGameCharacterDamage;
import zombie.characters.ILuaGameCharacterHealth;
import zombie.characters.ILuaVariableSource;
import zombie.characters.IsoGameCharacter;
import zombie.characters.Moodles.Moodles;
import zombie.characters.Safety;
import zombie.characters.Stats;
import zombie.characters.SurvivorDesc;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.characters.traits.CharacterTraits;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Literature;
import zombie.iso.ILuaIsoObject;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.pathfind.Path;
import zombie.pathfind.PathFindBehavior2;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.Recipe;
import zombie.ui.UIFont;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public interface ILuaGameCharacter
extends ILuaIsoObject,
ILuaVariableSource,
ILuaGameCharacterAttachedItems,
ILuaGameCharacterDamage,
ILuaGameCharacterClothing,
ILuaGameCharacterHealth,
IStateCharacter {
    public String getFullName();

    public SurvivorDesc getDescriptor();

    public void setDescriptor(SurvivorDesc var1);

    public boolean isRangedWeaponEmpty();

    public void setRangedWeaponEmpty(boolean var1);

    public BaseVisual getVisual();

    public BaseCharacterSoundEmitter getEmitter();

    public void resetModel();

    public void resetModelNextFrame();

    public IsoSpriteInstance getSpriteDef();

    public boolean hasItems(String var1, int var2);

    public int getXpForLevel(int var1);

    public IsoGameCharacter.XP getXp();

    public boolean isAsleep();

    public void setAsleep(boolean var1);

    public boolean isResting();

    public void setIsResting(boolean var1);

    public int getZombieKills();

    public void setForceWakeUpTime(float var1);

    public ItemContainer getInventory();

    public InventoryItem getPrimaryHandItem();

    public void setPrimaryHandItem(InventoryItem var1);

    public InventoryItem getSecondaryHandItem();

    public void setSecondaryHandItem(InventoryItem var1);

    public boolean hasEquipped(String var1);

    public boolean hasEquippedTag(ItemTag var1);

    public boolean hasWornTag(ItemTag var1);

    public boolean isHandItem(InventoryItem var1);

    public boolean isPrimaryHandItem(InventoryItem var1);

    public boolean isSecondaryHandItem(InventoryItem var1);

    public boolean isItemInBothHands(InventoryItem var1);

    public boolean removeFromHands(InventoryItem var1);

    public void setSpeakColourInfo(ColorInfo var1);

    public boolean isSpeaking();

    public Moodles getMoodles();

    public Stats getStats();

    public CharacterTraits getCharacterTraits();

    public int getMaxWeight();

    public void PlayAnim(String var1);

    public void PlayAnimWithSpeed(String var1, float var2);

    public void PlayAnimUnlooped(String var1);

    public void StartTimedActionAnim(String var1);

    public void StartTimedActionAnim(String var1, String var2);

    public void StopTimedActionAnim();

    public float getAnimationTimeDelta();

    public Stack<BaseAction> getCharacterActions();

    public void StartAction(BaseAction var1);

    public void StopAllActionQueue();

    public int getPerkLevel(PerkFactory.Perk var1);

    public IsoGameCharacter.PerkInfo getPerkInfo(PerkFactory.Perk var1);

    public void setPerkLevelDebug(PerkFactory.Perk var1, int var2);

    public void LoseLevel(PerkFactory.Perk var1);

    public void LevelPerk(PerkFactory.Perk var1, boolean var2);

    public void LevelPerk(PerkFactory.Perk var1);

    public void ReadLiterature(Literature var1);

    public void Callout();

    public boolean IsSpeaking();

    public void Say(String var1);

    public void Say(String var1, float var2, float var3, float var4, UIFont var5, float var6, String var7);

    public void setHaloNote(String var1);

    public void setHaloNote(String var1, float var2);

    public void setHaloNote(String var1, int var2, int var3, int var4, float var5);

    public void initSpritePartsEmpty();

    public boolean hasTrait(CharacterTrait var1);

    public void pathToLocation(int var1, int var2, int var3);

    public void pathToLocationF(float var1, float var2, float var3);

    public boolean isEnduranceSufficientForAction();

    public void smashCarWindow(VehiclePart var1);

    public void smashWindow(IsoWindow var1);

    public void openWindow(IsoWindow var1);

    public void closeWindow(IsoWindow var1);

    public void climbThroughWindow(IsoWindow var1);

    public void climbThroughWindow(IsoWindow var1, Integer var2);

    public void climbThroughWindowFrame(IsoWindowFrame var1);

    public void climbSheetRope();

    public void climbDownSheetRope();

    public boolean canClimbSheetRope(IsoGridSquare var1);

    public boolean canClimbDownSheetRopeInCurrentSquare();

    public boolean canClimbDownSheetRope(IsoGridSquare var1);

    public void climbThroughWindow(IsoThumpable var1);

    public void climbThroughWindow(IsoThumpable var1, Integer var2);

    public void climbOverFence(IsoDirections var1);

    public boolean isAboveTopOfStairs();

    public double getHoursSurvived();

    public boolean isOutside();

    public boolean isFemale();

    public void setFemale(boolean var1);

    public boolean isZombie();

    public boolean isEquipped(InventoryItem var1);

    public boolean isEquippedClothing(InventoryItem var1);

    public boolean isAttachedItem(InventoryItem var1);

    public void faceThisObject(IsoObject var1);

    public void facePosition(int var1, int var2);

    public void faceThisObjectAlt(IsoObject var1);

    public int getAlreadyReadPages(String var1);

    public void setAlreadyReadPages(String var1, int var2);

    public Safety getSafety();

    public void setSafety(Safety var1);

    public float getMeleeDelay();

    public void setMeleeDelay(float var1);

    public float getRecoilDelay();

    public void setRecoilDelay(float var1);

    public int getMaintenanceMod();

    public int getWeaponLevel();

    public int getWeaponLevel(HandWeapon var1);

    public float getHammerSoundMod();

    public float getWeldingSoundMod();

    public boolean isGodMod();

    public void setGodMod(boolean var1);

    public BaseVehicle getVehicle();

    public void setVehicle(BaseVehicle var1);

    public float getInventoryWeight();

    public void modifyTraitXPBoost(CharacterTrait var1, boolean var2);

    public void modifyTraitXPBoost(CharacterTraitDefinition var1, boolean var2);

    public List<String> getKnownRecipes();

    public boolean isRecipeKnown(Recipe var1);

    public boolean isRecipeKnown(String var1);

    public void addKnownMediaLine(String var1);

    public void removeKnownMediaLine(String var1);

    public void clearKnownMediaLines();

    public boolean isKnownMediaLine(String var1);

    public long playSound(String var1);

    public long playSoundLocal(String var1);

    public void stopOrTriggerSound(long var1);

    public void addWorldSoundUnlessInvisible(int var1, int var2, boolean var3);

    public boolean isKnownPoison(InventoryItem var1);

    public boolean isKnownPoison(Item var1);

    public String getBedType();

    public void setBedType(String var1);

    public Path getPath2();

    public void setPath2(Path var1);

    public PathFindBehavior2 getPathFindBehavior2();

    public IsoObject getBed();

    public void setBed(IsoObject var1);

    public boolean isReading();

    public void setReading(boolean var1);

    public float getTimeSinceLastSmoke();

    public void setTimeSinceLastSmoke(float var1);

    public boolean isInvisible();

    public void setInvisible(boolean var1);

    public boolean isDriving();

    public boolean isInARoom();

    public boolean isUnlimitedCarry();

    public void setUnlimitedCarry(boolean var1);

    public boolean isBuildCheat();

    public void setBuildCheat(boolean var1);

    public boolean isFarmingCheat();

    public void setFarmingCheat(boolean var1);

    public boolean isFishingCheat();

    public void setFishingCheat(boolean var1);

    public boolean isHealthCheat();

    public void setHealthCheat(boolean var1);

    public boolean isMechanicsCheat();

    public void setMechanicsCheat(boolean var1);

    public boolean isMovablesCheat();

    public void setMovablesCheat(boolean var1);

    public boolean isAnimalCheat();

    public void setAnimalCheat(boolean var1);

    public boolean isAnimalExtraValuesCheat();

    public void setAnimalExtraValuesCheat(boolean var1);

    public boolean isTimedActionInstantCheat();

    public void setTimedActionInstantCheat(boolean var1);

    public boolean isTimedActionInstant();

    public boolean isShowAdminTag();

    public void setShowAdminTag(boolean var1);

    public void reportEvent(String var1);

    public AnimatorDebugMonitor getDebugMonitor();

    public void setDebugMonitor(AnimatorDebugMonitor var1);

    public boolean isAiming();

    public boolean isTwisting();

    public boolean allowsTwist();

    public void resetBeardGrowingTime();

    public void resetHairGrowingTime();

    public float getPerkToUnit(PerkFactory.Perk var1);

    public HashMap<String, Integer> getReadLiterature();

    public boolean isLiteratureRead(String var1);

    public void addReadLiterature(String var1);

    public void addReadLiterature(String var1, int var2);

    public void addReadPrintMedia(String var1);

    public boolean isPrintMediaRead(String var1);

    public HashSet<String> getReadPrintMedia();

    public boolean hasReadMap(InventoryItem var1);

    public void addReadMap(InventoryItem var1);

    public void triggerContextualAction(String var1);

    public void triggerContextualAction(String var1, Object var2);

    public void triggerContextualAction(String var1, Object var2, Object var3);

    public void triggerContextualAction(String var1, Object var2, Object var3, Object var4);

    public void triggerContextualAction(String var1, Object var2, Object var3, Object var4, Object var5);
}

