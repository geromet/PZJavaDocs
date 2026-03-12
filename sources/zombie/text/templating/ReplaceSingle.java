/*
 * Decompiled with CFR 0.152.
 */
package zombie.text.templating;

import zombie.text.templating.IReplace;

public class ReplaceSingle
implements IReplace {
    private String value = "";

    public ReplaceSingle() {
    }

    public ReplaceSingle(String value) {
        this.value = value;
    }

    protected String getValue() {
        return this.value;
    }

    protected void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getString() {
        return this.value;
    }
}

