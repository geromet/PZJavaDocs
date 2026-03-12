/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.core.skinnedmodel.population.OutfitRNG;

public final class DefaultClothing {
    public static final DefaultClothing instance = new DefaultClothing();
    public final Clothing pants = new Clothing();
    public final Clothing tShirt = new Clothing();
    public final Clothing tShirtDecal = new Clothing();
    public final Clothing vest = new Clothing();
    public boolean dirty = true;

    private void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        this.pants.clear();
        this.tShirt.clear();
        this.tShirtDecal.clear();
        this.vest.clear();
        Object object = LuaManager.env.rawget("DefaultClothing");
        if (!(object instanceof KahluaTable)) {
            return;
        }
        KahluaTable defaults = (KahluaTable)object;
        this.initClothing(defaults, this.pants, "Pants");
        this.initClothing(defaults, this.tShirt, "TShirt");
        this.initClothing(defaults, this.tShirtDecal, "TShirtDecal");
        this.initClothing(defaults, this.vest, "Vest");
    }

    private void initClothing(KahluaTable defaults, Clothing clothing, String key) {
        Object object = defaults.rawget(key);
        if (!(object instanceof KahluaTable)) {
            return;
        }
        KahluaTable table = (KahluaTable)object;
        this.tableToArrayList(table, "hue", clothing.hue);
        this.tableToArrayList(table, "texture", clothing.texture);
        this.tableToArrayList(table, "tint", clothing.tint);
    }

    private void tableToArrayList(KahluaTable table, String key, ArrayList<String> list) {
        KahluaTableImpl table2 = (KahluaTableImpl)table.rawget(key);
        if (table2 == null) {
            return;
        }
        int len = table2.len();
        for (int i = 1; i <= len; ++i) {
            Object o = table2.rawget(i);
            if (o == null) continue;
            list.add(o.toString());
        }
    }

    public String pickPantsHue() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.hue);
    }

    public String pickPantsTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.texture);
    }

    public String pickPantsTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.tint);
    }

    public String pickTShirtTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirt.texture);
    }

    public String pickTShirtTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirt.tint);
    }

    public String pickTShirtDecalTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirtDecal.texture);
    }

    public String pickTShirtDecalTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirtDecal.tint);
    }

    public String pickVestTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.vest.texture);
    }

    public String pickVestTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.vest.tint);
    }

    private static final class Clothing {
        final ArrayList<String> hue = new ArrayList();
        final ArrayList<String> texture = new ArrayList();
        final ArrayList<String> tint = new ArrayList();

        private Clothing() {
        }

        void clear() {
            this.hue.clear();
            this.texture.clear();
            this.tint.clear();
        }
    }
}

