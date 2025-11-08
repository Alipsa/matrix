package test.alipsa.matrix.avro

import org.apache.avro.Schema
import org.apache.avro.file.DataFileReader
import org.apache.avro.generic.GenericDatumReader
import org.apache.avro.generic.GenericRecord
import org.junit.jupiter.api.*
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.avro.MatrixAvroReader
import se.alipsa.matrix.avro.MatrixAvroWriter

import java.nio.file.Files
import java.time.*

import static org.junit.jupiter.api.Assertions.*

class MatrixAvroRoundTripTest {


  @Test
  void roundTrip_withDecimalInference_preservesTypes() {
    File tmp = Files.createTempFile("matrix-avro-rt-withDecimalInference", ".avro").toFile()
    // --- Build a source Matrix with a variety of types (and some nulls) ---
    def uuid1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    def uuid2 = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeffffffff")

    // Use millis-precision for time-like values to avoid rounding surprises
    LocalTime t1 = LocalTime.of(9, 10, 11, 345_000_000)
    LocalTime t2 = LocalTime.of(23, 59, 0, 1_000_000) // will round down to 00:00:00.001
    Instant i1 = Instant.parse("2024-03-01T12:34:56Z")
    Instant i2 = Instant.parse("2024-12-24T08:09:10Z")
    LocalDateTime ldt1 = LocalDateTime.of(2024, 7, 1, 10, 20, 30, 999_000_000)
    LocalDateTime ldt2 = LocalDateTime.of(2025, 1, 2, 3, 4, 5, 123_000_000)

    Map<String, List> cols = new LinkedHashMap<>()
    cols["name"]      = ["Alice", "Bob", null]
    cols["age"]       = [30, null, 41]
    cols["birthday"]  = [
      LocalDate.of(1990,1,5),
      LocalDate.of(1984,7,23),
      null
    ]
    cols["time"]      = [t1, t2, null]
    cols["ts"]        = [
      i1,
      i2,
      null]                    // Instant -> timestamp-millis
    cols["ldt"]       = [
      ldt1,
      ldt2,
      null]                // LocalDateTime -> local-timestamp-millis
    cols["price"]     = [
      new BigDecimal("12.34"),
      null,
      new BigDecimal("1000.50")] // BigDecimal
    cols["flag"]      = [true, false, null]
    cols["bytes"]     = [
      ([1, 2, 3] as byte[]),
      null,
      (byte[])[42]
    ]
    cols["uuid"]      = [uuid1, null, uuid2]
    cols["note"]      = ["hi", null, ""]

    Matrix src = Matrix.builder("RoundTripDecimal")
        .columns(cols)
        .types(String, Integer, LocalDate, LocalTime, Instant, LocalDateTime,
        BigDecimal, Boolean, byte[], UUID, String)
        .build()

    assert src.type("ldt") == LocalDateTime : "Matrix reported type for 'ldt' is ${src.type("ldt")}"

    // --- Write with inference === true (decimal logical type) ---
    MatrixAvroWriter.write(src, tmp, true)
    def ltName = avroLogicalTypeName(tmp, "ldt")
    assertNotNull(ltName, "No logicalType on 'ldt' field")
    assertTrue(ltName == "local-timestamp-micros" || ltName == "local-timestamp-millis",
        "Unexpected logicalType for 'ldt': $ltName")

    long raw = rawLongFor(tmp, "ldt")
    if (ltName == "local-timestamp-micros") {
      assertEquals(999_000L, raw % 1_000_000L, "Expected micros remainder 999000, got ${raw % 1_000_000L}")
    } else {
      // millis
      assertEquals(999L, raw % 1_000L, "Expected millis remainder 999, got ${raw % 1_000L}")
    }
    // --- Read back ---
    Matrix back = MatrixAvroReader.read(tmp)

    // --- Basic shape ---
    assertEquals(src.rowCount(), back.rowCount())
    assertEquals(src.columnCount(), back.columnCount())
    assertEquals(src.columnNames(), back.columnNames())

    // --- Per-column checks ---

    // name (String)
    assertEquals("Alice", back[0, "name"])
    assertEquals("Alice", back[0, "name"])
    assertEquals("Bob", back[1, "name"])
    assertNull(back[2, "name"])
    assertEquals("", back[2, "note"])

    // age (Integer with null)
    assertEquals(30, back[0, "age"])
    assertNull(back[1, "age"])
    assertEquals(41, back[2, "age"])

    // birthday (LocalDate)
    assertEquals(LocalDate.of(1990,1,5), back[0, "birthday"])
    assertEquals(LocalDate.of(1984,7,23), back[1, "birthday"])
    assertNull(back[2, "birthday"])
    assertTrue(back["birthday"][0] instanceof LocalDate)

    // time (LocalTime, millis precision)
    assertEquals(truncMillis(t1), back[0, "time"])
    assertEquals(truncMillis(t2), back[1, "time"])
    assertNull(back[2, "time"])
    assertTrue(back["time"][0] instanceof LocalTime)

    // ts (Instant, millis precision)
    assertEquals(i1, back[0, "ts"])
    assertEquals(i2, back[1, "ts"])
    assertNull(back[2, "ts"])
    assertTrue(back["ts"][0] instanceof Instant)

    // ldt (LocalDateTime, millis precision)
    assertEquals(truncMillis(ldt1), back[0, "ldt"])
    assertEquals(truncMillis(ldt2), back[1, "ldt"])
    assertNull(back[2, "ldt"])
    assertTrue(back["ldt"][0] instanceof LocalDateTime)

    // price (BigDecimal due to inference)
    assertEquals(new BigDecimal("12.34"), back[0, "price"])
    assertNull(back[1, "price"])
    assertEquals(new BigDecimal("1000.50"), back[2, "price"])
    assertTrue(back["price"][0] instanceof BigDecimal)

    // flag (Boolean)
    assertEquals(true, back[0, "flag"])
    assertEquals(false, back[1, "flag"])
    assertNull(back[2, "flag"])

    // bytes (byte[])
    assertArrayEquals([1, 2, 3] as byte[], (byte[]) back[0, "bytes"])
    assertNull(back[1, "bytes"])
    assertArrayEquals([42] as byte[], (byte[]) back[2, "bytes"])

    // uuid (stored as logical uuid(string) → reader returns String)
    assertEquals(uuid1.toString(), back[0, "uuid"])
    assertNull(back[1, "uuid"])
    assertEquals(uuid2.toString(), back[2, "uuid"])
    assertTrue(back["uuid"][0] instanceof String)
    tmp.delete()
  }

