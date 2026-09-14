package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

import se.alipsa.matrix.core.util.Logger

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAccumulator
import java.util.concurrent.atomic.LongAdder
import java.util.regex.Matcher

/**
 * Utility methods for working with Google Sheets.
 */
class GsUtil {

  private static final Logger log = Logger.getLogger(GsUtil)

  private static final String SPREADSHEET_ID_ERROR = 'spreadsheetId must not be null or empty'
  private static final String RANGE_ERROR = 'range must not be null or empty'
  private static final String COLON = ':'
  private static final String SHEET_SEPARATOR = '!'
  private static final String SINGLE_CELL_PATTERN = '^[A-Z]{1,3}\\d+$'
  private static final String ENDPOINT_PATTERN = '^([A-Z]{1,3})(\\d*)$'
  private static final String ROW_ONLY_PATTERN = '^\\d+$'
  // -1 sentinel: used as the split() limit that keeps trailing empty parts (so e.g. 'A1:' is
  // rejected instead of collapsing to its single valid endpoint) and as a "not found" marker
  private static final int NOT_FOUND = -1
  private static final int RANGE_ENDPOINT_COUNT = 2
  private static final String INVALID_RANGE_ERROR = "Invalid range format: '%s'. Expected A1 notation like 'Sheet1!A1:D10', 'A1:D10', or 'Sheet1!A1'"
  private static final String SINGLE_QUOTE = "'"
  private static final int MAX_SHEET_NAME_LENGTH = 100
  // Decimal values with more significant digits are outside the conservative precision
  // envelope we allow for Google Sheets' IEEE-754 double storage.
  private static final int MAX_DOUBLE_SAFE_PRECISION = 15
  private static final BigInteger MAX_EXACT_DOUBLE_INTEGER = 9007199254740992G

