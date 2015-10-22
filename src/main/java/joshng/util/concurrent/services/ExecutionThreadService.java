package joshng.util.concurrent.services;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

/**
 * User: josh
 * Date: 7/16/13
 * Time: 10:31 PM
 */
public abstract class ExecutionThreadService extends BaseService<ExecutionThreadService.InnerExecutionThreadService> {

  protected ExecutionThreadService() {
    super(new InnerExecutionThreadService());
    delegate().wrapper = this;
  }

  /**
   * Start the service. This method is invoked on the execution thread.
   * <p>
   * <p>By default this method does nothing.
   */
  protected void startUp() throws Exception {}

  /**
   * Run the service. This method is invoked on the execution thread.
   * Implementations must respond to stop requests. You could poll for lifecycle
   * changes in a work loop:
   * <pre>
   *   public void run() {
   *     while ({@link #isRunning()}) {
   *       // perform a unit of work
   *     }
   *   }
   * </pre>
   * ...or you could respond to stop requests by implementing {@link
   * #triggerShutdown()}, which should cause {@link #run()} to return.
   */
  protected abstract void run() throws Exception;

  /**
   * Invoked to request the service to stop.
   * <p>
   * <p>By default this method does nothing.
   */
  protected void triggerShutdown() {}

  /**
   * Stop the service. This method is invoked on the execution thread.
   * <p>
   * <p>By default this method does nothing.
   */
  protected void shutDown() throws Exception {}

  static class InnerExecutionThreadService extends AbstractExecutionThreadService {
    private ExecutionThreadService wrapper;

    @Override
    protected void run() throws Exception {
      wrapper.run();
    }

    @Override
    protected void startUp() throws Exception {
      wrapper.startUp();
    }

    @Override
    protected void triggerShutdown() {
      wrapper.triggerShutdown();
    }

    @Override
    protected void shutDown() throws Exception {
      wrapper.shutDown();
    }

    @Override
    protected String serviceName() {
      return wrapper.serviceName();
    }
  }
}
