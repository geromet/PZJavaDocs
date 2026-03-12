/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import zombie.core.math.PZMath;
import zombie.core.random.RandInterface;
import zombie.core.random.RandStandard;
import zombie.util.ICloner;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;
import zombie.util.lambda.Predicates;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZConvertIterable;
import zombie.util.list.PZConvertList;
import zombie.util.list.PZEmptyIterable;
import zombie.util.list.PrimitiveFloatList;

public class PZArrayUtil {
    public static final int[] emptyIntArray = new int[0];
    public static final float[] emptyFloatArray = new float[0];

    public static <E> E pickRandom(E[] collection, RandInterface rnd) {
        if (collection.length == 0) {
            return null;
        }
        int randomIndex = rnd.Next(collection.length);
        return collection[randomIndex];
    }

    public static <E> E pickRandom(List<E> collection, RandInterface rnd) {
        if (collection.isEmpty()) {
            return null;
        }
        int randomIndex = rnd.Next(collection.size());
        return collection.get(randomIndex);
    }

    public static <E> E pickRandom(Collection<E> collection, RandInterface rnd) {
        if (collection.isEmpty()) {
            return null;
        }
        int randomIndex = rnd.Next(collection.size());
        return PZArrayUtil.getElementAt(collection, randomIndex);
    }

    public static <E> E pickRandom(Iterable<E> collection, RandInterface rnd) {
        int size = PZArrayUtil.getSize(collection);
        if (size == 0) {
            return null;
        }
        int randomIndex = rnd.Next(size);
        return PZArrayUtil.getElementAt(collection, randomIndex);
    }

