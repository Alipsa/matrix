package se.alipsa.matrix.datasets

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.jsoup.Jsoup

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Convenience wrapper for accessing datasets from the [R datasets repository](https://vincentarelbundock.github.io/Rdatasets/).
 * Rdatasets is a collection of more than 3,600 datasets which were originally distributed alongside the
 * statistical software environment R and some of its add-on packages.
 * <p>
 * All remote access uses a 15 second connect timeout and a 120 second request timeout that bounds the entire
 * response, headers and body. Two kinds of remote failure
 * are reported, depending on which request failed:
 * <ul>
 *   <li>loading the dataset index — {@link UncheckedIOException} from {@link #overview()} and {@link #search}, and from
 *       {@link #fetchData} / {@link #fetchInfo} when they are the first call while the cache is cold;</li>
 *   <li>fetching the selected csv or documentation page ({@link #fetchData} / {@link #fetchInfo}) — {@link IOException}.</li>
 * </ul>
 */
@CompileStatic
class Rdatasets {

  private static final Logger log = Logger.getLogger(Rdatasets)

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15)
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120)
  private static final String OVERVIEW_URL = 'https://raw.githubusercontent.com/vincentarelbundock/Rdatasets/master/datasets.csv'
  private static final String OVERVIEW_NAME = 'datasets'
  private static final String COMMA = ','
  private static final String QUOTE = '"'
  private static final String NULL_OR_BLANK_MSG = 'Package name and item name cannot be null or blank'
  private static final int EXPECTED_PARTS = 2
  private static final int HTTP_OK = 200

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(CONNECT_TIMEOUT)
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()

  private static volatile Matrix cachedOverview = null

  /**
   * Returns an overview of the datasets available in the R datasets repository.
   * The overview includes columns for Package, Item, Title, CSV URL, and html url.
   * The index is fetched on first call and cached; each call returns an independent copy so callers may
   * freely convert, filter or rename columns without affecting later calls. Use {@link #refresh()} to re-fetch.
   *
   * @return a Matrix containing the overview of datasets
   * @throws UncheckedIOException if the remote data cannot be fetched
   */
  static Matrix overview() {
    cachedIndex().clone()
  }

  /**
   * Returns the cached overview matrix without copying.
   * Callers must not mutate the returned matrix; use {@link #overview()} for a mutable, independent copy.
   */
  private static Matrix cachedIndex() {
    Matrix result = cachedOverview
    if (result == null) {
      synchronized (Rdatasets) {
        result = cachedOverview
        if (result == null) {
          try {
            result = fetchCsv(OVERVIEW_URL, OVERVIEW_NAME)
          } catch (IOException e) {
            throw new UncheckedIOException("Failed to fetch Rdatasets overview: ${e.message}", e)
          } catch (IllegalArgumentException e) {
            throw new UncheckedIOException(
                "Rdatasets overview response could not be parsed: ${e.message}", new IOException(e))
          }
          cachedOverview = result
        }
      }
    }
    result
  }

  /** Clears the cached overview so the next call to {@link #overview()} re-fetches the data. */
  static void refresh() {
    cachedOverview = null
  }

  /**
   * Fetches the documentation for a specific dataset from the R datasets repository.
   *
   * @param packageName the name of the package containing the dataset
   * @param itemName the name of the dataset
   * @param toPlainText if true, converts HTML content to plain text (default is false)
   * @return the documentation for the specified dataset
   * @throws IllegalArgumentException if a name is null or blank, or the dataset does not exist
   * @throws UncheckedIOException if the dataset index cannot be fetched
   * @throws IOException if the documentation page cannot be fetched
   */
  @CompileDynamic
  static String fetchInfo(String packageName, String itemName, boolean toPlainText = false) {
    requireNames(packageName, itemName)
    log.debug("Fetching info for $packageName/$itemName (plainText=$toPlainText)")
    def urlResult = GQ {
      from d in cachedIndex()
      where d.Package == packageName && d.Item == itemName
      select d.Doc
    }
    def resultList = urlResult.toList()
    if (resultList.isEmpty()) {
      log.warn("Dataset not found: $packageName/$itemName")
      throw new IllegalArgumentException("Dataset not found: $packageName/$itemName")
    }
    String content = fetchText(resultList[0] as String)
    if (toPlainText) {
      content = Jsoup.parse(content).wholeText()
    }
    log.debug("Successfully fetched info for $packageName/$itemName")
    content
  }

  /**
   * Fetches the data for a specific dataset from the R datasets repository.
   *
   * @param packageName the name of the package containing the dataset
   * @param itemName the name of the dataset
   * @return a Matrix containing the data for the specified dataset
   * @throws IllegalArgumentException if a name is null or blank, or the dataset does not exist
   * @throws UncheckedIOException if the dataset index cannot be fetched
   * @throws IOException if the csv cannot be fetched
   */
  @CompileDynamic
  static Matrix fetchData(String packageName, String itemName) {
    requireNames(packageName, itemName)
    log.debug("Fetching data for $packageName/$itemName")
    def urlResult = GQ {
      from d in cachedIndex()
      where d.Package == packageName && d.Item == itemName
      select d.CSV
    }
    def resultList = urlResult.toList()
    if (resultList.isEmpty()) {
      log.warn("Dataset not found: $packageName/$itemName")
      throw new IllegalArgumentException("Dataset not found: $packageName/$itemName")
    }
    Matrix result = fetchCsv(resultList[0] as String, itemName)
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
      from d in cachedIndex()
      where d.Item.toLowerCase(Locale.ROOT).contains(searchText) || d.Title.toLowerCase(Locale.ROOT).contains(searchText)
      select d
    }
    Matrix.builder()
        .ginqResult(result)
        .build()
  }

  /** Validates that both names are present. */
  private static void requireNames(String packageName, String itemName) {
    if (packageName == null || packageName.isBlank() || itemName == null || itemName.isBlank()) {
      throw new IllegalArgumentException(NULL_OR_BLANK_MSG)
    }
  }

  /**
   * Downloads a csv document and parses it into a Matrix.
   *
   * @param url the csv location
   * @param name the matrixName to assign
   * @return the parsed matrix
   * @throws IOException on any transport error, timeout or non-200 response
   */
  private static Matrix fetchCsv(String url, String name) throws IOException {
    String csv = fetchText(url)
    Matrix.builder()
        .matrixName(name)
        .csvString(csv, [delimiter: COMMA, quoteString: QUOTE, lineComment: ''])
        .build()
  }

  /**
   * Performs a GET request with a connect timeout and a total timeout that bounds the entire response
   * (headers and body) and returns the body as text.
   * The body is decoded with the charset from the Content-Type header, falling back to UTF-8.
   *
   * @param url the location to fetch
   * @param timeout the total request timeout including body transfer, default {@link #REQUEST_TIMEOUT}
   * @return the response body
   * @throws IOException on transport errors, timeouts, interruption, a non-200 status or an empty body
   */
  private static String fetchText(String url, Duration timeout = REQUEST_TIMEOUT) throws IOException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .build()
    CompletableFuture<HttpResponse<String>> future = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    HttpResponse<String> response
    try {
      response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    } catch (TimeoutException e) {
      future.cancel(true)
      throw (HttpTimeoutException) new HttpTimeoutException("Request to $url timed out after ${timeout.toMillis()} ms").initCause(e)
    } catch (InterruptedException e) {
      future.cancel(true)
      Thread.currentThread().interrupt()
      throw new IOException("Interrupted while fetching $url", e)
    } catch (ExecutionException e) {
      Throwable cause = e.cause
      if (cause instanceof IOException) {
        throw (IOException) cause
      }
      throw new IOException("Failed to fetch $url: ${cause?.message}", cause)
    }
    if (response.statusCode() != HTTP_OK) {
      throw new IOException("HTTP ${response.statusCode()} when fetching $url")
    }
    String body = response.body()
    if (body == null || body.isBlank()) {
      throw new IOException("Empty response body from $url")
    }
    body
  }
}
