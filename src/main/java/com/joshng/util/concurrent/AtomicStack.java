package com.joshng.util.concurrent;

import com.google.common.collect.ForwardingList;
import org.pcollections.Empty;
import org.pcollections.PStack;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * User: josh
 * Date: 7/31/14
 * Time: 8:08 AM
 */
public class AtomicStack<T> extends ForwardingList<T> {
  private static final AtomicReferenceFieldUpdater<AtomicStack, PStack> storageUpdater
          = AtomicReferenceFieldUpdater.newUpdater(AtomicStack.class, PStack.class, "storage");
  private volatile PStack<T> storage = Empty.stack();

  public static <T> AtomicStack<T> newAtomicStack() {
    return new AtomicStack<>();
  }

  @Override protected List<T> delegate() {
    return storage;
  }

  @SuppressWarnings("unchecked") @Override
  public boolean add(T value) {
    storageUpdater.getAndUpdate(this, s -> s.plus(value));
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> items) {
    storageUpdater.getAndUpdate(this, s -> s.plusAll(items));
    return true;
  }
}
