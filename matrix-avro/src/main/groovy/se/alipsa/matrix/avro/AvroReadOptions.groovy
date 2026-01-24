package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import org.apache.avro.Schema

/**
 * Configuration options for reading Avro data into a Matrix.
 *
 * <p>This class provides a fluent builder pattern for configuring Avro read operations.
 * All options have sensible defaults, so you can use an empty options object for standard behavior.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Use defaults
 * Matrix m = MatrixAvroReader.read(file, new AvroReadOptions())
 *
 * // With custom matrix name
 * def options = new AvroReadOptions()
 *     .matrixName("MyData")
 * Matrix m = MatrixAvroReader.read(file, options)
 *
 * // With schema evolution (reader schema)
 * def readerSchema = new Schema.Parser().parse(schemaJson)
 * def options = new AvroReadOptions()
 *     .readerSchema(readerSchema)
 *     .lenientTypeConversion(true)
 * Matrix m = MatrixAvroReader.read(file, options)
 * }</pre>
 *
 * @see MatrixAvroReader
 */
@CompileStatic
class AvroReadOptions {

  private String matrixName = null
  private Schema readerSchema = null
  private boolean lenientTypeConversion = false

  /**
   * Creates a new AvroReadOptions with default settings.
   */
  AvroReadOptions() {
  }

  /**
   * Sets the name for the resulting Matrix.
   *
   * <p>If not set, the name is derived from the source file name or defaults to "AvroMatrix".
   *
   * @param name the Matrix name
   * @return this options instance for method chaining
   */
  AvroReadOptions matrixName(String name) {
    this.matrixName = name
    return this
  }

  /**
   * Sets a reader schema for schema evolution.
   *
   * <p>When provided, Avro will use this schema to read the data, allowing for:
   * <ul>
   *   <li>Reading files with added fields (new fields get default values)</li>
   *   <li>Reading files with removed fields (old fields are ignored)</li>
   *   <li>Field type promotions (e.g., int to long)</li>
   * </ul>
   *
   * <p>The reader schema must be compatible with the writer schema in the file.
   *
   * @param schema the reader schema to use for schema evolution
   * @return this options instance for method chaining
   */
  AvroReadOptions readerSchema(Schema schema) {
    this.readerSchema = schema
    return this
  }

  /**
   * Sets whether to use lenient type conversion.
   *
   * <p>When true:
   * <ul>
   *   <li>Numeric type mismatches are converted (e.g., Long to Integer if in range)</li>
   *   <li>String values may be parsed to appropriate types</li>
   *   <li>Null values are handled gracefully for non-nullable types</li>
   * </ul>
   *
   * <p>When false (default), type mismatches may throw exceptions.
   *
   * @param lenient true for lenient conversion, false for strict
   * @return this options instance for method chaining
   */
  AvroReadOptions lenientTypeConversion(boolean lenient) {
    this.lenientTypeConversion = lenient
    return this
  }

  // Getters

  /**
   * @return the Matrix name, or null if not set
   */
  String getMatrixName() {
    return matrixName
  }

  /**
   * @return the reader schema, or null if not set
   */
  Schema getReaderSchema() {
    return readerSchema
  }

  /**
   * @return true if lenient type conversion is enabled
   */
  boolean isLenientTypeConversion() {
    return lenientTypeConversion
  }
}
