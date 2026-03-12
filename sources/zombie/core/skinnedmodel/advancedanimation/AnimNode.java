/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.IInterpolator;
import zombie.core.math.interpolators.xml.InterpolatorSlot;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.Anim2DBlend;
import zombie.core.skinnedmodel.advancedanimation.Anim2DBlendPicker;
import zombie.core.skinnedmodel.advancedanimation.Anim2DBlendTriangle;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.core.skinnedmodel.advancedanimation.AnimCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimEventFlagWhileAlive;
import zombie.core.skinnedmodel.advancedanimation.AnimEventSetVariable;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.GrappleOffsetBehaviour;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.animation.BoneAxis;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.debug.DebugType;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@XmlRootElement(name="animNode")
public final class AnimNode {
    private static final Comparator<AnimEvent> eventsComparator = (lhs, rhs) -> Float.compare(lhs.timePc, rhs.timePc);
    @XmlElement(name="m_Name")
    public String name = "";
    @XmlTransient
    private boolean isIdleAnim;
    @XmlElement(name="m_Priority")
    public int priority = 5;
    @XmlElement(name="m_ConditionPriority")
    public int conditionPriority;
    @XmlElement(name="m_AnimName")
    public String animName = "";
    @XmlElement(name="m_AlternateAnims")
    public List<String> alternateAnims = new ArrayList<String>();
    @XmlElement(name="m_MatchingGrappledAnimNode")
    public String matchingGrappledAnimNode = "";
    @XmlElement(name="m_GrappleOffsetForward")
    public float grappleOffsetForward;
    @XmlElement(name="m_GrappleOffsetYaw")
    public float grappleOffsetYaw;
    @XmlElement(name="m_GrappleTweenInTime")
    public float grappleTweenInTime = 0.25f;
    @XmlElement(name="m_GrapplerOffsetBehaviour")
    public GrappleOffsetBehaviour grapplerOffsetBehaviour = GrappleOffsetBehaviour.GRAPPLED;
    @XmlElement(name="m_isRagdoll")
    public boolean isRagdoll;
    @XmlElement(name="m_chanceToRagdoll")
    public float chanceToRagdoll = 1.0f;
    @XmlElement(name="m_ragdollStartTimeMin")
    public float ragdollStartTimeMin;
    @XmlElement(name="m_ragdollStartTimeMax")
    public float ragdollStartTimeMax;
    @XmlElement(name="m_ragdollMaxTime")
    public float ragdollMaxTime = 5.0f;
    @XmlElement(name="m_DeferredBoneName")
    public String deferredBoneName = "Translation_Data";
    @XmlElement(name="m_deferredBoneAxis")
    public BoneAxis deferredBoneAxis = BoneAxis.Y;
    @XmlElement(name="m_useDeferedRotation")
    public boolean useDeferedRotation;
    @XmlElement(name="m_useDeferredMovement")
    public boolean useDeferredMovement = true;
    @XmlElement(name="m_deferredRotationScale")
    public float deferredRotationScale = 1.0f;
    @XmlElement(name="m_Looped")
    public boolean isLooped = true;
    @XmlElement(name="m_BlendTime")
    public float blendTime;
    @XmlElement(name="m_BlendOutTime")
    public float blendOutTime = -1.0f;
    @XmlElement(name="m_BlendCurve")
    public InterpolatorSlot blendCurve;
    @XmlElement(name="m_StopAnimOnExit")
    public boolean stopAnimOnExit;
    @XmlElement(name="m_EarlyTransitionOut")
    public boolean earlyTransitionOut;
    @XmlElement(name="m_SpeedScale")
    public String speedScale = "1.00";
    @XmlElement(name="m_SpeedScaleVariable")
    public String speedScaleVariable;
    @XmlElement(name="m_SpeedScaleRandomMultiplierMin")
    public float speedScaleRandomMultiplierMin = 1.0f;
    @XmlElement(name="m_SpeedScaleRandomMultiplierMax")
    public float speedScaleRandomMultiplierMax = 1.0f;
    @XmlTransient
    private float speedScaleF = Float.POSITIVE_INFINITY;
    @XmlElement(name="m_randomAdvanceFraction")
    public float randomAdvanceFraction;
    @XmlElement(name="m_maxTorsoTwist")
    public float maxTorsoTwist = 15.0f;
    @XmlElement(name="m_Scalar")
    public String scalar = "";
    @XmlElement(name="m_Scalar2")
    public String scalar2 = "";
    @XmlElement(name="m_AnimReverse")
    public boolean isAnimReverse;
    @XmlElement(name="m_SyncTrackingEnabled")
    public boolean syncTrackingEnabled = true;
    @XmlElement(name="m_TrackTimeToVariable")
    public String trackTimeToVariable;
    @XmlElement(name="m_2DBlends")
    public List<Anim2DBlend> blends2d = new ArrayList<Anim2DBlend>();
    @XmlElement(name="m_Conditions")
    public AnimCondition[] conditions = new AnimCondition[0];
    @XmlElement(name="m_Events")
    public List<AnimEvent> events = new ArrayList<AnimEvent>();
    @XmlElement(name="m_2DBlendTri")
    public List<Anim2DBlendTriangle> blendTris = new ArrayList<Anim2DBlendTriangle>();
    @XmlElement(name="m_Transitions")
    public List<AnimTransition> transitions = new ArrayList<AnimTransition>();
    @XmlElement(name="m_SubStateBoneWeights")
    public List<AnimBoneWeight> subStateBoneWeights = new ArrayList<AnimBoneWeight>();
    @XmlTransient
    public Anim2DBlendPicker blend2dPicker;
    @XmlTransient
    public AnimState parentState;
    @XmlTransient
    private AnimTransition transitionOut;

