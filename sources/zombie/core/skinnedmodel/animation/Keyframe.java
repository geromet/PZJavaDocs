/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.animation;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;
import zombie.core.math.PZMath;

public final class Keyframe {
    public Quaternion rotation;
    public Vector3f position;
    public Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
    public int none;
    public String boneName;
    public float time = -1.0f;

    public Keyframe() {
    }

    public Keyframe(Vector3f pos, Quaternion rotation, Vector3f scale) {
        this.position = new Vector3f(pos);
        this.rotation = new Quaternion(rotation);
        this.scale = new Vector3f(scale);
    }

    public void set(Keyframe keyframe) {
        this.setPosition(keyframe.position);
        this.setRotation(keyframe.rotation);
        this.setScale(keyframe.scale);
        this.time = keyframe.time;
        this.none = keyframe.none;
        this.boneName = keyframe.boneName;
    }

    public void set(Vector3f pos, Quaternion rot, Vector3f scale) {
        this.setPosition(pos);
        this.setRotation(rot);
        this.setScale(scale);
    }

    public void get(Vector3f pos, Quaternion rot, Vector3f scale) {
        Keyframe.setIfNotNull(pos, this.position, 0.0f, 0.0f, 0.0f);
        Keyframe.setIfNotNull(rot, this.rotation);
        Keyframe.setIfNotNull(scale, this.scale, 1.0f, 1.0f, 1.0f);
    }

    public void setScale(Vector3f scale) {
        if (scale == null) {
            this.scale = null;
            return;
        }
        if (this.scale == null) {
            this.scale = new Vector3f();
        }
        this.scale.set(scale);
    }

    public void setRotation(Quaternion rot) {
        if (rot == null) {
            this.rotation = null;
            return;
        }
        if (this.rotation == null) {
            this.rotation = new Quaternion();
        }
        this.rotation.set(rot);
    }

    public void setPosition(Vector3f pos) {
        if (pos == null) {
            this.position = null;
            return;
        }
        if (this.position == null) {
            this.position = new Vector3f();
        }
        this.position.set(pos);
    }

    public void clear() {
        this.time = -1.0f;
        this.position = null;
        this.rotation = null;
    }

    public void setIdentity() {
        Keyframe.setIdentity(this.position, this.rotation, this.scale);
    }

    public static void setIdentity(Vector3f pos, Quaternion rot, Vector3f scale) {
        Keyframe.setIfNotNull(pos, 0.0f, 0.0f, 0.0f);
        Keyframe.setIdentityIfNotNull(rot);
        Keyframe.setIfNotNull(scale, 1.0f, 1.0f, 1.0f);
    }

    public static Keyframe lerp(Keyframe a, Keyframe b, float time, Keyframe result) {
        Keyframe.lerp(a, b, time, result.position, result.rotation, result.scale);
        result.none = b.none;
        result.boneName = b.boneName;
        result.time = time;
        return result;
    }

    public static void setIfNotNull(Vector3f to, Vector3f val, float defaultX, float defaultY, float defaultZ) {
        if (to != null) {
            if (val != null) {
                to.set(val);
            } else {
                to.set(defaultX, defaultY, defaultZ);
            }
        }
    }

    public static void setIfNotNull(Vector3f to, float x, float y, float z) {
        if (to != null) {
            to.set(x, y, z);
        }
    }

    public static void setIfNotNull(Quaternion to, Quaternion val) {
        if (to != null) {
            if (val != null) {
                to.set(val);
            } else {
                to.setIdentity();
            }
        }
    }

    public static void setIdentityIfNotNull(Quaternion to) {
        if (to != null) {
            to.setIdentity();
        }
    }

    public static void lerp(Keyframe a, Keyframe b, float time, Vector3f pos, Quaternion rot, Vector3f scale) {
        if (b.time == a.time) {
            b.get(pos, rot, scale);
            return;
        }
        float del = (time - a.time) / (b.time - a.time);
        if (pos != null) {
            PZMath.lerp(pos, a.position, b.position, del);
        }
        if (rot != null) {
            PZMath.slerp(rot, a.rotation, b.rotation, del);
        }
        if (scale != null) {
            PZMath.lerp(scale, a.scale, b.scale, del);
        }
    }
}

