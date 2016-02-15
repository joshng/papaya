package com.joshng.util.concurrent.services;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.joshng.util.blocks.Pred;
import com.joshng.util.concurrent.Promise;
import com.joshng.util.exceptions.FatalErrorHandler;
import com.joshng.util.proxy.ProxyUtil;
import com.joshng.util.blocks.Source;
import com.joshng.util.concurrent.AsyncF;
import com.joshng.util.concurrent.FunFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:16 PM
 */
public abstract class BaseService<S extends Service> implements Service {
  static final AsyncF<BaseService<?>, State> START = input -> FunFuture.extendFuture(input.start());
  private static final Source<State> FAILED_SHUTDOWN_RECOVERY = Source.ofInstance(State.FAILED);
  static final AsyncF<BaseService<?>, State> STOP = input -> input.stop()
                                                               .recover(Pred.alwaysTrue(), FAILED_SHUTDOWN_RECOVERY);

  private final S delegate;
  private final AtomicBoolean started = new AtomicBoolean();
  private final Promise<State> startedPromise = Promise.newPromise();
  private final Promise<State> stoppedPromise = Promise.newPromise();

  protected BaseService(S delegate) {
    this.delegate = delegate;
    delegate.addListener(new Listener() {
                           @Override public void running() {
                             startedPromise.setSuccess(state());
                           }

                           @Override public void terminated(State from) {
                             startedPromise.setSuccess(from);
                             stoppedPromise.setSuccess(state());
                           }

                           @Override public void failed(State from, Throwable failure) {
                             startedPromise.setFailure(failure);
                             stoppedPromise.setFailure(failure);
                           }
                         }, MoreExecutors.directExecutor());
  }

  public FunFuture<State> getStartedFuture() {
    return startedPromise;
  }

  public FunFuture<State> getStoppedFuture() {
    return stoppedPromise;
  }

  protected final S delegate() {
    return delegate;
  }

  public boolean isRunning() {
    return delegate().isRunning();
  }

  public void addListener(Service.Listener listener, Executor executor) {
    delegate().addListener(listener, executor);
  }

  public Throwable failureCause() {
    return delegate().failureCause();
  }

  public FunFuture<Service.State> start() {
    if (started.compareAndSet(false, true)) delegate().startAsync();
    return startedPromise;
  }

  public FunFuture<Service.State> stop() {
    delegate.stopAsync();
    return stoppedPromise;
  }

  public Service.State startAndWait() {
    return start().getUnchecked();
  }

  public Service.State stopAndWait() {
    return stop().getUnchecked();
  }

  public Service.State state() {
    return delegate().state();
  }

  @Override public void awaitRunning() {delegate().awaitRunning();}

  @Override public void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
    delegate().awaitRunning(timeout,
            unit);
  }

  @Override public void awaitTerminated() {delegate().awaitTerminated();}

  @Override public void awaitTerminated(
          long timeout,
          TimeUnit unit
  ) throws TimeoutException {delegate().awaitTerminated(timeout, unit);}

  @Override public Service startAsync() {
    delegate().startAsync();
    return this;
  }

  @Override public Service stopAsync() {
    delegate().stopAsync();
    return this;
  }

  public String serviceName() {
    return ProxyUtil.getUnenhancedClass(this).getSimpleName();
  }

  public void terminateProcessOnFailure() {
    addListener(new Listener() {
      @Override public void failed(final State from, final Throwable failure) {
        FatalErrorHandler.terminateProcess(serviceName() + ": Error while " + from, failure);
      }
    }, Executors.newSingleThreadExecutor());
  }

  @Override
  public String toString() {
    return serviceStateString(state());
  }

  protected String serviceStateString(State state) {return serviceName() + "[" + stateChar(state) + "]";}

  public static String stateChar(State state) {
    switch (state) {
      case NEW: return " ";
      case STARTING: return "➚";
      case RUNNING: return "√";
      case STOPPING: return "➘";
      case TERMINATED: return "×";
      case FAILED: return "!";
      default: return "?";
    }
  }
}
