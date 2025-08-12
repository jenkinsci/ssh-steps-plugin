package org.jenkinsci.plugins.sshsteps.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.Serial;

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
@Getter
public class GetStep extends BasicSSHStep {

  @Serial
  private static final long serialVersionUID = -8831609599645560972L;

  private final String from;

  private final String into;

  @Setter
  @DataBoundSetter
  private String filterBy = "name";

  @Setter
  @DataBoundSetter
  private String filterRegex;

  @Setter
  @DataBoundSetter
  private boolean override = false;

  @Setter
  @DataBoundSetter
  private boolean verbose = false;

  @DataBoundConstructor
  public GetStep(String from, String into) {
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

    @NonNull
    @Override
    public String getDisplayName() {
      return getPrefix() + getFunctionName() + " - Get a file or directory from remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    @Serial
    private static final long serialVersionUID = 8544114488028417422L;

    protected Execution(GetStep step, StepContext context)
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

      return getChannel().call(new GetCallable(step, getListener(), intoPath.getRemote()));
    }

    private static class GetCallable extends SSHMasterToSlaveCallable {

      private final String into;

      public GetCallable(GetStep step, TaskListener listener, String into) {
        super(step, listener);
        this.into = into;
      }

      @Override
      public Object execute() {
        final GetStep step = (GetStep) getStep();
        getService().setVerbose(step.isVerbose());
        return getService().get(step.getFrom(), into, step.getFilterBy(), step.getFilterRegex());
      }
    }
  }
}
