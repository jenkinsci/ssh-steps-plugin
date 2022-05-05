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
 * Unit test cases for ScriptStep class.
 *
 * @author Naresh Rayapati
 */
public class ScriptStepTest extends BaseTest {

  final String scriptName = "test.sh";

  @Mock
  FilePath filePathMock;

  ScriptStep.Execution stepExecution;

  @Before
  public void setup() throws IOException, InterruptedException {

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(scriptName);

    when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

  }

  @Test
  public void testWithEmptyCommandThrowsIllegalArgumentException() throws Exception {
    final ScriptStep step = new ScriptStep("");
    stepExecution = new ScriptStep.Execution(step, contextMock);

    // Execute and assert Test.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stepExecution.run())
        .withMessage("script is null or empty")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  public void testSuccessfulExecuteScript() throws Exception {
    final ScriptStep step = new ScriptStep(scriptName);

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new ScriptStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).executeScriptFromFile(scriptName);
  }
}
