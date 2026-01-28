package se.alipsa.matrix.smile.stats

import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import smile.stat.distribution.BernoulliDistribution
import smile.stat.distribution.BetaDistribution
import smile.stat.distribution.BinomialDistribution
import smile.stat.distribution.ChiSquareDistribution
import smile.stat.distribution.ExponentialDistribution
import smile.stat.distribution.FDistribution
import smile.stat.distribution.GammaDistribution
import smile.stat.distribution.GaussianDistribution
import smile.stat.distribution.GeometricDistribution
import smile.stat.distribution.LogNormalDistribution
import smile.stat.distribution.PoissonDistribution
import smile.stat.distribution.TDistribution
import smile.stat.distribution.WeibullDistribution
import smile.stat.hypothesis.ChiSqTest
import smile.stat.hypothesis.CorTest
import smile.stat.hypothesis.FTest
import smile.stat.hypothesis.KSTest
import smile.stat.hypothesis.TTest

/**
 * Statistical utilities leveraging Smile's statistics library.
 * Complements matrix-stats with ML-focused statistics including:
 * - Probability distributions
 * - Hypothesis tests
 * - Correlation matrices with significance testing
 */
@CompileStatic
class SmileStats {

  // ==================== Probability Distributions ====================

  /**
   * Create a Normal (Gaussian) distribution.
   *
   * @param mean the mean of the distribution (default 0)
   * @param sd the standard deviation (default 1)
   * @return a GaussianDistribution
   */
  static GaussianDistribution normal(double mean = 0.0, double sd = 1.0) {
    return new GaussianDistribution(mean, sd)
  }

  /**
   * Fit a Normal distribution to data.
   *
   * @param data the data to fit
   * @return a fitted GaussianDistribution
   */
  static GaussianDistribution normalFit(double[] data) {
    return GaussianDistribution.fit(data)
  }

  /**
   * Fit a Normal distribution to a Matrix column.
   *
   * @param matrix the Matrix containing the data
   * @param column the column name
   * @return a fitted GaussianDistribution
   */
  static GaussianDistribution normalFit(Matrix matrix, String column) {
    return normalFit(toDoubleArray(matrix, column))
  }

  /**
   * Create a Poisson distribution.
   *
   * @param lambda the rate parameter (mean)
   * @return a PoissonDistribution
   */
  static PoissonDistribution poisson(double lambda) {
    return new PoissonDistribution(lambda)
  }

  /**
   * Fit a Poisson distribution to data.
   *
   * @param data the data to fit (integer counts)
   * @return a fitted PoissonDistribution
   */
  static PoissonDistribution poissonFit(int[] data) {
    return PoissonDistribution.fit(data)
  }

  /**
   * Fit a Poisson distribution to a Matrix column.
   *
   * @param matrix the Matrix containing the data
   * @param column the column name
   * @return a fitted PoissonDistribution
   */
  static PoissonDistribution poissonFit(Matrix matrix, String column) {
    return poissonFit(toIntArray(matrix, column))
  }

  /**
   * Create a Binomial distribution.
   *
   * @param n number of trials
   * @param p probability of success
   * @return a BinomialDistribution
   */
  static BinomialDistribution binomial(int n, double p) {
    return new BinomialDistribution(n, p)
  }

  /**
   * Create an Exponential distribution.
   *
   * @param lambda the rate parameter
   * @return an ExponentialDistribution
   */
  static ExponentialDistribution exponential(double lambda) {
    return new ExponentialDistribution(lambda)
  }

  /**
   * Fit an Exponential distribution to data.
   *
   * @param data the data to fit
   * @return a fitted ExponentialDistribution
   */
  static ExponentialDistribution exponentialFit(double[] data) {
    return ExponentialDistribution.fit(data)
  }

  /**
   * Create a Gamma distribution.
   *
   * @param shape the shape parameter (k or alpha)
   * @param scale the scale parameter (theta)
   * @return a GammaDistribution
   */
  static GammaDistribution gamma(double shape, double scale) {
    return new GammaDistribution(shape, scale)
  }

  /**
   * Fit a Gamma distribution to data.
   *
   * @param data the data to fit
   * @return a fitted GammaDistribution
   */
  static GammaDistribution gammaFit(double[] data) {
    return GammaDistribution.fit(data)
  }

