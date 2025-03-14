package org.jenkinsci.plugins.sshsteps.steps;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.sshsteps.SSHService;
import org.jenkinsci.plugins.sshsteps.util.TestVirtualChannel;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Base Test Class.
 *
 * @author Naresh Rayapati
 */
class BaseTest {

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

  private AutoCloseable closeable;
  private MockedStatic<SSHService> sshService;

  @BeforeEach
  void setUpBase() throws IOException, InterruptedException {

    closeable = MockitoAnnotations.openMocks(this);

    when(runMock.getCauses()).thenReturn(null);
    when(taskListenerMock.getLogger()).thenReturn(printStreamMock);
    doNothing().when(printStreamMock).println();
    when(launcherMock.getChannel()).thenReturn(new TestVirtualChannel());

    sshService = Mockito.mockStatic(SSHService.class);
    sshService.when(() -> SSHService.create(any(), anyBoolean(), anyBoolean(), any())).thenReturn(sshServiceMock);

    when(contextMock.get(Run.class)).thenReturn(runMock);
    when(contextMock.get(TaskListener.class)).thenReturn(taskListenerMock);
    when(contextMock.get(EnvVars.class)).thenReturn(envVarsMock);
    when(contextMock.get(Launcher.class)).thenReturn(launcherMock);
  }

  @AfterEach
  void tearUpBase() throws Exception {
    sshService.close();
    closeable.close();
  }
}
