import se.alipsa.matrix.datasets.Dataset
import se.alipsa.matrix.json.JsonImporter

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.json.JsonExporter

import java.time.LocalDate

import static se.alipsa.matrix.core.ListConverter.toLocalDates

class JsonExporterTest {


    @Test
    void testExportJson() {
        def empData = Matrix.builder().data(
                emp_id: 1..3,
                emp_name: ["Rick","Dan","Michelle"],
                salary: [623.3,515.2,611.0],
                start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
            .types([int, String, Number, LocalDate])
            .build()
        //println empData.content()

        def exporter = new JsonExporter(empData)


        assertEquals(
                '[{"emp_id":1,"emp_name":"Rick","salary":623.3,"start_date":"2012-01-01"},{"emp_id":2,"emp_name":"Dan","salary":515.2,"start_date":"2013-09-23"},{"emp_id":3,"emp_name":"Michelle","salary":611.0,"start_date":"2014-11-15"}]',
                exporter.toJson(),
                exporter.toJson())

        def exp2 =  new JsonExporter(empData.grid(), empData.columnNames())
        assertEquals(
            '[{"emp_id":1,"emp_name":"Rick","salary":623.3,"start_date":"2012-01-01"},{"emp_id":2,"emp_name":"Dan","salary":515.2,"start_date":"2013-09-23"},{"emp_id":3,"emp_name":"Michelle","salary":611.0,"start_date":"2014-11-15"}]',
            exp2.toJson(),
            exp2.toJson())

        // custom date format
        assertEquals(
            '[{"emp_id":1,"emp_name":"Rick","salary":623.3,"start_date":"2012/01/01"},{"emp_id":2,"emp_name":"Dan","salary":515.2,"start_date":"2013/09/23"},{"emp_id":3,"emp_name":"Michelle","salary":611.0,"start_date":"2014/11/15"}]',
            exp2.toJson('yyyy/MM/dd'),
            exp2.toJson())
    }

    @Test
    void testExportJsonWithConverter() {
        def empData = Matrix.builder().data(
            emp_id: 1..3,
            emp_name: ["Rick","Dan","Michelle"],
            salary: [623.3,515.2,611.0],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"))
            .types([int, String, Number, LocalDate])
            .build()
        //println empData.content()

        def exporter = new JsonExporter(empData)
        assertEquals(['start_date'], empData.columnNames(TemporalAccessor))
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern('yy/dd/MM')

        def json = exporter.toJson('salary': {it * 10 + ' kr'}, 'start_date': {dateTimeFormatter.format(it)})
        assertEquals(
            '[{"emp_id":1,"emp_name":"Rick","salary":"6233.0 kr","start_date":"12/01/01"},{"emp_id":2,"emp_name":"Dan","salary":"5152.0 kr","start_date":"13/23/09"},{"emp_id":3,"emp_name":"Michelle","salary":"6110.0 kr","start_date":"14/15/11"}]',
            json,
            json
        )
    }

    @Test
    void testExportDatasets() {
        def ds = Dataset.mtcars()
        JsonExporter exporter = new JsonExporter(ds)
        String json = exporter.toJson(true)
        Matrix m = JsonImporter.parse(json)
        .convert(ds.types()).withMatrixName(ds.matrixName)
        assertEquals(ds, m, ds.diff(m))

        ds = Dataset.airquality()
        exporter = new JsonExporter(ds)
        json = exporter.toJson()
        m = JsonImporter.parse(json)
            .convert(ds.types()).withMatrixName(ds.matrixName)
        assertEquals(ds, m, ds.diff(m))

        ds = Dataset.diamonds()
        exporter = new JsonExporter(ds)
        json = exporter.toJson(true)
        m = JsonImporter.parse(json)
            .convert(ds.types()).withMatrixName(ds.matrixName)
        assertEquals(ds, m, ds.diff(m))


    }

    // Phase 2: Edge Case Tests

    @Test
    void testEmptyMatrixExport() {
        Matrix empty = Matrix.builder().build()
        JsonExporter exporter = new JsonExporter(empty)
        String json = exporter.toJson()
        assertEquals('[]', json, "Empty matrix should export as empty JSON array")
    }

    @Test
    void testEmptyMatrixWithColumns() {
        Matrix m = Matrix.builder()
            .matrixName('empty')
            .data(id: [], name: [])
            .build()

        JsonExporter exporter = new JsonExporter(m)
        String json = exporter.toJson()
        assertEquals('[]', json, "Matrix with 0 rows should export as empty array")
    }

    @Test
    void testPrettyPrint() {
        Matrix m = Matrix.builder()
            .data(id: [1, 2], name: ['Alice', 'Bob'])
            .build()

        JsonExporter exporter = new JsonExporter(m)
        String json = exporter.toJson(true)

        assertTrue(json.contains('\n'), "Pretty printed JSON should contain newlines")
        assertTrue(json.contains('  '), "Pretty printed JSON should contain indentation")
        assertTrue(json.contains('"id"'), "Should contain id field")
        assertTrue(json.contains('"name"'), "Should contain name field")
    }

    // Phase 3: API Enhancement Tests

    @Test
    void testNullMatrixValidation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
            new JsonExporter(null)
        }
        assertTrue(ex.message.contains("cannot be null"), "Error should mention null validation")
    }

