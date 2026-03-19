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

  JsonReadOptions charset(Charset value) {
    this.charset = value
    this
  }

  JsonReadOptions charset(String value) {
    this.charset = Charset.forName(value)
    this
  }

  static JsonReadOptions fromMap(Map<String, ?> options) {
    JsonReadOptions result = new JsonReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('charset')) {
      def value = normalized.charset
      if (value instanceof Charset) {
        result.charset(value as Charset)
      } else if (value instanceof CharSequence) {
        result.charset(String.valueOf(value))
      } else if (value == null) {
        // Treat null as absent to preserve the default charset.
      } else {
        throw new IllegalArgumentException("charset must be a Charset or String but was ${value?.class}")
      }
    }
    result
  }

  Map<String, ?> toMap() {
    [charset: charset]
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('charset', Charset, 'UTF-8', 'The character encoding to use when reading JSON')
    ]
  }
}
