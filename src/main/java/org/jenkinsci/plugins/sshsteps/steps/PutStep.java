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
 * Step to place a file/directory onto a remote node.
 *
 * @author Naresh Rayapati
 */
public class PutStep extends BasicSSHStep {

  private static final long serialVersionUID = 9183111587222550149L;

  @Getter
  private final String from;

  @Getter
  private final String into;

  @Getter
  @Setter
  @DataBoundSetter
  private String filterBy = "name";

  @Getter
  @Setter
  @DataBoundSetter
  private String filterRegex;

  @DataBoundConstructor
  public PutStep(String from, String into) {
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
      return "sshPut";
    }

    @Override
    public String getDisplayName() {
      return getPrefix() + getFunctionName() + " - Put a file/directory on remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    private static final long serialVersionUID = -4497192469254138827L;

    protected Execution(PutStep step, StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      PutStep step = (PutStep) getStep();
      FilePath ws = getContext().get(FilePath.class);
      assert ws != null;
      FilePath fromPath;

      if (Util.fixEmpty(step.getFrom()) == null) {
        throw new IllegalArgumentException("from is null or empty");
      }

      fromPath = ws.child(step.getFrom());

      if (!fromPath.exists()) {
        throw new IllegalArgumentException(fromPath.getRemote() + " does not exist.");
      }

      if (Util.fixEmpty(step.getInto()) == null) {
        throw new IllegalArgumentException("into is null or empty");
      }

      return getChannel().call(new PutCallable(step, getListener(), fromPath.getRemote()));
    }

    private static class PutCallable extends SSHMasterToSlaveCallable {

      private final String from;

      public PutCallable(PutStep step, TaskListener listener, String from) {
        super(step, listener);
        this.from = from;
      }

      @Override
      public Object execute() {
        final PutStep step = (PutStep) getStep();
        return getService().put(from, step.getInto(), step.getFilterBy(), step.getFilterRegex());
      }
    }
  }
}