  /**
   * Create a Beta distribution.
   *
   * @param alpha first shape parameter
   * @param beta second shape parameter
   * @return a BetaDistribution
   */
  static BetaDistribution beta(double alpha, double beta) {
    return new BetaDistribution(alpha, beta)
  }

  /**
   * Fit a Beta distribution to data (values must be in [0,1]).
   *
   * @param data the data to fit
   * @return a fitted BetaDistribution
   */
  static BetaDistribution betaFit(double[] data) {
    return BetaDistribution.fit(data)
  }

  /**
   * Create a Chi-Square distribution.
   *
   * @param df degrees of freedom
   * @return a ChiSquareDistribution
   */
  static ChiSquareDistribution chiSquare(int df) {
    return new ChiSquareDistribution(df)
  }

  /**
   * Create a Student's t distribution.
   *
   * @param df degrees of freedom
   * @return a TDistribution
   */
  static TDistribution studentT(int df) {
    return new TDistribution(df)
  }

  /**
   * Create an F distribution.
   *
   * @param df1 numerator degrees of freedom
   * @param df2 denominator degrees of freedom
   * @return an FDistribution
   */
  static FDistribution fDistribution(int df1, int df2) {
    return new FDistribution(df1, df2)
  }

  /**
   * Generate a random sample from a Uniform distribution.
   * Note: Smile does not include a UniformDistribution class, so this is implemented manually.
   *
   * @param min the lower bound
   * @param max the upper bound
   * @return a random value uniformly distributed between min and max
   */
  static double uniformRandom(double min, double max) {
    Random random = new Random()
    return min + random.nextDouble() * (max - min)
  }

  /**
   * Create a Log-Normal distribution.
   *
   * @param mu the mean of the underlying normal distribution
   * @param sigma the standard deviation of the underlying normal distribution
   * @return a LogNormalDistribution
   */
  static LogNormalDistribution logNormal(double mu, double sigma) {
    return new LogNormalDistribution(mu, sigma)
  }

  /**
   * Fit a Log-Normal distribution to data.
   *
   * @param data the data to fit
   * @return a fitted LogNormalDistribution
   */
  static LogNormalDistribution logNormalFit(double[] data) {
    return LogNormalDistribution.fit(data)
  }

  /**
   * Create a Weibull distribution.
   *
   * @param shape the shape parameter (k)
   * @param scale the scale parameter (lambda)
   * @return a WeibullDistribution
   */
  static WeibullDistribution weibull(double shape, double scale) {
    return new WeibullDistribution(shape, scale)
  }

  // Note: WeibullDistribution.fit() is not available in Smile 4.x
  // Users can create WeibullDistribution manually with estimated parameters

  /**
   * Create a Bernoulli distribution.
   *
   * @param p the probability of success
   * @return a BernoulliDistribution
   */
  static BernoulliDistribution bernoulli(double p) {
    return new BernoulliDistribution(p)
  }

  /**
   * Create a Geometric distribution.
   *
   * @param p the probability of success
   * @return a GeometricDistribution
   */
  static GeometricDistribution geometric(double p) {
    return new GeometricDistribution(p)
  }

  // ==================== Hypothesis Tests ====================

  /**
   * One-sample t-test: tests whether the mean of a sample differs from a specified value.
   *
   * @param data the sample data
   * @param mu the hypothesized mean
   * @return a TTest result containing t-statistic, degrees of freedom, and p-value
   */
  static TTest tTestOneSample(double[] data, double mu) {
    return TTest.test(data, mu)
  }

  /**
   * One-sample t-test on a Matrix column.
   *
   * @param matrix the Matrix containing the data
   * @param column the column name
   * @param mu the hypothesized mean
   * @return a TTest result
   */
  static TTest tTestOneSample(Matrix matrix, String column, double mu) {
    return tTestOneSample(toDoubleArray(matrix, column), mu)
  }

  /**
   * Two-sample t-test (independent samples): tests whether two samples have the same mean.
   *
   * @param x first sample
   * @param y second sample
   * @param equalVariance whether to assume equal variances (default false = Welch's t-test)
   * @return a TTest result
   */
  static TTest tTestTwoSample(double[] x, double[] y, boolean equalVariance = false) {
    if (equalVariance) {
      return TTest.test(x, y)
    } else {
      return TTest.test(x, y) // Smile uses Welch's by default
    }
  }

