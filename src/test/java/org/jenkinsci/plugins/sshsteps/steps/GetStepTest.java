package org.jenkinsci.plugins.sshsteps.steps;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.FilePath;
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
 * Unit test cases for GetStep class.
 *
 * @author Naresh Rayapati
 */
public class GetStepTest extends BaseTest {

  final String path = "test.sh";
  final String filterBy = "name";
  final String filterRegex = null;

  @Mock
  FilePath filePathMock;

  GetStep.Execution stepExecution;

  @Before
  public void setup() throws IOException, InterruptedException {

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(path);

    when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

  }

  @Test
  public void testWithEmptyFromThrowsIllegalArgumentException() throws Exception {
    final GetStep step = new GetStep("", path);
    stepExecution = new GetStep.Execution(step, contextMock);

    // Execute and assert Test.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stepExecution.run())
        .withMessage("from is null or empty")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  public void testWithEmptyIntoThrowsIllegalArgumentException() throws Exception {
    final GetStep step = new GetStep(path, "");
    step.setOverride(true);
    stepExecution = new GetStep.Execution(step, contextMock);

    // Execute and assert Test.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stepExecution.run())
        .withMessage("into is null or empty")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  public void testSuccessfulExecuteScript() throws Exception {
    final GetStep step = new GetStep(path, path);
    step.setOverride(true);

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new GetStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).get(path, path, filterBy, filterRegex);
  }
}
