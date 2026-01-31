package se.alipsa.matrix.gsheets

import com.google.auth.oauth2.GoogleCredentials
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Exports Matrix data to Google Sheets spreadsheets.
 *
 * @deprecated Use {@link GsheetsWriter} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code GsExporter.exportSheet(matrix)} → {@code GsheetsWriter.write(matrix)}</li>
 *   <li>{@code GsExporter.exportSheet(matrix, creds, convertNulls, convertDates)} → {@code GsheetsWriter.write(matrix, creds, convertNulls, convertDates)}</li>
 * </ul>
 *
 * <p>This class creates new Google Spreadsheets from Matrix objects. The exported spreadsheet
 * will have a single sheet containing the matrix data with column names as the first row.
 *
 * <h3>Authentication</h3>
 * If no credentials are provided, the exporter will attempt to use Application Default Credentials (ADC).
 * For interactive authentication, use {@link BqAuthenticator#authenticate()}.
 *
 * <h3>Setup Requirements</h3>
 * Before using the exporter, ensure your Google Cloud project has the required APIs enabled:
 * <pre><code>
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
 * </code></pre>
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
 * // Export to Google Sheets
 * String spreadsheetId = GsExporter.exportSheet(employees)
 * println "View at: https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
 *
 * // Export with date conversion
 * Matrix withDates = Matrix.builder("Sales Data")
 *     .data(date: [LocalDate.now()], amount: [1000])
 *     .types([LocalDate, BigDecimal])
 *     .build()
 *
 * String id = GsExporter.exportSheet(
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
 * @see GsheetsWriter
 * @see GsImporter
 * @see GsConverter
 * @since 0.1.0
 */
@Deprecated
@CompileStatic
class GsExporter {

  /**
   * Creates a new Google Spreadsheet and exports the Matrix data to it.
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
   * String id = GsExporter.exportSheet(data)
   * // Creates spreadsheet titled "Q1 Sales" with sheet "Q1 Sales"
   * }</pre>
   *
   * @param matrix The Matrix to export (must not be null, must have at least one column and one row)
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @param convertNullsToEmptyString If true, null values become empty strings; if false, remain null
   * @param convertDatesToSerial If true, date/time types are converted to Google Sheets serial numbers;
   *                             if false, they are written as ISO-8601 strings
   * @return The spreadsheet ID of the created spreadsheet (use with
   *         https://docs.google.com/spreadsheets/d/{spreadsheetId}/edit)
   * @throws IllegalArgumentException if matrix is null, has no columns, or has no rows
   * @throws SheetOperationException if spreadsheet creation or data writing fails
   * @see GsConverter#asSerial(LocalDate)
   * @see GsConverter#asSerial(LocalDateTime)
   * @deprecated Use {@link GsheetsWriter#write(Matrix, GoogleCredentials, boolean, boolean)} instead
   * @since 0.1.0
   */
  @Deprecated
  static String exportSheet(Matrix matrix, GoogleCredentials credentials = null, boolean convertNullsToEmptyString = true, boolean convertDatesToSerial = false) {
    GsheetsWriter.write(matrix, credentials, convertNullsToEmptyString, convertDatesToSerial)
  }

}
