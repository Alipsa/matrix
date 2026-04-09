package se.alipsa.matrix.stats.timeseries

import static org.junit.jupiter.api.Assertions.assertEquals

import org.ejml.simple.SimpleMatrix
import org.ejml.simple.SimpleSVD
import org.junit.jupiter.api.Test

import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.util.Logger
import se.alipsa.matrix.stats.linalg.Linalg
import se.alipsa.matrix.stats.linalg.SvdResult
import se.alipsa.matrix.stats.regression.MultipleLinearRegression
import se.alipsa.matrix.stats.solver.BrentSolver
import se.alipsa.matrix.stats.solver.LinearProgramSolver
import se.alipsa.matrix.stats.solver.MultivariateObjective
import se.alipsa.matrix.stats.solver.NelderMeadOptimizer
import se.alipsa.matrix.stats.solver.UnivariateObjective

/**
 * Informational section 4.5 benchmarks for the initial hotspot areas identified in PR 1.
 *
 * These tests deliberately avoid strict timing assertions because CI environments vary.
 * Instead they verify output equivalence and log warm benchmark timings for the idiomatic
 * Groovy-facing path versus the retained primitive kernel or primitive overload.
 */
@SuppressWarnings(['DuplicateNumberLiteral', 'MethodName'])
class Section45BenchmarkTest {

  private static final Logger log = Logger.getLogger(Section45BenchmarkTest)
  private static final int WARMUPS = 2
  private static final int RUNS = 5

  @Test
  void benchmarkLinalgSvdFacadeAgainstPrimitiveKernel() {
    double[][] values = createDenseMatrix(18)
    Matrix matrix = toMatrix(values)

    SvdResult groovyResult = Linalg.svd(matrix)
    double[] primitiveValues = primitiveSingularValues(values)
    assertEquals(primitiveValues.length, groovyResult.singularValues.size())
    for (int i = 0; i < primitiveValues.length; i++) {
      assertEquals(primitiveValues[i], groovyResult.singularValues[i] as double, 1e-10d)
    }

    List<Long> groovyTimings = benchmarkSamples {
      for (int i = 0; i < 15; i++) {
        Linalg.svd(matrix)
      }
    }
    List<Long> primitiveTimings = benchmarkSamples {
      for (int i = 0; i < 15; i++) {
        primitiveSingularValues(values)
      }
    }

    logBenchmark('linalg.svd(Matrix)', groovyTimings, 'primitive EJML SVD', primitiveTimings)
  }

  @Test
  void benchmarkMultipleLinearRegressionListConstructorAgainstPrimitiveConstructor() {
    RegressionData data = regressionData(160)

    MultipleLinearRegression groovyModel = new MultipleLinearRegression(data.responseValues, data.predictorValues)
    MultipleLinearRegression primitiveModel = new MultipleLinearRegression(data.response, data.predictors)
    assertEquals(primitiveModel.coefficients.length, groovyModel.coefficients.length)
    for (int i = 0; i < primitiveModel.coefficients.length; i++) {
      assertEquals(primitiveModel.coefficients[i], groovyModel.coefficients[i], 1e-12d)
    }

    List<Long> groovyTimings = benchmarkSamples {
      for (int i = 0; i < 40; i++) {
        new MultipleLinearRegression(data.responseValues, data.predictorValues)
      }
    }
    List<Long> primitiveTimings = benchmarkSamples {
      for (int i = 0; i < 40; i++) {
        new MultipleLinearRegression(data.response, data.predictors)
      }
    }

    logBenchmark('MultipleLinearRegression(List, List)', groovyTimings, 'MultipleLinearRegression(double[], double[][])', primitiveTimings)
  }

  @Test
  void benchmarkBrentSolverNumberOverloadAgainstPrimitiveOverload() {
    UnivariateObjective scalarObjective = { double x -> x * x * x - x - 2.0d } as UnivariateObjective
    BrentSolver.SolverResult groovyBrent = BrentSolver.solve(
      scalarObjective,
      1.0G,
      2.0G,
      0.000000000001G,
      0.000000000001G,
      100
    )
    BrentSolver.SolverResult primitiveBrent = BrentSolver.solve(
      scalarObjective,
      1.0d,
      2.0d,
      1e-12d,
      1e-12d,
      100
    )
    assertEquals(primitiveBrent.root, groovyBrent.root, 1e-12d)

    List<Long> brentGroovyTimings = benchmarkSamples {
      for (int i = 0; i < 120; i++) {
        BrentSolver.solve(scalarObjective, 1.0G, 2.0G, 0.000000000001G, 0.000000000001G, 100)
      }
    }
    List<Long> brentPrimitiveTimings = benchmarkSamples {
      for (int i = 0; i < 120; i++) {
        BrentSolver.solve(scalarObjective, 1.0d, 2.0d, 1e-12d, 1e-12d, 100)
      }
    }
    logBenchmark('BrentSolver.solve(Number...)', brentGroovyTimings, 'BrentSolver.solve(double...)', brentPrimitiveTimings)
  }

