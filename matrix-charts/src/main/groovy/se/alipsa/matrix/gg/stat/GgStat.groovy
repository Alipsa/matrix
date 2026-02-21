package se.alipsa.matrix.gg.stat

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.core.Matrix
import se.alipsa.matrix.core.Row
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.gg.aes.Aes
import se.alipsa.matrix.gg.geom.Point
import se.alipsa.matrix.charm.render.scale.ScaleUtils
import se.alipsa.matrix.stats.distribution.TDistribution
import se.alipsa.matrix.stats.kde.KernelDensity
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.regression.PolynomialRegression
import se.alipsa.matrix.stats.regression.QuantileRegression
import se.alipsa.matrix.stats.regression.RegressionUtils
import se.alipsa.matrix.charm.sf.SfGeometry
import se.alipsa.matrix.charm.sf.SfGeometryUtils
import se.alipsa.matrix.charm.sf.SfPoint
import se.alipsa.matrix.charm.sf.WktReader

import java.math.RoundingMode
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Consolidated statistical transformation utilities for ggplot.
 * All methods are static and delegate to matrix-core Stat or matrix-stats where possible.
 */
@CompileStatic
class GgStat {

  private static final boolean VALIDATE_SORTED_QUANTILES =
      Boolean.getBoolean('matrix.gg.validateQuantiles')

  /**
   * Identity transformation - returns data unchanged.
   * Default stat for most geoms.
   */
  static Matrix identity(Matrix data, Aes aes) {
    return data
  }

  /**
   * Expand WKT geometries into x/y coordinate rows for spatial rendering.
   *
   * @param data Input matrix containing a geometry column
   * @param aes Aesthetic mappings (uses geometry if provided)
   * @param params Optional params (geometry column override)
   * @return Matrix with x/y coordinates and SF grouping metadata
   */
  static Matrix sf(Matrix data, Aes aes, Map params = [:]) {
    if (data == null || data.rowCount() == 0) {
      return Matrix.builder().rows([]).build()
    }

    String geometryCol = resolveGeometryColumn(data, aes, params)
    List<Map<String, Object>> rows = []

    data.eachWithIndex { Row row, int idx ->
      Object geomVal = row[geometryCol]
      if (geomVal == null) return

      SfGeometry geometry = toGeometry(geomVal)
      if (geometry == null || geometry.empty) return

      int partIndex = 0
      geometry.shapes.each { shape ->
        int ringIndex = 0
        shape.rings.each { ring ->
          String shapeType = (shape.type ?: geometry.type)?.toString()
          ring.points.each { point ->
            Map<String, Object> rowMap = new LinkedHashMap<>(row.toMap())
            rowMap.put('x', point.x)
            rowMap.put('y', point.y)
            rowMap.put('__sf_id', idx)
            rowMap.put('__sf_part', partIndex)
            rowMap.put('__sf_ring', ringIndex)
            rowMap.put('__sf_hole', ring.hole)
            rowMap.put('__sf_type', shapeType)
            rowMap.put('__sf_group', "${idx}:${partIndex}:${ringIndex}".toString())
            rows << rowMap
          }
          ringIndex++
        }
        partIndex++
      }
    }

    return Matrix.builder().mapList(rows).build()
  }

  /**
   * Compute representative points from WKT geometries for labeling.
   *
   * @param data Input matrix containing a geometry column
   * @param aes Aesthetic mappings (uses geometry if provided)
   * @param params Optional params (geometry column override)
   * @return Matrix with x/y coordinates and SF metadata
   */
  static Matrix sfCoordinates(Matrix data, Aes aes, Map params = [:]) {
    if (data == null || data.rowCount() == 0) {
      return Matrix.builder().rows([]).build()
    }

    String geometryCol = resolveGeometryColumn(data, aes, params)
    List<Map<String, Object>> rows = []

    data.eachWithIndex { Row row, int idx ->
      Object geomVal = row[geometryCol]
      if (geomVal == null) return

      SfGeometry geometry = toGeometry(geomVal)
      if (geometry == null || geometry.empty) return

      SfPoint point = SfGeometryUtils.representativePoint(geometry)
      if (point == null) return

      Map<String, Object> rowMap = new LinkedHashMap<>(row.toMap())
      rowMap.put('x', point.x)
      rowMap.put('y', point.y)
      rowMap.put('__sf_id', idx)
      rowMap.put('__sf_type', geometry.type.toString())
      rows << rowMap
    }

    return Matrix.builder().mapList(rows).build()
  }

  private static String resolveGeometryColumn(Matrix data, Aes aes, Map params) {
    String col = aes?.geometryColName
    if (col != null) {
      requireColumn(data, col, 'aes(geometry: ...)')
      return col
    }

    Object paramGeom = params?.geometry
    if (paramGeom != null) {
      col = paramGeom.toString()
      requireColumn(data, col, 'geom_sf(geometry: ...)')
      return col
    }

    Object metaGeom = data.metaData?.get('geometryColumn')
    if (metaGeom != null) {
      col = metaGeom.toString()
      requireColumn(data, col, 'metaData.geometryColumn')
      return col
    }

    col = 'geometry'
    requireColumn(data, col, 'default geometry column')
    return col
  }

  private static void requireColumn(Matrix data, String col, String source) {
    if (!data.columnNames().contains(col)) {
      throw new IllegalArgumentException("Geometry column '$col' from $source not found in data")
    }
  }

