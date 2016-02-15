package com.joshng.util.collect;

import java.util.Iterator;

public interface AbstractFunIterable<T> extends FunIterable<T> {
  @Override
  default Iterable<T> delegate() {
    return this;
  }

  Iterator<T> iterator();
}
