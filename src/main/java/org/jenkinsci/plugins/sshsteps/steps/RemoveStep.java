package org.jenkinsci.plugins.sshsteps.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.Serial;

import lombok.Getter;
import org.jenkinsci.plugins.sshsteps.util.SSHMasterToSlaveCallable;
import org.jenkinsci.plugins.sshsteps.util.SSHStepDescriptorImpl;
import org.jenkinsci.plugins.sshsteps.util.SSHStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Step to remove a file/directory on remote node.
 *
 * @author Naresh Rayapati
 */
@Getter
public class RemoveStep extends BasicSSHStep {

  @Serial
  private static final long serialVersionUID = -177489327125117255L;

  private final String path;

  @DataBoundConstructor
  public RemoveStep(String path) {
    this.path = path;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  @Extension
  public static class DescriptorImpl extends SSHStepDescriptorImpl {

    @Override
    public String getFunctionName() {
      return "sshRemove";
    }

    @NonNull
    @Override
    public String getDisplayName() {
      return getPrefix() + getFunctionName() + " - Remove a file or directory from remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    @Serial
    private static final long serialVersionUID = 862708152481251266L;

    protected Execution(RemoveStep step, StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      RemoveStep step = (RemoveStep) getStep();
      if (Util.fixEmpty(step.getPath()) == null) {
        throw new IllegalArgumentException("path is null or empty");
      }

      return getChannel().call(new RemoveCallable(step, getListener()));
    }

    private static class RemoveCallable extends SSHMasterToSlaveCallable {

      public RemoveCallable(RemoveStep step, TaskListener listener) {
        super(step, listener);
      }

      @Override
      public Object execute() {
        return getService().remove(((RemoveStep) getStep()).getPath());
      }
    }
  }
}
