package se.alipsa.matrix.gg.stat

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.stats.distribution.TDistribution
import se.alipsa.matrix.stats.kde.Kernel
import se.alipsa.matrix.stats.kde.KernelDensity
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.regression.PolynomialRegression
import se.alipsa.matrix.stats.regression.RegressionUtils

import java.math.RoundingMode
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
    String fillCol = aes.fillColName
    if (fillCol != null && data.columnNames().contains(fillCol)) {
      Map<List<Object>, Integer> counts = new LinkedHashMap<>()
      data.each { row ->
        Object xVal = row[xCol]
        Object fillVal = row[fillCol]
        List<Object> key = [xVal, fillVal]
        counts[key] = (counts[key] ?: 0) + 1
      }
      Map<Object, Integer> totalsByX = [:].withDefault { 0 }
      counts.each { key, count ->
        totalsByX[key[0]] = totalsByX[key[0]] + count
      }
      List<Map<String, Object>> rows = []
      counts.each { key, count ->
        int totalForX = totalsByX[key[0]] as int
        BigDecimal percent = totalForX == 0 ? BigDecimal.ZERO : (count * 100.0 / totalForX).setScale(2, RoundingMode.HALF_EVEN)
        rows << [
            (xCol): key[0],
            (fillCol): key[1],
            count: count,
            percent: percent
        ]
      }
      return Matrix.builder().mapList(rows).build()
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
   * When a group aesthetic is specified separately from x:
   * - Groups data by the group column for computing boxplot statistics
   * - Computes median x value for each group for positioning (ggplot2 behavior)
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses group or x for grouping, y for values)
   * @return Matrix with columns: x, group, ymin, lower, middle, upper, ymax, outliers, n, relvarwidth, width, xmin, xmax, xresolution
   */
  static Matrix boxplot(Matrix data, Aes aes, Map params = [:]) {
    String yCol = aes.yColName
    if (yCol == null) {
      throw new IllegalArgumentException("stat_boxplot requires y aesthetic")
    }

    String xCol = aes.xColName
    String groupCol = aes.groupColName

    // Determine grouping strategy
    boolean hasExplicitGroup = groupCol != null && data.columnNames().contains(groupCol)
    boolean hasContinuousX = xCol != null && data.columnNames().contains(xCol) && isNumericColumn(data, xCol)

    // Use group aesthetic if present, otherwise fall back to x
    String groupingCol = hasExplicitGroup ? groupCol : xCol

    // Group by the grouping column if present, otherwise compute for all data
    Map<String, Matrix> groups
    if (groupingCol != null && data.columnNames().contains(groupingCol)) {
      groups = Stat.groupBy(data, groupingCol)
    } else {
      groups = ['all': data]
    }

    Double xResolution = null
    if (hasContinuousX) {
      List<Number> xValues = (data[xCol] as List).findAll { it instanceof Number } as List<Number>
      xResolution = resolution(xValues)
    }
    double defaultWidth = params?.width instanceof Number ?
        (params.width as Number).doubleValue() :
        ((xResolution ?: 1.0d) * 0.75d)
    double coef = params?.coef instanceof Number ? (params.coef as Number).doubleValue() : 1.5d

    List<Map> results = groups.collect { groupKey, groupData ->
      List<Number> values = (groupData[yCol] as List).findAll { it instanceof Number } as List<Number>
      if (values.isEmpty()) {
        return null
      }
      values.sort()

      Number lower = quantileType7(values, 0.25d)
      Number middle = quantileType7(values, 0.5d)
      Number upper = quantileType7(values, 0.75d)
      Number iqr = (upper as double) - (lower as double)

      double lowerFence = (lower as double) - coef * iqr
      double upperFence = (upper as double) + coef * iqr

      List<Number> outliers = values.findAll { v ->
        (v as double) < lowerFence || (v as double) > upperFence
      }
      List<Number> inliers = values.findAll { v ->
        (v as double) >= lowerFence && (v as double) <= upperFence
      }
      Number whiskerLow = inliers.isEmpty() ? values.first() : inliers.min()
      Number whiskerHigh = inliers.isEmpty() ? values.last() : inliers.max()

      // Compute x position: if we have explicit group and continuous x, use center of x range
      // ggplot2 positions boxes at the center (mean of min and max) of the x range within each group
      def xPosition
      Number xMin = null
      Number xMax = null
      if (hasExplicitGroup && hasContinuousX) {
        List<Number> xValues = (groupData[xCol] as List).findAll { it instanceof Number } as List<Number>
        if (!xValues.isEmpty()) {
          xValues.sort()
          xMin = xValues.first()
          xMax = xValues.last()
          // Use center of the x range for positioning
          xPosition = ((xMin as double) + (xMax as double)) / 2.0d
        }
      } else {
        xPosition = groupKey
      }

      double widthValue = defaultWidth
      if (groupData.columnNames().contains('width')) {
        def widthColVal = (groupData['width'] as List)?.find { it instanceof Number }
        if (widthColVal instanceof Number) {
          widthValue = (widthColVal as Number).doubleValue()
        }
      } else if (hasContinuousX && xMin != null && xMax != null) {
        double range = (xMax as double) - (xMin as double)
        if (range > 0d) {
          widthValue = range * 0.9d
        }
      }
      Number xmin = null
      Number xmax = null
      if (xPosition instanceof Number && widthValue > 0d) {
        double half = widthValue / 2.0d
        xmin = (xPosition as Number).doubleValue() - half
        xmax = (xPosition as Number).doubleValue() + half
      }

      // Build result map - only include xmin/xmax if they have values
      Map result = [
          x: xPosition,
          group: groupKey,
          ymin: whiskerLow,
          lower: lower,
          middle: middle,
          upper: upper,
          ymax: whiskerHigh,
          outliers: outliers,
          n: values.size(),  // sample size for potential width scaling
          relvarwidth: Math.sqrt(values.size()),
          width: widthValue,
          xresolution: xResolution
      ]
      if (xmin != null && xmax != null) {
        result['xmin'] = xmin
        result['xmax'] = xmax
      }
      result
    } as List<Map>

    List<Map> filtered = results.findAll { it != null } as List<Map>

    return Matrix.builder().mapList(filtered).build()
  }

  /**
   * Check if a column contains numeric values.
   */
  private static boolean isNumericColumn(Matrix data, String colName) {
    if (!data.columnNames().contains(colName) || data.rowCount() == 0) {
      return false
    }
    def firstNonNull = data[colName].find { it != null }
    return firstNonNull instanceof Number
  }

  /** Pattern to parse poly(x, n) in formulas */
  private static final Pattern POLY_PATTERN = Pattern.compile(/poly\s*\(\s*(\w+)\s*,\s*(\d+)\s*\)/)

  /**
   * Parse a regression formula to extract polynomial degree if present.
   * Supports: "y ~ x" (linear), "y ~ poly(x, 2)" (quadratic), "y ~ poly(x, 3)" (cubic), etc.
   *
   * @param formula The formula string
   * @return Map with keys: polyDegree (int, 1 for linear), response (String), predictor (String),
   *   polyExplicit (boolean, true when formula includes poly(...))
   */
  static Map<String, Object> parseFormula(String formula) {
    if (formula == null || formula.trim().isEmpty()) {
      return [polyDegree: 1, response: 'y', predictor: 'x', polyExplicit: false] as Map<String, Object>
    }

    String cleaned = formula.trim()

    // Split on ~ if present, otherwise treat the whole string as predictor
    boolean hasTilde = cleaned.contains('~')
    String response
    String rhs
    if (hasTilde) {
      String[] parts = cleaned.split('~', 2)
      response = parts.length > 0 ? parts[0].trim() : 'y'
      rhs = parts.length > 1 ? parts[1].trim() : 'x'
    } else {
      response = 'y'
      rhs = cleaned
    }

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
      if (degree < 1) {
        throw new IllegalArgumentException(
          "Polynomial degree must be at least 1, got: $degree in formula '$formula'"
        )
      }
      return [polyDegree: degree, response: response, predictor: predictor, polyExplicit: true]
    }

    // Simple linear: y ~ x
    return [polyDegree: 1, response: response, predictor: rhs, polyExplicit: false]
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
   *   - se: show confidence interval (default: true; set to false to hide)
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
      int formulaDegree = (formulaParsed.polyDegree as int)
      boolean formulaExplicit = (formulaParsed.polyExplicit as boolean)
      if (formulaExplicit && formulaDegree != explicitDegree) {
        throw new IllegalArgumentException(
          "Conflicting polynomial degrees: 'degree' parameter is " +
            explicitDegree + " but 'formula' specifies degree " + formulaDegree
        )
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

    double sigma2 = 0.0d
    int dfResid = xValues.size() - (polyDegree + 1)
    double sxx = 0.0d
    double xBar = 0.0d
    double[][] xtxInv = null
    if (se && dfResid > 0) {
      if (polyDegree == 1) {
        xBar = (xValues.sum() as double) / xValues.size()
        xValues.each { Number val ->
          double diff = (val as double) - xBar
          sxx += diff * diff
        }
      }

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
      } else if (polyDegree == 1) {
        if (sxx <= 0.0d) {
          se = false
        }
      } else {
        xtxInv = RegressionUtils.polynomialXtxInverse(xValues, polyDegree)
        if (xtxInv == null) {
          se = false
        }
      }
    } else {
      se = false
    }

    double tCrit = se ? tCritical(dfResid, level) : 0.0d

    List<List<?>> results = []
    for (int i = 0; i < nPoints; i++) {
      Number x = xMin + (xMax - xMin) * i / (nPoints - 1)
      BigDecimal yFit = regression.predict(x)
      if (se) {
        double seFit
        if (polyDegree == 1) {
          double dx = (x as double) - xBar
          seFit = Math.sqrt(sigma2 * (1.0d / xValues.size() + (dx * dx) / sxx))
        } else {
          double leverage = RegressionUtils.polynomialLeverage(
            xtxInv,
            (x as double),
            polyDegree
          )
          seFit = Math.sqrt(sigma2 * Math.max(0.0d, leverage))
        }
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
    String funData = params.'fun.data'
    if (funData == null && params.funData) {
      funData = params.funData as String
    }

    if (yCol == null) {
      throw new IllegalArgumentException("stat_summary requires y aesthetic")
    }

    if (funData) {
      switch (funData) {
        case 'mean_cl_normal':
          double level = params.level != null ? (params.level as double) : 0.95d
          return meanClNormal(data, xCol, yCol, level)
        default:
          throw new IllegalArgumentException("Unknown summary function: $funData")
      }
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

  private static Matrix meanClNormal(Matrix data, String xCol, String yCol, double level) {
    if (xCol == null) {
      return meanClNormalUngrouped(data, yCol, level)
    }

    Map<String, Matrix> groups = Stat.groupBy(data, xCol)
    List<Map<String, Object>> rows = []
    groups.each { groupKey, groupData ->
      List<Number> values = groupData[yCol] as List<Number>
      rows << meanClNormalRow(groupKey, values, xCol, yCol, level)
    }
    return Matrix.builder().mapList(rows).build()
  }

  private static Matrix meanClNormalUngrouped(Matrix data, String yCol, double level) {
    List<Number> values = data[yCol] as List<Number>
    Map<String, Object> row = meanClNormalRow('all', values, 'x', yCol, level)
    return Matrix.builder().mapList([row]).build()
  }

  private static Map<String, Object> meanClNormalRow(Object groupKey, List<Number> values,
                                                     String xCol, String yCol, double level) {
    List<Number> numeric = values.findAll { it instanceof Number } as List<Number>
    int n = numeric.size()
    if (n == 0) {
      return [(xCol): groupKey, (yCol): null, ymin: null, ymax: null]
    }
    BigDecimal mean = Stat.mean(numeric)
    double ymin = mean as double
    double ymax = mean as double
    if (n > 1) {
      double sd = Stat.sd(numeric, true) as double
      double se = sd / Math.sqrt(n as double)
      double tCrit = tCritical(n - 1, level)
      ymin = (mean as double) - tCrit * se
      ymax = (mean as double) + tCrit * se
    }
    return [
        (xCol): groupKey,
        (yCol): mean,
        ymin: ymin,
        ymax: ymax
    ]
  }

  /**
   * Kernel density estimation.
   * Delegates to KernelDensity from matrix-stats.
   *
   * <p>Equivalent to R's stat_density():</p>
   * <pre>
   * # R equivalent
   * ggplot(data, aes(x = value)) + stat_density(kernel = "gaussian")
   * </pre>
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for values, optionally color/fill/group for grouping)
   * @param params Map with optional:
   *   - kernel: kernel type ('gaussian', 'epanechnikov', 'uniform', 'triangular'), default 'gaussian'
   *   - bw: bandwidth (overrides automatic selection)
   *   - adjust: bandwidth adjustment factor (default 1.0)
   *   - n: number of evaluation points (default 512)
   *   - trim: trim density to data range (default false)
   *   - from: custom range start
   *   - to: custom range end
   * @return Matrix with columns: x, density (and group column if grouping is used)
   */
  static Matrix density(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_density requires x aesthetic")
    }

    // Determine grouping column
    String groupCol = aes.groupColName ?: aes.colorColName ?: aes.fillColName

    // Build KDE parameters
    Map<String, Object> kdeParams = [:]
    if (params.kernel != null) {
      kdeParams.kernel = params.kernel
    }
    if (params.bw != null) {
      kdeParams.bandwidth = params.bw
    }
    if (params.adjust != null) {
      kdeParams.adjust = params.adjust
    }
    if (params.n != null) {
      kdeParams.n = params.n
    }
    if (params.trim != null) {
      kdeParams.trim = params.trim
    }
    if (params.from != null) {
      kdeParams.from = params.from
    }
    if (params.to != null) {
      kdeParams.to = params.to
    }

    if (groupCol != null && data.columnNames().contains(groupCol)) {
      // Grouped density estimation
      return densityGrouped(data, xCol, groupCol, kdeParams)
    } else {
      // Ungrouped density estimation
      return densityUngrouped(data, xCol, kdeParams)
    }
  }

  /**
   * Compute density for ungrouped data.
   */
  private static Matrix densityUngrouped(Matrix data, String xCol, Map<String, Object> kdeParams) {
    List<Number> values = extractNumericValues(data, xCol)

    if (values.size() < 2) {
      return Matrix.builder()
          .columnNames(['x', 'density'])
          .rows([])
          .types(Double, Double)
          .build()
    }

    KernelDensity kde = new KernelDensity(values, kdeParams)
    return kde.toMatrix()
  }

  /**
   * Compute density for grouped data.
   */
  private static Matrix densityGrouped(Matrix data, String xCol, String groupCol, Map<String, Object> kdeParams) {
    // Group data by the grouping column
    Map<Object, List<Number>> groups = new LinkedHashMap<>()
    data.each { row ->
      Object groupKey = row[groupCol]
      def xVal = row[xCol]
      if (xVal instanceof Number) {
        if (!groups.containsKey(groupKey)) {
          groups[groupKey] = []
        }
        groups[groupKey] << (xVal as Number)
      }
    }

    if (groups.isEmpty()) {
      return Matrix.builder()
          .columnNames(['x', 'density', groupCol])
          .rows([])
          .build()
    }

    // Compute density for each group and combine results
    List<Map<String, Object>> allRows = []
    groups.each { groupKey, values ->
      if (values.size() >= 2) {
        KernelDensity kde = new KernelDensity(values, kdeParams)
        double[] xVals = kde.getX()
        double[] densityVals = kde.getDensity()

        for (int i = 0; i < xVals.length; i++) {
          allRows << [
              x: xVals[i],
              density: densityVals[i],
              (groupCol): groupKey
          ]
        }
      }
    }

    if (allRows.isEmpty()) {
      return Matrix.builder()
          .columnNames(['x', 'density', groupCol])
          .rows([])
          .build()
    }

    return Matrix.builder().mapList(allRows).build()
  }

  /**
   * Extract numeric values from a column, filtering out nulls and non-numeric values.
   */
  private static List<Number> extractNumericValues(Matrix data, String colName) {
    List<Number> result = []
    data.each { row ->
      def val = row[colName]
      if (val instanceof Number) {
        result << (val as Number)
      }
    }
    return result
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

  /**
   * Compute the minimum non-zero resolution in a numeric vector.
   * Mirrors ggplot2's resolution() behavior for continuous data.
   */
  private static Double resolution(List<Number> values) {
    if (values == null || values.isEmpty()) {
      return 1.0d
    }
    List<Double> uniques = values.findAll { it instanceof Number }
        .collect { (it as Number).doubleValue() }
        .unique()
        .sort()
    if (uniques.size() <= 1) {
      return 1.0d
    }
    double minDiff = Double.MAX_VALUE
    for (int i = 1; i < uniques.size(); i++) {
      double diff = uniques[i] - uniques[i - 1]
      if (diff > 0 && diff < minDiff) {
        minDiff = diff
      }
    }
    return minDiff == Double.MAX_VALUE ? 1.0d : minDiff
  }

  /**
   * Compute quantiles using the same method as ggplot2 (type = 7).
   * This is a linear interpolation method where the kth quantile is computed as:
   * (1-g)*x[j] + g*x[j+1], where j = floor((n-1)*p + 1) and g is the fractional part.
   *
   * @param sortedValues a list of numeric values that MUST be sorted in ascending order.
   *        The caller is responsible for sorting before calling this method.
   *        Passing unsorted values will produce incorrect results.
   * @param prob the probability for the quantile, between 0 and 1 (e.g., 0.25 for Q1, 0.5 for median)
   * @return the computed quantile value, or null if the input list is null or empty
   */
  private static Number quantileType7(List<Number> sortedValues, double prob) {
    if (sortedValues == null || sortedValues.isEmpty()) {
      return null
    }
    if (sortedValues.size() == 1) {
      return sortedValues[0]
    }
    int n = sortedValues.size()
    double h = (n - 1) * prob + 1
    int j = (int) Math.floor(h)
    double g = h - j

    if (j <= 1) {
      double x1 = sortedValues[0] as double
      double x2 = sortedValues[1] as double
      return x1 + g * (x2 - x1)
    }
    if (j >= n) {
      return sortedValues[n - 1]
    }

    double xj = sortedValues[j - 1] as double
    double xj1 = sortedValues[j] as double
    return xj + g * (xj1 - xj)
  }

}
