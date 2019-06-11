package org.jenkinsci.plugins.sshsteps.steps;

import hudson.Extension;
import hudson.FilePath;
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

/**
 * Step to execute a script (a file) on remote node.
 *
 * @author Naresh Rayapati
 */
public class ScriptStep extends BasicSSHStep {

  private static final long serialVersionUID = 7358533459289529723L;

  @Getter
  private final String script;

  @DataBoundConstructor
  public ScriptStep(String script) {
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
      return "sshScript";
    }

    @Override
    public String getDisplayName() {
      return getPrefix() + getFunctionName() + " - Execute script(file) on remote node.";
    }
  }

  public static class Execution extends SSHStepExecution {

    private static final long serialVersionUID = 6008070200393301960L;

    protected Execution(ScriptStep step, StepContext context)
        throws IOException, InterruptedException {
      super(step, context);
    }

    @Override
    protected Object run() throws Exception {
      ScriptStep step = (ScriptStep) getStep();
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

      return getChannel().call(new ScriptCallable(step, getListener(), path.getRemote()));
    }

    private static class ScriptCallable extends SSHMasterToSlaveCallable {

      private final String script;

      public ScriptCallable(ScriptStep step, TaskListener listener, String script) {
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
