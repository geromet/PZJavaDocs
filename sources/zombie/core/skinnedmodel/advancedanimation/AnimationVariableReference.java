/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.util.StringUtils;

public class AnimationVariableReference {
    private String subVariableSourceName;
    private String name;
    private AnimationVariableHandle variableHandle;

    private AnimationVariableReference() {
    }

    public boolean equals(AnimationVariableReference rhs) {
        return rhs != null && StringUtils.equalsIgnoreCase(this.subVariableSourceName, rhs.subVariableSourceName) && StringUtils.equalsIgnoreCase(this.name, rhs.name) && AnimationVariableHandle.equals(this.getVariableHandle(), rhs.getVariableHandle());
    }

    public String getName() {
        return this.name;
    }

    public String getSubVariableSourceName() {
        return this.subVariableSourceName;
    }

    private void parse() {
        String rawVariableName = this.name;
        if (!rawVariableName.startsWith("$")) {
            return;
        }
        if (rawVariableName.indexOf(46) <= 1) {
            return;
        }
        int indexOfDot = rawVariableName.indexOf(46);
        String subVariableSourceName = rawVariableName.substring(1, indexOfDot);
        String subVariableName = rawVariableName.substring(indexOfDot + 1);
        if (StringUtils.isNullOrWhitespace(subVariableName)) {
            DebugType.Animation.warn("Error parsing: %s", rawVariableName);
            DebugType.Animation.warn("  SubVariableName not specified.");
            DebugType.Animation.warn("  Expected: $<subVariableSource>.<subVariableName>");
            return;
        }
        if (!StringUtils.isValidVariableName(subVariableSourceName)) {
            DebugType.Animation.warn("Error parsing: %s", rawVariableName);
            DebugType.Animation.warn("  SubVariableSource name not valid. Only AlphaNumeric or underscores '_' allowed.");
            return;
        }
        this.subVariableSourceName = subVariableSourceName;
        this.name = subVariableName;
    }

    public static AnimationVariableReference fromRawVariableName(String rawVariableName) {
        if (Core.debug) {
            AnimatorDebugMonitor.registerVariable(rawVariableName);
        }
        AnimationVariableReference newReference = new AnimationVariableReference();
        newReference.name = rawVariableName;
        newReference.parse();
        return newReference;
    }

    public IAnimationVariableSlot getVariable(IAnimationVariableSource varSource) {
        if (this.getName().isBlank()) {
            return null;
        }
        AnimationVariableHandle variableHandle = this.getVariableHandle();
        IAnimationVariableSource animationVariableSource = this.getAnimationVariableSource(varSource);
        if (animationVariableSource == null) {
            return null;
        }
        return animationVariableSource.getVariable(variableHandle);
    }

    private AnimationVariableHandle getVariableHandle() {
        if (this.variableHandle == null) {
            this.variableHandle = AnimationVariableHandle.alloc(this.name);
        }
        return this.variableHandle;
    }

    private IAnimationVariableSource getAnimationVariableSource(IAnimationVariableSource varSource) {
        if (this.subVariableSourceName != null) {
            IAnimationVariableSource subVarSource = varSource.getSubVariableSource(this.subVariableSourceName);
            if (subVarSource == null) {
                DebugType.Animation.warnOnce("SubVariableSource name \"%s\" does not exist in %s", this.subVariableSourceName, varSource);
            }
            return subVarSource;
        }
        return varSource;
    }

    public boolean isSubVariableSourceReference() {
        return this.subVariableSourceName != null;
    }

    public void setVariable(IAnimationVariableSource owner, String variableValue) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
            return;
        }
        IAnimationVariableSlot slot = this.getVariable(owner);
        if (slot != null) {
            slot.setValue(variableValue);
            return;
        }
        String variableName = this.getName();
        IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
        if (variableSource instanceof IAnimationVariableMap) {
            IAnimationVariableMap iAnimationVariableMap = (IAnimationVariableMap)variableSource;
            iAnimationVariableMap.setVariable(variableName, variableValue);
        } else {
            DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, variableValue);
        }
    }

    public void setVariable(IAnimationVariableSource owner, boolean variableValue) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
            return;
        }
        IAnimationVariableSlot slot = this.getVariable(owner);
        if (slot != null) {
            slot.setValue(variableValue);
            return;
        }
        String variableName = this.getName();
        IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
        if (variableSource instanceof IAnimationVariableMap) {
            IAnimationVariableMap iAnimationVariableMap = (IAnimationVariableMap)variableSource;
            iAnimationVariableMap.setVariable(variableName, variableValue);
        } else {
            DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot set %s.%s=%s", variableSource, variableName, variableValue);
        }
    }

    public void clearVariable(IAnimationVariableSource owner) {
        if (this.getName().isBlank()) {
            DebugLog.General.warnOnce("Variable name is blank. Cannot set.");
            return;
        }
        IAnimationVariableSlot slot = this.getVariable(owner);
        if (slot != null) {
            slot.clear();
            return;
        }
        String variableName = this.getName();
        IAnimationVariableSource variableSource = this.getAnimationVariableSource(owner);
        if (variableSource instanceof IAnimationVariableMap) {
            IAnimationVariableMap iAnimationVariableMap = (IAnimationVariableMap)variableSource;
            iAnimationVariableMap.clearVariable(variableName);
        } else {
            DebugLog.General.warnOnce("Destination VariableSource is read-only. Cannot clear variable %s.%s", variableSource, variableName);
        }
    }

    public String toString() {
        return this.getClass().getSimpleName() + "{" + (String)(this.isSubVariableSourceReference() ? " sourceName:" + this.getSubVariableSourceName() + ", " : "") + " variableName:" + this.getName() + " }";
    }
}

