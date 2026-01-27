package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight logging utility that automatically detects and uses SLF4J if available,
 * otherwise falls back to System.out/err. This supports both project dependency usage
 * (with SLF4J) and Groovy scripting usage (without SLF4J setup).
 *
 * <p><strong>SLF4J Detection:</strong> The logger detects if an SLF4J <em>implementation</em>
 * is available (not just the API). In environments where only the SLF4J API is present
 * (e.g., Groovy REPL with full distribution), the logger will correctly fall back to
 * System.out/err to ensure messages are actually logged.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * private static final Logger log = Logger.getLogger(MyClass)
 * log.info("Processing $count items")
 * log.warn("Failed to process item", exception)
 * </pre>
 */
@CompileStatic
class Logger {

  // Static fields
  private static final boolean SLF4J_AVAILABLE
  private static final ConcurrentHashMap<String, Logger> LOGGER_CACHE = new ConcurrentHashMap<>()
  private static volatile LogLevel CURRENT_LEVEL = LogLevel.INFO
  private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  // Instance fields
  private final String className
  private final Object slf4jLogger  // org.slf4j.Logger if available

  // Static initialization - detect SLF4J implementation (not just API)
  static {
    boolean slf4jDetected = false
    try {
      // First check if SLF4J API is available
      Class.forName('org.slf4j.LoggerFactory')
      Class.forName('org.slf4j.Logger')

      // Then check if there's an actual implementation (not just NOP logger)
      // Get a test logger and check if it's a NOP logger
      def loggerFactoryClass = Class.forName('org.slf4j.LoggerFactory')
      def getLoggerMethod = loggerFactoryClass.getMethod('getLogger', String)
      def testLogger = getLoggerMethod.invoke(null, 'se.alipsa.matrix.core.util.Logger.test')

      // If the logger's class name contains "NOP", there's no implementation
      String loggerClassName = testLogger.getClass().getName()
      if (!loggerClassName.contains('NOP')) {
        slf4jDetected = true
      }
    } catch (Exception ignored) {
      // SLF4J not available or no implementation, will use fallback
    }
    SLF4J_AVAILABLE = slf4jDetected
  }

  /**
   * Private constructor - use factory methods instead
   */
  private Logger(String className) {
    this.className = className
    this.slf4jLogger = SLF4J_AVAILABLE ? createSlf4jLogger(className) : null
  }

  /**
   * Get or create a logger for the specified class
   */
  static Logger getLogger(Class<?> clazz) {
    getLogger(clazz.name)
  }

  /**
   * Get or create a logger for the specified name
   */
  static Logger getLogger(String name) {
    LOGGER_CACHE.computeIfAbsent(name) { new Logger(it) }
  }

  /**
   * Set the log level for fallback mode (only affects fallback, not SLF4J)
   */
  static void setLevel(LogLevel level) {
    CURRENT_LEVEL = level
  }

  /**
   * Get the current log level for fallback mode
   */
  static LogLevel getLevel() {
    CURRENT_LEVEL
  }

  /**
   * Check if SLF4J is available
   */
  static boolean isSlf4jAvailable() {
    SLF4J_AVAILABLE
  }

  // DEBUG level logging

  void debug(String message) {
    log(LogLevel.DEBUG, message, null, null)
  }

