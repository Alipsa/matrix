package se.alipsa.matrix.parquet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.ZoneId

/**
 * Typed options for Parquet write operations via the SPI.
 */
@CompileStatic
class ParquetWriteOptions {

  boolean inferPrecisionAndScale = true
  Integer precision = null
  Integer scale = null
  Map<String, int[]> decimalMeta = [:]
  ZoneId zoneId = null

  ParquetWriteOptions inferPrecisionAndScale(boolean value) {
    this.inferPrecisionAndScale = value
    this
  }

  ParquetWriteOptions precision(int value) {
    this.precision = value
    this
  }

  ParquetWriteOptions scale(int value) {
    this.scale = value
    this
  }

  ParquetWriteOptions decimalMeta(Map<String, int[]> value) {
    this.decimalMeta = value ?: [:]
    this
  }

  ParquetWriteOptions zoneId(ZoneId value) {
    this.zoneId = value
    this
  }

  ParquetWriteOptions zoneId(String value) {
    this.zoneId = ZoneId.of(value)
    this
  }

  boolean hasFixedPrecisionAndScale() {
    precision != null || scale != null
  }

  boolean hasDecimalMeta() {
    !decimalMeta.isEmpty()
  }

  void validate() {
    if ((precision == null) != (scale == null)) {
      throw new IllegalArgumentException('precision and scale must be provided together')
    }
    validateDecimalMeta(decimalMeta)
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [inferPrecisionAndScale: inferPrecisionAndScale]
    if (precision != null) {
      result.precision = precision
      result.scale = scale
    }
    if (!decimalMeta.isEmpty()) {
      result.decimalMeta = decimalMeta
    }
    if (zoneId != null) {
      result.zoneId = zoneId
    }
    result
  }

  static ParquetWriteOptions fromMap(Map<String, ?> options) {
    ParquetWriteOptions result = new ParquetWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('inferprecisionandscale')) {
      Object inferPrecisionAndScale = normalized.inferprecisionandscale
      if (inferPrecisionAndScale != null) {
        result.inferPrecisionAndScale(inferPrecisionAndScale as boolean)
      }
    }
    if (normalized.containsKey('precision')) {
      Object precision = normalized.precision
      if (precision != null) {
        result.precision((precision as Number).intValue())
      }
    }
    if (normalized.containsKey('scale')) {
      Object scale = normalized.scale
      if (scale != null) {
        result.scale((scale as Number).intValue())
      }
    }
    if (normalized.containsKey('decimalmeta')) {
      def value = normalized.decimalmeta
      if (value == null) {
        // Treat null as absent to preserve defaults.
      } else if (!(value instanceof Map)) {
        throw new IllegalArgumentException("decimalMeta must be a Map<String, int[]> but was ${value?.class}")
      } else {
        result.decimalMeta(validateDecimalMeta(value as Map))
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
    result.validate()
    result
  }

  private static Map<String, int[]> validateDecimalMeta(Map<?, ?> value) {
    Map<String, int[]> validated = [:]
    value.each { key, meta ->
      String columnName = String.valueOf(key)
      if (!(meta instanceof int[]) || (meta as int[]).length != 2) {
        throw new IllegalArgumentException(
            "decimalMeta['$columnName'] must be an int[] of length 2 [precision, scale] but was ${meta?.class}")
      }
      validated[columnName] = meta as int[]
    }
    validated
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('inferPrecisionAndScale', Boolean, 'true', 'Infer precision and scale for BigDecimal columns'),
        new OptionDescriptor('precision', Integer, null, 'Uniform precision for all BigDecimal columns'),
        new OptionDescriptor('scale', Integer, null, 'Uniform scale for all BigDecimal columns'),
        new OptionDescriptor('decimalMeta', Map, null, 'Map of column names to [precision, scale] arrays'),
        new OptionDescriptor('zoneId', ZoneId, null, 'Time zone to use when writing timestamp values')
    ]
  }
}
