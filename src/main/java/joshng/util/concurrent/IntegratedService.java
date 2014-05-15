package joshng.util.concurrent;

import com.google.common.util.concurrent.Service;

import java.util.Collection;

/**
 * User: josh
 * Date: 6/14/13
 * Time: 11:19 AM
 */
public interface IntegratedService extends Service {
  Collection<? extends Service> getRequiredServices();

  Collection<? extends Service> getExcludedServiceDependencies();
}
