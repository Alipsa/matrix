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

  static Matrix importSheet(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
    return importSheetAsStrings(sheetId, range, firstRowAsColumnNames, credentials)
  }
  /**
   * @param sheetId This is a unique identifier found in the spreadsheet's URL. It's the string of characters
   *        between /d/ and /edit in the URL.
   * @param range A1 Notation: This is the standard way to specify a cell or range of cells in a spreadsheet,
   *        for example, Sheet1!A1:B2 refers to a specific range on a specific sheet.
   * @return A Matrix corresponding to the sheet range specified
   */
  static Matrix importSheetAsObject(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null, boolean convertEmptyToNull = false) {
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

    def response = sheetsService
        .spreadsheets()
        .values()
        .get(sheetId, range)
        .setValueRenderOption('UNFORMATTED_VALUE')
        .execute()
    List<List<Object>> values = response.getValues()
    int ncol = GsUtil.columnCountForRange(range)

    List<String> headers
    if (firstRowAsColumnNames) {
      List<Object> firstRow = values.remove(0)
      headers = buildHeader(ncol, firstRow)
    } else {
      headers = Matrix.anonymousHeader(ncol)
    }

    // if values are missing, fill with nulls
    for (int r = 0; r < values.size(); r++) {
      List<Object> row = values.get(r)
      if (convertEmptyToNull) {
        for (int c = 0; c < row.size(); c++) {
          Object v = row.get(c)
          if (v instanceof CharSequence && ((CharSequence) v).length() == 0) {
            row.set(c, null)
          }
        }
      }
      fillListToSize(row, ncol)
    }

    def sheetName = range.split('!')[0]
    Matrix.builder(sheetName)
        .rows(values)
        .columnNames(headers)
        .build()
  }

  static Matrix importSheetAsStrings(String sheetId, String range, boolean firstRowAsColumnNames, GoogleCredentials credentials = null) {
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

    def request = sheetsService
        .spreadsheets()
        .values()
        .get(sheetId, range)
        .setValueRenderOption('FORMATTED_VALUE')

    def response = request.execute()
    List<List<Object>> values = response.getValues()
    int ncol = GsUtil.columnCountForRange(range)

    List<String> headers
    if (firstRowAsColumnNames) {
      List<Object> firstRow = values.remove(0)
      headers = buildHeader(ncol, firstRow)
    } else {
      headers = Matrix.anonymousHeader(ncol)
    }

    def sheetName = range.split('!')[0]
    List<List<String>> rows = []
    values.each {valueRow ->
      List<String> row = []
      for (int i = 0; i < ncol; i++) {
        if (i < valueRow.size()) {
          def cell = valueRow.get(i)
          if (cell == '' || cell == null) {
            row << null
          } else {
            row << String.valueOf(cell)
          }
        } else {
          // Pad missing trailing columns with null
          row << null
        }
      }
      rows << row
    }
    Matrix.builder(sheetName)
        .rows(rows)
        .columnNames(headers)
        .types([String] * ncol)
        .build()
  }

  private static List<String> buildHeader(int ncol, List<Object> firstRow) {
    List<String> headers  = []
    for (int i = 0; i < ncol; i++) {
      def val = firstRow.get(i)
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
