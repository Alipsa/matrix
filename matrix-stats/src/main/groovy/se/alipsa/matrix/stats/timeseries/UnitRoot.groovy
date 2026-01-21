package se.alipsa.matrix.stats.timeseries

import groovy.transform.CompileStatic

/**
 * A unit root test tests whether a time series variable is non-stationary and possesses a unit root.
 * The null hypothesis is generally defined as the presence of a unit root and the alternative hypothesis
 * is either stationarity, trend stationarity or explosive root depending on the test used.
 *
 * This class provides a convenient interface to run multiple unit root tests and compare results:
 * - Dickey-Fuller (DF): Basic test for unit root
 * - Augmented Dickey-Fuller (ADF): Extension with lagged differences
 * - ADF-GLS: More powerful variant using GLS detrending
 * - KPSS: Tests stationarity (reversed null hypothesis)
 *
 * Example:
 * <pre>
 * def data = [1.2, 1.5, 1.3, 1.6, 1.4, 1.7, ...] as double[]
 * def result = UnitRoot.test(data)
 * println result.summary()
 * </pre>
 *
 * Reference:
 * - Dickey, D. A., & Fuller, W. A. (1979). "Distribution of the Estimators for Autoregressive Time Series"
 * - Elliott, G., Rothenberg, T. J., & Stock, J. H. (1996). "Efficient Tests for an Autoregressive Unit Root"
 * - Kwiatkowski, D., et al. (1992). "Testing the Null Hypothesis of Stationarity"
 */
@CompileStatic
class UnitRoot {

