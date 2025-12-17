package se.alipsa.matrix.smile


import groovy.transform.CompileStatic
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import smile.data.DataFrame
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
      // TODO: if we have nulls use ofNullable variants, if no nulls use the of variants
      if (dataType == Float.class) {
        columns.add(ValueVector.ofNullable(colName, columnData as Float[]))
      } else if (dataType == Double.class) {
        columns.add(ValueVector.ofNullable(colName, columnData as Double[]))
      } else if (dataType == Integer.class) {
        columns.add(ValueVector.ofNullable(colName, columnData as Integer[]))
      } else if (dataType == String.class) {
        columns.add(ValueVector.of(colName, columnData as String[]))
        // TODO add BooleanVector/NullableBooleanVector,
        //  CharVector/NullableCharVector
        //  ByteVector/NullableByteVector
        //  ShortVector/NullableShortVector
        //  LongVector/NullableLongVector
        //  NumberVector<BigDecimal>
        //  ObjectVector<Timestamp>
        //  ObjectVector<Instant>
        //  ObjectVector<LocalDateTime>
        //  ObjectVector<ZonedDateTime>
        //  ObjectVector<LocalDate>
        //  ObjectVector<LocalTime>
        //  ObjectVector<OffsetTime>
        //  ValueVector nominal (of enums)?
      } else {
        // Handle other types (Boolean, Date, Long, etc.) or default to StringVector
        System.err.println("Warning: Unhandled data type " + dataType.getSimpleName() +
            " for column " + colName + ". Defaulting to StringVector.")
        List<String> values = ListConverter.convert(columnData, String)
        columns.add(ValueVector.of(colName, values as String[]))
      }
    }

    // 5. Create the Smile DataFrame from the ValueVectors
    return new DataFrame(columns as ValueVector[])
  }
}
