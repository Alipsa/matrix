package datasets

import static org.junit.jupiter.api.Assertions.*

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.datasets.Rdatasets

import java.net.http.HttpTimeoutException
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Deterministic tests of the private http helpers in Rdatasets against a local HttpServer.
 * Groovy's dynamic dispatch can invoke private static methods, so no public API is added for testability.
 */
class RdatasetsHttpTest {

  private static HttpServer server
  private static String base

  @BeforeAll
  static void startServer() {
    server = HttpServer.create(new InetSocketAddress('127.0.0.1', 0), 0)
    server.with {
      createContext('/latin1') { HttpExchange ex ->
        byte[] body = 'Zürich'.getBytes(StandardCharsets.ISO_8859_1)
        ex.responseHeaders.add('Content-Type', 'text/plain; charset=ISO-8859-1')
        ex.sendResponseHeaders(200, body.length)
        ex.responseBody.withCloseable { it.write(body) }
      }
      createContext('/utf8-noheader') { HttpExchange ex ->
        byte[] body = 'Zürich'.getBytes(StandardCharsets.UTF_8)
        ex.sendResponseHeaders(200, body.length)
        ex.responseBody.withCloseable { it.write(body) }
      }
      createContext('/missing') { HttpExchange ex ->
        ex.sendResponseHeaders(404, -1)
        ex.close()
      }
      createContext('/slow') { HttpExchange ex ->
        Thread.sleep(2000)
        ex.sendResponseHeaders(200, -1)
        ex.close()
      }
      createContext('/stall') { HttpExchange ex ->
        // send the 200 headers immediately, then stall before writing the body
        byte[] body = 'x'.getBytes(StandardCharsets.UTF_8)
        ex.sendResponseHeaders(200, body.length)
        Thread.sleep(2000)
        ex.responseBody.withCloseable { it.write(body) }
      }
      createContext('/data.csv') { HttpExchange ex ->
        byte[] body = '"name","value","note"\n"a",1,"x"\n#row,2,"y"\n"b",NA,""\n'.getBytes(StandardCharsets.UTF_8)
        ex.responseHeaders.add('Content-Type', 'text/csv; charset=UTF-8')
        ex.sendResponseHeaders(200, body.length)
        ex.responseBody.withCloseable { it.write(body) }
      }
      start()
      base = "http://127.0.0.1:${address.port}"
    }
  }

  @AfterAll
  static void stopServer() {
    server.stop(0)
  }

  @Test
  void testFetchTextDecodesResponseCharset() {
    assertEquals('Zürich', Rdatasets.fetchText("$base/latin1"))
  }

  @Test
  void testFetchTextDefaultsToUtf8WithoutContentType() {
    assertEquals('Zürich', Rdatasets.fetchText("$base/utf8-noheader"))
  }

  @Test
  void testFetchTextNon200ThrowsIOException() {
    def exception = assertThrows(IOException) { Rdatasets.fetchText("$base/missing") }
    assertEquals("HTTP 404 when fetching $base/missing", exception.message)
  }

  @Test
  void testFetchTextHeaderTimeout() {
    assertThrows(HttpTimeoutException) { Rdatasets.fetchText("$base/slow", Duration.ofMillis(200)) }
  }

  @Test
  void testFetchTextBodyTransferTimeout() {
    assertThrows(HttpTimeoutException) { Rdatasets.fetchText("$base/stall", Duration.ofMillis(200)) }
  }

  @Test
  void testFetchCsvParsesQuotedValuesNullsAndHashes() {
    Matrix m = Rdatasets.fetchCsv("$base/data.csv", 'sample')
    assertEquals('sample', m.matrixName)
    assertEquals(['name', 'value', 'note'], m.columnNames())
    assertEquals(3, m.rowCount(), 'a row starting with # must be retained, not dropped as a comment')
    assertEquals('#row', m[1, 'name'])
    assertEquals('2', m[1, 'value'])
    assertNull(m[2, 'value'], 'NA must map to null as with the previous parser')
    assertEquals('', m[2, 'note'], 'an empty quoted field stays an empty string, as with the previous parser')
  }
}
