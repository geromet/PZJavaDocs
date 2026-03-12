/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class AnimationVariableWhileAliveFlagCounter
extends PooledObject {
    private AnimationVariableReference variableReference;
    private int counter;
    private static final Pool<AnimationVariableWhileAliveFlagCounter> s_pool = new Pool<AnimationVariableWhileAliveFlagCounter>(AnimationVariableWhileAliveFlagCounter::new);

    private AnimationVariableWhileAliveFlagCounter() {
    }

    public static AnimationVariableWhileAliveFlagCounter alloc(AnimationVariableReference variableReference) {
        AnimationVariableWhileAliveFlagCounter newCounter = s_pool.alloc();
        newCounter.variableReference = variableReference;
        newCounter.counter = 0;
        return newCounter;
    }

    @Override
    public void onReleased() {
        this.variableReference = null;
        this.counter = 0;
        super.onReleased();
    }

    public AnimationVariableReference getVariableRerefence() {
        return this.variableReference;
    }

    public int increment() {
        ++this.counter;
        return this.counter;
    }

    public int decrement() {
        if (this.counter == 0) {
            throw new IndexOutOfBoundsException("Too many decrements. var: " + String.valueOf(this.variableReference));
        }
        --this.counter;
        return this.counter;
    }

    public int getCount() {
        return this.counter;
    }
}

