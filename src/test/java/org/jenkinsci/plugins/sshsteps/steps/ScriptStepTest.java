package org.jenkinsci.plugins.sshsteps.steps;

import hudson.FilePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cases for ScriptStep class.
 *
 * @author Naresh Rayapati
 */
class ScriptStepTest extends BaseTest {

  final String scriptName = "test.sh";

  @Mock
  FilePath filePathMock;

  ScriptStep.Execution stepExecution;

  @BeforeEach
  void setup() throws IOException, InterruptedException {

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(scriptName);

    when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

  }

  @Test
  void testWithEmptyCommandThrowsIllegalArgumentException() throws Exception {
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
  void testSuccessfulExecuteScript() throws Exception {
    final ScriptStep step = new ScriptStep(scriptName);

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new ScriptStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).executeScriptFromFile(scriptName);
  }
}
