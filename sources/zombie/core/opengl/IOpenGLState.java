/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

public abstract class IOpenGLState<T extends Value> {
    protected final T currentValue = this.defaultValue();
    private boolean dirty = true;

    public void set(T value) {
        if (this.dirty || !value.equals(this.currentValue)) {
            this.setCurrentValue(value);
            this.Set(value);
        }
    }

    void setCurrentValue(T value) {
        this.dirty = false;
        this.currentValue.set((Value)value);
    }

    public void setDirty() {
        this.dirty = true;
    }

    public void restore() {
        this.dirty = false;
        this.Set(this.getCurrentValue());
    }

    T getCurrentValue() {
        return this.currentValue;
    }

    abstract T defaultValue();

    abstract void Set(T var1);

    public static interface Value {
        public Value set(Value var1);
    }
}

