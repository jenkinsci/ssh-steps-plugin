package org.jenkinsci.plugins.ssh.util;

import hudson.remoting.Callable;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import java.io.IOException;

/**
 * VirtualChannel for testing.
 *
 * @author Naresh Rayapati
 */
public class TestVirtualChannel implements VirtualChannel {

  @Override
  public <V, T extends Throwable> V call(Callable<V, T> callable)
      throws IOException, T, InterruptedException {
    return callable.call();
  }

  @Override
  public <V, T extends Throwable> Future<V> callAsync(Callable<V, T> callable) throws IOException {
    return null;
  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public void join() throws InterruptedException {

  }

  @Override
  public void join(long timeout) throws InterruptedException {

  }

  @Override
  public <T> T export(Class<T> type, T instance) {
    return null;
  }

  @Override
  public void syncLocalIO() throws InterruptedException {

  }
}
