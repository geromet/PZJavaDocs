/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.text.templating.IReplace;
import zombie.text.templating.IReplaceProvider;

public interface ITemplateBuilder {
    public String Build(String var1);

    public String Build(String var1, IReplaceProvider var2);

    public String Build(String var1, KahluaTableImpl var2);

    public void RegisterKey(String var1, KahluaTableImpl var2);

    public void RegisterKey(String var1, IReplace var2);

    public void Reset();

    public void CopyFrom(Object var1);
}

