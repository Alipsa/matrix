package test.alipsa.matrix.bigquery

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

import groovy.transform.CompileStatic

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import org.junit.jupiter.api.Test

import se.alipsa.matrix.bigquery.Bq
import se.alipsa.matrix.core.Matrix

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@CompileStatic
class SchemaCreationTest {

  @Test
  void testCreateSchemaSimpleMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['id', 'name', 'value'])
        .types([Integer, String, BigDecimal])
        .rows([
            [1, 'Alice', new BigDecimal("100.50")],
            [2, 'Bob', new BigDecimal("200.75")]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(3, schema.fields.size())

    Field idField = schema.fields[0]
    assertEquals('id', idField.name)
    assertEquals(LegacySQLTypeName.INTEGER, idField.type)

    Field nameField = schema.fields[1]
    assertEquals('name', nameField.name)
    assertEquals(LegacySQLTypeName.STRING, nameField.type)

    Field valueField = schema.fields[2]
    assertEquals('value', valueField.name)
    assertEquals(LegacySQLTypeName.BIGNUMERIC, valueField.type)
  }

  @Test
  void testCreateSchemaDateTimeTypes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['date', 'time', 'datetime', 'timestamp'])
        .types([LocalDate, LocalTime, LocalDateTime, Instant])
        .rows([
            [LocalDate.now(), LocalTime.now(), LocalDateTime.now(), Instant.now()]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(4, schema.fields.size())

    assertEquals('date', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.DATE, schema.fields[0].type)

    assertEquals('time', schema.fields[1].name)
    assertEquals(LegacySQLTypeName.TIME, schema.fields[1].type)

    assertEquals('datetime', schema.fields[2].name)
    assertEquals(LegacySQLTypeName.DATETIME, schema.fields[2].type)

    assertEquals('timestamp', schema.fields[3].name)
    assertEquals(LegacySQLTypeName.TIMESTAMP, schema.fields[3].type)
  }

  @Test
  void testCreateSchemaNumericTypes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['int_val', 'long_val', 'double_val', 'bigdec_val'])
        .types([Integer, Long, Double, BigDecimal])
        .rows([
            [1, 1000L, 1.5d, new BigDecimal("999.99")]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(4, schema.fields.size())

    assertEquals(LegacySQLTypeName.INTEGER, schema.fields[0].type)
    assertEquals(LegacySQLTypeName.INTEGER, schema.fields[1].type)
    assertEquals(LegacySQLTypeName.FLOAT, schema.fields[2].type)
    assertEquals(LegacySQLTypeName.BIGNUMERIC, schema.fields[3].type)
  }

  @Test
  void testCreateSchemaBooleanAndBytes() {
    Matrix matrix = Matrix.builder()
        .columnNames(['flag', 'data'])
        .types([Boolean, byte[]])
        .rows([
            [true, "test".bytes]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(2, schema.fields.size())

    assertEquals('flag', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.BOOLEAN, schema.fields[0].type)

    assertEquals('data', schema.fields[1].name)
    assertEquals(LegacySQLTypeName.BYTES, schema.fields[1].type)
  }

  @Test
  void testCreateSchemaEmptyMatrix() {
    Matrix matrix = Matrix.builder()
        .columnNames(['col1'])
        .types([String])
        .rows([])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(1, schema.fields.size())
    assertEquals('col1', schema.fields[0].name)
    assertEquals(LegacySQLTypeName.STRING, schema.fields[0].type)
  }

  @Test
  void testCreateSchemaWithTimestamp() {
    Matrix matrix = Matrix.builder()
        .columnNames(['event_time', 'event_date'])
        .types([Timestamp, Date])
        .rows([
            [new Timestamp(System.currentTimeMillis()), new Date()]
        ])
        .build()

    Schema schema = Bq.createSchema(matrix)

    assertNotNull(schema)
    assertEquals(2, schema.fields.size())

    Field timestampField = schema.fields[0]
    assertEquals('event_time', timestampField.name)
    assertEquals(LegacySQLTypeName.TIMESTAMP, timestampField.type)

    Field dateField = schema.fields[1]
    assertEquals('event_date', dateField.name)
    assertEquals(LegacySQLTypeName.DATE, dateField.type)
  }
}
