/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.list;

import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import zombie.core.random.Rand;

public class WeightedList<T> {
    private final List<T> list = new ArrayList<T>();
    private final TIntArrayList weight = new TIntArrayList();
    private long totalChance;

    public WeightedList<T> add(T obj, int chance) {
        this.list.add(obj);
        this.weight.add(chance);
        this.totalChance += (long)chance;
        return this;
    }

    public T getRandom() {
        long roll = Rand.Next(this.totalChance);
        long sum = 0L;
        for (int i = 0; i < this.list.size(); ++i) {
            if (roll >= (sum += (long)this.weight.get(i))) continue;
            return this.list.get(i);
        }
        throw new RuntimeException("Weighted list was empty.");
    }

    public boolean isEmpty() {
        return this.list.isEmpty();
    }
}

