package se.alipsa.matrix.gsheets

/**
 * Checks for Google Cloud authentication by checking for Application Default Credentials (ADC)
 * and delegates to the 'gcloud' SDK for an interactive login if needed.
 */
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@CompileStatic
class BqAuthenticator {

  private static final Logger log = LogManager.getLogger(BqAuthenticator)
  private BqAuthenticator() {}

  // The location where gcloud stores Application Default Credentials
  static final File ADC_FILE_PATH = new File(System.getProperty("user.home"), ".config/gcloud/application_default_credentials.json")

  static final List<String> SCOPES = [
      "https://www.googleapis.com/auth/cloud-platform",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/drive",
      "openid"
  ]

  /**
   * Checks for existing and valid Application Default Credentials.
   *
   * @return A GoogleCredentials object if they are valid or can be refreshed.
   *         Returns null if no credentials are found or if they require a new login.
   */
  static GoogleCredentials getCredentials(List<String> scopes = SCOPES, boolean verbose = true) {
    try {
      // Tries to find credentials in the environment (ADC)
      def credentials = GoogleCredentials.getApplicationDefault().createScoped(scopes)

      // Determine quota project: env var -> GOOGLE_CLOUD_PROJECT -> ADC file's quota_project_id
      String qp = System.getenv('GOOGLE_CLOUD_QUOTA_PROJECT') ?: System.getenv('GOOGLE_CLOUD_PROJECT')
      if (!qp && ADC_FILE_PATH.exists()) {
        try {
          Map json = new JsonSlurper().parseText(ADC_FILE_PATH.getText('UTF-8')) as Map
          qp = json?.quota_project_id as String
        } catch (ignored) {}
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
  static boolean runGcloudLogin(List<String> scopes) {
    if (isCommandAvailable('gcloud')) {
      try {
        // The command will run interactively in the user's terminal.
        def command = ["gcloud", "auth", "login", "--update-adc", "--enable-gdrive-access"]
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
      return runProgrammaticLogin(scopes)
    }
  }

  private static boolean runProgrammaticLogin(List<String> scopes) {
    try {
      def home = System.getProperty("user.home")
      def clientSecretFile = new File("$home/client_secret_desktop.json")

      def credentials = BqAuthUtils.loginAndWriteAdc(clientSecretFile, scopes, null)

      // If credentials are null, it means the process failed.
      return credentials != null
    } catch (IllegalStateException e) {
      log.error("Failed to obtain credentials!", e)
      return false
    } catch (Exception e) {
      log.error("Faild to execute programmatic login: ${e.message}", e)
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
      def httpTransport = GoogleNetHttpTransport.newTrustedTransport()
      def jsonFactory = GsonFactory.getDefaultInstance()
      def adapter = new HttpCredentialsAdapter(credentials)

      def oauth2Service = new Oauth2.Builder(httpTransport, jsonFactory, adapter)
          .setApplicationName("Groovy-Auth-Script")
          .build()

      return oauth2Service.userinfo().get().execute().getEmail()
    } catch (Exception e) {
      log.error "Could not fetch user email: ${e.message}"
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
  static GoogleCredentials authenticate(List<String> scopes = SCOPES) {
    def creds = getCredentials(scopes)

    if (creds) {
      def email = getUserEmail(creds)
      if (email) {
        log.info "Google Cloud is already authenticated with email: ${email}"
      } else {
        log.info "Google Cloud is already authenticated. (Could not fetch email)."
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