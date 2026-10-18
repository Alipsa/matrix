package se.alipsa.matrix.parquet

import org.apache.parquet.hadoop.metadata.CompressionCodecName

import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

import java.math.RoundingMode
import java.time.ZoneId

/**
 * Typed options for Parquet write operations via the SPI.
 */
class ParquetWriteOptions {
  private static final String MICROS = 'MICROS'
  private static final String ERR_CODEC_NULL = 'compressionCodec cannot be null'
  private static final String KEY_BLOOM_ENABLED = 'bloomFilterEnabled'
  private static final String KEY_BLOOM_NDV = 'bloomFilterNDV'
  private static final String KEY_DICTIONARY_ENCODING = 'dictionaryEncoding'
  private static final String KEY_WRITER_VERSION = 'writerVersion'
  private static final String KEY_SIZE_STATISTICS_ENABLED = 'sizeStatisticsEnabled'

  private static final String KEY_V1 = 'V1'


  private static final String KEY_PRECISION = 'precision'
  private static final String KEY_SCALE = 'scale'
  private static final String KEY_INFER_PRECISION_AND_SCALE = 'inferPrecisionAndScale'
  private static final String KEY_ROUNDING_MODE = 'roundingMode'
  private static final String ERR_ROUNDING_MODE_NULL = 'roundingMode cannot be null'

  String timestampUnit = MICROS

  /**
   * Sets timestamp precision (default MICROS). MICROS truncates sub-microsecond digits
   * downward in time for LocalDateTime, Timestamp and Instant; NANOS preserves them.
   * Spark-family readers may not support TIMESTAMP(NANOS). NANOS covers
   * 1677-09-21T00:12:43.145224192Z to 2262-04-11T23:47:16.854775807Z;
   * values outside that range fail. MICROS covers about ±292,000 years.
   * @param value MICROS or NANOS, trimmed and case-insensitive
   * @return this options instance
   */
  ParquetWriteOptions timestampUnit(String value) {
    if (value == null) { throw new IllegalArgumentException('timestampUnit cannot be null') }
    String unit = value.trim().toUpperCase(Locale.ROOT)
    if (!(unit in [MICROS, 'NANOS'])) { throw new IllegalArgumentException("Unknown timestampUnit '$value'; use MICROS or NANOS") }
    timestampUnit = unit
    this
  }

  long rowGroupSize = 134217728L

  /** Sets rowGroupSize (default 134217728L); target sizes are not hard memory limits. */
  ParquetWriteOptions rowGroupSize(long value) { this.rowGroupSize = value; this }

  int pageSize = 1048576

  /** Sets pageSize (default 1048576); target sizes are not hard memory limits. */
  ParquetWriteOptions pageSize(int value) { this.pageSize = value; this }

  boolean dictionaryEncoding = true

  /** Enable dictionary encoding with plain-page fallback (default true). */
  ParquetWriteOptions dictionaryEncoding(boolean value) { this.dictionaryEncoding = value; this }

  String writerVersion = KEY_V1

  /** Choose V1 or V2 value encodings and page layout (default V1). */
  ParquetWriteOptions writerVersion(String value) { this.writerVersion = value; this }

  int columnIndexTruncateLength = 64

  /** Maximum binary bound bytes in page indexes (default 64). */
  ParquetWriteOptions columnIndexTruncateLength(int value) { this.columnIndexTruncateLength = value; this }

  int statisticsTruncateLength = Integer.MAX_VALUE

  /** Maximum binary bound bytes in row-group statistics (default 2147483647). */
  ParquetWriteOptions statisticsTruncateLength(int value) { this.statisticsTruncateLength = value; this }

  Map<String, Boolean> bloomFilterEnabled = [:]

  /** Per-primitive-column bloom overrides (default empty, disabled). */
  ParquetWriteOptions bloomFilterEnabled(Map<String, Boolean> value) { this.bloomFilterEnabled = value; this }

  Map<String, Long> bloomFilterNDV = [:]

  /** Positive expected distinct counts for bloom sizing (default empty). */
  ParquetWriteOptions bloomFilterNDV(Map<String, Long> value) { this.bloomFilterNDV = value; this }

  boolean sizeStatisticsEnabled = true

  /** Collect column size statistics (default true). */
  ParquetWriteOptions sizeStatisticsEnabled(boolean value) { this.sizeStatisticsEnabled = value; this }

