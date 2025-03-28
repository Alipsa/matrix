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
}