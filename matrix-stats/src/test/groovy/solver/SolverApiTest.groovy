package solver

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.solver.BrentSolver
import se.alipsa.matrix.stats.solver.LinearProgramSolver
import se.alipsa.matrix.stats.solver.MultivariateObjective
import se.alipsa.matrix.stats.solver.NelderMeadOptimizer
import se.alipsa.matrix.stats.solver.UnivariateObjective

@CompileStatic
class SolverApiTest {

  @Test
  void testBrentSolverNumberOverloadAndValueAccessors() {
    BrentSolver.SolverResult result = BrentSolver.solve(
      { double x -> x * x - 2.0d } as UnivariateObjective,
      0.0G,
      2.0G,
      0.000000000001G,
      0.000000000001G,
      100
    )

    assertEquals(Math.sqrt(2.0d), result.rootValue as double, 1e-10d)
    assertTrue(result.lowerBoundValue <= result.rootValue)
    assertTrue(result.upperBoundValue >= result.rootValue)
  }

  @Test
  void testLinearProgramSolverListOverloadAndValueAccessors() {
    LinearProgramSolver.Solution result = LinearProgramSolver.minimize(
      [1.0, 2.0],
      [[1.0, 1.0]],
      [1.0]
    )

    assertEquals(2, result.pointValues.size())
    assertEquals(1.0d, result.pointValues[0] as double, 1e-10d)
    assertEquals(0.0d, result.pointValues[1] as double, 1e-10d)
    assertEquals(1.0d, result.objectiveValue as double, 1e-10d)
  }

  @Test
  void testNelderMeadListOverloadAndValueAccessors() {
    NelderMeadOptimizer.OptimizationResult result = NelderMeadOptimizer.minimize(
      { double[] point ->
        double dx = point[0] - 1.0d
        double dy = point[1] + 2.0d
        dx * dx + dy * dy
      } as MultivariateObjective,
      [0.0, 0.0],
      [0.5, 0.5],
      500,
      0.000001G,
      0.000001G
    )

    assertEquals(2, result.pointValues.size())
    assertEquals(1.0d, result.pointValues[0] as double, 1e-3d)
    assertEquals(-2.0d, result.pointValues[1] as double, 1e-3d)
    assertEquals(0.0d, result.objectiveValue as double, 1e-6d)
  }
}
