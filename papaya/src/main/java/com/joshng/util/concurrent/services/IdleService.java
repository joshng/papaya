package com.joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.MoreExecutors;
import com.joshng.util.blocks.ThrowingRunnable;

import java.util.concurrent.Executor;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:17 PM
 */
public abstract class IdleService extends BaseService<IdleService.DelegateService> {


  protected IdleService() {
    super(new DelegateService());
    delegate().wrapper = this;
  }

  protected void notifyFailed(Throwable cause) {
      delegate().doNotifyFailed(cause);
  }
  /**
   * Start the service.
   */
  protected abstract void startUp() throws Exception;

  /**
   * Stop the service.
   */
  protected abstract void shutDown() throws Exception;

  protected boolean startUpOnSeparateThread() {
    return true;
  }

  protected boolean shutDownOnSeparateThread() {
    return true;
  }

  static class DelegateService extends AbstractService {
      private IdleService wrapper;

      @Override
      protected final void doStart() {
          stateTransition(() -> {
              wrapper.startUp();
              notifyStarted();
          });
      }

      @Override
      protected final void doStop() {
          stateTransition(() ->{
              wrapper.shutDown();
              notifyStopped();
          });
      }

      final void doNotifyFailed(Throwable cause) {
          notifyFailed(cause);
      }

      private void stateTransition(ThrowingRunnable transition) {
          State state = state();
          executor(state).execute(() -> {
              Thread currentThread = Thread.currentThread();
              String prevName = currentThread.getName();
              try {
                  currentThread.setName(wrapper.serviceStateString(state));
                  transition.run();
              } catch (Throwable t) {
                  notifyFailed(t);
              } finally {
                  currentThread.setName(prevName);
              }
          });
      }

      private Executor executor(State state) {
          if ((state == State.STARTING && wrapper.startUpOnSeparateThread()) || wrapper.shutDownOnSeparateThread()) {
              return task -> new Thread(task).start();
          } else {
              return MoreExecutors.directExecutor();
          }
      }

      @Override
      public String toString() {
        return wrapper.toString();
      }
  }
}
