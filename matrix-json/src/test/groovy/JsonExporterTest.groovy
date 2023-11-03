import java.time.format.DateTimeFormatter

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.matrix.Matrix
import se.alipsa.groovy.matrixjson.JsonExporter

import java.time.LocalDate

import static se.alipsa.groovy.matrix.ListConverter.toLocalDates

class JsonExporterTest {


    @Test
    void testExportJson() {
        def empData = Matrix.create(
                emp_id: 1..3,
                emp_name: ["Rick","Dan","Michelle"],
                salary: [623.3,515.2,611.0],
                start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15"),
                [int, String, Number, LocalDate]
        )
        //println empData.content()

        def exporter = new JsonExporter(empData)


        assertEquals(
                '[{"emp_id":1,"emp_name":"Rick","salary":623.3,"start_date":"2012-01-01"},{"emp_id":2,"emp_name":"Dan","salary":515.2,"start_date":"2013-09-23"},{"emp_id":3,"emp_name":"Michelle","salary":611.0,"start_date":"2014-11-15"}]',
                exporter.toJson(),
                exporter.toJson())

        //println JsonOutput.prettyPrint(exporter.toJson())

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern('yy/dd/MM')

        def json = exporter.toJson(['salary': {it * 10 + ' kr'}, 'start_date': {dateTimeFormatter.format(it)}])
        assertEquals(
                '[{"emp_id":1,"emp_name":"Rick","salary":"6233.0 kr","start_date":"12/01/01"},{"emp_id":2,"emp_name":"Dan","salary":"5152.0 kr","start_date":"13/23/09"},{"emp_id":3,"emp_name":"Michelle","salary":"6110.0 kr","start_date":"14/15/11"}]',
                json,
                json
        )
        //println JsonOutput.prettyPrint(json)
    }
}