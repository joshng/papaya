package joshng.util.blocks;

import com.google.common.base.Supplier;
import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.Map;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:00:40 AM
 */
public abstract class F2<I1, I2, O> implements Function2<I1, I2, O> {
    public static <T> F2<Object, Object, T> constant(final T result) {
        return new F2<Object, Object, T>() {
            @Override
            public T apply(Object input1, Object input2) {
                return result;
            }
        };
    }
    public static <I1, I2, O> F2<I1, I2, O> of(final Function2<I1, I2, O> fn) {
        if (fn instanceof F2) return (F2<I1, I2, O>) fn;
        return new F2<I1, I2, O>() { public O apply(I1 input1, I2 input2) {
            return fn.apply(input1, input2);
        } };
    }

    public static <T> F2<T,T,T> minimizer(Comparator<? super T> comparator) {
        final Ordering<? super T> order = Ordering.from(comparator);
        return new F2<T, T, T>() {
            public T apply(T input1, T input2) {
                return order.min(input1, input2);
            }
        };
    }

    public static <T> F2<T,T,T> maximizer(Comparator<? super T> comparator) {
        final Ordering<? super T> order = Ordering.from(comparator);
        return new F2<T, T, T>() {
            public T apply(T input1, T input2) {
                return order.max(input1, input2);
            }
        };
    }

    public F<I2, O> bindFirst(final I1 input1) {
        return new F<I2, O>() {
            public O apply(final I2 input2) {
                return F2.this.apply(input1, input2);
            }
            public String toString() { return F2.this.toString() + "(" + input1 + ",?)";}
        };
    }

    public F<I1, O> bindSecond(final I2 input2) {
        return new F<I1, O>() {
            public O apply(I1 input) {
                return F2.this.apply(input, input2);
            }
            public String toString() { return F2.this.toString() + "(?, " + input2 + ")";}
        };
    }

    public F<I2, O> bindFirstFrom(final Supplier<? extends I1> input1Supplier) {
        return new F<I2, O>() {
            public O apply(final I2 input2) {
                return F2.this.apply(input1Supplier.get(), input2);
            }
        };
    }

    public F<I1, O> bindSecondFrom(final Supplier<? extends I2> input2Supplier) {
        return new F<I1, O>() {
            public O apply(I1 input) {
                return F2.this.apply(input, input2Supplier.get());
            }
        };
    }

    public Source<O> bind(final I1 input1, final I2 input2) {
        return new Source<O>() { public O get() {
            return F2.this.apply(input1, input2);
        } };
    }

    public F<I1, F<I2, O>> curried() {
        return new F<I1, F<I2, O>>() {
            public F<I2, O> apply(I1 input) {
                return bindFirst(input);
            }
        };
    }

    public F2<I2, I1, O> flip() {
        return new F2<I2, I1, O>() { public O apply(I2 input2, I1 input1) {
            return F2.this.apply(input1, input2);
        } };
    }

    public <I0> F2<I0, I2, O> composeFirst(final F<I0, I1> transformer) {
        return new F2<I0, I2, O>() { public O apply(I0 input1, I2 input2) {
            return F2.this.apply(transformer.apply(input1), input2);
        } };
    }

    public <I0> F2<I1, I0, O> composeSecond(final F<I0, I2> transformer) {
        return new F2<I1, I0, O>() { public O apply(I1 input1, I0 input2) {
            return F2.this.apply(input1, transformer.apply(input2));
        } };
    }

    public <I_1 extends I1, I_2 extends I2> F<Map.Entry<I_1, I_2>, O> tupled() {
        return new F<Map.Entry<I_1, I_2>, O>() { public O apply(Map.Entry<I_1, I_2> from) {
            return F2.this.apply(from.getKey(), from.getValue());
        } };
    }

    public <O2> F2<I1, I2, O2> andThen(final F<? super O, ? extends O2> next) {
        return new F2<I1, I2, O2>() {
            public O2 apply(I1 input1, I2 input2) {
                return next.apply(F2.this.apply(input1, input2));
            }
        };
    }
}
