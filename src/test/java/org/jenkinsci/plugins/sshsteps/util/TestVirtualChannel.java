package org.jenkinsci.plugins.sshsteps.util;

import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;

/**
 * VirtualChannel for testing.
 *
 * @author Naresh Rayapati
 */
public class TestVirtualChannel implements VirtualChannel {

  @Override
  public <V, T extends Throwable> V call(Callable<V, T> callable) throws T {
    return callable.call();
  }

  @Override
  public <V, T extends Throwable> Future<V> callAsync(Callable<V, T> callable) {
    return null;
  }

  @Override
  public void close() {

  }

  @Override
  public void join() {

  }

  @Override
  public void join(long timeout) {

  }

  @Override
  public <T> T export(Class<T> type, T instance) {
    return null;
  }

  @Override
  public void syncLocalIO() {

  }
}
