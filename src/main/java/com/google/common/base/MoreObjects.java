package com.google.common.base;

/**
 * User: josh * Date: 8/10/15 * Time: 1:06 PM
 */

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * for forward-compatibility with dropwizard 0.8, which depends on a more recent version of guava than we can tolerate
 */
public class MoreObjects {
  public static <T> T firstNonNull(@Nullable T first, @Nullable T second) {
    return first != null ? first : checkNotNull(second);
  }
}
