package joshng.util.concurrent.services;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import joshng.util.blocks.Pred;
import joshng.util.blocks.Source;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.FunFuture;
import joshng.util.exceptions.FatalErrorHandler;
import joshng.util.proxy.ProxyUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:16 PM
 */
public abstract class BaseService implements Service {
  static final AsyncF<Service, State> START = input -> FunFuture.extendFuture(input.start());
  private static final Source<State> FAILED_SHUTDOWN_RECOVERY = Source.ofInstance(State.FAILED);
  static final AsyncF<Service, State> STOP = input -> FunFuture.extendFuture(input.stop())
                                                               .recover(Pred.alwaysTrue(), FAILED_SHUTDOWN_RECOVERY);

  protected abstract Service delegate();

  public boolean isRunning() {
    return delegate().isRunning();
  }

  public void addListener(Service.Listener listener, Executor executor) {
    delegate().addListener(listener, executor);
  }

  public Throwable failureCause() {
    return delegate().failureCause();
  }

  public ListenableFuture<Service.State> start() {
    return delegate().start();
  }

  public ListenableFuture<Service.State> stop() {
    return delegate().stop();
  }

  public Service.State startAndWait() {
    return delegate().startAndWait();
  }

  public Service.State stopAndWait() {
    return delegate().stopAndWait();
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
