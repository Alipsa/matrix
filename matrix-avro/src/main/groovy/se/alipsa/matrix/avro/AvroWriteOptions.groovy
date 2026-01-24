package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import org.apache.avro.file.CodecFactory

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
  private String schemaName = "MatrixSchema"
  private Compression compression = Compression.NULL
  private int compressionLevel = -1  // -1 means use codec default
  private int syncInterval = 0  // 0 means use Avro default (approximately 64KB)

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
    this.compression = compression
    return this
  }

  /**
   * Sets the compression level for codecs that support it.
   *
   * <p>Only applies to DEFLATE (1-9), XZ (0-9), and ZSTANDARD (1-22).
   * Other codecs ignore this setting. Use -1 to use the codec's default level.
   *
   * @param level the compression level, or -1 for codec default
   * @return this options instance for method chaining
   */
  AvroWriteOptions compressionLevel(int level) {
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
}
