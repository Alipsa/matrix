package se.alipsa.matrix.smile

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import smile.data.DataFrame
import smile.data.vector.DoubleVector
import smile.data.vector.IntVector
import smile.data.vector.StringVector
import smile.data.vector.ValueVector

@CompileStatic
class DataframeConverter {

  static Matrix convert(DataFrame dataFrame) {
    def columnNames = dataFrame.schema().fields().collect { it.name() }
    def rowCount = dataFrame.nrow()
    Map<String, List<Object>> data = new LinkedHashMap<String, List<Object>>()
    for (String colName : columnNames) {
      data.put(colName, new ArrayList<Object>(rowCount))
    }
    for (int r = 0; r < rowCount; r++) {
      def row = dataFrame.get(r)
      for (String colName : columnNames) {
        data.get(colName).add(row.get(colName))
      }
    }
    return Matrix.builder().data(data).build()
  }

  @CompileDynamic
  static DataFrame convert(Matrix matrix) {
    int numCols = matrix.columnCount()
    String[] colNames = matrix.columnNames()

    List<ValueVector> columns = new ArrayList<>()

    for (int j = 0; j < numCols; j++) {
      String colName = colNames[j]
      Class<?> dataType = matrix.type(j)

      // Extract all data from the column
      List<Object> columnData = matrix.column(j)

      // 4. Create the appropriate Smile ValueVector based on the type
      if (dataType == Double.class) {
        columns.add(DoubleVector.of(colName, columnData as double[]))
      } else if (dataType == Integer.class) {
        columns.add(IntVector.of(colName, columnData as int[]))
      } else if (dataType == String.class) {
        columns.add(StringVector.of(colName, columnData as String[]))
      } else {
        // Handle other types (Boolean, Date, Long, etc.) or default to StringVector
        System.err.println("Warning: Unhandled data type " + dataType.getSimpleName() +
            " for column " + colName + ". Defaulting to StringVector.")
        List<String> values = ListConverter.convert(columnData, String)
        columns.add(StringVector.of(colName, values as String[]))
      }
    }

    // 5. Create the Smile DataFrame from the ValueVectors
    return DataFrame.of(columns as ValueVector[])
  }
}
