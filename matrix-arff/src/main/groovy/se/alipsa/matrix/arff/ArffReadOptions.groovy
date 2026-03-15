package se.alipsa.matrix.arff

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Typed options for ARFF read operations via the SPI.
 */
@CompileStatic
class ArffReadOptions {

  String matrixName = null

  ArffReadOptions matrixName(String value) {
    this.matrixName = value
    this
  }

  static ArffReadOptions fromMap(Map<String, ?> options) {
    ArffReadOptions result = new ArffReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('matrixname')) {
      String matrixName = OptionMaps.stringValueOrNull(normalized.matrixname)
      if (matrixName != null) {
        result.matrixName(matrixName)
      }
    }
    result
  }

  Map<String, ?> toMap() {
    matrixName == null ? [:] : [matrixName: matrixName]
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('matrixName', String, null, 'Fallback Matrix name when the ARFF file has no @RELATION')
    ]
  }
}
