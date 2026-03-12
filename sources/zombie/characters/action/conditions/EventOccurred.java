/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action.conditions;

import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;

public final class EventOccurred
implements IActionCondition {
    public String eventName;

    @Override
    public String getDescription() {
        return "EventOccurred(" + this.eventName + ")";
    }

    private boolean load(Element node) {
        this.eventName = node.getTextContent().toLowerCase();
        return true;
    }

    @Override
    public boolean passes(ActionContext context, ActionState state) {
        return context.hasEventOccurred(this.eventName, state.getName());
    }

    @Override
    public IActionCondition clone() {
        return null;
    }

    public String toString() {
        return this.toString("");
    }

    @Override
    public String toString(String indent) {
        return indent + this.getClass().getName() + "{ " + this.eventName + " }";
    }

    public static class Factory
    implements IActionCondition.IFactory {
        @Override
        public IActionCondition create(Element conditionNode) {
            EventOccurred cond = new EventOccurred();
            if (cond.load(conditionNode)) {
                return cond;
            }
            return null;
        }
    }
}

