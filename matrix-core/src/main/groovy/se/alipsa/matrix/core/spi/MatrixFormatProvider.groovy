package se.alipsa.matrix.core.spi

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

/**
 * Service Provider Interface for file-format modules.
 *
 * <p>Implementations of this interface are discovered at runtime via
 * {@link java.util.ServiceLoader}. Each provider declares the file extensions
 * it handles and provides read/write operations for those formats.</p>
 *
 * <p>To register a provider, create a file at
 * {@code META-INF/services/se.alipsa.matrix.core.spi.MatrixFormatProvider}
 * containing the fully qualified class name of the implementation.</p>
 *
 * <p>Options are passed as {@code Map<String, ?>} to keep the SPI generic.
 * Each provider is responsible for converting map entries to its typed options.</p>
 *
 * @see AbstractFormatProvider
 * @see FormatRegistry
 */
@CompileStatic
interface MatrixFormatProvider {

  /**
   * Returns the set of file extensions this provider handles (lowercase, no dot).
   *
   * @return file extensions, e.g. {@code ['csv', 'tsv', 'tab'] as Set}
   */
  Set<String> supportedExtensions()

  /**
   * Returns a human-readable name for this format.
   *
   * @return format name, e.g. {@code "CSV (Apache Commons CSV)"}
   */
  String formatName()

  /**
   * Whether this provider supports reading.
   *
   * @return true if {@link #read} methods are implemented
   */
  boolean canRead()

  /**
   * Whether this provider supports writing.
   *
   * @return true if {@link #write} methods are implemented
   */
  boolean canWrite()

  /**
   * Read a Matrix from a File.
   *
   * @param file the file to read
   * @param options format-specific options (may be empty)
   * @return the parsed Matrix
   */
  Matrix read(File file, Map<String, ?> options)

  /**
   * Read a Matrix from a Path.
   *
   * @param path the path to read
   * @param options format-specific options (may be empty)
   * @return the parsed Matrix
   */
  Matrix read(Path path, Map<String, ?> options)

  /**
   * Read a Matrix from a URL.
   *
   * @param url the URL to read from
   * @param options format-specific options (may be empty)
   * @return the parsed Matrix
   */
  Matrix read(URL url, Map<String, ?> options)

  /**
   * Read a Matrix from an InputStream.
   *
   * @param is the input stream to read from
   * @param options format-specific options (may be empty)
   * @return the parsed Matrix
   */
  Matrix read(InputStream is, Map<String, ?> options)

  /**
   * Write a Matrix to a File.
   *
   * @param matrix the matrix to write
   * @param file the target file
   * @param options format-specific options (may be empty)
   */
  void write(Matrix matrix, File file, Map<String, ?> options)

  /**
   * Write a Matrix to a Path.
   *
   * @param matrix the matrix to write
   * @param path the target path
   * @param options format-specific options (may be empty)
   */
  void write(Matrix matrix, Path path, Map<String, ?> options)

  /**
   * Returns descriptors for all supported read options.
   *
   * @return list of read option descriptors
   */
  List<OptionDescriptor> readOptionDescriptors()

  /**
   * Returns descriptors for all supported write options.
   *
   * @return list of write option descriptors
   */
  List<OptionDescriptor> writeOptionDescriptors()

}
