import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.*

import org.junit.jupiter.api.Test

import se.alipsa.matrix.charm.Charts
import se.alipsa.matrix.chartexport.ChartToImage
import se.alipsa.matrix.chartexport.ChartToPng
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.pict.*

import java.time.LocalDate

class PngTest {

    def empData = Matrix.builder().data(
            emp_id: 1..5,
            emp_name: ['Rick', 'Dan', 'Michelle', 'Ryan', 'Gary'],
            salary: [623.3, 515.2, 611.0, 729.0, 843.25],
            start_date: toLocalDates('2012-01-01', '2013-09-23', '2014-11-15', '2014-05-11', '2015-03-27')
    ).types([int, String, Number, LocalDate])
    .build()

    @Test
    void testPictBarchartToPng() {
        def file = File.createTempFile('barchart', '.png')
        BarChart chart = BarChart.createVertical('Salaries', empData, 'emp_name', ChartType.BASIC, 'salary')
        try {
            Plot.png(chart, file)
            assertTrue(file.exists())
        } finally {
            file.delete()
        }
    }

    @Test
    void testBarchartToBase64() {
        BarChart chart = BarChart.createVertical('Salaries', empData, 'emp_name', ChartType.BASIC, 'salary')
        def base64 = ChartToImage.base64(chart)
        assertNotNull(base64)
        assertTrue(base64.startsWith('data:image/png;base64,'))
    }

    @Test
    void testAreaChartToPng() {
        def file = File.createTempFile('areaChart', '.png')
        AreaChart chart = AreaChart.builder(empData)
            .title('Salaries')
            .x('emp_name')
            .y('salary')
            .build()
        try {
            Plot.png(chart, file)
            assertTrue(file.exists())
        } finally {
            file.delete()
        }
    }

}
