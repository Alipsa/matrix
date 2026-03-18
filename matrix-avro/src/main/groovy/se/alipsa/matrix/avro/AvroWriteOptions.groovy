package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import org.apache.avro.file.CodecFactory
import org.apache.avro.file.DataFileConstants
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

/**
 * Configuration options for writing Matrix data to Avro format.
 *
 * <p>This class provides a fluent builder pattern for configuring Avro write operations.
 * All options have sensible defaults, so you can use an empty options object for standard behavior.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Use defaults
 * MatrixAvroWriter.write(matrix, file, new AvroWriteOptions())
 *
 * // With compression
 * def options = new AvroWriteOptions()
 *     .compression(AvroWriteOptions.Compression.DEFLATE)
 *     .compressionLevel(6)
 * MatrixAvroWriter.write(matrix, file, options)
 *
 * // With all options
 * def options = new AvroWriteOptions()
 *     .inferPrecisionAndScale(true)
 *     .namespace("com.example.data")
 *     .schemaName("MyData")
 *     .compression(AvroWriteOptions.Compression.SNAPPY)
 *     .syncInterval(64000)
 * MatrixAvroWriter.write(matrix, file, options)
 * }</pre>
 *
 * @see MatrixAvroWriter
 */
@CompileStatic
class AvroWriteOptions {

  static final int DEFAULT_COMPRESSION_LEVEL = -1
  static final int DEFAULT_SYNC_INTERVAL = 0
  static final int MIN_SYNC_INTERVAL = 32
  static final int MAX_SYNC_INTERVAL = 1 << 30

  /**
   * Supported compression codecs for Avro files.
   */
  enum Compression {
    /** No compression (default) */
    NULL,
    /** Deflate compression (zlib) - good balance of speed and compression ratio */
    DEFLATE,
    /** Snappy compression - fast compression, moderate ratio (requires snappy library) */
    SNAPPY,
    /** Bzip2 compression - high compression ratio, slower (requires bzip2 library) */
    BZIP2,
    /** XZ compression - very high compression ratio, slowest (requires xz library) */
    XZ,
    /** Zstandard compression - excellent balance of speed and ratio (requires zstd library) */
    ZSTANDARD
  }

  private boolean inferPrecisionAndScale = false
  private String namespace = "se.alipsa.matrix.avro"
  private String schemaName = null
  private Compression compression = Compression.NULL
  private int compressionLevel = DEFAULT_COMPRESSION_LEVEL
  private int syncInterval = DEFAULT_SYNC_INTERVAL

  /**
   * Creates a new AvroWriteOptions with default settings.
   */
  AvroWriteOptions() {
  }

  /**
   * Sets whether to infer precision and scale for BigDecimal columns.
   *
   * <p>When true, scans all BigDecimal values in each column to determine the
   * maximum precision and scale needed. When false (default), BigDecimal columns
   * are stored as Avro doubles, which may lose precision.
   *
   * @param infer true to infer precision/scale, false to use double fallback
   * @return this options instance for method chaining
   */
  AvroWriteOptions inferPrecisionAndScale(boolean infer) {
    this.inferPrecisionAndScale = infer
    return this
  }

  /**
   * Sets the namespace for the generated Avro schema.
   *
   * <p>The namespace is used in the Avro schema's "namespace" field and helps
   * organize schemas when using schema registries.
   *
   * @param namespace the schema namespace (e.g., "com.example.data")
   * @return this options instance for method chaining
   */
  AvroWriteOptions namespace(String namespace) {
    this.namespace = namespace
    return this
  }

  /**
   * Sets the name for the generated Avro record schema.
   *
   * <p>This name appears in the Avro schema's "name" field.
   *
   * @param name the schema name (e.g., "UserData")
   * @return this options instance for method chaining
   */
  AvroWriteOptions schemaName(String name) {
    this.schemaName = name
    return this
  }

