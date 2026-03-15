package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for ARFF write operations via the SPI.
 */
@CompileStatic
class ArffWriteOptions {

  Map<String, List<String>> nominalMappings = [:]

  ArffWriteOptions nominalMappings(Map<String, List<String>> value) {
    this.nominalMappings = value ?: [:]
    this
  }

  static ArffWriteOptions fromMap(Map<String, ?> options) {
    ArffWriteOptions result = new ArffWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('nominalmappings')) {
      def value = normalized.nominalmappings
      if (!(value instanceof Map)) {
        throw new IllegalArgumentException("nominalMappings must be a Map<String, List<String>> but was ${value?.class}")
      }
      result.nominalMappings((Map<String, List<String>>) value)
    }
    result
  }

  Map<String, ?> toMap() {
    nominalMappings.isEmpty() ? [:] : [nominalMappings: nominalMappings]
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('nominalMappings', Map, null, 'Map of column names to explicit nominal values')
    ]
  }
}
