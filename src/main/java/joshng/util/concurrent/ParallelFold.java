package joshng.util.concurrent;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import joshng.util.blocks.F;
import joshng.util.blocks.F2;
import joshng.util.collect.MutableReference;
import joshng.util.exceptions.MultiException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: josh
 * Date: 8/10/12
 * Time: 4:55 PM
 */
public class ParallelFold<I, O> extends AbstractIndependentCompletionTracker<I, O> {
  private final MutableReference<O> foldedResult;
  private final F<I, O> resultFolder;
  private final Cache<Class<? extends Throwable>, ErrorAccumulator> errorAccumulators = CacheBuilder.newBuilder().build();
  private final boolean abortOnFailure;

  public ParallelFold(O seedValue, boolean abortOnFailure, ListeningExecutorService foldExecutor, F2<? super I, ? super O, ? extends O> folder) {
    super(foldExecutor);
    this.abortOnFailure = abortOnFailure;
    this.foldedResult = AtomicMutableReference.newReference(seedValue);
    resultFolder = foldedResult.modifier(folder);
  }

  public static <I, O> ParallelFold<I, O> newParallelFold(O initialValue, boolean abortOnFailure, ListeningExecutorService foldExecutor, F2<? super I, ? super O, ? extends O> folder) {
    return new ParallelFold<I, O>(initialValue, abortOnFailure, foldExecutor, folder);
  }

  @Override
  protected void handleCompletedJob(ListenableFuture<? extends I> job) throws Exception {
    try {
      resultFolder.apply(job.get());
    } catch (final Exception e) {
      if (abortOnFailure) throw e;
      Throwable cause = FunFuture.unwrapExecutionException(e);
      try {
        errorAccumulators.get(cause.getClass(), () -> new ErrorAccumulator(cause)).occurrenceCount.incrementAndGet();
      } catch (ExecutionException unlikely) {
        throw new RuntimeException(Objects.firstNonNull(unlikely.getCause(), unlikely));
      }
    }
  }

  @Override
  protected O computeResult() throws Exception {
    MultiException e = MultiException.Empty;
    for (ErrorAccumulator accumulator : errorAccumulators.asMap().values()) {
      e = e.with(accumulator.getRepresentativeException());
    }
    e.throwIfException(Exception.class);
    return foldedResult.get();
  }

  public static class ErrorAccumulator {
    private final Throwable firstOccurrence;
    private final AtomicInteger occurrenceCount = new AtomicInteger();

    // TODO: should we attempt to record (and sort by) a timestamp to indicate WHEN things went awry?
    private ErrorAccumulator(Throwable firstOccurrence) {
      this.firstOccurrence = firstOccurrence;
    }

    Throwable getRepresentativeException() {
      int count = occurrenceCount.get();
      if (count == 1) return firstOccurrence;
      return new RepeatedException(firstOccurrence, count);
    }

    public static class RepeatedException extends RuntimeException {
      public RepeatedException(Throwable firstOccurrence, int count) {
        super("The following exception-type occurred " + count + " times (note that this count is grouped by EXCEPTION-TYPE; other errors may have had different causes/stacktraces): " + firstOccurrence.getMessage(), firstOccurrence);
      }
    }
  }
}
