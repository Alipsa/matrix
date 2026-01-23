package se.alipsa.matrix.gsheets

import com.google.auth.oauth2.GoogleCredentials
import se.alipsa.matrix.core.Matrix
import groovy.transform.CompileStatic

/**
 * Reads data from Google Sheets into Matrix objects.
 *
 * <p>This class provides methods to read data from Google Spreadsheets into Matrix format.
 * It supports both formatted values (as displayed in Google Sheets) and raw unformatted values.
 *
 * <h3>Authentication</h3>
 * If no credentials are provided, the reader will attempt to use Application Default Credentials (ADC).
 * For interactive authentication, use {@link BqAuthenticator#authenticate()}.
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Read formatted values (dates as strings, numbers as displayed)
 * Matrix data = GsheetsReader.read(
 *     "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",  // spreadsheet ID
 *     "Sheet1!A1:D10",                                    // range in A1 notation
 *     true                                                // use first row as column names
 * )
 *
 * // Read with custom credentials
 * GoogleCredentials creds = GoogleCredentials.fromStream(...)
 * Matrix data = GsheetsReader.read(spreadsheetId, range, true, creds)
 *
 * // Read raw unformatted values (e.g., dates as serial numbers)
 * Matrix raw = GsheetsReader.readAsObject(spreadsheetId, range, true, null, true)
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
 * @see GsheetsWriter
 * @see GsConverter
 */
@CompileStatic
class GsheetsReader {

  /**
   * Reads data from a Google Sheets spreadsheet as formatted strings.
   *
   * <p>This is the primary read method. It retrieves values as they appear in Google Sheets
   * (formatted values), converting everything to strings. This is useful when you want to
   * preserve the exact display format from the spreadsheet.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * Matrix data = GsheetsReader.read(
   *     "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms",
   *     "Sheet1!A1:D10",
   *     true  // first row contains column names
   * )
   * }</pre>
   *
   * @param spreadsheetId The Google Sheets spreadsheet ID (from the URL between /d/ and /edit)
   * @param range The range in A1 notation (e.g., "Sheet1!A1:D10" or "A1:D10")
   * @param firstRowAsColumnNames If true, uses the first row as column names; otherwise generates c1, c2, etc.
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @return A Matrix containing the imported data as strings
   * @throws IllegalArgumentException if spreadsheetId or range is null/empty or malformed
   * @throws IOException if the Google Sheets API call fails
   * @see #readAsObject(String, String, boolean, GoogleCredentials, boolean)
   */
  static Matrix read(String spreadsheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
    return GsImporter.importSheet(spreadsheetId, range, firstRowAsColumnNames, credentials)
  }

  /**
   * Reads data from a Google Sheets spreadsheet as unformatted objects.
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
   * // Read raw values
   * Matrix raw = GsheetsReader.readAsObject(
   *     spreadsheetId,
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
   * @param spreadsheetId The Google Sheets spreadsheet ID (from the URL between /d/ and /edit)
   * @param range The range in A1 notation (e.g., "Sheet1!A1:D10")
   * @param firstRowAsColumnNames If true, uses first row as column names
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @param convertEmptyToNull If true, converts empty string cells to null
   * @return A Matrix containing the unformatted values
   * @throws IllegalArgumentException if spreadsheetId or range is null/empty or malformed
   * @throws IOException if the Google Sheets API call fails
   * @see GsConverter
   */
  static Matrix readAsObject(String spreadsheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null, boolean convertEmptyToNull = false) {
    return GsImporter.importSheetAsObject(spreadsheetId, range, firstRowAsColumnNames, credentials, convertEmptyToNull)
  }

  /**
   * Reads data from a Google Sheets spreadsheet as formatted strings.
   *
   * <p>This is an alias for {@link #read(String, String, boolean, GoogleCredentials)} and behaves identically.
   *
   * @param spreadsheetId The Google Sheets spreadsheet ID
   * @param range The range in A1 notation
   * @param firstRowAsColumnNames If true, uses first row as column names
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @return A Matrix containing the data as strings
   * @throws IllegalArgumentException if spreadsheetId or range is null/empty or malformed
   * @throws IOException if the Google Sheets API call fails
   */
  static Matrix readAsStrings(String spreadsheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
    return GsImporter.importSheetAsStrings(spreadsheetId, range, firstRowAsColumnNames, credentials)
  }
}
