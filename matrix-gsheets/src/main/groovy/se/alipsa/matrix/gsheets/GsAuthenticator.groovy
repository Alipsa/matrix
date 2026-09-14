package se.alipsa.matrix.gsheets

import groovy.json.JsonSlurper

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.oauth2.GoogleCredentials

import se.alipsa.matrix.core.util.Logger

import java.security.GeneralSecurityException

/**
 * Checks for Google Cloud authentication by checking for Application Default Credentials (ADC)
 * and delegates to the 'gcloud' SDK for an interactive login if needed.
 */
class GsAuthenticator {

  private static final Logger log = Logger.getLogger(GsAuthenticator)

  static final String SCOPE_CLOUD_PLATFORM = 'https://www.googleapis.com/auth/cloud-platform'
  static final String SCOPE_SHEETS = SheetsScopes.SPREADSHEETS
  static final String SCOPE_SHEETS_READONLY = SheetsScopes.SPREADSHEETS_READONLY
  static final String SCOPE_DRIVE_FILE = 'https://www.googleapis.com/auth/drive.file'
  static final String SCOPE_OPENID = 'openid'
  static final String SCOPE_USERINFO_EMAIL = 'https://www.googleapis.com/auth/userinfo.email'

  // Serializes login attempts within this JVM so concurrent callers wait for an in-flight
  // login instead of failing immediately, and so a failed/cancelled attempt doesn't
  // permanently prevent future retries (unlike a one-shot "already tried" flag would).
  private static final Object LOGIN_LOCK = new Object()
  // GoogleCredentials.getApplicationDefault() caches the first ADC instance it reads. Retain a
  // newer instance returned by an interactive login so later calls do not revert to stale scopes.
  private static volatile GoogleCredentials authenticatedCredentials

  private GsAuthenticator() { }

  private static final String PROP_USER_HOME = 'user.home'

  // The location where gcloud stores Application Default Credentials
  static final File ADC_FILE_PATH = new File(System.getProperty(PROP_USER_HOME), '.config/gcloud/application_default_credentials.json')

  // A caller-registered OAuth 2.0 "Desktop app" client, used in place of gcloud's shared
  // default client. Google blocks sensitive scopes (e.g. spreadsheets, drive) for that
  // shared client, so this is required for those scopes to keep working.
  static final File CLIENT_SECRET_FILE = new File(System.getProperty(PROP_USER_HOME), 'client_secret_desktop.json')

  private static final String GCLOUD_CMD = 'gcloud'
  private static final String GCLOUD_AUTH = 'auth'
  private static final String GCLOUD_APP_DEFAULT = 'application-default'
  private static final String ENV_GOOGLE_CLOUD_PROJECT = 'GOOGLE_CLOUD_PROJECT'
  private static final String AUTHENTICATE_OPERATION = 'authenticate'
  private static final String AUTHENTICATION_FAILED = 'Authentication failed. Could not validate credentials after login, even after retrying.'
  private static final String MISSING_REQUIRED_SCOPES = 'Authentication succeeded but the granted token is missing required scopes.'

  static final List<String> SCOPES = [
      SCOPE_CLOUD_PLATFORM,
      SCOPE_SHEETS,
      SCOPE_DRIVE_FILE,
      SCOPE_OPENID,
      SCOPE_USERINFO_EMAIL
  ]

  /** Pluggable authentication primitives, primarily for offline testing. */
  interface AuthBackend {
    GoogleCredentials existing(List<String> scopes)
    GoogleCredentials login(List<String> scopes, String quotaProjectId)
    boolean hasAllScopes(GoogleCredentials creds, List<String> scopes)
    String userEmail(GoogleCredentials creds)
  }

  private static final AuthBackend DEFAULT_BACKEND = new AuthBackend() {
    @Override
    GoogleCredentials existing(List<String> scopes) { getCredentials(scopes) }

    @Override
    GoogleCredentials login(List<String> scopes, String quotaProjectId) { loginAndGetCredentials(scopes, quotaProjectId) }

    @Override
    boolean hasAllScopes(GoogleCredentials creds, List<String> scopes) { GsAuthUtils.hasAllScopes(creds, scopes) }

    @Override
    String userEmail(GoogleCredentials creds) { getUserEmail(creds) }
  }

