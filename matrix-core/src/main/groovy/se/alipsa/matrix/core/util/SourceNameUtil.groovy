package se.alipsa.matrix.core.util

/**
 * Derives Matrix names from file and URL sources.
 */
final class SourceNameUtil {

  private static final String PATH_SEPARATOR = '/'
  private static final String EXTENSION_SEPARATOR = '.'

  private SourceNameUtil() {
  }

  /**
   * Removes the final extension from a file name.
   *
   * <p>For example, {@code archive.data.csv} becomes {@code archive.data}; a leading-dot name
   * such as {@code .hidden} remains unchanged.</p>
   *
   * @param fileName the file name to process
   * @return the file name without its final extension
   */
  static String stripExtension(String fileName) {
    int dot = fileName.lastIndexOf(EXTENSION_SEPARATOR)
    dot > 0 ? fileName.substring(0, dot) : fileName
  }

  /**
   * Derives a Matrix name from a file.
   *
   * <p>For example, {@code new File('sales.csv')} produces {@code sales}.</p>
   *
   * @param file the source file
   * @return the file name without its final extension
   */
  static String matrixName(File file) {
    stripExtension(file.name)
  }

  /**
   * Derives a Matrix name from a URL path, excluding query and fragment components.
   *
   * <p>For example, {@code https://example.test/a/sales%20report.csv?v=1} produces
   * {@code sales report}.</p>
   *
   * @param url the source URL
   * @return the decoded final path segment without its final extension, or an empty string
   */
  static String matrixName(URL url) {
    String path = null
    try {
      path = url.toURI().path
    } catch (URISyntaxException ignored) {
      // Fall through to URL accessors, which also work for non-RFC URL strings.
    }
    if (!path) {
      path = url.file ?: url.path ?: ''
      int query = path.indexOf('?')
      int fragment = path.indexOf('#')
      int cut = query < 0 ? fragment : (fragment < 0 ? query : Math.min(query, fragment))
      if (cut >= 0) {
        path = path.substring(0, cut)
      }
    }
    int slash = path.lastIndexOf(PATH_SEPARATOR)
    String segment = slash >= 0 ? path.substring(slash + 1) : path
    stripExtension(segment)
  }
}
