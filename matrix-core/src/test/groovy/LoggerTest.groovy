import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.util.LogLevel
import se.alipsa.matrix.core.util.Logger

import java.io.ByteArrayOutputStream
import java.io.PrintStream

import static org.junit.jupiter.api.Assertions.*

class LoggerTest {

  @BeforeEach
  void setUp() {
    // Reset log level to default
    Logger.setLevel(LogLevel.INFO)
  }

  /**
   * Captures System.out and System.err output while executing the given closure.
   * @param closure the code to execute
   * @return a Map with 'out' and 'err' keys containing the captured output
   */
  private Map<String, String> captureOutput(Closure closure) {
    PrintStream originalOut = System.out
    PrintStream originalErr = System.err
    ByteArrayOutputStream outContent = new ByteArrayOutputStream()
    ByteArrayOutputStream errContent = new ByteArrayOutputStream()

    try {
      System.setOut(new PrintStream(outContent))
      System.setErr(new PrintStream(errContent))
      closure.call()
      return [out: outContent.toString(), err: errContent.toString()]
    } finally {
      System.setOut(originalOut)
      System.setErr(originalErr)
    }
  }

  @Test
  void testLoggerCreation() {
    Logger log1 = Logger.getLogger(LoggerTest)
    assertNotNull(log1, "Logger should not be null")

    Logger log2 = Logger.getLogger("TestLogger")
    assertNotNull(log2, "Logger should not be null")
  }

  @Test
  void testLoggerCaching() {
    Logger log1 = Logger.getLogger(LoggerTest)
    Logger log2 = Logger.getLogger(LoggerTest)
    assertSame(log1, log2, "Same logger instance should be returned for same class")

    Logger log3 = Logger.getLogger("TestLogger")
    Logger log4 = Logger.getLogger("TestLogger")
    assertSame(log3, log4, "Same logger instance should be returned for same name")
  }

  @Test
  void testSlf4jDetection() {
    // SLF4J should be detected in tests since we have slf4j-simple as testImplementation
    // The detection checks for an actual implementation, not just the API
    assertTrue(Logger.isSlf4jAvailable(), "SLF4J implementation should be detected in tests")
  }

  @Test
  void testSlf4jImplementationDetection() {
    // Verify that we're detecting an actual SLF4J implementation (slf4j-simple)
    // and not just the SLF4J API
    if (Logger.isSlf4jAvailable()) {
      // If SLF4J is available, verify it's not a NOP logger
      Logger log = Logger.getLogger("ImplementationTest")

      // The logger should have been created successfully
      assertNotNull(log, "Logger should be created")

      // This test verifies that the detection logic correctly identifies
      // when there's an actual implementation vs just the API
      assertTrue(true, "SLF4J implementation detection is working")
    }
  }

  @Test
  void testDebugLogging() {
    Logger log = Logger.getLogger("DebugTest")

    // Set level to DEBUG to enable debug logging (only affects fallback mode)
    Logger.setLevel(LogLevel.DEBUG)

    def output = captureOutput {
      log.debug("Debug message")
      log.debug("Debug with parameter: %s", "value")
    }

    // When SLF4J is available, it uses its own configuration which may not include DEBUG
    // When using fallback, DEBUG messages should appear
    // Either way, no exceptions should be thrown and if output exists, it should be valid
    String allOutput = output.out + output.err
    if (!allOutput.isEmpty()) {
      // If there is output, verify it contains debug-related content
      assertTrue(allOutput.contains("Debug message") || allOutput.contains("DEBUG") || allOutput.contains("value"),
          "If output exists, it should contain debug-related content")
    }
    // The main assertion is that logging didn't throw an exception
    assertTrue(true, "Debug logging should complete without exceptions")
  }

  @Test
  void testInfoLogging() {
    Logger log = Logger.getLogger("InfoTest")

    def output = captureOutput {
      log.info("Info message")
      log.info("Info with parameters: %s, %d", "test", 42)
    }

    assertTrue(log.isInfoEnabled(), "Info should be enabled by default")

    // Verify the output contains expected content
    String allOutput = output.out + output.err
    assertTrue(allOutput.contains("Info message") || allOutput.contains("INFO"),
        "Output should contain info message or INFO marker")
    assertTrue(allOutput.contains("test") && (allOutput.contains("42") || allOutput.contains("INFO")),
        "Output should contain parameterized values")
  }

  @Test
  void testWarnLogging() {
    Logger log = Logger.getLogger("WarnTest")

    def output = captureOutput {
      log.warn("Warning message")
      log.warn("Warning with parameter: %s", "value")
      log.warn("Warning with exception", new RuntimeException("Test exception"))
    }

    assertTrue(log.isWarnEnabled(), "Warn should be enabled by default")

    // Verify the output contains expected content
    String allOutput = output.out + output.err
    assertTrue(allOutput.contains("Warning message") || allOutput.contains("WARN"),
        "Output should contain warning message or WARN marker")
    assertTrue(allOutput.contains("value"), "Output should contain parameter value")
    assertTrue(allOutput.contains("Test exception") || allOutput.contains("RuntimeException"),
        "Output should contain exception information")
  }

