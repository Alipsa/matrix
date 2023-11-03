package se.alipsa.groovy.spreadsheet.ods;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet
import se.alipsa.groovy.spreadsheet.SpreadsheetUtil
import se.alipsa.groovy.spreadsheet.ValueExtractor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A value extractor specialized in extracting info from a Calc (ods) file
 */
public class OdsValueExtractor extends ValueExtractor {

   private final Sheet sheet;

   public OdsValueExtractor(Sheet sheet) {
      this.sheet = sheet
   }


   public double getDouble(int row, int column) {
      return getDouble(sheet.getRange(row, column))
   }

   public double getDouble(Range range) {
      return getDouble(range.getValue())
   }

   public float getFloat(int row, int column) {
      return (float) getDouble(row, column)
   }

   public int getInt(int row, int column) {
      return getInt(sheet.getRange(row, column))
   }

   int getInt(Range range) {
      return getInt(range.getValue())
   }

   String getString(int row, int column) {
      try {
         return getString(sheet.getRange(row, column))
      } catch (IndexOutOfBoundsException e) {
         throw new IndexOutOfBoundsException("Failed to get String at row $row, col $column: ${e.getMessage()}")
      }
   }

   String getString(Range range) {
      Object val = range.getValue()
      if (val instanceof LocalDateTime) {
         return SpreadsheetUtil.dateTimeFormatter.format((LocalDateTime)val)
      }
      if (val instanceof LocalDate) {
         return SpreadsheetUtil.dateTimeFormatter.format(((LocalDate) val).atStartOfDay())
      }
      return val == null ? null : String.valueOf(val)
   }

   public Long getLong(Range range) {
      return getLong(range.getValue())
   }

   public Long getLong(int row, int column) {
      return getLong(sheet.getRange(row, column))
   }

   public Boolean getBoolean(int row, int column) {
      return getBoolean(sheet.getRange(row, column))
   }

   public Boolean getBoolean(Range range) {
      return getBoolean(range.getValue())
   }
}
