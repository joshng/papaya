package com.joshng.util.concurrent.services;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.joshng.util.collect.FunList;
import com.joshng.util.blocks.SideEffect;
import com.joshng.util.concurrent.AsyncF;
import com.joshng.util.concurrent.FunFuture;
import com.joshng.util.exceptions.MultiException;

import static com.google.common.base.Preconditions.checkState;
import static com.joshng.util.collect.Functional.funList;

/**
 * User: josh
 * Date: 6/17/13
 * Time: 2:39 PM
 */
public abstract class AggregateService extends NotifyingService {

  private volatile FunList<? extends BaseService<?>> componentServices;

  protected abstract Iterable<? extends BaseService<?>> getComponentServices();

  @Override
  protected final void doStart() {
    componentServices = funList(getComponentServices());
    for (Service service : componentServices) {
      service.addListener(new ComponentServiceListener(service), MoreExecutors.directExecutor());
      checkState(!service.isRunning(), "Service was already running", service);
    }

    perform(START);
  }


  @Override
  protected final void doStop() {
    perform(STOP);
  }

  private void perform(AsyncF<BaseService<?>, State> action) {
    FunFuture.successfulAsList(componentServices.map(action)).map((SideEffect) this::checkHealthy);
  }

  private void checkHealthy() {
    boolean healthy = true;
    MultiException e = MultiException.Empty;
    for (Service service : componentServices) {
      if (service.state() == State.FAILED) {
        e = e.with(service.failureCause());
        healthy = false;
      }
    }

    if (healthy) {
      switch (state()) {
        case STARTING:
          notifyStarted();
          break;
        case STOPPING:
          notifyStopped();
          break;
      }
    } else {
      notifyFailed(e.getCombinedThrowable().getOrThrow());
    }
  }

  @Override
  public String toString() {
    if (componentServices != null && !componentServices.isEmpty()) {
      return super.toString() + "\n--" + componentServices.join("\n--");
    }
    return super.toString();
  }

  private static class ComponentServiceFailureException extends RuntimeException {
    private ComponentServiceFailureException(Service failedService, Throwable cause) {
      super("Component-service Failed: " + failedService, cause);
    }
  }

  private class ComponentServiceListener extends Listener {
    private final Service componentService;

    private ComponentServiceListener(Service componentService) {
      this.componentService = componentService;
    }

    @Override
    public void stopping(State from) {
      triggerStop();
    }

    @Override
    public void terminated(State from) {
      triggerStop();
    }

    @Override
    public void failed(State from, Throwable failure) {
      notifyFailed(new ComponentServiceFailureException(componentService, failure));
    }

    private void triggerStop() {
      switch (state()) {
        case NEW:
        case STARTING:
        case RUNNING:
          stop();
      }
    }
  }
}
