/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package joshng.util.collect;

import com.google.common.base.Function;
import com.google.common.collect.PublicImmutableEntry;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static joshng.util.Reflect.blindCast;

@Immutable
public class Pair<T, U> extends PublicImmutableEntry<T, U> {
    private static final F GET_FIRST = new F<Map.Entry, Object>() { public Object apply(Map.Entry pair) {
            return pair.getKey();
    } };
    private static final F GET_SECOND = new F<Map.Entry, Object>() { public Object apply(Map.Entry pair) {
            return pair.getValue();
    } };
    private static final F2 FACTORY = new F2() {
        public Pair apply(Object input1, Object input2) {
            return new Pair<>(input1, input2);
        }
    };
    public static final F SWAPPER = new F<Map.Entry, Map.Entry>() {
        @Override
        public Map.Entry apply(Map.Entry input) {
            return Pair.of(input.getValue(), input.getKey());
        }
    };


    private Integer hashCode;

    public static <T, U> F2<T, U, Pair<T,U>> creator() {
        return blindCast(FACTORY);
    }

    public Pair(T first, U second) {
        super(checkNotNull(first, "first"), checkNotNull(second, "second"));
    }

    public static <T,U> Pair<T,U> of(T t, U u) {
        return new Pair<>(t, u);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> F<Map.Entry<? extends T, ?>, T> getFirstFromPair() {
        return (F<Map.Entry<? extends T,?>,T>) GET_FIRST;
    }

    @SuppressWarnings({"unchecked"})
    public static <T> F<Map.Entry<?, ? extends T>, T> getSecondFromPair() {
        return (F<Map.Entry<?, ? extends T>, T>) GET_SECOND;
    }

    @SuppressWarnings({"unchecked"})
    public static <K, V> F<Map.Entry<K, V>, Map.Entry<V, K>> swapper() {
        return SWAPPER;
    }
    
    public static <T,U> Pair<FunIterable<T>, FunIterable<U>> unzip(Iterable<? extends Map.Entry<T, U>> pairs) {
//        return Pair.of(FunIterable.map(pairs, Pair.<T>getFirstFromPair()), FunIterable.map(pairs, Pair.<U>getSecondFromPair()));
        return Pair.of(FunIterable.map(pairs, Pair.<T>getFirstFromPair()), FunIterable.map(pairs, Pair.<U>getSecondFromPair()));
    }

    public static <T, U> Pair<U, U> map(Pair<? extends T, ? extends T> pair, Function<? super T, ? extends U> mapper) {
        return Pair.of(mapper.apply(pair.getKey()), mapper.apply(pair.getValue()));
    }

    public T getFirst() {
        return getKey();
    }

    public U getSecond() {
        return getValue();
    }

    public Pair<U, T> swap() {
        return Pair.of(getValue(), getKey());
    }

    public <V> Pair<V, U> mapKey(Function<? super T, ? extends V> mapper) {
        return Pair.of(mapper.apply(getKey()), getValue());
    }

    public <V> Pair<T, V> mapValue(Function<? super U, ? extends V> mapper) {
        return Pair.of(getKey(), mapper.apply(getValue()));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair other = (Pair) obj;
            // if computed, use hashcodes to obtain a cheap answer
            if (hashCode != null && other.hashCode != null && !hashCode.equals(other.hashCode)) return false;
        } else if (!(obj instanceof Map.Entry)) {
            return false;
        }

        Map.Entry<?, ?> that = (Map.Entry<?, ?>) obj;
        return getKey().equals(that.getKey())
                && getValue().equals(that.getValue());
    }

    @Override
    public String toString() {
        return "[" + getKey() + "," + getValue() + "]";
    }

    @Override
    public int hashCode() {
        //cached because the graph can be deep
        if (hashCode == null) hashCode = super.hashCode();
        return hashCode;
    }
}
