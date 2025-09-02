package se.alipsa.matrix.gsheets

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import groovy.json.JsonOutput

class BqAuthUtils {
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
}
