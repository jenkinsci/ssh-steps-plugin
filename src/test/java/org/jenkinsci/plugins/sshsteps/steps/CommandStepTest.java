package org.jenkinsci.plugins.sshsteps.steps;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;
import org.jenkinsci.plugins.sshsteps.SSHService;
import org.jenkinsci.plugins.sshsteps.util.TestVirtualChannel;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * Unit test cases for CommandStep class.
 *
 * @author Naresh Rayapati
 */
public class CommandStepTest extends BaseTest {

  CommandStep.Execution stepExecution;

  @Test
  public void testWithEmptyCommandThrowsIllegalArgumentException() throws Exception {
    final CommandStep step = new CommandStep("");
    stepExecution = new CommandStep.Execution(step, contextMock);

    // Execute and assert Test.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stepExecution.run())
        .withMessage("command is null or empty")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  public void testSuccessfulExecuteCommand() throws Exception {
    final CommandStep step = new CommandStep("ls -lrt");

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new CommandStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).executeCommand("ls -lrt", false);
  }
}