  @Test
  void roundTrip_withoutDecimalInference_writesBigDecimalAsDouble() {
    File tmp = Files.createTempFile("matrix-avro-rt-withoutDecimalInference", ".avro").toFile()
    Map<String, List> cols = new LinkedHashMap<>()
    cols["price"] = [
      new BigDecimal("12.34"),
      null,
      new BigDecimal("1000.5")
    ]
    cols["name"]  = ["A", "B", "C"]

    Matrix src = Matrix.builder("RoundTripNoDecimal").columns(cols).build()

    // Write with inference === false → BigDecimal stored as DOUBLE
    MatrixAvroWriter.write(src, tmp, false)

    Matrix back = MatrixAvroReader.read(tmp)

    assertEquals(3, back.rowCount())
    assertEquals(2, back.columnCount())

    // price comes back as Double (per writer policy)
    assertTrue(back["price"][0] instanceof Double)
    assertEquals(12.34d, (Double) back[0, "price"], 1e-9)
    assertNull(back[1, "price"])
    assertEquals(1000.5d, (Double) back[2, "price"], 1e-9)

    assertEquals(["A", "B", "C"], (0..<3).collect { back[it, "name"] })
    tmp.delete()
  }

  @Test
  void roundTripArrayMapRecordColumns() {
    File tmp = Files.createTempFile("matrix-avro-rt-collections-", ".avro").toFile()

    // ----- build a Matrix with ARRAY, MAP, and RECORD-like columns -----
    List<Integer> a1 = [1, 2, 3]
    List<Integer> a2 = []            // empty list should work
    List<Integer> a3 = [
      10,
      null,
      30] // null element allowed
    Map<String,Integer> m1 = [x: 1, y: 2]
    Map<String,Integer> m2 = [y: 5, z: 9] // different keys => will serialize as Avro MAP
    Map<String,Integer> m3 = null

    // RECORD-like: fixed field set across rows => will serialize as Avro RECORD
    Map<String, Object> r1 = [name:"Alice", age:30, birthday: LocalDate.of(1990,1,5)]
    Map<String, Object> r2 = [name:"Bob",   age:41, birthday: LocalDate.of(1984,7,23)]
    Map<String, Object> r3 = [name:null,    age:null, birthday:null]

    Map<String, List> cols = new LinkedHashMap<>()
    cols["arr"]    = [a1, a2, a3]       // ARRAY
    cols["props"]  = [
      m1,
      m2,
      m3]       // MAP (keys vary across rows)
    cols["person"] = [
      r1,
      r2,
      r3]       // RECORD (same fields across rows)
    Matrix src = Matrix.builder("ArrMapRec")
        .columns(cols)
        .types(List, Map, Map) // record-like column is a Map but will be written as RECORD by heuristic
        .build()

    // ----- write + read -----
    MatrixAvroWriter.write(src, tmp, true)
    Matrix back = MatrixAvroReader.read(tmp)

    // shape & columns
    assertEquals(3, back.rowCount())
    assertEquals(["arr", "props", "person"], back.columnNames())

    // ----- ARRAY assertions -----
    assertEquals([1, 2, 3], back[0, "arr"])
    assertEquals([], back[1, "arr"])
    assertEquals([10, null, 30], back[2, "arr"])

    // ----- MAP assertions -----
    assertTrue(back[0, "props"] instanceof Map)
    assertTrue(back[1, "props"] instanceof Map)
    assertNull(back[2, "props"])
    assertEquals(1, (back[0, "props"] as Map).get("x"))
    assertEquals(2, (back[0, "props"] as Map).get("y"))
    assertEquals(5, (back[1, "props"] as Map).get("y"))
    assertEquals(9, (back[1, "props"] as Map).get("z"))

    // ----- RECORD-like assertions (nested Map from reader) -----
    def p0 = back[0, "person"] as Map
    def p1 = back[1, "person"] as Map
    def p2 = back[2, "person"] as Map

    assertEquals("Alice", p0["name"])
    assertEquals(30, p0["age"])
    assertEquals(LocalDate.of(1990,1,5), p0["birthday"])

    assertEquals("Bob", p1["name"])
    assertEquals(41, p1["age"])
    assertEquals(LocalDate.of(1984,7,23), p1["birthday"])

    assertNull(p2["name"])
    assertNull(p2["age"])
    assertNull(p2["birthday"])

    tmp.delete()
  }

  // --- helpers ---
  private static LocalTime truncMillis(LocalTime t) {
    if (t == null) return null
    // intdiv => integer division; result is int
    int msPart = t.nano.intdiv(1_000_000) * 1_000_000
    return t.withNano(msPart)
  }

  private static LocalDateTime truncMillis(LocalDateTime dt) {
    if (dt == null) return null
    int msPart = dt.nano.intdiv(1_000_000) * 1_000_000
    return dt.withNano(msPart)
  }

  private static String avroLogicalTypeName(File avroFile, String field) {
    def reader = new DataFileReader<>(avroFile, new GenericDatumReader<>())
    try {
      Schema s = reader.schema.getField(field).schema()
      if (s.getType() == Schema.Type.UNION) {
        s = s.getTypes().find { it.type != Schema.Type.NULL }
      }
      return s.getLogicalType()?.name
    } finally {
      reader.close()
    }
  }
  private static long rawLongFor(File avro, String field) {
    def rdr = new DataFileReader<GenericRecord>(avro, new GenericDatumReader<>())
    try {
      if (!rdr.hasNext()) throw new IllegalStateException("no records written")
      def rec = rdr.next()
      return (rec.get(field) as Long)
    } finally {
      rdr.close()
    }
  }
}