  static List<String> normalizeScopesForGcloud(List<String> scopes) {
    // Always add cloud-platform (gcloud insists on this when --scopes is used)
    Set<String> s = new LinkedHashSet<>((scopes ?: Collections.<String> emptyList()))
    s.add(SCOPE_CLOUD_PLATFORM)
    // Prefer write over readonly to avoid future “missing scope” churn
    if (s.remove(SCOPE_SHEETS_READONLY)) {
      s.add(SCOPE_SHEETS)
    }
    s.remove('email') // never ask for short OIDC email; use userinfo.email
    return new ArrayList<>(s)
  }

  /**
   * Checks for existing and valid Application Default Credentials.
   *
   * @return A GoogleCredentials object if they are valid or can be refreshed.
   *         Returns null if no credentials are found or if they require a new login.
   */
  @SuppressWarnings('ReturnNullFromCatchBlock')
  static GoogleCredentials getCredentials(List<String> scopes = SCOPES, boolean verbose = false) {
    try {
      // Tries to find credentials in the environment (ADC)
      GoogleCredentials sourceCredentials = authenticatedCredentials ?: GoogleCredentials.getApplicationDefault()
      def credentials = applyQuotaProject(sourceCredentials.createScoped(scopes), verbose)

      // The refreshIfExpired() method will handle checking if a refresh is needed.
      // If the refresh token is invalid, it will throw an IOException.
      credentials.refreshIfExpired()
      return credentials
    } catch (IOException e) {
      // This can be thrown if ADC file is not found or if refresh token is invalid.
      if (verbose) {
        if (ADC_FILE_PATH.exists()) {
          log.info '⚠️ Refresh token is invalid. A new login is required.'
        }
      }
      return null
    }
  }

  /**
   * Applies the configured quota project to the credentials, if any: the
   * {@code GOOGLE_CLOUD_QUOTA_PROJECT} env var, then {@code GOOGLE_CLOUD_PROJECT}, then the
   * {@code quota_project_id} recorded in the ADC file.
   */
  private static GoogleCredentials applyQuotaProject(GoogleCredentials credentials, boolean verbose = false) {
    String qp = System.getenv('GOOGLE_CLOUD_QUOTA_PROJECT') ?: System.getenv(ENV_GOOGLE_CLOUD_PROJECT)
    if (!qp && ADC_FILE_PATH.exists()) {
      try {
        Map json = new JsonSlurper().parseText(ADC_FILE_PATH.getText('UTF-8')) as Map
        qp = json?.quota_project_id as String
      } catch (ignored) {
      }
    }
    if (qp) {
      credentials = credentials.createWithQuotaProject(qp)
      if (verbose) {
        log.info("Using quota project: $qp")
      }
    }
    credentials
  }

  /**
   * Initiates authentication by calling 'gcloud auth login'.
   * This delegates the entire interactive login flow to the gcloud SDK.
   *
   * @return True if the gcloud command succeeds, False otherwise.
   */
  static boolean runGcloudLogin(List<String> requestedScopes) {
    runGcloudLogin(requestedScopes, isCommandAvailable(GCLOUD_CMD))
  }

  @SuppressWarnings('UnnecessaryGString')
  private static boolean runGcloudLogin(List<String> requestedScopes, boolean gcloudAvailable) {
    List<String> scopes = normalizeScopesForGcloud(requestedScopes)
    if (gcloudAvailable) {
      try {
        // The command will run interactively in the user's terminal.
        //def command = ['gcloud', 'auth', 'login', '--update-adc', '--enable-gdrive-access']
        def command = [GCLOUD_CMD, GCLOUD_AUTH, GCLOUD_APP_DEFAULT, 'login',
                       '--scopes', scopes.join(',')]
        def process = new ProcessBuilder(command)
            .inheritIO() // This connects the subprocess's I/O to the current terminal
            .start()

        def exitCode = process.waitFor()

        return exitCode == 0
      } catch (IOException e) {
        // This is often thrown if 'gcloud' command is not found.
        if (e.message.contains('Cannot run program "' + GCLOUD_CMD + '"')) {
          log.error 'Error: gcloud SDK is not installed or not in your PATH.'
          log.error 'Please install it to proceed with authentication.'
        } else {
          log.error "Failed to execute gcloud command: ${e.message}"
        }
        return false
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt() // Preserve the interrupted status
        log.warn "\nLogin process cancelled by user."
        return false
      }
    } else {
      log.warn 'gcloud SDK not found. Attempting programmatic login instead.'
      return runProgrammaticLogin(scopes, System.getenv(ENV_GOOGLE_CLOUD_PROJECT))
    }
  }