  void debug(String format, Object... args) {
    log(LogLevel.DEBUG, format, args, null)
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  boolean isDebugEnabled() {
    if (SLF4J_AVAILABLE && slf4jLogger != null) {
      return slf4jLogger.isDebugEnabled()
    }
    return CURRENT_LEVEL.ordinal() <= LogLevel.DEBUG.ordinal()
  }

  // INFO level logging

  void info(String message) {
    log(LogLevel.INFO, message, null, null)
  }

  void info(String format, Object... args) {
    log(LogLevel.INFO, format, args, null)
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  boolean isInfoEnabled() {
    if (SLF4J_AVAILABLE && slf4jLogger != null) {
      return slf4jLogger.isInfoEnabled()
    }
    return CURRENT_LEVEL.ordinal() <= LogLevel.INFO.ordinal()
  }

  // WARN level logging

  void warn(String message) {
    log(LogLevel.WARN, message, null, null)
  }

  void warn(String format, Object... args) {
    log(LogLevel.WARN, format, args, null)
  }

  void warn(String message, Throwable t) {
    log(LogLevel.WARN, message, null, t)
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  boolean isWarnEnabled() {
    if (SLF4J_AVAILABLE && slf4jLogger != null) {
      return slf4jLogger.isWarnEnabled()
    }
    return CURRENT_LEVEL.ordinal() <= LogLevel.WARN.ordinal()
  }

  // ERROR level logging

  void error(String message) {
    log(LogLevel.ERROR, message, null, null)
  }

  void error(String format, Object... args) {
    log(LogLevel.ERROR, format, args, null)
  }

  void error(String message, Throwable t) {
    log(LogLevel.ERROR, message, null, t)
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  boolean isErrorEnabled() {
    if (SLF4J_AVAILABLE && slf4jLogger != null) {
      return slf4jLogger.isErrorEnabled()
    }
    return CURRENT_LEVEL.ordinal() <= LogLevel.ERROR.ordinal()
  }

  // Private helper methods

  private void log(LogLevel level, String format, Object[] args, Throwable t) {
    if (SLF4J_AVAILABLE && slf4jLogger != null) {
      logWithSlf4j(level, format, args, t)
    } else {
      logWithFallback(level, format, args, t)
    }
  }

  @CompileStatic(TypeCheckingMode.SKIP)
  private void logWithSlf4j(LogLevel level, String format, Object[] args, Throwable t) {
    // Convert String.format style (%s, %d, etc.) to SLF4J style ({})
    String slf4jFormat = format
        .replaceAll('%s', '{}')
        .replaceAll('%d', '{}')
        .replaceAll('%f', '{}')
        .replaceAll('%b', '{}')
        .replaceAll('%c', '{}')
        .replaceAll('%n', '{}')
        .replaceAll('%x', '{}')
        .replaceAll('%o', '{}')

    switch (level) {
      case LogLevel.DEBUG:
        if (t != null) {
          slf4jLogger.debug(slf4jFormat, t)
        } else if (args != null && args.length > 0) {
          slf4jLogger.debug(slf4jFormat, args)
        } else {
          slf4jLogger.debug(slf4jFormat)
        }
        break
      case LogLevel.INFO:
        if (t != null) {
          slf4jLogger.info(slf4jFormat, t)
        } else if (args != null && args.length > 0) {
          slf4jLogger.info(slf4jFormat, args)
        } else {
          slf4jLogger.info(slf4jFormat)
        }
        break
      case LogLevel.WARN:
        if (t != null) {
          slf4jLogger.warn(slf4jFormat, t)
        } else if (args != null && args.length > 0) {
          slf4jLogger.warn(slf4jFormat, args)
        } else {
          slf4jLogger.warn(slf4jFormat)
        }
        break
      case LogLevel.ERROR:
        if (t != null) {
          slf4jLogger.error(slf4jFormat, t)
        } else if (args != null && args.length > 0) {
          slf4jLogger.error(slf4jFormat, args)
        } else {
          slf4jLogger.error(slf4jFormat)
        }
        break
    }
  }

  private void logWithFallback(LogLevel level, String format, Object[] args, Throwable t) {
    // Check if this level is enabled
    if (CURRENT_LEVEL.ordinal() > level.ordinal()) {
      return
    }

    // Format the message
    String message = (args != null && args.length > 0) ? String.format(format, args) : format

    // Build the log line
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
    String levelStr = String.format("%-5s", level.name())
    String logLine = "${timestamp} [${levelStr}] ${className} - ${message}"

    // Output to appropriate stream
    PrintStream out = (level == LogLevel.DEBUG || level == LogLevel.INFO) ? System.out : System.err

    out.println(logLine)

    // Print exception if present
    if (t != null) {
      t.printStackTrace(out)
    }
  }

  private static Object createSlf4jLogger(String className) {
    try {
      Class<?> loggerFactoryClass = Class.forName('org.slf4j.LoggerFactory')
      def getLoggerMethod = loggerFactoryClass.getMethod('getLogger', String)
      return getLoggerMethod.invoke(null, className)
    } catch (Exception e) {
      // Should not happen since we already checked SLF4J availability
      throw new RuntimeException("Failed to create SLF4J logger", e)
    }
  }
}

/**
 * Log levels supported by the Logger
 */
@CompileStatic
enum LogLevel {
  DEBUG,
  INFO,
  WARN,
  ERROR
}
