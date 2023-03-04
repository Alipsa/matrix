package se.alipsa.matrix

class Correlation {

  static final String PEARSON = "pearson"
  static final String SPEARMAN = "spearman"
  static final String KENDALL = "kendall"

  static BigDecimal cor(List<? extends Number> valuesX, List<? extends Number> valuesY, String method = PEARSON) {
    if (method == PEARSON) return corPearson(valuesX, valuesY)
    if (method == SPEARMAN) return corSpearman(valuesX, valuesY)
    if (method == KENDALL) return corKendall(valuesX, valuesY)
    throw new IllegalArgumentException("Unknown method: ${method}")
  }

  /**
   * measures a linear dependence between two variables (x and y) i.e. a parametric correlation test
   * as it depends on the distribution of the data.
   * @param numbers1 the first list of Numbers
   * @param numbers2 the second list of Numbers
   * @return a value between -1 to +1 where -1 represents X and Y are negatively correlated
   * and +1 represents X and Y are positively correlated
   */
  static BigDecimal corPearson( List<? extends Number> numbersX, List<? extends Number> numbersY) {
    def sumX = 0.0
    def sumY = 0.0
    def sumXY = 0.0
    def sumX2 = 0.0
    def sumY2 = 0.0
    def size = numbersX.size()

    def final itX = numbersX.iterator()
    def final itY = numbersY.iterator()
    for (int i = 0; i < size; i++) {
      def x = itX.next()
      def y = itY.next()

      sumX += x
      sumY += y
      sumXY += x * y
      sumX2 += x * x
      sumY2 += y * y
    }

    def final bottom = Math.sqrt((size * sumX2 - sumX * sumX) * (size * sumY2 - sumY * sumY))
    if (bottom == 0) return 0
    def final top = size * sumXY - sumX * sumY
    return top / bottom
  }

  static BigDecimal corSpearman(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    int n = numbersX.size()
    def sum_X = 0.0, sum_Y = 0.0, sum_XY = 0.0, squareSum_X = 0.0, squareSum_Y = 0.0
    Number x, y
    for (int i = 0; i < n; i++) {
      x = numbersX.get(i)
      y = numbersY.get(i)

      sum_X = sum_X + x
      sum_Y = sum_Y + y
      sum_XY = sum_XY + x * y

      // sum of square of elements.
      squareSum_X = squareSum_X + x ** 2
      squareSum_Y = squareSum_Y + y ** 2
    }

    return (n * sum_XY - sum_X * sum_Y) / Math.sqrt(
      (n * squareSum_X - sum_X * sum_X)
      * (n * squareSum_Y - sum_Y * sum_Y))
  }

  static BigDecimal corKendall(List<? extends Number> numbersX, List<? extends Number> numbersY) {
    throw new RuntimeException("Kendall's Tau-b rank correlation not yet implemented")
  }

}