  static void deleteSheet(String spreadsheetId) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException(SPREADSHEET_ID_ERROR)
    }

    def scopes = ['https://www.googleapis.com/auth/drive'] + GsAuthenticator.SCOPES
    def credentials = GsAuthenticator.authenticate(scopes)
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    def driveService = new Drive.Builder(transport, gsonFactory, cred)
        .setApplicationName('Matrix GSheets')
        .build()

    deleteSheet(spreadsheetId, driveService)
  }

  /**
   * Delete a spreadsheet using a provided Drive service.
   * This overload is useful for testing with mocked services.
   *
   * @param spreadsheetId The ID of the spreadsheet to delete
   * @param driveService The Drive service to use
   * @throws SheetOperationException if the delete operation fails
   */
  static void deleteSheet(String spreadsheetId, Drive driveService) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException(SPREADSHEET_ID_ERROR)
    }
    if (driveService == null) {
      throw new IllegalArgumentException('driveService must not be null')
    }

    try {
      // Perform the delete operation on the Drive file using its ID
      driveService.files().delete(spreadsheetId).execute()
      log.info "Successfully deleted spreadsheet with ID: ${spreadsheetId}"
    } catch (IOException e) {
      throw new SheetOperationException('delete', spreadsheetId, e)
    }
  }

  /**
   * Calculates the number of columns in a given A1-style range string.
   *
   * <p>Only cell-based ranges are supported: a single cell ({@code A1}), a cell span
   * ({@code A1:D10}), or open-ended column spans ({@code A:D}, {@code A1:D}). Row-only
   * ranges ({@code 1:5}) and bare sheet names ({@code Sheet1}) are rejected because the
   * column count is undefined. Use {@link #validateWriteRange(String)} for write-side
   * validation that also accepts row-only ranges.
   *
   * @param range The range string, e.g., 'Arkiv!B2:H100' or 'A1:C10'.
   * @return The number of columns in the range.
   */
  static int columnCountForRange(String range) {
    if (range == null || range.trim().isEmpty()) {
      throw new IllegalArgumentException(RANGE_ERROR)
    }

    String cellRange = splitSheetAndCells(range)[1].toUpperCase(Locale.ROOT)

    // -1 keeps trailing empty parts so that e.g. 'A1:' is rejected instead of collapsing
    // to the single valid endpoint 'A1'
    String[] cellParts = cellRange.split(COLON, NOT_FOUND)
    if (cellParts.size() == 1) {
      if (!cellParts[0].matches(SINGLE_CELL_PATTERN)) {
      throw invalidRange(range)
      }
      return 1
    }
    if (cellParts.size() != RANGE_ENDPOINT_COUNT) {
      throw invalidRange(range)
    }

    Matcher startMatcher = (cellParts[0] =~ ENDPOINT_PATTERN) as Matcher
    Matcher endMatcher = (cellParts[1] =~ ENDPOINT_PATTERN) as Matcher
    if (!startMatcher.matches() || !endMatcher.matches()) {
      throw invalidRange(range)
    }

    String startColumnLetters = startMatcher.group(1)
    String endColumnLetters = endMatcher.group(1)

    // Convert column letters to numerical indices
    int startColIndex = asColumnNumber(startColumnLetters)
    int endColIndex = asColumnNumber(endColumnLetters)

    // Calculate the number of columns
    endColIndex - startColIndex + 1
  }

  /**
   * Converts a column letter string (e.g., 'A', 'Z', 'AA') to its numerical index (1-based).
   * @param colLetters The column letter string.
   * @return The 1-based column index.
   */
  static int asColumnNumber(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException('Column name must not be null or empty')
    }
    String colName = name.toUpperCase()
    // Validate that it only contains letters A-Z
    if (!colName.matches('[A-Z]+')) {
      throw new IllegalArgumentException("Invalid column name: '${name}'. Must contain only letters A-Z")
    }
    int number = 0
    for (int i = 0; i < colName.length(); i++) {
      number = number * 26 + (colName.charAt(i) - ('A' as char - 1))
    }
    number
  }

  static List<String> getSheetNames(String spreadsheetId, GoogleCredentials credentials = null) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException(SPREADSHEET_ID_ERROR)
    }

    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    if (credentials == null) {
      credentials = GsAuthenticator.authenticate(GsAuthenticator.SCOPE_SHEETS_READONLY)
    }

    def sheetsService = new Sheets.Builder(
        transport,
        gsonFactory,
        new HttpCredentialsAdapter(credentials))
        .setApplicationName('Groovy Sheets Reader')
        .build()

    getSheetNames(spreadsheetId, sheetsService)
  }

  /**
   * Get sheet names from a spreadsheet using a provided Sheets service.
   * This overload is useful for testing with mocked services.
   *
   * @param spreadsheetId The ID of the spreadsheet
   * @param sheetsService The Sheets service to use
   * @return List of sheet names
   */
  static List<String> getSheetNames(String spreadsheetId, Sheets sheetsService) throws IOException {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException(SPREADSHEET_ID_ERROR)
    }
    if (sheetsService == null) {
      throw new IllegalArgumentException('sheetsService must not be null')
    }

    try {
      // Fetch the spreadsheet metadata (this includes the list of sheets)
      def spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()

      List<String> names = []
      spreadsheet.getSheets().each { sheet ->
        names.add(sheet.getProperties().getTitle())
      }

      names
    } catch (IOException e) {
      log.error("Failed to retrieve sheet names for spreadsheetId '$spreadsheetId': ${e.message}", e)
      throw e
    }
  }

  static String sanitizeSheetName(String name) {
    // Google Sheets sheet names cannot contain: : \ / ? * [ ]
    if (name == null) {
      return 'Sheet1'
    }
    String s = name.replaceAll('[:\\\\/?*\\[\\]]', ' ')
    if (s.length() > MAX_SHEET_NAME_LENGTH) {
      s = s.substring(0, MAX_SHEET_NAME_LENGTH)
    }
    s.trim().isEmpty() ? 'Sheet1' : s
  }

  /**
   * Quotes a sheet name for use in an A1 range reference (e.g. {@code 'My Sheet'!A1:D10}).
   * Google requires quoting for names containing spaces or special characters, and quoting
   * is always valid even when not strictly required, so this is applied unconditionally.
   * Embedded single quotes are doubled per Google's escaping rule.
   *
   * @see <a href="https://developers.google.com/workspace/sheets/api/guides/concepts">Sheets API concepts</a>
   */
  static String quoteSheetName(String sheetName) {
    SINGLE_QUOTE + sheetName.replace(SINGLE_QUOTE, SINGLE_QUOTE + SINGLE_QUOTE) + SINGLE_QUOTE
  }

  /**
   * Converts a matrix value to a Google Sheets cell value. Integral values outside the exact
   * IEEE-754 range are rejected; other third-party {@link Number} implementations are passed
   * through unchecked.
   */
  static Object toCell(Object v, boolean convertNullsToEmptyString, boolean convertDatesToSerial) {
    if (v == null) {
      return convertNullsToEmptyString ? '' : null
    }
    if (v in BigDecimal) {
      BigDecimal bd = (BigDecimal) v
      BigDecimal stripped = bd.stripTrailingZeros()
      if (stripped.scale() <= 0) {
        requireExactDouble(stripped.toBigIntegerExact(), v)
        // Preserve the original scale for writer-side number formatting, e.g. 729.0.
        return bd
      }
      if (bd.precision() > MAX_DOUBLE_SAFE_PRECISION) {
        throw new IllegalArgumentException(
            "BigDecimal value ${bd} has ${bd.precision()} significant digits, which is outside " +
            'matrix-gsheets\' conservative exact-write guard for Google Sheets\' IEEE-754 ' +
            "double storage (${MAX_DOUBLE_SAFE_PRECISION} significant digits). Round the value " +
            'before writing, or convert it to a String to preserve it verbatim as text.'
        )
      }
      return bd
    }
    if (v instanceof Long || v instanceof BigInteger) {
      requireExactDouble(v instanceof BigInteger ? v : BigInteger.valueOf(v.longValue()), v)
      return v
    }
    if (v instanceof AtomicLong || v instanceof LongAdder || v instanceof LongAccumulator) {
      requireExactDouble(BigInteger.valueOf(v.longValue()), v)
      return v.longValue()
    }
    if (v in Number || v in Boolean) {
      return v
    }
    // Dates/LocalDates/etc. are written as ISO strings unless you convert them to serial numbers yourself.
    if (convertDatesToSerial) {
      if (v in LocalDate) {
        return GsConverter.asSerial(v as LocalDate)
      }
      if (v in LocalDateTime) {
        return GsConverter.asSerial(v as LocalDateTime)
      }
      if (v in Date) {
        return GsConverter.asSerial(v as Date)
      }
      if (v in LocalTime) {
        return GsConverter.asSerial(v as LocalTime)
      }
    }
    String.valueOf(v)
  }

  /**
   * Validates a range for read operations: the range must be a cell-based A1 range from
   * which a column count can be derived. Row-only ranges ({@code Sheet1!1:5}) and bare
   * sheet names ({@code Sheet1}) are rejected, even though the Sheets API accepts them,
   * because the reader needs a defined column count.
   *
   * @param range The A1 range to validate
   * @throws IllegalArgumentException if the range is null, empty, or not a cell-based A1 range
   */
  static void validateRange(String range) {
    if (range == null || range.trim().isEmpty()) {
      throw new IllegalArgumentException(RANGE_ERROR)
    }
    columnCountForRange(range)
  }

  /**
   * Validates a range for write operations. Accepts everything {@link #validateRange(String)}
   * accepts, plus row-only ranges such as {@code Sheet1!1:5} whose column count is undefined
   * but which the Sheets API accepts for writes.
   *
   * @param range The A1 range to validate
   * @throws IllegalArgumentException if the range is null, empty, or not valid A1 notation
   */
  static void validateWriteRange(String range) {
    if (range == null || range.trim().isEmpty()) {
      throw new IllegalArgumentException(RANGE_ERROR)
    }
    try {
      columnCountForRange(range)
      return
    } catch (IllegalArgumentException ignored) {
      // not a cell-based range; row-only ranges are still valid for writes
    }
    String cellRange = splitSheetAndCells(range)[1].toUpperCase(Locale.ROOT)
    String[] cellParts = cellRange.split(COLON, NOT_FOUND)
    boolean rowOnly = cellParts.size() == 1
        ? cellParts[0].matches(ROW_ONLY_PATTERN)
        : cellParts.size() == RANGE_ENDPOINT_COUNT && cellParts[0].matches(ROW_ONLY_PATTERN) && cellParts[1].matches(ROW_ONLY_PATTERN)
    if (!rowOnly) {
      throw invalidRange(range)
    }
  }

  static void validateSheetId(String sheetId) {
    if (sheetId == null || sheetId.trim().isEmpty()) {
      throw new IllegalArgumentException('sheetId must not be null or empty')
    }
  }

  static List<Object> fillListToSize(List<Object> list, int desiredSize) {
    if (list.size() >= desiredSize) {
      return list
    }

    int currentSize = list.size()
    for (int i = currentSize; i < desiredSize; i++) {
      list.add(null)
    }
    list
  }

  static List<String> buildHeader(int ncol, List<Object> firstRow) {
    List<String> headers  = []
    int rowSize = firstRow.size()
    for (int i = 0; i < ncol; i++) {
      def val = i < rowSize ? firstRow.get(i) : null
      def colName
      if (val == null || val.toString().trim().isEmpty()) {
        colName = 'c' + (i + 1)
      } else {
        colName = String.valueOf(val)
      }
      headers << colName
    }
    headers
  }

  /**
   * Splits an A1 range into its raw sheet-name prefix and cell portion. Quotes and escaped
   * quotes in the sheet prefix are preserved; callers validate the resulting cell portion.
   *
   * @return a two-element array containing [sheetPart, cellPart], with a null sheetPart when
   *         no sheet prefix is present
   */
  static String[] splitSheetAndCells(String range) {
    if (range?.startsWith(SINGLE_QUOTE)) {
      int closingQuote = NOT_FOUND
      for (int i = 1; i < range.length(); i++) {
        if (range.charAt(i) == SINGLE_QUOTE.charAt(0)) {
          if (i + 1 < range.length() && range.charAt(i + 1) == SINGLE_QUOTE.charAt(0)) {
            i++
          } else {
            closingQuote = i
            break
          }
        }
      }
      if (closingQuote < 0) {
        throw invalidRange(range)
      }
      int separator = range.indexOf(SHEET_SEPARATOR, closingQuote + 1)
      if (separator >= 0) {
        return [range.substring(0, separator), range.substring(separator + 1)] as String[]
      }
      return [null, range] as String[]
    }
    int separator = range.indexOf(SHEET_SEPARATOR)
    separator >= 0 ? [range.substring(0, separator), range.substring(separator + 1)] as String[] : [null, range] as String[]
  }

  private static IllegalArgumentException invalidRange(String range) {
    new IllegalArgumentException(String.format(INVALID_RANGE_ERROR, range))
  }

  private static void requireExactDouble(BigInteger value, Object original) {
    if (value.abs() > MAX_EXACT_DOUBLE_INTEGER) {
      throw new IllegalArgumentException(
          "integer value ${original} exceeds the largest integer Google Sheets can store exactly " +
          '(Sheets stores all numbers as IEEE-754 doubles, exact for integers only up to ' +
          "${MAX_EXACT_DOUBLE_INTEGER}). Round the value, or convert it to a String to preserve it verbatim as text."
      )
    }
  }

}
