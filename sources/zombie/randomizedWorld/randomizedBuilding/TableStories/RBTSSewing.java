/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTableStoryBase;

public final class RBTSSewing
extends RBTableStoryBase {
    public RBTSSewing() {
        this.rooms.add("livingroom");
        this.rooms.add("kitchen");
        this.rooms.add("bedroom");
        this.rooms.add("hall");
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        int sewingType = Rand.Next(0, 2);
        if (sewingType == 0) {
            this.addWorldItem(Rand.NextBool(2) ? "Base.Socks_Ankle" : "Base.Socks_Long", this.table1.getSquare(), 0.476f, 0.767f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            this.addWorldItem(Rand.NextBool(2) ? "Base.Socks_Ankle" : "Base.Socks_Long", this.table1.getSquare(), 0.656f, 0.775f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            if (Rand.NextBool(3)) {
                this.addWorldItem(Rand.NextBool(2) ? "Base.Socks_Ankle" : "Base.Socks_Long", this.table1.getSquare(), 0.437f, 0.469f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            }
            this.addWorldItem("Base.SewingKit", this.table1.getSquare(), 0.835f, 0.476f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
            if (Rand.NextBool(2)) {
                this.addWorldItem("Base.Scissors", this.table1.getSquare(), 0.945f, 0.586f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
            }
            if (Rand.NextBool(2)) {
                this.addWorldItem("Base.Thread", this.table1.getSquare(), 0.899f, 0.914f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
            }
            if (Rand.NextBool(2)) {
                this.addWorldItem("Base.Needle", this.table1.getSquare(), 0.945f, 0.586f, this.table1.getSurfaceOffsetNoTable() / 96.0f, Rand.Next(75, 95));
            }
        } else if (sewingType == 1) {
            String item = "Base.Jumper_DiamondPatternTINT";
            int rand = Rand.Next(0, 4);
            switch (rand) {
                case 0: {
                    item = "Base.Jumper_TankTopDiamondTINT";
                    break;
                }
                case 1: {
                    item = "Base.Jumper_PoloNeck";
                    break;
                }
                case 2: {
                    item = "Base.Jumper_VNeck";
                    break;
                }
                case 3: {
                    item = "Base.Jumper_RoundNeck";
                }
            }
            this.addWorldItem("Base.KnittingNeedles", this.table1.getSquare(), 0.531f, 0.625f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            this.addWorldItem(item, this.table1.getSquare(), 0.687f, 0.687f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            this.addWorldItem("Base.Yarn", this.table1.getSquare(), 0.633f, 0.96f, this.table1.getSurfaceOffsetNoTable() / 96.0f);
            this.addWorldItem("Base.RippedSheets", this.table1.getSquare(), 0.875f, 0.91f, this.table1.getSurfaceOffsetNoTable() / 96.0f, 1);
        }
    }
}