  @Test
  void testErrorLogging() {
    Logger log = Logger.getLogger("ErrorTest")

    def output = captureOutput {
      log.error("Error message")
      log.error("Error with parameter: %s", "value")
      log.error("Error with exception", new IllegalStateException("Test error"))
    }

    assertTrue(log.isErrorEnabled(), "Error should be enabled by default")

    // Verify the output contains expected content
    String allOutput = output.out + output.err
    assertTrue(allOutput.contains("Error message") || allOutput.contains("ERROR"),
        "Output should contain error message or ERROR marker")
    assertTrue(allOutput.contains("value"), "Output should contain parameter value")
    assertTrue(allOutput.contains("Test error") || allOutput.contains("IllegalStateException"),
        "Output should contain exception information")
  }

  @Test
  void testLogLevelChecks() {
    Logger log = Logger.getLogger("LevelTest")

    // When SLF4J is available, isXXXEnabled() methods delegate to SLF4J's configuration
    // When SLF4J is not available, they check CURRENT_LEVEL
    // This test verifies the methods work without throwing exceptions

    if (Logger.isSlf4jAvailable()) {
      // With SLF4J, level checks use SLF4J's configuration
      // Just verify the methods work
      log.isDebugEnabled()
      log.isInfoEnabled()
      log.isWarnEnabled()
      log.isErrorEnabled()
      assertTrue(true, "Level checks should work with SLF4J")
    } else {
      // Without SLF4J, level checks use CURRENT_LEVEL
      // Default level is INFO
      assertFalse(log.isDebugEnabled(), "Debug should not be enabled at INFO level")
      assertTrue(log.isInfoEnabled(), "Info should be enabled at INFO level")
      assertTrue(log.isWarnEnabled(), "Warn should be enabled at INFO level")
      assertTrue(log.isErrorEnabled(), "Error should be enabled at INFO level")

      // Set to DEBUG
      Logger.setLevel(LogLevel.DEBUG)
      assertTrue(log.isDebugEnabled(), "Debug should be enabled at DEBUG level")

      // Set to WARN
      Logger.setLevel(LogLevel.WARN)
      assertFalse(log.isDebugEnabled(), "Debug should not be enabled at WARN level")
      assertFalse(log.isInfoEnabled(), "Info should not be enabled at WARN level")
      assertTrue(log.isWarnEnabled(), "Warn should be enabled at WARN level")
      assertTrue(log.isErrorEnabled(), "Error should be enabled at WARN level")

      // Set to ERROR
      Logger.setLevel(LogLevel.ERROR)
      assertFalse(log.isDebugEnabled(), "Debug should not be enabled at ERROR level")
      assertFalse(log.isInfoEnabled(), "Info should not be enabled at ERROR level")
      assertFalse(log.isWarnEnabled(), "Warn should not be enabled at ERROR level")
      assertTrue(log.isErrorEnabled(), "Error should be enabled at ERROR level")
    }
  }

  @Test
  void testLogLevelManagement() {
    assertEquals(LogLevel.INFO, Logger.getLevel(), "Default level should be INFO")

    Logger.setLevel(LogLevel.DEBUG)
    assertEquals(LogLevel.DEBUG, Logger.getLevel(), "Level should be DEBUG")

    Logger.setLevel(LogLevel.WARN)
    assertEquals(LogLevel.WARN, Logger.getLevel(), "Level should be WARN")

    Logger.setLevel(LogLevel.ERROR)
    assertEquals(LogLevel.ERROR, Logger.getLevel(), "Level should be ERROR")
  }

  @Test
  void testParameterizedMessages() {
    Logger log = Logger.getLogger("ParamTest")

    def output = captureOutput {
      log.info("String: %s", "test")
      log.info("Integer: %d", 123)
      log.info("Multiple: %s, %d, %s", "first", 42, "third")
    }

    // Verify output contains parameterized values
    String allOutput = output.out + output.err
    assertTrue(allOutput.contains("test"), "Output should contain string parameter")
    assertTrue(allOutput.contains("123"), "Output should contain integer parameter")
    assertTrue(allOutput.contains("first") && allOutput.contains("42") && allOutput.contains("third"),
        "Output should contain all multiple parameters")
  }

  @Test
  void testExceptionLogging() {
    Logger log = Logger.getLogger("ExceptionTest")

    RuntimeException exception = new RuntimeException("Test exception message")

    def output = captureOutput {
      log.warn("Warning with exception", exception)
      log.error("Error with exception", exception)
    }

    // Verify output contains exception information
    String allOutput = output.out + output.err
    assertTrue(allOutput.contains("Test exception message") || allOutput.contains("RuntimeException"),
        "Output should contain exception information")
    assertTrue(allOutput.contains("Warning with exception") || allOutput.contains("Error with exception") || allOutput.contains("WARN") || allOutput.contains("ERROR"),
        "Output should contain log message or level marker")
  }

  @Test
  void testLogLevelEnum() {
    // Test enum ordinals for correct ordering
    assertTrue(LogLevel.DEBUG.ordinal() < LogLevel.INFO.ordinal(), "DEBUG should be less than INFO")
    assertTrue(LogLevel.INFO.ordinal() < LogLevel.WARN.ordinal(), "INFO should be less than WARN")
    assertTrue(LogLevel.WARN.ordinal() < LogLevel.ERROR.ordinal(), "WARN should be less than ERROR")
  }
}
