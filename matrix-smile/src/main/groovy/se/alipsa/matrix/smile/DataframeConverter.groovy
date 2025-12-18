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

      // Check if column contains nulls - use optimized primitive arrays when no nulls
      boolean hasNulls = containsNull(columnData)

      // Create the appropriate Smile ValueVector based on the type
      // Use of() variants (primitive arrays) when no nulls for better performance
      // Use ofNullable() variants when nulls are present
      switch (dataType) {
        case Float -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Float[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveFloatArray(columnData)))
          }
        }
        case float -> columns.add(ValueVector.of(colName, columnData as float[]))
        case Double -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Double[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveDoubleArray(columnData)))
          }
        }
        case double -> columns.add(ValueVector.of(colName, columnData as double[]))
        case Integer -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Integer[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveIntArray(columnData)))
          }
        }
        case int -> columns.add(ValueVector.of(colName, columnData as int[]))
        case String -> columns.add(ValueVector.of(colName, columnData as String[]))
        case Boolean -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Boolean[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveBooleanArray(columnData)))
          }
        }
        case boolean -> columns.add(ValueVector.of(colName, columnData as boolean[]))
        case Character -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Character[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveCharArray(columnData)))
          }
        }
        case char -> columns.add(ValueVector.of(colName, columnData as char[]))
        case Byte -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Byte[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveByteArray(columnData)))
          }
        }
        case byte -> columns.add(ValueVector.of(colName, columnData as byte[]))
        case Short -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Short[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveShortArray(columnData)))
          }
        }
        case short -> columns.add(ValueVector.of(colName, columnData as short[]))
        case Long -> {
          if (hasNulls) {
            columns.add(ValueVector.ofNullable(colName, columnData as Long[]))
          } else {
            columns.add(ValueVector.of(colName, toPrimitiveLongArray(columnData)))
          }
        }
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
          // Handle other types or default to StringVector
          log.warn("Warning: Unhandled data type " + dataType.getSimpleName() +
              " for column " + colName + ". Defaulting to StringVector.")
          List<String> values = ListConverter.convert(columnData, String)
          columns.add(ValueVector.of(colName, values as String[]))
        }
      }
    }

    // Create the Smile DataFrame from the ValueVectors
    return new DataFrame(columns as ValueVector[])
  }

  /**
   * Check if a list contains any null values.
   */
  private static boolean containsNull(List<?> list) {
    for (Object item : list) {
      if (item == null) {
        return true
      }
    }
    return false
  }

  /**
   * Convert List to primitive float array.
   */
  private static float[] toPrimitiveFloatArray(List<?> list) {
    float[] result = new float[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).floatValue()
    }
    return result
  }

  /**
   * Convert List to primitive double array.
   */
  private static double[] toPrimitiveDoubleArray(List<?> list) {
    double[] result = new double[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).doubleValue()
    }
    return result
  }

  /**
   * Convert List to primitive int array.
   */
  private static int[] toPrimitiveIntArray(List<?> list) {
    int[] result = new int[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).intValue()
    }
    return result
  }

  /**
   * Convert List to primitive long array.
   */
  private static long[] toPrimitiveLongArray(List<?> list) {
    long[] result = new long[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).longValue()
    }
    return result
  }

  /**
   * Convert List to primitive short array.
   */
  private static short[] toPrimitiveShortArray(List<?> list) {
    short[] result = new short[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).shortValue()
    }
    return result
  }

  /**
   * Convert List to primitive byte array.
   */
  private static byte[] toPrimitiveByteArray(List<?> list) {
    byte[] result = new byte[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = ((Number) list.get(i)).byteValue()
    }
    return result
  }

  /**
   * Convert List to primitive boolean array.
   */
  private static boolean[] toPrimitiveBooleanArray(List<?> list) {
    boolean[] result = new boolean[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = (Boolean) list.get(i)
    }
    return result
  }

  /**
   * Convert List to primitive char array.
   */
  private static char[] toPrimitiveCharArray(List<?> list) {
    char[] result = new char[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = (Character) list.get(i)
    }
    return result
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
