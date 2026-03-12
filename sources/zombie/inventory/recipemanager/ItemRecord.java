/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidConsume;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemUser;
import zombie.inventory.recipemanager.ItemRecipe;
import zombie.inventory.recipemanager.RecipeMonitor;
import zombie.inventory.recipemanager.SourceRecord;
import zombie.inventory.recipemanager.SourceType;
import zombie.inventory.recipemanager.UsedItemProperties;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.GameClient;

public class ItemRecord {
    private static final ArrayDeque<ItemRecord> pool = new ArrayDeque();
    private ItemRecipe itemRecipe;
    private InventoryItem item;
    private SourceRecord sourceRecord;
    private SourceType sourceType;

    protected static ItemRecord alloc(ItemRecipe itemRecipe, InventoryItem item) {
        return pool.isEmpty() ? new ItemRecord().init(itemRecipe, item) : pool.pop().init(itemRecipe, item);
    }

    protected static void release(ItemRecord o) {
        assert (!pool.contains(o));
        pool.push(o.reset());
    }

    private ItemRecord() {
    }

    private ItemRecord init(ItemRecipe itemRecipe, InventoryItem item) {
        this.itemRecipe = itemRecipe;
        this.item = item;
        return this;
    }

    private ItemRecord reset() {
        this.itemRecipe = null;
        this.item = null;
        this.sourceType = null;
        this.sourceRecord = null;
        return this;
    }

    public String toString() {
        return "item:" + String.valueOf(this.item) + ", sourceType=" + String.valueOf(this.sourceType);
    }

    protected SourceType getSourceType() {
        return this.sourceType;
    }

    protected InventoryItem getItem() {
        return this.item;
    }

    protected void setSource(SourceRecord sourceRecord, SourceType sourceType) {
        this.sourceRecord = sourceRecord;
        this.sourceType = sourceType;
    }

    protected int getPriority() {
        if (this.itemRecipe.getSelectedItem() != null && this.itemRecipe.getSelectedItem() == this.item) {
            return 0;
        }
        if (this.item.isEquipped()) {
            return 1;
        }
        if (this.item.isInPlayerInventory()) {
            return 2;
        }
        return 3;
    }

    protected float getUses() {
        if (this.sourceRecord.isKeep()) {
            return 1.0E8f;
        }
        if (this.sourceType.isUsesFluid()) {
            if (this.item.getFluidContainer() != null) {
                return this.item.getFluidContainer().getAmount();
            }
            return 0.0f;
        }
        if (this.sourceRecord.isUseIsItemCount()) {
            return 1.0f;
        }
        if (this.item instanceof DrainableComboItem) {
            return this.item.getCurrentUses();
        }
        InventoryItem inventoryItem = this.item;
        if (inventoryItem instanceof Food) {
            Food foodItem = (Food)inventoryItem;
            return (int)(-foodItem.getHungerChange() * 100.0f);
        }
        return 1.0f;
    }

