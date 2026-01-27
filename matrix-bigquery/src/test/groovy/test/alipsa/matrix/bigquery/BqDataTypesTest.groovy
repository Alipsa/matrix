package test.alipsa.matrix.bigquery

import com.google.cloud.bigquery.StandardSQLTypeName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import se.alipsa.matrix.bigquery.TypeMapper
import se.alipsa.matrix.core.Column
import se.alipsa.matrix.core.util.Logger

import java.math.RoundingMode

import static org.junit.jupiter.api.Assertions.*

import groovy.transform.CompileStatic
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

import java.sql.Time
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Tag("external")
@CompileStatic
class BqDataTypesTest {

  private static final Logger log = Logger.getLogger(BqDataTypesTest)
  static String datasetName = 'BqDataTypesTest'
  @BeforeAll
  static void setup() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq(true)  // Use async queries for production BigQuery

    if (!bq.datasetExist(datasetName)) {
      bq.createDataset(datasetName)
    }
    assertTrue(bq.datasetExist(datasetName))
  }

  @AfterAll
  static void tearDown() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq(true)  // Use async queries for production BigQuery
    if (bq.datasetExist(datasetName)) {
      bq.dropDataset(datasetName)
    }
    assertFalse(bq.datasetExist(datasetName))
  }

  // === Number Classes ===
  List<Byte> byteList = ListConverter.toBytes(10, 20, 30, 40)
  List<Short> shortList = ListConverter.toShorts(5000, 6000, 7000, 8000)
  List<Integer> integerList = [10000, 20000, 30000, 40000]
  List<Long> longList = [12345678901L, 23456789012L, 34567890123L, 45678901234L]
  List<Float> floatList = [1.1f, 2.2f, 3.3f, 4.4f]
  List<Double> doubleList = [10.01d, 20.02d, 30.03d, 40.04d]
  List<BigDecimal> bigDecimalList = [new BigDecimal("9876.543"), new BigDecimal("8765.432"), new BigDecimal("7654.321"), new BigDecimal("6543.211")]
  List<BigInteger> bigIntegerList = [new BigInteger("10000000000"), new BigInteger("20000000000"), new BigInteger("30000000000"), new BigInteger("40000000000")]

