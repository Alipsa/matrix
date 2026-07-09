package se.alipsa.matrix.gsheets

import static se.alipsa.matrix.gsheets.GsAuthenticator.authenticate
import static se.alipsa.matrix.gsheets.GsAuthenticator.getSCOPES

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.GridRange
import com.google.api.services.sheets.v4.model.NumberFormat
import com.google.api.services.sheets.v4.model.RepeatCellRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row

import java.time.LocalDateTime

/**
 * Writes Matrix data to Google Sheets spreadsheets.
 *
 * <p>This class creates new Google Spreadsheets from Matrix objects. The created spreadsheet
 * will have a single sheet containing the matrix data with column names as the first row.
 *
 * <h3>Authentication</h3>
 * If no credentials are provided, the writer will attempt to use Application Default Credentials (ADC).
 * For interactive authentication, use {@link GsAuthenticator#authenticate()}.
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
 * Matrix employees = Matrix.builder('Employee Data')
 *     .data(
 *         emp_id: [1, 2, 3],
 *         name: ['Alice', 'Bob', 'Charlie'],
 *         salary: [50000, 60000, 70000]
 *     )
 *     .types([Integer, String, BigDecimal])
 *     .build()
 *
 * // Write to Google Sheets
 * String spreadsheetId = GsheetsWriter.write(employees)
 * println "View at: ${GsheetsWriter.spreadsheetUrl(spreadsheetId)}"
 *
 * // Write with date conversion
 * Matrix withDates = Matrix.builder('Sales Data')
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
class GsheetsWriter {

  private static final String MATRIX_NULL_ERROR = 'matrix must not be null'
  private static final String MATRIX_NO_COLUMNS_ERROR = 'matrix must have at least one column'
  private static final String MATRIX_NO_ROWS_ERROR = 'matrix must have at least one row'
  private static final String APP_NAME = 'Matrix GSheets'
  private static final String ROWS_DIMENSION = 'ROWS'
  private static final String RAW_INPUT = 'RAW'
  private static final String ZERO_DIGIT = '0'
  private static final String SINGLE_QUOTE = "'"
  private static final String SHEET_NAME_SEPARATOR = '!'
  private static final String APPLY_FORMATS_ERROR = 'apply number formats'
  private static final int RANGE_SPLIT_LIMIT = 2

  /**
   * Creates a new Google Spreadsheet and writes the Matrix data to it.
   *
   * <p>The spreadsheet title and sheet name are derived from the Matrix name. If the matrix
   * has no name, a timestamp-based name is generated. Sheet names are sanitized to remove
   * invalid characters (: \ / ? * [ ]) and truncated to 100 characters if needed.
   *
   * <p><strong>Example:</strong>
   * <pre>{@code
   * Matrix data = Matrix.builder('Q1 Sales')
   *     .data(
   *         month: ['Jan', 'Feb', 'Mar'],
   *         revenue: [10000, 12000, 15000]
   *     )
   *     .build()
   *
   * String id = GsheetsWriter.write(data)
   * // Creates spreadsheet titled 'Q1 Sales' with sheet 'Q1 Sales'
   * }</pre>
   *
   * @param matrix The Matrix to write (must not be null, must have at least one column and one row)
   * @param credentials Google Cloud credentials, or null to use Application Default Credentials
   * @param convertNullsToEmptyString If true, null values become empty strings; if false, remain null
   * @param convertDatesToSerial If true, date/time types are converted to Google Sheets serial numbers;
   *                             if false, they are written as ISO-8601 strings
   * @return The spreadsheet ID of the created spreadsheet (open with {@link #spreadsheetUrl(String)})
   * @throws IllegalArgumentException if matrix is null, has no columns, or has no rows
   * @throws SheetOperationException if spreadsheet creation or data writing fails
   * @see GsConverter#asSerial(java.time.LocalDate)
   * @see GsConverter#asSerial(java.time.LocalDateTime)
   */
  static String write(Matrix matrix, boolean convertNullsToEmptyString = true, boolean convertDatesToSerial = false) {
    write(matrix, null, convertNullsToEmptyString, convertDatesToSerial)
  }

  static String write(Matrix matrix, GoogleCredentials credentials, boolean convertNullsToEmptyString = true, boolean convertDatesToSerial = false) {
    if (matrix == null) {
      throw new IllegalArgumentException(MATRIX_NULL_ERROR)
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException(MATRIX_NO_COLUMNS_ERROR)
    }
    if (matrix.rowCount() == 0) {
      throw new IllegalArgumentException(MATRIX_NO_ROWS_ERROR)
    }

    String titleBase = matrix.matrixName ?: "Matrix ${LocalDateTime.now().toString().replace('T', '_')}"
    String sheetName = GsUtil.sanitizeSheetName(titleBase)
    String spreadsheetTitle = titleBase
    // Sheet names containing spaces or special characters must be quoted in A1 notation
    // (e.g. 'Employee Data'!A1); quoting is always valid, so it's applied unconditionally.
    String quotedRange = "${GsUtil.quoteSheetName(sheetName)}!A1"

    // Build the data before creating the spreadsheet so local validation errors cannot
    // leave an empty spreadsheet orphaned in Drive.
    List<List<Object>> values = buildValues(matrix, convertNullsToEmptyString, convertDatesToSerial)

    Sheets sheets = buildSheetsService(credentials)

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
          .setFields('spreadsheetId,sheets.properties.sheetId') // we only need the ids here
          .execute()
    } catch (IOException e) {
      throw new SheetOperationException('create spreadsheet', "Failed to create spreadsheet '${spreadsheetTitle}': ${e.message}")
    }

    String spreadsheetId = created.getSpreadsheetId()
    int sheetId = created.getSheets().get(0).getProperties().getSheetId()

    // 3) Write all values starting at A1
    ValueRange vr = new ValueRange()
        .setRange(quotedRange)
        .setMajorDimension(ROWS_DIMENSION)
        .setValues(values)

    try {
      sheets.spreadsheets().values()
          .update(spreadsheetId, quotedRange, vr)
          .setValueInputOption(RAW_INPUT) // don't coerce; write exact values/strings
          .execute()
    } catch (IOException e) {
      throw new SheetOperationException('write data', spreadsheetId, e)
    }

    // 4) Force explicit decimal places on BigDecimal cells: Sheets' default automatic
    // number format drops trailing zeros (e.g. 729.0 displays as "729"), which would
    // otherwise lose scale information whenever the formatted value is read back.
    // Row offset 1 accounts for the header row; column offset 0 since we always start at A1.
    List<Request> formatRequests = buildNumberFormatRequests(matrix, sheetId, 1, 0)
    if (formatRequests) {
      try {
        sheets.spreadsheets()
            .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(formatRequests))
            .execute()
      } catch (IOException e) {
        throw new SheetOperationException(APPLY_FORMATS_ERROR, spreadsheetId, e)
      }
    }

    spreadsheetId
  }

  /**
   * Builds one {@code RepeatCellRequest} per maximal run of consecutive rows in a column
   * that share the same BigDecimal scale, forcing that many decimal places so Sheets
   * preserves trailing zeros (e.g. 729.0) when the cell is later read as a formatted value.
   * Columns/cells without a positive-scale BigDecimal value are left with Sheets' default
   * formatting.
   *
   * @param rowOffset sheet row (0-based) where the matrix's data rows begin (i.e. after
   *        any header row and after the target range's own starting row)
   * @param colOffset sheet column (0-based) where the matrix's first column begins
   */
  private static List<Request> buildNumberFormatRequests(Matrix matrix, int sheetId, int rowOffset, int colOffset) {
    List<Request> requests = []
    int ncol = matrix.columnCount()
    int nrow = matrix.rowCount()
    for (int c = 0; c < ncol; c++) {
      List column = matrix.column(c)
      Integer runStart = null
      Integer runScale = null
      for (int r = 0; r <= nrow; r++) {
        Object val = r < nrow ? column.get(r) : null
        Integer scale = (val instanceof BigDecimal && ((BigDecimal) val).scale() > 0) ? ((BigDecimal) val).scale() : null
        if (runStart != null && scale == runScale) {
          continue
        }
        if (runStart != null) {
          requests << numberFormatRequest(sheetId, colOffset + c, rowOffset + runStart, rowOffset + r, runScale)
        }
        runStart = scale != null ? r : null
        runScale = scale
      }
    }
    requests
  }

  /**
   * True if the matrix has at least one BigDecimal cell whose scale would need an explicit
   * number format applied (see {@link #buildNumberFormatRequests}). Used to skip the extra
   * sheet-metadata lookup in {@link #update} when there's nothing to format.
   */
  private static boolean hasScaledDecimalCell(Matrix matrix) {
    int ncol = matrix.columnCount()
    for (int c = 0; c < ncol; c++) {
      for (Object val : matrix.column(c)) {
        if (val instanceof BigDecimal && ((BigDecimal) val).scale() > 0) {
          return true
        }
      }
    }
    false
  }

  private static List<List<Object>> buildValues(Matrix matrix, boolean convertNullsToEmptyString, boolean convertDatesToSerial) {
    List<String> headers = (List<String>) matrix.columnNames()
    List<List<Object>> values = [new ArrayList<Object>(headers)]
    matrix.each { Row it ->
      values << it.collect { GsUtil.toCell(it, convertNullsToEmptyString, convertDatesToSerial) }
    }
    values
  }

  private static Request numberFormatRequest(int sheetId, int col, int startRow, int endRow, int scale) {
    String pattern = ZERO_DIGIT + '.' + (ZERO_DIGIT * scale)
    new Request().setRepeatCell(
        new RepeatCellRequest()
            .setRange(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(startRow)
                .setEndRowIndex(endRow)
                .setStartColumnIndex(col)
                .setEndColumnIndex(col + 1))
            .setCell(new CellData().setUserEnteredFormat(
                new CellFormat().setNumberFormat(
                    new NumberFormat().setType('NUMBER').setPattern(pattern))))
            .setFields('userEnteredFormat.numberFormat')
    )
  }

  /**
   * Returns the Google Sheets edit URL for a spreadsheet ID.
   *
   * @param spreadsheetId The spreadsheet ID
   * @return The full URL to open the spreadsheet in a browser
   */
  static String spreadsheetUrl(String spreadsheetId) {
    "https://docs.google.com/spreadsheets/d/${spreadsheetId}/edit"
  }

  /**
   * Updates an existing Google Spreadsheet with Matrix data.
   *
   * @param spreadsheetId The ID of the existing spreadsheet
   * @param range The target range in A1 notation (e.g., 'Sheet1!A1')
   * @param matrix The Matrix to write
   * @param credentials Google Cloud credentials, or null to use ADC
   * @param convertNullsToEmptyString If true, null values become empty strings
   * @param convertDatesToSerial If true, date/time types are converted to serial numbers
   * @return The spreadsheetId
   * @throws IllegalArgumentException if any required parameter is null/empty
   * @throws SheetOperationException if the update fails
   */
  static String update(String spreadsheetId, String range, Matrix matrix,
                       GoogleCredentials credentials = null,
                       boolean convertNullsToEmptyString = true,
                       boolean convertDatesToSerial = false) {
    GsUtil.validateSheetId(spreadsheetId)
    GsUtil.validateRange(range)
    if (matrix == null) {
      throw new IllegalArgumentException(MATRIX_NULL_ERROR)
    }
    if (matrix.columnCount() == 0) {
      throw new IllegalArgumentException(MATRIX_NO_COLUMNS_ERROR)
    }
    if (matrix.rowCount() == 0) {
      throw new IllegalArgumentException(MATRIX_NO_ROWS_ERROR)
    }

    // Build data: header row + data rows
    List<List<Object>> values = buildValues(matrix, convertNullsToEmptyString, convertDatesToSerial)

    Sheets sheets = buildSheetsService(credentials)

    ValueRange vr = new ValueRange()
        .setRange(range)
        .setMajorDimension(ROWS_DIMENSION)
        .setValues(values)

    try {
      sheets.spreadsheets().values()
          .update(spreadsheetId, range, vr)
          .setValueInputOption(RAW_INPUT)
          .execute()
    } catch (IOException e) {
      throw new SheetOperationException('update data', spreadsheetId, e)
    }

    // Same fix as write(): force explicit decimal places on BigDecimal cells so Sheets
    // doesn't drop trailing zeros. Only resolves the target sheetId (an extra API call)
    // when the matrix actually has a cell that would need it.
    if (hasScaledDecimalCell(matrix)) {
      int sheetId = resolveSheetId(sheets, spreadsheetId, range)
      int[] startCell = parseStartCell(range)
      List<Request> formatRequests = buildNumberFormatRequests(matrix, sheetId, startCell[0] + 1, startCell[1])
      if (formatRequests) {
        try {
          sheets.spreadsheets()
              .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(formatRequests))
              .execute()
        } catch (IOException e) {
          throw new SheetOperationException(APPLY_FORMATS_ERROR, spreadsheetId, e)
        }
      }
    }

    spreadsheetId
  }

  /**
   * Resolves the numeric sheetId for the sheet targeted by an A1 range (e.g. {@code
   * 'My Sheet'!C5:E10}). Ranges without a sheet-name prefix (e.g. {@code A1:D10}) default
   * to the spreadsheet's first sheet, matching Google's own A1 notation convention.
   */
  private static int resolveSheetId(Sheets sheets, String spreadsheetId, String range) {
    String sheetName = extractSheetName(range)
    Spreadsheet meta
    try {
      meta = sheets.spreadsheets().get(spreadsheetId).setFields('sheets.properties').execute()
    } catch (IOException e) {
      throw new SheetOperationException('resolve sheet', spreadsheetId, e)
    }
    List<Sheet> sheetList = meta.getSheets()
    if (sheetName == null) {
      return sheetList.get(0).getProperties().getSheetId()
    }
    for (Sheet s : sheetList) {
      if (s.getProperties().getTitle() == sheetName) {
        return s.getProperties().getSheetId()
      }
    }
    throw new IllegalArgumentException("Sheet '${sheetName}' not found in spreadsheet ${spreadsheetId}")
  }

  /**
   * Extracts and unquotes the sheet-name prefix from an A1 range, or null if the range
   * has no {@code !} prefix.
   */
  private static String extractSheetName(String range) {
    String[] parts = range.split(SHEET_NAME_SEPARATOR, RANGE_SPLIT_LIMIT)
    if (parts.size() <= 1) {
      return null
    }
    String name = parts[0]
    if (name != SINGLE_QUOTE && name.startsWith(SINGLE_QUOTE) && name.endsWith(SINGLE_QUOTE)) {
      name = name.substring(1, name.length() - 1).replace(SINGLE_QUOTE + SINGLE_QUOTE, SINGLE_QUOTE)
    }
    name
  }

  /**
   * Parses the 0-based [row, col] of the starting cell of an A1 range, e.g. {@code
   * 'My Sheet'!C5:E10} -&gt; [4, 2].
   */
  private static int[] parseStartCell(String range) {
    String[] parts = range.split(SHEET_NAME_SEPARATOR, RANGE_SPLIT_LIMIT)
    String cellPart = parts.size() > 1 ? parts[1] : parts[0]
    String startCell = cellPart.split(':')[0]
    def m = startCell =~ /^([A-Z]+)\d+$/
    if (!m.matches()) {
      throw new IllegalArgumentException("Cannot parse starting cell from range '${range}'")
    }
    String colLetters = m.group(1)
    String rowDigits = startCell.substring(colLetters.length())
    int col = GsUtil.asColumnNumber(colLetters) - 1
    int row = Integer.parseInt(rowDigits) - 1
    [row, col] as int[]
  }

  private static Sheets buildSheetsService(GoogleCredentials credentials) {
    def creds = credentials ?: authenticate(SCOPES)
    new Sheets.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        GsonFactory.getDefaultInstance(),
        new HttpCredentialsAdapter(creds))
        .setApplicationName(APP_NAME)
        .build()
  }

}
