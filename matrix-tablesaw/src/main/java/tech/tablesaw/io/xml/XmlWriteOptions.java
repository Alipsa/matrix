package tech.tablesaw.io.xml;

import tech.tablesaw.io.Destination;
import tech.tablesaw.io.WriteOptions;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;

/**
 * Write options for XML format.
 *
 * <p>This class provides configuration options for writing Tablesaw tables to XML files.
 * Currently extends {@link WriteOptions} without adding XML-specific options, but provides
 * convenience builders for common destination types.
 *
 * <p>Example usage:
 * <pre>{@code
 * Table table = ...;
 * XmlWriteOptions options = XmlWriteOptions.builder("output.xml").build();
 * new XmlWriter().write(table, options);
 * }</pre>
 *
 * @see XmlWriter
 * @see WriteOptions
 */
public class XmlWriteOptions extends WriteOptions {

  /**
   * Creates a builder with the specified destination.
   *
   * @param dest the destination to write to
   * @return a new builder
   */
  public static Builder builder(Destination dest) {
    return new Builder(dest);
  }

  /**
   * Creates a builder with an OutputStream destination.
   *
   * @param dest the output stream to write to
   * @return a new builder
   */
  public static Builder builder(OutputStream dest) {
    return new Builder(dest);
  }

  /**
   * Creates a builder with a Writer destination.
   *
   * @param dest the writer to write to
   * @return a new builder
   */
  public static Builder builder(Writer dest) {
    return new Builder(dest);
  }

  /**
   * Creates a builder with a File destination.
   *
   * @param dest the file to write to
   * @return a new builder
   * <p>Note: this method does not access the filesystem; any I/O errors occur when writing.
   */
  public static Builder builder(File dest) {
    return new Builder(dest);
  }

  /**
   * Creates a builder with a file name destination.
   *
   * @param fileName the name of the file to write to
   * @return a new builder
   * <p>Note: this method does not access the filesystem; any I/O errors occur when writing.
   */
  public static Builder builder(String fileName) {
    return builder(new File(fileName));
  }

  /**
   * Constructs write options from a builder.
   *
   * @param builder the builder containing the configuration
   */
  protected XmlWriteOptions(Builder builder) {
    super(builder);
  }

  /**
   * Builder for {@link XmlWriteOptions}.
   *
   * <p>Provides a fluent API for configuring XML write options.
   */
  public static class Builder extends WriteOptions.Builder {

    /**
     * Constructs a builder with the specified destination.
     *
     * @param dest the destination to write to
     */
    protected Builder(Destination dest) {
      super(dest);
    }

    /**
     * Constructs a builder with an OutputStream destination.
     *
     * @param dest the output stream to write to
     */
    protected Builder(OutputStream dest) {
      super(dest);
    }

    /**
     * Constructs a builder with a Writer destination.
     *
     * @param dest the writer to write to
     */
    protected Builder(Writer dest) {
      super(dest);
    }

    /**
     * Constructs a builder with a File destination.
     *
     * @param dest the file to write to
     */
    protected Builder(File dest) {
      super(dest);
    }

    /**
     * Builds the XML write options.
     *
     * @return the configured write options
     */
    public XmlWriteOptions build() {
      return new XmlWriteOptions(this);
    }
  }
}
