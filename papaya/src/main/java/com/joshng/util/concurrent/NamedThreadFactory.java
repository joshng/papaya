package com.joshng.util.concurrent;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by: josh 2/25/14 12:48 AM
 */
public class NamedThreadFactory implements ThreadFactory {
  private static final LoadingCache<String, AtomicInteger> THREAD_NAME_COUNTERS = CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicInteger>() {
    @Override
    public AtomicInteger load(String key) throws Exception {
      return new AtomicInteger();
    }
  });
  private final String threadNamePrefix;
  private final AtomicInteger nameCounter;

  public NamedThreadFactory(String threadNamePrefix) {
    this.threadNamePrefix = threadNamePrefix;
    nameCounter = THREAD_NAME_COUNTERS.getUnchecked(threadNamePrefix);
  }


  @Override
  public Thread newThread(Runnable task) {
    return new Thread(task, threadNamePrefix + "-" + nameCounter.incrementAndGet());
  }
}
