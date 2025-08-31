package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic

import java.time.format.DateTimeFormatter

/**
 * Common spreadsheet utilities
 */
@CompileStatic
class SpreadsheetUtil {

   public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

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
         return 0
      }
      String colName = name.toUpperCase()
      int number = 0
      for (int i = 0; i < colName.length(); i++) {
         number = number * 26 + (colName.charAt(i) - ('A' as char - 1))
      }
      return number
   }

   /**
    * Convert a column number to ite equivalent name
    * @param number the 1 indexed number to convert
    * @return the corresponding name of the index (1 == A, 14 == N etc.)
    */
   static String asColumnName(int number) {
      StringBuilder sb = new StringBuilder()
      while (number-- > 0) {
         sb.append(('A' as char + (number % 26)) as char)
         number = (int) (number / 26)
      }
      return sb.reverse().toString()
   }

   static String createValidSheetName(String suggestion) {
      return createSafeSheetName(suggestion)
   }

   static List<String> createColumnNames(int startCol, int endCol) {
      createColumnNames(endCol - startCol + 1)
   }

   static List<String> createColumnNames(int nCols) {
      List<String> header = []
      for (int i = 1; i <= nCols; i++) {
         header.add("c$i".toString())
      }
      header
   }

   private final static String createSafeSheetName(final String nameProposal, char replaceChar = ' ' as char) {
      if (nameProposal == null) {
         return "null"
      }
      if (nameProposal.length() < 1) {
         return "empty"
      }
      final int length = Math.min(31, nameProposal.length())
      final String shortname = nameProposal.substring(0, length)
      final StringBuilder result = new StringBuilder(shortname)
      for (int i=0; i<length; i++) {
         char ch = result.charAt(i)
         switch (ch) {
            case '\u0000', '\u0003', ':', '/', '\\', '?', '*', ']', '[' ->
               result.setCharAt(i, replaceChar)
            case '\'' ->
               if (i==0 || i==length-1) {
                  result.setCharAt(i, replaceChar);
               }
            default -> result.setCharAt(i, ch)
         }
      }
      return result.toString()
   }
}