  boolean inferPrecisionAndScale = true
  Integer precision = null
  Integer scale = null
  Map<String, int[]> decimalMeta = [:]
  ZoneId zoneId = null
  CompressionCodecName compressionCodec = CompressionCodecName.SNAPPY
  RoundingMode roundingMode = RoundingMode.UNNECESSARY

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
    if (value == null) { throw new IllegalArgumentException(ERR_CODEC_NULL) }
    try { this.compressionCodec = CompressionCodecName.valueOf(value.trim().toUpperCase(Locale.ROOT)) }
    catch (IllegalArgumentException failure) {
      throw new IllegalArgumentException("Unknown compressionCodec '$value'; valid names: ${CompressionCodecName.values()*.name()}", failure)
    }
    this
  }

  /**
   * Sets the rounding mode used when a BigDecimal is rescaled for a DECIMAL field
   * (default: {@link RoundingMode#UNNECESSARY}).
   *
   * @param value the rounding mode
   * @return this options instance
   */
  ParquetWriteOptions roundingMode(RoundingMode value) {
    this.roundingMode = value
    this
  }

  /**
   * Sets the rounding mode by its trimmed, case-insensitive enum name.
   *
   * @param value the rounding mode name, for example {@code "HALF_UP"}
   * @return this options instance
   * @throws IllegalArgumentException if value is null or not a recognized rounding mode
   */
  ParquetWriteOptions roundingMode(String value) {
    if (value == null) {
      throw new IllegalArgumentException(ERR_ROUNDING_MODE_NULL)
    }
    try {
      this.roundingMode = RoundingMode.valueOf(value.trim().toUpperCase(Locale.ROOT))
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unknown roundingMode '$value'", exception)
    }
    this
  }

  boolean hasUniformPrecisionAndScale() {
    precision != null && scale != null
  }

  boolean hasDecimalMeta() {
    !decimalMeta.isEmpty()
  }

  void validate() {
    timestampUnit(timestampUnit)
    if (rowGroupSize <= 0 || pageSize <= 0 || columnIndexTruncateLength <= 0 || statisticsTruncateLength <= 0) {
      throw new IllegalArgumentException('Writer sizes/truncate lengths must be positive')
    }
    if (!(writerVersion in [KEY_V1, 'V2'])) { throw new IllegalArgumentException('writerVersion must be V1 or V2') }
    if (bloomFilterEnabled == null || bloomFilterNDV == null) { throw new IllegalArgumentException('Bloom option maps cannot be null') }
    bloomFilterEnabled.each { String path, Object value ->
      if (!(value instanceof Boolean)) { throw new IllegalArgumentException("bloomFilterEnabled['$path'] must be a non-null Boolean") }
    }
    bloomFilterNDV.each { String path, Object value ->
      if (!(value instanceof Number) || integralLong(value, "bloomFilterNDV['$path']") <= 0) {
        throw new IllegalArgumentException("bloomFilterNDV['$path'] must be a positive integer")
      }
    }

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
      throw new IllegalArgumentException(ERR_CODEC_NULL)
    }
    if (roundingMode == null) {
      throw new IllegalArgumentException(ERR_ROUNDING_MODE_NULL)
    }
  }

  Map<String, ?> toMap() {
    Map<String, Object> result = [(KEY_INFER_PRECISION_AND_SCALE): inferPrecisionAndScale,
                                  compressionCodec              : compressionCodec.name(),
                                  (KEY_ROUNDING_MODE)            : roundingMode.name()]
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
    result.timestampUnit = timestampUnit
    result.putAll([rowGroupSize: rowGroupSize, pageSize: pageSize, dictionaryEncoding: dictionaryEncoding, writerVersion: writerVersion, columnIndexTruncateLength: columnIndexTruncateLength, statisticsTruncateLength: statisticsTruncateLength, bloomFilterEnabled: bloomFilterEnabled, bloomFilterNDV: bloomFilterNDV, sizeStatisticsEnabled: sizeStatisticsEnabled])
    result
  }

  @SuppressWarnings('AbcMetric') // Explicit typed option normalization.
  static ParquetWriteOptions fromMap(Map<String, ?> options) {
    ParquetWriteOptions result = new ParquetWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    Set<String> known = descriptors()*.name*.toLowerCase(Locale.ROOT).toSet()
    Set<String> unknown = new HashSet<>(normalized.keySet())
    unknown.removeAll(known)
    if (!unknown.empty) { throw new IllegalArgumentException("Unknown Parquet options: $unknown") }
    normalized = normalized.findAll { String key, Object value -> value != null }
    if (normalized.containsKey('timestampunit')) { result.timestampUnit((String) normalized.timestampunit) }
    if (normalized.containsKey('inferprecisionandscale')) {
      Object inferPrecisionAndScale = normalized.inferprecisionandscale
      if (inferPrecisionAndScale != null) {
        result.inferPrecisionAndScale(OptionMaps.booleanValueOrNull(inferPrecisionAndScale, KEY_INFER_PRECISION_AND_SCALE))
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
    if (normalized.containsKey('roundingmode')) {
      Object value = normalized.roundingmode
      // Treat null as absent to preserve the default, consistent with the other option keys
      if (value instanceof RoundingMode) {
        result.roundingMode(value as RoundingMode)
      } else if (value != null) {
        result.roundingMode(String.valueOf(value))
      }
    }
    if (normalized.containsKey('rowgroupsize')) { result.rowGroupSize = ((Number) normalized.rowgroupsize).longValue() }
    if (normalized.containsKey('pagesize')) { result.pageSize = ((Number) normalized.pagesize).intValue() }
    if (normalized.containsKey('dictionaryencoding')) { result.dictionaryEncoding = OptionMaps.booleanValueOrNull(normalized.dictionaryencoding, KEY_DICTIONARY_ENCODING) }
    if (normalized.containsKey('writerversion')) { result.writerVersion = String.valueOf(normalized.writerversion) }
    if (normalized.containsKey('columnindextruncatelength')) { result.columnIndexTruncateLength = ((Number) normalized.columnindextruncatelength).intValue() }
    if (normalized.containsKey('statisticstruncatelength')) { result.statisticsTruncateLength = ((Number) normalized.statisticstruncatelength).intValue() }
    if (normalized.containsKey('bloomfilterenabled')) {
      result.bloomFilterEnabled = requireMap(normalized.bloomfilterenabled, KEY_BLOOM_ENABLED).collectEntries { key, value ->
        [(String.valueOf(key)): OptionMaps.booleanValueOrNull(value, "bloomFilterEnabled['$key']")]
      }
    }
    if (normalized.containsKey('bloomfilterndv')) {
      result.bloomFilterNDV = requireMap(normalized.bloomfilterndv, KEY_BLOOM_NDV).collectEntries { key, value ->
        [(String.valueOf(key)): integralLong(value, "bloomFilterNDV['$key']")]
      }
    }
    if (normalized.containsKey('sizestatisticsenabled')) { result.sizeStatisticsEnabled = OptionMaps.booleanValueOrNull(normalized.sizestatisticsenabled, KEY_SIZE_STATISTICS_ENABLED) }
    result.validate()
    result
  }

  private static Map requireMap(Object value, String name) {
    if (!(value instanceof Map)) { throw new IllegalArgumentException("$name must be a Map") }
    value
  }

  private static long integralLong(Object value, String name) {
    if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long ||
        value instanceof BigInteger || value instanceof BigDecimal || value instanceof CharSequence)) {
      throw new IllegalArgumentException("$name must be an integral number within long range")
    }
    try { new BigDecimal(value.toString().trim()).longValueExact() }
    catch (ArithmeticException | NumberFormatException failure) {
      throw new IllegalArgumentException("$name must be an integral number within long range", failure)
    }
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
    ParquetWriteOptions defaults = new ParquetWriteOptions()
    [
        new OptionDescriptor('timestampUnit', String, defaults.timestampUnit, 'MICROS truncates sub-microsecond digits; NANOS preserves precision within int64 range'),
        new OptionDescriptor(KEY_INFER_PRECISION_AND_SCALE, Boolean, Boolean.TRUE.toString(), 'Infer precision and scale for BigDecimal columns'),
        new OptionDescriptor(KEY_PRECISION, Integer, null, 'Uniform precision for all BigDecimal columns'),
        new OptionDescriptor(KEY_SCALE, Integer, null, 'Uniform scale for all BigDecimal columns'),
        new OptionDescriptor('decimalMeta', Map, null, 'Map of column names to [precision, scale] arrays'),
        new OptionDescriptor('zoneId', ZoneId, null, 'Time zone to use when writing timestamp values'),
        new OptionDescriptor('compressionCodec', CompressionCodecName, CompressionCodecName.SNAPPY.name(), 'Compression codec for the Parquet file (e.g. SNAPPY, GZIP, ZSTD, UNCOMPRESSED)'),
        new OptionDescriptor(KEY_ROUNDING_MODE, RoundingMode, RoundingMode.UNNECESSARY.name(), 'Rounding mode for explicit DECIMAL scale conversion'),
        new OptionDescriptor('rowGroupSize', Long, defaults.rowGroupSize, 'Target row-group bytes'),
        new OptionDescriptor('pageSize', Integer, defaults.pageSize, 'Target data-page bytes'),
        new OptionDescriptor(KEY_DICTIONARY_ENCODING, Boolean, defaults.dictionaryEncoding, 'Enable dictionary encoding'),
        new OptionDescriptor(KEY_WRITER_VERSION, String, defaults.writerVersion, 'V1 or V2 encodings and page layout'),
        new OptionDescriptor('columnIndexTruncateLength', Integer, defaults.columnIndexTruncateLength, 'Truncated binary page bounds bytes'),
        new OptionDescriptor('statisticsTruncateLength', Integer, defaults.statisticsTruncateLength, 'Truncated row-group binary statistics bytes'),
        new OptionDescriptor(KEY_BLOOM_ENABLED, Map, null, 'Per-column bloom-filter enabled overrides'),
        new OptionDescriptor(KEY_BLOOM_NDV, Map, null, 'Per-column positive expected distinct values'),
        new OptionDescriptor(KEY_SIZE_STATISTICS_ENABLED, Boolean, defaults.sizeStatisticsEnabled, 'Collect column size statistics')
    ]
  }
}
