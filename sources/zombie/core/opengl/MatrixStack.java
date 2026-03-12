/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.opengl;

import java.util.ArrayDeque;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.opengl.RenderThread;
import zombie.popman.ObjectPool;

public final class MatrixStack {
    private final int mode;
    private final ArrayDeque<Matrix4f> matrices = new ArrayDeque();
    private final ObjectPool<Matrix4f> pool = new ObjectPool<Matrix4f>(Matrix4f::new);

    public MatrixStack(int id) {
        this.mode = id;
    }

    public Matrix4f alloc() {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean bl = true;
        }
        return this.pool.alloc();
    }

    public void release(Matrix4f matrix) {
        this.pool.release(matrix);
    }

    public void push(Matrix4f m) {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean bl = true;
        }
        this.matrices.push(m);
    }

    public void pop() {
        if (Core.debug && Thread.currentThread() != RenderThread.renderThread) {
            boolean bl = true;
        }
        Matrix4f m = this.matrices.pop();
        this.pool.release(m);
    }

    public Matrix4f peek() {
        return this.matrices.peek();
    }

    public boolean isEmpty() {
        return this.matrices.isEmpty();
    }

    public void clear() {
        while (!this.isEmpty()) {
            this.pop();
        }
    }
}

