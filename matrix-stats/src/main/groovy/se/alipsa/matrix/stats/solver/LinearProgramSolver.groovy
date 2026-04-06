package se.alipsa.matrix.stats.solver

/**
 * Two-phase simplex solver for linear programs in equality form with non-negative variables.
 *
 * <p>This solver intentionally uses primitive double tableaux and vectors. The simplex
 * pivots are numerically low-level operations where primitive arrays keep the code fast
 * and predictable while the public stats APIs still expose Groovy-friendly types.</p>
 */
@SuppressWarnings('DuplicateNumberLiteral')
final class LinearProgramSolver {

  private static final double EPSILON = 1e-10

  private LinearProgramSolver() {
  }

  /**
   * Solves an equality-form linear program with non-negative variables.
   *
   * @param objective minimization objective coefficients
   * @param constraints equality constraint matrix
   * @param rhs right-hand-side vector
   * @param maxIterations maximum simplex iterations per phase
   * @return optimal solution point and objective value
   */
  static Solution minimize(double[] objective, double[][] constraints, double[] rhs, int maxIterations = 10_000) {
    validateInputs(objective, constraints, rhs, maxIterations)

    int constraintCount = rhs.length
    int variableCount = objective.length
    double[][] normalizedConstraints = new double[constraintCount][variableCount]
    double[] normalizedRhs = new double[constraintCount]

    for (int i = 0; i < constraintCount; i++) {
      double sign = rhs[i] < 0.0d ? -1.0d : 1.0d
      normalizedRhs[i] = rhs[i] * sign
      for (int j = 0; j < variableCount; j++) {
        normalizedConstraints[i][j] = constraints[i][j] * sign
      }
    }

    PhaseState phaseOne = buildPhaseOne(normalizedConstraints, normalizedRhs)
    iterateSimplex(phaseOne, phaseOne.objective, maxIterations)
    if (phaseOne.currentObjective() < -EPSILON) {
      throw new IllegalStateException("Linear program is infeasible")
    }

    PhaseState phaseTwo = removeArtificialVariables(phaseOne, variableCount)
    double[] maximizationObjective = negate(objective)
    iterateSimplex(phaseTwo, maximizationObjective, maxIterations)

    double[] point = new double[variableCount]
    for (int i = 0; i < phaseTwo.basis.length; i++) {
      int basisIndex = phaseTwo.basis[i]
      if (basisIndex < variableCount) {
        point[basisIndex] = phaseTwo.tableau[i][phaseTwo.rhsColumn]
      }
    }

    double objectiveValue = 0.0d
    for (int i = 0; i < variableCount; i++) {
      objectiveValue += objective[i] * point[i]
    }
    return new Solution(point: point, value: objectiveValue, iterations: phaseTwo.iterations)
  }

  private static PhaseState buildPhaseOne(double[][] constraints, double[] rhs) {
    int constraintCount = rhs.length
    int variableCount = constraints[0].length
    int totalVariables = variableCount + constraintCount
    int rhsColumn = totalVariables
    double[][] tableau = new double[constraintCount][rhsColumn + 1]
    int[] basis = new int[constraintCount]
    double[] objective = new double[totalVariables]

    for (int i = 0; i < constraintCount; i++) {
      System.arraycopy(constraints[i], 0, tableau[i], 0, variableCount)
      tableau[i][variableCount + i] = 1.0d
      tableau[i][rhsColumn] = rhs[i]
      basis[i] = variableCount + i
      objective[variableCount + i] = -1.0d
    }

    new PhaseState(tableau: tableau, basis: basis, rhsColumn: rhsColumn, iterations: 0, objective: objective)
  }

  private static PhaseState removeArtificialVariables(PhaseState phaseOne, int originalVariableCount) {
    double[][] tableau = phaseOne.tableau
    int[] basis = phaseOne.basis
    int rhsColumn = phaseOne.rhsColumn
    int rowCount = basis.length

    for (int row = 0; row < rowCount; row++) {
      if (basis[row] >= originalVariableCount) {
        int entering = findReplacementColumn(tableau[row], originalVariableCount)
        if (entering >= 0) {
          pivot(tableau, row, entering, rhsColumn)
          basis[row] = entering
        }
      }
    }

    List<Integer> keepRows = []
    for (int row = 0; row < rowCount; row++) {
      if (basis[row] < originalVariableCount || Math.abs(tableau[row][rhsColumn]) > EPSILON) {
        keepRows << row
      }
    }

    double[][] reducedTableau = new double[keepRows.size()][originalVariableCount + 1]
    int[] reducedBasis = new int[keepRows.size()]
    keepRows.eachWithIndex { Integer originalRow, int newRow ->
      System.arraycopy(tableau[originalRow], 0, reducedTableau[newRow], 0, originalVariableCount)
      reducedTableau[newRow][originalVariableCount] = tableau[originalRow][rhsColumn]
      reducedBasis[newRow] = basis[originalRow]
    }

    new PhaseState(tableau: reducedTableau, basis: reducedBasis, rhsColumn: originalVariableCount, iterations: phaseOne.iterations)
  }

