package org.jenkinsci.plugins.sshsteps.util;

import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;

/**
 * Default StepDescriptorImpl for all SSH steps.
 *
 * @author Naresh Rayapati
 */
public abstract class SSHStepDescriptorImpl extends StepDescriptor {

  protected String getPrefix() {
    return Common.getPrefix();
  }

  @Override
  public Set<? extends Class<?>> getRequiredContext() {
    return ImmutableSet.of(Run.class, TaskListener.class, EnvVars.class);
  }
}
