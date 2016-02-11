package com.joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractService;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 8:39 PM
 */
public abstract class NotifyingService extends BaseService<NotifyingService.InnerAbstractService> {
  protected NotifyingService() {
    super(new InnerAbstractService());
    delegate().wrapper = this;
  }

  /**
   * This method is called by {@link #start} to initiate service startup. The invocation of this
   * method should cause a call to {@link #notifyStarted()}, either during this method's run, or
   * after it has returned. If startup fails, the invocation should cause a call to
   * {@link #notifyFailed(Throwable)} instead.
   * <p>
   * <p>This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service startup, even when {@link #start} is called
   * multiple times.
   */
  protected abstract void doStart();

  /**
   * This method should be used to initiate service shutdown. The invocation of this method should
   * cause a call to {@link #notifyStopped()}, either during this method's run, or after it has
   * returned. If shutdown fails, the invocation should cause a call to
   * {@link #notifyFailed(Throwable)} instead.
   * <p>
   * <p> This method should return promptly; prefer to do work on a different thread where it is
   * convenient. It is invoked exactly once on service shutdown, even when {@link #stop} is called
   * multiple times.
   */
  protected abstract void doStop();

  /**
   * Implementing classes should invoke this method once their service has started. It will cause
   * the service to transition from {@link com.google.common.util.concurrent.Service.State#STARTING} to {@link com.google.common.util.concurrent.Service.State#RUNNING}.
   *
   * @throws IllegalStateException if the service is not {@link com.google.common.util.concurrent.Service.State#STARTING}.
   */
  protected final void notifyStarted() {
    delegate().doNotifyStarted();
  }

  /**
   * Implementing classes should invoke this method once their service has stopped. It will cause
   * the service to transition from {@link com.google.common.util.concurrent.Service.State#STOPPING} to {@link com.google.common.util.concurrent.Service.State#TERMINATED}.
   *
   * @throws IllegalStateException if the service is neither {@link com.google.common.util.concurrent.Service.State#STOPPING} nor
   *                               {@link com.google.common.util.concurrent.Service.State#RUNNING}.
   */
  protected final void notifyStopped() {
    delegate().doNotifyStopped();
  }

  /**
   * Invoke this method to transition the service to the {@link com.google.common.util.concurrent.Service.State#FAILED}. The service will
   * <b>not be stopped</b> if it is running. Invoke this method when a service has failed critically
   * or otherwise cannot be started nor stopped.
   */
  protected final void notifyFailed(Throwable cause) {
    delegate().doNotifyFailed(cause);
  }

  static class InnerAbstractService extends AbstractService {
    private NotifyingService wrapper;

    @Override
    protected void doStart() {
      wrapper.doStart();
    }

    @Override
    protected void doStop() {
      wrapper.doStop();
    }

    private void doNotifyStarted() {
      notifyStarted();
    }

    private void doNotifyStopped() {
      notifyStopped();
    }

    private void doNotifyFailed(Throwable cause) {
      notifyFailed(cause);
    }

    @Override public String toString() {
      return wrapper.serviceName() + " [" + state() + "]";
    }
  }
}