    public static <E> E pickRandom(E[] collection) {
        return PZArrayUtil.pickRandom(collection, (RandInterface)RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(List<E> collection) {
        return PZArrayUtil.pickRandom(collection, (RandInterface)RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(Collection<E> collection) {
        return PZArrayUtil.pickRandom(collection, (RandInterface)RandStandard.INSTANCE);
    }

    public static <E> E pickRandom(Iterable<E> collection) {
        return PZArrayUtil.pickRandom(collection, (RandInterface)RandStandard.INSTANCE);
    }

    public static <E> int getSize(Iterable<E> collection) {
        int count = 0;
        Iterator<E> it = collection.iterator();
        while (it.hasNext()) {
            ++count;
            it.next();
        }
        return count;
    }

    public static <E> E getElementAt(Iterable<E> collection, int index) throws ArrayIndexOutOfBoundsException {
        E item = null;
        Iterator<E> it = collection.iterator();
        for (int i = 0; i <= index; ++i) {
            if (!it.hasNext()) {
                throw new ArrayIndexOutOfBoundsException(i);
            }
            if (i != index) continue;
            item = it.next();
        }
        return item;
    }

    public static <E> void copy(List<E> target, List<E> source2) {
        PZArrayUtil.copy(target, source2, elem -> elem);
    }

    public static <E> void move(List<E> target, List<E> source2) {
        PZArrayUtil.move(target, source2, elem -> elem);
    }

    public static <E, S> void copy(List<E> target, List<S> source2, ICloner<E, S> elementCloner) {
        if (target == source2) {
            return;
        }
        target.clear();
        for (int i = 0; i < source2.size(); ++i) {
            S srcE = source2.get(i);
            target.add(elementCloner.clone(srcE));
        }
    }

    public static <E, S> void move(List<E> target, List<S> source2, ICloner<E, S> elementCloner) {
        if (target == source2) {
            return;
        }
        PZArrayUtil.copy(target, source2, elementCloner);
        source2.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E, E1> int indexOf(E[] collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        try {
            int collectionLength = PZArrayUtil.lengthOf(collection);
            for (int i = 0; i < collectionLength; ++i) {
                E element = collection[i];
                if (!predicate.test(containsItem, element)) continue;
                int n = i;
                return n;
            }
            int n = -1;
            return n;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E, E1> int indexOf(List<E> collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        try {
            int foundIdx = -1;
            for (int i = 0; i < collection.size(); ++i) {
                E element = collection.get(i);
                if (!predicate.test(containsItem, element)) continue;
                foundIdx = i;
                break;
            }
            int n = foundIdx;
            return n;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> int indexOf(E[] collection, Predicate<E> predicate) {
        try {
            int collectionLength = PZArrayUtil.lengthOf(collection);
            for (int i = 0; i < collectionLength; ++i) {
                E element = collection[i];
                if (!predicate.test(element)) continue;
                int n = i;
                return n;
            }
            int n = -1;
            return n;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> int indexOf(List<E> collection, Predicate<E> predicate) {
        try {
            int foundIdx = -1;
            for (int i = 0; i < collection.size(); ++i) {
                E element = collection.get(i);
                if (!predicate.test(element)) continue;
                foundIdx = i;
                break;
            }
            int n = foundIdx;
            return n;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E> boolean contains(E[] collection, int count, E e) {
        return PZArrayUtil.indexOf(collection, count, e) != -1;
    }

    public static <E, E1> boolean contains(E[] collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        return PZArrayUtil.indexOf(collection, containsItem, predicate) > -1;
    }

    public static <E, E1> boolean contains(List<E> collection, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        return PZArrayUtil.indexOf(collection, containsItem, predicate) > -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E, E1> boolean contains(Collection<E> it, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        if (it instanceof List) {
            List es = (List)it;
            return PZArrayUtil.contains(es, containsItem, predicate);
        }
        try {
            boolean contains = false;
            for (E val : it) {
                if (!predicate.test(containsItem, val)) continue;
                contains = true;
                break;
            }
            boolean bl = contains;
            return bl;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E, E1> boolean contains(Iterable<E> it, E1 containsItem, Predicates.Params1.ICallback<E1, E> predicate) {
        if (it instanceof List) {
            List es = (List)it;
            return PZArrayUtil.indexOf(es, containsItem, predicate) > -1;
        }
        try {
            boolean contains = false;
            for (E val : it) {
                if (!predicate.test(containsItem, val)) continue;
                contains = true;
                break;
            }
            boolean bl = contains;
            return bl;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E> boolean contains(E[] collection, Predicate<E> predicate) {
        return PZArrayUtil.indexOf(collection, predicate) > -1;
    }

    public static <E> boolean contains(List<E> collection, Predicate<E> predicate) {
        return PZArrayUtil.indexOf(collection, predicate) > -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> boolean contains(Collection<E> it, Predicate<E> predicate) {
        if (it instanceof List) {
            List es = (List)it;
            return PZArrayUtil.contains(es, predicate);
        }
        try {
            boolean contains = false;
            for (E val : it) {
                if (!predicate.test(val)) continue;
                contains = true;
                break;
            }
            boolean bl = contains;
            return bl;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> boolean contains(Iterable<E> it, Predicate<E> predicate) {
        if (it instanceof List) {
            List es = (List)it;
            return PZArrayUtil.indexOf(es, predicate) > -1;
        }
        try {
            boolean contains = false;
            for (E val : it) {
                if (!predicate.test(val)) continue;
                contains = true;
                break;
            }
            boolean bl = contains;
            return bl;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E> E find(E[] collection, Predicate<E> predicate) {
        int indexOf = PZArrayUtil.indexOf(collection, predicate);
        if (indexOf > -1) {
            return collection[indexOf];
        }
        return null;
    }

    public static <E> E find(List<E> collection, Predicate<E> predicate) {
        int indexOf = PZArrayUtil.indexOf(collection, predicate);
        if (indexOf > -1) {
            return collection.get(indexOf);
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> E find(Iterable<E> collection, Predicate<E> predicate) {
        if (collection instanceof List) {
            List es = (List)collection;
            return PZArrayUtil.find(es, predicate);
        }
        try {
            for (E element : collection) {
                if (!predicate.test(element)) continue;
                E e = element;
                return e;
            }
            Iterator<E> iterator2 = null;
            return (E)iterator2;
        }
        finally {
            Pool.tryRelease(predicate);
        }
    }

    public static <E, S> List<E> listConvert(List<S> source2, Function<S, E> converter) {
        if (source2.isEmpty()) {
            return PZArrayList.emptyList();
        }
        return new PZConvertList<S, E>(source2, converter);
    }

    public static <E, S> Iterable<E> itConvert(Iterable<S> source2, Function<S, E> converter) {
        return new PZConvertIterable<E, S>(source2, converter);
    }

    public static <E, S> List<E> listConvert(List<S> source2, List<E> dest, Function<S, E> converter) {
        dest.clear();
        for (int i = 0; i < source2.size(); ++i) {
            dest.add(converter.apply(source2.get(i)));
        }
        return dest;
    }

    public static <E> int lengthOf(E[] array) {
        return array != null ? array.length : 0;
    }

    public static int lengthOf(int[] array) {
        return array != null ? array.length : 0;
    }

    public static int lengthOf(float[] array) {
        return array != null ? array.length : 0;
    }

    public static <T extends Enum<T>> Map<String, T> generateNameToEnumLookUpTable(Class<T> enumClass, Function<T, String> nameProvider) {
        HashMap<String, Enum> map = new HashMap<String, Enum>();
        for (Enum val : (Enum[])enumClass.getEnumConstants()) {
            map.put(nameProvider.apply(val), val);
        }
        return map;
    }

    public static <E, S, T1> List<E> listConvert(List<S> source2, List<E> dest, T1 v1, IListConverter1Param<S, E, T1> converter) {
        dest.clear();
        for (int i = 0; i < source2.size(); ++i) {
            dest.add(converter.convert(source2.get(i), v1));
        }
        return dest;
    }

    private static <E> List<E> asList(E[] list) {
        return Arrays.asList(list);
    }

    private static List<Float> asList(float[] list) {
        return new PrimitiveFloatList(list);
    }

    private static <E> Iterable<E> asSafeIterable(E[] array) {
        return array != null ? PZArrayUtil.asList(array) : PZEmptyIterable.getInstance();
    }

    private static Iterable<Float> asSafeIterable(float[] array) {
        return array != null ? PZArrayUtil.asList(array) : PZEmptyIterable.getInstance();
    }

    public static String arrayToString(float[] list) {
        return PZArrayUtil.arrayToString(PZArrayUtil.asSafeIterable(list));
    }

    public static String arrayToString(float[] list, String prefix, String suffix, String delimiter) {
        return PZArrayUtil.arrayToString(PZArrayUtil.asSafeIterable(list), prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(E[] list) {
        return PZArrayUtil.arrayToString(PZArrayUtil.asSafeIterable(list));
    }

    public static <E> String arrayToString(E[] list, String prefix, String suffix, String delimiter) {
        return PZArrayUtil.arrayToString(PZArrayUtil.asSafeIterable(list), prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(E[] list, Function<E, String> toString2, String prefix, String suffix, String delimiter) {
        return PZArrayUtil.arrayToString(PZArrayUtil.asSafeIterable(list), toString2, prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(Iterable<E> list, Function<E, String> toString2) {
        return PZArrayUtil.arrayToString(list, toString2, "{", "}", System.lineSeparator());
    }

    public static <E> String arrayToString(Iterable<E> list) {
        return PZArrayUtil.arrayToString(list, String::valueOf, "{", "}", System.lineSeparator());
    }

    public static <E> String arrayToString(Iterable<E> list, String prefix, String suffix, String delimiter) {
        return PZArrayUtil.arrayToString(list, String::valueOf, prefix, suffix, delimiter);
    }

    public static <E> String arrayToString(Iterable<E> list, Function<E, String> toString2, String prefix, String suffix, String delimiter) {
        StringBuilder result = new StringBuilder(prefix);
        if (list != null) {
            boolean isFirst = true;
            for (E item : list) {
                if (!isFirst) {
                    result.append(delimiter);
                }
                String stringVal = toString2.apply(item);
                result.append(stringVal);
                isFirst = false;
            }
        }
        result.append(suffix);
        Pool.tryRelease(toString2);
        return result.toString();
    }

    public static <E> E[] newInstance(Class<?> componentType, int length) {
        return (Object[])Array.newInstance(componentType, length);
    }

    public static <E> E[] newInstance(Class<?> componentType, int length, Supplier<E> allocator) {
        E[] newArray = PZArrayUtil.newInstance(componentType, length);
        int count = newArray.length;
        for (int i = 0; i < count; ++i) {
            newArray[i] = allocator.get();
        }
        return newArray;
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength) {
        return PZArrayUtil.newInstance(componentType, reusableArray, newLength, false, () -> null);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, boolean growOnly) {
        return PZArrayUtil.newInstance(componentType, reusableArray, newLength, growOnly, () -> null);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, Supplier<E> newAllocator) {
        return PZArrayUtil.newInstance(componentType, reusableArray, newLength, false, newAllocator);
    }

    public static <E> E[] newInstance(Class<?> componentType, E[] reusableArray, int newLength, boolean growOnly, Supplier<E> newAllocator) {
        int i;
        if (reusableArray == null) {
            return PZArrayUtil.newInstance(componentType, newLength, newAllocator);
        }
        int oldLength = reusableArray.length;
        if (oldLength == newLength) {
            return reusableArray;
        }
        if (growOnly && oldLength > newLength) {
            return reusableArray;
        }
        E[] newArray = PZArrayUtil.newInstance(componentType, newLength);
        PZArrayUtil.arrayCopy(newArray, reusableArray, 0, PZMath.min(newLength, oldLength));
        if (newLength > oldLength) {
            for (i = oldLength; i < newLength; ++i) {
                newArray[i] = newAllocator.get();
            }
        }
        if (newLength < oldLength) {
            for (i = newLength; i < oldLength; ++i) {
                reusableArray[i] = Pool.tryRelease(reusableArray[i]);
            }
        }
        return newArray;
    }

    public static float[] add(float[] array, float val) {
        int lengthOf = PZArrayUtil.lengthOf(array);
        float[] newArray = new float[lengthOf + 1];
        PZArrayUtil.arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static int[] add(int[] array, int val) {
        int lengthOf = PZArrayUtil.lengthOf(array);
        int[] newArray = new int[lengthOf + 1];
        PZArrayUtil.arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static <E> E[] add(E[] array, E val) {
        int lengthOf = PZArrayUtil.lengthOf(array);
        E[] newArray = PZArrayUtil.newInstance(array.getClass().getComponentType(), lengthOf + 1);
        PZArrayUtil.arrayCopy(newArray, array, 0, lengthOf);
        newArray[lengthOf] = val;
        return newArray;
    }

    public static <E> E[] concat(E[] arrayA, E[] arrayB) {
        boolean arrayBEmpty;
        boolean arrayAEmpty = arrayA == null || arrayA.length == 0;
        boolean bl = arrayBEmpty = arrayB == null || arrayB.length == 0;
        if (arrayAEmpty && arrayBEmpty) {
            return null;
        }
        if (arrayAEmpty) {
            return PZArrayUtil.shallowClone(arrayB);
        }
        if (arrayBEmpty) {
            return arrayA;
        }
        E[] newArray = PZArrayUtil.newInstance(arrayA.getClass().getComponentType(), arrayA.length + arrayB.length);
        PZArrayUtil.arrayCopy(newArray, arrayA, 0, arrayA.length);
        PZArrayUtil.arrayCopy(newArray, arrayB, arrayA.length, newArray.length);
        return newArray;
    }

    public static <E, S extends E> E[] arrayCopy(E[] to, S[] from, int startIdx, int endIdx) {
        return PZArrayUtil.arrayCopy(to, from, startIdx, endIdx, null, null);
    }

    public static <E, S extends E> E[] arrayCopy(E[] to, S[] from, int startIdx, int endIdx, Supplier<E> allocator, Invokers.Params2.ICallback<E, S> copier) {
        if (copier != null) {
            for (int i = startIdx; i < endIdx; ++i) {
                if (to[i] == null && allocator != null) {
                    to[i] = allocator.get();
                }
                copier.accept(to[i], from[i]);
            }
        } else {
            for (int i = startIdx; i < endIdx; ++i) {
                to[i] = from[i];
            }
        }
        return to;
    }

    public static float[] arrayCopy(float[] to, float[] from, int startIdx, int endIdx) {
        for (int i = startIdx; i < endIdx; ++i) {
            to[i] = from[i];
        }
        return to;
    }

    public static int[] arrayCopy(int[] to, int[] from, int startIdx, int endIdx) {
        for (int i = startIdx; i < endIdx; ++i) {
            to[i] = from[i];
        }
        return to;
    }

    public static <L extends List<E>, E> L arrayCopy(L to, List<? extends E> from) {
        to.clear();
        to.addAll(from);
        return to;
    }

    public static <E> E[] arrayCopy(E[] to, List<? extends E> from) {
        for (int i = 0; i < from.size(); ++i) {
            to[i] = from.get(i);
        }
        return to;
    }

    public static <E, S extends E> E[] arrayCopy(E[] to, S[] from) {
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    public static <L extends List<E>, E, S> L arrayConvert(L to, List<S> from, Function<S, E> converter) {
        to.clear();
        int size = from.size();
        for (int i = 0; i < size; ++i) {
            S fromVal = from.get(i);
            to.add(converter.apply(fromVal));
        }
        return to;
    }

    public static float[] clone(float[] src) {
        if (PZArrayUtil.isNullOrEmpty(src)) {
            return src;
        }
        float[] copy = new float[src.length];
        PZArrayUtil.arrayCopy(copy, src, 0, src.length);
        return copy;
    }

    public static <E> E[] clone(E[] src, Supplier<E> allocator, Invokers.Params2.ICallback<E, E> copier) {
        if (PZArrayUtil.isNullOrEmpty(src)) {
            return src;
        }
        E[] copy = PZArrayUtil.newInstance(src.getClass().getComponentType(), src.length);
        PZArrayUtil.arrayCopy(copy, src, 0, src.length, allocator, copier);
        return copy;
    }

    public static <E> E[] shallowClone(E[] src) {
        return PZArrayUtil.clone(src, null, null);
    }

    public static <E> boolean isNullOrEmpty(E[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNullOrEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNullOrEmpty(float[] array) {
        return array == null || array.length == 0;
    }

    public static <E> boolean isNullOrEmpty(List<E> list) {
        return list == null || list.isEmpty();
    }

    public static <E> boolean isNullOrEmpty(Iterable<E> it) {
        boolean isEmpty;
        block1: {
            if (it instanceof List) {
                List es = (List)it;
                return PZArrayUtil.isNullOrEmpty(es);
            }
            isEmpty = true;
            Iterator<E> iterator2 = it.iterator();
            if (!iterator2.hasNext()) break block1;
            E e = iterator2.next();
            isEmpty = false;
        }
        return isEmpty;
    }

    public static <E> E getOrDefault(List<E> list, int i) {
        return PZArrayUtil.getOrDefault(list, i, null);
    }

    public static <E> E getOrDefault(List<E> list, int i, E defaultVal) {
        if (i >= 0 && i < list.size()) {
            return list.get(i);
        }
        return defaultVal;
    }

    public static <E> E getOrDefault(E[] list, int i, E defaultVal) {
        if (list != null && i >= 0 && i < list.length) {
            return list[i];
        }
        return defaultVal;
    }

    public static float getOrDefault(float[] list, int i, float defaultVal) {
        if (list != null && i >= 0 && i < list.length) {
            return list[i];
        }
        return defaultVal;
    }

    public static int[] arraySet(int[] arr, int val) {
        if (PZArrayUtil.isNullOrEmpty(arr)) {
            return arr;
        }
        int count = arr.length;
        for (int i = 0; i < count; ++i) {
            arr[i] = val;
        }
        return arr;
    }

    public static float[] arraySet(float[] arr, float val) {
        if (PZArrayUtil.isNullOrEmpty(arr)) {
            return arr;
        }
        int count = arr.length;
        for (int i = 0; i < count; ++i) {
            arr[i] = val;
        }
        return arr;
    }

    public static <E> E[] arraySet(E[] arr, E val) {
        if (PZArrayUtil.isNullOrEmpty(arr)) {
            return arr;
        }
        int count = arr.length;
        for (int i = 0; i < count; ++i) {
            arr[i] = val;
        }
        return arr;
    }

    public static <E> E[] arrayPopulate(E[] arr, Supplier<E> supplier) {
        return PZArrayUtil.arrayPopulate(arr, supplier, 0, PZArrayUtil.lengthOf(arr));
    }

    public static <E> E[] arrayPopulate(E[] arr, Supplier<E> supplier, int startIdx, int endIdx) {
        if (PZArrayUtil.isNullOrEmpty(arr)) {
            return arr;
        }
        for (int i = startIdx; i < endIdx; ++i) {
            arr[i] = supplier.get();
        }
        return arr;
    }

    public static void insertAt(int[] arr, int insertAt, int val) {
        for (int i = arr.length - 1; i > insertAt; --i) {
            arr[i] = arr[i - 1];
        }
        arr[insertAt] = val;
    }

    public static void insertAt(float[] arr, int insertAt, float val) {
        for (int i = arr.length - 1; i > insertAt; --i) {
            arr[i] = arr[i - 1];
        }
        arr[insertAt] = val;
    }

    public static <E> E[] toArray(List<E> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        E[] newArray = PZArrayUtil.newInstance(list.get(0).getClass(), list.size());
        PZArrayUtil.arrayCopy(newArray, list);
        return newArray;
    }

    public static <E> int indexOf(E[] arr, int count, E val) {
        for (int i = 0; i < count; ++i) {
            if (arr[i] != val) continue;
            return i;
        }
        return -1;
    }

    public static int indexOf(float[] arr, int count, float val) {
        for (int i = 0; i < count; ++i) {
            if (arr[i] != val) continue;
            return i;
        }
        return -1;
    }

    public static boolean contains(float[] arr, int count, float val) {
        return PZArrayUtil.indexOf(arr, count, val) != -1;
    }

    public static int indexOf(int[] arr, int count, int val) {
        for (int i = 0; i < count; ++i) {
            if (arr[i] != val) continue;
            return i;
        }
        return -1;
    }

    public static boolean contains(int[] arr, int count, int val) {
        return PZArrayUtil.indexOf(arr, count, val) != -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> void forEach(List<E> list, Consumer<? super E> consumer) {
        try {
            if (list == null) {
                return;
            }
            int count = list.size();
            for (int i = 0; i < count; ++i) {
                E element = list.get(i);
                consumer.accept(element);
            }
        }
        finally {
            Pool.tryRelease(consumer);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> void forEach(Iterable<E> it, Consumer<? super E> consumer) {
        if (it == null) {
            Pool.tryRelease(consumer);
            return;
        }
        if (it instanceof List) {
            List es = (List)it;
            PZArrayUtil.forEach(es, consumer);
            return;
        }
        try {
            for (E element : it) {
                consumer.accept(element);
            }
        }
        finally {
            Pool.tryRelease(consumer);
        }
    }

    public static <E> void forEach(E[] elements, Consumer<? super E> consumer) {
        if (PZArrayUtil.isNullOrEmpty(elements)) {
            return;
        }
        int elementsLength = elements.length;
        for (int i = 0; i < elementsLength; ++i) {
            consumer.accept(elements[i]);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <E> void forEachReplace(List<E> list, Function<? super E, ? super E> replacer) {
        try {
            if (list == null) {
                return;
            }
            int count = list.size();
            for (int i = 0; i < count; ++i) {
                E element = list.get(i);
                E replacement = replacer.apply(element);
                list.set(i, replacement);
            }
        }
        finally {
            Pool.tryRelease(replacer);
        }
    }

    public static <K, V> V getOrCreate(HashMap<K, V> map, K key, Supplier<V> allocator) {
        V val = map.get(key);
        if (val == null) {
            val = allocator.get();
            map.put(key, val);
        }
        return val;
    }

    public static <E> void sort(Stack<E> stack, Comparator<E> comparator) {
        try {
            stack.sort(comparator);
        }
        finally {
            Pool.tryRelease(comparator);
        }
    }

    public static <E> void sort(List<E> list, Comparator<E> comparator) {
        try {
            list.sort(comparator);
        }
        finally {
            Pool.tryRelease(comparator);
        }
    }

    public static <E> boolean sequenceEqual(E[] a, List<? extends E> b) {
        return PZArrayUtil.sequenceEqual(a, b, Comparators::objectsEqual);
    }

    public static <E> boolean sequenceEqual(E[] a, List<? extends E> b, Comparator<E> comparator) {
        return a.length == b.size() && PZArrayUtil.sequenceEqual(PZArrayUtil.asList(a), b, comparator);
    }

    public static <E> boolean sequenceEqual(List<? extends E> a, List<? extends E> b) {
        return PZArrayUtil.sequenceEqual(a, b, Comparators::objectsEqual);
    }

    public static <E> boolean sequenceEqual(List<? extends E> a, List<? extends E> b, Comparator<E> comparator) {
        if (a.size() != b.size()) {
            return false;
        }
        boolean equals = true;
        int count = a.size();
        for (int i = 0; i < count; ++i) {
            E valB;
            E valA = a.get(i);
            if (comparator.compare(valA, valB = b.get(i)) == 0) continue;
            equals = false;
            break;
        }
        return equals;
    }

    public static int[] arrayAdd(int[] a, int[] b) {
        for (int i = 0; i < a.length; ++i) {
            int n = i;
            a[n] = a[n] + b[i];
        }
        return a;
    }

    public static <E> void addAll(ArrayList<E> dest, List<E> src) {
        dest.ensureCapacity(dest.size() + src.size());
        for (int i = 0; i < src.size(); ++i) {
            dest.add(src.get(i));
        }
    }

    public static <E> void addAll(PZArrayList<E> dest, List<E> src) {
        dest.ensureCapacity(dest.size() + src.size());
        for (int i = 0; i < src.size(); ++i) {
            dest.add(src.get(i));
        }
    }

    public static interface IListConverter1Param<S, E, T1> {
        public E convert(S var1, T1 var2);
    }

    public static class Comparators {
        public static <E> int referencesEqual(E a, E b) {
            return a == b ? 0 : 1;
        }

        public static <E> int objectsEqual(E a, E b) {
            return a != null && a.equals(b) ? 0 : 1;
        }

        public static int equalsIgnoreCase(String a, String b) {
            return StringUtils.equals(a, b) ? 0 : 1;
        }
    }
}

