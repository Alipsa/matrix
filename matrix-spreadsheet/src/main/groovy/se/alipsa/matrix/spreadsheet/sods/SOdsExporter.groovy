package se.alipsa.matrix.spreadsheet.sods

import com.github.miachm.sods.Sheet
import com.github.miachm.sods.SpreadSheet
import groovy.transform.CompileStatic
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

@CompileStatic
class SOdsExporter {
  private static Logger logger = LogManager.getLogger()


  /**
   * Create a new Open Document Spreadsheet file.
   *
   * @param dataFrame the Matrix to export
   * @param filePath the file path + file name of the file to export to. Should end with .ods
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportOds(String filePath, Matrix dataFrame) {
    return exportOds(new File(filePath), dataFrame)
  }

  /**
   * Create a new Open Document Spreadsheet file.
   *
   * @param dataFrame the Matrix to export
   * @param filePath the file path + file name of the file to export to. Should end with .ods
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportOds(File file, Matrix dataFrame) {
    try {
      SpreadSheet spreadSheet
      if (file.exists()) {
        logger.info("File {} already exists, file length is {} kb", file.getAbsolutePath(), file.length()/1024 )
        spreadSheet = new SpreadSheet(file)
      } else {
        spreadSheet = new SpreadSheet()
      }
      int nextIdx = spreadSheet.getNumSheets() + 1
      Sheet sheet = new Sheet(String.valueOf(nextIdx), dataFrame.rowCount() + 1, dataFrame.columnCount() + 1)
      spreadSheet.appendSheet(sheet)
      buildSheet(dataFrame, sheet)
      writeFile(file, spreadSheet)
      return sheet.name
    } catch (IOException e) {
      logger.error("Failed to create ods file {}" + file.getAbsolutePath(), e)
      return null
    }
  }

  /**
   * upsert: Create new or update existing Open Document Spreadsheet, adding or updating a sheet with the name specified
   *
   * @param dataFrame the data.frame to export
   * @param filePath the file path + file name of the file to export to. Should end with .ods
   * @param sheetName the name of the sheet to write to
   * @return true if successful, false if not written (e.g. file cannot be written to)
   */
  static String exportOds(File file, Matrix dataFrame, String sheetName) {
    return exportOdsSheets(file, [dataFrame], [sheetName])[0]
  }

  /**
   * upsert: Create new or update existing Open Document Spreadsheet, adding or updating a sheet with the name specified
   *
   * @param dataFrame the data.frame to export
   * @param filePath the file path + file name of the file to export to. Should end with .ods
   * @param sheetName the name of the sheet to write to
   * @return true if successful, false if not written (e.g. file cannot be written to)
   */
  static String exportOds(String filePath, Matrix dataFrame, String sheetName) {
    return exportOdsSheets(filePath, [dataFrame], [sheetName])
  }

  static List<String> exportOdsSheets(String filePath, List<Matrix> data, List<String> sheetNames) {
    File file = new File(filePath)
    exportOdsSheets(file, data, sheetNames)
  }

  static List<String> exportOdsSheets(File file, List<Matrix> data, List<String> sheetNames) {
    try {
      SpreadSheet spreadSheet
      if (file.exists()) {
        spreadSheet = new SpreadSheet(file)
      } else {
        spreadSheet = new SpreadSheet()
      }
      List<String> actualSheetNames = []
      for (int i = 0; i < data.size(); i++) {
        Matrix dataFrame = data[i]
        String sheetName = SpreadsheetUtil.createValidSheetName(sheetNames[i])
        upsertSheet(dataFrame, sheetName, spreadSheet)
        actualSheetNames.add(sheetName)
      }
      writeFile(file, spreadSheet)
      return actualSheetNames
    } catch (IOException e) {
      logger.error("Failed to create ods file {}" + file.getAbsolutePath(), e)
      return []
    }
  }

  private static String upsertSheet(Matrix dataFrame, String sheetName, SpreadSheet spreadSheet) {
    Sheet sheet = spreadSheet.getSheet(sheetName)
    String actualSheetName = SpreadsheetUtil.createValidSheetName(sheetName)
    if (sheet == null) {
      sheet = new Sheet(actualSheetName, dataFrame.rowCount() + 1, dataFrame.columnCount() +1)
      spreadSheet.appendSheet(sheet)
    }
    return buildSheet(dataFrame, sheet)
  }

  private static String buildSheet(Matrix dataFrame, Sheet sheet) {

    List<String> names = dataFrame.columnNames()

    //Ensure there is enough space
    if (sheet.getMaxColumns() < names.size()) {
      sheet.appendColumns(names.size() - sheet.getMaxColumns())
    }
    if (sheet.getMaxRows() < dataFrame.rowCount() + 1) {
      sheet.appendRows(dataFrame.rowCount() + 1 - sheet.getMaxRows())
    }

    for (int i = 0; i < names.size(); i++) {
      sheet.getRange(0, i).setValue(names[i])
    }

    Iterator<Column> it = dataFrame.columns().iterator()
    int colIdx = 0
    while (it.hasNext()) {
      List<?> colVec = it.next()
      for (int i = 0; i < colVec.size(); i++) {
        int row = i + 1
        sheet.getRange(row, colIdx).setValue(colVec[i])
      }
      colIdx++
    }
    return sheet.name
  }

  private static boolean writeFile(File file, SpreadSheet spreadSheet) throws IOException {
    logger.info("Writing spreadsheet to {}", file.getAbsolutePath())
    try(FileOutputStream fos = new FileOutputStream(file)) {
      spreadSheet.save(fos)
    }
    if (!file.exists()) {
      System.err.println("Failed to write to file")
      return false
    }
    return true
  }
}