  private static int findReplacementColumn(double[] row, int originalVariableCount) {
    for (int j = 0; j < originalVariableCount; j++) {
      if (Math.abs(row[j]) > EPSILON) {
        return j
      }
    }
    -1
  }

  private static void iterateSimplex(PhaseState state, double[] objective, int maxIterations) {
    for (int iter = 0; iter < maxIterations; iter++) {
      int entering = selectEnteringColumn(state, objective)
      if (entering < 0) {
        return
      }
      int leaving = selectLeavingRow(state, entering)
      if (leaving < 0) {
        throw new IllegalStateException("Linear program is unbounded")
      }
      pivot(state.tableau, leaving, entering, state.rhsColumn)
      state.basis[leaving] = entering
      state.iterations++
    }
    throw new IllegalStateException("Linear program failed to converge after $maxIterations iterations")
  }

  private static int selectEnteringColumn(PhaseState state, double[] objective) {
    for (int column = 0; column < objective.length; column++) {
      double reducedCost = objective[column]
      for (int row = 0; row < state.basis.length; row++) {
        reducedCost -= objective[state.basis[row]] * state.tableau[row][column]
      }
      if (reducedCost > EPSILON) {
        return column
      }
    }
    -1
  }

  private static int selectLeavingRow(PhaseState state, int enteringColumn) {
    int leaving = -1
    double bestRatio = Double.POSITIVE_INFINITY

    for (int row = 0; row < state.basis.length; row++) {
      double coefficient = state.tableau[row][enteringColumn]
      if (coefficient > EPSILON) {
        double ratio = state.tableau[row][state.rhsColumn] / coefficient
        if (ratio < bestRatio - EPSILON ||
            (Math.abs(ratio - bestRatio) <= EPSILON && (leaving < 0 || state.basis[row] < state.basis[leaving]))) {
          bestRatio = ratio
          leaving = row
        }
      }
    }
    leaving
  }

  private static void pivot(double[][] tableau, int pivotRow, int pivotColumn, int rhsColumn) {
    double pivot = tableau[pivotRow][pivotColumn]
    for (int column = 0; column <= rhsColumn; column++) {
      tableau[pivotRow][column] /= pivot
    }
    for (int row = 0; row < tableau.length; row++) {
      if (row == pivotRow) {
        continue
      }
      double factor = tableau[row][pivotColumn]
      if (factor == 0.0d) {
        continue
      }
      for (int column = 0; column <= rhsColumn; column++) {
        tableau[row][column] -= factor * tableau[pivotRow][column]
      }
    }
  }

  private static void validateInputs(double[] objective, double[][] constraints, double[] rhs, int maxIterations) {
    if (objective == null || objective.length == 0) {
      throw new IllegalArgumentException("Objective must contain at least one variable")
    }
    if (constraints == null || rhs == null || constraints.length == 0 || constraints.length != rhs.length) {
      throw new IllegalArgumentException("Constraints and right-hand side must have matching non-empty sizes")
    }
    if (!constraints.every { double[] row -> row.length == objective.length }) {
      throw new IllegalArgumentException("Constraint rows must match the objective dimension")
    }
    if (maxIterations < 1) {
      throw new IllegalArgumentException("maxIterations must be positive")
    }
  }

  private static double[] negate(double[] values) {
    double[] negated = new double[values.length]
    for (int i = 0; i < values.length; i++) {
      negated[i] = -values[i]
    }
    negated
  }

  static class Solution {
    /** Optimal point for the original variables. */
    double[] point
    /** Objective value at {@link #point}. */
    double value
    /** Total simplex iterations performed. */
    int iterations
  }

  private static class PhaseState {
    double[][] tableau
    int[] basis
    int rhsColumn
    int iterations
    double[] objective

    double currentObjective() {
      double value = 0.0d
      for (int i = 0; i < basis.length; i++) {
        if (basis[i] >= rhsColumn) {
          value -= tableau[i][rhsColumn]
        }
      }
      value
    }
  }
}
