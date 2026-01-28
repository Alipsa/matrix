package se.alipsa.matrix.smile

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import smile.data.DataFrame
import smile.data.vector.ValueVector

/**
 * Groovy extension class that adds convenience methods to Matrix and Smile DataFrame.
 * Enables Groovy idioms like:
 * <pre>
 * // Convert Matrix to DataFrame
 * def df = matrix.toSmileDataFrame()
 *
 * // Convert DataFrame to Matrix
 * def matrix = df.toMatrix()
 *
 * // Slicing DataFrame with Groovy syntax
 * def subset = df[0..10, 'name', 'value']
 * def row = df[5]
 * def column = df['age']
 * </pre>
 *
 * Registered in resources/META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule
 */
@CompileStatic
class Gsmile {

  // ==================== Matrix Extensions ====================

  /**
   * Convert a Matrix to a Smile DataFrame.
   *
   * @param self the Matrix to convert
   * @return a Smile DataFrame containing the same data
   */
  static DataFrame toSmileDataFrame(Matrix self) {
    return SmileUtil.toDataFrame(self)
  }

  /**
   * Get a statistical summary of the Matrix using Smile.
   *
   * @param self the Matrix to describe
   * @return a Matrix with statistical summary (count, mean, std, min, quartiles, max)
   */
  static Matrix smileDescribe(Matrix self) {
    return SmileUtil.describe(self)
  }

  /**
   * Take a random sample from the Matrix.
   *
   * @param self the Matrix to sample from
   * @param n the number of rows to sample
   * @return a new Matrix with randomly selected rows
   */
  static Matrix smileSample(Matrix self, int n) {
    return SmileUtil.sample(self, n)
  }

  // ==================== DataFrame Extensions ====================

  /**
   * Convert a Smile DataFrame to a Matrix.
   *
   * @param self the DataFrame to convert
   * @return a Matrix containing the same data
   */
  static Matrix toMatrix(DataFrame self) {
    return SmileUtil.toMatrix(self)
  }

  /**
   * Convert a Smile DataFrame to a Matrix with a specified name.
   *
   * @param self the DataFrame to convert
   * @param name the name for the resulting Matrix
   * @return a Matrix containing the same data
   */
  static Matrix toMatrix(DataFrame self, String name) {
    return SmileUtil.toMatrix(self).withMatrixName(name)
  }

  /**
   * Get a single row from the DataFrame as a Map.
   *
   * @param self the DataFrame
   * @param rowIndex the row index
   * @return a Map with column names as keys and row values as values
   */
  static Map<String, Object> getAt(DataFrame self, int rowIndex) {
    Map<String, Object> row = new LinkedHashMap<>()
    for (int i = 0; i < self.ncol(); i++) {
      String colName = self.column(i).name()
      row.put(colName, self.get(rowIndex, i))
    }
    return row
  }

  /**
   * Get a column from the DataFrame by name.
   *
   * @param self the DataFrame
   * @param columnName the column name
   * @return the column as a ValueVector
   */
  static ValueVector getAt(DataFrame self, String columnName) {
    return self.column(columnName)
  }

  /**
   * Slice the DataFrame using a range of row indices.
   * Usage: df[0..10]
   *
   * @param self the DataFrame
   * @param range the row range (inclusive)
   * @return a new DataFrame with the selected rows
   */
  static DataFrame getAt(DataFrame self, IntRange range) {
    return sliceRows(self, range.getFromInt(), range.getToInt() + 1)
  }

  /**
   * Select specific columns from the DataFrame.
   * Usage: df['col1', 'col2', 'col3']
   *
   * @param self the DataFrame
   * @param columnNames the column names to select
   * @return a new DataFrame with only the selected columns
   */
  static DataFrame getAt(DataFrame self, List<String> columnNames) {
    return self.select(columnNames as String[])
  }

  /**
   * Slice the DataFrame with row range and column selection.
   * Usage: df[0..10, 'name', 'value'] or df[0..10, ['name', 'value']]
   *
   * @param self the DataFrame
   * @param args the slicing arguments (first is row range, rest are column names)
   * @return a new DataFrame with the selected subset
   */
  static DataFrame getAt(DataFrame self, Collection args) {
    if (args.isEmpty()) {
      return self
    }

    def first = args[0]

    // If first argument is IntRange, it's row slicing
    if (first instanceof IntRange) {
      IntRange rowRange = (IntRange) first
      DataFrame sliced = sliceRows(self, rowRange.getFromInt(), rowRange.getToInt() + 1)

      // If there are more arguments, they are column names
      if (args.size() > 1) {
        List<String> columnNames = []
        for (int i = 1; i < args.size(); i++) {
          def arg = args[i]
          if (arg instanceof String) {
            columnNames << (String) arg
          } else if (arg instanceof List) {
            columnNames.addAll((List<String>) arg)
          }
        }
        if (!columnNames.isEmpty()) {
          return sliced.select(columnNames as String[])
        }
      }
      return sliced
    }

    // If first argument is String, it's column selection
    if (first instanceof String) {
      List<String> columnNames = args.collect { it.toString() }
      return self.select(columnNames as String[])
    }

    // If first argument is Integer, it's a single row
    if (first instanceof Integer) {
      int rowIndex = (Integer) first
      return sliceRows(self, rowIndex, rowIndex + 1)
    }

    throw new IllegalArgumentException("Unsupported getAt arguments: $args")
  }

