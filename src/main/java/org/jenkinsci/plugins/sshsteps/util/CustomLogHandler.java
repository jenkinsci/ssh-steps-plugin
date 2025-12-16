package org.jenkinsci.plugins.sshsteps.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import org.slf4j.MDC;

/**
 * Custom log handler for hidetake's library with buffering and rate limiting support.
 *
 * @author Naresh Rayapati
 */
public class CustomLogHandler extends Handler {

  private static final int DEFAULT_BUFFER_SIZE = 50;
  private static final long DEFAULT_FLUSH_INTERVAL_MS = 100;
  private static final long DEFAULT_RATE_LIMIT_LINES_PER_SEC = 1000;

  private final PrintStream logger;
  private String uuid;
  private final List<String> buffer;
  private final int bufferSize;
  private final long flushIntervalMs;
  private final long rateLimitLinesPerSec;
  private long lastFlushTime;
  private final AtomicLong lineCount = new AtomicLong(0);
  private long lastRateLimitReset;

  /**
   * Constructor with default buffering.
   *
   * @param logger PrintStream to print messages to.
   * @param uuid Execution UUID for filtering.
   */
  public CustomLogHandler(PrintStream logger, String uuid) {
    this(logger, uuid, DEFAULT_BUFFER_SIZE, DEFAULT_FLUSH_INTERVAL_MS, DEFAULT_RATE_LIMIT_LINES_PER_SEC);
  }

  /**
   * Constructor with configurable buffering and rate limiting.
   *
   * @param logger PrintStream to print messages to.
   * @param uuid Execution UUID for filtering.
   * @param bufferSize Number of lines to buffer before flushing.
   * @param flushIntervalMs Time in milliseconds between flushes.
   * @param rateLimitLinesPerSec Maximum lines per second (0 to disable).
   */
  public CustomLogHandler(PrintStream logger, String uuid, int bufferSize, long flushIntervalMs, long rateLimitLinesPerSec) {
    this.logger = logger;
    this.uuid = uuid;
    this.bufferSize = bufferSize;
    this.flushIntervalMs = flushIntervalMs;
    this.rateLimitLinesPerSec = rateLimitLinesPerSec;
    this.buffer = new ArrayList<>(bufferSize);
    this.lastFlushTime = System.currentTimeMillis();
    this.lastRateLimitReset = System.currentTimeMillis();
  }

  @Override
  public synchronized void publish(LogRecord record) {
    // First time running publish method on this object - assign current execution id.
    if (this.uuid == null) {
      this.uuid = MDC.get("execution.id");
    }
    
    // Null-safe UUID check
    String currentUuid = MDC.get("execution.id");
    if (!Objects.equals(this.uuid, currentUuid)) {
      return;
    }

    // Rate limiting check
    if (rateLimitLinesPerSec > 0) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastRateLimitReset >= 1000) {
        // Reset counter every second
        lineCount.set(0);
        lastRateLimitReset = currentTime;
      }
      
      long currentCount = lineCount.incrementAndGet();
      if (currentCount > rateLimitLinesPerSec) {
        // Drop this line (rate limited)
        if (currentCount == rateLimitLinesPerSec + 1) {
          buffer.add("[Rate limit exceeded: some output suppressed]");
        }
        return;
      }
    }

    buffer.add(record.getMessage());

    // Flush if buffer is full or enough time has passed
    long currentTime = System.currentTimeMillis();
    if (buffer.size() >= bufferSize || (currentTime - lastFlushTime) >= flushIntervalMs) {
      flushBuffer();
      lastFlushTime = currentTime;
    }
  }

  private void flushBuffer() {
    if (!buffer.isEmpty()) {
      for (String message : buffer) {
        logger.println(message);
      }
      logger.flush();
      buffer.clear();
    }
  }

  @Override
  public synchronized void flush() {
    flushBuffer();
  }

  @Override
  public synchronized void close() throws SecurityException {
    // Flush any remaining buffered messages
    flushBuffer();
    // logger (PrintStream) is off of pipeline step, and being used after, so not closing it here.
  }
}