    public static AnimNode Parse(String source2) {
        try {
            AnimNode parsedNode = PZXmlUtil.parse(AnimNode.class, source2);
            parsedNode.isIdleAnim = parsedNode.name.contains("Idle");
            if (!parsedNode.blendTris.isEmpty()) {
                parsedNode.blend2dPicker = new Anim2DBlendPicker();
                parsedNode.blend2dPicker.SetPickTriangles(parsedNode.blendTris);
            }
            if (parsedNode.isRagdoll()) {
                AnimCondition canRagdollCondition = new AnimCondition();
                canRagdollCondition.type = AnimCondition.Type.BOOL;
                canRagdollCondition.boolValue = true;
                canRagdollCondition.name = "canRagdoll";
                parsedNode.conditions = (AnimCondition[])PZArrayUtil.add(parsedNode.conditions, canRagdollCondition);
            }
            for (AnimCondition condition : parsedNode.conditions) {
                condition.parse(parsedNode, null);
            }
            PZArrayUtil.forEachReplace(parsedNode.events, event -> {
                if ("SetVariable".equalsIgnoreCase(event.eventName)) {
                    return new AnimEventSetVariable((AnimEvent)event);
                }
                if ("FlagWhileAlive".equalsIgnoreCase(event.eventName)) {
                    return new AnimEventFlagWhileAlive((AnimEvent)event);
                }
                return event;
            });
            parsedNode.events.sort(eventsComparator);
            for (AnimEvent evt : parsedNode.events) {
                evt.parentAnimNode = parsedNode;
            }
            try {
                parsedNode.speedScaleF = Float.parseFloat(parsedNode.speedScale);
            }
            catch (NumberFormatException ex) {
                parsedNode.speedScaleVariable = parsedNode.speedScale;
            }
            if (parsedNode.subStateBoneWeights.isEmpty()) {
                parsedNode.subStateBoneWeights.add(new AnimBoneWeight("Bip01_Spine1", 0.5f));
                parsedNode.subStateBoneWeights.add(new AnimBoneWeight("Bip01_Neck", 1.0f));
                parsedNode.subStateBoneWeights.add(new AnimBoneWeight("Bip01_BackPack", 1.0f));
                parsedNode.subStateBoneWeights.add(new AnimBoneWeight("Bip01_Prop1", 1.0f));
                parsedNode.subStateBoneWeights.add(new AnimBoneWeight("Bip01_Prop2", 1.0f));
            }
            for (int i = 0; i < parsedNode.subStateBoneWeights.size(); ++i) {
                AnimBoneWeight animBoneWeight = parsedNode.subStateBoneWeights.get(i);
                animBoneWeight.boneName = JAssImpImporter.getSharedString(animBoneWeight.boneName, "AnimBoneWeight.boneName");
            }
            parsedNode.transitionOut = null;
            for (AnimTransition transition : parsedNode.transitions) {
                if (!StringUtils.isNullOrWhitespace(transition.target)) continue;
                parsedNode.transitionOut = transition;
            }
            return parsedNode;
        }
        catch (PZXmlParserException e) {
            System.err.println("AnimNode.Parse threw an exception reading file: " + source2);
            ExceptionLogger.logException(e);
            return null;
        }
    }

