package org.jenkinsci.plugins.sshsteps.steps;

import java.io.Serializable;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Base class for all SSH steps
 *
 * @author Naresh Rayapati
 */
public abstract class BasicSSHStep extends Step implements Serializable {

  @Getter
  @Setter
  @DataBoundSetter
  private Map remote;

  @Getter
  @DataBoundSetter
  private boolean failOnError = true;

  @Getter
  @DataBoundSetter
  private boolean dryRun = false;
}
