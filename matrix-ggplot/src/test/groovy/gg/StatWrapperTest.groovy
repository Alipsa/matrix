package gg

import org.junit.jupiter.api.Test
import se.alipsa.matrix.gg.stat.StatsBin
import se.alipsa.matrix.gg.stat.StatsDensity
import se.alipsa.matrix.gg.stat.StatsEcdf
import se.alipsa.matrix.gg.stat.StatsQq
import se.alipsa.matrix.gg.stat.StatsQqLine
import se.alipsa.matrix.gg.stat.StatsSmooth
import se.alipsa.matrix.gg.stat.Stats
import se.alipsa.matrix.gg.stat.StatsYDensity

import static org.junit.jupiter.api.Assertions.assertTrue
import static se.alipsa.matrix.gg.GgPlot.*

class StatWrapperTest {

  @Test
  void testStatWrappers() {
    assertTrue(stat_bin() instanceof StatsBin)
    assertTrue(stat_density() instanceof StatsDensity)
    assertTrue(stat_smooth() instanceof StatsSmooth)
    assertTrue(stat_ydensity() instanceof StatsYDensity)
    assertTrue(stat_ecdf() instanceof StatsEcdf)
    assertTrue(stat_qq() instanceof StatsQq)
    assertTrue(stat_qq_line() instanceof StatsQqLine)
    assertTrue(stat_sample() instanceof Stats)
  }
}
