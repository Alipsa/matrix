package se.alipsa.matrix.gsheets

/**
 * Checks for Google Cloud authentication by checking for Application Default Credentials (ADC)
 * and delegates to the 'gcloud' SDK for an interactive login if needed.
 */
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.oauth2.GoogleCredentials
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.security.GeneralSecurityException
import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
class BqAuthenticator {

  private static final Logger log = LogManager.getLogger(BqAuthenticator)

  static final String SCOPE_CLOUD_PLATFORM = "https://www.googleapis.com/auth/cloud-platform"
  static final String SCOPE_SHEETS = SheetsScopes.SPREADSHEETS
  static final String SCOPE_SHEETS_READONLY = SheetsScopes.SPREADSHEETS_READONLY
  static final String SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
  static final String SCOPE_OPENID = "openid"
  static final String SCOPE_USERINFO_EMAIL = "https://www.googleapis.com/auth/userinfo.email"

  private static final AtomicBoolean LOGIN_ATTEMPTED = new AtomicBoolean(false);

  private BqAuthenticator() {}

  // The location where gcloud stores Application Default Credentials
  static final File ADC_FILE_PATH = new File(System.getProperty("user.home"), ".config/gcloud/application_default_credentials.json")

  static final List<String> SCOPES = [
      SCOPE_CLOUD_PLATFORM,
      SCOPE_SHEETS,
      SCOPE_DRIVE_FILE,
      SCOPE_OPENID,
      SCOPE_USERINFO_EMAIL
  ]

  @CompileStatic
  static List<String> normalizeScopesForGcloud(List<String> scopes) {
    // Always add cloud-platform (gcloud insists on this when --scopes is used)
    LinkedHashSet<String> s = new LinkedHashSet<>((scopes ?: Collections.<String> emptyList()))
    s.add(SCOPE_CLOUD_PLATFORM)
    // Prefer write over readonly to avoid future “missing scope” churn
    if (s.remove(SCOPE_SHEETS_READONLY)) s.add(SCOPE_SHEETS)
    s.remove("email") // never ask for short OIDC email; use userinfo.email
    return new ArrayList<>(s)
  }

  /**
   * Checks for existing and valid Application Default Credentials.
   *
   * @return A GoogleCredentials object if they are valid or can be refreshed.
   *         Returns null if no credentials are found or if they require a new login.
   */
  static GoogleCredentials getCredentials(List<String> scopes = SCOPES, boolean verbose = false) {
    try {
      // Tries to find credentials in the environment (ADC)
      def credentials = GoogleCredentials.getApplicationDefault().createScoped(scopes)

      // Determine quota project: env var -> GOOGLE_CLOUD_PROJECT -> ADC file's quota_project_id
      String qp = System.getenv('GOOGLE_CLOUD_QUOTA_PROJECT') ?: System.getenv('GOOGLE_CLOUD_PROJECT')
      if (!qp && ADC_FILE_PATH.exists()) {
        try {
          Map json = new JsonSlurper().parseText(ADC_FILE_PATH.getText('UTF-8')) as Map
          qp = json?.quota_project_id as String
        } catch (ignored) {
        }
      }
      if (qp) {
        credentials = credentials.createWithQuotaProject(qp)
        if (verbose) println "Using quota project: ${qp}"
      }

      // The refreshIfExpired() method will handle checking if a refresh is needed.
      // If the refresh token is invalid, it will throw an IOException.
      credentials.refreshIfExpired()
      return credentials
    } catch (IOException e) {
      // This can be thrown if ADC file is not found or if refresh token is invalid.
      if (verbose) {
        if (ADC_FILE_PATH.exists()) {
          log.info "⚠️ Refresh token is invalid. A new login is required."
        }
      }
      return null
    }
  }

