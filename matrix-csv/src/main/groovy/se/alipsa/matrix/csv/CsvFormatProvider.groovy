package se.alipsa.matrix.csv

import groovy.transform.CompileStatic

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.util.Logger

/**
 * SPI format provider for CSV, TSV, and TAB-delimited files.
 *
 * <p>Discovered automatically via {@link java.util.ServiceLoader}. Delegates
 * to {@link CsvReader} for reading and {@link CsvWriter} for writing.</p>
 *
 * <p>For {@code .tsv} and {@code .tab} files, the delimiter is automatically
 * set to tab ({@code '\t'}) unless explicitly overridden in the options map.</p>
 *
 * @see CsvReader
 * @see CsvWriter
 * @see CsvReadOptions
 * @see CsvWriteOptions
 */
@CompileStatic
class CsvFormatProvider extends AbstractFormatProvider {

  private static final Logger log = Logger.getLogger(CsvFormatProvider)
  private static final String TSV_EXTENSION = 'tsv'
  private static final String TAB_EXTENSION = 'tab'

  private static final Set<String> EXTENSIONS = ['csv', TSV_EXTENSION, TAB_EXTENSION] as Set<String>

  @Override
  Set<String> supportedExtensions() {
    EXTENSIONS
  }

  @Override
  String formatName() {
    'CSV (Apache Commons CSV)'
  }

  @Override
  boolean canRead() {
    true
  }

  @Override
  boolean canWrite() {
    true
  }

  /**
   * Reads a Matrix from a CSV/TSV file.
   *
   * <p>Auto-detects TSV for {@code .tsv} and {@code .tab} extensions by
   * setting the delimiter to tab unless already specified in the options.</p>
   *
   * @param file the file to read
   * @param options format-specific options with string keys (case-insensitive)
   * @return the parsed Matrix
   */
  @Override
  Matrix read(File file, Map<String, ?> options) {
    log.debug("Reading CSV from file: ${file.absolutePath}")
    CsvReader.read(file, CsvReadOptions.fromMap(options))
  }

  /**
   * Reads a Matrix from a CSV/TSV URL.
   *
   * @param url the URL to read from
   * @param options format-specific options with string keys (case-insensitive)
   * @return the parsed Matrix
   */
  @Override
  Matrix read(URL url, Map<String, ?> options) {
    log.debug("Reading CSV from URL: ${url}")
    CsvReader.read(url, CsvReadOptions.fromMap(options))
  }

  /**
   * Reads a Matrix from a CSV InputStream.
   *
   * @param is the input stream to read from
   * @param options format-specific options with string keys (case-insensitive)
   * @return the parsed Matrix
   */
  @Override
  Matrix read(InputStream is, Map<String, ?> options) {
    log.debug('Reading CSV from InputStream')
    CsvReader.read(is, CsvReadOptions.fromMap(options))
  }

  /**
   * Writes a Matrix to a CSV/TSV file.
   *
   * <p>Builds a {@link CsvWriter.WriteBuilder} from the options map and
   * delegates to the fluent write API.</p>
   *
   * @param matrix the matrix to write
   * @param file the target file
   * @param options format-specific options with string keys
   */
  @Override
  void write(Matrix matrix, File file, Map<String, ?> options) {
    log.debug("Writing CSV to file: ${file.absolutePath}")
    CsvWriter.write(matrix, file, CsvWriteOptions.fromMap(options))
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    CsvReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    CsvWriteOptions.descriptors()
  }
}
