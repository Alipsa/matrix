package se.alipsa.groovy.spreadsheet.excel;

import org.apache.poi.ss.usermodel.*
import se.alipsa.groovy.spreadsheet.SpreadsheetUtil
import se.alipsa.groovy.spreadsheet.ValueExtractor

import java.time.LocalDateTime;

/**
 * A value extractor specialized in extracting info from an Excel file
 */
class ExcelValueExtractor extends ValueExtractor {

   private final Sheet sheet
   private final FormulaEvaluator evaluator
   private final DataFormatter dataFormatter

   ExcelValueExtractor(Sheet sheet, DataFormatter... dataFormatterOpt) {
      if (sheet == null) {
         throw new IllegalArgumentException("Sheet is null, will not be able to extract any values")
      }
      this.sheet = sheet
      evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator()
      if (dataFormatterOpt.length > 0) {
         dataFormatter = dataFormatterOpt[0]
      } else {
         dataFormatter = new DataFormatter()
      }
   }


   Double getDouble(int row, int column) {
      return getDouble(sheet.getRow(row), column)
   }

   Double getDouble(Row row, int column) {
      return row == null ? null : getDouble(getObject(row.getCell(column)))
   }

   Float getFloat(int row, int column) {
      Double d = getDouble(sheet.getRow(row), column)
      return d == null ? null : d.floatValue()
   }

   Float getFloat(Row row, int column) {
      if (row == null) return null
      Double d = getDouble(row, column)
      return d == null ? null : d.floatValue()
   }

   Integer getInteger(int row, int column) {
      return getInteger(sheet.getRow(row), column)
   }

   Integer getInteger(Row row, int column) {
      return row == null ? null : getInt(getObject(row.getCell(column)))
   }

   String getString(int row, int column) {
      return getString(sheet.getRow(row), column)
   }

   String getString(Row row, int column) {
      if (row == null) return null
      Object val = getObject(row.getCell(column))
      return val == null ? null : String.valueOf(val)
   }

   String getString(Cell cell) {
      return getString(getObject(cell))
   }

   Long getLong(int row, int column) {
      return getLong(sheet.getRow(row), column)
   }

   Long getLong(Row row, int column) {
      return row == null ? null : getLong(getObject(row.getCell(column)))
   }

   Boolean getBoolean(int row, int column) {
      return getBoolean(sheet.getRow(row), column)
   }

   Boolean getBoolean(Row row, int column) {
      return row == null ? null : getBoolean(getObject(row.getCell(column)))
   }

   LocalDateTime getLocalDateTime(Row row, int column) {
      return getLocalDateTime(row.getCell(column))
   }

   LocalDateTime getLocalDateTime(Cell cell) {
      return LocalDateTime.parse(String.valueOf(getObject(cell)), SpreadsheetUtil.dateTimeFormatter)
   }

   /**
    * get the value from a Excel cell
    * @param cell the cell to extract the value from
    * @return the value
    */
   Object getObject(Cell cell) {
      if (cell == null) {
         return null
      }

      switch (cell.getCellType()) {
         case CellType.BLANK:
            return null
         case CellType.NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
               return SpreadsheetUtil.dateTimeFormatter.format(cell.getLocalDateTimeCellValue())
            }
            return cell.getNumericCellValue()
         case CellType.BOOLEAN:
            return cell.getBooleanCellValue()
         case CellType.STRING:
            return cell.getStringCellValue()
         case CellType.FORMULA:
            return getValueFromFormulaCell(cell)
         default:
            return dataFormatter.formatCellValue(cell)
      }
   }

   private Object getValueFromFormulaCell(Cell cell) {
      switch (evaluator.evaluateFormulaCell(cell)) {
         case CellType.BLANK:
            return null
         case CellType.NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
               return cell.getLocalDateTimeCellValue()
            }
            return evaluator.evaluate(cell).getNumberValue()
         case CellType.BOOLEAN:
            return evaluator.evaluate(cell).getBooleanValue()
         case CellType.STRING:
            return evaluator.evaluate(cell).getStringValue()
         default:
            return dataFormatter.formatCellValue(cell)
      }
   }
}
