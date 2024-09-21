package rootfinding

import static org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.alipsa.groovy.stats.rootfinding.GoalSeek

class GoalSeekTest {

  @Test
  void testGoalSeek() {
    Map val = GoalSeek.solve(27000, 0, 100) {
      it * 100_000
    }
    println val
    assertTrue(val.interations < 37)
    assertEquals(0.270, val.value, 0.0000001)
  }
}
