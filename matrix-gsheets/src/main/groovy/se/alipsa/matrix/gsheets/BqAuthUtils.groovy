package se.alipsa.matrix.gsheets

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.util.Logger

import static se.alipsa.matrix.gsheets.BqAuthenticator.*
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
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic

@CompileStatic
class BqAuthUtils {

  private static final Logger log = Logger.getLogger(BqAuthUtils)

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
    if (!clientSecretJson.exists()) {
      log.info("Please create OAuth 2.0 credentials for a 'Desktop app' in the Google Cloud Console and save the downloaded JSON file to this location.")
      log.info("""
          1. Go to the Google Cloud Console (https://console.cloud.google.com/).
          2. Navigate to APIs & Services > Credentials.
          3. Click "Create Credentials" and choose "OAuth 2.0 Client ID".
          4. Select "Desktop App" as the application type.
          5. After creation, click the download icon to save the JSON file to $clientSecretJson.absolutePath.
        """)
      throw new IllegalStateException("Error: The 'client_secret_desktop.json' file was not found at $clientSecretJson.absolutePath")
    }
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
    log.info("Wrote ADC to ${adcFile.absolutePath}")

    // Return a ready-to-use GoogleCredentials as well
    GoogleCredentials gc = GoogleCredentials.fromStream(new ByteArrayInputStream(adcFile.bytes))
    if (scopes) gc = gc.createScoped(scopes)
    if (quotaProjectId) gc = gc.createWithQuotaProject(quotaProjectId)
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
        req.getHeaders().set("X-Goog-User-Project", (Object) null)
        req.getHeaders().set("x-goog-user-project", (Object) null)
      }
    }
  }

  @CompileDynamic
  static boolean hasAllScopes(GoogleCredentials creds, List<String> required) {
    if (creds == null) return false
    try {
      creds.refresh()
    } catch (IOException ignored) {
      creds.refreshIfExpired()
    }
    def token = creds.accessToken?.tokenValue
    if (!token) return false
    //def url = new URI("https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=${token}").toURL() // old url
    def url = new URI("https://oauth2.googleapis.com/tokeninfo?access_token=${URLEncoder.encode(token,'UTF-8')}").toURL()
    def json = new JsonSlurper().parse(url)
    def granted = (json?.scope ?: "")
        .split("\\s+")
        .collect { canonScope(it) }
        .findAll { it } as Set<String>
    for (String req : (required ?: Collections.<String>emptyList())) {
      if (!isSatisfied(granted, req)) return false
    }
    return true
  }

  @CompileStatic
  private static boolean isSatisfied(Set<String> granted, String required) {
    String r = canonScope(required)
    if (granted.contains(r)) return true
    // Implications
    if (SCOPE_SHEETS_READONLY.equals(r) && granted.contains(SCOPE_SHEETS)) return true
    if (SCOPE_DRIVE_FILE.equals(r) && granted.contains("https://www.googleapis.com/auth/drive")) return true
    return false
  }

  /**
   * Canonicalize some known scopes to a standard form
   * @param s the scope to canonicalize
   * @return the canonicalized scope or the original if no canonicalization is known
   */
  static String canonScope(String s) {
    if (s == null) return null
    switch (s) {
      case "email", SCOPE_USERINFO_EMAIL -> SCOPE_USERINFO_EMAIL                // canonicalize to the short OIDC form
      default -> s
    }
  }
}
