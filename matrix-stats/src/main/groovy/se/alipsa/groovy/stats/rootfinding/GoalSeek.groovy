package se.alipsa.groovy.stats.rootfinding

import static java.lang.Math.*

/**
Similar to the excel goal seek function. Uses the Bisection method which is predicable and linear
but slow compared to other ones such as Newton Raphson (which in the other hand is not guaranteed)
 */
class GoalSeek {

  /**
   * Note that the threshold should not be set too high as doubles are used in the calculations and evaluation
   * which does not have a reliable precision beyond about 11 decimals
   *
   * @param targetValue the result we are after
   * @param minValue the min value of the span to search for the value in
   * @param maxValue the max value of the span to search for the value in
   * @param threshold the allowed diff to still be considered as "no difference"
   * @param maxIterations the maximum number of iterations allowed
   * @param algorithm a closure containing the algoritm to apply
   * @return a Map of the results with the following keys:
   * <ul>
   *  <li>value: the actual value needed to produce the result</li>
   *  <li>result: the result of the value put in the algorithm</li>
   *  <li>diff: the difference from the target value</li>
   *  <li>iterations: the number of iterations it took to get to the conclusion</li>
   * </ul>
   * @throws RuntimeException if the goal cannot be found within the maxIterations
   */
  static Map solve(final double targetValue, double minValue, double maxValue, double threshold = 0.000_000_1, int maxIterations = 100_000, Closure<Double> algorithm) {

    Seeker seeker = new Seeker(targetValue as double, minValue as double, maxValue as double)
    int i = 1
    double currentValue
    do {
      currentValue = algorithm.call(seeker.getMidValue())
      if (!Double.isInfinite(currentValue) && !Double.isNaN(currentValue)) {
        seeker.evaluateAndAdjust(currentValue)

        if (abs(seeker.getMinValue() - seeker.getMaxValue()) / 2 <= threshold
            || abs(seeker.getDiff(currentValue)) < threshold) {
          double val = seeker.getMidValue()
          // println "Found value $val after $i iterations, ${algorithm.call(val)}"
          return [value: val, result: algorithm.call(val), diff: seeker.getDiff(currentValue), iterations: i]
        }
      }
      i++
    } while (i <= maxIterations)

    throw new RuntimeException("Failed to find goal $targetValue after $i iterations, the closest we got was ${seeker.getMidValue()}")
  }

  private static class Seeker {
    private final double targetValue
    private double minValue
    private double maxValue

    Seeker(double targetValue, double minValue, double maxValue) {
      this.targetValue = targetValue;
      this.minValue = minValue;
      this.maxValue = maxValue;
    }

    double getMidValue() {
      return (minValue + maxValue) / 2
    }

    void evaluateAndAdjust(final double currentValue) {
      final double difference = getDiff(currentValue)
      final double midValue = getMidValue()
      if (difference < 0) {
        maxValue = midValue
      } else {
        minValue = midValue
      }
    }

    double getDiff(final double currentValue) {
      return targetValue - currentValue
    }

    double getMinValue() {
      return minValue
    }

    double getMaxValue() {
      return maxValue
    }
  }
}
