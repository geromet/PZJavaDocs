/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import zombie.DebugFileWatcher;
import zombie.GameProfiler;
import zombie.Lua.LuaManager;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.characters.CharacterActionAnims;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimCondition;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimTransition;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableWhileAliveFlagsContainer;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.advancedanimation.SubLayerSlot;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventCallback;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.utils.TransitionNodeProxy;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.Pool;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

public final class AdvancedAnimator
implements IAnimEventCallback {
    private IAnimatable character;
    public AnimationSet animSet;
    public final ArrayList<IAnimEventCallback> animCallbackHandlers = new ArrayList();
    private AnimLayer rootLayer;
    private final List<SubLayerSlot> subLayers = new ArrayList<SubLayerSlot>();
    private AnimState rootState;
    private final List<AnimState> subStates = new ArrayList<AnimState>();
    public static float motionScale = 0.76f;
    public static float rotationScale = 0.76f;
    private static AnimatorDebugMonitor debugMonitor;
    private static long animSetModificationTime;
    private static long actionGroupModificationTime;
    private final AnimationVariableWhileAliveFlagsContainer setFlagCounters = new AnimationVariableWhileAliveFlagsContainer();
    private final TransitionNodeProxy transitionNodeProxy = new TransitionNodeProxy();

    public static void systemInit() {
        DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/AnimSets", AdvancedAnimator::isAnimSetFilePath, AdvancedAnimator::onAnimSetsRefreshTriggered));
        DebugFileWatcher.instance.add(new PredicatedFileWatcher("media/actiongroups", AdvancedAnimator::isActionGroupFilePath, AdvancedAnimator::onActionGroupsRefreshTriggered));
        AdvancedAnimator.LoadDefaults();
    }

    private static boolean isAnimSetFilePath(String path) {
        if (path == null) {
            return false;
        }
        if (!path.endsWith(".xml")) {
            return false;
        }
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (int i = 0; i < modIDs.size(); ++i) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modID);
            if (mod != null && mod.animSetsFile != null && mod.animSetsFile.common.canonicalFile != null && path.startsWith(mod.animSetsFile.common.canonicalFile.getPath())) {
                return true;
            }
            if (mod == null || mod.animSetsFile == null || mod.animSetsFile.version.canonicalFile == null || !path.startsWith(mod.animSetsFile.version.canonicalFile.getPath())) continue;
            return true;
        }
        String animSetsPath = ZomboidFileSystem.instance.getAnimSetsPath();
        return path.startsWith(animSetsPath);
    }

    private static boolean isActionGroupFilePath(String path) {
        if (path == null) {
            return false;
        }
        if (!path.endsWith(".xml")) {
            return false;
        }
        ArrayList<String> modIDs = ZomboidFileSystem.instance.getModIDs();
        for (int i = 0; i < modIDs.size(); ++i) {
            String modID = modIDs.get(i);
            ChooseGameInfo.Mod mod = ChooseGameInfo.getModDetails(modID);
            if (mod != null && mod.actionGroupsFile != null && mod.actionGroupsFile.common.canonicalFile != null && path.startsWith(mod.actionGroupsFile.common.canonicalFile.getPath())) {
                return true;
            }
            if (mod == null || mod.actionGroupsFile == null || mod.actionGroupsFile.version.canonicalFile == null || !path.startsWith(mod.actionGroupsFile.version.canonicalFile.getPath())) continue;
            return true;
        }
        String actionGroupsPath = ZomboidFileSystem.instance.getActionGroupsPath();
        return path.startsWith(actionGroupsPath);
    }

    private static void onActionGroupsRefreshTriggered(String entryKey) {
        DebugLog.General.println("DebugFileWatcher Hit. ActionGroups: " + entryKey);
        actionGroupModificationTime = System.currentTimeMillis() + 1000L;
    }

    private static void onAnimSetsRefreshTriggered(String entryKey) {
        DebugLog.General.println("DebugFileWatcher Hit. AnimSets: " + entryKey);
        animSetModificationTime = System.currentTimeMillis() + 1000L;
    }

    public static void checkModifiedFiles() {
        if (animSetModificationTime != -1L && animSetModificationTime < System.currentTimeMillis()) {
            DebugLog.General.println("Refreshing AnimSets.");
            animSetModificationTime = -1L;
            AdvancedAnimator.LoadDefaults();
            LuaManager.GlobalObject.refreshAnimSets(true);
        }
        if (actionGroupModificationTime != -1L && actionGroupModificationTime < System.currentTimeMillis()) {
            DebugLog.General.println("Refreshing action groups.");
            actionGroupModificationTime = -1L;
            LuaManager.GlobalObject.reloadActionGroups();
        }
    }

    private static void LoadDefaults() {
        try {
            Element rootXml = PZXmlUtil.parseXml("media/AnimSets/Defaults.xml");
            String mx = rootXml.getElementsByTagName("MotionScale").item(0).getTextContent();
            motionScale = Float.parseFloat(mx);
            String r = rootXml.getElementsByTagName("RotationScale").item(0).getTextContent();
            rotationScale = Float.parseFloat(r);
        }
        catch (PZXmlParserException e) {
            DebugLog.General.error("Exception thrown: " + String.valueOf(e));
            e.printStackTrace();
        }
    }

    public String GetDebug() {
        StringBuilder debug = new StringBuilder();
        debug.append("GameState: ");
        if (this.character instanceof IsoGameCharacter) {
            IsoGameCharacter character = (IsoGameCharacter)this.character;
            debug.append(character.getCurrentState() == null ? "null" : character.getCurrentState().getClass().getSimpleName()).append("\n");
        }
        if (this.rootLayer != null) {
            debug.append("Layer: ").append(0).append("\n");
            debug.append(this.rootLayer.GetDebugString()).append("\n");
        }
        for (int i = 0; i < this.subLayers.size(); ++i) {
            SubLayerSlot slot = this.subLayers.get(i);
            debug.append("SubLayer: ").append(i).append("\n");
            debug.append(slot.animLayer.GetDebugString()).append("\n");
        }
        debug.append("Variables:\n");
        debug.append("Weapon: ").append(this.character.getVariableString("weapon")).append("\n");
        debug.append("Aim: ").append(this.character.getVariableString("aim")).append("\n");
        ArrayList<IAnimationVariableSlot> sorted2 = new ArrayList<IAnimationVariableSlot>();
        for (IAnimationVariableSlot entry : this.character.getGameVariables()) {
            sorted2.add(entry);
        }
        sorted2.sort(Comparator.comparing(IAnimationVariableSlot::getKey));
        for (IAnimationVariableSlot entry : sorted2) {
            debug.append("  ").append(entry.getKey()).append(" : ").append(entry.getValueString()).append("\n");
        }
        sorted2.clear();
        return debug.toString();
    }

    public void OnAnimDataChanged(boolean reload) {
        if (reload && this.character instanceof IsoGameCharacter) {
            IsoGameCharacter character = (IsoGameCharacter)this.character;
            ++character.getStateMachine().activeStateChanged;
            character.setDefaultState();
            if (character instanceof IsoZombie) {
                character.setOnFloor(false);
            }
            --character.getStateMachine().activeStateChanged;
        }
        this.setAnimSet(AnimationSet.GetAnimationSet(this.character.GetAnimSetName(), false));
        if (this.character.getAnimationPlayer() != null) {
            this.character.getAnimationPlayer().reset();
        }
        if (this.rootLayer != null) {
            this.rootLayer.reset();
        }
        for (int i = 0; i < this.subLayers.size(); ++i) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.reset();
        }
    }

    public void reset() {
        if (this.rootLayer != null) {
            this.rootLayer.reset();
        }
        for (int i = 0; i < this.subLayers.size(); ++i) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.reset();
        }
    }

    public void Reload() {
    }

    public void init(IAnimatable character) {
        this.character = character;
        this.rootLayer = AnimLayer.alloc(character, this);
    }

    public void setAnimSet(AnimationSet aset) {
        this.animSet = aset;
    }

    @Override
    public void OnAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        this.invokeAnimEvent(sender, track, event);
    }

    private void invokeAnimEvent(AnimLayer sender, AnimationTrack track, AnimEvent event) {
        for (int i = 0; i < this.animCallbackHandlers.size(); ++i) {
            IAnimEventCallback callback = this.animCallbackHandlers.get(i);
            callback.OnAnimEvent(sender, track, event);
        }
    }

    public void invokeGlobalAnimEvent(AnimEvent event) {
        if (this.isRecording()) {
            this.logGlobalAnimEvent(event);
        }
        this.invokeAnimEvent(null, null, event);
    }

    private void logGlobalAnimEvent(AnimEvent evt) {
        AnimationPlayerRecorder recorder = this.character.getAnimationRecorder();
        recorder.logGlobalAnimEvent(evt);
    }

    public String getCurrentStateName() {
        return this.rootLayer == null ? null : this.rootLayer.getCurrentStateName();
    }

    public boolean containsState(String stateName) {
        return this.animSet != null && this.animSet.containsState(stateName);
    }

    private SubLayerSlot findSubLayerWithState(List<SubLayerSlot> subLayers, AnimState state) {
        SubLayerSlot foundSubLayerSlot = null;
        int subStateCount = subLayers.size();
        for (int i = 0; i < subStateCount; ++i) {
            SubLayerSlot subLayerSlot = subLayers.get(i);
            if (!subLayerSlot.isNextOrCurrentState(state)) continue;
            foundSubLayerSlot = subLayerSlot;
            break;
        }
        return foundSubLayerSlot;
    }

    private SubLayerSlot findInactiveSubLayerWithDesiredLayer(List<SubLayerSlot> subLayers, int desiredLayer) {
        SubLayerSlot foundSubLayerSlot = null;
        for (int i = subLayers.size() - 1; i >= 0; --i) {
            SubLayerSlot subLayerSlot = subLayers.get(i);
            if (subLayerSlot.shouldBeActive || subLayerSlot.desiredLayer != desiredLayer) continue;
            foundSubLayerSlot = subLayerSlot;
            break;
        }
        return foundSubLayerSlot;
    }

    public void setState(String stateName) {
        this.setState(stateName, PZArrayList.emptyList());
    }

    public void setState(String stateName, List<String> subStateNames) {
        if (this.animSet == null) {
            DebugType.Animation.error("(%s) Cannot set state. AnimSet is null.", stateName);
            return;
        }
        this.rootState = this.animSet.GetState(stateName);
        PZArrayUtil.copy(this.subStates, subStateNames, this.animSet::GetState);
        this.setState(this.rootState, this.subStates);
    }

    public void setState(AnimState rootState, List<AnimState> subStates) {
        int iSubStateIdx;
        if (this.isCurrentState(rootState, subStates)) {
            return;
        }
        if (!this.rootLayer.isCurrentState(rootState)) {
            this.rootLayer.transitionTo(rootState);
        }
        this.cleanUpEmptyLayers(this.subLayers);
        PZArrayUtil.forEach(this.subLayers, SubLayerSlot::clearShouldBeActiveFlag);
        for (iSubStateIdx = 0; iSubStateIdx < subStates.size(); ++iSubStateIdx) {
            SubLayerSlot existingSlot = this.findSubLayerWithState(this.subLayers, subStates.get(iSubStateIdx));
            if (existingSlot == null) continue;
            existingSlot.shouldBeActive = true;
            existingSlot.desiredLayer = iSubStateIdx;
        }
        for (iSubStateIdx = 0; iSubStateIdx < subStates.size(); ++iSubStateIdx) {
            AnimState subState2 = subStates.get(iSubStateIdx);
            SubLayerSlot existingSlot = this.findSubLayerWithState(this.subLayers, subState2);
            if (existingSlot != null) continue;
            SubLayerSlot existingInactiveSlot = this.findInactiveSubLayerWithDesiredLayer(this.subLayers, iSubStateIdx);
            if (existingInactiveSlot != null) {
                existingInactiveSlot.setNextTransitionTo(subState2);
                existingInactiveSlot.shouldBeActive = true;
                continue;
            }
            SubLayerSlot newSlot = SubLayerSlot.alloc(this.character, this);
            newSlot.setNextTransitionTo(subState2);
            newSlot.shouldBeActive = true;
            newSlot.desiredLayer = iSubStateIdx;
            this.subLayers.add(newSlot);
        }
        PZArrayUtil.sort(this.subLayers, SubLayerSlot::compare);
        this.ensureLayerParents(this.subLayers);
        PZArrayUtil.forEach(this.subLayers, SubLayerSlot::applyNextTransition);
        this.cleanUpEmptyLayers(this.subLayers);
        if (DebugType.AnimationLayers.isEnabled(LogSeverity.Debug)) {
            DebugType.AnimationLayers.debugln("States");
            DebugType.AnimationLayers.debugln("+++ rootState: %s", rootState.name);
            PZArrayUtil.forEach(subStates, subState -> DebugType.AnimationLayers.debugln("+++ subState: %s", subState.name));
            DebugType.AnimationLayers.debugln("------------------------------------------------");
            DebugType.AnimationLayers.debugln("Layers");
            DebugType.AnimationLayers.debugln("*** rootLayer: %s", this.rootLayer.getCurrentStateName());
            PZArrayUtil.forEach(this.subLayers, subSlot -> DebugType.AnimationLayers.debugln("*** Layer %d. DesiredLayer: %d State: %s ActiveNodes: %d", subSlot.animLayer.getDepth(), subSlot.desiredLayer, subSlot.animLayer.getCurrentStateName(), subSlot.animLayer.getLiveAnimNodes().size()));
            DebugType.AnimationLayers.debugln("------------------------------------------------");
        }
    }

    private boolean isCurrentState(AnimState rootState, List<AnimState> subStates) {
        if (!this.rootLayer.isCurrentState(rootState)) {
            return false;
        }
        int iMaxDesiredLayer = 0;
        for (int iLayerSlotIdx = 0; iLayerSlotIdx < this.subLayers.size(); ++iLayerSlotIdx) {
            SubLayerSlot existingSlot = this.subLayers.get(iLayerSlotIdx);
            if (!existingSlot.shouldBeActive) continue;
            if (existingSlot.desiredLayer >= subStates.size()) {
                return false;
            }
            if (!existingSlot.isNextOrCurrentState(subStates.get(existingSlot.desiredLayer))) {
                return false;
            }
            iMaxDesiredLayer = PZMath.max(existingSlot.desiredLayer, iMaxDesiredLayer);
        }
        return iMaxDesiredLayer == subStates.size();
    }

    private void ensureLayerParents(List<SubLayerSlot> subLayerSlots) {
        AnimLayer parentLayer = this.rootLayer;
        for (int isSubState = 0; isSubState < subLayerSlots.size(); ++isSubState) {
            SubLayerSlot subLayerSlot = subLayerSlots.get(isSubState);
            subLayerSlot.setParentLayer(parentLayer);
            parentLayer = subLayerSlot.animLayer;
        }
    }

    public void update(float deltaT) {
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("AdvancedAnimator.Update");){
            this.updateInternal(deltaT);
        }
    }

    private void updateInternal(float deltaT) {
        Object subLayer;
        int i;
        if (this.character.getAnimationPlayer() == null) {
            return;
        }
        if (!this.character.getAnimationPlayer().isReady()) {
            return;
        }
        if (this.animSet == null) {
            return;
        }
        if (!this.rootLayer.hasState()) {
            this.rootLayer.transitionTo(this.animSet.GetState("Idle"), true);
        }
        this.rootLayer.updateLiveAnimNodes();
        for (i = 0; i < this.subLayers.size(); ++i) {
            subLayer = this.subLayers.get(i);
            ((SubLayerSlot)subLayer).animLayer.updateLiveAnimNodes();
        }
        this.GenerateTransitionData();
        this.rootLayer.Update(deltaT);
        for (i = 0; i < this.subLayers.size(); ++i) {
            subLayer = this.subLayers.get(i);
            ((SubLayerSlot)subLayer).update(deltaT);
        }
        this.cleanUpEmptyLayers(this.subLayers);
        if (debugMonitor != null && (subLayer = this.character) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)subLayer;
            if (debugMonitor.getTarget() != this.character) {
                return;
            }
            int count = 1 + this.subLayers.size();
            AnimLayer[] layers = new AnimLayer[count];
            layers[0] = this.rootLayer;
            for (int i2 = 0; i2 < this.subLayers.size(); ++i2) {
                SubLayerSlot subLayer2 = this.subLayers.get(i2);
                layers[1 + i2] = subLayer2.animLayer;
            }
            debugMonitor.update(isoGameCharacter, layers);
        }
    }

    private void cleanUpEmptyLayers(List<SubLayerSlot> subLayers) {
        for (int i = 0; i < subLayers.size(); ++i) {
            SubLayerSlot subLayer = subLayers.get(i);
            if (subLayer.shouldBeActive || !subLayer.animLayer.isStateless() || subLayer.hasRunningAnims()) continue;
            subLayers.remove(i--);
            Pool.tryRelease(subLayer);
        }
        this.ensureLayerParents(subLayers);
    }

    private void GenerateTransitionData() {
        int i;
        TransitionNodeProxy proxy = this.transitionNodeProxy;
        proxy.reset();
        this.rootLayer.FindTransitioningLiveAnimNode(proxy, true);
        for (i = 0; i < this.subLayers.size(); ++i) {
            SubLayerSlot subLayer = this.subLayers.get(i);
            subLayer.animLayer.FindTransitioningLiveAnimNode(proxy, false);
        }
        if (!proxy.allNewNodes.isEmpty() || !proxy.allOutgoingNodes.isEmpty()) {
            DebugType.AnimationDetailed.debugln("************* New Nodes *************");
            for (i = 0; i < proxy.allNewNodes.size(); ++i) {
                DebugType.AnimationDetailed.debugln("  %s", proxy.allNewNodes.get((int)i).liveAnimNode.getName());
            }
            DebugType.AnimationDetailed.debugln("************* Out Nodes *************");
            for (i = 0; i < proxy.allOutgoingNodes.size(); ++i) {
                DebugType.AnimationDetailed.debugln("  %s", proxy.allOutgoingNodes.get((int)i).liveAnimNode.getName());
            }
            DebugType.AnimationDetailed.debugln("*************************************");
        }
        if (!proxy.HasAnyPossibleTransitions().booleanValue()) {
            return;
        }
        this.FindTransitionsFromProxy(proxy);
        this.ProcessTransitions(proxy);
    }

    public void FindTransitionsFromProxy(TransitionNodeProxy proxy) {
        for (int i = 0; i < proxy.allNewNodes.size(); ++i) {
            TransitionNodeProxy.NodeLayerPair toNodePair = proxy.allNewNodes.get(i);
            AnimNode toNode = toNodePair.liveAnimNode.getSourceNode();
            boolean j = false;
            while (i < proxy.allOutgoingNodes.size()) {
                AnimTransition animTransition;
                TransitionNodeProxy.NodeLayerPair fromNodePair = proxy.allOutgoingNodes.get(i);
                if (toNode != fromNodePair.liveAnimNode.getSourceNode() && (animTransition = fromNodePair.liveAnimNode.findTransitionTo(this.character, toNodePair.liveAnimNode.getSourceNode())) != null) {
                    TransitionNodeProxy.TransitionNodeProxyData transitionData = proxy.allocTransitionNodeProxyData();
                    transitionData.animLayerIn = toNodePair.animLayer;
                    transitionData.newAnimNode = toNodePair.liveAnimNode;
                    transitionData.animLayerOut = fromNodePair.animLayer;
                    transitionData.oldAnimNode = fromNodePair.liveAnimNode;
                    transitionData.transitionOut = animTransition;
                    proxy.foundTransitions.add(transitionData);
                    DebugType.AnimationDetailed.debugln("** NEW ** Anim: <%s>; <%s>; this: <%s>", transitionData.newAnimNode.getName(), transitionData.transitionOut != null ? "true" : "false", this.toString());
                }
                ++i;
            }
        }
    }

    public void ProcessTransitions(TransitionNodeProxy proxy) {
        for (int i = 0; i < proxy.foundTransitions.size(); ++i) {
            TransitionNodeProxy.TransitionNodeProxyData transition = proxy.foundTransitions.get(i);
            AnimationTrack transitionTrack = transition.animLayerOut.startTransitionAnimation(transition);
            transition.newAnimNode.startTransitionIn(transition.oldAnimNode, transition.transitionOut, transitionTrack);
            transition.oldAnimNode.setTransitionOut(transition.transitionOut);
        }
    }

    public void render() {
        if (this.character.getAnimationPlayer() == null) {
            return;
        }
        if (!this.character.getAnimationPlayer().isReady()) {
            return;
        }
        if (this.animSet == null) {
            return;
        }
        if (!this.rootLayer.hasState()) {
            return;
        }
        this.rootLayer.render();
    }

    public void printDebugCharacterActions(String target) {
        AnimState state;
        if (this.animSet != null && (state = this.animSet.GetState("actions")) != null) {
            boolean targFound = false;
            for (CharacterActionAnims act : CharacterActionAnims.values()) {
                boolean isTarg = act == CharacterActionAnims.None;
                String actname = isTarg ? target : act.toString();
                boolean found = false;
                for (AnimNode node : state.nodes) {
                    for (AnimCondition con : node.conditions) {
                        if (con.type != AnimCondition.Type.STRING || !con.name.equalsIgnoreCase("performingaction") || !con.stringValue.equalsIgnoreCase(actname)) continue;
                        found = true;
                        break;
                    }
                    if (!found) continue;
                    break;
                }
                if (found) {
                    if (!isTarg) continue;
                    targFound = true;
                    continue;
                }
                DebugType.General.warn("WARNING: did not find node with condition 'PerformingAction = %s' in player/actions/", actname);
            }
            if (targFound) {
                DebugType.Animation.debugln("SUCCESS - Current 'actions' TargetNode: '%s' was found.", target);
            } else {
                DebugType.Animation.debugln("FAIL - Current 'actions' TargetNode: '%s' not found.", target);
            }
        }
    }

    public ArrayList<String> debugGetVariables() {
        ArrayList<String> vars = new ArrayList<String>();
        if (this.animSet != null) {
            for (Map.Entry<String, AnimState> entry : this.animSet.states.entrySet()) {
                AnimState state = entry.getValue();
                for (AnimNode node : state.nodes) {
                    for (AnimCondition con : node.conditions) {
                        if (con.name == null || vars.contains(con.name.toLowerCase())) continue;
                        vars.add(con.name.toLowerCase());
                    }
                }
            }
        }
        return vars;
    }

    public AnimatorDebugMonitor getDebugMonitor() {
        return debugMonitor;
    }

    public void setDebugMonitor(AnimatorDebugMonitor monitor) {
        debugMonitor = monitor;
    }

    public IAnimatable getCharacter() {
        return this.character;
    }

    public void updateSpeedScale(String variable, float newSpeed) {
        if (this.rootLayer != null) {
            List<LiveAnimNode> liveAnimNodes = this.rootLayer.getLiveAnimNodes();
            for (int i = 0; i < liveAnimNodes.size(); ++i) {
                LiveAnimNode node = liveAnimNodes.get(i);
                if (!node.isActive() || node.getSourceNode() == null || !variable.equals(node.getSourceNode().speedScaleVariable)) continue;
                node.getSourceNode().speedScale = "" + newSpeed;
                for (int j = 0; j < node.getMainAnimationTracksCount(); ++j) {
                    node.getMainAnimationTrackAt(j).setSpeedDelta(newSpeed);
                }
            }
        }
    }

    public boolean containsAnyIdleNodes() {
        if (this.rootLayer == null) {
            return false;
        }
        boolean isIdle = false;
        List<LiveAnimNode> liveAnimNodes = this.rootLayer.getLiveAnimNodes();
        for (int i = 0; i < liveAnimNodes.size() && !isIdle; ++i) {
            isIdle = liveAnimNodes.get(i).isIdleAnimActive();
        }
        for (int j = 0; j < this.getSubLayerCount(); ++j) {
            AnimLayer subLayer = this.getSubLayerAt(j);
            liveAnimNodes = subLayer.getLiveAnimNodes();
            for (int i = 0; i < liveAnimNodes.size() && (isIdle = liveAnimNodes.get(i).isIdleAnimActive()); ++i) {
            }
        }
        return isIdle;
    }

    public AnimLayer getRootLayer() {
        return this.rootLayer;
    }

    public int getSubLayerCount() {
        return this.subLayers.size();
    }

    public AnimLayer getSubLayerAt(int idx) {
        return this.subLayers.get((int)idx).animLayer;
    }

    public boolean isRecording() {
        return this.character != null && this.character.isAnimationRecorderActive();
    }

    public void incrementWhileAliveFlag(AnimationVariableReference variableReference, boolean whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.incrementWhileAliveFlag(variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", variableReference, stillAliveCounter);
        variableReference.setVariable((IAnimationVariableSource)this.getCharacter(), stillAliveCounter > 0 ? whileAliveValue : !whileAliveValue);
    }

    public void decrementWhileAliveFlag(AnimationVariableReference variableReference, boolean whileAliveValue) {
        int stillAliveCounter = this.setFlagCounters.decrementWhileAliveFlag(variableReference);
        DebugType.Animation.trace("Variable: %s. Count: %d", variableReference, stillAliveCounter);
        variableReference.setVariable((IAnimationVariableSource)this.getCharacter(), stillAliveCounter > 0 ? whileAliveValue : !whileAliveValue);
    }

    static {
        animSetModificationTime = -1L;
        actionGroupModificationTime = -1L;
    }
}

