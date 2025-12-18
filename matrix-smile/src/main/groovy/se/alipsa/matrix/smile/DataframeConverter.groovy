package se.alipsa.matrix.smile

import com.sun.jdi.IntegerType
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import smile.data.DataFrame
import smile.data.type.BooleanType
import smile.data.type.ByteType
import smile.data.type.CharType
import smile.data.type.DataType
import smile.data.type.DateTimeType
import smile.data.type.DateType
import smile.data.type.DecimalType
import smile.data.type.DoubleType
import smile.data.type.FloatType
import smile.data.type.IntType
import smile.data.type.LongType
import smile.data.type.ShortType
import smile.data.type.StringType
import smile.data.type.TimeType
import smile.data.vector.ValueVector

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZonedDateTime

@CompileStatic
class DataframeConverter {

  private static Logger log = LoggerFactory.getLogger(DataframeConverter.class)

  static Matrix convert(DataFrame dataFrame) {
    def columnNames = dataFrame.schema().fields().collect { it.name() }
    def rowCount = dataFrame.nrow()
    Map<String, List<Object>> data = new LinkedHashMap<String, List<Object>>()
    for (String colName : columnNames) {
      data.put(colName, new ArrayList<Object>(rowCount))
    }
    List<Class> types = []
    for (dataType in dataFrame.dtypes()) {
      types << getType(dataType)
    }
    for (int r = 0; r < rowCount; r++) {
      def row = dataFrame.get(r)
      for (String colName : columnNames) {
        data.get(colName).add(row.get(colName))
      }
    }
    return Matrix.builder().data(data).types(types).build()
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
      switch (dataType) {
        case Float -> columns.add(ValueVector.ofNullable(colName, columnData as Float[]))
        case float -> columns.add(ValueVector.of(colName, columnData as float[]))
        case Double -> columns.add(ValueVector.ofNullable(colName, columnData as Double[]))
        case double -> columns.add(ValueVector.of(colName, columnData as double[]))
        case Integer -> columns.add(ValueVector.ofNullable(colName, columnData as Integer[]))
        case int -> columns.add(ValueVector.of(colName, columnData as int[]))
        case String -> columns.add(ValueVector.of(colName, columnData as String[]))
        case Boolean -> columns.add(ValueVector.ofNullable(colName, columnData as Boolean[]))
        case boolean -> columns.add(ValueVector.of(colName, columnData as boolean[]))
        case Character -> columns.add(ValueVector.ofNullable(colName, columnData as Character[]))
        case char -> columns.add(ValueVector.of(colName, columnData as char[]))
        case Byte -> columns.add(ValueVector.ofNullable(colName, columnData as Byte[]))
        case byte -> columns.add(ValueVector.of(colName, columnData as byte[]))
        case Short -> columns.add(ValueVector.ofNullable(colName, columnData as Short[]))
        case short -> columns.add(ValueVector.of(colName, columnData as short[]))
        case Long -> columns.add(ValueVector.ofNullable(colName, columnData as Long[]))
        case long -> columns.add(ValueVector.of(colName, columnData as long[]))
        case BigDecimal, BigInteger -> columns.add(ValueVector.of(colName, columnData as BigDecimal[]))
        case Timestamp -> columns.add(ValueVector.of(colName, columnData as Timestamp[]))
        case Instant -> columns.add(ValueVector.of(colName, columnData as Instant[]))
        case LocalDateTime -> columns.add(ValueVector.of(colName, columnData as LocalDateTime[]))
        case ZonedDateTime -> columns.add(ValueVector.of(colName, columnData as ZonedDateTime[]))
        case LocalDate -> columns.add(ValueVector.of(colName, columnData as LocalDate[]))
        case LocalTime -> columns.add(ValueVector.of(colName, columnData as LocalTime[]))
        case OffsetTime -> columns.add(ValueVector.of(colName, columnData as OffsetTime[]))
        case Enum -> columns.add(ValueVector.nominal(colName, columnData as Enum[]))
        default -> {
          // Handle other types (Boolean, Date, Long, etc.) or default to StringVector
          log.warn("Warning: Unhandled data type " + dataType.getSimpleName() +
              " for column " + colName + ". Defaulting to StringVector.")
          List<String> values = ListConverter.convert(columnData, String)
          columns.add(ValueVector.of(colName, values as String[]))
        }
      }
    }

    // 5. Create the Smile DataFrame from the ValueVectors
    return new DataFrame(columns as ValueVector[])
  }

  static Class getType(DataType dataType) {

    return switch (dataType) {
      case FloatType -> Float
      case DoubleType -> Double
      case IntegerType, IntType -> Integer
      case StringType -> String
      case BooleanType -> Boolean
      case CharType -> Character
      case ByteType -> Byte.class
      case ShortType -> Short.class
      case LongType -> Long.class
      case DecimalType -> BigDecimal
      case DateTimeType -> LocalDateTime
      case DateType -> LocalDate
      case TimeType -> LocalTime
      default -> {
        log.warn("Warning: Unhandled Smile DataType " + dataType + ". Defaulting to Object.class")
        Object.class
      }
    }
  }
}
