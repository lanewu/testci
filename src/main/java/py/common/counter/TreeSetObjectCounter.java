package py.common.counter;

import java.util.*;

public class TreeSetObjectCounter<T> implements ObjectCounter<T> {

    // use an long array to avoid creating too many instance of long
    protected final Map<T, long[]> counter = new HashMap<>();
    protected final TreeSet<T> tSet = new TreeSet<>(this.thenComparingInt(Object::hashCode));

    @Override
    public int compare(T o1, T o2) {
        return Long.compare(get(o1), get(o2));
    }

    @Override
    public long get(T t) {
        long[] var = counter.get(t);
        if (var == null) {
            return 0;
        } else {
            return var[0];
        }
    }

    @Override
    public void increment(T t) {
        long[] var = counter.get(t);
        if (var == null) {
            counter.put(t, new long[] { 1L });
        } else {
            tSet.remove(t);
            var[0]++;
        }
        tSet.add(t);
    }

    @Override
    public void increment(T t, long n) {
        long[] var = counter.get(t);
        if (var == null) {
            counter.put(t, new long[] { n });
        } else {
            tSet.remove(t);
            var[0] += n;
        }
        tSet.add(t);
    }

    @Override
    public void decrement(T t) {
        long[] var = counter.get(t);
        if (var == null) {
            counter.put(t, new long[] { -1L });
        } else {
            tSet.remove(t);
            var[0]--;
        }
        tSet.add(t);
    }

    @Override
    public void decrement(T t, long n) {
        long[] var = counter.get(t);
        if (var == null) {
            counter.put(t, new long[] { -n });
        } else {
            tSet.remove(t);
            var[0] -= n;
        }
        tSet.add(t);
    }

    @Override
    public void set(T t, long n) {
        long[] var = counter.get(t);
        if (var == null) {
            counter.put(t, new long[] { n });
        } else {
            tSet.remove(t);
            var[0] = n;
        }
        tSet.add(t);
    }

    @Override
    public boolean remove(T t) {
        boolean removed = tSet.remove(t);
        counter.remove(t);
        return removed;
    }

    @Override
    public T max() {
        return tSet.last();
    }

    @Override
    public long maxValue() {
        return get(max());
    }

    @Override
    public T min() {
        return tSet.first();
    }

    @Override
    public long minValue() {
        return get(min());
    }

    @Override
    public long total() {
        long sum = 0;
        for (long[] var : counter.values()) {
            sum += var[0];
        }
        return sum;
    }

    @Override
    public int size() {
        return counter.size();
    }

    @Override
    public void clear() {
        tSet.clear();
        counter.clear();
    }

    @Override
    public Iterator<T> iterator() {
        return tSet.iterator();
    }

    public Iterator<T> iterator(Comparator<T> comparator) {
        TreeSet<T> cloneSet = new TreeSet<>(comparator.thenComparingInt(Object::hashCode));
        cloneSet.addAll(tSet);
        return cloneSet.iterator();
    }

    @Override
    public Iterator<T> descendingIterator() {
        return tSet.descendingIterator();
    }

    @Override
    public Collection<T> getAll() {
        List<T> all = new ArrayList<>();
        all.addAll(tSet);
        return all;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TreeSetObjectCounter(").append(System.identityHashCode(this)).append(")")
                                                                    .append(":{counters=[");
        for (T t : tSet) {
            if (get(t) != 0) {
                sb.append(t).append(t.hashCode()).append("=").append(get(t)).append(", ");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public TreeSetObjectCounter<T> deepCopy() {
        TreeSetObjectCounter<T> another = new TreeSetObjectCounter<>();
        for (Map.Entry<T, long[]> tEntry : counter.entrySet()) {
            another.set(tEntry.getKey(), tEntry.getValue()[0]);
        }
        return another;
    }
}