  /**
   * Runs comprehensive unit root testing using multiple tests.
   *
   * @param data The time series data
   * @param type Type of deterministic component ('none', 'drift', or 'trend')
   * @param lags Number of lags for ADF tests (0 for automatic selection)
   * @return UnitRootResult containing results from all tests
   */
  static UnitRootResult test(double[] data, String type = 'drift', Integer lags = 0) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null")
    }

    if (data.length < 10) {
      throw new IllegalArgumentException("Need at least 10 observations for unit root tests (got ${data.length})")
    }

    if (!(type in ['none', 'drift', 'trend'])) {
      throw new IllegalArgumentException("Type must be 'none', 'drift', or 'trend' (got '${type}')")
    }

    // Convert to List for test methods
    List<Double> dataList = data.toList().collect { it as Double }

    // Map type for KPSS (uses 'level' or 'trend' instead of 'none'/'drift'/'trend')
    String kpssType = (type == 'none' || type == 'drift') ? 'level' : 'trend'

    // Run Dickey-Fuller test
    Df.DfResult dfResult = Df.test(dataList, type)

    // Run Augmented Dickey-Fuller test
    Adf.AdfResult adfResult = Adf.test(dataList, lags ?: 0, type)

    // Run ADF-GLS test
    AdfGls.AdfGlsResult adfGlsResult = AdfGls.test(dataList, lags ?: 0, type)

    // Run KPSS test (note: KPSS tests stationarity, opposite null hypothesis)
    Kpss.KpssResult kpssResult = Kpss.test(dataList, kpssType)

    return new UnitRootResult(
      dfResult: dfResult,
      adfResult: adfResult,
      adfGlsResult: adfGlsResult,
      kpssResult: kpssResult,
      sampleSize: data.length,
      type: type
    )
  }

  /**
   * Runs unit root testing with List input.
   */
  static UnitRootResult test(List<? extends Number> data, String type = 'drift', Integer lags = 0) {
    double[] array = data.collect { it.doubleValue() } as double[]
    return test(array, type, lags)
  }

  /**
   * Result class containing results from multiple unit root tests.
   */
  @CompileStatic
  static class UnitRootResult {
    /** Dickey-Fuller test result */
    Df.DfResult dfResult

    /** Augmented Dickey-Fuller test result */
    Adf.AdfResult adfResult

    /** ADF-GLS test result */
    AdfGls.AdfGlsResult adfGlsResult

    /** KPSS test result */
    Kpss.KpssResult kpssResult

    /** Sample size */
    int sampleSize

    /** Type of deterministic component tested */
    String type

    /**
     * Returns a summary of all test results.
     *
     * @param alpha Significance level (default 0.05)
     * @return Summary string with all test conclusions
     */
    String summary(double alpha = 0.05) {
      StringBuilder sb = new StringBuilder()
      sb.append("Unit Root Test Summary\n")
      sb.append("=" * 60).append("\n")
      sb.append("Sample size: ${sampleSize}\n")
      sb.append("Type: ${type}\n")
      sb.append("Significance level: ${alpha * 100}%\n")
      sb.append("=" * 60).append("\n\n")

      // DF Test
      sb.append("1. Dickey-Fuller Test:\n")
      sb.append("   Statistic: ${String.format('%.4f', dfResult.statistic)}\n")
      sb.append("   Critical value (5%): ${String.format('%.4f', dfResult.criticalValue5pct)}\n")
      sb.append("   Conclusion: ${dfResult.interpret()}\n\n")

      // ADF Test
      sb.append("2. Augmented Dickey-Fuller Test:\n")
      sb.append("   Lags: ${adfResult.lag}\n")
      sb.append("   Statistic: ${String.format('%.4f', adfResult.statistic)}\n")
      sb.append("   Critical value (5%): ${String.format('%.4f', adfResult.criticalValue)}\n")
      sb.append("   Conclusion: ${adfResult.interpret()}\n\n")

      // ADF-GLS Test
      sb.append("3. ADF-GLS Test:\n")
      sb.append("   Lags: ${adfGlsResult.lags}\n")
      sb.append("   Statistic: ${String.format('%.4f', adfGlsResult.statistic)}\n")
      sb.append("   Critical value (5%): ${String.format('%.4f', adfGlsResult.criticalValue5pct)}\n")
      sb.append("   Conclusion: ${adfGlsResult.interpret()}\n\n")

      // KPSS Test (note: opposite null hypothesis)
      sb.append("4. KPSS Test (tests stationarity, opposite null):\n")
      sb.append("   Lags: ${kpssResult.lags}\n")
      sb.append("   Statistic: ${String.format('%.4f', kpssResult.statistic)}\n")
      sb.append("   Critical value (5%): ${String.format('%.4f', kpssResult.criticalValue)}\n")
      sb.append("   Conclusion: ${kpssResult.interpret()}\n\n")

      // Overall assessment
      sb.append("=" * 60).append("\n")
      sb.append("Overall Assessment:\n")
      sb.append("=" * 60).append("\n")
      sb.append(getConsensus(alpha))

      return sb.toString()
    }

    /**
     * Determines the consensus conclusion from all tests.
     *
     * @param alpha Significance level
     * @return Consensus interpretation
     */
    String getConsensus(double alpha = 0.05) {
      int unitRootTests = 0  // DF, ADF, ADF-GLS reject null → stationary

      // Get critical values based on alpha
      double dfCritical = getCriticalValue(dfResult, alpha)
      double adfCritical = getCriticalValue(adfResult, alpha)
      double adfGlsCritical = getCriticalValue(adfGlsResult, alpha)

      // Count how many tests reject unit root (suggesting stationarity)
      // For DF/ADF/ADF-GLS: reject if statistic < critical value (more negative)
      if (dfResult.statistic < dfCritical) unitRootTests++
      if (adfResult.statistic < adfCritical) unitRootTests++
      if (adfGlsResult.statistic < adfGlsCritical) unitRootTests++

      // KPSS has opposite null: reject stationarity if statistic > critical value
      boolean kpssRejectsStationarity = kpssResult.statistic > kpssResult.criticalValue

      StringBuilder consensus = new StringBuilder()

      if (unitRootTests >= 2 && !kpssRejectsStationarity) {
        consensus.append("Strong evidence of STATIONARITY:\n")
        consensus.append("- ${unitRootTests}/3 unit root tests reject the null hypothesis\n")
        consensus.append("- KPSS test does not reject stationarity\n")
        consensus.append("Conclusion: Series appears to be stationary")
      } else if (unitRootTests == 0 && kpssRejectsStationarity) {
        consensus.append("Strong evidence of UNIT ROOT (non-stationarity):\n")
        consensus.append("- All unit root tests fail to reject the null hypothesis\n")
        consensus.append("- KPSS test rejects stationarity\n")
        consensus.append("Conclusion: Series appears to have a unit root")
      } else if (unitRootTests >= 2) {
        consensus.append("Mixed evidence, leaning toward STATIONARITY:\n")
        consensus.append("- ${unitRootTests}/3 unit root tests reject the null hypothesis\n")
        consensus.append("- But KPSS suggests non-stationarity\n")
        consensus.append("Conclusion: Results are conflicting, but majority suggests stationarity")
      } else if (kpssRejectsStationarity) {
        consensus.append("Mixed evidence, leaning toward UNIT ROOT:\n")
        consensus.append("- ${unitRootTests}/3 unit root tests reject the null hypothesis\n")
        consensus.append("- KPSS test rejects stationarity\n")
        consensus.append("Conclusion: Results are conflicting, but evidence suggests unit root")
      } else {
        consensus.append("INCONCLUSIVE evidence:\n")
        consensus.append("- ${unitRootTests}/3 unit root tests reject the null hypothesis\n")
        consensus.append("- KPSS test does not reject stationarity\n")
        consensus.append("Conclusion: Results are mixed, consider additional analysis")
      }

      return consensus.toString()
    }

    /**
     * Gets the appropriate critical value for a result object based on alpha.
     */
    private static double getCriticalValue(Object result, double alpha) {
      // Handle different result types
      if (result instanceof Df.DfResult) {
        Df.DfResult dfRes = (Df.DfResult) result
        if (alpha <= 0.01) return dfRes.criticalValue1pct
        else if (alpha <= 0.05) return dfRes.criticalValue5pct
        else return dfRes.criticalValue10pct
      } else if (result instanceof AdfGls.AdfGlsResult) {
        AdfGls.AdfGlsResult adfGlsRes = (AdfGls.AdfGlsResult) result
        if (alpha <= 0.01) return adfGlsRes.criticalValue1pct
        else if (alpha <= 0.05) return adfGlsRes.criticalValue5pct
        else return adfGlsRes.criticalValue10pct
      } else if (result instanceof Adf.AdfResult) {
        // ADF only has one critical value (5%)
        return ((Adf.AdfResult) result).criticalValue
      } else {
        throw new IllegalArgumentException("Unknown result type: ${result.class}")
      }
    }

    /**
     * Checks if the series is likely stationary based on test consensus.
     *
     * @param alpha Significance level
     * @return true if consensus suggests stationarity
     */
    boolean isStationary(double alpha = 0.05) {
      int unitRootTests = 0
      double dfCritical = getCriticalValue(dfResult, alpha)
      double adfCritical = getCriticalValue(adfResult, alpha)
      double adfGlsCritical = getCriticalValue(adfGlsResult, alpha)

      if (dfResult.statistic < dfCritical) unitRootTests++
      if (adfResult.statistic < adfCritical) unitRootTests++
      if (adfGlsResult.statistic < adfGlsCritical) unitRootTests++

      boolean kpssRejectsStationarity = kpssResult.statistic > kpssResult.criticalValue

      // Consider stationary if majority of unit root tests reject AND KPSS doesn't reject stationarity
      return unitRootTests >= 2 && !kpssRejectsStationarity
    }

    /**
     * Checks if the series has a unit root based on test consensus.
     *
     * @param alpha Significance level
     * @return true if consensus suggests unit root
     */
    boolean hasUnitRoot(double alpha = 0.05) {
      int unitRootTests = 0
      double dfCritical = getCriticalValue(dfResult, alpha)
      double adfCritical = getCriticalValue(adfResult, alpha)
      double adfGlsCritical = getCriticalValue(adfGlsResult, alpha)

      if (dfResult.statistic < dfCritical) unitRootTests++
      if (adfResult.statistic < adfCritical) unitRootTests++
      if (adfGlsResult.statistic < adfGlsCritical) unitRootTests++

      boolean kpssRejectsStationarity = kpssResult.statistic > kpssResult.criticalValue

      // Consider unit root if no unit root tests reject AND KPSS rejects stationarity
      return unitRootTests == 0 && kpssRejectsStationarity
    }

    @Override
    String toString() {
      return summary()
    }
  }
}
