package org.jenkinsci.plugins.sshsteps.steps;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.sshsteps.util.SSHMasterToSlaveCallable;
import org.jenkinsci.plugins.sshsteps.util.SSHStepDescriptorImpl;
import org.jenkinsci.plugins.sshsteps.util.SSHStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Step to get a file from remote node to local workspace.
 *
 * @author Naresh Rayapati
 */
public class GetStep extends BasicSSHStep {

  private static final long serialVersionUID = -8831609599645560972L;

  @Getter
  private final String from;

  @Getter
  private final String into;

  @Getter
  @Setter
  @DataBoundSetter
  private boolean override = false;

  @DataBoundConstructor
  public GetStep(final String from, final String into) {
    this.from = from;
    this.into = into;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  @Extension
  public static class DescriptorImpl extends SSHStepDescriptorImpl {

    @Override
    public String getFunctionName() {
      return "sshGet";
    }

    @Override
    public String getDisplayName() {
      return getPrefix() + "sshGet - Get a file/directory from remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    private static final long serialVersionUID = 8544114488028417422L;

    protected Execution(final GetStep step, final StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      GetStep step = (GetStep) getStep();
      FilePath ws = getContext().get(FilePath.class);
      assert ws != null;
      FilePath intoPath;

      if (Util.fixEmpty(step.getFrom()) == null) {
        throw new IllegalArgumentException("from is null or empty");
      }

      if (Util.fixEmpty(step.getInto()) == null) {
        throw new IllegalArgumentException("into is null or empty");
      }

      intoPath = ws.child(step.getInto());

      if (intoPath.exists() && !step.isOverride()) {
        throw new IllegalArgumentException(
            intoPath.getRemote() + " already exist. Please set override to true just in case.");
      }

      return getLauncher().getChannel()
          .call(new GetCallable(step, getListener(), intoPath.getRemote()));
    }

    private static class GetCallable extends SSHMasterToSlaveCallable {

      private final String into;

      public GetCallable(final GetStep step, final TaskListener listener, final String into) {
        super(step, listener);
        this.into = into;
      }

      @Override
      public Object execute() {
        return getService().get(((GetStep) getStep()).getFrom(), into);
      }
    }
  }
}
