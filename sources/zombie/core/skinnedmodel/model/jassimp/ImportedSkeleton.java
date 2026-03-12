/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model.jassimp;

import gnu.trove.list.array.TFloatArrayList;
import jassimp.AiAnimation;
import jassimp.AiBone;
import jassimp.AiBuiltInWrapperProvider;
import jassimp.AiMatrix4f;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiNodeAnim;
import jassimp.AiQuaternion;
import jassimp.AiScene;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.skinnedmodel.HelperFunctions;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.jassimp.ImportedSkeletonParams;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class ImportedSkeleton {
    final HashMap<String, Integer> boneIndices = new HashMap();
    final ArrayList<Integer> skeletonHierarchy = new ArrayList();
    final ArrayList<Matrix4f> bindPose = new ArrayList();
    final ArrayList<Matrix4f> invBindPose = new ArrayList();
    final ArrayList<Matrix4f> skinOffsetMatrices = new ArrayList();
    AiNode rootBoneNode;
    final HashMap<String, AnimationClip> clips = new HashMap();
    final AiBuiltInWrapperProvider wrapper = new AiBuiltInWrapperProvider();
    final Quaternion end = new Quaternion();

    private ImportedSkeleton() {
    }

    public static ImportedSkeleton process(ImportedSkeletonParams params) {
        ImportedSkeleton processedAiScene = new ImportedSkeleton();
        processedAiScene.processAiScene(params);
        return processedAiScene;
    }

    private void processAiScene(ImportedSkeletonParams params) {
        boolean bVehicle;
        AiScene scene = params.scene;
        JAssImpImporter.LoadMode mode = params.mode;
        SkinningData skinnedTo = params.skinnedTo;
        float animBonesScaleModifier = params.animBonesScaleModifier;
        Quaternion animBonesRotateModifier = params.animBonesRotateModifier;
        AiMesh mesh = params.mesh;
        AiNode rootnode = scene.getSceneRoot(this.wrapper);
        this.rootBoneNode = JAssImpImporter.FindNode("Dummy01", rootnode);
        if (this.rootBoneNode == null) {
            this.rootBoneNode = JAssImpImporter.FindNode("VehicleSkeleton", rootnode);
            bVehicle = true;
        } else {
            bVehicle = false;
        }
        while (this.rootBoneNode != null && this.rootBoneNode.getParent() != null && this.rootBoneNode.getParent() != rootnode) {
            this.rootBoneNode = this.rootBoneNode.getParent();
        }
        if (this.rootBoneNode == null) {
            this.rootBoneNode = rootnode;
        }
        ArrayList<AiNode> boneNodes = new ArrayList<AiNode>();
        JAssImpImporter.CollectBoneNodes(boneNodes, this.rootBoneNode);
        AiNode translationBonenode = JAssImpImporter.FindNode("Translation_Data", rootnode);
        if (translationBonenode != null) {
            boneNodes.add(translationBonenode);
            for (AiNode parent = translationBonenode.getParent(); parent != null && parent != rootnode; parent = parent.getParent()) {
                boneNodes.add(parent);
            }
        }
        if (skinnedTo != null) {
            this.boneIndices.putAll(skinnedTo.boneIndices);
            PZArrayUtil.addAll(this.skeletonHierarchy, skinnedTo.skeletonHierarchy);
        }
        for (int i = 0; i < boneNodes.size(); ++i) {
            AiNode parent;
            AiNode n = boneNodes.get(i);
            String bonename = n.getName();
            if (this.boneIndices.containsKey(bonename)) continue;
            int index = this.boneIndices.size();
            this.boneIndices.put(bonename, index);
            if (n == this.rootBoneNode) {
                this.skeletonHierarchy.add(-1);
                continue;
            }
            for (parent = n.getParent(); parent != null && !this.boneIndices.containsKey(parent.getName()); parent = parent.getParent()) {
            }
            if (parent != null) {
                this.skeletonHierarchy.add(this.boneIndices.get(parent.getName()));
                continue;
            }
            this.skeletonHierarchy.add(0);
        }
        Matrix4f mi = new Matrix4f();
        for (int i = 0; i < this.boneIndices.size(); ++i) {
            this.bindPose.add(mi);
            this.skinOffsetMatrices.add(mi);
        }
        List<AiBone> bones = mesh.getBones();
        Matrix4f skinOffseti = new Matrix4f();
        Matrix4f skinOffsetParent = new Matrix4f();
        Matrix4f skinOffsetParenti = new Matrix4f();
        for (int i = 0; i < boneNodes.size(); ++i) {
            AiMatrix4f pbonemat;
            AiMatrix4f bonemat;
            AiNode node = boneNodes.get(i);
            String nodeBoneName = node.getName();
            AiBone bone = JAssImpImporter.FindAiBone(nodeBoneName, bones);
            if (bone == null || (bonemat = bone.getOffsetMatrix(this.wrapper)) == null) continue;
            Matrix4f skinOffset = JAssImpImporter.getMatrixFromAiMatrix(bonemat);
            skinOffseti.load(skinOffset);
            skinOffseti.invert();
            skinOffsetParent.setIdentity();
            String parentBoneName = node.getParent().getName();
            AiBone pbone = JAssImpImporter.FindAiBone(parentBoneName, bones);
            if (pbone != null && (pbonemat = pbone.getOffsetMatrix(this.wrapper)) != null) {
                JAssImpImporter.getMatrixFromAiMatrix(pbonemat, skinOffsetParent);
            }
            skinOffsetParenti.load(skinOffsetParent);
            skinOffsetParenti.invert();
            Matrix4f bind = new Matrix4f();
            Matrix4f.mul(skinOffseti, skinOffsetParenti, bind);
            bind.invert();
            int boneindex = this.boneIndices.get(nodeBoneName);
            this.bindPose.set(boneindex, bind);
            this.skinOffsetMatrices.set(boneindex, skinOffset);
        }
        int num = this.bindPose.size();
        for (int i = 0; i < num; ++i) {
            Matrix4f ib = new Matrix4f(this.bindPose.get(i));
            ib.invert();
            this.invBindPose.add(i, ib);
        }
        int numAnims = scene.getNumAnimations();
        if (numAnims <= 0) {
            return;
        }
        List<AiAnimation> srcAnims = scene.getAnimations();
        for (int i = 0; i < numAnims; ++i) {
            AiAnimation srcAnim = srcAnims.get(i);
            if (bVehicle) {
                this.processAnimation(srcAnim, bVehicle, 1.0f, null);
                continue;
            }
            this.processAnimation(srcAnim, bVehicle, animBonesScaleModifier, animBonesRotateModifier);
        }
    }

    @Deprecated
    void processAnimationOld(AiAnimation srcAnim, boolean bVehicle) {
        ArrayList<Keyframe> frames = new ArrayList<Keyframe>();
        float numFrames = (float)srcAnim.getDuration();
        float duration = numFrames / (float)srcAnim.getTicksPerSecond();
        ArrayList<Float> frametimes = new ArrayList<Float>();
        List<AiNodeAnim> channels = srcAnim.getChannels();
        for (int j = 0; j < channels.size(); ++j) {
            float t;
            int k;
            AiNodeAnim a = channels.get(j);
            for (k = 0; k < a.getNumPosKeys(); ++k) {
                t = (float)a.getPosKeyTime(k);
                if (frametimes.contains(Float.valueOf(t))) continue;
                frametimes.add(Float.valueOf(t));
            }
            for (k = 0; k < a.getNumRotKeys(); ++k) {
                t = (float)a.getRotKeyTime(k);
                if (frametimes.contains(Float.valueOf(t))) continue;
                frametimes.add(Float.valueOf(t));
            }
            for (k = 0; k < a.getNumScaleKeys(); ++k) {
                t = (float)a.getScaleKeyTime(k);
                if (frametimes.contains(Float.valueOf(t))) continue;
                frametimes.add(Float.valueOf(t));
            }
        }
        Collections.sort(frametimes);
        for (int tk = 0; tk < frametimes.size(); ++tk) {
            for (int j = 0; j < channels.size(); ++j) {
                AiNodeAnim a = channels.get(j);
                Keyframe f = new Keyframe();
                f.clear();
                f.boneName = a.getNodeName();
                Integer boneIdx = this.boneIndices.get(f.boneName);
                if (boneIdx == null) {
                    DebugLog.General.error("Could not find bone index for node name: \"%s\"", f.boneName);
                    continue;
                }
                f.none = boneIdx;
                f.time = ((Float)frametimes.get(tk)).floatValue() / (float)srcAnim.getTicksPerSecond();
                if (!bVehicle) {
                    f.position = JAssImpImporter.GetKeyFramePosition(a, ((Float)frametimes.get(tk)).floatValue());
                    f.rotation = JAssImpImporter.GetKeyFrameRotation(a, ((Float)frametimes.get(tk)).floatValue());
                    f.scale = JAssImpImporter.GetKeyFrameScale(a, ((Float)frametimes.get(tk)).floatValue());
                } else {
                    f.position = this.GetKeyFramePosition(a, ((Float)frametimes.get(tk)).floatValue(), srcAnim.getDuration());
                    f.rotation = this.GetKeyFrameRotation(a, ((Float)frametimes.get(tk)).floatValue(), srcAnim.getDuration());
                    f.scale = this.GetKeyFrameScale(a, ((Float)frametimes.get(tk)).floatValue(), srcAnim.getDuration());
                }
                if (f.none < 0) continue;
                frames.add(f);
            }
        }
        String animName = srcAnim.getName();
        int p = animName.indexOf(124);
        if (p > 0) {
            animName = animName.substring(p + 1);
        }
        AnimationClip clip = new AnimationClip(duration, frames, animName, true);
        frames.clear();
        this.clips.put(animName, clip);
    }

    private void processAnimation(AiAnimation srcAnim, boolean bVehicle, float animBonesScaleModifier, Quaternion animBonesRotateModifier) {
        boolean applyBoneRotateModifier;
        ArrayList<Keyframe> frames = new ArrayList<Keyframe>();
        float duration = (float)srcAnim.getDuration();
        float durationSeconds = duration / (float)srcAnim.getTicksPerSecond();
        Object[] frameTimesPerBone = new TFloatArrayList[this.boneIndices.size()];
        Arrays.fill(frameTimesPerBone, null);
        ArrayList<ArrayList<AiNodeAnim>> channelsPerBone = new ArrayList<ArrayList<AiNodeAnim>>(this.boneIndices.size());
        for (int i = 0; i < this.boneIndices.size(); ++i) {
            channelsPerBone.add(null);
        }
        this.collectBoneFrames(srcAnim, (TFloatArrayList[])frameTimesPerBone, channelsPerBone);
        Quaternion animBonesRotateModifierInv = null;
        boolean bl = applyBoneRotateModifier = animBonesRotateModifier != null;
        if (applyBoneRotateModifier) {
            animBonesRotateModifierInv = new Quaternion();
            Quaternion.mulInverse(animBonesRotateModifierInv, animBonesRotateModifier, animBonesRotateModifierInv);
        }
        for (int boneIdx = 0; boneIdx < this.boneIndices.size(); ++boneIdx) {
            ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
            if (boneChannels == null) {
                if (boneIdx != 0 || animBonesRotateModifier == null) continue;
                String boneName = "RootNode";
                Quaternion rotation = new Quaternion();
                rotation.set(animBonesRotateModifier);
                this.addDefaultAnimTrack("RootNode", boneIdx, rotation, new Vector3f(0.0f, 0.0f, 0.0f), frames, durationSeconds);
                continue;
            }
            Object frameTimes = frameTimesPerBone[boneIdx];
            if (frameTimes == null) continue;
            ((TFloatArrayList)frameTimes).sort();
            int parentBoneIdx = this.getParentBoneIdx(boneIdx);
            boolean parentBoneAdjusted = applyBoneRotateModifier && (parentBoneIdx == 0 || this.doesParentBoneHaveAnimFrames((TFloatArrayList[])frameTimesPerBone, channelsPerBone, boneIdx));
            for (int tk = 0; tk < ((TFloatArrayList)frameTimes).size(); ++tk) {
                float frameTime = ((TFloatArrayList)frameTimes).get(tk);
                float frameSeconds = frameTime / (float)srcAnim.getTicksPerSecond();
                for (int channelIdx = 0; channelIdx < boneChannels.size(); ++channelIdx) {
                    AiNodeAnim a = boneChannels.get(channelIdx);
                    Keyframe f = new Keyframe();
                    f.clear();
                    f.boneName = a.getNodeName();
                    f.none = boneIdx;
                    f.time = frameSeconds;
                    if (!bVehicle) {
                        f.position = JAssImpImporter.GetKeyFramePosition(a, frameTime);
                        f.rotation = JAssImpImporter.GetKeyFrameRotation(a, frameTime);
                        f.scale = JAssImpImporter.GetKeyFrameScale(a, frameTime);
                    } else {
                        f.position = this.GetKeyFramePosition(a, frameTime, duration);
                        f.rotation = this.GetKeyFrameRotation(a, frameTime, duration);
                        f.scale = this.GetKeyFrameScale(a, frameTime, duration);
                    }
                    f.position.x *= animBonesScaleModifier;
                    f.position.y *= animBonesScaleModifier;
                    f.position.z *= animBonesScaleModifier;
                    if (applyBoneRotateModifier) {
                        if (parentBoneAdjusted) {
                            Quaternion.mul(animBonesRotateModifierInv, f.rotation, f.rotation);
                            boolean isTranslationBone = StringUtils.startsWithIgnoreCase(f.boneName, "Translation_Data");
                            if (!isTranslationBone) {
                                HelperFunctions.transform(animBonesRotateModifierInv, f.position, f.position);
                            }
                        }
                        Quaternion.mul(f.rotation, animBonesRotateModifier, f.rotation);
                    }
                    frames.add(f);
                }
            }
        }
        String animName = srcAnim.getName();
        int p = animName.indexOf(124);
        if (p > 0) {
            animName = animName.substring(p + 1);
        }
        animName = animName.trim();
        AnimationClip clip = new AnimationClip(durationSeconds, frames, animName, true);
        frames.clear();
        this.clips.put(animName, clip);
    }

    private void addDefaultAnimTrack(String boneName, int boneIdx, Quaternion rotation, Vector3f position, ArrayList<Keyframe> frames, float durationSeconds) {
        Vector3f one = new Vector3f(1.0f, 1.0f, 1.0f);
        Keyframe firstFrame = new Keyframe();
        firstFrame.clear();
        firstFrame.boneName = boneName;
        firstFrame.none = boneIdx;
        firstFrame.time = 0.0f;
        firstFrame.position = position;
        firstFrame.rotation = rotation;
        firstFrame.scale = one;
        frames.add(firstFrame);
        Keyframe lastFrame = new Keyframe();
        lastFrame.clear();
        lastFrame.boneName = boneName;
        lastFrame.none = boneIdx;
        lastFrame.time = durationSeconds;
        lastFrame.position = position;
        lastFrame.rotation = rotation;
        lastFrame.scale = one;
        frames.add(lastFrame);
    }

    private boolean doesParentBoneHaveAnimFrames(TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone, int boneIdx) {
        int parentBoneIdx = this.getParentBoneIdx(boneIdx);
        if (parentBoneIdx < 0) {
            return false;
        }
        return this.doesBoneHaveAnimFrames(frameTimesPerBone, channelsPerBone, parentBoneIdx);
    }

    private boolean doesBoneHaveAnimFrames(TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone, int boneIdx) {
        TFloatArrayList frameTimes = frameTimesPerBone[boneIdx];
        if (frameTimes == null || frameTimes.size() <= 0) {
            return false;
        }
        ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
        return boneChannels.size() > 0;
    }

    private void collectBoneFrames(AiAnimation srcAnim, TFloatArrayList[] frameTimesPerBone, ArrayList<ArrayList<AiNodeAnim>> channelsPerBone) {
        List<AiNodeAnim> channels = srcAnim.getChannels();
        for (int j = 0; j < channels.size(); ++j) {
            float t;
            int k;
            AiNodeAnim a = channels.get(j);
            String boneName = a.getNodeName();
            Integer boneIdx = this.boneIndices.get(boneName);
            if (boneIdx == null) {
                DebugLog.General.error("Could not find bone index for node name: \"%s\"", boneName);
                continue;
            }
            ArrayList<AiNodeAnim> boneChannels = channelsPerBone.get(boneIdx);
            if (boneChannels == null) {
                boneChannels = new ArrayList();
                channelsPerBone.set(boneIdx, boneChannels);
            }
            boneChannels.add(a);
            TFloatArrayList frametimes = frameTimesPerBone[boneIdx];
            if (frametimes == null) {
                frameTimesPerBone[boneIdx.intValue()] = frametimes = new TFloatArrayList();
            }
            for (k = 0; k < a.getNumPosKeys(); ++k) {
                t = (float)a.getPosKeyTime(k);
                if (frametimes.contains(t)) continue;
                frametimes.add(t);
            }
            for (k = 0; k < a.getNumRotKeys(); ++k) {
                t = (float)a.getRotKeyTime(k);
                if (frametimes.contains(t)) continue;
                frametimes.add(t);
            }
            for (k = 0; k < a.getNumScaleKeys(); ++k) {
                t = (float)a.getScaleKeyTime(k);
                if (frametimes.contains(t)) continue;
                frametimes.add(t);
            }
        }
    }

    private int getParentBoneIdx(int boneIdx) {
        if (boneIdx > -1) {
            return this.skeletonHierarchy.get(boneIdx);
        }
        return -1;
    }

    public int getNumBoneAncestors(int boneIdx) {
        int numAncestors = 0;
        int parentBoneIdx = this.getParentBoneIdx(boneIdx);
        while (parentBoneIdx > -1) {
            ++numAncestors;
            parentBoneIdx = this.getParentBoneIdx(parentBoneIdx);
        }
        return numAncestors;
    }

    private Vector3f GetKeyFramePosition(AiNodeAnim animNode, float time, double duration) {
        int frame;
        Vector3f pos = new Vector3f();
        if (animNode.getNumPosKeys() == 0) {
            return pos;
        }
        for (frame = 0; frame < animNode.getNumPosKeys() - 1 && !((double)time < animNode.getPosKeyTime(frame + 1)); ++frame) {
        }
        int nextFrame = (frame + 1) % animNode.getNumPosKeys();
        float t1 = (float)animNode.getPosKeyTime(frame);
        float t2 = (float)animNode.getPosKeyTime(nextFrame);
        float diffTime = t2 - t1;
        if (diffTime < 0.0f) {
            diffTime = (float)((double)diffTime + duration);
        }
        if (diffTime > 0.0f) {
            float r = t2 - t1;
            float s = time - t1;
            float x1 = animNode.getPosKeyX(frame);
            float x2 = animNode.getPosKeyX(nextFrame);
            float x = x1 + (s /= r) * (x2 - x1);
            float y1 = animNode.getPosKeyY(frame);
            float y2 = animNode.getPosKeyY(nextFrame);
            float y = y1 + s * (y2 - y1);
            float z1 = animNode.getPosKeyZ(frame);
            float z2 = animNode.getPosKeyZ(nextFrame);
            float z = z1 + s * (z2 - z1);
            pos.set(x, y, z);
        } else {
            pos.set(animNode.getPosKeyX(frame), animNode.getPosKeyY(frame), animNode.getPosKeyZ(frame));
        }
        return pos;
    }

    private Quaternion GetKeyFrameRotation(AiNodeAnim animNode, float time, double duration) {
        int frame;
        Quaternion foundQuat = new Quaternion();
        if (animNode.getNumRotKeys() == 0) {
            return foundQuat;
        }
        for (frame = 0; frame < animNode.getNumRotKeys() - 1 && !((double)time < animNode.getRotKeyTime(frame + 1)); ++frame) {
        }
        int nextFrame = (frame + 1) % animNode.getNumRotKeys();
        float t1 = (float)animNode.getRotKeyTime(frame);
        float t2 = (float)animNode.getRotKeyTime(nextFrame);
        float diffTime = t2 - t1;
        if (diffTime < 0.0f) {
            diffTime = (float)((double)diffTime + duration);
        }
        if (diffTime > 0.0f) {
            double sclq;
            double sclp;
            float pFactor = (time - t1) / diffTime;
            AiQuaternion pStart = animNode.getRotKeyQuaternion(frame, this.wrapper);
            AiQuaternion pEnd = animNode.getRotKeyQuaternion(nextFrame, this.wrapper);
            double cosom = pStart.getX() * pEnd.getX() + pStart.getY() * pEnd.getY() + pStart.getZ() * pEnd.getZ() + pStart.getW() * pEnd.getW();
            this.end.set(pEnd.getX(), pEnd.getY(), pEnd.getZ(), pEnd.getW());
            if (cosom < 0.0) {
                cosom *= -1.0;
                this.end.setX(-this.end.getX());
                this.end.setY(-this.end.getY());
                this.end.setZ(-this.end.getZ());
                this.end.setW(-this.end.getW());
            }
            if (1.0 - cosom > 1.0E-4) {
                double omega = Math.acos(cosom);
                double sinom = Math.sin(omega);
                sclp = Math.sin((1.0 - (double)pFactor) * omega) / sinom;
                sclq = Math.sin((double)pFactor * omega) / sinom;
            } else {
                sclp = 1.0 - (double)pFactor;
                sclq = pFactor;
            }
            foundQuat.set((float)(sclp * (double)pStart.getX() + sclq * (double)this.end.getX()), (float)(sclp * (double)pStart.getY() + sclq * (double)this.end.getY()), (float)(sclp * (double)pStart.getZ() + sclq * (double)this.end.getZ()), (float)(sclp * (double)pStart.getW() + sclq * (double)this.end.getW()));
        } else {
            float x = animNode.getRotKeyX(frame);
            float y = animNode.getRotKeyY(frame);
            float z = animNode.getRotKeyZ(frame);
            float w = animNode.getRotKeyW(frame);
            foundQuat.set(x, y, z, w);
        }
        return foundQuat;
    }

    private Vector3f GetKeyFrameScale(AiNodeAnim animNode, float time, double duration) {
        int frame;
        Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
        if (animNode.getNumScaleKeys() == 0) {
            return scale;
        }
        for (frame = 0; frame < animNode.getNumScaleKeys() - 1 && !((double)time < animNode.getScaleKeyTime(frame + 1)); ++frame) {
        }
        scale.set(animNode.getScaleKeyX(frame), animNode.getScaleKeyY(frame), animNode.getScaleKeyZ(frame));
        return scale;
    }
}

