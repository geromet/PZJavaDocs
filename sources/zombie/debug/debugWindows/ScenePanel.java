/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import gnu.trove.list.array.TIntArrayList;
import imgui.ImColor;
import imgui.ImGui;
import imgui.type.ImInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.debug.BaseDebugWindow;
import zombie.debug.BooleanDebugOption;
import zombie.debug.DebugContext;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.debug.debugWindows.PZImGui;
import zombie.debug.debugWindows.Wrappers;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.CharacterTrait;
import zombie.vehicles.BaseVehicle;

public class ScenePanel
extends BaseDebugWindow {
    private String selectedNode = "";
    private final int defaultflags = 192;
    private final int selectedflags = 193;
    private static final HashMap<String, String> filterMap = new HashMap();
    private final TIntArrayList zTracker = new TIntArrayList();
    private boolean zVisibleFilter;
    private float zDistanceFilter = 100.0f;
    private ObjectTreeSort zSort = ObjectTreeSort.None;
    private final TIntArrayList aTracker = new TIntArrayList();
    private boolean aVisibleFilter;
    private float aDistanceFilter = 100.0f;
    private ObjectTreeSort aSort = ObjectTreeSort.None;
    private final float vDistanceFilter = 100.0f;
    private final ObjectTreeSort vSort = ObjectTreeSort.None;

    @Override
    public String getTitle() {
        return "Scene";
    }

    @Override
    public int getWindowFlags() {
        return 64;
    }

    boolean doTreeNode(String id, String label, boolean leaf) {
        int flags;
        boolean selected = id.equals(this.selectedNode);
        int n = flags = selected ? 193 : 192;
        if (leaf) {
            flags |= 0x100;
        }
        boolean t = ImGui.treeNodeEx(id, flags, label);
        if (ImGui.isItemClicked()) {
            this.selectedNode = id;
        }
        return t;
    }

    boolean doTreeNode(String id, String label, boolean leaf, boolean defaultOpen) {
        int flags;
        boolean selected = id.equals(this.selectedNode);
        int n = flags = selected ? 193 : 192;
        if (leaf) {
            flags |= 0x100;
        }
        boolean t = ImGui.treeNodeEx(id, flags |= 0x20, label);
        if (ImGui.isItemClicked()) {
            this.selectedNode = id;
        }
        return t;
    }

    @Override
    protected void doWindowContents() {
        if (this.doTreeNode("optionsTree", "Settings", false)) {
            if (this.doTreeNode("debugOptionsTree", "Debug", false)) {
                this.updateFilter("options");
                for (int i = 0; i < DebugOptions.instance.getOptionCount(); ++i) {
                    BooleanDebugOption opt = DebugOptions.instance.getOptionByIndex(i);
                    if (this.isFiltered("options", opt.getName())) continue;
                    PZImGui.checkboxWithDefaultValueHighlight(opt.getName(), opt::getValue, opt::setValue, opt.getDefaultValue(), ImColor.rgb(255, 255, 0));
                }
                ImGui.treePop();
            }
            if (this.doTreeNode("logOptionsTree", "Logging", false)) {
                this.updateFilter("logging");
                ArrayList<DebugType> debugs = DebugLog.getDebugTypes();
                for (int i = 0; i < debugs.size(); ++i) {
                    boolean showLogSeverityCombo;
                    if (this.isFiltered("logging", debugs.get(i).name()) || !(showLogSeverityCombo = Wrappers.checkbox(debugs.get(i).name(), DebugLog::isEnabled, DebugLog::setLogEnabled, debugs.get(i)))) continue;
                    DebugType debugType = debugs.get(i);
                    LogSeverity current = DebugLog.getLogSeverity(debugType);
                    LogSeverity[] values2 = LogSeverity.values();
                    String[] items = (String[])Arrays.stream(values2).map(Enum::name).toArray(String[]::new);
                    float maxWidth = 0.0f;
                    for (String item : items) {
                        float w = ImGui.calcTextSize((String)item).x;
                        if (!(w > maxWidth)) continue;
                        maxWidth = w;
                    }
                    ImGui.sameLine();
                    ImGui.pushItemWidth(maxWidth + 20.0f);
                    String comboLabel = "##combo." + debugType.name();
                    ImInt currentIndex = new ImInt(current.ordinal());
                    if (ImGui.combo(comboLabel, currentIndex, items, items.length)) {
                        DebugLog.setLogSeverity(debugType, values2[currentIndex.get()]);
                    }
                    ImGui.popItemWidth();
                }
                ImGui.treePop();
            }
            ImGui.treePop();
        }
        if (IsoWorld.instance.currentCell != null && this.doTreeNode("worldTree", "IsoWorld", false, true)) {
            if (this.doTreeNode("metaWorldTree", "IsoMetaWorld", false, true)) {
                ImGui.treePop();
            }
            if (this.doTreeNode("playerTree", "IsoPlayers", false, true)) {
                this.doAllPlayers();
                ImGui.treePop();
            }
            if (this.doTreeNode("zombieTree", "IsoZombies", false)) {
                this.doAllZombies((ArrayList)IsoWorld.instance.currentCell.getZombieList().clone());
                ImGui.treePop();
            }
            if (this.doTreeNode("animalTree", "IsoAnimal", false)) {
                this.doAllAnimals(IsoWorld.instance.currentCell.getObjectList().stream().filter(o -> o instanceof IsoAnimal).map(o -> (IsoAnimal)o).collect(Collectors.toCollection(ArrayList::new)));
                ImGui.treePop();
            }
            if (this.doTreeNode("vehicleTree", "BaseVehicle", false)) {
                this.doAllVehicles((ArrayList)IsoWorld.instance.currentCell.vehicles.clone());
                ImGui.treePop();
            }
            if (this.doTreeNode("cellTree", "IsoCell", false, true)) {
                ImGui.treePop();
            }
            ImGui.treePop();
        }
    }

    private void highlight(IsoMovingObject obj) {
        obj.setAlphaAndTarget(IsoPlayer.getInstance().getPlayerNum(), 1.0f);
        obj.setOutlineHighlightCol(Core.getInstance().getBadHighlitedColor());
        obj.setOutlineHighlight(IsoPlayer.getInstance().getPlayerNum(), true);
    }

    private void doAllPlayers() {
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            if (IsoPlayer.getPlayers().get(i) == null) continue;
            ImGui.beginGroup();
            this.doPlayer("player" + i, IsoPlayer.getPlayers().get(i));
            ImGui.endGroup();
            if (!ImGui.isItemHovered(416)) continue;
            this.highlight(IsoPlayer.getPlayers().get(i));
        }
    }

    private void doPlayer(String id, IsoPlayer player) {
        if (this.doTreeNode(id, player.getDescriptor().getFullname(), false)) {
            this.doPopupMenu(id, player);
            if (this.doTreeNode(id + "Cheats", "Cheats", false)) {
                Wrappers.checkbox("Invisible", player::isInvisible, player::setInvisible);
                Wrappers.checkbox("God Mode", player::isGodMod, player::setGodMod);
                Wrappers.checkbox("Ghost Mode", player::isGhostMode, player::setGhostMode);
                Wrappers.checkbox("No Clip", player::isNoClip, player::setNoClip);
                Wrappers.checkbox("Timed Actions", player::isTimedActionInstantCheat, player::setTimedActionInstantCheat);
                Wrappers.checkbox("Unlimited Carry", player::isUnlimitedCarry, player::setUnlimitedCarry);
                Wrappers.checkbox("Unlimited Endurance", player::isUnlimitedEndurance, player::setUnlimitedEndurance);
                Wrappers.checkbox("Unlimited Ammo", player::isUnlimitedAmmo, player::setUnlimitedAmmo);
                Wrappers.checkbox("Recipes", player::isKnowAllRecipes, player::setKnowAllRecipes);
                Wrappers.checkbox("Build", player::isBuildCheat, player::setBuildCheat);
                Wrappers.checkbox("Farming", player::isFarmingCheat, player::setFarmingCheat);
                Wrappers.checkbox("Fishing", player::isFishingCheat, player::setFishingCheat);
                Wrappers.checkbox("Health", player::isHealthCheat, player::setHealthCheat);
                Wrappers.checkbox("Mechanics", player::isMechanicsCheat, player::setMechanicsCheat);
                Wrappers.checkbox("Movables", player::isMovablesCheat, player::setMovablesCheat);
                ImGui.treePop();
            }
            if (this.doTreeNode(id + "Moodles", "Moodles", false)) {
                for (CharacterStat stat : CharacterStat.ORDERED_STATS) {
                    float newValue = Wrappers.sliderFloat(stat.getId(), player.getStats().get(stat), stat.getMinimumValue(), stat.getMaximumValue());
                    player.getStats().set(stat, newValue);
                }
                ImGui.text(PZMath.roundFloat(player.getStats().getNicotineStress(), 3) + " :Effective Stress");
                Wrappers.sliderFloat("Time since last smoke", 0.0f, 10.0f, player::getTimeSinceLastSmoke, player::setTimeSinceLastSmoke);
                Wrappers.sliderFloat("Cold Damage (hypo 4)", 0.0f, 1.0f, player.getBodyDamage()::getColdDamageStage, player.getBodyDamage()::setColdDamageStage);
                Wrappers.sliderFloat("Overall Health", 0.0f, 100.0f, player.getBodyDamage()::getOverallBodyHealth, player.getBodyDamage()::setOverallBodyHealth);
                Wrappers.sliderFloat("Catch Cold Strength", 0.0f, 100.0f, player.getBodyDamage()::getColdStrength, player.getBodyDamage()::setColdStrength);
                Wrappers.checkbox("Is Infected", player.getBodyDamage()::IsInfected, player.getBodyDamage()::setInfected);
                Wrappers.checkbox("Is Fake Infected", player.getBodyDamage()::IsFakeInfected, player.getBodyDamage()::setIsFakeInfected);
                Wrappers.sliderFloat("Calories", -2200.0f, 3700.0f, player.getNutrition()::getCalories, player.getNutrition()::setCalories);
                Wrappers.sliderDouble("Weight", 30.0, 130.0, player.getNutrition()::getWeight, player.getNutrition()::setWeight);
                ImGui.treePop();
            }
            if (this.doTreeNode(id + "Traits", "Traits", false)) {
                this.updateFilter("traits");
                for (CharacterTraitDefinition trait : CharacterTraitDefinition.characterTraitDefinitions.values()) {
                    if (this.isFiltered("traits", trait.getUIName())) continue;
                    boolean disable = false;
                    for (CharacterTrait mutuallyExclusiveTrait : trait.getMutuallyExclusiveTraits()) {
                        if (!player.hasTrait(mutuallyExclusiveTrait)) continue;
                        disable = true;
                        break;
                    }
                    ImGui.beginDisabled(disable);
                    boolean hadTrait = player.hasTrait(trait.getType());
                    boolean result = Wrappers.checkbox(trait.getUIName() + (trait.isFree() ? " (Non-selectable)" : ""), hadTrait);
                    if (result != hadTrait) {
                        if (result) {
                            player.getCharacterTraits().add(trait.getType());
                        } else {
                            player.getCharacterTraits().remove(trait.getType());
                        }
                    }
                    ImGui.endDisabled();
                }
                ImGui.treePop();
            }
            if (this.doTreeNode(id + "Perks", "Perks", false)) {
                for (int i = 0; i < PerkFactory.PerkList.size(); ++i) {
                    PerkFactory.Perk perk = PerkFactory.PerkList.get(i);
                    if (perk.getParent() == PerkFactory.Perks.None) continue;
                    Wrappers.sliderInt(perk.name, 0, 10, player::getPerkLevel, player::setPerkLevelDebug, perk);
                }
                ImGui.treePop();
            }
            this.doAnimTable(id + "Anim", player);
            ImGui.treePop();
        } else {
            this.doPopupMenu(id, player);
        }
    }

    private void doAllZombies(ArrayList<IsoZombie> zombies) {
        this.zVisibleFilter = Wrappers.checkbox("Visible Only", this.zVisibleFilter);
        this.zDistanceFilter = Wrappers.sliderFloat("Range", this.zDistanceFilter, 1.0f, 100.0f);
        if (ImGui.isItemHovered() || ImGui.isItemFocused()) {
            LineDrawer.DrawIsoCircle(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), this.zDistanceFilter, 32, 0.0f, 1.0f, 0.0f, 0.3f);
        }
        this.zSort = this.doSortCombobox(this.zSort);
        if (this.zSort != ObjectTreeSort.None) {
            zombies.sort(this.zSort.comparator);
        }
        for (int i = 0; i < zombies.size(); ++i) {
            if (!this.zTracker.contains(zombies.get((int)i).zombieId)) {
                if (this.zVisibleFilter && !IsoPlayer.getInstance().getSpottedList().contains(zombies.get(i)) || PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(zombies.get(i))) >= this.zDistanceFilter) {
                    continue;
                }
            } else {
                this.highlight(zombies.get(i));
            }
            ImGui.beginGroup();
            this.doZombie("zombie" + zombies.get((int)i).zombieId, zombies.get(i));
            ImGui.endGroup();
            if (!ImGui.isItemHovered(416)) continue;
            this.highlight(zombies.get(i));
        }
    }

    private void doZombie(String id, IsoZombie zombie) {
        if (this.doTreeNode(id, String.valueOf(zombie.zombieId), false)) {
            this.doPopupMenu(id, zombie);
            boolean track = this.zTracker.contains(zombie.zombieId);
            if (Wrappers.checkbox("Track", track) != track) {
                if (track) {
                    this.zTracker.remove(zombie.zombieId);
                } else {
                    this.zTracker.add(zombie.zombieId);
                }
            }
            if (ImGui.beginTable(id + "Table", 2, 0)) {
                this.doTextRow("X:", String.valueOf(zombie.getX()));
                this.doTextRow("Y:", String.valueOf(zombie.getY()));
                this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(zombie))));
                this.doTextRow("Outfit:", zombie.getOutfitName());
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "Health:");
                ImGui.tableSetColumnIndex(1);
                Wrappers.dragFloat("", 0.0f, 100.0f, 0.005f, zombie::getHealth, zombie::setHealth);
                ImGui.endTable();
            }
            this.doAnimTable(id + "Anim", zombie);
            ImGui.treePop();
        } else {
            this.doPopupMenu(id, zombie);
        }
    }

    private ObjectTreeSort doSortCombobox(ObjectTreeSort value) {
        if (ImGui.beginCombo("Sort", value.name(), 0)) {
            for (int i = 0; i < ObjectTreeSort.values.length; ++i) {
                if (!Wrappers.selectable(ObjectTreeSort.values[i].name(), value == ObjectTreeSort.values[i])) continue;
                ImGui.setItemDefaultFocus();
                value = ObjectTreeSort.values[i];
            }
            ImGui.endCombo();
        }
        return value;
    }

    private void doAllAnimals(ArrayList<IsoAnimal> animals) {
        this.aVisibleFilter = Wrappers.checkbox("Visible Only", this.aVisibleFilter);
        this.aDistanceFilter = Wrappers.sliderFloat("Range", this.aDistanceFilter, 1.0f, 100.0f);
        if (ImGui.isItemHovered() || ImGui.isItemFocused()) {
            LineDrawer.DrawIsoCircle(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ(), this.aDistanceFilter, 32, 0.0f, 1.0f, 0.0f, 0.3f);
        }
        this.aSort = this.doSortCombobox(this.aSort);
        if (this.aSort != ObjectTreeSort.None) {
            animals.sort(this.aSort.comparator);
        }
        for (int i = 0; i < animals.size(); ++i) {
            if (!this.aTracker.contains(animals.get((int)i).animalId)) {
                if (this.aVisibleFilter && !IsoPlayer.getInstance().getSpottedList().contains(animals.get(i)) || PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(animals.get(i))) >= this.aDistanceFilter) {
                    continue;
                }
            } else {
                this.highlight(animals.get(i));
            }
            ImGui.beginGroup();
            this.doAnimal("animal" + animals.get((int)i).animalId, animals.get(i));
            ImGui.endGroup();
            if (!ImGui.isItemHovered(416)) continue;
            this.highlight(animals.get(i));
        }
    }

    private void doAnimal(String id, IsoAnimal animal) {
        if (this.doTreeNode(id, animal.animalId + " " + animal.getAnimalType(), false)) {
            this.doPopupMenu(id, animal);
            boolean track = this.aTracker.contains(animal.animalId);
            if (Wrappers.checkbox("Track", track) != track) {
                if (track) {
                    this.aTracker.remove(animal.animalId);
                } else {
                    this.aTracker.add(animal.animalId);
                }
            }
            if (ImGui.beginTable(id + "Table", 2, 0)) {
                this.doTextRow("X:", String.valueOf(animal.getX()));
                this.doTextRow("Y:", String.valueOf(animal.getY()));
                this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(animal))));
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);
                ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, "Health:");
                ImGui.tableSetColumnIndex(1);
                Wrappers.dragFloat("", 0.0f, 100.0f, 0.005f, animal::getHealth, animal::setHealth);
                ImGui.endTable();
            }
            this.doAnimTable(id + "Anim", animal);
            ImGui.treePop();
        } else {
            this.doPopupMenu(id, animal);
        }
    }

    private void doAllVehicles(ArrayList<BaseVehicle> vehicles) {
        for (int i = 0; i < vehicles.size(); ++i) {
            ImGui.beginGroup();
            this.doVehicle("vehicle" + vehicles.get((int)i).vehicleId, vehicles.get(i));
            ImGui.endGroup();
        }
    }

    private void doVehicle(String id, BaseVehicle vehicle) {
        if (this.doTreeNode(id, vehicle.vehicleId + " " + vehicle.getScriptName(), false)) {
            this.doPopupMenu(id, vehicle);
            if (ImGui.beginTable(id + "Table", 2, 0)) {
                this.doTextRow("X:", String.valueOf(vehicle.getX()));
                this.doTextRow("Y:", String.valueOf(vehicle.getY()));
                this.doTextRow("Distance:", String.valueOf(PZMath.sqrt(IsoPlayer.getInstance().getDistanceSq(vehicle))));
                this.doTextRow("Type:", vehicle.getVehicleType());
                ImGui.endTable();
            }
            ImGui.treePop();
        } else {
            this.doPopupMenu(id, vehicle);
        }
    }

    private void doAnimTable(String id, IsoGameCharacter character) {
        if (character.advancedAnimator == null) {
            return;
        }
        if (this.doTreeNode(id, "Animation Data", false)) {
            ImGui.textWrapped(character.advancedAnimator.getRootLayer().GetDebugString());
            if (ImGui.beginTable(id + "Table", 2, 1280)) {
                this.doTextRow("State:", character.getCurrentState().getName());
                for (IAnimationVariableSlot entry : character.getGameVariables()) {
                    this.doTextRow(entry.getKey() + ":", entry.getValueString());
                }
                ImGui.endTable();
            }
            ImGui.treePop();
        }
    }

    private void doTextRow(String key, String value) {
        if (key == null) {
            key = "null";
        }
        if (value == null) {
            value = "null";
        }
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(0);
        ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, key);
        ImGui.tableSetColumnIndex(1);
        ImGui.text(value);
    }

    static String doTextFilter(String value) {
        Wrappers.valueString.set(value);
        ImGui.inputText("Filter", Wrappers.valueString);
        return Wrappers.valueString.get();
    }

    private void updateFilter(String filter) {
        filterMap.put(filter, ScenePanel.doTextFilter(filterMap.get(filter)));
    }

    private boolean isFiltered(String filter, String id) {
        return !filterMap.getOrDefault(filter, "").isBlank() && !id.toLowerCase().contains(filterMap.get(filter).toLowerCase());
    }

    private void doPopupMenu(String s, Object obj) {
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.selectable("inspect class")) {
                DebugContext.instance.inspectJava(obj);
            }
            ImGui.endPopup();
        }
    }

    static {
        filterMap.put("options", "");
        filterMap.put("logging", "");
        filterMap.put("traits", "");
    }

    static enum ObjectTreeSort {
        None((o1, o2) -> 0),
        Distance((o1, o2) -> Float.compare(IsoPlayer.getInstance().getDistanceSq((IsoMovingObject)o1), IsoPlayer.getInstance().getDistanceSq((IsoMovingObject)o2))),
        ID((o1, o2) -> {
            if (o1 instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)o1;
                return Integer.compare(isoZombie.zombieId, ((IsoZombie)o2).zombieId);
            }
            if (o1 instanceof IsoAnimal) {
                IsoAnimal isoAnimal = (IsoAnimal)o1;
                return Integer.compare(isoAnimal.animalId, ((IsoAnimal)o2).animalId);
            }
            if (o1 instanceof IsoPlayer) {
                IsoPlayer isoPlayer = (IsoPlayer)o1;
                return Integer.compare(isoPlayer.getPlayerNum(), ((IsoPlayer)o2).getPlayerNum());
            }
            return Integer.compare(o1.getID(), o2.getID());
        });

        private static final ObjectTreeSort[] values;
        final Comparator<IsoMovingObject> comparator;

        private ObjectTreeSort(Comparator<IsoMovingObject> comparator) {
            this.comparator = comparator;
        }

        static {
            values = ObjectTreeSort.values();
        }
    }
}

