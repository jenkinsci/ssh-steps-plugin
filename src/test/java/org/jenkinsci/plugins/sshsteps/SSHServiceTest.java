package org.jenkinsci.plugins.sshsteps;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Test cases for SSHService.
 *
 * @author Naresh Rayapati.
 */
public class SSHServiceTest {

  @Test
  public void testWithEmptyRemoteThrowsAssertionError() {
    Map<String, String> remote = new HashMap<>();

    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(() -> SSHService.create(remote, false, false, null))
        .withMessage("SSH Steps: remote is null or empty. Expression: remote. Values: remote = [:]")
        .withStackTraceContaining("AssertionError")
        .withNoCause();
  }

  @Test
  public void testRemoteWithEmptyNameThrowsAssertionError() {
    Map<String, String> remote = new HashMap<>();
    remote.put("name", "");

    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(() -> SSHService.create(remote, false, false, null))
        .withMessage(
            "SSH Steps:  a remote (or a gateway) is missing the required field 'name'. Expression: remote.name")
        .withStackTraceContaining("AssertionError")
        .withNoCause();
  }

  @Test
  public void testRemoteWithEmptyUserThrowsAssertionError() {
    Map<String, String> remote = new HashMap<>();
    remote.put("name", "dummy");

    assertThatExceptionOfType(AssertionError.class)
        .isThrownBy(() -> SSHService.create(remote, false, false, null))
        .withMessage("SSH Steps: user must be given (dummy). Expression: remote.user")
        .withStackTraceContaining("AssertionError")
        .withNoCause();
  }

  @Test
  public void testRemoteWithOutKnownHostsAndAllowAnyHostsThrowsIllegalArgumentException() {
    Map<String, String> remote = new HashMap<>();
    remote.put("name", "dummy");
    remote.put("user", "dummy");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> SSHService.create(remote, false, false, null))
        .withMessage("SSH Steps: knownHosts must be provided when allowAnyHosts is false: dummy")
        .withStackTraceContaining("IllegalArgumentException")
        .withNoCause();
  }

  @Test
  public void testRemoteWithMinimumRequiredParams() {
    Map<String, Object> remote = new HashMap<>();
    remote.put("name", "dummy");
    remote.put("user", "dummy");
    remote.put("allowAnyHosts", true);

    SSHService.create(remote, false, false, null);
  }
}
