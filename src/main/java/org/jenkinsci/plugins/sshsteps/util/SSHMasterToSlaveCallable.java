package org.jenkinsci.plugins.sshsteps.util;

import com.google.common.annotations.VisibleForTesting;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.UUID;
import jenkins.security.MasterToSlaveCallable;
import org.apache.log4j.MDC;
import org.jenkinsci.plugins.sshsteps.SSHService;
import org.jenkinsci.plugins.sshsteps.steps.BasicSSHStep;

/**
 * Base Callable for all SSH Steps.
 *
 * @author Naresh Rayapati.
 */
public abstract class SSHMasterToSlaveCallable extends MasterToSlaveCallable<Object, IOException> {

  private final BasicSSHStep step;
  private final TaskListener listener;
  private SSHService service;

  public SSHMasterToSlaveCallable(BasicSSHStep step, TaskListener listener) {
    this.step = step;
    this.listener = listener;
  }

  @Override
  public Object call() {
    MDC.put("execution.id", UUID.randomUUID().toString());
    this.service = createService();
    return execute();
  }

  @VisibleForTesting
  public SSHService createService() {
    return SSHService
        .create(step.getRemote(), step.isFailOnError(), step.isDryRun(), listener.getLogger());
  }

  protected abstract Object execute();

  public BasicSSHStep getStep() {
    return step;
  }

  public SSHService getService() {
    return service;
  }
}