  private static boolean runProgrammaticLogin(List<String> scopes, String quotaProjectId = null) {
    programmaticLoginAndGetCredentials(scopes, quotaProjectId) != null
  }

  private static GoogleCredentials programmaticLoginAndGetCredentials(List<String> scopes, String quotaProjectId) {
    try {
      // Uses matrix-gsheets' own bundled OAuth client, or CLIENT_SECRET_FILE if the
      // caller has registered their own client and wants to override it.
      GoogleCredentials credentials = GsAuthUtils.loginAndWriteAdc(scopes, quotaProjectId)
      authenticatedCredentials = credentials
      credentials
    } catch (IllegalStateException | IOException | GeneralSecurityException e) {
      log.error("Failed to execute programmatic login: ${e.message}", e)
      null
    }
  }

  /**
   * Uses the authenticated credentials to fetch the user's email address.
   *
   * @param credentials The authenticated GoogleCredentials object.
   * @return The user's email address as a String.
   */
  @SuppressWarnings('ReturnNullFromCatchBlock')
  static String getUserEmail(GoogleCredentials credentials) {
    try {
      def http = GoogleNetHttpTransport.newTrustedTransport()
      def json = GsonFactory.getDefaultInstance()
      def init = GsAuthUtils.noUserProjectInitializer(credentials)
      def oauth2 = new Oauth2.Builder(http, json, init)
          .setApplicationName('Matrix GSheets')
          .build()
      return oauth2.userinfo().get().execute().getEmail()
    } catch (Exception e) {
      log.warn("Could not fetch user email: ${e.message}", e)
      return null
    }
  }

  /**
   * Ensures that the user is authenticated with gcp.
   *
   * @param scope the scope to grant access to.
   * @return credentials, or throws {@link SheetOperationException} when authentication fails
   */
  static GoogleCredentials authenticate(String scope) {
    authenticate([scope])
  }

  /**
   * Ensures that the user is authenticated with gcp.
   *
   * Requested scopes are merged into {@link #SCOPES} (cloud-platform, spreadsheets, drive.file,
   * openid, and userinfo.email). A read-only spreadsheets scope is upgraded to read/write, so it
   * may prompt for read/write consent.
   *
   * @param requestedScopes scopes to grant access to, defaulting to {@link #SCOPES}
   * @param quotaProjectId optional quota project ID
   * @return credentials, or throws {@link SheetOperationException} when authentication fails
   */
  static GoogleCredentials authenticate(List<String> requestedScopes = SCOPES, String quotaProjectId = null) {
    authenticate(requestedScopes, quotaProjectId, DEFAULT_BACKEND)
  }

  /**
   * Authenticates with the supplied backend. Requested scopes are merged into {@link #SCOPES},
   * and read-only Sheets access is upgraded to read/write access.
   *
   * @return credentials, or throws {@link SheetOperationException} when authentication fails
   */
  static GoogleCredentials authenticate(List<String> requestedScopes, String quotaProjectId, AuthBackend backend) {
    List<String> scopes = SCOPES
    if (requestedScopes) {
      Set<String> effective = new LinkedHashSet<>(SCOPES)
      effective.addAll(requestedScopes)
      // Upgrade readonly to read-write once to avoid future churn
      if (effective.remove(SCOPE_SHEETS_READONLY)) {
        effective.add(SCOPE_SHEETS)
      }
      scopes = new ArrayList<>(effective)
    }

    GoogleCredentials creds = backend.existing(new ArrayList<>(scopes))
    boolean hasScopes = creds != null && backend.hasAllScopes(creds, scopes)
    if (!hasScopes) {
      creds = backend.login(scopes, quotaProjectId)
    }

    if (creds == null) {
      log.error AUTHENTICATION_FAILED
      throw new SheetOperationException(AUTHENTICATE_OPERATION, AUTHENTICATION_FAILED)
    }
    if (!hasScopes && !backend.hasAllScopes(creds, scopes)) {
      log.error MISSING_REQUIRED_SCOPES
      throw new SheetOperationException(AUTHENTICATE_OPERATION, MISSING_REQUIRED_SCOPES)
    }

    // Only try userinfo if we actually asked for it
    boolean wantEmail = scopes.any { it == SCOPE_USERINFO_EMAIL || it == SCOPE_OPENID }
    if (wantEmail) {
      def email = backend.userEmail(creds)
      log.info "Google Cloud is authenticated with email: ${email}"
    } else {
      log.info 'Google Cloud is authenticated.'
    }
    creds
  }

