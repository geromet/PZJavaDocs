/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import zombie.text.templating.IReplace;

public interface IReplaceProvider {
    public boolean hasReplacer(String var1);

    public IReplace getReplacer(String var1);
}

