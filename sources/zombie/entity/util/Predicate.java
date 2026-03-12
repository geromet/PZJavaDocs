/*
 * Decompiled with CFR 0.152.
 */
package zombie.entity.util;

import java.util.Iterator;
import zombie.entity.util.Collections;

public interface Predicate<T> {
    public boolean evaluate(T var1);

    public static class PredicateIterable<T>
    implements Iterable<T> {
        public Iterable<T> iterable;
        public Predicate<T> predicate;
        public PredicateIterator<T> iterator;

        public PredicateIterable(Iterable<T> iterable, Predicate<T> predicate) {
            this.set(iterable, predicate);
        }

        public void set(Iterable<T> iterable, Predicate<T> predicate) {
            this.iterable = iterable;
            this.predicate = predicate;
        }

        @Override
        public Iterator<T> iterator() {
            if (Collections.allocateIterators) {
                return new PredicateIterator<T>(this.iterable.iterator(), this.predicate);
            }
            if (this.iterator == null) {
                this.iterator = new PredicateIterator<T>(this.iterable.iterator(), this.predicate);
            } else {
                this.iterator.set(this.iterable.iterator(), this.predicate);
            }
            return this.iterator;
        }
    }

    public static class PredicateIterator<T>
    implements Iterator<T> {
        public Iterator<T> iterator;
        public Predicate<T> predicate;
        public boolean end;
        public boolean peeked;
        public T next;

        public PredicateIterator(Iterable<T> iterable, Predicate<T> predicate) {
            this(iterable.iterator(), predicate);
        }

        public PredicateIterator(Iterator<T> iterator2, Predicate<T> predicate) {
            this.set(iterator2, predicate);
        }

        public void set(Iterable<T> iterable, Predicate<T> predicate) {
            this.set(iterable.iterator(), predicate);
        }

        public void set(Iterator<T> iterator2, Predicate<T> predicate) {
            this.iterator = iterator2;
            this.predicate = predicate;
            this.peeked = false;
            this.end = false;
            this.next = null;
        }

        @Override
        public boolean hasNext() {
            if (this.end) {
                return false;
            }
            if (this.next != null) {
                return true;
            }
            this.peeked = true;
            while (this.iterator.hasNext()) {
                T n = this.iterator.next();
                if (!this.predicate.evaluate(n)) continue;
                this.next = n;
                return true;
            }
            this.end = true;
            return false;
        }

        @Override
        public T next() {
            if (this.next == null && !this.hasNext()) {
                return null;
            }
            T result = this.next;
            this.next = null;
            this.peeked = false;
            return result;
        }

        @Override
        public void remove() {
            if (this.peeked) {
                throw new RuntimeException("Cannot remove between a call to hasNext() and next().");
            }
            this.iterator.remove();
        }
    }
}

