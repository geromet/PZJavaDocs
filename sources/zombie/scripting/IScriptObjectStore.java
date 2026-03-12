/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import zombie.scripting.objects.Item;
import zombie.scripting.objects.Recipe;

public interface IScriptObjectStore {
    public Item getItem(String var1);

    public Recipe getRecipe(String var1);
}

