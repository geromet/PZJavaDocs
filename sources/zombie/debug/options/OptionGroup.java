/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.options;

import java.util.ArrayList;
import zombie.debug.options.IDebugOption;
import zombie.debug.options.IDebugOptionGroup;
import zombie.util.StringUtils;

public class OptionGroup
implements IDebugOptionGroup {
    public final IDebugOptionGroup group;
    private IDebugOptionGroup parentGroup;
    private final String groupName;
    private String fullName;
    private final ArrayList<IDebugOption> children = new ArrayList();

    public OptionGroup() {
        this(null);
    }

    public OptionGroup(IDebugOptionGroup parentGroup) {
        this(parentGroup, null);
    }

    public OptionGroup(IDebugOptionGroup parentGroup, String groupName) {
        this.group = this;
        this.fullName = this.groupName = this.getGroupName(groupName);
        if (parentGroup != null) {
            parentGroup.addChild(this);
        }
    }

    @Override
    public String getName() {
        return this.fullName;
    }

    @Override
    public IDebugOptionGroup getParent() {
        return this.parentGroup;
    }

    @Override
    public void setParent(IDebugOptionGroup parent) {
        if (this.parentGroup != null) {
            IDebugOptionGroup parentGroup = this.parentGroup;
            this.parentGroup = null;
            parentGroup.removeChild(this);
        }
        this.parentGroup = parent;
        this.onFullPathChanged();
    }

    @Override
    public void onFullPathChanged() {
        String newFullName = OptionGroup.getCombinedName(this.parentGroup, this.groupName);
        if (!StringUtils.equals(this.fullName, newFullName)) {
            this.fullName = newFullName;
        }
        for (IDebugOption childOption : this.children) {
            childOption.onFullPathChanged();
        }
    }

    @Override
    public Iterable<IDebugOption> getChildren() {
        return this.children;
    }

    @Override
    public void addChild(IDebugOption childOption) {
        if (this.children.contains(childOption)) {
            return;
        }
        this.children.add(childOption);
        childOption.setParent(this);
        this.onChildAdded(childOption);
    }

    @Override
    public void removeChild(IDebugOption childOption) {
        if (!this.children.contains(childOption)) {
            return;
        }
        this.children.remove(childOption);
        childOption.setParent(null);
    }

    @Override
    public void onChildAdded(IDebugOption newOption) {
        this.onDescendantAdded(newOption);
    }

    @Override
    public void onDescendantAdded(IDebugOption newOption) {
        if (this.parentGroup != null) {
            this.parentGroup.onDescendantAdded(newOption);
        }
    }

    public String getGroupName(String groupName) {
        if (groupName == null && (groupName = this.getClass().getSimpleName()).endsWith("OG")) {
            groupName = groupName.substring(0, groupName.length() - 2);
        }
        return groupName;
    }

    public static String getCombinedName(IDebugOptionGroup parentGroup, String childName) {
        if (parentGroup == null) {
            return childName;
        }
        return parentGroup.getCombinedName(childName);
    }
}

