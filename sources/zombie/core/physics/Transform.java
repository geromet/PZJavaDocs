/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.physics;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Transform {
    public final Matrix3f basis = new Matrix3f();
    public final Vector3f origin = new Vector3f();

    public Transform() {
    }

    public Transform(Matrix3f mat) {
        this.basis.set(mat);
    }

    public Transform(Matrix4f mat) {
        this.set(mat);
    }

    public Transform(Transform tr) {
        this.set(tr);
    }

    public void set(Transform tr) {
        this.basis.set(tr.basis);
        this.origin.set(tr.origin);
    }

    public void set(Matrix3f mat) {
        this.basis.set(mat);
        this.origin.set(0.0f, 0.0f, 0.0f);
    }

    public void set(Matrix4f mat) {
        mat.get3x3(this.basis);
        mat.getTranslation(this.origin);
    }

    public void transform(Vector3f v) {
        this.basis.transform(v);
        v.add(this.origin);
    }

    public void setIdentity() {
        this.basis.identity();
        this.origin.set(0.0f, 0.0f, 0.0f);
    }

    public void inverse() {
        this.basis.transpose();
        this.origin.negate();
        this.basis.transform(this.origin);
    }

    public void inverse(Transform tr) {
        this.set(tr);
        this.inverse();
    }

    public Quaternionf getRotation(Quaternionf out) {
        this.basis.getUnnormalizedRotation(out);
        return out;
    }

    public void setRotation(Quaternionf q) {
        this.basis.set(q);
    }

    public Matrix4f getMatrix(Matrix4f out) {
        out.set(this.basis);
        out.setTranslation(this.origin);
        return out;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Transform)) {
            return false;
        }
        Transform tr = (Transform)obj;
        return this.basis.equals(tr.basis) && this.origin.equals(tr.origin);
    }

    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + this.basis.hashCode();
        hash = 41 * hash + this.origin.hashCode();
        return hash;
    }
}

