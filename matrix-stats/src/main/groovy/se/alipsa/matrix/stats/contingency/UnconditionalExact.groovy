package se.alipsa.matrix.stats.contingency

import groovy.transform.PackageScope

/** Shared enumeration and bounded nuisance-parameter optimisation for two binomial groups. */
@PackageScope
@SuppressWarnings('DuplicateNumberLiteral')
class UnconditionalExact {

  static final int REFINEMENT_BUDGET = 240

  static double[] maximise(int n1, int n2, Closure<Boolean> extreme) {
    int n = n1 + n2
    double[] factorial = new double[n + 1]
    (1..n).each { int i -> factorial[i] = factorial[i - 1] + Math.log(i) }
    List<int[]> mask = []
    List<Double> coefficients = []
    (0..n1).each { int x1 ->
      (0..n2).each { int x2 ->
        if (extreme.call(x1, x2, factorial)) {
          mask << ([x1 + x2, n - x1 - x2] as int[])
          coefficients << logChoose(factorial, n1, x1) + logChoose(factorial, n2, x2)
        }
      }
    }
    Closure<Double> probability = { double pi ->
      double sum = 0
      for (int i = 0; i < mask.size(); i++) {
        int successes = mask[i][0]
        int failures = mask[i][1]
        if (pi == 0 || pi == 1) {
          if (pi == 0 ? successes == 0 : failures == 0) {
            sum += Math.exp(coefficients[i])
          }
        } else {
          sum += Math.exp(coefficients[i] + successes * Math.log(pi) + failures * Math.log1p(-pi))
        }
      }
      Math.min(1.0d, sum)
    }
    double[] grid = (0..100).collect { int i -> probability.call(i / 100.0d) } as double[]
    int best = 0
    (1..100).each { int i -> if (grid[i] > grid[best]) { best = i } }
    double bestValue = grid[best]
    double bestPi = best / 100.0d
    int remaining = REFINEMENT_BUDGET
    for (int i = 1; i < 100 && remaining > 0; i++) {
      if (grid[i] >= grid[i - 1] && grid[i] >= grid[i + 1] && grid[i] >= bestValue - 1e-3) {
        double lo = (i - 1) / 100.0d
        double hi = (i + 1) / 100.0d
        double left = hi - (hi - lo) * 0.6180339887498949d
        double right = lo + (hi - lo) * 0.6180339887498949d
        double fl = probability.call(left)
        double fr = probability.call(right)
        remaining -= 2
        while (hi - lo > 1e-10 && remaining > 0) {
          if (fl > fr) {
            hi = right
            right = left
            fr = fl
            left = hi - (hi - lo) * 0.6180339887498949d
            fl = probability.call(left)
          } else {
            lo = left
            left = right
            fl = fr
            right = lo + (hi - lo) * 0.6180339887498949d
            fr = probability.call(right)
          }
          remaining--
        }
        if (Math.max(fl, fr) > bestValue) {
          bestValue = Math.max(fl, fr)
          bestPi = fl > fr ? left : right
        }
      }
    }
    [bestValue, bestPi] as double[]
  }

  static double logChoose(double[] factorial, int n, int k) {
    factorial[n] - factorial[k] - factorial[n - k]
  }

  static double fisher(int a, int n1, int c, int n2, double[] factorial) {
    int total = n1 + n2
    int successes = a + c
    int lo = Math.max(0, successes - n2)
    int hi = Math.min(n1, successes)
    double denominator = logChoose(factorial, total, successes)
    double observed = logChoose(factorial, n1, a) + logChoose(factorial, n2, c) - denominator
    double sum = 0
    (lo..hi).each { int x ->
      double probability = logChoose(factorial, n1, x) + logChoose(factorial, n2, successes - x) - denominator
      if (probability <= observed + Math.log1p(1e-7)) {
        sum += Math.exp(probability)
      }
    }
    Math.min(1.0d, sum)
  }
}
