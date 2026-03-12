/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSDrink
extends RBTableStoryBase {
    public RBTSDrink() {
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.539f, 0.742f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        if (Rand.Next(70) < 100) {
            this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.734f, 0.797f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.554f, 0.57f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.695f, 0.336f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.875f, 0.687f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem(this.getDrink(), this.table1.getSquare(), 0.476f, 0.273f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        this.addWorldItem("Base.PlasticCup", this.table1.getSquare(), 0.843f, 0.531f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        if (Rand.NextBool(3)) {
            String crisp = "Base.Crisps";
            int rand = Rand.Next(0, 4);
            switch (rand) {
                case 0: {
                    crisp = "Base.Crisps2";
                    break;
                }
                case 1: {
                    crisp = "Base.Crisps3";
                    break;
                }
                case 2: {
                    crisp = "Base.Crisps4";
                }
            }
            this.addWorldItem(crisp, this.table1.getSquare(), 0.87f, 0.86f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem("Base.CigaretteSingle", this.table1.getSquare(), 0.406f, 0.843f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
        if (Rand.Next(70) < 100) {
            this.addWorldItem("Base.CigaretteSingle", this.table1.getSquare(), 0.578f, 0.953f, this.table1.getSurfaceOffsetNoTable() / 96.0f, true);
        }
    }

    public String getDrink() {
        return "Base.PlasticCup";
    }
}

