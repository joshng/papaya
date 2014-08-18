package joshng.util.concurrent.services;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import joshng.util.blocks.F;
import joshng.util.blocks.SideEffect;
import joshng.util.collect.Maybe;
import joshng.util.concurrent.AsyncF;
import joshng.util.concurrent.FunFuture;
import joshng.util.concurrent.IntegratedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static joshng.util.collect.Functional.extend;

/**
 * User: josh
 * Date: 6/24/13
 * Time: 10:54 AM
 */
public class ServiceWithDependencies extends NotifyingService {
  private final Logger logger;

  private static final String REQUIRED_SERVICE_PREFIX = "\n  ->";

  private final Service underlyingService;
  private final Set<ServiceWithDependencies> dependentServices = Sets.newHashSet();
  private final Set<ServiceWithDependencies> requiredServices = Sets.newHashSet();
  private final StoppingListener stoppingListener = new StoppingListener();
  private final Set<Service> excludedServiceDependencies = Sets.newHashSet();

  ServiceWithDependencies(Service underlyingService, F<Service, ServiceWithDependencies> wrapperFunction) {
    underlyingService.addListener(stoppingListener, MoreExecutors.sameThreadExecutor());
    checkState(!underlyingService.isRunning(), "Underlying service was already running!", underlyingService);
    this.underlyingService = underlyingService;
    logger = LoggerFactory.getLogger(underlyingService.getClass());
    for (IntegratedService dependentService : Maybe.asInstance(underlyingService, IntegratedService.class)) {
      addRequiredServices(wrapperFunction.transform(dependentService.getRequiredServices()));
      excludedServiceDependencies.addAll(dependentService.getExcludedServiceDependencies());
    }
  }

  void addDependentService(ServiceWithDependencies dependentService) {
    dependentServices.add(dependentService);
  }

  void addRequiredServices(Iterable<? extends ServiceWithDependencies> services) {
    for (ServiceWithDependencies requiredService : services) {
      if (!excludedServiceDependencies.contains(requiredService.getUnderlyingService()) && requiredServices.add(
              requiredService)) {
        requiredService.addDependentService(this);
        requiredService.addListener(stoppingListener, MoreExecutors.sameThreadExecutor());
      }
    }
  }

  public Service getUnderlyingService() {
    return underlyingService;
  }

  public Set<ServiceWithDependencies> getDependentServices() {
    return Collections.unmodifiableSet(dependentServices);
  }

  public Set<ServiceWithDependencies> getRequiredServices() {
    return Collections.unmodifiableSet(requiredServices);
  }

  @Override
  protected void doStart() {
    logger.debug("Wrapper starting... {}", underlyingService);
    cascadeStateChange(START, requiredServices).uponCompletion(new FutureCallback<State>() {
      @Override public void onSuccess(State result) {
        logger.info("STARTED: {}", underlyingService);
        notifyStarted();
      }

      @Override public void onFailure(Throwable t) {
        logger.error("FAILED: " + ServiceWithDependencies.this, t);
        notifyFailed(t);
      }
    });
  }

  @Override
  protected void doStop() {
    logger.debug("Wrapper stopping... {}", underlyingService);
    cascadeStateChange(STOP, dependentServices).map(new SideEffect() {
      public void run() {
        logger.info("STOPPED: {}", underlyingService);
        notifyStopped();
      }
    });
  }

  private FunFuture<State> cascadeStateChange(
          AsyncF<Service, State> stateChange,
          Set<? extends Service> prerequisites
  ) {
    return FunFuture.allAsList(stateChange.transform(prerequisites)).flatMap(stateChange.bindAsync(underlyingService));
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Service{").append(underlyingService).append("}");
    if (!requiredServices.isEmpty()) {
      builder.append(" with dependencies:").append(REQUIRED_SERVICE_PREFIX);
      Joiner.on(REQUIRED_SERVICE_PREFIX)
            .appendTo(builder, extend(requiredServices).map(input -> input.underlyingService));
    }
    return builder.toString();
  }

  private class StoppingListener extends Listener {
    @Override public void starting() {
      start();
    }

    @Override public void stopping(State from) {
      switch (state()) {
        case STARTING:
        case RUNNING:
          stop();
          break;
      }
    }

    @Override public void failed(State from, Throwable failure) {
      logger.error("FAILED, service will stop", failure);
      notifyFailed(failure);
    }
  }
}
