package joshng.util.collect;

import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import joshng.util.blocks.F;
import joshng.util.blocks.Source;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * User: josh
 * Date: Feb 21, 2010
 * Time: 9:40:12 PM
 */
public class MoreMaps {
    private static final Source MAP_FACTORY = new Source<Map>() { public Map get() {
        return Maps.newHashMap();
    } };
    private static final F MULTISET_ENTRY_AS_MAP_ENTRY = new F<Multiset.Entry, Map.Entry>() {
        @Override public Map.Entry apply(Multiset.Entry input) {
            return Pair.of(input.getElement(), input.getCount());
        }
    };
//    public static <T> Map<T,T> map(T... keysAndValues) {
//        if (keysAndValues.length == 2) {
//            return Collections.singletonMap(keysAndValues[0], keysAndValues[1]);
//        }
//
//        Map<T, T> map = new HashMap<T, T>(keysAndValues.length / 2);
//        for (int i = 0; i < keysAndValues.length; i += 2) {
//            map.put(keysAndValues[i], keysAndValues[i + 1]);
//        }
//        return map;
//    }

    @SuppressWarnings({"unchecked"})
    public static <K,V>Source<Map<K,V>> hashMapFactory() {
        return MAP_FACTORY;
    }

    public static Map<String, String> newStringMap(Object... keysAndValues) {
        Map<String, String> map = new HashMap<String, String>(keysAndValues.length / 2);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i].toString(), keysAndValues[i+1].toString());
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static <K> F<Multiset.Entry<K>, Map.Entry<K,Integer>> multisetEntryAsMapEntry() {
        return MULTISET_ENTRY_AS_MAP_ENTRY;
    }

    public static <K, V> Map<K,V> zipMap(Iterable<K> keys, Iterable<V> values) {
        return zipMap(keys, values, Maps.<K,V>newHashMap());
    }

    public static <K, V> Map<K,V> zipMap(Iterable<K> keys, Iterable<V> values, Map<K,V> into) {
        Iterator<K> keyIter = keys.iterator();
        Iterator<V> valueIter = values.iterator();
        while (keyIter.hasNext()) {
            checkArgument(valueIter.hasNext(), "Mismatched iterable lengths: more keys than values");
            into.put(keyIter.next(), valueIter.next());
        }
        checkArgument(!valueIter.hasNext(), "Mismatched iterable lengths: more values than keys");
        return into;
    }

    private static F GET_ENTRY_KEY = new F<Map.Entry, Object>() { public Object apply(Map.Entry from) {
        return from.getKey();
    } };
    private static F GET_ENTRY_VALUE = new F<Map.Entry, Object>() { public Object apply(Map.Entry from) {
        return from.getValue();
    } };

    @SuppressWarnings({"unchecked"})
    public static <K> F<Map.Entry<K,?>, K> getEntryKey() {
        return (F<Map.Entry<K,?>,K>) GET_ENTRY_KEY;
    }
    @SuppressWarnings({"unchecked"})
    public static <V> F<Map.Entry<?,V>, V> getEntryValue() {
        return (F<Map.Entry<?,V>,V>) GET_ENTRY_VALUE;
    }

    public static <K,V> F<Map<? super K, ? extends V>, V> valueAccessor(final K key) {
        return new F<Map<? super K, ? extends V>, V>() { public V apply(Map<? super K, ? extends V> map) {
            return map.get(key);
        } };
    }

    public static <V> Map<V, Integer> frequencyTable(List<V> items) {
        Map<V, Integer> frequencies = Maps.newHashMap();
        Integer count;
        for (V item : items) {
            count = frequencies.get(item);
            if (count == null) {
                count = 0;
            }
            frequencies.put(item, count + 1);
        }
        return frequencies;
    }
}
