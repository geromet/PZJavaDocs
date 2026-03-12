/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;

public interface ILuaGameCharacterHealth {
    public void setSleepingTabletEffect(float var1);

    public float getSleepingTabletEffect();

    public float getFatigueMod();

    public boolean Eat(InventoryItem var1, float var2, boolean var3);

    public boolean Eat(InventoryItem var1, float var2);

    public boolean Eat(InventoryItem var1);

    public boolean DrinkFluid(InventoryItem var1, float var2, boolean var3);

    public boolean DrinkFluid(InventoryItem var1, float var2);

    public boolean DrinkFluid(InventoryItem var1);

    public boolean DrinkFluid(FluidContainer var1, float var2, boolean var3);

    public boolean DrinkFluid(FluidContainer var1, float var2);

    public float getReduceInfectionPower();

    public void setReduceInfectionPower(float var1);

    public int getLastHourSleeped();

    public void setLastHourSleeped(int var1);

    public void setTimeOfSleep(float var1);
}

