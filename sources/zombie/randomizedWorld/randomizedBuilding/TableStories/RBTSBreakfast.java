/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSBreakfast
extends RBTableStoryBase {
    public RBTSBreakfast() {
        this.need2Tables = true;
        this.ignoreAgainstWall = true;
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        if (this.table2 != null) {
            if (this.westTable) {
                String bowlOrMug = this.getBowlOrMug();
                RBTSBreakfast.trySpawnStoryItem(bowlOrMug, this.table1.getSquare(), 0.6875f, 0.7437f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                if (Rand.NextBool(3)) {
                    this.addWorldItem("Base.Spoon", this.table1.getSquare(), 0.3359f, 0.8765f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(260, 275));
                }
                bowlOrMug = this.getBowlOrMug();
                this.addWorldItem(bowlOrMug, this.table1.getSquare(), 0.6719f, 0.4531f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                if (Rand.NextBool(3)) {
                    this.addWorldItem("Base.Spoon", this.table1.getSquare(), 0.375f, 0.2656f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
                }
                if (Rand.Next(100) < 70) {
                    bowlOrMug = this.getBowlOrMug();
                    this.addWorldItem(bowlOrMug, this.table2.getSquare(), 0.6484f, 0.7353f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                    if (Rand.NextBool(3)) {
                        this.addWorldItem("Base.Spoon", this.table2.getSquare(), 0.8468f, 0.7906f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(260, 275));
                    }
                }
                if (Rand.Next(100) < 50) {
                    bowlOrMug = this.getBowlOrMug();
                    this.addWorldItem(bowlOrMug, this.table2.getSquare(), 0.5859f, 0.3941f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                    if (Rand.NextBool(3)) {
                        this.addWorldItem("Base.Spoon", this.table2.getSquare(), 0.7965f, 0.2343f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
                    }
                }
                this.addWorldItem("Base.Cereal", this.table2.getSquare(), 0.3281f, 0.6406f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                this.addWorldItem("Base.Milk", this.table1.getSquare(), 0.96f, 0.694f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                if (Rand.NextBool(0)) {
                    int fruitType = Rand.Next(0, 4);
                    switch (fruitType) {
                        case 0: {
                            this.addWorldItem("Base.Orange", this.table2.getSquare(), 0.6328f, 0.6484f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 1: {
                            this.addWorldItem("Base.Banana", this.table2.getSquare(), 0.6328f, 0.6484f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 2: {
                            this.addWorldItem("Base.Apple", this.table2.getSquare(), 0.6328f, 0.6484f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 3: {
                            this.addWorldItem("Base.Coffee2", this.table2.getSquare(), 0.6328f, 0.6484f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                        }
                    }
                }
            } else {
                String bowlOrMug = this.getBowlOrMug();
                this.addWorldItem(bowlOrMug, this.table1.getSquare(), 0.906f, 0.718f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                if (Rand.NextBool(3)) {
                    this.addWorldItem("Base.Spoon", this.table1.getSquare(), 0.945f, 0.336f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(165, 185));
                }
                bowlOrMug = this.getBowlOrMug();
                this.addWorldItem(bowlOrMug, this.table1.getSquare(), 0.406f, 0.562f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                if (Rand.NextBool(3)) {
                    this.addWorldItem("Base.Spoon", this.table1.getSquare(), 0.265f, 0.299f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 15));
                }
                if (Rand.Next(100) < 70) {
                    bowlOrMug = this.getBowlOrMug();
                    this.addWorldItem(bowlOrMug, this.table2.getSquare(), 0.929f, 0.726f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                    if (Rand.NextBool(3)) {
                        this.addWorldItem("Base.Spoon", this.table2.getSquare(), 0.976f, 0.46f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(165, 185));
                    }
                }
                if (Rand.Next(100) < 50) {
                    bowlOrMug = this.getBowlOrMug();
                    this.addWorldItem(bowlOrMug, this.table2.getSquare(), 0.382f, 0.78f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
                    if (Rand.NextBool(3)) {
                        this.addWorldItem("Base.Spoon", this.table2.getSquare(), 0.273f, 0.82f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 15));
                    }
                }
                this.addWorldItem("Base.Cereal", this.table2.getSquare(), 0.7f, 0.273f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                this.addWorldItem("Base.Milk", this.table2.getSquare(), 0.648f, 0.539f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                if (Rand.NextBool(0)) {
                    int fruitType = Rand.Next(0, 4);
                    switch (fruitType) {
                        case 0: {
                            this.addWorldItem("Base.Orange", this.table1.getSquare(), 0.703f, 0.765f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 1: {
                            this.addWorldItem("Base.Banana", this.table1.getSquare(), 0.703f, 0.765f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 2: {
                            this.addWorldItem("Base.Apple", this.table1.getSquare(), 0.703f, 0.765f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                            break;
                        }
                        case 3: {
                            this.addWorldItem("Base.Coffee2", this.table1.getSquare(), 0.703f, 0.765f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(0, 360));
                        }
                    }
                }
            }
        }
    }

    private String getBowlOrMug() {
        boolean bowl = Rand.NextBool(2);
        if (bowl) {
            return Rand.NextBool(4) ? "Base.Bowl" : "Base.CerealBowl";
        }
        return "Base.Bowl";
    }
}

