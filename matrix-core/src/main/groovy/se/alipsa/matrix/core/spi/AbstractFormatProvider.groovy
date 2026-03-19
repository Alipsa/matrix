package se.alipsa.matrix.core.spi

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix

import java.nio.file.Path

/**
 * Base class for format providers with sensible defaults.
 *
 * <p>Provides default delegation between Path/File/URL/InputStream overloads
 * so that concrete providers only need to implement the methods they support.</p>
 *
 * <p>Default behaviour:</p>
 * <ul>
 *   <li>{@code read(Path)} delegates to {@code read(File)}</li>
 *   <li>{@code read(URL)} opens a stream and delegates to {@code read(InputStream)}</li>
 *   <li>{@code write(Path)} delegates to {@code write(File)}</li>
 *   <li>{@code read(InputStream)} and {@code write} throw {@link UnsupportedOperationException}</li>
 * </ul>
 *
 * @see MatrixFormatProvider
 */
@CompileStatic
abstract class AbstractFormatProvider implements MatrixFormatProvider {

  @Override
  Matrix read(Path path, Map<String, ?> options) {
    read(path.toFile(), options)
  }

  @Override
  Matrix read(URL url, Map<String, ?> options) {
    url.openStream().withCloseable { InputStream is ->
      read(is, options)
    }
  }

  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    throw new UnsupportedOperationException("${formatName()} does not support reading from InputStream")
  }

  @Override
  void write(Matrix matrix, Path path, Map<String, ?> options) {
    write(matrix, path.toFile(), options)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    []
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    []
  }

  /**
   * Extracts the file extension (lowercase, no dot) from a filename.
   *
   * @param fileName the filename to extract from
   * @return the extension, or empty string if none
   */
  protected static String extractExtension(String fileName) {
    FormatRegistry.extractExtension(fileName)
  }
}
