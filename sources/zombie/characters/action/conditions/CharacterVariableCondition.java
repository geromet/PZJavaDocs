/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.util.StringUtils;

public final class CharacterVariableCondition
implements IActionCondition {
    private Operator op;
    private Object lhsValue;
    private Object rhsValue;

    private static Object parseValue(String value, boolean parseForCharacterVariableLookup) {
        if (value.length() <= 0) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '-' || first == '+' || first >= '0' && first <= '9') {
            int readPos;
            int intVal = 0;
            if (first >= '0' && first <= '9') {
                intVal = first - 48;
            }
            for (readPos = 1; readPos < value.length(); ++readPos) {
                char chr = value.charAt(readPos);
                if (chr >= '0' && chr <= '9') {
                    intVal = intVal * 10 + chr - 48;
                    continue;
                }
                if (chr == ',') continue;
                if (chr == '.') {
                    ++readPos;
                    break;
                }
                return value;
            }
            if (readPos == value.length()) {
                return intVal;
            }
            float floatVal = intVal;
            float divisor = 10.0f;
            while (readPos < value.length()) {
                char chr = value.charAt(readPos);
                if (chr >= '0' && chr <= '9') {
                    floatVal += (float)(chr - 48) / divisor;
                    divisor *= 10.0f;
                } else if (chr != ',') {
                    return value;
                }
                ++readPos;
            }
            if (first == '-') {
                floatVal *= -1.0f;
            }
            return Float.valueOf(floatVal);
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
            return false;
        }
        if (parseForCharacterVariableLookup) {
            if (first == '\'' || first == '\"') {
                StringBuilder sb = new StringBuilder(value.length() - 2);
                block6: for (int readPos = 1; readPos < value.length(); ++readPos) {
                    char c = value.charAt(readPos);
                    switch (c) {
                        case '\"': 
                        case '\'': {
                            if (c == first) {
                                return sb.toString();
                            }
                        }
                        default: {
                            sb.append(c);
                            continue block6;
                        }
                        case '\\': {
                            sb.append(value.charAt(readPos));
                        }
                    }
                }
                return sb.toString();
            }
            return new CharacterVariableLookup(value);
        }
        return value;
    }

    private boolean load(Element node) {
        switch (node.getNodeName()) {
            case "isTrue": {
                this.op = Operator.Equal;
                this.lhsValue = new CharacterVariableLookup(node.getTextContent().trim());
                this.rhsValue = true;
                return true;
            }
            case "isFalse": {
                this.op = Operator.Equal;
                this.lhsValue = new CharacterVariableLookup(node.getTextContent().trim());
                this.rhsValue = false;
                return true;
            }
            case "compare": {
                switch (node.getAttribute("op").trim()) {
                    default: {
                        return false;
                    }
                    case "=": 
                    case "==": {
                        this.op = Operator.Equal;
                        break;
                    }
                    case "!=": 
                    case "<>": {
                        this.op = Operator.NotEqual;
                        break;
                    }
                    case "<": {
                        this.op = Operator.Less;
                        break;
                    }
                    case ">": {
                        this.op = Operator.Greater;
                        break;
                    }
                    case "<=": {
                        this.op = Operator.LessEqual;
                        break;
                    }
                    case ">=": {
                        this.op = Operator.GreaterEqual;
                    }
                }
                this.loadCompareValues(node);
                return true;
            }
            case "gtr": {
                this.op = Operator.Greater;
                this.loadCompareValues(node);
                return true;
            }
            case "less": {
                this.op = Operator.Less;
                this.loadCompareValues(node);
                return true;
            }
            case "equals": {
                this.op = Operator.Equal;
                this.loadCompareValues(node);
                return true;
            }
            case "notEquals": {
                this.op = Operator.NotEqual;
                this.loadCompareValues(node);
                return true;
            }
            case "lessEqual": {
                this.op = Operator.LessEqual;
                this.loadCompareValues(node);
                return true;
            }
            case "gtrEqual": {
                this.op = Operator.GreaterEqual;
                this.loadCompareValues(node);
                return true;
            }
        }
        return false;
    }

    private void loadCompareValues(Element node) {
        String lhsString = node.getAttribute("a").trim();
        String rhsString = node.getAttribute("b").trim();
        this.lhsValue = CharacterVariableCondition.parseValue(lhsString, true);
        this.rhsValue = CharacterVariableCondition.parseValue(rhsString, false);
    }

    private static Object resolveValue(Object value, IAnimationVariableSource owner) {
        if (value instanceof CharacterVariableLookup) {
            CharacterVariableLookup lookUp = (CharacterVariableLookup)value;
            String variableValue = lookUp.getValueString(owner);
            if (variableValue != null) {
                return CharacterVariableCondition.parseValue(variableValue, false);
            }
            return null;
        }
        return value;
    }

    private boolean resolveCompareTo(int result) {
        switch (this.op.ordinal()) {
            case 0: {
                return result == 0;
            }
            case 1: {
                return result != 0;
            }
            case 2: {
                return result < 0;
            }
            case 4: {
                return result <= 0;
            }
            case 3: {
                return result > 0;
            }
            case 5: {
                return result >= 0;
            }
        }
        return false;
    }

    @Override
    public boolean passes(ActionContext context, ActionState state) {
        String string;
        IAnimatable owner = context.getOwner();
        Object lhsResolved = CharacterVariableCondition.resolveValue(this.lhsValue, owner);
        Object rhsResolved = CharacterVariableCondition.resolveValue(this.rhsValue, owner);
        if (lhsResolved == null && rhsResolved instanceof String && StringUtils.isNullOrEmpty(string = (String)rhsResolved)) {
            if (this.op == Operator.Equal) {
                return true;
            }
            if (this.op == Operator.NotEqual) {
                return false;
            }
            boolean bl = true;
        }
        if (lhsResolved == null || rhsResolved == null) {
            return false;
        }
        if (lhsResolved.getClass().equals(rhsResolved.getClass())) {
            if (lhsResolved instanceof String) {
                String s = (String)lhsResolved;
                return this.resolveCompareTo(s.compareTo((String)rhsResolved));
            }
            if (lhsResolved instanceof Integer) {
                Integer i = (Integer)lhsResolved;
                return this.resolveCompareTo(i.compareTo((Integer)rhsResolved));
            }
            if (lhsResolved instanceof Float) {
                Float f = (Float)lhsResolved;
                return this.resolveCompareTo(f.compareTo((Float)rhsResolved));
            }
            if (lhsResolved instanceof Boolean) {
                Boolean b = (Boolean)lhsResolved;
                return this.resolveCompareTo(b.compareTo((Boolean)rhsResolved));
            }
        }
        boolean lhsIsInt = lhsResolved instanceof Integer;
        boolean lhsIsFloat = lhsResolved instanceof Float;
        boolean rhsIsInt = rhsResolved instanceof Integer;
        boolean rhsIsFloat = rhsResolved instanceof Float;
        if ((lhsIsInt || lhsIsFloat) && (rhsIsInt || rhsIsFloat)) {
            boolean lhsWasLookup = this.lhsValue instanceof CharacterVariableLookup;
            boolean rhsWasLookup = this.rhsValue instanceof CharacterVariableLookup;
            if (lhsWasLookup == rhsWasLookup) {
                float lhsFloat = lhsIsFloat ? ((Float)lhsResolved).floatValue() : (float)((Integer)lhsResolved).intValue();
                float rhsFloat = rhsIsFloat ? ((Float)rhsResolved).floatValue() : (float)((Integer)rhsResolved).intValue();
                return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
            }
            if (lhsWasLookup) {
                if (rhsIsFloat) {
                    float lhsFloat = lhsIsFloat ? ((Float)lhsResolved).floatValue() : (float)((Integer)lhsResolved).intValue();
                    float rhsFloat = ((Float)rhsResolved).floatValue();
                    return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
                }
                int lhsInt = lhsIsFloat ? (int)((Float)lhsResolved).floatValue() : (Integer)lhsResolved;
                int rhsInt = (Integer)rhsResolved;
                return this.resolveCompareTo(Integer.compare(lhsInt, rhsInt));
            }
            if (lhsIsFloat) {
                float lhsFloat = ((Float)lhsResolved).floatValue();
                float rhsFloat = rhsIsFloat ? ((Float)rhsResolved).floatValue() : (float)((Integer)rhsResolved).intValue();
                return this.resolveCompareTo(Float.compare(lhsFloat, rhsFloat));
            }
            int lhsInt = (Integer)lhsResolved;
            int rhsInt = rhsIsFloat ? (int)((Float)rhsResolved).floatValue() : (Integer)rhsResolved;
            return this.resolveCompareTo(Integer.compare(lhsInt, rhsInt));
        }
        return false;
    }

    @Override
    public IActionCondition clone() {
        return this;
    }

    private static String getOpString(Operator op) {
        switch (op.ordinal()) {
            case 0: {
                return " == ";
            }
            case 1: {
                return " != ";
            }
            case 2: {
                return " < ";
            }
            case 4: {
                return " <= ";
            }
            case 3: {
                return " > ";
            }
            case 5: {
                return " >=";
            }
        }
        return " ?? ";
    }

    private static String valueToString(Object value) {
        if (value instanceof String) {
            return "\"" + String.valueOf(value) + "\"";
        }
        return value.toString();
    }

    @Override
    public String getDescription() {
        return CharacterVariableCondition.valueToString(this.lhsValue) + CharacterVariableCondition.getOpString(this.op) + CharacterVariableCondition.valueToString(this.rhsValue);
    }

    public String toString() {
        return this.toString("");
    }

    @Override
    public String toString(String indent) {
        return indent + this.getClass().getName() + "{ " + this.getDescription() + " }";
    }

    private static class CharacterVariableLookup {
        private final AnimationVariableReference variableReference;

        public CharacterVariableLookup(String variableName) {
            this.variableReference = AnimationVariableReference.fromRawVariableName(variableName);
        }

        public String getValueString(IAnimationVariableSource owner) {
            IAnimationVariableSlot variableSlot = this.variableReference.getVariable(owner);
            if (variableSlot == null) {
                return null;
            }
            return variableSlot.getValueString();
        }

        public String toString() {
            return this.variableReference.toString();
        }
    }

    static enum Operator {
        Equal,
        NotEqual,
        Less,
        Greater,
        LessEqual,
        GreaterEqual;

    }

    public static class Factory
    implements IActionCondition.IFactory {
        @Override
        public IActionCondition create(Element conditionNode) {
            CharacterVariableCondition cond = new CharacterVariableCondition();
            if (cond.load(conditionNode)) {
                return cond;
            }
            return null;
        }
    }
}