  private static SfGeometry toGeometry(Object value) {
    if (value instanceof SfGeometry) {
      return (SfGeometry) value
    }
    return WktReader.parse(value.toString())
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
      // Group by [xVal, fillVal] and count
      Map<List<Object>, List> groups = data.rows()
          .groupBy { Row row -> [row[xCol], row[fillCol]] } as Map<List<Object>, List>

      // Calculate totals by x for percentage calculation
      Map<Object, Integer> totalsByX = data.rows()
          .groupBy { row -> row[xCol] }
          .collectEntries { xVal, xRows -> [(xVal): xRows.size()] } as Map<Object, Integer>

      // Build result rows
      List<Map> rows = groups.collect { key, groupRows ->
        int count = groupRows.size()
        int totalForX = totalsByX[key[0]] ?: 0
        BigDecimal percent = totalForX == 0 ? BigDecimal.ZERO : (count * 100.0 / totalForX).setScale(2, RoundingMode.HALF_EVEN)
        [
            (xCol): key[0],
            (fillCol): key[1],
            count: count,
            percent: percent
        ] as Map<String, Object>
      }
      return Matrix.builder().mapList(rows as List<Map<String, Object>>).build()
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
    BigDecimal minVal = Stat.min(values) as BigDecimal
    BigDecimal maxVal = Stat.max(values) as BigDecimal
    BigDecimal range = maxVal - minVal

    // Handle edge case: all values are the same
    if (range == 0) {
      // Single bin containing all values
      return Matrix.builder()
          .columnNames(['x', 'xmin', 'xmax', 'count', 'density'])
          .rows([[minVal, minVal, minVal, values.size(), 1.0]])
          .types([BigDecimal, BigDecimal, BigDecimal, Integer, BigDecimal])
          .build()
    }

    BigDecimal binwidthBD
    if (binwidth == null) {
      binwidthBD = range / bins
    } else {
      binwidthBD = binwidth as BigDecimal
      bins = (range / binwidthBD).ceil().intValue()
      if (bins == 0) bins = 1
    }

    // Create bin counts
    List<Map> results = []
    for (int i = 0; i < bins; i++) {
      BigDecimal xmin = minVal + i * binwidthBD
      BigDecimal xmax = minVal + (i + 1) * binwidthBD
      BigDecimal center = (xmin + xmax) / 2

      int count = values.count { Number v ->
        BigDecimal vBD = v as BigDecimal
        vBD >= xmin && (i == bins - 1 ? vBD <= xmax : vBD < xmax)
      } as int

      BigDecimal density = count / (values.size() * binwidthBD)

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
   * Uses quantile Type 7 (linear interpolation) to compute quartiles, matching ggplot2's behavior.
   * IQR is computed as upper quartile - lower quartile using the same quantile method.
   *
   * When a group aesthetic is specified separately from x:
   * - Groups data by the group column for computing boxplot statistics
   * - Computes mean x value for each group for positioning (ggplot2 behavior)
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

    BigDecimal xResolution = null
    if (hasContinuousX) {
      List<Number> xValues = (data[xCol] as List).findAll { it instanceof Number } as List<Number>
      xResolution = resolution(xValues) as BigDecimal
    }
    BigDecimal defaultWidth = params?.width instanceof Number ?
        params.width as BigDecimal :
        ((xResolution ?: 1.0) * 0.75)
    BigDecimal coef = params?.coef instanceof Number ? params.coef as BigDecimal : 1.5

    List<Map> results = groups.collect { groupKey, groupData ->
      List<Number> values = (groupData[yCol] as List).findAll { it instanceof Number } as List<Number>
      if (values.isEmpty()) {
        return null
      }
      values.sort()

      BigDecimal lower = quantileType7(values, 0.25) as BigDecimal
      BigDecimal middle = quantileType7(values, 0.5) as BigDecimal
      BigDecimal upper = quantileType7(values, 0.75) as BigDecimal
      BigDecimal iqr = upper - lower

      BigDecimal lowerFence = lower - coef * iqr
      BigDecimal upperFence = upper + coef * iqr

      List<Number> outliers = values.findAll { Number v ->
        BigDecimal vBD = v as BigDecimal
        vBD < lowerFence || vBD > upperFence
      }
      List<Number> inliers = values.findAll { Number v ->
        BigDecimal vBD = v as BigDecimal
        vBD >= lowerFence && vBD <= upperFence
      }
      Number whiskerLow = inliers.isEmpty() ? values.first() : inliers.min()
      Number whiskerHigh = inliers.isEmpty() ? values.last() : inliers.max()

      // Compute x position: if we have explicit group and continuous x, use center of x range
      // ggplot2 positions boxes at the center (mean of min and max) of the x range within each group
      def xPosition
      BigDecimal xMin = null
      BigDecimal xMax = null
      if (hasExplicitGroup && hasContinuousX) {
        List<Number> xValues = (groupData[xCol] as List).findAll { it instanceof Number } as List<Number>
        if (!xValues.isEmpty()) {
          xValues.sort()
          xMin = xValues.first() as BigDecimal
          xMax = xValues.last() as BigDecimal
          // Use center of the x range for positioning
          xPosition = (xMin + xMax) / 2
        }
      } else {
        xPosition = groupKey
      }

      BigDecimal widthValue = defaultWidth
      if (groupData.columnNames().contains('width')) {
        def widthColVal = (groupData['width'] as List)?.find { it instanceof Number }
        if (widthColVal instanceof Number) {
          widthValue = widthColVal as BigDecimal
        }
      } else if (hasContinuousX && xMin != null && xMax != null) {
        BigDecimal range = xMax - xMin
        if (range > 0) {
          widthValue = range * 0.9
        }
      }
      BigDecimal xmin = null
      BigDecimal xmax = null
      if (xPosition instanceof Number && widthValue > 0) {
        BigDecimal half = widthValue / 2
        xmin = (xPosition as BigDecimal) - half
        xmax = (xPosition as BigDecimal) + half
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
      return [polyDegree: degree, response: response, predictor: predictor, polyExplicit: true] as Map<String, Object>
    }

    // Simple linear: y ~ x
    return [polyDegree: 1, response: response, predictor: rhs, polyExplicit: false] as Map<String, Object>
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
    BigDecimal level = params.level != null ? (params.level as BigDecimal) : 0.95

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
    int maxIdx = (rawX?.size() ?: 0).min(rawY?.size() ?: 0)
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

    BigDecimal sigma2 = 0.0
    int dfResid = xValues.size() - (polyDegree + 1)
    BigDecimal sxx = 0.0
    BigDecimal xBar = 0.0
    BigDecimal[][] xtxInv = null
    if (se && dfResid > 0) {
      if (polyDegree == 1) {
        xBar = (xValues.sum() as BigDecimal) / xValues.size()
        xValues.each { Number val ->
          BigDecimal diff = val - xBar
          sxx += diff * diff
        }
      }

      BigDecimal sse = 0.0
      for (int i = 0; i < xValues.size(); i++) {
        BigDecimal xi = xValues[i] as BigDecimal
        BigDecimal yi = yValues[i] as BigDecimal
        BigDecimal yFit = regression.predict(xi) as BigDecimal
        BigDecimal resid = yi - yFit
        sse += resid * resid
      }
      sigma2 = sse / dfResid
      if (sigma2 <= 0.0) {
        se = false
      } else if (polyDegree == 1) {
        if (sxx <= 0.0) {
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

    BigDecimal tCrit = se ? tCritical(dfResid, level) : 0.0

    List<List<?>> results = []
    for (int i = 0; i < nPoints; i++) {
      Number x = xMin + (xMax - xMin) * i / (nPoints - 1)
      BigDecimal yFit = regression.predict(x)
      if (se) {
        BigDecimal seFit
        if (polyDegree == 1) {
          BigDecimal dx = (x as BigDecimal) - xBar
          seFit = (sigma2 * (1.0 / xValues.size() + (dx * dx) / sxx)).sqrt()
        } else {
          BigDecimal leverage = RegressionUtils.polynomialLeverage(
            xtxInv,
            x as BigDecimal,
            polyDegree
          )
          seFit = (sigma2 * 0.0.max(leverage)).sqrt()
        }
        BigDecimal margin = tCrit * seFit
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

  /**
   * Compute quantile regression lines.
   *
   * Quantile regression estimates conditional quantiles rather than the conditional mean.
   * This method fits separate regression lines for each specified quantile.
   *
   * The computation is delegated to QuantileRegression class in matrix-stats, which uses
   * the Barrodale-Roberts simplex algorithm to solve the linear programming formulation.
   *
   * @param data Input matrix with data
   * @param aes Aesthetic mappings (uses x and y)
   * @param params Map with optional parameters:
   *   - quantiles: List of quantiles to fit, e.g. [0.25, 0.5, 0.75] (default)
   *   - n: Number of fitted points to generate (default: 80)
   * @return Matrix with columns:
   *   - x: Fitted x values (BigDecimal)
   *   - y: Fitted y values (BigDecimal)
   *   - quantile: The tau value for this line (BigDecimal)
   *   - group: Integer group index (0, 1, 2... for each quantile)
   */
  @CompileDynamic
  static Matrix quantile(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName

    if (!xCol || !yCol) {
      throw new IllegalArgumentException("stat_quantile requires x and y aesthetics")
    }

    // Extract quantiles parameter (default: [0.25, 0.5, 0.75])
    List<Number> quantiles = params.quantiles as List<Number> ?: [0.25, 0.5, 0.75]

    // Validate quantiles in (0, 1)
    for (Number tau : quantiles) {
      if (tau <= 0 || tau >= 1) {
        throw new IllegalArgumentException("Quantiles must be in (0, 1), got: ${tau}")
      }
    }

    // Check if data is empty
    if (data == null || data.rowCount() == 0) {
      return Matrix.builder()
          .columnNames(['x', 'y', 'quantile', 'group'])
          .types([BigDecimal, BigDecimal, BigDecimal, Integer])
          .build()
    }

    // Extract numeric x,y values, filtering out any null or non-numeric entries
    // while keeping x and y synchronized (same indices after filtering)
    List<Number> xValues = []
    List<Number> yValues = []
    List<?> rawX = data[xCol] as List<?>
    List<?> rawY = data[yCol] as List<?>
    int maxIdx = Math.min(rawX?.size() ?: 0, rawY?.size() ?: 0)
    for (int i = 0; i < maxIdx; i++) {
      def xVal = rawX[i]
      def yVal = rawY[i]
      // Filter pairs where both values are numeric (handles nulls and type mismatches)
      if (xVal instanceof Number && yVal instanceof Number) {
        xValues << (xVal as Number)
        yValues << (yVal as Number)
      }
    }

    if (xValues.size() < 2) {
      return Matrix.builder()
          .columnNames(['x', 'y', 'quantile', 'group'])
          .types([BigDecimal, BigDecimal, BigDecimal, Integer])
          .build()
    }

    // Generate fitted values at n points
    Number xMin = Stat.min(xValues)
    Number xMax = Stat.max(xValues)
    int nPoints = params.n != null ? (params.n as int) : 80

    // Validate nPoints to prevent division by zero
    if (nPoints < 2) {
      throw new IllegalArgumentException(
        "Parameter 'n' must be at least 2 to generate a line. Got n=${nPoints}."
      )
    }

    // Build results for all quantiles
    List<List<?>> results = []

    quantiles.eachWithIndex { Number tau, int groupIdx ->
      // Create regression instance (matrix-stats class - does all the math)
      QuantileRegression qr = new QuantileRegression(xValues, yValues, tau)

      // Generate fitted points using the regression (just calls predict)
      for (int i = 0; i < nPoints; i++) {
        BigDecimal x = xMin + (xMax - xMin) * i / (nPoints - 1)
        BigDecimal yFit = qr.predict(x)  // QuantileRegression does the work
        results << [x, yFit, tau as BigDecimal, groupIdx]
      }
    }

    return Matrix.builder()
        .columnNames(['x', 'y', 'quantile', 'group'])
        .rows(results)
        .types([BigDecimal, BigDecimal, BigDecimal, Integer])
        .build()
  }

  private static BigDecimal tCritical(int df, BigDecimal level) {
    if (df <= 0) return 0.0
    BigDecimal target = 0.5 + level / 2.0
    TDistribution dist = new TDistribution(df as double)
    BigDecimal low = 0.0
    BigDecimal high = 1.0
    while (dist.cdf(high) < target && high < 1.0e6) {
      high *= 2.0
    }
    for (int i = 0; i < 80; i++) {
      BigDecimal mid = (low + high) / 2.0
      if (dist.cdf(mid) < target) {
        low = mid
      } else {
        high = mid
      }
    }
    return (low + high) / 2.0
  }

  /**
   * Compute summary statistics by group.
   * Delegates to Stat.meanBy(), Stat.medianBy(), etc.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings
   * @param params Map with: fun ('mean', 'median', 'sum', 'min', 'max'),
   *   fun.data ('mean_cl_normal', 'median_hilow'), fun.args (e.g. [conf.int: 0.5])
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
    Map funArgs = [:]
    if (params.'fun.args' instanceof Map) {
      funArgs = params.'fun.args' as Map
    } else if (params.funArgs instanceof Map) {
      funArgs = params.funArgs as Map
    }

    if (yCol == null) {
      throw new IllegalArgumentException("stat_summary requires y aesthetic")
    }

    if (funData) {
      switch (funData) {
        case 'mean_cl_normal':
          BigDecimal level = params.level != null ? (params.level as BigDecimal) : 0.95
          return meanClNormal(data, xCol, yCol, level)
        case 'median_hilow':
          BigDecimal confInt = 0.95
          if (funArgs != null) {
            if (funArgs.containsKey('conf.int')) {
              confInt = funArgs.'conf.int' as BigDecimal
            } else if (funArgs.containsKey('confInt')) {
              confInt = funArgs.confInt as BigDecimal
            }
          }
          if (confInt == null || confInt <= 0.0 || confInt > 1.0) {
            throw new IllegalArgumentException("median_hilow requires conf.int in (0, 1]")
          }
          return medianHiLow(data, xCol, yCol, confInt)
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

  private static Matrix meanClNormal(Matrix data, String xCol, String yCol, BigDecimal level) {
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

  private static Matrix meanClNormalUngrouped(Matrix data, String yCol, BigDecimal level) {
    List<Number> values = data[yCol] as List<Number>
    Map<String, Object> row = meanClNormalRow('all', values, 'x', yCol, level)
    return Matrix.builder().mapList([row]).build()
  }

  private static Map<String, Object> meanClNormalRow(Object groupKey, List<Number> values,
                                                     String xCol, String yCol, BigDecimal level) {
    List<Number> numeric = values.findAll { it instanceof Number } as List<Number>
    int n = numeric.size()
    if (n == 0) {
      return [(xCol): groupKey, (yCol): null, ymin: null, ymax: null]
    }
    BigDecimal mean = Stat.mean(numeric)
    BigDecimal ymin = mean
    BigDecimal ymax = mean
    if (n > 1) {
      BigDecimal sd = Stat.sd(numeric, true)
      BigDecimal se = sd / n.sqrt()
      BigDecimal tCrit = tCritical(n - 1, level)
      ymin = mean - tCrit * se
      ymax = mean + tCrit * se
    }
    return [
        (xCol): groupKey,
        (yCol): mean,
        ymin: ymin,
        ymax: ymax
    ]
  }

  private static Matrix medianHiLow(Matrix data, String xCol, String yCol, BigDecimal confInt) {
    if (xCol == null) {
      return medianHiLowUngrouped(data, yCol, confInt)
    }

    Map<String, Matrix> groups = Stat.groupBy(data, xCol)
    List<Map<String, Object>> rows = []
    groups.each { groupKey, groupData ->
      List<Number> values = groupData[yCol] as List<Number>
      rows << medianHiLowRow(groupKey, values, xCol, yCol, confInt)
    }
    return Matrix.builder().mapList(rows).build()
  }

  private static Matrix medianHiLowUngrouped(Matrix data, String yCol, BigDecimal confInt) {
    List<Number> values = data[yCol] as List<Number>
    Map<String, Object> row = medianHiLowRow('all', values, 'x', yCol, confInt)
    return Matrix.builder().mapList([row]).build()
  }

  private static Map<String, Object> medianHiLowRow(Object groupKey, List<Number> values,
                                                    String xCol, String yCol, BigDecimal confInt) {
    List<Number> numeric = values.findAll { it instanceof Number } as List<Number>
    if (numeric.isEmpty()) {
      return [(xCol): groupKey, (yCol): null, ymin: null, ymax: null]
    }
    numeric.sort()

    BigDecimal alpha = (1.0 - confInt) / 2.0
    //BigDecimal lowerProb = Math.max(0.0d, Math.min(1.0d, alpha.doubleValue()))
    BigDecimal lowerProb = 0.0.max(1.0.min(alpha))
    //BigDecimal upperProb = Math.max(0.0d, Math.min(1.0d, 1.0d - alpha))
    BigDecimal upperProb = 0.0.max(1.0.min(1.0 - alpha))

    Number median = quantileType7(numeric, 0.5)
    Number lower = quantileType7(numeric, lowerProb)
    Number upper = quantileType7(numeric, upperProb)
    return [
        (xCol): groupKey,
        (yCol): median,
        ymin: lower,
        ymax: upper
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
   * Kernel density estimation using y values (stat_ydensity).
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses y for values, optionally group for grouping)
   * @param params Map with optional KDE parameters (see density())
   * @return Matrix with columns: y, density (and group column if grouping is used)
   */
  static Matrix ydensity(Matrix data, Aes aes, Map params = [:]) {
    String yCol = aes.yColName
    if (yCol == null) {
      throw new IllegalArgumentException("stat_ydensity requires y aesthetic")
    }

    String groupCol = aes.groupColName ?: aes.colorColName ?: aes.fillColName
    Aes yAes = new Aes([x: yCol, group: groupCol])
    Matrix result = density(data, yAes, params)
    if (result.columnNames().contains('x')) {
      return result.rename('x', 'y')
    }
    return result
  }

  /**
   * Empirical cumulative distribution function.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for values, optional group)
   * @param params Map with optional: pad (boolean, default false)
   * @return Matrix with columns: x, y (and group column if grouping is used)
   */
  static Matrix ecdf(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_ecdf requires x aesthetic")
    }

    String groupCol = aes.groupColName ?: aes.colorColName ?: aes.fillColName
    boolean pad = params.pad == true

    if (groupCol != null && data.columnNames().contains(groupCol)) {
      Map<Object, List<Number>> groups = data.rows()
          .findAll { row -> row[xCol] instanceof Number }
          .groupBy { row -> row[groupCol] }
          .collectEntries { key, groupRows ->
            [(key): groupRows.collect { row -> row[xCol] as Number }]
          } as Map<Object, List<Number>>

      List<Map<String, Object>> rows = groups.collectMany { key, values ->
        ecdfRows(values, 'x', groupCol, key, pad)
      }
      return Matrix.builder().mapList(rows).build()
    }

    List<Number> values = extractNumericValues(data, xCol)
    List<Map<String, Object>> rows = ecdfRows(values, 'x', null, null, pad)
    return Matrix.builder().mapList(rows).build()
  }

  private static List<Map<String, Object>> ecdfRows(List<Number> values, String xName,
                                                    String groupCol, Object groupKey, boolean pad) {
    List<Number> sorted = (values ?: []).findAll { it instanceof Number }.sort(false) as List<Number>
    if (sorted.isEmpty()) {
      return []
    }

    BigDecimal n = sorted.size()
    List<Map<String, Object>> rows = []
    if (pad) {
      rows << ecdfRow(xName, sorted.first(), 0.0, groupCol, groupKey)
    }
    rows.addAll(sorted.withIndex(1).collect { Number value, int idx ->
      ecdfRow(xName, value, (idx as BigDecimal) / n, groupCol, groupKey)
    })
    rows
  }

  private static Map<String, Object> ecdfRow(String xName, Number xValue, BigDecimal yValue,
                                              String groupCol, Object groupKey) {
    Map<String, Object> row = [(xName): xValue, y: yValue] as Map<String, Object>
    if (groupCol) {
      row[groupCol] = groupKey
    }
    row
  }

  /**
   * Q-Q plot statistics for normal distribution.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for sample values, optional group)
   * @param params Map with optional: distribution ('norm' only), sample (unused)
   * @return Matrix with columns: x (theoretical), y (sample), and group if provided
   */
  static Matrix qq(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_qq requires x aesthetic")
    }

    String groupCol = aes.groupColName ?: aes.colorColName ?: aes.fillColName
    if (groupCol != null && data.columnNames().contains(groupCol)) {
      Map<Object, List<Number>> groups = data.rows()
          .findAll { row -> row[xCol] instanceof Number }
          .groupBy { row -> row[groupCol] }
          .collectEntries { key, groupRows ->
            [(key): groupRows.collect { row -> row[xCol] as Number }]
          } as Map<Object, List<Number>>

      List<Map<String, Object>> rows = groups.collectMany { key, values ->
        qqRows(values, groupCol, key)
      }
      return Matrix.builder().mapList(rows).build()
    }

    List<Number> values = extractNumericValues(data, xCol)
    return Matrix.builder().mapList(qqRows(values, null, null)).build()
  }

  private static List<Map<String, Object>> qqRows(List<Number> values, String groupCol, Object groupKey) {
    if (values == null || values.isEmpty()) {
      return []
    }
    List<BigDecimal> sorted = values.findAll { it instanceof Number }
        .collect { it as BigDecimal }
        .sort()
    int n = sorted.size()
    List<Map<String, Object>> rows = []
    for (int i = 0; i < n; i++) {
      BigDecimal p = (i + 0.5) / n
      BigDecimal theoretical = normalQuantile(p)
      Map<String, Object> row = [x: theoretical, y: sorted[i]] as Map<String, Object>
      if (groupCol) {
        row[groupCol] = groupKey
      }
      rows << row
    }
    return rows
  }

  /**
   * Q-Q line statistics for normal distribution.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x for sample values, optional group)
   * @param params Map with optional: distribution ('norm' only)
   * @return Matrix with columns: x, y (line endpoints), and group if provided
   */
  static Matrix qqLine(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    if (xCol == null) {
      throw new IllegalArgumentException("stat_qq_line requires x aesthetic")
    }

    String groupCol = aes.groupColName ?: aes.colorColName ?: aes.fillColName
    if (groupCol != null && data.columnNames().contains(groupCol)) {
      Map<Object, List<Number>> groups = new LinkedHashMap<>()
      data.each { row ->
        def value = row[xCol]
        if (value instanceof Number) {
          Object key = row[groupCol]
          if (!groups.containsKey(key)) {
            groups[key] = []
          }
          groups[key] << (value as Number)
        }
      }
      List<Map<String, Object>> rows = []
      groups.each { key, values ->
        rows.addAll(qqLineRows(values, groupCol, key))
      }
      return Matrix.builder().mapList(rows).build()
    }

    List<Number> values = extractNumericValues(data, xCol)
    return Matrix.builder().mapList(qqLineRows(values, null, null)).build()
  }

  private static List<Map<String, Object>> qqLineRows(List<Number> values, String groupCol, Object groupKey) {
    if (values == null || values.isEmpty()) {
      return []
    }
    List<Number> numeric = values.findAll { it instanceof Number } as List<Number>
    if (numeric.size() < 2) {
      return []
    }
    numeric.sort()
    int n = numeric.size()
    BigDecimal q1 = quantileType7(numeric, 0.25)
    BigDecimal q3 = quantileType7(numeric, 0.75)
    BigDecimal theoQ1 = normalQuantile(0.25)
    BigDecimal theoQ3 = normalQuantile(0.75)
    BigDecimal slope = (q3 - q1) / (theoQ3 - theoQ1)
    BigDecimal intercept = q1 - slope * theoQ1

    // Clamp probabilities to prevent infinite values from normalQuantile
    BigDecimal minTheo = safeNormalQuantile(0.5 / n)
    BigDecimal maxTheo = safeNormalQuantile((n - 0.5) / n)

    Map<String, Object> start = [x: minTheo, y: slope * minTheo + intercept] as Map<String, Object>
    Map<String, Object> end = [x: maxTheo, y: slope * maxTheo + intercept] as Map<String, Object>
    if (groupCol) {
      start[groupCol] = groupKey
      end[groupCol] = groupKey
    }
    return [start, end]
  }

  /**
   * Safe wrapper for normalQuantile that clamps probability values to prevent
   * infinite return values from propagating to rendering code.
   * <p>
   * Clamps p to the range [QUANTILE_EPSILON, 1-QUANTILE_EPSILON] to ensure
   * finite results that won't cause issues in coordinate transformations.
   *
   * @param p probability value
   * @return finite quantile value
   */
  private static BigDecimal safeNormalQuantile(BigDecimal p) {
    // Minimum probability to avoid extreme quantile values
    // 1e-10 corresponds to approximately -6.4 standard deviations
    final BigDecimal QUANTILE_EPSILON = 1.0e-10
    BigDecimal clampedP = QUANTILE_EPSILON.max((1.0 - QUANTILE_EPSILON).min(p))
    return normalQuantile(clampedP)
  }

  /**
   * Approximate inverse CDF for the standard normal distribution.
   * Uses the Acklam rational approximation for good accuracy in the tails.
   * <p>
   * @param p probability value, should be in range (0, 1) for finite results
   * @return the quantile value
   */
  private static BigDecimal normalQuantile(BigDecimal p) {
    if (p == null || p <= 0.0 || p >= 1.0) {
      throw new IllegalArgumentException("normalQuantile requires p in (0, 1), got: $p")
    }

    // Coefficients in rational approximations
    BigDecimal[] a = [
        -3.969683028665376e+01,
        2.209460984245205e+02,
        -2.759285104469687e+02,
        1.383577518672690e+02,
        -3.066479806614716e+01,
        2.506628277459239e+00
    ] as BigDecimal[]
    BigDecimal[] b = [
        -5.447609879822406e+01,
        1.615858368580409e+02,
        -1.556989798598866e+02,
        6.680131188771972e+01,
        -1.328068155288572e+01
    ] as BigDecimal[]
    BigDecimal[] c = [
        -7.784894002430293e-03,
        -3.223964580411365e-01,
        -2.400758277161838e+00,
        -2.549732539343734e+00,
        4.374664141464968e+00,
        2.938163982698783e+00
    ] as BigDecimal[]
    BigDecimal[] d = [
        7.784695709041462e-03,
        3.224671290700398e-01,
        2.445134137142996e+00,
        3.754408661907416e+00
    ] as BigDecimal[]

    BigDecimal pLow = 0.02425
    BigDecimal pHigh = 1.0 - pLow
    BigDecimal q
    BigDecimal r

    if (p < pLow) {
      q =(-2.0 * p.log()).sqrt()
      return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0d)
    }
    if (p > pHigh) {
      q = (-2.0 * (1.0d - p).log()).sqrt()
      return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
          ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0d)
    }

    q = p - 0.5
    r = q * q
    return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
        (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0)
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
        .collect { it as double }
        .unique()
        .sort()
    if (uniques.size() <= 1) {
      return 1.0d
    }
    Double minDiff = null
    for (int i = 1; i < uniques.size(); i++) {
      double diff = uniques[i] - uniques[i - 1]
      if (diff > 0 && (minDiff == null || diff < minDiff)) {
        minDiff = diff
      }
    }
    return minDiff ?: 1.0d
  }

  /**
   * Compute quantiles using the same method as ggplot2 (type = 7).
   * This is a linear interpolation method where the kth quantile is computed as:
   * (1-g)*x[j] + g*x[j+1], where j = floor((n-1)*p + 1) and g is the fractional part.
   *
   * @param sortedValues a list of numeric values that MUST be sorted in ascending order.
   *        The caller is responsible for sorting before calling this method.
   *        Passing unsorted values will produce incorrect results.
   *        Set system property matrix.gg.validateQuantiles=true to enable a sortedness check.
   * @param prob the probability for the quantile, between 0 and 1 (e.g., 0.25 for Q1, 0.5 for median)
   * @return the computed quantile value, or null if the input list is null or empty
   */
  private static Number quantileType7(List<Number> sortedValues, BigDecimal prob) {
    if (sortedValues == null || sortedValues.isEmpty()) {
      return null
    }
    if (sortedValues.size() == 1) {
      return sortedValues[0]
    }
    if (VALIDATE_SORTED_QUANTILES && !isSortedAscending(sortedValues)) {
      throw new IllegalArgumentException('quantileType7 requires sortedValues in ascending order')
    }
    int n = sortedValues.size()
    BigDecimal h = (n - 1) * prob + 1
    int j = (int) h.floor()
    BigDecimal g = h - j

    if (j <= 1) {
      Number x1 = sortedValues[0]
      Number x2 = sortedValues[1]
      return x1 + g * (x2 - x1)
    }
    if (j >= n) {
      return sortedValues[n - 1]
    }

    Number xj = sortedValues[j - 1]
    Number xj1 = sortedValues[j]
    return xj + g * (xj1 - xj)
  }

  private static boolean isSortedAscending(List<Number> values) {
    if (values == null || values.size() < 2) {
      return true
    }
    double prev = values[0] as double
    for (int i = 1; i < values.size(); i++) {
      double current = values[i] as double
      if (current < prev) {
        return false
      }
      prev = current
    }
    return true
  }

  /**
   * Remove duplicate observations.
   * Keeps only unique rows based on aesthetics.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (uses x, y, or all columns if none specified)
   * @param params Map with optional columns to consider for uniqueness
   * @return Matrix with duplicate rows removed
   */
  static Matrix unique(Matrix data, Aes aes, Map params = [:]) {
    if (data == null || data.rowCount() == 0) return data

    // Determine which columns to use for uniqueness
    List<String> columns = []
    if (params.columns) {
      columns = params.columns as List<String>
    } else {
      // Use aesthetic columns if available
      if (aes.xColName) columns << aes.xColName
      if (aes.yColName) columns << aes.yColName
      if (columns.isEmpty()) {
        // Use all columns if no aesthetics specified
        columns = data.columnNames()
      }
    }

    // Track seen combinations
    Set<List<Object>> seenKeys = new LinkedHashSet<>()
    List<Map<String, Object>> uniqueRows = []

    data.each { row ->
      // Create key from relevant columns using the raw values to preserve null vs "null"
      List<Object> key = columns.collect { row[it] } as List<Object>

      if (!seenKeys.contains(key)) {
        seenKeys.add(key)
        Map<String, Object> rowMap = [:]
        data.columnNames().each { col ->
          rowMap[col] = row[col]
        }
        uniqueRows << rowMap
      }
    }

    return Matrix.builder().mapList(uniqueRows).build()
  }

  /**
   * Compute y values from function of x.
   * Evaluates a mathematical function over a range of x values.
   *
   * @param data Input matrix (can be null/empty - function generates its own x values)
   * @param aes Aesthetic mappings (x column used to determine range if provided)
   * @param params Map with:
   *   - fun: Closure that takes Number and returns Number (required)
   *   - xlim: [min, max] range for x (optional, auto-detected from data if not provided)
   *   - n: number of points to evaluate (default 101)
   * @return Matrix with columns: x, y
   */
  static Matrix function(Matrix data, Aes aes, Map params = [:]) {
    Closure<Number> fun = params.fun as Closure<Number>
    if (fun == null) {
      throw new IllegalArgumentException("stat_function requires 'fun' parameter (Closure)")
    }

    // Determine x range
    BigDecimal xmin, xmax
    if (params.xlim) {
      List xlim = params.xlim as List
      xmin = xlim[0] as BigDecimal
      xmax = xlim[1] as BigDecimal
    } else if (data != null && data.rowCount() > 0 && aes.xColName) {
      // Get range from data
      List<Number> xValues = data[aes.xColName].findAll { it instanceof Number } as List<Number>
      if (!xValues.isEmpty()) {
        xmin = xValues.min() as BigDecimal
        xmax = xValues.max() as BigDecimal
      } else {
        xmin = 0
        xmax = 1
      }
    } else {
      // Default range
      xmin = params.xmin as BigDecimal ?: 0
      xmax = params.xmax as BigDecimal ?: 1
    }

    int n = params.n as Integer ?: 101
    if (n < 1) {
      throw new IllegalArgumentException("stat_function parameter 'n' must be at least 1 but was " + n)
    }
    if (n == 1) {
      BigDecimal x = xmin
      BigDecimal y = fun.call(x) as BigDecimal
      return Matrix.builder()
          .data([x: [x], y: [y]])
          .build()
    }
    BigDecimal step = (xmax - xmin) / (n - 1)

    // Evaluate function at n points
    List<BigDecimal> xVals = []
    List<BigDecimal> yVals = []

    for (int i = 0; i < n; i++) {
      BigDecimal x = xmin + i * step
      BigDecimal y
      try {
        def result = fun.call(x)
        if (!(result instanceof Number)) {
          throw new IllegalArgumentException(
              "stat_function 'fun' must return a Number but returned " +
              "${result == null ? 'null' : result.getClass().name} for x=" + x
          )
        }
        y = (result as BigDecimal)
      } catch (Exception e) {
        if (e instanceof IllegalArgumentException) {
          throw e
        }
        throw new IllegalArgumentException("Error evaluating stat_function at x=" + x + ": " + e.message, e)
      }
      xVals << x
      yVals << y
    }

    return Matrix.builder()
        .data([x: xVals, y: yVals])
        .build()
  }

  /**
   * Binned summary statistics.
   * Bins x values and computes summary statistics for y in each bin.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (requires x and y)
   * @param params Map with:
   *   - bins: number of bins (default 30)
   *   - binwidth: width of bins (overrides bins)
   *   - fun: summary function - 'mean', 'median', 'sum', 'min', 'max' (default 'mean')
   *   - fun.data: Closure that takes List<Number> and returns Map<String, Number> for custom summaries
   * @return Matrix with columns: x (bin center), xmin, xmax, y (summary), n (count)
   */
  static Matrix summaryBin(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_summary_bin requires x and y aesthetics")
    }

    // Get numeric x and y values
    List<Map<String, Number>> points = []
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal instanceof Number && yVal instanceof Number) {
        points << [x: xVal as Number, y: yVal as Number]
      }
    }

    if (points.isEmpty()) {
      return Matrix.builder().data([x: [], xmin: [], xmax: [], y: [], n: []]).build()
    }

    // Calculate bins
    BigDecimal xMin = points.collect { it.x as BigDecimal }.min()
    BigDecimal xMax = points.collect { it.x as BigDecimal }.max()
    BigDecimal range = xMax - xMin
    if (range == 0) range = 1

    BigDecimal binWidth
    int nBins
    if (params.binwidth != null) {
      binWidth = params.binwidth as BigDecimal
      if (binWidth <= 0) {
        throw new IllegalArgumentException("stat_summary_bin requires a positive binwidth, but was " + binWidth)
      }
      if (binWidth <= 0) {
        throw new IllegalArgumentException("binwidth must be positive, was " + binWidth)
      }
      nBins = (range / binWidth).ceil() as int
    } else {
      Integer binsParam = params.bins as Integer
      if (binsParam == null) {
        nBins = 30
      } else if (binsParam <= 0) {
        throw new IllegalArgumentException("bins must be positive, was " + binsParam)
      } else {
        nBins = binsParam
      }
      binWidth = range / nBins
      if (binWidth <= 0) {
        throw new IllegalArgumentException("stat_summary_bin computed a non-positive binwidth " + binWidth + " from bins=" + nBins)
      }
    }

    // Group points into bins
    Map<Integer, List<Number>> binned = [:].withDefault { [] }
    points.each { point ->
      BigDecimal x = point.x as BigDecimal
      int binIdx = ((x - xMin) / binWidth) as int
      binIdx = Math.min(binIdx, nBins - 1)  // Handle edge case
      binned[binIdx] << (point.y as Number)
    }

    // Compute summaries for each bin
    String funType = params.fun as String ?: 'mean'
    Closure<Map<String, Number>> funData = params.'fun.data' as Closure<Map<String, Number>>

    List<BigDecimal> xVals = []
    List<BigDecimal> xMinVals = []
    List<BigDecimal> xMaxVals = []
    List<BigDecimal> yVals = []
    List<Integer> nVals = []

    binned.each { int binIdx, List<Number> yValues ->
      if (yValues.isEmpty()) return

      BigDecimal binMin = xMin + binIdx * binWidth
      BigDecimal binMax = binMin + binWidth
      BigDecimal binCenter = binMin + binWidth / 2

      xVals << binCenter
      xMinVals << binMin
      xMaxVals << binMax
      nVals << yValues.size()

      // Compute summary
      if (funData) {
        // Custom function
        Map<String, Number> result = funData.call(yValues)
        Number yValue = result.y ?: result.ymin ?: 0
        yVals << (yValue as BigDecimal)
      } else {
        // Standard summary function
        BigDecimal summary
        switch (funType) {
          case 'mean':
            summary = Stat.mean(yValues)
            break
          case 'median':
            summary = Stat.median(yValues)
            break
          case 'sum':
            summary = Stat.sum(yValues)
            break
          case 'min':
            summary = Stat.min(yValues)
            break
          case 'max':
            summary = Stat.max(yValues)
            break
          default:
            summary = Stat.mean(yValues)
        }
        yVals << summary
      }
    }

    return Matrix.builder()
        .data([
            x: xVals,
            xmin: xMinVals,
            xmax: xMaxVals,
            y: yVals,
            n: nVals
        ])
        .build()
  }

  /**
   * Confidence ellipse for bivariate normal data.
   * Computes points on an ellipse representing a confidence region.
   *
   * @param data Input matrix
   * @param aes Aesthetic mappings (requires x and y)
   * @param params Map with:
   *   - level: confidence level (default 0.95 for 95% confidence)
   *   - type: 't' for Student's t, 'norm' for normal, 'euclid' for raw (default 't')
   *   - segments: number of points on ellipse (default 51)
   * @return Matrix with columns: x, y (points on ellipse)
   */
  static Matrix ellipse(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_ellipse requires x and y aesthetics")
    }

    // Extract numeric x, y pairs
    List<BigDecimal> xValues = []
    List<BigDecimal> yValues = []
    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      if (xVal instanceof Number && yVal instanceof Number) {
        xValues << (xVal as BigDecimal)
        yValues << (yVal as BigDecimal)
      }
    }

