package se.alipsa.matrix.spreadsheet.fastexcel

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.dhatim.fastexcel.reader.ReadableWorkbook
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class FExcelExporter {

  static final Logger logger = LogManager.getLogger()
  static final String APP_NAME = 'matrix-spreadsheet'
  static final String VERSION = '02.1000' // must be in the format XX.YYYY

  /**
   * Export to an Excel file. If the file does not exists, a new file will be created
   * if the excel file exists and is not empty, a new sheet will be added to the excel
   * The name of the sheet will correspond to the name of the Matrix
   * @param filePath the path to the file to export
   * @param data the Matrix data to export
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(String filePath, Matrix data) {
    File file = new File(filePath)
    exportExcel(file, data)
  }

  /**
   * Export to an Excel file. If the file does not exists, a new file will be created
   * if the excel file exists and is not empty, a new sheet will be added to the excel
   * The name of the sheet will correspond to the name of the Matrix
   * @param file the file to export
   * @param data the Matrix data to export
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(File file, Matrix data) {
    String sheetName = SpreadsheetUtil.createValidSheetName(data.matrixName)
    exportExcel(file, data, sheetName)
    return sheetName
  }

  /**
   * Export to an Excel file. If the file does not exists, a new file will be created
   * if the excel file exists and is not empty, a new sheet will be added to the excel
   *
   * @param file the file to export
   * @param data the Matrix data to export
   * @param sheetName the name of the sheet to export to
   * @return the actual name of the sheet created (illegal characters replaced by space)
   */
  static String exportExcel(File file, Matrix data, String sheetName) {
    String validSheetName = SpreadsheetUtil.createValidSheetName(sheetName)
    if (file.exists() && file.length() > 0) {
      try (FileInputStream fis = new FileInputStream(file)
           ReadableWorkbook workbook = new ReadableWorkbook(fis, FExcelImporter.OPTIONS)) {
        List<String> sheetNames = FExcelReader.getSheetNames(workbook)
        if (sheetNames.contains(validSheetName)) {
          int index = 1
          while (true) {
            validSheetName = SpreadsheetUtil.createValidSheetName(validSheetName + index++)
            if (!sheetNames.contains(validSheetName)) {
              break
            }
          }
        }
        throw new Exception("append to existing excel is not yet supported")

        /*
        todo: 1. import each sheet into a map<sheetName, matrix>
          2. for each in map:  buildSheet(value, wb.newWorksheet(key))
          3. append the new sheet: buildSheet(data, wb.newWorksheet(validSheetName))
        Sheet sheet = workbook.(validSheetName)
        buildSheet(data, sheet)
        writeFile(file, workbook)*/
      }
    } else {
      try (FileOutputStream fos = new FileOutputStream(file); Workbook workbook = new Workbook(fos, APP_NAME, VERSION)) {
        Worksheet sheet = workbook.newWorksheet(validSheetName)
        buildSheet(data, sheet)
        //writeFile(file, workbook)
      }
    }
    return validSheetName
  }

  static List<String> exportExcelSheets(String filePath, List<Matrix> data, List<String> sheetNames) {
    return exportExcelSheets(new File(filePath), data, sheetNames)
  }

  static List<String> exportExcelSheets(File file, List<Matrix> data) {
    exportExcelSheets(file, data, data.collect{it.matrixName})
  }

  static List<String> exportExcelSheets(File file, List<Matrix> data, List<String> sheetNames, boolean overwrite = false) throws IOException {

    if (file.exists() && !overwrite && file.length() > 0) {
      throw new IllegalArgumentException("Appending to an external file is not supported, remove it first")
    }

    try (FileOutputStream fos = new FileOutputStream(file); Workbook workbook = new Workbook(fos, APP_NAME, VERSION)) {

      List<String> actualSheetNames = []
      for (int i = 0; i < data.size(); i++) {
        Matrix dataFrame = data.get(i)
        String sheetName = sheetNames.toArray()[i]
        buildSheet(dataFrame, workbook.newWorksheet(sheetName))
        actualSheetNames.add(sheetName)
      }
      return actualSheetNames
    } catch (IOException e) {
      logger.error("Failed to create excel file {}" + file.getAbsolutePath(), e)
      throw new IOException("Failed to create excel file ${file}", e)
    }
  }

  private static boolean isXssf(File file) {
    return isXssf(file.getName())
  }

  private static boolean isXssf(String filePath) {
    return !filePath.toLowerCase().endsWith(".xls")

  }

  private static void buildSheet(Matrix data, Worksheet sheet) {
    def names = data.columnNames()
    for (int i = 0; i < names.size(); i++) {
      sheet.value(0, i, names[i])
    }

    data.columns().eachWithIndex { Column column, int c ->
      Class type = column.type
      int col = c
      column.eachWithIndex { Object entry, int r ->
        int row = r + 1
        if (entry == null) {
          // do nothing
        } else if (Number.isAssignableFrom(type)) {
          sheet.value(row, col, ValueConverter.asNumber(entry))
        } else if (type in [byte, Byte]) {
          sheet.value(row, col, ValueConverter.asByte(entry))
        } else if (boolean == type || Boolean == type) {
          sheet.value(row, col, ValueConverter.asBoolean(entry))
        } else if (LocalDate == type) {
          sheet.value(row, col, ValueConverter.asLocalDate(entry))
          sheet.style(row, col).format("yyyy-MM-dd").set()
        } else if (LocalDateTime == type) {
          sheet.value(row, col, ValueConverter.asLocalDateTime(entry))
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS").set()
        } else if (ZonedDateTime == type) {
          sheet.value(row, col, entry as ZonedDateTime)
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS Z").set()
        } else if (Date == type) {
          sheet.value(row, col, ValueConverter.asDate(entry))
          sheet.style(row, col).format("yyyy-MM-dd HH:mm:ss.SSS").set()
        } else {
          sheet.value(row, col, String.valueOf(entry))
        }
      }
    }
  }

  private static String upsertSheet(Matrix dataFrame, String sheetName, Workbook workbook) {
    /*Worksheet sheet = workbook.getSheet(sheetName)
    if (sheet == null) {
      sheet = workbook.createSheet(SpreadsheetUtil.createValidSheetName(sheetName))
    }
    buildSheet(dataFrame, sheet)
    return sheet.getSheetName()*/
    System.err.println("Not yet implemented")
    return null
  }

  private static void writeFile(File file, Workbook workbook) throws IOException {
    if (workbook == null) {
      logger.warn("Workbook is null, cannot write to file")
      return
    }
    logger.info("Writing spreadsheet to {}", file.getAbsolutePath())
    // default is 100 000 000, 600M takes up about 1 GB of memory
    //IOUtils.setByteArrayMaxOverride(600_000_000)
    try (FileOutputStream fos = new FileOutputStream(file)) {
      workbook.write(fos)
    }
  }
}
