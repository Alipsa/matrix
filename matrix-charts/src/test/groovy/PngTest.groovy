import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import se.alipsa.matrix.core.Matrix

import java.time.LocalDate

import se.alipsa.matrix.charts.*

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.ListConverter.*

class PngTest {

    @BeforeAll
    static void init() {
        if (Boolean.getBoolean("headless")) {
            println("Enable monocle for headless testing")
            System.setProperty("testfx.robot", "glass")
            System.setProperty("testfx.headless", "true")
            System.setProperty("prism.order", "sw")
            System.setProperty("prism.text", "t2k")
        }
    }

    def empData = Matrix.builder().data(
            emp_id: 1..5,
            emp_name: ["Rick","Dan","Michelle","Ryan","Gary"],
            salary: [623.3,515.2,611.0,729.0,843.25],
            start_date: toLocalDates("2012-01-01", "2013-09-23", "2014-11-15", "2014-05-11", "2015-03-27")
    ).types([int, String, Number, LocalDate])
    .build()

    @Test
    void testBarchartToPng() {
        def file = File.createTempFile("barchart", ".png")
        BarChart chart = BarChart.createVertical("Salaries", empData, "emp_name", ChartType.BASIC, "salary")
        try {
            Plot.png(chart, file, 600, 400)
            //println("Wrote $file")
            assertTrue(file.exists())
            file.delete()
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            println "No graphics environment available: $e, skipping test"
        }
    }

    @Test
    void testBarchartToBase64() {
        BarChart chart = BarChart.createVertical("Salaries", empData, "emp_name", ChartType.BASIC, "salary")
        try {
            def base64 = Plot.base64(chart, 600, 400)
            //println(base64)
            assertNotNull(base64)
            assertTrue(base64.startsWith('data:image/png;base64,'))
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            println "No graphics environment available: $e, skipping test"
        }
    }

    @Test
    void testAreaChartToPng() {
        def file = File.createTempFile("areaChart", ".png")
        AreaChart chart = AreaChart.create("Salaries", empData, "emp_name", "salary")
        try {
            Plot.png(chart, file, 1024, 768)
            //println("Wrote $file")
            assertTrue(file.exists())
            file.delete()
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            println "No graphics environment available: $e, skipping test"
        }
    }
}
