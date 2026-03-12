/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.IDebugOption;
import zombie.util.StringUtils;

public interface IDebugOptionGroup
extends IDebugOption {
    public Iterable<IDebugOption> getChildren();

    public void addChild(IDebugOption var1);

    public void removeChild(IDebugOption var1);

    public void onChildAdded(IDebugOption var1);

    public void onDescendantAdded(IDebugOption var1);

    default public <E extends IDebugOptionGroup> E newOptionGroup(E newGroup) {
        this.addChild(newGroup);
        return newGroup;
    }

    default public BooleanDebugOption newOption(String name, boolean defaultValue) {
        return BooleanDebugOption.newOption(this, name, defaultValue);
    }

    default public BooleanDebugOption newDebugOnlyOption(String name, boolean defaultValue) {
        return BooleanDebugOption.newDebugOnlyOption(this, name, defaultValue);
    }

    default public String getCombinedName(String childName) {
        String parentGroupName = this.getName();
        if (StringUtils.isNullOrWhitespace(parentGroupName)) {
            return childName;
        }
        return String.format("%s.%s", parentGroupName, childName);
    }
}

