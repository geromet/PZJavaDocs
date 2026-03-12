/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSSandwich
extends RBTableStoryBase {
    public RBTSSandwich() {
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.addWorldItem("Base.Bread", this.table1.getSquare(), 0.804f, 0.726f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.BreadSlices", this.table1.getSquare(), 0.546f, 0.703f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.BreadKnife", this.table1.getSquare(), 0.648f, 0.414f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 30));
        this.addWorldItem("Base.PeanutButter", this.table1.getSquare(), 0.453f, 0.484f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem(Rand.NextBool(2) ? "Base.JamFruit" : "Base.JamMarmalade", this.table1.getSquare(), 0.351f, 0.836f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
    }
}

