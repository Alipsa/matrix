import se.alipsa.matrix.core.Grid
import se.alipsa.matrix.core.Matrix

import java.text.DecimalFormat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.core.Grid.*
import static se.alipsa.matrix.core.Stat.*

import org.junit.jupiter.api.Test

class GridTest {

    @Test
    void testGridCreation() {
        Grid grid = []
        grid << [1, 'foo', 3.14]
        grid << [2, 'bar', 9.81]
        def dims = grid.dimensions()
        assertEquals(2, dims.observations)
        assertEquals(3, dims.variables)

        def g2 = grid + [3, 'baz', 4.21]
        assertEquals(1, grid[0][0])
        assertEquals("bar", grid[1,1])
        assertEquals(4.21, g2[2,2])

        g2.eachWithIndex { List entry, int idx ->
            entry[1] = "'$idx - ${entry[1]}'".toString()
            //println "index $idx: ${entry[0]}, ${entry[1]}, ${entry[2]}"
        }
        assertEquals("'0 - foo'", g2[0,1])
        assertEquals("'1 - bar'", g2[1,1])
        assertEquals("'2 - baz'", g2[2,1])

        grid = new Grid(0, 5, 3)
        assertEquals(0, grid[0, 0])
        assertEquals(0, grid[4, 2])
        assertEquals(5, grid.getRowList().size())
        assertEquals(3, grid.getRowList()[0].size())
        assertEquals(3, grid.getRowList()[4].size())

    }

    @Test
    void testReplaceRow() {
        Grid grid = []
        grid << [1,2,3]
        grid << [10,20,30]
        assertIterableEquals([1,2,3], grid[0])
        grid[0] = [0.1, 0.2, 0.3]
        assertIterableEquals([0.1, 0.2, 0.3], grid[0])
        grid.replaceRow(1, [5,6,7])
        assertIterableEquals([5,6,7], grid[1])
    }

    @Test
    void testConvertToBigDecimal() {
        Grid foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals((12.0 + 3.0 + Math.PI), sum(foo[0]), "sum of foo 0: ")
        assertEquals(5 as BigDecimal, sum(foo[1]), "sum of foo 1: ")

        Grid bar = convert(foo, 0, BigDecimal)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")

        def format = DecimalFormat.getInstance(Locale.GERMANY)

        Grid foo2 = [
            [12.0, 3.0, Math.PI],
            ["1,3", "2", 3],
            ["4,3", "2,01", 3]
        ]
        Grid baz = convert(foo2, 0, BigDecimal.class, format)
        assertEquals(17.6, sum(baz, 0))

        Grid baz2 = convert(foo2, [0, 1, 2], BigDecimal.class, format)
        assertEquals(7.01, sum(baz2, 0..2)[1])
    }

    @Test
    void testCastToDouble() {
        Grid foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        Grid bar = convert(foo, 0, Double.class)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")

        Grid foo2 = [
            [12.0, 3.0, Math.PI],
            ["1,3", "2", 3],
            ["4,3", "2,01", 3]
        ]

        def format = DecimalFormat.getInstance(Locale.GERMANY)
        Grid baz2 = convert(foo2, 0..2, Double.class, format)
        assertEquals(7.01, sum(baz2, [0,1])[1])
    }

