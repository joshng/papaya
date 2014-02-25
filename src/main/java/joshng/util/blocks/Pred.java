package joshng.util.blocks;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import joshng.util.Comparison;
import joshng.util.collect.FunIterable;
import joshng.util.collect.Functional;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: josh
 * Date: 10/2/11
 * Time: 1:16 PM
 */
public abstract class Pred<T> implements Predicate<T> {
    public static final Pred<Boolean> IDENTITY = extendFunction(F.<Boolean>identity());

    public static <T> Pred<T> extendFunction(final Function<T, Boolean> fn) {
        return new Pred<T>() { public boolean apply(T input) {
            return fn.apply(input);
        } };
    }

    public static <T> Pred<T> extendPredicate(final Predicate<T> fn) {
        if (fn instanceof Pred) return (Pred<T>) fn;
        return new ExtendedPredicate<T>(fn);
    }

    public static Pred<Object> equalTo(final Object value) {
        return value == null ? Pred.isNull() : new Pred<Object>() {
            public boolean apply(Object input) {
                return value.equals(input);
            }
        };
    }

    public static <T> Pred<T> notEqualTo(final T value) {
        return value == null ? Pred.<T>notNull() : new Pred<T>() {
            public boolean apply(T input) {
                return !value.equals(input);
            }
        };
    }

    public static <T> Pred<T> in(Collection<? extends T> collection) {
        return new ExtendedPredicate<T>(Predicates.in(collection));
    }

    public static Pred<Object> instanceOf(final Class<?> instanceClass) {
        return new Pred<Object>() {
            public boolean apply(Object input) {
                return instanceClass.isInstance(input);
            }
        };
    }

    public static Pred<Class<?>> superclassOf(final Class<?> subclass) {
        return new Pred<Class<?>>() {
            public boolean apply(Class<?> input) {
                return input.isAssignableFrom(subclass);
            }
        };
    }

    public static Pred<Class<?>> subclassOf(final Class<?> superclass) {
        return new Pred<Class<?>>() {
            public boolean apply(Class<?> input) {
                return superclass.isAssignableFrom(input);
            }
        };
    }

    public static Pred<AnnotatedElement> annotatedWith(final Class<? extends Annotation> anno) {
        return new Pred<AnnotatedElement>() {
            public boolean apply(AnnotatedElement input) {
                return input.isAnnotationPresent(anno);
            }
        };
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

    public <U extends T> ImmutableListMultimap<Boolean, U> discriminate(Iterable<U> items) {
        return Multimaps.index(items, asFunction());
    }

    public Pred<Iterable<? extends T>> any() {
        return new Pred<Iterable<? extends T>>() {
            public boolean apply(Iterable<? extends T> input) {
                return Iterables.any(input, Pred.this);
            }
        };
    }

    public Pred<Iterable<? extends T>> all() {
        return new Pred<Iterable<? extends T>>() {
            public boolean apply(Iterable<? extends T> input) {
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

    public <U extends T> FunIterable<U> filter(Iterable<U> items) {
        return Functional.filter(items, this);
    }

    @Override
    public abstract boolean apply(T input);

    public Pred<T> and(final Function<? super T, Boolean> pred2) {
        return new Pred<T>() { public boolean apply(T input) {
            return Pred.this.apply(input) && pred2.apply(input);
        } };
    }

    public Pred<T> and(final Predicate<? super T> pred2) {
        return new Pred<T>() { public boolean apply(T input) {
            return Pred.this.apply(input) && pred2.apply(input);
        } };
    }

    public Pred<T> or(final Function<? super T, Boolean> pred2) {
        return new Pred<T>() { public boolean apply(T input) {
            return Pred.this.apply(input) || pred2.apply(input);
        } };
    }

    public Pred<T> or(final Predicate<? super T> pred2) {
        return new Pred<T>() { public boolean apply(T input) {
            return Pred.this.apply(input) || pred2.apply(input);
        } };
    }

    public <I0> Pred<I0> compose(final Function<? super I0, ? extends T> first) {
        return new Pred<I0>() {
            public boolean apply(I0 input) {
                return Pred.this.apply(first.apply(input));
            }
        };
    }

    public Pred<T> negated() {
        return not(this);
    }

    public static <T> Pred<T> not(final Pred<T> opposite) {
        return new Pred<T>() { public boolean apply(T input) {
            return !opposite.apply(input);
        } };
    }

    public F<T, Boolean> asFunction() {
        return new F<T, Boolean>() {
            public Boolean apply(T from) {
                return Pred.this.apply(from);
            }
        };
    }

    private static final Pred NOT_NULL = new Pred() {
        public boolean apply(Object input) {
            return input != null;
        }

        @Override
        public String toString() {
            return "IS NOT NULL";
        }
    };

    private static final Pred IS_NULL = new Pred() {
        public boolean apply(Object input) {
            return input == null;
        }

        @Override
        public String toString() {
            return "IS NULL";
        }
    };

    @SuppressWarnings({"unchecked"})
    private static final Pred ALWAYS_TRUE = new Pred() {
        public boolean apply(Object input) {
            return true;
        }

        @Override
        public Pred and(Function pred2) {
            return Pred.extendFunction(pred2);
        }

        @Override
        public Pred and(Predicate pred2) {
            return Pred.extendPredicate(pred2);
        }

        @Override
        public Pred or(Function pred2) {
            return this;
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
        public Pred negated() {
            return ALWAYS_FALSE;
        }

        @Override
        public String toString() {
            return "ALWAYS TRUE";
        }
    };

    @SuppressWarnings({"unchecked"})
    private static final Pred ALWAYS_FALSE = new Pred() {
        public boolean apply(Object input) {
            return false;
        }

        @Override
        public Pred and(Function pred2) {
            return this;
        }

        @Override
        public Pred and(Predicate pred2) {
            return this;
        }

        @Override
        public Pred or(Function pred2) {
            return Pred.extendFunction(pred2);
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
        public Pred negated() {
            return ALWAYS_TRUE;
        }

        @Override
        public String toString() {
            return "ALWAYS FALSE";
        }
    };


    public static class HashSetDeduplicatingPredicate extends Pred<Object> {
        private final Set<Object> seenValues = new HashSet<Object>();

        public boolean apply(Object input) {
            return seenValues.add(input);
        }
    }

    private static class ExtendedPredicate<T> extends Pred<T> {
        private final Predicate<T> fn;

        public ExtendedPredicate(Predicate<T> fn) {
            this.fn = fn;
        }

        public boolean apply(T input) {
            return fn.apply(input);
        }
    }
}
