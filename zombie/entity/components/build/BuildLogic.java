/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.components.build;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.entity.ComponentType;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.crafting.recipe.CraftRecipeListNodeCollection;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoObject;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.CraftRecipeComponentScript;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.WallCoveringConfigScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.WallCoveringType;

@UsedFromLua
public class BuildLogic
extends BaseCraftingLogic {
    private CraftRecipe selectedRecipe = null;
    private final CraftRecipeData recipeDataInProgress;
    private boolean craftActionInProgress;
    private final Dictionary<CraftRecipe, CraftRecipeComponentScript> recipeComponentScriptLookup = new Hashtable<CraftRecipe, CraftRecipeComponentScript>();

    public BuildLogic(IsoGameCharacter player, CraftBench craftBench, IsoObject isoObject) {
        super(player, craftBench);
        this.isoObject = isoObject;
        this.recipeDataInProgress = new CraftRecipeData(CraftMode.Handcraft, craftBench != null, true, false, true);
        this.recipeDataInProgress.setCharacter(player);
        if (this.craftBench != null) {
            this.sourceResources.addAll(craftBench.getResources());
        }
        this.registerEvent("onRecipeChanged");
        this.registerEvent("onStartCraft");
        this.registerEvent("onStopCraft");
        this.setSortModeInternal(this.getRecipeSortMode());
        this.setManualSelectInputs(this.getLastManualInputMode());
    }

    public CraftRecipeListNodeCollection getRecipeList() {
        return this.filteredRecipeList;
    }

    @Override
    public CraftRecipe getRecipe() {
        return this.selectedRecipe;
    }

    public CraftRecipeData getRecipeData() {
        return this.recipeData;
    }

    public CraftRecipeData getRecipeDataInProgress() {
        return this.recipeDataInProgress;
    }

    public SpriteConfigManager.ObjectInfo getSelectedBuildObject() {
        SpriteConfigScript configScript;
        CraftRecipeComponentScript craftRecipeComponentScript;
        GameEntityScript gameEntityScript;
        if (this.selectedRecipe != null && (gameEntityScript = (GameEntityScript)(craftRecipeComponentScript = this.recipeComponentScriptLookup.get(this.selectedRecipe)).getParent()) != null && (configScript = (SpriteConfigScript)gameEntityScript.getComponentScriptFor(ComponentType.SpriteConfig)) != null) {
            return SpriteConfigManager.GetObjectInfo(configScript.getName());
        }
        return null;
    }

    public KahluaTable getWallCoveringParams() {
        CraftRecipeComponentScript craftRecipeComponentScript;
        GameEntityScript gameEntityScript;
        WallCoveringConfigScript wallCoveringConfigScript = null;
        if (this.selectedRecipe != null && (gameEntityScript = (GameEntityScript)(craftRecipeComponentScript = this.recipeComponentScriptLookup.get(this.selectedRecipe)).getParent()) != null) {
            wallCoveringConfigScript = (WallCoveringConfigScript)gameEntityScript.getComponentScriptFor(ComponentType.WallCoveringConfig);
        }
        if (wallCoveringConfigScript != null) {
            InventoryItem wallpaper;
            InventoryItem paint;
            KahluaTable table = LuaManager.platform.newTable();
            table.rawset("actionType", (Object)wallCoveringConfigScript.getType());
            if (wallCoveringConfigScript.getType() == WallCoveringType.PAINT_SIGN) {
                table.rawset("sign", (Object)wallCoveringConfigScript.getSignIndex().doubleValue());
            }
            if ((wallCoveringConfigScript.getType() == WallCoveringType.PAINT_SIGN || wallCoveringConfigScript.getType() == WallCoveringType.PAINT_THUMP) && (paint = this.recipeData.getFirstInputItemWithTag(ItemTag.PAINT)) != null) {
                table.rawset("paintType", (Object)paint.getType());
                Color paintColor = this.getPaintColor(paint.getType());
                if (paintColor != null) {
                    table.rawset("r", (Object)Float.valueOf(paintColor.getR()));
                    table.rawset("g", (Object)Float.valueOf(paintColor.getG()));
                    table.rawset("b", (Object)Float.valueOf(paintColor.getB()));
                }
            }
            if (wallCoveringConfigScript.getType() == WallCoveringType.WALLPAPER && (wallpaper = this.recipeData.getFirstInputItemWithTag(ItemTag.WALLPAPER)) != null) {
                table.rawset("wallpaperType", (Object)wallpaper.getType());
            }
            return table;
        }
        return null;
    }

    private Color getPaintColor(String paintType) {
        KahluaTable paintTable;
        Object object;
        Object object2 = LuaManager.env.rawget("ISPaintMenu");
        if (object2 instanceof KahluaTable && (object = (paintTable = (KahluaTable)object2).rawget("PaintMenuItems")) instanceof KahluaTable) {
            KahluaTable paintItems = (KahluaTable)object;
            KahluaTableIterator iterator2 = paintItems.iterator();
            while (iterator2.advance()) {
                KahluaTable color;
                Object object3;
                String type;
                KahluaTable value;
                Object object4 = iterator2.getValue();
                if (!(object4 instanceof KahluaTable) || !((object4 = (value = (KahluaTable)object4).rawget("paint")) instanceof String) || !(type = (String)object4).equalsIgnoreCase(paintType) || !((object4 = value.rawget("color")) instanceof KahluaTable) || !((object3 = (color = (KahluaTable)object4).rawget(1)) instanceof Double)) continue;
                Double r = (Double)object3;
                object3 = color.rawget(2);
                if (!(object3 instanceof Double)) continue;
                Double g = (Double)object3;
                object3 = color.rawget(2);
                if (!(object3 instanceof Double)) continue;
                Double b = (Double)object3;
                return new Color(r.floatValue(), g.floatValue(), b.floatValue());
            }
        }
        return null;
    }

    public ArrayList<CraftRecipe> getAllBuildableRecipes() {
        ArrayList<CraftRecipe> allBuildableRecipes = new ArrayList<CraftRecipe>();
        ArrayList<GameEntityScript> entityScripts = ScriptManager.instance.getAllGameEntities();
        block0: for (int i = 0; i < entityScripts.size(); ++i) {
            ArrayList<ComponentScript> allComponents = entityScripts.get(i).getComponentScripts();
            for (int j = 0; j < allComponents.size(); ++j) {
                CraftRecipe craftRecipe;
                if (allComponents.get((int)j).type != ComponentType.CraftRecipe) continue;
                CraftRecipeComponentScript componentScript = (CraftRecipeComponentScript)allComponents.get(j);
                CraftRecipe craftRecipe2 = craftRecipe = componentScript != null ? componentScript.getCraftRecipe() : null;
                if (craftRecipe == null || !craftRecipe.hasTag(CraftRecipeTag.ENTITY_RECIPE)) continue;
                allBuildableRecipes.add(craftRecipe);
                this.recipeComponentScriptLookup.put(craftRecipe, componentScript);
                continue block0;
            }
        }
        return allBuildableRecipes;
    }

    @Override
    public void setRecipe(CraftRecipe recipe) {
        if (this.selectedRecipe != recipe) {
            this.selectedRecipe = recipe;
            this.setLastSelectedRecipe(recipe);
            super.setRecipe(recipe);
        }
    }

    public boolean isCraftActionInProgress() {
        return this.craftActionInProgress;
    }

    @Override
    public boolean areAllInputItemsSatisfied() {
        if (this.selectedRecipe == null) {
            return false;
        }
        for (InputScript inputScript : this.selectedRecipe.getInputs()) {
            if (this.isInputSatisfied(inputScript)) continue;
            return false;
        }
        return true;
    }

    @Override
    public boolean isInputSatisfied(InputScript inputScript) {
        if (this.recipeData.getRecipe() == null || !this.recipeData.getRecipe().containsIO(inputScript)) {
            return false;
        }
        CraftRecipeData.InputScriptData data = this.recipeData.getDataForInputScript(inputScript);
        return data.isCachedCanConsume();
    }

    public void startCraftAction(KahluaTableImpl actionTable) {
        this.craftActionInProgress = true;
        this.recipeDataInProgress.setRecipe(this.recipeData.getRecipe());
        this.updateFloorContainer();
        if (this.isManualSelectInputs()) {
            ArrayList<InventoryItem> inputList = new ArrayList<InventoryItem>();
            for (InputScript input : this.recipeData.getRecipe().getInputs()) {
                this.recipeDataInProgress.setManualInputsFor(input, this.getMulticraftConsumedItemsFor(input, inputList));
                inputList.clear();
            }
        }
        this.triggerEvent("onStartCraft", actionTable);
    }

    public void updateFloorContainer() {
        if (this.updateFloorContainer(this.containers)) {
            this.cachedPossibleCraftCount = this.getPossibleCraftCount(true);
            this.cachedCanPerform = this.canPerformCurrentRecipe();
        }
    }

    public boolean performCurrentRecipe() {
        if (!this.isContainersAccessible(this.containers)) {
            return false;
        }
        this.updateFloorContainer();
        return this.recipeDataInProgress.perform(this.player, this.sourceResources, this.isManualSelectInputs() ? null : this.allItems, this.containers);
    }

    public void stopCraftAction() {
        this.craftActionInProgress = false;
        this.recipeDataInProgress.setRecipe(null);
        this.triggerEvent("onStopCraft", new Object[0]);
    }

    public ArrayList<InventoryItem> getAllConsumedItems() {
        if (this.recipeData != null) {
            return this.recipeData.getAllConsumedItems();
        }
        return null;
    }

    public void setSelectedRecipeStyle(String style) {
        this.setSelectedRecipeStyle("build", style);
    }

    public String getSelectedRecipeStyle() {
        return this.getSelectedRecipeStyle("build");
    }

    public void setRecipeSortMode(String sortMode) {
        this.setRecipeSortMode("build", sortMode);
    }

    public String getRecipeSortMode() {
        return this.getRecipeSortMode("build");
    }

    public void setLastSelectedRecipe(CraftRecipe recipe) {
        this.setLastSelectedRecipe("build", recipe);
    }

    public CraftRecipe getLastSelectedRecipe() {
        return this.getLastSelectedRecipe("build");
    }

    public void setLastManualInputMode(boolean b) {
        this.setLastManualInputMode("build", b);
    }

    protected boolean getLastManualInputMode() {
        return this.getLastManualInputMode("build");
    }
}

