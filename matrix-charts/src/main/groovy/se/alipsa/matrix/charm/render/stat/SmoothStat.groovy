package se.alipsa.matrix.charm.render.stat

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import se.alipsa.matrix.charm.LayerSpec
import se.alipsa.matrix.charm.render.LayerData
import se.alipsa.matrix.charm.util.NumberCoercionUtil
import se.alipsa.matrix.core.Stat
import se.alipsa.matrix.stats.distribution.TDistribution
import se.alipsa.matrix.stats.regression.LinearRegression
import se.alipsa.matrix.stats.regression.PolynomialRegression
import se.alipsa.matrix.stats.regression.RegressionUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Smooth stat transformation - computes regression lines with optional confidence intervals.
 *
 * <p>Supports params:</p>
 * <ul>
 *   <li>{@code method} - regression method ('lm', default)</li>
 *   <li>{@code formula} - formula string, supports poly(x, n)</li>
 *   <li>{@code se} - show confidence interval (default true)</li>
 *   <li>{@code level} - confidence level (default 0.95)</li>
 *   <li>{@code n} - number of fitted points (default 80)</li>
 *   <li>{@code degree} - polynomial degree (default 1, overrides formula)</li>
 * </ul>
 */
@CompileStatic
class SmoothStat {

  private static final Pattern POLY_PATTERN = Pattern.compile(/poly\s*\(\s*(\w+)\s*,\s*(\d+)\s*\)/)

  /**
   * Computes regression line with optional confidence intervals.
   *
   * @param layer layer specification
   * @param data layer data
   * @return fitted points with x, y and meta.ymin/meta.ymax when se=true
   */
  @CompileDynamic
  static List<LayerData> compute(LayerSpec layer, List<LayerData> data) {
    List<LayerData> numeric = data.findAll { LayerData d ->
      NumberCoercionUtil.coerceToBigDecimal(d.x) != null &&
          NumberCoercionUtil.coerceToBigDecimal(d.y) != null
    }
    if (numeric.size() < 2) {
      return data
    }

    Map<String, Object> params = StatEngine.effectiveParams(layer)
    boolean se = params.se != false
    BigDecimal level = params.level != null ? (params.level as BigDecimal) : 0.95
    int nPoints = params.n != null ? (params.n as int) : 80
    int polyDegree = resolvePolyDegree(params)

    int minDataPoints = polyDegree + 1
    if (numeric.size() < minDataPoints) {
      return data
    }

    List<BigDecimal> xValues = numeric.collect { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.x) }
    List<BigDecimal> yValues = numeric.collect { LayerData d -> NumberCoercionUtil.coerceToBigDecimal(d.y) }

    def regression
    if (polyDegree > 1) {
      regression = new PolynomialRegression(xValues, yValues, polyDegree)
    } else {
      regression = new LinearRegression(xValues, yValues)
    }

    BigDecimal xMin = xValues.min()
    BigDecimal xMax = xValues.max()

    // Compute SE components if needed
    BigDecimal sigma2 = 0.0
    int dfResid = xValues.size() - (polyDegree + 1)
    BigDecimal sxx = 0.0
    BigDecimal xBar = 0.0
    BigDecimal[][] xtxInv = null
    if (se && dfResid > 0) {
      if (polyDegree == 1) {
        xBar = Stat.mean(xValues)
        xValues.each { Number val ->
          BigDecimal diff = val - xBar
          sxx += diff * diff
        }
      }
      BigDecimal sse = 0.0
      for (int i = 0; i < xValues.size(); i++) {
        BigDecimal xi = xValues[i]
        BigDecimal yi = yValues[i]
        BigDecimal yFit = regression.predict(xi)
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

    List<LayerData> result = []
    for (int i = 0; i < nPoints; i++) {
      BigDecimal x = xMin + (xMax - xMin) * i / (nPoints - 1)
      BigDecimal yFit = regression.predict(x)
      LayerData datum = new LayerData(
          x: x,
          y: yFit,
          color: numeric.first().color,
          fill: numeric.first().fill,
          rowIndex: -1
      )
      if (se) {
        BigDecimal seFit
        if (polyDegree == 1) {
          BigDecimal dx = x - xBar
          seFit = (sigma2 * (1.0 / xValues.size() + (dx * dx) / sxx)).sqrt()
        } else {
          BigDecimal leverage = RegressionUtils.polynomialLeverage(xtxInv, x, polyDegree)
          seFit = (sigma2 * 0.0.max(leverage)).sqrt()
        }
        BigDecimal margin = (tCrit * seFit) as BigDecimal
        datum.meta.ymin = yFit - margin
        datum.meta.ymax = yFit + margin
      }
      result << datum
    }
    result
  }

  private static int resolvePolyDegree(Map<String, Object> params) {
    String formula = params.formula as String
    if (params.degree != null) {
      return params.degree as int
    }
    if (formula != null) {
      return parseFormulaDegree(formula)
    }
    1
  }

  private static int parseFormulaDegree(String formula) {
    if (formula == null || formula.trim().isEmpty()) {
      return 1
    }
    String rhs = formula.contains('~') ? formula.split('~', 2)[1]?.trim() : formula.trim()
    if (rhs == null || rhs.isEmpty()) {
      return 1
    }
    Matcher matcher = POLY_PATTERN.matcher(rhs)
    if (matcher.find()) {
      int degree = Integer.parseInt(matcher.group(2))
      if (degree < 1) {
        throw new IllegalArgumentException("Polynomial degree must be at least 1, got: $degree")
      }
      return degree
    }
    1
  }

  private static BigDecimal tCritical(int df, double level) {
    if (df <= 0) return 0.0
    BigDecimal target = 0.5 + level / 2.0
    TDistribution dist = new TDistribution(df as BigDecimal)
    BigDecimal low = 0.0
    BigDecimal high = 1.0
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
    (low + high) / 2.0d
  }
}
