/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.opengl.RenderThread;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelTxt;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.util.SharedStrings;

public final class ModelLoader {
    public static final ModelLoader instance = new ModelLoader();
    private final ThreadLocal<SharedStrings> sharedStrings = ThreadLocal.withInitial(SharedStrings::new);

    protected ModelTxt loadTxt(String filename, boolean bStatic, boolean bReverse, SkinningData skinnedTo) throws IOException {
        ModelTxt mt = new ModelTxt();
        mt.isStatic = bStatic;
        mt.reverse = bReverse;
        VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(bStatic ? 4 : 6);
        format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        format.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        format.setElement(2, VertexBufferObject.VertexType.TangentArray, 12);
        format.setElement(3, VertexBufferObject.VertexType.TextureCoordArray, 8);
        if (!bStatic) {
            format.setElement(4, VertexBufferObject.VertexType.BlendWeightArray, 16);
            format.setElement(5, VertexBufferObject.VertexType.BlendIndexArray, 16);
        }
        format.calculate();
        try (FileReader fileReader = new FileReader(filename);
             BufferedReader reader = new BufferedReader(fileReader);){
            String line;
            SharedStrings sharedStrings = this.sharedStrings.get();
            LoadMode mode = LoadMode.Version;
            int skip = 0;
            int numVertices = 0;
            int numElements = 0;
            int numBones = 0;
            boolean bHasTangent = false;
            while ((line = reader.readLine()) != null) {
                if (line.indexOf(35) == 0) continue;
                if (line.contains("Tangent")) {
                    if (bStatic) {
                        skip += 2;
                    }
                    bHasTangent = true;
                }
                if (skip > 0) {
                    --skip;
                    continue;
                }
                switch (mode.ordinal()) {
                    case 0: {
                        mode = LoadMode.ModelName;
                        break;
                    }
                    case 1: {
                        mode = LoadMode.VertexStrideElementCount;
                        break;
                    }
                    case 2: {
                        mode = LoadMode.VertexCount;
                        if (bStatic) {
                            skip = 7;
                            break;
                        }
                        skip = 13;
                        break;
                    }
                    case 5: {
                        numVertices = Integer.parseInt(line);
                        mode = LoadMode.VertexBuffer;
                        mt.vertices = new VertexBufferObject.VertexArray(format, numVertices);
                        break;
                    }
                    case 6: {
                        for (int n = 0; n < numVertices; ++n) {
                            String[] spl = line.split(",");
                            float x = Float.parseFloat(spl[0].trim());
                            float y = Float.parseFloat(spl[1].trim());
                            float z = Float.parseFloat(spl[2].trim());
                            line = reader.readLine();
                            spl = line.split(",");
                            float nx = Float.parseFloat(spl[0].trim());
                            float ny = Float.parseFloat(spl[1].trim());
                            float nz = Float.parseFloat(spl[2].trim());
                            float tx = 0.0f;
                            float ty = 0.0f;
                            float tz = 0.0f;
                            if (bHasTangent) {
                                line = reader.readLine();
                                spl = line.split(",");
                                tx = Float.parseFloat(spl[0].trim());
                                ty = Float.parseFloat(spl[1].trim());
                                tz = Float.parseFloat(spl[2].trim());
                            }
                            line = reader.readLine();
                            spl = line.split(",");
                            float texx = Float.parseFloat(spl[0].trim());
                            float texy = Float.parseFloat(spl[1].trim());
                            float bx = 0.0f;
                            float by = 0.0f;
                            float bz = 0.0f;
                            float bw = 0.0f;
                            int bix = 0;
                            int biy = 0;
                            int biz = 0;
                            int biw = 0;
                            if (!bStatic) {
                                line = reader.readLine();
                                spl = line.split(",");
                                bx = Float.parseFloat(spl[0].trim());
                                by = Float.parseFloat(spl[1].trim());
                                bz = Float.parseFloat(spl[2].trim());
                                bw = Float.parseFloat(spl[3].trim());
                                line = reader.readLine();
                                spl = line.split(",");
                                bix = Integer.parseInt(spl[0].trim());
                                biy = Integer.parseInt(spl[1].trim());
                                biz = Integer.parseInt(spl[2].trim());
                                biw = Integer.parseInt(spl[3].trim());
                            }
                            line = reader.readLine();
                            mt.vertices.setElement(n, 0, x, y, z);
                            mt.vertices.setElement(n, 1, nx, ny, nz);
                            mt.vertices.setElement(n, 2, tx, ty, tz);
                            mt.vertices.setElement(n, 3, texx, texy);
                            if (bStatic) continue;
                            mt.vertices.setElement(n, 4, bx, by, bz, bw);
                            mt.vertices.setElement(n, 5, bix, biy, biz, biw);
                        }
                        mode = LoadMode.NumberOfFaces;
                        break;
                    }
                    case 7: {
                        numElements = Integer.parseInt(line);
                        mt.elements = new int[numElements * 3];
                        mode = LoadMode.FaceData;
                        break;
                    }
                    case 8: {
                        for (int n = 0; n < numElements; ++n) {
                            String[] spl = line.split(",");
                            int x = Integer.parseInt(spl[0].trim());
                            int y = Integer.parseInt(spl[1].trim());
                            int z = Integer.parseInt(spl[2].trim());
                            if (bReverse) {
                                mt.elements[n * 3 + 2] = x;
                                mt.elements[n * 3 + 1] = y;
                                mt.elements[n * 3 + 0] = z;
                            } else {
                                mt.elements[n * 3 + 0] = x;
                                mt.elements[n * 3 + 1] = y;
                                mt.elements[n * 3 + 2] = z;
                            }
                            line = reader.readLine();
                        }
                        mode = LoadMode.NumberOfBones;
                        break;
                    }
                    case 9: {
                        numBones = Integer.parseInt(line);
                        mode = LoadMode.SkeletonHierarchy;
                        break;
                    }
                    case 10: {
                        for (int n = 0; n < numBones; ++n) {
                            int index = Integer.parseInt(line);
                            line = reader.readLine();
                            int pindex = Integer.parseInt(line);
                            line = reader.readLine();
                            String bone = sharedStrings.get(line);
                            line = reader.readLine();
                            mt.skeletonHierarchy.add(pindex);
                            mt.boneIndices.put(bone, index);
                        }
                        mode = LoadMode.BindPose;
                        break;
                    }
                    case 11: {
                        for (int n = 0; n < numBones; ++n) {
                            line = reader.readLine();
                            String line2 = reader.readLine();
                            String line3 = reader.readLine();
                            String line4 = reader.readLine();
                            mt.bindPose.add(n, this.getMatrix(line, line2, line3, line4));
                            reader.readLine();
                        }
                        mode = LoadMode.InvBindPose;
                        break;
                    }
                    case 12: {
                        for (int n = 0; n < numBones; ++n) {
                            line = reader.readLine();
                            String line2 = reader.readLine();
                            String line3 = reader.readLine();
                            String line4 = reader.readLine();
                            mt.invBindPose.add(n, this.getMatrix(line, line2, line3, line4));
                            reader.readLine();
                        }
                        mode = LoadMode.SkinOffsetMatrices;
                        break;
                    }
                    case 13: {
                        for (int n = 0; n < numBones; ++n) {
                            line = reader.readLine();
                            String line2 = reader.readLine();
                            String line3 = reader.readLine();
                            String line4 = reader.readLine();
                            mt.skinOffsetMatrices.add(n, this.getMatrix(line, line2, line3, line4));
                            reader.readLine();
                        }
                        mode = LoadMode.NumberOfAnims;
                        break;
                    }
                    case 14: {
                        mode = LoadMode.Anim;
                        break;
                    }
                    case 15: {
                        ArrayList<Keyframe> frames = new ArrayList<Keyframe>();
                        String animName = line;
                        line = reader.readLine();
                        float duration = Float.parseFloat(line);
                        line = reader.readLine();
                        int nFrames = Integer.parseInt(line);
                        line = reader.readLine();
                        for (int n = 0; n < nFrames; ++n) {
                            Keyframe f = new Keyframe();
                            int bindex = Integer.parseInt(line);
                            line = reader.readLine();
                            String bone = sharedStrings.get(line);
                            line = reader.readLine();
                            float timeInSec = Float.parseFloat(line);
                            line = reader.readLine();
                            String line2 = reader.readLine();
                            Vector3f vec = this.getVector(line);
                            Quaternion q = this.getQuaternion(line2);
                            if (n < nFrames - 1) {
                                line = reader.readLine();
                            }
                            f.none = bindex;
                            f.boneName = bone;
                            f.time = timeInSec;
                            f.rotation = q;
                            f.position = new Vector3f(vec);
                            frames.add(f);
                        }
                        AnimationClip clip = new AnimationClip(duration, frames, animName, false);
                        frames.clear();
                        mt.clips.put(animName, clip);
                    }
                }
            }
            if (!bStatic && skinnedTo != null) {
                try {
                    int[] boneRemapping = new int[mt.boneIndices.size()];
                    ArrayList<Integer> originalHierarchy = mt.skeletonHierarchy;
                    HashMap<String, Integer> originalIndices = mt.boneIndices;
                    HashMap<String, Integer> remappedBoneIndices = new HashMap<String, Integer>(skinnedTo.boneIndices);
                    ArrayList<Integer> remappedSkeletonHierarchy = new ArrayList<Integer>(skinnedTo.skeletonHierarchy);
                    originalIndices.forEach((boneName, originalIndex) -> {
                        int remappedIdx = remappedBoneIndices.getOrDefault(boneName, -1);
                        if (remappedIdx == -1) {
                            remappedIdx = remappedBoneIndices.size();
                            remappedBoneIndices.put((String)boneName, remappedIdx);
                            int originalParentIdx = (Integer)originalHierarchy.get((int)originalIndex);
                            if (originalParentIdx >= 0) {
                                remappedSkeletonHierarchy.add(boneRemapping[originalParentIdx]);
                            }
                        }
                        boneRemapping[originalIndex.intValue()] = remappedIdx;
                    });
                    mt.boneIndices = remappedBoneIndices;
                    mt.skeletonHierarchy = remappedSkeletonHierarchy;
                    for (int i = 0; i < mt.vertices.numVertices; ++i) {
                        int x = (int)mt.vertices.getElementFloat(i, 5, 0);
                        int y = (int)mt.vertices.getElementFloat(i, 5, 1);
                        int z = (int)mt.vertices.getElementFloat(i, 5, 2);
                        int w = (int)mt.vertices.getElementFloat(i, 5, 3);
                        if (x >= 0) {
                            x = boneRemapping[x];
                        }
                        if (y >= 0) {
                            y = boneRemapping[y];
                        }
                        if (z >= 0) {
                            z = boneRemapping[z];
                        }
                        if (w >= 0) {
                            w = boneRemapping[w];
                        }
                        mt.vertices.setElement(i, 5, x, y, z, w);
                    }
                    for (AnimationClip clip : mt.clips.values()) {
                        for (Keyframe kf : clip.getKeyframes()) {
                            kf.none = boneRemapping[kf.none];
                        }
                    }
                    mt.skinOffsetMatrices = this.RemapMatrices(boneRemapping, mt.skinOffsetMatrices, mt.boneIndices.size());
                    mt.bindPose = this.RemapMatrices(boneRemapping, mt.bindPose, mt.boneIndices.size());
                    mt.invBindPose = this.RemapMatrices(boneRemapping, mt.invBindPose, mt.boneIndices.size());
                }
                catch (Exception e) {
                    e.toString();
                }
            }
        }
        return mt;
    }

