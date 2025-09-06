package se.alipsa.matrix.gsheets

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class GsUtil {

  private static Logger log = LogManager.getLogger(GsUtil)

  static boolean deleteSheet(String spreadsheetId) {
    def scopes = ["https://www.googleapis.com/auth/drive"] + BqAuthenticator.SCOPES
    def credentials = BqAuthenticator.authenticate(scopes)
    HttpRequestInitializer cred = new HttpCredentialsAdapter(credentials)
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    def driveService = new Drive.Builder(transport, gsonFactory, cred)
        .setApplicationName("Matrix GSheets")
        .build()

    try {
      // Perform the delete operation on the Drive file using its ID
      driveService.files().delete(spreadsheetId).execute()
      log.info "Successfully deleted spreadsheet with ID: ${spreadsheetId}"
      return true
    } catch (IOException e) {
      log.error("An error occurred while deleting the file: {}", e.getMessage())
    }
    return false
  }

  /**
   * Calculates the number of columns in a given A1-style range string.
   * @param range The range string, e.g., "Arkiv!B2:H100" or "A1:C10".
   * @return The number of columns in the range.
   */
  static int columnCountForRange(String range) {
    String[] parts = range.split('!')
    String cellRange = parts.size() > 1 ? parts[1] : parts[0]

    // Split the range into start and end cells
    String[] cellParts = cellRange.split(':')
    if (cellParts.size() != 2) {
      throw new IllegalArgumentException("Invalid range format: ${range}")
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
    if (name == null) {
      return 0
    }
    String colName = name.toUpperCase()
    int number = 0
    for (int i = 0; i < colName.length(); i++) {
      number = number * 26 + (colName.charAt(i) - ('A' as char - 1))
    }
    return number
  }
}
