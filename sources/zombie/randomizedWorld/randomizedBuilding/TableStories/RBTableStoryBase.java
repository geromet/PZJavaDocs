/*
 * Decompiled with CFR 0.152.
 */
package zombie.randomizedWorld.randomizedBuilding.TableStories;

import java.util.ArrayList;
import java.util.List;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSBreakfast;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSButcher;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSDinner;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSDrink;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSElectronics;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSFoodPreparation;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSSandwich;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSSewing;
import zombie.randomizedWorld.randomizedBuilding.TableStories.RBTSSoup;
import zombie.util.list.WeightedList;

public class RBTableStoryBase
extends RandomizedBuildingBase {
    public static final WeightedList<RBTableStoryBase> ALL_STORIES = new WeightedList();
    protected List<String> rooms = new ArrayList<String>();
    protected boolean need2Tables;
    protected boolean ignoreAgainstWall;
    protected IsoObject table2;
    protected IsoObject table1;
    protected boolean westTable;
    private static final ArrayList<IsoObject> tableObjects = new ArrayList();

    public static void initStories() {
        if (ALL_STORIES.isEmpty()) {
            ALL_STORIES.add(new RBTSBreakfast(), 10);
            ALL_STORIES.add(new RBTSDinner(), 10);
            ALL_STORIES.add(new RBTSSoup(), 10);
            ALL_STORIES.add(new RBTSSewing(), 5);
            ALL_STORIES.add(new RBTSElectronics(), 5);
            ALL_STORIES.add(new RBTSFoodPreparation(), 8);
            ALL_STORIES.add(new RBTSButcher(), 3);
            ALL_STORIES.add(new RBTSSandwich(), 10);
            ALL_STORIES.add(new RBTSDrink(), 7);
        }
    }

    public static RBTableStoryBase getRandomStory(IsoObject table) {
        if (GameServer.server) {
            RBTableStoryBase.initStories();
        }
        return ALL_STORIES.getRandom();
    }

    public void initTables(IsoObject table) {
        this.table1 = table;
        this.table2 = this.getSecondTable(table);
    }

    public boolean isValid(IsoGridSquare sq, boolean force) {
        if (force) {
            return true;
        }
        if (this.rooms != null && sq.getRoom() != null && !this.rooms.contains(sq.getRoom().getName())) {
            return false;
        }
        if (this.need2Tables && this.table2 == null) {
            return false;
        }
        return !this.ignoreAgainstWall || sq.getWallFull() == false;
    }

    public IsoObject getSecondTable(IsoObject table1) {
        this.westTable = true;
        IsoGridSquare sq = table1.getSquare();
        if (this.ignoreAgainstWall && sq.getWallFull().booleanValue()) {
            return null;
        }
        table1.getSpriteGridObjects(tableObjects);
        IsoGridSquare sq2 = sq.getAdjacentSquare(IsoDirections.W);
        IsoObject table2 = this.checkForTable(sq2, table1, tableObjects);
        if (table2 == null) {
            sq2 = sq.getAdjacentSquare(IsoDirections.E);
            table2 = this.checkForTable(sq2, table1, tableObjects);
        }
        if (table2 == null) {
            this.westTable = false;
        }
        if (table2 == null) {
            sq2 = sq.getAdjacentSquare(IsoDirections.N);
            table2 = this.checkForTable(sq2, table1, tableObjects);
        }
        if (table2 == null) {
            sq2 = sq.getAdjacentSquare(IsoDirections.S);
            table2 = this.checkForTable(sq2, table1, tableObjects);
        }
        if (table2 != null && this.ignoreAgainstWall && sq2.getWallFull().booleanValue()) {
            return null;
        }
        return table2;
    }

    private IsoObject checkForTable(IsoGridSquare sq, IsoObject table1, ArrayList<IsoObject> tableObjects) {
        if (sq == null) {
            return null;
        }
        if (sq.isSomethingTo(table1.getSquare())) {
            return null;
        }
        for (int o = 0; o < sq.getObjects().size(); ++o) {
            IsoObject obj = sq.getObjects().get(o);
            if (!tableObjects.isEmpty() && !tableObjects.contains(obj) || !obj.getProperties().isTable() || obj.getContainer() != null || obj == table1) continue;
            return obj;
        }
        return null;
    }
}

