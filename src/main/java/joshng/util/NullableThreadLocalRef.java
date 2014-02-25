package joshng.util;

import joshng.util.collect.Maybe;

/**
* User: josh
* Date: May 22, 2011
* Time: 10:54:16 AM
*/
public class NullableThreadLocalRef<T> extends ThreadLocalRef<T> {
    @Override
    protected final T initialValue() {
        return null;
    }

    public boolean isSet() {
        return get() != null;
    }

    public Maybe<T> getMaybe() {
        return Maybe.of(get());
    }
}
