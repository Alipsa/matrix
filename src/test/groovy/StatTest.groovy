import static org.junit.jupiter.api.Assertions.*
import static se.alipsa.matrix.Matrix.*
import static se.alipsa.matrix.Stat.*

import org.junit.jupiter.api.Test

class StatTest {

    @Test
    void testMean() {
        def matrix = [
                [1, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(2 as BigDecimal, mean(matrix[0]))
        def m = mean(matrix, [1, 2])
        assertEquals(10/3, m[0])
        assertEquals(1.95g, m[1])
    }

    @Test
    void testMedian() {
        def matrix = [
                [1, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(2 as BigDecimal, median(matrix[0]))
        def m = median(matrix, [1, 2])
        assertEquals(2 as BigDecimal, m[0])
        assertEquals(1.95g, m[1])
    }

    @Test
    void testMin() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(0.3, min(matrix[0]))
        def m = min(matrix, 1..2)
        assertEquals(1, m[0])
        assertEquals(0.9, m[1])
    }

    @Test
    void testMax() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(3, max(matrix[0]))
        def m = max(matrix, 1..2)
        assertEquals(7, m[0])
        assertEquals(3, m[1])
    }

    @Test
    void testSd() {
        //
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(1.3650396819628847, sd(matrix[0]))
        assertEquals(1.3650396819628847, sdSample([0.3, 2, 3]))
        def s = sd(matrix, [1, 2])
        assertEquals(3.214550253664318, s[0])
        assertEquals(1.4849242404917498, s[1])
    }
}
