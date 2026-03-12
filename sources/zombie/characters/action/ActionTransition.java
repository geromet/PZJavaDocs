/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import zombie.characters.action.ActionContext;
import zombie.characters.action.ActionState;
import zombie.characters.action.IActionCondition;
import zombie.core.math.PZMath;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.Lambda;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;

public final class ActionTransition
implements Cloneable {
    String transitionTo;
    boolean asSubstate;
    boolean transitionOut;
    boolean forceParent;
    int conditionPriority;
    final List<IActionCondition> conditions = new ArrayList<IActionCondition>();

    public static boolean parse(Element root, String srcInfo, List<ActionTransition> transitions) {
        if (root.getNodeName().equals("transitions")) {
            ActionTransition.parseTransitions(root, srcInfo, transitions);
            return true;
        }
        if (root.getNodeName().equals("transition")) {
            ActionTransition.parseTransition(root, transitions);
            return true;
        }
        return false;
    }

    public static void parseTransition(Element root, List<ActionTransition> transitions) {
        transitions.clear();
        ActionTransition trans = new ActionTransition();
        if (trans.load(root)) {
            transitions.add(trans);
        }
    }

    public static void parseTransitions(Element root, String srcInfo, List<ActionTransition> transitions) {
        transitions.clear();
        Lambda.forEachFrom(PZXmlUtil::forEachElement, root, srcInfo, transitions, (child, lSrcInfo, lTransitions) -> {
            if (!child.getNodeName().equals("transition")) {
                DebugType.ActionSystem.warn("Warning: Unrecognised element '%s' in %s", child.getNodeName(), lSrcInfo);
                return;
            }
            ActionTransition trans = new ActionTransition();
            if (trans.load((Element)child)) {
                lTransitions.add(trans);
            }
        });
    }

    private boolean load(Element transitionElement) {
        try {
            PZXmlUtil.forEachElement(transitionElement, child -> {
                try {
                    String s = child.getNodeName();
                    if ("transitionTo".equalsIgnoreCase(s)) {
                        this.transitionTo = child.getTextContent();
                    } else if ("transitionOut".equalsIgnoreCase(s)) {
                        this.transitionOut = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("forceParent".equalsIgnoreCase(s)) {
                        this.forceParent = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("asSubstate".equalsIgnoreCase(s)) {
                        this.asSubstate = StringUtils.tryParseBoolean(child.getTextContent());
                    } else if ("conditionPriority".equalsIgnoreCase(s)) {
                        this.conditionPriority = PZMath.tryParseInt(child.getTextContent(), 0);
                    } else if ("conditions".equalsIgnoreCase(s)) {
                        PZXmlUtil.forEachElement(child, conditionNode -> {
                            IActionCondition condition = IActionCondition.createInstance(conditionNode);
                            if (condition != null) {
                                this.conditions.add(condition);
                            }
                        });
                    }
                }
                catch (Exception e) {
                    DebugType.ActionSystem.printException(e, "Error while parsing xml element: " + child.getNodeName(), LogSeverity.Error);
                }
            });
            return true;
        }
        catch (Exception e) {
            DebugType.ActionSystem.printException(e, "Error while loading an ActionTransition element", LogSeverity.Error);
            return false;
        }
    }

    public String getTransitionTo() {
        return this.transitionTo;
    }

    public boolean passes(ActionContext context, ActionState state) {
        for (int i = 0; i < this.conditions.size(); ++i) {
            IActionCondition cond = this.conditions.get(i);
            if (cond.passes(context, state)) continue;
            return false;
        }
        return true;
    }

    public ActionTransition clone() {
        ActionTransition cloned = new ActionTransition();
        cloned.transitionTo = this.transitionTo;
        cloned.asSubstate = this.asSubstate;
        cloned.transitionOut = this.transitionOut;
        cloned.forceParent = this.forceParent;
        cloned.conditionPriority = this.conditionPriority;
        for (IActionCondition cond : this.conditions) {
            cloned.conditions.add(cond.clone());
        }
        return cloned;
    }

    public String toString() {
        return this.toString("");
    }

    public String toString(String indent) {
        StringBuilder result = new StringBuilder();
        result.append(indent).append(this.getClass().getName()).append("\r\n");
        result.append(indent).append("{").append("\r\n");
        result.append(indent).append("\t").append("transitionTo:").append(this.transitionTo).append("\r\n");
        result.append(indent).append("\t").append("asSubstate:").append(this.asSubstate).append("\r\n");
        result.append(indent).append("\t").append("transitionOut:").append(this.transitionOut).append("\r\n");
        result.append(indent).append("\t").append("forceParent:").append(this.forceParent).append("\r\n");
        result.append(indent).append("\t").append("conditionPriority:").append(this.forceParent).append("\r\n");
        result.append(indent).append("\t").append("transitions:").append("\r\n");
        result.append(indent).append("\t{").append("\r\n");
        for (int icondition = 0; icondition < this.conditions.size(); ++icondition) {
            result.append(indent).append(this.conditions.get(icondition).toString(indent + "\t")).append(",").append("\r\n");
        }
        result.append(indent).append("\t}").append("\r\n");
        result.append(indent).append("}");
        return result.toString();
    }
}