  /**
   * Serializes interactive login within this JVM: only one login flow runs at a time, and
   * concurrent callers block on {@link #LOGIN_LOCK} and then re-check credentials rather
   * than failing immediately or racing the in-flight attempt. A failed or cancelled login
   * does not leave any lasting state, so a later call will simply try again.
   */
  private static GoogleCredentials loginAndGetCredentials(List<String> scopes, String quotaProjectId) {
    synchronized (LOGIN_LOCK) {
      // Re-check: another thread may have completed login while we waited for the lock.
      GoogleCredentials creds = getCredentials(new ArrayList<>(scopes), false)
      if (creds != null && GsAuthUtils.hasAllScopes(creds, scopes)) {
        return creds
      }

      GoogleCredentials programmaticCredentials = programmaticLoginAndGetCredentials(scopes, quotaProjectId)
      if (programmaticCredentials != null) {
        return programmaticCredentials
      }
      boolean gcloudAvailable = isCommandAvailable(GCLOUD_CMD)
      if (!gcloudAvailable || !runGcloudLogin(scopes, gcloudAvailable)) {
        return null
      }
      if (quotaProjectId) {
        new ProcessBuilder([GCLOUD_CMD, GCLOUD_AUTH, GCLOUD_APP_DEFAULT, 'set-quota-project', quotaProjectId])
            .inheritIO().start().waitFor()
      }
      // There can be a small delay between the login flow completing and the
      // ADC file being fully written to disk. Retry a few times to handle this.
      waitForCredentials(scopes)
    }
  }

  private static GoogleCredentials waitForCredentials(List<String> scopes) {
    int maxRetries = 5
    int retryDelayMs = 1000 // 1 second
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      // Call with verbose=false to avoid noisy output during retries
      GoogleCredentials creds = freshAdcCredentials(scopes) ?: getCredentials(scopes, false)
      if (creds) {
        authenticatedCredentials = creds
        return creds
      }
      if (attempt < maxRetries) {
        log.debug "Login successful, but credentials not yet available. Retrying in ${retryDelayMs}ms..."
        Thread.currentThread().sleep(retryDelayMs)
      }
    }
    null
  }

  private static GoogleCredentials freshAdcCredentials(List<String> scopes) {
    if (!ADC_FILE_PATH.exists()) {
      return null
    }
    try {
      GoogleCredentials credentials = ADC_FILE_PATH.withInputStream { InputStream input ->
        GoogleCredentials.fromStream(input).createScoped(scopes)
      } as GoogleCredentials
      credentials = applyQuotaProject(credentials)
      credentials.refreshIfExpired()
      credentials
    } catch (IOException e) {
      log.debug("Could not reload fresh ADC credentials: ${e.message}", e)
      null
    }
  }

  private static boolean isCommandAvailable(String command) {
    try {
      def osName = System.getProperty('os.name').toLowerCase()
      def processBuilder = osName.contains('win') ?
          new ProcessBuilder('where', command) :
          new ProcessBuilder('which', command)
      def process = processBuilder.start()
      return process.waitFor() == 0
    } catch (Exception e) {
      return false
    }
  }

  /**
   * Dev convenience entry point — not part of the public API.
   */
  static void main(String[] args) {
    if (args.length > 0) {
      authenticate(args.collect() as List)
    } else {
      authenticate()
    }
  }

}
