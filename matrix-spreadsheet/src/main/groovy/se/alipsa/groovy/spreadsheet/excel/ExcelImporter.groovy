package se.alipsa.groovy.spreadsheet.excel

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import se.alipsa.groovy.matrix.*
import se.alipsa.groovy.spreadsheet.FileUtil
import se.alipsa.groovy.spreadsheet.SpreadsheetUtil

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory

class ExcelImporter {

    private static Logger log = LogManager.getLogger()
    /**
     * Import an excel spreadsheet
     * @param file the filePath or the file object pointing to the excel file
     * @param sheetName the name of the sheet to import, default is 'Sheet1'
     * @param startRow the starting row for the import (as you would see the row number in excel), defaults to 1
     * @param endRow the last row to import
     * @param startCol the starting column name (A, B etc) or column number (1, 2 etc.)
     * @param endCol the end column name (K, L etc) or column number (11, 12 etc.)
     * @param firstRowAsColNames whether the first row should be used for the names of each column, if false
     * it column names will be v1, v2 etc. Defaults to true
     * @return A Matrix with the excel data.
     */
    static Matrix importExcel(String file, String sheetName = 'Sheet1',
                                        int startRow = 1, int endRow,
                                        String startCol = 'A', String endCol,
                                        boolean firstRowAsColNames = true) {

        return importExcel(
                file,
                sheetName,
                startRow as int,
                endRow as int,
                SpreadsheetUtil.asColumnNumber(startCol) as int,
                SpreadsheetUtil.asColumnNumber(endCol) as int,
                firstRowAsColNames as boolean
        )
    }

    static Matrix importExcel(String file, int sheetNumber,
                                   int startRow = 1, int endRow,
                                   int startCol = 1, int endCol,
                                   boolean firstRowAsColNames = true) {
        def header = []
        File excelFile = FileUtil.checkFilePath(file)
        try (Workbook workbook = WorkbookFactory.create(excelFile)) {
            Sheet sheet = workbook.getSheetAt(sheetNumber)
            if (firstRowAsColNames) {
                buildHeaderRow(startRow, startCol, endCol, header, sheet)
                startRow = startRow + 1
            } else {
                for (int i = 1; i <= endCol - startCol; i++) {
                    header.add(String.valueOf(i))
                }
            }
            return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
        }
    }

    static Matrix importExcel(String file, int sheet,
                                       int startRow = 1, int endRow,
                                       String startColumn = 'A', String endColumn,
                                       boolean firstRowAsColNames = true) {
        return importExcel(
            file,
            sheet,
            startRow,
            endRow,
            SpreadsheetUtil.asColumnNumber(startColumn),
            SpreadsheetUtil.asColumnNumber(endColumn),
            firstRowAsColNames
        )
    }

    static Matrix importExcel(String file, String sheetName = 'Sheet1',
                                          int startRow = 1, int endRow,
                                          int startCol = 1, int endCol,
                                          boolean firstRowAsColNames = true) {
        def header = []
        File excelFile = FileUtil.checkFilePath(file)
        try (Workbook workbook = WorkbookFactory.create(excelFile)) {
            Sheet sheet = workbook.getSheet(sheetName)
            if (firstRowAsColNames) {
                buildHeaderRow(startRow, startCol, endCol, header, sheet)
                startRow = startRow + 1
            } else {
                for (int i = 1; i <= endCol - startCol; i++) {
                    header.add(String.valueOf(i))
                }
            }
            return importExcelSheet(sheet, startRow, endRow, startCol, endCol, header)
        }
    }

    private static void buildHeaderRow(int startRowNum, int startColNum, int endColNum, List<String> header, Sheet sheet) {
        startRowNum--
        startColNum--
        endColNum--
        ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
        Row row = sheet.getRow(startRowNum)
        for (int i = 0; i <= endColNum - startColNum; i++) {
            header.add(ext.getString(row, startColNum + i))
        }
    }

    private static Matrix importExcelSheet(Sheet sheet, int startRowNum, int endRowNum, int startColNum, int endColNum, List<String> colNames) {
        startRowNum--
        endRowNum--
        startColNum--
        endColNum--

        ExcelValueExtractor ext = new ExcelValueExtractor(sheet)
        List<List<?>> matrix = []
        List<?> rowList
        for (int rowIdx = startRowNum; rowIdx <= endRowNum; rowIdx++) {
            Row row = sheet.getRow(rowIdx)
            if (row == null) {
                log.warn("Row ${rowIdx + 1} is null")
                continue
            }
            rowList = []
            for (int colIdx = startColNum; colIdx <= endColNum; colIdx++) {
                def cell = row.getCell(colIdx)
                if (cell == null) {
                    rowList.add(null)
                } else {
                    //println("cell[$rowIdx, $colIdx]: ${cell.getCellType()}: val: ${ext.getObject(cell)}")
                    switch (cell.getCellType()) {
                        case CellType.BLANK -> rowList.add(null)
                        case CellType.BOOLEAN -> rowList.add(ext.getBoolean(row, colIdx))
                        case CellType.NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                rowList.add(ext.getLocalDateTime(cell))
                            } else {
                                rowList.add(ext.getDouble(row, colIdx))
                            }
                        }
                        case CellType.STRING -> rowList.add(ext.getString(row, colIdx))
                        case CellType.FORMULA -> rowList.add(ext.getObject(cell))
                        default -> rowList.add(ext.getString(row, colIdx))
                    }
                }
            }
            matrix.add(rowList)
        }
        return Matrix.create(sheet.getSheetName(), colNames, matrix, [Object]*colNames.size())
    }
}
