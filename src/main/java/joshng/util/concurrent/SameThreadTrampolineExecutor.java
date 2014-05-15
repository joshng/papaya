package joshng.util.concurrent;

import com.google.common.collect.Queues;
import joshng.util.ThreadLocalRef;
import joshng.util.exceptions.MultiException;

import java.util.Queue;
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
    trampoline.get().runWithTrampoline(command, trampoline);
  }

  private static class Trampoline {
    private final Queue<Runnable> queue = Queues.newArrayDeque();
    private boolean running = false;

    void runWithTrampoline(final Runnable command, ThreadLocalRef<Trampoline> trampoline) {
      queue.offer(command);
      if (!running) {
        running = true;
        try {
          drain();
        } finally {
          trampoline.remove();
        }
      }
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
