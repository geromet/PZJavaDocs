/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.FurnaceLogic;
import zombie.entity.components.crafting.StartMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class FurnaceLogicSystem
extends EngineSystem {
    private EntityBucket furnaceLogicEntities;
    private final List<Resource> tempSlotInputResources = new ArrayList<Resource>();
    private final List<Resource> tempSlotOutputResources = new ArrayList<Resource>();
    private final CraftRecipeData slotCraftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    private final CraftRecipeData slotCraftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    private final CraftRecipeMonitor debugMonitor = CraftRecipeMonitor.Create();

    public FurnaceLogicSystem(int updatePriority) {
        super(false, true, updatePriority);
        this.debugMonitor.setPrintToConsole(true);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.furnaceLogicEntities = engine.getBucket(Family.all(ComponentType.FurnaceLogic, ComponentType.Resources).get());
    }

    private boolean isValidEntity(GameEntity entity) {
        return entity.isEntityValid() && entity.isValidEngineEntity();
    }

    @Override
    public void updateSimulation() {
        if (GameClient.client) {
            return;
        }
        ImmutableArray<GameEntity> entities = this.furnaceLogicEntities.getEntities();
        if (entities.size() == 0) {
            return;
        }
        for (int i = 0; i < entities.size(); ++i) {
            ResourceGroup outputResources;
            GameEntity entity = entities.get(i);
            if (!this.isValidEntity(entity)) continue;
            FurnaceLogic furnaceLogic = (FurnaceLogic)entity.getComponent(ComponentType.FurnaceLogic);
            Resources resources = (Resources)entity.getComponent(ComponentType.Resources);
            if (!furnaceLogic.isValid() || !resources.isValid()) continue;
            ResourceGroup fuelInputResources = resources.getResourceGroup(furnaceLogic.getFuelInputsGroupName());
            ResourceGroup fuelOutputResources = resources.getResourceGroup(furnaceLogic.getFuelOutputsGroupName());
            ResourceGroup inputResources = resources.getResourceGroup(furnaceLogic.getFurnaceInputsGroupName());
            if (!this.verifyFurnaceSlots(furnaceLogic, inputResources, outputResources = resources.getResourceGroup(furnaceLogic.getFurnaceOutputsGroupName())) || furnaceLogic.getSlotSize() == 0) continue;
            this.updateFurnaceLogic(furnaceLogic, fuelInputResources, fuelOutputResources);
        }
    }

    private boolean verifyFurnaceSlots(FurnaceLogic logic, ResourceGroup inputs, ResourceGroup outputs) {
        if (inputs == null || inputs.getResources().isEmpty() || outputs == null || outputs.getResources().isEmpty()) {
            return false;
        }
        if (inputs.getResources().size() != outputs.getResources().size()) {
            return false;
        }
        if (logic.getSlotSize() != 0 && logic.getSlotSize() != inputs.getResources().size()) {
            logic.clearSlots();
        }
        for (int i = 0; i < inputs.getResources().size(); ++i) {
            Resource input = inputs.getResources().get(i);
            Resource output = outputs.getResources().get(i);
            if (input.getType() != ResourceType.Item || output.getType() != ResourceType.Item) {
                DebugLog.General.warn("FurnaceSlot must be of type item!");
                logic.clearSlots();
                return false;
            }
            FurnaceLogic.FurnaceSlot slot = logic.getSlot(i);
            if (slot == null) {
                slot = logic.createSlot(i);
            }
            slot.initialize(input.getId(), output.getId());
        }
        return true;
    }

    private void updateFurnaceLogic(FurnaceLogic logic, ResourceGroup fuelInputs, ResourceGroup fuelOutputs) {
        if (logic.isRunning()) {
            if (fuelOutputs != null && fuelOutputs.isDirty() && !logic.getCraftData().canCreateOutputs(fuelOutputs.getResources())) {
                this.cancel(logic, fuelOutputs);
                return;
            }
            if (logic.isFinished()) {
                this.finish(logic, fuelOutputs);
                return;
            }
            this.updateFurnaceSlots(logic, false);
            logic.setElapsedTime(logic.getElapsedTime() + 1);
            if (logic.getElapsedTime() > logic.getCurrentRecipe().getTime()) {
                logic.setElapsedTime(logic.getCurrentRecipe().getTime());
            }
            if (logic.isStopRequested()) {
                this.cancel(logic, fuelOutputs);
                logic.setStopRequested(false);
                logic.setRequestingPlayer(null);
            }
        } else if (logic.isStartRequested()) {
            this.start(logic, StartMode.Manual, logic.getRequestingPlayer(), fuelInputs);
            logic.setStartRequested(false);
            logic.setRequestingPlayer(null);
        } else if (logic.getStartMode() == StartMode.Automatic && (logic.isDoAutomaticCraftCheck() || fuelInputs.isDirty())) {
            this.start(logic, StartMode.Automatic, null, fuelInputs);
            logic.setDoAutomaticCraftCheck(false);
        }
    }

    private void updateFurnaceSlots(FurnaceLogic logic, boolean cancel) {
        int size = logic.getSlotSize();
        for (int index = 0; index < size; ++index) {
            ResourceItem outputResource;
            ResourceItem inputResource;
            FurnaceLogic.FurnaceSlot slot = logic.getSlot(index);
            if (slot == null) continue;
            if (cancel) {
                slot.clearRecipe();
                inputResource = logic.getInputSlotResource(index);
                outputResource = logic.getOutputSlotResource(index);
                if (inputResource != null) {
                    inputResource.setProgress(0.0);
                }
                if (outputResource == null) continue;
                outputResource.setProgress(0.0);
                continue;
            }
            inputResource = logic.getInputSlotResource(index);
            outputResource = logic.getOutputSlotResource(index);
            if (((Resource)inputResource).isEmpty() || ((Resource)outputResource).isFull()) {
                if (slot.getCurrentRecipe() == null) continue;
                slot.clearRecipe();
                continue;
            }
            if (slot.getCurrentRecipe() == null) {
                this.getSlotRecipe(logic, slot, index);
                if (slot.getCurrentRecipe() == null) continue;
                slot.setElapsedTime(slot.getElapsedTime() + 1);
                continue;
            }
            CraftRecipe recipe = slot.getCurrentRecipe();
            if (inputResource.isDirty() || outputResource.isDirty()) {
                this.setTempSlotResources(inputResource, outputResource);
                this.slotCraftTestData.setRecipe(slot.getCurrentRecipe());
                if (!this.slotCraftTestData.canConsumeInputs(this.tempSlotInputResources) || !this.slotCraftTestData.canCreateOutputs(this.tempSlotOutputResources)) {
                    slot.clearRecipe();
                    continue;
                }
            }
            slot.setElapsedTime(slot.getElapsedTime() + 1);
            if (slot.getElapsedTime() >= recipe.getTime()) {
                this.craftSlotRecipe(logic, slot, inputResource, outputResource);
            }
            inputResource.setProgress((double)slot.getElapsedTime() / (double)recipe.getTime());
        }
    }

    private void setTempSlotResources(Resource inputResource, Resource outputResource) {
        this.tempSlotInputResources.clear();
        this.tempSlotOutputResources.clear();
        this.tempSlotInputResources.add(inputResource);
        this.tempSlotOutputResources.add(outputResource);
    }

    private void getSlotRecipe(FurnaceLogic logic, FurnaceLogic.FurnaceSlot slot, int index) {
        ResourceItem inputResource = logic.getInputSlotResource(index);
        ResourceItem outputResource = logic.getOutputSlotResource(index);
        this.setTempSlotResources(inputResource, outputResource);
        this.slotCraftTestData.setRecipe(null);
        CraftRecipe recipe = CraftUtil.getPossibleRecipe(this.slotCraftTestData, logic.getFurnaceRecipes(), this.tempSlotInputResources, this.tempSlotOutputResources);
        if (recipe != null) {
            slot.setRecipe(recipe);
        } else {
            slot.clearRecipe();
        }
    }

    private void craftSlotRecipe(FurnaceLogic logic, FurnaceLogic.FurnaceSlot slot, Resource input, Resource output) {
        this.setTempSlotResources(input, output);
        this.slotCraftData.setRecipe(slot.getCurrentRecipe());
        if (this.slotCraftData.canConsumeInputs(this.tempSlotInputResources) && this.slotCraftData.canCreateOutputs(this.tempSlotOutputResources)) {
            this.slotCraftData.consumeInputs(this.tempSlotInputResources);
            this.slotCraftData.createOutputs(this.tempSlotOutputResources);
        }
        slot.clearRecipe();
    }

    private void start(FurnaceLogic logic, StartMode startMode, IsoPlayer player, ResourceGroup fuelInputs) {
        if (GameClient.client) {
            return;
        }
        if (logic.canStart(startMode, player)) {
            CraftRecipe recipe = logic.getPossibleRecipe();
            if (recipe == null) {
                return;
            }
            logic.setRecipe(recipe);
            if (!logic.getCraftData().consumeInputs(fuelInputs.getResources())) {
                logic.setRecipe(null);
                return;
            }
            logic.getCraftData().luaCallOnStart();
            if (GameServer.server) {
                // empty if block
            }
        }
    }

    private void cancel(FurnaceLogic logic, ResourceGroup fuelOutputs) {
        this.stop(logic, true, fuelOutputs);
    }

    private void finish(FurnaceLogic logic, ResourceGroup fuelOutputs) {
        this.stop(logic, false, fuelOutputs);
    }

    private void stop(FurnaceLogic logic, boolean isCancelled, ResourceGroup fuelOutputs) {
        if (GameClient.client) {
            return;
        }
        if (!logic.isValid()) {
            return;
        }
        if (!logic.isRunning()) {
            return;
        }
        if (isCancelled) {
            logic.getCraftData().luaCallOnFailed();
        } else if (logic.getCraftData().createOutputs(fuelOutputs.getResources())) {
            logic.getCraftData().luaCallOnCreate();
        }
        this.updateFurnaceSlots(logic, true);
        logic.setDoAutomaticCraftCheck(true);
        logic.setRecipe(null);
        if (GameServer.server) {
            // empty if block
        }
    }
}

