package se.alipsa.matrix.gsheets

import static se.alipsa.matrix.gsheets.BqAuthenticator.*
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Exports a Matrix to a Google Sheet
 */
@CompileStatic
class GsExporter {

  /**
   * Creates a new Google Spreadsheet and writes the Matrix into the first sheet.
   * The spreadsheet title and the first sheet name are derived from the Matrix name.
   *
   * Your default project can be seen by doing
   * <pre>
   *   <code>gcloud config get-value project</code>
   * </pre>
   * The default project must have granted the caller the roles/serviceusage.serviceUsageConsumer privilege
   * make that your ADC quota project:
   * <pre><code>
   *   PROJECT_ID=$(gcloud config get-value project 2> /dev/null)
   *   gcloud auth application-default set-quota-project $PROJECT_ID
   * </code></pre>
   * Enable Sheets and Drive APIs on that project
   * <pre><code>gcloud services enable \
   * sheets.googleapis.com \
   * drive.googleapis.com \
   * --project=$PROJECT_ID
   * </code></pre>
   * <p>You can verify that by doing</p>
   * <pre>
   *  <code>gcloud services list --enabled --project=$PROJECT_ID | grep -E 'sheets|drive'</code>
   * </pre>
   *
   * @param matrix the Matrix to export (first row in the sheet will be the column names)
   * @return the created spreadsheetId (open at https://docs.google.com/spreadsheets/d/{spreadsheetId}/edit)
   */
  static String exportSheet(Matrix matrix) {
    if (matrix == null) {
      throw new IllegalArgumentException("matrix must not be null")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("matrix has no columns")
    }

    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    // Need write scope for creating/updating spreadsheets
    def scopes = [SCOPE_DRIVE_FILE, SheetsScopes.SPREADSHEETS] + SCOPES

    GoogleCredentials credentials = authenticate(scopes)
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)
    /*
    def home = System.getProperty("user.home")
    def cred = BqAuthenticator.loginInstalledApp(
        new File("$home/client_secret_desktop.json"),
        scopes
    )

     */

    Sheets sheets = new Sheets.Builder(transport, gsonFactory, cred)
        .setApplicationName("Matrix GSheets")
        .build()

    String titleBase = matrix.matrixName ?: "Matrix ${LocalDateTime.now().toString().replace('T', '_')}"
    String sheetName = sanitizeSheetName(titleBase)
    String spreadsheetTitle = titleBase

    // 1) Create an empty spreadsheet with one sheet named after the matrix
    Spreadsheet requestBody = new Spreadsheet()
        .setProperties(new SpreadsheetProperties().setTitle(spreadsheetTitle))
        .setSheets([
            new Sheet().setProperties(new SheetProperties().setTitle(sheetName))
        ])

    Spreadsheet created = sheets.spreadsheets()
        .create(requestBody)
        .setFields("spreadsheetId") // we only need the id here
        .execute()

    String spreadsheetId = created.getSpreadsheetId()

    // 2) Build the data: header row + data rows
    List<String> headers = (List<String>) matrix.columnNames()
    List<List<Object>> values = new ArrayList<>()

    // header row
    values.add(new ArrayList<Object>(headers))

    // data rows
    matrix.each { Row it ->
      List<Object> row = new ArrayList<>(headers.size())
      it.each { v ->
        row.add(toCell(v))
      }
      values.add(row)
    }

    // 3) Write all values starting at A1
    ValueRange vr = new ValueRange()
        .setRange("${sheetName}!A1")
        .setMajorDimension("ROWS")
        .setValues(values)

    sheets.spreadsheets().values()
        .update(spreadsheetId, "${sheetName}!A1", vr)
        .setValueInputOption("RAW") // don't coerce; write exact values/strings
        .execute()

    return spreadsheetId
  }

  private static String sanitizeSheetName(String name) {
    // Google Sheets sheet names cannot contain: : \ / ? * [ ]
    String s = name.replaceAll('[:\\\\/?*\\[\\]]', ' ')
    if (s.length() > 100) s = s.substring(0, 100)
    return s.trim().isEmpty() ? "Sheet1" : s
  }

  private static Object toCell(Object v) {
    if (v == null) return null
    if (v instanceof Number || v instanceof Boolean) return v
    // Dates/LocalDates/etc. are written as ISO strings unless you convert them to serial numbers yourself.
    return String.valueOf(v)
  }
}
