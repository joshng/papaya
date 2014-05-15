package joshng.util.proxy;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * User: josh
 * Date: May 29, 2011
 * Time: 2:13:01 PM
 * <p>
 * A universal implementation of InvocationHandler than can be applied to both jdk proxies
 * and CGLib proxies
 */
public abstract class UniversalInvocationHandler implements MethodInterceptor, InvocationHandler {
  public Object intercept(final Object obj, final Method method, final Object[] args, final MethodProxy proxy) throws Throwable {
    return invoke(obj, method, args);
  }

  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    return handle(proxy, method, args);
  }

  protected abstract Object handle(final Object proxy, final Method method, final Object[] args) throws Throwable;
}
