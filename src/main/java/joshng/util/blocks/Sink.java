package joshng.util.blocks;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import joshng.util.collect.Nothing;
import org.slf4j.Logger;

/**
 * User: josh
 * Date: Sep 23, 2011
 * Time: 9:14:03 AM
 */
 public abstract class Sink<T> extends F<T, Nothing> implements Consumer<T> {
    private static final Sink<Object> NOOP_SINK = new Sink<Object>() {
        public void handle(Object value) {
        }
    };
    public static final Sink<Runnable> RUNNABLE_RUNNER = new Sink<Runnable>() {
        public void handle(Runnable value) {
            value.run();
        }
    };

    public static <T> Sink<T> extendHandler(final Consumer<T> handler) {
         if (handler instanceof Sink) return (Sink<T>) handler;
         return new Sink<T>() {
             public void handle(T value) {
                 handler.handle(value);
             }
         };
     }

     @SuppressWarnings("unchecked")
     public static <T> Sink<T> extendFunction(final Function<T, ?> handler) {
         if (handler instanceof Sink) return (Sink<T>) handler;
         return new Sink<T>() {
             public void handle(T value) {
                 handler.apply(value);
             }
         };
     }

    public static <T> Sink<T> warningLogger(final Logger logger, final String format) {
        return new Sink<T>() {
            @Override
            public void handle(T value) {
                logger.warn(format, value);
            }
        };
    }

    public static <T> Sink<T> infoLogger(final Logger logger, final String format) {
        return new Sink<T>() {
            @Override
            public void handle(T value) {
                logger.info(format, value);
            }
        };
    }

    @Override
    public <I0> Sink<I0> compose(final Function<I0, ? extends T> first) {
        return new Sink<I0>() {
            public void handle(I0 value) {
                Sink.this.handle(first.apply(value));
            }
        };
    }

    public Nothing apply(T input) {
        handle(input);
        return Nothing.NOTHING;
    }

    @Override
    public SideEffect bind(final T input) {
        return new SideEffect() {
            @Override
            public void run() {
                handle(input);
            }
        };
    }

    public Sink<T> filter(final Predicate<? super T> inputFilter) {
        return new Sink<T>() {
            @Override
            public void handle(T value) {
                if (inputFilter.apply(value)) Sink.this.handle(value);
            }
        };
    }

    public static Sink<Object> nullSink() {
        return NOOP_SINK;
    }
}
