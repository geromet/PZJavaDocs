/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.HashMap;

public final class SharedStrings {
    private final HashMap<String, String> strings = new HashMap();

    public String get(String s) {
        String shared = this.strings.get(s);
        if (shared == null) {
            this.strings.put(s, s);
            shared = s;
        }
        return shared;
    }

    public void clear() {
        this.strings.clear();
    }
}

