/*
 * Decompiled with CFR 0.152.
 */
package zombie.world.scripts;

import zombie.world.WorldDictionaryException;

public interface IVersionHash {
    public boolean isEmpty();

    public String getString();

    public void add(String var1);

    public void add(IVersionHash var1);

    public long getHash() throws WorldDictionaryException;
}

