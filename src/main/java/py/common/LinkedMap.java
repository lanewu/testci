package py.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A "map" that allows for an independent policy for specifying the order of iteration.
 * 
 * @author chenlia
 */
public class LinkedMap<K, V> {

    private final Map<K, LinkedMapEntry<K, V>> map = new HashMap<K, LinkedMapEntry<K, V>>();
    private LinkedMapEntry<K, V> head = null;
    private LinkedMapEntry<K, V> tail = null;

    private LinkedMapEntry<K, V> free = null;
    private int freeCount = 0;
    private static final int MAX_FREE_ENTRIES = 10; // arbitrary

    public V get(K key) {
        checkKeyNotNull(key);
        LinkedMapEntry<K, V> entry = map.get(key);
        return entry == null ? null : entry.getValue();
    }

    public V remove(K key) {
        checkKeyNotNull(key);
        LinkedMapEntry<K, V> entry = map.remove(key);
        if (entry == null)
            return null;
        V existing = entry.getValue();
        removeFromList(entry);
        releaseEntry(entry);
        return existing;
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public K getHead() {
        return head == null ? null : head.getKey();
    }

    public K getSuccessor(K key) {
        checkKeyNotNull(key);
        LinkedMapEntry<K, V> entry = map.get(key);
        if (entry == null)
            throw new NoSuchElementException();
        return entry.succ == null ? null : entry.succ.getKey();
    }

    public K getTail() {
        return tail == null ? null : tail.getKey();
    }

    public K getPredecessor(K key) {
        checkKeyNotNull(key);
        LinkedMapEntry<K, V> entry = map.get(key);
        if (entry == null)
            throw new NoSuchElementException();
        return entry.pred == null ? null : entry.pred.getKey();
    }

    public V putHead(K key, V value) {
        checkKeyNotNull(key);
        V existing = null;
        LinkedMapEntry<K, V> entry = map.get(key);
        if (entry == null) {
            entry = getFreeEntry();
            entry.setKey(key);
            existing = entry.setValue(value);
            map.put(key, entry);
        } else {
            assert key.equals(entry.getKey());
            existing = entry.setValue(value);
            if (entry == head)
                return existing;
            removeFromList(entry);
        }
        insertHead(entry);
        return existing;
    }

    public Map.Entry<K, V> removeTail() {
        if (tail == null) {
            return null;
        }
        K key = tail.getKey();
        V value = remove(key);
        assert value != null;
        return new EntryImpl<K, V>(key, value);
    }
    
    // the caller only cares about value
    public V removeTailToGetValue() {
        if (tail == null) {
            return null;
        }
        K key = tail.getKey();
        return remove(key);
    }

    public V putTail(K key, V value) {
        checkKeyNotNull(key);
        V existing = null;
        LinkedMapEntry<K, V> entry = map.get(key);
        if (entry == null) {
            entry = getFreeEntry();
            entry.setKey(key);
            existing = entry.setValue(value);
            map.put(key, entry);
        } else {
            assert key.equals(entry.getKey());
            existing = entry.setValue(value);
            if (entry == tail)
                return existing;
            removeFromList(entry);
        }
        insertTail(entry);
        return existing;
    }

    /**
     * @return all the keys currently in the map.
     */
    public Collection<K> getKeys() {
        LinkedList<K> result = new LinkedList<K>();
        LinkedMapEntry<K, V> entry = head;
        while (entry != null) {
            result.add(entry.key);
            entry = entry.succ;
        }
        return result;
    }

    private void checkKeyNotNull(K key) {
        if (key == null)
            throw new IllegalArgumentException("key cannot be null");
    }

    private LinkedMapEntry<K, V> getFreeEntry() {
        if (free == null) {
            assert freeCount == 0;
            return new LinkedMapEntry<K, V>();
        }
        freeCount--;
        LinkedMapEntry<K, V> result = free;
        free = result.succ;
        return result;
    }

    private void releaseEntry(LinkedMapEntry<K, V> entry) {
        entry.clear();
        if (freeCount >= MAX_FREE_ENTRIES)
            return;
        entry.succ = free;
        free = entry;
        freeCount++;
    }

    private void insertHead(LinkedMapEntry<K, V> entry) {
        if (head == null) {
            assert tail == null;
            head = entry;
            tail = entry;
            entry.pred = null;
            entry.succ = null;
        } else {
            entry.pred = null;
            entry.succ = head;
            head.pred = entry;
            head = entry;
        }
    }

    private void insertTail(LinkedMapEntry<K, V> entry) {
        if (tail == null) {
            assert head == null;
            head = entry;
            tail = entry;
            entry.pred = null;
            entry.succ = null;
        } else {
            entry.succ = null;
            entry.pred = tail;
            tail.succ = entry;
            tail = entry;
        }
    }

    private void removeFromList(LinkedMapEntry<K, V> entry) {
        if (head == entry && tail == entry) {
            assert entry.pred == null;
            assert entry.succ == null;
            head = null;
            tail = null;
        } else if (head == entry) {
            assert entry.pred == null;
            assert entry.succ != null && entry.succ.pred == entry;
            head = entry.succ;
            head.pred = null;
        } else if (tail == entry) {
            assert entry.pred != null && entry.pred.succ == entry;
            assert entry.succ == null;
            tail = entry.pred;
            tail.succ = null;
        } else {
            assert entry.pred != null && entry.pred.succ == entry;
            assert entry.succ != null && entry.succ.pred == entry;
            entry.pred.succ = entry.succ;
            entry.succ.pred = entry.pred;
        }
        entry.pred = null;
        entry.succ = null;
    }

    final private class EntryImpl<K1, V1> implements Map.Entry<K1, V1> {
        private final K1 key;
        private V1 value;

        public EntryImpl(K1 key, V1 value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K1 getKey() {
            return key;
        }

        @Override
        public V1 getValue() {
            return value;
        }

        @Override
        public V1 setValue(V1 value) {
            V1 old = this.value;
            this.value = value;
            return old;
        }
    }

    private static class LinkedMapEntry<K, V> implements Map.Entry<K, V> {

        private K key;
        private V value;
        LinkedMapEntry<K, V> pred;
        LinkedMapEntry<K, V> succ;

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V newValue) {
            V existing = value;
            value = newValue;
            return existing;
        }

        void setKey(K key) {
            this.key = key;
        }

        void clear() {
            key = null;
            value = null;
            pred = null;
            succ = null;
        }
    }

}