  /**
   * Sets the compression codec to use for the Avro file.
   *
   * <p>Note: Some codecs (SNAPPY, BZIP2, XZ, ZSTANDARD) require additional
   * libraries on the classpath. If the library is not available, writing
   * will fail with an appropriate error.
   *
   * @param compression the compression codec to use
   * @return this options instance for method chaining
   */
  AvroWriteOptions compression(Compression compression) {
    Compression effectiveCompression = compression ?: Compression.NULL
    validateCompressionLevel(effectiveCompression, compressionLevel)
    this.compression = effectiveCompression
    return this
  }

  /**
   * Sets the compression level for codecs that support it.
   *
   * <p>Only applies to DEFLATE (1-9), XZ (0-9), and ZSTANDARD (1-22).
   * Other codecs do not support this setting. Use -1 to use the codec's default level.
   * In fluent usage, call {@link #compression(Compression)} before this method so
   * codec-specific validation can run immediately.
   *
   * @param level the compression level, or -1 for codec default
   * @return this options instance for method chaining
   */
  AvroWriteOptions compressionLevel(int level) {
    validateCompressionLevel(compression, level)
    this.compressionLevel = level
    return this
  }

  /**
   * Sets the sync marker interval in bytes.
   *
   * <p>Sync markers allow readers to seek to arbitrary positions in the file.
   * Smaller intervals allow more precise seeking but increase file size.
   * Use 0 to use Avro's default (approximately 64KB).
   *
   * @param interval the sync interval in bytes, or 0 for default
   * @return this options instance for method chaining
   */
  AvroWriteOptions syncInterval(int interval) {
    validateSyncInterval(interval)
    this.syncInterval = interval
    return this
  }

  // Getters

  /**
   * @return true if precision and scale should be inferred for BigDecimal columns
   */
  boolean getInferPrecisionAndScale() {
    return inferPrecisionAndScale
  }

  /**
   * @return the schema namespace
   */
  String getNamespace() {
    return namespace
  }

  /**
   * @return the schema name
   */
  String getSchemaName() {
    return schemaName
  }

  /**
   * @return the compression codec
   */
  Compression getCompression() {
    return compression
  }

  /**
   * @return the compression level, or -1 for codec default
   */
  int getCompressionLevel() {
    return compressionLevel
  }

  /**
   * @return the sync interval in bytes, or 0 for default
   */
  int getSyncInterval() {
    return syncInterval
  }

  /**
   * Creates the Avro CodecFactory based on the configured compression settings.
   *
   * @return the CodecFactory for the configured compression
   */
  CodecFactory createCodecFactory() {
    switch (compression) {
      case Compression.DEFLATE ->
        compressionLevel >= 0 ? CodecFactory.deflateCodec(compressionLevel) : CodecFactory.deflateCodec(6)
      case Compression.SNAPPY ->
        CodecFactory.snappyCodec()
      case Compression.BZIP2 ->
        CodecFactory.bzip2Codec()
      case Compression.XZ ->
        compressionLevel >= 0 ? CodecFactory.xzCodec(compressionLevel) : CodecFactory.xzCodec(6)
      case Compression.ZSTANDARD ->
        compressionLevel >= 0 ? CodecFactory.zstandardCodec(compressionLevel) : CodecFactory.zstandardCodec(3)
      default ->
        CodecFactory.nullCodec()
    }
  }

  /**
   * Converts this options object to an SPI-friendly map.
   *
   * <p>{@code schemaName} is only included when explicitly set. When absent, the writer derives
   * the effective record name from {@code matrix.matrixName} and finally {@code MatrixSchema}
   * at write time.
   *
   * @return map representation of the configured options
   */
  Map<String, ?> toMap() {
    Map<String, Object> options = [
        inferPrecisionAndScale: inferPrecisionAndScale,
        namespace             : namespace,
        compression           : compression.name(),
        compressionLevel      : compressionLevel,
        syncInterval          : syncInterval
    ]
    if (schemaName != null) {
      options.schemaName = schemaName
    }
    options
  }

