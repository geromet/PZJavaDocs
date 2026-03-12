/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory.recipemanager;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.entity.components.fluids.FluidConsume;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;

public class UsedItemProperties {
    boolean tainted;
    boolean cooked;
    boolean burnt;
    int poisonLevel = -1;
    int poisonPower;
    boolean rotten;
    boolean stale;
    float condition;
    float rottenness;
    int itemUsed;
    int foodUsed;

    protected void reset() {
        this.tainted = false;
        this.cooked = false;
        this.burnt = false;
        this.poisonLevel = -1;
        this.poisonPower = 0;
        this.rotten = false;
        this.stale = false;
        this.condition = 0.0f;
        this.rottenness = 0.0f;
        this.itemUsed = 0;
        this.foodUsed = 0;
    }

    protected void addFluidConsume(FluidConsume fluidConsume) {
    }

    protected void addInventoryItem(InventoryItem usedItem) {
        if (usedItem instanceof Food) {
            Food food = (Food)usedItem;
            if (food.isTainted()) {
                this.tainted = true;
            }
            if (usedItem.isCooked()) {
                this.cooked = true;
            }
            if (usedItem.isBurnt()) {
                this.burnt = true;
            }
            if (food.getPoisonDetectionLevel() >= 0) {
                this.poisonLevel = this.poisonLevel == -1 ? food.getPoisonDetectionLevel() : PZMath.min(this.poisonLevel, food.getPoisonDetectionLevel());
            }
            this.poisonPower = PZMath.max(this.poisonPower, food.getPoisonPower());
            ++this.foodUsed;
            if (usedItem.getAge() > (float)usedItem.getOffAgeMax()) {
                this.rotten = true;
            } else if (!this.rotten && usedItem.getOffAgeMax() < 1000000000) {
                if (usedItem.getAge() < (float)usedItem.getOffAge()) {
                    this.rottenness += 0.5f * usedItem.getAge() / (float)usedItem.getOffAge();
                } else {
                    this.stale = true;
                    this.rottenness += 0.5f + 0.5f * (usedItem.getAge() - (float)usedItem.getOffAge()) / (float)(usedItem.getOffAgeMax() - usedItem.getOffAge());
                }
            }
        }
        this.condition += (float)usedItem.getCondition() / (float)usedItem.getConditionMax();
        ++this.itemUsed;
    }

    protected void transferToResults(ArrayList<InventoryItem> resultItems, ArrayList<InventoryItem> usedItems) {
        this.rottenness /= (float)this.foodUsed;
        for (int i = 0; i < resultItems.size(); ++i) {
            Food foodItem;
            InventoryItem resultItem = resultItems.get(i);
            if (resultItem instanceof Food && (foodItem = (Food)resultItem).isCookable()) {
                foodItem.setCooked(this.cooked);
                foodItem.setBurnt(this.burnt);
                foodItem.setPoisonDetectionLevel(this.poisonLevel);
                foodItem.setPoisonPower(this.poisonPower);
                if (this.tainted) {
                    foodItem.setTainted(true);
                }
            }
            if ((double)resultItem.getOffAgeMax() != 1.0E9) {
                if (this.rotten) {
                    resultItem.setAge(resultItem.getOffAgeMax());
                } else {
                    if (this.stale && this.rottenness < 0.5f) {
                        this.rottenness = 0.5f;
                    }
                    if (this.rottenness < 0.5f) {
                        resultItem.setAge(2.0f * this.rottenness * (float)resultItem.getOffAge());
                    } else {
                        resultItem.setAge((float)resultItem.getOffAge() + 2.0f * (this.rottenness - 0.5f) * (float)(resultItem.getOffAgeMax() - resultItem.getOffAge()));
                    }
                }
            }
            resultItem.setCondition(Math.round((float)resultItem.getConditionMax() * (this.condition / (float)this.itemUsed)));
            for (int j = 0; j < usedItems.size(); ++j) {
                InventoryItem usedItem = usedItems.get(j);
                resultItem.setConditionFromModData(usedItem);
                if (resultItem.getScriptItem() != usedItem.getScriptItem() || !usedItem.isFavorite()) continue;
                resultItem.setFavorite(true);
            }
        }
    }
}

