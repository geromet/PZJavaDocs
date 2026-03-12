/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoGameCharacter;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeManager;
import zombie.entity.components.crafting.recipe.InputItemNode;
import zombie.inventory.InventoryItem;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.util.list.PZArrayUtil;

public class InputItemNodeCollection {
    private final ArrayList<InputItemNode> inputItemNodes = new ArrayList();
    private final ArrayList<InputItemNode> oldInputItemNodes = new ArrayList();
    private final HashMap<Item, HashMap<InputScript, InputItemNode>> oldInputItemNodeMap = new HashMap();
    private boolean inputItemNodesDirty;
    private final boolean filterItemsByRecipe;
    private final boolean startExpanded;
    private IsoGameCharacter character;
    private CraftRecipe recipe;
    private final ArrayList<InventoryItem> allItems = new ArrayList();

    public InputItemNodeCollection(boolean filterItemsByRecipe, boolean startExpanded) {
        this.filterItemsByRecipe = filterItemsByRecipe;
        this.startExpanded = startExpanded;
    }

    public void setCharacter(IsoGameCharacter character) {
        this.character = character;
    }

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public void setRecipe(CraftRecipe craftRecipe) {
        this.recipe = craftRecipe;
        this.setDirty();
    }

    public void setItems(ArrayList<InventoryItem> items) {
        this.allItems.clear();
        PZArrayUtil.addAll(this.allItems, items);
        this.setDirty();
    }

    public ArrayList<InputItemNode> getInputItemNodes() {
        if (this.inputItemNodesDirty) {
            this.rebuildInputItemNodes(this.allItems, this.recipe);
        }
        return this.inputItemNodes;
    }

    public ArrayList<InputItemNode> getInputItemNodesForInput(InputScript input) {
        ArrayList<InputItemNode> outputs = new ArrayList<InputItemNode>();
        ArrayList<InputItemNode> inputs = this.getInputItemNodes();
        for (int i = 0; i < inputs.size(); ++i) {
            if (inputs.get(i).getFirstMatchedInputScript() != input) continue;
            outputs.add(this.getInputItemNodes().get(i));
        }
        return outputs;
    }

    public boolean isDirty() {
        return this.inputItemNodesDirty;
    }

    public void setDirty() {
        this.inputItemNodesDirty = true;
    }

    private void rebuildInputItemNodes(ArrayList<InventoryItem> allItems, CraftRecipe recipe) {
        int i;
        if (!this.inputItemNodesDirty) {
            return;
        }
        this.oldInputItemNodes.clear();
        this.oldInputItemNodeMap.clear();
        if (!this.inputItemNodes.isEmpty()) {
            for (i = 0; i < this.inputItemNodes.size(); ++i) {
                InputItemNode node = this.inputItemNodes.get(i);
                this.oldInputItemNodes.add(node);
                if (this.oldInputItemNodeMap.get(node.scriptItem) == null) {
                    this.oldInputItemNodeMap.put(node.scriptItem, new HashMap());
                }
                this.oldInputItemNodeMap.get(node.scriptItem).put(node.firstMatchedInputScript, node);
            }
        }
        this.inputItemNodes.clear();
        if (!allItems.isEmpty() && recipe != null) {
            for (int i2 = 0; i2 < allItems.size(); ++i2) {
                InventoryItem inventoryItem = allItems.get(i2);
                ArrayList<InputScript> inputScripts = CraftRecipeManager.getAllValidInputScriptsForItem(recipe, inventoryItem, this.getCharacter());
                if (inputScripts.isEmpty()) {
                    if (this.filterItemsByRecipe) continue;
                    this.rebuildInputItemNode(null, inventoryItem);
                    continue;
                }
                for (int l = 0; l < inputScripts.size(); ++l) {
                    this.rebuildInputItemNode(inputScripts.get(l), inventoryItem);
                }
            }
            this.inputItemNodes.sort(InputItemNode.inputItemNodeComparator);
        }
        for (i = 0; i < this.oldInputItemNodes.size(); ++i) {
            InputItemNode.Release(this.oldInputItemNodes.get(i));
        }
        this.inputItemNodesDirty = false;
    }

    private void rebuildInputItemNode(InputScript inputScript, InventoryItem inventoryItem) {
        boolean nodeFound = false;
        for (int k = 0; k < this.inputItemNodes.size(); ++k) {
            InputItemNode inputItemNode = this.inputItemNodes.get(k);
            if (inputItemNode.firstMatchedInputScript != inputScript || inputItemNode.scriptItem != inventoryItem.getScriptItem()) continue;
            inputItemNode.items.add(inventoryItem);
            nodeFound = true;
        }
        if (!nodeFound) {
            InputItemNode inputItemNode = InputItemNode.Alloc(this.recipe, inventoryItem.getScriptItem());
            inputItemNode.firstMatchedInputScript = inputScript;
            inputItemNode.isToolRight = inputScript != null ? inputScript.hasFlag(InputFlag.ToolRight) : false;
            inputItemNode.isToolLeft = inputScript != null ? inputScript.hasFlag(InputFlag.ToolLeft) : false;
            inputItemNode.isTool = inputScript != null ? inputScript.isTool() : false;
            inputItemNode.isKeep = inputScript != null ? inputScript.isKeep() : false;
            inputItemNode.isItemCount = inputScript != null ? inputScript.isItemCount() : false;
            inputItemNode.items.add(inventoryItem);
            if (this.startExpanded) {
                inputItemNode.expandedAvailable = true;
                inputItemNode.expandedUsed = true;
            }
            if (this.oldInputItemNodeMap.containsKey(inputItemNode.scriptItem) && this.oldInputItemNodeMap.get(inputItemNode.scriptItem).containsKey(inputItemNode.firstMatchedInputScript)) {
                inputItemNode.expandedAvailable = this.oldInputItemNodeMap.get((Object)inputItemNode.scriptItem).get((Object)inputItemNode.firstMatchedInputScript).expandedAvailable;
                inputItemNode.expandedUsed = this.oldInputItemNodeMap.get((Object)inputItemNode.scriptItem).get((Object)inputItemNode.firstMatchedInputScript).expandedUsed;
            }
            this.inputItemNodes.add(inputItemNode);
        }
    }
}

