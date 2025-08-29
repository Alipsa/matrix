package se.alipsa.matrix.datasets

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.jsoup.Jsoup
import se.alipsa.matrix.core.Matrix

/**
 * Convenience wrapper for accessing datasets from the [R datasets repository](https://vincentarelbundock.github.io/Rdatasets/).
 * Rdatasets is a collection of 2536 datasets which were originally distributed alongside the
 * statistical software environment R and some of its add-on packages.
 */
@CompileStatic
class Rdatasets {

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
    if (packageName == null || itemName.isEmpty()) {
      throw new IllegalArgumentException("Dataset name cannot be null or empty")
    }
    def urlResult = GQ {
      from d in overView
      where d.Package == "$packageName" && d.Item == "$itemName"
      select d.Doc
    }
    String content = urlResult.toList()[0].toURL().text
    if (toPlainText) {
      content = Jsoup.parse(content).wholeText()
    }
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
    if (packageName == null || itemName.isEmpty()) {
      throw new IllegalArgumentException("Dataset name cannot be null or empty")
    }
    def urlResult = GQ {
      from d in overView
      where d.Package == "$packageName" && d.Item == "$itemName"
      select d.CSV
    }
    Matrix.builder()
        .data(urlResult.toList()[0], ',', '"', true)
        .build()
  }
}
