package se.alipsa.matrix.parquet

import org.apache.parquet.hadoop.metadata.CompressionCodecName

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.time.ZoneId

/**
 * Typed options for Parquet write operations via the SPI.
 */
class ParquetWriteOptions {

  private static final String KEY_PRECISION = 'precision'
  private static final String KEY_SCALE = 'scale'

  boolean inferPrecisionAndScale = true
  Integer precision = null
  Integer scale = null
  Map<String, int[]> decimalMeta = [:]
  ZoneId zoneId = null
  CompressionCodecName compressionCodec = CompressionCodecName.SNAPPY

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

  /**
   * Sets the compression codec for the Parquet file (default: {@code SNAPPY}).
   *
   * @param value the compression codec
   * @return this options instance
   */
  ParquetWriteOptions compressionCodec(CompressionCodecName value) {
    this.compressionCodec = value
    this
  }

  /**
   * Sets the compression codec from its name (default: {@code "SNAPPY"}).
   *
   * @param value the codec name, e.g. "GZIP", "ZSTD", "UNCOMPRESSED"
   * @return this options instance
   * @throws IllegalArgumentException if value is not a recognized codec name
   */
  ParquetWriteOptions compressionCodec(String value) {
    this.compressionCodec = CompressionCodecName.valueOf(value.toUpperCase())
    this
  }

  boolean hasUniformPrecisionAndScale() {
    precision != null && scale != null
  }

  boolean hasDecimalMeta() {
    !decimalMeta.isEmpty()
  }

  void validate() {
    if ((precision == null) != (scale == null)) {
      throw new IllegalArgumentException('precision and scale must be provided together')
    }
    if (precision != null) {
      if (precision <= 0) {
        throw new IllegalArgumentException("precision must be > 0 but was $precision")
      }
      if (scale < 0) {
        throw new IllegalArgumentException("scale must be >= 0 but was $scale")
      }
      if (scale > precision) {
        throw new IllegalArgumentException("scale ($scale) must not exceed precision ($precision)")
      }
    }
    decimalMeta = normalizeDecimalMeta(decimalMeta)
    if (compressionCodec == null) {
      throw new IllegalArgumentException('compressionCodec cannot be null')
    }
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [inferPrecisionAndScale: inferPrecisionAndScale, compressionCodec: compressionCodec.name()]
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
    if (normalized.containsKey(KEY_PRECISION)) {
      Object precision = normalized.precision
      if (precision != null) {
        result.precision((precision as Number).intValue())
      }
    }
    if (normalized.containsKey(KEY_SCALE)) {
      Object scale = normalized.scale
      if (scale != null) {
        result.scale((scale as Number).intValue())
      }
    }
    if (normalized.containsKey('decimalmeta')) {
      def value = normalized.decimalmeta
      // Treat null as absent to preserve defaults
      if (value != null) {
        if (!(value instanceof Map)) {
          throw new IllegalArgumentException("decimalMeta must be a Map<String, int[]> but was ${value?.class}")
        }
        result.decimalMeta(normalizeDecimalMeta(value as Map))
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
    if (normalized.containsKey('compressioncodec')) {
      def value = normalized.compressioncodec
      if (value instanceof CompressionCodecName) {
        result.compressionCodec(value as CompressionCodecName)
      } else if (value != null) {
        result.compressionCodec(String.valueOf(value))
      }
    }
    result.validate()
    result
  }

  /**
   * Normalizes and validates a raw {@code decimalMeta} map into {@code Map<String, int[]>}.
   *
   * <p>Accepts {@code int[]}, {@code Number[]}, or {@code List<Number>} values of length 2
   * {@code [precision, scale]} per column, so both direct Groovy callers and SPI/options-map
   * callers can use the same entry point.</p>
   *
   * @param value raw map of column name to a length-2 precision/scale value
   * @return normalized map of column name to validated {@code int[2]} [precision, scale]
   * @throws IllegalArgumentException if any entry has the wrong shape, type, or an out-of-range precision/scale
   */
  static Map<String, int[]> normalizeDecimalMeta(Map<?, ?> value) {
    Map<String, int[]> normalized = [:]
    value.each { key, meta ->
      String columnName = String.valueOf(key)
      int[] ps
      if (meta instanceof int[]) {
        ps = meta as int[]
      } else if (meta instanceof Number[]) {
        Number[] numbers = meta as Number[]
        ps = decimalMetaNumbers(columnName, numbers.toList())
      } else if (meta instanceof List) {
        ps = decimalMetaNumbers(columnName, meta as List<?>)
      } else {
        throw new IllegalArgumentException(
            "decimalMeta['$columnName'] must be an int[], Number[], or List<Number> of length 2 [precision, scale] but was ${meta?.class}")
      }
      if (ps.length != 2) {
        throw new IllegalArgumentException(
            "decimalMeta['$columnName'] must be an int[] of length 2 [precision, scale] but was ${meta?.class}")
      }
      if (ps[0] <= 0) {
        throw new IllegalArgumentException("decimalMeta['$columnName'] precision must be > 0 but was ${ps[0]}")
      }
      if (ps[1] < 0) {
        throw new IllegalArgumentException("decimalMeta['$columnName'] scale must be >= 0 but was ${ps[1]}")
      }
      if (ps[1] > ps[0]) {
        throw new IllegalArgumentException("decimalMeta['$columnName'] scale (${ps[1]}) must not exceed precision (${ps[0]})")
      }
      normalized[columnName] = ps
    }
    normalized
  }

  private static int[] decimalMetaNumbers(String columnName, Iterable<?> values) {
    List<Integer> result = []
    values.eachWithIndex { value, int index ->
      if (!(value instanceof Number)) {
        throw new IllegalArgumentException("decimalMeta['$columnName'][$index] must be a Number but was ${value?.class}")
      }
      result << value.intValue()
    }
    result as int[]
  }

  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('inferPrecisionAndScale', Boolean, Boolean.TRUE.toString(), 'Infer precision and scale for BigDecimal columns'),
        new OptionDescriptor(KEY_PRECISION, Integer, null, 'Uniform precision for all BigDecimal columns'),
        new OptionDescriptor(KEY_SCALE, Integer, null, 'Uniform scale for all BigDecimal columns'),
        new OptionDescriptor('decimalMeta', Map, null, 'Map of column names to [precision, scale] arrays'),
        new OptionDescriptor('zoneId', ZoneId, null, 'Time zone to use when writing timestamp values'),
        new OptionDescriptor('compressionCodec', CompressionCodecName, CompressionCodecName.SNAPPY.name(), 'Compression codec for the Parquet file (e.g. SNAPPY, GZIP, ZSTD, UNCOMPRESSED)')
    ]
  }
}
