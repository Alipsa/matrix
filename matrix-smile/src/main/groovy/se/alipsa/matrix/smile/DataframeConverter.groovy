package se.alipsa.matrix.smile

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

import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZonedDateTime

/**
 * Converts between {@link Matrix} and Smile's {@link DataFrame}.
 */
class DataframeConverter {

  private static final Logger log = Logger.getLogger(DataframeConverter)

  /**
   * Converts a Smile {@link DataFrame} to a Matrix, preserving its column names and types.
   *
   * @param dataFrame the Smile data frame to convert
   * @return a Matrix containing the data frame's rows and columns
   */
  static Matrix convert(DataFrame dataFrame) {
    def columnNames = dataFrame.schema().fields()*.name()
    def rowCount = dataFrame.nrow()
    Map<String, List<Object>> data = [:]
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
    Matrix.builder().data(data).types(types).build()
  }

  /**
   * Converts a Matrix to a Smile {@link DataFrame}.
   *
   * @param matrix the Matrix to convert
   * @return a Smile data frame containing the Matrix columns
   */
  static DataFrame convert(Matrix matrix) {
    int numCols = matrix.columnCount()
    String[] colNames = matrix.columnNames()

    List<ValueVector> columns = []

    for (int j = 0; j < numCols; j++) {
      columns.add(toValueVector(colNames[j], matrix.type(j), matrix.column(j)))
    }

    // Create the Smile DataFrame from the ValueVectors
    new DataFrame(columns as ValueVector[])
  }

  /**
   * Builds a single Smile {@link ValueVector} for one Matrix column.
   * Uses {@code of()} variants (primitive arrays) when no nulls are present for better
   * performance, and {@code ofNullable()} variants when nulls are present.
   */
  private static ValueVector toValueVector(String colName, Class<?> dataType, List<Object> columnData) {
    boolean hasNulls = SmileUtil.hasNulls(columnData)
    switch (dataType) {
      case Float -> floatVector(colName, columnData, hasNulls)
      case float -> ValueVector.of(colName, columnData as float[])
      case Double -> doubleVector(colName, columnData, hasNulls)
      case double -> ValueVector.of(colName, columnData as double[])
      case Integer -> intVector(colName, columnData, hasNulls)
      case int -> ValueVector.of(colName, columnData as int[])
      case String -> ValueVector.of(colName, columnData as String[])
      case Boolean -> booleanVector(colName, columnData, hasNulls)
      case boolean -> ValueVector.of(colName, columnData as boolean[])
      case Character -> charVector(colName, columnData, hasNulls)
      case char -> ValueVector.of(colName, columnData as char[])
      case Byte -> byteVector(colName, columnData, hasNulls)
      case byte -> ValueVector.of(colName, columnData as byte[])
      case Short -> shortVector(colName, columnData, hasNulls)
      case short -> ValueVector.of(colName, columnData as short[])
      case Long -> longVector(colName, columnData, hasNulls)
      case long -> ValueVector.of(colName, columnData as long[])
      case BigDecimal, BigInteger -> ValueVector.of(colName, columnData as BigDecimal[])
      case Timestamp -> ValueVector.of(colName, columnData as Timestamp[])
      case Instant -> ValueVector.of(colName, columnData as Instant[])
      case LocalDateTime -> ValueVector.of(colName, columnData as LocalDateTime[])
      case ZonedDateTime -> ValueVector.of(colName, columnData as ZonedDateTime[])
      case LocalDate -> ValueVector.of(colName, columnData as LocalDate[])
      case LocalTime -> ValueVector.of(colName, columnData as LocalTime[])
      case OffsetTime -> ValueVector.of(colName, columnData as OffsetTime[])
      case Number -> numberVector(colName, columnData, hasNulls)
      case Enum -> ValueVector.nominal(colName, columnData as Enum[])
      default -> defaultVector(colName, dataType, columnData)
    }
  }