    public boolean checkConditions(IAnimationVariableSource varSource) {
        return AnimCondition.pass(varSource, this.conditions);
    }

    public float getSpeedScale(IAnimationVariableSource varSource) {
        if (this.speedScaleF != Float.POSITIVE_INFINITY) {
            return this.speedScaleF;
        }
        return varSource.getVariableFloat(this.speedScale, 1.0f);
    }

    public IInterpolator getBlendCurve() {
        if (this.blendCurve != null) {
            return this.blendCurve.interpolator;
        }
        return null;
    }

    public boolean isIdleAnim() {
        return this.isIdleAnim;
    }

    public AnimTransition findTransitionTo(IAnimationVariableSource varSource, AnimNode toNode) {
        DebugType.AnimationDetailed.debugln("  FindingTransition: <%s|%s>", this.animName, toNode.name);
        AnimTransition found = null;
        int count = this.transitions.size();
        for (int i = 0; i < count; ++i) {
            AnimTransition trans = this.transitions.get(i);
            DebugType.AnimationDetailed.debugln("    Transitions: <%s>", trans.target);
            if (!StringUtils.equalsIgnoreCase(trans.target, toNode.name)) continue;
            trans.parse(this, toNode);
            if (!AnimCondition.pass(varSource, trans.conditions)) continue;
            DebugType.AnimationDetailed.debugln("    *** Matched Transition: <%s> From Node <%s>", trans.target, this.animName);
            found = trans;
            break;
        }
        return found;
    }

    public String toString() {
        return String.format("AnimNode{ Name: %s, AnimName: %s, Conditions: %s }", this.name, this.animName, this.getConditionsString());
    }

    public String getConditionsString() {
        return PZArrayUtil.arrayToString(this.conditions, AnimCondition::getConditionString, "( ", " )", ", ");
    }

    public boolean isAbstract() {
        if (this.isRagdoll()) {
            return false;
        }
        if (!StringUtils.isNullOrWhitespace(this.animName)) {
            return false;
        }
        if (!this.alternateAnims.isEmpty()) {
            return false;
        }
        return this.blends2d.isEmpty();
    }

    public float getBlendOutTime() {
        if (this.transitionOut != null) {
            return this.transitionOut.blendOutTime;
        }
        if (this.blendOutTime >= 0.0f) {
            return this.blendOutTime;
        }
        return this.blendTime;
    }

    public String getDeferredBoneName() {
        if (StringUtils.isNullOrWhitespace(this.deferredBoneName)) {
            return "Translation_Data";
        }
        return this.deferredBoneName;
    }

    public BoneAxis getDeferredBoneAxis() {
        return this.deferredBoneAxis;
    }

    public int getPriority() {
        return this.priority;
    }

    public int compareSelectionConditions(AnimNode node) {
        return AnimNode.compareSelectionConditions(this, node);
    }

    public static int compareSelectionConditions(AnimNode a, AnimNode b) {
        if (a.isAbstract() != b.isAbstract()) {
            if (a.isAbstract()) {
                return -1;
            }
            return 1;
        }
        if (a.conditionPriority < b.conditionPriority) {
            return -1;
        }
        if (a.conditionPriority > b.conditionPriority) {
            return 1;
        }
        if (a.conditions.length < b.conditions.length) {
            return -1;
        }
        if (a.conditions.length > b.conditions.length) {
            return 1;
        }
        return 0;
    }

    public String getMatchingGrappledAnimNode() {
        return this.matchingGrappledAnimNode;
    }

    public boolean isGrappler() {
        return !StringUtils.isNullOrWhitespace(this.matchingGrappledAnimNode);
    }

    public boolean isRagdoll() {
        return this.isRagdoll;
    }

    public float getRagdollMaxTime() {
        return this.ragdollMaxTime;
    }

    public String getRandomAnim() {
        boolean bEmptyName = StringUtils.isNullOrEmpty(this.animName);
        if (bEmptyName && this.alternateAnims.isEmpty()) {
            DebugType.Animation.error("animNames is empty! isAbstract() = %s", this.isAbstract() ? "true" : "false");
            return "";
        }
        if (bEmptyName) {
            return PZArrayUtil.pickRandom(this.alternateAnims);
        }
        if (this.alternateAnims.isEmpty()) {
            return this.animName;
        }
        int idx = Rand.Next(1 + this.alternateAnims.size());
        return idx == 0 ? this.animName : PZArrayUtil.pickRandom(this.alternateAnims);
    }
}

