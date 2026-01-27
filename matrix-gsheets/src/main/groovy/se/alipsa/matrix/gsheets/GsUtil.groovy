package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import se.alipsa.matrix.core.util.Logger

class GsUtil {

  private static final Logger log = Logger.getLogger(GsUtil)

  static void deleteSheet(String spreadsheetId) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException("spreadsheetId must not be null or empty")
    }

    def scopes = ["https://www.googleapis.com/auth/drive"] + BqAuthenticator.SCOPES
    def credentials = BqAuthenticator.authenticate(scopes)
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    def driveService = new Drive.Builder(transport, gsonFactory, cred)
        .setApplicationName("Matrix GSheets")
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
      throw new IllegalArgumentException("spreadsheetId must not be null or empty")
    }
    if (driveService == null) {
      throw new IllegalArgumentException("driveService must not be null")
    }

    try {
      // Perform the delete operation on the Drive file using its ID
      driveService.files().delete(spreadsheetId).execute()
      log.info "Successfully deleted spreadsheet with ID: ${spreadsheetId}"
    } catch (IOException e) {
      throw new SheetOperationException("delete", spreadsheetId, e)
    }
  }

  /**
   * Calculates the number of columns in a given A1-style range string.
   * @param range The range string, e.g., "Arkiv!B2:H100" or "A1:C10".
   * @return The number of columns in the range.
   */
  static int columnCountForRange(String range) {
    if (range == null || range.trim().isEmpty()) {
      throw new IllegalArgumentException("range must not be null or empty")
    }

    String[] parts = range.split('!')
    String cellRange = parts.size() > 1 ? parts[1] : parts[0]

    // Split the range into start and end cells
    String[] cellParts = cellRange.split(':')
    if (cellParts.size() != 2) {
      throw new IllegalArgumentException(
        "Invalid range format: '${range}'. Expected A1 notation with a range like 'Sheet1!A1:D10' or 'A1:D10'"
      )
    }

    String startCell = cellParts[0]
    String endCell = cellParts[1]

    // Extract the column letters from the cell references
    String startColumnLetters = (startCell =~ /^([A-Z]+)/)[0][1]
    String endColumnLetters = (endCell =~ /^([A-Z]+)/)[0][1]

    // Convert column letters to numerical indices
    int startColIndex = asColumnNumber(startColumnLetters)
    int endColIndex = asColumnNumber(endColumnLetters)

    // Calculate the number of columns
    return endColIndex - startColIndex + 1
  }

  /**
   * Converts a column letter string (e.g., "A", "Z", "AA") to its numerical index (1-based).
   * @param colLetters The column letter string.
   * @return The 1-based column index.
   */
  static int asColumnNumber(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Column name must not be null or empty")
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
    return number
  }

  static List<String> getSheetNames(String spreadsheetId, GoogleCredentials credentials = null) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException("spreadsheetId must not be null or empty")
    }

    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    if (credentials == null) {
      credentials = BqAuthenticator.authenticate(BqAuthenticator.SCOPE_SHEETS_READONLY)
    }

    def sheetsService = new Sheets.Builder(
        transport,
        gsonFactory,
        new HttpCredentialsAdapter(credentials))
        .setApplicationName("Groovy Sheets Reader")
        .build()

    return getSheetNames(spreadsheetId, sheetsService)
  }

  /**
   * Get sheet names from a spreadsheet using a provided Sheets service.
   * This overload is useful for testing with mocked services.
   *
   * @param spreadsheetId The ID of the spreadsheet
   * @param sheetsService The Sheets service to use
   * @return List of sheet names
   */
  static List<String> getSheetNames(String spreadsheetId, Sheets sheetsService) {
    if (spreadsheetId == null || spreadsheetId.trim().isEmpty()) {
      throw new IllegalArgumentException("spreadsheetId must not be null or empty")
    }
    if (sheetsService == null) {
      throw new IllegalArgumentException("sheetsService must not be null")
    }

    try {
      // Fetch the spreadsheet metadata (this includes the list of sheets)
      def spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()

      List<String> names = []
      spreadsheet.getSheets().each { sheet ->
        names.add(sheet.getProperties().getTitle())
      }

      return names
    } catch (Exception e) {
      log.error("Failed to retrieve sheet names for spreadsheetId '$spreadsheetId': ${e.message}", e)
      throw e
    }
  }
}
