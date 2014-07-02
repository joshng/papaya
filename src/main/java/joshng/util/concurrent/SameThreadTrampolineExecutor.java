package joshng.util.concurrent;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFuture;
import joshng.util.ThreadLocalRef;
import joshng.util.collect.Nothing;
import joshng.util.exceptions.MultiException;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Created by: josh 10/24/13 1:53 PM
 */
public class SameThreadTrampolineExecutor implements Executor {
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

  public FunFuture<Nothing> submit(Runnable command) {
    return trampoline.get().runWithTrampoline(FunFuture.funFutureTask(command));
  }

  public <T> FunFuture<T> submit(Callable<T> command) {
    return trampoline.get().runWithTrampoline(FunFuture.funFutureTask(command));
  }

  public <T> FunFuture<T> submitAsync(Callable<? extends ListenableFuture<T>> command) {
    return submit(command).flatMap(AsyncF.<T>asyncIdentity());
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
      MultiException multiException = MultiException.Empty;
      Runnable job;
      while ((job = queue.poll()) != null) {
        try {
          job.run();
        } catch (Exception e) {
          multiException = multiException.with(e);
        }
        multiException.throwRuntimeIfAny();
      }
    }
  }
}
