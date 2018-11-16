package com.joshng.util.guice.events;

import com.joshng.util.collect.Nothing;
import com.joshng.util.concurrent.FunFuture;

/**
 * User: josh
 * Date: 11/16/18
 * Time: 9:43 AM
 */
public interface EventObserver<T> {
  FunFuture<Nothing> onEvent(T event);
}
