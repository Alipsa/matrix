package se.alipsa.matrix.logging;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.System.Logger.Level;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that matrix-logging's runtime dependencies actually route JDK System.Logger
 * and Log4j API calls through to the bundled slf4j-simple backend, so a dependency
 * version bump that silently breaks the routing (rather than just changing a number)
 * fails the build instead of only degrading Groovy script users' logging output.
 */
class MatrixLoggingSmokeTest {

  private final PrintStream originalErr = System.err;
  private ByteArrayOutputStream captured;

  @BeforeEach
  void redirectErr() {
    captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true));
  }

  @AfterEach
  void restoreErr() {
    System.setErr(originalErr);
  }

  @Test
  void systemLoggerRoutesToSlf4jSimple() {
    String marker = "matrix-logging-system-logger-" + System.nanoTime();
    System.getLogger(MatrixLoggingSmokeTest.class.getName()).log(Level.ERROR, marker);

    assertTrue(captured.toString().contains(marker),
        "Expected System.Logger message to reach slf4j-simple via slf4j-jdk-platform-logging");
  }

  @Test
  void log4jApiRoutesToSlf4jSimple() {
    String marker = "matrix-logging-log4j-api-" + System.nanoTime();
    LogManager.getLogger(MatrixLoggingSmokeTest.class).error(marker);

    assertTrue(captured.toString().contains(marker),
        "Expected Log4j API message to reach slf4j-simple via log4j-to-slf4j");
  }
}
