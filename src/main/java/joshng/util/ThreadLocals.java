package joshng.util;

import com.google.common.base.Supplier;
import com.google.common.collect.ForwardingDeque;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import java.util.Deque;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: josh
 * Date: Jul 20, 2010
 * Time: 8:59:00 AM
 */
public class ThreadLocals {
  public static <T> NullableThreadLocalRef<T> newThreadLocalRef() {
    return new NullableThreadLocalRef<T>();
  }

  public static <T> ThreadLocalRef<T> newThreadLocalRef(T initialValue) {
    checkNotNull(initialValue, "Initial value cannot be null (use newThreadLocalRef() with no arguments to specify a null initial value)");
    return new ThreadLocalRef<T>(initialValue);
  }

  public static <T> ThreadLocalRef<T> newThreadLocalRef(final Supplier<? extends T> initializer) {
    checkNotNull(initializer, "Supplier cannot be null (use newThreadLocalRef() with no arguments to specify a null initial value)");
    return new ThreadLocalRef<T>() {
      @Override
      protected T initialValue() {
        return initializer.get();
      }
    };
  }

  public static <T> Set<T> newThreadLocalSet() {
    return new ForwardingSet<T>() {
      private final ThreadLocal<Set<T>> threadSet = new ThreadLocal<Set<T>>() {
        @Override
        protected Set<T> initialValue() {
          return Sets.newHashSet();
        }
      };

      @Override
      protected Set<T> delegate() {
        return threadSet.get();
      }
    };
  }

  public static <T> Deque<T> newThreadLocalDeque() {
    return new ForwardingDeque<T>() {
      private final ThreadLocal<Deque<T>> threadDeque = new ThreadLocal<Deque<T>>() {
        @Override
        protected Deque<T> initialValue() {
          return Queues.newArrayDeque();
        }
      };

      @Override
      protected Deque<T> delegate() {
        return threadDeque.get();
      }
    };
  }
}
