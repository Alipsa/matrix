package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import se.alipsa.matrix.core.*
import groovy.transform.CompileStatic

/**
 * Imports data from Google Sheets into Matrix objects.
 *
 * @deprecated Use {@link GsheetsReader} instead. This class will be removed in v2.0.
 * <p>Migration guide:</p>
 * <ul>
 *   <li>{@code GsImporter.importSheet(sheetId, range, firstRow)} → {@code GsheetsReader.read(sheetId, range, firstRow)}</li>
 *   <li>{@code GsImporter.importSheetAsObject(sheetId, range, firstRow, creds, convertEmpty)} → {@code GsheetsReader.readAsObject(sheetId, range, firstRow, creds, convertEmpty)}</li>
 *   <li>{@code GsImporter.importSheetAsStrings(sheetId, range, firstRow)} → {@code GsheetsReader.readAsStrings(sheetId, range, firstRow)}</li>
 * </ul>
 *
 * <p>This class provides methods to import data from Google Spreadsheets into Matrix format.
 * It supports both formatted values (as displayed in Google Sheets) and raw unformatted values.
 *
 * <h3>Authentication</h3>
 * If no credentials are provided, the importer will attempt to use Application Default Credentials (ADC).
 * For interactive authentication, use {@link BqAuthenticator#authenticate()}.
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Import formatted values (dates as strings, numbers as displayed)
 * Matrix data = GsImporter.importSheet(
 *     "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",  // spreadsheet ID
 *     "Sheet1!A1:D10",                                    // range in A1 notation
 *     true                                                // use first row as column names
 * )
 *
 * // Import with custom credentials
 * GoogleCredentials creds = GoogleCredentials.fromStream(...)
 * Matrix data = GsImporter.importSheet(sheetId, range, true, creds)
 *
 * // Import raw unformatted values (e.g., dates as serial numbers)
 * Matrix raw = GsImporter.importSheetAsObject(sheetId, range, true, null, true)
 * }</pre>
 *
 * <h3>Google Sheets Quirks</h3>
 * <ul>
 * <li>Empty cells are represented as missing values in the API response</li>
 * <li>Dates are stored as serial numbers (days since 1899-12-30)</li>
 * <li>Google Sheets has a 1900 leap year bug (treats 1900 as leap year)</li>
 * <li>Trailing empty columns/rows may be omitted from the API response</li>
 * </ul>
 *
 * @see GsheetsReader
 * @see GsExporter
 * @see GsConverter
 * @since 0.1.0
 */
@Deprecated
@CompileStatic
class GsImporter {

  /**
   * Imports data from a Google Sheets spreadsheet as formatted strings.
   *
   * <p>This is the primary import method. It retrieves values as they appear in Google Sheets
   * (formatted values), converting everything to strings. This is useful when you want to
   * preserve the exact display format from the spreadsheet.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * Matrix data = GsImporter.importSheet(
   *     "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",
   *     "Sheet1!A1:D10",
   *     true  // first row contains column names
   * )
   * }</pre>
   *
   * @param sheetId The Google Sheets spreadsheet ID (from the URL between /d/ and /edit)
   * @param range The range in A1 notation (e.g., "Sheet1!A1:D10" or "A1:D10")
   * @param firstRowAsColumnNames If true, uses the first row as column names; otherwise generates c1, c2, etc.
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @return A Matrix containing the imported data as strings
   * @throws IllegalArgumentException if sheetId or range is null/empty or malformed
   * @throws IOException if the Google Sheets API call fails
   * @see #importSheetAsObject(String, String, boolean, GoogleCredentials, boolean)
   * @deprecated Use {@link GsheetsReader#read(String, String, boolean, GoogleCredentials)} instead
   * @since 0.1.0
   */
  @Deprecated
  static Matrix importSheet(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
    GsheetsReader.readAsStrings(sheetId, range, firstRowAsColumnNames, credentials)
  }

  /**
   * Imports data from a Google Sheets spreadsheet as unformatted objects.
   *
   * <p>This method retrieves the raw, unformatted values from Google Sheets. Useful when you need
   * the actual underlying values rather than their display format:
   * <ul>
   * <li>Dates are returned as serial numbers (days since 1899-12-30)</li>
   * <li>Times are returned as fractional days</li>
   * <li>Numbers are returned as their exact values</li>
   * <li>Formulas return their calculated results</li>
   * </ul>
   *
   * <p>Use {@link GsConverter} to convert serial numbers to Java date/time types.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * // Import raw values
   * Matrix raw = GsImporter.importSheetAsObject(
   *     sheetId,
   *     "Data!A1:F100",
   *     true,   // first row as column names
   *     null,   // use default credentials
   *     true    // convert empty strings to null
   * )
   *
   * // Convert date column from serial numbers to LocalDate
   * raw = raw.convert(
   *     Converter.of("date_column", LocalDate, GsConverter.&asLocalDate)
   * )
   * }</pre>
   *
   * @param sheetId The Google Sheets spreadsheet ID (from the URL between /d/ and /edit)
   * @param range The range in A1 notation (e.g., "Sheet1!A1:D10")
   * @param firstRowAsColumnNames If true, uses first row as column names
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @param convertEmptyToNull If true, converts empty string cells to null
   * @return A Matrix containing the unformatted values
   * @throws IllegalArgumentException if sheetId or range is null/empty or malformed
   * @throws IOException if the Google Sheets API call fails
   * @see GsConverter
   * @deprecated Use {@link GsheetsReader#readAsObject(String, String, boolean, GoogleCredentials, boolean)} instead
   * @since 0.1.0
   */
  @Deprecated
  static Matrix importSheetAsObject(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null, boolean convertEmptyToNull = false) {
    GsheetsReader.readAsObject(sheetId, range, firstRowAsColumnNames, credentials, convertEmptyToNull)
  }

  /**
   * @deprecated Use {@link GsheetsReader#readAsStrings(String, String, boolean, GoogleCredentials)} instead
   */
  @Deprecated
  static Matrix importSheetAsStrings(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
    GsheetsReader.readAsStrings(sheetId, range, firstRowAsColumnNames, credentials)
  }
}
