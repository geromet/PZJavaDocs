/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.debug.DebugType;
import zombie.util.StringUtils;

@XmlType(name="AnimCondition")
public final class AnimCondition {
    @XmlElement(name="m_Name")
    public String name = "";
    @XmlElement(name="m_Type")
    public Type type = Type.STRING;
    @XmlElement(name="m_Value")
    public String value = "";
    @XmlElement(name="m_FloatValue")
    public float floatValue;
    @XmlElement(name="m_BoolValue")
    public boolean boolValue;
    @XmlElement(name="m_StringValue")
    public String stringValue = "";
    @XmlTransient
    AnimationVariableReference variableReference;

    public void parse(AnimNode fromNode, AnimNode toNode) {
        this.parseValue();
        this.variableReference = AnimationVariableReference.fromRawVariableName(this.name);
        if (this.isTypeString()) {
            if (this.stringValue.contains("$this")) {
                this.stringValue = this.stringValue.replaceAll("\\$this", fromNode.name);
            }
            if (this.stringValue.contains("$source")) {
                this.stringValue = this.stringValue.replaceAll("\\$source", fromNode.name);
            }
            if (this.stringValue.contains("$target")) {
                if (toNode != null) {
                    this.stringValue = this.stringValue.replaceAll("\\$target", toNode.name);
                } else {
                    DebugType.Animation.error("$target not supported in conditions that have no toNode specified. Only allowed in AnimTransition. FromNode: %s, ToNode: %s", fromNode, toNode);
                }
            }
        }
    }

    public void parseValue() {
        if (this.value.isEmpty()) {
            return;
        }
        Type varType = this.type;
        switch (varType.ordinal()) {
            case 0: 
            case 1: {
                this.stringValue = this.value;
                break;
            }
            case 2: {
                this.boolValue = StringUtils.tryParseBoolean(this.value);
                break;
            }
            case 3: 
            case 4: 
            case 5: 
            case 6: 
            case 7: 
            case 8: {
                this.floatValue = StringUtils.tryParseFloat(this.value);
                break;
            }
        }
    }

    public String toString() {
        return String.format("AnimCondition{name:%s type:%s value:%s }", this.name, this.type.toString(), this.getValueString());
    }

    public String getConditionString() {
        if (this.type == Type.OR) {
            return "OR";
        }
        return String.format("( %s %s %s )", this.name, this.type.toString(), this.getValueString());
    }

    public String getValueString() {
        switch (this.type.ordinal()) {
            case 3: 
            case 4: 
            case 5: 
            case 6: 
            case 7: 
            case 8: {
                return String.valueOf(this.floatValue);
            }
            case 2: {
                return this.boolValue ? "true" : "false";
            }
            case 0: 
            case 1: {
                return this.stringValue;
            }
            case 9: {
                return " -- OR -- ";
            }
        }
        throw new RuntimeException("Unexpected internal type:" + String.valueOf((Object)this.type));
    }

    public boolean isTypeString() {
        return this.type == Type.STRING || this.type == Type.STRNEQ;
    }

    public boolean check(IAnimationVariableSource varSource) {
        Type varType = this.type;
        if (varType == Type.OR) {
            return false;
        }
        IAnimationVariableSlot variableSlot = this.variableReference.getVariable(varSource);
        if (variableSlot == null) {
            switch (varType.ordinal()) {
                case 0: {
                    return StringUtils.equalsIgnoreCase(this.stringValue, "");
                }
                case 1: {
                    return !StringUtils.equalsIgnoreCase(this.stringValue, "");
                }
                case 2: {
                    return !this.boolValue;
                }
                case 9: {
                    return false;
                }
                case 3: 
                case 4: 
                case 5: 
                case 6: 
                case 7: 
                case 8: {
                    DebugType.Animation.warnOnce("Variable \"%s\" not found in %s", this.variableReference, varSource);
                    return false;
                }
            }
        }
        switch (varType.ordinal()) {
            case 3: {
                return this.floatValue == variableSlot.getValueFloat();
            }
            case 4: {
                return this.floatValue != variableSlot.getValueFloat();
            }
            case 5: {
                return variableSlot.getValueFloat() < this.floatValue;
            }
            case 6: {
                return variableSlot.getValueFloat() > this.floatValue;
            }
            case 7: {
                return PZMath.abs(variableSlot.getValueFloat()) < this.floatValue;
            }
            case 8: {
                return PZMath.abs(variableSlot.getValueFloat()) > this.floatValue;
            }
            case 2: {
                return variableSlot.getValueBool() == this.boolValue;
            }
            case 0: {
                return StringUtils.equalsIgnoreCase(this.stringValue, variableSlot.getValueString());
            }
            case 1: {
                return !StringUtils.equalsIgnoreCase(this.stringValue, variableSlot.getValueString());
            }
            case 9: {
                return false;
            }
        }
        throw new RuntimeException("Unexpected internal type:" + String.valueOf((Object)this.type));
    }

    public static boolean pass(IAnimationVariableSource varSource, AnimCondition[] conditions) {
        boolean valid = true;
        for (AnimCondition condition : conditions) {
            if (condition.type == Type.OR) {
                if (valid) break;
                valid = true;
                continue;
            }
            valid = valid && condition.check(varSource);
        }
        return valid;
    }

    @XmlEnum
    @XmlType(name="Type")
    public static enum Type {
        STRING,
        STRNEQ,
        BOOL,
        EQU,
        NEQ,
        LESS,
        GTR,
        ABSLESS,
        ABSGTR,
        OR;

    }
}

