/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.Lua.LuaManager;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Literature;
import zombie.network.GameServer;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.Newspaper;

public class RecipeCodeHelper {
    private static final Map<String, Integer> ticketWinnings = Map.ofEntries(Map.entry("$1", 500), Map.entry("$2", 300), Map.entry("$5", 200), Map.entry("$10", 100), Map.entry("$20", 50), Map.entry("$50", 25), Map.entry("$100", 10), Map.entry("$500", 5), Map.entry("$1000", 2), Map.entry("$5000", 1), Map.entry("$10000", 1));

    public static void nameNewspaper(InventoryItem item, Newspaper newspaper) {
        String issue = Rand.Next(newspaper.getIssues());
        if (item.hasTag(ItemTag.NEWSPAPER_NEW)) {
            issue = newspaper.getIssues().get(newspaper.getIssues().size() - 1);
        }
        String title = newspaper.getTitle(issue);
        item.setName(Translator.getText(title));
        RecipeCodeHelper.setPrintMediaInfo(item, title, newspaper.getTranslationInfoKey(issue), newspaper.getTranslationTextKey(issue), newspaper.toString());
    }

    public static void setPrintMediaInfo(InventoryItem item, String title, String info, String text, String mediaId) {
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
        table.rawset("title", (Object)title);
        table.rawset("info", (Object)info);
        table.rawset("id", (Object)mediaId);
        table.rawset("text", (Object)text);
        item.getModData().rawset("printMedia", (Object)table);
        item.getModData().rawset("literatureTitle", (Object)title);
    }

    protected static void setColor(InventoryItem result, Color color) {
        result.setColorRed(color.getR());
        result.setColorGreen(color.getG());
        result.setColorBlue(color.getB());
        result.setColor(color);
        result.setCustomColor(true);
    }

    protected static <T extends InventoryItem> List<T> getInputItems(CraftRecipeData data, ItemTag itemTag) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem item : data.getAllInputItems()) {
            if (!item.hasTag(itemTag)) continue;
            result.add(item);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getConsumedItems(CraftRecipeData data, ItemTag itemTag) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (!item.hasTag(itemTag)) continue;
            result.add(item);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getConsumedItems(CraftRecipeData data, ItemKey ... itemKey) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem consumedItem : data.getAllConsumedItems()) {
            if (!consumedItem.is(itemKey)) continue;
            result.add(consumedItem);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getConsumedItems(CraftRecipeData data, Class<T> clazz) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem consumedItem : data.getAllConsumedItems()) {
            if (!clazz.isInstance(consumedItem)) continue;
            result.add(consumedItem);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getCreatedItems(CraftRecipeData data, Class<T> clazz) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem createdItem : data.getAllCreatedItems()) {
            if (!clazz.isInstance(createdItem)) continue;
            result.add(createdItem);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getCreatedItems(CraftRecipeData data, ItemTag tag) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem createdItem : data.getAllCreatedItems()) {
            if (!createdItem.hasTag(tag)) continue;
            result.add(createdItem);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getKeepItems(CraftRecipeData data, Class<T> clazz) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem item : data.getAllKeepInputItems()) {
            if (!clazz.isInstance(item)) continue;
            result.add(item);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getKeepItems(CraftRecipeData data, ItemTag ... tag) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem item : data.getAllKeepInputItems()) {
            if (!item.hasTag(tag)) continue;
            result.add(item);
        }
        return result;
    }

    protected static <T extends InventoryItem> List<T> getKeepItems(CraftRecipeData data, Fluid fluid) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (InventoryItem item : data.getAllKeepInputItems()) {
            if (item.getFluidContainer() == null || !item.getFluidContainer().contains(fluid)) continue;
            result.add(item);
        }
        return result;
    }

    protected static <T extends InventoryItem> T addItemToCharacterInventory(IsoGameCharacter character, String item) {
        return RecipeCodeHelper.addItemToCharacterInventory(character, InventoryItemFactory.CreateItem(item));
    }

    protected static <T extends InventoryItem> T addItemToCharacterInventory(IsoGameCharacter character, ItemKey itemKey) {
        return RecipeCodeHelper.addItemToCharacterInventory(character, InventoryItemFactory.CreateItem(itemKey));
    }

    protected static <T extends InventoryItem> T addItemToCharacterInventory(IsoGameCharacter character, T item) {
        character.getInventory().addItem(item);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), item);
        }
        return item;
    }

    protected static void removeItemFromCharacterInventory(IsoGameCharacter character, String item) {
        RecipeCodeHelper.addItemToCharacterInventory(character, InventoryItemFactory.CreateItem(item));
    }

    protected static void removeItemFromCharacterInventory(IsoGameCharacter character, ItemKey itemKey) {
        RecipeCodeHelper.addItemToCharacterInventory(character, InventoryItemFactory.CreateItem(itemKey));
    }

    protected static void removeItemFromCharacterInventory(IsoGameCharacter character, InventoryItem item) {
        character.getInventory().Remove(item);
        if (GameServer.server) {
            GameServer.sendRemoveItemFromContainer(character.getInventory(), item);
        }
    }

    protected static void scratchTicketWinner(IsoPlayer character, Literature result) {
        int total = ticketWinnings.values().stream().mapToInt(Integer::intValue).sum();
        int rand = Rand.Next(total);
        String winning = null;
        int currentWeight = 0;
        for (Map.Entry<String, Integer> check2 : ticketWinnings.entrySet()) {
            if (rand >= (currentWeight += check2.getValue().intValue())) continue;
            winning = check2.getKey();
            break;
        }
        if (character != null) {
            HaloTextHelper.addGoodText(character, winning);
        }
        result.setName(Translator.getText("IGUI_ScratchingTicketNameWinner", result.getDisplayName(), winning));
        result.setTexture(Texture.getSharedTexture("Item_ScratchTicket_Winner"));
        result.setWorldStaticModel(ModelKey.SCRATCH_TICKET_WINNER);
    }

    public record DateResult(int year, int month) {
    }
}

