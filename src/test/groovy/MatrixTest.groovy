import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.Matrix.*

import org.junit.jupiter.api.Test

class MatrixTest {

    @Test
    void testCastToBigDecimal() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        def bar = cast(foo, 0, BigDecimal.class)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")
    }

    @Test
    void testCastToDouble() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1.9", 2, 3],
                ["4.3", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        def bar = cast(foo, 0, Double.class)
        assertEquals(18.2g, sum(bar, 0), "sum of bar: ")

        assertEquals(12.0g, sum(foo, 0), "after cast, sum of foo: ")
    }

    @Test
    void testCastWithClosure() {
        def foo = [
                [12.0, 3.0, Math.PI],
                ["1,9", 2, 3],
                ["bla", 2, 3]
        ]

        assertEquals(12.0g, sum(foo, 0), "sum of foo: ")

        def bar = cast(foo, 0, {v -> {
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
                ["1,9", 2, 3],
                ["bla", 2, 3]
        ]
        assertEquals([
                [12.0, "1,9", "bla"],
                [3.0, 2, 2],
                [Math.PI, 3, 3]
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