  /**
   * Initiates authentication by calling 'gcloud auth login'.
   * This delegates the entire interactive login flow to the gcloud SDK.
   *
   * @return True if the gcloud command succeeds, False otherwise.
   */
  static boolean runGcloudLogin(List<String> requestedScopes) {
    List<String> scopes = normalizeScopesForGcloud(requestedScopes)
    if (isCommandAvailable('gcloud')) {
      try {
        // The command will run interactively in the user's terminal.
        //def command = ["gcloud", "auth", "login", "--update-adc", "--enable-gdrive-access"]
        def command = ["gcloud", "auth", "application-default", "login",
                       "--scopes", scopes.join(",")]
        def process = new ProcessBuilder(command)
            .inheritIO() // This connects the subprocess's I/O to the current terminal
            .start()

        def exitCode = process.waitFor()

        if (exitCode != 0) {
          // gcloud itself will have printed a specific error.
          return false
        }
        return true
      } catch (IOException e) {
        // This is often thrown if 'gcloud' command is not found.
        if (e.message.contains("Cannot run program \"gcloud\"")) {
          log.error "Error: gcloud SDK is not installed or not in your PATH."
          log.error "Please install it to proceed with authentication."
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
      log.warn "gcloud SDK not found. Attempting programmatic login instead."
      return runProgrammaticLogin(scopes, System.getenv('GOOGLE_CLOUD_PROJECT'))
    }
  }

  private static boolean runProgrammaticLogin(List<String> scopes, String quotaProjectId = null) {
    try {
      def home = System.getProperty("user.home")
      def clientSecretFile = new File("$home/client_secret_desktop.json")

      def credentials = BqAuthUtils.loginAndWriteAdc(clientSecretFile, scopes, quotaProjectId)

      // If credentials are null, it means the process failed.
      return credentials != null
    } catch (IllegalStateException | IOException | GeneralSecurityException e) {
      log.error("Failed to execute programmatic login: ${e.message}", e)
      return false
    }
  }

  /**
   * Uses the authenticated credentials to fetch the user's email address.
   *
   * @param credentials The authenticated GoogleCredentials object.
   * @return The user's email address as a String.
   */
  static String getUserEmail(GoogleCredentials credentials) {
    try {
      def http = GoogleNetHttpTransport.newTrustedTransport()
      def json = GsonFactory.getDefaultInstance()
      def init = BqAuthUtils.noUserProjectInitializer(credentials)
      def oauth2 = new Oauth2.Builder(http, json, init)
          .setApplicationName("Matrix GSheets")
          .build()
      return oauth2.userinfo().get().execute().getEmail()
    } catch (Exception e) {
      System.err.println "Could not fetch user email: ${e.message}"
      return null
    }
  }

  /**
   * Ensures that the user is authenticated with gcp.
   *
   * @param scope the scope to grant access to.
   * @return true if successful, false otherwise
   */
  static GoogleCredentials authenticate(String scope) {
    return authenticate([scope])
  }

  /**
   * Ensures that the user is authenticated with gcp.
   *
   * @param scopes a list of scopes to grant access to, defaults to SCOPES i.e. cloud platform, email, drive, openId
   * @return true if successful, false otherwise
   */
  static GoogleCredentials authenticate(List<String> requestedScopes = SCOPES, String quotaProjectId = null) {
    List<String> scopes = SCOPES
    if (requestedScopes) {
      LinkedHashSet<String> effective = new LinkedHashSet<>(SCOPES)
      effective.addAll(requestedScopes)
      // Upgrade readonly to read-write once to avoid future churn
      if (effective.remove(SCOPE_SHEETS_READONLY)) effective.add(SCOPE_SHEETS)
      scopes = new ArrayList<>(effective)
    }
    def creds = getCredentials(new ArrayList<>(scopes))

    if (creds == null || !BqAuthUtils.hasAllScopes(creds, scopes)) {
      // Do ONE interactive login (ADC or programmatic), then reload creds
      if (LOGIN_ATTEMPTED.compareAndSet(false, true)) {
        boolean ok = isCommandAvailable('gcloud')
            ? runGcloudLogin(scopes)
            : runProgrammaticLogin(scopes, quotaProjectId)
        if (!ok && isCommandAvailable('gcloud')) {
          // one fallback try
          ok = runProgrammaticLogin(scopes, quotaProjectId)
        }
        if (!ok) return null
        if (quotaProjectId && isCommandAvailable('gcloud')) {
          new ProcessBuilder(["gcloud", "auth", "application-default", "set-quota-project", quotaProjectId])
              .inheritIO().start().waitFor()
        }
        creds = getCredentials(scopes, true)
      } else {
        // We already attempted an interactive login in this JVM; don’t prompt again.
        creds = getCredentials(scopes, false)
      }
    }

    if (creds) {
      // Only try userinfo if we actually asked for it
      boolean wantEmail = scopes.any { it == SCOPE_USERINFO_EMAIL || it == SCOPE_OPENID }
      if (wantEmail) {
        def email = getUserEmail(creds)
        log.info "Google Cloud is already authenticated with email: ${email}"
      } else {
        log.info "Google Cloud is already authenticated."
      }
      return creds
    } else {
      log.info "Google Cloud is not authenticated. Initiating login flow..."
      if (runGcloudLogin(scopes)) {
        // After a successful login, we must get the newly created credentials
        log.debug "Re-checking credentials after login..."

        // There can be a small delay between gcloud exiting and the ADC file being
        // fully written to disk. We will retry a few times to handle this race condition.
        def newCreds = null
        int maxRetries = 5
        int retryDelayMs = 1000 // 1 second

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
          // Call with verbose=false to avoid noisy output during retries
          newCreds = getCredentials(scopes, false)
          if (newCreds) {
            break // Success, exit the loop
          }
          if (attempt < maxRetries) {
            log.debug "Login successful, but credentials not yet available. Retrying in ${retryDelayMs}ms..."
            Thread.currentThread().sleep(retryDelayMs)
          }
        }

        if (newCreds) {
          def email = getUserEmail(newCreds)
          if (email) {
            log.info "Authentication successful for user: ${email}"
          } else {
            log.info "Authentication successful after login."
          }
          return newCreds
        } else {
          log.error "Authentication failed. Could not validate credentials after login, even after retrying."
          return null
        }
      } else {
        return null
      }
    }
  }

  private static boolean isCommandAvailable(String command) {
    try {
      def osName = System.getProperty("os.name").toLowerCase()
      def processBuilder = osName.contains("win") ?
          new ProcessBuilder("where", command) :
          new ProcessBuilder("which", command)
      def process = processBuilder.start()
      return process.waitFor() == 0
    } catch (Exception e) {
      return false
    }
  }

  // --- Main execution ---
  static void main(String[] args) {
    if (args.length > 0) {
      authenticate(args.collect() as List)
    } else {
      authenticate()
    }
  }

}