/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSButcher
extends RBTableStoryBase {
    public RBTSButcher() {
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        String animal = "Base.DeadRabbit";
        String food = "Base.Rabbitmeat";
        int rand = Rand.Next(4);
        switch (rand) {
            case 0: {
                animal = "Base.DeadBird";
                food = "Base.Smallbirdmeat";
                break;
            }
            case 1: {
                animal = "Base.DeadSquirrel";
                food = "Base.Smallanimalmeat";
                break;
            }
            case 2: {
                animal = "Base.BaitFish";
                food = "Base.FishFillet";
            }
        }
        this.addWorldItem(animal, this.table1.getSquare(), 0.453f, 0.64f, this.table1.getSurfaceOffsetNoTable() / 96.0f, 1);
        this.addWorldItem(food, this.table1.getSquare(), 0.835f, 0.851f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.KitchenKnife", this.table1.getSquare(), 0.742f, 0.445f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
    }
}

