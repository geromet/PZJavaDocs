/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiMesh;
import org.joml.Vector3f;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class ImportedStaticMesh {
    VertexBufferObject.VertexArray verticesUnskinned;
    int[] elements;
    final Vector3f minXyz = new Vector3f(Float.MAX_VALUE);
    final Vector3f maxXyz = new Vector3f(-3.4028235E38f);
    Matrix4f transform;

    public ImportedStaticMesh(AiMesh mesh) {
        this.processAiScene(mesh);
    }

    private void processAiScene(AiMesh mesh) {
        int numVertices = mesh.getNumVertices();
        int numUVs = 0;
        for (int i = 0; i < 8; ++i) {
            if (!mesh.hasTexCoords(i)) continue;
            ++numUVs;
        }
        VertexBufferObject.VertexFormat format = new VertexBufferObject.VertexFormat(2 + numUVs);
        format.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
        format.setElement(1, VertexBufferObject.VertexType.NormalArray, 12);
        for (int i = 0; i < numUVs; ++i) {
            format.setElement(2 + i, VertexBufferObject.VertexType.TextureCoordArray, 8);
        }
        format.calculate();
        this.verticesUnskinned = new VertexBufferObject.VertexArray(format, numVertices);
        Vector3f pos = new Vector3f();
        for (int n = 0; n < numVertices; ++n) {
            float vx = mesh.getPositionX(n);
            float vy = mesh.getPositionY(n);
            float vz = mesh.getPositionZ(n);
            this.minXyz.min(pos.set(vx, vy, vz));
            this.maxXyz.max(pos.set(vx, vy, vz));
            this.verticesUnskinned.setElement(n, 0, mesh.getPositionX(n), mesh.getPositionY(n), mesh.getPositionZ(n));
            if (mesh.hasNormals()) {
                this.verticesUnskinned.setElement(n, 1, mesh.getNormalX(n), mesh.getNormalY(n), mesh.getNormalZ(n));
            } else {
                this.verticesUnskinned.setElement(n, 1, 0.0f, 1.0f, 0.0f);
            }
            if (numUVs <= 0) continue;
            int nUV = 0;
            for (int i = 0; i < 8; ++i) {
                if (!mesh.hasTexCoords(i)) continue;
                this.verticesUnskinned.setElement(n, 2 + nUV, mesh.getTexCoordU(n, i), 1.0f - mesh.getTexCoordV(n, i));
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
}

