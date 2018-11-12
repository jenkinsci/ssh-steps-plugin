package org.jenkinsci.plugins.sshsteps.util;

import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.security.NotReallyRoleSensitiveCallable;
import org.acegisecurity.Authentication;
import org.apache.log4j.MDC;
import org.jenkinsci.plugins.sshsteps.steps.BasicSSHStep;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;

/**
 * Non blocking step execution for ssh steps.
 *
 * @param <T> the type of the return value (may be {@link Void})
 * @author Naresh Rayapati
 * @see StepExecution
 */
public abstract class SSHStepExecution<T> extends StepExecution {

  private static ExecutorService executorService;
  private final transient TaskListener listener;
  private final transient Launcher launcher;
  private transient volatile Future<?> task;
  private transient String threadName;
  private final BasicSSHStep step;

  protected SSHStepExecution(BasicSSHStep step, @Nonnull StepContext context) throws IOException, InterruptedException {
    super(context);
    listener = context.get(TaskListener.class);
    launcher = context.get(Launcher.class);
    this.step = step;
  }

  static synchronized ExecutorService getExecutorService() {
    if (executorService == null) {
      executorService = Executors
          .newCachedThreadPool(new NamingThreadFactory(new DaemonThreadFactory(),
              "org.jenkinsci.plugins.ssh.util.SSHStepExecution"));
    }
    return executorService;
  }

  /**
   * Meat of the execution.
   *
   * When this method returns, a step execution is over.
   */
  protected abstract T run() throws Exception;

  @Override
  public final boolean start() {
    Authentication auth = Jenkins.getAuthentication();
    task = getExecutorService().submit(() -> {
      try {
        getContext().onSuccess(ACL.impersonate(auth, new NotReallyRoleSensitiveCallable<T, Exception>() {
          @Override
          public T call() throws Exception {
            threadName = Thread.currentThread().getName();
            MDC.put("execution.id", UUID.randomUUID().toString());
            return SSHStepExecution.this.run();
          }
        }));
      } catch (Exception e) {
        getContext().onFailure(e);
      } finally {
        MDC.clear();
      }
    });
    return false;
  }

  /**
   * If the computation is going synchronously, try to cancel that.
   */
  @Override
  public void stop(Throwable cause) {
    if (task != null) {
      task.cancel(true);
    }
  }

  @Override
  public void onResume() {
    getContext().onFailure(
        new Exception("Resume after a restart not supported for non-blocking synchronous steps"));
  }

  @Override
  public @Nonnull
  String getStatus() {
    if (threadName != null) {
      return "running in thread: " + threadName;
    } else {
      return "not yet scheduled";
    }
  }

  public TaskListener getListener() {
    return listener;
  }

  public Launcher getLauncher() {
    return launcher;
  }

  public BasicSSHStep getStep() {
    return step;
  }
}
