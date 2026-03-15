package se.alipsa.matrix.csv

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.spi.AbstractFormatProvider
import se.alipsa.matrix.core.spi.OptionDescriptor
import se.alipsa.matrix.core.util.Logger

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

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

  private static final Set<String> EXTENSIONS = ['csv', 'tsv', 'tab'] as Set<String>
  private static final Set<String> TSV_EXTENSIONS = ['tsv', 'tab'] as Set<String>

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
    Map<String, ?> readOptions = applyTsvDefaults(file.name, options)
    log.debug("Reading CSV from file: ${file.absolutePath}")
    CsvReader.read(readOptions as Map, file)
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
    Map<String, ?> readOptions = applyTsvDefaults(url.path, options)
    log.debug("Reading CSV from URL: ${url}")
    CsvReader.read(readOptions as Map, url)
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
    Map<String, ?> readOptions = copyOptions(options)
    log.debug("Reading CSV from InputStream")
    CsvReader.read(readOptions as Map, is)
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
    CsvWriter.WriteBuilder builder = CsvWriter.write(matrix)
    Map<String, Object> normalizedOptions = normalizeOptions(options)

    // Apply TSV default for .tsv/.tab extensions
    String ext = extractExtension(file.name)
    boolean delimiterSet = normalizedOptions.containsKey('delimiter')

    if (delimiterSet) {
      def delimiterValue = normalizedOptions.get('delimiter')
      if (delimiterValue instanceof Character) {
        builder.delimiter(delimiterValue as Character)
      } else {
        builder.delimiter(delimiterValue as String)
      }
    }

    if (!delimiterSet && TSV_EXTENSIONS.contains(ext)) {
      builder.delimiter('\t' as char)
    }

    if (normalizedOptions.containsKey('quote')) {
      def quoteValue = normalizedOptions.get('quote')
      if (quoteValue instanceof Character) {
        builder.quoteCharacter(quoteValue as Character)
      } else {
        builder.quoteCharacter(quoteValue as String)
      }
    }

    if (normalizedOptions.containsKey('withheader')) {
      builder.withHeader(normalizedOptions.get('withheader') as boolean)
    }

    if (normalizedOptions.containsKey('recordseparator')) {
      builder.recordSeparator(normalizedOptions.get('recordseparator') as String)
    }

    Charset charset = StandardCharsets.UTF_8
    if (normalizedOptions.containsKey('charset')) {
      def charsetValue = normalizedOptions.get('charset')
      if (charsetValue instanceof Charset) {
        charset = CsvOptionUtil.resolveCharset(charsetValue as Charset)
      } else if (charsetValue instanceof CharSequence) {
        charset = CsvOptionUtil.resolveCharset(charsetValue as CharSequence)
      } else {
        throw new IllegalArgumentException("Charset must be a java.nio.charset.Charset or CharSequence but was ${charsetValue?.class} = $charsetValue")
      }
    }
    builder.to(file, charset)
  }

  @Override
  List<OptionDescriptor> readOptionDescriptors() {
    CsvReadOptions.descriptors()
  }

  @Override
  List<OptionDescriptor> writeOptionDescriptors() {
    CsvWriteOptions.descriptors()
  }

  /**
   * Applies TSV defaults when the file extension is {@code .tsv} or {@code .tab}
   * and no delimiter is specified in the options map.
   *
   * @param fileName the file name or path to check extension
   * @param options the original options map
   * @return a new map with TSV defaults applied if needed
   */
  private static Map<String, ?> applyTsvDefaults(String fileName, Map<String, ?> options) {
    Map<String, Object> result = copyOptions(options)
    String ext = extractExtension(fileName)
    if (TSV_EXTENSIONS.contains(ext) && !hasDelimiterKey(result)) {
      result.put('Delimiter', '\t')
    }
    result
  }

  /**
   * Creates a mutable copy of an options map.
   *
   * @param options the original options map
   * @return a new LinkedHashMap with the same entries
   */
  private static Map<String, Object> copyOptions(Map<String, ?> options) {
    Map<String, Object> result = [:]
    if (options == null) {
      return result
    }
    options.each { k, v -> result.put(String.valueOf(k), v) }
    result
  }

  /**
   * Checks whether the options map contains a delimiter key (case-insensitive).
   *
   * @param options the options map to check
   * @return true if a delimiter key is present
   */
  private static boolean hasDelimiterKey(Map<String, ?> options) {
    options.keySet().any { String key -> key.equalsIgnoreCase('delimiter') }
  }

  private static Map<String, Object> normalizeOptions(Map<String, ?> options) {
    Map<String, Object> normalized = [:]
    if (options == null) {
      return normalized
    }
    options.each { k, v ->
      normalized.put(String.valueOf(k).toLowerCase(Locale.ROOT), v)
    }
    normalized
  }
}
