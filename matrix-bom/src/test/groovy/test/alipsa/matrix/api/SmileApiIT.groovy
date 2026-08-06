package test.alipsa.matrix.api

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.alipsa.matrix.smile.SmileUtil
import se.alipsa.matrix.smile.stats.SmileStats

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/** Covers Smile DataFrame conversion, summaries, distributions, and correlation tests. */
@Tag('smile')
class SmileApiIT implements ApiItSupport {

  @Test
  void smileConversionsAndStatistics() {
    def frame = SmileUtil.toDataFrame(mtcars())
    def restored = SmileUtil.toMatrix(frame)
    assertEquals(mtcars().rowCount(), restored.rowCount())
    assertEquals(mtcars().columnCount(), restored.columnCount())
    assertEquals(mtcars().columnCount(), SmileUtil.describe(mtcars()).columnCount())
    assertEquals(2.0d, SmileStats.normal(2, 1.5).mean(), 0.0001d)
    def fitted = SmileStats.normalFit(mtcars(), 'mpg')
    assertTrue(fitted.sd() > 0)
    assertTrue(SmileStats.correlationTest(mtcars(), 'mpg', 'wt').pvalue() < 0.05)
  }
}
