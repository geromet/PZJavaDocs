/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import zombie.core.skinnedmodel.model.SoftwareModelMesh;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public final class SoftwareModelMeshInstance {
    public SoftwareModelMesh softwareMesh;
    public VertexBufferObject vb;
    public String name;

    public SoftwareModelMeshInstance(String name, SoftwareModelMesh softwareMesh) {
        this.name = name;
        this.softwareMesh = softwareMesh;
        this.vb = new VertexBufferObject();
        this.vb.elements = softwareMesh.indicesUnskinned;
    }
}

