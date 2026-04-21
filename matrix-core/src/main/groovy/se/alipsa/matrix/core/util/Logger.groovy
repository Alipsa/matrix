package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight logging utility backed by the JDK {@link System.Logger} facade.
 * This keeps Matrix free of logging-framework dependencies while still allowing
 * applications to route logs through the JVM's configured logger backend.
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

  private static final String PLATFORM_NEWLINE_FORMAT = '%n'

  private static final Map<String, Logger> LOGGER_CACHE = new ConcurrentHashMap<>()
  private static volatile LogLevel CURRENT_LEVEL = LogLevel.INFO

  private final System.Logger systemLogger

  /**
   * Private constructor - use factory methods instead
   */
  private Logger(String className) {
    this.systemLogger = System.getLogger(className)
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
   * Set Matrix's minimum log level. The JVM logging backend may still apply
   * additional filtering.
   */
  static void setLevel(LogLevel level) {
    CURRENT_LEVEL = level
  }

  /**
   * Get the current Matrix log level.
   */
  @SuppressWarnings('GetterMethodCouldBeProperty')
  static LogLevel getLevel() {
    CURRENT_LEVEL
  }

  /**
   * Legacy compatibility method for callers that checked SLF4J availability.
   *
   * @deprecated Matrix logging no longer integrates with SLF4J directly. This
   * method is retained for source compatibility and always returns false.
   */
  @Deprecated
  static boolean isSlf4jAvailable() {
    false
  }

  // DEBUG level logging

  void debug(String message) {
    log(LogLevel.DEBUG, message, null, null)
  }

  void debug(String format, Object... args) {
    log(LogLevel.DEBUG, format, args, null)
  }

  boolean isDebugEnabled() {
    isEnabled(LogLevel.DEBUG)
  }

  // INFO level logging

  void info(String message) {
    log(LogLevel.INFO, message, null, null)
  }

  void info(String format, Object... args) {
    log(LogLevel.INFO, format, args, null)
  }

  boolean isInfoEnabled() {
    isEnabled(LogLevel.INFO)
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

  boolean isWarnEnabled() {
    isEnabled(LogLevel.WARN)
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

  boolean isErrorEnabled() {
    isEnabled(LogLevel.ERROR)
  }

  // Private helper methods

  private void log(LogLevel level, String format, Object[] args, Throwable t) {
    if (!isEnabled(level)) {
      return
    }

    System.Logger.Level systemLevel = toSystemLevel(level)
    String message = formatMessage(format, args)
    if (t != null) {
      systemLogger.log(systemLevel, message, t)
    } else {
      systemLogger.log(systemLevel, message)
    }
  }

  private boolean isEnabled(LogLevel level) {
    CURRENT_LEVEL.ordinal() <= level.ordinal() && systemLogger.isLoggable(toSystemLevel(level))
  }

  private static String formatMessage(String format, Object[] args) {
    String normalizedFormat = format.replace(PLATFORM_NEWLINE_FORMAT, System.lineSeparator())
    if (args == null || args.length == 0) {
      return normalizedFormat
    }
    String.format(normalizedFormat, args)
  }

  private static System.Logger.Level toSystemLevel(LogLevel level) {
    switch (level) {
      case LogLevel.DEBUG:
        return System.Logger.Level.DEBUG
      case LogLevel.INFO:
        return System.Logger.Level.INFO
      case LogLevel.WARN:
        return System.Logger.Level.WARNING
      case LogLevel.ERROR:
        return System.Logger.Level.ERROR
      default:
        return System.Logger.Level.INFO
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
