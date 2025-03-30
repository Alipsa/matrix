package se.alipsa.matrix.stats

import org.apache.commons.math3.stat.inference.TestUtils
import se.alipsa.matrix.core.ListConverter
import se.alipsa.matrix.core.Matrix

/**
 * Analysis of variance (ANOVA) is a collection of statistical models and their associated estimation
 * procedures (such as the "variation" among and between groups) used to analyze the differences among means.
 * ANOVA was developed by the statistician Ronald Fisher. ANOVA is based on the law of total variance,
 * where the observed variance in a particular variable is partitioned into components attributable to
 * different sources of variation. In its simplest form, ANOVA provides a statistical test of whether
 * two or more population means are equal, and therefore generalizes the t-test beyond two means.
 * In other words, the ANOVA is used to test the difference between two or more means.
 */
class Anova {

  static AnovaResult aov(Matrix data, List<String> colNames) {
    Map<String, List<? extends Number>> d = [:]
    colNames.each {
      String colName = it
      d.put(colName, data[colName] as List<? extends Number>)
    }
    return aov(d)
  }

  /**
   * Implements one-way ANOVA (analysis of variance) statistics.
   *
   */
  static AnovaResult aov(Map<String, List<? extends Number>> data) {
    if (data.size() < 2) {
      throw new IllegalArgumentException("data must contain at least 2 groups (found only ${data.size()} groups")
    }
    def result = new AnovaResult()
    // TODO implement me! using commons math for now...
    List categoryData = []
    data.each { String key, List<? extends Number> values ->
      categoryData.add(ListConverter.toDoubleArray(values))
    }

    result.pValue = TestUtils.oneWayAnovaPValue(categoryData)
    result.fValue = TestUtils.oneWayAnovaFValue(categoryData)
    return result
  }

  static class AnovaResult {
    Double pValue
    Double fValue

    String toString() {
      return "pValue: ${pValue}, fValue: ${fValue}"
    }

    boolean evaluate(double alpha = 0.05) {
      return pValue < alpha
    }
  }
}
