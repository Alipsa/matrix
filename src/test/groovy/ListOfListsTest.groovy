import org.junit.jupiter.api.Test

import java.text.DecimalFormat

import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.groovy.matrix.Grid.*
import static se.alipsa.groovy.matrix.Stat.sum

class ListOfListsTest {


    @Test
    void testConvertToBigDecimal() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals(5 as BigDecimal, sum(foo[1]), "sum of foo: ")

        def bar = convert(foo, 0, BigDecimal)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")

        def format = DecimalFormat.getInstance(Locale.GERMANY)

        def foo2 = [
            [12.0, 3.0, Math.PI],
            ["1,3", "2", 3],
            ["4,3", "2,01", 3]
        ]
        def baz = convert(foo2, 0, BigDecimal.class, format)
        assertEquals(17.6, sum(baz, 0))

        def baz2 = convert(foo2, [0, 1, 2], BigDecimal.class, format)
        assertEquals(7.01, sum(baz2, 0..2)[1])
    }

    @Test
    void testCastToDouble() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        def bar = convert(foo, 0, Double.class)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")

        def foo2 = [
            [12.0, 3.0, Math.PI],
            ["1,3", "2", 3],
            ["4,3", "2,01", 3]
        ]

        def format = DecimalFormat.getInstance(Locale.GERMANY)
        def baz2 = convert(foo2, 0..2, Double.class, format)
        assertEquals(7.01, sum(baz2, [0,1])[1])

        def baz3 = convert(foo2, 0, Double.class, format)
        def baz4 = convert(foo2, 0, Double.class, format)
        assertEquals(sum(baz3, 0), sum(baz4, 0))
    }

    @Test
    void testCastWithClosure() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 3],
                ["bla", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        def bar = convert(foo, 0, { v -> {
            if (v instanceof Number) {
                return v
            }
            if (v instanceof String) {
                def num = v.replace(',', '.')
                if (num.isNumber()) {
                    return new BigDecimal(num)
                }
            }
            return 0
        }})
        assertEquals(13.9g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")
    }

    @Test
    void testTranspose() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 4],
                ["bla", 7, 5]
        ]
        assertEquals([
                [12.0, "1,9", "bla"],
                [3.0, 2, 7],
                [Math.PI, 4, 5]
        ], transpose(foo))
    }

    @Test
    void testValidate() {
        def singleRow = [[1,2,3]]
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
}
