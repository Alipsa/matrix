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
  static Matrix importSheet(String sheetId, String range) {
    def transport = GoogleNetHttpTransport.newTrustedTransport()
    def gsonFactory = GsonFactory.getDefaultInstance()

    GoogleCredentials credentials = BqAuthenticator.authenticate(SheetsScopes.SPREADSHEETS_READONLY)
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
    def sheetName = range.split('!')[0]
    Matrix.builder(sheetName).rows(values).build()
  }
}
