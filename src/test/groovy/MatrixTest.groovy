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
}
