/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiBone;
import jassimp.AiBoneWeight;
import jassimp.AiMesh;
import java.util.List;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.model.jassimp.ImportedSkeleton;

public final class ImportedSkinnedMesh {
    final ImportedSkeleton skeleton;
    String name;
    VertexBufferObject.VertexArray vertices;
    int[] elements;
    Matrix4f transform;

    public ImportedSkinnedMesh(ImportedSkeleton skeleton, AiMesh mesh) {
        this.skeleton = skeleton;
        this.processAiScene(mesh);
    }

    private void processAiScene(AiMesh mesh) {
        int i;
        this.name = mesh.getName();
        int numVertices = mesh.getNumVertices();
        int weightsPerVertex = 4;
        int boneArraysSize = numVertices * 4;
        int[] boneIDs = new int[boneArraysSize];
        float[] boneWeights = new float[boneArraysSize];
        for (int i2 = 0; i2 < boneArraysSize; ++i2) {
            boneWeights[i2] = 0.0f;
        }
        List<AiBone> bones = mesh.getBones();
        int numMeshBones = bones.size();
        for (i = 0; i < numMeshBones; ++i) {
            AiBone aiBone = bones.get(i);
            String bonename = aiBone.getName();
            int boneindex = this.skeleton.boneIndices.get(bonename);
            List<AiBoneWeight> weights = aiBone.getBoneWeights();
            block2: for (int j = 0; j < aiBone.getNumWeights(); ++j) {
                AiBoneWeight weight = weights.get(j);
                int vertexStart = weight.getVertexId() * 4;
                for (int k = 0; k < 4; ++k) {
                    if (boneWeights[vertexStart + k] != 0.0f) continue;
                    boneWeights[vertexStart + k] = weight.getWeight();
                    boneIDs[vertexStart + k] = boneindex;
                    continue block2;
                }
            }
        }
        for (i = 0; i < numVertices; ++i) {
            float sum = 0.0f;
            for (int k = 0; k < 4; ++k) {
                sum += boneWeights[i * 4 + k];
            }
            if (sum == 0.0f) continue;
            float invSum = 1.0f / sum;
            for (int k = 0; k < 4; ++k) {
                int n = i * 4 + k;
                boneWeights[n] = boneWeights[n] * invSum;
            }
        }
        int numUVs = ImportedSkinnedMesh.getNumUVs(mesh);
        VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(4 + numUVs);
        format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        format.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        format.setElement(2, VertexBufferObject.VertexType.BlendWeightArray, 16);
        format.setElement(3, VertexBufferObject.VertexType.BlendIndexArray, 16);
        for (int i3 = 0; i3 < numUVs; ++i3) {
            format.setElement(4 + i3, VertexBufferObject.VertexType.TextureCoordArray, 8);
        }
        format.calculate();
        this.vertices = new VertexBufferObject.VertexArray(format, numVertices);
        for (int n = 0; n < numVertices; ++n) {
            this.vertices.setElement(n, 0, mesh.getPositionX(n), mesh.getPositionY(n), mesh.getPositionZ(n));
            if (mesh.hasNormals()) {
                this.vertices.setElement(n, 1, mesh.getNormalX(n), mesh.getNormalY(n), mesh.getNormalZ(n));
            } else {
                this.vertices.setElement(n, 1, 0.0f, 1.0f, 0.0f);
            }
            this.vertices.setElement(n, 2, boneWeights[n * 4], boneWeights[n * 4 + 1], boneWeights[n * 4 + 2], boneWeights[n * 4 + 3]);
            this.vertices.setElement(n, 3, boneIDs[n * 4], boneIDs[n * 4 + 1], boneIDs[n * 4 + 2], boneIDs[n * 4 + 3]);
            if (numUVs <= 0) continue;
            int nUV = 0;
            for (int i4 = 0; i4 < 8; ++i4) {
                if (!mesh.hasTexCoords(i4)) continue;
                this.vertices.setElement(n, 4 + nUV, mesh.getTexCoordU(n, i4), 1.0f - mesh.getTexCoordV(n, i4));
                ++nUV;
            }
        }
        int numElements = mesh.getNumFaces();
        this.elements = new int[numElements * 3];
        for (int f = 0; f < numElements; ++f) {
            this.elements[f * 3 + 2] = mesh.getFaceVertex(f, 0);
            this.elements[f * 3 + 1] = mesh.getFaceVertex(f, 1);
            this.elements[f * 3 + 0] = mesh.getFaceVertex(f, 2);
        }
    }

    private static int getNumUVs(AiMesh mesh) {
        int numUVs = 0;
        for (int i = 0; i < 8; ++i) {
            if (!mesh.hasTexCoords(i)) continue;
            ++numUVs;
        }
        return numUVs;
    }
}

