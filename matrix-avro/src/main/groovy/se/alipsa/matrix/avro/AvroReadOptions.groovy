package se.alipsa.matrix.avro

import groovy.transform.CompileStatic
import org.apache.avro.Schema
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.spi.OptionMaps

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
 * // With custom matrix name override
 * def options = new AvroReadOptions()
 *     .matrixName("MyData")
 * Matrix m = MatrixAvroReader.read(file, options)
 *
 * // With schema evolution (reader schema)
 * def readerSchema = new Schema.Parser().parse(schemaJson)
 * def options = new AvroReadOptions()
 *     .readerSchema(readerSchema)
 * Matrix m = MatrixAvroReader.read(file, options)
 * }</pre>
 *
 * @see MatrixAvroReader
 */
@CompileStatic
class AvroReadOptions {

  private String matrixName = null
  private Schema readerSchema = null

  /**
   * Creates a new AvroReadOptions with default settings.
   */
  AvroReadOptions() {
  }

  /**
   * Sets the name for the resulting Matrix.
   *
   * <p>If not set, the reader falls back to the Avro record name from the file schema and then
   * to a source-derived fallback such as the file name or "AvroMatrix".
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
   * Converts this options object to an SPI-friendly map.
   *
   * @return map representation of the configured options
   */
  Map<String, ?> toMap() {
    Map<String, Object> options = [:]
    if (matrixName != null) {
      options.matrixName = matrixName
    }
    if (readerSchema != null) {
      options.readerSchema = readerSchema
    }
    options
  }

  /**
   * Creates {@link AvroReadOptions} from a generic SPI options map.
   *
   * @param options the SPI options map
   * @return configured read options
   */
  static AvroReadOptions fromMap(Map<String, ?> options) {
    AvroReadOptions result = new AvroReadOptions()
    Map<String, Object> normalized = OptionMaps.normalizeKeys(options)
    if (normalized.containsKey('matrixname')) {
      String matrixName = OptionMaps.stringValueOrNull(normalized.matrixname)
      if (matrixName != null) {
        result.matrixName(matrixName)
      }
    }
    if (normalized.containsKey('readerschema')) {
      def value = normalized.readerschema
      if (value instanceof Schema) {
        result.readerSchema(value as Schema)
      } else if (value instanceof CharSequence) {
        result.readerSchema(new Schema.Parser().parse(String.valueOf(value)))
      } else {
        throw new IllegalArgumentException("readerSchema must be a Schema or schema JSON string but was ${value?.class}")
      }
    }
    result
  }

  /**
   * Returns a human-readable description of all supported read options.
   *
   * @return formatted table of option descriptors
   */
  static String describe() {
    OptionDescriptor.describe(descriptors())
  }

  /**
   * Returns descriptors for all supported read options.
   *
   * @return read option descriptors
   */
  static List<OptionDescriptor> descriptors() {
    [
        new OptionDescriptor('matrixName', String, null, 'Override name for the resulting Matrix'),
        new OptionDescriptor('readerSchema', Schema, null, 'Reader schema or schema JSON for schema evolution')
    ]
  }
}
