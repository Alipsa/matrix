package se.alipsa.matrix.gsheets

import static se.alipsa.matrix.gsheets.GsAuthenticator.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

import se.alipsa.matrix.core.util.Logger

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.AclEntry
import java.nio.file.attribute.AclEntryPermission
import java.nio.file.attribute.AclEntryType
import java.nio.file.attribute.AclFileAttributeView
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/**
 * OAuth2 / ADC token utilities for Google Sheets authentication.
 */
class GsAuthUtils {

  private static final Logger log = Logger.getLogger(GsAuthUtils)

  // The OAuth 2.0 "Desktop app" client registered for matrix-gsheets itself, bundled with
  // the library so callers get a working interactive login out of the box instead of having
  // to register their own client. Per Google's OAuth docs, installed-app client secrets are
  // not confidential: https://developers.google.com/identity/protocols/oauth2/native-app
  private static final String BUNDLED_CLIENT_SECRET_RESOURCE = '/se/alipsa/matrix/gsheets/oauth/client_secret.json'
  private static final String UTF8 = 'UTF-8'
  private static final String ADC_TEMP_PREFIX = 'adc-'
  private static final String ADC_TEMP_SUFFIX = '.tmp'
  private static final String POSIX_VIEW = 'posix'
  private static final String ACL_VIEW = 'acl'
  private static final String OWNER_ONLY_PERMISSIONS = 'rw-------'
  private static final int SCOPE_CACHE_SIZE = 8
  private static final Object SCOPE_CACHE_LOCK = new Object()
  private static final String TOKENINFO_REJECTED = 'tokeninfo request was rejected'
  private static final int TOKENINFO_TIMEOUT_MS = 10_000
  // Values are FutureTasks so the tokeninfo lookup itself runs outside SCOPE_CACHE_LOCK:
  // the lock only guards cache get/put, and FutureTask.run() guarantees the resolver is
  // invoked at most once per cached token no matter how many threads arrive concurrently.
  private static final Map<String, FutureTask<Set<String>>> SCOPE_CACHE =
      new LinkedHashMap<String, FutureTask<Set<String>>>(SCOPE_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, FutureTask<Set<String>>> eldest) {
          this.size() > GsAuthUtils.SCOPE_CACHE_SIZE
        }
      }

  @FunctionalInterface
  interface ScopeResolver {
    Set<String> grantedScopes(String accessToken) throws IOException
  }

  /**
   * Drop-in replacement for {@code gcloud auth application-default login}.
   * Writes {@code ~/.config/gcloud/application_default_credentials.json}.
   *
   * <p>Uses matrix-gsheets' own bundled OAuth client by default. To use a different
   * client instead (e.g. your own registered one), save it to
   * {@link GsAuthenticator#CLIENT_SECRET_FILE} (~/client_secret_desktop.json) or call
   * {@link #loginAndWriteAdc(File, List, String)} directly.
   *
   * <p><strong>Usage:</strong>
   * <pre>{@code
   * def creds = loginAndWriteAdc(
   *     [com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS,
   *      'https://www.googleapis.com/auth/drive.file',
   *      'openid', 'email'],
   *     "$PROJECT_ID" // optional; mainly needed if you still call Google OAuth2 userinfo via googleapis.com
   * )
   * }</pre>
   */
  static GoogleCredentials loginAndWriteAdc(List<String> scopes, String quotaProjectId = null) {
    InputStream secretStream = resolveClientSecretStream()
    try {
      loginAndWriteAdc(new InputStreamReader(secretStream, UTF8), scopes, quotaProjectId)
    } finally {
      secretStream.close()
    }
  }

  /**
   * Same as {@link #loginAndWriteAdc(List, String)} but with an explicit OAuth client
   * secret file, bypassing the bundled client and any {@code CLIENT_SECRET_FILE} override.
   */
  static GoogleCredentials loginAndWriteAdc(File clientSecretJson, List<String> scopes, String quotaProjectId = null) {
    if (!clientSecretJson.exists()) {
      throw new IllegalStateException("Error: OAuth client secret file was not found at $clientSecretJson.absolutePath")
    }
    clientSecretJson.withReader(UTF8) { Reader reader ->
      loginAndWriteAdc(reader, scopes, quotaProjectId)
    }
  }

  private static InputStream resolveClientSecretStream() {
    File override = GsAuthenticator.CLIENT_SECRET_FILE
    if (override.exists()) {
      log.info("Using OAuth client override at ${override.absolutePath}")
      return new FileInputStream(override)
    }
    InputStream bundled = GsAuthUtils.getResourceAsStream(BUNDLED_CLIENT_SECRET_RESOURCE)
    if (bundled == null) {
      throw new IllegalStateException(
          'No OAuth client available: matrix-gsheets was built without its bundled OAuth ' +
          "client resource ($BUNDLED_CLIENT_SECRET_RESOURCE), and no override was found at " +
          "$override.absolutePath. Provide your own client, or rebuild matrix-gsheets with " +
          'the bundled resource present.'
      )
    }
    bundled
  }

  private static GoogleCredentials loginAndWriteAdc(Reader clientSecretReader, List<String> scopes, String quotaProjectId) {
    def http = GoogleNetHttpTransport.newTrustedTransport()
    def json = GsonFactory.getDefaultInstance()
    GoogleClientSecrets secrets = GoogleClientSecrets.load(json, clientSecretReader)

    def flow = new GoogleAuthorizationCodeFlow.Builder(http, json, secrets, scopes)
        .setAccessType('offline')
        .setApprovalPrompt('force')
        .build()

    def receiver = new LocalServerReceiver.Builder().setPort(0).build()
    Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize('user')

    String refreshToken = cred.getRefreshToken()
    if (!refreshToken) {
      throw new IllegalStateException('No refresh token received. Remove old consent/tokens and try again.')
    }

    // Build ADC JSON
    def adc = [
        type           : 'authorized_user',
        client_id      : secrets.details.clientId,
        client_secret  : secrets.details.clientSecret,
        refresh_token  : refreshToken
    ]
    if (quotaProjectId) {
      adc.quota_project_id = quotaProjectId
    }

    // Persist to the standard ADC path
    File adcFile = GsAuthenticator.ADC_FILE_PATH
    writeAdcFile(adcFile, JsonOutput.prettyPrint(JsonOutput.toJson(adc)))
    log.info("Wrote ADC to ${adcFile.absolutePath}")

    // Return a ready-to-use GoogleCredentials as well
    GoogleCredentials gc = GoogleCredentials.fromStream(new ByteArrayInputStream(adcFile.bytes))
    if (scopes) {
      gc = gc.createScoped(scopes)
    }
    if (quotaProjectId) {
      gc = gc.createWithQuotaProject(quotaProjectId)
    }
    gc.refresh()
    return gc
  }

  static HttpRequestInitializer noUserProjectInitializer(GoogleCredentials creds) {
    def base = new HttpCredentialsAdapter(creds)
    return new HttpRequestInitializer() {
      @Override
      void initialize(HttpRequest req) throws IOException {
        base.initialize(req)
        // Strip quota project header for userinfo to avoid USER_PROJECT_DENIED
        req.getHeaders().set('X-Goog-User-Project', (Object) null)
        req.getHeaders().set('x-goog-user-project', (Object) null)
      }
    }
  }

  static boolean hasAllScopes(GoogleCredentials creds, List<String> required) {
    hasAllScopes(creds, required, GsAuthUtils.&fetchGrantedScopes as ScopeResolver)
  }

  /**
   * Checks whether a token grants every requested scope, resolving and caching its tokeninfo
   * scopes only once per access-token value. The tokeninfo network call runs outside the
   * cache lock so a slow or hung request cannot block scope checks for unrelated tokens.
   */
  @CompileDynamic
  static boolean hasAllScopes(GoogleCredentials creds, List<String> required, ScopeResolver resolver) {
    if (creds == null) {
      return false
    }
    try {
      creds.refreshIfExpired()
    } catch (IOException e) {
      log.debug("Could not refresh credentials while checking scopes: ${e.message}", e)
      return false
    }
    def token = creds.accessToken?.tokenValue
    if (!token) {
      return false
    }
    FutureTask<Set<String>> resolution
    synchronized (SCOPE_CACHE_LOCK) {
      resolution = SCOPE_CACHE.get(token)
      if (resolution == null) {
        resolution = new FutureTask<Set<String>>({
          // a custom resolver returning null means "no scopes granted", not "crash on NPE"
          resolver.grantedScopes(token) ?: ([] as Set<String>)
        } as Callable<Set<String>>)
        SCOPE_CACHE.put(token, resolution)
      }
    }
    // FutureTask.run() executes the callable at most once, so concurrent callers share the
    // single in-flight tokeninfo request; runners that lose the race return immediately.
    resolution.run()
    Set<String> granted
    try {
      granted = resolution.get()
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt()
      throw e
    } catch (ExecutionException e) {
      // Do not cache failures; a later call for the same token should be able to retry.
      synchronized (SCOPE_CACHE_LOCK) {
        if (SCOPE_CACHE.get(token)?.is(resolution)) {
          SCOPE_CACHE.remove(token)
        }
      }
      Throwable cause = e.cause
      if (cause instanceof IOException) {
        if (cause.message?.startsWith(TOKENINFO_REJECTED)) {
          log.warn("OAuth scope verification rejected the access token (${cause.message}); " +
              'it is invalid or revoked. Treating credentials as unverified and attempting login.')
        } else {
          log.warn("OAuth scope verification was unavailable (${cause.message}); " +
              'treating credentials as unverified and attempting login.')
        }
        log.debug('OAuth scope verification failure details', cause)
        return false
      }
      // Unwrap unexpected resolver failures (e.g. RuntimeException) so they propagate
      // unchanged, as they did before the resolution was delegated to a FutureTask;
      // only genuinely checked, non-IO causes get wrapped.
      if (cause instanceof RuntimeException || cause instanceof Error) {
        throw cause
      }
      throw new IOException('Scope resolver failed unexpectedly', cause)
    }
    for (String req : (required ?: Collections.<String>emptyList())) {
      if (!isSatisfied(granted, req)) {
        return false
      }
    }
    return true
  }

  /**
   * Fetches the scopes Google granted to an access token. Connect and read timeouts are set so
   * a hung request fails into the "verification unavailable" path instead of blocking
   * {@link #hasAllScopes(GoogleCredentials, List)} (and thus authenticate()) indefinitely.
   */
  @CompileDynamic
  static Set<String> fetchGrantedScopes(String token) throws IOException {
    def url = new URI("https://oauth2.googleapis.com/tokeninfo?access_token=${URLEncoder.encode(token, UTF8)}").toURL()
    HttpURLConnection connection = (HttpURLConnection) url.openConnection()
    connection.connectTimeout = TOKENINFO_TIMEOUT_MS
    connection.readTimeout = TOKENINFO_TIMEOUT_MS
    try {
      if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        String detail = ''
        try {
          detail = connection.errorStream?.getText(UTF8) ?: ''
        } catch (IOException ignored) {
        }
        throw new IOException("${TOKENINFO_REJECTED} with HTTP ${connection.responseCode}" +
            (detail ? ": ${detail}" : ''))
      }
      def json = new JsonSlurper().parse(connection.inputStream)
      (json?.scope ?: '')
          .split('\\s+')
          .collect { canonScope(it) }
          .findAll { it } as Set<String>
    } finally {
      connection.disconnect()
    }
  }

  /** Clears the token-scope cache; intended for diagnostics and test isolation. */
  static void clearScopeCache() {
    synchronized (SCOPE_CACHE_LOCK) {
      SCOPE_CACHE.clear()
    }
  }

  /**
   * Writes ADC JSON without ever persisting it with permissions broader than the current user.
   * On filesystems without POSIX or ACL permission support this refuses to write the secret.
   */
  static void writeAdcFile(File adcFile, String json) throws IOException {
    Path target = adcFile.toPath().toAbsolutePath()
    Path directory = target.parent
    Files.createDirectories(directory)
    Set<String> supportedViews = FileSystems.default.supportedFileAttributeViews()
    Path tmp = null
    try {
      if (supportedViews.contains(POSIX_VIEW)) {
        tmp = Files.createTempFile(directory, ADC_TEMP_PREFIX, ADC_TEMP_SUFFIX,
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(OWNER_ONLY_PERMISSIONS)))
      } else {
        tmp = Files.createTempFile(directory, ADC_TEMP_PREFIX, ADC_TEMP_SUFFIX)
      }
      restrictToOwner(tmp, supportedViews)
      Files.writeString(tmp, json, StandardCharsets.UTF_8)
      try {
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE)
      } catch (IOException atomicFailure) {
        if (Files.exists(tmp)) {
          try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
          } catch (IOException fallbackFailure) {
            fallbackFailure.addSuppressed(atomicFailure)
            throw fallbackFailure
          }
        } else {
          throw atomicFailure
        }
      }
    } finally {
      if (tmp != null) {
        Files.deleteIfExists(tmp)
      }
    }
  }

  /** Restricts an empty ADC file to the current user, or fails before any secrets are written. */
  static void restrictToOwner(Path file, Set<String> supportedViews) throws IOException {
    String unsupportedMessage = "Cannot restrict ADC file permissions to the current user on this file system; run 'gcloud auth application-default login' instead"
    try {
      if (supportedViews.contains(POSIX_VIEW)) {
        Set permissions = PosixFilePermissions.fromString(OWNER_ONLY_PERMISSIONS)
        Files.setPosixFilePermissions(file, permissions)
        if (Files.getPosixFilePermissions(file) != permissions) {
          throw new IOException(unsupportedMessage)
        }
        return
      }
      if (supportedViews.contains(ACL_VIEW)) {
        AclFileAttributeView view = Files.getFileAttributeView(file, AclFileAttributeView)
        if (view == null) {
          throw new IOException(unsupportedMessage)
        }
        AclEntry entry = AclEntry.newBuilder()
            .setType(AclEntryType.ALLOW)
            .setPrincipal(view.owner)
            .setPermissions(EnumSet.allOf(AclEntryPermission))
            .build()
        view.setAcl([entry])
        List<AclEntry> acl = view.acl
        // Compare principal and type rather than full AclEntry equality: some providers
        // normalize entry flags on read-back, which would make a correct owner-only ACL
        // compare unequal and block login entirely (observed risk on Windows/NTFS).
        boolean ownerOnly = acl.size() == 1 && acl[0].principal() == entry.principal() && acl[0].type() == AclEntryType.ALLOW
        if (!ownerOnly) {
          throw new IOException(unsupportedMessage)
        }
        return
      }
    } catch (IOException e) {
      if (e.message == unsupportedMessage) {
        throw e
      }
      throw new IOException(unsupportedMessage, e)
    }
    throw new IOException(unsupportedMessage)
  }

  private static boolean isSatisfied(Set<String> granted, String required) {
    String r = canonScope(required)
    if (granted.contains(r)) {
      return true
    }
    // Implications
    if (SCOPE_SHEETS_READONLY == r && granted.contains(SCOPE_SHEETS)) {
      return true
    }
    SCOPE_DRIVE_FILE == r && granted.contains('https://www.googleapis.com/auth/drive')
  }

  /**
   * Canonicalize some known scopes to a standard form
   * @param s the scope to canonicalize
   * @return the canonicalized scope or the original if no canonicalization is known
   */
  static String canonScope(String s) {
    if (s == null) {
      return null
    }
    switch (s) {
      case 'email', SCOPE_USERINFO_EMAIL -> SCOPE_USERINFO_EMAIL                // canonicalize to the short OIDC form
      default -> s
    }
  }
}
