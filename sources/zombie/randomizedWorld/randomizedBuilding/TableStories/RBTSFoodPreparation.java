/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSFoodPreparation
extends RBTableStoryBase {
    public RBTSFoodPreparation() {
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.addWorldItem("Base.BakingTray", this.table1.getSquare(), 0.695f, 0.648f, this.table1.getSurfaceOffsetNoTable() / 96.0f, 1);
        String food = "Base.Chicken";
        int rand = Rand.Next(0, 4);
        switch (rand) {
            case 0: {
                food = "Base.Steak";
                break;
            }
            case 1: {
                food = "Base.MuttonChop";
                break;
            }
            case 2: {
                food = "Base.Smallbirdmeat";
            }
        }
        this.addWorldItem(food, this.table1.getSquare(), 0.531f, 0.625f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem(food, this.table1.getSquare(), 0.836f, 0.627f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem(Rand.NextBool(2) ? "Base.Pepper" : "Base.Salt", this.table1.getSquare(), 0.492f, 0.94f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.KitchenKnife", this.table1.getSquare(), 0.492f, 0.29f, this.table1.getSurfaceOffsetNoTable() / 96.0f, 1);
        food = "Base.Tomato";
        rand = Rand.Next(0, 4);
        switch (rand) {
            case 0: {
                food = "Base.BellPepper";
                break;
            }
            case 1: {
                food = "Base.Broccoli";
                break;
            }
            case 2: {
                food = "Base.Carrots";
            }
        }
        this.addWorldItem(food, this.table1.getSquare(), 0.77f, 0.97f, this.table1.getSurfaceOffsetNoTable() / 96.0f, 70);
    }
}

