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
   *
   * @param data list of matrices
   * @param sheetNames list of sheet names corresponding to the matrices
   * @return a linked map of safe sheet names to matrices
   */
  static Map<String, Matrix> buildRequestedMap(List<Matrix> data, List<String> sheetNames) {
    if (data.size() != sheetNames.size()) {
      throw new IllegalArgumentException("Data and sheetNames lists must have the same size")
    }
    Map<String, Matrix> requested = new LinkedHashMap<>()
    for (int i = 0; i < data.size(); i++) {
      String name = SpreadsheetUtil.createValidSheetName(sheetNames.get(i))
      requested.put(name, data.get(i))
    }
    requested
  }
}