    @Test
    void testNullGridValidation() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
            new JsonExporter(null, ['col1', 'col2'])
        }
        assertTrue(ex.message.contains("cannot be null"), "Error should mention null validation")
    }

    @Test
    void testNullColumnNamesValidation() {
        Matrix m = Matrix.builder().data(id: [1, 2]).build()
        IllegalArgumentException ex = assertThrows(IllegalArgumentException) {
            new JsonExporter(m.grid(), null)
        }
        assertTrue(ex.message.contains("cannot be null"), "Error should mention null validation")
    }

    @Test
    void testStaticToJson() {
        Matrix m = Matrix.builder()
            .data(id: [1, 2], name: ['Alice', 'Bob'])
            .build()

        // Static method - one-liner convenience
        String json = JsonExporter.toJson(m)

        assertNotNull(json, "JSON should not be null")
        assertTrue(json.contains('"id"'), "Should contain id field")
        assertTrue(json.contains('"name"'), "Should contain name field")
        assertTrue(json.contains('Alice'), "Should contain Alice")
        assertTrue(json.contains('Bob'), "Should contain Bob")
    }

    @Test
    void testStaticToJsonWithIndent() {
        Matrix m = Matrix.builder()
            .data(x: [10], y: [20])
            .build()

        String json = JsonExporter.toJson(m, true)

        assertTrue(json.contains('\n'), "Pretty printed JSON should contain newlines")
        assertTrue(json.contains('  '), "Pretty printed JSON should contain indentation")
    }

    @Test
    void testToJsonFile() {
        Matrix m = Matrix.builder()
            .data(id: [1, 2, 3], value: [100, 200, 300])
            .build()

        File tempFile = File.createTempFile('test-json-export', '.json')
        try {
            JsonExporter.toJsonFile(m, tempFile)

            assertTrue(tempFile.exists(), "File should exist")
            assertTrue(tempFile.length() > 0, "File should not be empty")

            String content = tempFile.text
            assertTrue(content.contains('"id"'), "Should contain id field")
            assertTrue(content.contains('"value"'), "Should contain value field")

            // Verify we can parse it back
            Matrix parsed = JsonImporter.parse(tempFile)
            assertEquals(3, parsed.rowCount(), "Should have 3 rows")
            assertEquals(2, parsed.columnCount(), "Should have 2 columns")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    void testToJsonFileWithIndent() {
        Matrix m = Matrix.builder()
            .data(a: [1])
            .build()

        File tempFile = File.createTempFile('test-json-pretty', '.json')
        try {
            JsonExporter.toJsonFile(m, tempFile, true)

            String content = tempFile.text
            assertTrue(content.contains('\n'), "Should have pretty printing with newlines")
        } finally {
            tempFile.delete()
        }
    }
}