  private static ValueVector floatVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Float[]) :
        ValueVector.of(colName, toPrimitiveFloatArray(columnData))
  }

  private static ValueVector doubleVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Double[]) :
        ValueVector.of(colName, toPrimitiveDoubleArray(columnData))
  }

  private static ValueVector intVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Integer[]) :
        ValueVector.of(colName, toPrimitiveIntArray(columnData))
  }

  private static ValueVector booleanVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Boolean[]) :
        ValueVector.of(colName, toPrimitiveBooleanArray(columnData))
  }

  private static ValueVector charVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Character[]) :
        ValueVector.of(colName, toPrimitiveCharArray(columnData))
  }

  private static ValueVector byteVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Byte[]) :
        ValueVector.of(colName, toPrimitiveByteArray(columnData))
  }

  private static ValueVector shortVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Short[]) :
        ValueVector.of(colName, toPrimitiveShortArray(columnData))
  }

  private static ValueVector longVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, columnData as Long[]) :
        ValueVector.of(colName, toPrimitiveLongArray(columnData))
  }

  private static ValueVector numberVector(String colName, List<Object> columnData, boolean hasNulls) {
    hasNulls ? ValueVector.ofNullable(colName, toNullableDoubleArray(columnData)) :
        ValueVector.of(colName, toPrimitiveDoubleArray(columnData))
  }

  private static Double[] toNullableDoubleArray(List<Object> columnData) {
    columnData.collect { it != null ? it as Double : null } as Double[]
  }

  private static ValueVector defaultVector(String colName, Class<?> dataType, List<Object> columnData) {
    log.warn("Unhandled data type ${dataType.getSimpleName()} for column ${colName}; defaulting to String")
    List<String> values = ListConverter.convert(columnData, String)
    ValueVector.of(colName, values as String[])
  }

  /**
   * Convert List to primitive float array.
   */
  private static float[] toPrimitiveFloatArray(List<?> list) {
    float[] result = new float[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as float
    }
    result
  }

  /**
   * Convert List to primitive double array.
   */
  private static double[] toPrimitiveDoubleArray(List<?> list) {
    double[] result = new double[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as double
    }
    result
  }

  /**
   * Convert List to primitive int array.
   */
  private static int[] toPrimitiveIntArray(List<?> list) {
    int[] result = new int[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as int
    }
    result
  }

  /**
   * Convert List to primitive long array.
   */
  private static long[] toPrimitiveLongArray(List<?> list) {
    long[] result = new long[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as long
    }
    result
  }

  /**
   * Convert List to primitive short array.
   */
  private static short[] toPrimitiveShortArray(List<?> list) {
    short[] result = new short[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as short
    }
    result
  }

  /**
   * Convert List to primitive byte array.
   */
  private static byte[] toPrimitiveByteArray(List<?> list) {
    byte[] result = new byte[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = list.get(i) as byte
    }
    result
  }

  /**
   * Convert List to primitive boolean array.
   */
  private static boolean[] toPrimitiveBooleanArray(List<?> list) {
    boolean[] result = new boolean[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = (Boolean) list.get(i)
    }
    result
  }

  /**
   * Convert List to primitive char array.
   */
  private static char[] toPrimitiveCharArray(List<?> list) {
    char[] result = new char[list.size()]
    for (int i = 0; i < list.size(); i++) {
      result[i] = (Character) list.get(i)
    }
    result
  }

  static Class getType(DataType dataType) {
    switch (dataType) {
      case FloatType -> Float
      case DoubleType -> Double
      case IntType -> Integer
      case StringType -> String
      case BooleanType -> Boolean
      case CharType -> Character
      case ByteType -> Byte
      case ShortType -> Short
      case LongType -> Long
      case DecimalType -> BigDecimal
      case DateTimeType -> LocalDateTime
      case DateType -> LocalDate
      case TimeType -> LocalTime
      default -> {
        log.warn("Unhandled Smile DataType ${dataType}; defaulting to Object")
        Object
      }
    }
  }
}
