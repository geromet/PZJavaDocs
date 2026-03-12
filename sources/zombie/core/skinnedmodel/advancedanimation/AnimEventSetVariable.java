/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import javax.xml.bind.annotation.XmlTransient;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;

public class AnimEventSetVariable
extends AnimEvent {
    @XmlTransient
    public AnimationVariableReference variableReference;
    @XmlTransient
    public String setVariableValue;

    public AnimEventSetVariable(AnimEvent event) {
        super(event);
        String[] ss = event.parameterValue.split("=");
        if (ss.length == 2) {
            this.variableReference = AnimationVariableReference.fromRawVariableName(ss[0]);
            this.setVariableValue = ss[1];
        } else {
            this.variableReference = AnimationVariableReference.fromRawVariableName(event.parameterValue);
            this.setVariableValue = "";
        }
    }
}