    // Parameters
    Number levelParam = params.level as Number ?: 0.95
    double level = levelParam as double
    if (level <= 0d || level >= 1d) {
      throw new IllegalArgumentException("stat_ellipse level must be between 0 and 1 (exclusive), was: $level")
    }
    String type = params.type as String ?: 't'
    Integer segmentsParam = params.segments as Integer
    int segments = segmentsParam ?: 51
    if (segments < 3) {
      throw new IllegalArgumentException("stat_ellipse segments must be at least 3, was: $segments")
    }

    // Delegate to matrix-stats Ellipse class for calculation
    se.alipsa.matrix.stats.Ellipse.EllipseData result =
        se.alipsa.matrix.stats.Ellipse.calculate(xValues, yValues, level, type, segments)

    return Matrix.builder()
        .data([x: result.x, y: result.y])
        .build()
  }

  /**
   * Hexagonal binning - divides the plotting area into hexagonal bins and counts observations.
   *
   * @param data Input matrix with x and y columns
   * @param aes Aesthetic mappings
   * @param params Optional parameters: bins (default 30), binwidth
   * @return Matrix with columns: x (hex center), y (hex center), count
   */
  static Matrix binHex(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_bin_hex requires x and y aesthetics")
    }

    // Collect numeric x,y values and compute bounds
    def result = collectXYPointsAndBounds(data, xCol, yCol)
    List<Point> points = result.points as List<Point>
    BigDecimal xMin = result.xMin as BigDecimal
    BigDecimal yMin = result.yMin as BigDecimal
    BigDecimal xMax = result.xMax as BigDecimal

    if (points.isEmpty()) {
      return Matrix.builder().data([x: [], y: [], count: []]).build()
    }

    // Compute hex dimensions
    def hexDims = computeHexDimensions(xMax - xMin, params)
    BigDecimal dx = hexDims.dx as BigDecimal
    BigDecimal dy = hexDims.dy as BigDecimal

    // Count points in each hexagon
    Map<String, Integer> hexCounts = [:] as Map<String, Integer>

    for (se.alipsa.matrix.gg.geom.Point point : points) {
      // Find hexagon coordinates for this point
      int[] hexCoord = pointToHex(point.x as BigDecimal, point.y as BigDecimal, xMin, yMin, dx, dy)
      String hexKey = "${hexCoord[0]},${hexCoord[1]}"
      hexCounts[hexKey] = (hexCounts[hexKey] ?: 0) + 1
    }

    // Build result matrix
    List<BigDecimal> xVals = []
    List<BigDecimal> yVals = []
    List<Integer> countVals = []

    hexCounts.each { String key, Integer count ->
      def center = hexKeyToCenter(key, xMin, yMin, dx, dy)
      xVals << (center.x as BigDecimal)
      yVals << (center.y as BigDecimal)
      countVals << count
    }

    return Matrix.builder()
        .data([x: xVals, y: yVals, count: countVals])
        .build()
  }

  /**
   * Hexagonal summary statistics - divides the plotting area into hexagonal bins
   * and computes summary statistics for a z variable in each bin.
   *
   * @param data Input matrix with x, y, and z columns
   * @param aes Aesthetic mappings (requires x, y, and typically z or a fill aesthetic)
   * @param params Optional parameters: bins, binwidth, fun, fun.data
   * @return Matrix with columns: x (hex center), y (hex center), value (summary statistic)
   */
  static Matrix summaryHex(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName
    String zCol = aes.fillColName ?: aes.colorColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_summary_hex requires x and y aesthetics")
    }
    if (zCol == null) {
      throw new IllegalArgumentException("stat_summary_hex requires fill or color aesthetic for the summary variable")
    }

    // Collect numeric x,y,z values and compute bounds
    def result = collectXYZPointsAndBounds(data, xCol, yCol, zCol)
    List<Map<String, BigDecimal>> points = result.points as List<Map<String, BigDecimal>>
    BigDecimal xMin = result.xMin as BigDecimal
    BigDecimal yMin = result.yMin as BigDecimal
    BigDecimal xMax = result.xMax as BigDecimal

    if (points.isEmpty()) {
      return Matrix.builder().data([x: [], y: [], value: []]).build()
    }

    // Compute hex dimensions
    def hexDims = computeHexDimensions(xMax - xMin, params)
    BigDecimal dx = hexDims.dx as BigDecimal
    BigDecimal dy = hexDims.dy as BigDecimal

    // Group points by hexagon
    Map<String, List<BigDecimal>> hexValues = [:].withDefault { [] }

    for (Map<String, BigDecimal> point : points) {
      // Find hexagon coordinates for this point
      int[] hexCoord = pointToHex(point.x, point.y, xMin, yMin, dx, dy)
      String hexKey = "${hexCoord[0]},${hexCoord[1]}"
      hexValues[hexKey] << point.z
    }

    // Compute summaries for each bin
    String funType = params.fun as String ?: 'mean'
    Closure<Map<String, Number>> funData = params.'fun.data' as Closure<Map<String, Number>>

    List<BigDecimal> xVals = []
    List<BigDecimal> yVals = []
    List<BigDecimal> valueVals = []

    hexValues.each { String key, List<BigDecimal> zValues ->
      if (zValues.isEmpty()) return

      def center = hexKeyToCenter(key, xMin, yMin, dx, dy)
      xVals << (center.x as BigDecimal)
      yVals << (center.y as BigDecimal)

      // Compute summary
      BigDecimal summaryValue = computeHexSummary(zValues, funType, funData)
      valueVals << summaryValue
    }

    return Matrix.builder()
        .data([x: xVals, y: yVals, value: valueVals])
        .build()
  }

  /**
   * 2D rectangular binned summary statistics - divides the plotting area into rectangular bins
   * and computes summary statistics for a z variable in each bin.
   *
   * @param data Input matrix with x, y, and z columns
   * @param aes Aesthetic mappings (requires x, y, and fill or color for z)
   * @param params Optional parameters: bins, binwidth, fun, fun.data
   * @return Matrix with columns: x (bin center), y (bin center), value (summary statistic)
   */
  static Matrix summary2d(Matrix data, Aes aes, Map params = [:]) {
    String xCol = aes.xColName
    String yCol = aes.yColName
    String zCol = aes.fillColName ?: aes.colorColName

    if (xCol == null || yCol == null) {
      throw new IllegalArgumentException("stat_summary_2d requires x and y aesthetics")
    }
    if (zCol == null) {
      throw new IllegalArgumentException("stat_summary_2d requires fill or color aesthetic for the summary variable")
    }

    // Collect numeric x,y,z values and compute bounds
    def result = collectXYZPointsAndBounds(data, xCol, yCol, zCol)
    List<Map<String, BigDecimal>> points = result.points as List<Map<String, BigDecimal>>
    BigDecimal xMin = result.xMin as BigDecimal
    BigDecimal yMin = result.yMin as BigDecimal
    BigDecimal xMax = result.xMax as BigDecimal
    BigDecimal yMax = result.yMax as BigDecimal

    if (points.isEmpty()) {
      return Matrix.builder().data([x: [], y: [], value: []]).build()
    }

    // Calculate bin widths
    BigDecimal xRange = xMax - xMin
    BigDecimal yRange = yMax - yMin
    if (xRange == 0) xRange = 1
    if (yRange == 0) yRange = 1

    BigDecimal xBinWidth, yBinWidth
    int xNBins, yNBins

    if (params.binwidth != null) {
      xBinWidth = params.binwidth as BigDecimal
      yBinWidth = params.binwidth as BigDecimal
      if (xBinWidth <= 0) {
        throw new IllegalArgumentException("binwidth must be positive, was $xBinWidth")
      }
      xNBins = (xRange / xBinWidth).ceil() as int
      yNBins = (yRange / yBinWidth).ceil() as int
    } else {
      Integer binsParam = params.bins as Integer
      int bins
      if (binsParam == null) {
        bins = 30
      } else if (binsParam <= 0) {
        throw new IllegalArgumentException("bins must be positive, was $binsParam")
      } else {
        bins = binsParam
      }
      xNBins = yNBins = bins
      xBinWidth = xRange / xNBins
      yBinWidth = yRange / yNBins
    }

    // Group points by grid cell
    Map<String, List<BigDecimal>> gridValues = [:].withDefault { [] }

    for (Map<String, BigDecimal> point : points) {
      int xBinIdx = ((point.x - xMin) / xBinWidth) as int
      int yBinIdx = ((point.y - yMin) / yBinWidth) as int

      // Clamp to valid range
      xBinIdx = Math.max(0, Math.min(xBinIdx, xNBins - 1))
      yBinIdx = Math.max(0, Math.min(yBinIdx, yNBins - 1))

      String gridKey = "${xBinIdx},${yBinIdx}"
      gridValues[gridKey] << point.z
    }

    // Compute summaries for each grid cell
    String funType = params.fun as String ?: 'mean'
    Closure<Map<String, Number>> funData = params.'fun.data' as Closure<Map<String, Number>>

    List<BigDecimal> xVals = []
    List<BigDecimal> yVals = []
    List<BigDecimal> valueVals = []

    gridValues.each { String key, List<BigDecimal> zValues ->
      if (zValues.isEmpty()) return

      String[] coords = key.split(',')
      int xBinIdx = coords[0] as int
      int yBinIdx = coords[1] as int

      BigDecimal xCenter = xMin + xBinIdx * xBinWidth + xBinWidth / 2
      BigDecimal yCenter = yMin + yBinIdx * yBinWidth + yBinWidth / 2

      xVals << xCenter
      yVals << yCenter

      // Reuse computeHexSummary - it's generic despite the name
      BigDecimal summaryValue = computeHexSummary(zValues, funType, funData)
      valueVals << summaryValue
    }

    return Matrix.builder()
        .data([x: xVals, y: yVals, value: valueVals])
        .build()
  }

  /**
   * Helper: Collect x,y points from data and compute bounds.
   *
   * @param data Matrix containing the data
   * @param xCol Name of x column
   * @param yCol Name of y column
   * @return Map with points (List<BigDecimal[]>), xMin, xMax, yMin, yMax
   */
  private static Map<String, Object> collectXYPointsAndBounds(Matrix data, String xCol, String yCol) {
    List<se.alipsa.matrix.gg.geom.Point> points = []
    BigDecimal xMin = null
    BigDecimal xMax = null
    BigDecimal yMin = null
    BigDecimal yMax = null

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]

      if (xVal instanceof Number && yVal instanceof Number) {
        BigDecimal x = xVal as BigDecimal
        BigDecimal y = yVal as BigDecimal
        points << new Point(x, y)

        xMin = xMin == null ? x : xMin.min(x)
        xMax = xMax == null ? x : xMax.max(x)
        yMin = yMin == null ? y : yMin.min(y)
        yMax = yMax == null ? y : yMax.max(y)
      }
    }

    return [points: points, xMin: xMin, xMax: xMax, yMin: yMin, yMax: yMax]
  }

  /**
   * Helper: Collect x,y,z points from data and compute x,y bounds.
   *
   * @param data Matrix containing the data
   * @param xCol Name of x column
   * @param yCol Name of y column
   * @param zCol Name of z column
   * @return Map with points (List<Map<String,BigDecimal>>), xMin, xMax, yMin, yMax
   */
  private static Map<String, Object> collectXYZPointsAndBounds(Matrix data, String xCol, String yCol, String zCol) {
    List<Map<String, BigDecimal>> points = []
    BigDecimal xMin = null
    BigDecimal xMax = null
    BigDecimal yMin = null
    BigDecimal yMax = null

    data.each { row ->
      def xVal = row[xCol]
      def yVal = row[yCol]
      def zVal = row[zCol]

      if (xVal instanceof Number && yVal instanceof Number && zVal instanceof Number) {
        BigDecimal x = xVal as BigDecimal
        BigDecimal y = yVal as BigDecimal
        BigDecimal z = zVal as BigDecimal
        points << [x: x, y: y, z: z]

        xMin = xMin == null ? x : xMin.min(x)
        xMax = xMax == null ? x : xMax.max(x)
        yMin = yMin == null ? y : yMin.min(y)
        yMax = yMax == null ? y : yMax.max(y)
      }
    }

    return [points: points, xMin: xMin, xMax: xMax, yMin: yMin, yMax: yMax]
  }

  /**
   * Helper: Compute hexagon dimensions from x range and parameters.
   *
   * @param xRange The range of x values
   * @param params Parameters containing bins or binwidth
   * @return Map with dx (horizontal spacing) and dy (vertical spacing)
   */
  private static Map<String, BigDecimal> computeHexDimensions(BigDecimal xRange, Map params) {
    BigDecimal adjustedRange = xRange == 0 ? 1 : xRange

    BigDecimal hexWidth
    if (params.binwidth != null) {
      hexWidth = params.binwidth as BigDecimal
      if (hexWidth <= 0) {
        throw new IllegalArgumentException("binwidth must be positive, was $hexWidth")
      }
    } else {
      Integer binsParam = params.bins as Integer
      int bins
      if (binsParam == null) {
        bins = 30
      } else if (binsParam <= 0) {
        throw new IllegalArgumentException("bins must be positive, was $binsParam")
      } else {
        bins = binsParam
      }
      hexWidth = adjustedRange / bins
    }

    // For regular hexagons (flat-top), height = width * sqrt(3) / 2
    BigDecimal hexHeight = hexWidth * (3.sqrt() / 2)

    // Hexagon spacing
    BigDecimal dx = hexWidth * 0.75  // horizontal spacing between hex centers
    BigDecimal dy = hexHeight        // vertical spacing between hex centers

    return [dx: dx, dy: dy]
  }

  /**
   * Helper: Convert hex grid key to center coordinates.
   *
   * @param key Hex key in format "col,row"
   * @param xMin Minimum x value (origin)
   * @param yMin Minimum y value (origin)
   * @param dx Horizontal spacing
   * @param dy Vertical spacing
   * @return Map with x and y center coordinates
   */
  private static Map<String, BigDecimal> hexKeyToCenter(String key, BigDecimal xMin, BigDecimal yMin,
                                                         BigDecimal dx, BigDecimal dy) {
    def coords = key.split(',')
    int col = coords[0] as int
    int row = coords[1] as int

    // Calculate hexagon center
    BigDecimal hexX = xMin + col * dx
    BigDecimal hexY = yMin + row * dy

    // Offset every other row
    if (row % 2 == 1) {
      hexX = hexX + dx / 2
    }

    return [x: hexX, y: hexY]
  }

  /**
   * Helper: Compute summary statistic for a hex bin.
   *
   * @param zValues List of z values in the bin
   * @param funType Type of summary function ('mean', 'median', 'sum', 'min', 'max')
   * @param funData Optional custom function closure
   * @return Summary value
   */
  private static BigDecimal computeHexSummary(List<BigDecimal> zValues, String funType,
                                               Closure<Map<String, Number>> funData) {
    if (funData) {
      // Custom function
      Map<String, Number> result = funData.call(zValues)
      if (!result.containsKey('y') && !result.containsKey('value')) {
        throw new IllegalArgumentException(
            "Custom summary function must return a map with 'y' or 'value' key, got keys: ${result.keySet()}")
      }
      Number value = result.y ?: result.value
      return value as BigDecimal
    } else {
      // Standard summary function
      BigDecimal summary
      switch (funType) {
        case 'mean':
          summary = Stat.mean(zValues)
          break
        case 'median':
          summary = Stat.median(zValues)
          break
        case 'sum':
          summary = Stat.sum(zValues)
          break
        case 'min':
          summary = zValues.min()
          break
        case 'max':
          summary = zValues.max()
          break
        default:
          throw new IllegalArgumentException("Unknown summary function: $funType")
      }
      return summary
    }
  }

  /**
   * Convert a point to hexagon grid coordinates.
   * Returns [col, row] in the hexagonal grid.
   *
   * @param x    the x-coordinate of the point in data space
   * @param y    the y-coordinate of the point in data space
   * @param xMin the minimum x value (origin) of the hex grid
   * @param yMin the minimum y value (origin) of the hex grid
   * @param dx   the horizontal spacing between hexagon centers
   * @param dy   the vertical spacing between hexagon centers
   * @return an array [col, row] giving the hex grid column and row indices
   */
  private static int[] pointToHex(BigDecimal x, BigDecimal y, BigDecimal xMin, BigDecimal yMin,
                                   BigDecimal dx, BigDecimal dy) {
    BigDecimal relX = x - xMin
    BigDecimal relY = y - yMin

    // Approximate column and row
    int row = (relY / dy).round() as int
    int col = (relX / dx).round() as int

    // Adjust column for odd rows
    if (row % 2 == 1) {
      col = ((relX - dx / 2) / dx).round() as int
    }

    return [col, row] as int[]
  }

  /**
   * Align data groups to a common set of x-coordinates via linear interpolation.
   * Used primarily for stacked area charts where groups have different x-values.
   *
   * This stat creates a union of all x-values across all groups, then interpolates
   * y-values for each group at those common x-coordinates. This ensures smooth stacking.
   *
   * @param data the input data Matrix
   * @param aes the aesthetic mappings (must include x and y)
   * @param params optional parameters (currently unused)
   * @return Matrix with aligned data (all groups have same x-values)
   */
  @CompileDynamic
  static Matrix align(Matrix data, Aes aes, Map params = [:]) {
    if (data == null || data.rowCount() == 0) return data

    // 1. Extract column names
    String xCol = aes.xColName
    String yCol = aes.yColName
    String groupCol = aes.fillColName ?: aes.groupColName ?: aes.colorColName

    if (!xCol || !yCol) {
      return data  // Require x and y
    }

    // Verify columns exist in data
    if (!data.columnNames().contains(xCol) || !data.columnNames().contains(yCol)) {
      return data  // Return original if columns missing
    }

    // 2. Collect all unique x-values across all groups
    Set<BigDecimal> allXValues = new TreeSet<>()
    data.each { row ->
      def xVal = ScaleUtils.coerceToNumber(row[xCol])
      if (xVal != null) {
        allXValues.add(xVal)
      }
    }

    if (allXValues.size() <= 1) {
      return data  // No alignment needed
    }

    // 3. Group data by group column (or single group)
    def groups = data.rows().groupBy { row ->
      groupCol ? row[groupCol] : '__all__'
    }

    // 4. For each group, interpolate y-values at all x-coordinates
    List<Map> alignedRows = []
    groups.each { groupKey, rows ->
      // Sort by x (rows is a List but type checking doesn't see it as List<Map>)
      def sortedRows = rows.sort(false) { a, b ->
        def aNum = ScaleUtils.coerceToNumber(a[xCol])
        def bNum = ScaleUtils.coerceToNumber(b[xCol])
        if (aNum == null && bNum == null) return 0
        if (aNum == null) return -1
        if (bNum == null) return 1
        return aNum <=> bNum
      }

      // Filter out rows with null x or y
      def validRows = sortedRows.findAll { row ->
        ScaleUtils.coerceToNumber(row[xCol]) != null &&
            ScaleUtils.coerceToNumber(row[yCol]) != null
      }

      if (validRows.isEmpty()) return

      // Create interpolated points for each x-value in the union
      allXValues.each { targetX ->
        Map newRow = [:]

        // Copy group identifier
        if (groupCol) {
          newRow[groupCol] = groupKey != '__all__' ? groupKey : null
        }

        // Set x value
        newRow[xCol] = targetX

        // Interpolate y value
        BigDecimal yValue = interpolateY(validRows, xCol, yCol, targetX)
        newRow[yCol] = yValue

        // Copy other columns from nearest row (preserve additional aesthetics)
        copyOtherColumns(newRow, validRows, xCol, targetX, [xCol, yCol, groupCol] as Set)

        alignedRows << newRow
      }
    }

    // 5. Build result matrix
    if (alignedRows.isEmpty()) {
      return data
    }

    return Matrix.builder()
        .mapList(alignedRows)
        .build()
  }

  /**
   * Linearly interpolate y-value at targetX based on surrounding points.
   * Handles edge cases: extrapolation at boundaries uses nearest value.
   *
   * @param rows sorted list of data rows (must be sorted by x)
   * @param xCol x column name
   * @param yCol y column name
   * @param targetX x-coordinate to interpolate at
   * @return interpolated y-value
   */
  @CompileDynamic
  private static BigDecimal interpolateY(List rows, String xCol, String yCol, BigDecimal targetX) {
    if (rows.isEmpty()) return null

    // Find surrounding points
    def before = null
    def after = null

    for (int i = 0; i < rows.size(); i++) {
      BigDecimal rowX = ScaleUtils.coerceToNumber(rows[i][xCol])
      if (rowX == null) continue

      if (rowX <= targetX) {
        before = rows[i]
      }
      if (rowX >= targetX && after == null) {
        after = rows[i]
        break
      }
    }

    // Edge case: targetX is exactly at a data point
    if (before != null) {
      BigDecimal beforeX = ScaleUtils.coerceToNumber(before[xCol])
      if (beforeX == targetX) {
        return ScaleUtils.coerceToNumber(before[yCol])
      }
    }
    if (after != null) {
      BigDecimal afterX = ScaleUtils.coerceToNumber(after[xCol])
      if (afterX == targetX) {
        return ScaleUtils.coerceToNumber(after[yCol])
      }
    }

    // Case 1: targetX is before all data points (extrapolate using first point)
    if (before == null && after != null) {
      return ScaleUtils.coerceToNumber(after[yCol])
    }

    // Case 2: targetX is after all data points (extrapolate using last point)
    if (before != null && after == null) {
      return ScaleUtils.coerceToNumber(before[yCol])
    }

    // Case 3: Linear interpolation between before and after
    if (before != null && after != null) {
      BigDecimal x0 = ScaleUtils.coerceToNumber(before[xCol])
      BigDecimal y0 = ScaleUtils.coerceToNumber(before[yCol])
      BigDecimal x1 = ScaleUtils.coerceToNumber(after[xCol])
      BigDecimal y1 = ScaleUtils.coerceToNumber(after[yCol])

      if (x0 == null || y0 == null || x1 == null || y1 == null) {
        return y0 ?: y1  // Fallback to available value
      }

      if (x1 == x0) {
        // Avoid division by zero (points have same x)
        return (y0 + y1) / 2
      }

      // Linear interpolation formula: y = y0 + (y1 - y0) * (x - x0) / (x1 - x0)
      BigDecimal t = (targetX - x0) / (x1 - x0)
      return y0 + (y1 - y0) * t
    }

    // Fallback: no valid data
    return null
  }

  /**
   * Copy additional columns from the nearest row to preserve aesthetics.
   *
   * @param targetRow the row being constructed
   * @param sourceRows the list of source rows (sorted by x)
   * @param xCol x column name
   * @param targetX x-coordinate of target row
   * @param excludeCols columns to skip (already set)
   */
  @CompileDynamic
  private static void copyOtherColumns(Map targetRow, List sourceRows,
                                       String xCol, BigDecimal targetX, Set<String> excludeCols) {
    if (sourceRows.isEmpty()) return

    // Find nearest row by x-distance
    def nearest = sourceRows[0]
    BigDecimal minDist = null

    sourceRows.each { row ->
      BigDecimal rowX = ScaleUtils.coerceToNumber(row[xCol])
      if (rowX != null) {
        BigDecimal dist = (rowX - targetX).abs()
        if (minDist == null || dist < minDist) {
          minDist = dist
          nearest = row
        }
      }
    }

    // Copy all columns except those already set
    // Convert to Map if it's a Row object
    def nearestMap = (nearest instanceof Map) ? nearest : nearest.toMap()
    nearestMap.each { key, value ->
      if (!excludeCols.contains(key) && !targetRow.containsKey(key)) {
        targetRow[key] = value
      }
    }
  }

}
