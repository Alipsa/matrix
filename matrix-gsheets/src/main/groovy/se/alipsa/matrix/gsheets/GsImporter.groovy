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
 * Imports a Matrix from a Google Sheet
 */
@CompileStatic
class GsImporter {

  /**
   * @param sheetId This is a unique identifier found in the spreadsheet's URL. It's the string of characters
   *        between /d/ and /edit in the URL.
   * @param range A1 Notation: This is the standard way to specify a cell or range of cells in a spreadsheet,
   *        for example, Sheet1!A1:B2 refers to a specific range on a specific sheet.
   * @return A Matrix corresponding to the sheet range specified
   */
  static Matrix importSheet(String sheetId, String range, boolean firstRowAsColumnNames) {
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    GoogleCredentials credentials = BqAuthenticator.authenticate(BqAuthenticator.SCOPE_SHEETS_READONLY)
    def sheetsService = new Sheets.Builder(
        transport,
        gsonFactory,
        new HttpCredentialsAdapter(credentials))
        .setApplicationName("Groovy Sheets Reader")
        .build()

    def response = sheetsService
        .spreadsheets()
        .values()
        .get(sheetId, range)
        .setValueRenderOption('UNFORMATTED_VALUE')
        .execute()
    List<List<Object>> values = response.getValues()
    int ncol = GsUtil.columnCountForRange(range)
    // if values are missing, fill with nulls
    values.each {
      fillListToSize(it, ncol)
    }
    List<String> headers
    if (firstRowAsColumnNames) {
      List<Object> firstRow = values.remove(0)
      headers = firstRow.collect { String.valueOf(it) }
    } else {
      headers = Matrix.anonymousHeader(ncol)
    }
    values.each {

    }
    def sheetName = range.split('!')[0]
    Matrix.builder(sheetName)
        .rows(values)
        .columnNames(fillHeaderToSize(headers, ncol))
        .build()
  }

  static List<String> fillHeaderToSize(List<String> list, int desiredSize) {
    if (list.size() >= desiredSize) {
      return list
    }

    int currentSize = list.size()
    for (int i = currentSize; i < desiredSize; i++) {
      def index = i + 1
      list.add("c" + index)
    }
    list
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
}
