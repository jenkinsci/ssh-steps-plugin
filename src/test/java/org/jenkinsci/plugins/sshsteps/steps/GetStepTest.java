package org.jenkinsci.plugins.sshsteps.steps;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test cases for GetStep class.
 *
 * @author Naresh Rayapati
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GetStepTest.class, SSHService.class, FilePath.class})
public class GetStepTest {

  final String path = "test.sh";
  final String filterBy = "name";
  final String filterRegex = null;

  @Mock
  TaskListener taskListenerMock;
  @Mock
  Run<?, ?> runMock;
  @Mock
  EnvVars envVarsMock;
  @Mock
  PrintStream printStreamMock;
  @Mock
  SSHService sshServiceMock;
  @Mock
  StepContext contextMock;
  @Mock
  Launcher launcherMock;
  @Mock
  FilePath filePathMock;

  GetStep.Execution stepExecution;

  @Before
  public void setup() throws IOException, InterruptedException {

    when(runMock.getCauses()).thenReturn(null);
    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();
    when(launcherMock.getChannel()).thenReturn(new TestVirtualChannel());

    PowerMockito.mockStatic(SSHService.class);
    when(SSHService.create(any(), anyBoolean(), anyBoolean(), any())).thenReturn(sshServiceMock);

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(path);

    when(contextMock.get(Run.class)).thenReturn(runMock);
    when(contextMock.get(TaskListener.class)).thenReturn(taskListenerMock);
    when(contextMock.get(EnvVars.class)).thenReturn(envVarsMock);
    when(contextMock.get(Launcher.class)).thenReturn(launcherMock);
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
