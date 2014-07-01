package joshng.util.blocks;

import com.google.common.base.Supplier;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:00:40 AM
 */
public interface F2<I1, I2, O> extends BiFunction<I1, I2, O> {
  public static <T> F2<Object, Object, T> constant(final T result) {
    return (input1, input2) -> result;
  }

  public static <I1, I2, O> F2<I1, I2, O> function2(final F2<I1, I2, O> fn) {
    return fn;
  }

  public static <I1, I2, O> F2<I1, I2, O> of(final BiFunction<I1, I2, O> fn) {
    if (fn instanceof F2) return (F2<I1, I2, O>) fn;
    return fn::apply;
  }

//    public static <T> F2<T,T,T> minimizer(Comparator<? super T> comparator) {
//        final Ordering<? super T> order = Ordering.from(comparator);
//        return (F2<T, T, T>) order::min;
//    }
//
//    public static <T> F2<T,T,T> maximizer(Comparator<? super T> comparator) {
//        final Ordering<? super T> order = Ordering.from(comparator);
//        return new F2<T, T, T>() {
//            public T apply(T input1, T input2) {
//                return order.max(input1, input2);
//            }
//        };
//    }

  default F<I2, O> bindFirst(final I1 input1) {
    return new F<I2, O>() {
      public O apply(final I2 input2) {
        return F2.this.apply(input1, input2);
      }

      public String toString() {
        return F2.this.toString() + "(" + input1 + ",?)";
      }
    };
  }

  default F<I1, O> bindSecond(final I2 input2) {
    return new F<I1, O>() {
      public O apply(I1 input) {
        return F2.this.apply(input, input2);
      }

      public String toString() {
        return F2.this.toString() + "(?, " + input2 + ")";
      }
    };
  }

  default F<I2, O> bindFirstFrom(final Supplier<? extends I1> input1Supplier) {
    return new F<I2, O>() {
      public O apply(final I2 input2) {
        return F2.this.apply(input1Supplier.get(), input2);
      }
    };
  }

  default F<I1, O> bindSecondFrom(final Supplier<? extends I2> input2Supplier) {
    return new F<I1, O>() {
      public O apply(I1 input) {
        return F2.this.apply(input, input2Supplier.get());
      }
    };
  }

  default Source<O> bind(final I1 input1, final I2 input2) {
    return new Source<O>() {
      public O get() {
        return F2.this.apply(input1, input2);
      }
    };
  }

  default F<I1, F<I2, O>> curried() {
    return new F<I1, F<I2, O>>() {
      public F<I2, O> apply(I1 input) {
        return bindFirst(input);
      }
    };
  }

  default F2<I2, I1, O> flip() {
    return new F2<I2, I1, O>() {
      public O apply(I2 input2, I1 input1) {
        return F2.this.apply(input1, input2);
      }
    };
  }

  default <I0> F2<I0, I2, O> composeFirst(final F<I0, I1> transformer) {
    return (input1, input2) -> F2.this.apply(transformer.apply(input1), input2);
  }

  default <I0> F2<I1, I0, O> composeSecond(final F<I0, I2> transformer) {
    return (input1, input2) -> F2.this.apply(input1, transformer.apply(input2));
  }

  default F<Map.Entry<? extends I1, ? extends I2>, O> tupled() {
    return from -> F2.this.apply(from.getKey(), from.getValue());
  }

  default <O2> F2<I1, I2, O2> andThen(final F<? super O, ? extends O2> next) {
    return (input1, input2) -> next.apply(apply(input1, input2));
  }

  O apply(I1 input1, I2 input2);
}
