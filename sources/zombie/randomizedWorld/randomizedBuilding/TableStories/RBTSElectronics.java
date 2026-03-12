/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSElectronics
extends RBTableStoryBase {
    public RBTSElectronics() {
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("bedroom");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        String item = "Base.ElectronicsMag1";
        int rand = Rand.Next(4);
        switch (rand) {
            case 0: {
                item = "Base.ElectronicsMag2";
                break;
            }
            case 1: {
                item = "Base.ElectronicsMag3";
                break;
            }
            case 2: {
                item = "Base.ElectronicsMag5";
            }
        }
        this.addWorldItem(item, this.table1.getSquare(), 0.36f, 0.789f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.ElectronicsScrap", this.table1.getSquare(), 0.71f, 0.82f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        this.addWorldItem("Base.Screwdriver", this.table1.getSquare(), 0.36f, 0.421f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
        item = "Base.CDPlayer";
        rand = Rand.Next(8);
        switch (rand) {
            case 0: {
                item = "Base.Torch";
                break;
            }
            case 1: {
                item = "Base.Remote";
                break;
            }
            case 2: {
                item = "Base.VideoGame";
                break;
            }
            case 3: {
                item = "Base.CordlessPhone";
                break;
            }
            case 4: {
                item = "Base.Headphones";
                break;
            }
            case 5: {
                item = "Base.HairDryer";
                break;
            }
            case 6: {
                item = "Base.HomeAlarm";
                break;
            }
            case 7: {
                item = "Base.CDPlayer";
            }
        }
        this.addWorldItem(item, this.table1.getSquare(), 0.695f, 0.43f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
    }
}

