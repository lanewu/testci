package py.common.counter;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public interface ObjectCounter<T> extends Comparator<T>{
    long get(T t);

    void increment(T t);

    void increment(T t, long n);

    void decrement(T t);

    void decrement(T t, long n);

    void set(T t, long n);

    boolean remove(T t);

    T max();

    long maxValue();

    T min();

    long minValue();

    long total();

    int size();

    void clear();

    Iterator<T> iterator();

    Iterator<T> iterator(Comparator<T> comparator);

    Iterator<T> descendingIterator();

    Collection<T> getAll();

    ObjectCounter<T> deepCopy();

}