  @Test
  void benchmarkLinearProgramSolverListOverloadAgainstPrimitiveOverload() {
    List<BigDecimal> objectiveValues = [2.0, 1.0, 3.0]
    List<List<BigDecimal>> constraintValues = [
      [1.0, 1.0, 0.0],
      [0.0, 1.0, 1.0]
    ]
    List<BigDecimal> rhsValues = [4.0, 3.0]
    double[] objective = [2.0d, 1.0d, 3.0d] as double[]
    double[][] constraints = [
      [1.0d, 1.0d, 0.0d],
      [0.0d, 1.0d, 1.0d]
    ] as double[][]
    double[] rhs = [4.0d, 3.0d] as double[]

    LinearProgramSolver.Solution groovyLp = LinearProgramSolver.minimize(objectiveValues, constraintValues, rhsValues)
    LinearProgramSolver.Solution primitiveLp = LinearProgramSolver.minimize(objective, constraints, rhs)
    assertEquals(primitiveLp.value, groovyLp.value, 1e-12d)
    for (int i = 0; i < primitiveLp.point.length; i++) {
      assertEquals(primitiveLp.point[i], groovyLp.point[i], 1e-12d)
    }

    List<Long> lpGroovyTimings = benchmarkSamples {
      for (int i = 0; i < 90; i++) {
        LinearProgramSolver.minimize(objectiveValues, constraintValues, rhsValues)
      }
    }
    List<Long> lpPrimitiveTimings = benchmarkSamples {
      for (int i = 0; i < 90; i++) {
        LinearProgramSolver.minimize(objective, constraints, rhs)
      }
    }
    logBenchmark('LinearProgramSolver.minimize(List, List, List)', lpGroovyTimings, 'LinearProgramSolver.minimize(double[], double[][], double[])', lpPrimitiveTimings)
  }

  @Test
  void benchmarkNelderMeadListOverloadAgainstPrimitiveOverload() {
    MultivariateObjective vectorObjective = { double[] point ->
      double dx = point[0] - 1.0d
      double dy = point[1] + 2.0d
      dx * dx + dy * dy
    } as MultivariateObjective

    NelderMeadOptimizer.OptimizationResult groovyNelder = NelderMeadOptimizer.minimize(
      vectorObjective,
      [0.0, 0.0],
      [0.5, 0.5],
      500,
      0.000001G,
      0.000001G
    )
    NelderMeadOptimizer.OptimizationResult primitiveNelder = NelderMeadOptimizer.minimize(
      vectorObjective,
      [0.0d, 0.0d] as double[],
      [0.5d, 0.5d] as double[],
      500,
      1e-6d,
      1e-6d
    )
    assertEquals(primitiveNelder.value, groovyNelder.value, 1e-9d)
    assertEquals(primitiveNelder.point[0], groovyNelder.point[0], 1e-6d)
    assertEquals(primitiveNelder.point[1], groovyNelder.point[1], 1e-6d)

    List<Long> nelderGroovyTimings = benchmarkSamples {
      for (int i = 0; i < 80; i++) {
        NelderMeadOptimizer.minimize(vectorObjective, [0.0, 0.0], [0.5, 0.5], 500, 0.000001G, 0.000001G)
      }
    }
    List<Long> nelderPrimitiveTimings = benchmarkSamples {
      for (int i = 0; i < 80; i++) {
        NelderMeadOptimizer.minimize(vectorObjective, [0.0d, 0.0d] as double[], [0.5d, 0.5d] as double[], 500, 1e-6d, 1e-6d)
      }
    }
    logBenchmark('NelderMeadOptimizer.minimize(List, List)', nelderGroovyTimings, 'NelderMeadOptimizer.minimize(double[], double[])', nelderPrimitiveTimings)
  }

