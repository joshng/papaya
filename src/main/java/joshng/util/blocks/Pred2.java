package joshng.util.blocks;

import com.google.common.base.Objects;

import java.util.Map.Entry;

/**
 * User: josh
 * Date: 3/16/12
 * Time: 9:04 AM
 */
public abstract class Pred2<K,V> extends Pred<Entry<? extends K,? extends V>> {
    public static final Pred2<Object, Object> EQUAL_PARAMS = new Pred2<Object, Object>() {
        public boolean apply(Object key, Object value) {
            return Objects.equal(key, value);
        }
    };

    public boolean apply(Entry<? extends K, ? extends V> input) {
        return apply(input.getKey(), input.getValue());
    }

    public abstract boolean apply(K key, V value);

    public Pred2<V,K> flip() {
        return new Pred2<V, K>() {
            public boolean apply(V key, K value) {
                return Pred2.this.apply(value, key);
            }
        };
    }

    public Pred<V> bindFirst(final K key) {
        return new Pred<V>() {
            public boolean apply(V input) {
                return Pred2.this.apply(key, input);
            }
        };
    }

    public Pred<K> bindSecond(final V value) {
        return new Pred<K>() {
            public boolean apply(K input) {
                return Pred2.this.apply(input, value);
            }
        };
    }

    public Pred2<K,V> negated() {
        return new Pred2<K, V>() {
            public boolean apply(K key, V value) {
                return !Pred2.this.apply(key, value);
            }
        };
    }
}
