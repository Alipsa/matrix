import org.junit.jupiter.api.Test

import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static se.alipsa.matrix.Correlation.*;

class CorrelationTest {

  /**
   * in R
   * print(cor(c(15, 18, 21, 24, 27), c(25, 25, 27, 31, 32)))
   * [1] 0.95346258924559
   */
  @Test
  void testPearsonCorrelation() {
    assertEquals(
        0.95346258924559,
        cor([15, 18, 21, 24, 27], [25, 25, 27, 31, 32]).setScale(14, RoundingMode.HALF_EVEN))
  }

  /**
   * print(cor(c(15, 18, 21, 15, 21), c(25, 25, 27, 27, 27), method = "spearman"))
   * [1] 0.45643546458764
   */
  @Test
  void testSpearmanCorrelation() {
    assertEquals(
        0.45643546458764,
        cor([15, 18, 21, 15, 21], [25, 25, 27, 27, 27], SPEARMAN).setScale(14, RoundingMode.HALF_EVEN))
  }
}