  @Test
  void benchmarkTimeseriesListNormalizationAgainstPrimitiveHelper() {
    RegressionData data = regressionData(220)

    double[] groovyCoefficients = fitOlsFromLists(data.responseValues, data.predictorValues)
    double[] primitiveCoefficients = TimeSeriesUtils.fitOLS(data.response, data.predictors)
    assertEquals(primitiveCoefficients.length, groovyCoefficients.length)
    for (int i = 0; i < primitiveCoefficients.length; i++) {
      assertEquals(primitiveCoefficients[i], groovyCoefficients[i], 1e-12d)
    }

    List<Long> groovyTimings = benchmarkSamples {
      for (int i = 0; i < 60; i++) {
        fitOlsFromLists(data.responseValues, data.predictorValues)
      }
    }
    List<Long> primitiveTimings = benchmarkSamples {
      for (int i = 0; i < 60; i++) {
        TimeSeriesUtils.fitOLS(data.response, data.predictors)
      }
    }

    logBenchmark('timeseries list normalization + fitOLS', groovyTimings, 'TimeSeriesUtils.fitOLS(double[], double[][])', primitiveTimings)
  }

  private static Matrix toMatrix(double[][] values) {
    int columnCount = values[0].length
    Matrix.builder()
      .columnNames((0..<columnCount).collect { int idx -> "c${idx}" })
      .rows(values.collect { double[] row -> row.toList() })
      .types((0..<columnCount).collect { Double })
      .build()
  }

  private static double[][] createDenseMatrix(int size) {
    double[][] values = new double[size][size]
    for (int row = 0; row < size; row++) {
      for (int column = 0; column < size; column++) {
        if (row == column) {
          values[row][column] = size + row + 1.0d
        } else {
          values[row][column] = ((row + column) % 7 + 1) / 20.0d
        }
      }
    }
    values
  }

  private static double[] primitiveSingularValues(double[][] values) {
    SimpleSVD<SimpleMatrix> decomposition = new SimpleMatrix(values).svd()
    int singularValueCount = Math.min(decomposition.getW().numRows(), decomposition.getW().numCols())
    double[] singularValues = new double[singularValueCount]
    for (int i = 0; i < singularValueCount; i++) {
      singularValues[i] = decomposition.getW().get(i, i)
    }
    singularValues
  }

  private static RegressionData regressionData(int rowCount) {
    double[] response = new double[rowCount]
    double[][] predictors = new double[rowCount][3]
    List<BigDecimal> responseValues = []
    List<List<BigDecimal>> predictorValues = []

    for (int row = 0; row < rowCount; row++) {
      double x1 = row / 10.0d
      double x2 = Math.sin(row / 13.0d)
      predictors[row][0] = 1.0d
      predictors[row][1] = x1
      predictors[row][2] = x2
      response[row] = 2.5d + 0.75d * x1 - 0.5d * x2 + Math.cos(row / 17.0d) * 0.03d
      responseValues << BigDecimal.valueOf(response[row])
      predictorValues << predictors[row].collect { double value -> BigDecimal.valueOf(value) }
    }

    new RegressionData(
      response: response,
      predictors: predictors,
      responseValues: responseValues,
      predictorValues: predictorValues
    )
  }

  private static double[] fitOlsFromLists(List<? extends Number> responseValues, List<? extends List<? extends Number>> predictorValues) {
    double[] response = responseValues.collect { Number value -> value as double } as double[]
    double[][] predictors = predictorValues.collect { List<? extends Number> row ->
      row.collect { Number value -> value as double } as double[]
    } as double[][]
    TimeSeriesUtils.fitOLS(response, predictors)
  }

  private static List<Long> benchmarkSamples(Closure action) {
    for (int i = 0; i < WARMUPS; i++) {
      action.call()
    }

    List<Long> samples = []
    for (int i = 0; i < RUNS; i++) {
      long start = System.nanoTime()
      action.call()
      samples << (System.nanoTime() - start)
    }
    samples
  }

  private static void logBenchmark(String groovyLabel, List<Long> groovyTimings, String primitiveLabel, List<Long> primitiveTimings) {
    log.info(
      "${groovyLabel} avg=${avgMs(groovyTimings)}ms min=${minMs(groovyTimings)}ms max=${maxMs(groovyTimings)}ms; " +
      "${primitiveLabel} avg=${avgMs(primitiveTimings)}ms min=${minMs(primitiveTimings)}ms max=${maxMs(primitiveTimings)}ms"
    )
  }

  private static String avgMs(List<Long> values) {
    String.format(Locale.US, '%.2f', ((values.sum() as long) / 1_000_000.0d) / values.size())
  }

  private static String minMs(List<Long> values) {
    String.format(Locale.US, '%.2f', (values.min() as long) / 1_000_000.0d)
  }

  private static String maxMs(List<Long> values) {
    String.format(Locale.US, '%.2f', (values.max() as long) / 1_000_000.0d)
  }

  private static class RegressionData {
    double[] response
    double[][] predictors
    List<BigDecimal> responseValues
    List<List<BigDecimal>> predictorValues
  }
}
