package se.alipsa.matrix.spreadsheet

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix

/**
 * Shared helpers for spreadsheet writers/appenders.
 */
@CompileStatic
final class SpreadsheetWriteUtil {

  private SpreadsheetWriteUtil() {
    // utility class
  }

  /**
   * Build a map of safe sheet names to matrices, preserving order.
   * Handles collisions after sanitization by appending numeric suffixes.
   *
   * @param data list of matrices
   * @param sheetNames list of sheet names corresponding to the matrices
   * @return a linked map of safe sheet names to matrices
   */
  static Map<String, Matrix> buildRequestedMap(List<Matrix> data, List<String> sheetNames) {
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Data and sheetNames lists must have the same size")
    }
    List<String> uniqueNames = SpreadsheetUtil.createUniqueSheetNames(sheetNames)
    Map<String, Matrix> requested = new LinkedHashMap<>()
    for (int i = 0; i < data.size(); i++) {
      requested.put(uniqueNames.get(i), data.get(i))
    }
    requested
  }
}
