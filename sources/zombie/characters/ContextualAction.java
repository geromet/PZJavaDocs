/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import java.util.List;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.popman.ObjectPool;
import zombie.util.lambda.Invokers;

public final class ContextualAction {
    static final ObjectPool<ContextualAction> pool = new ObjectPool<ContextualAction>(ContextualAction::new);
    public Action action = Action.NONE;
    public IsoDirections dir;
    public IsoGridSquare square;
    public IsoObject object;
    public ItemContainer targetContainer;
    public InventoryItem inventoryItem;
    public int priority;
    public boolean behind;

    public ContextualAction reset() {
        this.action = Action.NONE;
        this.dir = null;
        this.square = null;
        this.object = null;
        this.targetContainer = null;
        this.inventoryItem = null;
        this.priority = 0;
        this.behind = false;
        return this;
    }

    public static ContextualAction alloc() {
        return pool.alloc().reset();
    }

    public static ContextualAction alloc(Action action) {
        ContextualAction newAction = ContextualAction.alloc();
        newAction.action = action;
        newAction.priority = action.priority;
        return newAction;
    }

    public static ContextualAction alloc(Action action, IsoDirections dir, IsoGridSquare square, IsoObject object) {
        ContextualAction newAction = ContextualAction.alloc(action);
        newAction.dir = dir;
        newAction.square = square;
        newAction.object = object;
        return newAction;
    }

    public static ContextualAction alloc(Action action, Invokers.Params1.ICallback<ContextualAction> populator) {
        ContextualAction newAction = ContextualAction.alloc(action);
        populator.accept(newAction);
        return newAction;
    }

    public void release() {
        this.reset();
        pool.release(this);
    }

    public static void releaseAll(ArrayList<ContextualAction> actions) {
        pool.releaseAll((List<ContextualAction>)actions);
        actions.clear();
    }

    public static enum Action {
        NONE(0),
        AnimalInteraction(90),
        ClimbOverFence(100),
        ClimbOverWall(100),
        ClimbSheetRope(100),
        ClimbThroughWindow(100),
        OpenButcherHook(90),
        OpenHutch(90),
        RestOnFurniture(90),
        ThrowGrappledTargetOutWindow(100),
        ThrowGrappledOverFence(100),
        ThrowGrappledIntoContainer(100),
        ToggleCurtain(100),
        ToggleDoor(100),
        ToggleWindow(100);

        public final int priority;

        private Action(int priority) {
            this.priority = priority;
        }
    }
}