  /**
   * Creates {@link AvroWriteOptions} from a generic SPI options map.
   *
   * @param options the SPI options map
   * @return configured write options
   */
  static AvroWriteOptions fromMap(Map<String, ?> options) {
    AvroWriteOptions result = new AvroWriteOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    Compression compression = null
    Integer compressionLevel = null
    Integer syncInterval = null
    if (normalized.containsKey('inferprecisionandscale')) {
      Object inferPrecisionAndScale = normalized.inferprecisionandscale
      if (inferPrecisionAndScale != null) {
        result.inferPrecisionAndScale(inferPrecisionAndScale as boolean)
      }
    }
    if (normalized.containsKey('namespace')) {
      String namespace = OptionMaps.stringValueOrNull(normalized.namespace)
      if (namespace != null) {
        result.namespace(namespace)
      }
    }
    if (normalized.containsKey('schemaname')) {
      String schemaName = OptionMaps.stringValueOrNull(normalized.schemaname)
      if (schemaName != null) {
        result.schemaName(schemaName)
      }
    }
    if (normalized.containsKey('compression')) {
      def value = normalized.compression
      if (value instanceof Compression) {
        compression = value as Compression
      } else if (value != null) {
        compression = Compression.valueOf(String.valueOf(value).toUpperCase())
      }
    }
    if (normalized.containsKey('compressionlevel')) {
      Object value = normalized.compressionlevel
      if (value != null) {
        compressionLevel = (value as Number).intValue()
      }
    }
    if (normalized.containsKey('syncinterval')) {
      Object value = normalized.syncinterval
      if (value != null) {
        syncInterval = (value as Number).intValue()
      }
    }
    if (compression != null) {
      result.compression(compression)
    }
    if (compressionLevel != null) {
      result.compressionLevel(compressionLevel)
    }
    if (syncInterval != null) {
      result.syncInterval(syncInterval)
    }
    result
  }

  private static void validateCompressionLevel(Compression compression, int level) {
    Compression effectiveCompression = compression ?: Compression.NULL
    if (level == DEFAULT_COMPRESSION_LEVEL) {
      return
    }
    switch (effectiveCompression) {
      case Compression.DEFLATE -> {
        if (level < 1 || level > 9) {
          throw new IllegalArgumentException("DEFLATE compressionLevel must be -1 or between 1 and 9 but was $level")
        }
      }
      case Compression.XZ -> {
        if (level < 0 || level > 9) {
          throw new IllegalArgumentException("XZ compressionLevel must be -1 or between 0 and 9 but was $level")
        }
      }
      case Compression.ZSTANDARD -> {
        if (level < 1 || level > 22) {
          throw new IllegalArgumentException("ZSTANDARD compressionLevel must be -1 or between 1 and 22 but was $level")
        }
      }
      case Compression.SNAPPY, Compression.BZIP2, Compression.NULL -> {
        throw new IllegalArgumentException("${effectiveCompression.name()} compression does not support compressionLevel; use -1")
      }
    }
  }

  private static void validateSyncInterval(int interval) {
    if (interval == DEFAULT_SYNC_INTERVAL) {
      return
    }
    if (interval < MIN_SYNC_INTERVAL || interval > MAX_SYNC_INTERVAL) {
      throw new IllegalArgumentException(
          "syncInterval must be 0 for the Avro default (${DataFileConstants.DEFAULT_SYNC_INTERVAL}) or between ${MIN_SYNC_INTERVAL} and ${MAX_SYNC_INTERVAL} bytes but was $interval"
      )
    }
  }

  /**
   * Returns a human-readable description of all supported write options.
   *
   * @return formatted table of option descriptors
   */
  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  /**
   * Returns descriptors for all supported write options.
   *
   * @return write option descriptors
   */
  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('inferPrecisionAndScale', Boolean, 'false', 'Infer decimal precision and scale for BigDecimal columns'),
        new OptionDescriptor('namespace', String, 'se.alipsa.matrix.avro', 'Namespace for the generated Avro schema'),
        new OptionDescriptor('schemaName', String, 'matrix.matrixName or MatrixSchema', 'Record name override for the generated Avro schema'),
        new OptionDescriptor('compression', Compression, 'NULL', 'Compression codec to use when writing'),
        new OptionDescriptor('compressionLevel', Integer, '-1', 'Codec-specific compression level'),
        new OptionDescriptor('syncInterval', Integer, '0', 'Sync marker interval in bytes')
    ]
  }
}
