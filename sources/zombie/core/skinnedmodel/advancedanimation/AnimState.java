/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetPath;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAsset;
import zombie.core.skinnedmodel.advancedanimation.AnimNodeAssetManager;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSource;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class AnimState {
    public String name = "";
    public final List<AnimNode> nodes = new ArrayList<AnimNode>();
    public final List<AnimNode> abstractNodes = new ArrayList<AnimNode>();
    public int defaultIndex;
    public AnimationSet set;

    public List<AnimNode> getAnimNodes(IAnimationVariableSource varSource, List<AnimNode> nodes) {
        nodes.clear();
        if (this.nodes.size() <= 0) {
            return nodes;
        }
        if (DebugOptions.instance.animation.animLayer.allowAnimNodeOverride.getValue() && varSource.getVariableBoolean("dbgForceAnim") && varSource.isVariable("dbgForceAnimStateName", this.name)) {
            String dbgForceAnimNodeName = varSource.getVariableString("dbgForceAnimNodeName");
            int nodeCount = this.nodes.size();
            for (int anIdx = 0; anIdx < nodeCount; ++anIdx) {
                AnimNode node2 = this.nodes.get(anIdx);
                if (!StringUtils.equalsIgnoreCase(node2.name, dbgForceAnimNodeName)) continue;
                nodes.add(node2);
                break;
            }
            return nodes;
        }
        AnimNode bestNode = null;
        int nodeCount = this.nodes.size();
        for (int i = 0; i < nodeCount; ++i) {
            AnimNode node3 = this.nodes.get(i);
            if (bestNode != null && bestNode.compareSelectionConditions(node3) > 0) break;
            if (!node3.checkConditions(varSource)) continue;
            bestNode = node3;
            nodes.add(node3);
        }
        if (!nodes.isEmpty() && DebugOptions.instance.animation.animLayer.logNodeConditions.getValue()) {
            DebugType.Animation.debugln("%s Nodes passed: %s", this.set.name, PZArrayUtil.arrayToString(nodes, node -> String.format("%s: %s", node.name, node.getConditionsString()), "{ ", " }", "; "));
        }
        return nodes;
    }

    public static AnimState Parse(String name, String statePath) {
        String[] listOfNodeFiles;
        AnimState state = new AnimState();
        state.name = name;
        DebugType.Animation.debugln("Loading AnimState: %s", name);
        for (String nodeFileName : listOfNodeFiles = ZomboidFileSystem.instance.resolveAllFiles(statePath, file -> file.getName().endsWith(".xml"), true)) {
            File nodeFile = new File(nodeFileName);
            String nodeName = nodeFile.getName().split(".xml")[0].toLowerCase();
            DebugType.Animation.debugln("%s -> AnimNode: %s", name, nodeName);
            String absolutePath = ZomboidFileSystem.instance.resolveFileOrGUID(nodeFileName);
            AnimNodeAsset asset = (AnimNodeAsset)AnimNodeAssetManager.instance.load(new AssetPath(absolutePath));
            if (!asset.isReady()) continue;
            AnimNode newNode = asset.animNode;
            newNode.parentState = state;
            state.addNode(newNode);
        }
        return state;
    }

    public void addNode(AnimNode newNode) {
        if (newNode.isAbstract()) {
            this.abstractNodes.add(newNode);
            return;
        }
        int insertAt = this.nodes.size();
        for (int i = 0; i < this.nodes.size(); ++i) {
            AnimNode node = this.nodes.get(i);
            if (newNode.compareSelectionConditions(node) <= 0) continue;
            insertAt = i;
            break;
        }
        this.nodes.add(insertAt, newNode);
    }

    public String toString() {
        return "AnimState{" + this.name + ", NodeCount:" + this.nodes.size() + ", AbstractNodeCount:" + this.abstractNodes.size() + ", DefaultIndex:" + this.defaultIndex + "}";
    }

    public static String getStateName(AnimState state) {
        return state != null ? state.name : null;
    }

    protected void clear() {
        this.nodes.clear();
        this.abstractNodes.clear();
        this.set = null;
    }
}

