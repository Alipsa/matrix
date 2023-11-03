package se.alipsa.groovy.spreadsheet

import org.apache.poi.ss.util.WorkbookUtil;

import java.time.format.DateTimeFormatter;

/**
 * Common spreadsheet utilities
 */
public class SpreadsheetUtil {

   public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

   private SpreadsheetUtil() {
      // prevent instantiation
   }

   /**
    * Convert a column name to its equivalent index
    * @param name the column name to convert e.g. "A"
    * @return the column number (1 indexed) corresponding to the name (A == 1, N == 14 etc.)
    */
   static int asColumnNumber(String name) {
      if (name == null) {
         return 0;
      }
      String colName = name.toUpperCase();
      int number = 0;
      for (int i = 0; i < colName.length(); i++) {
         number = number * 26 + (colName.charAt(i) - ('A' as char - 1))
      }
      return number;
   }

   /**
    * Convert a column number to ite equivalent name
    * @param number the 1 indexed number to convert
    * @return the corresponding name of the index (1 == A, 14 == N etc.)
    */
   static String asColumnName(int number) {
      StringBuilder sb = new StringBuilder();
      while (number-- > 0) {
         sb.append(('A' as char + (number % 26)) as char)
         number /= 26;
      }
      return sb.reverse().toString()
   }

   static String createValidSheetName(String suggestion) {
      return WorkbookUtil.createSafeSheetName(suggestion)
   }
}
