package joshng.util.collect;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multiset;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
* User: josh
* Date: 4/29/13
* Time: 12:54 PM
*/
public class AtomicIntMap<T> {
    private final Multiset<T> multiset = ConcurrentHashMultiset.create();

    public static <T> AtomicIntMap<T> newAtomicIntMap() {
        return new AtomicIntMap<T>();
    }

    public Map<T, Integer> intView() {
        return new AbstractMap<T, Integer>() {
            @Override public Set<Entry<T, Integer>> entrySet() {
                return new AbstractSet<Entry<T, Integer>>() {
                    @Override public Iterator<Entry<T, Integer>> iterator() {
                        return Iterators.transform(multiset.entrySet().iterator(), MoreMaps.<T>multisetEntryAsMapEntry());
                    }

                    @Override public int size() {
                        return multiset.elementSet().size();
                    }

                    @Override public boolean isEmpty() {
                        return multiset.isEmpty();
                    }

                    @Override public boolean contains(Object o) {
                        return multiset.entrySet().contains(o);
                    }
                };
            }

            @Override public int size() {
                return multiset.elementSet().size();
            }

            @Override public boolean isEmpty() {
                return multiset.isEmpty();
            }

            @Override public boolean containsKey(Object key) {
                return multiset.contains(key);
            }

            @Override public Integer get(Object key) {
                return multiset.count(key);
            }

            @Override public Integer put(T key, Integer value) {
                return multiset.setCount(key, value);
            }
        };
    }

    public int add(T key, int addend) {
        return multiset.add(key, addend) + addend;
    }

    public int increment(T key) {
        return multiset.add(key, 1) + 1;
    }

    public int decrement(T key) {
        return multiset.add(key, -1) - 1;
    }

    /**
     * sets the value associated with the given key
     * @return the previous value, or 0 if the key was not present
     */
    public int putInt(T key, int newValue)  {
        return multiset.setCount(key, newValue);
    }

    /**
     * gets the value associated with the given key, <em>without</em> creating an entry if the key is not already present
     * @return the int value associated with the given key, or 0 if the key is not present
     */
    public int getInt(T key) {
        return multiset.count(key);
    }

    public int sum() {
        return multiset.size();
    }

    public Set<T> keySet() {
        return multiset.elementSet();
    }

    public boolean containsKey(T key) {
        return multiset.contains(key);
    }
}
