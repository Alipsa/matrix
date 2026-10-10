package se.alipsa.matrix.gsheets.preflight

import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.gsheets.GsAuthenticator

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Noninteractive release authentication and network preflight; never creates spreadsheets. */
class AuthPreflight {
  private static final Duration TIMEOUT = Duration.ofSeconds(15)
  private static final Logger log = Logger.getLogger(AuthPreflight)

  /**
   * Checks enabled Sheets tests' shared scope union and both service endpoints.
   * @param args optional {@code --check-classpath} verifies dependencies without authentication or probes
   */
  static void main(String[] args) {
    if (args.toList() == ['--check-classpath']) {
      [groovy.json.JsonSlurper, GsAuthenticator, Logger].each { Class<?> dependency ->
        dependency.name
      }
      ExternalAuthRequirements.scopes()
      log.info('Authentication preflight runtime classpath resolved')
      return
    }
    if (args.length > 0) {
      throw new IllegalArgumentException('Supported argument: --check-classpath (no authentication)')
    }
    try {
      GsAuthenticator.authenticate(ExternalAuthRequirements.scopes())
    } catch (Exception ignored) {
      throw new IllegalStateException('Sheets external tests require noninteractive ADC with Sheets and Drive-file scopes; see docs/releaseAll.md')
    }
    HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
    ['https://sheets.googleapis.com/', 'https://www.googleapis.com/drive/v3/files'].each { String endpoint ->
      try {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(TIMEOUT).GET().build()
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() >= 500) {
          throw new IOException('Service unavailable')
        }
      } catch (Exception ignored) {
        throw new IllegalStateException("External test endpoint is unavailable: ${endpoint}")
      }
    }
    log.info('Sheets external test authentication and network preflight passed')
  }
}