// === Date and Time Classes ===
  List<LocalDate> localDateList = ListConverter.toLocalDates('2020-01-01', '2021-02-02', '2022-03-03', '2023-04-04')
  List<LocalTime> localTimeList = ListConverter.toLocalTimes("12:01", "13:02", "14:02", "15:03")

  List<LocalDateTime> localDateTimeList = [
      LocalDateTime.of(2020, 1, 1, 10, 0),
      LocalDateTime.of(2021, 2, 2, 11, 0),
      LocalDateTime.of(2022, 3, 3, 12, 0),
      LocalDateTime.of(2023, 4, 4, 13, 0)
  ]
  List<ZonedDateTime> zonedDateTimeList = [
      ZonedDateTime.of(2024, 5, 5, 14, 0, 0, 0, ZoneId.of('America/New_York')),
      ZonedDateTime.of(2024, 6, 6, 15, 0, 0, 0, ZoneId.of('Europe/Paris')),
      ZonedDateTime.of(2024, 7, 7, 16, 0, 0, 0, ZoneId.of('Asia/Tokyo')),
      ZonedDateTime.of(2024, 8, 8, 17, 0, 0, 0, ZoneId.of('Australia/Sydney'))
  ]
  List<Instant> instantList = [
      Instant.parse('2024-01-01T10:00:00Z'),
      Instant.parse('2024-02-02T11:00:00Z'),
      Instant.parse('2024-03-03T12:00:00Z'),
      Instant.parse('2024-04-04T13:00:00Z')
  ]
  List<Date> legacyDateList = ListConverter.toDates(
   "2020-10-10", "2020-11-11", "2020-12-12", "2021-01-01"
  )
  List<Time> legacyTimeList = [
      Time.valueOf("10:00:00"),
      Time.valueOf("11:00:00"),
      Time.valueOf("12:00:00"),
      Time.valueOf("13:00:00")
  ]

  void testSingleDataType(List list, Class type, String columnName) {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq(true)  // Use async queries for production BigQuery
    Matrix m = Matrix.builder(columnName+type.simpleName.replace('[]', 's')).data(
        (columnName): list
    ).types(type).build()
    bq.saveToBigQuery(m, datasetName)
    String metaData = bq.getTableInfo(datasetName, m.matrixName).content()
    //println "Meta data: $metaData"

    Matrix m2 = bq.query("select * from `${projectId}.${datasetName}.${m.matrixName}`").withMatrixName(m.matrixName)

    assertEquals(list.size(), m2.rowCount(), "Unexpected row count")
    assertEquals(1, m2.columnCount(), "Unexpected column count")
    //println m2.content()
    //println m2.types()
    m2.convert(columnName, type)
    if (type == byte[].class) {
      def toKey = { byte[] a -> Base64.encoder.encodeToString(a) }
      def expected = (list as List<byte[]>).collect(toKey).sort()
      def actual   = ((List<byte[]>) m2[columnName]).collect(toKey).sort()
      assertIterableEquals(expected, actual, "BYTES mismatch")
      return
    }
    assertIterableEquals(m[columnName].sort(), m2[columnName].sort(), "Data read from BigQuery does not match original data for type $type. Meta data: $metaData")
  }

  @Test
  void testByte() {
    testSingleDataType(byteList, Byte, 'test')
  }

  @Test
  void testShort() {
    testSingleDataType(shortList, Short, 'test')
  }

  @Test
  void testInteger() {
    testSingleDataType(integerList, Integer, 'test')
  }

  @Test
  void testLong() {
    testSingleDataType(longList, Long, 'test')
  }

  @Test
  void testFloat() {
    testSingleDataType(floatList, Float, 'test')
  }

  @Test
  void testDouble() {
    testSingleDataType(doubleList, Double, 'test')
  }

  @Test
  void testBigDecimal() {
    testSingleDataType(bigDecimalList, BigDecimal, 'test')
  }

  @Test
  void testBigInteger() {
    testSingleDataType(bigIntegerList, BigInteger, 'test')
  }

  @Test
  void testLocalDate() {
    testSingleDataType(localDateList, LocalDate, 'test')
  }

  @Test
  void testLocalTime() {
    testSingleDataType(localTimeList, LocalTime, 'test')
  }

  @Test
  void testLocalDateTime() {
    testSingleDataType(localDateTimeList, LocalDateTime, 'test')
  }

  @Test
  void testZonedDateTime() {
    testSingleDataType(zonedDateTimeList, ZonedDateTime, 'test')
  }

  @Test
  void testInstant() {
    testSingleDataType(instantList, Instant, 'test')
  }

  @Test
  void testLegacyDate() {
    testSingleDataType(legacyDateList, Date, 'test')
  }

  @Test
  void testLegacyTime() {
    testSingleDataType(legacyTimeList, Time, 'test')
  }

  @Test
  void testString() {
    List<String> stringList = ListConverter.toStrings("one", "two", "three", "four")
    testSingleDataType(stringList, String, 'test')
  }

  @Test
  void testBoolean() {
    List<Boolean> booleanList = [true, false, true, false]
    testSingleDataType(booleanList, Boolean, 'test')
  }

  @Test
  void testByteArray() {
    List<byte[]> byteArrayList = [ "one".bytes, "two".bytes, "three".bytes, "four".bytes ]
    testSingleDataType(byteArrayList, byte[], 'test')
  }

  @Test
  void testComplexData() {
    String projectId = System.getenv('GOOGLE_CLOUD_PROJECT')
    if (projectId == null) {
      log.warn("GOOGLE_CLOUD_PROJECT env variable not set, cannot run test!")
      return
    }
    Bq bq = new Bq(true)  // Use async queries for production BigQuery
    String datasetName = 'BqTestComplexData'
    bq.createDataset(datasetName)
    assertTrue(bq.datasetExist(datasetName))


// === Build the Matrix ===
    List<byte[]> byteArray = [toBytes([1,2,3]), toBytes([4,5,6]), toBytes([7,8,9]), toBytes([10,11,12])]
    Matrix m = Matrix.builder("allTypes").data(
        // Number types
        id: bigIntegerList,
        a_byte: byteList,
        a_bytes: byteArray,
        a_short: shortList,
        an_integer: integerList,
        a_long: longList,
        a_float: floatList,
        a_double: doubleList,
        a_bigDecimal: bigDecimalList,

        // Date and time types
        localDate: localDateList,
        times: localTimeList,
        a_localDateTime: localDateTimeList,
        a_zonedDateTime: zonedDateTimeList,
        an_instant: instantList,
        a_legacyDate: legacyDateList,
        a_legacyTime: legacyTimeList
    ).types(
        // Define the type for each column in the same order as the data above.
        BigInteger, Byte, byte[], Short, Integer, Long, Float, Double, BigDecimal,
        LocalDate, LocalTime, LocalDateTime, ZonedDateTime, Instant, Date, Time
    ).build()

    //println "Matrix 'allTypes' successfully built."
    //println "Columns: " + m.columnNames()
    //println "Types: " + m.types()
    //println "Number of rows: " + m.rowCount()
    bq.saveToBigQuery(m, datasetName)
    String metaData = bq.getTableInfo(datasetName, m.matrixName).content()
    //println "Meta data: $metaData"
    Matrix m2 = bq.query("select * from `${projectId}.${datasetName}.${m.matrixName}`").withMatrixName(m.matrixName)
    assertEquals(m.rowCount(), m2.rowCount(), "Unexpected row count")
    assertEquals(m.columnCount(), m2.columnCount(), "Unexpected column count")
    //println m2.content()
    //println m2.types()
    for (int i = 0; i < m.columnCount(); i++) {
      Column col = m.column(i)
      Class type = col.type
      String name = col.name
      if (type == byte[].class) {
        def toKey = { byte[] a -> Base64.encoder.encodeToString(a) }
        def expected = (m[name] as List<byte[]>).collect(toKey).sort()
        def actual   = ((List<byte[]>) m2[name]).collect(toKey).sort()
        assertIterableEquals(expected, actual, "BYTES mismatch for column $name")
        continue
      }
      m2.convert(name, type)
      if (type == BigDecimal) {
        List<BigDecimal> expected = (List<BigDecimal>) m[name]
        List<BigDecimal> actual   = (List<BigDecimal>) m2[name]
        for (int j = 0; j < expected.size(); j++) {
          BigDecimal e = expected[j]?.setScale(3, RoundingMode.HALF_UP)
          BigDecimal a = actual[j]?.setScale(3, RoundingMode.HALF_UP)
          assertEquals(e, a, "BigDecimal mismatch for column $name at row $j")
        }
        continue
      }
      assertIterableEquals(m[name].sort(), (m2[name] as List).sort(), "Data read from BigQuery does not match original data for column $name of type $type. Meta data: $metaData")
    }

  }

  @Test
  void testTypeMapping() {
    Byte[] ba = [1,2,3]
    assertEquals(StandardSQLTypeName.BYTES,  TypeMapper.toStandardSqlType(ba.class))
  }

  static byte[] toBytes(List<Integer> integers) {
    byte[] bytes = new byte[integers.size()]
    for (int i = 0; i < integers.size(); i++) {
      bytes[i] = integers[i].byteValue()
    }
    return bytes
  }
}
