package joshng.util.collect;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static joshng.util.collect.Functional.funPairs;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 12:10 PM
 */
public class ImmutableMaybeMap<K, V> {
    private final Map<K, Maybe<V>> map;

    public ImmutableMaybeMap(Map<K, V> map) {
        this(funPairs(map));
    }

    public ImmutableMaybeMap(Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
        this(funPairs(entries));
    }

    public ImmutableMaybeMap(FunPairs<K, V> entries) {
        map = entries.mapValues(Maybe.<V>definitely()).toMap();
    }

    @Nonnull
    public V getOrThrow(K key) {
        return getMaybe(key).getOrThrow("Unrecognized key", key);
    }

    public Maybe<V> getMaybe(K key) {
        return Maybe.nullToMaybeNot(map.get(key));
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public FunPairs<K, V> entries() {
        return funPairs(Maps.transformValues(map, Maybe.<V>getter()));
    }
}

