package solver

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

import groovy.transform.CompileStatic

import org.junit.jupiter.api.Test

import se.alipsa.matrix.stats.solver.BrentSolver
import se.alipsa.matrix.stats.solver.GoalSeek
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
  void testLinearProgramSolverRejectsInfeasibleEqualityConstraints() {
    IllegalStateException exception = assertThrows(IllegalStateException) {
      LinearProgramSolver.minimize(
        [1.0],
        [[1.0], [1.0]],
        [1.0, 2.0]
      )
    }

    assertEquals('Linear program is infeasible', exception.message)
  }

  @Test
  void testLinearProgramSolverHandlesFeasibleRedundantConstraints() {
    LinearProgramSolver.Solution result = LinearProgramSolver.minimize(
      [1.0, 2.0],
      [[1.0, 1.0], [2.0, 2.0]],
      [1.0, 2.0]
    )

    assertEquals(1.0d, result.pointValues[0] as double, 1e-10d)
    assertEquals(0.0d, result.pointValues[1] as double, 1e-10d)
    assertEquals(1.0d, result.objectiveValue as double, 1e-10d)
    assertTrue(result.iterations > 0)
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

  @Test
  void testGoalSeekTypedResultAndValueAccessors() {
    GoalSeek.Result result = GoalSeek.solve(70.0G, 0.0G, 100.0G, 0.0001G, 100) { Number grade ->
      (50.0G + 80.0G + 60.0G + (grade as BigDecimal)) / 4.0G
    }

    assertEquals(90.0d, result.value, 1e-8d)
    assertEquals(70.0d, result.result, 1e-8d)
    assertEquals(0.0d, result.diff, 1e-8d)
    assertTrue(result.iterations > 0)
    assertEquals(90.0d, result.computedValue as double, 1e-8d)
    assertEquals(70.0d, result.resultValue as double, 1e-8d)
    assertEquals(0.0d, result.diffValue as double, 1e-8d)
  }

  @Test
  void testGoalSeekNumberOverloadWithCustomThresholdAndDefaultMaxIterations() {
    GoalSeek.Result result = GoalSeek.solve(70.0G, 0.0G, 100.0G, 0.0001G) { Number grade ->
      (50.0G + 80.0G + 60.0G + (grade as BigDecimal)) / 4.0G
    }

    assertEquals(90.0d, result.value, 1e-8d)
    assertEquals(70.0d, result.result, 1e-8d)
    assertEquals(0.0d, result.diff, 1e-8d)
    assertTrue(result.iterations > 0)
  }

  @Test
  void testGoalSeekResultIsImmutable() {
    GoalSeek.Result result = GoalSeek.solve(70.0G, 0.0G, 100.0G, 0.0001G, 100) { Number grade ->
      (50.0G + 80.0G + 60.0G + (grade as BigDecimal)) / 4.0G
    }

    ['value', 'result', 'diff', 'iterations'].each { String propertyName ->
      assertThrows(ReadOnlyPropertyException) {
        result.setProperty(propertyName, 0.0d)
      }
    }
  }

  @Test
  void testGoalSeekReportsZeroIterationsForExactEndpointRoot() {
    GoalSeek.Result result = GoalSeek.solve(0.0G, 0.0G, 10.0G, 0.0001G, 100) { Number value ->
      value
    }

    assertEquals(0.0d, result.value, 1e-8d)
    assertEquals(0.0d, result.result, 1e-8d)
    assertEquals(0.0d, result.diff, 1e-8d)
    assertEquals(0, result.iterations)
  }

  @Test
  void testGoalSeekExplicitMapCoercion() {
    Map<String, Object> result = GoalSeek.solve(27000.0G, 0.0G, 100.0G) { Number value ->
      (value as BigDecimal) * 100_000.0G
    } as Map<String, Object>

    assertEquals(0.270d, result.value as double, 1e-8d)
    assertEquals(27000.0d, result.result as double, 1e-6d)
    assertEquals(0.0d, result.diff as double, 1e-6d)
    assertTrue(result.iterations as int > 0)
  }

  @Test
  void testObjectiveInterfacesAcceptGroovyFacingInputs() {
    UnivariateObjective scalarObjective = { double x -> x * x } as UnivariateObjective
    MultivariateObjective vectorObjective = { double[] point -> point[0] + point[1] } as MultivariateObjective

    assertEquals(2.25d, scalarObjective.value(1.5G), 1e-10d)
    assertEquals(3.0d, vectorObjective.value([1.0G, 2.0G]), 1e-10d)
  }
}
