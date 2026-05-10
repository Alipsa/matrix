package se.alipsa.matrix.datasets

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.jsoup.Jsoup

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

/**
 * Convenience wrapper for accessing datasets from the [R datasets repository](https://vincentarelbundock.github.io/Rdatasets/).
 * Rdatasets is a collection of 2536 datasets which were originally distributed alongside the
 * statistical software environment R and some of its add-on packages.
 */
@CompileStatic
class Rdatasets {

  private static final Logger log = Logger.getLogger(Rdatasets)

  private static final String COMMA = ','
  private static final String QUOTE = '"'
  private static final String NULL_OR_EMPTY_MSG = 'Package name and item name cannot be null or empty'
  private static final int EXPECTED_PARTS = 2

  private static volatile Matrix cachedOverview = null

  /**
   * Returns an overview of the datasets available in the R datasets repository.
   * The overview includes columns for Package, Item, Title, CSV URL, and html url.
   * Remote access can fail; on failure an exception wrapping the cause is thrown.
   *
   * @return a Matrix containing the overview of datasets
   * @throws UncheckedIOException if the remote data cannot be fetched
   */
  static Matrix overview() {
    Matrix result = cachedOverview
    if (result == null) {
      synchronized(Rdatasets) {
        result = cachedOverview
        if (result == null) {
          try {
            result = Matrix.builder()
                .data('https://raw.githubusercontent.com/vincentarelbundock/Rdatasets/master/datasets.csv', COMMA, QUOTE, true)
                .build()
            cachedOverview = result
          } catch (IOException e) {
            throw new UncheckedIOException("Failed to fetch Rdatasets overview: ${e.message}", e)
          }
        }
      }
    }
    result
  }

  /**
   * Clears the cached overview so the next call to {@link #overview()} re-fetches the data.
   */
  static void refresh() {
    cachedOverview = null
  }

  /**
   * Fetches the documentation for a specific dataset from the R datasets repository.
   * The documentation is returned as a String, and can be converted to plain text if desired.
   *
   * @param packageName the name of the package containing the dataset (column Package in the overview)
   * @param itemName the name of the dataset (column Item in the overview)
   * @param toPlainText if true, converts HTML content to plain text (default is false)
   * @return the documentation for the specified dataset
   */
  @CompileDynamic
  static String fetchInfo(String packageName, String itemName, boolean toPlainText = false) {
    if (packageName == null || packageName.isEmpty() || itemName == null || itemName.isEmpty()) {
      throw new IllegalArgumentException(NULL_OR_EMPTY_MSG)
    }
    log.debug("Fetching info for $packageName/$itemName (plainText=$toPlainText)")
    def urlResult = GQ {
      from d in overview()
      where d.Package == "$packageName" && d.Item == "$itemName"
      select d.Doc
    }
    def resultList = urlResult.toList()
    if (resultList.isEmpty()) {
      log.warn("Dataset not found: $packageName/$itemName")
      throw new IllegalArgumentException("Dataset not found: $packageName/$itemName")
    }
    String content = resultList[0].toURL().text
    if (toPlainText) {
      content = Jsoup.parse(content).wholeText()
    }
    log.debug("Successfully fetched info for $packageName/$itemName")
    content
  }

  /**
   * Fetches the data for a specific dataset from the R datasets repository.
   * The data is returned as a Matrix object, which can be used for further analysis.
   *
   * @param packageName the name of the package containing the dataset (column Package in the overview)
   * @param itemName the name of the dataset (column Item in the overview)
   * @return a Matrix containing the data for the specified dataset
   */
  @CompileDynamic
  static Matrix fetchData(String packageName, String itemName) {
    if (packageName == null || packageName.isEmpty() || itemName == null || itemName.isEmpty()) {
      throw new IllegalArgumentException(NULL_OR_EMPTY_MSG)
    }
    log.debug("Fetching data for $packageName/$itemName")
    def urlResult = GQ {
      from d in overview()
      where d.Package == "$packageName" && d.Item == "$itemName"
      select d.CSV
    }
    def resultList = urlResult.toList()
    if (resultList.isEmpty()) {
      log.warn("Dataset not found: $packageName/$itemName")
      throw new IllegalArgumentException("Dataset not found: $packageName/$itemName")
    }
    Matrix result = Matrix.builder()
        .data(resultList[0], COMMA, QUOTE, true)
        .build()
    log.debug("Successfully fetched $packageName/$itemName: ${result.rowCount()} rows, ${result.columnCount()} columns")
    result
  }

  /**
   * Fetches the data for a specific dataset from the R datasets repository.
   * The package/item string must contain exactly one forward slash.
   *
   * @param packageSlashItem the package and item name separated by a slash (e.g. {@code "datasets/iris"})
   * @return a Matrix containing the data for the specified dataset
   * @throws IllegalArgumentException if the input is null, blank, missing a slash, has more than one slash, or has blank parts
   */
  static Matrix fetchData(String packageSlashItem) {
    if (packageSlashItem == null || packageSlashItem.isBlank()) {
      throw new IllegalArgumentException('packageSlashItem cannot be null or blank')
    }
    def parts = packageSlashItem.split('/', -1)
    if (parts.length < EXPECTED_PARTS) {
      throw new IllegalArgumentException("packageSlashItem must contain a slash: '$packageSlashItem'")
    }
    if (parts.length > EXPECTED_PARTS) {
      throw new IllegalArgumentException("packageSlashItem must contain exactly one slash: '$packageSlashItem'")
    }
    String packageName = parts[0]
    String itemName = parts[1]
    if (packageName.isBlank() || itemName.isBlank()) {
      throw new IllegalArgumentException("package and item names cannot be blank: '$packageSlashItem'")
    }
    fetchData(packageName, itemName)
  }

  /**
   * Searches the Rdatasets overview for datasets whose Item or Title contains the given text (case-insensitive).
   *
   * @param text the text to search for
   * @return a Matrix containing matching rows, or an empty Matrix if no matches are found
   * @throws IllegalArgumentException if the search text is null or blank
   */
  @CompileDynamic
  static Matrix search(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException('search text cannot be null or blank')
    }
    def searchText = text.toLowerCase(Locale.ROOT)
    def result = GQ {
      from d in overview()
      where d.Item.toLowerCase(Locale.ROOT).contains(searchText) || d.Title.toLowerCase(Locale.ROOT).contains(searchText)
      select d
    }
    def rows = result.toList()
    if (rows.isEmpty()) {
      return Matrix.builder()
          .columnNames(overview().columnNames())
          .build()
    }
    Matrix.builder()
        .data(rows)
        .build()
  }

}
