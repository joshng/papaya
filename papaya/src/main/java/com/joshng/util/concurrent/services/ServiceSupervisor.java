package com.joshng.util.concurrent.services;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Value.Immutable
public abstract class ServiceSupervisor {
    private static final FutureTask<?> TERMINATOR = new FutureTask<>(ServiceSupervisor::terminateSystem);
    private static final Executor TERMINATOR_THREAD = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setNameFormat(threadName("Exit"))
            .build());

    public static ImmutableServiceSupervisor.Builder builder() {
        return ImmutableServiceSupervisor.builder();
    }

    public static ImmutableServiceSupervisor.Builder shutdownGracePeriod(Duration gracePeriod) {
        return builder().shutdownGracePeriod(gracePeriod);
    }

    public abstract Duration shutdownGracePeriod();

    @Value.Default
    public Logger logger() {
        return LoggerFactory.getLogger(ServiceSupervisor.class);
    }

    @Value.Default
    public boolean exitOnUncaughtException() {
        return false;
    }

    @Value.Default
    public boolean blockUntilShutdown() {
        return true;
    }
    public void startService(Callable<? extends Service> service) {
        startServiceManager(() -> new ServiceManager(ImmutableList.of(service.call())));
    }

    public void startServices(Callable<? extends Iterable<? extends Service>> services) {
        startServiceManager(() -> new ServiceManager(services.call()));
    }

    public void startServiceManager(Callable<ServiceManager> managerBuilder) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            logger().error("UNCAUGHT EXCEPTION in thread {}", t, e);
            if (exitOnUncaughtException()) terminateSystemAsync();
        });
        ServiceManager manager;
        try {
            manager = managerBuilder.call();
            manager.addListener(new ServiceManager.Listener() {
                                    public void stopped() {
                                    }

                                    public void healthy() {
                                        logger().info("Service started.");
                                    }

                                    public void failure(Service service) {
                                        logger().error("Service failed, terminating: {}", service, service.failureCause());
                                        terminateSystemAsync();
                                    }
                                },
                    MoreExecutors.directExecutor());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger().info("ShutdownHook invoked, stopping services...");
                try {
                    manager.stopAsync().awaitStopped(shutdownGracePeriod().toMillis(), TimeUnit.MILLISECONDS);
                    logger().info("Shutdown complete! Bye now :)");
                } catch (TimeoutException timeout) {
                    logger().warn("Graceful shutdown timed out after {}, aborting!", shutdownGracePeriod());
                }
            }, threadName("ShutdownHook")));

            manager.startAsync();
            do {
                try {
                    manager.awaitHealthy(10, TimeUnit.SECONDS);
                    break;
                } catch (TimeoutException e) {
                    logger().warn("... still starting up: {}", manager);
                }
            } while (true);
        } catch (Throwable e) {
            logger().error("Failed to start, terminating", e);
            throw terminateSystem();
        }

        if (blockUntilShutdown()) {
            try {
                manager.awaitStopped();
                logger().info("Service stopped.");
            } catch (Throwable e) {
                logger().error("Failed to stop cleanly, terminating", e);
                throw terminateSystem();
            }
        }
    }

    private static String threadName(String threadPurpose) {
        return ServiceSupervisor.class.getSimpleName() + "-" + threadPurpose;
    }

    private static void terminateSystemAsync() {
        TERMINATOR_THREAD.execute(TERMINATOR);
    }

    private static AssertionError terminateSystem() {
        System.exit(1);
        return new AssertionError("Unpossible");
    }

    public static abstract class Builder {
        public abstract ServiceSupervisor build();

        public void startService(Callable<? extends Service> serviceInitializer) {
            build().startService(serviceInitializer);
        }

        public void startServices(Callable<? extends Iterable<? extends Service>> servicesInitializer) {
            build().startServices(servicesInitializer);
        }

        public void startServiceManager(Callable<ServiceManager> managerBuilder) {
            build().startServiceManager(managerBuilder);
        }
    }
}
