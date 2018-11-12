package org.jenkinsci.plugins.sshsteps.util;

import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.slf4j.MDC;

/**
 * Custom log handler for hidetake's library.
 *
 * @author Naresh Rayapati
 */
public class CustomLogHandler extends Handler {

  private final PrintStream logger;
  private String uuid;

  /**
   * Constructor.
   *
   * @param logger PrintStream to print messages to.
   */
  public CustomLogHandler(PrintStream logger, String uuid) {
    this.logger = logger;
    this.uuid = uuid;
  }

  @Override
  public void publish(LogRecord record) {
    // First time running publish method on this object - assign current execution id.
    if (this.uuid == null) {
      this.uuid = MDC.get("execution.id");
    }
    if (this.uuid.equals(MDC.get("execution.id"))) {
      logger.println(record.getMessage());
    }
  }

  @Override
  public void flush() {
    this.logger.flush();
  }

  @Override
  public void close() throws SecurityException {
    // logger (PrintStream) is off of pipeline step, and being used after, so not closing it here.
  }
}
