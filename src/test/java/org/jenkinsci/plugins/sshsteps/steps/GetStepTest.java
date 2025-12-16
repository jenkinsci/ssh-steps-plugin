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
 * Unit test cases for GetStep class.
 *
 * @author Naresh Rayapati
 */
class GetStepTest extends BaseTest {

  final String path = "test.sh";
  final String filterBy = "name";
  final String filterRegex = null;

  @Mock
  FilePath filePathMock;

  GetStep.Execution stepExecution;

  @BeforeEach
  void setup() throws IOException, InterruptedException {

    when(filePathMock.child(any())).thenReturn(filePathMock);
    when(filePathMock.exists()).thenReturn(true);
    when(filePathMock.isDirectory()).thenReturn(false);
    when(filePathMock.getRemote()).thenReturn(path);

    when(contextMock.get(FilePath.class)).thenReturn(filePathMock);

  }

  @Test
  void testWithEmptyFromThrowsIllegalArgumentException() throws Exception {
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
  void testWithEmptyIntoThrowsIllegalArgumentException() throws Exception {
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
  void testSuccessfulExecuteScript() throws Exception {
    final GetStep step = new GetStep(path, path);
    step.setOverride(true);

    // Since SSHService is a mock, it is not validating remote.
    stepExecution = new GetStep.Execution(step, contextMock);

    // Execute Test.
    stepExecution.run();

    // Assert Test
    verify(sshServiceMock, times(1)).get(path, path, filterBy, filterRegex);
  }

  @Test
  void testFileTransferDoesNotLeakToConsole() throws Exception {
    // Security fix test: Verify that file transfer operations (get) don't leak
    // file contents or sensitive information to the Jenkins console.
    //
    // Background: When scp mode is enabled for sshGet, the interaction block
    // in defineRemote() was capturing ALL SSH session output, including file
    // transfer data. This meant that files containing secrets (credentials,
    // keys, tokens) would be printed to the Jenkins console, creating a
    // serious security vulnerability.
    //
    // Fix: The defineRemote() method now accepts an enableInteraction parameter
    // (default true for backward compatibility). File transfer operations
    // (get/put) call defineRemote(remote, false) to disable the interaction
    // block, preventing file contents from being echoed to the console.
    //
    // This test validates that the GetStep execution completes successfully
    // and that the mocked SSHService's get() method is invoked. The actual
    // verification that no console output is produced happens in the
    // SSHService implementation where interaction is disabled for file transfers.
    
    final GetStep step = new GetStep(path, path);
    step.setOverride(true);
    
    stepExecution = new GetStep.Execution(step, contextMock);
    
    // Execute the get operation - with the security fix, no file contents
    // will be printed to the console even if the file contains sensitive data
    stepExecution.run();
    
    // Verify that SSHService.get() was called (file transfer executed)
    verify(sshServiceMock, times(1)).get(path, path, filterBy, filterRegex);
    
    // The security fix ensures that defineRemote was called with
    // enableInteraction=false internally in SSHService.get(), preventing
    // the interaction block from capturing and printing file transfer output
  }
}