    protected void applyToMesh(ModelTxt mt, ModelMesh mesh, SkinningData skinnedTo) {
        if (mt.isStatic) {
            if (!ModelManager.noOpenGL) {
                mesh.hasVbo = true;
                RenderThread.queueInvokeOnRenderContext(() -> mesh.SetVertexBuffer(new VertexBufferObject(mt.vertices, mt.elements)));
            }
        } else {
            mesh.skinningData = new SkinningData(mt.clips, mt.bindPose, mt.invBindPose, mt.skinOffsetMatrices, mt.skeletonHierarchy, mt.boneIndices);
            if (!ModelManager.noOpenGL) {
                mesh.hasVbo = true;
                RenderThread.queueInvokeOnRenderContext(() -> mesh.SetVertexBuffer(new VertexBufferObject(mt.vertices, mt.elements, mt.reverse)));
            }
        }
        if (skinnedTo != null) {
            mesh.skinningData.animationClips = skinnedTo.animationClips;
        }
    }

    protected void applyToAnimation(ModelTxt mt, AnimationAsset anim) {
        anim.animationClips = mt.clips;
        anim.assetParams.animationsMesh.skinningData.animationClips.putAll(mt.clips);
    }

    private ArrayList<Matrix4f> RemapMatrices(int[] boneRemapping, ArrayList<Matrix4f> original, int newCount) {
        int i;
        ArrayList<Matrix4f> remapped = new ArrayList<Matrix4f>(newCount);
        Matrix4f ident = new Matrix4f();
        for (i = 0; i < newCount; ++i) {
            remapped.add(ident);
        }
        for (i = 0; i < boneRemapping.length; ++i) {
            remapped.set(boneRemapping[i], original.get(i));
        }
        return remapped;
    }

