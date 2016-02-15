package com.joshng.util.blocks;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.joshng.util.collect.FunIterable;
import com.joshng.util.collect.Functional;
import com.joshng.util.collect.Maybe;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * User: josh
 * Date: 10/2/11
 * Time: 1:16 PM
 */
@FunctionalInterface
public interface Pred<T> extends Predicate<T>, com.google.common.base.Predicate<T> {
  public static final Pred<Boolean> IDENTITY = extendFunction(F.<Boolean>identityF());

  default boolean apply(T value) {
    return test(value);
  }

  public static <T> Pred<T> pred(final Pred<T> fn) {
    return fn;
  }

  public static <T> Pred<T> extendFunction(final Function<T, Boolean> fn) {
    return new Pred<T>() {
      public boolean test(T input) {
        return fn.apply(input);
      }
    };
  }

  public static <T> Pred<T> extendPredicate(final Predicate<T> fn) {
    if (fn instanceof Pred) return (Pred<T>) fn;
    return fn::test;
  }

  public static Pred<Object> equalTo(final Object value) {
    return value == null ? Pred.isNull() : new Pred<Object>() {
      public boolean test(Object input) {
        return value.equals(input);
      }
    };
  }

  public static <T> Pred<T> notEqualTo(final T value) {
    return value == null ? Pred.<T>notNull() : new Pred<T>() {
      public boolean test(T input) {
        return !value.equals(input);
      }
    };
  }

  public static Pred<Object> identicalTo(Object value) {
    return value == null ? Pred.isNull() : input -> value == input;
  }

  public static <T> Pred<T> in(Collection<? extends T> collection) {
    return Predicates.in(collection)::apply;
  }

  public static Pred<Object> instanceOf(final Class<?> instanceClass) {
    return instanceClass::isInstance;
  }

  public static Pred<Class<?>> superclassOf(final Class<?> subclass) {
    return input -> input.isAssignableFrom(subclass);
  }

  public static Pred<Class<?>> subclassOf(final Class<?> superclass) {
    return superclass::isAssignableFrom;
  }

  public static Pred<AnnotatedElement> annotatedWith(final Class<? extends Annotation> anno) {
    return input -> input.isAnnotationPresent(anno);
  }

  public static <C extends Comparable<? super C>> Pred<C> greaterThan(final C value) {
    return Comparison.Greater.than(value);
  }

  public static <C extends Comparable<? super C>> Pred<C> lessThan(final C value) {
    return Comparison.Less.than(value);
  }

  public static <C extends Comparable<? super C>> Pred<C> greaterThanOrEqual(final C value) {
    return Comparison.GreaterOrEqual.to(value);
  }

  public static <C extends Comparable<? super C>> Pred<C> lessThanOrEqual(final C value) {
    return Comparison.LessOrEqual.to(value);
  }

  default <U extends T> ImmutableListMultimap<Boolean, U> discriminate(Iterable<U> items) {
    return Multimaps.index(items, asFunction());
  }

  default Pred<Iterable<? extends T>> any() {
    return new Pred<Iterable<? extends T>>() {
      public boolean test(Iterable<? extends T> input) {
        return Iterables.any(input, Pred.this);
      }
    };
  }

  default Pred<Iterable<? extends T>> all() {
    return new Pred<Iterable<? extends T>>() {
      public boolean test(Iterable<? extends T> input) {
        return Iterables.all(input, Pred.this);
      }
    };
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Pred<T> isNull() {
    return IS_NULL;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Pred<T> notNull() {
    return NOT_NULL;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Pred<T> alwaysTrue() {
    return ALWAYS_TRUE;
  }

  @SuppressWarnings({"unchecked"})
  public static <T> Pred<T> alwaysFalse() {
    return ALWAYS_FALSE;
  }

  public static Pred<Object> newDeduplicator() {
    return new HashSetDeduplicatingPredicate();
  }

  default <U extends T> FunIterable<U> filter(Iterable<U> items) {
    return FunIterable.filter(items, this);
  }

  default <U extends T> Maybe<U> find(Iterable<U> items) {
    return FunIterable.find(items, this);
  }

  default Pred<T> and(final Predicate<? super T> pred2) {
    return new Pred<T>() {
      public boolean test(T input) {
        return Pred.this.test(input) && pred2.test(input);
      }
    };
  }

  default Pred<T> or(final Predicate<? super T> pred2) {
    return input -> Pred.this.test(input) || pred2.test(input);
  }

  default <I0> Pred<I0> compose(final Function<? super I0, ? extends T> first) {
    return input -> test(first.apply(input));
  }

  default Pred<T> negate() {
    return not(this);
  }

  public static <T> Pred<T> not(final Pred<T> opposite) {
    return new Pred<T>() {
      public boolean test(T input) {
        return !opposite.test(input);
      }
    };
  }

  default F<T, Boolean> asFunction() {
    return Pred.this::test;
  }

  static final Pred NOT_NULL = new Pred() {
    public boolean test(Object input) {
      return input != null;
    }

    @Override
    public String toString() {
      return "IS NOT NULL";
    }
  };

  static final Pred IS_NULL = new Pred() {
    public boolean test(Object input) {
      return input == null;
    }

    @Override
    public String toString() {
      return "IS NULL";
    }
  };

  @SuppressWarnings({"unchecked"})
  static final Pred ALWAYS_TRUE = new Pred() {
    public boolean test(Object input) {
      return true;
    }

    @Override
    public Pred and(Predicate pred2) {
      return Pred.extendPredicate(pred2);
    }

    @Override
    public Pred or(Predicate pred2) {
      return this;
    }

    @Override
    public FunIterable filter(Iterable items) {
      return Functional.extend(items);
    }

    @Override
    public Pred compose(Function first) {
      return this;
    }

    @Override
    public Pred negate() {
      return ALWAYS_FALSE;
    }

    @Override
    public String toString() {
      return "ALWAYS TRUE";
    }
  };

  @SuppressWarnings({"unchecked"})
  static final Pred ALWAYS_FALSE = new Pred() {
    public boolean test(Object input) {
      return false;
    }

    @Override
    public Pred and(Predicate pred2) {
      return this;
    }

    @Override
    public Pred or(Predicate pred2) {
      return Pred.extendPredicate(pred2);
    }

    @Override
    public FunIterable filter(Iterable items) {
      return Functional.empty();
    }

    @Override
    public Pred compose(Function first) {
      return this;
    }

    @Override
    public Pred negate() {
      return ALWAYS_TRUE;
    }

    @Override
    public String toString() {
      return "ALWAYS FALSE";
    }
  };


  public static class HashSetDeduplicatingPredicate implements Pred<Object> {
    private final Set<Object> seenValues = new HashSet<>();

    public boolean test(Object input) {
      return seenValues.add(input);
    }
  }
}