    @Test
    void testCastWithClosure() {
        Grid<Object> foo = new Grid([
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 3],
                ["bla", 2, 3]
        ])

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        Grid<BigDecimal> bar = convert(foo, 0, { v -> {
            if (v instanceof Number) {
                return v as BigDecimal
            }
            if (v instanceof String) {
                def num = v.replace(',', '.')
                if (num.isNumber()) {
                    return new BigDecimal(num)
                }
            }
            return BigDecimal.ZERO
        }})
        assertEquals(13.9g, sum(bar, 0), "sum of bar: $bar")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")
    }

    @Test
    void testTranspose() {
        Grid<Object> foo = [
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 4],
                ["bla", 7, 5]
        ] as Grid
        assertEquals([
                [12.0, "1,9", "bla"],
                [3.0, 2, 7],
                [Math.PI, 4, 5]
        ], transpose(foo).data)

        // ensure that transpose did not change anything in the Grid
        assertEquals([
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 4],
                ["bla", 7, 5]
        ], foo.getRowList())
    }

    @Test
    void testValidate() {
        Grid singleRow = [[1,2,3]] as Grid
        assertTrue(isValid(singleRow), "Single row, " + singleRow.getClass())
        assertTrue(isValid([[1,2,3], ["a", "b", "c"]]), "two rows")
        assertFalse(isValid([1,2,3]), "One dimensional")
        assertTrue(isValid([[1,2,3], ["a", "b", "c"], [1.0, 1.1, 2.2]]), "3 rows")
        assertFalse(isValid([
                        [1,2,3],
                        ["a", "b", "c"],
                        [1.0, 1.1],
                        [1,1]
                        ]), "not a uniform matrix")
        assertTrue(isValid([
                [1, 2, 3],
                ["a", "b", "c"],
                [1.0, 1.1, null],
                [1, 1, null]
        ]), "uniform matrix with nulls")
        assertFalse(isValid(123), "not a matrix")
        assertFalse(isValid(null), "null matrix")
    }

    @Test
    void testIteration() {
        Grid foo = new Grid()
        foo.addAll([
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 4],
                ["bla", 7, 5]
        ])
        int i = 0;
        for(row in foo) {
            if (i == 0) {
                assertIterableEquals([12.0, 3.0, Math.PI], row)
            }
            if (i == 2) {
                assertIterableEquals(["bla", 7, 5], row)
            }
            i++
        }
    }

    @Test
    void testShortNotationAssign() {
        Grid foo = [
            [12.0, 3.0, Math.PI],
            ["1.9", 2, 3],
            ["4.3", 2, 3]
        ] as Grid

        foo[0, 1] = 3.23
        assertEquals(3.23, foo[0,1])

        assertIterableEquals(["4.3", 2, 3], foo[2])
        foo[3] = [1,2,3]
        assertIterableEquals([1,2,3], foo[3])
    }

    @Test
    void testAddRow() {
        Grid g = new Grid()
        g = g + [1,2,3,4,5]
        g << [10,20,30,40,50]
        g.add(0, ['Y1',	'Y2',	'Y3',	'Y4',	'Y5'])
        //g.rowList.each {println it}
        assertIterableEquals(['Y1',	'Y2',	'Y3',	'Y4',	'Y5'], g[0])
        assertIterableEquals([1,2,3,4,5], g.getAt(1))
        assertIterableEquals([10,20,30,40,50], g[2])

        g = new Grid(nrow: 2, ncol: 5)
        g[0] = ['Y1',	'Y2',	'Y3',	'Y4',	'Y5']
        g.replaceRow(1, [1,2,3,4,5])
        g << [10,20,30,40,50]
        assertIterableEquals(['Y1',	'Y2',	'Y3',	'Y4',	'Y5'], g[0])
        assertIterableEquals([1,2,3,4,5], g.getAt(1))
        assertIterableEquals([10,20,30,40,50], g[2])
    }

    @Test
    void testPopulateColumn() {
        def components = Matrix.builder().data([
            id: [1,2,3,4,5],
            size: [1.2,2.3,0.7,1.3,1.9]
        ]).types(Integer, BigDecimal).build()
        def yearCountMx = new Grid<Integer>(value:0, ncol: 31, nrow: components.rowCount())
        yearCountMx.replaceColumn(1, components['id'] as List<Integer>)
        assertEquals(1, yearCountMx[0, 1])
        assertEquals(3, yearCountMx[2, 1])
        assertEquals(5, yearCountMx[4, 1])
    }
}
