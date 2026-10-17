package se.alipsa.matrix.parquet

import groovy.transform.PackageScope

/** Shared bounded redirects, timeouts and origin-scoped Hub credentials. */
@PackageScope
final class HttpTransport {
  private static final List<String> PROTOCOLS = ['http', 'https']
  private static final String HTTPS = 'https'
  private static final List<Integer> HTTPS_PORTS = [-1, 443]

  private HttpTransport() {}

  static HttpURLConnection open(URL source, ParquetReadOptions options, Map<String, String> headers) {
    URL target = source
    int redirects = 0
    int transientFailures = 0
    while (true) {
      if (!PROTOCOLS.contains(target.protocol)) { throw new IOException('HTTP(S) URL required') }
      HttpURLConnection connection = (HttpURLConnection) target.openConnection()
      connection.instanceFollowRedirects = false
      connection.connectTimeout = options.connectTimeoutMillis
      connection.readTimeout = options.readTimeoutMillis
      connection.setRequestProperty('Accept-Encoding', 'identity')
      headers.each { String key, String value -> connection.setRequestProperty(key, value) }
      if (options.hubToken != null && target.protocol == HTTPS && target.host == 'huggingface.co' && HTTPS_PORTS.contains(target.port)) {
        connection.setRequestProperty('Authorization', "Bearer ${options.hubToken}")
      }
      int status
      try { status = connection.responseCode } catch (IOException failure) {
        connection.disconnect()
        if (++transientFailures >= 2) { throw failure }
        continue
      }
      transientFailures = 0
      if (!(status in [301, 302, 303, 307, 308])) { return connection }
      String location = connection.getHeaderField('Location')
      connection.disconnect()
      if (location == null || redirects++ >= 5) { throw new IOException('Invalid/excessive redirects') }
      URL next = new URL(target, location)
      if (target.protocol == HTTPS && next.protocol != HTTPS) { throw new IOException('Unsafe HTTP redirect') }
      target = next
    }
  }

  static InputStream download(URL source, ParquetReadOptions options) {
    HttpURLConnection connection = open(source, options, [:])
    int status = connection.responseCode
    if (status != 200) {
      connection.disconnect()
      throw new IOException("Download rejected: HTTP $status")
    }
    InputStream stream
    try { stream = connection.inputStream } catch (IOException failure) { connection.disconnect(); throw failure }
    new FilterInputStream(stream) {
      @Override
      void close() { try { super.close() } finally { connection.disconnect() } }
    }
  }
}
