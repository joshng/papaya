package joshng.util.blocks;

import com.google.common.base.Objects;

import java.util.Map.Entry;

/**
 * User: josh
 * Date: 3/16/12
 * Time: 9:04 AM
 */
public interface Pred2<K,V> extends Pred<Entry<? extends K,? extends V>> {
    public static final Pred2<Object, Object> EQUAL_PARAMS = new Pred2<Object, Object>() {
        public boolean test(Object key, Object value) {
            return Objects.equal(key, value);
        }
    };

    default boolean test(Entry<? extends K, ? extends V> input) {
        return test(input.getKey(), input.getValue());
    }

    boolean test(K key, V value);

    default Pred2<V,K> flip() {
        return new Pred2<V, K>() {
            public boolean test(V key, K value) {
                return Pred2.this.test(value, key);
            }
        };
    }

    default Pred<V> bindFirst(final K key) {
        return new Pred<V>() {
            public boolean test(V input) {
                return Pred2.this.test(key, input);
            }
        };
    }

    default Pred<K> bindSecond(final V value) {
        return new Pred<K>() {
            public boolean test(K input) {
                return Pred2.this.test(input, value);
            }
        };
    }

    default Pred2<K,V> negated() {
        return new Pred2<K, V>() {
            public boolean test(K key, V value) {
                return !Pred2.this.test(key, value);
            }
        };
    }
}
