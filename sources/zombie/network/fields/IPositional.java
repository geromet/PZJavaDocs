/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.fields;

public interface IPositional {
    public float getX();

    public float getY();

    public float getZ();

    default public boolean isInRange(IPositional other, float range) {
        return other != null && Math.abs(this.getX() - other.getX()) < range && Math.abs(this.getY() - other.getY()) < range && Math.abs(this.getZ() - other.getZ()) < range;
    }
}

