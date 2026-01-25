package se.alipsa.matrix.spreadsheet.fastexcel

import groovy.transform.CompileStatic
import org.dhatim.fastexcel.reader.Cell
import org.dhatim.fastexcel.reader.CellType
import org.dhatim.fastexcel.reader.Row
import org.dhatim.fastexcel.reader.Sheet
import se.alipsa.matrix.core.ValueConverter
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.ValueExtractor

import java.time.LocalDateTime

/**
 * A value extractor specialized in extracting info from an Excel file
 */
@CompileStatic
class FExcelValueExtractor extends ValueExtractor {

   private final Sheet sheet
   private final boolean isDate1904
   //private final FormulaEvaluator evaluator
   //private final DataFormatter dataFormatter

   FExcelValueExtractor(Sheet sheet, boolean isDate1904) {
      if (sheet == null) {
         throw new IllegalArgumentException("Sheet is null, will not be able to extract any values")
      }
      this.sheet = sheet
      this.isDate1904 = isDate1904
      /*
      evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator()
      if (dataFormatterOpt.length > 0) {
         dataFormatter = dataFormatterOpt[0]
      } else {
         dataFormatter = new DataFormatter()
      }*/
   }


   Double getDouble(int row, int column) {
      return getDouble(FExcelUtil.getRow(sheet, row), column)
   }

   Double getDouble(Row row, int column) {
      return row == null ? null : getDouble(getObject(row.getCell(column)))
   }

   Float getFloat(int row, int column) {
      Double d = getDouble(FExcelUtil.getRow(sheet, row), column)
      return d == null ? null : d.floatValue()
   }

   Float getFloat(Row row, int column) {
      if (row == null) return null
      Double d = getDouble(row, column)
      return d == null ? null : d.floatValue()
   }

   Integer getInteger(int row, int column) {
      return getInteger(FExcelUtil.getRow(sheet, row), column)
   }

   Integer getInteger(Row row, int column) {
      return row == null ? null : getInt(getObject(row.getCell(column)))
   }

   String getString(int row, int column) {
      return getString(FExcelUtil.getRow(sheet, row), column)
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
      return getLong(FExcelUtil.getRow(sheet, row), column)
   }

   Long getLong(Row row, int column) {
      return row == null ? null : getLong(getObject(row.getCell(column)))
   }

   Boolean getBoolean(int row, int column) {
      return getBoolean(FExcelUtil.getRow(sheet, row), column)
   }

   Boolean getBoolean(Row row, int column) {
      return row == null ? null : getBoolean(getObject(row.getCell(column)))
   }

   LocalDateTime getLocalDateTime(Row row, int column) {
      return row == null ? null : getLocalDateTime(row.getCell(column))
   }

   LocalDateTime getLocalDateTime(Cell cell) {
      Object value = getObject(cell)
      if (value == null) return null
      return LocalDateTime.parse(String.valueOf(value), SpreadsheetUtil.dateTimeFormatter)
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
      def formatId = cell.dataFormatId
      def formatString = cell.dataFormatString
      switch (cell.getType()) {
         case CellType.EMPTY:
            return null
         case CellType.NUMBER:
            if (FDateUtil.isADateFormat(formatId, formatString)) {
               def date = cell.asDate()
               if (!formatString.toLowerCase().contains('hh') && date.hour == 0 && date.minute == 0) {
                  return date.toLocalDate()
               } else {
                  return date
               }
            } else {
               return cell.asNumber()
            }
         case CellType.BOOLEAN:
            return cell.asBoolean()
         case CellType.STRING:
            return cell.asString()
         case CellType.FORMULA:
            return getValueFromFormulaCell(cell)
         default:
            return cell.getRawValue()
      }
   }

   private Object getValueFromFormulaCell(Cell cell) {
      String rawValue = cell.getRawValue()
      Integer dataFormatId = cell.dataFormatId
      String dateFormatString = cell.dataFormatString
      if (FDateUtil.isADateFormat(dataFormatId, dateFormatString)) {
         def date = FDateUtil.convertToDate(ValueConverter.asDouble(rawValue), isDate1904)
         if (!dateFormatString.toLowerCase().contains('hh') && date.hour == 0 && date.minute == 0) {
            return date.toLocalDate()
         } else {
            return date
         }
      } else if (ValueConverter.isNumeric(rawValue)){
         return ValueConverter.asNumber(rawValue)
      } else {
         return rawValue
      }
   }
}
