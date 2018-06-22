package org.jenkinsci.plugins.ssh.steps;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import java.io.IOException;
import lombok.Getter;
import org.jenkinsci.plugins.ssh.util.SSHMasterToSlaveCallable;
import org.jenkinsci.plugins.ssh.util.SSHStepDescriptorImpl;
import org.jenkinsci.plugins.ssh.util.SSHStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Step to execute a script (a file) on remote node.
 *
 * @author Naresh Rayapati
 */
public class ExecuteScriptStep extends BasicSSHStep {

  private static final long serialVersionUID = 7358533459289529723L;

  @Getter
  private final String script;

  @DataBoundConstructor
  public ExecuteScriptStep(final String script) {
    this.script = script;
  }

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new Execution(this, context);
  }

  @Extension
  public static class DescriptorImpl extends SSHStepDescriptorImpl {

    @Override
    public String getFunctionName() {
      return "sshExecuteScript";
    }

    @Override
    public String getDisplayName() {
      return getPrefix() + "Execute script(file) on remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    private static final long serialVersionUID = 6008070200393301960L;

    protected Execution(final ExecuteScriptStep step, final StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      ExecuteScriptStep step = (ExecuteScriptStep) getStep();
      FilePath ws = getContext().get(FilePath.class);
      assert ws != null;
      FilePath path;
      if (Util.fixEmpty(step.getScript()) == null) {
        throw new IllegalArgumentException("script is null or empty");
      }

      path = ws.child(step.getScript());

      if (!path.exists()) {
        throw new IllegalArgumentException(path.getRemote() + " does not exist.");
      }

      if (path.isDirectory()) {
        throw new IllegalArgumentException(path.getRemote() + " is a directory.");
      }

      return getLauncher().getChannel()
          .call(new ExecuteScriptCallable(step, getListener(), path.getRemote()));
    }

    private static class ExecuteScriptCallable extends SSHMasterToSlaveCallable {

      private final String script;

      public ExecuteScriptCallable(final ExecuteScriptStep step, final TaskListener listener,
          final String script) {
        super(step, listener);
        this.script = script;
      }

      @Override
      public Object execute() {
        return getService().executeScriptFromFile(script);
      }
    }
  }
}
