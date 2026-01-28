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

  static Matrix overView

  static {
    overView = Matrix.builder()
        .data('https://raw.githubusercontent.com/vincentarelbundock/Rdatasets/master/datasets.csv', ',', '"', true)
        .build()
  }

  /**
   * Returns an overview of the datasets available in the R datasets repository.
   * The overview includes columns for Package, Item, Title, CSV URL, and html url.
   *
   * @return a Matrix containing the overview of datasets
   */
  static Matrix overview() {
    overView
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
      throw new IllegalArgumentException("Package name and item name cannot be null or empty")
    }
    log.debug("Fetching info for $packageName/$itemName (plainText=$toPlainText)")
    def urlResult = GQ {
      from d in overView
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
      throw new IllegalArgumentException("Package name and item name cannot be null or empty")
    }
    log.debug("Fetching data for $packageName/$itemName")
    def urlResult = GQ {
      from d in overView
      where d.Package == "$packageName" && d.Item == "$itemName"
      select d.CSV
    }
    def resultList = urlResult.toList()
    if (resultList.isEmpty()) {
      log.warn("Dataset not found: $packageName/$itemName")
      throw new IllegalArgumentException("Dataset not found: $packageName/$itemName")
    }
    Matrix result = Matrix.builder()
        .data(resultList[0], ',', '"', true)
        .build()
    log.debug("Successfully fetched $packageName/$itemName: ${result.rowCount()} rows, ${result.columnCount()} columns")
    result
  }
}
