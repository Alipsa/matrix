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

        assertEquals(1.05g, mean(matrix, 0)[0])
        def means = mean(matrix, 1, 2)
        assertEquals(10/3, means[0])
        assertEquals(1.95g, means[1])
    }

    @Test
    void testMedian() {
        def matrix = [
                [1, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(1.05g, median(matrix, 0)[0])
        def medians = median(matrix, 1, 2)
        assertEquals(2 as BigDecimal, medians[0])
        assertEquals(1.95g, medians[1])
    }

    @Test
    void testMin() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(0.3, min(matrix, 0)[0])
        def mins = min(matrix, 1, 2)
        assertEquals(1, mins[0])
        assertEquals(0.9, mins[1])
    }

    @Test
    void testMax() {
        def matrix = [
                [0.3, 2, 3],
                [1.1, 1, 0.9],
                [null, 7, "2"]
        ]

        assertEquals(1.1, max(matrix, 0)[0])
        def maxs = max(matrix, 1, 2)
        assertEquals(7, maxs[0])
        assertEquals(3, maxs[1])
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
        def sds = sd(matrix, 1, 2)
        assertEquals(3.214550253664318, sds[0])
        assertEquals(1.4849242404917498, sds[1])
    }
}
