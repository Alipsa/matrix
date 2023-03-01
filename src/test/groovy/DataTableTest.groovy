import se.alipsa.matrix.DataTable

import java.time.LocalDate

import static se.alipsa.matrix.ValueConverter.*
import static org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.*


class DataTableTest {

    @Test
    void testTableCreationFromMatrix() {
        def employees = [
                "employee": ['John Doe','Peter Smith','Jane Doe'],
                "salary": [21000, 23400, 26800],
                "startDate": toLocalDate('2013-11-01','2018-03-25','2017-03-14'),
                "endDate": toLocalDate('2020-01-10', '2020-04-12', '2020-10-06')
        ]
        def table = DataTable.create(employees)
        assertEquals('John Doe', table.get(0,0))
        assertEquals(23400, table.get(1,1) as Integer)
        assertEquals(LocalDate.of(2017, 3, 14), table.get(2,2))
    }

    @Test
    void testTransposing() {
        def report = [
                "Full Funding": [4563.153, 380.263, 4.938],
                "Baseline Funding": [3385.593, 282.133, 3.664],
                "Current Funding": [2700, 225, 2.922]
        ]
        def tr = DataTable.create(report).transpose()
        assertEquals(["Full Funding", "Baseline Funding", "Current Funding"], tr.getColumnNames())
        assertEquals([
           [4563.153, 3385.593, 2700],
           [380.263, 282.133, 225],
           [4.938, 3.664, 2.922]
        ], tr.getMatrix())
    }
}
