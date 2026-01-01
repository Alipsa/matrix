package se.alipsa.matrix.gg.stat

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.stats.distribution.TDistribution
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.regression.PolynomialRegression

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Consolidated statistical transformation utilities for ggplot.
 * All methods are static and delegate to matrix-core Stat or matrix-stats where possible.
 */
@CompileStatic
class GgStat {

  /**
   * Identity transformation - returns data unchanged.
   * Default stat for most geoms.
   */
  static Matrix identity(Matrix data, Aes aes) {
    return data
  }

  /**
   * Count occurrences of each unique value.
   * Used by geom_bar() to count observations in each category.
   * Delegates to Stat.frequency() or Stat.countBy().
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for grouping)
   * @return Matrix with columns: x (original column name), count, percent
   */
  static Matrix count(Matrix data, Aes aes) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_count requires x aesthetic")
    }
    // Delegate to matrix-core Stat.frequency
    Matrix freq = Stat.frequency(data[xCol])
    // Rename columns to match ggplot2 computed variables:
    // "Value" -> original x column name, "Frequency" -> "count", "Percent" -> "percent"
    return freq.renameColumn(Stat.FREQUENCY_VALUE, xCol)
               .renameColumn(Stat.FREQUENCY_FREQUENCY, 'count')
               .renameColumn(Stat.FREQUENCY_PERCENT, 'percent')
  }

  /**
   * Bin continuous data into intervals.
   * Used by geom_histogram().
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x)
   * @param params Map with optional: bins (int), binwidth (Number), boundary, closed
   * @return Matrix with columns: x (bin center), xmin, xmax, count, density
   */
  static Matrix bin(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_bin requires x aesthetic")
    }

    List<Number> values = data[xCol] as List<Number>
    if (values == null || values.isEmpty()) {
      return Matrix.builder()
          .columnNames(['x', 'xmin', 'xmax', 'count', 'density'])
          .rows([])
          .build()
    }

    int bins = (params.bins ?: 30) as int
    Number binwidth = params.binwidth as Number

    // Calculate bin edges
    Number minVal = Stat.min(values)
    Number maxVal = Stat.max(values)
    Number range = (maxVal as double) - (minVal as double)

    // Handle edge case: all values are the same
    if (range == 0 || range.doubleValue() == 0.0d) {
      // Single bin containing all values
      return Matrix.builder()
          .columnNames(['x', 'xmin', 'xmax', 'count', 'density'])
          .rows([[minVal, minVal, minVal, values.size(), 1.0]])
          .types([BigDecimal, BigDecimal, BigDecimal, Integer, BigDecimal])
          .build()
    }

    if (binwidth == null) {
      binwidth = range / bins
    } else {
      bins = Math.ceil(range / (binwidth as double)) as int
      if (bins == 0) bins = 1
    }

    // Create bin counts
    List<Map> results = []
    for (int i = 0; i < bins; i++) {
      Number xmin = (minVal as double) + i * (binwidth as double)
      Number xmax = (minVal as double) + (i + 1) * (binwidth as double)
      Number center = ((xmin as double) + (xmax as double)) / 2

      int count = (int) values.count { v ->
        (v as double) >= (xmin as double) && (i == bins - 1 ? (v as double) <= (xmax as double) : (v as double) < (xmax as double))
      }

      double density = count / (values.size() * (binwidth as double))

      results << [
          x: center,
          xmin: xmin,
          xmax: xmax,
          count: count,
          density: density
      ]
    }

    return Matrix.builder().mapList(results).build()
  }

  /**
   * Compute boxplot statistics.
   * Delegates to Stat.quartiles(), Stat.iqr(), Stat.median().
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for grouping, y for values)
   * @return Matrix with columns: x, ymin, lower, middle, upper, ymax, outliers
   */
  static Matrix boxplot(Matrix data, Aes aes) {
    String yCol = aes.yColName
    if (yCol == null) {
      throw new IllegalArgumentException("stat_boxplot requires y aesthetic")
    }

    String xCol = aes.xColName

    // Group by x if present, otherwise compute for all data
    Map<String, Matrix> groups
    if (xCol != null) {
      groups = Stat.groupBy(data, xCol)
    } else {
      groups = ['all': data]
    }

    List<Map> results = groups.collect { groupKey, groupData ->
      List<Number> values = groupData[yCol] as List<Number>
      List<Number> quartiles = Stat.quartiles(values)
      Number median = Stat.median(values)
      Number iqr = Stat.iqr(values)

      Number whiskerLow = Math.max(Stat.min(values) as double, (quartiles[0] - 1.5 * iqr) as double)
      Number whiskerHigh = Math.min(Stat.max(values) as double, (quartiles[1] + 1.5 * iqr) as double)

      List<Number> outliers = values.findAll { v ->
        v < whiskerLow || v > whiskerHigh
      }

      [
          x: groupKey,
          ymin: whiskerLow,
          lower: quartiles[0],
          middle: median,
          upper: quartiles[1],
          ymax: whiskerHigh,
          outliers: outliers
      ]
    } as List<Map>

    return Matrix.builder().mapList(results).build()
  }

  /** Pattern to parse poly(x, n) in formulas */
  private static final Pattern POLY_PATTERN = Pattern.compile(/poly\s*\(\s*(\w+)\s*,\s*(\d+)\s*\)/)

  /**
   * Parse a regression formula to extract polynomial degree if present.
   * Supports: "y ~ x" (linear), "y ~ poly(x, 2)" (quadratic), "y ~ poly(x, 3)" (cubic), etc.
   *
   * @param formula The formula string
   * @return Map with keys: polyDegree (int, 1 for linear), response (String), predictor (String)
   */
  static Map<String, Object> parseFormula(String formula) {
    if (formula == null || formula.trim().isEmpty()) {
      return [polyDegree: 1, response: 'y', predictor: 'x']
    }

    String cleaned = formula.trim()

    // Split on ~
    String[] parts = cleaned.split('~')
    String response = parts.length > 0 ? parts[0].trim() : 'y'
    String rhs = parts.length > 1 ? parts[1].trim() : 'x'

    // Ensure both sides are non-empty after trimming; fall back to defaults if needed
    if (response.isEmpty()) {
      response = 'y'
    }
    if (rhs.isEmpty()) {
      rhs = 'x'
    }
    // Check for poly(x, n) pattern
    Matcher matcher = POLY_PATTERN.matcher(rhs)
    if (matcher.find()) {
      String predictor = matcher.group(1)
      int degree = Integer.parseInt(matcher.group(2))
      return [polyDegree: degree, response: response, predictor: predictor]
    }

    // Simple linear: y ~ x
    return [polyDegree: 1, response: response, predictor: rhs]
  }

  /**
   * Compute smoothed line (regression).
   * Supports linear regression and polynomial regression via formula parameter.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x and y)
   * @param params Map with optional:
   *   - method ('lm'): regression method
   *   - formula ('y ~ x'): formula string, supports poly(x, n)
   *   - se (true): show confidence interval
   *   - level (0.95): confidence level
   *   - n (80): number of fitted points
   *   - degree (1): polynomial degree; overrides degree parsed from formula if provided
   * @return Matrix with columns: x, y (fitted), ymin, ymax (if se=true)
   */
  @CompileDynamic
  static Matrix smooth(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_smooth requires x and y aesthetics")
    }

    String method = params.method ?: 'lm'
    boolean se = params.se != false
    double level = params.level != null ? (params.level as double) : 0.95d

    // Determine polynomial degree: check for conflicting 'degree' and 'formula' specifications
    int polyDegree
    String formula = params.formula as String
    if (params.degree != null && formula != null) {
      // Both degree and formula provided: ensure they are not in conflict
      int explicitDegree = params.degree as int
      Map<String, Object> formulaParsed = parseFormula(formula)
      def formulaDegreeObj = formulaParsed.polyDegree
      if (formulaDegreeObj != null) {
        int formulaDegree = (formulaDegreeObj as int)
        if (formulaDegree != explicitDegree) {
          throw new IllegalArgumentException(
            "Conflicting polynomial degrees: 'degree' parameter is " +
              explicitDegree + " but 'formula' specifies degree " + formulaDegree
          )
        }
      }
      polyDegree = explicitDegree
    } else if (params.degree != null) {
      // Direct degree parameter (Groovy-style shortcut)
      polyDegree = params.degree as int
    } else {
      // Parse formula string (R-style)
      Map<String, Object> formulaParsed = parseFormula(formula)
      polyDegree = (formulaParsed.polyDegree ?: 1) as int
    }

    List<Number> rawX = data[xCol] as List<Number>
    List<Number> rawY = data[yCol] as List<Number>
    List<Number> xValues = []
    List<Number> yValues = []
    int maxIdx = Math.min(rawX?.size() ?: 0, rawY?.size() ?: 0)
    for (int i = 0; i < maxIdx; i++) {
      def xVal = rawX[i]
      def yVal = rawY[i]
      if (xVal instanceof Number && yVal instanceof Number) {
        xValues << xVal
        yValues << yVal
      }
    }

    int minDataPoints = polyDegree + 1
    if (xValues.size() < minDataPoints) {
      return Matrix.builder().columnNames(['x', 'y']).rows([]).build()
    }

    // Choose regression model based on polynomial degree
    def regression
    if (polyDegree > 1) {
      regression = new PolynomialRegression(xValues, yValues, polyDegree)
    } else {
      regression = new LinearRegression(xValues, yValues)
    }

    // Generate fitted values
    Number xMin = Stat.min(xValues)
    Number xMax = Stat.max(xValues)
    int nPoints = (params.n ?: 80) as int

    // Compute standard error components for linear regression
    // Note: SE for polynomial regression is more complex; currently only supported for linear
    double sxx = 0.0d
    double xBar = (xValues.sum() as double) / xValues.size()
    xValues.each { Number val ->
      double diff = (val as double) - xBar
      sxx += diff * diff
    }

    double sigma2 = 0.0d
    int dfResid = xValues.size() - (polyDegree + 1)
    if (se && dfResid > 0 && sxx > 0.0d && polyDegree == 1) {
      double sse = 0.0d
      for (int i = 0; i < xValues.size(); i++) {
        double xi = xValues[i] as double
        double yi = yValues[i] as double
        double yFit = regression.predict(xi).doubleValue()
        double resid = yi - yFit
        sse += resid * resid
      }
      sigma2 = sse / dfResid
      if (sigma2 <= 0.0d) {
        se = false
      }
    } else {
      // Disable SE for polynomial (not yet implemented)
      if (polyDegree > 1) se = false
      if (dfResid <= 0) se = false
    }

    double tCrit = se ? tCritical(dfResid, level) : 0.0d

    List<List<?>> results = []
    for (int i = 0; i < nPoints; i++) {
      Number x = xMin + (xMax - xMin) * i / (nPoints - 1)
      BigDecimal yFit = regression.predict(x)
      if (se) {
        double dx = (x as double) - xBar
        double seFit = Math.sqrt(sigma2 * (1.0d / xValues.size() + (dx * dx) / sxx))
        BigDecimal margin = BigDecimal.valueOf(tCrit * seFit)
        BigDecimal yMin = yFit - margin
        BigDecimal yMax = yFit + margin
        results << [x as BigDecimal, yFit, yMin, yMax]
      } else {
        results << [x as BigDecimal, yFit]
      }
    }

    if (se) {
      return Matrix.builder()
          .columnNames(['x', 'y', 'ymin', 'ymax'])
          .rows(results)
          .types([BigDecimal, BigDecimal, BigDecimal, BigDecimal])
          .build()
    }
    return Matrix.builder()
        .columnNames(['x', 'y'])
        .rows(results)
        .types([BigDecimal, BigDecimal])
        .build()
  }

  private static double tCritical(int df, double level) {
    if (df <= 0) return 0.0d
    double target = 0.5d + level / 2.0d
    TDistribution dist = new TDistribution(df as double)
    double low = 0.0d
    double high = 1.0d
    while (dist.cdf(high) < target && high < 1.0e6) {
      high *= 2.0d
    }
    for (int i = 0; i < 80; i++) {
      double mid = (low + high) / 2.0d
      if (dist.cdf(mid) < target) {
        low = mid
      } else {
        high = mid
      }
    }
    return (low + high) / 2.0d
  }

  /**
   * Compute summary statistics by group.
   * Delegates to Stat.meanBy(), Stat.medianBy(), etc.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings
   * @param params Map with: fun ('mean', 'median', 'sum', 'min', 'max')
   * @return Matrix with summary values
   */
  static Matrix summary(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName
    String fun = params.fun ?: params.'fun.y' ?: 'mean'

    if (yCol == null) {
      throw new IllegalArgumentException("stat_summary requires y aesthetic")
    }

    if (xCol != null) {
      // Grouped summary
      switch (fun) {
        case 'mean':
          return Stat.meanBy(data, yCol, xCol)
        case 'median':
          return Stat.medianBy(data, yCol, xCol)
        case 'sum':
          return Stat.sumBy(data, yCol, xCol)
        default:
          throw new IllegalArgumentException("Unknown summary function: $fun")
      }
    } else {
      // Single summary value
      List<Number> values = data[yCol] as List<Number>
      Number result
      switch (fun) {
        case 'mean':
          result = Stat.mean(values)
          break
        case 'median':
          result = Stat.median(values)
          break
        case 'sum':
          result = Stat.sum(values)
          break
        case 'min':
          result = Stat.min(values)
          break
        case 'max':
          result = Stat.max(values)
          break
        default:
          throw new IllegalArgumentException("Unknown summary function: $fun")
      }
      return Matrix.builder().data([y: [result]]).build()
    }
  }

  /**
   * Kernel density estimation.
   * TODO: Implement or delegate to Smile's KernelDensity
   */
  static Matrix density(Matrix data, Aes aes, Map params = [:]) {
    // Placeholder - needs implementation
    throw new UnsupportedOperationException("stat_density not yet implemented")
  }

  /**
   * 2D binning.
   * TODO: Implement
   */
  static Matrix bin2d(Matrix data, Aes aes, Map params = [:]) {
    // Placeholder - needs implementation
    throw new UnsupportedOperationException("stat_bin2d not yet implemented")
  }

  /**
   * Contour computation.
   * TODO: Implement marching squares algorithm
   */
  static Matrix contour(Matrix data, Aes aes, Map params = [:]) {
    // Placeholder - needs implementation
    throw new UnsupportedOperationException("stat_contour not yet implemented")
  }
}
