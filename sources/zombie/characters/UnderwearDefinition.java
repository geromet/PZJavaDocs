/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

public class UnderwearDefinition {
    public static final UnderwearDefinition instance = new UnderwearDefinition();
    public boolean dirty = true;
    private static final ArrayList<OutfitUnderwearDefinition> m_outfitDefinition = new ArrayList();
    private static int baseChance = 50;

    public void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        m_outfitDefinition.clear();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("UnderwearDefinition");
        if (definitions == null) {
            return;
        }
        baseChance = definitions.rawgetInt("baseChance");
        KahluaTableIterator iterator2 = definitions.iterator();
        while (iterator2.advance()) {
            ArrayList<StringChance> allTops = null;
            Object object = iterator2.getValue();
            if (!(object instanceof KahluaTableImpl)) continue;
            KahluaTableImpl def = (KahluaTableImpl)object;
            Object object2 = def.rawget("top");
            if (object2 instanceof KahluaTableImpl) {
                KahluaTableImpl tops = (KahluaTableImpl)object2;
                allTops = new ArrayList<StringChance>();
                KahluaTableIterator ittop = tops.iterator();
                while (ittop.advance()) {
                    Object object3 = ittop.getValue();
                    if (!(object3 instanceof KahluaTableImpl)) continue;
                    KahluaTableImpl deftop = (KahluaTableImpl)object3;
                    allTops.add(new StringChance(deftop.rawgetStr("name"), deftop.rawgetFloat("chance")));
                }
            }
            OutfitUnderwearDefinition underwearDef = new OutfitUnderwearDefinition(allTops, def.rawgetStr("bottom"), def.rawgetInt("chanceToSpawn"), def.rawgetStr("gender"));
            m_outfitDefinition.add(underwearDef);
        }
    }

    public static void addRandomUnderwear(IsoZombie zed) {
        if (zed.isSkeleton()) {
            return;
        }
        instance.checkDirty();
        if (OutfitRNG.Next(100) > baseChance) {
            return;
        }
        ArrayList<OutfitUnderwearDefinition> validDefs = new ArrayList<OutfitUnderwearDefinition>();
        int totalChance = 0;
        for (int i = 0; i < m_outfitDefinition.size(); ++i) {
            OutfitUnderwearDefinition def = m_outfitDefinition.get(i);
            if ((!zed.isFemale() || !def.female) && (zed.isFemale() || def.female)) continue;
            validDefs.add(def);
            totalChance += def.chanceToSpawn;
        }
        int choice = OutfitRNG.Next(totalChance);
        OutfitUnderwearDefinition toDo = null;
        int subtotal = 0;
        for (int i = 0; i < validDefs.size(); ++i) {
            OutfitUnderwearDefinition testTable = (OutfitUnderwearDefinition)validDefs.get(i);
            if (choice >= (subtotal += testTable.chanceToSpawn)) continue;
            toDo = testTable;
            break;
        }
        if (toDo != null) {
            Item scriptItem = ScriptManager.instance.FindItem(toDo.bottom);
            ItemVisual bottomVisual = null;
            if (scriptItem != null) {
                bottomVisual = zed.getHumanVisual().addClothingItem(zed.getItemVisuals(), scriptItem);
            }
            if (toDo.top != null) {
                String top = null;
                choice = OutfitRNG.Next(toDo.topTotalChance);
                subtotal = 0;
                for (int i = 0; i < toDo.top.size(); ++i) {
                    StringChance testTable = toDo.top.get(i);
                    if (choice >= (subtotal = (int)((float)subtotal + testTable.chance))) continue;
                    top = testTable.str;
                    break;
                }
                if (top != null && (scriptItem = ScriptManager.instance.FindItem(top)) != null) {
                    ItemVisual topVisual = zed.getHumanVisual().addClothingItem(zed.getItemVisuals(), scriptItem);
                    if (OutfitRNG.Next(100) < 60 && topVisual != null && bottomVisual != null) {
                        topVisual.setTint(bottomVisual.getTint());
                    }
                }
            }
        }
    }

    private static final class StringChance {
        String str;
        float chance;

        public StringChance(String str, float chance) {
            this.str = str;
            this.chance = chance;
        }
    }

    public static final class OutfitUnderwearDefinition {
        public ArrayList<StringChance> top;
        public int topTotalChance;
        public String bottom;
        public int chanceToSpawn;
        public boolean female;

        public OutfitUnderwearDefinition(ArrayList<StringChance> top, String bottom, int chanceToSpawn, String gender) {
            this.top = top;
            if (top != null) {
                for (int i = 0; i < top.size(); ++i) {
                    this.topTotalChance = (int)((float)this.topTotalChance + top.get((int)i).chance);
                }
            }
            this.bottom = bottom;
            this.chanceToSpawn = chanceToSpawn;
            if ("female".equals(gender)) {
                this.female = true;
            }
        }
    }
}