    protected float applyUses(float usesRequired, UsedItemProperties usedItemProperties) {
        if (usesRequired <= 0.0f) {
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("uses required should not be <= 0", RecipeMonitor.colNeg);
            }
            DebugLog.General.error("uses required should not be <= 0");
            return 0.0f;
        }
        if (this.sourceRecord.isKeep()) {
            usedItemProperties.addInventoryItem(this.item);
            usesRequired -= 1.0f;
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> source = keep, use = -1", RecipeMonitor.colGray);
            }
        } else if (this.sourceType.isUsesFluid()) {
            float fluidAmount = this.getUses();
            float fluidToRemove = PZMath.min(fluidAmount, usesRequired);
            FluidConsume fluidConsume = this.item.getFluidContainer().removeFluid(fluidToRemove, true);
            usedItemProperties.addFluidConsume(fluidConsume);
            fluidConsume.release();
            usesRequired -= fluidToRemove;
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> source = fluid, use = -" + fluidToRemove, RecipeMonitor.colGray);
            }
        } else if (this.sourceRecord.isDestroy()) {
            usedItemProperties.addInventoryItem(this.item);
            ItemUser.RemoveItem(this.item);
            usesRequired -= 1.0f;
        } else if (this.item instanceof DrainableComboItem || this.item instanceof Food) {
            usedItemProperties.addInventoryItem(this.item);
            float uses = this.getUses();
            float usesToApply = PZMath.min(uses, usesRequired);
            this.applyUsesToItem(usesToApply);
            usesRequired -= usesToApply;
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> source = drainable, use = -" + usesToApply, RecipeMonitor.colGray);
            }
        } else {
            usedItemProperties.addInventoryItem(this.item);
            usesRequired -= (float)ItemUser.UseItem(this.item, true, false, (int)usesRequired, false, false);
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> source = generic, use = -1", RecipeMonitor.colGray);
            }
        }
        return usesRequired;
    }

    private void applyUsesToItem(float usesRequired) {
        InventoryItem inventoryItem = this.item;
        if (inventoryItem instanceof DrainableComboItem) {
            DrainableComboItem drainableItem = (DrainableComboItem)inventoryItem;
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> applyUsesToItem: drainable, uses = " + drainableItem.getCurrentUses(), RecipeMonitor.colGray);
            }
            drainableItem.setCurrentUses(drainableItem.getCurrentUses() - (int)usesRequired);
            if (this.getUses() < 1.0f) {
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("> applyUsesToItem: drainable, new uses = " + drainableItem.getCurrentUses() + " -> ItemUser.UseItem", RecipeMonitor.colGray);
                }
                ItemUser.UseItem(drainableItem);
                return;
            }
            if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> applyUsesToItem: drainable, new uses = " + drainableItem.getCurrentUses(), RecipeMonitor.colGray);
            }
            if (GameClient.client && !this.item.isInPlayerInventory()) {
                GameClient.instance.sendItemStats(this.item);
            }
        }
        if ((inventoryItem = this.item) instanceof Food) {
            Food food = (Food)inventoryItem;
            if (food.getHungerChange() < 0.0f) {
                float percentage;
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("> applyUsesToItem: food, hungerChange = " + food.getHungerChange(), RecipeMonitor.colGray);
                }
                float use = Math.min(-food.getHungerChange() * 100.0f, usesRequired);
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("> applyUsesToItem: food, use = " + use, RecipeMonitor.colGray);
                }
                if ((percentage = use / (-food.getHungerChange() * 100.0f)) < 0.0f) {
                    percentage = 0.0f;
                }
                if (percentage > 1.0f) {
                    percentage = 1.0f;
                }
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("> applyUsesToItem: food, percentage = " + percentage, RecipeMonitor.colGray);
                }
                food.setHungChange(food.getHungChange() - food.getHungChange() * percentage);
                food.setCalories(food.getCalories() - food.getCalories() * percentage);
                food.setCarbohydrates(food.getCarbohydrates() - food.getCarbohydrates() * percentage);
                food.setLipids(food.getLipids() - food.getLipids() * percentage);
                food.setProteins(food.getProteins() - food.getProteins() * percentage);
                food.setThirstChange(food.getThirstChangeUnmodified() - food.getThirstChangeUnmodified() * percentage);
                food.setFluReduction(food.getFluReduction() - (int)((float)food.getFluReduction() * percentage));
                food.setPainReduction(food.getPainReduction() - food.getPainReduction() * percentage);
                food.setEndChange(food.getEnduranceChangeUnmodified() - food.getEnduranceChangeUnmodified() * percentage);
                food.setFoodSicknessChange(food.getFoodSicknessChange() - (int)((float)food.getFoodSicknessChange() * percentage));
                food.setStressChange(food.getStressChangeUnmodified() - food.getStressChangeUnmodified() * percentage);
                food.setFatigueChange(food.getFatigueChange() - food.getFatigueChange() * percentage);
                if ((double)food.getHungerChange() > -0.01) {
                    if (RecipeMonitor.canLog()) {
                        RecipeMonitor.Log("> applyUsesToItem: food, new hungerChange = " + food.getHungerChange() + " -> ItemUser.UserItem", RecipeMonitor.colGray);
                    }
                    ItemUser.UseItem(food);
                    return;
                }
                if (RecipeMonitor.canLog()) {
                    RecipeMonitor.Log("> applyUsesToItem: food, new hungerChange = " + food.getHungerChange(), RecipeMonitor.colGray);
                }
                if (GameClient.client && !this.item.isInPlayerInventory()) {
                    GameClient.instance.sendItemStats(this.item);
                }
            } else if (RecipeMonitor.canLog()) {
                RecipeMonitor.Log("> [WARN] applyUsesToItem: food, hungerChange = " + food.getHungerChange(), RecipeMonitor.colNeg);
            }
        }
    }
}

