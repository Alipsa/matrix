package se.alipsa.matrix.parquet

import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class HttpTransportTest {
  @Test
  void hubTokenIsOriginScopedAcrossRedirects() {
    List<StubConnection> requests = []
    URLStreamHandler handler = new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL url) {
        def connection = new StubConnection(url, requests.empty ? 302 : 200,
            requests.empty ? [Location: 'https://cdn.example/signed'] : [:])
        requests.add(connection)
        connection
      }
    }
    URL source = new URL(null, 'https://huggingface.co/datasets/owner/repo/resolve/main/file', handler)
    def options = new ParquetReadOptions()
    options.hubToken = 'test-secret'
    HttpTransport.open(source, options, [:]).disconnect()
    assertEquals('Bearer test-secret', requests[0].getRequestProperty('Authorization'))
    assertNull(requests[1].getRequestProperty('Authorization'))
    assertFalse(options.toMap().containsKey('hubToken'))
  }

  @Test
  void downgradeAndMalformedRangeFailAndDisconnect() {
    StubConnection request
    URL source = new URL(null, 'https://huggingface.co/file', new URLStreamHandler() {
      @Override protected URLConnection openConnection(URL url) {
        request = new StubConnection(url, 302, [Location: 'http://huggingface.co/file'])
        request
      }
    })
    assertThrows(IOException) { HttpTransport.open(source, new ParquetReadOptions(), [:]) }
    assertTrue(request.disconnected)
    URL malformed = new URL(null, 'https://example.test/file', new URLStreamHandler() {
      @Override protected URLConnection openConnection(URL url) {
        request = new StubConnection(url, 206, ['Content-Range': 'bytes 0-1/10'])
        request
      }
    })
    assertThrows(IOException) { new HttpRangeInputFile(malformed, new ParquetReadOptions()) }
    assertTrue(request.disconnected)
  }

  @Test
  void truncatedRangeBodyFailsAndDisconnects() {
    List<StubConnection> requests = []
    URL source = new URL(null, 'https://example.test/file', new URLStreamHandler() {
      @Override protected URLConnection openConnection(URL url) {
        boolean probe = requests.empty
        def connection = new StubConnection(url, 206, ['Content-Range': probe ? 'bytes 0-0/12' : 'bytes 0-3/12'])
        requests.add(connection)
        connection
      }
    })
    def input = new HttpRangeInputFile(source, new ParquetReadOptions(rangeCacheBytes: 4))
    assertThrows(EOFException) { input.newStream().withCloseable { it.readFully(new byte[4]) } }
    assertTrue(requests.last().disconnected)
    assertFalse(requests.first().disconnected) // Fully consumed success allows keep-alive reuse.
  }

  @Test
  void staticUrlOverloadsUseBoundedTransportAndHubParsing() {
    java.time.ZoneId zone = java.time.ZoneId.of('UTC')
    [ { URL url -> MatrixParquetReader.read(url) },
      { URL url -> MatrixParquetReader.read(url, zone) },
      { URL url -> MatrixParquetReader.read(url, 'named', zone) } ].each { read ->
      StubConnection request
      URL url = new URL(null, 'https://example.test/file', new URLStreamHandler() {
        @Override protected URLConnection openConnection(URL source) {
          request = new StubConnection(source, 302, [Location: 'http://example.test/file'])
          request
        }
      })
      assertThrows(IOException) { read(url) }
      assertEquals(10000, request.connectTimeout)
      assertEquals(60000, request.readTimeout)
      assertTrue(request.disconnected)
    }
    String invalid = 'hf://datasets/owner/repo/../file.parquet'
    assertThrows(IllegalArgumentException) { MatrixParquetReader.readUrl(invalid, zone) }
    assertThrows(IllegalArgumentException) { MatrixParquetReader.readUrl(invalid, 'named', zone) }
  }

  @Test
  void connectionRetriesAreBounded() {
    int attempts = 0
    URL source = new URL(null, 'https://example.test/file', new URLStreamHandler() {
      @Override protected URLConnection openConnection(URL url) {
        attempts++
        new StubConnection(url, 0, [:]) {
          @Override int getResponseCode() { throw new IOException('Transient connect failure') }
        }
      }
    })
    assertThrows(IOException) { HttpTransport.open(source, new ParquetReadOptions(), [:]) }
    assertEquals(2, attempts)
  }

  private static class StubConnection extends HttpURLConnection {
    private final int status
    private final Map<String, String> headers
    boolean disconnected
    StubConnection(URL url, int status, Map<String, String> headers) {
      super(url)
      this.status = status
      this.headers = headers
    }
    @Override int getResponseCode() { status }
    @Override String getHeaderField(String key) { headers[key] }
    @Override void disconnect() { disconnected = true }
    @Override boolean usingProxy() { false }
    @Override void connect() {}
    @Override InputStream getInputStream() { new ByteArrayInputStream([0] as byte[]) }
  }
}
