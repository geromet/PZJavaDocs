/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.ui.ISUIWrapper.ISPanelWrapper;
import zombie.ui.ISUIWrapper.ISUIElementWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public class ISContextMenuWrapper
extends ISPanelWrapper {
    public ISContextMenuWrapper(KahluaTable table) {
        super(table);
    }

    public ISContextMenuWrapper(double x, double y, double width, double height, double zoom) {
        super(x, y, width, height);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISContextMenu");
        this.table.setMetatable(self);
        self.rawset("__index", (Object)self);
        UIFont font = UIFont.Medium;
        double fontHgt = TextManager.instance.getFontFromEnum(font).getLineHeight();
        double padY = LuaHelpers.castDouble(this.table.rawget("padY"));
        this.table.rawset("x", (Object)x);
        this.table.rawset("y", (Object)y);
        this.table.rawset("zoom", (Object)zoom);
        this.table.rawset("font", (Object)font);
        this.table.rawset("padY", (Object)6.0);
        this.table.rawset("fontHgt", (Object)fontHgt);
        this.table.rawset("itemHgt", (Object)(fontHgt + padY * 2.0));
        this.table.rawset("padTopBottom", (Object)0.0);
        this.table.rawset("borderColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 1.0, 1.0, 1.0, 0.15));
        this.table.rawset("backgroundColor", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.1, 0.1, 0.1, 0.7));
        this.table.rawset("backgroundColorMouseOver", (Object)this.setRGBA(LuaManager.platform.newTable(), 0.3, 0.3, 0.3, 1.0));
        this.table.rawset("width", (Object)width);
        this.table.rawset("height", (Object)height);
        this.table.rawset("anchorLeft", (Object)true);
        this.table.rawset("anchorRight", (Object)false);
        this.table.rawset("anchorTop", (Object)true);
        this.table.rawset("anchorBottom", (Object)false);
        this.table.rawset("parent", (Object)LuaManager.platform.newTable());
        this.table.rawset("keepOnScreen", (Object)true);
        this.table.rawset("options", (Object)LuaManager.platform.newTable());
        this.table.rawset("numOptions", (Object)1.0);
        this.table.rawset("optionPool", (Object)LuaManager.platform.newTable());
        this.table.rawset("visibleCheck", (Object)false);
        this.table.rawset("forceVisible", (Object)true);
        this.table.rawset("toolTip", null);
        this.table.rawset("subOptionNums", (Object)0.0);
        this.table.rawset("player", (Object)0.0);
        this.table.rawset("scrollIndicatorHgt", (Object)14.0);
        this.table.rawset("arrowUp", (Object)Texture.getSharedTexture("media/ui/ArrowUp.png"));
        this.table.rawset("arrowDown", (Object)Texture.getSharedTexture("media/ui/ArrowDown.png"));
        this.table.rawset("tickTexture", (Object)Texture.getSharedTexture("media/ui/inventoryPanes/Tickbox_Tick.png"));
    }

    private void clear() {
        KahluaTable options = this.getOptions();
        KahluaTable optionPool = this.getOptionPool();
        KahluaTableIterator iterator2 = options.iterator();
        while (iterator2.advance()) {
            Object option = iterator2.getValue();
            optionPool.rawset(optionPool.size() + 1, option);
        }
        options.wipe();
        this.table.rawset("numOptions", (Object)1.0);
        this.table.rawset("mouseOver", (Object)-1.0);
        this.table.rawset("subMenu", null);
        this.setHeight(0.0);
        this.table.rawset("addedDefaultOptions", (Object)false);
    }

    private KahluaTable getOptions() {
        return (KahluaTable)this.table.rawget("options");
    }

    private KahluaTable getOptionPool() {
        return (KahluaTable)this.table.rawget("optionPool");
    }

    public Double getNumOptions() {
        return (Double)this.table.rawget("numOptions");
    }

    private void setNumOptions(Double value) {
        this.table.rawset("numOptions", (Object)value);
    }

    private KahluaTable allocOption(String name, Object target, Object onSelect, Object ... params) {
        KahluaTable option;
        KahluaTable optionPool = this.getOptionPool();
        if (optionPool.isEmpty()) {
            option = LuaManager.platform.newTable();
        } else {
            option = (KahluaTable)optionPool.rawget(optionPool.size());
            optionPool.rawset(optionPool.size(), (Object)null);
        }
        option.wipe();
        option.rawset("id", (Object)this.getNumOptions());
        option.rawset("name", (Object)name);
        option.rawset("onSelect", onSelect);
        option.rawset("target", target);
        for (int i = 0; i < params.length; ++i) {
            String key = String.format("param%s", i + 1);
            option.rawset(key, params[i]);
        }
        option.rawset("subOption", null);
        return option;
    }

    public void addSubMenu(KahluaTable option, KahluaTable menu) {
        option.rawset("subOption", menu.rawget("subOptionNums"));
    }

    public KahluaTable addOption(String name, Object target, Object onSelect, Object ... params) {
        if (Core.getInstance().getGameMode().equals("Tutorial") && this.getOptionFromName(name) != null) {
            return null;
        }
        KahluaTable option = this.allocOption(name, target, onSelect, params);
        option.rawset("iconTexture", null);
        option.rawset("color", null);
        this.getOptions().rawset(this.getNumOptions(), (Object)option);
        this.setNumOptions(this.getNumOptions() + 1.0);
        this.calcHeight();
        this.setWidth(this.calcWidth());
        return option;
    }

    public KahluaTable addDebugOption(String name, Object target, Object onSelect, Object ... params) {
        if (DebugOptions.instance.getBoolean("UI.HideDebugContextMenuOptions")) {
            return null;
        }
        KahluaTable option = this.addOption(name, target, onSelect, params);
        option.rawset("iconTexture", (Object)Texture.getSharedTexture("media/textures/Item_Plumpabug_Left.png"));
        option.rawset("color", null);
        return option;
    }

    public KahluaTable addGetUpOption(String name, Object target, Object onSelect, Object ... params) {
        if (params.length > 9) {
            DebugType.General.error("ISContextMenuLogic:addGetUpOption - only 9 additional arguments are supported");
        }
        Object[] combinedParams = new Object[params.length + 2];
        combinedParams[0] = onSelect;
        combinedParams[1] = target;
        for (int i = 0; i < params.length; ++i) {
            combinedParams[i + 2] = params[i];
        }
        return this.addOption(name, this.table, this.table.rawget("onGetUpAndThen"), combinedParams);
    }

    public KahluaTable getOptionFromName(String name) {
        KahluaTableIterator iterator2 = this.getOptions().iterator();
        while (iterator2.advance()) {
            KahluaTableImpl option = (KahluaTableImpl)iterator2.getValue();
            if (!name.equals(option.rawgetStr("name"))) continue;
            return option;
        }
        return null;
    }

    public void removeLastOption() {
        KahluaTable optionPool = this.getOptionPool();
        KahluaTable options = this.getOptions();
        KahluaTable lastOption = (KahluaTable)options.rawget(options.size());
        optionPool.rawset(optionPool.size() + 1, (Object)lastOption);
        options.rawset(options.size(), (Object)null);
        this.setNumOptions(this.getNumOptions() - 1.0);
        Object requestX = this.table.rawget("requestX");
        Object requestY = this.table.rawget("requestY");
        if (requestX != null && requestY != null) {
            double requestXVal = LuaHelpers.castDouble(requestX);
            double requestYVal = LuaHelpers.castDouble(requestY);
            this.setSlideGoalX(requestXVal + 20.0, requestXVal);
            this.setSlideGoalY(requestYVal - 10.0, requestYVal);
        }
        this.calcHeight();
        this.setWidth(this.calcWidth());
    }

    private void calcHeight() {
        double numOptions = LuaHelpers.castDouble(this.table.rawget("numOptions"));
        double itemHgt = LuaHelpers.castDouble(this.table.rawget("itemHgt"));
        double itemsHgt = (numOptions - 1.0) * itemHgt;
        double screenHgt = Core.getInstance().getScreenHeight();
        double padTopBottom = LuaHelpers.castDouble(this.table.rawget("padTopBottom"));
        if (itemsHgt + padTopBottom * 2.0 > screenHgt) {
            double scrollIndicatorHgt = LuaHelpers.castDouble(this.table.rawget("scrollIndicatorHgt"));
            double numVisibleItems = Math.floor((screenHgt - padTopBottom * 2.0 - scrollIndicatorHgt * 2.0) / itemHgt);
            double scrollAreaHeight = numVisibleItems * itemHgt;
            this.table.rawset("scrollAreaHeight", (Object)scrollAreaHeight);
            this.setHeight(scrollAreaHeight + padTopBottom * 2.0 + scrollIndicatorHgt * 2.0);
            this.setScrollHeight(itemsHgt);
        } else {
            this.table.rawset("scrollAreaHeight", (Object)itemsHgt);
            this.setHeight(itemsHgt + padTopBottom * 2.0);
            this.setScrollHeight(itemsHgt);
        }
    }

    private double calcWidth() {
        double maxWidth = 0.0;
        UIFont font = (UIFont)((Object)this.table.rawget("font"));
        KahluaTable options = (KahluaTable)this.table.rawget("options");
        KahluaTableIterator iterator2 = options.iterator();
        while (iterator2.advance()) {
            KahluaTable k = (KahluaTable)iterator2.getValue();
            String name = LuaHelpers.castString(k.rawget("name"));
            double w = TextManager.instance.MeasureStringX(font, name);
            if (!(w > maxWidth)) continue;
            maxWidth = w;
        }
        double itemHgt = LuaHelpers.castDouble(this.table.rawget("itemHgt"));
        double iconSize = itemHgt - 12.0;
        double iconShiftX = 2.0;
        double textForIconShift = iconSize + 4.0 + 2.0;
        return Math.max(textForIconShift + maxWidth + 24.0 + (double)TextManager.instance.MeasureStringX(font, ">") + 4.0, 100.0);
    }

    private void setSlideGoalX(double startX, double finalX) {
        this.setX(finalX);
        this.table.rawset("slideGoalX", null);
        if (!this.isOptionSingleMenu()) {
            return;
        }
        if (LuaHelpers.getJoypadState(LuaHelpers.castDouble(this.table.rawget("player")).intValue()) == null) {
            return;
        }
        this.setX(startX);
        this.table.rawset("slideGoalX", (Object)finalX);
        this.table.rawset("slideGoalTime", (Object)System.currentTimeMillis());
    }

    private void setSlideGoalY(double startY, double finalY) {
        this.setY(finalY);
        this.table.rawset("slideGoalY", null);
        if (!this.isOptionSingleMenu()) {
            return;
        }
        if (LuaHelpers.getJoypadState(LuaHelpers.castDouble(this.table.rawget("player")).intValue()) == null) {
            return;
        }
        this.setY(startY);
        this.table.rawset("slideGoalY", (Object)finalY);
        this.table.rawset("slideGoalDY", (Object)(finalY - startY));
        this.table.rawset("slideGoalTime", (Object)System.currentTimeMillis());
    }

    private boolean isOptionSingleMenu() {
        return Core.getInstance().getOptionSingleContextMenu(LuaHelpers.castDouble(this.table.rawget("player")).intValue());
    }

    private void setFontFromOption() {
        String font = Core.getInstance().getOptionContextMenuFont();
        if (font.equals("Large")) {
            this.setFont(UIFont.Large);
        } else if (font.equals("Small")) {
            this.setFont(UIFont.Small);
        } else {
            this.setFont(UIFont.Medium);
        }
    }

    private void setFont(UIFont font) {
        double fontHgt = TextManager.instance.getFontHeight(font);
        double padY = LuaHelpers.castDouble(this.table.rawget("padY"));
        this.table.rawset("font", (Object)font);
        this.table.rawset("fontHgt", (Object)fontHgt);
        this.table.rawset("itemHgt", (Object)(fontHgt + padY * 2.0));
    }

    public static ISContextMenuWrapper getNew(ISUIElementWrapper parentContext) {
        ISContextMenuWrapper subInstance;
        double player = LuaHelpers.castDouble(parentContext.getTable().rawget("player"));
        KahluaTable context = LuaHelpers.getPlayerContextMenu(player);
        ISContextMenuWrapper contextWrapper = new ISContextMenuWrapper(context);
        KahluaTable subMenuPool = (KahluaTable)context.rawget("subMenuPool");
        if (subMenuPool.isEmpty()) {
            subInstance = new ISContextMenuWrapper(0.0, 0.0, 1.0, 1.0, 1.5);
        } else {
            subInstance = new ISContextMenuWrapper((KahluaTable)subMenuPool.rawget(subMenuPool.size()));
            subMenuPool.rawset(subMenuPool.size(), (Object)null);
        }
        context.rawset("subInstance", (Object)subInstance.getTable());
        subInstance.initialise();
        subInstance.instantiate();
        subInstance.addToUIManager();
        subInstance.clear();
        subInstance.setFontFromOption();
        subInstance.setX(parentContext.getX());
        subInstance.setY(parentContext.getY());
        subInstance.getTable().rawset("parent", (Object)parentContext.getTable());
        subInstance.getTable().rawset("forceVisible", (Object)true);
        subInstance.setVisible(false);
        subInstance.bringToTop();
        subInstance.getTable().rawset("player", (Object)player);
        contextWrapper.setForceCursorVisible(player == 0.0);
        double subOptionNums = LuaHelpers.castDouble(context.rawget("subOptionNums")) + 1.0;
        context.rawset("subOptionNums", (Object)subOptionNums);
        subInstance.getTable().rawset("subOptionNums", (Object)subOptionNums);
        KahluaTable instanceMap = (KahluaTable)context.rawget("instanceMap");
        instanceMap.rawset(subOptionNums, (Object)subInstance.getTable());
        return subInstance;
    }

    public KahluaTable addActionsOption(String text, Object getActionsFunction, Object ... args2) {
        Double player = (Double)this.table.rawget("player");
        IsoPlayer character = LuaManager.GlobalObject.getSpecificPlayer(player.intValue());
        return this.addOption(text, character, LuaManager.getFunctionObject("ISTimedActionQueue.queueActions"), getActionsFunction, args2);
    }

    public KahluaTable getContextFromOption(String optionName) {
        if (this.getOptionFromName(optionName) == null) {
            return null;
        }
        return (KahluaTable)((KahluaTable)this.table.rawget("instanceMap")).rawget(this.getOptionFromName(optionName).rawget("subOption"));
    }
}

