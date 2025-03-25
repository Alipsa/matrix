package se.alipsa.matrix.spreadsheet.sods

import com.github.miachm.sods.OfficeCurrency
import com.github.miachm.sods.OfficePercentage
import com.github.miachm.sods.Range
import com.github.miachm.sods.Sheet
import groovy.transform.CompileStatic
import se.alipsa.matrix.spreadsheet.SpreadsheetUtil
import se.alipsa.matrix.spreadsheet.ValueExtractor

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A value extractor specialized in extracting info from a Calc (ods) file
 */
@CompileStatic
class SOdsValueExtractor extends ValueExtractor {

   private final Sheet sheet

   SOdsValueExtractor(Sheet sheet) {
      this.sheet = sheet
   }


   double getDouble(int row, int column) {
      return getDouble(sheet.getRange(row, column))
   }

   double getDouble(Range range) {
      return getDouble(range.getValue())
   }

   float getFloat(int row, int column) {
      return (float) getDouble(row, column)
   }

   int getInt(int row, int column) {
      return getInt(sheet.getRange(row, column))
   }

   int getInt(Range range) {
      return getInt(range.getValue())
   }

   Object getObject(Range range) {
      range.getValue()
   }

   Object getObject(int row, int column) {
      try {
         def val = getObject(sheet.getRange(row, column))
         if (val != null) {
            if (val.class == OfficePercentage) {
               return ((OfficePercentage) val).value
            } else if (val.class == OfficeCurrency) {
               return ((OfficeCurrency)val).value
            }
         }
         return val
      } catch (IndexOutOfBoundsException e) {
         throw new IndexOutOfBoundsException("Sheet: ${sheet.name}: Failed to get Object at row $row, col $column: ${e.getMessage()}")
      }
   }

   String getString(int row, int column) {
      try {
         return getString(sheet.getRange(row, column))
      } catch (IndexOutOfBoundsException e) {
         throw new IndexOutOfBoundsException("Sheet: ${sheet.name}: Failed to get String at row $row, col $column: ${e.getMessage()}")
      }
   }

   static String getString(Range range) {
      Object val = range.getValue()
      if (val instanceof LocalDateTime) {
         return SpreadsheetUtil.dateTimeFormatter.format((LocalDateTime)val)
      }
      if (val instanceof LocalDate) {
         return SpreadsheetUtil.dateTimeFormatter.format(((LocalDate) val).atStartOfDay())
      }
      return val == null ? null : String.valueOf(val)
   }

   Long getLong(Range range) {
      return getLong(range.getValue())
   }

   Long getLong(int row, int column) {
      return getLong(sheet.getRange(row, column))
   }

   Boolean getBoolean(int row, int column) {
      return getBoolean(sheet.getRange(row, column))
   }

   Boolean getBoolean(Range range) {
      return getBoolean(range.getValue())
   }
}
