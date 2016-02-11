package com.joshng.util.concurrent;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.joshng.util.ThreadLocalRef;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by: josh 10/24/13 1:53 PM
 */
public class SameThreadTrampolineExecutor extends AbstractExecutorService implements ListeningExecutorService {
  private final ThreadLocalRef<Trampoline> trampoline = new ThreadLocalRef<Trampoline>() {
    @Override
    protected Trampoline initialValue() {
      return new Trampoline();
    }
  };

  @Override
  public void execute(Runnable command) {
    submit(command);
  }

  public FunFuture<?> submit(Runnable command) {
    return trampoline.get().runWithTrampoline(FunFuture.funFutureTask(command));
  }

  @Override public <T> FunFuture<T> submit(Runnable task, T result) {
    return submit(Executors.callable(task, result));
  }

  public <T> FunFuture<T> submit(Callable<T> command) {
    return trampoline.get().runWithTrampoline(FunFuture.funFutureTask(command));
  }

  public <T> FunFuture<T> submitAsync(Callable<? extends ListenableFuture<T>> command) {
    return FunFuture.<T>dereference(submit(command));
  }

  private class Trampoline {
    private final Queue<Runnable> queue = Queues.newArrayDeque();
    private boolean running = false;

    <T> FunFuture<T> runWithTrampoline(FunRunnableFuture<T> task) {
      queue.offer(task);
      if (!running) {
        running = true;
        try {
          drain();
        } finally {
          trampoline.remove();
        }
      }
      return task;
    }

    private void drain() {
      Runnable job;
      while ((job = queue.poll()) != null) {
        job.run();
      }
    }
  }

  @Override public void shutdown() {
    throw new UnsupportedOperationException("SameThreadTrampolineExecutor.shutdown has not been implemented");
  }

  @Override public List<Runnable> shutdownNow() {
    throw new UnsupportedOperationException("SameThreadTrampolineExecutor.shutdownNow has not been implemented");
  }

  @Override public boolean isShutdown() {
    throw new UnsupportedOperationException("SameThreadTrampolineExecutor.isShutdown has not been implemented");
  }

  @Override public boolean isTerminated() {
    throw new UnsupportedOperationException("SameThreadTrampolineExecutor.isTerminated has not been implemented");
  }

  @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException("SameThreadTrampolineExecutor.awaitTermination has not been implemented");
  }
}