  /**
   * Get the number of rows in the DataFrame.
   * Alias for nrow() to match Matrix API.
   *
   * @param self the DataFrame
   * @return the number of rows
   */
  static int rowCount(DataFrame self) {
    return self.nrow()
  }

  /**
   * Get the number of columns in the DataFrame.
   * Alias for ncol() to match Matrix API.
   *
   * @param self the DataFrame
   * @return the number of columns
   */
  static int columnCount(DataFrame self) {
    return self.ncol()
  }

  /**
   * Get the column names of the DataFrame.
   *
   * @param self the DataFrame
   * @return a list of column names
   */
  static List<String> columnNames(DataFrame self) {
    List<String> names = []
    for (int i = 0; i < self.ncol(); i++) {
      names << self.column(i).name()
    }
    return names
  }

  /**
   * Print a summary of the DataFrame structure.
   *
   * @param self the DataFrame
   * @return a string describing the DataFrame structure
   */
  static String structure(DataFrame self) {
    StringBuilder sb = new StringBuilder()
    sb.append("DataFrame: ${self.nrow()} rows x ${self.ncol()} columns\n")
    sb.append("Columns:\n")
    for (int i = 0; i < self.ncol(); i++) {
      def col = self.column(i)
      sb.append("  ${col.name()}: ${col.dtype()}\n")
    }
    return sb.toString()
  }

  /**
   * Get the first n rows of the DataFrame.
   *
   * @param self the DataFrame
   * @param n number of rows to return
   * @return a new DataFrame with the first n rows
   */
  static DataFrame head(DataFrame self, int n = 5) {
    int rows = Math.min(n, self.nrow())
    return sliceRows(self, 0, rows)
  }

  /**
   * Get the last n rows of the DataFrame.
   *
   * @param self the DataFrame
   * @param n number of rows to return
   * @return a new DataFrame with the last n rows
   */
  static DataFrame tail(DataFrame self, int n = 5) {
    int rows = Math.min(n, self.nrow())
    int start = self.nrow() - rows
    return sliceRows(self, start, self.nrow())
  }

  /**
   * Filter the DataFrame using a closure predicate.
   * The closure receives a Map representing each row.
   *
   * Usage: df.filter { it['age'] > 30 }
   *
   * @param self the DataFrame
   * @param predicate a closure that returns true for rows to keep
   * @return a new DataFrame with filtered rows
   */
  static DataFrame filter(DataFrame self, Closure<Boolean> predicate) {
    List<Integer> keepIndices = []
    for (int i = 0; i < self.nrow(); i++) {
      Map<String, Object> row = getAt(self, i)
      if (predicate.call(row)) {
        keepIndices << i
      }
    }

    if (keepIndices.isEmpty()) {
      // Return empty DataFrame with same schema
      return sliceRows(self, 0, 0)
    }

    // Build result by selecting rows via Matrix conversion
    Matrix matrix = toMatrix(self)
    Matrix filtered = Matrix.builder()
        .rows(matrix.rows(keepIndices) as List<List>)
        .columnNames(matrix.columnNames() as List<String>)
        .types(matrix.types())
        .build()
    return SmileUtil.toDataFrame(filtered)
  }

  /**
   * Iterate over each row of the DataFrame.
   * The closure receives a Map representing each row.
   *
   * Usage: df.eachRow { row -> log.info("Processing: ${row['name']}") }
   *
   * @param self the DataFrame
   * @param action a closure to execute for each row
   */
  static void eachRow(DataFrame self, Closure action) {
    for (int i = 0; i < self.nrow(); i++) {
      Map<String, Object> row = getAt(self, i)
      action.call(row)
    }
  }

  /**
   * Collect values from each row using a closure.
   *
   * Usage: def names = df.collectRows { it['name'] }
   *
   * @param self the DataFrame
   * @param transform a closure to transform each row
   * @return a list of transformed values
   */
  static <T> List<T> collectRows(DataFrame self, Closure<T> transform) {
    List<T> result = []
    for (int i = 0; i < self.nrow(); i++) {
      Map<String, Object> row = getAt(self, i)
      result << transform.call(row)
    }
    return result
  }

  // ==================== Private Helper Methods ====================

  /**
   * Slice rows from a DataFrame by converting through Matrix.
   * This is a workaround since Smile DataFrame doesn't have a direct slice method.
   *
   * @param df the source DataFrame
   * @param from start index (inclusive)
   * @param to end index (exclusive)
   * @return a new DataFrame with the selected rows
   */
  private static DataFrame sliceRows(DataFrame df, int from, int to) {
    if (from >= to || from >= df.nrow()) {
      // Return empty DataFrame - convert through Matrix to preserve schema
      Matrix matrix = SmileUtil.toMatrix(df)
      Matrix empty = Matrix.builder()
          .columnNames(matrix.columnNames() as List<String>)
          .types(matrix.types())
          .build()
      return SmileUtil.toDataFrame(empty)
    }

    // Clamp indices
    int start = Math.max(0, from)
    int end = Math.min(to, df.nrow())

    // Use boolean indexing
    boolean[] mask = new boolean[df.nrow()]
    for (int i = start; i < end; i++) {
      mask[i] = true
    }
    return df.get(mask)
  }
}
