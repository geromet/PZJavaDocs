/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.util.HashMap;
import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;

public interface IActionCondition {
    public static final HashMap<String, IFactory> s_factoryMap = new HashMap();

    public String getDescription();

    public boolean passes(ActionContext var1, ActionState var2);

    public IActionCondition clone();

    public String toString(String var1);

    public static IActionCondition createInstance(Element conditionNode) {
        IFactory fact = s_factoryMap.get(conditionNode.getNodeName());
        if (fact != null) {
            return fact.create(conditionNode);
        }
        return null;
    }

    public static void registerFactory(String elementName, IFactory factory2) {
        s_factoryMap.put(elementName, factory2);
    }

    public static interface IFactory {
        public IActionCondition create(Element var1);
    }
}

