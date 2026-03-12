/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandlePool;
import zombie.util.StringUtils;

public class AnimationVariableHandle {
    private String name;
    private int varIndex = -1;

    AnimationVariableHandle() {
    }

    public static boolean equals(AnimationVariableHandle lhs, AnimationVariableHandle rhs) {
        if (lhs == rhs) {
            return true;
        }
        return lhs != null && lhs.equals(rhs);
    }

    public boolean equals(AnimationVariableHandle rhs) {
        return rhs != null && StringUtils.equalsIgnoreCase(this.name, rhs.name) && this.varIndex == rhs.varIndex;
    }

    public static AnimationVariableHandle alloc(String name) {
        return AnimationVariableHandlePool.getOrCreate(name);
    }

    public String getVariableName() {
        return this.name;
    }

    public int getVariableIndex() {
        return this.varIndex;
    }

    void setVariableName(String name) {
        this.name = name;
    }

    void setVariableIndex(int idx) {
        this.varIndex = idx;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "{  variableName:" + this.name + ",  variableIndex:" + this.varIndex + " }";
    }
}

