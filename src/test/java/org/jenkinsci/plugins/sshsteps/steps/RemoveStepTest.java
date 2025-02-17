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
 * Unit test cases for RemoveStep class.
 *
 * @author Naresh Rayapati
 */
class RemoveStepTest extends BaseTest {

  final String path = "test.sh";

  @Mock
  FilePath filePathMock;

  RemoveStep.Execution stepExecution;

  @BeforeEach
  void setup() throws IOException, InterruptedException {

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(path);

    when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

  }

  @Test
  void testWithEmptyPathThrowsIllegalArgumentException() throws Exception {
    final RemoveStep step = new RemoveStep("");
    stepExecution = new RemoveStep.Execution(step, contextMock);

    // Execute and assert Test.
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> stepExecution.run())
        .withMessage("path is null or empty")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  void testSuccessfulRemove() throws Exception {
    final RemoveStep step = new RemoveStep(path);

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new RemoveStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).remove(path);
  }
}
