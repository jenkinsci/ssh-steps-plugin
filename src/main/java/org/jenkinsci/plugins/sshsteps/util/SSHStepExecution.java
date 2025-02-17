package org.jenkinsci.plugins.sshsteps.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.ClassLoaderSanityThreadFactory;
import hudson.util.DaemonThreadFactory;
import hudson.util.NamingThreadFactory;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.apache.log4j.MDC;
import org.jenkinsci.plugins.sshsteps.steps.BasicSSHStep;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.springframework.security.core.Authentication;

/**
 * Non-blocking step execution for ssh steps.
 *
 * @param <T> the type of the return value (may be {@link Void})
 * @author Naresh Rayapati
 * @see StepExecution
 */
public abstract class SSHStepExecution<T> extends StepExecution {

  @Getter
  private final transient TaskListener listener;
  @Getter
  private final transient Launcher launcher;
  private static ExecutorService executorService;
  @Getter
  private final BasicSSHStep step;

  private transient volatile Future<?> task;
  private transient String threadName;
  private transient Throwable stopCause;

  protected SSHStepExecution(BasicSSHStep step, @NonNull StepContext context)
      throws IOException, InterruptedException {
    super(context);
    listener = context.get(TaskListener.class);
    launcher = context.get(Launcher.class);
    this.step = step;
  }

  static synchronized ExecutorService getExecutorService() {
    if (executorService == null) {
      executorService = Executors.newCachedThreadPool(
          new NamingThreadFactory(new ClassLoaderSanityThreadFactory(new DaemonThreadFactory()),
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

  protected VirtualChannel getChannel() {
    final VirtualChannel channel = getLauncher().getChannel();
    if (channel == null) {
      throw new IllegalArgumentException(
          "Unable to get the channel, Perhaps you forgot to surround the code with a step that provides this, such as: node, dockerNode");
    }
    return channel;
  }

  @Override
  public final boolean start() {
    Authentication auth = Jenkins.getAuthentication2();
    task = getExecutorService().submit(() -> {
      threadName = Thread.currentThread().getName();
      try {
        MDC.put("execution.id", UUID.randomUUID().toString());
        T ret;
        try (ACLContext acl = ACL.as2(auth)) {
          ret = run();
        }
        getContext().onSuccess(ret);
      } catch (Throwable x) {
        if (stopCause == null) {
          getContext().onFailure(x);
        } else {
          stopCause.addSuppressed(x);
        }
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
  public void stop(@NonNull Throwable cause) throws Exception {
    if (task != null) {
      stopCause = cause;
      task.cancel(true);
    }
    super.stop(cause);
  }

  @Override
  public void onResume() {
    getContext().onFailure(
        new Exception("Resume after a restart not supported for non-blocking synchronous steps"));
  }

  @Override
  public @NonNull
  String getStatus() {
    if (threadName != null) {
      return "running in thread: " + threadName;
    } else {
      return "not yet scheduled";
    }
  }

}
