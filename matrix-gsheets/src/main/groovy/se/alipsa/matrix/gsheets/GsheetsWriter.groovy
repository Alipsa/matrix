package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import groovy.transform.PackageScope
import se.alipsa.matrix.core.Matrix
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Row

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

import static se.alipsa.matrix.gsheets.BqAuthenticator.authenticate
import static se.alipsa.matrix.gsheets.BqAuthenticator.getSCOPES

/**
 * Writes Matrix data to Google Sheets spreadsheets.
 *
 * <p>This class creates new Google Spreadsheets from Matrix objects. The created spreadsheet
 * will have a single sheet containing the matrix data with column names as the first row.
 *
 * <h3>Authentication</h3>
 * If no credentials are provided, the writer will attempt to use Application Default Credentials (ADC).
 * For interactive authentication, use {@link BqAuthenticator#authenticate()}.
 *
 * <h3>Setup Requirements</h3>
 * Before using the writer, ensure your Google Cloud project has the required APIs enabled:
 * <pre>
 * # Set your project ID
 * PROJECT_ID=$(gcloud config get-value project 2&gt; /dev/null)
 *
 * # Set quota project for ADC
 * gcloud auth application-default set-quota-project $PROJECT_ID
 *
 * # Enable required APIs
 * gcloud services enable sheets.googleapis.com drive.googleapis.com --project=$PROJECT_ID
 *
 * # Verify APIs are enabled
 * gcloud services list --enabled --project=$PROJECT_ID | grep -E 'sheets|drive'
 * </pre>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Create a matrix
 * Matrix employees = Matrix.builder("Employee Data")
 *     .data(
 *         emp_id: [1, 2, 3],
 *         name: ["Alice", "Bob", "Charlie"],
 *         salary: [50000, 60000, 70000]
 *     )
 *     .types([Integer, String, BigDecimal])
 *     .build()
 *
 * // Write to Google Sheets
 * String spreadsheetId = GsheetsWriter.write(employees)
 * println "View at: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
 *
 * // Write with date conversion
 * Matrix withDates = Matrix.builder("Sales Data")
 *     .data(date: [LocalDate.now()], amount: [1000])
 *     .types([LocalDate, BigDecimal])
 *     .build()
 *
 * String id = GsheetsWriter.write(
 *     withDates,
 *     null,   // use default credentials
 *     true,   // convert nulls to empty strings
 *     true    // convert dates to serial numbers
 * )
 * }</pre>
 *
 * <h3>Data Conversion</h3>
 * <ul>
 * <li><strong>Nulls:</strong> Converted to empty strings if convertNullsToEmptyString=true</li>
 * <li><strong>Dates:</strong> Stored as ISO strings unless convertDatesToSerial=true</li>
 * <li><strong>Numbers/Booleans:</strong> Written as-is</li>
 * <li><strong>Other types:</strong> Converted to strings via String.valueOf()</li>
 * </ul>
 *
 * @see GsheetsReader
 * @see GsConverter
 */
@CompileStatic
class GsheetsWriter {

  /**
   * Creates a new Google Spreadsheet and writes the Matrix data to it.
   *
   * <p>The spreadsheet title and sheet name are derived from the Matrix name. If the matrix
   * has no name, a timestamp-based name is generated. Sheet names are sanitized to remove
   * invalid characters (: \ / ? * [ ]) and truncated to 100 characters if needed.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * Matrix data = Matrix.builder("Q1 Sales")
   *     .data(
   *         month: ["Jan", "Feb", "Mar"],
   *         revenue: [10000, 12000, 15000]
   *     )
   *     .build()
   *
   * String id = GsheetsWriter.write(data)
   * // Creates spreadsheet titled "Q1 Sales" with sheet "Q1 Sales"
   * }</pre>
   *
   * @param matrix The Matrix to write (must not be null, must have at least one column and one row)
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @param convertNullsToEmptyString If true, null values become empty strings; if false, remain null
   * @param convertDatesToSerial If true, date/time types are converted to Google Sheets serial numbers;
   *                             if false, they are written as ISO-8601 strings
   * @return The spreadsheet ID of the created spreadsheet (use with
   *         https://docs.google.com/spreadsheets/d/{spreadsheetId}/edit)
   * @throws IllegalArgumentException if matrix is null, has no columns, or has no rows
   * @throws SheetOperationException if spreadsheet creation or data writing fails
   * @see GsConverter#asSerial(java.time.LocalDate)
   * @see GsConverter#asSerial(java.time.LocalDateTime)
   */
  static String write(Matrix matrix, boolean convertNullsToEmptyString = true, boolean convertDatesToSerial = false) {
    return write(matrix, null, convertNullsToEmptyString, convertDatesToSerial)
  }

  static String write(Matrix matrix, GoogleCredentials credentials, boolean convertNullsToEmptyString = true, boolean convertDatesToSerial = false) {
    if (matrix == null) {
      throw new IllegalArgumentException("matrix must not be null")
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException("matrix must have at least one column")
    }
    if (matrix.rowCount() == 0) {
      throw new IllegalArgumentException("matrix must have at least one row")
    }

    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    // Need write scope for creating/updating spreadsheets
    if (credentials == null) {
      credentials = authenticate(SCOPES)
    }
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)

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

    Spreadsheet created
    try {
      created = sheets.spreadsheets()
          .create(requestBody)
          .setFields("spreadsheetId") // we only need the id here
          .execute()
    } catch (IOException e) {
      throw new SheetOperationException("create spreadsheet", "Failed to create spreadsheet '${spreadsheetTitle}': ${e.message}")
    }

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
        row.add(toCell(v, convertNullsToEmptyString, convertDatesToSerial))
      }
      values.add(row)
    }

    // 3) Write all values starting at A1
    ValueRange vr = new ValueRange()
        .setRange("${sheetName}!A1")
        .setMajorDimension("ROWS")
        .setValues(values)

    try {
      sheets.spreadsheets().values()
          .update(spreadsheetId, "${sheetName}!A1", vr)
          .setValueInputOption("RAW") // don't coerce; write exact values/strings
          .execute()
    } catch (IOException e) {
      throw new SheetOperationException("write data", spreadsheetId, e)
    }

    return spreadsheetId
  }

  @PackageScope
  static String sanitizeSheetName(String name) {
    // Google Sheets sheet names cannot contain: : \ / ? * [ ]
    String s = name.replaceAll('[:\\\\/?*\\[\\]]', ' ')
    if (s.length() > 100) s = s.substring(0, 100)
    return s.trim().isEmpty() ? "Sheet1" : s
  }

  @PackageScope
  static Object toCell(Object v, boolean convertNullsToEmptyString, boolean convertDatesToSerial) {
    if (v == null) return convertNullsToEmptyString ? '' : null
    if (v instanceof Number || v instanceof Boolean) return v
    // Dates/LocalDates/etc. are written as ISO strings unless you convert them to serial numbers yourself.
    if (convertDatesToSerial) {
      if (v instanceof LocalDate) return GsConverter.asSerial(v as LocalDate)
      if (v instanceof LocalDateTime) return GsConverter.asSerial(v as LocalDateTime)
      if (v instanceof Date) return GsConverter.asSerial(v as Date)
      if (v instanceof LocalTime) return GsConverter.asSerial(v as LocalTime)
    }
    return String.valueOf(v)
  }
}
