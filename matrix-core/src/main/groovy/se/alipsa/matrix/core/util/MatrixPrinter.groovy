package se.alipsa.matrix.core.util

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

@CompileStatic
class MatrixPrinter {

  /**
   * This method is primarily meant for human viewíng of the content of the matrix
   *
   * @param rows the number of observations to include in the result
   * @param includeHeader optional, defaults to false
   * @param delimiter optional defaults to tab (\t)
   * @param lineEnding optional defaults to newline (\n)
   * @param maxColumnLength optional defaults to 50
   * @return a string representation of each observation up to the rows specified
   */
  static String head(Matrix m, int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, m.rowCount())
    List<String> colNames = m.columnNames()
    List<Integer> columnLengths = colNames.collect {
      colName -> m.maxContentLength(colName, includeHeader, maxColumnLength)
    }

    if (includeHeader) {
      List<String> headerRow = padRow(m, colNames, columnLengths)
      sb.append(String.join(delimiter, headerRow)).append(lineEnding)
    }

    for (int i = 0; i < nRows; i++) {
      List<String> stringRow = padRow(m, m.row(i), columnLengths)
      sb.append(String.join(delimiter, stringRow)).append(lineEnding)
    }
    return sb.toString()
  }

  static String tail(Matrix m, int rows, boolean includeHeader = true, String delimiter = '\t', String lineEnding = '\n', int maxColumnLength = 50) {
    StringBuilder sb = new StringBuilder()
    def nRows = Math.min(rows, m.rowCount())
    def headers = m.columnNames()
    if (includeHeader) {
      sb.append(String.join(delimiter, headers)).append(lineEnding)
    }
    List<Integer> columnLengths = headers.collect { colName -> m.maxContentLength(colName, includeHeader, maxColumnLength) }
    for (int i = m.rowCount() - nRows; i < m.rowCount(); i++) {
      List<String> stringRow = padRow(m, m.row(i), columnLengths)
      sb.append(String.join(delimiter, stringRow)).append(lineEnding)
    }
    return sb.toString()
  }

  /**
   * Used to pretty print (e.g. to console) a row
   */
  static List<String> padRow(Matrix m, List row, List<Integer> columnLengths) {
    List<String> stringRow = []
    for (int c = 0; c < m.columns().size(); c++) {
      def val = row[c]
      def strVal = String.valueOf(val)
      int columnLength = columnLengths[c]
      if (strVal.length() > columnLength) {
        strVal = strVal.substring(0, columnLength)
      }
      def valType = m.type(c)
      if (val instanceof Number || (valType != null && Number.isAssignableFrom(valType))) {
        strVal = strVal.padLeft(columnLength)
      } else {
        strVal = strVal.padRight(columnLength)
      }
      stringRow << strVal
    }
    stringRow
  }
}
