package se.alipsa.matrix.gsheets

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets

/**
 * Checks for Google Cloud authentication by checking for Application Default Credentials (ADC)
 * and delegates to the 'gcloud' SDK for an interactive login if needed.
 */
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class BqAuthenticator {

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
          println "⚠️ Refresh token is invalid. A new login is required."
        }
      }
      return null
    }
  }

  static Credential loginInstalledApp(File clientSecretJson, List<String> scopes) {
    def http = GoogleNetHttpTransport.newTrustedTransport()
    def json = GsonFactory.getDefaultInstance()
    GoogleClientSecrets secrets = GoogleClientSecrets.load(json, new FileReader(clientSecretJson))

    def flow = new GoogleAuthorizationCodeFlow.Builder(http, json, secrets, scopes)
        .setAccessType("offline")       // get a refresh token
        .setApprovalPrompt("force")     // ensure refresh token on re-consent
        .build()

    def receiver = new LocalServerReceiver.Builder().setPort(0).build() // auto-pick a free port
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  /* drop-in replacement for gcloud auth application-default login
  // Writes ~/.config/gcloud/application_default_credentials.json
  // Usage:
  def creds = loginAndWriteAdc(
      new File("$HOME/client_secret_desktop.json"),
      [com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS,
       "https://www.googleapis.com/auth/drive.file",
       "openid", "email"],
      "$PROJECT_ID" // optional; mainly needed if you still call Google OAuth2 userinfo via googleapis.com
  )*/
  static GoogleCredentials loginAndWriteAdc(File clientSecretJson, List<String> scopes, String quotaProjectId = null) {
    def http = GoogleNetHttpTransport.newTrustedTransport()
    def json = GsonFactory.getDefaultInstance()
    GoogleClientSecrets secrets = GoogleClientSecrets.load(json, new FileReader(clientSecretJson))

    def flow = new GoogleAuthorizationCodeFlow.Builder(http, json, secrets, scopes)
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .build()

    def receiver = new LocalServerReceiver.Builder().setPort(0).build()
    Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")

    String refreshToken = cred.getRefreshToken()
    if (!refreshToken) {
      throw new IllegalStateException("No refresh token received. Remove old consent/tokens and try again.")
    }

    // Build ADC JSON
    def adc = [
        type           : "authorized_user",
        client_id      : secrets.details.clientId,
        client_secret  : secrets.details.clientSecret,
        refresh_token  : refreshToken
    ]
    if (quotaProjectId) adc.quota_project_id = quotaProjectId

    // Persist to the standard ADC path
    File adcFile = BqAuthenticator.ADC_FILE_PATH
    adcFile.parentFile.mkdirs()
    adcFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(adc))
    println "Wrote ADC to ${adcFile.absolutePath}"

    // Return a ready-to-use GoogleCredentials as well
    GoogleCredentials gc = GoogleCredentials.fromStream(new ByteArrayInputStream(adcFile.bytes))
    if (scopes) gc = gc.createScoped(scopes)
    if (quotaProjectId) gc = gc.createWithQuotaProject(quotaProjectId)
    gc.refresh()
    return gc
  }

  /**
   * Initiates authentication by calling 'gcloud auth login'.
   * This delegates the entire interactive login flow to the gcloud SDK.
   *
   * @return True if the gcloud command succeeds, False otherwise.
   */
  static boolean runGcloudLogin() {
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
        System.err.println "❌ Error: gcloud SDK is not installed or not in your PATH."
        System.err.println "Please install it to proceed with authentication."
      } else {
        System.err.println "❌ Failed to execute gcloud command: ${e.message}"
      }
      return false
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt() // Preserve the interrupted status
      System.err.println "\nLogin process cancelled by user."
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
  static GoogleCredentials authenticate(List<String> scopes = SCOPES) {
    def creds = getCredentials(scopes)

    if (creds) {
      def email = getUserEmail(creds)
      if (email) {
        println "✅ Google Cloud is already authenticated with email: ${email}"
      } else {
        println "✅ Google Cloud is already authenticated. (Could not fetch email)."
      }
      return creds
    } else {
      println "⚠️ Google Cloud is not authenticated. Initiating login flow..."
      if (runGcloudLogin()) {
        // After a successful login, we must get the newly created credentials
        println "Re-checking credentials after login..."

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
            println "Login successful, but credentials not yet available. Retrying in ${retryDelayMs}ms..."
            Thread.currentThread().sleep(retryDelayMs)
          }
        }

        if (newCreds) {
          def email = getUserEmail(newCreds)
          if (email) {
            println "✅ Authentication successful for user: ${email}"
          } else {
            println "✅ Authentication successful after login."
          }
          return newCreds
        } else {
          System.err.println "❌ Authentication failed. Could not validate credentials after login, even after retrying."
          return null
        }
      } else {
        return null
      }
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