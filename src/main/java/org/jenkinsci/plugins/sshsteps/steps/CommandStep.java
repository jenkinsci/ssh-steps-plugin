package org.jenkinsci.plugins.sshsteps.steps;

import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.IOException;
import lombok.Getter;
import org.jenkinsci.plugins.sshsteps.util.SSHMasterToSlaveCallable;
import org.jenkinsci.plugins.sshsteps.util.SSHStepDescriptorImpl;
import org.jenkinsci.plugins.sshsteps.util.SSHStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Step to execute a command on remote node.
 *
 * @author Naresh Rayapati
 */
public class CommandStep extends BasicSSHStep {

  private static final long serialVersionUID = 7492916747486604582L;

  @Getter
  private final String command;

  @Getter
  @DataBoundSetter
  private boolean sudo = false;

  @DataBoundConstructor
  public CommandStep(String command) {
    this.command = command;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  @Extension
  public static class DescriptorImpl extends SSHStepDescriptorImpl {

    @Override
    public String getFunctionName() {
      return "sshCommand";
    }

    @Override
    public String getDisplayName() {
      return getPrefix() + getFunctionName() + " - Execute command on remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    private static final long serialVersionUID = -5293952534324828128L;

    protected Execution(CommandStep step, StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      CommandStep step = (CommandStep) getStep();
      if (Util.fixEmpty(step.getCommand()) == null) {
        throw new IllegalArgumentException("command is null or empty");
      }

      return getChannel().call(new CommandCallable(step, getListener()));
    }

    private static class CommandCallable extends SSHMasterToSlaveCallable {

      public CommandCallable(CommandStep step, TaskListener listener) {
        super(step, listener);
      }

      @Override
      public Object execute() {
        CommandStep step = (CommandStep) getStep();
        return getService().executeCommand(step.getCommand(), step.isSudo());
      }
    }
  }
}