    private Vector3f getVector(String line) {
        Vector3f v = new Vector3f();
        String[] split = line.split(",");
        v.x = Float.parseFloat(split[0]);
        v.y = Float.parseFloat(split[1]);
        v.z = Float.parseFloat(split[2]);
        return v;
    }

    private Quaternion getQuaternion(String line) {
        Quaternion v = new Quaternion();
        String[] split = line.split(",");
        v.x = Float.parseFloat(split[0]);
        v.y = Float.parseFloat(split[1]);
        v.z = Float.parseFloat(split[2]);
        v.w = Float.parseFloat(split[3]);
        return v;
    }

    private Matrix4f getMatrix(String line, String line2, String line3, String line4) {
        Matrix4f m = new Matrix4f();
        boolean i = false;
        String[] split = line.split(",");
        m.m00 = Float.parseFloat(split[0]);
        m.m01 = Float.parseFloat(split[1]);
        m.m02 = Float.parseFloat(split[2]);
        m.m03 = Float.parseFloat(split[3]);
        split = line2.split(",");
        m.m10 = Float.parseFloat(split[0]);
        m.m11 = Float.parseFloat(split[1]);
        m.m12 = Float.parseFloat(split[2]);
        m.m13 = Float.parseFloat(split[3]);
        split = line3.split(",");
        m.m20 = Float.parseFloat(split[0]);
        m.m21 = Float.parseFloat(split[1]);
        m.m22 = Float.parseFloat(split[2]);
        m.m23 = Float.parseFloat(split[3]);
        split = line4.split(",");
        m.m30 = Float.parseFloat(split[0]);
        m.m31 = Float.parseFloat(split[1]);
        m.m32 = Float.parseFloat(split[2]);
        m.m33 = Float.parseFloat(split[3]);
        return m;
    }

    public static enum LoadMode {
        Version,
        ModelName,
        VertexStrideElementCount,
        VertexStrideSize,
        VertexStrideData,
        VertexCount,
        VertexBuffer,
        NumberOfFaces,
        FaceData,
        NumberOfBones,
        SkeletonHierarchy,
        BindPose,
        InvBindPose,
        SkinOffsetMatrices,
        NumberOfAnims,
        Anim;

    }
}

