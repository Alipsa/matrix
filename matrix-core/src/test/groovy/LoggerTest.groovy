import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.util.LogLevel
import se.alipsa.matrix.core.util.Logger

import java.lang.reflect.Method

@SuppressWarnings(['UnnecessaryGroovyImport', 'UnnecessaryGString', 'ClassEndsWithBlankLine'])
class LoggerTest {

  private static final Method FORMAT_MESSAGE = Logger.getDeclaredMethod('formatMessage', String, Object[])

  static {
    FORMAT_MESSAGE.accessible = true
  }

  @BeforeEach
  void setUp() {
    Logger.setLevel(LogLevel.INFO)
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
  void testSlf4jCompatibilityMethod() {
    assertFalse(Logger.isSlf4jAvailable(), "Matrix Logger no longer integrates directly with SLF4J")
  }

  @Test
  void testJdkLoggerFacadeCreation() {
    assertNotNull(System.getLogger('ImplementationTest'), "JDK System.Logger should be available")
  }

  @Test
  void testDebugLogging() {
    Logger log = Logger.getLogger("DebugTest")
    Logger.setLevel(LogLevel.DEBUG)

    log.debug("Debug message")
    log.debug("Debug with parameter: %s", "value")

    assertEquals(LogLevel.DEBUG, Logger.getLevel())
  }

  @Test
  void testInfoLogging() {
    Logger log = Logger.getLogger("InfoTest")
    Logger.setLevel(LogLevel.WARN)

    log.info("Info message")
    log.info("Info with parameters: %s, %d", "test", 42)

    assertFalse(log.isInfoEnabled(), "Info should not be enabled at WARN level")
  }

  @Test
  void testWarnLogging() {
    Logger log = Logger.getLogger("WarnTest")
    Logger.setLevel(LogLevel.ERROR)

    log.warn("Warning message")
    log.warn("Warning with parameter: %s", "value")
    log.warn("Warning with exception", new RuntimeException("Test exception"))

    assertFalse(log.isWarnEnabled(), "Warn should not be enabled at ERROR level")
  }

  @Test
  void testErrorLogging() {
    Logger log = Logger.getLogger("ErrorTest")

    assertTrue(log.isErrorEnabled(), "Error should be enabled by default")
  }

  @Test
  void testLogLevelChecks() {
    Logger log = Logger.getLogger("LevelTest")

    assertFalse(log.isDebugEnabled(), "Debug should not be enabled at INFO level")
    assertTrue(log.isInfoEnabled(), "Info should be enabled at INFO level")
    assertTrue(log.isWarnEnabled(), "Warn should be enabled at INFO level")
    assertTrue(log.isErrorEnabled(), "Error should be enabled at INFO level")

    Logger.setLevel(LogLevel.WARN)
    assertFalse(log.isDebugEnabled(), "Debug should not be enabled at WARN level")
    assertFalse(log.isInfoEnabled(), "Info should not be enabled at WARN level")
    assertTrue(log.isWarnEnabled(), "Warn should be enabled at WARN level")
    assertTrue(log.isErrorEnabled(), "Error should be enabled at WARN level")

    Logger.setLevel(LogLevel.ERROR)
    assertFalse(log.isDebugEnabled(), "Debug should not be enabled at ERROR level")
    assertFalse(log.isInfoEnabled(), "Info should not be enabled at ERROR level")
    assertFalse(log.isWarnEnabled(), "Warn should not be enabled at ERROR level")
    assertTrue(log.isErrorEnabled(), "Error should be enabled at ERROR level")
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
    assertEquals("String: test", formatMessage("String: %s", "test"))
    assertEquals("Integer: 123", formatMessage("Integer: %d", 123))
    assertEquals("Multiple: first, 42, third", formatMessage("Multiple: %s, %d, %s", "first", 42, "third"))
  }

  @Test
  void testPlatformNewlinePlaceholderPreservesLineBreak() {
    assertEquals("First line${System.lineSeparator()}Second line", formatMessage("First line%nSecond line"))
  }

  @Test
  void testExceptionLogging() {
    Logger log = Logger.getLogger("ExceptionTest")
    Logger.setLevel(LogLevel.ERROR)

    RuntimeException exception = new RuntimeException("Test exception message")
    log.warn("Warning with exception", exception)

    assertFalse(log.isWarnEnabled(), "Warn should not be enabled at ERROR level")
    assertTrue(log.isErrorEnabled(), "Error should be enabled by default")
  }

  @Test
  void testLogLevelEnum() {
    assertTrue(LogLevel.DEBUG.ordinal() < LogLevel.INFO.ordinal(), "DEBUG should be less than INFO")
    assertTrue(LogLevel.INFO.ordinal() < LogLevel.WARN.ordinal(), "INFO should be less than WARN")
    assertTrue(LogLevel.WARN.ordinal() < LogLevel.ERROR.ordinal(), "WARN should be less than ERROR")
  }

  private static String formatMessage(String format, Object... args) {
    FORMAT_MESSAGE.invoke(null, format, args as Object[]) as String
  }
}
