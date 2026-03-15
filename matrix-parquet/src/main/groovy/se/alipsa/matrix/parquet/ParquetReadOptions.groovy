package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.ZoneId

/**
 * Typed options for Parquet read operations via the SPI.
 */
@CompileStatic
class ParquetReadOptions {

  String matrixName = null
  ZoneId zoneId = null

  ParquetReadOptions matrixName(String value) {
    this.matrixName = value
    this
  }

  ParquetReadOptions zoneId(ZoneId value) {
    this.zoneId = value
    this
  }

  ParquetReadOptions zoneId(String value) {
    this.zoneId = ZoneId.of(value)
    this
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [:]
    if (matrixName != null) {
      result.matrixName = matrixName
    }
    if (zoneId != null) {
      result.zoneId = zoneId
    }
    result
  }

  static ParquetReadOptions fromMap(Map<String, ?> options) {
    ParquetReadOptions result = new ParquetReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('matrixname')) {
      String matrixName = OptionMaps.stringValueOrNull(normalized.matrixname)
      if (matrixName != null) {
        result.matrixName(matrixName)
      }
    }
    if (normalized.containsKey('zoneid')) {
      def value = normalized.zoneid
      if (value instanceof ZoneId) {
        result.zoneId(value as ZoneId)
      } else if (value != null) {
        result.zoneId(String.valueOf(value))
      }
    }
    result
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('matrixName', String, null, 'Name for the resulting Matrix'),
        new OptionDescriptor('zoneId', ZoneId, null, 'Time zone to use when reading timestamp values')
    ]
  }
}
