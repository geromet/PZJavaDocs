/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util.reflect;

public final class Annotation {
    private final java.lang.annotation.Annotation annotation;

    Annotation(java.lang.annotation.Annotation annotation) {
        this.annotation = annotation;
    }

    public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationType) {
        if (this.annotation.annotationType().equals(annotationType)) {
            return (T)this.annotation;
        }
        return null;
    }

    public Class<? extends java.lang.annotation.Annotation> getAnnotationType() {
        return this.annotation.annotationType();
    }
}

