package se.alipsa.matrix.json

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Typed options for JSON read operations via the SPI.
 */
@CompileStatic
class JsonReadOptions {

  Charset charset = StandardCharsets.UTF_8
  String matrixName = null
  List<Class> types = null
  String dateTimeFormat = null

  JsonReadOptions charset(Charset value) {
    this.charset = value
    this
  }

  JsonReadOptions charset(String value) {
    this.charset = Charset.forName(value)
    this
  }

  /**
   * Set the name for the resulting Matrix, overriding any file-derived name.
   *
   * @param value the matrix name
   * @return this options instance for chaining
   */
  JsonReadOptions matrixName(String value) {
    this.matrixName = value
    this
  }

  /**
   * Set column types for automatic conversion after parsing.
   *
   * @param value list of column type classes
   * @return this options instance for chaining
   */
  JsonReadOptions types(List<Class> value) {
    this.types = value
    this
  }

  /**
   * Set the date/time format pattern used during type conversion.
   *
   * @param value date/time format pattern (e.g. 'yyyy-MM-dd')
   * @return this options instance for chaining
   */
  JsonReadOptions dateTimeFormat(String value) {
    this.dateTimeFormat = value
    this
  }

  static JsonReadOptions fromMap(Map<String, ?> options) {
    JsonReadOptions result = new JsonReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('charset')) {
      Object value = normalized.charset
      if (value instanceof Charset) {
        result.charset(value as Charset)
      } else if (value instanceof CharSequence) {
        result.charset(String.valueOf(value))
      } else if (value != null) {
        throw new IllegalArgumentException("charset must be a Charset or String but was ${value?.class}")
      }
    }
    if (normalized.containsKey('matrixname') || normalized.containsKey('tablename')) {
      String key = normalized.containsKey('matrixname') ? 'matrixname' : 'tablename'
      Object val = normalized[key]
      if (val != null) {
        result.matrixName(String.valueOf(val))
      }
    }
    if (normalized.containsKey('types')) {
      Object value = normalized.types
      if (value instanceof List) {
        result.types(value as List<Class>)
      } else if (value != null && value.getClass().isArray()) {
        result.types((value as Class[]).toList())
      }
    }
    if (normalized.containsKey('datetimeformat')) {
      Object value = normalized.datetimeformat
      if (value != null) {
        result.dateTimeFormat(String.valueOf(value))
      }
    }
    result
  }

  Map<String, ?> toMap() {
    Map<String, Object> m = [charset: (Object) charset]
    if (matrixName != null) {
      m.matrixName = matrixName
    }
    if (types != null) {
      m.types = types
    }
    if (dateTimeFormat != null) {
      m.dateTimeFormat = dateTimeFormat
    }
    m
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding to use when reading JSON'),
        new OptionDescriptor('matrixName', String, null, 'Override the name for the resulting Matrix (default: derived from filename)'),
        new OptionDescriptor('types', List, null, 'List of column types for automatic conversion after parsing'),
        new OptionDescriptor('dateTimeFormat', String, null, 'Date/time format pattern for type conversion')
    ]
  }
}