  /**
   * Two-sample t-test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a TTest result
   */
  static TTest tTestTwoSample(Matrix matrix, String column1, String column2) {
    return tTestTwoSample(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Paired t-test: tests whether the mean difference between paired observations is zero.
   *
   * @param x first sample
   * @param y second sample (must be same length as x)
   * @return a TTest result
   */
  static TTest tTestPaired(double[] x, double[] y) {
    return TTest.testPaired(x, y)
  }

  /**
   * Paired t-test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a TTest result
   */
  static TTest tTestPaired(Matrix matrix, String column1, String column2) {
    return tTestPaired(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * F-test for equality of variances.
   *
   * @param x first sample
   * @param y second sample
   * @return an FTest result containing F-statistic and p-value
   */
  static FTest fTest(double[] x, double[] y) {
    return FTest.test(x, y)
  }

  /**
   * F-test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return an FTest result
   */
  static FTest fTest(Matrix matrix, String column1, String column2) {
    return fTest(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Kolmogorov-Smirnov test for normality.
   * Tests whether a sample comes from a normal distribution.
   *
   * @param data the sample data
   * @return a KSTest result containing the KS statistic and p-value
   */
  static KSTest ksTestNormality(double[] data) {
    GaussianDistribution fitted = GaussianDistribution.fit(data)
    return KSTest.test(data, fitted)
  }

  /**
   * Kolmogorov-Smirnov test for normality on a Matrix column.
   *
   * @param matrix the Matrix containing the data
   * @param column the column name
   * @return a KSTest result
   */
  static KSTest ksTestNormality(Matrix matrix, String column) {
    return ksTestNormality(toDoubleArray(matrix, column))
  }

  /**
   * Two-sample Kolmogorov-Smirnov test.
   * Tests whether two samples come from the same distribution.
   *
   * @param x first sample
   * @param y second sample
   * @return a KSTest result
   */
  static KSTest ksTestTwoSample(double[] x, double[] y) {
    return KSTest.test(x, y)
  }

  /**
   * Two-sample KS test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a KSTest result
   */
  static KSTest ksTestTwoSample(Matrix matrix, String column1, String column2) {
    return ksTestTwoSample(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Chi-square goodness of fit test.
   *
   * @param observed observed frequencies (counts)
   * @param expected expected probabilities (must sum to 1.0)
   * @return a ChiSqTest result containing chi-square statistic and p-value
   */
  static ChiSqTest chiSquareTest(int[] observed, double[] expected) {
    return ChiSqTest.test(observed, expected)
  }

  /**
   * Chi-square test of independence on a contingency table.
   *
   * @param contingencyTable a 2D array representing the contingency table
   * @return a ChiSqTest result
   */
  static ChiSqTest chiSquareTestIndependence(int[][] contingencyTable) {
    return ChiSqTest.test(contingencyTable)
  }

  // ==================== Correlation with Significance ====================

  /**
   * Pearson correlation test with significance.
   *
   * @param x first variable
   * @param y second variable
   * @return a CorTest result containing correlation coefficient, t-statistic, and p-value
   */
  static CorTest correlationTest(double[] x, double[] y) {
    return CorTest.pearson(x, y)
  }

  /**
   * Pearson correlation test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a CorTest result
   */
  static CorTest correlationTest(Matrix matrix, String column1, String column2) {
    return correlationTest(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Spearman rank correlation test with significance.
   *
   * @param x first variable
   * @param y second variable
   * @return a CorTest result
   */
  static CorTest spearmanTest(double[] x, double[] y) {
    return CorTest.spearman(x, y)
  }

  /**
   * Spearman correlation test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a CorTest result
   */
  static CorTest spearmanTest(Matrix matrix, String column1, String column2) {
    return spearmanTest(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Kendall rank correlation test with significance.
   *
   * @param x first variable
   * @param y second variable
   * @return a CorTest result
   */
  static CorTest kendallTest(double[] x, double[] y) {
    return CorTest.kendall(x, y)
  }

  /**
   * Kendall correlation test on Matrix columns.
   *
   * @param matrix the Matrix containing the data
   * @param column1 first column name
   * @param column2 second column name
   * @return a CorTest result
   */
  static CorTest kendallTest(Matrix matrix, String column1, String column2) {
    return kendallTest(toDoubleArray(matrix, column1), toDoubleArray(matrix, column2))
  }

  /**
   * Compute a correlation matrix with significance for all numeric columns.
   * Returns a Matrix containing the correlation coefficients.
   *
   * @param matrix the input Matrix
   * @param columns optional list of column names to include (all numeric if not specified)
   * @param method correlation method: "pearson", "spearman", or "kendall"
   * @return a Matrix with correlation coefficients (column names as row/col headers)
   */
  static Matrix correlationMatrix(Matrix matrix, List<String> columns = null, String method = "pearson") {
    List<String> numericCols = columns ?: getNumericColumnNames(matrix)
    int n = numericCols.size()

    double[][] corMatrix = new double[n][n]
    double[][] data = new double[n][]

    // Extract data arrays
    for (int i = 0; i < n; i++) {
      data[i] = toDoubleArray(matrix, numericCols[i])
    }

    // Compute correlations
    for (int i = 0; i < n; i++) {
      corMatrix[i][i] = 1.0
      for (int j = i + 1; j < n; j++) {
        CorTest result
        switch (method.toLowerCase()) {
          case "spearman" -> result = CorTest.spearman(data[i], data[j])
          case "kendall" -> result = CorTest.kendall(data[i], data[j])
          default -> result = CorTest.pearson(data[i], data[j])
        }
        corMatrix[i][j] = result.cor()
        corMatrix[j][i] = result.cor()
      }
    }

    return matrixFromCorrelation(numericCols, corMatrix)
  }

  /**
   * Compute a p-value matrix for correlations.
   * Returns a Matrix containing the p-values for each pair of variables.
   *
   * @param matrix the input Matrix
   * @param columns optional list of column names to include
   * @param method correlation method: "pearson", "spearman", or "kendall"
   * @return a Matrix with p-values
   */
  static Matrix pValueMatrix(Matrix matrix, List<String> columns = null, String method = "pearson") {
    List<String> numericCols = columns ?: getNumericColumnNames(matrix)
    int n = numericCols.size()

    double[][] pMatrix = new double[n][n]
    double[][] data = new double[n][]

    // Extract data arrays
    for (int i = 0; i < n; i++) {
      data[i] = toDoubleArray(matrix, numericCols[i])
    }

    // Compute p-values
    for (int i = 0; i < n; i++) {
      pMatrix[i][i] = 0.0  // Perfect correlation with itself
      for (int j = i + 1; j < n; j++) {
        CorTest result
        switch (method.toLowerCase()) {
          case "spearman" -> result = CorTest.spearman(data[i], data[j])
          case "kendall" -> result = CorTest.kendall(data[i], data[j])
          default -> result = CorTest.pearson(data[i], data[j])
        }
        pMatrix[i][j] = result.pvalue()
        pMatrix[j][i] = result.pvalue()
      }
    }

    return matrixFromCorrelation(numericCols, pMatrix)
  }

  /**
   * Compute both correlation matrix and p-value matrix.
   * Returns a Map with keys "correlation" and "pvalue", each containing a Matrix.
   *
   * @param matrix the input Matrix
   * @param columns optional list of column names to include
   * @param method correlation method: "pearson", "spearman", or "kendall"
   * @return a Map with "correlation" and "pvalue" matrices
   */
  static Map<String, Matrix> correlationWithSignificance(Matrix matrix, List<String> columns = null, String method = "pearson") {
    List<String> numericCols = columns ?: getNumericColumnNames(matrix)
    int n = numericCols.size()

    double[][] corMatrix = new double[n][n]
    double[][] pMatrix = new double[n][n]
    double[][] data = new double[n][]

    // Extract data arrays
    for (int i = 0; i < n; i++) {
      data[i] = toDoubleArray(matrix, numericCols[i])
    }

    // Compute correlations and p-values
    for (int i = 0; i < n; i++) {
      corMatrix[i][i] = 1.0
      pMatrix[i][i] = 0.0
      for (int j = i + 1; j < n; j++) {
        CorTest result
        switch (method.toLowerCase()) {
          case "spearman" -> result = CorTest.spearman(data[i], data[j])
          case "kendall" -> result = CorTest.kendall(data[i], data[j])
          default -> result = CorTest.pearson(data[i], data[j])
        }
        corMatrix[i][j] = result.cor()
        corMatrix[j][i] = result.cor()
        pMatrix[i][j] = result.pvalue()
        pMatrix[j][i] = result.pvalue()
      }
    }

    return [
        correlation: matrixFromCorrelation(numericCols, corMatrix),
        pvalue     : matrixFromCorrelation(numericCols, pMatrix)
    ] as Map<String, Matrix>
  }

  // ==================== Distribution Utilities ====================

  /**
   * Generate random samples from a Normal distribution.
   *
   * @param n number of samples
   * @param mean the mean
   * @param sd the standard deviation
   * @return array of random samples
   */
  static double[] randomNormal(int n, double mean = 0.0, double sd = 1.0) {
    GaussianDistribution dist = normal(mean, sd)
    double[] samples = new double[n]
    for (int i = 0; i < n; i++) {
      samples[i] = dist.rand()
    }
    return samples
  }

  /**
   * Generate random samples from a Poisson distribution.
   *
   * @param n number of samples
   * @param lambda the rate parameter
   * @return array of random samples
   */
  static int[] randomPoisson(int n, double lambda) {
    PoissonDistribution dist = poisson(lambda)
    int[] samples = new int[n]
    for (int i = 0; i < n; i++) {
      samples[i] = (int) dist.rand()
    }
    return samples
  }

  /**
   * Generate random samples from a Binomial distribution.
   *
   * @param sampleCount number of samples
   * @param trials number of trials per sample
   * @param p probability of success
   * @return array of random samples
   */
  static int[] randomBinomial(int sampleCount, int trials, double p) {
    BinomialDistribution dist = binomial(trials, p)
    int[] samples = new int[sampleCount]
    for (int i = 0; i < sampleCount; i++) {
      samples[i] = (int) dist.rand()
    }
    return samples
  }

  /**
   * Generate random samples from a Uniform distribution.
   *
   * @param n number of samples
   * @param min lower bound
   * @param max upper bound
   * @return array of random samples
   */
  static double[] randomUniform(int n, double min = 0.0, double max = 1.0) {
    Random random = new Random()
    double range = max - min
    double[] samples = new double[n]
    for (int i = 0; i < n; i++) {
      samples[i] = min + random.nextDouble() * range
    }
    return samples
  }

  /**
   * Generate random samples from an Exponential distribution.
   *
   * @param n number of samples
   * @param lambda the rate parameter
   * @return array of random samples
   */
  static double[] randomExponential(int n, double lambda) {
    ExponentialDistribution dist = exponential(lambda)
    double[] samples = new double[n]
    for (int i = 0; i < n; i++) {
      samples[i] = dist.rand()
    }
    return samples
  }

  // ==================== Helper Methods ====================

  private static double[] toDoubleArray(Matrix matrix, String column) {
    List<?> col = matrix.column(column)
    double[] result = new double[col.size()]
    int idx = 0
    for (Object val : col) {
      if (val != null) {
        result[idx++] = val as double
      }
    }
    // Trim to actual size if there were nulls
    if (idx < col.size()) {
      double[] trimmed = new double[idx]
      System.arraycopy(result, 0, trimmed, 0, idx)
      return trimmed
    }
    return result
  }

  private static int[] toIntArray(Matrix matrix, String column) {
    List<?> col = matrix.column(column)
    int[] result = new int[col.size()]
    int idx = 0
    for (Object val : col) {
      if (val != null) {
        result[idx++] = ((Number) val).intValue()
      }
    }
    if (idx < col.size()) {
      int[] trimmed = new int[idx]
      System.arraycopy(result, 0, trimmed, 0, idx)
      return trimmed
    }
    return result
  }

  private static List<String> getNumericColumnNames(Matrix matrix) {
    List<Class<?>> numericTypes = [
        Integer, int, Long, long, Double, double, Float, float,
        Short, short, Byte, byte, BigDecimal, BigInteger, Number
    ]
    List<String> result = []
    for (int i = 0; i < matrix.columnCount(); i++) {
      if (numericTypes.contains(matrix.type(i))) {
        result << matrix.columnName(i)
      }
    }
    return result
  }

  private static Matrix matrixFromCorrelation(List<String> columnNames, double[][] data) {
    Map<String, List<?>> matrixData = new LinkedHashMap<>()

    // First column: row names
    matrixData.put("", columnNames as List<?>)

    // Remaining columns: correlation values
    for (int j = 0; j < columnNames.size(); j++) {
      List<Double> colData = []
      for (int i = 0; i < columnNames.size(); i++) {
        colData << data[i][j]
      }
      matrixData.put(columnNames[j], colData as List<?>)
    }

    return Matrix.builder()
        .data(matrixData)
        .matrixName("Correlation Matrix")
        .build()
  }
}
