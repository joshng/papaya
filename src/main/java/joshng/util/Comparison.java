package joshng.util;

import com.google.common.collect.Ordering;
import joshng.util.blocks.Pred;

import java.util.Comparator;

/**
 * User: josh
 * Date: 1/10/12
 * Time: 5:28 PM
 */
public enum Comparison {
    Greater(" than ") { protected final boolean matches(int comparison) { return comparison > 0; } },
    Less(" than ") { protected final boolean matches(int comparison) { return comparison < 0; } },
    GreaterOrEqual { protected final boolean matches(int comparison) { return comparison >= 0; } },
    LessOrEqual { protected final boolean matches(int comparison) { return comparison <= 0; } },
    Equal { protected final boolean matches(int comparison) { return comparison == 0; } };

    public static final Pred<Integer> IS_POSITIVE = Greater.than(0);
    public static final Pred<Integer> IS_NON_NEGATIVE = GreaterOrEqual.to(0);
    public static final Pred<Integer> IS_NEGATIVE = Less.than(0);

    Comparison(String preposition) {
        this.preposition = preposition;
    }

    Comparison() {
        this(" to ");
    }

    public <C extends Comparable<? super C>> boolean compare(C a, C b) {
        return matches(a.compareTo(b));
    }

    public <T> boolean compare(T a, T b, Comparator<? super T> comparator) {
        return matches(comparator.compare(a, b));
    }

    public <C extends Comparable<? super C>> ComparingPredicate<C> than(final C value) {
        return new ComparingPredicate<C>(Ordering.natural(), value);
    }

    public <T> ComparingPredicate<T> than(final T value, final Comparator<? super T> comparator) {
        return new ComparingPredicate<T>(comparator, value);
    }

    public <C extends Comparable<? super C>> ComparingPredicate<C> to(final C value) {
        return than(value);
    }

    public <T> ComparingPredicate<T> to(final T value, final Comparator<? super T> comparator) {
        return than(value, comparator);
    }

    protected abstract boolean matches(int comparison);

    private final String preposition;

    public class ComparingPredicate<T> extends Pred<T> {
        private final Comparator<? super T> comparator;
        private final T value;

        public ComparingPredicate(Comparator<? super T> comparator, T value) {
            this.comparator = comparator;
            this.value = value;
        }

        public boolean apply(T input) {
            return compare(input, value, comparator);
        }

        public Comparison getComparison() {
            return Comparison.this;
        }

        public T getThreshold() {
            return value;
        }

        @Override
        public String toString() {
            return Comparison.this + preposition + value;
        }
    }
}
