package org.jenkinsci.plugins.sshsteps.steps;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit test cases for CommandStep class.
 *
 * @author Naresh Rayapati
 */
class CommandStepTest extends BaseTest {

  CommandStep.Execution stepExecution;

  @Test
  void testWithEmptyCommandThrowsIllegalArgumentException() throws Exception {
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
  void testSuccessfulExecuteCommand() throws Exception {
    final CommandStep step = new CommandStep("ls -lrt");

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new CommandStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).executeCommand("ls -lrt", false);
  }
}
