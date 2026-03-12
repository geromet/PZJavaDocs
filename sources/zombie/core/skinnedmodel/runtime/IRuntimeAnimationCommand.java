/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.runtime;

import java.util.List;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.scripting.ScriptParser;

public interface IRuntimeAnimationCommand {
    public void parse(ScriptParser.Block var1);

    public void exec(List<Keyframe> var1);
}

