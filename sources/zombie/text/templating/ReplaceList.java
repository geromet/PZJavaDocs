/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import java.util.ArrayList;
import zombie.text.templating.IReplace;
import zombie.text.templating.TemplateText;

public class ReplaceList
implements IReplace {
    private final ArrayList<String> replacements;

    public ReplaceList() {
        this.replacements = new ArrayList();
    }

    public ReplaceList(ArrayList<String> replacements) {
        this.replacements = replacements;
    }

    protected ArrayList<String> getReplacements() {
        return this.replacements;
    }

    @Override
    public String getString() {
        if (this.replacements.isEmpty()) {
            return "!ERROR_EMPTY_LIST!";
        }
        return this.replacements.get(TemplateText.RandNext(this.replacements.size()));
    }
}

