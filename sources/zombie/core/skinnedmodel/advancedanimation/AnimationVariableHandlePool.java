/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.util.StringUtils;

public class AnimationVariableHandlePool {
    private static final Object s_threadLock = "AnimationVariableHandlePool.ThreadLock";
    private static final Map<String, AnimationVariableHandle> s_handlePoolMap = new TreeMap<String, AnimationVariableHandle>(String.CASE_INSENSITIVE_ORDER);
    private static final ArrayList<AnimationVariableHandle> s_handlePool = new ArrayList();
    private static volatile int globalIndexGenerator;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static AnimationVariableHandle getOrCreate(String name) {
        Object object = s_threadLock;
        synchronized (object) {
            return AnimationVariableHandlePool.getOrCreateInternal(name);
        }
    }

    private static AnimationVariableHandle getOrCreateInternal(String name) {
        if (StringUtils.isNullOrWhitespace(name)) {
            return null;
        }
        String key = name.trim();
        if (!AnimationVariableHandlePool.isVariableNameValid(key)) {
            return null;
        }
        AnimationVariableHandle handle = s_handlePoolMap.get(key);
        if (handle != null) {
            return handle;
        }
        AnimationVariableHandle newHandle = new AnimationVariableHandle();
        newHandle.setVariableName(key);
        newHandle.setVariableIndex(AnimationVariableHandlePool.generateNewVariableIndex());
        String keyLc = key.toLowerCase();
        s_handlePoolMap.put(keyLc, newHandle);
        s_handlePool.add(newHandle);
        return newHandle;
    }

    private static boolean isVariableNameValid(String name) {
        return !StringUtils.isNullOrWhitespace(name);
    }

    private static int generateNewVariableIndex() {
        return ++globalIndexGenerator;
    }

    public static Iterable<AnimationVariableHandle> all() {
        return () -> new Iterator<AnimationVariableHandle>(){
            private int currentIndex = -1;

            @Override
            public boolean hasNext() {
                return this.currentIndex + 1 < globalIndexGenerator;
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public AnimationVariableHandle next() {
                Object object = s_threadLock;
                synchronized (object) {
                    if (!this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    ++this.currentIndex;
                    return s_handlePool.get(this.currentIndex);
                }
            }
        };
    }
}

